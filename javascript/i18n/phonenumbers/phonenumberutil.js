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
 * @fileoverview  Utility for international phone numbers.
 * Functionality includes formatting, parsing and validation.
 * (based on the java implementation).
 *
 * @author Nikolaos Trogkanis
 */

goog.provide('i18n.phonenumbers.PhoneNumberUtil');

goog.require('goog.array');
goog.require('goog.proto2.PbLiteSerializer');
goog.require('goog.string');
goog.require('goog.string.StringBuffer');
goog.require('i18n.phonenumbers.NumberFormat');
goog.require('i18n.phonenumbers.PhoneMetadata');
goog.require('i18n.phonenumbers.PhoneMetadataCollection');
goog.require('i18n.phonenumbers.PhoneNumber');
goog.require('i18n.phonenumbers.PhoneNumber.CountryCodeSource');
goog.require('i18n.phonenumbers.PhoneNumberDesc');
goog.require('i18n.phonenumbers.metadata');



/**
 * @constructor
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil = function() {
  /**
   * A mapping from a region code to the PhoneMetadata for that region.
   * @type {Object.<string, i18n.phonenumbers.PhoneMetadata>}
   */
  this.countryToMetadata = {};
};
goog.addSingletonGetter(i18n.phonenumbers.PhoneNumberUtil);


/**
 * Errors encountered when parsing phone numbers.
 *
 * @enum {string}
 */
i18n.phonenumbers.Error = {
  INVALID_COUNTRY_CODE: 'Invalid country code',
  // This generally indicates the string passed in had less than 3 digits in it.
  // More specifically, the number failed to match the regular expression
  // VALID_PHONE_NUMBER.
  NOT_A_NUMBER: 'The string supplied did not seem to be a phone number',
  // This indicates the string started with an international dialing prefix, but
  // after this was stripped from the number, had less digits than any valid
  // phone number (including country code) could have.
  TOO_SHORT_AFTER_IDD: 'Phone number too short after IDD',
  // This indicates the string, after any country code has been stripped, had
  // less digits than any
  // valid phone number could have.
  TOO_SHORT_NSN: 'The string supplied is too short to be a phone number',
  // This indicates the string had more digits than any valid phone number could
  // have.
  TOO_LONG: 'The string supplied is too long to be a phone number'
};


/**
 * @const
 * @type {number}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.NANPA_COUNTRY_CODE_ = 1;


/**
 * The minimum length of the national significant number.
 *
 * @const
 * @type {number}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.MIN_LENGTH_FOR_NSN_ = 3;


/**
 * The maximum length of the national significant number.
 *
 * @const
 * @type {number}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.MAX_LENGTH_FOR_NSN_ = 15;


/**
 * The maximum length of the country code.
 *
 * @const
 * @type {number}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.MAX_LENGTH_COUNTRY_CODE_ = 3;


/**
 * Region-code for the unknown region.
 *
 * @const
 * @type {string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.UNKNOWN_REGION_ = 'ZZ';


/**
 * The PLUS_SIGN signifies the international prefix.
 *
 * @const
 * @type {string}
 */
i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN = '+';


/**
 * These mappings map a character (key) to a specific digit that should replace
 * it for normalization purposes. Non-European digits that may be used in phone
 * numbers are mapped to a European equivalent.
 *
 * @const
 */
i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS = {
  '0': '0',
  '1': '1',
  '2': '2',
  '3': '3',
  '4': '4',
  '5': '5',
  '6': '6',
  '7': '7',
  '8': '8',
  '9': '9',
  '\uFF10': '0', // Fullwidth digit 0
  '\uFF11': '1', // Fullwidth digit 1
  '\uFF12': '2', // Fullwidth digit 2
  '\uFF13': '3', // Fullwidth digit 3
  '\uFF14': '4', // Fullwidth digit 4
  '\uFF15': '5', // Fullwidth digit 5
  '\uFF16': '6', // Fullwidth digit 6
  '\uFF17': '7', // Fullwidth digit 7
  '\uFF18': '8', // Fullwidth digit 8
  '\uFF19': '9', // Fullwidth digit 9
  '\u0660': '0', // Arabic-indic digit 0
  '\u0661': '1', // Arabic-indic digit 1
  '\u0662': '2', // Arabic-indic digit 2
  '\u0663': '3', // Arabic-indic digit 3
  '\u0664': '4', // Arabic-indic digit 4
  '\u0665': '5', // Arabic-indic digit 5
  '\u0666': '6', // Arabic-indic digit 6
  '\u0667': '7', // Arabic-indic digit 7
  '\u0668': '8', // Arabic-indic digit 8
  '\u0669': '9', // Arabic-indic digit 9
  '\u06F0': '0', // Eastern-Arabic digit 0
  '\u06F1': '1', // Eastern-Arabic digit 1
  '\u06F2': '2', // Eastern-Arabic digit 2
  '\u06F3': '3', // Eastern-Arabic digit 3
  '\u06F4': '4', // Eastern-Arabic digit 4
  '\u06F5': '5', // Eastern-Arabic digit 5
  '\u06F6': '6', // Eastern-Arabic digit 6
  '\u06F7': '7', // Eastern-Arabic digit 7
  '\u06F8': '8', // Eastern-Arabic digit 8
  '\u06F9': '9'  // Eastern-Arabic digit 9
};


/**
 * Only upper-case variants of alpha characters are stored.
 *
 * @const
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.ALPHA_MAPPINGS_ = {
  'A': '2',
  'B': '2',
  'C': '2',
  'D': '3',
  'E': '3',
  'F': '3',
  'G': '4',
  'H': '4',
  'I': '4',
  'J': '5',
  'K': '5',
  'L': '5',
  'M': '6',
  'N': '6',
  'O': '6',
  'P': '7',
  'Q': '7',
  'R': '7',
  'S': '7',
  'T': '8',
  'U': '8',
  'V': '8',
  'W': '9',
  'X': '9',
  'Y': '9',
  'Z': '9'
};


/**
 * For performance reasons, amalgamate both into one map.
 *
 * @const
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.ALL_NORMALIZATION_MAPPINGS_ = {
  '0': '0',
  '1': '1',
  '2': '2',
  '3': '3',
  '4': '4',
  '5': '5',
  '6': '6',
  '7': '7',
  '8': '8',
  '9': '9',
  '\uFF10': '0', // Fullwidth digit 0
  '\uFF11': '1', // Fullwidth digit 1
  '\uFF12': '2', // Fullwidth digit 2
  '\uFF13': '3', // Fullwidth digit 3
  '\uFF14': '4', // Fullwidth digit 4
  '\uFF15': '5', // Fullwidth digit 5
  '\uFF16': '6', // Fullwidth digit 6
  '\uFF17': '7', // Fullwidth digit 7
  '\uFF18': '8', // Fullwidth digit 8
  '\uFF19': '9', // Fullwidth digit 9
  '\u0660': '0', // Arabic-indic digit 0
  '\u0661': '1', // Arabic-indic digit 1
  '\u0662': '2', // Arabic-indic digit 2
  '\u0663': '3', // Arabic-indic digit 3
  '\u0664': '4', // Arabic-indic digit 4
  '\u0665': '5', // Arabic-indic digit 5
  '\u0666': '6', // Arabic-indic digit 6
  '\u0667': '7', // Arabic-indic digit 7
  '\u0668': '8', // Arabic-indic digit 8
  '\u0669': '9', // Arabic-indic digit 9
  '\u06F0': '0', // Eastern-Arabic digit 0
  '\u06F1': '1', // Eastern-Arabic digit 1
  '\u06F2': '2', // Eastern-Arabic digit 2
  '\u06F3': '3', // Eastern-Arabic digit 3
  '\u06F4': '4', // Eastern-Arabic digit 4
  '\u06F5': '5', // Eastern-Arabic digit 5
  '\u06F6': '6', // Eastern-Arabic digit 6
  '\u06F7': '7', // Eastern-Arabic digit 7
  '\u06F8': '8', // Eastern-Arabic digit 8
  '\u06F9': '9', // Eastern-Arabic digit 9
  'A': '2',
  'B': '2',
  'C': '2',
  'D': '3',
  'E': '3',
  'F': '3',
  'G': '4',
  'H': '4',
  'I': '4',
  'J': '5',
  'K': '5',
  'L': '5',
  'M': '6',
  'N': '6',
  'O': '6',
  'P': '7',
  'Q': '7',
  'R': '7',
  'S': '7',
  'T': '8',
  'U': '8',
  'V': '8',
  'W': '9',
  'X': '9',
  'Y': '9',
  'Z': '9'
};


/**
 * A list of all country codes where national significant numbers (excluding any
 * national prefix) exist that start with a leading zero.
 *
 * @const
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.LEADING_ZERO_COUNTRIES_ = {
  39: 1,  // Italy
  47: 1,  // Norway
  225: 1,  // Cote d'Ivoire
  227: 1,  // Niger
  228: 1,  // Togo
  241: 1,  // Gabon
  242: 1,  // Congo (Rep. of the)
  268: 1,  // Swaziland
  378: 1,  // San Marino
  379: 1,  // Vatican City
  501: 1   // Belize
};


/**
 * Pattern that makes it easy to distinguish whether a country has a unique
 * international dialing prefix or not. If a country has a unique international
 * prefix (e.g. 011 in USA), it will be represented as a string that contains a
 * sequence of ASCII digits. If there are multiple available international
 * prefixes in a country, they will be represented as a regex string that always
 * contains character(s) other than ASCII digits. Note this regex also includes
 * tilde, which signals waiting for the tone.
 *
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.UNIQUE_INTERNATIONAL_PREFIX_ =
    /[\d]+(?:[~\u2053\u223C\uFF5E][\d]+)?/;


/**
 * Regular expression of acceptable punctuation found in phone numbers. This
 * excludes punctuation found as a leading character only. This consists of dash
 * characters, white space characters, full stops, slashes, square brackets,
 * parentheses and tildes. It also includes the letter 'x' as that is found as a
 * placeholder for carrier information in some phone numbers.
 *
 * @const
 * @type {string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.VALID_PUNCTUATION_ =
    '-x\u2010-\u2015\u2212\u30FC\uFF0D-\uFF0F \u00A0\u200B\u2060\u3000()' +
    '\uFF08\uFF09\uFF3B\uFF3D.\\[\\]/~\u2053\u223C\uFF5E';


/**
 * Digits accepted in phone numbers (ascii, fullwidth, arabic-indic, and eastern
 * arabic digits).
 *
 * @const
 * @type {string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_ =
    '0-9\uFF10-\uFF19\u0660-\u0669\u06F0-\u06F9';


/**
 * We accept alpha characters in phone numbers, ASCII only, upper and lower
 * case.
 *
 * @const
 * @type {string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.VALID_ALPHA_ = 'A-Za-z';


/**
 * @const
 * @type {string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_ = '+\uFF0B';


/**
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_PATTERN_ =
    new RegExp('^[' + i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_ + ']+');


/**
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.CAPTURING_DIGIT_PATTERN_ =
    new RegExp('([' + i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_ + '])');


/**
 * Regular expression of acceptable characters that may start a phone number for
 * the purposes of parsing. This allows us to strip away meaningless prefixes to
 * phone numbers that may be mistakenly given to us. This consists of digits,
 * the plus symbol and arabic-indic digits. This does not contain alpha
 * characters, although they may be used later in the number. It also does not
 * include other punctuation, as this will be stripped later during parsing and
 * is of no information value when parsing a number.
 *
 * @const
 * @type {RegExp}
 * @protected
 */
i18n.phonenumbers.PhoneNumberUtil.VALID_START_CHAR_PATTERN =
    new RegExp('[' + i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_ +
               i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_ + ']');


/**
 * Regular expression of characters typically used to start a second phone
 * number for the purposes of parsing. This allows us to strip off parts of the
 * number that are actually the start of another number, such as for:
 * (530) 583-6985 x302/x2303 -> the second extension here makes this actually
 * two phone numbers, (530) 583-6985 x302 and (530) 583-6985 x2303. We remove
 * the second extension so that the first number is parsed correctly.
 *
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.SECOND_NUMBER_START_PATTERN_ = /[\\\/] *x/;


/**
 * Regular expression of trailing characters that we want to remove. We remove
 * all characters that are not alpha or numerical characters. The hash character
 * is retained here, as it may signify the previous block was an extension.
 *
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN_ =
    new RegExp('[^' + i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_ +
               i18n.phonenumbers.PhoneNumberUtil.VALID_ALPHA_ + '#]+$');


/**
 * We use this pattern to check if the phone number has at least three letters
 * in it - if so, then we treat it as a number where some phone-number digits
 * are represented by letters.
 *
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.VALID_ALPHA_PHONE_PATTERN_ =
    /(?:.*?[A-Za-z]){3}.*/;


