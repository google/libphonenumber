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

package com.google.i18n.phonenumbers.geocoding;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.SortedMap;
import java.util.TreeSet;

/**
 * Abstracts the way area code data is stored into memory and serialized to a stream. It is used by
 * {@link AreaCodeMap} to support the most space-efficient storage strategy according to the
 * provided data.
 *
 * @author Philippe Liard
 */
// @VisibleForTesting
abstract class AreaCodeMapStorageStrategy {
  protected int numOfEntries = 0;
  protected final TreeSet<Integer> possibleLengths = new TreeSet<Integer>();

  public AreaCodeMapStorageStrategy() {}

  /**
   * Returns whether the underlying implementation of this abstract class is flyweight.
   * It is expected to be flyweight if it implements the {@code FlyweightMapStorage} class.
   *
   * @return  whether the underlying implementation of this abstract class is flyweight
   */
  public abstract boolean isFlyweight();

  /**
   * @return  the number of entries contained in the area code map
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

  /**
   * Gets the phone number prefix located at the provided {@code index}.
   *
   * @param index  the index of the prefix that needs to be returned
   * @return  the phone number prefix at the provided index
   */
  public abstract int getPrefix(int index);

  /**
   * Gets the description corresponding to the phone number prefix located at the provided {@code
   * index}.
   *
   * @param index  the index of the phone number prefix that needs to be returned
   * @return  the description corresponding to the phone number prefix at the provided index
   */
  public abstract String getDescription(int index);

  /**
   * Sets the internal state of the underlying storage implementation from the provided {@code
   * sortedAreaCodeMap} that maps phone number prefixes to description strings.
   *
   * @param sortedAreaCodeMap  a sorted map that maps phone number prefixes including country
   *    calling code to description strings
   */
  public abstract void readFromSortedMap(SortedMap<Integer, String> sortedAreaCodeMap);

  /**
   * Sets the internal state of the underlying storage implementation reading the provided {@code
   * objectInput}.
   *
   * @param objectInput  the object input stream from which the area code map is read
   * @throws IOException  if an error occurred reading the provided input stream
   */
  public abstract void readExternal(ObjectInput objectInput) throws IOException;

  /**
   * Writes the internal state of the underlying storage implementation to the provided {@code
   * objectOutput}.
   *
   * @param objectOutput  the object output stream to which the area code map is written
   * @throws IOException  if an error occurred writing to the provided output stream
   */
  public abstract void writeExternal(ObjectOutput objectOutput) throws IOException;

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder();
    int numOfEntries = getNumOfEntries();

    for (int i = 0; i < numOfEntries; i++) {
      output.append(getPrefix(i));
      output.append("|");
      output.append(getDescription(i));
      output.append("\n");
    }
    return output.toString();
  }
}
