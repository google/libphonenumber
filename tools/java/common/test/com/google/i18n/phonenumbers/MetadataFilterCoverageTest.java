/*
 *  Copyright (C) 2016 The Libphonenumber Authors
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests to ensure that the {@link MetadataFilter} logic over excludable fields cover all applicable
 * fields.
 */
@RunWith(JUnit4.class)
public final class MetadataFilterCoverageTest {
  private static final String CODE;

  static {
    try {
      BufferedReader source = new BufferedReader(new InputStreamReader(new BufferedInputStream(
          MetadataFilterTest.class.getResourceAsStream(
          "/com/google/i18n/phonenumbers/MetadataFilter.java")),
          Charset.forName("UTF-8")));
      StringBuilder codeBuilder = new StringBuilder();
      for (String line = source.readLine(); line != null; line = source.readLine()) {
        codeBuilder.append(line).append("\n");
      }
      CODE = codeBuilder.toString();
    } catch (IOException e) {
      throw new RuntimeException("MetadataFilter.java resource not set up properly", e);
    }
  }

  @Test
  public void testCoverageOfExcludableParentFields() {
    for (String field : MetadataFilter.excludableParentFields) {
      String capitalized = Character.toUpperCase(field.charAt(0)) + field.substring(1);
      String conditional = String.format("(?s).*if \\(metadata.has%s\\(\\)\\) \\{\\s+"
          + "metadata.set%s\\(getFiltered\\(\"%s\",\\s+metadata.get%s\\(\\)\\)\\);\\s+\\}.*",
          capitalized, capitalized, field, capitalized);
      assertTrue("Code is missing correct conditional for " + field, CODE.matches(conditional));
    }

    assertEquals(countOccurrencesOf("metadata.has", CODE),
        MetadataFilter.excludableParentFields.size());
  }

  @Test
  public void testCoverageOfExcludableChildFields() {
    for (String field : MetadataFilter.excludableChildFields) {
      String capitalized = Character.toUpperCase(field.charAt(0)) + field.substring(1);
      String conditional = String.format("(?s).*if \\(shouldDrop\\(type, \"%s\"\\)\\) \\{\\s+"
          + "builder.clear%s\\(\\);\\s+\\}.*", field, capitalized);
      assertTrue("Code is missing correct conditional for " + field, CODE.matches(conditional));
    }

    assertEquals(countOccurrencesOf("shouldDrop(type, \"", CODE),
        MetadataFilter.excludableChildFields.size());
  }

  @Test
  public void testCoverageOfExcludableChildlessFields() {
    for (String field : MetadataFilter.excludableChildlessFields) {
      String capitalized = Character.toUpperCase(field.charAt(0)) + field.substring(1);
      String conditional = String.format("(?s).*if \\(shouldDrop\\(\"%s\"\\)\\) \\{\\s+"
          + "metadata.clear%s\\(\\);\\s+\\}.*", field, capitalized);
      assertTrue("Code is missing correct conditional for " + field, CODE.matches(conditional));
    }

    assertEquals(countOccurrencesOf("shouldDrop(\"", CODE),
        MetadataFilter.excludableChildlessFields.size());
  }

  private static int countOccurrencesOf(String substring, String string) {
    int count = 0;
    for (int i = string.indexOf(substring); i != -1; i = string.indexOf(substring, i + 1)) {
      count++;
    }
    return count;
  }
}
