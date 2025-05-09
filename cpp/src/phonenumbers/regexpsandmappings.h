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

#include <bits/stl_pair.h>

#include <map>
#include <set>
#include <string>
#include <vector>

#include "phonenumbers/regexp_cache.h"
#include "phonenumbers/regexp_factory.h"
#include "phonenumbers/stringutil.h"

#ifndef I18N_PHONENUMBERS_REGEXPSANDMAPPINGS_H_
#define I18N_PHONENUMBERS_REGEXPSANDMAPPINGS_H_

namespace i18n {
namespace phonenumbers {

class PhoneNumberRegExpsAndMappings {
  friend class PhoneNumberUtil;

 private:
  void InitializeMapsAndSets();

  // Helper initialiser method to create the regular-expression pattern to match
  // extensions. Note that:
  // - There are currently six capturing groups for the extension itself. If this
  // number is changed, MaybeStripExtension needs to be updated.
  // - The only capturing groups should be around the digits that you want to
  // capture as part of the extension, or else parsing will fail!
  static std::string CreateExtnPattern(bool for_parsing);

  // Helper method for constructing regular expressions for parsing. Creates an
  // expression that captures up to max_length digits.
  static std::string ExtnDigits(int max_length);

  // Regular expression of viable phone numbers. This is location independent.
  // Checks we have at least three leading digits, and only valid punctuation,
  // alpha characters and digits in the phone number. Does not include extension
  // data. The symbol 'x' is allowed here as valid punctuation since it is often
  // used as a placeholder for carrier codes, for example in Brazilian phone
  // numbers. We also allow multiple plus-signs at the start.
  // Corresponds to the following:
  // [digits]{minLengthNsn}|
  // plus_sign*(([punctuation]|[star])*[digits]){3,}
  // ([punctuation]|[star]|[digits]|[alpha])*
  //
  // The first reg-ex is to allow short numbers (two digits long) to be parsed
  // if they are entered as "15" etc, but only if there is no punctuation in
  // them. The second expression restricts the number of digits to three or
  // more, but then allows them to be in international form, and to have
  // alpha-characters and punctuation.
  const string valid_phone_number_;

  // Regexp of all possible ways to write extensions, for use when parsing. This
  // will be run as a case-insensitive regexp match. Wide character versions are
  // also provided after each ASCII version.
  // For parsing, we are slightly more lenient in our interpretation than for
  // matching. Here we allow "comma" and "semicolon" as possible extension
  // indicators. When matching, these are hardly ever used to indicate this.
  const string extn_patterns_for_parsing_;

  // Regular expressions of different parts of the phone-context parameter,
  // following the syntax defined in RFC3966.
  const std::string rfc3966_phone_digit_;
  const std::string alphanum_;
  const std::string rfc3966_domainlabel_;
  const std::string rfc3966_toplabel_;

  scoped_ptr<const AbstractRegExpFactory> regexp_factory_;
  scoped_ptr<RegExpCache> regexp_cache_;

  // A map that contains characters that are essential when dialling. That means
  // any of the characters in this map must not be removed from a number when
  // dialing, otherwise the call will not reach the intended destination.
  std::map<char32, char> diallable_char_mappings_;
  // These mappings map a character (key) to a specific digit that should
  // replace it for normalization purposes.
  std::map<char32, char> alpha_mappings_;
  // For performance reasons, store a map of combining alpha_mappings with ASCII
  // digits.
  std::map<char32, char> alpha_phone_mappings_;

  // Separate map of all symbols that we wish to retain when formatting alpha
  // numbers. This includes digits, ascii letters and number grouping symbols
  // such as "-" and " ".
  std::map<char32, char> all_plus_number_grouping_symbols_;

  // Map of country calling codes that use a mobile token before the area code.
  // One example of when this is relevant is when determining the length of the
  // national destination code, which should be the length of the area code plus
  // the length of the mobile token.
  std::map<int, char> mobile_token_mappings_;

  // Set of country codes that doesn't have national prefix, but it has area
  // codes.
  std::set<int> countries_without_national_prefix_with_area_codes_;

