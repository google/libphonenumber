/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.i18n.phonenumbers.PhoneNumberUtil.Leniency;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Tests for {@link PhoneNumberMatcher}. This only tests basic functionality based on test metadata.
 *
 * @author Tom Hofmann
 * @see PhoneNumberUtilTest {@link PhoneNumberUtilTest} for the origin of the test data
 */
public class PhoneNumberMatcherTest extends TestCase {
  private PhoneNumberUtil phoneUtil;

  @Override
  protected void setUp() throws Exception {
    phoneUtil = PhoneNumberUtilTest.initializePhoneUtilForTesting();
  }

  /** See {@link PhoneNumberUtilTest#testParseNationalNumber()}. */
  public void testFindNationalNumber() throws Exception {
    // same cases as in testParseNationalNumber
    doTestFindInContext("033316005", "NZ");
    doTestFindInContext("33316005", "NZ");
    // National prefix attached and some formatting present.
    doTestFindInContext("03-331 6005", "NZ");
    doTestFindInContext("03 331 6005", "NZ");
    // Testing international prefixes.
    // Should strip country code.
    doTestFindInContext("0064 3 331 6005", "NZ");
    // Try again, but this time we have an international number with Region Code US. It should
    // recognize the country code and parse accordingly.
    doTestFindInContext("01164 3 331 6005", "US");
    doTestFindInContext("+64 3 331 6005", "US");

    doTestFindInContext("64(0)64123456", "NZ");
    // Check that using a "/" is fine in a phone number.
    doTestFindInContext("123/45678", "DE");
    doTestFindInContext("123-456-7890", "US");
  }

  /** See {@link PhoneNumberUtilTest#testParseWithInternationalPrefixes()}. */
  public void testFindWithInternationalPrefixes() throws Exception {
    doTestFindInContext("+1 (650) 333-6000", "NZ");
    doTestFindInContext("1-650-333-6000", "US");
    // Calling the US number from Singapore by using different service providers
    // 1st test: calling using SingTel IDD service (IDD is 001)
    doTestFindInContext("0011-650-333-6000", "SG");
    // 2nd test: calling using StarHub IDD service (IDD is 008)
    doTestFindInContext("0081-650-333-6000", "SG");
    // 3rd test: calling using SingTel V019 service (IDD is 019)
    doTestFindInContext("0191-650-333-6000", "SG");
    // Calling the US number from Poland
    doTestFindInContext("0~01-650-333-6000", "PL");
    // Using "++" at the start.
    doTestFindInContext("++1 (650) 333-6000", "PL");
    // Using a full-width plus sign.
    doTestFindInContext("\uFF0B1 (650) 333-6000", "SG");
    // The whole number, including punctuation, is here represented in full-width form.
    doTestFindInContext("\uFF0B\uFF11\u3000\uFF08\uFF16\uFF15\uFF10\uFF09" +
        "\u3000\uFF13\uFF13\uFF13\uFF0D\uFF16\uFF10\uFF10\uFF10",
        "SG");
  }

  /** See {@link PhoneNumberUtilTest#testParseWithLeadingZero()}. */
  public void testFindWithLeadingZero() throws Exception {
    doTestFindInContext("+39 02-36618 300", "NZ");
    doTestFindInContext("02-36618 300", "IT");
    doTestFindInContext("312 345 678", "IT");
  }

  /** See {@link PhoneNumberUtilTest#testParseNationalNumberArgentina()}. */
  public void testFindNationalNumberArgentina() throws Exception {
    // Test parsing mobile numbers of Argentina.
    doTestFindInContext("+54 9 343 555 1212", "AR");
    doTestFindInContext("0343 15 555 1212", "AR");

    doTestFindInContext("+54 9 3715 65 4320", "AR");
    doTestFindInContext("03715 15 65 4320", "AR");

    // Test parsing fixed-line numbers of Argentina.
    doTestFindInContext("+54 11 3797 0000", "AR");
    doTestFindInContext("011 3797 0000", "AR");

    doTestFindInContext("+54 3715 65 4321", "AR");
    doTestFindInContext("03715 65 4321", "AR");

    doTestFindInContext("+54 23 1234 0000", "AR");
    doTestFindInContext("023 1234 0000", "AR");
  }

