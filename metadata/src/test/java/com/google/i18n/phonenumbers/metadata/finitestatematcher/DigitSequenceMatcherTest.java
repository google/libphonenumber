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

package com.google.i18n.phonenumbers.metadata.finitestatematcher;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.Result.INVALID;
import static com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.Result.MATCHED;
import static com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.Result.TOO_LONG;
import static com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.Result.TOO_SHORT;

import com.google.common.base.CharMatcher;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.DigitSequence;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.Result;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.compiler.MatcherCompiler;
import com.google.i18n.phonenumbers.metadata.regex.RegexGenerator;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DigitSequenceMatcherTest {

  @Test public void testStringDigits() {
    DigitSequence digits = DigitSequenceMatcher.digitsFromString("1234");

    Assert.assertTrue(digits.hasNext());
    Assert.assertEquals(1, digits.next());
    Assert.assertTrue(digits.hasNext());
    Assert.assertEquals(2, digits.next());
    Assert.assertTrue(digits.hasNext());
    Assert.assertEquals(3, digits.next());
    Assert.assertTrue(digits.hasNext());
    Assert.assertEquals(4, digits.next());
    Assert.assertFalse(digits.hasNext());
  }

  @Test public void testSingleDigitMatching() {
    assertNotMatches(ranges("0"), INVALID, "1", "9");
    assertNotMatches(ranges("0"), TOO_LONG, "00");

    assertMatches(ranges("x"), "0", "5", "9");
    assertNotMatches(ranges("x"), TOO_SHORT, "");
    assertNotMatches(ranges("x"), TOO_LONG, "00");

    assertMatches(ranges("[2-6]"), "2", "3", "4", "5", "6");
    assertNotMatches(ranges("[2-6]"), INVALID, "0", "1", "7", "8", "9");
    assertNotMatches(ranges("[2-6]"), TOO_LONG, "26");
  }

  @Test public void testOptional() {
    RangeTree dfa = ranges("12", "123");
    assertMatches(ranges("12", "123"), "12", "123");
    assertNotMatches(dfa, TOO_SHORT, "1");
    assertNotMatches(dfa, INVALID, "13");
    assertNotMatches(dfa, TOO_LONG, "1233");
  }

  @Test public void testRepetition() {
    assertMatches(ranges("12xx", "12xxx", "12xxxx"), "1234", "12345", "123456");
  }

  @Test public void testOr() {
    RangeTree dfa = ranges("01", "23");
    assertMatches(dfa, "01", "23");
    assertNotMatches(dfa, INVALID, "03", "12");
    assertNotMatches(dfa, TOO_SHORT, "0", "2");
    assertNotMatches(dfa, TOO_LONG, "011", "233");

    assertMatches(ranges("01", "23", "45", "6789"), "01", "23", "45", "6789");
  }

  @Test public void testRealRegexShort() {
    RangeTree dfa = ranges(
        "11[2-7]xxxxxxx",
        "2[02][2-7]xxxxxxx",
        "33[2-7]xxxxxxx",
        "4[04][2-7]xxxxxxx",
        "79[2-7]xxxxxxx",
        "80[2-467]xxxxxxx");

    assertMatches(dfa, "112 1234567", "797 1234567", "807 1234567");
    assertNotMatches(dfa, TOO_SHORT, "112 123", "797 12345", "807 123456");
    assertNotMatches(dfa, TOO_LONG, "112 12345678", "797 123456789");
    assertNotMatches(dfa, INVALID, "122 1234567", "799 1234567", "805 1234567");
  }

  @Test public void testRealRegexLong() {
    RangeTree dfa = ranges(
        "12[0-249][2-7]xxxxxx",
        "13[0-25][2-7]xxxxxx",
        "14[145][2-7]xxxxxx",
        "1[59][14][2-7]xxxxxx",
        "16[014][2-7]xxxxxx",
        "17[1257][2-7]xxxxxx",
        "18[01346][2-7]xxxxxx",
        "21[257][2-7]xxxxxx",
        "23[013][2-7]xxxxxx",
        "24[01][2-7]xxxxxx",
        "25[0137][2-7]xxxxxx",
        "26[0158][2-7]xxxxxx",
        "278[2-7]xxxxxx",
        "28[1568][2-7]xxxxxx",
        "29[14][2-7]xxxxxx",
        "326[2-7]xxxxxx",
        "34[1-3][2-7]xxxxxx",
        "35[34][2-7]xxxxxx",
        "36[01489][2-7]xxxxxx",
        "37[02-46][2-7]xxxxxx",
        "38[159][2-7]xxxxxx",
        "41[36][2-7]xxxxxx",
        "42[1-47][2-7]xxxxxx",
        "43[15][2-7]xxxxxx",
        "45[12][2-7]xxxxxx",
        "46[126-9][2-7]xxxxxx",
        "47[0-24-9][2-7]xxxxxx",
        "48[013-57][2-7]xxxxxx",
        "49[014-7][2-7]xxxxxx",
        "5[136][25][2-7]xxxxxx",
        "522[2-7]xxxxxx",
        "54[28][2-7]xxxxxx",
        "55[12][2-7]xxxxxx",
        "5[78]1[2-7]xxxxxx",
        "59[15][2-7]xxxxxx",
        "612[2-7]xxxxxx",
        "6[2-4]1[2-7]xxxxxx",
        "65[17][2-7]xxxxxx",
        "66[13][2-7]xxxxxx",
        "67[14][2-7]xxxxxx",
        "680[2-7]xxxxxx",
        "712[2-7]xxxxxx",
        "72[14][2-7]xxxxxx",
        "73[134][2-7]xxxxxx",
        "74[47][2-7]xxxxxx",
        "75[15][2-7]xxxxxx",
        "7[67]1[2-7]xxxxxx",
        "788[2-7]xxxxxx",
        "816[2-7]xxxxxx",
        "82[014][2-7]xxxxxx",
        "83[126][2-7]xxxxxx",
        "86[136][2-7]xxxxxx",
        "87[078][2-7]xxxxxx",
        "88[34][2-7]xxxxxx",
        "891[2-7]xxxxxx");

    assertMatches(dfa, "364 2 123456", "674 4 123456", "883 7 123456");
    assertNotMatches(dfa, TOO_SHORT, "364 2 123", "674 4 1234", "883 7 12345");
    assertNotMatches(dfa, TOO_LONG, "364 2 1234567", "674 4 12345678");
    assertNotMatches(dfa, INVALID,
        "365 2 123456", "364 8 123456", "670 4 123456", "670 5 123456", "892 2 123456");
  }

  private static RangeTree ranges(String... lines) {
    return RangeTree.from(Arrays.stream(lines).map(RangeSpecification::parse));
  }

  private static void assertMatches(RangeTree dfa, String... numbers) {
    checkRegex(dfa, true, numbers);
    byte[] matcherData = MatcherCompiler.compile(dfa);

    DigitSequenceMatcher matcher = DigitSequenceMatcher.create(matcherData);
    assertMatcher(matcher, MATCHED, numbers);
  }

  private static void assertNotMatches(RangeTree dfa, Result error, String... numbers) {
    checkArgument(error != MATCHED);
    checkRegex(dfa, false, numbers);
    byte[] matcherData = MatcherCompiler.compile(dfa);
    DigitSequenceMatcher matcher = DigitSequenceMatcher.create(matcherData);
    assertMatcher(matcher, error, numbers);
  }

  private static void checkRegex(RangeTree dfa, boolean expectMatch, String... numbers) {
    Pattern pattern = Pattern.compile(RegexGenerator.basic().toRegex(dfa));
    for (String number : numbers) {
      checkArgument(expectMatch == pattern.matcher(noSpace(number)).matches(),
          "regex %s could not match input %s", dfa.asRangeSpecifications(), number);
    }
  }

  private static void assertMatcher(
      DigitSequenceMatcher matcher, Result expected, String... numbers) {
    for (final String number : numbers) {
      Assert.assertEquals(expected,
          matcher.match(DigitSequenceMatcher.digitsFromString(noSpace(number))));
    }
  }

  private static String noSpace(String input) {
    return CharMatcher.whitespace().removeFrom(input);
  }
}
