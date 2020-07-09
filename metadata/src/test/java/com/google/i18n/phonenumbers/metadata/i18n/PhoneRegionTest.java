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
package com.google.i18n.phonenumbers.metadata.i18n;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PhoneRegionTest {
  @Test
  public void testOrdering() {
    assertThat(Stream.of(r("US"), r("GB"), r("AE"), r("001"), r("KR"), r("MN")).sorted())
        .containsAtLeast(r("AE"), r("GB"), r("KR"), r("MN"), r("US"), r("001"))
        .inOrder();
  }

  @Test
  public void testWorld() {
    assertThat(PhoneRegion.getWorld()).isEqualTo(r("001"));
  }

  @Test
  public void testBadArgs() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> PhoneRegion.of("ABC")))
        .hasMessageThat()
        .contains("ABC");
    assertThat(assertThrows(IllegalArgumentException.class, () -> PhoneRegion.of("us")))
        .hasMessageThat()
        .contains("us");
    assertThat(assertThrows(IllegalArgumentException.class, () -> PhoneRegion.of("000")))
        .hasMessageThat()
        .contains("000");
  }

  private static PhoneRegion r(String cldrCode) {
    return PhoneRegion.of(cldrCode);
  }
}
