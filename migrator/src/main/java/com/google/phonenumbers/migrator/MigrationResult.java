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
 * Representation of the result for a given MigrationEntry. Contains the {@link MigrationEntry} with
 * its migrated E.164 {@link DigitSequence} value.
 */
@AutoValue
public abstract class MigrationResult {
  public abstract DigitSequence getMigratedNumber();
  public abstract MigrationEntry getMigrationEntry();

  public static MigrationResult create(DigitSequence migratedNumber,
      MigrationEntry migrationEntry) {
    return new AutoValue_MigrationResult(migratedNumber, migrationEntry);
  }

  @Override
  public String toString() {
    return getMigrationEntry().getOriginalNumber() + "  ->  +" + getMigratedNumber();
  }
}