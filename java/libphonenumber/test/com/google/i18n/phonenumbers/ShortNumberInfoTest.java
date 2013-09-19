/*
 * Copyright (C) 2013 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Unit tests for ShortNumberInfo.java
 *
 * @author Shaopeng Jia
 */
public class ShortNumberInfoTest extends TestMetadataTestCase {
  private ShortNumberInfo shortInfo;

  public ShortNumberInfoTest() {
    shortInfo = new ShortNumberInfo(phoneUtil);
  }

  public void testIsPossibleShortNumber() {
    PhoneNumber possibleNumber = new PhoneNumber();
    possibleNumber.setCountryCode(33).setNationalNumber(123456L);
    assertTrue(shortInfo.isPossibleShortNumber(possibleNumber));
    assertTrue(shortInfo.isPossibleShortNumber("123456", RegionCode.FR));

    PhoneNumber impossibleNumber = new PhoneNumber();
    impossibleNumber.setCountryCode(33).setNationalNumber(9L);
    assertFalse(shortInfo.isPossibleShortNumber(impossibleNumber));
    assertFalse(shortInfo.isPossibleShortNumber("9", RegionCode.FR));
  }

  public void testIsValidShortNumber() {
    assertTrue(shortInfo.isValidShortNumber(
        new PhoneNumber().setCountryCode(33).setNationalNumber(1010L)));
    assertTrue(shortInfo.isValidShortNumber("1010", RegionCode.FR));
    assertFalse(shortInfo.isValidShortNumber(
        new PhoneNumber().setCountryCode(33).setNationalNumber(123456L)));
    assertFalse(shortInfo.isValidShortNumber("123456", RegionCode.FR));

    // Note that GB and GG share the country calling code 44.
    assertTrue(shortInfo.isValidShortNumber(
        new PhoneNumber().setCountryCode(44).setNationalNumber(18001L)));
  }

  public void testGetExpectedCost() {
    PhoneNumber premiumRateNumber = new PhoneNumber();
    premiumRateNumber.setCountryCode(33).setNationalNumber(
        Integer.parseInt(shortInfo.getExampleShortNumberForCost(
            RegionCode.FR, ShortNumberInfo.ShortNumberCost.PREMIUM_RATE)));
    assertEquals(ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
        shortInfo.getExpectedCost(premiumRateNumber));

    PhoneNumber standardRateNumber = new PhoneNumber();
    standardRateNumber.setCountryCode(33).setNationalNumber(
        Integer.parseInt(shortInfo.getExampleShortNumberForCost(
            RegionCode.FR, ShortNumberInfo.ShortNumberCost.STANDARD_RATE)));
    assertEquals(ShortNumberInfo.ShortNumberCost.STANDARD_RATE,
        shortInfo.getExpectedCost(standardRateNumber));

    PhoneNumber tollFreeNumber = new PhoneNumber();
    tollFreeNumber.setCountryCode(33).setNationalNumber(
        Integer.parseInt(shortInfo.getExampleShortNumberForCost(
            RegionCode.FR, ShortNumberInfo.ShortNumberCost.TOLL_FREE)));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCost(tollFreeNumber));

