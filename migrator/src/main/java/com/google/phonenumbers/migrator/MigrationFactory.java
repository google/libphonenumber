package com.google.phonenumbers.migrator;

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MigrationFactory {

  private final static String DEFAULT_RECIPES_FILE = "../recipes.csv";

  /**
   * Returns a MigrationJob instance for a given single E.164 number input (e.g. +4477...) and its
   * corresponding BCP-47 region code (e.g. GB).
   */
  public static MigrationJob createMigration(String number, String region) throws IOException {
    PhoneRegion regionCode = PhoneRegion.of(region);
    ImmutableMap<RangeSpecification, String> numberRanges =
        ImmutableMap.of(sanitizeNumberString(number), number);
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(DEFAULT_RECIPES_FILE));

    return new MigrationJob(numberRanges, regionCode, recipes);
  }

  /**
   * Returns a MigrationJob instance for a given single E.164 number input, corresponding BCP-47
   * region code (e.g. GB), and custom user recipes.csv file.
   */
  public static MigrationJob createMigration(String number, String region, Path customRecipesFile)
      throws IOException {
    PhoneRegion regionCode = PhoneRegion.of(region);
    ImmutableMap<RangeSpecification, String> numberRanges =
        ImmutableMap.of(sanitizeNumberString(number), number);
    CsvTable<RangeKey> recipes = importRecipes(customRecipesFile);

    return new MigrationJob(numberRanges, regionCode, recipes);
  }

  /**
   * Returns a MigrationJob instance for a given file path containing one E.164
   * number per line (e.g. +4477..., +4478...) along with the corresponding
   * BCP-47 region code (e.g. GB) that numbers in the file belong to.
   * All numbers in the file should belong to the same region.
   */
  public static MigrationJob createMigration(Path file, String region) throws IOException {
    List<String> numbers = Files.readAllLines(file);
    PhoneRegion regionCode = PhoneRegion.of(region);
    Map<RangeSpecification, String> numberRanges = new HashMap<>();

    numbers.forEach(num -> numberRanges.put(sanitizeNumberString(num), num));
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(DEFAULT_RECIPES_FILE));

    return new MigrationJob(ImmutableMap.copyOf(numberRanges), regionCode, recipes);
  }

  /**
   * Returns a MigrationJob instance for a given file path containing one E.164
   * number per line, corresponding BCP-47 region code, and custom user recipes.csv file.
   */
  public static MigrationJob createMigration(Path file, String region, Path customRecipesFile)
      throws IOException {
    List<String> numbers = Files.readAllLines(file);
    PhoneRegion regionCode = PhoneRegion.of(region);
    Map<RangeSpecification, String> numberRanges = new HashMap<>();

    numbers.forEach(num -> numberRanges.put(sanitizeNumberString(num), num));
    CsvTable<RangeKey> recipes = importRecipes(customRecipesFile);

    return new MigrationJob(ImmutableMap.copyOf(numberRanges), regionCode, recipes);
  }

  /**
   * Removes spaces and '+' '(' ')' '-' characters expected in E.164 numbers then returns the
   * {@link RangeSpecification} representation of a given number. The method will not remove other
   * letters or special characters from strings to enable users to receive error messages in
   * cases where invalid numbers are inputted.
   */
  private static RangeSpecification sanitizeNumberString(String number) {
    String sanitizedString = number.replaceAll("[+]|[\\s]|[(]|[)]|[-]", "");
    return RangeSpecification.parse(sanitizedString);
  }

  /**
   * Returns the {@link CsvTable} for a given recipes file path if present.
   */
  private static CsvTable<RangeKey> importRecipes(Path recipesFile) throws IOException {
    InputStreamReader reader = new InputStreamReader(Files.newInputStream(recipesFile));
    return CsvTable.importCsv(RecipesTableSchema.SCHEMA, reader);
  }
}