  /** See {@link PhoneNumberUtilTest#testParseWithXInNumber()}. */
  public void testFindWithXInNumber() throws Exception {
    doTestFindInContext("(0xx) 123456789", "AR");

    // This test is intentionally constructed such that the number of digit after xx is larger than
    // 7, so that the number won't be mistakenly treated as an extension, as we allow extensions up
    // to 7 digits. This assumption is okay for now as all the countries where a carrier selection
    // code is written in the form of xx have a national significant number of length larger than 7.
    doTestFindInContext("011xx5481429712", "US");
  }

  /** See {@link PhoneNumberUtilTest#testParseNumbersMexico()}. */
  public void testFindNumbersMexico() throws Exception {
    // Test parsing fixed-line numbers of Mexico.
    doTestFindInContext("+52 (449)978-0001", "MX");
    doTestFindInContext("01 (449)978-0001", "MX");
    doTestFindInContext("(449)978-0001", "MX");

    // Test parsing mobile numbers of Mexico.
    doTestFindInContext("+52 1 33 1234-5678", "MX");
    doTestFindInContext("044 (33) 1234-5678", "MX");
    doTestFindInContext("045 33 1234-5678", "MX");
  }

  /** See {@link PhoneNumberUtilTest#testParseNumbersWithPlusWithNoRegion()}. */
  public void testFindNumbersWithPlusWithNoRegion() throws Exception {
    // "ZZ" is allowed only if the number starts with a '+' - then the country code can be
    // calculated.
    doTestFindInContext("+64 3 331 6005", "ZZ");
    // Null is also allowed for the region code in these cases.
    doTestFindInContext("+64 3 331 6005", null);
  }

  /** See {@link PhoneNumberUtilTest#testParseExtensions()}. */
  public void testFindExtensions() throws Exception {
    doTestFindInContext("03 331 6005 ext 3456", "NZ");
    doTestFindInContext("03-3316005x3456", "NZ");
    doTestFindInContext("03-3316005 int.3456", "NZ");
    doTestFindInContext("03 3316005 #3456", "NZ");
    doTestFindInContext("0~0 1800 7493 524", "PL");
    doTestFindInContext("(1800) 7493.524", "US");
    // Check that the last instance of an extension token is matched.
    doTestFindInContext("0~0 1800 7493 524 ~1234", "PL");
    // Verifying bug-fix where the last digit of a number was previously omitted if it was a 0 when
    // extracting the extension. Also verifying a few different cases of extensions.
    doTestFindInContext("+44 2034567890x456", "NZ");
    doTestFindInContext("+44 2034567890x456", "GB");
    doTestFindInContext("+44 2034567890 x456", "GB");
    doTestFindInContext("+44 2034567890 X456", "GB");
    doTestFindInContext("+44 2034567890 X 456", "GB");
    doTestFindInContext("+44 2034567890 X  456", "GB");
    doTestFindInContext("+44 2034567890  X 456", "GB");

    doTestFindInContext("(800) 901-3355 x 7246433", "US");
    doTestFindInContext("(800) 901-3355 , ext 7246433", "US");
    doTestFindInContext("(800) 901-3355 ,extension 7246433", "US");
    doTestFindInContext("(800) 901-3355 , 7246433", "US");
    doTestFindInContext("(800) 901-3355 ext: 7246433", "US");
  }

  public void testFindInterspersedWithSpace() throws Exception {
    doTestFindInContext("0 3   3 3 1   6 0 0 5", "NZ");
  }

  /**
   * Test matching behavior when starting in the middle of a phone number.
   */
  public void testIntermediateParsePositions() throws Exception {
    String text = "Call 033316005  or 032316005!";
    //             |    |    |    |    |    |
    //             0    5   10   15   20   25

    // Iterate over all possible indices.
    for (int i = 0; i <= 5; i++) {
      assertEqualRange(text, i, 5, 14);
    }
    // 7 and 8 digits in a row are still parsed as number.
    assertEqualRange(text, 6, 6, 14);
    assertEqualRange(text, 7, 7, 14);
    // Anything smaller is skipped to the second instance.
    for (int i = 8; i <= 19; i++) {
      assertEqualRange(text, i, 19, 28);
    }
  }

