/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
  static final String TEST_META_DATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting";
  static final String TEST_COUNTRY_CODE_TO_REGION_CODE_MAP_CLASS_NAME =
      "CountryCodeToRegionCodeMapForTesting";

  public PhoneNumberUtilTest() {
    phoneUtil = initilizePhoneUtilForTesting();
  }

  PhoneNumberUtil initilizePhoneUtilForTesting() {
    PhoneNumberUtil.resetInstance();
    return PhoneNumberUtil.getInstance(TEST_META_DATA_FILE_PREFIX,
        CountryCodeToRegionCodeMapForTesting.getCountryCodeToRegionCodeMap());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testGetInstanceLoadUSMetadata() {
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion("US");
    assertEquals("US", metadata.getId());
    assertEquals(1, metadata.getCountryCode());
    assertEquals("011", metadata.getInternationalPrefix());
    assertTrue(metadata.hasNationalPrefix());
    assertEquals(2, metadata.getNumberFormatCount());
    assertEquals("(\\d{3})(\\d{3})(\\d{4})",
                 metadata.getNumberFormat(0).getPattern());
    assertEquals("$1 $2 $3", metadata.getNumberFormat(0).getFormat());
    assertEquals("[13-9]\\d{9}|2[0-35-9]\\d{8}",
                 metadata.getGeneralDesc().getNationalNumberPattern());
    assertEquals("\\d{7,10}", metadata.getGeneralDesc().getPossibleNumberPattern());
    assertTrue(metadata.getGeneralDesc().exactlySameAs(metadata.getFixedLine()));
    assertEquals("\\d{10}", metadata.getTollFree().getPossibleNumberPattern());
    assertEquals("900\\d{7}", metadata.getPremiumRate().getNationalNumberPattern());
    // No shared-cost data is available, so it should be initialised to "NA".
    assertEquals("NA", metadata.getSharedCost().getNationalNumberPattern());
    assertEquals("NA", metadata.getSharedCost().getPossibleNumberPattern());
  }

  public void testGetInstanceLoadDEMetadata() {
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion("DE");
    assertEquals("DE", metadata.getId());
    assertEquals(49, metadata.getCountryCode());
    assertEquals("00", metadata.getInternationalPrefix());
    assertEquals("0", metadata.getNationalPrefix());
    assertEquals(5, metadata.getNumberFormatCount());
    assertEquals(1, metadata.getNumberFormat(4).getLeadingDigitsPatternCount());
    assertEquals("900", metadata.getNumberFormat(4).getLeadingDigitsPattern(0));
    assertEquals("(\\d{3})(\\d{3,4})(\\d{4})",
                 metadata.getNumberFormat(4).getPattern());
    assertEquals("$1 $2 $3", metadata.getNumberFormat(4).getFormat());
    assertEquals("(?:[24-6]\\d{2}|3[03-9]\\d|[789](?:[1-9]\\d|0[2-9]))\\d{3,8}",
                 metadata.getFixedLine().getNationalNumberPattern());
    assertEquals("\\d{2,14}", metadata.getFixedLine().getPossibleNumberPattern());
    assertEquals("30123456", metadata.getFixedLine().getExampleNumber());
    assertEquals("\\d{10}", metadata.getTollFree().getPossibleNumberPattern());
    assertEquals("900([135]\\d{6}|9\\d{7})", metadata.getPremiumRate().getNationalNumberPattern());
  }

  public void testGetInstanceLoadARMetadata() {
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion("AR");
    assertEquals("AR", metadata.getId());
    assertEquals(54, metadata.getCountryCode());
    assertEquals("00", metadata.getInternationalPrefix());
    assertEquals("0", metadata.getNationalPrefix());
    assertEquals("0(?:(11|343|3715)15)?", metadata.getNationalPrefixForParsing());
    assertEquals("9$1", metadata.getNationalPrefixTransformRule());
    assertEquals("$1 15 $2-$3", metadata.getNumberFormat(2).getFormat());
    assertEquals("9(\\d{4})(\\d{2})(\\d{4})",
                 metadata.getNumberFormat(3).getPattern());
    assertEquals("(9)(\\d{4})(\\d{2})(\\d{4})",
                 metadata.getIntlNumberFormat(3).getPattern());
    assertEquals("$1 $2 $3 $4", metadata.getIntlNumberFormat(3).getFormat());
  }

  public void testGetLengthOfGeographicalAreaCode() {
    PhoneNumber number = new PhoneNumber();
    // Google MTV, which has area code "650".
    number.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals(3, phoneUtil.getLengthOfGeographicalAreaCode(number));

    // A North America toll-free number, which has no area code.
    number.setCountryCode(1).setNationalNumber(8002530000L);
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(number));

    // An invalid US number (1 digit shorter), which has no area code.
    number.setCountryCode(1).setNationalNumber(650253000L);
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(number));

    // Google London, which has area code "20".
    number.setCountryCode(44).setNationalNumber(2070313000L);
    assertEquals(2, phoneUtil.getLengthOfGeographicalAreaCode(number));

    // A UK mobile phone, which has no area code.
    number.setCountryCode(44).setNationalNumber(7123456789L);
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(number));

    // Google Buenos Aires, which has area code "11".
    number.setCountryCode(54).setNationalNumber(1155303000L);
    assertEquals(2, phoneUtil.getLengthOfGeographicalAreaCode(number));

    // Google Sydney, which has area code "2".
    number.setCountryCode(61).setNationalNumber(293744000L);
    assertEquals(1, phoneUtil.getLengthOfGeographicalAreaCode(number));

    // Google Singapore. Singapore has no area code and no national prefix.
    number.setCountryCode(65).setNationalNumber(65218000L);
    assertEquals(0, phoneUtil.getLengthOfGeographicalAreaCode(number));
  }

  public void testGetLengthOfNationalDestinationCode() {
    PhoneNumber number = new PhoneNumber();
    // Google MTV, which has national destination code (NDC) "650".
    number.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(number));

    // A North America toll-free number, which has NDC "800".
    number.setCountryCode(1).setNationalNumber(8002530000L);
    assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(number));

    // Google London, which has NDC "20".
    number.setCountryCode(44).setNationalNumber(2070313000L);
    assertEquals(2, phoneUtil.getLengthOfNationalDestinationCode(number));

    // A UK mobile phone, which has NDC "7123".
    number.setCountryCode(44).setNationalNumber(7123456789L);
    assertEquals(4, phoneUtil.getLengthOfNationalDestinationCode(number));

    // Google Buenos Aires, which has NDC "11".
    number.setCountryCode(54).setNationalNumber(1155303000L);
    assertEquals(2, phoneUtil.getLengthOfNationalDestinationCode(number));

    // An Argentinian mobile which has NDC "911".
    number.setCountryCode(54).setNationalNumber(91155303001L);
    assertEquals(3, phoneUtil.getLengthOfNationalDestinationCode(number));

    // Google Sydney, which has NDC "2".
    number.setCountryCode(61).setNationalNumber(293744000L);
    assertEquals(1, phoneUtil.getLengthOfNationalDestinationCode(number));

    // Google Singapore, which has NDC "6521".
    number.setCountryCode(65).setNationalNumber(65218000L);
    assertEquals(4, phoneUtil.getLengthOfNationalDestinationCode(number));

    // An invalid US number (1 digit shorter), which has no NDC.
    number.setCountryCode(1).setNationalNumber(650253000L);
    assertEquals(0, phoneUtil.getLengthOfNationalDestinationCode(number));

    // A number containing an invalid country code, which shouldn't have any NDC.
    number.setCountryCode(123).setNationalNumber(6502530000L);
    assertEquals(0, phoneUtil.getLengthOfNationalDestinationCode(number));
  }

  public void testGetNationalSignificantNumber() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals("6502530000", PhoneNumberUtil.getNationalSignificantNumber(number));

    // An Italian mobile number.
    number.setCountryCode(39).setNationalNumber(312345678L);
    assertEquals("312345678", PhoneNumberUtil.getNationalSignificantNumber(number));

    // An Italian fixed line number.
    number.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    assertEquals("0236618300", PhoneNumberUtil.getNationalSignificantNumber(number));
  }

  public void testGetExampleNumber() {
    PhoneNumber deNumber = new PhoneNumber();
    deNumber.setCountryCode(49).setNationalNumber(30123456);
    assertEquals(deNumber, phoneUtil.getExampleNumber("DE"));
    assertEquals(deNumber, phoneUtil.getExampleNumber("de"));

    assertEquals(deNumber,
                 phoneUtil.getExampleNumberForType("DE",
                                                   PhoneNumberUtil.PhoneNumberType.FIXED_LINE));
    assertEquals(null,
                 phoneUtil.getExampleNumberForType("DE",
                                                   PhoneNumberUtil.PhoneNumberType.MOBILE));
    // For the US, the example number is placed under general description, and hence should be used
    // for both fixed line and mobile, so neither of these should return null.
    assertNotNull(phoneUtil.getExampleNumberForType("US",
                                                    PhoneNumberUtil.PhoneNumberType.FIXED_LINE));
    assertNotNull(phoneUtil.getExampleNumberForType("US",
                                                    PhoneNumberUtil.PhoneNumberType.MOBILE));
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
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals("650 253 0000", phoneUtil.format(usNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 650 253 0000", phoneUtil.format(usNumber, PhoneNumberFormat.INTERNATIONAL));

    usNumber.clear();
    usNumber.setCountryCode(1).setNationalNumber(8002530000L);
    assertEquals("800 253 0000", phoneUtil.format(usNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 800 253 0000", phoneUtil.format(usNumber, PhoneNumberFormat.INTERNATIONAL));

    usNumber.clear();
    usNumber.setCountryCode(1).setNationalNumber(9002530000L);
    assertEquals("900 253 0000", phoneUtil.format(usNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 900 253 0000", phoneUtil.format(usNumber, PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatBSNumber() {
    PhoneNumber bsNumber = new PhoneNumber();
    bsNumber.setCountryCode(1).setNationalNumber(2421234567L);
    assertEquals("242 123 4567", phoneUtil.format(bsNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 242 123 4567", phoneUtil.format(bsNumber, PhoneNumberFormat.INTERNATIONAL));

    bsNumber.clear();
    bsNumber.setCountryCode(1).setNationalNumber(8002530000L);
    assertEquals("800 253 0000", phoneUtil.format(bsNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 800 253 0000", phoneUtil.format(bsNumber, PhoneNumberFormat.INTERNATIONAL));

    bsNumber.clear();
    bsNumber.setCountryCode(1).setNationalNumber(9002530000L);
    assertEquals("900 253 0000", phoneUtil.format(bsNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+1 900 253 0000", phoneUtil.format(bsNumber, PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatGBNumber() {
    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(2087389353L);
    assertEquals("(020) 8738 9353", phoneUtil.format(gbNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+44 20 8738 9353", phoneUtil.format(gbNumber, PhoneNumberFormat.INTERNATIONAL));

    gbNumber.clear();
    gbNumber.setCountryCode(44).setNationalNumber(7912345678L);
    assertEquals("(07912) 345 678", phoneUtil.format(gbNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+44 7912 345 678", phoneUtil.format(gbNumber, PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatDENumber() {
    PhoneNumber deNumber = new PhoneNumber();
    deNumber.setCountryCode(49).setNationalNumber(301234L);
    assertEquals("030 1234", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 30 1234", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));

    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(291123L);
    assertEquals("0291 123", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 291 123", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));

    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(29112345678L);
    assertEquals("0291 12345678", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 291 12345678", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));

    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(9123123L);
    assertEquals("09123 123", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 9123 123", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));
    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(80212345L);
    assertEquals("08021 2345", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 8021 2345", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));
    deNumber.clear();
    deNumber.setCountryCode(49).setNationalNumber(1234L);
    // Note this number is correctly formatted without national prefix. Most of the numbers that
    // are treated as invalid numbers by the library are short numbers, and they are usually not
    // dialed with national prefix.
    assertEquals("1234", phoneUtil.format(deNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+49 1234", phoneUtil.format(deNumber, PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatITNumber() {
    PhoneNumber itNumber = new PhoneNumber();
    itNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    assertEquals("02 3661 8300", phoneUtil.format(itNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+39 02 3661 8300", phoneUtil.format(itNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+390236618300", phoneUtil.format(itNumber, PhoneNumberFormat.E164));

    itNumber.clear();
    itNumber.setCountryCode(39).setNationalNumber(345678901L);
    assertEquals("345 678 901", phoneUtil.format(itNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+39 345 678 901", phoneUtil.format(itNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+39345678901", phoneUtil.format(itNumber, PhoneNumberFormat.E164));
  }

  public void testFormatAUNumber() {
    PhoneNumber auNumber = new PhoneNumber();
    auNumber.setCountryCode(61).setNationalNumber(236618300L);
    assertEquals("02 3661 8300", phoneUtil.format(auNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+61 2 3661 8300", phoneUtil.format(auNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+61236618300", phoneUtil.format(auNumber, PhoneNumberFormat.E164));

    auNumber.clear();
    auNumber.setCountryCode(61).setNationalNumber(1800123456L);
    assertEquals("1800 123 456", phoneUtil.format(auNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+61 1800 123 456", phoneUtil.format(auNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+611800123456", phoneUtil.format(auNumber, PhoneNumberFormat.E164));
  }

  public void testFormatARNumber() {
    PhoneNumber arNumber = new PhoneNumber();
    arNumber.setCountryCode(54).setNationalNumber(1187654321L);
    assertEquals("011 8765-4321", phoneUtil.format(arNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+54 11 8765-4321", phoneUtil.format(arNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+541187654321", phoneUtil.format(arNumber, PhoneNumberFormat.E164));

    arNumber.clear();
    arNumber.setCountryCode(54).setNationalNumber(91187654321L);
    assertEquals("011 15 8765-4321", phoneUtil.format(arNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("+54 9 11 8765 4321", phoneUtil.format(arNumber, PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+5491187654321", phoneUtil.format(arNumber, PhoneNumberFormat.E164));
  }

  public void testFormatOutOfCountryCallingNumber() {
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(9002530000L);
    assertEquals("00 1 900 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(usNumber, "DE"));

    usNumber.clear();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals("1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(usNumber, "BS"));

    assertEquals("0~0 1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(usNumber, "PL"));

    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(7912345678L);
    assertEquals("011 44 7912 345 678",
                 phoneUtil.formatOutOfCountryCallingNumber(gbNumber, "US"));

    PhoneNumber deNumber = new PhoneNumber();
    deNumber.setCountryCode(49).setNationalNumber(1234L);
    assertEquals("00 49 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(deNumber, "GB"));
    // Note this number is correctly formatted without national prefix. Most of the numbers that
    // are treated as invalid numbers by the library are short numbers, and they are usually not
    // dialed with national prefix.
    assertEquals("1234",
                 phoneUtil.formatOutOfCountryCallingNumber(deNumber, "DE"));

    PhoneNumber itNumber = new PhoneNumber();
    itNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    assertEquals("011 39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(itNumber, "US"));
    assertEquals("02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(itNumber, "IT"));
    assertEquals("+39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(itNumber, "SG"));

    PhoneNumber sgNumber = new PhoneNumber();
    sgNumber.setCountryCode(65).setNationalNumber(94777892L);
    assertEquals("9477 7892",
                 phoneUtil.formatOutOfCountryCallingNumber(sgNumber, "SG"));

    PhoneNumber arNumber = new PhoneNumber();
    arNumber.setCountryCode(54).setNationalNumber(91187654321L);
    assertEquals("011 54 9 11 8765 4321",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumber, "US"));

    arNumber.setExtension("1234");
    assertEquals("011 54 9 11 8765 4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumber, "US"));
    assertEquals("0011 54 9 11 8765 4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumber, "AU"));
    assertEquals("011 15 8765-4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumber, "AR"));
  }

  public void testFormatOutOfCountryWithPreferredIntlPrefix() {
    PhoneNumber itNumber = new PhoneNumber();
    itNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    // This should use 0011, since that is the preferred international prefix (both 0011 and 0012
    // are accepted as possible international prefixes in our test metadta.)
    assertEquals("0011 39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(itNumber, "AU"));
  }

  public void testFormatWithCarrierCode() {
    // We only support this for AR in our test metadata.
    PhoneNumber arNumber = new PhoneNumber();
    arNumber.setCountryCode(54).setNationalNumber(91234125678L);
    assertEquals("01234 12-5678", phoneUtil.format(arNumber, PhoneNumberFormat.NATIONAL));
    // Test formatting with a carrier code.
    assertEquals("01234 15 12-5678", phoneUtil.formatNationalNumberWithCarrierCode(arNumber, "15"));
    // Here the international rule is used, so no carrier code should be present.
    assertEquals("+5491234125678", phoneUtil.format(arNumber, PhoneNumberFormat.E164));
    // We don't support this for the US so there should be no change.
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(4241231234L);
    assertEquals("424 123 1234", phoneUtil.format(usNumber, PhoneNumberFormat.NATIONAL));
    assertEquals("424 123 1234", phoneUtil.formatNationalNumberWithCarrierCode(usNumber, "15"));
  }

  public void testFormatByPattern() {
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);

    NumberFormat newNumFormat = new NumberFormat();
    newNumFormat.setPattern("(\\d{3})(\\d{3})(\\d{4})");
    newNumFormat.setFormat("($1) $2-$3");
    List<NumberFormat> newNumberFormats = new ArrayList<NumberFormat>();
    newNumberFormats.add(newNumFormat);

    assertEquals("(650) 253-0000", phoneUtil.formatByPattern(usNumber, PhoneNumberFormat.NATIONAL,
                                                             newNumberFormats));
    assertEquals("+1 (650) 253-0000", phoneUtil.formatByPattern(usNumber,
                                                                PhoneNumberFormat.INTERNATIONAL,
                                                                newNumberFormats));

    // $NP is set to '1' for the US. Here we check that for other NANPA countries the US rules are
    // followed.
    newNumFormat.setNationalPrefixFormattingRule("$NP ($FG)");
    newNumFormat.setFormat("$1 $2-$3");
    PhoneNumber bsNumber = new PhoneNumber();
    bsNumber.setCountryCode(1).setNationalNumber(4168819999L);
    assertEquals("1 (416) 881-9999",
                 phoneUtil.formatByPattern(bsNumber, PhoneNumberFormat.NATIONAL, newNumberFormats));
    assertEquals("+1 416 881-9999",
                 phoneUtil.formatByPattern(bsNumber, PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));

    PhoneNumber itNumber = new PhoneNumber();
    itNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);

    newNumFormat.setPattern("(\\d{2})(\\d{5})(\\d{3})");
    newNumFormat.setFormat("$1-$2 $3");
    newNumberFormats.set(0, newNumFormat);

    assertEquals("02-36618 300",
                 phoneUtil.formatByPattern(itNumber, PhoneNumberFormat.NATIONAL, newNumberFormats));
    assertEquals("+39 02-36618 300",
                 phoneUtil.formatByPattern(itNumber, PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));

    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(2012345678L);

    newNumFormat.setNationalPrefixFormattingRule("$NP$FG");
    newNumFormat.setPattern("(\\d{2})(\\d{4})(\\d{4})");
    newNumFormat.setFormat("$1 $2 $3");
    newNumberFormats.set(0, newNumFormat);
    assertEquals("020 1234 5678",
                 phoneUtil.formatByPattern(gbNumber, PhoneNumberFormat.NATIONAL, newNumberFormats));

    newNumFormat.setNationalPrefixFormattingRule("($NP$FG)");
    assertEquals("(020) 1234 5678",
                 phoneUtil.formatByPattern(gbNumber, PhoneNumberFormat.NATIONAL, newNumberFormats));

    newNumFormat.setNationalPrefixFormattingRule("");
    assertEquals("20 1234 5678",
                 phoneUtil.formatByPattern(gbNumber, PhoneNumberFormat.NATIONAL, newNumberFormats));

    newNumFormat.setNationalPrefixFormattingRule("");
    assertEquals("+44 20 1234 5678",
                 phoneUtil.formatByPattern(gbNumber, PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));
  }

  public void testFormatE164Number() {
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals("+16502530000", phoneUtil.format(usNumber, PhoneNumberFormat.E164));
    PhoneNumber deNumber = new PhoneNumber();
    deNumber.setCountryCode(49).setNationalNumber(301234L);
    assertEquals("+49301234", phoneUtil.format(deNumber, PhoneNumberFormat.E164));
  }

  public void testFormatNumberWithExtension() {
    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L).setExtension("1234");
    // Uses default extension prefix:
    assertEquals("03-331 6005 ext. 1234", phoneUtil.format(nzNumber, PhoneNumberFormat.NATIONAL));
    // Extension prefix overridden in the territory information for the US:
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L).setExtension("4567");
    assertEquals("650 253 0000 extn. 4567", phoneUtil.format(usNumber, PhoneNumberFormat.NATIONAL));
  }

  public void testFormatUsingOriginalNumberFormat() throws Exception {
    PhoneNumber number1 = phoneUtil.parseAndKeepRawInput("+442087654321", "GB");
    assertEquals("+44 20 8765 4321", phoneUtil.formatInOriginalFormat(number1, "GB"));

    PhoneNumber number2 = phoneUtil.parseAndKeepRawInput("02087654321", "GB");
    assertEquals("(020) 8765 4321", phoneUtil.formatInOriginalFormat(number2, "GB"));

    PhoneNumber number3 = phoneUtil.parseAndKeepRawInput("011442087654321", "US");
    assertEquals("011 44 20 8765 4321", phoneUtil.formatInOriginalFormat(number3, "US"));

    PhoneNumber number4 = phoneUtil.parseAndKeepRawInput("442087654321", "GB");
    assertEquals("44 20 8765 4321", phoneUtil.formatInOriginalFormat(number4, "GB"));

    PhoneNumber number5 = phoneUtil.parse("+442087654321", "GB");
    assertEquals("(020) 8765 4321", phoneUtil.formatInOriginalFormat(number5, "GB"));
  }

  public void testIsPremiumRate() {
    PhoneNumber premiumRateNumber = new PhoneNumber();

    premiumRateNumber.setCountryCode(1).setNationalNumber(9004433030L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber));

    premiumRateNumber.clear();
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
  }

  public void testIsMobile() {
    PhoneNumber mobileNumber = new PhoneNumber();

    // A Bahama mobile number
    mobileNumber.setCountryCode(1).setNationalNumber(2423570000L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber));

    mobileNumber.clear();
    mobileNumber.setCountryCode(39).setNationalNumber(312345678L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber));

    mobileNumber.clear();
    mobileNumber.setCountryCode(44).setNationalNumber(7912345678L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber));

    mobileNumber.clear();
    mobileNumber.setCountryCode(49).setNationalNumber(15123456789L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber));

    mobileNumber.clear();
    mobileNumber.setCountryCode(54).setNationalNumber(91187654321L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber));
  }

  public void testIsFixedLine() {
    PhoneNumber fixedLineNumber = new PhoneNumber();

    // A Bahama fixed-line number
    fixedLineNumber.setCountryCode(1).setNationalNumber(2423651234L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE,
                 phoneUtil.getNumberType(fixedLineNumber));

    // An Italian fixed-line number
    fixedLineNumber.clear();
    fixedLineNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE,
                 phoneUtil.getNumberType(fixedLineNumber));

    fixedLineNumber.clear();
    fixedLineNumber.setCountryCode(44).setNationalNumber(2012345678L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE,
                 phoneUtil.getNumberType(fixedLineNumber));

    fixedLineNumber.clear();
    fixedLineNumber.setCountryCode(49).setNationalNumber(301234L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE,
                 phoneUtil.getNumberType(fixedLineNumber));
  }

  public void testIsFixedLineAndMobile() {
    PhoneNumber fixedLineAndMobileNumber = new PhoneNumber();
    fixedLineAndMobileNumber.setCountryCode(1).setNationalNumber(6502531111L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE,
                 phoneUtil.getNumberType(fixedLineAndMobileNumber));

    fixedLineAndMobileNumber.clear();
    fixedLineAndMobileNumber.setCountryCode(54).setNationalNumber(1987654321L);
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
    PhoneNumber unknownNumber = new PhoneNumber();
    unknownNumber.setCountryCode(1).setNationalNumber(65025311111L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.UNKNOWN,
                 phoneUtil.getNumberType(unknownNumber));
  }

  public void testIsValidNumber() {
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertTrue(phoneUtil.isValidNumber(usNumber));

    PhoneNumber itNumber = new PhoneNumber();
    itNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    assertTrue(phoneUtil.isValidNumber(itNumber));

    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(7912345678L);
    assertTrue(phoneUtil.isValidNumber(gbNumber));

    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(21387835L);
    assertTrue(phoneUtil.isValidNumber(nzNumber));
  }

  public void testIsValidForRegion() {
    // This number is valid for the Bahamas, but is not a valid US number.
    PhoneNumber bsNumber = new PhoneNumber();
    bsNumber.setCountryCode(1).setNationalNumber(2423232345L);
    assertTrue(phoneUtil.isValidNumber(bsNumber));
    assertTrue(phoneUtil.isValidNumberForRegion(bsNumber, "BS"));
    assertTrue(phoneUtil.isValidNumberForRegion(bsNumber, "bs"));
    assertFalse(phoneUtil.isValidNumberForRegion(bsNumber, "US"));
    bsNumber.setNationalNumber(2421232345L);
    // This number is no longer valid.
    assertFalse(phoneUtil.isValidNumber(bsNumber));

    // La Mayotte and Reunion use 'leadingDigits' to differentiate them.
    PhoneNumber reNumber = new PhoneNumber();
    reNumber.setCountryCode(262).setNationalNumber(262123456L);
    assertTrue(phoneUtil.isValidNumber(reNumber));
    assertTrue(phoneUtil.isValidNumberForRegion(reNumber, "RE"));
    assertFalse(phoneUtil.isValidNumberForRegion(reNumber, "YT"));
    // Now change the number to be a number for La Mayotte.
    reNumber.setNationalNumber(269601234L);
    assertTrue(phoneUtil.isValidNumberForRegion(reNumber, "YT"));
    assertFalse(phoneUtil.isValidNumberForRegion(reNumber, "RE"));
    // This number is no longer valid for La Reunion.
    reNumber.setNationalNumber(269123456L);
    assertFalse(phoneUtil.isValidNumberForRegion(reNumber, "YT"));
    assertFalse(phoneUtil.isValidNumberForRegion(reNumber, "RE"));
    assertFalse(phoneUtil.isValidNumber(reNumber));
    // However, it should be recognised as from La Mayotte, since it is valid for this region.
    assertEquals("YT", phoneUtil.getRegionCodeForNumber(reNumber));
    // This number is valid in both places.
    reNumber.setNationalNumber(800123456L);
    assertTrue(phoneUtil.isValidNumberForRegion(reNumber, "YT"));
    assertTrue(phoneUtil.isValidNumberForRegion(reNumber, "RE"));
  }

  public void testIsNotValidNumber() {
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(2530000L);
    assertFalse(phoneUtil.isValidNumber(usNumber));

    PhoneNumber itNumber = new PhoneNumber();
    itNumber.setCountryCode(39).setNationalNumber(23661830000L).setItalianLeadingZero(true);
    assertFalse(phoneUtil.isValidNumber(itNumber));

    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(791234567L);
    assertFalse(phoneUtil.isValidNumber(gbNumber));

    PhoneNumber deNumber = new PhoneNumber();
    deNumber.setCountryCode(49).setNationalNumber(1234L);
    assertFalse(phoneUtil.isValidNumber(deNumber));

    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(3316005L);
    assertFalse(phoneUtil.isValidNumber(nzNumber));
  }

  public void testGetRegionCodeForCountryCode() {
    assertEquals("US", phoneUtil.getRegionCodeForCountryCode(1));
    assertEquals("GB", phoneUtil.getRegionCodeForCountryCode(44));
    assertEquals("DE", phoneUtil.getRegionCodeForCountryCode(49));
  }

  public void testGetRegionCodeForNumber() {
    PhoneNumber bsNumber = new PhoneNumber();
    bsNumber.setCountryCode(1).setNationalNumber(2423027000L);
    assertEquals("BS", phoneUtil.getRegionCodeForNumber(bsNumber));

    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals("US", phoneUtil.getRegionCodeForNumber(usNumber));

    PhoneNumber gbNumber = new PhoneNumber();
    gbNumber.setCountryCode(44).setNationalNumber(7912345678L);
    assertEquals("GB", phoneUtil.getRegionCodeForNumber(gbNumber));
  }

  public void testGetCountryCodeForRegion() {
    assertEquals(1, phoneUtil.getCountryCodeForRegion("US"));
    assertEquals(64, phoneUtil.getCountryCodeForRegion("NZ"));
    assertEquals(64, phoneUtil.getCountryCodeForRegion("nz"));
    assertEquals(0, phoneUtil.getCountryCodeForRegion(null));
    assertEquals(0, phoneUtil.getCountryCodeForRegion("ZZ"));
    // CS is already deprecated so the library doesn't support it.
    assertEquals(0, phoneUtil.getCountryCodeForRegion("CS"));
  }

  public void testGetNationalDiallingPrefixForRegion() {
    assertEquals("1", phoneUtil.getNddPrefixForRegion("US", false));
    // Test non-main country to see it gets the national dialling prefix for the main country with
    // that country calling code.
    assertEquals("1", phoneUtil.getNddPrefixForRegion("BS", false));
    assertEquals("0", phoneUtil.getNddPrefixForRegion("NZ", false));
    // Test case with non digit in the national prefix.
    assertEquals("0~0", phoneUtil.getNddPrefixForRegion("AO", false));
    assertEquals("00", phoneUtil.getNddPrefixForRegion("AO", true));
    // Test cases with invalid regions.
    assertEquals(null, phoneUtil.getNddPrefixForRegion(null, false));
    assertEquals(null, phoneUtil.getNddPrefixForRegion("ZZ", false));
    // CS is already deprecated so the library doesn't support it.
    assertEquals(null, phoneUtil.getNddPrefixForRegion("CS", false));
  }

  public void testIsNANPACountry() {
    assertTrue(phoneUtil.isNANPACountry("US"));
    assertTrue(phoneUtil.isNANPACountry("BS"));
    assertTrue(phoneUtil.isNANPACountry("bs"));
  }

  public void testIsPossibleNumber() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(6502530000L);
    assertTrue(phoneUtil.isPossibleNumber(number));

    number.clear();
    number.setCountryCode(1).setNationalNumber(2530000L);
    assertTrue(phoneUtil.isPossibleNumber(number));

    number.clear();
    number.setCountryCode(44).setNationalNumber(2070313000L);
    assertTrue(phoneUtil.isPossibleNumber(number));

    assertTrue(phoneUtil.isPossibleNumber("+1 650 253 0000", "US"));
    assertTrue(phoneUtil.isPossibleNumber("+1 650 GOO OGLE", "US"));
    assertTrue(phoneUtil.isPossibleNumber("(650) 253-0000", "US"));
    assertTrue(phoneUtil.isPossibleNumber("253-0000", "US"));
    assertTrue(phoneUtil.isPossibleNumber("+1 650 253 0000", "GB"));
    assertTrue(phoneUtil.isPossibleNumber("+44 20 7031 3000", "GB"));
    assertTrue(phoneUtil.isPossibleNumber("(020) 7031 3000", "GB"));
    assertTrue(phoneUtil.isPossibleNumber("7031 3000", "GB"));
    assertTrue(phoneUtil.isPossibleNumber("3331 6005", "NZ"));
    assertTrue(phoneUtil.isPossibleNumber("3331 6005", "nz"));
  }

  public void testIsPossibleNumberWithReason() {
    // FYI, national numbers for country code +1 that are within 7 to 10 digits are possible.
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(number));

    number.clear();
    number.setCountryCode(1).setNationalNumber(2530000L);
    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(number));

    number.clear();
    number.setCountryCode(0).setNationalNumber(2530000L);
    assertEquals(PhoneNumberUtil.ValidationResult.INVALID_COUNTRY_CODE,
                 phoneUtil.isPossibleNumberWithReason(number));

    number.clear();
    number.setCountryCode(1).setNationalNumber(253000L);
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_SHORT,
                 phoneUtil.isPossibleNumberWithReason(number));

    number.clear();
    number.setCountryCode(1).setNationalNumber(65025300000L);
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_LONG,
                 phoneUtil.isPossibleNumberWithReason(number));

    // Try with number that we don't have metadata for.
    PhoneNumber adNumber = new PhoneNumber();
    adNumber.setCountryCode(376).setNationalNumber(12345L);
    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(adNumber));
    adNumber.setCountryCode(376).setNationalNumber(13L);
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_SHORT,
                 phoneUtil.isPossibleNumberWithReason(adNumber));
    adNumber.setCountryCode(376).setNationalNumber(1234567890123456L);
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_LONG,
                 phoneUtil.isPossibleNumberWithReason(adNumber));
  }

  public void testIsNotPossibleNumber() {
    PhoneNumber number = new PhoneNumber();
    number.setCountryCode(1).setNationalNumber(65025300000L);
    assertFalse(phoneUtil.isPossibleNumber(number));

    number.clear();
    number.setCountryCode(1).setNationalNumber(253000L);
    assertFalse(phoneUtil.isPossibleNumber(number));

    number.clear();
    number.setCountryCode(44).setNationalNumber(300L);
    assertFalse(phoneUtil.isPossibleNumber(number));

    assertFalse(phoneUtil.isPossibleNumber("+1 650 253 00000", "US"));
    assertFalse(phoneUtil.isPossibleNumber("(650) 253-00000", "US"));
    assertFalse(phoneUtil.isPossibleNumber("I want a Pizza", "US"));
    assertFalse(phoneUtil.isPossibleNumber("253-000", "US"));
    assertFalse(phoneUtil.isPossibleNumber("1 3000", "GB"));
    assertFalse(phoneUtil.isPossibleNumber("+44 300", "GB"));
  }

  public void testTruncateTooLongNumber() {
    // US number 650-253-0000, but entered with one additional digit at the end.
    PhoneNumber tooLongNumber = new PhoneNumber();
    tooLongNumber.setCountryCode(1).setNationalNumber(65025300001L);
    PhoneNumber validNumber = new PhoneNumber();
    validNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertTrue(phoneUtil.truncateTooLongNumber(tooLongNumber));
    assertEquals(validNumber, tooLongNumber);

    // GB number 080 1234 5678, but entered with 4 extra digits at the end.
    tooLongNumber.clear();
    tooLongNumber.setCountryCode(44).setNationalNumber(80123456780123L);
    validNumber.clear();
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

    // Tests what happens when a valid number is passed in.
    PhoneNumber validNumberCopy = new PhoneNumber();
    validNumberCopy.mergeFrom(validNumber);
    assertTrue(phoneUtil.truncateTooLongNumber(validNumber));
    // Tests the number is not modified.
    assertEquals(validNumberCopy, validNumber);

    // Tests what happens when a number with invalid prefix is passed in.
    PhoneNumber numberWithInvalidPrefix = new PhoneNumber();
    // The test metadata says US numbers cannot have prefix 240.
    numberWithInvalidPrefix.setCountryCode(1).setNationalNumber(2401234567L);
    PhoneNumber invalidNumberCopy = new PhoneNumber();
    invalidNumberCopy.mergeFrom(numberWithInvalidPrefix);
    assertFalse(phoneUtil.truncateTooLongNumber(numberWithInvalidPrefix));
    // Tests the number is not modified.
    assertEquals(invalidNumberCopy, numberWithInvalidPrefix);

    // Tests what happens when a too short number is passed in.
    PhoneNumber tooShortNumber = new PhoneNumber();
    tooShortNumber.setCountryCode(1).setNationalNumber(1234L);
    PhoneNumber tooShortNumberCopy = new PhoneNumber();
    tooShortNumberCopy.mergeFrom(tooShortNumber);
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
    String nationalPrefix = "34";
    StringBuffer numberToStrip = new StringBuffer("34356778");
    String strippedNumber = "356778";
    String nationalRuleRegExp = "\\d{4,7}";
    Pattern nationalRule = Pattern.compile(nationalRuleRegExp);
    phoneUtil.maybeStripNationalPrefix(numberToStrip, nationalPrefix, "", nationalRule);
    assertEquals("Should have had national prefix stripped.",
                 strippedNumber, numberToStrip.toString());
    // Retry stripping - now the number should not start with the national prefix, so no more
    // stripping should occur.
    phoneUtil.maybeStripNationalPrefix(numberToStrip, nationalPrefix, "", nationalRule);
    assertEquals("Should have had no change - no national prefix present.",
                 strippedNumber, numberToStrip.toString());
    // Some countries have no national prefix. Repeat test with none specified.
    nationalPrefix = "";
    phoneUtil.maybeStripNationalPrefix(numberToStrip, nationalPrefix, "", nationalRule);
    assertEquals("Should not strip anything with empty national prefix.",
                 strippedNumber, numberToStrip.toString());
    // If the resultant number doesn't match the national rule, it shouldn't be stripped.
    nationalPrefix = "3";
    numberToStrip = new StringBuffer("3123");
    strippedNumber = "3123";
    phoneUtil.maybeStripNationalPrefix(numberToStrip, nationalPrefix, "", nationalRule);
    assertEquals("Should have had no change - after stripping, it wouldn't have matched " +
                 "the national rule.",
                 strippedNumber, numberToStrip.toString());
  }

  public void testMaybeStripInternationalPrefix() {
    String internationalPrefix = "00[39]";
    StringBuffer numberToStrip = new StringBuffer("0034567700-3898003");
    // Note the dash is removed as part of the normalization.
    StringBuffer strippedNumber = new StringBuffer("45677003898003");
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

    numberToStrip = new StringBuffer("00945677003898003");
    assertEquals(CountryCodeSource.FROM_NUMBER_WITH_IDD,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
    assertEquals("The number supplied was not stripped of its international prefix.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Test it works when the international prefix is broken up by spaces.
    numberToStrip = new StringBuffer("00 9 45677003898003");
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
    numberToStrip = new StringBuffer("+45677003898003");
    strippedNumber = new StringBuffer("45677003898003");
    assertEquals(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
    assertEquals("The number supplied was not stripped of the plus symbol.",
                 strippedNumber.toString(), numberToStrip.toString());

    // If the number afterwards is a zero, we should not strip this - no country code begins with 0.
    numberToStrip = new StringBuffer("0090112-3123");
    strippedNumber = new StringBuffer("00901123123");
    assertEquals(CountryCodeSource.FROM_DEFAULT_COUNTRY,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
    assertEquals("The number supplied had a 0 after the match so shouldn't be stripped.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Here the 0 is separated by a space from the IDD.
    numberToStrip = new StringBuffer("009 0-112-3123");
    assertEquals(CountryCodeSource.FROM_DEFAULT_COUNTRY,
                 phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                     internationalPrefix));
  }

  public void testMaybeExtractCountryCode() {
    PhoneNumber number = new PhoneNumber();
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion("US");
    // Note that for the US, the IDD is 011.
    try {
      String phoneNumber = "011112-3456789";
      String strippedNumber = "123456789";
      int countryCode = 1;
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Did not extract country code " + countryCode + " correctly.",
                   countryCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true,
                                                     number));
      assertEquals("Did not figure out CountryCodeSource correctly",
                   CountryCodeSource.FROM_NUMBER_WITH_IDD, number.getCountryCodeSource());
      // Should strip and normalize national significant number.
      assertEquals("Did not strip off the country code correctly.",
                   strippedNumber,
                   numberToFill.toString());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "+6423456789";
      int countryCode = 64;
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Did not extract country code " + countryCode + " correctly.",
                   countryCode,
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
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should not have extracted a country code - no international prefix present.",
                   0,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true,
                                                     number));
      assertEquals("Did not figure out CountryCodeSource correctly",
                   CountryCodeSource.FROM_DEFAULT_COUNTRY, number.getCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "0119991123456789";
      StringBuffer numberToFill = new StringBuffer();
      phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, true, number);
      fail("Should have thrown an exception, no valid country code present.");
    } catch (NumberParseException e) {
      // Expected.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    number.clear();
    try {
      String phoneNumber = "(1 610) 619 4466";
      int countryCode = 1;
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should have extracted the country code of the region passed in",
                   countryCode,
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
      int countryCode = 1;
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should have extracted the country code of the region passed in",
                   countryCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, false,
                                                     number));
      assertFalse("Should not contain CountryCodeSource.", number.hasCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "(1 610) 619 446";
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should not have extracted a country code - invalid number after extraction " +
                   "of uncertain country code.",
                   0,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill, false,
                                                     number));
      assertFalse("Should not contain CountryCodeSource.", number.hasCountryCodeSource());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    number.clear();
    try {
      String phoneNumber = "(1 610) 619 43";
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should not have extracted a country code - invalid number both before and " +
                   "after extraction of uncertain country code.",
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
    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L);

    // National prefix attached.
    assertEquals(nzNumber, phoneUtil.parse("033316005", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("033316005", "nz"));
    assertEquals(nzNumber, phoneUtil.parse("33316005", "NZ"));
    // National prefix attached and some formatting present.
    assertEquals(nzNumber, phoneUtil.parse("03-331 6005", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("03 331 6005", "NZ"));

    // Testing international prefixes.
    // Should strip country code.
    assertEquals(nzNumber, phoneUtil.parse("0064 3 331 6005", "NZ"));
    // Try again, but this time we have an international number with Region Code US. It should
    // recognise the country code and parse accordingly.
    assertEquals(nzNumber, phoneUtil.parse("01164 3 331 6005", "US"));
    assertEquals(nzNumber, phoneUtil.parse("+64 3 331 6005", "US"));

    nzNumber.clear();
    nzNumber.setCountryCode(64).setNationalNumber(64123456L);
    assertEquals(nzNumber, phoneUtil.parse("64(0)64123456", "NZ"));
    // Check that using a "/" is fine in a phone number.
    PhoneNumber deNumber = new PhoneNumber();
    deNumber.setCountryCode(49).setNationalNumber(12345678L);
    assertEquals(deNumber, phoneUtil.parse("123/45678", "DE"));

    PhoneNumber usNumber = new PhoneNumber();
    // Check it doesn't use the '1' as a country code when parsing if the phone number was already
    // possible.
    usNumber.setCountryCode(1).setNationalNumber(1234567890L);
    assertEquals(usNumber, phoneUtil.parse("123-456-7890", "US"));
  }

  public void testParseNumberWithAlphaCharacters() throws Exception {
    // Test case with alpha characters.
    PhoneNumber tollfreeNumber = new PhoneNumber();
    tollfreeNumber.setCountryCode(64).setNationalNumber(800332005L);
    assertEquals(tollfreeNumber, phoneUtil.parse("0800 DDA 005", "NZ"));
    PhoneNumber premiumNumber = new PhoneNumber();
    premiumNumber.setCountryCode(64).setNationalNumber(9003326005L);
    assertEquals(premiumNumber, phoneUtil.parse("0900 DDA 6005", "NZ"));
    // Not enough alpha characters for them to be considered intentional, so they are stripped.
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 6005a", "NZ"));
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 600a5", "NZ"));
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 600A5", "NZ"));
    assertEquals(premiumNumber, phoneUtil.parse("0900 a332 600A5", "NZ"));
  }

  public void testParseWithInternationalPrefixes() throws Exception {
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(6503336000L);
    assertEquals(usNumber, phoneUtil.parse("+1 (650) 333-6000", "NZ"));
    assertEquals(usNumber, phoneUtil.parse("1-650-333-6000", "US"));
    // Calling the US number from Singapore by using different service providers
    // 1st test: calling using SingTel IDD service (IDD is 001)
    assertEquals(usNumber, phoneUtil.parse("0011-650-333-6000", "SG"));
    // 2nd test: calling using StarHub IDD service (IDD is 008)
    assertEquals(usNumber, phoneUtil.parse("0081-650-333-6000", "SG"));
    // 3rd test: calling using SingTel V019 service (IDD is 019)
    assertEquals(usNumber, phoneUtil.parse("0191-650-333-6000", "SG"));
    // Calling the US number from Poland
    assertEquals(usNumber, phoneUtil.parse("0~01-650-333-6000", "PL"));
    // Using "++" at the start.
    assertEquals(usNumber, phoneUtil.parse("++1 (650) 333-6000", "PL"));
    // Using a full-width plus sign.
    assertEquals(usNumber, phoneUtil.parse("\uFF0B1 (650) 333-6000", "SG"));
    // The whole number, including punctuation, is here represented in full-width form.
    assertEquals(usNumber, phoneUtil.parse("\uFF0B\uFF11\u3000\uFF08\uFF16\uFF15\uFF10\uFF09" +
                                           "\u3000\uFF13\uFF13\uFF13\uFF0D\uFF16\uFF10\uFF10\uFF10",
                                           "SG"));
    // Using U+30FC dash instead.
    assertEquals(usNumber, phoneUtil.parse("\uFF0B\uFF11\u3000\uFF08\uFF16\uFF15\uFF10\uFF09" +
                                           "\u3000\uFF13\uFF13\uFF13\u30FC\uFF16\uFF10\uFF10\uFF10",
                                           "SG"));
  }

  public void testParseWithLeadingZero() throws Exception {
    PhoneNumber itNumber = new PhoneNumber();
    itNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    assertEquals(itNumber, phoneUtil.parse("+39 02-36618 300", "NZ"));
    assertEquals(itNumber, phoneUtil.parse("02-36618 300", "IT"));

    itNumber.clear();
    itNumber.setCountryCode(39).setNationalNumber(312345678L);
    assertEquals(itNumber, phoneUtil.parse("312 345 678", "IT"));
  }

  public void testParseNationalNumberArgentina() throws Exception {
    // Test parsing mobile numbers of Argentina.
    PhoneNumber arNumber = new PhoneNumber();

    arNumber.setCountryCode(54).setNationalNumber(93435551212L);
    assertEquals(arNumber, phoneUtil.parse("+54 9 343 555 1212", "AR"));
    assertEquals(arNumber, phoneUtil.parse("0343 15 555 1212", "AR"));

    arNumber.clear();
    arNumber.setCountryCode(54).setNationalNumber(93715654320L);
    assertEquals(arNumber, phoneUtil.parse("+54 9 3715 65 4320", "AR"));
    assertEquals(arNumber, phoneUtil.parse("03715 15 65 4320", "AR"));

    // Test parsing fixed-line numbers of Argentina.
    arNumber.clear();
    arNumber.setCountryCode(54).setNationalNumber(1137970000L);
    assertEquals(arNumber, phoneUtil.parse("+54 11 3797 0000", "AR"));
    assertEquals(arNumber, phoneUtil.parse("011 3797 0000", "AR"));

    arNumber.clear();
    arNumber.setCountryCode(54).setNationalNumber(3715654321L);
    assertEquals(arNumber, phoneUtil.parse("+54 3715 65 4321", "AR"));
    assertEquals(arNumber, phoneUtil.parse("03715 65 4321", "AR"));

    arNumber.clear();
    arNumber.setCountryCode(54).setNationalNumber(2312340000L);
    assertEquals(arNumber, phoneUtil.parse("+54 23 1234 0000", "AR"));
    assertEquals(arNumber, phoneUtil.parse("023 1234 0000", "AR"));
  }

  public void testParseWithXInNumber() throws Exception {
    // Test that having an 'x' in the phone number at the start is ok and that it just gets removed.
    PhoneNumber arNumber = new PhoneNumber();
    arNumber.setCountryCode(54).setNationalNumber(123456789L);
    assertEquals(arNumber, phoneUtil.parse("0123456789", "AR"));
    assertEquals(arNumber, phoneUtil.parse("(0) 123456789", "AR"));
    assertEquals(arNumber, phoneUtil.parse("0 123456789", "AR"));
    assertEquals(arNumber, phoneUtil.parse("(0xx) 123456789", "AR"));
    PhoneNumber arFromUs = new PhoneNumber();
    arFromUs.setCountryCode(54).setNationalNumber(81429712L);
    // This test is intentionally constructed such that the number of digit after xx is larger than
    // 7, so that the number won't be mistakenly treated as an extension, as we allow extensions up
    // to 7 digits. This assumption is okay for now as all the countries where a carrier selection
    // code is written in the form of xx have a national significant number of length larger than 7.
    assertEquals(arFromUs, phoneUtil.parse("011xx5481429712", "US"));
  }

  public void testParseNumbersMexico() throws Exception {
    // Test parsing fixed-line numbers of Mexico.
    PhoneNumber mxNumber = new PhoneNumber();
    mxNumber.setCountryCode(52).setNationalNumber(4499780001L);
    assertEquals(mxNumber, phoneUtil.parse("+52 (449)978-0001", "MX"));
    assertEquals(mxNumber, phoneUtil.parse("01 (449)978-0001", "MX"));
    assertEquals(mxNumber, phoneUtil.parse("(449)978-0001", "MX"));

    // Test parsing mobile numbers of Mexico.
    mxNumber.clear();
    mxNumber.setCountryCode(52).setNationalNumber(13312345678L);
    assertEquals(mxNumber, phoneUtil.parse("+52 1 33 1234-5678", "MX"));
    assertEquals(mxNumber, phoneUtil.parse("044 (33) 1234-5678", "MX"));
    assertEquals(mxNumber, phoneUtil.parse("045 33 1234-5678", "MX"));
  }

  public void testFailedParseOnInvalidNumbers() {
    try {
      String sentencePhoneNumber = "This is not a phone number";
      phoneUtil.parse(sentencePhoneNumber, "NZ");
      fail("This should not parse without throwing an exception " + sentencePhoneNumber);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.NOT_A_NUMBER,
                   e.getErrorType());
    }
    try {
      String tooLongPhoneNumber = "01495 72553301873 810104";
      phoneUtil.parse(tooLongPhoneNumber, "GB");
      fail("This should not parse without throwing an exception " + tooLongPhoneNumber);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_LONG,
                   e.getErrorType());
    }
    try {
      String plusMinusPhoneNumber = "+---";
      phoneUtil.parse(plusMinusPhoneNumber, "DE");
      fail("This should not parse without throwing an exception " + plusMinusPhoneNumber);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.NOT_A_NUMBER,
                   e.getErrorType());
    }
    try {
      String tooShortPhoneNumber = "+49 0";
      phoneUtil.parse(tooShortPhoneNumber, "DE");
      fail("This should not parse without throwing an exception " + tooShortPhoneNumber);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_NSN,
                   e.getErrorType());
    }
    try {
      String invalidCountryCode = "+210 3456 56789";
      phoneUtil.parse(invalidCountryCode, "NZ");
      fail("This is not a recognised country code: should fail: " + invalidCountryCode);
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String someNumber = "123 456 7890";
      phoneUtil.parse(someNumber, "YY");
      fail("'Unknown' country code not allowed: should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String someNumber = "123 456 7890";
      phoneUtil.parse(someNumber, "CS");
      fail("Deprecated country code not allowed: should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String someNumber = "123 456 7890";
      phoneUtil.parse(someNumber, null);
      fail("Null country code not allowed: should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String someNumber = "0044------";
      phoneUtil.parse(someNumber, "GB");
      fail("No number provided, only country code: should fail");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                   e.getErrorType());
    }
    try {
      String someNumber = "0044";
      phoneUtil.parse(someNumber, "GB");
      fail("No number provided, only country code: should fail");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                   e.getErrorType());
    }
    try {
      String someNumber = "011";
      phoneUtil.parse(someNumber, "US");
      fail("Only IDD provided - should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
                   e.getErrorType());
    }
    try {
      String someNumber = "0119";
      phoneUtil.parse(someNumber, "US");
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
      phoneUtil.parse(emptyNumber, "ZZ");
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
      phoneUtil.parse(nullNumber, "ZZ");
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
      phoneUtil.parse(nullNumber, "US");
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
    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L);
    // "ZZ" is allowed only if the number starts with a '+' - then the country code can be
    // calculated.
    assertEquals(nzNumber, phoneUtil.parse("+64 3 331 6005", "ZZ"));
    // Test with full-width plus.
    assertEquals(nzNumber, phoneUtil.parse("\uFF0B64 3 331 6005", "ZZ"));
    // Test with normal plus but leading characters that need to be stripped.
    assertEquals(nzNumber, phoneUtil.parse("Tel: +64 3 331 6005", "ZZ"));
    assertEquals(nzNumber, phoneUtil.parse("+64 3 331 6005", null));
    nzNumber.setRawInput("+64 3 331 6005").
        setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN);
    assertEquals(nzNumber, phoneUtil.parseAndKeepRawInput("+64 3 331 6005", "ZZ"));
    // Null is also allowed for the region code in these cases.
    assertEquals(nzNumber, phoneUtil.parseAndKeepRawInput("+64 3 331 6005", null));
  }

  public void testParseExtensions() throws Exception {
    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L).setExtension("3456");
    assertEquals(nzNumber, phoneUtil.parse("03 331 6005 ext 3456", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("03-3316005x3456", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("03-3316005 int.3456", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("03 3316005 #3456", "NZ"));
    // Test the following do not extract extensions:
    PhoneNumber nonExtnNumber = new PhoneNumber();
    nonExtnNumber.setCountryCode(1).setNationalNumber(80074935247L);
    assertEquals(nonExtnNumber, phoneUtil.parse("1800 six-flags", "US"));
    assertEquals(nonExtnNumber, phoneUtil.parse("1800 SIX FLAGS", "US"));
    assertEquals(nonExtnNumber, phoneUtil.parse("0~0 1800 7493 5247", "PL"));
    assertEquals(nonExtnNumber, phoneUtil.parse("(1800) 7493.5247", "US"));
    // Check that the last instance of an extension token is matched.
    PhoneNumber extnNumber = new PhoneNumber();
    extnNumber.setCountryCode(1).setNationalNumber(80074935247L).setExtension("1234");
    assertEquals(extnNumber, phoneUtil.parse("0~0 1800 7493 5247 ~1234", "PL"));
    // Verifying bug-fix where the last digit of a number was previously omitted if it was a 0 when
    // extracting the extension. Also verifying a few different cases of extensions.
    PhoneNumber ukNumber = new PhoneNumber();
    ukNumber.setCountryCode(44).setNationalNumber(2034567890L).setExtension("456");
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890x456", "NZ"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890x456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 x456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X 456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X  456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 x 456  ", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890  X 456", "GB"));

    PhoneNumber usWithExtension = new PhoneNumber();
    usWithExtension.setCountryCode(1).setNationalNumber(8009013355L).setExtension("7246433");
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 x 7246433", "US"));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 , ext 7246433", "US"));
    assertEquals(usWithExtension,
                 phoneUtil.parse("(800) 901-3355 ,extension 7246433", "US"));
    assertEquals(usWithExtension,
                 phoneUtil.parse("(800) 901-3355 ,extensi\u00F3n 7246433", "US"));
    // Repeat with the small letter o with acute accent created by combining characters.
    assertEquals(usWithExtension,
                 phoneUtil.parse("(800) 901-3355 ,extensio\u0301n 7246433", "US"));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 , 7246433", "US"));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 ext: 7246433", "US"));

    // Test that if a number has two extensions specified, we ignore the second.
    PhoneNumber usWithTwoExtensionsNumber = new PhoneNumber();
    usWithTwoExtensionsNumber.setCountryCode(1).setNationalNumber(2121231234L).setExtension("508");
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508/x1234",
                                                            "US"));
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508/ x1234",
                                                            "US"));
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508\\x1234",
                                                            "US"));

    // Test parsing numbers in the form (645) 123-1234-910# works, where the last 3 digits before
    // the # are an extension.
    usWithExtension.clear();
    usWithExtension.setCountryCode(1).setNationalNumber(6451231234L).setExtension("910");
    assertEquals(usWithExtension, phoneUtil.parse("+1 (645) 123 1234-910#", "US"));
    // Retry with the same number in a slightly different format.
    assertEquals(usWithExtension, phoneUtil.parse("+1 (645) 123 1234 ext. 910#", "US"));
  }

  public void testParseAndKeepRaw() throws Exception {
    PhoneNumber alphaNumericNumber = new PhoneNumber();
    alphaNumericNumber.
        setCountryCode(1).setNationalNumber(80074935247L).setRawInput("800 six-flags").
        setCountryCodeSource(CountryCodeSource.FROM_DEFAULT_COUNTRY);
    assertEquals(alphaNumericNumber,
                 phoneUtil.parseAndKeepRawInput("800 six-flags", "US"));

    alphaNumericNumber.
        setCountryCode(1).setNationalNumber(8007493524L).setRawInput("1800 six-flag").
        setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN);
    assertEquals(alphaNumericNumber,
                 phoneUtil.parseAndKeepRawInput("1800 six-flag", "US"));

    alphaNumericNumber.
        setCountryCode(1).setNationalNumber(8007493524L).setRawInput("+1800 six-flag").
        setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN);
    assertEquals(alphaNumericNumber,
                 phoneUtil.parseAndKeepRawInput("+1800 six-flag", "NZ"));

    alphaNumericNumber.
        setCountryCode(1).setNationalNumber(8007493524L).setRawInput("001800 six-flag").
        setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_IDD);
    assertEquals(alphaNumericNumber,
                 phoneUtil.parseAndKeepRawInput("001800 six-flag", "NZ"));

    // Invalid region code supplied.
    try {
      phoneUtil.parseAndKeepRawInput("123 456 7890", "CS");
      fail("Deprecated country code not allowed: should fail.");
    } catch (NumberParseException e) {
      // Expected this exception.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
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
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals("00 1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(usNumber, "AD"));
  }

  public void testUnknownCountryCallingCodeForValidation() {
    PhoneNumber invalidNumber = new PhoneNumber();
    invalidNumber.setCountryCode(0).setNationalNumber(1234L);
    assertFalse(phoneUtil.isValidNumber(invalidNumber));
  }

  public void testIsNumberMatchMatches() throws Exception {
    // Test simple matches where formatting is different, or leading zeroes, or country code has
    // been specified.
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331 6005", "+64 03 331 6005"));
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
    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L).setExtension("3456");
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "+643 331 6005 ext 3456"));
    nzNumber.clearExtension();
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "+6403 331 6005"));
    // Check empty extensions are ignored.
    nzNumber.setExtension("");
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "+6403 331 6005"));
    // Check variant with two proto buffers.
    PhoneNumber nzNumberTwo = new PhoneNumber();
    nzNumberTwo.setCountryCode(64).setNationalNumber(33316005L);
    assertEquals("Number " + nzNumber.toString() + " did not match " + nzNumberTwo.toString(),
                 PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, nzNumberTwo));
  }

  public void testIsNumberMatchNonMatches() throws Exception {
    // Non-matches.
    assertEquals(PhoneNumberUtil.MatchType.NO_MATCH,
                 phoneUtil.isNumberMatch("03 331 6005", "03 331 6006"));
    // Different country code, partial number match.
    assertEquals(PhoneNumberUtil.MatchType.NO_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "+16433316005"));
    // Different country code, same number.
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
    // Invalid numbers that can't be parsed.
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
                 phoneUtil.isNumberMatch("3 331-6005", "03 331 6005"));
    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L).setExtension("");
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "03 331 6005"));
    // Here the second number possibly starts with the country code for New Zealand, although we are
    // unsure.
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "(64-3) 331 6005"));
    PhoneNumber unchangedNzNumber = new PhoneNumber();
    unchangedNzNumber.setCountryCode(64).setNationalNumber(33316005L).setExtension("");
    // Check the phone number proto was not edited during the method call.
    assertEquals(unchangedNzNumber, nzNumber);

    // Here, the 1 might be a national prefix, if we compare it to the US number, so the resultant
    // match is an NSN match.
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(2345678901L).setExtension("");
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(usNumber, "1-234-567-8901"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(usNumber, "2345678901"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("+1 234-567 8901", "1 234 567 8901"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("1 234-567 8901", "1 234 567 8901"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("1 234-567 8901", "+1 234 567 8901"));
    // For this case, the match will be a short NSN match, because we cannot assume that the 1 might
    // be a national prefix, so don't remove it when parsing.
    PhoneNumber randomNumber = new PhoneNumber();
    randomNumber.setCountryCode(41).setNationalNumber(2345678901L).setExtension("");
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch(randomNumber, "1-234-567-8901"));
  }

  public void testIsNumberMatchShortNsnMatches() throws Exception {
    // Short NSN matches with the country not specified for either one or both numbers.
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "331 6005"));
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
    // NSN matches, country code omitted for one number, extension missing for one.
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
    // We have no-international-dialling rules for the US in our test metadata.
    PhoneNumber usNumber = new PhoneNumber();
    usNumber.setCountryCode(1).setNationalNumber(8001231234L);
    assertFalse(phoneUtil.canBeInternationallyDialled(usNumber));

    PhoneNumber usInternationalNumber = new PhoneNumber();
    usInternationalNumber.setCountryCode(1).setNationalNumber(2311231234L);
    assertTrue(phoneUtil.canBeInternationallyDialled(usInternationalNumber));

    PhoneNumber usInvalidNumber = new PhoneNumber();
    // Invalid number.
    usInvalidNumber.setCountryCode(1).setNationalNumber(13112312L);
    assertTrue(phoneUtil.canBeInternationallyDialled(usInvalidNumber));

    // We have no data for NZ - should return true.
    PhoneNumber nzNumber = new PhoneNumber();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L);
    assertTrue(phoneUtil.canBeInternationallyDialled(nzNumber));
  }
}
