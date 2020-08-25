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
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Represents a migration operation for a given region where each {@link MigrationJob} contains
 * a list of {@link MigrationEntry}'s to be migrated as well as the {@link CsvTable} which will
 * hold the available recipes that can be performed on the range. Each MigrationEntry has
 * the E.164 {@link DigitSequence} representation of a number along with the raw input
 * String originally entered. Only recipes from the given two digit BCP-47 regionCode will be used.
 */
public final class MigrationJob {

  private final CsvTable<RangeKey> recipesTable;
  private final ImmutableList<MigrationEntry> migrationEntries;
  private final PhoneRegion regionCode;

  MigrationJob(ImmutableList<MigrationEntry> migrationEntries,
      PhoneRegion regionCode,
      CsvTable<RangeKey> recipesTable) {
    this.migrationEntries = migrationEntries;
    this.regionCode = regionCode;
    this.recipesTable = recipesTable;
  }

  public PhoneRegion getRegionCode() {
    return regionCode;
  }

  public CsvTable<RangeKey> getRecipesCsvTable() {
    return recipesTable;
  }

  public RangeTable getRecipesRangeTable() {
    return RecipesTableSchema.toRangeTable(recipesTable);
  }

  public ImmutableList<MigrationEntry> getMigrationEntries() {
    return migrationEntries;
  }

  /**
   * Retrieves all migratable numbers from the numberRange and attempts to migrate them with recipes
   * from the recipesTable that belong to the given region code.
   */
  public ImmutableList<MigrationResult> performAllMigrations() {
    Stream<MigrationEntry> migratableRange = MigrationUtils
        .getMigratableRangeByRegion(getRecipesRangeTable(), regionCode, getMigrationEntries());

    ImmutableList.Builder<MigrationResult> migratedResults = ImmutableList.builder();
    migratableRange.forEach(entry -> {
      ImmutableMap<Column<?>, Object> matchingRecipe = MigrationUtils
          .findMatchingRecipe(getRecipesCsvTable(), regionCode, entry.getSanitizedNumber())
          // can never be thrown here because every staleNumber is from the migratableNumbers set
          .orElseThrow(RuntimeException::new);

       migratedResults.add(migrate(entry.getSanitizedNumber(), matchingRecipe, entry.getOriginalNumber()));
    });

    // TODO: create MigrationReport class holding details of migration and return it here instead
    return migratedResults.build();
  }

  /**
   * Retrieves all migratable numbers from the numberRange that can be migrated using the given
   * recipeKey and attempts to migrate them with recipes from the recipesTable that belong to the
   * given region code.
   */
  public ImmutableList<MigrationResult> performSingleRecipeMigration(RangeKey recipeKey) {
    Stream<MigrationEntry> migratableRange = MigrationUtils
        .getMigratableRangeByRecipe(getRecipesCsvTable(), recipeKey, getMigrationEntries());
    ImmutableMap<Column<?>, Object> recipeRow = getRecipesCsvTable().getRow(recipeKey);

    ImmutableList.Builder<MigrationResult> migratedResults = ImmutableList.builder();
    if (!recipeRow.get(RecipesTableSchema.REGION_CODE).equals(regionCode)) {
      return migratedResults.build();
    }
    migratableRange.forEach(entry -> {
        migratedResults.add(migrate(entry.getSanitizedNumber(), recipeRow, entry.getOriginalNumber()));
    });

    // TODO: create MigrationReport class holding details of migration and return it here instead
    return migratedResults.build();
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
          MigrationUtils.findMatchingRecipe(getRecipesCsvTable(), regionCode,
              DigitSequence.of(newString.toString()))
              .orElseThrow(() -> new RuntimeException(
                  "A multiple migration was required for the stale number '" + originalNumber
                      + "' but no other recipe could be found after migrating the number into +"
                      + newString));

      return migrate(DigitSequence.of(newString.toString()), nextRecipeRow, originalNumber);
    }

    return MigrationResult.create(DigitSequence.of(newString.toString()), originalNumber);
  }
}
