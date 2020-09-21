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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.phonenumbers.migrator.MigrationEntry;
import com.google.phonenumbers.migrator.MigrationFactory;
import com.google.phonenumbers.migrator.MigrationJob;
import com.google.phonenumbers.migrator.MigrationResult;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
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
import java.util.Map;
import java.util.StringTokenizer;

@WebServlet(name = "Migrate", value = "/migrate")
public class ServletMain extends HttpServlet {

  /**
   * Retrieves the form data for either a single number migration or a file migration from the index.jsp file and calls
   * the relevant method to perform the migration
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String number = req.getParameter("number");
    if (number != null) {
      String countryCode = req.getParameter("numberCountryCode");
      /* number and country code are being set again to allow users to see their inputs after the http request has
        re-rendered the page. */
      req.setAttribute("number", number);
      req.setAttribute("numberCountryCode", countryCode);
      if (countryCode == null) {
        req.setAttribute("numberError", "please specify a country code to perform a migration");
        req.getRequestDispatcher("index.jsp").forward(req, resp);
      }
      handleSingleNumberMigration(number, countryCode, req, resp);
    } else {
      handleFileMigration(req, resp);
    }
  }

  /** Retrieves the form data for the numbers after a file migration and downloads them as a text file. */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String fileName = "+" + req.getParameter("countryCode") + "_Migration_ " + req.getParameter("fileName");
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
  public void handleSingleNumberMigration(String number, String countryCode, HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {
    try {
      MigrationJob job = MigrationFactory.createMigration(number, countryCode);
      MigrationJob.MigrationReport report = job.getMigrationReportForCountry();
      if (report.getValidMigrations().size() == 1) {
        req.setAttribute("validMigration", report.getValidMigrations().get(0).getMigratedNumber());
      } else if (report.getInvalidMigrations().size() == 1) {
        req.setAttribute("invalidMigration", report.getInvalidMigrations().get(0).getMigratedNumber());
      } else if (report.getValidUntouchedEntries().size() == 1) {
        req.setAttribute("alreadyValidNumber", report.getValidUntouchedEntries().get(0).getSanitizedNumber());
      }

    } catch (RuntimeException | IOException e) {
      req.setAttribute("numberError", e.getMessage());

    } finally {
      req.getRequestDispatcher("index.jsp").forward(req, resp);
    }
  }

  public static ImmutableList<String> getMigrationEntryOutputList(ImmutableList<MigrationEntry> entryList) {
    if (entryList == null) return ImmutableList.of();
    return entryList.stream().map(MigrationEntry::getOriginalNumber).collect(ImmutableList.toImmutableList());
  }

  public static ImmutableList<String> getMigrationResultOutputList(ImmutableList<MigrationResult> resultList) {
    if (resultList == null) return ImmutableList.of();
    return resultList.stream().map(MigrationResult::toString).collect(ImmutableList.toImmutableList());
  }

  public void handleFileMigration(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      Map.Entry<String, MigrationJob.MigrationReport> result = performFileMigration(req).entrySet().iterator().next();
      String fileName = result.getKey();
      MigrationJob.MigrationReport report = result.getValue();
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

    } catch (Exception e) {
      req.setAttribute("fileError", e.getMessage());

    } finally {
      req.getRequestDispatcher("index.jsp").forward(req, resp);
    }
  }

  private ImmutableMap<String, MigrationJob.MigrationReport> performFileMigration(HttpServletRequest req) throws Exception {
    String countryCode = "";
    String file = "";
    String fileName = "";
    ServletFileUpload upload = new ServletFileUpload();
    upload.setSizeMax(50000);

    FileItemIterator iterator = upload.getItemIterator(req);
    while (iterator.hasNext()) {
      FileItemStream item = iterator.next();
      InputStream in = item.openStream();
      if (item.isFormField()) {
        countryCode = Streams.asString(in).toUpperCase();
      } else {
        fileName = item.getName();
        try {
          file = IOUtils.toString(in);
        } finally {
          IOUtils.closeQuietly(in);
        }
      }
    }

    ImmutableList.Builder<String> numbersFromFile = ImmutableList.builder();
    StringTokenizer tokenizer = new StringTokenizer(file, "\n");
    while (tokenizer.hasMoreTokens()) {
      numbersFromFile.add(tokenizer.nextToken());
    }
    ImmutableList<String> res = numbersFromFile.build();

    MigrationJob mj = MigrationFactory.createMigration(res, countryCode, /* exportInvalidMigrations= */ false);
    return ImmutableMap.of(fileName, mj.getMigrationReportForCountry());
  }
}
