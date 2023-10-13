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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static com.google.i18n.phonenumbers.metadata.table.DiffKey.Status.LHS_CHANGED;
import static com.google.i18n.phonenumbers.metadata.table.DiffKey.Status.LHS_ONLY;
import static com.google.i18n.phonenumbers.metadata.table.DiffKey.Status.RHS_CHANGED;
import static com.google.i18n.phonenumbers.metadata.table.DiffKey.Status.RHS_ONLY;
import static com.google.i18n.phonenumbers.metadata.table.DiffKey.Status.UNCHANGED;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.TreeBasedTable;
import com.google.common.escape.CharEscaperBuilder;
import com.google.common.escape.Escaper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * A general tabular representation of {@link Column} based data, which can include range data
 * (via {@link RangeTable}) or other tabular data using a specified row key implementation.
 *
 * @param <K> the row key type.
 */
@AutoValue
public abstract class CsvTable<K> {
  // Trim whitespace (since CSV files may be textually aligned) but don't allow multiline values
  // (we handle that by JSON style escaping to keep the "one row per line" assumption true).
  public static final String DEFAULT_DELIMETER = ";";
  private static final CsvParser CSV_PARSER =
      CsvParser.withSeparator(DEFAULT_DELIMETER.charAt(0)).trimWhitespace();

  /**
   * Mode to control how diffs are generated. If a diff table, rows have an additional
   * {@code Status} applied to describe whether they are unchanged, modified or exclusive (i.e.
   * exist only in one of the source tables).
   */
  public enum DiffMode {
    /** Include all rows in the "diff table" (unchanged, modified or exclusive). */
    ALL,
    /** Include only changed rows in the "diff table" (modified or exclusive). */
    CHANGES,
    /** Include only left-hand-side rows in the "diff table" (unchanged, modified or exclusive). */
    LHS,
    /** Include only right-hand-side rows in the "diff table" (unchanged, modified or exclusive). */
    RHS,
  }

  /** A simple builder for programmatic generation of CSV tables. */
  public static final class Builder<T> {
    private final CsvSchema<T> schema;
    private final Table<T, Column<?>, Object> table;

    private Builder(CsvSchema<T> schema) {
      this.schema = checkNotNull(schema);

      // Either use insertion order or sorted order for rows (depends on schema).
      if (schema.rowOrdering().isPresent()) {
        this.table = TreeBasedTable.create(schema.rowOrdering().get(), schema.columnOrdering());
      } else {
        this.table = Tables.newCustomTable(
            new LinkedHashMap<>(),
            () -> new TreeMap<>(schema.columnOrdering()));
      }
    }

    /**
     * Puts a row into the table using the specific mappings (potentially overwriting any existing
     * row).
     */
    public Builder<T> putRow(T key, Map<Column<?>, ?> row) {
      table.rowMap().remove(key);
      return addRow(key, row);
    }

    /**
     * Adds a new row to the table using the specific mappings (the row must not already be
     * present).
     */
    public Builder<T> addRow(T key, Map<Column<?>, ?> row) {
      checkArgument(!table.containsRow(key), "row '%s' already added\n%s", key, this);
      row.forEach((c, v) -> table.put(key, c, v));
      return this;
    }

    /**
     * Adds a new row to the table using the specific mappings (the row must not already be
     * present).
     */
    public Builder<T> addRow(T key, List<Assignment<?>> row) {
      checkArgument(!table.containsRow(key), "row '%s' already added\n%s", key, this);
      put(key, row);
      return this;
    }

    /** Puts (overwrites) a single value in the table. */
    public <V extends Comparable<V>> Builder<T> put(T key, Column<V> c, @Nullable V v) {
      schema.columns().checkColumn(c);
      if (v != null) {
        table.put(key, c, c.cast(v));
      } else {
        table.remove(key, c);
      }
      return this;
    }

    /** Puts (overwrites) a sequence of values in the table. */
    public Builder<T> put(T key, Iterable<Assignment<?>> assign) {
      for (Assignment<?> a : assign) {
        if (a.value().isPresent()) {
          table.put(key, a.column(), a.value().get());
        } else {
          table.remove(key, a.column());
        }
      }
      return this;
    }

