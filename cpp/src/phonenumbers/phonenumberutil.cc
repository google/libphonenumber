// Copyright (C) 2009 The Libphonenumber Authors
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

// Author: Shaopeng Jia
// Open-sourced by: Philippe Liard

#include "phonenumbers/phonenumberutil.h"

#include <algorithm>
#include <cctype>
#include <cstddef>
#include <fstream>
#include <iostream>
#include <iterator>
#include <map>
#include <sstream>
#include <utility>
#include <vector>

#include <google/protobuf/message_lite.h>
#include <unicode/uchar.h>
#include <unicode/utf8.h>

#include "base/basictypes.h"
#include "base/logging.h"
#include "base/memory/singleton.h"
#include "phonenumbers/asyoutypeformatter.h"
#include "phonenumbers/default_logger.h"
#include "phonenumbers/encoding_utils.h"
#include "phonenumbers/metadata.h"
#include "phonenumbers/normalize_utf8.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/regexp_adapter.h"
#include "phonenumbers/regexp_cache.h"
#include "phonenumbers/regexp_factory.h"
#include "phonenumbers/region_code.h"
#include "phonenumbers/stl_util.h"
#include "phonenumbers/stringutil.h"
#include "phonenumbers/utf/unicodetext.h"
#include "phonenumbers/utf/utf.h"

