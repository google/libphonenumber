package com.google.phonenumbers.demoapp.phonenumbers;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.ShortNumberInfo;
import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp.FormattingState;

/**
 * Handles everything related to the formatting {@link PhoneNumberInApp}s to E.164 format (e.g.
 * {@code +41446681800}) using LibPhoneNumber ({@link PhoneNumberUtil}).
 */
public class PhoneNumberFormatting {

  private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
  private static final ShortNumberInfo shortNumberInfo = ShortNumberInfo.getInstance();

  private PhoneNumberFormatting() {}

  /**
   * Attempts to format the param {@code phoneNumberInApp} in E.164 format (e.g. {@code
   * +41446681800}) using the country from param {@code nameCodeToUse} (e.g. {@code CH}).
   *
   * @param phoneNumberInApp PhoneNumberInApp to format to E.164 format
   * @param nameCodeToUse String in format of a name code (e.g. {@code CH})
   * @param ignoreWhitespace boolean whether a phone number should be treated as {@link
   *     FormattingState#NUMBER_IS_ALREADY_IN_E164} instead of suggesting to remove whitespace if
   *     that whitespace is the only difference
   */
  public static void formatPhoneNumberInApp(
      PhoneNumberInApp phoneNumberInApp, String nameCodeToUse, boolean ignoreWhitespace) {
    PhoneNumber originalPhoneNumberParsed;

    // Check PARSING_ERROR
    try {
      originalPhoneNumberParsed =
          phoneNumberUtil.parse(phoneNumberInApp.getOriginalPhoneNumber(), nameCodeToUse);
    } catch (NumberParseException e) {
      phoneNumberInApp.setFormattingState(FormattingState.PARSING_ERROR);
      return;
    }

    // Check NUMBER_IS_SHORT_NUMBER
    if (shortNumberInfo.isValidShortNumber(originalPhoneNumberParsed)) {
      phoneNumberInApp.setFormattingState(FormattingState.NUMBER_IS_SHORT_NUMBER);
      return;
    }

    // Check NUMBER_IS_NOT_VALID
    if (!phoneNumberUtil.isValidNumber(originalPhoneNumberParsed)) {
      phoneNumberInApp.setFormattingState(FormattingState.NUMBER_IS_NOT_VALID);
      return;
    }

    String formattedPhoneNumber =
        phoneNumberUtil.format(originalPhoneNumberParsed, PhoneNumberFormat.E164);

    // Check NUMBER_IS_ALREADY_IN_E164
    if (ignoreWhitespace
        ? phoneNumberInApp
            .getOriginalPhoneNumber()
            .replaceAll("\\s+", "")
            .equals(formattedPhoneNumber)
        : phoneNumberInApp.getOriginalPhoneNumber().equals(formattedPhoneNumber)) {
      phoneNumberInApp.setFormattingState(FormattingState.NUMBER_IS_ALREADY_IN_E164);
      return;
    }

    phoneNumberInApp.setFormattedPhoneNumber(formattedPhoneNumber);
    phoneNumberInApp.setFormattingState(FormattingState.COMPLETED);
    phoneNumberInApp.setShouldContactBeUpdated(true);
  }
}
