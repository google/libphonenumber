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
import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import java.util.Collections;
import java.util.Optional;

public class MigrationUtils {

  /**
   * Returns the sub range of numbers within numberRange that can be migrated using the given
   * recipe. This method will not perform migrations and as a result, the validity of migrations
   * using the given recipe cannot be verified.
   *
   * @param recipeKey: the key of the recipe that is being checked
   * @throws IllegalArgumentException if there is no row in the recipesTable with the given
   * recipeKey
   */
  public static RangeTree getMigratableRecipeRange(CsvTable<RangeKey> recipesTable, RangeKey recipeKey,
      RangeTree numberRange) {
    if (!recipesTable.containsRow(recipeKey)) {
      throw new IllegalArgumentException(
          recipeKey + " does not match any recipe row in the given recipes table");
    }
    return recipeKey.asRangeTree().intersect(numberRange);
  }

  /**
   * Returns the sub range of numbers within numberRange that can be migrated using any recipe from
   * the {@link CsvTable} recipesTable that matches the specified BCP-47 region code. This method
   * will not perform migrations and as a result, the validity of migrations using the given
   * recipesTable cannot be verified.
   */
  public static RangeTree getMigratableRegionRange(RangeTable recipesTable, PhoneRegion regionCode,
      RangeTree numberRange) {
    return recipesTable
        .getRanges(RecipesTableSchema.REGION_CODE, regionCode)
        .intersect(numberRange);
  }

  /**
   * Returns the {@link CsvTable} row for the given recipe in a recipes table that can be used to
   * migrate the given {@link RangeSpecification}. The found recipe must also be linked to the given
   * region code to ensure that recipes from incorrect regions are not used to migrated a given
   * number.
   */
  public static Optional<ImmutableMap<Column<?>, Object>> findMatchingRecipe(
      RangeTable recipesTable,
      PhoneRegion regionCode,
      RangeSpecification number) {
    RangeTable subRangeTable = recipesTable.subTable(recipesTable
        .getRanges(RecipesTableSchema.REGION_CODE, regionCode)
        .intersect(RangeTree.from(number)), RecipesTableSchema.RANGE_COLUMNS);

    CsvTable<RangeKey> subCsvTable = RecipesTableSchema.toCsv(subRangeTable);
    if (subCsvTable.isEmpty()) {
      return Optional.empty();
    }
    return Optional
        .of(subCsvTable.getRow(RangeKey.create(number, Collections.singleton(number.length()))));
  }

  /**
   * Returns a set of individual numbers from the map of numbers inputted for migration
   * that can be represented in the given minimal {@link RangeSpecification}. The method expects all
   * RangeSpecification keys from the map to only match a single number.
   */
  public static ImmutableSet<RangeSpecification> getNumbersFromMinimalRange(
      ImmutableMap<RangeSpecification, String> numberRangeMap,
      RangeSpecification minimalRange) {
    ImmutableSet<RangeSpecification> allNumbers = numberRangeMap.keySet();

    return allNumbers.stream()
        .filter(num -> minimalRange
        .matches(DigitSequence.of(num.toString())))
        .collect(ImmutableSet.toImmutableSet());
  }
}