namespace i18n {
namespace phonenumbers {

using std::cerr;
using std::endl;
using std::ifstream;
using std::make_pair;
using std::sort;
using std::stringstream;

using google::protobuf::RepeatedPtrField;

// static
const char PhoneNumberUtil::kPlusChars[] = "+\xEF\xBC\x8B";  /* "+＋" */
// To find out the unicode code-point of the characters below in vim, highlight
// the character and type 'ga'. Note that the - is used to express ranges of
// full-width punctuation below, as well as being present in the expression
// itself. In emacs, you can use M-x unicode-what to query information about the
// unicode character.
// static
const char PhoneNumberUtil::kValidPunctuation[] =
    /* "-x‐-―−ー－-／  <U+200B><U+2060>　()（）［］.\\[\\]/~⁓∼" */
    "-x\xE2\x80\x90-\xE2\x80\x95\xE2\x88\x92\xE3\x83\xBC\xEF\xBC\x8D-\xEF\xBC"
    "\x8F \xC2\xA0\xE2\x80\x8B\xE2\x81\xA0\xE3\x80\x80()\xEF\xBC\x88\xEF\xBC"
    "\x89\xEF\xBC\xBB\xEF\xBC\xBD.\\[\\]/~\xE2\x81\x93\xE2\x88\xBC";

// static
const char PhoneNumberUtil::kCaptureUpToSecondNumberStart[] = "(.*)[\\\\/] *x";

// static
const char PhoneNumberUtil::kRegionCodeForNonGeoEntity[] = "001";

namespace {

// The prefix that needs to be inserted in front of a Colombian landline
// number when dialed from a mobile phone in Colombia.
const char kColombiaMobileToFixedLinePrefix[] = "3";

// The kPlusSign signifies the international prefix.
const char kPlusSign[] = "+";

const char kStarSign[] = "*";

const char kRfc3966ExtnPrefix[] = ";ext=";
const char kRfc3966Prefix[] = "tel:";
// We include the "+" here since RFC3966 format specifies that the context must
// be specified in international format.
const char kRfc3966PhoneContext[] = ";phone-context=+";

const char kDigits[] = "\\p{Nd}";
// We accept alpha characters in phone numbers, ASCII only. We store lower-case
// here only since our regular expressions are case-insensitive.
const char kValidAlpha[] = "a-z";

// Default extension prefix to use when formatting. This will be put in front of
// any extension component of the number, after the main national number is
// formatted. For example, if you wish the default extension formatting to be "
// extn: 3456", then you should specify " extn: " here as the default extension
// prefix. This can be overridden by region-specific preferences.
const char kDefaultExtnPrefix[] = " ext. ";

// One-character symbols that can be used to indicate an extension.
const char kSingleExtnSymbolsForMatching[] =
    "x\xEF\xBD\x98#\xEF\xBC\x83~\xEF\xBD\x9E";

bool LoadCompiledInMetadata(PhoneMetadataCollection* metadata) {
  if (!metadata->ParseFromArray(metadata_get(), metadata_size())) {
    cerr << "Could not parse binary data." << endl;
    return false;
  }
  return true;
}

// Returns a pointer to the description inside the metadata of the appropriate
// type.
const PhoneNumberDesc* GetNumberDescByType(
    const PhoneMetadata& metadata,
    PhoneNumberUtil::PhoneNumberType type) {
  switch (type) {
    case PhoneNumberUtil::PREMIUM_RATE:
      return &metadata.premium_rate();
    case PhoneNumberUtil::TOLL_FREE:
      return &metadata.toll_free();
    case PhoneNumberUtil::MOBILE:
      return &metadata.mobile();
    case PhoneNumberUtil::FIXED_LINE:
    case PhoneNumberUtil::FIXED_LINE_OR_MOBILE:
      return &metadata.fixed_line();
    case PhoneNumberUtil::SHARED_COST:
      return &metadata.shared_cost();
    case PhoneNumberUtil::VOIP:
      return &metadata.voip();
    case PhoneNumberUtil::PERSONAL_NUMBER:
      return &metadata.personal_number();
    case PhoneNumberUtil::PAGER:
      return &metadata.pager();
    case PhoneNumberUtil::UAN:
      return &metadata.uan();
    case PhoneNumberUtil::VOICEMAIL:
      return &metadata.voicemail();
    default:
      return &metadata.general_desc();
  }
}

// A helper function that is used by Format and FormatByPattern.
void PrefixNumberWithCountryCallingCode(
    int country_calling_code,
    PhoneNumberUtil::PhoneNumberFormat number_format,
    string* formatted_number) {
  switch (number_format) {
    case PhoneNumberUtil::E164:
      formatted_number->insert(
          0, StrCat(kPlusSign, SimpleItoa(country_calling_code)));
      return;
    case PhoneNumberUtil::INTERNATIONAL:
      formatted_number->insert(
          0, StrCat(kPlusSign, SimpleItoa(country_calling_code), " "));
      return;
    case PhoneNumberUtil::RFC3966:
      formatted_number->insert(
          0, StrCat(kRfc3966Prefix, kPlusSign,
                    SimpleItoa(country_calling_code), "-"));
      return;
    case PhoneNumberUtil::NATIONAL:
    default:
      // Do nothing.
      return;
  }
}

// Returns true when one national number is the suffix of the other or both are
// the same.
bool IsNationalNumberSuffixOfTheOther(const PhoneNumber& first_number,
                                      const PhoneNumber& second_number) {
  const string& first_number_national_number =
    SimpleItoa(static_cast<uint64>(first_number.national_number()));
  const string& second_number_national_number =
    SimpleItoa(static_cast<uint64>(second_number.national_number()));
  // Note that HasSuffixString returns true if the numbers are equal.
  return HasSuffixString(first_number_national_number,
                         second_number_national_number) ||
         HasSuffixString(second_number_national_number,
                         first_number_national_number);
}

bool IsNumberMatchingDesc(const string& national_number,
                          const PhoneNumberDesc& number_desc,
                          RegExpCache* regexp_cache) {
  return regexp_cache->GetRegExp(number_desc.possible_number_pattern())
             .FullMatch(national_number) &&
         regexp_cache->GetRegExp(number_desc.national_number_pattern())
             .FullMatch(national_number);
}

PhoneNumberUtil::PhoneNumberType GetNumberTypeHelper(
    const string& national_number, const PhoneMetadata& metadata,
    RegExpCache* regexp_cache) {
  const PhoneNumberDesc& general_desc = metadata.general_desc();
  if (!general_desc.has_national_number_pattern() ||
      !IsNumberMatchingDesc(national_number, general_desc, regexp_cache)) {
    VLOG(4) << "Number type unknown - doesn't match general national number"
            << " pattern.";
    return PhoneNumberUtil::UNKNOWN;
  }
  if (IsNumberMatchingDesc(national_number, metadata.premium_rate(),
                           regexp_cache)) {
    VLOG(4) << "Number is a premium number.";
    return PhoneNumberUtil::PREMIUM_RATE;
  }
  if (IsNumberMatchingDesc(national_number, metadata.toll_free(),
                           regexp_cache)) {
    VLOG(4) << "Number is a toll-free number.";
    return PhoneNumberUtil::TOLL_FREE;
  }
  if (IsNumberMatchingDesc(national_number, metadata.shared_cost(),
                           regexp_cache)) {
    VLOG(4) << "Number is a shared cost number.";
    return PhoneNumberUtil::SHARED_COST;
  }
  if (IsNumberMatchingDesc(national_number, metadata.voip(), regexp_cache)) {
    VLOG(4) << "Number is a VOIP (Voice over IP) number.";
    return PhoneNumberUtil::VOIP;
  }
  if (IsNumberMatchingDesc(national_number, metadata.personal_number(),
                           regexp_cache)) {
    VLOG(4) << "Number is a personal number.";
    return PhoneNumberUtil::PERSONAL_NUMBER;
  }
  if (IsNumberMatchingDesc(national_number, metadata.pager(), regexp_cache)) {
    VLOG(4) << "Number is a pager number.";
    return PhoneNumberUtil::PAGER;
  }
  if (IsNumberMatchingDesc(national_number, metadata.uan(), regexp_cache)) {
    VLOG(4) << "Number is a UAN.";
    return PhoneNumberUtil::UAN;
  }
  if (IsNumberMatchingDesc(national_number, metadata.voicemail(),
                           regexp_cache)) {
    VLOG(4) << "Number is a voicemail number.";
    return PhoneNumberUtil::VOICEMAIL;
  }

  bool is_fixed_line =
      IsNumberMatchingDesc(national_number, metadata.fixed_line(),
                           regexp_cache);
  if (is_fixed_line) {
    if (metadata.same_mobile_and_fixed_line_pattern()) {
      VLOG(4) << "Fixed-line and mobile patterns equal, number is fixed-line"
              << " or mobile";
      return PhoneNumberUtil::FIXED_LINE_OR_MOBILE;
    } else if (IsNumberMatchingDesc(national_number, metadata.mobile(),
                                    regexp_cache)) {
      VLOG(4) << "Fixed-line and mobile patterns differ, but number is "
              << "still fixed-line or mobile";
      return PhoneNumberUtil::FIXED_LINE_OR_MOBILE;
    }
    VLOG(4) << "Number is a fixed line number.";
    return PhoneNumberUtil::FIXED_LINE;
  }
  // Otherwise, test to see if the number is mobile. Only do this if certain
  // that the patterns for mobile and fixed line aren't the same.
  if (!metadata.same_mobile_and_fixed_line_pattern() &&
      IsNumberMatchingDesc(national_number, metadata.mobile(), regexp_cache)) {
    VLOG(4) << "Number is a mobile number.";
    return PhoneNumberUtil::MOBILE;
  }
  VLOG(4) << "Number type unknown - doesn\'t match any specific number type"
          << " pattern.";
  return PhoneNumberUtil::UNKNOWN;
}

char32 ToUnicodeCodepoint(const char* unicode_char) {
  char32 codepoint;
  EncodingUtils::DecodeUTF8Char(unicode_char, &codepoint);
  return codepoint;
}

// Helper initialiser method to create the regular-expression pattern to match
// extensions, allowing the one-codepoint extension symbols provided by
// single_extn_symbols.
// Note that there are currently three capturing groups for the extension itself
// - if this number is changed, MaybeStripExtension needs to be updated.
string CreateExtnPattern(const string& single_extn_symbols) {
  static const string capturing_extn_digits = StrCat("([", kDigits, "]{1,7})");
  // The first regular expression covers RFC 3966 format, where the extension is
  // added using ";ext=". The second more generic one starts with optional white
  // space and ends with an optional full stop (.), followed by zero or more
  // spaces/tabs and then the numbers themselves. The third one covers the
  // special case of American numbers where the extension is written with a hash
  // at the end, such as "- 503#".
  // Note that the only capturing groups should be around the digits that you
  // want to capture as part of the extension, or else parsing will fail!
  // Canonical-equivalence doesn't seem to be an option with RE2, so we allow
  // two options for representing the ó - the character itself, and one in the
  // unicode decomposed form with the combining acute accent.
  return (StrCat(
      kRfc3966ExtnPrefix, capturing_extn_digits, "|"
       /* "[  \\t,]*(?:e?xt(?:ensi(?:ó?|ó))?n?|ｅ?ｘｔｎ?|single_extn_symbols|"
          "int|ｉｎｔ|anexo)"
          "[:\\.．]?[  \\t,-]*", capturing_extn_digits, "#?|" */
      "[ \xC2\xA0\\t,]*(?:e?xt(?:ensi(?:o\xCC\x81?|\xC3\xB3))?n?|"
      "(?:\xEF\xBD\x85)?\xEF\xBD\x98\xEF\xBD\x94(?:\xEF\xBD\x8E)?|"
      "[", single_extn_symbols, "]|int|"
      "\xEF\xBD\x89\xEF\xBD\x8E\xEF\xBD\x94|anexo)"
      "[:\\.\xEF\xBC\x8E]?[ \xC2\xA0\\t,-]*", capturing_extn_digits,
      "#?|[- ]+([", kDigits, "]{1,5})#"));
}

// Normalizes a string of characters representing a phone number by replacing
// all characters found in the accompanying map with the values therein, and
// stripping all other characters if remove_non_matches is true.
// Parameters:
// number - a pointer to a string of characters representing a phone number to
//   be normalized.
// normalization_replacements - a mapping of characters to what they should be
//   replaced by in the normalized version of the phone number
// remove_non_matches - indicates whether characters that are not able to be
//   replaced should be stripped from the number. If this is false, they will be
//   left unchanged in the number.
void NormalizeHelper(const map<char32, char>& normalization_replacements,
                     bool remove_non_matches,
                     string* number) {
  DCHECK(number);
  UnicodeText number_as_unicode;
  number_as_unicode.PointToUTF8(number->data(), number->size());
  string normalized_number;
  char unicode_char[5];
  for (UnicodeText::const_iterator it = number_as_unicode.begin();
       it != number_as_unicode.end();
       ++it) {
    map<char32, char>::const_iterator found_glyph_pair =
        normalization_replacements.find(*it);
    if (found_glyph_pair != normalization_replacements.end()) {
      normalized_number.push_back(found_glyph_pair->second);
    } else if (!remove_non_matches) {
      // Find out how long this unicode char is so we can append it all.
      int char_len = it.get_utf8(unicode_char);
      normalized_number.append(unicode_char, char_len);
    }
    // If neither of the above are true, we remove this character.
  }
  number->assign(normalized_number);
}

PhoneNumberUtil::ValidationResult TestNumberLengthAgainstPattern(
    const RegExp& number_pattern, const string& number) {
  string extracted_number;
  if (number_pattern.FullMatch(number, &extracted_number)) {
    return PhoneNumberUtil::IS_POSSIBLE;
  }
  if (number_pattern.PartialMatch(number, &extracted_number)) {
    return PhoneNumberUtil::TOO_LONG;
  } else {
    return PhoneNumberUtil::TOO_SHORT;
  }
}

}  // namespace

void PhoneNumberUtil::SetLogger(Logger* logger) {
  Logger::set_logger_impl(logger);
}

class PhoneNumberRegExpsAndMappings {
 private:
  void InitializeMapsAndSets() {
    diallable_char_mappings_.insert(make_pair('+', '+'));
    diallable_char_mappings_.insert(make_pair('*', '*'));
    // Here we insert all punctuation symbols that we wish to respect when
    // formatting alpha numbers, as they show the intended number groupings.
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("-"), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xEF\xBC\x8D" /* "－" */), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE2\x80\x90" /* "‐" */), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE2\x80\x91" /* "‑" */), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE2\x80\x92" /* "‒" */), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE2\x80\x93" /* "–" */), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE2\x80\x94" /* "—" */), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE2\x80\x95" /* "―" */), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE2\x88\x92" /* "−" */), '-'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("/"), '/'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xEF\xBC\x8F" /* "／" */), '/'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint(" "), ' '));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE3\x80\x80" /* "　" */), ' '));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xE2\x81\xA0"), ' '));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("."), '.'));
    all_plus_number_grouping_symbols_.insert(
        make_pair(ToUnicodeCodepoint("\xEF\xBC\x8E" /* "．" */), '.'));
    // Only the upper-case letters are added here - the lower-case versions are
    // added programmatically.
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("A"), '2'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("B"), '2'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("C"), '2'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("D"), '3'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("E"), '3'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("F"), '3'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("G"), '4'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("H"), '4'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("I"), '4'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("J"), '5'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("K"), '5'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("L"), '5'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("M"), '6'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("N"), '6'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("O"), '6'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("P"), '7'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("Q"), '7'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("R"), '7'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("S"), '7'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("T"), '8'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("U"), '8'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("V"), '8'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("W"), '9'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("X"), '9'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("Y"), '9'));
    alpha_mappings_.insert(make_pair(ToUnicodeCodepoint("Z"), '9'));
    map<char32, char> lower_case_mappings;
    map<char32, char> alpha_letters;
    for (map<char32, char>::const_iterator it = alpha_mappings_.begin();
         it != alpha_mappings_.end();
         ++it) {
      // Convert all the upper-case ASCII letters to lower-case.
      if (it->first < 128) {
        char letter_as_upper = static_cast<char>(it->first);
        char32 letter_as_lower = static_cast<char32>(tolower(letter_as_upper));
        lower_case_mappings.insert(make_pair(letter_as_lower, it->second));
        // Add the letters in both variants to the alpha_letters map. This just
        // pairs each letter with its upper-case representation so that it can
        // be retained when normalising alpha numbers.
        alpha_letters.insert(make_pair(letter_as_lower, letter_as_upper));
        alpha_letters.insert(make_pair(it->first, letter_as_upper));
      }
    }
    // In the Java version we don't insert the lower-case mappings in the map,
    // because we convert to upper case on the fly. Doing this here would
    // involve pulling in all of ICU, which we don't want to do if we don't have
    // to.
    alpha_mappings_.insert(lower_case_mappings.begin(),
                           lower_case_mappings.end());
    alpha_phone_mappings_.insert(alpha_mappings_.begin(),
                                 alpha_mappings_.end());
    all_plus_number_grouping_symbols_.insert(alpha_letters.begin(),
                                             alpha_letters.end());
    // Add the ASCII digits so that they don't get deleted by NormalizeHelper().
    for (char c = '0'; c <= '9'; ++c) {
      diallable_char_mappings_.insert(make_pair(c, c));
      alpha_phone_mappings_.insert(make_pair(c, c));
      all_plus_number_grouping_symbols_.insert(make_pair(c, c));
    }
  }

  const string punctuation_and_star_sign_;

  // Regular expression of viable phone numbers. This is location independent.
  // Checks we have at least three leading digits, and only valid punctuation,
  // alpha characters and digits in the phone number. Does not include extension
  // data. The symbol 'x' is allowed here as valid punctuation since it is often
  // used as a placeholder for carrier codes, for example in Brazilian phone
  // numbers. We also allow multiple plus-signs at the start.
  // Corresponds to the following:
  // plus_sign*(([punctuation]|[star])*[digits]){3,}
  // ([punctuation]|[star]|[digits]|[alpha])*
  const string valid_phone_number_;

  // Regexp of all possible ways to write extensions, for use when parsing. This
  // will be run as a case-insensitive regexp match. Wide character versions are
  // also provided after each ASCII version.
  // For parsing, we are slightly more lenient in our interpretation than for
  // matching. Here we allow a "comma" as a possible extension indicator. When
  // matching, this is hardly ever used to indicate this.
  const string extn_patterns_for_parsing_;

 public:
  scoped_ptr<const AbstractRegExpFactory> regexp_factory_;
  scoped_ptr<RegExpCache> regexp_cache_;

  // A map that contains characters that are essential when dialling. That means
  // any of the characters in this map must not be removed from a number when
  // dialing, otherwise the call will not reach the intended destination.
  map<char32, char> diallable_char_mappings_;
  // These mappings map a character (key) to a specific digit that should
  // replace it for normalization purposes.
  map<char32, char> alpha_mappings_;
  // For performance reasons, store a map of combining alpha_mappings with ASCII
  // digits.
  map<char32, char> alpha_phone_mappings_;

  // Separate map of all symbols that we wish to retain when formatting alpha
  // numbers. This includes digits, ascii letters and number grouping symbols
  // such as "-" and " ".
  map<char32, char> all_plus_number_grouping_symbols_;

  // Pattern that makes it easy to distinguish whether a region has a unique
  // international dialing prefix or not. If a region has a unique international
  // prefix (e.g. 011 in USA), it will be represented as a string that contains
  // a sequence of ASCII digits. If there are multiple available international
  // prefixes in a region, they will be represented as a regex string that
  // always contains character(s) other than ASCII digits.
  // Note this regex also includes tilde, which signals waiting for the tone.
  scoped_ptr<const RegExp> unique_international_prefix_;

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

  PhoneNumberRegExpsAndMappings()
      : punctuation_and_star_sign_(StrCat(PhoneNumberUtil::kValidPunctuation,
                                          kStarSign)),
        valid_phone_number_(
            StrCat("[", PhoneNumberUtil::kPlusChars, "]*(?:[",
                   punctuation_and_star_sign_, "]*[",
                   kDigits, "]){3,}[", kValidAlpha,
                   punctuation_and_star_sign_, kDigits,
                   "]*")),
        extn_patterns_for_parsing_(
            CreateExtnPattern(StrCat(",", kSingleExtnSymbolsForMatching))),
        regexp_factory_(new RegExpFactory()),
        regexp_cache_(new RegExpCache(*regexp_factory_.get(), 128)),
        diallable_char_mappings_(),
        alpha_mappings_(),
        alpha_phone_mappings_(),
        all_plus_number_grouping_symbols_(),
        unique_international_prefix_(regexp_factory_->CreateRegExp(
            /* "[\\d]+(?:[~⁓∼～][\\d]+)?" */
            "[\\d]+(?:[~\xE2\x81\x93\xE2\x88\xBC\xEF\xBD\x9E][\\d]+)?")),
        digits_pattern_(
            regexp_factory_->CreateRegExp(StrCat("[", kDigits, "]*"))),
        capturing_digit_pattern_(
            regexp_factory_->CreateRegExp(StrCat("([", kDigits, "])"))),
        capturing_ascii_digits_pattern_(
            regexp_factory_->CreateRegExp("(\\d+)")),
        valid_start_char_pattern_(regexp_factory_->CreateRegExp(
            StrCat("[", PhoneNumberUtil::kPlusChars, kDigits, "]"))),
        capture_up_to_second_number_start_pattern_(
            regexp_factory_->CreateRegExp(
                PhoneNumberUtil::kCaptureUpToSecondNumberStart)),
        unwanted_end_char_pattern_(
            regexp_factory_->CreateRegExp("[^\\p{N}\\p{L}#]")),
        separator_pattern_(
            regexp_factory_->CreateRegExp(
                StrCat("[", PhoneNumberUtil::kValidPunctuation, "]+"))),
        extn_patterns_for_matching_(
            CreateExtnPattern(kSingleExtnSymbolsForMatching)),
        extn_pattern_(regexp_factory_->CreateRegExp(
            StrCat("(?i)(?:", extn_patterns_for_parsing_, ")$"))),
        valid_phone_number_pattern_(regexp_factory_->CreateRegExp(
            StrCat("(?i)", valid_phone_number_,
                   "(?:", extn_patterns_for_parsing_, ")?"))),
        valid_alpha_phone_pattern_(regexp_factory_->CreateRegExp(
            StrCat("(?i)(?:.*?[", kValidAlpha, "]){3}"))),
        // The first_group_capturing_pattern was originally set to $1 but there
        // are some countries for which the first group is not used in the
        // national pattern (e.g. Argentina) so the $1 group does not match
        // correctly. Therefore, we use \d, so that the first group actually
        // used in the pattern will be matched.
        first_group_capturing_pattern_(
            regexp_factory_->CreateRegExp("(\\$\\d)")),
        carrier_code_pattern_(regexp_factory_->CreateRegExp("\\$CC")),
        plus_chars_pattern_(
            regexp_factory_->CreateRegExp(
                StrCat("[", PhoneNumberUtil::kPlusChars, "]+"))) {
    InitializeMapsAndSets();
  }

 private:
  DISALLOW_COPY_AND_ASSIGN(PhoneNumberRegExpsAndMappings);
};

