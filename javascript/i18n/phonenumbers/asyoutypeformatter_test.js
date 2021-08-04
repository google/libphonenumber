/**
 * @license
 * Copyright (C) 2010 The Libphonenumber Authors.
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
 * @fileoverview  Unit tests for the AsYouTypeFormatter.
 *
 * Note that these tests use the metadata contained in metadatafortesting.js,
 * not the normal metadata files, so should not be used for regression test
 * purposes - these tests are illustrative only and test functionality.
 *
 * @author Nikolaos Trogkanis
 */
goog.provide('i18n.phonenumbers.AsYouTypeFormatterTest');
goog.setTestOnly();

goog.require('goog.testing.jsunit');
goog.require('i18n.phonenumbers.AsYouTypeFormatter');
goog.require('i18n.phonenumbers.RegionCode');

var RegionCode = i18n.phonenumbers.RegionCode;

function testInvalidRegion() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.ZZ);
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+4', f.inputDigit('4'));
  assertEquals('+48 ', f.inputDigit('8'));
  assertEquals('+48 8', f.inputDigit('8'));
  assertEquals('+48 88', f.inputDigit('8'));
  assertEquals('+48 88 1', f.inputDigit('1'));
  assertEquals('+48 88 12', f.inputDigit('2'));
  assertEquals('+48 88 123', f.inputDigit('3'));
  assertEquals('+48 88 123 1', f.inputDigit('1'));
  assertEquals('+48 88 123 12', f.inputDigit('2'));

  f.clear();
  assertEquals('6', f.inputDigit('6'));
  assertEquals('65', f.inputDigit('5'));
  assertEquals('650', f.inputDigit('0'));
  assertEquals('6502', f.inputDigit('2'));
  assertEquals('65025', f.inputDigit('5'));
  assertEquals('650253', f.inputDigit('3'));
}

function testInvalidPlusSign() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.ZZ);
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+4', f.inputDigit('4'));
  assertEquals('+48 ', f.inputDigit('8'));
  assertEquals('+48 8', f.inputDigit('8'));
  assertEquals('+48 88', f.inputDigit('8'));
  assertEquals('+48 88 1', f.inputDigit('1'));
  assertEquals('+48 88 12', f.inputDigit('2'));
  assertEquals('+48 88 123', f.inputDigit('3'));
  assertEquals('+48 88 123 1', f.inputDigit('1'));
  // A plus sign can only appear at the beginning of the number;
  // otherwise, no formatting is applied.
  assertEquals('+48881231+', f.inputDigit('+'));
  assertEquals('+48881231+2', f.inputDigit('2'));
}

function testTooLongNumberMatchingMultipleLeadingDigits() {
  // See https://github.com/google/libphonenumber/issues/36
  // The bug occurred last time for countries which have two formatting rules
  // with exactly the same leading digits pattern but differ in length.
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.ZZ);
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+81 ', f.inputDigit('1'));
  assertEquals('+81 9', f.inputDigit('9'));
  assertEquals('+81 90', f.inputDigit('0'));
  assertEquals('+81 90 1', f.inputDigit('1'));
  assertEquals('+81 90 12', f.inputDigit('2'));
  assertEquals('+81 90 123', f.inputDigit('3'));
  assertEquals('+81 90 1234', f.inputDigit('4'));
  assertEquals('+81 90 1234 5', f.inputDigit('5'));
  assertEquals('+81 90 1234 56', f.inputDigit('6'));
  assertEquals('+81 90 1234 567', f.inputDigit('7'));
  assertEquals('+81 90 1234 5678', f.inputDigit('8'));
  assertEquals('+81 90 12 345 6789', f.inputDigit('9'));
  assertEquals('+81901234567890', f.inputDigit('0'));
  assertEquals('+819012345678901', f.inputDigit('1'));
}

function testCountryWithSpaceInNationalPrefixFormattingRule() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.BY);
  assertEquals('8', f.inputDigit('8'));
  assertEquals('88', f.inputDigit('8'));
  assertEquals('881', f.inputDigit('1'));
  assertEquals('8 819', f.inputDigit('9'));
  assertEquals('8 8190', f.inputDigit('0'));
  // The formatting rule for 5 digit numbers states that no space should be
  // present after the national prefix.
  assertEquals('881 901', f.inputDigit('1'));
  assertEquals('8 819 012', f.inputDigit('2'));
  // Too long, no formatting rule applies.
  assertEquals('88190123', f.inputDigit('3'));
}

function testCountryWithSpaceInNationalPrefixFormattingRuleAndLongNdd() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.BY);
  assertEquals('9', f.inputDigit('9'));
  assertEquals('99', f.inputDigit('9'));
  assertEquals('999', f.inputDigit('9'));
  assertEquals('9999', f.inputDigit('9'));
  assertEquals('99999 ', f.inputDigit('9'));
  assertEquals('99999 1', f.inputDigit('1'));
  assertEquals('99999 12', f.inputDigit('2'));
  assertEquals('99999 123', f.inputDigit('3'));
  assertEquals('99999 1234', f.inputDigit('4'));
  assertEquals('99999 12 345', f.inputDigit('5'));
}

