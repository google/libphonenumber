/*
 * @license
 * Copyright (C) 2010 Google Inc.
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
   * A pattern that is used to match character classes in regular expressions.
   * An example of a character class is [1-4].
   * @const
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
   * @const
   * @type {RegExp}
   * @private
   */
  this.STANDALONE_DIGIT_PATTERN_ = /\d(?=[^,}][^,}])/g;
  /**
   * This is the minimum length of national number accrued that is required to
   * trigger the formatter. The first element of the leadingDigitsPattern of
   * each numberFormat contains a regular expression that matches up to this
   * number of digits.
   * @const
   * @type {number}
   * @private
   */
  this.MIN_LEADING_DIGITS_LENGTH_ = 3;
  /**
   * The digits that have not been entered yet will be represented by a \u2008,
   * the punctuation space.
   * @const
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
   * @type {string}
   * @private
   */
  this.currentOutput_ = '';
  /**
   * @type {!goog.string.StringBuffer}
   * @private
   */
  this.formattingTemplate_ = new goog.string.StringBuffer();
  /**
   * The pattern from numberFormat that is currently used to create
   * formattingTemplate.
   * @type {string}
   * @private
   */
  this.currentFormattingPattern_ = '';
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
   * @type {boolean}
   * @private
   */
  this.isExpectingCountryCode_ = false;
  /**
   * @type {i18n.phonenumbers.PhoneNumberUtil}
   * @private
   */
  this.phoneUtil_ = i18n.phonenumbers.PhoneNumberUtil.getInstance();
  /**
   * @type {number}
   * @private
   */
  this.lastMatchPosition_ = 0;
  /**
   * The position of a digit upon which inputDigitAndRememberPosition is most
   * recently invoked, as found in the original sequence of characters the user
   * entered.
   * @type {number}
   * @private
   */
  this.originalPosition_ = 0;
  /**
   * The position of a digit upon which inputDigitAndRememberPosition is most
   * recently invoked, as found in accruedInputWithoutFormatting.
   * entered.
   * @type {number}
   * @private
   */
  this.positionToRemember_ = 0;
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
   * @type {Array.<i18n.phonenumbers.NumberFormat>}
   * @private
   */
  this.possibleFormats_ = [];
  /**
   * @type {string}
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
 * @return {boolean} true if a new template is created as opposed to reusing the
 *     existing template.
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.maybeCreateNewTemplate_ =
    function() {

  // When there are multiple available formats, the formatter uses the first
  // format where a formatting template could be created.
  /** @type {number} */
  var possibleFormatsLength = this.possibleFormats_.length;
  for (var i = 0; i < possibleFormatsLength; ++i) {
    /** @type {i18n.phonenumbers.NumberFormat} */
    var numberFormat = this.possibleFormats_[i];
    /** @type {string} */
    var pattern = numberFormat.getPatternOrDefault();
    if (this.currentFormattingPattern_ == pattern) {
      return false;
    }
    if (this.createFormattingTemplate_(numberFormat)) {
      this.currentFormattingPattern_ = pattern;
      return true;
    }
  }
  this.ableToFormat_ = false;
  return false;
};


