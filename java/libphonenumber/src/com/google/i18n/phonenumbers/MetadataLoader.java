/*
 * Copyright (C) 2014 The Libphonenumber Authors
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

import java.io.InputStream;

/**
 * Interface for caller to specify a customized phone metadata loader.
 */
public interface MetadataLoader {
  /**
   * Returns an input stream corresponding to the metadata to load.
   *
   * @param metadataFileName File name (including path) of metadata to load. File path is an
   *     absolute class path like /com/google/i18n/phonenumbers/data/PhoneNumberMetadataProto.
   * @return The input stream for the metadata file. The library will close this stream
   *     after it is done. Return null in case the metadata file could not be found.
   */
  public InputStream loadMetadata(String metadataFileName);
}
