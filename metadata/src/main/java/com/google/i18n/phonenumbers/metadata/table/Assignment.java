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
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A single assignment of a column to a value. This can be used to change values in a
 * {@code RangeTable} and well as query for ranges with its value.
 */
@AutoValue
public abstract class Assignment<T extends Comparable<T>> {
  private static final Splitter SPLITTER = Splitter.on("=").limit(2).trimResults();

  /**
   * Parses a string of the form {@code "<column>=<value>"} to create an assignment using the given
   * schema. The named column must exist in the schema, and the associated value must be a valid
   * value within that column.
   * <p>
   * Whitespace before and after the column or value is ignored. If the value is omitted, then an
   * unassignment is returned.
   */
  public static Assignment<?> parse(String s, Schema schema) {
    List<String> parts = SPLITTER.splitToList(s);
    checkArgument(parts.size() == 2, "invalid assigment string: %s", s);
    Column<?> column = schema.getColumn(parts.get(0));
    return create(column, column.parse(parts.get(1)));
  }

  // Type capture around AutoValue is a little painful, so this static helper ... helps.
  private static <T extends Comparable<T>> Assignment<T> create(Column<T> c, @Nullable Object v) {
    T value = c.cast(v);
    return new AutoValue_Assignment<>(c, Optional.ofNullable(value));
  }

  /**
   * Returns an assignment in the given column for the specified, non null, value.
   * <p>
   * Note that an assignment for the default value of a column will return an explicit assignment
   * for that value, rather than an "unassignment" in that column; so
   * {@code Assignment.of(c, c.defaultValue())} is not equal to {@code unassign(c)}, even though
   * they may have the same effect when applied to a range table, and may even have the same
   * {@link #toString()} representation (in the case of String columns).
   */
  public static <T extends Comparable<T>> Assignment<T> of(Column<T> c, Object v) {
    return new AutoValue_Assignment<>(c, Optional.of(c.cast(v)));
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>> Assignment<T> ofOptional(Column<T> c, Optional<?> v) {
    // Casting the value makes the optional cast below safe.
    v.ifPresent(c::cast);
    return new AutoValue_Assignment<>(c, (Optional<T>) v);
  }

  /**
   * Returns an unassignment in the given column. The {@link #value()} of this assignment is empty.
   */
  public static <T extends Comparable<T>> Assignment<T> unassign(Column<T> c) {
    return new AutoValue_Assignment<>(c, Optional.empty());
  }

  /** The column in which the assignment applies. */
  public abstract Column<T> column();

  /** The value in the column, or empty to signify unassignment. */
  public abstract Optional<T> value();

  @Override
  public final String toString() {
    return String.format("%s=%s", column().getName(), value().map(Object::toString).orElse(""));
  }
}
