package com.google.phonenumbers.migrator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

@Command(name = "Command Line Migrator Tool: (numberInput | fileInput) regionCode\n",
    description =  "Please enter a path to a text file containing E.164 phone numbers "
      + "(e.g. +12 (345) 67-890, +1234568890) from the same region or a single E.164 number as "
      + "well as the corresponding two digit BCP-47 region code (e.g. GB, US) to begin migrations.\n")
public class CommandLineMain {

  @Option(names = {"-n", "--number"},
      description = "Single E.164 phone number to migrate (e.g. +12 (345) 67-890 | +1234567890)")
  String numberInput;

  @Option(names = {"-f", "--file"},
      description = "Text file containing comma separated E.164 phone numbers to migrate")
  String fileInput;

  @Option(names = {"-r", "--region"},
      description = "The two digit BCP-47 region code the given phone number(s) belong to (e.g. GB)")
  String regionCode;

  @Option(names = {"-h", "--help"}, description = "Display help", usageHelp = true)
  boolean help;

  public String getNumberInput() {
    return numberInput;
  }

  public String getFileInput() {
    return fileInput;
  }

  public String getRegionCode() {
    return regionCode;
  }

  public boolean insufficientArguments() {
    return regionCode == null || (numberInput == null && fileInput == null);
  }


  /**
   * Runs the command line migrator tool with functionality specified by then given user's command
   * line arguments
   *
   * @param args which expects two command line arguments;
   *    numberInput: single E.164 number string to be potentially migrated
   *            OR
   *    fileInput: path to text file holding comma separated E.164 numbers to be migrated
   *
   *    regionCode: two digit BCP-47 code relating to the region the inputted number(s) originate
   *                (e.g. GB)
   */
  public static void main(String[] args) throws IOException {
    CommandLineMain clm = CommandLine.populateCommand(new CommandLineMain(), args);
    if (clm.help) {
      CommandLine.usage(clm, System.out, Ansi.AUTO);
    } else {
      MigrationJob migrationJob;

      if (clm.insufficientArguments()) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Missing argument(s)");
        if (clm.regionCode == null) {
          System.out.print("Enter two digit BCP-47 region code: ");
          clm.regionCode = scanner.next();
        }
        if (clm.numberInput == null && clm.fileInput == null) {
          String migrationType = "";
          do {
            System.out
                .print("Are you performing a file migration or single number migration? (f/n): ");
            migrationType = scanner.next().toLowerCase();
          } while (!migrationType.equals("f") && !migrationType.equals("n"));

          if (migrationType.equals("f")) {
            System.out.print("Enter file location: ");
            clm.fileInput = scanner.next();
          } else {
            System.out.print("Enter single E.164 number input: ");
            clm.numberInput = scanner.next();
          }
        }
      }

      if (clm.numberInput != null) {
        migrationJob = MigrationJob.from(clm.numberInput, clm.regionCode);
      } else {
        migrationJob = MigrationJob.from(Paths.get(clm.fileInput), clm.regionCode);
      }

      System.out.println(migrationJob.getRecipesTable());
      System.out.println(migrationJob.getNumberRange());
      System.out.println(migrationJob.getRegionCode());
      System.out.println(migrationJob.getAllMigratableNumbers());
    }
  }
}