  public void testMatchWithSurroundingZipcodes() throws Exception {
    String number = "415-666-7777";
    String zipPreceding = "My address is CA 34215. " + number + " is my number.";
    PhoneNumber expectedResult = phoneUtil.parse(number, "US");

    Iterator<PhoneNumberMatch> iterator = phoneUtil.findNumbers(zipPreceding, "US").iterator();
    PhoneNumberMatch match = iterator.hasNext() ? iterator.next() : null;
    assertNotNull("Did not find a number in '" + zipPreceding + "'; expected " + number, match);
    assertEquals(expectedResult, match.number());
    assertEquals(number, match.rawString());

    // Now repeat, but this time the phone number has spaces in it. It should still be found.
    number = "(415) 666 7777";

    String zipFollowing = "My number is " + number + ". 34215 is my zip-code.";
    iterator = phoneUtil.findNumbers(zipFollowing, "US").iterator();

    PhoneNumberMatch matchWithSpaces = iterator.hasNext() ? iterator.next() : null;
    assertNotNull("Did not find a number in '" + zipFollowing + "'; expected " + number,
                  matchWithSpaces);
    assertEquals(expectedResult, matchWithSpaces.number());
    assertEquals(number, matchWithSpaces.rawString());
  }

  public void testIsLatinLetter() throws Exception {
    assertTrue(PhoneNumberMatcher.isLatinLetter('c'));
    assertTrue(PhoneNumberMatcher.isLatinLetter('C'));
    assertTrue(PhoneNumberMatcher.isLatinLetter('\u00C9'));
    assertTrue(PhoneNumberMatcher.isLatinLetter('\u0301'));  // Combining acute accent
    // Punctuation, digits and white-space are not considered "latin letters".
    assertFalse(PhoneNumberMatcher.isLatinLetter(':'));
    assertFalse(PhoneNumberMatcher.isLatinLetter('5'));
    assertFalse(PhoneNumberMatcher.isLatinLetter('-'));
    assertFalse(PhoneNumberMatcher.isLatinLetter('.'));
    assertFalse(PhoneNumberMatcher.isLatinLetter(' '));
    assertFalse(PhoneNumberMatcher.isLatinLetter('\u6211'));  // Chinese character
  }

  public void testMatchesWithSurroundingLatinChars() throws Exception {
    ArrayList<NumberContext> contextPairs = new ArrayList<NumberContext>(5);
    contextPairs.add(new NumberContext("abc", "def"));
    contextPairs.add(new NumberContext("abc", ""));
    contextPairs.add(new NumberContext("", "def"));
    // Latin small letter e with an acute accent.
    contextPairs.add(new NumberContext("\u00C9", ""));
    // Same character decomposed (with combining mark).
    contextPairs.add(new NumberContext("e\u0301", ""));

    // Numbers should not be considered valid, if they are surrounded by Latin characters, but
    // should be considered possible.
    findMatchesInContexts(contextPairs, false, true);
  }

  public void testMatchesWithSurroundingLatinCharsAndLeadingPunctuation() throws Exception {
    // Contexts with trailing characters. Leading characters are okay here since the numbers we will
    // insert start with punctuation, but trailing characters are still not allowed.
    ArrayList<NumberContext> possibleOnlyContexts = new ArrayList<NumberContext>(3);
    possibleOnlyContexts.add(new NumberContext("abc", "def"));
    possibleOnlyContexts.add(new NumberContext("", "def"));
    possibleOnlyContexts.add(new NumberContext("", "\u00C9"));

    // Numbers should not be considered valid, if they have trailing Latin characters, but should be
    // considered possible.
    String numberWithPlus = "+14156667777";
    String numberWithBrackets = "(415)6667777";
    findMatchesInContexts(possibleOnlyContexts, false, true, "US", numberWithPlus);
    findMatchesInContexts(possibleOnlyContexts, false, true, "US", numberWithBrackets);

    ArrayList<NumberContext> validContexts = new ArrayList<NumberContext>(4);
    validContexts.add(new NumberContext("abc", ""));
    validContexts.add(new NumberContext("\u00C9", ""));
    validContexts.add(new NumberContext("\u00C9", "."));  // Trailing punctuation.
    validContexts.add(new NumberContext("\u00C9", " def"));  // Trailing white-space.

    // Numbers should be considered valid, since they start with punctuation.
    findMatchesInContexts(validContexts, true, true, "US", numberWithPlus);
    findMatchesInContexts(validContexts, true, true, "US", numberWithBrackets);
  }

