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

package com.google.i18n.phonenumbers.metadata.source;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.internal.GeoEntityUtility;

/**
 * Implementation of {@link MetadataContainer} which is a composition of different {@link
 * MapBackedMetadataContainer}s. It adds items to a single simpler container at a time depending on
 * the content of {@link PhoneMetadata}.
 */
final class CompositeMetadataContainer implements MetadataContainer {

  private final MapBackedMetadataContainer<Integer> metadataByCountryCode =
      MapBackedMetadataContainer.byCountryCallingCode();
  private final MapBackedMetadataContainer<String> metadataByRegionCode =
      MapBackedMetadataContainer.byRegionCode();

  /**
   * Intended to be called for geographical regions only. For non-geographical entities, use {@link
   * CompositeMetadataContainer#getMetadataBy(int)}
   */
  PhoneMetadata getMetadataBy(String regionCode) {
    return metadataByRegionCode.getMetadataBy(regionCode);
  }

  /**
   * Intended to be called for non-geographical entities only, such as 800 (country code assigned to
   * the Universal International Freephone Service). For geographical regions, use {@link
   * CompositeMetadataContainer#getMetadataBy(String)}
   */
  PhoneMetadata getMetadataBy(int countryCallingCode) {
    return metadataByCountryCode.getMetadataBy(countryCallingCode);
  }

  /**
   * If the metadata belongs to a specific geographical region (it has a region code other than
   * {@link GeoEntityUtility#REGION_CODE_FOR_NON_GEO_ENTITIES}), it will be added to a {@link
   * MapBackedMetadataContainer} which stores metadata by region code. Otherwise, it will be added
   * to a {@link MapBackedMetadataContainer} which stores metadata by country calling code. This
   * means that {@link CompositeMetadataContainer#getMetadataBy(int)} will not work for country
   * calling codes such as 41 (country calling code for Switzerland), only for country calling codes
   * such as 800 (country code assigned to the Universal International Freephone Service)
   */
  @Override
  public void accept(PhoneMetadata phoneMetadata) {
    String regionCode = metadataByRegionCode.getKeyProvider().getKeyOf(phoneMetadata);
    if (GeoEntityUtility.isGeoEntity(regionCode)) {
      metadataByRegionCode.accept(phoneMetadata);
    } else {
      metadataByCountryCode.accept(phoneMetadata);
    }
  }
}