// Private constructor. Also takes care of initialisation.
PhoneNumberUtil::PhoneNumberUtil()
    : logger_(Logger::set_logger_impl(new StdoutLogger())),
      reg_exps_(new PhoneNumberRegExpsAndMappings),
      country_calling_code_to_region_code_map_(new vector<IntRegionsPair>()),
      nanpa_regions_(new set<string>()),
      region_to_metadata_map_(new map<string, PhoneMetadata>()),
      country_code_to_non_geographical_metadata_map_(
          new map<int, PhoneMetadata>) {
  Logger::set_logger_impl(logger_.get());
  // TODO: Update the java version to put the contents of the init
  // method inside the constructor as well to keep both in sync.
  PhoneMetadataCollection metadata_collection;
  if (!LoadCompiledInMetadata(&metadata_collection)) {
    LOG(DFATAL) << "Could not parse compiled-in metadata.";
    return;
  }
  // Storing data in a temporary map to make it easier to find other regions
  // that share a country calling code when inserting data.
  map<int, list<string>* > country_calling_code_to_region_map;
  for (RepeatedPtrField<PhoneMetadata>::const_iterator it =
           metadata_collection.metadata().begin();
       it != metadata_collection.metadata().end();
       ++it) {
    const string& region_code = it->id();
    if (region_code == RegionCode::GetUnknown()) {
      continue;
    }

    int country_calling_code = it->country_code();
    if (kRegionCodeForNonGeoEntity == region_code) {
      country_code_to_non_geographical_metadata_map_->insert(
          make_pair(country_calling_code, *it));
    } else {
      region_to_metadata_map_->insert(make_pair(region_code, *it));
    }
    map<int, list<string>* >::iterator calling_code_in_map =
        country_calling_code_to_region_map.find(country_calling_code);
    if (calling_code_in_map != country_calling_code_to_region_map.end()) {
      if (it->main_country_for_code()) {
        calling_code_in_map->second->push_front(region_code);
      } else {
        calling_code_in_map->second->push_back(region_code);
      }
    } else {
      // For most country calling codes, there will be only one region code.
      list<string>* list_with_region_code = new list<string>();
      list_with_region_code->push_back(region_code);
      country_calling_code_to_region_map.insert(
          make_pair(country_calling_code, list_with_region_code));
    }
    if (country_calling_code == kNanpaCountryCode) {
        nanpa_regions_->insert(region_code);
    }
  }

  country_calling_code_to_region_code_map_->insert(
      country_calling_code_to_region_code_map_->begin(),
      country_calling_code_to_region_map.begin(),
      country_calling_code_to_region_map.end());
  // Sort all the pairs in ascending order according to country calling code.
  sort(country_calling_code_to_region_code_map_->begin(),
       country_calling_code_to_region_code_map_->end(),
       OrderByFirst());
}

PhoneNumberUtil::~PhoneNumberUtil() {
  STLDeleteContainerPairSecondPointers(
      country_calling_code_to_region_code_map_->begin(),
      country_calling_code_to_region_code_map_->end());
}

void PhoneNumberUtil::GetSupportedRegions(set<string>* regions) const {
  DCHECK(regions);
  for (map<string, PhoneMetadata>::const_iterator it =
       region_to_metadata_map_->begin(); it != region_to_metadata_map_->end();
       ++it) {
    regions->insert(it->first);
  }
}

// Public wrapper function to get a PhoneNumberUtil instance with the default
// metadata file.
// static
#ifdef USE_GOOGLE_BASE
PhoneNumberUtil* PhoneNumberUtil::GetInstance() {
  return Singleton<PhoneNumberUtil>::get();
}
#endif

const string& PhoneNumberUtil::GetExtnPatternsForMatching() const {
  return reg_exps_->extn_patterns_for_matching_;
}

bool PhoneNumberUtil::ContainsOnlyValidDigits(const string& s) const {
  return reg_exps_->digits_pattern_->FullMatch(s);
}

void PhoneNumberUtil::TrimUnwantedEndChars(string* number) const {
  DCHECK(number);
  UnicodeText number_as_unicode;
  number_as_unicode.PointToUTF8(number->data(), number->size());
  char current_char[5];
  int len;
  UnicodeText::const_reverse_iterator reverse_it(number_as_unicode.end());
  for (; reverse_it.base() != number_as_unicode.begin(); ++reverse_it) {
    len = reverse_it.get_utf8(current_char);
    current_char[len] = '\0';
    if (!reg_exps_->unwanted_end_char_pattern_->FullMatch(current_char)) {
      break;
    }
  }

  number->assign(UnicodeText::UTF8Substring(number_as_unicode.begin(),
                                            reverse_it.base()));
}

bool PhoneNumberUtil::IsFormatEligibleForAsYouTypeFormatter(
    const string& format) const {
  // A pattern that is used to determine if a numberFormat under
  // availableFormats is eligible to be used by the AYTF. It is eligible when
  // the format element under numberFormat contains groups of the dollar sign
  // followed by a single digit, separated by valid phone number punctuation.
  // This prevents invalid punctuation (such as the star sign in Israeli star
  // numbers) getting into the output of the AYTF.
  const RegExp& eligible_format_pattern = reg_exps_->regexp_cache_->GetRegExp(
      StrCat("[", kValidPunctuation, "]*", "(\\$\\d", "[",
             kValidPunctuation, "]*)+"));
  return eligible_format_pattern.FullMatch(format);
}

void PhoneNumberUtil::GetNddPrefixForRegion(const string& region_code,
                                            bool strip_non_digits,
                                            string* national_prefix) const {
  DCHECK(national_prefix);
  if (!IsValidRegionCode(region_code)) {
    LOG(WARNING) << "Invalid or unknown region code (" << region_code
                 << ") provided.";
    return;
  }
  const PhoneMetadata* metadata = GetMetadataForRegion(region_code);
  national_prefix->assign(metadata->national_prefix());
  if (strip_non_digits) {
    // Note: if any other non-numeric symbols are ever used in national
    // prefixes, these would have to be removed here as well.
    strrmm(national_prefix, "~");
  }
}

bool PhoneNumberUtil::IsValidRegionCode(const string& region_code) const {
  return (region_to_metadata_map_->find(region_code) !=
          region_to_metadata_map_->end());
}

bool PhoneNumberUtil::HasValidCountryCallingCode(
    int country_calling_code) const {
  // Create an IntRegionsPair with the country_code passed in, and use it to
  // locate the pair with the same country_code in the sorted vector.
  IntRegionsPair target_pair;
  target_pair.first = country_calling_code;
  return (binary_search(country_calling_code_to_region_code_map_->begin(),
                        country_calling_code_to_region_code_map_->end(),
                        target_pair, OrderByFirst()));
}

// Returns a pointer to the phone metadata for the appropriate region.
const PhoneMetadata* PhoneNumberUtil::GetMetadataForRegion(
    const string& region_code) const {
  map<string, PhoneMetadata>::const_iterator it =
      region_to_metadata_map_->find(region_code);
  if (it != region_to_metadata_map_->end()) {
    return &it->second;
  }
  return NULL;
}

const PhoneMetadata* PhoneNumberUtil::GetMetadataForNonGeographicalRegion(
    int country_calling_code) const {
  map<int, PhoneMetadata>::const_iterator it =
      country_code_to_non_geographical_metadata_map_->find(
          country_calling_code);
  if (it != country_code_to_non_geographical_metadata_map_->end()) {
    return &it->second;
  }
  return NULL;
}

void PhoneNumberUtil::Format(const PhoneNumber& number,
                             PhoneNumberFormat number_format,
                             string* formatted_number) const {
  DCHECK(formatted_number);
  if (number.national_number() == 0) {
    const string& raw_input = number.raw_input();
    if (!raw_input.empty()) {
      formatted_number->assign(raw_input);
      return;
    }
  }
  int country_calling_code = number.country_code();
  string national_significant_number;
  GetNationalSignificantNumber(number, &national_significant_number);
  if (number_format == E164) {
    // Early exit for E164 case since no formatting of the national number needs
    // to be applied. Extensions are not formatted.
    formatted_number->assign(national_significant_number);
    PrefixNumberWithCountryCallingCode(country_calling_code, E164,
                                       formatted_number);
    return;
  }
  // Note here that all NANPA formatting rules are contained by US, so we use
  // that to format NANPA numbers. The same applies to Russian Fed regions -
  // rules are contained by Russia. French Indian Ocean country rules are
  // contained by Réunion.
  string region_code;
  GetRegionCodeForCountryCode(country_calling_code, &region_code);
  if (!HasValidCountryCallingCode(country_calling_code)) {
    formatted_number->assign(national_significant_number);
    return;
  }
  const PhoneMetadata* metadata =
      GetMetadataForRegionOrCallingCode(country_calling_code, region_code);
  FormatNsn(national_significant_number, *metadata, number_format,
            formatted_number);
  MaybeAppendFormattedExtension(number, *metadata, number_format,
                                formatted_number);
  PrefixNumberWithCountryCallingCode(country_calling_code, number_format,
                                     formatted_number);
}

void PhoneNumberUtil::FormatByPattern(
    const PhoneNumber& number,
    PhoneNumberFormat number_format,
    const RepeatedPtrField<NumberFormat>& user_defined_formats,
    string* formatted_number) const {
  DCHECK(formatted_number);
  int country_calling_code = number.country_code();
  // Note GetRegionCodeForCountryCode() is used because formatting information
  // for regions which share a country calling code is contained by only one
  // region for performance reasons. For example, for NANPA regions it will be
  // contained in the metadata for US.
  string region_code;
  GetRegionCodeForCountryCode(country_calling_code, &region_code);
  string national_significant_number;
  GetNationalSignificantNumber(number, &national_significant_number);
  if (!HasValidCountryCallingCode(country_calling_code)) {
    formatted_number->assign(national_significant_number);
    return;
  }
  const PhoneMetadata* metadata =
      GetMetadataForRegionOrCallingCode(country_calling_code, region_code);
  const NumberFormat* formatting_pattern =
      ChooseFormattingPatternForNumber(user_defined_formats,
                                       national_significant_number);
  if (!formatting_pattern) {
    // If no pattern above is matched, we format the number as a whole.
    formatted_number->assign(national_significant_number);
  } else {
    NumberFormat num_format_copy;
    // Before we do a replacement of the national prefix pattern $NP with the
    // national prefix, we need to copy the rule so that subsequent replacements
    // for different numbers have the appropriate national prefix.
    num_format_copy.MergeFrom(*formatting_pattern);
    string national_prefix_formatting_rule(
        formatting_pattern->national_prefix_formatting_rule());
    if (!national_prefix_formatting_rule.empty()) {
      const string& national_prefix = metadata->national_prefix();
      if (!national_prefix.empty()) {
        // Replace $NP with national prefix and $FG with the first group ($1).
        GlobalReplaceSubstring("$NP", national_prefix,
                               &national_prefix_formatting_rule);
        GlobalReplaceSubstring("$FG", "$1",
                               &national_prefix_formatting_rule);
        num_format_copy.set_national_prefix_formatting_rule(
            national_prefix_formatting_rule);
      } else {
        // We don't want to have a rule for how to format the national prefix if
        // there isn't one.
        num_format_copy.clear_national_prefix_formatting_rule();
      }
    }
    FormatNsnUsingPattern(national_significant_number, num_format_copy,
                          number_format, formatted_number);
  }
  MaybeAppendFormattedExtension(number, *metadata, NATIONAL, formatted_number);
  PrefixNumberWithCountryCallingCode(country_calling_code, number_format,
                                     formatted_number);
}

void PhoneNumberUtil::FormatNationalNumberWithCarrierCode(
    const PhoneNumber& number,
    const string& carrier_code,
    string* formatted_number) const {
  int country_calling_code = number.country_code();
  string national_significant_number;
  GetNationalSignificantNumber(number, &national_significant_number);
  // Note GetRegionCodeForCountryCode() is used because formatting information
  // for regions which share a country calling code is contained by only one
  // region for performance reasons. For example, for NANPA regions it will be
  // contained in the metadata for US.
  string region_code;
  GetRegionCodeForCountryCode(country_calling_code, &region_code);
  if (!HasValidCountryCallingCode(country_calling_code)) {
    formatted_number->assign(national_significant_number);
  }
  const PhoneMetadata* metadata =
      GetMetadataForRegionOrCallingCode(country_calling_code, region_code);
  FormatNsnWithCarrier(national_significant_number, *metadata, NATIONAL,
                       carrier_code, formatted_number);
  MaybeAppendFormattedExtension(number, *metadata, NATIONAL, formatted_number);
  PrefixNumberWithCountryCallingCode(country_calling_code, NATIONAL,
                                     formatted_number);
}

const PhoneMetadata* PhoneNumberUtil::GetMetadataForRegionOrCallingCode(
      int country_calling_code, const string& region_code) const {
  return kRegionCodeForNonGeoEntity == region_code
      ? GetMetadataForNonGeographicalRegion(country_calling_code)
      : GetMetadataForRegion(region_code);
}

void PhoneNumberUtil::FormatNationalNumberWithPreferredCarrierCode(
    const PhoneNumber& number,
    const string& fallback_carrier_code,
    string* formatted_number) const {
  FormatNationalNumberWithCarrierCode(
      number,
      number.has_preferred_domestic_carrier_code()
          ? number.preferred_domestic_carrier_code()
          : fallback_carrier_code,
      formatted_number);
}

