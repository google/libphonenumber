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
import com.google.common.collect.Iterables;
import com.google.common.graph.ValueGraph;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Function;

/**
 * Flattens an NFA graph of simple edges into a composite edge which represents all the same
 * transitions in a strict tree structure (i.e. nestable sub-groups). This can entail some
 * duplication of edges, but this should be kept to a minimum and favours duplicating trailing
 * paths to avoid introducing additional non-determinism.
 */
final class NfaFlattener {
  /**
  * Flattens the given NFA graph into a single composite edge composed of concatenation and
  * disjunction. The resulting edge can be visited using the {@code Edge.Visitor} class.
  */
  public static Edge flatten(ValueGraph<Node, SimpleEdge> graph) {
    return new NfaFlattener(graph).flatten();
  }

  /*
   * A simple pair of edge value and target node which represents the current state along any path
   * in the NFA graph. Path followers may be joined (if they point at the same node) but can only
   * be split by recursion into the new subgraph.
   */
  @AutoValue
  abstract static class PathFollower {
    private static PathFollower of(Node node, Edge edge) {
      return new AutoValue_NfaFlattener_PathFollower(node, edge);
    }

    /** The target node that this follower points to. */
    abstract Node node();
    /** A composite edge representing everything up to the target node in the current sub-graph. */
    abstract Edge edge();
  }

  // The graph being flattened.
  private final ValueGraph<Node, SimpleEdge> graph;
  // An ordering for the work queue which ensures that followers with the same node are adjacent.
  private final Comparator<PathFollower> queueOrder;

  private NfaFlattener(ValueGraph<Node, SimpleEdge> graph) {
    this.graph = graph;
    this.queueOrder = Comparator
        .comparing(PathFollower::node, nodeOrdering(graph))
        .thenComparing(PathFollower::edge);
  }

  private Edge flatten() {
    // Sub-graph visitation only works for graphs which branch from and collapse to a single node.
    // An NFA graph could be multiple sequential edges or a sequence of edges and sub-graphs.
    // Handle that in this outer loop rather than complicate the visitor (already quite complex).
    PathFollower out = visitSubgraph(Node.INITIAL);
    while (out.node() != Node.TERMINAL) {
      PathFollower subgraph = visitSubgraph(out.node());
      out = PathFollower.of(subgraph.node(), Edge.concatenation(out.edge(), subgraph.edge()));
    }
    return out.edge();
  }

  /**
   * Visits the sub-graph rooted at the given node, following all out-edges until they eventually
   * re-join. Because the given graph has only one terminal node and no cycles, all sub-graphs must
   * eventually rejoin at some point. If during visitation of a sub-graph, a node with multiple
   * out-edges is reached, then the sub-graph it starts is recursively visited. Note that as "inner"
   * sub-graphs must terminate at or before their parent graph, nesting is assured.
   *
   * <p>The key to the implementation of this algorithm is that visitation occurs in breadth-first
   * order defined according to the reachability of the nodes in the graph. This ensures that when
   * an edge follower which reaches a node at which other edges join together is processed (i.e.
   * when it gets to the head of the queue) all the other followers that can also reach that node
   * must also be present in a contiguous sequence at the front of the queue.
   */
  private PathFollower visitSubgraph(Node node) {
    Preconditions.checkArgument(graph.outDegree(node) > 0, "cannot recurse from the terminal node");
    if (graph.outDegree(node) == 1) {
      // Visit the trivial "subgraph" that's really just a single edge. Note that this code could
      // loop and concatenate all sequential single edges, but it also works fine to rely on the
      // recursion of the caller (the advantage of doing it this, simpler, way means that this code
      // doesn't have to know about termination due to reaching the terminal node).
      Node target = Iterables.getOnlyElement(graph.successors(node));
      return PathFollower.of(target, graph.edgeValue(node, target).get());
    }
    // A work-queue of the path followers, ordered primarily by the node they target. This results
    // in the followers at any "point of collapse" being adjacent in the queue.
    PriorityQueue<PathFollower> followerQueue = new PriorityQueue<>(queueOrder);
    for (Node t : graph.successors(node)) {
      followerQueue.add(PathFollower.of(t, graph.edgeValue(node, t).get()));
    }
    while (true) {
      // Get the set of followers that share the same target node at the head of the queue. The
      // ordering in the queue ensures that followers for the same target are always adjacent.
      PathFollower follower = followerQueue.remove();
      Node target = follower.node();
      List<Edge> joiningEdges = collectJoiningEdges(followerQueue, target);
      if (joiningEdges != null) {
        // Replace any joined followers with their disjunction (they all have the same target).
        joiningEdges.add(follower.edge());
        follower = PathFollower.of(target, Edge.disjunction(joiningEdges));
      }
      if (followerQueue.isEmpty()) {
        // If we just processed the last "joining" paths then this sub-graph has been collapsed
        // into a single edge and we just return the current follower. Note that we can join edges
        // without ending recursion (when 3 followers become 2) but we can only end recursion after
        // joining at least 2 edges at the terminal sub-graph node.
        return follower;
      }
      // Recurse into the next sub-graph (possibly just a single edge) which is just concatenated
      // onto the current follower.
      PathFollower subgraph = visitSubgraph(target);
      followerQueue.add(
          PathFollower.of(subgraph.node(), Edge.concatenation(follower.edge(), subgraph.edge())));
    }
  }

