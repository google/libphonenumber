/*
 * Copyright (C) 2017 The Libphonenumber Authors.
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
package com.google.i18n.phonenumbers.metadata;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.DiscreteDomain;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;

/**
 * A small, fast, immutable representation of a phone number digit sequence. This class represents
 * contiguous sequences of digits in phone numbers, such as "123" or "000". It does not encode
 * semantic information such as the region code to which a number belongs or perform any semantic
 * validation. It can be thought of as equivalent to a String containing only the ASCII digits
 * {@code '0'} to {@code '9'}.
 */
@Immutable
public final class DigitSequence implements Comparable<DigitSequence> {

  private static final CharMatcher ASCII_DIGITS = CharMatcher.inRange('0', '9');

  // IMPORTANT
  // This cannot be more than 18 to avoid overflowing a signed long (it must be signed due to the
  // calculation of the "distance" metric which can be +ve or -ve).
  //
  // If it does need to be raised, this whole class probably needs to be rethought. ITU recommends
  // a limit of 15 digits (not including country calling code) but there are currently 2 examples
  // in the metadata XML file which exceed this (Japan) where some non-international toll free
  // numbers (those starting with 0037 and 0036) can be up to 17 digits (still okay) in the current
  // metadata but there's a note saying that they may even extend to 21 digits!!
  //
  // An appropriate way to split this class would be to make a closed type hierarchy with 2
  // separate implementations, one using a long to encode the numbers and one using BigInteger (or
  // maybe just encoding digits in a string directly).
  // The good thing about this approach is that instances of the different implementations could
  // never be equal to each other. This is likely not a difficult refactoring, although the Domain
  // class will also need to be considered carefully and details like the "index()" value will have
  // to change completely between the classes.
  //
  /** The maximum number of digits which can be held in a digit sequence. */
  public static final int MAX_DIGITS = 18;

  // Simple lookup of powers-of-10 for all valid sequence lengths (0 - MAX_DIGITS).
  private static final long[] POWERS_OF_TEN = new long[MAX_DIGITS + 1];
  static {
    // 1, 10, 100, 1000, 10000 ...
    POWERS_OF_TEN[0] = 1;
    for (int n = 1; n < POWERS_OF_TEN.length; n++) {
      POWERS_OF_TEN[n] = 10 * POWERS_OF_TEN[n - 1];
    }
  }

  // A table of adjustment values to convert a digit sequence into an absolute index in the
  // integer domain, to impose a true lexicographical ordering. The value of a digit sequence is
  // adjusted by the number of additional elements in the phone number domain which cannot be
  // represented as integers (the empty sequence or anything with leading zeros). This results in
  // an absolute ordering of all digit sequences. For example the digit sequence "0123" is length
  // 4, and there are 111 additional additional elements that come before 4-length sequences
  // ("", "00"-"09", "000"-"099"), so its index is {@code 123 + 111 = 234}.
  // To calculate this value dynamically for any length N, offset=floor(10^N / 9).
  private static final long[] DOMAIN_OFFSET = new long[MAX_DIGITS + 1];
  static {
    // 0, 1, 11, 111, 1111 ...
    for (int n = 1; n < DOMAIN_OFFSET.length; n++) {
      DOMAIN_OFFSET[n] = 10 * DOMAIN_OFFSET[n - 1] + 1;
    }
  }

  private static final DigitSequence EMPTY = new DigitSequence(0, 0L);
  private static final DigitSequence[] SINGLETON_DIGITS = new DigitSequence[] {
      new DigitSequence(1, 0L),
      new DigitSequence(1, 1L),
      new DigitSequence(1, 2L),
      new DigitSequence(1, 3L),
      new DigitSequence(1, 4L),
      new DigitSequence(1, 5L),
      new DigitSequence(1, 6L),
      new DigitSequence(1, 7L),
      new DigitSequence(1, 8L),
      new DigitSequence(1, 9L),
  };

  // Simple helper to return {@code 10^n} for all valid sequence lengths.
  private static long pow10(int n) {
    return POWERS_OF_TEN[n];
  }

  /**
   * Returns the domain in which phone number digit sequences exist. This is needed when creating
   * canonical {@link com.google.common.collect.Range Ranges} of digit-sequences.
   */
  public static DiscreteDomain<DigitSequence> domain() {
    return Domain.INSTANCE;
  }

  private static final class Domain extends DiscreteDomain<DigitSequence> {
    private static final Domain INSTANCE = new Domain();
    private static final DigitSequence MIN = EMPTY;
    private static final DigitSequence MAX = DigitSequence.of("999999999999999999");

    @Override
    public DigitSequence next(DigitSequence num) {
      long next = num.value + 1;
      if (next < pow10(num.length)) {
        return new DigitSequence(num.length, next);
      } else {
        int len = num.length + 1;
        return (len <= MAX_DIGITS) ? new DigitSequence(len, 0) : null;
      }
    }

    @Override
    public DigitSequence previous(DigitSequence num) {
      long prev = num.value - 1;
      if (prev >= 0) {
        return new DigitSequence(num.length, prev);
      } else {
        int len = num.length - 1;
        return (len >= 0) ? new DigitSequence(len, pow10(len) - 1) : null;
      }
    }

    @Override
    public long distance(DigitSequence start, DigitSequence end) {
      // The indices get up to 19 digits but can't overflow Long.MAX_VALUE, so they can be safely
      // subtracted to get a signed long "distance" without risk of over-/under- flow.
      return end.index() - start.index();
    }

