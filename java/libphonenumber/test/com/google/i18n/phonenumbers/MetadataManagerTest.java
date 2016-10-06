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

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;

import junit.framework.TestCase;

/**
 * Some basic tests to check that the phone number metadata can be correctly loaded.
 */
public class MetadataManagerTest extends TestCase {

  public void testAlternateFormatsContainsData() throws Exception {
    // We should have some data for Germany.
    PhoneMetadata germanyAlternateFormats = MetadataManager.getAlternateFormatsForCountry(49);
    assertNotNull(germanyAlternateFormats);
    assertTrue(germanyAlternateFormats.numberFormatSize() > 0);
  }

  public void testShortNumberMetadataContainsData() throws Exception {
    // We should have some data for France.
    PhoneMetadata franceShortNumberMetadata = MetadataManager.getShortNumberMetadataForRegion("FR");
    assertNotNull(franceShortNumberMetadata);
    assertTrue(franceShortNumberMetadata.hasShortCode());
  }

  public void testAlternateFormatsFailsGracefully() throws Exception {
    PhoneMetadata noAlternateFormats = MetadataManager.getAlternateFormatsForCountry(999);
    assertNull(noAlternateFormats);
  }

  public void testShortNumberMetadataFailsGracefully() throws Exception {
    PhoneMetadata noShortNumberMetadata = MetadataManager.getShortNumberMetadataForRegion("XXX");
    assertNull(noShortNumberMetadata);
  }
}
