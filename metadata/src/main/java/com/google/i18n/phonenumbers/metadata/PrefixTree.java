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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.i18n.phonenumbers.metadata.RangeTree.DfaEdge;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaVisitor;
import com.google.i18n.phonenumbers.metadata.RangeTree.SetOperations;
import java.util.ArrayList;
import java.util.List;

/**
 * A variation of a {@link RangeTree} which represents a set of prefixes (as opposed to a set of
 * ranges). While this implementation is backed by a {@code RangeTree} and has a similar serialized
 * representation, it is a deliberately distinct type and should not be thought of as a subset of
 * {@code RangeTree}. In particular, set operations are defined to work differently for
 * {@code PrefixTree} due to its differing semantics and some set operations (e.g. subtraction) are
 * not even well defined.
 */
public final class PrefixTree {
  private static final PrefixTree EMPTY = new PrefixTree(RangeTree.empty());

  /** Returns the "empty" prefix tree, which matches no ranges. */
  public static PrefixTree empty() {
    return EMPTY;
  }

  /**
   * Returns a prefix tree with the paths of the given ranges, trimmed to the earliest point of
   * termination. For example, the ranges {@code {"1[0-3]", "1234", "56x"}} will result in the
   * prefixes {@code {"1[0-3]", "56x"}}, since {@code "1[0-3]"} contains {@code "12"}, which is a
   * prefix of {@code "1234"}.
   */
  public static PrefixTree from(RangeTree ranges) {
    return !ranges.isEmpty()
        ? new PrefixTree(removeTrailingAnyDigitPaths(TrimmingVisitor.trim(ranges)))
        : empty();
  }

  /**
    * Returns a prefix tree containing all digit sequences in the given range specification. A
    * single range specification cannot overlap in the way that general range trees can, so unlike
    * {@link #from(RangeTree)}, this method will never throw {@code IllegalArgumentException}.
    */
  public static PrefixTree from(RangeSpecification spec) {
    // Range specifications define ranges of a single length, so must always be a valid prefix.
    return from(RangeTree.from(spec));
  }

  /**
   * Returns the minimal prefix tree which includes all the paths in "include", and none of the
   * paths in "exclude". For example:
   * <pre> {@code
   *   minimal({ "123x", "456x" }, { "13xx", "459x" }, 0) == { "12", "456" }
   *   minimal({ "123x", "456x" }, {}, 0) == { "" }
   *   minimal({ "123x", "456x" }, {}, 1) == { "[14]" }
   * }</pre>
   *
   * <p>A minimal length can be specified to avoid creating prefixes that are "too short" for some
   * circumstances.
   *
   * <p>Caveat: In cases where the {@code include} and {@code exclude} ranges overlap, the shortest
   * possible prefix is chosen. For example:
   * <pre> {@code
   *   minimal({ "12", "1234", "56" }, { "123", "5678" }) == { "12", "56" }
   * }</pre>
   * This means that it may not always be true that {@code minimal(A, B).intersect(minimal(B, A))}
   * is empty.
   */
  public static PrefixTree minimal(RangeTree include, RangeTree exclude, int minLength) {
    checkArgument(include.intersect(exclude).isEmpty(), "ranges must be disjoint");
    checkArgument(minLength >= 0, "invalid minimum prefix length: %s", minLength);
    PrefixTree prefix = PrefixTree.from(include);
    if (prefix.isEmpty()) {
      // This matches no input, not all input.
      return prefix;
    }
    // Ignore anything that the prefix already captures, since there's no point avoiding shortening
    // the prefix to avoid what's already overlapping.
    exclude = exclude.subtract(prefix.retainFrom(exclude));

    // This can contain only the empty sequence (i.e. match all input) if the original include set
    // was something like "xxxxx". In that case the initial node is just the terminal.
    RangeTree minimal;
    DfaNode root = prefix.asRangeTree().getInitial();
    if (prefix.isIdentity() || exclude.isEmpty()) {
      // Either we already accept anything, or there is nothing to exclude.
      minimal = emit(root, RangeSpecification.empty(), RangeTree.empty(), minLength);
    } else {
      minimal = recursivelyMinimize(
          root, RangeSpecification.empty(), exclude.getInitial(), RangeTree.empty(), minLength);
    }
    // No need to go via the static factory here, since that does a bunch of work we know cannot
    // be necessary. The range tree here is a subset of an already valid prefix tree, so cannot
    // contain "early terminating nodes" or "trailing any digit sequences".
    return new PrefixTree(minimal);
  }

  private final RangeTree ranges;

  private PrefixTree(RangeTree ranges) {
    // Caller is responsible for ensuring that the ranges conform to expectations of a prefix tree.
    this.ranges = ranges;
  }

