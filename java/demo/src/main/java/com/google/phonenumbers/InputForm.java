/*
 * Copyright (C) 2011 The Libphonenumber Authors
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

package com.google.phonenumbers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.PhoneNumberUtil.ValidationResult;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.ShortNumberInfo;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.tofu.SoyTofu;
import example.HelloWorld;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * A servlet that accepts requests that contain strings representing a phone number and a default
 * country, and responds with results from parsing, validating and formatting the number. The
 * default country is a two-letter region code representing the country that we are expecting the
 * number to be from.
 */
@SuppressWarnings("serial")
public class InputForm extends HttpServlet {
  private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private ShortNumberInfo shortInfo = ShortNumberInfo.getInstance();


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
    SoyFileSet sfs = SoyFileSet
        .builder()
        .add(HelloWorld.class.getResource("simple.soy"))
        .build();
    SoyTofu tofu = sfs.compileToTofu();
    resp.getWriter()
        .println(
            tofu.newRenderer("examples.simple.helloWorld").render());
  }


  private void appendLine(String title, String data, StringBuilder output) {
    output.append("<TR>");
    output.append("<TH>").append(title).append("</TH>");
    output.append("<TD>").append(data.length() > 0 ? data : "&nbsp;").append("</TD>");
    output.append("</TR>");
  }

  /** Returns a stable URL pointing to the result page for the given input. */
  private String getPermaLinkURL(
      String phoneNumber, String defaultCountry, Locale geocodingLocale, boolean absoluteURL) {
    // If absoluteURL is false, generate a relative path. Otherwise, produce an absolute URL.
    StringBuilder permaLink =
        new StringBuilder(
            absoluteURL
                ? "http://libphonenumber.appspot.com/phonenumberparser"
                : "/phonenumberparser");
    try {
      permaLink.append(
          "?number=" + URLEncoder.encode(phoneNumber != null ? phoneNumber : "", UTF_8.name()));
      if (defaultCountry != null && !defaultCountry.isEmpty()) {
        permaLink.append("&country=" + URLEncoder.encode(defaultCountry, UTF_8.name()));
      }
      if (!geocodingLocale.getLanguage().equals(ENGLISH.getLanguage())
          || !geocodingLocale.getCountry().isEmpty()) {
        permaLink.append(
            "&geocodingLocale=" + URLEncoder.encode(geocodingLocale.toLanguageTag(), UTF_8.name()));
      }
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is guaranteed in Java, so this should be impossible.
      throw new AssertionError(e);
    }
    return permaLink.toString();
  }

  private static final String NEW_ISSUE_BASE_URL =
      "https://issuetracker.google.com/issues/new?component=192347&title=";

  /** Returns a link to create a new github issue with the relevant information. */
  private String getNewIssueLink(
      String phoneNumber, String defaultCountry, Locale geocodingLocale) {
    boolean hasDefaultCountry = !defaultCountry.isEmpty() && defaultCountry != "ZZ";
    String issueTitle =
        "Validation issue with "
            + phoneNumber
            + (hasDefaultCountry ? " (" + defaultCountry + ")" : "");

    String newIssueLink = NEW_ISSUE_BASE_URL;
    try {
      newIssueLink += URLEncoder.encode(issueTitle, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is guaranteed in Java, so this should be impossible.
      throw new AssertionError(e);
    }
    return newIssueLink;
  }


}
