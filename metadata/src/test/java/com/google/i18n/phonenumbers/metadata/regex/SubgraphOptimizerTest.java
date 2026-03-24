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
import static com.google.common.truth.Truth8.assertThat;

import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.regex.SubgroupOptimizer.LinkNodeVisitor;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SubgraphOptimizerTest {
  // The subgraph of "everything except 95, 96 and 100" (this appears in China leading digits).
  // Note that unlike China, there's also an early terminating '9' in the subgraph to ensure that
  // the entire subgraph is extracted (including teminating node).
  private static final RangeTree POSTGRAPH = ranges("[02-9]", "1[1-9]", "10[1-9]", "9[0-47-9]");

  // Some prefixes which come before the subgraph.
  private static final RangeTree PREGRAPH = ranges("123", "234", "345", "456", "567");

  // Cross product of pre and post paths.
  private static final RangeTree SUBGRAPH = RangeTree.from(
      PREGRAPH.asRangeSpecifications().stream()
          .flatMap(a -> POSTGRAPH.asRangeSpecifications().stream().map(a::extendBy)));

  // Additional paths which share edges in the subgraph and will cause repetition in regular
  // expressions. Also add a couple of early terminating paths "on the way to" the subgroup.
  // Note however that a terminating path that reaches the root of the subgraph (e.g. "123") will
  // cause a split in the DFA at the root node (one terminating, one not terminating).
  private static final RangeTree TEST_RANGES =
      SUBGRAPH.union(ranges("128xx", "238xx", "348xx", "458xx", "568xx", "12", "34"));

  @Test
  public void testSubgraphWeightAndInOrder() {
    LinkNodeVisitor v = new LinkNodeVisitor();
    TEST_RANGES.accept(v);
    DfaNode n = v.getHighestCostNode();
    assertThat(n).isNotNull();
    // 5 paths in PREGRAPH which reach the root of POSTGRAPH.
    assertThat(v.getInOrder(n)).isEqualTo(5);
    // 7 edges in POSTGRAPH with a total weight of 27:
    // "[02-8]" = 6, "1", "0", "9" = 3, 2 x "[1-9]" = 10, "[0-47-9]" = 8
    assertThat(v.getSubgraphWeight(n)).isEqualTo(27);
  }

  @Test
  public void testSubgraphExtraction() {
    Optional<RangeTree> extracted = SubgroupOptimizer.extractRepeatingSubgraph(TEST_RANGES);
    assertThat(extracted).hasValue(SUBGRAPH);
    // The "bridge" node is the same, so we extract the whole graph (so we return nothing).
    assertThat(SubgroupOptimizer.extractRepeatingSubgraph(SUBGRAPH)).isEmpty();
    // There's no repetition in this graph, so return nothing.
    assertThat(SubgroupOptimizer.extractRepeatingSubgraph(ranges("123", "234", "345"))).isEmpty();
  }

  private static RangeTree ranges(String... specs) {
    return RangeTree.from(Arrays.stream(specs).map(RangeSpecification::parse));
  }
}
