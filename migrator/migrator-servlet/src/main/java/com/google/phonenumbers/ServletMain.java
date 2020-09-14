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

import com.google.phonenumbers.migrator.MigrationFactory;
import com.google.phonenumbers.migrator.MigrationJob;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "Migrate", value = "/migrate")
public class ServletMain extends HttpServlet {

  /**
   * Retrieves the form data for either a single number migration or a file migration from the index.jsp file and calls
   * the relevant method to perform the migration
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String number = req.getParameter("number");
    String countryCode;
    if (number != null) {
      countryCode = req.getParameter("numberCountryCode");
      req.setAttribute("number", number);
      req.setAttribute("numberCountryCode", countryCode);
      handleSingleNumberMigration(number, countryCode, req, resp);
    } else {
      String file = req.getParameter("file");
      countryCode = req.getParameter("fileCountryCode");
    }
  }

  /**
   * Performs a single number migration of a given number and country code and sends the details of the migration to the
   * jsp file which outputs it to the user.
   *
   * @throws Exception, which can be any of the given exceptions when going through the process of creating a {@link
   * MigrationJob} and running a migration.
   */
  public void handleSingleNumberMigration(String number, String countryCode, HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {
    try {
      MigrationJob mj = MigrationFactory.createMigration(number, countryCode);
      MigrationJob.MigrationReport mr = mj.getMigrationReportForCountry();
      if (mr.getValidMigrations().size() == 1) {
        req.setAttribute("validMigration", mr.getValidMigrations().get(0).getMigratedNumber());
      } else if (mr.getInvalidMigrations().size() == 1) {
        req.setAttribute("invalidMigration", mr.getInvalidMigrations().get(0).getMigratedNumber());
      } else if (mr.getValidUntouchedEntries().size() == 1) {
        req.setAttribute("alreadyValidNumber", mr.getValidUntouchedEntries().get(0).getSanitizedNumber());
      }

    } catch (Exception e) {
      req.setAttribute("numberError", e.getMessage());

    } finally {
      req.getRequestDispatcher("index.jsp").forward(req, resp);
    }
  }
}
