/*
 * Copyright (C) 2013 The Libphonenumber Authors
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

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.prefixmapper.PrefixFileReader;

import java.util.Locale;

/**
 * A phone prefix mapper which provides carrier information related to a phone number.
 *
 * @author Cecilia Roes
 */
public class PhoneNumberToCarrierMapper {
  private static PhoneNumberToCarrierMapper instance = null;
  private static final String MAPPING_DATA_DIRECTORY =
      "/com/google/i18n/phonenumbers/carrier/data/";
  private PrefixFileReader prefixFileReader = null;

  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

  // @VisibleForTesting
  PhoneNumberToCarrierMapper(String phonePrefixDataDirectory) {
    prefixFileReader = new PrefixFileReader(phonePrefixDataDirectory);
  }

  /**
   * Gets a {@link PhoneNumberToCarrierMapper} instance to carry out international carrier lookup.
   *
   * <p> The {@link PhoneNumberToCarrierMapper} is implemented as a singleton. Therefore, calling
   * this method multiple times will only result in one instance being created.
   *
   * @return  a {@link PhoneNumberToCarrierMapper} instance
   */
  public static synchronized PhoneNumberToCarrierMapper getInstance() {
    if (instance == null) {
      instance = new PhoneNumberToCarrierMapper(MAPPING_DATA_DIRECTORY);
    }
    return instance;
  }

  /**
   * Returns a text description for the given phone number, in the language provided. The
   * description consists of the name of the carrier the number was originally allocated to, however
   * if the country supports mobile number portability the number might not belong to the returned
   * carrier anymore. If no mapping is found an empty string is returned.
   *
   * <p>This method assumes the validity of the number passed in has already been checked, and that
   * the number is suitable for carrier lookup. We consider mobile and pager numbers possible
   * candidates for carrier lookup.
   *
   * @param number  a valid phone number for which we want to get a text description
   * @param languageCode  the language code for which the description should be written
   * @return  a text description for the given language code for the given phone number
   */
  public String getDescriptionForValidNumber(PhoneNumber number, Locale languageCode) {
    String langStr = languageCode.getLanguage();
    String scriptStr = "";  // No script is specified
    String regionStr = languageCode.getCountry();

    return prefixFileReader.getDescriptionForNumber(number, langStr, scriptStr, regionStr);
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
    PhoneNumberType numberType = phoneUtil.getNumberType(number);
    if (isMobile(numberType)) {
      return getDescriptionForValidNumber(number, languageCode);
    }
    return "";
  }

  /**
   * Checks if the supplied number type supports carrier lookup.
   */
  private boolean isMobile(PhoneNumberType numberType) {
    return (numberType == PhoneNumberType.MOBILE ||
            numberType == PhoneNumberType.FIXED_LINE_OR_MOBILE ||
            numberType == PhoneNumberType.PAGER);
  }
}
