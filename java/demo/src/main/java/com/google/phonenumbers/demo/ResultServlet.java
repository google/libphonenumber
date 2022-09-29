/*
 * Copyright (C) 2022 The Libphonenumber Authors
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
 *
 * @author Shaopeng Jia
 */

package com.google.phonenumbers.demo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.phonenumbers.demo.helper.WebHelper;
import com.google.phonenumbers.demo.render.ErrorRenderer;
import com.google.phonenumbers.demo.render.ResultFileRenderer;
import com.google.phonenumbers.demo.render.ResultRenderer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;

/**
 * A servlet that accepts requests that contain strings representing a phone number and a default
 * country, and responds with results from parsing, validating and formatting the number. The
 * default country is a two-letter region code representing the country that we are expecting the
 * number to be from.
 */
@SuppressWarnings("serial")
public class ResultServlet extends HttpServlet {

  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String phoneNumber = null;
    String defaultCountry = null;
    String languageCode = "en"; // Default languageCode to English if nothing is entered.
    String regionCode = "";
    String fileContents = null;
    ServletFileUpload upload = new ServletFileUpload();
    upload.setSizeMax(50000);
    try {
      FileItemIterator iterator = upload.getItemIterator(req);
      while (iterator.hasNext()) {
        FileItemStream item = iterator.next();
        InputStream in = item.openStream();
        if (item.isFormField()) {
          String fieldName = item.getFieldName();
          if (fieldName.equals("phoneNumber")) {
            phoneNumber = Streams.asString(in, UTF_8.name());
          } else if (fieldName.equals("defaultCountry")) {
            defaultCountry = Streams.asString(in).toUpperCase();
          } else if (fieldName.equals("languageCode")) {
            String languageEntered = Streams.asString(in).toLowerCase();
            if (languageEntered.length() > 0) {
              languageCode = languageEntered;
            }
          } else if (fieldName.equals("regionCode")) {
            regionCode = Streams.asString(in).toUpperCase();
          }
        } else {
          try {
            fileContents = IOUtils.toString(in);
          } finally {
            IOUtils.closeQuietly(in);
          }
        }
      }
    } catch (FileUploadException e1) {
      e1.printStackTrace();
    }

    resp.setContentType("text/html");
    resp.setCharacterEncoding(UTF_8.name());
    if (fileContents == null || fileContents.length() == 0) {
      // Redirect to a URL with the given input encoded in the query parameters.
      Locale geocodingLocale = new Locale(languageCode, regionCode);
      resp.sendRedirect(
          WebHelper.getPermaLinkURL(
              phoneNumber, defaultCountry, geocodingLocale, false /* absoluteURL */));
    } else {
      resp.getWriter().println(new ResultFileRenderer(defaultCountry, fileContents).genHtml());
    }
  }

  /** Handle the get request to get information about a number based on query parameters. */
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String phoneNumber = req.getParameter("number");
    if (phoneNumber == null) {
      phoneNumber = "";
    }
    String defaultCountry = req.getParameter("country");
    if (defaultCountry == null) {
      defaultCountry = "";
    }
    String geocodingParam = req.getParameter("geocodingLocale");
    Locale geocodingLocale;
    if (geocodingParam == null) {
      geocodingLocale = ENGLISH; // Default languageCode to English if nothing is entered.
    } else {
      geocodingLocale = Locale.forLanguageTag(geocodingParam);
    }
    resp.setContentType("text/html");
    resp.setCharacterEncoding(UTF_8.name());
    resp.getWriter()
        .println(getOutputForSingleNumber(phoneNumber, defaultCountry, geocodingLocale));
  }

  /**
   * The defaultCountry here is used for parsing phoneNumber. The geocodingLocale is used to specify
   * the language used for displaying the area descriptions generated from phone number geocoding.
   */
  private String getOutputForSingleNumber(
      String phoneNumber, String defaultCountry, Locale geocodingLocale) {
    try {
      PhoneNumber number = phoneUtil.parseAndKeepRawInput(phoneNumber, defaultCountry);
      return new ResultRenderer(phoneNumber, defaultCountry, geocodingLocale, number).genHtml();
    } catch (NumberParseException e) {
      return new ErrorRenderer(phoneNumber, defaultCountry, geocodingLocale, e.toString())
          .genHtml();
    }
  }
}