  // Set of country codes that have geographically assigned mobile numbers (see
  // geo_mobile_countries_ below) which are not based on *area codes*. For
  // example, in China mobile numbers start with a carrier indicator, and beyond
  // that are geographically assigned: this carrier indicator is not considered
  // to be an area code.
  std::set<int> geo_mobile_countries_without_mobile_area_codes_;

  // Set of country calling codes that have geographically assigned mobile
  // numbers. This may not be complete; we add calling codes case by case, as we
  // find geographical mobile numbers or hear from user reports.
  std::set<int> geo_mobile_countries_;

  // Pattern that makes it easy to distinguish whether a region has a single
  // international dialing prefix or not. If a region has a single international
  // prefix (e.g. 011 in USA), it will be represented as a string that contains
  // a sequence of ASCII digits, and possibly a tilde, which signals waiting for
  // the tone. If there are multiple available international prefixes in a
  // region, they will be represented as a regex string that always contains one
  // or more characters that are not ASCII digits or a tilde.
  scoped_ptr<const RegExp> single_international_prefix_;

  scoped_ptr<const RegExp> digits_pattern_;
  scoped_ptr<const RegExp> capturing_digit_pattern_;
  scoped_ptr<const RegExp> capturing_ascii_digits_pattern_;

  // Regular expression of acceptable characters that may start a phone number
  // for the purposes of parsing. This allows us to strip away meaningless
  // prefixes to phone numbers that may be mistakenly given to us. This consists
  // of digits, the plus symbol and arabic-indic digits. This does not contain
  // alpha characters, although they may be used later in the number. It also
  // does not include other punctuation, as this will be stripped later during
  // parsing and is of no information value when parsing a number. The string
  // starting with this valid character is captured.
  // This corresponds to VALID_START_CHAR in the java version.
  scoped_ptr<const RegExp> valid_start_char_pattern_;

  // Regular expression of valid characters before a marker that might indicate
  // a second number.
  scoped_ptr<const RegExp> capture_up_to_second_number_start_pattern_;

  // Regular expression of trailing characters that we want to remove. We remove
  // all characters that are not alpha or numerical characters. The hash
  // character is retained here, as it may signify the previous block was an
  // extension. Note the capturing block at the start to capture the rest of the
  // number if this was a match.
  // This corresponds to UNWANTED_END_CHAR_PATTERN in the java version.
  scoped_ptr<const RegExp> unwanted_end_char_pattern_;

  // Regular expression of groups of valid punctuation characters.
  scoped_ptr<const RegExp> separator_pattern_;

  // Regexp of all possible ways to write extensions, for use when finding phone
  // numbers in text. This will be run as a case-insensitive regexp match. Wide
  // character versions are also provided after each ASCII version.
  const string extn_patterns_for_matching_;

  // Regexp of all known extension prefixes used by different regions followed
  // by 1 or more valid digits, for use when parsing.
  scoped_ptr<const RegExp> extn_pattern_;

  // We append optionally the extension pattern to the end here, as a valid
  // phone number may have an extension prefix appended, followed by 1 or more
  // digits.
  scoped_ptr<const RegExp> valid_phone_number_pattern_;

  // We use this pattern to check if the phone number has at least three letters
  // in it - if so, then we treat it as a number where some phone-number digits
  // are represented by letters.
  scoped_ptr<const RegExp> valid_alpha_phone_pattern_;

  scoped_ptr<const RegExp> first_group_capturing_pattern_;

  scoped_ptr<const RegExp> carrier_code_pattern_;

  scoped_ptr<const RegExp> plus_chars_pattern_;

  // Regular expression of valid global-number-digits for the phone-context
  // parameter, following the syntax defined in RFC3966.
  std::unique_ptr<const RegExp> rfc3966_global_number_digits_pattern_;

  // Regular expression of valid domainname for the phone-context parameter,
  // following the syntax defined in RFC3966.
  std::unique_ptr<const RegExp> rfc3966_domainname_pattern_;

  PhoneNumberRegExpsAndMappings();

  // This type is neither copyable nor movable.
  PhoneNumberRegExpsAndMappings(const PhoneNumberRegExpsAndMappings&) = delete;
  PhoneNumberRegExpsAndMappings& operator=(
      const PhoneNumberRegExpsAndMappings&) = delete;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_REGEXPSANDMAPPINGS_H_
