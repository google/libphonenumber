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
public class MultiFileMetadataSourceImplTest extends TestCase {
  public MultiFileMetadataSourceImplTest() {}

  public void testGeographicalRegionMetadataLoadsCorrectly() {
    ConcurrentHashMap<String, PhoneMetadata> map = new ConcurrentHashMap<String, PhoneMetadata>();
    PhoneMetadata metadata = MultiFileMetadataSourceImpl.loadMetadataFromFile(
        "CA", map, "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting",
        PhoneNumberUtil.DEFAULT_METADATA_LOADER);
    assertEquals(metadata, map.get("CA"));
  }

  public void testNonGeographicalRegionMetadataLoadsCorrectly() {
    ConcurrentHashMap<Integer, PhoneMetadata> map = new ConcurrentHashMap<Integer, PhoneMetadata>();
    PhoneMetadata metadata = MultiFileMetadataSourceImpl.loadMetadataFromFile(
        800, map, "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting",
        PhoneNumberUtil.DEFAULT_METADATA_LOADER);
    assertEquals(metadata, map.get(800));
  }

  public void testMissingMetadataFileThrowsRuntimeException() {
    // In normal usage we should never get a state where we are asking to load metadata that doesn't
    // exist. However if the library is packaged incorrectly in the jar, this could happen and the
    // best we can do is make sure the exception has the file name in it.
    try {
      MultiFileMetadataSourceImpl.loadMetadataFromFile(
          "XX", new ConcurrentHashMap<String, PhoneMetadata>(), "no/such/file",
          PhoneNumberUtil.DEFAULT_METADATA_LOADER);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue("Unexpected error: " + e, e.getMessage().contains("no/such/file_XX"));
    }
    try {
      MultiFileMetadataSourceImpl.loadMetadataFromFile(
          123, new ConcurrentHashMap<Integer, PhoneMetadata>(), "no/such/file",
          PhoneNumberUtil.DEFAULT_METADATA_LOADER);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue("Unexpected error: " + e, e.getMessage().contains("no/such/file_123"));
    }
  }
}
