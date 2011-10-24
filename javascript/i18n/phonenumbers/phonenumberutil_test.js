/**
 * @license
 * Copyright (C) 2010 The Libphonenumber Authors
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
 * @fileoverview  Unit tests for the PhoneNumberUtil.
 *
 * Note that these tests use the metadata contained in metadatafortesting.js,
 * not the normal metadata files, so should not be used for regression test
 * purposes - these tests are illustrative only and test functionality.
 *
 * @author Nikolaos Trogkanis
 */

goog.require('goog.testing.jsunit');
goog.require('i18n.phonenumbers.PhoneNumberUtil');
goog.require('i18n.phonenumbers.RegionCode');


/** @type {i18n.phonenumbers.PhoneNumberUtil} */
var phoneUtil = i18n.phonenumbers.PhoneNumberUtil.getInstance();


// Set up some test numbers to re-use.
/** @type {i18n.phonenumbers.PhoneNumber} */
var ALPHA_NUMERIC_NUMBER = new i18n.phonenumbers.PhoneNumber();
ALPHA_NUMERIC_NUMBER.setCountryCode(1);
ALPHA_NUMERIC_NUMBER.setNationalNumber(80074935247);


/** @type {i18n.phonenumbers.PhoneNumber} */
var AR_MOBILE = new i18n.phonenumbers.PhoneNumber();
AR_MOBILE.setCountryCode(54);
AR_MOBILE.setNationalNumber(91187654321);


/** @type {i18n.phonenumbers.PhoneNumber} */
var AR_NUMBER = new i18n.phonenumbers.PhoneNumber();
AR_NUMBER.setCountryCode(54);
AR_NUMBER.setNationalNumber(1187654321);


/** @type {i18n.phonenumbers.PhoneNumber} */
var AU_NUMBER = new i18n.phonenumbers.PhoneNumber();
AU_NUMBER.setCountryCode(61);
AU_NUMBER.setNationalNumber(236618300);


/** @type {i18n.phonenumbers.PhoneNumber} */
var BS_MOBILE = new i18n.phonenumbers.PhoneNumber();
BS_MOBILE.setCountryCode(1);
BS_MOBILE.setNationalNumber(2423570000);


/** @type {i18n.phonenumbers.PhoneNumber} */
var BS_NUMBER = new i18n.phonenumbers.PhoneNumber();
BS_NUMBER.setCountryCode(1);
BS_NUMBER.setNationalNumber(2423651234);


// Note that this is the same as the example number for DE in the metadata.
/** @type {i18n.phonenumbers.PhoneNumber} */
var DE_NUMBER = new i18n.phonenumbers.PhoneNumber();
DE_NUMBER.setCountryCode(49);
DE_NUMBER.setNationalNumber(30123456);


/** @type {i18n.phonenumbers.PhoneNumber} */
var DE_SHORT_NUMBER = new i18n.phonenumbers.PhoneNumber();
DE_SHORT_NUMBER.setCountryCode(49);
DE_SHORT_NUMBER.setNationalNumber(1234);


/** @type {i18n.phonenumbers.PhoneNumber} */
var GB_MOBILE = new i18n.phonenumbers.PhoneNumber();
GB_MOBILE.setCountryCode(44);
GB_MOBILE.setNationalNumber(7912345678);


/** @type {i18n.phonenumbers.PhoneNumber} */
var GB_NUMBER = new i18n.phonenumbers.PhoneNumber();
GB_NUMBER.setCountryCode(44);
GB_NUMBER.setNationalNumber(2070313000);


/** @type {i18n.phonenumbers.PhoneNumber} */
var IT_MOBILE = new i18n.phonenumbers.PhoneNumber();
IT_MOBILE.setCountryCode(39);
IT_MOBILE.setNationalNumber(345678901);


/** @type {i18n.phonenumbers.PhoneNumber} */
var IT_NUMBER = new i18n.phonenumbers.PhoneNumber();
IT_NUMBER.setCountryCode(39);
IT_NUMBER.setNationalNumber(236618300);
IT_NUMBER.setItalianLeadingZero(true);


// Numbers to test the formatting rules from Mexico.
/** @type {i18n.phonenumbers.PhoneNumber} */
var MX_MOBILE1 = new i18n.phonenumbers.PhoneNumber();
MX_MOBILE1.setCountryCode(52);
MX_MOBILE1.setNationalNumber(12345678900);


/** @type {i18n.phonenumbers.PhoneNumber} */
var MX_MOBILE2 = new i18n.phonenumbers.PhoneNumber();
MX_MOBILE2.setCountryCode(52);
MX_MOBILE2.setNationalNumber(15512345678);


/** @type {i18n.phonenumbers.PhoneNumber} */
var MX_NUMBER1 = new i18n.phonenumbers.PhoneNumber();
MX_NUMBER1.setCountryCode(52);
MX_NUMBER1.setNationalNumber(3312345678);


/** @type {i18n.phonenumbers.PhoneNumber} */
var MX_NUMBER2 = new i18n.phonenumbers.PhoneNumber();
MX_NUMBER2.setCountryCode(52);
MX_NUMBER2.setNationalNumber(8211234567);


/** @type {i18n.phonenumbers.PhoneNumber} */
var NZ_NUMBER = new i18n.phonenumbers.PhoneNumber();
NZ_NUMBER.setCountryCode(64);
NZ_NUMBER.setNationalNumber(33316005);


/** @type {i18n.phonenumbers.PhoneNumber} */
var SG_NUMBER = new i18n.phonenumbers.PhoneNumber();
SG_NUMBER.setCountryCode(65);
SG_NUMBER.setNationalNumber(65218000);


// A too-long and hence invalid US number.
/** @type {i18n.phonenumbers.PhoneNumber} */
var US_LONG_NUMBER = new i18n.phonenumbers.PhoneNumber();
US_LONG_NUMBER.setCountryCode(1);
US_LONG_NUMBER.setNationalNumber(65025300001);


/** @type {i18n.phonenumbers.PhoneNumber} */
var US_NUMBER = new i18n.phonenumbers.PhoneNumber();
US_NUMBER.setCountryCode(1);
US_NUMBER.setNationalNumber(6502530000);


/** @type {i18n.phonenumbers.PhoneNumber} */
var US_PREMIUM = new i18n.phonenumbers.PhoneNumber();
US_PREMIUM.setCountryCode(1);
US_PREMIUM.setNationalNumber(9002530000);


// Too short, but still possible US numbers.
/** @type {i18n.phonenumbers.PhoneNumber} */
var US_LOCAL_NUMBER = new i18n.phonenumbers.PhoneNumber();
US_LOCAL_NUMBER.setCountryCode(1);
US_LOCAL_NUMBER.setNationalNumber(2530000);


/** @type {i18n.phonenumbers.PhoneNumber} */
var US_SHORT_BY_ONE_NUMBER = new i18n.phonenumbers.PhoneNumber();
US_SHORT_BY_ONE_NUMBER.setCountryCode(1);
US_SHORT_BY_ONE_NUMBER.setNationalNumber(650253000);


/** @type {i18n.phonenumbers.PhoneNumber} */
var US_TOLLFREE = new i18n.phonenumbers.PhoneNumber();
US_TOLLFREE.setCountryCode(1);
US_TOLLFREE.setNationalNumber(8002530000);


/** @type {i18n.phonenumbers.PhoneNumber} */
var US_SPOOF = new i18n.phonenumbers.PhoneNumber();
US_SPOOF.setCountryCode(1);
US_SPOOF.setNationalNumber(0);


/** @type {i18n.phonenumbers.PhoneNumber} */
var US_SPOOF_WITH_RAW_INPUT = new i18n.phonenumbers.PhoneNumber();
US_SPOOF_WITH_RAW_INPUT.setCountryCode(1);
US_SPOOF_WITH_RAW_INPUT.setNationalNumber(0);
US_SPOOF_WITH_RAW_INPUT.setRawInput('000-000-0000');

var RegionCode = i18n.phonenumbers.RegionCode;

function testGetInstanceLoadUSMetadata() {
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = phoneUtil.getMetadataForRegion(RegionCode.US);
  assertEquals(RegionCode.US, metadata.getId());
  assertEquals(1, metadata.getCountryCode());
  assertEquals('011', metadata.getInternationalPrefix());
  assertTrue(metadata.hasNationalPrefix());
  assertEquals(2, metadata.numberFormatCount());
  assertEquals('(\\d{3})(\\d{3})(\\d{4})',
               metadata.getNumberFormat(1).getPattern());
  assertEquals('$1 $2 $3', metadata.getNumberFormat(1).getFormat());
  assertEquals('[13-689]\\d{9}|2[0-35-9]\\d{8}',
               metadata.getGeneralDesc().getNationalNumberPattern());
  assertEquals('\\d{7}(?:\\d{3})?',
               metadata.getGeneralDesc().getPossibleNumberPattern());
  assertTrue(metadata.getGeneralDesc().equals(metadata.getFixedLine()));
  assertEquals('\\d{10}',
               metadata.getTollFree().getPossibleNumberPattern());
  assertEquals('900\\d{7}',
               metadata.getPremiumRate().getNationalNumberPattern());
  // No shared-cost data is available, so it should be initialised to 'NA'.
  assertEquals('NA', metadata.getSharedCost().getNationalNumberPattern());
  assertEquals('NA', metadata.getSharedCost().getPossibleNumberPattern());
}

function testGetInstanceLoadDEMetadata() {
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = phoneUtil.getMetadataForRegion(RegionCode.DE);
  assertEquals(RegionCode.DE, metadata.getId());
  assertEquals(49, metadata.getCountryCode());
  assertEquals('00', metadata.getInternationalPrefix());
  assertEquals('0', metadata.getNationalPrefix());
  assertEquals(6, metadata.numberFormatCount());
  assertEquals(1, metadata.getNumberFormat(5).leadingDigitsPatternCount());
  assertEquals('900', metadata.getNumberFormat(5).getLeadingDigitsPattern(0));
  assertEquals('(\\d{3})(\\d{3,4})(\\d{4})',
               metadata.getNumberFormat(5).getPattern());
  assertEquals('$1 $2 $3', metadata.getNumberFormat(5).getFormat());
  assertEquals('(?:[24-6]\\d{2}|3[03-9]\\d|[789](?:[1-9]\\d|0[2-9]))\\d{1,8}',
               metadata.getFixedLine().getNationalNumberPattern());
  assertEquals('\\d{2,14}', metadata.getFixedLine().getPossibleNumberPattern());
  assertEquals('30123456', metadata.getFixedLine().getExampleNumber());
  assertEquals('\\d{10}', metadata.getTollFree().getPossibleNumberPattern());
  assertEquals('900([135]\\d{6}|9\\d{7})',
               metadata.getPremiumRate().getNationalNumberPattern());
}

function testGetInstanceLoadARMetadata() {
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = phoneUtil.getMetadataForRegion(RegionCode.AR);
  assertEquals(RegionCode.AR, metadata.getId());
  assertEquals(54, metadata.getCountryCode());
  assertEquals('00', metadata.getInternationalPrefix());
  assertEquals('0', metadata.getNationalPrefix());
  assertEquals('0(?:(11|343|3715)15)?', metadata.getNationalPrefixForParsing());
  assertEquals('9$1', metadata.getNationalPrefixTransformRule());
  assertEquals('$2 15 $3-$4', metadata.getNumberFormat(2).getFormat());
  assertEquals('(9)(\\d{4})(\\d{2})(\\d{4})',
               metadata.getNumberFormat(3).getPattern());
  assertEquals('(9)(\\d{4})(\\d{2})(\\d{4})',
               metadata.getIntlNumberFormat(3).getPattern());
  assertEquals('$1 $2 $3 $4', metadata.getIntlNumberFormat(3).getFormat());
}

function testIsLeadingZeroPossible() {
  // Italy
  assertTrue(phoneUtil.isLeadingZeroPossible(39));
  // USA
  assertFalse(phoneUtil.isLeadingZeroPossible(1));
  // Not in metadata file, just default to false.
  assertFalse(phoneUtil.isLeadingZeroPossible(800));
}

