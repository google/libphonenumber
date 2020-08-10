package com.google.phonenumbers.migrator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

@Command(name = "Migrator Tool", description = "Command line phone number migration tool.\n"
    + "Please enter a path to a text file containing E.164 phone numbers from the same region or a "
    + "single E.164 number as well as the corresponding region code to begin migrations.\n")
public class CommandLineMain {

  @Option(names = {"-n", "--number"}, description = "Single E.164 phone number to migrate")
  String numberInput;

  @Option(names = {"-f",
      "--file"}, description = "Text file containing E.164 phone numbers to migrate")
  String fileInput;

  @Option(names = {"-r",
      "--region"}, description = "The two digit BCP-47 region code the given phone number(s) belong to")
  String regionCode;

  @Option(names = {"-h", "--help"}, description = "Display help", usageHelp = true)
  boolean help;


  public boolean argumentsValid() {
    return regionCode != null && (numberInput != null || fileInput != null);
  }


  public static void main(String[] args) throws IOException {
    CommandLineMain clm = CommandLine.populateCommand(new CommandLineMain(), args);
    if (clm.help) {
      CommandLine.usage(clm, System.out, Ansi.AUTO);
    } else {
      MigrationJob migrationJob;

      if (!clm.argumentsValid()) {
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
    }
  }
}
