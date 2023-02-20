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

package com.google.i18n.phonenumbers.metadata.finitestatematcher.compiler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.primitives.Bytes.asList;
import static com.google.i18n.phonenumbers.metadata.finitestatematcher.compiler.MatcherCompiler.compile;

import com.google.common.truth.Truth;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.OpCode;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MatcherCompilerTest {

  private static final Byte TERMINATOR = (byte) 0;

  @Test public void testSingleOperation() {
    byte digit0 = single(0);
    byte digit5 = single(5);
    byte digit9 = single(9);
    assertCompile(ranges("0"), digit0, TERMINATOR);
    assertCompile(ranges("5"), digit5, TERMINATOR);
    assertCompile(ranges("9"), digit9, TERMINATOR);
    assertCompile(ranges("0559"), digit0, digit5, digit5, digit9, TERMINATOR);

    byte digit5Terminating = (byte) (digit5 | (1 << 4));
    assertCompile(ranges("05", "0559"),
        digit0, digit5, digit5Terminating, digit9, TERMINATOR);
  }

  @Test public void testAnyOperation() {
    byte anyDigit = any(1);
    byte anyDigit16Times = any(16);
    assertCompile(ranges("x"), anyDigit, TERMINATOR);
    assertCompile(ranges("xxxx_xxxx_xxxx_xxxx"), anyDigit16Times, TERMINATOR);
    assertCompile(ranges("xxxx_xxxx_xxxx_xxxx_x"),
        anyDigit16Times, anyDigit, TERMINATOR);

    byte anyDigitTerminating = (byte) (anyDigit | (1 << 4));
    assertCompile(ranges("x", "xx"), anyDigit, anyDigitTerminating, TERMINATOR);
    assertCompile(ranges("xxxx_xxxx_xxxx_xxxx", "xxxx_xxxx_xxxx_xxxx_x"),
        anyDigit16Times, anyDigitTerminating, TERMINATOR);
  }

  @Test public void testRangeOperation() {
    int range09 = range(0, 9);
    int range123 = range(1, 2, 3);
    int range789 = range(7, 8, 9);

    assertCompile(ranges("[09]"), hi(range09), lo(range09), TERMINATOR);
    assertCompile(ranges("[123][789]"),
        hi(range123), lo(range123), hi(range789), lo(range789), TERMINATOR);
  }

  @Test public void testMapOperation() {
    // Force all 10 possible branches to be taken.
    byte[] data = compile(ranges("00", "11", "22", "33", "44", "55", "66", "77", "88", "99"));
    // Check only the first 4 bytes for exact values.
    Assert.assertEquals(
        asList((byte) 0x95, (byte) 0x31, (byte) 0xF5, (byte) 0x9D),
        asList(data).subList(0, 4));
    // Each branch should jump to a 2 byte sequence between 10 and 28 bytes away (inclusive).
    List<Byte> jumpTable = asList(data).subList(4, 14);
    List<Byte> remainder = asList(data).subList(14, data.length);
    // TODO: Now that ordering should be consistent, tighten up this test to ensure
    // consistency and remove the shorter consistency test below.
    for (byte jump : new byte[] {0xA, 0xC, 0xE, 0x10, 0x12, 0x14, 0x16, 0x18, 0x1A, 0x1C}) {
      Assert.assertTrue(jumpTable.contains(jump));
      int index = jumpTable.indexOf(jump);
      // Subtract the length of the jump table to get relative offset in remaining code.
      jump = (byte) (jump - 10);
      // Each jump should end in 2 single-byte instructions (match corresponding digit, terminate).
      Assert.assertEquals(single(index), remainder.get(jump));
      Assert.assertEquals(TERMINATOR, remainder.get(jump + 1));
    }
  }

  @Test public void testConsistentSorting() {
    // Ensure that the MatcherCompiler output is consistent, otherwise it can result in a
    // non-deterministic build, because the generated file changes with each execution.
    byte[] expected = new byte[] {-128, 0, 0, 29, 3, 5, 7, 32, 0, 33, 0, 34, 0};
    assertCompile(ranges("00", "11", "22"), expected);
  }

  /** Returns the 1-byte instruction representing matching a single digit once. */
  private static Byte single(int value) {
    checkArgument(value >= 0 && value < 10);
    return (byte) ((OpCode.SINGLE.ordinal() << 5) | value);
  }

  /** Returns the 1-byte instruction representing matching any digit a specified number of times. */
  private static Byte any(int count) {
    checkArgument(count > 0 && count <= 16);
    return (byte) ((OpCode.ANY.ordinal() << 5) | (count - 1));
  }

  /** Returns the 2-byte instruction representing matching a range of digits. */
  private static int range(int... digits) {
    int mask = 0;
    for (int d : digits) {
      checkArgument(0 <= d && d <= 9);
      mask |= 1 << d;
    }
    return (OpCode.RANGE.ordinal() << 13) | mask;
  }

  private static Byte hi(int shortInstruction) {
    return (byte) (shortInstruction >> 8);
  }

  private static Byte lo(int shortInstruction) {
    return (byte) (shortInstruction & 0xFF);
  }

  private void assertCompile(RangeTree dfa, byte... expected) {
    Truth.assertThat(compile(dfa)).isEqualTo(expected);
  }

  private static RangeTree ranges(String... lines) {
    return RangeTree.from(Arrays.stream(lines).map(RangeSpecification::parse));
  }
}