  // Collects the edges of any followers at the front of the queue which share the same target node
  // as the given follower. If the node is not a target of any other followers then return null.
  private static List<Edge> collectJoiningEdges(PriorityQueue<PathFollower> queue, Node target) {
    // It's really common for edges not to join, so avoid making the list unless necessary.
    if (!nextFollowerJoinsTarget(queue, target)) {
      return null;
    }
    List<Edge> joiningEdges = new ArrayList<>();
    do {
      joiningEdges.add(queue.remove().edge());
    } while (nextFollowerJoinsTarget(queue, target));
    return joiningEdges;
  }

  // Checks if the head of the queue is a follower with the same target node.
  private static boolean nextFollowerJoinsTarget(PriorityQueue<PathFollower> queue, Node target) {
    return !queue.isEmpty() && queue.peek().node().equals(target);
  }

  /**
   * Returns a total ordering of nodes in this graph based on the maximum path length from the
   * initial node. If path lengths are equal for two nodes, then the node ID is used to tie break.
   *
   * <p>The property of this ordering that is critical to the node flattening algorithm is that if
   * {@code a < b}, then no path exists in the graph where {@code b} precedes {@code a}. This
   * ensures that path followers are processed consistently with the "node reachability" and if
   * several path followers target the same node, then they are adjacent in the follower queue.
   *
   * <p>Using the node ID as a tie-break is safe, because while node IDs are assigned arbitrarily,
   * they only apply between nodes in the same path length "bucket", so it cannot violate the total
   * ordering requirement, since any order within a "bucket" is equally good.
   */
  // Note: If there are graph cycles this will not terminate, but that implies bad bugs elsewhere.
  @VisibleForTesting
  static Comparator<Node> nodeOrdering(ValueGraph<Node, ?> graph) {
    Map<Node, Integer> map = new HashMap<>();
    recursivelySetMaxPathLength(Node.INITIAL, 0, graph, map);
    // We have to cast the "get" method since it accepts "Object", not "Node" on a map.
    return Comparator.comparing((Function<Node, Integer>) map::get).thenComparing(Node::id);
  }

  private static void recursivelySetMaxPathLength(
      Node node, int length, ValueGraph<Node, ?> graph, Map<Node, Integer> map) {
    // Only continue if at least some paths can be lengthened from here onwards.
    if (length > map.getOrDefault(node, -1)) {
      map.put(node, length);
      for (Node target : graph.successors(node)) {
        recursivelySetMaxPathLength(target, length + 1, graph, map);
      }
    }
  }
}
