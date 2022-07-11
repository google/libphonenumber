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
 * @fileoverview  Unit tests for the ShortNumberInfo.
 *
 * Note that these tests use the test metadata for PhoneNumberUtil related
 * operations, but the real short number metadata for testing ShortNumberInfo
 * specific operations. This is not intentional, but mirrors the current state
 * of the Java test suite.
 *
 * @author James Wright
 */
goog.provide('i18n.phonenumbers.ShortNumberInfoTest');
goog.setTestOnly();

goog.require('goog.testing.jsunit');
goog.require('i18n.phonenumbers.PhoneNumber');
goog.require('i18n.phonenumbers.PhoneNumberUtil');
goog.require('i18n.phonenumbers.RegionCode');
goog.require('i18n.phonenumbers.ShortNumberInfo');


/** @type {i18n.phonenumbers.ShortNumberInfo} */
var shortInfo = i18n.phonenumbers.ShortNumberInfo.getInstance();


/** @type {i18n.phonenumbers.PhoneNumberUtil} */
var phoneUtil = i18n.phonenumbers.PhoneNumberUtil.getInstance();

var RegionCode = i18n.phonenumbers.RegionCode;

function testIsPossibleShortNumber() {
  var possibleNumber = new i18n.phonenumbers.PhoneNumber();
  possibleNumber.setCountryCode(33);
  possibleNumber.setNationalNumber(123456);
  assertTrue(shortInfo.isPossibleShortNumber(possibleNumber));
  assertTrue(shortInfo.isPossibleShortNumberForRegion(
      phoneUtil.parse('123456', RegionCode.FR), RegionCode.FR));

  var impossibleNumber = new i18n.phonenumbers.PhoneNumber();
  impossibleNumber.setCountryCode(33);
  impossibleNumber.setNationalNumber(9);
  assertFalse(shortInfo.isPossibleShortNumber(impossibleNumber));

  // Note that GB and GG share the country calling code 44, and that this number
  // is possible but not valid.
  var impossibleUkNumber = new i18n.phonenumbers.PhoneNumber();
  impossibleUkNumber.setCountryCode(44);
  impossibleUkNumber.setNationalNumber(11001);
  assertTrue(shortInfo.isPossibleShortNumber(impossibleUkNumber));
}

function testIsValidShortNumber() {
  var shortNumber1 = new i18n.phonenumbers.PhoneNumber();
  shortNumber1.setCountryCode(33);
  shortNumber1.setNationalNumber(1010);
  assertTrue(shortInfo.isValidShortNumber(shortNumber1));
  assertTrue(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse('1010', RegionCode.FR), RegionCode.FR));
  var shortNumber2 = new i18n.phonenumbers.PhoneNumber();
  shortNumber2.setCountryCode(33);
  shortNumber2.setNationalNumber(123456);
  assertFalse(shortInfo.isValidShortNumber(shortNumber2));
  assertFalse(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse('123456', RegionCode.FR), RegionCode.FR));

  // Note that GB and GG share the country calling code 44.
  var shortNumber3 = new i18n.phonenumbers.PhoneNumber();
  shortNumber3.setCountryCode(44);
  shortNumber3.setNationalNumber(18001);
  assertTrue(shortInfo.isValidShortNumber(shortNumber3));
}