function testAYTFUS() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.US);
  assertEquals('6', f.inputDigit('6'));
  assertEquals('65', f.inputDigit('5'));
  assertEquals('650', f.inputDigit('0'));
  assertEquals('650 2', f.inputDigit('2'));
  assertEquals('650 25', f.inputDigit('5'));
  assertEquals('650 253', f.inputDigit('3'));
  // Note this is how a US local number (without area code) should be formatted.
  assertEquals('650 2532', f.inputDigit('2'));
  assertEquals('650 253 22', f.inputDigit('2'));
  assertEquals('650 253 222', f.inputDigit('2'));
  assertEquals('650 253 2222', f.inputDigit('2'));

  f.clear();
  assertEquals('1', f.inputDigit('1'));
  assertEquals('16', f.inputDigit('6'));
  assertEquals('1 65', f.inputDigit('5'));
  assertEquals('1 650', f.inputDigit('0'));
  assertEquals('1 650 2', f.inputDigit('2'));
  assertEquals('1 650 25', f.inputDigit('5'));
  assertEquals('1 650 253', f.inputDigit('3'));
  assertEquals('1 650 253 2', f.inputDigit('2'));
  assertEquals('1 650 253 22', f.inputDigit('2'));
  assertEquals('1 650 253 222', f.inputDigit('2'));
  assertEquals('1 650 253 2222', f.inputDigit('2'));

  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011 ', f.inputDigit('1'));
  assertEquals('011 4', f.inputDigit('4'));
  assertEquals('011 44 ', f.inputDigit('4'));
  assertEquals('011 44 6', f.inputDigit('6'));
  assertEquals('011 44 61', f.inputDigit('1'));
  assertEquals('011 44 6 12', f.inputDigit('2'));
  assertEquals('011 44 6 123', f.inputDigit('3'));
  assertEquals('011 44 6 123 1', f.inputDigit('1'));
  assertEquals('011 44 6 123 12', f.inputDigit('2'));
  assertEquals('011 44 6 123 123', f.inputDigit('3'));
  assertEquals('011 44 6 123 123 1', f.inputDigit('1'));
  assertEquals('011 44 6 123 123 12', f.inputDigit('2'));
  assertEquals('011 44 6 123 123 123', f.inputDigit('3'));

  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011 ', f.inputDigit('1'));
  assertEquals('011 5', f.inputDigit('5'));
  assertEquals('011 54 ', f.inputDigit('4'));
  assertEquals('011 54 9', f.inputDigit('9'));
  assertEquals('011 54 91', f.inputDigit('1'));
  assertEquals('011 54 9 11', f.inputDigit('1'));
  assertEquals('011 54 9 11 2', f.inputDigit('2'));
  assertEquals('011 54 9 11 23', f.inputDigit('3'));
  assertEquals('011 54 9 11 231', f.inputDigit('1'));
  assertEquals('011 54 9 11 2312', f.inputDigit('2'));
  assertEquals('011 54 9 11 2312 1', f.inputDigit('1'));
  assertEquals('011 54 9 11 2312 12', f.inputDigit('2'));
  assertEquals('011 54 9 11 2312 123', f.inputDigit('3'));
  assertEquals('011 54 9 11 2312 1234', f.inputDigit('4'));

  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011 ', f.inputDigit('1'));
  assertEquals('011 2', f.inputDigit('2'));
  assertEquals('011 24', f.inputDigit('4'));
  assertEquals('011 244 ', f.inputDigit('4'));
  assertEquals('011 244 2', f.inputDigit('2'));
  assertEquals('011 244 28', f.inputDigit('8'));
  assertEquals('011 244 280', f.inputDigit('0'));
  assertEquals('011 244 280 0', f.inputDigit('0'));
  assertEquals('011 244 280 00', f.inputDigit('0'));
  assertEquals('011 244 280 000', f.inputDigit('0'));
  assertEquals('011 244 280 000 0', f.inputDigit('0'));
  assertEquals('011 244 280 000 00', f.inputDigit('0'));
  assertEquals('011 244 280 000 000', f.inputDigit('0'));

  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+4', f.inputDigit('4'));
  assertEquals('+48 ', f.inputDigit('8'));
  assertEquals('+48 8', f.inputDigit('8'));
  assertEquals('+48 88', f.inputDigit('8'));
  assertEquals('+48 88 1', f.inputDigit('1'));
  assertEquals('+48 88 12', f.inputDigit('2'));
  assertEquals('+48 88 123', f.inputDigit('3'));
  assertEquals('+48 88 123 1', f.inputDigit('1'));
  assertEquals('+48 88 123 12', f.inputDigit('2'));
  assertEquals('+48 88 123 12 1', f.inputDigit('1'));
  assertEquals('+48 88 123 12 12', f.inputDigit('2'));
}

function testAYTFUSFullWidthCharacters() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.US);
  assertEquals('\uFF16', f.inputDigit('\uFF16'));
  assertEquals('\uFF16\uFF15', f.inputDigit('\uFF15'));
  assertEquals('650', f.inputDigit('\uFF10'));
  assertEquals('650 2', f.inputDigit('\uFF12'));
  assertEquals('650 25', f.inputDigit('\uFF15'));
  assertEquals('650 253', f.inputDigit('\uFF13'));
  assertEquals('650 2532', f.inputDigit('\uFF12'));
  assertEquals('650 253 22', f.inputDigit('\uFF12'));
  assertEquals('650 253 222', f.inputDigit('\uFF12'));
  assertEquals('650 253 2222', f.inputDigit('\uFF12'));
}

function testAYTFUSMobileShortCode() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.US);
  assertEquals('*', f.inputDigit('*'));
  assertEquals('*1', f.inputDigit('1'));
  assertEquals('*12', f.inputDigit('2'));
  assertEquals('*121', f.inputDigit('1'));
  assertEquals('*121#', f.inputDigit('#'));
}

function testAYTFUSVanityNumber() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.US);
  assertEquals('8', f.inputDigit('8'));
  assertEquals('80', f.inputDigit('0'));
  assertEquals('800', f.inputDigit('0'));
  assertEquals('800 ', f.inputDigit(' '));
  assertEquals('800 M', f.inputDigit('M'));
  assertEquals('800 MY', f.inputDigit('Y'));
  assertEquals('800 MY ', f.inputDigit(' '));
  assertEquals('800 MY A', f.inputDigit('A'));
  assertEquals('800 MY AP', f.inputDigit('P'));
  assertEquals('800 MY APP', f.inputDigit('P'));
  assertEquals('800 MY APPL', f.inputDigit('L'));
  assertEquals('800 MY APPLE', f.inputDigit('E'));
}

