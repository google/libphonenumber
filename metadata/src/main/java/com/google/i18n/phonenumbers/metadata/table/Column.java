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

import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import com.google.auto.value.AutoValue;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * A column specifier which holds a set of values that are allowed with a column.
 */
@AutoValue
public abstract class Column<T extends Comparable<T>> {
  private static final ImmutableMap<String, Boolean> BOOLEAN_MAP =
      ImmutableMap.of("true", TRUE, "TRUE", TRUE, "false", FALSE, "FALSE", false);
  private static final CharMatcher ASCII_LETTER_OR_DIGIT =
      inRange('a', 'z').or(inRange('A', 'Z')).or(inRange('0', '9'));
  private static final CharMatcher LOWER_ASCII_LETTER_OR_DIGIT =
      inRange('a', 'z').or(inRange('0', '9'));
  private static final CharMatcher LOWER_UNDERSCORE =
      CharMatcher.is('_').or(LOWER_ASCII_LETTER_OR_DIGIT);


  /**
   * Returns a column for the specified type with a given parsing function. Use alternate helper
   * methods for creating columns of common types.
   */
  public static <T extends Comparable<T>> Column<T> create(
      Class<T> clazz, String name, T defaultValue, Function<String, T> parseFn) {
    return new AutoValue_Column<>(
        checkName(name), clazz, parseFn, String::valueOf, defaultValue, null);
  }

  /**
   * Returns a column for the specified enum type. The string representation of a value in this
   * column is just the {@code toString()} value of the enum.
   */
  public static <T extends Enum<T>> Column<T> of(Class<T> clazz, String name, T defaultValue) {
    return create(clazz, name, defaultValue, s -> Enum.valueOf(clazz, toEnumName(s)));
  }

  /**
   * Returns a column for strings. In there serialized form, strings do not preserve leading or
   * trailing whitespace, unless surrounded by double-quotes (e.g. {@code " foo "}). The quotes are
   * stripped on parsing and added back for any String value with leading/trailing whitespace. The
   * default value is the empty string.
   */
  public static Column<String> ofString(String name) {
    return new AutoValue_Column<>(
        checkName(name), String.class, Column::trimOrUnquote, Column::maybeQuote, "", null);
  }

  /**
   * Returns a column for unsigned integers. The string representation of a value in this column
   * matches the {@link Integer#toString(int)} value. The default value is {@code 0}.
   */
  public static Column<Integer> ofUnsignedInteger(String name) {
    return create(Integer.class, name, 0, Integer::parseUnsignedInt);
  }

  /**
   * Returns a column for booleans. The string representation of a value in this column can be any
   * of "true", "false", "TRUE", "FALSE" (but not things like "True", "T" or "YES"). The default
   * value is {@code false}.
   */
  public static Column<Boolean> ofBoolean(String name) {
    return create(Boolean.class, name, false, BOOLEAN_MAP::get);
  }

  private static String checkName(String name) {
    checkArgument(name.indexOf(':') == -1, "invalid column name: %s", name);
    return name;
  }

  // Converts to UPPER_UNDERSCORE naming for enums.
  private static String toEnumName(String name) {
    // Allow conversion for lower_underscore and lowerCamel, since UPPER_UNDERSCORE is so "LOUD".
    // We can be sloppy with respect to errors here since all runtime exceptions are handled.
    if (LOWER_ASCII_LETTER_OR_DIGIT.matches(name.charAt(0))) {
      if (LOWER_UNDERSCORE.matchesAllOf(name)) {
        name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_UNDERSCORE, name);
      } else if (ASCII_LETTER_OR_DIGIT.matchesAllOf(name)) {
        name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
      } else {
        // Message/type not important here since all exceptions are replaced anyway.
        throw new IllegalArgumentException();
      }
    }
    return name;
  }

  // Trims whitespace from a serialize string, unless the value is surrounded by double-quotes (in
  // which case the quotes are removed). This is done to permit the rare use of leading/trailing
  // whitespace in data in a visually distinct and deliberate way.
  private static String trimOrUnquote(String s) {
    if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
      return s.substring(1, s.length() - 1);
    }
    return whitespace().trimFrom(s);
  }

  // Surrounds any string with whitespace at either end with double quotes.
  private static String maybeQuote(String s) {
    if (s.length() > 0
        && (whitespace().matches(s.charAt(0)) || whitespace().matches(s.charAt(s.length() - 1)))) {
      return '"' + s + '"';
    }
    return s;
  }

  /** Returns the column name (which can be used as a human readable title if needed). */
  public abstract String getName();

  abstract Class<T> type();

  // The parsing function from a string to a value.
  abstract Function<String, T> parseFn();
  // The serialization function from a value to a String. This must be the inverse of the parseFn.
  abstract Function<T, String> serializeFn();

  /** Default value for this column (inferred for unassigned ranges when a snapshot is built). */
  public abstract T defaultValue();

  // This is very private and should only be used in this class.
  @Nullable abstract Column<T> owningGroup();

  /** Attempts to cast the given instance to the runtime type of this column. */
  @Nullable public final T cast(@Nullable Object value) {
    return type().cast(value);
  }

  /**
   * Returns the value of this column based on its serialized representation (which is not
   * necessarily its {@code toString()} representation).
   */
  @Nullable public final T parse(String id) {
    if (id.isEmpty()) {
      return null;
    }
    try {
      // TODO: Technically wrong, since for String columns this will unquote strings.
      // Hopefully this won't be an issue, since quoting is really only likely to be used for
      // preserving whitespace (which i

      T value = parseFn().apply(id);
      if (value != null) {
        return value;
      }
    } catch (RuntimeException e) {
      // fall through
    }
    throw new IllegalArgumentException(
        String.format("unknown value '%s' in column '%s'", id, getName()));
  }

  /**
   * Returns the serialized representation of a value in this column. This is the stored
   * representation of the value, not the value itself.
   */
  public final String serialize(@Nullable Object value) {
    return (value != null) ? serializeFn().apply(cast(value)) : "";
  }

  // Only to be called by ColumnGroup.
  final Column<T> fromPrototype(String suffix) {
    String name = getName() + ":" + checkName(suffix);
    return new AutoValue_Column<T>(name, type(), parseFn(), serializeFn(), defaultValue(), this);
  }

  final boolean isIn(ColumnGroup<?, ?> group) {
    return group.prototype().equals(owningGroup());
  }

  @Override
  public final String toString() {
    return "Column{'" + getName() + "'}";
  }

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof Column<?>)) {
      return false;
    }
    Column<?> c = (Column<?>) obj;
    return c.getName().equals(getName()) && c.type().equals(type());
  }

  @Override
  public final int hashCode() {
    return getName().hashCode() ^ type().hashCode();
  }

  // Visible only for AutoValue
  Column() {}
}
