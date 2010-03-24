/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.protobuf.MessageLite;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for international phone numbers. Functionality includes formatting, parsing and
 * validation.
 *
 * @author Shaopeng Jia
 * @author Lara Rennie
 */
public class PhoneNumberUtil {
  // The minimum and maximum length of the national significant number.
  private static final int MIN_LENGTH_FOR_NSN = 3;
  private static final int MAX_LENGTH_FOR_NSN = 15;
  private static final String META_DATA_FILE =
      "/com/google/i18n/phonenumbers/src/generated_files/PhoneNumberMetadataProto";
  private static final Logger LOGGER = Logger.getLogger(PhoneNumberUtil.class.getName());

  // A mapping from a country code to a region code which denotes the country/region
  // represented by that country code. Note countries under NANPA share the country code 1,
  // Russia and Kazakhstan share the country code 7, and many French territories in the Indian
  // Ocean share the country code 262. Under this map, 1 is mapped to US, 7 is mapped to RU,
  // and 262 is mapped to RE.
  private final Map<Integer, String> countryCodeToRegionCodeMap = new HashMap<Integer, String>();

  // The set of countries that share country code 1.
  private final Set<String> nanpaCountries = new HashSet<String>();
  private static final int NANPA_COUNTRY_CODE = 1;

  // The set of countries that share country code 7.
  private final Set<String> russiaFederationCountries = new HashSet<String>(2);
  private static final int RUSSIAN_FED_COUNTRY_CODE = 7;

  // The set of countries that share country code 262.
  private final Set<String> frenchIndianOceanTerritories = new HashSet<String>(6);

  private static final int FRENCH_INDIAN_OCEAN_COUNTRY_CODE = 262;

  // The PLUS_SIGN signifies the international prefix.
  static final char PLUS_SIGN = '+';

  // These mappings map a character (key) to a specific digit that should replace it for
  // normalization purposes. Non-European digits that may be used in phone numbers are mapped to a
  // European equivalent.
  static final Map<Character, Character> DIGIT_MAPPINGS =
      new ImmutableMap.Builder<Character, Character>()
      .put('0', '0')
      .put('\uFF10', '0')  // Fullwidth digit 0
      .put('\u0660', '0')  // Arabic-indic digit 0
      .put('1', '1')
      .put('\uFF11', '1')  // Fullwidth digit 1
      .put('\u0661', '1')  // Arabic-indic digit 1
      .put('2', '2')
      .put('\uFF12', '2')  // Fullwidth digit 2
      .put('\u0662', '2')  // Arabic-indic digit 2
      .put('3', '3')
      .put('\uFF13', '3')  // Fullwidth digit 3
      .put('\u0663', '3')  // Arabic-indic digit 3
      .put('4', '4')
      .put('\uFF14', '4')  // Fullwidth digit 4
      .put('\u0664', '4')  // Arabic-indic digit 4
      .put('5', '5')
      .put('\uFF15', '5')  // Fullwidth digit 5
      .put('\u0665', '5')  // Arabic-indic digit 5
      .put('6', '6')
      .put('\uFF16', '6')  // Fullwidth digit 6
      .put('\u0666', '6')  // Arabic-indic digit 6
      .put('7', '7')
      .put('\uFF17', '7')  // Fullwidth digit 7
      .put('\u0667', '7')  // Arabic-indic digit 7
      .put('8', '8')
      .put('\uFF18', '8')  // Fullwidth digit 8
      .put('\u0668', '8')  // Arabic-indic digit 8
      .put('9', '9')
      .put('\uFF19', '9')  // Fullwidth digit 9
      .put('\u0669', '9')  // Arabic-indic digit 9
      .build();

  // Only upper-case variants of alpha characters are stored.
  private static final Map<Character, Character> ALPHA_MAPPINGS =
      new ImmutableMap.Builder<Character, Character>()
      .put('A', '2')
      .put('B', '2')
      .put('C', '2')
      .put('D', '3')
      .put('E', '3')
      .put('F', '3')
      .put('G', '4')
      .put('H', '4')
      .put('I', '4')
      .put('J', '5')
      .put('K', '5')
      .put('L', '5')
      .put('M', '6')
      .put('N', '6')
      .put('O', '6')
      .put('P', '7')
      .put('Q', '7')
      .put('R', '7')
      .put('S', '7')
      .put('T', '8')
      .put('U', '8')
      .put('V', '8')
      .put('W', '9')
      .put('X', '9')
      .put('Y', '9')
      .put('Z', '9')
      .build();

  // For performance reasons, amalgamate both into one map.
  private static final Map<Character, Character> ALL_NORMALIZATION_MAPPINGS =
      new ImmutableMap.Builder<Character, Character>()
      .putAll(ALPHA_MAPPINGS)
      .putAll(DIGIT_MAPPINGS)
      .build();

  // A list of all country codes where national significant numbers (excluding any national prefix)
  // exist that start with a leading zero.
  private static final Set<Integer> LEADING_ZERO_COUNTRIES =
      new ImmutableSet.Builder<Integer>()
      .add(39)  // Italy
      .add(225)  // C™te d'Ivoire
      .add(228)  // Togo
      .add(240)  // Equatorial Guinea
      .add(241)  // Gabon
      .build();

  // Pattern that makes it easy to distinguish whether a country has a unique international dialing
  // prefix or not. If a country has a unique international prefix (e.g. 011 in USA), it will be
  // represented as a string that contains a sequence of ASCII digits. If there are multiple
  // available international prefixes in a country, they will be represented as a regex string that
  // always contains character(s) other than ASCII digits.
  // Note this regex also includes tilde, which signals waiting for the tone.
  private static final Pattern UNIQUE_INTERNATIONAL_PREFIX =
      Pattern.compile("[\\d]+(?:[~\u2053\u223C\uFF5E][\\d]+)?");

  // Regular expression of acceptable punctuation found in phone numbers. This excludes punctuation
  // found as a leading character only.
  // This consists of dash characters, white space characters, full stops, slashes,
  // square brackets, parentheses and tildes. It also includes the letter 'x' as that is found as a
  // placeholder for carrier information in some phone numbers.
  private static final String VALID_PUNCTUATION = "-x\u2010-\u2015\u2212\uFF0D-\uFF0F " +
      "\u00A0\u200B\u2060\u3000()\uFF08\uFF09\uFF3B\uFF3D.\\[\\]/~\u2053\u223C\uFF5E";