void PhoneNumberUtil::FormatNumberForMobileDialing(
    const PhoneNumber& number,
    const string& calling_from,
    bool with_formatting,
    string* formatted_number) const {
  int country_calling_code = number.country_code();
  if (!HasValidCountryCallingCode(country_calling_code)) {
    formatted_number->assign(number.has_raw_input() ? number.raw_input() : "");
    return;
  }

  // Clear the extension, as that part cannot normally be dialed together with
  // the main number.
  PhoneNumber number_no_extension(number);
  number_no_extension.clear_extension();
  PhoneNumberType number_type = GetNumberType(number_no_extension);
  string region_code;
  GetRegionCodeForCountryCode(country_calling_code, &region_code);
  if ((region_code == "CO") && (calling_from == "CO")) {
    if (number_type == FIXED_LINE) {
      FormatNationalNumberWithCarrierCode(
          number_no_extension, kColombiaMobileToFixedLinePrefix,
          formatted_number);
    } else {
      // E164 doesn't work at all when dialing within Colombia.
      Format(number_no_extension, NATIONAL, formatted_number);
    }
  } else if ((region_code == "PE") && (calling_from == "PE")) {
    // In Peru, numbers cannot be dialled using E164 format from a mobile phone
    // for Movistar. Instead they must be dialled in national format.
    Format(number_no_extension, NATIONAL, formatted_number);
  } else if ((region_code == "BR") && (calling_from == "BR") &&
      ((number_type == FIXED_LINE) || (number_type == MOBILE) ||
       (number_type == FIXED_LINE_OR_MOBILE))) {
    if (number_no_extension.has_preferred_domestic_carrier_code()) {
      FormatNationalNumberWithPreferredCarrierCode(number_no_extension, "",
                                                   formatted_number);
    } else {
      // Brazilian fixed line and mobile numbers need to be dialed with a
      // carrier code when called within Brazil. Without that, most of the
      // carriers won't connect the call. Because of that, we return an empty
      // string here.
      formatted_number->assign("");
    }
  } else if (CanBeInternationallyDialled(number_no_extension)) {
    with_formatting
        ? Format(number_no_extension, INTERNATIONAL, formatted_number)
        : Format(number_no_extension, E164, formatted_number);
    return;
  } else if (calling_from == region_code) {
    Format(number_no_extension, NATIONAL, formatted_number);
  } else {
    formatted_number->assign("");
  }
  if (!with_formatting) {
    NormalizeHelper(reg_exps_->diallable_char_mappings_,
                    true /* remove non matches */, formatted_number);
  }
}

void PhoneNumberUtil::FormatOutOfCountryCallingNumber(
    const PhoneNumber& number,
    const string& calling_from,
    string* formatted_number) const {
  DCHECK(formatted_number);
  if (!IsValidRegionCode(calling_from)) {
    LOG(WARNING) << "Trying to format number from invalid region "
                 << calling_from
                 << ". International formatting applied.";
    Format(number, INTERNATIONAL, formatted_number);
    return;
  }
  int country_code = number.country_code();
  string national_significant_number;
  GetNationalSignificantNumber(number, &national_significant_number);
  if (!HasValidCountryCallingCode(country_code)) {
    formatted_number->assign(national_significant_number);
    return;
  }
  if (country_code == kNanpaCountryCode) {
    if (IsNANPACountry(calling_from)) {
      // For NANPA regions, return the national format for these regions but
      // prefix it with the country calling code.
      Format(number, NATIONAL, formatted_number);
      formatted_number->insert(0, StrCat(country_code, " "));
      return;
    }
  } else if (country_code == GetCountryCodeForValidRegion(calling_from)) {
    // If neither region is a NANPA region, then we check to see if the
    // country calling code of the number and the country calling code of the
    // region we are calling from are the same.
    // For regions that share a country calling code, the country calling code
    // need not be dialled. This also applies when dialling within a region, so
    // this if clause covers both these cases.
    // Technically this is the case for dialling from la Réunion to other
    // overseas departments of France (French Guiana, Martinique, Guadeloupe),
    // but not vice versa - so we don't cover this edge case for now and for
    // those cases return the version including country calling code.
    // Details here:
    // http://www.petitfute.com/voyage/225-info-pratiques-reunion
    Format(number, NATIONAL, formatted_number);
    return;
  }
  string region_code;
  GetRegionCodeForCountryCode(country_code, &region_code);
  const PhoneMetadata* metadata_for_region =
      GetMetadataForRegionOrCallingCode(country_code, region_code);
  const PhoneMetadata* metadata_calling_from =
      GetMetadataForRegion(calling_from);
  const string& international_prefix =
      metadata_calling_from->international_prefix();
  // For regions that have multiple international prefixes, the international
  // format of the number is returned, unless there is a preferred international
  // prefix.
  const string international_prefix_for_formatting(
      reg_exps_->unique_international_prefix_->FullMatch(international_prefix)
      ? international_prefix
      : metadata_calling_from->preferred_international_prefix());
  FormatNsn(national_significant_number, *metadata_for_region, INTERNATIONAL,
            formatted_number);
  MaybeAppendFormattedExtension(number, *metadata_for_region, INTERNATIONAL,
                                formatted_number);
  if (!international_prefix_for_formatting.empty()) {
    formatted_number->insert(
        0, StrCat(international_prefix_for_formatting, " ", country_code, " "));
  } else {
    PrefixNumberWithCountryCallingCode(country_code, INTERNATIONAL,
                                       formatted_number);
  }
}

void PhoneNumberUtil::FormatInOriginalFormat(const PhoneNumber& number,
                                             const string& region_calling_from,
                                             string* formatted_number) const {
  DCHECK(formatted_number);

  if (number.has_raw_input() &&
      (HasUnexpectedItalianLeadingZero(number) ||
       !HasFormattingPatternForNumber(number))) {
    // We check if we have the formatting pattern because without that, we might
    // format the number as a group without national prefix.
    formatted_number->assign(number.raw_input());
    return;
  }
  if (!number.has_country_code_source()) {
    Format(number, NATIONAL, formatted_number);
    return;
  }
  switch (number.country_code_source()) {
    case PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN:
      Format(number, INTERNATIONAL, formatted_number);
      break;
    case PhoneNumber::FROM_NUMBER_WITH_IDD:
      FormatOutOfCountryCallingNumber(number, region_calling_from,
                                      formatted_number);
      break;
    case PhoneNumber::FROM_NUMBER_WITHOUT_PLUS_SIGN:
      Format(number, INTERNATIONAL, formatted_number);
      formatted_number->erase(formatted_number->begin());
      break;
    case PhoneNumber::FROM_DEFAULT_COUNTRY:
      // Fall-through to default case.
    default:
      string region_code;
      GetRegionCodeForCountryCode(number.country_code(), &region_code);
      // We strip non-digits from the NDD here, and from the raw input later, so
      // that we can compare them easily.
      string national_prefix;
      GetNddPrefixForRegion(region_code, true /* strip non-digits */,
                            &national_prefix);
      if (national_prefix.empty()) {
        // If the region doesn't have a national prefix at all, we can safely
        // return the national format without worrying about a national prefix
        // being added.
        Format(number, NATIONAL, formatted_number);
        break;
      }
      // Otherwise, we check if the original number was entered with a national
      // prefix.
      if (RawInputContainsNationalPrefix(number.raw_input(), national_prefix,
                                         region_code)) {
        // If so, we can safely return the national format.
        Format(number, NATIONAL, formatted_number);
        break;
      }
      const PhoneMetadata* metadata = GetMetadataForRegion(region_code);
      string national_number;
      GetNationalSignificantNumber(number, &national_number);
      // This shouldn't be NULL, because we have checked that above with
      // HasFormattingPatternForNumber.
      const NumberFormat* format_rule =
          ChooseFormattingPatternForNumber(metadata->number_format(),
                                           national_number);
      // When the format we apply to this number doesn't contain national
      // prefix, we can just return the national format.
      // TODO: Refactor the code below with the code in
      // IsNationalPrefixPresentIfRequired.
      string candidate_national_prefix_rule(
          format_rule->national_prefix_formatting_rule());
      // We assume that the first-group symbol will never be _before_ the
      // national prefix.
      if (!candidate_national_prefix_rule.empty()) {
        candidate_national_prefix_rule.erase(
            candidate_national_prefix_rule.find("$1"));
        NormalizeDigitsOnly(&candidate_national_prefix_rule);
      }
      if (candidate_national_prefix_rule.empty()) {
        // National prefix not used when formatting this number.
        Format(number, NATIONAL, formatted_number);
        break;
      }
      // Otherwise, we need to remove the national prefix from our output.
      RepeatedPtrField<NumberFormat> number_formats;
      NumberFormat* number_format = number_formats.Add();
      number_format->MergeFrom(*format_rule);
      number_format->clear_national_prefix_formatting_rule();
      FormatByPattern(number, NATIONAL, number_formats, formatted_number);
      break;
  }
  // If no digit is inserted/removed/modified as a result of our formatting, we
  // return the formatted phone number; otherwise we return the raw input the
  // user entered.
  if (!formatted_number->empty()) {
    string formatted_number_copy(*formatted_number);
    NormalizeDigitsOnly(&formatted_number_copy);
    string raw_input_copy(number.raw_input());
    NormalizeDigitsOnly(&raw_input_copy);
    if (formatted_number_copy != raw_input_copy) {
      formatted_number->assign(number.raw_input());
    }
  }
}

// Check if raw_input, which is assumed to be in the national format, has a
// national prefix. The national prefix is assumed to be in digits-only form.
bool PhoneNumberUtil::RawInputContainsNationalPrefix(
    const string& raw_input,
    const string& national_prefix,
    const string& region_code) const {
  string normalized_national_number(raw_input);
  NormalizeDigitsOnly(&normalized_national_number);
  if (HasPrefixString(normalized_national_number, national_prefix)) {
    // Some Japanese numbers (e.g. 00777123) might be mistaken to contain
    // the national prefix when written without it (e.g. 0777123) if we just
    // do prefix matching. To tackle that, we check the validity of the
    // number if the assumed national prefix is removed (777123 won't be
    // valid in Japan).
    PhoneNumber number_without_national_prefix;
    if (Parse(normalized_national_number.substr(national_prefix.length()),
              region_code, &number_without_national_prefix)
        == NO_PARSING_ERROR) {
      return IsValidNumber(number_without_national_prefix);
    }
  }
  return false;
}

bool PhoneNumberUtil::HasUnexpectedItalianLeadingZero(
    const PhoneNumber& number) const {
  return number.has_italian_leading_zero() &&
      !IsLeadingZeroPossible(number.country_code());
}

bool PhoneNumberUtil::HasFormattingPatternForNumber(
    const PhoneNumber& number) const {
  int country_calling_code = number.country_code();
  string region_code;
  GetRegionCodeForCountryCode(country_calling_code, &region_code);
  const PhoneMetadata* metadata =
      GetMetadataForRegionOrCallingCode(country_calling_code, region_code);
  if (!metadata) {
    return false;
  }
  string national_number;
  GetNationalSignificantNumber(number, &national_number);
  const NumberFormat* format_rule =
      ChooseFormattingPatternForNumber(metadata->number_format(),
                                       national_number);
  return format_rule;
}

void PhoneNumberUtil::FormatOutOfCountryKeepingAlphaChars(
    const PhoneNumber& number,
    const string& calling_from,
    string* formatted_number) const {
  // If there is no raw input, then we can't keep alpha characters because there
  // aren't any. In this case, we return FormatOutOfCountryCallingNumber.
  if (number.raw_input().empty()) {
    FormatOutOfCountryCallingNumber(number, calling_from, formatted_number);
    return;
  }
  int country_code = number.country_code();
  if (!HasValidCountryCallingCode(country_code)) {
    formatted_number->assign(number.raw_input());
    return;
  }
  // Strip any prefix such as country calling code, IDD, that was present. We do
  // this by comparing the number in raw_input with the parsed number.
  string raw_input_copy(number.raw_input());
  // Normalize punctuation. We retain number grouping symbols such as " " only.
  NormalizeHelper(reg_exps_->all_plus_number_grouping_symbols_, true,
                  &raw_input_copy);
  // Now we trim everything before the first three digits in the parsed number.
  // We choose three because all valid alpha numbers have 3 digits at the start
  // - if it does not, then we don't trim anything at all. Similarly, if the
  // national number was less than three digits, we don't trim anything at all.
  string national_number;
  GetNationalSignificantNumber(number, &national_number);
  if (national_number.length() > 3) {
    size_t first_national_number_digit =
        raw_input_copy.find(national_number.substr(0, 3));
    if (first_national_number_digit != string::npos) {
      raw_input_copy = raw_input_copy.substr(first_national_number_digit);
    }
  }
  const PhoneMetadata* metadata = GetMetadataForRegion(calling_from);
  if (country_code == kNanpaCountryCode) {
    if (IsNANPACountry(calling_from)) {
      StrAppend(formatted_number, country_code, " ", raw_input_copy);
      return;
    }
  } else if (IsValidRegionCode(calling_from) &&
             country_code == GetCountryCodeForValidRegion(calling_from)) {
    const NumberFormat* formatting_pattern =
        ChooseFormattingPatternForNumber(metadata->number_format(),
                                         national_number);
    if (!formatting_pattern) {
      // If no pattern above is matched, we format the original input.
      formatted_number->assign(raw_input_copy);
      return;
    }
    NumberFormat new_format;
    new_format.MergeFrom(*formatting_pattern);
    // The first group is the first group of digits that the user wrote
    // together.
    new_format.set_pattern("(\\d+)(.*)");
    // Here we just concatenate them back together after the national prefix
    // has been fixed.
    new_format.set_format("$1$2");
    // Now we format using this pattern instead of the default pattern, but
    // with the national prefix prefixed if necessary.
    // This will not work in the cases where the pattern (and not the
    // leading digits) decide whether a national prefix needs to be used, since
    // we have overridden the pattern to match anything, but that is not the
    // case in the metadata to date.
    FormatNsnUsingPattern(raw_input_copy, new_format, NATIONAL,
                          formatted_number);
    return;
  }

  string international_prefix_for_formatting;
  // If an unsupported region-calling-from is entered, or a country with
  // multiple international prefixes, the international format of the number is
  // returned, unless there is a preferred international prefix.
  if (metadata) {
    const string& international_prefix = metadata->international_prefix();
    international_prefix_for_formatting =
        reg_exps_->unique_international_prefix_->FullMatch(international_prefix)
        ? international_prefix
        : metadata->preferred_international_prefix();
  }
  if (!international_prefix_for_formatting.empty()) {
    StrAppend(formatted_number, international_prefix_for_formatting, " ",
              country_code, " ", raw_input_copy);
  } else {
    // Invalid region entered as country-calling-from (so no metadata was found
    // for it) or the region chosen has multiple international dialling
    // prefixes.
    LOG(WARNING) << "Trying to format number from invalid region "
                 << calling_from
                 << ". International formatting applied.";
    formatted_number->assign(raw_input_copy);
    PrefixNumberWithCountryCallingCode(country_code, INTERNATIONAL,
                                       formatted_number);
  }
}

