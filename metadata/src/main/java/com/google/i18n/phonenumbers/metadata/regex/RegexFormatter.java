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

package com.google.i18n.phonenumbers.metadata.regex;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

/**
 * Simple indenting formatter for regular expressions and other similar nested syntax. Obviously
 * the results are not the same from a match perspective as the new string contains whitespace.
 */
public final class RegexFormatter {
  /** Option for how to handle formatting of groups. */
  public enum FormatOption {
    PRESERVE_CAPTURING_GROUPS,
    FORCE_NON_CAPTURING_GROUPS,
    FORCE_CAPTURING_GROUPS,
  }

  // We only care about 3 specific tokens, so this code can be used to print strings which look
  // similar (nested, disjunctive groups) such as the toString() of the Edge class.
  private static final CharMatcher tokens = CharMatcher.anyOf("()|");

  /**
   * Formats a regular expression (or similar nested group syntax) using the following rules:
   * <ol>
   * <li>Newline after opening '(?:' and increase indent.
   * <li>Newline after '|'
   * <li>Decrease indent and add newline before closing ')'
   * </ol>
   */
  public static String format(String regex, FormatOption formatOption) {
    return new RegexFormatter(regex, formatOption).format();
  }

  private final StringBuilder out = new StringBuilder();
  private final String regex;
  private final FormatOption formatOption;

  private RegexFormatter(String regex, FormatOption formatOption) {
    this.regex = CharMatcher.whitespace().removeFrom(regex);
    this.formatOption = Preconditions.checkNotNull(formatOption);
  }

  private String format() {
    recurse(0, 0);
    return out.toString();
  }

  // Assume at line start.
  private int recurse(int pos, int level) {
    while (pos < regex.length()) {
      indent(level);
      // Optionally printing closing group from previous recursion.
      if (regex.charAt(pos) == ')') {
        out.append(')');
        pos++;
      }
      int nextToken = tokens.indexIn(regex, pos);
      if (nextToken == -1) {
        out.append(regex.substring(pos, regex.length()));
        return regex.length();
      }
      out.append(regex.substring(pos, nextToken));
      pos = nextToken;
      switch (regex.charAt(pos)) {
        case '(':
          out.append("(");
          pos++;
          if (regex.indexOf("?:", pos) == pos) {
            if (formatOption != FormatOption.FORCE_CAPTURING_GROUPS) {
              out.append("?:");
            }
            pos += 2;
          } else if (formatOption == FormatOption.FORCE_NON_CAPTURING_GROUPS) {
            out.append("?:");
          }
          out.append('\n');
          pos = recurse(pos, level + 1);
          break;

        case '|':
          out.append("|\n");
          pos++;
          break;

        case ')':
          // Just exit recursion and let the parent write the ')', so don't update our position.
          out.append("\n");
          return pos;

        default:
          throw new AssertionError();
      }
    }
    return pos;
  }

  private void indent(int level) {
    while (level-- > 0) {
      out.append("  ");
    }
  }
}
