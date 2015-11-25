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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Flyweight phone prefix map storage strategy that uses a table to store unique strings and shorts
 * to store the prefix and description indexes when possible. It is particularly space-efficient
 * when the provided phone prefix map contains a lot of redundant descriptions.
 *
 * @author Philippe Liard
 */
final class FlyweightMapStorage extends PhonePrefixMapStorageStrategy {
  // Size of short and integer types in bytes.
  private static final int SHORT_NUM_BYTES = Short.SIZE / 8;
  private static final int INT_NUM_BYTES = Integer.SIZE / 8;

  // The number of bytes used to store a phone number prefix.
  private int prefixSizeInBytes;
  // The number of bytes used to store a description index. It is computed from the size of the
  // description pool containing all the strings.
  private int descIndexSizeInBytes;

  private ByteBuffer phoneNumberPrefixes;
  private ByteBuffer descriptionIndexes;

  // Sorted string array of unique description strings.
  private String[] descriptionPool;

  @Override
  public int getPrefix(int index) {
    return readWordFromBuffer(phoneNumberPrefixes, prefixSizeInBytes, index);
  }

  /**
   * This implementation returns the same string (same identity) when called for multiple indexes
   * corresponding to prefixes that have the same description.
   */
  @Override
  public String getDescription(int index) {
    int indexInDescriptionPool =
        readWordFromBuffer(descriptionIndexes, descIndexSizeInBytes, index);
    return descriptionPool[indexInDescriptionPool];
  }

  @Override
  public void readFromSortedMap(SortedMap<Integer, String> phonePrefixMap) {
    SortedSet<String> descriptionsSet = new TreeSet<String>();
    numOfEntries = phonePrefixMap.size();
    prefixSizeInBytes = getOptimalNumberOfBytesForValue(phonePrefixMap.lastKey());
    phoneNumberPrefixes = ByteBuffer.allocate(numOfEntries * prefixSizeInBytes);

    // Fill the phone number prefixes byte buffer, the set of possible lengths of prefixes and the
    // description set.
    int index = 0;
    for (Entry<Integer, String> entry : phonePrefixMap.entrySet()) {
      int prefix = entry.getKey();
      storeWordInBuffer(phoneNumberPrefixes, prefixSizeInBytes, index, prefix);
      possibleLengths.add((int) Math.log10(prefix) + 1);
      descriptionsSet.add(entry.getValue());
      ++index;
    }
    createDescriptionPool(descriptionsSet, phonePrefixMap);
  }

  /**
   * Creates the description pool from the provided set of string descriptions and phone prefix map.
   */
  private void createDescriptionPool(SortedSet<String> descriptionsSet,
      SortedMap<Integer, String> phonePrefixMap) {
    descIndexSizeInBytes = getOptimalNumberOfBytesForValue(descriptionsSet.size() - 1);
    descriptionIndexes = ByteBuffer.allocate(numOfEntries * descIndexSizeInBytes);
    descriptionPool = new String[descriptionsSet.size()];
    descriptionsSet.toArray(descriptionPool);

    // Map the phone number prefixes to the descriptions.
    int index = 0;
    for (int i = 0; i < numOfEntries; i++) {
      int prefix = readWordFromBuffer(phoneNumberPrefixes, prefixSizeInBytes, i);
      String description = phonePrefixMap.get(prefix);
      int positionInDescriptionPool = Arrays.binarySearch(descriptionPool, description);
      storeWordInBuffer(descriptionIndexes, descIndexSizeInBytes, index, positionInDescriptionPool);
      ++index;
    }
  }

  @Override
  public void readExternal(ObjectInput objectInput) throws IOException {
    // Read binary words sizes.
    prefixSizeInBytes = objectInput.readInt();
    descIndexSizeInBytes = objectInput.readInt();

    // Read possible lengths.
    int sizeOfLengths = objectInput.readInt();
    possibleLengths.clear();
    for (int i = 0; i < sizeOfLengths; i++) {
      possibleLengths.add(objectInput.readInt());
    }

    // Read description pool size.
    int descriptionPoolSize = objectInput.readInt();
    // Read description pool.
    if (descriptionPool == null || descriptionPool.length < descriptionPoolSize) {
      descriptionPool = new String[descriptionPoolSize];
    }
    for (int i = 0; i < descriptionPoolSize; i++) {
      String description = objectInput.readUTF();
      descriptionPool[i] = description;
    }
    readEntries(objectInput);
  }

