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
import static com.google.phonenumbers.migrator.testing.testUtils.AssertUtil.assertThrows;

import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetadataZipFileReaderTest {
  private String testDataPath = "./src/test/java/com/google/phonenumbers/migrator/testing/testData/";

  @Test
  public void testInvalidFileLocation() {
    String fileLocation = "invalid-zipfile-location";
    assertThrows(IllegalArgumentException.class, () -> {
      try {
        MetadataZipFileReader.of(fileLocation);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  @Test
  public void testValidCsvTable() throws IOException {
    String fileLocation = testDataPath + "testMetadataZip.zip";
    MetadataZipFileReader validZip = MetadataZipFileReader.of(fileLocation);
    Optional<CsvTable<RangeKey>> regionTable = validZip.importCsvTable("1");
    assertThat(regionTable.isPresent()).isEqualTo(true);
  }

  @Test
  public void testUnsupportedCsvTable() throws IOException {
    String fileLocation = testDataPath + "testMetadataZip.zip/";
    MetadataZipFileReader validZip = MetadataZipFileReader.of(fileLocation);
    Optional<CsvTable<RangeKey>> regionTable = validZip.importCsvTable("2");
    assertThat(regionTable.isPresent()).isEqualTo(false);
  }
}
