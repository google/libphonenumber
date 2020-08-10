package com.google.phonenumbers.migrator;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

public class CommandLineMain {

  public static void main(String[] args) throws NumberParseException, IOException {

    // FileInputStream re = new FileInputStream(new File("../recipes.csv"));
    // CsvTable<RangeKey> h = CsvTable.importCsv(RecipesTableSchema.SCHEMA, );

    // System.out.println(RecipesTableSchema.toRangeTable(h));
    // RangeTable t = RecipesTableSchema.toRangeTable(h);
    //
    // System.out.println(RecipesTableSchema.toCsv(t));
    // RangeTree op = t.getRanges(RecipesTableSchema.IS_FINAL_MIGRATION, true);
    // RangeTree up = t.getRanges(RecipesTableSchema.NEW_FORMAT, "xx898x");
    // System.out.println(op.intersect(up));
    // System.out.println(op.contains(DigitSequence.of("1233212345")));

    MigrationJob b = MigrationJob.from(" + 7789780946130  ", "GB");
    MigrationJob a = MigrationJob.from(Paths.get("../numbers.txt"), "GB");

    // System.out.println(a.getNumberRanges());
    // System.out.println(a.getRegionCode());
    System.out.println(b.getNumberRanges());
    System.out.println(b.getRegionCode());
    System.out.println(b.getRecipes());

    // runZipTest();
  }










  public static void runZipTest() throws IOException {
    DigitSequence test = DigitSequence.of(sanitizeString("+447780946130"));
    System.out.println(test.last(test.length()-2));
    MetadataZipFileReader m = MetadataZipFileReader.of(Paths.get("../metadata.zip"));
    Optional<CsvTable<RangeKey>> ranges = Optional.ofNullable(
        m.importCsvTable(DigitSequence.of("44"))
            .orElseThrow(() -> new RuntimeException("Country code not supported in zipfile")));

    System.out.println("Table imported!");
    RangeTable x = RangesTableSchema.toRangeTable(ranges.get());
    // RangeTree o = x.getRanges(RangesTableSchema.TYPE, ExtType.MOBILE);
    // System.out.println(o);
    // System.out.println(x);
    System.out.println(ranges.get());
    for (RangeKey rangeKey : ranges.get().getKeys()) {

      if (rangeKey.contains(test.last(test.length()-2), test.length()-2)) {
        System.out.println(rangeKey);
        System.out.println("\tHERE " + test);
      }
    }
  }

  private static String sanitizeString(String number) {
    return number.replace('+', ' ')
        .replaceAll("[\\s]", "");
  }
}
