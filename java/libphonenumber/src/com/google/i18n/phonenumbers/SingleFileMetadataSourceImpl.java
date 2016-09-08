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
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implementation of {@link MetadataSource} that reads from a single resource file.
 */
final class SingleFileMetadataSourceImpl implements MetadataSource {

  private static final Logger logger =
      Logger.getLogger(SingleFileMetadataSourceImpl.class.getName());

  private static final String META_DATA_FILE_NAME =
      "/com/google/i18n/phonenumbers/data/SingleFilePhoneNumberMetadataProto";

  // A mapping from a region code to the PhoneMetadata for that region.
  // Note: Synchronization, though only needed for the Android version of the library, is used in
  // all versions for consistency.
  private final Map<String, PhoneMetadata> regionToMetadataMap =
      Collections.synchronizedMap(new HashMap<String, PhoneMetadata>());

  // A mapping from a country calling code for a non-geographical entity to the PhoneMetadata for
  // that country calling code. Examples of the country calling codes include 800 (International
  // Toll Free Service) and 808 (International Shared Cost Service).
  // Note: Synchronization, though only needed for the Android version of the library, is used in
  // all versions for consistency.
  private final Map<Integer, PhoneMetadata> countryCodeToNonGeographicalMetadataMap =
      Collections.synchronizedMap(new HashMap<Integer, PhoneMetadata>());

  // The metadata file from which region data is loaded.
  private final String fileName;

  // The metadata loader used to inject alternative metadata sources.
  private final MetadataLoader metadataLoader;

  // It is assumed that metadataLoader is not null.
  public SingleFileMetadataSourceImpl(String fileName, MetadataLoader metadataLoader) {
    this.fileName = fileName;
    this.metadataLoader = metadataLoader;
  }

  // It is assumed that metadataLoader is not null.
  public SingleFileMetadataSourceImpl(MetadataLoader metadataLoader) {
    this(META_DATA_FILE_NAME, metadataLoader);
  }

  @Override
  public PhoneMetadata getMetadataForRegion(String regionCode) {
    synchronized (regionToMetadataMap) {
      if (!regionToMetadataMap.containsKey(regionCode)) {
        // The regionCode here will be valid and won't be '001', so we don't need to worry about
        // what to pass in for the country calling code.
        loadMetadataFromFile();
      }
    }
    return regionToMetadataMap.get(regionCode);
  }

  @Override
  public PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode) {
    synchronized (countryCodeToNonGeographicalMetadataMap) {
      if (!countryCodeToNonGeographicalMetadataMap.containsKey(countryCallingCode)) {
        loadMetadataFromFile();
      }
    }
    return countryCodeToNonGeographicalMetadataMap.get(countryCallingCode);
  }

  // @VisibleForTesting
  void loadMetadataFromFile() {
    InputStream source = metadataLoader.loadMetadata(fileName);
    if (source == null) {
      // This should not happen since clients shouldn't be using this implementation directly.
      // The single file implementation is experimental, only for when the jars contain a single
      // file with all regions' metadata. Currently we do not release such jars.
      // TODO(b/30807096): Get the MetadataManager to decide whether to use this or the multi file
      // loading depending on what data is available in the jar.
      throw new IllegalStateException("missing metadata: " + fileName);
    }
    PhoneMetadataCollection metadataCollection = MetadataManager.loadMetadataAndCloseInput(source);
    List<PhoneMetadata> metadataList = metadataCollection.getMetadataList();
    if (metadataList.isEmpty()) {
      // This should not happen since clients shouldn't be using this implementation!
      throw new IllegalStateException("empty metadata: " + fileName);
    }
    for (PhoneMetadata metadata : metadataList) {
      String regionCode = metadata.getId();
      int countryCallingCode = metadata.getCountryCode();
      boolean isNonGeoRegion = PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCode);
      if (isNonGeoRegion) {
        countryCodeToNonGeographicalMetadataMap.put(countryCallingCode, metadata);
      } else {
        regionToMetadataMap.put(regionCode, metadata);
      }
    }
  }
}
