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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.i18n.phonenumbers.metadata.model.MetadataException.checkMetadata;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.ExamplesTableSchema.ExampleNumberKey;
import com.google.i18n.phonenumbers.metadata.model.MetadataTableSchema.Regions;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment;
import com.google.i18n.phonenumbers.metadata.model.ShortcodesTableSchema.ShortcodeKey;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.CsvTable.DiffMode;
import com.google.i18n.phonenumbers.metadata.table.DiffKey;
import com.google.i18n.phonenumbers.metadata.table.DiffKey.Status;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * All CSV based tables and legacy XML for a single calling code. This is the data from which all
 * legacy data can be reconstructed (metadata XML, carrier/geocode/timezone mappings).
 *
 * <p>This is loaded at once, possibly from multiple files, since conversion to legacy formats
 * often requires more than one of these data structures.
 */
@AutoValue
public abstract class CsvData {
  /** CSV data loading API. */
  public interface CsvDataProvider {
    /** Loads the top-level metadata table which containing data for all supported calling codes.*/
    CsvTable<DigitSequence> loadMetadata() throws IOException;
    /** Loads the CSV data for a single calling code. */
    CsvData loadData(DigitSequence cc) throws IOException;
  }

  /**
   * Creates a single CsvData instance, either directly or from a provider. The given metadata
   * table will have the single row relating to the specified calling code removed.
   */
  public static CsvData create(
      DigitSequence cc,
      CsvTable<DigitSequence> allMetadata,
      CsvTable<RangeKey> ranges,
      CsvTable<ShortcodeKey> shortcodes,
      CsvTable<ExampleNumberKey> examples,
      CsvTable<String> formats,
      ImmutableList<AltFormatSpec> altFormats,
      CsvTable<String> operators,
      ImmutableList<Comment> comments) {
    // Row keys are unique, so we end up with at most 1 row in the filtered table.
    CsvTable<DigitSequence> ccMetadata =
        allMetadata.toBuilder().filterRows(r -> r.equals(cc)).build();
    checkMetadata(!ccMetadata.getKeys().isEmpty(), "no such calling code %s in metadata", cc);
    checkRegions(ccMetadata, ranges, shortcodes);
    checkNoOverlappingRows(ranges);
    checkNoOverlappingShortcodeRows(shortcodes);
    return new AutoValue_CsvData(
        cc, ccMetadata, ranges, shortcodes, examples, formats, altFormats, operators, comments);
  }

  private static void checkNoOverlappingRows(CsvTable<RangeKey> csv) {
    RangeTree allRanges = RangeTree.empty();
    for (RangeKey key : csv.getKeys()) {
      RangeTree ranges = key.asRangeTree();
      checkMetadata(allRanges.intersect(ranges).isEmpty(), "overlapping row in CSV: %s", key);
      allRanges = allRanges.union(ranges);
    }
  }

  private static void checkNoOverlappingShortcodeRows(CsvTable<ShortcodeKey> csv) {
    Map<PhoneRegion, RangeTree> allRangesMap = new HashMap<>();
    for (ShortcodeKey key : csv.getKeys()) {
      RangeTree allRegionRanges = allRangesMap.getOrDefault(key.getRegion(), RangeTree.empty());
      RangeTree ranges = key.getRangeKey().asRangeTree();
      checkMetadata(allRegionRanges.intersect(ranges).isEmpty(), "overlapping row in CSV: %s", key);
      allRangesMap.put(key.getRegion(), allRegionRanges.union(ranges));
    }
  }

  private static void checkRegions(
      CsvTable<DigitSequence> metadata,
      CsvTable<RangeKey> ranges,
      CsvTable<ShortcodeKey> shortcodes) {
    DigitSequence cc = Iterables.getOnlyElement(metadata.getKeys());
    PhoneRegion mainRegion = metadata.getOrDefault(cc, MetadataTableSchema.MAIN_REGION);
    Regions extraRegions = metadata.getOrDefault(cc, MetadataTableSchema.EXTRA_REGIONS);

    ImmutableSet<PhoneRegion> csvRegions = ranges
        .getValues(RangesTableSchema.CSV_REGIONS).stream()
        .flatMap(r -> r.getValues().stream())
        .collect(toImmutableSet());
    if (extraRegions.getValues().isEmpty()) {
      checkMetadata(csvRegions.size() == 1 && csvRegions.contains(mainRegion),
          "inconsistent regions:\nmetadata: %s\nranges table: %s", mainRegion, csvRegions);
    } else {
      checkMetadata(!extraRegions.getValues().contains(mainRegion),
          "invalid metadata: main region is duplicated in 'extra regions' column");
      checkMetadata(
          csvRegions.contains(mainRegion)
              && csvRegions.containsAll(extraRegions.getValues())
              && csvRegions.size() == extraRegions.getValues().size() + 1,
          "inconsistent regions:\nmetadata: %s + %s\nranges table: %s",
          mainRegion, extraRegions, csvRegions);
    }
    ImmutableSet<PhoneRegion> shortcodeRegions =
        shortcodes.getKeys().stream().map(ShortcodeKey::getRegion).collect(toImmutableSet());
    checkMetadata(csvRegions.containsAll(shortcodeRegions),
        "unexpected regions for shortcodes:\nmetadata: %s\nshortcode regions: %s",
        csvRegions, shortcodeRegions);
  }

