/*
 * Copyright (C) 2012 The Libphonenumber Authors
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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for loading metadata for alternate formats and short numbers. We also declare some
 * constants for phone number metadata loading, to more easily maintain all three types of metadata
 * together.
 * TODO: Consider managing phone number metadata loading here too.
 */
final class MetadataManager {
  static final String MULTI_FILE_PHONE_NUMBER_METADATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProto";
  static final String SINGLE_FILE_PHONE_NUMBER_METADATA_FILE_NAME =
      "/com/google/i18n/phonenumbers/data/SingleFilePhoneNumberMetadataProto";
  private static final String ALTERNATE_FORMATS_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberAlternateFormatsProto";
  private static final String SHORT_NUMBER_METADATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/ShortNumberMetadataProto";

  static final MetadataLoader DEFAULT_METADATA_LOADER = new MetadataLoader() {
    @Override
    public InputStream loadMetadata(String metadataFileName) {
      return MetadataManager.class.getResourceAsStream(metadataFileName);
    }
  };

  private static final Logger logger = Logger.getLogger(MetadataManager.class.getName());

  // A mapping from a country calling code to the alternate formats for that country calling code.
  private static final ConcurrentHashMap<Integer, PhoneMetadata> alternateFormatsMap =
      new ConcurrentHashMap<Integer, PhoneMetadata>();

  // A mapping from a region code to the short number metadata for that region code.
  private static final ConcurrentHashMap<String, PhoneMetadata> shortNumberMetadataMap =
      new ConcurrentHashMap<String, PhoneMetadata>();

  // The set of country calling codes for which there are alternate formats. For every country
  // calling code in this set there should be metadata linked into the resources.
  private static final Set<Integer> alternateFormatsCountryCodes =
      AlternateFormatsCountryCodeSet.getCountryCodeSet();

  // The set of region codes for which there are short number metadata. For every region code in
  // this set there should be metadata linked into the resources.
  private static final Set<String> shortNumberMetadataRegionCodes =
      ShortNumbersRegionCodeSet.getRegionCodeSet();

  private MetadataManager() {}

  static PhoneMetadata getAlternateFormatsForCountry(int countryCallingCode) {
    if (!alternateFormatsCountryCodes.contains(countryCallingCode)) {
      return null;
    }
    return getMetadataFromMultiFilePrefix(countryCallingCode, alternateFormatsMap,
        ALTERNATE_FORMATS_FILE_PREFIX, DEFAULT_METADATA_LOADER);
  }

  static PhoneMetadata getShortNumberMetadataForRegion(String regionCode) {
    if (!shortNumberMetadataRegionCodes.contains(regionCode)) {
      return null;
    }
    return getMetadataFromMultiFilePrefix(regionCode, shortNumberMetadataMap,
        SHORT_NUMBER_METADATA_FILE_PREFIX, DEFAULT_METADATA_LOADER);
  }

  static Set<String> getSupportedShortNumberRegions() {
    return Collections.unmodifiableSet(shortNumberMetadataRegionCodes);
  }

  /**
   * @param key  the lookup key for the provided map, typically a region code or a country calling
   *     code
   * @param map  the map containing mappings of already loaded metadata from their {@code key}. If
   *     this {@code key}'s metadata isn't already loaded, it will be added to this map after
   *     loading
   * @param filePrefix  the prefix of the file to load metadata from
   * @param metadataLoader  the metadata loader used to inject alternative metadata sources
   */
  static <T> PhoneMetadata getMetadataFromMultiFilePrefix(T key,
      ConcurrentHashMap<T, PhoneMetadata> map, String filePrefix, MetadataLoader metadataLoader) {
    PhoneMetadata metadata = map.get(key);
    if (metadata != null) {
      return metadata;
    }
    // We assume key.toString() is well-defined.
    String fileName = filePrefix + "_" + key;
    List<PhoneMetadata> metadataList = getMetadataFromSingleFileName(fileName, metadataLoader);
    if (metadataList.size() > 1) {
      logger.log(Level.WARNING, "more than one metadata in file " + fileName);
    }
    metadata = metadataList.get(0);
    PhoneMetadata oldValue = map.putIfAbsent(key, metadata);
    return (oldValue != null) ? oldValue : metadata;
  }

