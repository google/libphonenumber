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

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Methods for getting information about short phone numbers, such as short codes and emergency
 * numbers. Note that most commercial short numbers are not handled here, but by the
 * {@link PhoneNumberUtil}.
 *
 * @author Shaopeng Jia
 * @author David Yonge-Mallo
 */
public class ShortNumberInfo {
  private static final Logger logger = Logger.getLogger(ShortNumberInfo.class.getName());

  private static final ShortNumberInfo INSTANCE =
      new ShortNumberInfo(PhoneNumberUtil.getInstance());

  // In these countries, if extra digits are added to an emergency number, it no longer connects
  // to the emergency service.
  private static final Set<String> REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT =
      new HashSet<String>();
  static {
    REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("BR");
    REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("CL");
    REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("NI");
  }

  /** Cost categories of short numbers. */
  public enum ShortNumberCost {
    TOLL_FREE,
    STANDARD_RATE,
    PREMIUM_RATE,
    UNKNOWN_COST
  }

  /** Returns the singleton instance of the ShortNumberInfo. */
  public static ShortNumberInfo getInstance() {
    return INSTANCE;
  }

  private final PhoneNumberUtil phoneUtil;

  // @VisibleForTesting
  ShortNumberInfo(PhoneNumberUtil util) {
    phoneUtil = util;
  }

  /**
   * Check whether a short number is a possible number when dialled from a region, given the number
   * in the form of a string, and the region where the number is dialed from. This provides a more
   * lenient check than {@link #isValidShortNumberForRegion}.
   *
   * @param shortNumber the short number to check as a string
   * @param regionDialingFrom the region from which the number is dialed
   * @return whether the number is a possible short number
   */
  public boolean isPossibleShortNumberForRegion(String shortNumber, String regionDialingFrom) {
    PhoneMetadata phoneMetadata =
        MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
    if (phoneMetadata == null) {
      return false;
    }
    PhoneNumberDesc generalDesc = phoneMetadata.getGeneralDesc();
    return phoneUtil.isNumberPossibleForDesc(shortNumber, generalDesc);
  }

