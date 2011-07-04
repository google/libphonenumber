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

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.logging.Logger;

/**
 * A utility that maps phone number prefixes to a string describing the geographical area the prefix
 * covers.
 *
 * @author Shaopeng Jia
 */
public class AreaCodeMap implements Externalizable {
  private final int countryCallingCode;
  private final boolean isLeadingZeroPossible;
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private static final Logger LOGGER = Logger.getLogger(AreaCodeMap.class.getName());

  private AreaCodeMapStorageStrategy areaCodeMapStorage;

  // @VisibleForTesting
  AreaCodeMapStorageStrategy getAreaCodeMapStorage() {
    return areaCodeMapStorage;
  }

  /**
   * Creates an empty {@link AreaCodeMap}. The default constructor is necessary for implementing
   * {@link Externalizable}. The empty map could later populated by
   * {@link #readAreaCodeMap(java.util.SortedMap)} or {@link #readExternal(java.io.ObjectInput)}.
   *
   * @param countryCallingCode  the country calling code for the region that the area code map
   *     belongs to.
   */
  public AreaCodeMap(int countryCallingCode) {
    this.countryCallingCode = countryCallingCode;
    isLeadingZeroPossible = phoneUtil.isLeadingZeroPossible(countryCallingCode);
  }

  /**
   * Gets the size of the provided area code map storage. The map storage passed-in will be filled
   * as a result.
   */
  private static int getSizeOfAreaCodeMapStorage(AreaCodeMapStorageStrategy mapStorage,
      SortedMap<Integer, String> areaCodeMap) throws IOException {
    mapStorage.readFromSortedMap(areaCodeMap);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    mapStorage.writeExternal(objectOutputStream);
    objectOutputStream.flush();
    int sizeOfStorage = byteArrayOutputStream.size();
    objectOutputStream.close();
    return sizeOfStorage;
  }

  private AreaCodeMapStorageStrategy createDefaultMapStorage() {
    return new DefaultMapStorage(countryCallingCode, isLeadingZeroPossible);
  }

  private AreaCodeMapStorageStrategy createFlyweightMapStorage() {
    return new FlyweightMapStorage(countryCallingCode, isLeadingZeroPossible);
  }

  /**
   * Gets the smaller area code map storage strategy according to the provided area code map. It
   * actually uses (outputs the data to a stream) both strategies and retains the best one which
   * make this method quite expensive.
   */
  // @VisibleForTesting
  AreaCodeMapStorageStrategy getSmallerMapStorage(SortedMap<Integer, String> areaCodeMap) {
    try {
      AreaCodeMapStorageStrategy flyweightMapStorage = createFlyweightMapStorage();
      int sizeOfFlyweightMapStorage = getSizeOfAreaCodeMapStorage(flyweightMapStorage, areaCodeMap);

      AreaCodeMapStorageStrategy defaultMapStorage = createDefaultMapStorage();
      int sizeOfDefaultMapStorage = getSizeOfAreaCodeMapStorage(defaultMapStorage, areaCodeMap);

      return sizeOfFlyweightMapStorage < sizeOfDefaultMapStorage
          ? flyweightMapStorage : defaultMapStorage;
    } catch (IOException e) {
      LOGGER.severe(e.getMessage());
      return createFlyweightMapStorage();
    }
  }

  /**
   * Creates an {@link AreaCodeMap} initialized with {@code sortedAreaCodeMap}.  Note that the
   * underlying implementation of this method is expensive thus should not be called by
   * time-critical applications.
   *
   * @param sortedAreaCodeMap  a map from phone number prefixes to descriptions of corresponding
   *     geographical areas, sorted in ascending order of the phone number prefixes as integers.
   */
  public void readAreaCodeMap(SortedMap<Integer, String> sortedAreaCodeMap) {
    areaCodeMapStorage = getSmallerMapStorage(sortedAreaCodeMap);
  }

  /**
   * Supports Java Serialization.
   */
  public void readExternal(ObjectInput objectInput) throws IOException {
    // Read the area code map storage strategy flag.
    boolean useFlyweightMapStorage = objectInput.readBoolean();
    if (useFlyweightMapStorage) {
      areaCodeMapStorage = new FlyweightMapStorage(countryCallingCode, isLeadingZeroPossible);
    } else {
      areaCodeMapStorage = new DefaultMapStorage(countryCallingCode, isLeadingZeroPossible);
    }
    areaCodeMapStorage.readExternal(objectInput);
  }

  /**
   * Supports Java Serialization.
   */
  public void writeExternal(ObjectOutput objectOutput) throws IOException {
    objectOutput.writeBoolean(areaCodeMapStorage.isFlyweight());
    areaCodeMapStorage.writeExternal(objectOutput);
  }

  /**
   * Returns the description of the geographical area the {@code number} corresponds to.
   *
   * @param number  the phone number to look up
   * @return  the description of the geographical area
   */
  String lookup(PhoneNumber number) {
    int numOfEntries = areaCodeMapStorage.getNumOfEntries();
    if (numOfEntries == 0) {
      return "";
    }
    long phonePrefix = isLeadingZeroPossible
        ? Long.parseLong(number.getCountryCode() + phoneUtil.getNationalSignificantNumber(number))
        : Long.parseLong(phoneUtil.getNationalSignificantNumber(number));
    int currentIndex = numOfEntries - 1;
    SortedSet<Integer> currentSetOfLengths = areaCodeMapStorage.getPossibleLengths();
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
      int currentPrefix = areaCodeMapStorage.getPrefix(currentIndex);
      if (phonePrefix == currentPrefix) {
        return areaCodeMapStorage.getDescription(currentIndex);
      }
      currentSetOfLengths = currentSetOfLengths.headSet(possibleLength);
    }
    return "";
  }

  /**
   * Does a binary search for {@code value} in the provided array from {@code start} to {@code end}
   * (inclusive). Returns the position if {@code value} is found; otherwise, returns the
   * position which has the largest value that is less than {@code value}. This means if
   * {@code value} is the smallest, -1 will be returned.
   */
  private int binarySearch(int start, int end, long value) {
    int current = 0;
    while (start <= end) {
      current = (start + end) / 2;
      int currentValue = areaCodeMapStorage.getPrefix(current);
      if (currentValue == value) {
        return current;
      } else if (currentValue > value) {
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
    int numOfEntries = areaCodeMapStorage.getNumOfEntries();

    for (int i = 0; i < numOfEntries; i++) {
      if (!isLeadingZeroPossible) {
        output.append(countryCallingCode);
      }
      output.append(areaCodeMapStorage.getPrefix(i));
      output.append("|");
      output.append(areaCodeMapStorage.getDescription(i));
      output.append("\n");
    }
    return output.toString();
  }
}