    /** Puts (overwrites) a sequence of values in the table. */
    public Builder<T> put(T key, Assignment<?>... assign) {
      return put(key, Arrays.asList(assign));
    }

    /** Returns an unmodifiable view of the keys for the table. */
    public Set<T> getKeys() {
      return Collections.unmodifiableSet(table.rowKeySet());
    }

    /** Gets a single value in the table (or null). */
    public <V extends Comparable<V>> V get(T key, Column<V> c) {
      return c.cast(table.get(key, c));
    }

    /** Removes an entire row from the table (does nothing if the row did no exist). */
    public Builder<T> removeRow(T key) {
      table.rowKeySet().remove(key);
      return this;
    }

    /** Filters the rows of a table, keeping those which match the given predicate. */
    public Builder<T> filterRows(Predicate<T> predicate) {
      Set<T> rows = table.rowKeySet();
      // Copy to avoid concurrent modification exception.
      for (T key : ImmutableSet.copyOf(table.rowKeySet())) {
        if (!predicate.test(key)) {
          rows.remove(key);
        }
      }
      return this;
    }

    /** Filters the columns of a table, keeping only those which match the given predicate. */
    public Builder<T> filterColumns(Predicate<Column<?>> predicate) {
      Set<Column<?>> toRemove =
          table.columnKeySet().stream().filter(predicate.negate()).collect(toImmutableSet());
      table.columnKeySet().removeAll(toRemove);
      return this;
    }

    /** Builds the immutable CSV table. */
    public CsvTable<T> build() {
      return from(schema, table);
    }

    @Override
    public String toString() {
      return build().toString();
    }
  }

  /** Returns a builder for a CSV table with the expected key and column semantics. */
  public static <K> Builder<K> builder(CsvSchema<K> schema) {
    return new Builder<>(schema);
  }

  /** Returns a CSV table based on the given table with the expected key and column semantics. */
  public static <K> CsvTable<K> from(CsvSchema<K> schema, Table<K, Column<?>, Object> table) {
    ImmutableSet<Column<?>> columns = table.columnKeySet().stream()
        .sorted(schema.columnOrdering())
        .collect(toImmutableSet());
    columns.forEach(schema.columns()::checkColumn);
    return new AutoValue_CsvTable<>(
        schema,
        ImmutableMap.copyOf(Maps.transformValues(table.rowMap(), ImmutableMap::copyOf)),
        columns);
  }

  /**
   * Imports a semicolon separated CSV file. The CSV file needs to have the following layout:
   * <pre>
   * Key1 ; Key2 ; Column1 ; Column2 ; Column3
   * k1   ; k2   ; OTHER   ; "Text"  ; true
   * ...
   * </pre>
   * Where the first {@code N} columns represent the row key (as encapsulated by the key
   * {@link CsvKeyMarshaller}) and the remaining columns correspond to the given {@link Schema}
   * via the column names.
   * <p>
   * Column values are represented in a semi-typed fashion according to the associated column (some
   * columns require values to be escaped, others do not). Note that it's the column that defines
   * whether the value needs escaping, not the content of the value itself (all values in a String
   * column are required to be quoted).
   */
  public static <K> CsvTable<K> importCsv(CsvSchema<K> schema, Reader csv) throws IOException {
    return importCsv(schema, csv, CSV_PARSER);
  }

  /** Imports a CSV file using a specified parser. */
  public static <K> CsvTable<K> importCsv(CsvSchema<K> schema, Reader csv, CsvParser csvParser)
      throws IOException {
    TableParser<K> parser = new TableParser<>(schema);
    try (BufferedReader r = new BufferedReader(csv)) {
      csvParser.parse(
          r.lines(),
          row -> parser.accept(
              row.map(CsvTable::unescapeSingleLineCsvText).collect(toImmutableList())));
    }
    return parser.done();
  }

