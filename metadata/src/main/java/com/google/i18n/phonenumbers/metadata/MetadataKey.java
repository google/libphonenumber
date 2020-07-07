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
package com.google.i18n.phonenumbers.metadata;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import java.util.Comparator;

/**
 * A key for uniquely identifying number metadata for a region. For "geographical" regions, the
 * region code suffices to identify the range information, but for "non geographical" regions, the
 * calling code is required and the region is set to "UN001" (world).
 */
@AutoValue
public abstract class MetadataKey implements Comparable<MetadataKey> {
  private static final Comparator<MetadataKey> ORDERING =
      Comparator.comparing(MetadataKey::region).thenComparing(MetadataKey::callingCode);

  /**
   * Returns a key to identify phone number data in the given region with the specified calling
   * code. Care must be taken when creating keys because it is possible to create invalid keys that
   * would not match any data (e.g. region="US", calling code="44").
   */
  public static MetadataKey create(PhoneRegion region, DigitSequence callingCode) {
    // Null checks and semantic checks.
    Preconditions.checkArgument(region.equals(PhoneRegion.getWorld())
        || (region.toString().length() == 2 && !region.equals(PhoneRegion.getUnknown())));
    Preconditions.checkArgument(!callingCode.isEmpty());
    return new AutoValue_MetadataKey(region, callingCode);
  }

  /**
   * Returns the region for this key (this is {@link PhoneRegion#getWorld()} for non-geographical
   * regions).
   */
  public abstract PhoneRegion region();

  /** Returns the calling code for this key. */
  public abstract DigitSequence callingCode();

  @Override
  public int compareTo(MetadataKey other) {
    return ORDERING.compare(this, other);
  }

  // Used in human readable formatting during presubmit checks; be careful if you change it.
  @Override
  public final String toString() {
    return String.format("region=%s, calling code=+%s", region(), callingCode());
  }
}