  /**
   * Reads the phone prefix entries from the provided input stream and stores them to the internal
   * byte buffers.
   */
  private void readEntries(ObjectInput objectInput) throws IOException {
    numOfEntries = objectInput.readInt();
    if (phoneNumberPrefixes == null || phoneNumberPrefixes.capacity() < numOfEntries) {
      phoneNumberPrefixes = ByteBuffer.allocate(numOfEntries * prefixSizeInBytes);
    }
    if (descriptionIndexes == null || descriptionIndexes.capacity() < numOfEntries) {
      descriptionIndexes = ByteBuffer.allocate(numOfEntries * descIndexSizeInBytes);
    }
    for (int i = 0; i < numOfEntries; i++) {
      readExternalWord(objectInput, prefixSizeInBytes, phoneNumberPrefixes, i);
      readExternalWord(objectInput, descIndexSizeInBytes, descriptionIndexes, i);
    }
  }

  @Override
  public void writeExternal(ObjectOutput objectOutput) throws IOException {
    // Write binary words sizes.
    objectOutput.writeInt(prefixSizeInBytes);
    objectOutput.writeInt(descIndexSizeInBytes);

    // Write possible lengths.
    int sizeOfLengths = possibleLengths.size();
    objectOutput.writeInt(sizeOfLengths);
    for (Integer length : possibleLengths) {
      objectOutput.writeInt(length);
    }

    // Write description pool size.
    objectOutput.writeInt(descriptionPool.length);
    // Write description pool.
    for (String description : descriptionPool) {
      objectOutput.writeUTF(description);
    }

    // Write entries.
    objectOutput.writeInt(numOfEntries);
    for (int i = 0; i < numOfEntries; i++) {
      writeExternalWord(objectOutput, prefixSizeInBytes, phoneNumberPrefixes, i);
      writeExternalWord(objectOutput, descIndexSizeInBytes, descriptionIndexes, i);
    }
  }

  /**
   * Gets the minimum number of bytes that can be used to store the provided {@code value}.
   */
  private static int getOptimalNumberOfBytesForValue(int value) {
    return value <= Short.MAX_VALUE ? SHORT_NUM_BYTES : INT_NUM_BYTES;
  }

  /**
   * Stores a value which is read from the provided {@code objectInput} to the provided byte {@code
   * buffer} at the specified {@code index}.
   *
   * @param objectInput  the object input stream from which the value is read
   * @param wordSize  the number of bytes used to store the value read from the stream
   * @param outputBuffer  the byte buffer to which the value is stored
   * @param index  the index where the value is stored in the buffer
   * @throws IOException  if an error occurred reading from the object input stream
   */
  private static void readExternalWord(ObjectInput objectInput, int wordSize,
      ByteBuffer outputBuffer, int index) throws IOException {
    int wordIndex = index * wordSize;
    if (wordSize == SHORT_NUM_BYTES) {
      outputBuffer.putShort(wordIndex, objectInput.readShort());
    } else {
      outputBuffer.putInt(wordIndex, objectInput.readInt());
    }
  }

  /**
   * Writes the value read from the provided byte {@code buffer} at the specified {@code index} to
   * the provided {@code objectOutput}.
   *
   * @param objectOutput  the object output stream to which the value is written
   * @param wordSize  the number of bytes used to store the value
   * @param inputBuffer  the byte buffer from which the value is read
   * @param index  the index of the value in the the byte buffer
   * @throws IOException if an error occurred writing to the provided object output stream
   */
  private static void writeExternalWord(ObjectOutput objectOutput, int wordSize,
      ByteBuffer inputBuffer, int index) throws IOException {
    int wordIndex = index * wordSize;
    if (wordSize == SHORT_NUM_BYTES) {
      objectOutput.writeShort(inputBuffer.getShort(wordIndex));
    } else {
      objectOutput.writeInt(inputBuffer.getInt(wordIndex));
    }
  }

  /**
   * Reads the {@code value} at the specified {@code index} from the provided byte {@code buffer}.
   * Note that only integer and short sizes are supported.
   *
   * @param buffer  the byte buffer from which the value is read
   * @param wordSize  the number of bytes used to store the value
   * @param index  the index where the value is read from
   *
   * @return  the value read from the buffer
   */
  private static int readWordFromBuffer(ByteBuffer buffer, int wordSize, int index) {
    int wordIndex = index * wordSize;
    return wordSize == SHORT_NUM_BYTES ? buffer.getShort(wordIndex) : buffer.getInt(wordIndex);
  }

  /**
   * Stores the provided {@code value} to the provided byte {@code buffer} at the specified {@code
   * index} using the provided {@code wordSize} in bytes. Note that only integer and short sizes are
   * supported.
   *
   * @param buffer  the byte buffer to which the value is stored
   * @param wordSize  the number of bytes used to store the provided value
   * @param index  the index to which the value is stored
   * @param value  the value that is stored assuming it does not require more than the specified
   *    number of bytes.
   */
  private static void storeWordInBuffer(ByteBuffer buffer, int wordSize, int index, int value) {
    int wordIndex = index * wordSize;
    if (wordSize == SHORT_NUM_BYTES) {
      buffer.putShort(wordIndex, (short) value);
    } else {
      buffer.putInt(wordIndex, value);
    }
  }
}