/**
 * Regular expression of viable phone numbers. This is location independent.
 * Checks we have at least three leading digits, and only valid punctuation,
 * alpha characters and digits in the phone number. Does not include extension
 * data. The symbol 'x' is allowed here as valid punctuation since it is often
 * used as a placeholder for carrier codes, for example in Brazilian phone
 * numbers. We also allow multiple '+' characters at the start.
 * Corresponds to the following:
 * plus_sign*([punctuation]*[digits]){3,}([punctuation]|[digits]|[alpha])*
 * Note VALID_PUNCTUATION starts with a -, so must be the first in the range.
 *
 * @const
 * @type {string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.VALID_PHONE_NUMBER_ =
    '[' + i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_ + ']*(?:[' +
    i18n.phonenumbers.PhoneNumberUtil.VALID_PUNCTUATION_ + ']*[' +
    i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_ + ']){3,}[' +
    i18n.phonenumbers.PhoneNumberUtil.VALID_PUNCTUATION_ +
    i18n.phonenumbers.PhoneNumberUtil.VALID_ALPHA_ +
    i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_ + ']*';


/**
 * Default extension prefix to use when formatting. This will be put in front of
 * any extension component of the number, after the main national number is
 * formatted. For example, if you wish the default extension formatting to be
 * ' extn: 3456', then you should specify ' extn: ' here as the default
 * extension prefix. This can be overridden by country-specific preferences.
 *
 * @const
 * @type {string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.DEFAULT_EXTN_PREFIX_ = ' ext. ';


/**
 * Regexp of all possible ways to write extensions, for use when parsing. This
 * will be run as a case-insensitive regexp match. Wide character versions are
 * also provided after each ascii version. There are two regular expressions
 * here: the more generic one starts with optional white space and ends with an
 * optional full stop (.), followed by zero or more spaces/tabs and then the
 * numbers themselves. The other one covers the special case of American numbers
 * where the extension is written with a hash at the end, such as "- 503#". Note
 * that the only capturing groups should be around the digits that you want to
 * capture as part of the extension, or else parsing will fail! We allow two
 * options for representing the accented o - the character itself, and one in
 * the unicode decomposed form with the combining acute accent.
 *
 * @const
 * @type {string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.KNOWN_EXTN_PATTERNS_ =
    '[ \u00A0\\t,]*' +
    '(?:ext(?:ensi(?:o\u0301?|\u00F3))?n?|\uFF45\uFF58\uFF54\uFF4E?|' +
    '[,x\uFF58#\uFF03~\uFF5E]|int|anexo|\uFF49\uFF4E\uFF54)' +
    '[:\\.\uFF0E]?[ \u00A0\\t,-]*([' +
    i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_ + ']{1,7})#?|[- ]+([' +
    i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_ + ']{1,5})#';


/**
 * Regexp of all known extension prefixes used by different countries followed
 * by 1 or more valid digits, for use when parsing.
 *
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.EXTN_PATTERN_ =
    new RegExp('(?:' + i18n.phonenumbers.PhoneNumberUtil.KNOWN_EXTN_PATTERNS_ +
               ')$', 'i');


/**
 * We append optionally the extension pattern to the end here, as a valid phone
 * number may have an extension prefix appended, followed by 1 or more digits.
 *
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.VALID_PHONE_NUMBER_PATTERN_ =
    new RegExp('^' + i18n.phonenumbers.PhoneNumberUtil.VALID_PHONE_NUMBER_ +
               '(?:' + i18n.phonenumbers.PhoneNumberUtil.KNOWN_EXTN_PATTERNS_ +
               ')?' + '$', 'i');


/**
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.NON_DIGITS_PATTERN_ = /\D+/;


/**
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.FIRST_GROUP_PATTERN_ = /(\$1)/;


/**
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.NP_PATTERN_ = /\$NP/;


/**
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.FG_PATTERN_ = /\$FG/;


/**
 * @const
 * @type {RegExp}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.CC_PATTERN_ = /\$CC/;


/**
 * INTERNATIONAL and NATIONAL formats are consistent with the definition in
 * ITU-T Recommendation E. 123. For example, the number of the Google Zurich
 * office will be written as "+41 44 668 1800" in INTERNATIONAL format, and as
 * "044 668 1800" in NATIONAL format. E164 format is as per INTERNATIONAL format
 * but with no formatting applied, e.g. +41446681800.
 *
 * @enum {number}
 */
i18n.phonenumbers.PhoneNumberFormat = {
  E164: 0,
  INTERNATIONAL: 1,
  NATIONAL: 2
};


/**
 * Type of phone numbers.
 *
 * @enum {number}
 */
i18n.phonenumbers.PhoneNumberType = {
  FIXED_LINE: 0,
  MOBILE: 1,
  // In some countries (e.g. the USA), it is impossible to distinguish between
  // fixed-line and mobile numbers by looking at the phone number itself.
  FIXED_LINE_OR_MOBILE: 2,
  // Freephone lines
  TOLL_FREE: 3,
  PREMIUM_RATE: 4,
  // The cost of this call is shared between the caller and the recipient, and
  // is hence typically less than PREMIUM_RATE calls. See
  // http://en.wikipedia.org/wiki/Shared_Cost_Service for more information.
  SHARED_COST: 5,
  // Voice over IP numbers. This includes TSoIP (Telephony Service over IP).
  VOIP: 6,
  // A personal number is associated with a particular person, and may be routed
  // to either a MOBILE or FIXED_LINE number. Some more information can be found
  // here: http://en.wikipedia.org/wiki/Personal_Numbers
  PERSONAL_NUMBER: 7,
  PAGER: 8,
  // Used for "Universal Access Numbers" or "Company Numbers". They may be
  // further routed to specific offices, but allow one number to be used for a
  // company.
  UAN: 9,
  // A phone number is of type UNKNOWN when it does not fit any of the known
  // patterns for a specific country.
  UNKNOWN: 10
};


/**
 * Types of phone number matches. See detailed description beside the
 * isNumberMatch() method.
 *
 * @enum {number}
 */
i18n.phonenumbers.PhoneNumberUtil.MatchType = {
  NOT_A_NUMBER: 0,
  NO_MATCH: 1,
  SHORT_NSN_MATCH: 2,
  NSN_MATCH: 3,
  EXACT_MATCH: 4
};


/**
 * Possible outcomes when testing if a PhoneNumber is possible.
 *
 * @enum {number}
 */
i18n.phonenumbers.PhoneNumberUtil.ValidationResult = {
  IS_POSSIBLE: 0,
  INVALID_COUNTRY_CODE: 1,
  TOO_SHORT: 2,
  TOO_LONG: 3
};


/**
 * Attempts to extract a possible number from the string passed in. This
 * currently strips all leading characters that could not be used to start a
 * phone number. Characters that can be used to start a phone number are defined
 * in the VALID_START_CHAR_PATTERN. If none of these characters are found in the
 * number passed in, an empty string is returned. This function also attempts to
 * strip off any alternative extensions or endings if two or more are present,
 * such as in the case of: (530) 583-6985 x302/x2303. The second extension here
 * makes this actually two phone numbers, (530) 583-6985 x302 and (530) 583-6985
 * x2303. We remove the second extension so that the first number is parsed
 * correctly.
 *
 * @param {string} number the string that might contain a phone number.
 * @return {string} the number, stripped of any non-phone-number prefix (such as
 *     'Tel:') or an empty string if no character used to start phone numbers
 *     (such as + or any digit) is found in the number.
 */
i18n.phonenumbers.PhoneNumberUtil.extractPossibleNumber = function(number) {
  /** @type {string} */
  var possibleNumber;

  /** @type {number} */
  var start = number
      .search(i18n.phonenumbers.PhoneNumberUtil.VALID_START_CHAR_PATTERN);
  if (start >= 0) {
    possibleNumber = number.substring(start);
    // Remove trailing non-alpha non-numerical characters.
    possibleNumber = possibleNumber.replace(
        i18n.phonenumbers.PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN_, '');

    // Check for extra numbers at the end.
    /** @type {number} */
    var secondNumberStart = possibleNumber
        .search(i18n.phonenumbers.PhoneNumberUtil.SECOND_NUMBER_START_PATTERN_);
    if (secondNumberStart >= 0) {
      possibleNumber = possibleNumber.substring(0, secondNumberStart);
    }
  } else {
    possibleNumber = '';
  }
  return possibleNumber;
};


/**
 * Checks to see if the string of characters could possibly be a phone number at
 * all. At the moment, checks to see that the string begins with at least 3
 * digits, ignoring any punctuation commonly found in phone numbers. This method
 * does not require the number to be normalized in advance - but does assume
 * that leading non-number symbols have been removed, such as by the method
 * extractPossibleNumber.
 *
 * @param {string} number string to be checked for viability as a phone number.
 * @return {boolean} true if the number could be a phone number of some sort,
 *     otherwise false.
 */
i18n.phonenumbers.PhoneNumberUtil.isViablePhoneNumber = function(number) {
  if (number.length < i18n.phonenumbers.PhoneNumberUtil.MIN_LENGTH_FOR_NSN_) {
    return false;
  }
  return i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(
      i18n.phonenumbers.PhoneNumberUtil.VALID_PHONE_NUMBER_PATTERN_, number);
};


/**
 * Normalizes a string of characters representing a phone number. This performs
 * the following conversions:
 *  - Wide-ascii digits are converted to normal ASCII (European) digits.
 *  - Letters are converted to their numeric representation on a telephone
 * keypad. The keypad used here is the one defined in ITU Recommendation E.161.
 * This is only done if there are 3 or more letters in the number, to lessen the
 * risk that such letters are typos - otherwise alpha characters are stripped.
 *  - Punctuation is stripped.
 *  - Arabic-Indic numerals are converted to European numerals.
 *
 * @param {string} number a string of characters representing a phone number.
 * @return {string} the normalized string version of the phone number.
 */
i18n.phonenumbers.PhoneNumberUtil.normalize = function(number) {
  if (i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(
      i18n.phonenumbers.PhoneNumberUtil.VALID_ALPHA_PHONE_PATTERN_, number)) {
    return i18n.phonenumbers.PhoneNumberUtil.normalizeHelper_(number,
        i18n.phonenumbers.PhoneNumberUtil.ALL_NORMALIZATION_MAPPINGS_, true);
  } else {
    return i18n.phonenumbers.PhoneNumberUtil.normalizeHelper_(number,
        i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS, true);
  }
};


/**
 * Normalizes a string of characters representing a phone number. This is a
 * wrapper for normalize(String number) but does in-place normalization of the
 * StringBuffer provided.
 *
 * @param {!goog.string.StringBuffer} number a StringBuffer of characters
 *     representing a phone number that will be normalized in place.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.normalizeSB_ = function(number) {
  /** @type {string} */
  var normalizedNumber = i18n.phonenumbers.PhoneNumberUtil.normalize(number
      .toString());
  number.clear();
  number.append(normalizedNumber);
};


/**
 * Normalizes a string of characters representing a phone number. This converts
 * wide-ascii and arabic-indic numerals to European numerals, and strips
 * punctuation and alpha characters.
 *
 * @param {string} number a string of characters representing a phone number.
 * @return {string} the normalized string version of the phone number.
 */
i18n.phonenumbers.PhoneNumberUtil.normalizeDigitsOnly = function(number) {
  return i18n.phonenumbers.PhoneNumberUtil.normalizeHelper_(number,
      i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS, true);
};


/**
 * Converts all alpha characters in a number to their respective digits on a
 * keypad, but retains existing formatting. Also converts wide-ascii digits to
 * normal ascii digits, and converts Arabic-Indic numerals to European numerals.
 *
 * @param {string} number a string of characters representing a phone number.
 * @return {string} the normalized string version of the phone number.
 */
i18n.phonenumbers.PhoneNumberUtil.convertAlphaCharactersInNumber =
    function(number) {

  return i18n.phonenumbers.PhoneNumberUtil.normalizeHelper_(number,
      i18n.phonenumbers.PhoneNumberUtil.ALL_NORMALIZATION_MAPPINGS_, false);
};


/**
 * Gets the length of the geographical area code from the national_number field
 * of the PhoneNumber object passed in, so that clients could use it to split a
 * national significant number into geographical area code and subscriber
 * number. It works in such a way that the resultant subscriber number should be
 * diallable, at least on some devices. An example of how this could be used:
 *
 * var phoneUtil = i18n.phonenumbers.PhoneNumberUtil.getInstance();
 * var number = phoneUtil.parse('16502530000', 'US');
 * var nationalSignificantNumber =
 *     i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);
 * var areaCode;
 * var subscriberNumber;
 *
 * var areaCodeLength = phoneUtil.getLengthOfGeographicalAreaCode(number);
 * if (areaCodeLength > 0) {
 *   areaCode = nationalSignificantNumber.substring(0, areaCodeLength);
 *   subscriberNumber = nationalSignificantNumber.substring(areaCodeLength);
 * } else {
 *   areaCode = '';
 *   subscriberNumber = nationalSignificantNumber;
 * }
 *
 * N.B.: area code is a very ambiguous concept, so the I18N team generally
 * recommends against using it for most purposes, but recommends using the more
 * general national_number instead. Read the following carefully before deciding
 * to use this method:
 *  - geographical area codes change over time, and this method honors those
 * changes; therefore, it doesn't guarantee the stability of the result it
 * produces.
 *  - subscriber numbers may not be diallable from all devices (notably mobile
 * devices, which typically requires the full national_number to be dialled in
 * most countries).
 *  - most non-geographical numbers have no area codes.
 *  - some geographical numbers have no area codes.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the PhoneNumber object for
 *     which clients want to know the length of the area code.
 * @return {number} the length of area code of the PhoneNumber object passed in.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getLengthOfGeographicalAreaCode =
    function(number) {

  if (number == null) {
    return 0;
  }
  /** @type {?string} */
  var regionCode = this.getRegionCodeForNumber(number);
  if (!this.isValidRegionCode_(regionCode)) {
    return 0;
  }
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.getMetadataForRegion(regionCode);
  if (!metadata.hasNationalPrefix()) {
    return 0;
  }

  /** @type {i18n.phonenumbers.PhoneNumberType} */
  var type = this.getNumberTypeHelper_(
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number),
      metadata);
  // Most numbers other than the two types below have to be dialled in full.
  if (type != i18n.phonenumbers.PhoneNumberType.FIXED_LINE &&
      type != i18n.phonenumbers.PhoneNumberType.FIXED_LINE_OR_MOBILE) {
    return 0;
  }

  return this.getLengthOfNationalDestinationCode(number);
};


