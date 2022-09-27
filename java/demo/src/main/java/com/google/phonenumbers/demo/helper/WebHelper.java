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

package com.google.phonenumbers.demo.helper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class WebHelper {
  private static final String NEW_ISSUE_BASE_URL =
      "https://issuetracker.google.com/issues/new?component=192347&title=";
  /** Returns a stable URL pointing to the result page for the given input. */
  public static String getPermaLinkURL(
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
  public static String getNewIssueLink(
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
