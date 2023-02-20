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

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvKeyMarshaller;
import com.google.i18n.phonenumbers.metadata.table.CsvSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.util.Optional;

/**
 * The schema of the "Formats" table with rows keyed by ID, and columns:
 * <ol>
 *   <li>{@link #NATIONAL}: Required national format (may contain '#' for national prefix).
 *   <li>{@link #CARRIER}: Optional carrier format (may contain '#' and '@' for carrier
 *       specifier). Must be compatible with the national format (same suffix).
 *   <li>{@link #INTERNATIONAL}: International format (must not contain '#' or '@').
 *   <li>{@link #LOCAL}: Local format (must not contain '#' or '@', and must correspond to assigned
 *       area code lengths if present).
 *   <li>{@link #COMMENT}: Freeform comment text.
 * </ol>
 *
 * <p>Rows keys are serialized via the marshaller and produce the leading column:
 * <ol>
 *   <li>{@code Id}: The format ID.
 * </ol>
 */
public final class FormatsTableSchema {
  public static final Column<String> NATIONAL = Column.ofString("National");
  public static final Column<String> CARRIER = Column.ofString("Carrier");
  public static final Column<String> INTERNATIONAL = Column.ofString("International");
  public static final Column<String> LOCAL = Column.ofString("Local");

  public static final Column<Boolean> NATIONAL_PREFIX_OPTIONAL =
      Column.ofBoolean("National Prefix Optional");
  /** An arbitrary optional text comment. */
  public static final Column<String> COMMENT = Column.ofString("Comment");

  private static final CsvKeyMarshaller<String> MARSHALLER = CsvKeyMarshaller.ofSortedString("Id");

  private static final Schema COLUMNS =
      Schema.builder()
          .add(NATIONAL)
          .add(CARRIER)
          .add(INTERNATIONAL)
          .add(LOCAL)
          .add(NATIONAL_PREFIX_OPTIONAL)
          .add(COMMENT)
          .build();

  /** Schema instance defining the operators CSV table. */
  public static final CsvSchema<String> SCHEMA = CsvSchema.of(MARSHALLER, COLUMNS);

  /** Converts a CSV table into a map of format specifiers. */
  public static ImmutableMap<String, FormatSpec> toFormatSpecs(CsvTable<String> formats) {
    ImmutableMap.Builder<String, FormatSpec> specs = ImmutableMap.builder();
    for (String id : formats.getKeys()) {
      specs.put(
          id,
          FormatSpec.of(
              formats.getOrDefault(id, NATIONAL),
              toOptional(formats.getOrDefault(id, CARRIER)),
              toOptional(formats.getOrDefault(id, INTERNATIONAL)),
              toOptional(formats.getOrDefault(id, LOCAL)),
              formats.getOrDefault(id, NATIONAL_PREFIX_OPTIONAL),
              toComment(formats.getOrDefault(id, COMMENT))));
    }
    return specs.buildOrThrow();
  }

  private static Optional<String> toOptional(String s) {
    return s.isEmpty() ? Optional.empty() : Optional.of(s);
  }

  private static Optional<Comment> toComment(String s) {
    return s.isEmpty() ? Optional.empty() : Optional.of(Comment.fromText(s));
  }

  private FormatsTableSchema() {}
}
