package com.google.phonenumbers.migrator;

import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MetadataReader {

  private static ZipFile metadata;
  private static String regionCode;

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      manuallySetArguments();
    } else {
      setMetadata(args[0]);
      setRegionCode(args[1]);
    }

    ZipEntry csvFile = getCsvFile();

    if (csvFile == null) {
      System.err.println("ERROR: Ranges for " + regionCode + " region code not found");
      System.exit(1);
    }

    CsvTable ranges = CsvTable.importCsv(RangesTableSchema.SCHEMA,
        new InputStreamReader(metadata.getInputStream(csvFile)));
    System.out.println("Table imported!");
  }

  public static ZipEntry getCsvFile() {
    String CsvTableLocation = "metadata/" + regionCode + "/ranges.csv";
    return metadata.getEntry(CsvTableLocation);
  }

  public static void manuallySetArguments() {
    Scanner scanner = new Scanner(System.in);
    System.out.println("You have not entered correct command line arguments");
    System.out.print("\tPlease enter a zip file location of metadata csv files: ");
    setMetadata(scanner.next());
    System.out.print("\tPlease enter the region code of the ranges you want to import: ");
    setRegionCode(scanner.next());
  }

  public static void setMetadata(String fileLocation) {
    try {
      MetadataReader.metadata = new ZipFile(fileLocation);
    } catch (IOException e) {
      System.err.println("ERROR: " + fileLocation + " does not route to a valid zip file");
      System.exit(1);
    }
  }

  public static void setRegionCode(String regionCode) {
    MetadataReader.regionCode = regionCode;
  }
}
