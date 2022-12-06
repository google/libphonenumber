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
package com.google.i18n.phonenumbers.metadata.testing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.Boolean.TRUE;
import static java.util.function.Function.identity;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.Types;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.i18n.SimpleLanguageTag;
import com.google.i18n.phonenumbers.metadata.model.AltFormatSpec;
import com.google.i18n.phonenumbers.metadata.model.FormatSpec;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Attributes;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.ExtTariff;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.ExtType;
import com.google.i18n.phonenumbers.metadata.model.ShortcodesTableSchema;
import com.google.i18n.phonenumbers.metadata.model.ShortcodesTableSchema.ShortcodeType;
import com.google.i18n.phonenumbers.metadata.model.XmlRangesSchema;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reusable test-only builder for numbering schemes. More methods can be added as necessary to
 * support whatever is needed for testing.
 *
 * <p>Note that the various "modifer" classes returned by methods such as "addRanges()" are
 * designed only as fluent APIs and instances of modifiers should never be assigned to variables
 * and especially not interleaved with other mutations of the range tables.
 */
public final class TestNumberingScheme {
  /**
   * Returns a mutable numbering scheme builder for testing. Since an IDD is always required by
   * NumberingScheme for geographic regions, a default value of "00" is set by default. This can be
   * overridden or reset by {@code setInternationalPrefix{}} and {@code clearInternationalPrefix()}.
   */
  public static TestNumberingScheme forCallingCode(
      String cc, PhoneRegion main, PhoneRegion... others) {
    return new TestNumberingScheme(DigitSequence.of(cc), main, ImmutableSet.copyOf(others));
  }

  private final DigitSequence callingCode;
  private final PhoneRegion mainRegion;
  private final ImmutableSet<PhoneRegion> otherRegions;
  private final ImmutableMap<PhoneRegion, Column<Boolean>> regionMap;

  // See setNationalPrefix() / clearNationalPrefix()
  private final List<DigitSequence> nationalPrefix = new ArrayList<>();

  // See setInternationalPrefix() / clearInternationalPrefix()
  private Optional<DigitSequence> internationalPrefix = Optional.empty();

  // See setCarrierPrefixes()
  private RangeTree carrierPrefixes = RangeTree.empty();

  // Uses the CSV schema (rather than XML) since that handles type/tariff better.
  private final RangeTable.Builder csvRanges = RangeTable.builder(RangesTableSchema.TABLE_COLUMNS);
  private final Map<PhoneRegion, RangeTable.Builder> shortcodes = new HashMap<>();
  private final Map<FormatSpec, String> formats = new LinkedHashMap<>();

  // Alternate formats are largely separate from everything else.
  private ImmutableList<AltFormatSpec> altFormats = ImmutableList.of();

  // Explicit example numbers.
  private final Table<PhoneRegion, ValidNumberType, DigitSequence> examples =
      HashBasedTable.create();

  private final List<Comment> comments = new ArrayList<>();

  private TestNumberingScheme(
      DigitSequence cc, PhoneRegion main, ImmutableSet<PhoneRegion> others) {
    checkArgument(!others.contains(main), "duplicate regions");
    this.callingCode = checkNotNull(cc);
    this.mainRegion = checkNotNull(main);
    this.otherRegions = others;
    this.regionMap = Stream.concat(Stream.of(main), others.stream())
        .collect(toImmutableMap(identity(), RangesTableSchema.REGIONS::getColumn));
    // Set a reasonable IDD default for geographic regions.
    if (!main.equals(PhoneRegion.getWorld())) {
      setInternationalPrefix("00");
    }
  }

  /** Sets the national prefix of this scheme, replacing any previous value. */
  public TestNumberingScheme setNationalPrefix(String prefix) {
    checkArgument(!prefix.isEmpty(), "national prefix must not be empty");
    this.nationalPrefix.clear();
    this.nationalPrefix.add(DigitSequence.of(prefix));
    return this;
  }

  /** Sets the national prefix of this scheme, replacing any previous value. */
  public TestNumberingScheme setNationalPrefixes(String... prefix) {
    List<String> prefixes = Arrays.asList(prefix);
    this.nationalPrefix.clear();
    prefixes.forEach(p -> {
      checkArgument(!p.isEmpty(), "national prefix must not be empty");
      this.nationalPrefix.add(DigitSequence.of(p));
    });
    return this;
  }

  /** Removes the national prefix  */
  public TestNumberingScheme clearNationalPrefix() {
    this.nationalPrefix.clear();
    return this;
  }

  /** Sets the international prefix of this scheme, replacing any previous value. */
  public TestNumberingScheme setInternationalPrefix(String prefix) {
    checkState(!mainRegion.equals(PhoneRegion.getWorld()),
        "[%s] cannot set IDD for non-geographic calling code", callingCode);
    this.internationalPrefix = Optional.of(DigitSequence.of(prefix));
    return this;
  }

