/*
 * Copyright (C) 2015 The Libphonenumber Authors
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
import java.util.concurrent.ConcurrentHashMap;
import junit.framework.TestCase;

/**
 * Unit tests for MultiFileMetadataSourceImpl.java.
 */
public final class MultiFileMetadataSourceImplTest extends TestCase {
  private static final MultiFileMetadataSourceImpl source =
      new MultiFileMetadataSourceImpl(PhoneNumberUtil.DEFAULT_METADATA_LOADER);
  private static final MultiFileMetadataSourceImpl missingFileSource =
      new MultiFileMetadataSourceImpl("no/such/file", PhoneNumberUtil.DEFAULT_METADATA_LOADER);

  public void testGeoPhoneNumberMetadataLoadCorrectly() {
    // We should have some data for the UAE.
    PhoneMetadata uaeMetadata = source.getMetadataForRegion("AE");
    assertEquals(uaeMetadata.getCountryCode(), 971);
    assertTrue(uaeMetadata.hasGeneralDesc());
  }

  public void testGeoPhoneNumberMetadataLoadFromMissingFileThrowsException() throws Exception {
    try {
      missingFileSource.getMetadataForRegion("AE");
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue("Unexpected error: " + e, e.getMessage().contains("no/such/file"));
    }
  }

  public void testNonGeoPhoneNumberMetadataLoadCorrectly() {
    // We should have some data for international toll-free numbers.
    PhoneMetadata intlMetadata = source.getMetadataForNonGeographicalRegion(800);
    assertEquals(intlMetadata.getId(), "001");
    assertTrue(intlMetadata.hasGeneralDesc());
  }

  public void testNonGeoPhoneNumberMetadataLoadFromMissingFileThrowsException() throws Exception {
    try {
      missingFileSource.getMetadataForNonGeographicalRegion(800);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue("Unexpected error: " + e, e.getMessage().contains("no/such/file"));
    }
  }
}
