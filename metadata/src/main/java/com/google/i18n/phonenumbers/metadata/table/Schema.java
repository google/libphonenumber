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
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Comparator;

/**
 * Representation of ordered {@link Column}s in a table. Schemas define columns in both
 * {@code RangeTable} and {@code CsvTable}.
 */
@AutoValue
public abstract class Schema {
  /**
   * Builder for a table schema. Columns are ordered in the order in which they, or their owning
   * group is added to the schema.
   */
  public static final class Builder {
    private final ImmutableSet.Builder<String> names = ImmutableSet.builder();
    private final ImmutableMap.Builder<String, Column<?>> columns = ImmutableMap.builder();
    private final ImmutableMap.Builder<String, ColumnGroup<?, ?>> groups = ImmutableMap.builder();

    /** Adds the given column to the schema. */
    public Builder add(Column<?> column) {
      names.add(column.getName());
      columns.put(column.getName(), column);
      return this;
    }

    /** Adds the given column group to the schema. */
    public Builder add(ColumnGroup<?, ?> group) {
      names.add(group.prototype().getName());
      groups.put(group.prototype().getName(), group);
      return this;
    }

    public Schema build() {
      return new AutoValue_Schema(names.build(), columns.buildOrThrow(), groups.buildOrThrow());
    }
  }

  private static final Schema EMPTY = builder().build();

  /** Returns an empty schema with no assigned columns. */
  public static Schema empty() {
    return EMPTY;
  }

  /** Returns a new schema builder. */
  public static Builder builder() {
    return new Builder();
  }

  // Visible for AutoValue only.
  Schema() {}

  // List of column/group names used to determine column order:
  // E.g. if "names" is: ["col1", "grp1", "col2", "col3"]
  // You can have the table <<"col1", "grp1:xx", "grp1:yy", "col3">>
  // Not all columns need to be present and groups are ordered contiguously as the group prefix
  // appears in the names list.
  abstract ImmutableSet<String> names();
  abstract ImmutableMap<String, Column<?>> columns();
  abstract ImmutableMap<String, ColumnGroup<?, ?>> groups();

  /**
   * Returns the column for the specified key string. For "plain" columns (not in groups) the key
   * is just the column name. For group columns, the key takes the form "prefix:suffix", where the
   * prefix is the name of the "prototype" column, and the "suffix" is an ID of a value within the
   * group. For example:
   *
   * <pre> {@code
   * // Schema has a plain column called "Type" in it.
   * typeCol = table.getColumn("Type");
   *
   * // Schema has a group called "Region" in it which can parse RegionCodes.
   * usRegionCol = table.getColumn("Region:US");
   * }</pre>
   */
  public Column<?> getColumn(String key) {
    int split = key.indexOf(':');
    Column<?> column;
    if (split == -1) {
      column = columns().get(key);
    } else {
      ColumnGroup<?, ?> group = groups().get(key.substring(0, split));
      checkArgument(group != null, "invalid column %s, not in schema: %s", key, this);
      column = group.getColumnFromId(key.substring(split + 1));
    }
    checkArgument(column != null, "invalid column %s, not in schema: %s", key, this);
    return column;
  }

  /** Returns whether the given column is valid within this schema.  */
  public <T extends Comparable<T>> boolean isValidColumn(Column<T> column) {
    int split = column.getName().indexOf(':');
    if (split == -1) {
      return columns().containsValue(column);
    } else {
      ColumnGroup<?, ?> group = groups().get(column.getName().substring(0, split));
      return group != null && column.isIn(group);
    }
  }

  /**
   * Checks whether the given column is valid within this schema, otherwise throws
   * IllegalArgumentException. This is expected to be internal use only, since table users are
   * meant to always know which columns are valid.
   */
  <T extends Comparable<T>> Column<T> checkColumn(Column<T> column) {
    checkArgument(isValidColumn(column), "invalid column %s, not in schema: %s", column, this);
    return column;
  }

  /**
   * Returns whether the this schema has a subset of columns/groups, in the same order as the
   * given schema.
   */
  public boolean isSubSchemaOf(Schema schema) {
    return schema.columns().values().containsAll(columns().values())
        && schema.groups().entrySet().containsAll(groups().entrySet())
        && names().asList().equals(
        schema.names().stream().filter(names()::contains).collect(toImmutableList()));
  }

  /** Returns an ordering for all columns in this schema. */
  public Comparator<Column<?>> ordering() {
    return Comparator
        .comparing(Schema::getPrefix, Ordering.explicit(names().asList()))
        .thenComparing(Schema::getSuffix);
  }

  public ImmutableSet<String> getNames() {
    return names();
  }

  public ImmutableCollection<Column<?>> getColumns() {
    return columns().values();
  }

  private static String getPrefix(Column<?> column) {
    int split = column.getName().indexOf(':');
    return split != -1 ? column.getName().substring(0, split) : column.getName();
  }

  private static String getSuffix(Column<?> column) {
    int split = column.getName().indexOf(':');
    return split == -1 ? "" : column.getName().substring(split + 1);
  }
}
