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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.metadata.DefaultMetadataDependenciesProvider;
import com.google.i18n.phonenumbers.prefixmapper.PrefixFileReader;

import java.util.List;
import java.util.Locale;

/**
 * An offline geocoder which provides geographical information related to a phone number.
 *
 * @author Shaopeng Jia
 */
public class PhoneNumberOfflineGeocoder {
  private static PhoneNumberOfflineGeocoder instance = null;
  private final PrefixFileReader prefixFileReader;

  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

  // @VisibleForTesting
  PhoneNumberOfflineGeocoder(String phonePrefixDataDirectory) {
    prefixFileReader = new PrefixFileReader(phonePrefixDataDirectory);
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
      instance = new PhoneNumberOfflineGeocoder(DefaultMetadataDependenciesProvider.getInstance()
          .getGeocodingDataDirectory());
    }
    return instance;
  }

  /**
   * Returns the customary display name in the given language for the given territory the phone
   * number is from. If it could be from many territories, nothing is returned.
   */
  private String getCountryNameForNumber(PhoneNumber number, Locale language) {
    List<String> regionCodes =
        phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
    if (regionCodes.size() == 1) {
      return getRegionDisplayName(regionCodes.get(0), language);
    } else {
      String regionWhereNumberIsValid = "ZZ";
      for (String regionCode : regionCodes) {
        if (phoneUtil.isValidNumberForRegion(number, regionCode)) {
          // If the number has already been found valid for one region, then we don't know which
          // region it belongs to so we return nothing.
          if (!regionWhereNumberIsValid.equals("ZZ")) {
            return "";
          }
          regionWhereNumberIsValid = regionCode;
        }
      }
      return getRegionDisplayName(regionWhereNumberIsValid, language);
    }
  }

  /**
   * Returns the customary display name in the given language for the given region.
   */
  private String getRegionDisplayName(String regionCode, Locale language) {
    return (regionCode == null || regionCode.equals("ZZ")
        || regionCode.equals(PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY))
        ? "" : new Locale("", regionCode).getDisplayCountry(language);
  }

  /**
   * Returns a text description for the given phone number, in the language provided. The
   * description might consist of the name of the country where the phone number is from, or the
   * name of the geographical area the phone number is from if more detailed information is
   * available.
   *
   * <p>This method assumes the validity of the number passed in has already been checked, and that
   * the number is suitable for geocoding. We consider fixed-line and mobile numbers possible
   * candidates for geocoding.
   *
   * @param number  a valid phone number for which we want to get a text description
   * @param languageCode  the language code for which the description should be written
   * @return  a text description for the given language code for the given phone number, or an
   *     empty string if the number could come from multiple countries, or the country code is
   *     in fact invalid
   */
  public String getDescriptionForValidNumber(PhoneNumber number, Locale languageCode) {
    String langStr = languageCode.getLanguage();
    String scriptStr = "";  // No script is specified
    String regionStr = languageCode.getCountry();

    String areaDescription;
    String mobileToken = PhoneNumberUtil.getCountryMobileToken(number.getCountryCode());
    String nationalNumber = phoneUtil.getNationalSignificantNumber(number);
    if (!mobileToken.equals("") && nationalNumber.startsWith(mobileToken)) {
      // In some countries, eg. Argentina, mobile numbers have a mobile token before the national
      // destination code, this should be removed before geocoding.
      nationalNumber = nationalNumber.substring(mobileToken.length());
      String region = phoneUtil.getRegionCodeForCountryCode(number.getCountryCode());
      PhoneNumber copiedNumber;
      try {
        copiedNumber = phoneUtil.parse(nationalNumber, region);
      } catch (NumberParseException e) {
        // If this happens, just reuse what we had.
        copiedNumber = number;
      }
      areaDescription = prefixFileReader.getDescriptionForNumber(copiedNumber, langStr, scriptStr,
                                                                 regionStr);
    } else {
      areaDescription = prefixFileReader.getDescriptionForNumber(number, langStr, scriptStr,
                                                                 regionStr);
    }
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
   *     description if the phone number comes from this region. It should be a two-letter
   *     upper-case CLDR region code.
   * @return  a text description for the given language code for the given phone number, or an
   *     empty string if the number could come from multiple countries, or the country code is
   *     in fact invalid
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
   *     string if the number passed in is invalid or could belong to multiple countries
   */
  public String getDescriptionForNumber(PhoneNumber number, Locale languageCode) {
    PhoneNumberType numberType = phoneUtil.getNumberType(number);
    if (numberType == PhoneNumberType.UNKNOWN) {
      return "";
    } else if (!phoneUtil.isNumberGeographical(numberType, number.getCountryCode())) {
      return getCountryNameForNumber(number, languageCode);
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
   *     description if the phone number comes from this region. It should be a two-letter
   *     upper-case CLDR region code.
   * @return  a text description for the given language code for the given phone number, or empty
   *     string if the number passed in is invalid or could belong to multiple countries
   */
  public String getDescriptionForNumber(PhoneNumber number, Locale languageCode,
                                        String userRegion) {
    PhoneNumberType numberType = phoneUtil.getNumberType(number);
    if (numberType == PhoneNumberType.UNKNOWN) {
      return "";
    } else if (!phoneUtil.isNumberGeographical(numberType, number.getCountryCode())) {
      return getCountryNameForNumber(number, languageCode);
    }
    return getDescriptionForValidNumber(number, languageCode, userRegion);
  }
}
