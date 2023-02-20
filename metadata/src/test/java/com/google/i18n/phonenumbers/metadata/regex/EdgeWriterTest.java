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

import com.google.common.base.Preconditions;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EdgeWriterTest {

  // Note that this code is tested very thoroughly by any "round-tripping" of regular expressions
  // in the metadata (i.e. generating regular expressions from DFAs and then re-parsing then to
  // ensure that the same DFA is produced). This is part of any acceptance test for generating
  // regular expressions and serves as a far more comprehensive stress test on the code. These
  // tests are thus limited to simpler cases and highlighting interesting behaviour.

  // The 'any digit' edge.
  private static final Edge X = e("x");

  @Test
  public void testSimple() {
    assertThat(regex(e("0"))).isEqualTo("0");
    assertThat(regex(e("[0-7]"))).isEqualTo("[0-7]");
    assertThat(regex(e("[0-9]"))).isEqualTo("\\d");
    assertThat(regex(X)).isEqualTo("\\d");
  }

  @Test
  public void testSequences() {
    assertThat(regex(seq(e("0"), e("1"), e("2")))).isEqualTo("012");
  }

  @Test
  public void testGroups() {
    // Non-optional groups spanning the top level don't need parentheses.
    assertThat(regex(or(e("0"), e("1"), e("2")))).isEqualTo("0|1|2");
    // Optional groups always need parentheses.
    assertThat(regex(opt(e("0"), e("1"), e("2")))).isEqualTo("(?:0|1|2)?");
    // Once a group has prefix or suffix, parentheses are needed.
    assertThat(regex(
        seq(
            or(e("0"), e("1")),
            e("2"))))
        .isEqualTo("(?:0|1)2");
  }

  @Test
  public void testNesting() {
    // Basic nesting is handled by a very straightforward edge visitor, so one non-trivial test
    // will cover all the basic cases ("any digit" sequences are a different matter however).
    assertThat(regex(
        seq(
            e("0"),
            or(
                e("1"),
                seq(
                    e("2"),
                    opt(e("3"), e("4")))),
            e("5"), e("6"))))
        .isEqualTo("0(?:1|2(?:3|4)?)56");
  }

  @Test
  public void testAnyDigitSequences() {
    // This is the complex part of efficient regular expression generation.
    assertThat(regex(seq(e("0"), e("1"), X))).isEqualTo("01\\d");
    // "\d\d" is shorter than "\d{2}"
    assertThat(regex(seq(X, X))).isEqualTo("\\d\\d");
    assertThat(regex(seq(X, X, X))).isEqualTo("\\d{3}");
    // Top level optional groups are supported.
    assertThat(regex(opt(seq(X, X)))).isEqualTo("(?:\\d{2})?");
    // Optional parts go at the end.
    assertThat(regex(
        seq(
            opt(seq(X, X)),
            X, X)))
        .isEqualTo("\\d\\d(?:\\d{2})?");
    // "(x(x(x)?)?)?"
    Edge anyGrp = opt(seq(
        X,
        opt(seq(
            X,
            opt(X)))));
    // The two cases of a group on its own or as part of a sequence are handled separately, so
    // must be tested separately.
    assertThat(regex(anyGrp)).isEqualTo("\\d{0,3}");
    assertThat(regex(seq(e("1"), e("2"), anyGrp))).isEqualTo("12\\d{0,3}");
    // xx(x(x(x)?)?)?"
    assertThat(regex(seq(X, X, anyGrp))).isEqualTo("\\d{2,5}");
    // Combining "any digit" groups produces minimal representation
    assertThat(regex(seq(anyGrp, anyGrp))).isEqualTo("\\d{0,6}");
  }

  // Helper to call standard version of regex generator (not using 'dot' for matching).
  private String regex(Edge e) {
    return EdgeWriter.toRegex(e, false /* use dot match */);
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

  // Creates a non-optional disjunction of edges.
  private static Edge or(Edge... edges) {
    List<Edge> e = Arrays.asList(edges);
    Preconditions.checkArgument(!e.contains(Edge.epsilon()), "use 'opt()' for optional groups");
    return Edge.disjunction(e);
  }

  // Creates an optional disjunction of edges.
  private static Edge opt(Edge... edges) {
    List<Edge> e = new ArrayList<>();
    e.addAll(Arrays.asList(edges));
    Preconditions.checkArgument(!e.contains(Edge.epsilon()), "don't pass epsilon directly");
    e.add(Edge.epsilon());
    return Edge.disjunction(e);
  }
}
