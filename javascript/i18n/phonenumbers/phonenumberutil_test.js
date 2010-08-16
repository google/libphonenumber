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
 * @fileoverview  Unit tests for the PhoneNumberUtil.
 *
 * @author Nikolaos Trogkanis
 */

goog.require('goog.testing.jsunit');
goog.require('i18n.phonenumbers.PhoneNumberUtil');

/** @type {i18n.phonenumbers.PhoneNumberUtil} */
var phoneUtil = i18n.phonenumbers.PhoneNumberUtil.getInstance();

function testGetInstanceLoadUSMetadata() {
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = phoneUtil.getMetadataForRegion('US');
  assertEquals('US', metadata.getId());
  assertEquals(1, metadata.getCountryCode());
  assertEquals('011', metadata.getInternationalPrefix());
  assertTrue(metadata.hasNationalPrefix());
  assertEquals(2, metadata.numberFormatCount());
  assertEquals('(\\d{3})(\\d{3})(\\d{4})',
               metadata.getNumberFormat(0).getPattern());
  assertEquals('$1 $2 $3', metadata.getNumberFormat(0).getFormat());
  assertEquals('[13-9]\\d{9}|2[0-35-9]\\d{8}',
               metadata.getGeneralDesc().getNationalNumberPattern());
  assertEquals('\\d{7,10}',
               metadata.getGeneralDesc().getPossibleNumberPattern());
  assertTrue(metadata.getGeneralDesc().exactlySameAs(metadata.getFixedLine()));
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
  var metadata = phoneUtil.getMetadataForRegion('DE');
  assertEquals('DE', metadata.getId());
  assertEquals(49, metadata.getCountryCode());
  assertEquals('00', metadata.getInternationalPrefix());
  assertEquals('0', metadata.getNationalPrefix());
  assertEquals(5, metadata.numberFormatCount());
  assertEquals(1, metadata.getNumberFormat(4).leadingDigitsPatternCount());
  assertEquals('900', metadata.getNumberFormat(4).getLeadingDigitsPattern(0));
  assertEquals('(\\d{3})(\\d{3,4})(\\d{4})',
               metadata.getNumberFormat(4).getPattern());
  assertEquals('$1 $2 $3', metadata.getNumberFormat(4).getFormat());
  assertEquals('(?:[24-6]\\d{2}|3[03-9]\\d|[789](?:[1-9]\\d|0[2-9]))\\d{3,8}',
               metadata.getFixedLine().getNationalNumberPattern());
  assertEquals('\\d{2,14}', metadata.getFixedLine().getPossibleNumberPattern());
  assertEquals('30123456', metadata.getFixedLine().getExampleNumber());
  assertEquals('\\d{10}', metadata.getTollFree().getPossibleNumberPattern());
  assertEquals('900([135]\\d{6}|9\\d{7})',
               metadata.getPremiumRate().getNationalNumberPattern());
}

function testGetInstanceLoadARMetadata() {
  /** @type {i18n.phonenumbers.PhoneMetadata} */
  var metadata = phoneUtil.getMetadataForRegion('AR');
  assertEquals('AR', metadata.getId());
  assertEquals(54, metadata.getCountryCode());
  assertEquals('00', metadata.getInternationalPrefix());
  assertEquals('0', metadata.getNationalPrefix());
  assertEquals('0(?:(11|343|3715)15)?', metadata.getNationalPrefixForParsing());
  assertEquals('9$1', metadata.getNationalPrefixTransformRule());
  assertEquals('$1 15 $2-$3', metadata.getNumberFormat(2).getFormat());
  assertEquals('9(\\d{4})(\\d{2})(\\d{4})',
               metadata.getNumberFormat(3).getPattern());
  assertEquals('(9)(\\d{4})(\\d{2})(\\d{4})',
               metadata.getIntlNumberFormat(3).getPattern());
  assertEquals('$1 $2 $3 $4', metadata.getIntlNumberFormat(3).getFormat());
}

function testGetLengthOfGeographicalAreaCode() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  // Google MTV, which has area code '650'.
  number.setCountryCode(1);
  number.setNationalNumber(6502530000);
  assertEquals(3, phoneUtil.getLengthOfGeographicalAreaCode(number));

  // A North America toll-free number, which has no area code.
  number.setCountryCode(1);
  number.setNationalNumber(8002530000);
  assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(number));

  // An invalid US number (1 digit shorter), which has no area code.
  number.setCountryCode(1);
  number.setNationalNumber(650253000);
  assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(number));

  // Google London, which has area code '20'.
  number.setCountryCode(44);
  number.setNationalNumber(2070313000);
  assertEquals(2, phoneUtil.getLengthOfGeographicalAreaCode(number));

  // A UK mobile phone, which has no area code.
  number.setCountryCode(44);
  number.setNationalNumber(7123456789);
  assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(number));

  // Google Buenos Aires, which has area code '11'.
  number.setCountryCode(54);
  number.setNationalNumber(1155303000);
  assertEquals(2, phoneUtil.getLengthOfGeographicalAreaCode(number));

  // Google Sydney, which has area code '2'.
  number.setCountryCode(61);
  number.setNationalNumber(293744000);
  assertEquals(1, phoneUtil.getLengthOfGeographicalAreaCode(number));

  // Google Singapore. Singapore has no area code and no national prefix.
  number.setCountryCode(65);
  number.setNationalNumber(65218000);
  assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(number));
}

function testGetNationalSignificantNumber() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(6502530000);
  assertEquals('6502530000',
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number));

  // An Italian mobile number.
  number.setCountryCode(39);
  number.setNationalNumber(312345678);
  assertEquals('312345678',
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number));

  // An Italian fixed line number.
  number.setCountryCode(39);
  number.setNationalNumber(236618300);
  number.setItalianLeadingZero(true);
  assertEquals('0236618300',
      i18n.phonenumbers.PhoneNumberUtil.getNationalSignificantNumber(number));
}

function testGetExampleNumber() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(30123456);
  assertTrue(deNumber.exactlySameAs(phoneUtil.getExampleNumber('DE')));
  assertTrue(deNumber.exactlySameAs(phoneUtil.getExampleNumber('de')));

  assertTrue(deNumber.exactlySameAs(
      phoneUtil.getExampleNumberForType('DE', PNT.FIXED_LINE)));
  assertNull(phoneUtil.getExampleNumberForType('DE', PNT.MOBILE));
  // For the US, the example number is placed under general description, and
  // hence should be used for both fixed line and mobile, so neither of these
  // should return null.
  assertNotNull(phoneUtil.getExampleNumberForType('US', PNT.FIXED_LINE));
  assertNotNull(phoneUtil.getExampleNumberForType('US', PNT.MOBILE));
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
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6502530000);
  assertEquals('650 253 0000',
               phoneUtil.format(usNumber, PNF.NATIONAL));
  assertEquals('+1 650 253 0000',
               phoneUtil.format(usNumber, PNF.INTERNATIONAL));

  usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(8002530000);
  assertEquals('800 253 0000',
               phoneUtil.format(usNumber, PNF.NATIONAL));
  assertEquals('+1 800 253 0000',
               phoneUtil.format(usNumber, PNF.INTERNATIONAL));

  usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(9002530000);
  assertEquals('900 253 0000',
               phoneUtil.format(usNumber, PNF.NATIONAL));
  assertEquals('+1 900 253 0000',
               phoneUtil.format(usNumber, PNF.INTERNATIONAL));
}

function testFormatBSNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var bsNumber = new i18n.phonenumbers.PhoneNumber();
  bsNumber.setCountryCode(1);
  bsNumber.setNationalNumber(2421234567);
  assertEquals('242 123 4567',
               phoneUtil.format(bsNumber, PNF.NATIONAL));
  assertEquals('+1 242 123 4567',
               phoneUtil.format(bsNumber, PNF.INTERNATIONAL));

  bsNumber = new i18n.phonenumbers.PhoneNumber();
  bsNumber.setCountryCode(1);
  bsNumber.setNationalNumber(8002530000);
  assertEquals('800 253 0000',
               phoneUtil.format(bsNumber, PNF.NATIONAL));
  assertEquals('+1 800 253 0000',
               phoneUtil.format(bsNumber, PNF.INTERNATIONAL));

  bsNumber = new i18n.phonenumbers.PhoneNumber();
  bsNumber.setCountryCode(1);
  bsNumber.setNationalNumber(9002530000);
  assertEquals('900 253 0000',
               phoneUtil.format(bsNumber, PNF.NATIONAL));
  assertEquals('+1 900 253 0000',
               phoneUtil.format(bsNumber, PNF.INTERNATIONAL));
}

function testFormatGBNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(2087389353);
  assertEquals('(020) 8738 9353',
               phoneUtil.format(gbNumber, PNF.NATIONAL));
  assertEquals('+44 20 8738 9353',
               phoneUtil.format(gbNumber, PNF.INTERNATIONAL));

  gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(7912345678);
  assertEquals('(07912) 345 678',
               phoneUtil.format(gbNumber, PNF.NATIONAL));
  assertEquals('+44 7912 345 678',
               phoneUtil.format(gbNumber, PNF.INTERNATIONAL));
}

function testFormatDENumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(301234);
  assertEquals('030 1234',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 30 1234',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));

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
  deNumber.setNationalNumber(9123123);
  assertEquals('09123 123',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 9123 123',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));

  deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(80212345);
  assertEquals('08021 2345',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 8021 2345',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));

  deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(1234);
  // Note this number is correctly formatted without national prefix.
  // Most of the numbers that are treated as invalid numbers by the library are
  // short numbers, and they are usually not dialed with national prefix.
  assertEquals('1234',
               phoneUtil.format(deNumber, PNF.NATIONAL));
  assertEquals('+49 1234',
               phoneUtil.format(deNumber, PNF.INTERNATIONAL));
}

function testFormatITNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(236618300);
  itNumber.setItalianLeadingZero(true);
  assertEquals('02 3661 8300',
               phoneUtil.format(itNumber, PNF.NATIONAL));
  assertEquals('+39 02 3661 8300',
               phoneUtil.format(itNumber, PNF.INTERNATIONAL));
  assertEquals('+390236618300',
               phoneUtil.format(itNumber, PNF.E164));

  itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(345678901);
  assertEquals('345 678 901',
               phoneUtil.format(itNumber, PNF.NATIONAL));
  assertEquals('+39 345 678 901',
               phoneUtil.format(itNumber, PNF.INTERNATIONAL));
  assertEquals('+39345678901',
               phoneUtil.format(itNumber, PNF.E164));
}

function testFormatAUNumber() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var auNumber = new i18n.phonenumbers.PhoneNumber();
  auNumber.setCountryCode(61);
  auNumber.setNationalNumber(236618300);
  assertEquals('02 3661 8300',
               phoneUtil.format(auNumber, PNF.NATIONAL));
  assertEquals('+61 2 3661 8300',
               phoneUtil.format(auNumber, PNF.INTERNATIONAL));
  assertEquals('+61236618300',
               phoneUtil.format(auNumber, PNF.E164));

  auNumber = new i18n.phonenumbers.PhoneNumber();
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
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(1187654321);
  assertEquals('011 8765-4321',
               phoneUtil.format(arNumber, PNF.NATIONAL));
  assertEquals('+54 11 8765-4321',
               phoneUtil.format(arNumber, PNF.INTERNATIONAL));
  assertEquals('+541187654321',
               phoneUtil.format(arNumber, PNF.E164));

  arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(91187654321);
  assertEquals('011 15 8765-4321',
               phoneUtil.format(arNumber, PNF.NATIONAL));
  assertEquals('+54 9 11 8765 4321',
               phoneUtil.format(arNumber, PNF.INTERNATIONAL));
  assertEquals('+5491187654321',
               phoneUtil.format(arNumber, PNF.E164));
}

