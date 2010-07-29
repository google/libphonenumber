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
 * @fileoverview  Unit tests for the AsYouTypeFormatter.
 *
 * @author Nikolaos Trogkanis
 */

goog.require('goog.testing.jsunit');
goog.require('i18n.phonenumbers.AsYouTypeFormatter');

function testAsYouTypeFormatterUS() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('US');
  assertEquals('6', f.inputDigit('6'));
  assertEquals('65', f.inputDigit('5'));
  assertEquals('650', f.inputDigit('0'));
  assertEquals('6502', f.inputDigit('2'));
  assertEquals('65025', f.inputDigit('5'));
  assertEquals('650 253', f.inputDigit('3'));
  assertEquals('650 253 2', f.inputDigit('2'));
  assertEquals('650 253 22', f.inputDigit('2'));
  assertEquals('650 253 222', f.inputDigit('2'));
  assertEquals('650 253 2222', f.inputDigit('2'));

  f.clear();
  assertEquals('1', f.inputDigit('1'));
  assertEquals('16', f.inputDigit('6'));
  assertEquals('165', f.inputDigit('5'));
  assertEquals('1650', f.inputDigit('0'));
  assertEquals('16502', f.inputDigit('2'));
  assertEquals('1 650 25', f.inputDigit('5'));
  assertEquals('1 650 253', f.inputDigit('3'));
  assertEquals('1 650 253 2', f.inputDigit('2'));
  assertEquals('1 650 253 22', f.inputDigit('2'));
  assertEquals('1 650 253 222', f.inputDigit('2'));
  assertEquals('1 650 253 2222', f.inputDigit('2'));

  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011', f.inputDigit('1'));
  assertEquals('0114', f.inputDigit('4'));
  assertEquals('01144', f.inputDigit('4'));
  assertEquals('011 44 6', f.inputDigit('6'));
  assertEquals('011 44 61', f.inputDigit('1'));
  assertEquals('011 44 612', f.inputDigit('2'));
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
  assertEquals('011', f.inputDigit('1'));
  assertEquals('0115', f.inputDigit('5'));
  assertEquals('01154', f.inputDigit('4'));
  assertEquals('011 54 9', f.inputDigit('9'));
  assertEquals('011 54 91', f.inputDigit('1'));
  assertEquals('011 54 911', f.inputDigit('1'));
  assertEquals('011 54 9 11 2', f.inputDigit('2'));
  assertEquals('011 54 9 11 23', f.inputDigit('3'));
  assertEquals('011 54 9 11 231', f.inputDigit('1'));
  assertEquals('011 54 9 11 2312', f.inputDigit('2'));
  assertEquals('011 54 9 11 2312 1', f.inputDigit('1'));
  assertEquals('011 54 9 11 2312 12', f.inputDigit('2'));
  assertEquals('011 54 9 11 2312 123', f.inputDigit('3'));
  assertEquals('011 54 9 11 2312 1234', f.inputDigit('4'));

  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+4', f.inputDigit('4'));
  assertEquals('+48', f.inputDigit('8'));
  assertEquals('+488', f.inputDigit('8'));
  assertEquals('+4888', f.inputDigit('8'));
  assertEquals('+48 881', f.inputDigit('1'));
  assertEquals('+48 88 12', f.inputDigit('2'));
  assertEquals('+48 88 123', f.inputDigit('3'));
  assertEquals('+48 88 123 1', f.inputDigit('1'));
  assertEquals('+48 88 123 12', f.inputDigit('2'));
  assertEquals('+48 88 123 12 1', f.inputDigit('1'));
  assertEquals('+48 88 123 12 12', f.inputDigit('2'));
}

