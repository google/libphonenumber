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

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for PhoneNumberUtil.java
 *
 * Note that these tests use the metadata contained in the files with TEST_META_DATA_FILE_PREFIX,
 * not the normal metadata files, so should not be used for regression test purposes - these tests
 * are illustrative only and test functionality.
 *
 * @author Shaopeng Jia
 * @author Lara Rennie
 */
public class PhoneNumberUtilTest extends TestCase {
  private PhoneNumberUtil phoneUtil;
  // This is used by BuildMetadataProtoFromXml.
  static final String TEST_META_DATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting";

  // Set up some test numbers to re-use.
  private static final PhoneNumber ALPHA_NUMERIC_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(80074935247L);
  private static final PhoneNumber AR_MOBILE =
      new PhoneNumber().setCountryCode(54).setNationalNumber(91187654321L);
  private static final PhoneNumber AR_NUMBER =
      new PhoneNumber().setCountryCode(54).setNationalNumber(1187654321);
  private static final PhoneNumber AU_NUMBER =
      new PhoneNumber().setCountryCode(61).setNationalNumber(236618300L);
  private static final PhoneNumber BS_MOBILE =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2423570000L);
  private static final PhoneNumber BS_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2423651234L);
  // Note that this is the same as the example number for DE in the metadata.
  private static final PhoneNumber DE_NUMBER =
      new PhoneNumber().setCountryCode(49).setNationalNumber(30123456L);
  private static final PhoneNumber DE_SHORT_NUMBER =
      new PhoneNumber().setCountryCode(49).setNationalNumber(1234L);
  private static final PhoneNumber GB_MOBILE =
      new PhoneNumber().setCountryCode(44).setNationalNumber(7912345678L);
  private static final PhoneNumber GB_NUMBER =
      new PhoneNumber().setCountryCode(44).setNationalNumber(2070313000L);
  private static final PhoneNumber IT_MOBILE =
      new PhoneNumber().setCountryCode(39).setNationalNumber(345678901L);
  private static final PhoneNumber IT_NUMBER =
      new PhoneNumber().setCountryCode(39).setNationalNumber(236618300L).
      setItalianLeadingZero(true);
  private static final PhoneNumber JP_STAR_NUMBER =
      new PhoneNumber().setCountryCode(81).setNationalNumber(2345);
  // Numbers to test the formatting rules from Mexico.
  private static final PhoneNumber MX_MOBILE1 =
      new PhoneNumber().setCountryCode(52).setNationalNumber(12345678900L);
  private static final PhoneNumber MX_MOBILE2 =
      new PhoneNumber().setCountryCode(52).setNationalNumber(15512345678L);
  private static final PhoneNumber MX_NUMBER1 =
      new PhoneNumber().setCountryCode(52).setNationalNumber(3312345678L);
  private static final PhoneNumber MX_NUMBER2 =
      new PhoneNumber().setCountryCode(52).setNationalNumber(8211234567L);
  private static final PhoneNumber NZ_NUMBER =
      new PhoneNumber().setCountryCode(64).setNationalNumber(33316005L);
  private static final PhoneNumber SG_NUMBER =
      new PhoneNumber().setCountryCode(65).setNationalNumber(65218000L);
  // A too-long and hence invalid US number.
  private static final PhoneNumber US_LONG_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(65025300001L);
  private static final PhoneNumber US_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(6502530000L);
  private static final PhoneNumber US_PREMIUM =
      new PhoneNumber().setCountryCode(1).setNationalNumber(9002530000L);
  // Too short, but still possible US numbers.
  private static final PhoneNumber US_LOCAL_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(2530000L);
  private static final PhoneNumber US_SHORT_BY_ONE_NUMBER =
      new PhoneNumber().setCountryCode(1).setNationalNumber(650253000L);
  private static final PhoneNumber US_TOLLFREE =
      new PhoneNumber().setCountryCode(1).setNationalNumber(8002530000L);
  private static final PhoneNumber US_SPOOF =
      new PhoneNumber().setCountryCode(1).setNationalNumber(0L);
  private static final PhoneNumber US_SPOOF_WITH_RAW_INPUT =
      new PhoneNumber().setCountryCode(1).setNationalNumber(0L)
          .setRawInput("000-000-0000");
  private static final PhoneNumber INTERNATIONAL_TOLL_FREE =
      new PhoneNumber().setCountryCode(800).setNationalNumber(12345678L);
  private static final PhoneNumber INTERNATIONAL_TOLL_FREE_TOO_LONG =
      new PhoneNumber().setCountryCode(800).setNationalNumber(1234567890L);

  public PhoneNumberUtilTest() {
    phoneUtil = initializePhoneUtilForTesting();
  }

  static PhoneNumberUtil initializePhoneUtilForTesting() {
    PhoneNumberUtil.resetInstance();
    return PhoneNumberUtil.getInstance(
        TEST_META_DATA_FILE_PREFIX,
        CountryCodeToRegionCodeMapForTesting.getCountryCodeToRegionCodeMap());
  }

  public void testGetSupportedRegions() {
    assertTrue(phoneUtil.getSupportedRegions().size() > 0);
  }

  public void testGetInstanceLoadUSMetadata() {
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion(RegionCode.US);
    assertEquals("US", metadata.getId());
    assertEquals(1, metadata.getCountryCode());
    assertEquals("011", metadata.getInternationalPrefix());
    assertTrue(metadata.hasNationalPrefix());
    assertEquals(2, metadata.numberFormatSize());
    assertEquals("(\\d{3})(\\d{3})(\\d{4})",
                 metadata.getNumberFormat(1).getPattern());
    assertEquals("$1 $2 $3", metadata.getNumberFormat(1).getFormat());
    assertEquals("[13-689]\\d{9}|2[0-35-9]\\d{8}",
                 metadata.getGeneralDesc().getNationalNumberPattern());
    assertEquals("\\d{7}(?:\\d{3})?", metadata.getGeneralDesc().getPossibleNumberPattern());
    assertTrue(metadata.getGeneralDesc().exactlySameAs(metadata.getFixedLine()));
    assertEquals("\\d{10}", metadata.getTollFree().getPossibleNumberPattern());
    assertEquals("900\\d{7}", metadata.getPremiumRate().getNationalNumberPattern());
    // No shared-cost data is available, so it should be initialised to "NA".
    assertEquals("NA", metadata.getSharedCost().getNationalNumberPattern());
    assertEquals("NA", metadata.getSharedCost().getPossibleNumberPattern());
  }

  public void testGetInstanceLoadDEMetadata() {
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion(RegionCode.DE);
    assertEquals("DE", metadata.getId());
    assertEquals(49, metadata.getCountryCode());
    assertEquals("00", metadata.getInternationalPrefix());
    assertEquals("0", metadata.getNationalPrefix());
    assertEquals(6, metadata.numberFormatSize());
    assertEquals(1, metadata.getNumberFormat(5).leadingDigitsPatternSize());
    assertEquals("900", metadata.getNumberFormat(5).getLeadingDigitsPattern(0));
    assertEquals("(\\d{3})(\\d{3,4})(\\d{4})",
                 metadata.getNumberFormat(5).getPattern());
    assertEquals("$1 $2 $3", metadata.getNumberFormat(5).getFormat());
    assertEquals("(?:[24-6]\\d{2}|3[03-9]\\d|[789](?:[1-9]\\d|0[2-9]))\\d{1,8}",
                 metadata.getFixedLine().getNationalNumberPattern());
    assertEquals("\\d{2,14}", metadata.getFixedLine().getPossibleNumberPattern());
    assertEquals("30123456", metadata.getFixedLine().getExampleNumber());
    assertEquals("\\d{10}", metadata.getTollFree().getPossibleNumberPattern());
    assertEquals("900([135]\\d{6}|9\\d{7})", metadata.getPremiumRate().getNationalNumberPattern());
  }

  public void testGetInstanceLoadARMetadata() {
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion(RegionCode.AR);
    assertEquals("AR", metadata.getId());
    assertEquals(54, metadata.getCountryCode());
    assertEquals("00", metadata.getInternationalPrefix());
    assertEquals("0", metadata.getNationalPrefix());
    assertEquals("0(?:(11|343|3715)15)?", metadata.getNationalPrefixForParsing());
    assertEquals("9$1", metadata.getNationalPrefixTransformRule());
    assertEquals("$2 15 $3-$4", metadata.getNumberFormat(2).getFormat());
    assertEquals("(9)(\\d{4})(\\d{2})(\\d{4})",
                 metadata.getNumberFormat(3).getPattern());
    assertEquals("(9)(\\d{4})(\\d{2})(\\d{4})",
                 metadata.getIntlNumberFormat(3).getPattern());
    assertEquals("$1 $2 $3 $4", metadata.getIntlNumberFormat(3).getFormat());
  }

  public void testGetInstanceLoadInternationalTollFreeMetadata() {
    PhoneMetadata metadata = phoneUtil.getMetadataForNonGeographicalRegion(800);
    assertEquals("001", metadata.getId());
    assertEquals(800, metadata.getCountryCode());
    assertEquals("$1 $2", metadata.getNumberFormat(0).getFormat());
    assertEquals("(\\d{4})(\\d{4})", metadata.getNumberFormat(0).getPattern());
    assertEquals("12345678", metadata.getGeneralDesc().getExampleNumber());
    assertEquals("12345678", metadata.getTollFree().getExampleNumber());
  }

  public void testIsLeadingZeroPossible() {
    assertTrue(phoneUtil.isLeadingZeroPossible(39));  // Italy
    assertFalse(phoneUtil.isLeadingZeroPossible(1));  // USA
    assertFalse(phoneUtil.isLeadingZeroPossible(800));  // International toll free numbers
    assertFalse(phoneUtil.isLeadingZeroPossible(888));  // Not in metadata file, just default to
                                                        // false.
  }

  public void testGetLengthOfGeographicalAreaCode() {
    // Google MTV, which has area code "650".
    assertEquals(3, phoneUtil.getLengthOfGeographicalAreaCode(US_NUMBER));

    // A North America toll-free number, which has no area code.
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(US_TOLLFREE));

    // Google London, which has area code "20".
    assertEquals(2, phoneUtil.getLengthOfGeographicalAreaCode(GB_NUMBER));

    // A UK mobile phone, which has no area code.
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(GB_MOBILE));

    // Google Buenos Aires, which has area code "11".
    assertEquals(2, phoneUtil.getLengthOfGeographicalAreaCode(AR_NUMBER));

    // Google Sydney, which has area code "2".
    assertEquals(1, phoneUtil.getLengthOfGeographicalAreaCode(AU_NUMBER));

    // Google Singapore. Singapore has no area code and no national prefix.
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(SG_NUMBER));

    // An invalid US number (1 digit shorter), which has no area code.
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(US_SHORT_BY_ONE_NUMBER));

    // An international toll free number, which has no area code.
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(INTERNATIONAL_TOLL_FREE));
  }

  public void testGetLengthOfNationalDestinationCode() {
    // Google MTV, which has national destination code (NDC) "650".
    assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(US_NUMBER));

    // A North America toll-free number, which has NDC "800".
    assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(US_TOLLFREE));

    // Google London, which has NDC "20".
    assertEquals(2, phoneUtil.getLengthOfNationalDestinationCode(GB_NUMBER));

    // A UK mobile phone, which has NDC "7912".
    assertEquals(4, phoneUtil.getLengthOfNationalDestinationCode(GB_MOBILE));

    // Google Buenos Aires, which has NDC "11".
    assertEquals(2, phoneUtil.getLengthOfNationalDestinationCode(AR_NUMBER));

    // An Argentinian mobile which has NDC "911".
    assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(AR_MOBILE));

    // Google Sydney, which has NDC "2".
    assertEquals(1, phoneUtil.getLengthOfNationalDestinationCode(AU_NUMBER));

    // Google Singapore, which has NDC "6521".
    assertEquals(4, phoneUtil.getLengthOfNationalDestinationCode(SG_NUMBER));

    // An invalid US number (1 digit shorter), which has no NDC.
    assertEquals(0, phoneUtil.getLengthOfNationalDestinationCode(US_SHORT_BY_ONE_NUMBER));

    // A number containing an invalid country calling code, which shouldn't have any NDC.
    PhoneNumber number = new PhoneNumber().setCountryCode(123).setNationalNumber(6502530000L);
    assertEquals(0, phoneUtil.getLengthOfNationalDestinationCode(number));

    // An international toll free number, which has NDC "1234".
    assertEquals(4, phoneUtil.getLengthOfNationalDestinationCode(INTERNATIONAL_TOLL_FREE));
  }

  public void testGetNationalSignificantNumber() {
    assertEquals("6502530000", phoneUtil.getNationalSignificantNumber(US_NUMBER));

    // An Italian mobile number.
    assertEquals("345678901", phoneUtil.getNationalSignificantNumber(IT_MOBILE));

    // An Italian fixed line number.
    assertEquals("0236618300", phoneUtil.getNationalSignificantNumber(IT_NUMBER));

    assertEquals("12345678", phoneUtil.getNationalSignificantNumber(INTERNATIONAL_TOLL_FREE));
  }

  public void testGetExampleNumber() {
    assertEquals(DE_NUMBER, phoneUtil.getExampleNumber(RegionCode.DE));

    assertEquals(DE_NUMBER,
                 phoneUtil.getExampleNumberForType(RegionCode.DE,
                                                   PhoneNumberUtil.PhoneNumberType.FIXED_LINE));
    assertEquals(null,
                 phoneUtil.getExampleNumberForType(RegionCode.DE,
                                                   PhoneNumberUtil.PhoneNumberType.MOBILE));
    // For the US, the example number is placed under general description, and hence should be used
    // for both fixed line and mobile, so neither of these should return null.
    assertNotNull(phoneUtil.getExampleNumberForType(RegionCode.US,
                                                    PhoneNumberUtil.PhoneNumberType.FIXED_LINE));
    assertNotNull(phoneUtil.getExampleNumberForType(RegionCode.US,
                                                    PhoneNumberUtil.PhoneNumberType.MOBILE));
    // CS is an invalid region, so we have no data for it.
    assertNull(phoneUtil.getExampleNumberForType(RegionCode.CS,
                                                 PhoneNumberUtil.PhoneNumberType.MOBILE));
    // RegionCode 001 is reserved for supporting non-geographical country calling code. We don't
    // support getting an example number for it with this method.
    assertEquals(null, phoneUtil.getExampleNumber(RegionCode.UN001));
  }

  public void testGetExampleNumberForNonGeoEntity() {
    assertEquals(INTERNATIONAL_TOLL_FREE, phoneUtil.getExampleNumberForNonGeoEntity(800));
  }

  public void testConvertAlphaCharactersInNumber() {
    String input = "1800-ABC-DEF";
    // Alpha chars are converted to digits; everything else is left untouched.
    String expectedOutput = "1800-222-333";
    assertEquals(expectedOutput, PhoneNumberUtil.convertAlphaCharactersInNumber(input));
  }

  public void testNormaliseRemovePunctuation() {
    String inputNumber = "034-56&+#234";
    String expectedOutput = "03456234";
    assertEquals("Conversion did not correctly remove punctuation",
                 expectedOutput,
                 PhoneNumberUtil.normalize(inputNumber));
  }

  public void testNormaliseReplaceAlphaCharacters() {
    String inputNumber = "034-I-am-HUNGRY";
    String expectedOutput = "034426486479";
    assertEquals("Conversion did not correctly replace alpha characters",
                 expectedOutput,
                 PhoneNumberUtil.normalize(inputNumber));
  }

  public void testNormaliseOtherDigits() {
    String inputNumber = "\uFF125\u0665";
    String expectedOutput = "255";
    assertEquals("Conversion did not correctly replace non-latin digits",
                 expectedOutput,
                 PhoneNumberUtil.normalize(inputNumber));
    // Eastern-Arabic digits.
    inputNumber = "\u06F52\u06F0";
    expectedOutput = "520";
    assertEquals("Conversion did not correctly replace non-latin digits",
                 expectedOutput,
                 PhoneNumberUtil.normalize(inputNumber));
  }

  public void testNormaliseStripAlphaCharacters() {
    String inputNumber = "034-56&+a#234";
    String expectedOutput = "03456234";
    assertEquals("Conversion did not correctly remove alpha character",
                 expectedOutput,
                 PhoneNumberUtil.normalizeDigitsOnly(inputNumber));
  }

  public void testFormatUSNumber() {
    assertEquals("650 253 0000", phoneUtil.format(US_NUMBER, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 650 253 0000", phoneUtil.format(US_NUMBER, PhoneNumberFormat.INTERNATIONAL));

    assertEquals("800 253 0000", phoneUtil.format(US_TOLLFREE, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 800 253 0000", phoneUtil.format(US_TOLLFREE, PhoneNumberFormat.INTERNATIONAL));

    assertEquals("900 253 0000", phoneUtil.format(US_PREMIUM, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 900 253 0000", phoneUtil.format(US_PREMIUM, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+1-900-253-0000", phoneUtil.format(US_PREMIUM, PhoneNumberFormat.RFC3966));
    // Numbers with all zeros in the national number part will be formatted by using the raw_input
    // if that is available no matter which format is specified.
    assertEquals("000-000-0000",
                 phoneUtil.format(US_SPOOF_WITH_RAW_INPUT, PhoneNumberFormat.NATIONAL));
    assertEquals("0", phoneUtil.format(US_SPOOF, PhoneNumberFormat.NATIONAL));
  }

  public void testFormatBSNumber() {
    assertEquals("242 365 1234", phoneUtil.format(BS_NUMBER, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 242 365 1234", phoneUtil.format(BS_NUMBER, PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatGBNumber() {
    assertEquals("(020) 7031 3000", phoneUtil.format(GB_NUMBER, PhoneNumberFormat.NATIONAL));
    assertEquals("+44 20 7031 3000", phoneUtil.format(GB_NUMBER, PhoneNumberFormat.INTERNATIONAL));

    assertEquals("(07912) 345 678", phoneUtil.format(GB_MOBILE, PhoneNumberFormat.NATIONAL));
    assertEquals("+44 7912 345 678", phoneUtil.format(GB_MOBILE, PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatDENumber() {
    PhoneNumber deNumber = new PhoneNumber();
    deNumber.setCountryCode(49).setNationalNumber(301234L);
    assertEquals("030/1234", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 30/1234", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+49-30-1234", phoneUtil.format(deNumber, PhoneNumberFormat.RFC3966));

    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(291123L);
    assertEquals("0291 123", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 291 123", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));

    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(29112345678L);
    assertEquals("0291 12345678", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 291 12345678", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));

    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(912312345L);
    assertEquals("09123 12345", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 9123 12345", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));
    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(80212345L);
    assertEquals("08021 2345", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 8021 2345", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));
    // Note this number is correctly formatted without national prefix. Most of the numbers that
    // are treated as invalid numbers by the library are short numbers, and they are usually not
    // dialed with national prefix.
    assertEquals("1234", phoneUtil.format(DE_SHORT_NUMBER, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 1234", phoneUtil.format(DE_SHORT_NUMBER, PhoneNumberFormat.INTERNATIONAL));

    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(41341234);
    assertEquals("04134 1234", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
  }

  public void testFormatITNumber() {
    assertEquals("02 3661 8300", phoneUtil.format(IT_NUMBER, PhoneNumberFormat.NATIONAL));
    assertEquals("+39 02 3661 8300", phoneUtil.format(IT_NUMBER, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+390236618300", phoneUtil.format(IT_NUMBER, PhoneNumberFormat.E164));

    assertEquals("345 678 901", phoneUtil.format(IT_MOBILE, PhoneNumberFormat.NATIONAL));
    assertEquals("+39 345 678 901", phoneUtil.format(IT_MOBILE, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+39345678901", phoneUtil.format(IT_MOBILE, PhoneNumberFormat.E164));
  }

  public void testFormatAUNumber() {
    assertEquals("02 3661 8300", phoneUtil.format(AU_NUMBER, PhoneNumberFormat.NATIONAL));
    assertEquals("+61 2 3661 8300", phoneUtil.format(AU_NUMBER, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+61236618300", phoneUtil.format(AU_NUMBER, PhoneNumberFormat.E164));

    PhoneNumber auNumber = new PhoneNumber().setCountryCode(61).setNationalNumber(1800123456L);
    assertEquals("1800 123 456", phoneUtil.format(auNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+61 1800 123 456", phoneUtil.format(auNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+611800123456", phoneUtil.format(auNumber, PhoneNumberFormat.E164));
  }

  public void testFormatARNumber() {
    assertEquals("011 8765-4321", phoneUtil.format(AR_NUMBER, PhoneNumberFormat.NATIONAL));
    assertEquals("+54 11 8765-4321", phoneUtil.format(AR_NUMBER, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+541187654321", phoneUtil.format(AR_NUMBER, PhoneNumberFormat.E164));

    assertEquals("011 15 8765-4321", phoneUtil.format(AR_MOBILE, PhoneNumberFormat.NATIONAL));
    assertEquals("+54 9 11 8765 4321", phoneUtil.format(AR_MOBILE,
                                                        PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+5491187654321", phoneUtil.format(AR_MOBILE, PhoneNumberFormat.E164));
  }

  public void testFormatMXNumber() {
    assertEquals("045 234 567 8900", phoneUtil.format(MX_MOBILE1, PhoneNumberFormat.NATIONAL));
    assertEquals("+52 1 234 567 8900", phoneUtil.format(
        MX_MOBILE1, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+5212345678900", phoneUtil.format(MX_MOBILE1, PhoneNumberFormat.E164));

    assertEquals("045 55 1234 5678", phoneUtil.format(MX_MOBILE2, PhoneNumberFormat.NATIONAL));
    assertEquals("+52 1 55 1234 5678", phoneUtil.format(
        MX_MOBILE2, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+5215512345678", phoneUtil.format(MX_MOBILE2, PhoneNumberFormat.E164));

    assertEquals("01 33 1234 5678", phoneUtil.format(MX_NUMBER1, PhoneNumberFormat.NATIONAL));
    assertEquals("+52 33 1234 5678", phoneUtil.format(MX_NUMBER1, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+523312345678", phoneUtil.format(MX_NUMBER1, PhoneNumberFormat.E164));

    assertEquals("01 821 123 4567", phoneUtil.format(MX_NUMBER2, PhoneNumberFormat.NATIONAL));
    assertEquals("+52 821 123 4567", phoneUtil.format(MX_NUMBER2, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+528211234567", phoneUtil.format(MX_NUMBER2, PhoneNumberFormat.E164));
  }

  public void testFormatOutOfCountryCallingNumber() {
    assertEquals("00 1 900 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(US_PREMIUM, RegionCode.DE));

    assertEquals("1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(US_NUMBER, RegionCode.BS));

    assertEquals("00 1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(US_NUMBER, RegionCode.PL));

    assertEquals("011 44 7912 345 678",
                 phoneUtil.formatOutOfCountryCallingNumber(GB_MOBILE, RegionCode.US));

    assertEquals("00 49 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(DE_SHORT_NUMBER, RegionCode.GB));
    // Note this number is correctly formatted without national prefix. Most of the numbers that
    // are treated as invalid numbers by the library are short numbers, and they are usually not
    // dialed with national prefix.
    assertEquals("1234", phoneUtil.formatOutOfCountryCallingNumber(DE_SHORT_NUMBER, RegionCode.DE));

    assertEquals("011 39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(IT_NUMBER, RegionCode.US));
    assertEquals("02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(IT_NUMBER, RegionCode.IT));
    assertEquals("+39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(IT_NUMBER, RegionCode.SG));

    assertEquals("6521 8000",
                 phoneUtil.formatOutOfCountryCallingNumber(SG_NUMBER, RegionCode.SG));

    assertEquals("011 54 9 11 8765 4321",
                 phoneUtil.formatOutOfCountryCallingNumber(AR_MOBILE, RegionCode.US));
    assertEquals("011 800 1234 5678",
                 phoneUtil.formatOutOfCountryCallingNumber(INTERNATIONAL_TOLL_FREE, RegionCode.US));

    PhoneNumber arNumberWithExtn = new PhoneNumber().mergeFrom(AR_MOBILE).setExtension("1234");
    assertEquals("011 54 9 11 8765 4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumberWithExtn, RegionCode.US));
    assertEquals("0011 54 9 11 8765 4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumberWithExtn, RegionCode.AU));
    assertEquals("011 15 8765-4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumberWithExtn, RegionCode.AR));
  }

  public void testFormatOutOfCountryWithInvalidRegion() {
    // AQ/Antarctica isn't a valid region code for phone number formatting,
    // so this falls back to intl formatting.
    assertEquals("+1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(US_NUMBER, RegionCode.AQ));
    // For region code 001, the out-of-country format always turns into the international format.
    assertEquals("+1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(US_NUMBER, RegionCode.UN001));
  }

  public void testFormatOutOfCountryWithPreferredIntlPrefix() {
    // This should use 0011, since that is the preferred international prefix (both 0011 and 0012
    // are accepted as possible international prefixes in our test metadta.)
    assertEquals("0011 39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(IT_NUMBER, RegionCode.AU));
  }

  public void testFormatOutOfCountryKeepingAlphaChars() {
    PhoneNumber alphaNumericNumber = new PhoneNumber();
    alphaNumericNumber.setCountryCode(1).setNationalNumber(8007493524L)
        .setRawInput("1800 six-flag");
    assertEquals("0011 1 800 SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AU));

    alphaNumericNumber.setRawInput("1-800-SIX-flag");
    assertEquals("0011 1 800-SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AU));

    alphaNumericNumber.setRawInput("Call us from UK: 00 1 800 SIX-flag");
    assertEquals("0011 1 800 SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AU));

    alphaNumericNumber.setRawInput("800 SIX-flag");
    assertEquals("0011 1 800 SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AU));

    // Formatting from within the NANPA region.
    assertEquals("1 800 SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.US));

    assertEquals("1 800 SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.BS));

    // Testing that if the raw input doesn't exist, it is formatted using
    // formatOutOfCountryCallingNumber.
    alphaNumericNumber.clearRawInput();
    assertEquals("00 1 800 749 3524",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.DE));

    // Testing AU alpha number formatted from Australia.
    alphaNumericNumber.setCountryCode(61).setNationalNumber(827493524L)
        .setRawInput("+61 82749-FLAG");
    // This number should have the national prefix fixed.
    assertEquals("082749-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AU));

    alphaNumericNumber.setRawInput("082749-FLAG");
    assertEquals("082749-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AU));

    alphaNumericNumber.setNationalNumber(18007493524L).setRawInput("1-800-SIX-flag");
    // This number should not have the national prefix prefixed, in accordance with the override for
    // this specific formatting rule.
    assertEquals("1-800-SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AU));

    // The metadata should not be permanently changed, since we copied it before modifying patterns.
    // Here we check this.
    alphaNumericNumber.setNationalNumber(1800749352L);
    assertEquals("1800 749 352",
                 phoneUtil.formatOutOfCountryCallingNumber(alphaNumericNumber, RegionCode.AU));

    // Testing a region with multiple international prefixes.
    assertEquals("+61 1-800-SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.SG));
    // Testing the case of calling from a non-supported region.
    assertEquals("+61 1-800-SIX-FLAG",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AQ));

    // Testing the case with an invalid country calling code.
    alphaNumericNumber.setCountryCode(0).setNationalNumber(18007493524L)
        .setRawInput("1-800-SIX-flag");
    // Uses the raw input only.
    assertEquals("1-800-SIX-flag",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.DE));

    // Testing the case of an invalid alpha number.
    alphaNumericNumber.setCountryCode(1).setNationalNumber(80749L).setRawInput("180-SIX");
    // No country-code stripping can be done.
    assertEquals("00 1 180-SIX",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.DE));

    // Testing the case of calling from a non-supported region.
    alphaNumericNumber.setCountryCode(1).setNationalNumber(80749L).setRawInput("180-SIX");
    // No country-code stripping can be done since the number is invalid.
    assertEquals("+1 180-SIX",
                 phoneUtil.formatOutOfCountryKeepingAlphaChars(alphaNumericNumber, RegionCode.AQ));
  }

  public void testFormatWithCarrierCode() {
    // We only support this for AR in our test metadata, and only for mobile numbers starting with
    // certain values.
    PhoneNumber arMobile = new PhoneNumber().setCountryCode(54).setNationalNumber(92234654321L);
    assertEquals("02234 65-4321", phoneUtil.format(arMobile, PhoneNumberFormat.NATIONAL));
    // Here we force 14 as the carrier code.
    assertEquals("02234 14 65-4321",
                 phoneUtil.formatNationalNumberWithCarrierCode(arMobile, "14"));
    // Here we force the number to be shown with no carrier code.
    assertEquals("02234 65-4321",
                 phoneUtil.formatNationalNumberWithCarrierCode(arMobile, ""));
    // Here the international rule is used, so no carrier code should be present.
    assertEquals("+5492234654321", phoneUtil.format(arMobile, PhoneNumberFormat.E164));
    // We don't support this for the US so there should be no change.
    assertEquals("650 253 0000", phoneUtil.formatNationalNumberWithCarrierCode(US_NUMBER, "15"));
  }

  public void testFormatWithPreferredCarrierCode() {
    // We only support this for AR in our test metadata.
    PhoneNumber arNumber = new PhoneNumber();
    arNumber.setCountryCode(54).setNationalNumber(91234125678L);
    // Test formatting with no preferred carrier code stored in the number itself.
    assertEquals("01234 15 12-5678",
        phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, "15"));
    assertEquals("01234 12-5678",
        phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, ""));
    // Test formatting with preferred carrier code present.
    arNumber.setPreferredDomesticCarrierCode("19");
    assertEquals("01234 12-5678", phoneUtil.format(arNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("01234 19 12-5678",
        phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, "15"));
    assertEquals("01234 19 12-5678",
        phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, ""));
    // When the preferred_domestic_carrier_code is present (even when it contains an empty string),
    // use it instead of the default carrier code passed in.
    arNumber.setPreferredDomesticCarrierCode("");
    assertEquals("01234 12-5678",
        phoneUtil.formatNationalNumberWithPreferredCarrierCode(arNumber, "15"));
    // We don't support this for the US so there should be no change.
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(4241231234L).setPreferredDomesticCarrierCode("99");
    assertEquals("424 123 1234", phoneUtil.format(usNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("424 123 1234",
        phoneUtil.formatNationalNumberWithPreferredCarrierCode(usNumber, "15"));
  }

  public void testFormatNumberForMobileDialing() {
    // US toll free numbers are marked as noInternationalDialling in the test metadata for testing
    // purposes.
    assertEquals("800 253 0000",
        phoneUtil.formatNumberForMobileDialing(US_TOLLFREE, RegionCode.US,
                                               true /*  keep formatting */));
    assertEquals("", phoneUtil.formatNumberForMobileDialing(US_TOLLFREE, RegionCode.CN, true));
    assertEquals("+1 650 253 0000",
        phoneUtil.formatNumberForMobileDialing(US_NUMBER, RegionCode.US, true));
    PhoneNumber usNumberWithExtn = new PhoneNumber().mergeFrom(US_NUMBER).setExtension("1234");
    assertEquals("+1 650 253 0000",
        phoneUtil.formatNumberForMobileDialing(usNumberWithExtn, RegionCode.US, true));

    assertEquals("8002530000",
        phoneUtil.formatNumberForMobileDialing(US_TOLLFREE, RegionCode.US,
                                               false /* remove formatting */));
    assertEquals("", phoneUtil.formatNumberForMobileDialing(US_TOLLFREE, RegionCode.CN, false));
    assertEquals("+16502530000",
        phoneUtil.formatNumberForMobileDialing(US_NUMBER, RegionCode.US, false));
    assertEquals("+16502530000",
        phoneUtil.formatNumberForMobileDialing(usNumberWithExtn, RegionCode.US, false));

    // An invalid US number, which is one digit too long.
    assertEquals("+165025300001",
        phoneUtil.formatNumberForMobileDialing(US_LONG_NUMBER, RegionCode.US, false));
    assertEquals("+1 65025300001",
        phoneUtil.formatNumberForMobileDialing(US_LONG_NUMBER, RegionCode.US, true));

    // Star numbers. In real life they appear in Israel, but we have them in JP in our test
    // metadata.
    assertEquals("*2345",
        phoneUtil.formatNumberForMobileDialing(JP_STAR_NUMBER, RegionCode.JP, false));
    assertEquals("*2345",
        phoneUtil.formatNumberForMobileDialing(JP_STAR_NUMBER, RegionCode.JP, true));

    assertEquals("+80012345678",
        phoneUtil.formatNumberForMobileDialing(INTERNATIONAL_TOLL_FREE, RegionCode.JP, false));
    assertEquals("+800 1234 5678",
        phoneUtil.formatNumberForMobileDialing(INTERNATIONAL_TOLL_FREE, RegionCode.JP, true));
  }

  public void testFormatByPattern() {
    NumberFormat newNumFormat = new NumberFormat();
    newNumFormat.setPattern("(\\d{3})(\\d{3})(\\d{4})");
    newNumFormat.setFormat("($1) $2-$3");
    List<NumberFormat> newNumberFormats = new ArrayList<NumberFormat>();
    newNumberFormats.add(newNumFormat);

    assertEquals("(650) 253-0000", phoneUtil.formatByPattern(US_NUMBER, PhoneNumberFormat.NATIONAL,
                                                             newNumberFormats));
    assertEquals("+1 (650) 253-0000", phoneUtil.formatByPattern(US_NUMBER,
                                                                PhoneNumberFormat.INTERNATIONAL,
                                                                newNumberFormats));

    // $NP is set to '1' for the US. Here we check that for other NANPA countries the US rules are
    // followed.
    newNumFormat.setNationalPrefixFormattingRule("$NP ($FG)");
    newNumFormat.setFormat("$1 $2-$3");
    assertEquals("1 (242) 365-1234",
                 phoneUtil.formatByPattern(BS_NUMBER, PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));
    assertEquals("+1 242 365-1234",
                 phoneUtil.formatByPattern(BS_NUMBER, PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));

    newNumFormat.setPattern("(\\d{2})(\\d{5})(\\d{3})");
    newNumFormat.setFormat("$1-$2 $3");
    newNumberFormats.set(0, newNumFormat);

    assertEquals("02-36618 300",
                 phoneUtil.formatByPattern(IT_NUMBER, PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));
    assertEquals("+39 02-36618 300",
                 phoneUtil.formatByPattern(IT_NUMBER, PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));

    newNumFormat.setNationalPrefixFormattingRule("$NP$FG");
    newNumFormat.setPattern("(\\d{2})(\\d{4})(\\d{4})");
    newNumFormat.setFormat("$1 $2 $3");
    newNumberFormats.set(0, newNumFormat);
    assertEquals("020 7031 3000",
                 phoneUtil.formatByPattern(GB_NUMBER, PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));

    newNumFormat.setNationalPrefixFormattingRule("($NP$FG)");
    assertEquals("(020) 7031 3000",
                 phoneUtil.formatByPattern(GB_NUMBER, PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));

    newNumFormat.setNationalPrefixFormattingRule("");
    assertEquals("20 7031 3000",
                 phoneUtil.formatByPattern(GB_NUMBER, PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));

    assertEquals("+44 20 7031 3000",
                 phoneUtil.formatByPattern(GB_NUMBER, PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));
  }

  public void testFormatE164Number() {
    assertEquals("+16502530000", phoneUtil.format(US_NUMBER, PhoneNumberFormat.E164));
    assertEquals("+4930123456", phoneUtil.format(DE_NUMBER, PhoneNumberFormat.E164));
    assertEquals("+80012345678", phoneUtil.format(INTERNATIONAL_TOLL_FREE, PhoneNumberFormat.E164));
  }

  public void testFormatNumberWithExtension() {
    PhoneNumber nzNumber = new PhoneNumber().mergeFrom(NZ_NUMBER).setExtension("1234");
    // Uses default extension prefix:
    assertEquals("03-331 6005 ext. 1234", phoneUtil.format(nzNumber, PhoneNumberFormat.NATIONAL));
    // Uses RFC 3966 syntax.
    assertEquals("+64-3-331-6005;ext=1234", phoneUtil.format(nzNumber, PhoneNumberFormat.RFC3966));
    // Extension prefix overridden in the territory information for the US:
    PhoneNumber usNumberWithExtension = new PhoneNumber().mergeFrom(US_NUMBER).setExtension("4567");
    assertEquals("650 253 0000 extn. 4567", phoneUtil.format(usNumberWithExtension,
                                                             PhoneNumberFormat.NATIONAL));
  }

  public void testFormatInOriginalFormat() throws Exception {
    PhoneNumber number1 = phoneUtil.parseAndKeepRawInput("+442087654321", RegionCode.GB);
    assertEquals("+44 20 8765 4321", phoneUtil.formatInOriginalFormat(number1, RegionCode.GB));

    PhoneNumber number2 = phoneUtil.parseAndKeepRawInput("02087654321", RegionCode.GB);
    assertEquals("(020) 8765 4321", phoneUtil.formatInOriginalFormat(number2, RegionCode.GB));

    PhoneNumber number3 = phoneUtil.parseAndKeepRawInput("011442087654321", RegionCode.US);
    assertEquals("011 44 20 8765 4321", phoneUtil.formatInOriginalFormat(number3, RegionCode.US));

    PhoneNumber number4 = phoneUtil.parseAndKeepRawInput("442087654321", RegionCode.GB);
    assertEquals("44 20 8765 4321", phoneUtil.formatInOriginalFormat(number4, RegionCode.GB));

    PhoneNumber number5 = phoneUtil.parse("+442087654321", RegionCode.GB);
    assertEquals("(020) 8765 4321", phoneUtil.formatInOriginalFormat(number5, RegionCode.GB));

    // Invalid numbers that we have a formatting pattern for should be formatted properly. Note area
    // codes starting with 7 are intentionally excluded in the test metadata for testing purposes.
    PhoneNumber number6 = phoneUtil.parseAndKeepRawInput("7345678901", RegionCode.US);
    assertEquals("734 567 8901", phoneUtil.formatInOriginalFormat(number6, RegionCode.US));

    // US is not a leading zero country, and the presence of the leading zero leads us to format the
    // number using raw_input.
    PhoneNumber number7 = phoneUtil.parseAndKeepRawInput("0734567 8901", RegionCode.US);
    assertEquals("0734567 8901", phoneUtil.formatInOriginalFormat(number7, RegionCode.US));

    // This number is valid, but we don't have a formatting pattern for it. Fall back to the raw
    // input.
    PhoneNumber number8 = phoneUtil.parseAndKeepRawInput("02-4567-8900", RegionCode.KR);
    assertEquals("02-4567-8900", phoneUtil.formatInOriginalFormat(number8, RegionCode.KR));

    PhoneNumber number9 = phoneUtil.parseAndKeepRawInput("01180012345678", RegionCode.US);
    assertEquals("011 800 1234 5678", phoneUtil.formatInOriginalFormat(number9, RegionCode.US));

    PhoneNumber number10 = phoneUtil.parseAndKeepRawInput("+80012345678", RegionCode.KR);
    assertEquals("+800 1234 5678", phoneUtil.formatInOriginalFormat(number10, RegionCode.KR));

    // US local numbers are formatted correctly, as we have formatting patterns for them.
    PhoneNumber localNumberUS = phoneUtil.parseAndKeepRawInput("2530000", RegionCode.US);
    assertEquals("253 0000", phoneUtil.formatInOriginalFormat(localNumberUS, RegionCode.US));

    PhoneNumber numberWithNationalPrefixUS =
        phoneUtil.parseAndKeepRawInput("18003456789", RegionCode.US);
    assertEquals("1 800 345 6789",
        phoneUtil.formatInOriginalFormat(numberWithNationalPrefixUS, RegionCode.US));

    PhoneNumber numberWithoutNationalPrefixGB =
        phoneUtil.parseAndKeepRawInput("2087654321", RegionCode.GB);
    assertEquals("20 8765 4321",
        phoneUtil.formatInOriginalFormat(numberWithoutNationalPrefixGB, RegionCode.GB));
    // Make sure no metadata is modified as a result of the previous function call.
    assertEquals("(020) 8765 4321", phoneUtil.formatInOriginalFormat(number5, RegionCode.GB));

    PhoneNumber numberWithNationalPrefixMX =
        phoneUtil.parseAndKeepRawInput("013312345678", RegionCode.MX);
    assertEquals("01 33 1234 5678",
        phoneUtil.formatInOriginalFormat(numberWithNationalPrefixMX, RegionCode.MX));

    PhoneNumber numberWithoutNationalPrefixMX =
        phoneUtil.parseAndKeepRawInput("3312345678", RegionCode.MX);
    assertEquals("33 1234 5678",
        phoneUtil.formatInOriginalFormat(numberWithoutNationalPrefixMX, RegionCode.MX));

    PhoneNumber italianFixedLineNumber =
        phoneUtil.parseAndKeepRawInput("0212345678", RegionCode.IT);
    assertEquals("02 1234 5678",
        phoneUtil.formatInOriginalFormat(italianFixedLineNumber, RegionCode.IT));

    PhoneNumber numberWithNationalPrefixJP =
        phoneUtil.parseAndKeepRawInput("00777012", RegionCode.JP);
    assertEquals("0077-7012",
        phoneUtil.formatInOriginalFormat(numberWithNationalPrefixJP, RegionCode.JP));

    PhoneNumber numberWithoutNationalPrefixJP =
        phoneUtil.parseAndKeepRawInput("0777012", RegionCode.JP);
    assertEquals("0777012",
        phoneUtil.formatInOriginalFormat(numberWithoutNationalPrefixJP, RegionCode.JP));

    PhoneNumber numberWithCarrierCodeBR =
        phoneUtil.parseAndKeepRawInput("012 3121286979", RegionCode.BR);
    assertEquals("012 3121286979",
        phoneUtil.formatInOriginalFormat(numberWithCarrierCodeBR, RegionCode.BR));

    // The default national prefix used in this case is 045. When a number with national prefix 044
    // is entered, we return the raw input as we don't want to change the number entered.
    PhoneNumber numberWithNationalPrefixMX1 =
        phoneUtil.parseAndKeepRawInput("044(33)1234-5678", RegionCode.MX);
    assertEquals("044(33)1234-5678",
        phoneUtil.formatInOriginalFormat(numberWithNationalPrefixMX1, RegionCode.MX));

    PhoneNumber numberWithNationalPrefixMX2 =
        phoneUtil.parseAndKeepRawInput("045(33)1234-5678", RegionCode.MX);
    assertEquals("045 33 1234 5678",
        phoneUtil.formatInOriginalFormat(numberWithNationalPrefixMX2, RegionCode.MX));

    // The default international prefix used in this case is 0011. When a number with international
    // prefix 0012 is entered, we return the raw input as we don't want to change the number
    // entered.
    PhoneNumber outOfCountryNumberFromAU1 =
        phoneUtil.parseAndKeepRawInput("0012 16502530000", RegionCode.AU);
    assertEquals("0012 16502530000",
        phoneUtil.formatInOriginalFormat(outOfCountryNumberFromAU1, RegionCode.AU));

    PhoneNumber outOfCountryNumberFromAU2 =
        phoneUtil.parseAndKeepRawInput("0011 16502530000", RegionCode.AU);
    assertEquals("0011 1 650 253 0000",
        phoneUtil.formatInOriginalFormat(outOfCountryNumberFromAU2, RegionCode.AU));
  }

  public void testIsPremiumRate() {
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE, phoneUtil.getNumberType(US_PREMIUM));

    PhoneNumber premiumRateNumber = new PhoneNumber();
    premiumRateNumber.setCountryCode(39).setNationalNumber(892123L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber));

    premiumRateNumber.clear();
    premiumRateNumber.setCountryCode(44).setNationalNumber(9187654321L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber));

    premiumRateNumber.clear();
    premiumRateNumber.setCountryCode(49).setNationalNumber(9001654321L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber));

    premiumRateNumber.clear();
    premiumRateNumber.setCountryCode(49).setNationalNumber(90091234567L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber));
  }

  public void testIsTollFree() {
    PhoneNumber tollFreeNumber = new PhoneNumber();

    tollFreeNumber.setCountryCode(1).setNationalNumber(8881234567L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(tollFreeNumber));

    tollFreeNumber.clear();
    tollFreeNumber.setCountryCode(39).setNationalNumber(803123L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(tollFreeNumber));

    tollFreeNumber.clear();
    tollFreeNumber.setCountryCode(44).setNationalNumber(8012345678L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(tollFreeNumber));

    tollFreeNumber.clear();
    tollFreeNumber.setCountryCode(49).setNationalNumber(8001234567L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(tollFreeNumber));

    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(INTERNATIONAL_TOLL_FREE));
  }

  public void testIsMobile() {
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE, phoneUtil.getNumberType(BS_MOBILE));
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE, phoneUtil.getNumberType(GB_MOBILE));
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE, phoneUtil.getNumberType(IT_MOBILE));
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE, phoneUtil.getNumberType(AR_MOBILE));

    PhoneNumber mobileNumber = new PhoneNumber();
    mobileNumber.setCountryCode(49).setNationalNumber(15123456789L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE, phoneUtil.getNumberType(mobileNumber));
  }

  public void testIsFixedLine() {
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE, phoneUtil.getNumberType(BS_NUMBER));
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE, phoneUtil.getNumberType(IT_NUMBER));
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE, phoneUtil.getNumberType(GB_NUMBER));
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE, phoneUtil.getNumberType(DE_NUMBER));
  }

  public void testIsFixedLineAndMobile() {
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE,
                 phoneUtil.getNumberType(US_NUMBER));

    PhoneNumber fixedLineAndMobileNumber = new PhoneNumber().
        setCountryCode(54).setNationalNumber(1987654321L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE,
                 phoneUtil.getNumberType(fixedLineAndMobileNumber));
  }

  public void testIsSharedCost() {
    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(8431231234L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.SHARED_COST, phoneUtil.getNumberType(gbNumber));
  }

  public void testIsVoip() {
    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(5631231234L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.VOIP, phoneUtil.getNumberType(gbNumber));
  }

  public void testIsPersonalNumber() {
    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(7031231234L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.PERSONAL_NUMBER,
                 phoneUtil.getNumberType(gbNumber));
  }

  public void testIsUnknown() {
    // Invalid numbers should be of type UNKNOWN.
    assertEquals(PhoneNumberUtil.PhoneNumberType.UNKNOWN, phoneUtil.getNumberType(US_LOCAL_NUMBER));
  }

  public void testIsValidNumber() {
    assertTrue(phoneUtil.isValidNumber(US_NUMBER));
    assertTrue(phoneUtil.isValidNumber(IT_NUMBER));
    assertTrue(phoneUtil.isValidNumber(GB_MOBILE));
    assertTrue(phoneUtil.isValidNumber(INTERNATIONAL_TOLL_FREE));

    PhoneNumber nzNumber = new PhoneNumber().setCountryCode(64).setNationalNumber(21387835L);
    assertTrue(phoneUtil.isValidNumber(nzNumber));
  }

  public void testIsValidForRegion() {
    // This number is valid for the Bahamas, but is not a valid US number.
    assertTrue(phoneUtil.isValidNumber(BS_NUMBER));
    assertTrue(phoneUtil.isValidNumberForRegion(BS_NUMBER, RegionCode.BS));
    assertFalse(phoneUtil.isValidNumberForRegion(BS_NUMBER, RegionCode.US));
    PhoneNumber bsInvalidNumber =
        new PhoneNumber().setCountryCode(1).setNationalNumber(2421232345L);
    // This number is no longer valid.
    assertFalse(phoneUtil.isValidNumber(bsInvalidNumber));

    // La Mayotte and Reunion use 'leadingDigits' to differentiate them.
    PhoneNumber reNumber = new PhoneNumber();
    reNumber.setCountryCode(262).setNationalNumber(262123456L);
    assertTrue(phoneUtil.isValidNumber(reNumber));
    assertTrue(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.RE));
    assertFalse(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.YT));
    // Now change the number to be a number for La Mayotte.
    reNumber.setNationalNumber(269601234L);
    assertTrue(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.YT));
    assertFalse(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.RE));
    // This number is no longer valid for La Reunion.
    reNumber.setNationalNumber(269123456L);
    assertFalse(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.YT));
    assertFalse(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.RE));
    assertFalse(phoneUtil.isValidNumber(reNumber));
    // However, it should be recognised as from La Mayotte, since it is valid for this region.
    assertEquals(RegionCode.YT, phoneUtil.getRegionCodeForNumber(reNumber));
    // This number is valid in both places.
    reNumber.setNationalNumber(800123456L);
    assertTrue(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.YT));
    assertTrue(phoneUtil.isValidNumberForRegion(reNumber, RegionCode.RE));
    assertTrue(phoneUtil.isValidNumberForRegion(INTERNATIONAL_TOLL_FREE, RegionCode.UN001));
    assertFalse(phoneUtil.isValidNumberForRegion(INTERNATIONAL_TOLL_FREE, RegionCode.US));
  }

  public void testIsNotValidNumber() {
    assertFalse(phoneUtil.isValidNumber(US_LOCAL_NUMBER));

    PhoneNumber invalidNumber = new PhoneNumber();
    invalidNumber.setCountryCode(39).setNationalNumber(23661830000L).setItalianLeadingZero(true);
    assertFalse(phoneUtil.isValidNumber(invalidNumber));

    invalidNumber.clear();
    invalidNumber.setCountryCode(44).setNationalNumber(791234567L);
    assertFalse(phoneUtil.isValidNumber(invalidNumber));

    invalidNumber.clear();
    invalidNumber.setCountryCode(49).setNationalNumber(1234L);
    assertFalse(phoneUtil.isValidNumber(invalidNumber));

    invalidNumber.clear();
    invalidNumber.setCountryCode(64).setNationalNumber(3316005L);
    assertFalse(phoneUtil.isValidNumber(invalidNumber));

    assertFalse(phoneUtil.isValidNumber(INTERNATIONAL_TOLL_FREE_TOO_LONG));
  }

  public void testGetRegionCodeForCountryCode() {
    assertEquals(RegionCode.US, phoneUtil.getRegionCodeForCountryCode(1));
    assertEquals(RegionCode.GB, phoneUtil.getRegionCodeForCountryCode(44));
    assertEquals(RegionCode.DE, phoneUtil.getRegionCodeForCountryCode(49));
    assertEquals(RegionCode.UN001, phoneUtil.getRegionCodeForCountryCode(800));
  }

  public void testGetRegionCodeForNumber() {
    assertEquals(RegionCode.BS, phoneUtil.getRegionCodeForNumber(BS_NUMBER));
    assertEquals(RegionCode.US, phoneUtil.getRegionCodeForNumber(US_NUMBER));
    assertEquals(RegionCode.GB, phoneUtil.getRegionCodeForNumber(GB_MOBILE));
    assertEquals(RegionCode.UN001, phoneUtil.getRegionCodeForNumber(INTERNATIONAL_TOLL_FREE));
  }

  public void testGetCountryCodeForRegion() {
    assertEquals(1, phoneUtil.getCountryCodeForRegion(RegionCode.US));
    assertEquals(64, phoneUtil.getCountryCodeForRegion(RegionCode.NZ));
    assertEquals(0, phoneUtil.getCountryCodeForRegion(null));
    assertEquals(0, phoneUtil.getCountryCodeForRegion(RegionCode.ZZ));
    assertEquals(0, phoneUtil.getCountryCodeForRegion(RegionCode.UN001));
    // CS is already deprecated so the library doesn't support it.
    assertEquals(0, phoneUtil.getCountryCodeForRegion(RegionCode.CS));
  }

  public void testGetNationalDiallingPrefixForRegion() {
    assertEquals("1", phoneUtil.getNddPrefixForRegion(RegionCode.US, false));
    // Test non-main country to see it gets the national dialling prefix for the main country with
    // that country calling code.
    assertEquals("1", phoneUtil.getNddPrefixForRegion(RegionCode.BS, false));
    assertEquals("0", phoneUtil.getNddPrefixForRegion(RegionCode.NZ, false));
    // Test case with non digit in the national prefix.
    assertEquals("0~0", phoneUtil.getNddPrefixForRegion(RegionCode.AO, false));
    assertEquals("00", phoneUtil.getNddPrefixForRegion(RegionCode.AO, true));
    // Test cases with invalid regions.
    assertEquals(null, phoneUtil.getNddPrefixForRegion(null, false));
    assertEquals(null, phoneUtil.getNddPrefixForRegion(RegionCode.ZZ, false));
    assertEquals(null, phoneUtil.getNddPrefixForRegion(RegionCode.UN001, false));
    // CS is already deprecated so the library doesn't support it.
    assertEquals(null, phoneUtil.getNddPrefixForRegion(RegionCode.CS, false));
  }

  public void testIsNANPACountry() {
    assertTrue(phoneUtil.isNANPACountry(RegionCode.US));
    assertTrue(phoneUtil.isNANPACountry(RegionCode.BS));
    assertFalse(phoneUtil.isNANPACountry(RegionCode.DE));
    assertFalse(phoneUtil.isNANPACountry(RegionCode.ZZ));
    assertFalse(phoneUtil.isNANPACountry(RegionCode.UN001));
    assertFalse(phoneUtil.isNANPACountry(null));
  }

  public void testIsPossibleNumber() {
    assertTrue(phoneUtil.isPossibleNumber(US_NUMBER));
    assertTrue(phoneUtil.isPossibleNumber(US_LOCAL_NUMBER));
    assertTrue(phoneUtil.isPossibleNumber(GB_NUMBER));
    assertTrue(phoneUtil.isPossibleNumber(INTERNATIONAL_TOLL_FREE));

    assertTrue(phoneUtil.isPossibleNumber("+1 650 253 0000", RegionCode.US));
    assertTrue(phoneUtil.isPossibleNumber("+1 650 GOO OGLE", RegionCode.US));
    assertTrue(phoneUtil.isPossibleNumber("(650) 253-0000", RegionCode.US));
    assertTrue(phoneUtil.isPossibleNumber("253-0000", RegionCode.US));
    assertTrue(phoneUtil.isPossibleNumber("+1 650 253 0000", RegionCode.GB));
    assertTrue(phoneUtil.isPossibleNumber("+44 20 7031 3000", RegionCode.GB));
    assertTrue(phoneUtil.isPossibleNumber("(020) 7031 3000", RegionCode.GB));
    assertTrue(phoneUtil.isPossibleNumber("7031 3000", RegionCode.GB));
    assertTrue(phoneUtil.isPossibleNumber("3331 6005", RegionCode.NZ));
    assertTrue(phoneUtil.isPossibleNumber("+800 1234 5678", RegionCode.UN001));
  }

  public void testIsPossibleNumberWithReason() {
    // National numbers for country calling code +1 that are within 7 to 10 digits are possible.
    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(US_NUMBER));

    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(US_LOCAL_NUMBER));

    assertEquals(PhoneNumberUtil.ValidationResult.TOO_LONG,
                 phoneUtil.isPossibleNumberWithReason(US_LONG_NUMBER));

    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(0).setNationalNumber(2530000L);
    assertEquals(PhoneNumberUtil.ValidationResult.INVALID_COUNTRY_CODE,
                 phoneUtil.isPossibleNumberWithReason(number));

    number.clear();
    number.setCountryCode(1).setNationalNumber(253000L);
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_SHORT,
                 phoneUtil.isPossibleNumberWithReason(number));

    number.clear();
    number.setCountryCode(65).setNationalNumber(1234567890L);
    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(number));

    assertEquals(PhoneNumberUtil.ValidationResult.TOO_LONG,
                 phoneUtil.isPossibleNumberWithReason(INTERNATIONAL_TOLL_FREE_TOO_LONG));

    // Try with number that we don't have metadata for.
    PhoneNumber adNumber = new PhoneNumber();
    adNumber.setCountryCode(376).setNationalNumber(12345L);
    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(adNumber));
    adNumber.setCountryCode(376).setNationalNumber(13L);
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_SHORT,
                 phoneUtil.isPossibleNumberWithReason(adNumber));
    adNumber.setCountryCode(376).setNationalNumber(12345678901234567L);
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_LONG,
                 phoneUtil.isPossibleNumberWithReason(adNumber));
  }

  public void testIsNotPossibleNumber() {
    assertFalse(phoneUtil.isPossibleNumber(US_LONG_NUMBER));
    assertFalse(phoneUtil.isPossibleNumber(INTERNATIONAL_TOLL_FREE_TOO_LONG));

    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(253000L);
    assertFalse(phoneUtil.isPossibleNumber(number));

    number.clear();
    number.setCountryCode(44).setNationalNumber(300L);
    assertFalse(phoneUtil.isPossibleNumber(number));
    assertFalse(phoneUtil.isPossibleNumber("+1 650 253 00000", RegionCode.US));
    assertFalse(phoneUtil.isPossibleNumber("(650) 253-00000", RegionCode.US));
    assertFalse(phoneUtil.isPossibleNumber("I want a Pizza", RegionCode.US));
    assertFalse(phoneUtil.isPossibleNumber("253-000", RegionCode.US));
    assertFalse(phoneUtil.isPossibleNumber("1 3000", RegionCode.GB));
    assertFalse(phoneUtil.isPossibleNumber("+44 300", RegionCode.GB));
    assertFalse(phoneUtil.isPossibleNumber("+800 1234 5678 9", RegionCode.UN001));
  }

  public void testTruncateTooLongNumber() {
    // GB number 080 1234 5678, but entered with 4 extra digits at the end.
    PhoneNumber tooLongNumber = new PhoneNumber();
    tooLongNumber.setCountryCode(44).setNationalNumber(80123456780123L);
    PhoneNumber validNumber = new PhoneNumber();
    validNumber.setCountryCode(44).setNationalNumber(8012345678L);
    assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
    assertEquals(validNumber, tooLongNumber);

    // IT number 022 3456 7890, but entered with 3 extra digits at the end.
    tooLongNumber.clear();
    tooLongNumber.setCountryCode(39).setNationalNumber(2234567890123L).setItalianLeadingZero(true);
    validNumber.clear();
    validNumber.setCountryCode(39).setNationalNumber(2234567890L).setItalianLeadingZero(true);
    assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
    assertEquals(validNumber, tooLongNumber);

    // US number 650-253-0000, but entered with one additional digit at the end.
    tooLongNumber.clear();
    tooLongNumber.mergeFrom(US_LONG_NUMBER);
    assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
    assertEquals(US_NUMBER, tooLongNumber);

    tooLongNumber.clear();
    tooLongNumber.mergeFrom(INTERNATIONAL_TOLL_FREE_TOO_LONG);
    assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
    assertEquals(INTERNATIONAL_TOLL_FREE, tooLongNumber);

    // Tests what happens when a valid number is passed in.
    PhoneNumber validNumberCopy = new PhoneNumber().mergeFrom(validNumber);
    assertTrue(phoneUtil.truncateTooLongNumber(validNumber));
    // Tests the number is not modified.
    assertEquals(validNumberCopy, validNumber);

    // Tests what happens when a number with invalid prefix is passed in.
    PhoneNumber numberWithInvalidPrefix = new PhoneNumber();
    // The test metadata says US numbers cannot have prefix 240.
    numberWithInvalidPrefix.setCountryCode(1).setNationalNumber(2401234567L);
    PhoneNumber invalidNumberCopy = new PhoneNumber().mergeFrom(numberWithInvalidPrefix);
    assertFalse(phoneUtil.truncateTooLongNumber(numberWithInvalidPrefix));
    // Tests the number is not modified.
    assertEquals(invalidNumberCopy, numberWithInvalidPrefix);

    // Tests what happens when a too short number is passed in.
    PhoneNumber tooShortNumber = new PhoneNumber().setCountryCode(1).setNationalNumber(1234L);
    PhoneNumber tooShortNumberCopy = new PhoneNumber().mergeFrom(tooShortNumber);
    assertFalse(phoneUtil.truncateTooLongNumber(tooShortNumber));
    // Tests the number is not modified.
    assertEquals(tooShortNumberCopy, tooShortNumber);
  }

  public void testIsViablePhoneNumber() {
    // Only one or two digits before strange non-possible punctuation.
    assertFalse(PhoneNumberUtil.isViablePhoneNumber("12. March"));
    assertFalse(PhoneNumberUtil.isViablePhoneNumber("1+1+1"));
    assertFalse(PhoneNumberUtil.isViablePhoneNumber("80+0"));
    assertFalse(PhoneNumberUtil.isViablePhoneNumber("00"));
    // Three digits is viable.
    assertTrue(PhoneNumberUtil.isViablePhoneNumber("111"));
    // Alpha numbers.
    assertTrue(PhoneNumberUtil.isViablePhoneNumber("0800-4-pizza"));
    assertTrue(PhoneNumberUtil.isViablePhoneNumber("0800-4-PIZZA"));
  }

  public void testIsViablePhoneNumberNonAscii() {
    // Only one or two digits before possible punctuation followed by more digits.
    assertTrue(PhoneNumberUtil.isViablePhoneNumber("1\u300034"));
    assertFalse(PhoneNumberUtil.isViablePhoneNumber("1\u30003+4"));
    // Unicode variants of possible starting character and other allowed punctuation/digits.
    assertTrue(PhoneNumberUtil.isViablePhoneNumber("\uFF081\uFF09\u30003456789"));
    // Testing a leading + is okay.
    assertTrue(PhoneNumberUtil.isViablePhoneNumber("+1\uFF09\u30003456789"));
  }

  public void testExtractPossibleNumber() {
    // Removes preceding funky punctuation and letters but leaves the rest untouched.
    assertEquals("0800-345-600", PhoneNumberUtil.extractPossibleNumber("Tel:0800-345-600"));
    assertEquals("0800 FOR PIZZA", PhoneNumberUtil.extractPossibleNumber("Tel:0800 FOR PIZZA"));
    // Should not remove plus sign
    assertEquals("+800-345-600", PhoneNumberUtil.extractPossibleNumber("Tel:+800-345-600"));
    // Should recognise wide digits as possible start values.
    assertEquals("\uFF10\uFF12\uFF13",
                 PhoneNumberUtil.extractPossibleNumber("\uFF10\uFF12\uFF13"));
    // Dashes are not possible start values and should be removed.
    assertEquals("\uFF11\uFF12\uFF13",
                 PhoneNumberUtil.extractPossibleNumber("Num-\uFF11\uFF12\uFF13"));
    // If not possible number present, return empty string.
    assertEquals("", PhoneNumberUtil.extractPossibleNumber("Num-...."));
    // Leading brackets are stripped - these are not used when parsing.
    assertEquals("650) 253-0000", PhoneNumberUtil.extractPossibleNumber("(650) 253-0000"));

    // Trailing non-alpha-numeric characters should be removed.
    assertEquals("650) 253-0000", PhoneNumberUtil.extractPossibleNumber("(650) 253-0000..- .."));
    assertEquals("650) 253-0000", PhoneNumberUtil.extractPossibleNumber("(650) 253-0000."));
    // This case has a trailing RTL char.
    assertEquals("650) 253-0000", PhoneNumberUtil.extractPossibleNumber("(650) 253-0000\u200F"));
  }

  public void testMaybeStripNationalPrefix() {
    PhoneMetadata metadata = new PhoneMetadata();
    metadata.setNationalPrefixForParsing("34");
    metadata.setGeneralDesc(new PhoneNumberDesc().setNationalNumberPattern("\\d{4,8}"));
    StringBuilder numberToStrip = new StringBuilder("34356778");
    String strippedNumber = "356778";
    assertTrue(phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata, null));
    assertEquals("Should have had national prefix stripped.",
                 strippedNumber, numberToStrip.toString());
    // Retry stripping - now the number should not start with the national prefix, so no more
    // stripping should occur.
    assertFalse(phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata, null));
    assertEquals("Should have had no change - no national prefix present.",
                 strippedNumber, numberToStrip.toString());
    // Some countries have no national prefix. Repeat test with none specified.
    metadata.setNationalPrefixForParsing("");
    assertFalse(phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata, null));
    assertEquals("Should not strip anything with empty national prefix.",
                 strippedNumber, numberToStrip.toString());
    // If the resultant number doesn't match the national rule, it shouldn't be stripped.
    metadata.setNationalPrefixForParsing("3");
    numberToStrip = new StringBuilder("3123");
    strippedNumber = "3123";
    assertFalse(phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata, null));
    assertEquals("Should have had no change - after stripping, it wouldn't have matched " +
                 "the national rule.",
                 strippedNumber, numberToStrip.toString());
    // Test extracting carrier selection code.
    metadata.setNationalPrefixForParsing("0(81)?");
    numberToStrip = new StringBuilder("08122123456");
    strippedNumber = "22123456";
    StringBuilder carrierCode = new StringBuilder();
    assertTrue(phoneUtil.maybeStripNationalPrefixAndCarrierCode(
        numberToStrip, metadata, carrierCode));
    assertEquals("81", carrierCode.toString());
    assertEquals("Should have had national prefix and carrier code stripped.",
                 strippedNumber, numberToStrip.toString());
    // If there was a transform rule, check it was applied.
    metadata.setNationalPrefixTransformRule("5$15");
    // Note that a capturing group is present here.
    metadata.setNationalPrefixForParsing("0(\\d{2})");
    numberToStrip = new StringBuilder("031123");
    String transformedNumber = "5315123";
    assertTrue(phoneUtil.maybeStripNationalPrefixAndCarrierCode(numberToStrip, metadata, null));
    assertEquals("Should transform the 031 to a 5315.",
                 transformedNumber, numberToStrip.toString());
  }

  public void testMaybeStripInternationalPrefix() {
    String internationalPrefix = "00[39]";
    StringBuilder numberToStrip = new StringBuilder("0034567700-3898003");
    // Note the dash is removed as part of the normalization.
    StringBuilder strippedNumber = new StringBuilder("45677003898003");
    assertEquals(CountryCodeSource.FROM_NUMBER_WITH_IDD,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
    assertEquals("The number supplied was not stripped of its international prefix.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Now the number no longer starts with an IDD prefix, so it should now report
    // FROM_DEFAULT_COUNTRY.
    assertEquals(CountryCodeSource.FROM_DEFAULT_COUNTRY,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));

    numberToStrip = new StringBuilder("00945677003898003");
    assertEquals(CountryCodeSource.FROM_NUMBER_WITH_IDD,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
    assertEquals("The number supplied was not stripped of its international prefix.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Test it works when the international prefix is broken up by spaces.
    numberToStrip = new StringBuilder("00 9 45677003898003");
    assertEquals(CountryCodeSource.FROM_NUMBER_WITH_IDD,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
    assertEquals("The number supplied was not stripped of its international prefix.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Now the number no longer starts with an IDD prefix, so it should now report
    // FROM_DEFAULT_COUNTRY.
    assertEquals(CountryCodeSource.FROM_DEFAULT_COUNTRY,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));

    // Test the + symbol is also recognised and stripped.
    numberToStrip = new StringBuilder("+45677003898003");
    strippedNumber = new StringBuilder("45677003898003");
    assertEquals(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
    assertEquals("The number supplied was not stripped of the plus symbol.",
                 strippedNumber.toString(), numberToStrip.toString());

    // If the number afterwards is a zero, we should not strip this - no country calling code begins
    // with 0.
    numberToStrip = new StringBuilder("0090112-3123");
    strippedNumber = new StringBuilder("00901123123");
    assertEquals(CountryCodeSource.FROM_DEFAULT_COUNTRY,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
    assertEquals("The number supplied had a 0 after the match so shouldn't be stripped.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Here the 0 is separated by a space from the IDD.
    numberToStrip = new StringBuilder("009 0-112-3123");
    assertEquals(CountryCodeSource.FROM_DEFAULT_COUNTRY,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
  }

  public void testMaybeExtractCountryCode() {
    PhoneNumber number = new PhoneNumber();
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion(RegionCode.US);
    // Note that for the US, the IDD is 011.
    try {
      String phoneNumber = "011112-3456789";
      String strippedNumber = "123456789";
      int countryCallingCode = 1;
      StringBuilder numberToFill = new StringBuilder();
      assertEquals("Did not extract country calling code " + countryCallingCode + " correctly.",
                   countryCallingCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true,
                                                     number));
      assertEquals("Did not figure out CountryCodeSource correctly",
                   CountryCodeSource.FROM_NUMBER_WITH_IDD, number.getCountryCodeSource());
      // Should strip and normalize national significant number.
      assertEquals("Did not strip off the country calling code correctly.",
                   strippedNumber,
                   numberToFill.toString());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "+6423456789";
      int countryCallingCode = 64;
      StringBuilder numberToFill = new StringBuilder();
      assertEquals("Did not extract country calling code " + countryCallingCode + " correctly.",
                   countryCallingCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true,
                                                     number));
      assertEquals("Did not figure out CountryCodeSource correctly",
                   CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN, number.getCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "+80012345678";
      int countryCallingCode = 800;
      StringBuilder numberToFill = new StringBuilder();
      assertEquals("Did not extract country calling code " + countryCallingCode + " correctly.",
                   countryCallingCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true,
                                                     number));
      assertEquals("Did not figure out CountryCodeSource correctly",
                   CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN, number.getCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "2345-6789";
      StringBuilder numberToFill = new StringBuilder();
      assertEquals(
          "Should not have extracted a country calling code - no international prefix present.",
          0,
          phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true, number));
      assertEquals("Did not figure out CountryCodeSource correctly",
                   CountryCodeSource.FROM_DEFAULT_COUNTRY, number.getCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "0119991123456789";
      StringBuilder numberToFill = new StringBuilder();
      phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true, number);
      fail("Should have thrown an exception, no valid country calling code present.");
    } catch (NumberParseException e) {
      // Expected.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    number.clear();
    try {
      String phoneNumber = "(1 610) 619 4466";
      int countryCallingCode = 1;
      StringBuilder numberToFill = new StringBuilder();
      assertEquals("Should have extracted the country calling code of the region passed in",
                   countryCallingCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true,
                                                     number));
      assertEquals("Did not figure out CountryCodeSource correctly",
                   CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN,
                   number.getCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "(1 610) 619 4466";
      int countryCallingCode = 1;
      StringBuilder numberToFill = new StringBuilder();
      assertEquals("Should have extracted the country calling code of the region passed in",
                   countryCallingCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, false,
                                                     number));
      assertFalse("Should not contain CountryCodeSource.", number.hasCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "(1 610) 619 446";
      StringBuilder numberToFill = new StringBuilder();
      assertEquals("Should not have extracted a country calling code - invalid number after " +
                   "extraction of uncertain country calling code.",
                   0,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, false,
                                                     number));
      assertFalse("Should not contain CountryCodeSource.", number.hasCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "(1 610) 619";
      StringBuilder numberToFill = new StringBuilder();
      assertEquals("Should not have extracted a country calling code - too short number both " +
                   "before and after extraction of uncertain country calling code.",
                   0,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true,
                                                     number));
      assertEquals("Did not figure out CountryCodeSource correctly",
                   CountryCodeSource.FROM_DEFAULT_COUNTRY, number.getCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
  }

  public void testParseNationalNumber() throws Exception {
    // National prefix attached.
    assertEquals(NZ_NUMBER, phoneUtil.parse("033316005", RegionCode.NZ));
    assertEquals(NZ_NUMBER, phoneUtil.parse("33316005", RegionCode.NZ));
    // National prefix attached and some formatting present.
    assertEquals(NZ_NUMBER, phoneUtil.parse("03-331 6005", RegionCode.NZ));
    assertEquals(NZ_NUMBER, phoneUtil.parse("03 331 6005", RegionCode.NZ));

    // Testing international prefixes.
    // Should strip country calling code.
    assertEquals(NZ_NUMBER, phoneUtil.parse("0064 3 331 6005", RegionCode.NZ));
    // Try again, but this time we have an international number with Region Code US. It should
    // recognise the country calling code and parse accordingly.
    assertEquals(NZ_NUMBER, phoneUtil.parse("01164 3 331 6005", RegionCode.US));
    assertEquals(NZ_NUMBER, phoneUtil.parse("+64 3 331 6005", RegionCode.US));
    // We should ignore the leading plus here, since it is not followed by a valid country code but
    // instead is followed by the IDD for the US.
    assertEquals(NZ_NUMBER, phoneUtil.parse("+01164 3 331 6005", RegionCode.US));
    assertEquals(NZ_NUMBER, phoneUtil.parse("+0064 3 331 6005", RegionCode.NZ));
    assertEquals(NZ_NUMBER, phoneUtil.parse("+ 00 64 3 331 6005", RegionCode.NZ));

    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(64123456L);
    assertEquals(nzNumber, phoneUtil.parse("64(0)64123456", RegionCode.NZ));
    // Check that using a "/" is fine in a phone number.
    assertEquals(DE_NUMBER, phoneUtil.parse("301/23456", RegionCode.DE));

    PhoneNumber usNumber = new PhoneNumber();
    // Check it doesn't use the '1' as a country calling code when parsing if the phone number was
    // already possible.
    usNumber.setCountryCode(1).setNationalNumber(1234567890L);
    assertEquals(usNumber, phoneUtil.parse("123-456-7890", RegionCode.US));
  }

  public void testParseNumberWithAlphaCharacters() throws Exception {
    // Test case with alpha characters.
    PhoneNumber tollfreeNumber = new PhoneNumber();
    tollfreeNumber.setCountryCode(64).setNationalNumber(800332005L);
    assertEquals(tollfreeNumber, phoneUtil.parse("0800 DDA 005", RegionCode.NZ));
    PhoneNumber premiumNumber = new PhoneNumber();
    premiumNumber.setCountryCode(64).setNationalNumber(9003326005L);
    assertEquals(premiumNumber, phoneUtil.parse("0900 DDA 6005", RegionCode.NZ));
    // Not enough alpha characters for them to be considered intentional, so they are stripped.
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 6005a", RegionCode.NZ));
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 600a5", RegionCode.NZ));
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 600A5", RegionCode.NZ));
    assertEquals(premiumNumber, phoneUtil.parse("0900 a332 600A5", RegionCode.NZ));
  }

  public void testParseWithInternationalPrefixes() throws Exception {
    assertEquals(US_NUMBER, phoneUtil.parse("+1 (650) 253-0000", RegionCode.NZ));
    assertEquals(INTERNATIONAL_TOLL_FREE, phoneUtil.parse("011 800 1234 5678", RegionCode.US));
    assertEquals(US_NUMBER, phoneUtil.parse("1-650-253-0000", RegionCode.US));
    // Calling the US number from Singapore by using different service providers
    // 1st test: calling using SingTel IDD service (IDD is 001)
    assertEquals(US_NUMBER, phoneUtil.parse("0011-650-253-0000", RegionCode.SG));
    // 2nd test: calling using StarHub IDD service (IDD is 008)
    assertEquals(US_NUMBER, phoneUtil.parse("0081-650-253-0000", RegionCode.SG));
    // 3rd test: calling using SingTel V019 service (IDD is 019)
    assertEquals(US_NUMBER, phoneUtil.parse("0191-650-253-0000", RegionCode.SG));
    // Calling the US number from Poland
    assertEquals(US_NUMBER, phoneUtil.parse("0~01-650-253-0000", RegionCode.PL));
    // Using "++" at the start.
    assertEquals(US_NUMBER, phoneUtil.parse("++1 (650) 253-0000", RegionCode.PL));
  }

  public void testParseNonAscii() throws Exception {
    // Using a full-width plus sign.
    assertEquals(US_NUMBER, phoneUtil.parse("\uFF0B1 (650) 253-0000", RegionCode.SG));
    // The whole number, including punctuation, is here represented in full-width form.
    assertEquals(US_NUMBER, phoneUtil.parse("\uFF0B\uFF11\u3000\uFF08\uFF16\uFF15\uFF10\uFF09" +
                                            "\u3000\uFF12\uFF15\uFF13\uFF0D\uFF10\uFF10\uFF10" +
                                            "\uFF10",
                                            RegionCode.SG));
    // Using U+30FC dash instead.
    assertEquals(US_NUMBER, phoneUtil.parse("\uFF0B\uFF11\u3000\uFF08\uFF16\uFF15\uFF10\uFF09" +
                                            "\u3000\uFF12\uFF15\uFF13\u30FC\uFF10\uFF10\uFF10" +
                                            "\uFF10",
                                            RegionCode.SG));

    // Using a very strange decimal digit range (Mongolian digits).
    assertEquals(US_NUMBER, phoneUtil.parse("\u1811 \u1816\u1815\u1810 " +
                                            "\u1812\u1815\u1813 \u1810\u1810\u1810\u1810",
                                            RegionCode.US));
  }

  public void testParseWithLeadingZero() throws Exception {
    assertEquals(IT_NUMBER, phoneUtil.parse("+39 02-36618 300", RegionCode.NZ));
    assertEquals(IT_NUMBER, phoneUtil.parse("02-36618 300", RegionCode.IT));

    assertEquals(IT_MOBILE, phoneUtil.parse("345 678 901", RegionCode.IT));
  }

  public void testParseNationalNumberArgentina() throws Exception {
    // Test parsing mobile numbers of Argentina.
    PhoneNumber arNumber = new PhoneNumber();
    arNumber.setCountryCode(54).setNationalNumber(93435551212L);
    assertEquals(arNumber, phoneUtil.parse("+54 9 343 555 1212", RegionCode.AR));
    assertEquals(arNumber, phoneUtil.parse("0343 15 555 1212", RegionCode.AR));

    arNumber.clear();
    arNumber.setCountryCode(54).setNationalNumber(93715654320L);
    assertEquals(arNumber, phoneUtil.parse("+54 9 3715 65 4320", RegionCode.AR));
    assertEquals(arNumber, phoneUtil.parse("03715 15 65 4320", RegionCode.AR));
    assertEquals(AR_MOBILE, phoneUtil.parse("911 876 54321", RegionCode.AR));

    // Test parsing fixed-line numbers of Argentina.
    assertEquals(AR_NUMBER, phoneUtil.parse("+54 11 8765 4321", RegionCode.AR));
    assertEquals(AR_NUMBER, phoneUtil.parse("011 8765 4321", RegionCode.AR));

    arNumber.clear();
    arNumber.setCountryCode(54).setNationalNumber(3715654321L);
    assertEquals(arNumber, phoneUtil.parse("+54 3715 65 4321", RegionCode.AR));
    assertEquals(arNumber, phoneUtil.parse("03715 65 4321", RegionCode.AR));

    arNumber.clear();
    arNumber.setCountryCode(54).setNationalNumber(2312340000L);
    assertEquals(arNumber, phoneUtil.parse("+54 23 1234 0000", RegionCode.AR));
    assertEquals(arNumber, phoneUtil.parse("023 1234 0000", RegionCode.AR));
  }

  public void testParseWithXInNumber() throws Exception {
    // Test that having an 'x' in the phone number at the start is ok and that it just gets removed.
    assertEquals(AR_NUMBER, phoneUtil.parse("01187654321", RegionCode.AR));
    assertEquals(AR_NUMBER, phoneUtil.parse("(0) 1187654321", RegionCode.AR));
    assertEquals(AR_NUMBER, phoneUtil.parse("0 1187654321", RegionCode.AR));
    assertEquals(AR_NUMBER, phoneUtil.parse("(0xx) 1187654321", RegionCode.AR));
    PhoneNumber arFromUs = new PhoneNumber();
    arFromUs.setCountryCode(54).setNationalNumber(81429712L);
    // This test is intentionally constructed such that the number of digit after xx is larger than
    // 7, so that the number won't be mistakenly treated as an extension, as we allow extensions up
    // to 7 digits. This assumption is okay for now as all the countries where a carrier selection
    // code is written in the form of xx have a national significant number of length larger than 7.
    assertEquals(arFromUs, phoneUtil.parse("011xx5481429712", RegionCode.US));
  }

  public void testParseNumbersMexico() throws Exception {
    // Test parsing fixed-line numbers of Mexico.
    PhoneNumber mxNumber = new PhoneNumber();
    mxNumber.setCountryCode(52).setNationalNumber(4499780001L);
    assertEquals(mxNumber, phoneUtil.parse("+52 (449)978-0001", RegionCode.MX));
    assertEquals(mxNumber, phoneUtil.parse("01 (449)978-0001", RegionCode.MX));
    assertEquals(mxNumber, phoneUtil.parse("(449)978-0001", RegionCode.MX));

    // Test parsing mobile numbers of Mexico.
    mxNumber.clear();
    mxNumber.setCountryCode(52).setNationalNumber(13312345678L);
    assertEquals(mxNumber, phoneUtil.parse("+52 1 33 1234-5678", RegionCode.MX));
    assertEquals(mxNumber, phoneUtil.parse("044 (33) 1234-5678", RegionCode.MX));
    assertEquals(mxNumber, phoneUtil.parse("045 33 1234-5678", RegionCode.MX));
  }

  public void testFailedParseOnInvalidNumbers() {
    try {
      String sentencePhoneNumber = "This is not a phone number";
      phoneUtil.parse(sentencePhoneNumber, RegionCode.NZ);
      fail("This should not parse without throwing an exception " + sentencePhoneNumber);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.NOT_A_NUMBER,
                   e.getErrorType());
    }
    try {
      String tooLongPhoneNumber = "01495 72553301873 810104";
      phoneUtil.parse(tooLongPhoneNumber, RegionCode.GB);
      fail("This should not parse without throwing an exception " + tooLongPhoneNumber);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_LONG,
                   e.getErrorType());
    }
    try {
      String plusMinusPhoneNumber = "+---";
      phoneUtil.parse(plusMinusPhoneNumber, RegionCode.DE);
      fail("This should not parse without throwing an exception " + plusMinusPhoneNumber);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.NOT_A_NUMBER,
                   e.getErrorType());
    }
    try {
      String tooShortPhoneNumber = "+49 0";
      phoneUtil.parse(tooShortPhoneNumber, RegionCode.DE);
      fail("This should not parse without throwing an exception " + tooShortPhoneNumber);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_NSN,
                   e.getErrorType());
    }
    try {
      String invalidCountryCode = "+210 3456 56789";
      phoneUtil.parse(invalidCountryCode, RegionCode.NZ);
      fail("This is not a recognised region code: should fail: " + invalidCountryCode);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String plusAndIddAndInvalidCountryCode = "+ 00 210 3 331 6005";
      phoneUtil.parse(plusAndIddAndInvalidCountryCode, RegionCode.NZ);
      fail("This should not parse without throwing an exception.");
    } catch (NumberParseException e) {
      // Expected this exception. 00 is a correct IDD, but 210 is not a valid country code.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String someNumber = "123 456 7890";
      phoneUtil.parse(someNumber, RegionCode.ZZ);
      fail("'Unknown' region code not allowed: should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String someNumber = "123 456 7890";
      phoneUtil.parse(someNumber, RegionCode.CS);
      fail("Deprecated region code not allowed: should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String someNumber = "123 456 7890";
      phoneUtil.parse(someNumber, null);
      fail("Null region code not allowed: should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String someNumber = "0044------";
      phoneUtil.parse(someNumber, RegionCode.GB);
      fail("No number provided, only region code: should fail");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                   e.getErrorType());
    }
    try {
      String someNumber = "0044";
      phoneUtil.parse(someNumber, RegionCode.GB);
      fail("No number provided, only region code: should fail");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                   e.getErrorType());
    }
    try {
      String someNumber = "011";
      phoneUtil.parse(someNumber, RegionCode.US);
      fail("Only IDD provided - should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                   e.getErrorType());
    }
    try {
      String someNumber = "0119";
      phoneUtil.parse(someNumber, RegionCode.US);
      fail("Only IDD provided and then 9 - should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                   e.getErrorType());
    }
    try {
      String emptyNumber = "";
      // Invalid region.
      phoneUtil.parse(emptyNumber, RegionCode.ZZ);
      fail("Empty string - should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.NOT_A_NUMBER,
                   e.getErrorType());
    }
    try {
      String nullNumber = null;
      // Invalid region.
      phoneUtil.parse(nullNumber, RegionCode.ZZ);
      fail("Null string - should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.NOT_A_NUMBER,
                   e.getErrorType());
    } catch (NullPointerException e) {
      fail("Null string - but should not throw a null pointer exception.");
    }
    try {
      String nullNumber = null;
      phoneUtil.parse(nullNumber, RegionCode.US);
      fail("Null string - should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.NOT_A_NUMBER,
                   e.getErrorType());
    } catch (NullPointerException e) {
      fail("Null string - but should not throw a null pointer exception.");
    }
  }

  public void testParseNumbersWithPlusWithNoRegion() throws Exception {
    // RegionCode.ZZ is allowed only if the number starts with a '+' - then the country calling code
    // can be calculated.
    assertEquals(NZ_NUMBER, phoneUtil.parse("+64 3 331 6005", RegionCode.ZZ));
    // Test with full-width plus.
    assertEquals(NZ_NUMBER, phoneUtil.parse("\uFF0B64 3 331 6005", RegionCode.ZZ));
    // Test with normal plus but leading characters that need to be stripped.
    assertEquals(NZ_NUMBER, phoneUtil.parse("Tel: +64 3 331 6005", RegionCode.ZZ));
    assertEquals(NZ_NUMBER, phoneUtil.parse("+64 3 331 6005", null));
    assertEquals(INTERNATIONAL_TOLL_FREE, phoneUtil.parse("+800 1234 5678", null));

    // It is important that we set the carrier code to an empty string, since we used
    // ParseAndKeepRawInput and no carrier code was found.
    PhoneNumber nzNumberWithRawInput = new PhoneNumber().mergeFrom(NZ_NUMBER).
        setRawInput("+64 3 331 6005").
        setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN).
        setPreferredDomesticCarrierCode("");
    assertEquals(nzNumberWithRawInput, phoneUtil.parseAndKeepRawInput("+64 3 331 6005",
                                                                      RegionCode.ZZ));
    // Null is also allowed for the region code in these cases.
    assertEquals(nzNumberWithRawInput, phoneUtil.parseAndKeepRawInput("+64 3 331 6005", null));
  }

  public void testParseExtensions() throws Exception {
    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L).setExtension("3456");
    assertEquals(nzNumber, phoneUtil.parse("03 331 6005 ext 3456", RegionCode.NZ));
    assertEquals(nzNumber, phoneUtil.parse("03-3316005x3456", RegionCode.NZ));
    assertEquals(nzNumber, phoneUtil.parse("03-3316005 int.3456", RegionCode.NZ));
    assertEquals(nzNumber, phoneUtil.parse("03 3316005 #3456", RegionCode.NZ));
    // Test the following do not extract extensions:
    assertEquals(ALPHA_NUMERIC_NUMBER, phoneUtil.parse("1800 six-flags", RegionCode.US));
    assertEquals(ALPHA_NUMERIC_NUMBER, phoneUtil.parse("1800 SIX FLAGS", RegionCode.US));
    assertEquals(ALPHA_NUMERIC_NUMBER, phoneUtil.parse("0~0 1800 7493 5247", RegionCode.PL));
    assertEquals(ALPHA_NUMERIC_NUMBER, phoneUtil.parse("(1800) 7493.5247", RegionCode.US));
    // Check that the last instance of an extension token is matched.
    PhoneNumber extnNumber = new PhoneNumber().mergeFrom(ALPHA_NUMERIC_NUMBER).setExtension("1234");
    assertEquals(extnNumber, phoneUtil.parse("0~0 1800 7493 5247 ~1234", RegionCode.PL));
    // Verifying bug-fix where the last digit of a number was previously omitted if it was a 0 when
    // extracting the extension. Also verifying a few different cases of extensions.
    PhoneNumber ukNumber = new PhoneNumber();
    ukNumber.setCountryCode(44).setNationalNumber(2034567890L).setExtension("456");
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890x456", RegionCode.NZ));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890x456", RegionCode.GB));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 x456", RegionCode.GB));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X456", RegionCode.GB));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X 456", RegionCode.GB));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X  456", RegionCode.GB));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 x 456  ", RegionCode.GB));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890  X 456", RegionCode.GB));
    assertEquals(ukNumber, phoneUtil.parse("+44-2034567890;ext=456", RegionCode.GB));
    // Full-width extension, "extn" only.
    assertEquals(ukNumber, phoneUtil.parse("+442034567890\uFF45\uFF58\uFF54\uFF4E456",
                                           RegionCode.GB));
    // "xtn" only.
    assertEquals(ukNumber, phoneUtil.parse("+442034567890\uFF58\uFF54\uFF4E456",
                                           RegionCode.GB));
    // "xt" only.
    assertEquals(ukNumber, phoneUtil.parse("+442034567890\uFF58\uFF54456",
                                           RegionCode.GB));

    PhoneNumber usWithExtension = new PhoneNumber();
    usWithExtension.setCountryCode(1).setNationalNumber(8009013355L).setExtension("7246433");
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 x 7246433", RegionCode.US));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 , ext 7246433", RegionCode.US));
    assertEquals(usWithExtension,
                 phoneUtil.parse("(800) 901-3355 ,extension 7246433", RegionCode.US));
    assertEquals(usWithExtension,
                 phoneUtil.parse("(800) 901-3355 ,extensi\u00F3n 7246433", RegionCode.US));
    // Repeat with the small letter o with acute accent created by combining characters.
    assertEquals(usWithExtension,
                 phoneUtil.parse("(800) 901-3355 ,extensio\u0301n 7246433", RegionCode.US));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 , 7246433", RegionCode.US));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 ext: 7246433", RegionCode.US));

    // Test that if a number has two extensions specified, we ignore the second.
    PhoneNumber usWithTwoExtensionsNumber = new PhoneNumber();
    usWithTwoExtensionsNumber.setCountryCode(1).setNationalNumber(2121231234L).setExtension("508");
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508/x1234",
                                                            RegionCode.US));
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508/ x1234",
                                                            RegionCode.US));
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508\\x1234",
                                                            RegionCode.US));

    // Test parsing numbers in the form (645) 123-1234-910# works, where the last 3 digits before
    // the # are an extension.
    usWithExtension.clear();
    usWithExtension.setCountryCode(1).setNationalNumber(6451231234L).setExtension("910");
    assertEquals(usWithExtension, phoneUtil.parse("+1 (645) 123 1234-910#", RegionCode.US));
    // Retry with the same number in a slightly different format.
    assertEquals(usWithExtension, phoneUtil.parse("+1 (645) 123 1234 ext. 910#", RegionCode.US));
  }

  public void testParseAndKeepRaw() throws Exception {
    PhoneNumber alphaNumericNumber = new PhoneNumber().mergeFrom(ALPHA_NUMERIC_NUMBER).
        setRawInput("800 six-flags").
        setCountryCodeSource(CountryCodeSource.FROM_DEFAULT_COUNTRY).
        setPreferredDomesticCarrierCode("");
    assertEquals(alphaNumericNumber,
                 phoneUtil.parseAndKeepRawInput("800 six-flags", RegionCode.US));

    PhoneNumber shorterAlphaNumber = new PhoneNumber().
        setCountryCode(1).setNationalNumber(8007493524L).
        setRawInput("1800 six-flag").
        setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN).
        setPreferredDomesticCarrierCode("");
    assertEquals(shorterAlphaNumber,
                 phoneUtil.parseAndKeepRawInput("1800 six-flag", RegionCode.US));

    shorterAlphaNumber.setRawInput("+1800 six-flag").
        setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN);
    assertEquals(shorterAlphaNumber,
                 phoneUtil.parseAndKeepRawInput("+1800 six-flag", RegionCode.NZ));

    shorterAlphaNumber.setRawInput("001800 six-flag").
        setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_IDD);
    assertEquals(shorterAlphaNumber,
                 phoneUtil.parseAndKeepRawInput("001800 six-flag", RegionCode.NZ));

    // Invalid region code supplied.
    try {
      phoneUtil.parseAndKeepRawInput("123 456 7890", RegionCode.CS);
      fail("Deprecated region code not allowed: should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }

    PhoneNumber koreanNumber = new PhoneNumber();
    koreanNumber.setCountryCode(82).setNationalNumber(22123456).setRawInput("08122123456").
        setCountryCodeSource(CountryCodeSource.FROM_DEFAULT_COUNTRY).
        setPreferredDomesticCarrierCode("81");
    assertEquals(koreanNumber, phoneUtil.parseAndKeepRawInput("08122123456", RegionCode.KR));
  }

  public void testCountryWithNoNumberDesc() {
    // Andorra is a country where we don't have PhoneNumberDesc info in the metadata.
    PhoneNumber adNumber = new PhoneNumber();
    adNumber.setCountryCode(376).setNationalNumber(12345L);
    assertEquals("+376 12345", phoneUtil.format(adNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+37612345", phoneUtil.format(adNumber, PhoneNumberFormat.E164));
    assertEquals("12345", phoneUtil.format(adNumber, PhoneNumberFormat.NATIONAL));
    assertEquals(PhoneNumberUtil.PhoneNumberType.UNKNOWN, phoneUtil.getNumberType(adNumber));
    assertTrue(phoneUtil.isValidNumber(adNumber));

    // Test dialing a US number from within Andorra.
    assertEquals("00 1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(US_NUMBER, RegionCode.AD));
  }

  public void testUnknownCountryCallingCodeForValidation() {
    PhoneNumber invalidNumber = new PhoneNumber();
    invalidNumber.setCountryCode(0).setNationalNumber(1234L);
    assertFalse(phoneUtil.isValidNumber(invalidNumber));
  }

  public void testIsNumberMatchMatches() throws Exception {
    // Test simple matches where formatting is different, or leading zeroes, or country calling code
    // has been specified.
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331 6005", "+64 03 331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+800 1234 5678", "+80012345678"));
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+64 03 331-6005", "+64 03331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+643 331-6005", "+64033316005"));
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+643 331-6005", "+6433316005"));
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "+6433316005"));
    // Test alpha numbers.
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+1800 siX-Flags", "+1 800 7493 5247"));
    // Test numbers with extensions.
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005 extn 1234", "+6433316005#1234"));
    // Test proto buffers.
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(NZ_NUMBER, "+6403 331 6005"));

    PhoneNumber nzNumber = new PhoneNumber().mergeFrom(NZ_NUMBER).setExtension("3456");
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "+643 331 6005 ext 3456"));
    // Check empty extensions are ignored.
    nzNumber.setExtension("");
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "+6403 331 6005"));
    // Check variant with two proto buffers.
    assertEquals("Number " + nzNumber.toString() + " did not match " + NZ_NUMBER.toString(),
                 PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, NZ_NUMBER));

    // Check raw_input, country_code_source and preferred_domestic_carrier_code are ignored.
    PhoneNumber brNumberOne = new PhoneNumber();
    PhoneNumber brNumberTwo = new PhoneNumber();
    brNumberOne.setCountryCode(55).setNationalNumber(3121286979L)
        .setCountryCodeSource(PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN)
        .setPreferredDomesticCarrierCode("12").setRawInput("012 3121286979");
    brNumberTwo.setCountryCode(55).setNationalNumber(3121286979L)
        .setCountryCodeSource(PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY)
        .setPreferredDomesticCarrierCode("14").setRawInput("143121286979");
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(brNumberOne, brNumberTwo));
  }

  public void testIsNumberMatchNonMatches() throws Exception {
    // Non-matches.
    assertEquals(PhoneNumberUtil.MatchType.NO_MATCH,
                 phoneUtil.isNumberMatch("03 331 6005", "03 331 6006"));
    assertEquals(PhoneNumberUtil.MatchType.NO_MATCH,
                 phoneUtil.isNumberMatch("+800 1234 5678", "+1 800 1234 5678"));
    // Different country calling code, partial number match.
    assertEquals(PhoneNumberUtil.MatchType.NO_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "+16433316005"));
    // Different country calling code, same number.
    assertEquals(PhoneNumberUtil.MatchType.NO_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "+6133316005"));
    // Extension different, all else the same.
    assertEquals(PhoneNumberUtil.MatchType.NO_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005 extn 1234", "0116433316005#1235"));
    // NSN matches, but extension is different - not the same number.
    assertEquals(PhoneNumberUtil.MatchType.NO_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005 ext.1235", "3 331 6005#1234"));

    // Invalid numbers that can't be parsed.
    assertEquals(PhoneNumberUtil.MatchType.NOT_A_NUMBER,
                 phoneUtil.isNumberMatch("43", "3 331 6043"));
    assertEquals(PhoneNumberUtil.MatchType.NOT_A_NUMBER,
                 phoneUtil.isNumberMatch("+43", "+64 3 331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.NOT_A_NUMBER,
                 phoneUtil.isNumberMatch("+43", "64 3 331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.NOT_A_NUMBER,
                 phoneUtil.isNumberMatch("Dog", "64 3 331 6005"));
  }

  public void testIsNumberMatchNsnMatches() throws Exception {
    // NSN matches.
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "03 331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(NZ_NUMBER, "03 331 6005"));
    // Here the second number possibly starts with the country calling code for New Zealand,
    // although we are unsure.
    PhoneNumber unchangedNzNumber = new PhoneNumber().mergeFrom(NZ_NUMBER);
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(unchangedNzNumber, "(64-3) 331 6005"));
    // Check the phone number proto was not edited during the method call.
    assertEquals(NZ_NUMBER, unchangedNzNumber);

    // Here, the 1 might be a national prefix, if we compare it to the US number, so the resultant
    // match is an NSN match.
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(US_NUMBER, "1-650-253-0000"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(US_NUMBER, "6502530000"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("+1 650-253 0000", "1 650 253 0000"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("1 650-253 0000", "1 650 253 0000"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("1 650-253 0000", "+1 650 253 0000"));
    // For this case, the match will be a short NSN match, because we cannot assume that the 1 might
    // be a national prefix, so don't remove it when parsing.
    PhoneNumber randomNumber = new PhoneNumber();
    randomNumber.setCountryCode(41).setNationalNumber(6502530000L);
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch(randomNumber, "1-650-253-0000"));
  }

  public void testIsNumberMatchShortNsnMatches() throws Exception {
    // Short NSN matches with the country not specified for either one or both numbers.
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "331 6005"));
    // We did not know that the "0" was a national prefix since neither number has a country code,
    // so this is considered a SHORT_NSN_MATCH.
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("3 331-6005", "03 331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("3 331-6005", "331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("3 331-6005", "+64 331 6005"));
    // Short NSN match with the country specified.
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("03 331-6005", "331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("1 234 345 6789", "345 6789"));
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("+1 (234) 345 6789", "345 6789"));
    // NSN matches, country calling code omitted for one number, extension missing for one.
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "3 331 6005#1234"));
    // One has Italian leading zero, one does not.
    PhoneNumber italianNumberOne = new PhoneNumber();
    italianNumberOne.setCountryCode(39).setNationalNumber(1234L).setItalianLeadingZero(true);
    PhoneNumber italianNumberTwo = new PhoneNumber();
    italianNumberTwo.setCountryCode(39).setNationalNumber(1234L);
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch(italianNumberOne, italianNumberTwo));
    // One has an extension, the other has an extension of "".
    italianNumberOne.setExtension("1234").clearItalianLeadingZero();
    italianNumberTwo.setExtension("");
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch(italianNumberOne, italianNumberTwo));
  }

  public void testCanBeInternationallyDialled() throws Exception {
    // We have no-international-dialling rules for the US in our test metadata that say that
    // toll-free numbers cannot be dialled internationally.
    assertFalse(phoneUtil.canBeInternationallyDialled(US_TOLLFREE));

    // Normal US numbers can be internationally dialled.
    assertTrue(phoneUtil.canBeInternationallyDialled(US_NUMBER));

    // Invalid number.
    assertTrue(phoneUtil.canBeInternationallyDialled(US_LOCAL_NUMBER));

    // We have no data for NZ - should return true.
    assertTrue(phoneUtil.canBeInternationallyDialled(NZ_NUMBER));
    assertTrue(phoneUtil.canBeInternationallyDialled(INTERNATIONAL_TOLL_FREE));
  }

  public void testIsAlphaNumber() throws Exception {
    assertTrue(phoneUtil.isAlphaNumber("1800 six-flags"));
    assertTrue(phoneUtil.isAlphaNumber("1800 six-flags ext. 1234"));
    assertTrue(phoneUtil.isAlphaNumber("+800 six-flags"));
    assertFalse(phoneUtil.isAlphaNumber("1800 123-1234"));
    assertFalse(phoneUtil.isAlphaNumber("1800 123-1234 extension: 1234"));
    assertFalse(phoneUtil.isAlphaNumber("+800 1234-1234"));
  }
}
