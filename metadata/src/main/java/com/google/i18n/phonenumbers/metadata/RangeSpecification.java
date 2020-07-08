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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.i18n.phonenumbers.metadata.DigitSequence.domain;
import static java.lang.Integer.numberOfLeadingZeros;
import static java.lang.Integer.numberOfTrailingZeros;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A compact representation of a disjoint set of ranges of digit sequences. This is a compact way
 * to represent one or many ranges of digit sequences which share the same length. Examples include:
 * <pre>{@code
 * "01234" --> the singleton range containing only the digit sequence "01234"
 * "012xx" --> the contiguous digit sequence range ["01200".."01299"]
 * "012[3-5]6xx" --> the disjoint set of contiguous digit sequence ranges
 *     ["0123600".."0123699"], ["0124600".."0124699"], ["0125600".."0125699"]
 * }</pre>
 * Note that the sets of contiguous ranges defined by a {@code RangeSpecification} are always
 * mutually disjoint.
 *
 * <p>Range specifications have a natural prefix based lexicographical ordering (based on the
 * most-significant point at which a difference appears), but if you are comparing a disjoint set
 * of range specifications (e.g. from a {@link RangeTree}) then it can be more intuitive to use an
 * ordering based on the minimum digit sequence, but note this approach fails if the range
 * specifications can overlap (e.g. comparing "1xx" and "100").
 */
public final class RangeSpecification implements Comparable<RangeSpecification> {
  /** The mask of all possible digits. */
  public static final char ALL_DIGITS_MASK = (1 << 10) - 1;

  private static final RangeSpecification EMPTY = new RangeSpecification("");

  /** Returns the empty range specification, which matches only the empty digit sequence. */
  public static RangeSpecification empty() {
    return EMPTY;
  }

  /** Returns the range specification of length one which matches any of the given digits. */
  public static RangeSpecification singleton(Iterable<Integer> digits) {
    int mask = 0;
    for (int digit : digits) {
      checkArgument(0 <= digit && digit <= 9, "bad digit value '%s'", digit);
      mask |= (1 << digit);
    }
    return new RangeSpecification(String.valueOf((char) mask));
  }

  /** Returns a new range specification which matches only the given non-empty digit sequence. */
  public static RangeSpecification from(DigitSequence s) {
    if (s.length() == 0) {
      return RangeSpecification.empty();
    }
    char[] masks = new char[s.length()];
    for (int n = 0; n < masks.length; n++) {
      masks[n] = (char) (1 << s.getDigit(n));
    }
    return new RangeSpecification(new String(masks));
  }

  /** Returns a new range specification which matches any digit sequence of the specified length. */
  public static RangeSpecification any(int length) {
    checkArgument(length >= 0);
    if (length == 0) {
      return RangeSpecification.empty();
    }
    char[] masks = new char[length];
    Arrays.fill(masks, ALL_DIGITS_MASK);
    return new RangeSpecification(new String(masks));
  }

  /**
   * Parses the string form of a range specification (e.g. "1234[57-9]xxx"). This must be
   * correctly formed, including having all ranges be well formed (e.g. not "[33]", "[3-3]" or
   * "[6-4]").
   *
   * <p>Note that non-canonical ranges are permitted if the digits are in order (e.g. "[1234]",
   * "[4-5]" or "[0-9]" but not "[4321]"). The returned range specification is canonical (e.g.
   * {@code parse("12[34569]").toString() == "12[3-69]"}).
   *
   * <p>The empty string is parsed as the empty range specification.
   *
   * <p>The use of single ASCII underscores ("_") to group ranges and aid readability is supported
   * during parsing but is not retained in the parsed result (e.g.
   * {@code parse("12_34[5-8]_xxx_xxx").toString() == "1234[5-8]xxxxxx"}). Note that underscore may
   * not be present inside ranges (e.g. "1_4") or at the ends of the range (e.g. "123xxx_").
   */
  public static RangeSpecification parse(String s) {
    if (s.isEmpty()) {
      return empty();
    }
    checkArgument(!s.startsWith("_") && !s.endsWith("_"), "cannot start/end with '_': %s", s);
    StringBuilder bitmasks = new StringBuilder();
    boolean lastCharWasUnderscore = false;
    for (int n = 0; n < s.length(); n++) {
      char c = s.charAt(n);
      switch (c) {
        case '_':
          checkArgument(!lastCharWasUnderscore, "cannot have multiple '_' in a row: %s", s);
          lastCharWasUnderscore = true;
          // Continue the for-loop rather than breaking out the switch to avoid resetting the flag.
          continue;
        case 'x':
          bitmasks.append(ALL_DIGITS_MASK);
          break;
        case '[':
          n += 1;
          int end = s.indexOf(']', n);
          checkArgument(end != -1, "unclosed range in specification: %s", s);
          checkArgument(end > n, "empty range in specification: %s", s);
          bitmasks.append(parseRange(s, n, end));
          n = end;
          break;
        default:
          checkArgument('0' <= c && c <= '9',
              "bad digit value '%s' in range specification: %s", c, s);
          bitmasks.append((char) (1 << (c - '0')));
          break;
      }
      lastCharWasUnderscore = false;
    }
    return new RangeSpecification(bitmasks.toString());
  }

