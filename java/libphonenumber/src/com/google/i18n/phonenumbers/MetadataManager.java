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
        LOGGER.log(Level.WARNING, "error closing input stream (ignored)", e);
      }
    }
  }

  // The size of the byte buffer used for deserializing the alternate formats and short number
  // metadata files for each region.
  private static final int BUFFER_SIZE = 16 * 1024;

  static CodedInputByteBufferNano convertStreamToByteBuffer(ObjectInputStream in, int bufferSize)
      throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[bufferSize];

    while ((nRead = in.read(data, 0, bufferSize)) != -1) {
      outputStream.write(data, 0, nRead);
    }

    outputStream.flush();
    return CodedInputByteBufferNano.newInstance(outputStream.toByteArray());
  }

  /**
   * Loads and returns the metadata protocol buffer from the given stream and closes the stream.
   */
  static PhoneMetadataCollection loadMetadataAndCloseInput(InputStream source, int bufferSize) {
    ObjectInputStream ois;
    try {
      ois = new ObjectInputStream(source);
    } catch (IOException e) {
      close(source);
      throw new RuntimeException("cannot load/parse metadata", e);
    }

    try {
      PhoneMetadataCollection metadataCollection = new PhoneMetadataCollection();
      try {
        metadataCollection.mergeFrom(convertStreamToByteBuffer(ois, bufferSize));
      } catch (IOException e) {
        throw new RuntimeException("cannot load/parse metadata", e);
      }
      return metadataCollection;
    } finally {
      close(ois);
    }
  }

  private static void loadAlternateFormatsMetadataFromFile(int countryCallingCode) {
    String fileName = ALTERNATE_FORMATS_FILE_PREFIX + "_" + countryCallingCode;
    InputStream source = PhoneNumberMatcher.class.getResourceAsStream(fileName);
    if (source == null) {
      throw new IllegalStateException("missing metadata: " + fileName);
    }
    PhoneMetadataCollection metadataCollection = loadMetadataAndCloseInput(source, BUFFER_SIZE);
    for (PhoneMetadata metadata : metadataCollection.metadata) {
      callingCodeToAlternateFormatsMap.put(metadata.countryCode, metadata);
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
    if (source == null) {
      throw new IllegalStateException("missing metadata: " + fileName);
    }
    PhoneMetadataCollection metadataCollection = loadMetadataAndCloseInput(source, BUFFER_SIZE);
    for (PhoneMetadata metadata : metadataCollection.metadata) {
        regionCodeToShortNumberMetadataMap.put(regionCode, metadata);
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
