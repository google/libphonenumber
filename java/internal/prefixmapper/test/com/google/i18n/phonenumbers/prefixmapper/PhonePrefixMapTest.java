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
 * Unittests for PhonePrefixMap.java
 *
 * @author Shaopeng Jia
 */
public class PhonePrefixMapTest extends TestCase {
  private final PhonePrefixMap phonePrefixMapForUS = new PhonePrefixMap();
  private final PhonePrefixMap phonePrefixMapForIT = new PhonePrefixMap();
  private PhoneNumber number = new PhoneNumber();

  public PhonePrefixMapTest() {
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

    phonePrefixMapForUS.readPhonePrefixMap(sortedMapForUS);

    SortedMap<Integer, String> sortedMapForIT = new TreeMap<Integer, String>();
    sortedMapForIT.put(3902, "Milan");
    sortedMapForIT.put(3906, "Rome");
    sortedMapForIT.put(39010, "Genoa");
    sortedMapForIT.put(390131, "Alessandria");
    sortedMapForIT.put(390321, "Novara");
    sortedMapForIT.put(390975, "Potenza");

    phonePrefixMapForIT.readPhonePrefixMap(sortedMapForIT);
  }

  private static SortedMap<Integer, String> createDefaultStorageMapCandidate() {
    SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();
    // Make the phone prefixs bigger to store them using integer.
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
    PhonePrefixMapStorageStrategy mapStorage =
        new PhonePrefixMap().getSmallerMapStorage(createDefaultStorageMapCandidate());
    assertFalse(mapStorage instanceof FlyweightMapStorage);
  }

  public void testGetSmallerMapStorageChoosesFlyweightImpl() {
    PhonePrefixMapStorageStrategy mapStorage =
        new PhonePrefixMap().getSmallerMapStorage(createFlyweightStorageMapCandidate());
    assertTrue(mapStorage instanceof FlyweightMapStorage);
  }

  public void testLookupInvalidNumber_US() {
    // central office code cannot start with 1.
    number.setCountryCode(1).setNationalNumber(2121234567L);
    assertEquals("New York", phonePrefixMapForUS.lookup(number));
  }

  public void testLookupNumber_NJ() {
    number.setCountryCode(1).setNationalNumber(2016641234L);
    assertEquals("Westwood, NJ", phonePrefixMapForUS.lookup(number));
  }

  public void testLookupNumber_NY() {
    number.setCountryCode(1).setNationalNumber(2126641234L);
    assertEquals("New York", phonePrefixMapForUS.lookup(number));
  }

  public void testLookupNumber_CA_1() {
    number.setCountryCode(1).setNationalNumber(6503451234L);
    assertEquals("San Mateo, CA", phonePrefixMapForUS.lookup(number));
  }

  public void testLookupNumber_CA_2() {
    number.setCountryCode(1).setNationalNumber(6502531234L);
    assertEquals("California", phonePrefixMapForUS.lookup(number));
  }

  public void testLookupNumberFound_TX() {
    number.setCountryCode(1).setNationalNumber(9724801234L);
    assertEquals("Richardson, TX", phonePrefixMapForUS.lookup(number));
  }

  public void testLookupNumberNotFound_TX() {
    number.setCountryCode(1).setNationalNumber(9724811234L);
    assertNull(phonePrefixMapForUS.lookup(number));
  }

  public void testLookupNumber_CH() {
    number.setCountryCode(41).setNationalNumber(446681300L);
    assertNull(phonePrefixMapForUS.lookup(number));
  }

  public void testLookupNumber_IT() {
    number.setCountryCode(39).setNationalNumber(212345678L).setItalianLeadingZero(true);
    assertEquals("Milan", phonePrefixMapForIT.lookup(number));

    number.setNationalNumber(612345678L);
    assertEquals("Rome", phonePrefixMapForIT.lookup(number));

    number.setNationalNumber(3211234L);
    assertEquals("Novara", phonePrefixMapForIT.lookup(number));

    // A mobile number
    number.setNationalNumber(321123456L).setItalianLeadingZero(false);
    assertNull(phonePrefixMapForIT.lookup(number));

    // An invalid number (too short)
    number.setNationalNumber(321123L).setItalianLeadingZero(true);
    assertEquals("Novara", phonePrefixMapForIT.lookup(number));
  }

  /**
   * Creates a new phone prefix map serializing the provided phone prefix map to a stream and then
   * reading this stream. The resulting phone prefix map is expected to be strictly equal to the
   * provided one from which it was generated.
   */
  private static PhonePrefixMap createNewPhonePrefixMap(
      PhonePrefixMap phonePrefixMap) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    phonePrefixMap.writeExternal(objectOutputStream);
    objectOutputStream.flush();

    PhonePrefixMap newPhonePrefixMap = new PhonePrefixMap();
    newPhonePrefixMap.readExternal(
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
    return newPhonePrefixMap;
  }

  public void testReadWriteExternalWithDefaultStrategy() throws IOException {
    PhonePrefixMap localPhonePrefixMap = new PhonePrefixMap();
    localPhonePrefixMap.readPhonePrefixMap(createDefaultStorageMapCandidate());
    assertFalse(localPhonePrefixMap.getPhonePrefixMapStorage() instanceof FlyweightMapStorage);

    PhonePrefixMap newPhonePrefixMap;
    newPhonePrefixMap = createNewPhonePrefixMap(localPhonePrefixMap);
    assertEquals(localPhonePrefixMap.toString(), newPhonePrefixMap.toString());
  }

  public void testReadWriteExternalWithFlyweightStrategy() throws IOException {
    PhonePrefixMap localPhonePrefixMap = new PhonePrefixMap();
    localPhonePrefixMap.readPhonePrefixMap(createFlyweightStorageMapCandidate());
    assertTrue(localPhonePrefixMap.getPhonePrefixMapStorage() instanceof FlyweightMapStorage);

    PhonePrefixMap newPhonePrefixMap;
    newPhonePrefixMap = createNewPhonePrefixMap(localPhonePrefixMap);
    assertEquals(localPhonePrefixMap.toString(), newPhonePrefixMap.toString());
  }
}
