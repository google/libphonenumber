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

package com.google.i18n.phonenumbers.metadata.regex;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * Represents an NFA graph which accepts sequences of inputs of any digit (also known as "any-digit
 * sequences"), possibly of variable length. For example, an {@code AnyPath} instance might accept
 * a single input of any digit (i.e. equivalent to the regular expression {@code "\d"}), or it might
 * accept sequences of any digits of length 4 or 6 (i.e. equivalent to the regular expression
 * {@code "\d{4}\d{2}?"}.
 *
 * <p>As {@code AnyPath} instances are all restricted to only accepting any-digits sequences, the
 * only interesting thing about them is the set of sequence lengths they accept.
 */
@AutoValue
abstract class AnyPath implements Comparable<AnyPath> {
  /**
   * The special empty path which matches zero length input. This is useful as an identity value
   * when constructing other paths but should never be a path in the graph.
   */
  public static final AnyPath EMPTY = new AutoValue_AnyPath(0x1);

  /** The path matching exactly one input of any digit. */
  public static final AnyPath SINGLE = of(0x2);

  /** The path matching one or zero inputs of any digit. */
  public static final AnyPath OPTIONAL = of(0x3);

  @VisibleForTesting
  static AnyPath of(int mask) {
    Preconditions.checkArgument(mask > 1, "invalid path mask: %s", mask);
    return new AutoValue_AnyPath(mask);
  }

  /**
   * Returns a bit-mask representing the lengths of any-digit sequences accepted by this path.
   * If bit-N is set, then this path accepts an N-length sequence of any digits.
   */
  abstract int mask();

  /** Returns whether this path accepts an any-digit sequence of length {@code n}.*/
  public boolean acceptsLength(int n) {
    Preconditions.checkArgument(n >= 0 && n < 32, "invalid path length: %s", n);
    return (mask() & (1 << n)) != 0;
  }

  /** Returns the maximum length any-sequence that this path will accept. */
  public int maxLength() {
    return (31 - Integer.numberOfLeadingZeros(mask()));
  }

  /**
   * Returns whether this path is empty (i.e. accepts only zero length sequences). This is only
   * useful when constructing paths and empty paths should never appear in an NFA graph.
   */
  public boolean isEmpty() {
    return mask() == 0x1;
  }

  /**
   * Extends this path by one input, potentially setting all input as optional. For example (using
   * 'x' to represent a single "any digit" input):
   * <ul>
   *   <li>{@code "xx".extend(false) == "xxx"}
   *   <li>{@code "xx".extend(true) == "(xxx)?"}
   *   <li>{@code "xx(x)?".extend(false) == "xxx(x)?"}
   *   <li>{@code "xx(x)?".extend(true) == "(xxx(x)?)?"}
   * </ul>
   */
  public AnyPath extend(boolean allOptional) {
    return of((mask() << 1) | (allOptional ? 0x1 : 0x0));
  }

  /**
   * Joins the given path to this one, results in a new path which is equivalent to the
   * concatenation of the regular expressions they represent. For example (using
   * 'x' to represent a single "any digit" input):
   * <ul>
   *   <li>{@code "xx".join("xx") == "xxxx"}
   *   <li>{@code "xx".join("x?") == "xx(x)?"}
   * </ul>
   */
  public AnyPath join(AnyPath other) {
    int newMask = 0;
    // Include the length itself (which is always accepted).
    for (int n = 0; n <= other.maxLength(); n++) {
      if (other.acceptsLength(n)) {
        newMask |= mask() << n;
      }
    }
    return of(newMask);
  }

  /**
   * Returns a new path which is equal to this path, except that it also accepts zero length
   * sequences.
   */
  public AnyPath makeOptional() {
    return of(mask() | 0x1);
  }

  /**
   * Attempts to "factor" this path by the given path to produce a path such that
   * {@code p.factor(q).join(q)} is equivalent to {@code p}. This is useful when trying to
   * determine longest common paths. Factorizing may not succeed in cases where no common path
   * exists (e.g. {@code "xx(xx)?".factor("x?")} fails because there is no way to join anything
   * to the path {@code "x?"} to make it accept exactly 2 or 4 length any-digit sequences).
   */
  public Optional<AnyPath> factor(AnyPath other) {
    int factor = mask() / other.mask();
    if (factor > 1 && (other.mask() * factor) == mask()) {
      return Optional.of(of(factor));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public int compareTo(AnyPath other) {
    return Integer.compare(mask(), other.mask());
  }

  @Override
  public final String toString() {
    // A non-obvious algorithm for getting a reasonable toString() using x's.
    // Best understood via examples:
    //
    // 0001 is invalid as we cannot represent an optional zero-length sequence.
    //
    // Hi-bit-1 ==> 1 x
    // 0010 -> x, 0011 -> (x)?
    //
    // Hi-bit-2 ==> 2 x's
    // 0100 -> xx, 0101 -> (xx)?, 0110 -> x(x)?, 0111 -> (x(x)?)?
    //
    // Hi-bit-3 ==> 3 x's
    // 1000 -> xxx,    1001 -> (xxx)?,    1010 -> x(xx)?,    1011 -> (x(xx)?)?
    // 1100 -> xx(x)?, 1101 -> (xx(x)?)?, 1110 -> x(x(x)?)?, 1111 -> (x(x(x)?)?)?
    //
    // Rules:
    // * For hi-bit M, there are M x's in the string.
    // * For N < M; if bit-N is set, then a group starts after the Nth-x.
    if (mask() == 0x1) {
      return "<EMPTY>";
    }
    StringBuilder out = new StringBuilder();
    for (int n = 0; n < maxLength(); n++) {
      out.append('x');
    }
    // Loop high-to-low to prevent earlier insertions messing with the index.
    for (int n = maxLength() - 1; n >= 0; n--) {
      if (acceptsLength(n)) {
        out.insert(n, '(');
      }
    }
    // The number of opened groups was the number of set bits - 1.
    for (int n = Integer.bitCount(mask()) - 1; n > 0; n--) {
      out.append(")?");
    }
    return out.toString();
  }
}