function testAYTFAndRememberPositionUS() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.US);
  assertEquals('1', f.inputDigitAndRememberPosition('1'));
  assertEquals(1, f.getRememberedPosition());
  assertEquals('16', f.inputDigit('6'));
  assertEquals('1 65', f.inputDigit('5'));
  assertEquals(1, f.getRememberedPosition());
  assertEquals('1 650', f.inputDigitAndRememberPosition('0'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('1 650 2', f.inputDigit('2'));
  assertEquals('1 650 25', f.inputDigit('5'));
  // Note the remembered position for digit '0' changes from 4 to 5, because a
  // space is now inserted in the front.
  assertEquals(5, f.getRememberedPosition());
  assertEquals('1 650 253', f.inputDigit('3'));
  assertEquals('1 650 253 2', f.inputDigit('2'));
  assertEquals('1 650 253 22', f.inputDigit('2'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('1 650 253 222', f.inputDigitAndRememberPosition('2'));
  assertEquals(13, f.getRememberedPosition());
  assertEquals('1 650 253 2222', f.inputDigit('2'));
  assertEquals(13, f.getRememberedPosition());
  assertEquals('165025322222', f.inputDigit('2'));
  assertEquals(10, f.getRememberedPosition());
  assertEquals('1650253222222', f.inputDigit('2'));
  assertEquals(10, f.getRememberedPosition());

  f.clear();
  assertEquals('1', f.inputDigit('1'));
  assertEquals('16', f.inputDigitAndRememberPosition('6'));
  assertEquals(2, f.getRememberedPosition());
  assertEquals('1 65', f.inputDigit('5'));
  assertEquals('1 650', f.inputDigit('0'));
  assertEquals(3, f.getRememberedPosition());
  assertEquals('1 650 2', f.inputDigit('2'));
  assertEquals('1 650 25', f.inputDigit('5'));
  assertEquals(3, f.getRememberedPosition());
  assertEquals('1 650 253', f.inputDigit('3'));
  assertEquals('1 650 253 2', f.inputDigit('2'));
  assertEquals('1 650 253 22', f.inputDigit('2'));
  assertEquals(3, f.getRememberedPosition());
  assertEquals('1 650 253 222', f.inputDigit('2'));
  assertEquals('1 650 253 2222', f.inputDigit('2'));
  assertEquals('165025322222', f.inputDigit('2'));
  assertEquals(2, f.getRememberedPosition());
  assertEquals('1650253222222', f.inputDigit('2'));
  assertEquals(2, f.getRememberedPosition());

  f.clear();
  assertEquals('6', f.inputDigit('6'));
  assertEquals('65', f.inputDigit('5'));
  assertEquals('650', f.inputDigit('0'));
  assertEquals('650 2', f.inputDigit('2'));
  assertEquals('650 25', f.inputDigit('5'));
  assertEquals('650 253', f.inputDigit('3'));
  assertEquals('650 2532', f.inputDigitAndRememberPosition('2'));
  assertEquals(8, f.getRememberedPosition());
  assertEquals('650 253 22', f.inputDigit('2'));
  assertEquals(9, f.getRememberedPosition());
  assertEquals('650 253 222', f.inputDigit('2'));
  // No more formatting when semicolon is entered.
  assertEquals('650253222;', f.inputDigit(';'));
  assertEquals(7, f.getRememberedPosition());
  assertEquals('650253222;2', f.inputDigit('2'));

  f.clear();
  assertEquals('6', f.inputDigit('6'));
  assertEquals('65', f.inputDigit('5'));
  assertEquals('650', f.inputDigit('0'));
  // No more formatting when users choose to do their own formatting.
  assertEquals('650-', f.inputDigit('-'));
  assertEquals('650-2', f.inputDigitAndRememberPosition('2'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('650-25', f.inputDigit('5'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('650-253', f.inputDigit('3'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('650-253-', f.inputDigit('-'));
  assertEquals('650-253-2', f.inputDigit('2'));
  assertEquals('650-253-22', f.inputDigit('2'));
  assertEquals('650-253-222', f.inputDigit('2'));
  assertEquals('650-253-2222', f.inputDigit('2'));

  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011 ', f.inputDigit('1'));
  assertEquals('011 4', f.inputDigitAndRememberPosition('4'));
  assertEquals('011 48 ', f.inputDigit('8'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('011 48 8', f.inputDigit('8'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('011 48 88', f.inputDigit('8'));
  assertEquals('011 48 88 1', f.inputDigit('1'));
  assertEquals('011 48 88 12', f.inputDigit('2'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('011 48 88 123', f.inputDigit('3'));
  assertEquals('011 48 88 123 1', f.inputDigit('1'));
  assertEquals('011 48 88 123 12', f.inputDigit('2'));
  assertEquals('011 48 88 123 12 1', f.inputDigit('1'));
  assertEquals('011 48 88 123 12 12', f.inputDigit('2'));

  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+1', f.inputDigit('1'));
  assertEquals('+1 6', f.inputDigitAndRememberPosition('6'));
  assertEquals('+1 65', f.inputDigit('5'));
  assertEquals('+1 650', f.inputDigit('0'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('+1 650 2', f.inputDigit('2'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('+1 650 25', f.inputDigit('5'));
  assertEquals('+1 650 253', f.inputDigitAndRememberPosition('3'));
  assertEquals('+1 650 253 2', f.inputDigit('2'));
  assertEquals('+1 650 253 22', f.inputDigit('2'));
  assertEquals('+1 650 253 222', f.inputDigit('2'));
  assertEquals(10, f.getRememberedPosition());

  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+1', f.inputDigit('1'));
  assertEquals('+1 6', f.inputDigitAndRememberPosition('6'));
  assertEquals('+1 65', f.inputDigit('5'));
  assertEquals('+1 650', f.inputDigit('0'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('+1 650 2', f.inputDigit('2'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('+1 650 25', f.inputDigit('5'));
  assertEquals('+1 650 253', f.inputDigit('3'));
  assertEquals('+1 650 253 2', f.inputDigit('2'));
  assertEquals('+1 650 253 22', f.inputDigit('2'));
  assertEquals('+1 650 253 222', f.inputDigit('2'));
  assertEquals('+1650253222;', f.inputDigit(';'));
  assertEquals(3, f.getRememberedPosition());
}

function testAYTFGBFixedLine() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.GB);
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('020', f.inputDigit('0'));
  assertEquals('020 7', f.inputDigitAndRememberPosition('7'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('020 70', f.inputDigit('0'));
  assertEquals('020 703', f.inputDigit('3'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('020 7031', f.inputDigit('1'));
  assertEquals('020 7031 3', f.inputDigit('3'));
  assertEquals('020 7031 30', f.inputDigit('0'));
  assertEquals('020 7031 300', f.inputDigit('0'));
  assertEquals('020 7031 3000', f.inputDigit('0'));
}

function testAYTFGBTollFree() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.GB);
  assertEquals('0', f.inputDigit('0'));
  assertEquals('08', f.inputDigit('8'));
  assertEquals('080', f.inputDigit('0'));
  assertEquals('080 7', f.inputDigit('7'));
  assertEquals('080 70', f.inputDigit('0'));
  assertEquals('080 703', f.inputDigit('3'));
  assertEquals('080 7031', f.inputDigit('1'));
  assertEquals('080 7031 3', f.inputDigit('3'));
  assertEquals('080 7031 30', f.inputDigit('0'));
  assertEquals('080 7031 300', f.inputDigit('0'));
  assertEquals('080 7031 3000', f.inputDigit('0'));
}

function testAYTFGBPremiumRate() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.GB);
  assertEquals('0', f.inputDigit('0'));
  assertEquals('09', f.inputDigit('9'));
  assertEquals('090', f.inputDigit('0'));
  assertEquals('090 7', f.inputDigit('7'));
  assertEquals('090 70', f.inputDigit('0'));
  assertEquals('090 703', f.inputDigit('3'));
  assertEquals('090 7031', f.inputDigit('1'));
  assertEquals('090 7031 3', f.inputDigit('3'));
  assertEquals('090 7031 30', f.inputDigit('0'));
  assertEquals('090 7031 300', f.inputDigit('0'));
  assertEquals('090 7031 3000', f.inputDigit('0'));
}

function testAYTFNZMobile() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.NZ);
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('021', f.inputDigit('1'));
  assertEquals('02-11', f.inputDigit('1'));
  assertEquals('02-112', f.inputDigit('2'));
  // Note the unittest is using fake metadata which might produce non-ideal
  // results.
  assertEquals('02-112 3', f.inputDigit('3'));
  assertEquals('02-112 34', f.inputDigit('4'));
  assertEquals('02-112 345', f.inputDigit('5'));
  assertEquals('02-112 3456', f.inputDigit('6'));
}

function testAYTFDE() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.DE);
  assertEquals('0', f.inputDigit('0'));
  assertEquals('03', f.inputDigit('3'));
  assertEquals('030', f.inputDigit('0'));
  assertEquals('030/1', f.inputDigit('1'));
  assertEquals('030/12', f.inputDigit('2'));
  assertEquals('030/123', f.inputDigit('3'));
  assertEquals('030/1234', f.inputDigit('4'));

  // 04134 1234
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('04', f.inputDigit('4'));
  assertEquals('041', f.inputDigit('1'));
  assertEquals('041 3', f.inputDigit('3'));
  assertEquals('041 34', f.inputDigit('4'));
  assertEquals('04134 1', f.inputDigit('1'));
  assertEquals('04134 12', f.inputDigit('2'));
  assertEquals('04134 123', f.inputDigit('3'));
  assertEquals('04134 1234', f.inputDigit('4'));

  // 08021 2345
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('08', f.inputDigit('8'));
  assertEquals('080', f.inputDigit('0'));
  assertEquals('080 2', f.inputDigit('2'));
  assertEquals('080 21', f.inputDigit('1'));
  assertEquals('08021 2', f.inputDigit('2'));
  assertEquals('08021 23', f.inputDigit('3'));
  assertEquals('08021 234', f.inputDigit('4'));
  assertEquals('08021 2345', f.inputDigit('5'));

  // 00 1 650 253 2250
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('00', f.inputDigit('0'));
  assertEquals('00 1 ', f.inputDigit('1'));
  assertEquals('00 1 6', f.inputDigit('6'));
  assertEquals('00 1 65', f.inputDigit('5'));
  assertEquals('00 1 650', f.inputDigit('0'));
  assertEquals('00 1 650 2', f.inputDigit('2'));
  assertEquals('00 1 650 25', f.inputDigit('5'));
  assertEquals('00 1 650 253', f.inputDigit('3'));
  assertEquals('00 1 650 253 2', f.inputDigit('2'));
  assertEquals('00 1 650 253 22', f.inputDigit('2'));
  assertEquals('00 1 650 253 222', f.inputDigit('2'));
  assertEquals('00 1 650 253 2222', f.inputDigit('2'));
}

function testAYTFAR() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.AR);
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011', f.inputDigit('1'));
  assertEquals('011 7', f.inputDigit('7'));
  assertEquals('011 70', f.inputDigit('0'));
  assertEquals('011 703', f.inputDigit('3'));
  assertEquals('011 7031', f.inputDigit('1'));
  assertEquals('011 7031-3', f.inputDigit('3'));
  assertEquals('011 7031-30', f.inputDigit('0'));
  assertEquals('011 7031-300', f.inputDigit('0'));
  assertEquals('011 7031-3000', f.inputDigit('0'));
}

function testAYTFARMobile() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.AR);
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+5', f.inputDigit('5'));
  assertEquals('+54 ', f.inputDigit('4'));
  assertEquals('+54 9', f.inputDigit('9'));
  assertEquals('+54 91', f.inputDigit('1'));
  assertEquals('+54 9 11', f.inputDigit('1'));
  assertEquals('+54 9 11 2', f.inputDigit('2'));
  assertEquals('+54 9 11 23', f.inputDigit('3'));
  assertEquals('+54 9 11 231', f.inputDigit('1'));
  assertEquals('+54 9 11 2312', f.inputDigit('2'));
  assertEquals('+54 9 11 2312 1', f.inputDigit('1'));
  assertEquals('+54 9 11 2312 12', f.inputDigit('2'));
  assertEquals('+54 9 11 2312 123', f.inputDigit('3'));
  assertEquals('+54 9 11 2312 1234', f.inputDigit('4'));
}

function testAYTFKR() {
  // +82 51 234 5678
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.KR);
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+82 ', f.inputDigit('2'));
  assertEquals('+82 5', f.inputDigit('5'));
  assertEquals('+82 51', f.inputDigit('1'));
  assertEquals('+82 51-2', f.inputDigit('2'));
  assertEquals('+82 51-23', f.inputDigit('3'));
  assertEquals('+82 51-234', f.inputDigit('4'));
  assertEquals('+82 51-234-5', f.inputDigit('5'));
  assertEquals('+82 51-234-56', f.inputDigit('6'));
  assertEquals('+82 51-234-567', f.inputDigit('7'));
  assertEquals('+82 51-234-5678', f.inputDigit('8'));

  // +82 2 531 5678
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+82 ', f.inputDigit('2'));
  assertEquals('+82 2', f.inputDigit('2'));
  assertEquals('+82 25', f.inputDigit('5'));
  assertEquals('+82 2-53', f.inputDigit('3'));
  assertEquals('+82 2-531', f.inputDigit('1'));
  assertEquals('+82 2-531-5', f.inputDigit('5'));
  assertEquals('+82 2-531-56', f.inputDigit('6'));
  assertEquals('+82 2-531-567', f.inputDigit('7'));
  assertEquals('+82 2-531-5678', f.inputDigit('8'));

  // +82 2 3665 5678
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+82 ', f.inputDigit('2'));
  assertEquals('+82 2', f.inputDigit('2'));
  assertEquals('+82 23', f.inputDigit('3'));
  assertEquals('+82 2-36', f.inputDigit('6'));
  assertEquals('+82 2-366', f.inputDigit('6'));
  assertEquals('+82 2-3665', f.inputDigit('5'));
  assertEquals('+82 2-3665-5', f.inputDigit('5'));
  assertEquals('+82 2-3665-56', f.inputDigit('6'));
  assertEquals('+82 2-3665-567', f.inputDigit('7'));
  assertEquals('+82 2-3665-5678', f.inputDigit('8'));

  // 02-114
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('021', f.inputDigit('1'));
  assertEquals('02-11', f.inputDigit('1'));
  assertEquals('02-114', f.inputDigit('4'));

  // 02-1300
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('021', f.inputDigit('1'));
  assertEquals('02-13', f.inputDigit('3'));
  assertEquals('02-130', f.inputDigit('0'));
  assertEquals('02-1300', f.inputDigit('0'));

  // 011-456-7890
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011', f.inputDigit('1'));
  assertEquals('011-4', f.inputDigit('4'));
  assertEquals('011-45', f.inputDigit('5'));
  assertEquals('011-456', f.inputDigit('6'));
  assertEquals('011-456-7', f.inputDigit('7'));
  assertEquals('011-456-78', f.inputDigit('8'));
  assertEquals('011-456-789', f.inputDigit('9'));
  assertEquals('011-456-7890', f.inputDigit('0'));

  // 011-9876-7890
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011', f.inputDigit('1'));
  assertEquals('011-9', f.inputDigit('9'));
  assertEquals('011-98', f.inputDigit('8'));
  assertEquals('011-987', f.inputDigit('7'));
  assertEquals('011-9876', f.inputDigit('6'));
  assertEquals('011-9876-7', f.inputDigit('7'));
  assertEquals('011-9876-78', f.inputDigit('8'));
  assertEquals('011-9876-789', f.inputDigit('9'));
  assertEquals('011-9876-7890', f.inputDigit('0'));
}

function testAYTF_MX() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.MX);

  // +52 800 123 4567
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+5', f.inputDigit('5'));
  assertEquals('+52 ', f.inputDigit('2'));
  assertEquals('+52 8', f.inputDigit('8'));
  assertEquals('+52 80', f.inputDigit('0'));
  assertEquals('+52 800', f.inputDigit('0'));
  assertEquals('+52 800 1', f.inputDigit('1'));
  assertEquals('+52 800 12', f.inputDigit('2'));
  assertEquals('+52 800 123', f.inputDigit('3'));
  assertEquals('+52 800 123 4', f.inputDigit('4'));
  assertEquals('+52 800 123 45', f.inputDigit('5'));
  assertEquals('+52 800 123 456', f.inputDigit('6'));
  assertEquals('+52 800 123 4567', f.inputDigit('7'));
  
  // +529011234567, proactively ensuring that no formatting is applied, where a format is chosen
  // that would otherwise have led to some digits being dropped.
  f.clear();
  assertEquals('9', f.inputDigit('9'));
  assertEquals('90', f.inputDigit('0'));
  assertEquals('901', f.inputDigit('1'));
  assertEquals('9011', f.inputDigit('1'));
  assertEquals('90112', f.inputDigit('2'));
  assertEquals('901123', f.inputDigit('3'));
  assertEquals('9011234', f.inputDigit('4'));
  assertEquals('90112345', f.inputDigit('5'));
  assertEquals('901123456', f.inputDigit('6'));
  assertEquals('9011234567', f.inputDigit('7'));


  // +52 55 1234 5678
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+5', f.inputDigit('5'));
  assertEquals('+52 ', f.inputDigit('2'));
  assertEquals('+52 5', f.inputDigit('5'));
  assertEquals('+52 55', f.inputDigit('5'));
  assertEquals('+52 55 1', f.inputDigit('1'));
  assertEquals('+52 55 12', f.inputDigit('2'));
  assertEquals('+52 55 123', f.inputDigit('3'));
  assertEquals('+52 55 1234', f.inputDigit('4'));
  assertEquals('+52 55 1234 5', f.inputDigit('5'));
  assertEquals('+52 55 1234 56', f.inputDigit('6'));
  assertEquals('+52 55 1234 567', f.inputDigit('7'));
  assertEquals('+52 55 1234 5678', f.inputDigit('8'));

  // +52 212 345 6789
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+5', f.inputDigit('5'));
  assertEquals('+52 ', f.inputDigit('2'));
  assertEquals('+52 2', f.inputDigit('2'));
  assertEquals('+52 21', f.inputDigit('1'));
  assertEquals('+52 212', f.inputDigit('2'));
  assertEquals('+52 212 3', f.inputDigit('3'));
  assertEquals('+52 212 34', f.inputDigit('4'));
  assertEquals('+52 212 345', f.inputDigit('5'));
  assertEquals('+52 212 345 6', f.inputDigit('6'));
  assertEquals('+52 212 345 67', f.inputDigit('7'));
  assertEquals('+52 212 345 678', f.inputDigit('8'));
  assertEquals('+52 212 345 6789', f.inputDigit('9'));

  // +52 1 55 1234 5678
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+5', f.inputDigit('5'));
  assertEquals('+52 ', f.inputDigit('2'));
  assertEquals('+52 1', f.inputDigit('1'));
  assertEquals('+52 15', f.inputDigit('5'));
  assertEquals('+52 1 55', f.inputDigit('5'));
  assertEquals('+52 1 55 1', f.inputDigit('1'));
  assertEquals('+52 1 55 12', f.inputDigit('2'));
  assertEquals('+52 1 55 123', f.inputDigit('3'));
  assertEquals('+52 1 55 1234', f.inputDigit('4'));
  assertEquals('+52 1 55 1234 5', f.inputDigit('5'));
  assertEquals('+52 1 55 1234 56', f.inputDigit('6'));
  assertEquals('+52 1 55 1234 567', f.inputDigit('7'));
  assertEquals('+52 1 55 1234 5678', f.inputDigit('8'));

  // +52 1 541 234 5678
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+5', f.inputDigit('5'));
  assertEquals('+52 ', f.inputDigit('2'));
  assertEquals('+52 1', f.inputDigit('1'));
  assertEquals('+52 15', f.inputDigit('5'));
  assertEquals('+52 1 54', f.inputDigit('4'));
  assertEquals('+52 1 541', f.inputDigit('1'));
  assertEquals('+52 1 541 2', f.inputDigit('2'));
  assertEquals('+52 1 541 23', f.inputDigit('3'));
  assertEquals('+52 1 541 234', f.inputDigit('4'));
  assertEquals('+52 1 541 234 5', f.inputDigit('5'));
  assertEquals('+52 1 541 234 56', f.inputDigit('6'));
  assertEquals('+52 1 541 234 567', f.inputDigit('7'));
  assertEquals('+52 1 541 234 5678', f.inputDigit('8'));
}

function testAYTF_International_Toll_Free() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.US);
  // +800 1234 5678
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+80', f.inputDigit('0'));
  assertEquals('+800 ', f.inputDigit('0'));
  assertEquals('+800 1', f.inputDigit('1'));
  assertEquals('+800 12', f.inputDigit('2'));
  assertEquals('+800 123', f.inputDigit('3'));
  assertEquals('+800 1234', f.inputDigit('4'));
  assertEquals('+800 1234 5', f.inputDigit('5'));
  assertEquals('+800 1234 56', f.inputDigit('6'));
  assertEquals('+800 1234 567', f.inputDigit('7'));
  assertEquals('+800 1234 5678', f.inputDigit('8'));
  assertEquals('+800123456789', f.inputDigit('9'));
}

function testAYTFMultipleLeadingDigitPatterns() {
  // +81 50 2345 6789
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.JP);
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+81 ', f.inputDigit('1'));
  assertEquals('+81 5', f.inputDigit('5'));
  assertEquals('+81 50', f.inputDigit('0'));
  assertEquals('+81 50 2', f.inputDigit('2'));
  assertEquals('+81 50 23', f.inputDigit('3'));
  assertEquals('+81 50 234', f.inputDigit('4'));
  assertEquals('+81 50 2345', f.inputDigit('5'));
  assertEquals('+81 50 2345 6', f.inputDigit('6'));
  assertEquals('+81 50 2345 67', f.inputDigit('7'));
  assertEquals('+81 50 2345 678', f.inputDigit('8'));
  assertEquals('+81 50 2345 6789', f.inputDigit('9'));

  // +81 222 12 5678
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+81 ', f.inputDigit('1'));
  assertEquals('+81 2', f.inputDigit('2'));
  assertEquals('+81 22', f.inputDigit('2'));
  assertEquals('+81 22 2', f.inputDigit('2'));
  assertEquals('+81 22 21', f.inputDigit('1'));
  assertEquals('+81 2221 2', f.inputDigit('2'));
  assertEquals('+81 222 12 5', f.inputDigit('5'));
  assertEquals('+81 222 12 56', f.inputDigit('6'));
  assertEquals('+81 222 12 567', f.inputDigit('7'));
  assertEquals('+81 222 12 5678', f.inputDigit('8'));

  // 011113
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011', f.inputDigit('1'));
  assertEquals('011 1', f.inputDigit('1'));
  assertEquals('011 11', f.inputDigit('1'));
  assertEquals('011113', f.inputDigit('3'));

  // +81 3332 2 5678
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+81 ', f.inputDigit('1'));
  assertEquals('+81 3', f.inputDigit('3'));
  assertEquals('+81 33', f.inputDigit('3'));
  assertEquals('+81 33 3', f.inputDigit('3'));
  assertEquals('+81 3332', f.inputDigit('2'));
  assertEquals('+81 3332 2', f.inputDigit('2'));
  assertEquals('+81 3332 2 5', f.inputDigit('5'));
  assertEquals('+81 3332 2 56', f.inputDigit('6'));
  assertEquals('+81 3332 2 567', f.inputDigit('7'));
  assertEquals('+81 3332 2 5678', f.inputDigit('8'));
}

function testAYTFLongIDD_AU() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.AU);
  // 0011 1 650 253 2250
  assertEquals('0', f.inputDigit('0'));
  assertEquals('00', f.inputDigit('0'));
  assertEquals('001', f.inputDigit('1'));
  assertEquals('0011', f.inputDigit('1'));
  assertEquals('0011 1 ', f.inputDigit('1'));
  assertEquals('0011 1 6', f.inputDigit('6'));
  assertEquals('0011 1 65', f.inputDigit('5'));
  assertEquals('0011 1 650', f.inputDigit('0'));
  assertEquals('0011 1 650 2', f.inputDigit('2'));
  assertEquals('0011 1 650 25', f.inputDigit('5'));
  assertEquals('0011 1 650 253', f.inputDigit('3'));
  assertEquals('0011 1 650 253 2', f.inputDigit('2'));
  assertEquals('0011 1 650 253 22', f.inputDigit('2'));
  assertEquals('0011 1 650 253 222', f.inputDigit('2'));
  assertEquals('0011 1 650 253 2222', f.inputDigit('2'));

  // 0011 81 3332 2 5678
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('00', f.inputDigit('0'));
  assertEquals('001', f.inputDigit('1'));
  assertEquals('0011', f.inputDigit('1'));
  assertEquals('00118', f.inputDigit('8'));
  assertEquals('0011 81 ', f.inputDigit('1'));
  assertEquals('0011 81 3', f.inputDigit('3'));
  assertEquals('0011 81 33', f.inputDigit('3'));
  assertEquals('0011 81 33 3', f.inputDigit('3'));
  assertEquals('0011 81 3332', f.inputDigit('2'));
  assertEquals('0011 81 3332 2', f.inputDigit('2'));
  assertEquals('0011 81 3332 2 5', f.inputDigit('5'));
  assertEquals('0011 81 3332 2 56', f.inputDigit('6'));
  assertEquals('0011 81 3332 2 567', f.inputDigit('7'));
  assertEquals('0011 81 3332 2 5678', f.inputDigit('8'));

  // 0011 244 250 253 222
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('00', f.inputDigit('0'));
  assertEquals('001', f.inputDigit('1'));
  assertEquals('0011', f.inputDigit('1'));
  assertEquals('00112', f.inputDigit('2'));
  assertEquals('001124', f.inputDigit('4'));
  assertEquals('0011 244 ', f.inputDigit('4'));
  assertEquals('0011 244 2', f.inputDigit('2'));
  assertEquals('0011 244 25', f.inputDigit('5'));
  assertEquals('0011 244 250', f.inputDigit('0'));
  assertEquals('0011 244 250 2', f.inputDigit('2'));
  assertEquals('0011 244 250 25', f.inputDigit('5'));
  assertEquals('0011 244 250 253', f.inputDigit('3'));
  assertEquals('0011 244 250 253 2', f.inputDigit('2'));
  assertEquals('0011 244 250 253 22', f.inputDigit('2'));
  assertEquals('0011 244 250 253 222', f.inputDigit('2'));
}

function testAYTFLongIDD_KR() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.KR);
  // 00300 1 650 253 2222
  assertEquals('0', f.inputDigit('0'));
  assertEquals('00', f.inputDigit('0'));
  assertEquals('003', f.inputDigit('3'));
  assertEquals('0030', f.inputDigit('0'));
  assertEquals('00300', f.inputDigit('0'));
  assertEquals('00300 1 ', f.inputDigit('1'));
  assertEquals('00300 1 6', f.inputDigit('6'));
  assertEquals('00300 1 65', f.inputDigit('5'));
  assertEquals('00300 1 650', f.inputDigit('0'));
  assertEquals('00300 1 650 2', f.inputDigit('2'));
  assertEquals('00300 1 650 25', f.inputDigit('5'));
  assertEquals('00300 1 650 253', f.inputDigit('3'));
  assertEquals('00300 1 650 253 2', f.inputDigit('2'));
  assertEquals('00300 1 650 253 22', f.inputDigit('2'));
  assertEquals('00300 1 650 253 222', f.inputDigit('2'));
  assertEquals('00300 1 650 253 2222', f.inputDigit('2'));
}

function testAYTFLongNDD_KR() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.KR);
  // 08811-9876-7890
  assertEquals('0', f.inputDigit('0'));
  assertEquals('08', f.inputDigit('8'));
  assertEquals('088', f.inputDigit('8'));
  assertEquals('0881', f.inputDigit('1'));
  assertEquals('08811', f.inputDigit('1'));
  assertEquals('08811-9', f.inputDigit('9'));
  assertEquals('08811-98', f.inputDigit('8'));
  assertEquals('08811-987', f.inputDigit('7'));
  assertEquals('08811-9876', f.inputDigit('6'));
  assertEquals('08811-9876-7', f.inputDigit('7'));
  assertEquals('08811-9876-78', f.inputDigit('8'));
  assertEquals('08811-9876-789', f.inputDigit('9'));
  assertEquals('08811-9876-7890', f.inputDigit('0'));

  // 08500 11-9876-7890
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('08', f.inputDigit('8'));
  assertEquals('085', f.inputDigit('5'));
  assertEquals('0850', f.inputDigit('0'));
  assertEquals('08500 ', f.inputDigit('0'));
  assertEquals('08500 1', f.inputDigit('1'));
  assertEquals('08500 11', f.inputDigit('1'));
  assertEquals('08500 11-9', f.inputDigit('9'));
  assertEquals('08500 11-98', f.inputDigit('8'));
  assertEquals('08500 11-987', f.inputDigit('7'));
  assertEquals('08500 11-9876', f.inputDigit('6'));
  assertEquals('08500 11-9876-7', f.inputDigit('7'));
  assertEquals('08500 11-9876-78', f.inputDigit('8'));
  assertEquals('08500 11-9876-789', f.inputDigit('9'));
  assertEquals('08500 11-9876-7890', f.inputDigit('0'));
}

function testAYTFLongNDD_SG() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.SG);
  // 777777 9876 7890
  assertEquals('7', f.inputDigit('7'));
  assertEquals('77', f.inputDigit('7'));
  assertEquals('777', f.inputDigit('7'));
  assertEquals('7777', f.inputDigit('7'));
  assertEquals('77777', f.inputDigit('7'));
  assertEquals('777777 ', f.inputDigit('7'));
  assertEquals('777777 9', f.inputDigit('9'));
  assertEquals('777777 98', f.inputDigit('8'));
  assertEquals('777777 987', f.inputDigit('7'));
  assertEquals('777777 9876', f.inputDigit('6'));
  assertEquals('777777 9876 7', f.inputDigit('7'));
  assertEquals('777777 9876 78', f.inputDigit('8'));
  assertEquals('777777 9876 789', f.inputDigit('9'));
  assertEquals('777777 9876 7890', f.inputDigit('0'));
}

