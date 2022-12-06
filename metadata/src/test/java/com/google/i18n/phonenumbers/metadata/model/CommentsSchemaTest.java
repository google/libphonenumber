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
import static com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment.anchor;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_FIXED_LINE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_MOBILE;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment.Anchor;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CommentsSchemaTest {

  private static final String NEW_LINE = LINE_SEPARATOR.value();

  private static final PhoneRegion REGION_US = PhoneRegion.of("US");
  private static final PhoneRegion REGION_CA = PhoneRegion.of("CA");

  private static final Anchor US_TOP = Comment.anchor(REGION_US);
  private static final Anchor US_FIXED_LINE = anchor(REGION_US, XML_FIXED_LINE);
  private static final Anchor US_MOBILE = anchor(REGION_US, XML_MOBILE);
  private static final Anchor US_SHORTCODE = Comment.shortcodeAnchor(REGION_US);
  private static final Anchor CA_FIXED_LINE = anchor(REGION_CA, XML_FIXED_LINE);

  @Test
  public void testSimple_export() throws IOException {
    assertThat(
        exportCsv(
            comment(US_FIXED_LINE, "Hello World")))
        .containsExactly(
            "Region ; Label          ; Comment",
            "US     ; XML_FIXED_LINE ; \"Hello World\"")
        .inOrder();
  }

  @Test
  public void testSimple_import() throws IOException {
    assertThat(
        importCsv(
            "Region ; Label          ; Comment",
            "US     ; XML_FIXED_LINE ; \"Hello World\""))
        .containsExactly(
            comment(US_FIXED_LINE, "Hello World"));
  }

  @Test
  public void testEscapedText_export() throws IOException {
    assertThat(
        exportCsv(
            comment(US_FIXED_LINE, "\tHello", "World\\")))
        .containsExactly(
            "Region ; Label          ; Comment",
            "US     ; XML_FIXED_LINE ; \"\\tHello\\nWorld\\\\\"")
        .inOrder();
  }

  @Test
  public void testEscapedText_import() throws IOException {
    assertThat(
        importCsv(
            "Region ; Label          ; Comment",
            "US     ; XML_FIXED_LINE ; \"\\tHello\\nWorld\\\\\""))
        .containsExactly(
            comment(US_FIXED_LINE, "\tHello", "World\\"));
  }

  @Test
  public void testOrdering_export() throws IOException {
    assertThat(
        exportCsv(
            comment(US_FIXED_LINE, "First"),
            comment(US_FIXED_LINE, "Second"),
            comment(US_FIXED_LINE, "Third"),
            comment(US_TOP, "Top Level Comment"),
            comment(US_SHORTCODE, "Shortcode Comment"),
            comment(US_MOBILE, "Other Type"),
            comment(CA_FIXED_LINE, "Other Region")))
        .containsExactly(
            "Region ; Label          ; Comment",
            "CA     ; XML_FIXED_LINE ; \"Other Region\"",
            "US     ; SC             ; \"Shortcode Comment\"",
            "US     ; XML            ; \"Top Level Comment\"",
            "US     ; XML_FIXED_LINE ; \"First\"",
            "US     ; XML_FIXED_LINE ; \"Second\"",
            "US     ; XML_FIXED_LINE ; \"Third\"",
            "US     ; XML_MOBILE     ; \"Other Type\"")
        .inOrder();
  }

  @Test
  public void testOrdering_import() throws IOException {
    assertThat(
        importCsv(
            "Region ; Label          ; Comment",
            "US     ; XML_FIXED_LINE ; \"First\"",
            "US     ; XML_FIXED_LINE ; \"Second\"",
            "US     ; XML_FIXED_LINE ; \"Third\"",
            "US     ; XML            ; \"Top Level Comment\"",
            "US     ; SC             ; \"Shortcode Comment\"",
            "US     ; XML_MOBILE     ; \"Other Type\"",
            "CA     ; XML_FIXED_LINE ; \"Other Region\""))
        .containsExactly(
            comment(CA_FIXED_LINE, "Other Region"),
            comment(US_SHORTCODE, "Shortcode Comment"),
            comment(US_TOP, "Top Level Comment"),
            comment(US_FIXED_LINE, "First"),
            comment(US_FIXED_LINE, "Second"),
            comment(US_FIXED_LINE, "Third"),
            comment(US_MOBILE, "Other Type"))
        .inOrder();
  }

  private Comment comment(Anchor a, String... lines) {
    return Comment.create(a, Arrays.asList(lines));
  }

  private static List<String> exportCsv(Comment... comments) throws IOException {
    try (StringWriter out = new StringWriter()) {
      CommentsSchema.exportCsv(out, Arrays.asList(comments));
      // Ignore trailing empty lines.
      return Splitter.on(NEW_LINE).omitEmptyStrings().splitToList(out.toString());
    }
  }

  private static ImmutableList<Comment> importCsv(String... lines)
      throws IOException {
    // Add a trailing newline, since that's what we expect in the real CSV files.
    StringReader file = new StringReader(Joiner.on(NEW_LINE).join(lines) + NEW_LINE);
    return CommentsSchema.importComments(file);
  }
}

