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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AssignmentTest {
  private static final Column<String> COL_A = Column.ofString("A");
  private static final Column<String> COL_B = Column.ofString("B");
  private static final Column<Integer> COL_X = Column.ofUnsignedInteger("X");

  private static final Schema SCHEMA = Schema.builder().add(COL_A).add(COL_B).add(COL_X).build();

  @Test
  public void testParsing() {
    assertAssignment(Assignment.parse("A=foo", SCHEMA), COL_A, "foo");
    assertAssignment(Assignment.parse(" B = bar ", SCHEMA), COL_B, "bar");
    assertUnassignment(Assignment.parse("A=", SCHEMA), COL_A);
    assertAssignment(Assignment.parse("X=23", SCHEMA), COL_X, 23);
    assertThrows(IllegalArgumentException.class, () -> Assignment.parse("C=Nope", SCHEMA));
    assertThrows(IllegalArgumentException.class, () -> Assignment.parse("X=NaN", SCHEMA));
  }

  @Test
  public void testOf() {
    assertAssignment(Assignment.of(COL_A, "foo"), COL_A, "foo");
    assertThat(Assignment.of(COL_A, "foo")).isNotEqualTo(Assignment.of(COL_A, "bar"));
    assertThat(Assignment.of(COL_A, "")).isNotEqualTo(Assignment.of(COL_B, ""));
    assertThat(Assignment.of(COL_A, COL_A.defaultValue())).isNotEqualTo(Assignment.unassign(COL_A));
    assertThrows(NullPointerException.class, () -> Assignment.of(COL_A, null));
  }

  @Test
  public void testUnassign() {
    // Not much else to do here...
    assertThat(Assignment.unassign(COL_A)).isEqualTo(Assignment.unassign(COL_A));
    assertUnassignment(Assignment.unassign(COL_A), COL_A);
  }

  private static <T extends Comparable<T>> void assertAssignment(
      Assignment<?> a, Column<T> c, T v) {
    assertThat(a.column()).isSameInstanceAs(c);
    assertThat(a.value()).hasValue(v);
  }

  private static void assertUnassignment(Assignment<?> a, Column<?> c) {
    assertThat(a.column()).isSameInstanceAs(c);
    assertThat(a.value()).isEmpty();
  }
}
