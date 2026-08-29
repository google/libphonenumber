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


/**
 * {@link PhoneMetadataFileNameProvider} implementation which appends key as a suffix to the
 * predefined metadata file name base.
 */
public final class MultiFileModeFileNameProvider implements PhoneMetadataFileNameProvider {

  private final String phoneMetadataFileNamePrefix;

  public MultiFileModeFileNameProvider(String phoneMetadataFileNameBase) {
    this.phoneMetadataFileNamePrefix = phoneMetadataFileNameBase + "_";
  }

  @Override
  public String getFor(Object key) {
    String keyAsString = key.toString();
    if (!isAlphanumeric(keyAsString)) {
      throw new IllegalArgumentException("Invalid key: " + keyAsString);
    }
    return phoneMetadataFileNamePrefix + key;
  }

  private boolean isAlphanumeric(String key) {
    if (key == null || key.length() == 0) {
      return false;
    }
    // String#length doesn't actually return the number of
    // code points in the String, it returns the number
    // of char values.
    int size = key.length();
    for (int charIdx = 0; charIdx < size; ) {
      final int codePoint = key.codePointAt(charIdx);
      if (!Character.isLetterOrDigit(codePoint)) {
        return false;
      }
      charIdx += Character.charCount(codePoint);
    }
    return true;
  }
}
