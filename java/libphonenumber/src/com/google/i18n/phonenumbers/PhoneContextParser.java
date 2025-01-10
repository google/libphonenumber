/*
 * Copyright (C) 2025 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers;

import java.util.Set;

/** Utility class for parsing the phone-context parameter of a phone number. */
final class PhoneContextParser {
  private final Set<Integer> countryCallingCodeSet;

  PhoneContextParser(Set<Integer> countryCallingCodeSet) {
    this.countryCallingCodeSet = countryCallingCodeSet;
  }

  /**
   * Extracts the value of the phone-context parameter of numberToExtractFrom, following the
   * syntax defined in RFC3966.
   *
   * @return the extracted string (possibly empty), or null if no phone-context parameter is
   *         found.
   */
  private static String extractPhoneContext(String number) {
    int indexOfPhoneContext = number.indexOf(Constants.RFC3966_PHONE_CONTEXT);

    // If no phone-context parameter is present
    if (indexOfPhoneContext == -1) {
      return null;
    }

    int phoneContextStart = indexOfPhoneContext + Constants.RFC3966_PHONE_CONTEXT.length();
    // If phone-context parameter is empty
    if (phoneContextStart >= number.length()) {
      return "";
    }

    int phoneContextEnd = number.indexOf(';', phoneContextStart);
    // If phone-context is the last parameter
    if (phoneContextEnd < 0) {
      return number.substring(phoneContextStart);
    } else {
      return number.substring(phoneContextStart, phoneContextEnd);
    }
  }

  /**
   * Returns whether the value of phoneContext follows the syntax defined in RFC3966.
   */
  private static boolean isValid(String phoneContext) {
    if (phoneContext.equals("")) {
      return false;
    }

    // Does phone-context value match pattern of global-number-digits or domain name
    return Constants.RFC3966_GLOBAL_NUMBER_DIGITS_PATTERN.matcher(phoneContext).matches()
        || Constants.RFC3966_DOMAINNAME_PATTERN.matcher(phoneContext).matches();
  }

  /** Checks if the int is a valid country calling code. */
  private boolean isValidCountryCode(int countryCode) {
    return countryCallingCodeSet.contains(countryCode);
  }

  /**
   * Parses the value of the phone-context parameter of number, following the syntax defined in
   * RFC3966.
   *
   * @return the parsed phone-context parameter as a PhoneContext object, or null if no
   *         phone-context parameter is found.
   */
  private PhoneContext parsePhoneContext(String phoneContext) {
    // Ignore phone-context values that do not start with a plus sign. Could be a domain name.
    if (phoneContext.charAt(0) != Constants.PLUS_SIGN) {
      return new PhoneContext().setRawContext(phoneContext).setCountryCode(null);
    }

    // Remove the plus sign from the phone context and normalize the digits.
    String normalizedPhoneContext =
        PhoneNumberNormalizer.normalizeDigitsOnly(phoneContext.substring(1));

    // Check if the phone context is a valid country calling code.
    if (!normalizedPhoneContext.equals("")
        && normalizedPhoneContext.length() <= Constants.MAX_LENGTH_COUNTRY_CODE) {
      int potentialCountryCode = Integer.parseInt(normalizedPhoneContext);
      if (isValidCountryCode(potentialCountryCode)) {
        return new PhoneContext().setRawContext(phoneContext).setCountryCode(potentialCountryCode);
      }
    }

    // If the country code is not valid, return the phone context as is.
    return new PhoneContext().setRawContext(phoneContext).setCountryCode(null);
  }

  /**
   * Parses the phone-context parameter of number, following the syntax defined in RFC3966.
   *
   * @return the parsed phone-context parameter as a PhoneContext object, or null if no
   *         phone-context parameter is found.
   * @throws NumberParseException if the phone-context parameter is invalid.
   */
  PhoneContext parse(String number) throws NumberParseException {
    String phoneContext = extractPhoneContext(number);

    if (phoneContext == null) {
      return null;
    }

    if (!isValid(phoneContext)) {
      throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER,
          "The phone-context value is invalid.");
    }

    return parsePhoneContext(phoneContext);
  }

  /** Represents the parsed phone-context parameter of an RFC3966 tel-URI. */
  static class PhoneContext {
    /** The raw value of the phone-context parameter. */
    private String rawContext_ = null;

    /**
     * The country code of the phone-context parameter if the phone-context parameter is exactly
     * and only a + followed by a valid country code.
     *
     * <p>
     * For example, if the phone-context parameter is "+1", the country code is 1. If the
     * phone-context parameter is "+123", the country code is null.
     */
    private Integer countryCode_ = null;

    /** Get the value for {@link #rawContext_} */
    String getRawContext() {
      return rawContext_;
    }

    /** Set the value for {@link #rawContext_} */
    PhoneContext setRawContext(String value) {
      rawContext_ = value;
      return this;
    }

    /** Get the value for {@link #countryCode_} */
    Integer getCountryCode() {
      return countryCode_;
    }

    /** Set the value for {@link #countryCode_} */
    PhoneContext setCountryCode(Integer value) {
      countryCode_ = value;
      return this;
    }
  }
}
