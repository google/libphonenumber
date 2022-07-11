/*
 * Copyright (C) 2022 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.metadata.source;

import junit.framework.TestCase;

public final class SingleFileModeFileNameProviderTest extends TestCase {

  private final PhoneMetadataFileNameProvider metadataFileNameProvider =
      new SingleFileModeFileNameProvider("some/file");

  public void test_getFor_shouldReturnTheFileNameBase() {
    String metadataFileName = metadataFileNameProvider.getFor("key1");

    assertEquals("some/file", metadataFileName);
  }
}