/**
 * Gets the length of the national destination code (NDC) from the PhoneNumber
 * object passed in, so that clients could use it to split a national
 * significant number into NDC and subscriber number. The NDC of a phone number
 * is normally the first group of digit(s) right after the country code when the
 * number is formatted in the international format, if there is a subscriber
 * number part that follows. An example of how this could be used:
 *
 * var phoneUtil = i18n.phonenumbers.PhoneNumberUtil.getInstance();
 * var number = phoneUtil.parse('18002530000', 'US');
 * var nationalSignificantNumber =
 *     i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);
 * var nationalDestinationCode;
 * var subscriberNumber;
 *
 * var nationalDestinationCodeLength =
 *     phoneUtil.getLengthOfNationalDestinationCode(number);
 * if (nationalDestinationCodeLength > 0) {
 *   nationalDestinationCode =
 *       nationalSignificantNumber.substring(0, nationalDestinationCodeLength);
 *   subscriberNumber =
 *       nationalSignificantNumber.substring(nationalDestinationCodeLength);
 * } else {
 *   nationalDestinationCode = '';
 *   subscriberNumber = nationalSignificantNumber;
 * }
 *
 * Refer to the unittests to see the difference between this function and
 * getLengthOfGeographicalAreaCode().
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the PhoneNumber object for
 *     which clients want to know the length of the NDC.
 * @return {number} the length of NDC of the PhoneNumber object passed in.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getLengthOfNationalDestinationCode =
    function(number) {

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var copiedProto;
  if (number.hasExtension()) {
    // We don't want to alter the proto given to us, but we don't want to
    // include the extension when we format it, so we copy it and clear the
    // extension here.
    copiedProto = number.clone();
    copiedProto.clearExtension();
  } else {
    copiedProto = number;
  }

  /** @type {string} */
  var nationalSignificantNumber = this.format(copiedProto,
      i18n.phonenumbers.PhoneNumberFormat.INTERNATIONAL);
  /** @type {!Array.<string>} */
  var numberGroups = nationalSignificantNumber.split(
      i18n.phonenumbers.PhoneNumberUtil.NON_DIGITS_PATTERN_);
  // The pattern will start with '+COUNTRY_CODE ' so the first group will always
  // be the empty string (before the + symbol) and the second group will be the
  // country code. The third group will be area code if it's not the last group.
  // NOTE: On IE the first group that is supposed to be the empty string does
  // not appear in the array of number groups... so make the result on non-IE
  // browsers to be that of IE.
  if (numberGroups[0].length == 0) {
    numberGroups.shift();
  }
  if (numberGroups.length <= 2) {
    return 0;
  }

  if (this.getRegionCodeForNumber(number) == 'AR' &&
      this.getNumberType(number) == i18n.phonenumbers.PhoneNumberType.MOBILE) {
    // Argentinian mobile numbers, when formatted in the international format,
    // are in the form of +54 9 NDC XXXX.... As a result, we take the length of
    // the third group (NDC) and add 1 for the digit 9, which also forms part of
    // the national significant number.
    //
    // TODO: Investigate the possibility of better modeling the metadata to make
    // it easier to obtain the NDC.
    return numberGroups[2].length + 1;
  }
  return numberGroups[1].length;
};


/**
 * Normalizes a string of characters representing a phone number by replacing
 * all characters found in the accompanying map with the values therein, and
 * stripping all other characters if removeNonMatches is true.
 *
 * @param {string} number a string of characters representing a phone number.
 * @param {!Object} normalizationReplacements a mapping of characters to what
 *     they should be replaced by in the normalized version of the phone number.
 * @param {boolean} removeNonMatches indicates whether characters that are not
 *     able to be replaced should be stripped from the number. If this is false,
 *     they will be left unchanged in the number.
 * @return {string} the normalized string version of the phone number.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.normalizeHelper_ =
    function(number, normalizationReplacements, removeNonMatches) {

  /** @type {!goog.string.StringBuffer} */
  var normalizedNumber = new goog.string.StringBuffer();
  /** @type {string} */
  var character;
  /** @type {string} */
  var newDigit;
  /** @type {number} */
  var numberLength = number.length;
  for (var i = 0; i < numberLength; ++i) {
    character = number.charAt(i);
    newDigit = normalizationReplacements[character.toUpperCase()];
    if (newDigit != null) {
      normalizedNumber.append(newDigit);
    } else if (!removeNonMatches) {
      normalizedNumber.append(character);
    }
    // If neither of the above are true, we remove this character.
  }
  return normalizedNumber.toString();
};


/**
 * Helper function to check region code is not unknown or null.
 *
 * @param {?string} regionCode the ISO 3166-1 two-letter country code that
 *     denotes the country/region that we want to get the country code for.
 * @return {boolean} true if region code is valid.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isValidRegionCode_ =
    function(regionCode) {

  return regionCode != null &&
      regionCode.toUpperCase() in i18n.phonenumbers.metadata.countryToMetadata;
};


/**
 * Formats a phone number in the specified format using default rules. Note that
 * this does not promise to produce a phone number that the user can dial from
 * where they are - although we do format in either 'national' or
 * 'international' format depending on what the client asks for, we do not
 * currently support a more abbreviated format, such as for users in the same
 * "area" who could potentially dial the number without area code. Note that if
 * the phone number has a country code of 0 or an otherwise invalid country
 * code, we cannot work out which formatting rules to apply so we return the
 * national significant number with no formatting applied.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number to be
 *     formatted.
 * @param {i18n.phonenumbers.PhoneNumberFormat} numberFormat the format the
 *     phone number should be formatted into.
 * @return {string} the formatted phone number.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.format =
    function(number, numberFormat) {

  /** @type {number} */
  var countryCode = number.getCountryCodeOrDefault();
  /** @type {string} */
  var nationalSignificantNumber = i18n.phonenumbers.PhoneNumberUtil
      .getNationalSignificantNumber(number);
  if (numberFormat == i18n.phonenumbers.PhoneNumberFormat.E164) {
    // Early exit for E164 case since no formatting of the national number needs
    // to be applied. Extensions are not formatted.
    return this.formatNumberByFormat_(countryCode,
                                      i18n.phonenumbers.PhoneNumberFormat.E164,
                                      nationalSignificantNumber, '');
  }
  // Note getRegionCodeForCountryCode() is used because formatting information
  // for countries which share a country code is contained by only one country
  // for performance reasons. For example, for NANPA countries it will be
  // contained in the metadata for US.
  /** @type {string} */
  var regionCode = this.getRegionCodeForCountryCode(countryCode);
  if (!this.isValidRegionCode_(regionCode)) {
    return nationalSignificantNumber;
  }

  /** @type {string} */
  var formattedExtension = this.maybeGetFormattedExtension_(number, regionCode);
  /** @type {string} */
  var formattedNationalNumber =
      this.formatNationalNumber_(nationalSignificantNumber,
                                 regionCode,
                                 numberFormat);
  return this.formatNumberByFormat_(countryCode,
                                    numberFormat,
                                    formattedNationalNumber,
                                    formattedExtension);
};


/**
 * Formats a phone number in the specified format using client-defined
 * formatting rules. Note that if the phone number has a country code of zero or
 * an otherwise invalid country code, we cannot work out things like whether
 * there should be a national prefix applied, or how to format extensions, so we
 * return the national significant number with no formatting applied.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone  number to be
 *     formatted.
 * @param {i18n.phonenumbers.PhoneNumberFormat} numberFormat the format the
 *     phone number should be formatted into.
 * @param {Array.<i18n.phonenumbers.NumberFormat>} userDefinedFormats formatting
 *     rules specified by clients.
 * @return {string} the formatted phone number.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.formatByPattern =
    function(number, numberFormat, userDefinedFormats) {

  /** @type {number} */
  var countryCode = number.getCountryCodeOrDefault();
  /** @type {string} */
  var nationalSignificantNumber =
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);
  // Note getRegionCodeForCountryCode() is used because formatting information
  // for countries which share a country code is contained by only one country
  // for performance reasons. For example, for NANPA countries it will be
  // contained in the metadata for US.
  /** @type {string} */
  var regionCode = this.getRegionCodeForCountryCode(countryCode);
  if (!this.isValidRegionCode_(regionCode)) {
    return nationalSignificantNumber;
  }
  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var userDefinedFormatsCopy = [];
  /** @type {number} */
  var size = userDefinedFormats.length;
  for (var i = 0; i < size; ++i) {
    /** @type {i18n.phonenumbers.NumberFormat} */
    var numFormat = userDefinedFormats[i];
    /** @type {string} */
    var nationalPrefixFormattingRule =
        numFormat.getNationalPrefixFormattingRuleOrDefault();
    if (nationalPrefixFormattingRule.length > 0) {
      // Before we do a replacement of the national prefix pattern $NP with the
      // national prefix, we need to copy the rule so that subsequent
      // replacements for different numbers have the appropriate national
      // prefix.
      /** type {i18n.phonenumbers.NumberFormat} */
      var numFormatCopy = numFormat.clone();
      /** @type {string} */
      var nationalPrefix =
          this.getMetadataForRegion(regionCode).getNationalPrefixOrDefault();
      if (nationalPrefix.length > 0) {
        // Replace $NP with national prefix and $FG with the first group ($1).
        nationalPrefixFormattingRule = nationalPrefixFormattingRule
            .replace(i18n.phonenumbers.PhoneNumberUtil.NP_PATTERN_,
                     nationalPrefix)
            .replace(i18n.phonenumbers.PhoneNumberUtil.FG_PATTERN_, '$1');
        numFormatCopy.setNationalPrefixFormattingRule(
            nationalPrefixFormattingRule);
      } else {
        // We don't want to have a rule for how to format the national prefix if
        // there isn't one.
        numFormatCopy.clearNationalPrefixFormattingRule();
      }
      userDefinedFormatsCopy.push(numFormatCopy);
    } else {
      // Otherwise, we just add the original rule to the modified list of
      // formats.
      userDefinedFormatsCopy.push(numFormat);
    }
  }

  /** @type {string} */
  var formattedExtension = this.maybeGetFormattedExtension_(number, regionCode);
  /** @type {string} */
  var formattedNationalNumber =
      this.formatAccordingToFormats_(nationalSignificantNumber,
                                     userDefinedFormatsCopy,
                                     numberFormat);
  return this.formatNumberByFormat_(countryCode,
                                    numberFormat,
                                    formattedNationalNumber,
                                    formattedExtension);
};


/**
 * Formats a phone number in national format for dialing using the carrier as
 * specified in the carrierCode. The carrierCode will always be used regardless
 * of whether the phone number already has a preferred domestic carrier code
 * stored. If carrierCode contains an empty string, return the number in
 * national format without any carrier code.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number to be
 *     formatted.
 * @param {string} carrierCode the carrier selection code to be used.
 * @return {string} the formatted phone number in national format for dialing
 *     using the carrier as specified in the carrierCode.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.
    formatNationalNumberWithCarrierCode = function(number, carrierCode) {

  /** @type {number} */
  var countryCode = number.getCountryCodeOrDefault();
  /** @type {string} */
  var nationalSignificantNumber =
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);
  // Note getRegionCodeForCountryCode() is used because formatting information
  // for countries which share a country code is contained by only one country
  // for performance reasons. For example, for NANPA countries it will be
  // contained in the metadata for US.
  /** @type {string} */
  var regionCode = this.getRegionCodeForCountryCode(countryCode);
  if (!this.isValidRegionCode_(regionCode)) {
    return nationalSignificantNumber;
  }

  /** @type {string} */
  var formattedExtension = this.maybeGetFormattedExtension_(number, regionCode);
  /** @type {string} */
  var formattedNationalNumber =
      this.formatNationalNumber_(nationalSignificantNumber,
                                 regionCode,
                                 i18n.phonenumbers.PhoneNumberFormat.NATIONAL,
                                 carrierCode);
  return this.formatNumberByFormat_(
      countryCode, i18n.phonenumbers.PhoneNumberFormat.NATIONAL,
      formattedNationalNumber, formattedExtension);
};


