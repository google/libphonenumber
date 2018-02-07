/**
 * @license
 * Copyright (C) 2018 The Libphonenumber Authors.
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

/**
 * @fileoverview  Utility for international phone numbers.
 * Functionality includes formatting, parsing and validation.
 * (based on the java implementation).
 *
 * NOTE: A lot of methods in this class require Region Code strings. These must
 * be provided using CLDR two-letter region-code format. These should be in
 * upper-case. The list of the codes can be found here:
 * http://www.unicode.org/cldr/charts/30/supplemental/territory_information.html
 */

goog.provide('i18n.phonenumbers.ShortNumberInfo');

goog.require('goog.array');
goog.require('goog.object');
goog.require('goog.proto2.PbLiteSerializer');
goog.require('goog.string');
goog.require('goog.string.StringBuffer');
goog.require('i18n.phonenumbers.PhoneMetadata');
goog.require('i18n.phonenumbers.PhoneNumber');
goog.require('i18n.phonenumbers.PhoneNumberDesc');
goog.require('i18n.phonenumbers.PhoneNumberUtil');
goog.require('i18n.phonenumbers.shortnumbermetadata');
goog.require('i18n.phonenumbers.metadata');



/**
 * @constructor
 * @private
 */
i18n.phonenumbers.ShortNumberInfo = function() {
  /**
   * A reference to the PhoneNumberUtil
   *
   * @const
   * @type {!PhoneNumberUtil}
   */
  this.phoneNumberUtil = i18n.phonenumbers.PhoneNumberUtil.getInstance();
  /**
   * A mapping from region code to the short-number metadata for that region.
   * @type {Object.<string, i18n.phonenumbers.PhoneMetadata>}
   */
  this.regionToMetadataMap = {};
};
goog.addSingletonGetter(i18n.phonenumbers.ShortNumberInfo);


/**
 * In these countries, if extra digits are added to an emergency number, it no
 * longer connects to the emergency service.
 * @const
 * @type {!Array<string>}
 * @private
 */
i18n.phonenumbers.ShortNumberInfo.
    REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT_ = [
      'BR',
      'CL',
      'NI'
    ];


/**
 * @enum {number} Cost categories of short numbers.
 */
i18n.phonenumbers.ShortNumberInfo.ShortNumberCost = {
  TOLL_FREE: 0,
  STANDARD_RATE: 1,
  PREMIUM_RATE: 2,
  UNKNOWN_COST: 3
};

i18n.phonenumbers.ShortNumberInfo.prototype.getRegionCodesForCountryCode = function(countryCallingCode) {
  var regionCodes = i18n.phonenumbers.metadata.countryCodeToRegionCodeMap[countryCallingCode];
  return regionCodes ? regionCodes : [];
};

i18n.phonenumbers.ShortNumberInfo.prototype.regionDialingFromMatchesNumber_ = function(number, regionDialingFrom) {
  var regionCodes = this.getRegionCodesForCountryCode(number.getCountryCode());
  return goog.array.contains(regionCodes, regionDialingFrom);
};

i18n.phonenumbers.ShortNumberInfo.prototype.isPossibleShortNumberForRegion = function(number, regionDialingFrom) {
  if (!this.regionDialingFromMatchesNumber_(number, regionDialingFrom)) {
    return false;
  }
  var phoneMetadata = this.getMetadataForRegion_(regionDialingFrom);
  if (!phoneMetadata) {
    return false;
  }
  var numberLength = this.getNationalSignificantNumber_(number).length;
  return goog.array.contains(phoneMetadata.getGeneralDesc().possibleLengthArray(), numberLength);
};

i18n.phonenumbers.ShortNumberInfo.prototype.isPossibleShortNumber = function(number) {
  var regionCodes = this.getRegionCodesForCountryCode(number.getCountryCode());
  var shortNumberLength = this.getNationalSignificantNumber_(number).length;
  for (var i = 0; i < regionCodes.length; i++) {
    var region = regionCodes[i];
    var phoneMetadata = this.getMetadataForRegion_(region);
    if (!phoneMetadata) {
      continue;
    }
    if (goog.array.contains(phoneMetadata.getGeneralDesc().possibleLengthArray(), shortNumberLength)) {
      return true;
    }
  }
  return false;
};

i18n.phonenumbers.ShortNumberInfo.prototype.isValidShortNumberForRegion = function(number, regionDialingFrom) {
  if (!this.regionDialingFromMatchesNumber_(number, regionDialingFrom)) {
    return false;
  }
  var phoneMetadata = this.getMetadataForRegion_(regionDialingFrom);
  if (!phoneMetadata) {
    return false;
  }
  var shortNumber = this.getNationalSignificantNumber_(number);
  var generalDesc = phoneMetadata.getGeneralDesc();
  if (!this.matchesPossibleNumberAndNationalNumber_(shortNumber, generalDesc)) {
    return false;
  }
  var shortNumberDesc = phoneMetadata.getShortCode();
  return this.matchesPossibleNumberAndNationalNumber_(shortNumber, shortNumberDesc);
};

