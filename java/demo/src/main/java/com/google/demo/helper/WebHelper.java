package com.google.demo.helper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class WebHelper {
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
}