function testGetLengthOfGeographicalAreaCode() {
  // Google MTV, which has area code '650'.
  assertEquals(3, phoneUtil.getLengthOfGeographicalAreaCode(US_NUMBER));

  // A North America toll-free number, which has no area code.
  assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(US_TOLLFREE));

  // Google London, which has area code '20'.
  assertEquals(2, phoneUtil.getLengthOfGeographicalAreaCode(GB_NUMBER));

  // A UK mobile phone, which has no area code.
  assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(GB_MOBILE));

  // Google Buenos Aires, which has area code '11'.
  assertEquals(2, phoneUtil.getLengthOfGeographicalAreaCode(AR_NUMBER));

  // Google Sydney, which has area code '2'.
  assertEquals(1, phoneUtil.getLengthOfGeographicalAreaCode(AU_NUMBER));

  // Google Singapore. Singapore has no area code and no national prefix.
  assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(SG_NUMBER));

  // An invalid US number (1 digit shorter), which has no area code.
  assertEquals(0,
      phoneUtil.getLengthOfGeographicalAreaCode(US_SHORT_BY_ONE_NUMBER));
}

function testGetLengthOfNationalDestinationCode() {
  // Google MTV, which has national destination code (NDC) '650'.
  assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(US_NUMBER));

  // A North America toll-free number, which has NDC '800'.
  assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(US_TOLLFREE));

  // Google London, which has NDC '20'.
  assertEquals(2, phoneUtil.getLengthOfNationalDestinationCode(GB_NUMBER));

  // A UK mobile phone, which has NDC '7912'.
  assertEquals(4, phoneUtil.getLengthOfNationalDestinationCode(GB_MOBILE));

  // Google Buenos Aires, which has NDC '11'.
  assertEquals(2, phoneUtil.getLengthOfNationalDestinationCode(AR_NUMBER));

  // An Argentinian mobile which has NDC '911'.
  assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(AR_MOBILE));

  // Google Sydney, which has NDC '2'.
  assertEquals(1, phoneUtil.getLengthOfNationalDestinationCode(AU_NUMBER));

  // Google Singapore, which has NDC '6521'.
  assertEquals(4, phoneUtil.getLengthOfNationalDestinationCode(SG_NUMBER));

  // An invalid US number (1 digit shorter), which has no NDC.
  assertEquals(0,
      phoneUtil.getLengthOfNationalDestinationCode(US_SHORT_BY_ONE_NUMBER));

  // A number containing an invalid country calling code, which shouldn't have
  // any NDC.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(123);
  number.setNationalNumber(6502530000);
  assertEquals(0, phoneUtil.getLengthOfNationalDestinationCode(number));
}

function testGetNationalSignificantNumber() {
  assertEquals('6502530000',
      phoneUtil.getNationalSignificantNumber(US_NUMBER));

  // An Italian mobile number.
  assertEquals('345678901',
      phoneUtil.getNationalSignificantNumber(IT_MOBILE));

  // An Italian fixed line number.
  assertEquals('0236618300',
      phoneUtil.getNationalSignificantNumber(IT_NUMBER));
}

function testGetExampleNumber() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  assertTrue(DE_NUMBER.equals(phoneUtil.getExampleNumber(RegionCode.DE)));

  assertTrue(DE_NUMBER.equals(
      phoneUtil.getExampleNumberForType(RegionCode.DE, PNT.FIXED_LINE)));
  assertNull(phoneUtil.getExampleNumberForType(RegionCode.DE, PNT.MOBILE));
  // For the US, the example number is placed under general description, and
  // hence should be used for both fixed line and mobile, so neither of these
  // should return null.
  assertNotNull(
      phoneUtil.getExampleNumberForType(RegionCode.US, PNT.FIXED_LINE));
  assertNotNull(phoneUtil.getExampleNumberForType(RegionCode.US, PNT.MOBILE));
  // CS is an invalid region, so we have no data for it.
  assertNull(phoneUtil.getExampleNumberForType(RegionCode.CS, PNT.MOBILE));
}

function testConvertAlphaCharactersInNumber() {
  /** @type {string} */
  var input = '1800-ABC-DEF';
  // Alpha chars are converted to digits; everything else is left untouched.
  /** @type {string} */
  var expectedOutput = '1800-222-333';
  assertEquals(expectedOutput,
      i18n.phonenumbers.PhoneNumberUtil.convertAlphaCharactersInNumber(input));
}

function testNormaliseRemovePunctuation() {
  /** @type {string} */
  var inputNumber = '034-56&+#234';
  /** @type {string} */
  var expectedOutput = '03456234';
  assertEquals('Conversion did not correctly remove punctuation',
      expectedOutput,
      i18n.phonenumbers.PhoneNumberUtil.normalize(inputNumber));
}

function testNormaliseReplaceAlphaCharacters() {
  /** @type {string} */
  var inputNumber = '034-I-am-HUNGRY';
  /** @type {string} */
  var expectedOutput = '034426486479';
  assertEquals('Conversion did not correctly replace alpha characters',
      expectedOutput,
      i18n.phonenumbers.PhoneNumberUtil.normalize(inputNumber));
}

function testNormaliseOtherDigits() {
  /** @type {string} */
  var inputNumber = '\uFF125\u0665';
  /** @type {string} */
  var expectedOutput = '255';
  assertEquals('Conversion did not correctly replace non-latin digits',
      expectedOutput,
      i18n.phonenumbers.PhoneNumberUtil.normalize(inputNumber));
  // Eastern-Arabic digits.
  inputNumber = '\u06F52\u06F0';
  expectedOutput = '520';
  assertEquals('Conversion did not correctly replace non-latin digits',
      expectedOutput,
      i18n.phonenumbers.PhoneNumberUtil.normalize(inputNumber));
}

function testNormaliseStripAlphaCharacters() {
  /** @type {string} */
  var inputNumber = '034-56&+a#234';
  /** @type {string} */
  var expectedOutput = '03456234';
  assertEquals('Conversion did not correctly remove alpha character',
      expectedOutput,
      i18n.phonenumbers.PhoneNumberUtil.normalizeDigitsOnly(inputNumber));
}

function testFormatUSNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  assertEquals('650 253 0000',
               phoneUtil.format(US_NUMBER, PNF.NATIONAL));
  assertEquals('+1 650 253 0000',
               phoneUtil.format(US_NUMBER, PNF.INTERNATIONAL));

  assertEquals('800 253 0000',
               phoneUtil.format(US_TOLLFREE, PNF.NATIONAL));
  assertEquals('+1 800 253 0000',
               phoneUtil.format(US_TOLLFREE, PNF.INTERNATIONAL));

  assertEquals('900 253 0000',
               phoneUtil.format(US_PREMIUM, PNF.NATIONAL));
  assertEquals('+1 900 253 0000',
               phoneUtil.format(US_PREMIUM, PNF.INTERNATIONAL));
  assertEquals('+1-900-253-0000',
               phoneUtil.format(US_PREMIUM, PNF.RFC3966));
  // Numbers with all zeros in the national number part will be formatted by
  // using the raw_input if that is available no matter which format is
  // specified.
  assertEquals('000-000-0000',
               phoneUtil.format(US_SPOOF_WITH_RAW_INPUT, PNF.NATIONAL));
  assertEquals('0', phoneUtil.format(US_SPOOF, PNF.NATIONAL));
}

function testFormatBSNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  assertEquals('242 365 1234',
               phoneUtil.format(BS_NUMBER, PNF.NATIONAL));
  assertEquals('+1 242 365 1234',
               phoneUtil.format(BS_NUMBER, PNF.INTERNATIONAL));
}

function testFormatGBNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  assertEquals('(020) 7031 3000',
               phoneUtil.format(GB_NUMBER, PNF.NATIONAL));
  assertEquals('+44 20 7031 3000',
               phoneUtil.format(GB_NUMBER, PNF.INTERNATIONAL));

  assertEquals('(07912) 345 678',
               phoneUtil.format(GB_MOBILE, PNF.NATIONAL));
  assertEquals('+44 7912 345 678',
               phoneUtil.format(GB_MOBILE, PNF.INTERNATIONAL));
}

function testFormatDENumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(301234);
  assertEquals('030/1234',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 30/1234',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));
  assertEquals('+49-30-1234',
               phoneUtil.format(deNumber, PNF.RFC3966));

  deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(291123);
  assertEquals('0291 123',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 291 123',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));

  deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(29112345678);
  assertEquals('0291 12345678',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 291 12345678',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));

  deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(912312345);
  assertEquals('09123 12345',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 9123 12345',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));

  deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(80212345);
  assertEquals('08021 2345',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 8021 2345',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));

  // Note this number is correctly formatted without national prefix. Most of
  // the numbers that are treated as invalid numbers by the library are short
  // numbers, and they are usually not dialed with national prefix.
  assertEquals('1234',
               phoneUtil.format(DE_SHORT_NUMBER, PNF.NATIONAL));
  assertEquals('+49 1234',
               phoneUtil.format(DE_SHORT_NUMBER, PNF.INTERNATIONAL));

  deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(41341234);
  assertEquals('04134 1234',
               phoneUtil.format(deNumber, PNF.NATIONAL));
}

function testFormatITNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  assertEquals('02 3661 8300',
               phoneUtil.format(IT_NUMBER, PNF.NATIONAL));
  assertEquals('+39 02 3661 8300',
               phoneUtil.format(IT_NUMBER, PNF.INTERNATIONAL));
  assertEquals('+390236618300',
               phoneUtil.format(IT_NUMBER, PNF.E164));

  assertEquals('345 678 901',
               phoneUtil.format(IT_MOBILE, PNF.NATIONAL));
  assertEquals('+39 345 678 901',
               phoneUtil.format(IT_MOBILE, PNF.INTERNATIONAL));
  assertEquals('+39345678901',
               phoneUtil.format(IT_MOBILE, PNF.E164));
}

function testFormatAUNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  assertEquals('02 3661 8300',
               phoneUtil.format(AU_NUMBER, PNF.NATIONAL));
  assertEquals('+61 2 3661 8300',
               phoneUtil.format(AU_NUMBER, PNF.INTERNATIONAL));
  assertEquals('+61236618300',
               phoneUtil.format(AU_NUMBER, PNF.E164));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var auNumber = new i18n.phonenumbers.PhoneNumber();
  auNumber.setCountryCode(61);
  auNumber.setNationalNumber(1800123456);
  assertEquals('1800 123 456',
               phoneUtil.format(auNumber, PNF.NATIONAL));
  assertEquals('+61 1800 123 456',
               phoneUtil.format(auNumber, PNF.INTERNATIONAL));
  assertEquals('+611800123456',
               phoneUtil.format(auNumber, PNF.E164));
}

function testFormatARNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  assertEquals('011 8765-4321',
               phoneUtil.format(AR_NUMBER, PNF.NATIONAL));
  assertEquals('+54 11 8765-4321',
               phoneUtil.format(AR_NUMBER, PNF.INTERNATIONAL));
  assertEquals('+541187654321',
               phoneUtil.format(AR_NUMBER, PNF.E164));

  assertEquals('011 15 8765-4321',
               phoneUtil.format(AR_MOBILE, PNF.NATIONAL));
  assertEquals('+54 9 11 8765 4321',
               phoneUtil.format(AR_MOBILE, PNF.INTERNATIONAL));
  assertEquals('+5491187654321',
               phoneUtil.format(AR_MOBILE, PNF.E164));
}

function testFormatMXNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  assertEquals('045 234 567 8900',
               phoneUtil.format(MX_MOBILE1, PNF.NATIONAL));
  assertEquals('+52 1 234 567 8900',
               phoneUtil.format(MX_MOBILE1, PNF.INTERNATIONAL));
  assertEquals('+5212345678900',
               phoneUtil.format(MX_MOBILE1, PNF.E164));

  assertEquals('045 55 1234 5678',
               phoneUtil.format(MX_MOBILE2, PNF.NATIONAL));
  assertEquals('+52 1 55 1234 5678',
               phoneUtil.format(MX_MOBILE2, PNF.INTERNATIONAL));
  assertEquals('+5215512345678',
               phoneUtil.format(MX_MOBILE2, PNF.E164));

  assertEquals('01 33 1234 5678',
               phoneUtil.format(MX_NUMBER1, PNF.NATIONAL));
  assertEquals('+52 33 1234 5678',
               phoneUtil.format(MX_NUMBER1, PNF.INTERNATIONAL));
  assertEquals('+523312345678',
               phoneUtil.format(MX_NUMBER1, PNF.E164));

  assertEquals('01 821 123 4567',
               phoneUtil.format(MX_NUMBER2, PNF.NATIONAL));
  assertEquals('+52 821 123 4567',
               phoneUtil.format(MX_NUMBER2, PNF.INTERNATIONAL));
  assertEquals('+528211234567',
               phoneUtil.format(MX_NUMBER2, PNF.E164));
}

