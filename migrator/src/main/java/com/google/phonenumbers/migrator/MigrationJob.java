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
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.util.Collection;

/**
 * Represents a migration operation for a given region where each {@link MigrationJob} contains
 * a map of E.164 numbers to be migrated as well as the {@link CsvTable} which will
 * hold the available recipes that can be performed on the range. The number range map is a key value
 * pair of the E.164 {@link RangeSpecification} representation of a number along with the raw input
 * String originally entered. Only recipes from the given two digit BCP-47 regionCode will be used.
 */
public final class MigrationJob {

  private final CsvTable<RangeKey> recipesTable;
  private final ImmutableMap<RangeSpecification, String> numberRangeMap;
  private final PhoneRegion regionCode;

  MigrationJob(ImmutableMap<RangeSpecification,
      String> numberRangeMap,
      PhoneRegion regionCode,
      CsvTable<RangeKey> recipesTable) {
    this.numberRangeMap = numberRangeMap;
    this.regionCode = regionCode;
    this.recipesTable = recipesTable;
  }

  public CsvTable<RangeKey> getRecipesTable() {
    return recipesTable;
  }

  public ImmutableMap<RangeSpecification, String> getNumberRangeMap() {
    return numberRangeMap;
  }

  /**
   * Returns the formatted version of the number range for migration
   */
  public RangeTree getNumberRange() {
    return RangeTree.from(numberRangeMap.keySet());
  }

  /**
   * Returns a list of the raw number range for migration
   */
  public Collection<String> getRawNumberRange() {
    return numberRangeMap.values();
  }

  public PhoneRegion getRegionCode() {
    return regionCode;
  }

  /**
   * Returns the sub range of numbers within numberRange that can be migrated using any recipe from
   * the {@link CsvTable} recipesTable that matches the specified BCP-47 region code. This method will
   * not perform migrations and as a result, the validity of migrations using the given recipesTable
   * cannot be verified.
   */
  public RangeTree getAllMigratableNumbers() {
    return RecipesTableSchema.toRangeTable(recipesTable)
        .getRanges(RecipesTableSchema.REGION_CODE, regionCode)
        .intersect(getNumberRange());
  }

  /**
   * Returns the sub range of numbers within numberRange that can be migrated using the given recipe.
   * This method will not perform migrations and as a result, the validity of migrations using the
   * given recipe cannot be verified.
   *
   * @param recipeKey: the key of the recipe that is being checked
   * @throws IllegalArgumentException if there is no row in the recipesTable with the given recipeKey
   */
  public RangeTree getMigratableNumbers(RangeKey recipeKey) {
    if (!recipesTable.containsRow(recipeKey)) {
      throw new IllegalArgumentException(
          recipeKey + " does not match any recipe row in the given recipes table");
    }
    return recipeKey.asRangeTree().intersect(getNumberRange());
  }
}
