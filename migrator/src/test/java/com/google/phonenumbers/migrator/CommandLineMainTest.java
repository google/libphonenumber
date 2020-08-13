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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import picocli.CommandLine;

@RunWith(JUnit4.class)
public class CommandLineMainTest {
  private static final String TEST_REGION_CODE = "GB";
  private static final String TEST_NUMBER_INPUT = "12345";
  private static final String TEST_FILE_INPUT = "../test-file-path.txt";

  @Test
  public void createFromPath_noPathSpecified_expectInsufficientArguments() {
    String[] args = ("--region="+TEST_REGION_CODE).split(" ");
    CommandLineMain p = CommandLine.populateCommand(new CommandLineMain(), args);
    assertThat(p.insufficientArguments()).isTrue();
  }

  @Test
  public void createFromNumberString_expectSufficientArguments() {
    String[] args = ("--region="+TEST_REGION_CODE+" --number="+TEST_NUMBER_INPUT).split(" ");
    CommandLineMain p = CommandLine.populateCommand(new CommandLineMain(), args);
    assertThat(p.getRegionCode()).matches(TEST_REGION_CODE);
    assertThat(p.getNumberInput()).matches(TEST_NUMBER_INPUT);
    assertThat(p.getFileInput()).isNull();
    assertThat(p.insufficientArguments()).isFalse();
  }

  @Test
  public void createFromPath_expectSufficientArguments() {
    String[] args = ("--region="+TEST_REGION_CODE+" --file="+TEST_FILE_INPUT).split(" ");
    CommandLineMain p = CommandLine.populateCommand(new CommandLineMain(), args);
    assertThat(p.getRegionCode()).matches(TEST_REGION_CODE);
    assertThat(p.getFileInput()).matches(TEST_FILE_INPUT);
    assertThat(p.getNumberInput()).isNull();
    assertThat(p.insufficientArguments()).isFalse();
  }
}
