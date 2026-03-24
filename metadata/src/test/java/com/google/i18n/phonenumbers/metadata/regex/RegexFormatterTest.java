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
import static com.google.i18n.phonenumbers.metadata.regex.RegexFormatter.FormatOption.FORCE_CAPTURING_GROUPS;
import static com.google.i18n.phonenumbers.metadata.regex.RegexFormatter.FormatOption.FORCE_NON_CAPTURING_GROUPS;
import static com.google.i18n.phonenumbers.metadata.regex.RegexFormatter.FormatOption.PRESERVE_CAPTURING_GROUPS;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RegexFormatterTest {

  // Luckily the formatter cares only about 3 special characters, '(', '|' and ')', so we only need
  // to test a few very straightforward cases to cover everything.

  @Test
  public void testSimple() {
    assertThat(RegexFormatter.format("abcd", PRESERVE_CAPTURING_GROUPS))
        .isEqualTo("abcd");
  }

  @Test
  public void testNested() {
    assertThat(RegexFormatter.format("ab(cd|ef)gh", PRESERVE_CAPTURING_GROUPS)).isEqualTo(lines(
        "ab(",
        "  cd|",
        "  ef",
        ")gh"));

    assertThat(RegexFormatter.format("ab(?:cd|ef)gh", PRESERVE_CAPTURING_GROUPS)).isEqualTo(lines(
        "ab(?:",
        "  cd|",
        "  ef",
        ")gh"));
  }

  @Test
  public void testDoubleNested() {
    assertThat(RegexFormatter.format("ab(cd(ef|gh)|ij)", PRESERVE_CAPTURING_GROUPS))
        .isEqualTo(lines(
            "ab(",
            "  cd(",
            "    ef|",
            "    gh",
            "  )|",
            "  ij",
            ")"));

    assertThat(RegexFormatter.format("ab(cd(?:ef|gh)|ij)", PRESERVE_CAPTURING_GROUPS))
        .isEqualTo(lines(
            "ab(",
            "  cd(?:",
            "    ef|",
            "    gh",
            "  )|",
            "  ij",
            ")"));
  }

  @Test
  public void testForceNonCapturingGroups() {
    assertThat(RegexFormatter.format("ab(?:cd(ef|gh)|ij)", FORCE_NON_CAPTURING_GROUPS))
        .isEqualTo(lines(
            "ab(?:",
            "  cd(?:",
            "    ef|",
            "    gh",
            "  )|",
            "  ij",
            ")"));
  }

  @Test
  public void testForceCapturingGroups() {
    assertThat(RegexFormatter.format("ab(?:cd(ef|gh)|ij)", FORCE_CAPTURING_GROUPS)).isEqualTo(lines(
        "ab(",
        "  cd(",
        "    ef|",
        "    gh",
        "  )|",
        "  ij",
        ")"));
  }

  private static String lines(String... s) {
    return Joiner.on('\n').join(s);
  }
}
