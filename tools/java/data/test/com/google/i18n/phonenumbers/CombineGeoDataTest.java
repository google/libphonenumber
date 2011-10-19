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

package com.google.i18n.phonenumbers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.i18n.phonenumbers.CombineGeoData.Range;

import org.junit.Test;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Unit tests for CombineGeoData class.
 *
 * @author Philippe Liard
 */
public class CombineGeoDataTest {
  @Test
  public void createSortedPrefixArray() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("122", null);
    phonePrefixMap.put("42", null);
    phonePrefixMap.put("4012", null);
    phonePrefixMap.put("1000", null);

    String[] sortedPrefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    assertEquals("1000", sortedPrefixes[0]);
    assertEquals("122", sortedPrefixes[1]);
    assertEquals("4012", sortedPrefixes[2]);
    assertEquals("42", sortedPrefixes[3]);
  }

  @Test
  public void findRangeEndFromStart() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33130", "Paris");
    phonePrefixMap.put("33139", "Paris");
    phonePrefixMap.put("334", "Marseille");

    String[] prefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    int rangeEnd = CombineGeoData.findRangeEnd(prefixes, phonePrefixMap, 0);
    assertEquals(1, rangeEnd);
  }

  @Test
  public void findRangeEndFromMiddle() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33130", "Paris");
    phonePrefixMap.put("33139", "Paris");
    phonePrefixMap.put("3341", "Marseille");
    phonePrefixMap.put("3342", "Marseille");

    String[] prefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    int rangeEnd = CombineGeoData.findRangeEnd(prefixes, phonePrefixMap, 2);
    assertEquals(3, rangeEnd);
  }

  @Test
  public void findRangeEndWithSameLocationButDifferentPrefix() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33130", "Paris");
    phonePrefixMap.put("3314", "Paris");
    phonePrefixMap.put("3341", "Marseille");
    phonePrefixMap.put("3342", "Marseille");

    String[] prefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    int rangeEnd = CombineGeoData.findRangeEnd(prefixes, phonePrefixMap, 0);
    assertEquals(0, rangeEnd);
  }

  @Test
  public void createRanges() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33120", "Paris");
    phonePrefixMap.put("33130", "Paris");
    phonePrefixMap.put("33139", "Paris");
    phonePrefixMap.put("3341", "Marseille");
    phonePrefixMap.put("3342", "Marseille");

    String[] prefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    List<Range> ranges = CombineGeoData.createRanges(prefixes, phonePrefixMap);
    assertEquals(3, ranges.size());
    assertEquals(0, ranges.get(0).start);
    assertEquals(0, ranges.get(0).end);
    assertEquals(1, ranges.get(1).start);
    assertEquals(2, ranges.get(1).end);
    assertEquals(3, ranges.get(2).start);
    assertEquals(4, ranges.get(2).end);
  }

  @Test
  public void findConflict() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33130", "Saint Germain en Laye");
    phonePrefixMap.put("33132", "Paris");
    phonePrefixMap.put("33139", "Paris");

    String[] prefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    assertTrue(CombineGeoData.findConflict(prefixes, 3313, 0, 0));
  }

  @Test
  public void conflictBefore() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33130", "Saint Germain en Laye");
    phonePrefixMap.put("33132", "Paris");
    phonePrefixMap.put("33139", "Paris");
    phonePrefixMap.put("3341", "Marseille");
    phonePrefixMap.put("3342", "Marseille");

    String[] prefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    List<Range> ranges = CombineGeoData.createRanges(prefixes, phonePrefixMap);
    assertTrue(CombineGeoData.hasConflict(ranges, 1, prefixes, 3313));
  }

  @Test
  public void conflictAfter() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33122", "Poissy");
    phonePrefixMap.put("33132", "Paris");
    phonePrefixMap.put("33138", "Paris");
    phonePrefixMap.put("33139", "Saint Germain en Laye");
    phonePrefixMap.put("3341", "Marseille");
    phonePrefixMap.put("3342", "Marseille");

    String[] prefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    List<Range> ranges = CombineGeoData.createRanges(prefixes, phonePrefixMap);
    assertEquals(4, ranges.size());
    assertTrue(CombineGeoData.hasConflict(ranges, 1, prefixes, 3313));
  }

  @Test
  public void noConflict() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33122", "Poissy");
    phonePrefixMap.put("33132", "Paris");
    phonePrefixMap.put("33138", "Paris");
    phonePrefixMap.put("33149", "Saint Germain en Laye");
    phonePrefixMap.put("3341", "Marseille");
    phonePrefixMap.put("3342", "Marseille");

    String[] prefixes = CombineGeoData.createSortedPrefixArray(phonePrefixMap);
    List<Range> ranges = CombineGeoData.createRanges(prefixes, phonePrefixMap);
    assertEquals(4, ranges.size());
    assertFalse(CombineGeoData.hasConflict(ranges, 1, prefixes, 3313));
  }

  @Test
  public void combineRemovesLastDigit() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33122", "Poissy");
    phonePrefixMap.put("33132", "Paris");
    phonePrefixMap.put("33149", "Saint Germain en Laye");
    phonePrefixMap.put("3342", "Marseille");

    phonePrefixMap = CombineGeoData.combine(phonePrefixMap);
    assertEquals(4, phonePrefixMap.size());
    assertEquals("Poissy", phonePrefixMap.get("3312"));
    assertEquals("Paris", phonePrefixMap.get("3313"));
    assertEquals("Saint Germain en Laye", phonePrefixMap.get("3314"));
    assertEquals("Marseille", phonePrefixMap.get("334"));
  }

  @Test
  public void combineMergesSamePrefixAndLocation() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33132", "Paris");
    phonePrefixMap.put("33133", "Paris");
    phonePrefixMap.put("33134", "Paris");

    phonePrefixMap = CombineGeoData.combine(phonePrefixMap);
    assertEquals(1, phonePrefixMap.size());
    assertEquals("Paris", phonePrefixMap.get("3313"));
  }

  @Test
  public void combineWithNoPossibleCombination() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("3312", "Poissy");
    phonePrefixMap.put("3313", "Paris");
    phonePrefixMap.put("3314", "Saint Germain en Laye");

    phonePrefixMap = CombineGeoData.combine(phonePrefixMap);
    assertEquals(3, phonePrefixMap.size());
    assertEquals("Poissy", phonePrefixMap.get("3312"));
    assertEquals("Paris", phonePrefixMap.get("3313"));
    assertEquals("Saint Germain en Laye", phonePrefixMap.get("3314"));
  }

  @Test
  public void combineMultipleTimes() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("33132", "Paris");
    phonePrefixMap.put("33133", "Paris");
    phonePrefixMap.put("33134", "Paris");

    phonePrefixMap = CombineGeoData.combineMultipleTimes(phonePrefixMap);
    assertEquals(1, phonePrefixMap.size());
    assertEquals("Paris", phonePrefixMap.get("3"));
  }

  @Test
  public void combineMultipleTimesWithPrefixesWithDifferentLengths() {
    SortedMap<String, String> phonePrefixMap = new TreeMap<String, String>();
    phonePrefixMap.put("332", "Paris");
    phonePrefixMap.put("33133", "Paris");
    phonePrefixMap.put("41", "Marseille");

    phonePrefixMap = CombineGeoData.combineMultipleTimes(phonePrefixMap);
    assertEquals(2, phonePrefixMap.size());
    assertEquals("Paris", phonePrefixMap.get("3"));
    assertEquals("Marseille", phonePrefixMap.get("4"));
  }
}