function testAsYouTypeFormatterUSFullWidthCharacters() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('US');
  assertEquals('\uFF16', f.inputDigit('\uFF16'));
  assertEquals('\uFF16\uFF15', f.inputDigit('\uFF15'));
  assertEquals('\uFF16\uFF15\uFF10', f.inputDigit('\uFF10'));
  assertEquals('\uFF16\uFF15\uFF10\uFF12', f.inputDigit('\uFF12'));
  assertEquals('\uFF16\uFF15\uFF10\uFF12\uFF15', f.inputDigit('\uFF15'));
  assertEquals('650 253', f.inputDigit('\uFF13'));
  assertEquals('650 253 2', f.inputDigit('\uFF12'));
  assertEquals('650 253 22', f.inputDigit('\uFF12'));
  assertEquals('650 253 222', f.inputDigit('\uFF12'));
  assertEquals('650 253 2222', f.inputDigit('\uFF12'));
}

function testAsYouTypeFormatterUSMobileShortCode() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('US');
  assertEquals('*', f.inputDigit('*'));
  assertEquals('*1', f.inputDigit('1'));
  assertEquals('*12', f.inputDigit('2'));
  assertEquals('*121', f.inputDigit('1'));
  assertEquals('*121#', f.inputDigit('#'));
}

function testAsYouTypeFormatterUSVanityNumber() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('US');
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

