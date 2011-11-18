/*
 * Copyright (C) 2010 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers;

import java.util.Iterator;

/**
 * A sequence of elements representing a JavaScript Array. The principal operation on a
 * JSArrayBuilder is the append method that appends an element to the array. To facilitate nesting
 * beginArray and endArray are also supported. Example of a JSArray: ["a", ["b', "c"]].
 *
 * @author Nikolaos Trogkanis
 */
public class JSArrayBuilder implements CharSequence {
  // Internal representation.
  private StringBuilder data = new StringBuilder();
  // Flag that keeps track whether the element being added to the array is the first element.
  private boolean isFirstElement = true;

  /**
   * Begin a new element.
   */
  private void beginElement() {
    if (!isFirstElement) {
      data.append(',');
    }
    isFirstElement = false;
  }

  /**
   * Begin a new array.
   */
  public JSArrayBuilder beginArray() {
    beginElement();
    data.append('[');
    isFirstElement = true;
    return this;
  }

  /**
   * End an array.
   */
  public JSArrayBuilder endArray() {
    trimTrailingCommas();
    data.append("]\n");
    isFirstElement = false;
    return this;
  }

  /**
   * Add a number to the array.
   */
  public JSArrayBuilder append(int number) {
    return append(Integer.toString(number), false);
  }

  /**
   * Add a string to the array.
   */
  public JSArrayBuilder append(String string) {
    return append(string, true);
  }

  /**
   * Add a boolean to the array.
   */
  public JSArrayBuilder append(boolean b) {
    return append(b ? 1 : 0);
  }

  /**
   * Add a collection of strings to the array.
   */
  public final JSArrayBuilder appendIterator(Iterator<String> iterator) {
    while (iterator.hasNext()) {
      append(iterator.next());
    }
    return this;
  }

  // Adds a string to the array with an option to escape the string or not.
  private JSArrayBuilder append(String string, boolean escapeString) {
    beginElement();
    if (string != null) {
      if (escapeString) {
        escape(string, data);
      } else {
        data.append(string);
      }
    }
    return this;
  }

  // Returns a string representing the data in this JSArray.
  @Override public String toString() {
    return data.toString();
  }

  // Double quotes a string and replaces "\" with "\\".
  private static void escape(String str, StringBuilder out) {
    out.append('"');
    out.append(str.replaceAll("\\\\", "\\\\\\\\"));
    out.append('"');
  }

  // Trims trailing commas.
  private void trimTrailingCommas() {
    int i = data.length();
    while (i > 0 && data.charAt(i - 1) == ',') {
      i--;
    }
    if (i < data.length()) {
      data.delete(i, data.length());
    }
  }

  public char charAt(int index) {
    return data.charAt(index);
  }

  public int length() {
    return data.length();
  }

  public CharSequence subSequence(int start, int end) {
    return data.subSequence(start, end);
  }
}
