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
package com.google.i18n.phonenumbers.metadata;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.RangeTree.empty;
import static com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer.MergeStrategy.ALLOW_EDGE_SPLITTING;
import static com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer.MergeStrategy.REQUIRE_EQUAL_EDGES;
import static com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer.factor;

import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RangeTreeFactorizerTest {
  @Test
  public void testEmpty() {
    assertThat(factor(empty(), REQUIRE_EQUAL_EDGES)).isEmpty();
    assertThat(factor(empty(), ALLOW_EDGE_SPLITTING)).isEmpty();
  }

  @Test
  public void testSimplePrefix() {
    RangeTree t = ranges("123x", "123xx", "123xxx");
    assertThat(factor(t, REQUIRE_EQUAL_EDGES)).containsExactly(t);
    assertThat(factor(t, ALLOW_EDGE_SPLITTING)).containsExactly(t);
  }

  @Test
  public void testDisjointBranchesNotFactored() {
    RangeTree t = ranges("123xxx", "124xx", "125x");
    assertThat(factor(t, REQUIRE_EQUAL_EDGES)).containsExactly(t);
    assertThat(factor(t, ALLOW_EDGE_SPLITTING)).containsExactly(t);
  }

  @Test
  public void testOverlappingBranchesAreFactored() {
    RangeTree t = ranges("123xxx", "1234x", "1234", "123");
    assertThat(factor(t, REQUIRE_EQUAL_EDGES))
        .containsExactly(ranges("123xxx", "123"), ranges("1234x", "1234"))
        .inOrder();
    assertThat(factor(t, ALLOW_EDGE_SPLITTING))
        .containsExactly(ranges("123xxx", "123"), ranges("1234x", "1234"))
        .inOrder();
  }

  @Test
  public void testStrategyDifference() {
    // When factoring with REQUIRE_EQUAL_EDGES the [3-9] edge in the shorter path cannot be merged
    // into the longer path of the first factor, since [3-4] already exists and is not equal to
    // [3-9]. However since [3-4] is contained by [3-9], when we ALLOW_EDGE_SPLITTING, we can split
    // the edge we are trying to merge to add paths for both [3-4] and [5-9]. This isn't always a
    // win for regular expression length, and in fact for the most complex cases,
    // REQUIRE_EQUAL_EDGES often ends up smaller.
    RangeTree splittable = ranges("12[3-5]xx", "12[3-9]x");
    assertThat(factor(splittable, REQUIRE_EQUAL_EDGES))
        .containsExactly(ranges("12[3-5]xx"), ranges("12[3-9]x"))
        .inOrder();
    assertThat(factor(splittable, ALLOW_EDGE_SPLITTING))
        .containsExactly(ranges("12[3-5]xx", "12[3-9]x"));

    // In this case, the [3-5] edge in the first factor in only a partial overlap with the [4-9]
    // edge we are trying to merge in. Now both strategies will prefer to treat the shorter path
    // as a separate factor, since there's no clean way to merge into the existing edge.
    RangeTree unsplittable = ranges("12[3-5]xx", "12[4-9]x");
    assertThat(factor(unsplittable, REQUIRE_EQUAL_EDGES))
        .containsExactly(ranges("12[3-5]xx"), ranges("12[4-9]x"))
        .inOrder();
    assertThat(factor(unsplittable, ALLOW_EDGE_SPLITTING))
        .containsExactly(ranges("12[3-5]xx"), ranges("12[4-9]x"))
        .inOrder();

    // TODO: Find a non-complex example where REQUIRE_EQUAL_EDGES yeilds smaller regex.
    // Approximately 50 out of the 1000+ regex's in the XML get smaller with REQUIRE_EQUAL_EDGES.
  }

  RangeTree ranges(String... s) {
    return RangeTree.from(specs(s));
  }

  List<RangeSpecification> specs(String... s) {
    return Stream.of(s).map(RangeSpecification::parse).collect(toImmutableList());
  }
}
