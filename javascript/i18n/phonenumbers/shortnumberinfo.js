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
