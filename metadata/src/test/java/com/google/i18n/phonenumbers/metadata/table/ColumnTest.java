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
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.FIXED_LINE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.UNKNOWN;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_UNKNOWN;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ColumnTest {
  @Test
  public void testBooleanColumn() {
    Column<Boolean> column = Column.ofBoolean("bool");
    assertThat(column.getName()).isEqualTo("bool");
    assertThat(column.type()).isEqualTo(Boolean.class);
    assertThat(column.cast(true)).isTrue();
    assertThrows(ClassCastException.class, () -> column.cast(""));
    // All upper or all lower case are accepted.
    assertThat(column.parse("true")).isTrue();
    assertThat(column.parse("false")).isFalse();
    assertThat(column.parse("TRUE")).isTrue();
    assertThat(column.parse("FALSE")).isFalse();
    assertThat(column.serialize(TRUE)).isEqualTo("true");
    assertThat(column.serialize(FALSE)).isEqualTo("false");
    // We're lenient, but not that lenient.
    assertThrows(IllegalArgumentException.class, () -> column.parse("TruE"));
    assertThrows(IllegalArgumentException.class, () -> column.parse("FaLse"));
    assertThrows(IllegalArgumentException.class, () -> Column.ofBoolean("Foo:Bar"));
  }

  @Test
  public void testStringColumn() {
    Column<String> column = Column.ofString("string");
    assertThat(column.getName()).isEqualTo("string");
    assertThat(column.type()).isEqualTo(String.class);
    assertThat(column.cast("hello")).isEqualTo("hello");
    assertThat(column.parse("")).isNull();
    assertThrows(ClassCastException.class, () -> column.cast(true));
    // Anything other than the empty string is permitted.
    assertThat(column.parse("world")).isEqualTo("world");
    assertThat(column.serialize("world")).isEqualTo("world");
    // Unquoted whitespace is stripped.
    assertThat(column.parse("  world  ")).isEqualTo("world");
    // You can preserve whitespace by surrounding the string with double quotes.
    assertThat(column.parse("\"  world  \"")).isEqualTo("  world  ");
    assertThat(column.serialize("  world  ")).isEqualTo("\"  world  \"");
    // And null is always the empty string.
    assertThat(column.serialize(null)).isEqualTo("");
    assertThrows(IllegalArgumentException.class, () -> Column.ofString("Foo:Bar"));
  }

  @Test
  public void testEnumColumn() {
    Column<ValidNumberType> column = Column.of(ValidNumberType.class, "type", UNKNOWN);
    assertThat(column.getName()).isEqualTo("type");
    assertThat(column.type()).isEqualTo(ValidNumberType.class);
    assertThat(column.cast(FIXED_LINE)).isEqualTo(FIXED_LINE);
    assertThrows(ClassCastException.class, () -> column.cast(""));

    // Several case formats are supported.
    assertThat(column.parse("FIXED_LINE")).isEqualTo(FIXED_LINE);
    assertThat(column.parse("fixed_line")).isEqualTo(FIXED_LINE);
    assertThat(column.parse("fixedLine")).isEqualTo(FIXED_LINE);

    // We're lenient, but not that lenient.
    assertThrows(IllegalArgumentException.class, () -> column.parse("fIxEdLiNe"));
    assertThrows(IllegalArgumentException.class,
        () -> Column.of(XmlNumberType.class, "Foo:Bar", XML_UNKNOWN));
  }
}
