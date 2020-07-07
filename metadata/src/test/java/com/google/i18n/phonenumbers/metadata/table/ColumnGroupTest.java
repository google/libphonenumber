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
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ColumnGroupTest {
  @Test
  public void testGroupColumns() {
    Column<Boolean> prototype = Column.ofBoolean("Region");
    ColumnGroup<PhoneRegion, Boolean> group = ColumnGroup.byRegion(prototype);

    Column<Boolean> us = group.getColumnFromId("US");
    assertThat(us.getName()).isEqualTo("Region:US");
    assertThat(us.type()).isEqualTo(Boolean.class);

    Column<Boolean> ca = group.getColumn(PhoneRegion.of("CA"));
    assertThat(ca.getName()).isEqualTo("Region:CA");

    // Only the suffix part should be given to get the column from the group.
    assertThrows(IllegalArgumentException.class, () -> group.getColumnFromId("Region:US"));
  }

  @Test
  public void testExtractGroupColumns() {
    Column<String> first = Column.ofString("FirstColumn");
    Column<String> last = Column.ofString("LastColumn");
    Column<Boolean> prototype = Column.ofBoolean("Region");
    ColumnGroup<PhoneRegion, Boolean> group = ColumnGroup.byRegion(prototype);
    Column<Boolean> us = group.getColumnFromId("US");
    Column<Boolean> ca = group.getColumn(PhoneRegion.of("CA"));

    // The prototype is a valid column, but it's not part of its own group.
    assertThat(group.extractGroupColumns(ImmutableSet.of(first, us, prototype, ca, last)))
        .containsExactly(PhoneRegion.of("US"), us, PhoneRegion.of("CA"), ca).inOrder();
  }
}
