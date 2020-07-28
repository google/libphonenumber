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
package com.google.i18n.phonenumbers.metadata.model;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.model.FormatSpec.FormatTemplate;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AltFormatSpecTest {
  @Test
  public void testSimple() {
    FormatTemplate template = FormatTemplate.parse("XXXX XXXX");
    RangeSpecification prefix = RangeSpecification.parse("123");

    AltFormatSpec spec = AltFormatSpec.create(template, prefix, "foo", Optional.of("Comment"));

    assertThat(spec.template()).isEqualTo(template);
    assertThat(spec.prefix()).isEqualTo(prefix);
    assertThat(spec.parentFormatId()).isEqualTo("foo");
    assertThat(spec.comment()).hasValue("Comment");
    assertThat(spec.specifier()).isEqualTo("123X XXXX");
  }

  @Test
  public void testGoodTemplateAndPrefix() {
    assertGoodTemplateAndPrefix("XXX XXX", "", "XXX XXX");
    assertGoodTemplateAndPrefix("XXX XXX", "123", "123 XXX");
    assertGoodTemplateAndPrefix("XXX XXX", "1234", "123 4XX");
    assertGoodTemplateAndPrefix("XXX XXX", "123456", "123 456");
    assertGoodTemplateAndPrefix("XXX XXX**", "123", "123 XXX**");
    assertGoodTemplateAndPrefix("XXX XXX", "12[3-6]", "12[3-6] XXX");
    assertGoodTemplateAndPrefix("XXX XXX", "1x3", "1X3 XXX");
  }

  @Test
  public void testBadTemplateOrPrefix() {
    // Prefix too long.
    assertBadTemplateAndPrefix("XXXX", "12345");
    // Prefix too long for min length.
    assertBadTemplateAndPrefix("XXXX**", "12345");
    // Bad template chars.
    assertBadTemplateAndPrefix("XXX-XXX", "123");
    // Extra whitespace.
    assertBadTemplateAndPrefix(" XXXXXX", "123");
    // Prefix must not end with "any digit".
    assertBadTemplateAndPrefix(" XXXXXX", "123xx");
  }

  private static void assertGoodTemplateAndPrefix(String template, String prefix, String spec) {
    FormatTemplate t = FormatTemplate.parse(template);
    RangeSpecification p = RangeSpecification.parse(prefix);
    assertThat(AltFormatSpec.create(t, p, "foo", Optional.empty()).specifier()).isEqualTo(spec);
  }

  private static void assertBadTemplateAndPrefix(String template, String prefix) {
    FormatTemplate t = FormatTemplate.parse(template);
    RangeSpecification p = RangeSpecification.parse(prefix);
    assertThrows(IllegalArgumentException.class,
        () -> AltFormatSpec.create(t, p, "foo", Optional.empty()));
  }
}