function testFormatOutOfCountryCallingNumber() {
  assertEquals('00 1 900 253 0000',
      phoneUtil.formatOutOfCountryCallingNumber(US_PREMIUM, RegionCode.DE));

  assertEquals('1 650 253 0000',
      phoneUtil.formatOutOfCountryCallingNumber(US_NUMBER, RegionCode.BS));

  assertEquals('00 1 650 253 0000',
      phoneUtil.formatOutOfCountryCallingNumber(US_NUMBER, RegionCode.PL));

  assertEquals('011 44 7912 345 678',
      phoneUtil.formatOutOfCountryCallingNumber(GB_MOBILE, RegionCode.US));

  assertEquals('00 49 1234',
      phoneUtil.formatOutOfCountryCallingNumber(DE_SHORT_NUMBER,
                                                RegionCode.GB));
  // Note this number is correctly formatted without national prefix. Most of
  // the numbers that are treated as invalid numbers by the library are short
  // numbers, and they are usually not dialed with national prefix.
  assertEquals('1234',
      phoneUtil.formatOutOfCountryCallingNumber(DE_SHORT_NUMBER,
                                                RegionCode.DE));

  assertEquals('011 39 02 3661 8300',
      phoneUtil.formatOutOfCountryCallingNumber(IT_NUMBER, RegionCode.US));
  assertEquals('02 3661 8300',
      phoneUtil.formatOutOfCountryCallingNumber(IT_NUMBER, RegionCode.IT));
  assertEquals('+39 02 3661 8300',
      phoneUtil.formatOutOfCountryCallingNumber(IT_NUMBER, RegionCode.SG));

  assertEquals('6521 8000',
      phoneUtil.formatOutOfCountryCallingNumber(SG_NUMBER, RegionCode.SG));

  assertEquals('011 54 9 11 8765 4321',
      phoneUtil.formatOutOfCountryCallingNumber(AR_MOBILE, RegionCode.US));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arNumberWithExtn = AR_MOBILE.clone();
  arNumberWithExtn.setExtension('1234');
  assertEquals('011 54 9 11 8765 4321 ext. 1234',
      phoneUtil.formatOutOfCountryCallingNumber(arNumberWithExtn,
                                                RegionCode.US));
  assertEquals('0011 54 9 11 8765 4321 ext. 1234',
      phoneUtil.formatOutOfCountryCallingNumber(arNumberWithExtn,
                                                RegionCode.AU));
  assertEquals('011 15 8765-4321 ext. 1234',
      phoneUtil.formatOutOfCountryCallingNumber(arNumberWithExtn,
                                                RegionCode.AR));
}

function testFormatOutOfCountryWithPreferredIntlPrefix() {
  // This should use 0011, since that is the preferred international prefix
  // (both 0011 and 0012 are accepted as possible international prefixes in our
  // test metadta.)
  assertEquals('0011 39 02 3661 8300',
               phoneUtil.formatOutOfCountryCallingNumber(IT_NUMBER,
                                                         RegionCode.AU));
}

function testFormatOutOfCountryKeepingAlphaChars() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var alphaNumericNumber = new i18n.phonenumbers.PhoneNumber();
  alphaNumericNumber.setCountryCode(1);
  alphaNumericNumber.setNationalNumber(8007493524);
  alphaNumericNumber.setRawInput('1800 six-flag');
  assertEquals('0011 1 800 SIX-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.AU));

  alphaNumericNumber.setRawInput('1-800-SIX-flag');
  assertEquals('0011 1 800-SIX-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.AU));

  alphaNumericNumber.setRawInput('Call us from UK: 00 1 800 SIX-flag');
  assertEquals('0011 1 800 SIX-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.AU));

  alphaNumericNumber.setRawInput('800 SIX-flag');
  assertEquals('0011 1 800 SIX-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.AU));

  // Formatting from within the NANPA region.
  assertEquals('1 800 SIX-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.US));

  assertEquals('1 800 SIX-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.BS));

  // Testing that if the raw input doesn't exist, it is formatted using
  // formatOutOfCountryCallingNumber.
  alphaNumericNumber.clearRawInput();
  assertEquals('00 1 800 749 3524',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.DE));

  // Testing AU alpha number formatted from Australia.
  alphaNumericNumber.setCountryCode(61);
  alphaNumericNumber.setNationalNumber(827493524);
  alphaNumericNumber.setRawInput('+61 82749-FLAG');
  // This number should have the national prefix fixed.
  assertEquals('082749-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.AU));

  alphaNumericNumber.setRawInput('082749-FLAG');
  assertEquals('082749-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.AU));

  alphaNumericNumber.setNationalNumber(18007493524);
  alphaNumericNumber.setRawInput('1-800-SIX-flag');
  // This number should not have the national prefix prefixed, in accordance
  // with the override for this specific formatting rule.
  assertEquals('1-800-SIX-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.AU));

  // The metadata should not be permanently changed, since we copied it before
  // modifying patterns. Here we check this.
  alphaNumericNumber.setNationalNumber(1800749352);
  assertEquals('1800 749 352',
      phoneUtil.formatOutOfCountryCallingNumber(alphaNumericNumber,
                                                RegionCode.AU));

  // Testing a region with multiple international prefixes.
  assertEquals('+61 1-800-SIX-FLAG',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.SG));

  // Testing the case with an invalid country calling code.
  alphaNumericNumber.setCountryCode(0);
  alphaNumericNumber.setNationalNumber(18007493524);
  alphaNumericNumber.setRawInput('1-800-SIX-flag');
  // Uses the raw input only.
  assertEquals('1-800-SIX-flag',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.DE));

  // Testing the case of an invalid alpha number.
  alphaNumericNumber.setCountryCode(1);
  alphaNumericNumber.setNationalNumber(80749);
  alphaNumericNumber.setRawInput('180-SIX');
  // No country-code stripping can be done.
  assertEquals('00 1 180-SIX',
      phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber,
                                                    RegionCode.DE));
}

function testFormatWithCarrierCode() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  // We only support this for AR in our test metadata, and only for mobile
  // numbers starting with certain values.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arMobile = new i18n.phonenumbers.PhoneNumber();
  arMobile.setCountryCode(54);
  arMobile.setNationalNumber(92234654321);
  assertEquals('02234 65-4321', phoneUtil.format(arMobile, PNF.NATIONAL));
  // Here we force 14 as the carrier code.
  assertEquals('02234 14 65-4321',
               phoneUtil.formatNationalNumberWithCarrierCode(arMobile, '14'));
  // Here we force the number to be shown with no carrier code.
  assertEquals('02234 65-4321',
               phoneUtil.formatNationalNumberWithCarrierCode(arMobile, ''));
  // Here the international rule is used, so no carrier code should be present.
  assertEquals('+5492234654321', phoneUtil.format(arMobile, PNF.E164));
  // We don't support this for the US so there should be no change.
  assertEquals('650 253 0000',
               phoneUtil.formatNationalNumberWithCarrierCode(US_NUMBER, '15'));
}

function testFormatWithPreferredCarrierCode() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  // We only support this for AR in our test metadata.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(91234125678);
  // Test formatting with no preferred carrier code stored in the number itself.
  assertEquals('01234 15 12-5678',
      phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, '15'));
  assertEquals('01234 12-5678',
      phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, ''));
  // Test formatting with preferred carrier code present.
  arNumber.setPreferredDomesticCarrierCode('19');
  assertEquals('01234 12-5678', phoneUtil.format(arNumber, PNF.NATIONAL));
  assertEquals('01234 19 12-5678',
      phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, '15'));
  assertEquals('01234 19 12-5678',
      phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, ''));
  // When the preferred_domestic_carrier_code is present (even when it contains
  // an empty string), use it instead of the default carrier code passed in.
  arNumber.setPreferredDomesticCarrierCode('');
  assertEquals('01234 12-5678',
      phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, '15'));
  // We don't support this for the US so there should be no change.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(4241231234);
  usNumber.setPreferredDomesticCarrierCode('99');
  assertEquals('424 123 1234', phoneUtil.format(usNumber, PNF.NATIONAL));
  assertEquals('424 123 1234',
      phoneUtil.formatNationalNumberWithPreferredCarrierCode(usNumber, '15'));
}

function testFormatNumberForMobileDialing() {
  // US toll free numbers are marked as noInternationalDialling in the test
  // metadata for testing purposes.
  assertEquals('800 253 0000',
      phoneUtil.formatNumberForMobileDialing(US_TOLLFREE, RegionCode.US, true));
  assertEquals('',
      phoneUtil.formatNumberForMobileDialing(US_TOLLFREE, RegionCode.CN, true));
  assertEquals('+1 650 253 0000',
      phoneUtil.formatNumberForMobileDialing(US_NUMBER, RegionCode.US, true));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumberWithExtn = US_NUMBER.clone();
  usNumberWithExtn.setExtension('1234');
  assertEquals('+1 650 253 0000',
      phoneUtil.formatNumberForMobileDialing(usNumberWithExtn,
                                             RegionCode.US, true));

  assertEquals('8002530000',
      phoneUtil.formatNumberForMobileDialing(US_TOLLFREE,
                                             RegionCode.US, false));
  assertEquals('',
      phoneUtil.formatNumberForMobileDialing(US_TOLLFREE,
                                             RegionCode.CN, false));
  assertEquals('+16502530000',
      phoneUtil.formatNumberForMobileDialing(US_NUMBER, RegionCode.US, false));
  assertEquals('+16502530000',
      phoneUtil.formatNumberForMobileDialing(usNumberWithExtn,
                                             RegionCode.US, false));
}

function testFormatByPattern() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.NumberFormat} */
  var newNumFormat = new i18n.phonenumbers.NumberFormat();
  newNumFormat.setPattern('(\\d{3})(\\d{3})(\\d{4})');
  newNumFormat.setFormat('($1) $2-$3');

  assertEquals('(650) 253-0000',
               phoneUtil.formatByPattern(US_NUMBER,
                                         PNF.NATIONAL,
                                         [newNumFormat]));
  assertEquals('+1 (650) 253-0000',
               phoneUtil.formatByPattern(US_NUMBER,
                                         PNF.INTERNATIONAL,
                                         [newNumFormat]));

  // $NP is set to '1' for the US. Here we check that for other NANPA countries
  // the US rules are followed.
  newNumFormat.setNationalPrefixFormattingRule('$NP ($FG)');
  newNumFormat.setFormat('$1 $2-$3');
  assertEquals('1 (242) 365-1234',
               phoneUtil.formatByPattern(BS_NUMBER,
                                         PNF.NATIONAL,
                                         [newNumFormat]));
  assertEquals('+1 242 365-1234',
               phoneUtil.formatByPattern(BS_NUMBER,
                                         PNF.INTERNATIONAL,
                                         [newNumFormat]));

  newNumFormat.setPattern('(\\d{2})(\\d{5})(\\d{3})');
  newNumFormat.setFormat('$1-$2 $3');

  assertEquals('02-36618 300',
               phoneUtil.formatByPattern(IT_NUMBER,
                                         PNF.NATIONAL,
                                         [newNumFormat]));
  assertEquals('+39 02-36618 300',
               phoneUtil.formatByPattern(IT_NUMBER,
                                         PNF.INTERNATIONAL,
                                         [newNumFormat]));

  newNumFormat.setNationalPrefixFormattingRule('$NP$FG');
  newNumFormat.setPattern('(\\d{2})(\\d{4})(\\d{4})');
  newNumFormat.setFormat('$1 $2 $3');
  assertEquals('020 7031 3000',
               phoneUtil.formatByPattern(GB_NUMBER,
                                         PNF.NATIONAL,
                                         [newNumFormat]));

  newNumFormat.setNationalPrefixFormattingRule('($NP$FG)');
  assertEquals('(020) 7031 3000',
               phoneUtil.formatByPattern(GB_NUMBER,
                                         PNF.NATIONAL,
                                         [newNumFormat]));

  newNumFormat.setNationalPrefixFormattingRule('');
  assertEquals('20 7031 3000',
               phoneUtil.formatByPattern(GB_NUMBER,
                                         PNF.NATIONAL,
                                         [newNumFormat]));

  assertEquals('+44 20 7031 3000',
               phoneUtil.formatByPattern(GB_NUMBER,
                                         PNF.INTERNATIONAL,
                                         [newNumFormat]));
}

