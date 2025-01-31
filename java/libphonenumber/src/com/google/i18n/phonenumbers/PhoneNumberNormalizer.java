package com.google.i18n.phonenumbers;

/** Utility class for normalizing phone numbers. */
final class PhoneNumberNormalizer {
  private PhoneNumberNormalizer() {}

  /**
   * Normalizes a string of characters representing a phone number. This converts wide-ascii and
   * arabic-indic numerals to European numerals, and strips punctuation and alpha characters.
   *
   * @param number a string of characters representing a phone number
   * @return the normalized string version of the phone number
   */
  static String normalizeDigitsOnly(CharSequence number) {
    return normalizeDigits(number, false /* strip non-digits */).toString();
  }

  /**
   * Helper method for normalizing a string of characters representing a phone number. See {@link
   * PhoneNumberUtil#normalize(StringBuilder)} and {@link #normalizeDigitsOnly(CharSequence)} for
   * more details.
   *
   * @param number a string of characters representing a phone number
   * @param keepNonDigits whether to keep non-digits in the normalized string
   * @return the normalized string version of the phone number
   */
  static StringBuilder normalizeDigits(CharSequence number, boolean keepNonDigits) {
    StringBuilder normalizedDigits = new StringBuilder(number.length());
    for (int i = 0; i < number.length(); i++) {
      char c = number.charAt(i);
      int digit = Character.digit(c, 10);
      if (digit != -1) {
        normalizedDigits.append(digit);
      } else if (keepNonDigits) {
        normalizedDigits.append(c);
      }
    }
    return normalizedDigits;
  }
}
