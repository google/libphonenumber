/*
 * Copyright (C) 2022 The Libphonenumber Authors.
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
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class LengthsParserTest {

  @Test
  public void shouldThrowIfStringContainsForbiddenCharacters() {
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("a-6,7"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("8, B, C"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("8, ,10"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("4, +7-9, +11"));
  }

  @Test
  public void shouldThrowIfNumbersAreOutOfOrder() {
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("9-7"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("8,12-11"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("5,4,7-8"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("6-8, 7-9"));
  }

  @Test
  public void shouldThrowIfFormatIsWrong() {
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("4-6-8"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("7-"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("3, -7"));
    assertThrows(IllegalArgumentException.class, () -> LengthsParser.parseLengths("1 2-3 4, 5 6"));
  }

  @Test
  public void testParseSingletons() {
    assertThat(LengthsParser.parseLengths("8")).containsExactly(8);
    assertThat(LengthsParser.parseLengths("14")).containsExactly(14);
  }

  @Test
  public void testParseCommaSeparatedNumbers() {
    assertThat(LengthsParser.parseLengths("6,8,9")).containsExactly(6, 8, 9);
    assertThat(LengthsParser.parseLengths("13, 14")).containsExactly(13, 14);
  }

  @Test
  public void testParseRanges() {
    assertThat(LengthsParser.parseLengths("6-8")).containsExactly(6, 7, 8);
    assertThat(LengthsParser.parseLengths("13 - 14")).containsExactly(13, 14);
  }

  @Test
  public void testParseComplex() {
    assertThat(LengthsParser.parseLengths("4,7,9-12")).containsExactly(4, 7, 9, 10, 11, 12);
    assertThat(LengthsParser.parseLengths("4-6, 8, 10-12")).containsExactly(4, 5, 6, 8, 10, 11, 12);
  }
}
