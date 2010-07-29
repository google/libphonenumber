// Copyright (C) 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview  A formatter which formats phone numbers as they are entered.
 * (based on the java implementation).
 *
 * An AsYouTypeFormatter could be created by new AsYouTypeFormatter(). After
 * that digits could be added by invoking the inputDigit method on the formatter
 * instance, and the partially formatted phone number will be returned each time
 * a digit is added. The clear method should be invoked before a new number
 * needs to be formatted.
 *
 * See testAsYouTypeFormatterUS(), testAsYouTestFormatterGB() and
 * testAsYouTypeFormatterDE() in asyoutypeformatter_test.js for more details
 * on how the formatter is to be used.
 *
 * @author Nikolaos Trogkanis
 */

goog.provide('i18n.phonenumbers.AsYouTypeFormatter');

goog.require('goog.string.StringBuffer');
goog.require('i18n.phonenumbers.NumberFormat');
goog.require('i18n.phonenumbers.PhoneMetadata');
goog.require('i18n.phonenumbers.PhoneMetadataCollection');
goog.require('i18n.phonenumbers.PhoneNumber');
goog.require('i18n.phonenumbers.PhoneNumber.CountryCodeSource');
goog.require('i18n.phonenumbers.PhoneNumberDesc');
goog.require('i18n.phonenumbers.PhoneNumberUtil');
goog.require('i18n.phonenumbers.metadata');

/**
 * Constructs a light-weight formatter which does no formatting, but outputs
 * exactly what is fed into the inputDigit method.
 *
 * @param {string} regionCode the country/region where the phone number is being
 *     entered.
 * @constructor
 */
i18n.phonenumbers.AsYouTypeFormatter = function(regionCode) {
  /**
   * @type {boolean}
   * @private
   */
  this.ableToFormat_ = true;
  /**
   * @type {boolean}
   * @private
   */
  this.isInternationalFormatting_ = false;
  /**
   * @type {i18n.phonenumbers.PhoneNumberUtil}
   * @private
   */
  this.phoneUtil_ = i18n.phonenumbers.PhoneNumberUtil.getInstance();
  // The digits that have not been entered yet will be represented by a \u2008,
  // the punctuation space.
  /**
   * @type {string}
   * @private
   */
  this.digitPlaceholder_ = '\u2008';
  /**
   * @type {RegExp}
   * @private
   */
  this.digitPattern_ = new RegExp(this.digitPlaceholder_);
  /**
   * @type {number}
   * @private
   */
  this.lastMatchPosition_ = 0;
  /**
   * The position of a digit upon which inputDigitAndRememberPosition is most
   * recently invoked, as found in the current output.
   * @type {number}
   * @private
   */
  this.positionRemembered_ = 0;
  /**
   * The position of a digit upon which inputDigitAndRememberPosition is most
   * recently invoked, as found in the original sequence of characters the user
   * entered.
   * @type {number}
   * @private
   */
  this.originalPosition_ = 0;
  /**
   * A pattern that is used to match character classes in regular expressions.
   * An example of a character class is [1-4].
   * @type {RegExp}
   * @private
   */
  this.CHARACTER_CLASS_PATTERN_ = /\[([^\[\]])*\]/g;
  /**
   * Any digit in a regular expression that actually denotes a digit. For
   * example, in the regular expression 80[0-2]\d{6,10}, the first 2 digits
   * (8 and 0) are standalone digits, but the rest are not.
   * Two look-aheads are needed because the number following \\d could be a
   * two-digit number, since the phone number can be as long as 15 digits.
   * @type {RegExp}
   * @private
   */
  this.STANDALONE_DIGIT_PATTERN_ = /\d(?=[^,}][^,}])/g;
  /**
   * @type {!goog.string.StringBuffer}
   * @private
   */
  this.accruedInput_ = new goog.string.StringBuffer();
  /**
   * @type {!goog.string.StringBuffer}
   * @private
   */
  this.accruedInputWithoutFormatting_ = new goog.string.StringBuffer();
  /**
   * @type {!goog.string.StringBuffer}
   * @private
   */
  this.currentOutput_ = new goog.string.StringBuffer();
  /**
   * @type {!goog.string.StringBuffer}
   * @private
   */
  this.prefixBeforeNationalNumber_ = new goog.string.StringBuffer();
  /**
   * @type {!goog.string.StringBuffer}
   * @private
   */
  this.nationalNumber_ = new goog.string.StringBuffer();
  /**
   *  @type {string}
   * @private
   */
  this.defaultCountry_ = regionCode;
  this.initializeCountrySpecificInfo_(this.defaultCountry_);
  /**
   * @type {i18n.phonenumbers.PhoneMetadata}
   * @private
   */
  this.defaultMetaData_ = this.currentMetaData_;
};

