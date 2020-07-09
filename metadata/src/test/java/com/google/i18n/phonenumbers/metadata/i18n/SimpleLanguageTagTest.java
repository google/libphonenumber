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
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SimpleLanguageTagTest {
  @Test
  public void testSimple() {
    assertThat(SimpleLanguageTag.of("en").toString()).isEqualTo("en");
    assertThat(SimpleLanguageTag.of("zh_Hant").toString()).isEqualTo("zh-Hant");
  }

  @Test
  public void testBadArgs() {
    assertThat(assertThrows(IllegalArgumentException.class, () -> SimpleLanguageTag.of("x")))
        .hasMessageThat().contains("x");
    assertThat(assertThrows(IllegalArgumentException.class, () -> SimpleLanguageTag.of("EN")))
        .hasMessageThat().contains("EN");
    assertThat(assertThrows(IllegalArgumentException.class, () -> SimpleLanguageTag.of("003")))
        .hasMessageThat().contains("003");
  }
}
