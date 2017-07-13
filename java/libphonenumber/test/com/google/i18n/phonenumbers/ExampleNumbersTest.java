/*
 * Copyright (C) 2009 The Libphonenumber Authors
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

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verifies all of the example numbers in the metadata are valid and of the correct type. If no
 * example number exists for a particular type, the test still passes since not all types are
 * relevant for all regions. Tests that check the XML schema will ensure that an exampleNumber
 * node is present for every phone number description.
 */
public class ExampleNumbersTest extends TestCase {
  private static final Logger logger = Logger.getLogger(ExampleNumbersTest.class.getName());
  private PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
  private ShortNumberInfo shortNumberInfo = ShortNumberInfo.getInstance();
  private List<PhoneNumber> invalidCases = new ArrayList<PhoneNumber>();
  private List<PhoneNumber> wrongTypeCases = new ArrayList<PhoneNumber>();

  /**
   * @param exampleNumberRequestedType  type we are requesting an example number for
   * @param possibleExpectedTypes  acceptable types that this number should match, such as
   *     FIXED_LINE and FIXED_LINE_OR_MOBILE for a fixed line example number.
   */
  private void checkNumbersValidAndCorrectType(PhoneNumberType exampleNumberRequestedType,
                                               Set<PhoneNumberType> possibleExpectedTypes) {
    for (String regionCode : phoneNumberUtil.getSupportedRegions()) {
      PhoneNumber exampleNumber =
          phoneNumberUtil.getExampleNumberForType(regionCode, exampleNumberRequestedType);
      if (exampleNumber != null) {
        if (!phoneNumberUtil.isValidNumber(exampleNumber)) {
          invalidCases.add(exampleNumber);
          logger.log(Level.SEVERE, "Failed validation for " + exampleNumber.toString());
        } else {
          // We know the number is valid, now we check the type.
          PhoneNumberType exampleNumberType = phoneNumberUtil.getNumberType(exampleNumber);
          if (!possibleExpectedTypes.contains(exampleNumberType)) {
            wrongTypeCases.add(exampleNumber);
            logger.log(Level.SEVERE, "Wrong type for "
                + exampleNumber.toString()
                + ": got " + exampleNumberType);
            logger.log(Level.WARNING, "Expected types: ");
            for (PhoneNumberType type : possibleExpectedTypes) {
              logger.log(Level.WARNING, type.toString());
            }
          }
        }
      }
    }
  }

