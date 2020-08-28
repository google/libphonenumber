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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a migration operation for a given country where each {@link MigrationJob} contains
 * a list of {@link MigrationEntry}'s to be migrated as well as the {@link CsvTable} which will
 * hold the available recipes that can be performed on the range. Each MigrationEntry has
 * the E.164 {@link DigitSequence} representation of a number along with the raw input
 * String originally entered. Only recipes from the given BCP-47 countryCode will be used.
 */
public final class MigrationJob {

  private final CsvTable<RangeKey> rangesTable;
  private final CsvTable<RangeKey> recipesTable;
  private final ImmutableList<MigrationEntry> migrationEntries;
  private final DigitSequence countryCode;

  MigrationJob(ImmutableList<MigrationEntry> migrationEntries,
      DigitSequence countryCode,
      CsvTable<RangeKey> recipesTable,
      CsvTable<RangeKey> rangesTable) {
    this.migrationEntries = migrationEntries;
    this.countryCode = countryCode;
    this.recipesTable = recipesTable;
    this.rangesTable = rangesTable;
  }

  public DigitSequence getCountryCode() {
    return countryCode;
  }

  public CsvTable<RangeKey> getRecipesCsvTable() {
    return recipesTable;
  }

  public RangeTable getRecipesRangeTable() {
    return RecipesTableSchema.toRangeTable(recipesTable);
  }

  public Stream<MigrationEntry> getMigrationEntries() {
    return migrationEntries.stream();
  }

  /**
   * Retrieves all migratable numbers from the numberRange and attempts to migrate them with recipes
   * from the recipesTable that belong to the given country code.
   */
  public MigrationReport performAllMigrations() {
    ImmutableList<MigrationEntry> migratableRange = MigrationUtils
        .getMigratableRangeByCountry(getRecipesRangeTable(), countryCode, getMigrationEntries())
        .collect(ImmutableList.toImmutableList());

    ImmutableList.Builder<MigrationResult> migratedResults = ImmutableList.builder();
    migratableRange.forEach(entry -> {
      ImmutableMap<Column<?>, Object> matchingRecipe = MigrationUtils
          .findMatchingRecipe(getRecipesCsvTable(), countryCode, entry.getSanitizedNumber())
          // can never be thrown here because every staleNumber is from the migratableNumbers set
          .orElseThrow(RuntimeException::new);

       migratedResults.add(migrate(entry.getSanitizedNumber(), matchingRecipe, entry.getOriginalNumber()));
    });

    // TODO: add strict/lenient flag to specify if invalid migrations should be rolled back or used anyway
    // TODO: this should have multiplicity with custom recipes due to it not being possible for it
    Stream<MigrationEntry> untouchedEntries = getMigrationEntries()
        .filter(entry -> !migratableRange.contains(entry));

    if (rangesTable != null) {
      return new MigrationReport(untouchedEntries, verifyMigratedNumbers(migratedResults.build()));
    } else {
      return new MigrationReport(untouchedEntries,
          ImmutableMap.of("Valid", migratedResults.build(), "Invalid", ImmutableList.of()));
    }
  }

  /**
   * Retrieves all migratable numbers from the numberRange that can be migrated using the given
   * recipeKey and attempts to migrate them with recipes from the recipesTable that belong to the
   * given country code.
   */
  public Optional<MigrationReport> performSingleRecipeMigration(RangeKey recipeKey) {
    ImmutableMap<Column<?>, Object> recipeRow = getRecipesCsvTable().getRow(recipeKey);
    ImmutableList<MigrationEntry> migratableRange = MigrationUtils
        .getMigratableRangeByRecipe(getRecipesCsvTable(), recipeKey, getMigrationEntries())
        .collect(ImmutableList.toImmutableList());

    ImmutableList.Builder<MigrationResult> migratedResults = ImmutableList.builder();
    if (!recipeRow.get(RecipesTableSchema.COUNTRY_CODE).equals(countryCode)) {
      return Optional.empty();
    }
    migratableRange.forEach(entry -> {
        migratedResults.add(migrate(entry.getSanitizedNumber(), recipeRow, entry.getOriginalNumber()));
    });

    // TODO: add strict/lenient flag to specify if invalid migrations should be rolled back or used anyway
    // TODO: this should have multiplicity with custom recipes due to it not being possible for it
    Stream<MigrationEntry> untouchedEntries = getMigrationEntries()
        .filter(entry -> !migratableRange.contains(entry));

    if (rangesTable != null) {
      return Optional.of(new MigrationReport(untouchedEntries,
          verifyMigratedNumbers(migratedResults.build())));
    }
    // TODO: fix MigrationJobTest
    return Optional.of(new MigrationReport(untouchedEntries,
        ImmutableMap.of("Valid", migratedResults.build(), "Invalid", ImmutableList.of())));
  }

