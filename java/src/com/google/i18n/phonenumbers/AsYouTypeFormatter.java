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
import java.util.Iterator;
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
  private String currentOutput = "";
  private StringBuffer formattingTemplate = new StringBuffer();
  // The pattern from numberFormat that is currently used to create formattingTemplate.
  private String currentFormattingPattern = "";
  private StringBuffer accruedInput = new StringBuffer();
  private StringBuffer accruedInputWithoutFormatting = new StringBuffer();
  private boolean ableToFormat = true;
  private boolean isInternationalFormatting = false;
  private boolean isExpectingCountryCode = false;
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private String defaultCountry;
  private PhoneMetadata defaultMetaData;
  private PhoneMetadata currentMetaData;

  // A pattern that is used to match character classes in regular expressions. An example of a
  // character class is [1-4].
  private static final Pattern CHARACTER_CLASS_PATTERN = Pattern.compile("\\[([^\\[\\]])*\\]");
  // Any digit in a regular expression that actually denotes a digit. For example, in the regular
  // expression 80[0-2]\d{6,10}, the first 2 digits (8 and 0) are standalone digits, but the rest
  // are not.
  // Two look-aheads are needed because the number following \\d could be a two-digit number, since
  // the phone number can be as long as 15 digits.
  private static final Pattern STANDALONE_DIGIT_PATTERN = Pattern.compile("\\d(?=[^,}][^,}])");

  // This is the minimum length of national number accrued that is required to trigger the
  // formatter. The first element of the leadingDigitsPattern of each numberFormat contains a
  // regular expression that matches up to this number of digits.
  private static final int MIN_LEADING_DIGITS_LENGTH = 3;

  // The digits that have not been entered yet will be represented by a \u2008, the punctuation
  // space.
  private String digitPlaceholder = "\u2008";
  private Pattern digitPattern = Pattern.compile(digitPlaceholder);
  private int lastMatchPosition = 0;
  // The position of a digit upon which inputDigitAndRememberPosition is most recently invoked, as
  // found in the original sequence of characters the user entered.
  private int originalPosition = 0;
  // The position of a digit upon which inputDigitAndRememberPosition is most recently invoked, as
  // found in accruedInputWithoutFormatting.
  private int positionToRemember = 0;
  private Pattern nationalPrefixForParsing;
  private Pattern internationalPrefix;
  private StringBuffer prefixBeforeNationalNumber = new StringBuffer();
  private StringBuffer nationalNumber = new StringBuffer();
  private List<NumberFormat> possibleFormats = new ArrayList<NumberFormat>();

    // A cache for frequently used country-specific regular expressions.
  private RegexCache regexCache = new RegexCache(64);

  /**
   * Constructs an as-you-type formatter. Should be obtained from {@link
   * PhoneNumberUtil#getAsYouTypeFormatter}.
   *
   * @param regionCode  the country/region where the phone number is being entered
   */
  AsYouTypeFormatter(String regionCode) {
    defaultCountry = regionCode;
    initializeCountrySpecificInfo(defaultCountry);
    defaultMetaData = currentMetaData;
  }

  private void initializeCountrySpecificInfo(String regionCode) {
    currentMetaData = phoneUtil.getMetadataForRegion(regionCode);
    if (currentMetaData == null) {
      // Set to a default instance of the metadata. This allows us to function with an incorrect
      // region code, even if formatting only works for numbers specified with "+".
      currentMetaData = new PhoneMetadata().setInternationalPrefix("NA");
    }
    nationalPrefixForParsing =
        regexCache.getPatternForRegex(currentMetaData.getNationalPrefixForParsing());
    internationalPrefix =
        regexCache.getPatternForRegex("\\+|" + currentMetaData.getInternationalPrefix());
  }

  // Returns true if a new template is created as opposed to reusing the existing template.
  private boolean maybeCreateNewTemplate() {
    // When there are multiple available formats, the formatter uses the first format where a
    // formatting template could be created.
    for (NumberFormat numberFormat : possibleFormats) {
      String pattern = numberFormat.getPattern();
      if (currentFormattingPattern.equals(pattern)) {
        return false;
      }
      if (createFormattingTemplate(numberFormat)) {
        currentFormattingPattern = pattern;
        return true;
      }
    }
    ableToFormat = false;
    return false;
  }

  private void getAvailableFormats(String leadingThreeDigits) {
    List<NumberFormat> formatList =
        (isInternationalFormatting && currentMetaData.getIntlNumberFormatCount() > 0)
        ? currentMetaData.getIntlNumberFormatList()
        : currentMetaData.getNumberFormatList();
    possibleFormats.addAll(formatList);
    narrowDownPossibleFormats(leadingThreeDigits);
  }

  private void narrowDownPossibleFormats(String leadingDigits) {
    int lengthOfLeadingDigits = leadingDigits.length();
    int indexOfLeadingDigitsPattern = lengthOfLeadingDigits - MIN_LEADING_DIGITS_LENGTH;
    Iterator<NumberFormat> it = possibleFormats.iterator();
    while (it.hasNext()) {
      NumberFormat format = it.next();
      if (format.getLeadingDigitsPatternCount() > indexOfLeadingDigitsPattern) {
        Pattern leadingDigitsPattern =
            regexCache.getPatternForRegex(
                format.getLeadingDigitsPattern(indexOfLeadingDigitsPattern));
        Matcher m = leadingDigitsPattern.matcher(leadingDigits);
        if (!m.lookingAt()) {
          it.remove();
        }
      } // else the particular format has no more specific leadingDigitsPattern, and it should be
        // retained.
    }
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
    formattingTemplate.setLength(0);
    String tempTemplate = getFormattingTemplate(numberPattern, numberFormat);
    if (tempTemplate.length() > nationalNumber.length()) {
      formattingTemplate.append(tempTemplate);
      return true;
    }
    return false;
  }

  // Gets a formatting template which could be used to efficiently format a partial number where
  // digits are added one by one.
  private String getFormattingTemplate(String numberPattern, String numberFormat) {
    // Creates a phone number consisting only of the digit 9 that matches the
    // numberPattern by applying the pattern to the longestPhoneNumber string.
    String longestPhoneNumber = "999999999999999";
    Matcher m = regexCache.getPatternForRegex(numberPattern).matcher(longestPhoneNumber);
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
    currentOutput = "";
    accruedInput.setLength(0);
    accruedInputWithoutFormatting.setLength(0);
    formattingTemplate.setLength(0);
    lastMatchPosition = 0;
    currentFormattingPattern = "";
    prefixBeforeNationalNumber.setLength(0);
    nationalNumber.setLength(0);
    ableToFormat = true;
    positionToRemember = 0;
    originalPosition = 0;
    isInternationalFormatting = false;
    isExpectingCountryCode = false;
    possibleFormats.clear();
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
    currentOutput = inputDigitWithOptionToRememberPosition(nextChar, false);
    return currentOutput;
  }

  /**
   * Same as inputDigit, but remembers the position where nextChar is inserted, so that it could be
   * retrieved later by using getRememberedPosition(). The remembered position will be automatically
   * adjusted if additional formatting characters are later inserted/removed in front of nextChar.
   */
  public String inputDigitAndRememberPosition(char nextChar) {
    currentOutput = inputDigitWithOptionToRememberPosition(nextChar, true);
    return currentOutput;
  }

  @SuppressWarnings("fallthrough")
  private String inputDigitWithOptionToRememberPosition(char nextChar, boolean rememberPosition) {
    accruedInput.append(nextChar);
    if (rememberPosition) {
      originalPosition = accruedInput.length();
    }
    // We do formatting on-the-fly only when each character entered is either a plus sign or a
    // digit.
    if (!PhoneNumberUtil.VALID_START_CHAR_PATTERN.matcher(Character.toString(nextChar)).matches()) {
      ableToFormat = false;
    }
    if (!ableToFormat) {
      return accruedInput.toString();
    }

    nextChar = normalizeAndAccrueDigitsAndPlusSign(nextChar, rememberPosition);

    // We start to attempt to format only when at least MIN_LEADING_DIGITS_LENGTH digits (the plus
    // sign is counted as a digit as well for this purpose) have been entered.
    switch (accruedInputWithoutFormatting.length()) {
      case 0: // this is the case where the first few inputs are neither digits nor the plus sign.
      case 1:
      case 2:
        return accruedInput.toString();
      case 3:
        if (attemptToExtractIdd()) {
          isExpectingCountryCode = true;
        } else {  // No IDD or plus sign is found, must be entering in national format.
          removeNationalPrefixFromNationalNumber();
          return attemptToChooseFormattingPattern();
        }
      case 4:
      case 5:
        if (isExpectingCountryCode) {
          if (attemptToExtractCountryCode()) {
            isExpectingCountryCode = false;
          }
          return prefixBeforeNationalNumber + nationalNumber.toString();
        }
      // We make a last attempt to extract a country code at the 6th digit because the maximum
      // length of IDD and country code are both 3.
      case 6:
        if (isExpectingCountryCode && !attemptToExtractCountryCode()) {
          ableToFormat = false;
          return accruedInput.toString();
        }
      default:
        if (possibleFormats.size() > 0) {  // The formatting pattern is already chosen.
          String tempNationalNumber = inputDigitHelper(nextChar);
          // See if the accrued digits can be formatted properly already. If not, use the results
          // from inputDigitHelper, which does formatting based on the formatting pattern chosen.
          String formattedNumber = attemptToFormatAccruedDigits();
          if (formattedNumber.length() > 0) {
            return formattedNumber;
          }
          narrowDownPossibleFormats(nationalNumber.toString());
          if (maybeCreateNewTemplate()) {
            return inputAccruedNationalNumber();
          }
          return ableToFormat
             ? prefixBeforeNationalNumber + tempNationalNumber
             : tempNationalNumber;
        } else {
          return attemptToChooseFormattingPattern();
        }
    }
  }

  String attemptToFormatAccruedDigits() {
    for (NumberFormat numFormat : possibleFormats) {
      Matcher m = regexCache.getPatternForRegex(numFormat.getPattern()).matcher(nationalNumber);
      if (m.matches()) {
        String formattedNumber = m.replaceAll(numFormat.getFormat());
        return prefixBeforeNationalNumber + formattedNumber;
      }
    }
    return "";
  }

  /**
   * Returns the current position in the partially formatted phone number of the character which was
   * previously passed in as the parameter of inputDigitAndRememberPosition().
   */
  public int getRememberedPosition() {
    if (!ableToFormat) {
      return originalPosition;
    }
    int accruedInputIndex = 0, currentOutputIndex = 0;
    int currentOutputLength = currentOutput.length();
    while (accruedInputIndex < positionToRemember && currentOutputIndex < currentOutputLength) {
      if (accruedInputWithoutFormatting.charAt(accruedInputIndex) ==
          currentOutput.charAt(currentOutputIndex)) {
        accruedInputIndex++;
        currentOutputIndex++;
      } else {
        currentOutputIndex++;
      }
    }
    return currentOutputIndex;
  }

  // Attempts to set the formatting template and returns a string which contains the formatted
  // version of the digits entered so far.
  private String attemptToChooseFormattingPattern() {
    // We start to attempt to format only when as least MIN_LEADING_DIGITS_LENGTH digits of national
    // number (excluding national prefix) have been entered.
    if (nationalNumber.length() >= MIN_LEADING_DIGITS_LENGTH) {
      getAvailableFormats(nationalNumber.substring(0, MIN_LEADING_DIGITS_LENGTH));
      maybeCreateNewTemplate();
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
      String tempNationalNumber = "";
      for (int i = 0; i < lengthOfNationalNumber; i++) {
        tempNationalNumber = inputDigitHelper(nationalNumber.charAt(i));
      }
      return ableToFormat
          ? prefixBeforeNationalNumber + tempNationalNumber
          : tempNationalNumber;
    } else {
      return prefixBeforeNationalNumber.toString();
    }
  }

  private void removeNationalPrefixFromNationalNumber() {
    int startOfNationalNumber = 0;
    if (currentMetaData.getCountryCode() == 1 && nationalNumber.charAt(0) == '1') {
      startOfNationalNumber = 1;
      prefixBeforeNationalNumber.append("1 ");
      isInternationalFormatting = true;
    } else if (currentMetaData.hasNationalPrefix()) {
      Matcher m = nationalPrefixForParsing.matcher(nationalNumber);
      if (m.lookingAt()) {
        // When the national prefix is detected, we use international formatting rules instead of
        // national ones, because national formatting rules could contain local formatting rules
        // for numbers entered without area code.
        isInternationalFormatting = true;
        startOfNationalNumber = m.end();
        prefixBeforeNationalNumber.append(nationalNumber.substring(0, startOfNationalNumber));
      }
    }
    nationalNumber.delete(0, startOfNationalNumber);
  }

  /**
   * Extracts IDD and plus sign to prefixBeforeNationalNumber when they are available, and places
   * the remaining input into nationalNumber.
   *
   * @return  true when accruedInputWithoutFormatting begins with the plus sign or valid IDD for
   *     defaultCountry.
   */
  private boolean attemptToExtractIdd() {
    Matcher iddMatcher = internationalPrefix.matcher(accruedInputWithoutFormatting);
    if (iddMatcher.lookingAt()) {
      isInternationalFormatting = true;
      int startOfCountryCode = iddMatcher.end();
      nationalNumber.setLength(0);
      nationalNumber.append(accruedInputWithoutFormatting.substring(startOfCountryCode));
      prefixBeforeNationalNumber.append(
          accruedInputWithoutFormatting.substring(0, startOfCountryCode));
      if (accruedInputWithoutFormatting.charAt(0) != PhoneNumberUtil.PLUS_SIGN) {
        prefixBeforeNationalNumber.append(" ");
      }
      return true;
    }
    return false;
  }

  /**
   * Extracts country code from the beginning of nationalNumber to prefixBeforeNationalNumber when
   * they are available, and places the remaining input into nationalNumber.
   *
   * @return  true when a valid country code can be found.
   */
  private boolean attemptToExtractCountryCode() {
    if (nationalNumber.length() == 0) {
      return false;
    }
    StringBuffer numberWithoutCountryCode = new StringBuffer();
    int countryCode = phoneUtil.extractCountryCode(nationalNumber, numberWithoutCountryCode);
    if (countryCode == 0) {
      return false;
    } else {
      nationalNumber.setLength(0);
      nationalNumber.append(numberWithoutCountryCode);
      String newRegionCode = phoneUtil.getRegionCodeForCountryCode(countryCode);
      if (!newRegionCode.equals(defaultCountry)) {
        initializeCountrySpecificInfo(newRegionCode);
      }
      String countryCodeString = Integer.toString(countryCode);
      prefixBeforeNationalNumber.append(countryCodeString).append(" ");
    }
    return true;
  }

  // Accrues digits and the plus sign to accruedInputWithoutFormatting for later use. If nextChar
  // contains a digit in non-ASCII format (e.g. the full-width version of digits), it is first
  // normalized to the ASCII version. The return value is nextChar itself, or its normalized
  // version, if nextChar is a digit in non-ASCII format.
  private char normalizeAndAccrueDigitsAndPlusSign(char nextChar, boolean rememberPosition) {
    if (nextChar == PhoneNumberUtil.PLUS_SIGN) {
      accruedInputWithoutFormatting.append(nextChar);
    }
    if (PhoneNumberUtil.DIGIT_MAPPINGS.containsKey(nextChar)) {
      nextChar = PhoneNumberUtil.DIGIT_MAPPINGS.get(nextChar);
      accruedInputWithoutFormatting.append(nextChar);
      nationalNumber.append(nextChar);
    }
    if (rememberPosition) {
      positionToRemember = accruedInputWithoutFormatting.length();
    }
    return nextChar;
  }

  private String inputDigitHelper(char nextChar) {
    Matcher digitMatcher = digitPattern.matcher(formattingTemplate);
    if (digitMatcher.find(lastMatchPosition)) {
      String tempTemplate = digitMatcher.replaceFirst(Character.toString(nextChar));
      formattingTemplate.replace(0, tempTemplate.length(), tempTemplate);
      lastMatchPosition = digitMatcher.start();
      return formattingTemplate.substring(0, lastMatchPosition + 1);
    } else {
      if (possibleFormats.size() == 1) {
        // More digits are entered than we could handle, and there are no other valid patterns to
        // try.
        ableToFormat = false;
      }  // else, we just reset the formatting pattern.
      currentFormattingPattern = "";
      return accruedInput.toString();
    }
  }
}