function testFormatE164Number() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  assertEquals('+16502530000', phoneUtil.format(US_NUMBER, PNF.E164));
  assertEquals('+4930123456', phoneUtil.format(DE_NUMBER, PNF.E164));
}

function testFormatNumberWithExtension() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = NZ_NUMBER.clone();
  nzNumber.setExtension('1234');
  // Uses default extension prefix:
  assertEquals('03-331 6005 ext. 1234',
               phoneUtil.format(nzNumber, PNF.NATIONAL));
  // Uses RFC 3966 syntax.
  assertEquals('+64-3-331-6005;ext=1234',
               phoneUtil.format(nzNumber, PNF.RFC3966));
  // Extension prefix overridden in the territory information for the US:
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumberWithExtension = US_NUMBER.clone();
  usNumberWithExtension.setExtension('4567');
  assertEquals('650 253 0000 extn. 4567',
               phoneUtil.format(usNumberWithExtension, PNF.NATIONAL));
}

function testFormatUsingOriginalNumberFormat() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number1 = phoneUtil.parseAndKeepRawInput('+442087654321', RegionCode.GB);
  assertEquals('+44 20 8765 4321',
               phoneUtil.formatInOriginalFormat(number1, RegionCode.GB));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number2 = phoneUtil.parseAndKeepRawInput('02087654321', RegionCode.GB);
  assertEquals('(020) 8765 4321',
               phoneUtil.formatInOriginalFormat(number2, RegionCode.GB));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number3 = phoneUtil.parseAndKeepRawInput('011442087654321',
                                               RegionCode.US);
  assertEquals('011 44 20 8765 4321',
               phoneUtil.formatInOriginalFormat(number3, RegionCode.US));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number4 = phoneUtil.parseAndKeepRawInput('442087654321', RegionCode.GB);
  assertEquals('44 20 8765 4321',
               phoneUtil.formatInOriginalFormat(number4, RegionCode.GB));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number5 = phoneUtil.parse('+442087654321', RegionCode.GB);
  assertEquals('(020) 8765 4321',
               phoneUtil.formatInOriginalFormat(number5, RegionCode.GB));

  // Invalid numbers should be formatted using its raw input when that is
  // available. Note area codes starting with 7 are intentionally excluded in
  // the test metadata for testing purposes.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number6 = phoneUtil.parseAndKeepRawInput('7345678901', RegionCode.US);
  assertEquals('7345678901',
               phoneUtil.formatInOriginalFormat(number6, RegionCode.US));

  // When the raw input is unavailable, format as usual.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number7 = phoneUtil.parse('7345678901', RegionCode.US);
  assertEquals('734 567 8901',
               phoneUtil.formatInOriginalFormat(number7, RegionCode.US));
}

function testIsPremiumRate() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  assertEquals(PNT.PREMIUM_RATE, phoneUtil.getNumberType(US_PREMIUM));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var premiumRateNumber = new i18n.phonenumbers.PhoneNumber();
  premiumRateNumber = new i18n.phonenumbers.PhoneNumber();
  premiumRateNumber.setCountryCode(39);
  premiumRateNumber.setNationalNumber(892123);
  assertEquals(PNT.PREMIUM_RATE, phoneUtil.getNumberType(premiumRateNumber));

  premiumRateNumber = new i18n.phonenumbers.PhoneNumber();
  premiumRateNumber.setCountryCode(44);
  premiumRateNumber.setNationalNumber(9187654321);
  assertEquals(PNT.PREMIUM_RATE, phoneUtil.getNumberType(premiumRateNumber));

  premiumRateNumber = new i18n.phonenumbers.PhoneNumber();
  premiumRateNumber.setCountryCode(49);
  premiumRateNumber.setNationalNumber(9001654321);
  assertEquals(PNT.PREMIUM_RATE, phoneUtil.getNumberType(premiumRateNumber));

  premiumRateNumber = new i18n.phonenumbers.PhoneNumber();
  premiumRateNumber.setCountryCode(49);
  premiumRateNumber.setNationalNumber(90091234567);
  assertEquals(PNT.PREMIUM_RATE, phoneUtil.getNumberType(premiumRateNumber));
}

function testIsTollFree() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tollFreeNumber = new i18n.phonenumbers.PhoneNumber();

  tollFreeNumber.setCountryCode(1);
  tollFreeNumber.setNationalNumber(8881234567);
  assertEquals(PNT.TOLL_FREE, phoneUtil.getNumberType(tollFreeNumber));

  tollFreeNumber = new i18n.phonenumbers.PhoneNumber();
  tollFreeNumber.setCountryCode(39);
  tollFreeNumber.setNationalNumber(803123);
  assertEquals(PNT.TOLL_FREE, phoneUtil.getNumberType(tollFreeNumber));

  tollFreeNumber = new i18n.phonenumbers.PhoneNumber();
  tollFreeNumber.setCountryCode(44);
  tollFreeNumber.setNationalNumber(8012345678);
  assertEquals(PNT.TOLL_FREE, phoneUtil.getNumberType(tollFreeNumber));

  tollFreeNumber = new i18n.phonenumbers.PhoneNumber();
  tollFreeNumber.setCountryCode(49);
  tollFreeNumber.setNationalNumber(8001234567);
  assertEquals(PNT.TOLL_FREE, phoneUtil.getNumberType(tollFreeNumber));
}

function testIsMobile() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(BS_MOBILE));
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(GB_MOBILE));
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(IT_MOBILE));
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(AR_MOBILE));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var mobileNumber = new i18n.phonenumbers.PhoneNumber();
  mobileNumber.setCountryCode(49);
  mobileNumber.setNationalNumber(15123456789);
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(mobileNumber));
}

function testIsFixedLine() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  assertEquals(PNT.FIXED_LINE, phoneUtil.getNumberType(BS_NUMBER));
  assertEquals(PNT.FIXED_LINE, phoneUtil.getNumberType(IT_NUMBER));
  assertEquals(PNT.FIXED_LINE, phoneUtil.getNumberType(GB_NUMBER));
  assertEquals(PNT.FIXED_LINE, phoneUtil.getNumberType(DE_NUMBER));
}

function testIsFixedLineAndMobile() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  assertEquals(PNT.FIXED_LINE_OR_MOBILE,
               phoneUtil.getNumberType(US_NUMBER));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var fixedLineAndMobileNumber = new i18n.phonenumbers.PhoneNumber();
  fixedLineAndMobileNumber.setCountryCode(54);
  fixedLineAndMobileNumber.setNationalNumber(1987654321);
  assertEquals(PNT.FIXED_LINE_OR_MOBILE,
               phoneUtil.getNumberType(fixedLineAndMobileNumber));
}

function testIsSharedCost() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(8431231234);
  assertEquals(PNT.SHARED_COST, phoneUtil.getNumberType(gbNumber));
}

function testIsVoip() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(5631231234);
  assertEquals(PNT.VOIP, phoneUtil.getNumberType(gbNumber));
}

function testIsPersonalNumber() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(7031231234);
  assertEquals(PNT.PERSONAL_NUMBER, phoneUtil.getNumberType(gbNumber));
}

function testIsUnknown() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  // Invalid numbers should be of type UNKNOWN.
  assertEquals(PNT.UNKNOWN, phoneUtil.getNumberType(US_LOCAL_NUMBER));
}

function testIsValidNumber() {
  assertTrue(phoneUtil.isValidNumber(US_NUMBER));
  assertTrue(phoneUtil.isValidNumber(IT_NUMBER));
  assertTrue(phoneUtil.isValidNumber(GB_MOBILE));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(21387835);
  assertTrue(phoneUtil.isValidNumber(nzNumber));
}

function testIsValidForRegion() {
  // This number is valid for the Bahamas, but is not a valid US number.
  assertTrue(phoneUtil.isValidNumber(BS_NUMBER));
  assertTrue(phoneUtil.isValidNumberForRegion(BS_NUMBER, RegionCode.BS));
  assertFalse(phoneUtil.isValidNumberForRegion(BS_NUMBER, RegionCode.US));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var bsInvalidNumber = new i18n.phonenumbers.PhoneNumber();
  bsInvalidNumber.setCountryCode(1);
  bsInvalidNumber.setNationalNumber(2421232345);
  // This number is no longer valid.
  assertFalse(phoneUtil.isValidNumber(bsInvalidNumber));

  // La Mayotte and Reunion use 'leadingDigits' to differentiate them.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var reNumber = new i18n.phonenumbers.PhoneNumber();
  reNumber.setCountryCode(262);
  reNumber.setNationalNumber(262123456);
  assertTrue(phoneUtil.isValidNumber(reNumber));
  assertTrue(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.RE));
  assertFalse(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.YT));
  // Now change the number to be a number for La Mayotte.
  reNumber.setNationalNumber(269601234);
  assertTrue(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.YT));
  assertFalse(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.RE));
  // This number is no longer valid for La Reunion.
  reNumber.setNationalNumber(269123456);
  assertFalse(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.YT));
  assertFalse(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.RE));
  assertFalse(phoneUtil.isValidNumber(reNumber));
  // However, it should be recognised as from La Mayotte, since it is valid for
  // this region.
  assertEquals(RegionCode.YT, phoneUtil.getRegionCodeForNumber(reNumber));
  // This number is valid in both places.
  reNumber.setNationalNumber(800123456);
  assertTrue(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.YT));
  assertTrue(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.RE));
}

function testIsNotValidNumber() {
  assertFalse(phoneUtil.isValidNumber(US_LOCAL_NUMBER));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var invalidNumber = new i18n.phonenumbers.PhoneNumber();
  invalidNumber.setCountryCode(39);
  invalidNumber.setNationalNumber(23661830000);
  invalidNumber.setItalianLeadingZero(true);
  assertFalse(phoneUtil.isValidNumber(invalidNumber));

  invalidNumber = new i18n.phonenumbers.PhoneNumber();
  invalidNumber.setCountryCode(44);
  invalidNumber.setNationalNumber(791234567);
  assertFalse(phoneUtil.isValidNumber(invalidNumber));

  invalidNumber = new i18n.phonenumbers.PhoneNumber();
  invalidNumber.setCountryCode(49);
  invalidNumber.setNationalNumber(1234);
  assertFalse(phoneUtil.isValidNumber(invalidNumber));

  invalidNumber = new i18n.phonenumbers.PhoneNumber();
  invalidNumber.setCountryCode(64);
  invalidNumber.setNationalNumber(3316005);
  assertFalse(phoneUtil.isValidNumber(invalidNumber));
}

function testGetRegionCodeForCountryCode() {
  assertEquals(RegionCode.US, phoneUtil.getRegionCodeForCountryCode(1));
  assertEquals(RegionCode.GB, phoneUtil.getRegionCodeForCountryCode(44));
  assertEquals(RegionCode.DE, phoneUtil.getRegionCodeForCountryCode(49));
}

function testGetRegionCodeForNumber() {
  assertEquals(RegionCode.BS, phoneUtil.getRegionCodeForNumber(BS_NUMBER));
  assertEquals(RegionCode.US, phoneUtil.getRegionCodeForNumber(US_NUMBER));
  assertEquals(RegionCode.GB, phoneUtil.getRegionCodeForNumber(GB_MOBILE));
}

function testGetCountryCodeForRegion() {
  assertEquals(1, phoneUtil.getCountryCodeForRegion(RegionCode.US));
  assertEquals(64, phoneUtil.getCountryCodeForRegion(RegionCode.NZ));
  assertEquals(0, phoneUtil.getCountryCodeForRegion(null));
  assertEquals(0, phoneUtil.getCountryCodeForRegion(RegionCode.ZZ));
  // CS is already deprecated so the library doesn't support it.
  assertEquals(0, phoneUtil.getCountryCodeForRegion(RegionCode.CS));
}