  // Loader and holder for the metadata maps loaded from a single file.
  static class SingleFileMetadataMaps {
    static SingleFileMetadataMaps load(String fileName, MetadataLoader metadataLoader) {
      List<PhoneMetadata> metadataList = getMetadataFromSingleFileName(fileName, metadataLoader);
      Map<String, PhoneMetadata> regionCodeToMetadata = new HashMap<String, PhoneMetadata>();
      Map<Integer, PhoneMetadata> countryCallingCodeToMetadata =
          new HashMap<Integer, PhoneMetadata>();
      for (PhoneMetadata metadata : metadataList) {
        String regionCode = metadata.getId();
        if (PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCode)) {
          // regionCode belongs to a non-geographical entity.
          countryCallingCodeToMetadata.put(metadata.getCountryCode(), metadata);
        } else {
          regionCodeToMetadata.put(regionCode, metadata);
        }
      }
      return new SingleFileMetadataMaps(regionCodeToMetadata, countryCallingCodeToMetadata);
    }

    // A map from a region code to the PhoneMetadata for that region.
    // For phone number metadata, the region code "001" is excluded, since that is used for the
    // non-geographical phone number entities.
    private final Map<String, PhoneMetadata> regionCodeToMetadata;

    // A map from a country calling code to the PhoneMetadata for that country calling code.
    // Examples of the country calling codes include 800 (International Toll Free Service) and 808
    // (International Shared Cost Service).
    // For phone number metadata, only the non-geographical phone number entities' country calling
    // codes are present.
    private final Map<Integer, PhoneMetadata> countryCallingCodeToMetadata;

    private SingleFileMetadataMaps(Map<String, PhoneMetadata> regionCodeToMetadata,
        Map<Integer, PhoneMetadata> countryCallingCodeToMetadata) {
      this.regionCodeToMetadata = Collections.unmodifiableMap(regionCodeToMetadata);
      this.countryCallingCodeToMetadata = Collections.unmodifiableMap(countryCallingCodeToMetadata);
    }

    PhoneMetadata get(String regionCode) {
      return regionCodeToMetadata.get(regionCode);
    }

    PhoneMetadata get(int countryCallingCode) {
      return countryCallingCodeToMetadata.get(countryCallingCode);
    }
  }

  // Manages the atomic reference lifecycle of a SingleFileMetadataMaps encapsulation.
  static SingleFileMetadataMaps getSingleFileMetadataMaps(
      AtomicReference<SingleFileMetadataMaps> ref, String fileName, MetadataLoader metadataLoader) {
    SingleFileMetadataMaps maps = ref.get();
    if (maps != null) {
      return maps;
    }
    maps = SingleFileMetadataMaps.load(fileName, metadataLoader);
    ref.compareAndSet(null, maps);
    return ref.get();
  }

  private static List<PhoneMetadata> getMetadataFromSingleFileName(String fileName,
      MetadataLoader metadataLoader) {
    InputStream source = metadataLoader.loadMetadata(fileName);
    if (source == null) {
      // Sanity check; this would only happen if we packaged jars incorrectly.
      throw new IllegalStateException("missing metadata: " + fileName);
    }
    PhoneMetadataCollection metadataCollection = loadMetadataAndCloseInput(source);
    List<PhoneMetadata> metadataList = metadataCollection.getMetadataList();
    if (metadataList.size() == 0) {
      // Sanity check; this should not happen since we build with non-empty metadata.
      throw new IllegalStateException("empty metadata: " + fileName);
    }
    return metadataList;
  }

  /**
   * Loads and returns the metadata from the given stream and closes the stream.
   *
   * @param source  the non-null stream from which metadata is to be read
   * @return  the loaded metadata
   */
  private static PhoneMetadataCollection loadMetadataAndCloseInput(InputStream source) {
    ObjectInputStream ois = null;
    try {
      try {
        ois = new ObjectInputStream(source);
      } catch (IOException e) {
        throw new RuntimeException("cannot load/parse metadata", e);
      }
      PhoneMetadataCollection metadataCollection = new PhoneMetadataCollection();
      try {
        metadataCollection.readExternal(ois);
      } catch (IOException e) {
        throw new RuntimeException("cannot load/parse metadata", e);
      }
      return metadataCollection;
    } finally {
      try {
        if (ois != null) {
          // This will close all underlying streams as well, including source.
          ois.close();
        } else {
          source.close();
        }
      } catch (IOException e) {
        logger.log(Level.WARNING, "error closing input stream (ignored)", e);
      }
    }
  }
}
