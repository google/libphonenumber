package com.google.phonenumbers.migrator;

import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public final class MigrationJob {

  private final static String DEFAULT_RECIPES_PATH = "../recipes.csv";

  private final CsvTable<RangeKey> recipes;
  private final RangeTree numberRanges;
  private final PhoneRegion regionCode;

  public CsvTable<RangeKey> getRecipes() {
    return recipes;
  }

  public RangeTree getNumberRanges() {
    return numberRanges;
  }

  public PhoneRegion getRegionCode() {
    return regionCode;
  }

  public static MigrationJob from(String numberInput, String regionCodeInput) throws IOException {
    PhoneRegion regionCode = PhoneRegion.of(regionCodeInput);
    RangeTree numberRanges = RangeTree.from(sanitizeNumberString(numberInput));
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(DEFAULT_RECIPES_PATH));

    return new MigrationJob(numberRanges, regionCode, recipes);
  }

  public static MigrationJob from(Path fileInput, String regionCodeInput) throws IOException {
    Scanner scanner = new Scanner(new FileReader(fileInput.toString()));
    List<String> numbers = new ArrayList<>();
    while (scanner.hasNext()) {
      numbers.addAll(Arrays.asList(scanner.nextLine().split(",")));
    }

    PhoneRegion regionCode = PhoneRegion.of(regionCodeInput);
    RangeTree numberRanges = RangeTree.from(numbers.stream().map(MigrationJob::sanitizeNumberString));
    CsvTable<RangeKey> recipes = importRecipes(Paths.get(DEFAULT_RECIPES_PATH));

    return new MigrationJob(numberRanges, regionCode, recipes);
  }

  private static RangeSpecification sanitizeNumberString(String number) {
    String sanitizedString = number.replaceAll("[+]|[\\s]", "");
    return RangeSpecification.parse(sanitizedString);
  }

  private static CsvTable<RangeKey> importRecipes(Path recipesPath) throws IOException {
    InputStreamReader reader = new InputStreamReader(Files.newInputStream(recipesPath));
    return CsvTable.importCsv(RecipesTableSchema.SCHEMA, reader);
  }

  private MigrationJob(RangeTree numberRanges, PhoneRegion regionCode, CsvTable<RangeKey> recipes) {
    this.numberRanges = numberRanges;
    this.regionCode = regionCode;
    this.recipes = recipes;
  }
}
