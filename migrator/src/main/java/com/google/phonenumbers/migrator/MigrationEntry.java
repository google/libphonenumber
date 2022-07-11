/*
 * Copyright (C) 2020 The Libphonenumber Authors.
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
package com.google.phonenumbers.migrator;

import com.google.auto.value.AutoValue;
import com.google.i18n.phonenumbers.metadata.DigitSequence;

/**
 * Representation of each number to be migrated by a given MigrationJob. Contains the original number
 * string as well as the sanitized E.164 {@link DigitSequence}.
 */
@AutoValue
public abstract class MigrationEntry {
  public abstract DigitSequence getSanitizedNumber();
  public abstract String getOriginalNumber();

  public static MigrationEntry create(DigitSequence sanitizedNumber, String originalNumber) {
    return new AutoValue_MigrationEntry(sanitizedNumber, originalNumber);
  }
}
