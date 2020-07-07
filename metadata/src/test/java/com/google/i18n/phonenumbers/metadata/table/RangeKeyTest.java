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
package com.google.i18n.phonenumbers.metadata.table;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.PrefixTree;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RangeKeyTest {
  @Test
  public void testEmpty() {
    ImmutableList<RangeKey> keys = RangeKey.decompose(RangeTree.empty());
    assertThat(keys).isEmpty();
  }

  @Test
  public void testZeroLengthMatch() {
    ImmutableList<RangeKey> keys = RangeKey.decompose(RangeTree.from(RangeSpecification.empty()));
    assertThat(keys).containsExactly(key("", 0));
  }

  @Test
  public void testOnlyAnyPath() {
    ImmutableList<RangeKey> keys = RangeKey.decompose(ranges("xxx", "xxxx", "xxxxx"));
    assertThat(keys).containsExactly(key("", 3, 4, 5));
  }

  @Test
  public void testSimple() {
    ImmutableList<RangeKey> keys = RangeKey.decompose(ranges("123xxx", "123xxxx", "123xxxxx"));
    assertThat(keys).containsExactly(key("123", 6, 7, 8));
  }

  @Test
  public void testEmbeddedRanges() {
    ImmutableList<RangeKey> keys =
        RangeKey.decompose(ranges("1x", "1xx", "1xx23", "1xx23x", "1xx23xx"));
    assertThat(keys).containsExactly(key("1", 2, 3), key("1xx23", 5, 6, 7)).inOrder();
  }

  @Test
  public void testSplitFactors() {
    ImmutableList<RangeKey> keys = RangeKey.decompose(ranges("123xxxx", "1234x", "1234xx"));
    // If the input wasn't "factored" first, this would result in:
    // key("123[0-35-9]", 7), key("1234", 5, 6, 7)
    assertThat(keys).containsExactly(key("123", 7), key("1234", 5, 6)).inOrder();
  }

  @Test
  public void testMergeStrategy() {
    ImmutableList<RangeKey> keys = RangeKey.decompose(ranges("12[0-4]xxx", "12xxx", "12xx"));
    // The merge strategy for factorizing the tree will prefer to keep the longer paths intact
    // and split shorter paths around it. Using the other strategy we would get:
    // key("12", 4, 5), key("12[0-4]", 6)
    assertThat(keys).containsExactly(key("12[0-4]", 4, 5, 6), key("12[5-9]", 4, 5)).inOrder();
  }

  @Test
  public void testAsRangeSpecifications() {
    assertThat(key("", 3, 4, 5).asRangeSpecifications())
        .containsExactly(spec("xxx"), spec("xxxx"), spec("xxxxx")).inOrder();
    assertThat(key("1[2-4]", 3, 4, 5).asRangeSpecifications())
        .containsExactly(spec("1[2-4]x"), spec("1[2-4]xx"), spec("1[2-4]xxx")).inOrder();
    assertThat(key("1x[468]", 3, 5, 7).asRangeSpecifications())
        .containsExactly(spec("1x[468]"), spec("1x[468]xx"), spec("1x[468]xxxx")).inOrder();
  }

  @Test
  public void testSimpleRealWorldData() {
    // From ITU German numbering plan, first few fixed line ranges.
    PrefixTree prefixes =
        PrefixTree.from(ranges("20[1-389]", "204[135]", "205[1-468]", "206[4-6]", "20[89]"));
    RangeTree ranges = prefixes.retainFrom(
        ranges("xxxxxx", "xxxxxxx", "xxxxxxxx", "xxxxxxxxx", "xxxxxxxxxx", "xxxxxxxxxxx"));
    ImmutableList<RangeKey> keys = RangeKey.decompose(ranges);
    assertThat(keys).containsExactly(
            key("20[1-389]", 6, 7, 8, 9, 10, 11),
            key("204[135]", 6, 7, 8, 9, 10, 11),
            key("205[1-468]", 6, 7, 8, 9, 10, 11),
            key("206[4-6]", 6, 7, 8, 9, 10, 11))
        .inOrder();
  }

  @Test
  public void testContains() {
    RangeKey key = key("1[23]", 7, 8, 9);
    assertThat(key.contains(digitSequence("12"), 8)).isTrue();
    assertThat(key.contains(digitSequence("12"), 10)).isFalse();
    assertThat(key.contains(digitSequence("7"), 8)).isFalse();
  }

  private static RangeKey key(String spec, Integer... lengths) {
    RangeSpecification prefix =
        spec.isEmpty() ? RangeSpecification.empty() : RangeSpecification.parse(spec);
    return RangeKey.create(prefix, ImmutableSet.copyOf(lengths));
  }

  private static RangeTree ranges(String... spec) {
    return RangeTree.from(Stream.of(spec).map(RangeSpecification::parse));
  }

  private static RangeSpecification spec(String spec) {
    return RangeSpecification.parse(spec);
  }

  private static DigitSequence digitSequence(String spec) {
    return DigitSequence.of(spec);
  }
}
