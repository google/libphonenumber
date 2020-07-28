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
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Maps.transformValues;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nullable;

/**
 * A mapping from category values to a set of disjoint ranges. This is used only by the RangeTable
 * class to represent a column of values.
 */
final class DisjointRangeMap<T extends Comparable<T>> {

  static final class Builder<T extends Comparable<T>> {
    private final Column<T> column;
    private final SortedMap<T, RangeTree> map = new TreeMap<>();
    // Cache of all assigned ranges, used repeatedly by RangeTable . This could be recalculated
    // every time it's needed, but it's just as easy to keep it cached here.
    private RangeTree assignedRanges = RangeTree.empty();

    Builder(Column<T> column) {
      this.column = checkNotNull(column);
    }

    /**
     * Returns the ranges assigned to the given value (returns the empty range if the given value
     * is unassigned in this column). Note that unlike table operations, it makes no sense to allow
     * {@code null} to be used to determine the unassigned ranges, since calculating that requires
     * knowledge of the table in which this column exists.
     */
    RangeTree getRanges(Object value) {
      T checkedValue = column.cast(checkNotNull(value));
      return map.getOrDefault(checkedValue, RangeTree.empty());
    }

    /** Returns the currently assigned ranges for this column. */
    RangeTree getAssignedRanges() {
      return assignedRanges;
    }

    /**
     * Checks whether the "proposed" assignment would succeed with the specified overwrite mode
     * (assignments always succeed if the mode is {@link OverwriteMode#ALWAYS} ALWAYS). If the
     * given value is {@code null} and the mode is not {@code ALWAYS}, this method ensures that
     * none of the given ranges are assigned to any value in this column.
     * <p>
     * This is useful as a separate method when multiple changes are to be made which cannot be
     * allowed to fail halfway through.
     *
     * @throws IllegalArgumentException if the value cannot be added to the column.
     * @throws RangeException if the write is not possible with the given mode.
     */
    T checkAssign(@Nullable Object value, RangeTree ranges, OverwriteMode mode) {
      // Always check the proposed value (for consistency).
      T checkedValue = column.cast(value);
      if (mode != OverwriteMode.ALWAYS) {
        checkArgument(checkedValue != null,
            "Assigning a null value (unassignment) with mode other than ALWAYS makes no sense: %s",
            mode);
        if (mode == OverwriteMode.SAME) {
          // Don't care about ranges that are already in the map.
          ranges = ranges.subtract(map.getOrDefault(checkedValue, RangeTree.empty()));
        }
        RangeException.checkDisjoint(column, checkedValue, assignedRanges, ranges, mode);
      }
      return checkedValue;
    }

    /**
     * Assigns the given ranges to the specified value in this column. After a call to
     * {@code assign()} with a non-null value it is true that:
     * <ul>
     *   <li>The result of {@code getRanges(value)} will contain at least the given ranges.
     *   <li>No ranges assigned to any other category value will intersect with the given ranges.
     * </ul>
     * If ranges are "assigned" to {@code null}, it has the effect of unassigning them.
     *
     * @param value the category value to assign ranges to, or {@code null} to unassign.
     * @param ranges the ranges to assign to the category value with ID {@code id}.
     * @param mode the overwrite mode describing how to handle existing assignments.
     * @throws IllegalArgumentException if the assignment violates the given {@link OverwriteMode}.
     */
    void assign(@Nullable Object value, RangeTree ranges, OverwriteMode mode) {
      T checkedValue = checkAssign(value, ranges, mode);
      // Now unassign the ranges for all other values (only necessary if mode is "ALWAYS" since in
      // other modes we've already ensured there's no intersection).
      if (mode == OverwriteMode.ALWAYS) {
        RangeTree overlap = assignedRanges.intersect(ranges);
        if (!overlap.isEmpty()) {
          for (Entry<T, RangeTree> e : map.entrySet()) {
            // Skip needless extra work for the value we are about to assign.
            if (!e.getKey().equals(checkedValue)) {
              e.setValue(e.getValue().subtract(overlap));
            }
          }
        }
      }
      if (checkedValue != null) {
        map.put(checkedValue, ranges.union(map.getOrDefault(checkedValue, RangeTree.empty())));
        assignedRanges = assignedRanges.union(ranges);
      } else {
        assignedRanges = assignedRanges.subtract(ranges);
      }
    }

    /** Builds the range map. */
    DisjointRangeMap<T> build() {
      return new DisjointRangeMap<T>(column, map, assignedRanges);
    }
  }

  private final Column<T> column;
  private final ImmutableSortedMap<T, RangeTree> map;
  private final RangeTree assignedRanges;

  private DisjointRangeMap(
      Column<T> column, SortedMap<T, RangeTree> map, RangeTree assignedRanges) {
    this.column = checkNotNull(column);
    this.map = ImmutableSortedMap.copyOfSorted(filterValues(map, r -> !r.isEmpty()));
    this.assignedRanges = assignedRanges;
  }

  /**
   * Returns the ranges assigned to the given value.
   *
   * @throws IllegalArgumentException if {@code value} is not a value in this category.
   */
  RangeTree getRanges(Object value) {
    return map.get(column.cast(value));
  }

  /** Returns all values assigned to non-empty ranges in this column. */
  ImmutableSet<T> getAssignedValues() {
    return map.keySet();
  }

  /** Returns the union of all assigned ranges in this column. */
  RangeTree getAssignedRanges() {
    return assignedRanges;
  }

  /** Intersects this column with the given bounds. */
  DisjointRangeMap<T> intersect(RangeTree bounds) {
    return new DisjointRangeMap<T>(
        column, transformValues(map, r -> r.intersect(bounds)), assignedRanges.intersect(bounds));
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DisjointRangeMap<?>)) {
      return false;
    }
    // No need to check "assignedRanges" since it's just a cache of other values anyway.
    DisjointRangeMap<?> other = (DisjointRangeMap<?>) obj;
    return this == other || (column.equals(other.column) && map.equals(other.map));
  }

  @Override
  public int hashCode() {
    return column.hashCode() ^ map.hashCode();
  }
}