/**
 * Formats a phone number in national format for dialing using the carrier as
 * specified in the preferred_domestic_carrier_code field of the PhoneNumber
 * object passed in. If that is missing, use the fallbackCarrierCode passed in
 * instead. If there is no preferred_domestic_carrier_code, and the
 * fallbackCarrierCode contains an empty string, return the number in national
 * format without any carrier code.
 *
 * Use formatNationalNumberWithCarrierCode instead if the carrier code passed in
 * should take precedence over the number's preferred_domestic_carrier_code when
 * formatting.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number to be
 *     formatted.
 * @param {string} fallbackCarrierCode the carrier selection code to be used, if
 *     none is found in the phone number itself.
 * @return {string} the formatted phone number in national format for dialing
 *     using the number's preferred_domestic_carrier_code, or the
 *     fallbackCarrierCode passed in if none is found.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.
    formatNationalNumberWithPreferredCarrierCode = function(
        number, fallbackCarrierCode) {
  return this.formatNationalNumberWithCarrierCode(
      number,
      number.hasPreferredDomesticCarrierCode() ?
          number.getPreferredDomesticCarrierCodeOrDefault() :
          fallbackCarrierCode);
};


/**
 * Formats a phone number for out-of-country dialing purpose. If no
 * countryCallingFrom is supplied, we format the number in its INTERNATIONAL
 * format. If the countryCallingFrom is the same as the country where the number
 * is from, then NATIONAL formatting will be applied.
 *
 * If the number itself has a country code of zero or an otherwise invalid
 * country code, then we return the number with no formatting applied.
 *
 * Note this function takes care of the case for calling inside of NANPA and
 * between Russia and Kazakhstan (who share the same country code). In those
 * cases, no international prefix is used. For countries which have multiple
 * international prefixes, the number in its INTERNATIONAL format will be
 * returned instead.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number to be
 *     formatted.
 * @param {string} countryCallingFrom the ISO 3166-1 two-letter country code
 *     that denotes the foreign country where the call is being placed.
 * @return {string} the formatted phone number.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.formatOutOfCountryCallingNumber =
    function(number, countryCallingFrom) {

  if (!this.isValidRegionCode_(countryCallingFrom)) {
    return this.format(number,
                       i18n.phonenumbers.PhoneNumberFormat.INTERNATIONAL);
  }
  /** @type {number} */
  var countryCode = number.getCountryCodeOrDefault();
  /** @type {string} */
  var regionCode = this.getRegionCodeForCountryCode(countryCode);
  /** @type {string} */
  var nationalSignificantNumber =
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);
  if (!this.isValidRegionCode_(regionCode)) {
    return nationalSignificantNumber;
  }
  if (countryCode == i18n.phonenumbers.PhoneNumberUtil.NANPA_COUNTRY_CODE_) {
    if (this.isNANPACountry(countryCallingFrom)) {
      // For NANPA countries, return the national format for these countries but
      // prefix it with the country code.
      return countryCode + ' ' +
          this.format(number, i18n.phonenumbers.PhoneNumberFormat.NATIONAL);
    }
  } else if (countryCode == this.getCountryCodeForRegion(countryCallingFrom)) {
    // For countries that share a country calling code, the country code need
    // not be dialled. This also applies when dialling within a country, so this
    // if clause covers both these cases. Technically this is the case for
    // dialling from la Reunion to other overseas departments of France (French
    // Guiana, Martinique, Guadeloupe), but not vice versa - so we don't cover
    // this edge case for now and for those cases return the version including
    // country code. Details here:
    // http://www.petitfute.com/voyage/225-info-pratiques-reunion
    return this.format(number,
                       i18n.phonenumbers.PhoneNumberFormat.NATIONAL);
  }
  /** @type {string} */
  var formattedNationalNumber =
      this.formatNationalNumber_(nationalSignificantNumber, regionCode,
          i18n.phonenumbers.PhoneNumberFormat.INTERNATIONAL);
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.getMetadataForRegion(countryCallingFrom);
  /** @type {string} */
  var internationalPrefix = metadata.getInternationalPrefixOrDefault();
  /** @type {string} */
  var formattedExtension = this.maybeGetFormattedExtension_(number, regionCode);

  // For countries that have multiple international prefixes, the international
  // format of the number is returned, unless there is a preferred international
  // prefix.
  /** @type {string} */
  var internationalPrefixForFormatting = '';
  if (i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(
      i18n.phonenumbers.PhoneNumberUtil.UNIQUE_INTERNATIONAL_PREFIX_,
      internationalPrefix)) {
    internationalPrefixForFormatting = internationalPrefix;
  } else if (metadata.hasPreferredInternationalPrefix()) {
    internationalPrefixForFormatting =
        metadata.getPreferredInternationalPrefixOrDefault();
  }

  return internationalPrefixForFormatting != '' ?
      internationalPrefixForFormatting + ' ' + countryCode + ' ' +
          formattedNationalNumber + formattedExtension :
      this.formatNumberByFormat_(
          countryCode, i18n.phonenumbers.PhoneNumberFormat.INTERNATIONAL,
          formattedNationalNumber, formattedExtension);
};


/**
 * Formats a phone number using the original phone number format that the number
 * is parsed from. The original format is embedded in the country_code_source
 * field of the PhoneNumber object passed in. If such information is missing,
 * the number will be formatted into the NATIONAL format by default.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the PhoneNumber that needs to
 *     be formatted in its original number format.
 * @param {string} countryCallingFrom the country whose IDD needs to be prefixed
 *     if the original number has one.
 * @return {string} the formatted phone number in its original number format.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.formatInOriginalFormat =
    function(number, countryCallingFrom) {

  if (!number.hasCountryCodeSource()) {
    return this.format(number, i18n.phonenumbers.PhoneNumberFormat.NATIONAL);
  }
  switch (number.getCountryCodeSource()) {
    case i18n.phonenumbers.PhoneNumber.CountryCodeSource
        .FROM_NUMBER_WITH_PLUS_SIGN:
      return this.format(number,
          i18n.phonenumbers.PhoneNumberFormat.INTERNATIONAL);
    case i18n.phonenumbers.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_IDD:
      return this.formatOutOfCountryCallingNumber(number, countryCallingFrom);
    case i18n.phonenumbers.PhoneNumber.CountryCodeSource
        .FROM_NUMBER_WITHOUT_PLUS_SIGN:
      return this.format(number,
          i18n.phonenumbers.PhoneNumberFormat.INTERNATIONAL).substring(1);
    case i18n.phonenumbers.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY:
    default:
      return this.format(number, i18n.phonenumbers.PhoneNumberFormat.NATIONAL);
  }
};


/**
 * Gets the national significant number of the a phone number. Note a national
 * significant number doesn't contain a national prefix or any formatting.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the PhoneNumber object for
 *     which the national significant number is needed.
 * @return {string} the national significant number of the PhoneNumber object
 *     passed in.
 */
i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber =
    function(number) {

  // The leading zero in the national (significant) number of an Italian phone
  // number has a special meaning. Unlike the rest of the world, it indicates
  // the number is a landline number. There have been plans to migrate landline
  // numbers to start with the digit two since December 2000, but it has not yet
  // happened. See http://en.wikipedia.org/wiki/%2B39 for more details.
  // Other countries such as Cote d'Ivoire and Gabon use this for their mobile
  // numbers.
  /** @type {string} */
  var nationalNumber = '' + number.getNationalNumber();
  if (number.hasItalianLeadingZero() && number.getItalianLeadingZero() &&
      i18n.phonenumbers.PhoneNumberUtil.isLeadingZeroCountry(
          number.getCountryCodeOrDefault())) {
    return '0' + nationalNumber;
  }
  return nationalNumber;
};


/**
 * A helper function that is used by format and formatByPattern.
 *
 * @param {number} countryCode the country calling code.
 * @param {i18n.phonenumbers.PhoneNumberFormat} numberFormat the format the
 *     phone number should be formatted into.
 * @param {string} formattedNationalNumber
 * @param {string} formattedExtension
 * @return {string} the formatted phone number.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.formatNumberByFormat_ =
    function(countryCode, numberFormat,
             formattedNationalNumber, formattedExtension) {

  switch (numberFormat) {
    case i18n.phonenumbers.PhoneNumberFormat.E164:
      return i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN + countryCode +
          formattedNationalNumber + formattedExtension;
    case i18n.phonenumbers.PhoneNumberFormat.INTERNATIONAL:
      return i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN + countryCode + ' ' +
          formattedNationalNumber + formattedExtension;
    case i18n.phonenumbers.PhoneNumberFormat.NATIONAL:
    default:
      return formattedNationalNumber + formattedExtension;
  }
};


/**
 * Note in some countries, the national number can be written in two completely
 * different ways depending on whether it forms part of the NATIONAL format or
 * INTERNATIONAL format. The numberFormat parameter here is used to specify
 * which format to use for those cases. If a carrierCode is specified, this will
 * be inserted into the formatted string to replace $CC.
 *
 * @param {string} number a string of characters representing a phone number.
 * @param {string} regionCode the ISO 3166-1 two-letter country code.
 * @param {i18n.phonenumbers.PhoneNumberFormat} numberFormat the format the
 *     phone number should be formatted into.
 * @param {string=} opt_carrierCode
 * @return {string} the formatted phone number.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.formatNationalNumber_ =
    function(number, regionCode, numberFormat, opt_carrierCode) {

  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.getMetadataForRegion(regionCode);
  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var intlNumberFormats = metadata.intlNumberFormatArray();
  // When the intlNumberFormats exists, we use that to format national number
  // for the INTERNATIONAL format instead of using the numberDesc.numberFormats.
  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var availableFormats =
      (intlNumberFormats.length == 0 ||
          numberFormat == i18n.phonenumbers.PhoneNumberFormat.NATIONAL) ?
      metadata.numberFormatArray() : metadata.intlNumberFormatArray();
  return this.formatAccordingToFormats_(number, availableFormats, numberFormat,
                                        opt_carrierCode);
};


/**
 * Note that carrierCode is optional - if NULL or an empty string, no carrier
 * code replacement will take place.
 *
 * @param {string} nationalNumber a string of characters representing a phone
 *     number.
 * @param {Array.<i18n.phonenumbers.NumberFormat>} availableFormats the
 *     available formats the phone number could be formatted into.
 * @param {i18n.phonenumbers.PhoneNumberFormat} numberFormat the format the
 *     phone number should be formatted into.
 * @param {string=} opt_carrierCode
 * @return {string} the formatted phone number.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.formatAccordingToFormats_ =
    function(nationalNumber, availableFormats, numberFormat, opt_carrierCode) {

  /** @type {i18n.phonenumbers.NumberFormat} */
  var numFormat;
  /** @type {number} */
  var l = availableFormats.length;
  for (var i = 0; i < l; ++i) {
    numFormat = availableFormats[i];
    /** @type {number} */
    var size = numFormat.leadingDigitsPatternCount();
    if (size == 0 ||
        // We always use the last leading_digits_pattern, as it is the most
        // detailed.
        nationalNumber
            .search(numFormat.getLeadingDigitsPattern(size - 1)) == 0) {
      /** @type {RegExp} */
      var patternToMatch = new RegExp(numFormat.getPattern());
      if (i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(patternToMatch,
                                                             nationalNumber)) {
        /** @type {string} */
        var numberFormatRule = numFormat.getFormatOrDefault();
        /** @type {string} */
        var domesticCarrierCodeFormattingRule =
            numFormat.getDomesticCarrierCodeFormattingRuleOrDefault();
        if (numberFormat == i18n.phonenumbers.PhoneNumberFormat.NATIONAL &&
            opt_carrierCode != null && opt_carrierCode.length > 0 &&
            domesticCarrierCodeFormattingRule.length > 0) {
          // Replace the $CC in the formatting rule with the desired carrier
          // code.
          /** @type {string} */
          var carrierCodeFormattingRule = domesticCarrierCodeFormattingRule
              .replace(i18n.phonenumbers.PhoneNumberUtil.CC_PATTERN_,
                       opt_carrierCode);
          // Now replace the $FG in the formatting rule with the first group
          // and the carrier code combined in the appropriate way.
          numberFormatRule = numberFormatRule.replace(
              i18n.phonenumbers.PhoneNumberUtil.FIRST_GROUP_PATTERN_,
              carrierCodeFormattingRule);
          return nationalNumber.replace(patternToMatch, numberFormatRule);
        } else {
          // Use the national prefix formatting rule instead.
          /** @type {string} */
          var nationalPrefixFormattingRule =
              numFormat.getNationalPrefixFormattingRuleOrDefault();
          if (numberFormat == i18n.phonenumbers.PhoneNumberFormat.NATIONAL &&
              nationalPrefixFormattingRule != null &&
              nationalPrefixFormattingRule.length > 0) {
            return nationalNumber.replace(patternToMatch, numberFormatRule
                .replace(i18n.phonenumbers.PhoneNumberUtil.FIRST_GROUP_PATTERN_,
                         nationalPrefixFormattingRule));
          } else {
            return nationalNumber.replace(patternToMatch, numberFormatRule);
          }
        }
      }
    }
  }

  // If no pattern above is matched, we format the number as a whole.
  return nationalNumber;
};


/**
 * Gets a valid number for the specified country.
 *
 * @param {string} regionCode the ISO 3166-1 two-letter country code that
 *     denotes the country for which an example number is needed.
 * @return {i18n.phonenumbers.PhoneNumber} a valid fixed-line number for the
 *     specified country. Returns null when the metadata does not contain such
 *     information.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getExampleNumber =
    function(regionCode) {

  return this.getExampleNumberForType(regionCode,
      i18n.phonenumbers.PhoneNumberType.FIXED_LINE);
};


/**
 * Gets a valid number, if any, for the specified country and number type.
 *
 * @param {string} regionCode the ISO 3166-1 two-letter country code that
 *     denotes the country for which an example number is needed.
 * @param {i18n.phonenumbers.PhoneNumberType} type the type of number that is
 *     needed.
 * @return {i18n.phonenumbers.PhoneNumber} a valid number for the specified
 *     country and type. Returns null when the metadata does not contain such
 *     information.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getExampleNumberForType =
    function(regionCode, type) {

  /** @type {i18n.phonenumbers.PhoneNumberDesc} */
  var desc = this.getNumberDescByType_(
      this.getMetadataForRegion(regionCode), type);
  try {
    if (desc.hasExampleNumber()) {
      return this.parse(desc.getExampleNumberOrDefault(), regionCode);
    }
  } catch (e) {
  }
  return null;
};


