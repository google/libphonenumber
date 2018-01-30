/*
 * Copyright (C) 2011 The Libphonenumber Authors
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

goog.require('goog.testing.jsunit');
goog.require('i18n.phonenumbers.PhoneNumber');
goog.require('i18n.phonenumbers.PhoneNumber.CountryCodeSource');
goog.require('i18n.phonenumbers.PhoneNumberMatcher');
goog.require('i18n.phonenumbers.PhoneNumberMatch');
goog.require('i18n.phonenumbers.PhoneNumberUtil');
goog.require('i18n.phonenumbers.RegionCode');

var phoneUtil = i18n.phonenumbers.PhoneNumberUtil.getInstance();
var PhoneNumber = i18n.phonenumbers.PhoneNumber;
var PhoneNumberMatch = i18n.phonenumbers.PhoneNumberMatch;
var PhoneNumberMatcher = i18n.phonenumbers.PhoneNumberMatcher;
var CountryCodeSource = i18n.phonenumbers.PhoneNumber.CountryCodeSource;
var RegionCode = i18n.phonenumbers.RegionCode;

/**
 * Asserts that the expected match is non-null, and that the raw string and expected
 * proto buffer are set appropriately.
 */
function assertMatchProperties(match, text, number, region) {
  var expectedResult = phoneUtil.parse(number, region);
  assertNotNull("Did not find a number in '" + text + "'; expected " + number, match);
  assertTrue(expectedResult.equals(match.number));
  assertEquals(number, match.rawString);
}



function testContainsMoreThanOneSlashInNationalNumber() {
    // A date should return true.
    var number = new PhoneNumber();
    number.setCountryCode(1);
    number.setCountryCodeSource(CountryCodeSource.FROM_DEFAULT_COUNTRY);
    var candidate = "1/05/2013";
    assertTrue(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate));

    // Here, the country code source thinks it started with a country calling code, but this is not
    // the same as the part before the slash, so it's still true.
    number = new PhoneNumber();
    number.setCountryCode(274);
    number.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN);
    candidate = "27/4/2013";
    assertTrue(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate));

    // Now it should be false, because the first slash is after the country calling code.
    number = new PhoneNumber();
    number.setCountryCode(49);
    number.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN);
    candidate = "49/69/2013";
    assertFalse(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate));

    number = new PhoneNumber();
    number.setCountryCode(49);
    number.setCountryCodeSource(CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN);
    candidate = "+49/69/2013";
    assertFalse(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate));

    candidate = "+ 49/69/2013";
    assertFalse(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate));

    candidate = "+ 49/69/20/13";
    assertTrue(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate));

    // Here, the first group is not assumed to be the country calling code, even though it is the
    // same as it, so this should return true.
    number = new PhoneNumber();
    number.setCountryCode(49);
    number.setCountryCodeSource(CountryCodeSource.FROM_DEFAULT_COUNTRY);
    candidate = "49/69/2013";
    assertTrue(PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber(number, candidate));
}

function testMatchesFoundWithMultipleSpaces() {
    var number1 = "(415) 666-7777";
    var number2 = "(800) 443-1223";
    var text = number1 + " " + number2;

    var iterator = phoneUtil.findNumbers(text, RegionCode.US);
    var match = iterator.hasNext() ? iterator.next() : null;
    assertMatchProperties(match, text, number1, RegionCode.US);

    match = iterator.hasNext() ? iterator.next() : null;
    assertMatchProperties(match, text, number2, RegionCode.US);
}

function testFourMatchesInARow() {
    var number1 = "415-666-7777";
    var number2 = "800-443-1223";
    var number3 = "212-443-1223";
    var number4 = "650-443-1223";
    var text = number1 + " - " + number2 + " - " + number3 + " - " + number4;

    var iterator = phoneUtil.findNumbers(text, RegionCode.US);
    var match = iterator.hasNext() ? iterator.next() : null;
    assertMatchProperties(match, text, number1, RegionCode.US);

    match = iterator.hasNext() ? iterator.next() : null;
    assertMatchProperties(match, text, number2, RegionCode.US);

    match = iterator.hasNext() ? iterator.next() : null;
    assertMatchProperties(match, text, number3, RegionCode.US);

    match = iterator.hasNext() ? iterator.next() : null;
    assertMatchProperties(match, text, number4, RegionCode.US);
}

function testMatchWithSurroundingZipcodes() {
    var number = "415-666-7777";
    var zipPreceding = "My address is CA 34215 - " + number + " is my number.";

    var iterator = phoneUtil.findNumbers(zipPreceding, RegionCode.US);
    var match = iterator.hasNext() ? iterator.next() : null;
    assertMatchProperties(match, zipPreceding, number, RegionCode.US);

    // Now repeat, but this time the phone number has spaces in it. It should still be found.
    number = "(415) 666 7777";

    var zipFollowing = "My number is " + number + ". 34215 is my zip-code.";
    iterator = phoneUtil.findNumbers(zipFollowing, RegionCode.US);
    var matchWithSpaces = iterator.hasNext() ? iterator.next() : null;
    assertMatchProperties(matchWithSpaces, zipFollowing, number, RegionCode.US);
}

function testIsLatinLetter() {
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
    assertFalse(PhoneNumberMatcher.isLatinLetter('\u306E'));  // Hiragana letter no
}

