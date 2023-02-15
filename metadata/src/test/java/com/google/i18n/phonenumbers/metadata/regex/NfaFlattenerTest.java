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

import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.regex.Node.INITIAL;
import static com.google.i18n.phonenumbers.metadata.regex.Node.TERMINAL;

import com.google.common.base.Preconditions;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NfaFlattenerTest {
  // The 'any digit' edge.
  private static final Edge X = e("x");

  @Test
  public void testSimple() {
    NfaBuilder nfa = new NfaBuilder();
    nfa.addPath(INITIAL, TERMINAL, "12");
    Edge flat = NfaFlattener.flatten(nfa.graph());
    assertThat(flat).isEqualTo(seq(e("1"), e("2")));
    assertThat(flat.toString()).isEqualTo("12");

    nfa.addPath(INITIAL, TERMINAL, "34");
    flat = NfaFlattener.flatten(nfa.graph());
    assertThat(flat).isEqualTo(
        or(
            seq(e("1"), e("2")),
            seq(e("3"), e("4"))));
    assertThat(flat.toString()).isEqualTo("(12|34)");
  }

  @Test
  public void testSubgroup() {
    NfaBuilder nfa = new NfaBuilder();
    Node split = nfa.addPath(INITIAL, "12");
    Node join = nfa.addPath(split, "34");
    nfa.addPath(split, join, "56");
    nfa.addPath(join, TERMINAL, "78");

    Edge flat = NfaFlattener.flatten(nfa.graph());
    assertThat(flat).isEqualTo(
        seq(e("1"), e("2"),
            or(
                seq(e("3"), e("4")),
                seq(e("5"), e("6"))
            ),
            e("7"), e("8")));
    assertThat(flat.toString()).isEqualTo("12(34|56)78");
  }

  @Test
  public void testSubgroupWithEarlyJoining() {
    NfaBuilder nfa = new NfaBuilder();
    // Create a graph with 4 initial paths branching out which collapses to 3, 2 and then 1.
    Node groupStart = nfa.addPath(INITIAL, "0");
    // Add 2 edges to the first join point (if we add only one edge then it clashes with the
    // joining edge, which goes directly from groupStart to firstJoin.
    Node firstJoin = nfa.addPath(nfa.addPath(groupStart, "1"), "2");
    nfa.addPath(groupStart, firstJoin, "3");
    Node secondJoin = nfa.addPath(firstJoin, "4");
    nfa.addPath(groupStart, secondJoin, "5");
    Node groupEnd = nfa.addPath(secondJoin, "6");
    nfa.addPath(groupStart, groupEnd, "7");
    nfa.addPath(groupEnd, TERMINAL, "8");

    Edge flat = NfaFlattener.flatten(nfa.graph());
    assertThat(flat).isEqualTo(
        seq(e("0"),
            or(
                seq(
                    or(
                        seq(
                            or(
                                seq(e("1"), e("2")),
                                e("3")),
                            e("4")),
                        e("5")),
                    e("6")),
                e("7")),
            e("8")));
    assertThat(flat.toString()).isEqualTo("0(((12|3)4|5)6|7)8");
  }

  @Test
  public void testPathDuplication() {
    NfaBuilder nfa = new NfaBuilder();
    Node groupStart = nfa.addPath(INITIAL, "0");
    Node lhsMid = nfa.addPath(groupStart, "1");
    Node groupEnd = nfa.addPath(lhsMid, "2");
    Node rhsMid = nfa.addPath(groupStart, "3");
    nfa.addPath(rhsMid, groupEnd, "4");
    nfa.addPath(groupEnd, TERMINAL, "5");

    // So far this is a normal nestable graph:
    //           ,--1-->()--2--v
    // (I)--0-->()             ()--5-->(T)
    //           `--3-->()--4--^
    Edge flat = NfaFlattener.flatten(nfa.graph());
    assertThat(flat).isEqualTo(
        seq(e("0"),
            or(
                seq(e("1"), e("2")),
                seq(e("3"), e("4"))),
            e("5")));
    assertThat(flat.toString()).isEqualTo("0(12|34)5");

    // This new path "crosses" the group, creating a non-nestable structure which can only be
    // resolved by duplicating some path (in this case it's the 2nd part of the right-hand-side).
    nfa.addPath(lhsMid, rhsMid, "x");

    flat = NfaFlattener.flatten(nfa.graph());
    assertThat(flat).isEqualTo(
        seq(e("0"),
            or(
                seq(e("1"),
                    or(
                        e("2"),
                        seq(X, e("4")))),
                seq(e("3"), e("4"))),
            e("5")));
    // Note the duplication of the '4' to make the graph nestable.
    assertThat(flat.toString()).isEqualTo("0(1(x4|2)|34)5");

  }

  @Test
  public void testNodeOrdering_bug_65250963() {
    //  ,--->(C)----------.
    //  |                 v
    // (I)-->(D)-->(B)-->(T)
    //  |           ^
    //  `--->(A)----'
    NfaBuilder nfa = new NfaBuilder();
    // IMPORTANT: Order of insertion determines the node IDs (A=1, B=2...). The edge index just
    // happens to match node ID for readability, but doesn't affect the test directly.
    Node a = nfa.addPath(INITIAL, "1");
    Node b = nfa.addPath(a, "2");
    Node c = nfa.addPath(INITIAL, "3");
    Node d = nfa.addPath(INITIAL, "4");
    // Now join up remaining paths.
    nfa.addPath(d, b, "5");
    nfa.addPath(b, TERMINAL, "6");
    nfa.addPath(c, TERMINAL, "7");
    Comparator<Node> ordering = NfaFlattener.nodeOrdering(nfa.graph());

    // In the old ordering code, because (B) and (D) are not reachable to/from (C) we would have
    // had the ordering (D < B), (B < C), (C < D) giving a cycle. In the new code, the longest path
    // length to reach (C) is less than (B), so we get (C < B) and we no longer have a cycle.
    // The node ordering is now: (INITIAL, A, C, D, B, TERMINAL)
    TreeSet<Node> nodes = new TreeSet<>(ordering);
    nodes.add(INITIAL);
    nodes.add(TERMINAL);
    nodes.add(a);
    nodes.add(b);
    nodes.add(c);
    nodes.add(d);
    assertThat(nodes).containsExactly(INITIAL, a, c, d, b, TERMINAL).inOrder();
  }

  @Test
  public void testOptionalTopLevelGroup_bug_69101586() {
    //  ,--->(e)----.
    //  |           v
    // (I)-->(A)-->(T)
    NfaBuilder nfa = new NfaBuilder();
    nfa.addOptionalPath(INITIAL, TERMINAL, "xx");
    Edge flat = NfaFlattener.flatten(nfa.graph());
    assertThat(flat).isEqualTo(opt(seq(X, X)));
    assertThat(flat.toString()).isEqualTo("(xx)?");
  }

  // Creates a simple edge from a range specification string for testing.
  private static SimpleEdge e(String s) {
    RangeSpecification spec = RangeSpecification.parse(s);
    Preconditions.checkArgument(spec.length() == 1, "only specify single digit ranges");
    return SimpleEdge.fromMask(spec.getBitmask(0));
  }

  // Creates sequence of edges (wrapping for convenience).
  private static Edge seq(Edge first, Edge second, Edge... rest) {
    // This already rejects epsilon edges.
    Edge edge = Edge.concatenation(first, second);
    for (Edge e : rest) {
      edge = Edge.concatenation(edge, e);
    }
    return edge;
  }

  // Creates an optional disjunction of edges.
  private static Edge opt(Edge... edges) {
    List<Edge> e = new ArrayList<>();
    e.addAll(Arrays.asList(edges));
    Preconditions.checkArgument(!e.contains(Edge.epsilon()), "don't pass epsilon directly");
    e.add(Edge.epsilon());
    return Edge.disjunction(e);
  }

  // Creates a non-optional disjunction of edges.
  private static Edge or(Edge... edges) {
    List<Edge> e = Arrays.asList(edges);
    Preconditions.checkArgument(!e.contains(Edge.epsilon()), "use 'opt()' for optional groups");
    return Edge.disjunction(e);
  }
}