  public void testMatchesWithSurroundingChineseChars() throws Exception {
    ArrayList<NumberContext> validContexts = new ArrayList<NumberContext>(3);
    validContexts.add(new NumberContext("\u6211\u7684\u7535\u8BDD\u53F7\u7801\u662F", ""));
    validContexts.add(new NumberContext("", "\u662F\u6211\u7684\u7535\u8BDD\u53F7\u7801"));
    validContexts.add(new NumberContext("\u8BF7\u62E8\u6253", "\u6211\u5728\u660E\u5929"));

    // Numbers should be considered valid, since they are surrounded by Chinese.
    findMatchesInContexts(validContexts, true, true);
  }

  public void testMatchesWithSurroundingPunctuation() throws Exception {
    ArrayList<NumberContext> validContexts = new ArrayList<NumberContext>(4);
    validContexts.add(new NumberContext("My number-", ""));  // At end of text.
    validContexts.add(new NumberContext("", ".Nice day."));  // At start of text.
    validContexts.add(new NumberContext("Tel:", "."));  // Punctuation surrounds number.
    validContexts.add(new NumberContext("Tel: ", " on Saturdays."));  // White-space is also fine.

    // Numbers should be considered valid, since they are surrounded by punctuation.
    findMatchesInContexts(validContexts, true, true);
  }

  /**
   * Helper method which tests the contexts provided and ensures that:
   * -- if isValid is true, they all find a test number inserted in the middle when leniency of
   *  matching is set to VALID; else no test number should be extracted at that leniency level
   * -- if isPossible is true, they all find a test number inserted in the middle when leniency of
   *  matching is set to POSSIBLE; else no test number should be extracted at that leniency level
   */
  private void findMatchesInContexts(List<NumberContext> contexts, boolean isValid,
                                     boolean isPossible, String region, String number) {
    if (isValid) {
      doTestInContext(number, region, contexts, Leniency.VALID);
    } else {
      for (NumberContext context : contexts) {
        String text = context.leadingText + number + context.trailingText;
        assertTrue("Should not have found a number in " + text,
                   hasNoMatches(phoneUtil.findNumbers(text, region)));
      }
    }
    if (isPossible) {
      doTestInContext(number, region, contexts, Leniency.POSSIBLE);
    } else {
      for (NumberContext context : contexts) {
        String text = context.leadingText + number + context.trailingText;
        assertTrue("Should not have found a number in " + text,
                   hasNoMatches(phoneUtil.findNumbers(text, region, Leniency.POSSIBLE,
                                                      Long.MAX_VALUE)));
      }
    }
  }

  /**
   * Variant of findMatchesInContexts that uses a default number and region.
   */
  private void findMatchesInContexts(List<NumberContext> contexts, boolean isValid,
                                     boolean isPossible) {
    String region = "US";
    String number = "415-666-7777";

    findMatchesInContexts(contexts, isValid, isPossible, region, number);
  }

  public void testNonMatchingBracketsAreInvalid() throws Exception {
    // The digits up to the ", " form a valid US number, but it shouldn't be matched as one since
    // there was a non-matching bracket present.
    assertTrue(hasNoMatches(phoneUtil.findNumbers(
        "80.585 [79.964, 81.191]", "US")));

    // The trailing "]" is thrown away before parsing, so the resultant number, while a valid US
    // number, does not have matching brackets.
    assertTrue(hasNoMatches(phoneUtil.findNumbers(
        "80.585 [79.964]", "US")));

    assertTrue(hasNoMatches(phoneUtil.findNumbers(
        "80.585 ((79.964)", "US")));

    // This case has too many sets of brackets to be valid.
    assertTrue(hasNoMatches(phoneUtil.findNumbers(
        "(80).(585) (79).(9)64", "US")));
  }

  public void testNoMatchIfRegionIsNull() throws Exception {
    // Fail on non-international prefix if region code is null.
    assertTrue(hasNoMatches(phoneUtil.findNumbers(
        "Random text body - number is 0331 6005, see you there", null)));
  }

  public void testNoMatchInEmptyString() throws Exception {
    assertTrue(hasNoMatches(phoneUtil.findNumbers("", "US")));
    assertTrue(hasNoMatches(phoneUtil.findNumbers("  ", "US")));
  }

  public void testNoMatchIfNoNumber() throws Exception {
    assertTrue(hasNoMatches(phoneUtil.findNumbers(
        "Random text body - number is foobar, see you there", "US")));
  }

