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
package com.google.i18n.phonenumbers.metadata.table;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.table.CsvParser.rowMapper;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.metadata.table.CsvParser.RowMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CsvParserTest {
  @Test
  public void testSimple() {
    // Simplest case.
    assertSingleRow(CsvParser.commaSeparated(), "Hello,World!", "Hello", "World!");

    // Empty row yields one empty value in the "first column" (matches behaviour with quoting).
    assertSingleRow(CsvParser.commaSeparated(), "", "");
    assertSingleRow(CsvParser.commaSeparated(), "\"\"", "");

    // Trailing delimiter yields a trailing empty value (matches behaviour with quoting).
    assertSingleRow(CsvParser.commaSeparated(), "foo,", "foo", "");
    assertSingleRow(CsvParser.commaSeparated(), "foo,\"\"", "foo", "");
  }

  @Test
  public void testOtherDelimiters() {
    // Tabs sequences are not "folded" (maybe this could be an option?)
    assertSingleRow(CsvParser.tabSeparated(), "Hello\t\tWorld!", "Hello", "", "World!");
    assertSingleRow(CsvParser.withSeparator(';'), "Hello;World!", "Hello", "World!");
  }

  @Test
  public void testWhitespaceTrimming() {
    // Whitespace is preserved by default, but can be trimmed.
    assertSingleRow(CsvParser.commaSeparated(),
        " foo, bar, baz ", " foo", " bar", " baz ");
    assertSingleRow(CsvParser.commaSeparated().trimWhitespace(),
        " foo, bar, baz ", "foo", "bar", "baz");
    assertSingleRow(CsvParser.commaSeparated().trimWhitespace(),
        " foo,   ,   ", "foo", "", "");

  }

  @Test
  public void testQuoting() {
    // Quoting works as expected (and combines with whitespace trimming).
    assertSingleRow(CsvParser.commaSeparated(),
        "\"foo\",\"\"\"bar, baz\"\"\"", "foo", "\"bar, baz\"");
    assertSingleRow(CsvParser.commaSeparated().trimWhitespace(),
        "  \"foo\"  ,  \"\"\"bar, baz\"\"\"  ", "foo", "\"bar, baz\"");
  }

  @Test
  public void testQuoting_illegal() {
    // Without whitespace trimming any quotes in "unquoted" values are not permitted.
    assertThrows(IllegalArgumentException.class, () ->
        parse(CsvParser.commaSeparated(), "foo, \"bar, baz\""));
  }

  @Test
  public void testDelimiter() {
    assertSingleRow(CsvParser.tabSeparated(), "Hello\tWorld!", "Hello", "World!");
    assertSingleRow(CsvParser.withSeparator(';'), "Hello;World!", "Hello", "World!");
  }

  @Test
  public void testUnicode() {
    assertSingleRow(CsvParser.withSeparator('-'), "😱-😂-💩", "😱", "😂", "💩");
    assertSingleRow(CsvParser.commaSeparated(), "\0,😱😂,\n", "\0", "😱😂", "\n");
    // Fun fact, not all ISO control codes count as "whitespace".
    assertSingleRow(CsvParser.commaSeparated().trimWhitespace(), "\0,😱😂,\n", "\0", "😱😂", "");
  }

  @Test
  public void testMultiline() {
    // Newlines become literals in quoted values.
    List<List<String>> rows = parse(CsvParser.commaSeparated().allowMultiline(),
        "foo,\"Hello,",
        "World!\"");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsExactly("foo", "Hello,\nWorld!").inOrder();
  }

  @Test
  public void testMultilineWithTrimming() {
    List<List<String>> rows = parse(
        CsvParser.commaSeparated().allowMultiline().trimWhitespace(),
        "  foo  ,  \" Hello,",
        "World! \"  ");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsExactly("foo", " Hello,\nWorld! ").inOrder();
  }

  @Test
  public void testMultiline_illegal() {
    // If not configured for multiline values, this is an unterminated quoted value.
    assertThrows(IllegalArgumentException.class, () ->
        parse(CsvParser.commaSeparated(), "foo,\"Hello,", "World!\""));
    // This fails because no more lines exist (even if multiline is allowed)
    assertThrows(IllegalArgumentException.class, () ->
        parse(CsvParser.commaSeparated().allowMultiline(), "foo,\"Hello,"));
  }

  @Test
  public void testRowMapping() {
    List<ImmutableMap<String, String>> rows = parseMap(
        CsvParser.commaSeparated(),
        rowMapper(),
        "FOO,BAR",
        "foo,bar",
        "Hello,World!",
        "No Trailing,",
        ",",
        "");
    assertThat(rows).hasSize(5);
    assertThat(rows.get(0)).containsExactly("FOO", "foo", "BAR", "bar").inOrder();
    assertThat(rows.get(1)).containsExactly("FOO", "Hello", "BAR", "World!").inOrder();
    assertThat(rows.get(2)).containsExactly("FOO", "No Trailing").inOrder();
    assertThat(rows.get(3)).isEmpty();
    assertThat(rows.get(4)).isEmpty();
  }

  @Test
  public void testRowMapping_withHeader() {
    List<String> header = new ArrayList<>();
    List<ImmutableMap<String, String>> rows = parseMap(
        CsvParser.commaSeparated(),
        rowMapper(header::addAll),
        "FOO,BAR",
        "foo,bar");
    assertThat(rows).hasSize(1);
    assertThat(header).containsExactly("FOO", "BAR").inOrder();
    assertThat(rows.get(0)).containsExactly("FOO", "foo", "BAR", "bar").inOrder();
  }

  private void assertSingleRow(CsvParser parser, String line, String... values) {
    List<List<String>> rows = parse(parser, line);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsExactlyElementsIn(values).inOrder();
  }

  private static List<List<String>> parse(CsvParser parser, String... lines) {
    List<List<String>> rows = new ArrayList<>();
    parser.parse(Stream.of(lines), r -> rows.add(r.collect(toImmutableList())));
    return rows;
  }

  private static List<ImmutableMap<String, String>> parseMap(
      CsvParser p, RowMapper mapper, String... lines) {
    List<ImmutableMap<String, String>> rows = new ArrayList<>();
    p.parse(Stream.of(lines), mapper.mapTo(rows::add));
    return rows;
  }
}
