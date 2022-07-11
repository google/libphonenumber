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

import static com.google.common.base.StandardSystemProperty.LINE_SEPARATOR;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AltFormatsSchemaTest {

  private static final String NEW_LINE = LINE_SEPARATOR.value();

  @Test
  public void testSimple_export() throws IOException {
    assertThat(
        exportCsv(
            altFormat("123 XXX XXXX", "foo", "Hello World")))
        .containsExactly(
            "Format       ; Parent Format ; Comment",
            "123 XXX XXXX ; foo           ; \"Hello World\"")
        .inOrder();
  }

  @Test
  public void testSimple_import() throws IOException {
    assertThat(
        importCsv(
            "Format       ; Parent Format ; Comment",
            "123 XXX XXXX ; foo           ; \"Hello World\""))
        .containsExactly(
            altFormat("123 XXX XXXX", "foo", "Hello World"));
  }

  @Test
  public void testEscapedText_export() throws IOException {
    assertThat(
        exportCsv(
            altFormat("123 XXX XXXX", "foo", "\tHello\nWorld\\")))
        .containsExactly(
            "Format       ; Parent Format ; Comment",
            "123 XXX XXXX ; foo           ; \"\\tHello\\nWorld\\\\\"")
        .inOrder();
  }

  @Test
  public void testEscapedText_import() throws IOException {
    assertThat(
        importCsv(
            "Format       ; Parent Format ; Comment",
            "123 XXX XXXX ; foo           ; \"\\tHello\\nWorld\\\\\""))
        .containsExactly(
            altFormat("123 XXX XXXX", "foo", "\tHello\nWorld\\"));
  }

  @Test
  public void testRetainsExplicitOrdering() throws IOException {
    assertThat(
        exportCsv(
            altFormat("123 XXXXXX", "foo", "First"),
            altFormat("XX XXXX", "bar", "Second"),
            altFormat("9X XXX XXX", "baz", "Third")))
        .containsExactly(
            "Format     ; Parent Format ; Comment",
            "123 XXXXXX ; foo           ; \"First\"",
            "XX XXXX    ; bar           ; \"Second\"",
            "9X XXX XXX ; baz           ; \"Third\"")
        .inOrder();
  }

  private AltFormatSpec altFormat(String spec, String parentId, String comment) {
    return AltFormatsSchema.parseAltFormat(spec, parentId, comment);
  }

  private static List<String> exportCsv(AltFormatSpec... altFormats) throws IOException {
    try (StringWriter out = new StringWriter()) {
      AltFormatsSchema.exportCsv(out, Arrays.asList(altFormats));
      // Ignore trailing empty lines.
      return Splitter.on(NEW_LINE).omitEmptyStrings().splitToList(out.toString());
    }
  }

  private static ImmutableList<AltFormatSpec> importCsv(String... lines)
      throws IOException {
    // Add a trailing newline, since that's what we expect in the real CSV files.
    StringReader file = new StringReader(Joiner.on(NEW_LINE).join(lines) + NEW_LINE);
    return AltFormatsSchema.importAltFormats(file);
  }
}