function testFormatOutOfCountryCallingNumber() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(9002530000);
  assertEquals('00 1 900 253 0000',
               phoneUtil.formatOutOfCountryCallingNumber(usNumber, 'DE'));

  usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6502530000);
  assertEquals('1 650 253 0000',
               phoneUtil.formatOutOfCountryCallingNumber(usNumber, 'BS'));

  assertEquals('0~0 1 650 253 0000',
               phoneUtil.formatOutOfCountryCallingNumber(usNumber, 'PL'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(7912345678);
  assertEquals('011 44 7912 345 678',
               phoneUtil.formatOutOfCountryCallingNumber(gbNumber, 'US'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(1234);
  assertEquals('00 49 1234',
               phoneUtil.formatOutOfCountryCallingNumber(deNumber, 'GB'));
  // Note this number is correctly formatted without national prefix.
  // Most of the numbers that are treated as invalid numbers by the library are
  // short numbers, and they are usually not dialed with national prefix.
  assertEquals('1234',
               phoneUtil.formatOutOfCountryCallingNumber(deNumber, 'DE'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(236618300);
  itNumber.setItalianLeadingZero(true);
  assertEquals('011 39 02 3661 8300',
               phoneUtil.formatOutOfCountryCallingNumber(itNumber, 'US'));
  assertEquals('02 3661 8300',
               phoneUtil.formatOutOfCountryCallingNumber(itNumber, 'IT'));
  assertEquals('+39 02 3661 8300',
               phoneUtil.formatOutOfCountryCallingNumber(itNumber, 'SG'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var sgNumber = new i18n.phonenumbers.PhoneNumber();
  sgNumber.setCountryCode(65);
  sgNumber.setNationalNumber(94777892);
  assertEquals('9477 7892',
               phoneUtil.formatOutOfCountryCallingNumber(sgNumber, 'SG'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(91187654321);
  assertEquals('011 54 9 11 8765 4321',
               phoneUtil.formatOutOfCountryCallingNumber(arNumber, 'US'));

  arNumber.setExtension('1234');
  assertEquals('011 54 9 11 8765 4321 ext. 1234',
               phoneUtil.formatOutOfCountryCallingNumber(arNumber, 'US'));
  assertEquals('0011 54 9 11 8765 4321 ext. 1234',
               phoneUtil.formatOutOfCountryCallingNumber(arNumber, 'AU'));
  assertEquals('011 15 8765-4321 ext. 1234',
               phoneUtil.formatOutOfCountryCallingNumber(arNumber, 'AR'));
  assertEquals('011 15 8765-4321 ext. 1234',
               phoneUtil.formatOutOfCountryCallingNumber(arNumber, 'ar'));
}

function testFormatOutOfCountryWithPreferredIntlPrefix() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(236618300);
  itNumber.setItalianLeadingZero(true);
  // This should use 0011, since that is the preferred international prefix
  // (both 0011 and 0012 are accepted as possible international prefixes in our
  // test metadta.)
  assertEquals('0011 39 02 3661 8300',
               phoneUtil.formatOutOfCountryCallingNumber(itNumber, 'AU'));
}

function testFormatWithCarrierCode() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  // We only support this for AR in our test metadata.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(91234125678);
  assertEquals('01234 12-5678',
               phoneUtil.format(arNumber, PNF.NATIONAL));
  // Test formatting with a carrier code.
  assertEquals('01234 15 12-5678',
               phoneUtil.formatNationalNumberWithCarrierCode(arNumber, '15'));
  // Here the international rule is used, so no carrier code should be present.
  assertEquals('+5491234125678',
               phoneUtil.format(arNumber, PNF.E164));
  // We don't support this for the US so there should be no change.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(4241231234);
  assertEquals('424 123 1234',
               phoneUtil.format(usNumber, PNF.NATIONAL));
  assertEquals('424 123 1234',
               phoneUtil.formatNationalNumberWithCarrierCode(usNumber, '15'));
}

function testFormatByPattern() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6502530000);
  /** @type {i18n.phonenumbers.NumberFormat} */
  var newNumFormat = new i18n.phonenumbers.NumberFormat();
  newNumFormat.setPattern('(\\d{3})(\\d{3})(\\d{4})');
  newNumFormat.setFormat('($1) $2-$3');
  /** @type {Array.<i18n.phonenumbers.NumberFormat>} */
  var newNumberFormats = [];
  newNumberFormats[0] = newNumFormat;

  assertEquals('(650) 253-0000',
               phoneUtil.formatByPattern(usNumber,
                                         PNF.NATIONAL,
                                         newNumberFormats));
  assertEquals('+1 (650) 253-0000',
               phoneUtil.formatByPattern(usNumber,
                                         PNF.INTERNATIONAL,
                                         newNumberFormats));

  // $NP is set to '1' for the US. Here we check that for other NANPA countries
  // the US rules are followed.
  newNumFormat.setNationalPrefixFormattingRule('$NP ($FG)');
  newNumFormat.setFormat('$1 $2-$3');
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var bsNumber = new i18n.phonenumbers.PhoneNumber();
  bsNumber.setCountryCode(1);
  bsNumber.setNationalNumber(4168819999);
  assertEquals('1 (416) 881-9999',
               phoneUtil.formatByPattern(bsNumber,
                                         PNF.NATIONAL,
                                         newNumberFormats));
  assertEquals('+1 416 881-9999',
               phoneUtil.formatByPattern(bsNumber,
                                         PNF.INTERNATIONAL,
                                         newNumberFormats));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(236618300);
  itNumber.setItalianLeadingZero(true);

  newNumFormat.setPattern('(\\d{2})(\\d{5})(\\d{3})');
  newNumFormat.setFormat('$1-$2 $3');
  newNumberFormats[0] = newNumFormat;

  assertEquals('02-36618 300',
               phoneUtil.formatByPattern(itNumber,
                                         PNF.NATIONAL,
                                         newNumberFormats));
  assertEquals('+39 02-36618 300',
               phoneUtil.formatByPattern(itNumber,
                                         PNF.INTERNATIONAL,
                                         newNumberFormats));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(2012345678);

  newNumFormat.setNationalPrefixFormattingRule('$NP$FG');
  newNumFormat.setPattern('(\\d{2})(\\d{4})(\\d{4})');
  newNumFormat.setFormat('$1 $2 $3');
  newNumberFormats[0] = newNumFormat;
  assertEquals('020 1234 5678',
               phoneUtil.formatByPattern(gbNumber,
                                         PNF.NATIONAL,
                                         newNumberFormats));

  newNumFormat.setNationalPrefixFormattingRule('($NP$FG)');
  newNumberFormats[0] = newNumFormat;
  assertEquals('(020) 1234 5678',
               phoneUtil.formatByPattern(gbNumber,
                                         PNF.NATIONAL,
                                         newNumberFormats));

  newNumFormat.setNationalPrefixFormattingRule('');
  newNumberFormats[0] = newNumFormat;
  assertEquals('20 1234 5678',
               phoneUtil.formatByPattern(gbNumber,
                                         PNF.NATIONAL,
                                         newNumberFormats));

  newNumFormat.setNationalPrefixFormattingRule('');
  newNumberFormats[0] = newNumFormat;
  assertEquals('+44 20 1234 5678',
               phoneUtil.formatByPattern(gbNumber,
                                         PNF.INTERNATIONAL,
                                         newNumberFormats));
}

function testFormatE164Number() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6502530000);
  assertEquals('+16502530000', phoneUtil.format(usNumber, PNF.E164));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(301234);
  assertEquals('+49301234', phoneUtil.format(deNumber, PNF.E164));
}

function testFormatNumberWithExtension() {
  var PNF = i18n.phonenumbers.PhoneNumberFormat;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(33316005);
  nzNumber.setExtension('1234');
  // Uses default extension prefix:
  assertEquals('03-331 6005 ext. 1234',
               phoneUtil.format(nzNumber, PNF.NATIONAL));
  // Extension prefix overridden in the territory information for the US:
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6502530000);
  usNumber.setExtension('4567');
  assertEquals('650 253 0000 extn. 4567',
               phoneUtil.format(usNumber, PNF.NATIONAL));
}

function testFormatInOriginalFormat() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number1 = phoneUtil.parseAndKeepRawInput('+442087654321', 'GB');
  assertEquals('+44 20 8765 4321',
               phoneUtil.formatInOriginalFormat(number1, 'GB'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number2 = phoneUtil.parseAndKeepRawInput('02087654321', 'GB');
  assertEquals('(020) 8765 4321',
               phoneUtil.formatInOriginalFormat(number2, 'GB'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number3 = phoneUtil.parseAndKeepRawInput('011442087654321', 'US');
  assertEquals('011 44 20 8765 4321',
               phoneUtil.formatInOriginalFormat(number3, 'US'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number4 = phoneUtil.parseAndKeepRawInput('442087654321', 'GB');
  assertEquals('44 20 8765 4321',
               phoneUtil.formatInOriginalFormat(number4, 'GB'));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number5 = phoneUtil.parse('+442087654321', 'GB');
  assertEquals('(020) 8765 4321',
               phoneUtil.formatInOriginalFormat(number5, 'GB'));
}

function testIsPremiumRate() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var premiumRateNumber = new i18n.phonenumbers.PhoneNumber();

  premiumRateNumber.setCountryCode(1);
  premiumRateNumber.setNationalNumber(9004433030);
  assertEquals(PNT.PREMIUM_RATE, phoneUtil.getNumberType(premiumRateNumber));

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
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var mobileNumber = new i18n.phonenumbers.PhoneNumber();

  // A Bahama mobile number
  mobileNumber.setCountryCode(1);
  mobileNumber.setNationalNumber(2423570000);
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(mobileNumber));

  mobileNumber = new i18n.phonenumbers.PhoneNumber();
  mobileNumber.setCountryCode(39);
  mobileNumber.setNationalNumber(312345678);
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(mobileNumber));

  mobileNumber = new i18n.phonenumbers.PhoneNumber();
  mobileNumber.setCountryCode(44);
  mobileNumber.setNationalNumber(7912345678);
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(mobileNumber));

  mobileNumber = new i18n.phonenumbers.PhoneNumber();
  mobileNumber.setCountryCode(49);
  mobileNumber.setNationalNumber(15123456789);
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(mobileNumber));

  mobileNumber = new i18n.phonenumbers.PhoneNumber();
  mobileNumber.setCountryCode(54);
  mobileNumber.setNationalNumber(91187654321);
  assertEquals(PNT.MOBILE, phoneUtil.getNumberType(mobileNumber));
}

function testIsFixedLine() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var fixedLineNumber = new i18n.phonenumbers.PhoneNumber();

  // A Bahama fixed-line number
  fixedLineNumber.setCountryCode(1);
  fixedLineNumber.setNationalNumber(2423651234);
  assertEquals(PNT.FIXED_LINE, phoneUtil.getNumberType(fixedLineNumber));

  // An Italian fixed-line number
  fixedLineNumber = new i18n.phonenumbers.PhoneNumber();
  fixedLineNumber.setCountryCode(39);
  fixedLineNumber.setNationalNumber(236618300);
  fixedLineNumber.setItalianLeadingZero(true);
  assertEquals(PNT.FIXED_LINE, phoneUtil.getNumberType(fixedLineNumber));

  fixedLineNumber = new i18n.phonenumbers.PhoneNumber();
  fixedLineNumber.setCountryCode(44);
  fixedLineNumber.setNationalNumber(2012345678);
  assertEquals(PNT.FIXED_LINE, phoneUtil.getNumberType(fixedLineNumber));

  fixedLineNumber = new i18n.phonenumbers.PhoneNumber();
  fixedLineNumber.setCountryCode(49);
  fixedLineNumber.setNationalNumber(301234);
  assertEquals(PNT.FIXED_LINE, phoneUtil.getNumberType(fixedLineNumber));
}

function testIsFixedLineAndMobile() {
  var PNT = i18n.phonenumbers.PhoneNumberType;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var fixedLineAndMobileNumber = new i18n.phonenumbers.PhoneNumber();
  fixedLineAndMobileNumber.setCountryCode(1);
  fixedLineAndMobileNumber.setNationalNumber(6502531111);
  assertEquals(PNT.FIXED_LINE_OR_MOBILE,
               phoneUtil.getNumberType(fixedLineAndMobileNumber));

  fixedLineAndMobileNumber = new i18n.phonenumbers.PhoneNumber();
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
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var unknownNumber = new i18n.phonenumbers.PhoneNumber();
  unknownNumber.setCountryCode(1);
  unknownNumber.setNationalNumber(65025311111);
  assertEquals(PNT.UNKNOWN, phoneUtil.getNumberType(unknownNumber));
}

function testIsValidNumber() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6502530000);
  assertTrue(phoneUtil.isValidNumber(usNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(236618300);
  itNumber.setItalianLeadingZero(true);
  assertTrue(phoneUtil.isValidNumber(itNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(7912345678);
  assertTrue(phoneUtil.isValidNumber(gbNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(21387835);
  assertTrue(phoneUtil.isValidNumber(nzNumber));
}


function testIsValidForRegion() {
  // This number is valid for the Bahamas, but is not a valid US number.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var bsNumber = new i18n.phonenumbers.PhoneNumber();
  bsNumber.setCountryCode(1);
  bsNumber.setNationalNumber(2423232345);
  assertTrue(phoneUtil.isValidNumber(bsNumber));
  assertTrue(phoneUtil.isValidNumberForRegion(bsNumber, 'BS'));
  assertTrue(phoneUtil.isValidNumberForRegion(bsNumber, 'bs'));
  assertFalse(phoneUtil.isValidNumberForRegion(bsNumber, 'US'));
  bsNumber.setNationalNumber(2421232345);
  // This number is no longer valid.
  assertFalse(phoneUtil.isValidNumber(bsNumber));

  // La Mayotte and Reunion use 'leadingDigits' to differentiate them.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var reNumber = new i18n.phonenumbers.PhoneNumber();
  reNumber.setCountryCode(262);
  reNumber.setNationalNumber(262123456);
  assertTrue(phoneUtil.isValidNumber(reNumber));
  assertTrue(phoneUtil.isValidNumberForRegion(reNumber, 'RE'));
  assertFalse(phoneUtil.isValidNumberForRegion(reNumber, 'YT'));
  // Now change the number to be a number for La Mayotte.
  reNumber.setNationalNumber(269601234);
  assertTrue(phoneUtil.isValidNumberForRegion(reNumber, 'YT'));
  assertFalse(phoneUtil.isValidNumberForRegion(reNumber, 'RE'));
  // This number is no longer valid for La Reunion.
  reNumber.setNationalNumber(269123456);
  assertFalse(phoneUtil.isValidNumberForRegion(reNumber, 'YT'));
  assertFalse(phoneUtil.isValidNumberForRegion(reNumber, 'RE'));
  assertFalse(phoneUtil.isValidNumber(reNumber));
  // However, it should be recognised as from La Mayotte, since it is valid for
  // this region.
  assertEquals('YT', phoneUtil.getRegionCodeForNumber(reNumber));
  // This number is valid in both places.
  reNumber.setNationalNumber(800123456);
  assertTrue(phoneUtil.isValidNumberForRegion(reNumber, 'YT'));
  assertTrue(phoneUtil.isValidNumberForRegion(reNumber, 'RE'));
}

function testIsNotValidNumber() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(2530000);
  assertFalse(phoneUtil.isValidNumber(usNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(23661830000);
  itNumber.setItalianLeadingZero(true);
  assertFalse(phoneUtil.isValidNumber(itNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(791234567);
  assertFalse(phoneUtil.isValidNumber(gbNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(1234);
  assertFalse(phoneUtil.isValidNumber(deNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(3316005);
  assertFalse(phoneUtil.isValidNumber(nzNumber));
}

function testGetRegionCodeForCountryCode() {
  assertEquals('US', phoneUtil.getRegionCodeForCountryCode(1));
  assertEquals('GB', phoneUtil.getRegionCodeForCountryCode(44));
  assertEquals('DE', phoneUtil.getRegionCodeForCountryCode(49));
}

function testGetRegionCodeForNumber() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var bsNumber = new i18n.phonenumbers.PhoneNumber();
  bsNumber.setCountryCode(1);
  bsNumber.setNationalNumber(2423027000);
  assertEquals('BS', phoneUtil.getRegionCodeForNumber(bsNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6502530000);
  assertEquals('US', phoneUtil.getRegionCodeForNumber(usNumber));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var gbNumber = new i18n.phonenumbers.PhoneNumber();
  gbNumber.setCountryCode(44);
  gbNumber.setNationalNumber(7912345678);
  assertEquals('GB', phoneUtil.getRegionCodeForNumber(gbNumber));
}

function testGetCountryCodeForRegion() {
  assertEquals(1, phoneUtil.getCountryCodeForRegion('US'));
  assertEquals(64, phoneUtil.getCountryCodeForRegion('NZ'));
  assertEquals(64, phoneUtil.getCountryCodeForRegion('nz'));
  assertEquals(0, phoneUtil.getCountryCodeForRegion(null));
  assertEquals(0, phoneUtil.getCountryCodeForRegion('ZZ'));
  // CS is already deprecated so the library doesn't support it.
  assertEquals(0, phoneUtil.getCountryCodeForRegion('CS'));
}

function testIsNANPACountry() {
  assertTrue(phoneUtil.isNANPACountry('US'));
  assertTrue(phoneUtil.isNANPACountry('BS'));
  assertTrue(phoneUtil.isNANPACountry('bs'));
}

function testIsPossibleNumber() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(6502530000);
  assertTrue(phoneUtil.isPossibleNumber(number));

  number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(2530000);
  assertTrue(phoneUtil.isPossibleNumber(number));

  number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(44);
  number.setNationalNumber(2070313000);
  assertTrue(phoneUtil.isPossibleNumber(number));

  assertTrue(phoneUtil.isPossibleNumberString('+1 650 253 0000', 'US'));
  assertTrue(phoneUtil.isPossibleNumberString('+1 650 GOO OGLE', 'US'));
  assertTrue(phoneUtil.isPossibleNumberString('(650) 253-0000', 'US'));
  assertTrue(phoneUtil.isPossibleNumberString('253-0000', 'US'));
  assertTrue(phoneUtil.isPossibleNumberString('+1 650 253 0000', 'GB'));
  assertTrue(phoneUtil.isPossibleNumberString('+44 20 7031 3000', 'GB'));
  assertTrue(phoneUtil.isPossibleNumberString('(020) 7031 3000', 'GB'));
  assertTrue(phoneUtil.isPossibleNumberString('7031 3000', 'GB'));
  assertTrue(phoneUtil.isPossibleNumberString('3331 6005', 'NZ'));
  assertTrue(phoneUtil.isPossibleNumberString('3331 6005', 'nz'));
}

function testIsPossibleNumberWithReason() {
  var VR = i18n.phonenumbers.PhoneNumberUtil.ValidationResult;
  // FYI, national numbers for country code +1 that are within 7 to 10 digits
  // are possible.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(6502530000);
  assertEquals(VR.IS_POSSIBLE,
               phoneUtil.isPossibleNumberWithReason(number));

  number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(2530000);
  assertEquals(VR.IS_POSSIBLE,
               phoneUtil.isPossibleNumberWithReason(number));

  number = new i18n.phonenumbers.PhoneNumber();
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
  number.setCountryCode(1);
  number.setNationalNumber(65025300000);
  assertEquals(VR.TOO_LONG,
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
  adNumber.setNationalNumber(1234567890123456);
  assertEquals(VR.TOO_LONG,
               phoneUtil.isPossibleNumberWithReason(adNumber));
}

function testIsNotPossibleNumber() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(65025300000);
  assertFalse(phoneUtil.isPossibleNumber(number));

  number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(1);
  number.setNationalNumber(253000);
  assertFalse(phoneUtil.isPossibleNumber(number));

  number = new i18n.phonenumbers.PhoneNumber();
  number.setCountryCode(44);
  number.setNationalNumber(300);
  assertFalse(phoneUtil.isPossibleNumber(number));

  assertFalse(phoneUtil.isPossibleNumberString('+1 650 253 00000', 'US'));
  assertFalse(phoneUtil.isPossibleNumberString('(650) 253-00000', 'US'));
  assertFalse(phoneUtil.isPossibleNumberString('I want a Pizza', 'US'));
  assertFalse(phoneUtil.isPossibleNumberString('253-000', 'US'));
  assertFalse(phoneUtil.isPossibleNumberString('1 3000', 'GB'));
  assertFalse(phoneUtil.isPossibleNumberString('+44 300', 'GB'));
}

function testTruncateTooLongNumber() {
  // US number 650-253-0000, but entered with one additional digit at the end.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tooLongNumber = new i18n.phonenumbers.PhoneNumber();
  tooLongNumber.setCountryCode(1);
  tooLongNumber.setNationalNumber(65025300001);
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var validNumber = new i18n.phonenumbers.PhoneNumber();
  validNumber.setCountryCode(1);
  validNumber.setNationalNumber(6502530000);
  assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
  assertTrue(validNumber.exactlySameAs(tooLongNumber));

  // GB number 080 1234 5678, but entered with 4 extra digits at the end.
  tooLongNumber = new i18n.phonenumbers.PhoneNumber();
  tooLongNumber.setCountryCode(44);
  tooLongNumber.setNationalNumber(80123456780123);
  validNumber = new i18n.phonenumbers.PhoneNumber();
  validNumber.setCountryCode(44);
  validNumber.setNationalNumber(8012345678);
  assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
  assertTrue(validNumber.exactlySameAs(tooLongNumber));

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
  assertTrue(validNumber.exactlySameAs(tooLongNumber));

  // Tests what happens when a valid number is passed in.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var validNumberCopy = new i18n.phonenumbers.PhoneNumber();
  validNumberCopy.mergeFrom(validNumber);
  assertTrue(phoneUtil.truncateTooLongNumber(validNumber));
  // Tests the number is not modified.
  assertTrue(validNumber.exactlySameAs(validNumberCopy));

  // Tests what happens when a number with invalid prefix is passed in.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var numberWithInvalidPrefix = new i18n.phonenumbers.PhoneNumber();
  // The test metadata says US numbers cannot have prefix 240.
  numberWithInvalidPrefix.setCountryCode(1);
  numberWithInvalidPrefix.setNationalNumber(2401234567);
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var invalidNumberCopy = new i18n.phonenumbers.PhoneNumber();
  invalidNumberCopy.mergeFrom(numberWithInvalidPrefix);
  assertFalse(phoneUtil.truncateTooLongNumber(numberWithInvalidPrefix));
  // Tests the number is not modified.
  assertTrue(numberWithInvalidPrefix.exactlySameAs(invalidNumberCopy));

  // Tests what happens when a too short number is passed in.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tooShortNumber = new i18n.phonenumbers.PhoneNumber();
  tooShortNumber.setCountryCode(1);
  tooShortNumber.setNationalNumber(1234);
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tooShortNumberCopy = new i18n.phonenumbers.PhoneNumber();
  tooShortNumberCopy.mergeFrom(tooShortNumber);
  assertFalse(phoneUtil.truncateTooLongNumber(tooShortNumber));
  // Tests the number is not modified.
  assertTrue(tooShortNumber.exactlySameAs(tooShortNumberCopy));
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
  /** @type {string} */
  var nationalPrefix = '34';
  /** @type {!goog.string.StringBuffer} */
  var numberToStrip = new goog.string.StringBuffer('34356778');
  /** @type {string} */
  var strippedNumber = '356778';
  /** @type {RegExp} */
  var nationalRule = /\d{4,7}/;
  phoneUtil.maybeStripNationalPrefix(numberToStrip, nationalPrefix, '',
                                     nationalRule);
  assertEquals('Should have had national prefix stripped.',
               strippedNumber, numberToStrip.toString());
  // Retry stripping - now the number should not start with the national prefix,
  // so no more stripping should occur.
  phoneUtil.maybeStripNationalPrefix(numberToStrip, nationalPrefix, '',
                                     nationalRule);
  assertEquals('Should have had no change - no national prefix present.',
               strippedNumber, numberToStrip.toString());
  // Some countries have no national prefix. Repeat test with none specified.
  nationalPrefix = '';
  phoneUtil.maybeStripNationalPrefix(numberToStrip, nationalPrefix, '',
                                     nationalRule);
  assertEquals('Should not strip anything with empty national prefix.',
               strippedNumber, numberToStrip.toString());
  // If the resultant number doesn't match the national rule, it shouldn't be
  // stripped.
  nationalPrefix = '3';
  numberToStrip = new goog.string.StringBuffer('3123');
  strippedNumber = '3123';
  phoneUtil.maybeStripNationalPrefix(numberToStrip, nationalPrefix, '',
                                     nationalRule);
  assertEquals('Should have had no change - after stripping, it would not ' +
               'have matched the national rule.',
               strippedNumber, numberToStrip.toString());
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
  // code begins with 0.
  numberToStrip = new goog.string.StringBuffer('0090112-3123');
  strippedNumber = new goog.string.StringBuffer('00901123123');
  assertEquals(CCS.FROM_DEFAULT_COUNTRY,
      phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                          internationalPrefix));
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
  var metadata = phoneUtil.getMetadataForRegion('US');
  // Note that for the US, the IDD is 011.
  try {
    /** @type {string} */
    var phoneNumber = '011112-3456789';
    /** @type {string} */
    var strippedNumber = '123456789';
    /** @type {number} */
    var countryCode = 1;
    /** @type {!goog.string.StringBuffer} */
    var numberToFill = new goog.string.StringBuffer();
    assertEquals('Did not extract country code ' + countryCode + ' correctly.',
                 countryCode,
                 phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                   numberToFill, true, number));
    assertEquals('Did not figure out CountryCodeSource correctly',
                 CCS.FROM_NUMBER_WITH_IDD,
                 number.getCountryCodeSource());
    // Should strip and normalize national significant number.
    assertEquals('Did not strip off the country code correctly.',
                 strippedNumber,
                 numberToFill.toString());
  } catch (e) {
    fail('Should not have thrown an exception: ' + e.toString());
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '+6423456789';
    countryCode = 64;
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Did not extract country code ' + countryCode + ' correctly.',
                 countryCode,
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
    assertEquals('Should not have extracted a country code - ' +
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
    fail('Should have thrown an exception, no valid country code present.');
  } catch (e) {
    // Expected.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  number = new i18n.phonenumbers.PhoneNumber();
  try {
    phoneNumber = '(1 610) 619 4466';
    countryCode = 1;
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Should have extracted the country code of the region ' +
                 'passed in',
                 countryCode,
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
    countryCode = 1;
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Should have extracted the country code of the region ' +
                 'passed in',
                 countryCode,
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
    assertEquals('Should not have extracted a country code - ' +
                 'invalid number after extraction of uncertain country code.',
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
    phoneNumber = '(1 610) 619 43';
    numberToFill = new goog.string.StringBuffer();
    assertEquals('Should not have extracted a country code - invalid number ' +
                 'both before and after extraction of uncertain country code.',
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
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(33316005);

  // National prefix attached.
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('033316005', 'NZ')));
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('033316005', 'nz')));
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('33316005', 'NZ')));
  // National prefix attached and some formatting present.
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('03-331 6005', 'NZ')));
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('03 331 6005', 'NZ')));

  // Testing international prefixes.
  // Should strip country code.
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('0064 3 331 6005', 'NZ')));
  // Try again, but this time we have an international number with Region Code
  // US. It should recognise the country code and parse accordingly.
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('01164 3 331 6005', 'US')));
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('+64 3 331 6005', 'US')));

  nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(64123456);
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('64(0)64123456', 'NZ')));
  // Check that using a '/' is fine in a phone number.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var deNumber = new i18n.phonenumbers.PhoneNumber();
  deNumber.setCountryCode(49);
  deNumber.setNationalNumber(12345678);
  assertTrue(deNumber.exactlySameAs(phoneUtil.parse('123/45678', 'DE')));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  // Check it doesn't use the '1' as a country code when parsing if the phone
  // number was already possible.
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(1234567890);
  assertTrue(usNumber.exactlySameAs(phoneUtil.parse('123-456-7890', 'US')));
}

 function testParseNumberWithAlphaCharacters() {
  // Test case with alpha characters.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var tollfreeNumber = new i18n.phonenumbers.PhoneNumber();
  tollfreeNumber.setCountryCode(64);
  tollfreeNumber.setNationalNumber(800332005);
  assertTrue(tollfreeNumber.exactlySameAs(
      phoneUtil.parse('0800 DDA 005', 'NZ')));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var premiumNumber = new i18n.phonenumbers.PhoneNumber();
  premiumNumber.setCountryCode(64);
  premiumNumber.setNationalNumber(9003326005);
  assertTrue(premiumNumber.exactlySameAs(
      phoneUtil.parse('0900 DDA 6005', 'NZ')));
  // Not enough alpha characters for them to be considered intentional, so they
  // are stripped.
  assertTrue(premiumNumber.exactlySameAs(
      phoneUtil.parse('0900 332 6005a', 'NZ')));
  assertTrue(premiumNumber.exactlySameAs(
      phoneUtil.parse('0900 332 600a5', 'NZ')));
  assertTrue(premiumNumber.exactlySameAs(
      phoneUtil.parse('0900 332 600A5', 'NZ')));
  assertTrue(premiumNumber.exactlySameAs(
      phoneUtil.parse('0900 a332 600A5', 'NZ')));
}

