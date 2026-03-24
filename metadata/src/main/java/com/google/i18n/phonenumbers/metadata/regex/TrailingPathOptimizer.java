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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Optimizer for NFA graphs which attempts to restructure the trailing paths to maximize sharing
 * and hopefully minimize the amount of duplication in the resulting regular expression.
 */
public final class TrailingPathOptimizer {
  /**
   * Optimizes an NFA graph to make trailing "any digit" sequences common where possible. In many
   * cases this will result in no change to the structure of the NFA (common trailing paths are
   * not a feature of every NFA), but in some cases a substantial reduction in duplication can
   * occur.
   *
   * <p>This is equivalent to recognizing that {@code "12\d{2}\d{2}?|34\d{2}|56\d{3}"} can be
   * written as {@code "(?:12\d{2}?|34|56\d)\d{2}"}.
   */
  public static ValueGraph<Node, SimpleEdge> optimize(ValueGraph<Node, SimpleEdge> graph) {
    MutableValueGraph<Node, SimpleEdge> out = Graphs.copyOf(graph);

    // Build a map of trailing "any digit" sequences (key is the node it starts from).
    Map<Node, AnyPath> anyPaths = new HashMap<>();
    recursivelyDetachTrailingPaths(Node.TERMINAL, AnyPath.EMPTY, out, anyPaths);

    // If the terminal node has no "any digit" sequences leading to it, there's nothing we can do
    // (well not in this simplistic algorithm anyway). This should almost never happen for phone
    // number matching graphs as it implies a match expression that can terminate at a precise
    // digit, rather than any digit. The only time this might occur is for short-codes, but due to
    // their size it's likely to be fine if we don't try to aggressively optimize them.
    if (anyPaths.size() == 1 && anyPaths.containsKey(Node.TERMINAL)) {
      return graph;
    }
    // This is just a way to find a node from which we can start generating new nodes.
    Node lastAddedNode = out.nodes().stream().max(naturalOrder()).get();

    // Process paths from short to long (since some paths are sub-paths of longer ones).
    List<Node> shortestPathsFirst = anyPaths.entrySet().stream()
        .sorted(Comparator.comparing(Entry::getValue))
        .map(Entry::getKey)
        .collect(toList());
    Node pathEnd = Node.TERMINAL;
    while (true) {
      // Start with the next path that might be a factor of all the remaining paths.
      Node shortestPathNode = shortestPathsFirst.get(0);
      AnyPath shortestPath = anyPaths.get(shortestPathNode);
      int pathsToFactor = shortestPathsFirst.size() - 1;
      if (pathsToFactor == 0) {
        // If all paths are factored, we're done.
        break;
      }
      // Factor all the remaining paths by the shortest path (where a missing result means it
      // cannot be factored).
      ImmutableList<AnyPath> factored = shortestPathsFirst.stream()
          .skip(1)
          .map(n -> anyPaths.get(n).factor(shortestPath))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(toImmutableList());
      // If not all the remaining paths have the shortest path as a common factor, we're done (in
      // this simplistic algorithm we don't consider cases where an AnyPath is the factor of some,
      // but not all, other paths; we could but it's far less likely to reduce regex size).
      if (factored.size() < pathsToFactor) {
        break;
      }
      // Shortest path is a factor of all remaining paths, so add a new path to the graph for it.
      lastAddedNode = addPath(shortestPathNode, pathEnd, shortestPath, lastAddedNode, out);
      // We're done with this path, but might still be able to find more factors of remaining paths.
      anyPaths.remove(shortestPathNode);
      shortestPathsFirst.remove(0);  // index, not value.
      // The newly factored edges now replace the original factors in the map.
      for (int n = 0; n < factored.size(); n++) {
        Preconditions.checkState(anyPaths.containsKey(shortestPathsFirst.get(n)));
        anyPaths.put(shortestPathsFirst.get(n), factored.get(n));
      }
      // We now connect any new factored edges to the node we just added (not the terminal node).
      pathEnd = shortestPathNode;
    }
    // If we exit, we must still reconnect any remaining, unfactored, paths to the graph.
    for (Map.Entry<Node, AnyPath> e : anyPaths.entrySet()) {
      lastAddedNode = addPath(e.getKey(), pathEnd, e.getValue(), lastAddedNode, out);
    }
    return out;
  }