  /**
   * Imports a sequence of rows to create a CSV table. The values in the rows are unescaped and
   * require no explicit parsing.
   */
  public static <K> CsvTable<K> importRows(CsvSchema<K> schema, Supplier<List<String>> rows) {
    TableParser<K> parser = new TableParser<>(schema);
    List<String> row;
    while ((row = rows.get()) != null) {
      parser.accept(row);
    }
    return parser.done();
  }
  /**
   * Creates a "diff table" based on the given left and right table inputs. The resulting table
   * has a new key column which indicates (via the {@code Status} enum) how rows difference between
   * the left and right tables.
   */
  public static <K> CsvTable<DiffKey<K>> diff(CsvTable<K> lhs, CsvTable<K> rhs, DiffMode mode) {
    checkArgument(lhs.getSchema().equals(rhs.getSchema()), "Cannot diff with different schemas");
    checkNotNull(mode, "Must specify a diff mode");

    CsvKeyMarshaller<DiffKey<K>> marshaller = DiffKey.wrap(lhs.getSchema().keyMarshaller());
    CsvSchema<DiffKey<K>> diffSchema = CsvSchema.of(marshaller, lhs.getSchema().columns());

    Builder<DiffKey<K>> diff = CsvTable.builder(diffSchema);
    if (mode != DiffMode.RHS) {
      Sets.difference(lhs.getKeys(), rhs.getKeys())
          .forEach(k -> diff.addRow(DiffKey.of(LHS_ONLY, k), lhs.getRow(k)));
    }
    if (mode != DiffMode.LHS) {
      Sets.difference(rhs.getKeys(), lhs.getKeys())
          .forEach(k -> diff.addRow(DiffKey.of(RHS_ONLY, k), rhs.getRow(k)));
    }
    for (K key : Sets.intersection(lhs.getKeys(), rhs.getKeys())) {
      Map<Column<?>, Object> lhsRow = lhs.getRow(key);
      Map<Column<?>, Object> rhsRow = rhs.getRow(key);
      if (lhsRow.equals(rhsRow)) {
        if (mode != DiffMode.CHANGES) {
          diff.addRow(DiffKey.of(UNCHANGED, key), lhsRow);
        }
      } else {
        if (mode != DiffMode.RHS) {
          diff.addRow(DiffKey.of(LHS_CHANGED, key), lhsRow);
        }
        if (mode != DiffMode.LHS) {
          diff.addRow(DiffKey.of(RHS_CHANGED, key), rhsRow);
        }
      }
    }
    return diff.build();
  }

  /** Returns the schema for this table. */
  public abstract CsvSchema<K> getSchema();

  /** Returns the rows of the table (not public to avoid access to untyped access). */
  // Note that this cannot easily be replaced by ImmutableTable (as of Jan 2019) because
  // ImmutableTable has severe limitations on how row/column ordering is handled that make the
  // row/column ordering required in CsvTable currently impossible.
  abstract ImmutableMap<K, ImmutableMap<Column<?>, Object>> getRows();

  /**
   * Returns the set of columns for the table (excluding the synthetic key columns, which are
   * handled by the marshaller).
   */
  public abstract ImmutableSet<Column<?>> getColumns();

  /** Returns whether a row is in the table. */
  public boolean isEmpty() {
    return getRows().isEmpty();
  }

  /** Returns the set of keys for the table. */
  public ImmutableSet<K> getKeys() {
    return getRows().keySet();
  }

  /** Returns a single row as a map of column assignments. */
  public ImmutableMap<Column<?>, Object> getRow(K rowKey) {
    ImmutableMap<Column<?>, Object> row = getRows().get(rowKey);
    return row != null ? row : ImmutableMap.of();
  }

  /** Returns whether a row is in the table. */
  public boolean containsRow(K rowKey) {
    return getKeys().contains(rowKey);
  }

  public Builder<K> toBuilder() {
    Builder<K> builder = builder(getSchema());
    getRows().forEach(builder::putRow);
    return builder;
  }

  /** Returns the table column names, including the key columns, in schema order. */
  public Stream<String> getCsvHeader() {
    return Stream.concat(
            getSchema().keyMarshaller().getColumns().stream(),
            getColumns().stream().map(Column::getName));
  }

  /** Returns the unescaped CSV values for the specified row, in order. */
  public Stream<String> getCsvRow(K key) {
    checkArgument(getKeys().contains(key), "no such row: %s", key);
    // Note that we pass the raw value (possibly null) to serialize so that we don't conflate
    // missing and default values.
    return Stream.concat(
        getSchema().keyMarshaller().serialize(key),
        getColumns().stream().map(c -> c.serialize(getOrNull(key, c))));
  }

