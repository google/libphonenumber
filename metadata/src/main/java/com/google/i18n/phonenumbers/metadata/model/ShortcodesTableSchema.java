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
import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static com.google.i18n.phonenumbers.metadata.model.ShortcodesTableSchema.ShortcodeType.EMERGENCY;
import static com.google.i18n.phonenumbers.metadata.model.ShortcodesTableSchema.ShortcodeType.EXPANDED_EMERGENCY;
import static java.util.function.Function.identity;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.ExtTariff;
import com.google.i18n.phonenumbers.metadata.proto.Enums.Provenance;
import com.google.i18n.phonenumbers.metadata.proto.Types.XmlShortcodeType;
import com.google.i18n.phonenumbers.metadata.table.Change;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvKeyMarshaller;
import com.google.i18n.phonenumbers.metadata.table.CsvSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The schema of the standard "Shortcodes" table with rows keyed by {@link RangeKey} and columns:
 * <ol>
 *   <li>{@link #TYPE}: The semantic type of numbers in a range. All ranges should be assigned a
 *       type.
 *   <li>{@link #TARIFF}: The expected cost of numbers in a range. All ranges should be assigned a
 *       tariff.
 *   <li>{@link #SMS}: True if numbers in a range are expected to support SMS.
 *   <li>{@link #SUBREGION}: True if numbers in a range are expected to be only diallable from a
 *       geographic subregion (rather than the whole region).
 *   <li>{@link #PROVENANCE}: Indicates the most important reason for a range to be valid.
 *   <li>{@link #COMMENT}: Free text field usually containing evidence related to the provenance.
 * </ol>
 *
 * <p>Rows keys are serialized via the marshaller and produce leading columns:
 * <ol>
 *   <li>{@code Region}: The region code for which this range applies.
 *   <li>{@code Prefix}: The prefix (RangeSpecification) for the ranges in a row (e.g. "12[3-6]").
 *   <li>{@code Length}: A set of lengths for the ranges in a row (e.g. "9", "8,9" or "5,7-9").
 * </ol>
 *
 * <p>Note that the region must be part of the key, since some shortcodes have different types
 * between different regions.
 */
public final class ShortcodesTableSchema {
  /**
   * The row key of the shortcode table, specifying region and range key. This permits all
   * shortcodes to be stored in a single table (which is very helpful in NANPA, where there are
   * many regions, most with only a tiny amount of shortcode information).
   */
  @AutoValue
  public abstract static class ShortcodeKey {
    private static final Comparator<ShortcodeKey> ORDERING = Comparator
        .comparing(ShortcodeKey::getRegion)
        .thenComparing(ShortcodeKey::getRangeKey, RangeKey.ORDERING);

    private static final CsvKeyMarshaller<ShortcodeKey> MARSHALLER = new CsvKeyMarshaller<>(
        ShortcodeKey::write,
        ShortcodeKey::read,
        Optional.of(ShortcodeKey.ORDERING),
        "Region",
        "Prefix",
        "Length");

    private static Stream<String> write(ShortcodeKey key) {
      return Stream.concat(
          Stream.of(key.getRegion().toString()),
          RangesTableSchema.write(key.getRangeKey()));
    }

    private static ShortcodeKey read(List<String> parts) {
      return ShortcodeKey.create(
          PhoneRegion.of(parts.get(0)),
          RangesTableSchema.read(parts.subList(1, parts.size())));
    }

    public static ShortcodeKey create(PhoneRegion region, RangeKey rangeKey) {
      checkArgument(!region.equals(PhoneRegion.getUnknown()), "region must be valid");
      return new AutoValue_ShortcodesTableSchema_ShortcodeKey(region, rangeKey);
    }

    public abstract PhoneRegion getRegion();
    public abstract RangeKey getRangeKey();
  }

  /** Shortcode type enum. */
  public enum ShortcodeType {
    /** Default value not permitted in real data. */
    UNKNOWN,

    /**
     * General purpose non-governmental services including commercial or charity services. This is
     * the default type for shortcodes if no other category is more applicable.
     */
    COMMERCIAL,
    /**
     * Non-emergency, government run public services (e.g. directory enquiries).
     */
    PUBLIC_SERVICE,
    /**
     * Public services which provide important non-emergency information for health or safety
     * (e.g. https://www.police.uk/contact/101/).
     */
    EXPANDED_EMERGENCY,
    /**
     * Primary public emergency numbers (i.e. police, fire or ambulance) which are available to
     * everyone. Numbers in this category must be toll-free and not carrier specific. Mobile phone
     * manufacturers will often allow these numbers to be dialled from a locked device, so it's
     * important that they work for everyone.
     */
    EMERGENCY;
  }

  private static final ImmutableBiMap<ExtTariff, XmlShortcodeType> XML_TARIFF_MAP =
      Stream.of(ExtTariff.TOLL_FREE, ExtTariff.STANDARD_RATE, ExtTariff.PREMIUM_RATE)
          .collect(toImmutableBiMap(identity(), v -> XmlShortcodeType.valueOf("SC_" + v.name())));

  private static final ImmutableBiMap<ShortcodeType, XmlShortcodeType> XML_TYPE_MAP =
      Stream.of(EXPANDED_EMERGENCY, EMERGENCY)
          .collect(toImmutableBiMap(identity(), v -> XmlShortcodeType.valueOf("SC_" + v.name())));

  /** Return the known mapping from the schema shortcode types to the XML type. */
  public static Optional<XmlShortcodeType> getXmlType(ShortcodeType type) {
    return Optional.ofNullable(XML_TYPE_MAP.get(type));
  }

  /** Return the mapping from the schema tariff to the XML type. */
  public static XmlShortcodeType getXmlType(ExtTariff tariff) {
    XmlShortcodeType xmlType = XML_TARIFF_MAP.get(tariff);
    checkArgument(xmlType != null, "shortcodes do not support tariff: %s", tariff);
    return xmlType;
  }

  public static final Column<ShortcodeType> TYPE =
      Column.of(ShortcodeType.class, "Type", ShortcodeType.UNKNOWN);

  public static final Column<ExtTariff> TARIFF = RangesTableSchema.TARIFF;
  public static final Column<Boolean> SMS = RangesTableSchema.SMS;
  public static final Column<Boolean> CARRIER_SPECIFIC = Column.ofBoolean("Carrier Specific");
  public static final Column<Boolean> SUBREGION = Column.ofBoolean("Subregion");
  public static final Column<String> FORMAT = RangesTableSchema.FORMAT;
  public static final Column<Provenance> PROVENANCE = RangesTableSchema.PROVENANCE;
  public static final Column<String> COMMENT = RangesTableSchema.COMMENT;

  private static final Schema COLUMNS =
      Schema.builder()
          .add(TYPE)
          .add(TARIFF)
          .add(SMS)
          .add(CARRIER_SPECIFIC)
          .add(SUBREGION)
          .add(FORMAT)
          .add(PROVENANCE)
          .add(COMMENT)
          .build();

  /** Schema instance defining the "Shortcodes" CSV table. */
  public static final CsvSchema<ShortcodeKey> SCHEMA =
      CsvSchema.of(ShortcodeKey.MARSHALLER, COLUMNS);

  /**
   */
  public static CsvTable<ShortcodeKey> toCsv(Map<PhoneRegion, RangeTable> tables) {
    CsvTable.Builder<ShortcodeKey> csv = CsvTable.builder(SCHEMA);
    tables.forEach((r, t) -> {
      for (Change c : t.toChanges()) {
        for (RangeKey k : RangeKey.decompose(c.getRanges())) {
          csv.put(ShortcodeKey.create(r, k), c.getAssignments());
        }
      }
    });
    return csv.build();
  }

  /**
   * Maps a single shortcode CSV table into a map of region specific range tables. Note that the
   * ranges in these tables do not need to be consistent across regions (e.g. "toll free" in one
   * might be "premium rate" in the other).
   */
  public static ImmutableSortedMap<PhoneRegion, RangeTable> toShortcodeTables(
      CsvTable<ShortcodeKey> csv) {
    // Retain order of regions in the CSV table (not natural region order).
    Map<PhoneRegion, RangeTable.Builder> builderMap = new LinkedHashMap<>();
    for (ShortcodeKey k : csv.getKeys()) {
      // Basically the same as for RangesTableSchema, except that we deal with region codes in the
      // key.
      Change.Builder change = Change.builder(k.getRangeKey().asRangeTree());
      csv.getRow(k).forEach(change::assign);
      PhoneRegion region = k.getRegion();
      RangeTable.Builder table = builderMap.get(region);
      if (table == null) {
        table = RangeTable.builder(COLUMNS);
        builderMap.put(region, table);
      }
      table.apply(change.build(), OverwriteMode.NEVER);
    }
    return ImmutableSortedMap.copyOf(Maps.transformValues(builderMap, RangeTable.Builder::build));
  }

  private ShortcodesTableSchema() {}
}
