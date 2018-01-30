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

console.log('phoneUtil', phoneUtil);
console.log('PhoneNumberMatcher', PhoneNumberMatcher);

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