function testAYTFShortNumberFormattingFix_AU() {
  // For Australia, the national prefix is not optional when formatting.
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.AU);

  // 1234567890 - For leading digit 1, the national prefix formatting rule has
  // first group only.
  assertEquals('1', f.inputDigit('1'));
  assertEquals('12', f.inputDigit('2'));
  assertEquals('123', f.inputDigit('3'));
  assertEquals('1234', f.inputDigit('4'));
  assertEquals('1234 5', f.inputDigit('5'));
  assertEquals('1234 56', f.inputDigit('6'));
  assertEquals('1234 567', f.inputDigit('7'));
  assertEquals('1234 567 8', f.inputDigit('8'));
  assertEquals('1234 567 89', f.inputDigit('9'));
  assertEquals('1234 567 890', f.inputDigit('0'));

  // +61 1234 567 890 - Test the same number, but with the country code.
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+6', f.inputDigit('6'));
  assertEquals('+61 ', f.inputDigit('1'));
  assertEquals('+61 1', f.inputDigit('1'));
  assertEquals('+61 12', f.inputDigit('2'));
  assertEquals('+61 123', f.inputDigit('3'));
  assertEquals('+61 1234', f.inputDigit('4'));
  assertEquals('+61 1234 5', f.inputDigit('5'));
  assertEquals('+61 1234 56', f.inputDigit('6'));
  assertEquals('+61 1234 567', f.inputDigit('7'));
  assertEquals('+61 1234 567 8', f.inputDigit('8'));
  assertEquals('+61 1234 567 89', f.inputDigit('9'));
  assertEquals('+61 1234 567 890', f.inputDigit('0'));

  // 212345678 - For leading digit 2, the national prefix formatting rule puts
  // the national prefix before the first group.
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('021', f.inputDigit('1'));
  assertEquals('02 12', f.inputDigit('2'));
  assertEquals('02 123', f.inputDigit('3'));
  assertEquals('02 1234', f.inputDigit('4'));
  assertEquals('02 1234 5', f.inputDigit('5'));
  assertEquals('02 1234 56', f.inputDigit('6'));
  assertEquals('02 1234 567', f.inputDigit('7'));
  assertEquals('02 1234 5678', f.inputDigit('8'));

  // 212345678 - Test the same number, but without the leading 0.
  f.clear();
  assertEquals('2', f.inputDigit('2'));
  assertEquals('21', f.inputDigit('1'));
  assertEquals('212', f.inputDigit('2'));
  assertEquals('2123', f.inputDigit('3'));
  assertEquals('21234', f.inputDigit('4'));
  assertEquals('212345', f.inputDigit('5'));
  assertEquals('2123456', f.inputDigit('6'));
  assertEquals('21234567', f.inputDigit('7'));
  assertEquals('212345678', f.inputDigit('8'));

  // +61 2 1234 5678 - Test the same number, but with the country code.
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+6', f.inputDigit('6'));
  assertEquals('+61 ', f.inputDigit('1'));
  assertEquals('+61 2', f.inputDigit('2'));
  assertEquals('+61 21', f.inputDigit('1'));
  assertEquals('+61 2 12', f.inputDigit('2'));
  assertEquals('+61 2 123', f.inputDigit('3'));
  assertEquals('+61 2 1234', f.inputDigit('4'));
  assertEquals('+61 2 1234 5', f.inputDigit('5'));
  assertEquals('+61 2 1234 56', f.inputDigit('6'));
  assertEquals('+61 2 1234 567', f.inputDigit('7'));
  assertEquals('+61 2 1234 5678', f.inputDigit('8'));
}

