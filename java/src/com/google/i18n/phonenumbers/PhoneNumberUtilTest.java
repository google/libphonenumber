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
import junit.framework.TestCase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Unit tests for PhoneNumberUtil.java
 *
 * Note that these tests use the metadata contained in the file specified by TEST_META_DATA_FILE,
 * not the normal metadata file, so should not be used for regression test purposes - these tests
 * are illustrative only and test functionality.
 *
 * @author Shaopeng Jia
 * @author Lara Rennie
 */
public class PhoneNumberUtilTest extends TestCase {
  private PhoneNumberUtil phoneUtil;
  private static final String TEST_META_DATA_FILE =
      "/com/google/i18n/phonenumbers/PhoneNumberMetadataProtoForTesting";

  public PhoneNumberUtilTest() {
    PhoneNumberUtil.resetInstance();
    InputStream in = PhoneNumberUtilTest.class.getResourceAsStream(TEST_META_DATA_FILE);
    phoneUtil = PhoneNumberUtil.getInstance(in);
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
    PhoneMetadata metadata = phoneUtil.getPhoneMetadata("US");
    assertEquals("US", metadata.getId());
    assertEquals(1, metadata.getCountryCode());
    assertEquals("011", metadata.getInternationalPrefix());
    assertFalse(metadata.hasNationalPrefix());
    assertEquals(2, metadata.getNumberFormatCount());
    assertEquals("(\\d{3})(\\d{3})(\\d{4})",
                 metadata.getNumberFormat(0).getPattern());
    assertEquals("$1 $2 $3", metadata.getNumberFormat(0).getFormat());
    assertEquals("[13-9]\\d{9}|2[0-35-9]\\d{8}",
                 metadata.getGeneralDesc().getNationalNumberPattern());
    assertEquals("\\d{7,10}", metadata.getGeneralDesc().getPossibleNumberPattern());
    assertEquals(metadata.getGeneralDesc(), metadata.getFixedLine());
    assertEquals("\\d{10}", metadata.getTollFree().getPossibleNumberPattern());
    assertEquals("900\\d{7}", metadata.getPremiumRate().getNationalNumberPattern());
    // No shared-cost data is available, so it should be initialised to "NA".
    assertEquals("NA", metadata.getSharedCost().getNationalNumberPattern());
    assertEquals("NA", metadata.getSharedCost().getPossibleNumberPattern());
  }

  public void testGetInstanceLoadDEMetadata() {
    PhoneMetadata metadata = phoneUtil.getPhoneMetadata("DE");
    assertEquals("DE", metadata.getId());
    assertEquals(49, metadata.getCountryCode());
    assertEquals("00", metadata.getInternationalPrefix());
    assertEquals("0", metadata.getNationalPrefix());
    assertEquals(6, metadata.getNumberFormatCount());
    assertEquals("9009", metadata.getNumberFormat(5).getLeadingDigits());
    assertEquals("(\\d{3})(\\d{4})(\\d{4})",
                 metadata.getNumberFormat(5).getPattern());
    assertEquals("$1 $2 $3", metadata.getNumberFormat(5).getFormat());
    assertEquals("(?:[24-6]\\d{2}|3[03-9]\\d|[789](?:[1-9]\\d|0[2-9]))\\d{3,8}",
                 metadata.getFixedLine().getNationalNumberPattern());
    assertEquals("\\d{2,14}", metadata.getFixedLine().getPossibleNumberPattern());
    assertEquals("30123456", metadata.getFixedLine().getExampleNumber());
    assertEquals("\\d{10}", metadata.getTollFree().getPossibleNumberPattern());
    assertEquals("900([135]\\d{6}|9\\d{7})", metadata.getPremiumRate().getNationalNumberPattern());
  }

  public void testGetInstanceLoadARMetadata() {
    PhoneMetadata metadata = phoneUtil.getPhoneMetadata("AR");
    assertEquals("AR", metadata.getId());
    assertEquals(54, metadata.getCountryCode());
    assertEquals("00", metadata.getInternationalPrefix());
    assertEquals("0", metadata.getNationalPrefix());
    assertEquals("0(?:(11|343|3715)15)?", metadata.getNationalPrefixForParsing());
    assertEquals("9$1", metadata.getNationalPrefixTransformRule());
    assertEquals("9(\\d{4})(\\d{2})(\\d{4})",
                 metadata.getNumberFormat(3).getPattern());
    assertEquals("$1 15 $2-$3", metadata.getNumberFormat(3).getFormat());
    assertEquals("(9)(\\d{4})(\\d{2})(\\d{4})",
                 metadata.getIntlNumberFormat(3).getPattern());
    assertEquals("$1 $2 $3 $4", metadata.getIntlNumberFormat(3).getFormat());
  }

  public void testGetExampleNumber() {
    PhoneNumber deNumber =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(30123456).build();
    assertEquals(deNumber, phoneUtil.getExampleNumber("DE"));

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
  }

  public void testNormaliseStripAlphaCharacters() {
    String inputNumber = "034-56&+a#234";
    String expectedOutput = "03456234";
    assertEquals("Conversion did not correctly remove alpha character",
                 expectedOutput,
                 PhoneNumberUtil.normalizeDigitsOnly(inputNumber));
  }