/**
 * @param {string} regionCode
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.initializeCountrySpecificInfo_ =
  function(regionCode) {

  /** @type {i18n.phonenumbers.PhoneMetadata} */
  this.currentMetaData_ = this.phoneUtil_.getMetadataForRegion(regionCode);
  /** @type {RegExp} */
  this.nationalPrefixForParsing_ = new RegExp('^(' + this.currentMetaData_
      .getNationalPrefixForParsing() + ')');
  /** @type {RegExp} */
  this.internationalPrefix_ = new RegExp('^(' + '\\+|' +
      this.currentMetaData_.getInternationalPrefix() + ')');
};

/**
 * @param {string} leadingFourDigitsOfNationalNumber
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.chooseFormatAndCreateTemplate_ =
  function(leadingFourDigitsOfNationalNumber) {

  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var formatList = this.getAvailableFormats_(leadingFourDigitsOfNationalNumber);
  if (formatList.length < 1) {
    this.ableToFormat_ = false;
  } else {
    // When there are multiple available formats, the formatter uses the first
    // format.
    /** @type {i18n.phonenumbers.NumberFormat} */
    var format = formatList[0];
    if (!this.createFormattingTemplate_(format)) {
      this.ableToFormat_ = false;
    } else {
      this.currentOutput_ =
          new goog.string.StringBuffer(this.formattingTemplate_);
    }
  }
};

/**
 * @param {string} leadingFourDigits
 * @return {Array.<i18n.phonenumbers.NumberFormat>}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.getAvailableFormats_ =
  function(leadingFourDigits) {

  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var matchedList = [];
  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var formatList = (this.isInternationalFormatting_ && this.currentMetaData_
      .intlNumberFormatCount() > 0) ? this.currentMetaData_
      .intlNumberFormatArray() : this.currentMetaData_.numberFormatArray();
  /** @type {number} */
  var formatListLength = formatList.length;
  for (var i = 0; i < formatListLength; ++i) {
    /** @type {i18n.phonenumbers.NumberFormat} */
    var format = formatList[i];
    if (format.hasLeadingDigits()) {
      /** @type {RegExp} */
      var leadingDigitsPattern =
          new RegExp('^(' + format.getLeadingDigits() + ')');
      if (leadingDigitsPattern.test(leadingFourDigits)) {
        matchedList.push(format);
      }
    } else {
      matchedList.push(format);
    }
  }
  return matchedList;
};

/**
 * @param {i18n.phonenumbers.NumberFormat} format
 * @return {boolean}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.createFormattingTemplate_ =
  function(format) {

  /** @type {string} */
  var numberFormat = format.getFormatOrDefault();
  /** @type {string} */
  var numberPattern = format.getPatternOrDefault();

  // The formatter doesn't format numbers when numberPattern contains '|', e.g.
  // (20|3)\d{4}. In those cases we quickly return.
  if (numberPattern.indexOf('|') != -1) {
    return false;
  }

  // Replace anything in the form of [..] with \d
  numberPattern = numberPattern.replace(this.CHARACTER_CLASS_PATTERN_, '\\d');

  // Replace any standalone digit (not the one in d{}) with \d
  numberPattern = numberPattern.replace(this.STANDALONE_DIGIT_PATTERN_, '\\d');

  this.formattingTemplate_ = this.getFormattingTemplate_(numberPattern,
      numberFormat);
  return true;
};

