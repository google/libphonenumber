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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Comparator.naturalOrder;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/** Marshaller to handle key serialization and ordering in {@code CsvTable}. */
public final class CsvKeyMarshaller<K> {
  private final Function<K, Stream<String>> serialize;
  private final Function<List<String>, K> deserialize;
  private final Optional<Comparator<K>> ordering;
  private final ImmutableList<String> columns;

  public static CsvKeyMarshaller<String> ofSortedString(String columnName) {
    return new CsvKeyMarshaller<String>(
        Stream::of, p -> p.get(0), Optional.of(naturalOrder()), columnName);
  }

  public CsvKeyMarshaller(
      Function<K, Stream<String>> serialize,
      Function<List<String>, K> deserialize,
      Optional<Comparator<K>> ordering,
      String... columns) {
    this(serialize, deserialize, ordering, ImmutableList.copyOf(columns));
  }

  public CsvKeyMarshaller(
      Function<K, Stream<String>> serialize,
      Function<List<String>, K> deserialize,
      Optional<Comparator<K>> ordering,
      List<String> columns) {
    this.serialize = checkNotNull(serialize);
    this.deserialize = checkNotNull(deserialize);
    this.ordering = checkNotNull(ordering);
    this.columns = ImmutableList.copyOf(columns);
  }

  public ImmutableList<String> getColumns() {
    return columns;
  }

  Stream<String> serialize(K key) {
    return serialize.apply(key);
  }

  K deserialize(List<String> keyParts) {
    return deserialize.apply(keyParts);
  }

  Optional<Comparator<K>> ordering() {
    return ordering;
  }
}
