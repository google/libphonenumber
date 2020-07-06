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
package com.google.i18n.phonenumbers.metadata.i18n;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import java.util.regex.Pattern;

/**
 * A simple type-safe identifier for BCP 47 language tags containing only language code and an
 * optional script (e.g. "en" or "zh-Hant"). This class does no canonicalization on the values its
 * given, apart from normalizing the separator to a hyphen.
 *
 * <p>We can't really use {@code Locale} here because there's an issue whereby the JDK deliberately
 * uses deprecated language tags and would, for example, convert "id" (Indonesian) to "in", which
 * is at odds with BCP 47. See {@link java.util.Locale#forLanguageTag(String) forLanguageTag()} for
 * more information.
 *
 * <p>The metadata tooling makes only minimal use of the semantics of language codes, relying on
 * them mainly as key values, and never tries to canonicalize or modify them (i.e. it is possible
 * that a language code used for this data may end up being non-canonical). It is up to any library
 * which loads the metadata at runtime to ensure that its mappings to the data account for current
 * canonicalization.
 */
@AutoValue
public abstract class SimpleLanguageTag {
  // This can be extended or modified to use Locale as necessary.
  private static final Pattern SIMPLE_TAG = Pattern.compile("[a-z]{2,3}(?:[-_][A-Z][a-z]{3})?");

  /**
   * Returns a language tag instance for the given string with minimal structural checking. If the
   * given tag uses {@code '_'} for separating language and script it's converted into {@code '-'}.
   */
  public static SimpleLanguageTag of(String lang) {
    checkArgument(SIMPLE_TAG.matcher(lang).matches(), "invalid language tag: %s", lang);
    return new AutoValue_SimpleLanguageTag(lang.replace('_', '-'));
  }

  // Visible for AutoValue only.
  abstract String lang();

  @Override
  public final String toString() {
    return lang();
  }
}
