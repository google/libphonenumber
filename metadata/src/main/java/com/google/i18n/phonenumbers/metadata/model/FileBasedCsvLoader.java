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
package com.google.i18n.phonenumbers.metadata.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.model.CsvData.CsvDataProvider;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A CSV provider which reads files rooted in a given directory. The file layout should match that
 * in the CSV metadata directory ({@code third_party/libphonenumber_metadata/metadata}).
 */
public final class FileBasedCsvLoader implements CsvDataProvider {
  /** Returns a CSV loader which reads files from the given base directory. */
  public static FileBasedCsvLoader using(Path dir) throws IOException {
    return new FileBasedCsvLoader(dir);
  }

  private final Path root;
  private final CsvTable<DigitSequence> metadata;

  private FileBasedCsvLoader(Path root) throws IOException {
    this.root = checkNotNull(root);
    this.metadata = MetadataTableSchema.SCHEMA.load(root.resolve("metadata.csv"));
  }

  @Override
  public CsvTable<DigitSequence> loadMetadata() {
    return metadata;
  }

  @Override
  public CsvData loadData(DigitSequence cc) throws IOException {
    Path ccDir = root.resolve(cc.toString());
    return CsvData.create(
        cc,
        metadata,
        RangesTableSchema.SCHEMA.load(csvFile(ccDir, "ranges")),
        ShortcodesTableSchema.SCHEMA.load(csvFile(ccDir, "shortcodes")),
        ExamplesTableSchema.SCHEMA.load(csvFile(ccDir, "examples")),
        FormatsTableSchema.SCHEMA.load(csvFile(ccDir, "formats")),
        AltFormatsSchema.loadAltFormats(csvFile(ccDir, "altformats")),
        OperatorsTableSchema.SCHEMA.load(csvFile(ccDir, "operators")),
        CommentsSchema.loadComments(csvFile(ccDir, "comments"))
    );
  }

  private static Path csvFile(Path dir, String name) {
    return dir.resolve(name + ".csv");
  }
}