  /**
   * Recursively build up a map of trailing "any digit" sequences (AnyPath), starting from some
   * current node (initially the terminal node) and working backwards. The key in the map is the
   * node at which the AnyPath value starts from. Edges and nodes are removed from the graph,
   * leaving "ragged" paths which will need to be reconnected later (the keys in the map are the
   * set of nodes that need to be reconnected).
   *
   * @return whether the given node is the start of an AnyPath (i.e. if it immediately follows any
   *     edges which are not "any digit" sequences).
   */
  private static boolean recursivelyDetachTrailingPaths(
      Node node, AnyPath path, MutableValueGraph<Node, SimpleEdge> g, Map<Node, AnyPath> anyPaths) {
    if (beginsAnAnyPath(node, g)) {
      anyPaths.put(node, path);
      return true;
    }
    // All incoming edges accept all digits, so we can recurse (but don't traverse epsilons).
    List<Node> sources = g.predecessors(node).stream()
        .filter(s -> !g.edgeValue(s, node).get().equals(Edge.epsilon()))
        .collect(toList());
    for (Node source : sources) {
      AnyPath newPath = path.extend(canTerminate(source, g));
      // Recurse to remove trailing paths higher in the tree and keep this source node only if
      // recursion stopped here.
      boolean keepSourceNode = recursivelyDetachTrailingPaths(source, newPath, g, anyPaths);
      g.removeEdge(source, node);
      // This removes the epsilon if it exists (and does nothing otherwise). This is safe since we
      // know the other out-edge of this node accepts all digits, so the only remaining type of
      // edge that could exist is an epsilon. After removing both we expect not to find any others.
      g.removeEdge(source, Node.TERMINAL);
      Preconditions.checkState(g.outDegree(source) == 0, "unexpected out edges in trailing graph");
      // If we were able to recurse past this node, it can be removed.
      if (!keepSourceNode) {
        g.removeNode(source);
      }
    }
    return false;
  }

  /**
   * Returns whether the given node has incoming edges that do not just accept "any digit". This is
   * the point at which recursion must stop since AnyPath can only represent "any digit" sequences.
   */
  private static boolean beginsAnAnyPath(Node target, ValueGraph<Node, SimpleEdge> g) {
    // Obviously we cannot recurse past the initial node.
    if (target == Node.INITIAL) {
      return true;
    }
    return g.predecessors(target).stream()
        .map(s -> g.edgeValue(s, target).get())
        .filter(e -> !e.equals(Edge.epsilon()))
        .anyMatch(e -> e.getDigitMask() != ALL_DIGITS_MASK);
  }

  /**
   * Returns whether this node can terminate. This logic relies on the input graph not having had
   * its epsilon edges moved (i.e. if an epsilon edge exists it must point to the terminal node).
   * This also looks for special "optional" edges which exist when a non-epsilon edge already
   * exists from this node to the terminal node.
   */
  private static boolean canTerminate(Node node, ValueGraph<Node, SimpleEdge> g) {
    return g.successors(node).stream()
        .map(t -> g.edgeValue(node, t).get())
        .anyMatch(e -> e.isOptional() || e.equals(Edge.epsilon()));
  }

  /** Adds the given "AnyPath" into the graph, generating new nodes and edges as necessary. */
  private static Node addPath(
      Node node, Node end, AnyPath path, Node lastAdded, MutableValueGraph<Node, SimpleEdge> out) {
    // Path length is always at least 1 for an AnyPath.
    int pathLength = path.maxLength();
    for (int n = 0; n < pathLength - 1; n++) {
      if (path.acceptsLength(n)) {
        out.putEdgeValue(node, end, Edge.epsilon());
      }
      lastAdded = lastAdded.createNext();
      out.addNode(lastAdded);
      out.putEdgeValue(node, lastAdded, Edge.any());
      node = lastAdded;
    }
    // For the last edge we cannot add a parallel epsilon path if we need to skip to the end,
    // so add the special "optional any" edge instead.
    out.putEdgeValue(
        node, end, path.acceptsLength(pathLength - 1) ? Edge.optionalAny() : Edge.any());
    return lastAdded;
  }

  private TrailingPathOptimizer() {}
}
