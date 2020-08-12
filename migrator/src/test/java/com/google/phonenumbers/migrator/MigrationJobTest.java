package com.google.phonenumbers.migrator;

import static com.google.common.truth.Truth.assertThat;

import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MigrationJobTest {

  private static final String TEST_DATA_PATH = "./src/test/java/com/google/phonenumbers/migrator/testing/testData/";

  @Test
  public void createFromFilePath_invalidPathLocation_expectException() {
    String fileLocation = "invalid-path-location";
    try {
      MigrationFactory.createMigration(Paths.get(fileLocation), "GB");
      Assert.fail("Expected IOException and did not receive");
    } catch (IOException e) {
      assertThat(e).isInstanceOf(NoSuchFileException.class);
      assertThat(e).hasMessageThat().contains(fileLocation);
    }
  }

  @Test
  public void createFromNumberString_invalidNumberFormat_expectException() {
    String numberInput = "+44 one2 34 56";
    String sanitizedNumber = "44one23456";
    try {
      MigrationFactory.createMigration(numberInput, "GB");
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e).hasMessageThat().contains(sanitizedNumber);
    } catch (IOException e) {
      // IOException is not being tested here
      e.printStackTrace();
      Assert.fail("Expected RuntimeException and did not receive");
    }
  }

  @Test
  public void createWithCustomRecipes_invalidPathLocation_expectException() {
    String fileLocation = "invalid-recipe-location";
    try {
      MigrationFactory.createMigration("12345", "GB", Paths.get(fileLocation));
      Assert.fail("Expected IOException and did not receive");
    } catch (IOException e) {
      assertThat(e).isInstanceOf(NoSuchFileException.class);
      assertThat(e).hasMessageThat().contains(fileLocation);
    }
  }

  @Test
  public void getAllMigratableNumbers_expectNoMatches() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    MigrationJob job = MigrationFactory.createMigration("34", "GB", Paths.get(recipesPath));

    RangeTree noMatchesRange = job.getAllMigratableNumbers();
    assertThat(noMatchesRange.asRangeSpecifications()).isEmpty();
  }

  @Test
  public void getAllMigratableNumbers_expectMatches() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
    MigrationJob job = MigrationFactory
        .createMigration(Paths.get(numbersPath), "GB", Paths.get(recipesPath));

    RangeTree noMatchesRange = job.getAllMigratableNumbers();
    assertThat(noMatchesRange.asRangeSpecifications())
        .containsExactlyElementsIn(job.getNumberRange().asRangeSpecifications());
  }

  @Test
  public void getMigratableNumbers_invalidKey_expectException() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("123"));
    RangeKey invalidKey = RangeKey.create(testRangeSpec, Collections.singleton(3));

    MigrationJob job = MigrationFactory.createMigration("123", "GB", Paths.get(recipesPath));
    try {
      job.getMigratableNumbers(invalidKey);
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e).hasMessageThat().contains(invalidKey.toString());
    }
  }

  @Test
  public void getMigratableNumbers_validKey_expectNoExceptionAndNoMatches() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("12"));
    RangeKey validKey = RangeKey.create(testRangeSpec, Collections.singleton(5));

    MigrationJob job = MigrationFactory.createMigration("123", "GB", Paths.get(recipesPath));
    assertThat(job.getMigratableNumbers(validKey).asRangeSpecifications()).isEmpty();
  }
}