function testAYTFShortNumberFormattingFix_KR() {
  // For Korea, the national prefix is not optional when formatting, and the
  // national prefix formatting rule doesn't consist of only the first group.
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.KR);

  // 111
  assertEquals('1', f.inputDigit('1'));
  assertEquals('11', f.inputDigit('1'));
  assertEquals('111', f.inputDigit('1'));

  // 114
  f.clear();
  assertEquals('1', f.inputDigit('1'));
  assertEquals('11', f.inputDigit('1'));
  assertEquals('114', f.inputDigit('4'));

  // 13121234 - Test a mobile number without the national prefix. Even though it
  // is not an emergency number, it should be formatted as a block.
  f.clear();
  assertEquals('1', f.inputDigit('1'));
  assertEquals('13', f.inputDigit('3'));
  assertEquals('131', f.inputDigit('1'));
  assertEquals('1312', f.inputDigit('2'));
  assertEquals('13121', f.inputDigit('1'));
  assertEquals('131212', f.inputDigit('2'));
  assertEquals('1312123', f.inputDigit('3'));
  assertEquals('13121234', f.inputDigit('4'));

  // +82 131-2-1234 - Test the same number, but with the country code.
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+82 ', f.inputDigit('2'));
  assertEquals('+82 1', f.inputDigit('1'));
  assertEquals('+82 13', f.inputDigit('3'));
  assertEquals('+82 131', f.inputDigit('1'));
  assertEquals('+82 131-2', f.inputDigit('2'));
  assertEquals('+82 131-2-1', f.inputDigit('1'));
  assertEquals('+82 131-2-12', f.inputDigit('2'));
  assertEquals('+82 131-2-123', f.inputDigit('3'));
  assertEquals('+82 131-2-1234', f.inputDigit('4'));
}

