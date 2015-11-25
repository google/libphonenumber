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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.SortedMap;
import java.util.TreeSet;

/**
 * Abstracts the way phone prefix data is stored into memory and serialized to a stream. It is used
 * by {@link PhonePrefixMap} to support the most space-efficient storage strategy according to the
 * provided data.
 *
 * @author Philippe Liard
 */
abstract class PhonePrefixMapStorageStrategy {
  protected int numOfEntries = 0;
  protected final TreeSet<Integer> possibleLengths = new TreeSet<Integer>();

  /**
   * Gets the phone number prefix located at the provided {@code index}.
   *
   * @param index  the index of the prefix that needs to be returned
   * @return  the phone number prefix at the provided index
   */
  public abstract int getPrefix(int index);

  /**
   * Gets the description corresponding to the phone number prefix located at the provided {@code
   * index}. If the description is not available in the current language an empty string is
   * returned.
   *
   * @param index  the index of the phone number prefix that needs to be returned
   * @return  the description corresponding to the phone number prefix at the provided index
   */
  public abstract String getDescription(int index);

  /**
   * Sets the internal state of the underlying storage implementation from the provided {@code
   * sortedPhonePrefixMap} that maps phone number prefixes to description strings.
   *
   * @param sortedPhonePrefixMap  a sorted map that maps phone number prefixes including country
   *    calling code to description strings
   */
  public abstract void readFromSortedMap(SortedMap<Integer, String> sortedPhonePrefixMap);

  /**
   * Sets the internal state of the underlying storage implementation reading the provided {@code
   * objectInput}.
   *
   * @param objectInput  the object input stream from which the phone prefix map is read
   * @throws IOException  if an error occurred reading the provided input stream
   */
  public abstract void readExternal(ObjectInput objectInput) throws IOException;

  /**
   * Writes the internal state of the underlying storage implementation to the provided {@code
   * objectOutput}.
   *
   * @param objectOutput  the object output stream to which the phone prefix map is written
   * @throws IOException  if an error occurred writing to the provided output stream
   */
  public abstract void writeExternal(ObjectOutput objectOutput) throws IOException;

  /**
   * @return  the number of entries contained in the phone prefix map
   */
  public int getNumOfEntries() {
    return numOfEntries;
  }

  /**
   * @return  the set containing the possible lengths of prefixes
   */
  public TreeSet<Integer> getPossibleLengths() {
    return possibleLengths;
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    int numOfEntries = getNumOfEntries();

    for (int i = 0; i < numOfEntries; i++) {
      output.append(getPrefix(i))
          .append("|")
          .append(getDescription(i))
          .append("\n");
    }
    return output.toString();
  }
}
