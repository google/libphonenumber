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

import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.RangeTree.empty;
import static com.google.i18n.phonenumbers.metadata.testing.RangeTreeSubject.assertThat;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrefixTreeTest {
  @Test
  public void testNewInstancesNormalized() {
    assertThat(prefixes("123", "1234")).containsExactly("123");
    assertThat(prefixes("70x", "7[1-9]")).containsExactly("7");
    // Regression test for b/68707522
    assertThat(prefixes("123xxx", "123x_xxx", "567xxx", "567x_xxx")).containsExactly("123", "567");
  }

  @Test
  public void testRetainFrom() {
    PrefixTree prefix = prefixes("123", "124", "126", "555");
    RangeTree ranges = ranges("1xxxxxx", "5xxxxxx", "6xxxxxx");
    assertThat(prefix.retainFrom(ranges)).containsExactly("12[346]xxxx", "555xxxx");
  }

  @Test
  public void testPrefixes() {
    PrefixTree prefix = prefixes("123", "124", "126", "555");
    assertThat(prefix.prefixes(seq("1230000"))).isTrue();
    assertThat(prefix.prefixes(seq("555000"))).isTrue();
    assertThat(prefix.prefixes(seq("12"))).isFalse();
    assertThat(prefix.prefixes(seq("120000"))).isFalse();
  }

  @Test
  public void testEmptyVsZeroLength() {
    PrefixTree empty = PrefixTree.from(empty());
    PrefixTree zeroLength = prefixes("xxx");

    assertThat(empty).isEmpty();
    assertThat(zeroLength).isNotEmpty();
    assertThat(zeroLength).hasSize(1);
    assertThat(zeroLength).containsExactly(RangeSpecification.empty());

    // While the empty prefix tree filters out everything, the zero length tree allows everything
    // to pass. This is because the zero length prefix tree represents a single prefix of length
    // zero and all digit sequences start with a zero length sub-sequence.
    RangeTree ranges = ranges("12x", "3xx", "456");
    assertThat(empty.retainFrom(ranges)).isEqualTo(empty());
    assertThat(zeroLength.retainFrom(ranges)).isEqualTo(ranges);
  }

  @Test
  public void testNoTrailingAnyPath() {
    assertThat(prefixes("123xxx", "456xx", "789x")).containsExactly("123", "456", "789");
  }

  @Test
  public void testRangeAndPrefixSameLength() {
    PrefixTree prefix = prefixes("1234");
    RangeTree ranges = ranges("xxxx");
    assertThat(prefix.retainFrom(ranges)).containsExactly("1234");
  }

  @Test
  public void testRangeShorterThanPrefix() {
    PrefixTree prefix = prefixes("1234");
    RangeTree ranges = ranges("xxx");
    assertThat(prefix.retainFrom(ranges)).isEmpty();
  }

  @Test
  public void testComplex() {
    PrefixTree prefix = prefixes("[12]", "3x4x5", "67890", "987xx9");
    RangeTree ranges = ranges("x", "xx", "xxx", "1234xx", "234xxx", "3xx8xx", "67890");
    assertThat(prefix.retainFrom(ranges))
        .containsExactly("[12]", "[12]x", "[12]xx", "67890", "1234xx", "234xxx", "3x485x");
  }

  @Test
  public void testEmptyPrefixTree() {
    // The empty filter filters everything out, since a filter operation is defined to return
    // only ranges which are prefixed by an element in the filter (of which there are none).
    assertThat(PrefixTree.from(empty()).retainFrom(ranges("12xxx"))).isEmpty();
  }

  @Test
  public void testZeroLengthPrefix() {
    // The non-empty prefix tree which contains a single prefix of zero length. This has no effect
    // as a filter, since all ranges "have a zero length prefix".
    PrefixTree prefix = PrefixTree.from(RangeTree.from(RangeSpecification.empty()));
    RangeTree input = ranges("12xxx");
    assertThat(prefix.retainFrom(input)).isEqualTo(input);
  }

  @Test
  public void testUnion() {
    // Overlapping prefixes retain the more general (shorter) one.
    assertThat(prefixes("1234").union(prefixes("12"))).containsExactly("12");
    // Indentical prefixes treated like normal union.
    assertThat(prefixes("12").union(prefixes("12"))).containsExactly("12");
    // Non-overlapping prefixes treated like normal union.
    assertThat(prefixes("123").union(prefixes("124"))).containsExactly("12[34]");
    // Complex case where prefixes are split into 2 lengths due to a partial overlap.
    assertThat(prefixes("1234", "45", "800").union(prefixes("12", "4x67")))
        .containsExactly("12", "45", "4[0-46-9]67", "800");
  }

  @Test
  public void testIntersection() {
    // Overlapping prefixes retain the more specific (longer) one.
    assertThat(prefixes("1234").intersect(prefixes("12"))).containsExactly("1234");
    // Indentical prefixes treated like normal intersection.
    assertThat(prefixes("12").intersect(prefixes("12"))).containsExactly("12");
    // Non-overlapping prefixes treated like normal intersection.
    assertThat(prefixes("123").intersect(prefixes("124"))).isEmpty();
    // Unlike the union case, with intersection, only the longest prefix remains.
    assertThat(prefixes("1234", "45x", "800").intersect(prefixes("12x", "4x67")))
        .containsExactly("1234", "4567");
  }

  @Test
  public void testTrim() {
    assertThat(prefixes("1234").trim(3)).containsExactly("123");
    assertThat(prefixes("12").trim(3)).containsExactly("12");
    assertThat(prefixes("1234").trim(0)).containsExactly(RangeSpecification.empty());
    // Trimming can result in prefixes shorter than the stated length if by collapsing the original
    // prefix tree you end up with trailing any digit sequences.
    assertThat(prefixes("12[0-4]5", "12[5-9]").trim(3)).containsExactly("12");
    assertThat(prefixes("7001", "70[1-9]", "7[1-9]").trim(3)).containsExactly("7");
  }

  @Test
  public void testMinimal() {
    // If there are no ranges to include, the minimal prefix is empty (matching nothing).
    assertThat(PrefixTree.minimal(RangeTree.empty(), ranges("123x"), 0)).isEmpty();

    // If the prefix for the included ranges is the identity, then the result is the identity
    // (after converting to a prefix, ranges like "xxx.." become the identity prefix).
    assertThat(PrefixTree.minimal(ranges("xxxx"), ranges("123"), 0).isIdentity()).isTrue();
    // Without an exclude set, the prefix returned (at zero length) can just accept everything.
    assertThat(PrefixTree.minimal(ranges("123x"), RangeTree.empty(), 0).isIdentity()).isTrue();

    assertThat(PrefixTree.minimal(ranges("123x", "456x"), ranges("13xx", "459x"), 0))
        .containsExactly("12", "456");

    assertThat(PrefixTree.minimal(ranges("123x", "456x"), empty(), 1)).containsExactly("[14]");
    assertThat(PrefixTree.minimal(ranges("123x", "456x"), empty(), 2)).containsExactly("12", "45");

    // Pick the shortest prefix when several suffice.
    assertThat(PrefixTree.minimal(ranges("12", "1234", "56"), ranges("1xx", "5xxx"), 0))
        .containsExactly("12", "56");
    assertThat(PrefixTree.minimal(ranges("12", "1234", "56"), ranges("1xx", "5xxx"), 3))
        .containsExactly("12", "56");

    // When ranges are contested, split the prefix (only "12" is contested out of "1[2-4]").
    assertThat(PrefixTree.minimal(ranges("1[2-4]5xx", "189xx"), ranges("128xx"), 0))
        .containsExactly("125", "1[348]");

    // If the include range already prefixes an entire path of the exclude set, ignore that path.
    // Here '12' (the shorter path) already captures '123', so '123' is ignored.
    assertThat(PrefixTree.minimal(ranges("12", "1234", "56"), ranges("123", "5xxx"), 0))
        .containsExactly("1", "56");
    // Now all exclude paths are ignored, so you get the "identity" prefix that catches everything.
    assertThat(PrefixTree.minimal(ranges("12", "1234", "56"), ranges("123", "5678"), 0))
        .containsExactly("");
  }

  @Test
  public void testMinimal_regression() {
    // This is extracted from a real case in which the old algorithm would fail for this case. The
    // "281xxxxxxx" path was necessary for failing since while visiting this, the old algorithm
    // became "confused" and added an additional "250" path to the minimal prefix, meaning that
    // the resulting range tree was "250", "250395". When this was turned into a prefix tree, the
    // shorter, early terminating, path took precedence and the result was (incorrectly) "250".
    assertThat(
        PrefixTree.minimal(
            ranges("250395xxxx"),
            ranges("250[24-9]xxxxxx", "2503[0-8]xxxxx", "25039[0-46-9]xxxx", "281xxxxxxx"),
            3))
        .containsExactly("250395");
  }

  private static DigitSequence seq(String s) {
    return DigitSequence.of(s);
  }

  private static PrefixTree prefixes(String... specs) {
    return PrefixTree.from(ranges(specs));
  }

  private static RangeTree ranges(String... specs) {
    return RangeTree.from(Arrays.stream(specs).map(RangeSpecification::parse));
  }
}