function testAsYouTypeFormatterAndRememberPositionUS() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('US');
  assertEquals('1', f.inputDigitAndRememberPosition('1'));
  assertEquals(1, f.getRememberedPosition());
  assertEquals('16', f.inputDigit('6'));
  assertEquals('165', f.inputDigit('5'));
  assertEquals(1, f.getRememberedPosition());
  assertEquals('1650', f.inputDigitAndRememberPosition('0'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('16502', f.inputDigit('2'));
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
  assertEquals('16', f.inputDigit('6'));
  assertEquals('165', f.inputDigitAndRememberPosition('5'));
  assertEquals('1650', f.inputDigit('0'));
  assertEquals(3, f.getRememberedPosition());
  assertEquals('16502', f.inputDigit('2'));
  assertEquals('1 650 25', f.inputDigit('5'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('1 650 253', f.inputDigit('3'));
  assertEquals('1 650 253 2', f.inputDigit('2'));
  assertEquals('1 650 253 22', f.inputDigit('2'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('1 650 253 222', f.inputDigit('2'));
  assertEquals('1 650 253 2222', f.inputDigit('2'));
  assertEquals('165025322222', f.inputDigit('2'));
  assertEquals(3, f.getRememberedPosition());
  assertEquals('1650253222222', f.inputDigit('2'));
  assertEquals(3, f.getRememberedPosition());

  f.clear();
  assertEquals('6', f.inputDigit('6'));
  assertEquals('65', f.inputDigit('5'));
  assertEquals('650', f.inputDigit('0'));
  assertEquals('6502', f.inputDigit('2'));
  assertEquals('65025', f.inputDigitAndRememberPosition('5'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('650 253', f.inputDigit('3'));
  assertEquals(6, f.getRememberedPosition());
  assertEquals('650 253 2', f.inputDigit('2'));
  assertEquals('650 253 22', f.inputDigit('2'));
  assertEquals('650 253 222', f.inputDigit('2'));
  // No more formatting when semicolon is entered.
  assertEquals('650253222;', f.inputDigit(';'));
  assertEquals(5, f.getRememberedPosition());
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
  assertEquals('011', f.inputDigit('1'));
  assertEquals('0114', f.inputDigitAndRememberPosition('4'));
  assertEquals('01148', f.inputDigit('8'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('011 48 8', f.inputDigit('8'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('011 48 88', f.inputDigit('8'));
  assertEquals('011 48 881', f.inputDigit('1'));
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
  assertEquals('+16', f.inputDigitAndRememberPosition('6'));
  assertEquals('+165', f.inputDigit('5'));
  assertEquals('+1650', f.inputDigit('0'));
  assertEquals(3, f.getRememberedPosition());
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
  assertEquals('+16', f.inputDigitAndRememberPosition('6'));
  assertEquals('+165', f.inputDigit('5'));
  assertEquals('+1650', f.inputDigit('0'));
  assertEquals(3, f.getRememberedPosition());
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

function testAsYouTypeFormatterGBFixedLine() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('GB');
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('020', f.inputDigit('0'));
  assertEquals('0207', f.inputDigitAndRememberPosition('7'));
  assertEquals(4, f.getRememberedPosition());
  assertEquals('02070', f.inputDigit('0'));
  assertEquals('020 703', f.inputDigit('3'));
  assertEquals(5, f.getRememberedPosition());
  assertEquals('020 7031', f.inputDigit('1'));
  assertEquals('020 7031 3', f.inputDigit('3'));
  assertEquals('020 7031 30', f.inputDigit('0'));
  assertEquals('020 7031 300', f.inputDigit('0'));
  assertEquals('020 7031 3000', f.inputDigit('0'));
}

function testAsYouTypeFormatterGBTollFree() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('gb');
  assertEquals('0', f.inputDigit('0'));
  assertEquals('08', f.inputDigit('8'));
  assertEquals('080', f.inputDigit('0'));
  assertEquals('0807', f.inputDigit('7'));
  assertEquals('08070', f.inputDigit('0'));
  assertEquals('080 703', f.inputDigit('3'));
  assertEquals('080 7031', f.inputDigit('1'));
  assertEquals('080 7031 3', f.inputDigit('3'));
  assertEquals('080 7031 30', f.inputDigit('0'));
  assertEquals('080 7031 300', f.inputDigit('0'));
  assertEquals('080 7031 3000', f.inputDigit('0'));
}

function testAsYouTypeFormatterGBPremiumRate() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('GB');
  assertEquals('0', f.inputDigit('0'));
  assertEquals('09', f.inputDigit('9'));
  assertEquals('090', f.inputDigit('0'));
  assertEquals('0907', f.inputDigit('7'));
  assertEquals('09070', f.inputDigit('0'));
  assertEquals('090 703', f.inputDigit('3'));
  assertEquals('090 7031', f.inputDigit('1'));
  assertEquals('090 7031 3', f.inputDigit('3'));
  assertEquals('090 7031 30', f.inputDigit('0'));
  assertEquals('090 7031 300', f.inputDigit('0'));
  assertEquals('090 7031 3000', f.inputDigit('0'));
}

function testAsYouTypeFormatterNZMobile() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('NZ');
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('021', f.inputDigit('1'));
  assertEquals('0211', f.inputDigit('1'));
  assertEquals('02112', f.inputDigit('2'));
  // Note the unittest is using fake metadata which might produce non-ideal
  // results.
  assertEquals('02-112 3', f.inputDigit('3'));
  assertEquals('02-112 34', f.inputDigit('4'));
  assertEquals('02-112 345', f.inputDigit('5'));
  assertEquals('02-112 3456', f.inputDigit('6'));
}

function testAsYouTypeFormatterDE() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('DE');
  assertEquals('0', f.inputDigit('0'));
  assertEquals('03', f.inputDigit('3'));
  assertEquals('030', f.inputDigit('0'));
  assertEquals('0301', f.inputDigit('1'));
  assertEquals('03012', f.inputDigit('2'));
  assertEquals('030 123', f.inputDigit('3'));
  assertEquals('030 1234', f.inputDigit('4'));
}

function testAsYouTypeFormatterAR() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('AR');
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011', f.inputDigit('1'));
  assertEquals('0117', f.inputDigit('7'));
  assertEquals('01170', f.inputDigit('0'));
  assertEquals('011 703', f.inputDigit('3'));
  assertEquals('011 7031', f.inputDigit('1'));
  assertEquals('011 7031-3', f.inputDigit('3'));
  assertEquals('011 7031-30', f.inputDigit('0'));
  assertEquals('011 7031-300', f.inputDigit('0'));
  assertEquals('011 7031-3000', f.inputDigit('0'));
}

function testAsYouTypeFormatterARMobile() {
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('AR');
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+5', f.inputDigit('5'));
  assertEquals('+54', f.inputDigit('4'));
  assertEquals('+549', f.inputDigit('9'));
  assertEquals('+5491', f.inputDigit('1'));
  assertEquals('+54 911', f.inputDigit('1'));
  assertEquals('+54 9 11 2', f.inputDigit('2'));
  assertEquals('+54 9 11 23', f.inputDigit('3'));
  assertEquals('+54 9 11 231', f.inputDigit('1'));
  assertEquals('+54 9 11 2312', f.inputDigit('2'));
  assertEquals('+54 9 11 2312 1', f.inputDigit('1'));
  assertEquals('+54 9 11 2312 12', f.inputDigit('2'));
  assertEquals('+54 9 11 2312 123', f.inputDigit('3'));
  assertEquals('+54 9 11 2312 1234', f.inputDigit('4'));
}

function testAsYouTypeFormatterKR() {
  // +82 51 234 5678
  /** @type {i18n.phonenumbers.AsYouTypeFormatter} */
  var f = new i18n.phonenumbers.AsYouTypeFormatter('KR');
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+82', f.inputDigit('2'));
  assertEquals('+825', f.inputDigit('5'));
  assertEquals('+8251', f.inputDigit('1'));
  assertEquals('+82 512', f.inputDigit('2'));
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
  assertEquals('+82', f.inputDigit('2'));
  assertEquals('+822', f.inputDigit('2'));
  assertEquals('+8225', f.inputDigit('5'));
  assertEquals('+82 253', f.inputDigit('3'));
  assertEquals('+82 2-531', f.inputDigit('1'));
  assertEquals('+82 2-531-5', f.inputDigit('5'));
  assertEquals('+82 2-531-56', f.inputDigit('6'));
  assertEquals('+82 2-531-567', f.inputDigit('7'));
  assertEquals('+82 2-531-5678', f.inputDigit('8'));

  // +82 2 3665 5678
  f.clear();
  assertEquals('+', f.inputDigit('+'));
  assertEquals('+8', f.inputDigit('8'));
  assertEquals('+82', f.inputDigit('2'));
  assertEquals('+822', f.inputDigit('2'));
  assertEquals('+8223', f.inputDigit('3'));
  assertEquals('+82 236', f.inputDigit('6'));
  assertEquals('+82 2-366', f.inputDigit('6'));
  assertEquals('+82 2-3665', f.inputDigit('5'));
  assertEquals('+82 2-3665-5', f.inputDigit('5'));
  assertEquals('+82 2-3665-56', f.inputDigit('6'));
  assertEquals('+82 2-3665-567', f.inputDigit('7'));
  assertEquals('+82 2-3665-5678', f.inputDigit('8'));

  // 02-114 : This is too short to format. Checking that there are no
  // side-effects.
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('021', f.inputDigit('1'));
  assertEquals('0211', f.inputDigit('1'));
  assertEquals('02114', f.inputDigit('4'));

  // 02-1300
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('02', f.inputDigit('2'));
  assertEquals('021', f.inputDigit('1'));
  assertEquals('0213', f.inputDigit('3'));
  assertEquals('02130', f.inputDigit('0'));
  assertEquals('02-1300', f.inputDigit('0'));

  // 011-456-7890
  f.clear();
  assertEquals('0', f.inputDigit('0'));
  assertEquals('01', f.inputDigit('1'));
  assertEquals('011', f.inputDigit('1'));
  assertEquals('0114', f.inputDigit('4'));
  assertEquals('01145', f.inputDigit('5'));
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
  assertEquals('0119', f.inputDigit('9'));
  assertEquals('01198', f.inputDigit('8'));
  assertEquals('011-987', f.inputDigit('7'));
  assertEquals('011-9876', f.inputDigit('6'));
  assertEquals('011-9876-7', f.inputDigit('7'));
  assertEquals('011-9876-78', f.inputDigit('8'));
  assertEquals('011-9876-789', f.inputDigit('9'));
  assertEquals('011-9876-7890', f.inputDigit('0'));
}