    PhoneNumber unknownCostNumber = new PhoneNumber();
    unknownCostNumber.setCountryCode(33).setNationalNumber(12345L);
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCost(unknownCostNumber));

    // Test that an invalid number may nevertheless have a cost other than UNKNOWN_COST.
    PhoneNumber invalidNumber = new PhoneNumber();
    invalidNumber.setCountryCode(33).setNationalNumber(116123L);
    assertFalse(shortInfo.isValidShortNumber(invalidNumber));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCost(invalidNumber));

    // Test a non-existent country code.
    unknownCostNumber.clear();
    unknownCostNumber.setCountryCode(123).setNationalNumber(911L);
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCost(unknownCostNumber));
  }

  public void testGetExampleShortNumber() {
    assertEquals("8711", shortInfo.getExampleShortNumber(RegionCode.AM));
    assertEquals("1010", shortInfo.getExampleShortNumber(RegionCode.FR));
    assertEquals("", shortInfo.getExampleShortNumber(RegionCode.UN001));
    assertEquals("", shortInfo.getExampleShortNumber(null));
  }

  public void testGetExampleShortNumberForCost() {
    assertEquals("3010", shortInfo.getExampleShortNumberForCost(RegionCode.FR,
        ShortNumberInfo.ShortNumberCost.TOLL_FREE));
    assertEquals("1023", shortInfo.getExampleShortNumberForCost(RegionCode.FR,
        ShortNumberInfo.ShortNumberCost.STANDARD_RATE));
    assertEquals("42000", shortInfo.getExampleShortNumberForCost(RegionCode.FR,
        ShortNumberInfo.ShortNumberCost.PREMIUM_RATE));
    assertEquals("", shortInfo.getExampleShortNumberForCost(RegionCode.FR,
        ShortNumberInfo.ShortNumberCost.UNKNOWN_COST));
  }

  public void testConnectsToEmergencyNumber_US() {
    assertTrue(shortInfo.connectsToEmergencyNumber("911", RegionCode.US));
    assertTrue(shortInfo.connectsToEmergencyNumber("112", RegionCode.US));
    assertFalse(shortInfo.connectsToEmergencyNumber("999", RegionCode.US));
  }

  public void testConnectsToEmergencyNumberLongNumber_US() {
    assertTrue(shortInfo.connectsToEmergencyNumber("9116666666", RegionCode.US));
    assertTrue(shortInfo.connectsToEmergencyNumber("1126666666", RegionCode.US));
    assertFalse(shortInfo.connectsToEmergencyNumber("9996666666", RegionCode.US));
  }

  public void testConnectsToEmergencyNumberWithFormatting_US() {
    assertTrue(shortInfo.connectsToEmergencyNumber("9-1-1", RegionCode.US));
    assertTrue(shortInfo.connectsToEmergencyNumber("1-1-2", RegionCode.US));
    assertFalse(shortInfo.connectsToEmergencyNumber("9-9-9", RegionCode.US));
  }

  public void testConnectsToEmergencyNumberWithPlusSign_US() {
    assertFalse(shortInfo.connectsToEmergencyNumber("+911", RegionCode.US));
    assertFalse(shortInfo.connectsToEmergencyNumber("\uFF0B911", RegionCode.US));
    assertFalse(shortInfo.connectsToEmergencyNumber(" +911", RegionCode.US));
    assertFalse(shortInfo.connectsToEmergencyNumber("+112", RegionCode.US));
    assertFalse(shortInfo.connectsToEmergencyNumber("+999", RegionCode.US));
  }

  public void testConnectsToEmergencyNumber_BR() {
    assertTrue(shortInfo.connectsToEmergencyNumber("911", RegionCode.BR));
    assertTrue(shortInfo.connectsToEmergencyNumber("190", RegionCode.BR));
    assertFalse(shortInfo.connectsToEmergencyNumber("999", RegionCode.BR));
  }

  public void testConnectsToEmergencyNumberLongNumber_BR() {
    // Brazilian emergency numbers don't work when additional digits are appended.
    assertFalse(shortInfo.connectsToEmergencyNumber("9111", RegionCode.BR));
    assertFalse(shortInfo.connectsToEmergencyNumber("1900", RegionCode.BR));
    assertFalse(shortInfo.connectsToEmergencyNumber("9996", RegionCode.BR));
  }

  public void testConnectsToEmergencyNumber_CL() {
    assertTrue(shortInfo.connectsToEmergencyNumber("131", RegionCode.CL));
    assertTrue(shortInfo.connectsToEmergencyNumber("133", RegionCode.CL));
  }

  public void testConnectsToEmergencyNumberLongNumber_CL() {
    // Chilean emergency numbers don't work when additional digits are appended.
    assertFalse(shortInfo.connectsToEmergencyNumber("1313", RegionCode.CL));
    assertFalse(shortInfo.connectsToEmergencyNumber("1330", RegionCode.CL));
  }

  public void testConnectsToEmergencyNumber_AO() {
    // Angola doesn't have any metadata for emergency numbers in the test metadata.
    assertFalse(shortInfo.connectsToEmergencyNumber("911", RegionCode.AO));
    assertFalse(shortInfo.connectsToEmergencyNumber("222123456", RegionCode.AO));
    assertFalse(shortInfo.connectsToEmergencyNumber("923123456", RegionCode.AO));
  }

  public void testConnectsToEmergencyNumber_ZW() {
    // Zimbabwe doesn't have any metadata in the test metadata.
    assertFalse(shortInfo.connectsToEmergencyNumber("911", RegionCode.ZW));
    assertFalse(shortInfo.connectsToEmergencyNumber("01312345", RegionCode.ZW));
    assertFalse(shortInfo.connectsToEmergencyNumber("0711234567", RegionCode.ZW));
  }

  public void testIsEmergencyNumber_US() {
    assertTrue(shortInfo.isEmergencyNumber("911", RegionCode.US));
    assertTrue(shortInfo.isEmergencyNumber("112", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("999", RegionCode.US));
  }

  public void testIsEmergencyNumberLongNumber_US() {
    assertFalse(shortInfo.isEmergencyNumber("9116666666", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("1126666666", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("9996666666", RegionCode.US));
  }

  public void testIsEmergencyNumberWithFormatting_US() {
    assertTrue(shortInfo.isEmergencyNumber("9-1-1", RegionCode.US));
    assertTrue(shortInfo.isEmergencyNumber("*911", RegionCode.US));
    assertTrue(shortInfo.isEmergencyNumber("1-1-2", RegionCode.US));
    assertTrue(shortInfo.isEmergencyNumber("*112", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("9-9-9", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("*999", RegionCode.US));
  }

  public void testIsEmergencyNumberWithPlusSign_US() {
    assertFalse(shortInfo.isEmergencyNumber("+911", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("\uFF0B911", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber(" +911", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("+112", RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("+999", RegionCode.US));
  }

  public void testIsEmergencyNumber_BR() {
    assertTrue(shortInfo.isEmergencyNumber("911", RegionCode.BR));
    assertTrue(shortInfo.isEmergencyNumber("190", RegionCode.BR));
    assertFalse(shortInfo.isEmergencyNumber("999", RegionCode.BR));
  }

  public void testIsEmergencyNumberLongNumber_BR() {
    assertFalse(shortInfo.isEmergencyNumber("9111", RegionCode.BR));
    assertFalse(shortInfo.isEmergencyNumber("1900", RegionCode.BR));
    assertFalse(shortInfo.isEmergencyNumber("9996", RegionCode.BR));
  }

  public void testIsEmergencyNumber_AO() {
    // Angola doesn't have any metadata for emergency numbers in the test metadata.
    assertFalse(shortInfo.isEmergencyNumber("911", RegionCode.AO));
    assertFalse(shortInfo.isEmergencyNumber("222123456", RegionCode.AO));
    assertFalse(shortInfo.isEmergencyNumber("923123456", RegionCode.AO));
  }

  public void testIsEmergencyNumber_ZW() {
    // Zimbabwe doesn't have any metadata in the test metadata.
    assertFalse(shortInfo.isEmergencyNumber("911", RegionCode.ZW));
    assertFalse(shortInfo.isEmergencyNumber("01312345", RegionCode.ZW));
    assertFalse(shortInfo.isEmergencyNumber("0711234567", RegionCode.ZW));
  }
}
