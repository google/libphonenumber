package com.google.phonenumbers.migrator;

    import static com.google.common.truth.Truth.assertThat;

    import com.google.i18n.phonenumbers.metadata.DigitSequence;
    import com.google.i18n.phonenumbers.metadata.RangeSpecification;
    import com.google.i18n.phonenumbers.metadata.RangeTree;
    import com.google.i18n.phonenumbers.metadata.table.RangeKey;
    import java.io.FileNotFoundException;
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
  public void testInvalidNumbersFile() {
    String fileLocation = "invalid-path-location";
    try {
      MigrationJob.from(Paths.get(fileLocation), "GB");
      Assert.fail("Expected IOException and did not receive");
    } catch (IOException e) {
      assertThat(e).isInstanceOf(FileNotFoundException.class);
      assertThat(e).hasMessageThat().contains(fileLocation);
    }
  }

  @Test
  public void testInvalidNumberString() {
    String numberInput = "+44 one2 34 56";
    String sanitizedNumber = "44one23456";
    try {
      MigrationJob.from(numberInput, "GB");
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
  public void testInvalidCustomRecipesPath() {
    String fileLocation = "invalid-recipe-location";
    try {
      MigrationJob.from("12345", "GB", fileLocation);
      Assert.fail("Expected IOException and did not receive");
    } catch (IOException e) {
      assertThat(e).isInstanceOf(NoSuchFileException.class);
      assertThat(e).hasMessageThat().contains(fileLocation);
    }
  }

  @Test
  public void testGetAllMigratableNumbersWithNoMatches() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    MigrationJob job = MigrationJob.from("34", "GB", recipesPath);

    RangeTree noMatchesRange = job.getAllMigratableNumbers();
    assertThat(noMatchesRange.asRangeSpecifications()).isEmpty();
  }

  @Test
  public void testGetAllMigratableNumbersWithMatches() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    String numbersPath = TEST_DATA_PATH + "testNumbersFile.txt";
    MigrationJob job = MigrationJob.from(Paths.get(numbersPath), "GB", recipesPath);

    RangeTree noMatchesRange = job.getAllMigratableNumbers();
    assertThat(noMatchesRange.asRangeSpecifications())
        .containsExactlyElementsIn(job.getNumberRange().asRangeSpecifications());
  }

  @Test
  public void testGetMigratableNumbersWithInvalidKey() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("123"));
    RangeKey invalidKey = RangeKey.create(testRangeSpec, Collections.singleton(3));

    MigrationJob job = MigrationJob.from("123", "GB", recipesPath);
    try {
      job.getMigratableNumbers(invalidKey);
      Assert.fail("Expected RuntimeException and did not receive");
    } catch (RuntimeException e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
      assertThat(e).hasMessageThat().contains(invalidKey.toString());
    }
  }

  @Test
  public void testGetMigratableNumbersWithValidKey() throws IOException {
    String recipesPath = TEST_DATA_PATH + "testRecipesFile.csv";
    RangeSpecification testRangeSpec = RangeSpecification.from(DigitSequence.of("12"));
    RangeKey validKey = RangeKey.create(testRangeSpec, Collections.singleton(5));

    MigrationJob job = MigrationJob.from("123", "GB", recipesPath);
    assertThat(job.getMigratableNumbers(validKey).asRangeSpecifications()).isEmpty();
  }
}
