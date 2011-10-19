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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Utility class that makes the geocoding data as small as possible. This class assumes the
 * geocoding data provided as input doesn't contain any gaps thus should not be used with incomplete
 * data (missing prefixes).
 * <pre>
 * Example:        Can be combined as:
 *   33131|Paris     331|Paris
 *   33132|Paris     334|Marseille
 *   3341|Marseille
 * </pre>
 *
 * @author Philippe Liard
 */
public class CombineGeoData {
  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final String outputLineSeparator;
  private static final Logger LOGGER = Logger.getLogger(CombineGeoData.class.getName());

  public CombineGeoData(InputStream inputStream, OutputStream outputStream, String lineSeparator) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    this.outputLineSeparator = lineSeparator;
  }

  public CombineGeoData(InputStream inputStream, OutputStream outputStream) {
    this(inputStream, outputStream, System.getProperty("line.separator"));
  }

  /**
   * Utility class that contains two indexes (start and end).
   */
  static class Range {
    public final int start;
    public final int end;

    public Range(int start, int end) {
      this.start = start;
      this.end = end;
    }
  }

  /**
   * Parses the input text file expected to contain lines written as 'prefix|description'. Note that
   * description can be empty.
   *
   * @return the map of phone prefix data parsed.
   * @throws IOException
   */
  private SortedMap<String, String> parseInput() throws IOException {
    SortedMap<String, String> outputMap = new TreeMap<String, String>();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

    for (String line; (line = bufferedReader.readLine()) != null; ) {
      int indexOfPipe = line.indexOf('|');
      if (indexOfPipe == -1) {
        continue;
      }
      outputMap.put(line.substring(0, indexOfPipe), line.substring(indexOfPipe + 1));
    }
    return outputMap;
  }

  /**
   * Creates a sorted array of phone number prefixes as strings from the provided phone number
   * prefix map.
   *
   * @return the array of phone number prefixes sorted by string.
   */
  static String[] createSortedPrefixArray(SortedMap<String, String> phonePrefixMap) {
    String[] sortedPrefixes = new String[phonePrefixMap.size()];
    phonePrefixMap.keySet().toArray(sortedPrefixes);
    return sortedPrefixes;
  }

  /**
   * Finds the end index of the range of phone number prefixes starting at the provided index.
   * A range ends when a different description or prefix divided by 10 is encountered.
   *
   * @param prefixes  the array of phone number prefixes sorted by string
   * @param phonePrefixMap  the map associating phone number prefixes and descriptions
   * @param start  the start index of the prefixes array
   * @return  the index of the end of the range starting at the provided index
   */
  static int findRangeEnd(String[] prefixes, Map<String, String> phonePrefixMap, int start) {
    String previousPrefix = prefixes[start];
    int previousPrefixAsInt = Integer.parseInt(previousPrefix);
    String previousLocation = phonePrefixMap.get(previousPrefix);

    for (int i = start; i < prefixes.length; i++) {
      String currentPrefix = prefixes[i];
      String currentLocation = phonePrefixMap.get(currentPrefix);
      if (!currentLocation.equals(previousLocation) ||
          (Integer.parseInt(currentPrefix) / 10 != previousPrefixAsInt / 10)) {
        return i - 1;
      }
    }
    return prefixes.length - 1;
  }

  /**
   * Splits the provided array of prefixes into an array of ranges. A range contains the start and
   * end indexes of a set of mappings that share the same description and have the same prefix minus
   * the last digit.
   *
   * @param prefixes  the array of phone number prefixes sorted by string
   * @param phonePrefixMap  the map associating phone number prefixes and descriptions
   * @return  the list of ranges
   */
  static List<Range> createRanges(String[] prefixes, Map<String, String> phonePrefixMap) {
    List<Range> ranges = new ArrayList<Range>();
    int index = 0;
    int phonePrefixMapSize = phonePrefixMap.size();

    while (index < phonePrefixMapSize) {
      int rangeEnd = findRangeEnd(prefixes, phonePrefixMap, index);
      ranges.add(new Range(index, rangeEnd));
      index = rangeEnd + 1;
    }
    return ranges;
  }

  /**
   * Checks whether the provided candidate prefix conflicts with the prefixes contained in the
   * provided range. A conflict occurs if the provided prefix covers (is a prefix of) one of the
   * prefixes contained in the range.
   *
   * @param prefixes  the array of phone number prefixes sorted by string
   * @param candidate  the candidate phone number prefix
   * @param start  the start of the range
   * @param end  the end of the range
   * @return  whether the candidate prefix conflicts with the prefixes contained in the range
   */
  static boolean findConflict(String[] prefixes, int candidate, int start, int end) {
    String candidateAsString = String.valueOf(candidate);
    for (int i = start; i <= end; i++) {
      String prefix = prefixes[i];
      if (prefix.startsWith(candidateAsString)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether the provided candidate prefix conflicts with the prefixes contained in the
   * ranges adjacent (before and after the provided range) of the provided range.
   *
   * @param ranges  the list of ranges
   * @param rangeIndex  the index of the range in which the conflict search occurs
   * @param prefixes  the array of phone number prefixes sorted by string
   * @param candidate  the candidate phone number prefix
   * @return  whether a conflict was found in the provided range
   */
  static boolean hasConflict(List<Range> ranges, int rangeIndex, String[] prefixes, int candidate) {
    if (rangeIndex > 0) {
      Range previousRange = ranges.get(rangeIndex - 1);
      if (findConflict(prefixes, candidate, previousRange.start, previousRange.end)) {
        return true;
      }
    }
    if (rangeIndex < ranges.size() - 1) {
      Range nextRange = ranges.get(rangeIndex + 1);
      if (findConflict(prefixes, candidate, nextRange.start, nextRange.end)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Combines the mappings contained in the provided map. A new combined map is returned as a result
   * in case any combination occurred. Otherwise (if no combination occurred) the same map (same
   * identity) is returned. Note that this method performs a 'single step' (i.e performs only one
   * combination iteration).
   */
  static SortedMap<String, String> combine(SortedMap<String, String> phonePrefixMap) {
    String[] prefixes = createSortedPrefixArray(phonePrefixMap);
    List<Range> ranges = createRanges(prefixes, phonePrefixMap);
    Map<Integer /* range index */, Integer> combinedPrefixes = new HashMap<Integer, Integer>();
    int rangeIndex = 0;

    for (Range range : ranges) {
      int prefixCandidate = Integer.parseInt(prefixes[range.start]) / 10;
      if (prefixCandidate != 0 && !hasConflict(ranges, rangeIndex, prefixes, prefixCandidate)) {
        combinedPrefixes.put(rangeIndex, prefixCandidate);
      }
      ++rangeIndex;
    }
    if (combinedPrefixes.size() == 0) {
      return phonePrefixMap;
    }
    SortedMap<String, String> combinedMap = new TreeMap<String, String>();
    rangeIndex = 0;
    for (Range range : ranges) {
      Integer combinedRange = combinedPrefixes.get(rangeIndex++);
      if (combinedRange != null) {
        String firstPrefixOfRange = prefixes[range.start];
        combinedMap.put(String.valueOf(combinedRange), phonePrefixMap.get(firstPrefixOfRange));
      } else {
        for (int i = range.start; i <= range.end; i++) {
          String prefix = prefixes[i];
          combinedMap.put(prefix, phonePrefixMap.get(prefix));
        }
      }
    }
    return combinedMap;
  }

  /**
   * Combines the provided map associating phone number prefixes and descriptions.
   *
   * @return  the combined map
   */
  static SortedMap<String, String> combineMultipleTimes(SortedMap<String, String> phonePrefixMap) {
    SortedMap<String, String> previousMap = null;
    while (phonePrefixMap != previousMap) {
      previousMap = phonePrefixMap;
      phonePrefixMap = combine(phonePrefixMap);
    }
    return phonePrefixMap;
  }

  /**
   * Combines the geocoding data read from the provided input stream and writes it as a result to
   * the provided output stream. Uses the provided string as the line separator.
   */
  public void run() throws IOException {
    SortedMap<String, String> phonePrefixMap = parseInput();
    phonePrefixMap = combineMultipleTimes(phonePrefixMap);
    PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(outputStream));
    for (Map.Entry<String, String> mapping : phonePrefixMap.entrySet()) {
      printWriter.printf("%s|%s%s", mapping.getKey(), mapping.getValue(), outputLineSeparator);
    }
    printWriter.flush();
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      LOGGER.severe("usage: java -jar combine-geodata.jar /path/to/input /path/to/output");
      System.exit(1);
    }
    try {
      CombineGeoData combineGeoData =
          new CombineGeoData(new FileInputStream(args[0]), new FileOutputStream(args[1]));
      combineGeoData.run();
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      System.exit(1);
    }
  }
}
