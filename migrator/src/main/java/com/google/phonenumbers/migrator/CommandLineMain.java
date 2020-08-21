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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

@Command(name = "Command Line Migrator Tool:",
    description =  "Please enter a path to a text file containing E.164 phone numbers "
      + "(e.g. +1234567891, +1234568890) from the same region or a single E.164 number as "
      + "well as the corresponding two digit BCP-47 region code (e.g. GB, US) to begin migrations.\n")
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

  @Option(names = {"-r", "--region"},
      required = true,
      description = "The two digit BCP-47 region code the given phone number(s) belong to (e.g. GB)")
  String regionCode;

  @Option(names = {"-h", "--help"}, description = "Display help", usageHelp = true)
  boolean help;

  public static void main(String[] args) throws IOException {
    CommandLineMain clm = CommandLine.populateCommand(new CommandLineMain(), args);
    if (clm.help) {
      CommandLine.usage(clm, System.out, Ansi.AUTO);
    }

    MigrationJob migrationJob;
    if (clm.numberInput.number != null) {
      migrationJob = MigrationFactory.createMigration(clm.numberInput.number, clm.regionCode);
    } else {
      migrationJob = MigrationFactory.createMigration(Paths.get(clm.numberInput.file), clm.regionCode);
    }

    System.out.println(migrationJob.getRecipesTable());
    System.out.println(migrationJob.getNumberRange().collect(Collectors.toList()));
    System.out.println(migrationJob.getRegionCode());
    System.out.println(migrationJob.getAllMigratableNumbers().collect(Collectors.toList()));
  }
}