  /**
   * Exports the given table by writing its values as semicolon separated "CSV", with or without
   * alignment. For example (with alignment):
   *
   * <pre>
   * Key1 ; Key2 ; Column1 ; Column2 ; Column3
   * k1   ; k2   ; OTHER   ; "Text"  ; true
   * ...
   * </pre>
   *
   * Where the first {@code N} columns represent the row key (as encapsulated by the key {@link
   * CsvKeyMarshaller}) and the remaining columns correspond to the given {@link Schema} via the
   * column names.
   */
  public boolean exportCsv(Writer writer, boolean align) {
    return exportCsvHelper(writer, align, getColumns());
  }

  /**
   * Exports the given table by writing its values as semicolon separated "CSV", with or without
   * alignment. For example (with alignment):
   *
   * <pre>
   * Key1 ; Key2 ; Column1 ; Column2 ; Column3
   * k1   ; k2   ; OTHER   ; "Text"  ; true
   * ...
   * </pre>
   *
   * Where the first {@code N} columns represent the row key (as encapsulated by the key {@link
   * CsvKeyMarshaller}) and the remaining columns correspond to the given {@link Schema} via the
   * column names. This will add columns that are part of the schema for the given table but have no
   * assigned values.
   */
  public boolean exportCsvWithEmptyColumnsPresent(Writer writer, boolean align) {

    return exportCsvHelper(
        writer,
        align,
        Stream.concat(getSchema().columns().getColumns().stream(), getColumns().stream())
            .collect(ImmutableSet.toImmutableSet()));
  }

  private boolean exportCsvHelper(
      Writer writer, boolean align, ImmutableSet<Column<?>> columnsToExport) {

    if (isEmpty()) {
      // Exit for empty tables (CSV file is truncated). The caller may then delete the empty file.
      return false;
    }
    CsvTableCollector collector = new CsvTableCollector(align);
    collector.accept(
        Stream.concat(
                getSchema().keyMarshaller().getColumns().stream(),
                columnsToExport.stream().map(Column::getName))
            .distinct());
    for (K k : getKeys()) {
      // Format raw values (possibly null) to avoid default values everywhere.
      collector.accept(
          Stream.concat(
              getSchema().keyMarshaller().serialize(k),
              columnsToExport.stream().map(c -> formatValue(c, getOrNull(k, c)))));
    }
    collector.writeCsv(writer);
    return true;
  }

  @Nullable private <T extends Comparable<T>> T getOrNull(K rowKey, Column<T> column) {
    return column.cast(getRow(rowKey).get(column));
  }

  /**
   * Returns the value from the underlying table for the given row and column if present.
   */
  public <T extends Comparable<T>> Optional<T> get(K rowKey, Column<T> column) {
    return Optional.ofNullable(getOrNull(rowKey, column));
  }

  /**
   * Returns the value from the underlying table for the given row and column, or the (non-null)
   * default value.
   */
  public <T extends Comparable<T>> T getOrDefault(K rowKey, Column<T> column) {
    T value = getOrNull(rowKey, column);
    return value != null ? value : column.defaultValue();
  }

  /**
   * Returns the set of unique values in the given column. Note that if some rows do not have a
   * value, then this will NOT result in the column default value being in the returned set. An
   * empty column will result in an empty set being returned here.
   */
  public <T extends Comparable<T>> ImmutableSortedSet<T> getValues(Column<T> column) {
    return getKeys().stream()
        .map(k -> getOrNull(k, column))
        .filter(Objects::nonNull)
        .collect(toImmutableSortedSet(Ordering.natural()));
  }

  @Override
  public final String toString() {
    StringWriter w = new StringWriter();
    exportCsv(w, true);
    return w.toString();
  }

  /** Parses CSV data on per-row basis, deserializing keys and adding values to a table. */
  static class TableParser<K> implements Consumer<List<String>> {
    private final Builder<K> table;
    // Set when the header row is processed.
    private ImmutableList<Column<?>> columns = null;

