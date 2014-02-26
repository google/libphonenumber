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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class encapsulating loading of PhoneNumber Metadata information. Currently this is used only for
 * additional data files such as PhoneNumberAlternateFormats, but in the future it is envisaged it
 * would handle the main metadata file (PhoneNumberMetadata.xml) as well.
 */
class MetadataManager {
  private static final String ALTERNATE_FORMATS_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberAlternateFormatsProto";
  private static final String SHORT_NUMBER_METADATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/ShortNumberMetadataProto";

  private static final Logger LOGGER = Logger.getLogger(MetadataManager.class.getName());

  private static final Map<Integer, PhoneMetadata> callingCodeToAlternateFormatsMap =
      Collections.synchronizedMap(new HashMap<Integer, PhoneMetadata>());
  private static final Map<String, PhoneMetadata> regionCodeToShortNumberMetadataMap =
      Collections.synchronizedMap(new HashMap<String, PhoneMetadata>());

  // A set of which country calling codes there are alternate format data for. If the set has an
  // entry for a code, then there should be data for that code linked into the resources.
  private static final Set<Integer> countryCodeSet =
      AlternateFormatsCountryCodeSet.getCountryCodeSet();

  // A set of which region codes there are short number data for. If the set has an entry for a
  // code, then there should be data for that code linked into the resources.
  private static final Set<String> regionCodeSet = ShortNumbersRegionCodeSet.getRegionCodeSet();

  private MetadataManager() {
  }

  private static void close(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, e.toString());
      }
    }
  }

  private static void loadAlternateFormatsMetadataFromFile(int countryCallingCode) {
    InputStream source = PhoneNumberMatcher.class.getResourceAsStream(
        ALTERNATE_FORMATS_FILE_PREFIX + "_" + countryCallingCode);
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(source);
      PhoneMetadataCollection alternateFormats = new PhoneMetadataCollection();
      alternateFormats.readExternal(in);
      for (PhoneMetadata metadata : alternateFormats.getMetadataList()) {
        callingCodeToAlternateFormatsMap.put(metadata.getCountryCode(), metadata);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
  }

  static PhoneMetadata getAlternateFormatsForCountry(int countryCallingCode) {
    if (!countryCodeSet.contains(countryCallingCode)) {
      return null;
    }
    synchronized (callingCodeToAlternateFormatsMap) {
      if (!callingCodeToAlternateFormatsMap.containsKey(countryCallingCode)) {
        loadAlternateFormatsMetadataFromFile(countryCallingCode);
      }
    }
    return callingCodeToAlternateFormatsMap.get(countryCallingCode);
  }

  private static void loadShortNumberMetadataFromFile(String regionCode) {
    InputStream source = PhoneNumberMatcher.class.getResourceAsStream(
        SHORT_NUMBER_METADATA_FILE_PREFIX + "_" + regionCode);
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(source);
      PhoneMetadataCollection shortNumberMetadata = new PhoneMetadataCollection();
      shortNumberMetadata.readExternal(in);
      for (PhoneMetadata metadata : shortNumberMetadata.getMetadataList()) {
        regionCodeToShortNumberMetadataMap.put(regionCode, metadata);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
  }

  // @VisibleForTesting
  static Set<String> getShortNumberMetadataSupportedRegions() {
    return regionCodeSet;
  }

  static PhoneMetadata getShortNumberMetadataForRegion(String regionCode) {
    if (!regionCodeSet.contains(regionCode)) {
      return null;
    }
    synchronized (regionCodeToShortNumberMetadataMap) {
      if (!regionCodeToShortNumberMetadataMap.containsKey(regionCode)) {
        loadShortNumberMetadataFromFile(regionCode);
      }
    }
    return regionCodeToShortNumberMetadataMap.get(regionCode);
  }
}
