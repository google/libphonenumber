package com.google.phonenumbers.migrator;

import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Represents a migration operation for a given region where each {@link MigrationJob} contains
 * a {@link RangeTree} of E.164 numbers to be migrated as well as the {@link CsvTable} which will
 * hold the available recipes that can be performed on the range. Only recipes from the given
 * two digit BCP-47 regionCode will be used.
 */
public final class MigrationJob {

  private final CsvTable<RangeKey> recipesTable;
  private final RangeTree numberRange;
  private final PhoneRegion regionCode;

  public MigrationJob(RangeTree numberRange, PhoneRegion regionCode, CsvTable<RangeKey> recipesTable) {
    this.numberRange = numberRange;
    this.regionCode = regionCode;
    this.recipesTable = recipesTable;
  }

  public CsvTable<RangeKey> getRecipesTable() {
    return recipesTable;
  }

  public RangeTree getNumberRange() {
    return numberRange;
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
        .intersect(numberRange);
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
    return recipeKey.asRangeTree().intersect(numberRange);
  }




}
