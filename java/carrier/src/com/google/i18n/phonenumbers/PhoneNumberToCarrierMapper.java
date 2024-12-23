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

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.util.Locale;

/**
 * A phone prefix mapper which provides carrier information related to a phone number.
 *
 * @author Cecilia Roes
 * @deprecated Use {@link com.google.i18n.phonenumbers.carrier.PhoneNumberToCarrierMapper} instead,
 *     which is the same class, but in a different package.
 */
@Deprecated
public class PhoneNumberToCarrierMapper {

  private static PhoneNumberToCarrierMapper instance = null;
  private final com.google.i18n.phonenumbers.carrier.PhoneNumberToCarrierMapper delegate;

  @Deprecated
  public PhoneNumberToCarrierMapper(
      com.google.i18n.phonenumbers.carrier.PhoneNumberToCarrierMapper delegate) {
    this.delegate = delegate;
  }

  /**
   * @deprecated Use
   *     {@link com.google.i18n.phonenumbers.carrier.PhoneNumberToCarrierMapper#getInstance()}
   *     instead
   */
  public static synchronized PhoneNumberToCarrierMapper getInstance() {
    if (instance == null) {
      instance = new PhoneNumberToCarrierMapper(
          com.google.i18n.phonenumbers.carrier.PhoneNumberToCarrierMapper.getInstance());
    }
    return instance;
  }

  /**
   * @deprecated Use
   *     {@link
   *     com.google.i18n.phonenumbers.carrier.PhoneNumberToCarrierMapper#getNameForValidNumber(PhoneNumber,
   *     Locale)} instead.
   */
  @Deprecated
  public String getNameForValidNumber(PhoneNumber number, Locale languageCode) {
    return delegate.getNameForValidNumber(number, languageCode);
  }

  /**
   * @deprecated Use
   *     {@link
   *     com.google.i18n.phonenumbers.carrier.PhoneNumberToCarrierMapper#getNameForNumber(PhoneNumber,
   *     Locale)} instead.
   */
  @Deprecated
  public String getNameForNumber(PhoneNumber number, Locale languageCode) {
    return delegate.getNameForNumber(number, languageCode);
  }

  /**
   * @deprecated Use
   *     {@link
   *     com.google.i18n.phonenumbers.carrier.PhoneNumberToCarrierMapper#getSafeDisplayName(PhoneNumber,
   *     Locale)} instead.
   */
  @Deprecated
  public String getSafeDisplayName(PhoneNumber number, Locale languageCode) {
    return delegate.getSafeDisplayName(number, languageCode);
  }
}
