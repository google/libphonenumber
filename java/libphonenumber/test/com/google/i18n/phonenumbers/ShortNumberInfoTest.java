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
  private static final ShortNumberInfo shortInfo = ShortNumberInfo.getInstance();

  public void testIsPossibleShortNumber() {
    PhoneNumber possibleNumber = new PhoneNumber();
    possibleNumber.setCountryCode(33).setNationalNumber(123456L);
    assertTrue(shortInfo.isPossibleShortNumber(possibleNumber));
    assertTrue(
        shortInfo.isPossibleShortNumberForRegion(parse("123456", RegionCode.FR), RegionCode.FR));

    PhoneNumber impossibleNumber = new PhoneNumber();
    impossibleNumber.setCountryCode(33).setNationalNumber(9L);
    assertFalse(shortInfo.isPossibleShortNumber(impossibleNumber));

    // Note that GB and GG share the country calling code 44, and that this number is possible but
    // not valid.
    assertTrue(shortInfo.isPossibleShortNumber(
        new PhoneNumber().setCountryCode(44).setNationalNumber(11001L)));
  }

  public void testIsValidShortNumber() {
    assertTrue(shortInfo.isValidShortNumber(
        new PhoneNumber().setCountryCode(33).setNationalNumber(1010L)));
    assertTrue(shortInfo.isValidShortNumberForRegion(parse("1010", RegionCode.FR), RegionCode.FR));
    assertFalse(shortInfo.isValidShortNumber(
        new PhoneNumber().setCountryCode(33).setNationalNumber(123456L)));
    assertFalse(
        shortInfo.isValidShortNumberForRegion(parse("123456", RegionCode.FR), RegionCode.FR));

    // Note that GB and GG share the country calling code 44.
    assertTrue(shortInfo.isValidShortNumber(
        new PhoneNumber().setCountryCode(44).setNationalNumber(18001L)));
  }

  public void testIsCarrierSpecific() {
    PhoneNumber carrierSpecificNumber = new PhoneNumber();
    carrierSpecificNumber.setCountryCode(1).setNationalNumber(33669L);
    assertTrue(shortInfo.isCarrierSpecific(carrierSpecificNumber));
    assertTrue(
        shortInfo.isCarrierSpecificForRegion(parse("33669", RegionCode.US), RegionCode.US));

    PhoneNumber notCarrierSpecificNumber = new PhoneNumber();
    notCarrierSpecificNumber.setCountryCode(1).setNationalNumber(911L);
    assertFalse(shortInfo.isCarrierSpecific(notCarrierSpecificNumber));
    assertFalse(
        shortInfo.isCarrierSpecificForRegion(parse("911", RegionCode.US), RegionCode.US));

    PhoneNumber carrierSpecificNumberForSomeRegion = new PhoneNumber();
    carrierSpecificNumberForSomeRegion.setCountryCode(1).setNationalNumber(211L);
    assertTrue(shortInfo.isCarrierSpecific(carrierSpecificNumberForSomeRegion));
    assertTrue(
        shortInfo.isCarrierSpecificForRegion(carrierSpecificNumberForSomeRegion, RegionCode.US));
    assertFalse(
        shortInfo.isCarrierSpecificForRegion(carrierSpecificNumberForSomeRegion, RegionCode.BB));
  }

  public void testIsSmsService() {
    PhoneNumber smsServiceNumberForSomeRegion = new PhoneNumber();
    smsServiceNumberForSomeRegion.setCountryCode(1).setNationalNumber(21234L);
    assertTrue(shortInfo.isSmsServiceForRegion(smsServiceNumberForSomeRegion, RegionCode.US));
    assertFalse(shortInfo.isSmsServiceForRegion(smsServiceNumberForSomeRegion, RegionCode.BB));
  }

  public void testGetExpectedCost() {
    String premiumRateExample = shortInfo.getExampleShortNumberForCost(RegionCode.FR,
        ShortNumberInfo.ShortNumberCost.PREMIUM_RATE);
    assertEquals(ShortNumberInfo.ShortNumberCost.PREMIUM_RATE, shortInfo.getExpectedCostForRegion(
        parse(premiumRateExample, RegionCode.FR), RegionCode.FR));
    PhoneNumber premiumRateNumber = new PhoneNumber();
    premiumRateNumber.setCountryCode(33).setNationalNumber(Integer.parseInt(premiumRateExample));
    assertEquals(ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
        shortInfo.getExpectedCost(premiumRateNumber));

    String standardRateExample = shortInfo.getExampleShortNumberForCost(RegionCode.FR,
        ShortNumberInfo.ShortNumberCost.STANDARD_RATE);
    assertEquals(ShortNumberInfo.ShortNumberCost.STANDARD_RATE, shortInfo.getExpectedCostForRegion(
        parse(standardRateExample, RegionCode.FR), RegionCode.FR));
    PhoneNumber standardRateNumber = new PhoneNumber();
    standardRateNumber.setCountryCode(33).setNationalNumber(Integer.parseInt(standardRateExample));
    assertEquals(ShortNumberInfo.ShortNumberCost.STANDARD_RATE,
        shortInfo.getExpectedCost(standardRateNumber));

    String tollFreeExample = shortInfo.getExampleShortNumberForCost(RegionCode.FR,
        ShortNumberInfo.ShortNumberCost.TOLL_FREE);
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCostForRegion(parse(tollFreeExample, RegionCode.FR), RegionCode.FR));
    PhoneNumber tollFreeNumber = new PhoneNumber();
    tollFreeNumber.setCountryCode(33).setNationalNumber(Integer.parseInt(tollFreeExample));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCost(tollFreeNumber));

    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCostForRegion(parse("12345", RegionCode.FR), RegionCode.FR));
    PhoneNumber unknownCostNumber = new PhoneNumber();
    unknownCostNumber.setCountryCode(33).setNationalNumber(12345L);
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCost(unknownCostNumber));

    // Test that an invalid number may nevertheless have a cost other than UNKNOWN_COST.
    assertFalse(
        shortInfo.isValidShortNumberForRegion(parse("116123", RegionCode.FR), RegionCode.FR));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCostForRegion(parse("116123", RegionCode.FR), RegionCode.FR));
    PhoneNumber invalidNumber = new PhoneNumber();
    invalidNumber.setCountryCode(33).setNationalNumber(116123L);
    assertFalse(shortInfo.isValidShortNumber(invalidNumber));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCost(invalidNumber));

    // Test a nonexistent country code.
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCostForRegion(parse("911", RegionCode.US), RegionCode.ZZ));
    unknownCostNumber.clear();
    unknownCostNumber.setCountryCode(123).setNationalNumber(911L);
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCost(unknownCostNumber));
  }

  public void testGetExpectedCostForSharedCountryCallingCode() {
    // Test some numbers which have different costs in countries sharing the same country calling
    // code. In Australia, 1234 is premium-rate, 1194 is standard-rate, and 733 is toll-free. These
    // are not known to be valid numbers in the Christmas Islands.
    String ambiguousPremiumRateString = "1234";
    PhoneNumber ambiguousPremiumRateNumber =
        new PhoneNumber().setCountryCode(61).setNationalNumber(1234L);
    String ambiguousStandardRateString = "1194";
    PhoneNumber ambiguousStandardRateNumber =
        new PhoneNumber().setCountryCode(61).setNationalNumber(1194L);
    String ambiguousTollFreeString = "733";
    PhoneNumber ambiguousTollFreeNumber =
        new PhoneNumber().setCountryCode(61).setNationalNumber(733L);

    assertTrue(shortInfo.isValidShortNumber(ambiguousPremiumRateNumber));
    assertTrue(shortInfo.isValidShortNumber(ambiguousStandardRateNumber));
    assertTrue(shortInfo.isValidShortNumber(ambiguousTollFreeNumber));

    assertTrue(shortInfo.isValidShortNumberForRegion(
        parse(ambiguousPremiumRateString, RegionCode.AU), RegionCode.AU));
    assertEquals(ShortNumberInfo.ShortNumberCost.PREMIUM_RATE, shortInfo.getExpectedCostForRegion(
        parse(ambiguousPremiumRateString, RegionCode.AU), RegionCode.AU));
    assertFalse(shortInfo.isValidShortNumberForRegion(
        parse(ambiguousPremiumRateString, RegionCode.CX), RegionCode.CX));
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
        parse(ambiguousPremiumRateString, RegionCode.CX), RegionCode.CX));
    // PREMIUM_RATE takes precedence over UNKNOWN_COST.
    assertEquals(ShortNumberInfo.ShortNumberCost.PREMIUM_RATE,
        shortInfo.getExpectedCost(ambiguousPremiumRateNumber));

    assertTrue(shortInfo.isValidShortNumberForRegion(
        parse(ambiguousStandardRateString, RegionCode.AU), RegionCode.AU));
    assertEquals(ShortNumberInfo.ShortNumberCost.STANDARD_RATE, shortInfo.getExpectedCostForRegion(
        parse(ambiguousStandardRateString, RegionCode.AU), RegionCode.AU));
    assertFalse(shortInfo.isValidShortNumberForRegion(
        parse(ambiguousStandardRateString, RegionCode.CX), RegionCode.CX));
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
        parse(ambiguousStandardRateString, RegionCode.CX), RegionCode.CX));
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCost(ambiguousStandardRateNumber));

    assertTrue(shortInfo.isValidShortNumberForRegion(parse(ambiguousTollFreeString, RegionCode.AU),
        RegionCode.AU));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE, shortInfo.getExpectedCostForRegion(
        parse(ambiguousTollFreeString, RegionCode.AU), RegionCode.AU));
    assertFalse(shortInfo.isValidShortNumberForRegion(parse(ambiguousTollFreeString, RegionCode.CX),
        RegionCode.CX));
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST, shortInfo.getExpectedCostForRegion(
        parse(ambiguousTollFreeString, RegionCode.CX), RegionCode.CX));
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCost(ambiguousTollFreeNumber));
  }

  public void testExampleShortNumberPresence() {
    assertFalse(shortInfo.getExampleShortNumber(RegionCode.AD).isEmpty());
    assertFalse(shortInfo.getExampleShortNumber(RegionCode.FR).isEmpty());
    assertTrue(shortInfo.getExampleShortNumber(RegionCode.UN001).isEmpty());
    assertTrue(shortInfo.getExampleShortNumber(null).isEmpty());
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

  public void testEmergencyNumberForSharedCountryCallingCode() {
    // Test the emergency number 112, which is valid in both Australia and the Christmas Islands.
    assertTrue(shortInfo.isEmergencyNumber("112", RegionCode.AU));
    assertTrue(shortInfo.isValidShortNumberForRegion(parse("112", RegionCode.AU), RegionCode.AU));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCostForRegion(parse("112", RegionCode.AU), RegionCode.AU));
    assertTrue(shortInfo.isEmergencyNumber("112", RegionCode.CX));
    assertTrue(shortInfo.isValidShortNumberForRegion(parse("112", RegionCode.CX), RegionCode.CX));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCostForRegion(parse("112", RegionCode.CX), RegionCode.CX));
    PhoneNumber sharedEmergencyNumber =
        new PhoneNumber().setCountryCode(61).setNationalNumber(112L);
    assertTrue(shortInfo.isValidShortNumber(sharedEmergencyNumber));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCost(sharedEmergencyNumber));
  }

  public void testOverlappingNANPANumber() {
    // 211 is an emergency number in Barbados, while it is a toll-free information line in Canada
    // and the USA.
    assertTrue(shortInfo.isEmergencyNumber("211", RegionCode.BB));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCostForRegion(parse("211", RegionCode.BB), RegionCode.BB));
    assertFalse(shortInfo.isEmergencyNumber("211", RegionCode.US));
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCostForRegion(parse("211", RegionCode.US), RegionCode.US));
    assertFalse(shortInfo.isEmergencyNumber("211", RegionCode.CA));
    assertEquals(ShortNumberInfo.ShortNumberCost.TOLL_FREE,
        shortInfo.getExpectedCostForRegion(parse("211", RegionCode.CA), RegionCode.CA));
  }

  public void testCountryCallingCodeIsNotIgnored() {
    // +46 is the country calling code for Sweden (SE), and 40404 is a valid short number in the US.
    assertFalse(shortInfo.isPossibleShortNumberForRegion(
        parse("+4640404", RegionCode.SE), RegionCode.US));
    assertFalse(shortInfo.isValidShortNumberForRegion(
        parse("+4640404", RegionCode.SE), RegionCode.US));
    assertEquals(ShortNumberInfo.ShortNumberCost.UNKNOWN_COST,
        shortInfo.getExpectedCostForRegion(
            parse("+4640404", RegionCode.SE), RegionCode.US));
  }

  private PhoneNumber parse(String number, String regionCode) {
    try {
      return phoneUtil.parse(number, regionCode);
    } catch (NumberParseException e) {
      throw new AssertionError(
          "Test input data should always parse correctly: " + number + " (" + regionCode + ")", e);
    }
  }
}
