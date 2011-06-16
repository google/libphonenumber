/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.i18n.phonenumbers.geocoding;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A utility that maps phone number prefixes to a string describing the geographical area the prefix
 * covers.
 *
 * @author Shaopeng Jia
 */
public class AreaCodeMap implements Externalizable {
  private int numOfEntries = 0;
  private TreeSet<Integer> possibleLengths = new TreeSet<Integer>();
  private int[] phoneNumberPrefixes;
  private String[] descriptions;
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

  /**
   * Creates an empty {@link AreaCodeMap}. The default constructor is necessary for implementing
   * {@link Externalizable}. The empty map could later populated by
   * {@link #readAreaCodeMap(java.util.SortedMap)} or {@link #readExternal(java.io.ObjectInput)}.
   */
  public AreaCodeMap() {}

  /**
   * Creates an {@link AreaCodeMap} initialized with {@code sortedAreaCodeMap}.
   *
   * @param sortedAreaCodeMap  a map from phone number prefixes to descriptions of corresponding
   *     geographical areas, sorted in ascending order of the phone number prefixes as integers.
   */
  public void readAreaCodeMap(SortedMap<Integer, String> sortedAreaCodeMap) {
    numOfEntries = sortedAreaCodeMap.size();
    phoneNumberPrefixes = new int[numOfEntries];
    descriptions = new String[numOfEntries];
    int index = 0;
    for (int prefix : sortedAreaCodeMap.keySet()) {
      phoneNumberPrefixes[index++] = prefix;
      possibleLengths.add((int) Math.log10(prefix) + 1);
    }
    sortedAreaCodeMap.values().toArray(descriptions);
  }

  /**
   * Supports Java Serialization.
   */
  public void readExternal(ObjectInput objectInput) throws IOException {
    numOfEntries = objectInput.readInt();
    if (phoneNumberPrefixes == null || phoneNumberPrefixes.length < numOfEntries) {
      phoneNumberPrefixes = new int[numOfEntries];
    }
    if (descriptions == null || descriptions.length < numOfEntries) {
      descriptions = new String[numOfEntries];
    }
    for (int i = 0; i < numOfEntries; i++) {
      phoneNumberPrefixes[i] = objectInput.readInt();
      descriptions[i] = objectInput.readUTF();
    }
    int sizeOfLengths = objectInput.readInt();
    possibleLengths.clear();
    for (int i = 0; i < sizeOfLengths; i++) {
      possibleLengths.add(objectInput.readInt());
    }
  }

  /**
   * Supports Java Serialization.
   */
  public void writeExternal(ObjectOutput objectOutput) throws IOException {
    objectOutput.writeInt(numOfEntries);
    for (int i = 0; i < numOfEntries; i++) {
      objectOutput.writeInt(phoneNumberPrefixes[i]);
      objectOutput.writeUTF(descriptions[i]);
    }
    int sizeOfLengths = possibleLengths.size();
    objectOutput.writeInt(sizeOfLengths);
    for (Integer length : possibleLengths) {
      objectOutput.writeInt(length);
    }
  }

  /**
   * Returns the description of the geographical area the {@code number} corresponds to.
   *
   * @param number  the phone number to look up
   * @return  the description of the geographical area
   */
  String lookup(PhoneNumber number) {
    if (numOfEntries == 0) {
      return "";
    }
    long phonePrefix =
        Long.parseLong(number.getCountryCode() + phoneUtil.getNationalSignificantNumber(number));
    int currentIndex = numOfEntries - 1;
    SortedSet<Integer> currentSetOfLengths = possibleLengths;
    while (currentSetOfLengths.size() > 0) {
      Integer possibleLength = currentSetOfLengths.last();
      String phonePrefixStr = String.valueOf(phonePrefix);
      if (phonePrefixStr.length() > possibleLength) {
        phonePrefix = Long.parseLong(phonePrefixStr.substring(0, possibleLength));
      }
      currentIndex = binarySearch(0, currentIndex, phonePrefix);
      if (currentIndex < 0) {
        return "";
      }
      if (phonePrefix == phoneNumberPrefixes[currentIndex]) {
        return descriptions[currentIndex];
      }
      currentSetOfLengths = possibleLengths.headSet(possibleLength);
    }
    return "";
  }

  /**
   * Does a binary search for {@code value} in the phoneNumberPrefixes array from {@code start} to
   * {@code end} (inclusive). Returns the position if {@code value} is found; otherwise, returns the
   * position which has the largest value that is less than {@code value}. This means if
   * {@code value} is the smallest, -1 will be returned.
   */
  private int binarySearch(int start, int end, long value) {
    int current = 0;
    while (start <= end) {
      current = (start + end) / 2;
      if (phoneNumberPrefixes[current] == value) {
        return current;
      } else if (phoneNumberPrefixes[current] > value) {
        current--;
        end = current;
      } else {
        start = current + 1;
      }
    }
    return current;
  }

  /**
   * Dumps the mappings contained in the area code map.
   */
  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    for (int i = 0; i < numOfEntries; i++) {
      output.append(phoneNumberPrefixes[i]);
      output.append("|");
      output.append(descriptions[i]);
      output.append("\n");
    }
    return output.toString();
  }
}
