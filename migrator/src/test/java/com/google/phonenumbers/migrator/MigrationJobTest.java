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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.phonenumbers.migrator.MigrationJob.MigrationReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MigrationJobTest {

  private static final String COUNTRY_CODE = "44";
  private static final String TEST_DATA_PATH = "./src/test/java/com/google/phonenumbers/migrator/testing/testData/";

  @Test
  public void customRecipesMigration_expectMigrations() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
    MigrationJob job = MigrationFactory
        .createCustomRecipeMigration(Paths.get(numbersPath), COUNTRY_CODE, Paths.get(recipesPath));

    MigrationReport report = job.getMigrationReportForCountry();
    assertThat(report.getValidMigrations()).isNotEmpty();
  }

  @Test
  public void customRecipesMigration_noRecipesFromCountry_expectNoMigrations() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
    String unsupportedCountry = "1";
    MigrationJob job = MigrationFactory
        .createCustomRecipeMigration(Paths.get(numbersPath), unsupportedCountry, Paths.get(recipesPath));

    MigrationReport report = job.getMigrationReportForCountry();
    assertThat(report.getValidMigrations()).isEmpty();
  }

  @Test
  public void customRecipes_singleMigration_unsupportedRecipeKey_expectException() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
    MigrationJob job = MigrationFactory
        .createCustomRecipeMigration(Paths.get(numbersPath), COUNTRY_CODE, Paths.get(recipesPath));

    RangeSpecification testRecipePrefix = RangeSpecification.from(DigitSequence.of("123"));
    int testRecipeLength = 3;
    RangeKey invalidKey = RangeKey.create(testRecipePrefix, Collections.singleton(testRecipeLength));

    try {
      job.getMigrationReportForRecipe(invalidKey);
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (IllegalArgumentException e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e).hasMessageThat().contains(invalidKey.toString());
    }
  }

  @Test
  public void customRecipes_singleMigration_validKey_expectMigration() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
    MigrationJob job = MigrationFactory
        .createCustomRecipeMigration(Paths.get(numbersPath), COUNTRY_CODE, Paths.get(recipesPath));

    RangeSpecification testRecipePrefix = RangeSpecification.from(DigitSequence.of("12"));
    int testRecipeLength = 5;
    RangeKey validKey = RangeKey.create(testRecipePrefix, Collections.singleton(testRecipeLength));

    Optional<MigrationReport> migratedNums = job.getMigrationReportForRecipe(validKey);
    assertThat(migratedNums).isPresent();
  }

  @Test
  public void customRecipes_invalidOldFormatValue_expectException() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    MigrationJob job = MigrationFactory
        .createCustomRecipeMigration("13321", COUNTRY_CODE, Paths.get(recipesPath));

    RangeSpecification testRecipePrefix = RangeSpecification.from(DigitSequence.of("13"));
    int testRecipeLength = 5;
    RangeKey recipeKey = RangeKey.create(testRecipePrefix, Collections.singleton(testRecipeLength));

    try {
      job.getMigrationReportForRecipe(recipeKey);
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e).hasMessageThat().contains("Old Format");
    }
  }

  @Test
  public void customRecipe_multipleMigration_nextRecipeNotFound_expectException() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    String staleNumber = "10321";
    MigrationJob job = MigrationFactory
        .createCustomRecipeMigration(staleNumber, COUNTRY_CODE, Paths.get(recipesPath));

    RangeSpecification testRecipePrefix = RangeSpecification.from(DigitSequence.of("10"));
    int testRecipeLength = 5;
    RangeKey recipeKey = RangeKey.create(testRecipePrefix, Collections.singleton(testRecipeLength));

    try {
      job.getMigrationReportForRecipe(recipeKey);
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(RuntimeException.class);
      assertThat(e).hasMessageThat().contains("multiple migration");
      assertThat(e).hasMessageThat().contains(staleNumber);
    }
  }

  @Test
  public void customRecipe_multipleMigration_expectMigration() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    String staleNumber = "15321";
    String migratedNumber = "130211";
    MigrationJob job = MigrationFactory
        .createCustomRecipeMigration(staleNumber, COUNTRY_CODE, Paths.get(recipesPath));

    RangeSpecification testRecipePrefix = RangeSpecification.from(DigitSequence.of("15"));
    int testRecipeLength = 5;
    RangeKey recipeKey = RangeKey.create(testRecipePrefix, Collections.singleton(testRecipeLength));

    MigrationReport migratedNums = job.getMigrationReportForRecipe(recipeKey)
        .orElseThrow(() -> new RuntimeException("Migration was expected but from found"));

    assertThat(migratedNums.getValidMigrations().stream().map(MigrationResult::getMigratedNumber)
        .collect(Collectors.toList()))
        .containsExactly(DigitSequence.of(migratedNumber));
  }

  @Test
  public void standardMigration_invalidNumberNoRecipe_expectNoMigration() throws IOException {
    String invalidNumber = "1234567";
    MigrationJob job = MigrationFactory.createMigration(invalidNumber, COUNTRY_CODE);

    MigrationReport report = job.getMigrationReportForCountry();
    assertThat(report.getValidUntouchedEntries()).isEmpty();
    assertThat(report.getUntouchedEntries().stream().map(MigrationEntry::getOriginalNumber))
        .containsExactly(invalidNumber);
  }

  @Test
  public void standardMigration_numberAlreadyValid_expectNoMigration() throws IOException {
    String alreadyValidNumber = "84701234567";
    String vietnamCode = "84";
    MigrationJob job = MigrationFactory.createMigration(alreadyValidNumber, vietnamCode);

    MigrationReport report = job.getMigrationReportForCountry();
    assertThat(report.getValidMigrations()).isEmpty();
    assertThat(report.getValidUntouchedEntries().stream().map(MigrationEntry::getOriginalNumber))
        .containsExactly(alreadyValidNumber);
  }

  @Test
  public void standardMigration_migratableNumber_expectMigration() throws IOException {
    String alreadyValidNumber = "841201234567";
    String vietnamCode = "84";
    MigrationJob job = MigrationFactory.createMigration(alreadyValidNumber, vietnamCode);

    MigrationReport report = job.getMigrationReportForCountry();
    assertThat(report.getValidMigrations().stream().map(res -> res.getMigrationEntry().getOriginalNumber()))
        .containsExactly(alreadyValidNumber);
  }

  @Test
  public void standardMigration_invalidMigration_expectInvalidMigration() throws IOException {
    Path recipesPath = Paths.get(TEST_DATA_PATH + "testRecipesFile.csv");
    DigitSequence migratingNumber = DigitSequence.of("12345");

    ImmutableList<MigrationEntry> numberRanges = ImmutableList
        .of(MigrationEntry.create(migratingNumber, migratingNumber.toString()));
    CsvTable<RangeKey> recipes = MigrationFactory
        .importRecipes(Files.newInputStream(recipesPath));

    MetadataZipFileReader metadata = MetadataZipFileReader.of(MigrationFactory.class
        .getResourceAsStream(MigrationFactory.METADATA_ZIPFILE));
    CsvTable<RangeKey> ranges = metadata.importCsvTable(DigitSequence.of(COUNTRY_CODE))
        .orElseThrow(RuntimeException::new);

    MigrationJob job =
        new MigrationJob(numberRanges, DigitSequence.of(COUNTRY_CODE), recipes, ranges, false);

    MigrationReport report = job.getMigrationReportForCountry();
    assertThat(report.getValidMigrations()).isEmpty();
    assertThat(report.getInvalidMigrations().stream()
        .map(result -> result.getMigrationEntry().getOriginalNumber()))
        .containsExactly(migratingNumber.toString());
  }
}