/**
 * @param {string} leadingThreeDigits
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.getAvailableFormats_ =
    function(leadingThreeDigits) {

  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var formatList = (this.isInternationalFormatting_ && this.currentMetaData_
      .intlNumberFormatCount() > 0) ? this.currentMetaData_
      .intlNumberFormatArray() : this.currentMetaData_.numberFormatArray();
  this.possibleFormats_ = formatList;
  this.narrowDownPossibleFormats_(leadingThreeDigits);
};


/**
 * @param {string} leadingDigits
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.narrowDownPossibleFormats_ =
    function(leadingDigits) {

  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var possibleFormats = [];
  /** @type {number} */
  var lengthOfLeadingDigits = leadingDigits.length;
  /** @type {number} */
  var indexOfLeadingDigitsPattern =
      lengthOfLeadingDigits - this.MIN_LEADING_DIGITS_LENGTH_;
  /** @type {number} */
  var possibleFormatsLength = this.possibleFormats_.length;
  for (var i = 0; i < possibleFormatsLength; ++i) {
    /** @type {i18n.phonenumbers.NumberFormat} */
    var format = this.possibleFormats_[i];
    if (format.leadingDigitsPatternCount() > indexOfLeadingDigitsPattern) {
      /** @type {RegExp} */
      var leadingDigitsPattern = new RegExp('^(' +
          format.getLeadingDigitsPattern(indexOfLeadingDigitsPattern) + ')');
      if (leadingDigitsPattern.test(leadingDigits)) {
        possibleFormats.push(this.possibleFormats_[i]);
      }
    } else {
      // else the particular format has no more specific leadingDigitsPattern,
      // and it should be retained.
      possibleFormats.push(this.possibleFormats_[i]);
    }
  }
  this.possibleFormats_ = possibleFormats;
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
  this.formattingTemplate_.clear();
  this.formattingTemplate_.append(this.getFormattingTemplate_(numberPattern,
      numberFormat));
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
  this.currentOutput_ = '';
  this.accruedInput_.clear();
  this.accruedInputWithoutFormatting_.clear();
  this.formattingTemplate_.clear();
  this.lastMatchPosition_ = 0;
  this.currentFormattingPattern_ = '';
  this.prefixBeforeNationalNumber_.clear();
  this.nationalNumber_.clear();
  this.ableToFormat_ = true;
  this.positionToRemember_ = 0;
  this.originalPosition_ = 0;
  this.isInternationalFormatting_ = false;
  this.isExpectingCountryCode_ = false;
  this.possibleFormats_ = [];
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
  this.currentOutput_ =
      this.inputDigitWithOptionToRememberPosition_(nextChar, false);
  return this.currentOutput_;
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

  this.currentOutput_ =
      this.inputDigitWithOptionToRememberPosition_(nextChar, true);
  return this.currentOutput_;
};


/**
 * @param {string} nextChar
 * @param {boolean} rememberPosition
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
    inputDigitWithOptionToRememberPosition_ = function(nextChar,
                                                       rememberPosition) {

  this.accruedInput_.append(nextChar);
  if (rememberPosition) {
    this.originalPosition_ = this.accruedInput_.getLength();
  }
  // We do formatting on-the-fly only when each character entered is either a
  // plus sign or a digit.
  if (!i18n.phonenumbers.PhoneNumberUtil.VALID_START_CHAR_PATTERN
      .test(nextChar)) {
    this.ableToFormat_ = false;
  }
  if (!this.ableToFormat_) {
    return this.accruedInput_.toString();
  }

  nextChar = this.normalizeAndAccrueDigitsAndPlusSign_(nextChar,
                                                       rememberPosition);

  // We start to attempt to format only when at least MIN_LEADING_DIGITS_LENGTH
  // digits (the plus sign is counted as a digit as well for this purpose) have
  // been entered.
  switch (this.accruedInputWithoutFormatting_.getLength()) {
    case 0: // when the first few inputs are neither digits nor the plus sign.
    case 1:
    case 2:
      return this.accruedInput_.toString();
    case 3:
      if (this.attemptToExtractIdd_()) {
        this.isExpectingCountryCode_ = true;
      } else {
        // No IDD or plus sign is found, must be entering in national format.
        this.removeNationalPrefixFromNationalNumber_();
        return this.attemptToChooseFormattingPattern_();
      }
    case 4:
    case 5:
      if (this.isExpectingCountryCode_) {
        if (this.attemptToExtractCountryCode_()) {
          this.isExpectingCountryCode_ = false;
        }
        return this.prefixBeforeNationalNumber_.toString() +
            this.nationalNumber_.toString();
      }
    // We make a last attempt to extract a country code at the 6th digit because
    // the maximum length of IDD and country code are both 3.
    case 6:
      if (this.isExpectingCountryCode_ &&
          !this.attemptToExtractCountryCode_()) {
        this.ableToFormat_ = false;
        return this.accruedInput_.toString();
      }
    default:
      if (this.possibleFormats_.length > 0) {
        // The formatting pattern is already chosen.
        /** @type {string} */
        var tempNationalNumber = this.inputDigitHelper_(nextChar);
        // See if the accrued digits can be formatted properly already. If not,
        // use the results from inputDigitHelper, which does formatting based on
        // the formatting pattern chosen.
        /** @type {string} */
        var formattedNumber = this.attemptToFormatAccruedDigits_();
        if (formattedNumber.length > 0) {
          return formattedNumber;
        }
        this.narrowDownPossibleFormats_(this.nationalNumber_.toString());
        if (this.maybeCreateNewTemplate_()) {
          return this.inputAccruedNationalNumber_();
        }
        return this.ableToFormat_ ?
            this.prefixBeforeNationalNumber_.toString() + tempNationalNumber :
            tempNationalNumber;
      } else {
        return this.attemptToChooseFormattingPattern_();
      }
  }
};