function testIsCarrierSpecific() {
  var carrierSpecificNumber = new i18n.phonenumbers.PhoneNumber();
  carrierSpecificNumber.setCountryCode(1);
  carrierSpecificNumber.setNationalNumber(33669);
  assertTrue(shortInfo.isCarrierSpecific(carrierSpecificNumber));
  assertTrue(shortInfo.isCarrierSpecificForRegion(
      phoneUtil.parse('33669', RegionCode.US), RegionCode.US));

  var notCarrierSpecificNumber = new i18n.phonenumbers.PhoneNumber();
  notCarrierSpecificNumber.setCountryCode(1);
  notCarrierSpecificNumber.setNationalNumber(911);
  assertFalse(shortInfo.isCarrierSpecific(notCarrierSpecificNumber));
  assertFalse(shortInfo.isCarrierSpecificForRegion(
      phoneUtil.parse('911', RegionCode.US), RegionCode.US));

  var carrierSpecificNumberForSomeRegion = new i18n.phonenumbers.PhoneNumber();
  carrierSpecificNumberForSomeRegion.setCountryCode(1);
  carrierSpecificNumberForSomeRegion.setNationalNumber(211);
  assertTrue(shortInfo.isCarrierSpecific(carrierSpecificNumberForSomeRegion));
  assertTrue(shortInfo.isCarrierSpecificForRegion(
      carrierSpecificNumberForSomeRegion, RegionCode.US));
  assertFalse(shortInfo.isCarrierSpecificForRegion(
      carrierSpecificNumberForSomeRegion, RegionCode.BB));
}

function testIsSmsService() {
  var smsServiceNumberForSomeRegion = new i18n.phonenumbers.PhoneNumber();
  smsServiceNumberForSomeRegion.setCountryCode(1);
  smsServiceNumberForSomeRegion.setNationalNumber(21234);
  assertTrue(shortInfo.isSmsServiceForRegion(
      smsServiceNumberForSomeRegion, RegionCode.US));
  assertFalse(shortInfo.isSmsServiceForRegion(
      smsServiceNumberForSomeRegion, RegionCode.BB));
}

function testGetExpectedCost() {
  var premiumRateExample = shortInfo.getExampleShortNumberForCost(
      RegionCode.FR,
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.PREMIUM_RATE);
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(premiumRateExample, RegionCode.FR), RegionCode.FR));
  var premiumRateNumber = new i18n.phonenumbers.PhoneNumber();
  premiumRateNumber.setCountryCode(33);
  premiumRateNumber.setNationalNumber(parseInt(premiumRateExample, 10));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
      shortInfo.getExpectedCost(premiumRateNumber));

  var standardRateExample = shortInfo.getExampleShortNumberForCost(
      RegionCode.FR,
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.STANDARD_RATE);
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.STANDARD_RATE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(standardRateExample, RegionCode.FR), RegionCode.FR));
  var standardRateNumber = new i18n.phonenumbers.PhoneNumber();
  standardRateNumber.setCountryCode(33);
  standardRateNumber.setNationalNumber(parseInt(standardRateExample, 10));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.STANDARD_RATE,
      shortInfo.getExpectedCost(standardRateNumber));

  var tollFreeExample = shortInfo.getExampleShortNumberForCost(
      RegionCode.FR,
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE);
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(tollFreeExample, RegionCode.FR), RegionCode.FR));
  var tollFreeNumber = new i18n.phonenumbers.PhoneNumber();
  tollFreeNumber.setCountryCode(33);
  tollFreeNumber.setNationalNumber(parseInt(tollFreeExample, 10));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCost(tollFreeNumber));

  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('12345', RegionCode.FR), RegionCode.FR));
  var unknownCostNumber = new i18n.phonenumbers.PhoneNumber();
  unknownCostNumber.setCountryCode(33);
  unknownCostNumber.setNationalNumber(12345);
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCost(unknownCostNumber));

  // Test that an invalid number may nevertheless have a cost other than
  // UNKNOWN_COST.
  assertFalse(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse('116123', RegionCode.FR), RegionCode.FR));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('116123', RegionCode.FR), RegionCode.FR));
  var invalidNumber = new i18n.phonenumbers.PhoneNumber();
  invalidNumber.setCountryCode(33);
  invalidNumber.setNationalNumber(116123);
  assertFalse(shortInfo.isValidShortNumber(invalidNumber));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCost(invalidNumber));

  // Test a nonexistent country code.
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('911', RegionCode.US), RegionCode.ZZ));
  unknownCostNumber = new i18n.phonenumbers.PhoneNumber();
  unknownCostNumber.setCountryCode(123);
  unknownCostNumber.setNationalNumber(911);
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCost(unknownCostNumber));
}