  public void testFixedLine() throws Exception {
    Set<PhoneNumberType> fixedLineTypes = EnumSet.of(PhoneNumberType.FIXED_LINE,
                                                     PhoneNumberType.FIXED_LINE_OR_MOBILE);
    checkNumbersValidAndCorrectType(PhoneNumberType.FIXED_LINE, fixedLineTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testMobile() throws Exception {
    Set<PhoneNumberType> mobileTypes = EnumSet.of(PhoneNumberType.MOBILE,
                                                  PhoneNumberType.FIXED_LINE_OR_MOBILE);
    checkNumbersValidAndCorrectType(PhoneNumberType.MOBILE, mobileTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testTollFree() throws Exception {
    Set<PhoneNumberType> tollFreeTypes = EnumSet.of(PhoneNumberType.TOLL_FREE);
    checkNumbersValidAndCorrectType(PhoneNumberType.TOLL_FREE, tollFreeTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testPremiumRate() throws Exception {
    Set<PhoneNumberType> premiumRateTypes = EnumSet.of(PhoneNumberType.PREMIUM_RATE);
    checkNumbersValidAndCorrectType(PhoneNumberType.PREMIUM_RATE, premiumRateTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testVoip() throws Exception {
    Set<PhoneNumberType> voipTypes = EnumSet.of(PhoneNumberType.VOIP);
    checkNumbersValidAndCorrectType(PhoneNumberType.VOIP, voipTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testPager() throws Exception {
    Set<PhoneNumberType> pagerTypes = EnumSet.of(PhoneNumberType.PAGER);
    checkNumbersValidAndCorrectType(PhoneNumberType.PAGER, pagerTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testUan() throws Exception {
    Set<PhoneNumberType> uanTypes = EnumSet.of(PhoneNumberType.UAN);
    checkNumbersValidAndCorrectType(PhoneNumberType.UAN, uanTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testVoicemail() throws Exception {
    Set<PhoneNumberType> voicemailTypes = EnumSet.of(PhoneNumberType.VOICEMAIL);
    checkNumbersValidAndCorrectType(PhoneNumberType.VOICEMAIL, voicemailTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testSharedCost() throws Exception {
    Set<PhoneNumberType> sharedCostTypes = EnumSet.of(PhoneNumberType.SHARED_COST);
    checkNumbersValidAndCorrectType(PhoneNumberType.SHARED_COST, sharedCostTypes);
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testCanBeInternationallyDialled() throws Exception {
    for (String regionCode : phoneNumberUtil.getSupportedRegions()) {
      PhoneNumber exampleNumber = null;
      PhoneNumberDesc desc =
          phoneNumberUtil.getMetadataForRegion(regionCode).getNoInternationalDialling();
      try {
        if (desc.hasExampleNumber()) {
          exampleNumber = phoneNumberUtil.parse(desc.getExampleNumber(), regionCode);
        }
      } catch (NumberParseException e) {
        logger.log(Level.SEVERE, e.toString());
      }
      if (exampleNumber != null && phoneNumberUtil.canBeInternationallyDialled(exampleNumber)) {
        wrongTypeCases.add(exampleNumber);
        logger.log(Level.SEVERE, "Number " + exampleNumber.toString()
            + " should not be internationally diallable");
      }
    }
    assertEquals(0, wrongTypeCases.size());
  }

  public void testGlobalNetworkNumbers() throws Exception {
    for (Integer callingCode : phoneNumberUtil.getSupportedGlobalNetworkCallingCodes()) {
      PhoneNumber exampleNumber =
          phoneNumberUtil.getExampleNumberForNonGeoEntity(callingCode);
      assertNotNull("No example phone number for calling code " + callingCode, exampleNumber);
      if (!phoneNumberUtil.isValidNumber(exampleNumber)) {
        invalidCases.add(exampleNumber);
        logger.log(Level.SEVERE, "Failed validation for " + exampleNumber.toString());
      }
    }
    assertEquals(0, invalidCases.size());
  }

  public void testEveryRegionHasAnExampleNumber() throws Exception {
    for (String regionCode : phoneNumberUtil.getSupportedRegions()) {
      PhoneNumber exampleNumber = phoneNumberUtil.getExampleNumber(regionCode);
      assertNotNull("No example number found for region " + regionCode, exampleNumber);
    }
  }

  public void testEveryRegionHasAnInvalidExampleNumber() throws Exception {
    for (String regionCode : phoneNumberUtil.getSupportedRegions()) {
      PhoneNumber exampleNumber = phoneNumberUtil.getInvalidExampleNumber(regionCode);
      assertNotNull("No invalid example number found for region " + regionCode, exampleNumber);
    }
  }

  public void testEveryTypeHasAnExampleNumber() throws Exception {
    for (PhoneNumberUtil.PhoneNumberType type : PhoneNumberUtil.PhoneNumberType.values()) {
      if (type == PhoneNumberType.UNKNOWN) {
        continue;
      }
      PhoneNumber exampleNumber = phoneNumberUtil.getExampleNumberForType(type);
      assertNotNull("No example number found for type " + type, exampleNumber);
    }
  }

  public void testShortNumbersValidAndCorrectCost() throws Exception {
    List<String> invalidStringCases = new ArrayList<String>();
    for (String regionCode : shortNumberInfo.getSupportedRegions()) {
      String exampleShortNumber = shortNumberInfo.getExampleShortNumber(regionCode);
      if (!shortNumberInfo.isValidShortNumberForRegion(
          phoneNumberUtil.parse(exampleShortNumber, regionCode), regionCode)) {
        String invalidStringCase = "region_code: " + regionCode + ", national_number: "
            + exampleShortNumber;
        invalidStringCases.add(invalidStringCase);
        logger.log(Level.SEVERE, "Failed validation for string " + invalidStringCase);
      }
      PhoneNumber phoneNumber = phoneNumberUtil.parse(exampleShortNumber, regionCode);
      if (!shortNumberInfo.isValidShortNumber(phoneNumber)) {
        invalidCases.add(phoneNumber);
        logger.log(Level.SEVERE, "Failed validation for " + phoneNumber.toString());
      }

      for (ShortNumberInfo.ShortNumberCost cost : ShortNumberInfo.ShortNumberCost.values()) {
        exampleShortNumber = shortNumberInfo.getExampleShortNumberForCost(regionCode, cost);
        if (!exampleShortNumber.equals("")) {
          phoneNumber = phoneNumberUtil.parse(exampleShortNumber, regionCode);
          ShortNumberInfo.ShortNumberCost exampleShortNumberCost =
              shortNumberInfo.getExpectedCostForRegion(phoneNumber, regionCode);
          if (cost != exampleShortNumberCost) {
            wrongTypeCases.add(phoneNumber);
            logger.log(Level.SEVERE, "Wrong cost for " + phoneNumber.toString()
                + ": got " + exampleShortNumberCost
                + ", expected: " + cost);
          }
        }
      }
    }
    assertEquals(0, invalidStringCases.size());
    assertEquals(0, invalidCases.size());
    assertEquals(0, wrongTypeCases.size());
  }

  public void testEmergency() throws Exception {
    int wrongTypeCounter = 0;
    for (String regionCode : shortNumberInfo.getSupportedRegions()) {
      PhoneNumberDesc desc =
          MetadataManager.getShortNumberMetadataForRegion(regionCode).getEmergency();
      if (desc.hasExampleNumber()) {
        String exampleNumber = desc.getExampleNumber();
        PhoneNumber phoneNumber = phoneNumberUtil.parse(exampleNumber, regionCode);
        if (!shortNumberInfo.isPossibleShortNumberForRegion(phoneNumber, regionCode)
            || !shortNumberInfo.isEmergencyNumber(exampleNumber, regionCode)) {
          wrongTypeCounter++;
          logger.log(Level.SEVERE, "Emergency example number test failed for " + regionCode);
        } else if (shortNumberInfo.getExpectedCostForRegion(phoneNumber, regionCode)
            != ShortNumberInfo.ShortNumberCost.TOLL_FREE) {
          wrongTypeCounter++;
          logger.log(Level.WARNING, "Emergency example number not toll free for " + regionCode);
        }
      }
    }
    assertEquals(0, wrongTypeCounter);
  }

  public void testCarrierSpecificShortNumbers() throws Exception {
    int wrongTagCounter = 0;
    for (String regionCode : shortNumberInfo.getSupportedRegions()) {
      PhoneNumberDesc desc =
          MetadataManager.getShortNumberMetadataForRegion(regionCode).getCarrierSpecific();
      if (desc.hasExampleNumber()) {
        String exampleNumber = desc.getExampleNumber();
        PhoneNumber carrierSpecificNumber = phoneNumberUtil.parse(exampleNumber, regionCode);
        if (!shortNumberInfo.isPossibleShortNumberForRegion(carrierSpecificNumber, regionCode)
            || !shortNumberInfo.isCarrierSpecificForRegion(carrierSpecificNumber, regionCode)) {
          wrongTagCounter++;
          logger.log(Level.SEVERE, "Carrier-specific test failed for " + regionCode);
        }
      }
    }
    assertEquals(0, wrongTagCounter);
  }

  public void testSmsServiceShortNumbers() throws Exception {
    int wrongTagCounter = 0;
    for (String regionCode : shortNumberInfo.getSupportedRegions()) {
      PhoneNumberDesc desc =
          MetadataManager.getShortNumberMetadataForRegion(regionCode).getSmsServices();
      if (desc.hasExampleNumber()) {
        String exampleNumber = desc.getExampleNumber();
        PhoneNumber smsServiceNumber = phoneNumberUtil.parse(exampleNumber, regionCode);
        if (!shortNumberInfo.isPossibleShortNumberForRegion(smsServiceNumber, regionCode)
            || !shortNumberInfo.isSmsServiceForRegion(smsServiceNumber, regionCode)) {
          wrongTagCounter++;
          logger.log(Level.SEVERE, "SMS service test failed for " + regionCode);
        }
      }
    }
    assertEquals(0, wrongTagCounter);
  }
}
