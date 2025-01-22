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

#include "phonenumbers/phonenumbernormalizer.h"

#include "phonenumbers/base/logging.h"
#include "phonenumbers/constants.h"
#include "phonenumbers/normalize_utf8.h"

namespace i18n {
namespace phonenumbers {

PhoneNumberNormalizer::PhoneNumberNormalizer(
    std::shared_ptr<PhoneNumberRegExpsAndMappings> reg_exps)
    : reg_exps_(reg_exps) {}

void PhoneNumberNormalizer::NormalizeDigitsOnly(std::string* number) const {
  DCHECK(number);
  const RegExp& non_digits_pattern = reg_exps_->regexp_cache_->GetRegExp(
      absl::StrCat("[^", Constants::kDigits, "]"));
  // Delete everything that isn't valid digits.
  non_digits_pattern.GlobalReplace(number, "");
  // Normalize all decimal digits to ASCII digits.
  number->assign(NormalizeUTF8::NormalizeDecimalDigits(*number));
}

}  // namespace phonenumbers
}  // namespace i18n