  private static char parseRange(String s, int start, int end) {
    int mask = 0;
    for (int n = start; n < end;) {
      char c = s.charAt(n++);
      checkArgument('0' <= c && c <= '9',
          "bad digit value '%s' in range specification: %s", c, s);
      int shift = (c - '0');
      // check that this bit and all above it are zero (to ensure correct ordering).
      checkArgument(mask >> shift == 0, "unordered range in specification: %s", s);
      if (n == end || s.charAt(n) != '-') {
        // Single digit not in a range.
        mask |= 1 << shift;
        continue;
      }
      n++;
      checkArgument(n < end, "unclosed range in specification: %s", s);
      c = s.charAt(n++);
      checkArgument('0' <= c && c <= '9',
          "bad digit value '%s' in range specification: %s", c, s);
      int rshift = (c - '0');
      checkArgument(rshift > shift, "unordered range in specification: %s", s);
      // Set bits from shift to rshift inclusive (e.g. 11111 & ~11 = 11100).
      mask |= ((1 << (rshift + 1)) - 1) & ~((1 << shift) - 1);
    }
    return (char) mask;
  }

  /**
   * Returns the canonical representation of the given ranges. The number of range specifications
   * in the returned instance may be higher or lower than the number of given ranges.
   * <p>
   * NOTE: This is only used by RangeTree for generating a RangeTree from a RangeSet, and is not
   * suitable as a public API (one day we might generate the RangeTree directly and be able to
   * delete this code).
   */
  static ImmutableList<RangeSpecification> from(RangeSet<DigitSequence> ranges) {
    List<RangeSpecification> specs = new ArrayList<>();
    Set<Range<DigitSequence>> s = ranges.asRanges();
    checkArgument(!s.isEmpty(), "empty range set not permitted");
    // Make sure are ranges we use are canonicalized over the domain of DigitSequences (so Range
    // operations (e.g. isConnected()) work as expected. See Range for more on why this matters.
    Range<DigitSequence> cur = s.iterator().next().canonical(domain());
    checkArgument(!cur.contains(DigitSequence.empty()),
        "empty digit sequence not permitted in range set");
    for (Range<DigitSequence> next : Iterables.skip(ranges.asRanges(), 1)) {
      next = next.canonical(domain());
      if (cur.isConnected(next)) {
        // Even though 'cur' and 'next' are both canonicalized, it's not guaranteed that they are
        // closed-open (singleton ranges are fully closed and any range containing the maximum
        // value must be closed. To "union" the two ranges we must also preserve the bound types.
        cur = Range.range(
            cur.lowerEndpoint(), cur.lowerBoundType(),
            next.upperEndpoint(), next.upperBoundType())
            .canonical(domain());
        continue;
      }
      addRangeSpecsOf(cur, specs);
      cur = next;
    }
    addRangeSpecsOf(cur, specs);
    return ImmutableList.sortedCopyOf(Comparator.comparing(RangeSpecification::min), specs);
  }

