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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A formatter which formats phone numbers as they are entered.
 *
 * An AsYouTypeFormatter could be created by invoking the getAsYouTypeFormatter method of the
 * PhoneNumberUtil. After that digits could be added by invoking the inputDigit method on the
 * formatter instance, and the partially formatted phone number will be returned each time a digit
 * is added. The clear method could be invoked before a new number needs to be formatted.
 *
 * See testAsYouTypeFormatterUS(), testAsYouTestFormatterGB() and testAsYouTypeFormatterDE() in
 * PhoneNumberUtilTest.java for more details on how the formatter is to be used.
 *
 * @author Shaopeng Jia
 */
public class AsYouTypeFormatter {
  private StringBuffer currentOutput;
  private String formattingTemplate;
  private StringBuffer accruedInput;
  private StringBuffer accruedInputWithoutFormatting;
  private boolean ableToFormat = true;
  private boolean isInternationalFormatting = false;
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private String defaultCountry;
  private Phonemetadata.PhoneMetadata defaultMetaData;
  private PhoneMetadata currentMetaData;

  // The digits that have not been entered yet will be represented by a \u2008, the punctuation
  // space.
  private String digitPlaceholder = "\u2008";
  private Pattern digitPattern = Pattern.compile(digitPlaceholder);
  private int lastMatchPosition = 0;
  private Pattern nationalPrefixForParsing;
  private Pattern internationalPrefix;
  private StringBuffer prefixBeforeNationalNumber;
  private StringBuffer nationalNumber;
  private final Pattern UNSUPPORTED_SYNTAX = Pattern.compile("[*#;,a-zA-Z]");
  private final Pattern CHARACTER_CLASS_PATTERN = Pattern.compile("\\[([^\\[\\]])*\\]");
  private final Pattern STANDALONE_DIGIT_PATTERN = Pattern.compile("\\d(?=[^,}][^,}])");

  /**
   * Constructs a light-weight formatter which does no formatting, but outputs exactly what is
   * fed into the inputDigit method.
   *
   * @param regionCode  the country/region where the phone number is being entered
   */
  AsYouTypeFormatter(String regionCode) {
    accruedInput = new StringBuffer();
    accruedInputWithoutFormatting = new StringBuffer();
    currentOutput = new StringBuffer();
    prefixBeforeNationalNumber = new StringBuffer();
    nationalNumber = new StringBuffer();
    defaultCountry = regionCode;
    initializeCountrySpecificInfo(defaultCountry);
    defaultMetaData = currentMetaData;
  }

  private void initializeCountrySpecificInfo(String regionCode) {
    currentMetaData = phoneUtil.getMetadataForRegion(regionCode);
    nationalPrefixForParsing =
        Pattern.compile(currentMetaData.getNationalPrefixForParsing());
    internationalPrefix =
        Pattern.compile("\\+|" + currentMetaData.getInternationalPrefix());
  }

  private void chooseFormatAndCreateTemplate(String leadingFourDigitsOfNationalNumber) {
    List<NumberFormat> formatList = getAvailableFormats(leadingFourDigitsOfNationalNumber);
    if (formatList.size() < 1) {
      ableToFormat = false;
    } else {
      // When there are multiple available formats, the formatter uses the first format.
      NumberFormat format = formatList.get(0);
      if (!createFormattingTemplate(format)) {
        ableToFormat = false;
      } else {
        currentOutput = new StringBuffer(formattingTemplate);
      }
    }
  }

  private List<NumberFormat> getAvailableFormats(String leadingFourDigits) {
    List<NumberFormat> matchedList = new ArrayList<NumberFormat>();
    List<NumberFormat> formatList =
        (isInternationalFormatting && currentMetaData.getIntlNumberFormatCount() > 0)
        ? currentMetaData.getIntlNumberFormatList()
        : currentMetaData.getNumberFormatList();
    for (NumberFormat format : formatList) {
      if (format.hasLeadingDigits()) {
        Pattern leadingDigitsPattern = Pattern.compile(format.getLeadingDigits());
        Matcher m = leadingDigitsPattern.matcher(leadingFourDigits);
        if (m.lookingAt()) {
          matchedList.add(format);
        }
      } else {
        matchedList.add(format);
      }
    }
    return matchedList;
  }

