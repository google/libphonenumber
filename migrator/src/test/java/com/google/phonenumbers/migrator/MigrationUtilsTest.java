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

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MigrationUtilsTest {

  private static final String TEST_DATA_PATH = "./src/test/java/com/google/phonenumbers/migrator/testing/testData/";
  private static final Path RECIPES_PATH = Paths.get(TEST_DATA_PATH + "testRecipesFile.csv");
  private static final String COUNTRY_CODE = "44";
  private static final String VALID_TEST_NUMBER = "123";

  @Test
  public void getCountryMigratableNumbers_expectNoMatches() throws IOException {
    String invalidTestNumber = "34";
    MigrationJob job = MigrationFactory.createCustomRecipeMigration(invalidTestNumber, COUNTRY_CODE, MigrationFactory
            .importRecipes(Files.newInputStream(RECIPES_PATH)));

    Stream<MigrationEntry> noMatchesRange = MigrationUtils
        .getMigratableRangeByCountry(job.getRecipesRangeTable(), job.getCountryCode(),
            job.getMigrationEntries());
    assertThat(noMatchesRange.collect(Collectors.toSet())).isEmpty();
  }

  @Test
  public void getCountryMigratableNumbers_expectMatches() throws IOException {
    String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
    MigrationJob job = MigrationFactory
        .createCustomRecipeMigration(Paths.get(numbersPath), COUNTRY_CODE, MigrationFactory
                .importRecipes(Files.newInputStream(RECIPES_PATH)));

    Stream<MigrationEntry> matchesRange = MigrationUtils
        .getMigratableRangeByCountry(job.getRecipesRangeTable(), job.getCountryCode(),
            job.getMigrationEntries());

    assertThat(matchesRange.collect(Collectors.toList()))
        .containsExactlyElementsIn(job.getMigrationEntries().collect(Collectors.toList()));
  }

  @Test
  public void getMigratableNumbers_invalidKey_expectException() throws IOException {
    RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of(VALID_TEST_NUMBER));
    RangeKey invalidKey = RangeKey.create(testRangeSpec, Collections.singleton(3));

    MigrationJob job = MigrationFactory.createCustomRecipeMigration(VALID_TEST_NUMBER, COUNTRY_CODE, MigrationFactory
            .importRecipes(Files.newInputStream(RECIPES_PATH)));
    try {
      MigrationUtils
          .getMigratableRangeByRecipe(job.getRecipesCsvTable(), invalidKey, job.getMigrationEntries());
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e).hasMessageThat().contains(invalidKey.toString());
    }
  }

  @Test
  public void getMigratableNumbers_validKey_expectNoExceptionAndNoMatches() throws IOException {
    RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("12"));
    RangeKey validKey = RangeKey.create(testRangeSpec, Collections.singleton(5));

    MigrationJob job = MigrationFactory.createCustomRecipeMigration(VALID_TEST_NUMBER, COUNTRY_CODE, MigrationFactory
            .importRecipes(Files.newInputStream(RECIPES_PATH)));

    assertThat(MigrationUtils
        .getMigratableRangeByRecipe(job.getRecipesCsvTable(), validKey, job.getMigrationEntries())
        .collect(Collectors.toSet()))
        .isEmpty();
  }

  @Test
  public void findMatchingRecipe_expectNoMatchingRecipe() throws IOException {
    MigrationJob job = MigrationFactory.createCustomRecipeMigration(VALID_TEST_NUMBER, COUNTRY_CODE, MigrationFactory
            .importRecipes(Files.newInputStream(RECIPES_PATH)));

    DigitSequence testNumberToMatch = DigitSequence.of("12");
    assertThat(MigrationUtils
        .findMatchingRecipe(job.getRecipesCsvTable(), job.getCountryCode(), testNumberToMatch))
        .isEmpty();
  }

  @Test
  public void findMatchingRecipe_expectMatchingRecipe() throws IOException {
    MigrationJob job = MigrationFactory.createCustomRecipeMigration(VALID_TEST_NUMBER, COUNTRY_CODE, MigrationFactory
            .importRecipes(Files.newInputStream(RECIPES_PATH)));
    DigitSequence testNumberToMatch = DigitSequence.of("12345");

    Optional<ImmutableMap<Column<?>, Object>> foundRecipe = MigrationUtils
        .findMatchingRecipe(job.getRecipesCsvTable(), job.getCountryCode(), testNumberToMatch);
    assertThat(foundRecipe).isPresent();

    RangeSpecification oldFormat = RangeSpecification
        .parse((String) foundRecipe.get().get(RecipesTableSchema.OLD_FORMAT));
    assertThat(oldFormat.matches(testNumberToMatch)).isTrue();
  }
}
