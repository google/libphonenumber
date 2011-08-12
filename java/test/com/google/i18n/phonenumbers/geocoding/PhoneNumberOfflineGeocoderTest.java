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

import java.util.Locale;

/**
 * Unit tests for PhoneNumberOfflineGeocoder.java
 *
 * @author Shaopeng Jia
 */
public class PhoneNumberOfflineGeocoderTest extends TestCase {
  private final PhoneNumberOfflineGeocoder geocoder =
      new PhoneNumberOfflineGeocoder(TEST_MAPPING_DATA_DIRECTORY);
  private static final String TEST_MAPPING_DATA_DIRECTORY =
      "/com/google/i18n/phonenumbers/geocoding/testing_data/";

  // Set up some test numbers to re-use.
  private static final PhoneNumber KO_NUMBER1 =
      new PhoneNumber().setCountryCode(82).setNationalNumber(22123456L);
  private static final PhoneNumber KO_NUMBER2 =
      new PhoneNumber().setCountryCode(82).setNationalNumber(322123456L);
  private static final PhoneNumber KO_NUMBER3 =
      new PhoneNumber().setCountryCode(82).setNationalNumber(6421234567L);
  private static final PhoneNumber KO_INVALID_NUMBER =
      new PhoneNumber().setCountryCode(82).setNationalNumber(1234L);
  private static final PhoneNumber US_NUMBER1 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6502530000L);
  private static final PhoneNumber US_NUMBER2 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6509600000L);
  private static final PhoneNumber US_NUMBER3 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2128120000L);
  private static final PhoneNumber US_INVALID_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(123456789L);
  private static final PhoneNumber BS_NUMBER1 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2423651234L);
  private static final PhoneNumber AU_NUMBER =
      new PhoneNumber().setCountryCode(61).setNationalNumber(236618300L);
  private static final PhoneNumber NUMBER_WITH_INVALID_COUNTRY_CODE =
      new PhoneNumber().setCountryCode(999).setNationalNumber(2423651234L);

  public void testGetDescriptionForNumberWithNoDataFile() {
    // No data file containing mappings for US numbers is available in Chinese for the unittests. As
    // a result, the country name of United States in simplified Chinese is returned.
    assertEquals("\u7F8E\u56FD",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.SIMPLIFIED_CHINESE));
    assertEquals("Stati Uniti",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.ITALIAN));
    assertEquals("Bahamas",
        geocoder.getDescriptionForNumber(BS_NUMBER1, new Locale("en", "US")));
    assertEquals("Australia",
        geocoder.getDescriptionForNumber(AU_NUMBER, new Locale("en", "US")));
    assertEquals("", geocoder.getDescriptionForNumber(NUMBER_WITH_INVALID_COUNTRY_CODE,
                                                      new Locale("en", "US")));
  }

  public void testGetDescriptionForNumber_en_US() {
    assertEquals("CA",
        geocoder.getDescriptionForNumber(US_NUMBER1, new Locale("en", "US")));
    assertEquals("Mountain View, CA",
        geocoder.getDescriptionForNumber(US_NUMBER2, new Locale("en", "US")));
    assertEquals("New York, NY",
        geocoder.getDescriptionForNumber(US_NUMBER3, new Locale("en", "US")));
  }

  public void testGetDescriptionForKoreanNumber() {
    assertEquals("Seoul",
        geocoder.getDescriptionForNumber(KO_NUMBER1, Locale.ENGLISH));
    assertEquals("Incheon",
        geocoder.getDescriptionForNumber(KO_NUMBER2, Locale.ENGLISH));
    assertEquals("Jeju",
        geocoder.getDescriptionForNumber(KO_NUMBER3, Locale.ENGLISH));
    assertEquals("\uC11C\uC6B8",
        geocoder.getDescriptionForNumber(KO_NUMBER1, Locale.KOREAN));
    assertEquals("\uC778\uCC9C",
        geocoder.getDescriptionForNumber(KO_NUMBER2, Locale.KOREAN));
    assertEquals("\uC81C\uC8FC",
        geocoder.getDescriptionForNumber(KO_NUMBER3, Locale.KOREAN));
  }

  public void testGetDescriptionForInvalidNumber() {
    assertEquals("", geocoder.getDescriptionForNumber(KO_INVALID_NUMBER, Locale.ENGLISH));
    assertEquals("", geocoder.getDescriptionForNumber(US_INVALID_NUMBER, Locale.ENGLISH));
  }
}