  // Digits accepted in phone numbers
  private static final String VALID_DIGITS =
      Arrays.toString(DIGIT_MAPPINGS.keySet().toArray()).replaceAll(", ", "");
  // We accept alpha characters in phone numbers, ASCII only, upper and lower case.
  private static final String VALID_ALPHA =
      Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).replaceAll(", ", "") +
      Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).toLowerCase().replaceAll(", ", "");
  private static final String PLUS_CHARS = "+\uFF0B";
  private static final Pattern CAPTURING_DIGIT_PATTERN =
      Pattern.compile("([" + VALID_DIGITS + "])");

  // Regular expression of acceptable characters that may start a phone number for the purposes of
  // parsing. This allows us to strip away meaningless prefixes to phone numbers that may be
  // mistakenly given to us. This consists of digits, the plus symbol and arabic-indic digits. This
  // does not contain alpha characters, although they may be used later in the number. It also does
  // not include other punctuation, as this will be stripped later during parsing and is of no
  // information value when parsing a number.
  private static final String VALID_START_CHAR = "[" + PLUS_CHARS + VALID_DIGITS + "]";
  private static final Pattern VALID_START_CHAR_PATTERN = Pattern.compile(VALID_START_CHAR);

  // Regular expression of characters typically used to start a second phone number for the purposes
  // of parsing. This allows us to strip off parts of the number that are actually the start of
  // another number, such as for: (530) 583-6985 x302/x2303 -> the second extension here makes this
  // actually two phone numbers, (530) 583-6985 x302 and (530) 583-6985 x2303. We remove the second
  // extension so that the first number is parsed correctly.
  private static final String SECOND_NUMBER_START = "[\\\\/] *x";
  private static final Pattern SECOND_NUMBER_START_PATTERN = Pattern.compile(SECOND_NUMBER_START);

  // Regular expression of trailing characters that we want to remove. We remove all characters that
  // are not alpha or numerical characters. The hash character is retained here, as it may signify
  // the previous block was an extension.
  private static final String UNWANTED_END_CHARS = "[[\\P{N}&&\\P{L}]&&[^#]]+$";
  private static final Pattern UNWANTED_END_CHAR_PATTERN = Pattern.compile(UNWANTED_END_CHARS);

  // We use this pattern to check if the phone number has at least three letters in it - if so, then
  // we treat it as a number where some phone-number digits are represented by letters.
  private static final Pattern VALID_ALPHA_PHONE_PATTERN = Pattern.compile("(?:.*?[A-Za-z]){3}.*");

  // Regular expression of viable phone numbers. This is location independent. Checks we have at
  // least three leading digits, and only valid punctuation, alpha characters and
  // digits in the phone number. Does not include extension data.
  // The symbol 'x' is allowed here as valid punctuation since it is often used as a placeholder for
  // carrier codes, for example in Brazilian phone numbers.
  // Corresponds to the following:
  // plus_sign?([punctuation]*[digits]){3,}([punctuation]|[digits]|[alpha])*
  private static final String VALID_PHONE_NUMBER =
      "[" + PLUS_CHARS + "]?(?:[" + VALID_PUNCTUATION + "]*[" + VALID_DIGITS + "]){3,}[" +
      VALID_ALPHA + VALID_PUNCTUATION + VALID_DIGITS + "]*";

  // Default extension prefix to use when formatting. This will be put in front of any extension
  // component of the number, after the main national number is formatted. For example, if you wish
  // the default extension formatting to be " extn: 3456", then you should specify " extn: " here
  // as the default extension prefix. This can be overridden by country-specific preferences.
  private static final String DEFAULT_EXTN_PREFIX = " ext. ";

  // Regexp of all possible ways to write extensions, for use when parsing. This will be run as a
  // case-insensitive regexp match. Wide character versions are also provided after each ascii
  // version. There are two regular expressions here: the more generic one starts with optional
  // white space and ends with an optional full stop (.), followed by zero or more spaces/tabs and
  // then the numbers themselves. The other one covers the special case of American numbers where
  // the extension is written with a hash at the end, such as "- 503#".
  // Note that the only capturing groups should be around the digits that you want to capture as
  // part of the extension, or else parsing will fail!
  private static final String KNOWN_EXTN_PATTERNS = "[ \u00A0\\t,]*(?:ext(?:ensio)?n?|" +
      "\uFF45\uFF58\uFF54\uFF4E?|[,x\uFF58#\uFF03~\uFF5E]|int|\uFF49\uFF4E\uFF54)" +
      "[:\\.\uFF0E]?[ \u00A0\\t,]*([" + VALID_DIGITS + "]{1,7})|[- ]+([" + VALID_DIGITS +
      "]{1,5})#";

  // Regexp of all known extension prefixes used by different countries followed by 1 or more valid
  // digits, for use when parsing.
  private static final Pattern EXTN_PATTERN =
      Pattern.compile("(?:" + KNOWN_EXTN_PATTERNS + ")$",
                      Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

  // We append optionally the extension pattern to the end here, as a valid phone number may
  // have an extension prefix appended, followed by 1 or more digits.
  private static final Pattern VALID_PHONE_NUMBER_PATTERN =
      Pattern.compile(VALID_PHONE_NUMBER + "(?:" + KNOWN_EXTN_PATTERNS + ")?",
                      Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

  private static PhoneNumberUtil instance = null;

  // A mapping from a region code to the PhoneMetadata for that region.
  private Map<String, PhoneMetadata> countryToMetadataMap =
      Collections.synchronizedMap(new HashMap<String, PhoneMetadata>());

  /**
   * INTERNATIONAL and NATIONAL formats are consistent with the definition in ITU-T Recommendation
   * E. 123. For example, the number of the Google Zurich office will be written as
   * "+41 44 668 1800" in INTERNATIONAL format, and as "044 668 1800" in NATIONAL format.
   * E164 format is as per INTERNATIONAL format but with no formatting applied, e.g. +41446681800.
   *
   * Note: If you are considering storing the number in a neutral format, you are highly advised to
   * use the phonenumber.proto.
   */
  public enum PhoneNumberFormat {
    E164,
    INTERNATIONAL,
    NATIONAL
  }

  /**
   * Type of phone numbers.
   */
  public enum PhoneNumberType {
    FIXED_LINE,
    MOBILE,
    // In some countries (e.g. the USA), it is impossible to distinguish between fixed-line and
    // mobile numbers by looking at the phone number itself.
    FIXED_LINE_OR_MOBILE,
    // Freephone lines
    TOLL_FREE,
    PREMIUM_RATE,
    // The cost of this call is shared between the caller and the recipient, and is hence typically
    // less than PREMIUM_RATE calls. See // http://en.wikipedia.org/wiki/Shared_Cost_Service for
    // more information.
    SHARED_COST,
    // Voice over IP numbers. This includes TSoIP (Telephony Service over IP).
    VOIP,
    // A personal number is associated with a particular person, and may be routed to either a
    // MOBILE or FIXED_LINE number. Some more information can be found here:
    // http://en.wikipedia.org/wiki/Personal_Numbers
    PERSONAL_NUMBER,
    // A phone number is of type UNKNOWN when it does not fit any of the known patterns for a
    // specific country.
    UNKNOWN
  }

  /**
   * Types of phone number matches. See detailed description beside the isNumberMatch() method.
   */
  public enum MatchType {
    NO_MATCH,
    SHORT_NSN_MATCH,
    NSN_MATCH,
    EXACT_MATCH,
  }

  /**
   * Possible outcomes when testing if a PhoneNumber is possible.
   */
  public enum ValidationResult {
    IS_POSSIBLE,
    INVALID_COUNTRY_CODE,
    TOO_SHORT,
    TOO_LONG,
  }

  /**
   * This class implements a singleton, so the only constructor is private.
   */
  private PhoneNumberUtil() {
  }

  private void init(InputStream source) {
    // Read in metadata for each country.
    try {
      PhoneMetadataCollection metadataCollection = PhoneMetadataCollection.parseFrom(source);
      for (PhoneMetadata metadata : metadataCollection.getMetadataList()) {
        String regionCode = metadata.getId();
        countryToMetadataMap.put(regionCode, metadata);
        int countryCode = metadata.getCountryCode();
        switch (countryCode) {
          case NANPA_COUNTRY_CODE:
            nanpaCountries.add(regionCode);
            break;
          case RUSSIAN_FED_COUNTRY_CODE:
            russiaFederationCountries.add(regionCode);
            break;
          case FRENCH_INDIAN_OCEAN_COUNTRY_CODE:
            frenchIndianOceanTerritories.add(regionCode);
            break;
          default:
            countryCodeToRegionCodeMap.put(countryCode, regionCode);
            break;
        }
      }

      // Override the value, so that 1 is always mapped to US, 7 is always mapped to RU, and 262 to
      // RE.
      countryCodeToRegionCodeMap.put(NANPA_COUNTRY_CODE, "US");
      countryCodeToRegionCodeMap.put(RUSSIAN_FED_COUNTRY_CODE, "RU");
      countryCodeToRegionCodeMap.put(FRENCH_INDIAN_OCEAN_COUNTRY_CODE, "RE");
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    }
  }

  /**
   * Attempts to extract a possible number from the string passed in. This currently strips all
   * leading characters that could not be used to start a phone number. Characters that can be used
   * to start a phone number are defined in the VALID_START_CHAR_PATTERN. If none of these
   * characters are found in the number passed in, an empty string is returned. This function also
   * attempts to strip off any alternative extensions or endings if two or more are present, such as
   * in the case of: (530) 583-6985 x302/x2303. The second extension here makes this actually two
   * phone numbers, (530) 583-6985 x302 and (530) 583-6985 x2303. We remove the second extension so
   * that the first number is parsed correctly.
   *
   * @param number  the string that might contain a phone number
   * @return        the number, stripped of any non-phone-number prefix (such as "Tel:") or an empty
   *                string if no character used to start phone numbers (such as + or any digit) is
   *                found in the number
   */
  @VisibleForTesting
  static String extractPossibleNumber(String number) {
    // Remove trailing non-alpha non-numerical characters.
    Matcher trailingCharsMatcher = UNWANTED_END_CHAR_PATTERN.matcher(number);
    if (trailingCharsMatcher.find()) {
      number = number.substring(0, trailingCharsMatcher.start());
      LOGGER.log(Level.FINER, "Stripped trailing characters: " + number);
    }
    Matcher m = VALID_START_CHAR_PATTERN.matcher(number);
    if (m.find()) {
      number = number.substring(m.start());
      // Check for extra numbers at the end.
      Matcher secondNumber = SECOND_NUMBER_START_PATTERN.matcher(number);
      if (secondNumber.find()) {
        number = number.substring(0, secondNumber.start());
      }
      return number;
    } else {
      return "";
    }
  }

  /**
   * Checks to see if the string of characters could possibly be a phone number at all. At the
   * moment, checks to see that the string begins with at least 3 digits, ignoring any punctuation
   * commonly found in phone numbers.
   * This method does not require the number to be normalized in advance - but does assume that
   * leading non-number symbols have been removed, such as by the method extractPossibleNumber.
   *
   * @param number  string to be checked for viability as a phone number
   * @return        true if the number could be a phone number of some sort, otherwise false
   */
  @VisibleForTesting
  static boolean isViablePhoneNumber(String number) {
    if (number.length() < MIN_LENGTH_FOR_NSN) {
      return false;
    }
    Matcher m = VALID_PHONE_NUMBER_PATTERN.matcher(number);
    return m.matches();
  }

  /**
   * Normalizes a string of characters representing a phone number. This performs the following
   * conversions:
   *   Wide-ascii digits are converted to normal ASCII (European) digits.
   *   Letters are converted to their numeric representation on a telephone keypad. The keypad
   *       used here is the one defined in ITU Recommendation E.161. This is only done if there are
   *       3 or more letters in the number, to lessen the risk that such letters are typos -
   *       otherwise alpha characters are stripped.
   *   Punctuation is stripped.
   *   Arabic-Indic numerals are converted to European numerals.
   *
   * @param number  a string of characters representing a phone number
   * @return        the normalized string version of the phone number
   */
  static String normalize(String number) {
    Matcher m = VALID_ALPHA_PHONE_PATTERN.matcher(number);
    if (m.matches()) {
      return normalizeHelper(number, ALL_NORMALIZATION_MAPPINGS, true);
    } else {
      return normalizeHelper(number, DIGIT_MAPPINGS, true);
    }
  }

  /**
   * Normalizes a string of characters representing a phone number. This is a wrapper for
   * normalize(String number) but does in-place normalization of the StringBuffer provided.
   *
   * @param number  a StringBuffer of characters representing a phone number that will be normalized
   *     in place
   */
  static void normalize(StringBuffer number) {
    String normalizedNumber = normalize(number.toString());
    number.replace(0, number.length(), normalizedNumber);
  }

  /**
   * Normalizes a string of characters representing a phone number. This converts wide-ascii and
   * arabic-indic numerals to European numerals, and strips punctuation and alpha characters.
   *
   * @param number  a string of characters representing a phone number
   * @return        the normalized string version of the phone number
   */
  public static String normalizeDigitsOnly(String number) {
    return normalizeHelper(number, DIGIT_MAPPINGS, true);
  }

  /**
   * Converts all alpha characters in a number to their respective digits on a keypad, but retains
   * existing formatting. Also converts wide-ascii digits to normal ascii digits, and converts
   * Arabic-Indic numerals to European numerals.
   */
  public static String convertAlphaCharactersInNumber(String number) {
    return normalizeHelper(number, ALL_NORMALIZATION_MAPPINGS, false);
  }

  /**
   * Normalizes a string of characters representing a phone number by replacing all characters found
   * in the accompanying map with the values therein, and stripping all other characters if
   * removeNonMatches is true.
   *
   * @param number                     a string of characters representing a phone number
   * @param normalizationReplacements  a mapping of characters to what they should be replaced by in
   *                                   the normalized version of the phone number
   * @param removeNonMatches           indicates whether characters that are not able to be replaced
   *                                   should be stripped from the number. If this is false, they
   *                                   will be left unchanged in the number.
   * @return the normalized string version of the phone number
   */
  private static String normalizeHelper(String number,
                                        Map<Character, Character> normalizationReplacements,
                                        boolean removeNonMatches) {
    StringBuffer normalizedNumber = new StringBuffer(number.length());
    char[] numberAsCharArray = number.toCharArray();
    for (char character : numberAsCharArray) {
      Character newDigit = normalizationReplacements.get(Character.toUpperCase(character));
      if (newDigit != null) {
        normalizedNumber.append(newDigit);
      } else if (!removeNonMatches) {
        normalizedNumber.append(character);
      }
      // If neither of the above are true, we remove this character.
    }
    return normalizedNumber.toString();
  }

  @VisibleForTesting
  static synchronized PhoneNumberUtil getInstance(InputStream source) {
    if (instance == null) {
      instance = new PhoneNumberUtil();
      instance.init(source);
    }
    return instance;
  }

  /**
   * Used for testing purposes only to reset the PhoneNumberUtil singleton to null.
   */
  @VisibleForTesting
  static synchronized void resetInstance() {
    instance = null;
  }

  /**
   * Convenience method to enable tests to get a list of what countries the library has metadata
   * for.
   */
  @VisibleForTesting
  Set<String> getSupportedCountries() {
    return countryToMetadataMap.keySet();
  }

  /**
   * Gets a PhoneNumberUtil instance to carry out international phone number formatting, parsing,
   * or validation. The instance is loaded with phone number metadata for a number of most commonly
   * used countries/regions.
   *
   * The PhoneNumberUtil is implemented as a singleton. Therefore, calling getInstance multiple
   * times will only result in one instance being created.
   *
   * @return a PhoneNumberUtil instance
   */
  public static synchronized PhoneNumberUtil getInstance() {
    if (instance == null) {
      instance = new PhoneNumberUtil();
      InputStream in = PhoneNumberUtil.class.getResourceAsStream(META_DATA_FILE);
      instance.init(in);
    }
    return instance;
  }

  /**
   * Helper function to check region code is not unknown or null. The countryCode and number
   * supplied is used only for the resultant log message.
   */
  private boolean isValidRegionCode(String regionCode, int countryCode, String number) {
    if (regionCode == null || regionCode.equals("ZZ")) {
      LOGGER.log(Level.WARNING,
                 "Number " + number + "has invalid or missing country code (" + countryCode + ")");
      return false;
    }
    return true;
  }

  /**
   * Formats a phone number in the specified format using default rules. Note that this does not
   * promise to produce a phone number that the user can dial from where they are - although we do
   * format in either 'national' or 'international' format depending on what the client asks for, we
   * do not currently support a more abbreviated format, such as for users in the same "area" who
   * could potentially dial the number without area code. Note that if the phone number has a
   * country code of 0 or an otherwise invalid country code, we cannot work out which formatting
   * rules to apply so we return the national significant number with no formatting applied.
   *
   * @param number         the phone number to be formatted
   * @param numberFormat   the format the phone number should be formatted into
   * @return the formatted phone number
   */
  public String format(PhoneNumber number, PhoneNumberFormat numberFormat) {
    int countryCode = number.getCountryCode();
    String nationalSignificantNumber = getUnformattedNationalNumber(number);
    if (numberFormat == PhoneNumberFormat.E164) {
      // Early exit for E164 case since no formatting of the national number needs to be applied.
      // Extensions are not formatted.
      return formatNumberByFormat(countryCode, PhoneNumberFormat.E164,
                                  nationalSignificantNumber, "");
    }
    // Note here that all NANPA formatting rules are contained by US, so we use that to format NANPA
    // numbers. The same applies to Russian Fed countries - rules are contained by Russia. French
    // Indian Ocean country rules are contained by Reunion.
    String regionCode = getRegionCodeForCountryCode(countryCode);
    if (!isValidRegionCode(regionCode, countryCode, nationalSignificantNumber)) {
      return nationalSignificantNumber;
    }
    String formattedExtension = maybeGetFormattedExtension(number, regionCode);
    return formatNumberByFormat(countryCode, numberFormat,
                                formatNationalNumber(nationalSignificantNumber,
                                                     regionCode,
                                                     numberFormat),
                                formattedExtension);
  }

  /**
   * Formats a phone number in the specified format using client-defined formatting rules. Note that
   * if the phone number has a country code of zero or an otherwise invalid country code, we cannot
   * work out things like whether there should be a national prefix applied, or how to format
   * extensions, so we return the national significant number with no formatting applied.
   *
   * @param number                        the phone number to be formatted
   * @param numberFormat                  the format the phone number should be formatted into
   * @param userDefinedFormats            formatting rules specified by clients
   * @return the formatted phone number
   */
  public String formatByPattern(PhoneNumber number,
                                PhoneNumberFormat numberFormat,
                                List<NumberFormat> userDefinedFormats) {
    int countryCode = number.getCountryCode();
    // Note getRegionCodeForCountryCode() is used because formatting information for countries which
    // share a country code is contained by only one country for performance reasons. For example,
    // for NANPA countries it will be contained in the metadata for US.
    String regionCode = getRegionCodeForCountryCode(countryCode);
    String nationalSignificantNumber = getUnformattedNationalNumber(number);
    if (!isValidRegionCode(regionCode, countryCode, nationalSignificantNumber)) {
      return nationalSignificantNumber;
    }
    int size = userDefinedFormats.size();
    for (int i = 0; i < size; i++) {
      NumberFormat numFormat = userDefinedFormats.get(i);
      String nationalPrefixFormattingRule = numFormat.getNationalPrefixFormattingRule();
      if (nationalPrefixFormattingRule.length() > 0) {
        String nationalPrefix = getMetadataForRegion(regionCode).getNationalPrefix();
        // Replace $NP with national prefix and $FG with the first group ($1).
        nationalPrefixFormattingRule =
            nationalPrefixFormattingRule.replaceFirst("\\$NP", nationalPrefix)
                .replaceFirst("\\$FG", "\\$1");
        userDefinedFormats.set(i, NumberFormat.newBuilder(numFormat)
            .setNationalPrefixFormattingRule(nationalPrefixFormattingRule).build());
      }
    }

    String formattedExtension = maybeGetFormattedExtension(number, regionCode);
    return formatNumberByFormat(countryCode,
                                numberFormat,
                                formatAccordingToFormats(nationalSignificantNumber,
                                                         userDefinedFormats,
                                                         numberFormat),
                                formattedExtension);
  }

  /**
   * Formats a phone number for out-of-country dialing purpose. If no countryCallingFrom
   * is supplied, we format the number in its INTERNATIONAL format. If the countryCallingFrom is
   * the same as the country where the number is from, then NATIONAL formatting will be applied.
   *
   * If the number itself has a country code of zero or an otherwise invalid country code, then we
   * return the number with no formatting applied.
   *
   * Note this function takes care of the case for calling inside of NANPA and between Russia and
   * Kazakhstan (who share the same country code). In those cases, no international prefix is used.
   * For countries which have multiple international prefixes, the number in its INTERNATIONAL
   * format will be returned instead.
   *
   * @param number               the phone number to be formatted
   * @param countryCallingFrom   the ISO 3166-1 two-letter country code that denotes the foreign
   *                             country where the call is being placed
   * @return the formatted phone number
   */
  public String formatOutOfCountryCallingNumber(PhoneNumber number,
                                                String countryCallingFrom) {
    if (countryCallingFrom == null || countryCallingFrom.equals("ZZ")) {
      LOGGER.log(Level.WARNING,
                 "Trying to format number from invalid region. International formatting applied.");
      return format(number, PhoneNumberFormat.INTERNATIONAL);
    }
    int countryCode = number.getCountryCode();
    if (countryCode == NANPA_COUNTRY_CODE && isNANPACountry(countryCallingFrom)) {
      // For NANPA countries, return the national format for these countries but prefix it with the
      // country code.
      return countryCode + " " + format(number, PhoneNumberFormat.NATIONAL);
    }
    if (countryCode == FRENCH_INDIAN_OCEAN_COUNTRY_CODE &&
        frenchIndianOceanTerritories.contains(countryCallingFrom)) {
      // For dialling between FRENCH_INDIAN_OCEAN countries, the 10 digit number is all we need.
      // Technically this is the case for dialling from la Reunion to other overseas departments of
      // France (French Guiana, Martinique, Guadeloupe), but not vice versa - so we don't cover this
      // edge case for now and for those cases return the version including country code.
      // Details here: http://www.petitfute.com/voyage/225-info-pratiques-reunion
      return format(number, PhoneNumberFormat.NATIONAL);
    }
    // If the country code is the Russian Fed country code, we check the number itself to determine
    // which region code it is for. We don't do this for NANPA countries because of performance
    // reasons, and instead use US rules for all NANPA numbers. Also, NANPA countries share the
    // same national and international prefixes, which is not the case for Russian Fed countries.
    // There is also a special case for toll-free and premium rate numbers dialled within Russian
    // Fed countries.
    String regionCode;
    if (countryCode == RUSSIAN_FED_COUNTRY_CODE) {
      if (russiaFederationCountries.contains(countryCallingFrom)) {
        // For toll-free numbers and premium rate numbers dialled from within Russian Fed countries,
        // we should format them as if they are local numbers.
        // A toll-free number would be dialled from KZ as 8-800-080-7777 but from Russia as
        // 0-800-080-7777. (Confirmation on government websites such as e.gov.kz).
        PhoneNumberType numberType = getNumberType(number);
        if (numberType == PhoneNumberType.TOLL_FREE || numberType == PhoneNumberType.PREMIUM_RATE) {
          return format(number, PhoneNumberFormat.NATIONAL);
        }
      }
      // Otherwise, we should find out what region the number really belongs to before continuing,
      // since they have different formatting rules.
      regionCode = getRegionCodeForNumber(number);
    } else {
      regionCode = getRegionCodeForCountryCode(countryCode);
    }
    String nationalSignificantNumber = getUnformattedNationalNumber(number);
    if (!isValidRegionCode(regionCode, countryCode, nationalSignificantNumber)) {
      return nationalSignificantNumber;
    }
    if (regionCode.equals(countryCallingFrom)) {
      return format(number, PhoneNumberFormat.NATIONAL);
    }
    String formattedNationalNumber =
        formatNationalNumber(nationalSignificantNumber,
                             regionCode, PhoneNumberFormat.INTERNATIONAL);
    PhoneMetadata metadata = getMetadataForRegion(countryCallingFrom);
    String internationalPrefix = metadata.getInternationalPrefix();
    String formattedExtension = maybeGetFormattedExtension(number, regionCode);
    // For countries that have multiple international prefixes, the international format of the
    // number is returned, unless there is a preferred international prefix.
    String internationalPrefixForFormatting = "";
    if (UNIQUE_INTERNATIONAL_PREFIX.matcher(internationalPrefix).matches()) {
      internationalPrefixForFormatting = internationalPrefix;
    } else if (metadata.hasPreferredInternationalPrefix()) {
      internationalPrefixForFormatting = metadata.getPreferredInternationalPrefix();
    }
    return !internationalPrefixForFormatting.equals("")
        ? internationalPrefixForFormatting + " " + countryCode + " " + formattedNationalNumber
          + formattedExtension
        : formatNumberByFormat(countryCode,
                               PhoneNumberFormat.INTERNATIONAL,
                               formattedNationalNumber,
                               formattedExtension);
  }

  static String getUnformattedNationalNumber(PhoneNumber number) {
    // The leading zero in the national (significant) number of an Italian phone number has a
    // special meaning. Unlike the rest of the world, it indicates the number is a landline
    // number. There have been plans to migrate landline numbers to start with the digit two since
    // December 2000, but it has not yet happened.
    // See http://en.wikipedia.org/wiki/%2B39 for more details.
    // Other countries such as Cote d'Ivoire and Gabon use this for their mobile numbers.
    StringBuffer nationalNumber = new StringBuffer(
        (isLeadingZeroCountry(number.getCountryCode()) &&
         number.hasItalianLeadingZero() &&
         number.getItalianLeadingZero())
        ? "0" : ""
    );
    nationalNumber.append(number.getNationalNumber());
    return nationalNumber.toString();
  }

  /**
   * A helper function that is used by format and formatByPattern.
   */
  private String formatNumberByFormat(int countryCode,
                                      PhoneNumberFormat numberFormat,
                                      String formattedNationalNumber,
                                      String formattedExtension) {
    switch (numberFormat) {
      case E164:
        return String.valueOf(PLUS_SIGN) + countryCode + formattedNationalNumber
            + formattedExtension;
      case INTERNATIONAL:
        return String.valueOf(PLUS_SIGN) + countryCode + " " + formattedNationalNumber
            + formattedExtension;
      case NATIONAL:
      default:
        return formattedNationalNumber + formattedExtension;
    }
  }

  // Note in some countries, the national number can be written in two completely different ways
  // depending on whether it forms part of the NATIONAL format or INTERNATIONAL format. The
  // numberFormat parameter here is used to specify which format to use for those cases.
  private String formatNationalNumber(String number,
                                      String regionCode,
                                      PhoneNumberFormat numberFormat) {
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    List<NumberFormat> intlNumberFormats = metadata.getIntlNumberFormatList();
    // When the intlNumberFormats exists, we use that to format national number for the
    // INTERNATIONAL format instead of using the numberDesc.numberFormats.
    List<NumberFormat> availableFormats =
        (intlNumberFormats.size() == 0 || numberFormat == PhoneNumberFormat.NATIONAL)
        ? metadata.getNumberFormatList()
        : metadata.getIntlNumberFormatList();
    return formatAccordingToFormats(number, availableFormats, numberFormat);
  }

  private String formatAccordingToFormats(String nationalNumber,
                                          List<NumberFormat> availableFormats,
                                          PhoneNumberFormat numberFormat) {
    for (NumberFormat numFormat : availableFormats) {
      if (!numFormat.hasLeadingDigits() ||
          Pattern.compile(numFormat.getLeadingDigits()).matcher(nationalNumber).lookingAt()) {
        String patternToMatch = numFormat.getPattern();
        if (nationalNumber.matches(patternToMatch)) {
          String nationalPrefixFormattingRule = numFormat.getNationalPrefixFormattingRule();
          if (numberFormat == PhoneNumberFormat.NATIONAL &&
              nationalPrefixFormattingRule != null &&
              nationalPrefixFormattingRule.length() > 0) {
            return nationalNumber.replaceAll(
                patternToMatch,
                numFormat.getFormat().replaceFirst("(\\$1)", nationalPrefixFormattingRule));
          } else {
            return nationalNumber.replaceAll(patternToMatch, numFormat.getFormat());
          }
        }
      }
    }

    // If no pattern above is matched, we format the number as a whole.
    return nationalNumber;
  }

  /**
   * Gets a valid number for the specified country.
   *
   * @param regionCode  the ISO 3166-1 two-letter country code that denotes the country for which
   *                    an example number is needed
   * @return  a valid fixed-line number for the specified country. Returns null when the metadata
   *    does not contain such information.
   */
  public PhoneNumber getExampleNumber(String regionCode) {
    return getExampleNumberForType(regionCode, PhoneNumberType.FIXED_LINE);
  }

  /**
   * Gets a valid number for the specified country and number type.
   *
   * @param regionCode  the ISO 3166-1 two-letter country code that denotes the country for which
   *                    an example number is needed
   * @param type  the type of number that is needed
   * @return  a valid number for the specified country and type. Returns null when the metadata
   *     does not contain such information.
   */
  public PhoneNumber getExampleNumberForType(String regionCode, PhoneNumberType type) {
    PhoneNumberDesc desc = getNumberDescByType(getMetadataForRegion(regionCode), type);
    try {
      if (desc.hasExampleNumber()) {
        return parse(desc.getExampleNumber(), regionCode);
      }
    } catch (NumberParseException e) {
      LOGGER.log(Level.SEVERE, e.toString());
    }
    return null;
  }

  /**
   * Gets the formatted extension of a phone number, if the phone number had an extension specified.
   * If not, it returns an empty string.
   */
  private String maybeGetFormattedExtension(PhoneNumber number, String regionCode) {
    if (!number.hasExtension()) {
      return "";
    } else {
      return formatExtension(number.getExtension(), regionCode);
    }
  }

  /**
   * Formats the extension part of the phone number by prefixing it with the appropriate extension
   * prefix. This will be the default extension prefix, unless overridden by a preferred
   * extension prefix for this country.
   */
  private String formatExtension(String extensionDigits, String regionCode) {
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    if (metadata.hasPreferredExtnPrefix()) {
      return metadata.getPreferredExtnPrefix() + extensionDigits;
    } else {
      return DEFAULT_EXTN_PREFIX + extensionDigits;
    }
  }

  PhoneNumberDesc getNumberDescByType(PhoneMetadata metadata, PhoneNumberType type) {
    switch (type) {
      case PREMIUM_RATE:
        return metadata.getPremiumRate();
      case TOLL_FREE:
        return metadata.getTollFree();
      case MOBILE:
        return metadata.getMobile();
      case FIXED_LINE:
      case FIXED_LINE_OR_MOBILE:
        return metadata.getFixedLine();
      case SHARED_COST:
        return metadata.getSharedCost();
      case VOIP:
        return metadata.getVoip();
      case PERSONAL_NUMBER:
        return metadata.getPersonalNumber();
      default:
        return metadata.getGeneralDesc();
    }
  }

  /**
   * Gets the type of a phone number.
   *
   * @param number  the phone number that we want to know the type
   * @return  the type of the phone number
   */
  public PhoneNumberType getNumberType(PhoneNumber number) {
    String regionCode = getRegionCodeForNumber(number);
    String nationalSignificantNumber = getUnformattedNationalNumber(number);
    if (!isValidRegionCode(regionCode, number.getCountryCode(), nationalSignificantNumber)) {
      return PhoneNumberType.UNKNOWN;
    }
    return getNumberTypeHelper(nationalSignificantNumber, getMetadataForRegion(regionCode));
  }

  private PhoneNumberType getNumberTypeHelper(String nationalNumber, PhoneMetadata metadata) {
    PhoneNumberDesc generalNumberDesc = metadata.getGeneralDesc();
    if (!generalNumberDesc.hasNationalNumberPattern() ||
        !isNumberMatchingDesc(nationalNumber, generalNumberDesc)) {
      LOGGER.log(Level.FINEST,
                 "Number type unknown - doesn't match general national number pattern.");
      return PhoneNumberType.UNKNOWN;
    }

    if (isNumberMatchingDesc(nationalNumber, metadata.getPremiumRate())) {
      LOGGER.log(Level.FINEST, "Number is a premium number.");
      return PhoneNumberType.PREMIUM_RATE;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getTollFree())) {
      LOGGER.log(Level.FINEST, "Number is a toll-free number.");
      return PhoneNumberType.TOLL_FREE;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getSharedCost())) {
      LOGGER.log(Level.FINEST, "Number is a shared cost number.");
      return PhoneNumberType.SHARED_COST;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getVoip())) {
      LOGGER.log(Level.FINEST, "Number is a VOIP (Voice over IP) number.");
      return PhoneNumberType.VOIP;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getPersonalNumber())) {
      LOGGER.log(Level.FINEST, "Number is a personal number.");
      return PhoneNumberType.PERSONAL_NUMBER;
    }

    boolean isFixedLine = isNumberMatchingDesc(nationalNumber, metadata.getFixedLine());
    if (isFixedLine) {
      if (metadata.getSameMobileAndFixedLinePattern()) {
        LOGGER.log(Level.FINEST,
                   "Fixed-line and mobile patterns equal, number is fixed-line or mobile");
        return PhoneNumberType.FIXED_LINE_OR_MOBILE;
      } else if (isNumberMatchingDesc(nationalNumber, metadata.getMobile())) {
        LOGGER.log(Level.FINEST,
                   "Fixed-line and mobile patterns differ, but number is " +
                   "still fixed-line or mobile");
        return PhoneNumberType.FIXED_LINE_OR_MOBILE;
      }
      LOGGER.log(Level.FINEST, "Number is a fixed line number.");
      return PhoneNumberType.FIXED_LINE;
    }
    // Otherwise, test to see if the number is mobile. Only do this if certain that the patterns for
    // mobile and fixed line aren't the same.
    if (!metadata.getSameMobileAndFixedLinePattern() &&
        isNumberMatchingDesc(nationalNumber, metadata.getMobile())) {
      LOGGER.log(Level.FINEST, "Number is a mobile number.");
      return PhoneNumberType.MOBILE;
    }
    LOGGER.log(Level.FINEST,
               "Number type unknown - doesn't match any specific number type pattern.");
    return PhoneNumberType.UNKNOWN;
  }

  PhoneMetadata getMetadataForRegion(String regionCode) {
    if (regionCode == null) {
      return null;
    }
    return countryToMetadataMap.get(regionCode);
  }

  private boolean isNumberMatchingDesc(String nationalNumber, PhoneNumberDesc numberDesc) {
    return nationalNumber.matches(numberDesc.getPossibleNumberPattern()) &&
           nationalNumber.matches(numberDesc.getNationalNumberPattern());
  }

  /**
   * Tests whether a phone number matches a valid pattern. Note this doesn't verify the number
   * is actually in use, which is impossible to tell by just looking at a number itself.
   *
   * @param number       the phone number that we want to validate
   * @return  a boolean that indicates whether the number is of a valid pattern
   */
  public boolean isValidNumber(PhoneNumber number) {
    String regionCode = getRegionCodeForNumber(number);
    return isValidRegionCode(regionCode, number.getCountryCode(),
                             getUnformattedNationalNumber(number))
           && isValidNumberForRegion(number, regionCode);
  }

  /**
   * Tests whether a phone number is valid for a certain region. Note this doesn't verify the number
   * is actually in use, which is impossible to tell by just looking at a number itself. If the
   * country code is not the same as the country code for the region, this immediately exits with
   * false. After this, the specific number pattern rules for the region are examined. This is
   * useful for determining for example whether a particular number is valid for Canada, rather than
   * just a valid NANPA number.
   *
   * @param number       the phone number that we want to validate
   * @param regionCode   the ISO 3166-1 two-letter country code that denotes the region/country
   *                     that we want to validate the phone number for
   * @return  a boolean that indicates whether the number is of a valid pattern
   */
  public boolean isValidNumberForRegion(PhoneNumber number, String regionCode) {
    if (number.getCountryCode() != getCountryCodeForRegion(regionCode)) {
      return false;
    }
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    PhoneNumberDesc generalNumDesc = metadata.getGeneralDesc();
    String nationalSignificantNumber = getUnformattedNationalNumber(number);

    // For countries where we don't have metadata for PhoneNumberDesc, we treat any number passed
    // in as a valid number if its national significant number is between the minimum and maximum
    // lengths defined by ITU for a national significant number.
    if (!generalNumDesc.hasNationalNumberPattern()) {
      LOGGER.log(Level.FINER, "Validating number with incomplete metadata.");
      int numberLength = nationalSignificantNumber.length();
      return numberLength > MIN_LENGTH_FOR_NSN && numberLength <= MAX_LENGTH_FOR_NSN;
    }
    return isNumberMatchingDesc(nationalSignificantNumber, generalNumDesc)
           && getNumberTypeHelper(nationalSignificantNumber, metadata) != PhoneNumberType.UNKNOWN;
  }

  /**
   * Returns the country/region where a phone number is from. This could be used for geo-coding in
   * the country/region level.
   *
   * @param number  the phone number whose origin we want to know
   * @return  the country/region where the phone number is from
   */
  public String getRegionCodeForNumber(PhoneNumber number) {
    int countryCode = number.getCountryCode();
    switch (countryCode) {
      case NANPA_COUNTRY_CODE:
        // Override this and try the US case first, since it is more likely than other countries,
        // for performance reasons.
        if (isValidNumberForRegion(number, "US")) {
          return "US";
        }
        Set<String> nanpaExceptUS = new HashSet<String>(nanpaCountries);
        nanpaExceptUS.remove("US");
        return getRegionCodeForNumberFromRegionList(number, nanpaExceptUS);
      case RUSSIAN_FED_COUNTRY_CODE:
        return getRegionCodeForNumberFromRegionList(number, russiaFederationCountries);
      case FRENCH_INDIAN_OCEAN_COUNTRY_CODE:
        return getRegionCodeForNumberFromRegionList(number, frenchIndianOceanTerritories);
      default:
        return getRegionCodeForCountryCode(countryCode);
    }
  }

  private String getRegionCodeForNumberFromRegionList(PhoneNumber number,
                                                      Set<String> regionCodes) {
    String nationalNumber = String.valueOf(number.getNationalNumber());
    for (String regionCode : regionCodes) {
      if (getNumberTypeHelper(nationalNumber, getMetadataForRegion(regionCode)) !=
          PhoneNumberType.UNKNOWN) {
        return regionCode;
      }
    }
    return null;
  }

  /**
   * Returns the region code that matches the specific country code. In the case of no region code
   * being found, ZZ will be returned.
   */
  String getRegionCodeForCountryCode(int countryCode) {
    String regionCode = countryCodeToRegionCodeMap.get(countryCode);
    return regionCode == null ? "ZZ" : regionCode;
  }

  /**
   * Returns the country calling code for a specific region. For example, this would be 1 for the
   * United States, and 64 for New Zealand.
   *
   * @param regionCode  the ISO 3166-1 two-letter country code that denotes the country/region that
   *                    we want to get the country code for
   * @return  the country calling code for the country/region denoted by regionCode
   */
  public int getCountryCodeForRegion(String regionCode) {
    if (regionCode == null || regionCode.equals("ZZ")) {
      LOGGER.log(Level.SEVERE, "Invalid or missing country code provided.");
      return 0;
    }
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    if (metadata == null) {
      LOGGER.log(Level.SEVERE, "Unsupported country code provided.");
      return 0;
    }
    return metadata.getCountryCode();
  }

  /**
   * Check if a country is one of the countries under the North American Numbering Plan
   * Administration (NANPA).
   *
   * @return  true if regionCode is one of the countries under NANPA
   */
  public boolean isNANPACountry(String regionCode) {
    return nanpaCountries.contains(regionCode);
  }

  /**
   * Convenience wrapper around isPossibleNumberWithReason. Instead of returning the reason for
   * failure, this method returns a boolean value.
   * @param number  the number that needs to be checked
   * @return  true if the number is possible
   */
  public boolean isPossibleNumber(PhoneNumber number) {
    return isPossibleNumberWithReason(number) == ValidationResult.IS_POSSIBLE;
  }

  /**
   * Check whether countryCode represents the country calling code from a country whose national
   * significant number could contain a leading zero. An example of such a country is Italy.
   */
  public static boolean isLeadingZeroCountry(int countryCode) {
    return LEADING_ZERO_COUNTRIES.contains(countryCode);
  }

  /**
   * Check whether a phone number is a possible number. It provides a more lenient check than
   * isValidNumber in the following sense:
   *   1. It only checks the length of phone numbers. In particular, it doesn't check starting
   *      digits of the number.
   *   2. It doesn't attempt to figure out the type of the number, but uses general rules which
   *      applies to all types of phone numbers in a country. Therefore, it is much faster than
   *      isValidNumber.
   *   3. For fixed line numbers, many countries have the concept of area code, which together with
   *      subscriber number constitute the national significant number. It is sometimes okay to dial
   *      the subscriber number only when dialing in the same area. This function will return
   *      true if the subscriber-number-only version is passed in. On the other hand, because
   *      isValidNumber validates using information on both starting digits (for fixed line
   *      numbers, that would most likely be area codes) and length (obviously includes the
   *      length of area codes for fixed line numbers), it will return false for the
   *      subscriber-number-only version.
   *
   * @param number  the number that needs to be checked
   * @return  a ValidationResult object which indicates whether the number is possible
   */
  public ValidationResult isPossibleNumberWithReason(PhoneNumber number) {
    String nationalNumber = getUnformattedNationalNumber(number);
    int countryCode = number.getCountryCode();
    // Note: For Russian Fed and NANPA numbers, we just use the rules from the default region (US or
    // Russia) since the getRegionCodeForNumber will not work if the number is possible but not
    // valid. This would need to be revisited if the possible number pattern ever differed between
    // various countries within those plans.
    String regionCode = getRegionCodeForCountryCode(countryCode);
    if (!isValidRegionCode(regionCode, countryCode, nationalNumber)) {
      return ValidationResult.INVALID_COUNTRY_CODE;
    }
    PhoneNumberDesc generalNumDesc = getMetadataForRegion(regionCode).getGeneralDesc();
    String possibleNumberPattern = generalNumDesc.getPossibleNumberPattern();
    Matcher m = Pattern.compile(possibleNumberPattern).matcher(nationalNumber);
    if (m.lookingAt()) {
      return (m.end() == nationalNumber.length()) ? ValidationResult.IS_POSSIBLE
                                                  : ValidationResult.TOO_LONG;
    } else {
      return ValidationResult.TOO_SHORT;
    }
  }

  /**
   * Check whether a phone number is a possible number given a number in the form of a string, and
   * the country where the number could be dialed from. It provides a more lenient check than
   * isValidNumber. See isPossibleNumber(PhoneNumber number) for details.
   *
   * This method first parses the number, then invokes isPossibleNumber(PhoneNumber number) with the
   * resultant PhoneNumber object.
   *
   * @param number  the number that needs to be checked, in the form of a string
   * @param countryDialingFrom  the ISO 3166-1 two-letter country code that denotes
   *            the country that we are expecting the number to be dialed from.
   *            Note this is different from the country where the number belongs.
   *            For example, the number +1 650 253 0000 is a number that belongs to US.
   *            When written in this form, it could be dialed from any country.
   *            When it is written as 00 1 650 253 0000, it could be dialed from
   *            any country which has international prefix 00. When it is written as
   *            650 253 0000, it could only be dialed from US, and when written as
   *            253 0000, it could only be dialed from US (Mountain View, CA, to be
   *            more specific).
   * @return  true if the number is possible
   */
  public boolean isPossibleNumber(String number, String countryDialingFrom) {
    try {
      return isPossibleNumber(parse(number, countryDialingFrom));
    } catch (NumberParseException e) {
      return false;
    }
  }

  /**
   * Gets an AsYouTypeFormatter for the specific country. Note this function doesn't attempt to
   * figure out the types of phone number being entered on the fly due to performance reasons.
   * Instead, it tries to apply a standard format to all types of phone numbers. For countries
   * where different types of phone numbers follow different formats, the formatter returned
   * will do no formatting but output exactly what is fed into the inputDigit method.
   *
   * If the type of the phone number being entered is known beforehand, use
   * getAsYouTypeFormatterByType instead.
   *
   * @param regionCode  the ISO 3166-1 two-letter country code that denotes the country/region
   *                    where the phone number is being entered
   * @return  an AsYouTypeFormatter object, which could be used to format phone numbers in the
   *     specific country "as you type"
   */
  public AsYouTypeFormatter getAsYouTypeFormatter(String regionCode) {
    return new AsYouTypeFormatter(regionCode);
  }

  // Extracts country code from fullNumber, returns it and places the remaining number in
  // nationalNumber. It assumes that the leading plus sign or IDD has already been removed. Returns
  // 0 if fullNumber doesn't start with a valid country code, and leaves nationalNumber unmodified.
  int extractCountryCode(StringBuffer fullNumber, StringBuffer nationalNumber) {
    int potentialCountryCode;
    for (int i = 1; i <= 3; i++) {
      potentialCountryCode = Integer.parseInt(fullNumber.substring(0, i));
      if (countryCodeToRegionCodeMap.containsKey(potentialCountryCode)) {
        nationalNumber.append(fullNumber.substring(i));
        return potentialCountryCode;
      }
    }
    return 0;
  }

  /**
   * Tries to extract a country code from a number. This method will return zero if no country code
   * is considered to be present. Country codes are extracted in the following ways:
   *     - by stripping the international dialing prefix of the country the person is dialing from,
   *       if this is present in the number, and looking at the next digits
   *     - by stripping the '+' sign if present and then looking at the next digits
   *     - by comparing the start of the number and the country code of the default region. If the
   *       number is not considered possible for the numbering plan of the default region initially,
   *       but starts with the country code of this region, validation will be reattempted after
   *       stripping this country code. If this number is considered a possible number, then the
   *       first digits will be considered the country code and removed as such.
   *
   * It will throw a NumberParseException if the number starts with a '+' but the country code
   * supplied after this does not match that of any known country.
   *
   * @param number  non-normalized telephone number that we wish to extract a country
   *     code from - may begin with '+'
   * @param defaultRegionMetadata  metadata about the region this number may be from
   * @param nationalNumber  a string buffer to store the national significant number in, in the case
   *     that a country code was extracted. The number is appended to any existing contents. If no
   *     country code was extracted, this will be left unchanged.
   * @return  the country code extracted or 0 if none could be extracted
   */
  @VisibleForTesting
  int maybeExtractCountryCode(String number, PhoneMetadata defaultRegionMetadata,
                              StringBuffer nationalNumber)
      throws NumberParseException {
    StringBuffer fullNumber = new StringBuffer(number);
    // Set the default prefix to be something that will never match.
    String possibleCountryIddPrefix = "NonMatch";
    if (defaultRegionMetadata != null) {
      possibleCountryIddPrefix = defaultRegionMetadata.getInternationalPrefix();
    }
    if (maybeStripInternationalPrefixAndNormalize(fullNumber, possibleCountryIddPrefix)) {
      if (fullNumber.length() < MIN_LENGTH_FOR_NSN) {
        throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                                       "Phone number had an IDD, but after this was not "
                                       + "long enough to be a viable phone number.");
      }
      int potentialCountryCode = extractCountryCode(fullNumber, nationalNumber);
      if (potentialCountryCode != 0) {
        return potentialCountryCode;
      }

      // If this fails, they must be using a strange country code that we don't recognize, or
      // that doesn't exist.
      throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                                     "Country code supplied was not recognised.");
    } else if (defaultRegionMetadata != null) {
      // Check to see if the number is valid for the default region already. If not, we check to
      // see if the country code for the default region is present at the start of the number.
      Pattern validNumberPattern =
          Pattern.compile(defaultRegionMetadata.getGeneralDesc().getNationalNumberPattern());
      if (!validNumberPattern.matcher(fullNumber).matches()) {
        int defaultCountryCode = defaultRegionMetadata.getCountryCode();
        String defaultCountryCodeString = String.valueOf(defaultCountryCode);
        String normalizedNumber = fullNumber.toString();
        if (normalizedNumber.startsWith(defaultCountryCodeString)) {
          // If so, strip this, and see if the resultant number is valid.
          StringBuffer potentialNationalNumber =
              new StringBuffer(normalizedNumber.substring(defaultCountryCodeString.length()));
          maybeStripNationalPrefix(
              potentialNationalNumber,
              defaultRegionMetadata.getNationalPrefixForParsing(),
              defaultRegionMetadata.getNationalPrefixTransformRule(),
              validNumberPattern);
          if (validNumberPattern.matcher(potentialNationalNumber).matches()) {
            nationalNumber.append(potentialNationalNumber);
            return defaultCountryCode;
          }
        }
      }
    }
    // No country code present.
    return 0;
  }

  /**
   * Strips the IDD from the start of the number if present. Helper function used by
   * maybeStripInternationalPrefixAndNormalize.
   */
  private boolean parsePrefixAsIdd(Pattern iddPattern,
                                   StringBuffer number) {
    Matcher m = iddPattern.matcher(number);
    if (m.lookingAt()) {
      int matchEnd = m.end();
      // Only strip this if the first digit after the match is not a 0, since country codes cannot
      // begin with 0.
      Matcher digitMatcher = CAPTURING_DIGIT_PATTERN.matcher(number.substring(matchEnd));
      if (digitMatcher.find()) {
        String normalizedGroup = normalizeHelper(digitMatcher.group(1), DIGIT_MAPPINGS, true);
        if (normalizedGroup.equals("0")) {
          return false;
        }
      }
      number.delete(0, matchEnd);
      return true;
    }
    return false;
  }

  /**
   * Strips any international prefix (such as +, 00, 011) present in the number provided, normalizes
   *     the resulting number, and indicates if an international prefix was present.
   *
   * @param number  the non-normalized telephone number that we wish to strip any international
   *     dialing prefix from
   * @param possibleIddPrefix  the international direct dialing prefix from the country we
   *     think this number may be dialed in
   * @return  true if an international dialing prefix could be removed from the number, otherwise
   *     false if the number did not seem to be in international format
   */
  @VisibleForTesting
  boolean maybeStripInternationalPrefixAndNormalize(StringBuffer number, String possibleIddPrefix) {
    if (number.length() == 0) {
      return false;
    }
    if (number.charAt(0) == PLUS_SIGN) {
      number.deleteCharAt(0);
      // Can now normalize the rest of the number since we've consumed the "+" sign at the start.
      normalize(number);
      return true;
    }
    // Attempt to parse the first digits as an international prefix.
    Pattern iddPattern = Pattern.compile(possibleIddPrefix);
    if (parsePrefixAsIdd(iddPattern, number)) {
      normalize(number);
      return true;
    }
    // If still not found, then try and normalize the number and then try again. This shouldn't be
    // done before, since non-numeric characters (+ and ~) may legally be in the international
    // prefix.
    normalize(number);
    return parsePrefixAsIdd(iddPattern, number);
  }

  /**
   * Strips any national prefix (such as 0, 1) present in the number provided.
   *
   * @param number  the normalized telephone number that we wish to strip any national
   *     dialing prefix from
   * @param possibleNationalPrefix  a regex that represents the national direct dialing prefix
   *     from the country we think this number may be dialed in
   * @param transformRule  the string that specifies how number should be transformed according
   *     to the regex specified in possibleNationalPrefix
   * @param nationalNumberRule  a regular expression that specifies what a valid phonenumber from
   *     this region should look like after any national prefix was stripped or transformed
   */
  @VisibleForTesting
  void maybeStripNationalPrefix(StringBuffer number, String possibleNationalPrefix,
                                String transformRule, Pattern nationalNumberRule) {
    int numberLength = number.length();
    if (numberLength == 0 || possibleNationalPrefix.equals("")) {
      // Early return for numbers of zero length.
      return;
    }
    // Attempt to parse the first digits as a national prefix.
    Matcher m = Pattern.compile(possibleNationalPrefix).matcher(number);
    if (m.lookingAt()) {
      // m.group(1) == null implies nothing was captured by the capturing groups in
      // possibleNationalPrefix; therefore, no transformation is necessary, and we
      // just remove the national prefix.
      if (transformRule == null || transformRule.equals("") || m.group(1) == null) {
        // Check that the resultant number is viable. If not, return.
        Matcher nationalNumber = nationalNumberRule.matcher(number.substring(m.end()));
        if (!nationalNumber.matches()) {
          return;
        }
        number.delete(0, m.end());
      } else {
        // Check that the resultant number is viable. If not, return. Check this by copying the
        // string buffer and making the transformation on the copy first.
        StringBuffer transformedNumber = new StringBuffer(number);
        transformedNumber.replace(0, numberLength, m.replaceFirst(transformRule));
        Matcher nationalNumber = nationalNumberRule.matcher(transformedNumber.toString());
        if (!nationalNumber.matches()) {
          return;
        }
        number.replace(0, number.length(), transformedNumber.toString());
      }
    }
  }

  /**
   * Strips any extension (as in, the part of the number dialled after the call is connected,
   * usually indicated with extn, ext, x or similar) from the end of the number, and returns it.
   *
   * @param number  the non-normalized telephone number that we wish to strip the extension from
   * @return        the phone extension
   */
  @VisibleForTesting
  String maybeStripExtension(StringBuffer number) {
    Matcher m = EXTN_PATTERN.matcher(number);
    // If we find a potential extension, and the number preceding this is a viable number, we assume
    // it is an extension.
    if (m.find() && isViablePhoneNumber(number.substring(0, m.start()))) {
      // The numbers are captured into groups in the regular expression.
      for (int i = 1; i <= m.groupCount(); i++) {
        if (m.group(i) != null) {
          // We go through the capturing groups until we find one that captured some digits. If none
          // did, then we will return the empty string.
          String extension = m.group(i);
          number.delete(m.start(), number.length());
          return extension;
        }
      }
    }
    return "";
  }

  /**
   * Parses a string and returns it in proto buffer format. This method will throw a
   * NumberParseException exception if the number is not considered to be a possible number. Note
   * that validation of whether the number is actually a valid number for a particular
   * country/region is not performed. This can be done separately with isValidNumber.
   *
   * @param numberToParse     number that we are attempting to parse. This can contain formatting
   *                          such as +, ( and -, as well as a phone number extension.
   * @param defaultCountry    the ISO 3166-1 two-letter country code that denotes the country that
   *                          we are expecting the number to be from. This is only used
   *                          if the number being parsed is not written in international format.
   *                          The country code for the number in this case would be stored as that
   *                          of the default country supplied.
   * @return                  a phone number proto buffer filled with the parsed number
   * @throws NumberParseException  if the string is not considered to be a viable phone number or if
   *                               no default country was supplied
   */
  public PhoneNumber parse(String numberToParse, String defaultCountry)
      throws NumberParseException {
    if (defaultCountry == null || defaultCountry.equals("ZZ")) {
      throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                                     "No default country was supplied.");
    }
    return parseHelper(numberToParse, defaultCountry, false);
  }

  /**
   * Parses a string and returns it in proto buffer format. This method differs from parse() in that
   * it always populates the raw_input field of the protocol buffer with numberToParse.
   *
   * @param numberToParse     number that we are attempting to parse. This can contain formatting
   *                          such as +, ( and -, as well as a phone number extension.
   * @param defaultCountry    the ISO 3166-1 two-letter country code that denotes the country that
   *                          we are expecting the number to be from. This is only used
   *                          if the number being parsed is not written in international format.
   *                          The country code for the number in this case would be stored as that
   *                          of the default country supplied.
   * @return                  a phone number proto buffer filled with the parsed number
   * @throws NumberParseException  if the string is not considered to be a viable phone number or if
   *                               no default country was supplied
   */
  public PhoneNumber parseAndKeepRawInput(String numberToParse, String defaultCountry)
      throws NumberParseException {
    if (defaultCountry == null || defaultCountry.equals("ZZ")) {
      throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                                     "No default country was supplied.");
    }
    return parseHelper(numberToParse, defaultCountry, true);
  }

  /**
   * As no equals method is implemented for MessageLite, we implement our own equals method here
   * to compare the serialized data.
   */
  @VisibleForTesting
  static Boolean areSameMessages(MessageLite message1, MessageLite message2) {
    if (message1 == null && message2 == null) {
      return true;
    }
    if (message1 == null || message2 == null) {
      return false;
    }
    OutputStream output1 = new ByteArrayOutputStream();
    OutputStream output2 = new ByteArrayOutputStream();
    try {
      message1.writeTo(output1);
      message2.writeTo(output2);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    }

    return output1.toString().equals(output2.toString());
  }


  /**
   * Parses a string and returns it in proto buffer format. This method is the same as the public
   * parse() method, with the exception that it  allows the default country to be null, for use by
   * isNumberMatch().
   */
  private PhoneNumber parseHelper(String numberToParse, String defaultCountry,
                                  Boolean keepRawInput)
      throws NumberParseException {
    // Extract a possible number from the string passed in (this strips leading characters that
    // could not be the start of a phone number.)
    String number = extractPossibleNumber(numberToParse);
    if (!isViablePhoneNumber(number)) {
      throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER,
                                     "The string supplied did not seem to be a phone number.");
    }

    PhoneNumber.Builder phoneNumber = PhoneNumber.newBuilder();
    if (keepRawInput) {
      phoneNumber.setRawInput(numberToParse);
    }
    StringBuffer nationalNumber = new StringBuffer(number);
    // Attempt to parse extension first, since it doesn't require country-specific data and we want
    // to have the non-normalised number here.
    String extension = maybeStripExtension(nationalNumber);
    if (!extension.equals("")) {
      phoneNumber.setExtension(extension);
    }

    PhoneMetadata countryMetadata = getMetadataForRegion(defaultCountry);
    // Check to see if the number is given in international format so we know whether this number is
    // from the default country or not.
    StringBuffer normalizedNationalNumber = new StringBuffer();
    // been created, and just remove the prefix, rather than taking in a string and then outputting
    // a string buffer.
    int countryCode = maybeExtractCountryCode(nationalNumber.toString(), countryMetadata,
                                              normalizedNationalNumber);
    if (countryCode != 0) {
      String phoneNumberRegion = getRegionCodeForCountryCode(countryCode);
      countryMetadata = getMetadataForRegion(phoneNumberRegion);
    } else {
      // If no extracted country code, use the region supplied instead. The national number is just
      // the normalized version of the number we were given to parse.
      normalize(nationalNumber);
      normalizedNationalNumber.append(nationalNumber);
      if (defaultCountry != null) {
        countryCode = countryMetadata.getCountryCode();
      }
    }
    if (normalizedNationalNumber.length() < MIN_LENGTH_FOR_NSN) {
      throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_NSN,
                                     "The string supplied is too short to be a phone number.");
    }
    if (countryMetadata != null) {
      Pattern validNumberPattern =
          Pattern.compile(countryMetadata.getGeneralDesc().getNationalNumberPattern());
      maybeStripNationalPrefix(normalizedNationalNumber,
                               countryMetadata.getNationalPrefixForParsing(),
                               countryMetadata.getNationalPrefixTransformRule(),
                               validNumberPattern);
    }
    phoneNumber.setCountryCode(countryCode);
    if (isLeadingZeroCountry(countryCode) &&
        normalizedNationalNumber.charAt(0) == '0') {
      phoneNumber.setItalianLeadingZero(true);
    }
    int lengthOfNationalNumber = normalizedNationalNumber.length();
    if (lengthOfNationalNumber < MIN_LENGTH_FOR_NSN) {
      throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_NSN,
                                     "The string supplied is too short to be a "
                                     + "phone number.");
    }
    if (lengthOfNationalNumber > MAX_LENGTH_FOR_NSN) {
      throw new NumberParseException(NumberParseException.ErrorType.TOO_LONG,
                                     "The string supplied is too long to be a "
                                     + "phone number.");
    }
    phoneNumber.setNationalNumber(Long.parseLong(normalizedNationalNumber.toString()));
    return phoneNumber.build();
  }

  /**
   * Takes two phone numbers and compares them for equality.
   *
   * Returns EXACT_MATCH if the country code, NSN, presence of a leading zero for Italian numbers
   * and any extension present are the same.
   * Returns NSN_MATCH if either or both has no country specified, and the NSNs and extensions are
   * the same.
   * Returns SHORT_NSN_MATCH if either or both has no country specified, or the country specified
   * is the same, and one NSN could be a shorter version of the other number. This includes the case
   * where one has an extension specified, and the other does not.
   * Returns NO_MATCH otherwise.
   * For example, the numbers +1 345 657 1234 and 657 1234 are a SHORT_NSN_MATCH.
   * The numbers +1 345 657 1234 and 345 657 are a NO_MATCH.
   *
   * @param firstNumberIn  first number to compare
   * @param secondNumberIn  second number to compare
   *
   * @return  NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH or EXACT_MATCH depending on the level of equality
   *     of the two numbers, described in the method definition.
   */
  public MatchType isNumberMatch(PhoneNumber firstNumberIn, PhoneNumber secondNumberIn) {
    // Make copies of the phone number so that the numbers passed in are not edited.
    PhoneNumber.Builder firstNumber = PhoneNumber.newBuilder();
    firstNumber.mergeFrom(firstNumberIn);
    PhoneNumber.Builder secondNumber = PhoneNumber.newBuilder();
    secondNumber.mergeFrom(secondNumberIn);
    // First clear raw_input field and any empty-string extensions so that we can use the
    // proto-buffer equality method.
    firstNumber.clearRawInput();
    secondNumber.clearRawInput();
    if (firstNumber.hasExtension() &&
        firstNumber.getExtension().equals("")) {
        firstNumber.clearExtension();
    }
    if (secondNumber.hasExtension() &&
        secondNumber.getExtension().equals("")) {
        secondNumber.clearExtension();
    }

    PhoneNumber number1 = firstNumber.build();
    PhoneNumber number2 = secondNumber.build();

    // Early exit if both had extensions and these are different.
    if (number1.hasExtension() && number2.hasExtension() &&
        !number1.getExtension().equals(number2.getExtension())) {
      return MatchType.NO_MATCH;
    }
    int firstNumberCountryCode = number1.getCountryCode();
    int secondNumberCountryCode = number2.getCountryCode();
    // Both had country code specified.
    if (firstNumberCountryCode != 0 && secondNumberCountryCode != 0) {
      if (areSameMessages(number1, number2)) {
        return MatchType.EXACT_MATCH;
      } else if (firstNumberCountryCode == secondNumberCountryCode &&
                 isNationalNumberSuffixOfTheOther(number1, number2)) {
        // A SHORT_NSN_MATCH occurs if there is a difference because of the presence or absence of
        // an 'Italian leading zero', the presence or absence of an extension, or one NSN being a
        // shorter variant of the other.
        return MatchType.SHORT_NSN_MATCH;
      }
      // This is not a match.
      return MatchType.NO_MATCH;
    }
    // Checks cases where one or both country codes were not specified. To make equality checks
    // easier, we first set the country codes to be equal.
    PhoneNumber newNumber =
        PhoneNumber.newBuilder(number1).setCountryCode(secondNumberCountryCode).build();
    // If all else was the same, then this is an NSN_MATCH.
    if (areSameMessages(newNumber, number2)) {
      return MatchType.NSN_MATCH;
    }
    if (isNationalNumberSuffixOfTheOther(newNumber, number2)) {
      return MatchType.SHORT_NSN_MATCH;
    }
  return MatchType.NO_MATCH;
  }

  // Returns true when one national number is the suffix of the other or both are the same.
  private boolean isNationalNumberSuffixOfTheOther(PhoneNumber firstNumber,
                                                   PhoneNumber secondNumber) {
    String firstNumberNationalNumber = String.valueOf(firstNumber.getNationalNumber());
    String secondNumberNationalNumber = String.valueOf(secondNumber.getNationalNumber());
    // Note that endsWith returns true if the numbers are equal.
    return firstNumberNationalNumber.endsWith(secondNumberNationalNumber) ||
           secondNumberNationalNumber.endsWith(firstNumberNationalNumber);
  }

  /**
   * Takes two phone numbers as strings and compares them for equality. This is a convenience
   * wrapper for isNumberMatch(PhoneNumber firstNumber, PhoneNumber secondNumber). No default region
   * is known.
   *
   * @param firstNumber  first number to compare. Can contain formatting, and can have country code
   *     specified with + at the start.
   * @param secondNumber  second number to compare. Can contain formatting, and can have country
   *     code specified with + at the start.
   * @return  NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH, EXACT_MATCH. See isNumberMatch(PhoneNumber
   *     firstNumber, PhoneNumber secondNumber) for more details.
   * @throws NumberParseException  if either number is not considered to be a viable phone
   *     number
   */
  public MatchType isNumberMatch(String firstNumber, String secondNumber)
      throws NumberParseException {
    return isNumberMatch(parseHelper(firstNumber, null, false),
                         parseHelper(secondNumber, null, false));
  }

  /**
   * Takes two phone numbers and compares them for equality. This is a convenience wrapper for
   * isNumberMatch(PhoneNumber firstNumber, PhoneNumber secondNumber). No default region is known.
   *
   * @param firstNumber  first number to compare in proto buffer format.
   * @param secondNumber  second number to compare. Can contain formatting, and can have country
   *     code specified with + at the start.
   * @return  NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH, EXACT_MATCH. See isNumberMatch(PhoneNumber
   *     firstNumber, PhoneNumber secondNumber) for more details.
   * @throws NumberParseException  if the second number is not considered to be a viable phone
   *     number
   */
  public MatchType isNumberMatch(PhoneNumber firstNumber, String secondNumber)
      throws NumberParseException {
    return isNumberMatch(firstNumber, parseHelper(secondNumber, null, false));
  }
}
