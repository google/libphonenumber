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
import static com.google.i18n.phonenumbers.metadata.DigitSequence.domain;
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.parse;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.truth.Truth;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RangeSpecificationTest {
  @Test
  public void testParse() {
    assertThat(parse("")).isSameInstanceAs(RangeSpecification.empty());
    assertThat(parse("0").toString()).isEqualTo("0");
    assertThat(parse("0").length()).isEqualTo(1);
    assertThat(parse("01234").toString()).isEqualTo("01234");
    assertThat(parse("01234").length()).isEqualTo(5);
    assertThat(parse("012[0-9]").toString()).isEqualTo("012x");
    assertThat(parse("012[0234789]xxx").toString()).isEqualTo("012[02-47-9]xxx");
    assertThat(parse("0_1_2").toString()).isEqualTo("012");
    assertThat(parse("0_12[3-8]_xxx_xxx").toString()).isEqualTo("012[3-8]xxxxxx");
  }

  @Test
  public void testParseBad() {
    assertThrows(NullPointerException.class, () -> parse(null));
    assertThrows(IllegalArgumentException.class, () -> parse("#"));
    assertThrows(IllegalArgumentException.class, () -> parse("["));
    assertThrows(IllegalArgumentException.class, () -> parse("[]"));
    assertThrows(IllegalArgumentException.class, () -> parse("[0-"));
    assertThrows(IllegalArgumentException.class, () -> parse("[0-]"));
    assertThrows(IllegalArgumentException.class, () -> parse("[0--9]"));
    assertThrows(IllegalArgumentException.class, () -> parse("[0..9]"));
    assertThrows(IllegalArgumentException.class, () -> parse("[33]"));
    assertThrows(IllegalArgumentException.class, () -> parse("[32]"));
    assertThrows(IllegalArgumentException.class, () -> parse("[3-3]"));
    assertThrows(IllegalArgumentException.class, () -> parse("[3-2]"));
    assertThrows(IllegalArgumentException.class, () -> parse("123[9-0]456"));
    assertThrows(IllegalArgumentException.class, () -> parse("1234_"));
    assertThrows(IllegalArgumentException.class, () -> parse("_1234"));
    assertThrows(IllegalArgumentException.class, () -> parse("12__34"));
    assertThrows(IllegalArgumentException.class, () -> parse("1[2_4]5"));
  }

  @Test
  public void testSingleton() {
    assertThat(RangeSpecification.singleton(asList(0, 1, 2, 4, 5, 7, 8, 9)))
        .isEqualTo(parse("[0-2457-9]"));
  }

  @Test
  public void testMatches() {
    assertThat(RangeSpecification.empty().matches(DigitSequence.empty())).isTrue();

    assertAllMatch(parse("0"), "0");
    assertNoneMatch(parse("0"), "00", "1");

    assertAllMatch(parse("01234"), "01234");
    assertNoneMatch(parse("01234"), "01233", "01235");

    assertAllMatch(parse("012x"), "0120", "0125", "0129");
    assertNoneMatch(parse("012x"), "012", "0119", "0130", "01200");

    assertAllMatch(parse("012[3-689]xxx"), "0124000", "0128999");
    assertNoneMatch(parse("012[3-689]xxx"), "0122000", "0127999");
  }

  @Test
  public void testMinMax() {
    assertThat(parse("123xxx").min()).isEqualTo(DigitSequence.of("123000"));
    assertThat(parse("123xxx").max()).isEqualTo(DigitSequence.of("123999"));
    assertThat(parse("1x[2-3]x4").min()).isEqualTo(DigitSequence.of("10204"));
    assertThat(parse("1x[2-3]x4").max()).isEqualTo(DigitSequence.of("19394"));
  }

  @Test
  public void testSequenceCount() {
    assertThat(RangeSpecification.empty().getSequenceCount()).isEqualTo(1);
    assertThat(parse("1xx").getSequenceCount()).isEqualTo(100);
    assertThat(parse("1[2-46-8]x").getSequenceCount()).isEqualTo(60);
    assertThat(parse("1xx[0-27-9]").getSequenceCount()).isEqualTo(600);
  }

  @Test
  public void testFrom() {
    assertThat(RangeSpecification.from(DigitSequence.empty()))
        .isEqualTo(RangeSpecification.empty());
    assertThat(RangeSpecification.from(DigitSequence.of("1"))).isEqualTo(parse("1"));
    assertThat(RangeSpecification.from(DigitSequence.of("1234"))).isEqualTo(parse("1234"));
  }

  @Test
  public void testAny() {
    assertThat(RangeSpecification.any(0)).isEqualTo(RangeSpecification.empty());
    assertThat(RangeSpecification.any(2)).isEqualTo(parse("xx"));
    assertThat(RangeSpecification.any(10)).isEqualTo(parse("xxxxxxxxxx"));
    assertThrows(IllegalArgumentException.class, () -> RangeSpecification.any(-1));
    assertThrows(IllegalArgumentException.class, () -> RangeSpecification.any(19));
  }

  @Test
  public void testFirst() {
    RangeSpecification spec = parse("123[4-7]xxxx");
    assertThat(spec.first(3)).isEqualTo(parse("123"));
    assertThat(spec.first(6)).isEqualTo(parse("123[4-7]xx"));
    assertThat(spec.first(spec.length())).isSameInstanceAs(spec);
    assertThat(spec.first(100)).isSameInstanceAs(spec);
    assertThat(spec.first(0)).isEqualTo(RangeSpecification.empty());
    assertThrows(IllegalArgumentException.class, () -> spec.first(-1));
  }

  @Test
  public void testLast() {
    RangeSpecification spec = parse("123[4-7]xxxx");
    assertThat(spec.last(3)).isEqualTo(parse("xxx"));
    assertThat(spec.last(6)).isEqualTo(parse("3[4-7]xxxx"));
    assertThat(spec.last(spec.length())).isSameInstanceAs(spec);
    assertThat(spec.last(100)).isSameInstanceAs(spec);
    assertThat(spec.last(0)).isEqualTo(RangeSpecification.empty());
    assertThrows(IllegalArgumentException.class, () -> spec.last(-1));
  }

  @Test
  public void testGetPrefix() {
    assertThat(RangeSpecification.empty().getPrefix()).isEqualTo(RangeSpecification.empty());
    assertThat(parse("xxxx").getPrefix()).isEqualTo(RangeSpecification.empty());
    assertThat(parse("xx1x").getPrefix()).isEqualTo(parse("xx1"));
    assertThat(parse("123[4-7]xxxx").getPrefix()).isEqualTo(parse("123[4-7]"));
  }

  @Test
  public void testOrdering_simple() {
    // For specifications representing a single DigitSequence, the ordering should be the same.
    testComparator(
        RangeSpecification.empty(),
        parse("0"),
        parse("00"),
        parse("000"),
        parse("01"),
        parse("1"),
        parse("10"),
        parse("123"),
        parse("124"),
        parse("4111"),
        parse("4200"),
        parse("4555"),
        parse("9"),
        parse("99"),
        parse("999"));
  }

  @Test
  public void testOrdering_disjoint() {
    // NOT the same as using the min() sequence for ordering (since "4555" > "4200" > "4111").
    testComparator(
        parse("12xx"),
        parse("13xx"),
        parse("14xx"),
        parse("1[5-8]00"),
        parse("[2-3]xxx"),
        parse("[4-6]555"),
        parse("[45]111"),
        parse("[45]2xx"),
        parse("4999"));
  }

  @Test
  public void testOrdering_overlapping() {
    // Ordering for overlapping ranges is well defined but not particularly intuitive.
    testComparator(
        parse("01xxx"),
        parse("01xx[0-5]"),
        parse("01x0[0-5]"),
        parse("01x00"),
        parse("01[0-6]00"),
        parse("01[2-7]xx"),
        parse("01[2-7]00"),
        parse("01[2-7]67"),
        parse("01[4-9]00"));
  }

  @Test
  public void testToString() {
    assertThat(parse("0").toString()).isEqualTo("0");
    assertThat(parse("01234").toString()).isEqualTo("01234");
    assertThat(parse("012[3-4]").toString()).isEqualTo("012[34]");
    assertThat(parse("012[0-9]").toString()).isEqualTo("012x");
    assertThat(parse("012[3-689]xxx").toString()).isEqualTo("012[3-689]xxx");
  }

  @Test
  public void testBitmaskToString() {
    assertThat(RangeSpecification.toString(1 << 0)).isEqualTo("0");
    assertThat(RangeSpecification.toString(1 << 9)).isEqualTo("9");
    assertThat(RangeSpecification.toString(0xF)).isEqualTo("[0-3]");
    assertThat(RangeSpecification.toString(0xF1)).isEqualTo("[04-7]");
    assertThat(RangeSpecification.toString(ALL_DIGITS_MASK)).isEqualTo("x");

    assertThrows(IllegalArgumentException.class, () -> RangeSpecification.toString(0));
    assertThrows(IllegalArgumentException.class, () -> RangeSpecification.toString(0x400));
  }

  @Test
  public void testRangeProcessing_singleBlock() {
    Truth.assertThat(RangeSpecification.from(setOf(range("1200", "1299"))))
        .isEqualTo(specs("12xx"));
  }

  @Test
  public void testRangeProcessing_fullRange() {
    Truth.assertThat(RangeSpecification.from(setOf(range("0000", "9999"))))
        .isEqualTo(specs("xxxx"));
  }

  @Test
  public void testRangeProcessing_edgeCases() {
    Truth.assertThat(RangeSpecification.from(setOf(range("1199", "1300")))).isEqualTo(specs(
        "1199",
        "12xx",
        "1300"));
  }

  @Test
  public void testRangeProcessing_complex() {
    Truth.assertThat(RangeSpecification.from(setOf(range("123", "45678")))).isEqualTo(specs(
        "12[3-9]",
        "1[3-9]x",
        "[2-9]xx",
        "xxxx",
        "[0-3]xxxx",
        "4[0-4]xxx",
        "45[0-5]xx",
        "456[0-6]x",
        "4567[0-8]"));
  }

  @Test
  public void testAsRanges_edgeCase() {
    // The middle 2 ranges abut.
    assertThat(RangeSpecification.parse("12[34][0189]x").asRanges())
        .containsExactly(range("12300", "12319"), range("12380", "12419"), range("12480", "12499"))
        .inOrder();
  }

  private static void assertAllMatch(RangeSpecification r, String... sequences) {
    for (String digits : sequences) {
      assertThat(r.matches(DigitSequence.of(digits))).isTrue();
    }
  }

  private static void assertNoneMatch(RangeSpecification r, String... sequences) {
    for (String digits : sequences) {
      assertThat(r.matches(DigitSequence.of(digits))).isFalse();
    }
  }

  List<RangeSpecification> specs(String... s) {
    return Stream.of(s).map(RangeSpecification::parse).collect(toImmutableList());
  }

  private static Range<DigitSequence> range(String lo, String hi) {
    return Range.closed(DigitSequence.of(lo), DigitSequence.of(hi)).canonical(domain());
  }

  private static RangeSet<DigitSequence> setOf(Range<DigitSequence>... r) {
    return ImmutableRangeSet.copyOf(Arrays.asList(r));
  }

  private static <T extends Comparable<T>> void testComparator(T... items) {
    for (int i = 0; i < items.length; i++) {
      assertThat(items[i]).isEqualTo(items[i]);
      assertThat(items[i]).isEquivalentAccordingToCompareTo(items[i]);
      for (int j = i + 1; j < items.length; j++) {
        assertThat(items[i]).isNotEqualTo(items[j]);
        assertThat(items[i]).isLessThan(items[j]);
        assertThat(items[j]).isGreaterThan(items[i]);
      }
    }
  }
}
