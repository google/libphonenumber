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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaEdge;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

/**
 * An optimization for RangeTree DFAs which attempts to isolate and extract subgraphs which would
 * otherwise cause a lot of repetition in the generated regular expression.
 */
public final class SubgroupOptimizer {
  /**
   * Returns the subgraph which is likely to cause the most repetition in the regular expression
   * of the given DFA. Subtracting the result out of the original range tree and generating two
   * distinct regular expressions is likely to be shorter than the regular expression of the
   * original range.
   */
  public static Optional<RangeTree> extractRepeatingSubgraph(RangeTree ranges) {
    return LinkNodeVisitor
        .findBridgingNode(ranges)
        .flatMap(n -> SubgraphExtractionVisitor.extractSubgraph(ranges, n));
  }

  /**
   * A visitor which applies two types of weights to every interior node in a DFA.
   * <ul>
   *   <li>A count of incoming edges to that node.
   *   <li>A count of all edges in the subgraph rooted at that node.
   * </ul>
   * These are then multiplied together using the cost function:
   * <pre>cost(n) = subgraph-weight(n) * (in-order(n) - 1)</pre>
   * get get a proxy for the cost of additional duplicates likely to be created by this node.
   */
  static class LinkNodeVisitor implements DfaVisitor {
    // Reasonable approximation for the cost of an edge in a subgraph is the length of the
    // corresponding range specification (it doesn't work so well for repeated edges like
    // 'xxxxxxxx' --> "\d{8}", but it's good to help break ties in the cost function).
    private static final ImmutableList<Integer> EDGE_WEIGHTS =
        IntStream.rangeClosed(1, 0x3FF)
            .mapToObj(m -> RangeSpecification.toString(m).length())
            .collect(toImmutableList());

    // Important to use "linked" multisets here (at least for the one we iterate over) since
    // otherwise we end up with non-deterministic regular expression generation.
    private final Multiset<DfaNode> inOrder = LinkedHashMultiset.create();
    private final Multiset<DfaNode> subgraphWeight = LinkedHashMultiset.create();

    /**
     * Returns the interior node whose subgraph is likely to cause the most repetition in the
     * regular expression of the given DFA.
     */
    static Optional<DfaNode> findBridgingNode(RangeTree ranges) {
      checkArgument(!ranges.isEmpty(), "cannot visit empty ranges");
      LinkNodeVisitor v = new LinkNodeVisitor();
      ranges.accept(v);
      return Optional.ofNullable(v.getHighestCostNode());
    }

    private static int getEdgeWeight(DfaEdge edge) {
      // Subtract 1 since the array is 1-based (a zero edge mask is not legal).
      return EDGE_WEIGHTS.get(edge.getDigitMask() - 1);
    }

    @VisibleForTesting
    int getSubgraphWeight(DfaNode n) {
      return subgraphWeight.count(n);
    }

    @VisibleForTesting
    int getInOrder(DfaNode n) {
      return inOrder.count(n);
    }

    // This returns null if no edge has a cost greater than zero. Since the cost function uses
    // (in-order(n) - 1) this is trivially true for any graph where all interior nodes have only
    // a single in-edge (the terminal node can have more than one in-edge, but it has a weight of
    // zero and the initial node is never considered a candidate).
    @VisibleForTesting
    @Nullable
    DfaNode getHighestCostNode() {
      DfaNode node = null;
      int maxWeight = 0;
      for (DfaNode n : inOrder.elementSet()) {
        int weight = getSubgraphWeight(n) * (getInOrder(n) - 1);
        if (weight > maxWeight) {
          maxWeight = weight;
          node = n;
        }
      }
      return node;
    }

    @Override
    public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
      // The weight is zero only if we haven't visited this node before (or it's the terminal).
      int targetWeight = subgraphWeight.count(target);
      if (targetWeight == 0 && !target.equals(RangeTree.getTerminal())) {
        target.accept(this);
        targetWeight = subgraphWeight.count(target);
      }
      // Add an extra one for the edge we are processing now and increment our target's in-order.
      subgraphWeight.add(source, targetWeight + getEdgeWeight(edge));
      inOrder.add(target);
    }
  }

  /**
   * A visitor to extract the subgraph of a DFA which passes through a specified interior
   * "bridging" node.
   */
  private static class SubgraphExtractionVisitor implements DfaVisitor {
    private final DfaNode bridgingNode;
    private final List<RangeSpecification> paths = new ArrayList<>();
    private RangeSpecification path = RangeSpecification.empty();
    private boolean sawBridgingNode = false;
    private boolean splitHappens = false;

    /** Returns the subgraph which passes through the specified node. */
    static Optional<RangeTree> extractSubgraph(RangeTree ranges, DfaNode node) {
      SubgraphExtractionVisitor v = new SubgraphExtractionVisitor(node);
      ranges.accept(v);
      // Only return proper subgraphs.
      return v.splitHappens ? Optional.of(RangeTree.from(v.paths)) : Optional.empty();
    }

    private SubgraphExtractionVisitor(DfaNode bridgingNode) {
      this.bridgingNode = checkNotNull(bridgingNode);
    }

    @Override
    public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
      RangeSpecification oldPath = path;
      path = path.extendByMask(edge.getDigitMask());
      // Potentially emit paths for any terminating node (not just the end of the graph). We have
      // to extract the entire sub-graph _after_ the bridging node, including terminating nodes.
      if (target.canTerminate()) {
        // Emit path if we are "below" the bridging node.
        if (sawBridgingNode) {
          paths.add(path);
        } else {
          // Records that there were other paths not in the subgroup (since we only want to return
          // a new DFA that's a proper subgraph of the original graph).
          splitHappens = true;
        }
      }
      if (target.equals(bridgingNode)) {
        // Recurse with the flag set to emit paths once we hit the terminal node (note that the
        // bridging node cannot be the terminal node).
        sawBridgingNode = true;
        target.accept(this);
        sawBridgingNode = false;
      } else {
        // Recurse normally regardless of the flag.
        target.accept(this);
      }
      path = oldPath;
    }
  }
}