function testParseWithInternationalPrefixes() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6503336000);
  assertTrue(usNumber.exactlySameAs(
      phoneUtil.parse('+1 (650) 333-6000', 'NZ')));
  assertTrue(usNumber.exactlySameAs(
      phoneUtil.parse('1-650-333-6000', 'US')));
  // Calling the US number from Singapore by using different service providers
  // 1st test: calling using SingTel IDD service (IDD is 001)
  assertTrue(usNumber.exactlySameAs(
      phoneUtil.parse('0011-650-333-6000', 'SG')));
  // 2nd test: calling using StarHub IDD service (IDD is 008)
  assertTrue(usNumber.exactlySameAs(
      phoneUtil.parse('0081-650-333-6000', 'SG')));
  // 3rd test: calling using SingTel V019 service (IDD is 019)
  assertTrue(usNumber.exactlySameAs(
      phoneUtil.parse('0191-650-333-6000', 'SG')));
  // Calling the US number from Poland
  assertTrue(usNumber.exactlySameAs(
      phoneUtil.parse('0~01-650-333-6000', 'PL')));
}

function testParseWithLeadingZero() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(236618300);
  itNumber.setItalianLeadingZero(true);
  assertTrue(itNumber.exactlySameAs(phoneUtil.parse('+39 02-36618 300', 'NZ')));
  assertTrue(itNumber.exactlySameAs(phoneUtil.parse('02-36618 300', 'IT')));

  itNumber = new i18n.phonenumbers.PhoneNumber();
  itNumber.setCountryCode(39);
  itNumber.setNationalNumber(312345678);
  assertTrue(itNumber.exactlySameAs(phoneUtil.parse('312 345 678', 'IT')));
}

