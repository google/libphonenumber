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

import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
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
      description = "Text file to be migrated which contains one E.164 phone "
          + "number per line")
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
   *    fileInput: path to a given text file to be migrated which holds one
   *               E.164 number per line
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
            scanner.nextLine();
            clm.numberInput = scanner.nextLine();
          }
        }
      }

      if (clm.numberInput != null) {
        migrationJob = MigrationFactory.createMigration(clm.numberInput, clm.regionCode);
      } else {
        migrationJob = MigrationFactory.createMigration(Paths.get(clm.fileInput), clm.regionCode);
      }

      System.out.println(migrationJob.getRecipesCsvTable());
      System.out.println(migrationJob.getNumberRange());
      System.out.println("");
      RangeKey key = RangeKey.create(RangeSpecification.from(DigitSequence.of("84120")), Collections.singleton(12));
      System.out.println(key);
      System.out.println(migrationJob.performSingleRecipeMigration(key));
      System.out.println(migrationJob.performAllMigrations());
    }
  }
}
