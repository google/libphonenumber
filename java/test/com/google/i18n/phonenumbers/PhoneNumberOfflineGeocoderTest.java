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

import java.util.Locale;

/**
 * Unit tests for PhoneNumberOfflineGeocoder.java
 *
 * @author Shaopeng Jia
 */
public class PhoneNumberOfflineGeocoderTest extends TestCase {
  private PhoneNumberOfflineGeocoder geocoder;
  static final String TEST_META_DATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting";

  // Set up some test numbers to re-use.
  private static final PhoneNumber US_NUMBER1 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6502530000L);
  private static final PhoneNumber BS_NUMBER1 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2423651234L);
  private static final PhoneNumber AU_NUMBER =
      new PhoneNumber().setCountryCode(61).setNationalNumber(236618300L);
  private static final PhoneNumber NUMBER_WITH_INVALID_COUNTRY_CODE =
      new PhoneNumber().setCountryCode(999).setNationalNumber(2423651234L);

  public PhoneNumberOfflineGeocoderTest() {
    PhoneNumberUtil.resetInstance();
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance(
        TEST_META_DATA_FILE_PREFIX,
        CountryCodeToRegionCodeMapForTesting.getCountryCodeToRegionCodeMap());
    geocoder = new PhoneNumberOfflineGeocoder(phoneUtil);
  }

  public void testGetCompactDescriptionForNumber() {
    assertEquals("United States",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.ENGLISH));
    assertEquals("Stati Uniti",
        geocoder.getDescriptionForNumber(US_NUMBER1, Locale.ITALIAN));
    assertEquals("Bahamas",
        geocoder.getDescriptionForNumber(BS_NUMBER1, Locale.ENGLISH));
    assertEquals("Australia",
        geocoder.getDescriptionForNumber(AU_NUMBER, Locale.ENGLISH));
    assertEquals("", geocoder.getDescriptionForNumber(NUMBER_WITH_INVALID_COUNTRY_CODE,
                                                      Locale.ENGLISH));
  }
}
