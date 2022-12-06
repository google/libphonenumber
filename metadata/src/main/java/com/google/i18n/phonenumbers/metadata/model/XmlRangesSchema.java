/*
 * Copyright (C) 2017 The Libphonenumber Authors.
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
package com.google.i18n.phonenumbers.metadata.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.i18n.phonenumbers.metadata.model.MetadataException.checkMetadata;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.UNKNOWN;
import static com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode.NEVER;

import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.ExtTariff;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.ExtType;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.ColumnGroup;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.util.Optional;

/**
 * A schema describing the columns which are required for creating a {@link NumberingScheme}.
 * <ol>
 *   <li>{@link #TYPE}: The semantic type of numbers in a range (note that this is not the same as
 *       an {@code XmlNumberType}). All ranges should be assigned a validation type.
 *   <li>{@link #AREA_CODE_LENGTH}: The length of an optional prefix which may be removed from
 *       numbers in a range for local dialling. Local only lengths are derived using this column.
 *   <li>{@link #NATIONAL_ONLY}: True if numbers in a range cannot be dialled from outside its
 *       region. The "noInternationalDialling" ranges are derived from this column.
 *   <li>{@link #REGIONS}: A group of boolean columns in the form "Region:XX", where ranges are
 *       set {@code true} that range is valid within the region {@code XX}.
 * </ol>
 *
 * <p>This schema is sufficient for generating {@link NumberingScheme} instances, but isn't what we
 * expect to import data from (which is why it doesn't have a {@code CsvKeyMarshaller} associated
 * with it. That's covered by the {@code RangesTableSchema}.
 */
public final class XmlRangesSchema {
  /**
   * The internal "Type" column in the range table This is present in the schema and used is a lot
   * of places, but it is not what the type/tariff data is imported as (it's derived from other
   * columns).
   */
  public static final Column<ValidNumberType> TYPE =
      Column.of(ValidNumberType.class, "Type", UNKNOWN);

  /**
   * The "Area Code Length" column in the range table, denoting the length of a prefix which can
   * be removed from all numbers in a range to obtain locally diallable numbers. If an
   * "area code" is not optional for dialling, then no value should be set here.
   */
  public static final Column<Integer> AREA_CODE_LENGTH = RangesTableSchema.AREA_CODE_LENGTH;

  /** Denotes ranges which cannot be dialled internationally. */
  public static final Column<Boolean> NATIONAL_ONLY = RangesTableSchema.NATIONAL_ONLY;

  /** Format specifier IDs. */
  public static final Column<String> FORMAT = RangesTableSchema.FORMAT;

  /** The "Region:XX" column group in the range table. */
  public static final ColumnGroup<PhoneRegion, Boolean> REGIONS = RangesTableSchema.REGIONS;

  /** The standard columns required for generating a {@link NumberingScheme}. */
  public static final Schema COLUMNS =
      Schema.builder()
          .add(TYPE)
          .add(AREA_CODE_LENGTH)
          .add(NATIONAL_ONLY)
          .add(FORMAT)
          .add(REGIONS)
          .build();

  /** Columns for per-region tables (just {@link #COLUMNS} without {@link #REGIONS}). */
  public static final Schema PER_REGION_COLUMNS =
      Schema.builder()
          .add(TYPE)
          .add(AREA_CODE_LENGTH)
          .add(NATIONAL_ONLY)
          .add(FORMAT)
          .build();

  public static RangeTable fromExternalTable(RangeTable src) {
    checkArgument(RangesTableSchema.TABLE_COLUMNS.isSubSchemaOf(src.getSchema()),
        "unexpected schema for source table, should be subschema of %s",
        RangesTableSchema.TABLE_COLUMNS);
    RangeTree unknown = src.getRanges(RangesTableSchema.TYPE, ExtType.UNKNOWN);
    checkMetadata(unknown.isEmpty(), "source table contains unknown type for ranges\n%s", unknown);
    checkSourceColumn(src, RangesTableSchema.TYPE);
    checkSourceColumn(src, RangesTableSchema.TARIFF);

    // We can copy most columns verbatim.
    RangeTable.Builder dst = RangeTable.builder(COLUMNS);
    copyColumn(src, dst, AREA_CODE_LENGTH);
    copyColumn(src, dst, NATIONAL_ONLY);
    copyColumn(src, dst, FORMAT);
    REGIONS.extractGroupColumns(src.getColumns()).values().forEach(c -> copyColumn(src, dst, c));

    // But the type column must be inferred from a combination of the external type and tariff.
    // Tariff takes precedence, so we do type first and then overwrite ranges for tariff.
    // We also capture unsupported ranges as they must be ignored in this conversion.
    RangeTree unsupportedRanges = RangeTree.empty();
    for (ExtType extType : src.getAssignedValues(RangesTableSchema.TYPE)) {
      RangeTree ranges = src.getRanges(RangesTableSchema.TYPE, extType);
      Optional<ValidNumberType> t = extType.toValidNumberType();
      if (t.isPresent()) {
        dst.assign(TYPE, t.get(), ranges, OverwriteMode.NEVER);
      } else {
        unsupportedRanges = unsupportedRanges.union(ranges);
      }
    }
    // Because we know that both the type and tariff columns have assignments for every range (and
    // there's no "unknown" values for these) we can just ignore "standard rate" tariff ranges
    // since they must have had a type assigned above already.
    for (ExtTariff extTariff : src.getAssignedValues(RangesTableSchema.TARIFF)) {
      // Ignore unsupported ranges here (since otherwise they could add ranges based only on the
      // tariff, which would be wrong). For example, a toll free ISP number range should NOT be
      // in the table as TOLL_FREE, since ISP numbers should not be in the table at all (until
      // such time as they are a fully supported type).
      RangeTree ranges =
          src.getRanges(RangesTableSchema.TARIFF, extTariff).subtract(unsupportedRanges);
      extTariff.toValidNumberType()
          .ifPresent(t -> dst.assign(TYPE, t, ranges, OverwriteMode.ALWAYS));
    }
    return dst.build();
  }

  private static void checkSourceColumn(RangeTable table, Column<?> col) {
    checkMetadata(table.getAssignedRanges(col).equals(table.getAllRanges()),
        "table is missing assignments in column %s for ranges\n%s",
        col, table.getAllRanges().subtract(table.getAssignedRanges(col)));
  }

  private static void copyColumn(RangeTable src, RangeTable.Builder dst, Column<?> col) {
    if (src.getColumns().contains(col)) {
      src.getAssignedValues(col).forEach(v -> dst.assign(col, v, src.getRanges(col, v), NEVER));
    }
  }

  private XmlRangesSchema() {}
}
