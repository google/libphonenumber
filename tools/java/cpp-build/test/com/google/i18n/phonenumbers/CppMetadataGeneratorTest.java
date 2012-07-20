/*
 *  Copyright (C) 2012 The Libphonenumber Authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.i18n.phonenumbers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.i18n.phonenumbers.CppMetadataGenerator.Type;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests that the CppXmlMetadata class emits the expected source and header files for metadata.
 */
public class CppMetadataGeneratorTest {

  @Test
  public void emitStaticArrayData() {
    // 13 bytes per line, so have 16 bytes to test > 1 line (general case).
    // Use all hex digits in both nibbles to test hex formatting.
    byte[] data = new byte[] {
      (byte) 0xF0, (byte) 0xE1, (byte) 0xD2, (byte) 0xC3,
      (byte) 0xB4, (byte) 0xA5, (byte) 0x96, (byte) 0x87,
      (byte) 0x78, (byte) 0x69, (byte) 0x5A, (byte) 0x4B,
      (byte) 0x3C, (byte) 0x2D, (byte) 0x1E, (byte) 0x0F,
    };

    StringWriter writer = new StringWriter();
    CppMetadataGenerator.emitStaticArrayData(new PrintWriter(writer), data);
    assertEquals(
        "  0xF0, 0xE1, 0xD2, 0xC3, 0xB4, 0xA5, 0x96, 0x87, 0x78, 0x69, 0x5A, 0x4B, 0x3C,\n" +
        "  0x2D, 0x1E, 0x0F\n",
        writer.toString());
  }

  @Test
  public void outputHeaderFile() throws IOException {
    byte[] data = new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };
    CppMetadataGenerator metadata = CppMetadataGenerator.create(Type.METADATA, data);

    StringWriter writer = new StringWriter();
    metadata.outputHeaderFile(writer);
    Iterator<String> lines = toLines(writer.toString()).iterator();
    // Sanity check that at least some of the expected lines are present.
    assertTrue(consumeUntil(" * Copyright (C) 2011 The Libphonenumber Authors", lines));
    assertTrue(consumeUntil("#ifndef I18N_PHONENUMBERS_METADATA_H_", lines));
    assertTrue(consumeUntil("#define I18N_PHONENUMBERS_METADATA_H_", lines));
    assertTrue(consumeUntil("namespace i18n {", lines));
    assertTrue(consumeUntil("namespace phonenumbers {", lines));
    assertTrue(consumeUntil("int metadata_size();", lines));
    assertTrue(consumeUntil("const void* metadata_get();", lines));
    assertTrue(consumeUntil("#endif  // I18N_PHONENUMBERS_METADATA_H_", lines));
  }

  @Test
  public void outputSourceFile() throws IOException {
    byte[] data = new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };
    CppMetadataGenerator metadata = CppMetadataGenerator.create(Type.ALTERNATE_FORMAT, data);

    StringWriter writer = new StringWriter();
    metadata.outputSourceFile(writer);
    Iterator<String> lines = toLines(writer.toString()).iterator();
    // Sanity check that at least some of the expected lines are present.
    assertTrue(consumeUntil(" * Copyright (C) 2012 The Libphonenumber Authors", lines));
    assertTrue(consumeUntil("namespace i18n {", lines));
    assertTrue(consumeUntil("namespace phonenumbers {", lines));
    assertTrue(consumeUntil("namespace {", lines));
    assertTrue(consumeUntil("static const unsigned char data[] = {", lines));
    assertTrue(consumeUntil("  0xCA, 0xFE, 0xBA, 0xBE", lines));
    assertTrue(consumeUntil("int alternate_format_size() {", lines));
    assertTrue(consumeUntil("const void* alternate_format_get() {", lines));
  }

  /** Converts a string containing newlines into a list of lines. */
  private static List<String> toLines(String s) throws IOException {
    BufferedReader reader = new BufferedReader(new StringReader(s));
    List<String> lines = new ArrayList<String>();
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      lines.add(line);
    }
    return lines;
  }

  /**
   * Consumes strings from the given iterator until the expected string is reached (it is also
   * consumed). If the expected string is not found, the iterator is exhausted and {@code false} is
   * returned.
   *
   * @return true if the expected string was found while consuming the iterator.
   */
  private static boolean consumeUntil(String expected, Iterator<String> it) {
    while (it.hasNext()) {
      if (it.next().equals(expected)) {
        return true;
      }
    }
    return false;
  }
}
