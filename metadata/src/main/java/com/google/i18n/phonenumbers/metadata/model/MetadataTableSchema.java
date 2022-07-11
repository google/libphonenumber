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

import static java.util.Comparator.naturalOrder;

import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.Timezones;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvKeyMarshaller;
import com.google.i18n.phonenumbers.metadata.table.CsvSchema;
import com.google.i18n.phonenumbers.metadata.table.MultiValue;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The schema of the "Metadata" table with rows keyed by {@link DigitSequence} and columns:
 *
 * <ol>
 *   <li>{@link #MAIN_REGION}: The primary region associated with a calling code.
 *   <li>{@link #EXTRA_REGIONS}: A list of additional regions shared by the calling code.
 *   <li>{@link #NATIONAL_PREFIX}: The (optional) prefix used when dialling national numbers.
 *   <li>{@link #IDD_PREFIX}: The default international dialling (IDD) prefix.
 *   <li>{@link #TIMEZONE}: The default timezone name(s) for a calling code. Multiple timezones
 *       can be specific if separated by {@code '&'}.
 *   <li>{@link #MOBILE_PORTABLE_REGIONS}: A list of regions in which mobile numbers are portable
 *       between operators.
 *   <li>{@link #NATIONAL_PREFIX_OPTIONAL}: True if the national prefix is optional throughout the
 *       numbering plan (e.g. a prefix is defined, but does not have to be present when numbers are
 *       used).
 * </ol>
 *
 * <p>Rows keys are serialized via the marshaller and produce the leading column:
 * <ol>
 *   <li>{@code Calling Code}: The country calling code.
 * </ol>
 */
public final class MetadataTableSchema {
  /** Values in the "REGIONS" column are a sorted list of region codes. */
  public static final class Regions extends MultiValue<PhoneRegion, Regions> {
    private static final Regions EMPTY = new Regions(ImmutableSet.of());

    public static Column<Regions> column(String name) {
      return Column.create(Regions.class, name, EMPTY, Regions::new);
    }

    public static Regions of(PhoneRegion... regions) {
      return new Regions(Arrays.asList(regions));
    }

    public static Regions of(Iterable<PhoneRegion> regions) {
      return new Regions(regions);
    }

    private Regions(Iterable<PhoneRegion> regions) {
      super(regions, ',', naturalOrder(), true);
    }

    private Regions(String s) {
      super(s, PhoneRegion::of, ',', naturalOrder(), true);
    }
  }

  /**
   * Values in the "NATIONAL_PREFIX" column are an (unsorted) list of prefixes, with the preferred
   * prefix first.
   */
  public static final class DigitSequences extends MultiValue<DigitSequence, DigitSequences> {
    private static final DigitSequences EMPTY = new DigitSequences(ImmutableSet.of());

    public static Column<DigitSequences> column(String name) {
      return Column.create(DigitSequences.class, name, EMPTY, DigitSequences::new);
    }

    public static DigitSequences of(DigitSequence... numbers) {
      return new DigitSequences(Arrays.asList(numbers));
    }

    private DigitSequences(Iterable<DigitSequence> numbers) {
      super(numbers, ',', naturalOrder(), false);
    }

    private DigitSequences(String s) {
      super(s, DigitSequence::of, ',', naturalOrder(), false);
    }
  }

  /** The primary region associated with a calling code (e.g. "US" for NANPA). */
  public static final Column<PhoneRegion> MAIN_REGION =
      Column.create(PhoneRegion.class, "Main Region", PhoneRegion.getUnknown(), PhoneRegion::of);

  /** A comma separated list of expected regions for the calling code. */
  public static final Column<Regions> EXTRA_REGIONS = Regions.column("Extra Regions");

  /**
   * A list of prefixes used when dialling national numbers (e.g. "0" for "US"). If more than one
   * prefix is given, the first prefix is assumed to be "preferred" and the others are considered
   * alternatives. Having multiple prefixes is useful if a country switches between prefixes and
   * a period of "parallel running" is needed.
   */
  public static final Column<DigitSequences> NATIONAL_PREFIX =
      DigitSequences.column("National Prefix");

  /**
   * The default international dialling (IDD) prefix.  This is a string, rather than a digit
   * sequence, because it can optionally contain a single '~' character to indicate a pause while
   * dialling (e.g. "8~10" in Russia). This is stripped everywhere except when used to populate
   * the "preferredInternationalPrefix" attribute in the libphonenumber XML file.
   */
  public static final Column<String> IDD_PREFIX = Column.ofString("IDD Prefix");

  /**
   * The default value for the "Timezone" column in the ranges table (in many regions, this is a
   * single constant value).
   */
  public static final Column<Timezones> TIMEZONE = RangesTableSchema.TIMEZONE;

  /** A comma separated list of regions in which mobile numbers are portable between carriers. */
  public static final Column<Regions> MOBILE_PORTABLE_REGIONS =
      Regions.column("Mobile Portable Regions");

  /** Describes whether the "national prefix" is optional when parsing a national number. */
  public static final Column<Boolean> NATIONAL_PREFIX_OPTIONAL =
      Column.ofBoolean("National Prefix Optional");

  /** The preferred prefix for specifying extensions to numbers (e.g. "ext" for "1234 ext 56"). */
  public static final Column<String> EXTENSION_PREFIX = Column.ofString("Extension Prefix");

  private static final CsvKeyMarshaller<DigitSequence> MARSHALLER = new CsvKeyMarshaller<>(
      k -> Stream.of(k.toString()),
      p -> DigitSequence.of(p.get(0)),
      Optional.of(Comparator.comparing(Object::toString)),
      "Calling Code");

  private static final Schema COLUMNS = Schema.builder()
      .add(MAIN_REGION)
      .add(EXTRA_REGIONS)
      .add(NATIONAL_PREFIX)
      .add(IDD_PREFIX)
      .add(TIMEZONE)
      .add(MOBILE_PORTABLE_REGIONS)
      .add(NATIONAL_PREFIX_OPTIONAL)
      .add(EXTENSION_PREFIX)
      .build();

  /** Schema instance defining the metadata CSV table. */
  public static final CsvSchema<DigitSequence> SCHEMA = CsvSchema.of(MARSHALLER, COLUMNS);

  private MetadataTableSchema() {}
}
