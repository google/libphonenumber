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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Unittests for FlyweightMapStorage.java
 *
 * @author Philippe Liard
 */
public class FlyweightMapStorageTest extends TestCase {
  private static final SortedMap<Integer, String> phonePrefixMap;
  static {
    SortedMap<Integer, String> tmpMap = new TreeMap<Integer, String>();
    tmpMap.put(331402, "Paris");
    tmpMap.put(331434, "Paris");
    tmpMap.put(334910, "Marseille");
    tmpMap.put(334911, "Marseille");
    tmpMap.put(334912, "");
    tmpMap.put(334913, "");
    phonePrefixMap = Collections.unmodifiableSortedMap(tmpMap);
  }

  private FlyweightMapStorage mapStorage;

  @Override
  protected void setUp() throws Exception {
    mapStorage = new FlyweightMapStorage();
    mapStorage.readFromSortedMap(phonePrefixMap);
  }

  public void testReadFromSortedMap() {
    assertEquals(331402, mapStorage.getPrefix(0));
    assertEquals(331434, mapStorage.getPrefix(1));
    assertEquals(334910, mapStorage.getPrefix(2));
    assertEquals(334911, mapStorage.getPrefix(3));

    assertEquals("Paris", mapStorage.getDescription(0));
    assertSame(mapStorage.getDescription(0), mapStorage.getDescription(1));

    assertEquals("Marseille", mapStorage.getDescription(2));
    assertSame(mapStorage.getDescription(2), mapStorage.getDescription(3));
  }

  public void testReadFromSortedMapSupportsEmptyDescription() {
    assertEquals(334912, mapStorage.getPrefix(4));
    assertEquals(334913, mapStorage.getPrefix(5));

    assertEquals("", mapStorage.getDescription(4));
    assertSame(mapStorage.getDescription(4), mapStorage.getDescription(5));
  }

  public void testWriteAndReadExternal() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    mapStorage.writeExternal(objectOutputStream);
    objectOutputStream.flush();

    FlyweightMapStorage newMapStorage = new FlyweightMapStorage();
    ObjectInputStream objectInputStream =
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    newMapStorage.readExternal(objectInputStream);

    String expected = mapStorage.toString();
    assertEquals(expected, newMapStorage.toString());
  }

  public void testReadExternalThrowsIOExceptionWithMalformedData() throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeUTF("hello");
    objectOutputStream.flush();
    ObjectInputStream objectInputStream =
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    FlyweightMapStorage newMapStorage = new FlyweightMapStorage();
    try {
      newMapStorage.readExternal(objectInputStream);
      fail();
    } catch (IOException e) {
      // Exception expected.
    }
  }
}
