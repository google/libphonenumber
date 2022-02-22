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

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.metadata.DefaultMetadataDependenciesProvider;
import com.google.i18n.phonenumbers.prefixmapper.PrefixFileReader;
import java.util.Locale;

/**
 * A phone prefix mapper which provides carrier information related to a phone number.
 *
 * @author Cecilia Roes
 */
public class PhoneNumberToCarrierMapper {
  private static PhoneNumberToCarrierMapper instance = null;
  private final PrefixFileReader prefixFileReader;

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
      instance = new PhoneNumberToCarrierMapper(DefaultMetadataDependenciesProvider.getInstance()
          .getCarrierDataDirectory());
    }
    return instance;
  }

  /**
   * Returns a carrier name for the given phone number, in the language provided. The carrier name
   * is the one the number was originally allocated to, however if the country supports mobile
   * number portability the number might not belong to the returned carrier anymore. If no mapping
   * is found an empty string is returned.
   *
   * <p>This method assumes the validity of the number passed in has already been checked, and that
   * the number is suitable for carrier lookup. We consider mobile and pager numbers possible
   * candidates for carrier lookup.
   *
   * @param number  a valid phone number for which we want to get a carrier name
   * @param languageCode  the language code in which the name should be written
   * @return  a carrier name for the given phone number
   */
  public String getNameForValidNumber(PhoneNumber number, Locale languageCode) {
    String langStr = languageCode.getLanguage();
    String scriptStr = "";  // No script is specified
    String regionStr = languageCode.getCountry();

    return prefixFileReader.getDescriptionForNumber(number, langStr, scriptStr, regionStr);
  }

  /**
   * Gets the name of the carrier for the given phone number, in the language provided. As per
   * {@link #getNameForValidNumber(PhoneNumber, Locale)} but explicitly checks the validity of
   * the number passed in.
   *
   * @param number  the phone number for which we want to get a carrier name
   * @param languageCode  the language code in which the name should be written
   * @return  a carrier name for the given phone number, or empty string if the number passed in is
   *     invalid
   */
  public String getNameForNumber(PhoneNumber number, Locale languageCode) {
    PhoneNumberType numberType = phoneUtil.getNumberType(number);
    if (isMobile(numberType)) {
      return getNameForValidNumber(number, languageCode);
    }
    return "";
  }

  /**
   * Gets the name of the carrier for the given phone number only when it is 'safe' to display to
   * users. A carrier name is considered safe if the number is valid and for a region that doesn't
   * support
   * <a href="http://en.wikipedia.org/wiki/Mobile_number_portability">mobile number portability</a>.
   *
   * @param number  the phone number for which we want to get a carrier name
   * @param languageCode  the language code in which the name should be written
   * @return  a carrier name that is safe to display to users, or the empty string
   */
  public String getSafeDisplayName(PhoneNumber number, Locale languageCode) {
    if (phoneUtil.isMobileNumberPortableRegion(phoneUtil.getRegionCodeForNumber(number))) {
      return "";
    }
    return getNameForNumber(number, languageCode);
  }

  /**
   * Checks if the supplied number type supports carrier lookup.
   */
  private boolean isMobile(PhoneNumberType numberType) {
    return (numberType == PhoneNumberType.MOBILE
        || numberType == PhoneNumberType.FIXED_LINE_OR_MOBILE
        || numberType == PhoneNumberType.PAGER);
  }
}
