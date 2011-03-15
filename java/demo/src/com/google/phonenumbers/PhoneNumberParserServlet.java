/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
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
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String phoneNumber = null;
    String defaultCountry = null;
    String fileContents = null;
    ServletFileUpload upload = new ServletFileUpload();
    upload.setSizeMax(50000);
    try {
      FileItemIterator iterator = upload.getItemIterator(req);
      while (iterator.hasNext()) {
        FileItemStream item = iterator.next();
        InputStream in = item.openStream();
        if (item.isFormField()) {
          if (item.getFieldName().equals("phoneNumber")) {
            phoneNumber = Streams.asString(in, "UTF-8");
          } else if (item.getFieldName().equals("defaultCountry")) {
            defaultCountry = Streams.asString(in);
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

    StringBuffer output;
    if (fileContents.length() == 0) {
      output = getOutputForSingleNumber(phoneNumber, defaultCountry);
      resp.setContentType("text/plain");
      resp.getWriter().println("Phone Number entered: " + phoneNumber);
      resp.getWriter().println("defaultCountry entered: " + defaultCountry);
    } else {
      output = getOutputForFile(defaultCountry, fileContents);
      resp.setContentType("text/html");
    }
    resp.getWriter().println(output);
  }

  private StringBuffer getOutputForFile(String defaultCountry, String fileContents) {
    StringBuffer output = new StringBuffer();
    output.append("<HTML><HEAD><TITLE>Results generated from phone numbers in the file provided:" 
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
      output.append("<TD align=center>").append(numberStr).append(" </TD> \n");
      try {
        PhoneNumber number = phoneUtil.parseAndKeepRawInput(numberStr, defaultCountry);
        boolean isNumberValid = phoneUtil.isValidNumber(number);
        String prettyFormat = isNumberValid
            ? phoneUtil.formatInOriginalFormat(number, defaultCountry)
            : "invalid";
        String internationalFormat = isNumberValid
            ? phoneUtil.format(number, PhoneNumberFormat.INTERNATIONAL)
            : "invalid";

        output.append("<TD align=center>").append(prettyFormat).append(" </TD> \n");
        output.append("<TD align=center>").append(internationalFormat).append(" </TD> \n");
      } catch (NumberParseException e) {
        output.append("<TD align=center colspan=2>").append(e.toString()).append(" </TD> \n");
      }
      output.append("</TR>");
    }
    output.append("</BODY></HTML>");
    return output;
  }

  private StringBuffer getOutputForSingleNumber(String phoneNumber, String defaultCountry) {
    StringBuffer output = new StringBuffer();
    try {
      PhoneNumber number = phoneUtil.parseAndKeepRawInput(phoneNumber, defaultCountry);
      output.append("\n\n****Parsing Result:****");
      output.append("\ncountry_code: ").append(number.getCountryCode());
      output.append("\nnational_number: ").append(number.getNationalNumber());
      output.append("\nextension:").append(number.getExtension());
      output.append("\ncountry_code_source: ").append(number.getCountryCodeSource());
      output.append("\nitalian_leading_zero: ").append(number.getItalianLeadingZero());
      output.append("\nraw_input: ").append(number.getRawInput());

      output.append("\n\n****Validation Results:****");
      boolean isNumberValid = phoneUtil.isValidNumber(number);
      output.append("\nResult from isValidNumber(): ").append(isNumberValid);
      output.append("\nResult from isValidNumberForRegion(): ").append(
          phoneUtil.isValidNumberForRegion(number, defaultCountry));
      output.append("\nPhone Number region: ").append(phoneUtil.getRegionCodeForNumber(number));
      output.append("\nResult from isPossibleNumber(): ").append(
          phoneUtil.isPossibleNumber(number));
      output.append("\nResult from getNumberType(): ").append(phoneUtil.getNumberType(number));

      output.append("\n\n****Formatting Results:**** ");
      output.append("\nE164 format: ").append(
          isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.E164) : "invalid");
      output.append("\nOriginal format: ").append(
          phoneUtil.formatInOriginalFormat(number, defaultCountry));
      output.append("\nInternational format: ").append(
          isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.INTERNATIONAL) : "invalid");
      output.append("\nNational format: ").append(
          phoneUtil.format(number, PhoneNumberFormat.NATIONAL));
      output.append("\nOut-of-country format from US: ").append(
          isNumberValid ? phoneUtil.formatOutOfCountryCallingNumber(number, "US") : "invalid");
      output.append("\n\n****AsYouTypeFormatter Results****");
      AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(defaultCountry);
      int rawNumberLength = phoneNumber.length();
      for (int i = 0; i < rawNumberLength; i++) {
        // Note this doesn't handle supplementary characters, but it shouldn't be a big deal as
        // there are no dial-pad characters in the supplementary range.
        char inputChar = phoneNumber.charAt(i);
        output.append("\nChar entered: ").append(inputChar).append(" Output: ")
            .append(formatter.inputDigit(inputChar));
      }
      output.append("\n\n");
    } catch (NumberParseException e) {
      output.append(e.toString());
    }
    return output;
  }
}
