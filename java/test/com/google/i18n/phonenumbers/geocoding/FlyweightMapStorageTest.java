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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Unittests for FlyweightMapStorage.java
 *
 * @author Philippe Liard
 */
public class FlyweightMapStorageTest extends TestCase {
  private final SortedMap<Integer, String> areaCodeMap = new TreeMap<Integer, String>();

  public FlyweightMapStorageTest() {
    areaCodeMap.put(331402, "Paris");
    areaCodeMap.put(331434, "Paris");
    areaCodeMap.put(334910, "Marseille");
    areaCodeMap.put(334911, "Marseille");
  }

  public void testReadFromSortedMap() {
    FlyweightMapStorage mapStorage = new FlyweightMapStorage();
    mapStorage.readFromSortedMap(areaCodeMap);

    assertEquals(331402, mapStorage.getPrefix(0));
    assertEquals(331434, mapStorage.getPrefix(1));
    assertEquals(334910, mapStorage.getPrefix(2));
    assertEquals(334911, mapStorage.getPrefix(3));

    String desc = mapStorage.getDescription(0);
    assertEquals("Paris", desc);
    assertTrue(desc == mapStorage.getDescription(1));  // Same identity.

    desc = mapStorage.getDescription(2);
    assertEquals("Marseille", desc);
    assertTrue(desc == mapStorage.getDescription(3));  // Same identity.
  }

  public void testWriteAndReadExternal() throws IOException {
    FlyweightMapStorage mapStorage = new FlyweightMapStorage();
    mapStorage.readFromSortedMap(areaCodeMap);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    mapStorage.writeExternal(objectOutputStream);
    objectOutputStream.flush();

    FlyweightMapStorage newMapStorage = new FlyweightMapStorage();
    ObjectInputStream objectInputStream =
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    newMapStorage.readExternal(objectInputStream);

    String expected = mapStorage.toString();
    assertFalse(expected.length() == 0);
    assertEquals(expected, newMapStorage.toString());
  }
}
