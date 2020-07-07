/*
 * Copyright (C) 2017 The Libphonenumber Authors.
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
package com.google.i18n.phonenumbers.metadata.model;

import com.google.errorprone.annotations.FormatMethod;

/**
 * Represents an error related to CSV metadata, either structural issues in the CSV or semantic
 * errors in the XML representation. MetadataExceptions should only correspond to problems fixable
 * by editing the CSV data.
 */
public final class MetadataException extends RuntimeException {
  @FormatMethod
  public static void checkMetadata(boolean cond, String msg, Object... args) {
    if (!cond) {
      throw new MetadataException(String.format(msg, args));
    }
  }

  public MetadataException(String message) {
    super(message);
  }
}
