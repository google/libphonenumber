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
package com.google.i18n.phonenumbers.metadata.testing;

import static com.google.common.base.Strings.lenientFormat;
import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import java.util.Optional;
import javax.annotation.Nullable;

/** A Truth subject for asserting on {@link RangeTable} instances. */
public class RangeTableSubject extends Subject {
  /** Returns Truth subject for asserting on a {@link RangeTable}. */
  public static RangeTableSubject assertThat(@Nullable RangeTable table) {
    return assertAbout(RangeTableSubject.SUBJECT_FACTORY).that(table);
  }

  private static final Factory<RangeTableSubject, RangeTable> SUBJECT_FACTORY =
      RangeTableSubject::new;

  private final RangeTable actual;

  private RangeTableSubject(FailureMetadata failureMetadata, @Nullable RangeTable subject) {
    super(failureMetadata, subject);
    this.actual = subject;
  }

  // Add more methods below as needed.

  /** Asserts that the table is empty. */
  public void isEmpty() {
    if (!actual.isEmpty()) {
      failWithActual(simpleFact("expected to be empty"));
    }
  }

  /** Asserts that the table has exactly the given columns in the given order (and no others). */
  public void hasColumns(Column<?>... columns) {
    check("getColumns()").that(actual.getColumns()).containsExactlyElementsIn(asList(columns));
  }

  /** Asserts that the table has the specified number of rows. */
  public void hasRowCount(int count) {
    check("toImmutableTable().rowKeySet().size()")
        .that(actual.toImmutableTable().rowKeySet().size())
        .isEqualTo(count);
  }

  /**
   * Asserts the specified range has the given values for each column. All columns need to be
   * specified, with {@code null} meanings "no value present". This method does not ensure that no
   * other ranges were also assigned the same values, so for complete coverage in a test it's best
   * to use this in conjunction with something like {@link #allRanges()}.
   */
  public void hasRanges(String spec, Object... values) {
    ImmutableTable<RangeSpecification, Column<?>, Optional<?>> table =
        this.actual.toImmutableTable();
    RangeSpecification rowKey = RangeSpecification.parse(spec);
    if (!table.rowKeySet().contains(rowKey)) {
      failWithoutActual(
          simpleFact(
              lenientFormat(
                  "specified row %s does not exist in the table: rows=%s",
                  rowKey, table.rowKeySet())));
    }
    ImmutableMap<Column<?>, Optional<?>> row = table.row(rowKey);
    if (row.size() != values.length) {
      failWithoutActual(
          simpleFact(
              lenientFormat(
                  "incorrect number of columns: expected %s, got %s", row.size(), values.length)));
    }
    int n = 0;
    for (Optional<?> actual : row.values()) {
      Object expected = values[n++];
      if (actual.isPresent()) {
        if (!actual.get().equals(expected)) {
          failWithoutActual(
              simpleFact(
                  lenientFormat("unexpected value in row: expected %s, got %s", expected, actual)));
        }
      } else if (expected != null) {
        failWithoutActual(simpleFact(lenientFormat("missing value in row: expected %s", expected)));
      }
    }
  }

  /**
   * Returns a {@link RangeTreeSubject} for asserting about the ranges assigned to the given value
   * in the specified column.
   */
  public RangeTreeSubject assigned(Column<?> column, Object value) {
    return RangeTreeSubject.assertWithMessageThat(
        actual.getRanges(column, value), "%s in column %s", value, column);
  }

  /**
   * Returns a {@link RangeTreeSubject} for asserting about all ranges assigned in the specified
   * column.
   */
  public RangeTreeSubject assigned(Column<?> column) {
    return RangeTreeSubject.assertWithMessageThat(
        actual.getAssignedRanges(column), "column %s", column);
  }

  /** Returns a {@link RangeTreeSubject} for asserting about all ranges in the table. */
  public RangeTreeSubject allRanges() {
    return RangeTreeSubject.assertWithMessageThat(actual.getAllRanges(), "all ranges");
  }
}
