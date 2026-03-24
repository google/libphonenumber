/*
 * Copyright (C) 2022 The Libphonenumber Authors.
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

package com.google.i18n.phonenumbers.metadata;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseUnsignedInt;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/** Parses strings of form "4,7-9,11" which are used as length specifiers across LPN metadata */
public final class LengthsParser {

  private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults(whitespace());
  private static final Splitter RANGE_SPLITTER =
      Splitter.on('-').trimResults(whitespace()).limit(2);
  private static final CharMatcher ALLOWED_CHARACTERS =
      CharMatcher.inRange('0', '9').or(CharMatcher.anyOf("-,")).or(whitespace());

  /** Returns the set of integers specified by this string. */
  public static ImmutableSortedSet<Integer> parseLengths(String s) {
    checkArgument(
        ALLOWED_CHARACTERS.matchesAllOf(s),
        "Length specifier contains forbidden characters: %s",
        s);
    NavigableSet<Integer> lengths = new TreeSet<>();
    for (String lengthOrRange : COMMA_SPLITTER.split(s)) {
      if (lengthOrRange.contains("-")) {
        List<String> lohi = RANGE_SPLITTER.splitToList(lengthOrRange);
        int lo = parseUnsignedInt(lohi.get(0));
        int hi = parseUnsignedInt(lohi.get(1));
        checkArgument(lo < hi, "Invalid range: %s-%s", lo, hi);
        checkArgument(
            lengths.isEmpty() || lo > lengths.last(),
            "Numbers in length specifier are out of order: %s",
            s);
        lengths.addAll(ContiguousSet.closed(lo, hi));
      } else {
        int length = parseUnsignedInt(lengthOrRange);
        checkArgument(
            lengths.isEmpty() || length > lengths.last(),
            "Numbers in length specifier are out of order: %s",
            s);
        lengths.add(length);
      }
    }
    return ImmutableSortedSet.copyOf(lengths);
  }

  private LengthsParser() {}
}
