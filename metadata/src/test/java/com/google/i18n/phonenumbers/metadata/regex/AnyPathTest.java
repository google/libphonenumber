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
import static com.google.i18n.phonenumbers.metadata.regex.AnyPath.EMPTY;
import static com.google.i18n.phonenumbers.metadata.regex.AnyPath.OPTIONAL;
import static com.google.i18n.phonenumbers.metadata.regex.AnyPath.SINGLE;

import com.google.common.collect.ImmutableSortedSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AnyPathTest {
  @Test
  public void testConstants() {
    assertPath(EMPTY, 0);
    assertPath(SINGLE, 1);
    assertPath(OPTIONAL, 0, 1);
  }

  @Test
  public void testExtend() {
    assertThat(EMPTY.extend(false)).isEqualTo(SINGLE);
    assertThat(EMPTY.extend(true)).isEqualTo(OPTIONAL);
    // Non-optional extension is the same as joining with SINGLE.
    assertPath(SINGLE.extend(false), 2);
    // This is not the same as joining SINGLE.join(OPTIONAL).
    assertPath(SINGLE.extend(true), 0, 2);

    // 100 extends to 1000 or 1001 (if optional).
    assertPath(AnyPath.of(0x4).extend(false), 3);
    assertPath(AnyPath.of(0x4).extend(true), 0, 3);
  }

  @Test
  public void testJoin() {
    assertThat(EMPTY.join(SINGLE)).isEqualTo(SINGLE);
    assertThat(EMPTY.join(OPTIONAL)).isEqualTo(OPTIONAL);
    assertPath(SINGLE.join(SINGLE), 2);
    assertPath(SINGLE.join(OPTIONAL), 1, 2);
    assertPath(OPTIONAL.join(OPTIONAL), 0, 1, 2);

    // "(x(x)?)?" == 110 and matches 0 to 2.
    // "(x(x)?)?".join("(x(x)?)?") == "(x(x(x(x)?)?)?)?" == 11111 and matches 0 to 4.
    assertThat(AnyPath.of(0x7).join(AnyPath.of(0x7))).isEqualTo(AnyPath.of(0x1F));

    // "xx(x)?" == 1100 and matches 2 or 3.
    // "(xx)?" == 0101 and matches 0 or 2.
    // "xx(x)?".join("(xx)?") == "xx(xx)?" == 111100 and matches 2 to 5.
    assertThat(AnyPath.of(0xC).join(AnyPath.of(0x5))).isEqualTo(AnyPath.of(0x3C));
  }

  @Test
  public void testMakeOptional() {
    assertThat(OPTIONAL.makeOptional()).isEqualTo(OPTIONAL);
    assertThat(SINGLE.makeOptional()).isEqualTo(OPTIONAL);
    assertPath(AnyPath.of(0x4).makeOptional(), 0, 2);
  }

  @Test
  public void testToString() {
    assertThat(SINGLE.toString()).isEqualTo("x");
    assertThat(OPTIONAL.toString()).isEqualTo("(x)?");
    assertThat(AnyPath.of(0x8).toString()).isEqualTo("xxx");  // 1000 = 3 digits
    assertThat(AnyPath.of(0xA).toString()).isEqualTo("x(xx)?");  // 1010 = 1 or 3 digits
    assertThat(AnyPath.of(0xF).toString()).isEqualTo("(x(x(x)?)?)?");  // 1111 = 0 to 3 digits
  }

  // Ordering is important as we need to find the shortest path at certain times.
  @Test
  public void testOrdering() {
    assertThat(SINGLE).isGreaterThan(EMPTY);
    assertThat(OPTIONAL).isGreaterThan(SINGLE);

    assertThat(AnyPath.of(0x8)).isGreaterThan(AnyPath.of(0x4));
    // Same length, but the 2nd highest length match is taken into account as a tie break.
    // This strategy turns out to match numeric comparison perfectly since set-bits are lengths.
    assertThat(AnyPath.of(0xA)).isGreaterThan(AnyPath.of(0x9));
  }

  private static void assertPath(AnyPath p, Integer... n) {
    ImmutableSortedSet<Integer> lengths = ImmutableSortedSet.copyOf(n);
    int maxLength = lengths.last();
    assertThat(p.maxLength()).isEqualTo(maxLength);
    for (int i = 0; i <= maxLength; i++) {
      assertThat(p.acceptsLength(i)).isEqualTo(lengths.contains(i));
    }
  }
}