  /**
   * Check whether a short number is a possible number. If a country calling code is shared by
   * multiple regions, this returns true if it's possible in any of them. This provides a more
   * lenient check than {@link #isValidShortNumber}. See {@link
   * #isPossibleShortNumberForRegion(String, String)} for details.
   *
   * @param number the short number to check
   * @return whether the number is a possible short number
   */
  public boolean isPossibleShortNumber(PhoneNumber number) {
    List<String> regionCodes = phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
    String shortNumber = phoneUtil.getNationalSignificantNumber(number);
    for (String region : regionCodes) {
      PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(region);
      if (phoneUtil.isNumberPossibleForDesc(shortNumber, phoneMetadata.getGeneralDesc())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tests whether a short number matches a valid pattern in a region. Note that this doesn't verify
   * the number is actually in use, which is impossible to tell by just looking at the number
   * itself.
   *
   * @param shortNumber the short number to check as a string
   * @param regionDialingFrom the region from which the number is dialed
   * @return whether the short number matches a valid pattern
   */
  public boolean isValidShortNumberForRegion(String shortNumber, String regionDialingFrom) {
    PhoneMetadata phoneMetadata =
        MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
    if (phoneMetadata == null) {
      return false;
    }
    PhoneNumberDesc generalDesc = phoneMetadata.getGeneralDesc();
    if (!generalDesc.hasNationalNumberPattern() ||
        !phoneUtil.isNumberMatchingDesc(shortNumber, generalDesc)) {
      return false;
    }
    PhoneNumberDesc shortNumberDesc = phoneMetadata.getShortCode();
    if (!shortNumberDesc.hasNationalNumberPattern()) {
      logger.log(Level.WARNING, "No short code national number pattern found for region: " +
          regionDialingFrom);
      return false;
    }
    return phoneUtil.isNumberMatchingDesc(shortNumber, shortNumberDesc);
  }

  /**
   * Tests whether a short number matches a valid pattern. If a country calling code is shared by
   * multiple regions, this returns true if it's valid in any of them. Note that this doesn't verify
   * the number is actually in use, which is impossible to tell by just looking at the number
   * itself. See {@link #isValidShortNumberForRegion(String, RegionCode)} for details.
   *
   * @param number the short number for which we want to test the validity
   * @return whether the short number matches a valid pattern
   */
  public boolean isValidShortNumber(PhoneNumber number) {
    List<String> regionCodes = phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
    String shortNumber = phoneUtil.getNationalSignificantNumber(number);
    String regionCode = getRegionCodeForShortNumberFromRegionList(number, regionCodes);
    if (regionCodes.size() > 1 && regionCode != null) {
      // If a matching region had been found for the phone number from among two or more regions,
      // then we have already implicitly verified its validity for that region.
      return true;
    }
    return isValidShortNumberForRegion(shortNumber, regionCode);
  }

  /**
   * Gets the expected cost category of a short number when dialled from a region (however, nothing
   * is implied about its validity). If it is important that the number is valid, then its validity
   * must first be checked using {@link isValidShortNumberForRegion}. Note that emergency numbers
   * are always considered toll-free. Example usage:
   * <pre>{@code
   * ShortNumberInfo shortInfo = ShortNumberInfo.getInstance();
   * String shortNumber = "110";
   * String regionCode = "FR";
   * if (shortInfo.isValidShortNumberForRegion(shortNumber, regionCode)) {
   *   ShortNumberInfo.ShortNumberCost cost = shortInfo.getExpectedCostForRegion(shortNumber,
   *       regionCode);
   *   // Do something with the cost information here.
   * }}</pre>
   *
   * @param shortNumber the short number for which we want to know the expected cost category,
   *     as a string
   * @param regionDialingFrom the region from which the number is dialed
   * @return the expected cost category for that region of the short number. Returns UNKNOWN_COST if
   *     the number does not match a cost category. Note that an invalid number may match any cost
   *     category.
   */
  public ShortNumberCost getExpectedCostForRegion(String shortNumber, String regionDialingFrom) {
    // Note that regionDialingFrom may be null, in which case phoneMetadata will also be null.
    PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(
        regionDialingFrom);
    if (phoneMetadata == null) {
      return ShortNumberCost.UNKNOWN_COST;
    }

    // The cost categories are tested in order of decreasing expense, since if for some reason the
    // patterns overlap the most expensive matching cost category should be returned.
    if (phoneUtil.isNumberMatchingDesc(shortNumber, phoneMetadata.getPremiumRate())) {
      return ShortNumberCost.PREMIUM_RATE;
    }
    if (phoneUtil.isNumberMatchingDesc(shortNumber, phoneMetadata.getStandardRate())) {
      return ShortNumberCost.STANDARD_RATE;
    }
    if (phoneUtil.isNumberMatchingDesc(shortNumber, phoneMetadata.getTollFree())) {
      return ShortNumberCost.TOLL_FREE;
    }
    if (isEmergencyNumber(shortNumber, regionDialingFrom)) {
      // Emergency numbers are implicitly toll-free.
      return ShortNumberCost.TOLL_FREE;
    }
    return ShortNumberCost.UNKNOWN_COST;
  }

  /**
   * Gets the expected cost category of a short number (however, nothing is implied about its
   * validity). If the country calling code is unique to a region, this method behaves exactly the
   * same as {@link #getExpectedCostForRegion(String, RegionCode)}. However, if the country calling
   * code is shared by multiple regions, then it returns the highest cost in the sequence
   * PREMIUM_RATE, UNKNOWN_COST, STANDARD_RATE, TOLL_FREE. The reason for the position of
   * UNKNOWN_COST in this order is that if a number is UNKNOWN_COST in one region but STANDARD_RATE
   * or TOLL_FREE in another, its expected cost cannot be estimated as one of the latter since it
   * might be a PREMIUM_RATE number.
   *
   * For example, if a number is STANDARD_RATE in the US, but TOLL_FREE in Canada, the expected cost
   * returned by this method will be STANDARD_RATE, since the NANPA countries share the same country
   * calling code.
   *
   * Note: If the region from which the number is dialed is known, it is highly preferable to call
   * {@link #getExpectedCostForRegion(String, RegionCode)} instead.
   *
   * @param number the short number for which we want to know the expected cost category
   * @return the highest expected cost category of the short number in the region(s) with the given
   *     country calling code
   */
  public ShortNumberCost getExpectedCost(PhoneNumber number) {
    List<String> regionCodes = phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
    if (regionCodes.size() == 0) {
      return ShortNumberCost.UNKNOWN_COST;
    }
    String shortNumber = phoneUtil.getNationalSignificantNumber(number);
    if (regionCodes.size() == 1) {
      return getExpectedCostForRegion(shortNumber, regionCodes.get(0));
    }
    ShortNumberCost cost = ShortNumberCost.TOLL_FREE;
    for (String regionCode : regionCodes) {
      ShortNumberCost costForRegion = getExpectedCostForRegion(shortNumber, regionCode);
      switch (costForRegion) {
        case PREMIUM_RATE:
          return ShortNumberCost.PREMIUM_RATE;
        case UNKNOWN_COST:
          cost = ShortNumberCost.UNKNOWN_COST;
          break;
        case STANDARD_RATE:
          if (cost != ShortNumberCost.UNKNOWN_COST) {
            cost = ShortNumberCost.STANDARD_RATE;
          }
          break;
        case TOLL_FREE:
          // Do nothing.
          break;
        default:
          logger.log(Level.SEVERE, "Unrecognised cost for region: " + costForRegion);
      }
    }
    return cost;
  }

  // Helper method to get the region code for a given phone number, from a list of possible region
  // codes. If the list contains more than one region, the first region for which the number is
  // valid is returned.
  private String getRegionCodeForShortNumberFromRegionList(PhoneNumber number,
                                                           List<String> regionCodes) {
    if (regionCodes.size() == 0) {
      return null;
    } else if (regionCodes.size() == 1) {
      return regionCodes.get(0);
    }
    String nationalNumber = phoneUtil.getNationalSignificantNumber(number);
    for (String regionCode : regionCodes) {
      PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
      if (phoneMetadata != null &&
          phoneUtil.isNumberMatchingDesc(nationalNumber, phoneMetadata.getShortCode())) {
        // The number is valid for this region.
        return regionCode;
      }
    }
    return null;
  }

  /**
   * Convenience method to get a list of what regions the library has metadata for.
   */
  Set<String> getSupportedRegions() {
    return Collections.unmodifiableSet(MetadataManager.getShortNumberMetadataSupportedRegions());
  }

  /**
   * Gets a valid short number for the specified region.
   *
   * @param regionCode the region for which an example short number is needed
   * @return a valid short number for the specified region. Returns an empty string when the
   *     metadata does not contain such information.
   */
  // @VisibleForTesting
  String getExampleShortNumber(String regionCode) {
    PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
    if (phoneMetadata == null) {
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
   * @param regionCode the region for which an example short number is needed
   * @param cost the cost category of number that is needed
   * @return a valid short number for the specified region and cost category. Returns an empty
   *     string when the metadata does not contain such information, or the cost is UNKNOWN_COST.
   */
  // @VisibleForTesting
  String getExampleShortNumberForCost(String regionCode, ShortNumberCost cost) {
    PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
    if (phoneMetadata == null) {
      return "";
    }
    PhoneNumberDesc desc = null;
    switch (cost) {
      case TOLL_FREE:
        desc = phoneMetadata.getTollFree();
        break;
      case STANDARD_RATE:
        desc = phoneMetadata.getStandardRate();
        break;
      case PREMIUM_RATE:
        desc = phoneMetadata.getPremiumRate();
        break;
      default:
        // UNKNOWN_COST numbers are computed by the process of elimination from the other cost
        // categories.
    }
    if (desc != null && desc.hasExampleNumber()) {
      return desc.getExampleNumber();
    }
    return "";
  }

  /**
   * Returns true if the number might be used to connect to an emergency service in the given
   * region.
   *
   * This method takes into account cases where the number might contain formatting, or might have
   * additional digits appended (when it is okay to do that in the region specified).
   *
   * @param number the phone number to test
   * @param regionCode the region where the phone number is being dialed
   * @return whether the number might be used to connect to an emergency service in the given region
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
   * @param number the phone number to test
   * @param regionCode the region where the phone number is being dialed
   * @return whether the number exactly matches an emergency services number in the given region
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
    PhoneMetadata metadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
    if (metadata == null || !metadata.hasEmergency()) {
      return false;
    }
    Pattern emergencyNumberPattern =
        Pattern.compile(metadata.getEmergency().getNationalNumberPattern());
    String normalizedNumber = PhoneNumberUtil.normalizeDigitsOnly(number);
    return (!allowPrefixMatch || REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.contains(regionCode))
        ? emergencyNumberPattern.matcher(normalizedNumber).matches()
        : emergencyNumberPattern.matcher(normalizedNumber).lookingAt();
  }

  /**
   * Given a valid short number, determines whether it is carrier-specific (however, nothing is
   * implied about its validity). If it is important that the number is valid, then its validity
   * must first be checked using {@link #isValidShortNumber} or
   * {@link #isValidShortNumberForRegion}.
   *
   * @param number the valid short number to check
   * @return whether the short number is carrier-specific (assuming the input was a valid short
   *     number).
   */
  public boolean isCarrierSpecific(PhoneNumber number) {
    List<String> regionCodes = phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
    String regionCode = getRegionCodeForShortNumberFromRegionList(number, regionCodes);
    String nationalNumber = phoneUtil.getNationalSignificantNumber(number);
    PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
    return (phoneMetadata != null) &&
        (phoneUtil.isNumberMatchingDesc(nationalNumber, phoneMetadata.getCarrierSpecific()));
  }
}