function testAYTFShortNumberFormattingFix_MX() {
  // For Mexico, the national prefix is optional when formatting.
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.MX);

  // 911
  assertEquals('9', f.inputDigit('9'));
  assertEquals('91', f.inputDigit('1'));
  assertEquals('911', f.inputDigit('1'));

  // 800 123 4567 - Test a toll-free number, which should have a formatting rule
  // applied to it even though it doesn't begin with the national prefix.
  f.clear();
  assertEquals('8', f.inputDigit('8'));
  assertEquals('80', f.inputDigit('0'));
  assertEquals('800', f.inputDigit('0'));
  assertEquals('800 1', f.inputDigit('1'));
  assertEquals('800 12', f.inputDigit('2'));
  assertEquals('800 123', f.inputDigit('3'));
  assertEquals('800 123 4', f.inputDigit('4'));
  assertEquals('800 123 45', f.inputDigit('5'));
  assertEquals('800 123 456', f.inputDigit('6'));
  assertEquals('800 123 4567', f.inputDigit('7'));

  // +52 800 123 4567 - Test the same number, but with the country code.
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+5', f.inputDigit('5'));
  assertEquals('+52 ', f.inputDigit('2'));
  assertEquals('+52 8', f.inputDigit('8'));
  assertEquals('+52 80', f.inputDigit('0'));
  assertEquals('+52 800', f.inputDigit('0'));
  assertEquals('+52 800 1', f.inputDigit('1'));
  assertEquals('+52 800 12', f.inputDigit('2'));
  assertEquals('+52 800 123', f.inputDigit('3'));
  assertEquals('+52 800 123 4', f.inputDigit('4'));
  assertEquals('+52 800 123 45', f.inputDigit('5'));
  assertEquals('+52 800 123 456', f.inputDigit('6'));
  assertEquals('+52 800 123 4567', f.inputDigit('7'));
}

