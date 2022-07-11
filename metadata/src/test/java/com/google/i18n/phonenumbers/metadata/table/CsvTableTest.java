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
import static com.google.common.base.StandardSystemProperty.LINE_SEPARATOR;
import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.AREA_CODE_LENGTH;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.COMMENT;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.ExtType.FIXED_LINE;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.ExtType.FIXED_LINE_OR_MOBILE;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.ExtType.MOBILE;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.FORMAT;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.REGIONS;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.TABLE_COLUMNS;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.TYPE;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.toCsv;
import static com.google.i18n.phonenumbers.metadata.model.RangesTableSchema.toRangeTable;
import static com.google.i18n.phonenumbers.metadata.table.CsvTable.DiffMode.ALL;
import static com.google.i18n.phonenumbers.metadata.table.CsvTable.DiffMode.CHANGES;
import static com.google.i18n.phonenumbers.metadata.table.CsvTable.DiffMode.LHS;
import static com.google.i18n.phonenumbers.metadata.table.CsvTable.DiffMode.RHS;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.ExamplesTableSchema;
import com.google.i18n.phonenumbers.metadata.model.ExamplesTableSchema.ExampleNumberKey;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CsvTableTest {
  private static final CsvKeyMarshaller<String> TEST_MARSHALLER =
      CsvKeyMarshaller.ofSortedString("Id");

  private static final Column<Boolean> REGION_CA = REGIONS.getColumn(PhoneRegion.of("CA"));
  private static final Column<Boolean> REGION_US = REGIONS.getColumn(PhoneRegion.of("US"));

  private static final String NEW_LINE = LINE_SEPARATOR.value();

  @Test
  public void testRangeTableExport() throws IOException {
    ImmutableList<Column<?>> columns =
        ImmutableList.of(TYPE, AREA_CODE_LENGTH, REGION_CA, REGION_US, COMMENT);
    RangeTable table = RangeTable.builder(TABLE_COLUMNS)
        .apply(row(columns, key("1", 7), MOBILE, 0, true, true))
        .apply(row(columns, key("2x[34]", 7, 8), FIXED_LINE_OR_MOBILE, 0, true, null, "Foo Bar"))
        .apply(row(columns, key("345", 8), FIXED_LINE, 3, true, null))
        .apply(row(columns, key("456x8", 8), FIXED_LINE, 3, null, true))
        .build();
    CsvTable<RangeKey> csv = toCsv(table);
    assertCsv(csv,
        "Prefix ; Length ; Type                 ; Area Code Length ; Regions ; Comment",
        "1      ; 7      ; MOBILE               ; 0                ; \"CA,US\"",
        "2x[34] ; 7,8    ; FIXED_LINE_OR_MOBILE ; 0                ; \"CA\"    ; \"Foo Bar\"",
        "345    ; 8      ; FIXED_LINE           ; 3                ; \"CA\"",
        "456x8  ; 8      ; FIXED_LINE           ; 3                ; \"US\"");
    assertThat(toRangeTable(csv)).isEqualTo(table);
  }

  @Test
  public void testExampleNumberExport() throws IOException {
    Table<PhoneRegion, ValidNumberType, DigitSequence> table = HashBasedTable.create();
    table.put(PhoneRegion.of("US"), ValidNumberType.TOLL_FREE, DigitSequence.of("800123456"));
    table.put(PhoneRegion.of("US"), ValidNumberType.PREMIUM_RATE, DigitSequence.of("945123456"));
    table.put(PhoneRegion.of("CA"), ValidNumberType.MOBILE, DigitSequence.of("555123456"));
    // Ordering is well defined in the CSV output.
    CsvTable<ExampleNumberKey> csv = ExamplesTableSchema.toCsv(table);
    assertCsv(csv,
        "Region ; Type         ; Number",
        "CA     ; MOBILE       ; \"555123456\"",
        "US     ; TOLL_FREE    ; \"800123456\"",
        "US     ; PREMIUM_RATE ; \"945123456\"");
    assertThat(ExamplesTableSchema.toExampleTable(csv)).isEqualTo(table);
  }

  @Test
  public void testDiff() throws IOException {
    ImmutableList<Column<?>> columns = ImmutableList.of(COMMENT);
    RangeTable lhs = RangeTable.builder(TABLE_COLUMNS)
        .apply(row(columns, key("1", 6), "Left Side Only"))
        .apply(row(columns, key("3", 6), "Left Value"))
        .apply(row(columns, key("4", 6), "Same Value"))
        .build();
    RangeTable rhs = RangeTable.builder(TABLE_COLUMNS)
        .apply(row(columns, key("2", 6), "Right Side Only"))
        .apply(row(columns, key("3", 6), "Right Value"))
        .apply(row(columns, key("4", 6), "Same Value"))
        .build();
    assertCsv(CsvTable.diff(toCsv(lhs), toCsv(rhs), ALL),
        "Diff ; Prefix ; Length ; Comment",
        "---- ; 1      ; 6      ; \"Left Side Only\"",
        "++++ ; 2      ; 6      ; \"Right Side Only\"",
        "<<<< ; 3      ; 6      ; \"Left Value\"",
        ">>>> ; 3      ; 6      ; \"Right Value\"",
        "==== ; 4      ; 6      ; \"Same Value\"");
    assertCsv(CsvTable.diff(toCsv(lhs), toCsv(rhs), CHANGES),
        "Diff ; Prefix ; Length ; Comment",
        "---- ; 1      ; 6      ; \"Left Side Only\"",
        "++++ ; 2      ; 6      ; \"Right Side Only\"",
        "<<<< ; 3      ; 6      ; \"Left Value\"",
        ">>>> ; 3      ; 6      ; \"Right Value\"");
    assertCsv(CsvTable.diff(toCsv(lhs), toCsv(rhs), LHS),
        "Diff ; Prefix ; Length ; Comment",
        "---- ; 1      ; 6      ; \"Left Side Only\"",
        "<<<< ; 3      ; 6      ; \"Left Value\"",
        "==== ; 4      ; 6      ; \"Same Value\"");
    assertCsv(CsvTable.diff(toCsv(lhs), toCsv(rhs), RHS),
        "Diff ; Prefix ; Length ; Comment",
        "++++ ; 2      ; 6      ; \"Right Side Only\"",
        ">>>> ; 3      ; 6      ; \"Right Value\"",
        "==== ; 4      ; 6      ; \"Same Value\"");
  }

  @Test
  public void testEscaping() throws IOException {
    ImmutableList<Column<?>> columns = ImmutableList.of(COMMENT);
    RangeTable table = RangeTable.builder(TABLE_COLUMNS)
        .apply(row(columns, key("1", 6), "Doubling \" Double Quotes"))
        .apply(row(columns, key("2", 6), "Escaping \n Newlines"))
        .apply(row(columns, key("3", 6), "Other \t \\ \r Escaping"))
        .build();
    assertCsv(toCsv(table),
        "Prefix ; Length ; Comment",
        "1      ; 6      ; \"Doubling \"\" Double Quotes\"",
        "2      ; 6      ; \"Escaping \\n Newlines\"",
        "3      ; 6      ; \"Other \\t \\\\ \\r Escaping\"");
  }

  @Test
  public void testOrdering() throws IOException {
    // This came up in relation to discovering that ImmutableSet.copyOf(TreeBasedTable) does not
    // result in rows/columns in the order of the TreeBasedTable's column comparator. Hence the
    // code does a copy via a temporary ImmutableTable.Builder.
    ImmutableList<Column<?>> columns =
        ImmutableList.of(TYPE, AREA_CODE_LENGTH, REGION_US, COMMENT);
    RangeTable table = RangeTable.builder(TABLE_COLUMNS)
        .apply(row(columns, key("1", 4), null, null, null, "Foo Bar"))
        .apply(row(columns, key("2", 4), null, null, true))
        .apply(row(columns, key("3", 4), null, 2))
        .apply(row(columns, key("4", 4), MOBILE))
        .build();
    CsvTable<RangeKey> csv = toCsv(table);
    assertCsv(
        csv,
        "Prefix ; Length ; Type   ; Area Code Length ; Regions ; Comment",
        "1      ; 4      ;        ;                  ;         ; \"Foo Bar\"",
        "2      ; 4      ;        ;                  ; \"US\"",
        "3      ; 4      ;        ; 2",
        "4      ; 4      ; MOBILE");
    assertThat(toRangeTable(csv)).isEqualTo(table);
  }

  // This is (Jan 2019) currently impossible using ImmutableTable.
  @Test
  public void testOptionalRowOrdering() throws IOException {
    CsvKeyMarshaller<Integer> unorderedIntegerMarshaller =
        new CsvKeyMarshaller<>(
            n -> IntStream.of(n).boxed().map(Object::toString),
            p -> Integer.parseInt(p.get(0)),
            Optional.empty(),
            "Unordered");
    CsvSchema<Integer> schema =
        CsvSchema.of(unorderedIntegerMarshaller, RangesTableSchema.SCHEMA.columns());

    CsvTable.Builder<Integer> csv = CsvTable.builder(schema);
    csv.putRow(4, ImmutableMap.of(COMMENT, "Foo Bar"));
    csv.putRow(1, ImmutableMap.of(FORMAT, "Quux"));
    csv.putRow(3, ImmutableMap.of(AREA_CODE_LENGTH, 2));
    csv.putRow(2, ImmutableMap.of(TYPE, MOBILE));

    assertCsv(
        csv.build(),
        "Unordered ; Type   ; Area Code Length ; Format ; Comment",
        "4         ;        ;                  ;        ; \"Foo Bar\"",
        "1         ;        ;                  ; \"Quux\"",
        "3         ;        ; 2",
        "2         ; MOBILE");
  }

  @Test
  public void testUnsafeString() {
    Column<String> unsafe = Column.ofString("unsafe");
    CsvSchema<String> schema = CsvSchema.of(TEST_MARSHALLER, Schema.builder().add(unsafe).build());
    CsvTable<String> csv =
        CsvTable.builder(schema).put("key", unsafe, "Control chars Not \0 Allowed").build();
    assertThrows(IllegalArgumentException.class, () -> export(csv, false));
  }

  private enum Perverse {
    UNSAFE_VALUE() {
      @Override
      public String toString() {
        return "Unsafe ; for \n \"CSV\"";
      }
    };
  }

  @Test
  public void testPerverseEdgeCase() {
    Column<Perverse> unsafe = Column.of(Perverse.class, "Unsafe", Perverse.UNSAFE_VALUE);
    CsvSchema<String> schema = CsvSchema.of(TEST_MARSHALLER, Schema.builder().add(unsafe).build());
    CsvTable<String> csv =
        CsvTable.builder(schema).put("key", unsafe, Perverse.UNSAFE_VALUE).build();
    assertThrows(IllegalArgumentException.class, () -> export(csv, false));
  }

  private static <K> void assertCsv(CsvTable<K> csv, String... lines) throws IOException {
    String aligned = join(lines);
    // Assumes test values don't contain semi-colons where space matters.
    String unaligned = aligned.replaceAll(" *; *", ";");
    String exported = export(csv, true);
    assertThat(exported).isEqualTo(aligned);
    assertThat(export(csv, false)).isEqualTo(unaligned);
    CsvTable<K> imported = CsvTable.importCsv(csv.getSchema(), new StringReader(exported));
    assertThat(csv).isEqualTo(imported);
  }

  private static String export(CsvTable<?> csv, boolean align) {
    StringWriter out = new StringWriter();
    csv.exportCsv(new PrintWriter(out), align);
    return out.toString();
  }

  private static Change row(ImmutableList<Column<?>> columns, RangeKey key, Object... values) {
    Change.Builder row = Change.builder(key.asRangeTree());
    checkArgument(values.length <= columns.size());
    int n = 0;
    for (Object v : values) {
      if (v != null) {
        Column<?> c = columns.get(n);
        row.assign(c, c.cast(v));
      }
      n++;
    }
    return row.build();
  }

  private static String join(String... lines) {
    return String.join(NEW_LINE, lines) + NEW_LINE;
  }

  private static RangeKey key(String spec, Integer... lengths) {
    RangeSpecification prefix =
        spec.isEmpty() ? RangeSpecification.empty() : RangeSpecification.parse(spec);
    return RangeKey.create(prefix, ImmutableSet.copyOf(lengths));
  }
}