function testGetNationalDiallingPrefixForRegion() {
  assertEquals('1', phoneUtil.getNddPrefixForRegion(RegionCode.US, false));
  // Test non-main country to see it gets the national dialling prefix for the
  // main country with that country calling code.
  assertEquals('1', phoneUtil.getNddPrefixForRegion(RegionCode.BS, false));
  assertEquals('0', phoneUtil.getNddPrefixForRegion(RegionCode.NZ, false));
  // Test case with non digit in the national prefix.
  assertEquals('0~0', phoneUtil.getNddPrefixForRegion(RegionCode.AO, false));
  assertEquals('00', phoneUtil.getNddPrefixForRegion(RegionCode.AO, true));
  // Test cases with invalid regions.
  assertNull(phoneUtil.getNddPrefixForRegion(null, false));
  assertNull(phoneUtil.getNddPrefixForRegion(RegionCode.ZZ, false));
  // CS is already deprecated so the library doesn't support it.
  assertNull(phoneUtil.getNddPrefixForRegion(RegionCode.CS, false));
}

function testIsNANPACountry() {
  assertTrue(phoneUtil.isNANPACountry(RegionCode.US));
  assertTrue(phoneUtil.isNANPACountry(RegionCode.BS));
  assertFalse(phoneUtil.isNANPACountry(RegionCode.DE));
  assertFalse(phoneUtil.isNANPACountry(RegionCode.ZZ));
  assertFalse(phoneUtil.isNANPACountry(null));
}

function testIsPossibleNumber() {
  assertTrue(phoneUtil.isPossibleNumber(US_NUMBER));
  assertTrue(phoneUtil.isPossibleNumber(US_LOCAL_NUMBER));
  assertTrue(phoneUtil.isPossibleNumber(GB_NUMBER));

  assertTrue(
      phoneUtil.isPossibleNumberString('+1 650 253 0000', RegionCode.US));
  assertTrue(
      phoneUtil.isPossibleNumberString('+1 650 GOO OGLE', RegionCode.US));
  assertTrue(
      phoneUtil.isPossibleNumberString('(650) 253-0000', RegionCode.US));
  assertTrue(
      phoneUtil.isPossibleNumberString('253-0000', RegionCode.US));
  assertTrue(
      phoneUtil.isPossibleNumberString('+1 650 253 0000', RegionCode.GB));
  assertTrue(
      phoneUtil.isPossibleNumberString('+44 20 7031 3000', RegionCode.GB));
  assertTrue(
      phoneUtil.isPossibleNumberString('(020) 7031 3000', RegionCode.GB));
  assertTrue(
      phoneUtil.isPossibleNumberString('7031 3000', RegionCode.GB));
  assertTrue(
      phoneUtil.isPossibleNumberString('3331 6005', RegionCode.NZ));
}

function testIsPossibleNumberWithReason() {
  var VR = i18n.phonenumbers.PhoneNumberUtil.ValidationResult;
  // National numbers for country calling code +1 that are within 7 to 10 digits
  // are possible.
  assertEquals(VR.IS_POSSIBLE,
               phoneUtil.isPossibleNumberWithReason(US_NUMBER));

  assertEquals(VR.IS_POSSIBLE,
               phoneUtil.isPossibleNumberWithReason(US_LOCAL_NUMBER));

  assertEquals(VR.TOO_LONG,
               phoneUtil.isPossibleNumberWithReason(US_LONG_NUMBER));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(0);
  number.setNationalNumber(2530000);
  assertEquals(VR.INVALID_COUNTRY_CODE,
               phoneUtil.isPossibleNumberWithReason(number));

  number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(253000);
  assertEquals(VR.TOO_SHORT,
               phoneUtil.isPossibleNumberWithReason(number));

  number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(65);
  number.setNationalNumber(1234567890);
  assertEquals(VR.IS_POSSIBLE,
               phoneUtil.isPossibleNumberWithReason(number));

  // Try with number that we don't have metadata for.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var adNumber = new i18n.phonenumbers.PhoneNumber();
  adNumber.setCountryCode(376);
  adNumber.setNationalNumber(12345);
  assertEquals(VR.IS_POSSIBLE,
               phoneUtil.isPossibleNumberWithReason(adNumber));
  adNumber.setCountryCode(376);
  adNumber.setNationalNumber(13);
  assertEquals(VR.TOO_SHORT,
               phoneUtil.isPossibleNumberWithReason(adNumber));
  adNumber.setCountryCode(376);
  adNumber.setNationalNumber(12345678901234567);
  assertEquals(VR.TOO_LONG,
               phoneUtil.isPossibleNumberWithReason(adNumber));
}

function testIsNotPossibleNumber() {
  assertFalse(phoneUtil.isPossibleNumber(US_LONG_NUMBER));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(253000);
  assertFalse(phoneUtil.isPossibleNumber(number));

  number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(44);
  number.setNationalNumber(300);
  assertFalse(phoneUtil.isPossibleNumber(number));

  assertFalse(
      phoneUtil.isPossibleNumberString('+1 650 253 00000', RegionCode.US));
  assertFalse(
      phoneUtil.isPossibleNumberString('(650) 253-00000', RegionCode.US));
  assertFalse(
      phoneUtil.isPossibleNumberString('I want a Pizza', RegionCode.US));
  assertFalse(
      phoneUtil.isPossibleNumberString('253-000', RegionCode.US));
  assertFalse(
      phoneUtil.isPossibleNumberString('1 3000', RegionCode.GB));
  assertFalse(
      phoneUtil.isPossibleNumberString('+44 300', RegionCode.GB));
}

function testTruncateTooLongNumber() {
  // US number 650-253-0000, but entered with one additional digit at the end.
  assertTrue(phoneUtil.truncateTooLongNumber(US_LONG_NUMBER));
  assertTrue(US_NUMBER.equals(US_LONG_NUMBER));

  // GB number 080 1234 5678, but entered with 4 extra digits at the end.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tooLongNumber = new i18n.phonenumbers.PhoneNumber();
  tooLongNumber.setCountryCode(44);
  tooLongNumber.setNationalNumber(80123456780123);
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var validNumber = new i18n.phonenumbers.PhoneNumber();
  validNumber.setCountryCode(44);
  validNumber.setNationalNumber(8012345678);
  assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
  assertTrue(validNumber.equals(tooLongNumber));

  // IT number 022 3456 7890, but entered with 3 extra digits at the end.
  tooLongNumber = new i18n.phonenumbers.PhoneNumber();
  tooLongNumber.setCountryCode(39);
  tooLongNumber.setNationalNumber(2234567890123);
  tooLongNumber.setItalianLeadingZero(true);
  validNumber = new i18n.phonenumbers.PhoneNumber();
  validNumber.setCountryCode(39);
  validNumber.setNationalNumber(2234567890);
  validNumber.setItalianLeadingZero(true);
  assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
  assertTrue(validNumber.equals(tooLongNumber));

  // Tests what happens when a valid number is passed in.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var validNumberCopy = validNumber.clone();
  assertTrue(phoneUtil.truncateTooLongNumber(validNumber));
  // Tests the number is not modified.
  assertTrue(validNumber.equals(validNumberCopy));

  // Tests what happens when a number with invalid prefix is passed in.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var numberWithInvalidPrefix = new i18n.phonenumbers.PhoneNumber();
  // The test metadata says US numbers cannot have prefix 240.
  numberWithInvalidPrefix.setCountryCode(1);
  numberWithInvalidPrefix.setNationalNumber(2401234567);
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var invalidNumberCopy = numberWithInvalidPrefix.clone();
  assertFalse(phoneUtil.truncateTooLongNumber(numberWithInvalidPrefix));
  // Tests the number is not modified.
  assertTrue(numberWithInvalidPrefix.equals(invalidNumberCopy));

  // Tests what happens when a too short number is passed in.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tooShortNumber = new i18n.phonenumbers.PhoneNumber();
  tooShortNumber.setCountryCode(1);
  tooShortNumber.setNationalNumber(1234);
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tooShortNumberCopy = tooShortNumber.clone();
  assertFalse(phoneUtil.truncateTooLongNumber(tooShortNumber));
  // Tests the number is not modified.
  assertTrue(tooShortNumber.equals(tooShortNumberCopy));
}

function testIsViablePhoneNumber() {
  var isViable = i18n.phonenumbers.PhoneNumberUtil.isViablePhoneNumber;
  // Only one or two digits before strange non-possible punctuation.
  assertFalse(isViable('12. March'));
  assertFalse(isViable('1+1+1'));
  assertFalse(isViable('80+0'));
  assertFalse(isViable('00'));
  // Three digits is viable.
  assertTrue(isViable('111'));
  // Alpha numbers.
  assertTrue(isViable('0800-4-pizza'));
  assertTrue(isViable('0800-4-PIZZA'));
}

function testIsViablePhoneNumberNonAscii() {
  var isViable = i18n.phonenumbers.PhoneNumberUtil.isViablePhoneNumber;
  // Only one or two digits before possible punctuation followed by more digits.
  assertTrue(isViable('1\u300034'));
  assertFalse(isViable('1\u30003+4'));
  // Unicode variants of possible starting character and other allowed
  // punctuation/digits.
  assertTrue(isViable('\uFF081\uFF09\u30003456789'));
  // Testing a leading + is okay.
  assertTrue(isViable('+1\uFF09\u30003456789'));
}

function testExtractPossibleNumber() {
  var extract = i18n.phonenumbers.PhoneNumberUtil.extractPossibleNumber;
  // Removes preceding funky punctuation and letters but leaves the rest
  // untouched.
  assertEquals('0800-345-600', extract('Tel:0800-345-600'));
  assertEquals('0800 FOR PIZZA', extract('Tel:0800 FOR PIZZA'));
  // Should not remove plus sign
  assertEquals('+800-345-600', extract('Tel:+800-345-600'));
  // Should recognise wide digits as possible start values.
  assertEquals('\uFF10\uFF12\uFF13', extract('\uFF10\uFF12\uFF13'));
  // Dashes are not possible start values and should be removed.
  assertEquals('\uFF11\uFF12\uFF13', extract('Num-\uFF11\uFF12\uFF13'));
  // If not possible number present, return empty string.
  assertEquals('', extract('Num-....'));
  // Leading brackets are stripped - these are not used when parsing.
  assertEquals('650) 253-0000', extract('(650) 253-0000'));

  // Trailing non-alpha-numeric characters should be removed.
  assertEquals('650) 253-0000', extract('(650) 253-0000..- ..'));
  assertEquals('650) 253-0000', extract('(650) 253-0000.'));
  // This case has a trailing RTL char.
  assertEquals('650) 253-0000', extract('(650) 253-0000\u200F'));
}

function testMaybeStripNationalPrefix() {
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = new i18n.phonenumbers.PhoneMetadata();
  metadata.setNationalPrefixForParsing('34');
  /** @type {i18n.phonenumbers.PhoneNumberDesc} */
  var generalDesc = new i18n.phonenumbers.PhoneNumberDesc();
  generalDesc.setNationalNumberPattern('\\d{4,8}');
  metadata.setGeneralDesc(generalDesc);
  /** @type {!goog.string.StringBuffer} */
  var numberToStrip = new goog.string.StringBuffer('34356778');
  /** @type {string} */
  var strippedNumber = '356778';
  phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata);
  assertEquals('Should have had national prefix stripped.',
               strippedNumber, numberToStrip.toString());
  // Retry stripping - now the number should not start with the national prefix,
  // so no more stripping should occur.
  phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata);
  assertEquals('Should have had no change - no national prefix present.',
               strippedNumber, numberToStrip.toString());
  // Some countries have no national prefix. Repeat test with none specified.
  metadata.setNationalPrefixForParsing('');
  phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata);
  assertEquals('Should not strip anything with empty national prefix.',
               strippedNumber, numberToStrip.toString());
  // If the resultant number doesn't match the national rule, it shouldn't be
  // stripped.
  metadata.setNationalPrefixForParsing('3');
  numberToStrip = new goog.string.StringBuffer('3123');
  strippedNumber = '3123';
  phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata);
  assertEquals('Should have had no change - after stripping, it would not ' +
               'have matched the national rule.',
               strippedNumber, numberToStrip.toString());
  // Test extracting carrier selection code.
  metadata.setNationalPrefixForParsing('0(81)?');
  numberToStrip = new goog.string.StringBuffer('08122123456');
  strippedNumber = '22123456';
  assertEquals('81',
               phoneUtil.maybeStripNationalPrefixAndCarrierCode(
                   numberToStrip, metadata));
  assertEquals('Should have had national prefix and carrier code stripped.',
               strippedNumber, numberToStrip.toString());
  // If there was a transform rule, check it was applied.
  metadata.setNationalPrefixTransformRule('5$15');
  // Note that a capturing group is present here.
  metadata.setNationalPrefixForParsing('0(\\d{2})');
  numberToStrip = new goog.string.StringBuffer('031123');
  /** @type {string} */
  var transformedNumber = '5315123';
  phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata);
  assertEquals('Should transform the 031 to a 5315.',
               transformedNumber, numberToStrip.toString());
}

