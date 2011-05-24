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

import java.util.Locale;

/**
 * A offline geocoder which provides geographical information related to a phone number.
 *
 * @author Shaopeng Jia
 */
public class PhoneNumberOfflineGeocoder {
  private static PhoneNumberOfflineGeocoder instance = null;
  private PhoneNumberUtil phoneUtil;

  /**
   * For testing purposes, we allow the phone number util variable to be injected.
   */
  PhoneNumberOfflineGeocoder(PhoneNumberUtil phoneUtil) {
    this.phoneUtil = phoneUtil;
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
      instance = new PhoneNumberOfflineGeocoder(PhoneNumberUtil.getInstance());
    }
    return instance;
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
   * Returns a text description in the given language for the given phone number. The
   * description might consist of the name of the country where the phone number is from and/or the
   * name of the geographical area the phone number is from.
   *
   * @param number  the phone number for which we want to get a text description
   * @param language  the language in which the description should be written
   * @return  a text description in the given language for the given phone number
   */
  public String getDescriptionForNumber(PhoneNumber number, Locale language) {
    // TODO: Implement logic to figure out fine-grained geographical information based
    // on area code here.
    return getCountryNameForNumber(number, language);
  }
}
