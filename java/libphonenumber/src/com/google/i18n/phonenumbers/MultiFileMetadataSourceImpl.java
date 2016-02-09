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

import com.google.i18n.phonenumbers.nano.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.nano.Phonemetadata.PhoneMetadataCollection;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  // The prefix of the metadata files from which region data is loaded.
  private final String filePrefix;

  // The metadata loader used to inject alternative metadata sources.
  private final MetadataLoader metadataLoader;

  // It is assumed that metadataLoader is not null.
  public MultiFileMetadataSourceImpl(String filePrefix, MetadataLoader metadataLoader) {
    this.filePrefix = filePrefix;
    this.metadataLoader = metadataLoader;
  }

  // It is assumed that metadataLoader is not null.
  public MultiFileMetadataSourceImpl(MetadataLoader metadataLoader) {
    this(META_DATA_FILE_PREFIX, metadataLoader);
  }

  @Override
  public PhoneMetadata getMetadataForRegion(String regionCode) {
    synchronized (regionToMetadataMap) {
      if (!regionToMetadataMap.containsKey(regionCode)) {
        // The regionCode here will be valid and won't be '001', so we don't need to worry about
        // what to pass in for the country calling code.
        loadMetadataFromFile(regionCode, 0);
      }
    }
    return regionToMetadataMap.get(regionCode);
  }

  @Override
  public PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode) {
    synchronized (countryCodeToNonGeographicalMetadataMap) {
      if (!countryCodeToNonGeographicalMetadataMap.containsKey(countryCallingCode)) {
        List<String> regionCodes =
            CountryCodeToRegionCodeMap.getCountryCodeToRegionCodeMap().get(countryCallingCode);
        // We can assume that if the country calling code maps to the non-geo entity region code
        // then that's the only region code it maps to.
        if (regionCodes.size() == 1 &&
            PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCodes.get(0))) {
          loadMetadataFromFile(PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY, countryCallingCode);
        }
      }
    }
    return countryCodeToNonGeographicalMetadataMap.get(countryCallingCode);
  }

  // @VisibleForTesting
  void loadMetadataFromFile(String regionCode, int countryCallingCode) {
    boolean isNonGeoRegion = PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCode);
    String fileName = filePrefix + "_" +
        (isNonGeoRegion ? String.valueOf(countryCallingCode) : regionCode);
    InputStream source = metadataLoader.loadMetadata(fileName);
    if (source == null) {
      logger.log(Level.SEVERE, "missing metadata: " + fileName);
      throw new IllegalStateException("missing metadata: " + fileName);
    }
    try {
      PhoneMetadataCollection metadataCollection =
          loadMetadataAndCloseInput(new ObjectInputStream(source));
      PhoneMetadata[] metadataList = metadataCollection.metadata;
      if (metadataList.length == 0) {
        logger.log(Level.SEVERE, "empty metadata: " + fileName);
        throw new IllegalStateException("empty metadata: " + fileName);
      }
      if (metadataList.length > 1) {
        logger.log(Level.WARNING, "invalid metadata (too many entries): " + fileName);
      }
      PhoneMetadata metadata = metadataList[0];
      if (isNonGeoRegion) {
        countryCodeToNonGeographicalMetadataMap.put(countryCallingCode, metadata);
      } else {
        regionToMetadataMap.put(regionCode, metadata);
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "cannot load/parse metadata: " + fileName, e);
      throw new RuntimeException("cannot load/parse metadata: " + fileName, e);
    }
  }

  /**
   * Loads the metadata protocol buffer from the given stream and closes the stream afterwards. Any
   * exceptions that occur while reading or closing the stream are ignored.
   *
   * @param source  the non-null stream from which metadata is to be read.
   * @return        the loaded metadata protocol buffer.
   */
  private static PhoneMetadataCollection loadMetadataAndCloseInput(ObjectInputStream source) {
    // The size of the byte buffer used for deserializing the phone number metadata files for each
    // region.
    final int MULTI_FILE_BUFFER_SIZE = 16 * 1024;

    PhoneMetadataCollection metadataCollection = new PhoneMetadataCollection();
    try {
      metadataCollection.mergeFrom(
          MetadataManager.convertStreamToByteBuffer(source, MULTI_FILE_BUFFER_SIZE));
    } catch (IOException e) {
      logger.log(Level.WARNING, "error reading input (ignored)", e);
    } finally {
      try {
        source.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, "error closing input stream (ignored)", e);
      }
    }
    return metadataCollection;
  }
}
