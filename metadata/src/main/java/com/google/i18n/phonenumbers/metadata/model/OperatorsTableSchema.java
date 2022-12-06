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

import com.google.i18n.phonenumbers.metadata.i18n.SimpleLanguageTag;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.ColumnGroup;
import com.google.i18n.phonenumbers.metadata.table.CsvKeyMarshaller;
import com.google.i18n.phonenumbers.metadata.table.CsvSchema;
import com.google.i18n.phonenumbers.metadata.table.Schema;

/**
 * The schema of the "Operators" table with rows keyed by operator ID and columns:
 * <ol>
 *   <li>{@link #SELECTION_CODES}: Operator selection codes for national dialling.
 *   <li>{@link #IDD_PREFIXES}: International direct dialling codes.
 *   <li>{@link #NAMES}: A group of columns containing the name of the operator, potential in
 *       multiple languages. Note that English translations for all operators need not be present.
 * </ol>
 *
 * <p>Rows keys are serialized via the marshaller and produce the leading column:
 * <ol>
 *   <li>{@code Id}: The operator ID.
 * </ol>
 *
 * <p>The default IDD prefix should not be in this table, but is instead stored in the top-level
 * {@link MetadataTableSchema#IDD_PREFIX} column.
 *
 * <p>Note that there is a special case in which we need to store a selection code or IDD code, but
 * it does not below to a operator with an assigned range (e.g. it's a universally available code).
 * In these situations, you should ensure that the operator ID starts with "__" (double underscore)
 * to prevent consistency checks from complaining about unassigned operators. You can also omit a
 * name for the row, but should probably add a comment.
 */
public final class OperatorsTableSchema {
  /**
   * A comma separated list of "selection codes" (as range specifications) which are added to
   * national numbers (not always as a prefix) to select an operator for national dialling.
   * This will often contain many of the same values as IDD_CODES but need not be identical.
   *
   * <p>Note that while a single operator may have more than one code associated with it, the same
   * code cannot appear in more than one row in this table.
   */
  public static final Column<String> SELECTION_CODES = Column.ofString("Domestic Selection Codes");

  /**
   * A comma separated list of "International Direct Dialing" codes (as range specifications) which
   * are prefixes for international dialling. This will often contain many of the same prefixes as
   * SELECTION_CODES but need not be identical.
   *
   * <p>Note that while a single operator may have more than one code associated with it, the same
   * code cannot appear in more than one row in this table.
   */
  public static final Column<String> IDD_PREFIXES = Column.ofString("International Dialling Codes");

  /** The "Name:XXX" column group in the operator table. */
  public static final ColumnGroup<SimpleLanguageTag, String> NAMES =
      ColumnGroup.byLanguage(Column.ofString("Name"));

  public static final Column<String> COMMENT = RangesTableSchema.COMMENT;

  private static final CsvKeyMarshaller<String> MARSHALLER = CsvKeyMarshaller.ofSortedString("Id");

  private static final Schema COLUMNS = Schema.builder()
      .add(SELECTION_CODES)
      .add(IDD_PREFIXES)
      .add(NAMES)
      .add(COMMENT)
      .build();

  /** Schema instance defining the operators CSV table. */
  public static final CsvSchema<String> SCHEMA = CsvSchema.of(MARSHALLER, COLUMNS);

  private OperatorsTableSchema() {}
}
