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