i18n.phonenumbers.ShortNumberInfo.prototype.isValidShortNumber = function(number) {
  var regionCodes = this.getRegionCodesForCountryCode(number.getCountryCode());
  var regionCode = this.getRegionCodeForShortNumberFromRegionList_(number, regionCodes);
  if (regionCodes.length > 1 && regionCode != null) {
    // If a matching region had been found for the phone number from among two
    // or more regions, then we have already implicitly verified its validity
    // for that region.
    return true;
  }
  return this.isValidShortNumberForRegion(number, regionCode);
};

i18n.phonenumbers.ShortNumberInfo.prototype.getExpectedCostForRegion = function(number, regionDialingFrom) {
  var ShortNumberCost = i18n.phonenumbers.ShortNumberInfo.ShortNumberCost;
  if (!this.regionDialingFromMatchesNumber_(number, regionDialingFrom)) {
    return ShortNumberCost.UNKNOWN_COST;
  }
  var phoneMetadata = this.getMetadataForRegion_(regionDialingFrom);
  if (!phoneMetadata) {
    return ShortNumberCost.UNKNOWN_COST;
  }
  var shortNumber = this.getNationalSignificantNumber_(number);

  if (!goog.array.contains(phoneMetadata.getGeneralDesc().possibleLengthArray(), shortNumber.length)) {
    return ShortNumberCost.UNKNOWN_COST;
  }
  if (this.matchesPossibleNumberAndNationalNumber_(shortNumber, phoneMetadata.getPremiumRate())) {
    return ShortNumberCost.PREMIUM_RATE;
  }
  if (this.matchesPossibleNumberAndNationalNumber_(shortNumber, phoneMetadata.getStandardRate())) {
    return ShortNumberCost.STANDARD_RATE;
  }
  if (this.matchesPossibleNumberAndNationalNumber_(shortNumber, phoneMetadata.getTollFree())) {
    return ShortNumberCost.TOLL_FREE;
  }
  if (this.isEmergencyNumber(shortNumber, regionDialingFrom)) {
    // Emergency numbers are implicitly toll-free
    return ShortNumberCost.TOLL_FREE;
  }
  return ShortNumberCost.UNKNOWN_COST;
};

i18n.phonenumbers.ShortNumberInfo.prototype.getExpectedCost = function(number) {
  var ShortNumberCost = i18n.phonenumbers.ShortNumberInfo.ShortNumberCost;
  var regionCodes = this.getRegionCodesForCountryCode(number.getCountryCode());
  if (regionCodes.length === 0) {
    return ShortNumberCost.UNKNOWN_COST;
  }
  if (regionCodes.length === 1) {
    return this.getExpectedCostForRegion(number, regionCodes[0]);
  }
  var cost = ShortNumberCost.TOLL_FREE;
  for (var i=0; i<regionCodes.length; i++) {
    var regionCode = regionCodes[i];
    var costForRegion = this.getExpectedCostForRegion(number, regionCode);
    switch (costForRegion) {
      case ShortNumberCost.PREMIUM_RATE:
        return ShortNumberCost.PREMIUM_RATE;
      case ShortNumberCost.UNKNOWN_COST:
        cost = ShortNumberCost.UNKNOWN_COST;
        break;
      case ShortNumberCost.STANDARD_RATE:
        if (cost !== ShortNumberCost.UNKNOWN_COST) {
          cost = ShortNumberCost.STANDARD_RATE;
        }
        break;
      case ShortNumberCost.TOLL_FREE:
        // Do nothing.
        break;
      default:
        throw new Error("Unrecognized cost for region: " + costForRegion);
    }
  }
  return cost;
};

i18n.phonenumbers.ShortNumberInfo.prototype.getRegionCodeForShortNumberFromRegionList_ = function(number, regionCodes) {
  if (regionCodes.length === 0) {
    return null;
  } else if (regionCodes.length === 1) {
    return regionCodes[0];
  }
  var nationalNumber = this.getNationalSignificantNumber_(number);
  for (var i = 0; i < regionCodes.length; i++) {
    var regionCode = regionCodes[i];
    var phoneMetadata = this.getMetadataForRegion_(regionCode);
    if (phoneMetadata && this.matchesPossibleNumberAndNationalNumber_(nationalNumber, phoneMetadata.getShortCode())) {
      return regionCode;
    }
  }
  return null;
};

