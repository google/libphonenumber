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

import junit.framework.TestCase;

/**
 * Unit tests for SingleFileMetadataSourceImpl.java.
 *
 * <p>
 * We do not package single file metadata files, so it is only possible to test failures here.
 */
public class SingleFileMetadataSourceImplTest extends TestCase {
  private static final SingleFileMetadataSourceImpl MISSING_FILE_SOURCE =
      new SingleFileMetadataSourceImpl("no/such/file", MetadataManager.DEFAULT_METADATA_LOADER);

  public void testGeoPhoneNumberMetadataLoadFromMissingFileThrowsException() throws Exception {
    try {
      MISSING_FILE_SOURCE.getMetadataForRegion("AE");
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue("Unexpected error: " + e, e.getMessage().contains("no/such/file"));
    }
  }

  public void testNonGeoPhoneNumberMetadataLoadFromMissingFileThrowsException() throws Exception {
    try {
      MISSING_FILE_SOURCE.getMetadataForNonGeographicalRegion(800);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue("Unexpected error: " + e, e.getMessage().contains("no/such/file"));
    }
  }
}
