/*
 * Copyright (C) 2020 The Libphonenumber Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.phonenumbers;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.phonenumbers.migrator.MigrationEntry;
import com.google.phonenumbers.migrator.MigrationFactory;
import com.google.phonenumbers.migrator.MigrationJob;
import com.google.phonenumbers.migrator.MigrationResult;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

@WebServlet(name = "Migrate", value = "/migrate")
public class ServletMain extends HttpServlet {

  public static final int MAX_UPLOAD_SIZE = 50000;
  /**
   * Countries with large valid number ranges cannot be migrated using the web application due to request timeouts. The
   * command line tool must be used in such cases.
   */
  // TODO: add loading spinner and longer request timeouts to UI to allow for migrations of given country codes
  public static final ImmutableSet<String> LARGE_COUNTRY_RANGES = ImmutableSet.of(
          "1", // US/Canada -- 9.8MB
          "86", // China -- 16.1MB
          "55", // Brazil -- 3.4MB
          "61" // Australia -- 12.8MB
  );

  /**
   * Retrieves the form data for either a single number migration or a file migration from the index.jsp file and calls
   * the relevant method to perform the migration
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    ServletFileUpload upload = new ServletFileUpload();
    String countryCode = "";
    String number = "";
    String file = "";
    String fileName = "";
    String customRecipe = "";

    try {
      upload.setSizeMax(MAX_UPLOAD_SIZE);
      FileItemIterator iterator = upload.getItemIterator(req);
      while (iterator.hasNext()) {
        FileItemStream item = iterator.next();
        InputStream in = item.openStream();

        if (item.isFormField() && (item.getFieldName().equals("numberCountryCode")
                || item.getFieldName().equals("fileCountryCode"))) {
          countryCode = Streams.asString(in);

        } else if (item.isFormField() && item.getFieldName().equals("number")) {
          number = Streams.asString(in);

        } else if (item.getFieldName().equals("file")) {
          fileName = item.getName();
          try {
            file = IOUtils.toString(in);
          } finally {
            IOUtils.closeQuietly(in);
          }

        } else {
          try {
            customRecipe = IOUtils.toString(in);
          } finally {
            IOUtils.closeQuietly(in);
          }
        }
      }
    } catch (FileUploadException e) {
      e.printStackTrace();
    }

    if (!number.isEmpty() && !countryCode.isEmpty()) {
      /*
        number and country code are being set again to allow users to see their inputs after the http request has
        re-rendered the page.
      */
      req.setAttribute("number", number);
      req.setAttribute("numberCountryCode", countryCode);

      handleSingleNumberMigration(req, resp, number, countryCode, customRecipe);

    } else if (!file.isEmpty() && !countryCode.isEmpty()) {
      ImmutableList.Builder<String> numbersFromFile = ImmutableList.builder();
      StringTokenizer tokenizer = new StringTokenizer(file, "\n");
      while (tokenizer.hasMoreTokens()) {
        numbersFromFile.add(tokenizer.nextToken());
      }

      handleFileMigration(req, resp, numbersFromFile.build(), countryCode, customRecipe, fileName);
    }
  }

  /** Retrieves the form data for the numbers after a file migration and downloads them as a text file. */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    CharMatcher matcher = CharMatcher.anyOf("/\\");
    String fileName = "+" + matcher.removeFrom(req.getParameter("countryCode")) + "_Migration_ " +
            matcher.removeFrom(req.getParameter("fileName"));
    String fileContent = req.getParameter("fileContent");

    resp.setContentType("text/plain");
    resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
    try {
      OutputStream outputStream = resp.getOutputStream();
      outputStream.write(fileContent.getBytes());
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      req.setAttribute("fileError", e.getMessage());
      req.getRequestDispatcher("index.jsp").forward(req, resp);
    }
  }

  /**
   * Performs a single number migration of a given number and country code and sends the details of the migration to the
   * jsp file which outputs it to the user.
   *
   * @throws RuntimeException, which can be any of the given exceptions when going through the process of creating a {@link
   * MigrationJob} and running a migration which includes IllegalArgumentExceptions.
   */
  public void handleSingleNumberMigration(HttpServletRequest req,
      HttpServletResponse resp,
      String number,
      String countryCode,
      String customRecipe)
      throws ServletException, IOException {

    if (customRecipe.isEmpty() && LARGE_COUNTRY_RANGES.contains(countryCode)) {
      req.setAttribute("numberError", "'+" + countryCode + "' migrations cannot be performed using this web" +
              " application, please follow the documentation link above to use the command line tool instead.");
      req.getRequestDispatcher("index.jsp").forward(req, resp);
      return;
    }

    MigrationJob job;
    try {
      if (!customRecipe.isEmpty()) {
        job = MigrationFactory.createCustomRecipeMigration(number, countryCode, MigrationFactory
                .importRecipes(IOUtils.toInputStream(customRecipe)));
      } else {
        job = MigrationFactory.createMigration(number, countryCode);
      }
      MigrationJob.MigrationReport report = job.getMigrationReportForCountry();
      if (report.getValidMigrations().size() == 1) {
        req.setAttribute("validMigration", report.getValidMigrations().get(0).getMigratedNumber());
      } else if (report.getInvalidMigrations().size() == 1) {
        req.setAttribute("invalidMigration", report.getInvalidMigrations().get(0).getMigratedNumber());
      } else if (report.getValidUntouchedEntries().size() == 1) {
        req.setAttribute("alreadyValidNumber", report.getValidUntouchedEntries().get(0).getSanitizedNumber());
      }

    } catch (RuntimeException e) {
      req.setAttribute("numberError", e.getMessage());

    } finally {
      req.getRequestDispatcher("index.jsp").forward(req, resp);
    }
  }

  /**
   * Performs a file migration of a given numbersList and country code and sends the details of the migration to the
   * jsp file which outputs it to the user.
   *
   * @throws RuntimeException, which can be any of the given exceptions when going through the process of creating a {@link
   * MigrationJob} and running a migration which includes IllegalArgumentExceptions.
   */
  public void handleFileMigration(HttpServletRequest req, HttpServletResponse resp,
      ImmutableList<String> numbersList,
      String countryCode,
      String customRecipe,
      String fileName)
      throws ServletException, IOException {

    if (customRecipe.isEmpty() && LARGE_COUNTRY_RANGES.contains(countryCode)) {
      req.setAttribute("fileError", "'+" + countryCode + "' migrations cannot be performed using this web" +
              " application, please follow the documentation link above to use the command line tool instead.");
      req.getRequestDispatcher("index.jsp").forward(req, resp);
      return;
    }

    MigrationJob job;
    try {
      if (!customRecipe.isEmpty()) {
        job = MigrationFactory.createCustomRecipeMigration(numbersList, countryCode, MigrationFactory
                .importRecipes(IOUtils.toInputStream(customRecipe)));
      } else {
        job = MigrationFactory.createMigration(numbersList, countryCode, /* exportInvalidMigrations= */ false);
      }

      MigrationJob.MigrationReport report = job.getMigrationReportForCountry();
      // List converted into a Set to allow for constant time contains() method below
      ImmutableSet<MigrationEntry> validUntouchedEntriesSet = ImmutableSet.copyOf(report.getValidUntouchedEntries());

      req.setAttribute("fileName", fileName);
      req.setAttribute("fileContent", report.toString());
      req.setAttribute("fileCountryCode", report.getCountryCode());
      req.setAttribute("validMigrations", report.getValidMigrations());
      req.setAttribute("invalidMigrations", report.getInvalidMigrations());
      req.setAttribute("validUntouchedNumbers", report.getValidUntouchedEntries());
      req.setAttribute("invalidUntouchedNumbers", report.getUntouchedEntries().stream()
              .filter(entry -> !validUntouchedEntriesSet.contains(entry)).collect(ImmutableList.toImmutableList()));

    } catch (RuntimeException e) {
      req.setAttribute("fileError", e.getMessage());

    } finally {
      req.getRequestDispatcher("index.jsp").forward(req, resp);
    }
  }

  /**
   * Takes a list of {@link MigrationEntry}'s and returns a list with the corresponding strings to output for each value
   * in the given entryList
   */
  public static ImmutableList<String> getMigrationEntryOutputList(ImmutableList<MigrationEntry> entryList) {
    if (entryList == null) return ImmutableList.of();
    return entryList.stream().map(MigrationEntry::getOriginalNumber).collect(ImmutableList.toImmutableList());
  }

  /**
   * Takes a list of {@link MigrationResult}'s and returns a list with the corresponding strings to output for each value
   * in the given resultList
   */
  public static ImmutableList<String> getMigrationResultOutputList(ImmutableList<MigrationResult> resultList) {
    if (resultList == null) return ImmutableList.of();
    return resultList.stream().map(MigrationResult::toString).collect(ImmutableList.toImmutableList());
  }
}