  /**
   * Returns a {@link RangeTree} containing the same digit sequences as this prefix tree. Prefix
   * trees and range trees do not have the same semantics, but they do have the same serialized
   * form (i.e. to serialize a prefix tree, you can just serialize the corresponding range tree).
   */
  public RangeTree asRangeTree() {
    return ranges;
  }

  /**
   * Returns whether this prefix tree is empty. Filtering a {@link RangeTree} by the empty prefix
   * tree always returns the empty range tree. The result of filtering a range tree is defined as
   * containing only digit sequences which are prefixed by some digit sequence in the prefix tree.
   * If the prefix tree is empty, no digit sequence can ever satisfy that requirement.
   */
  public boolean isEmpty() {
    return ranges.isEmpty();
  }

  /**
   * Returns whether this prefix tree matches any digit sequence. Filtering a {@link RangeTree} by
   * the identity prefix returns the original range tree. The result of filtering a range tree is
   * defined as containing only digit sequences which are prefixed by some digit sequence in the
   * prefix tree. The identity prefix tree contains the empty digit sequence, which is a prefix of
   * every digit sequence.
   */
  public boolean isIdentity() {
    return !ranges.isEmpty() && ranges.getInitial().equals(RangeTree.getTerminal());
  }

  /** Returns whether the given sequence would be retained by this prefix tree. */
  public boolean prefixes(DigitSequence digits) {
    DfaNode node = ranges.getInitial();
    for (int n = 0; n < digits.length(); n++) {
      DfaEdge e = node.find(digits.getDigit(n));
      if (e == null) {
        break;
      }
      node = e.getTarget();
    }
    return node.equals(RangeTree.getTerminal());
  }

  /**
   * Returns a subset of the given ranges, containing only ranges which are prefixed by an
   * element in this prefix tree. For example:
   * <pre> {@code
   *   RangeTree r = { "12xx", "1234x" }
   *   PrefixTree p = { "12[0-5]" }
   *   p.retainFrom(r) = { "12[0-5]x", "1234x"}
   * }</pre>
   * Note that if the prefix tree is empty, this method returns the empty range tree.
   */
  public RangeTree retainFrom(RangeTree ranges) {
    return SetOperations.INSTANCE.retainFrom(this, ranges);
  }

  /**
   * Returns the union of two prefix trees. For prefix trees {@code p1}, {@code p2} and any range
   * tree {@code R}, the union {@code P = p1.union(p2)} is defined such that:
   * <pre> {@code
   *   P.retainFrom(R) = p1.retainFrom(R).union(p2.retainFrom(R))
   * }</pre>
   * If prefixes are the same length this is equivalent to {@link RangeTree#union(RangeTree)},
   * but when prefixes overlap, only the more general (shorter) prefix is retained.
   */
  public PrefixTree union(PrefixTree other) {
    return SetOperations.INSTANCE.union(this, other);
  }

  /**
   * Returns the intersection of two prefix trees. For prefix trees {@code p1}, {@code p2} and any
   * range tree {@code R}, the intersection {@code P = p1.intersect(p2)} is defined such that:
   * <pre> {@code
   *   P.retainFrom(R) = p1.retainFrom(R).intersect(p2.retainFrom(R))
   * }</pre>
   * If prefixes are the same length this is equivalent to {@link RangeTree#intersect(RangeTree)},
   * but when prefixes overlap, only the more specific (longer) prefix is retained.
   */
  public PrefixTree intersect(PrefixTree other) {
    return SetOperations.INSTANCE.intersect(this, other);
  }

  /**
   * Returns a prefix tree trimmed to at most {@code maxLength} digits. The returned value may be
   * shorter if, in the process of trimming, trailing edges are collapsed to "any digit" sequences.
   * For example:
   * <pre> {@code
   * { "12[0-4]5", "12[5-9]" }.trim(3) == "12"
   * { "7001", "70[1-9]", "7[1-9]" }.trim(3) == "7"
   * }</pre>
   */
  public PrefixTree trim(int maxLength) {
    return PrefixTree.from(
        RangeTree.from(
            ranges.asRangeSpecifications().stream()
                .map(s -> s.first(maxLength))
                .collect(toImmutableList())));
  }

