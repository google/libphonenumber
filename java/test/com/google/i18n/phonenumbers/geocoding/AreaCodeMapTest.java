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

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Unittests for AreaCodeMap.java
 *
 * @author Shaopeng Jia
 */
public class AreaCodeMapTest extends TestCase {
  private final AreaCodeMap areaCodeMapForUS = new AreaCodeMap(1);
  private final AreaCodeMap areaCodeMapForIT = new AreaCodeMap(39);
  private PhoneNumber number = new PhoneNumber();

  public AreaCodeMapTest() {
    SortedMap<Integer, String> sortedMapForUS = new TreeMap<Integer, String>();
    sortedMapForUS.put(1212, "New York");
    sortedMapForUS.put(1480, "Arizona");
    sortedMapForUS.put(1650, "California");
    sortedMapForUS.put(1907, "Alaska");
    sortedMapForUS.put(1201664, "Westwood, NJ");
    sortedMapForUS.put(1480893, "Phoenix, AZ");
    sortedMapForUS.put(1501372, "Little Rock, AR");
    sortedMapForUS.put(1626308, "Alhambra, CA");
    sortedMapForUS.put(1650345, "San Mateo, CA");
    sortedMapForUS.put(1867993, "Dawson, YT");
    sortedMapForUS.put(1972480, "Richardson, TX");

    areaCodeMapForUS.readAreaCodeMap(sortedMapForUS);

    SortedMap<Integer, String> sortedMapForIT = new TreeMap<Integer, String>();
    sortedMapForIT.put(3902, "Milan");
    sortedMapForIT.put(3906, "Rome");
    sortedMapForIT.put(39010, "Genoa");
    sortedMapForIT.put(390131, "Alessandria");
    sortedMapForIT.put(390321, "Novara");
    sortedMapForIT.put(390975, "Potenza");

    areaCodeMapForIT.readAreaCodeMap(sortedMapForIT);
  }

  private static SortedMap<Integer, String> createDefaultStorageMapCandidate() {
    SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();
    // Make the area codes bigger to store them using integer.
    sortedMap.put(121212345, "New York");
    sortedMap.put(148034434, "Arizona");
    return sortedMap;
  }

  private static SortedMap<Integer, String> createFlyweightStorageMapCandidate() {
    SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();
    sortedMap.put(1212, "New York");
    sortedMap.put(1213, "New York");
    sortedMap.put(1214, "New York");
    sortedMap.put(1480, "Arizona");
    return sortedMap;
  }

  public void testGetSmallerMapStorageChoosesDefaultImpl() {
    AreaCodeMapStorageStrategy mapStorage =
        new AreaCodeMap(1).getSmallerMapStorage(createDefaultStorageMapCandidate());
    assertFalse(mapStorage.isFlyweight());
  }

  public void testGetSmallerMapStorageChoosesFlyweightImpl() {
    AreaCodeMapStorageStrategy mapStorage =
        new AreaCodeMap(1).getSmallerMapStorage(createFlyweightStorageMapCandidate());
    assertTrue(mapStorage.isFlyweight());
  }

  public void testLookupInvalidNumber_US() {
    // central office code cannot start with 1.
    number.setCountryCode(1).setNationalNumber(2121234567L);
    assertEquals("New York", areaCodeMapForUS.lookup(number));
  }

  public void testLookupNumber_NJ() {
    number.setCountryCode(1).setNationalNumber(2016641234L);
    assertEquals("Westwood, NJ", areaCodeMapForUS.lookup(number));
  }

  public void testLookupNumber_NY() {
    number.setCountryCode(1).setNationalNumber(2126641234L);
    assertEquals("New York", areaCodeMapForUS.lookup(number));
  }

  public void testLookupNumber_CA_1() {
    number.setCountryCode(1).setNationalNumber(6503451234L);
    assertEquals("San Mateo, CA", areaCodeMapForUS.lookup(number));
  }

  public void testLookupNumber_CA_2() {
    number.setCountryCode(1).setNationalNumber(6502531234L);
    assertEquals("California", areaCodeMapForUS.lookup(number));
  }

  public void testLookupNumberFound_TX() {
    number.setCountryCode(1).setNationalNumber(9724801234L);
    assertEquals("Richardson, TX", areaCodeMapForUS.lookup(number));
  }

  public void testLookupNumberNotFound_TX() {
    number.setCountryCode(1).setNationalNumber(9724811234L);
    assertEquals("", areaCodeMapForUS.lookup(number));
  }

  public void testLookupNumber_CH() {
    number.setCountryCode(41).setNationalNumber(446681300L);
    assertEquals("", areaCodeMapForUS.lookup(number));
  }

  public void testLookupNumber_IT() {
    number.setCountryCode(39).setNationalNumber(212345678L).setItalianLeadingZero(true);
    assertEquals("Milan", areaCodeMapForIT.lookup(number));

    number.setNationalNumber(612345678L);
    assertEquals("Rome", areaCodeMapForIT.lookup(number));

    number.setNationalNumber(3211234L);
    assertEquals("Novara", areaCodeMapForIT.lookup(number));

    // A mobile number
    number.setNationalNumber(321123456L).setItalianLeadingZero(false);
    assertEquals("", areaCodeMapForIT.lookup(number));

    // An invalid number (too short)
    number.setNationalNumber(321123L).setItalianLeadingZero(true);
    assertEquals("Novara", areaCodeMapForIT.lookup(number));
  }

  /**
   * Creates a new area code map serializing the provided area code map to a stream and then reading
   * this stream. The resulting area code map is expected to be strictly equal to the provided one
   * from which it was generated.
   */
  private static AreaCodeMap createNewAreaCodeMap(AreaCodeMap areaCodeMap)
      throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    areaCodeMap.writeExternal(objectOutputStream);
    objectOutputStream.flush();

    AreaCodeMap newAreaCodeMap = new AreaCodeMap(1);
    newAreaCodeMap.readExternal(
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
    return newAreaCodeMap;
  }

  public void testReadWriteExternalWithDefaultStrategy() throws IOException {
    AreaCodeMap localAreaCodeMap = new AreaCodeMap(1);
    localAreaCodeMap.readAreaCodeMap(createDefaultStorageMapCandidate());
    assertFalse(localAreaCodeMap.getAreaCodeMapStorage().isFlyweight());

    AreaCodeMap newAreaCodeMap;
    newAreaCodeMap = createNewAreaCodeMap(localAreaCodeMap);
    assertEquals(localAreaCodeMap.toString(), newAreaCodeMap.toString());
  }

  public void testReadWriteExternalWithFlyweightStrategy() throws IOException {
    AreaCodeMap localAreaCodeMap = new AreaCodeMap(1);
    localAreaCodeMap.readAreaCodeMap(createFlyweightStorageMapCandidate());
    assertTrue(localAreaCodeMap.getAreaCodeMapStorage().isFlyweight());

    AreaCodeMap newAreaCodeMap;
    newAreaCodeMap = createNewAreaCodeMap(localAreaCodeMap);
    assertEquals(localAreaCodeMap.toString(), newAreaCodeMap.toString());
  }
}
