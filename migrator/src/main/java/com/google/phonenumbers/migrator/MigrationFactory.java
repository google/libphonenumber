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
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Factory class to instantiate {@link MigrationJob} objects. To create a Migration Job, a two digit
 * BCP-47 region code is required as well as a number input (either a single E.164 number or a file
 * path containing one E.164 number per line). There is also the option for specifying a custom
 * recipes file as a third parameter to use for migrations instead of the default recipes file.
 */
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
    ImmutableMap.Builder<RangeSpecification, String> numberRanges = ImmutableMap.builder();

    numbers.forEach(num -> numberRanges.put(sanitizeNumberString(num), num));
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(DEFAULT_RECIPES_FILE));

    return new MigrationJob(numberRanges.build(), regionCode, recipes);
  }

  /**
   * Returns a MigrationJob instance for a given file path containing one E.164
   * number per line, corresponding BCP-47 region code, and custom user recipes.csv file.
   */
  public static MigrationJob createMigration(Path file, String region, Path customRecipesFile)
      throws IOException {
    List<String> numbers = Files.readAllLines(file);
    PhoneRegion regionCode = PhoneRegion.of(region);
    ImmutableMap.Builder<RangeSpecification, String> numberRanges = ImmutableMap.builder();

    numbers.forEach(num -> numberRanges.put(sanitizeNumberString(num), num));
    CsvTable<RangeKey> recipes = importRecipes(customRecipesFile);

    return new MigrationJob(numberRanges.build(), regionCode, recipes);
  }

  /**
   * Removes spaces and '+' '(' ')' '-' characters expected in E.164 numbers then returns the
   * {@link RangeSpecification} representation of a given number. The method will not remove other
   * letters or special characters from strings to enable error messages in cases where invalid
   * numbers are inputted.
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
