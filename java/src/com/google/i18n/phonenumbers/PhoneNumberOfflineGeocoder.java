/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An offline geocoder which provides geographical information related to a phone number.
 *
 * @author Shaopeng Jia
 */
public class PhoneNumberOfflineGeocoder {
  private static PhoneNumberOfflineGeocoder instance = null;
  private static final String MAPPING_DATA_DIRECTORY =
      "/com/google/i18n/phonenumbers/geocoding_data/";
  private static final Logger LOGGER = Logger.getLogger(PhoneNumberOfflineGeocoder.class.getName());

  private final PhoneNumberUtil phoneUtil;
  private final String phonePrefixDataDirectory;

  // The mappingFileProvider knows for which combination of countryCallingCode and language a phone
  // prefix mapping file is available in the file system, so that a file can be loaded when needed.
  private MappingFileProvider mappingFileProvider = new MappingFileProvider();

  // A mapping from countryCallingCode_lang to the corresponding phone prefix map that has been
  // loaded.
  private Map<String, AreaCodeMap> availablePhonePrefixMaps = new HashMap<String, AreaCodeMap>();

  /**
   * For testing purposes, we allow the phone number util variable to be injected.
   *
   * @VisibleForTesting
   */
  PhoneNumberOfflineGeocoder(String phonePrefixDataDirectory, PhoneNumberUtil phoneUtil) {
    this.phoneUtil = phoneUtil;
    this.phonePrefixDataDirectory = phonePrefixDataDirectory;
    loadMappingFileProvider();
  }

  private void loadMappingFileProvider() {
    InputStream source =
        PhoneNumberOfflineGeocoder.class.getResourceAsStream(phonePrefixDataDirectory + "config");
    ObjectInputStream in;
    try {
      in = new ObjectInputStream(source);
      mappingFileProvider.readExternal(in);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    }
  }

  private AreaCodeMap getPhonePrefixDescriptions(
      int countryCallingCode, String language, String script, String region) {
    String fileName = mappingFileProvider.getFileName(countryCallingCode, language, script, region);
    if (fileName.length() == 0) {
      return null;
    }
    if (!availablePhonePrefixMaps.containsKey(fileName)) {
      loadAreaCodeMapFromFile(fileName);
    }
    return availablePhonePrefixMaps.get(fileName);
  }

  private void loadAreaCodeMapFromFile(String fileName) {
    InputStream source =
        PhoneNumberOfflineGeocoder.class.getResourceAsStream(phonePrefixDataDirectory + fileName);
    ObjectInputStream in;
    try {
      in = new ObjectInputStream(source);
      AreaCodeMap map = new AreaCodeMap();
      map.readExternal(in);
      availablePhonePrefixMaps.put(fileName, map);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    }
  }

  /**
   * Gets a {@link PhoneNumberOfflineGeocoder} instance to carry out international phone number
   * geocoding.
   *
   * <p> The {@link PhoneNumberOfflineGeocoder} is implemented as a singleton. Therefore, calling
   * this method multiple times will only result in one instance being created.
   *
   * @return  a {@link PhoneNumberOfflineGeocoder} instance
   */
  public static synchronized PhoneNumberOfflineGeocoder getInstance() {
    if (instance == null) {
      instance =
          new PhoneNumberOfflineGeocoder(MAPPING_DATA_DIRECTORY, PhoneNumberUtil.getInstance());
    }
    return instance;
  }

  /**
   * Preload the data file for the given language and country calling code, so that a future lookup
   * for this language and country calling code will not incur any file loading.
   *
   * @param locale  specifies the language of the data file to load
   * @param countryCallingCode   specifies the country calling code of phone numbers that are
   *     contained by the file to be loaded
   */
  public void loadDataFile(Locale locale, int countryCallingCode) {
    instance.getPhonePrefixDescriptions(countryCallingCode, locale.getLanguage(), "",
        locale.getCountry());
  }

  /**
   * Returns the customary display name in the given language for the given territory the phone
   * number is from.
   */
  private String getCountryNameForNumber(PhoneNumber number, Locale language) {
    String regionCode = phoneUtil.getRegionCodeForNumber(number);
    return (regionCode == null || regionCode.equals("ZZ"))
        ? "" : new Locale("", regionCode).getDisplayCountry(language);
  }

  /**
   * Returns a text description for the given language code for the given phone number. The
   * description might consist of the name of the country where the phone number is from and/or the
   * name of the geographical area the phone number is from.
   *
   * @param number  the phone number for which we want to get a text description
   * @param languageCode  the language code for which the description should be written
   * @return  a text description for the given language code for the given phone number
   */
  public String getDescriptionForNumber(PhoneNumber number, Locale languageCode) {
    String areaDescription =
        getAreaDescriptionForNumber(
            number, languageCode.getLanguage(), "",  // No script is specified.
            languageCode.getCountry());
    return (areaDescription.length() > 0)
        ? areaDescription : getCountryNameForNumber(number, languageCode);
  }

  /**
   * Returns an area-level text description in the given language for the given phone number.
   *
   * @param number  the phone number for which we want to get a text description
   * @param lang  two-letter lowercase ISO language codes as defined by ISO 639-1
   * @param script  four-letter titlecase (the first letter is uppercase and the rest of the letters
   *     are lowercase) ISO script codes as defined in ISO 15924
   * @param region  two-letter uppercase ISO country codes as defined by ISO 3166-1
   * @return  an area-level text description in the given language for the given phone number, or an
   *     empty string if such a description is not available
   */
  private String getAreaDescriptionForNumber(
      PhoneNumber number, String lang, String script, String region) {
    AreaCodeMap phonePrefixDescriptions =
        getPhonePrefixDescriptions(number.getCountryCode(), lang, script, region);
    return (phonePrefixDescriptions != null) ? phonePrefixDescriptions.lookup(number) : "";
  }
}