  public void testSequences() throws Exception {
    // Test multiple occurrences.
    String text = "Call 033316005  or 032316005!";
    String region = "NZ";

    PhoneNumber number1 = new PhoneNumber();
    number1.setCountryCode(phoneUtil.getCountryCodeForRegion(region));
    number1.setNationalNumber(33316005);
    PhoneNumberMatch match1 = new PhoneNumberMatch(5, "033316005", number1);

    PhoneNumber number2 = new PhoneNumber();
    number2.setCountryCode(phoneUtil.getCountryCodeForRegion(region));
    number2.setNationalNumber(32316005);
    PhoneNumberMatch match2 = new PhoneNumberMatch(19, "032316005", number2);

    Iterator<PhoneNumberMatch> matches =
        phoneUtil.findNumbers(text, region, Leniency.POSSIBLE, Long.MAX_VALUE).iterator();

    assertEquals(match1, matches.next());
    assertEquals(match2, matches.next());
  }

  public void testNullInput() throws Exception {
    assertTrue(hasNoMatches(phoneUtil.findNumbers(null, "US")));
    assertTrue(hasNoMatches(phoneUtil.findNumbers(null, null)));
  }

  public void testMaxMatches() throws Exception {
    // Set up text with 100 valid phone numbers.
    StringBuilder numbers = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      numbers.append("My info: 415-666-7777,");
    }

    // Matches all 100. Max only applies to failed cases.
    List<PhoneNumber> expected = new ArrayList<PhoneNumber>(100);
    PhoneNumber number = phoneUtil.parse("+14156667777", null);
    for (int i = 0; i < 100; i++) {
      expected.add(number);
    }