  @Override
  public int hashCode() {
    return ranges.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof PrefixTree) && ranges.equals(((PrefixTree) o).ranges);
  }

  @Override
  public String toString() {
    return ranges.toString();
  }

  private static final class TrimmingVisitor implements DfaVisitor {
    static RangeTree trim(RangeTree ranges) {
      if (ranges.isEmpty()) {
        return ranges;
      }
      if (ranges.getInitial().canTerminate()) {
        // Not the "empty range tree" (which matches no input), but the range tree containing the
        // empty range specification (which matches only the empty digit sequence).
        return RangeTree.from(RangeSpecification.empty());
      }
      TrimmingVisitor v = new TrimmingVisitor();
      ranges.accept(v);
      return RangeTree.from(v.paths);
    }

    private final List<RangeSpecification> paths = new ArrayList<>();
    private RangeSpecification path = RangeSpecification.empty();

    @Override
    public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
      RangeSpecification oldPath = path;
      path = path.extendByMask(edge.getDigitMask());
      if (target.canTerminate()) {
        paths.add(path);
      } else {
        target.accept(this);
      }
      path = oldPath;
    }
  }

  // Note: This is NOT as simple as just calling "getPrefix()" on each range specification because
  // ranges that are too short become problematic. Consider { "7[1-9]", "70x" } which should result
  // in "7". If we just call "getPrefix()" and merge, we end up with "7x".
  //
  // One way to fix this is by repeatedly creating prefix trees (removing trailing "any digit"
  // sequences) until it becomes stable.
  //
  // The other way (simpler) is to extend the length of any shorter range specifications to bring
  // them up to the max length before merging them. In the above example, we extend the length of
  // "7[1-9]" to "7[1-9]x" and merge it with "70x" to get "7xx", which can then have its prefix
  // extracted.
  private static RangeTree removeTrailingAnyDigitPaths(RangeTree ranges) {
    if (ranges.isEmpty()) {
      return ranges;
    }
    // Skip this if "ranges" matches only one length (since it would be a no-op).
    if (ranges.getLengths().size() > 1) {
      int length = ranges.getLengths().last();
      ranges = ranges.map(s -> s.length() < length ? s.extendByLength(length - s.length()) : s);
    }
    // Having merged everything, we can now extract the correct prefixes as the final step.
    return ranges.map(RangeSpecification::getPrefix);
  }

  /**
   * Recursively determines the next level of prefix minimization. The algorithm follows as much
   * of the "included" path as possible (node), potentially splitting into several sub-recursive
   * steps if the current included edge overlaps with multiple "excluded" paths. Once a path no
   * longer overlaps with the exclude paths, it is added to the result. Paths are also added to
   * the result if they terminate while still overlapping the excluded paths.
   */
  private static RangeTree recursivelyMinimize(
      DfaNode node, RangeSpecification path, DfaNode exclude, RangeTree minimal, int minLength) {
    for (DfaEdge edge : node.getEdges()) {
      int mask = edge.getDigitMask();
      DfaNode target = edge.getTarget();
      // This algorithm only operates on the DFA of a prefix tree (not a general range tree). As
      // such the only terminating node we can reach is the terminal node itself. If we hit that
      // from the current edge, just emit it and continue on to the next edge.
      if (target.equals(RangeTree.getTerminal())) {
        minimal = minimal.union(RangeTree.from(path.extendByMask(mask)));
        continue;
      }
      checkState(!target.canTerminate(), "invalid DFA state for prefix tree at: %s", path);
      // Otherwise recurse on every "exclude" path, using the intersection of the "include" and
      // "exclude" masks. Anything left on the include mask which didn't overlap any of excluded
      // edges can emitted. This also works at the end of the exclude paths (exclude == TERMINAL)
      // since that has no outgoing edges (so the entire include path is emitted).
      for (DfaEdge ex : exclude.getEdges()) {
        int m = ex.getDigitMask() & mask;
        if (m != 0) {
          mask &= ~m;
          minimal =
              recursivelyMinimize(target, path.extendByMask(m), ex.getTarget(), minimal, minLength);
        }
      }
      // The mask identifies edges which are now outside the exclude tree, and thus safe to emit.
      if (mask != 0) {
        // Emitting an included path may involve emitting some of the sub-tree below it in order
        // to make up the minimal length (we can't do this for the terminating case above).
        minimal = emit(target, path.extendByMask(mask), minimal, minLength);
      }
    }
    return minimal;
  }

  /**
   * Recursively visits the sub-tree under the given node, extending the path until it reaches the
   * minimum length before emitting it.
   */
  private static RangeTree emit(
      DfaNode node, RangeSpecification path, RangeTree minimal, int minLength) {
    if (path.length() >= minLength || node.equals(RangeTree.getTerminal())) {
      minimal = minimal.union(RangeTree.from(path));
    } else {
      for (DfaEdge e : node.getEdges()) {
        minimal = minimal.union(
            emit(e.getTarget(), path.extendByMask(e.getDigitMask()), minimal, minLength));
      }
    }
    return minimal;
  }
}
