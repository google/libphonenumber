/*
 * Copyright (C) 2022 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.internal;

import com.google.i18n.phonenumbers.CountryCodeToRegionCodeMap;
import java.util.List;

/**
 * Utility class for checking whether identifiers region code and country calling code belong
 * to geographical entities. For more information about geo vs. non-geo entities see {@link
 * com.google.i18n.phonenumbers.metadata.source.RegionMetadataSource} and {@link
 * com.google.i18n.phonenumbers.metadata.source.NonGeographicalEntityMetadataSource}
 */
public final class GeoEntityUtility {

  /** Region code with a special meaning, used to mark non-geographical entities */
  public static final String REGION_CODE_FOR_NON_GEO_ENTITIES = "001";

  /** Determines whether {@code regionCode} belongs to a geographical entity. */
  public static boolean isGeoEntity(String regionCode) {
    return !regionCode.equals(REGION_CODE_FOR_NON_GEO_ENTITIES);
  }

  /**
   * Determines whether {@code countryCallingCode} belongs to a geographical entity.
   *
   * <p>A single country calling code could map to several different regions. It is considered that
   * {@code countryCallingCode} belongs to a geo entity if all of these regions are geo entities
   *
   * <p>Note that this method will not throw an exception even when the underlying mapping for the
   * {@code countryCallingCode} does not exist, instead it will return {@code false}
   */
  public static boolean isGeoEntity(int countryCallingCode) {
    List<String> regionCodesForCountryCallingCode =
        CountryCodeToRegionCodeMap.getCountryCodeToRegionCodeMap().get(countryCallingCode);

    return regionCodesForCountryCallingCode != null
        && !regionCodesForCountryCallingCode.contains(REGION_CODE_FOR_NON_GEO_ENTITIES);
  }

  private GeoEntityUtility() {}
}
