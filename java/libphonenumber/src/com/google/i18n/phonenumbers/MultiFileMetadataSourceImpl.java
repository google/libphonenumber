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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link MetadataSource} that reads from multiple resource files.
 */
final class MultiFileMetadataSourceImpl implements MetadataSource {

  private static final Logger logger =
      Logger.getLogger(MultiFileMetadataSourceImpl.class.getName());

  private static final String META_DATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProto";

  // A mapping from a region code to the PhoneMetadata for that region.
  private final ConcurrentHashMap<String, PhoneMetadata> geographicalRegions =
      new ConcurrentHashMap<String, PhoneMetadata>();

  // A mapping from a country calling code for a non-geographical entity to the PhoneMetadata for
  // that country calling code. Examples of the country calling codes include 800 (International
  // Toll Free Service) and 808 (International Shared Cost Service).
  private final ConcurrentHashMap<Integer, PhoneMetadata> nonGeographicalRegions =
      new ConcurrentHashMap<Integer, PhoneMetadata>();

  // The prefix of the metadata files from which region data is loaded.
  private final String filePrefix;

  // The metadata loader used to inject alternative metadata sources.
  private final MetadataLoader metadataLoader;

  // It is assumed that metadataLoader is not null. If needed, checks should happen before passing
  // here.
  // @VisibleForTesting
  MultiFileMetadataSourceImpl(String filePrefix, MetadataLoader metadataLoader) {
    this.filePrefix = filePrefix;
    this.metadataLoader = metadataLoader;
  }

  // It is assumed that metadataLoader is not null. If needed, checks should happen before passing
  // here.
  public MultiFileMetadataSourceImpl(MetadataLoader metadataLoader) {
    this(META_DATA_FILE_PREFIX, metadataLoader);
  }

  @Override
  public PhoneMetadata getMetadataForRegion(String regionCode) {
    PhoneMetadata metadata = geographicalRegions.get(regionCode);
    return (metadata != null) ? metadata : loadMetadataFromFile(
        regionCode, geographicalRegions, filePrefix, metadataLoader);
  }

  @Override
  public PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode) {
    PhoneMetadata metadata = nonGeographicalRegions.get(countryCallingCode);
    if (metadata != null) {
      return metadata;
    }
    if (isNonGeographical(countryCallingCode)) {
      return loadMetadataFromFile(
          countryCallingCode, nonGeographicalRegions, filePrefix, metadataLoader);
    }
    // The given country calling code was for a geographical region.
    return null;
  }

  // A country calling code is non-geographical if it only maps to the non-geographical region code,
  // i.e. "001".
  private boolean isNonGeographical(int countryCallingCode) {
    List<String> regionCodes =
        CountryCodeToRegionCodeMap.getCountryCodeToRegionCodeMap().get(countryCallingCode);
    return (regionCodes.size() == 1
        && PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCodes.get(0)));
  }

  /**
   * @param key             The geographical region code or the non-geographical region's country
   *                        calling code.
   * @param map             The map to contain the mapping from {@code key} to the corresponding
   *                        metadata.
   * @param filePrefix      The prefix of the metadata files from which region data is loaded.
   * @param metadataLoader  The metadata loader used to inject alternative metadata sources.
   */
  // @VisibleForTesting
  static <T> PhoneMetadata loadMetadataFromFile(
      T key, ConcurrentHashMap<T, PhoneMetadata> map, String filePrefix,
      MetadataLoader metadataLoader) {
    // We assume key.toString() is well-defined.
    String fileName = filePrefix + "_" + key;
    InputStream source = metadataLoader.loadMetadata(fileName);
    if (source == null) {
      // Sanity check; this should not happen since we only load things based on the expectation
      // that they are present, by checking the map of available data first.
      throw new IllegalStateException("missing metadata: " + fileName);
    }
    PhoneMetadataCollection metadataCollection = MetadataManager.loadMetadataAndCloseInput(source);
    List<PhoneMetadata> metadataList = metadataCollection.getMetadataList();
    if (metadataList.isEmpty()) {
      // Sanity check; this should not happen since we build with non-empty metadata.
      throw new IllegalStateException("empty metadata: " + fileName);
    }
    if (metadataList.size() > 1) {
      logger.log(Level.WARNING, "invalid metadata (too many entries): " + fileName);
    }
    PhoneMetadata metadata = metadataList.get(0);
    PhoneMetadata oldValue = map.putIfAbsent(key, metadata);
    return (oldValue != null) ? oldValue : metadata;
  }
}
