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

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Factory class to instantiate {@link MigrationJob} objects. To create a Migration Job, a BCP-47
 * country code is required as well as a number input (either a single E.164 number or a file
 * path containing one E.164 number per line). There is also the option for specifying a custom
 * recipes file as a third parameter to use for migrations instead of the default recipes file.
 */
public class MigrationFactory {

  private final static String DEFAULT_RECIPES_FILE = "/recipes.csv";
  private final static String METADATA_ZIPFILE ="/metadata.zip";

  /**
   * Creates a new MigrationJob for a given single E.164 number input (e.g. +4477...) and its
   * corresponding BCP-47 country code (e.g. 44 for United Kingdom).
   */
  public static MigrationJob createMigration(String number, String country) throws IOException {
    DigitSequence countryCode = DigitSequence.of(country);
    ImmutableList<MigrationEntry> numberRanges =
        ImmutableList.of(MigrationEntry.create(sanitizeNumberString(number), number));
    CsvTable<RangeKey> recipes = importRecipes(MigrationFactory.class
        .getResourceAsStream(DEFAULT_RECIPES_FILE));

    MetadataZipFileReader metadata = MetadataZipFileReader.of(MigrationFactory.class
        .getResourceAsStream(METADATA_ZIPFILE));
    CsvTable<RangeKey> ranges = metadata.importCsvTable(countryCode)
        .orElseThrow(() -> new RuntimeException(
            "Country code " + countryCode+ " not supported in metadata"));

    return new MigrationJob(numberRanges, countryCode, recipes, ranges);
  }

  /**
   * Returns a MigrationJob instance for a given single E.164 number input, corresponding BCP-47
   * country code (e.g. 1 for Canada), and custom user recipes.csv file.
   */
  public static MigrationJob createCustomRecipeMigration(String number,
      String country,
      Path customRecipesFile)
      throws IOException {
    DigitSequence countryCode = DigitSequence.of(country);
    ImmutableList<MigrationEntry> numberRanges =
        ImmutableList.of(MigrationEntry.create(sanitizeNumberString(number), number));
    CsvTable<RangeKey> recipes = importRecipes(Files.newInputStream(customRecipesFile));

    return new MigrationJob(numberRanges, countryCode, recipes, null);
  }

  /**
   * Returns a MigrationJob instance for a given file path containing one E.164
   * number per line (e.g. +4477..., +4478...) along with the corresponding
   * BCP-47 country code (e.g. 44) that numbers in the file belong to.
   * All numbers in the file should belong to the same region.
   */
  public static MigrationJob createMigration(Path file, String country) throws IOException {
    List<String> numbers = Files.readAllLines(file);
    DigitSequence countryCode = DigitSequence.of(country);
    ImmutableList.Builder<MigrationEntry> numberRanges = ImmutableList.builder();

    numbers.forEach(num -> numberRanges.add(MigrationEntry.create(sanitizeNumberString(num), num)));
    CsvTable<RangeKey> recipes = importRecipes(MigrationFactory.class
        .getResourceAsStream(DEFAULT_RECIPES_FILE));

    MetadataZipFileReader metadata = MetadataZipFileReader.of(MigrationFactory.class
        .getResourceAsStream(METADATA_ZIPFILE));
    CsvTable<RangeKey> ranges = metadata.importCsvTable(countryCode)
        .orElseThrow(() -> new RuntimeException(
            "Country code " + countryCode+ " not supported in metadata"));

    return new MigrationJob(numberRanges.build(), countryCode, recipes, ranges);
  }

  /**
   * Returns a MigrationJob instance for a given file path containing one E.164
   * number per line, corresponding BCP-47 country code, and custom user recipes.csv file.
   */
  public static MigrationJob createCustomRecipeMigration(Path file,
      String country,
      Path customRecipesFile)
      throws IOException {
    List<String> numbers = Files.readAllLines(file);
    DigitSequence countryCode = DigitSequence.of(country);
    ImmutableList.Builder<MigrationEntry> numberRanges = ImmutableList.builder();

    numbers.forEach(num -> numberRanges.add(MigrationEntry.create(sanitizeNumberString(num), num)));
    CsvTable<RangeKey> recipes = importRecipes(Files.newInputStream(customRecipesFile));

    return new MigrationJob(numberRanges.build(), countryCode, recipes, null);
  }

  /**
   * Removes spaces and '+' '(' ')' '-' characters expected in E.164 numbers then returns the
   * {@link DigitSequence} representation of a given number. The method will not remove other
   * letters or special characters from strings to enable error messages in cases where invalid
   * numbers are inputted.
   */
  private static DigitSequence sanitizeNumberString(String number) {
    CharMatcher matcher = CharMatcher.anyOf("-+()").or(CharMatcher.whitespace());
    return DigitSequence.of(matcher.removeFrom(number));
  }

  /**
   * Returns the {@link CsvTable} for a given recipes file path if present.
   */
  private static CsvTable<RangeKey> importRecipes(InputStream recipesFile) throws IOException {
    InputStreamReader reader = new InputStreamReader(recipesFile);
    return CsvTable.importCsv(RecipesTableSchema.SCHEMA, reader);
  }
}
