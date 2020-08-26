// /*
//  * Copyright (C) 2020 The Libphonenumber Authors.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  * http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
// package com.google.phonenumbers.migrator;
//
// import static com.google.common.truth.Truth.assertThat;
//
// import com.google.common.collect.ImmutableList;
// import com.google.i18n.phonenumbers.metadata.DigitSequence;
// import com.google.i18n.phonenumbers.metadata.RangeSpecification;
// import com.google.i18n.phonenumbers.metadata.table.RangeKey;
// import java.io.IOException;
// import java.nio.file.Paths;
// import java.util.Collections;
// import java.util.stream.Collectors;
// import org.junit.Assert;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.JUnit4;
//
// @RunWith(JUnit4.class)
// public class MigrationJobTest {
//
//   private static final String TEST_DATA_PATH = "./src/test/java/com/google/phonenumbers/migrator/testing/testData/";
//
//   @Test
//   public void performAllMigrations_expectMigrations() throws IOException {
//     String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
//     String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
//     MigrationJob job = MigrationFactory
//         .createMigration(Paths.get(numbersPath), "44", Paths.get(recipesPath));
//
//     ImmutableList<MigrationResult> migratedNums = job.performAllMigrations();
//     assertThat(migratedNums).isNotEmpty();
//   }
//
//   @Test
//   public void performAllMigrations_noRecipesFromCountry_expectNoMigrations() throws IOException {
//     String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
//     String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
//     MigrationJob job = MigrationFactory
//         .createMigration(Paths.get(numbersPath), "1", Paths.get(recipesPath));
//
//     ImmutableList<MigrationResult> migratedNums = job.performAllMigrations();
//     assertThat(migratedNums).isEmpty();
//   }
//
//   @Test
//   public void performSingleMigration_unsupportedRecipeKey_expectException() throws IOException {
//     String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
//     String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
//     MigrationJob job = MigrationFactory
//         .createMigration(Paths.get(numbersPath), "1", Paths.get(recipesPath));
//
//     RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("123"));
//     RangeKey invalidKey = RangeKey.create(testRangeSpec, Collections.singleton(3));
//
//     try {
//       job.performSingleRecipeMigration(invalidKey);
//       Assert.fail("Expected RuntimeException and did not receive");
//     } catch (IllegalArgumentException e) {
//       assertThat(e).isInstanceOf(IllegalArgumentException.class);
//       assertThat(e).hasMessageThat().contains(invalidKey.toString());
//     }
//   }
//
//   @Test
//   public void performSingleMigration_validKey_expectMigration() throws IOException {
//     String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
//     String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
//     MigrationJob job = MigrationFactory
//         .createMigration(Paths.get(numbersPath), "44", Paths.get(recipesPath));
//
//     RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("12"));
//     RangeKey validKey = RangeKey.create(testRangeSpec, Collections.singleton(5));
//
//     ImmutableList<MigrationResult> migratedNums = job.performSingleRecipeMigration(validKey);
//     assertThat(migratedNums).isNotEmpty();
//   }
//
//   @Test
//   public void performMigration_invalidOldFormatValue_expectException() throws IOException {
//     String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
//     MigrationJob job = MigrationFactory
//         .createMigration("13321", "44", Paths.get(recipesPath));
//
//     RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("13"));
//     RangeKey recipeKey = RangeKey.create(testRangeSpec, Collections.singleton(5));
//
//     try {
//       job.performSingleRecipeMigration(recipeKey);
//       Assert.fail("Expected RuntimeException and did not receive");
//     } catch (RuntimeException e) {
//       assertThat(e).isInstanceOf(IllegalArgumentException.class);
//       assertThat(e).hasMessageThat().contains("Old Format");
//     }
//   }
//
//   @Test
//   public void performMultipleMigration_nextRecipeNotFound_expectException() throws IOException {
//     String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
//     String staleNumber = "10321";
//     MigrationJob job = MigrationFactory
//         .createMigration(staleNumber, "44", Paths.get(recipesPath));
//
//     RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("10"));
//     RangeKey recipeKey = RangeKey.create(testRangeSpec, Collections.singleton(5));
//
//     try {
//       job.performSingleRecipeMigration(recipeKey);
//       Assert.fail("Expected RuntimeException and did not receive");
//     } catch (RuntimeException e) {
//       assertThat(e).isInstanceOf(RuntimeException.class);
//       assertThat(e).hasMessageThat().contains("multiple migration");
//       assertThat(e).hasMessageThat().contains(staleNumber);
//     }
//   }
//
//   @Test
//   public void performMultipleMigration_expectMigration() throws IOException {
//     String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
//     String staleNumber = "15321";
//     MigrationJob job = MigrationFactory
//         .createMigration(staleNumber, "44", Paths.get(recipesPath));
//
//     RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("15"));
//     RangeKey recipeKey = RangeKey.create(testRangeSpec, Collections.singleton(5));
//
//     ImmutableList<MigrationResult> migratedNums = job.performSingleRecipeMigration(recipeKey);
//     assertThat(migratedNums.stream().map(MigrationResult::getMigratedNumber)
//         .collect(Collectors.toList()))
//         .containsExactly(DigitSequence.of("130211"));
//   }
// }