  /** Adds the canonical minimal range specifications for a single range to the given list. */
  private static void addRangeSpecsOf(Range<DigitSequence> r, List<RangeSpecification> specs) {
    // Given range is already canonical but may span multiple lengths. It's easier to view this
    // as a contiguous set when finding first/last elements however to avoid worrying about bound
    // types. A contiguous set is not an expensive class to create.
    ContiguousSet<DigitSequence> s = ContiguousSet.create(r, domain());
    DigitSequence start = s.first();
    DigitSequence end = s.last();
    while (start.length() < end.length()) {
      // Add <start> to "999..." for the current block length (the max domain value is all 9's).
      DigitSequence blockEnd = DigitSequence.nines(start.length());
      addRangeSpecs(start, blockEnd, specs);
      // Reset the start to the next length up (i.e. the "000..." sequence that's one longer).
      start = blockEnd.next();
    }
    // Finally and the range specs up to (and including) the end value.
    addRangeSpecs(start, end, specs);
  }

  // Adds canonical minimal range specifications for the range of same-length digit sequences.
  private static void addRangeSpecs(
      DigitSequence start, DigitSequence end, List<RangeSpecification> specs) {
    int length = start.length();
    checkArgument(end.length() == length);

    // Masks contains a running total of the bitmasks we want to convert to RangeSpecifications.
    // As processing proceeds, the mask array is reused. This is because the prefix used for
    // successive range specifications is always a subset of the previous specifications and the
    // trailing part of the array always fills up with the range mask for 'x' (i.e. [0-9]).
    int[] masks = new int[length];

    // Stage 1:
    // Starting from the last digit in the 'start' sequence, work up until we find something that
    // is not a '0'. This is the first digit that needs to be adjusted to create a range
    // specification covering it and the digits 'below' it. For example, the first specification
    // for the range ["1200".."9999"] is "1[2-9]xx".
    // Once a specification is emitted, the start value is adjusted to the next digit sequence
    // immediately above the end of the emitted range, so after emitting "1[2-9]xx", start="2000".
    // Once each range specification is emitted, we continue working 'up' the digit sequence until
    // the next calculated start value exceeds the 'end' of our range. This specification cannot
    // be emitted and signals the end of stage 1.
    setBitmasks(masks, start);
    for (int n = previousNon(0, start, length); n != -1; n = previousNon(0, start, n)) {
      int loDigit = start.getDigit(n);
      DigitSequence prefix = start.first(n);
      DigitSequence blockEnd = prefix.extendBy(DigitSequence.nines(length - n));
      if (blockEnd.compareTo(end) > 0) {
        // The end of this block would exceed the end of the main range, so we must stop.
        break;
      }
      // The bitmasks we want is:
      // <first (n-1) digits of 'start'> [loDigit..9] <any digits mask...>
      masks[n] = bitmaskUpFrom(loDigit);
      fillBitmasksAfter(masks, n);
      specs.add(RangeSpecification.fromBitmasks(masks));
      // Adjust the range start now we have emitted the range specification.
      start = blockEnd.next();
    }

    // Stage 2:
    // Very similar to stage 1, but work up from the last digit in the 'end' sequence. The
    // difference now is that we look for the first digit that's not '9' and generate ranges that
    // go down to the start of the range, not up to the end. Thus for ["0000", "1299"] the first
    // specification generated is "1[0-2]xx", which is emitted at the end of the list.
    int midIdx = specs.size();
    setBitmasks(masks, end);
    for (int n = previousNon(9, end, length); n != -1; n = previousNon(9, end, n)) {
      int hiDigit = end.getDigit(n);
      DigitSequence prefix = end.first(n);
      DigitSequence blockStart = prefix.extendBy(DigitSequence.zeros(length - n));
      if (blockStart.compareTo(start) < 0) {
        // The start of this block would precede the start of the main range, so we must stop.
        break;
      }
      // The bitmasks we want is:
      // <first (n-1) digits of 'end'> [0..hiDigit] <any digits mask...>
      masks[n] = bitmaskDownFrom(hiDigit);
      fillBitmasksAfter(masks, n);
      specs.add(midIdx, RangeSpecification.fromBitmasks(masks));
      // Adjust the range end now we have emitted the range specification.
      end = blockStart.previous();
    }

    // Stage 3: Having emitted the first and last set of range specifications, it only remains to
    // emit the "center" specification in the middle of the list. This is special as neither bound
    // is the end of a block. In previous stages, all partial ranges are either "up to 9" or
    // "down to zero". For example: ["1234".."1789"] has the center range "1[3-6]xx", and
    // ["1234".."1345"] has no center range at all.
    if (start.compareTo(end) < 0) {
      // Find the last digit before start and end combine (ie, 1200, 1299 --> 12xx --> n=1). We
      // know that 'start' and 'end' are the same length and bound a range like:
      //   <prefix> [X..Y] [000..999]
      // but X or Y could be 0 or 9 respectively (just not both).
      //
      // Note that we don't even both to test the first digit in the sequences because if 'start'
      // and 'end' span a full range (e.g. [000.999]) we can just use the same code to fill the
      // masks correctly anyway.
      int n = start.length();
      while (--n > 0 && start.getDigit(n) == 0 && end.getDigit(n) == 9) {}
      // Bitwise AND the masks for [X..9] and [0..Y] to get the mask for [X..Y].
      // Note that the "masks" array already contains the correct prefix digits up to (n-1).
      masks[n] = bitmaskUpFrom(start.getDigit(n)) & bitmaskDownFrom(end.getDigit(n));
      fillBitmasksAfter(masks, n);
      specs.add(midIdx, RangeSpecification.fromBitmasks(masks));
    }
  }