    Iterable<PhoneNumberMatch> iterable =
        phoneUtil.findNumbers(numbers.toString(), "US", Leniency.VALID, 10);
    List<PhoneNumber> actual = new ArrayList<PhoneNumber>(100);
    for (PhoneNumberMatch match : iterable) {
      actual.add(match.number());
    }
    assertEquals(expected, actual);
  }

  public void testMaxMatchesInvalid() throws Exception {
    // Set up text with 10 invalid phone numbers followed by 100 valid.
    StringBuilder numbers = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      numbers.append("My address 949-8945-0");
    }
    for (int i = 0; i < 100; i++) {
      numbers.append("My info: 415-666-7777,");
    }

    Iterable<PhoneNumberMatch> iterable =
        phoneUtil.findNumbers(numbers.toString(), "US", Leniency.VALID, 10);
    assertFalse(iterable.iterator().hasNext());
  }

  public void testMaxMatchesMixed() throws Exception {
    // Set up text with 100 valid numbers inside an invalid number.
    StringBuilder numbers = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      numbers.append("My info: 415-666-7777 123 fake street");
    }

    // Only matches the first 5 despite there being 100 numbers due to max matches.
    // There are two false positives per line as "123" is also tried.
    List<PhoneNumber> expected = new ArrayList<PhoneNumber>(100);
    PhoneNumber number = phoneUtil.parse("+14156667777", null);
    for (int i = 0; i < 5; i++) {
      expected.add(number);
    }

    Iterable<PhoneNumberMatch> iterable =
        phoneUtil.findNumbers(numbers.toString(), "US", Leniency.VALID, 10);
    List<PhoneNumber> actual = new ArrayList<PhoneNumber>(100);
    for (PhoneNumberMatch match : iterable) {
      actual.add(match.number());
    }
    assertEquals(expected, actual);
  }

  public void testEmptyIteration() throws Exception {
    Iterable<PhoneNumberMatch> iterable = phoneUtil.findNumbers("", "ZZ");
    Iterator<PhoneNumberMatch> iterator = iterable.iterator();

    assertFalse(iterator.hasNext());
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("Violation of the Iterator contract.");
    } catch (NoSuchElementException e) { /* Success */ }
    assertFalse(iterator.hasNext());
  }

  public void testSingleIteration() throws Exception {
    Iterable<PhoneNumberMatch> iterable = phoneUtil.findNumbers("+14156667777", "ZZ");

    // With hasNext() -> next().
    Iterator<PhoneNumberMatch> iterator = iterable.iterator();
    // Double hasNext() to ensure it does not advance.
    assertTrue(iterator.hasNext());
    assertTrue(iterator.hasNext());
    assertNotNull(iterator.next());
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("Violation of the Iterator contract.");
    } catch (NoSuchElementException e) { /* Success */ }
    assertFalse(iterator.hasNext());

    // With next() only.
    iterator = iterable.iterator();
    assertNotNull(iterator.next());
    try {
      iterator.next();
      fail("Violation of the Iterator contract.");
    } catch (NoSuchElementException e) { /* Success */ }
  }

  public void testDoubleIteration() throws Exception {
    Iterable<PhoneNumberMatch> iterable =
        phoneUtil.findNumbers("+14156667777 foobar +14156667777 ", "ZZ");

    // With hasNext() -> next().
    Iterator<PhoneNumberMatch> iterator = iterable.iterator();
    // Double hasNext() to ensure it does not advance.
    assertTrue(iterator.hasNext());
    assertTrue(iterator.hasNext());
    assertNotNull(iterator.next());
    assertTrue(iterator.hasNext());
    assertTrue(iterator.hasNext());
    assertNotNull(iterator.next());
    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("Violation of the Iterator contract.");
    } catch (NoSuchElementException e) { /* Success */ }
    assertFalse(iterator.hasNext());

    // With next() only.
    iterator = iterable.iterator();
    assertNotNull(iterator.next());
    assertNotNull(iterator.next());
    try {
      iterator.next();
      fail("Violation of the Iterator contract.");
    } catch (NoSuchElementException e) { /* Success */ }
  }

  /**
   * Ensures that {@link Iterator#remove()} is not supported and that calling it does not
   * change iteration behavior.
   */
  public void testRemovalNotSupported() throws Exception {
    Iterable<PhoneNumberMatch> iterable = phoneUtil.findNumbers("+14156667777", "ZZ");

    Iterator<PhoneNumberMatch> iterator = iterable.iterator();
    try {
      iterator.remove();
      fail("Iterator must not support remove.");
    } catch (UnsupportedOperationException e) { /* success */ }

    assertTrue(iterator.hasNext());

    try {
      iterator.remove();
      fail("Iterator must not support remove.");
    } catch (UnsupportedOperationException e) { /* success */ }

    assertNotNull(iterator.next());

    try {
      iterator.remove();
      fail("Iterator must not support remove.");
    } catch (UnsupportedOperationException e) { /* success */ }

    assertFalse(iterator.hasNext());
  }

  /**
   * Asserts that another number can be found in {@code text} starting at {@code index}, and that
   * its corresponding range is {@code [start, end)}.
   */
  private void assertEqualRange(CharSequence text, int index, int start, int end) {
    CharSequence sub = text.subSequence(index, text.length());
    Iterator<PhoneNumberMatch> matches =
      phoneUtil.findNumbers(sub, "NZ", Leniency.POSSIBLE, Long.MAX_VALUE).iterator();
    assertTrue(matches.hasNext());
    PhoneNumberMatch match = matches.next();
    assertEquals(start - index, match.start());
    assertEquals(end - index, match.end());
    assertEquals(match.rawString(), sub.subSequence(match.start(), match.end()).toString());
  }

  /**
   * Tests numbers found by {@link PhoneNumberUtil#findNumbers(CharSequence, String)} in various
   * textual contexts.
   *
   * @param number the number to test and the corresponding region code to use
   */
  private void doTestFindInContext(String number, String defaultCountry) throws Exception {
    findPossibleInContext(number, defaultCountry);

    PhoneNumber parsed = phoneUtil.parse(number, defaultCountry);
    if (phoneUtil.isValidNumber(parsed)) {
      findValidInContext(number, defaultCountry);
    }
  }

  /**
   * Tests valid numbers in contexts that should pass for {@link Leniency#POSSIBLE}.
   */
  private void findPossibleInContext(String number, String defaultCountry) {
    ArrayList<NumberContext> contextPairs = new ArrayList<NumberContext>(15);
    contextPairs.add(new NumberContext("", ""));  // no context
    contextPairs.add(new NumberContext("   ", "\t"));  // whitespace only
    contextPairs.add(new NumberContext("Hello ", ""));  // no context at end
    contextPairs.add(new NumberContext("", " to call me!"));  // no context at start
    contextPairs.add(new NumberContext("Hi there, call ", " to reach me!"));  // no context at start
    contextPairs.add(new NumberContext("Hi there, call ", ", or don't"));  // with commas
    // Three examples without whitespace around the number.
    contextPairs.add(new NumberContext("Hi call", ""));
    contextPairs.add(new NumberContext("", "forme"));
    contextPairs.add(new NumberContext("Hi call", "forme"));
    // With other small numbers.
    contextPairs.add(new NumberContext("It's cheap! Call ", " before 6:30"));
    // With a second number later.
    contextPairs.add(new NumberContext("Call ", " or +1800-123-4567!"));
    contextPairs.add(new NumberContext("Call me on June 21 at", ""));  // with a Month-Day date
    // With publication pages.
    contextPairs.add(new NumberContext(
        "As quoted by Alfonso 12-15 (2009), you may call me at ", ""));
    contextPairs.add(new NumberContext(
        "As quoted by Alfonso et al. 12-15 (2009), you may call me at ", ""));
    // With dates, written in the American style.
    contextPairs.add(new NumberContext(
        "As I said on 03/10/2011, you may call me at ", ""));
    contextPairs.add(new NumberContext(
        "As I said on 03/27/2011, you may call me at ", ""));
    contextPairs.add(new NumberContext(
        "As I said on 31/8/2011, you may call me at ", ""));
    contextPairs.add(new NumberContext(
        "As I said on 1/12/2011, you may call me at ", ""));
    contextPairs.add(new NumberContext(
        "I was born on 10/12/82. Please call me at ", ""));
    // With a postfix stripped off as it looks like the start of another number
    contextPairs.add(new NumberContext("Call ", "/x12 more"));

    doTestInContext(number, defaultCountry, contextPairs, Leniency.POSSIBLE);
  }

  /**
   * Tests valid numbers in contexts that fail for {@link Leniency#POSSIBLE} but are valid for
   * {@link Leniency#VALID}.
   */
  private void findValidInContext(String number, String defaultCountry) {
    ArrayList<NumberContext> contextPairs = new ArrayList<NumberContext>(5);
    // With other small numbers.
    contextPairs.add(new NumberContext("It's only 9.99! Call ", " to buy"));
    // With a number Day.Month.Year date.
    contextPairs.add(new NumberContext("Call me on 21.6.1984 at ", ""));
    // With a number Month/Day date.
    contextPairs.add(new NumberContext("Call me on 06/21 at ", ""));
    // With a number Day.Month date
    contextPairs.add(new NumberContext("Call me on 21.6. at ", ""));
    // With a number Month/Day/Year date.
    contextPairs.add(new NumberContext("Call me on 06/21/84 at ", ""));
    doTestInContext(number, defaultCountry, contextPairs, Leniency.VALID);
  }

  private void doTestInContext(String number, String defaultCountry,
      List<NumberContext> contextPairs, Leniency leniency) {
    for (NumberContext context : contextPairs) {
      String prefix = context.leadingText;
      String text = prefix + number + context.trailingText;

      int start = prefix.length();
      int end = start + number.length();
      Iterable<PhoneNumberMatch> iterable =
          phoneUtil.findNumbers(text, defaultCountry, leniency, Long.MAX_VALUE);

      PhoneNumberMatch match = iterable.iterator().hasNext() ? iterable.iterator().next() : null;
      assertNotNull("Did not find a number in '" + text + "'; expected '" + number + "'", match);

      CharSequence extracted = text.subSequence(match.start(), match.end());
      assertTrue("Unexpected phone region in '" + text + "'; extracted '" + extracted + "'",
          start == match.start() && end == match.end());
      assertTrue(number.contentEquals(extracted));
      assertTrue(match.rawString().contentEquals(extracted));

      ensureTermination(text, defaultCountry, leniency);
    }
  }

  /**
   * Exhaustively searches for phone numbers from each index within {@code text} to test that
   * finding matches always terminates.
   */
  private void ensureTermination(String text, String defaultCountry, Leniency leniency) {
    for (int index = 0; index <= text.length(); index++) {
      String sub = text.substring(index);
      StringBuilder matches = new StringBuilder();
      // Iterates over all matches.
      for (PhoneNumberMatch match :
           phoneUtil.findNumbers(sub, defaultCountry, leniency, Long.MAX_VALUE)) {
        matches.append(", ").append(match.toString());
      }
    }
  }

  /**
   * Returns true if there were no matches found.
   */
  private boolean hasNoMatches(Iterable<PhoneNumberMatch> iterable) {
    return !iterable.iterator().hasNext();
  }

  /**
   * Small class that holds the context of the number we are testing against. The test will
   * insert the phone number to be found between leadingText and trailingText.
   */
  private class NumberContext {
    final String leadingText;
    final String trailingText;

    NumberContext(String leadingText, String trailingText) {
      this.leadingText = leadingText;
      this.trailingText = trailingText;
    }
  }
}
