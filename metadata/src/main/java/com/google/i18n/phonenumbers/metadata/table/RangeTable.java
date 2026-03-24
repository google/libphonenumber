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
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.UnmodifiableIterator;
import com.google.i18n.phonenumbers.metadata.PrefixTree;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * A tabular representation of attributes, assigned to number ranges.
 * <p>
 * A {@code RangeTable} is equivalent to {@code Table&lt;RangeSpecification, Column, Value&gt;},
 * but is expressed as a mapping of {@code (Column, Value) -> RangeTree} (since {@code RangeTree}
 * is not a good key). To keep the data structurally equivalent to its tabular form, it's important
 * that within a column, all assigned ranges are mutually disjoint (and thus a digit sequence can
 * have at most one value assigned in any column).
 *
 * <h3>Table Schemas</h3>
 * A table requires a {@link Schema}, which defines the columns which can be present and their
 * order. Column ordering is important since it relates to how rules are applied (see below).
 *
 * <h3>Columns and Column Groups</h3>
 * A {@link Column} defines a category of values of a particular type (e.g. String, Boolean,
 * Integer or user specified enums) and a default value. New columns can be implemented easily and
 * can choose to limit their values to some known set.
 * <p>
 * A {@link ColumnGroup} defines a related set of columns of the same type. The exact set of
 * columns available in a group is not necessarily known in advance. A good example of a column
 * group is having columns for names is different languages. A column group of "Name" could define
 * columns such as "Name:en", "Name:fr", "Name:ja" etc. which contain the various translations of
 * the value. The first time a value is added for a column inferred by a column group, that column
 * is created.
 * <p>
 * An {@link Assignment} is a useful way to encapsulate "a value in a column" and can be used to
 * assign or unassign values to ranges, or query for the ranges which have that assignment.
 *
 * <h3>Builders and Unassigned Values</h3>
 * To allow a {@code RangeTable} to fully represent data in a tabular way, it must be possible to
 * have rows in a table for which no value is assigned in any column. Unassigned ranges can be
 * added to a builder via the {@link Builder#add(RangeTree)} method, and these "empty rows" are
 * preserved in the final table.
 * <p>
 * This is useful since it allows a {@link Change} to affect no columns, but still have an effect
 * on the final table. It's also useful when applying rules to infer values and fill-in column
 * defaults.
 */
public final class RangeTable {

  /** Overwrite rules for modifying range categorization. */
  public enum OverwriteMode {
    /** Only assign ranges that were previously unassigned. */
    NEVER,
    /** Only assign ranges that were either unassigned or had the same value. */
    SAME,
    /** Always assign ranges (and unassign them from any other values in the same category). */
    ALWAYS;
  }

  /** A builder for an immutable range table to which changes and rules can be applied. */
  public static final class Builder {
    // The schema for the table to be built.
    private final Schema schema;
    // The map of per-column ranges.
    private final SortedMap<Column<?>, DisjointRangeMap.Builder<?>> columnRanges;
    // The union of all ranges added to the builder (either by assignment or range addition).
    // This is not just a cache of all the assigned ranges, since assigning and unassigning a range
    // will not cause it to be removed from the table altogether (even if it is no longer assigned
    // in any column).
    private RangeTree allRanges = RangeTree.empty();

    private Builder(Schema schema) {
      this.schema = checkNotNull(schema);
      this.columnRanges = new TreeMap<>(schema.ordering());
    }

    // Helper to return an on-demand builder for a column.
    private <T extends Comparable<T>> DisjointRangeMap.Builder<T> getOrAddRangeMap(Column<T> c) {
      // The generic type of the builder is defined by the column it's building for, and the map
      // just uses that column as its key. Thus, if the given column is recognized by the schema,
      // the returned builder must be of the same type.
      @SuppressWarnings("unchecked")
      DisjointRangeMap.Builder<T> ranges = (DisjointRangeMap.Builder<T>)
          columnRanges.computeIfAbsent(schema.checkColumn(c), DisjointRangeMap.Builder::new);
      return ranges;
    }

    // ---- Read-only API ----

    /** Returns the schema for this builder. */
    public Schema getSchema() {
      return schema;
    }

    /**
     * Returns ranges for the given assignment. If the value is {@code empty}, then the unassigned
     * ranges in the column are returned.
     */
    public RangeTree getRanges(Assignment<?> assignment) {
      return getRanges(assignment.column(), assignment.value().orElse(null));
    }

    /**
     * Returns ranges for the given value in the specified column. If the value is {@code null},
     * then the unassigned ranges in the column are returned. If the column has no values assigned,
     * then the empty range is returned (or, if {@code value == null}, all ranges in the table).
     */
    public RangeTree getRanges(Column<?> column, @Nullable Object value) {
      getSchema().checkColumn(column);
      DisjointRangeMap.Builder<?> rangeMap = columnRanges.get(column);
      if (value != null) {
        return rangeMap != null ? rangeMap.getRanges(value) : RangeTree.empty();
      } else {
        RangeTree all = getAllRanges();
        return rangeMap != null ? all.subtract(rangeMap.getAssignedRanges()) : all;
      }
    }

    /**
     * Returns all assigned ranges in the specified column. If the column doesn't exist in the
     * table, the empty range is returned).
     */
    public RangeTree getAssignedRanges(Column<?> column) {
      getSchema().checkColumn(column);
      DisjointRangeMap.Builder<?> rangeMap = columnRanges.get(column);
      return rangeMap != null ? rangeMap.getAssignedRanges() : RangeTree.empty();
    }

    /**
     * Returns ranges which were added to this builder, either directly via {@link #add(RangeTree)}
     * or indirectly via assignment.
     */
    public RangeTree getAllRanges() {
      return allRanges;
    }

    /** Returns all ranges present in this table which are not assigned in any column. */
    public RangeTree getUnassignedRanges() {
      RangeTree allAssigned = columnRanges.values().stream()
          .map(DisjointRangeMap.Builder::getAssignedRanges)
          .reduce(RangeTree.empty(), RangeTree::union);
      return allRanges.subtract(allAssigned);
    }

    /**
     * Returns a snapshot of the columns in schema order (including empty columns which may have
     * been added explicitly or exist due to values being unassigned).
     */
    public ImmutableSet<Column<?>> getColumns() {
      return columnRanges.entrySet().stream()
          .map(Entry::getKey)
          .collect(toImmutableSet());
    }

    // ---- Range assignment/addition/removal ----

    /**
     * Assigns the specified ranges to the given assignment. If the value is {@code empty}, then
     * this has the effect of unassigning the given ranges, but does not remove them from the
     * table. If {@code ranges} is empty, this method has no effect.
     *
     * @throws RangeException if assignment cannot be performed according to the overwrite mode
     *     (no change will have occurred in the table if this occurs).
     */
    public Builder assign(Assignment<?> assignment, RangeTree ranges, OverwriteMode mode) {
      assign(assignment.column(), assignment.value().orElse(null), ranges, mode);
      return this;
    }

    /**
     * Assigns the specified ranges to a value within a column (other columns unaffected). If the
     * value is {@code null}, then this has the effect of unassigning the given ranges, but does
     * not remove them from the table. If {@code ranges} is empty, this method has no effect.
     *
     * @throws RangeException if assignment cannot be performed according to the overwrite mode
     *     (no change will have occurred in the table if this occurs).
     */
    public Builder assign(
        Column<?> column, @Nullable Object value, RangeTree ranges, OverwriteMode mode) {
      if (!ranges.isEmpty()) {
        getOrAddRangeMap(column).assign(value, ranges, mode);
        allRanges = allRanges.union(ranges);
      }
      return this;
    }

    /**
     * Unconditionally assigns all values, ranges and columns in the given table. This does not
     * clear any already assigned ranges.
     */
    public Builder add(RangeTable table) {
      add(table.getAllRanges());
      add(table.getColumns());
      for (Column<?> column : table.getColumns()) {
        for (Object value : table.getAssignedValues(column)) {
          assign(column, value, table.getRanges(column, value), OverwriteMode.ALWAYS);
        }
      }
      return this;
    }

    /**
     * Ensures that the given ranges exist in the table, even if no assignments are ever made in
     * any columns.
     */
    public Builder add(RangeTree ranges) {
      allRanges = allRanges.union(ranges);
      return this;
    }

    /** Ensures that the given column exists in the table (even if there are no assignments). */
    public Builder add(Column<?> column) {
      getOrAddRangeMap(checkNotNull(column));
      return this;
    }

    /** Ensures that the given columns exist in the table (even if there are no assignments). */
    public Builder add(Collection<Column<?>> columns) {
      columns.forEach(this::add);
      return this;
    }

    /** Removes the given ranges from the table, including all assignments in all columns. */
    public Builder remove(RangeTree ranges) {
      for (DisjointRangeMap.Builder<?> rangeMap : columnRanges.values()) {
        rangeMap.assign(null, ranges, OverwriteMode.ALWAYS);
      }
      allRanges = allRanges.subtract(ranges);
      return this;
    }

    /** Removes the given column from the table (has no effect if the column is not present). */
    public Builder remove(Column<?> column) {
      columnRanges.remove(checkNotNull(column));
      return this;
    }

    /** Removes the given columns from the table (has no effect if columns are not present). */
    public Builder remove(Collection<Column<?>> columns) {
      columns.forEach(this::remove);
      return this;
    }

    /** Copies the assigned, non-default, values of the specified column. */
    public <T extends Comparable<T>> Builder copyNonDefaultValues(
        Column<T> column, RangeTable src, OverwriteMode mode) {
      for (T v : src.getAssignedValues(column)) {
        if (!column.defaultValue().equals(v)) {
          assign(column, v, src.getRanges(column, v), mode);
        }
      }
      return this;
    }

    // ---- Applying changes ----

    /**
     * Unconditionally applies the given change to this range table. Unlike
     * {@link #apply(Change, OverwriteMode)}, this method cannot fail, since changes are applied
     * unconditionally.
     */
    public Builder apply(Change change) {
      return apply(change, OverwriteMode.ALWAYS);
    }

    /**
     * Applies the given change to this range table. A change adds ranges to the table, optionally
     * assigning them specific category values within columns.
     *
     * @throws RangeException if the overwrite mode prohibits the modification in this change (the
     *    builder remains unchanged).
     */
    public Builder apply(Change change, OverwriteMode mode) {
      RangeTree ranges = change.getRanges();
      if (!ranges.isEmpty()) {
        // Check first that the assignments will succeed before attempting them (so as not to
        // leave the builder in an inconsistent state if it fails).
        if (mode != OverwriteMode.ALWAYS) {
          for (Assignment<?> a : change.getAssignments()) {
            getOrAddRangeMap(a.column()).checkAssign(a.value().orElse(null), ranges, mode);
          }
        }
        for (Assignment<?> a : change.getAssignments()) {
          getOrAddRangeMap(a.column()).assign(a.value().orElse(null), ranges, mode);
        }
        allRanges = allRanges.union(ranges);
      }
      return this;
    }

    // ---- Builder related methods ----

    /** Builds the range table from the current state of the builder. */
    public RangeTable build() {
      ImmutableMap<Column<?>, DisjointRangeMap<?>> columnMap = columnRanges.entrySet().stream()
          .map(e -> immutableEntry(e.getKey(), e.getValue().build()))
          .sorted(comparingByKey(schema.ordering()))
          .collect(toImmutableMap(Entry::getKey, Entry::getValue));
      return new RangeTable(schema, columnMap, allRanges, getUnassignedRanges());
    }

    /**
     * Returns a new builder with the same state as the current builder. This is useful when state
     * is being built up incrementally.
     */
    public Builder copy() {
      // Can be made more efficient if necessary...
      return build().toBuilder();
    }

    /** Builds a minimal version of this table in which empty columns are no longer present. */
    public RangeTable buildMinimal() {
      ImmutableSet<Column<?>> empty = columnRanges.entrySet().stream()
          .filter(e -> e.getValue().getAssignedRanges().isEmpty())
          .map(Entry::getKey)
          .collect(toImmutableSet());
      remove(empty);
      return build();
    }

    @Override
    public final String toString() {
      return build().toString();
    }
  }

  /** Returns a builder for a range table with the specified column mapping. */
  public static Builder builder(Schema schema) {
    return new Builder(schema);
  }

  public static RangeTable from(
      Schema schema, Table<RangeSpecification, Column<?>, Optional<?>> t) {
    Builder table = builder(schema);
    for (Entry<RangeSpecification, Map<Column<?>, Optional<?>>> row : t.rowMap().entrySet()) {
      List<Assignment<?>> assignments = row.getValue().entrySet().stream()
          .map(e -> Assignment.ofOptional(e.getKey(), e.getValue()))
          .collect(toImmutableList());
      table.apply(Change.of(RangeTree.from(row.getKey()), assignments));
    }
    return table.build();
  }

  // Definition of table columns.
  private final Schema schema;
  // Mapping to the assigned ranges for each column type.
  private final ImmutableMap<Column<?>, DisjointRangeMap<?>> columnRanges;
  // All ranges in this table (possibly larger than union of all assigned ranges in all columns).
  private final RangeTree allRanges;
  // Ranges unassigned in any column (a subset of, or equal to allRanges).
  private final RangeTree unassigned;

  private RangeTable(
      Schema schema,
      ImmutableMap<Column<?>, DisjointRangeMap<?>> columnRanges,
      RangeTree allRanges,
      RangeTree unassigned) {
    this.schema = checkNotNull(schema);
    this.columnRanges = checkNotNull(columnRanges);
    this.allRanges = checkNotNull(allRanges);
    this.unassigned = checkNotNull(unassigned);
  }

  /** Returns a builder initialized to the ranges and assignements in this table. */
  public Builder toBuilder() {
    // Any mode would work here (the builder is empty) but the "always overwrite" mode is fastest.
    return new Builder(schema).add(this);
  }

  private Optional<DisjointRangeMap<?>> getRangeMap(Column<?> column) {
    return Optional.ofNullable(columnRanges.get(schema.checkColumn(column)));
  }

  public Schema getSchema() {
    return schema;
  }

  public ImmutableSet<Column<?>> getColumns() {
    return columnRanges.keySet();
  }

  /**
   * Returns the set of values with assigned ranges in the given column.
   *
   * @throws IllegalArgumentException if the specified column does not exist in this table.
   */
  public <T extends Comparable<T>> ImmutableSet<T> getAssignedValues(Column<T> column) {
    getSchema().checkColumn(column);
    // Safe since if the column is in the schema the values must have been checked when added.
    @SuppressWarnings("unchecked")
    DisjointRangeMap<T> rangeMap =
        (DisjointRangeMap<T>) columnRanges.get(schema.checkColumn(column));
    return rangeMap != null ? rangeMap.getAssignedValues() : ImmutableSet.of();
  }

  /** Returns all assigned ranges in the specified column. */
  public RangeTree getAssignedRanges(Column<?> column) {
    return getRangeMap(column).map(DisjointRangeMap::getAssignedRanges).orElse(RangeTree.empty());
  }

  /**
   * Returns ranges for the given assignment. If the value is {@code empty}, then the unassigned
   * ranges in the column are returned.
   */
  public RangeTree getRanges(Assignment<?> assignment) {
    return getRanges(assignment.column(), assignment.value().orElse(null));
  }

  /**
   * Returns ranges for the given value in the specified column. If the value is {@code null}, then
   * the unassigned ranges in the column are returned.
   */
  public RangeTree getRanges(Column<?> column, @Nullable Object value) {
    getSchema().checkColumn(column);
    if (value == null) {
      return getAllRanges().subtract(getAssignedRanges(column));
    } else {
      return getRangeMap(column).map(m -> m.getRanges(value)).orElse(RangeTree.empty());
    }
  }

  /** Returns all ranges present in this table. */
  public RangeTree getAllRanges() {
    return allRanges;
  }

  /** Returns all ranges present in this table which are not assigned in any column. */
  public RangeTree getUnassignedRanges() {
    return unassigned;
  }

  /**
   * Returns whether this table contains no ranges (assigned or unassigned). Note that not all
   * empty tables are equal, since they may still differ by the columns they have.
   */
  public boolean isEmpty() {
    return allRanges.isEmpty();
  }

  /**
   * Returns a sub-table with rows and columns limited by the specified bounds. The schema of the
   * returned table is the same as this table.
   */
  public RangeTable subTable(RangeTree bounds, Set<Column<?>> columns) {
    // Columns must be a subset of what's allowed in this schema.
    columns.forEach(getSchema()::checkColumn);
    return subTable(bounds, getSchema(), columns);
  }

  /**
   * Returns a sub-table with rows and columns limited by the specified bounds. The schema of the
   * returned table is the same as this table.
   */
  public RangeTable subTable(RangeTree bounds, Column<?> first, Column<?>... rest) {
    return subTable(bounds, ImmutableSet.<Column<?>>builder().add(first).add(rest).build());
  }

  /**
   * Returns a table with rows and columns limited by the specified bounds. The schema of the
   * returned table is the given sub-schema.
   */
  public RangeTable subTable(RangeTree bounds, Schema subSchema) {
    checkArgument(subSchema.isSubSchemaOf(getSchema()),
        "expected sub-schema of %s, got %s", getSchema(), subSchema);
    return subTable(bounds, subSchema, Sets.filter(getColumns(), subSchema::isValidColumn));
  }

  // Callers MUST validate that the given set of columns are all valid in the subSchema.
  private RangeTable subTable(RangeTree bounds, Schema subSchema, Set<Column<?>> columns) {
    ImmutableMap<Column<?>, DisjointRangeMap<?>> columnMap = columns.stream()
        // Bound the given columns which exist in this table.
        .map(c -> immutableEntry(c, getRangeMap(c).map(r -> r.intersect(bounds))))
        // Reject columns we didn't already have (but allow empty columns if they exist).
        .filter(e -> e.getValue().isPresent())
        // Sort to our schema (since the given set of columns is not required to be sorted).
        .sorted(comparingByKey(schema.ordering()))
        .collect(toImmutableMap(Entry::getKey, e -> e.getValue().get()));
    return new RangeTable(
        subSchema, columnMap, allRanges.intersect(bounds), unassigned.intersect(bounds));
  }

  /**
   * Returns the assigned rows of a RangeTable as a minimal list of disjoint changes, which can
   * be applied to an empty table to recreate this table. No two changes affect the same columns
   * in the same way and changes are ordered by the minimal values of their ranges. This is
   * essentially the same information as returned in {@link #toImmutableTable()} but does not
   * decompose ranges into range specifications, and it thus more amenable to compact
   * serialization.
   */
  // Note that the minimal nature of the returned changes is essential for some algorithms that
  // operate on tables and this must not be changed.
  public ImmutableList<Change> toChanges() {
    Table<Column<?>, Optional<?>, RangeTree> table = HashBasedTable.create();
    for (Column<?> c : getColumns()) {
      for (Object v : getAssignedValues(c)) {
        table.put(c, Optional.of(v), getRanges(c, v));
      }
    }
    return toChanges(schema, table, getAllRanges());
  }

  /**
   * Returns a minimum set of changes based on a table of assignments (column plus value). This is
   * not expected to be used often (since RangeTable is usually a better representation of the data
   * but can be useful in representing things like updates and patches in which only some rows or
   * columns are represented.
   *  @param schema a schema for the columns in the given Table (used to determine column order).
   * @param table the table of assignments to assigned ranges.
   * @param allRanges the set of all ranges affected by the changes (this might include ranges not
 *     present anywhere in the table, which correspond to empty rows).
   */
  public static ImmutableList<Change> toChanges(
      Schema schema, Table<Column<?>, Optional<?>, RangeTree> table, RangeTree allRanges) {
    return ImmutableList.copyOf(
        transform(toRows(table, allRanges, schema.ordering()), Row::toChange));
  }

  /**
   * Returns the data in this table represented as a {@link ImmutableTable}. Row keys are disjoint
   * range specifications (in order). The returned table has the smallest number of rows necessary
   * to represent the data in this range table. This is useful as a human readable serialized form
   * since any digit sequence in the table is contained in a unique row.
   */
  public ImmutableTable<RangeSpecification, Column<?>, Optional<?>> toImmutableTable() {
    Table<Column<?>, Optional<?>, RangeTree> table = HashBasedTable.create();
    for (Column<?> c : getColumns()) {
      for (Object v : getAssignedValues(c)) {
        table.put(c, Optional.of(v), getRanges(c, v));
      }
      RangeTree unassigned = getAllRanges().subtract(getAssignedRanges(c));
      if (!unassigned.isEmpty()) {
        table.put(c, Optional.empty(), unassigned);
      }
    }
    // Unique changes contain disjoint ranges, each associated with a unique combination of
    // assignments.
    TreeBasedTable<RangeSpecification, Column<?>, Optional<?>> out =
        TreeBasedTable.create(comparing(RangeSpecification::min), schema.ordering());
    for (Change c : toChanges(schema, table, getAllRanges())) {
      List<RangeSpecification> keys = c.getRanges().asRangeSpecifications();
      for (Assignment<?> a : c.getAssignments()) {
        for (RangeSpecification k : keys) {
          out.put(k, a.column(), a.value());
        }
      }
    }
    return ImmutableTable.copyOf(out);
  }

  /**
   * Extracts a map for a single column in this table containing the minimal prefix tree for each
   * of the assigned values. The returned prefixes are the shortest prefixes possible for
   * distinguishing each value in the column. This method is especially useful if you want to
   * categorize partial digit sequences efficiently (i.e. prefix matching).
   *
   * <p>A minimal length can be specified to avoid creating prefixes that are "too short" for some
   * circumstances. Note that returned prefixes are never zero length, so {@code 1} is the lowest
   * meaningful value (although zero is still accepted to imply "no length restriction").
   *
   * <p>Note that for some table data, it is technically impossible to obtain perfect prefix
   * information and in cases where overlap occurs, this method returns the shortest prefixes. This
   * means that for some valid inputs it might be true that more than one prefix is matched. It
   * is therefore up to the caller to determine a "best order" for testing the prefixes if this
   * matters. See {@link PrefixTree#minimal(RangeTree, RangeTree, int)} for more information.
   *
   * <p>An example of an "impossible" prefix would be if "123" has value A, "1234" has value B and
   * "12345" has value A again. In this case there is no prefix which can distinguish A and B
   * (the calculated map would be { "123" ⟹ A, "1234" ⟹ B }). In this situation, testing for the
   * longer prefix would help preserve as much of the original mapping as possible, but it would
   * never be possible to correctly distinguish all inputs.
   */
  public <T extends Comparable<T>> ImmutableMap<T, PrefixTree> getPrefixMap(
      Column<T> column, int minPrefixLength) {
    ImmutableMap.Builder<T, PrefixTree> map = ImmutableMap.builder();
    // Important: Don't just use the assigned ranges in the column, use the assigned ranges of the
    // entire table. This ensures unassigned ranges in the column are not accidentally captured by
    // any of the generated prefixes.
    RangeTree allRanges = getAllRanges();
    for (T value : getAssignedValues(column)) {
      RangeTree include = getRanges(column, value);
      map.put(value, PrefixTree.minimal(include, allRanges.subtract(include), minPrefixLength));
    }
    return map.buildOrThrow();
  }

  // Constants for the simplification routine below.
  // Use -1 for unassigned rows (these are the "overlap" ranges and they don't have an index).
  private static final Column<Integer> INDEX =
      Column.create(Integer.class, "Change Index", -1, Integer::parseInt);
  private static final Schema INDEX_SCHEMA = Schema.builder().add(INDEX).build();

  /**
   * Applies a simplification function to the rows defined by the given columns of this table. The
   * returned table will only have (at most) the specified columns present.
   *
   * <p>The simplification function is used to produce ranges which satisfy some business logic
   * criteria (such as having at most N significant digits, or merging lengths). Range
   * simplification enables easier comparison between data sources of differing precision, and
   * helps to reduce unnecessary complexity in generated regular expressions.
   *
   * <p>The simplification function should return a range that's at least as large as the input
   * range. This is to ensure that simplification cannot unassign ranges, even accidentally. The
   * returned range is automatically restricted to preserve disjoint ranges in the final table.
   *
   * <p>By passing a {@link Change} rather than just a {@link RangeTree}, the simplification
   * function has access to the row assignments for the range it is simplifying. This allows it to
   * select different strategies according to the values in specific columns (e.g. area code
   * length).
   *
   * <p>Note that unassigned ranges in the original table will be preserved and simplified ranges
   * will not overwrite them. This can be useful for defining "no go" ranges which should be left
   * alone.
   */
  public RangeTable simplify(
      Function<Change, RangeTree> simplifyFn,
      int minPrefixLength,
      Column<?> first,
      Column<?>... rest) {
    // Build the single column "index" table (one index for each change) and simplify its ranges.
    // This only works because "toChanges()" produces the minimal set of changes such that each
    // unique combination of assignments appears only once.
    ImmutableList<Change> rows = subTable(getAllRanges(), first, rest).toChanges();
    RangeTable simplifiedIndexTable = simplifyIndexTable(rows, simplifyFn, minPrefixLength);

    // Reconstruct the output table by assigning values from the original change set according to
    // the indices in the simplified index table.
    Builder simplified = RangeTable.builder(getSchema()).add(simplifiedIndexTable.getAllRanges());
    for (int i : simplifiedIndexTable.getAssignedValues(INDEX)) {
      RangeTree simplifiedRange = simplifiedIndexTable.getRanges(INDEX, i);
      for (Assignment<?> a : rows.get(i).getAssignments()) {
        simplified.assign(a, simplifiedRange, OverwriteMode.NEVER);
      }
    }
    return simplified.build();
  }

  /**
   * Helper function to simplify an index table based on the given rows. The resulting table will
   * have a single "index" column with simplified ranges, where the index value {@code N}
   * references the Nth row in the given list of disjoint changes. This is a 3 stage process:
   * <ol>
   *   <li>Step 1: Determine which ranges can overlap with respect to set of range prefixes.
   *   <li>Step 2: Do simplification on the non-overlapping "prefix disjoint" ranges in the table,
   *   which are then be re-partitioned by the disjoint prefixes.
   *   <li>Step 3: Copy over any overlapping ranges from the original table (these don't get
   *   simplified since it's not possible to easily re-pertition them).
   * </ol>
   */
  private static <T extends Comparable<T>> RangeTable simplifyIndexTable(
      ImmutableList<Change> rows, Function<Change, RangeTree> simplifyFn, int minPrefixLength) {
    RangeTable indexTable = makeIndexTable(rows);

    // Step 1: Determine overlapping ranges from the index table, retaining minimum prefix length.
    ImmutableMap<Integer, PrefixTree> nonDisjointPrefixes =
        indexTable.getPrefixMap(INDEX, minPrefixLength);
    // Don't just use the assigned ranges (we need to account for valid but unassigned ranges when
    // determining overlaps).
    RangeTree allRanges = indexTable.getAllRanges();
    RangeTree overlaps = RangeTree.empty();
    for (int n : indexTable.getAssignedValues(INDEX)) {
      RangeTree otherRanges = allRanges.subtract(indexTable.getRanges(INDEX, n));
      overlaps = overlaps.union(nonDisjointPrefixes.get(n).retainFrom(otherRanges));
    }

    // Step 2: Determine the "prefix disjoint" ranges in a new table and simplify it.
    //
    // Before getting the new set of prefixes, add the overlapping ranges back to the table, but
    // without assigning them to anything. This keeps the generated prefixes as long as necessary
    // to avoid creating conflicting assignments for different values. Essentially we're trying to
    // keep ranges "away from" any overlaps. Note however that it is still possible for simplified
    // ranges encroach on the overlapping areas, so we must still forcibly overwrite the original
    // overlapping values after siplification. Consider:
    //   A = { "12x", "12xxx" }, B = { "123x" }
    // where the simplification function just creates any "any" range for all lengths between the
    // minimum and maximum range lengths (e.g. { "123", "45678" } ==> { "xxx", "xxxx", "xxxxx" }.
    //
    // The (non disjoint) prefix table is Pre(A) => { "12" }, Pre(B) => { "123" } and this
    // captures the overlaps:
    //   Pre(A).retainFrom(B) = { "123x" } = B
    //   Pre(B).retainFrom(A) = { "123xx" }
    //
    // Since is of "B" is entirely contained by the overlap, it is not simplified, but A is
    // simplified to:
    //   { "xxx", "xxxx", "xxxxx" }
    // and the re-captured by the "disjoint" prefix (which is still just "12") to:
    //   { "12x", "12xx", "12xxx" }
    //
    // However now, when the original overlaps are added back at the end (in step 3) we find that
    // both "123xx" already exists (with the same index) and "123x" exists with a different index.
    // The resolution is to just overwrite all overlaps back into the table, since these represent
    // the original (unsimplified) values.
    //
    // Thus in this case, the simplified table is:
    //   Sim(A) = { "12x", "12[0-24-9]x", "12xxx" }, Sim(B) = { "123x" }
    //
    // And it is still true that: Sim(A).containsAll(A) and Sim(B).containsAll(B)
    RangeTable prefixDisjointTable = indexTable
        .subTable(allRanges.subtract(overlaps), INDEX)
        .toBuilder()
        .add(overlaps)
        .build();

    // NOTE: Another way to do this would be to implement an "exclusive prefix" method which could
    // be used to immediately return a set of truly "disjoint" prefixes (although this would change
    // the algorithm's behaviour since more ranges would be considered "overlapping" than now).
    // TODO: Experiment with an alternate "exclusive" prefix function.
    ImmutableMap<Integer, PrefixTree> disjointPrefixes = prefixDisjointTable.getPrefixMap(INDEX, 1);
    // Not all values from the original table need be present in the derived table (since some
    // overlaps account for all the ranges of a value).
    Builder simplified = RangeTable.builder(INDEX_SCHEMA);
    for (int n : prefixDisjointTable.getAssignedValues(INDEX)) {
      RangeTree disjointRange = prefixDisjointTable.getRanges(INDEX, n);
      // Pass just the assignments, not the whole row (Change) because that also contains a range,
      // which might not be the same as the disjoint range (so it could be rather confusing).
      PrefixTree disjointPrefix = disjointPrefixes.get(n);
      RangeTree simplifiedRange =
          simplifyFn.apply(Change.of(disjointRange, rows.get(n).getAssignments()));
      // Technically this check is not strictly required, but there's probably no good use-case in
      // which you'd want to remove assignments via the simplification process.
      checkArgument(simplifiedRange.containsAll(disjointRange),
          "simplification should return a superset of the given range\n"
              + "input: %s\n"
              + "output: %s\n"
              + "missing: %s",
          disjointRange, simplifiedRange, disjointRange.subtract(simplifiedRange));
      // Repartition the simplified ranges by the "disjoint" prefixes to restore most of the
      // simplified ranges. These ranges should never overlap with each other.
      RangeTree repartitionedRange = disjointPrefix.retainFrom(simplifiedRange);
      simplified.assign(INDEX, n, repartitionedRange, OverwriteMode.NEVER);
    }

    // Step 3: Copy remaining overlapping ranges from the original table back into the result.
    // Note that we may end up overwriting values here, but that's correct since it restores
    // original "unsimplifiable" ranges.
    for (int n : indexTable.getAssignedValues(INDEX)) {
      simplified.assign(
          INDEX, n, indexTable.getRanges(INDEX, n).intersect(overlaps), OverwriteMode.ALWAYS);
    }
    return simplified.build();
  }

  // Helper to make a table with a single column than references a list of disjoint changes by
  // index (against the range of that change).
  private static RangeTable makeIndexTable(ImmutableList<Change> rows) {
    Builder indexTable = RangeTable.builder(INDEX_SCHEMA);
    for (int i = 0; i < rows.size(); i++) {
      // Empty rows are added to the table, but not assigned an index. Their existence in the index
      // table prevents over simplification from affecting unassigned rows of the original table.
      if (rows.get(i).getAssignments().isEmpty()) {
        indexTable.add(rows.get(i).getRanges());
      } else {
        indexTable.assign(INDEX, i, rows.get(i).getRanges(), OverwriteMode.NEVER);
      }
    }
    return indexTable.build();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RangeTable)) {
      return false;
    }
    RangeTable other = (RangeTable) obj;
    return this == other
        || (schema.equals(other.schema)
            && allRanges.equals(other.allRanges)
            && columnRanges.values().asList().equals(other.columnRanges.values().asList()));
  }

  @Override
  public int hashCode() {
    // This could be memoized if it turns out to be slow.
    return schema.hashCode() ^ columnRanges.hashCode() ^ allRanges.hashCode();
  }

  // TODO: Prettier format for toString().
  @Override
  public final String toString() {
    ImmutableTable<RangeSpecification, Column<?>, Optional<?>> table = toImmutableTable();
    return table.rowMap().entrySet().stream()
        .map(e -> String.format("%s, %s", e.getKey(), rowToString(e.getValue())))
        .collect(joining("\n"));
  }

  private static String rowToString(Map<Column<?>, Optional<?>> r) {
    return r.values().stream()
        .map(v -> v.map(Object::toString).orElse("UNSET"))
        .collect(joining(", "));
  }

  // Helper method to convert a table of values into a minimal set of changes. This is used to
  // turn a single RangeTable into an ImmutableTable, but also to convert a Patch into a minimal
  // sequence of Changes. Each returned "row" defines a range, and a unique sequence of assignments
  // over that range (i.e. no two rows have the same assignments in). The assignments are ordered
  // in column order within each row, and the rows are ordered by the minimum digit sequence in
  // each range and the ranges form a disjoint covering of the ranges in the original table.
  //
  // See go/phonenumber-v2-data-structure for more details.
  private static ImmutableList<Row> toRows(
      Table<Column<?>, Optional<?>, RangeTree> src,
      RangeTree allRanges,
      Comparator<Column<?>> columnOrdering) {
    // Get the non-empty columns in _reverse_ iteration order. We build up rows as a linked list
    // structure, started from the "right hand side". This avoids a lot of copying as new columns
    // are processed.
    ImmutableList<Column<?>> reversedColumns = src.rowMap().entrySet().stream()
        .filter(e -> !e.getValue().isEmpty())
        .map(Entry::getKey)
        .sorted(columnOrdering.reversed())
        .collect(toImmutableList());
    List<Row> uniqueRows = new ArrayList<>();
    uniqueRows.add(Row.empty(allRanges));
    for (Column<?> col : reversedColumns) {
      // Loop backward here so that rows can be (a) removed in place and (b) added at the end.
      for (int i = uniqueRows.size() - 1; i >= 0; i--) {
        Row row = uniqueRows.get(i);
        // Track the unprocessed range for each row as we extend it.
        RangeTree remainder = row.getRanges();
        for (Entry<Optional<?>, RangeTree> e : src.row(col).entrySet()) {
          RangeTree overlap = e.getValue().intersect(remainder);
          if (overlap.isEmpty()) {
            continue;
          }
          // Extend the existing row by the current column value and reduce the remaining ranges.
          uniqueRows.add(Row.of(overlap, col, e.getKey(), row));
          remainder = remainder.subtract(overlap);
          if (remainder.isEmpty()) {
            // We've accounted for all of the existing row in the new column, so remove it.
            uniqueRows.remove(i);
            break;
          }
        }
        if (!remainder.isEmpty()) {
          // The existing row is not completely covered by the new column, so retain what's left.
          uniqueRows.set(i, row.bound(remainder));
        }
      }
    }
    return ImmutableList.sortedCopyOf(comparing(r -> r.getRanges().first()), uniqueRows);
  }

  /**
   * A notional "row" with some set of assignments in a range table or table like structure. Note
   * that a Row can represent unassignment as well as assignment, and not all rows need to contain
   * all columns. Rows are used for representing value in a table, but also changes between tables.
   */
  @AutoValue
  abstract static class Row implements Iterable<Assignment<?>> {
    private static Row empty(RangeTree row) {
      return new AutoValue_RangeTable_Row(row, null);
    }

    private static Row of(RangeTree row, Column<?> col, Optional<?> val, Row next) {
      checkArgument(!row.isEmpty(), "empty ranges not permitted (col=%s, val=%s)", col, val);
      return new AutoValue_RangeTable_Row(
          row, new AutoValue_RangeTable_Cell(Assignment.ofOptional(col, val), next.head()));
    }

    public abstract RangeTree getRanges();
    @Nullable abstract Cell head();

    Change toChange() {
      return Change.of(getRanges(), this);
    }

    private Row bound(RangeTree ranges) {
      return new AutoValue_RangeTable_Row(getRanges().intersect(ranges), head());
    }

    @Override
    public Iterator<Assignment<?>> iterator() {
      return new UnmodifiableIterator<Assignment<?>>() {
        @Nullable private Cell cur = Row.this.head();

        @Override
        public boolean hasNext() {
          return cur != null;
        }

        @Override
        public Assignment<?> next() {
          Cell c = cur;
          if (c == null) {
            throw new NoSuchElementException();
          }
          cur = cur.next();
          return c.assignment();
        }
      };
    }

    @Override
    public final String toString() {
      return "Row{" + getRanges() + " >> " + Iterables.toString(this) + "}";
    }
  }

  @AutoValue
  abstract static class Cell {
    abstract Assignment<?> assignment();
    @Nullable abstract Cell next();
  }
}
