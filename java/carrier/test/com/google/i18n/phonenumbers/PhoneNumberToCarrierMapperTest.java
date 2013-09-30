/*
 * Copyright (C) 2013 The Libphonenumber Authors
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
 * Unit tests for PhoneNumberToCarrierMapper.java
 *
 * @author Cecilia Roes
 */
public class PhoneNumberToCarrierMapperTest extends TestCase {
  private final PhoneNumberToCarrierMapper carrierMapper =
      new PhoneNumberToCarrierMapper(TEST_MAPPING_DATA_DIRECTORY);
  private static final String TEST_MAPPING_DATA_DIRECTORY =
      "/com/google/i18n/phonenumbers/carrier/testing_data/";

  // Set up some test numbers to re-use.
  private static final PhoneNumber AO_MOBILE1 =
      new PhoneNumber().setCountryCode(244).setNationalNumber(917654321L);
  private static final PhoneNumber AO_MOBILE2 =
      new PhoneNumber().setCountryCode(244).setNationalNumber(927654321L);
  private static final PhoneNumber AO_FIXED1 =
      new PhoneNumber().setCountryCode(244).setNationalNumber(22254321L);
  private static final PhoneNumber AO_FIXED2 =
      new PhoneNumber().setCountryCode(244).setNationalNumber(26254321L);
  private static final PhoneNumber AO_INVALID_NUMBER =
      new PhoneNumber().setCountryCode(244).setNationalNumber(101234L);
  private static final PhoneNumber UK_MOBILE1 =
      new PhoneNumber().setCountryCode(44).setNationalNumber(7387654321L);
  private static final PhoneNumber UK_MOBILE2 =
      new PhoneNumber().setCountryCode(44).setNationalNumber(7487654321L);
  private static final PhoneNumber UK_FIXED1 =
      new PhoneNumber().setCountryCode(44).setNationalNumber(1123456789L);
  private static final PhoneNumber UK_FIXED2 =
      new PhoneNumber().setCountryCode(44).setNationalNumber(2987654321L);
  private static final PhoneNumber UK_INVALID_NUMBER =
      new PhoneNumber().setCountryCode(44).setNationalNumber(7301234L);
  private static final PhoneNumber UK_PAGER =
      new PhoneNumber().setCountryCode(44).setNationalNumber(7601234567L);
  private static final PhoneNumber US_FIXED_OR_MOBILE =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6502123456L);
  private static final PhoneNumber NUMBER_WITH_INVALID_COUNTRY_CODE =
      new PhoneNumber().setCountryCode(999).setNationalNumber(2423651234L);
  private static final PhoneNumber INTERNATIONAL_TOLL_FREE =
      new PhoneNumber().setCountryCode(800).setNationalNumber(12345678L);

  public void testGetDescriptionForMobilePortableRegion() {
    assertEquals("British carrier",
                 carrierMapper.getDescriptionForNumber(UK_MOBILE1, Locale.ENGLISH));
    assertEquals("Brittisk operatör",
                 carrierMapper.getDescriptionForNumber(UK_MOBILE1, new Locale("sv", "SE")));
    assertEquals("British carrier",
                 carrierMapper.getDescriptionForNumber(UK_MOBILE1, Locale.FRENCH));
  }

  public void testGetDescriptionForNonMobilePortableRegion() {
    assertEquals("Angolan carrier",
                 carrierMapper.getDescriptionForNumber(AO_MOBILE1, Locale.ENGLISH));
  }

  public void testGetDescriptionForFixedLineNumber() {
    assertEquals("", carrierMapper.getDescriptionForNumber(AO_FIXED1, Locale.ENGLISH));
    assertEquals("", carrierMapper.getDescriptionForNumber(UK_FIXED1, Locale.ENGLISH));
    // If the carrier information is present in the files and the method that assumes a valid
    // number is used, a carrier is returned.
    assertEquals("Angolan fixed line carrier",
                 carrierMapper.getDescriptionForValidNumber(AO_FIXED2, Locale.ENGLISH));
    assertEquals("", carrierMapper.getDescriptionForValidNumber(UK_FIXED2, Locale.ENGLISH));
  }

  public void testGetDescriptionForFixedOrMobileNumber() {
    assertEquals("US carrier", carrierMapper.getDescriptionForNumber(US_FIXED_OR_MOBILE,
                                                                     Locale.ENGLISH));
  }

  public void testGetDescriptionForPagerNumber() {
    assertEquals("British pager", carrierMapper.getDescriptionForNumber(UK_PAGER, Locale.ENGLISH));
  }

  public void testGetDescriptionForNumberWithNoDataFile() {
    assertEquals("", carrierMapper.getDescriptionForNumber(NUMBER_WITH_INVALID_COUNTRY_CODE,
                                                           Locale.ENGLISH));
    assertEquals("", carrierMapper.getDescriptionForNumber(INTERNATIONAL_TOLL_FREE,
                                                           Locale.ENGLISH));
    assertEquals("", carrierMapper.getDescriptionForValidNumber(NUMBER_WITH_INVALID_COUNTRY_CODE,
                                                                Locale.ENGLISH));
    assertEquals("", carrierMapper.getDescriptionForValidNumber(INTERNATIONAL_TOLL_FREE,
                                                                Locale.ENGLISH));
  }

  public void testGetDescriptionForNumberWithMissingPrefix() {
    assertEquals("", carrierMapper.getDescriptionForNumber(UK_MOBILE2, Locale.ENGLISH));
    assertEquals("", carrierMapper.getDescriptionForNumber(AO_MOBILE2, Locale.ENGLISH));
  }

  public void testGetDescriptionForInvalidNumber() {
    assertEquals("", carrierMapper.getDescriptionForNumber(UK_INVALID_NUMBER, Locale.ENGLISH));
    assertEquals("", carrierMapper.getDescriptionForNumber(AO_INVALID_NUMBER, Locale.ENGLISH));
  }
}
