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
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.table.Change;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvKeyMarshaller;
import com.google.i18n.phonenumbers.metadata.table.CsvSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The schema of the standard "Recipes" table with rows keyed by {@link RangeKey} and columns:
 * <ol>
 *   <li>{@link #OLD_FORMAT}: The original format of the represented range in the row to be changed.
 *       'x' characters represent indexes that do not need to be changed in a number within the range
 *       and actual digits in the string are values that need to be removed or replaced. (e.g. xx98xx).
 *       The length of this string must match the lengths of (DigitSequence)'s produced by the Row Key.
 *   <li>{@link #NEW_FORMAT}: The migrated format of the represented range in the row. 'x' characters
 *       represent indexes that do not need to be changed in a number within the range and actual
 *       digits in the string are values that need to be added at that given index.
 *   <li>{@link #IS_FINAL_MIGRATION}: A boolean indicating whether the given recipe row would result
 *       in the represented range being migrated into up to date, dialable formats. Recipes which
 *       do not will require the newly formatted range to be migrated again using another matching
 *       recipe.
 *   <li>{@link #COUNTRY_CODE}: The BCP-47 country code in which a given recipe corresponds to.
 *   <li>{@link #DESCRIPTION}: TThe explanation of a migration recipe in words.
 * </ol>
 *
 * <p>Rows keys are serialized via the marshaller and produce leading columns:
 * <ol>
 *   <li>{@code "Old Prefix"}: The prefix (RangeSpecification) for the original ranges in a row
 *   (e.g. "44123").
 *   <li>{@code "Old Length"}: The length for the original ranges in a row (e.g. "9", "8" or "5").
 * </ol>
 */
public class RecipesTableSchema {

  /** The format of the original numbers in a given range. */
  public static final Column<String> OLD_FORMAT = Column.ofString("Old Format");

  /** The new format of the migrated numbers in a given range. */
  public static final Column<String> NEW_FORMAT = Column.ofString("New Format");

  /** The BCP-47 country code the given recipe belongs to. */
  public static final Column<DigitSequence> COUNTRY_CODE =
      Column.create(DigitSequence.class, "Country Code", DigitSequence.empty(), DigitSequence::of);

  /** Indicates whether a given recipe will result in a valid, dialable range */
  public static final Column<Boolean> IS_FINAL_MIGRATION = Column.ofBoolean("Is Final Migration");

  /** The explanation of a migration recipe in words. */
  public static final Column<String> DESCRIPTION = Column.ofString("Description");

  /** Marshaller for constructing CsvTable from RangeTable. */
  private static final CsvKeyMarshaller<RangeKey> MARSHALLER = new CsvKeyMarshaller<>(
      RangesTableSchema::write,
      // uses a read method that will only allow a single numerical value in the 'Old Length' column
      RecipesTableSchema::read,
      Optional.of(RangeKey.ORDERING),
      "Old Prefix",
      "Old Length");

  /**
   * Instantiates a {@link RangeKey} from the prefix and length columns of a given recipe
   * row.
   *
   * @throws IllegalArgumentException when the 'Old Length' value is anything other than a number
   */
  public static RangeKey read(List<String> parts) {
    Set<Integer> rangeKeyLength;

    try {
      rangeKeyLength = Collections.singleton(Integer.parseInt(parts.get(1)));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number '" + parts.get(1) + "' in column 'Old Length'");
    }
    return RangeKey.create(RangeSpecification.parse(parts.get(0)), rangeKeyLength);
  }

  /** The columns for the serialized CSV table. */
  private static final Schema CSV_COLUMNS =
      Schema.builder()
          .add(COUNTRY_CODE)
          .add(OLD_FORMAT)
          .add(NEW_FORMAT)
          .add(IS_FINAL_MIGRATION)
          .add(DESCRIPTION)
          .build();

  /** Schema instance defining the ranges CSV table. */
  public static final CsvSchema<RangeKey> SCHEMA = CsvSchema.of(MARSHALLER, CSV_COLUMNS);

  /** The non-key columns of a range table. */
  private static final Schema RANGE_COLUMNS =
      Schema.builder()
          .add(COUNTRY_CODE)
          .add(OLD_FORMAT)
          .add(NEW_FORMAT)
          .add(IS_FINAL_MIGRATION)
          .add(DESCRIPTION)
          .build();

  /**
   * Converts a {@link RangeKey} based {@link CsvTable} to a {@link RangeTable}, preserving the
   * original table columns. The {@link CsvSchema} of the returned table is not guaranteed to be
   * the {@link #SCHEMA} instance if the given table had different columns.
   */
  public static RangeTable toRangeTable(CsvTable<RangeKey> csv) {
    RangeTable.Builder out = RangeTable.builder(RANGE_COLUMNS);
    for (RangeKey k : csv.getKeys()) {
      Change.Builder change = Change.builder(k.asRangeTree());
      csv.getRow(k).forEach(change::assign);
      out.apply(change.build(), OverwriteMode.NEVER);
    }
    return out.build();
  }

  /**
   * Converts a {@link RangeTable} to a {@link CsvTable}, using {@link RangeKey}s as row keys and
   * preserving the original table columns. The {@link CsvSchema} of the returned table is not
   * guaranteed to be the {@link #SCHEMA} instance if the given table had different columns.
   */
  @SuppressWarnings("unchecked")
  public static CsvTable<RangeKey> toCsv(RangeTable table) {
    CsvTable.Builder<RangeKey> csv = CsvTable.builder(SCHEMA);
    for (Change c : table.toChanges()) {
      for (RangeKey k : RangeKey.decompose(c.getRanges())) {
        c.getAssignments().forEach(a -> csv.put(k, a));
      }
    }
    return csv.build();
  }

  /** Converts recipe into format more human-friendly than the default ImmutableMap toString(). */
  public static String formatRecipe(ImmutableMap<Column<?>, Object> recipe) {
    StringBuilder formattedRecipe = new StringBuilder();
    for (Column<?> column : recipe.keySet()) {
      String columnValue = column.getName() + ": " + recipe.get(column) + "  |  ";
      formattedRecipe.append(columnValue);
    }
    return formattedRecipe.toString();
  }
}
