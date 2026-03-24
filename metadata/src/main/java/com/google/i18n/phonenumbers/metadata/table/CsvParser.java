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

import static com.google.common.base.CharMatcher.isNot;
import static com.google.common.base.CharMatcher.javaIsoControl;
import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * An efficient, fluent CSV parser which operates on a {@link Stream} of lines. It handles quoting
 * of values, whitespace trimming and mapping values via a "schema" row.
 *
 * <p>This class is sadly necessary since the one in {@code com.google.common.text} doesn't support
 * ignoring whitespace (and making it do so would take longer than writing this).
 *
 * <p>This class is immutable and thread-safe.
 */
// TODO: Investigate other "standard" CSV parsers such as org.apache.commons.csv.
public final class CsvParser {
  /**
   * A consumer for CSV rows which can automatically map values according to a header row.
   *
   * <p>This class is immutable and thread-safe.
   */
  public static final class RowMapper {
    @Nullable private final Consumer<ImmutableList<String>> headerHandler;

    private RowMapper(Consumer<ImmutableList<String>> headerHandler) {
      this.headerHandler = headerHandler;
    }

    public Consumer<Stream<String>> mapTo(Consumer<ImmutableMap<String, String>> handler) {
      return new Consumer<Stream<String>>() {
        private ImmutableList<String> header = null;

        @Override
        public void accept(Stream<String> row) {
          if (header == null) {
            // Can contain duplicates (but that's bad for mapping).
            header = row.collect(toImmutableList());
            checkArgument(
                header.size() == header.stream().distinct().count(),
                "duplicate values in CSV header: %s",
                header);
            if (headerHandler != null) {
              headerHandler.accept(header);
            }
          } else {
            ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
            // Not a pure lambda due to the need to index columns.
            row.forEach(
                new Consumer<String>() {
                  private int i = 0;

                  @Override
                  public void accept(String v) {
                    checkArgument(
                        i < header.size(),
                        "too many columns (expected %s): %s",
                        header.size(),
                        map);
                    if (!v.isEmpty()) {
                      map.put(header.get(i++), v);
                    }
                  }
                });
            handler.accept(map.buildOrThrow());
          }
        }
      };
    }
  }

  private static final CharMatcher NON_WHITESPACE = CharMatcher.whitespace().negate();
  private static final char QUOTE = '"';
  private static final CharMatcher VALID_DELIMITER_CHAR =
      NON_WHITESPACE.and(javaIsoControl().negate()).and(isNot(QUOTE)).or(CharMatcher.anyOf(" \t"));

  public static CsvParser withSeparator(char delimiter) {
    return new CsvParser(delimiter, false, false);
  }

  public static CsvParser commaSeparated() {
    return withSeparator(',');
  }

  public static CsvParser tabSeparated() {
    return withSeparator('\t');
  }

  public static RowMapper rowMapper() {
    return new RowMapper(null);
  }

  public static RowMapper rowMapper(Consumer<ImmutableList<String>> headerHandler) {
    return new RowMapper(headerHandler);
  }

  private final char delimiter;
  private final boolean trimWhitespace;
  private final boolean allowMultiline;

  private CsvParser(char delimiter, boolean trimWhitespace, boolean allowMultiline) {
    checkArgument(VALID_DELIMITER_CHAR.matches(delimiter),
        "invalid delimiter: %s", delimiter);
    this.delimiter = delimiter;
    this.trimWhitespace = trimWhitespace;
    this.allowMultiline = allowMultiline;
  }

  public CsvParser trimWhitespace() {
    checkArgument(NON_WHITESPACE.matches(delimiter),
        "cannot trim whitespace if delimiter is whitespace");
    return new CsvParser(delimiter, true, allowMultiline);
  }

  public CsvParser allowMultiline() {
    return new CsvParser(delimiter, trimWhitespace, true);
  }

  public void parse(Stream<String> lines, Consumer<Stream<String>> rowCallback) {
    // Allow whitespace delimiter if we aren't also trimming whitespace.
    List<String> row = new ArrayList<>();
    StringBuilder buffer = new StringBuilder();
    Iterator<String> it = lines.iterator();
    while (parseRow(it, row, buffer)) {
      rowCallback.accept(row.stream());
      row.clear();
    }
  }

  private boolean parseRow(Iterator<String> lines, List<String> row, StringBuilder buffer) {
    if (!lines.hasNext()) {
      return false;
    }
    // First line of potentially several which make up this row.
    String line = lines.next();
    int start = maybeTrimWhitespace(line, 0);
    while (start < line.length()) {
      // "start" is the start of the next part and must be a valid index into current "line".
      // Could be high or low surrogate if badly formed string, or just point at the delimiter.
      char c = line.charAt(start);
      int pos;
      if (c == QUOTE) {
        // Quoted value, maybe parse and unescape multiple lines here.
        pos = ++start;
        while (true) {
          if (pos == line.length()) {
            buffer.append(line, start, pos);
            checkArgument(allowMultiline && lines.hasNext(),
                "unterminated quoted value: %s", buffer);
            buffer.append('\n');
            line = lines.next();
            start = 0;
            pos = 0;
          }
          c = line.charAt(pos);
          if (c == QUOTE) {
            buffer.append(line, start, pos++);
            if (pos == line.length()) {
              break;
            }
            if (line.charAt(pos) != QUOTE) {
              pos = maybeTrimWhitespace(line, pos);
              checkArgument(pos == line.length() || line.codePointAt(pos) == delimiter,
                  "unexpected character (expected delimiter) in: %s", line);
              break;
            }
            // "Double double quotes, what does it mean?" (oh yeah, a single double quote).
            buffer.append(QUOTE);
            start = pos + 1;
          }
          pos++;
        }
        row.add(buffer.toString());
        buffer.setLength(0);
      } else if (c == delimiter) {
        // Empty unquoted empty value (e.g. "foo,,bar").
        row.add("");
        pos = start;
      } else {
        // Non-empty unquoted value.
        pos = line.indexOf(delimiter, start + 1);
        if (pos == -1) {
          pos = line.length();
        }
        String value = line.substring(start, maybeTrimTrailingWhitespace(line, pos));
        checkArgument(value.indexOf(QUOTE) == -1,
            "quotes cannot appear in unquoted values: %s", value);
        row.add(value);
      }
      if (pos == line.length()) {
        // We hit end-of-line at the end of a value, so just return (no trailing empty value).
        return true;
      }
      // If not end-of-line, "pos" points at the last delimiter, so we can find the next start.
      start = maybeTrimWhitespace(line, pos + 1);
    }
    // We hit end-of-line either immediately, or after a delimiter. Either way we always need to
    // add a trailing empty value for consistency.
    row.add("");
    return true;
  }

  private int maybeTrimWhitespace(String s, int i) {
    if (trimWhitespace) {
      i = NON_WHITESPACE.indexIn(s, i);
      if (i == -1) {
        i = s.length();
      }
    }
    return i;
  }

  private int maybeTrimTrailingWhitespace(String s, int i) {
    if (trimWhitespace) {
      // There is no "lastIndexIn(String, int)" sadly.
      while (i > 0 && whitespace().matches(s.charAt(i - 1))) {
        i--;
      }
    }
    return i;
  }
}
