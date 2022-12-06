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
import static com.google.i18n.phonenumbers.metadata.DigitSequence.domain;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DigitSequenceTest {

  @Test
  public void testEmpty() {
    Object e = DigitSequence.of("");
    assertThat(e).isSameInstanceAs(DigitSequence.empty());
    assertThat(DigitSequence.empty().length()).isEqualTo(0);
    assertThrows(IndexOutOfBoundsException.class, () -> DigitSequence.empty().getDigit(0));
    assertThat(DigitSequence.empty().toString()).isEqualTo("");
  }

  @Test
  public void testCreate() {
    DigitSequence s = DigitSequence.of("0123456789");
    assertThat(s).isEqualTo(DigitSequence.of("0123456789"));
    assertThat(s).isNotEqualTo(DigitSequence.of("1111111111"));
  }

  @Test
  public void testGetDigit() {
    DigitSequence s = DigitSequence.of("0123456789");
    assertThat(s.length()).isEqualTo(10);
    for (int n = 0; n < s.length(); n++) {
      assertThat(s.getDigit(n)).isEqualTo(n);
    }
    assertThat(s.toString()).isEqualTo("0123456789");
  }

  @Test
  public void testBadArguments() {
    assertThrows(NullPointerException.class, () -> DigitSequence.of(null));
    assertThrows(IllegalArgumentException.class, () -> DigitSequence.of("123X"));
    // Too long (19 digits).
    assertThrows(IllegalArgumentException.class, () -> DigitSequence.of("1234567890123456789"));
  }

  @Test
  public void testMin() {
    assertThat(domain().minValue()).isEqualTo(DigitSequence.empty());
    assertThat(domain().next(DigitSequence.empty())).isNotNull();
    assertThat(domain().previous(DigitSequence.empty())).isNull();
  }

  @Test
  public void testMax() {
    DigitSequence max = DigitSequence.of("999999999999999999");
    assertThat(domain().maxValue()).isEqualTo(max);
    assertThat(domain().previous(max)).isNotNull();
    assertThat(domain().next(max)).isNull();
  }

  @Test
  public void testDistance() {
    assertThat(domain().distance(DigitSequence.empty(), DigitSequence.of("0")))
        .isEqualTo(1);
    assertThat(domain().distance(DigitSequence.of("0"), DigitSequence.of("1")))
        .isEqualTo(1);
    assertThat(domain().distance(DigitSequence.of("0"), DigitSequence.of("00")))
        .isEqualTo(10);
    assertThat(domain().distance(DigitSequence.of("0"), DigitSequence.of("10")))
        .isEqualTo(20);
    assertThat(domain().distance(DigitSequence.of("10"), DigitSequence.of("0")))
        .isEqualTo(-20);
    assertThat(domain().distance(DigitSequence.empty(), DigitSequence.of("000000")))
        .isEqualTo(111111);
    assertThat(domain().distance(DigitSequence.of("000"), DigitSequence.of("000000")))
        .isEqualTo(111000);
    // Max distance is one less than the total number of digit sequences.
    assertThat(domain().distance(domain().minValue(), domain().maxValue()))
        .isEqualTo(1111111111111111110L);
  }

  @Test
  public void testLexicographicalOrdering() {
    testComparator(
        DigitSequence.empty(),
        DigitSequence.of("0"),
        DigitSequence.of("1"),
        DigitSequence.of("9"),
        DigitSequence.of("00"),
        DigitSequence.of("01"),
        DigitSequence.of("10"),
        DigitSequence.of("99"),
        DigitSequence.of("000"),
        DigitSequence.of("123"),
        DigitSequence.of("124"),
        DigitSequence.of("999"));
  }

  @Test
  public void testExtend() {
    assertThat(DigitSequence.empty().extendBy(0)).isEqualTo(DigitSequence.of("0"));
    assertThat(DigitSequence.of("1234").extendBy(DigitSequence.of("5678")))
        .isEqualTo(DigitSequence.of("12345678"));
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
