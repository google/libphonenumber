/*
 * Copyright (C) 2009 The Libphonenumber Authors
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

import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
 * <p>If you use this library, and want to be notified about important changes, please sign up to
 * our <a href="http://groups.google.com/group/libphonenumber-discuss/about">mailing list</a>.
 *
 * NOTE: A lot of methods in this class require Region Code strings. These must be provided using
 * ISO 3166-1 two-letter country-code format. These should be in upper-case. The list of the codes
 * can be found here: http://www.iso.org/iso/english_country_names_and_code_elements
 *
 * @author Shaopeng Jia
 * @author Lara Rennie
 */
public class PhoneNumberUtil {
  /** Flags to use when compiling regular expressions for phone numbers. */
  static final int REGEX_FLAGS = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;
  // The minimum and maximum length of the national significant number.
  private static final int MIN_LENGTH_FOR_NSN = 3;
  static final int MAX_LENGTH_FOR_NSN = 15;
  // The maximum length of the country calling code.
  static final int MAX_LENGTH_COUNTRY_CODE = 3;
  static final String META_DATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProto";
  private String currentFilePrefix = META_DATA_FILE_PREFIX;
  private static final Logger LOGGER = Logger.getLogger(PhoneNumberUtil.class.getName());

  // A mapping from a country calling code to the region codes which denote the region represented
  // by that country calling code. In the case of multiple regions sharing a calling code, such as
  // the NANPA regions, the one indicated with "isMainCountryForCode" in the metadata should be
  // first.
  private Map<Integer, List<String>> countryCallingCodeToRegionCodeMap = null;

  // The set of regions the library supports.
  // There are roughly 220 of them and we set the initial capacity of the HashSet to 300 to offer a
  // load factor of roughly 0.75.
  private final Set<String> supportedRegions = new HashSet<String>(300);

  // Region-code for the unknown region.
  private static final String UNKNOWN_REGION = "ZZ";

  // The set of regions that share country calling code 1.
  // There are roughly 26 regions and we set the initial capacity of the HashSet to 35 to offer a
  // load factor of roughly 0.75.
  private final Set<String> nanpaRegions = new HashSet<String>(35);
  private static final int NANPA_COUNTRY_CODE = 1;

  // The PLUS_SIGN signifies the international prefix.
  static final char PLUS_SIGN = '+';

  private static final String RFC3966_EXTN_PREFIX = ";ext=";

  // Only upper-case variants of alpha characters are stored.
  private static final Map<Character, Character> ALPHA_MAPPINGS;

  // For performance reasons, amalgamate both into one map.
  private static final Map<Character, Character> ALPHA_PHONE_MAPPINGS;

  // Separate map of all symbols that we wish to retain when formatting alpha numbers. This
  // includes digits, ASCII letters and number grouping symbols such as "-" and " ".
  private static final Map<Character, Character> ALL_PLUS_NUMBER_GROUPING_SYMBOLS;

