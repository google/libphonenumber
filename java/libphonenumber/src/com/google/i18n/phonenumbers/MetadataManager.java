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

import com.google.i18n.phonenumbers.nano.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.nano.Phonemetadata.PhoneMetadataCollection;
import com.google.protobuf.nano.CodedInputByteBufferNano;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
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
  // Size of byte buffer for deserializing nano metadata files. The largest binary is 10k size so
  // 16k is sufficient for now
  private static final int BUFFER_SIZE = 16 * 1024;
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

  static CodedInputByteBufferNano convertStreamToBufferNano(ObjectInputStream in, int bufferSize) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      int nRead;
      byte[] data = new byte[bufferSize];

      while ((nRead = in.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    }
    return CodedInputByteBufferNano.newInstance(buffer.toByteArray());
  }

  private static void loadAlternateFormatsMetadataFromFile(int countryCallingCode) {
    String fileName = ALTERNATE_FORMATS_FILE_PREFIX + "_" + countryCallingCode;
    InputStream source = PhoneNumberMatcher.class.getResourceAsStream(fileName);
    try {
      ObjectInputStream src = new ObjectInputStream(source);
      CodedInputByteBufferNano buffer = convertStreamToBufferNano(src, BUFFER_SIZE);
      PhoneMetadataCollection alternateFormats = new PhoneMetadataCollection();
      alternateFormats.mergeFrom(buffer);
      for (PhoneMetadata metadata : alternateFormats.metadata) {
        callingCodeToAlternateFormatsMap.put(metadata.countryCode, metadata);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
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
    String fileName = SHORT_NUMBER_METADATA_FILE_PREFIX + "_" + regionCode;
    InputStream source = PhoneNumberMatcher.class.getResourceAsStream(fileName);
    try {
      ObjectInputStream src = new ObjectInputStream(source);
      CodedInputByteBufferNano buffer = convertStreamToBufferNano(src, BUFFER_SIZE);
      PhoneMetadataCollection shortNumberMetadata = new PhoneMetadataCollection();
      shortNumberMetadata.mergeFrom(buffer);
      for (PhoneMetadata metadata : shortNumberMetadata.metadata) {
        regionCodeToShortNumberMetadataMap.put(regionCode, metadata);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
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
