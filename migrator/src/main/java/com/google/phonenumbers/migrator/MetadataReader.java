package com.google.phonenumbers.migrator;

import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A class used to represent a given metadata zip file. Each {@link MetadataReader} instance
 * represents a zip file containing Csv Tables that can be imported based on specified
 * region codes without the need to extract the whole zip file.
 */
public final class MetadataReader {
  private final ZipFile metadataZipFile;

  public MetadataReader(String fileLocation) throws IOException {
    this.metadataZipFile = new ZipFile(fileLocation);
  }

  /**
   * Returns an Optional {@link CsvTable} correlating to a given regionCode. If no ranges.csv file
   * can be found in the {@link #metadataZipFile} for the region code, an empty Optional is returned
   */
  public Optional<CsvTable<RangeKey>> getCsvTable(String regionCode) throws IOException {
    String CsvTableLocation = "metadata/" + regionCode + "/ranges.csv";
    ZipEntry csvFile = metadataZipFile.getEntry(CsvTableLocation);

    if (csvFile == null) {
      return Optional.empty();
    }
    return Optional.of(CsvTable.importCsv(RangesTableSchema.SCHEMA,
        new InputStreamReader(metadataZipFile.getInputStream(csvFile))));
  }

  public static void main(String[] args) throws IOException {
    String fileLocation;
    String regionCode;

    if (args.length < 2) {
      Scanner scanner = new Scanner(System.in);
      System.out.println("You have not entered correct command line arguments");

      System.out.print("\tPlease enter a zip file location of metadata csv files: ");
      fileLocation = scanner.next();

      System.out.print("\tPlease enter the region code of the ranges you want to import: ");
      regionCode = scanner.next();
    } else {
      fileLocation = args[0];
      regionCode = args[1];
    }

    MetadataReader m = new MetadataReader(fileLocation);
    Optional<CsvTable<RangeKey>> ranges = m.getCsvTable(regionCode);

    if (ranges.isPresent()) {
      System.out.println("Table imported!");
      ImmutableSet<RangeKey> columns = ranges.get().getKeys();
      for (RangeKey key : columns) {
        System.out.println(key);
      }
    } else {
      System.out.println("Region code not supported in specified metadata zip");
    }

  }

}
