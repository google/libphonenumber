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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.function.Function.identity;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.MetadataKey;
import java.util.List;

/**
 * Collection of numbering schemes, mapped primarily by calling code, but available via other
 * mappings (e.g. metadata key) for convenience.
 */
// TODO: Delete this (it's hardly used and very little more than a simple collection).
@AutoValue
public abstract class NumberingSchemes {
  /**
   * Aggregates a list of numbering schemes into a single collection which mirrors the structure and
   * mapping of the libphonenumber XML metadata file.
   */
  public static NumberingSchemes from(List<NumberingScheme> schemes) {
    ImmutableMap<DigitSequence, NumberingScheme> map =
        schemes.stream().collect(toImmutableMap(NumberingScheme::getCallingCode, identity()));
    ImmutableSet<MetadataKey> allKeys = map.values().stream()
        .flatMap(s -> s.getRegions().stream().map(r -> MetadataKey.create(r, s.getCallingCode())))
        .collect(toImmutableSet());
    return new AutoValue_NumberingSchemes(map, allKeys);
  }

  /** Returns a mapping of top-level numbering schemes by calling code. */
  // TODO: Rename to getSchemeMap() since it's confusing, or add a direct getter.
  public abstract ImmutableMap<DigitSequence, NumberingScheme> getSchemes();

  /** Returns the set of all calling codes for top-level schemes in this collection. */
  public ImmutableSet<DigitSequence> getCallingCodes() {
    return getSchemes().keySet();
  }

  /** Returns the set of all metadata keys for regional schemes in this collection. */
  public abstract ImmutableSet<MetadataKey> getKeys();

  // Visible for AutoValue.
  NumberingSchemes() {}
}
