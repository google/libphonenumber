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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MigrationFactoryTest {

  private static final String TEST_DATA_PATH = "./src/test/java/com/google/phonenumbers/migrator/testing/testData/";
  private static final Path RECIPES_PATH = Paths.get(TEST_DATA_PATH + "testRecipesFile.csv");

  @Test
  public void createFromFilePath_invalidPathLocation_expectException() {
    String fileLocation = "invalid-path-location";
    try {
      MigrationFactory.createMigration(Paths.get(fileLocation), "44", false);
      Assert.fail("Expected IOException and did not receive");
    } catch (IOException e) {
      assertThat(e).isInstanceOf(NoSuchFileException.class);
      assertThat(e).hasMessageThat().contains(fileLocation);
    }
  }

  @Test
  public void createFromFilePath_validPathLocation_expectValidFields() throws IOException {
    Path fileLocation = Paths.get(TEST_DATA_PATH + "testNumbersFile.txt");
    String countryCode = "44";
    MigrationJob mj = MigrationFactory.createCustomRecipeMigration(fileLocation, countryCode, MigrationFactory
            .importRecipes(Files.newInputStream(RECIPES_PATH)));

    assertThat(mj.getMigrationEntries().map(MigrationEntry::getOriginalNumber)
        .collect(Collectors.toList()))
        .containsExactlyElementsIn(Files.readAllLines(fileLocation));
    assertThat(mj.getCountryCode().toString()).matches(countryCode);
  }

  @Test
  public void createFromNumberString_invalidNumberFormat_expectException() {
    String numberInput = "+44 one2 34 56";
    String sanitizedNumber = "44one23456";
    try {
      MigrationFactory.createMigration(numberInput, "44");
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (IllegalArgumentException e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e).hasMessageThat().contains(sanitizedNumber);
    } catch (IOException e) {
      // IOException is not being tested here
      e.printStackTrace();
      Assert.fail("Expected RuntimeException and did not receive");
    }
  }

  @Test
  public void createFromNumberString_validNumberFormat_expectValidFields() throws IOException {
    String numberString = "12345";
    String countryCode = "1";
    MigrationJob mj = MigrationFactory.createCustomRecipeMigration(numberString, countryCode, MigrationFactory
            .importRecipes(Files.newInputStream(RECIPES_PATH)));

    assertThat(mj.getMigrationEntries().map(MigrationEntry::getOriginalNumber)
        .collect(Collectors.toList()))
        .containsExactly(numberString);
    assertThat(mj.getCountryCode().toString()).matches(countryCode);
  }

  @Test
  public void createWithCustomRecipes_invalidPathLocation_expectException() {
    Path fileLocation = Paths.get("invalid-recipe-location");
    try {
      MigrationFactory.createCustomRecipeMigration("12345", "44", MigrationFactory
              .importRecipes(Files.newInputStream(fileLocation)));
      Assert.fail("Expected IOException and did not receive");
    } catch (IOException e) {
      assertThat(e).isInstanceOf(NoSuchFileException.class);
      assertThat(e).hasMessageThat().contains(fileLocation.toString());
    }
  }
}
