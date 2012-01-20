/*
 * Copyright (C) 2011 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.geocoding;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
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
      "/com/google/i18n/phonenumbers/geocoding/data/";
  private static final Logger LOGGER = Logger.getLogger(PhoneNumberOfflineGeocoder.class.getName());

  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private final String phonePrefixDataDirectory;

  // The mappingFileProvider knows for which combination of countryCallingCode and language a phone
  // prefix mapping file is available in the file system, so that a file can be loaded when needed.
  private MappingFileProvider mappingFileProvider = new MappingFileProvider();

  // A mapping from countryCallingCode_lang to the corresponding phone prefix map that has been
  // loaded.
  private Map<String, AreaCodeMap> availablePhonePrefixMaps = new HashMap<String, AreaCodeMap>();

  // @VisibleForTesting
  PhoneNumberOfflineGeocoder(String phonePrefixDataDirectory) {
    this.phonePrefixDataDirectory = phonePrefixDataDirectory;
    loadMappingFileProvider();
  }

  private void loadMappingFileProvider() {
    InputStream source =
        PhoneNumberOfflineGeocoder.class.getResourceAsStream(phonePrefixDataDirectory + "config");
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(source);
      mappingFileProvider.readExternal(in);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
  }

  private AreaCodeMap getPhonePrefixDescriptions(
      int prefixMapKey, String language, String script, String region) {
    String fileName = mappingFileProvider.getFileName(prefixMapKey, language, script, region);
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
    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(source);
      AreaCodeMap map = new AreaCodeMap();
      map.readExternal(in);
      availablePhonePrefixMaps.put(fileName, map);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
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
      instance = new PhoneNumberOfflineGeocoder(MAPPING_DATA_DIRECTORY);
    }
    return instance;
  }

  /**
   * Returns the customary display name in the given language for the given territory the phone
   * number is from.
   */
  private String getCountryNameForNumber(PhoneNumber number, Locale language) {
    String regionCode = phoneUtil.getRegionCodeForNumber(number);
    return getRegionDisplayName(regionCode, language);
  }

  /**
   * Returns the customary display name in the given language for the given region.
   */
  private String getRegionDisplayName(String regionCode, Locale language) {
    return (regionCode == null || regionCode.equals("ZZ") ||
            regionCode.equals(PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY))
        ? "" : new Locale("", regionCode).getDisplayCountry(language);
  }

  /**
   * Returns a text description for the given phone number, in the language provided. The
   * description might consist of the name of the country where the phone number is from, or the
   * name of the geographical area the phone number is from if more detailed information is
   * available.
   *
   * <p>This method assumes the validity of the number passed in has already been checked.
   *
   * @param number  a valid phone number for which we want to get a text description
   * @param languageCode  the language code for which the description should be written
   * @return  a text description for the given language code for the given phone number
   */
  public String getDescriptionForValidNumber(PhoneNumber number, Locale languageCode) {
    String langStr = languageCode.getLanguage();
    String scriptStr = "";  // No script is specified
    String regionStr = languageCode.getCountry();

    String areaDescription =
        getAreaDescriptionForNumber(number, langStr, scriptStr, regionStr);
    return (areaDescription.length() > 0)
        ? areaDescription : getCountryNameForNumber(number, languageCode);
  }

  /**
   * As per {@link #getDescriptionForValidNumber(PhoneNumber, Locale)} but also considers the
   * region of the user. If the phone number is from the same region as the user, only a lower-level
   * description will be returned, if one exists. Otherwise, the phone number's region will be
   * returned, with optionally some more detailed information.
   *
   * <p>For example, for a user from the region "US" (United States), we would show "Mountain View,
   * CA" for a particular number, omitting the United States from the description. For a user from
   * the United Kingdom (region "GB"), for the same number we may show "Mountain View, CA, United
   * States" or even just "United States".
   *
   * <p>This method assumes the validity of the number passed in has already been checked.
   *
   * @param number  the phone number for which we want to get a text description
   * @param languageCode  the language code for which the description should be written
   * @param userRegion  the region code for a given user. This region will be omitted from the
   *     description if the phone number comes from this region. It is a two-letter uppercase ISO
   *     country code as defined by ISO 3166-1.
   * @return  a text description for the given language code for the given phone number, or empty
   *     string if the number passed in is invalid
   */
  public String getDescriptionForValidNumber(PhoneNumber number, Locale languageCode,
                                             String userRegion) {
    // If the user region matches the number's region, then we just show the lower-level
    // description, if one exists - if no description exists, we will show the region(country) name
    // for the number.
    String regionCode = phoneUtil.getRegionCodeForNumber(number);
    if (userRegion.equals(regionCode)) {
      return getDescriptionForValidNumber(number, languageCode);
    }
    // Otherwise, we just show the region(country) name for now.
    return getRegionDisplayName(regionCode, languageCode);
    // TODO: Concatenate the lower-level and country-name information in an appropriate
    // way for each language.
  }

  /**
   * As per {@link #getDescriptionForValidNumber(PhoneNumber, Locale)} but explicitly checks
   * the validity of the number passed in.
   *
   * @param number  the phone number for which we want to get a text description
   * @param languageCode  the language code for which the description should be written
   * @return  a text description for the given language code for the given phone number, or empty
   *     string if the number passed in is invalid
   */
  public String getDescriptionForNumber(PhoneNumber number, Locale languageCode) {
    if (!phoneUtil.isValidNumber(number)) {
      return "";
    }
    return getDescriptionForValidNumber(number, languageCode);
  }

  /**
   * As per {@link #getDescriptionForValidNumber(PhoneNumber, Locale, String)} but
   * explicitly checks the validity of the number passed in.
   *
   * @param number  the phone number for which we want to get a text description
   * @param languageCode  the language code for which the description should be written
   * @param userRegion  the region code for a given user. This region will be omitted from the
   *     description if the phone number comes from this region. It is a two-letter uppercase ISO
   *     country code as defined by ISO 3166-1.
   * @return  a text description for the given language code for the given phone number, or empty
   *     string if the number passed in is invalid
   */
  public String getDescriptionForNumber(PhoneNumber number, Locale languageCode,
                                        String userRegion) {
    if (!phoneUtil.isValidNumber(number)) {
      return "";
    }
    return getDescriptionForValidNumber(number, languageCode, userRegion);
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
    int countryCallingCode = number.getCountryCode();
    // As the NANPA data is split into multiple files covering 3-digit areas, use a phone number
    // prefix of 4 digits for NANPA instead, e.g. 1650.
    int phonePrefix = (countryCallingCode != 1) ?
        countryCallingCode : (1000 + (int) (number.getNationalNumber() / 10000000));
    AreaCodeMap phonePrefixDescriptions =
        getPhonePrefixDescriptions(phonePrefix, lang, script, region);
    String description = (phonePrefixDescriptions != null)
        ? phonePrefixDescriptions.lookup(number)
        : null;
    // When a location is not available in the requested language, fall back to English.
    if ((description == null || description.length() == 0) && mayFallBackToEnglish(lang)) {
      AreaCodeMap defaultMap = getPhonePrefixDescriptions(phonePrefix, "en", "", "");
      if (defaultMap == null) {
        return "";
      }
      description = defaultMap.lookup(number);
    }
    return description != null ? description : "";
  }

  private boolean mayFallBackToEnglish(String lang) {
    // Don't fall back to English if the requested language is among the following:
    // - Chinese
    // - Japanese
    // - Korean
    return !lang.equals("zh") && !lang.equals("ja") && !lang.equals("ko");
  }
}