function testAYTFNoNationalPrefix() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.IT);
  assertEquals('3', f.inputDigit('3'));
  assertEquals('33', f.inputDigit('3'));
  assertEquals('333', f.inputDigit('3'));
  assertEquals('333 3', f.inputDigit('3'));
  assertEquals('333 33', f.inputDigit('3'));
  assertEquals('333 333', f.inputDigit('3'));
}

function testAYTFNoNationalPrefixFormattingRule() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.AO);
  assertEquals('3', f.inputDigit('3'));
  assertEquals('33', f.inputDigit('3'));
  assertEquals('333', f.inputDigit('3'));
  assertEquals('333 3', f.inputDigit('3'));
  assertEquals('333 33', f.inputDigit('3'));
  assertEquals('333 333', f.inputDigit('3'));
}

function testAYTFShortNumberFormattingFix_US() {
  // For the US, an initial 1 is treated specially.
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.US);

  // 101 - Test that the initial 1 is not treated as a national prefix.
  assertEquals('1', f.inputDigit('1'));
  assertEquals('10', f.inputDigit('0'));
  assertEquals('101', f.inputDigit('1'));

  // 112 - Test that the initial 1 is not treated as a national prefix.
  f.clear();
  assertEquals('1', f.inputDigit('1'));
  assertEquals('11', f.inputDigit('1'));
  assertEquals('112', f.inputDigit('2'));

  // 122 - Test that the initial 1 is treated as a national prefix.
  f.clear();
  assertEquals('1', f.inputDigit('1'));
  assertEquals('12', f.inputDigit('2'));
  assertEquals('1 22', f.inputDigit('2'));
}

