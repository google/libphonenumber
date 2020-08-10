package com.google.phonenumbers.migrator;

import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.MetadataTableSchema.Regions;
import com.google.i18n.phonenumbers.metadata.model.RangesTableSchema;
import com.google.i18n.phonenumbers.metadata.table.Change;
import com.google.i18n.phonenumbers.metadata.table.Column;
import com.google.i18n.phonenumbers.metadata.table.ColumnGroup;
import com.google.i18n.phonenumbers.metadata.table.CsvKeyMarshaller;
import com.google.i18n.phonenumbers.metadata.table.CsvSchema;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.RangeKey;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.util.Optional;
import java.util.TreeSet;

public abstract class RecipesTableSchema {

  public static final Column<Regions> CSV_REGION = Regions.column("Region");

  public static final ColumnGroup<PhoneRegion, Boolean> RANGE_REGION =
      ColumnGroup.byRegion(Column.ofBoolean("Region"));

  public static final Column<String> OLD_FORMAT = Column.ofString("Old Format");

  public static final Column<String> NEW_FORMAT = Column.ofString("New Format");

  public static final Column<Boolean> IS_FINAL_MIGRATION = Column.ofBoolean("Is Final Migration");

  private static final CsvKeyMarshaller<RangeKey> MARSHALLER = new CsvKeyMarshaller<>(
      RangesTableSchema::write,
      RangesTableSchema::read,
      Optional.of(RangeKey.ORDERING),
      "Old Prefix",
      "Old Length");

  private static final Schema CSV_COLUMNS =
      Schema.builder()
          .add(CSV_REGION)
          .add(OLD_FORMAT)
          .add(NEW_FORMAT)
          .add(IS_FINAL_MIGRATION)
          .build();

  public static final CsvSchema<RangeKey> SCHEMA = CsvSchema.of(MARSHALLER, CSV_COLUMNS);

  private static final Schema RANGE_COLUMNS =
      Schema.builder()
          .add(RANGE_REGION)
          .add(OLD_FORMAT)
          .add(NEW_FORMAT)
          .add(IS_FINAL_MIGRATION)
          .build();

  public static RangeTable toRangeTable(CsvTable<RangeKey> csv) {
    RangeTable.Builder out = RangeTable.builder(RANGE_COLUMNS);
    for (RangeKey k : csv.getKeys()) {
      Change.Builder change = Change.builder(k.asRangeTree());
      csv.getRow(k).forEach((c, v) -> {
        // We special case the regions column, converting a comma separated list of region codes
        // into a series of boolean column assignments.
        if (c.equals(CSV_REGION)) {
          CSV_REGION.cast(v).getValues().forEach(r -> change.assign(RANGE_REGION.getColumn(r), true));
        } else {
          change.assign(c, v);
        }
      });
      out.apply(change.build(), OverwriteMode.NEVER);
    }
    return out.build();
  }

  @SuppressWarnings("unchecked")
  public static CsvTable<RangeKey> toCsv(RangeTable table) {
    CsvTable.Builder<RangeKey> csv = CsvTable.builder(SCHEMA);
    ImmutableSet<Column<Boolean>> regionColumns =
        RANGE_REGION.extractGroupColumns(table.getColumns()).values();
    TreeSet<PhoneRegion> regions = new TreeSet<>();
    for (Change c : table.toChanges()) {
      for (RangeKey k : RangeKey.decompose(c.getRanges())) {
        regions.clear();
        c.getAssignments().forEach(a -> {
          // We special case the regions column, converting a group of boolean columns into a
          // multi-value of region codes. If the column is in the group, it must hold Booleans.
          if (regionColumns.contains(a.column())) {
            if (a.value().map(((Column<Boolean>) a.column())::cast).orElse(Boolean.FALSE)) {
              regions.add(RANGE_REGION.getKey(a.column()));
            }
          } else {
            csv.put(k, a);
          }
        });
        // We can do this out-of-sequence because the table will order its columns.
        if (!regions.isEmpty()) {
          csv.put(k, CSV_REGION, Regions.of(regions));
        }
      }
    }
    return csv.build();
  }
}