/**
 * Gets the formatted extension of a phone number, if the phone number had an
 * extension specified. If not, it returns an empty string.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the PhoneNumber that might have
 *     an extension.
 * @param {string} regionCode the ISO 3166-1 two-letter country code.
 * @return {string} the formatted extension if any.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.maybeGetFormattedExtension_ =
    function(number, regionCode) {

  if (!number.hasExtension()) {
    return '';
  } else {
    return this.formatExtension_(number.getExtensionOrDefault(), regionCode);
  }
};


/**
 * Formats the extension part of the phone number by prefixing it with the
 * appropriate extension prefix. This will be the default extension prefix,
 * unless overridden by a preferred extension prefix for this country.
 *
 * @param {string} extensionDigits the extension digits.
 * @param {string} regionCode the ISO 3166-1 two-letter country code.
 * @return {string} the formatted extension.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.formatExtension_ =
    function(extensionDigits, regionCode) {

  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.getMetadataForRegion(regionCode);
  if (metadata.hasPreferredExtnPrefix()) {
    return metadata.getPreferredExtnPrefix() + extensionDigits;
  } else {
    return i18n.phonenumbers.PhoneNumberUtil.DEFAULT_EXTN_PREFIX_ +
        extensionDigits;
  }
};


/**
 * @param {i18n.phonenumbers.PhoneMetadata} metadata
 * @param {i18n.phonenumbers.PhoneNumberType} type
 * @return {i18n.phonenumbers.PhoneNumberDesc}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getNumberDescByType_ =
    function(metadata, type) {

  switch (type) {
    case i18n.phonenumbers.PhoneNumberType.PREMIUM_RATE:
      return metadata.getPremiumRate();
    case i18n.phonenumbers.PhoneNumberType.TOLL_FREE:
      return metadata.getTollFree();
    case i18n.phonenumbers.PhoneNumberType.MOBILE:
      return metadata.getMobile();
    case i18n.phonenumbers.PhoneNumberType.FIXED_LINE:
    case i18n.phonenumbers.PhoneNumberType.FIXED_LINE_OR_MOBILE:
      return metadata.getFixedLine();
    case i18n.phonenumbers.PhoneNumberType.SHARED_COST:
      return metadata.getSharedCost();
    case i18n.phonenumbers.PhoneNumberType.VOIP:
      return metadata.getVoip();
    case i18n.phonenumbers.PhoneNumberType.PERSONAL_NUMBER:
      return metadata.getPersonalNumber();
    case i18n.phonenumbers.PhoneNumberType.PAGER:
      return metadata.getPager();
    case i18n.phonenumbers.PhoneNumberType.UAN:
      return metadata.getUan();
    default:
      return metadata.getGeneralDesc();
  }
};


/**
 * Gets the type of a phone number.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number that we want
 *     to know the type.
 * @return {i18n.phonenumbers.PhoneNumberType} the type of the phone number.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getNumberType =
    function(number) {

  /** @type {?string} */
  var regionCode = this.getRegionCodeForNumber(number);
  if (!this.isValidRegionCode_(regionCode)) {
    return i18n.phonenumbers.PhoneNumberType.UNKNOWN;
  }
  /** @type {string} */
  var nationalSignificantNumber =
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);
  return this.getNumberTypeHelper_(nationalSignificantNumber,
      this.getMetadataForRegion(regionCode));
};


/**
 * @param {string} nationalNumber
 * @param {i18n.phonenumbers.PhoneMetadata} metadata
 * @return {i18n.phonenumbers.PhoneNumberType}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getNumberTypeHelper_ =
    function(nationalNumber, metadata) {

  /** @type {i18n.phonenumbers.PhoneNumberDesc} */
  var generalNumberDesc = metadata.getGeneralDesc();
  if (!generalNumberDesc.hasNationalNumberPattern() ||
      !this.isNumberMatchingDesc_(nationalNumber, generalNumberDesc)) {
    return i18n.phonenumbers.PhoneNumberType.UNKNOWN;
  }

  if (this.isNumberMatchingDesc_(nationalNumber, metadata.getPremiumRate())) {
    return i18n.phonenumbers.PhoneNumberType.PREMIUM_RATE;
  }
  if (this.isNumberMatchingDesc_(nationalNumber, metadata.getTollFree())) {
    return i18n.phonenumbers.PhoneNumberType.TOLL_FREE;
  }
  if (this.isNumberMatchingDesc_(nationalNumber, metadata.getSharedCost())) {
    return i18n.phonenumbers.PhoneNumberType.SHARED_COST;
  }
  if (this.isNumberMatchingDesc_(nationalNumber, metadata.getVoip())) {
    return i18n.phonenumbers.PhoneNumberType.VOIP;
  }
  if (this.isNumberMatchingDesc_(nationalNumber,
                                 metadata.getPersonalNumber())) {
    return i18n.phonenumbers.PhoneNumberType.PERSONAL_NUMBER;
  }
  if (this.isNumberMatchingDesc_(nationalNumber, metadata.getPager())) {
    return i18n.phonenumbers.PhoneNumberType.PAGER;
  }
  if (this.isNumberMatchingDesc_(nationalNumber, metadata.getUan())) {
    return i18n.phonenumbers.PhoneNumberType.UAN;
  }

  /** @type {boolean} */
  var isFixedLine = this.isNumberMatchingDesc_(nationalNumber, metadata
      .getFixedLine());
  if (isFixedLine) {
    if (metadata.getSameMobileAndFixedLinePattern()) {
      return i18n.phonenumbers.PhoneNumberType.FIXED_LINE_OR_MOBILE;
    } else if (this.isNumberMatchingDesc_(nationalNumber,
                                          metadata.getMobile())) {
      return i18n.phonenumbers.PhoneNumberType.FIXED_LINE_OR_MOBILE;
    }
    return i18n.phonenumbers.PhoneNumberType.FIXED_LINE;
  }
  // Otherwise, test to see if the number is mobile. Only do this if certain
  // that the patterns for mobile and fixed line aren't the same.
  if (!metadata.getSameMobileAndFixedLinePattern() &&
      this.isNumberMatchingDesc_(nationalNumber, metadata.getMobile())) {
    return i18n.phonenumbers.PhoneNumberType.MOBILE;
  }
  return i18n.phonenumbers.PhoneNumberType.UNKNOWN;
};


/**
 * @param {?string} regionCode
 * @return {i18n.phonenumbers.PhoneMetadata}
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getMetadataForRegion =
    function(regionCode) {

  if (regionCode == null) {
    return null;
  }
  regionCode = regionCode.toUpperCase();
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.countryToMetadata[regionCode];
  if (metadata == null) {
    /** @type {goog.proto2.PbLiteSerializer} */
    var serializer = new goog.proto2.PbLiteSerializer();
    /** @type {Array} */
    var metadataSerialized =
        i18n.phonenumbers.metadata.countryToMetadata[regionCode];
    if (metadataSerialized == null) {
      return null;
    }
    metadata = /** @type {i18n.phonenumbers.PhoneMetadata} */ (
        serializer.deserialize(i18n.phonenumbers.PhoneMetadata.getDescriptor(),
            metadataSerialized));
    this.countryToMetadata[regionCode] = metadata;
  }
  return metadata;
};


/**
 * @param {string} nationalNumber
 * @param {i18n.phonenumbers.PhoneNumberDesc} numberDesc
 * @return {boolean}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isNumberMatchingDesc_ =
    function(nationalNumber, numberDesc) {

  return i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(
      numberDesc.getPossibleNumberPattern(), nationalNumber) &&
      i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(
          numberDesc.getNationalNumberPattern(), nationalNumber);
};


/**
 * Tests whether a phone number matches a valid pattern. Note this doesn't
 * verify the number is actually in use, which is impossible to tell by just
 * looking at a number itself.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number that we want
 *     to validate.
 * @return {boolean} a boolean that indicates whether the number is of a valid
 *     pattern.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isValidNumber = function(number) {
  /** @type {?string} */
  var regionCode = this.getRegionCodeForNumber(number);
  return this.isValidRegionCode_(regionCode) &&
      this.isValidNumberForRegion(number, /** @type {string} */ (regionCode));
};


/**
 * Tests whether a phone number is valid for a certain region. Note this doesn't
 * verify the number is actually in use, which is impossible to tell by just
 * looking at a number itself. If the country code is not the same as the
 * country code for the region, this immediately exits with false. After this,
 * the specific number pattern rules for the region are examined. This is useful
 * for determining for example whether a particular number is valid for Canada,
 * rather than just a valid NANPA number.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number that we want
 *     to validate.
 * @param {string} regionCode the ISO 3166-1 two-letter country code that
 *     denotes the region/country that we want to validate the phone number for.
 * @return {boolean} a boolean that indicates whether the number is of a valid
 *     pattern.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isValidNumberForRegion =
    function(number, regionCode) {

  if (number.getCountryCodeOrDefault() !=
      this.getCountryCodeForRegion(regionCode)) {
    return false;
  }
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.getMetadataForRegion(regionCode);
  /** @type {i18n.phonenumbers.PhoneNumberDesc} */
  var generalNumDesc = metadata.getGeneralDesc();
  /** @type {string} */
  var nationalSignificantNumber =
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);

  // For countries where we don't have metadata for PhoneNumberDesc, we treat
  // any number passed in as a valid number if its national significant number
  // is between the minimum and maximum lengths defined by ITU for a national
  // significant number.
  if (!generalNumDesc.hasNationalNumberPattern()) {
    /** @type {number} */
    var numberLength = nationalSignificantNumber.length;
    return numberLength >
        i18n.phonenumbers.PhoneNumberUtil.MIN_LENGTH_FOR_NSN_ &&
        numberLength <= i18n.phonenumbers.PhoneNumberUtil.MAX_LENGTH_FOR_NSN_;
  }
  return this.getNumberTypeHelper_(nationalSignificantNumber, metadata) !=
      i18n.phonenumbers.PhoneNumberType.UNKNOWN;
};


/**
 * Returns the country/region where a phone number is from. This could be used
 * for geo-coding in the country/region level.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone number whose origin
 *     we want to know.
 * @return {?string} the country/region where the phone number is from, or null
 *     if no country matches this calling code.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getRegionCodeForNumber =
    function(number) {

  if (number == null) {
    return null;
  }
  /** @type {number} */
  var countryCode = number.getCountryCodeOrDefault();
  /** @type {Array.<string>} */
  var regions =
      i18n.phonenumbers.metadata.countryCodeToRegionCodeMap[countryCode];
  if (regions == null) {
    return null;
  }
  if (regions.length == 1) {
    return regions[0];
  } else {
    return this.getRegionCodeForNumberFromRegionList_(number, regions);
  }
};


/**
 * @param {i18n.phonenumbers.PhoneNumber} number
 * @param {Array.<string>} regionCodes
 * @return {?string}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.
    getRegionCodeForNumberFromRegionList_ = function(number, regionCodes) {

  /** @type {string} */
  var nationalNumber = '' + number.getNationalNumber();
  /** @type {string} */
  var regionCode;
  /** @type {number} */
  var regionCodesLength = regionCodes.length;
  for (var i = 0; i < regionCodesLength; i++) {
    regionCode = regionCodes[i];
    // If leadingDigits is present, use this. Otherwise, do full validation.
    /** @type {i18n.phonenumbers.PhoneMetadata} */
    var metadata = this.getMetadataForRegion(regionCode);
    if (metadata.hasLeadingDigits()) {
      if (nationalNumber.search(metadata.getLeadingDigits()) == 0) {
        return regionCode;
      }
    } else if (this.getNumberTypeHelper_(nationalNumber, metadata) !=
        i18n.phonenumbers.PhoneNumberType.UNKNOWN) {
      return regionCode;
    }
  }
  return null;
};


/**
 * Returns the region code that matches the specific country code. In the case
 * of no region code being found, ZZ will be returned. In the case of multiple
 * regions, the one designated in the metadata as the "main" country for this
 * calling code will be returned.
 *
 * @param {number} countryCode the country calling code.
 * @return {string}
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getRegionCodeForCountryCode =
    function(countryCode) {

  /** @type {Array.<string>} */
  var regionCodes =
      i18n.phonenumbers.metadata.countryCodeToRegionCodeMap[countryCode];
  return regionCodes == null ?
      i18n.phonenumbers.PhoneNumberUtil.UNKNOWN_REGION_ : regionCodes[0];
};


