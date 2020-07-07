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

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.i18n.phonenumbers.metadata.table.CsvParser.rowMapper;
import static java.util.function.Function.identity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.model.FormatSpec.FormatTemplate;
import com.google.i18n.phonenumbers.metadata.table.CsvParser;
import com.google.i18n.phonenumbers.metadata.table.CsvParser.RowMapper;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.CsvTableCollector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * The schema of the "AltFormats" table with rows identified by an "alternate format specifier":
 * <ol>
 *   <li>{@link #PARENT}: The ID of the "main" format that this is an alternate of.
 *   <li>{@link #COMMENT}: Freeform comment text.
 * </ol>
 *
 * <p>Rows keys are serialized via the marshaller and produce the leading column:
 * <ol>
 *   <li>{@code Format}: The alternate format specifier including prefix and grouping information
 *   (e.g. "20 XXXX XXXX").
 * </ol>
 */
public final class AltFormatsSchema {
  private static final String FORMAT = "Format";
  private static final String PARENT = "Parent Format";
  private static final String COMMENT = "Comment";

  public static final ImmutableList<String> HEADER = ImmutableList.of(FORMAT, PARENT, COMMENT);

  private static final CsvParser CSV_PARSER = CsvParser.withSeparator(';').trimWhitespace();
  private static final RowMapper ROW_MAPPER =
      rowMapper(h -> checkArgument(h.equals(HEADER), "unexpected alt-format header: %s", h));

  /** Loads the alternate formats from a given file path. */
  public static ImmutableList<AltFormatSpec> loadAltFormats(Path path) {
    if (!Files.exists(path)) {
      return ImmutableList.of();
    }
    try (Reader csv = Files.newBufferedReader(path)) {
      return importAltFormats(csv);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  static ImmutableList<AltFormatSpec> importAltFormats(Reader csv) throws IOException {
    List<AltFormatSpec> altFormats = new ArrayList<>();
    Consumer<Stream<String>> rowCallback = getRowCallback(altFormats);
    try (BufferedReader r = new BufferedReader(csv)) {
      CSV_PARSER.parse(r.lines(),
          row -> rowCallback.accept(row.map(CsvTable::unescapeSingleLineCsvText)));
    }
    return ImmutableList.copyOf(altFormats);
  }

  public static ImmutableList<AltFormatSpec> importAltFormats(Supplier<List<String>> rows) {
    List<AltFormatSpec> altFormats = new ArrayList<>();
    Consumer<Stream<String>> rowCallback = getRowCallback(altFormats);
    // Expect header row always.
    rowCallback.accept(rows.get().stream());
    List<String> row;
    while ((row = rows.get()) != null) {
      rowCallback.accept(row.stream());
    }
    return ImmutableList.copyOf(altFormats);
  }

  private static Consumer<Stream<String>> getRowCallback(List<AltFormatSpec> altFormats) {
    return ROW_MAPPER.mapTo(
        row -> altFormats.add(parseAltFormat(row.get(FORMAT), row.get(PARENT), row.get(COMMENT))));
  }

  public static AltFormatSpec parseAltFormat(
      String altId, String parent, @Nullable String comment) {
    // "1X [2-8]XXX** XXX" --> "XX XXXX** XXX"
    FormatTemplate template = FormatTemplate.parse(altId.replaceAll("[0-9]|\\[[-0-9]+\\]", "X"));

    // "1X [2-8]XXX** XXX" --> "1X [2-8]" --> "1X[2-8]" --> "1x[2-8]"
    // The prefix here can (and often will be) the empty string.
    // This fails if '*' is ever left in the specification, but that really should not happen.
    RangeSpecification prefix = RangeSpecification.parse(
        Ascii.toLowerCase(whitespace().removeFrom(altId.replaceAll("[X* ]*$", ""))));
    return AltFormatSpec.create(template, prefix, parent, Optional.ofNullable(comment));
  }

  /** Exports alternate formats to a collector (potentially escaping fields for CSV). */
  public static void export(
      List<AltFormatSpec> altFormats, Consumer<Stream<String>> collector, boolean toCsv) {
    collector.accept(HEADER.stream());
    Function<String, String> escapeFn = toCsv ? CsvTable::escapeForSingleLineCsv : identity();
    altFormats.forEach(
        f -> collector.accept(
            Stream.of(f.specifier(), f.parentFormatId(), f.comment().map(escapeFn).orElse(""))));
  }

  /** Helper method to write alternate formats in same CSV format as CsvTable. */
  public static boolean exportCsv(Writer csv, List<AltFormatSpec> altFormats) {
    if (altFormats.isEmpty()) {
      return false;
    }
    CsvTableCollector collector = new CsvTableCollector(true);
    export(altFormats, collector, true);
    collector.writeCsv(csv);
    return true;
  }

  private AltFormatsSchema() {}
}
