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

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/*
 * Utility for international short phone numbers, such as short codes and emergency numbers. Note
 * most commercial short numbers are not handled here, but by the PhoneNumberUtil.
 *
 * @author Shaopeng Jia
 * @author David Yonge-Mallo
 */
public class ShortNumberUtil {

  private final PhoneNumberUtil phoneUtil;
  private static final Logger LOGGER = Logger.getLogger(ShortNumberUtil.class.getName());

  /**
   * Cost categories of short numbers.
   */
  public enum ShortNumberCost {
    TOLL_FREE,
    STANDARD_RATE,
    PREMIUM_RATE,
    UNKNOWN_COST
  }

  public ShortNumberUtil() {
    phoneUtil = PhoneNumberUtil.getInstance();
  }

  // @VisibleForTesting
  ShortNumberUtil(PhoneNumberUtil util) {
    phoneUtil = util;
  }

  /**
   * Convenience method to get a list of what regions the library has metadata for.
   */
  public Set<String> getSupportedRegions() {
    return Collections.unmodifiableSet(MetadataManager.getShortNumberMetadataSupportedRegions());
  }

  /**
   * Gets a valid short number for the specified region.
   *
   * @param regionCode  the region for which an example short number is needed
   * @return  a valid short number for the specified region. Returns an empty string when the
   *    metadata does not contain such information.
   */
  // @VisibleForTesting
  String getExampleShortNumber(String regionCode) {
    PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
    if (null == phoneMetadata) {
      LOGGER.log(Level.WARNING, "Unable to get short number metadata for region: " + regionCode);
      return "";
    }
    PhoneNumberDesc desc = phoneMetadata.getShortCode();
    if (desc.hasExampleNumber()) {
      return desc.getExampleNumber();
    }
    return "";
  }

  /**
   * Gets a valid short number for the specified cost category.
   *
   * @param regionCode  the region for which an example short number is needed
   * @param cost  the cost category of number that is needed
   * @return  a valid short number for the specified region and cost category. Returns an empty
   *    string when the metadata does not contain such information, or the cost is UNKNOWN_COST.
   */
  // @VisibleForTesting
  String getExampleShortNumberForCost(String regionCode, ShortNumberCost cost) {
    PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
    if (null == phoneMetadata) {
      LOGGER.log(Level.WARNING, "Unable to get short number metadata for region: " + regionCode);
      return "";
    }
    PhoneNumberDesc desc = getShortNumberDescByCost(phoneMetadata, cost);
    if (desc != null && desc.hasExampleNumber()) {
      return desc.getExampleNumber();
    }
    return "";
  }

  private PhoneNumberDesc getShortNumberDescByCost(PhoneMetadata metadata, ShortNumberCost cost) {
    switch (cost) {
      case TOLL_FREE:
        return metadata.getTollFree();
      case STANDARD_RATE:
        return metadata.getStandardRate();
      case PREMIUM_RATE:
        return metadata.getPremiumRate();
      default:
        // UNKNOWN_COST numbers are computed by the process of elimination from the other cost
        // categories.
        return null;
    }
  }

  /**
   * Returns true if the number might be used to connect to an emergency service in the given
   * region.
   *
   * This method takes into account cases where the number might contain formatting, or might have
   * additional digits appended (when it is okay to do that in the region specified).
   *
   * @param number  the phone number to test
   * @param regionCode  the region where the phone number is being dialed
   * @return  if the number might be used to connect to an emergency service in the given region.
   */
  public boolean connectsToEmergencyNumber(String number, String regionCode) {
    return matchesEmergencyNumberHelper(number, regionCode, true /* allows prefix match */);
  }

  /**
   * Returns true if the number exactly matches an emergency service number in the given region.
   *
   * This method takes into account cases where the number might contain formatting, but doesn't
   * allow additional digits to be appended.
   *
   * @param number  the phone number to test
   * @param regionCode  the region where the phone number is being dialed
   * @return  if the number exactly matches an emergency services number in the given region.
   */
  public boolean isEmergencyNumber(String number, String regionCode) {
    return matchesEmergencyNumberHelper(number, regionCode, false /* doesn't allow prefix match */);
  }

  private boolean matchesEmergencyNumberHelper(String number, String regionCode,
      boolean allowPrefixMatch) {
    number = PhoneNumberUtil.extractPossibleNumber(number);
    if (PhoneNumberUtil.PLUS_CHARS_PATTERN.matcher(number).lookingAt()) {
      // Returns false if the number starts with a plus sign. We don't believe dialing the country
      // code before emergency numbers (e.g. +1911) works, but later, if that proves to work, we can
      // add additional logic here to handle it.
      return false;
    }
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion(regionCode);
    if (metadata == null || !metadata.hasEmergency()) {
      return false;
    }
    Pattern emergencyNumberPattern =
        Pattern.compile(metadata.getEmergency().getNationalNumberPattern());
    String normalizedNumber = PhoneNumberUtil.normalizeDigitsOnly(number);
    // In Brazil, emergency numbers don't work when additional digits are appended.
    return (!allowPrefixMatch || regionCode.equals("BR"))
        ? emergencyNumberPattern.matcher(normalizedNumber).matches()
        : emergencyNumberPattern.matcher(normalizedNumber).lookingAt();
  }
}
