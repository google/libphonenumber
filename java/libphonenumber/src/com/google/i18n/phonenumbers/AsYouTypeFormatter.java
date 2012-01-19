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
 * <p>An AsYouTypeFormatter can be created by invoking
 * {@link PhoneNumberUtil#getAsYouTypeFormatter}. After that, digits can be added by invoking
 * {@link #inputDigit} on the formatter instance, and the partially formatted phone number will be
 * returned each time a digit is added. {@link #clear} can be invoked before formatting a new
 * number.
 *
 * <p>See the unittests for more details on how the formatter is to be used.
 *
 * @author Shaopeng Jia
 */
public class AsYouTypeFormatter {
  private String currentOutput = "";
  private StringBuilder formattingTemplate = new StringBuilder();
  // The pattern from numberFormat that is currently used to create formattingTemplate.
  private String currentFormattingPattern = "";
  private StringBuilder accruedInput = new StringBuilder();
  private StringBuilder accruedInputWithoutFormatting = new StringBuilder();
  // This indicates whether AsYouTypeFormatter is currently doing the formatting.
  private boolean ableToFormat = true;
  // Set to true when users enter their own formatting. AsYouTypeFormatter will do no formatting at
  // all when this is set to true.
  private boolean inputHasFormatting = false;
  private boolean isInternationalFormatting = false;
  private boolean isExpectingCountryCallingCode = false;
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private String defaultCountry;

  private static final PhoneMetadata EMPTY_METADATA =
      new PhoneMetadata().setInternationalPrefix("NA");
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

  // A pattern that is used to determine if a numberFormat under availableFormats is eligible to be
  // used by the AYTF. It is eligible when the format element under numberFormat contains groups of
  // the dollar sign followed by a single digit, separated by valid phone number punctuation. This
  // prevents invalid punctuation (such as the star sign in Israeli star numbers) getting into the
  // output of the AYTF.
  private static final Pattern ELIGIBLE_FORMAT_PATTERN =
      Pattern.compile("[" + PhoneNumberUtil.VALID_PUNCTUATION + "]*" +
          "(\\$\\d" + "[" + PhoneNumberUtil.VALID_PUNCTUATION + "]*)+");

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
  // This contains anything that has been entered so far preceding the national significant number,
  // and it is formatted (e.g. with space inserted). For example, this can contain IDD, country
  // code, and/or NDD, etc.
  private StringBuilder prefixBeforeNationalNumber = new StringBuilder();
  // This contains the national prefix that has been extracted. It contains only digits without
  // formatting.
  private String nationalPrefixExtracted = "";
  private StringBuilder nationalNumber = new StringBuilder();
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
    currentMetaData = getMetadataForRegion(defaultCountry);
    defaultMetaData = currentMetaData;
  }

  // The metadata needed by this class is the same for all regions sharing the same country calling
  // code. Therefore, we return the metadata for "main" region for this country calling code.
  private PhoneMetadata getMetadataForRegion(String regionCode) {
    int countryCallingCode = phoneUtil.getCountryCodeForRegion(regionCode);
    String mainCountry = phoneUtil.getRegionCodeForCountryCode(countryCallingCode);
    PhoneMetadata metadata = phoneUtil.getMetadataForRegion(mainCountry);
    if (metadata != null) {
      return metadata;
    }
    // Set to a default instance of the metadata. This allows us to function with an incorrect
    // region code, even if formatting only works for numbers specified with "+".
    return EMPTY_METADATA;
  }

  // Returns true if a new template is created as opposed to reusing the existing template.
  private boolean maybeCreateNewTemplate() {
    // When there are multiple available formats, the formatter uses the first format where a
    // formatting template could be created.
    Iterator<NumberFormat> it = possibleFormats.iterator();
    while (it.hasNext()) {
      NumberFormat numberFormat = it.next();
      String pattern = numberFormat.getPattern();
      if (currentFormattingPattern.equals(pattern)) {
        return false;
      }
      if (createFormattingTemplate(numberFormat)) {
        currentFormattingPattern = pattern;
        // With a new formatting template, the matched position using the old template needs to be
        // reset.
        lastMatchPosition = 0;
        return true;
      } else {  // Remove the current number format from possibleFormats.
        it.remove();
      }
    }
    ableToFormat = false;
    return false;
  }

  private void getAvailableFormats(String leadingThreeDigits) {
    List<NumberFormat> formatList =
        (isInternationalFormatting && currentMetaData.intlNumberFormatSize() > 0)
        ? currentMetaData.intlNumberFormats()
        : currentMetaData.numberFormats();
    for (NumberFormat format : formatList) {
      if (isFormatEligible(format.getFormat())) {
        possibleFormats.add(format);
      }
    }
    narrowDownPossibleFormats(leadingThreeDigits);
  }

  private boolean isFormatEligible(String format) {
    return ELIGIBLE_FORMAT_PATTERN.matcher(format).matches();
  }

  private void narrowDownPossibleFormats(String leadingDigits) {
    int indexOfLeadingDigitsPattern = leadingDigits.length() - MIN_LEADING_DIGITS_LENGTH;
    Iterator<NumberFormat> it = possibleFormats.iterator();
    while (it.hasNext()) {
      NumberFormat format = it.next();
      if (format.leadingDigitsPatternSize() > indexOfLeadingDigitsPattern) {
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
    String tempTemplate = getFormattingTemplate(numberPattern, format.getFormat());
    if (tempTemplate.length() > 0) {
      formattingTemplate.append(tempTemplate);
      return true;
    }
    return false;
  }

  // Gets a formatting template which can be used to efficiently format a partial number where
  // digits are added one by one.
  private String getFormattingTemplate(String numberPattern, String numberFormat) {
    // Creates a phone number consisting only of the digit 9 that matches the
    // numberPattern by applying the pattern to the longestPhoneNumber string.
    String longestPhoneNumber = "999999999999999";
    Matcher m = regexCache.getPatternForRegex(numberPattern).matcher(longestPhoneNumber);
    m.find();  // this will always succeed
    String aPhoneNumber = m.group();
    // No formatting template can be created if the number of digits entered so far is longer than
    // the maximum the current formatting rule can accommodate.
    if (aPhoneNumber.length() < nationalNumber.length()) {
      return "";
    }
    // Formats the number according to numberFormat
    String template = aPhoneNumber.replaceAll(numberPattern, numberFormat);
    // Replaces each digit with character digitPlaceholder
    template = template.replaceAll("9", digitPlaceholder);
    return template;
  }

  /**
   * Clears the internal state of the formatter, so it can be reused.
   */
  public void clear() {
    currentOutput = "";
    accruedInput.setLength(0);
    accruedInputWithoutFormatting.setLength(0);
    formattingTemplate.setLength(0);
    lastMatchPosition = 0;
    currentFormattingPattern = "";
    prefixBeforeNationalNumber.setLength(0);
    nationalPrefixExtracted = "";
    nationalNumber.setLength(0);
    ableToFormat = true;
    inputHasFormatting = false;
    positionToRemember = 0;
    originalPosition = 0;
    isInternationalFormatting = false;
    isExpectingCountryCallingCode = false;
    possibleFormats.clear();
    if (!currentMetaData.equals(defaultMetaData)) {
      currentMetaData = getMetadataForRegion(defaultCountry);
    }
  }

  /**
   * Formats a phone number on-the-fly as each digit is entered.
   *
   * @param nextChar  the most recently entered digit of a phone number. Formatting characters are
   *     allowed, but as soon as they are encountered this method formats the number as entered and
   *     not "as you type" anymore. Full width digits and Arabic-indic digits are allowed, and will
   *     be shown as they are.
   * @return  the partially formatted phone number.
   */
  public String inputDigit(char nextChar) {
    currentOutput = inputDigitWithOptionToRememberPosition(nextChar, false);
    return currentOutput;
  }

  /**
   * Same as {@link #inputDigit}, but remembers the position where {@code nextChar} is inserted, so
   * that it can be retrieved later by using {@link #getRememberedPosition}. The remembered
   * position will be automatically adjusted if additional formatting characters are later
   * inserted/removed in front of {@code nextChar}.
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
    // We do formatting on-the-fly only when each character entered is either a digit, or a plus
    // sign (accepted at the start of the number only).
    if (!isDigitOrLeadingPlusSign(nextChar)) {
      ableToFormat = false;
      inputHasFormatting = true;
    } else {
      nextChar = normalizeAndAccrueDigitsAndPlusSign(nextChar, rememberPosition);
    }
    if (!ableToFormat) {
      // When we are unable to format because of reasons other than that formatting chars have been
      // entered, it can be due to really long IDDs or NDDs. If that is the case, we might be able
      // to do formatting again after extracting them.
      if (inputHasFormatting) {
        return accruedInput.toString();
      } else if (attemptToExtractIdd()) {
        if (attemptToExtractCountryCallingCode()) {
          return attemptToChoosePatternWithPrefixExtracted();
        }
      } else if (ableToExtractLongerNdd()) {
        // Add an additional space to separate long NDD and national significant number for
        // readability.
        prefixBeforeNationalNumber.append(" ");
        return attemptToChoosePatternWithPrefixExtracted();
      }
      return accruedInput.toString();
    }

    // We start to attempt to format only when at least MIN_LEADING_DIGITS_LENGTH digits (the plus
    // sign is counted as a digit as well for this purpose) have been entered.
    switch (accruedInputWithoutFormatting.length()) {
      case 0:
      case 1:
      case 2:
        return accruedInput.toString();
      case 3:
        if (attemptToExtractIdd()) {
          isExpectingCountryCallingCode = true;
        } else {  // No IDD or plus sign is found, might be entering in national format.
          nationalPrefixExtracted = removeNationalPrefixFromNationalNumber();
          return attemptToChooseFormattingPattern();
        }
      default:
        if (isExpectingCountryCallingCode) {
          if (attemptToExtractCountryCallingCode()) {
            isExpectingCountryCallingCode = false;
          }
          return prefixBeforeNationalNumber + nationalNumber.toString();
        }
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
             : accruedInput.toString();
        } else {
          return attemptToChooseFormattingPattern();
        }
    }
  }

  private String attemptToChoosePatternWithPrefixExtracted() {
    ableToFormat = true;
    isExpectingCountryCallingCode = false;
    possibleFormats.clear();
    return attemptToChooseFormattingPattern();
  }

  // Some national prefixes are a substring of others. If extracting the shorter NDD doesn't result
  // in a number we can format, we try to see if we can extract a longer version here.
  private boolean ableToExtractLongerNdd() {
    if (nationalPrefixExtracted.length() > 0) {
      // Put the extracted NDD back to the national number before attempting to extract a new NDD.
      nationalNumber.insert(0, nationalPrefixExtracted);
      // Remove the previously extracted NDD from prefixBeforeNationalNumber. We cannot simply set
      // it to empty string because people sometimes enter national prefix after country code, e.g
      // +44 (0)20-1234-5678.
      int indexOfPreviousNdd = prefixBeforeNationalNumber.lastIndexOf(nationalPrefixExtracted);
      prefixBeforeNationalNumber.setLength(indexOfPreviousNdd);
    }
    return !nationalPrefixExtracted.equals(removeNationalPrefixFromNationalNumber());
  }

  private boolean isDigitOrLeadingPlusSign(char nextChar) {
    return Character.isDigit(nextChar) ||
        (accruedInput.length() == 1 &&
         PhoneNumberUtil.PLUS_CHARS_PATTERN.matcher(Character.toString(nextChar)).matches());
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
   * previously passed in as the parameter of {@link #inputDigitAndRememberPosition}.
   */
  public int getRememberedPosition() {
    if (!ableToFormat) {
      return originalPosition;
    }
    int accruedInputIndex = 0, currentOutputIndex = 0;
    while (accruedInputIndex < positionToRemember && currentOutputIndex < currentOutput.length()) {
      if (accruedInputWithoutFormatting.charAt(accruedInputIndex) ==
          currentOutput.charAt(currentOutputIndex)) {
        accruedInputIndex++;
      }
      currentOutputIndex++;
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
      return maybeCreateNewTemplate() ? inputAccruedNationalNumber() : accruedInput.toString();
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
          : accruedInput.toString();
    } else {
      return prefixBeforeNationalNumber.toString();
    }
  }

  // Returns the national prefix extracted, or an empty string if it is not present.
  private String removeNationalPrefixFromNationalNumber() {
    int startOfNationalNumber = 0;
    if (currentMetaData.getCountryCode() == 1 && nationalNumber.charAt(0) == '1') {
      startOfNationalNumber = 1;
      prefixBeforeNationalNumber.append("1 ");
      isInternationalFormatting = true;
    } else if (currentMetaData.hasNationalPrefixForParsing()) {
      Pattern nationalPrefixForParsing =
        regexCache.getPatternForRegex(currentMetaData.getNationalPrefixForParsing());
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
    String nationalPrefix = nationalNumber.substring(0, startOfNationalNumber);
    nationalNumber.delete(0, startOfNationalNumber);
    return nationalPrefix;
  }

  /**
   * Extracts IDD and plus sign to prefixBeforeNationalNumber when they are available, and places
   * the remaining input into nationalNumber.
   *
   * @return  true when accruedInputWithoutFormatting begins with the plus sign or valid IDD for
   *     defaultCountry.
   */
  private boolean attemptToExtractIdd() {
    Pattern internationalPrefix =
        regexCache.getPatternForRegex("\\" + PhoneNumberUtil.PLUS_SIGN + "|" +
            currentMetaData.getInternationalPrefix());
    Matcher iddMatcher = internationalPrefix.matcher(accruedInputWithoutFormatting);
    if (iddMatcher.lookingAt()) {
      isInternationalFormatting = true;
      int startOfCountryCallingCode = iddMatcher.end();
      nationalNumber.setLength(0);
      nationalNumber.append(accruedInputWithoutFormatting.substring(startOfCountryCallingCode));
      prefixBeforeNationalNumber.setLength(0);
      prefixBeforeNationalNumber.append(
          accruedInputWithoutFormatting.substring(0, startOfCountryCallingCode));
      if (accruedInputWithoutFormatting.charAt(0) != PhoneNumberUtil.PLUS_SIGN) {
        prefixBeforeNationalNumber.append(" ");
      }
      return true;
    }
    return false;
  }

  /**
   * Extracts the country calling code from the beginning of nationalNumber to
   * prefixBeforeNationalNumber when they are available, and places the remaining input into
   * nationalNumber.
   *
   * @return  true when a valid country calling code can be found.
   */
  private boolean attemptToExtractCountryCallingCode() {
    if (nationalNumber.length() == 0) {
      return false;
    }
    StringBuilder numberWithoutCountryCallingCode = new StringBuilder();
    int countryCode = phoneUtil.extractCountryCode(nationalNumber, numberWithoutCountryCallingCode);
    if (countryCode == 0) {
      return false;
    }
    nationalNumber.setLength(0);
    nationalNumber.append(numberWithoutCountryCallingCode);
    String newRegionCode = phoneUtil.getRegionCodeForCountryCode(countryCode);
    if (PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(newRegionCode)) {
      currentMetaData = phoneUtil.getMetadataForNonGeographicalRegion(countryCode);
    } else if (!newRegionCode.equals(defaultCountry)) {
      currentMetaData = getMetadataForRegion(newRegionCode);
    }
    String countryCodeString = Integer.toString(countryCode);
    prefixBeforeNationalNumber.append(countryCodeString).append(" ");
    return true;
  }

  // Accrues digits and the plus sign to accruedInputWithoutFormatting for later use. If nextChar
  // contains a digit in non-ASCII format (e.g. the full-width version of digits), it is first
  // normalized to the ASCII version. The return value is nextChar itself, or its normalized
  // version, if nextChar is a digit in non-ASCII format. This method assumes its input is either a
  // digit or the plus sign.
  private char normalizeAndAccrueDigitsAndPlusSign(char nextChar, boolean rememberPosition) {
    char normalizedChar;
    if (nextChar == PhoneNumberUtil.PLUS_SIGN) {
      normalizedChar = nextChar;
      accruedInputWithoutFormatting.append(nextChar);
    } else {
      int radix = 10;
      normalizedChar = Character.forDigit(Character.digit(nextChar, radix), radix);
      accruedInputWithoutFormatting.append(normalizedChar);
      nationalNumber.append(normalizedChar);
    }
    if (rememberPosition) {
      positionToRemember = accruedInputWithoutFormatting.length();
    }
    return normalizedChar;
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
