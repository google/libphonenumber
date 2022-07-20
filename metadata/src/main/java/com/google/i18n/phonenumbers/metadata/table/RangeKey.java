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
package com.google.i18n.phonenumbers.metadata.table;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;
import static java.lang.Integer.numberOfTrailingZeros;
import static java.util.Comparator.comparing;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaEdge;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaVisitor;
import com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer;
import com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer.MergeStrategy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

/**
 * A range key is somewhat similar to a {@link RangeSpecification}, except that it can encode
 * multiple possible lengths for a single range prefix. Range keys are particularly useful as
 * unique "row keys" when representing range trees as tabular data.
 */
@AutoValue
public abstract class RangeKey {
  /**
   * Order by prefix first and then minimum length. For row keys representing disjoint ranges, this
   * will be a total ordering (since the comparison is really with the "shortest" digit sequence in
   * the ranges, which must be distinct for disjoint ranges).
   */
  public static final Comparator<RangeKey> ORDERING =
      comparing(RangeKey::getPrefix, comparing(s -> s.min().toString()))
          .thenComparing(RangeKey::getLengths, comparing(NavigableSet::first));

  /**
   * Creates a range key representing ranges with a prefix of some set of lengths. The prefix must
   * not be longer than the possible lengths and cannot end with an "any" edge (i.e. "x").
   */
  public static RangeKey create(RangeSpecification prefix, Set<Integer> lengths) {
    checkArgument(prefix.length() == 0 || prefix.getBitmask(prefix.length() - 1) != ALL_DIGITS_MASK,
        "prefix cannot end with an 'any' edge: %s", prefix);
    ImmutableSortedSet<Integer> sorted = ImmutableSortedSet.copyOf(lengths);
    checkArgument(sorted.first() >= prefix.length(),
        "lengths cannot be shorter than the prefix: %s - %s", prefix, lengths);
    return new AutoValue_RangeKey(prefix, sorted);
  }

  /**
   * Decomposes the given range tree into a sorted sequence of keys, representing the same digit
   * sequences. The resulting keys form a disjoint covering of the original range set, and no
   * two keys will contain the same prefix (but prefixes of keys may overlap, even if the ranges
   * they ultimately represent do not). The resulting sequence is ordered by {@link #ORDERING}.
   */
  public static ImmutableList<RangeKey> decompose(RangeTree tree) {
    List<RangeKey> keys = new ArrayList<>();
    // The ALLOW_EDGE_SPLITTING strategy works best for the case of generating row keys because it
    // helps avoid having the same sequence appear in multiple rows. Note however than even this
    // strategy isn't perfect, and partially overlapping ranges with different lengths can still
    // cause issues. For example, 851 appears as a prefix for 2 rows in the following (real world)
    // example.
    //   prefix=85[1-9], length=10
    //   prefix=8[57]1,  length=11
    // However a given digit sequence will still only appear in (at most) one range key based on
    // its length.
    for (RangeTree f : RangeTreeFactorizer.factor(tree, MergeStrategy.ALLOW_EDGE_SPLITTING)) {
      KeyVisitor.visit(f, keys);
    }
    return ImmutableList.sortedCopyOf(ORDERING, keys);
  }

  // A recursive descent visitor that splits range keys from the visited tree on the upward phase
  // of visitation. After finding the terminal node, the visitor tries to strip as much of the
  // trailing "any" path as possible, to leave the prefix. Note that the visitor can never start
  // another downward visitation while its processing the "any" paths, because if it walks up
  // through an "any" path, the node it reaches cannot have any other edges coming from it (the
  // "any" path is all the possible edges).
  private static class KeyVisitor implements DfaVisitor {
    private static void visit(RangeTree tree, List<RangeKey> keys) {
      KeyVisitor v = new KeyVisitor(keys);
      tree.accept(v);
      // We may still need to emit a key for ranges with "any" paths that reach the root node.
      int lengthMask = v.lengthMask;
      // Shouldn't happen for phone numbers, since it implies the existence of "zero length" digit
      // sequences.
      if (tree.getInitial().canTerminate()) {
        lengthMask |= 1;
      }
      if (lengthMask != 0) {
        // Use the empty specification as a prefix since the ranges are defined purely by length.
        keys.add(new AutoValue_RangeKey(RangeSpecification.empty(), buildLengths(lengthMask)));
      }
    }