function testGetExpectedCostForSharedCountryCallingCode() {
  // Test some numbers which have different costs in countries sharing the same
  // country calling code. In Australia, 1234 is premium-rate, 1194 is
  // standard-rate, and 733 is toll-free. These are not known to be valid
  // numbers in the Christmas Islands.
  var ambiguousPremiumRateString = '1234';
  var ambiguousPremiumRateNumber = new i18n.phonenumbers.PhoneNumber();
  ambiguousPremiumRateNumber.setCountryCode(61);
  ambiguousPremiumRateNumber.setNationalNumber(1234);
  var ambiguousStandardRateString = '1194';
  var ambiguousStandardRateNumber = new i18n.phonenumbers.PhoneNumber();
  ambiguousStandardRateNumber.setCountryCode(61);
  ambiguousStandardRateNumber.setNationalNumber(1194);
  var ambiguousTollFreeString = '733';
  var ambiguousTollFreeNumber = new i18n.phonenumbers.PhoneNumber();
  ambiguousTollFreeNumber.setCountryCode(61);
  ambiguousTollFreeNumber.setNationalNumber(733);
  assertTrue(shortInfo.isValidShortNumber(ambiguousPremiumRateNumber));
  assertTrue(shortInfo.isValidShortNumber(ambiguousStandardRateNumber));
  assertTrue(shortInfo.isValidShortNumber(ambiguousTollFreeNumber));
  assertTrue(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse(ambiguousPremiumRateString, RegionCode.AU),
      RegionCode.AU));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(ambiguousPremiumRateString, RegionCode.AU),
          RegionCode.AU));
  assertFalse(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse(ambiguousPremiumRateString, RegionCode.CX),
      RegionCode.CX));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(ambiguousPremiumRateString, RegionCode.CX),
          RegionCode.CX));
  // PREMIUM_RATE takes precedence over UNKNOWN_COST.
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
      shortInfo.getExpectedCost(ambiguousPremiumRateNumber));
  assertTrue(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse(ambiguousStandardRateString, RegionCode.AU),
      RegionCode.AU));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.STANDARD_RATE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(ambiguousStandardRateString, RegionCode.AU),
          RegionCode.AU));
  assertFalse(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse(ambiguousStandardRateString, RegionCode.CX),
      RegionCode.CX));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(ambiguousStandardRateString, RegionCode.CX),
          RegionCode.CX));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCost(ambiguousStandardRateNumber));
  assertTrue(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse(ambiguousTollFreeString, RegionCode.AU), RegionCode.AU));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(ambiguousTollFreeString, RegionCode.AU),
          RegionCode.AU));
  assertFalse(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse(ambiguousTollFreeString, RegionCode.CX), RegionCode.CX));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse(ambiguousTollFreeString, RegionCode.CX),
          RegionCode.CX));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCost(ambiguousTollFreeNumber));
}

function testExampleShortNumberPresence() {
  assertNonEmptyString(shortInfo.getExampleShortNumber(RegionCode.AD));
  assertNonEmptyString(shortInfo.getExampleShortNumber(RegionCode.FR));
  assertEquals('', shortInfo.getExampleShortNumber(RegionCode.UN001));
  assertEquals('', shortInfo.getExampleShortNumber(null));
}

function testConnectsToEmergencyNumber_US() {
  assertTrue(shortInfo.connectsToEmergencyNumber('911', RegionCode.US));
  assertTrue(shortInfo.connectsToEmergencyNumber('112', RegionCode.US));
  assertFalse(shortInfo.connectsToEmergencyNumber('999', RegionCode.US));
}

function testConnectsToEmergencyNumberLongNumber_US() {
  assertTrue(shortInfo.connectsToEmergencyNumber('9116666666', RegionCode.US));
  assertTrue(shortInfo.connectsToEmergencyNumber('1126666666', RegionCode.US));
  assertFalse(shortInfo.connectsToEmergencyNumber('9996666666', RegionCode.US));
}

