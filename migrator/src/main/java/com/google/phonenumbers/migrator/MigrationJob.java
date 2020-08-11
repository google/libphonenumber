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
 * a {@link RangeTree} of numbers to be migrated as well as the {@link CsvTable} which will hold the
 * available recipes that can be performed on the range. Only recipes from the given regionCode will
 * be used.
 */
public final class MigrationJob {

  private final static String DEFAULT_RECIPES_PATH = "../recipes.csv";

  private final CsvTable<RangeKey> recipesTable;
  private final RangeTree numberRange;
  private final PhoneRegion regionCode;

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

  /**
   * Removes spaces and '+' characters expected in E.164 numbers and returns the
   * {@link RangeSpecification} representation of a given number. The method will not remove other
   * letters or special characters from strings to enable users to receive error messages in
   * cases where invalid numbers are inputted.
   */
  private static RangeSpecification sanitizeNumberString(String number) {
    String sanitizedString = number.replaceAll("[+]|[\\s]", "");
    return RangeSpecification.parse(sanitizedString);
  }

  /**
   * Returns the {@link CsvTable} for a given recipes file path if present.
   */
  private static CsvTable<RangeKey> importRecipes(Path recipesPath) throws IOException {
    InputStreamReader reader = new InputStreamReader(Files.newInputStream(recipesPath));
    return CsvTable.importCsv(RecipesTableSchema.SCHEMA, reader);
  }

  /**
   * Returns a MigrationJob instance for a given single E.164 number input (e.g. +4477...) and its
   * corresponding BCP-47 region code (e.g. GB).
   */
  public static MigrationJob from(String numberInput, String regionCodeInput) throws IOException {
    PhoneRegion regionCode = PhoneRegion.of(regionCodeInput);
    RangeTree numberRanges = RangeTree.from(sanitizeNumberString(numberInput));
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(DEFAULT_RECIPES_PATH));

    return new MigrationJob(numberRanges, regionCode, recipes);
  }

  /**
   * Returns a MigrationJob instance for a given single E.164 number input, corresponding BCP-47
   * region code (e.g. GB), and custom user recipes.csv file.
   */
  public static MigrationJob from(String numberInput, String regionCodeInput, String customRecipesPath)
      throws IOException {
    PhoneRegion regionCode = PhoneRegion.of(regionCodeInput);
    RangeTree numberRanges = RangeTree.from(sanitizeNumberString(numberInput));
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(customRecipesPath));

    return new MigrationJob(numberRanges, regionCode, recipes);
  }

  /**
   * Returns a MigrationJob instance for a given file path containing comma separated E.164 numbers
   * (e.g. +4477..., +4478...) along with the corresponding BCP-47 region code (e.g. GB) that
   * numbers in the file belong to. All numbers in the file should belong to the same region.
   */
  public static MigrationJob from(Path fileInput, String regionCodeInput) throws IOException {
    Scanner scanner = new Scanner(new FileReader(fileInput.toString()));
    List<String> numbers = new ArrayList<>();
    while (scanner.hasNext()) {
      numbers.addAll(Arrays.asList(scanner.nextLine().split(",")));
    }

    PhoneRegion regionCode = PhoneRegion.of(regionCodeInput);
    RangeTree numberRanges = RangeTree.from(numbers.stream().map(MigrationJob::sanitizeNumberString));
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(DEFAULT_RECIPES_PATH));

    return new MigrationJob(numberRanges, regionCode, recipes);
  }

  /**
   * Returns a MigrationJob instance for a given file path containing comma separated E.164 numbers,
   * corresponding BCP-47 region code, and custom user recipes.csv file.
   */
  public static MigrationJob from(Path fileInput, String regionCodeInput, String customRecipesPath)
      throws IOException {
    Scanner scanner = new Scanner(new FileReader(fileInput.toString()));
    List<String> numbers = new ArrayList<>();
    while (scanner.hasNext()) {
      numbers.addAll(Arrays.asList(scanner.nextLine().split(",")));
    }

    PhoneRegion regionCode = PhoneRegion.of(regionCodeInput);
    RangeTree numberRanges = RangeTree.from(numbers.stream().map(MigrationJob::sanitizeNumberString));
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(customRecipesPath));

    return new MigrationJob(numberRanges, regionCode, recipes);
  }

  private MigrationJob(RangeTree numberRange, PhoneRegion regionCode, CsvTable<RangeKey> recipesTable) {
    this.numberRange = numberRange;
    this.regionCode = regionCode;
    this.recipesTable = recipesTable;
  }
}
