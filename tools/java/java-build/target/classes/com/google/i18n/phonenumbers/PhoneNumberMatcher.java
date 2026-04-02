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
 */

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.PhoneNumberUtil.Leniency;
import com.google.i18n.phonenumbers.PhoneNumberUtil.MatchType;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.internal.RegexCache;
import com.google.i18n.phonenumbers.metadata.DefaultMetadataDependenciesProvider;
import java.lang.Character.UnicodeBlock;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stateful class that finds and extracts telephone numbers from {@linkplain CharSequence text}.
 * Instances can be created using the {@linkplain PhoneNumberUtil#findNumbers factory methods} in
 * {@link PhoneNumberUtil}.
 *
 * <p>Vanity numbers (phone numbers using alphabetic digits such as <tt>1-800-SIX-FLAGS</tt> are
 * not found.
 *
 * <p>This class is not thread-safe.
 */
final class PhoneNumberMatcher implements Iterator<PhoneNumberMatch> {
  /**
   * The phone number pattern used by {@link #find}, similar to
   * {@code PhoneNumberUtil.VALID_PHONE_NUMBER}, but with the following differences:
   * <ul>
   *   <li>All captures are limited in order to place an upper bound to the text matched by the
   *       pattern.
   * <ul>
   *   <li>Leading punctuation / plus signs are limited.
   *   <li>Consecutive occurrences of punctuation are limited.
   *   <li>Number of digits is limited.
   * </ul>
   *   <li>No whitespace is allowed at the start or end.
   *   <li>No alpha digits (vanity numbers such as 1-800-SIX-FLAGS) are currently supported.
   * </ul>
   */
  private static final Pattern PATTERN;
  /**
   * Matches strings that look like publication pages. Example:
   * <pre>Computing Complete Answers to Queries in the Presence of Limited Access Patterns.
   * Chen Li. VLDB J. 12(3): 211-227 (2003).</pre>
   *
   * The string "211-227 (2003)" is not a telephone number.
   */
  private static final Pattern PUB_PAGES = Pattern.compile("\\d{1,5}-+\\d{1,5}\\s{0,4}\\(\\d{1,4}");

  /**
   * Matches strings that look like dates using "/" as a separator. Examples: 3/10/2011, 31/10/96 or
   * 08/31/95.
   */
  private static final Pattern SLASH_SEPARATED_DATES =
      Pattern.compile("(?:(?:[0-3]?\\d/[01]?\\d)|(?:[01]?\\d/[0-3]?\\d))/(?:[12]\\d)?\\d{2}");

  /**
   * Matches timestamps. Examples: "2012-01-02 08:00". Note that the reg-ex does not include the
   * trailing ":\d\d" -- that is covered by TIME_STAMPS_SUFFIX.
   */
  private static final Pattern TIME_STAMPS =
      Pattern.compile("[12]\\d{3}[-/]?[01]\\d[-/]?[0-3]\\d +[0-2]\\d$");
  private static final Pattern TIME_STAMPS_SUFFIX = Pattern.compile(":[0-5]\\d");

  /**
   * Pattern to check that brackets match. Opening brackets should be closed within a phone number.
   * This also checks that there is something inside the brackets. Having no brackets at all is also
   * fine.
   */
  private static final Pattern MATCHING_BRACKETS;

  /**
   * Patterns used to extract phone numbers from a larger phone-number-like pattern. These are
   * ordered according to specificity. For example, white-space is last since that is frequently
   * used in numbers, not just to separate two numbers. We have separate patterns since we don't
   * want to break up the phone-number-like text on more than one different kind of symbol at one
   * time, although symbols of the same type (e.g. space) can be safely grouped together.
   *
   * Note that if there is a match, we will always check any text found up to the first match as
   * well.
   */
  private static final Pattern[] INNER_MATCHES = {
      // Breaks on the slash - e.g. "651-234-2345/332-445-1234"
      Pattern.compile("/+(.*)"),
      // Note that the bracket here is inside the capturing group, since we consider it part of the
      // phone number. Will match a pattern like "(650) 223 3345 (754) 223 3321".
      Pattern.compile("(\\([^(]*)"),
      // Breaks on a hyphen - e.g. "12345 - 332-445-1234 is my number."
      // We require a space on either side of the hyphen for it to be considered a separator.
      Pattern.compile("(?:\\p{Z}-|-\\p{Z})\\p{Z}*(.+)"),
      // Various types of wide hyphens. Note we have decided not to enforce a space here, since it's
      // possible that it's supposed to be used to break two numbers without spaces, and we haven't
      // seen many instances of it used within a number.
      Pattern.compile("[\u2012-\u2015\uFF0D]\\p{Z}*(.+)"),
      // Breaks on a full stop - e.g. "12345. 332-445-1234 is my number."
      Pattern.compile("\\.+\\p{Z}*([^.]+)"),
      // Breaks on space - e.g. "3324451234 8002341234"
      Pattern.compile("\\p{Z}+(\\P{Z}+)")
  };

  /**
   * Punctuation that may be at the start of a phone number - brackets and plus signs.
   */
  private static final Pattern LEAD_CLASS;

  static {
    /* Builds the MATCHING_BRACKETS and PATTERN regular expressions. The building blocks below exist
     * to make the pattern more easily understood. */

    String openingParens = "(\\[\uFF08\uFF3B";
    String closingParens = ")\\]\uFF09\uFF3D";
    String nonParens = "[^" + openingParens + closingParens + "]";

    /* Limit on the number of pairs of brackets in a phone number. */
    String bracketPairLimit = limit(0, 3);
    /*
     * An opening bracket at the beginning may not be closed, but subsequent ones should be.  It's
     * also possible that the leading bracket was dropped, so we shouldn't be surprised if we see a
     * closing bracket first. We limit the sets of brackets in a phone number to four.
     */
    MATCHING_BRACKETS = Pattern.compile(
        "(?:[" + openingParens + "])?" + "(?:" + nonParens + "+" + "[" + closingParens + "])?"
        + nonParens + "+"
        + "(?:[" + openingParens + "]" + nonParens + "+[" + closingParens + "])" + bracketPairLimit
        + nonParens + "*");

    /* Limit on the number of leading (plus) characters. */
    String leadLimit = limit(0, 2);
    /* Limit on the number of consecutive punctuation characters. */
    String punctuationLimit = limit(0, 4);
    /* The maximum number of digits allowed in a digit-separated block. As we allow all digits in a
     * single block, set high enough to accommodate the entire national number and the international
     * country code. */
    int digitBlockLimit =
        PhoneNumberUtil.MAX_LENGTH_FOR_NSN + PhoneNumberUtil.MAX_LENGTH_COUNTRY_CODE;
    /* Limit on the number of blocks separated by punctuation. Uses digitBlockLimit since some
     * formats use spaces to separate each digit. */
    String blockLimit = limit(0, digitBlockLimit);

    /* A punctuation sequence allowing white space. */
    String punctuation = "[" + PhoneNumberUtil.VALID_PUNCTUATION + "]" + punctuationLimit;
    /* A digits block without punctuation. */
    String digitSequence = "\\p{Nd}" + limit(1, digitBlockLimit);

    String leadClassChars = openingParens + PhoneNumberUtil.PLUS_CHARS;
    String leadClass = "[" + leadClassChars + "]";
    LEAD_CLASS = Pattern.compile(leadClass);

    /* Phone number pattern allowing optional punctuation. */
    PATTERN = Pattern.compile(
        "(?:" + leadClass + punctuation + ")" + leadLimit
        + digitSequence + "(?:" + punctuation + digitSequence + ")" + blockLimit
        + "(?:" + PhoneNumberUtil.EXTN_PATTERNS_FOR_MATCHING + ")?",
        PhoneNumberUtil.REGEX_FLAGS);
  }

  /** Returns a regular expression quantifier with an upper and lower limit. */
  private static String limit(int lower, int upper) {
    if ((lower < 0) || (upper <= 0) || (upper < lower)) {
      throw new IllegalArgumentException();
    }
    return "{" + lower + "," + upper + "}";
  }

  /** The potential states of a PhoneNumberMatcher. */
  private enum State {
    NOT_READY, READY, DONE
  }

  /** The phone number utility. */
  private final PhoneNumberUtil phoneUtil;
  /** The text searched for phone numbers. */
  private final CharSequence text;
  /**
   * The region (country) to assume for phone numbers without an international prefix, possibly
   * null.
   */
  private final String preferredRegion;
  /** The degree of validation requested. */
  private final Leniency leniency;
  /** The maximum number of retries after matching an invalid number. */
  private long maxTries;

  /** The iteration tristate. */
  private State state = State.NOT_READY;
  /** The last successful match, null unless in {@link State#READY}. */
  private PhoneNumberMatch lastMatch = null;
  /** The next index to start searching at. Undefined in {@link State#DONE}. */
  private int searchIndex = 0;

  // A cache for frequently used country-specific regular expressions. Set to 32 to cover ~2-3
  // countries being used for the same doc with ~10 patterns for each country. Some pages will have
  // a lot more countries in use, but typically fewer numbers for each so expanding the cache for
  // that use-case won't have a lot of benefit.
  private final RegexCache regexCache = new RegexCache(32);

  /**
   * Creates a new instance. See the factory methods in {@link PhoneNumberUtil} on how to obtain a
   * new instance.
   *
   * @param util  the phone number util to use
   * @param text  the character sequence that we will search, null for no text
   * @param country  the country to assume for phone numbers not written in international format
   *     (with a leading plus, or with the international dialing prefix of the specified region).
   *     May be null or "ZZ" if only numbers with a leading plus should be
   *     considered.
   * @param leniency  the leniency to use when evaluating candidate phone numbers
   * @param maxTries  the maximum number of invalid numbers to try before giving up on the text.
   *     This is to cover degenerate cases where the text has a lot of false positives in it. Must
   *     be {@code >= 0}.
   */
  PhoneNumberMatcher(PhoneNumberUtil util, CharSequence text, String country, Leniency leniency,
      long maxTries) {

    if ((util == null) || (leniency == null)) {
      throw new NullPointerException();
    }
    if (maxTries < 0) {
      throw new IllegalArgumentException();
    }
    this.phoneUtil = util;
    this.text = (text != null) ? text : "";
    this.preferredRegion = country;
    this.leniency = leniency;
    this.maxTries = maxTries;
  }

  /**
   * Attempts to find the next subsequence in the searched sequence on or after {@code searchIndex}
   * that represents a phone number. Returns the next match, null if none was found.
   *
   * @param index  the search index to start searching at
   * @return  the phone number match found, null if none can be found
   */
  private PhoneNumberMatch find(int index) {
    Matcher matcher = PATTERN.matcher(text);
    while ((maxTries > 0) && matcher.find(index)) {
      int start = matcher.start();
      CharSequence candidate = text.subSequence(start, matcher.end());

      // Check for extra numbers at the end.
      // TODO: This is the place to start when trying to support extraction of multiple phone number
      // from split notations (+41 79 123 45 67 / 68).
      candidate = trimAfterFirstMatch(PhoneNumberUtil.SECOND_NUMBER_START_PATTERN, candidate);

      PhoneNumberMatch match = extractMatch(candidate, start);
      if (match != null) {
        return match;
      }

      index = start + candidate.length();
      maxTries--;
    }

    return null;
  }

  /**
   * Trims away any characters after the first match of {@code pattern} in {@code candidate},
   * returning the trimmed version.
   */
  private static CharSequence trimAfterFirstMatch(Pattern pattern, CharSequence candidate) {
    Matcher trailingCharsMatcher = pattern.matcher(candidate);
    if (trailingCharsMatcher.find()) {
      candidate = candidate.subSequence(0, trailingCharsMatcher.start());
    }
    return candidate;
  }

  /**
   * Helper method to determine if a character is a Latin-script letter or not. For our purposes,
   * combining marks should also return true since we assume they have been added to a preceding
   * Latin character.
   */
  // @VisibleForTesting
  static boolean isLatinLetter(char letter) {
    // Combining marks are a subset of non-spacing-mark.
    if (!Character.isLetter(letter) && Character.getType(letter) != Character.NON_SPACING_MARK) {
      return false;
    }
    UnicodeBlock block = UnicodeBlock.of(letter);
    return block.equals(UnicodeBlock.BASIC_LATIN)
        || block.equals(UnicodeBlock.LATIN_1_SUPPLEMENT)
        || block.equals(UnicodeBlock.LATIN_EXTENDED_A)
        || block.equals(UnicodeBlock.LATIN_EXTENDED_ADDITIONAL)
        || block.equals(UnicodeBlock.LATIN_EXTENDED_B)
        || block.equals(UnicodeBlock.COMBINING_DIACRITICAL_MARKS);
  }

  private static boolean isInvalidPunctuationSymbol(char character) {
    return character == '%' || Character.getType(character) == Character.CURRENCY_SYMBOL;
  }

  /**
   * Attempts to extract a match from a {@code candidate} character sequence.
   *
   * @param candidate  the candidate text that might contain a phone number
   * @param offset  the offset of {@code candidate} within {@link #text}
   * @return  the match found, null if none can be found
   */
  private PhoneNumberMatch extractMatch(CharSequence candidate, int offset) {
    // Skip a match that is more likely to be a date.
    if (SLASH_SEPARATED_DATES.matcher(candidate).find()) {
      return null;
    }

    // Skip potential time-stamps.
    if (TIME_STAMPS.matcher(candidate).find()) {
      String followingText = text.toString().substring(offset + candidate.length());
      if (TIME_STAMPS_SUFFIX.matcher(followingText).lookingAt()) {
        return null;
      }
    }

    // Try to come up with a valid match given the entire candidate.
    PhoneNumberMatch match = parseAndVerify(candidate, offset);
    if (match != null) {
      return match;
    }

    // If that failed, try to find an "inner match" - there might be a phone number within this
    // candidate.
    return extractInnerMatch(candidate, offset);
  }

  /**
   * Attempts to extract a match from {@code candidate} if the whole candidate does not qualify as a
   * match.
   *
   * @param candidate  the candidate text that might contain a phone number
   * @param offset  the current offset of {@code candidate} within {@link #text}
   * @return  the match found, null if none can be found
   */
  private PhoneNumberMatch extractInnerMatch(CharSequence candidate, int offset) {
    for (Pattern possibleInnerMatch : INNER_MATCHES) {
      Matcher groupMatcher = possibleInnerMatch.matcher(candidate);
      boolean isFirstMatch = true;
      while (groupMatcher.find() && maxTries > 0) {
        if (isFirstMatch) {
          // We should handle any group before this one too.
          CharSequence group = trimAfterFirstMatch(
              PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN,
              candidate.subSequence(0, groupMatcher.start()));
          PhoneNumberMatch match = parseAndVerify(group, offset);
          if (match != null) {
            return match;
          }
          maxTries--;
          isFirstMatch = false;
        }
        CharSequence group = trimAfterFirstMatch(
            PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN, groupMatcher.group(1));
        PhoneNumberMatch match = parseAndVerify(group, offset + groupMatcher.start(1));
        if (match != null) {
          return match;
        }
        maxTries--;
      }
    }
    return null;
  }

  /**
   * Parses a phone number from the {@code candidate} using {@link PhoneNumberUtil#parse} and
   * verifies it matches the requested {@link #leniency}. If parsing and verification succeed, a
   * corresponding {@link PhoneNumberMatch} is returned, otherwise this method returns null.
   *
   * @param candidate  the candidate match
   * @param offset  the offset of {@code candidate} within {@link #text}
   * @return  the parsed and validated phone number match, or null
   */
  private PhoneNumberMatch parseAndVerify(CharSequence candidate, int offset) {
    try {
      // Check the candidate doesn't contain any formatting which would indicate that it really
      // isn't a phone number.
      if (!MATCHING_BRACKETS.matcher(candidate).matches() || PUB_PAGES.matcher(candidate).find()) {
        return null;
      }

      // If leniency is set to VALID or stricter, we also want to skip numbers that are surrounded
      // by Latin alphabetic characters, to skip cases like abc8005001234 or 8005001234def.
      if (leniency.compareTo(Leniency.VALID) >= 0) {
        // If the candidate is not at the start of the text, and does not start with phone-number
        // punctuation, check the previous character.
        if (offset > 0 && !LEAD_CLASS.matcher(candidate).lookingAt()) {
          char previousChar = text.charAt(offset - 1);
          // We return null if it is a latin letter or an invalid punctuation symbol.
          if (isInvalidPunctuationSymbol(previousChar) || isLatinLetter(previousChar)) {
            return null;
          }
        }
        int lastCharIndex = offset + candidate.length();
        if (lastCharIndex < text.length()) {
          char nextChar = text.charAt(lastCharIndex);
          if (isInvalidPunctuationSymbol(nextChar) || isLatinLetter(nextChar)) {
            return null;
          }
        }
      }

      PhoneNumber number = phoneUtil.parseAndKeepRawInput(candidate, preferredRegion);

      if (leniency.verify(number, candidate, phoneUtil, this)) {
        // We used parseAndKeepRawInput to create this number, but for now we don't return the extra
        // values parsed. TODO: stop clearing all values here and switch all users over
        // to using rawInput() rather than the rawString() of PhoneNumberMatch.
        number.clearCountryCodeSource();
        number.clearRawInput();
        number.clearPreferredDomesticCarrierCode();
        return new PhoneNumberMatch(offset, candidate.toString(), number);
      }
    } catch (NumberParseException e) {
      // ignore and continue
    }
    return null;
  }

  /**
   * Small helper interface such that the number groups can be checked according to different
   * criteria, both for our default way of performing formatting and for any alternate formats we
   * may want to check.
   */
  interface NumberGroupingChecker {
    /**
     * Returns true if the groups of digits found in our candidate phone number match our
     * expectations.
     *
     * @param number  the original number we found when parsing
     * @param normalizedCandidate  the candidate number, normalized to only contain ASCII digits,
     *     but with non-digits (spaces etc) retained
     * @param expectedNumberGroups  the groups of digits that we would expect to see if we
     *     formatted this number
     */
    boolean checkGroups(PhoneNumberUtil util, PhoneNumber number,
                        StringBuilder normalizedCandidate, String[] expectedNumberGroups);
  }

  static boolean allNumberGroupsRemainGrouped(PhoneNumberUtil util,
                                              PhoneNumber number,
                                              StringBuilder normalizedCandidate,
                                              String[] formattedNumberGroups) {
    int fromIndex = 0;
    if (number.getCountryCodeSource() != CountryCodeSource.FROM_DEFAULT_COUNTRY) {
      // First skip the country code if the normalized candidate contained it.
      String countryCode = Integer.toString(number.getCountryCode());
      fromIndex = normalizedCandidate.indexOf(countryCode) + countryCode.length();
    }
    // Check each group of consecutive digits are not broken into separate groupings in the
    // {@code normalizedCandidate} string.
    for (int i = 0; i < formattedNumberGroups.length; i++) {
      // Fails if the substring of {@code normalizedCandidate} starting from {@code fromIndex}
      // doesn't contain the consecutive digits in formattedNumberGroups[i].
      fromIndex = normalizedCandidate.indexOf(formattedNumberGroups[i], fromIndex);
      if (fromIndex < 0) {
        return false;
      }
      // Moves {@code fromIndex} forward.
      fromIndex += formattedNumberGroups[i].length();
      if (i == 0 && fromIndex < normalizedCandidate.length()) {
        // We are at the position right after the NDC. We get the region used for formatting
        // information based on the country code in the phone number, rather than the number itself,
        // as we do not need to distinguish between different countries with the same country
        // calling code and this is faster.
        String region = util.getRegionCodeForCountryCode(number.getCountryCode());
        if (util.getNddPrefixForRegion(region, true) != null
            && Character.isDigit(normalizedCandidate.charAt(fromIndex))) {
          // This means there is no formatting symbol after the NDC. In this case, we only
          // accept the number if there is no formatting symbol at all in the number, except
          // for extensions. This is only important for countries with national prefixes.
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

  static boolean allNumberGroupsAreExactlyPresent(PhoneNumberUtil util,
                                                  PhoneNumber number,
                                                  StringBuilder normalizedCandidate,
                                                  String[] formattedNumberGroups) {
    String[] candidateGroups =
        PhoneNumberUtil.NON_DIGITS_PATTERN.split(normalizedCandidate.toString());
    // Set this to the last group, skipping it if the number has an extension.
    int candidateNumberGroupIndex =
        number.hasExtension() ? candidateGroups.length - 2 : candidateGroups.length - 1;
    // First we check if the national significant number is formatted as a block.
    // We use contains and not equals, since the national significant number may be present with
    // a prefix such as a national number prefix, or the country code itself.
    if (candidateGroups.length == 1
        || candidateGroups[candidateNumberGroupIndex].contains(
            util.getNationalSignificantNumber(number))) {
      return true;
    }
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
    return (candidateNumberGroupIndex >= 0
        && candidateGroups[candidateNumberGroupIndex].endsWith(formattedNumberGroups[0]));
  }

  /**
   * Helper method to get the national-number part of a number, formatted without any national
   * prefix, and return it as a set of digit blocks that would be formatted together following
   * standard formatting rules.
   */
  private static String[] getNationalNumberGroups(PhoneNumberUtil util, PhoneNumber number) {
    // This will be in the format +CC-DG1-DG2-DGX;ext=EXT where DG1..DGX represents groups of
    // digits.
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

  /**
   * Helper method to get the national-number part of a number, formatted without any national
   * prefix, and return it as a set of digit blocks that should be formatted together according to
   * the formatting pattern passed in.
   */
  private static String[] getNationalNumberGroups(PhoneNumberUtil util, PhoneNumber number,
                                                  NumberFormat formattingPattern) {
    // If a format is provided, we format the NSN only, and split that according to the separator.
    String nationalSignificantNumber = util.getNationalSignificantNumber(number);
    return util.formatNsnUsingPattern(nationalSignificantNumber,
                                      formattingPattern, PhoneNumberFormat.RFC3966).split("-");
  }

  boolean checkNumberGroupingIsValid(
      PhoneNumber number, CharSequence candidate, PhoneNumberUtil util,
      NumberGroupingChecker checker) {
    StringBuilder normalizedCandidate =
        PhoneNumberUtil.normalizeDigits(candidate, true /* keep non-digits */);
    String[] formattedNumberGroups = getNationalNumberGroups(util, number);
    if (checker.checkGroups(util, number, normalizedCandidate, formattedNumberGroups)) {
      return true;
    }
    // If this didn't pass, see if there are any alternate formats that match, and try them instead.
    PhoneMetadata alternateFormats =
        DefaultMetadataDependenciesProvider.getInstance()
            .getAlternateFormatsMetadataSource()
              .getFormattingMetadataForCountryCallingCode(number.getCountryCode());
    String nationalSignificantNumber = util.getNationalSignificantNumber(number);
    if (alternateFormats != null) {
      for (NumberFormat alternateFormat : alternateFormats.getNumberFormatList()) {
        if (alternateFormat.getLeadingDigitsPatternCount() > 0) {
          // There is only one leading digits pattern for alternate formats.
          Pattern pattern =
              regexCache.getPatternForRegex(alternateFormat.getLeadingDigitsPattern(0));
          if (!pattern.matcher(nationalSignificantNumber).lookingAt()) {
            // Leading digits don't match; try another one.
            continue;
          }
        }
        formattedNumberGroups = getNationalNumberGroups(util, number, alternateFormat);
        if (checker.checkGroups(util, number, normalizedCandidate, formattedNumberGroups)) {
          return true;
        }
      }
    }
    return false;
  }

  static boolean containsMoreThanOneSlashInNationalNumber(PhoneNumber number, String candidate) {
    int firstSlashInBodyIndex = candidate.indexOf('/');
    if (firstSlashInBodyIndex < 0) {
      // No slashes, this is okay.
      return false;
    }
    // Now look for a second one.
    int secondSlashInBodyIndex = candidate.indexOf('/', firstSlashInBodyIndex + 1);
    if (secondSlashInBodyIndex < 0) {
      // Only one slash, this is okay.
      return false;
    }

    // If the first slash is after the country calling code, this is permitted.
    boolean candidateHasCountryCode =
        (number.getCountryCodeSource() == CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN
         || number.getCountryCodeSource() == CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN);
    if (candidateHasCountryCode
        && PhoneNumberUtil.normalizeDigitsOnly(candidate.substring(0, firstSlashInBodyIndex))
            .equals(Integer.toString(number.getCountryCode()))) {
      // Any more slashes and this is illegal.
      return candidate.substring(secondSlashInBodyIndex + 1).contains("/");
    }
    return true;
  }

  static boolean containsOnlyValidXChars(
      PhoneNumber number, String candidate, PhoneNumberUtil util) {
    // The characters 'x' and 'X' can be (1) a carrier code, in which case they always precede the
    // national significant number or (2) an extension sign, in which case they always precede the
    // extension number. We assume a carrier code is more than 1 digit, so the first case has to
    // have more than 1 consecutive 'x' or 'X', whereas the second case can only have exactly 1 'x'
    // or 'X'. We ignore the character if it appears as the last character of the string.
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

  static boolean isNationalPrefixPresentIfRequired(PhoneNumber number, PhoneNumberUtil util) {
    // First, check how we deduced the country code. If it was written in international format, then
    // the national prefix is not required.
    if (number.getCountryCodeSource() != CountryCodeSource.FROM_DEFAULT_COUNTRY) {
      return true;
    }
    String phoneNumberRegion =
        util.getRegionCodeForCountryCode(number.getCountryCode());
    PhoneMetadata metadata = util.getMetadataForRegion(phoneNumberRegion);
    if (metadata == null) {
      return true;
    }
    // Check if a national prefix should be present when formatting this number.
    String nationalNumber = util.getNationalSignificantNumber(number);
    NumberFormat formatRule =
        util.chooseFormattingPatternForNumber(metadata.getNumberFormatList(), nationalNumber);
    // To do this, we check that a national prefix formatting rule was present and that it wasn't
    // just the first-group symbol ($1) with punctuation.
    if ((formatRule != null) && formatRule.getNationalPrefixFormattingRule().length() > 0) {
      if (formatRule.getNationalPrefixOptionalWhenFormatting()) {
        // The national-prefix is optional in these cases, so we don't need to check if it was
        // present.
        return true;
      }
      if (PhoneNumberUtil.formattingRuleHasFirstGroupOnly(
          formatRule.getNationalPrefixFormattingRule())) {
        // National Prefix not needed for this number.
        return true;
      }
      // Normalize the remainder.
      String rawInputCopy = PhoneNumberUtil.normalizeDigitsOnly(number.getRawInput());
      StringBuilder rawInput = new StringBuilder(rawInputCopy);
      // Check if we found a national prefix and/or carrier code at the start of the raw input, and
      // return the result.
      return util.maybeStripNationalPrefixAndCarrierCode(rawInput, metadata, null);
    }
    return true;
  }

  @Override
  public boolean hasNext() {
    if (state == State.NOT_READY) {
      lastMatch = find(searchIndex);
      if (lastMatch == null) {
        state = State.DONE;
      } else {
        searchIndex = lastMatch.end();
        state = State.READY;
      }
    }
    return state == State.READY;
  }

  @Override
  public PhoneNumberMatch next() {
    // Check the state and find the next match as a side-effect if necessary.
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    // Don't retain that memory any longer than necessary.
    PhoneNumberMatch result = lastMatch;
    lastMatch = null;
    state = State.NOT_READY;
    return result;
  }

  /**
   * Always throws {@link UnsupportedOperationException} as removal is not supported.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
