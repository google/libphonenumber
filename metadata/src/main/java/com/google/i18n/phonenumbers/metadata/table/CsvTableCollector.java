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
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Collects cell values and tracks maximum cell width to make it easy to output aligned CSV. */
public final class CsvTableCollector implements Consumer<Stream<String>> {
  private final NavigableMap<Integer, Integer> maxLengths = new TreeMap<>();
  private final List<List<String>> cells = new ArrayList<>();
  private final boolean align;

  public CsvTableCollector(boolean align) {
    this.align = align;
  }

  /**
   * Writes the contents of this table, with optional alignment, as a CSV table. Returns whether
   * anything was written.
   */
  public void writeCsv(Writer writer) {
    try (PrintWriter out = new PrintWriter(writer)) {
      // Pad elements with whitespace when aligning (since we've gone to all the effort of padding
      // everything else).
      String joiner = align ? " ; " : ";";
      for (int rowIndex = 0; rowIndex < cells.size(); rowIndex++) {
        // No need to use CharMatcher to trim "properly" since only ASCII space is possible.
        out.println(getRow(rowIndex).collect(joining(joiner)).trim());
      }
    }
  }

  /**
   * Accepts the next row in the CSV table. Note that the first consumer returned is expected to
   * have the title row written to it.
   *
   * <p>Values passed into the accept method of the returned consumer are expected to have already
   * been escaped if necessary. The caller must call the {@link Consumer#accept(Object)} method for
   * every column of the table, even if only to pass an empty string to indicate an empty cell.
   */
  @Override
  public void accept(Stream<String> row) {
    ImmutableList<String> rowValues = row.collect(toImmutableList());
    for (int i = 0; i < rowValues.size(); i++) {
      updateMaxLength(rowValues.get(i), i);
    }
    cells.add(rowValues);
  }

  private Stream<String> getRow(int index) {
    List<String> row = cells.get(index);
    int length = row.size();
    while (length > 0 && row.get(length - 1).isEmpty()) {
      length--;
    }
    if (align) {
      return IntStream.range(0, length).mapToObj(n -> pad(row.get(n), maxLength(n)));
    }
    return row.stream().limit(length);
  }

  private static String pad(String s, int len) {
    return len > 0 ? String.format("%-" + len + "s", s) : "";
  }

  private int maxLength(int index) {
    return maxLengths.getOrDefault(index, 0);
  }

  private void updateMaxLength(String s, int index) {
    // Note: This isn't Unicode aware, but in reality it's not that important.
    maxLengths.put(index, Math.max(s.length(), maxLength(index)));
  }
}