function testConnectsToEmergencyNumberWithFormatting_US() {
  assertTrue(shortInfo.connectsToEmergencyNumber('9-1-1', RegionCode.US));
  assertTrue(shortInfo.connectsToEmergencyNumber('1-1-2', RegionCode.US));
  assertFalse(shortInfo.connectsToEmergencyNumber('9-9-9', RegionCode.US));
}

function testConnectsToEmergencyNumberWithPlusSign_US() {
  assertFalse(shortInfo.connectsToEmergencyNumber('+911', RegionCode.US));
  assertFalse(shortInfo.connectsToEmergencyNumber('\uFF0B911', RegionCode.US));
  assertFalse(shortInfo.connectsToEmergencyNumber(' +911', RegionCode.US));
  assertFalse(shortInfo.connectsToEmergencyNumber('+112', RegionCode.US));
  assertFalse(shortInfo.connectsToEmergencyNumber('+999', RegionCode.US));
}

function testConnectsToEmergencyNumber_BR() {
  assertTrue(shortInfo.connectsToEmergencyNumber('911', RegionCode.BR));
  assertTrue(shortInfo.connectsToEmergencyNumber('190', RegionCode.BR));
  assertFalse(shortInfo.connectsToEmergencyNumber('999', RegionCode.BR));
}

function testConnectsToEmergencyNumberLongNumber_BR() {
  // Brazilian emergency numbers don't work when additional digits are appended.
  assertFalse(shortInfo.connectsToEmergencyNumber('9111', RegionCode.BR));
  assertFalse(shortInfo.connectsToEmergencyNumber('1900', RegionCode.BR));
  assertFalse(shortInfo.connectsToEmergencyNumber('9996', RegionCode.BR));
}

function testConnectsToEmergencyNumber_CL() {
  assertTrue(shortInfo.connectsToEmergencyNumber('131', RegionCode.CL));
  assertTrue(shortInfo.connectsToEmergencyNumber('133', RegionCode.CL));
}

function testConnectsToEmergencyNumberLongNumber_CL() {
  // Chilean emergency numbers don't work when additional digits are appended.
  assertFalse(shortInfo.connectsToEmergencyNumber('1313', RegionCode.CL));
  assertFalse(shortInfo.connectsToEmergencyNumber('1330', RegionCode.CL));
}

function testConnectsToEmergencyNumber_AO() {
  // Angola doesn't have any metadata for emergency numbers in the test
  // metadata.
  assertFalse(shortInfo.connectsToEmergencyNumber('911', RegionCode.AO));
  assertFalse(shortInfo.connectsToEmergencyNumber('222123456', RegionCode.AO));
  assertFalse(shortInfo.connectsToEmergencyNumber('923123456', RegionCode.AO));
}

function testConnectsToEmergencyNumber_ZW() {
  // Zimbabwe doesn't have any metadata in the test metadata.
  assertFalse(shortInfo.connectsToEmergencyNumber('911', RegionCode.ZW));
  assertFalse(shortInfo.connectsToEmergencyNumber('01312345', RegionCode.ZW));
  assertFalse(shortInfo.connectsToEmergencyNumber('0711234567', RegionCode.ZW));
}

