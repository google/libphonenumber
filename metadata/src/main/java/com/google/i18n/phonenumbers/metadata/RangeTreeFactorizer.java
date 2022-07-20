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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer.MergeStrategy.REQUIRE_EQUAL_EDGES;

import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaEdge;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 * Factor a range tree into a sequence of trees which attempts to minimize overall complexity in
 * the face of non-determinism. This can be used to reduce the size of any generated regular
 * expressions.
 */
public final class RangeTreeFactorizer {
  /** Strategies to control how merging is achieved when building factors.*/
  public enum MergeStrategy {
    /**
     * Edges are only merged if they accept exactly the same set of digits. If the existing factor
     * contains "[0-5]" it will not be merged with the candidate edge "[0-8]".
     */
    REQUIRE_EQUAL_EDGES,
    /**
     * Edges can be merged if the candidate edge accepts more digits than the existing edge. If the
     * existing factor contains "[0-5]" and the candidate edge is "[0-8]", the candidate edge is
     * split so that "[0-5]" is merged as normal and an additional edge "[6-8]" is branched off.
     */
    ALLOW_EDGE_SPLITTING,
  }

  /**
   * Factors the given range tree.
   * <p>
   * Paths are processed longest-first, and a path belongs in particular "factor" if it can be
   * added without "causing a split" in the existing factor. For example, given an existing factor
   * {@code {"12[3-6]x", "45xx"}}:
   * <ul>
   *   <li>The path "12[3-6]" can be added, since it is a prefix of one of the existing paths in
   *   the DFA.
   *   <li>The path "13xx" can be added since it forms a new branch in the DFA, which does not
   *   affect any existing branches ("13..." is disjoint with "12...").
   *   <li>The path "12[34]" cannot be added since it would "split" the existing path
   *   "12[3-6]x" in the DFA ("[34]" is a subset of "[3-6]"). "
   *   <li>Depending on the merge strategy, the path "12[0-6]x" might be added ("[0-6]" is a
   *   superset of "[3-6]"). See {@link MergeStrategy} for more information.
   * </ul>
   */
  public static ImmutableList<RangeTree> factor(RangeTree ranges, MergeStrategy strategy) {
    // If only one length on all paths, the DFA is already "factored".
    if (ranges.getLengths().size() == 1) {
      return ImmutableList.of(ranges);
    }
    List<RangeTree> factors = new ArrayList<>();
    // Start with the "naive" factors (splitting by length) from longest to shortest.
    for (int n : ranges.getLengths().descendingSet()) {
      factors.add(ranges.intersect(RangeTree.from(RangeSpecification.any(n))));
    }
    // Now attempt to merge as much of each of the shorter factors as possible into the longer ones.
    // In each loop we subsume a candidate factor into previous factors, either in whole or in part.
    int index = 1;
    while (index < factors.size()) {
      // Merge (as much as possible) each "naive" factor into earlier factors.
      RangeTree r = factors.get(index);
      for (int n = 0; n < index && !r.isEmpty(); n++) {
        RangeTree merged = new RangeTreeFactorizer(factors.get(n), strategy).mergeFrom(r);
        factors.set(n, merged);
        // Calculate the ranges which haven't yet been merged into any earlier factor.
        r = r.subtract(merged);
      }
      if (r.isEmpty()) {
        // All ranges merged, so remove the original factor (index now references the next factor).
        factors.remove(index);
      } else {
        // We have some un-factorable ranges which are kept to start a new factor.
        factors.set(index, r);
        index++;
      }
    }
    return ImmutableList.copyOf(factors);
  }

  // This is modified as paths are added.
  private RangeTree factor;
  private final MergeStrategy strategy;

  RangeTreeFactorizer(RangeTree factor, MergeStrategy strategy) {
    this.factor = checkNotNull(factor);
    this.strategy = strategy;
  }

  RangeTree mergeFrom(RangeTree ranges) {
    recursivelyMerge(ranges.getInitial(), factor.getInitial(), RangeSpecification.empty());
    return factor;
  }

  void recursivelyMerge(DfaNode srcNode, DfaNode dstNode, RangeSpecification path) {
    if (srcNode.canTerminate()) {
      factor = factor.union(RangeTree.from(path));
    } else {
      srcNode.accept(new FactoringVisitor(dstNode, path));
    }
  }

  private final class FactoringVisitor implements DfaVisitor {
    private final RangeSpecification path;
    private final DfaNode dstNode;

    // True if we encountered a situation when an edge we are merging (srcMask) has a partial
    // overlap with the existing edge (dstMask) (e.g. merging "[0-6]" into "[4-9]"). This is
    // distinct from the case where the existing edge is a subset of the edge being merged (e.g.
    // merging "[0-6]" into "[2-4]", where the edge being merged can be split into "[0156]" and
    // "[2-4]"). In either strategy, a partial overlap will prevent merging.
    private boolean partialOverlap = false;

    // Records the union of all edge ranges visited for the current node. This is used to determine
    // the remaining edges that must be added after visiting the existing factor (especially in the
    // case of ALLOW_EDGE_SPLITTING).
    private int allDstMask = 0;

    FactoringVisitor(DfaNode dstNode, RangeSpecification path) {
      this.dstNode = dstNode;
      this.path = path;
    }

    @Override
    public void visit(DfaNode source, DfaEdge srcEdge, DfaNode srcTarget) {
      int srcMask = srcEdge.getDigitMask();
      dstNode.accept((s, dstEdge, dstTarget) -> {
        int dstMask = dstEdge.getDigitMask();
        if ((strategy == REQUIRE_EQUAL_EDGES) ? (dstMask == srcMask) : (dstMask & ~srcMask) == 0) {
          // The set of digits accepted by the edge being merged (mask) is equal-to or a superset
          // of the digits of the edge in the factor we are merging into. The path is extended by
          // the destination edge because during recursion we only follow paths already in the
          // factor.
          recursivelyMerge(srcTarget, dstTarget, path.extendByMask(dstMask));
        } else {
          partialOverlap |= (dstMask & srcMask) != 0;
        }
        allDstMask |= dstMask;
      });
      if (!partialOverlap) {
        // Work out the digits that weren't in any of the edges of the factor we were processing
        // and merge the sub-tree under that edge into the current factor. For REQUIRE_EQUAL_EDGES
        // the extraMask is always either srcMask or 0 (since the edge was either added in full,
        // or disjoint with all the existing edges). For ALLOW_EDGE_SPLITTING it's the remaining
        // range that wasn't merged with any of the existing paths.
        int extraMask = srcMask & ~allDstMask;
        if (extraMask != 0) {
          new MergingVisitor(path).recurse(srcTarget, extraMask);
        }
      }
    }
  }

  private final class MergingVisitor implements DfaVisitor {
    private final RangeSpecification path;

    MergingVisitor(RangeSpecification path) {
      this.path = checkNotNull(path);
    }

    void recurse(DfaNode node, int mask) {
      RangeSpecification newPath = path.extendByMask(mask);
      if (node.canTerminate()) {
        factor = factor.union(RangeTree.from(newPath));
      } else {
        node.accept(new MergingVisitor(newPath));
      }
    }

    @Override
    public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
      recurse(target, edge.getDigitMask());
    }
  }
}
