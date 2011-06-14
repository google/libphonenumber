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

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unittests for AreaCodeMap.java
 *
 * @author Shaopeng Jia
 */
public class AreaCodeMapTest extends TestCase {
  private final AreaCodeMap areaCodeMap;
  private PhoneNumber number = new PhoneNumber();
  private static final Logger LOGGER = Logger.getLogger(AreaCodeMapTest.class.getName());
  static final String TEST_META_DATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting";

  public AreaCodeMapTest() {
    PhoneNumberUtil.resetInstance();
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance(
        TEST_META_DATA_FILE_PREFIX,
        CountryCodeToRegionCodeMapForTesting.getCountryCodeToRegionCodeMap());
    areaCodeMap = new AreaCodeMap(phoneUtil);

    SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();
    sortedMap.put(1212, "New York");
    sortedMap.put(1480, "Arizona");
    sortedMap.put(1650, "California");
    sortedMap.put(1907, "Alaska");
    sortedMap.put(1201664, "Westwood, NJ");
    sortedMap.put(1480893, "Phoenix, AZ");
    sortedMap.put(1501372, "Little Rock, AR");
    sortedMap.put(1626308, "Alhambra, CA");
    sortedMap.put(1650345, "San Mateo, CA");
    sortedMap.put(1867993, "Dawson, YT");
    sortedMap.put(1972480, "Richardson, TX");

    areaCodeMap.readAreaCodeMap(sortedMap);
  }

  public void testLookupInvalidNumber_US() {
    // central office code cannot start with 1.
    number.setCountryCode(1).setNationalNumber(2121234567L);
    assertEquals("New York", areaCodeMap.lookup(number));
  }

  public void testLookupNumber_NJ() {
    number.setCountryCode(1).setNationalNumber(2016641234L);
    assertEquals("Westwood, NJ", areaCodeMap.lookup(number));
  }

  public void testLookupNumber_NY() {
    number.setCountryCode(1).setNationalNumber(2126641234L);
    assertEquals("New York", areaCodeMap.lookup(number));
  }

  public void testLookupNumber_CA_1() {
    number.setCountryCode(1).setNationalNumber(6503451234L);
    assertEquals("San Mateo, CA", areaCodeMap.lookup(number));
  }

  public void testLookupNumber_CA_2() {
    number.setCountryCode(1).setNationalNumber(6502531234L);
    assertEquals("California", areaCodeMap.lookup(number));
  }

  public void testLookupNumberFound_TX() {
    number.setCountryCode(1).setNationalNumber(9724801234L);
    assertEquals("Richardson, TX", areaCodeMap.lookup(number));
  }

  public void testLookupNumberNotFound_TX() {
    number.setCountryCode(1).setNationalNumber(9724811234L);
    assertEquals("", areaCodeMap.lookup(number));
  }

  public void testLookupNumber_CH() {
    number.setCountryCode(41).setNationalNumber(446681300L);
    assertEquals("", areaCodeMap.lookup(number));
  }

  public void testReadWriteExternal() {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      areaCodeMap.writeExternal(objectOutputStream);
      objectOutputStream.flush();

      AreaCodeMap newAreaCodeMap = new AreaCodeMap();
      newAreaCodeMap.readExternal(
          new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));

      assertEquals(areaCodeMap.toString(), newAreaCodeMap.toString());
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
      fail();
    }
  }
}
