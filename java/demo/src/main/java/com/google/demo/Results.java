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

package com.google.demo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

import com.google.demo.helper.TemplateHelper;
import com.google.demo.template.ResultErrorTemplates;
import com.google.demo.template.ResultTemplates.SingleNumber;
import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.ShortNumberInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
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
public class Results extends HttpServlet {

  private static final String NEW_ISSUE_BASE_URL =
      "https://issuetracker.google.com/issues/new?component=192347&title=";
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private final ShortNumberInfo shortInfo = ShortNumberInfo.getInstance();

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

    StringBuilder output;
    resp.setContentType("text/html");
    resp.setCharacterEncoding(UTF_8.name());
    if (fileContents == null || fileContents.length() == 0) {
      // Redirect to a URL with the given input encoded in the query parameters.
      Locale geocodingLocale = new Locale(languageCode, regionCode);
      resp.sendRedirect(
          getPermaLinkURL(phoneNumber, defaultCountry, geocodingLocale, false /* absoluteURL */));
    } else {
      resp.getWriter().println(getOutputForFile(defaultCountry, fileContents));
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

  private StringBuilder getOutputForFile(String defaultCountry, String fileContents) {
    StringBuilder output =
        new StringBuilder(
            "<HTML><HEAD><TITLE>Results generated from phone numbers in the file provided:"
                + "</TITLE></HEAD><BODY>");
    output.append("<TABLE align=center border=1>");
    output.append("<TH align=center>ID</TH>");
    output.append("<TH align=center>Raw phone number</TH>");
    output.append("<TH align=center>Pretty formatting</TH>");
    output.append("<TH align=center>International format</TH>");

    int phoneNumberId = 0;
    StringTokenizer tokenizer = new StringTokenizer(fileContents, ",");
    while (tokenizer.hasMoreTokens()) {
      String numberStr = tokenizer.nextToken();
      phoneNumberId++;
      output.append("<TR>");
      output.append("<TD align=center>").append(phoneNumberId).append(" </TD> \n");
      output
          .append("<TD align=center>")
          .append(StringEscapeUtils.escapeHtml(numberStr))
          .append(" </TD> \n");
      try {
        PhoneNumber number = phoneUtil.parseAndKeepRawInput(numberStr, defaultCountry);
        boolean isNumberValid = phoneUtil.isValidNumber(number);
        String prettyFormat =
            isNumberValid ? phoneUtil.formatInOriginalFormat(number, defaultCountry) : "invalid";
        String internationalFormat =
            isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.INTERNATIONAL) : "invalid";

        output
            .append("<TD align=center>")
            .append(StringEscapeUtils.escapeHtml(prettyFormat))
            .append(" </TD> \n");
        output
            .append("<TD align=center>")
            .append(StringEscapeUtils.escapeHtml(internationalFormat))
            .append(" </TD> \n");
      } catch (NumberParseException e) {
        output
            .append("<TD align=center colspan=2>")
            .append(StringEscapeUtils.escapeHtml(e.toString()))
            .append(" </TD> \n");
      }
      output.append("</TR>");
    }
    output.append("</BODY></HTML>");
    return output;
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

  /**
   * The defaultCountry here is used for parsing phoneNumber. The geocodingLocale is used to specify
   * the language used for displaying the area descriptions generated from phone number geocoding.
   */
  private String getOutputForSingleNumber(
      String phoneNumber, String defaultCountry, Locale geocodingLocale) {

    // Header info at Start of Page
    SingleNumber.Builder soyTemplate =
        SingleNumber.builder()
            .setPhoneNumber(phoneNumber)
            .setDefaultCountry(defaultCountry)
            .setGeocodingLocale(geocodingLocale.toLanguageTag());
    try {
      PhoneNumber number = phoneUtil.parseAndKeepRawInput(phoneNumber, defaultCountry);
      soyTemplate
          .setCountryCode(number.getCountryCode())
          .setNationalNumber(number.getNationalNumber())
          .setExtension(number.getExtension())
          .setCountryCodeSource(number.getCountryCodeSource().toString())
          .setItalianLeadingZero(number.isItalianLeadingZero())
          .setRawInput(number.getRawInput());

      boolean isNumberValid = phoneUtil.isValidNumber(number);
      boolean hasDefaultCountry = !defaultCountry.isEmpty() && !defaultCountry.equals("ZZ");

      // Validation Results Table
      soyTemplate
          .setIsPossibleNumber(phoneUtil.isPossibleNumber(number))
          .setIsValidNumber(isNumberValid)
          .setIsValidNumberForRegion(
              isNumberValid && hasDefaultCountry
                  ? phoneUtil.isValidNumberForRegion(number, defaultCountry)
                  : null)
          .setPhoneNumberRegion(phoneUtil.getRegionCodeForNumber(number))
          .setNumberType(phoneUtil.getNumberType(number).toString())
          .setValidationResult(phoneUtil.isPossibleNumberWithReason(number).toString());

      // Short Number Results Table
      soyTemplate
          .setIsPossibleShortNumber(shortInfo.isPossibleShortNumber(number))
          .setIsValidShortNumber(shortInfo.isValidShortNumber(number))
          .setIsPossibleShortNumberForRegion(
              hasDefaultCountry
                  ? shortInfo.isPossibleShortNumberForRegion(number, defaultCountry)
                  : null)
          .setIsValidShortNumberForRegion(
              hasDefaultCountry
                  ? shortInfo.isValidShortNumberForRegion(number, defaultCountry)
                  : null);

      // Formatting Results Table
      soyTemplate
          .setE164Format(
              isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.E164) : "invalid")
          .setOriginalFormat(phoneUtil.formatInOriginalFormat(number, defaultCountry))
          .setNationalFormat(phoneUtil.format(number, PhoneNumberFormat.NATIONAL))
          .setInternationalFormat(
              isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.INTERNATIONAL) : "invalid")
          .setOutOfCountryFormatFromUs(
              isNumberValid ? phoneUtil.formatOutOfCountryCallingNumber(number, "US") : "invalid")
          .setOutOfCountryFormatFromCh(
              isNumberValid ? phoneUtil.formatOutOfCountryCallingNumber(number, "CH") : "invalid");

      List<List<String>> rows = new ArrayList<>();
      AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(defaultCountry);
      int rawNumberLength = phoneNumber.length();

      for (int i = 0; i < rawNumberLength; i++) {
        // Note this doesn't handle supplementary characters, but it shouldn't be a big deal as
        // there are no dial-pad characters in the supplementary range.
        char inputChar = phoneNumber.charAt(i);
        rows.add(
            List.of("Char entered: '" + inputChar + "' Output: ", formatter.inputDigit(inputChar)));
      }

      soyTemplate.setRows(rows);

    } catch (NumberParseException e) {
      return TemplateHelper.templateToString(
          "result_error.soy",
          "demo.output",
          ResultErrorTemplates.SingleNumber.builder()
              .setPhoneNumber(phoneNumber)
              .setDefaultCountry(defaultCountry)
              .setGeocodingLocale(geocodingLocale.toLanguageTag())
              .setError(e.toString())
              .build());
    }
    return TemplateHelper.templateToString("result.soy", "demo.output", soyTemplate.build());
  }
}