const NumberFormat* PhoneNumberUtil::ChooseFormattingPatternForNumber(
    const RepeatedPtrField<NumberFormat>& available_formats,
    const string& national_number) const {
  for (RepeatedPtrField<NumberFormat>::const_iterator
       it = available_formats.begin(); it != available_formats.end(); ++it) {
    int size = it->leading_digits_pattern_size();
    if (size > 0) {
      const scoped_ptr<RegExpInput> number_copy(
          reg_exps_->regexp_factory_->CreateInput(national_number));
      // We always use the last leading_digits_pattern, as it is the most
      // detailed.
      if (!reg_exps_->regexp_cache_->GetRegExp(
              it->leading_digits_pattern(size - 1)).Consume(
                  number_copy.get())) {
        continue;
      }
    }
    const RegExp& pattern_to_match(
        reg_exps_->regexp_cache_->GetRegExp(it->pattern()));
    if (pattern_to_match.FullMatch(national_number)) {
      return &(*it);
    }
  }
  return NULL;
}

// Note that carrier_code is optional - if an empty string, no carrier code
// replacement will take place.
void PhoneNumberUtil::FormatNsnUsingPatternWithCarrier(
    const string& national_number,
    const NumberFormat& formatting_pattern,
    PhoneNumberUtil::PhoneNumberFormat number_format,
    const string& carrier_code,
    string* formatted_number) const {
  DCHECK(formatted_number);
  string number_format_rule(formatting_pattern.format());
  if (number_format == PhoneNumberUtil::NATIONAL &&
      carrier_code.length() > 0 &&
      formatting_pattern.domestic_carrier_code_formatting_rule().length() > 0) {
    // Replace the $CC in the formatting rule with the desired carrier code.
    string carrier_code_formatting_rule =
        formatting_pattern.domestic_carrier_code_formatting_rule();
    reg_exps_->carrier_code_pattern_->Replace(&carrier_code_formatting_rule,
                                              carrier_code);
    reg_exps_->first_group_capturing_pattern_->
        Replace(&number_format_rule, carrier_code_formatting_rule);
  } else {
    // Use the national prefix formatting rule instead.
    string national_prefix_formatting_rule =
        formatting_pattern.national_prefix_formatting_rule();
    if (number_format == PhoneNumberUtil::NATIONAL &&
        national_prefix_formatting_rule.length() > 0) {
      // Apply the national_prefix_formatting_rule as the formatting_pattern
      // contains only information on how the national significant number
      // should be formatted at this point.
      reg_exps_->first_group_capturing_pattern_->Replace(
          &number_format_rule, national_prefix_formatting_rule);
    }
  }
  formatted_number->assign(national_number);

  const RegExp& pattern_to_match(
      reg_exps_->regexp_cache_->GetRegExp(formatting_pattern.pattern()));
  pattern_to_match.GlobalReplace(formatted_number, number_format_rule);

  if (number_format == RFC3966) {
    // First consume any leading punctuation, if any was present.
    const scoped_ptr<RegExpInput> number(
        reg_exps_->regexp_factory_->CreateInput(*formatted_number));
    if (reg_exps_->separator_pattern_->Consume(number.get())) {
      formatted_number->assign(number->ToString());
    }
    // Then replace all separators with a "-".
    reg_exps_->separator_pattern_->GlobalReplace(formatted_number, "-");
  }
}

// Simple wrapper of FormatNsnUsingPatternWithCarrier for the common case of
// no carrier code.
void PhoneNumberUtil::FormatNsnUsingPattern(
    const string& national_number,
    const NumberFormat& formatting_pattern,
    PhoneNumberUtil::PhoneNumberFormat number_format,
    string* formatted_number) const {
  DCHECK(formatted_number);
  FormatNsnUsingPatternWithCarrier(national_number, formatting_pattern,
                                   number_format, "", formatted_number);
}

void PhoneNumberUtil::FormatNsn(const string& number,
                                const PhoneMetadata& metadata,
                                PhoneNumberFormat number_format,
                                string* formatted_number) const {
  DCHECK(formatted_number);
  FormatNsnWithCarrier(number, metadata, number_format, "", formatted_number);
}

// Note in some regions, the national number can be written in two completely
// different ways depending on whether it forms part of the NATIONAL format or
// INTERNATIONAL format. The number_format parameter here is used to specify
// which format to use for those cases. If a carrier_code is specified, this
// will be inserted into the formatted string to replace $CC.
void PhoneNumberUtil::FormatNsnWithCarrier(const string& number,
                                           const PhoneMetadata& metadata,
                                           PhoneNumberFormat number_format,
                                           const string& carrier_code,
                                           string* formatted_number) const {
  DCHECK(formatted_number);
  // When the intl_number_formats exists, we use that to format national number
  // for the INTERNATIONAL format instead of using the number_formats.
  const RepeatedPtrField<NumberFormat> available_formats =
      (metadata.intl_number_format_size() == 0 || number_format == NATIONAL)
      ? metadata.number_format()
      : metadata.intl_number_format();
  const NumberFormat* formatting_pattern =
      ChooseFormattingPatternForNumber(available_formats, number);
  if (!formatting_pattern) {
    formatted_number->assign(number);
  } else {
    FormatNsnUsingPatternWithCarrier(number, *formatting_pattern, number_format,
                                     carrier_code, formatted_number);
  }
}

// Appends the formatted extension of a phone number, if the phone number had an
// extension specified.
void PhoneNumberUtil::MaybeAppendFormattedExtension(
    const PhoneNumber& number,
    const PhoneMetadata& metadata,
    PhoneNumberFormat number_format,
    string* formatted_number) const {
  DCHECK(formatted_number);
  if (number.has_extension() && number.extension().length() > 0) {
    if (number_format == RFC3966) {
      StrAppend(formatted_number, kRfc3966ExtnPrefix, number.extension());
    } else {
      if (metadata.has_preferred_extn_prefix()) {
        StrAppend(formatted_number, metadata.preferred_extn_prefix(),
                  number.extension());
      } else {
        StrAppend(formatted_number, kDefaultExtnPrefix, number.extension());
      }
    }
  }
}

bool PhoneNumberUtil::IsNANPACountry(const string& region_code) const {
  return nanpa_regions_->find(region_code) != nanpa_regions_->end();
}

// Returns the region codes that matches the specific country calling code. In
// the case of no region code being found, region_codes will be left empty.
void PhoneNumberUtil::GetRegionCodesForCountryCallingCode(
    int country_calling_code,
    list<string>* region_codes) const {
  DCHECK(region_codes);
  // Create a IntRegionsPair with the country_code passed in, and use it to
  // locate the pair with the same country_code in the sorted vector.
  IntRegionsPair target_pair;
  target_pair.first = country_calling_code;
  typedef vector<IntRegionsPair>::const_iterator ConstIterator;
  pair<ConstIterator, ConstIterator> range = equal_range(
      country_calling_code_to_region_code_map_->begin(),
      country_calling_code_to_region_code_map_->end(),
      target_pair, OrderByFirst());
  if (range.first != range.second) {
    region_codes->insert(region_codes->begin(),
                         range.first->second->begin(),
                         range.first->second->end());
  }
}

// Returns the region code that matches the specific country calling code. In
// the case of no region code being found, the unknown region code will be
// returned.
void PhoneNumberUtil::GetRegionCodeForCountryCode(
    int country_calling_code,
    string* region_code) const {
  DCHECK(region_code);
  list<string> region_codes;

  GetRegionCodesForCountryCallingCode(country_calling_code, &region_codes);
  *region_code = (region_codes.size() > 0) ?
      region_codes.front() : RegionCode::GetUnknown();
}

void PhoneNumberUtil::GetRegionCodeForNumber(const PhoneNumber& number,
                                             string* region_code) const {
  DCHECK(region_code);
  int country_calling_code = number.country_code();
  list<string> region_codes;
  GetRegionCodesForCountryCallingCode(country_calling_code, &region_codes);
  if (region_codes.size() == 0) {
    string number_string;
    GetNationalSignificantNumber(number, &number_string);
    LOG(WARNING) << "Missing/invalid country calling code ("
                 << country_calling_code
                 << ") for number " << number_string;
    *region_code = RegionCode::GetUnknown();
    return;
  }
  if (region_codes.size() == 1) {
    *region_code = region_codes.front();
  } else {
    GetRegionCodeForNumberFromRegionList(number, region_codes, region_code);
  }
}

void PhoneNumberUtil::GetRegionCodeForNumberFromRegionList(
    const PhoneNumber& number, const list<string>& region_codes,
    string* region_code) const {
  DCHECK(region_code);
  string national_number;
  GetNationalSignificantNumber(number, &national_number);
  for (list<string>::const_iterator it = region_codes.begin();
       it != region_codes.end(); ++it) {
    const PhoneMetadata* metadata = GetMetadataForRegion(*it);
    if (metadata->has_leading_digits()) {
      const scoped_ptr<RegExpInput> number(
          reg_exps_->regexp_factory_->CreateInput(national_number));
      if (reg_exps_->regexp_cache_->
              GetRegExp(metadata->leading_digits()).Consume(number.get())) {
        *region_code = *it;
        return;
      }
    } else if (GetNumberTypeHelper(national_number, *metadata,
                                   reg_exps_->regexp_cache_.get()) != UNKNOWN) {
      *region_code = *it;
      return;
    }
  }
  *region_code = RegionCode::GetUnknown();
}

int PhoneNumberUtil::GetCountryCodeForRegion(const string& region_code) const {
  if (!IsValidRegionCode(region_code)) {
    LOG(WARNING) << "Invalid or unknown region code (" << region_code
                 << ") provided.";
    return 0;
  }
  return GetCountryCodeForValidRegion(region_code);
}

int PhoneNumberUtil::GetCountryCodeForValidRegion(
    const string& region_code) const {
  const PhoneMetadata* metadata = GetMetadataForRegion(region_code);
  return metadata->country_code();
}

// Gets a valid fixed-line number for the specified region_code. Returns false
// if the region was unknown or 001 (representing non-geographical regions), or
// if no number exists.
bool PhoneNumberUtil::GetExampleNumber(const string& region_code,
                                       PhoneNumber* number) const {
  DCHECK(number);
  return GetExampleNumberForType(region_code, FIXED_LINE, number);
}

// Gets a valid number for the specified region_code and type.  Returns false if
// the country was unknown or 001 (representing non-geographical regions), or if
// no number exists.
bool PhoneNumberUtil::GetExampleNumberForType(
    const string& region_code,
    PhoneNumberUtil::PhoneNumberType type,
    PhoneNumber* number) const {
  DCHECK(number);
  if (!IsValidRegionCode(region_code)) {
    LOG(WARNING) << "Invalid or unknown region code (" << region_code
                 << ") provided.";
    return false;
  }
  const PhoneMetadata* region_metadata = GetMetadataForRegion(region_code);
  const PhoneNumberDesc* desc = GetNumberDescByType(*region_metadata, type);
  if (desc && desc->has_example_number()) {
    ErrorType success = Parse(desc->example_number(), region_code, number);
    if (success == NO_PARSING_ERROR) {
      return true;
    } else {
      LOG(ERROR) << "Error parsing example number ("
                 << static_cast<int>(success) << ")";
    }
  }
  return false;
}

