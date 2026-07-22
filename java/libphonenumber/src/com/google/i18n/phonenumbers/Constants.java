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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Constants used by the PhoneNumberUtil. */
final class Constants {
  // The maximum length of the country calling code.
  static final int MAX_LENGTH_COUNTRY_CODE = 3;

  // Map of country calling codes that use a mobile token before the area code. One example of when
  // this is relevant is when determining the length of the national destination code, which should
  // be the length of the area code plus the length of the mobile token.
  static final Map<Integer, String> MOBILE_TOKEN_MAPPINGS;

  // Set of country codes that have geographically assigned mobile numbers (see GEO_MOBILE_COUNTRIES
  // below) which are not based on *area codes*. For example, in China mobile numbers start with a
  // carrier indicator, and beyond that are geographically assigned: this carrier indicator is not
  // considered to be an area code.
  static final Set<Integer> GEO_MOBILE_COUNTRIES_WITHOUT_MOBILE_AREA_CODES;

  // Set of country codes that doesn't have national prefix, but it has area codes.
  static final Set<Integer> COUNTRIES_WITHOUT_NATIONAL_PREFIX_WITH_AREA_CODES;

  // Set of country calling codes that have geographically assigned mobile numbers. This may not be
  // complete; we add calling codes case by case, as we find geographical mobile numbers or hear
  // from user reports. Note that countries like the US, where we can't distinguish between
  // fixed-line or mobile numbers, are not listed here, since we consider FIXED_LINE_OR_MOBILE to be
  // a possibly geographically-related type anyway (like FIXED_LINE).
  static final Set<Integer> GEO_MOBILE_COUNTRIES;

  // The PLUS_SIGN signifies the international prefix.
  static final char PLUS_SIGN = '+';

  static final String RFC3966_PHONE_CONTEXT = ";phone-context=";

  // A map that contains characters that are essential when dialling. That means any of the
  // characters in this map must not be removed from a number when dialling, otherwise the call
  // will not reach the intended destination.
  static final Map<Character, Character> DIALLABLE_CHAR_MAPPINGS;


  // Only upper-case variants of alpha characters are stored.
  static final Map<Character, Character> ALPHA_MAPPINGS;

  // For performance reasons, amalgamate both into one map.
  static final Map<Character, Character> ALPHA_PHONE_MAPPINGS;

  // Separate map of all symbols that we wish to retain when formatting alpha numbers. This
  // includes digits, ASCII letters and number grouping symbols such as "-" and " ".
  static final Map<Character, Character> ALL_PLUS_NUMBER_GROUPING_SYMBOLS;