  // Sets the values in the given array to correspond to the digits in the given sequence. If a
  // range specification were made from the resulting array it would match only that digit sequence.
  private static void setBitmasks(int[] masks, DigitSequence s) {
    for (int n = 0; n < s.length(); n++) {
      masks[n] = 1 << s.getDigit(n);
    }
  }

  /**
   * Creates a range specification from a given array of integer masks. The Nth element of the
   * array corresponds to the Nth element in the range specification, and mask values must be
   * non-zero and have only bits 0 to 9 set.
   */
  private static RangeSpecification fromBitmasks(int[] bitmasks) {
    checkArgument(bitmasks.length <= DigitSequence.MAX_DIGITS,
        "range specification too large");
    StringBuilder s = new StringBuilder(bitmasks.length);
    s.setLength(bitmasks.length);
    for (int n = 0; n < bitmasks.length; n++) {
      int mask = bitmasks[n];
      checkArgument(mask > 0 && mask <= ALL_DIGITS_MASK, "invalid bitmask: %s", mask);
      s.setCharAt(n, (char) mask);
    }
    return new RangeSpecification(s.toString());
  }

  // Fills the bitmasks after the given index with the "all digits" mask (i.e. matching [0-9]).
  // This can accept -1 as the index since it always pre-increments before using it.
  private static void fillBitmasksAfter(int[] masks, int n) {
    // Because of the iterative way the mask array is handled, we can stop filling when we hit
    // ALL_DIGITS_MASK because everything past that must already be filled.
    while (++n < masks.length && masks[n] != ALL_DIGITS_MASK) {
      masks[n] = ALL_DIGITS_MASK;
    }
  }

  // Starting at digit-N, returns the index of the nearest preceding digit that's not equal to the
  // given value (or -1 if no such digit exists).
  private static int previousNon(int digit, DigitSequence s, int n) {
    while (--n >= 0 && s.getDigit(n) == digit) {}
    return n;
  }

  /** Returns the bitmask for the range {@code [n-9]}. */
  private static int bitmaskUpFrom(int n) {
    return (-1 << n) & ALL_DIGITS_MASK;
  }

  /** Returns the bitmask for the range {@code [0-n]}. */
  private static int bitmaskDownFrom(int n) {
    return ALL_DIGITS_MASK >>> (9 - n);
  }


  // String containing one bitmasks per character (bits 0..9).
  private final String bitmasks;
  // Minimum and maximum sequences (inclusive) which span the ranges defined by this specification.
  // Caching this is deliberate, since we sort disjoint ranges using the minimum value. It might
  // not be so useful to cache the maximum value though.
  private final DigitSequence min;
  private final DigitSequence max;
  // Total number of sequences matched by this specification.
  private final long sequenceCount;

  private RangeSpecification(String bitmasks) {
    int length = bitmasks.length();
    checkArgument(length <= DigitSequence.MAX_DIGITS,
        "Range specification too long (%s digits)", length);
    this.bitmasks = bitmasks;
    long minValue = 0;
    long maxValue = 0;
    long sequenceCount = 1;
    for (int n = 0; n < length; n++) {
      int mask = bitmasks.charAt(n);
      checkArgument(mask > 0 && mask <= ALL_DIGITS_MASK, "invalid bitmask: %s", mask);
      minValue = (minValue * 10) + numberOfTrailingZeros(mask);
      maxValue = (maxValue * 10) + (31 - numberOfLeadingZeros(mask));
      sequenceCount *= Integer.bitCount(mask);
    }
    this.min = new DigitSequence(length, minValue);
    this.max = new DigitSequence(length, maxValue);
    this.sequenceCount = sequenceCount;
  }

