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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.phonenumbers.migrator.MigrationJob.MigrationReport;
import java.io.IOException;
import java.nio.file.Files;
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

  @ArgGroup()
  OptionalParameterType optionalParameter;

  static class OptionalParameterType {
    @Option(names = {"-e", "--exportInvalidMigrations"},
        description = "boolean flag specifying that text files created after the migration process"
            + " for standard recipe --file migrations should contain the migrated version of a given"
            + " phone number, regardless of whether the migration resulted in an invalid phone number."
            + " By default, a strict approach is used and when a migration is seen as invalid, the"
            + " original phone number is written to file. Invalid numbers will be printed at the"
            + " bottom of the text file.")
    boolean exportInvalidMigrations;

    @Option(names = {"-r", "--customRecipe"},
        description = "Csv file containing a custom migration recipes table. When using custom recipes"
            + ", validity checks on migrated numbers will not be performed. Note: custom recipes must"
            + " be run with the --exportInvalidMigrations flag.")
    String customRecipe;
  }

  @Option(names = {"-h", "--help"}, description = "Display help", usageHelp = true)
  boolean help;

  public static void main(String[] args) throws IOException {
    CommandLineMain clm = CommandLine.populateCommand(new CommandLineMain(), args);
    if (clm.help) {
      CommandLine.usage(clm, System.out, Ansi.AUTO);
    } else {
      MigrationJob migrationJob;
      if (clm.numberInput.number != null) {
        if (clm.optionalParameter != null && clm.optionalParameter.customRecipe != null) {
          migrationJob = MigrationFactory
              .createCustomRecipeMigration(clm.numberInput.number, clm.countryCode,
                      MigrationFactory.importRecipes(Files.newInputStream(Paths.get(clm.optionalParameter.customRecipe))));
        } else {
          migrationJob = MigrationFactory.createMigration(clm.numberInput.number, clm.countryCode);
        }
      } else {
        if (clm.optionalParameter != null && clm.optionalParameter.customRecipe != null) {
          migrationJob = MigrationFactory
              .createCustomRecipeMigration(Paths.get(clm.numberInput.file), clm.countryCode,
                      MigrationFactory.importRecipes(Files.newInputStream(Paths.get(clm.optionalParameter.customRecipe))));
        } else {
          migrationJob = MigrationFactory
              .createMigration(Paths.get(clm.numberInput.file), clm.countryCode,
                  clm.optionalParameter != null && clm.optionalParameter.exportInvalidMigrations);
        }
      }

      MigrationReport mr =  migrationJob.getMigrationReportForCountry();
      System.out.println("Migration of country code +" + migrationJob.getCountryCode() + " phone "
          + "number(s):");
      if (clm.numberInput.file != null) {
        printFileReport(mr, Paths.get(clm.numberInput.file));
      } else {
        printNumberReport(mr);
      }
    }
  }

  /** Details printed to console after a --file type migration. */
  private static void printFileReport(MigrationReport mr, Path originalFile) throws IOException {
    String newFile = mr.exportToFile(originalFile.getFileName().toString());
    Scanner scanner = new Scanner((System.in));
    String response = "";

    System.out.println("New numbers file created at: " + System.getProperty("user.dir") + "/" + newFile);
    while (!response.equals("0")) {
      System.out.println("\n(0) Exit");
      System.out.println("(1) Print Metrics");
      System.out.println("(2) View All Recipes Used");
      System.out.print("Select from the above options: ");
      response = scanner.nextLine();
      if (response.equals("1")) {
        mr.printMetrics();
      } else if (response.equals("2")) {
        printUsedRecipes(mr);
      }
    }
  }

  /** Details printed to console after a --number type migration. */
  private static void printNumberReport(MigrationReport mr) {
    if (mr.getValidMigrations().size() == 1) {
      System.out.println("Successful migration into: +"
          + mr.getValidMigrations().get(0).getMigratedNumber());
      printUsedRecipes(mr);
    } else if (mr.getInvalidMigrations().size() == 1) {
      System.out.println("The number was migrated into '+"
          + mr.getInvalidMigrations().get(0).getMigratedNumber() + "' but this number was not "
          + "seen as being valid and dialable when inspecting our data for the given country");
      printUsedRecipes(mr);
    } else if (mr.getValidUntouchedEntries().size() == 1) {
      System.out.println("This number was seen to already be valid and dialable based on "
          + "our data for the given country");
    } else {
      System.out.println("This number could not be migrated using any of the recipes from the given"
          + " recipes file");
    }
  }

  private static void printUsedRecipes(MigrationReport mr) {
    Multimap<ImmutableMap<Column<?>, Object>, MigrationResult> recipeToNumbers = mr.getAllRecipesUsed();
    System.out.println("\nRecipe(s) Used:");
    for (ImmutableMap<Column<?>, Object> recipe : recipeToNumbers.keySet()) {
      System.out.println("* " + RecipesTableSchema.formatRecipe(recipe));
      recipeToNumbers.get(recipe).forEach(result -> System.out.println("\t" + result));
      System.out.println("");
    }
  }
}