  static {
    HashMap<Integer, String> mobileTokenMap = new HashMap<>();
    mobileTokenMap.put(54, "9");
    MOBILE_TOKEN_MAPPINGS = Collections.unmodifiableMap(mobileTokenMap);

    HashSet<Integer> geoMobileCountriesWithoutMobileAreaCodes = new HashSet<>();
    geoMobileCountriesWithoutMobileAreaCodes.add(86); // China
    GEO_MOBILE_COUNTRIES_WITHOUT_MOBILE_AREA_CODES =
        Collections.unmodifiableSet(geoMobileCountriesWithoutMobileAreaCodes);

    HashSet<Integer> countriesWithoutNationalPrefixWithAreaCodes = new HashSet<>();
    countriesWithoutNationalPrefixWithAreaCodes.add(52); // Mexico
    COUNTRIES_WITHOUT_NATIONAL_PREFIX_WITH_AREA_CODES =
        Collections.unmodifiableSet(countriesWithoutNationalPrefixWithAreaCodes);

    HashSet<Integer> geoMobileCountries = new HashSet<>();
    geoMobileCountries.add(52); // Mexico
    geoMobileCountries.add(54); // Argentina
    geoMobileCountries.add(55); // Brazil
    geoMobileCountries.add(62); // Indonesia: some prefixes only (fixed CMDA wireless)
    geoMobileCountries.addAll(geoMobileCountriesWithoutMobileAreaCodes);
    GEO_MOBILE_COUNTRIES = Collections.unmodifiableSet(geoMobileCountries);

    // Simple ASCII digits map used to populate ALPHA_PHONE_MAPPINGS and
    // ALL_PLUS_NUMBER_GROUPING_SYMBOLS.
    HashMap<Character, Character> asciiDigitMappings = new HashMap<>();
    asciiDigitMappings.put('0', '0');
    asciiDigitMappings.put('1', '1');
    asciiDigitMappings.put('2', '2');
    asciiDigitMappings.put('3', '3');
    asciiDigitMappings.put('4', '4');
    asciiDigitMappings.put('5', '5');
    asciiDigitMappings.put('6', '6');
    asciiDigitMappings.put('7', '7');
    asciiDigitMappings.put('8', '8');
    asciiDigitMappings.put('9', '9');

    HashMap<Character, Character> alphaMap = new HashMap<>(40);
    alphaMap.put('A', '2');
    alphaMap.put('B', '2');
    alphaMap.put('C', '2');
    alphaMap.put('D', '3');
    alphaMap.put('E', '3');
    alphaMap.put('F', '3');
    alphaMap.put('G', '4');
    alphaMap.put('H', '4');
    alphaMap.put('I', '4');
    alphaMap.put('J', '5');
    alphaMap.put('K', '5');
    alphaMap.put('L', '5');
    alphaMap.put('M', '6');
    alphaMap.put('N', '6');
    alphaMap.put('O', '6');
    alphaMap.put('P', '7');
    alphaMap.put('Q', '7');
    alphaMap.put('R', '7');
    alphaMap.put('S', '7');
    alphaMap.put('T', '8');
    alphaMap.put('U', '8');
    alphaMap.put('V', '8');
    alphaMap.put('W', '9');
    alphaMap.put('X', '9');
    alphaMap.put('Y', '9');
    alphaMap.put('Z', '9');
    ALPHA_MAPPINGS = Collections.unmodifiableMap(alphaMap);

    HashMap<Character, Character> combinedMap = new HashMap<>(100);
    combinedMap.putAll(ALPHA_MAPPINGS);
    combinedMap.putAll(asciiDigitMappings);
    ALPHA_PHONE_MAPPINGS = Collections.unmodifiableMap(combinedMap);

    HashMap<Character, Character> diallableCharMap = new HashMap<>();
    diallableCharMap.putAll(asciiDigitMappings);
    diallableCharMap.put(PLUS_SIGN, PLUS_SIGN);
    diallableCharMap.put('*', '*');
    diallableCharMap.put('#', '#');
    DIALLABLE_CHAR_MAPPINGS = Collections.unmodifiableMap(diallableCharMap);

    HashMap<Character, Character> allPlusNumberGroupings = new HashMap<>();
    // Put (lower letter -> upper letter) and (upper letter -> upper letter) mappings.
    for (char c : ALPHA_MAPPINGS.keySet()) {
      allPlusNumberGroupings.put(Character.toLowerCase(c), c);
      allPlusNumberGroupings.put(c, c);
    }
    allPlusNumberGroupings.putAll(asciiDigitMappings);
    // Put grouping symbols.
    allPlusNumberGroupings.put('-', '-');
    allPlusNumberGroupings.put('\uFF0D', '-');
    allPlusNumberGroupings.put('\u2010', '-');
    allPlusNumberGroupings.put('\u2011', '-');
    allPlusNumberGroupings.put('\u2012', '-');
    allPlusNumberGroupings.put('\u2013', '-');
    allPlusNumberGroupings.put('\u2014', '-');
    allPlusNumberGroupings.put('\u2015', '-');
    allPlusNumberGroupings.put('\u2212', '-');
    allPlusNumberGroupings.put('/', '/');
    allPlusNumberGroupings.put('\uFF0F', '/');
    allPlusNumberGroupings.put(' ', ' ');
    allPlusNumberGroupings.put('\u3000', ' ');
    allPlusNumberGroupings.put('\u2060', ' ');
    allPlusNumberGroupings.put('.', '.');
    allPlusNumberGroupings.put('\uFF0E', '.');
    ALL_PLUS_NUMBER_GROUPING_SYMBOLS = Collections.unmodifiableMap(allPlusNumberGroupings);
  }

  static final String DIGITS = "\\p{Nd}";
  // We accept alpha characters in phone numbers, ASCII only, upper and lower case.
  static final String VALID_ALPHA =
      Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).replaceAll("[, \\[\\]]", "")
          + Arrays.toString(ALPHA_MAPPINGS.keySet().toArray())
              .toLowerCase().replaceAll("[, \\[\\]]", "");

  // We use this pattern to check if the phone number has at least three letters in it - if so, then
  // we treat it as a number where some phone-number digits are represented by letters.
  static final Pattern VALID_ALPHA_PHONE_PATTERN = Pattern.compile("(?:.*?[A-Za-z]){3}.*");

  // Regular expression of valid global-number-digits for the phone-context parameter, following the
  // syntax defined in RFC3966.
  static final String RFC3966_VISUAL_SEPARATOR = "[\\-\\.\\(\\)]?";
  static final String RFC3966_PHONE_DIGIT =
      "(" + DIGITS + "|" + RFC3966_VISUAL_SEPARATOR + ")";
  static final String RFC3966_GLOBAL_NUMBER_DIGITS =
      "^\\" + PLUS_SIGN + RFC3966_PHONE_DIGIT + "*" + DIGITS + RFC3966_PHONE_DIGIT + "*$";
  static final Pattern RFC3966_GLOBAL_NUMBER_DIGITS_PATTERN =
      Pattern.compile(RFC3966_GLOBAL_NUMBER_DIGITS);

  // Regular expression of valid domainname for the phone-context parameter, following the syntax
  // defined in RFC3966.
  static final String ALPHANUM = VALID_ALPHA + DIGITS;
  static final String RFC3966_DOMAINLABEL =
      "[" + ALPHANUM + "]+((\\-)*[" + ALPHANUM + "])*";
  static final String RFC3966_TOPLABEL =
      "[" + VALID_ALPHA + "]+((\\-)*[" + ALPHANUM + "])*";
  static final String RFC3966_DOMAINNAME =
      "^(" + RFC3966_DOMAINLABEL + "\\.)*" + RFC3966_TOPLABEL + "\\.?$";
  static final Pattern RFC3966_DOMAINNAME_PATTERN = Pattern.compile(RFC3966_DOMAINNAME);

  private Constants() {}
}
