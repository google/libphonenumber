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
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.UNKNOWN;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SchemaTest {

  private static final Column<ValidNumberType> TYPE =
      Column.of(ValidNumberType.class, "Type", UNKNOWN);
  private static final Column<String> OPERATORS = Column.ofString("Operators");

  private static final ColumnGroup<PhoneRegion, Boolean> REGIONS =
      ColumnGroup.byRegion(Column.ofBoolean("Region"));
  private static final Column<Boolean> REGION_US = REGIONS.getColumn(PhoneRegion.of("US"));
  private static final Column<Boolean> REGION_CA = REGIONS.getColumn(PhoneRegion.of("CA"));

  private static final Column<Boolean> BOGUS = Column.ofBoolean("Bogus");

  private static final Schema SCHEMA =
      Schema.builder().add(TYPE).add(OPERATORS).add(REGIONS).build();

  @Test
  public void testColumnOrdering() {
    assertThat(Stream.of(OPERATORS, REGION_US, TYPE, REGION_CA).sorted(SCHEMA.ordering()))
        .containsExactly(TYPE, OPERATORS, REGION_CA, REGION_US)
        .inOrder();
    // The names are the columns/groups (but not the names of columns in groups, such as
    // "Region:US", since those are functionally generated and aren't known by the schema.
    assertThat(SCHEMA.names()).containsExactly("Type", "Operators", "Region").inOrder();
  }

  @Test
  public void test() {
    assertThat(SCHEMA.getColumn("Type")).isEqualTo(TYPE);
    assertThat(SCHEMA.getColumn("Region:US")).isEqualTo(REGION_US);
    assertThrows(IllegalArgumentException.class, () -> SCHEMA.getColumn("Region"));
    assertThrows(IllegalArgumentException.class, () -> SCHEMA.getColumn("Bogus"));
  }

  @Test
  public void testCheckColumn() {
    assertThat(SCHEMA.checkColumn(TYPE)).isEqualTo(TYPE);
    assertThat(SCHEMA.checkColumn(REGION_US)).isEqualTo(REGION_US);
    assertThrows(IllegalArgumentException.class, () -> SCHEMA.checkColumn(BOGUS));
  }
}