  /**
   * Returns the number of digits that this specification can match. This is the length of all
   * digit sequences which can match this specification.
   */
  public int length() {
    return bitmasks.length();
  }

  /** Returns the smallest digit sequence matched by this range. */
  public DigitSequence min() {
    return min;
  }

  /** Returns the largest digit sequence matched by this range. */
  public DigitSequence max() {
    return max;
  }

  /** Returns the total number of digit sequences matched by (contained in) this specification. */
  public long getSequenceCount() {
    return sequenceCount;
  }

  /**
   * Returns the bitmask of the Nth range in this specification. Bit-X (0 ≤ X ≤ 9) corresponds to
   * the digit with value X. As every range in a specification must match at least one digit, this
   * mask can never be zero.
   */
  public int getBitmask(int n) {
    return bitmasks.charAt(n);
  }

  /**
   * Returns whether the given digit sequence is in one of the ranges specified by this instance.
   * This is more efficient that obtaining the associated {@code RangeSet} and checking that.
   */
  public boolean matches(DigitSequence digits) {
    if (digits.length() != length()) {
      return false;
    }
    for (int n = 0; n < length(); n++) {
      if ((bitmasks.charAt(n) & (1 << digits.getDigit(n))) == 0) {
        return false;
      }
    }
    return true;
  }

  // Returns the next sequence in forward order which is contained by a range defined by this
  // range specification, or null if none exists. The given sequence must not be matched by this
  // specification.
  private DigitSequence nextRangeStart(DigitSequence s) {
    // Easy length based checks (this is where the fact that range specification only define ranges
    // of the same length really simplifies things).
    if (s.length() < length()) {
      return min();
    } else if (s.length() > length()) {
      return null;
    }
    // Algorithm:
    // 1) Find the highest digit that isn't in the corresponding bitmask for the range.
    // 2) Try and increase the digit value until it's inside the next available range.
    // 3) If that fails, move back up the sequence and increment the next digit up.
    // 4) Repeat until a digit can be adjusted to start a new range, or all digits are exhausted.
    // If all digits exhausted, the sequence was above all ranges in this specification.
    // Otherwise return a new sequence using the unchanged prefix of the original sequence, the
    // newly adjusted digit and the trailing digits of the minimal sequence.
    for (int n = 0; n < length(); n++) {
      int d = s.getDigit(n);
      int mask = bitmasks.charAt(n);
      if ((mask & (1 << d)) != 0) {
        continue;
      }
      while (true) {
        // Digit 'd' is either outside the range mask (first time though the loop) or inside a
        // range. Either way we want to find the next digit above it which is inside a range.
        // First increment 'd', and then find the next set bit in the mask at or above that point.
        // Not extra check is needed at the end of ranges because numberOfTrailingZeros(0)==32
        // which neatly ensures that the new value of 'd' must be out-of-range.
        // If mask=[3-58]: d=1-->d'=3, d=4-->d'=5, d=5-->d'=8, d=8-->d'>9
        d++;
        d += numberOfTrailingZeros(mask >>> d);
        if (d <= 9) {
          // Found the value of the largest digit which can be adjusted to start the next range.
          // Everything higher than this digit is the same as the original sequence and everything
          // lower that this digit is the same as the corresponding digit in the minimal value.
          return s.first(n).extendBy(d).extendBy(min.last((length() - n) - 1));
        }
        // No more bits available in this range, so go back up to the previous range.
        if (--n < 0) {
          // The sequence was above the last element in the set.
          // Example: Range Spec: 1[2-8][3-8]456, Sequence: 188457
          return null;
        }
        d = s.getDigit(n);
        mask = bitmasks.charAt(n);
      }
    }
    // If we finish the outer loop the given sequence was in a range (which is an error).
    throw new IllegalArgumentException(
        "Digit sequence '" + s + "' is in the range specified by: " + this);
  }

