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

goog.provide('i18n.phonenumbers.PhoneNumberMatch');

/**
 * The immutable match of a phone number within a piece of text. Matches may be found using
 * {@link PhoneNumberUtil#findNumbers}.
 *
 * <p>A match consists of the {@linkplain #number() phone number} as well as the
 * {@linkplain #start() start} and {@linkplain #end() end} offsets of the corresponding subsequence
 * of the searched text. Use {@link #rawString()} to obtain a copy of the matched subsequence.
 *
 * <p>The following annotated example clarifies the relationship between the searched text, the
 * match offsets, and the parsed number:

 * <pre>
 * CharSequence text = "Call me at +1 425 882-8080 for details.";
 * String country = "US";
 * PhoneNumberUtil util = PhoneNumberUtil.getInstance();
 *
 * // Find the first phone number match:
 * PhoneNumberMatch m = util.findNumbers(text, country).iterator().next();
 *
 * // rawString() contains the phone number as it appears in the text.
 * "+1 425 882-8080".equals(m.rawString());
 *
 * // start() and end() define the range of the matched subsequence.
 * CharSequence subsequence = text.subSequence(m.start(), m.end());
 * "+1 425 882-8080".contentEquals(subsequence);
 *
 * // number() returns the the same result as PhoneNumberUtil.{@link PhoneNumberUtil#parse parse()}
 * // invoked on rawString().
 * util.parse(m.rawString(), country).equals(m.number());
 * </pre>
 */
i18n.phonenumbers.PhoneNumberMatch = function(start, rawString, number) {
  if (start < 0) {
    throw new Error('Start index must be >= 0.');
  }
  if (rawString == null) {
    throw new Error('rawString must not be null');
  }
  if (number == null) {
    throw new Error('number must not be null');
  }
  
  /** The start index into the text. */
  this.start = start;
  /** The raw substring matched. */
  this.rawString = rawString;
  /** The matched phone number. */
  this.number = number;

  /** The exclusive end index of the matched phone number within the searched text. */
  this.end = start + rawString.length;
};

i18n.phonenumbers.PhoneNumberMatch.prototype.toString = function() {
  return 'PhoneNumberMatch [' + this.start + ',' + this.end + ') ' + this.rawString;
};

i18n.phonenumbers.PhoneNumberMatch.prototype.equals = function(obj) {
  if(this === obj) {
    return true;
  }
  if(!(obj instanceof i18n.phonenumbers.PhoneNumberMatch)) {
    return false;
  }
  return this.rawString == obj.rawString &&
         this.start == obj.start         &&
         this.number.equals(obj.number);
};
