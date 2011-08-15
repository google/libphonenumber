/**
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
 * <p>An AsYouTypeFormatter can be created by new AsYouTypeFormatter(). After
 * that, digits can be added by invoking {@link #inputDigit} on the formatter
 * instance, and the partially formatted phone number will be returned each time
 * a digit is added. {@link #clear} can be invoked before formatting a new
 * number.
 *
 * <p>See the unittests for more details on how the formatter is to be used.
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
 * Constructs an AsYouTypeFormatter for the specific region.
 *
 * @param {string} regionCode the ISO 3166-1 two-letter region code that denotes
 *     the region where the phone number is being entered.
 * @constructor
 */
i18n.phonenumbers.AsYouTypeFormatter = function(regionCode) {
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
  this.isExpectingCountryCallingCode_ = false;
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
  this.currentMetaData_ = this.getMetadataForRegion_(this.defaultCountry_);
  /**
   * @type {i18n.phonenumbers.PhoneMetadata}
   * @private
   */
  this.defaultMetaData_ = this.currentMetaData_;
};


/**
 * @const
 * @type {i18n.phonenumbers.PhoneMetadata}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.EMPTY_METADATA_ =
    new i18n.phonenumbers.PhoneMetadata();
i18n.phonenumbers.AsYouTypeFormatter.EMPTY_METADATA_
    .setInternationalPrefix('NA');


/**
 * A pattern that is used to match character classes in regular expressions.
 * An example of a character class is [1-4].
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.CHARACTER_CLASS_PATTERN_ =
    /\[([^\[\]])*\]/g;


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
i18n.phonenumbers.AsYouTypeFormatter.STANDALONE_DIGIT_PATTERN_ =
    /\d(?=[^,}][^,}])/g;


/**
 * A pattern that is used to determine if a numberFormat under availableFormats
 * is eligible to be used by the AYTF. It is eligible when the format element
 * under numberFormat contains groups of the dollar sign followed by a single
 * digit, separated by valid phone number punctuation. This prevents invalid
 * punctuation (such as the star sign in Israeli star numbers) getting into the
 * output of the AYTF.
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.ELIGIBLE_FORMAT_PATTERN_ = new RegExp(
    '^[' + i18n.phonenumbers.PhoneNumberUtil.VALID_PUNCTUATION + ']*' +
    '(\\$\\d[' + i18n.phonenumbers.PhoneNumberUtil.VALID_PUNCTUATION + ']*)+$');


/**
 * This is the minimum length of national number accrued that is required to
 * trigger the formatter. The first element of the leadingDigitsPattern of
 * each numberFormat contains a regular expression that matches up to this
 * number of digits.
 * @const
 * @type {number}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.MIN_LEADING_DIGITS_LENGTH_ = 3;


/**
 * The metadata needed by this class is the same for all regions sharing the
 * same country calling code. Therefore, we return the metadata for "main"
 * region for this country calling code.
 * @param {string} regionCode
 * @return {i18n.phonenumbers.PhoneMetadata}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.getMetadataForRegion_ =
    function(regionCode) {

  /** @type {number} */
  var countryCallingCode = this.phoneUtil_.getCountryCodeForRegion(regionCode);
  /** @type {string} */
  var mainCountry =
      this.phoneUtil_.getRegionCodeForCountryCode(countryCallingCode);
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.phoneUtil_.getMetadataForRegion(mainCountry);
  if (metadata != null) {
    return metadata;
  }
  // Set to a default instance of the metadata. This allows us to function with
  // an incorrect region code, even if formatting only works for numbers
  // specified with '+'.
  return i18n.phonenumbers.AsYouTypeFormatter.EMPTY_METADATA_;
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
  var formatList =
      (this.isInternationalFormatting_ &&
           this.currentMetaData_.intlNumberFormatCount() > 0) ?
      this.currentMetaData_.intlNumberFormatArray() :
      this.currentMetaData_.numberFormatArray();
  /** @type {number} */
  var formatListLength = formatList.length;
  for (var i = 0; i < formatListLength; ++i) {
    /** @type {i18n.phonenumbers.NumberFormat} */
    var format = formatList[i];
    if (this.isFormatEligible_(format.getFormatOrDefault())) {
      this.possibleFormats_.push(format);
    }
  }
  this.narrowDownPossibleFormats_(leadingThreeDigits);
};


