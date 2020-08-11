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
  public void testInsufficientArguments() {
    String[] args = ("--region="+TEST_REGION_CODE).split(" ");
    CommandLineMain p = CommandLine.populateCommand(new CommandLineMain(), args);
    assertThat(p.insufficientArguments()).isTrue();
  }

  @Test
  public void testAcceptableNumberInputArguments() {
    String[] args = ("--region="+TEST_REGION_CODE+" --number="+TEST_NUMBER_INPUT).split(" ");
    CommandLineMain p = CommandLine.populateCommand(new CommandLineMain(), args);
    assertThat(p.getRegionCode()).matches(TEST_REGION_CODE);
    assertThat(p.getNumberInput()).matches(TEST_NUMBER_INPUT);
    assertThat(p.getFileInput()).isNull();
    assertThat(p.insufficientArguments()).isFalse();
  }

  @Test
  public void testAcceptableFileInputArguments() {
    String[] args = ("--region="+TEST_REGION_CODE+" --file="+TEST_FILE_INPUT).split(" ");
    CommandLineMain p = CommandLine.populateCommand(new CommandLineMain(), args);
    assertThat(p.getRegionCode()).matches(TEST_REGION_CODE);
    assertThat(p.getFileInput()).matches(TEST_FILE_INPUT);
    assertThat(p.getNumberInput()).isNull();
    assertThat(p.insufficientArguments()).isFalse();
  }
}