  static {
    // Simple ASCII digits map used to populate ALPHA_PHONE_MAPPINGS and
    // ALL_PLUS_NUMBER_GROUPING_SYMBOLS.
    HashMap<Character, Character> asciiDigitMappings = new HashMap<Character, Character>();
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

    HashMap<Character, Character> alphaMap = new HashMap<Character, Character>(40);
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

    HashMap<Character, Character> combinedMap = new HashMap<Character, Character>(100);
    combinedMap.putAll(ALPHA_MAPPINGS);
    combinedMap.putAll(asciiDigitMappings);
    ALPHA_PHONE_MAPPINGS = Collections.unmodifiableMap(combinedMap);

    HashMap<Character, Character> allPlusNumberGroupings = new HashMap<Character, Character>();
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

  // Pattern that makes it easy to distinguish whether a region has a unique international dialing
  // prefix or not. If a region has a unique international prefix (e.g. 011 in USA), it will be
  // represented as a string that contains a sequence of ASCII digits. If there are multiple
  // available international prefixes in a region, they will be represented as a regex string that
  // always contains character(s) other than ASCII digits.
  // Note this regex also includes tilde, which signals waiting for the tone.
  private static final Pattern UNIQUE_INTERNATIONAL_PREFIX =
      Pattern.compile("[\\d]+(?:[~\u2053\u223C\uFF5E][\\d]+)?");

  // Regular expression of acceptable punctuation found in phone numbers. This excludes punctuation
  // found as a leading character only.
  // This consists of dash characters, white space characters, full stops, slashes,
  // square brackets, parentheses and tildes. It also includes the letter 'x' as that is found as a
  // placeholder for carrier information in some phone numbers. Full-width variants are also
  // present.
  static final String VALID_PUNCTUATION = "-x\u2010-\u2015\u2212\u30FC\uFF0D-\uFF0F " +
      "\u00A0\u200B\u2060\u3000()\uFF08\uFF09\uFF3B\uFF3D.\\[\\]/~\u2053\u223C\uFF5E";

  private static final String DIGITS = "\\p{Nd}";
  // We accept alpha characters in phone numbers, ASCII only, upper and lower case.
  private static final String VALID_ALPHA =
      Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).replaceAll("[, \\[\\]]", "") +
      Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).toLowerCase().replaceAll("[, \\[\\]]", "");
  static final String PLUS_CHARS = "+\uFF0B";
  static final Pattern PLUS_CHARS_PATTERN = Pattern.compile("[" + PLUS_CHARS + "]+");
  private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[" + VALID_PUNCTUATION + "]+");
  private static final Pattern CAPTURING_DIGIT_PATTERN = Pattern.compile("(" + DIGITS + ")");

  // Regular expression of acceptable characters that may start a phone number for the purposes of
  // parsing. This allows us to strip away meaningless prefixes to phone numbers that may be
  // mistakenly given to us. This consists of digits, the plus symbol and arabic-indic digits. This
  // does not contain alpha characters, although they may be used later in the number. It also does
  // not include other punctuation, as this will be stripped later during parsing and is of no
  // information value when parsing a number.
  private static final String VALID_START_CHAR = "[" + PLUS_CHARS + DIGITS + "]";
  private static final Pattern VALID_START_CHAR_PATTERN = Pattern.compile(VALID_START_CHAR);

  // Regular expression of characters typically used to start a second phone number for the purposes
  // of parsing. This allows us to strip off parts of the number that are actually the start of
  // another number, such as for: (530) 583-6985 x302/x2303 -> the second extension here makes this
  // actually two phone numbers, (530) 583-6985 x302 and (530) 583-6985 x2303. We remove the second
  // extension so that the first number is parsed correctly.
  private static final String SECOND_NUMBER_START = "[\\\\/] *x";
  static final Pattern SECOND_NUMBER_START_PATTERN = Pattern.compile(SECOND_NUMBER_START);

  // Regular expression of trailing characters that we want to remove. We remove all characters that
  // are not alpha or numerical characters. The hash character is retained here, as it may signify
  // the previous block was an extension.
  private static final String UNWANTED_END_CHARS = "[[\\P{N}&&\\P{L}]&&[^#]]+$";
  static final Pattern UNWANTED_END_CHAR_PATTERN = Pattern.compile(UNWANTED_END_CHARS);

  // We use this pattern to check if the phone number has at least three letters in it - if so, then
  // we treat it as a number where some phone-number digits are represented by letters.
  private static final Pattern VALID_ALPHA_PHONE_PATTERN = Pattern.compile("(?:.*?[A-Za-z]){3}.*");

  // Regular expression of viable phone numbers. This is location independent. Checks we have at
  // least three leading digits, and only valid punctuation, alpha characters and
  // digits in the phone number. Does not include extension data.
  // The symbol 'x' is allowed here as valid punctuation since it is often used as a placeholder for
  // carrier codes, for example in Brazilian phone numbers. We also allow multiple "+" characters at
  // the start.
  // Corresponds to the following:
  // plus_sign*([punctuation]*[digits]){3,}([punctuation]|[digits]|[alpha])*
  // Note VALID_PUNCTUATION starts with a -, so must be the first in the range.
  private static final String VALID_PHONE_NUMBER =
      "[" + PLUS_CHARS + "]*(?:[" + VALID_PUNCTUATION + "]*" + DIGITS + "){3,}[" +
      VALID_PUNCTUATION + VALID_ALPHA + DIGITS + "]*";

  // Default extension prefix to use when formatting. This will be put in front of any extension
  // component of the number, after the main national number is formatted. For example, if you wish
  // the default extension formatting to be " extn: 3456", then you should specify " extn: " here
  // as the default extension prefix. This can be overridden by region-specific preferences.
  private static final String DEFAULT_EXTN_PREFIX = " ext. ";

  // Pattern to capture digits used in an extension. Places a maximum length of "7" for an
  // extension.
  private static final String CAPTURING_EXTN_DIGITS = "(" + DIGITS + "{1,7})";
  // Regexp of all possible ways to write extensions, for use when parsing. This will be run as a
  // case-insensitive regexp match. Wide character versions are also provided after each ASCII
  // version.
  private static final String EXTN_PATTERNS_FOR_PARSING;
  static final String EXTN_PATTERNS_FOR_MATCHING;
  static {
    // One-character symbols that can be used to indicate an extension.
    String singleExtnSymbolsForMatching = "x\uFF58#\uFF03~\uFF5E";
    // For parsing, we are slightly more lenient in our interpretation than for matching. Here we
    // allow a "comma" as a possible extension indicator. When matching, this is hardly ever used to
    // indicate this.
    String singleExtnSymbolsForParsing = "," + singleExtnSymbolsForMatching;

    EXTN_PATTERNS_FOR_PARSING = createExtnPattern(singleExtnSymbolsForParsing);
    EXTN_PATTERNS_FOR_MATCHING = createExtnPattern(singleExtnSymbolsForMatching);
  }

  /**
   * Helper initialiser method to create the regular-expression pattern to match extensions,
   * allowing the one-char extension symbols provided by {@code singleExtnSymbols}.
   */
  private static String createExtnPattern(String singleExtnSymbols) {
    // There are three regular expressions here. The first covers RFC 3966 format, where the
    // extension is added using ";ext=". The second more generic one starts with optional white
    // space and ends with an optional full stop (.), followed by zero or more spaces/tabs and then
    // the numbers themselves. The other one covers the special case of American numbers where the
    // extension is written with a hash at the end, such as "- 503#".
    // Note that the only capturing groups should be around the digits that you want to capture as
    // part of the extension, or else parsing will fail!
    // Canonical-equivalence doesn't seem to be an option with Android java, so we allow two options
    // for representing the accented o - the character itself, and one in the unicode decomposed
    // form with the combining acute accent.
    return (RFC3966_EXTN_PREFIX + CAPTURING_EXTN_DIGITS + "|" + "[ \u00A0\\t,]*" +
            "(?:ext(?:ensi(?:o\u0301?|\u00F3))?n?|\uFF45\uFF58\uFF54\uFF4E?|" +
            "[" + singleExtnSymbols + "]|int|anexo|\uFF49\uFF4E\uFF54)" +
            "[:\\.\uFF0E]?[ \u00A0\\t,-]*" + CAPTURING_EXTN_DIGITS + "#?|" +
            "[- ]+(" + DIGITS + "{1,5})#");
  }

  // Regexp of all known extension prefixes used by different regions followed by 1 or more valid
  // digits, for use when parsing.
  private static final Pattern EXTN_PATTERN =
      Pattern.compile("(?:" + EXTN_PATTERNS_FOR_PARSING + ")$", REGEX_FLAGS);

  // We append optionally the extension pattern to the end here, as a valid phone number may
  // have an extension prefix appended, followed by 1 or more digits.
  private static final Pattern VALID_PHONE_NUMBER_PATTERN =
      Pattern.compile(VALID_PHONE_NUMBER + "(?:" + EXTN_PATTERNS_FOR_PARSING + ")?", REGEX_FLAGS);

  private static final Pattern NON_DIGITS_PATTERN = Pattern.compile("(\\D+)");

  // The FIRST_GROUP_PATTERN was originally set to $1 but there are some countries for which the
  // first group is not used in the national pattern (e.g. Argentina) so the $1 group does not match
  // correctly.  Therefore, we use \d, so that the first group actually used in the pattern will be
  // matched.
  private static final Pattern FIRST_GROUP_PATTERN = Pattern.compile("(\\$\\d)");
  private static final Pattern NP_PATTERN = Pattern.compile("\\$NP");
  private static final Pattern FG_PATTERN = Pattern.compile("\\$FG");
  private static final Pattern CC_PATTERN = Pattern.compile("\\$CC");

  private static PhoneNumberUtil instance = null;

  // A mapping from a region code to the PhoneMetadata for that region.
  private Map<String, PhoneMetadata> regionToMetadataMap = new HashMap<String, PhoneMetadata>();

  // A cache for frequently used region-specific regular expressions.
  // As most people use phone numbers primarily from one to two countries, and there are roughly 60
  // regular expressions needed, the initial capacity of 100 offers a rough load factor of 0.75.
  private RegexCache regexCache = new RegexCache(100);

  /**
   * INTERNATIONAL and NATIONAL formats are consistent with the definition in ITU-T Recommendation
   * E123. For example, the number of the Google Switzerland office will be written as
   * "+41 44 668 1800" in INTERNATIONAL format, and as "044 668 1800" in NATIONAL format.
   * E164 format is as per INTERNATIONAL format but with no formatting applied, e.g. +41446681800.
   * RFC3966 is as per INTERNATIONAL format, but with all spaces and other separating symbols
   * replaced with a hyphen, and with any phone number extension appended with ";ext=".
   *
   * Note: If you are considering storing the number in a neutral format, you are highly advised to
   * use the PhoneNumber class.
   */
  public enum PhoneNumberFormat {
    E164,
    INTERNATIONAL,
    NATIONAL,
    RFC3966
  }

  /**
   * Type of phone numbers.
   */
  public enum PhoneNumberType {
    FIXED_LINE,
    MOBILE,
    // In some regions (e.g. the USA), it is impossible to distinguish between fixed-line and
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
    PAGER,
    // Used for "Universal Access Numbers" or "Company Numbers". They may be further routed to
    // specific offices, but allow one number to be used for a company.
    UAN,
    // A phone number is of type UNKNOWN when it does not fit any of the known patterns for a
    // specific region.
    UNKNOWN
  }

  /**
   * Types of phone number matches. See detailed description beside the isNumberMatch() method.
   */
  public enum MatchType {
    NOT_A_NUMBER,
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
   * Leniency when {@linkplain PhoneNumberUtil#findNumbers finding} potential phone numbers in text
   * segments. The levels here are ordered in increasing strictness.
   */
  public enum Leniency {
    /**
     * Phone numbers accepted are
     * {@linkplain PhoneNumberUtil#isPossibleNumber(Phonenumber.PhoneNumber) possible}, but not
     * necessarily {@linkplain PhoneNumberUtil#isValidNumber(Phonenumber.PhoneNumber) valid}.
     */
    POSSIBLE {
      @Override
      boolean verify(PhoneNumber number, String candidate, PhoneNumberUtil util) {
        return util.isPossibleNumber(number);
      }
    },
    /**
     * Phone numbers accepted are
     * {@linkplain PhoneNumberUtil#isPossibleNumber(Phonenumber.PhoneNumber) possible} and
     * {@linkplain PhoneNumberUtil#isValidNumber(Phonenumber.PhoneNumber) valid}.
     */
    VALID {
      @Override
      boolean verify(PhoneNumber number, String candidate, PhoneNumberUtil util) {
        if (!util.isValidNumber(number)) {
          return false;
        }
        return containsOnlyValidXChars(number, candidate, util);
      }
    },
    /**
     * Phone numbers accepted are {@linkplain PhoneNumberUtil#isValidNumber(PhoneNumber) valid} and
     * are grouped in a possible way for this locale. For example, a US number written as
     * "65 02 53 00 00" and "650253 0000" are not accepted at this leniency level, whereas
     * "650 253 0000", "650 2530000" or "6502530000" are.
     * Numbers with more than one '/' symbol are also dropped at this level.
     * <p>
     * Warning: This level might result in lower coverage especially for regions outside of country
     * code "+1". If you are not sure about which level to use, email the discussion group
     * libphonenumber-discuss@googlegroups.com.
     */
    STRICT_GROUPING {
      @Override
      boolean verify(PhoneNumber number, String candidate, PhoneNumberUtil util) {
        if (!util.isValidNumber(number) ||
            !containsOnlyValidXChars(number, candidate, util) ||
            containsMoreThanOneSlash(candidate)) {
          return false;
        }
        // TODO: Evaluate how this works for other locales (testing has been
        // limited to NANPA regions) and optimise if necessary.
        String[] formattedNumberGroups = getNationalNumberGroups(util, number);
        StringBuilder normalizedCandidate = normalizeDigits(candidate,
                                                            true /* keep strip non-digits */);
        int fromIndex = 0;
        // Check each group of consecutive digits are not broken into separate groups in the
        // {@code candidate} string.
        for (int i = 0; i < formattedNumberGroups.length; i++) {
          // Fails if the substring of {@code candidate} starting from {@code fromIndex} doesn't
          // contain the consecutive digits in formattedNumberGroups[i].
          fromIndex = normalizedCandidate.indexOf(formattedNumberGroups[i], fromIndex);
          if (fromIndex < 0) {
            return false;
          }
          // Moves {@code fromIndex} forward.
          fromIndex += formattedNumberGroups[i].length();
          if (i == 0 && fromIndex < normalizedCandidate.length()) {
            // We are at the position right after the NDC.
            if (Character.isDigit(normalizedCandidate.charAt(fromIndex))) {
              // This means there is no formatting symbol after the NDC. In this case, we only
              // accept the number if there is no formatting symbol at all in the number, except
              // for extensions.
              String nationalSignificantNumber = util.getNationalSignificantNumber(number);
              return normalizedCandidate.substring(fromIndex - formattedNumberGroups[i].length())
                  .startsWith(nationalSignificantNumber);
            }
          }
        }
        // The check here makes sure that we haven't mistakenly already used the extension to
        // match the last group of the subscriber number. Note the extension cannot have
        // formatting in-between digits.
        return normalizedCandidate.substring(fromIndex).contains(number.getExtension());
      }
    },
    /**
     * Phone numbers accepted are {@linkplain PhoneNumberUtil#isValidNumber(PhoneNumber) valid} and
     * are grouped in the same way that we would have formatted it, or as a single block. For
     * example, a US number written as "650 2530000" is not accepted at this leniency level, whereas
     * "650 253 0000" or "6502530000" are.
     * Numbers with more than one '/' symbol are also dropped at this level.
     * <p>
     * Warning: This level might result in lower coverage especially for regions outside of country
     * code "+1". If you are not sure about which level to use, email the discussion group
     * libphonenumber-discuss@googlegroups.com.
     */
    EXACT_GROUPING {
      @Override
      boolean verify(PhoneNumber number, String candidate, PhoneNumberUtil util) {
        if (!util.isValidNumber(number) ||
            !containsOnlyValidXChars(number, candidate, util) ||
            containsMoreThanOneSlash(candidate)) {
          return false;
        }
        // TODO: Evaluate how this works for other locales (testing has been
        // limited to NANPA regions) and optimise if necessary.
        StringBuilder normalizedCandidate = normalizeDigits(candidate,
                                                            true /* keep strip non-digits */);
        String[] candidateGroups =
            NON_DIGITS_PATTERN.split(normalizedCandidate.toString());
        // Set this to the last group, skipping it if the number has an extension.
        int candidateNumberGroupIndex =
            number.hasExtension() ? candidateGroups.length - 2 : candidateGroups.length - 1;
        // First we check if the national significant number is formatted as a block.
        // We use contains and not equals, since the national significant number may be present with
        // a prefix such as a national number prefix, or the country code itself.
        if (candidateGroups.length == 1 ||
            candidateGroups[candidateNumberGroupIndex].contains(
                util.getNationalSignificantNumber(number))) {
          return true;
        }
        String[] formattedNumberGroups = getNationalNumberGroups(util, number);
        // Starting from the end, go through in reverse, excluding the first group, and check the
        // candidate and number groups are the same.
        for (int formattedNumberGroupIndex = (formattedNumberGroups.length - 1);
             formattedNumberGroupIndex > 0 && candidateNumberGroupIndex >= 0;
             formattedNumberGroupIndex--, candidateNumberGroupIndex--) {
          if (!candidateGroups[candidateNumberGroupIndex].equals(
              formattedNumberGroups[formattedNumberGroupIndex])) {
            return false;
          }
        }
        // Now check the first group. There may be a national prefix at the start, so we only check
        // that the candidate group ends with the formatted number group.
        return (candidateNumberGroupIndex >= 0 &&
                candidateGroups[candidateNumberGroupIndex].endsWith(formattedNumberGroups[0]));
      }
    };

    /**
     * Helper method to get the national-number part of a number, formatted without any national
     * prefix, and return it as a set of digit blocks that would be formatted together.
     */
    private static String[] getNationalNumberGroups(PhoneNumberUtil util, PhoneNumber number) {
      // This will be in the format +CC-DG;ext=EXT where DG represents groups of digits.
      String rfc3966Format = util.format(number, PhoneNumberFormat.RFC3966);
      // We remove the extension part from the formatted string before splitting it into different
      // groups.
      int endIndex = rfc3966Format.indexOf(';');
      if (endIndex < 0) {
        endIndex = rfc3966Format.length();
      }
      // The country-code will have a '-' following it.
      int startIndex = rfc3966Format.indexOf('-') + 1;
      return rfc3966Format.substring(startIndex, endIndex).split("-");
    }

    private static boolean containsMoreThanOneSlash(String candidate) {
      int firstSlashIndex = candidate.indexOf('/');
      return (firstSlashIndex > 0 && candidate.substring(firstSlashIndex + 1).contains("/"));
    }

    private static boolean containsOnlyValidXChars(
        PhoneNumber number, String candidate, PhoneNumberUtil util) {
      // The characters 'x' and 'X' can be (1) a carrier code, in which case they always precede the
      // national significant number or (2) an extension sign, in which case they always precede the
      // extension number. We assume a carrier code is more than 1 digit, so the first case has to
      // have more than 1 consecutive 'x' or 'X', whereas the second case can only have exactly 1
      // 'x' or 'X'. We ignore the character if it appears as the last character of the string.
      for (int index = 0; index < candidate.length() - 1; index++) {
        char charAtIndex = candidate.charAt(index);
        if (charAtIndex == 'x' || charAtIndex == 'X') {
          char charAtNextIndex = candidate.charAt(index + 1);
          if (charAtNextIndex == 'x' || charAtNextIndex == 'X') {
            // This is the carrier code case, in which the 'X's always precede the national
            // significant number.
            index++;
            if (util.isNumberMatch(number, candidate.substring(index)) != MatchType.NSN_MATCH) {
              return false;
            }
          // This is the extension sign case, in which the 'x' or 'X' should always precede the
          // extension number.
          } else if (!PhoneNumberUtil.normalizeDigitsOnly(candidate.substring(index)).equals(
              number.getExtension())) {
              return false;
          }
        }
      }
      return true;
    }

    /** Returns true if {@code number} is a verified number according to this leniency. */
    abstract boolean verify(PhoneNumber number, String candidate, PhoneNumberUtil util);
  }

  /**
   * This class implements a singleton, so the only constructor is private.
   */
  private PhoneNumberUtil() {
  }

  private void init(String filePrefix) {
    currentFilePrefix = filePrefix;
    for (List<String> regionCodes : countryCallingCodeToRegionCodeMap.values()) {
      supportedRegions.addAll(regionCodes);
    }
    nanpaRegions.addAll(countryCallingCodeToRegionCodeMap.get(NANPA_COUNTRY_CODE));
  }

  private void loadMetadataForRegionFromFile(String filePrefix, String regionCode) {
    InputStream source =
        PhoneNumberUtil.class.getResourceAsStream(filePrefix + "_" + regionCode);
    ObjectInputStream in;
    try {
      in = new ObjectInputStream(source);
      PhoneMetadataCollection metadataCollection = new PhoneMetadataCollection();
      metadataCollection.readExternal(in);
      for (PhoneMetadata metadata : metadataCollection.getMetadataList()) {
        regionToMetadataMap.put(regionCode, metadata);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    }
  }

  /**
   * Attempts to extract a possible number from the string passed in. This currently strips all
   * leading characters that cannot be used to start a phone number. Characters that can be used to
   * start a phone number are defined in the VALID_START_CHAR_PATTERN. If none of these characters
   * are found in the number passed in, an empty string is returned. This function also attempts to
   * strip off any alternative extensions or endings if two or more are present, such as in the case
   * of: (530) 583-6985 x302/x2303. The second extension here makes this actually two phone numbers,
   * (530) 583-6985 x302 and (530) 583-6985 x2303. We remove the second extension so that the first
   * number is parsed correctly.
   *
   * @param number  the string that might contain a phone number
   * @return        the number, stripped of any non-phone-number prefix (such as "Tel:") or an empty
   *                string if no character used to start phone numbers (such as + or any digit) is
   *                found in the number
   */
  static String extractPossibleNumber(String number) {
    Matcher m = VALID_START_CHAR_PATTERN.matcher(number);
    if (m.find()) {
      number = number.substring(m.start());
      // Remove trailing non-alpha non-numerical characters.
      Matcher trailingCharsMatcher = UNWANTED_END_CHAR_PATTERN.matcher(number);
      if (trailingCharsMatcher.find()) {
        number = number.substring(0, trailingCharsMatcher.start());
        LOGGER.log(Level.FINER, "Stripped trailing characters: " + number);
      }
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
   *   Punctuation is stripped.
   *   For ALPHA/VANITY numbers:
   *   Letters are converted to their numeric representation on a telephone keypad. The keypad
   *       used here is the one defined in ITU Recommendation E.161. This is only done if there are
   *       3 or more letters in the number, to lessen the risk that such letters are typos.
   *   For other numbers:
   *   Wide-ascii digits are converted to normal ASCII (European) digits.
   *   Arabic-Indic numerals are converted to European numerals.
   *   Spurious alpha characters are stripped.
   *
   * @param number  a string of characters representing a phone number
   * @return        the normalized string version of the phone number
   */
  static String normalize(String number) {
    Matcher m = VALID_ALPHA_PHONE_PATTERN.matcher(number);
    if (m.matches()) {
      return normalizeHelper(number, ALPHA_PHONE_MAPPINGS, true);
    } else {
      return normalizeDigitsOnly(number);
    }
  }

  /**
   * Normalizes a string of characters representing a phone number. This is a wrapper for
   * normalize(String number) but does in-place normalization of the StringBuilder provided.
   *
   * @param number  a StringBuilder of characters representing a phone number that will be
   *     normalized in place
   */
  static void normalize(StringBuilder number) {
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
    return normalizeDigits(number, false /* strip non-digits */).toString();
  }

  private static StringBuilder normalizeDigits(String number, boolean keepNonDigits) {
    StringBuilder normalizedDigits = new StringBuilder(number.length());
    for (char c : number.toCharArray()) {
      int digit = Character.digit(c, 10);
      if (digit != -1) {
        normalizedDigits.append(digit);
      } else if (keepNonDigits) {
        normalizedDigits.append(c);
      }
    }
    return normalizedDigits;
  }

  /**
   * Converts all alpha characters in a number to their respective digits on a keypad, but retains
   * existing formatting.
   */
  public static String convertAlphaCharactersInNumber(String number) {
    return normalizeHelper(number, ALPHA_PHONE_MAPPINGS, false);
  }

  /**
   * Gets the length of the geographical area code in the {@code nationalNumber_} field of the
   * PhoneNumber object passed in, so that clients could use it to split a national significant
   * number into geographical area code and subscriber number. It works in such a way that the
   * resultant subscriber number should be diallable, at least on some devices. An example of how
   * this could be used:
   *
   * <pre>
   * PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
   * PhoneNumber number = phoneUtil.parse("16502530000", "US");
   * String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(number);
   * String areaCode;
   * String subscriberNumber;
   *
   * int areaCodeLength = phoneUtil.getLengthOfGeographicalAreaCode(number);
   * if (areaCodeLength > 0) {
   *   areaCode = nationalSignificantNumber.substring(0, areaCodeLength);
   *   subscriberNumber = nationalSignificantNumber.substring(areaCodeLength);
   * } else {
   *   areaCode = "";
   *   subscriberNumber = nationalSignificantNumber;
   * }
   * </pre>
   *
   * N.B.: area code is a very ambiguous concept, so the I18N team generally recommends against
   * using it for most purposes, but recommends using the more general {@code national_number}
   * instead. Read the following carefully before deciding to use this method:
   * <ul>
   *  <li> geographical area codes change over time, and this method honors those changes;
   *    therefore, it doesn't guarantee the stability of the result it produces.
   *  <li> subscriber numbers may not be diallable from all devices (notably mobile devices, which
   *    typically requires the full national_number to be dialled in most regions).
   *  <li> most non-geographical numbers have no area codes.
   *  <li> some geographical numbers have no area codes.
   * </ul>
   * @param number  the PhoneNumber object for which clients want to know the length of the area
   *     code.
   * @return  the length of area code of the PhoneNumber object passed in.
   */
  public int getLengthOfGeographicalAreaCode(PhoneNumber number) {
    String regionCode = getRegionCodeForNumber(number);
    if (!isValidRegionCode(regionCode)) {
      return 0;
    }
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    if (!metadata.hasNationalPrefix()) {
      return 0;
    }

    PhoneNumberType type = getNumberTypeHelper(getNationalSignificantNumber(number),
                                               metadata);
    // Most numbers other than the two types below have to be dialled in full.
    if (type != PhoneNumberType.FIXED_LINE && type != PhoneNumberType.FIXED_LINE_OR_MOBILE) {
      return 0;
    }

    return getLengthOfNationalDestinationCode(number);
  }

  /**
   * Gets the length of the national destination code (NDC) from the PhoneNumber object passed in,
   * so that clients could use it to split a national significant number into NDC and subscriber
   * number. The NDC of a phone number is normally the first group of digit(s) right after the
   * country calling code when the number is formatted in the international format, if there is a
   * subscriber number part that follows. An example of how this could be used:
   *
   * <pre>
   * PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
   * PhoneNumber number = phoneUtil.parse("18002530000", "US");
   * String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(number);
   * String nationalDestinationCode;
   * String subscriberNumber;
   *
   * int nationalDestinationCodeLength = phoneUtil.getLengthOfNationalDestinationCode(number);
   * if (nationalDestinationCodeLength > 0) {
   *   nationalDestinationCode = nationalSignificantNumber.substring(0,
   *       nationalDestinationCodeLength);
   *   subscriberNumber = nationalSignificantNumber.substring(nationalDestinationCodeLength);
   * } else {
   *   nationalDestinationCode = "";
   *   subscriberNumber = nationalSignificantNumber;
   * }
   * </pre>
   *
   * Refer to the unittests to see the difference between this function and
   * {@link #getLengthOfGeographicalAreaCode}.
   *
   * @param number  the PhoneNumber object for which clients want to know the length of the NDC.
   * @return  the length of NDC of the PhoneNumber object passed in.
   */
  public int getLengthOfNationalDestinationCode(PhoneNumber number) {
    PhoneNumber copiedProto;
    if (number.hasExtension()) {
      // We don't want to alter the proto given to us, but we don't want to include the extension
      // when we format it, so we copy it and clear the extension here.
      copiedProto = new PhoneNumber();
      copiedProto.mergeFrom(number);
      copiedProto.clearExtension();
    } else {
      copiedProto = number;
    }

    String nationalSignificantNumber = format(copiedProto,
                                              PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    String[] numberGroups = NON_DIGITS_PATTERN.split(nationalSignificantNumber);
    // The pattern will start with "+COUNTRY_CODE " so the first group will always be the empty
    // string (before the + symbol) and the second group will be the country calling code. The third
    // group will be area code if it is not the last group.
    if (numberGroups.length <= 3) {
      return 0;
    }

    if (getRegionCodeForNumber(number).equals("AR") &&
        getNumberType(number) == PhoneNumberType.MOBILE) {
      // Argentinian mobile numbers, when formatted in the international format, are in the form of
      // +54 9 NDC XXXX.... As a result, we take the length of the third group (NDC) and add 1 for
      // the digit 9, which also forms part of the national significant number.
      //
      // TODO: Investigate the possibility of better modeling the metadata to make it
      // easier to obtain the NDC.
      return numberGroups[3].length() + 1;
    }
    return numberGroups[2].length();
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
   * @return  the normalized string version of the phone number
   */
  private static String normalizeHelper(String number,
                                        Map<Character, Character> normalizationReplacements,
                                        boolean removeNonMatches) {
    StringBuilder normalizedNumber = new StringBuilder(number.length());
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

  static synchronized PhoneNumberUtil getInstance(
      String baseFileLocation,
      Map<Integer, List<String>> countryCallingCodeToRegionCodeMap) {
    if (instance == null) {
      instance = new PhoneNumberUtil();
      instance.countryCallingCodeToRegionCodeMap = countryCallingCodeToRegionCodeMap;
      instance.init(baseFileLocation);
    }
    return instance;
  }

  /**
   * Used for testing purposes only to reset the PhoneNumberUtil singleton to null.
   */
  static synchronized void resetInstance() {
    instance = null;
  }

  /**
   * Convenience method to enable tests to get a list of what regions the library has metadata for.
   */
  public Set<String> getSupportedRegions() {
    return supportedRegions;
  }

  /**
   * Gets a {@link PhoneNumberUtil} instance to carry out international phone number formatting,
   * parsing, or validation. The instance is loaded with phone number metadata for a number of most
   * commonly used regions.
   *
   * <p>The {@link PhoneNumberUtil} is implemented as a singleton. Therefore, calling getInstance
   * multiple times will only result in one instance being created.
   *
   * @return a PhoneNumberUtil instance
   */
  public static synchronized PhoneNumberUtil getInstance() {
    if (instance == null) {
      return getInstance(META_DATA_FILE_PREFIX,
          CountryCodeToRegionCodeMap.getCountryCodeToRegionCodeMap());
    }
    return instance;
  }

  /**
   * Helper function to check region code is not unknown or null.
   */
  private boolean isValidRegionCode(String regionCode) {
    return regionCode != null && supportedRegions.contains(regionCode);
  }

  /**
   * Helper function to check region code is not unknown or null and log an error message. The
   * {@code countryCallingCode} and {@code number} supplied is used only for the resultant log
   * message.
   */
  private boolean hasValidRegionCode(String regionCode,
                                     int countryCallingCode, String number) {
    if (!isValidRegionCode(regionCode)) {
      LOGGER.log(Level.WARNING,
                 "Number " + number + " has invalid or missing country calling code ("
                 + countryCallingCode + ")");
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
   * country calling code of 0 or an otherwise invalid country calling code, we cannot work out
   * which formatting rules to apply so we return the national significant number with no formatting
   * applied.
   *
   * @param number         the phone number to be formatted
   * @param numberFormat   the format the phone number should be formatted into
   * @return  the formatted phone number
   */
  public String format(PhoneNumber number, PhoneNumberFormat numberFormat) {
    if (number.getNationalNumber() == 0 && number.hasRawInput()) {
      String rawInput = number.getRawInput();
      if (rawInput.length() > 0) {
        return rawInput;
      }
    }
    StringBuilder formattedNumber = new StringBuilder(20);
    format(number, numberFormat, formattedNumber);
    return formattedNumber.toString();
  }

  /**
   * Same as {@link #format(Phonenumber.PhoneNumber, PhoneNumberUtil.PhoneNumberFormat)}, but
   * accepts a mutable StringBuilder as a parameter to decrease object creation when invoked many
   * times.
   */
  public void format(PhoneNumber number, PhoneNumberFormat numberFormat,
                     StringBuilder formattedNumber) {
    // Clear the StringBuilder first.
    formattedNumber.setLength(0);
    int countryCallingCode = number.getCountryCode();
    String nationalSignificantNumber = getNationalSignificantNumber(number);
    if (numberFormat == PhoneNumberFormat.E164) {
      // Early exit for E164 case since no formatting of the national number needs to be applied.
      // Extensions are not formatted.
      formattedNumber.append(nationalSignificantNumber);
      formatNumberByFormat(countryCallingCode, PhoneNumberFormat.E164, formattedNumber);
      return;
    }
    // Note getRegionCodeForCountryCode() is used because formatting information for regions which
    // share a country calling code is contained by only one region for performance reasons. For
    // example, for NANPA regions it will be contained in the metadata for US.
    String regionCode = getRegionCodeForCountryCode(countryCallingCode);
    if (!isValidRegionCode(regionCode)) {
      formattedNumber.append(nationalSignificantNumber);
      return;
    }

    formattedNumber.append(formatNationalNumber(nationalSignificantNumber,
                                                regionCode, numberFormat));
    maybeGetFormattedExtension(number, regionCode, numberFormat, formattedNumber);
    formatNumberByFormat(countryCallingCode, numberFormat, formattedNumber);
  }

  /**
   * Formats a phone number in the specified format using client-defined formatting rules. Note that
   * if the phone number has a country calling code of zero or an otherwise invalid country calling
   * code, we cannot work out things like whether there should be a national prefix applied, or how
   * to format extensions, so we return the national significant number with no formatting applied.
   *
   * @param number                        the phone number to be formatted
   * @param numberFormat                  the format the phone number should be formatted into
   * @param userDefinedFormats            formatting rules specified by clients
   * @return  the formatted phone number
   */
  public String formatByPattern(PhoneNumber number,
                                PhoneNumberFormat numberFormat,
                                List<NumberFormat> userDefinedFormats) {
    int countryCallingCode = number.getCountryCode();
    String nationalSignificantNumber = getNationalSignificantNumber(number);
    // Note getRegionCodeForCountryCode() is used because formatting information for regions which
    // share a country calling code is contained by only one region for performance reasons. For
    // example, for NANPA regions it will be contained in the metadata for US.
    String regionCode = getRegionCodeForCountryCode(countryCallingCode);
    if (!hasValidRegionCode(regionCode, countryCallingCode, nationalSignificantNumber)) {
      return nationalSignificantNumber;
    }
    List<NumberFormat> userDefinedFormatsCopy =
        new ArrayList<NumberFormat>(userDefinedFormats.size());
    for (NumberFormat numFormat : userDefinedFormats) {
      String nationalPrefixFormattingRule = numFormat.getNationalPrefixFormattingRule();
      if (nationalPrefixFormattingRule.length() > 0) {
        // Before we do a replacement of the national prefix pattern $NP with the national prefix,
        // we need to copy the rule so that subsequent replacements for different numbers have the
        // appropriate national prefix.
        NumberFormat numFormatCopy = new NumberFormat();
        numFormatCopy.mergeFrom(numFormat);
        String nationalPrefix = getMetadataForRegion(regionCode).getNationalPrefix();
        if (nationalPrefix.length() > 0) {
          // Replace $NP with national prefix and $FG with the first group ($1).
          nationalPrefixFormattingRule =
              NP_PATTERN.matcher(nationalPrefixFormattingRule).replaceFirst(nationalPrefix);
          nationalPrefixFormattingRule =
              FG_PATTERN.matcher(nationalPrefixFormattingRule).replaceFirst("\\$1");
          numFormatCopy.setNationalPrefixFormattingRule(nationalPrefixFormattingRule);
        } else {
          // We don't want to have a rule for how to format the national prefix if there isn't one.
          numFormatCopy.clearNationalPrefixFormattingRule();
        }
        userDefinedFormatsCopy.add(numFormatCopy);
      } else {
        // Otherwise, we just add the original rule to the modified list of formats.
        userDefinedFormatsCopy.add(numFormat);
      }
    }

    StringBuilder formattedNumber =
        new StringBuilder(formatAccordingToFormats(nationalSignificantNumber,
                                                   userDefinedFormatsCopy,
                                                   numberFormat));
    maybeGetFormattedExtension(number, regionCode, numberFormat, formattedNumber);
    formatNumberByFormat(countryCallingCode, numberFormat, formattedNumber);
    return formattedNumber.toString();
  }

  /**
   * Formats a phone number in national format for dialing using the carrier as specified in the
   * {@code carrierCode}. The {@code carrierCode} will always be used regardless of whether the
   * phone number already has a preferred domestic carrier code stored. If {@code carrierCode}
   * contains an empty string, returns the number in national format without any carrier code.
   *
   * @param number  the phone number to be formatted
   * @param carrierCode  the carrier selection code to be used
   * @return  the formatted phone number in national format for dialing using the carrier as
   *          specified in the {@code carrierCode}
   */
  public String formatNationalNumberWithCarrierCode(PhoneNumber number, String carrierCode) {
    int countryCallingCode = number.getCountryCode();
    String nationalSignificantNumber = getNationalSignificantNumber(number);
    // Note getRegionCodeForCountryCode() is used because formatting information for regions which
    // share a country calling code is contained by only one region for performance reasons. For
    // example, for NANPA regions it will be contained in the metadata for US.
    String regionCode = getRegionCodeForCountryCode(countryCallingCode);
    if (!hasValidRegionCode(regionCode, countryCallingCode, nationalSignificantNumber)) {
      return nationalSignificantNumber;
    }

    StringBuilder formattedNumber = new StringBuilder(20);
    formattedNumber.append(formatNationalNumber(nationalSignificantNumber,
                                                regionCode,
                                                PhoneNumberFormat.NATIONAL,
                                                carrierCode));
    maybeGetFormattedExtension(number, regionCode, PhoneNumberFormat.NATIONAL, formattedNumber);
    formatNumberByFormat(countryCallingCode, PhoneNumberFormat.NATIONAL, formattedNumber);
    return formattedNumber.toString();
  }

  /**
   * Formats a phone number in national format for dialing using the carrier as specified in the
   * preferredDomesticCarrierCode field of the PhoneNumber object passed in. If that is missing,
   * use the {@code fallbackCarrierCode} passed in instead. If there is no
   * {@code preferredDomesticCarrierCode}, and the {@code fallbackCarrierCode} contains an empty
   * string, return the number in national format without any carrier code.
   *
   * <p>Use {@link #formatNationalNumberWithCarrierCode} instead if the carrier code passed in
   * should take precedence over the number's {@code preferredDomesticCarrierCode} when formatting.
   *
   * @param number  the phone number to be formatted
   * @param fallbackCarrierCode  the carrier selection code to be used, if none is found in the
   *     phone number itself
   * @return  the formatted phone number in national format for dialing using the number's
   *     {@code preferredDomesticCarrierCode}, or the {@code fallbackCarrierCode} passed in if
   *     none is found
   */
  public String formatNationalNumberWithPreferredCarrierCode(PhoneNumber number,
                                                             String fallbackCarrierCode) {
    return formatNationalNumberWithCarrierCode(number, number.hasPreferredDomesticCarrierCode()
                                                       ? number.getPreferredDomesticCarrierCode()
                                                       : fallbackCarrierCode);
  }

  /**
   * Formats a phone number for out-of-country dialing purposes. If no regionCallingFrom is
   * supplied, we format the number in its INTERNATIONAL format. If the country calling code is the
   * same as that of the region where the number is from, then NATIONAL formatting will be applied.
   *
   * <p>If the number itself has a country calling code of zero or an otherwise invalid country
   * calling code, then we return the number with no formatting applied.
   *
   * <p>Note this function takes care of the case for calling inside of NANPA and between Russia and
   * Kazakhstan (who share the same country calling code). In those cases, no international prefix
   * is used. For regions which have multiple international prefixes, the number in its
   * INTERNATIONAL format will be returned instead.
   *
   * @param number               the phone number to be formatted
   * @param regionCallingFrom    the region where the call is being placed
   * @return  the formatted phone number
   */
  public String formatOutOfCountryCallingNumber(PhoneNumber number,
                                                String regionCallingFrom) {
    if (!isValidRegionCode(regionCallingFrom)) {
      return format(number, PhoneNumberFormat.INTERNATIONAL);
    }
    int countryCallingCode = number.getCountryCode();
    String regionCode = getRegionCodeForCountryCode(countryCallingCode);
    String nationalSignificantNumber = getNationalSignificantNumber(number);
    if (!hasValidRegionCode(regionCode, countryCallingCode, nationalSignificantNumber)) {
      return nationalSignificantNumber;
    }
    if (countryCallingCode == NANPA_COUNTRY_CODE) {
      if (isNANPACountry(regionCallingFrom)) {
        // For NANPA regions, return the national format for these regions but prefix it with the
        // country calling code.
        return countryCallingCode + " " + format(number, PhoneNumberFormat.NATIONAL);
      }
    } else if (countryCallingCode == getCountryCodeForRegion(regionCallingFrom)) {
    // For regions that share a country calling code, the country calling code need not be dialled.
    // This also applies when dialling within a region, so this if clause covers both these cases.
    // Technically this is the case for dialling from La Reunion to other overseas departments of
    // France (French Guiana, Martinique, Guadeloupe), but not vice versa - so we don't cover this
    // edge case for now and for those cases return the version including country calling code.
    // Details here: http://www.petitfute.com/voyage/225-info-pratiques-reunion
      return format(number, PhoneNumberFormat.NATIONAL);
    }
    String formattedNationalNumber =
        formatNationalNumber(nationalSignificantNumber,
                             regionCode, PhoneNumberFormat.INTERNATIONAL);
    PhoneMetadata metadata = getMetadataForRegion(regionCallingFrom);
    String internationalPrefix = metadata.getInternationalPrefix();

    // For regions that have multiple international prefixes, the international format of the
    // number is returned, unless there is a preferred international prefix.
    String internationalPrefixForFormatting = "";
    if (UNIQUE_INTERNATIONAL_PREFIX.matcher(internationalPrefix).matches()) {
      internationalPrefixForFormatting = internationalPrefix;
    } else if (metadata.hasPreferredInternationalPrefix()) {
      internationalPrefixForFormatting = metadata.getPreferredInternationalPrefix();
    }

    StringBuilder formattedNumber = new StringBuilder(formattedNationalNumber);
    maybeGetFormattedExtension(number, regionCode, PhoneNumberFormat.INTERNATIONAL,
                               formattedNumber);
    if (internationalPrefixForFormatting.length() > 0) {
      formattedNumber.insert(0, " ").insert(0, countryCallingCode).insert(0, " ")
          .insert(0, internationalPrefixForFormatting);
    } else {
      formatNumberByFormat(countryCallingCode,
                           PhoneNumberFormat.INTERNATIONAL,
                           formattedNumber);
    }
    return formattedNumber.toString();
  }

  /**
   * Formats a phone number using the original phone number format that the number is parsed from.
   * The original format is embedded in the country_code_source field of the PhoneNumber object
   * passed in. If such information is missing, the number will be formatted into the NATIONAL
   * format by default.
   *
   * @param number  the phone number that needs to be formatted in its original number format
   * @param regionCallingFrom  the region whose IDD needs to be prefixed if the original number
   *     has one
   * @return  the formatted phone number in its original number format
   */
  public String formatInOriginalFormat(PhoneNumber number, String regionCallingFrom) {
    if (!number.hasCountryCodeSource()) {
      return format(number, PhoneNumberFormat.NATIONAL);
    }
    switch (number.getCountryCodeSource()) {
      case FROM_NUMBER_WITH_PLUS_SIGN:
        return format(number, PhoneNumberFormat.INTERNATIONAL);
      case FROM_NUMBER_WITH_IDD:
        return formatOutOfCountryCallingNumber(number, regionCallingFrom);
      case FROM_NUMBER_WITHOUT_PLUS_SIGN:
        return format(number, PhoneNumberFormat.INTERNATIONAL).substring(1);
      case FROM_DEFAULT_COUNTRY:
      default:
        return format(number, PhoneNumberFormat.NATIONAL);
    }
  }

  /**
   * Formats a phone number for out-of-country dialing purposes.
   *
   * Note that in this version, if the number was entered originally using alpha characters and
   * this version of the number is stored in raw_input, this representation of the number will be
   * used rather than the digit representation. Grouping information, as specified by characters
   * such as "-" and " ", will be retained.
   *
   * <p><b>Caveats:</b></p>
   * <ul>
   *  <li> This will not produce good results if the country calling code is both present in the raw
   *       input _and_ is the start of the national number. This is not a problem in the regions
   *       which typically use alpha numbers.
   *  <li> This will also not produce good results if the raw input has any grouping information
   *       within the first three digits of the national number, and if the function needs to strip
   *       preceding digits/words in the raw input before these digits. Normally people group the
   *       first three digits together so this is not a huge problem - and will be fixed if it
   *       proves to be so.
   * </ul>
   *
   * @param number  the phone number that needs to be formatted
   * @param regionCallingFrom  the region where the call is being placed
   * @return  the formatted phone number
   */
  public String formatOutOfCountryKeepingAlphaChars(PhoneNumber number,
                                                    String regionCallingFrom) {
    String rawInput = number.getRawInput();
    // If there is no raw input, then we can't keep alpha characters because there aren't any.
    // In this case, we return formatOutOfCountryCallingNumber.
    if (rawInput.length() == 0) {
      return formatOutOfCountryCallingNumber(number, regionCallingFrom);
    }
    int countryCode = number.getCountryCode();
    String regionCode = getRegionCodeForCountryCode(countryCode);
    if (!hasValidRegionCode(regionCode, countryCode, rawInput)) {
      return rawInput;
    }
    // Strip any prefix such as country calling code, IDD, that was present. We do this by comparing
    // the number in raw_input with the parsed number.
    // To do this, first we normalize punctuation. We retain number grouping symbols such as " "
    // only.
    rawInput = normalizeHelper(rawInput, ALL_PLUS_NUMBER_GROUPING_SYMBOLS, true);
    // Now we trim everything before the first three digits in the parsed number. We choose three
    // because all valid alpha numbers have 3 digits at the start - if it does not, then we don't
    // trim anything at all. Similarly, if the national number was less than three digits, we don't
    // trim anything at all.
    String nationalNumber = getNationalSignificantNumber(number);
    if (nationalNumber.length() > 3) {
      int firstNationalNumberDigit = rawInput.indexOf(nationalNumber.substring(0, 3));
      if (firstNationalNumberDigit != -1) {
        rawInput = rawInput.substring(firstNationalNumberDigit);
      }
    }
    PhoneMetadata metadata = getMetadataForRegion(regionCallingFrom);
    if (countryCode == NANPA_COUNTRY_CODE) {
      if (isNANPACountry(regionCallingFrom)) {
        return countryCode + " " + rawInput;
      }
    } else if (countryCode == getCountryCodeForRegion(regionCallingFrom)) {
      // Here we copy the formatting rules so we can modify the pattern we expect to match against.
      List<NumberFormat> availableFormats =
          new ArrayList<NumberFormat>(metadata.numberFormatSize());
      for (NumberFormat format : metadata.numberFormats()) {
        NumberFormat newFormat = new NumberFormat();
        newFormat.mergeFrom(format);
        // The first group is the first group of digits that the user determined.
        newFormat.setPattern("(\\d+)(.*)");
        // Here we just concatenate them back together after the national prefix has been fixed.
        newFormat.setFormat("$1$2");
        availableFormats.add(newFormat);
      }
      // Now we format using these patterns instead of the default pattern, but with the national
      // prefix prefixed if necessary, by choosing the format rule based on the leading digits
      // present in the unformatted national number.
      // This will not work in the cases where the pattern (and not the leading digits) decide
      // whether a national prefix needs to be used, since we have overridden the pattern to match
      // anything, but that is not the case in the metadata to date.
      return formatAccordingToFormats(rawInput, availableFormats, PhoneNumberFormat.NATIONAL);
    }
    String internationalPrefix = metadata.getInternationalPrefix();
    // For countries that have multiple international prefixes, the international format of the
    // number is returned, unless there is a preferred international prefix.
    String internationalPrefixForFormatting =
        UNIQUE_INTERNATIONAL_PREFIX.matcher(internationalPrefix).matches()
        ? internationalPrefix
        : metadata.getPreferredInternationalPrefix();
    StringBuilder formattedNumber = new StringBuilder(rawInput);
    maybeGetFormattedExtension(number, regionCode, PhoneNumberFormat.INTERNATIONAL,
                               formattedNumber);
    if (internationalPrefixForFormatting.length() > 0) {
      formattedNumber.insert(0, " ").insert(0, countryCode).insert(0, " ")
          .insert(0, internationalPrefixForFormatting);
    } else {
      formatNumberByFormat(countryCode,
                           PhoneNumberFormat.INTERNATIONAL,
                           formattedNumber);
    }
    return formattedNumber.toString();
  }

  /**
   * Gets the national significant number of the a phone number. Note a national significant number
   * doesn't contain a national prefix or any formatting.
   *
   * @param number  the phone number for which the national significant number is needed
   * @return  the national significant number of the PhoneNumber object passed in
   */
  public String getNationalSignificantNumber(PhoneNumber number) {
    // The leading zero in the national (significant) number of an Italian phone number has a
    // special meaning. Unlike the rest of the world, it indicates the number is a landline
    // number. There have been plans to migrate landline numbers to start with the digit two since
    // December 2000, but it has not yet happened.
    // See http://en.wikipedia.org/wiki/%2B39 for more details.
    // Other regions such as Cote d'Ivoire and Gabon use this for their mobile numbers.
    StringBuilder nationalNumber = new StringBuilder(
        (number.hasItalianLeadingZero() &&
         number.isItalianLeadingZero() &&
         isLeadingZeroPossible(number.getCountryCode()))
        ? "0" : ""
    );
    nationalNumber.append(number.getNationalNumber());
    return nationalNumber.toString();
  }

  /**
   * A helper function that is used by format and formatByPattern.
   */
  private void formatNumberByFormat(int countryCallingCode,
                                    PhoneNumberFormat numberFormat,
                                    StringBuilder formattedNumber) {
    switch (numberFormat) {
      case E164:
        formattedNumber.insert(0, countryCallingCode).insert(0, PLUS_SIGN);
        return;
      case INTERNATIONAL:
        formattedNumber.insert(0, " ").insert(0, countryCallingCode).insert(0, PLUS_SIGN);
        return;
      case RFC3966:
        formattedNumber.insert(0, "-").insert(0, countryCallingCode) .insert(0, PLUS_SIGN);
        return;
      case NATIONAL:
      default:
        return;
    }
  }

  // Simple wrapper of formatNationalNumber for the common case of no carrier code.
  private String formatNationalNumber(String number,
                                      String regionCode,
                                      PhoneNumberFormat numberFormat) {
    return formatNationalNumber(number, regionCode, numberFormat, null);
  }

  // Note in some regions, the national number can be written in two completely different ways
  // depending on whether it forms part of the NATIONAL format or INTERNATIONAL format. The
  // numberFormat parameter here is used to specify which format to use for those cases. If a
  // carrierCode is specified, this will be inserted into the formatted string to replace $CC.
  private String formatNationalNumber(String number,
                                      String regionCode,
                                      PhoneNumberFormat numberFormat,
                                      String carrierCode) {
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    List<NumberFormat> intlNumberFormats = metadata.intlNumberFormats();
    // When the intlNumberFormats exists, we use that to format national number for the
    // INTERNATIONAL format instead of using the numberDesc.numberFormats.
    List<NumberFormat> availableFormats =
        (intlNumberFormats.size() == 0 || numberFormat == PhoneNumberFormat.NATIONAL)
        ? metadata.numberFormats()
        : metadata.intlNumberFormats();
    String formattedNationalNumber =
        formatAccordingToFormats(number, availableFormats, numberFormat, carrierCode);
    if (numberFormat == PhoneNumberFormat.RFC3966) {
      formattedNationalNumber =
          SEPARATOR_PATTERN.matcher(formattedNationalNumber).replaceAll("-");
    }
    return formattedNationalNumber;
  }

  // Simple wrapper of formatAccordingToFormats for the common case of no carrier code.
  private String formatAccordingToFormats(String nationalNumber,
                                          List<NumberFormat> availableFormats,
                                          PhoneNumberFormat numberFormat) {
    return formatAccordingToFormats(nationalNumber, availableFormats, numberFormat, null);
  }

  // Note that carrierCode is optional - if NULL or an empty string, no carrier code replacement
  // will take place.
  private String formatAccordingToFormats(String nationalNumber,
                                          List<NumberFormat> availableFormats,
                                          PhoneNumberFormat numberFormat,
                                          String carrierCode) {
    for (NumberFormat numFormat : availableFormats) {
      int size = numFormat.leadingDigitsPatternSize();
      if (size == 0 || regexCache.getPatternForRegex(
              // We always use the last leading_digits_pattern, as it is the most detailed.
              numFormat.getLeadingDigitsPattern(size - 1)).matcher(nationalNumber).lookingAt()) {
        Matcher m = regexCache.getPatternForRegex(numFormat.getPattern()).matcher(nationalNumber);
        if (m.matches()) {
          String numberFormatRule = numFormat.getFormat();
          if (numberFormat == PhoneNumberFormat.NATIONAL &&
              carrierCode != null && carrierCode.length() > 0 &&
              numFormat.getDomesticCarrierCodeFormattingRule().length() > 0) {
            // Replace the $CC in the formatting rule with the desired carrier code.
            String carrierCodeFormattingRule = numFormat.getDomesticCarrierCodeFormattingRule();
            carrierCodeFormattingRule =
                CC_PATTERN.matcher(carrierCodeFormattingRule).replaceFirst(carrierCode);
            // Now replace the $FG in the formatting rule with the first group and the carrier code
            // combined in the appropriate way.
            numberFormatRule = FIRST_GROUP_PATTERN.matcher(numberFormatRule)
                .replaceFirst(carrierCodeFormattingRule);
            return m.replaceAll(numberFormatRule);
          } else {
            // Use the national prefix formatting rule instead.
            String nationalPrefixFormattingRule = numFormat.getNationalPrefixFormattingRule();
            if (numberFormat == PhoneNumberFormat.NATIONAL &&
                nationalPrefixFormattingRule != null &&
                nationalPrefixFormattingRule.length() > 0) {
              Matcher firstGroupMatcher = FIRST_GROUP_PATTERN.matcher(numberFormatRule);
              return m.replaceAll(firstGroupMatcher.replaceFirst(nationalPrefixFormattingRule));
            } else {
              return m.replaceAll(numberFormatRule);
            }
          }
        }
      }
    }

    // If no pattern above is matched, we format the number as a whole.
    return nationalNumber;
  }

  /**
   * Gets a valid number for the specified region.
   *
   * @param regionCode  the region for which an example number is needed
   * @return  a valid fixed-line number for the specified region. Returns null when the metadata
   *    does not contain such information.
   */
  public PhoneNumber getExampleNumber(String regionCode) {
    return getExampleNumberForType(regionCode, PhoneNumberType.FIXED_LINE);
  }

  /**
   * Gets a valid number for the specified region and number type.
   *
   * @param regionCode  the region for which an example number is needed
   * @param type  the type of number that is needed
   * @return  a valid number for the specified region and type. Returns null when the metadata
   *     does not contain such information or if an invalid region was entered.
   */
  public PhoneNumber getExampleNumberForType(String regionCode, PhoneNumberType type) {
    // Check the region code is valid.
    if (!isValidRegionCode(regionCode)) {
      LOGGER.log(Level.SEVERE, "Invalid or unknown region code provided: " + regionCode);
      return null;
    }
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
   * Appends the formatted extension of a phone number to formattedNumber, if the phone number had
   * an extension specified.
   */
  private void maybeGetFormattedExtension(PhoneNumber number, String regionCode,
                                          PhoneNumberFormat numberFormat,
                                          StringBuilder formattedNumber) {
    if (number.hasExtension() && number.getExtension().length() > 0) {
      if (numberFormat == PhoneNumberFormat.RFC3966) {
        formattedNumber.append(RFC3966_EXTN_PREFIX).append(number.getExtension());
      } else {
        formatExtension(number.getExtension(), regionCode, formattedNumber);
      }
    }
  }

  /**
   * Formats the extension part of the phone number by prefixing it with the appropriate extension
   * prefix. This will be the default extension prefix, unless overridden by a preferred
   * extension prefix for this region.
   */
  private void formatExtension(String extensionDigits, String regionCode,
                               StringBuilder extension) {
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    if (metadata.hasPreferredExtnPrefix()) {
      extension.append(metadata.getPreferredExtnPrefix()).append(extensionDigits);
    } else {
      extension.append(DEFAULT_EXTN_PREFIX).append(extensionDigits);
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
      case PAGER:
        return metadata.getPager();
      case UAN:
        return metadata.getUan();
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
    if (!isValidRegionCode(regionCode)) {
      return PhoneNumberType.UNKNOWN;
    }
    String nationalSignificantNumber = getNationalSignificantNumber(number);
    return getNumberTypeHelper(nationalSignificantNumber, getMetadataForRegion(regionCode));
  }

  private PhoneNumberType getNumberTypeHelper(String nationalNumber, PhoneMetadata metadata) {
    PhoneNumberDesc generalNumberDesc = metadata.getGeneralDesc();
    if (!generalNumberDesc.hasNationalNumberPattern() ||
        !isNumberMatchingDesc(nationalNumber, generalNumberDesc)) {
      return PhoneNumberType.UNKNOWN;
    }

    if (isNumberMatchingDesc(nationalNumber, metadata.getPremiumRate())) {
      return PhoneNumberType.PREMIUM_RATE;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getTollFree())) {
      return PhoneNumberType.TOLL_FREE;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getSharedCost())) {
      return PhoneNumberType.SHARED_COST;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getVoip())) {
      return PhoneNumberType.VOIP;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getPersonalNumber())) {
      return PhoneNumberType.PERSONAL_NUMBER;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getPager())) {
      return PhoneNumberType.PAGER;
    }
    if (isNumberMatchingDesc(nationalNumber, metadata.getUan())) {
      return PhoneNumberType.UAN;
    }

    boolean isFixedLine = isNumberMatchingDesc(nationalNumber, metadata.getFixedLine());
    if (isFixedLine) {
      if (metadata.isSameMobileAndFixedLinePattern()) {
        return PhoneNumberType.FIXED_LINE_OR_MOBILE;
      } else if (isNumberMatchingDesc(nationalNumber, metadata.getMobile())) {
        return PhoneNumberType.FIXED_LINE_OR_MOBILE;
      }
      return PhoneNumberType.FIXED_LINE;
    }
    // Otherwise, test to see if the number is mobile. Only do this if certain that the patterns for
    // mobile and fixed line aren't the same.
    if (!metadata.isSameMobileAndFixedLinePattern() &&
        isNumberMatchingDesc(nationalNumber, metadata.getMobile())) {
      return PhoneNumberType.MOBILE;
    }
    return PhoneNumberType.UNKNOWN;
  }

  PhoneMetadata getMetadataForRegion(String regionCode) {
    if (!isValidRegionCode(regionCode)) {
      return null;
    }
    if (!regionToMetadataMap.containsKey(regionCode)) {
      loadMetadataForRegionFromFile(currentFilePrefix, regionCode);
    }
    return regionToMetadataMap.get(regionCode);
  }

  private boolean isNumberMatchingDesc(String nationalNumber, PhoneNumberDesc numberDesc) {
    Matcher possibleNumberPatternMatcher =
        regexCache.getPatternForRegex(numberDesc.getPossibleNumberPattern())
            .matcher(nationalNumber);
    Matcher nationalNumberPatternMatcher =
        regexCache.getPatternForRegex(numberDesc.getNationalNumberPattern())
            .matcher(nationalNumber);
    return possibleNumberPatternMatcher.matches() && nationalNumberPatternMatcher.matches();
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
    return (isValidRegionCode(regionCode) && isValidNumberForRegion(number, regionCode));
  }

  /**
   * Tests whether a phone number is valid for a certain region. Note this doesn't verify the number
   * is actually in use, which is impossible to tell by just looking at a number itself. If the
   * country calling code is not the same as the country calling code for the region, this
   * immediately exits with false. After this, the specific number pattern rules for the region are
   * examined. This is useful for determining for example whether a particular number is valid for
   * Canada, rather than just a valid NANPA number.
   *
   * @param number       the phone number that we want to validate
   * @param regionCode   the region that we want to validate the phone number for
   * @return  a boolean that indicates whether the number is of a valid pattern
   */
  public boolean isValidNumberForRegion(PhoneNumber number, String regionCode) {
    if (number.getCountryCode() != getCountryCodeForRegion(regionCode)) {
      return false;
    }
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    PhoneNumberDesc generalNumDesc = metadata.getGeneralDesc();
    String nationalSignificantNumber = getNationalSignificantNumber(number);

    // For regions where we don't have metadata for PhoneNumberDesc, we treat any number passed in
    // as a valid number if its national significant number is between the minimum and maximum
    // lengths defined by ITU for a national significant number.
    if (!generalNumDesc.hasNationalNumberPattern()) {
      int numberLength = nationalSignificantNumber.length();
      return numberLength > MIN_LENGTH_FOR_NSN && numberLength <= MAX_LENGTH_FOR_NSN;
    }
    return getNumberTypeHelper(nationalSignificantNumber, metadata) != PhoneNumberType.UNKNOWN;
  }

  /**
   * Returns the region where a phone number is from. This could be used for geocoding at the region
   * level.
   *
   * @param number  the phone number whose origin we want to know
   * @return  the region where the phone number is from, or null if no region matches this calling
   *     code
   */
  public String getRegionCodeForNumber(PhoneNumber number) {
    int countryCode = number.getCountryCode();
    List<String> regions = countryCallingCodeToRegionCodeMap.get(countryCode);
    if (regions == null) {
      return null;
    }
    if (regions.size() == 1) {
      return regions.get(0);
    } else {
      return getRegionCodeForNumberFromRegionList(number, regions);
    }
  }

  private String getRegionCodeForNumberFromRegionList(PhoneNumber number,
                                                      List<String> regionCodes) {
    String nationalNumber = getNationalSignificantNumber(number);
    for (String regionCode : regionCodes) {
      // If leadingDigits is present, use this. Otherwise, do full validation.
      PhoneMetadata metadata = getMetadataForRegion(regionCode);
      if (metadata.hasLeadingDigits()) {
        if (regexCache.getPatternForRegex(metadata.getLeadingDigits())
                .matcher(nationalNumber).lookingAt()) {
          return regionCode;
        }
      } else if (getNumberTypeHelper(nationalNumber, metadata) != PhoneNumberType.UNKNOWN) {
        return regionCode;
      }
    }
    return null;
  }

  /**
   * Returns the region code that matches the specific country calling code. In the case of no
   * region code being found, ZZ will be returned. In the case of multiple regions, the one
   * designated in the metadata as the "main" region for this calling code will be returned.
   */
  public String getRegionCodeForCountryCode(int countryCallingCode) {
    List<String> regionCodes = countryCallingCodeToRegionCodeMap.get(countryCallingCode);
    return regionCodes == null ? UNKNOWN_REGION : regionCodes.get(0);
  }

  /**
   * Returns the country calling code for a specific region. For example, this would be 1 for the
   * United States, and 64 for New Zealand.
   *
   * @param regionCode  the region that we want to get the country calling code for
   * @return  the country calling code for the region denoted by regionCode
   */
  public int getCountryCodeForRegion(String regionCode) {
    if (!isValidRegionCode(regionCode)) {
      return 0;
    }
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    return metadata.getCountryCode();
  }

  /**
   * Returns the national dialling prefix for a specific region. For example, this would be 1 for
   * the United States, and 0 for New Zealand. Set stripNonDigits to true to strip symbols like "~"
   * (which indicates a wait for a dialling tone) from the prefix returned. If no national prefix is
   * present, we return null.
   *
   * <p>Warning: Do not use this method for do-your-own formatting - for some regions, the
   * national dialling prefix is used only for certain types of numbers. Use the library's
   * formatting functions to prefix the national prefix when required.
   *
   * @param regionCode  the region that we want to get the dialling prefix for
   * @param stripNonDigits  true to strip non-digits from the national dialling prefix
   * @return  the dialling prefix for the region denoted by regionCode
   */
  public String getNddPrefixForRegion(String regionCode, boolean stripNonDigits) {
    if (!isValidRegionCode(regionCode)) {
      LOGGER.log(Level.SEVERE, "Invalid or missing region code provided.");
      return null;
    }
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    String nationalPrefix = metadata.getNationalPrefix();
    // If no national prefix was found, we return null.
    if (nationalPrefix.length() == 0) {
      return null;
    }
    if (stripNonDigits) {
      // Note: if any other non-numeric symbols are ever used in national prefixes, these would have
      // to be removed here as well.
      nationalPrefix = nationalPrefix.replace("~", "");
    }
    return nationalPrefix;
  }

  /**
   * Checks if this is a region under the North American Numbering Plan Administration (NANPA).
   *
   * @return  true if regionCode is one of the regions under NANPA
   */
  public boolean isNANPACountry(String regionCode) {
    return nanpaRegions.contains(regionCode);
  }

  /**
   * Checks whether the country calling code is from a region whose national significant number
   * could contain a leading zero. An example of such a region is Italy. Returns false if no
   * metadata for the country is found.
   */
  boolean isLeadingZeroPossible(int countryCallingCode) {
    PhoneMetadata mainMetadataForCallingCode = getMetadataForRegion(
        getRegionCodeForCountryCode(countryCallingCode));
    if (mainMetadataForCallingCode == null) {
      return false;
    }
    return mainMetadataForCallingCode.isLeadingZeroPossible();
  }

  /**
   * Checks if the number is a valid vanity (alpha) number such as 800 MICROSOFT. A valid vanity
   * number will start with at least 3 digits and will have three or more alpha characters. This
   * does not do region-specific checks - to work out if this number is actually valid for a region,
   * it should be parsed and methods such as {@link #isPossibleNumberWithReason} and
   * {@link #isValidNumber} should be used.
   *
   * @param number  the number that needs to be checked
   * @return  true if the number is a valid vanity number
   */
  public boolean isAlphaNumber(String number) {
    if (!isViablePhoneNumber(number)) {
      // Number is too short, or doesn't match the basic phone number pattern.
      return false;
    }
    StringBuilder strippedNumber = new StringBuilder(number);
    maybeStripExtension(strippedNumber);
    return VALID_ALPHA_PHONE_PATTERN.matcher(strippedNumber).matches();
  }

  /**
   * Convenience wrapper around {@link #isPossibleNumberWithReason}. Instead of returning the reason
   * for failure, this method returns a boolean value.
   * @param number  the number that needs to be checked
   * @return  true if the number is possible
   */
  public boolean isPossibleNumber(PhoneNumber number) {
    return isPossibleNumberWithReason(number) == ValidationResult.IS_POSSIBLE;
  }

  /**
   * Helper method to check a number against a particular pattern and determine whether it matches,
   * or is too short or too long. Currently, if a number pattern suggests that numbers of length 7
   * and 10 are possible, and a number in between these possible lengths is entered, such as of
   * length 8, this will return TOO_LONG.
   */
  private ValidationResult testNumberLengthAgainstPattern(Pattern numberPattern, String number) {
    Matcher numberMatcher = numberPattern.matcher(number);
    if (numberMatcher.matches()) {
      return ValidationResult.IS_POSSIBLE;
    }
    if (numberMatcher.lookingAt()) {
      return ValidationResult.TOO_LONG;
    } else {
      return ValidationResult.TOO_SHORT;
    }
  }

  /**
   * Check whether a phone number is a possible number. It provides a more lenient check than
   * {@link #isValidNumber} in the following sense:
   *<ol>
   * <li> It only checks the length of phone numbers. In particular, it doesn't check starting
   *      digits of the number.
   * <li> It doesn't attempt to figure out the type of the number, but uses general rules which
   *      applies to all types of phone numbers in a region. Therefore, it is much faster than
   *      isValidNumber.
   * <li> For fixed line numbers, many regions have the concept of area code, which together with
   *      subscriber number constitute the national significant number. It is sometimes okay to dial
   *      the subscriber number only when dialing in the same area. This function will return
   *      true if the subscriber-number-only version is passed in. On the other hand, because
   *      isValidNumber validates using information on both starting digits (for fixed line
   *      numbers, that would most likely be area codes) and length (obviously includes the
   *      length of area codes for fixed line numbers), it will return false for the
   *      subscriber-number-only version.
   * </ol
   * @param number  the number that needs to be checked
   * @return  a ValidationResult object which indicates whether the number is possible
   */
  public ValidationResult isPossibleNumberWithReason(PhoneNumber number) {
    String nationalNumber = getNationalSignificantNumber(number);
    int countryCode = number.getCountryCode();
    // Note: For Russian Fed and NANPA numbers, we just use the rules from the default region (US or
    // Russia) since the getRegionCodeForNumber will not work if the number is possible but not
    // valid. This would need to be revisited if the possible number pattern ever differed between
    // various regions within those plans.
    String regionCode = getRegionCodeForCountryCode(countryCode);
    if (!isValidRegionCode(regionCode)) {
      return ValidationResult.INVALID_COUNTRY_CODE;
    }
    PhoneNumberDesc generalNumDesc = getMetadataForRegion(regionCode).getGeneralDesc();
    // Handling case of numbers with no metadata.
    if (!generalNumDesc.hasNationalNumberPattern()) {
      LOGGER.log(Level.FINER, "Checking if number is possible with incomplete metadata.");
      int numberLength = nationalNumber.length();
      if (numberLength < MIN_LENGTH_FOR_NSN) {
        return ValidationResult.TOO_SHORT;
      } else if (numberLength > MAX_LENGTH_FOR_NSN) {
        return ValidationResult.TOO_LONG;
      } else {
        return ValidationResult.IS_POSSIBLE;
      }
    }
    Pattern possibleNumberPattern =
        regexCache.getPatternForRegex(generalNumDesc.getPossibleNumberPattern());
    return testNumberLengthAgainstPattern(possibleNumberPattern, nationalNumber);
  }

  /**
   * Check whether a phone number is a possible number given a number in the form of a string, and
   * the region where the number could be dialed from. It provides a more lenient check than
   * {@link #isValidNumber}. See {@link #isPossibleNumber(Phonenumber.PhoneNumber)} for details.
   *
   * <p>This method first parses the number, then invokes
   * {@link #isPossibleNumber(Phonenumber.PhoneNumber)} with the resultant PhoneNumber object.
   *
   * @param number  the number that needs to be checked, in the form of a string
   * @param regionDialingFrom  the region that we are expecting the number to be dialed from.
   *     Note this is different from the region where the number belongs.  For example, the number
   *     +1 650 253 0000 is a number that belongs to US. When written in this form, it can be
   *     dialed from any region. When it is written as 00 1 650 253 0000, it can be dialed from any
   *     region which uses an international dialling prefix of 00. When it is written as
   *     650 253 0000, it can only be dialed from within the US, and when written as 253 0000, it
   *     can only be dialed from within a smaller area in the US (Mountain View, CA, to be more
   *     specific).
   * @return  true if the number is possible
   */
  public boolean isPossibleNumber(String number, String regionDialingFrom) {
    try {
      return isPossibleNumber(parse(number, regionDialingFrom));
    } catch (NumberParseException e) {
      return false;
    }
  }

  /**
   * Attempts to extract a valid number from a phone number that is too long to be valid, and resets
   * the PhoneNumber object passed in to that valid version. If no valid number could be extracted,
   * the PhoneNumber object passed in will not be modified.
   * @param number a PhoneNumber object which contains a number that is too long to be valid.
   * @return  true if a valid phone number can be successfully extracted.
   */
  public boolean truncateTooLongNumber(PhoneNumber number) {
    if (isValidNumber(number)) {
      return true;
    }
    PhoneNumber numberCopy = new PhoneNumber();
    numberCopy.mergeFrom(number);
    long nationalNumber = number.getNationalNumber();
    do {
      nationalNumber /= 10;
      numberCopy.setNationalNumber(nationalNumber);
      if (isPossibleNumberWithReason(numberCopy) == ValidationResult.TOO_SHORT ||
          nationalNumber == 0) {
        return false;
      }
    } while (!isValidNumber(numberCopy));
    number.setNationalNumber(nationalNumber);
    return true;
  }

  /**
   * Gets an {@link com.google.i18n.phonenumbers.AsYouTypeFormatter} for the specific region.
   *
   * @param regionCode  the region where the phone number is being entered
   * @return  an {@link com.google.i18n.phonenumbers.AsYouTypeFormatter} object, which can be used
   *     to format phone numbers in the specific region "as you type"
   */
  public AsYouTypeFormatter getAsYouTypeFormatter(String regionCode) {
    return new AsYouTypeFormatter(regionCode);
  }

  // Extracts country calling code from fullNumber, returns it and places the remaining number in
  // nationalNumber. It assumes that the leading plus sign or IDD has already been removed. Returns
  // 0 if fullNumber doesn't start with a valid country calling code, and leaves nationalNumber
  // unmodified.
  int extractCountryCode(StringBuilder fullNumber, StringBuilder nationalNumber) {
    if ((fullNumber.length() == 0) || (fullNumber.charAt(0) == '0')) {
      // Country codes do not begin with a '0'.
      return 0;
    }
    int potentialCountryCode;
    int numberLength = fullNumber.length();
    for (int i = 1; i <= MAX_LENGTH_COUNTRY_CODE && i <= numberLength; i++) {
      potentialCountryCode = Integer.parseInt(fullNumber.substring(0, i));
      if (countryCallingCodeToRegionCodeMap.containsKey(potentialCountryCode)) {
        nationalNumber.append(fullNumber.substring(i));
        return potentialCountryCode;
      }
    }
    return 0;
  }

  /**
   * Tries to extract a country calling code from a number. This method will return zero if no
   * country calling code is considered to be present. Country calling codes are extracted in the
   * following ways:
   * <ul>
   *  <li> by stripping the international dialing prefix of the region the person is dialing from,
   *       if this is present in the number, and looking at the next digits
   *  <li> by stripping the '+' sign if present and then looking at the next digits
   *  <li> by comparing the start of the number and the country calling code of the default region.
   *       If the number is not considered possible for the numbering plan of the default region
   *       initially, but starts with the country calling code of this region, validation will be
   *       reattempted after stripping this country calling code. If this number is considered a
   *       possible number, then the first digits will be considered the country calling code and
   *       removed as such.
   * </ul>
   * It will throw a NumberParseException if the number starts with a '+' but the country calling
   * code supplied after this does not match that of any known region.
   *
   * @param number  non-normalized telephone number that we wish to extract a country calling
   *     code from - may begin with '+'
   * @param defaultRegionMetadata  metadata about the region this number may be from
   * @param nationalNumber  a string buffer to store the national significant number in, in the case
   *     that a country calling code was extracted. The number is appended to any existing contents.
   *     If no country calling code was extracted, this will be left unchanged.
   * @param keepRawInput  true if the country_code_source and preferred_carrier_code fields of
   *     phoneNumber should be populated.
   * @param phoneNumber  the PhoneNumber object where the country_code and country_code_source need
   *     to be populated. Note the country_code is always populated, whereas country_code_source is
   *     only populated when keepCountryCodeSource is true.
   * @return  the country calling code extracted or 0 if none could be extracted
   */
  int maybeExtractCountryCode(String number, PhoneMetadata defaultRegionMetadata,
                              StringBuilder nationalNumber, boolean keepRawInput,
                              PhoneNumber phoneNumber)
      throws NumberParseException {
    if (number.length() == 0) {
      return 0;
    }
    StringBuilder fullNumber = new StringBuilder(number);
    // Set the default prefix to be something that will never match.
    String possibleCountryIddPrefix = "NonMatch";
    if (defaultRegionMetadata != null) {
      possibleCountryIddPrefix = defaultRegionMetadata.getInternationalPrefix();
    }

    CountryCodeSource countryCodeSource =
        maybeStripInternationalPrefixAndNormalize(fullNumber, possibleCountryIddPrefix);
    if (keepRawInput) {
      phoneNumber.setCountryCodeSource(countryCodeSource);
    }
    if (countryCodeSource != CountryCodeSource.FROM_DEFAULT_COUNTRY) {
      if (fullNumber.length() < MIN_LENGTH_FOR_NSN) {
        throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                                       "Phone number had an IDD, but after this was not "
                                       + "long enough to be a viable phone number.");
      }
      int potentialCountryCode = extractCountryCode(fullNumber, nationalNumber);
      if (potentialCountryCode != 0) {
        phoneNumber.setCountryCode(potentialCountryCode);
        return potentialCountryCode;
      }

      // If this fails, they must be using a strange country calling code that we don't recognize,
      // or that doesn't exist.
      throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                                     "Country calling code supplied was not recognised.");
    } else if (defaultRegionMetadata != null) {
      // Check to see if the number starts with the country calling code for the default region. If
      // so, we remove the country calling code, and do some checks on the validity of the number
      // before and after.
      int defaultCountryCode = defaultRegionMetadata.getCountryCode();
      String defaultCountryCodeString = String.valueOf(defaultCountryCode);
      String normalizedNumber = fullNumber.toString();
      if (normalizedNumber.startsWith(defaultCountryCodeString)) {
        StringBuilder potentialNationalNumber =
            new StringBuilder(normalizedNumber.substring(defaultCountryCodeString.length()));
        PhoneNumberDesc generalDesc = defaultRegionMetadata.getGeneralDesc();
        Pattern validNumberPattern =
            regexCache.getPatternForRegex(generalDesc.getNationalNumberPattern());
        maybeStripNationalPrefixAndCarrierCode(potentialNationalNumber, defaultRegionMetadata);
        Pattern possibleNumberPattern =
            regexCache.getPatternForRegex(generalDesc.getPossibleNumberPattern());
        // If the number was not valid before but is valid now, or if it was too long before, we
        // consider the number with the country calling code stripped to be a better result and
        // keep that instead.
        if ((!validNumberPattern.matcher(fullNumber).matches() &&
             validNumberPattern.matcher(potentialNationalNumber).matches()) ||
             testNumberLengthAgainstPattern(possibleNumberPattern, fullNumber.toString())
                  == ValidationResult.TOO_LONG) {
          nationalNumber.append(potentialNationalNumber);
          if (keepRawInput) {
            phoneNumber.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN);
          }
          phoneNumber.setCountryCode(defaultCountryCode);
          return defaultCountryCode;
        }
      }
    }
    // No country calling code present.
    phoneNumber.setCountryCode(0);
    return 0;
  }

  /**
   * Strips the IDD from the start of the number if present. Helper function used by
   * maybeStripInternationalPrefixAndNormalize.
   */
  private boolean parsePrefixAsIdd(Pattern iddPattern, StringBuilder number) {
    Matcher m = iddPattern.matcher(number);
    if (m.lookingAt()) {
      int matchEnd = m.end();
      // Only strip this if the first digit after the match is not a 0, since country calling codes
      // cannot begin with 0.
      Matcher digitMatcher = CAPTURING_DIGIT_PATTERN.matcher(number.substring(matchEnd));
      if (digitMatcher.find()) {
        String normalizedGroup = normalizeDigitsOnly(digitMatcher.group(1));
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
   * the resulting number, and indicates if an international prefix was present.
   *
   * @param number  the non-normalized telephone number that we wish to strip any international
   *     dialing prefix from.
   * @param possibleIddPrefix  the international direct dialing prefix from the region we
   *     think this number may be dialed in
   * @return  the corresponding CountryCodeSource if an international dialing prefix could be
   *     removed from the number, otherwise CountryCodeSource.FROM_DEFAULT_COUNTRY if the number did
   *     not seem to be in international format.
   */
  CountryCodeSource maybeStripInternationalPrefixAndNormalize(
      StringBuilder number,
      String possibleIddPrefix) {
    if (number.length() == 0) {
      return CountryCodeSource.FROM_DEFAULT_COUNTRY;
    }
    // Check to see if the number begins with one or more plus signs.
    Matcher m = PLUS_CHARS_PATTERN.matcher(number);
    if (m.lookingAt()) {
      number.delete(0, m.end());
      // Can now normalize the rest of the number since we've consumed the "+" sign at the start.
      normalize(number);
      return CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN;
    }
    // Attempt to parse the first digits as an international prefix.
    Pattern iddPattern = regexCache.getPatternForRegex(possibleIddPrefix);
    if (parsePrefixAsIdd(iddPattern, number)) {
      normalize(number);
      return CountryCodeSource.FROM_NUMBER_WITH_IDD;
    }
    // If still not found, then try and normalize the number and then try again. This shouldn't be
    // done before, since non-numeric characters (+ and ~) may legally be in the international
    // prefix.
    normalize(number);
    return parsePrefixAsIdd(iddPattern, number)
           ? CountryCodeSource.FROM_NUMBER_WITH_IDD
           : CountryCodeSource.FROM_DEFAULT_COUNTRY;
  }

  /**
   * Strips any national prefix (such as 0, 1) present in the number provided.
   *
   * @param number  the normalized telephone number that we wish to strip any national
   *     dialing prefix from
   * @param metadata  the metadata for the region that we think this number is from
   * @return the carrier code extracted if it is present, otherwise return an empty string.
   */
  String maybeStripNationalPrefixAndCarrierCode(StringBuilder number, PhoneMetadata metadata) {
    String carrierCode = "";
    int numberLength = number.length();
    String possibleNationalPrefix = metadata.getNationalPrefixForParsing();
    if (numberLength == 0 || possibleNationalPrefix.length() == 0) {
      // Early return for numbers of zero length.
      return "";
    }
    // Attempt to parse the first digits as a national prefix.
    Matcher prefixMatcher = regexCache.getPatternForRegex(possibleNationalPrefix).matcher(number);
    if (prefixMatcher.lookingAt()) {
      Pattern nationalNumberRule =
          regexCache.getPatternForRegex(metadata.getGeneralDesc().getNationalNumberPattern());
      // Check if the original number is viable.
      boolean isViableOriginalNumber = nationalNumberRule.matcher(number).matches();
      // prefixMatcher.group(numOfGroups) == null implies nothing was captured by the capturing
      // groups in possibleNationalPrefix; therefore, no transformation is necessary, and we just
      // remove the national prefix.
      int numOfGroups = prefixMatcher.groupCount();
      String transformRule = metadata.getNationalPrefixTransformRule();
      if (transformRule == null || transformRule.length() == 0 ||
          prefixMatcher.group(numOfGroups) == null) {
        // If the original number was viable, and the resultant number is not, we return.
        if (isViableOriginalNumber &&
            !nationalNumberRule.matcher(number.substring(prefixMatcher.end())).matches()) {
          return "";
        }
        if (numOfGroups > 0 && prefixMatcher.group(numOfGroups) != null) {
          carrierCode = prefixMatcher.group(1);
        }
        number.delete(0, prefixMatcher.end());
      } else {
        // Check that the resultant number is still viable. If not, return. Check this by copying
        // the string buffer and making the transformation on the copy first.
        StringBuilder transformedNumber = new StringBuilder(number);
        transformedNumber.replace(0, numberLength, prefixMatcher.replaceFirst(transformRule));
        if (isViableOriginalNumber &&
            !nationalNumberRule.matcher(transformedNumber.toString()).matches()) {
          return "";
        }
        if (numOfGroups > 1) {
          carrierCode = prefixMatcher.group(1);
        }
        number.replace(0, number.length(), transformedNumber.toString());
      }
    }
    return carrierCode;
  }

  /**
   * Strips any extension (as in, the part of the number dialled after the call is connected,
   * usually indicated with extn, ext, x or similar) from the end of the number, and returns it.
   *
   * @param number  the non-normalized telephone number that we wish to strip the extension from
   * @return        the phone extension
   */
  String maybeStripExtension(StringBuilder number) {
    Matcher m = EXTN_PATTERN.matcher(number);
    // If we find a potential extension, and the number preceding this is a viable number, we assume
    // it is an extension.
    if (m.find() && isViablePhoneNumber(number.substring(0, m.start()))) {
      // The numbers are captured into groups in the regular expression.
      for (int i = 1, length = m.groupCount(); i <= length; i++) {
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
   * Checks to see that the region code used is valid, or if it is not valid, that the number to
   * parse starts with a + symbol so that we can attempt to infer the region from the number.
   * Returns false if it cannot use the region provided and the region cannot be inferred.
   */
  private boolean checkRegionForParsing(String numberToParse, String defaultRegion) {
    if (!isValidRegionCode(defaultRegion)) {
      // If the number is null or empty, we can't infer the region.
      if (numberToParse == null || numberToParse.length() == 0 ||
          !PLUS_CHARS_PATTERN.matcher(numberToParse).lookingAt()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Parses a string and returns it in proto buffer format. This method will throw a
   * {@link com.google.i18n.phonenumbers.NumberParseException} if the number is not considered to be
   * a possible number. Note that validation of whether the number is actually a valid number for a
   * particular region is not performed. This can be done separately with {@link #isValidNumber}.
   *
   * @param numberToParse     number that we are attempting to parse. This can contain formatting
   *                          such as +, ( and -, as well as a phone number extension.
   * @param defaultRegion     region that we are expecting the number to be from. This is only used
   *                          if the number being parsed is not written in international format.
   *                          The country_code for the number in this case would be stored as that
   *                          of the default region supplied. If the number is guaranteed to
   *                          start with a '+' followed by the country calling code, then
   *                          "ZZ" or null can be supplied.
   * @return                  a phone number proto buffer filled with the parsed number
   * @throws NumberParseException  if the string is not considered to be a viable phone number or if
   *                               no default region was supplied and the number is not in
   *                               international format (does not start with +)
   */
  public PhoneNumber parse(String numberToParse, String defaultRegion)
      throws NumberParseException {
    PhoneNumber phoneNumber = new PhoneNumber();
    parse(numberToParse, defaultRegion, phoneNumber);
    return phoneNumber;
  }

  /**
   * Same as {@link #parse(String, String)}, but accepts mutable PhoneNumber as a parameter to
   * decrease object creation when invoked many times.
   */
  public void parse(String numberToParse, String defaultRegion, PhoneNumber phoneNumber)
      throws NumberParseException {
    parseHelper(numberToParse, defaultRegion, false, true, phoneNumber);
  }

  /**
   * Parses a string and returns it in proto buffer format. This method differs from {@link #parse}
   * in that it always populates the raw_input field of the protocol buffer with numberToParse as
   * well as the country_code_source field.
   *
   * @param numberToParse     number that we are attempting to parse. This can contain formatting
   *                          such as +, ( and -, as well as a phone number extension.
   * @param defaultRegion     region that we are expecting the number to be from. This is only used
   *                          if the number being parsed is not written in international format.
   *                          The country calling code for the number in this case would be stored
   *                          as that of the default region supplied.
   * @return                  a phone number proto buffer filled with the parsed number
   * @throws NumberParseException  if the string is not considered to be a viable phone number or if
   *                               no default region was supplied
   */
  public PhoneNumber parseAndKeepRawInput(String numberToParse, String defaultRegion)
      throws NumberParseException {
    PhoneNumber phoneNumber = new PhoneNumber();
    parseAndKeepRawInput(numberToParse, defaultRegion, phoneNumber);
    return phoneNumber;
  }

  /**
   * Same as{@link #parseAndKeepRawInput(String, String)}, but accepts a mutable PhoneNumber as
   * a parameter to decrease object creation when invoked many times.
   */
  public void parseAndKeepRawInput(String numberToParse, String defaultRegion,
                                   PhoneNumber phoneNumber)
      throws NumberParseException {
    parseHelper(numberToParse, defaultRegion, true, true, phoneNumber);
  }

  /**
   * Returns an iterable over all {@link PhoneNumberMatch PhoneNumberMatches} in {@code text}. This
   * is a shortcut for {@link #findNumbers(CharSequence, String, Leniency, long)
   * getMatcher(text, defaultRegion, Leniency.VALID, Long.MAX_VALUE)}.
   *
   * @param text              the text to search for phone numbers, null for no text
   * @param defaultRegion     region that we are expecting the number to be from. This is only used
   *                          if the number being parsed is not written in international format. The
   *                          country_code for the number in this case would be stored as that of
   *                          the default region supplied. May be null if only international
   *                          numbers are expected.
   */
  public Iterable<PhoneNumberMatch> findNumbers(CharSequence text, String defaultRegion) {
    return findNumbers(text, defaultRegion, Leniency.VALID, Long.MAX_VALUE);
  }

  /**
   * Returns an iterable over all {@link PhoneNumberMatch PhoneNumberMatches} in {@code text}.
   *
   * @param text              the text to search for phone numbers, null for no text
   * @param defaultRegion     region that we are expecting the number to be from. This is only used
   *                          if the number being parsed is not written in international format. The
   *                          country_code for the number in this case would be stored as that of
   *                          the default region supplied. May be null if only international
   *                          numbers are expected.
   * @param leniency          the leniency to use when evaluating candidate phone numbers
   * @param maxTries          the maximum number of invalid numbers to try before giving up on the
   *                          text. This is to cover degenerate cases where the text has a lot of
   *                          false positives in it. Must be {@code >= 0}.
   */
  public Iterable<PhoneNumberMatch> findNumbers(
      final CharSequence text, final String defaultRegion, final Leniency leniency,
      final long maxTries) {

    return new Iterable<PhoneNumberMatch>() {
      public Iterator<PhoneNumberMatch> iterator() {
        return new PhoneNumberMatcher(
            PhoneNumberUtil.this, text, defaultRegion, leniency, maxTries);
      }
    };
  }

  /**
   * Parses a string and fills up the phoneNumber. This method is the same as the public
   * parse() method, with the exception that it allows the default region to be null, for use by
   * isNumberMatch(). checkRegion should be set to false if it is permitted for the default region
   * to be null or unknown ("ZZ").
   */
  private void parseHelper(String numberToParse, String defaultRegion, boolean keepRawInput,
                           boolean checkRegion, PhoneNumber phoneNumber)
      throws NumberParseException {
    if (numberToParse == null) {
      throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER,
                                     "The phone number supplied was null.");
    }
    // Extract a possible number from the string passed in (this strips leading characters that
    // could not be the start of a phone number.)
    String number = extractPossibleNumber(numberToParse);
    if (!isViablePhoneNumber(number)) {
      throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER,
                                     "The string supplied did not seem to be a phone number.");
    }

    // Check the region supplied is valid, or that the extracted number starts with some sort of +
    // sign so the number's region can be determined.
    if (checkRegion && !checkRegionForParsing(number, defaultRegion)) {
      throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                                     "Missing or invalid default region.");
    }

    if (keepRawInput) {
      phoneNumber.setRawInput(numberToParse);
    }
    StringBuilder nationalNumber = new StringBuilder(number);
    // Attempt to parse extension first, since it doesn't require region-specific data and we want
    // to have the non-normalised number here.
    String extension = maybeStripExtension(nationalNumber);
    if (extension.length() > 0) {
      phoneNumber.setExtension(extension);
    }

    PhoneMetadata regionMetadata = getMetadataForRegion(defaultRegion);
    // Check to see if the number is given in international format so we know whether this number is
    // from the default region or not.
    StringBuilder normalizedNationalNumber = new StringBuilder();
    int countryCode = 0;
    try {
      // TODO: This method should really just take in the string buffer that has already
      // been created, and just remove the prefix, rather than taking in a string and then
      // outputting a string buffer.
      countryCode = maybeExtractCountryCode(nationalNumber.toString(), regionMetadata,
                                            normalizedNationalNumber, keepRawInput, phoneNumber);
    } catch (NumberParseException e) {
      Matcher matcher = PLUS_CHARS_PATTERN.matcher(nationalNumber.toString());
      if (e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE &&
          matcher.lookingAt()) {
        // Strip the plus-char, and try again.
        countryCode = maybeExtractCountryCode(nationalNumber.substring(matcher.end()),
                                              regionMetadata, normalizedNationalNumber,
                                              keepRawInput, phoneNumber);
        if (countryCode == 0) {
          throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                                         "Could not interpret numbers after plus-sign.");
        }
      } else {
        throw new NumberParseException(e.getErrorType(), e.getMessage());
      }
    }
    if (countryCode != 0) {
      String phoneNumberRegion = getRegionCodeForCountryCode(countryCode);
      if (!phoneNumberRegion.equals(defaultRegion)) {
        regionMetadata = getMetadataForRegion(phoneNumberRegion);
      }
    } else {
      // If no extracted country calling code, use the region supplied instead. The national number
      // is just the normalized version of the number we were given to parse.
      normalize(nationalNumber);
      normalizedNationalNumber.append(nationalNumber);
      if (defaultRegion != null) {
        countryCode = regionMetadata.getCountryCode();
        phoneNumber.setCountryCode(countryCode);
      } else if (keepRawInput) {
        phoneNumber.clearCountryCodeSource();
      }
    }
    if (normalizedNationalNumber.length() < MIN_LENGTH_FOR_NSN) {
      throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_NSN,
                                     "The string supplied is too short to be a phone number.");
    }
    if (regionMetadata != null) {
      String carrierCode =
          maybeStripNationalPrefixAndCarrierCode(normalizedNationalNumber, regionMetadata);
      if (keepRawInput) {
        phoneNumber.setPreferredDomesticCarrierCode(carrierCode);
      }
    }
    int lengthOfNationalNumber = normalizedNationalNumber.length();
    if (lengthOfNationalNumber < MIN_LENGTH_FOR_NSN) {
      throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_NSN,
                                     "The string supplied is too short to be a phone number.");
    }
    if (lengthOfNationalNumber > MAX_LENGTH_FOR_NSN) {
      throw new NumberParseException(NumberParseException.ErrorType.TOO_LONG,
                                     "The string supplied is too long to be a phone number.");
    }
    if (normalizedNationalNumber.charAt(0) == '0') {
      phoneNumber.setItalianLeadingZero(true);
    }
    phoneNumber.setNationalNumber(Long.parseLong(normalizedNationalNumber.toString()));
  }

  /**
   * Takes two phone numbers and compares them for equality.
   *
   * <p>Returns EXACT_MATCH if the country_code, NSN, presence of a leading zero for Italian numbers
   * and any extension present are the same.
   * Returns NSN_MATCH if either or both has no region specified, and the NSNs and extensions are
   * the same.
   * Returns SHORT_NSN_MATCH if either or both has no region specified, or the region specified is
   * the same, and one NSN could be a shorter version of the other number. This includes the case
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
    PhoneNumber firstNumber = new PhoneNumber();
    firstNumber.mergeFrom(firstNumberIn);
    PhoneNumber secondNumber = new PhoneNumber();
    secondNumber.mergeFrom(secondNumberIn);
    // First clear raw_input, country_code_source and preferred_domestic_carrier_code fields and any
    // empty-string extensions so that we can use the proto-buffer equality method.
    firstNumber.clearRawInput();
    firstNumber.clearCountryCodeSource();
    firstNumber.clearPreferredDomesticCarrierCode();
    secondNumber.clearRawInput();
    secondNumber.clearCountryCodeSource();
    secondNumber.clearPreferredDomesticCarrierCode();
    if (firstNumber.hasExtension() &&
        firstNumber.getExtension().length() == 0) {
        firstNumber.clearExtension();
    }
    if (secondNumber.hasExtension() &&
        secondNumber.getExtension().length() == 0) {
        secondNumber.clearExtension();
    }
    // Early exit if both had extensions and these are different.
    if (firstNumber.hasExtension() && secondNumber.hasExtension() &&
        !firstNumber.getExtension().equals(secondNumber.getExtension())) {
      return MatchType.NO_MATCH;
    }
    int firstNumberCountryCode = firstNumber.getCountryCode();
    int secondNumberCountryCode = secondNumber.getCountryCode();
    // Both had country_code specified.
    if (firstNumberCountryCode != 0 && secondNumberCountryCode != 0) {
      if (firstNumber.exactlySameAs(secondNumber)) {
        return MatchType.EXACT_MATCH;
      } else if (firstNumberCountryCode == secondNumberCountryCode &&
                 isNationalNumberSuffixOfTheOther(firstNumber, secondNumber)) {
        // A SHORT_NSN_MATCH occurs if there is a difference because of the presence or absence of
        // an 'Italian leading zero', the presence or absence of an extension, or one NSN being a
        // shorter variant of the other.
        return MatchType.SHORT_NSN_MATCH;
      }
      // This is not a match.
      return MatchType.NO_MATCH;
    }
    // Checks cases where one or both country_code fields were not specified. To make equality
    // checks easier, we first set the country_code fields to be equal.
    firstNumber.setCountryCode(secondNumberCountryCode);
    // If all else was the same, then this is an NSN_MATCH.
    if (firstNumber.exactlySameAs(secondNumber)) {
      return MatchType.NSN_MATCH;
    }
    if (isNationalNumberSuffixOfTheOther(firstNumber, secondNumber)) {
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
   * wrapper for {@link #isNumberMatch(Phonenumber.PhoneNumber, Phonenumber.PhoneNumber)}. No
   * default region is known.
   *
   * @param firstNumber  first number to compare. Can contain formatting, and can have country
   *     calling code specified with + at the start.
   * @param secondNumber  second number to compare. Can contain formatting, and can have country
   *     calling code specified with + at the start.
   * @return  NOT_A_NUMBER, NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH, EXACT_MATCH. See
   *     {@link #isNumberMatch(Phonenumber.PhoneNumber, Phonenumber.PhoneNumber)} for more details.
   */
  public MatchType isNumberMatch(String firstNumber, String secondNumber) {
    try {
      PhoneNumber firstNumberAsProto = parse(firstNumber, UNKNOWN_REGION);
      return isNumberMatch(firstNumberAsProto, secondNumber);
    } catch (NumberParseException e) {
      if (e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
        try {
          PhoneNumber secondNumberAsProto = parse(secondNumber, UNKNOWN_REGION);
          return isNumberMatch(secondNumberAsProto, firstNumber);
        } catch (NumberParseException e2) {
          if (e2.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
            try {
              PhoneNumber firstNumberProto = new PhoneNumber();
              PhoneNumber secondNumberProto = new PhoneNumber();
              parseHelper(firstNumber, null, false, false, firstNumberProto);
              parseHelper(secondNumber, null, false, false, secondNumberProto);
              return isNumberMatch(firstNumberProto, secondNumberProto);
            } catch (NumberParseException e3) {
              // Fall through and return MatchType.NOT_A_NUMBER.
            }
          }
        }
      }
    }
    // One or more of the phone numbers we are trying to match is not a viable phone number.
    return MatchType.NOT_A_NUMBER;
  }

  /**
   * Takes two phone numbers and compares them for equality. This is a convenience wrapper for
   * {@link #isNumberMatch(Phonenumber.PhoneNumber, Phonenumber.PhoneNumber)}. No default region is
   * known.
   *
   * @param firstNumber  first number to compare in proto buffer format.
   * @param secondNumber  second number to compare. Can contain formatting, and can have country
   *     calling code specified with + at the start.
   * @return  NOT_A_NUMBER, NO_MATCH, SHORT_NSN_MATCH, NSN_MATCH, EXACT_MATCH. See
   *     {@link #isNumberMatch(Phonenumber.PhoneNumber, Phonenumber.PhoneNumber)} for more details.
   */
  public MatchType isNumberMatch(PhoneNumber firstNumber, String secondNumber) {
    // First see if the second number has an implicit country calling code, by attempting to parse
    // it.
    try {
      PhoneNumber secondNumberAsProto = parse(secondNumber, UNKNOWN_REGION);
      return isNumberMatch(firstNumber, secondNumberAsProto);
    } catch (NumberParseException e) {
      if (e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
        // The second number has no country calling code. EXACT_MATCH is no longer possible.
        // We parse it as if the region was the same as that for the first number, and if
        // EXACT_MATCH is returned, we replace this with NSN_MATCH.
        String firstNumberRegion = getRegionCodeForCountryCode(firstNumber.getCountryCode());
        try {
          if (!firstNumberRegion.equals(UNKNOWN_REGION)) {
            PhoneNumber secondNumberWithFirstNumberRegion = parse(secondNumber, firstNumberRegion);
            MatchType match = isNumberMatch(firstNumber, secondNumberWithFirstNumberRegion);
            if (match == MatchType.EXACT_MATCH) {
              return MatchType.NSN_MATCH;
            }
            return match;
          } else {
            // If the first number didn't have a valid country calling code, then we parse the
            // second number without one as well.
            PhoneNumber secondNumberProto = new PhoneNumber();
            parseHelper(secondNumber, null, false, false, secondNumberProto);
            return isNumberMatch(firstNumber, secondNumberProto);
          }
        } catch (NumberParseException e2) {
          // Fall-through to return NOT_A_NUMBER.
        }
      }
    }
    // One or more of the phone numbers we are trying to match is not a viable phone number.
    return MatchType.NOT_A_NUMBER;
  }

  /**
   * Returns true if the number can only be dialled from within the region. If unknown, or the
   * number can be dialled from outside the region as well, returns false. Does not check the
   * number is a valid number.
   * TODO: Make this method public when we have enough metadata to make it worthwhile. Currently
   * visible for testing purposes only.
   *
   * @param number  the phone-number for which we want to know whether it is only diallable from
   *     within the region
   */
  boolean canBeInternationallyDialled(PhoneNumber number) {
    String regionCode = getRegionCodeForNumber(number);
    String nationalSignificantNumber = getNationalSignificantNumber(number);
    if (!hasValidRegionCode(regionCode, number.getCountryCode(), nationalSignificantNumber)) {
      return true;
    }
    PhoneMetadata metadata = getMetadataForRegion(regionCode);
    return !isNumberMatchingDesc(nationalSignificantNumber, metadata.getNoInternationalDialling());
  }
}
