// Copyright (C) 2012 The Libphonenumber Authors
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

// Author: David Yonge-Mallo

#include "phonenumbers/shortnumberutil.h"

#include "base/memory/scoped_ptr.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/regexp_adapter.h"
#include "phonenumbers/regexp_factory.h"

namespace i18n {
namespace phonenumbers {

using std::string;

ShortNumberUtil::ShortNumberUtil()
    : phone_util_(*PhoneNumberUtil::GetInstance()) {
}

bool ShortNumberUtil::ConnectsToEmergencyNumber(const string& number,
    const string& region_code) const {
  return MatchesEmergencyNumberHelper(number, region_code,
      true /* allows prefix match */);
}

bool ShortNumberUtil::IsEmergencyNumber(const string& number,
    const string& region_code) const {
  return MatchesEmergencyNumberHelper(number, region_code,
      false /* doesn't allow prefix match */);
}

bool ShortNumberUtil::MatchesEmergencyNumberHelper(const string& number,
    const string& region_code, bool allow_prefix_match) const {
  string extracted_number;
  phone_util_.ExtractPossibleNumber(number, &extracted_number);
  if (phone_util_.StartsWithPlusCharsPattern(extracted_number)) {
    // Returns false if the number starts with a plus sign. We don't believe
    // dialing the country code before emergency numbers (e.g. +1911) works,
    // but later, if that proves to work, we can add additional logic here to
    // handle it.
    return false;
  }
  const PhoneMetadata* metadata = phone_util_.GetMetadataForRegion(region_code);
  if (!metadata || !metadata->has_emergency()) {
    return false;
  }
  const scoped_ptr<const AbstractRegExpFactory> regexp_factory(
      new RegExpFactory());
  const scoped_ptr<const RegExp> emergency_number_pattern(
      regexp_factory->CreateRegExp(
          metadata->emergency().national_number_pattern()));
  phone_util_.NormalizeDigitsOnly(&extracted_number);
  const scoped_ptr<RegExpInput> normalized_number_input(
      regexp_factory->CreateInput(extracted_number));

  // In Brazil, it is impossible to append additional digits to an emergency
  // number to dial the number.
  return (!allow_prefix_match || region_code == "BR")
      ? emergency_number_pattern->FullMatch(extracted_number)
      : emergency_number_pattern->Consume(normalized_number_input.get());
}

}  // namespace phonenumbers
}  // namespace i18n