/**
 * Returns the country calling code for a specific region. For example, this
 * would be 1 for the United States, and 64 for New Zealand.
 *
 * @param {?string} regionCode the ISO 3166-1 two-letter country code that
 *     denotes the country/region that we want to get the country code for.
 * @return {number} the country calling code for the country/region denoted by
 *     regionCode.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getCountryCodeForRegion =
    function(regionCode) {

  if (!this.isValidRegionCode_(regionCode)) {
    return 0;
  }
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.getMetadataForRegion(regionCode);
  if (metadata == null) {
    return 0;
  }
  return metadata.getCountryCodeOrDefault();
};


/**
 * Returns the national dialling prefix for a specific region. For example, this
 * would be 1 for the United States, and 0 for New Zealand. Set stripNonDigits
 * to true to strip symbols like "~" (which indicates a wait for a dialling
 * tone) from the prefix returned. If no national prefix is present, we return
 * null.
 *
 * Warning: Do not use this method for do-your-own formatting - for some
 * countries, the national dialling prefix is used only for certain types of
 * numbers. Use the library's formatting functions to prefix the national prefix
 * when required.
 *
 * @param {string} regionCode the ISO 3166-1 two-letter country code that
 *     denotes the country/region that we want to get the dialling prefix for.
 * @param {boolean} stripNonDigits true to strip non-digits from the national
 *     dialling prefix.
 * @return {?string} the dialling prefix for the country/region denoted by
 *     regionCode.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.getNddPrefixForRegion = function(
    regionCode, stripNonDigits) {
  if (!this.isValidRegionCode_(regionCode)) {
    return null;
  }
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.getMetadataForRegion(regionCode);
  if (metadata == null) {
    return null;
  }
  /** @type {string} */
  var nationalPrefix = metadata.getNationalPrefixOrDefault();
  // If no national prefix was found, we return null.
  if (nationalPrefix.length == 0) {
    return null;
  }
  if (stripNonDigits) {
    // Note: if any other non-numeric symbols are ever used in national
    // prefixes, these would have to be removed here as well.
    nationalPrefix = nationalPrefix.replace('~', '');
  }
  return nationalPrefix;
};


/**
 * Check if a country is one of the countries under the North American Numbering
 * Plan Administration (NANPA).
 *
 * @param {string} regionCode the ISO 3166-1 two-letter country code.
 * @return {boolean} true if regionCode is one of the countries under NANPA.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isNANPACountry =
    function(regionCode) {

  return goog.array.contains(
      i18n.phonenumbers.metadata.countryCodeToRegionCodeMap[
          i18n.phonenumbers.PhoneNumberUtil.NANPA_COUNTRY_CODE_],
      regionCode.toUpperCase());
};


/**
 * Check whether countryCode represents the country calling code from a country
 * whose national significant number could contain a leading zero. An example of
 * such a country is Italy.
 *
 * @param {number} countryCode the country calling code.
 * @return {boolean}
 */
i18n.phonenumbers.PhoneNumberUtil.isLeadingZeroCountry = function(countryCode) {
  return countryCode in
      i18n.phonenumbers.PhoneNumberUtil.LEADING_ZERO_COUNTRIES_;
};


/**
 * Convenience wrapper around isPossibleNumberWithReason. Instead of returning
 * the reason for failure, this method returns a boolean value.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the number that needs to be
 *     checked.
 * @return {boolean} true if the number is possible.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isPossibleNumber =
    function(number) {

  return this.isPossibleNumberWithReason(number) ==
      i18n.phonenumbers.PhoneNumberUtil.ValidationResult.IS_POSSIBLE;
};


/**
 * Check whether a phone number is a possible number. It provides a more lenient
 * check than isValidNumber in the following sense:
 *
 * 1. It only checks the length of phone numbers. In particular, it doesn't
 * check starting digits of the number.
 *
 * 2. It doesn't attempt to figure out the type of the number, but uses general
 * rules which applies to all types of phone numbers in a country. Therefore, it
 * is much faster than isValidNumber.
 *
 * 3. For fixed line numbers, many countries have the concept of area code,
 * which together with subscriber number constitute the national significant
 * number. It is sometimes okay to dial the subscriber number only when dialing
 * in the same area. This function will return true if the
 * subscriber-number-only version is passed in. On the other hand, because
 * isValidNumber validates using information on both starting digits (for fixed
 * line numbers, that would most likely be area codes) and length (obviously
 * includes the length of area codes for fixed line numbers), it will return
 * false for the subscriber-number-only version.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the number that needs to be
 *     checked.
 * @return {i18n.phonenumbers.PhoneNumberUtil.ValidationResult} a
 *     ValidationResult object which indicates whether the number is possible.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isPossibleNumberWithReason =
    function(number) {

  /** @type {number} */
  var countryCode = number.getCountryCodeOrDefault();
  // Note: For Russian Fed and NANPA numbers, we just use the rules from the
  // default region (US or Russia) since the getRegionCodeForNumber will not
  // work if the number is possible but not valid. This would need to be
  // revisited if the possible number pattern ever differed between various
  // countries within those plans.
  /** @type {string} */
  var regionCode = this.getRegionCodeForCountryCode(countryCode);
  if (!this.isValidRegionCode_(regionCode)) {
    return i18n.phonenumbers.PhoneNumberUtil.ValidationResult
        .INVALID_COUNTRY_CODE;
  }
  /** @type {string} */
  var nationalNumber =
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);
  /** @type {i18n.phonenumbers.PhoneNumberDesc} */
  var generalNumDesc = this.getMetadataForRegion(regionCode).getGeneralDesc();
  // Handling case of numbers with no metadata.
  if (!generalNumDesc.hasNationalNumberPattern()) {
    /** @type {number} */
    var numberLength = nationalNumber.length;
    if (numberLength < i18n.phonenumbers.PhoneNumberUtil.MIN_LENGTH_FOR_NSN_) {
      return i18n.phonenumbers.PhoneNumberUtil.ValidationResult.TOO_SHORT;
    } else if (numberLength >
               i18n.phonenumbers.PhoneNumberUtil.MAX_LENGTH_FOR_NSN_) {
      return i18n.phonenumbers.PhoneNumberUtil.ValidationResult.TOO_LONG;
    } else {
      return i18n.phonenumbers.PhoneNumberUtil.ValidationResult.IS_POSSIBLE;
    }
  }
  /** @type {string} */
  var possibleNumberPattern =
      generalNumDesc.getPossibleNumberPatternOrDefault();
  /** @type {Array.<string> } */
  var matchedGroups = nationalNumber.match('^' + possibleNumberPattern);
  /** @type {string} */
  var firstGroup = matchedGroups ? matchedGroups[0] : '';
  if (firstGroup.length > 0) {
    return (firstGroup.length == nationalNumber.length) ?
        i18n.phonenumbers.PhoneNumberUtil.ValidationResult.IS_POSSIBLE :
        i18n.phonenumbers.PhoneNumberUtil.ValidationResult.TOO_LONG;
  } else {
    return i18n.phonenumbers.PhoneNumberUtil.ValidationResult.TOO_SHORT;
  }
};


/**
 * Check whether a phone number is a possible number given a number in the form
 * of a string, and the country where the number could be dialed from. It
 * provides a more lenient check than isValidNumber. See
 * isPossibleNumber(number) for details.
 *
 * This method first parses the number, then invokes
 * isPossibleNumber(PhoneNumber number) with the resultant PhoneNumber object.
 *
 * @param {string} number the number that needs to be checked, in the form of a
 *     string.
 * @param {string} countryDialingFrom the ISO 3166-1 two-letter country code
 *     that denotes the country that we are expecting the number to be dialed
 *     from. Note this is different from the country where the number belongs.
 *     For example, the number +1 650 253 0000 is a number that belongs to US.
 *     When written in this form, it could be dialed from any country. When it
 *     is written as 00 1 650 253 0000, it could be dialed from any country
 *     which uses an international dialling prefix of 00. When it is written as
 *     650 253 0000, it could only be dialed from within the US, and when
 *     written as 253 0000, it could only be dialed from within a smaller area
 *     in the US (Mountain View, CA, to be more specific).
 * @return {boolean} true if the number is possible.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isPossibleNumberString =
    function(number, countryDialingFrom) {

  try {
    return this.isPossibleNumber(this.parse(number, countryDialingFrom));
  } catch (e) {
    return false;
  }
};


/**
 * Attempts to extract a valid number from a phone number that is too long to be
 * valid, and resets the PhoneNumber object passed in to that valid version. If
 * no valid number could be extracted, the PhoneNumber object passed in will not
 * be modified.
 * @param {i18n.phonenumbers.PhoneNumber} number a PhoneNumber object which
 *     contains a number that is too long to be valid.
 * @return {boolean} true if a valid phone number can be successfully extracted.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.truncateTooLongNumber =
    function(number) {

  if (this.isValidNumber(number)) {
    return true;
  }
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var numberCopy = number.clone();
  /** @type {number} */
  var nationalNumber = number.getNationalNumberOrDefault();
  do {
    nationalNumber = Math.floor(nationalNumber / 10);
    numberCopy.setNationalNumber(nationalNumber);
    if (nationalNumber == 0 ||
        this.isPossibleNumberWithReason(numberCopy) ==
            i18n.phonenumbers.PhoneNumberUtil.ValidationResult.TOO_SHORT) {
      return false;
    }
  } while (!this.isValidNumber(numberCopy));
  number.setNationalNumber(nationalNumber);
  return true;
};


/**
 * Extracts country code from fullNumber, returns it and places the remaining
 * number in nationalNumber. It assumes that the leading plus sign or IDD has
 * already been removed. Returns 0 if fullNumber doesn't start with a valid
 * country code, and leaves nationalNumber unmodified.
 *
 * @param {!goog.string.StringBuffer} fullNumber
 * @param {!goog.string.StringBuffer} nationalNumber
 * @return {number}
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.extractCountryCode =
    function(fullNumber, nationalNumber) {

  /** @type {string} */
  var fullNumberStr = fullNumber.toString();
  /** @type {number} */
  var potentialCountryCode;
  /** @type {number} */
  var numberLength = fullNumberStr.length;
  for (var i = 1;
      i <= i18n.phonenumbers.PhoneNumberUtil.MAX_LENGTH_COUNTRY_CODE_ &&
      i <= numberLength; ++i) {
    potentialCountryCode = parseInt(fullNumberStr.substring(0, i), 10);
    if (potentialCountryCode in
        i18n.phonenumbers.metadata.countryCodeToRegionCodeMap) {
      nationalNumber.append(fullNumberStr.substring(i));
      return potentialCountryCode;
    }
  }
  return 0;
};


/**
 * Tries to extract a country code from a number. This method will return zero
 * if no country code is considered to be present. Country codes are extracted
 * in the following ways:
 *  - by stripping the international dialing prefix of the country the person is
 * dialing from, if this is present in the number, and looking at the next
 * digits
 *  - by stripping the '+' sign if present and then looking at the next digits
 *  - by comparing the start of the number and the country code of the default
 * region. If the number is not considered possible for the numbering plan of
 * the default region initially, but starts with the country code of this
 * region, validation will be reattempted after stripping this country code. If
 * this number is considered a possible number, then the first digits will be
 * considered the country code and removed as such.
 *
 * It will throw a i18n.phonenumbers.Error if the number starts with a '+' but
 * the country code supplied after this does not match that of any known
 * country.
 *
 * @param {string} number non-normalized telephone number that we wish to
 *     extract a country code from - may begin with '+'.
 * @param {i18n.phonenumbers.PhoneMetadata} defaultRegionMetadata metadata
 *     about the region this number may be from.
 * @param {!goog.string.StringBuffer} nationalNumber a string buffer to store
 *     the national significant number in, in the case that a country code was
 *     extracted. The number is appended to any existing contents. If no country
 *     code was extracted, this will be left unchanged.
 * @param {boolean} keepRawInput true if the country_code_source and
 *     preferred_carrier_code fields of phoneNumber should be populated.
 *     of phoneNumber should be populated.
 * @param {i18n.phonenumbers.PhoneNumber} phoneNumber the PhoneNumber object
 *     that needs to be populated with country code and country code source.
 *     Note the country code is always populated, whereas country code source is
 *     only populated when keepCountryCodeSource is true.
 * @return {number} the country code extracted or 0 if none could be extracted.
 * @throws {i18n.phonenumbers.Error}
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.maybeExtractCountryCode =
    function(number, defaultRegionMetadata, nationalNumber,
             keepRawInput, phoneNumber) {

  if (number.length == 0) {
    return 0;
  }
  /** @type {!goog.string.StringBuffer} */
  var fullNumber = new goog.string.StringBuffer(number);
  // Set the default prefix to be something that will never match.
  /** @type {?string} */
  var possibleCountryIddPrefix;
  if (defaultRegionMetadata != null) {
    possibleCountryIddPrefix = defaultRegionMetadata.getInternationalPrefix();
  }
  if (possibleCountryIddPrefix == null) {
    possibleCountryIddPrefix = 'NonMatch';
  }

  /** @type {i18n.phonenumbers.PhoneNumber.CountryCodeSource} */
  var countryCodeSource = this.maybeStripInternationalPrefixAndNormalize(
      fullNumber, possibleCountryIddPrefix);
  if (keepRawInput) {
    phoneNumber.setCountryCodeSource(countryCodeSource);
  }
  if (countryCodeSource !=
      i18n.phonenumbers.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY) {
    if (fullNumber.getLength() <
        i18n.phonenumbers.PhoneNumberUtil.MIN_LENGTH_FOR_NSN_) {
      throw i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD;
    }
    /** @type {number} */
    var potentialCountryCode = this.extractCountryCode(fullNumber,
                                                       nationalNumber);
    if (potentialCountryCode != 0) {
      phoneNumber.setCountryCode(potentialCountryCode);
      return potentialCountryCode;
    }

    // If this fails, they must be using a strange country code that we don't
    // recognize, or that doesn't exist.
    throw i18n.phonenumbers.Error.INVALID_COUNTRY_CODE;
  } else if (defaultRegionMetadata != null) {
    // Check to see if the number is valid for the default region already. If
    // not, we check to see if the country code for the default region is
    // present at the start of the number.
    /** @type {i18n.phonenumbers.PhoneNumberDesc} */
    var generalDesc = defaultRegionMetadata.getGeneralDesc();
    /** @type {RegExp} */
    var validNumberPattern = new RegExp(generalDesc.getNationalNumberPattern());
    if (!i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(
        validNumberPattern, fullNumber.toString())) {
      /** @type {number} */
      var defaultCountryCode = defaultRegionMetadata.getCountryCodeOrDefault();
      /** @type {string} */
      var defaultCountryCodeString = '' + defaultCountryCode;
      /** @type {string} */
      var normalizedNumber = fullNumber.toString();
      if (goog.string.startsWith(normalizedNumber, defaultCountryCodeString)) {
        // If so, strip this, and see if the resultant number is valid.
        /** @type {!goog.string.StringBuffer} */
        var potentialNationalNumber = new goog.string.StringBuffer(
            normalizedNumber.substring(defaultCountryCodeString.length));
        this.maybeStripNationalPrefixAndCarrierCode(
            potentialNationalNumber, defaultRegionMetadata);
        /** @type {string} */
        var potentialNationalNumberStr = potentialNationalNumber.toString();
        /** @type {Array.<string> } */
        var matchedGroups = potentialNationalNumberStr.match(
            '^' + generalDesc.getPossibleNumberPattern());
        /** @type {number} */
        var possibleNumberMatchedLength = matchedGroups &&
            matchedGroups[0] != null && matchedGroups[0].length || 0;
        // If the resultant number is either valid, or still too long even with
        // the country code stripped, we consider this a better result and keep
        // the potential national number.
        if (i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(
            validNumberPattern, potentialNationalNumberStr) ||
            possibleNumberMatchedLength > 0 &&
            possibleNumberMatchedLength != potentialNationalNumberStr.length) {
          nationalNumber.append(potentialNationalNumberStr);
          if (keepRawInput) {
            phoneNumber.setCountryCodeSource(
                i18n.phonenumbers.PhoneNumber.CountryCodeSource
                    .FROM_NUMBER_WITHOUT_PLUS_SIGN);
          }
          phoneNumber.setCountryCode(defaultCountryCode);
          return defaultCountryCode;
        }
      }
    }
  }
  // No country code present.
  phoneNumber.setCountryCode(0);
  return 0;
};