function testAYTFClearNDDAfterIddExtraction() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.KR);

  assertEquals('0', f.inputDigit('0'));
  assertEquals('00', f.inputDigit('0'));
  assertEquals('007', f.inputDigit('7'));
  assertEquals('0070', f.inputDigit('0'));
  assertEquals('00700', f.inputDigit('0'));
  // NDD is '0' at this stage (the first '0' in '00700') because it's not
  // clear if the number is a national number or using the IDD to dial out.
  assertEquals('00700 1 ', f.inputDigit('1'));
  // NDD should be cleared here because IDD '00700' was extracted after the
  // country calling code '1' (US) was entered.
  assertEquals('00700 1 2', f.inputDigit('2'));
  // The remaining long sequence of inputs is because the original bug that
  // this test if for only triggered after a lot of subsequent inputs.
  assertEquals('00700 1 23', f.inputDigit('3'));
  assertEquals('00700 1 234', f.inputDigit('4'));
  assertEquals('00700 1 234 5', f.inputDigit('5'));
  assertEquals('00700 1 234 56', f.inputDigit('6'));
  assertEquals('00700 1 234 567', f.inputDigit('7'));
  assertEquals('00700 1 234 567 8', f.inputDigit('8'));
  assertEquals('00700 1 234 567 89', f.inputDigit('9'));
  assertEquals('00700 1 234 567 890', f.inputDigit('0'));
  assertEquals('00700 1 234 567 8901', f.inputDigit('1'));
  assertEquals('00700123456789012', f.inputDigit('2'));
  assertEquals('007001234567890123', f.inputDigit('3'));
  assertEquals('0070012345678901234', f.inputDigit('4'));
  assertEquals('00700123456789012345', f.inputDigit('5'));
  assertEquals('007001234567890123456', f.inputDigit('6'));
  assertEquals('0070012345678901234567', f.inputDigit('7'));
}

function testAYTFNumberPatternsBecomingInvalidShouldNotResultInDigitLoss() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter(RegionCode.CN);

  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+86 ', f.inputDigit('6'));
  assertEquals('+86 9', f.inputDigit('9'));
  assertEquals('+86 98', f.inputDigit('8'));
  assertEquals('+86 988', f.inputDigit('8'));
  assertEquals('+86 988 1', f.inputDigit('1'));
  // Now the number pattern is no longer valid because there are multiple
  // leading digit patterns; when we try again to extract a country code we
  // should ensure we use the last leading digit pattern, rather than the first
  // one such that it *thinks* it's found a valid formatting rule again.
  // https://github.com/google/libphonenumber/issues/437
  assertEquals('+8698812', f.inputDigit('2'));
  assertEquals('+86988123', f.inputDigit('3'));
  assertEquals('+869881234', f.inputDigit('4'));
  assertEquals('+8698812345', f.inputDigit('5'));
}
