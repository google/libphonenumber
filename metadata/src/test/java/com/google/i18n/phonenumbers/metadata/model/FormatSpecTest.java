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
import static java.util.Optional.empty;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.i18n.phonenumbers.metadata.model.FormatSpec.FormatTemplate;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FormatSpecTest {
  @Test
  public void testCreate_national() {
    national("XXXX");
    national("XXX***");
    national("#XXX XXX");
    national("(#XXX) XX**-XXX");
    assertThat(national("XX\\XXX").national().skeleton()).isEqualTo("$1X$2");
  }

  @Test
  public void testCreate_international() {
    // The international spec can be a duplicate (signifies international formatting is permitted).
    international("XXX XXXX", "XXX XXXX");
    // Or it can be different (including grouping and separators).
    international("(#XXX) XXXX", "XXX-XXXX");
  }

  @Test
  public void testCreate_carrier() {
    carrier("# XXX XXXX", "# @ XXX XXXX");
    carrier("XXX XXXX", "@ XXX XXXX");
    // Carrier and national prefix can differ on whether national prefix is needed.
    carrier("XXX XXXX", "#@ XXX XXXX");
  }

  @Test
  public void testCreate_national_bad() {
    assertThrows(IllegalArgumentException.class, () -> national(""));
    assertThrows(IllegalArgumentException.class, () -> national("Hello"));
    assertThrows(IllegalArgumentException.class, () -> national("$1"));
    assertThrows(IllegalArgumentException.class, () -> national("XX**XX"));
    assertThrows(IllegalArgumentException.class, () -> national("****"));
    assertThrows(IllegalArgumentException.class, () -> national("@ XXX"));
  }

  @Test
  public void testCreate_international_bad() {
    // National prefix is not allowed.
    assertThrows(IllegalArgumentException.class, () -> international("#XXXX", "#XXXX"));
    // Groups must match.
    assertThrows(IllegalArgumentException.class, () -> international("# XXXX", "XX XX"));
    assertThrows(IllegalArgumentException.class, () -> international("# XXXX", "XXX"));
  }

  @Test
  public void testCreate_carrier_bad() {
    // Carrier specs must have '@' present.
    assertThrows(IllegalArgumentException.class, () -> carrier("XXX XXXX", "XXX XXXX"));
    // Carrier specs cannot differ after the first group (including separator).
    assertThrows(IllegalArgumentException.class, () -> carrier("#XXX XXXX", "#@XXX-XXXX"));
    // National prefix (if present) must come first (if this is ever relaxed, we would need to
    // change how carrier prefixes are handled and how nationalPrefixForParsing is generated).
    assertThrows(IllegalArgumentException.class, () -> carrier("# XXX XXXX", "@# XXX XXXX"));
  }

  @Test
  public void testTemplate_splitPrefix() {
    FormatTemplate t = FormatTemplate.parse("(#) XXX - XXX**");
    assertThat(t.getXmlCapturingPattern()).isEqualTo("(\\d{3})(\\d{3,5})");
    assertThat(t.getXmlFormat()).isEqualTo("$1 - $2");
    assertThat(t.getXmlPrefix()).hasValue("($NP) $FG");
    assertThat(t.hasNationalPrefix()).isTrue();
    assertThat(t.hasCarrierCode()).isFalse();
  }

  @Test
  public void testTemplate_noPrefix() {
    FormatTemplate t = FormatTemplate.parse("XXX XX-XX");
    assertThat(t.getXmlCapturingPattern()).isEqualTo("(\\d{3})(\\d{2})(\\d{2})");
    assertThat(t.getXmlFormat()).isEqualTo("$1 $2-$3");
    assertThat(t.getXmlPrefix()).isEmpty();
    assertThat(t.hasNationalPrefix()).isFalse();
    assertThat(t.hasCarrierCode()).isFalse();
  }

  @Test
  public void testTemplate_replacementNoNationalPrefix() {
    FormatTemplate t = FormatTemplate.parse("{XXX>123} XX-XX");
    assertThat(t.getXmlCapturingPattern()).isEqualTo("(\\d{3})(\\d{2})(\\d{2})");
    assertThat(t.getXmlFormat()).isEqualTo("$2-$3");
    assertThat(t.getXmlPrefix()).hasValue("123 $FG");
    assertThat(t.hasNationalPrefix()).isFalse();
    assertThat(t.hasCarrierCode()).isFalse();
  }

  @Test
  public void testTemplate_replacementWithNationalPrefix() {
    FormatTemplate t = FormatTemplate.parse("#{XXX>123} XX-XX");
    assertThat(t.getXmlCapturingPattern()).isEqualTo("(\\d{3})(\\d{2})(\\d{2})");
    assertThat(t.getXmlFormat()).isEqualTo("$2-$3");
    assertThat(t.getXmlPrefix()).hasValue("$NP123 $FG");
    assertThat(t.hasNationalPrefix()).isTrue();
    assertThat(t.hasCarrierCode()).isFalse();
  }

  @Test
  public void testTemplate_replacementNotFirstGroup() {
    FormatTemplate t = FormatTemplate.parse("XXX {XX>ABC} XX");
    assertThat(t.getXmlCapturingPattern()).isEqualTo("(\\d{3})(\\d{2})(\\d{2})");
    assertThat(t.getXmlFormat()).isEqualTo("$1 ABC $3");
    assertThat(t.getXmlPrefix()).isEmpty();
    assertThat(t.hasNationalPrefix()).isFalse();
    assertThat(t.hasCarrierCode()).isFalse();
  }

  @Test
  public void testTemplate_removeFirstGroupViaReplacement() {
    // This test is very important for Argentina, where the leading group must be removed (and a
    // different mobile token is used after the area code).
    FormatTemplate t = FormatTemplate.parse("{XX>}XXX XXXX");
    assertThat(t.getXmlCapturingPattern()).isEqualTo("(\\d{2})(\\d{3})(\\d{4})");
    assertThat(t.getXmlFormat()).isEqualTo("$2 $3");
    assertThat(t.getXmlPrefix()).isEmpty();
    assertThat(t.hasNationalPrefix()).isFalse();
    assertThat(t.hasCarrierCode()).isFalse();
  }


  private static FormatSpec national(String national) {
    return FormatSpec.of(national, empty(), empty(), empty(), false, empty());
  }

  private static FormatSpec international(String national, String intl) {
    return FormatSpec.of(national, empty(), Optional.of(intl), empty(), false, empty());
  }

  private static FormatSpec carrier(String national, String carrier) {
    return FormatSpec.of(national, Optional.of(carrier), empty(), empty(), false, empty());
  }
}