/**
 * Strips the IDD from the start of the number if present. Helper function used
 * by maybeStripInternationalPrefixAndNormalize.
 *
 * @param {RegExp} iddPattern the regular expression for the international
 *     prefix.
 * @param {!goog.string.StringBuffer} number the phone number that we wish to
 *     strip any international dialing prefix from.
 * @return {boolean} true if an international prefix was present.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.parsePrefixAsIdd_ =
    function(iddPattern, number) {

  /** @type {string} */
  var numberStr = number.toString();
  if (numberStr.search(iddPattern) == 0) {
    /** @type {number} */
    var matchEnd = numberStr.match(iddPattern)[0].length;
    /** @type {Array.<string> } */
    var matchedGroups = numberStr.substring(matchEnd).match(
        i18n.phonenumbers.PhoneNumberUtil.CAPTURING_DIGIT_PATTERN_);
    if (matchedGroups && matchedGroups[1] != null &&
        matchedGroups[1].length > 0) {
      /** @type {string} */
      var normalizedGroup = i18n.phonenumbers.PhoneNumberUtil.normalizeHelper_(
          matchedGroups[1], i18n.phonenumbers.PhoneNumberUtil.DIGIT_MAPPINGS,
          true);
      if (normalizedGroup == '0') {
        return false;
      }
    }
    number.clear();
    number.append(numberStr.substring(matchEnd));
    return true;
  }
  return false;
};


/**
 * Strips any international prefix (such as +, 00, 011) present in the number
 * provided, normalizes the resulting number, and indicates if an international
 * prefix was present.
 *
 * @param {!goog.string.StringBuffer} number the non-normalized telephone number
 *     that we wish to strip any international dialing prefix from.
 * @param {string} possibleIddPrefix the international direct dialing prefix
 *     from the country we think this number may be dialed in.
 * @return {i18n.phonenumbers.PhoneNumber.CountryCodeSource} the corresponding
 *     CountryCodeSource if an international dialing prefix could be removed
 *     from the number, otherwise CountryCodeSource.FROM_DEFAULT_COUNTRY if
 *     the number did not seem to be in international format.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.
    maybeStripInternationalPrefixAndNormalize = function(number,
                                                         possibleIddPrefix) {
  /** @type {string} */
  var numberStr = number.toString();
  if (numberStr.length == 0) {
    return i18n.phonenumbers.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY;
  }
  // Check to see if the number begins with one or more plus signs.
  if (i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_PATTERN_.test(numberStr)) {
    numberStr = numberStr.replace(
        i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_PATTERN_, '');
    // Can now normalize the rest of the number since we've consumed the "+"
    // sign at the start.
    number.clear();
    number.append(i18n.phonenumbers.PhoneNumberUtil.normalize(numberStr));
    return i18n.phonenumbers.PhoneNumber.CountryCodeSource
        .FROM_NUMBER_WITH_PLUS_SIGN;
  }
  // Attempt to parse the first digits as an international prefix.
  /** @type {RegExp} */
  var iddPattern = new RegExp(possibleIddPrefix);
  if (this.parsePrefixAsIdd_(iddPattern, number)) {
    i18n.phonenumbers.PhoneNumberUtil.normalizeSB_(number);
    return i18n.phonenumbers.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_IDD;
  }
  // If still not found, then try and normalize the number and then try again.
  // This shouldn't be done before, since non-numeric characters (+ and ~) may
  // legally be in the international prefix.
  i18n.phonenumbers.PhoneNumberUtil.normalizeSB_(number);
  return this.parsePrefixAsIdd_(iddPattern, number) ?
      i18n.phonenumbers.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_IDD :
      i18n.phonenumbers.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY;
};


/**
 * Strips any national prefix (such as 0, 1) present in the number provided.
 *
 * @param {!goog.string.StringBuffer} number the normalized telephone number
 *     that we wish to strip any national dialing prefix from.
 * @param {i18n.phonenumbers.PhoneMetadata} metadata the metadata for the
 *     country that we think this number is from.
 * @return {string} the carrier code extracted if it is present, otherwise
 *     return an empty string.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.
    maybeStripNationalPrefixAndCarrierCode = function(number, metadata) {

  /** @type {string} */
  var carrierCode = '';
  /** @type {string} */
  var numberStr = number.toString();
  /** @type {number} */
  var numberLength = numberStr.length;
  /** @type {?string} */
  var possibleNationalPrefix = metadata.getNationalPrefixForParsing();
  if (numberLength == 0 || possibleNationalPrefix == null ||
      possibleNationalPrefix.length == 0) {
    // Early return for numbers of zero length.
    return carrierCode;
  }
  // Attempt to parse the first digits as a national prefix.
  /** @type {RegExp} */
  var prefixPattern = new RegExp('^' + possibleNationalPrefix);
  /** @type {Array.<string>} */
  var prefixMatcher = prefixPattern.exec(numberStr);
  if (prefixMatcher) {
    /** @type {RegExp} */
    var nationalNumberRule = new RegExp(
        metadata.getGeneralDesc().getNationalNumberPattern());
    // prefixMatcher[numOfGroups] == null implies nothing was captured by the
    // capturing groups in possibleNationalPrefix; therefore, no transformation
    // is necessary, and we just remove the national prefix.
    /** @type {number} */
    var numOfGroups = prefixMatcher.length - 1;
    /** @type {?string} */
    var transformRule = metadata.getNationalPrefixTransformRule();
    /** @type {string} */
    var transformedNumber;
    if (transformRule == null || transformRule.length == 0 ||
        prefixMatcher[numOfGroups] == null ||
        prefixMatcher[numOfGroups].length == 0) {
      transformedNumber = numberStr.substring(prefixMatcher[0].length);
    } else {
      transformedNumber = numberStr.replace(prefixPattern, transformRule);
    }
    // Check that the resultant number is viable. If not, return.
    if (!i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_(nationalNumberRule,
        transformedNumber)) {
      return carrierCode;
    }
    if (numOfGroups > 0) {
      carrierCode = prefixMatcher[1];
    }
    number.clear();
    number.append(transformedNumber);
  }
  return carrierCode;
};


/**
 * Strips any extension (as in, the part of the number dialled after the call is
 * connected, usually indicated with extn, ext, x or similar) from the end of
 * the number, and returns it.
 *
 * @param {!goog.string.StringBuffer} number the non-normalized telephone number
 *     that we wish to strip the extension from.
 * @return {string} the phone extension.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.maybeStripExtension =
    function(number) {

  /** @type {string} */
  var numberStr = number.toString();
  /** @type {number} */
  var mStart =
      numberStr.search(i18n.phonenumbers.PhoneNumberUtil.EXTN_PATTERN_);
  // If we find a potential extension, and the number preceding this is a viable
  // number, we assume it is an extension.
  if (mStart >= 0 && i18n.phonenumbers.PhoneNumberUtil.isViablePhoneNumber(
      numberStr.substring(0, mStart))) {
    // The numbers are captured into groups in the regular expression.
    /** @type {Array.<string>} */
    var matchedGroups =
        numberStr.match(i18n.phonenumbers.PhoneNumberUtil.EXTN_PATTERN_);
    /** @type {number} */
    var matchedGroupsLength = matchedGroups.length;
    for (var i = 1; i < matchedGroupsLength; ++i) {
      if (matchedGroups[i] != null && matchedGroups[i].length > 0) {
        number.clear();
        number.append(numberStr.substring(0, mStart));
        return matchedGroups[i];
      }
    }
  }
  return '';
};