function testMaybeStripInternationalPrefix() {
  var CCS = i18n.phonenumbers.PhoneNumber.CountryCodeSource;
  /** @type {string} */
  var internationalPrefix = '00[39]';
  /** @type {!goog.string.StringBuffer} */
  var numberToStrip = new goog.string.StringBuffer('0034567700-3898003');
  // Note the dash is removed as part of the normalization.
  /** @type {!goog.string.StringBuffer} */
  var strippedNumber = new goog.string.StringBuffer('45677003898003');
  assertEquals(CCS.FROM_NUMBER_WITH_IDD,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                          internationalPrefix));
  assertEquals('The number supplied was not stripped of its international ' +
               'prefix.',
               strippedNumber.toString(), numberToStrip.toString());
  // Now the number no longer starts with an IDD prefix, so it should now report
  // FROM_DEFAULT_COUNTRY.
  assertEquals(CCS.FROM_DEFAULT_COUNTRY,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                          internationalPrefix));

  numberToStrip = new goog.string.StringBuffer('00945677003898003');
  assertEquals(CCS.FROM_NUMBER_WITH_IDD,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                          internationalPrefix));
  assertEquals('The number supplied was not stripped of its international ' +
               'prefix.',
               strippedNumber.toString(), numberToStrip.toString());
  // Test it works when the international prefix is broken up by spaces.
  numberToStrip = new goog.string.StringBuffer('00 9 45677003898003');
  assertEquals(CCS.FROM_NUMBER_WITH_IDD,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                          internationalPrefix));
  assertEquals('The number supplied was not stripped of its international ' +
               'prefix.',
               strippedNumber.toString(), numberToStrip.toString());
  // Now the number no longer starts with an IDD prefix, so it should now report
  // FROM_DEFAULT_COUNTRY.
  assertEquals(CCS.FROM_DEFAULT_COUNTRY,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                          internationalPrefix));

  // Test the + symbol is also recognised and stripped.
  numberToStrip = new goog.string.StringBuffer('+45677003898003');
  strippedNumber = new goog.string.StringBuffer('45677003898003');
  assertEquals(CCS.FROM_NUMBER_WITH_PLUS_SIGN,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                          internationalPrefix));
  assertEquals('The number supplied was not stripped of the plus symbol.',
               strippedNumber.toString(), numberToStrip.toString());

  // If the number afterwards is a zero, we should not strip this - no country
  // calling code begins with 0.
  numberToStrip = new goog.string.StringBuffer('0090112-3123');
  strippedNumber = new goog.string.StringBuffer('00901123123');
  assertEquals(CCS.FROM_DEFAULT_COUNTRY,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(
          numberToStrip, internationalPrefix));
  assertEquals('The number supplied had a 0 after the match so should not be ' +
               'stripped.',
               strippedNumber.toString(), numberToStrip.toString());
  // Here the 0 is separated by a space from the IDD.
  numberToStrip = new goog.string.StringBuffer('009 0-112-3123');
  assertEquals(CCS.FROM_DEFAULT_COUNTRY,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                          internationalPrefix));
}

function testMaybeExtractCountryCode() {
  var CCS = i18n.phonenumbers.PhoneNumber.CountryCodeSource;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = phoneUtil.getMetadataForRegion(RegionCode.US);
  // Note that for the US, the IDD is 011.
  try {
    /** @type {string} */
    var phoneNumber = '011112-3456789';
    /** @type {string} */
    var strippedNumber = '123456789';
    /** @type {number} */
    var countryCallingCode = 1;
    /** @type {!goog.string.StringBuffer} */
    var numberToFill = new goog.string.StringBuffer();
    assertEquals('Did not extract country calling code ' + countryCallingCode +
                 ' correctly.',
                 countryCallingCode,
                 phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                   numberToFill, true, number));
    assertEquals('Did not figure out CountryCodeSource correctly',
                 CCS.FROM_NUMBER_WITH_IDD,
                 number.getCountryCodeSource());
    // Should strip and normalize national significant number.
    assertEquals('Did not strip off the country calling code correctly.',
                 strippedNumber,
                 numberToFill.toString());
  } catch (e) {
    fail('Should not have thrown an exception: ' + e.toString());
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '+6423456789';
    countryCallingCode = 64;
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Did not extract country calling code ' + countryCallingCode +
                 ' correctly.',
                 countryCallingCode,
                 phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                   numberToFill, true, number));
    assertEquals('Did not figure out CountryCodeSource correctly',
                 CCS.FROM_NUMBER_WITH_PLUS_SIGN,
                 number.getCountryCodeSource());
  } catch (e) {
    fail('Should not have thrown an exception: ' + e.toString());
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '2345-6789';
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Should not have extracted a country calling code - ' +
                 'no international prefix present.',
                 0,
                 phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                   numberToFill, true, number));
    assertEquals('Did not figure out CountryCodeSource correctly',
                 CCS.FROM_DEFAULT_COUNTRY,
                 number.getCountryCodeSource());
  } catch (e) {
    fail('Should not have thrown an exception: ' + e.toString());
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '0119991123456789';
    numberToFill = new goog.string.StringBuffer();
    phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                      numberToFill, true, number);
    fail('Should have thrown an exception, no valid country calling code ' +
         'present.');
  } catch (e) {
    // Expected.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '(1 610) 619 4466';
    countryCallingCode = 1;
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Should have extracted the country calling code of the ' +
                 'region passed in',
                 countryCallingCode,
                 phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                   numberToFill, true, number));
    assertEquals('Did not figure out CountryCodeSource correctly',
                 CCS.FROM_NUMBER_WITHOUT_PLUS_SIGN,
                 number.getCountryCodeSource());
  } catch (e) {
    fail('Should not have thrown an exception: ' + e.toString());
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '(1 610) 619 4466';
    countryCallingCode = 1;
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Should have extracted the country calling code of the ' +
                 'region passed in',
                 countryCallingCode,
                 phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                   numberToFill, false,
                                                   number));
    assertFalse('Should not contain CountryCodeSource.',
                number.hasCountryCodeSource());
  } catch (e) {
    fail('Should not have thrown an exception: ' + e.toString());
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '(1 610) 619 446';
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Should not have extracted a country calling code - invalid ' +
                 'number after extraction of uncertain country calling code.',
                 0,
                 phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                   numberToFill, false,
                                                   number));
    assertFalse('Should not contain CountryCodeSource.',
                number.hasCountryCodeSource());
  } catch (e) {
    fail('Should not have thrown an exception: ' + e.toString());
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '(1 610) 619';
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Should not have extracted a country calling code - too ' +
                 'short number both before and after extraction of uncertain ' +
                 'country calling code.',
                 0,
                 phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                   numberToFill, true, number));
    assertEquals('Did not figure out CountryCodeSource correctly',
                 CCS.FROM_DEFAULT_COUNTRY,
                 number.getCountryCodeSource());
  } catch (e) {
    fail('Should not have thrown an exception: ' + e.toString());
  }
}

function testParseNationalNumber() {
  // National prefix attached.
  assertTrue(NZ_NUMBER.equals(phoneUtil.parse('033316005', RegionCode.NZ)));
  assertTrue(NZ_NUMBER.equals(phoneUtil.parse('33316005', RegionCode.NZ)));
  // National prefix attached and some formatting present.
  assertTrue(NZ_NUMBER.equals(phoneUtil.parse('03-331 6005', RegionCode.NZ)));
  assertTrue(NZ_NUMBER.equals(phoneUtil.parse('03 331 6005', RegionCode.NZ)));

  // Testing international prefixes.
  // Should strip country calling code.
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('0064 3 331 6005', RegionCode.NZ)));
  // Try again, but this time we have an international number with Region Code
  // US. It should recognise the country calling code and parse accordingly.
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('01164 3 331 6005', RegionCode.US)));
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('+64 3 331 6005', RegionCode.US)));
  // We should ignore the leading plus here, since it is not followed by a valid
  // country code but instead is followed by the IDD for the US.
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('+01164 3 331 6005', RegionCode.US)));
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('+0064 3 331 6005', RegionCode.NZ)));
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('+ 00 64 3 331 6005', RegionCode.NZ)));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(64123456);
  assertTrue(nzNumber.equals(phoneUtil.parse('64(0)64123456', RegionCode.NZ)));
  // Check that using a '/' is fine in a phone number.
  assertTrue(DE_NUMBER.equals(phoneUtil.parse('301/23456', RegionCode.DE)));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  // Check it doesn't use the '1' as a country calling code when parsing if the
  // phone number was already possible.
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(1234567890);
  assertTrue(usNumber.equals(phoneUtil.parse('123-456-7890', RegionCode.US)));
}

function testParseNumberWithAlphaCharacters() {
  // Test case with alpha characters.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tollfreeNumber = new i18n.phonenumbers.PhoneNumber();
  tollfreeNumber.setCountryCode(64);
  tollfreeNumber.setNationalNumber(800332005);
  assertTrue(tollfreeNumber.equals(
      phoneUtil.parse('0800 DDA 005', RegionCode.NZ)));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var premiumNumber = new i18n.phonenumbers.PhoneNumber();
  premiumNumber.setCountryCode(64);
  premiumNumber.setNationalNumber(9003326005);
  assertTrue(premiumNumber.equals(
      phoneUtil.parse('0900 DDA 6005', RegionCode.NZ)));
  // Not enough alpha characters for them to be considered intentional, so they
  // are stripped.
  assertTrue(premiumNumber.equals(
      phoneUtil.parse('0900 332 6005a', RegionCode.NZ)));
  assertTrue(premiumNumber.equals(
      phoneUtil.parse('0900 332 600a5', RegionCode.NZ)));
  assertTrue(premiumNumber.equals(
      phoneUtil.parse('0900 332 600A5', RegionCode.NZ)));
  assertTrue(premiumNumber.equals(
      phoneUtil.parse('0900 a332 600A5', RegionCode.NZ)));
}

function testParseWithInternationalPrefixes() {
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('+1 (650) 253-0000', RegionCode.NZ)));
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('1-650-253-0000', RegionCode.US)));
  // Calling the US number from Singapore by using different service providers
  // 1st test: calling using SingTel IDD service (IDD is 001)
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('0011-650-253-0000', RegionCode.SG)));
  // 2nd test: calling using StarHub IDD service (IDD is 008)
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('0081-650-253-0000', RegionCode.SG)));
  // 3rd test: calling using SingTel V019 service (IDD is 019)
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('0191-650-253-0000', RegionCode.SG)));
  // Calling the US number from Poland
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('0~01-650-253-0000', RegionCode.PL)));
  // Using '++' at the start.
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('++1 (650) 253-0000', RegionCode.PL)));
}

function testParseNonAscii() {
  // Using a full-width plus sign.
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('\uFF0B1 (650) 253-0000', RegionCode.SG)));
  // The whole number, including punctuation, is here represented in full-width
  // form.
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('\uFF0B\uFF11\u3000\uFF08\uFF16\uFF15\uFF10\uFF09' +
                      '\u3000\uFF12\uFF15\uFF13\uFF0D\uFF10\uFF10\uFF10\uFF10',
                      RegionCode.SG)));
  // Using U+30FC dash instead.
  assertTrue(US_NUMBER.equals(
      phoneUtil.parse('\uFF0B\uFF11\u3000\uFF08\uFF16\uFF15\uFF10\uFF09' +
                      '\u3000\uFF12\uFF15\uFF13\u30FC\uFF10\uFF10\uFF10\uFF10',
                      RegionCode.SG)));

  // Using a very strange decimal digit range (Mongolian digits).
  // TODO(user): Support Mongolian digits
  // assertTrue(US_NUMBER.equals(
  //     phoneUtil.parse('\u1811 \u1816\u1815\u1810 ' +
  //                     '\u1812\u1815\u1813 \u1810\u1810\u1810\u1810',
  //                     RegionCode.US)));
}

