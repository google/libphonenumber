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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import picocli.CommandLine;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.MutuallyExclusiveArgsException;

@RunWith(JUnit4.class)
public class CommandLineMainTest {
  private static final String TEST_COUNTRY_CODE = "44";
  private static final String TEST_NUMBER_INPUT = "12345";
  private static final String TEST_FILE_INPUT = "../test-file-path.txt";

  @Test
  public void createMigrationJob_noNumberInputSpecified_expectException() {
    String[] args = ("--countryCode=" + TEST_COUNTRY_CODE).split(" ");
    try {
      CommandLine.populateCommand(new CommandLineMain(), args);
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (MissingParameterException e) {
      assertThat(e).isInstanceOf(MissingParameterException.class);
    }
  }

  @Test
  public void createMigrationJob_numberAndFile_expectException() {
    String[] args = ("--countryCode=" + TEST_COUNTRY_CODE + " --number=" + TEST_NUMBER_INPUT +
        " --file=" + TEST_FILE_INPUT).split(" ");
    try {
      CommandLine.populateCommand(new CommandLineMain(), args);
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (MutuallyExclusiveArgsException e) {
      assertThat(e).isInstanceOf(MutuallyExclusiveArgsException.class);
    }
  }

  @Test
  public void createFromNumberString_expectSufficientArguments() {
    String[] args = ("--countryCode=" + TEST_COUNTRY_CODE + " --number=" + TEST_NUMBER_INPUT)
        .split(" ");
    CommandLineMain p = CommandLine.populateCommand(new CommandLineMain(), args);
    assertThat(p.countryCode).matches(TEST_COUNTRY_CODE);
    assertThat(p.numberInput.number).matches(TEST_NUMBER_INPUT);
    assertThat(p.numberInput.file).isNull();
  }

  @Test
  public void createFromPath_expectSufficientArguments() {
    String[] args = ("--countryCode="+ TEST_COUNTRY_CODE +" --file="+TEST_FILE_INPUT)
        .split(" ");
    CommandLineMain p = CommandLine.populateCommand(new CommandLineMain(), args);
    assertThat(p.countryCode).matches(TEST_COUNTRY_CODE);
    assertThat(p.numberInput.file).matches(TEST_FILE_INPUT);
    assertThat(p.numberInput.number).isNull();
  }

  @Test
  public void createMigrationJob_exportInvalidMigrationsAndCustomRecipe_expectException() {
    String[] args = ("--countryCode=" + TEST_COUNTRY_CODE + " --number=" + TEST_NUMBER_INPUT
        + " --exportInvalidMigrations --customRecipe=" + TEST_FILE_INPUT).split(" ");
    try {
      CommandLine.populateCommand(new CommandLineMain(), args);
      Assert.fail("Expected MutuallyExclusiveArgsException and did not receive");
    } catch (MutuallyExclusiveArgsException e) {
      assertThat(e.getMessage()).contains("mutually exclusive");
      assertThat(e.getMessage()).contains("--exportInvalidMigrations, --customRecipe");
    }
  }
}
