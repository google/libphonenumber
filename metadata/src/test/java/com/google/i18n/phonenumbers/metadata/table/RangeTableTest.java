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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.FIXED_LINE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.MOBILE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.PREMIUM_RATE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.SHARED_COST;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.TOLL_FREE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.UNKNOWN;
import static com.google.i18n.phonenumbers.metadata.testing.RangeTableSubject.assertThat;
import static com.google.i18n.phonenumbers.metadata.testing.RangeTreeSubject.assertThat;
import static java.util.stream.IntStream.rangeClosed;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;
import com.google.i18n.phonenumbers.metadata.PrefixTree;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RangeTableTest {

  private static final Column<ValidNumberType> TYPE =
      Column.of(ValidNumberType.class, "Type", UNKNOWN);
  public static final Column<Integer> AREA_CODE_LENGTH = Column.ofUnsignedInteger("AreaCodeLength");

  private static final ColumnGroup<PhoneRegion, Boolean> REGIONS =
      ColumnGroup.byRegion(Column.ofBoolean("Region"));
  private static final Column<Boolean> REGION_US = REGIONS.getColumn(PhoneRegion.of("US"));
  private static final Column<Boolean> REGION_CA = REGIONS.getColumn(PhoneRegion.of("CA"));

  private static final Schema SCHEMA =
      Schema.builder().add(TYPE).add(AREA_CODE_LENGTH).add(REGIONS).build();

  // This is essentially the most "extreme" simplification you can have. All detail is removed and
  // lengths are merged into a contiguous range. It's basically like turning a range into "\d{n,m}"
  // For example, { "123", "12345" } becomes { "xxx", "xxxx", "xxxxx" }.
  private static final Function<Change, RangeTree> EXTREME_SIMPLIFICATION =
      c -> RangeTree.from(
          rangeClosed(c.getRanges().getLengths().first(), c.getRanges().getLengths().last())
              .mapToObj(RangeSpecification::any));

  @Test
  public void testEmptyMap() {
    RangeTable table = RangeTable.builder(SCHEMA).build();
    assertThat(table).isEmpty();
  }

  @Test
  public void testBasicAssign() {
    RangeTable.Builder table = RangeTable.builder(SCHEMA);

    table.assign(TYPE, MOBILE, ranges("1[234]xxxx"), OverwriteMode.ALWAYS);
    assertThat(table.getRanges(TYPE, MOBILE)).isEqualTo(ranges("1[234]xxxx"));

    table.assign(TYPE, null, ranges("13xxxx"), OverwriteMode.ALWAYS);
    assertThat(table.getRanges(TYPE, MOBILE)).isEqualTo(ranges("1[24]xxxx"));

    Assignment<ValidNumberType> fixedLine = Assignment.of(TYPE, FIXED_LINE);

    // Overwrite an existing range.
    table.assign(fixedLine, ranges("14xxxx"), OverwriteMode.ALWAYS);
    assertThat(table.getRanges(TYPE, MOBILE)).isEqualTo(ranges("12xxxx"));
    assertThat(table.getRanges(TYPE, FIXED_LINE)).isEqualTo(ranges("14xxxx"));

    // Partially overwrite an existing range (same value).
    table.assign(fixedLine, ranges("1[34]xxxx"), OverwriteMode.SAME);
    assertThat(table.getRanges(TYPE, MOBILE)).isEqualTo(ranges("12xxxx"));
    assertThat(table.getRanges(TYPE, FIXED_LINE)).isEqualTo(ranges("1[34]xxxx"));

    // Fail to overwrite range with a different value in "SAME" mode.
    assertThrows(IllegalArgumentException.class,
        () -> table.assign(fixedLine, ranges("1[23]xxxx"), OverwriteMode.SAME));

    // Add new ranges (but never overwriting).
    table.assign(fixedLine, ranges("15xxxx"), OverwriteMode.NEVER);
    assertThat(table.getRanges(TYPE, MOBILE)).isEqualTo(ranges("12xxxx"));
    assertThat(table.getRanges(TYPE, FIXED_LINE)).isEqualTo(ranges("1[3-5]xxxx"));

    // Fail to write ranges with the same value in "NEVER" mode.
    assertThrows(IllegalArgumentException.class,
        () -> table.assign(fixedLine, ranges("15xxxx"), OverwriteMode.NEVER));

    // Unassignment (null value) makes no sense for modes other than "ALWAYS".
    // TODO: This highlights the way this API is bad, make a separate "unassign" method.
    assertThrows(IllegalArgumentException.class,
        () -> table.assign(TYPE, null, ranges("123"), OverwriteMode.SAME));
    assertThrows(IllegalArgumentException.class,
        () -> table.assign(TYPE, null, ranges("123"), OverwriteMode.NEVER));
  }

  @Test
  public void testApplyChanges() {
    // Changes ordered top-to-bottom.
    RangeTable table = RangeTable.builder(SCHEMA)
        .apply(assign(
            ranges("[18]2xxxxx"), ImmutableMap.of(TYPE, MOBILE, AREA_CODE_LENGTH, 3)))
        .apply(assign(ranges("7xxxxxx"), TYPE, MOBILE))
        .apply(assign(ranges("[1-3]xxxxxx"), TYPE, FIXED_LINE))
        .build();
    // The union of all the ranges.
    assertThat(table).allRanges().containsExactly("[1-37]xxxxxx", "82xxxxx");
    // The ranges assigned for various columns.
    assertThat(table).assigned(TYPE).containsExactly("[1-37]xxxxxx", "82xxxxx");
    assertThat(table).assigned(AREA_CODE_LENGTH).containsExactly("[18]2xxxxx");

    // Note that the 12xxxxx range is replaced by the fixed line in the type map.
    assertThat(table).assigned(TYPE, FIXED_LINE).containsExactly("[1-3]xxxxxx");
    assertThat(table).assigned(TYPE, MOBILE).containsExactly("7xxxxxx", "82xxxxx");
    // Area code length unaffected by update of the 12xxxxx range (only type was affected).
    assertThat(table).assigned(AREA_CODE_LENGTH, 3).containsExactly("[18]2xxxxx");
  }

  @Test
  public void testBareRangeAddition() {
    RangeTable table = RangeTable.builder(SCHEMA)
        .add(ranges("1xxxxx"))
        .apply(assign(ranges("12xxxx"), TYPE, MOBILE))
        .build();
    assertThat(table).allRanges().containsExactly("1xxxxx");
    // Note that there is not "getUnassignedRanges()" on RangeTable (yet), so we fudge it by
    // checking that there's only one column and looking at all the assigned ranges in it.
    assertThat(table).hasColumns(TYPE);
    assertThat(table).assigned(TYPE).containsExactly("12xxxx");

    // Also check that the re-built builder remembers the unassigned ranges.
    RangeTable.Builder builder = table.toBuilder();
    assertThat(builder.getAllRanges()).containsExactly("1xxxxx");
    assertThat(builder.getAssignedRanges(TYPE)).containsExactly("12xxxx");
  }

  @Test
  public void testAssignAndUnassign() {
    RangeTable table = RangeTable.builder(SCHEMA)
        .apply(assign(ranges("1xxxxx"), TYPE, MOBILE))
        .apply(unassign(ranges("1[0-4]xxxx"), TYPE))
        .build();
    assertThat(table).allRanges().containsExactly("1xxxxx");
    assertThat(table).hasColumns(TYPE);
    assertThat(table).assigned(TYPE).containsExactly("1[5-9]xxxx");

    // Also check that the re-built builder remembers the unassigned ranges.
    RangeTable.Builder builder = table.toBuilder();
    assertThat(builder.getAllRanges()).containsExactly("1xxxxx");
    assertThat(builder.getAssignedRanges(TYPE)).containsExactly("1[5-9]xxxx");
  }

  @Test
  public void testAssignAndRemove() {
    RangeTable table = RangeTable.builder(SCHEMA)
        .apply(assign(ranges("1xxxxx"), TYPE, MOBILE))
        .remove(ranges("1[5-9]xxxx"))
        .build();
    assertThat(table).allRanges().containsExactly("1[0-4]xxxx");
    assertThat(table).hasColumns(TYPE);
    assertThat(table).assigned(TYPE).containsExactly("1[0-4]xxxx");

    RangeTable.Builder builder = table.toBuilder();
    assertThat(builder.getAllRanges()).containsExactly("1[0-4]xxxx");
    assertThat(builder.getAssignedRanges(TYPE)).containsExactly("1[0-4]xxxx");
  }

  @Test
  public void testTableImportExport() {
    RangeTable original = RangeTable.builder(SCHEMA)
        .apply(assign(ranges("[13]xxxxxx"), TYPE, MOBILE))
        .apply(assign(ranges("[24]xxxxxx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("[14]xxxxxx"), AREA_CODE_LENGTH, 3))
        .apply(assign(ranges("[23]xxxxxx"), AREA_CODE_LENGTH, 2))
        .build();

    Table<RangeSpecification, Column<?>, Optional<?>> exported = original.toImmutableTable();
    assertThat(exported).hasSize(8);
    assertThat(exported).containsCell(assigned("1xxxxxx", TYPE, MOBILE));
    assertThat(exported).containsCell(assigned("1xxxxxx", AREA_CODE_LENGTH, 3));
    assertThat(exported).containsCell(assigned("2xxxxxx", TYPE, FIXED_LINE));
    assertThat(exported).containsCell(assigned("2xxxxxx", AREA_CODE_LENGTH, 2));
    assertThat(exported).containsCell(assigned("3xxxxxx", TYPE, MOBILE));
    assertThat(exported).containsCell(assigned("3xxxxxx", AREA_CODE_LENGTH, 2));
    assertThat(exported).containsCell(assigned("4xxxxxx", TYPE, FIXED_LINE));
    assertThat(exported).containsCell(assigned("4xxxxxx", AREA_CODE_LENGTH, 3));

    RangeTable imported = RangeTable.from(SCHEMA, exported);
    assertThat(imported).isEqualTo(original);
    assertThat(imported.toImmutableTable()).isEqualTo(exported);
  }

  @Test
  public void testColumnGroupMapping() {
    // Changes ordered top-to-bottom.
    RangeTable table = RangeTable.builder(SCHEMA)
        .apply(assign(ranges("1xxxxx"), ImmutableMap.of(REGION_US, true)))
        .apply(assign(ranges("2xxxxx"), ImmutableMap.of(REGION_CA, true)))
        .apply(assign(ranges("3xxxxx"), ImmutableMap.of(REGION_US, true, REGION_CA, true)))
        .build();
    // The union of all the ranges.
    assertThat(table).allRanges().containsExactly("[1-3]xxxxx");
    Map<PhoneRegion, Column<Boolean>> regionMap = REGIONS.extractGroupColumns(table.getColumns());
    assertThat(regionMap.keySet()).containsExactly(PhoneRegion.of("US"), PhoneRegion.of("CA"));
    assertThat(table.getAssignedRanges(regionMap.get(PhoneRegion.of("US")))).containsExactly("[13]xxxxx");
    assertThat(table.getAssignedRanges(regionMap.get(PhoneRegion.of("CA")))).containsExactly("[23]xxxxx");
    // If a column in a group is not present, it counts as having no ranges, but if a plain column
    // is not in the schema at all, it's an error.
    assertThat(table.getAssignedRanges(REGIONS.getColumn(PhoneRegion.of("CH")))).isEmpty();
    Column<String> bogus = Column.ofString("Bogus");
    assertThrows(IllegalArgumentException.class, () -> table.getAssignedRanges(bogus));
    Column<String> nope = ColumnGroup.byRegion(bogus).getColumn(PhoneRegion.of("US"));
    assertThrows(IllegalArgumentException.class, () -> table.getAssignedRanges(nope));
  }

  @Test
  public void testSubTable() {
    RangeTable original = RangeTable.builder(SCHEMA)
        .apply(assign(ranges("[13]xxxxxx"), TYPE, MOBILE))
        .apply(assign(ranges("[24]xxxxxx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("[14]xxxxxx"), AREA_CODE_LENGTH, 3))
        .apply(assign(ranges("[23]xxxxxx"), AREA_CODE_LENGTH, 2))
        .build();
    // Restrict to the ranges in which area code length is 2, but keep only the type column.
    RangeTable subTable = original.subTable(original.getRanges(AREA_CODE_LENGTH, 2), TYPE);

    assertThat(subTable).hasColumns(TYPE);
    assertThat(subTable).hasRowCount(2);
    assertThat(subTable).hasRanges("2xxxxxx", FIXED_LINE);
    assertThat(subTable).hasRanges("3xxxxxx", MOBILE);
  }

  @Test
  public void testGetPrefixMap() {
    RangeTable table = RangeTable.builder(SCHEMA)
        .apply(assign(ranges("1234xxxx", "1256xxxx"), TYPE, MOBILE))
        .apply(assign(ranges("1236xxx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("4xxxx"), TYPE, TOLL_FREE))
        .apply(assign(ranges("49xxxx"), TYPE, PREMIUM_RATE))
        .build();

    ImmutableMap<ValidNumberType, PrefixTree> map = table.getPrefixMap(TYPE, 0);

    assertThat(map).containsEntry(MOBILE, PrefixTree.from(ranges("1234", "125")));
    assertThat(map).containsEntry(FIXED_LINE, PrefixTree.from(ranges("1236")));
    // The ranges 4xxxx and 49xxxx overlap (since 49 is a prefix for both) and the prefix map
    // contains the shortest unique prefix for each range. The mapping from TOLL_FREE could not
    // contain only "4[0-8]" since that would not match "49123". Overlapping range lengths with
    // different types is thus highly problematic, but the prefix map will contain mappings for
    // both, and it's up to the caller to handle this, possibly by ordering any checks made.
    assertThat(map).containsEntry(TOLL_FREE, PrefixTree.from(ranges("4")));
    assertThat(map).containsEntry(PREMIUM_RATE, PrefixTree.from(ranges("49")));
  }

  @Test
  public void testGetPrefixMap_minLength() {
    RangeTable table = RangeTable.builder(SCHEMA)
        .apply(assign(ranges("123xxxxx", "1256xxxx"), TYPE, MOBILE))
        .apply(assign(ranges("124xxx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("4xxxx"), TYPE, TOLL_FREE))
        .apply(assign(ranges("49xxxx"), TYPE, PREMIUM_RATE))
        .build();

    ImmutableMap<ValidNumberType, PrefixTree> map = table.getPrefixMap(TYPE, 3);

    assertThat(map).containsEntry(MOBILE, PrefixTree.from(ranges("12[35]")));
    assertThat(map).containsEntry(FIXED_LINE, PrefixTree.from(ranges("124")));
    assertThat(map).containsEntry(TOLL_FREE, PrefixTree.from(ranges("4")));
    assertThat(map).containsEntry(PREMIUM_RATE, PrefixTree.from(ranges("49")));
  }

  @Test
  public void testSimplify_multipleColumns() {
    RangeTable table = RangeTable.builder(SCHEMA)
        // This can't be simplified since expanding any of the area code length ranges will overlap
        // (possibly with the unassigned area code length ranges).
        .apply(assign(ranges("1[0-4]x_xxxx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("12x_xxxx"), AREA_CODE_LENGTH, 2))
        .apply(assign(ranges("123_xxxx"), AREA_CODE_LENGTH, 3))
        .apply(assign(ranges("123_4xxx"), AREA_CODE_LENGTH, 4))
        // This can be simplified since it expands into "empty" ranges.
        .apply(assign(ranges("156_xxxx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("156_xxxx"), AREA_CODE_LENGTH, 3))
        .apply(assign(ranges("234_xxxx"), TYPE, MOBILE))
        // This should be ignored since simplification happens only on the other columns.
        .apply(assign(ranges("[12]23_xxxx"), REGION_CA, true))
        .build();

    RangeTable simplified =
        table.simplify(c -> c.getRanges().significantDigits(2), 0, TYPE, AREA_CODE_LENGTH);

    assertThat(simplified).hasColumns(TYPE, AREA_CODE_LENGTH);
    // The 156 range got pulled back to 2 digits (the other was already 2 digits).
    assertThat(simplified).assigned(TYPE, FIXED_LINE).containsExactly("1[0-4]x_xxxx", "15x_xxxx");
    // The 234 range got pulled back to 2 digits.
    assertThat(simplified).assigned(TYPE, MOBILE).containsExactly("23x_xxxx");
    assertThat(simplified).assigned(AREA_CODE_LENGTH, 2).containsExactly("12[0-24-9]_xxxx");
    // The 123 ranges were preserved, but the 156 range was pulled back to 2 digits.
    assertThat(simplified).assigned(AREA_CODE_LENGTH, 3)
        .containsExactly("123_[0-35-9]xxx", "15x_xxxx");
    assertThat(simplified).assigned(AREA_CODE_LENGTH, 4).containsExactly("123_4xxx");
  }

  @Test
  public void testSimplify_chineseRanges() {
    // This mimics real data found in the CN regular expression whereby a SHARED_COST range
    // partially overlaps with the fixed line prefixes.
    RangeTable table = RangeTable.builder(SCHEMA)
        // The pattern is:
        // abc    | length=10  | FIXED_LINE
        // abc100 | length=8   | FIXED_LINE
        // abc95  | length=8,9 | FIXED_LINE
        // abc96  | length=8,9 | SHARED_COST
        .apply(assign(ranges("123_xxx_xxxx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("123_100xx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("123_95xxx", "123_95xxxx"), TYPE, FIXED_LINE))
        .apply(assign(ranges("123_96xxx", "123_96xxxx"), TYPE, SHARED_COST))
        // Just add a range that sits "either side" of what's being simplified to ensure it
        // doesn't "leak".
        .apply(assign(ranges("1[13]4_56xx_xxxx"), TYPE, MOBILE))
        .build();

    RangeTable simplified = table.simplify(c -> c.getRanges().significantDigits(3), 0, TYPE);

    // The simplification function just takes the first 3 significant digits. If the "shared cost"
    // ranges were not overlapping, this would result in a "fixed line" range of "123xxx..." with
    // lengths 8,9,10. However to avoid corrupting the shared cost range, we end up with:
    // abc          | length=10  | FIXED_LINE
    // abc[0-8]     | length=8,9 | FIXED_LINE
    // abc9[0-57-9] | length=8,9 | FIXED_LINE
    // abc96        | length=8,9 | SHARED_COST
    assertThat(simplified).hasColumns(TYPE);
    assertThat(simplified).assigned(TYPE, FIXED_LINE).containsExactly(
        "123_xxx_xxxx",
        "123_[0-8]xx_xx",
        "123_[0-8]xx_xxx",
        "123_9[0-57-9]x_xx",
        "123_9[0-57-9]x_xxx");
    assertThat(simplified).assigned(TYPE, SHARED_COST).containsExactly(
        "123_96x_xx",
        "123_96x_xxx");
    assertThat(simplified).assigned(TYPE, MOBILE).containsExactly(
        "1[13]4_xxxx_xxxx");
  }

  @Test
  public void testSimplify_overlappingCheck() {
    Schema shortcodeSchema = Schema.builder().add(TYPE).build();

    RangeTable table = RangeTable.builder(shortcodeSchema)
        .apply(assign(ranges("123x"), TYPE, FIXED_LINE))
        .apply(assign(ranges("12x", "12xxx"), TYPE, MOBILE))
        .build();

    // The simplification function here is good for testing edge case behaviour since it's
    // essentially the most "extreme" simplification you can have.
    RangeTable simplified = table.simplify(EXTREME_SIMPLIFICATION, 0, TYPE);

    assertThat(simplified).hasColumns(TYPE);
    assertThat(simplified).assigned(TYPE, FIXED_LINE).containsExactly("123x");
    assertThat(simplified).assigned(TYPE, MOBILE).containsExactly("12x", "12[0-24-9]x", "12xxx");
  }

  private static RangeTree ranges(String... rangeSpecs) {
    return RangeTree.from(Arrays.stream(rangeSpecs).map(RangeSpecification::parse));
  }

  private static <T extends Comparable<T>> Change assign(
      RangeTree ranges, Column<T> column, T value) {
    return Change.builder(ranges).assign(column, value).build();
  }

  private static <T extends Comparable<T>> Change unassign(RangeTree ranges, Column<T> column) {
    return Change.builder(ranges).unassign(column).build();
  }

  private Change assign(RangeTree ranges, Map<Column<?>, ?> map) {
    return Change.of(ranges,
        map.entrySet().stream()
            .map(e -> Assignment.of(e.getKey(), e.getValue()))
            .collect(toImmutableList()));
  }

  private static Cell<RangeSpecification, Column<?>, Optional<?>> assigned(
      String range, Column<?> column, Object value) {
    return Tables.immutableCell(RangeSpecification.parse(range), column, Optional.of(value));
  }
}