/**
 * Gets a formatting template which could be used to efficiently format a
 * partial number where digits are added one by one.
 *
 * @param {string} numberPattern
 * @param {string} numberFormat
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.getFormattingTemplate_ =
  function(numberPattern, numberFormat) {

  // Creates a phone number consisting only of the digit 9 that matches the
  // numberPattern by applying the pattern to the longestPhoneNumber string.
  /** @type {string} */
  var longestPhoneNumber = '999999999999999';
  /** @type {Array.<string>} */
  var m = longestPhoneNumber.match(numberPattern);
  // this match will always succeed
  /** @type {string} */
  var aPhoneNumber = m[0];
  // Formats the number according to numberFormat
  /** @type {string} */
  var template = aPhoneNumber.replace(new RegExp(numberPattern, 'g'),
                                      numberFormat);
  // Replaces each digit with character digitPlaceholder
  template = template.replace(new RegExp('9', 'g'), this.digitPlaceholder_);
  return template;
};

/**
 * Clears the internal state of the formatter, so it could be reused.
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.clear = function() {
  this.accruedInput_.clear();
  this.accruedInputWithoutFormatting_.clear();
  this.currentOutput_.clear();
  this.lastMatchPosition_ = 0;
  this.prefixBeforeNationalNumber_.clear();
  this.nationalNumber_.clear();
  this.ableToFormat_ = true;
  this.positionRemembered_ = 0;
  this.originalPosition_ = 0;
  this.isInternationalFormatting_ = false;
  if (this.currentMetaData_ != this.defaultMetaData_) {
    this.initializeCountrySpecificInfo_(this.defaultCountry_);
  }
};

/**
 * Formats a phone number on-the-fly as each digit is entered.
 *
 * @param {string} nextChar the most recently entered digit of a phone number.
 *     Formatting characters are allowed, but they are removed from the result.
 * @return {string} the partially formatted phone number.
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.inputDigit = function(nextChar) {
  return this.inputDigitWithOptionToRememberPosition_(nextChar, false);
};

/**
 * Same as inputDigit, but remembers the position where nextChar is inserted, so
 * that it could be retrieved later by using getRememberedPosition(). The
 * remembered position will be automatically adjusted if additional formatting
 * characters are later inserted/removed in front of nextChar.
 *
 * @param {string} nextChar
 * @return {string}
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.inputDigitAndRememberPosition =
  function(nextChar) {

  return this.inputDigitWithOptionToRememberPosition_(nextChar, true);
};

/**
 * @param {string} nextChar
 * @param {boolean} rememberPosition
 * @return {string}
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
    inputDigitWithOptionToRememberPosition_ = function(nextChar,
                                                       rememberPosition) {

  this.accruedInput_.append(nextChar);
  if (rememberPosition) {
    this.positionRemembered_ = this.accruedInput_.getLength();
    this.originalPosition_ = this.positionRemembered_;
  }
  // We do formatting on-the-fly only when each character entered is either a
  // plus sign or a digit.
  if (!i18n.phonenumbers.PhoneNumberUtil.VALID_START_CHAR_PATTERN
      .test(nextChar)) {
    this.ableToFormat_ = false;
  }
  if (!this.ableToFormat_) {
    this.resetPositionOnFailureToFormat_();
    return this.accruedInput_.toString();
  }

  nextChar = this.normalizeAndAccrueDigitsAndPlusSign_(nextChar);

  // We start to attempt to format only when at least 6 digits (the plus sign is
  // counted as a digit as well for this purpose) have been entered.
  switch (this.accruedInputWithoutFormatting_.getLength()) {
  case 0: // this is the case where the first few inputs are neither digits nor
          // the plus sign.
  case 1:
  case 2:
  case 3:
  case 4:
  case 5:
    return this.accruedInput_.toString();
  case 6:
    if (!this.extractIddAndValidCountryCode_()) {
      this.ableToFormat_ = false;
      return this.accruedInput_.toString();
    }
    this.removeNationalPrefixFromNationalNumber_();
    return this.attemptToChooseFormattingPattern_(rememberPosition);
  default:
    if (this.nationalNumber_.getLength() > 4) {
      // The formatting pattern is already chosen.
      /** @type {string} */
      var temp = this.inputDigitHelper_(nextChar, rememberPosition);
      return this.ableToFormat_ ?
          this.prefixBeforeNationalNumber_.toString() + temp : temp;
    } else {
      return this.attemptToChooseFormattingPattern_(rememberPosition);
    }
  }
};