/**
 * Checks to see that the region code used is valid, or if it is not valid, that
 * the number to parse starts with a + symbol so that we can attempt to infer
 * the country from the number.
 * @param {string} numberToParse number that we are attempting to parse.
 * @param {?string} defaultCountry the ISO 3166-1 two-letter country code that
 *     denotes the country that we are expecting the number to be from.
 * @return {boolean} false if it cannot use the region provided and the region
 *     cannot be inferred.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.checkRegionForParsing_ = function(
    numberToParse, defaultCountry) {
  // If the number is null or empty, we can't guess the country code.
  return this.isValidRegionCode_(defaultCountry) ||
      (numberToParse != null && numberToParse.length > 0 &&
          i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_PATTERN_.test(
              numberToParse));
};


/**
 * Parses a string and returns it in proto buffer format. This method will throw
 * a i18n.phonenumbers.Error if the number is not considered to be a possible
 * number. Note that validation of whether the number is actually a valid number
 * for a particular country/region is not performed. This can be done separately
 * with isValidNumber.
 *
 * @param {string} numberToParse number that we are attempting to parse. This
 *     can contain formatting such as +, ( and -, as well as a phone number
 *     extension.
 * @param {?string} defaultCountry the ISO 3166-1 two-letter country code that
 *     denotes the country that we are expecting the number to be from. This is
 *     only used if the number being parsed is not written in international
 *     format. The country code for the number in this case would be stored as
 *     that of the default country supplied.  If the number is guaranteed to
 *     start with a '+' followed by the country code, then 'ZZ' or null can be
 *     supplied.
 * @return {i18n.phonenumbers.PhoneNumber} a phone number proto buffer filled
 *     with the parsed number.
 * @throws {i18n.phonenumbers.Error} if the string is not considered to be a
 *     viable phone number or if no default country was supplied and the number
 *     is not in international format (does not start with +).
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.parse = function(numberToParse,
                                                             defaultCountry) {
  return this.parseHelper_(numberToParse, defaultCountry, false, true);
};


/**
 * Parses a string and returns it in proto buffer format. This method differs
 * from parse() in that it always populates the raw_input field of the protocol
 * buffer with numberToParse as well as the country_code_source field.
 *
 * @param {string} numberToParse number that we are attempting to parse. This
 *     can contain formatting such as +, ( and -, as well as a phone number
 *     extension.
 * @param {?string} defaultCountry the ISO 3166-1 two-letter country code that
 *     denotes the country that we are expecting the number to be from. This is
 *     only used if the number being parsed is not written in international
 *     format. The country code for the number in this case would be stored as
 *     that of the default country supplied.
 * @return {i18n.phonenumbers.PhoneNumber} a phone number proto buffer filled
 *     with the parsed number.
 * @throws {i18n.phonenumbers.Error} if the string is not considered to be a
 *     viable phone number or if no default country was supplied and the number
 *     is not in international format (does not start with +).
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.parseAndKeepRawInput =
    function(numberToParse, defaultCountry) {

  if (!this.isValidRegionCode_(defaultCountry)) {
    if (numberToParse.length > 0 && numberToParse.charAt(0) !=
        i18n.phonenumbers.PhoneNumberUtil.PLUS_SIGN) {
      throw i18n.phonenumbers.Error.INVALID_COUNTRY_CODE;
    }
  }
  return this.parseHelper_(numberToParse, defaultCountry, true, true);
};


/**
 * Parses a string and returns it in proto buffer format. This method is the
 * same as the public parse() method, with the exception that it allows the
 * default country to be null, for use by isNumberMatch().
 *
 * @param {string} numberToParse number that we are attempting to parse. This
 *     can contain formatting such as +, ( and -, as well as a phone number
 *     extension.
 * @param {?string} defaultCountry the ISO 3166-1 two-letter country code that
 *     denotes the country that we are expecting the number to be from. This is
 *     only used if the number being parsed is not written in international
 *     format. The country code for the number in this case would be stored as
 *     that of the default country supplied.
 * @param {boolean} keepRawInput whether to populate the raw_input field of the
 *     phoneNumber with numberToParse.
 * @param {boolean} checkRegion should be set to false if it is permitted for
 *     the default country to be null or unknown ('ZZ').
 * @return {i18n.phonenumbers.PhoneNumber} a phone number proto buffer filled
 *     with the parsed number.
 * @throws {i18n.phonenumbers.Error}
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.parseHelper_ =
    function(numberToParse, defaultCountry, keepRawInput, checkRegion) {

  if (numberToParse == null) {
    throw i18n.phonenumbers.Error.NOT_A_NUMBER;
  }
  // Extract a possible number from the string passed in (this strips leading
  // characters that could not be the start of a phone number.)
  /** @type {string} */
  var number =
      i18n.phonenumbers.PhoneNumberUtil.extractPossibleNumber(numberToParse);
  if (!i18n.phonenumbers.PhoneNumberUtil.isViablePhoneNumber(number)) {
    throw i18n.phonenumbers.Error.NOT_A_NUMBER;
  }

  // Check the country supplied is valid, or that the extracted number starts
  // with some sort of + sign so the number's region can be determined.
  if (checkRegion && !this.checkRegionForParsing_(number, defaultCountry)) {
    throw i18n.phonenumbers.Error.INVALID_COUNTRY_CODE;
  }

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var phoneNumber = new i18n.phonenumbers.PhoneNumber();
  if (keepRawInput) {
    phoneNumber.setRawInput(numberToParse);
  }
  /** @type {!goog.string.StringBuffer} */
  var nationalNumber = new goog.string.StringBuffer(number);
  // Attempt to parse extension first, since it doesn't require
  // country-specific data and we want to have the non-normalised number here.
  /** @type {string} */
  var extension = this.maybeStripExtension(nationalNumber);
  if (extension.length > 0) {
    phoneNumber.setExtension(extension);
  }

  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var countryMetadata = this.getMetadataForRegion(defaultCountry);
  // Check to see if the number is given in international format so we know
  // whether this number is from the default country or not.
  /** @type {!goog.string.StringBuffer} */
  var normalizedNationalNumber = new goog.string.StringBuffer();
  /** @type {number} */
  var countryCode = this.maybeExtractCountryCode(nationalNumber.toString(),
      countryMetadata, normalizedNationalNumber, keepRawInput, phoneNumber);
  if (countryCode != 0) {
    /** @type {string} */
    var phoneNumberRegion = this.getRegionCodeForCountryCode(countryCode);
    if (phoneNumberRegion != defaultCountry) {
      countryMetadata = this.getMetadataForRegion(phoneNumberRegion);
    }
  } else {
    // If no extracted country code, use the region supplied instead. The
    // national number is just the normalized version of the number we were
    // given to parse.
    i18n.phonenumbers.PhoneNumberUtil.normalizeSB_(nationalNumber);
    normalizedNationalNumber.append(nationalNumber.toString());
    if (defaultCountry != null) {
      countryCode = countryMetadata.getCountryCodeOrDefault();
      phoneNumber.setCountryCode(countryCode);
    } else if (keepRawInput) {
      phoneNumber.clearCountryCodeSource();
    }
  }
  if (normalizedNationalNumber.getLength() <
      i18n.phonenumbers.PhoneNumberUtil.MIN_LENGTH_FOR_NSN_) {
    throw i18n.phonenumbers.Error.TOO_SHORT_NSN;
  }

  if (countryMetadata != null) {
    /** @type {string} */
    var carrierCode = this.maybeStripNationalPrefixAndCarrierCode(
        normalizedNationalNumber, countryMetadata);
    if (keepRawInput) {
      phoneNumber.setPreferredDomesticCarrierCode(carrierCode);
    }
  }
  /** @type {string} */
  var normalizedNationalNumberStr = normalizedNationalNumber.toString();
  /** @type {number} */
  var lengthOfNationalNumber = normalizedNationalNumberStr.length;
  if (lengthOfNationalNumber <
      i18n.phonenumbers.PhoneNumberUtil.MIN_LENGTH_FOR_NSN_) {
    throw i18n.phonenumbers.Error.TOO_SHORT_NSN;
  }
  if (lengthOfNationalNumber >
      i18n.phonenumbers.PhoneNumberUtil.MAX_LENGTH_FOR_NSN_) {
    throw i18n.phonenumbers.Error.TOO_LONG;
  }
  if (normalizedNationalNumberStr.charAt(0) == '0' &&
      i18n.phonenumbers.PhoneNumberUtil.isLeadingZeroCountry(countryCode)) {
    phoneNumber.setItalianLeadingZero(true);
  }
  phoneNumber.setNationalNumber(parseInt(normalizedNationalNumberStr, 10));
  return phoneNumber;
};


/**
 * Takes two phone numbers and compares them for equality.
 *
 * Returns EXACT_MATCH if the country code, NSN, presence of a leading zero for
 * Italian numbers and any extension present are the same. Returns NSN_MATCH if
 * either or both has no country specified, and the NSNs and extensions are the
 * same. Returns SHORT_NSN_MATCH if either or both has no country specified, or
 * the country specified is the same, and one NSN could be a shorter version of
 * the other number. This includes the case where one has an extension
 * specified, and the other does not. Returns NO_MATCH otherwise. For example,
 * the numbers +1 345 657 1234 and 657 1234 are a SHORT_NSN_MATCH. The numbers
 * +1 345 657 1234 and 345 657 are a NO_MATCH.
 *
 * @param {i18n.phonenumbers.PhoneNumber|string} firstNumberIn first number to
 *     compare. If it is a string it can contain formatting, and can have
 *     country code specified with + at the start.
 * @param {i18n.phonenumbers.PhoneNumber|string} secondNumberIn second number to
 *     compare. If it is a string it can contain formatting, and can have
 *     country code specified with + at the start.
 * @return {i18n.phonenumbers.PhoneNumberUtil.MatchType} NOT_A_NUMBER, NO_MATCH,
 *     SHORT_NSN_MATCH, NSN_MATCH or EXACT_MATCH depending on the level of
 *     equality of the two numbers, described in the method definition.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isNumberMatch =
    function(firstNumberIn, secondNumberIn) {

  // If the input arguements are strings parse them to a proto buffer format.
  // Else make copies of the phone numbers so that the numbers passed in are not
  // edited.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var firstNumber;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var secondNumber;
  if (typeof firstNumberIn == 'string') {
    // First see if the first number has an implicit country code, by attempting
    // to parse it.
    try {
      firstNumber = this.parse(
          firstNumberIn, i18n.phonenumbers.PhoneNumberUtil.UNKNOWN_REGION_);
    } catch (e) {
      if (e != i18n.phonenumbers.Error.INVALID_COUNTRY_CODE) {
        return i18n.phonenumbers.PhoneNumberUtil.MatchType.NOT_A_NUMBER;
      }
      // The first number has no country code. EXACT_MATCH is no longer
      // possible. We parse it as if the region was the same as that for the
      // second number, and if EXACT_MATCH is returned, we replace this with
      // NSN_MATCH.
      if (typeof secondNumberIn != 'string') {
        /** @type {string} */
        var secondNumberRegion = this.getRegionCodeForCountryCode(
            secondNumberIn.getCountryCodeOrDefault());
        if (secondNumberRegion !=
            i18n.phonenumbers.PhoneNumberUtil.UNKNOWN_REGION_) {
          try {
            firstNumber = this.parse(firstNumberIn, secondNumberRegion);
          } catch (e2) {
            return i18n.phonenumbers.PhoneNumberUtil.MatchType.NOT_A_NUMBER;
          }
          /** @type {i18n.phonenumbers.PhoneNumberUtil.MatchType} */
          var match = this.isNumberMatch(firstNumber, secondNumberIn);
          if (match ==
              i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH) {
            return i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH;
          }
          return match;
        }
      }
      // If the second number is a string or doesn't have a valid country code,
      // we parse the first number without country code.
      try {
        firstNumber = this.parseHelper_(firstNumberIn, null, false, false);
      } catch (e2) {
        return i18n.phonenumbers.PhoneNumberUtil.MatchType.NOT_A_NUMBER;
      }
    }
  } else {
    firstNumber = firstNumberIn.clone();
  }
  if (typeof secondNumberIn == 'string') {
    try {
      secondNumber = this.parse(
          secondNumberIn, i18n.phonenumbers.PhoneNumberUtil.UNKNOWN_REGION_);
      return this.isNumberMatch(firstNumberIn, secondNumber);
    } catch (e) {
      if (e != i18n.phonenumbers.Error.INVALID_COUNTRY_CODE) {
        return i18n.phonenumbers.PhoneNumberUtil.MatchType.NOT_A_NUMBER;
      }
      return this.isNumberMatch(secondNumberIn, firstNumber);
    }
  } else {
    secondNumber = secondNumberIn.clone();
  }
  // First clear raw_input, country_code_source and
  // preferred_domestic_carrier_code fields and any empty-string extensions so
  // that we can use the proto-buffer equality method.
  firstNumber.clearRawInput();
  firstNumber.clearCountryCodeSource();
  firstNumber.clearPreferredDomesticCarrierCode();
  secondNumber.clearRawInput();
  secondNumber.clearCountryCodeSource();
  secondNumber.clearPreferredDomesticCarrierCode();
  if (firstNumber.hasExtension() && firstNumber.getExtension().length == 0) {
    firstNumber.clearExtension();
  }
  if (secondNumber.hasExtension() && secondNumber.getExtension().length == 0) {
    secondNumber.clearExtension();
  }

  // Early exit if both had extensions and these are different.
  if (firstNumber.hasExtension() && secondNumber.hasExtension() &&
      firstNumber.getExtension() != secondNumber.getExtension()) {
    return i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH;
  }
  /** @type {number} */
  var firstNumberCountryCode = firstNumber.getCountryCodeOrDefault();
  /** @type {number} */
  var secondNumberCountryCode = secondNumber.getCountryCodeOrDefault();
  // Both had country code specified.
  if (firstNumberCountryCode != 0 && secondNumberCountryCode != 0) {
    if (firstNumber.equals(secondNumber)) {
      return i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH;
    } else if (firstNumberCountryCode == secondNumberCountryCode &&
        this.isNationalNumberSuffixOfTheOther_(firstNumber, secondNumber)) {
      // A SHORT_NSN_MATCH occurs if there is a difference because of the
      // presence or absence of an 'Italian leading zero', the presence or
      // absence of an extension, or one NSN being a shorter variant of the
      // other.
      return i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH;
    }
    // This is not a match.
    return i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH;
  }
  // Checks cases where one or both country codes were not specified. To make
  // equality checks easier, we first set the country codes to be equal.
  firstNumber.setCountryCode(0);
  secondNumber.setCountryCode(0);
  // If all else was the same, then this is an NSN_MATCH.
  if (firstNumber.equals(secondNumber)) {
    return i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH;
  }
  if (this.isNationalNumberSuffixOfTheOther_(firstNumber, secondNumber)) {
    return i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH;
  }
  return i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH;
};


/**
 * Returns true when one national number is the suffix of the other or both are
 * the same.
 *
 * @param {i18n.phonenumbers.PhoneNumber} firstNumber the first PhoneNumber
 *     object.
 * @param {i18n.phonenumbers.PhoneNumber} secondNumber the second PhoneNumber
 *     object.
 * @return {boolean} true if one PhoneNumber is the suffix of the other one.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.isNationalNumberSuffixOfTheOther_ =
    function(firstNumber, secondNumber) {

  /** @type {string} */
  var firstNumberNationalNumber = '' + firstNumber.getNationalNumber();
  /** @type {string} */
  var secondNumberNationalNumber = '' + secondNumber.getNationalNumber();
  // Note that endsWith returns true if the numbers are equal.
  return goog.string.endsWith(firstNumberNationalNumber,
                              secondNumberNationalNumber) ||
         goog.string.endsWith(secondNumberNationalNumber,
                              firstNumberNationalNumber);
};


/**
 * Returns true if the number can only be dialled from within the country. If
 * unknown, or the number can be dialled from outside the country as well,
 * returns false. Does not check the number is a valid number.
 * TODO: Make this method public when we have enough metadata to make it
 * worthwhile. Currently visible for testing purposes only.
 *
 * @param {i18n.phonenumbers.PhoneNumber} number the phone-number for which we
 *     want to know whether it is only diallable from within the country.
 * @return {boolean} true if the number can only be dialled from within the
 *     country.
 */
i18n.phonenumbers.PhoneNumberUtil.prototype.canBeInternationallyDialled =
    function(number) {
  /** @type {?string} */
  var regionCode = this.getRegionCodeForNumber(number);
  /** @type {string} */
  var nationalSignificantNumber =
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number);
  if (!this.isValidRegionCode_(regionCode)) {
    return true;
  }
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = this.getMetadataForRegion(regionCode);
  return !this.isNumberMatchingDesc_(nationalSignificantNumber,
                                     metadata.getNoInternationalDialling());
};


/**
 * Check whether the entire input sequence can be matched against the regular
 * expression.
 *
 * @param {RegExp|string} regex the regular expression to match against.
 * @param {string} str the string to test.
 * @return {boolean} true if str can be matched entirely against regex.
 * @private
 */
i18n.phonenumbers.PhoneNumberUtil.matchesEntirely_ = function(regex, str) {
  /** @type {Array.<string>} */
  var matchedGroups = str.match(regex);
  if (matchedGroups && matchedGroups[0].length == str.length) {
    return true;
  }
  return false;
};