  /**
   * Takes a given number and migrates it using the given matching recipe row. If the given recipe
   * is not a final migration, the method is recursively called with the recipe that matches the new
   * migrated number until a recipe that produces a final migration (a recipe that results in the
   * new format being valid and dialable) has been used. Once this occurs, the {@link MigrationResult}
   * is returned.
   *
   * @throws IllegalArgumentException if the 'Old Format' value in the given recipe row does not
   * match the number to migrate. This means that the 'Old Format' value cannot be represented by
   * the given recipes 'Old Prefix' and 'Old Length'.
   * @throws RuntimeException when the given recipe is not a final migration and a recipe cannot be
   * found in the recipesTable to match the resulting number from the initial migrating recipe.
   */
  private MigrationResult migrate(DigitSequence migratingNumber,
      ImmutableMap<Column<?>, Object> recipeRow,
      String originalNumber) {
    String oldFormat = (String) recipeRow.get(RecipesTableSchema.OLD_FORMAT);
    String newFormat = (String) recipeRow.get(RecipesTableSchema.NEW_FORMAT);

    if (!RangeSpecification.parse(oldFormat).matches(migratingNumber)) {
      throw new IllegalArgumentException(
          "value '" + oldFormat + "' in column 'Old Format' cannot be"
              + " represented by its given recipe key (Old Prefix + Old Length)");
    }
    String staleString = migratingNumber.toString();
    StringBuilder newString = new StringBuilder();

    for (int i = 0; i < oldFormat.length(); i++) {
      if (!Character.isDigit(oldFormat.charAt(i))) {
        newString.append(staleString.charAt(i));
      }
    }

    int newFormatPointer = 0;
    for (int i = 0; i < Math.max(oldFormat.length(), newFormat.length()); i++) {
      if (i < newFormat.length() && i == newFormatPointer
          && Character.isDigit(newFormat.charAt(i))) {
        do {
          newString.insert(newFormatPointer, newFormat.charAt(newFormatPointer++));
        } while (newFormatPointer < newFormat.length()
            && Character.isDigit(newFormat.charAt(newFormatPointer)));
      }
      if (newFormatPointer == i) {
        newFormatPointer++;
      }
    }

    if (!(boolean) recipeRow.get(RecipesTableSchema.IS_FINAL_MIGRATION)) {
      ImmutableMap<Column<?>, Object> nextRecipeRow =
          MigrationUtils.findMatchingRecipe(getRecipesCsvTable(), countryCode,
              DigitSequence.of(newString.toString()))
              .orElseThrow(() -> new RuntimeException(
                  "A multiple migration was required for the stale number '" + originalNumber
                      + "' but no other recipe could be found after migrating the number into +"
                      + newString));

      return migrate(DigitSequence.of(newString.toString()), nextRecipeRow, originalNumber);
    }
    return MigrationResult.create(DigitSequence.of(newString.toString()), originalNumber);
  }

  // TODO: add method comment
  // TODO: fix MigrationJobTest
  private ImmutableMap<String, ImmutableList<MigrationResult>> verifyMigratedNumbers(
      ImmutableList<MigrationResult> migrations) {
    ImmutableList.Builder<MigrationResult> validMigrations = ImmutableList.builder();
    ImmutableList.Builder<MigrationResult> invalidMigrations = ImmutableList.builder();

    RangeTree validRanges = RangesTableSchema.toRangeTable(rangesTable).getAllRanges();
    for (MigrationResult migration : migrations) {
      DigitSequence migratedNum = migration.getMigratedNumber();
      if (migratedNum.length() <= countryCode.length()) {
        invalidMigrations.add(migration);
        continue;
      }
      if(validRanges.contains(migratedNum.last(migratedNum.length() - countryCode.length()))) {
        validMigrations.add(migration);
      } else {
        invalidMigrations.add(migration);
      }
    }
    return ImmutableMap.of("Valid", validMigrations.build(), "Invalid", invalidMigrations.build());
  }

