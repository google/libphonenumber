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

import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetadataZipFileReaderTest {

  private static final String TEST_DATA_PATH = "./src/test/java/com/google/phonenumbers/migrator/testing/testData/";

  @Test
  public void createInstance_invalidFileLocation_expectException() {
    String fileLocation = "invalid-zipfile-location";
    try {
      MetadataZipFileReader.of(Files.newInputStream(Paths.get(fileLocation)));
      Assert.fail("Expected IOException and did not receive");
    } catch (IOException e) {
      assertThat(e).isInstanceOf(NoSuchFileException.class);
      assertThat(e).hasMessageThat().contains(fileLocation);
    }
  }

  @Test
  public void importTable_countryCodeInZip_expectCsvTable() throws IOException {
    String fileLocation = TEST_DATA_PATH + "testMetadataZip.zip";
    MetadataZipFileReader validZip = MetadataZipFileReader.of(Files.newInputStream(Paths.get(fileLocation)));
    Optional<CsvTable<RangeKey>> regionTable = validZip.importCsvTable(DigitSequence.of("1"));
    assertThat(regionTable).isPresent();
  }

  @Test
  public void importTable_countryCodeNotInZip_expectEmptyCsvTable() throws IOException {
    String fileLocation = TEST_DATA_PATH + "testMetadataZip.zip/";
    MetadataZipFileReader validZip = MetadataZipFileReader.of(Files.newInputStream(Paths.get(fileLocation)));
    Optional<CsvTable<RangeKey>> regionTable = validZip.importCsvTable(DigitSequence.of("2"));
    assertThat(regionTable).isEmpty();
  }
}
