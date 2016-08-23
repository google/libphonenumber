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
  private static final PhoneNumber KO_MOBILE =
      new PhoneNumber().setCountryCode(82).setNationalNumber(101234567L);
  private static final PhoneNumber US_NUMBER1 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6502530000L);
  private static final PhoneNumber US_NUMBER2 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6509600000L);
  private static final PhoneNumber US_NUMBER3 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2128120000L);
  private static final PhoneNumber US_NUMBER4 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6174240000L);
  private static final PhoneNumber US_INVALID_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(123456789L);
  private static final PhoneNumber NANPA_TOLL_FREE =
      new PhoneNumber().setCountryCode(1).setNationalNumber(8002431234L);
  private static final PhoneNumber BS_NUMBER1 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2423651234L);
  private static final PhoneNumber AU_NUMBER =
      new PhoneNumber().setCountryCode(61).setNationalNumber(236618300L);
  private static final PhoneNumber AR_MOBILE_NUMBER =
      new PhoneNumber().setCountryCode(54).setNationalNumber(92214000000L);
  private static final PhoneNumber NUMBER_WITH_INVALID_COUNTRY_CODE =
      new PhoneNumber().setCountryCode(999).setNationalNumber(2423651234L);
  private static final PhoneNumber INTERNATIONAL_TOLL_FREE =
      new PhoneNumber().setCountryCode(800).setNationalNumber(12345678L);

  public void testGetDescriptionForNumberWithNoDataFile() {
    // No data file containing mappings for US numbers is available in Chinese for the unittests. As
    // a result, the country name of United States in simplified Chinese is returned.
    assertEquals("\u7F8E\u56FD",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.SIMPLIFIED_CHINESE));
    assertEquals("Bahamas",
        geocoder.getDescriptionForNumber(BS_NUMBER1, new Locale("en", "US")));
    assertEquals("Australia",
        geocoder.getDescriptionForNumber(AU_NUMBER, new Locale("en", "US")));
    assertEquals("", geocoder.getDescriptionForNumber(NUMBER_WITH_INVALID_COUNTRY_CODE,
                                                      new Locale("en", "US")));
    assertEquals("", geocoder.getDescriptionForNumber(INTERNATIONAL_TOLL_FREE,
                                                      new Locale("en", "US")));
  }

  public void testGetDescriptionForNumberWithMissingPrefix() {
    // Test that the name of the country is returned when the number passed in is valid but not
    // covered by the geocoding data file.
    assertEquals("United States",
        geocoder.getDescriptionForNumber(US_NUMBER4, new Locale("en", "US")));
  }

  public void testGetDescriptionForNumberBelongingToMultipleCountriesIsEmpty() {
      // Test that nothing is returned when the number passed in is valid but not
      // covered by the geocoding data file and belongs to multiple countries
      assertEquals("",
          geocoder.getDescriptionForNumber(NANPA_TOLL_FREE, new Locale("en", "US")));
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
  }

  public void testGetDescriptionForArgentinianMobileNumber() {
    assertEquals("La Plata",
        geocoder.getDescriptionForNumber(AR_MOBILE_NUMBER, Locale.ENGLISH));
  }

  public void testGetDescriptionForFallBack() {
    // No fallback, as the location name for the given phone number is available in the requested
    // language.
    assertEquals("Kalifornien",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.GERMAN));
    // German falls back to English.
    assertEquals("New York, NY",
        geocoder.getDescriptionForNumber(US_NUMBER3, Locale.GERMAN));
    // Italian falls back to English.
    assertEquals("CA",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.ITALIAN));
    // Korean doesn't fall back to English.
    assertEquals("\uB300\uD55C\uBBFC\uAD6D",
        geocoder.getDescriptionForNumber(KO_NUMBER3, Locale.KOREAN));
  }

  public void testGetDescriptionForNumberWithUserRegion() {
    // User in Italy, American number. We should just show United States, in Spanish, and not more
    // detailed information.
    assertEquals("Estados Unidos",
        geocoder.getDescriptionForNumber(US_NUMBER1, new Locale("es", "ES"), "IT"));
    // Unknown region - should just show country name.
    assertEquals("Estados Unidos",
        geocoder.getDescriptionForNumber(US_NUMBER1, new Locale("es", "ES"), "ZZ"));
    // User in the States, language German, should show detailed data.
    assertEquals("Kalifornien",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.GERMAN, "US"));
    // User in the States, language French, no data for French, so we fallback to English detailed
    // data.
    assertEquals("CA",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.FRENCH, "US"));
    // Invalid number - return an empty string.
    assertEquals("", geocoder.getDescriptionForNumber(US_INVALID_NUMBER, Locale.ENGLISH,
                                                      "US"));
  }

  public void testGetDescriptionForInvalidNumber() {
    assertEquals("", geocoder.getDescriptionForNumber(KO_INVALID_NUMBER, Locale.ENGLISH));
    assertEquals("", geocoder.getDescriptionForNumber(US_INVALID_NUMBER, Locale.ENGLISH));
  }

  public void testGetDescriptionForNonGeographicalNumberWithGeocodingPrefix() {
    // We have a geocoding prefix, but we shouldn't use it since this is not geographical.
    assertEquals("South Korea", geocoder.getDescriptionForNumber(KO_MOBILE, Locale.ENGLISH));
  }
}
