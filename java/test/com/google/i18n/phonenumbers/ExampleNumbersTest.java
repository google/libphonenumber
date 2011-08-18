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
 * @author Lara Rennie
 *
 * Verifies all of the example numbers in the metadata are valid and of the correct type. If no
 * example number exists for a particular type, the test still passes.
 */
public class ExampleNumbersTest extends TestCase {
  private static final Logger LOGGER = Logger.getLogger(ExampleNumbersTest.class.getName());
  private PhoneNumberUtil phoneNumberUtil;
  private List<PhoneNumber> invalidCases = new ArrayList<PhoneNumber>();
  private List<PhoneNumber> wrongTypeCases = new ArrayList<PhoneNumber>();

  public ExampleNumbersTest() {
    PhoneNumberUtil.resetInstance();
    phoneNumberUtil = PhoneNumberUtil.getInstance();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    invalidCases.clear();
    wrongTypeCases.clear();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * @param exampleNumberRequestedType  type we are requesting an example number for
   * @param possibleExpectedTypes       acceptable types that this number should match, such as
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
          LOGGER.log(Level.SEVERE, "Failed validation for " + exampleNumber.toString());
        } else {
          // We know the number is valid, now we check the type.
          PhoneNumberType exampleNumberType = phoneNumberUtil.getNumberType(exampleNumber);
          if (!possibleExpectedTypes.contains(exampleNumberType)) {
            wrongTypeCases.add(exampleNumber);
            LOGGER.log(Level.SEVERE, "Wrong type for " +
                       exampleNumber.toString() +
                       ": got " + exampleNumberType);
            LOGGER.log(Level.WARNING, "Expected types: ");
            for (PhoneNumberType type : possibleExpectedTypes) {
              LOGGER.log(Level.WARNING, type.toString());
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
        LOGGER.log(Level.SEVERE, e.toString());
      }
      if (exampleNumber != null && phoneNumberUtil.canBeInternationallyDialled(exampleNumber)) {
        wrongTypeCases.add(exampleNumber);
      }
    }
    assertEquals(0, wrongTypeCases.size());
  }
}
