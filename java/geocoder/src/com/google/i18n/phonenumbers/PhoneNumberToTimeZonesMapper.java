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

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.prefixmapper.PrefixTimeZonesMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An offline mapper from phone numbers to time zones.
 */
public class PhoneNumberToTimeZonesMapper {
  private static final String MAPPING_DATA_DIRECTORY =
      "/com/google/i18n/phonenumbers/timezones/data/";
  private static final String MAPPING_DATA_FILE_NAME = "map_data";
  // This is defined by ICU as the unknown time zone.
  private static final String UNKNOWN_TIMEZONE = "Etc/Unknown";
  // A list with the ICU unknown time zone as single element.
  // @VisibleForTesting
  static final List<String> UNKNOWN_TIME_ZONE_LIST = new ArrayList<String>(1);
  static {
    UNKNOWN_TIME_ZONE_LIST.add(UNKNOWN_TIMEZONE);
  }

  private static final Logger logger =
      Logger.getLogger(PhoneNumberToTimeZonesMapper.class.getName());

  private PrefixTimeZonesMap prefixTimeZonesMap = null;

  // @VisibleForTesting
  PhoneNumberToTimeZonesMapper(String prefixTimeZonesMapDataDirectory) {
    this.prefixTimeZonesMap = loadPrefixTimeZonesMapFromFile(
        prefixTimeZonesMapDataDirectory + MAPPING_DATA_FILE_NAME);
  }

  private PhoneNumberToTimeZonesMapper(PrefixTimeZonesMap prefixTimeZonesMap) {
    this.prefixTimeZonesMap = prefixTimeZonesMap;
  }

  private static PrefixTimeZonesMap loadPrefixTimeZonesMapFromFile(String path) {
    InputStream source = PhoneNumberToTimeZonesMapper.class.getResourceAsStream(path);
    ObjectInputStream in = null;
    PrefixTimeZonesMap map = new PrefixTimeZonesMap();
    try {
      in = new ObjectInputStream(source);
      map.readExternal(in);
    } catch (IOException e) {
      logger.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
    return map;
  }

  private static void close(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
        logger.log(Level.WARNING, e.toString());
      }
    }
  }

  /**
   * Helper class used for lazy instantiation of a PhoneNumberToTimeZonesMapper. This also loads the
   * map data in a thread-safe way.
   */
  private static class LazyHolder {
    private static final PhoneNumberToTimeZonesMapper INSTANCE;
    static {
      PrefixTimeZonesMap map =
          loadPrefixTimeZonesMapFromFile(MAPPING_DATA_DIRECTORY + MAPPING_DATA_FILE_NAME);
      INSTANCE = new PhoneNumberToTimeZonesMapper(map);
    }
  }

  /**
   * Gets a {@link PhoneNumberToTimeZonesMapper} instance.
   *
   * <p> The {@link PhoneNumberToTimeZonesMapper} is implemented as a singleton. Therefore, calling
   * this method multiple times will only result in one instance being created.
   *
   * @return  a {@link PhoneNumberToTimeZonesMapper} instance
   */
  public static synchronized PhoneNumberToTimeZonesMapper getInstance() {
    return LazyHolder.INSTANCE;
  }

  /**
   * Returns a list of time zones to which a phone number belongs.
   *
   * <p>This method assumes the validity of the number passed in has already been checked, and that
   * the number is geo-localizable. We consider fixed-line and mobile numbers possible candidates
   * for geo-localization.
   *
   * @param number  a valid phone number for which we want to get the time zones to which it belongs
   * @return  a list of the corresponding time zones or a single element list with the default
   *     unknown time zone if no other time zone was found or if the number was invalid
   */
  public List<String> getTimeZonesForGeographicalNumber(PhoneNumber number) {
    return getTimeZonesForGeocodableNumber(number);
  }

  /**
   * As per {@link #getTimeZonesForGeographicalNumber(PhoneNumber)} but explicitly checks
   * the validity of the number passed in.
   *
   * @param number  the phone number for which we want to get the time zones to which it belongs
   * @return  a list of the corresponding time zones or a single element list with the default
   *     unknown time zone if no other time zone was found or if the number was invalid
   */
  public List<String> getTimeZonesForNumber(PhoneNumber number) {
    PhoneNumberType numberType = PhoneNumberUtil.getInstance().getNumberType(number);
    if (numberType == PhoneNumberType.UNKNOWN) {
      return UNKNOWN_TIME_ZONE_LIST;
    } else if (!PhoneNumberUtil.getInstance().isNumberGeographical(
        numberType, number.getCountryCode())) {
      return getCountryLevelTimeZonesforNumber(number);
    }
    return getTimeZonesForGeographicalNumber(number);
  }

  /**
   * Returns a String with the ICU unknown time zone.
   */
  public static String getUnknownTimeZone() {
    return UNKNOWN_TIMEZONE;
  }

  /**
   * Returns a list of time zones to which a geocodable phone number belongs.
   *
   * @param number  the phone number for which we want to get the time zones to which it belongs
   * @return  the list of corresponding  time zones or a single element list with the default
   *     unknown time zone if no other time zone was found or if the number was invalid
   */
  private List<String> getTimeZonesForGeocodableNumber(PhoneNumber number) {
    List<String> timezones = prefixTimeZonesMap.lookupTimeZonesForNumber(number);
    return Collections.unmodifiableList(timezones.isEmpty() ? UNKNOWN_TIME_ZONE_LIST
                                                            : timezones);
  }

  /**
   * Returns the list of time zones corresponding to the country calling code of {@code number}.
   *
   * @param number  the phone number to look up
   * @return  the list of corresponding time zones or a single element list with the default
   *     unknown time zone if no other time zone was found
   */
  private List<String> getCountryLevelTimeZonesforNumber(PhoneNumber number) {
    List<String> timezones = prefixTimeZonesMap.lookupCountryLevelTimeZonesForNumber(number);
    return Collections.unmodifiableList(timezones.isEmpty() ? UNKNOWN_TIME_ZONE_LIST
                                                            : timezones);
  }
}