i18n.phonenumbers.ShortNumberInfo.prototype.getSupportedRegions = function() {
  return goog.array.filter(Object.keys(i18n.phonenumbers.shortnumbermetadata.countryToMetadata), function(regionCode) {
    return isNaN(regionCode);
  });
};

i18n.phonenumbers.ShortNumberInfo.prototype.getExampleShortNumber = function(regionCode) {
  var phoneMetadata = this.getMetadataForRegion_(regionCode);
  if (!phoneMetadata) {
    return '';
  }
  var desc = phoneMetadata.getShortCode();
  if (desc.hasExampleNumber()) {
    return desc.getExampleNumber();
  }
  return "";
};

i18n.phonenumbers.ShortNumberInfo.prototype.getExampleShortNumberForCost = function(regionCode, cost) {
  var phoneMetadata = this.getMetadataForRegion_(regionCode);
  if (!phoneMetadata) {
    return "";
  }
  var ShortNumberCost = i18n.phonenumbers.ShortNumberInfo.ShortNumberCost;
  var desc = null;
  switch (cost) {
    case ShortNumberCost.TOLL_FREE:
      desc = phoneMetadata.getTollFree();
      break;
    case ShortNumberCost.STANDARD_RATE:
      desc = phoneMetadata.getStandardRate();
      break;
    case ShortNumberCost.PREMIUM_RATE:
      desc = phoneMetadata.getPremiumRate();
      break;
    default:
      // UNKNOWN_COST numbers are computed by the process of elimination from
      // the other cost categories.
  }
  if (desc && desc.hasExampleNumber()) {
    return desc.getExampleNumber();
  }
  return "";
};

/**
   * Returns true if the given number, exactly as dialed, might be used to
   * connect to an emergency service in the given region.
   * <p>
   * This method accepts a string, rather than a PhoneNumber, because it needs
   * to distinguish cases such as "+1 911" and "911", where the former may not
   * connect to an emergency service in all cases but the latter would. This
   * method takes into account cases where the number might contain formatting,
   * or might have additional digits appended (when it is okay to do that in
   * the specified region).
   *
   * @param {number} number the phone number to test
   * @param {string} regionCode the region where the phone number is being
   *     dialed
   * @return {boolean} whether the number might be used to connect to an
   *     emergency service in the given region
   */
i18n.phonenumbers.ShortNumberInfo.prototype.connectsToEmergencyNumber =
    function(number, regionCode) {
  return this.matchesEmergencyNumberHelper_(number, regionCode,
      true /* allows prefix match */);
};


/**
   * Returns true if the given number exactly matches an emergency service
   * number in the given region.
   * <p>
   * This method takes into account cases where the number might contain
   * formatting, but doesn't allow additional digits to be appended. Note that
   * {@code isEmergencyNumber(number, region)} implies
   * {@code connectsToEmergencyNumber(number, region)}.
   *
   * @param {number} number the phone number to test
   * @param {string} regionCode the region where the phone number is being
   *     dialed
   * @return {boolean} whether the number exactly matches an emergency services
   *     number in the given region.
   */
i18n.phonenumbers.ShortNumberInfo.prototype.isEmergencyNumber =
    function(number, regionCode) {
  return this.matchesEmergencyNumberHelper_(number, regionCode,
      false /* doesn't allow prefix match */);
};


/**
 * @param {string} regionCode The region code to get metadata for
 * @return {?i18n.phonenumbers.PhoneMetadata} The region code's metadata, or
 *     null if it is not available or the region code is invalid.
 * @private
 */
i18n.phonenumbers.ShortNumberInfo.prototype.getMetadataForRegion_ =
    function(regionCode) {
  if (regionCode == null) {
    return null;
  }
  regionCode = regionCode.toUpperCase();
  var metadata = this.regionToMetadataMap[regionCode];
  if (metadata == null) {
    /** @type {i18n.phonenumbers.PhoneMetadata} */
    var serializer = new goog.proto2.PbLiteSerializer();
    var metadataSerialized =
        i18n.phonenumbers.shortnumbermetadata.countryToMetadata[regionCode];
    if (metadataSerialized == null) {
      return null;
    }
    metadata = /** @type {i18n.phonenumbers.PhoneMetadata} */ (
        serializer.deserialize(i18n.phonenumbers.PhoneMetadata.getDescriptor(),
        metadataSerialized));
    this.regionToMetadataMap[regionCode] = metadata;
  }
  return metadata;
};


/**
 * @param {string} number the number to match against
 * @param {string} regionCode the region code to check against
 * @param {boolean} allowPrefixMatch whether to allow prefix matching
 * @return {boolean} True iff the number matches an emergency number for that
 *     particular region.
 * @private
 */
