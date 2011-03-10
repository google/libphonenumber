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
 */

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.PhoneNumberUtil.Leniency;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

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
   * A phone number pattern that does not allow whitespace as punctuation. This pattern is only used
   * in a second attempt to find a phone number occurring in the context of other numbers, such as
   * when the preceding or following token is a zip code.
   */
  private static final Pattern INNER;
  /**
   * Matches strings that look like publication pages. Example:
   * <pre>Computing Complete Answers to Queries in the Presence of Limited Access Patterns.
   * Chen Li. VLDB J. 12(3): 211-227 (2003).</pre>
   *
   * The string "211-227 (2003)" is not a telephone number.
   */
  private static final Pattern PUB_PAGES = Pattern.compile("\\d{1,5}-+\\d{1,5}\\s{0,4}\\(\\d{1,4}");

  static {
    /* Builds the PATTERN and INNER regular expression patterns. The building blocks below
     * exist to make the patterns more easily understood. */

    /* Limit on the number of leading (plus) characters. */
    String leadLimit = limit(0, 2);
    /* Limit on the number of consecutive punctuation characters. */
    String punctuationLimit = limit(0, 4);
    /* The maximum number of digits allowed in a digit-separated block. As we allow all digits in a
     * single block, set high enough to accommodate the entire national number and the international
     * country code. */
    int digitBlockLimit =
        PhoneNumberUtil.MAX_LENGTH_FOR_NSN + PhoneNumberUtil.MAX_LENGTH_COUNTRY_CODE;
    /* Limit on the number of blocks separated by punctuation. Use digitBlockLimit since in some
     * formats use spaces to separate each digit. */
    String blockLimit = limit(0, digitBlockLimit);

    /* Same as {@link PhoneNumberUtil#VALID_PUNCTUATION} but without space characters. */
    String nonSpacePunctuationChars = removeSpace(PhoneNumberUtil.VALID_PUNCTUATION);
    /* A punctuation sequence without white space. */
    String nonSpacePunctuation = "[" + nonSpacePunctuationChars + "]" + punctuationLimit;
    /* A punctuation sequence allowing white space. */
    String punctuation = "[" + PhoneNumberUtil.VALID_PUNCTUATION + "]" + punctuationLimit;
    /* A digits block without punctuation. */
    String digitSequence = "\\p{Nd}" + limit(1, digitBlockLimit);
    /* Punctuation that may be at the start of a phone number - brackets and plus signs. */
    String leadClass = "[(\\[" + PhoneNumberUtil.PLUS_CHARS + "]";

    /* Phone number pattern allowing optional punctuation. */
    PATTERN = Pattern.compile(
        "(?:" + leadClass + punctuation + ")" + leadLimit +
        digitSequence + "(?:" + punctuation + digitSequence + ")" + blockLimit +
        "(?:" + PhoneNumberUtil.KNOWN_EXTN_PATTERNS + ")?",
        PhoneNumberUtil.REGEX_FLAGS);

    /* Phone number pattern with no whitespace allowed. */
    INNER = Pattern.compile(
        leadClass + leadLimit +
        digitSequence + "(?:" + nonSpacePunctuation + digitSequence + ")" + blockLimit,
        PhoneNumberUtil.REGEX_FLAGS);
  }

  /** Returns a regular expression quantifier with an upper and lower limit. */
  private static String limit(int lower, int upper) {
    if ((lower < 0) || (upper <= 0) || (upper < lower)) {
      throw new IllegalArgumentException();
    }
    return "{" + lower + "," + upper + "}";
  }

  /**
   * Returns a copy of {@code characters} with any {@linkplain Character#isSpaceChar space}
   * characters removed.
   */
  private static String removeSpace(String characters) {
    StringBuilder builder = new StringBuilder(characters.length());
    int i = 0;
    while (i < characters.length()) {
      int codePoint = characters.codePointAt(i);
      if (!Character.isSpaceChar(codePoint)) {
        builder.appendCodePoint(codePoint);
      }
      i += Character.charCount(codePoint);
    }
    return builder.toString();
  }

  /** The potential states of a PhoneNumberMatcher. */
  private enum State {
    NOT_READY, READY, DONE
  }

  /** The phone number utility. */
  private final PhoneNumberUtil util;
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
   * @param country   the ISO 3166-1 two-letter country code indicating the country to assume for
   *                  phone numbers not written in international format (with a leading plus, or
   *                  with the international dialing prefix of the specified region). May be null or
   *                  "ZZ" if only numbers with a leading plus should be considered.
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
    this.util = util;
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
   * Attempts to extract a match from a {@code candidate} character sequence.
   *
   * @param candidate  the candidate text that might contain a phone number
   * @param offset  the offset of {@code candidate} within {@link #text}
   * @return  the match found, null if none can be found
   */
  private PhoneNumberMatch extractMatch(CharSequence candidate, int offset) {
    // Skip a match that is more likely a publication page reference.
    if (PUB_PAGES.matcher(candidate).find()) {
      return null;
    }

    // Try to come up with a valid match given the entire candidate.
    String rawString = candidate.toString();
    PhoneNumberMatch match = parseAndVerify(rawString, offset);
    if (match != null) {
      return match;
    }

    // If that failed, try to find an inner match without white space.
    return extractInnerMatch(rawString, offset);
  }

  /**
   * Attempts to extract a match from {@code candidate} using the {@link #INNER} pattern.
   *
   * @param candidate  the candidate text that might contain a phone number
   * @param offset  the offset of {@code candidate} within {@link #text}
   * @return  the match found, null if none can be found
   */
  private PhoneNumberMatch extractInnerMatch(String candidate, int offset) {
    int index = 0;
    Matcher matcher = INNER.matcher(candidate);
    while ((maxTries > 0) && matcher.find(index)) {
      String innerCandidate = candidate.substring(matcher.start(), matcher.end());
      PhoneNumberMatch match = parseAndVerify(innerCandidate, offset + matcher.start());
      if (match != null) {
        return match;
      }
      maxTries--;
      index = matcher.end();
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
      PhoneNumber number = util.parse(candidate, preferredRegion);
      if (leniency.verify(number, util)) {
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
