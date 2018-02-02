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
 * @fileoverview  Unit tests for the ShortNumberInfo.
 *
 * Note that these tests use the test metadata for PhoneNumberUtil related
 * operations, but the real short number metadata for testing ShortNumberInfo
 * specific operations. This is not intentional, but mirrors the current state
 * of the Java test suite.
 *
 * @author James Wright
 */

goog.require('goog.array');
goog.require('goog.string.StringBuffer');
goog.require('goog.testing.jsunit');
goog.require('i18n.phonenumbers.NumberFormat');
goog.require('i18n.phonenumbers.PhoneMetadata');
goog.require('i18n.phonenumbers.PhoneNumber');
goog.require('i18n.phonenumbers.PhoneNumberDesc');
goog.require('i18n.phonenumbers.RegionCode');
goog.require('i18n.phonenumbers.ShortNumberInfo');


/** @type {i18n.phonenumbers.PhoneNumberUtil} */
var shortInfo = i18n.phonenumbers.ShortNumberInfo.getInstance();

var RegionCode = i18n.phonenumbers.RegionCode;

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
