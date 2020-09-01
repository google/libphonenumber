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

import com.google.phonenumbers.migrator.MigrationJob.MigrationReport;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

@Command(name = "Command Line Migrator Tool:",
    description =  "Please enter a path to a text file containing E.164 phone numbers "
      + "(e.g. +4434567891, +1234568890) from the same country or a single E.164 number as "
      + "well as the corresponding BCP-47 country code (e.g. 44, 1) to begin migrations.\n")
public final class CommandLineMain {
  /**
   * Fields cannot be private or final to allow for @Command annotation to set and retrieve values.
   */

  @ArgGroup(multiplicity = "1")
  NumberInputType numberInput;

  static class NumberInputType {
    @Option(names = {"-n", "--number"},
        description = "Single E.164 phone number to migrate (e.g. \"+1234567890\") or an internationally formatted "
            + "number starting with '+' (e.g. \"+12 (345) 67-890\") which will have any non-digit characters after "
            + "the leading '+' removed for processing.")
    String number;

    @Option(names = {"-f", "--file"},
        description = "Text file to be migrated which contains one E.164 phone "
            + "number per line")
    String file;
  }

  @Option(names = {"-c", "--countryCode"},
      required = true,
      description = "The BCP-47 country code the given phone number(s) belong to (e.g. 44)")
  String countryCode;

  @Option(names = {"-h", "--help"}, description = "Display help", usageHelp = true)
  boolean help;

  public static void main(String[] args) throws IOException {
    CommandLineMain clm = CommandLine.populateCommand(new CommandLineMain(), args);
    if (clm.help) {
      CommandLine.usage(clm, System.out, Ansi.AUTO);
    } else {
      MigrationJob migrationJob;
      if (clm.numberInput.number != null) {
        migrationJob = MigrationFactory.createMigration(clm.numberInput.number, clm.countryCode);
      } else {
        migrationJob = MigrationFactory
            .createMigration(Paths.get(clm.numberInput.file), clm.countryCode);
      }

      MigrationReport mr =  migrationJob.getMigrationReportForCountry();
      if (clm.numberInput.file != null) {
        printFileReport(mr, Paths.get(clm.numberInput.file));
      } else {
        printNumberReport(mr);
      }
    }
  }

  private static void printFileReport(MigrationReport mr, Path originalFile) throws IOException {
    String newFile = mr.exportToFile(originalFile.getFileName().toString());
    Scanner scanner = new Scanner((System.in));
    String response = "";

    System.out.println("New numbers file created at: " + newFile);
    while (!response.equals("0")) {
      System.out.println("\n(0) Exit");
      System.out.println("(1) Print Metrics");
      System.out.print("Select from the above options: ");
      response = scanner.nextLine();
      if (response.equals("1")) {
        mr.printMetrics();
      }
    }
  }

  private static void printNumberReport(MigrationReport mr) {
    if (mr.getValidMigrations().size() == 1) {
      System.out.println("Successful migration into: +"
          + mr.getValidMigrations().get(0).getMigratedNumber());
    } else if (mr.getInvalidMigrations().size() == 1) {
      System.out.println("The number was migrated into '+"
          + mr.getInvalidMigrations().get(0).getMigratedNumber() + "' but this number was not "
          + "seen as being valid and dialable when inspecting our data for the given country");
    } else if (mr.getValidUntouchedEntries().size() == 1) {
      System.out.println("This number was seen to already be valid and dialable based on "
          + "our data for the given country");
    } else {
      System.out.println("This number was seen as being invalid based on our data for the "
          + "given country but we could not migrate it into a valid format.");
    }
  }
}
