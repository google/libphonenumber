// Copyright (C) 2025 The Libphonenumber Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <stddef.h>

#ifndef I18N_PHONENUMBERS_CONSTANTS_H_
#define I18N_PHONENUMBERS_CONSTANTS_H_

namespace i18n {
namespace phonenumbers {

class Constants {
  friend class PhoneNumberMatcherRegExps;
  friend class PhoneNumberRegExpsAndMappings;
  friend class PhoneNumberUtil;

 private:
  // The kPlusSign signifies the international prefix.
  static constexpr char kPlusSign[] = "+";

  static constexpr char kStarSign[] = "*";

  static constexpr char kRfc3966ExtnPrefix[] = ";ext=";
  static constexpr char kRfc3966VisualSeparator[] = "[\\-\\.\\(\\)]?";

  static constexpr char kDigits[] = "\\p{Nd}";

  // We accept alpha characters in phone numbers, ASCII only. We store
  // lower-case here only since our regular expressions are case-insensitive.
  static constexpr char kValidAlpha[] = "a-z";
  static constexpr char kValidAlphaInclUppercase[] = "A-Za-z";

  static constexpr char kPossibleSeparatorsBetweenNumberAndExtLabel[] =
      "[ \xC2\xA0\\t,]*";

  // Optional full stop (.) or colon, followed by zero or more
  // spaces/tabs/commas.
  static constexpr char kPossibleCharsAfterExtLabel[] =
      "[:\\.\xEF\xBC\x8E]?[ \xC2\xA0\\t,-]*";

  static constexpr char kOptionalExtSuffix[] = "#?";

  // The minimum and maximum length of the national significant number.
  static constexpr size_t kMinLengthForNsn = 2;

  static constexpr char kPlusChars[] = "+\xEF\xBC\x8B"; /* "+＋" */

  // Regular expression of acceptable punctuation found in phone numbers, used
  // to find numbers in text and to decide what is a viable phone number. This
  // excludes diallable characters.
  // This consists of dash characters, white space characters, full stops,
  // slashes, square brackets, parentheses and tildes. It also includes the
  // letter 'x' as that is found as a placeholder for carrier information in
  // some phone numbers. Full-width variants are also present. To find out the
  // unicode code-point of the characters below in vim, highlight the character
  // and type 'ga'. Note that the - is used to express ranges of full-width
  // punctuation below, as well as being present in the expression itself. In
  // emacs, you can use M-x unicode-what to query information about the unicode
  // character.
  static constexpr char kValidPunctuation[] =
      /* "-x‐-―−ー－-／  ­<U+200B><U+2060>　()（）［］.\\[\\]/~⁓∼" */
      "-x\xE2\x80\x90-\xE2\x80\x95\xE2\x88\x92\xE3\x83\xBC\xEF\xBC\x8D-\xEF\xBC"
      "\x8F \xC2\xA0\xC2\xAD\xE2\x80\x8B\xE2\x81\xA0\xE3\x80\x80()\xEF\xBC\x88"
      "\xEF\xBC\x89\xEF\xBC\xBB\xEF\xBC\xBD.\\[\\]/~\xE2\x81\x93\xE2\x88\xBC";

  static constexpr char kCaptureUpToSecondNumberStart[] = "(.*)[\\\\/] *x";
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_CONSTANTS_H_