    TableParser(CsvSchema<K> schema) {
      this.table = builder(schema);
    }

    @Override
    public void accept(List<String> row) {
      if (columns == null) {
        columns = table.schema.parseHeader(row);
      } else {
        table.schema.parseRow(columns, row, table::addRow);
      }
    }

    public CsvTable<K> done() {
      return table.build();
    }
  }

  // Newlines can, in theory, be emitted "raw" in the CSV output inside a quoted string, but
  // this breaks all sorts of nice properties of CSV files, since there's no longer one row per
  // line. This export process escapes literal newlines and other control characters into Json
  // like escape sequences ('\n', '\t', '\\' etc...). Unlike Json however, any double-quotes are
  // _not_ escaped via '\' since the CSV way to escape those is via doubling. We leave other
  // non-ASCII characters as-is, since this is meant to be as human readable as possible.
  private static final Escaper ESCAPER = new CharEscaperBuilder()
      .addEscape('\n', "\\n")
      .addEscape('\r', "\\r")
      .addEscape('\t', "\\t")
      .addEscape('\\', "\\\\")
      // This is a special case only required when writing CSV file (since the parser handles
      // unescaping quotes when they are read back in). In theory it should be part of a separate
      // step during CSV writing, but it's not worth splitting it out. This is not considered an
      // unsafe char (since it definitely does appear).
      .addEscape('"', "\"\"")
      .toEscaper();

  private static final CharMatcher ESCAPED_CHARS = CharMatcher.anyOf("\n\r\t\\");
  private static final CharMatcher UNSAFE_CHARS =
      CharMatcher.javaIsoControl().and(ESCAPED_CHARS.negate());

  private static String formatValue(Column<?> column, @Nullable Object value) {
    String unescaped = column.serialize(value);
    if (unescaped.isEmpty()) {
      return unescaped;
    }
    // Slightly risky with enums, since an enum could have ';' in its toString() representation.
    // However since columns and their semantics are tightly controlled, this should never happen.
    if (Number.class.isAssignableFrom(column.type())
        || column.type() == Boolean.class
        || column.type().isEnum()) {
      checkArgument(ESCAPED_CHARS.matchesNoneOf(unescaped), "Bad 'safe' value: %s", unescaped);
      return unescaped;
    }
    return escapeForSingleLineCsv(unescaped);
  }

  /**
   * Escapes and quotes an arbitrary text string, ensuring it is safe for use as a single-line CSV
   * value. Newlines, carriage returns and tabs are backslash escaped (as is backslash itself) and
   * other ISO control characters are not permitted.
   *
   * <p>The purpose of this method is to make arbitrary Unicode text readable in a single line of
   * a CSV file so that we can rely on per-line processing tools, such as "grep" or "sed" if needed
   * without requiring expensive conversion to/from a spreadsheet.
   */
  public static String escapeForSingleLineCsv(String unescaped) {
    checkArgument(UNSAFE_CHARS.matchesNoneOf(unescaped), "Bad string value: %s", unescaped);
    return '"' + ESCAPER.escape(unescaped) + '"';
  }

  /**
   * Unescapes a line of text escaped by {@link #escapeForSingleLineCsv(String)} to restore literal
   * newlines and other backslash-escaped characters. Note that if the given string already has
   * newlines present, they are preserved but will then be escaped if the text is re-escaped later.
   */
  public static String unescapeSingleLineCsvText(String s) {
    int i = s.indexOf('\\');
    if (i == -1) {
      return s;
    }
    StringBuilder out = new StringBuilder();
    int start = 0;
    do {
      out.append(s, start, i);
      char c = s.charAt(++i);
      out.append(checkNotNull(UNESCAPE.get(c), "invalid escape sequence: \\%s", c));
      start = i + 1;
      i = s.indexOf('\\', start);
    } while (i != -1);
    return out.append(s, start, s.length()).toString();
  }

  private static final ImmutableMap<Character, Character> UNESCAPE =
      ImmutableMap.<Character, Character>builder()
          .put('n', '\n')
          .put('r', '\r')
          .put('t', '\t')
          .put('\\', '\\')
          .buildOrThrow();

  // Visible for AutoValue only.
  CsvTable() {}
}
