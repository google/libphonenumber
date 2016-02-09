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
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.ShortNumberInfo;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that accepts requests that contain strings representing a phone number and a default
 * country, and responds with results from parsing, validating and formatting the number. The
 * default country is a two-letter region code representing the country that we are expecting the
 * number to be from.
 */
@SuppressWarnings("serial")
public class PhoneNumberParserServlet extends HttpServlet {
  private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private ShortNumberInfo shortInfo = ShortNumberInfo.getInstance();
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String phoneNumber = null;
    String defaultCountry = null;
    String languageCode = "en";  // Default languageCode to English if nothing is entered.
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
      resp.sendRedirect(getPermaLinkURL(phoneNumber, defaultCountry, geocodingLocale,
          false /* absoluteURL */));
    } else {
      resp.getWriter().println(getOutputForFile(defaultCountry, fileContents));
    }
  }

  /**
   * Handle the get request to get information about a number based on query parameters.
   */
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
      geocodingLocale = ENGLISH;  // Default languageCode to English if nothing is entered.
    } else {
      geocodingLocale = Locale.forLanguageTag(geocodingParam);
    }
    resp.getWriter().println(
        getOutputForSingleNumber(phoneNumber, defaultCountry, geocodingLocale));
  }

  private StringBuilder getOutputForFile(String defaultCountry, String fileContents) {
    StringBuilder output = new StringBuilder(
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
      output.append("<TD align=center>").append(
          StringEscapeUtils.escapeHtml(numberStr)).append(" </TD> \n");
      try {
        PhoneNumber number = phoneUtil.parseAndKeepRawInput(numberStr, defaultCountry);
        boolean isNumberValid = phoneUtil.isValidNumber(number);
        String prettyFormat = isNumberValid
            ? phoneUtil.formatInOriginalFormat(number, defaultCountry)
            : "invalid";
        String internationalFormat = isNumberValid
            ? phoneUtil.format(number, PhoneNumberFormat.INTERNATIONAL)
            : "invalid";

        output.append("<TD align=center>").append(
            StringEscapeUtils.escapeHtml(prettyFormat)).append(" </TD> \n");
        output.append("<TD align=center>").append(
            StringEscapeUtils.escapeHtml(internationalFormat)).append(" </TD> \n");
      } catch (NumberParseException e) {
        output.append("<TD align=center colspan=2>").append(
            StringEscapeUtils.escapeHtml(e.toString())).append(" </TD> \n");
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

  /**
   * Returns a stable URL pointing to the result page for the given input.
   */
  private String getPermaLinkURL(
      String phoneNumber, String defaultCountry, Locale geocodingLocale, boolean absoluteURL) {
    // If absoluteURL is false, generate a relative path. Otherwise, produce an absolute URL.
    StringBuilder permaLink = new StringBuilder(
        absoluteURL ? "http://libphonenumber.appspot.com/phonenumberparser" : "/phonenumberparser");
    try {
      permaLink.append(
          "?number=" + URLEncoder.encode(phoneNumber != null ? phoneNumber : "", UTF_8.name()));
      if (defaultCountry != null && !defaultCountry.isEmpty()) {
        permaLink.append("&country=" + URLEncoder.encode(defaultCountry, UTF_8.name()));
      }
      if (!geocodingLocale.getLanguage().equals(ENGLISH.getLanguage()) ||
          !geocodingLocale.getCountry().isEmpty()) {
        permaLink.append("&geocodingLocale=" +
            URLEncoder.encode(geocodingLocale.toLanguageTag(), UTF_8.name()));
      }
    } catch(UnsupportedEncodingException e) {
      // UTF-8 is guaranteed in Java, so this should be impossible.
      throw new AssertionError(e);
    }
    return permaLink.toString();
  }

  /**
   * Returns a link to create a new github issue with the relevant information.
   */
  private String getNewIssueLink(
      String phoneNumber, String defaultCountry, Locale geocodingLocale) {
    boolean hasDefaultCountry = !defaultCountry.isEmpty() && defaultCountry != "ZZ";
    String issueTitle = "Validation issue with " + phoneNumber
        + (hasDefaultCountry ? " (" + defaultCountry + ")" : "");

    // Issue template. This must be kept in sync with the template in
    // https://github.com/googlei18n/libphonenumber/blob/master/CONTRIBUTING.md.
    StringBuilder issueTemplate = new StringBuilder(
        "Please read the \"guidelines for contributing\" (linked above) and fill "
        + "in the template below.\n\n");
    issueTemplate.append("Country/region affected (e.g., \"US\"): ")
        .append(defaultCountry).append("\n\n");
    issueTemplate.append("Example number(s) affected (\"+1 555 555-1234\"): ")
        .append(phoneNumber).append("\n\n");
    issueTemplate.append(
        "The phone number range(s) to which the issue applies (\"+1 555 555-XXXX\"): \n\n");
    issueTemplate.append(
        "The type of the number(s) (\"fixed-line\", \"mobile\", \"short code\", etc.): \n\n");
    issueTemplate.append(
        "The cost, if applicable (\"toll-free\", \"premium rate\", \"shared cost\"): \n\n");
    issueTemplate.append(
        "Supporting evidence (for example, national numbering plan, announcement from mobile "
        + "carrier, news article): **IMPORTANT - anything posted here is made public. "
        + "Read the guidelines first!** \n\n");
    issueTemplate.append("[link to demo]("
        + getPermaLinkURL(phoneNumber, defaultCountry, geocodingLocale, true /* absoluteURL */)
        + ")\n\n");
    String newIssueLink = "https://github.com/googlei18n/libphonenumber/issues/new?title=";
    try {
      newIssueLink += URLEncoder.encode(issueTitle, UTF_8.name()) + "&body="
        + URLEncoder.encode(issueTemplate.toString(), UTF_8.name());
    } catch(UnsupportedEncodingException e) {
      // UTF-8 is guaranteed in Java, so this should be impossible.
      throw new AssertionError(e);
    }
    return newIssueLink;
  }

  /**
   * The defaultCountry here is used for parsing phoneNumber. The geocodingLocale is used to specify
   * the language used for displaying the area descriptions generated from phone number geocoding.
   */
  private StringBuilder getOutputForSingleNumber(
      String phoneNumber, String defaultCountry, Locale geocodingLocale) {
    StringBuilder output = new StringBuilder("<HTML><HEAD>");
    output.append(
        "<LINK type=\"text/css\" rel=\"stylesheet\" href=\"/stylesheets/main.css\" />");
    output.append("</HEAD>");
    output.append("<BODY>");
    output.append("Phone Number entered: " + StringEscapeUtils.escapeHtml(phoneNumber) + "<BR>");
    output.append("defaultCountry entered: " + StringEscapeUtils.escapeHtml(defaultCountry)
        + "<BR>");
    output.append("Language entered: "
        + StringEscapeUtils.escapeHtml(geocodingLocale.toLanguageTag()) + "<BR>");
    try {
      PhoneNumber number = phoneUtil.parseAndKeepRawInput(phoneNumber, defaultCountry);
      output.append("<DIV>");
      output.append("<TABLE border=1>");
      output.append("<TR><TD colspan=2>Parsing Result</TD></TR>");

      appendLine("country_code", Integer.toString(number.getCountryCode()), output);
      appendLine("national_number", Long.toString(number.getNationalNumber()), output);
      appendLine("extension", number.getExtension(), output);
      appendLine("country_code_source", number.getCountryCodeSource().toString(), output);
      appendLine("italian_leading_zero", Boolean.toString(number.isItalianLeadingZero()), output);
      appendLine("raw_input", number.getRawInput(), output);
      output.append("</TABLE>");
      output.append("</DIV>");

      boolean isPossible = phoneUtil.isPossibleNumber(number);
      boolean isNumberValid = phoneUtil.isValidNumber(number);
      PhoneNumberType numberType = phoneUtil.getNumberType(number);
      boolean hasDefaultCountry = !defaultCountry.isEmpty() && defaultCountry != "ZZ";

      output.append("<DIV>");
      output.append("<TABLE border=1>");
      output.append("<TR><TD colspan=2>Validation Results</TD></TR>");
      appendLine("Result from isPossibleNumber()", Boolean.toString(isPossible), output);
      if (!isPossible) {
        appendLine("Result from isPossibleNumberWithReason()",
                   phoneUtil.isPossibleNumberWithReason(number).toString(), output);
        output.append("<TR><TD colspan=2>Note: numbers that are not possible have type " +
                      "UNKNOWN, an unknown region, and are considered invalid.</TD></TR>");
      } else {
        appendLine("Result from isValidNumber()", Boolean.toString(isNumberValid), output);
        if (isNumberValid) {
          if (hasDefaultCountry) {
            appendLine(
                "Result from isValidNumberForRegion()",
                Boolean.toString(phoneUtil.isValidNumberForRegion(number, defaultCountry)),
                output);
          }
        }
        String region = phoneUtil.getRegionCodeForNumber(number);
        appendLine("Phone Number region", region == null ? "" : region, output);
        appendLine("Result from getNumberType()", numberType.toString(), output);
      }
      output.append("</TABLE>");
      output.append("</DIV>");

      if (!isNumberValid) {
        output.append("<DIV>");
        output.append("<TABLE border=1>");
        output.append("<TR><TD colspan=2>Short Number Results</TD></TR>");
        boolean isPossibleShort = shortInfo.isPossibleShortNumber(number);
        appendLine("Result from isPossibleShortNumber()",
            Boolean.toString(isPossibleShort), output);
        if (isPossibleShort) {
          appendLine("Result from isValidShortNumber()",
              Boolean.toString(shortInfo.isValidShortNumber(number)), output);
          if (hasDefaultCountry) {
            boolean isPossibleShortForRegion =
                shortInfo.isPossibleShortNumberForRegion(number, defaultCountry);
            appendLine("Result from isPossibleShortNumberForRegion()",
                Boolean.toString(isPossibleShortForRegion), output);
            if (isPossibleShortForRegion) {
              appendLine("Result from isValidShortNumberForRegion()",
                  Boolean.toString(shortInfo.isValidShortNumberForRegion(number,
                      defaultCountry)), output);
            }
          }
        }
        output.append("</TABLE>");
        output.append("</DIV>");
      }

      output.append("<DIV>");
      output.append("<TABLE border=1>");
      output.append("<TR><TD colspan=2>Formatting Results</TD></TR>");
      appendLine("E164 format",
                 isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.E164) : "invalid",
                 output);
      appendLine("Original format",
                 phoneUtil.formatInOriginalFormat(number, defaultCountry), output);
      appendLine("National format", phoneUtil.format(number, PhoneNumberFormat.NATIONAL), output);
      appendLine(
          "International format",
          isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.INTERNATIONAL) : "invalid",
          output);
      appendLine(
          "Out-of-country format from US",
          isNumberValid ? phoneUtil.formatOutOfCountryCallingNumber(number, "US") : "invalid",
          output);
      appendLine(
          "Out-of-country format from CH",
          isNumberValid ? phoneUtil.formatOutOfCountryCallingNumber(number, "CH") : "invalid",
          output);
      output.append("</TABLE>");
      output.append("</DIV>");

      AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(defaultCountry);
      int rawNumberLength = phoneNumber.length();
      output.append("<DIV>");
      output.append("<TABLE border=1>");
      output.append("<TR><TD colspan=2>AsYouTypeFormatter Results</TD></TR>");
      for (int i = 0; i < rawNumberLength; i++) {
        // Note this doesn't handle supplementary characters, but it shouldn't be a big deal as
        // there are no dial-pad characters in the supplementary range.
        char inputChar = phoneNumber.charAt(i);
        appendLine("Char entered: '" + inputChar + "' Output: ",
                   formatter.inputDigit(inputChar), output);
      }
      output.append("</TABLE>");
      output.append("</DIV>");

      if (isNumberValid) {
        output.append("<DIV>");
        output.append("<TABLE border=1>");
        output.append("<TR><TD colspan=2>PhoneNumberOfflineGeocoder Results</TD></TR>");
        appendLine(
            "Location",
            PhoneNumberOfflineGeocoder.getInstance().getDescriptionForNumber(
                number, geocodingLocale),
            output);
        output.append("</TABLE>");
        output.append("</DIV>");

        output.append("<DIV>");
        output.append("<TABLE border=1>");
        output.append("<TR><TD colspan=2>PhoneNumberToTimeZonesMapper Results</TD></TR>");
        appendLine(
            "Time zone(s)",
            PhoneNumberToTimeZonesMapper.getInstance().getTimeZonesForNumber(number).toString(),
            output);
        output.append("</TABLE>");
        output.append("</DIV>");

        if (numberType == PhoneNumberType.MOBILE ||
            numberType == PhoneNumberType.FIXED_LINE_OR_MOBILE ||
            numberType == PhoneNumberType.PAGER) {
          output.append("<DIV>");
          output.append("<TABLE border=1>");
          output.append("<TR><TD colspan=2>PhoneNumberToCarrierMapper Results</TD></TR>");
          appendLine(
              "Carrier",
              PhoneNumberToCarrierMapper.getInstance().getNameForNumber(number, geocodingLocale),
              output);
          output.append("</TABLE>");
          output.append("</DIV>");
        }
      }

      String newIssueLink = getNewIssueLink(phoneNumber, defaultCountry, geocodingLocale);
      String guidelinesLink =
          "https://github.com/googlei18n/libphonenumber/blob/master/CONTRIBUTING.md";
      output.append("<b style=\"color:red\">File an issue</b>: by clicking on "
          + "<a target=\"_blank\" href=\"" + newIssueLink + "\">this link</a>, I confirm that I "
          + "have read the <a target=\"_blank\" href=\"" + guidelinesLink
          + "\">contributor's guidelines</a>.");
    } catch (NumberParseException e) {
      output.append(StringEscapeUtils.escapeHtml(e.toString()));
    }
    output.append("</BODY></HTML>");
    return output;
  }
}
