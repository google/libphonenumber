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

package com.google.i18n.phonenumbers.prefixmapper;

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import junit.framework.TestCase;

/**
 * Unit tests for PrefixFileReader.java
 *
 * @author Cecilia Roes
 */
public class PrefixFileReaderTest extends TestCase {
  private final PrefixFileReader reader = new PrefixFileReader(TEST_MAPPING_DATA_DIRECTORY);
  private static final String TEST_MAPPING_DATA_DIRECTORY =
      "/com/google/i18n/phonenumbers/geocoding/testing_data/";

  private static final PhoneNumber KO_NUMBER =
      new PhoneNumber().setCountryCode(82).setNationalNumber(22123456L);
  private static final PhoneNumber US_NUMBER1 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6502530000L);
  private static final PhoneNumber US_NUMBER2 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2128120000L);
  private static final PhoneNumber US_NUMBER3 =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6174240000L);
  private static final PhoneNumber SE_NUMBER =
      new PhoneNumber().setCountryCode(46).setNationalNumber(81234567L);

  public void testGetDescriptionForNumberWithMapping() {
    assertEquals("Kalifornien",
                 reader.getDescriptionForNumber(US_NUMBER1, "de", "", "CH"));
    assertEquals("CA",
                 reader.getDescriptionForNumber(US_NUMBER1, "en", "", "AU"));
    assertEquals("\uC11C\uC6B8",
                 reader.getDescriptionForNumber(KO_NUMBER, "ko", "", ""));
    assertEquals("Seoul",
                 reader.getDescriptionForNumber(KO_NUMBER, "en", "", ""));
  }

  public void testGetDescriptionForNumberWithMissingMapping() {
    assertEquals("", reader.getDescriptionForNumber(US_NUMBER3, "en", "", ""));
  }

  public void testGetDescriptionUsingFallbackLanguage() {
    // Mapping file exists but the number isn't present, causing it to fallback.
    assertEquals("New York, NY",
                 reader.getDescriptionForNumber(US_NUMBER2, "de", "", "CH"));
    // No mapping file exists, causing it to fallback.
    assertEquals("New York, NY",
                 reader.getDescriptionForNumber(US_NUMBER2, "sv", "", ""));
  }

  public void testGetDescriptionForNonFallbackLanguage() {
    assertEquals("", reader.getDescriptionForNumber(US_NUMBER2, "ko", "", ""));
  }

  public void testGetDescriptionForNumberWithoutMappingFile() {
    assertEquals("", reader.getDescriptionForNumber(SE_NUMBER, "sv", "", ""));
    assertEquals("", reader.getDescriptionForNumber(SE_NUMBER, "en", "", ""));
  }
}