/**
 * @param {string} format
 * @return {boolean}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.isFormatEligible_ =
    function(format) {
  return i18n.phonenumbers.AsYouTypeFormatter.ELIGIBLE_FORMAT_PATTERN_
      .test(format);
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
  var indexOfLeadingDigitsPattern =
      leadingDigits.length -
      i18n.phonenumbers.AsYouTypeFormatter.MIN_LEADING_DIGITS_LENGTH_;
  /** @type {number} */
  var possibleFormatsLength = this.possibleFormats_.length;
  for (var i = 0; i < possibleFormatsLength; ++i) {
    /** @type {i18n.phonenumbers.NumberFormat} */
    var format = this.possibleFormats_[i];
    if (format.leadingDigitsPatternCount() > indexOfLeadingDigitsPattern) {
      /** @type {string} */
      var leadingDigitsPattern =
          format.getLeadingDigitsPatternOrDefault(indexOfLeadingDigitsPattern);
      if (leadingDigits.search(leadingDigitsPattern) == 0) {
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
  var numberPattern = format.getPatternOrDefault();

  // The formatter doesn't format numbers when numberPattern contains '|', e.g.
  // (20|3)\d{4}. In those cases we quickly return.
  if (numberPattern.indexOf('|') != -1) {
    return false;
  }

  // Replace anything in the form of [..] with \d
  numberPattern = numberPattern.replace(
      i18n.phonenumbers.AsYouTypeFormatter.CHARACTER_CLASS_PATTERN_, '\\d');

  // Replace any standalone digit (not the one in d{}) with \d
  numberPattern = numberPattern.replace(
      i18n.phonenumbers.AsYouTypeFormatter.STANDALONE_DIGIT_PATTERN_, '\\d');
  this.formattingTemplate_.clear();
  /** @type {string} */
  var tempTemplate = this.getFormattingTemplate_(numberPattern,
                                                 format.getFormatOrDefault());
  if (tempTemplate.length > 0) {
    this.formattingTemplate_.append(tempTemplate);
    return true;
  }
  return false;
};


/**
 * Gets a formatting template which can be used to efficiently format a
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
  // No formatting template can be created if the number of digits entered so
  // far is longer than the maximum the current formatting rule can accommodate.
  if (aPhoneNumber.length < this.nationalNumber_.getLength()) {
    return '';
  }
  // Formats the number according to numberFormat
  /** @type {string} */
  var template = aPhoneNumber.replace(new RegExp(numberPattern, 'g'),
                                      numberFormat);
  // Replaces each digit with character digitPlaceholder
  template = template.replace(new RegExp('9', 'g'), this.digitPlaceholder_);
  return template;
};


/**
 * Clears the internal state of the formatter, so it can be reused.
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
  this.isExpectingCountryCallingCode_ = false;
  this.possibleFormats_ = [];
  if (this.currentMetaData_ != this.defaultMetaData_) {
    this.currentMetaData_ = this.getMetadataForRegion_(this.defaultCountry_);
  }
};


/**
 * Formats a phone number on-the-fly as each digit is entered.
 *
 * @param {string} nextChar the most recently entered digit of a phone number.
 *     Formatting characters are allowed, but as soon as they are encountered
 *     this method formats the number as entered and not 'as you type' anymore.
 *     Full width digits and Arabic-indic digits are allowed, and will be shown
 *     as they are.
 * @return {string} the partially formatted phone number.
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.inputDigit = function(nextChar) {
  this.currentOutput_ =
      this.inputDigitWithOptionToRememberPosition_(nextChar, false);
  return this.currentOutput_;
};


/**
 * Same as {@link #inputDigit}, but remembers the position where
 * {@code nextChar} is inserted, so that it can be retrieved later by using
 * {@link #getRememberedPosition}. The remembered position will be automatically
 * adjusted if additional formatting characters are later inserted/removed in
 * front of {@code nextChar}.
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
  // digit, or a plus sign (accepted at the start of the number only).
  if (!this.isDigitOrLeadingPlusSign_(nextChar)) {
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
    case 0:
    case 1:
    case 2:
      return this.accruedInput_.toString();
    case 3:
      if (this.attemptToExtractIdd_()) {
        this.isExpectingCountryCallingCode_ = true;
      } else {
        // No IDD or plus sign is found, must be entering in national format.
        this.removeNationalPrefixFromNationalNumber_();
        return this.attemptToChooseFormattingPattern_();
      }
    case 4:
    case 5:
      if (this.isExpectingCountryCallingCode_) {
        if (this.attemptToExtractCountryCallingCode_()) {
          this.isExpectingCountryCallingCode_ = false;
        }
        return this.prefixBeforeNationalNumber_.toString() +
            this.nationalNumber_.toString();
      }
    // We make a last attempt to extract a country calling code at the 6th digit
    // because the maximum length of IDD and country calling code are both 3.
    case 6:
      if (this.isExpectingCountryCallingCode_ &&
          !this.attemptToExtractCountryCallingCode_()) {
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
 * @param {string} nextChar
 * @return {boolean}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.isDigitOrLeadingPlusSign_ =
    function(nextChar) {
  return i18n.phonenumbers.PhoneNumberUtil.CAPTURING_DIGIT_PATTERN
      .test(nextChar) ||
      (this.accruedInput_.getLength() == 1 &&
       i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_PATTERN.test(nextChar));
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
    var patternRegExp = new RegExp('^(?:' + pattern + ')$');
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
 * {@link #inputDigitAndRememberPosition}.
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
  while (accruedInputIndex < this.positionToRemember_ &&
         currentOutputIndex < currentOutput.length) {
    if (accruedInputWithoutFormatting.charAt(accruedInputIndex) ==
        currentOutput.charAt(currentOutputIndex)) {
      accruedInputIndex++;
    }
    currentOutputIndex++;
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
  if (nationalNumber.length >=
      i18n.phonenumbers.AsYouTypeFormatter.MIN_LEADING_DIGITS_LENGTH_) {
    this.getAvailableFormats_(
        nationalNumber.substring(0,
            i18n.phonenumbers.AsYouTypeFormatter.MIN_LEADING_DIGITS_LENGTH_));
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
    /** @type {RegExp} */
    var nationalPrefixForParsing = new RegExp(
        '^(?:' + this.currentMetaData_.getNationalPrefixForParsing() + ')');
    /** @type {Array.<string>} */
    var m = nationalNumber.match(nationalPrefixForParsing);
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
  /** @type {RegExp} */
  var internationalPrefix = new RegExp(
      '^(?:' + '\\' + i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN + '|' +
      this.currentMetaData_.getInternationalPrefix() + ')');
  /** @type {Array.<string>} */
  var m = accruedInputWithoutFormatting.match(internationalPrefix);
  if (m != null && m[0] != null && m[0].length > 0) {
    this.isInternationalFormatting_ = true;
    /** @type {number} */
    var startOfCountryCallingCode = m[0].length;
    this.nationalNumber_.clear();
    this.nationalNumber_.append(
        accruedInputWithoutFormatting.substring(startOfCountryCallingCode));
    this.prefixBeforeNationalNumber_.append(
        accruedInputWithoutFormatting.substring(0, startOfCountryCallingCode));
    if (accruedInputWithoutFormatting.charAt(0) !=
        i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN) {
      this.prefixBeforeNationalNumber_.append(' ');
    }
    return true;
  }
  return false;
};