function testMatchesMultiplePhoneNumbersSeparatedByPhoneNumberPunctuation() {
    var text = "Call 650-253-4561 -- 455-234-3451";
    var region = RegionCode.US;

    var number1 = new PhoneNumber();
    number1.setCountryCode(phoneUtil.getCountryCodeForRegion(region));
    number1.setNationalNumber(6502534561); // was 6502534561L
    var match1 = new PhoneNumberMatch(5, "650-253-4561", number1);

    var number2 = new PhoneNumber();
    number2.setCountryCode(phoneUtil.getCountryCodeForRegion(region));
    number2.setNationalNumber(4552343451); // 4552343451L
    var match2 = new PhoneNumberMatch(21, "455-234-3451", number2);

    var matches = phoneUtil.findNumbers(text, region);
    assertTrue(match1.equals(matches.next()));
    assertTrue(match2.equals(matches.next()));
}


/**
 * Tests numbers found by {@link PhoneNumberUtil#findNumbers(CharSequence, String)} in various
 * textual contexts.
 *
 * @param number the number to test and the corresponding region code to use
 */
function doTestFindInContext(number, defaultCountry) {
    findPossibleInContext(number, defaultCountry);

    var parsed = phoneUtil.parse(number, defaultCountry);
    if (phoneUtil.isValidNumber(parsed)) {
        findValidInContext(number, defaultCountry);
    }
}

/**
 * Tests valid numbers in contexts that should pass for {@link Leniency#POSSIBLE}.
 */
function findPossibleInContext(number, defaultCountry) {
    var contextPairs = [];
    contextPairs.push(new NumberContext("", ""));  // no context
    contextPairs.push(new NumberContext("   ", "\t"));  // whitespace only
    contextPairs.push(new NumberContext("Hello ", ""));  // no context at end
    contextPairs.push(new NumberContext("", " to call me!"));  // no context at start
    contextPairs.push(new NumberContext("Hi there, call ", " to reach me!"));  // no context at start
    contextPairs.push(new NumberContext("Hi there, call ", ", or don't"));  // with commas
    // Three examples without whitespace around the number.
    contextPairs.push(new NumberContext("Hi call", ""));
    contextPairs.push(new NumberContext("", "forme"));
    contextPairs.push(new NumberContext("Hi call", "forme"));
    // With other small numbers.
    contextPairs.push(new NumberContext("It's cheap! Call ", " before 6:30"));
    // With a second number later.
    contextPairs.push(new NumberContext("Call ", " or +1800-123-4567!"));
    contextPairs.push(new NumberContext("Call me on June 2 at", ""));  // with a Month-Day date
    // With publication pages.
    contextPairs.push(new NumberContext(
        "As quoted by Alfonso 12-15 (2009), you may call me at ", ""));
    contextPairs.push(new NumberContext(
        "As quoted by Alfonso et al. 12-15 (2009), you may call me at ", ""));
    // With dates, written in the American style.
    contextPairs.push(new NumberContext(
        "As I said on 03/10/2011, you may call me at ", ""));
    // With trailing numbers after a comma. The 45 should not be considered an extension.
    contextPairs.push(new NumberContext("", ", 45 days a year"));
    // When matching we don't consider semicolon along with legitimate extension symbol to indicate
    // an extension. The 7246433 should not be considered an extension.
    contextPairs.push(new NumberContext("", ";x 7246433"));
        // With a postfix stripped off as it looks like the start of another number.
    contextPairs.push(new NumberContext("Call ", "/x12 more"));

    doTestInContext(number, defaultCountry, contextPairs, Leniency.POSSIBLE);
}

function doTestInContext(number, defaultCountry,contextPairs, leniency) {
    contextPairs.forEach(function(context) {
        var prefix = context.leadingText;
        var text = prefix + number + context.trailingText;
    
        var start = prefix.length;
        var end = start + number.length;
        var iterator =
            phoneUtil.findNumbers(text, defaultCountry, leniency, Long.MAX_VALUE).iterator();
    
        var match = iterator.hasNext() ? iterator.next() : null;
        assertNotNull("Did not find a number in '" + text + "'; expected '" + number + "'", match);
    
        var extracted = text.substrig(match.start, match.end);
        assertTrue("Unexpected phone region in '" + text + "'; extracted '" + extracted + "'",
            start == match.start() && end == match.end());
        assertTrue(number.equals(extracted)); // XXX: need to figure out equals vs. contentEquals
        assertEquals(match.rawString, extracted); // XXX: need to figure out equals vs. contentEquals
    
        ensureTermination(text, defaultCountry, leniency);    
    });
}

/**
 * Tests valid numbers in contexts that fail for {@link Leniency#POSSIBLE} but are valid for
 * {@link Leniency#VALID}.
 */
function findValidInContext(number, defaultCountry) {
    var contextPairs = [];
    // With other small numbers.
    contextPairs.push(new NumberContext("It's only 9.99! Call ", " to buy"));
    // With a number Day.Month.Year date.
    contextPairs.push(new NumberContext("Call me on 21.6.1984 at ", ""));
    // With a number Month/Day date.
    contextPairs.push(new NumberContext("Call me on 06/21 at ", ""));
    // With a number Day.Month date.
    contextPairs.push(new NumberContext("Call me on 21.6. at ", ""));
    // With a number Month/Day/Year date.
    contextPairs.push(new NumberContext("Call me on 06/21/84 at ", ""));

    doTestInContext(number, defaultCountry, contextPairs, Leniency.VALID);
}


/**
 * Small class that holds the context of the number we are testing against. The test will
 * insert the phone number to be found between leadingText and trailingText.
 */
function NumberContext(leadingText, trailingText) {
    this.leadingText = leadingText;
    this.trailingText = trailingText;
}

/**
 * Small class that holds the number we want to test and the region for which it should be valid.
 */
function NumberTest (rawString, region) {
    this.rawString = rawString;
    this.region = regionCode;
}
NumberTest.prototype.toString = function() {
    return this.rawString + " (" + this.region.toString() + ")";
};
