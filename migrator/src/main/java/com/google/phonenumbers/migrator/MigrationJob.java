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

import com.google.common.collect.*;
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
import java.io.OutputStream;
import java.util.Optional;
import com.google.common.base.Preconditions;
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
  /**
   * If true, when a {@link MigrationReport} is exported, the migrated version
   * of numbers are written to file regardless of if the migrated number was seen as valid or invalid.
   * By default, when a migration results in an invalid number for the given countryCode, the
   * original number is written to file.
   */
  private final boolean exportInvalidMigrations;

  MigrationJob(ImmutableList<MigrationEntry> migrationEntries,
      DigitSequence countryCode,
      CsvTable<RangeKey> recipesTable,
      CsvTable<RangeKey> rangesTable,
      boolean exportInvalidMigrations) {
    this.migrationEntries = migrationEntries;
    this.countryCode = countryCode;
    this.recipesTable = recipesTable;
    this.rangesTable = rangesTable;
    this.exportInvalidMigrations = exportInvalidMigrations;
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
  public MigrationReport getMigrationReportForCountry() {
    ImmutableList<MigrationEntry> migratableRange = MigrationUtils
        .getMigratableRangeByCountry(getRecipesRangeTable(), countryCode, getMigrationEntries())
        .collect(ImmutableList.toImmutableList());

    ImmutableList.Builder<MigrationResult> migratedResults = ImmutableList.builder();
    for (MigrationEntry entry : migratableRange) {
      MigrationUtils
          .findMatchingRecipe(getRecipesCsvTable(), countryCode, entry.getSanitizedNumber())
          .ifPresent(recipe -> migratedResults.add(migrate(entry.getSanitizedNumber(), recipe, entry)));
    }
    Stream<MigrationEntry> untouchedEntries = getMigrationEntries()
        .filter(entry -> !migratableRange.contains(entry));

    if (rangesTable == null) {
      /*
       * MigrationJob's with no rangesTable are based on a custom recipe file. This means there is no
       * concept of invalid migrations so all migrations can just be seen as valid.
       */
      return new MigrationReport(untouchedEntries,
          ImmutableMap.of("Valid", migratedResults.build(), "Invalid", ImmutableList.of()));
    }
    return new MigrationReport(untouchedEntries, verifyMigratedNumbers(migratedResults.build()));
  }

  /**
   * Retrieves all migratable numbers from the numberRange that can be migrated using the given
   * recipeKey and attempts to migrate them with recipes from the recipesTable that belong to the
   * given country code.
   */
  public Optional<MigrationReport> getMigrationReportForRecipe(RangeKey recipeKey) {
    ImmutableMap<Column<?>, Object> recipeRow = getRecipesCsvTable().getRow(recipeKey);
    ImmutableList<MigrationEntry> migratableRange = MigrationUtils
        .getMigratableRangeByRecipe(getRecipesCsvTable(), recipeKey, getMigrationEntries())
        .collect(ImmutableList.toImmutableList());
    ImmutableList.Builder<MigrationResult> migratedResults = ImmutableList.builder();

    if (!recipeRow.get(RecipesTableSchema.COUNTRY_CODE).equals(countryCode)) {
      return Optional.empty();
    }
    migratableRange.forEach(entry -> migratedResults
        .add(migrate(entry.getSanitizedNumber(), recipeRow, entry)));

    Stream<MigrationEntry> untouchedEntries = getMigrationEntries()
        .filter(entry -> !migratableRange.contains(entry));
    if (rangesTable == null) {
      /*
       * MigrationJob's with no rangesTable are based on a custom recipe file. This means there is no
       * concept of invalid migrations so all migrations can just be seen as valid.
       */
      return Optional.of(new MigrationReport(untouchedEntries,
          ImmutableMap.of("Valid", migratedResults.build(), "Invalid", ImmutableList.of())));
    }
    return Optional
        .of(new MigrationReport(untouchedEntries, verifyMigratedNumbers(migratedResults.build())));
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
      MigrationEntry migrationEntry) {
    String oldFormat = (String) recipeRow.get(RecipesTableSchema.OLD_FORMAT);
    String newFormat = (String) recipeRow.get(RecipesTableSchema.NEW_FORMAT);

    Preconditions.checkArgument(RangeSpecification.parse(oldFormat).matches(migratingNumber),
        "value '%s' in column 'Old Format' cannot be represented by its given"
            + " recipe key (Old Prefix + Old Length)", oldFormat);

    DigitSequence migratedVal = getMigratedValue(migratingNumber.toString(), oldFormat, newFormat);

    /*
     * Only recursively migrate when the recipe explicitly states it is not a final migration.
     * Custom recipes have no concept of an Is_Final_Migration column so their value will be seen
     * as null here. In such cases, the tool should not look for another recipe after a migration.
     */
    if (Boolean.FALSE.equals(recipeRow.get(RecipesTableSchema.IS_FINAL_MIGRATION))) {
      ImmutableMap<Column<?>, Object> nextRecipeRow =
          MigrationUtils.findMatchingRecipe(getRecipesCsvTable(), countryCode, migratedVal)
              .orElseThrow(() -> new RuntimeException(
                  "A multiple migration was required for the stale number '" + migrationEntry
                      .getOriginalNumber() + "' but no other recipe could be found after migrating "
                      + "the number into +" + migratedVal));

      return migrate(migratedVal, nextRecipeRow, migrationEntry);
    }
    return MigrationResult.create(migratedVal, migrationEntry);
  }

  /**
   * Converts a stale number into the new migrated format based on the information from the given
   * oldFormat and newFormat values.
   */
  private DigitSequence getMigratedValue(String staleString, String oldFormat, String newFormat) {
    StringBuilder migratedValue = new StringBuilder();
    int newFormatPointer = 0;

    for (int i = 0; i < oldFormat.length(); i++) {
      if (!Character.isDigit(oldFormat.charAt(i))) {
        migratedValue.append(staleString.charAt(i));
      }
    }
    for (int i = 0; i < Math.max(oldFormat.length(), newFormat.length()); i++) {
      if (i < newFormat.length() && i == newFormatPointer
          && Character.isDigit(newFormat.charAt(i))) {
        do {
          migratedValue.insert(newFormatPointer, newFormat.charAt(newFormatPointer++));
        } while (newFormatPointer < newFormat.length()
            && Character.isDigit(newFormat.charAt(newFormatPointer)));
      }
      if (newFormatPointer == i) {
        newFormatPointer++;
      }
    }
    return DigitSequence.of(migratedValue.toString());
  }

  /**
   * Given a list of {@link MigrationResult}'s, returns a map detailing which migrations resulted in
   * valid phone numbers based on the given rangesTable data. The map will contain to entries; an
   * entry with the key 'Valid' with a list of the valid migrations and an entry with the key
   * 'Invalid', with a list of the invalid migrations from the overall list.
   */
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

  /**
   * Represents the results of a migration when calling {@link #getMigrationReportForCountry()}
   * or {@link #getMigrationReportForRecipe(RangeKey)}
   */
  public final class MigrationReport {
    private final ImmutableList<MigrationEntry> untouchedEntries;
    private final ImmutableList<MigrationResult> validMigrations;
    private final ImmutableList<MigrationResult> invalidMigrations;

    private MigrationReport(Stream<MigrationEntry> untouchedEntries,
        ImmutableMap<String, ImmutableList<MigrationResult>> migratedEntries) {
      this.untouchedEntries = untouchedEntries.collect(ImmutableList.toImmutableList());
      this.validMigrations = migratedEntries.get("Valid");
      this.invalidMigrations = migratedEntries.get("Invalid");
    }

    public DigitSequence getCountryCode() {
      return countryCode;
    }

    /**
     * Returns the Migration results which were seen as valid when queried against the rangesTable
     * containing valid number representations for the given countryCode.
     *
     * Note: for customRecipe migrations, there is no concept of invalid migrations so all
     * {@link MigrationEntry}'s that were migrated will be seen as valid.
     */
    public ImmutableList<MigrationResult> getValidMigrations() {
      return validMigrations;
    }

    /**
     * Returns the Migration results which were seen as invalid when queried against the given
     * rangesTable.
     *
     * Note: for customRecipe migrations, there is no concept of invalid migrations so all
     * {@link MigrationEntry}'s that were migrated will be seen as valid.
     */
    public ImmutableList<MigrationResult> getInvalidMigrations() {
      return invalidMigrations;
    }

    /**
     * Returns the Migration entry's that were not migrated but were seen as being already valid
     * when querying against the rangesTable. Custom recipe migrations do not have range tables so
     * this list will be empty when called from such instance.
     */
    public ImmutableList<MigrationEntry> getUntouchedEntries() {
      return untouchedEntries;
    }

    /**
     * Creates a text file of the new number list after a migration has been performed.
     *
     * @param fileName: the given suffix of the new file to be created.
     */
    public String exportToFile(String fileName) throws IOException {
      String newFileLocation = "+" + countryCode + "_Migration_" + fileName;
      FileWriter fw = new FileWriter(newFileLocation);
      fw.write(toString());
      fw.close();
      return newFileLocation;
    }

    /**
     * Returns the content for the given migration. Numbers that were not migrated are added in their original format as
     * well migrated numbers that were seen as being invalid, unless the migration job is set to exportInvalidMigrations.
     * Successfully migrated numbers will be added in their new format.
     */
    public String toString() {
      StringBuilder fileContent = new StringBuilder();
      for (MigrationResult result : validMigrations) {
        fileContent.append("+").append(result.getMigratedNumber()).append("\n");
      }
      for (MigrationEntry entry : untouchedEntries) {
        fileContent.append(entry.getOriginalNumber()).append("\n");
      }
      if (exportInvalidMigrations && invalidMigrations.size() > 0) {
        fileContent.append("\nInvalid migrations due to an issue in either the used internal recipe or the internal +")
                .append(countryCode).append(" valid metadata range:\n");
      }
      for (MigrationResult result : invalidMigrations) {
        String number = exportInvalidMigrations ? "+" + result.getMigratedNumber() :
                result.getMigrationEntry().getOriginalNumber();
        fileContent.append(number).append("\n");
      }
      return fileContent.toString();
    }

    /**
     * Queries the list of numbers that were not migrated and returns numbers from the list which are
     * seen as valid based on the given rangesTable for the given countryCode.
     */
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

    /**
     * Maps all migrated numbers, whether invalid or valid, to the recipe from the recipesTable that
     * was used to migrate them.
     */
    public Multimap<ImmutableMap<Column<?>, Object>, MigrationResult> getAllRecipesUsed() {
      Multimap<ImmutableMap<Column<?>, Object>, MigrationResult> recipeToNumbers = ArrayListMultimap
          .create();

      for (MigrationResult migration : validMigrations) {
        MigrationUtils.findMatchingRecipe(recipesTable, countryCode,
            migration.getMigrationEntry().getSanitizedNumber())
            .ifPresent(recipe -> recipeToNumbers.put(recipe, migration));
      }
      for (MigrationResult migration : invalidMigrations) {
        MigrationUtils.findMatchingRecipe(recipesTable, countryCode,
            migration.getMigrationEntry().getSanitizedNumber())
            .ifPresent(recipe -> recipeToNumbers.put(recipe, migration));
      }
      return recipeToNumbers;
    }

    /** Prints to console the details of the given migration. */
    public void printMetrics() {
      int migratedCount = validMigrations.size() + invalidMigrations.size();
      int totalCount = untouchedEntries.size() + migratedCount;

      System.out.println("\nMetrics:");
      System.out.println("* " + migratedCount + " out of the " + totalCount + " inputted numbers "
          + "were/was migrated");

      if (rangesTable == null) {
        /*
         * MigrationJob's with no rangesTable are based on a custom recipe file. This means there
         * is no concept of invalid/valid migrations so all migrations can just be listed.
         */
        System.out.println("* Migrated numbers:");
        validMigrations.forEach(val -> System.out.println("\t" + val));
        System.out.println("\n* Untouched number(s):");
        untouchedEntries.forEach(val -> System.out.println("\t" + val.getOriginalNumber()));

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
        validUntouchedEntries.forEach(val -> System.out.println("\t" + val.getOriginalNumber()
            + " (untouched)"));

        System.out.println("\n* Invalid migrated number(s):");
        invalidMigrations.forEach(val -> System.out.println("\t" + val));

        System.out.println("\n* Untouched number(s):");
        /* converted into a Set to allow for constant time contains() method. Can only be converted into a set once all its
          numbers have been printed out above because duplicate numbers could have been entered for migration and users
          should still receive all duplicates. */
        ImmutableSet<MigrationEntry> validUntouchedEntriesSet = ImmutableSet.copyOf(validUntouchedEntries);
        untouchedEntries.forEach(val -> {
          if (validUntouchedEntriesSet.contains(val)) {
            System.out.println("\t" + val.getOriginalNumber() + " (already valid)");
          } else {
            System.out.println("\t" + val.getOriginalNumber());
          }
        });
      }
    }
  }
}
