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
package com.google.i18n.phonenumbers.metadata.table;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * A CSV schema is a combination of a key marshaller and table columns. A CSV schema defines a
 * CSV table with key columns, followed by non-key columns.
 */
@AutoValue
public abstract class CsvSchema<K> {
  /**
   * Returns a schema for a CSV file using the given marshaller to define key columns, and a table
   * schema to define any additional columns in a row.
   */
  public static <K> CsvSchema<K> of(CsvKeyMarshaller<K> marshaller, Schema columns) {
    return new AutoValue_CsvSchema<>(marshaller, columns);
  }

  /** The marshaller defining table keys and how they are serialized in CSV. */
  public abstract CsvKeyMarshaller<K> keyMarshaller();

  /** The table schema defining non-key columns in the table. */
  public abstract Schema columns();

  /** Returns the ordering for keys in the CSV table, as defined by the key marshaller. */
  public Optional<Comparator<K>> rowOrdering() {
    return keyMarshaller().ordering();
  }

  /**
   * Returns the ordering for additional non-key columns in the CSV table as defined by the table
   * schema.
   */
  public Comparator<Column<?>> columnOrdering() {
    return columns().ordering();
  }

  /**
   * Extracts the non-key columns of a table from the header row. The header row is expected to
   * contain the names of all columns (including key columns) in the CSV table and this method
   * verifies that the key columns are present as expected before resolving the non-key columns
   * in order.
   */
  public ImmutableList<Column<?>> parseHeader(List<String> header) {
    int hsize = keyMarshaller().getColumns().size();
    checkArgument(header.size() >= hsize, "CSV header too short: %s", header);
    checkArgument(header.subList(0, hsize).equals(keyMarshaller().getColumns()),
        "Invalid CSV header: %s", header);
    ImmutableList.Builder<Column<?>> columns = ImmutableList.builder();
    header.subList(hsize, header.size()).forEach(s -> columns.add(columns().getColumn(s)));
    return columns.build();
  }

  /** Parses a row from a CSV table containing unescaped values. */
  public void parseRow(
      ImmutableList<Column<?>> columns, List<String> row, BiConsumer<K, List<Assignment<?>>> fn) {
    int hsize = keyMarshaller().getColumns().size();
    checkArgument(row.size() >= hsize, "CSV row too short: %s", row);
    K key = keyMarshaller().deserialize(row.subList(0, hsize));
    List<Assignment<?>> rowAssignments = new ArrayList<>();
    for (int n = 0; n < row.size() - hsize; n++) {
      Column<?> c = columns.get(n);
      rowAssignments.add(
          Assignment.ofOptional(c, Optional.ofNullable(c.parse(row.get(n + hsize)))));
    }
    fn.accept(key, rowAssignments);
  }

  public CsvTable<K> load(Path file) throws IOException {
    if (!Files.exists(file)) {
      return CsvTable.builder(this).build();
    }
    try (Reader csv = Files.newBufferedReader(file)) {
      return CsvTable.importCsv(this, csv);
    }
  }

  public CsvTable<K> load(Reader reader) throws IOException {
    return CsvTable.importCsv(this, reader);
  }
}
