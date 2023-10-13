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
import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;
import com.google.i18n.phonenumbers.metadata.LengthsParser;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.i18n.SimpleLanguageTag;
import com.google.i18n.phonenumbers.metadata.model.MetadataTableSchema.Regions;
import com.google.i18n.phonenumbers.metadata.proto.Enums.Provenance;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.table.Change;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.ColumnGroup;
import com.google.i18n.phonenumbers.metadata.table.CsvKeyMarshaller;
import com.google.i18n.phonenumbers.metadata.table.CsvSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.MultiValue;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * The schema of the standard "Ranges" table with rows keyed by {@link RangeKey} and columns:
 *
 * <ol>
 *   <li>{@link #TYPE}: The semantic type of numbers in a range (note that this is not the same a
 *       XmlNumberType or ValidNumberType). All ranges should be assigned a type.
 *   <li>{@link #TARIFF}: The expected cost of numbers in a range (combining TYPE and TARIFF can
 *       yield the internal ValidNumberType). All ranges should be assigned a tariff.
 *   <li>{@link #AREA_CODE_LENGTH}: The length of an optional prefix which may be removed from
 *       numbers in a range for local dialling. Local only lengths are derived using this column.
 *   <li>{@link #NATIONAL_ONLY}: True if numbers in a range cannot be dialled from outside its
 *       region. The "noInternationalDialling" ranges are derived from this column.
 *   <li>{@link #SMS}: True if numbers in a range are expected to support SMS.
 *   <li>{@link #OPERATOR}: The expected operator (carrier) ID for a range (or empty if no carrier
 *       is known).
 *   <li>{@link #FORMAT}: The expected format ID for a range (or empty if no formatting should be
 *       applied).
 *   <li>{@link #TIMEZONE}: The timezone names for a range (or empty to imply the default
 *       timezones). Multiple timezones can be specific if separated by {@code '&'}.
 *   <li>{@link #REGIONS}: A group of boolean columns in the form "Region:XX", where ranges are set
 *       {@code true} that range is valid within the region {@code XX}.
 *   <li>{@link #GEOCODES}: A group of String columns in the form "Geocode:XXX" containing the
 *       geocode string for a range, where {@code XXX} is the language code of the string.
 *   <li>{@link #PROVENANCE}: Indicates the most important reason for a range to be valid.
 *   <li>{@link #COMMENT}: Free text field usually containing evidence related to the provenance.
 * </ol>
 *
 * <p>Rows keys are serialized via the marshaller and produce leading columns:
 *
 * <ol>
 *   <li>{@code Prefix}: The prefix (RangeSpecification) for the ranges in a row (e.g. "12[3-6]").
 *   <li>{@code Length}: A set of lengths for the ranges in a row (e.g. "9", "8,9" or "5,7-9").
 * </ol>
 */
public final class RangesTableSchema {
  /**
   * External number type enum. This is technically much better than ValidNumberType since it splits
   * type and cost properly. Unfortunately the internal logic of the phonenumber library doesn't
   * really cope with this, which is why we convert to {@code XmlRangesSchema} before creating
   * legacy data structures.
   *
   * <p>This enum can be modified as new types are requested from data providers, providing the type
   * mapping to ValidNumberType is updated appropriately. Note that until it's clear that mapping
   * types such as {@link #M2M} to {@link ValidNumberType#UNKNOWN} will work okay, we should be very
   * careful about using the additional types. Additional types need to be removed before the
   * generated table can be turned into a {@link NumberingScheme}.
   */
  public enum ExtType {
    /** Default value not permitted in real data. */
    UNKNOWN,
    /** Maps to {@link ValidNumberType#FIXED_LINE}. */
    FIXED_LINE,
    /** Maps to {@link ValidNumberType#MOBILE}. */
    MOBILE,
    /** Maps to {@link ValidNumberType#FIXED_LINE_OR_MOBILE}. */
    FIXED_LINE_OR_MOBILE,
    /** Maps to {@link ValidNumberType#VOIP}. */
    VOIP,
    /** Maps to {@link ValidNumberType#PAGER}. */
    PAGER,
    /** Maps to {@link ValidNumberType#PERSONAL_NUMBER}. */
    PERSONAL_NUMBER,
    /** Maps to {@link ValidNumberType#UAN}. */
    UAN,
    /** Maps to {@link ValidNumberType#VOICEMAIL}. */
    VOICEMAIL,
    /** Machine-to-machine numbers (additional type for future support). */
    M2M,
    /** ISP dial-up numbers (additional type for future support). */
    ISP;

    private static final ImmutableMap<ExtType, ValidNumberType> TYPE_MAP =
        Stream.of(
                ExtType.FIXED_LINE,
                ExtType.MOBILE,
                ExtType.FIXED_LINE_OR_MOBILE,
                ExtType.PAGER,
                ExtType.PERSONAL_NUMBER,
                ExtType.UAN,
                ExtType.VOICEMAIL,
                ExtType.VOIP)
            .collect(toImmutableMap(identity(), v -> ValidNumberType.valueOf(v.name())));

    public Optional<ValidNumberType> toValidNumberType() {
      return Optional.ofNullable(TYPE_MAP.get(this));
    }
  }

  /**
   * External tariff enum. By splitting tariff information out from the "line type", we can
   * represent a much wider (and more realistic) set of combinations for number ranges. When
   * combined with {@link ExtType}, this maps back to {@code ValidNumberType}.
   */
  public enum ExtTariff {
    /** Does not affect ValidNumberType mapping. */
    STANDARD_RATE,
    /** Maps to {@link ValidNumberType#TOLL_FREE}. */
    TOLL_FREE,
    /** Maps to {@link ValidNumberType#SHARED_COST}. */
    SHARED_COST,
    /** Maps to {@link ValidNumberType#PREMIUM_RATE}. */
    PREMIUM_RATE;

    private static final ImmutableMap<ExtTariff, ValidNumberType> TARIFF_MAP =
        Stream.of(ExtTariff.TOLL_FREE, ExtTariff.SHARED_COST, ExtTariff.PREMIUM_RATE)
            .collect(toImmutableMap(identity(), v -> ValidNumberType.valueOf(v.name())));

    public Optional<ValidNumberType> toValidNumberType() {
      return Optional.ofNullable(TARIFF_MAP.get(this));
    }
  }

  /** The value in the "TIMEZONE" column, which is effectively a list of timezone strings. */
  public static final class Timezones extends MultiValue<ZoneId, Timezones> {
    public static Column<Timezones> column(String name) {
      return Column.create(Timezones.class, name, new Timezones(""), Timezones::new);
    }

    public Timezones(Iterable<ZoneId> ids) {
      super(ids, '&', comparing(ZoneId::getId), true);
    }

    public Timezones(String s) {
      super(s, ZoneId::of, '&', comparing(ZoneId::getId), true);
    }
  }

  public static final Column<ExtType> TYPE = Column.of(ExtType.class, "Type", ExtType.UNKNOWN);

  public static final Column<ExtTariff> TARIFF =
      Column.of(ExtTariff.class, "Tariff", ExtTariff.STANDARD_RATE);

  /**
   * The "Area Code Length" column in the range table, denoting the length of a prefix which can be
   * removed from all numbers in a range to obtain locally diallable numbers. If an "area code" is
   * not optional for dialling, then no value should be set here.
   */
  public static final Column<Integer> AREA_CODE_LENGTH =
      Column.ofUnsignedInteger("Area Code Length");

  /** Denotes ranges which cannot be dialled internationally. */
  public static final Column<Boolean> NATIONAL_ONLY = Column.ofBoolean("National Only");

  /** Denotes ranges which can reasonably be expected to receive SMS. */
  public static final Column<Boolean> SMS = Column.ofBoolean("Sms");

  /** The ID of the primary/original operator assigned to a range. */
  public static final Column<String> OPERATOR = Column.ofString("Operator");

  /** The ID of the format assigned to a range. */
  public static final Column<String> FORMAT = Column.ofString("Format");

  /** An '&amp;'-separated list of timezone IDs associated with this range. */
  public static final Column<Timezones> TIMEZONE = Timezones.column("Timezone");

  /** The "Region:XX" column group in the range table. */
  public static final ColumnGroup<PhoneRegion, Boolean> REGIONS =
      ColumnGroup.byRegion(Column.ofBoolean("Region"));

  /** The "Regions" column in the CSV table. */
  public static final Column<Regions> CSV_REGIONS = Regions.column("Regions");

  /** The "Geocode:XXX" column group in the range table. */
  public static final ColumnGroup<SimpleLanguageTag, String> GEOCODES =
      ColumnGroup.byLanguage(Column.ofString("Geocode"));

  /** The provenance column indicating why a range is considered valid. */
  public static final Column<Provenance> PROVENANCE =
      Column.of(Provenance.class, "Provenance", Provenance.UNKNOWN);

  /** An arbitrary text comment, usually (at least) supplying information about the provenance. */
  public static final Column<String> COMMENT = Column.ofString("Comment");

  /** Marshaller for constructing CsvTable from RangeTable. */
  private static final CsvKeyMarshaller<RangeKey> MARSHALLER =
      new CsvKeyMarshaller<>(
          RangesTableSchema::write,
          RangesTableSchema::read,
          Optional.of(RangeKey.ORDERING),
          "Prefix",
          "Length");

  /** The non-key columns of a range table. */
  public static final Schema TABLE_COLUMNS =
      Schema.builder()
          .add(TYPE)
          .add(TARIFF)
          .add(AREA_CODE_LENGTH)
          .add(NATIONAL_ONLY)
          .add(SMS)
          .add(OPERATOR)
          .add(FORMAT)
          .add(TIMEZONE)
          .add(REGIONS)
          .add(GEOCODES)
          .add(PROVENANCE)
          .add(COMMENT)
          .build();

  /**
   * The columns for the serialized CSV table. Note that the "REGIONS" column group is replaced by
   * the CSV regions multi-value. This allows region codes to be serialize in a single column (which
   * is far nicer when looking at data in a spreadsheet). In the range table, this is normalized
   * into the boolean column group (because that's far nicer to work with).
   */
  private static final Schema CSV_COLUMNS =
      Schema.builder()
          .add(TYPE)
          .add(TARIFF)
          .add(AREA_CODE_LENGTH)
          .add(NATIONAL_ONLY)
          .add(SMS)
          .add(OPERATOR)
          .add(FORMAT)
          .add(TIMEZONE)
          .add(CSV_REGIONS)
          .add(GEOCODES)
          .add(PROVENANCE)
          .add(COMMENT)
          .build();

  /** Schema instance defining the ranges CSV table. */
  public static final CsvSchema<RangeKey> SCHEMA = CsvSchema.of(MARSHALLER, CSV_COLUMNS);

  /**
   * Converts a {@link RangeTable} to a {@link CsvTable}, using {@link RangeKey}s as row keys and
   * preserving the original table columns. The {@link CsvSchema} of the returned table is not
   * guaranteed to be the {@link #SCHEMA} instance if the given table had different columns.
   */
  @SuppressWarnings("unchecked")
  public static CsvTable<RangeKey> toCsv(RangeTable table) {
    CsvTable.Builder<RangeKey> csv = CsvTable.builder(SCHEMA);
    ImmutableSet<Column<Boolean>> regionColumns =
        REGIONS.extractGroupColumns(table.getColumns()).values();
    TreeSet<PhoneRegion> regions = new TreeSet<>();
    for (Change c : table.toChanges()) {
      for (RangeKey k : RangeKey.decompose(c.getRanges())) {
        regions.clear();
        c.getAssignments()
            .forEach(
                a -> {
                  // We special case the regions column, converting a group of boolean columns into
                  // a
                  // multi-value of region codes. If the column is in the group, it must hold
                  // Booleans.
                  if (regionColumns.contains(a.column())) {
                    if (a.value().map(((Column<Boolean>) a.column())::cast).orElse(Boolean.FALSE)) {
                      regions.add(REGIONS.getKey(a.column()));
                    }
                  } else {
                    csv.put(k, a);
                  }
                });
        // We can do this out-of-sequence because the table will order its columns.
        if (!regions.isEmpty()) {
          csv.put(k, CSV_REGIONS, Regions.of(regions));
        }
      }
    }
    return csv.build();
  }

  /**
   * Converts a {@link RangeKey} based {@link CsvTable} to a {@link RangeTable}, preserving the
   * original table columns. The {@link CsvSchema} of the returned table is not guaranteed to be the
   * {@link #SCHEMA} instance if the given table had different columns.
   */
  public static RangeTable toRangeTable(CsvTable<RangeKey> csv) {
    RangeTable.Builder out = RangeTable.builder(TABLE_COLUMNS);
    for (RangeKey k : csv.getKeys()) {
      Change.Builder change = Change.builder(k.asRangeTree());
      csv.getRow(k)
          .forEach(
              (c, v) -> {
                // We special case the regions column, converting a comma separated list of region
                // codes
                // into a series of boolean column assignments.
                if (c.equals(CSV_REGIONS)) {
                  CSV_REGIONS
                      .cast(v)
                      .getValues()
                      .forEach(r -> change.assign(REGIONS.getColumn(r), true));
                } else {
                  change.assign(c, v);
                }
              });
      out.apply(change.build(), OverwriteMode.NEVER);
    }
    return out.build();
  }

  // Shared by ShortcodeTableSchema
  public static Stream<String> write(RangeKey key) {
    return Stream.of(key.getPrefix().toString(), formatLength(key.getLengths()));
  }

  // Shared by ShortcodeTableSchema
  public static RangeKey read(List<String> parts) {
    return RangeKey.create(
        RangeSpecification.parse(parts.get(0)), LengthsParser.parseLengths(parts.get(1)));
  }

  private static String formatLength(ImmutableSortedSet<Integer> lengthSet) {
    checkArgument(!lengthSet.isEmpty());
    ImmutableRangeSet<Integer> r =
        ImmutableRangeSet.unionOf(
            lengthSet.stream()
                .map(n -> Range.singleton(n).canonical(integers()))
                .collect(toImmutableList()));
    return r.asRanges().stream().map(RangesTableSchema::formatRange).collect(joining(","));
  }

  private static String formatRange(Range<Integer> r) {
    ContiguousSet<Integer> s = ContiguousSet.create(r, integers());
    switch (s.size()) {
      case 1:
        return String.valueOf(s.first());
      case 2:
        return s.first() + "," + s.last();
      default:
        return s.first() + "-" + s.last();
    }
  }

  private RangesTableSchema() {}
}