function testParseNationalNumberArgentina() {
  // Test parsing mobile numbers of Argentina.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arNumber = new i18n.phonenumbers.PhoneNumber();

  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(93435551212);
  assertTrue(arNumber.exactlySameAs(
      phoneUtil.parse('+54 9 343 555 1212', 'AR')));
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('0343 15 555 1212', 'AR')));

  arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(93715654320);
  assertTrue(arNumber.exactlySameAs(
      phoneUtil.parse('+54 9 3715 65 4320', 'AR')));
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('03715 15 65 4320', 'AR')));

  // Test parsing fixed-line numbers of Argentina.
  arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(1137970000);
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('+54 11 3797 0000', 'AR')));
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('011 3797 0000', 'AR')));

  arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(3715654321);
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('+54 3715 65 4321', 'AR')));
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('03715 65 4321', 'AR')));

  arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(2312340000);
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('+54 23 1234 0000', 'AR')));
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('023 1234 0000', 'AR')));
}

function testParseWithXInNumber() {
  // Test that having an 'x' in the phone number at the start is ok and that it
  // just gets removed.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arNumber = new i18n.phonenumbers.PhoneNumber();
  arNumber.setCountryCode(54);
  arNumber.setNationalNumber(123456789);
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('0123456789', 'AR')));
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('(0) 123456789', 'AR')));
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('0 123456789', 'AR')));
  assertTrue(arNumber.exactlySameAs(phoneUtil.parse('(0xx) 123456789', 'AR')));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var arFromUs = new i18n.phonenumbers.PhoneNumber();
  arFromUs.setCountryCode(54);
  arFromUs.setNationalNumber(81429712);
  // This test is intentionally constructed such that the number of digit after
  // xx is larger than 7, so that the number won't be mistakenly treated as an
  // extension, as we allow extensions up to 7 digits. This assumption is okay
  // for now as all the countries where a carrier selection code is written in
  // the form of xx have a national significant number of length larger than 7.
  assertTrue(arFromUs.exactlySameAs(phoneUtil.parse('011xx5481429712', 'US')));
}

