// Copyright (C) 2011 Google Inc.
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

// Author: George Yakovlev
//         Philippe Liard

#include "phonenumbers/regexp_adapter_icu.h"

#include <string>

#include <unicode/regex.h>
#include <unicode/unistr.h>

#include "base/basictypes.h"
#include "base/logging.h"
#include "base/scoped_ptr.h"
#include "phonenumbers/default_logger.h"

namespace i18n {
namespace phonenumbers {

using icu::RegexMatcher;
using icu::RegexPattern;
using icu::UnicodeString;

namespace {

// Converts UnicodeString 'source' to a UTF8-formatted std::string.
string UnicodeStringToUtf8String(const UnicodeString& source) {
  string data;
  source.toUTF8String<string>(data);
  return data;
}

}  // namespace

// Implementation of the abstract classes RegExpInput and RegExp using ICU
// regular expression capabilities.

// ICU implementation of the RegExpInput abstract class.
class IcuRegExpInput : public RegExpInput {
 public:
  explicit IcuRegExpInput(const string& utf8_input)
      : utf8_input_(UnicodeString::fromUTF8(utf8_input)),
        position_(0) {}

  virtual ~IcuRegExpInput() {}

  virtual string ToString() const {
    return UnicodeStringToUtf8String(utf8_input_.tempSubString(position_));
  }

  UnicodeString* Data() {
    return &utf8_input_;
  }

  // The current start position. For a newly created input, position is 0. Each
  // call to ConsumeRegExp() or RegExp::Consume() advances the position in the
  // case of the successful match to be after the match.
  int position() const {
    return position_;
  }

  void set_position(int position) {
    DCHECK(position >= 0 && position <= utf8_input_.length());
    position_ = position;
  }

 private:
  UnicodeString utf8_input_;
  int position_;

  DISALLOW_COPY_AND_ASSIGN(IcuRegExpInput);
};

// ICU implementation of the RegExp abstract class.
class IcuRegExp : public RegExp {
 public:
  explicit IcuRegExp(const string& utf8_regexp) {
    UParseError parse_error;
    UErrorCode status = U_ZERO_ERROR;
    utf8_regexp_.reset(RegexPattern::compile(
        UnicodeString::fromUTF8(utf8_regexp), 0, parse_error, status));
    if (U_FAILURE(status)) {
      // The provided regular expressions should compile correctly.
      LOG(ERROR) << "Error compiling regular expression: " << utf8_regexp;
      utf8_regexp_.reset(NULL);
    }
  }

  virtual ~IcuRegExp() {}

  virtual bool Consume(RegExpInput* input_string,
                       bool anchor_at_start,
                       string* matched_string1,
                       string* matched_string2,
                       string* matched_string3) const {
    DCHECK(input_string);
    if (!utf8_regexp_.get()) {
      return false;
    }
    IcuRegExpInput* const input = static_cast<IcuRegExpInput*>(input_string);
    UErrorCode status = U_ZERO_ERROR;
    const scoped_ptr<RegexMatcher> matcher(
        utf8_regexp_->matcher(*input->Data(), status));
    bool match_succeeded = anchor_at_start
        ? matcher->lookingAt(input->position(), status)
        : matcher->find(input->position(), status);
    if (!match_succeeded || U_FAILURE(status)) {
      return false;
    }
    string* const matched_strings[] = {
      matched_string1, matched_string2, matched_string3
    };
    // If less matches than expected - fail.
    for (size_t i = 0; i < arraysize(matched_strings); ++i) {
      if (matched_strings[i]) {
        // Groups are counted from 1 rather than 0.
        const int group_index = i + 1;
        if (group_index > matcher->groupCount()) {
          return false;
        }
        *matched_strings[i] =
            UnicodeStringToUtf8String(matcher->group(group_index, status));
      }
    }
    input->set_position(matcher->end(status));
    return !U_FAILURE(status);
  }

  bool Match(const string& input_string,
             bool full_match,
             string* matched_string) const {
    if (!utf8_regexp_.get()) {
      return false;
    }
    IcuRegExpInput input(input_string);
    UErrorCode status = U_ZERO_ERROR;
    const scoped_ptr<RegexMatcher> matcher(
        utf8_regexp_->matcher(*input.Data(), status));
    bool match_succeeded = full_match
        ? matcher->matches(input.position(), status)
        : matcher->find(input.position(), status);
    if (!match_succeeded || U_FAILURE(status)) {
      return false;
    }
    if (matcher->groupCount() > 0 && matched_string) {
      *matched_string = UnicodeStringToUtf8String(matcher->group(1, status));
    }
    return !U_FAILURE(status);
  }

  bool Replace(string* string_to_process,
               bool global,
               const string& replacement_string) const {
    DCHECK(string_to_process);
    if (!utf8_regexp_.get()) {
      return false;
    }
    IcuRegExpInput input(*string_to_process);
    UErrorCode status = U_ZERO_ERROR;
    const scoped_ptr<RegexMatcher> matcher(
        utf8_regexp_->matcher(*input.Data(), status));
    if (U_FAILURE(status)) {
      return false;
    }
    UnicodeString result = global
        ? matcher->replaceAll(
            UnicodeString::fromUTF8(replacement_string), status)
        : matcher->replaceFirst(
            UnicodeString::fromUTF8(replacement_string), status);
    if (U_FAILURE(status)) {
      return false;
    }
    const string replaced_string = UnicodeStringToUtf8String(result);
    if (replaced_string == *string_to_process) {
      return false;
    }
    *string_to_process = replaced_string;
    return true;
  }

 private:
  scoped_ptr<RegexPattern> utf8_regexp_;

  DISALLOW_COPY_AND_ASSIGN(IcuRegExp);
};

RegExpInput* ICURegExpFactory::CreateInput(const string& utf8_input) const {
  return new IcuRegExpInput(utf8_input);
}

RegExp* ICURegExpFactory::CreateRegExp(const string& utf8_regexp) const {
  return new IcuRegExp(utf8_regexp);
}

}  // namespace phonenumbers
}  // namespace i18n