/**
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.resetPositionOnFailureToFormat_ =
  function() {

  if (this.positionRemembered_ > 0) {
    this.positionRemembered_ = this.originalPosition_;
    this.currentOutput_.clear();
  }
};

/**
 * Returns the current position in the partially formatted phone number of the
 * character which was previously passed in as the parameter of
 * inputDigitAndRememberPosition().
 *
 * @return {number}
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.getRememberedPosition =
  function() {

  return this.positionRemembered_;
};

/**
 * Attempts to set the formatting template and returns a string which contains
 * the formatted version of the digits entered so far.
 *
 * @param {boolean} rememberPosition
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
    attemptToChooseFormattingPattern_ = function(rememberPosition) {

  /** @type {string} */
  var nationalNumber = this.nationalNumber_.toString();
  /** @type {number} */
  var nationalNumberLength = nationalNumber.length;
  // We start to attempt to format only when as least 4 digits of national
  // number (excluding national prefix) have been entered.
  if (nationalNumberLength >= 4) {
    this.chooseFormatAndCreateTemplate_(nationalNumber.substring(0, 4));
    return this.inputAccruedNationalNumber_(rememberPosition);
  } else {
    if (rememberPosition) {
      this.positionRemembered_ =
          this.prefixBeforeNationalNumber_.length() + nationalNumberLength;
    }
    return this.prefixBeforeNationalNumber_.toString() +
        this.nationalNumber_.toString();
  }
};

/**
 * Invokes inputDigitHelper on each digit of the national number accrued, and
 * returns a formatted string in the end.
 *
 * @param {boolean} rememberPosition
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.inputAccruedNationalNumber_ =
  function(rememberPosition) {

  /** @type {number} */
  var lengthOfNationalNumber = this.nationalNumber_.getLength();
  if (lengthOfNationalNumber > 0) {
    // The positionRemembered should be only adjusted once in the loop that
    // follows.
    /** @type {boolean} */
    var positionAlreadyAdjusted = false;
    /** @type {string} */
    var tempNationalNumber = '';
    for (var i = 0; i < lengthOfNationalNumber; i++) {
      tempNationalNumber =
          this.inputDigitHelper_(this.nationalNumber_.toString().charAt(i),
                                 rememberPosition);
      if (!positionAlreadyAdjusted &&
          this.positionRemembered_ -
              this.prefixBeforeNationalNumber_.getLength() == i + 1) {
        this.positionRemembered_ =
            this.prefixBeforeNationalNumber_.getLength() +
            tempNationalNumber.length;
        positionAlreadyAdjusted = true;
      }
    }
    return this.ableToFormat_ ?
        this.prefixBeforeNationalNumber_.toString() + tempNationalNumber :
        tempNationalNumber;
  } else {
    if (rememberPosition) {
      this.positionRemembered_ = this.prefixBeforeNationalNumber_.length();
    }
    return this.prefixBeforeNationalNumber_.toString();
  }
};

/**
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
      removeNationalPrefixFromNationalNumber_ = function() {

  /** @type {string} */
  var nationalNumber = this.nationalNumber_.toString();
  /** @type {number} */
  var startOfNationalNumber = 0;
  if (this.currentMetaData_.getCountryCode() == 1 &&
      nationalNumber.charAt(0) == '1') {
    startOfNationalNumber = 1;
    this.prefixBeforeNationalNumber_.append('1 ');
    // Since a space is inserted after the national prefix in this case, we
    // increase the remembered position by 1 for anything that is after the
    // national prefix.
    if (this.positionRemembered_ >
        this.prefixBeforeNationalNumber_.getLength() - 1) {
      this.positionRemembered_++;
    }
  } else if (this.currentMetaData_.hasNationalPrefix()) {
    /** @type {Array.<string>} */
    var m = nationalNumber.match(this.nationalPrefixForParsing_);
    if (m != null && m[0] != null && m[0].length > 0) {
      startOfNationalNumber = m[0].length;
      this.prefixBeforeNationalNumber_.append(nationalNumber.substring(0,
          startOfNationalNumber));
    }
  }
  this.nationalNumber_.clear();
  this.nationalNumber_.append(nationalNumber.substring(startOfNationalNumber));
};

