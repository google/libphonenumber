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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/** Key for use in "diff" tables, allowing rows to be marked with a diff status. */
@AutoValue
public abstract class DiffKey<K> {
  /**
   * Status for rows in a "diff table". Every row in a diff table has a {@code DiffKey}, with a
   * status. Modified rows appear twice in the diff table, once for the left-side row, and once for
   * the right-side row.
   */
  public enum Status {
    /** A row which appears exclusively in the left-hand-side of the diff. */
    LHS_ONLY("----"),
    /** A row which appears exclusively in the right-hand-side of the diff. */
    RHS_ONLY("++++"),
    /** The left-hand-side row which was modified by the  diff. */
    LHS_CHANGED("<<<<"),
    /** The right-hand-side row which was modified by the diff. */
    RHS_CHANGED(">>>>"),
    /** A row unchanged by the diff. */
    UNCHANGED("====");

    private static final ImmutableMap<String, Status> MAP =
        Maps.uniqueIndex(EnumSet.allOf(Status.class), Status::getLabel);

    private final String label;

    Status(String label) {
      this.label = label;
    }

    String getLabel() {
      return label;
    }

    static Status parse(String s) {
      return MAP.get(s);
    }
  }

  static <K> CsvKeyMarshaller<DiffKey<K>> wrap(CsvKeyMarshaller<K> keyMarshaller) {
    List<String> keyColumns = new ArrayList<>();
    keyColumns.add("Diff");
    keyColumns.addAll(keyMarshaller.getColumns());
    return new CsvKeyMarshaller<>(
        serialize(keyMarshaller), deserialize(keyMarshaller), ordering(keyMarshaller), keyColumns);
  }

  static <K> DiffKey<K> of(Status status, K key) {
    return new AutoValue_DiffKey<>(status, key);
  }

  public abstract Status getStatus();

  public abstract K getOriginalKey();

  private static <T> Function<DiffKey<T>, Stream<String>> serialize(CsvKeyMarshaller<T> m) {
    return k -> Stream.concat(Stream.of(k.getStatus().getLabel()), m.serialize(k.getOriginalKey()));
  }

  private static <T> Function<List<String>, DiffKey<T>> deserialize(CsvKeyMarshaller<T> m) {
    return r ->
        new AutoValue_DiffKey<>(Status.parse(r.get(0)), m.deserialize(r.subList(1, r.size())));
  }

  private static <T> Optional<Comparator<DiffKey<T>>> ordering(CsvKeyMarshaller<T> m) {
    return m.ordering().map(o -> {
      // Weird bug (possibly IntelliJ) means it really doesn't do well inferring types over lambdas
      // for this sort of chained API call. Pulling into separate variables works fine.
      Comparator<DiffKey<T>> keyFn = Comparator.comparing(DiffKey::getOriginalKey, o);
      return keyFn.thenComparing(DiffKey::getStatus);
    });
  }
}
