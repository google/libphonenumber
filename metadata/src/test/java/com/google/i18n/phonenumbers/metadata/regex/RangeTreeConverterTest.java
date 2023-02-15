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
import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.regex.Edge.any;
import static com.google.i18n.phonenumbers.metadata.regex.Edge.epsilon;
import static com.google.i18n.phonenumbers.metadata.regex.Edge.optionalAny;
import static com.google.i18n.phonenumbers.metadata.regex.Node.INITIAL;
import static com.google.i18n.phonenumbers.metadata.regex.Node.TERMINAL;

import com.google.common.collect.Iterables;
import com.google.common.graph.ValueGraph;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RangeTreeConverterTest {
  // Simple 4 node DFA.
  // (I) --1--> ( ) --2--> ( ) --x--> (T)
  @Test
  public void testSimple() {
    RangeTree dfa = RangeTree.from(specs("12x"));
    ValueGraph<Node, SimpleEdge> nfa = RangeTreeConverter.toNfaGraph(dfa);
    assertThat(nfa.nodes()).hasSize(4);
    Node node = assertOutEdge(nfa, INITIAL, edge(1));
    node = assertOutEdge(nfa, node, edge(2));
    node = assertOutEdge(nfa, node, any());
    assertThat(node).isEqualTo(TERMINAL);
  }

  // Simple 4 node DFA with optional termination immediately before the terminal node.
  // (I) --1--> ( ) --2--> (T) --x--> (T)
  @Test
  public void testWithOptionalEdge() {
    RangeTree dfa = RangeTree.from(specs("12x", "12"));

    ValueGraph<Node, SimpleEdge> nfa = RangeTreeConverter.toNfaGraph(dfa);
    assertThat(nfa.nodes()).hasSize(4);
    Node node = assertOutEdge(nfa, INITIAL, edge(1));
    node = assertOutEdge(nfa, node, edge(2));
    node = assertOutEdge(nfa, node, optionalAny());
    assertThat(node).isEqualTo(TERMINAL);
  }

  // Simple 4 node DFA with optional termination.
  // (I) --1--> (T) --2--> ( ) --x--> (T)
  @Test
  public void testWithEpsilon() {
    RangeTree dfa = RangeTree.from(specs("12x", "1"));

    ValueGraph<Node, SimpleEdge> nfa = RangeTreeConverter.toNfaGraph(dfa);
    assertThat(nfa.nodes()).hasSize(4);
    Node node = assertOutEdge(nfa, INITIAL, edge(1));
    assertOutEdges(nfa, node, edge(2), epsilon());
    // One of the out nodes should be the terminal.
    assertThat(follow(nfa, node, epsilon())).isEqualTo(Node.TERMINAL);
    node = follow(nfa, node, edge(2));
    // The other is the normal edge that leads to the terminal.
    node = follow(nfa, node, any());
    assertThat(node).isEqualTo(TERMINAL);
  }

  // Simple 5 node DFA with 2 paths.
  // (I) --1--> ( ) --2--> ( ) --x--> (T)
  //   `---3--> ( ) --4----^
  @Test
  public void testMultiplePathsWithCommonTail() {
    RangeTree dfa = RangeTree.from(specs("12x", "34x"));

    ValueGraph<Node, SimpleEdge> nfa = RangeTreeConverter.toNfaGraph(dfa);
    assertThat(nfa.nodes()).hasSize(5);

    assertOutEdges(nfa, INITIAL, edge(1), edge(3));
    Node lhs = follow(nfa, INITIAL, edge(1));
    lhs = assertOutEdge(nfa, lhs, edge(2));
    Node rhs = follow(nfa, INITIAL, edge(3));
    rhs = assertOutEdge(nfa, rhs, edge(4));
    assertThat(lhs).isEqualTo(rhs);
    Node node = assertOutEdge(nfa, lhs, any());
    assertThat(node).isEqualTo(TERMINAL);
  }

  @Test
  public void testOptionalTopLevelGroup_bug_69101586() {
    // Requires making a top level optional group, which is (deliberately) not easy with the
    // DFA tooling since it's pretty rare. This is a DFA which can terminate immediately and will
    // match the empty input (as well as its normal input).
    RangeTree dfa = RangeTree.from(specs("xx")).union(RangeTree.from(RangeSpecification.empty()));

    ValueGraph<Node, SimpleEdge> nfa = RangeTreeConverter.toNfaGraph(dfa);
    assertThat(nfa.nodes()).hasSize(3);
    assertThat(follow(nfa, INITIAL, epsilon())).isEqualTo(Node.TERMINAL);
    Node node = follow(nfa, INITIAL, any());
    node = assertOutEdge(nfa, node, any());
    assertThat(node).isEqualTo(TERMINAL);
  }

  // Returns the simple edge matching exactly this one digit value.
  SimpleEdge edge(int n) {
    return SimpleEdge.fromMask(1 << n);
  }

  List<RangeSpecification> specs(String... s) {
    return Stream.of(s).map(RangeSpecification::parse).collect(toImmutableList());
  }

  // Asserts that a node has only one out edge and returns that edge's target.
  Node assertOutEdge(ValueGraph<Node, SimpleEdge> nfa, Node node, SimpleEdge edge) {
    assertThat(nfa.successors(node)).hasSize(1);
    Node target = Iterables.getOnlyElement(nfa.successors(node));
    assertThat(nfa.edgeValue(node, target).get()).isEqualTo(edge);
    return target;
  }

  // Asserts that a node has all the given edges.
  void assertOutEdges(ValueGraph<Node, SimpleEdge> nfa, Node node, SimpleEdge... edges) {
    assertThat(nfa.successors(node)).hasSize(edges.length);
    List<Edge> out = nfa.successors(node).stream()
        .map(t -> nfa.edgeValue(node, t).get())
        .collect(toImmutableList());
    assertThat(out).containsExactlyElementsIn(edges);
  }

  // Follows the given edge from a node (which must be in the graph), returning the target node
  // (or null if the edge does not exist in the graph).
  Node follow(ValueGraph<Node, SimpleEdge> nfa, Node node, SimpleEdge edge) {
    return nfa.successors(node).stream()
        .filter(t -> nfa.edgeValue(node, t).get().equals(edge))
        .findFirst()
        .orElse(null);
  }
}