  final class MigrationReport {
    private final ImmutableList<MigrationEntry> untouchedEntries;
    private final ImmutableList<MigrationResult> validMigrations;
    private final ImmutableList<MigrationResult> invalidMigrations;

    private MigrationReport(Stream<MigrationEntry> untouchedEntries,
        ImmutableMap<String, ImmutableList<MigrationResult>> migratedEntries) {
      this.untouchedEntries = untouchedEntries.collect(ImmutableList.toImmutableList());
      this.validMigrations = migratedEntries.get("Valid");
      this.invalidMigrations = migratedEntries.get("Invalid");
    }

    public ImmutableList<MigrationResult> getValidMigrations() {
      return validMigrations;
    }

    public ImmutableList<MigrationResult> getInvalidMigrations() {
      return invalidMigrations;
    }

    public String exportToFile(String originalFileName) throws IOException {
      String newFileLocation = System.getProperty("user.dir") + "/+" + countryCode + "_Migration_" +
          originalFileName;
      FileWriter fw = new FileWriter(newFileLocation);

      for (MigrationResult result : validMigrations) {
        fw.write(result.getMigratedNumber() + "\n");
      }
      for (MigrationResult result : invalidMigrations) {
        fw.write(result.getOriginalNumber() + "\n");
      }
      for (MigrationEntry entry : untouchedEntries) {
        fw.write(entry.getOriginalNumber() + "\n");
      }
      fw.close();
      return newFileLocation;
    }

    public ImmutableList<MigrationEntry> getValidUntouchedEntries() {
      if (rangesTable == null) {
        return ImmutableList.of();
      }
      ImmutableList.Builder<MigrationEntry> validEntries = ImmutableList.builder();
      RangeTree validRanges = RangesTableSchema.toRangeTable(rangesTable).getAllRanges();

      for (MigrationEntry entry : untouchedEntries) {
        DigitSequence sanitizedNum = entry.getSanitizedNumber();
        if (sanitizedNum.length() <= countryCode.length() ||
            !sanitizedNum.first(countryCode.length()).equals(countryCode)) {
          continue;
        }
        if(validRanges.contains(sanitizedNum.last(sanitizedNum.length() - countryCode.length()))) {
          validEntries.add(entry);
        }
      }
      return validEntries.build();
    }

    public void printMetrics() {
      int migratedCount = validMigrations.size() + invalidMigrations.size();
      int totalCount = untouchedEntries.size() + migratedCount;

      System.out.println("\nMetrics:");
      System.out.println("* " + migratedCount + " out of the " + totalCount + " inputted numbers "
          + "were/was migrated");

      if (rangesTable == null) {
        System.out.println("* Migrated numbers:");
        validMigrations.forEach(val -> System.out.println("\t" + val));
        System.out.println("\n* Untouched number(s):");
        untouchedEntries.forEach(val -> System.out.println("\t'" + val.getOriginalNumber() + "'"));

      } else {
        ImmutableList<MigrationEntry> validUntouchedEntries = getValidUntouchedEntries();
        System.out.println("* " + validMigrations.size() + " out of the " + migratedCount +
            " migrated numbers were/was verified as being in a valid, dialable format based on our "
            + "data for the given country");
        System.out.println("* " + validUntouchedEntries.size() + " out of the " +
            untouchedEntries.size() + " non-migrated numbers were/was already in a valid, dialable "
            + "format based on our data for the given country");

        System.out.println("\n* Valid number(s):");
        validMigrations.forEach(val -> System.out.println("\t" + val));
        validUntouchedEntries.forEach(val -> System.out.println("\t'" + val.getOriginalNumber()
            + "' (untouched)"));

        System.out.println("\n* Invalid migrated number(s):");
        invalidMigrations.forEach(val -> System.out.println("\t" + val));

        System.out.println("\n* Untouched number(s):");
        untouchedEntries.forEach(val -> {
          if (validUntouchedEntries.contains(val)) {
            System.out.println("\t'" + val.getOriginalNumber() + "' (already valid)");
          } else {
            System.out.println("\t'" + val.getOriginalNumber() + "'");
          }
        });
      }
    }
  }
}