  // Given a sequence inside a range defined by this specification, return the highest sequence
  // in the current range (possibly just the given sequence).
  private DigitSequence currentRangeEnd(DigitSequence s) {
    // Build up a value representing the trailing digits (which must always be 9's).
    long nines = 0;
    for (int n = length() - 1; n >= 0; n--, nines = (10 * nines) + 9) {
      int mask = bitmasks.charAt(n);
      if (mask == ALL_DIGITS_MASK) {
        continue;
      }
      // The new digit is the top of the current range that the current sequence digit is in.
      int d = nextUnsetBit(mask, s.getDigit(n)) - 1;
      DigitSequence end =
          s.first(n).extendBy(d).extendBy(new DigitSequence((length() - n) - 1, nines));
      // Edge case for cases like "12[34][09]x" where "1239x" and "1240x" abut. This adjustment
      // will happen at most once because the second range cannot also include an upper bound
      // ending at '9', since otherwise (mask == ALL_DIGITS_MASK) at this position. The next
      // sequence must be terminated with zeros starting at the current position having "rolled
      // over" on the digit above.
      if (d == 9) {
        DigitSequence next = end.next();
        if (matches(next)) {
          d = nextUnsetBit(mask, 0) - 1;
          end = next.first(n).extendBy(d).extendBy(new DigitSequence((length() - n) - 1, nines));
        }
      }
      return end;
    }
    // The range specification is entirely 'x', which means it's a single range.
    return max;
  }

  /**
   * Returns a generating iterator which iterates in forward order over the disjoint ranges defined
   * by this specification. This is not actually as useful as you might expect because in a lot of
   * cases you would be dealing with a sequence of range specifications and it's not true that all
   * ranges from multiple specifications are disjoint.
   */
  Iterable<Range<DigitSequence>> asRanges() {
    return () -> new Iterator<Range<DigitSequence>>() {
      // Start is always in a range.
      private DigitSequence start = min;

      @Override
      public boolean hasNext() {
        return start != null;
      }

      @Override
      public Range<DigitSequence> next() {
        DigitSequence end = currentRangeEnd(start);
        Range<DigitSequence> r = Range.closed(start, end).canonical(DigitSequence.domain());
        start = nextRangeStart(end.next());
        return r;
      }
    };
  }

  /**
   * Returns a new range specification which is extended by the given mask value. For example:
   * <pre>{@code
   * "0123[4-6]".extendByMask(7) == "0123[4-6][0-2]"
   * }</pre>
   */
  public RangeSpecification extendByMask(int mask) {
    checkArgument(mask > 0 && mask <= ALL_DIGITS_MASK, "bad mask value '%s'", mask);
    return new RangeSpecification(bitmasks + ((char) mask));
  }

  /**
   * Returns a new range specification which is extended by the given specification. For example:
   * <pre>{@code
   * "0123[4-6]".extendBy("7[89]") == "0123[4-6]7[89]"
   * }</pre>
   */
  public RangeSpecification extendBy(RangeSpecification extra) {
    return new RangeSpecification(bitmasks + extra.bitmasks);
  }

  /**
   * Returns a new range specification which is extended by a sequence of any digits of the given
   * length. For example:
   * <pre>{@code
   * "012".extendByLength(4) == "012xxxx"
   * }</pre>
   */
  public RangeSpecification extendByLength(int length) {
    return this.extendBy(any(length));
  }

  /**
   * Returns a range specification containing only the first {@code n} digits. If the given length
   * is the same or greater than the specification's length, this specification is returned.
   * For example:
   * <pre>{@code
   * "01[2-4]xx".first(8) == "01[2-4]xx" (same instance)
   * "01[2-4]xx".first(5) == "01[2-4]xx" (same instance)
   * "01[2-4]xx".first(3) == "01[2-4]"
   * "01[2-4]xx".first(0) == "" (the empty specification)
   * }</pre>
   */
  public RangeSpecification first(int n) {
    checkArgument(n >= 0);
    if (n == 0) {
      return empty();
    }
    return n < length() ? new RangeSpecification(bitmasks.substring(0, n)) : this;
  }

