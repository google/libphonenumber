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
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
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
import java.util.stream.Stream;

/**
 * Utilities for migration tool. Functionality includes lookup of migratable E.164 numbers in a
 * given range based on a given BCP-47 region code, lookup of migratable E.164 numbers in a range
 * based on a specified recipe, and also lookup for the corresponding recipe in a given recipes
 * table which can be used to migrate a given E.164 number.
 */
public final class MigrationUtils {

  /**
   * Returns the sub range of numbers within numberRange that can be migrated using the given
   * recipe. This method will not perform migrations and as a result, the validity of migrations
   * using the given recipe cannot be verified.
   *
   * @param recipeKey: the key of the recipe that is being checked
   * @throws IllegalArgumentException if there is no row in the recipesTable with the given
   * recipeKey
   */
  public static Stream<DigitSequence> getMigratableRecipeRange(CsvTable<RangeKey> recipesTable,
      RangeKey recipeKey,
      RangeSet<DigitSequence> numberRange) {
    if (!recipesTable.containsRow(recipeKey)) {
      throw new IllegalArgumentException(
          recipeKey + " does not match any recipe row in the given recipes table");
    }
    return recipeKey.asRangeTree().asRangeSet().intersection(numberRange).asRanges().stream()
        .map(Range::lowerEndpoint);
  }

  /**
   * Returns the sub range of numbers within numberRange that can be migrated using any recipe from
   * the {@link CsvTable} recipesTable that matches the specified BCP-47 region code. This method
   * will not perform migrations and as a result, the validity of migrations using the given
   * recipesTable cannot be verified.
   */
  public static Stream<DigitSequence> getMigratableRegionRange(RangeTable recipesTable,
      PhoneRegion regionCode,
      RangeSet<DigitSequence> numberRange) {

    return recipesTable
        .getRanges(RecipesTableSchema.REGION_CODE, regionCode).asRangeSet()
        .intersection(numberRange).asRanges().stream().map(Range::lowerEndpoint);
  }

  /**
   * Returns the {@link CsvTable} row for the given recipe in a recipes table that can be used to
   * migrate the given {@link DigitSequence}. The found recipe must also be linked to the given
   * region code to ensure that recipes from incorrect regions are not used to migrated a given
   * number.
   */
  public static Optional<ImmutableMap<Column<?>, Object>> findMatchingRecipe(
      RangeTable recipesTable,
      PhoneRegion regionCode,
      DigitSequence number) {

    RangeTable subRangeTable = recipesTable.subTable(recipesTable
        .getRanges(RecipesTableSchema.REGION_CODE, regionCode)
        .intersect(RangeTree.from(RangeSpecification.from(number))), RecipesTableSchema.RANGE_COLUMNS);

    CsvTable<RangeKey> subCsvTable = RecipesTableSchema.toCsv(subRangeTable);
    if (subCsvTable.isEmpty()) {
      return Optional.empty();
    }
    return Optional
        .of(subCsvTable.getRow(RangeKey.create(RangeSpecification.from(number), Collections.singleton(number.length()))));
  }
}
