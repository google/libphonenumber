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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.i18n.phonenumbers.metadata.table.CsvParser.rowMapper;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment.Anchor;
import com.google.i18n.phonenumbers.metadata.table.CsvParser;
import com.google.i18n.phonenumbers.metadata.table.CsvParser.RowMapper;
import com.google.i18n.phonenumbers.metadata.table.CsvTable;
import com.google.i18n.phonenumbers.metadata.table.CsvTableCollector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The data schema for handling XML comments. Note that, unlike other "table" schemas, this does
 * not represent comments in the form of a CsvTable. This is because comment anchors can appear
 * multiple times in the CSV file (so there's no unique key). This is not an issue since the
 * internal data representation handles this, but it just means that code cannot be reused as much.
 */
public class CommentsSchema {
  private static final String REGION = "Region";
  private static final String LABEL = "Label";
  private static final String COMMENT = "Comment";

  public static final ImmutableList<String> HEADER = ImmutableList.of(REGION, LABEL, COMMENT);

  private static final Comparator<Comment> ORDERING = comparing(Comment::getAnchor);

  private static final CsvParser CSV_PARSER = CsvParser.withSeparator(';').trimWhitespace();
  private static final RowMapper ROW_MAPPER =
      rowMapper(h -> checkArgument(h.equals(HEADER), "unexpected comment header: %s", h));

  /** Loads the comments from a given file path. */
  public static ImmutableList<Comment> loadComments(Path path) {
    if (!Files.exists(path)) {
      return ImmutableList.of();
    }
    try (Reader csv = Files.newBufferedReader(path)) {
      return importComments(csv);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  static ImmutableList<Comment> importComments(Reader csv) throws IOException {
    List<Comment> comments = new ArrayList<>();
    Consumer<Stream<String>> rowCallback = getRowCallback(comments);
    try (BufferedReader r = new BufferedReader(csv)) {
      CSV_PARSER.parse(r.lines(),
          row -> rowCallback.accept(row.map(CsvTable::unescapeSingleLineCsvText)));
    }
    return ImmutableList.sortedCopyOf(ORDERING, comments);
  }

  public static ImmutableList<Comment> importComments(Supplier<List<String>> rows) {
    List<Comment> comments = new ArrayList<>();
    Consumer<Stream<String>> rowCallback = getRowCallback(comments);
    // Expect header row always.
    rowCallback.accept(rows.get().stream());
    List<String> row;
    while ((row = rows.get()) != null) {
      rowCallback.accept(row.stream());
    }
    return ImmutableList.sortedCopyOf(ORDERING, comments);
  }

  private static Consumer<Stream<String>> getRowCallback(List<Comment> comments) {
    return ROW_MAPPER.mapTo(row -> {
      if (row.containsKey(COMMENT)) {
          comments.add(
              Comment.fromText(
                  Anchor.of(PhoneRegion.of(row.get(REGION)), row.get(LABEL)),
                  row.get(COMMENT)));
      }
    });
  }

  /** Exports alternate formats to a collector (potentially escaping fields for CSV). */
  public static void export(
      List<Comment> comments, Consumer<Stream<String>> collector, boolean toCsv) {
    collector.accept(HEADER.stream());
    Function<String, String> escapeFn = toCsv ? CsvTable::escapeForSingleLineCsv : identity();
    comments.stream()
        .sorted(ORDERING)
        .forEach(c -> collector.accept(Stream.of(
            c.getAnchor().region().toString(), c.getAnchor().label(), escapeFn.apply(c.toText()))));
  }

  /** Helper method to write comments in same CSV format as CsvTable. */
  public static boolean exportCsv(Writer csv, List<Comment> comments) {
    if (comments.isEmpty()) {
      return false;
    }
    CsvTableCollector collector = new CsvTableCollector(true);
    export(comments, collector, true);
    collector.writeCsv(csv);
    return true;
  }
}