  /** The difference between two CSV snapshots captured as a set of CVS tables. */
  @AutoValue
  public abstract static class Diff {
    private static <K> Optional<CsvTable<DiffKey<K>>> diff(CsvTable<K> lhs, CsvTable<K> rhs) {
      CsvTable<DiffKey<K>> diff = CsvTable.diff(lhs, rhs, DiffMode.CHANGES);
      if (diff.getKeys().stream().anyMatch(k -> k.getStatus() != Status.UNCHANGED)) {
        return Optional.of(diff);
      }
      return Optional.empty();
    }

    // Visible for AutoValue
    Diff() {}

    /** Returns the contextualized diff of the ranges table. */
    public abstract Optional<CsvTable<DiffKey<RangeKey>>> rangesDiff();
    /** Returns the contextualized diff of the shortcodes table. */
    public abstract Optional<CsvTable<DiffKey<ShortcodeKey>>> shortcodesDiff();
    /** Returns the contextualized diff of the examples table. */
    public abstract Optional<CsvTable<DiffKey<ExampleNumberKey>>> examplesDiff();
    /** Returns the contextualized diff of the formats table. */
    public abstract Optional<CsvTable<DiffKey<String>>> formatsDiff();
    /** Returns the contextualized diff of the operators table. */
    public abstract Optional<CsvTable<DiffKey<String>>> operatorsDiff();
  }

  /** Creates the diff between two CSV data snapshots. */
  public static Diff diff(CsvData before, CsvData after) {
    // TODO: Add diffing for comments and/or alternate formats.
    return new AutoValue_CsvData_Diff(
        Diff.diff(before.getRanges(), after.getRanges()),
        Diff.diff(before.getShortcodes(), after.getShortcodes()),
        Diff.diff(before.getExamples(), after.getExamples()),
        Diff.diff(before.getFormats(), after.getFormats()),
        Diff.diff(before.getOperators(), after.getOperators()));
  }

  // Visible for AutoValue
  CsvData() {}

  /** Returns the calling code for this CSV data. */
  public abstract DigitSequence getCallingCode();
  /**
   * Returns the single row of the metadata table for the calling code (see
   * {@code MetadataTableSchema}).
   */
  public abstract CsvTable<DigitSequence> getMetadata();
  /** Returns the ranges table for the calling code (see {@code RangesTableSchema}) */
  public abstract CsvTable<RangeKey> getRanges();
  /** Returns the shortcode table for the calling code (see {@code ShortcodesTableSchema}) */
  public abstract CsvTable<ShortcodeKey> getShortcodes();
  /** Returns the examples table for the calling code (see {@code ExamplesTableSchema}). */
  public abstract CsvTable<ExampleNumberKey> getExamples();
  /** Returns the format table for the calling code (see {@code FormatsTableSchema}). */
  public abstract CsvTable<String> getFormats();
  /**
   * Returns the alternate format table for the calling code (see {@code AltFormatsTableSchema}).
   */
  public abstract ImmutableList<AltFormatSpec> getAltFormats();
  /** Returns the operator table for the calling code (see {@code OperatorsTableSchema}). */
  public abstract CsvTable<String> getOperators();
  /** Returns the set of comments for the calling code. */
  public abstract ImmutableList<Comment> getComments();

  @Memoized
  public RangeTable getRangesAsTable() {
    return RangesTableSchema.toRangeTable(getRanges());
  }

  @Memoized
  public ImmutableSortedMap<PhoneRegion, RangeTable> getShortcodesAsTables() {
    return ShortcodesTableSchema.toShortcodeTables(getShortcodes());
  }

  @Memoized
  public ImmutableTable<PhoneRegion, ValidNumberType, DigitSequence> getExamplesAsTable() {
    return ExamplesTableSchema.toExampleTable(getExamples());
  }

  /** Canonicalizes range tables in the CSV data. This is potentially slow for large regions. */
  // TODO: Is there any way to reliably detect canonical CSV for sub-regions?
  public final CsvData canonicalizeRangeTables() {
    CsvTable<RangeKey> ranges = RangesTableSchema.toCsv(getRangesAsTable());
    CsvTable<ShortcodeKey> shortcodes = ShortcodesTableSchema.toCsv(getShortcodesAsTables());
    return create(
        getCallingCode(),
        getMetadata(),
        ranges,
        shortcodes,
        getExamples(),
        getFormats(),
        getAltFormats(),
        getOperators(),
        getComments()
    );
  }
}