  /** Removes the international prefix  */
  public TestNumberingScheme clearInternationalPrefix() {
    this.internationalPrefix = Optional.empty();
    return this;
  }

  /** Sets the national prefix of this scheme, replacing any previous value. */
  public TestNumberingScheme setCarrierPrefixes(String... prefix) {
    this.carrierPrefixes = RangeTree.from(Arrays.stream(prefix).map(RangeSpecification::parse));
    return this;
  }

  /**
   * Adds ranges (which must not already exist) to the scheme. This method returns a fluent API
   * for modifying the newly added ranges.
   */
  public RangeModifier addRanges(ExtType type, ExtTariff tariff, String... specs) {
    return addRanges(type, tariff, rangesOf(specs));
  }

  /**
   * Adds ranges (which must not already exist) to the scheme. This method returns a fluent API
   * for modifying the newly added ranges.
   */
  public RangeModifier addRanges(ExtType type, ExtTariff tariff, RangeTree ranges) {
    RangeTree overlap = csvRanges.getAllRanges().intersect(ranges);
    checkArgument(overlap.isEmpty(), "ranges already added: %s", overlap);
    csvRanges.assign(RangesTableSchema.TYPE, checkNotNull(type), ranges, OverwriteMode.NEVER);
    csvRanges.assign(RangesTableSchema.TARIFF, checkNotNull(tariff), ranges, OverwriteMode.NEVER);
    // Setting all regions here generates "legal" numbering schemes by default.
    regionMap.values().forEach(c -> csvRanges.assign(c, true, ranges, OverwriteMode.NEVER));
    return new RangeModifier(ranges);
  }

  /** Removes ranges (which need not already exist) from the scheme. */
  public void removeRanges(String... specs) {
    removeRanges(rangesOf(specs));
  }

  /** Removes ranges (which need not already exist) from the scheme. */
  public void removeRanges(RangeTree ranges) {
    csvRanges.remove(ranges);
  }

  /** Returns a fluent API for modifying existing ranges (constrained by the given bounds). */
  public RangeModifier forRangesIn(String... specs) {
    return forRangesIn(rangesOf(specs));
  }

  /** Returns a fluent API for modifying existing ranges (constrained by the given bounds). */
  public RangeModifier forRangesIn(RangeTree ranges) {
    return new RangeModifier(ranges.intersect(csvRanges.getAllRanges()));
  }

  /**
   * Adds shortcodes (which must not already exist) to a given region in the scheme. This method
   * returns a fluent API for modifying the newly added shortcodes.
   */
  public ShortcodeModifier addShortcodes(
      PhoneRegion region, ShortcodeType type, ExtTariff tariff, String... specs) {
    return addShortcodes(region, type, tariff, rangesOf(specs));
  }

  /**
   * Adds shortcodes (which must not already exist) to a given region in the scheme. This method
   * returns a fluent API for modifying the newly added shortcodes.
   */
  public ShortcodeModifier addShortcodes(
      PhoneRegion region, ShortcodeType type, ExtTariff tariff, RangeTree ranges) {
    RangeTable.Builder table = shortcodes
        .computeIfAbsent(region, r -> RangeTable.builder(ShortcodesTableSchema.SCHEMA.columns()));
    RangeTree overlap = table.getAllRanges().intersect(ranges);
    checkArgument(overlap.isEmpty(), "ranges already added: %s", overlap);
    table.assign(ShortcodesTableSchema.TYPE, checkNotNull(type), ranges, OverwriteMode.NEVER);
    table.assign(ShortcodesTableSchema.TARIFF, checkNotNull(tariff), ranges, OverwriteMode.NEVER);
    return new ShortcodeModifier(region, ranges);
  }

  /** Returns a fluent API for modifying existing shortcodes (constrained by the given bounds). */
  public ShortcodeModifier forShortcodesIn(PhoneRegion region, String... specs) {
    return forShortcodesIn(region, rangesOf(specs));
  }

  /** Returns a fluent API for modifying existing shortcodes (constrained by the given bounds). */
  public ShortcodeModifier forShortcodesIn(PhoneRegion region, RangeTree ranges) {
    RangeTable.Builder shortcodeTable =
        checkNotNull(shortcodes.get(region), "no shortcodes in region %s", region);
    return new ShortcodeModifier(region, ranges.intersect(shortcodeTable.getAllRanges()));
  }

  public TypeModifier forRangeTypes(PhoneRegion region, ExtType type, ExtTariff tariff) {
    return new TypeModifier(region, type, tariff);
  }