function testParseWithLeadingZero() {
  assertTrue(
      IT_NUMBER.equals(phoneUtil.parse('+39 02-36618 300', RegionCode.NZ)));
  assertTrue(
      IT_NUMBER.equals(phoneUtil.parse('02-36618 300', RegionCode.IT)));

  assertTrue(
      IT_MOBILE.equals(phoneUtil.parse('345 678 901', RegionCode.IT)));
}

function testParseNationalNumberArgentina() {
  // Test parsing mobile numbers of Argentina.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(93435551212);
  assertTrue(
      arNumber.equals(phoneUtil.parse('+54 9 343 555 1212', RegionCode.AR)));
  assertTrue(
      arNumber.equals(phoneUtil.parse('0343 15 555 1212', RegionCode.AR)));

  arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(93715654320);
  assertTrue(
      arNumber.equals(phoneUtil.parse('+54 9 3715 65 4320', RegionCode.AR)));
  assertTrue(
      arNumber.equals(phoneUtil.parse('03715 15 65 4320', RegionCode.AR)));
  assertTrue(
      AR_MOBILE.equals(phoneUtil.parse('911 876 54321', RegionCode.AR)));

  // Test parsing fixed-line numbers of Argentina.
  assertTrue(
      AR_NUMBER.equals(phoneUtil.parse('+54 11 8765 4321', RegionCode.AR)));
  assertTrue(
      AR_NUMBER.equals(phoneUtil.parse('011 8765 4321', RegionCode.AR)));

  arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(3715654321);
  assertTrue(
      arNumber.equals(phoneUtil.parse('+54 3715 65 4321', RegionCode.AR)));
  assertTrue(
      arNumber.equals(phoneUtil.parse('03715 65 4321', RegionCode.AR)));

  arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(2312340000);
  assertTrue(
      arNumber.equals(phoneUtil.parse('+54 23 1234 0000', RegionCode.AR)));
  assertTrue(
      arNumber.equals(phoneUtil.parse('023 1234 0000', RegionCode.AR)));
}

function testParseWithXInNumber() {
  // Test that having an 'x' in the phone number at the start is ok and that it
  // just gets removed.
  assertTrue(
      AR_NUMBER.equals(phoneUtil.parse('01187654321', RegionCode.AR)));
  assertTrue(
      AR_NUMBER.equals(phoneUtil.parse('(0) 1187654321', RegionCode.AR)));
  assertTrue(
      AR_NUMBER.equals(phoneUtil.parse('0 1187654321', RegionCode.AR)));
  assertTrue(
      AR_NUMBER.equals(phoneUtil.parse('(0xx) 1187654321', RegionCode.AR)));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arFromUs = new i18n.phonenumbers.PhoneNumber();
  arFromUs.setCountryCode(54);
  arFromUs.setNationalNumber(81429712);
  // This test is intentionally constructed such that the number of digit after
  // xx is larger than 7, so that the number won't be mistakenly treated as an
  // extension, as we allow extensions up to 7 digits. This assumption is okay
  // for now as all the countries where a carrier selection code is written in
  // the form of xx have a national significant number of length larger than 7.
  assertTrue(
      arFromUs.equals(phoneUtil.parse('011xx5481429712', RegionCode.US)));
}

function testParseNumbersMexico() {
  // Test parsing fixed-line numbers of Mexico.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var mxNumber = new i18n.phonenumbers.PhoneNumber();
  mxNumber.setCountryCode(52);
  mxNumber.setNationalNumber(4499780001);
  assertTrue(mxNumber.equals(
      phoneUtil.parse('+52 (449)978-0001', RegionCode.MX)));
  assertTrue(
      mxNumber.equals(phoneUtil.parse('01 (449)978-0001', RegionCode.MX)));
  assertTrue(
      mxNumber.equals(phoneUtil.parse('(449)978-0001', RegionCode.MX)));

  // Test parsing mobile numbers of Mexico.
  mxNumber = new i18n.phonenumbers.PhoneNumber();
  mxNumber.setCountryCode(52);
  mxNumber.setNationalNumber(13312345678);
  assertTrue(mxNumber.equals(
      phoneUtil.parse('+52 1 33 1234-5678', RegionCode.MX)));
  assertTrue(mxNumber.equals(
      phoneUtil.parse('044 (33) 1234-5678', RegionCode.MX)));
  assertTrue(mxNumber.equals(
      phoneUtil.parse('045 33 1234-5678', RegionCode.MX)));
}

function testFailedParseOnInvalidNumbers() {
  try {
    /** @type {string} */
    var sentencePhoneNumber = 'This is not a phone number';
    phoneUtil.parse(sentencePhoneNumber, RegionCode.NZ);
    fail('This should not parse without throwing an exception ' +
         sentencePhoneNumber);
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.NOT_A_NUMBER,
                 e);
  }
  try {
    /** @type {string} */
    var tooLongPhoneNumber = '01495 72553301873 810104';
    phoneUtil.parse(tooLongPhoneNumber, RegionCode.GB);
    fail('This should not parse without throwing an exception ' +
         tooLongPhoneNumber);
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_LONG,
                 e);
  }
  try {
    /** @type {string} */
    var plusMinusPhoneNumber = '+---';
    phoneUtil.parse(plusMinusPhoneNumber, RegionCode.DE);
    fail('This should not parse without throwing an exception ' +
         plusMinusPhoneNumber);
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.NOT_A_NUMBER,
                 e);
  }
  try {
    /** @type {string} */
    var tooShortPhoneNumber = '+49 0';
    phoneUtil.parse(tooShortPhoneNumber, RegionCode.DE);
    fail('This should not parse without throwing an exception ' +
         tooShortPhoneNumber);
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_NSN,
                 e);
  }
  try {
    /** @type {string} */
    var invalidCountryCode = '+210 3456 56789';
    phoneUtil.parse(invalidCountryCode, RegionCode.NZ);
    fail('This is not a recognised region code: should fail: ' +
         invalidCountryCode);
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    /** @type {string} */
    var plusAndIddAndInvalidCountryCode = '+ 00 210 3 331 6005';
    phoneUtil.parse(plusAndIddAndInvalidCountryCode, RegionCode.NZ);
    fail('This should not parse without throwing an exception.');
  } catch (e) {
    // Expected this exception. 00 is a correct IDD, but 210 is not a valid
    // country code.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    /** @type {string} */
    var someNumber = '123 456 7890';
    phoneUtil.parse(someNumber, RegionCode.ZZ);
    fail('Unknown region code not allowed: should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    /** @type {string} */
    someNumber = '123 456 7890';
    phoneUtil.parse(someNumber, RegionCode.CS);
    fail('Deprecated region code not allowed: should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    someNumber = '123 456 7890';
    phoneUtil.parse(someNumber, null);
    fail('Null region code not allowed: should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    someNumber = '0044------';
    phoneUtil.parse(someNumber, RegionCode.GB);
    fail('No number provided, only region code: should fail');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD,
                 e);
  }
  try {
    someNumber = '0044';
    phoneUtil.parse(someNumber, RegionCode.GB);
    fail('No number provided, only region code: should fail');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD,
                 e);
  }
  try {
    someNumber = '011';
    phoneUtil.parse(someNumber, RegionCode.US);
    fail('Only IDD provided - should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD,
                 e);
  }
  try {
    someNumber = '0119';
    phoneUtil.parse(someNumber, RegionCode.US);
    fail('Only IDD provided and then 9 - should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD,
                 e);
  }
  try {
    /** @type {string} */
    var emptyNumber = '';
    // Invalid region.
    phoneUtil.parse(emptyNumber, RegionCode.ZZ);
    fail('Empty string - should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.NOT_A_NUMBER,
                 e);
  }
  try {
    // Invalid region.
    phoneUtil.parse(null, RegionCode.ZZ);
    fail('Null string - should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.NOT_A_NUMBER,
                 e);
  }
  try {
    phoneUtil.parse(null, RegionCode.US);
    fail('Null string - should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.NOT_A_NUMBER,
                 e);
  }
}

function testParseNumbersWithPlusWithNoRegion() {
  // RegionCode.ZZ is allowed only if the number starts with a '+' - then the
  // country calling code can be calculated.
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('+64 3 331 6005', RegionCode.ZZ)));
  // Test with full-width plus.
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('\uFF0B64 3 331 6005', RegionCode.ZZ)));
  // Test with normal plus but leading characters that need to be stripped.
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('Tel: +64 3 331 6005', RegionCode.ZZ)));
  assertTrue(
      NZ_NUMBER.equals(phoneUtil.parse('+64 3 331 6005', null)));

  // It is important that we set the carrier code to an empty string, since we
  // used ParseAndKeepRawInput and no carrier code was found.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumberWithRawInput = NZ_NUMBER.clone();
  nzNumberWithRawInput.setRawInput('+64 3 331 6005');
  nzNumberWithRawInput.setCountryCodeSource(i18n.phonenumbers.PhoneNumber
      .CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN);
  nzNumberWithRawInput.setPreferredDomesticCarrierCode('');
  assertTrue(nzNumberWithRawInput.equals(
      phoneUtil.parseAndKeepRawInput('+64 3 331 6005', RegionCode.ZZ)));
  // Null is also allowed for the region code in these cases.
  assertTrue(nzNumberWithRawInput.equals(
      phoneUtil.parseAndKeepRawInput('+64 3 331 6005', null)));
}

function testParseExtensions() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(33316005);
  nzNumber.setExtension('3456');
  assertTrue(nzNumber.equals(
      phoneUtil.parse('03 331 6005 ext 3456', RegionCode.NZ)));
  assertTrue(nzNumber.equals(
      phoneUtil.parse('03-3316005x3456', RegionCode.NZ)));
  assertTrue(nzNumber.equals(
      phoneUtil.parse('03-3316005 int.3456', RegionCode.NZ)));
  assertTrue(nzNumber.equals(
      phoneUtil.parse('03 3316005 #3456', RegionCode.NZ)));

  // Test the following do not extract extensions:
  assertTrue(ALPHA_NUMERIC_NUMBER.equals(
      phoneUtil.parse('1800 six-flags', RegionCode.US)));
  assertTrue(ALPHA_NUMERIC_NUMBER.equals(
      phoneUtil.parse('1800 SIX FLAGS', RegionCode.US)));
  assertTrue(ALPHA_NUMERIC_NUMBER.equals(
      phoneUtil.parse('0~0 1800 7493 5247', RegionCode.PL)));
  assertTrue(ALPHA_NUMERIC_NUMBER.equals(
      phoneUtil.parse('(1800) 7493.5247', RegionCode.US)));

  // Check that the last instance of an extension token is matched.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var extnNumber = ALPHA_NUMERIC_NUMBER.clone();
  extnNumber.setExtension('1234');
  assertTrue(extnNumber.equals(
      phoneUtil.parse('0~0 1800 7493 5247 ~1234', RegionCode.PL)));

  // Verifying bug-fix where the last digit of a number was previously omitted
  // if it was a 0 when extracting the extension. Also verifying a few different
  // cases of extensions.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var ukNumber = new i18n.phonenumbers.PhoneNumber();
  ukNumber.setCountryCode(44);
  ukNumber.setNationalNumber(2034567890);
  ukNumber.setExtension('456');
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44 2034567890x456', RegionCode.NZ)));
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44 2034567890x456', RegionCode.GB)));
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44 2034567890 x456', RegionCode.GB)));
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44 2034567890 X456', RegionCode.GB)));
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44 2034567890 X 456', RegionCode.GB)));
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44 2034567890 X  456', RegionCode.GB)));
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44 2034567890 x 456  ', RegionCode.GB)));
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44 2034567890  X 456', RegionCode.GB)));
  assertTrue(ukNumber.equals(
      phoneUtil.parse('+44-2034567890;ext=456', RegionCode.GB)));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usWithExtension = new i18n.phonenumbers.PhoneNumber();
  usWithExtension.setCountryCode(1);
  usWithExtension.setNationalNumber(8009013355);
  usWithExtension.setExtension('7246433');
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('(800) 901-3355 x 7246433', RegionCode.US)));
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('(800) 901-3355 , ext 7246433', RegionCode.US)));
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('(800) 901-3355 ,extension 7246433', RegionCode.US)));
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('(800) 901-3355 ,extensi\u00F3n 7246433',
      RegionCode.US)));
  // Repeat with the small letter o with acute accent created by combining
  // characters.
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('(800) 901-3355 ,extensio\u0301n 7246433',
      RegionCode.US)));
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('(800) 901-3355 , 7246433', RegionCode.US)));
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('(800) 901-3355 ext: 7246433', RegionCode.US)));

  // Test that if a number has two extensions specified, we ignore the second.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usWithTwoExtensionsNumber = new i18n.phonenumbers.PhoneNumber();
  usWithTwoExtensionsNumber.setCountryCode(1);
  usWithTwoExtensionsNumber.setNationalNumber(2121231234);
  usWithTwoExtensionsNumber.setExtension('508');
  assertTrue(usWithTwoExtensionsNumber.equals(
      phoneUtil.parse('(212)123-1234 x508/x1234', RegionCode.US)));
  assertTrue(usWithTwoExtensionsNumber.equals(
      phoneUtil.parse('(212)123-1234 x508/ x1234', RegionCode.US)));
  assertTrue(usWithTwoExtensionsNumber.equals(
      phoneUtil.parse('(212)123-1234 x508\\x1234', RegionCode.US)));

  // Test parsing numbers in the form (645) 123-1234-910# works, where the last
  // 3 digits before the # are an extension.
  usWithExtension = new i18n.phonenumbers.PhoneNumber();
  usWithExtension.setCountryCode(1);
  usWithExtension.setNationalNumber(6451231234);
  usWithExtension.setExtension('910');
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('+1 (645) 123 1234-910#', RegionCode.US)));
  // Retry with the same number in a slightly different format.
  assertTrue(usWithExtension.equals(
      phoneUtil.parse('+1 (645) 123 1234 ext. 910#', RegionCode.US)));
}