  private boolean createFormattingTemplate(NumberFormat format) {
    String numberFormat = format.getFormat();
    String numberPattern = format.getPattern();

    // The formatter doesn't format numbers when numberPattern contains "|", e.g.
    // (20|3)\d{4}. In those cases we quickly return.
    if (numberPattern.indexOf('|') != -1) {
      return false;
    }

    // Replace anything in the form of [..] with \d
    numberPattern = CHARACTER_CLASS_PATTERN.matcher(numberPattern).replaceAll("\\\\d");

    // Replace any standalone digit (not the one in d{}) with \d
    numberPattern = STANDALONE_DIGIT_PATTERN.matcher(numberPattern).replaceAll("\\\\d");

    formattingTemplate = getFormattingTemplate(numberPattern, numberFormat);
    return true;
  }

  // Gets a formatting template which could be used to efficiently format a partial number where
  // digits are added one by one.
  private String getFormattingTemplate(String numberPattern, String numberFormat) {
    // Creates a phone number consisting only of the digit 9 that matches the
    // numberPattern by applying the pattern to the longestPhoneNumber string.
    String longestPhoneNumber = "999999999999999";
    Matcher m = Pattern.compile(numberPattern).matcher(longestPhoneNumber);
    m.find();  // this will always succeed
    String aPhoneNumber = m.group();
    // Formats the number according to numberFormat
    String template = aPhoneNumber.replaceAll(numberPattern, numberFormat);
    // Replaces each digit with character digitPlaceholder
    template = template.replaceAll("9", digitPlaceholder);
    return template;
  }

  /**
   * Clears the internal state of the formatter, so it could be reused.
   */
  public void clear() {
    accruedInput.setLength(0);
    accruedInputWithoutFormatting.setLength(0);
    currentOutput.setLength(0);
    lastMatchPosition = 0;
    prefixBeforeNationalNumber.setLength(0);
    nationalNumber.setLength(0);
    ableToFormat = true;
    isInternationalFormatting = false;
    if (!currentMetaData.equals(defaultMetaData)) {
      initializeCountrySpecificInfo(defaultCountry);
    }
  }

  /**
   * Formats a phone number on-the-fly as each digit is entered.
   *
   * @param nextChar  the most recently entered digit of a phone number. Formatting characters are
   *     allowed, but they are removed from the result. Full width digits and Arabic-indic digits
   *     are allowed, and will be shown as they are.
   * @return  the partially formatted phone number.
   */
  public String inputDigit(char nextChar) {
    accruedInput.append(nextChar);
    // * and # are normally used in mobile codes, which we do not format.
    if (UNSUPPORTED_SYNTAX.matcher(Character.toString(nextChar)).matches()) {
      ableToFormat = false;
    }
    if (!ableToFormat) {
      return accruedInput.toString();
    }

    nextChar = normalizeAndAccrueDigitsAndPlusSign(nextChar);

    // We start to attempt to format only when at least 6 digits (the plus sign is counted as a
    // digit as well for this purpose) have been entered.
    switch (accruedInputWithoutFormatting.length()) {
      case 0: // this is the case where the first few inputs are neither digits nor the plus sign.
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
        return accruedInput.toString();
      case 6:
        if (!extractIddAndValidCountryCode()) {
          ableToFormat = false;
          return accruedInput.toString();
        }
        removeNationalPrefixFromNationalNumber();
        return attemptToChooseFormattingPattern();
      default:
        if (nationalNumber.length() > 4) {  // The formatting pattern is already chosen.
          return prefixBeforeNationalNumber + inputDigitHelper(nextChar);
        } else {
          return attemptToChooseFormattingPattern();
        }
    }
  }

  // Attempts to set the formatting template and returns a string which contains the formatted
  // version of the digits entered so far.
  private String attemptToChooseFormattingPattern() {
    // We start to attempt to format only when as least 4 digits of national number (excluding
    // national prefix) have been entered.
    if (nationalNumber.length() >= 4) {
      chooseFormatAndCreateTemplate(nationalNumber.substring(0, 4));
      return inputAccruedNationalNumber();
    } else {
      return prefixBeforeNationalNumber + nationalNumber.toString();
    }
  }

