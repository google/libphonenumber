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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents a standard libphonenumber metadata zip file where each {@link MetadataZipFileReader}
 * zip file contains {@link CsvTable}'s that can be imported based on specified BCP-47 format
 * numerical country codes without the need to extract the whole zip file.
 */
public final class MetadataZipFileReader {

  private final ZipInputStream metadataZipFile;

  private MetadataZipFileReader(ZipInputStream metadataZipFile) {
    this.metadataZipFile = checkNotNull(metadataZipFile);
  }

  /**
   * Returns a MetadataZipFileReader for the given zip stream.
   */
  public static MetadataZipFileReader of(InputStream file) throws IOException {
    checkNotNull(file);

    return new MetadataZipFileReader(new ZipInputStream(file));
  }

  /**
   * Returns the {@link CsvTable} for the given BCP-47 numerical country code (e.g. "44") if present.
   */
  public Optional<CsvTable<RangeKey>> importCsvTable(DigitSequence countryCode) throws IOException {
    String csvTableLocation = "metadata/" + countryCode + "/ranges.csv";
    ZipEntry entry;

    while ((entry = metadataZipFile.getNextEntry()) != null) {
      if (entry.getName().equals(csvTableLocation)) {
        return Optional.of(CsvTable.importCsv(RangesTableSchema.SCHEMA,
            new InputStreamReader(metadataZipFile)));
      }
    }
    return Optional.empty();
  }

  /**
   * Method used as an example of how to instantiate a MetadataReader object for a given zip file
   * and import the {@link CsvTable} corresponding to a given BCP-47 numerical country code.
   *
   * @param args which expects two command line arguments;
   *  fileLocation: the path of the zipfile to be read
   *  countryCode: numerical code relating to the CSV table that is to be imported
   */
  public static void main(String[] args) throws IOException, RuntimeException {
    String fileLocation;
    String countryCode;

    if (args.length < 2) {
      Scanner scanner = new Scanner(System.in);
      System.out.println("You have not entered correct command line arguments");

      System.out.print("\tPlease enter a zip file location of metadata csv files: ");
      fileLocation = scanner.next();

      System.out.print("\tPlease enter the country code of the ranges you want to import: ");
      countryCode = scanner.next();
    } else {
      fileLocation = args[0];
      countryCode = args[1];
    }

    MetadataZipFileReader m = MetadataZipFileReader.of(Files.newInputStream(Paths.get(fileLocation)));
    Optional<CsvTable<RangeKey>> ranges = Optional.ofNullable(
        m.importCsvTable(DigitSequence.of(countryCode))
            .orElseThrow(() -> new RuntimeException("Country code not supported in zipfile")));

    System.out.println("Table imported!");
    ranges.get().getKeys().forEach(System.out::println);
  }
}