function testParseAndKeepRaw() {
  var CCS = i18n.phonenumbers.PhoneNumber.CountryCodeSource;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var alphaNumericNumber = ALPHA_NUMERIC_NUMBER.clone();
  alphaNumericNumber.setRawInput('800 six-flags');
  alphaNumericNumber.setCountryCodeSource(CCS.FROM_DEFAULT_COUNTRY);
  alphaNumericNumber.setPreferredDomesticCarrierCode('');
  assertTrue(alphaNumericNumber.equals(
      phoneUtil.parseAndKeepRawInput('800 six-flags', RegionCode.US)));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var shorterAlphaNumber = new i18n.phonenumbers.PhoneNumber();
  shorterAlphaNumber.setCountryCode(1);
  shorterAlphaNumber.setNationalNumber(8007493524);
  shorterAlphaNumber.setRawInput('1800 six-flag');
  shorterAlphaNumber.setCountryCodeSource(CCS.FROM_NUMBER_WITHOUT_PLUS_SIGN);
  shorterAlphaNumber.setPreferredDomesticCarrierCode('');
  assertTrue(shorterAlphaNumber.equals(
      phoneUtil.parseAndKeepRawInput('1800 six-flag', RegionCode.US)));

  shorterAlphaNumber.setRawInput('+1800 six-flag');
  shorterAlphaNumber.setCountryCodeSource(CCS.FROM_NUMBER_WITH_PLUS_SIGN);
  assertTrue(shorterAlphaNumber.equals(
      phoneUtil.parseAndKeepRawInput('+1800 six-flag', RegionCode.NZ)));

  alphaNumericNumber.setCountryCode(1);
  alphaNumericNumber.setNationalNumber(8007493524);
  alphaNumericNumber.setRawInput('001800 six-flag');
  alphaNumericNumber.setCountryCodeSource(CCS.FROM_NUMBER_WITH_IDD);
  assertTrue(alphaNumericNumber.equals(
      phoneUtil.parseAndKeepRawInput('001800 six-flag', RegionCode.NZ)));

  // Invalid region code supplied.
  try {
    phoneUtil.parseAndKeepRawInput('123 456 7890', RegionCode.CS);
    fail('Deprecated region code not allowed: should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var koreanNumber = new i18n.phonenumbers.PhoneNumber();
  koreanNumber.setCountryCode(82);
  koreanNumber.setNationalNumber(22123456);
  koreanNumber.setRawInput('08122123456');
  koreanNumber.setCountryCodeSource(CCS.FROM_DEFAULT_COUNTRY);
  koreanNumber.setPreferredDomesticCarrierCode('81');
  assertTrue(koreanNumber.equals(
      phoneUtil.parseAndKeepRawInput('08122123456', RegionCode.KR)));
}

function testCountryWithNoNumberDesc() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  var PNT = i18n.phonenumbers.PhoneNumberType;
  // Andorra is a country where we don't have PhoneNumberDesc info in the
  // metadata.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var adNumber = new i18n.phonenumbers.PhoneNumber();
  adNumber.setCountryCode(376);
  adNumber.setNationalNumber(12345);
  assertEquals('+376 12345', phoneUtil.format(adNumber, PNF.INTERNATIONAL));
  assertEquals('+37612345', phoneUtil.format(adNumber, PNF.E164));
  assertEquals('12345', phoneUtil.format(adNumber, PNF.NATIONAL));
  assertEquals(PNT.UNKNOWN, phoneUtil.getNumberType(adNumber));
  assertTrue(phoneUtil.isValidNumber(adNumber));

  // Test dialing a US number from within Andorra.
  assertEquals('00 1 650 253 0000',
               phoneUtil.formatOutOfCountryCallingNumber(US_NUMBER,
                                                         RegionCode.AD));
}

function testUnknownCountryCallingCodeForValidation() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var invalidNumber = new i18n.phonenumbers.PhoneNumber();
  invalidNumber.setCountryCode(0);
  invalidNumber.setNationalNumber(1234);
  assertFalse(phoneUtil.isValidNumber(invalidNumber));
}

function testIsNumberMatchMatches() {
  // Test simple matches where formatting is different, or leading zeroes,
  // or country calling code has been specified.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var num1 = phoneUtil.parse('+64 3 331 6005', RegionCode.NZ);
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var num2 = phoneUtil.parse('+64 03 331 6005', RegionCode.NZ);
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(num1, num2));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch('+64 3 331 6005', '+64 03 331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch('+64 03 331-6005', '+64 03331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch('+643 331-6005', '+64033316005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch('+643 331-6005', '+6433316005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '+6433316005'));
  // Test alpha numbers.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch('+1800 siX-Flags', '+1 800 7493 5247'));
  // Test numbers with extensions.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005 extn 1234',
                                       '+6433316005#1234'));
  // Test proto buffers.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(NZ_NUMBER, '+6403 331 6005'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = NZ_NUMBER.clone();
  nzNumber.setExtension('3456');
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(nzNumber, '+643 331 6005 ext 3456'));
  // Check empty extensions are ignored.
  nzNumber.setExtension('');
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(nzNumber, '+6403 331 6005'));
  // Check variant with two proto buffers.
  assertEquals('Numbers did not match',
               i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(nzNumber, NZ_NUMBER));

  var CCS = i18n.phonenumbers.PhoneNumber.CountryCodeSource;
  // Check raw_input, country_code_source and preferred_domestic_carrier_code
  // are ignored.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var brNumberOne = new i18n.phonenumbers.PhoneNumber();
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var brNumberTwo = new i18n.phonenumbers.PhoneNumber();
  brNumberOne.setCountryCode(55);
  brNumberOne.setNationalNumber(3121286979);
  brNumberOne.setCountryCodeSource(CCS.FROM_NUMBER_WITH_PLUS_SIGN);
  brNumberOne.setPreferredDomesticCarrierCode('12');
  brNumberOne.setRawInput('012 3121286979');
  brNumberTwo.setCountryCode(55);
  brNumberTwo.setNationalNumber(3121286979);
  brNumberTwo.setCountryCodeSource(CCS.FROM_DEFAULT_COUNTRY);
  brNumberTwo.setPreferredDomesticCarrierCode('14');
  brNumberTwo.setRawInput('143121286979');
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(brNumberOne, brNumberTwo));
}

function testIsNumberMatchNonMatches() {
  // Non-matches.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH,
               phoneUtil.isNumberMatch('03 331 6005', '03 331 6006'));
  // Different country calling code, partial number match.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '+16433316005'));
  // Different country calling code, same number.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '+6133316005'));
  // Extension different, all else the same.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005 extn 1234',
                                       '0116433316005#1235'));
  // NSN matches, but extension is different - not the same number.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005 ext.1235',
                                       '3 331 6005#1234'));

  // Invalid numbers that can't be parsed.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NOT_A_NUMBER,
               phoneUtil.isNumberMatch('43', '3 331 6043'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NOT_A_NUMBER,
               phoneUtil.isNumberMatch('+43', '+64 3 331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NOT_A_NUMBER,
               phoneUtil.isNumberMatch('+43', '64 3 331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NOT_A_NUMBER,
               phoneUtil.isNumberMatch('Dog', '64 3 331 6005'));
}

function testIsNumberMatchNsnMatches() {
  // NSN matches.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '03 331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch(NZ_NUMBER, '03 331 6005'));
  // Here the second number possibly starts with the country calling code for
  // New Zealand, although we are unsure.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var unchangedNzNumber = NZ_NUMBER.clone();
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch(unchangedNzNumber, '(64-3) 331 6005'));
  // Check the phone number proto was not edited during the method call.
  assertTrue(NZ_NUMBER.equals(unchangedNzNumber));

  // Here, the 1 might be a national prefix, if we compare it to the US number,
  // so the resultant match is an NSN match.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch(US_NUMBER, '1-650-253-0000'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch(US_NUMBER, '6502530000'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch('+1 650-253 0000', '1 650 253 0000'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch('1 650-253 0000', '1 650 253 0000'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch('1 650-253 0000', '+1 650 253 0000'));
  // For this case, the match will be a short NSN match, because we cannot
  // assume that the 1 might be a national prefix, so don't remove it when
  // parsing.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var randomNumber = new i18n.phonenumbers.PhoneNumber();
  randomNumber.setCountryCode(41);
  randomNumber.setNationalNumber(6502530000);
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch(randomNumber, '1-650-253-0000'));
}

function testIsNumberMatchShortNsnMatches() {
  // Short NSN matches with the country not specified for either one or both
  // numbers.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '331 6005'));
  // We did not know that the '0' was a national prefix since neither number has
  // a country code, so this is considered a SHORT_NSN_MATCH.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('3 331-6005', '03 331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('3 331-6005', '331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('3 331-6005', '+64 331 6005'));
  // Short NSN match with the country specified.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('03 331-6005', '331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('1 234 345 6789', '345 6789'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('+1 (234) 345 6789', '345 6789'));
  // NSN matches, country calling code omitted for one number, extension missing
  // for one.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '3 331 6005#1234'));
  // One has Italian leading zero, one does not.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var italianNumberOne = new i18n.phonenumbers.PhoneNumber();
  italianNumberOne.setCountryCode(39);
  italianNumberOne.setNationalNumber(1234);
  italianNumberOne.setItalianLeadingZero(true);
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var italianNumberTwo = new i18n.phonenumbers.PhoneNumber();
  italianNumberTwo.setCountryCode(39);
  italianNumberTwo.setNationalNumber(1234);
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch(italianNumberOne, italianNumberTwo));
  // One has an extension, the other has an extension of ''.
  italianNumberOne.setExtension('1234');
  italianNumberOne.clearItalianLeadingZero();
  italianNumberTwo.setExtension('');
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch(italianNumberOne, italianNumberTwo));
}

function testCanBeInternationallyDialled() {
  // We have no-international-dialling rules for the US in our test metadata
  // that say that toll-free numbers cannot be dialled internationally.
  assertFalse(phoneUtil.canBeInternationallyDialled(US_TOLLFREE));

  // Normal US numbers can be internationally dialled.
  assertTrue(phoneUtil.canBeInternationallyDialled(US_NUMBER));

  // Invalid number.
  assertTrue(phoneUtil.canBeInternationallyDialled(US_LOCAL_NUMBER));

  // We have no data for NZ - should return true.
  assertTrue(phoneUtil.canBeInternationallyDialled(NZ_NUMBER));
}

function testIsAlphaNumber() {
  assertTrue(phoneUtil.isAlphaNumber('1800 six-flags'));
  assertTrue(phoneUtil.isAlphaNumber('1800 six-flags ext. 1234'));
  assertFalse(phoneUtil.isAlphaNumber('1800 123-1234'));
  assertFalse(phoneUtil.isAlphaNumber('1800 123-1234 extension: 1234'));
}
