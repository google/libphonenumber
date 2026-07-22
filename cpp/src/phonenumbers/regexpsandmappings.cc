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

#include "phonenumbers/regexpsandmappings.h"

#include "phonenumbers/constants.h"
#include "phonenumbers/encoding_utils.h"

namespace i18n {
namespace phonenumbers {

char32 ToUnicodeCodepoint(const char* unicode_char) {
  char32 codepoint;
  EncodingUtils::DecodeUTF8Char(unicode_char, &codepoint);
  return codepoint;
}

std::string PhoneNumberRegExpsAndMappings::CreateExtnPattern(bool for_parsing) {
  // We cap the maximum length of an extension based on the ambiguity of the
  // way the extension is prefixed. As per ITU, the officially allowed
  // length for extensions is actually 40, but we don't support this since we
  // haven't seen real examples and this introduces many false interpretations
  // as the extension labels are not standardized.
  int ext_limit_after_explicit_label = 20;
  int ext_limit_after_likely_label = 15;
  int ext_limit_after_ambiguous_char = 9;
  int ext_limit_when_not_sure = 6;

  // Canonical-equivalence doesn't seem to be an option with RE2, so we allow
  // two options for representing any non-ASCII character like ó - the character
  // itself, and one in the unicode decomposed form with the combining acute
  // accent.

  // Here the extension is called out in a more explicit way, i.e mentioning it
  // obvious patterns like "ext.".
  string explicit_ext_labels =
      "(?:e?xt(?:ensi(?:o\xCC\x81?|\xC3\xB3))?n?|(?:\xEF\xBD\x85)?"
      "\xEF\xBD\x98\xEF\xBD\x94(?:\xEF\xBD\x8E)?|\xD0\xB4\xD0\xBE\xD0\xB1|"
      "anexo)";
  // One-character symbols that can be used to indicate an extension, and less
  // commonly used or more ambiguous extension labels.
  string ambiguous_ext_labels =
      "(?:[x\xEF\xBD\x98#\xEF\xBC\x83~\xEF\xBD\x9E]|int|"
      "\xEF\xBD\x89\xEF\xBD\x8E\xEF\xBD\x94)";
  // When extension is not separated clearly.
  string ambiguous_separator = "[- ]+";

  string rfc_extn = StrCat(Constants::kRfc3966ExtnPrefix,
                           ExtnDigits(ext_limit_after_explicit_label));
  string explicit_extn =
      StrCat(Constants::kPossibleSeparatorsBetweenNumberAndExtLabel,
             explicit_ext_labels, Constants::kPossibleCharsAfterExtLabel,
             ExtnDigits(ext_limit_after_explicit_label),
             Constants::kOptionalExtSuffix);
  string ambiguous_extn =
      StrCat(Constants::kPossibleSeparatorsBetweenNumberAndExtLabel,
             ambiguous_ext_labels, Constants::kPossibleCharsAfterExtLabel,
             ExtnDigits(ext_limit_after_ambiguous_char),
             Constants::kOptionalExtSuffix);
  string american_style_extn_with_suffix =
      StrCat(ambiguous_separator, ExtnDigits(ext_limit_when_not_sure), "#");

  // The first regular expression covers RFC 3966 format, where the extension is
  // added using ";ext=". The second more generic where extension is mentioned
  // with explicit labels like "ext:". In both the above cases we allow more
  // numbers in extension than any other extension labels. The third one
  // captures when single character extension labels or less commonly used
  // labels are present. In such cases we capture fewer extension digits in
  // order to reduce the chance of falsely interpreting two numbers beside each
  // other as a number + extension. The fourth one covers the special case of
  // American numbers where the extension is written with a hash at the end,
  // such as "- 503#".
  string extension_pattern =
      StrCat(rfc_extn, "|", explicit_extn, "|", ambiguous_extn, "|",
             american_style_extn_with_suffix);
  // Additional pattern that is supported when parsing extensions, not when
  // matching.
  if (for_parsing) {
    // ",," is commonly used for auto dialling the extension when connected.
    // Semi-colon works in Iphone and also in Android to pop up a button with
    // the extension number following.
    string auto_dialling_and_ext_labels_found = "(?:,{2}|;)";
    // This is same as kPossibleSeparatorsBetweenNumberAndExtLabel, but not
    // matching comma as extension label may have it.
    string possible_separators_number_extLabel_no_comma = "[ \xC2\xA0\\t]*";

    string auto_dialling_extn =
        StrCat(possible_separators_number_extLabel_no_comma,
               auto_dialling_and_ext_labels_found,
               Constants::kPossibleCharsAfterExtLabel,
               ExtnDigits(ext_limit_after_likely_label),
               Constants::kOptionalExtSuffix);
    string only_commas_extn =
        StrCat(possible_separators_number_extLabel_no_comma, "(?:,)+",
               Constants::kPossibleCharsAfterExtLabel,
               ExtnDigits(ext_limit_after_ambiguous_char),
               Constants::kOptionalExtSuffix);
    // Here the first pattern is exclusive for extension autodialling formats
    // which are used when dialling and in this case we accept longer
    // extensions. However, the second pattern is more liberal on number of
    // commas that acts as extension labels, so we have strict cap on number of
    // digits in such extensions.
    return StrCat(extension_pattern, "|", auto_dialling_extn, "|",
                  only_commas_extn);
  }
  return extension_pattern;
}

std::string PhoneNumberRegExpsAndMappings::ExtnDigits(int max_length) {
  return StrCat("([", Constants::kDigits, "]{1,", max_length, "})");
}

PhoneNumberRegExpsAndMappings::PhoneNumberRegExpsAndMappings()
    : valid_phone_number_(
          StrCat(Constants::kDigits, "{", Constants::kMinLengthForNsn, "}|[",
                 Constants::kPlusChars, "]*(?:[", Constants::kValidPunctuation,
                 Constants::kStarSign, "]*", Constants::kDigits, "){3,}[",
                 Constants::kValidPunctuation, Constants::kStarSign,
                 Constants::kValidAlpha, Constants::kDigits, "]*")),
      extn_patterns_for_parsing_(CreateExtnPattern(/* for_parsing= */ true)),
      rfc3966_phone_digit_(StrCat("(", Constants::kDigits, "|",
                                  Constants::kRfc3966VisualSeparator, ")")),
      alphanum_(
          StrCat(Constants::kValidAlphaInclUppercase, Constants::kDigits)),
      rfc3966_domainlabel_(
          StrCat("[", alphanum_, "]+((\\-)*[", alphanum_, "])*")),
      rfc3966_toplabel_(StrCat("[", Constants::kValidAlphaInclUppercase,
                               "]+((\\-)*[", alphanum_, "])*")),
      regexp_factory_(new RegExpFactory()),
      regexp_cache_(new RegExpCache(*regexp_factory_.get(), 128)),
      diallable_char_mappings_(),
      alpha_mappings_(),
      alpha_phone_mappings_(),
      all_plus_number_grouping_symbols_(),
      mobile_token_mappings_(),
      countries_without_national_prefix_with_area_codes_(),
      geo_mobile_countries_without_mobile_area_codes_(),
      geo_mobile_countries_(),
      single_international_prefix_(regexp_factory_->CreateRegExp(
          /* "[\\d]+(?:[~⁓∼～][\\d]+)?" */
          "[\\d]+(?:[~\xE2\x81\x93\xE2\x88\xBC\xEF\xBD\x9E][\\d]+)?")),
      digits_pattern_(
          regexp_factory_->CreateRegExp(StrCat("[", Constants::kDigits, "]*"))),
      capturing_digit_pattern_(regexp_factory_->CreateRegExp(
          StrCat("([", Constants::kDigits, "])"))),
      capturing_ascii_digits_pattern_(regexp_factory_->CreateRegExp("(\\d+)")),
      valid_start_char_pattern_(regexp_factory_->CreateRegExp(
          StrCat("[", Constants::kPlusChars, Constants::kDigits, "]"))),
      capture_up_to_second_number_start_pattern_(regexp_factory_->CreateRegExp(
          Constants::kCaptureUpToSecondNumberStart)),
      unwanted_end_char_pattern_(
          regexp_factory_->CreateRegExp("[^\\p{N}\\p{L}#]")),
      separator_pattern_(regexp_factory_->CreateRegExp(
          StrCat("[", Constants::kValidPunctuation, "]+"))),
      extn_patterns_for_matching_(CreateExtnPattern(/* for_parsing= */ false)),
      extn_pattern_(regexp_factory_->CreateRegExp(
          StrCat("(?i)(?:", extn_patterns_for_parsing_, ")$"))),
      valid_phone_number_pattern_(regexp_factory_->CreateRegExp(
          StrCat("(?i)", valid_phone_number_, "(?:", extn_patterns_for_parsing_,
                 ")?"))),
      valid_alpha_phone_pattern_(regexp_factory_->CreateRegExp(
          StrCat("(?i)(?:.*?[", Constants::kValidAlpha, "]){3}"))),
      // The first_group_capturing_pattern was originally set to $1 but there
      // are some countries for which the first group is not used in the
      // national pattern (e.g. Argentina) so the $1 group does not match
      // correctly. Therefore, we use \d, so that the first group actually
      // used in the pattern will be matched.
      first_group_capturing_pattern_(regexp_factory_->CreateRegExp("(\\$\\d)")),
      carrier_code_pattern_(regexp_factory_->CreateRegExp("\\$CC")),
      plus_chars_pattern_(regexp_factory_->CreateRegExp(
          StrCat("[", Constants::kPlusChars, "]+"))),
      rfc3966_global_number_digits_pattern_(regexp_factory_->CreateRegExp(
          StrCat("^\\", Constants::kPlusSign, rfc3966_phone_digit_, "*",
                 Constants::kDigits, rfc3966_phone_digit_, "*$"))),
      rfc3966_domainname_pattern_(regexp_factory_->CreateRegExp(StrCat(
          "^(", rfc3966_domainlabel_, "\\.)*", rfc3966_toplabel_, "\\.?$"))) {
  InitializeMapsAndSets();
}

void PhoneNumberRegExpsAndMappings::InitializeMapsAndSets() {
  diallable_char_mappings_.insert(std::make_pair('+', '+'));
  diallable_char_mappings_.insert(std::make_pair('*', '*'));
  diallable_char_mappings_.insert(std::make_pair('#', '#'));
  // Here we insert all punctuation symbols that we wish to respect when
  // formatting alpha numbers, as they show the intended number groupings.
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("-"), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xEF\xBC\x8D" /* "－" */), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE2\x80\x90" /* "‐" */), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE2\x80\x91" /* "‑" */), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE2\x80\x92" /* "‒" */), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE2\x80\x93" /* "–" */), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE2\x80\x94" /* "—" */), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE2\x80\x95" /* "―" */), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE2\x88\x92" /* "−" */), '-'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("/"), '/'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xEF\xBC\x8F" /* "／" */), '/'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint(" "), ' '));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE3\x80\x80" /* "　" */), ' '));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xE2\x81\xA0"), ' '));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("."), '.'));
  all_plus_number_grouping_symbols_.insert(
      std::make_pair(ToUnicodeCodepoint("\xEF\xBC\x8E" /* "．" */), '.'));
  // Only the upper-case letters are added here - the lower-case versions are
  // added programmatically.
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("A"), '2'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("B"), '2'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("C"), '2'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("D"), '3'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("E"), '3'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("F"), '3'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("G"), '4'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("H"), '4'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("I"), '4'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("J"), '5'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("K"), '5'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("L"), '5'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("M"), '6'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("N"), '6'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("O"), '6'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("P"), '7'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("Q"), '7'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("R"), '7'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("S"), '7'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("T"), '8'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("U"), '8'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("V"), '8'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("W"), '9'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("X"), '9'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("Y"), '9'));
  alpha_mappings_.insert(std::make_pair(ToUnicodeCodepoint("Z"), '9'));
  std::map<char32, char> lower_case_mappings;
  std::map<char32, char> alpha_letters;
  for (std::map<char32, char>::const_iterator it = alpha_mappings_.begin();
       it != alpha_mappings_.end(); ++it) {
    // Convert all the upper-case ASCII letters to lower-case.
    if (it->first < 128) {
      char letter_as_upper = static_cast<char>(it->first);
      char32 letter_as_lower = static_cast<char32>(tolower(letter_as_upper));
      lower_case_mappings.insert(std::make_pair(letter_as_lower, it->second));
      // Add the letters in both variants to the alpha_letters map. This just
      // pairs each letter with its upper-case representation so that it can
      // be retained when normalising alpha numbers.
      alpha_letters.insert(std::make_pair(letter_as_lower, letter_as_upper));
      alpha_letters.insert(std::make_pair(it->first, letter_as_upper));
    }
  }
  // In the Java version we don't insert the lower-case mappings in the map,
  // because we convert to upper case on the fly. Doing this here would
  // involve pulling in all of ICU, which we don't want to do if we don't have
  // to.
  alpha_mappings_.insert(lower_case_mappings.begin(),
                         lower_case_mappings.end());
  alpha_phone_mappings_.insert(alpha_mappings_.begin(), alpha_mappings_.end());
  all_plus_number_grouping_symbols_.insert(alpha_letters.begin(),
                                           alpha_letters.end());
  // Add the ASCII digits so that they don't get deleted by NormalizeHelper().
  for (char c = '0'; c <= '9'; ++c) {
    diallable_char_mappings_.insert(std::make_pair(c, c));
    alpha_phone_mappings_.insert(std::make_pair(c, c));
    all_plus_number_grouping_symbols_.insert(std::make_pair(c, c));
  }

  mobile_token_mappings_.insert(std::make_pair(54, '9'));
  countries_without_national_prefix_with_area_codes_.insert(52);  // Mexico
  geo_mobile_countries_without_mobile_area_codes_.insert(86);     // China
  geo_mobile_countries_.insert(52);                               // Mexico
  geo_mobile_countries_.insert(54);                               // Argentina
  geo_mobile_countries_.insert(55);                               // Brazil
  // Indonesia: some prefixes only (fixed CMDA wireless)
  geo_mobile_countries_.insert(62);
  geo_mobile_countries_.insert(
      geo_mobile_countries_without_mobile_area_codes_.begin(),
      geo_mobile_countries_without_mobile_area_codes_.end());
}

}  // namespace phonenumbers
}  // namespace i18n