  public void testFormatUSNumber() {
    PhoneNumber usNumber1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6502530000L).build();
    assertEquals("650 253 0000", phoneUtil.format(usNumber1,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+1 650 253 0000",
                 phoneUtil.format(usNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));

    PhoneNumber usNumber2 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(8002530000L).build();
    assertEquals("800 253 0000", phoneUtil.format(usNumber2,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+1 800 253 0000",
                 phoneUtil.format(usNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));

    PhoneNumber usNumber3 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(9002530000L).build();
    assertEquals("900 253 0000", phoneUtil.format(usNumber3,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+1 900 253 0000",
                 phoneUtil.format(usNumber3,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatBSNumber() {
    PhoneNumber bsNumber1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(2421234567L).build();
    assertEquals("242 123 4567", phoneUtil.format(bsNumber1,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+1 242 123 4567",
                 phoneUtil.format(bsNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));

    PhoneNumber bsNumber2 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(8002530000L).build();
    assertEquals("800 253 0000", phoneUtil.format(bsNumber2,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+1 800 253 0000",
                 phoneUtil.format(bsNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));

    PhoneNumber bsNumber3 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(9002530000L).build();
    assertEquals("900 253 0000", phoneUtil.format(bsNumber3,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+1 900 253 0000",
                 phoneUtil.format(bsNumber3,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatGBNumber() {
    PhoneNumber gbNumber1 =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(2087389353L).build();
    assertEquals("(020) 8738 9353", phoneUtil.format(gbNumber1,
                                                     PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+44 20 8738 9353",
                 phoneUtil.format(gbNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));

    PhoneNumber gbNumber2 =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(7912345678L).build();
    assertEquals("(07912) 345 678", phoneUtil.format(gbNumber2,
                                                     PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+44 7912 345 678",
                 phoneUtil.format(gbNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatDENumber() {
    PhoneNumber deNumber1 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(301234L).build();
    assertEquals("030 1234", phoneUtil.format(deNumber1,
                                              PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+49 30 1234",
                 phoneUtil.format(deNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));

    PhoneNumber deNumber2 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(291123L).build();
    assertEquals("0291 123", phoneUtil.format(deNumber2,
                                              PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+49 291 123",
                 phoneUtil.format(deNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));

    PhoneNumber deNumber3 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(29112345678L).build();
    assertEquals("0291 12345678", phoneUtil.format(deNumber3,
                                                   PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+49 291 12345678",
                 phoneUtil.format(deNumber3,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));

    PhoneNumber deNumber4 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(9123123L).build();
    assertEquals("09123 123", phoneUtil.format(deNumber4,
                                               PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+49 9123 123",
                 phoneUtil.format(deNumber4,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
    PhoneNumber deNumber5 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(1234L).build();
    // Note this number is correctly formatted without national prefix. Most of the numbers that
    // are treated as invalid numbers by the library are short numbers, and they are usually not
    // dialed with national prefix.
    assertEquals("1234", phoneUtil.format(deNumber5,
                                          PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+49 1234",
                 phoneUtil.format(deNumber5,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
  }

  public void testFormatITNumber() {
    PhoneNumber itNumber1 =
        PhoneNumber.newBuilder()
            .setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true).build();
    assertEquals("02 3661 8300", phoneUtil.format(itNumber1,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+39 02 3661 8300",
                 phoneUtil.format(itNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+390236618300",
                 phoneUtil.format(itNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.E164));

    PhoneNumber itNumber2 =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(345678901L).build();
    assertEquals("345 678 901", phoneUtil.format(itNumber2,
                                                 PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+39 345 678 901",
                 phoneUtil.format(itNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+39345678901",
                 phoneUtil.format(itNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.E164));
  }

  public void testFormatAUNumber() {
    PhoneNumber auNumber1 =
        PhoneNumber.newBuilder().setCountryCode(61).setNationalNumber(236618300L).build();
    assertEquals("02 3661 8300", phoneUtil.format(auNumber1,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+61 2 3661 8300",
                 phoneUtil.format(auNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+61236618300",
                 phoneUtil.format(auNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.E164));

    PhoneNumber auNumber2 =
        PhoneNumber.newBuilder().setCountryCode(61).setNationalNumber(1800123456L).build();
    assertEquals("1800 123 456", phoneUtil.format(auNumber2,
                                                 PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+61 1800 123 456",
                 phoneUtil.format(auNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+611800123456",
                 phoneUtil.format(auNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.E164));
  }

  public void testFormatARNumber() {
    PhoneNumber arNumber1 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(1187654321L).build();
    assertEquals("011 8765-4321", phoneUtil.format(arNumber1,
                                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+54 11 8765-4321",
                 phoneUtil.format(arNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+541187654321",
                 phoneUtil.format(arNumber1,
                                  PhoneNumberUtil.PhoneNumberFormat.E164));

    PhoneNumber arNumber2 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(91187654321L).build();
    assertEquals("011 15 8765-4321", phoneUtil.format(arNumber2,
                                                      PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals("+54 9 11 8765 4321",
                 phoneUtil.format(arNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+5491187654321",
                 phoneUtil.format(arNumber2,
                                  PhoneNumberUtil.PhoneNumberFormat.E164));
  }

  public void testFormatOutOfCountryCallingNumber() {
    PhoneNumber usNumber1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(9002530000L).build();
    assertEquals("00 1 900 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(usNumber1, "DE"));

    PhoneNumber usNumber2 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6502530000L).build();
    assertEquals("1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(usNumber2, "BS"));

    assertEquals("0~0 1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(usNumber2, "PL"));

    PhoneNumber gbNumber =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(7912345678L).build();
    assertEquals("011 44 7912 345 678",
                 phoneUtil.formatOutOfCountryCallingNumber(gbNumber, "US"));

    PhoneNumber deNumber =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(1234L).build();
    assertEquals("00 49 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(deNumber, "GB"));
    // Note this number is correctly formatted without national prefix. Most of the numbers that
    // are treated as invalid numbers by the library are short numbers, and they are usually not
    // dialed with national prefix.
    assertEquals("1234",
                 phoneUtil.formatOutOfCountryCallingNumber(deNumber, "DE"));

    PhoneNumber itNumber =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(236618300L)
            .setItalianLeadingZero(true).build();
    assertEquals("011 39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(itNumber, "US"));
    assertEquals("02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(itNumber, "IT"));
    assertEquals("+39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(itNumber, "SG"));

    PhoneNumber sgNumber =
        PhoneNumber.newBuilder().setCountryCode(65).setNationalNumber(94777892L).build();
    assertEquals("9477 7892",
                 phoneUtil.formatOutOfCountryCallingNumber(sgNumber, "SG"));

    PhoneNumber arNumber1 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(91187654321L).build();
    assertEquals("011 54 9 11 8765 4321",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumber1, "US"));

    PhoneNumber arNumber2 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(91187654321L)
            .setExtension("1234").build();
    assertEquals("011 54 9 11 8765 4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumber2, "US"));
    assertEquals("0011 54 9 11 8765 4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumber2, "AU"));
    assertEquals("011 15 8765-4321 ext. 1234",
                 phoneUtil.formatOutOfCountryCallingNumber(arNumber2, "AR"));
  }

  public void testFormatOutOfCountryWithPreferredIntlPrefix() {
    PhoneNumber.Builder itNumber = PhoneNumber.newBuilder();
    itNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    // This should use 0011, since that is the preferred international prefix (both 0011 and 0012
    // are accepted as possible international prefixes in our test metadta.)
    assertEquals("0011 39 02 3661 8300",
                 phoneUtil.formatOutOfCountryCallingNumber(itNumber.build(), "AU"));
  }

  public void testFormatByPattern() {
    PhoneNumber usNumber =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6502530000L).build();
    NumberFormat newNumFormat1 =
        NumberFormat.newBuilder().setPattern("(\\d{3})(\\d{3})(\\d{4})")
            .setFormat("($1) $2-$3").build();
    List<NumberFormat> newNumberFormats = new ArrayList<NumberFormat>();
    newNumberFormats.add(newNumFormat1);

    assertEquals("(650) 253-0000",
                 phoneUtil.formatByPattern(usNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));
    assertEquals("+1 (650) 253-0000",
                 phoneUtil.formatByPattern(usNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));

    PhoneNumber itNumber =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(236618300L)
            .setItalianLeadingZero(true).build();
    NumberFormat newNumFormat2 =
        NumberFormat.newBuilder().setPattern("(\\d{2})(\\d{5})(\\d{3})")
            .setFormat("$1-$2 $3").build();
    newNumberFormats.set(0, newNumFormat2);

    assertEquals("02-36618 300",
                 phoneUtil.formatByPattern(itNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));
    assertEquals("+39 02-36618 300",
                 phoneUtil.formatByPattern(itNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));

    PhoneNumber gbNumber =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(2012345678L).build();

    NumberFormat newNumFormat3 =
        NumberFormat.newBuilder().setNationalPrefixFormattingRule("$NP$FG")
            .setPattern("(\\d{2})(\\d{4})(\\d{4})").setFormat("$1 $2 $3").build();
    newNumberFormats.set(0, newNumFormat3);
    assertEquals("020 1234 5678",
                 phoneUtil.formatByPattern(gbNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));

    NumberFormat newNumFormat4 =
        NumberFormat.newBuilder(newNumFormat3).setNationalPrefixFormattingRule("($NP$FG)").build();
    newNumberFormats.set(0, newNumFormat4);
    assertEquals("(020) 1234 5678",
                 phoneUtil.formatByPattern(gbNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));
    NumberFormat newNumFormat5 =
        NumberFormat.newBuilder(newNumFormat4).setNationalPrefixFormattingRule("").build();
    newNumberFormats.set(0, newNumFormat5);
    assertEquals("20 1234 5678",
                 phoneUtil.formatByPattern(gbNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.NATIONAL,
                                           newNumberFormats));

    NumberFormat newNumFormat6 =
        NumberFormat.newBuilder(newNumFormat5).setNationalPrefixFormattingRule("").build();
    newNumberFormats.set(0, newNumFormat6);
    assertEquals("+44 20 1234 5678",
                 phoneUtil.formatByPattern(gbNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL,
                                           newNumberFormats));
  }

  public void testFormatE164Number() {
    PhoneNumber.Builder usNumber = PhoneNumber.newBuilder();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals("+16502530000", phoneUtil.format(usNumber.build(),
                                                  PhoneNumberUtil.PhoneNumberFormat.E164));
    PhoneNumber.Builder deNumber = PhoneNumber.newBuilder();
    deNumber.setCountryCode(49).setNationalNumber(301234L);
    assertEquals("+49301234", phoneUtil.format(deNumber.build(),
                                               PhoneNumberUtil.PhoneNumberFormat.E164));
  }

  public void testFormatNumberWithExtension() {
    PhoneNumber.Builder nzNumber = PhoneNumber.newBuilder();
    nzNumber.setCountryCode(64).setNationalNumber(33316005L).setExtension("1234");
    // Uses default extension prefix:
    assertEquals("03-331 6005 ext. 1234",
                 phoneUtil.format(nzNumber.build(),
                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    // Extension prefix overridden in the territory information for the US:
    PhoneNumber.Builder usNumber = PhoneNumber.newBuilder();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L).setExtension("4567");
    assertEquals("650 253 0000 extn. 4567",
                 phoneUtil.format(usNumber.build(),
                                  PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
  }

  public void testIsPremiumRate() {
    PhoneNumber premiumRateNumber1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(9004433030L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber1));

    PhoneNumber premiumRateNumber2 =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(892123L).build();
        assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber2));

    PhoneNumber premiumRateNumber3 =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(9187654321L).build();
        assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber3));

    PhoneNumber premiumRateNumber4 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(9001654321L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber4));

    PhoneNumber premiumRateNumber5 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(90091234567L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.PREMIUM_RATE,
                 phoneUtil.getNumberType(premiumRateNumber5));
  }

  public void testIsTollFree() {
    PhoneNumber tollFreeNumber1
        = PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(8881234567L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(tollFreeNumber1));

    PhoneNumber tollFreeNumber2
        = PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(803123L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(tollFreeNumber2));

    PhoneNumber tollFreeNumber3
        = PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(8012345678L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(tollFreeNumber3));

    PhoneNumber tollFreeNumber4
        = PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(8001234567L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.TOLL_FREE,
                 phoneUtil.getNumberType(tollFreeNumber4));
  }

  public void testIsMobile() {
    PhoneNumber mobileNumber1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(2423570000L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber1));

    PhoneNumber mobileNumber2 =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(312345678L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber2));

    PhoneNumber mobileNumber3 =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(7912345678L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber3));

    PhoneNumber mobileNumber4 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(15123456789L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber4));

    PhoneNumber mobileNumber5 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(91187654321L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.MOBILE,
                 phoneUtil.getNumberType(mobileNumber5));
  }

  public void testIsFixedLine() {
    // A Bahama fixed-line number
    PhoneNumber fixedLineNumber1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(2423651234L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE,
                 phoneUtil.getNumberType(fixedLineNumber1));

    // An Italian fixed-line number
    PhoneNumber fixedLineNumber2 =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(236618300L)
            .setItalianLeadingZero(true).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE,
                 phoneUtil.getNumberType(fixedLineNumber2));

    PhoneNumber fixedLineNumber3 =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(2012345678L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE,
                 phoneUtil.getNumberType(fixedLineNumber3));

    PhoneNumber fixedLineNumber4 =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(301234L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE,
                 phoneUtil.getNumberType(fixedLineNumber4));
  }

  public void testIsFixedLineAndMobile() {
    PhoneNumber fixedLineAndMobileNumber1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6502531111L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE,
                 phoneUtil.getNumberType(fixedLineAndMobileNumber1));

    PhoneNumber fixedLineAndMobileNumber2 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(1987654321L).build();
    assertEquals(PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE,
                 phoneUtil.getNumberType(fixedLineAndMobileNumber2));
  }

  public void testIsSharedCost() {
    PhoneNumber.Builder gbNumber = PhoneNumber.newBuilder();
    gbNumber.setCountryCode(44).setNationalNumber(8431231234L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.SHARED_COST,
        phoneUtil.getNumberType(gbNumber.build()));
  }

  public void testIsVoip() {
    PhoneNumber.Builder gbNumber = PhoneNumber.newBuilder();
    gbNumber.setCountryCode(44).setNationalNumber(5631231234L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.VOIP, phoneUtil.getNumberType(gbNumber.build()));
  }

  public void testIsPersonalNumber() {
    PhoneNumber.Builder gbNumber = PhoneNumber.newBuilder();
    gbNumber.setCountryCode(44).setNationalNumber(7031231234L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.PERSONAL_NUMBER,
                 phoneUtil.getNumberType(gbNumber.build()));
  }

  public void testIsUnknown() {
    PhoneNumber.Builder unknownNumber = PhoneNumber.newBuilder();
    unknownNumber.setCountryCode(1).setNationalNumber(65025311111L);
    assertEquals(PhoneNumberUtil.PhoneNumberType.UNKNOWN,
                 phoneUtil.getNumberType(unknownNumber.build()));
  }

  public void testIsValidNumber() {
    PhoneNumber.Builder usNumber = PhoneNumber.newBuilder();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertTrue(phoneUtil.isValidNumber(usNumber.build()));

    PhoneNumber.Builder itNumber = PhoneNumber.newBuilder();
    itNumber.setCountryCode(39).setNationalNumber(236618300L).setItalianLeadingZero(true);
    assertTrue(phoneUtil.isValidNumber(itNumber.build()));

    PhoneNumber.Builder gbNumber = PhoneNumber.newBuilder();
    gbNumber.setCountryCode(44).setNationalNumber(7912345678L);
    assertTrue(phoneUtil.isValidNumber(gbNumber.build()));

    PhoneNumber.Builder nzNumber = PhoneNumber.newBuilder();
    nzNumber.setCountryCode(64).setNationalNumber(21387835L);
    assertTrue(phoneUtil.isValidNumber(nzNumber.build()));
  }

  public void testIsValidForRegion() {
    // This number is valid for the Bahamas, but is not a valid US number.
    PhoneNumber bsNumber1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(2423232345L).build();
    assertTrue(phoneUtil.isValidNumber(bsNumber1));
    assertTrue(phoneUtil.isValidNumberForRegion(bsNumber1, "BS"));
    assertFalse(phoneUtil.isValidNumberForRegion(bsNumber1, "US"));
    PhoneNumber bsNumber2 =
        PhoneNumber.newBuilder(bsNumber1).setNationalNumber(2421232345L).build();
    // This number is no longer valid.
    assertFalse(phoneUtil.isValidNumber(bsNumber2));
  }

  public void testIsNotValidNumber() {
    PhoneNumber.Builder usNumber = PhoneNumber.newBuilder();
    usNumber.setCountryCode(1).setNationalNumber(2530000L);
    assertFalse(phoneUtil.isValidNumber(usNumber.build()));

    PhoneNumber.Builder itNumber = PhoneNumber.newBuilder();
    itNumber.setCountryCode(39).setNationalNumber(23661830000L).setItalianLeadingZero(true);
    assertFalse(phoneUtil.isValidNumber(itNumber.build()));

    PhoneNumber.Builder gbNumber = PhoneNumber.newBuilder();
    gbNumber.setCountryCode(44).setNationalNumber(791234567L);
    assertFalse(phoneUtil.isValidNumber(gbNumber.build()));

    PhoneNumber.Builder deNumber = PhoneNumber.newBuilder();
    deNumber.setCountryCode(49).setNationalNumber(1234L);
    assertFalse(phoneUtil.isValidNumber(deNumber.build()));

    PhoneNumber.Builder nzNumber = PhoneNumber.newBuilder();
    nzNumber.setCountryCode(64).setNationalNumber(3316005L);
    assertFalse(phoneUtil.isValidNumber(nzNumber.build()));
  }

  public void testGetRegionCodeForCountryCode() {
    assertEquals("US", phoneUtil.getRegionCodeForCountryCode(1));
    assertEquals("GB", phoneUtil.getRegionCodeForCountryCode(44));
    assertEquals("DE", phoneUtil.getRegionCodeForCountryCode(49));
  }

  public void testGetRegionCodeForNumber() {
    PhoneNumber.Builder bsNumber = PhoneNumber.newBuilder();
    bsNumber.setCountryCode(1).setNationalNumber(2423027000L);
    assertEquals("BS", phoneUtil.getRegionCodeForNumber(bsNumber.build()));

    PhoneNumber.Builder usNumber = PhoneNumber.newBuilder();
    usNumber.setCountryCode(1).setNationalNumber(6502530000L);
    assertEquals("US", phoneUtil.getRegionCodeForNumber(usNumber.build()));

    PhoneNumber.Builder gbNumber = PhoneNumber.newBuilder();
    gbNumber.setCountryCode(44).setNationalNumber(7912345678L);
    assertEquals("GB", phoneUtil.getRegionCodeForNumber(gbNumber.build()));
  }

  public void testGetCountryCodeForRegion() {
    assertEquals(1, phoneUtil.getCountryCodeForRegion("US"));
    assertEquals(64, phoneUtil.getCountryCodeForRegion("NZ"));
  }

  public void testGetNANPACountries() {
    Set nanpaCountries = phoneUtil.getNANPACountries();
    assertEquals(2, nanpaCountries.size());
    assertTrue(nanpaCountries.contains("US"));
    assertTrue(nanpaCountries.contains("BS"));
  }

  public void testIsPossibleNumber() {
    PhoneNumber number1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6502530000L).build();
    assertTrue(phoneUtil.isPossibleNumber(number1));

    PhoneNumber number2 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(2530000L).build();
    assertTrue(phoneUtil.isPossibleNumber(number2));

    PhoneNumber number3 =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(2070313000L).build();
    assertTrue(phoneUtil.isPossibleNumber(number3));
       
    assertTrue(phoneUtil.isPossibleNumber("+1 650 253 0000", "US"));
    assertTrue(phoneUtil.isPossibleNumber("+1 650 GOO OGLE", "US"));
    assertTrue(phoneUtil.isPossibleNumber("(650) 253-0000", "US"));
    assertTrue(phoneUtil.isPossibleNumber("253-0000", "US"));
    assertTrue(phoneUtil.isPossibleNumber("+1 650 253 0000", "GB"));
    assertTrue(phoneUtil.isPossibleNumber("+44 20 7031 3000", "GB"));
    assertTrue(phoneUtil.isPossibleNumber("(020) 7031 3000", "GB"));
    assertTrue(phoneUtil.isPossibleNumber("7031 3000", "GB"));
    assertTrue(phoneUtil.isPossibleNumber("3331 6005", "NZ"));
  }


  public void testIsPossibleNumberWithReason() {
    // FYI, national numbers for country code +1 that are within 7 to 10 digits are possible.
    PhoneNumber number1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6502530000L).build();
    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(number1));

    PhoneNumber number2 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(2530000L).build();
    assertEquals(PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                 phoneUtil.isPossibleNumberWithReason(number2));

    PhoneNumber number3 =
        PhoneNumber.newBuilder().setCountryCode(0).setNationalNumber(2530000L).build();
    assertEquals(PhoneNumberUtil.ValidationResult.INVALID_COUNTRY_CODE,
                 phoneUtil.isPossibleNumberWithReason(number3));

    PhoneNumber number4 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(253000L).build();
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_SHORT,
                 phoneUtil.isPossibleNumberWithReason(number4));

    PhoneNumber number5 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(65025300000L).build();
    assertEquals(PhoneNumberUtil.ValidationResult.TOO_LONG,
                 phoneUtil.isPossibleNumberWithReason(number5));
  }

  public void testIsNotPossibleNumber() {
    PhoneNumber number1 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(65025300000L).build();
    assertFalse(phoneUtil.isPossibleNumber(number1));

    PhoneNumber number2 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(253000L).build();
    assertFalse(phoneUtil.isPossibleNumber(number2));

    PhoneNumber number3 =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(300L).build();
    assertFalse(phoneUtil.isPossibleNumber(number3));

    assertFalse(phoneUtil.isPossibleNumber("+1 650 253 00000", "US"));
    assertFalse(phoneUtil.isPossibleNumber("(650) 253-00000", "US"));
    assertFalse(phoneUtil.isPossibleNumber("I want a Pizza", "US"));
    assertFalse(phoneUtil.isPossibleNumber("253-000", "US"));
    assertFalse(phoneUtil.isPossibleNumber("1 3000", "GB"));
    assertFalse(phoneUtil.isPossibleNumber("+44 300", "GB"));
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
    assertEquals(true, phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                           internationalPrefix));
    assertEquals("The number supplied was not stripped of its international prefix.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Now the number no longer starts with an IDD prefix, so it should now report false.
    assertEquals(false, phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                            internationalPrefix));

    numberToStrip = new StringBuffer("00945677003898003");
    assertEquals(true, phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                           internationalPrefix));
    assertEquals("The number supplied was not stripped of its international prefix.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Test it works when the international prefix is broken up by spaces.
    numberToStrip = new StringBuffer("00 9 45677003898003");
    assertEquals(true, phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                           internationalPrefix));
    assertEquals("The number supplied was not stripped of its international prefix.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Now the number no longer starts with an IDD prefix, so it should now report false.
    assertEquals(false, phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                            internationalPrefix));

    // Test the + symbol is also recognised and stripped.
    numberToStrip = new StringBuffer("+45677003898003");
    strippedNumber = new StringBuffer("45677003898003");
    assertEquals(true, phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                           internationalPrefix));
    assertEquals("The number supplied was not stripped of the plus symbol.",
                 strippedNumber.toString(), numberToStrip.toString());

    // If the number afterwards is a zero, we should not strip this - no country code begins with 0.
    numberToStrip = new StringBuffer("0090112-3123");
    strippedNumber = new StringBuffer("00901123123");
    assertEquals(false, phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                            internationalPrefix));
    assertEquals("The number supplied had a 0 after the match so shouldn't be stripped.",
                 strippedNumber.toString(), numberToStrip.toString());
    // Here the 0 is separated by a space from the IDD.
    numberToStrip = new StringBuffer("009 0-112-3123");
    assertEquals(false, phoneUtil.maybeStripInternationalPrefixAndNormalize(numberToStrip,
                                                                            internationalPrefix));
  }

  public void testMaybeExtractCountryCode() {
    PhoneMetadata metadata = phoneUtil.getPhoneMetadata("US");
    // Note that for the US, the IDD is 011.
    try {
      String phoneNumber = "011112-3456789";
      String strippedNumber = "123456789";
      int countryCode = 1;
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Did not extract country code " + countryCode + " correctly.",
                   countryCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill));
      // Should strip and normalize national significant number.
      assertEquals("Did not strip off the country code correctly.",
                   strippedNumber,
                   numberToFill.toString());
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    try {
      String phoneNumber = "+6423456789";
      int countryCode = 64;
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Did not extract country code " + countryCode + " correctly.",
                   countryCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill));
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    try {
      String phoneNumber = "2345-6789";
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should not have extracted a country code - no international prefix present.",
                   0,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill));
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    try {
      String phoneNumber = "0119991123456789";
      StringBuffer numberToFill = new StringBuffer();
      phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                        numberToFill);
      fail("Should have thrown an exception, no valid country code present.");
    } catch (NumberParseException e) {
      // Expected.
      assertEquals("Wrong error type stored in exception.",
                   NumberParseException.ErrorType.INVALID_COUNTRY_CODE,
                   e.getErrorType());
    }
    try {
      String phoneNumber = "(1 610) 619 4466";
      int countryCode = 1;
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should have extracted the country code of the region passed in",
                   countryCode,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill));
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    try {
      String phoneNumber = "(1 610) 619 446";
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should not have extracted a country code - invalid number after extraction " +
                   "of uncertain country code.",
                   0,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata, numberToFill));
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
    try {
      String phoneNumber = "(1 610) 619 43 446";
      StringBuffer numberToFill = new StringBuffer();
      assertEquals("Should not have extracted a country code - invalid number both before and " +
                   "after extraction of uncertain country code.",
                   0,
                   phoneUtil.maybeExtractCountryCode(phoneNumber, metadata,
                                                     numberToFill));
    } catch (NumberParseException e) {
      fail("Should not have thrown an exception: " + e.toString());
    }
  }

  public void testParseNationalNumber() throws Exception {
    PhoneNumber nzNumber =
        PhoneNumber.newBuilder().setCountryCode(64).setNationalNumber(33316005L).build();

    // National prefix attached.
    assertEquals(nzNumber, phoneUtil.parse("033316005", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("33316005", "NZ"));
    // National prefix attached and some formatting present.
    assertEquals(nzNumber, phoneUtil.parse("03-331 6005", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("03 331 6005", "NZ"));
    // Test case with alpha characters.
    PhoneNumber tollfreeNumber =
        PhoneNumber.newBuilder().setCountryCode(64).setNationalNumber(800332005L).build();
    assertEquals(tollfreeNumber, phoneUtil.parse("0800 DDA 005", "NZ"));
    PhoneNumber premiumNumber =
        PhoneNumber.newBuilder().setCountryCode(64).setNationalNumber(9003326005L).build();
    assertEquals(premiumNumber, phoneUtil.parse("0900 DDA 6005", "NZ"));
    // Not enough alpha characters for them to be considered intentional, so they are stripped.
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 6005a", "NZ"));
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 600a5", "NZ"));
    assertEquals(premiumNumber, phoneUtil.parse("0900 332 600A5", "NZ"));
    assertEquals(premiumNumber, phoneUtil.parse("0900 a332 600A5", "NZ"));

    // Testing international prefixes.
    // Should strip country code.
    assertEquals(nzNumber, phoneUtil.parse("0064 3 331 6005", "NZ"));
    // Try again, but this time we have an international number with Region Code US. It should
    // recognise the country code and parse accordingly.
    assertEquals(nzNumber, phoneUtil.parse("01164 3 331 6005", "US"));
    assertEquals(nzNumber, phoneUtil.parse("+64 3 331 6005", "US"));

    // Test for http://b/issue?id=2247493
    PhoneNumber nzNumber2 =
        PhoneNumber.newBuilder().setCountryCode(64).setNationalNumber(64123456L).build();
    assertEquals(nzNumber2, phoneUtil.parse("64(0)64123456", "NZ"));

    PhoneNumber usNumber =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6503336000L).build();
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
    // Check it doesn't use the '1' as a country code when parsing if the phone number was already
    // possible.
    
    PhoneNumber usNumber2 =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(1234567890L).build();
    assertEquals(usNumber2, phoneUtil.parse("123-456-7890", "US"));

    PhoneNumber itNumber =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(236618300L)
            .setItalianLeadingZero(true).build();
    assertEquals(itNumber, phoneUtil.parse("+39 02-36618 300", "NZ"));
    assertEquals(itNumber, phoneUtil.parse("02-36618 300", "IT"));

    PhoneNumber itNumber2 =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(312345678L).build();
    assertEquals(itNumber2, phoneUtil.parse("312 345 678", "IT"));

    // Check that using a "/" is fine in a phone number.
    PhoneNumber deNumber =
        PhoneNumber.newBuilder().setCountryCode(49).setNationalNumber(12345678L).build();
    assertEquals(deNumber, phoneUtil.parse("123/45678", "DE"));

    // Test parsing mobile numbers of Argentina.
    PhoneNumber arNumber =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(93435551212L).build();
    assertEquals(arNumber, phoneUtil.parse("+54 9 343 555 1212", "AR"));
    assertEquals(arNumber, phoneUtil.parse("0343 15 555 1212", "AR"));

    PhoneNumber arNumber2 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(93715654320L).build();
    assertEquals(arNumber2, phoneUtil.parse("+54 9 3715 65 4320", "AR"));
    assertEquals(arNumber2, phoneUtil.parse("03715 15 65 4320", "AR"));

    // Test parsing fixed-line numbers of Argentina.
    PhoneNumber arNumber3 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(1137970000L).build();
    assertEquals(arNumber3, phoneUtil.parse("+54 11 3797 0000", "AR"));
    assertEquals(arNumber3, phoneUtil.parse("011 3797 0000", "AR"));

    PhoneNumber arNumber4 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(3715654321L).build();
    assertEquals(arNumber4, phoneUtil.parse("+54 3715 65 4321", "AR"));
    assertEquals(arNumber4, phoneUtil.parse("03715 65 4321", "AR"));

    PhoneNumber arNumber5 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(2312340000L).build();
    assertEquals(arNumber5, phoneUtil.parse("+54 23 1234 0000", "AR"));
    assertEquals(arNumber5, phoneUtil.parse("023 1234 0000", "AR"));

    // Test that having an 'x' in the phone number at the start is ok and that it just gets removed.
    PhoneNumber arNumber6 =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(123456789L).build();
    assertEquals(arNumber6, phoneUtil.parse("0123456789", "AR"));
    assertEquals(arNumber6, phoneUtil.parse("(0) 123456789", "AR"));
    assertEquals(arNumber6, phoneUtil.parse("0 123456789", "AR"));
    assertEquals(arNumber6, phoneUtil.parse("(0xx) 123456789", "AR"));
    PhoneNumber arFromUs =
        PhoneNumber.newBuilder().setCountryCode(54).setNationalNumber(81429712L).build();
    // This test is intentionally constructed such that the number of digit after xx is larger than
    // 7, so that the number won't be mistakenly treated as an extension, as we allow extensions up
    // to 7 digits. This assumption is okay for now as all the countries where a carrier selection
    // code is written in the form of xx have a national significant number of length larger than 7.
    assertEquals(arFromUs, phoneUtil.parse("011xx5481429712", "US"));

    // Test parsing fixed-line numbers of Mexico.
    PhoneNumber mxNumber =
        PhoneNumber.newBuilder().setCountryCode(52).setNationalNumber(4499780001L).build();
    assertEquals(mxNumber, phoneUtil.parse("+52 (449)978-0001", "MX"));
    assertEquals(mxNumber, phoneUtil.parse("01 (449)978-0001", "MX"));
    assertEquals(mxNumber, phoneUtil.parse("(449)978-0001", "MX"));

    // Test parsing mobile numbers of Mexico.
    PhoneNumber mxNumber2 =
        PhoneNumber.newBuilder().setCountryCode(52).setNationalNumber(13312345678L).build();
    assertEquals(mxNumber2, phoneUtil.parse("+52 1 33 1234-5678", "MX"));
    assertEquals(mxNumber2, phoneUtil.parse("044 (33) 1234-5678", "MX"));
    assertEquals(mxNumber2, phoneUtil.parse("045 33 1234-5678", "MX"));

    // Test that if a number has two extensions specified, we ignore the second.
    PhoneNumber usWithTwoExtensionsNumber =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(2121231234L)
            .setExtension("508").build();
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508/x1234",
                                                            "US"));
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508/ x1234",
                                                            "US"));
    assertEquals(usWithTwoExtensionsNumber, phoneUtil.parse("(212)123-1234 x508\\x1234",
                                                            "US"));

    // Test parsing numbers in the form (645) 123-1234-910# works, where the last 3 digits before
    // the # are an extension.
    PhoneNumber usWithExtension =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6451231234L)
            .setExtension("910").build();
    assertEquals(usWithExtension, phoneUtil.parse("+1 (645) 123 1234-910#", "US"));
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
      phoneUtil.parse(someNumber, "ZZ");
      fail("'Unknown' country code not allowed: should fail.");
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
  }

  public void testParseExtensions() throws Exception {
    PhoneNumber nzNumber =
        PhoneNumber.newBuilder().setCountryCode(64).setNationalNumber(33316005L)
            .setExtension("3456").build();
    assertEquals(nzNumber, phoneUtil.parse("03 331 6005 ext 3456", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("03-3316005x3456", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("03-3316005 int.3456", "NZ"));
    assertEquals(nzNumber, phoneUtil.parse("03 3316005 #3456", "NZ"));
    // Test the following do not extract extensions:
    PhoneNumber nonExtnNumber =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(180074935247L).build();
    assertEquals(nonExtnNumber, phoneUtil.parse("1800 six-flags", "US"));
    assertEquals(nonExtnNumber, phoneUtil.parse("1800 SIX FLAGS", "US"));
    assertEquals(nonExtnNumber, phoneUtil.parse("0~01 1800 7493 5247", "PL"));
    assertEquals(nonExtnNumber, phoneUtil.parse("(1800) 7493.5247", "US"));
    // Check that the last instance of an extension token is matched.
    PhoneNumber extnNumber =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(180074935247L)
            .setExtension("1234").build();
    assertEquals(extnNumber, phoneUtil.parse("0~01 1800 7493 5247 ~1234", "PL"));
    // Verifying bug-fix where the last digit of a number was previously omitted if it was a 0 when
    // extracting the extension. Also verifying a few different cases of extensions.
    PhoneNumber ukNumber =
        PhoneNumber.newBuilder().setCountryCode(44).setNationalNumber(2034567890L)
            .setExtension("456").build();
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890x456", "NZ"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890x456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 x456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X 456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 X  456", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890 x 456  ", "GB"));
    assertEquals(ukNumber, phoneUtil.parse("+44 2034567890  X 456", "GB"));

    PhoneNumber usWithExtension =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(8009013355L)
            .setExtension("7246433").build();
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 x 7246433", "US"));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 , ext 7246433", "US"));
    assertEquals(usWithExtension,
                 phoneUtil.parse("(800) 901-3355 ,extension 7246433", "US"));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 , 7246433", "US"));
    assertEquals(usWithExtension, phoneUtil.parse("(800) 901-3355 ext: 7246433", "US"));
  }

  public void testCountryWithNoNumberDesc() {
    // Andorra is a country where we don't have PhoneNumberDesc info in the meta data.
    PhoneNumber adNumber =
        PhoneNumber.newBuilder().setCountryCode(376).setNationalNumber(12345L).build();
    assertEquals("+376 12345", phoneUtil.format(adNumber,
                                                PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
    assertEquals("+37612345", phoneUtil.format(adNumber,
                                                PhoneNumberUtil.PhoneNumberFormat.E164));
    assertEquals("12345", phoneUtil.format(adNumber,
                                           PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
    assertEquals(PhoneNumberUtil.PhoneNumberType.UNKNOWN,
        phoneUtil.getNumberType(adNumber));
    assertTrue(phoneUtil.isValidNumber(adNumber));

    // Test dialing a US number from within Andorra.
    PhoneNumber usNumber =
        PhoneNumber.newBuilder().setCountryCode(1).setNationalNumber(6502530000L).build();
    assertEquals("00 1 650 253 0000",
                 phoneUtil.formatOutOfCountryCallingNumber(usNumber, "AD"));
  }

  public void testUnknownCountryCallingCodeForValidation() {
    PhoneNumber.Builder invalidNumber = PhoneNumber.newBuilder();
    invalidNumber.setCountryCode(0).setNationalNumber(1234L);
    assertFalse(phoneUtil.isValidNumber(invalidNumber.build()));
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
    PhoneNumber nzNumber =
        PhoneNumber.newBuilder().setCountryCode(64).setNationalNumber(33316005L)
            .setExtension("3456").build();
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "+643 331 6005 ext 3456"));
    PhoneNumber nzNumber2 = PhoneNumber.newBuilder(nzNumber).clearExtension().build();
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber2, "+6403 331 6005"));
    // Check empty extensions are ignored.
    PhoneNumber nzNumber3 = PhoneNumber.newBuilder(nzNumber).setExtension("").build();
    assertEquals(PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber3, "+6403 331 6005"));
    // Check variant with two proto buffers.
    PhoneNumber nzNumber4 =
        PhoneNumber.newBuilder().setCountryCode(64).setNationalNumber(33316005L).build();
    assertEquals("Number " + nzNumber.toString() + " did not match " + nzNumber4.toString(),
                 PhoneNumberUtil.MatchType.EXACT_MATCH,
                 phoneUtil.isNumberMatch(nzNumber3, nzNumber4));
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
  }

  public void testIsNumberMatchNsnMatches() throws Exception {
    // NSN matches.
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("+64 3 331-6005", "03 331 6005"));
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch("3 331-6005", "03 331 6005"));
    PhoneNumber nzNumber =
        PhoneNumber.newBuilder().setCountryCode(64).setNationalNumber(33316005L)
            .setExtension("").build();
    assertEquals(PhoneNumberUtil.MatchType.NSN_MATCH,
                 phoneUtil.isNumberMatch(nzNumber, "03 331 6005"));
    PhoneNumber unchangedNzNumber = PhoneNumber.newBuilder().setCountryCode(64)
        .setNationalNumber(33316005L).setExtension("").build();
    // Check the phone number proto was not edited during the method call.
    assertEquals(unchangedNzNumber, nzNumber);
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
    PhoneNumber italianNumber1 =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(1234L)
            .setItalianLeadingZero(true).build();
    PhoneNumber italianNumber2 =
        PhoneNumber.newBuilder().setCountryCode(39).setNationalNumber(1234L).build();
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch(italianNumber1, italianNumber2));
    // One has an extension, the other has an extension of "".
    PhoneNumber italianNumber3 =
        PhoneNumber.newBuilder(italianNumber1).setExtension("1234")
            .clearItalianLeadingZero().build();
    PhoneNumber italianNumber4 =
        PhoneNumber.newBuilder(italianNumber2).setExtension("").build();
    assertEquals(PhoneNumberUtil.MatchType.SHORT_NSN_MATCH,
                 phoneUtil.isNumberMatch(italianNumber3, italianNumber4));
  }
}
