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
import static com.google.i18n.phonenumbers.metadata.regex.Node.INITIAL;
import static com.google.i18n.phonenumbers.metadata.regex.Node.TERMINAL;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;

/** Simple fluent API for constructing graphs for testing. */
final class NfaBuilder {
  private final MutableValueGraph<Node, SimpleEdge> graph =
      ValueGraphBuilder.directed().allowsSelfLoops(false).build();
  // The last node added to the graph.
  private Node lastNode;

  /** Creates a new mutable NFA graph. */
  public NfaBuilder() {
    graph.addNode(INITIAL);
    graph.addNode(TERMINAL);
    lastNode = TERMINAL;
  }

  /**
   * Returns an unmodifiable view of the underlying graph (not a snapshot). If the builder is
   * modified after this method is called, it will affect what was returned.
   */
  public ValueGraph<Node, SimpleEdge> graph() {
    return graph;
  }

  /** Adds a new path from the given source node, returning the newly created target node. */
  public Node addPath(Node source, String path) {
    RangeSpecification spec = RangeSpecification.parse(path);
    for (int n = 0; n < spec.length(); n++) {
      lastNode = lastNode.createNext();
      addEdge(source, lastNode, SimpleEdge.fromMask(spec.getBitmask(n)));
      source = lastNode;
    }
    return lastNode;
  }

  /** Adds a new path between the given source and target (all intermediate nodes are new). */
  public void addPath(Node source, Node target, String path) {
    RangeSpecification spec = RangeSpecification.parse(path);
    for (int n = 0; n < spec.length() - 1; n++) {
      lastNode = lastNode.createNext();
      addEdge(source, lastNode, SimpleEdge.fromMask(spec.getBitmask(n)));
      source = lastNode;
    }
    addEdge(source, target, SimpleEdge.fromMask(spec.getBitmask(spec.length() - 1)));
  }

  /**
   * Adds a new path between the given source and target nodes, along with an epsilon edge from the
   * source to the target.
   */
  public void addOptionalPath(Node source, Node target, String path) {
    addPath(source, target, path);
    addEpsilon(source, target);
  }

  private void addEpsilon(Node s, Node t) {
    checkArgument(graph.nodes().contains(s), "missing source node");
    checkArgument(graph.nodes().contains(s), "missing target node");
    SimpleEdge e = graph.putEdgeValue(s, t, Edge.epsilon());
    if (e != null) {
      // Edge already exists; if not an epsilon, make it optional.
      checkArgument(!e.equals(Edge.epsilon()) && !e.isOptional(), "epsilon already added");
      graph.putEdgeValue(s, t, e.optional());
    }
  }

  private void addEdge(Node s, Node t, SimpleEdge e) {
    graph.addNode(s);
    graph.addNode(t);
    checkArgument(graph.putEdgeValue(s, t, e) == null, "edge already exists");
  }
}
