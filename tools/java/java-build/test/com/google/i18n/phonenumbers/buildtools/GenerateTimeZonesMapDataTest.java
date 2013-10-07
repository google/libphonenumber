/*
 * Copyright (C) 2012 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.buildtools;

import com.google.i18n.phonenumbers.prefixmapper.PrefixTimeZonesMap;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.SortedMap;

/**
 * Unittests for GenerateTimeZonesMapData.java
 *
 * @author Walter Erquinigo
 */
public class GenerateTimeZonesMapDataTest extends TestCase {
  private static final String BRUSSELS_TZ = "Europe/Brussels";
  private static final String PARIS_TZ = "Europe/Paris";
  private static final String PARIS_BRUSSELS_LINES =
        "322|" + BRUSSELS_TZ + "\n331|" + PARIS_TZ + "\n";

  private static SortedMap<Integer, String> parseTextFileHelper(String input) throws IOException {
    return GenerateTimeZonesMapData.parseTextFile(new ByteArrayInputStream(input.getBytes()));
  }

  public void testParseTextFile() throws IOException {
    Map<Integer, String> result = parseTextFileHelper(PARIS_BRUSSELS_LINES);
    assertEquals(2, result.size());
    assertEquals(PARIS_TZ, result.get(331));
    assertEquals(BRUSSELS_TZ, result.get(322));
  }

  public void testParseTextFileIgnoresComments() throws IOException {
    Map<Integer, String> result = parseTextFileHelper("# Hello\n" + PARIS_BRUSSELS_LINES);
    assertEquals(2, result.size());
    assertEquals(PARIS_TZ, result.get(331));
    assertEquals(BRUSSELS_TZ, result.get(322));
  }

  public void testParseTextFileIgnoresBlankLines() throws IOException {
    Map<Integer, String> result = parseTextFileHelper("\n" + PARIS_BRUSSELS_LINES);
    assertEquals(2, result.size());
    assertEquals(PARIS_TZ, result.get(331));
    assertEquals(BRUSSELS_TZ, result.get(322));
  }

  public void testParseTextFileIgnoresTrailingWhitespaces() throws IOException {
    Map<Integer, String> result = parseTextFileHelper(
        "331|" + PARIS_TZ + "\n322|" + BRUSSELS_TZ + "  \n");
    assertEquals(2, result.size());
    assertEquals(PARIS_TZ, result.get(331));
    assertEquals(BRUSSELS_TZ, result.get(322));
  }

  public void testParseTextFileThrowsExceptionWithMalformattedData() throws IOException {
    try {
      parseTextFileHelper("331");
      fail();
    } catch (RuntimeException e) {
      // Expected.
    }
  }

  public void testParseTextFileThrowsExceptionWithMissingTimeZone() throws IOException {
    try {
      parseTextFileHelper("331|");
      fail();
    } catch (RuntimeException e) {
      // Expected.
    }
  }

  // Returns a String representing the input after serialization and deserialization by
  // PrefixTimeZonesMap.
  private static String convertDataHelper(String input) throws IOException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input.getBytes());
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    SortedMap<Integer, String> prefixTimeZonesMapping = parseTextFileHelper(input);
    GenerateTimeZonesMapData.writeToBinaryFile(prefixTimeZonesMapping, byteArrayOutputStream);
    // The byte array output stream now contains the corresponding serialized prefix to time zones
    // map. Try to deserialize it and compare it with the initial input.
    PrefixTimeZonesMap prefixTimeZonesMap = new PrefixTimeZonesMap();
    prefixTimeZonesMap.readExternal(
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));

    return prefixTimeZonesMap.toString();
  }

  public void testConvertData() throws IOException {
    String input = PARIS_BRUSSELS_LINES;

    String dataAfterDeserialization = convertDataHelper(input);
    assertEquals(input, dataAfterDeserialization);
  }

  public void testConvertThrowsExceptionWithMissingTimeZone() throws IOException {
    String input = PARIS_BRUSSELS_LINES + "3341|\n";

    try {
      String dataAfterDeserialization = convertDataHelper(input);
    } catch (RuntimeException e) {
      // Expected.
    }
  }

  public void testConvertDataThrowsExceptionWithDuplicatedPrefixes() throws IOException {
    String input = "331|" + PARIS_TZ + "\n331|" + BRUSSELS_TZ + "\n";

    try {
      convertDataHelper(input);
      fail();
    } catch (RuntimeException e) {
      // Expected.
    }
  }
}
