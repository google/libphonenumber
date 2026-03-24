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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaEdge;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaVisitor;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts DFA {@link RangeTree}s to NFA {@link ValueGraph}s. The resulting graph has almost
 * exactly the same node and edge structure as the original DFA, with the following exceptions:
 * <ol>
 *   <li>Nodes which could optionally terminate now have 'epsilon' edges connecting them to the
 *   terminal node.
 *   <li>If an optionally terminating node connects directly to the terminal node, then a special
 *   "optional edge" is used (this is because the {@link ValueGraph} structure allows only one
 *   value for each edge, so you can't have an epsilon edge that goes between the same source and
 *   target as other edge).
 * </ol>
 */
public final class RangeTreeConverter {
  /**
   * Returns the directed NFA graph representation of a {@link RangeTree}. The returned graph is
   * not a DFA and may contain epsilon transitions. Nodes are assigned in visitation order, except
   * for the initial and terminal nodes which are always present in the graph.
   */
  public static ValueGraph<Node, SimpleEdge> toNfaGraph(RangeTree ranges) {
    NfaVisitor visitor = new NfaVisitor(ranges.getInitial());
    ranges.accept(visitor);
    return visitor.graph;
  }

  private static class NfaVisitor implements DfaVisitor {
    private final MutableValueGraph<Node, SimpleEdge> graph = ValueGraphBuilder
        .directed()
        .allowsSelfLoops(false)
        // Stable ordering should help keep any generated structures (regex, graph files) stable.
        .nodeOrder(ElementOrder.<Node>natural())
        .build();
    // Map of nodes added to the new graph (keyed by the corresponding DFA node).
    private final Map<DfaNode, Node> nodeMap = new HashMap<>();
    // The last node we added.
    private Node lastAdded;

    private NfaVisitor(DfaNode initial) {
      // Add initial and terminal nodes first (there's always exactly one of each).
      graph.addNode(Node.INITIAL);
      graph.addNode(Node.TERMINAL);
      // During visitation we check only target nodes to add epsilon edges, but we may also need
      // to add an epsilon from the very top if the DFA can match the empty input.
      if (initial.canTerminate()) {
        graph.putEdgeValue(Node.INITIAL, Node.TERMINAL, Edge.epsilon());
      }
      nodeMap.put(initial, Node.INITIAL);
      nodeMap.put(RangeTree.getTerminal(), Node.TERMINAL);
      lastAdded = Node.TERMINAL;
    }

    @Override
    public void visit(DfaNode dfaSource, DfaEdge dfaEdge, DfaNode dfaTarget) {
      SimpleEdge simpleEdge = Edge.fromMask(dfaEdge.getDigitMask());
      Node source = nodeMap.get(dfaSource);
      Node target = getTarget(dfaTarget);
      boolean wasNewNode = graph.addNode(target);
      // The only chance of an existing edge is if an epsilon was already added immediately before
      // visiting this edge. This can only occur if (target == TERMINAL) however.
      SimpleEdge epsilon = graph.putEdgeValue(source, target, simpleEdge);
      if (epsilon != null) {
        checkState(target.equals(Node.TERMINAL) && epsilon.equals(Edge.epsilon()),
            "unexpected edge during visitation: %s -- %s --> %s", source, epsilon, target);
        // Re-add the edge, but this time make it optional (because that's what epsilon means).
        graph.putEdgeValue(source, target, simpleEdge.optional());
      }
      // Only recurse if the target node was newly added to the graph in this visitation.
      if (wasNewNode) {
        // The TERMINAL node is always in the map so (target != TERMINAL) here. This means we
        // never risk adding a loop in the graph. The epsilon may end up being swapped out for
        // an optional edge when we visit the dfaTarget, but that's fine.
        if (dfaTarget.canTerminate()) {
          graph.putEdgeValue(target, Node.TERMINAL, Edge.epsilon());
        }
        dfaTarget.accept(this);
      }
    }

    // Gets or creates a new target node, adding it to the node map (but not to the graph itself).
    private Node getTarget(DfaNode gnode) {
      Node target = nodeMap.get(gnode);
      if (target != null) {
        return target;
      }
      lastAdded = lastAdded.createNext();
      nodeMap.put(gnode, lastAdded);
      return lastAdded;
    }
  }

  private RangeTreeConverter() {}
}
