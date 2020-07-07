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

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.model.FormatSpec.FormatGroup;
import com.google.i18n.phonenumbers.metadata.model.FormatSpec.FormatTemplate;
import java.util.Optional;

/**
 * An alternate format, used to describe less common ways we believe a phone number can be
 * formatted in a region. These can be derived from an "alias" in the formats table, or as
 * "historical" formats which are not associated with any specific current format.
 *
 * <p>Note that alternate formats can be defined with the same template, and they are merged
 * together to produce a canonical map in which the format template is the key.
 */
@AutoValue
public abstract class AltFormatSpec {
  private static final CharMatcher OPT_DIGIT = CharMatcher.is('*');
  private static final CharMatcher ANY_DIGIT = CharMatcher.is('X');
  private static final CharMatcher ALLOWED_TEMPLATE_CHARS = CharMatcher.anyOf("X* ");

  public static AltFormatSpec create(
      FormatTemplate template, RangeSpecification prefix, String parent, Optional<String> comment) {
    // As only a limited set of chars is allowed, we know things like national prefix or carrier
    // codes cannot be present. We're just interested in basic grouping like "XXX XXX**".
    String spec = template.getSpecifier();
    checkArgument(ALLOWED_TEMPLATE_CHARS.matchesAllOf(spec) && !template.getXmlPrefix().isPresent(),
        "invalid alternate format template: %s", template);
    // Prefix must be shorter than the template and not contain any trailing 'x'.
    checkArgument(prefix.length() <= template.minLength() && prefix.equals(prefix.getPrefix()),
        "invalid prefix '%s' for alternate format template: %s", prefix, template);
    // If variable length, the spec must have room for the prefix before the '*' characters.
    checkArgument(
        OPT_DIGIT.matchesNoneOf(spec)
            || prefix.length() <= ANY_DIGIT.countIn(spec.substring(0, OPT_DIGIT.indexIn(spec))),
        "invalid prefix '%s' for alternate format template: %s", prefix, template);
    return new AutoValue_AltFormatSpec(template, prefix, parent, comment);
  }

  /** Return the alternate format template containing only simple grouping (e.g. "XXX XXX**"). */
  public abstract FormatTemplate template();

  /**
   * Returns the prefix for this alternate format which (along with the template length) defines
   * the bounds over which this format can apply based.
   */
  public abstract RangeSpecification prefix();

  /** Returns the ID of the format for which this specifier is an alternative. */
  public abstract String parentFormatId();

  /** Returns the arbitrary comment, possibly containing newlines, for this format. */
  public abstract Optional<String> comment();

  /** Returns the format specifier as used in the CSV representation (e.g. "20 XXX XXX"). */
  @Memoized
  public String specifier() {
    RangeSpecification prefix = prefix();
    int digitIdx = 0;
    StringBuilder buf = new StringBuilder();
    for (FormatGroup g : template().getGroups()) {
      for (int i = 0; i < g.maxLength(); i++, digitIdx++) {
        // Uppercasing is so that 'x' --> 'X'
        buf.append(digitIdx < prefix.length()
            ? Ascii.toUpperCase(RangeSpecification.toString(prefix.getBitmask(digitIdx)))
            : (i < g.minLength() ? "X" : "*"));
      }
      buf.append(" ");
    }
    buf.setLength(buf.length() - 1);
    return buf.toString();
  }
}