  public TestNumberingScheme setAlternateFormats(List<AltFormatSpec> altFormats) {
    this.altFormats = ImmutableList.copyOf(altFormats);
    return this;
  }

  /** Builds a valid numbering scheme from the current state of this builder. */
  public NumberingScheme build() {
    Attributes attributes = Attributes.create(
        callingCode,
        mainRegion,
        otherRegions,
        ImmutableSet.copyOf(nationalPrefix),
        carrierPrefixes,
        // This is currently simplistic (only 1 value) and could be extended for tests if needed.
        internationalPrefix.map(Object::toString).orElse(""),
        internationalPrefix.map(p -> RangeTree.from(RangeSpecification.from(p)))
            .orElse(RangeTree.empty()),
        "",
        ImmutableSet.of());
    RangeTable xmlTable = XmlRangesSchema.fromExternalTable(csvRanges.build());
    ImmutableMap<PhoneRegion, RangeTable> shortcodeMap =
        shortcodes.entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, e -> e.getValue().build()));
    // Some formats may have been unassigned by modifications to the test scheme. Only copy the
    // formats with keys that exist in the range tables at the time the scheme is built.
    ImmutableSet<String> assignedFormats = Stream.concat(
        xmlTable.getAssignedValues(XmlRangesSchema.FORMAT).stream(),
        shortcodeMap.values().stream()
            .flatMap(t -> t.getAssignedValues(ShortcodesTableSchema.FORMAT).stream()))
        .collect(toImmutableSet());
    ImmutableMap<String, FormatSpec> formatMap = formats.entrySet().stream()
        .filter(e -> assignedFormats.contains(e.getValue()))
        .collect(toImmutableMap(Entry::getValue, Entry::getKey));
    return NumberingScheme.from(
        attributes,
        xmlTable,
        Maps.transformValues(shortcodes, RangeTable.Builder::build),
        formatMap,
        altFormats,
        fillInMissingExampleNumbersFrom(xmlTable, examples),
        comments);
  }

  public TerritoryModifier forTerritory(PhoneRegion region) {
    return new TerritoryModifier(region);
  }

  /** Fluent API for modifying a set of ranges. */
  public final class RangeModifier {
    private final RangeTree ranges;

    private RangeModifier(RangeTree ranges) {
      checkArgument(!ranges.isEmpty(), "cannot modify empty ranges");
      this.ranges = ranges;
    }

    /** Sets the regions in which the ranges are valid. */
    public RangeModifier setRegions(PhoneRegion... regions) {
      ImmutableSet<PhoneRegion> regionsToSet = ImmutableSet.copyOf(regions);
      checkArgument(regionMap.keySet().containsAll(regionsToSet));
      regionMap.forEach((r, c) ->
          csvRanges.assign(c, regionsToSet.contains(r), ranges, OverwriteMode.ALWAYS));
      return this;
    }

    /** Sets ranges to be "national only" dialing. */
    public RangeModifier setNationalOnly(boolean nationalOnly) {
      csvRanges.assign(RangesTableSchema.NATIONAL_ONLY, nationalOnly, ranges, OverwriteMode.ALWAYS);
      return this;
    }

    /** Sets the area code length of the ranges. */
    public RangeModifier setAreaCodeLength(int n) {
      csvRanges.assign(RangesTableSchema.AREA_CODE_LENGTH, n, ranges, OverwriteMode.ALWAYS);
      return this;
    }

    /** Sets the format assigned to the ranges. */
    public RangeModifier setFormat(FormatSpec format) {
      String id =
          formats.computeIfAbsent(format, f -> String.format("__fmt_%02d", formats.size() + 1));
      csvRanges.assign(RangesTableSchema.FORMAT, id, ranges, OverwriteMode.ALWAYS);
      return this;
    }

    public RangeModifier setFormat(String id, FormatSpec format) {
      formats.put(format, id);
      csvRanges.assign(RangesTableSchema.FORMAT, id, ranges, OverwriteMode.ALWAYS);
      return this;
    }

    /** Clears the format assigned to the ranges. */
    public RangeModifier clearFormat() {
      csvRanges.assign(RangesTableSchema.FORMAT, null, ranges, OverwriteMode.ALWAYS);
      return this;
    }

    public RangeModifier setGeocode(SimpleLanguageTag lang, String name) {
      csvRanges.assign(
          RangesTableSchema.GEOCODES.getColumn(lang), name, ranges, OverwriteMode.ALWAYS);
      return this;
    }
  }

  /** Fluent API for modifying a set of shortcodes in a region. */
  public final class ShortcodeModifier {
    private final PhoneRegion region;
    private final RangeTree ranges;

    private ShortcodeModifier(PhoneRegion region, RangeTree ranges) {
      checkArgument(!ranges.isEmpty(), "cannot modify empty ranges");
      this.region = checkNotNull(region);
      this.ranges = ranges;
    }

    private RangeTable.Builder shortcode() {
      return shortcodes.get(region);
    }

    /** Sets the format assigned to the shortcodes. */
    public ShortcodeModifier setFormat(FormatSpec format) {
      String id =
          formats.computeIfAbsent(format, f -> String.format("__fmt_%02d", formats.size() + 1));
      shortcode().assign(ShortcodesTableSchema.FORMAT, id, ranges, OverwriteMode.ALWAYS);
      return this;
    }

    /** Sets the format assigned to the shortcodes. */
    public ShortcodeModifier setFormat(String id, FormatSpec format) {
      formats.put(format, id);
      shortcode().assign(ShortcodesTableSchema.FORMAT, id, ranges, OverwriteMode.ALWAYS);
      return this;
    }

    /** Clears the format assigned to the shortcodes. */
    public ShortcodeModifier clearFormat() {
      shortcode().assign(ShortcodesTableSchema.FORMAT, null, ranges, OverwriteMode.ALWAYS);
      return this;
    }
  }

  /** Fluent API for modifying attributes of range types. */
  public final class TypeModifier {
    private final PhoneRegion region;
    private final ExtType type;
    private final ExtTariff tariff;

    public TypeModifier(PhoneRegion region, ExtType type, ExtTariff tariff) {
      this.region = checkNotNull(region);
      this.type = checkNotNull(type);
      this.tariff = checkNotNull(tariff);
      checkArgument(regionMap.containsKey(region),
          "invalid test region '%s' not in: %s", region, regionMap.keySet());
    }

    public TypeModifier setExampleNumber(String ex) {
      inferValidNumberType(type, tariff)
          .ifPresent(t -> examples.put(region, t, DigitSequence.of(ex)));
      return this;
    }

    public TypeModifier addComment(String... lines) {
      inferValidNumberType(type, tariff)
          .flatMap(Types::toXmlType)
          .ifPresent(t -> comments.add(
              Comment.create(Comment.anchor(region, t), Arrays.asList(lines))));
      return this;
    }
  }

  /** Fluent API for modifying territory-level attributes. */
  public final class TerritoryModifier {
    private final PhoneRegion region;

    public TerritoryModifier(PhoneRegion region) {
      this.region = checkNotNull(region);
    }

    public TerritoryModifier addComment(String... lines) {
      comments.add(Comment.create(Comment.anchor(region), Arrays.asList(lines)));
      return this;
    }
  }

  private Table<PhoneRegion, ValidNumberType, DigitSequence> fillInMissingExampleNumbersFrom(
      RangeTable xmlTable, Table<PhoneRegion, ValidNumberType, DigitSequence> examples) {
    // Take a copy since the build() method is not meant to be modifying the builder itself.
    HashBasedTable<PhoneRegion, ValidNumberType, DigitSequence> examplesCopy =
        HashBasedTable.create(examples);
    addMissingExampleNumbersFor(mainRegion, xmlTable, examplesCopy);
    otherRegions.forEach(r -> addMissingExampleNumbersFor(r, xmlTable, examplesCopy));
    return examplesCopy;
  }

  private static void addMissingExampleNumbersFor(
      PhoneRegion region,
      RangeTable xmlTable,
      Table<PhoneRegion, ValidNumberType, DigitSequence> examples) {
    Column<Boolean> regionColumn = XmlRangesSchema.REGIONS.getColumn(region);
    RangeTable regionTable =
        xmlTable.subTable(xmlTable.getRanges(regionColumn, TRUE), XmlRangesSchema.TYPE);
    for (ValidNumberType type : regionTable.getAssignedValues(XmlRangesSchema.TYPE)) {
      if (examples.contains(region, type)) {
        continue;
      }
      RangeTree ranges = regionTable.getRanges(XmlRangesSchema.TYPE, type);
      // Assigned types must be assigned via non empty ranges (so first() cannot fail).
      examples.put(region, type, ranges.first());
    }
  }

  private static RangeTree rangesOf(String... specs) {
    checkArgument(specs.length > 0, "must provide at least one range specifier");
    RangeTree ranges = RangeTree.from(Arrays.stream(specs).map(RangeSpecification::parse));
    checkArgument(!ranges.getInitial().canTerminate(), "cannot add the empty digit sequence");
    return ranges;
  }

  private static Optional<ValidNumberType> inferValidNumberType(ExtType type, ExtTariff tariff) {
    // Tariff takes precedence over type.
    Optional<ValidNumberType> vnt = tariff.toValidNumberType();
    if (!vnt.isPresent()) {
      vnt = type.toValidNumberType();
    }
    return vnt;
  }
}
