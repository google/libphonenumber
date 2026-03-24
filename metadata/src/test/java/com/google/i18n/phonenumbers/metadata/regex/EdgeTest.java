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
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import com.google.i18n.phonenumbers.metadata.regex.Edge.Visitor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EdgeTest {
  @Test
  public void testSimple() {
    assertThat(Edge.fromMask(0x6).getDigitMask()).isEqualTo(0x6);
    assertThat(Edge.fromMask(0x6).isOptional()).isFalse();

    assertThat(Edge.fromMask(0x3).toString()).isEqualTo("[01]");  // 0000000011
    assertThat(Edge.fromMask(0x300).toString()).isEqualTo("[89]");  // 1100000000
    assertThat(Edge.fromMask(0x1FE).toString()).isEqualTo("[1-8]");  // 0111111110
    assertThat(Edge.fromMask(ALL_DIGITS_MASK).toString()).isEqualTo("x");  // any digit
  }

  @Test
  public void testAny() {
    assertThat(Edge.fromMask(ALL_DIGITS_MASK)).isEqualTo(Edge.any());
    assertThat(Edge.any().optional()).isEqualTo(Edge.optionalAny());

    assertThat(Edge.any().toString()).isEqualTo("x");
    // Unlike AnyPath, simple edges are not sequences, so don't need parens for optional.
    assertThat(Edge.optionalAny().toString()).isEqualTo("x?");
  }

  @Test
  public void testEpsilon() {
    // Epsilon isn't optional, it represents a path that non-optionally accepts no input.
    assertThat(Edge.epsilon().isOptional()).isFalse();
    assertThat(Edge.epsilon().toString()).isEqualTo("e");
  }

  @Test
  public void testConcatenation() {
    Edge concatenated = Edge.concatenation(Edge.fromMask(0x3), Edge.any());
    assertThat(concatenated.toString()).isEqualTo("[01]x");
    TestingVisitor v = new TestingVisitor() {
      @Override
      public void visitSequence(List<Edge> edges) {
        assertThat(edges).containsExactly(Edge.fromMask(0x3), Edge.any()).inOrder();
        wasTested = true;
      }
    };
    concatenated.accept(v);
    assertThat(v.wasTested).isTrue();
  }

  @Test
  public void testGroup() {
    Edge group = Edge.disjunction(ImmutableSet.of(Edge.fromMask(0x3), Edge.any()));
    TestingVisitor v = new TestingVisitor() {
      @Override
      public void visitGroup(Set<Edge> edges, boolean isOptional) {
        assertThat(edges).containsExactly(Edge.any(), Edge.fromMask(0x3)).inOrder();
        assertThat(isOptional).isFalse();
        wasTested = true;
      }
    };
    group.accept(v);
    assertThat(group.toString()).isEqualTo("(x|[01])");
    assertThat(v.wasTested).isTrue();
  }

  @Test
  public void testOptionalGroup() {
    Edge group = Edge.disjunction(ImmutableSet.of(Edge.fromMask(0x3), Edge.epsilon(), Edge.any()));
    TestingVisitor v = new TestingVisitor() {
      @Override
      public void visitGroup(Set<Edge> edges, boolean isOptional) {
        // Reordered and epsilon removed.
        assertThat(edges).containsExactly(Edge.any(), Edge.fromMask(0x3)).inOrder();
        assertThat(isOptional).isTrue();
        wasTested = true;
      }
    };
    group.accept(v);
    assertThat(group.toString()).isEqualTo("(x|[01])?");
    assertThat(v.wasTested).isTrue();
  }

  @Test
  public void testOrdering() {
    // Testing ordering is important because when generating regular expressions, the edge order
    // defines a lot about the visual order of the final regular expression. This order should be
    // as close to "what a person would consider reasonable" as possible. In fact some of the cases
    // tested here will never occur in real situations (e.g. sequences compared with groups)
    // because of the way composite edges are created. However it seems sensible to test the
    // behaviour nevertheless.

    // Simple Edges

    assertSameOrder(e("0"), e("0"));
    // "0" < "1" - lowest bit set wins
    assertOrdered(e("0"), e("1"));
    // "[01]" < "1" - lowest bit set wins
    assertOrdered(e("[01]"), e("1"));
    // "x" < "9" - lowest bit set wins
    assertOrdered(X, e("9"));

    // Sequences

    // ("0x" < "1") and ("0" < "1x") - first edge in sequence is compared to single edge.
    assertOrdered(seq(e("0"), X), e("1"));
    assertOrdered(e("0"), seq(e("1"), X));
    // "[01]" < "[01]x" - single edges are "smaller" than sequences of edges if all else is equal.
    assertOrdered(e("[01]"), seq(e("[01]"), X));

    // "[01]x" == "[01]x"
    assertSameOrder(seq(e("[01]"), X), seq(e("[01]"), X));
    // "x1" < "x2" - comparing 2 sequences compares all edges.
    assertOrdered(seq(X, e("1")), seq(X, e("2")));

    // "[01]x" < "[01]xx" - shortest sequence wins in tie break (similar to how "[01]" < "[01]x")
    assertOrdered(seq(e("[01]"), X), seq(e("[01]"), X, X));

    // Disjunctions

    // "(1|2)" == "(2|1)" - edges are sorted when creating disjunctions
    assertSameOrder(or(e("1"), e("2")), or(e("2"), e("1")));
    // "(1|2|3)" < "(1|2|4)" - comparing 2 disjunctions compares all edges.
    assertOrdered(or(e("1"), e("2"), e("3")), or(e("1"), e("2"), e("4")));
    // "(1|2)" < "(1|2|3)" - shortest sequence wins in tie break
    assertOrdered(or(e("1"), e("2")), or(e("1"), e("2"), e("3")));

    // Miscellaneous

    // "1" < "(1|2)" - if first edge matches, single edges sort before groups.
    assertOrdered(e("1"), or(e("1"), e("2")));

    // "(1|x)" < "1x" - because "(1|x)" is actually "(x|1)" and "x" < "1".
    assertOrdered(or(e("1"), X), seq(e("1"), X));
  }

  private static void assertSameOrder(Edge lhs, Edge rhs) {
    assertThat(lhs).isEquivalentAccordingToCompareTo(rhs);
    assertThat(lhs).isEqualTo(rhs);
  }

  private static void assertOrdered(Edge lhs, Edge rhs) {
    assertThat(lhs).isNotEqualTo(rhs);
    assertThat(lhs).isLessThan(rhs);
    assertThat(rhs).isGreaterThan(lhs);
  }

  // A bit like a mock, but not really "mocking" existing behaviour.
  private static class TestingVisitor implements Visitor {
    // Set this in overridden method(s).
    protected boolean wasTested = false;

    @Override
    public void visit(SimpleEdge edge) {
      fail("unexpected call");
    }

    @Override
    public void visitSequence(List<Edge> edges) {
      fail("unexpected call");
    }

    @Override
    public void visitGroup(Set<Edge> edges, boolean isOptional) {
      fail("unexpected call");
    }
  }

  // The 'any digit' edge.
  private static final Edge X = e("x");

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
}
