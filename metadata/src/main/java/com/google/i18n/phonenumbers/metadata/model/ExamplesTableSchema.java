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

import static com.google.i18n.phonenumbers.metadata.model.ExamplesTableSchema.ExampleNumberKey.ORDERING;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.CsvKeyMarshaller;
import com.google.i18n.phonenumbers.metadata.table.CsvSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The schema of the "Example Numbers" table with rows keyed by {@link ExampleNumberKey} and
 * columns:
 * <ol>
 *   <li>{@link #NUMBER}: The national number
 *   <li>{@link #COMMENT}: Evidence for why an example number was chosen.
 * </ol>
 *
 * <p>Rows keys are serialized via the marshaller and produce leading columns:
 * <ol>
 *   <li>{@code Region}: The region code of the example number.
 *   <li>{@code Type}: The {@link ValidNumberType} of the example number.
 * </ol>
 */
public final class ExamplesTableSchema {
  /** A key for rows in the example numbers table. */
  @AutoValue
  public abstract static class ExampleNumberKey {
    public static final Comparator<ExampleNumberKey> ORDERING =
        Comparator.comparing(ExampleNumberKey::getRegion).thenComparing(ExampleNumberKey::getType);

    public static ExampleNumberKey of(PhoneRegion region, ValidNumberType type) {
      return new AutoValue_ExamplesTableSchema_ExampleNumberKey(region, type);
    }

    public abstract PhoneRegion getRegion();
    public abstract ValidNumberType getType();
  }

  /** A number column containing the digit sequence of a national number. */
  public static final Column<DigitSequence> NUMBER = Column.create(
      DigitSequence.class, "Number", DigitSequence.empty(), DigitSequence::of);

  /** A general comment field, usually describing how an example number was determined. */
  public static final Column<String> COMMENT = Column.ofString("Comment");

  private static final CsvKeyMarshaller<ExampleNumberKey> MARSHALLER = new CsvKeyMarshaller<>(
      ExamplesTableSchema::write,
      ExamplesTableSchema::read,
      Optional.of(ORDERING),
      "Region",
      "Type");

  private static final Schema COLUMNS = Schema.builder()
      .add(NUMBER)
      .add(COMMENT)
      .build();

  /** Schema instance defining the example numbers CSV table. */
  public static final CsvSchema<ExampleNumberKey> SCHEMA = CsvSchema.of(MARSHALLER, COLUMNS);

  /**
   * Converts a {@link Table} of example numbers into a {@link CsvTable}, using
   * {@link ExampleNumberKey}s as row keys.
   */
  public static CsvTable<ExampleNumberKey> toCsv(
      Table<PhoneRegion, ValidNumberType, DigitSequence> table) {
    ImmutableTable.Builder<ExampleNumberKey, Column<?>, Object> out = ImmutableTable.builder();
    out.orderRowsBy(ORDERING).orderColumnsBy(COLUMNS.ordering());
    for (Cell<PhoneRegion, ValidNumberType, DigitSequence> c : table.cellSet()) {
      out.put(ExampleNumberKey.of(c.getRowKey(), c.getColumnKey()), NUMBER, c.getValue());
    }
    return CsvTable.from(SCHEMA, out.buildOrThrow());
  }

  /**
   * Converts a {@link Table} of example numbers into a {@link CsvTable}, using
   * {@link ExampleNumberKey}s as row keys.
   */
  public static ImmutableTable<PhoneRegion, ValidNumberType, DigitSequence>
      toExampleTable(CsvTable<ExampleNumberKey> csv) {
    ImmutableTable.Builder<PhoneRegion, ValidNumberType, DigitSequence> out =
        ImmutableTable.builder();
    for (ExampleNumberKey k : csv.getKeys()) {
      out.put(k.getRegion(), k.getType(), csv.getOrDefault(k, NUMBER));
    }
    return out.buildOrThrow();
  }

  private static Stream<String> write(ExampleNumberKey key) {
    return Stream.of(key.getRegion().toString(), key.getType().toString());
  }

  private static ExampleNumberKey read(List<String> parts) {
    return ExampleNumberKey.of(
        PhoneRegion.of(parts.get(0)), ValidNumberType.valueOf(parts.get(1)));
  }

  private ExamplesTableSchema() {}
}
