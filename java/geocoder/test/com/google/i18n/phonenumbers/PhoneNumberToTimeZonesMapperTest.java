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

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for PhoneNumberToTimeZonesMapper.java
 *
 * @author Walter Erquinigo
 */
public class PhoneNumberToTimeZonesMapperTest extends TestCase {
  private final PhoneNumberToTimeZonesMapper prefixTimeZonesMapper =
      new PhoneNumberToTimeZonesMapper(TEST_MAPPING_DATA_DIRECTORY);
  private static final String TEST_MAPPING_DATA_DIRECTORY =
      "/com/google/i18n/phonenumbers/timezones/testing_data/";
  // Set up some test numbers to re-use.
  private static final PhoneNumber AU_NUMBER =
      new PhoneNumber().setCountryCode(61).setNationalNumber(236618300L);
  private static final PhoneNumber CA_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6048406565L);
  private static final PhoneNumber KO_NUMBER =
      new PhoneNumber().setCountryCode(82).setNationalNumber(22123456L);
  private static final PhoneNumber KO_INVALID_NUMBER =
      new PhoneNumber().setCountryCode(82).setNationalNumber(1234L);
  private static final PhoneNumber US_NUMBER1 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6509600000L);
  private static final PhoneNumber US_NUMBER2 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2128120000L);
  private static final PhoneNumber US_NUMBER3 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6174240000L);
  private static final PhoneNumber US_INVALID_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(123456789L);
  private static final PhoneNumber NUMBER_WITH_INVALID_COUNTRY_CODE =
      new PhoneNumber().setCountryCode(999).setNationalNumber(2423651234L);
  private static final PhoneNumber INTERNATIONAL_TOLL_FREE =
      new PhoneNumber().setCountryCode(800).setNationalNumber(12345678L);

  // NANPA time zones.
  private static final String CHICAGO_TZ = "America/Chicago";
  private static final String LOS_ANGELES_TZ = "America/Los_Angeles";
  private static final String NEW_YORK_TZ = "America/New_York";
  private static final String WINNIPEG_TZ = "America/Winnipeg";
  // Non NANPA time zones.
  private static final String SEOUL_TZ = "Asia/Seoul";
  private static final String SYDNEY_TZ = "Australia/Sydney";

  static List<String> buildListOfTimeZones(String ... timezones) {
    ArrayList<String> timezonesList = new ArrayList<String>(timezones.length);
    for (String timezone : timezones) {
      timezonesList.add(timezone);
    }
    return timezonesList;
  }

  private static List<String> getNanpaTimeZonesList() {
    return buildListOfTimeZones(NEW_YORK_TZ, CHICAGO_TZ, WINNIPEG_TZ, LOS_ANGELES_TZ);
  }

  public void testGetTimeZonesForNumber() {
    // Test with invalid numbers even when their country code prefixes exist in the mapper.
    assertEquals(PhoneNumberToTimeZonesMapper.UNKNOWN_TIME_ZONE_LIST,
                 prefixTimeZonesMapper.getTimeZonesForNumber(US_INVALID_NUMBER));
    assertEquals(PhoneNumberToTimeZonesMapper.UNKNOWN_TIME_ZONE_LIST,
                 prefixTimeZonesMapper.getTimeZonesForNumber(KO_INVALID_NUMBER));
    // Test with valid prefixes.
    assertEquals(buildListOfTimeZones(SYDNEY_TZ),
                 prefixTimeZonesMapper.getTimeZonesForNumber(AU_NUMBER));
    assertEquals(buildListOfTimeZones(SEOUL_TZ),
                 prefixTimeZonesMapper.getTimeZonesForNumber(KO_NUMBER));
    assertEquals(buildListOfTimeZones(WINNIPEG_TZ),
                 prefixTimeZonesMapper.getTimeZonesForNumber(CA_NUMBER));
    assertEquals(buildListOfTimeZones(LOS_ANGELES_TZ),
                 prefixTimeZonesMapper.getTimeZonesForNumber(US_NUMBER1));
    assertEquals(buildListOfTimeZones(NEW_YORK_TZ),
                 prefixTimeZonesMapper.getTimeZonesForNumber(US_NUMBER2));
    // Test with an invalid country code.
    assertEquals(PhoneNumberToTimeZonesMapper.UNKNOWN_TIME_ZONE_LIST,
                 prefixTimeZonesMapper.getTimeZonesForNumber(NUMBER_WITH_INVALID_COUNTRY_CODE));
    // Test with a non geographical phone number.
    assertEquals(PhoneNumberToTimeZonesMapper.UNKNOWN_TIME_ZONE_LIST,
                 prefixTimeZonesMapper.getTimeZonesForNumber(INTERNATIONAL_TOLL_FREE));
  }

  public void testGetTimeZonesForValidNumber() {
    // Test with invalid numbers even when their country code prefixes exist in the mapper.
    assertEquals(getNanpaTimeZonesList(),
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(US_INVALID_NUMBER));
    assertEquals(buildListOfTimeZones(SEOUL_TZ),
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(KO_INVALID_NUMBER));
    // Test with valid prefixes.
    assertEquals(buildListOfTimeZones(SYDNEY_TZ),
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(AU_NUMBER));
    assertEquals(buildListOfTimeZones(SEOUL_TZ),
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(KO_NUMBER));
    assertEquals(buildListOfTimeZones(WINNIPEG_TZ),
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(CA_NUMBER));
    assertEquals(buildListOfTimeZones(LOS_ANGELES_TZ),
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(US_NUMBER1));
    assertEquals(buildListOfTimeZones(NEW_YORK_TZ),
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(US_NUMBER2));
    // Test with an invalid country code.
    assertEquals(PhoneNumberToTimeZonesMapper.UNKNOWN_TIME_ZONE_LIST,
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(
                     NUMBER_WITH_INVALID_COUNTRY_CODE));
    // Test with a non geographical phone number.
    assertEquals(PhoneNumberToTimeZonesMapper.UNKNOWN_TIME_ZONE_LIST,
                 prefixTimeZonesMapper.getTimeZonesForGeographicalNumber(
                     INTERNATIONAL_TOLL_FREE));
  }

  public void testGetTimeZonesForValidNumberSearchingAtCountryCodeLevel() {
    // Test that the country level time zones are returned when the number passed in is valid but
    // not covered by any non-country level prefixes in the mapper.
    assertEquals(prefixTimeZonesMapper.getTimeZonesForNumber(US_NUMBER3),
                 getNanpaTimeZonesList());
  }
}
