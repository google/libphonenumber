package com.google.i18n.phonenumbers.internal;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;

/**
 * Internal phonenumber matching API used to isolate the underlying implementation of the
 * matcher and allow different implementations to be swapped in easily.
 */
public interface MatcherApi {
  /**
   * Returns whether the given national number (a string containing only decimal digits) matches
   * the national number pattern defined in the given {@code PhoneNumberDesc} message.
   */
  boolean matchesNationalNumber(String nationalNumber, PhoneNumberDesc numberDesc,
      boolean allowPrefixMatch);

  /**
   * Returns whether the given national number (a string containing only decimal digits) matches
   * the possible number pattern defined in the given {@code PhoneNumberDesc} message.
   */
  boolean matchesPossibleNumber(String nationalNumber, PhoneNumberDesc numberDesc);
}