    @Override
    public DigitSequence minValue() {
      return MIN;
    }

    @Override
    public DigitSequence maxValue() {
      return MAX;
    }
  }

  /** Returns the digit sequence of length one representing the given digit value. */
  public static DigitSequence singleton(int digit) {
    Preconditions.checkArgument(0 <= digit && digit <= 9, "invalid digit value: %s", digit);
    return SINGLETON_DIGITS[digit];
  }

  /**
   * Returns the empty digit sequence. This is useful in special cases where you need to build up
   * a digit sequence starting from nothing).
   */
  public static DigitSequence empty() {
    return EMPTY;
  }

  /** Returns a digit sequence for the given string (e.g. "012345"). */
  public static DigitSequence of(String digits) {
    Preconditions.checkArgument(digits.length() <= MAX_DIGITS,
        "Digit string too long: '%s'", digits);
    Preconditions.checkArgument(ASCII_DIGITS.matchesAllOf(digits),
        "Digit string contains non-digit characters: '%s'", digits);
    return digits.isEmpty() ? empty() : new DigitSequence(digits.length(), Long.parseLong(digits));
  }

  /**
   * Returns a digit sequence of {@code length} containing only the digit '0'. This is useful when
   * performing range calculations to determine the smallest digit sequence in a block.
   */
  public static DigitSequence zeros(int length) {
    return new DigitSequence(length, 0L);
  }

  /**
   * Returns a digit sequence of {@code length} containing only the digit '9'. This is useful when
   * performing range calculations to determine the largest digit sequence in a block.
   */
  public static DigitSequence nines(int length) {
    return new DigitSequence(length, pow10(length) - 1);
  }

  // The overall length of the digit sequence, including any leading zeros.
  private final int length;
  // The decimal value of the digit sequence (excluding leading zeros, obviously).
  private final long value;
  // Cached toString() representation (toString() of DigitSequence is used in comparisons for
  // sorting to achieve lexicographical ordering, which means it gets churned a lot).
  @LazyInit
  private String toString;

  // Called directly from RangeSpecification.
  DigitSequence(int length, long value) {
    // Don't check for -ve length as this should never happen and will blow up in pow10() anyway.
    Preconditions.checkArgument(length <= MAX_DIGITS,
        "Digit sequence too long [%s digits]", length);
    // This should not happen unless there's a code error, so nice user messages aren't needed.
    Preconditions.checkArgument(value >= 0 && value < pow10(length));
    this.length = length;
    this.value = value;
  }

  /** Returns if this sequence is empty (i.e. length == 0). */
  public boolean isEmpty() {
    return length == 0;
  }

  /** Returns the length of this digit sequence. */
  public int length() {
    return length;
  }

  /**
   * Returns the digit at index {@code n} in this digit sequence, starting from the most
   * significant digit.
   */
  public int getDigit(int n) {
    Preconditions.checkElementIndex(n, length);
    return (int) (value / pow10(((length - 1) - n)) % 10);
  }

  /**
   * Returns the sub-sequence representing only the first {@code n} digits in this sequence. For
   * example, {@code "01234".first(3) == "012"}.
   */
  public DigitSequence first(int n) {
    Preconditions.checkElementIndex(n, length);
    return new DigitSequence(n, value / pow10(length - n));
  }

  /**
   * Returns the sub-sequence representing only the last {@code n} digits in this sequence. For
   * example, {@code "01234".last(3) == "234"}.
   */
  public DigitSequence last(int n) {
    Preconditions.checkElementIndex(n, length);
    return new DigitSequence(n, value % pow10(n));
  }

  /**
   * Returns a new sequence which extends this sequence by a single digit ({@code 0 <= digit <= 9}).
   */
  public DigitSequence extendBy(int digit) {
    Preconditions.checkArgument(0 <= digit && digit <= 9);
    return new DigitSequence(length + 1, (10 * value) + digit);
  }

  /** Returns a new sequence which extends this sequence by the given value. */
  public DigitSequence extendBy(DigitSequence n) {
    Preconditions.checkNotNull(n);
    return new DigitSequence(length + n.length, (pow10(n.length) * value) + n.value);
  }

  /**
   * Returns the digit sequence immediately after this one, or {@code null} if this is the
   * maximum value.
   */
  public DigitSequence next() {
    return domain().next(this);
  }

  /**
   * Returns the digit sequence immediately before this one, or {@code null} if this is the
   * minimum value.
   */
  public DigitSequence previous() {
    return domain().previous(this);
  }

  /** Returns the absolute index of this digit sequence within the integer domain. */
  private long index() {
    return value + DOMAIN_OFFSET[length];
  }

  @Override
  public int compareTo(DigitSequence other) {
    return Long.signum(index() - other.index());
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof DigitSequence) && index() == ((DigitSequence) o).index();
  }

  @Override
  public int hashCode() {
    return Long.hashCode(index());
  }

  @Override
  public String toString() {
    // This little dance is required (according to the docs for the LazyInit annotation) for lazy
    // initialization of non-volatile fields (yes, that's a double init in a single statement).
    String localVar = toString;
    if (localVar == null) {
      toString = localVar = (length > 0 ? String.format("%0" + length + "d", value) : "");
    }
    return localVar;
  }
}
