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
import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static java.util.function.Function.identity;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableBiMap;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.i18n.SimpleLanguageTag;
import java.util.Set;
import java.util.function.Function;

/** A group of {@link RangeTable} columns. */
@AutoValue
public abstract class ColumnGroup<K, T extends Comparable<T>> {
  /**
   * Returns a group for columns with the same type as the given "prototype" column and which has a
   * a prefix that's the name of the prototype. Suffix values are parsed using the given function.
   */
  public static <K, T extends Comparable<T>> ColumnGroup<K, T> of(
      Column<T> prototype, Function<String, K> parseFn) {
    return new AutoValue_ColumnGroup<>(prototype, parseFn);
  }

  /** Returns a group for the specified prototype column keyed by {@link PhoneRegion}. */
  public static <T extends Comparable<T>> ColumnGroup<PhoneRegion, T> byRegion(
      Column<T> prototype) {
    return of(prototype, PhoneRegion::of);
  }

  /** Returns a group for the specified prototype column keyed by {@link SimpleLanguageTag}. */
  public static <T extends Comparable<T>> ColumnGroup<SimpleLanguageTag, T> byLanguage(
      Column<T> prototype) {
    return of(prototype, SimpleLanguageTag::of);
  }

  // Internal use only.
  abstract Column<T> prototype();
  abstract Function<String, K> parseFn();

  /** Returns the column for a specified key. */
  public Column<T> getColumn(K key) {
    // The reason this does not just call "prototype().fromPrototype(...)" is that the key may not
    // be parsable by the function just because it's the "right" type. This allows people to pass
    // in a function that limits columns to some subset of the domain (e.g. a subset of region
    // codes).
    return getColumnFromId(key.toString());
  }

  /** Returns the column for a specified ID string. */
  public Column<T> getColumnFromId(String id) {
    try {
      Object unused = parseFn().apply(id);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          String.format("invalid column %s, not in group: %s", id, this), e);
    }
    return prototype().fromPrototype(id);
  }

  /** Returns the key of a column in this group. */
  @SuppressWarnings("unchecked")
  public K getKey(Column<?> c) {
    checkArgument(c.isIn(this), "column %s in not group %s", c, this);
    // Cast is safe since any column in this group is a Column<T>.
    return extractKey((Column<T>) c);
  }

  /** Returns a bidirectional mapping from group key to column, for columns in this group. */
  @SuppressWarnings("unchecked")
  public ImmutableBiMap<K, Column<T>> extractGroupColumns(Set<Column<?>> columns) {
    return columns.stream()
        .filter(c -> c.isIn(this))
        // Cast is safe since any column in this group is a Column<T>.
        .map(c -> (Column<T>) c)
        .collect(toImmutableBiMap(this::extractKey, identity()));
  }

  // Assumes we've already verified that the column is in this group.
  private K extractKey(Column<T> column) {
    String name = column.getName();
    return parseFn().apply(name.substring(name.lastIndexOf(':') + 1));
  }
}