  /**
   * Returns a range specification containing only the last {@code n} digits. If the given length
   * is the same or greater than the specification's length, this specification is returned.
   * For example:
   * <pre>{@code
   * "01[2-4]xx".last(8) == "01[2-4]xx" (same instance)
   * "01[2-4]xx".last(5) == "01[2-4]xx" (same instance)
   * "01[2-4]xx".last(3) == "[2-4]xx"
   * "01[2-4]xx".last(0) == "" (the empty specification)
   * }</pre>
   */
  public RangeSpecification last(int n) {
    checkArgument(n >= 0);
    if (n == 0) {
      return empty();
    }
    return n < length() ? new RangeSpecification(bitmasks.substring(length() - n)) : this;
  }

  /**
   * Returns a range specification with any trailing "any digit" sequence removed. For example:
   * <pre>{@code
   * "0123".getPrefix() == "0123" (same instance)
   * "0123xx".getPrefix() == "0123"
   * "xxx".getPrefix() == "" (the empty specification)
   * }</pre>
   */
  public RangeSpecification getPrefix() {
    int length = length();
    while (length > 0 && getBitmask(length - 1) == ALL_DIGITS_MASK) {
      length--;
    }
    return first(length);
  }

  @Override
  public int compareTo(RangeSpecification other) {
    int length = Math.min(length(), other.length());
    for (int i = 0; i < length; i++) {
      int mask = getBitmask(i);
      int otherMask = other.getBitmask(i);
      if (mask == otherMask) {
        continue;
      }
      int commonBits = mask & otherMask;
      mask -= commonBits;
      otherMask -= commonBits;
      // At least one mask is still non-zero and they don't overlap.
      //
      // The mask with the lowest set bit is the smaller mask in the ordering, since that bit
      // distinguishes a smaller prefix than can never exist in the other specification.
      // Testing the number of trailing zeros is equivalent to finding the lowest set bit.
      return Integer.compare(numberOfTrailingZeros(mask), numberOfTrailingZeros(otherMask));
    }
    return Integer.compare(length(), other.length());
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof RangeSpecification) && bitmasks.equals(((RangeSpecification) o).bitmasks);
  }

  @Override
  public int hashCode() {
    return bitmasks.hashCode();
  }

  /**
   * If you want lexicographical ordering of range specifications, don't use this method, use the
   * {@code min().toString()}. This works assuming the ranges being compared are disjoint.
   */
  @Override
  public String toString() {
    // Consider caching if it turns out that we are serializing a lot of these.
    StringBuilder s = new StringBuilder();
    for (int n = 0; n < bitmasks.length(); n++) {
      appendMask(bitmasks.charAt(n), s);
    }
    return s.toString();
  }

  /** Returns the string representation of a single bit-mask. */
  public static String toString(int bitMask) {
    checkArgument(bitMask > 0 && bitMask < (1 << 10), "bad mask value: %s", bitMask);
    return appendMask(bitMask, new StringBuilder()).toString();
  }

  static StringBuilder appendMask(int mask, StringBuilder out) {
    if (mask == ALL_DIGITS_MASK) {
      out.append('x');
    } else if (hasOneBit(mask)) {
      out.append(asChar(numberOfTrailingZeros(mask)));
    } else {
      out.append('[');
      for (int loBit = numberOfTrailingZeros(mask);
          loBit != 32;
          loBit = numberOfTrailingZeros(mask)) {
        // Always append the loBit digit into the range.
        out.append(asChar(loBit));
        int hiBit = nextUnsetBit(mask, loBit);
        int numBits = hiBit - loBit;
        if (numBits > 1) {
          // Stylistically prefer "[34]" to "[3-4]" for compactness.
          if (numBits > 2) {
            out.append('-');
          }
          out.append(asChar(hiBit - 1));
        }
        // Clear the bits we've just processed before going back round the loop.
        mask &= ~((1 << hiBit) - 1);
      }
      out.append(']');
    }
    return out;
  }

  // Turns a value in the range [0-9] into the corresponding ASCII character.
  private static char asChar(int digit) {
    return (char) ('0' + digit);
  }

  // Determines if the given bit-mask has only one bit set.
  private static boolean hasOneBit(int mask) {
    return (mask & (mask - 1)) == 0;
  }

  private static int nextUnsetBit(int mask, int bit) {
    // Example mask transform for [013-589] if bit=3:
    //        v-- bit=3
    // 01100111011
    // 00000000111  (1 << 3) - 1
    // 01100111111  OR with mask
    // 10011000000  Bitwise NOT
    //     ^-- return=6
    return numberOfTrailingZeros(~(mask | ((1 << bit) - 1)));
  }
}