bool PhoneNumberUtil::GetExampleNumberForNonGeoEntity(
    int country_calling_code, PhoneNumber* number) const {
  DCHECK(number);
  const PhoneMetadata* metadata =
      GetMetadataForNonGeographicalRegion(country_calling_code);
  if (metadata) {
    const PhoneNumberDesc& desc = metadata->general_desc();
    if (desc.has_example_number()) {
      ErrorType success = Parse(StrCat(kPlusSign,
                                       SimpleItoa(country_calling_code),
                                       desc.example_number()),
                                RegionCode::ZZ(), number);
      if (success == NO_PARSING_ERROR) {
        return true;
      } else {
        LOG(ERROR) << "Error parsing example number ("
                   << static_cast<int>(success) << ")";
      }
    }
  } else {
    LOG(WARNING) << "Invalid or unknown country calling code provided: "
                 << country_calling_code;
  }
  return false;
}

PhoneNumberUtil::ErrorType PhoneNumberUtil::Parse(const string& number_to_parse,
                                                  const string& default_region,
                                                  PhoneNumber* number) const {
  DCHECK(number);
  return ParseHelper(number_to_parse, default_region, false, true, number);
}

PhoneNumberUtil::ErrorType PhoneNumberUtil::ParseAndKeepRawInput(
    const string& number_to_parse,
    const string& default_region,
    PhoneNumber* number) const {
  DCHECK(number);
  return ParseHelper(number_to_parse, default_region, true, true, number);
}

// Checks to see that the region code used is valid, or if it is not valid, that
// the number to parse starts with a + symbol so that we can attempt to infer
// the country from the number. Returns false if it cannot use the region
// provided and the region cannot be inferred.
bool PhoneNumberUtil::CheckRegionForParsing(
    const string& number_to_parse,
    const string& default_region) const {
  if (!IsValidRegionCode(default_region) && !number_to_parse.empty()) {
    const scoped_ptr<RegExpInput> number(
        reg_exps_->regexp_factory_->CreateInput(number_to_parse));
    if (!reg_exps_->plus_chars_pattern_->Consume(number.get())) {
      return false;
    }
  }
  return true;
}

PhoneNumberUtil::ErrorType PhoneNumberUtil::ParseHelper(
    const string& number_to_parse,
    const string& default_region,
    bool keep_raw_input,
    bool check_region,
    PhoneNumber* phone_number) const {
  DCHECK(phone_number);

  size_t index_of_phone_context = number_to_parse.find(kRfc3966PhoneContext);
  string national_number;
  if (index_of_phone_context != string::npos) {
    // Prefix the number with the phone context. The offset here is because the
    // context we are expecting to match should start with a "+" sign, and we
    // want to include this at the start of the number.
    StrAppend(
        &national_number,
        number_to_parse.substr(
            index_of_phone_context + strlen(kRfc3966PhoneContext) - 1));
    // Now append everything between the "tel:" prefix and the phone-context.
    int end_of_rfc_prefix =
        number_to_parse.find(kRfc3966Prefix) + strlen(kRfc3966Prefix);
    StrAppend(
        &national_number,
        number_to_parse.substr(end_of_rfc_prefix,
                               index_of_phone_context - end_of_rfc_prefix));
    // Note that phone-contexts that are URLs will not be parsed -
    // IsViablePhoneNumber will throw an exception below.
  } else {
    // Extract a possible number from the string passed in (this strips leading
    // characters that could not be the start of a phone number.)
    ExtractPossibleNumber(number_to_parse, &national_number);
  }

  if (!IsViablePhoneNumber(national_number)) {
    VLOG(2) << "The string supplied did not seem to be a phone number.";
    return NOT_A_NUMBER;
  }

  if (check_region &&
      !CheckRegionForParsing(national_number, default_region)) {
    VLOG(1) << "Missing or invalid default country.";
    return INVALID_COUNTRY_CODE_ERROR;
  }
  PhoneNumber temp_number;
  if (keep_raw_input) {
    temp_number.set_raw_input(number_to_parse);
  }
  // Attempt to parse extension first, since it doesn't require country-specific
  // data and we want to have the non-normalised number here.
  string extension;
  MaybeStripExtension(&national_number, &extension);
  if (!extension.empty()) {
    temp_number.set_extension(extension);
  }
  const PhoneMetadata* country_metadata = GetMetadataForRegion(default_region);
  // Check to see if the number is given in international format so we know
  // whether this number is from the default country or not.
  string normalized_national_number(national_number);
  ErrorType country_code_error =
      MaybeExtractCountryCode(country_metadata, keep_raw_input,
                              &normalized_national_number, &temp_number);
  if (country_code_error != NO_PARSING_ERROR) {
     const scoped_ptr<RegExpInput> number_string_piece(
        reg_exps_->regexp_factory_->CreateInput(national_number));
    if ((country_code_error == INVALID_COUNTRY_CODE_ERROR) &&
        (reg_exps_->plus_chars_pattern_->Consume(number_string_piece.get()))) {
      normalized_national_number.assign(number_string_piece->ToString());
      // Strip the plus-char, and try again.
      MaybeExtractCountryCode(country_metadata,
                              keep_raw_input,
                              &normalized_national_number,
                              &temp_number);
      if (temp_number.country_code() == 0) {
        return INVALID_COUNTRY_CODE_ERROR;
      }
    } else {
      return country_code_error;
    }
  }
  int country_code = temp_number.country_code();
  if (country_code != 0) {
    string phone_number_region;
    GetRegionCodeForCountryCode(country_code, &phone_number_region);
    if (phone_number_region != default_region) {
      country_metadata =
          GetMetadataForRegionOrCallingCode(country_code, phone_number_region);
    }
  } else if (country_metadata) {
    // If no extracted country calling code, use the region supplied instead.
    // Note that the national number was already normalized by
    // MaybeExtractCountryCode.
    country_code = country_metadata->country_code();
  }
  if (normalized_national_number.length() < kMinLengthForNsn) {
    VLOG(2) << "The string supplied is too short to be a phone number.";
    return TOO_SHORT_NSN;
  }
  if (country_metadata) {
    string* carrier_code = keep_raw_input ?
        temp_number.mutable_preferred_domestic_carrier_code() : NULL;
    MaybeStripNationalPrefixAndCarrierCode(*country_metadata,
                                           &normalized_national_number,
                                           carrier_code);
  }
  size_t normalized_national_number_length =
      normalized_national_number.length();
  if (normalized_national_number_length < kMinLengthForNsn) {
    VLOG(2) << "The string supplied is too short to be a phone number.";
    return TOO_SHORT_NSN;
  }
  if (normalized_national_number_length > kMaxLengthForNsn) {
    VLOG(2) << "The string supplied is too long to be a phone number.";
    return TOO_LONG_NSN;
  }
  temp_number.set_country_code(country_code);
  if (normalized_national_number[0] == '0') {
    temp_number.set_italian_leading_zero(true);
  }
  uint64 number_as_int;
  safe_strtou64(normalized_national_number, &number_as_int);
  temp_number.set_national_number(number_as_int);
  phone_number->MergeFrom(temp_number);
  return NO_PARSING_ERROR;
}

// Attempts to extract a possible number from the string passed in. This
// currently strips all leading characters that could not be used to start a
// phone number. Characters that can be used to start a phone number are
// defined in the valid_start_char_pattern. If none of these characters are
// found in the number passed in, an empty string is returned. This function
// also attempts to strip off any alternative extensions or endings if two or
// more are present, such as in the case of: (530) 583-6985 x302/x2303. The
// second extension here makes this actually two phone numbers, (530) 583-6985
// x302 and (530) 583-6985 x2303. We remove the second extension so that the
// first number is parsed correctly.
void PhoneNumberUtil::ExtractPossibleNumber(const string& number,
                                            string* extracted_number) const {
  DCHECK(extracted_number);

  UnicodeText number_as_unicode;
  number_as_unicode.PointToUTF8(number.data(), number.size());
  char current_char[5];
  int len;
  UnicodeText::const_iterator it;
  for (it = number_as_unicode.begin(); it != number_as_unicode.end(); ++it) {
    len = it.get_utf8(current_char);
    current_char[len] = '\0';
    if (reg_exps_->valid_start_char_pattern_->FullMatch(current_char)) {
      break;
    }
  }

  if (it == number_as_unicode.end()) {
    // No valid start character was found. extracted_number should be set to
    // empty string.
    extracted_number->assign("");
    return;
  }

  extracted_number->assign(
      UnicodeText::UTF8Substring(it, number_as_unicode.end()));
  TrimUnwantedEndChars(extracted_number);
  if (extracted_number->length() == 0) {
    return;
  }

  VLOG(3) << "After stripping starting and trailing characters, left with: "
          << *extracted_number;

  // Now remove any extra numbers at the end.
  reg_exps_->capture_up_to_second_number_start_pattern_->
      PartialMatch(*extracted_number, extracted_number);
}

bool PhoneNumberUtil::IsPossibleNumber(const PhoneNumber& number) const {
  return IsPossibleNumberWithReason(number) == IS_POSSIBLE;
}

bool PhoneNumberUtil::IsPossibleNumberForString(
    const string& number,
    const string& region_dialing_from) const {
  PhoneNumber number_proto;
  if (Parse(number, region_dialing_from, &number_proto) == NO_PARSING_ERROR) {
    return IsPossibleNumber(number_proto);
  } else {
    return false;
  }
}

PhoneNumberUtil::ValidationResult PhoneNumberUtil::IsPossibleNumberWithReason(
    const PhoneNumber& number) const {
  string national_number;
  GetNationalSignificantNumber(number, &national_number);
  int country_code = number.country_code();
  // Note: For Russian Fed and NANPA numbers, we just use the rules from the
  // default region (US or Russia) since the GetRegionCodeForNumber will not
  // work if the number is possible but not valid. This would need to be
  // revisited if the possible number pattern ever differed between various
  // regions within those plans.
  if (!HasValidCountryCallingCode(country_code)) {
    return INVALID_COUNTRY_CODE;
  }
  string region_code;
  GetRegionCodeForCountryCode(country_code, &region_code);
  const PhoneMetadata* metadata =
      GetMetadataForRegionOrCallingCode(country_code, region_code);
  const PhoneNumberDesc& general_num_desc = metadata->general_desc();
  // Handling case of numbers with no metadata.
  if (!general_num_desc.has_national_number_pattern()) {
    size_t number_length = national_number.length();
    if (number_length < kMinLengthForNsn) {
      return TOO_SHORT;
    } else if (number_length > kMaxLengthForNsn) {
      return TOO_LONG;
    } else {
      return IS_POSSIBLE;
    }
  }
  const RegExp& possible_number_pattern = reg_exps_->regexp_cache_->GetRegExp(
      StrCat("(", general_num_desc.possible_number_pattern(), ")"));
  return TestNumberLengthAgainstPattern(possible_number_pattern,
                                        national_number);
}

bool PhoneNumberUtil::TruncateTooLongNumber(PhoneNumber* number) const {
  if (IsValidNumber(*number)) {
    return true;
  }
  PhoneNumber number_copy(*number);
  uint64 national_number = number->national_number();
  do {
    national_number /= 10;
    number_copy.set_national_number(national_number);
    if (IsPossibleNumberWithReason(number_copy) == TOO_SHORT ||
        national_number == 0) {
      return false;
    }
  } while (!IsValidNumber(number_copy));
  number->set_national_number(national_number);
  return true;
}

PhoneNumberUtil::PhoneNumberType PhoneNumberUtil::GetNumberType(
    const PhoneNumber& number) const {
  string region_code;
  GetRegionCodeForNumber(number, &region_code);
  if (!IsValidRegionCode(region_code) &&
      kRegionCodeForNonGeoEntity != region_code) {
    return UNKNOWN;
  }
  string national_significant_number;
  GetNationalSignificantNumber(number, &national_significant_number);
  const PhoneMetadata* metadata =
      GetMetadataForRegionOrCallingCode(number.country_code(), region_code);
  return GetNumberTypeHelper(national_significant_number,
                             *metadata,
                             reg_exps_->regexp_cache_.get());
}

bool PhoneNumberUtil::IsValidNumber(const PhoneNumber& number) const {
  string region_code;
  GetRegionCodeForNumber(number, &region_code);
  return IsValidNumberForRegion(number, region_code);
}

