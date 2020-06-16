/*
 * Copyright (C) 2017 The Libphonenumber Authors
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

import junit.framework.TestCase;

/**
 * Tests that all implementations of {@code MatcherApi} are consistent.
 */
public class MatcherTest extends TestCase {
  public void testRegexBasedMatcher() {
    checkMatcherBehavesAsExpected(RegexBasedMatcher.create());
  }

  private void checkMatcherBehavesAsExpected(MatcherApi matcher) {
    PhoneNumberDesc desc = createDesc("");
    // Test if there is no matcher data.
    assertInvalid(matcher, "1", desc);

    desc = createDesc("9\\d{2}");
    assertInvalid(matcher, "91", desc);
    assertInvalid(matcher, "81", desc);
    assertMatched(matcher, "911", desc);
    assertInvalid(matcher, "811", desc);
    assertTooLong(matcher, "9111", desc);
    assertInvalid(matcher, "8111", desc);

    desc = createDesc("\\d{1,2}");
    assertMatched(matcher, "2", desc);
    assertMatched(matcher, "20", desc);

    desc = createDesc("20?");
    assertMatched(matcher, "2", desc);
    assertMatched(matcher, "20", desc);

    desc = createDesc("2|20");
    assertMatched(matcher, "2", desc);
    // Subtle case where lookingAt() and matches() result in different end()s.
    assertMatched(matcher, "20", desc);
  }

  // Helper method to set national number fields in the PhoneNumberDesc proto. Empty fields won't be
  // set.
  private PhoneNumberDesc createDesc(String nationalNumberPattern) {
    PhoneNumberDesc.Builder desc = PhoneNumberDesc.newBuilder();
    if (nationalNumberPattern.length() > 0) {
      desc.setNationalNumberPattern(nationalNumberPattern);
    }
    return desc.build();
  }

  private void assertMatched(MatcherApi matcher, String number, PhoneNumberDesc desc) {
    assertTrue(String.format("%s should have matched %s.", number, toString(desc)),
        matcher.matchNationalNumber(number, desc, false));
    assertTrue(String.format("%s should have matched %s.", number, toString(desc)),
        matcher.matchNationalNumber(number, desc, true));
  }

  private void assertInvalid(MatcherApi matcher, String number, PhoneNumberDesc desc) {
    assertFalse(String.format("%s should not have matched %s.", number, toString(desc)),
        matcher.matchNationalNumber(number, desc, false));
    assertFalse(String.format("%s should not have matched %s.", number, toString(desc)),
        matcher.matchNationalNumber(number, desc, true));
  }

  private void assertTooLong(MatcherApi matcher, String number, PhoneNumberDesc desc) {
    assertFalse(String.format("%s should have been too long for %s.", number, toString(desc)),
        matcher.matchNationalNumber(number, desc, false));
    assertTrue(String.format("%s should have been too long for %s.", number, toString(desc)),
        matcher.matchNationalNumber(number, desc, true));
  }

  private String toString(PhoneNumberDesc desc) {
    StringBuilder strBuilder = new StringBuilder("pattern: ");
    if (desc.hasNationalNumberPattern()) {
      strBuilder.append(desc.getNationalNumberPattern());
    } else {
      strBuilder.append("none");
    }
    return strBuilder.toString();
  }
}
