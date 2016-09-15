/*
 * Copyright (C) 2014 The Libphonenumber Authors
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
