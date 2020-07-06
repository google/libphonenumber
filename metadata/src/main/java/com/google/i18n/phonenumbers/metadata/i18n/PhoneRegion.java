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
import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import com.google.auto.value.AutoValue;
import com.ibm.icu.util.ULocale;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * A simple type-safe identifier for CLDR regions for phone numbers. Only basic checking of regions
 * is performed, but this should be fine since the set of input regions is tightly controlled.
 *
 * <p>The metadata tooling makes only minimal use of the semantics of region codes, relying on
 * them mainly as key values, and never tries to canonicalize or modify them.
 */
@AutoValue
public abstract class PhoneRegion implements Comparable<PhoneRegion> {
  // We limit the non XX region codes to just "world" for this project.
  private static final Pattern VALID_CODE = Pattern.compile("[A-Z]{2}|001");
  // Since we want "ZZ" < "001" in the ordering.
  private static Comparator<PhoneRegion> ORDERING =
      comparing(r -> r.locale().getCountry(),
          comparing(String::length).thenComparing(naturalOrder()));

  private static final PhoneRegion UNKNOWN = of("ZZ");
  private static final PhoneRegion WORLD = of("001");

  /** Returns the "world" region (001). */
  public static PhoneRegion getWorld() {
    return PhoneRegion.WORLD;
  }

  /** Returns the "unknown" region (ZZ). */
  public static PhoneRegion getUnknown() {
    return PhoneRegion.UNKNOWN;
  }

  /**
   * Returns the region identified by the given case-insensitive CLDR String representation.
   *
   * @throws IllegalArgumentException if there is no region for {@code cldrCode}
   */
  public static PhoneRegion of(String cldrCode) {
    checkArgument(VALID_CODE.matcher(cldrCode).matches(), "invalid region code: %s", cldrCode);
    return new AutoValue_PhoneRegion(new ULocale.Builder().setRegion(cldrCode).build());
  }

  @Override
  public int compareTo(PhoneRegion other) {
    return ORDERING.compare(this, other);
  }

  /** Returns the string representation for the region (either a two-letter or three-digit code). */
  @Override public final String toString() {
    String s = locale().getCountry();
    checkArgument(!s.isEmpty(), "invalid (empty) country: %s", locale());
    return s;
  }

  // Visible for AutoValue only.
  abstract ULocale locale();

  /**
   * Return an English identifier for the region in the form {@code "<region name> (<cldr code>)"}.
   * If the English name is not available, then {@code "Region: <cldr code>"} is returned. This
   * This string is only suitable for use in comments.
   *
   * @throws IllegalStateException if this method is called on the "world" region.
   */
  public String getEnglishNameForXmlComments() {
    checkState(!equals(getWorld()), "cannot ask for display name of 'world' region");
    String regionStr = locale().getCountry();
    // Use "US" so we get "en_US", and not just "en", since the policy is to use the name as it
    // would appear in America.
    String displayCountry = locale().getDisplayCountry(ULocale.US);
    return !displayCountry.isEmpty() && !displayCountry.equals(regionStr)
        ? String.format("%s (%s)", displayCountry, regionStr)
        : String.format("Region: %s", regionStr);
  }
}