bool PhoneNumberUtil::IsValidNumberForRegion(const PhoneNumber& number,
                                             const string& region_code) const {
  int country_code = number.country_code();
  const PhoneMetadata* metadata =
      GetMetadataForRegionOrCallingCode(country_code, region_code);
  if (!metadata ||
      ((kRegionCodeForNonGeoEntity != region_code) &&
       country_code != GetCountryCodeForValidRegion(region_code))) {
    // Either the region code was invalid, or the country calling code for this
    // number does not match that of the region code.
    return false;
  }
  const PhoneNumberDesc& general_desc = metadata->general_desc();
  string national_number;
  GetNationalSignificantNumber(number, &national_number);

  // For regions where we don't have metadata for PhoneNumberDesc, we treat
  // any number passed in as a valid number if its national significant number
  // is between the minimum and maximum lengths defined by ITU for a national
  // significant number.
  if (!general_desc.has_national_number_pattern()) {
    VLOG(3) << "Validating number with incomplete metadata.";
    size_t number_length = national_number.length();
    return number_length > kMinLengthForNsn &&
        number_length <= kMaxLengthForNsn;
  }
  return GetNumberTypeHelper(national_number, *metadata,
                             reg_exps_->regexp_cache_.get()) != UNKNOWN;
}

bool PhoneNumberUtil::IsLeadingZeroPossible(int country_calling_code) const {
  string region_code;
  GetRegionCodeForCountryCode(country_calling_code, &region_code);
  const PhoneMetadata* main_metadata_for_calling_code = GetMetadataForRegion(
      region_code);
  if (!main_metadata_for_calling_code) return false;
  return main_metadata_for_calling_code->leading_zero_possible();
}

void PhoneNumberUtil::GetNationalSignificantNumber(
    const PhoneNumber& number,
    string* national_number) const {
  DCHECK(national_number);
  // If a leading zero has been set, we prefix this now. Note this is not a
  // national prefix.
  StrAppend(national_number, number.italian_leading_zero() ? "0" : "");
  StrAppend(national_number, number.national_number());
}

int PhoneNumberUtil::GetLengthOfGeographicalAreaCode(
    const PhoneNumber& number) const {
  string region_code;
  GetRegionCodeForNumber(number, &region_code);
  if (!IsValidRegionCode(region_code)) {
    return 0;
  }
  const PhoneMetadata* metadata = GetMetadataForRegion(region_code);
  DCHECK(metadata);
  // If a country doesn't use a national prefix, and this number doesn't have an
  // Italian leading zero, we assume it is a closed dialling plan with no area
  // codes.
  if (!metadata->has_national_prefix() && !number.italian_leading_zero()) {
    return 0;
  }

  string national_significant_number;
  GetNationalSignificantNumber(number, &national_significant_number);
  PhoneNumberType type = GetNumberTypeHelper(national_significant_number,
                                             *metadata,
                                             reg_exps_->regexp_cache_.get());
  // Most numbers other than the two types below have to be dialled in full.
  if (type != FIXED_LINE && type != FIXED_LINE_OR_MOBILE) {
    return 0;
  }

  return GetLengthOfNationalDestinationCode(number);
}

int PhoneNumberUtil::GetLengthOfNationalDestinationCode(
    const PhoneNumber& number) const {
  PhoneNumber copied_proto(number);
  if (number.has_extension()) {
    // Clear the extension so it's not included when formatting.
    copied_proto.clear_extension();
  }

  string formatted_number;
  Format(copied_proto, INTERNATIONAL, &formatted_number);
  const scoped_ptr<RegExpInput> i18n_number(
      reg_exps_->regexp_factory_->CreateInput(formatted_number));
  string digit_group;
  string ndc;
  string third_group;
  for (int i = 0; i < 3; ++i) {
    if (!reg_exps_->capturing_ascii_digits_pattern_->FindAndConsume(
            i18n_number.get(), &digit_group)) {
      // We should find at least three groups.
      return 0;
    }
    if (i == 1) {
      ndc = digit_group;
    } else if (i == 2) {
      third_group = digit_group;
    }
  }
  string region_code;
  GetRegionCodeForCountryCode(number.country_code(), &region_code);
  if (region_code == "AR" &&
      GetNumberType(number) == MOBILE) {
    // Argentinian mobile numbers, when formatted in the international format,
    // are in the form of +54 9 NDC XXXX.... As a result, we take the length of
    // the third group (NDC) and add 1 for the digit 9, which also forms part of
    // the national significant number.
    return third_group.size() + 1;
  }
  return ndc.size();
}

void PhoneNumberUtil::NormalizeDigitsOnly(string* number) const {
  DCHECK(number);
  const RegExp& non_digits_pattern = reg_exps_->regexp_cache_->GetRegExp(
      StrCat("[^", kDigits, "]"));
  // Delete everything that isn't valid digits.
  non_digits_pattern.GlobalReplace(number, "");
  // Normalize all decimal digits to ASCII digits.
  number->assign(NormalizeUTF8::NormalizeDecimalDigits(*number));
}

bool PhoneNumberUtil::IsAlphaNumber(const string& number) const {
  if (!IsViablePhoneNumber(number)) {
    // Number is too short, or doesn't match the basic phone number pattern.
    return false;
  }
  // Copy the number, since we are going to try and strip the extension from it.
  string number_copy(number);
  string extension;
  MaybeStripExtension(&number_copy, &extension);
  return reg_exps_->valid_alpha_phone_pattern_->FullMatch(number_copy);
}

void PhoneNumberUtil::ConvertAlphaCharactersInNumber(string* number) const {
  DCHECK(number);
  NormalizeHelper(reg_exps_->alpha_phone_mappings_, false, number);
}

// Normalizes a string of characters representing a phone number. This performs
// the following conversions:
//   - Punctuation is stripped.
//   For ALPHA/VANITY numbers:
//   - Letters are converted to their numeric representation on a telephone
//     keypad. The keypad used here is the one defined in ITU Recommendation
//     E.161. This is only done if there are 3 or more letters in the number, to
//     lessen the risk that such letters are typos.
//   For other numbers:
//   - Wide-ascii digits are converted to normal ASCII (European) digits.
//   - Arabic-Indic numerals are converted to European numerals.
//   - Spurious alpha characters are stripped.
void PhoneNumberUtil::Normalize(string* number) const {
  DCHECK(number);
  if (reg_exps_->valid_alpha_phone_pattern_->PartialMatch(*number)) {
    NormalizeHelper(reg_exps_->alpha_phone_mappings_, true, number);
  }
  NormalizeDigitsOnly(number);
}

// Checks to see if the string of characters could possibly be a phone number at
// all. At the moment, checks to see that the string begins with at least 3
// digits, ignoring any punctuation commonly found in phone numbers.  This
// method does not require the number to be normalized in advance - but does
// assume that leading non-number symbols have been removed, such as by the
// method ExtractPossibleNumber.
bool PhoneNumberUtil::IsViablePhoneNumber(const string& number) const {
  if (number.length() < kMinLengthForNsn) {
    VLOG(2) << "Number too short to be viable:" << number;
    return false;
  }
  return reg_exps_->valid_phone_number_pattern_->FullMatch(number);
}

// Strips the IDD from the start of the number if present. Helper function used
// by MaybeStripInternationalPrefixAndNormalize.
bool PhoneNumberUtil::ParsePrefixAsIdd(const RegExp& idd_pattern,
                                       string* number) const {
  DCHECK(number);
  const scoped_ptr<RegExpInput> number_copy(
      reg_exps_->regexp_factory_->CreateInput(*number));
  // First attempt to strip the idd_pattern at the start, if present. We make a
  // copy so that we can revert to the original string if necessary.
  if (idd_pattern.Consume(number_copy.get())) {
    // Only strip this if the first digit after the match is not a 0, since
    // country calling codes cannot begin with 0.
    string extracted_digit;
    if (reg_exps_->capturing_digit_pattern_->PartialMatch(
            number_copy->ToString(), &extracted_digit)) {
      NormalizeDigitsOnly(&extracted_digit);
      if (extracted_digit == "0") {
        return false;
      }
    }
    number->assign(number_copy->ToString());
    return true;
  }
  return false;
}

// Strips any international prefix (such as +, 00, 011) present in the number
// provided, normalizes the resulting number, and indicates if an international
// prefix was present.
//
// possible_idd_prefix represents the international direct dialing prefix from
// the region we think this number may be dialed in.
// Returns true if an international dialing prefix could be removed from the
// number, otherwise false if the number did not seem to be in international
// format.
PhoneNumber::CountryCodeSource
PhoneNumberUtil::MaybeStripInternationalPrefixAndNormalize(
    const string& possible_idd_prefix,
    string* number) const {
  DCHECK(number);
  if (number->empty()) {
    return PhoneNumber::FROM_DEFAULT_COUNTRY;
  }
  const scoped_ptr<RegExpInput> number_string_piece(
      reg_exps_->regexp_factory_->CreateInput(*number));
  if (reg_exps_->plus_chars_pattern_->Consume(number_string_piece.get())) {
    number->assign(number_string_piece->ToString());
    // Can now normalize the rest of the number since we've consumed the "+"
    // sign at the start.
    Normalize(number);
    return PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN;
  }
  // Attempt to parse the first digits as an international prefix.
  const RegExp& idd_pattern =
      reg_exps_->regexp_cache_->GetRegExp(possible_idd_prefix);
  Normalize(number);
  return ParsePrefixAsIdd(idd_pattern, number)
      ? PhoneNumber::FROM_NUMBER_WITH_IDD
      : PhoneNumber::FROM_DEFAULT_COUNTRY;
}

// Strips any national prefix (such as 0, 1) present in the number provided.
// The number passed in should be the normalized telephone number that we wish
// to strip any national dialing prefix from. The metadata should be for the
// region that we think this number is from. Returns true if a national prefix
// and/or carrier code was stripped.
bool PhoneNumberUtil::MaybeStripNationalPrefixAndCarrierCode(
    const PhoneMetadata& metadata,
    string* number,
    string* carrier_code) const {
  DCHECK(number);
  string carrier_code_temp;
  const string& possible_national_prefix =
      metadata.national_prefix_for_parsing();
  if (number->empty() || possible_national_prefix.empty()) {
    // Early return for numbers of zero length or with no national prefix
    // possible.
    return false;
  }
  // We use two copies here since Consume modifies the phone number, and if the
  // first if-clause fails the number will already be changed.
  const scoped_ptr<RegExpInput> number_copy(
      reg_exps_->regexp_factory_->CreateInput(*number));
  const scoped_ptr<RegExpInput> number_copy_without_transform(
      reg_exps_->regexp_factory_->CreateInput(*number));
  string number_string_copy(*number);
  string captured_part_of_prefix;
  const RegExp& national_number_rule = reg_exps_->regexp_cache_->GetRegExp(
      metadata.general_desc().national_number_pattern());
  // Check if the original number is viable.
  bool is_viable_original_number = national_number_rule.FullMatch(*number);
  // Attempt to parse the first digits as a national prefix. We make a
  // copy so that we can revert to the original string if necessary.
  const string& transform_rule = metadata.national_prefix_transform_rule();
  const RegExp& possible_national_prefix_pattern =
      reg_exps_->regexp_cache_->GetRegExp(possible_national_prefix);
  if (!transform_rule.empty() &&
      (possible_national_prefix_pattern.Consume(
          number_copy.get(), &carrier_code_temp, &captured_part_of_prefix) ||
       possible_national_prefix_pattern.Consume(
           number_copy.get(), &captured_part_of_prefix)) &&
      !captured_part_of_prefix.empty()) {
    // If this succeeded, then we must have had a transform rule and there must
    // have been some part of the prefix that we captured.
    // We make the transformation and check that the resultant number is still
    // viable. If so, replace the number and return.
    possible_national_prefix_pattern.Replace(&number_string_copy,
                                             transform_rule);
    if (is_viable_original_number &&
        !national_number_rule.FullMatch(number_string_copy)) {
      return false;
    }
    number->assign(number_string_copy);
    if (carrier_code) {
      carrier_code->assign(carrier_code_temp);
    }
  } else if (possible_national_prefix_pattern.Consume(
                 number_copy_without_transform.get(), &carrier_code_temp) ||
             possible_national_prefix_pattern.Consume(
                 number_copy_without_transform.get())) {
    VLOG(4) << "Parsed the first digits as a national prefix.";
    // If captured_part_of_prefix is empty, this implies nothing was captured by
    // the capturing groups in possible_national_prefix; therefore, no
    // transformation is necessary, and we just remove the national prefix.
    const string number_copy_as_string =
        number_copy_without_transform->ToString();
    if (is_viable_original_number &&
        !national_number_rule.FullMatch(number_copy_as_string)) {
      return false;
    }
    number->assign(number_copy_as_string);
    if (carrier_code) {
      carrier_code->assign(carrier_code_temp);
    }
  } else {
    return false;
    VLOG(4) << "The first digits did not match the national prefix.";
  }
  return true;
}

