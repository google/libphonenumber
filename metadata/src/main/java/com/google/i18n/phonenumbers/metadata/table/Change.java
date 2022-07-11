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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A change which can be applied to a range table. Changes are applied sequentially to build a
 * range table and new changes overwrite existing mappings. Changes are additive, and cannot be
 * used to remove ranges from a table (but they can unassign previous assignments).
 */
@AutoValue
public abstract class Change {
  private static final Change EMPTY = of(RangeTree.empty(), ImmutableList.of());

  /** A builder for changes that supports assigning and unassigning column values for a range. */
  public static final class Builder {
    private final RangeTree ranges;
    private final Map<Column<?>, Assignment<?>> assignments = new LinkedHashMap<>();

    private Builder(RangeTree ranges) {
      this.ranges = checkNotNull(ranges);
    }

    /**
     * Assigns the optional value in the given column for the ranges of this builder (an empty
     * value has the effect of unassigning the value in the table that this change is applied to).
     */
    public Builder assign(Assignment<?> assignment) {
      checkArgument(assignments.put(assignment.column(), assignment) == null,
          "Column already assigned: %s", assignment.column());
      return this;
    }

    /** Assigns the non-null value in the given column for the ranges of this builder. */
    public Builder assign(Column<?> column, Object value) {
      return assign(Assignment.of(column, value));
    }

    /** Unassigns any values in the given column for the ranges of this builder. */
    public Builder unassign(Column<?> column) {
      return assign(Assignment.unassign(column));
    }

    /** Builds an immutable change from the current state of this builder. */
    public Change build() {
      return Change.of(ranges, assignments.values());
    }
  }

  public static Builder builder(RangeTree ranges) {
    return new Builder(ranges);
  }

  /** Returns the empty change which has no effect when applied to any table. */
  public static Change empty() {
    return EMPTY;
  }

  /** Builds a change from a set of assignments (columns must be unique). */
  public static Change of(RangeTree ranges, Iterable<Assignment<?>> assignments) {
    ImmutableList<Assignment<?>> a = ImmutableList.copyOf(assignments);
    checkArgument(a.size() == a.stream().map(Assignment::column).distinct().count(),
        "cannot supply different assignments for the same column: %s", a);
    return new AutoValue_Change(ranges, a);
  }

  /**
   * Returns the ranges affected by this change. These ranges are added to the table and
   * optionally assigned category values according to {@link #getAssignments()}. No other ranges
   * will be affected by this change.
   */
  public abstract RangeTree getRanges();

  /**
   * Returns a list of assignments to be applied for this change. Note that the set of columns for
   * these assignments is itself also a set (i.e. no two assignments in a change ever share the
   * same column).
   */
  public abstract ImmutableList<Assignment<?>> getAssignments();

  /** Returns whether this change contains any of the specified values in a given column. */
  @SafeVarargs
  public final <T extends Comparable<T>> boolean hasAssignment(Column<T> column, T... values) {
    for (Assignment<?> a : getAssignments()) {
      if (column.equals(a.column())) {
        return a.value().map(v -> Arrays.asList(values).contains(column.cast(v))).orElse(false);
      }
    }
    return false;
  }

  /**
   * Returns the value of the column in this change (or empty if there was not value or the value
   * was empty. This because it conflates "no value" and "explicitly empty value", this method
   * might not be suitable for Changes that unassign values.
   */
  public final <T extends Comparable<T>> Optional<T> getAssignment(Column<T> column) {
    for (Assignment<?> a : getAssignments()) {
      if (column.equals(a.column())) {
        return a.value().map(column::cast);
      }
    }
    return Optional.empty();
  }

  // Visible for AutoValue.
  Change() {}
}
