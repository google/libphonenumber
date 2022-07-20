// Copyright (C) 2011 The Libphonenumber Authors
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

#include "phonenumbers/asyoutypeformatter.h"

#include <math.h>
#include <cctype>
#include <list>
#include <string>

#include <google/protobuf/message_lite.h>

#include "phonenumbers/base/logging.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/regexp_cache.h"
#include "phonenumbers/regexp_factory.h"
#include "phonenumbers/stringutil.h"
#include "phonenumbers/unicodestring.h"


namespace i18n {
namespace phonenumbers {

using google::protobuf::RepeatedPtrField;

namespace {

const char kPlusSign = '+';

// This is the minimum length of national number accrued that is required to
// trigger the formatter. The first element of the leading_digits_pattern of
// each number_format contains a regular expression that matches up to this
// number of digits.
const size_t kMinLeadingDigitsLength = 3;

// The digits that have not been entered yet will be represented by a \u2008,
// the punctuation space.
const char kDigitPlaceholder[] = "\xE2\x80\x88"; /* " " */

// Character used when appropriate to separate a prefix, such as a long NDD or a
// country calling code, from the national number.
const char kSeparatorBeforeNationalNumber = ' ';

// A set of characters that, if found in a national prefix formatting rules, are
// an indicator to us that we should separate the national prefix from the
// number when formatting.
const char kNationalPrefixSeparatorsPattern[] = "[- ]";

// Matches all the groups contained in 'input' against 'pattern'.
void MatchAllGroups(const string& pattern,
                    const string& input,
                    const AbstractRegExpFactory& regexp_factory,
                    RegExpCache* cache,
                    string* group) {
  DCHECK(cache);
  DCHECK(group);
  string new_pattern(pattern);

  // Transforms pattern "(...)(...)(...)" to "(.........)".
  strrmm(&new_pattern, "()");
  new_pattern = StrCat("(", new_pattern, ")");

  const scoped_ptr<RegExpInput> consume_input(
      regexp_factory.CreateInput(input));
  bool status =
      cache->GetRegExp(new_pattern).Consume(consume_input.get(), group);
  DCHECK(status);
  IGNORE_UNUSED(status);
}

PhoneMetadata CreateEmptyMetadata() {
  PhoneMetadata metadata;
  metadata.set_international_prefix("NA");
  return metadata;
}

}  // namespace

AsYouTypeFormatter::AsYouTypeFormatter(const string& region_code)
    : regexp_factory_(new RegExpFactory()),
      regexp_cache_(*regexp_factory_.get(), 64),
      current_output_(),
      formatting_template_(),
      current_formatting_pattern_(),
      accrued_input_(),
      accrued_input_without_formatting_(),
      able_to_format_(true),
      input_has_formatting_(false),
      is_complete_number_(false),
      is_expecting_country_code_(false),
      phone_util_(*PhoneNumberUtil::GetInstance()),
      default_country_(region_code),
      empty_metadata_(CreateEmptyMetadata()),
      default_metadata_(GetMetadataForRegion(region_code)),
      current_metadata_(default_metadata_),
      last_match_position_(0),
      original_position_(0),
      position_to_remember_(0),
      prefix_before_national_number_(),
      should_add_space_after_national_prefix_(false),
      extracted_national_prefix_(),
      national_number_(),
      possible_formats_() {
}

// The metadata needed by this class is the same for all regions sharing the
// same country calling code. Therefore, we return the metadata for "main"
// region for this country calling code.
const PhoneMetadata* AsYouTypeFormatter::GetMetadataForRegion(
    const string& region_code) const {
  int country_calling_code = phone_util_.GetCountryCodeForRegion(region_code);
  string main_country;
  phone_util_.GetRegionCodeForCountryCode(country_calling_code, &main_country);
  const PhoneMetadata* const metadata =
      phone_util_.GetMetadataForRegion(main_country);
  if (metadata) {
    return metadata;
  }
  // Set to a default instance of the metadata. This allows us to function with
  // an incorrect region code, even if formatting only works for numbers
  // specified with "+".
  return &empty_metadata_;
}

bool AsYouTypeFormatter::MaybeCreateNewTemplate() {
  // When there are multiple available formats, the formatter uses the first
  // format where a formatting template could be created.
  for (list<const NumberFormat*>::const_iterator it = possible_formats_.begin();
       it != possible_formats_.end(); ++it) {
    DCHECK(*it);
    const NumberFormat& number_format = **it;
    const string& pattern = number_format.pattern();
    if (current_formatting_pattern_ == pattern) {
      return false;
    }
    if (CreateFormattingTemplate(number_format)) {
      current_formatting_pattern_ = pattern;
      SetShouldAddSpaceAfterNationalPrefix(number_format);
      // With a new formatting template, the matched position using the old
      // template needs to be reset.
      last_match_position_ = 0;
      return true;
    }
  }
  able_to_format_ = false;
  return false;
}

void AsYouTypeFormatter::GetAvailableFormats(const string& leading_digits) {
  // First decide whether we should use international or national number rules.
  bool is_international_number =
      is_complete_number_ && extracted_national_prefix_.empty();
  const RepeatedPtrField<NumberFormat>& format_list =
      (is_international_number &&
       current_metadata_->intl_number_format().size() > 0)
          ? current_metadata_->intl_number_format()
          : current_metadata_->number_format();
  for (RepeatedPtrField<NumberFormat>::const_iterator it = format_list.begin();
       it != format_list.end(); ++it) {
    // Discard a few formats that we know are not relevant based on the presence
    // of the national prefix.
    if (!extracted_national_prefix_.empty() &&
        phone_util_.FormattingRuleHasFirstGroupOnly(
            it->national_prefix_formatting_rule()) &&
        !it->national_prefix_optional_when_formatting() &&
        !it->has_domestic_carrier_code_formatting_rule()) {
      // If it is a national number that had a national prefix, any rules that
      // aren't valid with a national prefix should be excluded. A rule that has
      // a carrier-code formatting rule is kept since the national prefix might
      // actually be an extracted carrier code - we don't distinguish between
      // these when extracting it in the AYTF.
      continue;
    } else if (extracted_national_prefix_.empty() &&
               !is_complete_number_ &&
               !phone_util_.FormattingRuleHasFirstGroupOnly(
                    it->national_prefix_formatting_rule()) &&
               !it->national_prefix_optional_when_formatting()) {
      // This number was entered without a national prefix, and this formatting
      // rule requires one, so we discard it.
      continue;
    }
    if (phone_util_.IsFormatEligibleForAsYouTypeFormatter(it->format())) {
      possible_formats_.push_back(&*it);
    }
  }
  NarrowDownPossibleFormats(leading_digits);
}

void AsYouTypeFormatter::NarrowDownPossibleFormats(
    const string& leading_digits) {
  const int index_of_leading_digits_pattern =
      static_cast<int>(leading_digits.length() - kMinLeadingDigitsLength);

  for (list<const NumberFormat*>::iterator it = possible_formats_.begin();
       it != possible_formats_.end(); ) {
    DCHECK(*it);
    const NumberFormat& format = **it;
    if (format.leading_digits_pattern_size() == 0) {
      // Keep everything that isn't restricted by leading digits.
      ++it;
      continue;
    }
    // We don't use std::min because there is stange symbol conflict
    // with including <windows.h> and protobuf symbols
    int last_leading_digits_pattern = format.leading_digits_pattern_size() - 1;
    if (last_leading_digits_pattern > index_of_leading_digits_pattern)
      last_leading_digits_pattern = index_of_leading_digits_pattern;
    const scoped_ptr<RegExpInput> input(
        regexp_factory_->CreateInput(leading_digits));
    if (!regexp_cache_.GetRegExp(format.leading_digits_pattern().Get(
            last_leading_digits_pattern)).Consume(input.get())) {
      it = possible_formats_.erase(it);
      continue;
    }
    ++it;
  }
}

void AsYouTypeFormatter::SetShouldAddSpaceAfterNationalPrefix(
    const NumberFormat& format) {
  static const scoped_ptr<const RegExp> national_prefix_separators_pattern(
      regexp_factory_->CreateRegExp(kNationalPrefixSeparatorsPattern));
  should_add_space_after_national_prefix_ =
      national_prefix_separators_pattern->PartialMatch(
          format.national_prefix_formatting_rule());
}

bool AsYouTypeFormatter::CreateFormattingTemplate(const NumberFormat& format) {
  string number_pattern = format.pattern();
  string number_format = format.format();
  formatting_template_.remove();
  UnicodeString temp_template;
  GetFormattingTemplate(number_pattern, number_format, &temp_template);

  if (temp_template.length() > 0) {
    formatting_template_.append(temp_template);
    return true;
  }
  return false;
}

void AsYouTypeFormatter::GetFormattingTemplate(
    const string& number_pattern,
    const string& number_format,
    UnicodeString* formatting_template) {
  DCHECK(formatting_template);

  // Creates a phone number consisting only of the digit 9 that matches the
  // number_pattern by applying the pattern to the longest_phone_number string.
  static const char longest_phone_number[] = "999999999999999";
  string a_phone_number;

  MatchAllGroups(number_pattern, longest_phone_number, *regexp_factory_,
                 &regexp_cache_, &a_phone_number);
  // No formatting template can be created if the number of digits entered so
  // far is longer than the maximum the current formatting rule can accommodate.
  if (a_phone_number.length() < national_number_.length()) {
    formatting_template->remove();
    return;
  }
  // Formats the number according to number_format.
  regexp_cache_.GetRegExp(number_pattern).GlobalReplace(
      &a_phone_number, number_format);
  // Replaces each digit with character kDigitPlaceholder.
  GlobalReplaceSubstring("9", kDigitPlaceholder, &a_phone_number);
  formatting_template->setTo(a_phone_number.c_str(), a_phone_number.size());
}

void AsYouTypeFormatter::Clear() {
  current_output_.clear();
  accrued_input_.remove();
  accrued_input_without_formatting_.remove();
  formatting_template_.remove();
  last_match_position_ = 0;
  current_formatting_pattern_.clear();
  prefix_before_national_number_.clear();
  extracted_national_prefix_.clear();
  national_number_.clear();
  able_to_format_ = true;
  input_has_formatting_ = false;
  position_to_remember_ = 0;
  original_position_ = 0;
  is_complete_number_ = false;
  is_expecting_country_code_ = false;
  possible_formats_.clear();
  should_add_space_after_national_prefix_ = false;

  if (current_metadata_ != default_metadata_) {
    current_metadata_ = GetMetadataForRegion(default_country_);
  }
}

const string& AsYouTypeFormatter::InputDigit(char32 next_char, string* result) {
  DCHECK(result);

  InputDigitWithOptionToRememberPosition(next_char, false, &current_output_);
  result->assign(current_output_);
  return *result;
}

const string& AsYouTypeFormatter::InputDigitAndRememberPosition(
    char32 next_char,
    string* result) {
  DCHECK(result);

  InputDigitWithOptionToRememberPosition(next_char, true, &current_output_);
  result->assign(current_output_);
  return *result;
}

void AsYouTypeFormatter::InputDigitWithOptionToRememberPosition(
    char32 next_char,
    bool remember_position,
    string* phone_number) {
  DCHECK(phone_number);

  accrued_input_.append(next_char);
  if (remember_position) {
    original_position_ = accrued_input_.length();
  }
  // We do formatting on-the-fly only when each character entered is either a
  // plus sign (accepted at the start of the number only).
  string next_char_string;
  UnicodeString(next_char).toUTF8String(next_char_string);

  char normalized_next_char = '\0';
  if (!(phone_util_.ContainsOnlyValidDigits(next_char_string) ||
      (accrued_input_.length() == 1 && next_char == kPlusSign))) {
    able_to_format_ = false;
    input_has_formatting_ = true;
  } else {
    normalized_next_char =
        NormalizeAndAccrueDigitsAndPlusSign(next_char, remember_position);
  }
  if (!able_to_format_) {
    // When we are unable to format because of reasons other than that
    // formatting chars have been entered, it can be due to really long IDDs or
    // NDDs. If that is the case, we might be able to do formatting again after
    // extracting them.
    if (input_has_formatting_) {
      phone_number->clear();
      accrued_input_.toUTF8String(*phone_number);
    } else if (AttemptToExtractIdd()) {
      if (AttemptToExtractCountryCode()) {
        AttemptToChoosePatternWithPrefixExtracted(phone_number);
        return;
      }
    } else if (AbleToExtractLongerNdd()) {
      // Add an additional space to separate long NDD and national significant
      // number for readability. We don't set
      // should_add_space_after_national_prefix_ to true, since we don't want
      // this to change later when we choose formatting templates.
      prefix_before_national_number_.push_back(kSeparatorBeforeNationalNumber);
      AttemptToChoosePatternWithPrefixExtracted(phone_number);
      return;
    }
    phone_number->clear();
    accrued_input_.toUTF8String(*phone_number);
    return;
  }

  // We start to attempt to format only when at least kMinLeadingDigitsLength
  // digits (the plus sign is counted as a digit as well for this purpose) have
  // been entered.
  switch (accrued_input_without_formatting_.length()) {
    case 0:
    case 1:
    case 2:
      phone_number->clear();
      accrued_input_.toUTF8String(*phone_number);
      return;
    case 3:
      if (AttemptToExtractIdd()) {
        is_expecting_country_code_ = true;
        // FALLTHROUGH_INTENDED
      } else {
        // No IDD or plus sign is found, might be entering in national format.
        RemoveNationalPrefixFromNationalNumber(&extracted_national_prefix_);
        AttemptToChooseFormattingPattern(phone_number);
        return;
      }
    default:
      if (is_expecting_country_code_) {
        if (AttemptToExtractCountryCode()) {
          is_expecting_country_code_ = false;
        }
        phone_number->assign(prefix_before_national_number_);
        phone_number->append(national_number_);
        return;
      }
      if (possible_formats_.size() > 0) {
        // The formatting patterns are already chosen.
        string temp_national_number;
        InputDigitHelper(normalized_next_char, &temp_national_number);
        // See if accrued digits can be formatted properly already. If not, use
        // the results from InputDigitHelper, which does formatting based on the
        // formatting pattern chosen.
        string formatted_number;
        AttemptToFormatAccruedDigits(&formatted_number);
        if (formatted_number.length() > 0) {
          phone_number->assign(formatted_number);
          return;
        }
        NarrowDownPossibleFormats(national_number_);
        if (MaybeCreateNewTemplate()) {
          InputAccruedNationalNumber(phone_number);
          return;
        }
        if (able_to_format_) {
          AppendNationalNumber(temp_national_number, phone_number);
        } else {
          phone_number->clear();
          accrued_input_.toUTF8String(*phone_number);
        }
        return;
      } else {
        AttemptToChooseFormattingPattern(phone_number);
      }
  }
}

void AsYouTypeFormatter::AttemptToChoosePatternWithPrefixExtracted(
    string* formatted_number) {
  able_to_format_ = true;
  is_expecting_country_code_ = false;
  possible_formats_.clear();
  last_match_position_ = 0;
  formatting_template_.remove();
  current_formatting_pattern_.clear();
  AttemptToChooseFormattingPattern(formatted_number);
}

const string& AsYouTypeFormatter::GetExtractedNationalPrefix() const {
  return extracted_national_prefix_;
}

bool AsYouTypeFormatter::AbleToExtractLongerNdd() {
  if (extracted_national_prefix_.length() > 0) {
    // Put the extracted NDD back to the national number before attempting to
    // extract a new NDD.
    national_number_.insert(0, extracted_national_prefix_);
    // Remove the previously extracted NDD from prefixBeforeNationalNumber. We
    // cannot simply set it to empty string because people sometimes incorrectly
    // enter national prefix after the country code, e.g. +44 (0)20-1234-5678.
    int index_of_previous_ndd = static_cast<int>(
        prefix_before_national_number_.find_last_of(extracted_national_prefix_));
    prefix_before_national_number_.resize(index_of_previous_ndd);
  }
  string new_national_prefix;
  RemoveNationalPrefixFromNationalNumber(&new_national_prefix);
  return extracted_national_prefix_ != new_national_prefix;
}

void AsYouTypeFormatter::AttemptToFormatAccruedDigits(
    string* formatted_result) {
  DCHECK(formatted_result);

  for (list<const NumberFormat*>::const_iterator it = possible_formats_.begin();
       it != possible_formats_.end(); ++it) {
    DCHECK(*it);
    const NumberFormat& number_format = **it;
    const string& pattern = number_format.pattern();

    if (regexp_cache_.GetRegExp(pattern).FullMatch(national_number_)) {
      SetShouldAddSpaceAfterNationalPrefix(number_format);

      string formatted_number(national_number_);
      bool status = regexp_cache_.GetRegExp(pattern).GlobalReplace(
          &formatted_number, number_format.format());
      DCHECK(status);
      IGNORE_UNUSED(status);

      string full_output(*formatted_result);
      // Check that we didn't remove nor add any extra digits when we matched
      // this formatting pattern. This usually happens after we entered the last
      // digit during AYTF. Eg: In case of MX, we swallow mobile token (1) when
      // formatted but AYTF should retain all the number entered and not change
      // in order to match a format (of same leading digits and length) display
      // in that way.
      AppendNationalNumber(formatted_number, &full_output);
      phone_util_.NormalizeDiallableCharsOnly(&full_output);
      string accrued_input_without_formatting_stdstring;
      accrued_input_without_formatting_.toUTF8String(
          accrued_input_without_formatting_stdstring);
      if (full_output == accrued_input_without_formatting_stdstring) {
        // If it's the same (i.e entered number and format is same), then it's
        // safe to return this in formatted number as nothing is lost / added.
        AppendNationalNumber(formatted_number, formatted_result);
        return;
      }
    }
  }
}

int AsYouTypeFormatter::GetRememberedPosition() const {
  UnicodeString current_output(current_output_.c_str());
  if (!able_to_format_) {
    return ConvertUnicodeStringPosition(current_output, original_position_);
  }
  int accrued_input_index = 0;
  int current_output_index = 0;

  while (accrued_input_index < position_to_remember_ &&
         current_output_index < current_output.length()) {
    if (accrued_input_without_formatting_[accrued_input_index] ==
        current_output[current_output_index]) {
      ++accrued_input_index;
    }
    ++current_output_index;
  }
  return ConvertUnicodeStringPosition(current_output, current_output_index);
}

void AsYouTypeFormatter::AppendNationalNumber(const string& national_number,
                                              string* phone_number) const {
  int prefix_before_national_number_length =
      static_cast<int>(prefix_before_national_number_.size());
  if (should_add_space_after_national_prefix_ &&
      prefix_before_national_number_length > 0 &&
      prefix_before_national_number_.at(
          prefix_before_national_number_length - 1) !=
      kSeparatorBeforeNationalNumber) {
    // We want to add a space after the national prefix if the national prefix
    // formatting rule indicates that this would normally be done, with the
    // exception of the case where we already appended a space because the NDD
    // was surprisingly long.
    phone_number->assign(prefix_before_national_number_);
    phone_number->push_back(kSeparatorBeforeNationalNumber);
    StrAppend(phone_number, national_number);
  } else {
    phone_number->assign(
        StrCat(prefix_before_national_number_, national_number));
  }
}

void AsYouTypeFormatter::AttemptToChooseFormattingPattern(
    string* formatted_number) {
  DCHECK(formatted_number);
  // We start to attempt to format only when at least MIN_LEADING_DIGITS_LENGTH
  // digits of national number (excluding national prefix) have been entered.
  if (national_number_.length() >= kMinLeadingDigitsLength) {
    GetAvailableFormats(national_number_);
    formatted_number->clear();
    AttemptToFormatAccruedDigits(formatted_number);
    // See if the accrued digits can be formatted properly already.
    if (formatted_number->length() > 0) {
      return;
    }
    if (MaybeCreateNewTemplate()) {
      InputAccruedNationalNumber(formatted_number);
    } else {
      formatted_number->clear();
      accrued_input_.toUTF8String(*formatted_number);
    }
    return;
  } else {
    AppendNationalNumber(national_number_, formatted_number);
  }
}

void AsYouTypeFormatter::InputAccruedNationalNumber(string* number) {
  DCHECK(number);
  int length_of_national_number = static_cast<int>(national_number_.length());

  if (length_of_national_number > 0) {
    string temp_national_number;

    for (int i = 0; i < length_of_national_number; ++i) {
      temp_national_number.clear();
      InputDigitHelper(national_number_[i], &temp_national_number);
    }
    if (able_to_format_) {
      AppendNationalNumber(temp_national_number, number);
    } else {
      number->clear();
      accrued_input_.toUTF8String(*number);
    }
    return;
  } else {
    number->assign(prefix_before_national_number_);
  }
}

bool AsYouTypeFormatter::IsNanpaNumberWithNationalPrefix() const {
  // For NANPA numbers beginning with 1[2-9], treat the 1 as the national
  // prefix. The reason is that national significant numbers in NANPA always
  // start with [2-9] after the national prefix.  Numbers beginning with 1[01]
  // can only be short/emergency numbers, which don't need the national
  // prefix.
  return (current_metadata_->country_code() == 1) &&
         (national_number_[0] == '1') && (national_number_[1] != '0') &&
         (national_number_[1] != '1');
}

void AsYouTypeFormatter::RemoveNationalPrefixFromNationalNumber(
    string* national_prefix) {
  int start_of_national_number = 0;

  if (IsNanpaNumberWithNationalPrefix()) {
    start_of_national_number = 1;
    prefix_before_national_number_.append("1");
    prefix_before_national_number_.push_back(kSeparatorBeforeNationalNumber);
    is_complete_number_ = true;
  } else if (current_metadata_->has_national_prefix_for_parsing()) {
    const scoped_ptr<RegExpInput> consumed_input(
        regexp_factory_->CreateInput(national_number_));
    const RegExp& pattern = regexp_cache_.GetRegExp(
        current_metadata_->national_prefix_for_parsing());

    // Since some national prefix patterns are entirely optional, check that a
    // national prefix could actually be extracted.
    if (pattern.Consume(consumed_input.get())) {
      start_of_national_number = static_cast<int>(
          national_number_.length() - consumed_input->ToString().length());
      if (start_of_national_number > 0) {
        // When the national prefix is detected, we use international formatting
        // rules instead of national ones, because national formatting rules
        // could countain local formatting rules for numbers entered without
        // area code.
        is_complete_number_ = true;
        prefix_before_national_number_.append(
            national_number_.substr(0, start_of_national_number));
      }
    }
  }
  national_prefix->assign(national_number_, 0, start_of_national_number);
  national_number_.erase(0, start_of_national_number);
}

bool AsYouTypeFormatter::AttemptToExtractIdd() {
  string accrued_input_without_formatting_stdstring;
  accrued_input_without_formatting_
      .toUTF8String(accrued_input_without_formatting_stdstring);
  const scoped_ptr<RegExpInput> consumed_input(
      regexp_factory_->CreateInput(accrued_input_without_formatting_stdstring));
  const RegExp& international_prefix = regexp_cache_.GetRegExp(
      StrCat("\\", string(&kPlusSign, 1), "|",
             current_metadata_->international_prefix()));

  if (international_prefix.Consume(consumed_input.get())) {
    is_complete_number_ = true;
    const int start_of_country_code = static_cast<int>(
        accrued_input_without_formatting_.length() -
        consumed_input->ToString().length());

    national_number_.clear();
    accrued_input_without_formatting_.tempSubString(start_of_country_code)
        .toUTF8String(national_number_);

    string before_country_code;
    accrued_input_without_formatting_.tempSubString(0, start_of_country_code)
        .toUTF8String(before_country_code);
    prefix_before_national_number_.clear();
    prefix_before_national_number_.append(before_country_code);

    if (accrued_input_without_formatting_[0] != kPlusSign) {
      prefix_before_national_number_.push_back(kSeparatorBeforeNationalNumber);
    }
    return true;
  }
  return false;
}

bool AsYouTypeFormatter::AttemptToExtractCountryCode() {
  if (national_number_.length() == 0) {
    return false;
  }
  string number_without_country_code(national_number_);
  int country_code =
    phone_util_.ExtractCountryCode(&number_without_country_code);
  if (country_code == 0) {
    return false;
  }
  national_number_.assign(number_without_country_code);
  string new_region_code;
  phone_util_.GetRegionCodeForCountryCode(country_code, &new_region_code);
  if (PhoneNumberUtil::kRegionCodeForNonGeoEntity == new_region_code) {
    current_metadata_ =
        phone_util_.GetMetadataForNonGeographicalRegion(country_code);
  } else if (new_region_code != default_country_) {
    current_metadata_ = GetMetadataForRegion(new_region_code);
  }
  StrAppend(&prefix_before_national_number_, country_code);
  prefix_before_national_number_.push_back(kSeparatorBeforeNationalNumber);
  // When we have successfully extracted the IDD, the previously extracted NDD
  // should be cleared because it is no longer valid.
  extracted_national_prefix_.clear();

  return true;
}

char AsYouTypeFormatter::NormalizeAndAccrueDigitsAndPlusSign(
    char32 next_char,
    bool remember_position) {
  char normalized_char = next_char;

  if (next_char == kPlusSign) {
    accrued_input_without_formatting_.append(next_char);
  } else {
    string number;
    UnicodeString(next_char).toUTF8String(number);
    phone_util_.NormalizeDigitsOnly(&number);
    accrued_input_without_formatting_.append(next_char);
    national_number_.append(number);
    normalized_char = number[0];
  }
  if (remember_position) {
    position_to_remember_ = accrued_input_without_formatting_.length();
  }
  return normalized_char;
}

void AsYouTypeFormatter::InputDigitHelper(char next_char, string* number) {
  DCHECK(number);
  number->clear();
  // Note that formattingTemplate is not guaranteed to have a value, it could be
  // empty, e.g. when the next digit is entered after extracting an IDD or NDD.
  const char32 placeholder_codepoint = UnicodeString(kDigitPlaceholder)[0];
  int placeholder_pos = formatting_template_
      .tempSubString(last_match_position_).indexOf(placeholder_codepoint);
  if (placeholder_pos != -1) {
    UnicodeString temp_template = formatting_template_;
    placeholder_pos = temp_template.indexOf(placeholder_codepoint);
    temp_template.setCharAt(placeholder_pos, UnicodeString(next_char)[0]);
    last_match_position_ = placeholder_pos;
    formatting_template_.replace(0, temp_template.length(), temp_template);
    formatting_template_.tempSubString(0, last_match_position_ + 1)
        .toUTF8String(*number);
  } else {
    if (possible_formats_.size() == 1) {
      // More digits are entered than we could handle, and there are no other
      // valid patterns to try.
      able_to_format_ = false;
    }  // else, we just reset the formatting pattern.
    current_formatting_pattern_.clear();
    accrued_input_.toUTF8String(*number);
  }
}

// Returns the number of bytes contained in the given UnicodeString up to the
// specified position.
// static
int AsYouTypeFormatter::ConvertUnicodeStringPosition(const UnicodeString& s,
                                                     int pos) {
  if (pos > s.length()) {
    return -1;
  }
  string substring;
  s.tempSubString(0, pos).toUTF8String(substring);
  return static_cast<int>(substring.length());
}

}  // namespace phonenumbers
}  // namespace i18n