// Strips any extension (as in, the part of the number dialled after the call is
// connected, usually indicated with extn, ext, x or similar) from the end of
// the number, and returns it. The number passed in should be non-normalized.
bool PhoneNumberUtil::MaybeStripExtension(string* number, string* extension)
    const {
  DCHECK(number);
  DCHECK(extension);
  // There are three extension capturing groups in the regular expression.
  string possible_extension_one;
  string possible_extension_two;
  string possible_extension_three;
  string number_copy(*number);
  const scoped_ptr<RegExpInput> number_copy_as_regexp_input(
      reg_exps_->regexp_factory_->CreateInput(number_copy));
  if (reg_exps_->extn_pattern_->Consume(number_copy_as_regexp_input.get(),
                            false,
                            &possible_extension_one,
                            &possible_extension_two,
                            &possible_extension_three)) {
    // Replace the extensions in the original string here.
    reg_exps_->extn_pattern_->Replace(&number_copy, "");
    VLOG(4) << "Found an extension. Possible extension one: "
            << possible_extension_one
            << ". Possible extension two: " << possible_extension_two
            << ". Possible extension three: " << possible_extension_three
            << ". Remaining number: " << number_copy;
    // If we find a potential extension, and the number preceding this is a
    // viable number, we assume it is an extension.
    if ((!possible_extension_one.empty() || !possible_extension_two.empty() ||
         !possible_extension_three.empty()) &&
        IsViablePhoneNumber(number_copy)) {
      number->assign(number_copy);
      if (!possible_extension_one.empty()) {
        extension->assign(possible_extension_one);
      } else if (!possible_extension_two.empty()) {
        extension->assign(possible_extension_two);
      } else if (!possible_extension_three.empty()) {
        extension->assign(possible_extension_three);
      }
      return true;
    }
  }
  return false;
}

// Extracts country calling code from national_number, and returns it. It
// assumes that the leading plus sign or IDD has already been removed. Returns 0
// if national_number doesn't start with a valid country calling code, and
// leaves national_number unmodified. Assumes the national_number is at least 3
// characters long.
int PhoneNumberUtil::ExtractCountryCode(string* national_number) const {
  int potential_country_code;
  if (national_number->empty() || (national_number->at(0) == '0')) {
    // Country codes do not begin with a '0'.
    return 0;
  }
  for (size_t i = 1; i <= kMaxLengthCountryCode; ++i) {
    safe_strto32(national_number->substr(0, i), &potential_country_code);
    string region_code;
    GetRegionCodeForCountryCode(potential_country_code, &region_code);
    if (region_code != RegionCode::GetUnknown()) {
      national_number->erase(0, i);
      return potential_country_code;
    }
  }
  return 0;
}

// Tries to extract a country calling code from a number. Country calling codes
// are extracted in the following ways:
//   - by stripping the international dialing prefix of the region the person
//   is dialing from, if this is present in the number, and looking at the next
//   digits
//   - by stripping the '+' sign if present and then looking at the next digits
//   - by comparing the start of the number and the country calling code of the
//   default region. If the number is not considered possible for the numbering
//   plan of the default region initially, but starts with the country calling
//   code of this region, validation will be reattempted after stripping this
//   country calling code. If this number is considered a possible number, then
//   the first digits will be considered the country calling code and removed as
//   such.
//
//   Returns NO_PARSING_ERROR if a country calling code was successfully
//   extracted or none was present, or the appropriate error otherwise, such as
//   if a + was present but it was not followed by a valid country calling code.
//   If NO_PARSING_ERROR is returned, the national_number without the country
//   calling code is populated, and the country_code of the phone_number passed
//   in is set to the country calling code if found, otherwise to 0.
PhoneNumberUtil::ErrorType PhoneNumberUtil::MaybeExtractCountryCode(
    const PhoneMetadata* default_region_metadata,
    bool keep_raw_input,
    string* national_number,
    PhoneNumber* phone_number) const {
  DCHECK(national_number);
  DCHECK(phone_number);
  // Set the default prefix to be something that will never match if there is no
  // default region.
  string possible_country_idd_prefix = default_region_metadata
      ?  default_region_metadata->international_prefix()
      : "NonMatch";
  PhoneNumber::CountryCodeSource country_code_source =
      MaybeStripInternationalPrefixAndNormalize(possible_country_idd_prefix,
                                                national_number);
  if (keep_raw_input) {
    phone_number->set_country_code_source(country_code_source);
  }
  if (country_code_source != PhoneNumber::FROM_DEFAULT_COUNTRY) {
    if (national_number->length() < kMinLengthForNsn) {
      VLOG(2) << "Phone number had an IDD, but after this was not "
              << "long enough to be a viable phone number.";
      return TOO_SHORT_AFTER_IDD;
    }
    int potential_country_code = ExtractCountryCode(national_number);
    if (potential_country_code != 0) {
      phone_number->set_country_code(potential_country_code);
      return NO_PARSING_ERROR;
    }
    // If this fails, they must be using a strange country calling code that we
    // don't recognize, or that doesn't exist.
    return INVALID_COUNTRY_CODE_ERROR;
  } else if (default_region_metadata) {
    // Check to see if the number starts with the country calling code for the
    // default region. If so, we remove the country calling code, and do some
    // checks on the validity of the number before and after.
    int default_country_code = default_region_metadata->country_code();
    string default_country_code_string(SimpleItoa(default_country_code));
    VLOG(4) << "Possible country calling code: " << default_country_code_string;
    string potential_national_number;
    if (TryStripPrefixString(*national_number,
                             default_country_code_string,
                             &potential_national_number)) {
      const PhoneNumberDesc& general_num_desc =
          default_region_metadata->general_desc();
      const RegExp& valid_number_pattern =
          reg_exps_->regexp_cache_->GetRegExp(
              general_num_desc.national_number_pattern());
      MaybeStripNationalPrefixAndCarrierCode(*default_region_metadata,
                                             &potential_national_number,
                                             NULL);
      VLOG(4) << "Number without country calling code prefix: "
              << potential_national_number;
      const RegExp& possible_number_pattern =
          reg_exps_->regexp_cache_->GetRegExp(
              StrCat("(", general_num_desc.possible_number_pattern(), ")"));
      // If the number was not valid before but is valid now, or if it was too
      // long before, we consider the number with the country code stripped to
      // be a better result and keep that instead.
      if ((!valid_number_pattern.FullMatch(*national_number) &&
           valid_number_pattern.FullMatch(potential_national_number)) ||
           TestNumberLengthAgainstPattern(possible_number_pattern,
                                          *national_number) == TOO_LONG) {
        national_number->assign(potential_national_number);
        if (keep_raw_input) {
          phone_number->set_country_code_source(
              PhoneNumber::FROM_NUMBER_WITHOUT_PLUS_SIGN);
        }
        phone_number->set_country_code(default_country_code);
        return NO_PARSING_ERROR;
      }
    }
  }
  // No country calling code present. Set the country_code to 0.
  phone_number->set_country_code(0);
  return NO_PARSING_ERROR;
}

PhoneNumberUtil::MatchType PhoneNumberUtil::IsNumberMatch(
    const PhoneNumber& first_number_in,
    const PhoneNumber& second_number_in) const {
  // Make copies of the phone number so that the numbers passed in are not
  // edited.
  PhoneNumber first_number(first_number_in);
  PhoneNumber second_number(second_number_in);
  // First clear raw_input and country_code_source and
  // preferred_domestic_carrier_code fields and any empty-string extensions so
  // that we can use the proto-buffer equality method.
  first_number.clear_raw_input();
  first_number.clear_country_code_source();
  first_number.clear_preferred_domestic_carrier_code();
  second_number.clear_raw_input();
  second_number.clear_country_code_source();
  second_number.clear_preferred_domestic_carrier_code();
  if (first_number.extension().empty()) {
    first_number.clear_extension();
  }
  if (second_number.extension().empty()) {
    second_number.clear_extension();
  }
  // Early exit if both had extensions and these are different.
  if (first_number.has_extension() && second_number.has_extension() &&
      first_number.extension() != second_number.extension()) {
    return NO_MATCH;
  }
  int first_number_country_code = first_number.country_code();
  int second_number_country_code = second_number.country_code();
  // Both had country calling code specified.
  if (first_number_country_code != 0 && second_number_country_code != 0) {
    if (ExactlySameAs(first_number, second_number)) {
      return EXACT_MATCH;
    } else if (first_number_country_code == second_number_country_code &&
               IsNationalNumberSuffixOfTheOther(first_number, second_number)) {
      // A SHORT_NSN_MATCH occurs if there is a difference because of the
      // presence or absence of an 'Italian leading zero', the presence or
      // absence of an extension, or one NSN being a shorter variant of the
      // other.
      return SHORT_NSN_MATCH;
    }
    // This is not a match.
    return NO_MATCH;
  }
  // Checks cases where one or both country calling codes were not specified. To
  // make equality checks easier, we first set the country_code fields to be
  // equal.
  first_number.set_country_code(second_number_country_code);
  // If all else was the same, then this is an NSN_MATCH.
  if (ExactlySameAs(first_number, second_number)) {
    return NSN_MATCH;
  }
  if (IsNationalNumberSuffixOfTheOther(first_number, second_number)) {
    return SHORT_NSN_MATCH;
  }
  return NO_MATCH;
}

PhoneNumberUtil::MatchType PhoneNumberUtil::IsNumberMatchWithTwoStrings(
    const string& first_number,
    const string& second_number) const {
  PhoneNumber first_number_as_proto;
  ErrorType error_type =
      Parse(first_number, RegionCode::GetUnknown(), &first_number_as_proto);
  if (error_type == NO_PARSING_ERROR) {
    return IsNumberMatchWithOneString(first_number_as_proto, second_number);
  }
  if (error_type == INVALID_COUNTRY_CODE_ERROR) {
    PhoneNumber second_number_as_proto;
    ErrorType error_type = Parse(second_number, RegionCode::GetUnknown(),
                                 &second_number_as_proto);
    if (error_type == NO_PARSING_ERROR) {
      return IsNumberMatchWithOneString(second_number_as_proto, first_number);
    }
    if (error_type == INVALID_COUNTRY_CODE_ERROR) {
      error_type  = ParseHelper(first_number, RegionCode::GetUnknown(), false,
                                false, &first_number_as_proto);
      if (error_type == NO_PARSING_ERROR) {
        error_type = ParseHelper(second_number, RegionCode::GetUnknown(), false,
                                 false, &second_number_as_proto);
        if (error_type == NO_PARSING_ERROR) {
          return IsNumberMatch(first_number_as_proto, second_number_as_proto);
        }
      }
    }
  }
  // One or more of the phone numbers we are trying to match is not a viable
  // phone number.
  return INVALID_NUMBER;
}

PhoneNumberUtil::MatchType PhoneNumberUtil::IsNumberMatchWithOneString(
    const PhoneNumber& first_number,
    const string& second_number) const {
  // First see if the second number has an implicit country calling code, by
  // attempting to parse it.
  PhoneNumber second_number_as_proto;
  ErrorType error_type =
      Parse(second_number, RegionCode::GetUnknown(), &second_number_as_proto);
  if (error_type == NO_PARSING_ERROR) {
    return IsNumberMatch(first_number, second_number_as_proto);
  }
  if (error_type == INVALID_COUNTRY_CODE_ERROR) {
    // The second number has no country calling code. EXACT_MATCH is no longer
    // possible.  We parse it as if the region was the same as that for the
    // first number, and if EXACT_MATCH is returned, we replace this with
    // NSN_MATCH.
    string first_number_region;
    GetRegionCodeForCountryCode(first_number.country_code(),
                                &first_number_region);
    if (first_number_region != RegionCode::GetUnknown()) {
      PhoneNumber second_number_with_first_number_region;
      Parse(second_number, first_number_region,
            &second_number_with_first_number_region);
      MatchType match = IsNumberMatch(first_number,
                                      second_number_with_first_number_region);
      if (match == EXACT_MATCH) {
        return NSN_MATCH;
      }
      return match;
    } else {
      // If the first number didn't have a valid country calling code, then we
      // parse the second number without one as well.
      error_type = ParseHelper(second_number, RegionCode::GetUnknown(), false,
                               false, &second_number_as_proto);
      if (error_type == NO_PARSING_ERROR) {
        return IsNumberMatch(first_number, second_number_as_proto);
      }
    }
  }
  // One or more of the phone numbers we are trying to match is not a viable
  // phone number.
  return INVALID_NUMBER;
}

AsYouTypeFormatter* PhoneNumberUtil::GetAsYouTypeFormatter(
    const string& region_code) const {
  return new AsYouTypeFormatter(region_code);
}

bool PhoneNumberUtil::CanBeInternationallyDialled(
    const PhoneNumber& number) const {
  string region_code;
  GetRegionCodeForNumber(number, &region_code);
  if (!IsValidRegionCode(region_code)) {
    // Note numbers belonging to non-geographical entities (e.g. +800 numbers)
    // are always internationally diallable, and will be caught here.
    return true;
  }
  const PhoneMetadata* metadata = GetMetadataForRegion(region_code);
  string national_significant_number;
  GetNationalSignificantNumber(number, &national_significant_number);
  return !IsNumberMatchingDesc(
      national_significant_number, metadata->no_international_dialling(),
      reg_exps_->regexp_cache_.get());
}

}  // namespace phonenumbers
}  // namespace i18n