/**
 * Extracts IDD, plus sign and country code to prefixBeforeNationalNumber when
 * they are available, and places the remaining input into nationalNumber.
 *
 * @return {boolean} false when accruedInputWithoutFormatting begins with the
 *     plus sign or valid IDD for defaultCountry, but the sequence of digits
 *     after that does not form a valid country code. It returns true for all
 *     other cases.
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.extractIddAndValidCountryCode_ =
  function() {

  /** @type {string} */
  var accruedInputWithoutFormatting =
      this.accruedInputWithoutFormatting_.toString();
  this.nationalNumber_.clear();
  /** @type {Array.<string>} */
  var m = accruedInputWithoutFormatting.match(this.internationalPrefix_);
  if (m != null && m[0] != null && m[0].length > 0) {
    this.isInternationalFormatting_ = true;
    /** @type {number} */
    var startOfCountryCode = m[0].length;
    /** @type {!goog.string.StringBuffer} */
    var numberIncludeCountryCode = new goog.string.StringBuffer(
        accruedInputWithoutFormatting.substring(startOfCountryCode));
    /** @type {number} */
    var countryCode = this.phoneUtil_.extractCountryCode(
        numberIncludeCountryCode, this.nationalNumber_);
    if (countryCode == 0) {
      return false;
    } else {
      /** @type {string} */
      var newRegionCode =
          this.phoneUtil_.getRegionCodeForCountryCode(countryCode);
      if (newRegionCode != this.defaultCountry_) {
        this.initializeCountrySpecificInfo_(newRegionCode);
      }
      this.prefixBeforeNationalNumber_.append(accruedInputWithoutFormatting
          .substring(0, startOfCountryCode));
      if (accruedInputWithoutFormatting.charAt(0) !=
          i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN) {
        if (this.positionRemembered_ >
            this.prefixBeforeNationalNumber_.getLength()) {
          // Since a space will be inserted in front of the country code in this
          // case, we increase the remembered position by 1.
          this.positionRemembered_++;
        }
        this.prefixBeforeNationalNumber_.append(' ');
      }
      /** @type {string} */
      var countryCodeString = '' + countryCode;
      if (this.positionRemembered_ >
          this.prefixBeforeNationalNumber_.getLength() +
              countryCodeString.length) {
        // Since a space will be inserted after the country code in this case,
        // we increase the remembered position by 1.
        this.positionRemembered_++;
      }
      this.prefixBeforeNationalNumber_.append(countryCodeString).append(' ');
    }
  } else {
    this.nationalNumber_.clear();
    this.nationalNumber_.append(accruedInputWithoutFormatting);
  }
  return true;
};

/**
 * Accrues digits and the plus sign to accruedInputWithoutFormatting for later
 * use. If nextChar contains a digit in non-ASCII format (e.g. the full-width
 * version of digits), it is first normalized to the ASCII version. The return
 * value is nextChar itself, or its normalized version, if nextChar is a digit
 * in non-ASCII format.
 *
 * @param {string} nextChar
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
    normalizeAndAccrueDigitsAndPlusSign_ = function(nextChar) {

  if (nextChar == i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN) {
    this.accruedInputWithoutFormatting_.append(nextChar);
  }

  if (nextChar in i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS) {
    nextChar = i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS[nextChar];
    this.accruedInputWithoutFormatting_.append(nextChar);
    this.nationalNumber_.append(nextChar);
  }
  return nextChar;
};

/**
 * @param {string} nextChar
 * @param {boolean} rememberPosition
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.inputDigitHelper_ =
  function(nextChar, rememberPosition) {

  if (!(nextChar in i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS)) {
    return this.currentOutput_.toString();
  }

  /** @type {string} */
  var currentOutput = this.currentOutput_.toString();
  /** @type {string} */
  var currentOutput2 = currentOutput.substring(this.lastMatchPosition_);
  /** @type {number} */
  var digitPatternStart = currentOutput2.search(this.digitPattern_);
  if (digitPatternStart >= 0) {
    this.currentOutput_ = new goog.string.StringBuffer(
        currentOutput.substring(0, this.lastMatchPosition_) +
        currentOutput2.replace(this.digitPattern_, nextChar));
    this.lastMatchPosition_ += digitPatternStart;
    if (rememberPosition) {
      this.positionRemembered_ = this.prefixBeforeNationalNumber_.getLength() +
          this.lastMatchPosition_ + 1;
    }
    return this.currentOutput_.toString()
        .substring(0, this.lastMatchPosition_ + 1);
  } else {
    // More digits are entered than we could handle.
    this.currentOutput_.append(nextChar);
    this.ableToFormat_ = false;
    this.resetPositionOnFailureToFormat_();
    return this.accruedInput_.toString();
  }
};
