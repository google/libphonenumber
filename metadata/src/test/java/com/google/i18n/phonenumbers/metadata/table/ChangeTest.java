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
import static com.google.i18n.phonenumbers.metadata.testing.RangeTreeSubject.assertThat;
import static java.util.Arrays.asList;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ChangeTest {
  private static final Column<String> COL_A = Column.ofString("A");
  private static final Column<String> COL_B = Column.ofString("B");

  @Test
  public void testEmpty() {
    assertThat(Change.empty().getRanges()).isEmpty();
    assertThat(Change.empty().getAssignments()).isEmpty();
    // Not all "no-op" changes are equal to the "empty" change (unlike RangeTree). This should be
    // fine however since Changes are expected to have a very short lifecycle in most code and not
    // be used as keys in maps etc...
    assertThat(Change.empty())
        .isNotEqualTo(Change.builder(RangeTree.empty()).assign(COL_A, "foo").build());
    assertThat(Change.empty()).isNotEqualTo(Change.builder(ranges("12xxxx")).build());
  }

  @Test
  public void testBuilder() {
    Change c = Change.builder(ranges("12xxxx")).assign(COL_A, "foo").assign(COL_B, "bar").build();
    assertThat(c.getRanges()).containsExactly("12xxxx");
    Assignment<String> assignFoo = Assignment.of(COL_A, "foo");
    Assignment<String> assignBar = Assignment.of(COL_B, "bar");
    assertThat(c.getAssignments()).containsExactly(assignFoo, assignBar);
    assertThat(c).isEqualTo(Change.of(ranges("12xxxx"), asList(assignFoo, assignBar)));
    // Don't allow same column twice (this could be relaxed in future if necessary)!
    assertThrows(IllegalArgumentException.class,
        () -> Change.builder(ranges("12xxxx")).assign(COL_A, "foo").assign(COL_A, "bar").build());
  }

  @Test
  public void testBuilderUnassignment() {
    Change c = Change.builder(ranges("12xxxx")).unassign(COL_A).build();
    Assignment<String> unassign = Assignment.unassign(COL_A);
    assertThat(c.getAssignments()).containsExactly(unassign);
    assertThat(c).isEqualTo(Change.of(ranges("12xxxx"), asList(unassign)));
  }

  private static RangeTree ranges(String... rangeSpecs) {
    return RangeTree.from(Arrays.stream(rangeSpecs).map(RangeSpecification::parse));
  }
}
