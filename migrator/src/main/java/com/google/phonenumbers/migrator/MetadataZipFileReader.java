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

import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents a standard compliant metadata zip file where each {@link MetadataZipFileReader} zip
 * file contains {@link CsvTable}'s that can be imported based on specified BCP-47 format numerical
 * country codes without the need to extract the whole zip file.
 */
public final class MetadataZipFileReader {

  private final ZipFile metadataZipFile;
  private final String zipName;

  private MetadataZipFileReader(String fileLocation, String zipName) throws IOException {
    this.metadataZipFile = new ZipFile(fileLocation);
    this.zipName = zipName;
  }

  /**
   * Returns a MetadataZipFileReader for the given string (e.g. "./metadata.zip").
   */
  public static MetadataZipFileReader of(String fileLocation) throws IOException {
    Preconditions.checkNotNull(fileLocation);
    Preconditions.checkArgument(new File(fileLocation).exists(),
        "Invalid zip file location: ", fileLocation);

    String[] dirPaths = fileLocation.split("/");
    String zipName = dirPaths[dirPaths.length - 1].isEmpty() ?
        dirPaths[dirPaths.length - 2] : dirPaths[dirPaths.length - 1];
    return new MetadataZipFileReader(fileLocation, zipName.split(".zip")[0]);
  }

  /**
   * Returns the {@link CsvTable} for the given BCP-47 numerical country code (e.g. "44") if present.
   */
  public Optional<CsvTable<RangeKey>> importCsvTable(String countryCode) throws IOException {
    String csvTableLocation = zipName + "/" + countryCode + "/ranges.csv";
    ZipEntry csvFile = metadataZipFile.getEntry(csvTableLocation);

    if (csvFile == null) {
      return Optional.empty();
    }
    return Optional.of(CsvTable.importCsv(RangesTableSchema.SCHEMA,
        new InputStreamReader(metadataZipFile.getInputStream(csvFile))));
  }

  public static void main(String[] args) throws Exception {
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

    MetadataZipFileReader m = MetadataZipFileReader.of(fileLocation);
    Optional<CsvTable<RangeKey>> ranges = m.importCsvTable(countryCode);
    ranges.orElseThrow(() -> new Exception("Country code not supported in zipfile"));

    System.out.println("Table imported!");
    ImmutableSet<RangeKey> columns = ranges.get().getKeys();
    for (RangeKey key : columns) {
      System.out.println(key);
    }
  }

}
