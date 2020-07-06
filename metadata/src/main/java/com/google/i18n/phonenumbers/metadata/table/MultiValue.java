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

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Comparator;
import java.util.function.Function;

/**
 * A wrapper to permit sets of values to be specified as a single "cell" in a CsvTable or
 * RangeTable. Currently only sets of values are permitted (not lists) so duplicate elements are
 * not allowed. This is easy to change in future, but the real data suggests no use case for that.
 *
 * <p>The expectation of this class is that specific, non-generic subclasses will be made to
 * "solidify" the choice of value type, separator and value ordering. This is why those specific
 * attributes are not tested in the equals()/hashCode() methods, since they are expected to be
 * constant for a given implementation. Subclasses should be final, and look something like:
 * <pre> {@code
 * public static final class Foos extends MultiValue<Foo, Foos> {
 *   private static final Foos EMPTY = new Foos(ImmutableSet.of());
 *
 *   public static Column<Foos> column(String name) {
 *     return Column.create(Foos.class, name, EMPTY, Foos::new);
 *   }
 *
 *   public static Foos of(Iterable<Foo> foos) {
 *     return new Foos(foos);
 *   }
 *
 *   private Foos(Iterable<Foo> foos) { super(foos, <separator>, <ordering>, <sorted>); }
 *   private Foos(String s) { super(s, <parseFn>, <separator>, <ordering>, <sorted>); }
 * }
 * }</pre>
 * where {@code <separator>}, {@code <ordering>} and {@code <sorted>} are the same constants in
 * both places.
 */
public abstract class MultiValue<T, M extends MultiValue<T, M>>
    implements Comparable<M> {

  private final ImmutableSet<T> values;
  private final char separator;
  private final Comparator<Iterable<T>> comparator;

  protected MultiValue(
      String s, Function<String, T> fn, char separator, Comparator<T> comparator, boolean sorted) {
    this(parse(s, fn, separator), separator, comparator, sorted);
  }

  protected MultiValue(
      Iterable<T> values, char separator, Comparator<T> comparator, boolean sorted) {
    this.separator = separator;
    this.values =
        sorted ? ImmutableSortedSet.copyOf(comparator, values) : ImmutableSet.copyOf(values);
    this.comparator = Comparators.lexicographical(comparator);
  }

  private static <T> ImmutableList<T> parse(String s, Function<String, T> fn, char separator) {
    Splitter splitter = Splitter.on(separator).omitEmptyStrings().trimResults(whitespace());
    return splitter.splitToList(s).stream().map(fn).collect(toImmutableList());
  }

  public final ImmutableSet<T> getValues() {
    return values;
  }

  public final char separator() {
    return separator;
  }

  @Override
  public final int compareTo(M that) {
    // The separator doesn't factor in here since it's always the same.
    return comparator.compare(this.getValues(), that.getValues());
  }

  @Override
  @SuppressWarnings({"unchecked", "EqualsGetClass"})
  public final boolean equals(Object obj) {
    // Check exact subclass, since we expect separators and ordering to always be the same.
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    return getValues().equals(((MultiValue<T, M>) obj).getValues());
  }

  @Override
  public final int hashCode() {
    return getValues().hashCode();
  }

  @Override
  public final String toString() {
    return Joiner.on(separator()).join(getValues());
  }
}
