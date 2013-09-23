/*
 * Copyright (C) 2011 The Libphonenumber Authors
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

import com.google.i18n.phonenumbers.buildtools.GeneratePhonePrefixData.PhonePrefixMappingHandler;
import com.google.i18n.phonenumbers.prefixmapper.MappingFileProvider;
import com.google.i18n.phonenumbers.prefixmapper.PhonePrefixMap;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Unittests for GeneratePhonePrefixData.java
 *
 * @author Philippe Liard
 */
public class GeneratePhonePrefixDataTest extends TestCase {
  private static final SortedMap<Integer, Set<String>> AVAILABLE_DATA_FILES;
  static {
    SortedMap<Integer, Set<String>> temporaryMap = new TreeMap<Integer, Set<String>>();

    // Languages for US.
    GeneratePhonePrefixData.addConfigurationMapping(temporaryMap, new File("1_en"));
    GeneratePhonePrefixData.addConfigurationMapping(temporaryMap, new File("1_en_US"));
    GeneratePhonePrefixData.addConfigurationMapping(temporaryMap, new File("1_es"));

    // Languages for France.
    GeneratePhonePrefixData.addConfigurationMapping(temporaryMap, new File("33_fr"));
    GeneratePhonePrefixData.addConfigurationMapping(temporaryMap, new File("33_en"));

    // Languages for China.
    GeneratePhonePrefixData.addConfigurationMapping(temporaryMap, new File("86_zh_Hans"));

    AVAILABLE_DATA_FILES = Collections.unmodifiableSortedMap(temporaryMap);
  }

  public void testAddConfigurationMapping() {
    assertEquals(3, AVAILABLE_DATA_FILES.size());

    Set<String> languagesForUS = AVAILABLE_DATA_FILES.get(1);
    assertEquals(3, languagesForUS.size());
    assertTrue(languagesForUS.contains("en"));
    assertTrue(languagesForUS.contains("en_US"));
    assertTrue(languagesForUS.contains("es"));

    Set<String> languagesForFR = AVAILABLE_DATA_FILES.get(33);
    assertEquals(2, languagesForFR.size());
    assertTrue(languagesForFR.contains("fr"));
    assertTrue(languagesForFR.contains("en"));

    Set<String> languagesForCN = AVAILABLE_DATA_FILES.get(86);
    assertEquals(1, languagesForCN.size());
    assertTrue(languagesForCN.contains("zh_Hans"));
  }

  public void testOutputBinaryConfiguration() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    GeneratePhonePrefixData.outputBinaryConfiguration(AVAILABLE_DATA_FILES, byteArrayOutputStream);
    MappingFileProvider mappingFileProvider = new MappingFileProvider();
    mappingFileProvider.readExternal(
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
    assertEquals("1|en,en_US,es,\n33|en,fr,\n86|zh_Hans,\n", mappingFileProvider.toString());
  }

  private static Map<Integer, String> parseTextFileHelper(String input) throws IOException {
    final Map<Integer, String> mappings = new HashMap<Integer, String>();
    GeneratePhonePrefixData.parseTextFile(new ByteArrayInputStream(input.getBytes()),
                                          new PhonePrefixMappingHandler() {
      @Override
      public void process(int phonePrefix, String location) {
        mappings.put(phonePrefix, location);
      }
    });
    return mappings;
  }

  public void testParseTextFile() throws IOException {
    Map<Integer, String> result = parseTextFileHelper("331|Paris\n334|Marseilles\n");
    assertEquals(2, result.size());
    assertEquals("Paris", result.get(331));
    assertEquals("Marseilles", result.get(334));
  }

  public void testParseTextFileIgnoresComments() throws IOException {
    Map<Integer, String> result = parseTextFileHelper("# Hello\n331|Paris\n334|Marseilles\n");
    assertEquals(2, result.size());
    assertEquals("Paris", result.get(331));
    assertEquals("Marseilles", result.get(334));
  }

  public void testParseTextFileIgnoresBlankLines() throws IOException {
    Map<Integer, String> result = parseTextFileHelper("\n331|Paris\n334|Marseilles\n");
    assertEquals(2, result.size());
    assertEquals("Paris", result.get(331));
    assertEquals("Marseilles", result.get(334));
  }

  public void testParseTextFileIgnoresTrailingWhitespaces() throws IOException {
    Map<Integer, String> result = parseTextFileHelper("331|Paris  \n334|Marseilles  \n");
    assertEquals(2, result.size());
    assertEquals("Paris", result.get(331));
    assertEquals("Marseilles", result.get(334));
  }

  public void testParseTextFileThrowsExceptionWithMalformattedData() throws IOException {
    try {
      parseTextFileHelper("331");
      fail();
    } catch (RuntimeException e) {
      // Expected.
    }
  }

  public void testParseTextFileAcceptsMissingLocation() throws IOException {
    parseTextFileHelper("331|");
  }

  public void testSplitMap() {
    SortedMap<Integer, String> mappings = new TreeMap<Integer, String>();
    List<File> outputFiles = Arrays.asList(new File("1201_en"), new File("1202_en"));
    mappings.put(12011, "Location1");
    mappings.put(12012, "Location2");
    mappings.put(12021, "Location3");
    mappings.put(12022, "Location4");

    Map<File, SortedMap<Integer, String>> splitMaps =
        GeneratePhonePrefixData.splitMap(mappings, outputFiles);
    assertEquals(2, splitMaps.size());
    assertEquals("Location1", splitMaps.get(new File("1201_en")).get(12011));
    assertEquals("Location2", splitMaps.get(new File("1201_en")).get(12012));
    assertEquals("Location3", splitMaps.get(new File("1202_en")).get(12021));
    assertEquals("Location4", splitMaps.get(new File("1202_en")).get(12022));
  }

