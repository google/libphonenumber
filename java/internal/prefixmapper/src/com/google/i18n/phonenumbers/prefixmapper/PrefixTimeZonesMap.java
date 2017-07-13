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

package com.google.i18n.phonenumbers.prefixmapper;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;

/**
 * A utility that maps phone number prefixes to a list of strings describing the time zones to
 * which each prefix belongs.
 */
public class PrefixTimeZonesMap implements Externalizable {
  private final PhonePrefixMap phonePrefixMap = new PhonePrefixMap();
  private static final String RAW_STRING_TIMEZONES_SEPARATOR = "&";

  /**
    * Creates a {@link PrefixTimeZonesMap} initialized with {@code sortedPrefixTimeZoneMap}.  Note
    * that the underlying implementation of this method is expensive thus should not be called by
    * time-critical applications.
    *
    * @param sortedPrefixTimeZoneMap  a map from phone number prefixes to their corresponding time
    * zones, sorted in ascending order of the phone number prefixes as integers.
    */
  public void readPrefixTimeZonesMap(SortedMap<Integer, String> sortedPrefixTimeZoneMap) {
    phonePrefixMap.readPhonePrefixMap(sortedPrefixTimeZoneMap);
  }

  /**
   * Supports Java Serialization.
   */
  public void writeExternal(ObjectOutput objectOutput) throws IOException {
    phonePrefixMap.writeExternal(objectOutput);
  }

  public void readExternal(ObjectInput objectInput) throws IOException {
    phonePrefixMap.readExternal(objectInput);
  }

  /**
   * Returns the list of time zones {@code key} corresponds to.
   *
   * <p>{@code key} could be the calling country code and the full significant number of a
   * certain number, or it could be just a phone-number prefix.
   * For example, the full number 16502530000 (from the phone-number +1 650 253 0000) is a valid
   * input. Also, any of its prefixes, such as 16502, is also valid.
   *
   * @param key  the key to look up
   * @return  the list of corresponding time zones
   */
  private List<String> lookupTimeZonesForNumber(long key) {
    // Lookup in the map data. The returned String may consist of several time zones, so it must be
    // split.
    String timezonesString = phonePrefixMap.lookup(key);
    if (timezonesString == null) {
      return new LinkedList<String>();
    }
    return tokenizeRawOutputString(timezonesString);
  }

  /**
   * As per {@link #lookupTimeZonesForNumber(long)}, but receives the number as a PhoneNumber
   * instead of a long.
   *
   * @param number  the phone number to look up
   * @return  the list of corresponding time zones
   */
  public List<String> lookupTimeZonesForNumber(PhoneNumber number) {
    long phonePrefix = Long.parseLong(number.getCountryCode()
        + PhoneNumberUtil.getInstance().getNationalSignificantNumber(number));
    return lookupTimeZonesForNumber(phonePrefix);
  }

  /**
   * Returns the list of time zones {@code number}'s calling country code corresponds to.
   *
   * @param number  the phone number to look up
   * @return  the list of corresponding time zones
   */
  public List<String> lookupCountryLevelTimeZonesForNumber(PhoneNumber number) {
    return lookupTimeZonesForNumber(number.getCountryCode());
  }

  /**
   * Split {@code timezonesString} into all the time zones that are part of it.
   */
  private List<String> tokenizeRawOutputString(String timezonesString) {
    StringTokenizer tokenizer = new StringTokenizer(timezonesString,
                                                    RAW_STRING_TIMEZONES_SEPARATOR);
    LinkedList<String> timezonesList = new LinkedList<String>();
    while (tokenizer.hasMoreTokens()) {
      timezonesList.add(tokenizer.nextToken());
    }
    return timezonesList;
  }

  /**
   * Dumps the mappings contained in the phone prefix map.
   */
  @Override
  public String toString() {
    return phonePrefixMap.toString();
  }
}
