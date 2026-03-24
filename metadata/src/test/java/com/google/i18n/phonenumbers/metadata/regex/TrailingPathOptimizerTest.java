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

import com.google.common.graph.ValueGraph;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TrailingPathOptimizerTest {
  @Test
  public void testSimple() {
    NfaBuilder nfa = new NfaBuilder();
    nfa.addPath(INITIAL, TERMINAL, "12xx");
    nfa.addPath(INITIAL, TERMINAL, "34xxxx");
    ValueGraph<Node, SimpleEdge> actual = TrailingPathOptimizer.optimize(nfa.graph());

    // Expect the common trailing "xx" to be factored out at some new join point.
    NfaBuilder expected = new NfaBuilder();
    Node join = expected.addPath(INITIAL, "12");
    expected.addPath(INITIAL, join, "34xx");
    expected.addPath(join, TERMINAL, "xx");

    assertEquivalent(actual, expected);
  }

  @Test
  public void testTrailingOptionalGroup() {
    NfaBuilder nfa = new NfaBuilder();
    nfa.addPath(INITIAL, TERMINAL, "12xx");
    // Add path "34xx(xx)?"
    Node optStart = nfa.addPath(INITIAL, "34xx");
    nfa.addOptionalPath(optStart, TERMINAL, "xx");

    ValueGraph<Node, SimpleEdge> actual = TrailingPathOptimizer.optimize(nfa.graph());

    // Expect the common trailing "xx" to be factored out at some new join point.
    NfaBuilder expected = new NfaBuilder();
    Node join = expected.addPath(INITIAL, "12");
    // Add "34(xx)?" up to the joining node.
    optStart = expected.addPath(INITIAL, "34");
    expected.addOptionalPath(optStart, join, "xx");
    // Add the trailing "xx".
    expected.addPath(join, TERMINAL, "xx");

    assertEquivalent(actual, expected);
  }

  @Test
  public void testDoubleRecursion() {
    NfaBuilder nfa = new NfaBuilder();
    nfa.addPath(INITIAL, TERMINAL, "12xx");
    nfa.addPath(INITIAL, TERMINAL, "34xxxx");
    // Add path "56xxxx(xx)?"
    Node optStart = nfa.addPath(INITIAL, "56xxxx");
    nfa.addOptionalPath(optStart, TERMINAL, "xx");
    ValueGraph<Node, SimpleEdge> actual = TrailingPathOptimizer.optimize(nfa.graph());

    // Factoring should be applied twice to pull out 2 lots of "xx".
    // How I wish we had a way to embed proper graphs in JavaDoc!
    //
    //    ,-----------12-----------v
    // (I)------34----->(1)--xx-->(2)--xx-->(T)
    //    `-56-->()--xx--^
    //            `--e---^
    //
    NfaBuilder expected = new NfaBuilder();
    Node secondJoin = expected.addPath(INITIAL, "12");
    expected.addPath(secondJoin, TERMINAL, "xx");
    Node firstJoin = expected.addPath(INITIAL, "34");
    expected.addPath(firstJoin, secondJoin, "xx");
    optStart = expected.addPath(INITIAL, "56");
    expected.addOptionalPath(optStart, firstJoin, "xx");

    assertEquivalent(actual, expected);
  }

  @Test
  public void testNoChangeIfNoCommonFactor() {
    NfaBuilder nfa = new NfaBuilder();
    nfa.addPath(INITIAL, TERMINAL, "12xxxxxx");
    // Add path "34xxx(xx)?" which, while it shares 'xxx' with '12xxxxxx', will not be factored
    // because splitting out 'xxx' would make the resulting regular expression longer
    // (e.g. "(?:34\d{2}?|12\d{3})\d{3}" is longer than "34\d{2}?\d{3}|12\d{6}").
    //
    // Note that there are some cases in which this isn't true (shorter sequences like 'x' might be
    // splittable without cost, but they are unlikely to ever make the expression shorter,
    // especially if they result in adding new parentheses for grouping.
    Node optStart = nfa.addPath(INITIAL, "34xxx");
    nfa.addOptionalPath(optStart, TERMINAL, "xx");

    ValueGraph<Node, SimpleEdge> actual = TrailingPathOptimizer.optimize(nfa.graph());
    assertEquivalent(actual, nfa);
  }

  private static void assertEquivalent(ValueGraph<Node, SimpleEdge> actual, NfaBuilder expected) {
    // This is a somewhat cheeky way to test graph isomorphism and relies on the fact that graph
    // flattening is deterministic according to how edges sort and doesn't care about node values.
    // It also, obviously, relies on the flattening code to be vaguely well tested.
    assertThat(NfaFlattener.flatten(actual)).isEqualTo(NfaFlattener.flatten(expected.graph()));
  }
}
