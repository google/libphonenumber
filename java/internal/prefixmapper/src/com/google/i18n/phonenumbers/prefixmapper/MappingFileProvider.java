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

package com.google.i18n.phonenumbers.prefixmapper;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A utility which knows the data files that are available for the phone prefix mappers to use.
 * The data files contain mappings from phone number prefixes to text descriptions, and are
 * organized by country calling code and language that the text descriptions are in.
 *
 * @author Shaopeng Jia
 */
public class MappingFileProvider implements Externalizable {
  private int numOfEntries = 0;
  private int[] countryCallingCodes;
  private List<Set<String>> availableLanguages;
  private static final Map<String, String> LOCALE_NORMALIZATION_MAP;

  static {
    Map<String, String> normalizationMap = new HashMap<String, String>();
    normalizationMap.put("zh_TW", "zh_Hant");
    normalizationMap.put("zh_HK", "zh_Hant");
    normalizationMap.put("zh_MO", "zh_Hant");

    LOCALE_NORMALIZATION_MAP = Collections.unmodifiableMap(normalizationMap);
  }

  /**
   * Creates an empty {@link MappingFileProvider}. The default constructor is necessary for
   * implementing {@link Externalizable}. The empty provider could later be populated by
   * {@link #readFileConfigs(java.util.SortedMap)} or {@link #readExternal(java.io.ObjectInput)}.
   */
  public MappingFileProvider() {
  }

  /**
   * Initializes an {@link MappingFileProvider} with {@code availableDataFiles}.
   *
   * @param availableDataFiles  a map from country calling codes to sets of languages in which data
   *     files are available for the specific country calling code. The map is sorted in ascending
   *     order of the country calling codes as integers.
   */
  public void readFileConfigs(SortedMap<Integer, Set<String>> availableDataFiles) {
    numOfEntries = availableDataFiles.size();
    countryCallingCodes = new int[numOfEntries];
    availableLanguages = new ArrayList<Set<String>>(numOfEntries);
    int index = 0;
    for (int countryCallingCode : availableDataFiles.keySet()) {
      countryCallingCodes[index++] = countryCallingCode;
      availableLanguages.add(new HashSet<String>(availableDataFiles.get(countryCallingCode)));
    }
  }

  /**
   * Supports Java Serialization.
   */
  public void readExternal(ObjectInput objectInput) throws IOException {
    numOfEntries = objectInput.readInt();
    if (countryCallingCodes == null || countryCallingCodes.length < numOfEntries) {
      countryCallingCodes = new int[numOfEntries];
    }
    if (availableLanguages == null) {
      availableLanguages = new ArrayList<Set<String>>();
    }
    for (int i = 0; i < numOfEntries; i++) {
      countryCallingCodes[i] = objectInput.readInt();
      int numOfLangs = objectInput.readInt();
      Set<String> setOfLangs = new HashSet<String>();
      for (int j = 0; j < numOfLangs; j++) {
        setOfLangs.add(objectInput.readUTF());
      }
      availableLanguages.add(setOfLangs);
    }
  }

  /**
   * Supports Java Serialization.
   */
  public void writeExternal(ObjectOutput objectOutput) throws IOException {
    objectOutput.writeInt(numOfEntries);
    for (int i = 0; i < numOfEntries; i++) {
      objectOutput.writeInt(countryCallingCodes[i]);
      Set<String> setOfLangs = availableLanguages.get(i);
      int numOfLangs = setOfLangs.size();
      objectOutput.writeInt(numOfLangs);
      for (String lang : setOfLangs) {
        objectOutput.writeUTF(lang);
      }
    }
  }

  /**
   * Returns a string representing the data in this class. The string contains one line for each
   * country calling code. The country calling code is followed by a '|' and then a list of
   * comma-separated languages sorted in ascending order.
   */
  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    for (int i = 0; i < numOfEntries; i++) {
      output.append(countryCallingCodes[i]);
      output.append('|');
      SortedSet<String> sortedSetOfLangs = new TreeSet<String>(availableLanguages.get(i));
      for (String lang : sortedSetOfLangs) {
        output.append(lang);
        output.append(',');
      }
      output.append('\n');
    }
    return output.toString();
  }

  /**
   * Gets the name of the file that contains the mapping data for the {@code countryCallingCode} in
   * the language specified.
   *
   * @param countryCallingCode  the country calling code of phone numbers which the data file
   *     contains
   * @param language  two or three-letter lowercase ISO language codes as defined by ISO 639. Note
   *     that where two different language codes exist (e.g. 'he' and 'iw' for Hebrew) we use the
   *     one that Java/Android canonicalized on ('iw' in this case).
   * @param script  four-letter titlecase (the first letter is uppercase and the rest of the letters
   *     are lowercase) ISO script codes as defined in ISO 15924
   * @param region  two-letter uppercase ISO country codes as defined by ISO 3166-1
   * @return  the name of the file, or empty string if no such file can be found
   */
  String getFileName(int countryCallingCode, String language, String script, String region) {
    if (language.length() == 0) {
      return "";
    }
    int index = Arrays.binarySearch(countryCallingCodes, countryCallingCode);
    if (index < 0) {
      return "";
    }
    Set<String> setOfLangs = availableLanguages.get(index);
    if (setOfLangs.size() > 0) {
      String languageCode = findBestMatchingLanguageCode(setOfLangs, language, script, region);
      if (languageCode.length() > 0) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(countryCallingCode).append('_').append(languageCode);
        return fileName.toString();
      }
    }
    return "";
  }

  private String findBestMatchingLanguageCode(
      Set<String> setOfLangs, String language, String script, String region) {
    StringBuilder fullLocale = constructFullLocale(language, script, region);
    String fullLocaleStr = fullLocale.toString();
    String normalizedLocale = LOCALE_NORMALIZATION_MAP.get(fullLocaleStr);
    if (normalizedLocale != null) {
      if (setOfLangs.contains(normalizedLocale)) {
        return normalizedLocale;
      }
    }
    if (setOfLangs.contains(fullLocaleStr)) {
      return fullLocaleStr;
    }

    if (onlyOneOfScriptOrRegionIsEmpty(script, region)) {
      if (setOfLangs.contains(language)) {
        return language;
      }
    } else if (script.length() > 0 && region.length() > 0) {
      StringBuilder langWithScript = new StringBuilder(language).append('_').append(script);
      String langWithScriptStr = langWithScript.toString();
      if (setOfLangs.contains(langWithScriptStr)) {
        return langWithScriptStr;
      }

      StringBuilder langWithRegion = new StringBuilder(language).append('_').append(region);
      String langWithRegionStr = langWithRegion.toString();
      if (setOfLangs.contains(langWithRegionStr)) {
        return langWithRegionStr;
      }

      if (setOfLangs.contains(language)) {
        return language;
      }
    }
    return "";
  }

  private boolean onlyOneOfScriptOrRegionIsEmpty(String script, String region) {
    return (script.length() == 0 && region.length() > 0)
        || (region.length() == 0 && script.length() > 0);
  }

  private StringBuilder constructFullLocale(String language, String script, String region) {
    StringBuilder fullLocale = new StringBuilder(language);
    appendSubsequentLocalePart(script, fullLocale);
    appendSubsequentLocalePart(region, fullLocale);
    return fullLocale;
  }

  private void appendSubsequentLocalePart(String subsequentLocalePart, StringBuilder fullLocale) {
    if (subsequentLocalePart.length() > 0) {
      fullLocale.append('_').append(subsequentLocalePart);
    }
  }
}