/**
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.attemptToFormatAccruedDigits_ =
    function() {

  /** @type {string} */
  var nationalNumber = this.nationalNumber_.toString();
  /** @type {number} */
  var possibleFormatsLength = this.possibleFormats_.length;
  for (var i = 0; i < possibleFormatsLength; ++i) {
    /** @type {i18n.phonenumbers.NumberFormat} */
    var numFormat = this.possibleFormats_[i];
    /** @type {string} */
    var pattern = numFormat.getPatternOrDefault();
    /** @type {RegExp} */
    var patternRegExp = new RegExp('^(' + pattern + ')$');
    if (patternRegExp.test(nationalNumber)) {
      /** @type {string} */
      var formattedNumber = nationalNumber.replace(new RegExp(pattern, 'g'),
                                                   numFormat.getFormat());
      return this.prefixBeforeNationalNumber_.toString() + formattedNumber;
    }
  }
  return '';
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

  if (!this.ableToFormat_) {
    return this.originalPosition_;
  }
  /** @type {number} */
  var accruedInputIndex = 0;
  /** @type {number} */
  var currentOutputIndex = 0;
  /** @type {string} */
  var accruedInputWithoutFormatting =
      this.accruedInputWithoutFormatting_.toString();
  /** @type {string} */
  var currentOutput = this.currentOutput_.toString();
  /** @type {number} */
  var currentOutputLength = currentOutput.length;
  while (accruedInputIndex < this.positionToRemember_ &&
         currentOutputIndex < currentOutputLength) {
    if (accruedInputWithoutFormatting.charAt(accruedInputIndex) ==
        currentOutput.charAt(currentOutputIndex)) {
      accruedInputIndex++;
      currentOutputIndex++;
    } else {
      currentOutputIndex++;
    }
  }
  return currentOutputIndex;
};


/**
 * Attempts to set the formatting template and returns a string which contains
 * the formatted version of the digits entered so far.
 *
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
    attemptToChooseFormattingPattern_ = function() {

  /** @type {string} */
  var nationalNumber = this.nationalNumber_.toString();
  // We start to attempt to format only when as least MIN_LEADING_DIGITS_LENGTH
  // digits of national number (excluding national prefix) have been entered.
  if (nationalNumber.length >= this.MIN_LEADING_DIGITS_LENGTH_) {
    this.getAvailableFormats_(
        nationalNumber.substring(0, this.MIN_LEADING_DIGITS_LENGTH_));
    this.maybeCreateNewTemplate_();
    return this.inputAccruedNationalNumber_();
  } else {
    return this.prefixBeforeNationalNumber_.toString() + nationalNumber;
  }
};


/**
 * Invokes inputDigitHelper on each digit of the national number accrued, and
 * returns a formatted string in the end.
 *
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.inputAccruedNationalNumber_ =
    function() {

  /** @type {string} */
  var nationalNumber = this.nationalNumber_.toString();
  /** @type {number} */
  var lengthOfNationalNumber = nationalNumber.length;
  if (lengthOfNationalNumber > 0) {
    /** @type {string} */
    var tempNationalNumber = '';
    for (var i = 0; i < lengthOfNationalNumber; i++) {
      tempNationalNumber =
          this.inputDigitHelper_(nationalNumber.charAt(i));
    }
    return this.ableToFormat_ ?
        this.prefixBeforeNationalNumber_.toString() + tempNationalNumber :
        tempNationalNumber;
  } else {
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
    this.isInternationalFormatting_ = true;
  } else if (this.currentMetaData_.hasNationalPrefix()) {
    /** @type {Array.<string>} */
    var m = nationalNumber.match(this.nationalPrefixForParsing_);
    if (m != null && m[0] != null && m[0].length > 0) {
      // When the national prefix is detected, we use international formatting
      // rules instead of national ones, because national formatting rules could
      // contain local formatting rules for numbers entered without area code.
      this.isInternationalFormatting_ = true;
      startOfNationalNumber = m[0].length;
      this.prefixBeforeNationalNumber_.append(nationalNumber.substring(0,
          startOfNationalNumber));
    }
  }
  this.nationalNumber_.clear();
  this.nationalNumber_.append(nationalNumber.substring(startOfNationalNumber));
};


