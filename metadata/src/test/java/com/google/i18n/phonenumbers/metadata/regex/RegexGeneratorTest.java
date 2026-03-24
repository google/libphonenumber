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

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.regex.RegexGenerator.basic;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RegexGeneratorTest {
  @Test
  public void testSimple() {
    assertRegex(basic(), ranges("123xxx"), "123\\d{3}");
    // This could be improved to "..." rather than ".{3}" saving 1 char, probably not worth it.
    assertRegex(basic().withDotMatch(), ranges("123xxx"), "123.{3}");
  }

  @Test
  public void testVariableLength() {
    assertRegex(basic(), ranges("123xxx", "123xxxx", "123xxxxx", "123xxxxxx"), "123\\d{3,6}");
  }

  @Test
  public void testTailOptimization() {
    RangeTree dfa = ranges("123xxx", "123xxxx", "145xxx");
    assertRegex(basic(), dfa, "1(?:23\\d{3,4}|45\\d{3})");
    assertRegex(basic().withTailOptimization(), dfa, "1(?:23\\d?|45)\\d{3}");
  }

  @Test
  public void testDfaFactorization() {
    // Essentially create a "thin" wedge of specific non-determinism with the shorter (5-digit)
    // numbers which prevents the larger ranges from being contiguous in the DFA.
    RangeTree dfa = ranges("1234x", "1256x", "[0-4]xxxxxx", "[0-4]xxxxxxx");
    assertRegex(basic(), dfa,
        "[02-4]\\d{6,7}|",
        "1(?:[013-9]\\d{5,6}|",
        "2(?:[0-246-9]\\d{4,5}|",
        "3(?:[0-35-9]\\d{3,4}|4\\d(?:\\d{2,3})?)|",
        "5(?:[0-57-9]\\d{3,4}|6\\d(?:\\d{2,3})?)))");
    assertRegex(basic().withDfaFactorization(), dfa, "[0-4]\\d{6,7}|12(?:34|56)\\d");
  }

  @Test
  public void testSubgroupOptimization() {
    // The subgraph of "everything except 95, 96 and 100" (this appears in China leading digits).
    RangeTree postgraph = ranges("[02-8]", "1[1-9]", "10[1-9]", "9[0-47-9]");
    RangeTree pregraph = ranges("123", "234", "345", "456", "567");

    // Cross product of pre and post paths.
    RangeTree subgraph = RangeTree.from(
        pregraph.asRangeSpecifications().stream()
            .flatMap(a -> postgraph.asRangeSpecifications().stream().map(a::extendBy)));

    // Union in other paths to trigger repetition in the "basic" case.
    RangeTree rest = ranges("128xx", "238xx", "348xx", "458xx", "568xx");
    RangeTree dfa = rest.union(subgraph);

    assertRegex(basic(), dfa,
        "12(?:3(?:[02-8]|1(?:0[1-9]|[1-9])|9[0-47-9])|8\\d\\d)|",
        "23(?:4(?:[02-8]|1(?:0[1-9]|[1-9])|9[0-47-9])|8\\d\\d)|",
        "34(?:5(?:[02-8]|1(?:0[1-9]|[1-9])|9[0-47-9])|8\\d\\d)|",
        "45(?:6(?:[02-8]|1(?:0[1-9]|[1-9])|9[0-47-9])|8\\d\\d)|",
        "56(?:7(?:[02-8]|1(?:0[1-9]|[1-9])|9[0-47-9])|8\\d\\d)");

    assertRegex(basic().withSubgroupOptimization(), dfa,
        "(?:12|23|34|45|56)8\\d\\d|",
        "(?:123|234|345|456|567)(?:[02-8]|1(?:0[1-9]|[1-9])|9[0-47-9])");
  }

  @Test
  public void testRegression_bug_65250963() {
    RangeTree dfa = ranges(
        "1387",
        "1697",
        "1524",
        "1539",
        "1768",
        "1946");
    assertRegex(basic(), dfa,
        "1(?:",
        "  (?:",
        "    38|",
        "    69",
        "  )7|",
        "  5(?:",
        "    24|",
        "    39",
        "  )|",
        "  768|",
        "  946",
        ")");
  }

  @Test
  public void testRegression_bug_68929642() {
    assertMatches(
        "1\\d{6}(?:\\d{2})?",
        ImmutableList.of("1234567", "123456789"),
        ImmutableList.of("12345678"),
        "1xxx_xxx", "1xx_xxx_xxx");

    assertMatches(
        "1\\d{6}[0-7]?",
        ImmutableList.of("1234567", "12345670"),
        ImmutableList.of("123456", "123456700"),
        "1xxx_xxx", "1x_xxx_xx[0-7]");

    assertMatches(
        "\\d\\d?",
        ImmutableList.of("1", "12"),
        ImmutableList.of("", "123"),
        "x", "xx");

    assertMatches(
        "\\d{1,3}",
        ImmutableList.of("1", "12", "123"),
        ImmutableList.of("", "1234"),
        "x", "xx", "xxx");

    assertMatches(
        "\\d(?:\\d{3}(?:\\d{2})?)?",
        ImmutableList.of("1", "1234", "123456"),
        ImmutableList.of("", "12", "123", "12345", "1234567"),
        "x", "xxxx", "xxx_xxx");

    assertMatches(
        "(?:\\d\\d(?:\\d(?:\\d{2,4})?)?)?",
        ImmutableList.of("", "12", "123", "12345", "123456", "1234567"),
        ImmutableList.of("1", "1234", "12345678"),
        "", "xx", "xxx", "xx_xxx", "xxx_xxx", "xxxx_xxx");

    assertMatches(
        "(?:\\d{2})?",
        ImmutableList.of("", "12"),
        ImmutableList.of("1", "123"),
        "", "xx");

    assertMatches(
        "\\d?",
        ImmutableList.of("", "1"),
        ImmutableList.of("12"),
        "", "x");
  }

  // This does not check that the generated regex is the same as the input, but it does test some
  // positive/negative matching cases against both and verifies that the DFA for both are equal.
  private static void assertMatches(
      String pattern, List<String> matchNumbers, List<String> noMatchNumbers, String... specs) {
    String regex = basic().toRegex(ranges(specs));
    assertThat(regex).isEqualTo(pattern);

    // Test the given positive/negative match numbers and expect the same behaviour from both.
    for (String number : matchNumbers) {
      assertThat(number).matches(pattern);
      assertThat(number).matches(regex);
    }
    for (String number : noMatchNumbers) {
      assertThat(number).doesNotMatch(pattern);
      assertThat(number).doesNotMatch(regex);
    }
  }

  private static void assertRegex(RegexGenerator generator, RangeTree dfa, String... lines) {
    String regex = generator.toRegex(dfa);
    String expected = Arrays.stream(lines).map(whitespace()::removeFrom).collect(joining());
    assertThat(regex).isEqualTo(expected);
  }

  private static RangeTree ranges(String... specs) {
    return RangeTree.from(Arrays.stream(specs).map(RangeSpecification::parse));
  }
}
