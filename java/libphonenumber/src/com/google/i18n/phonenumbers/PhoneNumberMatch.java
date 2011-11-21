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

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.Arrays;

/**
 * The immutable match of a phone number within a piece of text. Matches may be found using
 * {@link PhoneNumberUtil#findNumbers}.
 *
 * <p>A match consists of the {@linkplain #number() phone number} as well as the
 * {@linkplain #start() start} and {@linkplain #end() end} offsets of the corresponding subsequence
 * of the searched text. Use {@link #rawString()} to obtain a copy of the matched subsequence.
 *
 * <p>The following annotated example clarifies the relationship between the searched text, the
 * match offsets, and the parsed number:

 * <pre>
 * CharSequence text = "Call me at +1 425 882-8080 for details.";
 * RegionCode country = RegionCode.US;
 * PhoneNumberUtil util = PhoneNumberUtil.getInstance();
 *
 * // Find the first phone number match:
 * PhoneNumberMatch m = util.findNumbers(text, country).iterator().next();
 *
 * // rawString() contains the phone number as it appears in the text.
 * "+1 425 882-8080".equals(m.rawString());
 *
 * // start() and end() define the range of the matched subsequence.
 * CharSequence subsequence = text.subSequence(m.start(), m.end());
 * "+1 425 882-8080".contentEquals(subsequence);
 *
 * // number() returns the the same result as PhoneNumberUtil.{@link PhoneNumberUtil#parse parse()}
 * // invoked on rawString().
 * util.parse(m.rawString(), country).equals(m.number());
 * </pre>
 *
 * @author Tom Hofmann
 */
public final class PhoneNumberMatch {
  /** The start index into the text. */
  private final int start;
  /** The raw substring matched. */
  private final String rawString;
  /** The matched phone number. */
  private final PhoneNumber number;

  /**
   * Creates a new match.
   *
   * @param start  the start index into the target text
   * @param rawString  the matched substring of the target text
   * @param number  the matched phone number
   */
  PhoneNumberMatch(int start, String rawString, PhoneNumber number) {
    if (start < 0) {
      throw new IllegalArgumentException("Start index must be >= 0.");
    }
    if (rawString == null || number == null) {
      throw new NullPointerException();
    }
    this.start = start;
    this.rawString = rawString;
    this.number = number;
  }

  /** Returns the phone number matched by the receiver. */
  public PhoneNumber number() {
    return number;
  }

  /** Returns the start index of the matched phone number within the searched text. */
  public int start() {
    return start;
  }

  /** Returns the exclusive end index of the matched phone number within the searched text. */
  public int end() {
    return start + rawString.length();
  }

  /** Returns the raw string matched as a phone number in the searched text. */
  public String rawString() {
    return rawString;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{start, rawString, number});
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PhoneNumberMatch)) {
      return false;
    }
    PhoneNumberMatch other = (PhoneNumberMatch) obj;
    return rawString.equals(other.rawString) && (start == other.start) &&
        number.equals(other.number);
  }

  @Override
  public String toString() {
    return "PhoneNumberMatch [" + start() + "," + end() + ") " + rawString;
  }
}
