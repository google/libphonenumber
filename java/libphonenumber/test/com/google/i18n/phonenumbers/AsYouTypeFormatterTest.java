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

/**
 * Unit tests for AsYouTypeFormatter.java
 *
 * Note that these tests use the test metadata, not the normal metadata file, so should not be used
 * for regression test purposes - these tests are illustrative only and test functionality.
 *
 * @author Shaopeng Jia
 */
public class AsYouTypeFormatterTest extends TestMetadataTestCase {

  public void testInvalidRegion() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.ZZ);
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+4", formatter.inputDigit('4'));
    assertEquals("+48 ", formatter.inputDigit('8'));
    assertEquals("+48 8", formatter.inputDigit('8'));
    assertEquals("+48 88", formatter.inputDigit('8'));
    assertEquals("+48 88 1", formatter.inputDigit('1'));
    assertEquals("+48 88 12", formatter.inputDigit('2'));
    assertEquals("+48 88 123", formatter.inputDigit('3'));
    assertEquals("+48 88 123 1", formatter.inputDigit('1'));
    assertEquals("+48 88 123 12", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("6", formatter.inputDigit('6'));
    assertEquals("65", formatter.inputDigit('5'));
    assertEquals("650", formatter.inputDigit('0'));
    assertEquals("6502", formatter.inputDigit('2'));
    assertEquals("65025", formatter.inputDigit('5'));
    assertEquals("650253", formatter.inputDigit('3'));
  }

  public void testInvalidPlusSign() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.ZZ);
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+4", formatter.inputDigit('4'));
    assertEquals("+48 ", formatter.inputDigit('8'));
    assertEquals("+48 8", formatter.inputDigit('8'));
    assertEquals("+48 88", formatter.inputDigit('8'));
    assertEquals("+48 88 1", formatter.inputDigit('1'));
    assertEquals("+48 88 12", formatter.inputDigit('2'));
    assertEquals("+48 88 123", formatter.inputDigit('3'));
    assertEquals("+48 88 123 1", formatter.inputDigit('1'));
    // A plus sign can only appear at the beginning of the number; otherwise, no formatting is
    // applied.
    assertEquals("+48881231+", formatter.inputDigit('+'));
    assertEquals("+48881231+2", formatter.inputDigit('2'));
  }

  public void testTooLongNumberMatchingMultipleLeadingDigits() {
    // See https://github.com/google/libphonenumber/issues/36
    // The bug occurred last time for countries which have two formatting rules with exactly the
    // same leading digits pattern but differ in length.
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.ZZ);
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+81 ", formatter.inputDigit('1'));
    assertEquals("+81 9", formatter.inputDigit('9'));
    assertEquals("+81 90", formatter.inputDigit('0'));
    assertEquals("+81 90 1", formatter.inputDigit('1'));
    assertEquals("+81 90 12", formatter.inputDigit('2'));
    assertEquals("+81 90 123", formatter.inputDigit('3'));
    assertEquals("+81 90 1234", formatter.inputDigit('4'));
    assertEquals("+81 90 1234 5", formatter.inputDigit('5'));
    assertEquals("+81 90 1234 56", formatter.inputDigit('6'));
    assertEquals("+81 90 1234 567", formatter.inputDigit('7'));
    assertEquals("+81 90 1234 5678", formatter.inputDigit('8'));
    assertEquals("+81 90 12 345 6789", formatter.inputDigit('9'));
    assertEquals("+81901234567890", formatter.inputDigit('0'));
    assertEquals("+819012345678901", formatter.inputDigit('1'));
  }

  public void testCountryWithSpaceInNationalPrefixFormattingRule() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.BY);
    assertEquals("8", formatter.inputDigit('8'));
    assertEquals("88", formatter.inputDigit('8'));
    assertEquals("881", formatter.inputDigit('1'));
    assertEquals("8 819", formatter.inputDigit('9'));
    assertEquals("8 8190", formatter.inputDigit('0'));
    // The formatting rule for 5 digit numbers states that no space should be present after the
    // national prefix.
    assertEquals("881 901", formatter.inputDigit('1'));
    assertEquals("8 819 012", formatter.inputDigit('2'));
    // Too long, no formatting rule applies.
    assertEquals("88190123", formatter.inputDigit('3'));
  }

  public void testCountryWithSpaceInNationalPrefixFormattingRuleAndLongNdd() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.BY);
    assertEquals("9", formatter.inputDigit('9'));
    assertEquals("99", formatter.inputDigit('9'));
    assertEquals("999", formatter.inputDigit('9'));
    assertEquals("9999", formatter.inputDigit('9'));
    assertEquals("99999 ", formatter.inputDigit('9'));
    assertEquals("99999 1", formatter.inputDigit('1'));
    assertEquals("99999 12", formatter.inputDigit('2'));
    assertEquals("99999 123", formatter.inputDigit('3'));
    assertEquals("99999 1234", formatter.inputDigit('4'));
    assertEquals("99999 12 345", formatter.inputDigit('5'));
  }

  public void testAYTFUS() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.US);
    assertEquals("6", formatter.inputDigit('6'));
    assertEquals("65", formatter.inputDigit('5'));
    assertEquals("650", formatter.inputDigit('0'));
    assertEquals("650 2", formatter.inputDigit('2'));
    assertEquals("650 25", formatter.inputDigit('5'));
    assertEquals("650 253", formatter.inputDigit('3'));
    // Note this is how a US local number (without area code) should be formatted.
    assertEquals("650 2532", formatter.inputDigit('2'));
    assertEquals("650 253 22", formatter.inputDigit('2'));
    assertEquals("650 253 222", formatter.inputDigit('2'));
    assertEquals("650 253 2222", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("16", formatter.inputDigit('6'));
    assertEquals("1 65", formatter.inputDigit('5'));
    assertEquals("1 650", formatter.inputDigit('0'));
    assertEquals("1 650 2", formatter.inputDigit('2'));
    assertEquals("1 650 25", formatter.inputDigit('5'));
    assertEquals("1 650 253", formatter.inputDigit('3'));
    assertEquals("1 650 253 2", formatter.inputDigit('2'));
    assertEquals("1 650 253 22", formatter.inputDigit('2'));
    assertEquals("1 650 253 222", formatter.inputDigit('2'));
    assertEquals("1 650 253 2222", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011 ", formatter.inputDigit('1'));
    assertEquals("011 4", formatter.inputDigit('4'));
    assertEquals("011 44 ", formatter.inputDigit('4'));
    assertEquals("011 44 6", formatter.inputDigit('6'));
    assertEquals("011 44 61", formatter.inputDigit('1'));
    assertEquals("011 44 6 12", formatter.inputDigit('2'));
    assertEquals("011 44 6 123", formatter.inputDigit('3'));
    assertEquals("011 44 6 123 1", formatter.inputDigit('1'));
    assertEquals("011 44 6 123 12", formatter.inputDigit('2'));
    assertEquals("011 44 6 123 123", formatter.inputDigit('3'));
    assertEquals("011 44 6 123 123 1", formatter.inputDigit('1'));
    assertEquals("011 44 6 123 123 12", formatter.inputDigit('2'));
    assertEquals("011 44 6 123 123 123", formatter.inputDigit('3'));

    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011 ", formatter.inputDigit('1'));
    assertEquals("011 5", formatter.inputDigit('5'));
    assertEquals("011 54 ", formatter.inputDigit('4'));
    assertEquals("011 54 9", formatter.inputDigit('9'));
    assertEquals("011 54 91", formatter.inputDigit('1'));
    assertEquals("011 54 9 11", formatter.inputDigit('1'));
    assertEquals("011 54 9 11 2", formatter.inputDigit('2'));
    assertEquals("011 54 9 11 23", formatter.inputDigit('3'));
    assertEquals("011 54 9 11 231", formatter.inputDigit('1'));
    assertEquals("011 54 9 11 2312", formatter.inputDigit('2'));
    assertEquals("011 54 9 11 2312 1", formatter.inputDigit('1'));
    assertEquals("011 54 9 11 2312 12", formatter.inputDigit('2'));
    assertEquals("011 54 9 11 2312 123", formatter.inputDigit('3'));
    assertEquals("011 54 9 11 2312 1234", formatter.inputDigit('4'));

    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011 ", formatter.inputDigit('1'));
    assertEquals("011 2", formatter.inputDigit('2'));
    assertEquals("011 24", formatter.inputDigit('4'));
    assertEquals("011 244 ", formatter.inputDigit('4'));
    assertEquals("011 244 2", formatter.inputDigit('2'));
    assertEquals("011 244 28", formatter.inputDigit('8'));
    assertEquals("011 244 280", formatter.inputDigit('0'));
    assertEquals("011 244 280 0", formatter.inputDigit('0'));
    assertEquals("011 244 280 00", formatter.inputDigit('0'));
    assertEquals("011 244 280 000", formatter.inputDigit('0'));
    assertEquals("011 244 280 000 0", formatter.inputDigit('0'));
    assertEquals("011 244 280 000 00", formatter.inputDigit('0'));
    assertEquals("011 244 280 000 000", formatter.inputDigit('0'));

    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+4", formatter.inputDigit('4'));
    assertEquals("+48 ", formatter.inputDigit('8'));
    assertEquals("+48 8", formatter.inputDigit('8'));
    assertEquals("+48 88", formatter.inputDigit('8'));
    assertEquals("+48 88 1", formatter.inputDigit('1'));
    assertEquals("+48 88 12", formatter.inputDigit('2'));
    assertEquals("+48 88 123", formatter.inputDigit('3'));
    assertEquals("+48 88 123 1", formatter.inputDigit('1'));
    assertEquals("+48 88 123 12", formatter.inputDigit('2'));
    assertEquals("+48 88 123 12 1", formatter.inputDigit('1'));
    assertEquals("+48 88 123 12 12", formatter.inputDigit('2'));
  }

  public void testAYTFUSFullWidthCharacters() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.US);
    assertEquals("\uFF16", formatter.inputDigit('\uFF16'));
    assertEquals("\uFF16\uFF15", formatter.inputDigit('\uFF15'));
    assertEquals("650", formatter.inputDigit('\uFF10'));
    assertEquals("650 2", formatter.inputDigit('\uFF12'));
    assertEquals("650 25", formatter.inputDigit('\uFF15'));
    assertEquals("650 253", formatter.inputDigit('\uFF13'));
    assertEquals("650 2532", formatter.inputDigit('\uFF12'));
    assertEquals("650 253 22", formatter.inputDigit('\uFF12'));
    assertEquals("650 253 222", formatter.inputDigit('\uFF12'));
    assertEquals("650 253 2222", formatter.inputDigit('\uFF12'));
  }

  public void testAYTFUSMobileShortCode() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.US);
    assertEquals("*", formatter.inputDigit('*'));
    assertEquals("*1", formatter.inputDigit('1'));
    assertEquals("*12", formatter.inputDigit('2'));
    assertEquals("*121", formatter.inputDigit('1'));
    assertEquals("*121#", formatter.inputDigit('#'));
  }

  public void testAYTFUSVanityNumber() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.US);
    assertEquals("8", formatter.inputDigit('8'));
    assertEquals("80", formatter.inputDigit('0'));
    assertEquals("800", formatter.inputDigit('0'));
    assertEquals("800 ", formatter.inputDigit(' '));
    assertEquals("800 M", formatter.inputDigit('M'));
    assertEquals("800 MY", formatter.inputDigit('Y'));
    assertEquals("800 MY ", formatter.inputDigit(' '));
    assertEquals("800 MY A", formatter.inputDigit('A'));
    assertEquals("800 MY AP", formatter.inputDigit('P'));
    assertEquals("800 MY APP", formatter.inputDigit('P'));
    assertEquals("800 MY APPL", formatter.inputDigit('L'));
    assertEquals("800 MY APPLE", formatter.inputDigit('E'));
  }

  public void testAYTFAndRememberPositionUS() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.US);
    assertEquals("1", formatter.inputDigitAndRememberPosition('1'));
    assertEquals(1, formatter.getRememberedPosition());
    assertEquals("16", formatter.inputDigit('6'));
    assertEquals("1 65", formatter.inputDigit('5'));
    assertEquals(1, formatter.getRememberedPosition());
    assertEquals("1 650", formatter.inputDigitAndRememberPosition('0'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("1 650 2", formatter.inputDigit('2'));
    assertEquals("1 650 25", formatter.inputDigit('5'));
    // Note the remembered position for digit "0" changes from 4 to 5, because a space is now
    // inserted in the front.
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("1 650 253", formatter.inputDigit('3'));
    assertEquals("1 650 253 2", formatter.inputDigit('2'));
    assertEquals("1 650 253 22", formatter.inputDigit('2'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("1 650 253 222", formatter.inputDigitAndRememberPosition('2'));
    assertEquals(13, formatter.getRememberedPosition());
    assertEquals("1 650 253 2222", formatter.inputDigit('2'));
    assertEquals(13, formatter.getRememberedPosition());
    assertEquals("165025322222", formatter.inputDigit('2'));
    assertEquals(10, formatter.getRememberedPosition());
    assertEquals("1650253222222", formatter.inputDigit('2'));
    assertEquals(10, formatter.getRememberedPosition());

    formatter.clear();
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("16", formatter.inputDigitAndRememberPosition('6'));
    assertEquals(2, formatter.getRememberedPosition());
    assertEquals("1 65", formatter.inputDigit('5'));
    assertEquals("1 650", formatter.inputDigit('0'));
    assertEquals(3, formatter.getRememberedPosition());
    assertEquals("1 650 2", formatter.inputDigit('2'));
    assertEquals("1 650 25", formatter.inputDigit('5'));
    assertEquals(3, formatter.getRememberedPosition());
    assertEquals("1 650 253", formatter.inputDigit('3'));
    assertEquals("1 650 253 2", formatter.inputDigit('2'));
    assertEquals("1 650 253 22", formatter.inputDigit('2'));
    assertEquals(3, formatter.getRememberedPosition());
    assertEquals("1 650 253 222", formatter.inputDigit('2'));
    assertEquals("1 650 253 2222", formatter.inputDigit('2'));
    assertEquals("165025322222", formatter.inputDigit('2'));
    assertEquals(2, formatter.getRememberedPosition());
    assertEquals("1650253222222", formatter.inputDigit('2'));
    assertEquals(2, formatter.getRememberedPosition());

    formatter.clear();
    assertEquals("6", formatter.inputDigit('6'));
    assertEquals("65", formatter.inputDigit('5'));
    assertEquals("650", formatter.inputDigit('0'));
    assertEquals("650 2", formatter.inputDigit('2'));
    assertEquals("650 25", formatter.inputDigit('5'));
    assertEquals("650 253", formatter.inputDigit('3'));
    assertEquals("650 2532", formatter.inputDigitAndRememberPosition('2'));
    assertEquals(8, formatter.getRememberedPosition());
    assertEquals("650 253 22", formatter.inputDigit('2'));
    assertEquals(9, formatter.getRememberedPosition());
    assertEquals("650 253 222", formatter.inputDigit('2'));
    // No more formatting when semicolon is entered.
    assertEquals("650253222;", formatter.inputDigit(';'));
    assertEquals(7, formatter.getRememberedPosition());
    assertEquals("650253222;2", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("6", formatter.inputDigit('6'));
    assertEquals("65", formatter.inputDigit('5'));
    assertEquals("650", formatter.inputDigit('0'));
    // No more formatting when users choose to do their own formatting.
    assertEquals("650-", formatter.inputDigit('-'));
    assertEquals("650-2", formatter.inputDigitAndRememberPosition('2'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("650-25", formatter.inputDigit('5'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("650-253", formatter.inputDigit('3'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("650-253-", formatter.inputDigit('-'));
    assertEquals("650-253-2", formatter.inputDigit('2'));
    assertEquals("650-253-22", formatter.inputDigit('2'));
    assertEquals("650-253-222", formatter.inputDigit('2'));
    assertEquals("650-253-2222", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011 ", formatter.inputDigit('1'));
    assertEquals("011 4", formatter.inputDigitAndRememberPosition('4'));
    assertEquals("011 48 ", formatter.inputDigit('8'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("011 48 8", formatter.inputDigit('8'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("011 48 88", formatter.inputDigit('8'));
    assertEquals("011 48 88 1", formatter.inputDigit('1'));
    assertEquals("011 48 88 12", formatter.inputDigit('2'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("011 48 88 123", formatter.inputDigit('3'));
    assertEquals("011 48 88 123 1", formatter.inputDigit('1'));
    assertEquals("011 48 88 123 12", formatter.inputDigit('2'));
    assertEquals("011 48 88 123 12 1", formatter.inputDigit('1'));
    assertEquals("011 48 88 123 12 12", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+1", formatter.inputDigit('1'));
    assertEquals("+1 6", formatter.inputDigitAndRememberPosition('6'));
    assertEquals("+1 65", formatter.inputDigit('5'));
    assertEquals("+1 650", formatter.inputDigit('0'));
    assertEquals(4, formatter.getRememberedPosition());
    assertEquals("+1 650 2", formatter.inputDigit('2'));
    assertEquals(4, formatter.getRememberedPosition());
    assertEquals("+1 650 25", formatter.inputDigit('5'));
    assertEquals("+1 650 253", formatter.inputDigitAndRememberPosition('3'));
    assertEquals("+1 650 253 2", formatter.inputDigit('2'));
    assertEquals("+1 650 253 22", formatter.inputDigit('2'));
    assertEquals("+1 650 253 222", formatter.inputDigit('2'));
    assertEquals(10, formatter.getRememberedPosition());

    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+1", formatter.inputDigit('1'));
    assertEquals("+1 6", formatter.inputDigitAndRememberPosition('6'));
    assertEquals("+1 65", formatter.inputDigit('5'));
    assertEquals("+1 650", formatter.inputDigit('0'));
    assertEquals(4, formatter.getRememberedPosition());
    assertEquals("+1 650 2", formatter.inputDigit('2'));
    assertEquals(4, formatter.getRememberedPosition());
    assertEquals("+1 650 25", formatter.inputDigit('5'));
    assertEquals("+1 650 253", formatter.inputDigit('3'));
    assertEquals("+1 650 253 2", formatter.inputDigit('2'));
    assertEquals("+1 650 253 22", formatter.inputDigit('2'));
    assertEquals("+1 650 253 222", formatter.inputDigit('2'));
    assertEquals("+1650253222;", formatter.inputDigit(';'));
    assertEquals(3, formatter.getRememberedPosition());
  }

  public void testAYTFGBFixedLine() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.GB);
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("02", formatter.inputDigit('2'));
    assertEquals("020", formatter.inputDigit('0'));
    assertEquals("020 7", formatter.inputDigitAndRememberPosition('7'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("020 70", formatter.inputDigit('0'));
    assertEquals("020 703", formatter.inputDigit('3'));
    assertEquals(5, formatter.getRememberedPosition());
    assertEquals("020 7031", formatter.inputDigit('1'));
    assertEquals("020 7031 3", formatter.inputDigit('3'));
    assertEquals("020 7031 30", formatter.inputDigit('0'));
    assertEquals("020 7031 300", formatter.inputDigit('0'));
    assertEquals("020 7031 3000", formatter.inputDigit('0'));
  }

  public void testAYTFGBTollFree() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.GB);
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("08", formatter.inputDigit('8'));
    assertEquals("080", formatter.inputDigit('0'));
    assertEquals("080 7", formatter.inputDigit('7'));
    assertEquals("080 70", formatter.inputDigit('0'));
    assertEquals("080 703", formatter.inputDigit('3'));
    assertEquals("080 7031", formatter.inputDigit('1'));
    assertEquals("080 7031 3", formatter.inputDigit('3'));
    assertEquals("080 7031 30", formatter.inputDigit('0'));
    assertEquals("080 7031 300", formatter.inputDigit('0'));
    assertEquals("080 7031 3000", formatter.inputDigit('0'));
  }

  public void testAYTFGBPremiumRate() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.GB);
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("09", formatter.inputDigit('9'));
    assertEquals("090", formatter.inputDigit('0'));
    assertEquals("090 7", formatter.inputDigit('7'));
    assertEquals("090 70", formatter.inputDigit('0'));
    assertEquals("090 703", formatter.inputDigit('3'));
    assertEquals("090 7031", formatter.inputDigit('1'));
    assertEquals("090 7031 3", formatter.inputDigit('3'));
    assertEquals("090 7031 30", formatter.inputDigit('0'));
    assertEquals("090 7031 300", formatter.inputDigit('0'));
    assertEquals("090 7031 3000", formatter.inputDigit('0'));
  }

  public void testAYTFNZMobile() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.NZ);
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("02", formatter.inputDigit('2'));
    assertEquals("021", formatter.inputDigit('1'));
    assertEquals("02-11", formatter.inputDigit('1'));
    assertEquals("02-112", formatter.inputDigit('2'));
    // Note the unittest is using fake metadata which might produce non-ideal results.
    assertEquals("02-112 3", formatter.inputDigit('3'));
    assertEquals("02-112 34", formatter.inputDigit('4'));
    assertEquals("02-112 345", formatter.inputDigit('5'));
    assertEquals("02-112 3456", formatter.inputDigit('6'));
  }

  public void testAYTFDE() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.DE);
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("03", formatter.inputDigit('3'));
    assertEquals("030", formatter.inputDigit('0'));
    assertEquals("030/1", formatter.inputDigit('1'));
    assertEquals("030/12", formatter.inputDigit('2'));
    assertEquals("030/123", formatter.inputDigit('3'));
    assertEquals("030/1234", formatter.inputDigit('4'));

    // 04134 1234
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("04", formatter.inputDigit('4'));
    assertEquals("041", formatter.inputDigit('1'));
    assertEquals("041 3", formatter.inputDigit('3'));
    assertEquals("041 34", formatter.inputDigit('4'));
    assertEquals("04134 1", formatter.inputDigit('1'));
    assertEquals("04134 12", formatter.inputDigit('2'));
    assertEquals("04134 123", formatter.inputDigit('3'));
    assertEquals("04134 1234", formatter.inputDigit('4'));

    // 08021 2345
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("08", formatter.inputDigit('8'));
    assertEquals("080", formatter.inputDigit('0'));
    assertEquals("080 2", formatter.inputDigit('2'));
    assertEquals("080 21", formatter.inputDigit('1'));
    assertEquals("08021 2", formatter.inputDigit('2'));
    assertEquals("08021 23", formatter.inputDigit('3'));
    assertEquals("08021 234", formatter.inputDigit('4'));
    assertEquals("08021 2345", formatter.inputDigit('5'));

    // 00 1 650 253 2250
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("00", formatter.inputDigit('0'));
    assertEquals("00 1 ", formatter.inputDigit('1'));
    assertEquals("00 1 6", formatter.inputDigit('6'));
    assertEquals("00 1 65", formatter.inputDigit('5'));
    assertEquals("00 1 650", formatter.inputDigit('0'));
    assertEquals("00 1 650 2", formatter.inputDigit('2'));
    assertEquals("00 1 650 25", formatter.inputDigit('5'));
    assertEquals("00 1 650 253", formatter.inputDigit('3'));
    assertEquals("00 1 650 253 2", formatter.inputDigit('2'));
    assertEquals("00 1 650 253 22", formatter.inputDigit('2'));
    assertEquals("00 1 650 253 222", formatter.inputDigit('2'));
    assertEquals("00 1 650 253 2222", formatter.inputDigit('2'));
  }

  public void testAYTFAR() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.AR);
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011", formatter.inputDigit('1'));
    assertEquals("011 7", formatter.inputDigit('7'));
    assertEquals("011 70", formatter.inputDigit('0'));
    assertEquals("011 703", formatter.inputDigit('3'));
    assertEquals("011 7031", formatter.inputDigit('1'));
    assertEquals("011 7031-3", formatter.inputDigit('3'));
    assertEquals("011 7031-30", formatter.inputDigit('0'));
    assertEquals("011 7031-300", formatter.inputDigit('0'));
    assertEquals("011 7031-3000", formatter.inputDigit('0'));
  }

  public void testAYTFARMobile() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.AR);
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+5", formatter.inputDigit('5'));
    assertEquals("+54 ", formatter.inputDigit('4'));
    assertEquals("+54 9", formatter.inputDigit('9'));
    assertEquals("+54 91", formatter.inputDigit('1'));
    assertEquals("+54 9 11", formatter.inputDigit('1'));
    assertEquals("+54 9 11 2", formatter.inputDigit('2'));
    assertEquals("+54 9 11 23", formatter.inputDigit('3'));
    assertEquals("+54 9 11 231", formatter.inputDigit('1'));
    assertEquals("+54 9 11 2312", formatter.inputDigit('2'));
    assertEquals("+54 9 11 2312 1", formatter.inputDigit('1'));
    assertEquals("+54 9 11 2312 12", formatter.inputDigit('2'));
    assertEquals("+54 9 11 2312 123", formatter.inputDigit('3'));
    assertEquals("+54 9 11 2312 1234", formatter.inputDigit('4'));
  }

  public void testAYTFKR() {
    // +82 51 234 5678
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.KR);
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+82 ", formatter.inputDigit('2'));
    assertEquals("+82 5", formatter.inputDigit('5'));
    assertEquals("+82 51", formatter.inputDigit('1'));
    assertEquals("+82 51-2", formatter.inputDigit('2'));
    assertEquals("+82 51-23", formatter.inputDigit('3'));
    assertEquals("+82 51-234", formatter.inputDigit('4'));
    assertEquals("+82 51-234-5", formatter.inputDigit('5'));
    assertEquals("+82 51-234-56", formatter.inputDigit('6'));
    assertEquals("+82 51-234-567", formatter.inputDigit('7'));
    assertEquals("+82 51-234-5678", formatter.inputDigit('8'));

    // +82 2 531 5678
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+82 ", formatter.inputDigit('2'));
    assertEquals("+82 2", formatter.inputDigit('2'));
    assertEquals("+82 25", formatter.inputDigit('5'));
    assertEquals("+82 2-53", formatter.inputDigit('3'));
    assertEquals("+82 2-531", formatter.inputDigit('1'));
    assertEquals("+82 2-531-5", formatter.inputDigit('5'));
    assertEquals("+82 2-531-56", formatter.inputDigit('6'));
    assertEquals("+82 2-531-567", formatter.inputDigit('7'));
    assertEquals("+82 2-531-5678", formatter.inputDigit('8'));

    // +82 2 3665 5678
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+82 ", formatter.inputDigit('2'));
    assertEquals("+82 2", formatter.inputDigit('2'));
    assertEquals("+82 23", formatter.inputDigit('3'));
    assertEquals("+82 2-36", formatter.inputDigit('6'));
    assertEquals("+82 2-366", formatter.inputDigit('6'));
    assertEquals("+82 2-3665", formatter.inputDigit('5'));
    assertEquals("+82 2-3665-5", formatter.inputDigit('5'));
    assertEquals("+82 2-3665-56", formatter.inputDigit('6'));
    assertEquals("+82 2-3665-567", formatter.inputDigit('7'));
    assertEquals("+82 2-3665-5678", formatter.inputDigit('8'));

    // 02-114
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("02", formatter.inputDigit('2'));
    assertEquals("021", formatter.inputDigit('1'));
    assertEquals("02-11", formatter.inputDigit('1'));
    assertEquals("02-114", formatter.inputDigit('4'));

    // 02-1300
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("02", formatter.inputDigit('2'));
    assertEquals("021", formatter.inputDigit('1'));
    assertEquals("02-13", formatter.inputDigit('3'));
    assertEquals("02-130", formatter.inputDigit('0'));
    assertEquals("02-1300", formatter.inputDigit('0'));

    // 011-456-7890
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011", formatter.inputDigit('1'));
    assertEquals("011-4", formatter.inputDigit('4'));
    assertEquals("011-45", formatter.inputDigit('5'));
    assertEquals("011-456", formatter.inputDigit('6'));
    assertEquals("011-456-7", formatter.inputDigit('7'));
    assertEquals("011-456-78", formatter.inputDigit('8'));
    assertEquals("011-456-789", formatter.inputDigit('9'));
    assertEquals("011-456-7890", formatter.inputDigit('0'));

    // 011-9876-7890
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011", formatter.inputDigit('1'));
    assertEquals("011-9", formatter.inputDigit('9'));
    assertEquals("011-98", formatter.inputDigit('8'));
    assertEquals("011-987", formatter.inputDigit('7'));
    assertEquals("011-9876", formatter.inputDigit('6'));
    assertEquals("011-9876-7", formatter.inputDigit('7'));
    assertEquals("011-9876-78", formatter.inputDigit('8'));
    assertEquals("011-9876-789", formatter.inputDigit('9'));
    assertEquals("011-9876-7890", formatter.inputDigit('0'));
  }

  public void testAYTF_MX() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.MX);

    // +52 800 123 4567
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+5", formatter.inputDigit('5'));
    assertEquals("+52 ", formatter.inputDigit('2'));
    assertEquals("+52 8", formatter.inputDigit('8'));
    assertEquals("+52 80", formatter.inputDigit('0'));
    assertEquals("+52 800", formatter.inputDigit('0'));
    assertEquals("+52 800 1", formatter.inputDigit('1'));
    assertEquals("+52 800 12", formatter.inputDigit('2'));
    assertEquals("+52 800 123", formatter.inputDigit('3'));
    assertEquals("+52 800 123 4", formatter.inputDigit('4'));
    assertEquals("+52 800 123 45", formatter.inputDigit('5'));
    assertEquals("+52 800 123 456", formatter.inputDigit('6'));
    assertEquals("+52 800 123 4567", formatter.inputDigit('7'));
    
    // +529011234567, proactively ensuring that no formatting is applied, where a format is chosen
    // that would otherwise have led to some digits being dropped.
    formatter.clear();
    assertEquals("9", formatter.inputDigit('9'));
    assertEquals("90", formatter.inputDigit('0'));
    assertEquals("901", formatter.inputDigit('1'));
    assertEquals("9011", formatter.inputDigit('1'));
    assertEquals("90112", formatter.inputDigit('2'));
    assertEquals("901123", formatter.inputDigit('3'));
    assertEquals("9011234", formatter.inputDigit('4'));
    assertEquals("90112345", formatter.inputDigit('5'));
    assertEquals("901123456", formatter.inputDigit('6'));
    assertEquals("9011234567", formatter.inputDigit('7'));

    // +52 55 1234 5678
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+5", formatter.inputDigit('5'));
    assertEquals("+52 ", formatter.inputDigit('2'));
    assertEquals("+52 5", formatter.inputDigit('5'));
    assertEquals("+52 55", formatter.inputDigit('5'));
    assertEquals("+52 55 1", formatter.inputDigit('1'));
    assertEquals("+52 55 12", formatter.inputDigit('2'));
    assertEquals("+52 55 123", formatter.inputDigit('3'));
    assertEquals("+52 55 1234", formatter.inputDigit('4'));
    assertEquals("+52 55 1234 5", formatter.inputDigit('5'));
    assertEquals("+52 55 1234 56", formatter.inputDigit('6'));
    assertEquals("+52 55 1234 567", formatter.inputDigit('7'));
    assertEquals("+52 55 1234 5678", formatter.inputDigit('8'));

    // +52 212 345 6789
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+5", formatter.inputDigit('5'));
    assertEquals("+52 ", formatter.inputDigit('2'));
    assertEquals("+52 2", formatter.inputDigit('2'));
    assertEquals("+52 21", formatter.inputDigit('1'));
    assertEquals("+52 212", formatter.inputDigit('2'));
    assertEquals("+52 212 3", formatter.inputDigit('3'));
    assertEquals("+52 212 34", formatter.inputDigit('4'));
    assertEquals("+52 212 345", formatter.inputDigit('5'));
    assertEquals("+52 212 345 6", formatter.inputDigit('6'));
    assertEquals("+52 212 345 67", formatter.inputDigit('7'));
    assertEquals("+52 212 345 678", formatter.inputDigit('8'));
    assertEquals("+52 212 345 6789", formatter.inputDigit('9'));

    // +52 1 55 1234 5678
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+5", formatter.inputDigit('5'));
    assertEquals("+52 ", formatter.inputDigit('2'));
    assertEquals("+52 1", formatter.inputDigit('1'));
    assertEquals("+52 15", formatter.inputDigit('5'));
    assertEquals("+52 1 55", formatter.inputDigit('5'));
    assertEquals("+52 1 55 1", formatter.inputDigit('1'));
    assertEquals("+52 1 55 12", formatter.inputDigit('2'));
    assertEquals("+52 1 55 123", formatter.inputDigit('3'));
    assertEquals("+52 1 55 1234", formatter.inputDigit('4'));
    assertEquals("+52 1 55 1234 5", formatter.inputDigit('5'));
    assertEquals("+52 1 55 1234 56", formatter.inputDigit('6'));
    assertEquals("+52 1 55 1234 567", formatter.inputDigit('7'));
    assertEquals("+52 1 55 1234 5678", formatter.inputDigit('8'));

    // +52 1 541 234 5678
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+5", formatter.inputDigit('5'));
    assertEquals("+52 ", formatter.inputDigit('2'));
    assertEquals("+52 1", formatter.inputDigit('1'));
    assertEquals("+52 15", formatter.inputDigit('5'));
    assertEquals("+52 1 54", formatter.inputDigit('4'));
    assertEquals("+52 1 541", formatter.inputDigit('1'));
    assertEquals("+52 1 541 2", formatter.inputDigit('2'));
    assertEquals("+52 1 541 23", formatter.inputDigit('3'));
    assertEquals("+52 1 541 234", formatter.inputDigit('4'));
    assertEquals("+52 1 541 234 5", formatter.inputDigit('5'));
    assertEquals("+52 1 541 234 56", formatter.inputDigit('6'));
    assertEquals("+52 1 541 234 567", formatter.inputDigit('7'));
    assertEquals("+52 1 541 234 5678", formatter.inputDigit('8'));
  }

  public void testAYTF_International_Toll_Free() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.US);
    // +800 1234 5678
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+80", formatter.inputDigit('0'));
    assertEquals("+800 ", formatter.inputDigit('0'));
    assertEquals("+800 1", formatter.inputDigit('1'));
    assertEquals("+800 12", formatter.inputDigit('2'));
    assertEquals("+800 123", formatter.inputDigit('3'));
    assertEquals("+800 1234", formatter.inputDigit('4'));
    assertEquals("+800 1234 5", formatter.inputDigit('5'));
    assertEquals("+800 1234 56", formatter.inputDigit('6'));
    assertEquals("+800 1234 567", formatter.inputDigit('7'));
    assertEquals("+800 1234 5678", formatter.inputDigit('8'));
    assertEquals("+800123456789", formatter.inputDigit('9'));
  }

  public void testAYTFMultipleLeadingDigitPatterns() {
    // +81 50 2345 6789
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.JP);
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+81 ", formatter.inputDigit('1'));
    assertEquals("+81 5", formatter.inputDigit('5'));
    assertEquals("+81 50", formatter.inputDigit('0'));
    assertEquals("+81 50 2", formatter.inputDigit('2'));
    assertEquals("+81 50 23", formatter.inputDigit('3'));
    assertEquals("+81 50 234", formatter.inputDigit('4'));
    assertEquals("+81 50 2345", formatter.inputDigit('5'));
    assertEquals("+81 50 2345 6", formatter.inputDigit('6'));
    assertEquals("+81 50 2345 67", formatter.inputDigit('7'));
    assertEquals("+81 50 2345 678", formatter.inputDigit('8'));
    assertEquals("+81 50 2345 6789", formatter.inputDigit('9'));

    // +81 222 12 5678
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+81 ", formatter.inputDigit('1'));
    assertEquals("+81 2", formatter.inputDigit('2'));
    assertEquals("+81 22", formatter.inputDigit('2'));
    assertEquals("+81 22 2", formatter.inputDigit('2'));
    assertEquals("+81 22 21", formatter.inputDigit('1'));
    assertEquals("+81 2221 2", formatter.inputDigit('2'));
    assertEquals("+81 222 12 5", formatter.inputDigit('5'));
    assertEquals("+81 222 12 56", formatter.inputDigit('6'));
    assertEquals("+81 222 12 567", formatter.inputDigit('7'));
    assertEquals("+81 222 12 5678", formatter.inputDigit('8'));

    // 011113
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011", formatter.inputDigit('1'));
    assertEquals("011 1", formatter.inputDigit('1'));
    assertEquals("011 11", formatter.inputDigit('1'));
    assertEquals("011113", formatter.inputDigit('3'));

    // +81 3332 2 5678
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+81 ", formatter.inputDigit('1'));
    assertEquals("+81 3", formatter.inputDigit('3'));
    assertEquals("+81 33", formatter.inputDigit('3'));
    assertEquals("+81 33 3", formatter.inputDigit('3'));
    assertEquals("+81 3332", formatter.inputDigit('2'));
    assertEquals("+81 3332 2", formatter.inputDigit('2'));
    assertEquals("+81 3332 2 5", formatter.inputDigit('5'));
    assertEquals("+81 3332 2 56", formatter.inputDigit('6'));
    assertEquals("+81 3332 2 567", formatter.inputDigit('7'));
    assertEquals("+81 3332 2 5678", formatter.inputDigit('8'));
  }

  public void testAYTFLongIDD_AU() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.AU);
    // 0011 1 650 253 2250
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("00", formatter.inputDigit('0'));
    assertEquals("001", formatter.inputDigit('1'));
    assertEquals("0011", formatter.inputDigit('1'));
    assertEquals("0011 1 ", formatter.inputDigit('1'));
    assertEquals("0011 1 6", formatter.inputDigit('6'));
    assertEquals("0011 1 65", formatter.inputDigit('5'));
    assertEquals("0011 1 650", formatter.inputDigit('0'));
    assertEquals("0011 1 650 2", formatter.inputDigit('2'));
    assertEquals("0011 1 650 25", formatter.inputDigit('5'));
    assertEquals("0011 1 650 253", formatter.inputDigit('3'));
    assertEquals("0011 1 650 253 2", formatter.inputDigit('2'));
    assertEquals("0011 1 650 253 22", formatter.inputDigit('2'));
    assertEquals("0011 1 650 253 222", formatter.inputDigit('2'));
    assertEquals("0011 1 650 253 2222", formatter.inputDigit('2'));

    // 0011 81 3332 2 5678
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("00", formatter.inputDigit('0'));
    assertEquals("001", formatter.inputDigit('1'));
    assertEquals("0011", formatter.inputDigit('1'));
    assertEquals("00118", formatter.inputDigit('8'));
    assertEquals("0011 81 ", formatter.inputDigit('1'));
    assertEquals("0011 81 3", formatter.inputDigit('3'));
    assertEquals("0011 81 33", formatter.inputDigit('3'));
    assertEquals("0011 81 33 3", formatter.inputDigit('3'));
    assertEquals("0011 81 3332", formatter.inputDigit('2'));
    assertEquals("0011 81 3332 2", formatter.inputDigit('2'));
    assertEquals("0011 81 3332 2 5", formatter.inputDigit('5'));
    assertEquals("0011 81 3332 2 56", formatter.inputDigit('6'));
    assertEquals("0011 81 3332 2 567", formatter.inputDigit('7'));
    assertEquals("0011 81 3332 2 5678", formatter.inputDigit('8'));

    // 0011 244 250 253 222
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("00", formatter.inputDigit('0'));
    assertEquals("001", formatter.inputDigit('1'));
    assertEquals("0011", formatter.inputDigit('1'));
    assertEquals("00112", formatter.inputDigit('2'));
    assertEquals("001124", formatter.inputDigit('4'));
    assertEquals("0011 244 ", formatter.inputDigit('4'));
    assertEquals("0011 244 2", formatter.inputDigit('2'));
    assertEquals("0011 244 25", formatter.inputDigit('5'));
    assertEquals("0011 244 250", formatter.inputDigit('0'));
    assertEquals("0011 244 250 2", formatter.inputDigit('2'));
    assertEquals("0011 244 250 25", formatter.inputDigit('5'));
    assertEquals("0011 244 250 253", formatter.inputDigit('3'));
    assertEquals("0011 244 250 253 2", formatter.inputDigit('2'));
    assertEquals("0011 244 250 253 22", formatter.inputDigit('2'));
    assertEquals("0011 244 250 253 222", formatter.inputDigit('2'));
  }

  public void testAYTFLongIDD_KR() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.KR);
    // 00300 1 650 253 2222
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("00", formatter.inputDigit('0'));
    assertEquals("003", formatter.inputDigit('3'));
    assertEquals("0030", formatter.inputDigit('0'));
    assertEquals("00300", formatter.inputDigit('0'));
    assertEquals("00300 1 ", formatter.inputDigit('1'));
    assertEquals("00300 1 6", formatter.inputDigit('6'));
    assertEquals("00300 1 65", formatter.inputDigit('5'));
    assertEquals("00300 1 650", formatter.inputDigit('0'));
    assertEquals("00300 1 650 2", formatter.inputDigit('2'));
    assertEquals("00300 1 650 25", formatter.inputDigit('5'));
    assertEquals("00300 1 650 253", formatter.inputDigit('3'));
    assertEquals("00300 1 650 253 2", formatter.inputDigit('2'));
    assertEquals("00300 1 650 253 22", formatter.inputDigit('2'));
    assertEquals("00300 1 650 253 222", formatter.inputDigit('2'));
    assertEquals("00300 1 650 253 2222", formatter.inputDigit('2'));
  }

  public void testAYTFLongNDD_KR() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.KR);
    // 08811-9876-7890
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("08", formatter.inputDigit('8'));
    assertEquals("088", formatter.inputDigit('8'));
    assertEquals("0881", formatter.inputDigit('1'));
    assertEquals("08811", formatter.inputDigit('1'));
    assertEquals("08811-9", formatter.inputDigit('9'));
    assertEquals("08811-98", formatter.inputDigit('8'));
    assertEquals("08811-987", formatter.inputDigit('7'));
    assertEquals("08811-9876", formatter.inputDigit('6'));
    assertEquals("08811-9876-7", formatter.inputDigit('7'));
    assertEquals("08811-9876-78", formatter.inputDigit('8'));
    assertEquals("08811-9876-789", formatter.inputDigit('9'));
    assertEquals("08811-9876-7890", formatter.inputDigit('0'));

    // 08500 11-9876-7890
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("08", formatter.inputDigit('8'));
    assertEquals("085", formatter.inputDigit('5'));
    assertEquals("0850", formatter.inputDigit('0'));
    assertEquals("08500 ", formatter.inputDigit('0'));
    assertEquals("08500 1", formatter.inputDigit('1'));
    assertEquals("08500 11", formatter.inputDigit('1'));
    assertEquals("08500 11-9", formatter.inputDigit('9'));
    assertEquals("08500 11-98", formatter.inputDigit('8'));
    assertEquals("08500 11-987", formatter.inputDigit('7'));
    assertEquals("08500 11-9876", formatter.inputDigit('6'));
    assertEquals("08500 11-9876-7", formatter.inputDigit('7'));
    assertEquals("08500 11-9876-78", formatter.inputDigit('8'));
    assertEquals("08500 11-9876-789", formatter.inputDigit('9'));
    assertEquals("08500 11-9876-7890", formatter.inputDigit('0'));
  }

  public void testAYTFLongNDD_SG() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.SG);
    // 777777 9876 7890
    assertEquals("7", formatter.inputDigit('7'));
    assertEquals("77", formatter.inputDigit('7'));
    assertEquals("777", formatter.inputDigit('7'));
    assertEquals("7777", formatter.inputDigit('7'));
    assertEquals("77777", formatter.inputDigit('7'));
    assertEquals("777777 ", formatter.inputDigit('7'));
    assertEquals("777777 9", formatter.inputDigit('9'));
    assertEquals("777777 98", formatter.inputDigit('8'));
    assertEquals("777777 987", formatter.inputDigit('7'));
    assertEquals("777777 9876", formatter.inputDigit('6'));
    assertEquals("777777 9876 7", formatter.inputDigit('7'));
    assertEquals("777777 9876 78", formatter.inputDigit('8'));
    assertEquals("777777 9876 789", formatter.inputDigit('9'));
    assertEquals("777777 9876 7890", formatter.inputDigit('0'));
  }

  public void testAYTFShortNumberFormattingFix_AU() {
    // For Australia, the national prefix is not optional when formatting.
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.AU);

    // 1234567890 - For leading digit 1, the national prefix formatting rule has first group only.
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("12", formatter.inputDigit('2'));
    assertEquals("123", formatter.inputDigit('3'));
    assertEquals("1234", formatter.inputDigit('4'));
    assertEquals("1234 5", formatter.inputDigit('5'));
    assertEquals("1234 56", formatter.inputDigit('6'));
    assertEquals("1234 567", formatter.inputDigit('7'));
    assertEquals("1234 567 8", formatter.inputDigit('8'));
    assertEquals("1234 567 89", formatter.inputDigit('9'));
    assertEquals("1234 567 890", formatter.inputDigit('0'));

    // +61 1234 567 890 - Test the same number, but with the country code.
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+6", formatter.inputDigit('6'));
    assertEquals("+61 ", formatter.inputDigit('1'));
    assertEquals("+61 1", formatter.inputDigit('1'));
    assertEquals("+61 12", formatter.inputDigit('2'));
    assertEquals("+61 123", formatter.inputDigit('3'));
    assertEquals("+61 1234", formatter.inputDigit('4'));
    assertEquals("+61 1234 5", formatter.inputDigit('5'));
    assertEquals("+61 1234 56", formatter.inputDigit('6'));
    assertEquals("+61 1234 567", formatter.inputDigit('7'));
    assertEquals("+61 1234 567 8", formatter.inputDigit('8'));
    assertEquals("+61 1234 567 89", formatter.inputDigit('9'));
    assertEquals("+61 1234 567 890", formatter.inputDigit('0'));

    // 212345678 - For leading digit 2, the national prefix formatting rule puts the national prefix
    // before the first group.
    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("02", formatter.inputDigit('2'));
    assertEquals("021", formatter.inputDigit('1'));
    assertEquals("02 12", formatter.inputDigit('2'));
    assertEquals("02 123", formatter.inputDigit('3'));
    assertEquals("02 1234", formatter.inputDigit('4'));
    assertEquals("02 1234 5", formatter.inputDigit('5'));
    assertEquals("02 1234 56", formatter.inputDigit('6'));
    assertEquals("02 1234 567", formatter.inputDigit('7'));
    assertEquals("02 1234 5678", formatter.inputDigit('8'));

    // 212345678 - Test the same number, but without the leading 0.
    formatter.clear();
    assertEquals("2", formatter.inputDigit('2'));
    assertEquals("21", formatter.inputDigit('1'));
    assertEquals("212", formatter.inputDigit('2'));
    assertEquals("2123", formatter.inputDigit('3'));
    assertEquals("21234", formatter.inputDigit('4'));
    assertEquals("212345", formatter.inputDigit('5'));
    assertEquals("2123456", formatter.inputDigit('6'));
    assertEquals("21234567", formatter.inputDigit('7'));
    assertEquals("212345678", formatter.inputDigit('8'));

    // +61 2 1234 5678 - Test the same number, but with the country code.
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+6", formatter.inputDigit('6'));
    assertEquals("+61 ", formatter.inputDigit('1'));
    assertEquals("+61 2", formatter.inputDigit('2'));
    assertEquals("+61 21", formatter.inputDigit('1'));
    assertEquals("+61 2 12", formatter.inputDigit('2'));
    assertEquals("+61 2 123", formatter.inputDigit('3'));
    assertEquals("+61 2 1234", formatter.inputDigit('4'));
    assertEquals("+61 2 1234 5", formatter.inputDigit('5'));
    assertEquals("+61 2 1234 56", formatter.inputDigit('6'));
    assertEquals("+61 2 1234 567", formatter.inputDigit('7'));
    assertEquals("+61 2 1234 5678", formatter.inputDigit('8'));
  }

  public void testAYTFShortNumberFormattingFix_KR() {
    // For Korea, the national prefix is not optional when formatting, and the national prefix
    // formatting rule doesn't consist of only the first group.
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.KR);

    // 111
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("11", formatter.inputDigit('1'));
    assertEquals("111", formatter.inputDigit('1'));

    // 114
    formatter.clear();
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("11", formatter.inputDigit('1'));
    assertEquals("114", formatter.inputDigit('4'));

    // 13121234 - Test a mobile number without the national prefix. Even though it is not an
    // emergency number, it should be formatted as a block.
    formatter.clear();
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("13", formatter.inputDigit('3'));
    assertEquals("131", formatter.inputDigit('1'));
    assertEquals("1312", formatter.inputDigit('2'));
    assertEquals("13121", formatter.inputDigit('1'));
    assertEquals("131212", formatter.inputDigit('2'));
    assertEquals("1312123", formatter.inputDigit('3'));
    assertEquals("13121234", formatter.inputDigit('4'));

    // +82 131-2-1234 - Test the same number, but with the country code.
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+82 ", formatter.inputDigit('2'));
    assertEquals("+82 1", formatter.inputDigit('1'));
    assertEquals("+82 13", formatter.inputDigit('3'));
    assertEquals("+82 131", formatter.inputDigit('1'));
    assertEquals("+82 131-2", formatter.inputDigit('2'));
    assertEquals("+82 131-2-1", formatter.inputDigit('1'));
    assertEquals("+82 131-2-12", formatter.inputDigit('2'));
    assertEquals("+82 131-2-123", formatter.inputDigit('3'));
    assertEquals("+82 131-2-1234", formatter.inputDigit('4'));
  }

  public void testAYTFShortNumberFormattingFix_MX() {
    // For Mexico, the national prefix is optional when formatting.
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.MX);

    // 911
    assertEquals("9", formatter.inputDigit('9'));
    assertEquals("91", formatter.inputDigit('1'));
    assertEquals("911", formatter.inputDigit('1'));

    // 800 123 4567 - Test a toll-free number, which should have a formatting rule applied to it
    // even though it doesn't begin with the national prefix.
    formatter.clear();
    assertEquals("8", formatter.inputDigit('8'));
    assertEquals("80", formatter.inputDigit('0'));
    assertEquals("800", formatter.inputDigit('0'));
    assertEquals("800 1", formatter.inputDigit('1'));
    assertEquals("800 12", formatter.inputDigit('2'));
    assertEquals("800 123", formatter.inputDigit('3'));
    assertEquals("800 123 4", formatter.inputDigit('4'));
    assertEquals("800 123 45", formatter.inputDigit('5'));
    assertEquals("800 123 456", formatter.inputDigit('6'));
    assertEquals("800 123 4567", formatter.inputDigit('7'));

    // +52 800 123 4567 - Test the same number, but with the country code.
    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+5", formatter.inputDigit('5'));
    assertEquals("+52 ", formatter.inputDigit('2'));
    assertEquals("+52 8", formatter.inputDigit('8'));
    assertEquals("+52 80", formatter.inputDigit('0'));
    assertEquals("+52 800", formatter.inputDigit('0'));
    assertEquals("+52 800 1", formatter.inputDigit('1'));
    assertEquals("+52 800 12", formatter.inputDigit('2'));
    assertEquals("+52 800 123", formatter.inputDigit('3'));
    assertEquals("+52 800 123 4", formatter.inputDigit('4'));
    assertEquals("+52 800 123 45", formatter.inputDigit('5'));
    assertEquals("+52 800 123 456", formatter.inputDigit('6'));
    assertEquals("+52 800 123 4567", formatter.inputDigit('7'));
  }

  public void testAYTFNoNationalPrefix() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.IT);

    assertEquals("3", formatter.inputDigit('3'));
    assertEquals("33", formatter.inputDigit('3'));
    assertEquals("333", formatter.inputDigit('3'));
    assertEquals("333 3", formatter.inputDigit('3'));
    assertEquals("333 33", formatter.inputDigit('3'));
    assertEquals("333 333", formatter.inputDigit('3'));
  }

  public void testAYTFNoNationalPrefixFormattingRule() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.AO);

    assertEquals("3", formatter.inputDigit('3'));
    assertEquals("33", formatter.inputDigit('3'));
    assertEquals("333", formatter.inputDigit('3'));
    assertEquals("333 3", formatter.inputDigit('3'));
    assertEquals("333 33", formatter.inputDigit('3'));
    assertEquals("333 333", formatter.inputDigit('3'));
  }

  public void testAYTFShortNumberFormattingFix_US() {
    // For the US, an initial 1 is treated specially.
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.US);

    // 101 - Test that the initial 1 is not treated as a national prefix.
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("10", formatter.inputDigit('0'));
    assertEquals("101", formatter.inputDigit('1'));

    // 112 - Test that the initial 1 is not treated as a national prefix.
    formatter.clear();
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("11", formatter.inputDigit('1'));
    assertEquals("112", formatter.inputDigit('2'));

    // 122 - Test that the initial 1 is treated as a national prefix.
    formatter.clear();
    assertEquals("1", formatter.inputDigit('1'));
    assertEquals("12", formatter.inputDigit('2'));
    assertEquals("1 22", formatter.inputDigit('2'));
  }

  public void testAYTFClearNDDAfterIDDExtraction() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.KR);

    // Check that when we have successfully extracted an IDD, the previously extracted NDD is
    // cleared since it is no longer valid.
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("00", formatter.inputDigit('0'));
    assertEquals("007", formatter.inputDigit('7'));
    assertEquals("0070", formatter.inputDigit('0'));
    assertEquals("00700", formatter.inputDigit('0'));
    assertEquals("0", formatter.getExtractedNationalPrefix());

    // Once the IDD "00700" has been extracted, it no longer makes sense for the initial "0" to be
    // treated as an NDD.
    assertEquals("00700 1 ", formatter.inputDigit('1'));
    assertEquals("", formatter.getExtractedNationalPrefix());

    assertEquals("00700 1 2", formatter.inputDigit('2'));
    assertEquals("00700 1 23", formatter.inputDigit('3'));
    assertEquals("00700 1 234", formatter.inputDigit('4'));
    assertEquals("00700 1 234 5", formatter.inputDigit('5'));
    assertEquals("00700 1 234 56", formatter.inputDigit('6'));
    assertEquals("00700 1 234 567", formatter.inputDigit('7'));
    assertEquals("00700 1 234 567 8", formatter.inputDigit('8'));
    assertEquals("00700 1 234 567 89", formatter.inputDigit('9'));
    assertEquals("00700 1 234 567 890", formatter.inputDigit('0'));
    assertEquals("00700 1 234 567 8901", formatter.inputDigit('1'));
    assertEquals("00700123456789012", formatter.inputDigit('2'));
    assertEquals("007001234567890123", formatter.inputDigit('3'));
    assertEquals("0070012345678901234", formatter.inputDigit('4'));
    assertEquals("00700123456789012345", formatter.inputDigit('5'));
    assertEquals("007001234567890123456", formatter.inputDigit('6'));
    assertEquals("0070012345678901234567", formatter.inputDigit('7'));
  }

  public void testAYTFNumberPatternsBecomingInvalidShouldNotResultInDigitLoss() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(RegionCode.CN);

    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+8", formatter.inputDigit('8'));
    assertEquals("+86 ", formatter.inputDigit('6'));
    assertEquals("+86 9", formatter.inputDigit('9'));
    assertEquals("+86 98", formatter.inputDigit('8'));
    assertEquals("+86 988", formatter.inputDigit('8'));
    assertEquals("+86 988 1", formatter.inputDigit('1'));
    // Now the number pattern is no longer valid because there are multiple leading digit patterns;
    // when we try again to extract a country code we should ensure we use the last leading digit
    // pattern, rather than the first one such that it *thinks* it's found a valid formatting rule
    // again.
    // https://github.com/google/libphonenumber/issues/437
    assertEquals("+8698812", formatter.inputDigit('2'));
    assertEquals("+86988123", formatter.inputDigit('3'));
    assertEquals("+869881234", formatter.inputDigit('4'));
    assertEquals("+8698812345", formatter.inputDigit('5'));
  }
}
