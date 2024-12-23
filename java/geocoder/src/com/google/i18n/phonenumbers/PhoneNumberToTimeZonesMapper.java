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

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.util.List;

/**
 * An offline mapper from phone numbers to time zones.
 *
 * @deprecated Use {@link com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper}
 *     instead, which is the same class, but in a different package.
 */
@Deprecated
public class PhoneNumberToTimeZonesMapper {

  private final com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper delegate;

  @Deprecated
  public PhoneNumberToTimeZonesMapper(
      com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper delegate) {
    this.delegate = delegate;
  }

  /**
   * @deprecated Use
   *     {@link com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper#getInstance()}
   *     instead.
   */
  @Deprecated
  public static synchronized PhoneNumberToTimeZonesMapper getInstance() {
    return new PhoneNumberToTimeZonesMapper(
        com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper.getInstance());
  }

  /**
   * @deprecated Use
   *     {@link
   *     com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper#getTimeZonesForGeographicalNumber(PhoneNumber)}
   *     instead.
   */
  @Deprecated
  public List<String> getTimeZonesForGeographicalNumber(PhoneNumber number) {
    return delegate.getTimeZonesForGeographicalNumber(number);
  }

  /**
   * @deprecated Use
   *     {@link
   *     com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper#getTimeZonesForNumber(PhoneNumber)}
   *     instead.
   */
  @Deprecated
  public List<String> getTimeZonesForNumber(PhoneNumber number) {
    return delegate.getTimeZonesForNumber(number);
  }

  /**
   * @deprecated Use
   *     {@link
   *     com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper#getUnknownTimeZone()}
   *     instead.
   */
  public static String getUnknownTimeZone() {
    return com.google.i18n.phonenumbers.timezones.PhoneNumberToTimeZonesMapper.getUnknownTimeZone();
  }
}
