/*
 * Copyright (C) 2015 The Libphonenumber Authors
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

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link MetadataSource} that reads from multiple resource files.
 */
final class MultiFileMetadataSourceImpl implements MetadataSource {
  // The prefix of the binary files containing phone number metadata for different regions.
  // This enables us to set up with different metadata, such as for testing.
  private final String phoneNumberMetadataFilePrefix;

  // The {@link MetadataLoader} used to inject alternative metadata sources.
  private final MetadataLoader metadataLoader;

  // A mapping from a region code to the phone number metadata for that region code.
  // Unlike the mappings for alternate formats and short number metadata, the phone number metadata
  // is loaded from a non-statically determined file prefix; therefore this map is bound to the
  // instance and not static.
  private final ConcurrentHashMap<String, PhoneMetadata> geographicalRegions =
      new ConcurrentHashMap<String, PhoneMetadata>();

  // A mapping from a country calling code for a non-geographical entity to the phone number
  // metadata for that country calling code. Examples of the country calling codes include 800
  // (International Toll Free Service) and 808 (International Shared Cost Service).
  // Unlike the mappings for alternate formats and short number metadata, the phone number metadata
  // is loaded from a non-statically determined file prefix; therefore this map is bound to the
  // instance and not static.
  private final ConcurrentHashMap<Integer, PhoneMetadata> nonGeographicalRegions =
      new ConcurrentHashMap<Integer, PhoneMetadata>();

  // It is assumed that metadataLoader is not null. Checks should happen before passing it in here.
  // @VisibleForTesting
  MultiFileMetadataSourceImpl(String phoneNumberMetadataFilePrefix, MetadataLoader metadataLoader) {
    this.phoneNumberMetadataFilePrefix = phoneNumberMetadataFilePrefix;
    this.metadataLoader = metadataLoader;
  }

  // It is assumed that metadataLoader is not null. Checks should happen before passing it in here.
  MultiFileMetadataSourceImpl(MetadataLoader metadataLoader) {
    this(MetadataManager.MULTI_FILE_PHONE_NUMBER_METADATA_FILE_PREFIX, metadataLoader);
  }

  @Override
  public PhoneMetadata getMetadataForRegion(String regionCode) {
    return MetadataManager.getMetadataFromMultiFilePrefix(regionCode, geographicalRegions,
        phoneNumberMetadataFilePrefix, metadataLoader);
  }

  @Override
  public PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode) {
    if (!isNonGeographical(countryCallingCode)) {
      // The given country calling code was for a geographical region.
      return null;
    }
    return MetadataManager.getMetadataFromMultiFilePrefix(countryCallingCode, nonGeographicalRegions,
        phoneNumberMetadataFilePrefix, metadataLoader);
  }

  // A country calling code is non-geographical if it only maps to the non-geographical region code,
  // i.e. "001".
  private boolean isNonGeographical(int countryCallingCode) {
    List<String> regionCodes =
        CountryCodeToRegionCodeMap.getCountryCodeToRegionCodeMap().get(countryCallingCode);
    return (regionCodes.size() == 1
        && PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCodes.get(0)));
  }
}
