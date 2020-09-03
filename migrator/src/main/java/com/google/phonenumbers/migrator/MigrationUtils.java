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

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import java.util.Optional;
import java.util.stream.Stream;

/** Utilities for migration tool. */
public final class MigrationUtils {

  /**
   * Returns the entries within migrationEntries that can be migrated using the given recipe. This
   * method will not perform migrations and as a result, the validity of migrations using the given
   * recipe cannot be verified.
   *
   * @param recipeKey: the key of the recipe that is being checked
   * @throws IllegalArgumentException if there is no row in the recipesTable with the given
   * recipeKey
   */
  public static Stream<MigrationEntry> getMigratableRangeByRecipe(CsvTable<RangeKey> recipesTable,
      RangeKey recipeKey,
      Stream<MigrationEntry> migrationEntries) {
    if (!recipesTable.containsRow(recipeKey)) {
      throw new IllegalArgumentException(
          recipeKey + " does not match any recipe row in the given recipes table");
    }

    return migrationEntries
        .filter(entry -> recipeKey.asRangeTree().contains(entry.getSanitizedNumber()));
  }

  /**
   * Returns the sub range of entries within migrationEntries that can be migrated using any recipe
   * from the {@link CsvTable} recipesTable that matches the specified BCP-47 country code. This
   * method will not perform migrations and as a result, the validity of migrations using the given
   * recipesTable cannot be verified.
   */
  public static Stream<MigrationEntry> getMigratableRangeByCountry(RangeTable recipesTable,
      DigitSequence countryCode,
      Stream<MigrationEntry> migrationEntries) {

    RangeTree countryRecipes = recipesTable
        .getRanges(RecipesTableSchema.COUNTRY_CODE, countryCode);

    return migrationEntries
        .filter(entry -> countryRecipes.contains(entry.getSanitizedNumber()));
  }

  /**
   * Returns the {@link CsvTable} row for the given recipe in a recipes table that can be used to
   * migrate the given {@link DigitSequence}. The found recipe must also be linked to the given
   * country code to ensure that recipes from incorrect countries are not used to migrated a given
   * number.
   */
  public static Optional<ImmutableMap<Column<?>, Object>> findMatchingRecipe(
      CsvTable<RangeKey> recipesTable,
      DigitSequence countryCode,
      DigitSequence number) {

    for (RangeKey recipeKey : recipesTable.getKeys()) {
      if (recipeKey.contains(number, number.length()) && recipesTable.getRow(recipeKey)
          .get(RecipesTableSchema.COUNTRY_CODE).equals(countryCode)) {
        return Optional.of(recipesTable.getRow(recipeKey));
      }
    }
    return Optional.empty();
  }
}