  // Invokes inputDigitHelper on each digit of the national number accrued, and returns a formatted
  // string in the end.
  private String inputAccruedNationalNumber() {
    int lengthOfNationalNumber = nationalNumber.length();
    if (lengthOfNationalNumber > 0) {
      for (int i = 0; i < lengthOfNationalNumber - 1; i++) {
        inputDigitHelper(nationalNumber.charAt(i));
      }
      return prefixBeforeNationalNumber
             + inputDigitHelper(nationalNumber.charAt(lengthOfNationalNumber - 1));
    } else {
      return prefixBeforeNationalNumber.toString();
    }
  }

  private void removeNationalPrefixFromNationalNumber() {
    int startOfNationalNumber = 0;
    if (currentMetaData.getCountryCode() == 1 && nationalNumber.charAt(0) == '1') {
      startOfNationalNumber = 1;
      prefixBeforeNationalNumber.append("1 ");
    } else if (currentMetaData.hasNationalPrefix()) {
      Matcher m = nationalPrefixForParsing.matcher(nationalNumber);
      if (m.lookingAt()) {
        startOfNationalNumber = m.end();
        prefixBeforeNationalNumber.append(nationalNumber.substring(0, startOfNationalNumber));
      }
    }
    nationalNumber.delete(0, startOfNationalNumber);
  }

  /**
   * Extracts IDD, plus sign and country code to prefixBeforeNationalNumber when they are available,
   * and places the remaining input into nationalNumber.
   *
   * @return  false when accruedInputWithoutFormatting begins with the plus sign or valid IDD for
   *     defaultCountry, but the sequence of digits after that does not form a valid country code.
   *     It returns true for all other cases.
   */
  private boolean extractIddAndValidCountryCode() {
    nationalNumber.setLength(0);
    Matcher iddMatcher = internationalPrefix.matcher(accruedInputWithoutFormatting);
    if (iddMatcher.lookingAt()) {
      isInternationalFormatting = true;
      int startOfCountryCode = iddMatcher.end();
      StringBuffer numberIncludeCountryCode =
          new StringBuffer(accruedInputWithoutFormatting.substring(startOfCountryCode));
      int countryCode = phoneUtil.extractCountryCode(numberIncludeCountryCode, nationalNumber);
      if (countryCode == 0) {
        return false;
      } else {
        String newRegionCode = phoneUtil.getRegionCodeForCountryCode(countryCode);
        if (!newRegionCode.equals(defaultCountry)) {
          initializeCountrySpecificInfo(newRegionCode);
        }
        prefixBeforeNationalNumber.append(
            accruedInputWithoutFormatting.substring(0, startOfCountryCode));
        if (accruedInputWithoutFormatting.charAt(0) != PhoneNumberUtil.PLUS_SIGN ) {
          prefixBeforeNationalNumber.append(" ");
        }
        prefixBeforeNationalNumber.append(countryCode).append(" ");
      }
    } else {
      nationalNumber.setLength(0);
      nationalNumber.append(accruedInputWithoutFormatting);
    }
    return true;
  }

  // Accrues digits and the plus sign to accruedInputWithoutFormatting for later use. If nextChar
  // contains a digit in non-ASCII format (e.g. the full-width version of digits), it is first
  // normalized to the ASCII version. The return value is nextChar itself, or its normalized
  // version, if nextChar is a digit in non-ASCII format.
  private char normalizeAndAccrueDigitsAndPlusSign(char nextChar) {
    if (nextChar == PhoneNumberUtil.PLUS_SIGN) {
      accruedInputWithoutFormatting.append(nextChar);
    }

    if (PhoneNumberUtil.DIGIT_MAPPINGS.containsKey(nextChar)) {
      nextChar = PhoneNumberUtil.DIGIT_MAPPINGS.get(nextChar);
      accruedInputWithoutFormatting.append(nextChar);
      nationalNumber.append(nextChar);
    }
    return nextChar;
  }

  private String inputDigitHelper(char nextChar) {
    if (!PhoneNumberUtil.DIGIT_MAPPINGS.containsKey(nextChar)) {
      return currentOutput.toString();
    }

    Matcher digitMatcher = digitPattern.matcher(currentOutput);
    if (digitMatcher.find(lastMatchPosition)) {
      currentOutput = new StringBuffer(digitMatcher.replaceFirst(Character.toString(nextChar)));
      lastMatchPosition = digitMatcher.start();
      return currentOutput.substring(0, lastMatchPosition + 1);
    } else {  // More digits are entered than we could handle.
      currentOutput.append(nextChar);
      ableToFormat = false;
      return currentOutput.toString();
    }
  }
}