function testParseNumbersMexico() {
  // Test parsing fixed-line numbers of Mexico.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var mxNumber = new i18n.phonenumbers.PhoneNumber();
  mxNumber.setCountryCode(52);
  mxNumber.setNationalNumber(4499780001);
  assertTrue(mxNumber.exactlySameAs(
      phoneUtil.parse('+52 (449)978-0001', 'MX')));
  assertTrue(mxNumber.exactlySameAs(phoneUtil.parse('01 (449)978-0001', 'MX')));
  assertTrue(mxNumber.exactlySameAs(phoneUtil.parse('(449)978-0001', 'MX')));

  // Test parsing mobile numbers of Mexico.
  mxNumber = new i18n.phonenumbers.PhoneNumber();
  mxNumber.setCountryCode(52);
  mxNumber.setNationalNumber(13312345678);
  assertTrue(mxNumber.exactlySameAs(
      phoneUtil.parse('+52 1 33 1234-5678', 'MX')));
  assertTrue(mxNumber.exactlySameAs(
      phoneUtil.parse('044 (33) 1234-5678', 'MX')));
  assertTrue(mxNumber.exactlySameAs(
      phoneUtil.parse('045 33 1234-5678', 'MX')));
}

function testFailedParseOnInvalidNumbers() {
  try {
    /** @type {string} */
    var sentencePhoneNumber = 'This is not a phone number';
    phoneUtil.parse(sentencePhoneNumber, 'NZ');
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
    phoneUtil.parse(tooLongPhoneNumber, 'GB');
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
    phoneUtil.parse(plusMinusPhoneNumber, 'DE');
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
    phoneUtil.parse(tooShortPhoneNumber, 'DE');
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
    phoneUtil.parse(invalidCountryCode, 'NZ');
    fail('This is not a recognised country code: should fail: ' +
         invalidCountryCode);
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    /** @type {string} */
    var someNumber = '123 456 7890';
    phoneUtil.parse(someNumber, 'YY');
    fail('Unknown country code not allowed: should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    /** @type {string} */
    someNumber = '123 456 7890';
    phoneUtil.parse(someNumber, 'CS');
    fail('Deprecated country code not allowed: should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    someNumber = '123 456 7890';
    phoneUtil.parse(someNumber, null);
    fail('Null country code not allowed: should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
  try {
    someNumber = '0044------';
    phoneUtil.parse(someNumber, 'GB');
    fail('No number provided, only country code: should fail');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD,
                 e);
  }
  try {
    someNumber = '0044';
    phoneUtil.parse(someNumber, 'GB');
    fail('No number provided, only country code: should fail');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD,
                 e);
  }
  try {
    someNumber = '011';
    phoneUtil.parse(someNumber, 'US');
    fail('Only IDD provided - should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD,
                 e);
  }
  try {
    someNumber = '0119';
    phoneUtil.parse(someNumber, 'US');
    fail('Only IDD provided and then 9 - should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.TOO_SHORT_AFTER_IDD,
                 e);
  }
}

function testParseNumbersWithPlusWithNoRegion() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(33316005);
  // 'ZZ' is allowed only if the number starts with a '+' - then the country
  // code can be calculated.
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('+64 3 331 6005', 'ZZ')));
  assertTrue(nzNumber.exactlySameAs(phoneUtil.parse('+64 3 331 6005', null)));
  nzNumber.setRawInput('+64 3 331 6005');
  nzNumber.setCountryCodeSource(i18n.phonenumbers.PhoneNumber
      .CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN);
  assertTrue(nzNumber.exactlySameAs(
      phoneUtil.parseAndKeepRawInput('+64 3 331 6005', 'ZZ')));
  // Null is also allowed for the region code in these cases.
  assertTrue(nzNumber.exactlySameAs(
      phoneUtil.parseAndKeepRawInput('+64 3 331 6005', null)));
}

