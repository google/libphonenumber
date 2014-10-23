package com.google.i18n.phonenumbers.internal;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import com.google.i18n.phonenumbers.RegexCache;

import java.util.regex.Matcher;

/**
 * Implementation of the matcher API using the regular expressions in the PhoneNumberDesc
 * proto message to match numbers.
 */
public final class RegexBasedMatcher implements MatcherApi {
  public static MatcherApi create() {
    return new RegexBasedMatcher();
  }

  private final RegexCache regexCache = new RegexCache(100);

  private RegexBasedMatcher() {}

  // @Override
  public boolean matchesNationalNumber(String nationalNumber, PhoneNumberDesc numberDesc,
      boolean allowPrefixMatch) {
    Matcher nationalNumberPatternMatcher = regexCache.getPatternForRegex(
        numberDesc.getNationalNumberPattern()).matcher(nationalNumber);
    return nationalNumberPatternMatcher.matches()
        || (allowPrefixMatch && nationalNumberPatternMatcher.lookingAt());
  }

  // @Override
  public boolean matchesPossibleNumber(String nationalNumber, PhoneNumberDesc numberDesc) {
    Matcher possibleNumberPatternMatcher = regexCache.getPatternForRegex(
        numberDesc.getPossibleNumberPattern()).matcher(nationalNumber);
    return possibleNumberPatternMatcher.matches();
  }
}
