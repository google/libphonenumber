/*
 * Copyright (C) 2014 The Libphonenumber Authors
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

#include "phonenumbers/regex_based_matcher.h"

#include <string>

#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/regexp_adapter.h"
#include "phonenumbers/regexp_cache.h"
#include "phonenumbers/regexp_factory.h"

namespace i18n {
namespace phonenumbers {

// Same implementations of AbstractRegExpFactory and RegExpCache in
// PhoneNumberUtil (copy from phonenumberutil.cc).
RegexBasedMatcher::RegexBasedMatcher()
    : regexp_factory_(new RegExpFactory()),
      regexp_cache_(new RegExpCache(*regexp_factory_, 128)) {}

RegexBasedMatcher::~RegexBasedMatcher() {}

bool RegexBasedMatcher::MatchNationalNumber(
    const string& number,
    const PhoneNumberDesc& number_desc,
    bool allow_prefix_match) const {
  const string& national_number_pattern = number_desc.national_number_pattern();
  // We don't want to consider it a prefix match when matching non-empty input
  // against an empty pattern.
  if (national_number_pattern.empty()) {
    return false;
  }
  return Match(number, national_number_pattern, allow_prefix_match);
}

bool RegexBasedMatcher::Match(
    const string& number,
    const string& number_pattern,
    bool allow_prefix_match) const {
  const RegExp& regexp(regexp_cache_->GetRegExp(number_pattern));

  if (regexp.FullMatch(number)) {
    return true;
  }
  const scoped_ptr<RegExpInput> normalized_number_input(
      regexp_factory_->CreateInput(number));
  return regexp.Consume(normalized_number_input.get())
      ? allow_prefix_match
      : false;
}

}  // namespace phonenumbers
}  // namespace i18n