/**
 * Extracts the country calling code from the beginning of nationalNumber to
 * prefixBeforeNationalNumber when they are available, and places the remaining
 * input into nationalNumber.
 *
 * @return {boolean} true when a valid country calling code can be found.
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
    attemptToExtractCountryCallingCode_ = function() {

  if (this.nationalNumber_.getLength() == 0) {
    return false;
  }
  /** @type {!goog.string.StringBuffer} */
  var numberWithoutCountryCallingCode = new goog.string.StringBuffer();
  /** @type {number} */
  var countryCode = this.phoneUtil_.extractCountryCode(
      this.nationalNumber_, numberWithoutCountryCallingCode);
  if (countryCode == 0) {
    return false;
  }
  this.nationalNumber_.clear();
  this.nationalNumber_.append(numberWithoutCountryCallingCode.toString());
  /** @type {string} */
  var newRegionCode = this.phoneUtil_.getRegionCodeForCountryCode(countryCode);
  if (newRegionCode != this.defaultCountry_) {
    this.currentMetaData_ = this.getMetadataForRegion_(newRegionCode);
  }
  /** @type {string} */
  var countryCodeString = '' + countryCode;
  this.prefixBeforeNationalNumber_.append(countryCodeString).append(' ');
  return true;
};


/**
 * Accrues digits and the plus sign to accruedInputWithoutFormatting for later
 * use. If nextChar contains a digit in non-ASCII format (e.g. the full-width
 * version of digits), it is first normalized to the ASCII version. The return
 * value is nextChar itself, or its normalized version, if nextChar is a digit
 * in non-ASCII format. This method assumes its input is either a digit or the
 * plus sign.
 *
 * @param {string} nextChar
 * @param {boolean} rememberPosition
 * @return {string}
 * @private
 */
i18n.phonenumbers.AsYouTypeFormatter.prototype.
    normalizeAndAccrueDigitsAndPlusSign_ = function(nextChar,
                                                    rememberPosition) {

  /** @type {string} */
  var normalizedChar;
  if (nextChar == i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN) {
    normalizedChar = nextChar;
    this.accruedInputWithoutFormatting_.append(nextChar);
  } else {
    normalizedChar = i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS[nextChar];
    this.accruedInputWithoutFormatting_.append(normalizedChar);
    this.nationalNumber_.append(normalizedChar);
  }
  if (rememberPosition) {
    this.positionToRemember_ = this.accruedInputWithoutFormatting_.getLength();
  }
  return normalizedChar;
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
    if (this.possibleFormats_.length == 1) {
      // More digits are entered than we could handle, and there are no other
      // valid patterns to try.
      this.ableToFormat_ = false;
    }  // else, we just reset the formatting pattern.
    this.currentFormattingPattern_ = '';
    return this.accruedInput_.toString();
  }
};