    // Collection of extracted keys.
    private final List<RangeKey> keys;
    // Current path from the root of the tree being visited.
    private RangeSpecification path = RangeSpecification.empty();
    // Non-zero when we are in the "upward" phase of visitation, processing trailing "any" paths.
    // When zero we are either in a "downward" phase or traversing up without stripping paths.
    private int lengthMask = 0;

    private KeyVisitor(List<RangeKey> keys) {
      this.keys = checkNotNull(keys);
    }

    @Override
    public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
      checkState(lengthMask == 0,
          "during downward tree traversal, length mask should be zero (was %s)", lengthMask);
      RangeSpecification oldPath = path;
      path = path.extendByMask(edge.getDigitMask());
      if (target.equals(RangeTree.getTerminal())) {
        lengthMask = (1 << path.length());
        // We might emit the key immediately for ranges without trailing paths (e.g. "1234").
        maybeEmitKey();
      } else {
        target.accept(this);
        // If we see a terminating node, we are either adding a new possible length to an existing
        // key or starting to process a new key (we don't know and it doesn't matter providing we
        // capture the current length in the mask).
        if (target.canTerminate()) {
          lengthMask |= (1 << path.length());
        }
        maybeEmitKey();
      }
      path = oldPath;
    }

    // Conditionally emits a key for the current path prefix and possible lengths if we've found
    // the "end" of an "any" path (e.g. we have possible lengths and the edge above us is not an
    // "any" path).
    private void maybeEmitKey() {
      if (lengthMask != 0 && path.getBitmask(path.length() - 1) != ALL_DIGITS_MASK) {
        keys.add(new AutoValue_RangeKey(path, buildLengths(lengthMask)));
        lengthMask = 0;
      }
    }
  }

  /**
   * Returns the prefix for this range key. All digit sequences matches by this key are of the
   * form {@code "<prefix>xxxx"} for some number of "any" edges. This prefix can be "empty" for
   * ranges such as {@code "xxxx"}.
   */
  public abstract RangeSpecification getPrefix();

  /**
   * Returns the possible lengths for digit sequences matched by this key.  The returned set is
   * never empty.
   */
  public abstract ImmutableSortedSet<Integer> getLengths();

  /**
   * Converts the range key into a sequence of range specifications, ordered by length. The
   * returned set is never empty.
   */
  public final ImmutableList<RangeSpecification> asRangeSpecifications() {
    RangeSpecification s = getPrefix();
    return getLengths().stream()
        .map(n -> s.extendByLength(n - s.length()))
        .collect(toImmutableList());
  }

  public final RangeTree asRangeTree() {
    RangeSpecification s = getPrefix();
    return RangeTree.from(getLengths().stream().map(n -> s.extendByLength(n - s.length())));
  }

  /*
   * Checks if the RangeKey contains a range represented by the given prefix and length.
   */
  public boolean contains(DigitSequence prefix, Integer length) {
    return asRangeSpecifications().stream()
        .anyMatch(
            specification ->
                specification.matches(
                    prefix.extendBy(DigitSequence.zeros(length - prefix.length()))));
  }

  private static ImmutableSortedSet<Integer> buildLengths(int lengthMask) {
    checkArgument(lengthMask != 0);
    ImmutableSortedSet.Builder<Integer> lengths = ImmutableSortedSet.naturalOrder();
    do {
      int length = numberOfTrailingZeros(lengthMask);
      lengths.add(length);
      // Clear each bit as we go.
      lengthMask &= ~(1 << length);
    } while (lengthMask != 0);
    return lengths.build();
  }
}
