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
import static java.util.stream.Collectors.joining;

import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import javax.annotation.Nullable;

/** A structured exception which should be used whenever structural errors occur in table data. */
public final class RangeException extends IllegalArgumentException {
  // Called when assigning ranges, depending on the overwrite mode. As more cases are added,
  // consider refactoring and subclassing for clean semantics.
  static <T extends Comparable<T>> void checkDisjoint(
      Column<T> column, T value, RangeTree existing, RangeTree ranges, OverwriteMode mode) {
    RangeTree intersection = existing.intersect(ranges);
    if (!intersection.isEmpty()) {
      // A non-empty intersection implies both inputs are also non-empty.
      throw new RangeException(column, value, existing, ranges, intersection, mode);
    }
  }

  RangeException(Column<?> column,
      @Nullable Object value,
      RangeTree existing,
      RangeTree ranges,
      RangeTree intersection,
      OverwriteMode mode) {
    super(explain(checkNotNull(column), value, existing, ranges, intersection, checkNotNull(mode)));
  }

  private static String explain(
      Column<?> column,
      @Nullable Object value,
      RangeTree existing,
      RangeTree ranges,
      RangeTree intersection,
      OverwriteMode mode) {
    return String.format(
        "cannot assign non-disjoint ranges for value '%s' in column '%s' using overwrite mode: %s\n"
            + "overlapping ranges:\n%s"
            + "existing ranges:\n%s"
            + "new ranges:\n%s",
        value, column, mode, toLines(intersection), toLines(existing), toLines(ranges));
  }

  private static String toLines(RangeTree ranges) {
    checkArgument(!ranges.isEmpty());
    return ranges.asRangeSpecifications().stream().map(s -> "  " + s + "\n").collect(joining());
  }

  // We suppress stack traces for "semantic" exceptions, since these aren't intended to indicate
  // bugs, but rather user error (for which a stack trace is not very useful).
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
