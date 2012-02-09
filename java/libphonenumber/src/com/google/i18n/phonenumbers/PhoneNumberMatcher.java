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
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

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
 *
 * @author Tom Hofmann
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
      Pattern.compile("[12]\\d{3}[-/]?[01]\\d[-/]?[0-3]\\d [0-2]\\d$");
  private static final Pattern TIME_STAMPS_SUFFIX = Pattern.compile(":[0-5]\\d");

  /**
   * Pattern to check that brackets match. Opening brackets should be closed within a phone number.
   * This also checks that there is something inside the brackets. Having no brackets at all is also
   * fine.
   */
  private static final Pattern MATCHING_BRACKETS;

  /**
   * Matches white-space, which may indicate the end of a phone number and the start of something
   * else (such as a neighbouring zip-code). If white-space is found, continues to match all
   * characters that are not typically used to start a phone number.
   */
  private static final Pattern GROUP_SEPARATOR;

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
        "(?:[" + openingParens + "])?" + "(?:" + nonParens + "+" + "[" + closingParens + "])?" +
        nonParens + "+" +
        "(?:[" + openingParens + "]" + nonParens + "+[" + closingParens + "])" + bracketPairLimit +
        nonParens + "*");

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
    GROUP_SEPARATOR = Pattern.compile("\\p{Z}" + "[^" + leadClassChars  + "\\p{Nd}]*");

    /* Phone number pattern allowing optional punctuation. */
    PATTERN = Pattern.compile(
        "(?:" + leadClass + punctuation + ")" + leadLimit +
        digitSequence + "(?:" + punctuation + digitSequence + ")" + blockLimit +
        "(?:" + PhoneNumberUtil.EXTN_PATTERNS_FOR_MATCHING + ")?",
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

  /**
   * Creates a new instance. See the factory methods in {@link PhoneNumberUtil} on how to obtain a
   * new instance.
   *
   * @param util      the phone number util to use
   * @param text      the character sequence that we will search, null for no text
   * @param country   the country to assume for phone numbers not written in international format
   *                  (with a leading plus, or with the international dialing prefix of the
   *                  specified region). May be null or "ZZ" if only numbers with a
   *                  leading plus should be considered.
   * @param leniency  the leniency to use when evaluating candidate phone numbers
   * @param maxTries  the maximum number of invalid numbers to try before giving up on the text.
   *                  This is to cover degenerate cases where the text has a lot of false positives
   *                  in it. Must be {@code >= 0}.
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
    return block.equals(UnicodeBlock.BASIC_LATIN) ||
        block.equals(UnicodeBlock.LATIN_1_SUPPLEMENT) ||
        block.equals(UnicodeBlock.LATIN_EXTENDED_A) ||
        block.equals(UnicodeBlock.LATIN_EXTENDED_ADDITIONAL) ||
        block.equals(UnicodeBlock.LATIN_EXTENDED_B) ||
        block.equals(UnicodeBlock.COMBINING_DIACRITICAL_MARKS);
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
    // Skip a match that is more likely a publication page reference or a date.
    if (PUB_PAGES.matcher(candidate).find() || SLASH_SEPARATED_DATES.matcher(candidate).find()) {
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
    String rawString = candidate.toString();
    PhoneNumberMatch match = parseAndVerify(rawString, offset);
    if (match != null) {
      return match;
    }

    // If that failed, try to find an "inner match" - there might be a phone number within this
    // candidate.
    return extractInnerMatch(rawString, offset);
  }

  /**
   * Attempts to extract a match from {@code candidate} if the whole candidate does not qualify as a
   * match.
   *
   * @param candidate  the candidate text that might contain a phone number
   * @param offset  the current offset of {@code candidate} within {@link #text}
   * @return  the match found, null if none can be found
   */
  private PhoneNumberMatch extractInnerMatch(String candidate, int offset) {
    // Try removing either the first or last "group" in the number and see if this gives a result.
    // We consider white space to be a possible indication of the start or end of the phone number.
    Matcher groupMatcher = GROUP_SEPARATOR.matcher(candidate);

    if (groupMatcher.find()) {
      // Try the first group by itself.
      CharSequence firstGroupOnly = candidate.substring(0, groupMatcher.start());
      firstGroupOnly = trimAfterFirstMatch(PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN,
                                           firstGroupOnly);
      PhoneNumberMatch match = parseAndVerify(firstGroupOnly.toString(), offset);
      if (match != null) {
        return match;
      }
      maxTries--;

      int withoutFirstGroupStart = groupMatcher.end();
      // Try the rest of the candidate without the first group.
      CharSequence withoutFirstGroup = candidate.substring(withoutFirstGroupStart);
      withoutFirstGroup = trimAfterFirstMatch(PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN,
                                              withoutFirstGroup);
      match = parseAndVerify(withoutFirstGroup.toString(), offset + withoutFirstGroupStart);
      if (match != null) {
        return match;
      }
      maxTries--;

      if (maxTries > 0) {
        int lastGroupStart = withoutFirstGroupStart;
        while (groupMatcher.find()) {
          // Find the last group.
          lastGroupStart = groupMatcher.start();
        }
        CharSequence withoutLastGroup = candidate.substring(0, lastGroupStart);
        withoutLastGroup = trimAfterFirstMatch(PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN,
                                               withoutLastGroup);
        if (withoutLastGroup.equals(firstGroupOnly)) {
          // If there are only two groups, then the group "without the last group" is the same as
          // the first group. In these cases, we don't want to re-check the number group, so we exit
          // already.
          return null;
        }
        match = parseAndVerify(withoutLastGroup.toString(), offset);
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
  private PhoneNumberMatch parseAndVerify(String candidate, int offset) {
    try {
      // Check the candidate doesn't contain any formatting which would indicate that it really
      // isn't a phone number.
      if (!MATCHING_BRACKETS.matcher(candidate).matches()) {
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
      if (leniency.verify(number, candidate, phoneUtil)) {
        // We used parseAndKeepRawInput to create this number, but for now we don't return the extra
        // values parsed. TODO: stop clearing all values here and switch all users over
        // to using rawInput() rather than the rawString() of PhoneNumberMatch.
        number.clearCountryCodeSource();
        number.clearRawInput();
        number.clearPreferredDomesticCarrierCode();
        return new PhoneNumberMatch(offset, candidate, number);
      }
    } catch (NumberParseException e) {
      // ignore and continue
    }
    return null;
  }

  /**
   * Always throws {@link UnsupportedOperationException} as removal is not supported.
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
