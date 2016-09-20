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
 * A utility that maps phone number prefixes to a description string, which may be, for example,
 * the geographical area the prefix covers.
 *
 * @author Shaopeng Jia
 */
public class PhonePrefixMap implements Externalizable {
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private static final Logger logger = Logger.getLogger(PhonePrefixMap.class.getName());

  private PhonePrefixMapStorageStrategy phonePrefixMapStorage;

  // @VisibleForTesting
  PhonePrefixMapStorageStrategy getPhonePrefixMapStorage() {
    return phonePrefixMapStorage;
  }

  /**
   * Creates an empty {@link PhonePrefixMap}. The default constructor is necessary for implementing
   * {@link Externalizable}. The empty map could later be populated by
   * {@link #readPhonePrefixMap(java.util.SortedMap)} or {@link #readExternal(java.io.ObjectInput)}.
   */
  public PhonePrefixMap() {}

  /**
   * Gets the size of the provided phone prefix map storage. The map storage passed-in will be
   * filled as a result.
   */
  private static int getSizeOfPhonePrefixMapStorage(PhonePrefixMapStorageStrategy mapStorage,
      SortedMap<Integer, String> phonePrefixMap) throws IOException {
    mapStorage.readFromSortedMap(phonePrefixMap);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    mapStorage.writeExternal(objectOutputStream);
    objectOutputStream.flush();
    int sizeOfStorage = byteArrayOutputStream.size();
    objectOutputStream.close();
    return sizeOfStorage;
  }

  private PhonePrefixMapStorageStrategy createDefaultMapStorage() {
    return new DefaultMapStorage();
  }

  private PhonePrefixMapStorageStrategy createFlyweightMapStorage() {
    return new FlyweightMapStorage();
  }

  /**
   * Gets the smaller phone prefix map storage strategy according to the provided phone prefix map.
   * It actually uses (outputs the data to a stream) both strategies and retains the best one which
   * make this method quite expensive.
   */
  // @VisibleForTesting
  PhonePrefixMapStorageStrategy getSmallerMapStorage(SortedMap<Integer, String> phonePrefixMap) {
    try {
      PhonePrefixMapStorageStrategy flyweightMapStorage = createFlyweightMapStorage();
      int sizeOfFlyweightMapStorage = getSizeOfPhonePrefixMapStorage(flyweightMapStorage,
                                                                     phonePrefixMap);

      PhonePrefixMapStorageStrategy defaultMapStorage = createDefaultMapStorage();
      int sizeOfDefaultMapStorage = getSizeOfPhonePrefixMapStorage(defaultMapStorage,
                                                                   phonePrefixMap);

      return sizeOfFlyweightMapStorage < sizeOfDefaultMapStorage
          ? flyweightMapStorage : defaultMapStorage;
    } catch (IOException e) {
      logger.severe(e.getMessage());
      return createFlyweightMapStorage();
    }
  }

  /**
   * Creates an {@link PhonePrefixMap} initialized with {@code sortedPhonePrefixMap}.  Note that the
   * underlying implementation of this method is expensive thus should not be called by
   * time-critical applications.
   *
   * @param sortedPhonePrefixMap  a map from phone number prefixes to descriptions of those prefixes
   * sorted in ascending order of the phone number prefixes as integers.
   */
  public void readPhonePrefixMap(SortedMap<Integer, String> sortedPhonePrefixMap) {
    phonePrefixMapStorage = getSmallerMapStorage(sortedPhonePrefixMap);
  }

  /**
   * Supports Java Serialization.
   */
  public void readExternal(ObjectInput objectInput) throws IOException {
    // Read the phone prefix map storage strategy flag.
    boolean useFlyweightMapStorage = objectInput.readBoolean();
    if (useFlyweightMapStorage) {
      phonePrefixMapStorage = new FlyweightMapStorage();
    } else {
      phonePrefixMapStorage = new DefaultMapStorage();
    }
    phonePrefixMapStorage.readExternal(objectInput);
  }

  /**
   * Supports Java Serialization.
   */
  public void writeExternal(ObjectOutput objectOutput) throws IOException {
    objectOutput.writeBoolean(phonePrefixMapStorage instanceof FlyweightMapStorage);
    phonePrefixMapStorage.writeExternal(objectOutput);
  }

  /**
   * Returns the description of the {@code number}. This method distinguishes the case of an invalid
   * prefix and a prefix for which the name is not available in the current language. If the
   * description is not available in the current language an empty string is returned. If no
   * description was found for the provided number, null is returned.
   *
   * @param number  the phone number to look up
   * @return  the description of the number
   */
  String lookup(long number) {
    int numOfEntries = phonePrefixMapStorage.getNumOfEntries();
    if (numOfEntries == 0) {
      return null;
    }
    long phonePrefix = number;
    int currentIndex = numOfEntries - 1;
    SortedSet<Integer> currentSetOfLengths = phonePrefixMapStorage.getPossibleLengths();
    while (currentSetOfLengths.size() > 0) {
      Integer possibleLength = currentSetOfLengths.last();
      String phonePrefixStr = String.valueOf(phonePrefix);
      if (phonePrefixStr.length() > possibleLength) {
        phonePrefix = Long.parseLong(phonePrefixStr.substring(0, possibleLength));
      }
      currentIndex = binarySearch(0, currentIndex, phonePrefix);
      if (currentIndex < 0) {
        return null;
      }
      int currentPrefix = phonePrefixMapStorage.getPrefix(currentIndex);
      if (phonePrefix == currentPrefix) {
        return phonePrefixMapStorage.getDescription(currentIndex);
      }
      currentSetOfLengths = currentSetOfLengths.headSet(possibleLength);
    }
    return null;
  }

  /**
   * As per {@link #lookup(long)}, but receives the number as a PhoneNumber instead of a long.
   *
   * @param number  the phone number to look up
   * @return  the description corresponding to the prefix that best matches this phone number
   */
  public String lookup(PhoneNumber number) {
    long phonePrefix =
        Long.parseLong(number.getCountryCode() + phoneUtil.getNationalSignificantNumber(number));
    return lookup(phonePrefix);
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
      current = (start + end) >>> 1;
      int currentValue = phonePrefixMapStorage.getPrefix(current);
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
   * Dumps the mappings contained in the phone prefix map.
   */
  @Override
  public String toString() {
    return phonePrefixMapStorage.toString();
  }
}