/**
 * Extracts IDD and plus sign to prefixBeforeNationalNumber when they are
 * available, and places the remaining input into nationalNumber.
 *
 * @return {boolean} true when accruedInputWithoutFormatting begins with the
 *     plus sign or valid IDD for defaultCountry.
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.attemptToExtractIdd_ =
    function() {

  /** @type {string} */
  var accruedInputWithoutFormatting =
      this.accruedInputWithoutFormatting_.toString();
  /** @type {Array.<string>} */
  var m = accruedInputWithoutFormatting.match(this.internationalPrefix_);
  if (m != null && m[0] != null && m[0].length > 0) {
    this.isInternationalFormatting_ = true;
    /** @type {number} */
    var startOfCountryCode = m[0].length;
    this.nationalNumber_.clear();
    this.nationalNumber_.append(
        accruedInputWithoutFormatting.substring(startOfCountryCode));
    this.prefixBeforeNationalNumber_.append(
        accruedInputWithoutFormatting.substring(0, startOfCountryCode));
    if (accruedInputWithoutFormatting.charAt(0) !=
        i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN) {
      this.prefixBeforeNationalNumber_.append(' ');
    }
    return true;
  }
  return false;
};


/**
 * Extracts country code from the beginning of nationalNumber to
 * prefixBeforeNationalNumber when they are available, and places the remaining
 * input into nationalNumber.
 *
 * @return {boolean} true when a valid country code can be found.
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.attemptToExtractCountryCode_ =
    function() {

  if (this.nationalNumber_.getLength() == 0) {
    return false;
  }
  /** @type {!goog.string.StringBuffer} */
  var numberWithoutCountryCode = new goog.string.StringBuffer();
  /** @type {number} */
  var countryCode = this.phoneUtil_.extractCountryCode(
      this.nationalNumber_, numberWithoutCountryCode);
  if (countryCode == 0) {
    return false;
  } else {
    this.nationalNumber_.clear();
    this.nationalNumber_.append(numberWithoutCountryCode.toString());
    /** @type {string} */
    var newRegionCode =
        this.phoneUtil_.getRegionCodeForCountryCode(countryCode);
    if (newRegionCode != this.defaultCountry_) {
      this.initializeCountrySpecificInfo_(newRegionCode);
    }
    /** @type {string} */
    var countryCodeString = '' + countryCode;
    this.prefixBeforeNationalNumber_.append(countryCodeString).append(' ');
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
 * @param {boolean} rememberPosition
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
    normalizeAndAccrueDigitsAndPlusSign_ = function(nextChar,
                                                    rememberPosition) {

  if (nextChar == i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN) {
    this.accruedInputWithoutFormatting_.append(nextChar);
  }
  if (nextChar in i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS) {
    nextChar = i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS[nextChar];
    this.accruedInputWithoutFormatting_.append(nextChar);
    this.nationalNumber_.append(nextChar);
  }
  if (rememberPosition) {
    this.positionToRemember_ = this.accruedInputWithoutFormatting_.getLength();
  }
  return nextChar;
};


/**
 * @param {string} nextChar
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.inputDigitHelper_ =
    function(nextChar) {

  /** @type {string} */
  var formattingTemplate = this.formattingTemplate_.toString();
  if (formattingTemplate.substring(this.lastMatchPosition_)
      .search(this.digitPattern_) >= 0) {
    /** @type {number} */
    var digitPatternStart = formattingTemplate.search(this.digitPattern_);
    /** @type {string} */
    var tempTemplate = formattingTemplate.replace(this.digitPattern_, nextChar);
    this.formattingTemplate_.clear();
    this.formattingTemplate_.append(tempTemplate);
    this.lastMatchPosition_ = digitPatternStart;
    return tempTemplate.substring(0, this.lastMatchPosition_ + 1);
  } else {
    // More digits are entered than we could handle.
    this.ableToFormat_ = false;
    return this.accruedInput_.toString();
  }
};