i18n.phonenumbers.ShortNumberInfo.prototype.matchesEmergencyNumberHelper_ =
    function(number, regionCode, allowPrefixMatch) {
  var possibleNumber = i18n.phonenumbers.PhoneNumberUtil
      .extractPossibleNumber(number);
  if (i18n.phonenumbers.PhoneNumberUtil.LEADING_PLUS_CHARS_PATTERN_
      .test(possibleNumber)) {
    return false;
  }
  var metadata = this.getMetadataForRegion_(regionCode);
  if (metadata == null || !metadata.hasEmergency()) {
    return false;
  }

  var normalizedNumber = i18n.phonenumbers.PhoneNumberUtil
      .normalizeDigitsOnly(possibleNumber);
  var allowPrefixMatchForRegion = allowPrefixMatch && !goog.array.contains(
      i18n.phonenumbers.ShortNumberInfo.
          REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT_,
      regionCode);
  var emergencyNumberPattern = metadata.getEmergency()
      .getNationalNumberPatternOrDefault();
  var result = i18n.phonenumbers.PhoneNumberUtil.matchesEntirely(
      emergencyNumberPattern, normalizedNumber);
  return result ||
         (allowPrefixMatchForRegion &&
          i18n.phonenumbers.PhoneNumberUtil
              .matchesPrefix(emergencyNumberPattern, normalizedNumber));
};

i18n.phonenumbers.ShortNumberInfo.prototype.isCarrierSpecific = function(number) {
  var regionCodes = this.getRegionCodesForCountryCode(number.getCountryCode());
  var regionCode = this.getRegionCodeForShortNumberFromRegionList_(number, regionCodes);
  var nationalNumber = this.getNationalSignificantNumber_(number);
  var phoneMetadata = this.getMetadataForRegion_(regionCode);
  return !!phoneMetadata && this.matchesPossibleNumberAndNationalNumber_(nationalNumber, phoneMetadata.getCarrierSpecific());
};

i18n.phonenumbers.ShortNumberInfo.prototype.isCarrierSpecificForRegion = function(number, regionDialingFrom) {
  if (!this.regionDialingFromMatchesNumber_(number, regionDialingFrom)) {
    return false;
  }
  var nationalNumber = this.getNationalSignificantNumber_(number);
  var phoneMetadata = this.getMetadataForRegion_(regionDialingFrom);
  return !!phoneMetadata && this.matchesPossibleNumberAndNationalNumber_(nationalNumber, phoneMetadata.getCarrierSpecific());
};

i18n.phonenumbers.ShortNumberInfo.prototype.isSmsServiceForRegion = function(number, regionDialingFrom) {
  if (!this.regionDialingFromMatchesNumber_(number, regionDialingFrom)) {
    return false;
  }
  var phoneMetadata = this.getMetadataForRegion_(regionDialingFrom);
  return !!phoneMetadata && this.matchesPossibleNumberAndNationalNumber_(this.getNationalSignificantNumber_(number), phoneMetadata.getSmsServices());
};

/**
 * Gets the national significant number of a phone number. Note a national
 * significant number doesn't contain a national prefix or any formatting.
 * <p>
 * This is a temporary duplicate of the {@code getNationalSignificantNumber} method from
 * {@code PhoneNumberUtil}. Ultimately a canonical static version should exist in a separate
 * utility class (to prevent {@code ShortNumberInfo} needing to depend on PhoneNumberUtil).
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number for which the
 *     national significant number is needed.
 * @return {string} the national significant number of the PhoneNumber object
 *     passed in.
 */
i18n.phonenumbers.ShortNumberInfo.prototype.getNationalSignificantNumber_ = function(number) {
  if (!number.hasNationalNumber()) {
    return '';
  }
  /** @type {string} */
  var nationalNumber = '' + number.getNationalNumber();
  // If leading zero(s) have been set, we prefix this now. Note that a single
  // leading zero is not the same as a national prefix; leading zeros should be
  // dialled no matter whether you are dialling from within or outside the
  // country, national prefixes are added when formatting nationally if
  // applicable.
  if (number.hasItalianLeadingZero() && number.getItalianLeadingZero() &&
      number.getNumberOfLeadingZerosOrDefault() > 0) {
    return Array(number.getNumberOfLeadingZerosOrDefault() + 1).join('0') +
        nationalNumber;
  }
  return nationalNumber;
};

i18n.phonenumbers.ShortNumberInfo.prototype.matchesPossibleNumberAndNationalNumber_ = function(number, numberDesc) {
  if (numberDesc.possibleLengthArray().length > 0 && !goog.array.contains(numberDesc.possibleLengthArray(), number.length)) {
    return false;
  }
  return i18n.phonenumbers.PhoneNumberUtil.matchesEntirely(numberDesc.getNationalNumberPatternOrDefault(), number);
};
