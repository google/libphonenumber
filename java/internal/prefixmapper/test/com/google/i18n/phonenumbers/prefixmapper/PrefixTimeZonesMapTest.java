/*
 * Copyright (C) 2012 The Libphonenumber Authors
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
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Unittests for PrefixTimeZonesMap.java
 */
public class PrefixTimeZonesMapTest extends TestCase {
  private final PrefixTimeZonesMap prefixTimeZonesMapForUS = new PrefixTimeZonesMap();
  private final PrefixTimeZonesMap prefixTimeZonesMapForRU = new PrefixTimeZonesMap();

  // US time zones
  private static final String CHICAGO_TZ = "America/Chicago";
  private static final String DENVER_TZ = "America/Denver";
  private static final String LOS_ANGELES_TZ = "America/Los_Angeles";
  private static final String NEW_YORK_TZ = "America/New_York";

  // Russian time zones
  private static final String IRKUTSK_TZ = "Asia/Irkutsk";
  private static final String MOSCOW_TZ = "Europe/Moscow";
  private static final String VLADIVOSTOK_TZ = "Asia/Vladivostok";
  private static final String YEKATERINBURG_TZ = "Asia/Yekaterinburg";

  public PrefixTimeZonesMapTest() {
    SortedMap<Integer, String> sortedMapForUS = new TreeMap<Integer, String>();
    sortedMapForUS.put(1, NEW_YORK_TZ + "&" + CHICAGO_TZ + "&" + LOS_ANGELES_TZ + "&" + DENVER_TZ);
    sortedMapForUS.put(1201, NEW_YORK_TZ);
    sortedMapForUS.put(1205, CHICAGO_TZ);
    sortedMapForUS.put(1208292, LOS_ANGELES_TZ);
    sortedMapForUS.put(1208234, DENVER_TZ);
    sortedMapForUS.put(1541367, LOS_ANGELES_TZ);
    sortedMapForUS.put(1423843, NEW_YORK_TZ);
    sortedMapForUS.put(1402721, CHICAGO_TZ);
    sortedMapForUS.put(1208888, DENVER_TZ);

    prefixTimeZonesMapForUS.readPrefixTimeZonesMap(sortedMapForUS);

    SortedMap<Integer, String> sortedMapForRU = new TreeMap<Integer, String>();
    sortedMapForRU.put(7421, VLADIVOSTOK_TZ);
    sortedMapForRU.put(7879, MOSCOW_TZ);
    sortedMapForRU.put(7342, YEKATERINBURG_TZ);
    sortedMapForRU.put(7395, IRKUTSK_TZ);

    prefixTimeZonesMapForRU.readPrefixTimeZonesMap(sortedMapForRU);
  }

  static List<String> buildListOfTimeZones(String ... timezones) {
    ArrayList<String> timezonesList = new ArrayList<String>(timezones.length);
    for (String timezone : timezones) {
      timezonesList.add(timezone);
    }
    return timezonesList;
  }

  private static SortedMap<Integer, String> createMapCandidate() {
    SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();
    sortedMap.put(1212, NEW_YORK_TZ);
    sortedMap.put(1213, NEW_YORK_TZ);
    sortedMap.put(1214, NEW_YORK_TZ);
    sortedMap.put(1480, CHICAGO_TZ);
    return sortedMap;
  }

  public void testLookupTimeZonesForNumberCountryLevel_US() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(1000000000L);
    assertEquals(buildListOfTimeZones(NEW_YORK_TZ, CHICAGO_TZ, LOS_ANGELES_TZ, DENVER_TZ),
                 prefixTimeZonesMapForUS.lookupTimeZonesForNumber(number));
  }

  public void testLookupTimeZonesForNumber_ValidNumber_Chicago() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(2051235458L);
    assertEquals(buildListOfTimeZones(CHICAGO_TZ),
                 prefixTimeZonesMapForUS.lookupTimeZonesForNumber(number));
  }

  public void testLookupTimeZonesForNumber_LA() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(2082924565L);
    assertEquals(buildListOfTimeZones(LOS_ANGELES_TZ),
                 prefixTimeZonesMapForUS.lookupTimeZonesForNumber(number));
  }

  public void testLookupTimeZonesForNumber_NY() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(2016641234L);
    assertEquals(buildListOfTimeZones(NEW_YORK_TZ),
                 prefixTimeZonesMapForUS.lookupTimeZonesForNumber(number));
  }

  public void testLookupTimeZonesForNumber_CH() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(41).setNationalNumber(446681300L);
    assertEquals(buildListOfTimeZones(),
                 prefixTimeZonesMapForUS.lookupTimeZonesForNumber(number));
  }

  public void testLookupTimeZonesForNumber_RU() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(7).setNationalNumber(87945154L);
    assertEquals(buildListOfTimeZones(MOSCOW_TZ),
                 prefixTimeZonesMapForRU.lookupTimeZonesForNumber(number));

    number.setNationalNumber(421548578L);
    assertEquals(buildListOfTimeZones(VLADIVOSTOK_TZ),
                 prefixTimeZonesMapForRU.lookupTimeZonesForNumber(number));

    number.setNationalNumber(342457897L);
    assertEquals(buildListOfTimeZones(YEKATERINBURG_TZ),
                 prefixTimeZonesMapForRU.lookupTimeZonesForNumber(number));

    // A mobile number
    number.setNationalNumber(9342457897L);
    assertEquals(buildListOfTimeZones(),
                 prefixTimeZonesMapForRU.lookupTimeZonesForNumber(number));

    // An invalid number (too short)
    number.setNationalNumber(3951L);
    assertEquals(buildListOfTimeZones(IRKUTSK_TZ),
                 prefixTimeZonesMapForRU.lookupTimeZonesForNumber(number));
  }

  /**
   * Creates a new PrefixTimeZonesMap serializing the provided map to a stream and then reading
   * this stream. The resulting PrefixTimeZonesMap is expected to be strictly equal to the provided
   * one from which it was generated.
   */
  private static PrefixTimeZonesMap createNewPrefixTimeZonesMap(
      PrefixTimeZonesMap prefixTimeZonesMap) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    prefixTimeZonesMap.writeExternal(objectOutputStream);
    objectOutputStream.flush();

    PrefixTimeZonesMap newPrefixTimeZonesMap = new PrefixTimeZonesMap();
    newPrefixTimeZonesMap.readExternal(
        new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
    return newPrefixTimeZonesMap;
  }

  public void testReadWriteExternal() throws IOException {
    PrefixTimeZonesMap localPrefixTimeZonesMap = new PrefixTimeZonesMap();
    localPrefixTimeZonesMap.readPrefixTimeZonesMap(createMapCandidate());

    PrefixTimeZonesMap newPrefixTimeZonesMap = createNewPrefixTimeZonesMap(localPrefixTimeZonesMap);
    assertEquals(localPrefixTimeZonesMap.toString(), newPrefixTimeZonesMap.toString());
  }
}