  private static String convertDataHelper(String input) throws IOException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input.getBytes());
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    SortedMap<Integer, String> phonePrefixMappings =
        GeneratePhonePrefixData.readMappingsFromTextFile(byteArrayInputStream);
    GeneratePhonePrefixData.writeToBinaryFile(phonePrefixMappings, byteArrayOutputStream);
    // The byte array output stream now contains the corresponding serialized phone prefix map. Try
    // to deserialize it and compare it with the initial input.
    PhonePrefixMap phonePrefixMap = new PhonePrefixMap();
    phonePrefixMap.readExternal(
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));

    return phonePrefixMap.toString();
  }

  public void testConvertData() throws IOException {
    String input = "331|Paris\n334|Marseilles\n";

    String dataAfterDeserialization = convertDataHelper(input);
    assertEquals(input, dataAfterDeserialization);
  }

  public void testConvertDataSupportsEmptyDescription() throws IOException {
    String input = "331|Paris\n334|Marseilles\n3341|\n";

    String dataAfterDeserialization = convertDataHelper(input);
    assertEquals(3, dataAfterDeserialization.split("\n").length);
    assertEquals(input, dataAfterDeserialization);
  }

  public void testConvertDataThrowsExceptionWithDuplicatedPhonePrefixes() throws IOException {
    String input = "331|Paris\n331|Marseilles\n";

    try {
      convertDataHelper(input);
      fail();
    } catch (RuntimeException e) {
      // Expected.
    }
  }

  public void testGetEnglishDataPath() {
    assertEquals("/path/en/33.txt", GeneratePhonePrefixData.getEnglishDataPath("/path/fr/33.txt"));
  }

  public void testHasOverlap() {
    SortedMap<Integer, String> map = new TreeMap<Integer, String>();
    map.put(1234, "");
    map.put(123, "");
    map.put(2345, "");

    assertTrue(GeneratePhonePrefixData.hasOverlappingPrefix(1234, map));
    assertFalse(GeneratePhonePrefixData.hasOverlappingPrefix(2345, map));
  }

  public void testCompressAccordingToEnglishDataMakesDescriptionEmpty() {
    SortedMap<Integer, String> frenchMappings = new TreeMap<Integer, String>();
    frenchMappings.put(411, "Genève");
    frenchMappings.put(4112, "Zurich");

    SortedMap<Integer, String> englishMappings = new TreeMap<Integer, String>();
    englishMappings.put(411, "Geneva");
    englishMappings.put(4112, "Zurich");
    // The English map should not be modified.
    englishMappings = Collections.unmodifiableSortedMap(englishMappings);

    GeneratePhonePrefixData.compressAccordingToEnglishData(englishMappings, frenchMappings);

    assertEquals(2, frenchMappings.size());
    assertEquals("Genève", frenchMappings.get(411));
    assertEquals("", frenchMappings.get(4112));
  }

  public void testCompressAccordingToEnglishDataRemovesMappingWhenNoOverlap() {
    SortedMap<Integer, String> frenchMappings = new TreeMap<Integer, String>();
    frenchMappings.put(411, "Genève");
    frenchMappings.put(412, "Zurich");

    SortedMap<Integer, String> englishMappings = new TreeMap<Integer, String>();
    englishMappings.put(411, "Geneva");
    englishMappings.put(412, "Zurich");
    // The English map should not be modified.
    englishMappings = Collections.unmodifiableSortedMap(englishMappings);

    GeneratePhonePrefixData.compressAccordingToEnglishData(englishMappings, frenchMappings);

    assertEquals(1, frenchMappings.size());
    assertEquals("Genève", frenchMappings.get(411));
  }

  public void testCompressAccordingToEnglishData() {
    SortedMap<Integer, String> frenchMappings = new TreeMap<Integer, String>();
    frenchMappings.put(12, "A");
    frenchMappings.put(123, "B");

    SortedMap<Integer, String> englishMappings = new TreeMap<Integer, String>();
    englishMappings.put(12, "A");
    englishMappings.put(123, "B");
    // The English map should not be modified.
    englishMappings = Collections.unmodifiableSortedMap(englishMappings);

    GeneratePhonePrefixData.compressAccordingToEnglishData(englishMappings, frenchMappings);

    assertEquals(0, frenchMappings.size());
  }

  public void testRemoveEmptyEnglishMappingsDoesNotRemoveNonEnglishMappings() {
    SortedMap<Integer, String> frenchMappings = new TreeMap<Integer, String>();
    frenchMappings.put(331, "Paris");
    frenchMappings.put(334, "");
    // The French map should not be modified.
    frenchMappings = Collections.unmodifiableSortedMap(frenchMappings);

    GeneratePhonePrefixData.removeEmptyEnglishMappings(frenchMappings, "fr");

    assertEquals(2, frenchMappings.size());
  }

  public void testRemoveEmptyEnglishMappings() {
    SortedMap<Integer, String> englishMappings = new TreeMap<Integer, String>();
    englishMappings.put(331, "Paris");
    englishMappings.put(334, "");

    GeneratePhonePrefixData.removeEmptyEnglishMappings(englishMappings, "en");

    assertEquals(1, englishMappings.size());
    assertEquals("Paris", englishMappings.get(331));
  }
}