function testIsEmergencyNumber_US() {
  assertTrue(shortInfo.isEmergencyNumber('911', RegionCode.US));
  assertTrue(shortInfo.isEmergencyNumber('112', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('999', RegionCode.US));
}

function testIsEmergencyNumberLongNumber_US() {
  assertFalse(shortInfo.isEmergencyNumber('9116666666', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('1126666666', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('9996666666', RegionCode.US));
}

function testIsEmergencyNumberWithFormatting_US() {
  assertTrue(shortInfo.isEmergencyNumber('9-1-1', RegionCode.US));
  assertTrue(shortInfo.isEmergencyNumber('*911', RegionCode.US));
  assertTrue(shortInfo.isEmergencyNumber('1-1-2', RegionCode.US));
  assertTrue(shortInfo.isEmergencyNumber('*112', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('9-9-9', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('*999', RegionCode.US));
}

function testIsEmergencyNumberWithPlusSign_US() {
  assertFalse(shortInfo.isEmergencyNumber('+911', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('\uFF0B911', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber(' +911', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('+112', RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('+999', RegionCode.US));
}

function testIsEmergencyNumber_BR() {
  assertTrue(shortInfo.isEmergencyNumber('911', RegionCode.BR));
  assertTrue(shortInfo.isEmergencyNumber('190', RegionCode.BR));
  assertFalse(shortInfo.isEmergencyNumber('999', RegionCode.BR));
}

function testIsEmergencyNumberLongNumber_BR() {
  assertFalse(shortInfo.isEmergencyNumber('9111', RegionCode.BR));
  assertFalse(shortInfo.isEmergencyNumber('1900', RegionCode.BR));
  assertFalse(shortInfo.isEmergencyNumber('9996', RegionCode.BR));
}

function testIsEmergencyNumber_AO() {
  // Angola doesn't have any metadata for emergency numbers in the test
  // metadata.
  assertFalse(shortInfo.isEmergencyNumber('911', RegionCode.AO));
  assertFalse(shortInfo.isEmergencyNumber('222123456', RegionCode.AO));
  assertFalse(shortInfo.isEmergencyNumber('923123456', RegionCode.AO));
}

function testIsEmergencyNumber_ZW() {
  // Zimbabwe doesn't have any metadata in the test metadata.
  assertFalse(shortInfo.isEmergencyNumber('911', RegionCode.ZW));
  assertFalse(shortInfo.isEmergencyNumber('01312345', RegionCode.ZW));
  assertFalse(shortInfo.isEmergencyNumber('0711234567', RegionCode.ZW));
}

function testEmergencyNumberForSharedCountryCallingCode() {
  // Test the emergency number 112, which is valid in both Australia and the
  // Christmas Islands.
  assertTrue(shortInfo.isEmergencyNumber('112', RegionCode.AU));
  assertTrue(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse('112', RegionCode.AU), RegionCode.AU));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('112', RegionCode.AU), RegionCode.AU));
  assertTrue(shortInfo.isEmergencyNumber('112', RegionCode.CX));
  assertTrue(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse('112', RegionCode.CX), RegionCode.CX));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('112', RegionCode.CX), RegionCode.CX));
  var sharedEmergencyNumber = new i18n.phonenumbers.PhoneNumber();
  sharedEmergencyNumber.setCountryCode(61);
  sharedEmergencyNumber.setNationalNumber(112);
  assertTrue(shortInfo.isValidShortNumber(sharedEmergencyNumber));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCost(sharedEmergencyNumber));
}

function testOverlappingNANPANumber() {
  // 211 is an emergency number in Barbados, while it is a toll-free information
  // line in Canada and the USA.
  assertTrue(shortInfo.isEmergencyNumber('211', RegionCode.BB));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('211', RegionCode.BB), RegionCode.BB));
  assertFalse(shortInfo.isEmergencyNumber('211', RegionCode.US));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('211', RegionCode.US), RegionCode.US));
  assertFalse(shortInfo.isEmergencyNumber('211', RegionCode.CA));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.TOLL_FREE,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('211', RegionCode.CA), RegionCode.CA));
}

function testCountryCallingCodeIsNotIgnored() {
  // +46 is the country calling code for Sweden (SE), and 40404 is a valid short
  // number in the US.
  assertFalse(shortInfo.isPossibleShortNumberForRegion(
      phoneUtil.parse('+4640404', RegionCode.SE), RegionCode.US));
  assertFalse(shortInfo.isValidShortNumberForRegion(
      phoneUtil.parse('+4640404', RegionCode.SE), RegionCode.US));
  assertEquals(
      i18n.phonenumbers.ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
      shortInfo.getExpectedCostForRegion(
          phoneUtil.parse('+4640404', RegionCode.SE), RegionCode.US));
}
