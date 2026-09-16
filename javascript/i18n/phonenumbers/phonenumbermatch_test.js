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
goog.require('i18n.phonenumbers.PhoneNumberMatch');
goog.require('i18n.phonenumbers.PhoneNumberUtil');

var phoneUtil = i18n.phonenumbers.PhoneNumberUtil.getInstance();
var PhoneNumber = i18n.phonenumbers.PhoneNumber;
var PhoneNumberMatch = i18n.phonenumbers.PhoneNumberMatch

/**
 * Tests the value type semantics. Equality and hash code must be based on the covered range and
 * corresponding phone number. Range and number correctness are tested by
 * {@link PhoneNumberMatcherTest}.
 */
function testPhoneNumberMatchValueTypeSemantics() {
    var number = new PhoneNumber();
    var match1 = new PhoneNumberMatch(10, "1 800 234 45 67", number);
    var match2 = new PhoneNumberMatch(10, "1 800 234 45 67", number);

    assertEquals(match1.start, match2.start);
    assertEquals(match1.end, match2.end);
    assertEquals(match1.number, match2.number);
    assertEquals(match1.rawString, match2.rawString);
    assertEquals("1 800 234 45 67", match1.rawString);
}

/**
 * Tests the value type semantics for matches with a null number.
 */
function testPhoneNumberMatchIllegalArguments() {
    var number;

    try {
        number = new PhoneNumberMatch(-110, "1 800 234 45 67", new PhoneNumber());
        fail();
    } catch (e) { /* success */ }

    try {
        number = new PhoneNumberMatch(10, "1 800 234 45 67", null);
        fail();
    } catch (e) { /* success */ }

    try {
        number = new PhoneNumberMatch(10, null, new PhoneNumber());
        fail();
    } catch (e) { /* success */ }

    try {
        number = new PhoneNumberMatch(10, null, null);
        fail();
    } catch (e) { /* success */ }
}