function testParseExtensions() {
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(33316005);
  nzNumber.setExtension('3456');
  assertTrue(nzNumber.exactlySameAs(
      phoneUtil.parse('03 331 6005 ext 3456', 'NZ')));
  assertTrue(nzNumber.exactlySameAs(
      phoneUtil.parse('03-3316005x3456', 'NZ')));
  assertTrue(nzNumber.exactlySameAs(
      phoneUtil.parse('03-3316005 int.3456', 'NZ')));
  assertTrue(nzNumber.exactlySameAs(
      phoneUtil.parse('03 3316005 #3456', 'NZ')));

  // Test the following do not extract extensions:
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nonExtnNumber = new i18n.phonenumbers.PhoneNumber();
  nonExtnNumber.setCountryCode(1);
  nonExtnNumber.setNationalNumber(80074935247);
  assertTrue(nonExtnNumber.exactlySameAs(
      phoneUtil.parse('1800 six-flags', 'US')));
  assertTrue(nonExtnNumber.exactlySameAs(
      phoneUtil.parse('1800 SIX FLAGS', 'US')));
  assertTrue(nonExtnNumber.exactlySameAs(
      phoneUtil.parse('0~0 1800 7493 5247', 'PL')));
  assertTrue(nonExtnNumber.exactlySameAs(
      phoneUtil.parse('(1800) 7493.5247', 'US')));

  // Check that the last instance of an extension token is matched.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var extnNumber = new i18n.phonenumbers.PhoneNumber();
  extnNumber.setCountryCode(1);
  extnNumber.setNationalNumber(80074935247);
  extnNumber.setExtension('1234');
  assertTrue(extnNumber.exactlySameAs(
      phoneUtil.parse('0~0 1800 7493 5247 ~1234', 'PL')));

  // Verifying bug-fix where the last digit of a number was previously omitted
  // if it was a 0 when extracting the extension. Also verifying a few different
  // cases of extensions.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var ukNumber = new i18n.phonenumbers.PhoneNumber();
  ukNumber.setCountryCode(44);
  ukNumber.setNationalNumber(2034567890);
  ukNumber.setExtension('456');
  assertTrue(ukNumber.exactlySameAs(
      phoneUtil.parse('+44 2034567890x456', 'NZ')));
  assertTrue(ukNumber.exactlySameAs(
      phoneUtil.parse('+44 2034567890x456', 'GB')));
  assertTrue(ukNumber.exactlySameAs(
      phoneUtil.parse('+44 2034567890 x456', 'GB')));
  assertTrue(ukNumber.exactlySameAs(
      phoneUtil.parse('+44 2034567890 X456', 'GB')));
  assertTrue(ukNumber.exactlySameAs(
      phoneUtil.parse('+44 2034567890 X 456', 'GB')));
  assertTrue(ukNumber.exactlySameAs(
      phoneUtil.parse('+44 2034567890 X  456', 'GB')));
  assertTrue(ukNumber.exactlySameAs(
      phoneUtil.parse('+44 2034567890 x 456  ', 'GB')));
  assertTrue(ukNumber.exactlySameAs(
      phoneUtil.parse('+44 2034567890  X 456', 'GB')));

  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usWithExtension = new i18n.phonenumbers.PhoneNumber();
  usWithExtension.setCountryCode(1);
  usWithExtension.setNationalNumber(8009013355);
  usWithExtension.setExtension('7246433');
  assertTrue(usWithExtension.exactlySameAs(
      phoneUtil.parse('(800) 901-3355 x 7246433', 'US')));
  assertTrue(usWithExtension.exactlySameAs(
      phoneUtil.parse('(800) 901-3355 , ext 7246433', 'US')));
  assertTrue(usWithExtension.exactlySameAs(
      phoneUtil.parse('(800) 901-3355 ,extension 7246433', 'US')));
  assertTrue(usWithExtension.exactlySameAs(
      phoneUtil.parse('(800) 901-3355 , 7246433', 'US')));
  assertTrue(usWithExtension.exactlySameAs(
      phoneUtil.parse('(800) 901-3355 ext: 7246433', 'US')));

  // Test that if a number has two extensions specified, we ignore the second.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usWithTwoExtensionsNumber = new i18n.phonenumbers.PhoneNumber();
  usWithTwoExtensionsNumber.setCountryCode(1);
  usWithTwoExtensionsNumber.setNationalNumber(2121231234);
  usWithTwoExtensionsNumber.setExtension('508');
  assertTrue(usWithTwoExtensionsNumber.exactlySameAs(
      phoneUtil.parse('(212)123-1234 x508/x1234', 'US')));
  assertTrue(usWithTwoExtensionsNumber.exactlySameAs(
      phoneUtil.parse('(212)123-1234 x508/ x1234', 'US')));
  assertTrue(usWithTwoExtensionsNumber.exactlySameAs(
      phoneUtil.parse('(212)123-1234 x508\\x1234', 'US')));

  // Test parsing numbers in the form (645) 123-1234-910# works, where the last
  // 3 digits before the # are an extension.
  usWithExtension = new i18n.phonenumbers.PhoneNumber();
  usWithExtension.setCountryCode(1);
  usWithExtension.setNationalNumber(6451231234);
  usWithExtension.setExtension('910');
  assertTrue(usWithExtension.exactlySameAs(
      phoneUtil.parse('+1 (645) 123 1234-910#', 'US')));
  // Retry with the same number in a slightly different format.
  assertTrue(usWithExtension.exactlySameAs(
      phoneUtil.parse('+1 (645) 123 1234 ext. 910#', 'US')));
}

