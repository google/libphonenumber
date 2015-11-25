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

/**
 * Default phone prefix map storage strategy that is used for data not containing description
 * duplications. It is mainly intended to avoid the overhead of the string table management when it
 * is actually unnecessary (i.e no string duplication).
 *
 * @author Shaopeng Jia
 */
class DefaultMapStorage extends PhonePrefixMapStorageStrategy {

  public DefaultMapStorage() {}

  private int[] phoneNumberPrefixes;
  private String[] descriptions;

  @Override
  public int getPrefix(int index) {
    return phoneNumberPrefixes[index];
  }

  @Override
  public String getDescription(int index) {
    return descriptions[index];
  }

  @Override
  public void readFromSortedMap(SortedMap<Integer, String> sortedPhonePrefixMap) {
    numOfEntries = sortedPhonePrefixMap.size();
    phoneNumberPrefixes = new int[numOfEntries];
    descriptions = new String[numOfEntries];
    int index = 0;
    for (int prefix : sortedPhonePrefixMap.keySet()) {
      phoneNumberPrefixes[index++] = prefix;
      possibleLengths.add((int) Math.log10(prefix) + 1);
    }
    sortedPhonePrefixMap.values().toArray(descriptions);
  }

  @Override
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

  @Override
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
}
