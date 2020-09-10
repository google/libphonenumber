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

import com.google.appengine.api.utils.SystemProperty;

import com.google.i18n.phonenumbers.metadata.DigitSequence;
//import com.google.phonenumbers.migrator.MigrationFactory;
//import com.google.phonenumbers.migrator.MigrationJob;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "HelloWorld", value = "/hello")
public class HelloWorld extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
          throws IOException {

    Properties properties = System.getProperties();

    response.setContentType("text/plain");
    response.getWriter().println("Hello App Engine - Standard using "
            + SystemProperty.version.get() + " Java " + properties.get("java.specification.version"));
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setContentType("text/html");


    PrintWriter out = resp.getWriter();
    String number = req.getParameter("number");
    String countryCode = req.getParameter("countryCode");

    DigitSequence a = DigitSequence.of(number);
//    MigrationJob m = MigrationFactory.createMigration(number, countryCode);

//    MigrationJob.MigrationReport r = m.getMigrationReportForCountry();

    out.println("<html><body>");
    out.println("Number = " + number);
    out.println("Country Code = " + countryCode);
    out.println(a.toString());
//    r.getValidMigrations().forEach(out::println);
    out.println("</body></html>");
  }

  public static String getInfo() {
    return "Version: " + System.getProperty("java.version")
            + " OS: " + System.getProperty("os.name")
            + " User: " + System.getProperty("user.name");
  }

}