function testParseAndKeepRaw() {
  var CCS = i18n.phonenumbers.PhoneNumber.CountryCodeSource;
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var alphaNumericNumber = new i18n.phonenumbers.PhoneNumber();
  alphaNumericNumber.setCountryCode(1);
  alphaNumericNumber.setNationalNumber(80074935247);
  alphaNumericNumber.setRawInput('800 six-flags');
  alphaNumericNumber.setCountryCodeSource(CCS.FROM_DEFAULT_COUNTRY);
  assertTrue(alphaNumericNumber.exactlySameAs(
      phoneUtil.parseAndKeepRawInput('800 six-flags', 'US')));

  alphaNumericNumber.setCountryCode(1);
  alphaNumericNumber.setNationalNumber(8007493524);
  alphaNumericNumber.setRawInput('1800 six-flag');
  alphaNumericNumber.setCountryCodeSource(CCS.FROM_NUMBER_WITHOUT_PLUS_SIGN);
  assertTrue(alphaNumericNumber.exactlySameAs(
      phoneUtil.parseAndKeepRawInput('1800 six-flag', 'US')));

  alphaNumericNumber.setCountryCode(1);
  alphaNumericNumber.setNationalNumber(8007493524);
  alphaNumericNumber.setRawInput('+1800 six-flag');
  alphaNumericNumber.setCountryCodeSource(CCS.FROM_NUMBER_WITH_PLUS_SIGN);
  assertTrue(alphaNumericNumber.exactlySameAs(
      phoneUtil.parseAndKeepRawInput('+1800 six-flag', 'NZ')));

  alphaNumericNumber.setCountryCode(1);
  alphaNumericNumber.setNationalNumber(8007493524);
  alphaNumericNumber.setRawInput('001800 six-flag');
  alphaNumericNumber.setCountryCodeSource(CCS.FROM_NUMBER_WITH_IDD);
  assertTrue(alphaNumericNumber.exactlySameAs(
      phoneUtil.parseAndKeepRawInput('001800 six-flag', 'NZ')));

  // Invalid region code supplied.
  try {
    phoneUtil.parseAndKeepRawInput('123 456 7890', 'CS');
    fail('Deprecated country code not allowed: should fail.');
  } catch (e) {
    // Expected this exception.
    assertEquals('Wrong error type stored in exception.',
                 i18n.phonenumbers.Error.INVALID_COUNTRY_CODE,
                 e);
  }
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
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var usNumber = new i18n.phonenumbers.PhoneNumber();
  usNumber.setCountryCode(1);
  usNumber.setNationalNumber(6502530000);
  assertEquals('00 1 650 253 0000',
               phoneUtil.formatOutOfCountryCallingNumber(usNumber, 'AD'));
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
  // or country code has been specified.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var num1 = phoneUtil.parse('+64 3 331 6005', 'NZ');
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var num2 = phoneUtil.parse('+64 03 331 6005', 'NZ');
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
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(33316005);
  nzNumber.setExtension('3456');
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(nzNumber, '+643 331 6005 ext 3456'));
  nzNumber.clearExtension();
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(nzNumber, '+6403 331 6005'));
  // Check empty extensions are ignored.
  nzNumber.setExtension('');
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(nzNumber, '+6403 331 6005'));
  // Check variant with two proto buffers.
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumberTwo = new i18n.phonenumbers.PhoneNumber();
  nzNumberTwo.setCountryCode(64);
  nzNumberTwo.setNationalNumber(33316005);
  assertEquals('Number nzNumber did not match nzNumberTwo',
               i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH,
               phoneUtil.isNumberMatch(nzNumber, nzNumberTwo));
}

function testIsNumberMatchNonMatches() {
  // Non-matches.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH,
               phoneUtil.isNumberMatch('03 331 6005', '03 331 6006'));
  // Different country code, partial number match.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NO_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '+16433316005'));
  // Different country code, same number.
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
}

function testIsNumberMatchNsnMatches() {
  // NSN matches.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '03 331 6005'));
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch('3 331-6005', '03 331 6005'));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var nzNumber = new i18n.phonenumbers.PhoneNumber();
  nzNumber.setCountryCode(64);
  nzNumber.setNationalNumber(33316005);
  nzNumber.setExtension('');
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH,
               phoneUtil.isNumberMatch(nzNumber, '03 331 6005'));
  /** @type {i18n.phonenumbers.PhoneNumber} */
  var unchangedNzNumber = new i18n.phonenumbers.PhoneNumber();
  unchangedNzNumber.setCountryCode(64);
  unchangedNzNumber.setNationalNumber(33316005);
  unchangedNzNumber.setExtension('');
  // Check the phone number proto was not edited during the method call.
  assertTrue(unchangedNzNumber.exactlySameAs(nzNumber));
}

function testIsNumberMatchShortNsnMatches() {
  // Short NSN matches with the country not specified for either one or both
  // numbers.
  assertEquals(i18n.phonenumbers.PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
               phoneUtil.isNumberMatch('+64 3 331-6005', '331 6005'));
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
  // NSN matches, country code omitted for one number, extension missing for
  // one.
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
