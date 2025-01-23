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

#ifndef I18N_PHONENUMBERS_PHONENUMBERNORMALIZER_H_
#define I18N_PHONENUMBERS_PHONENUMBERNORMALIZER_H_

#include <memory>
#include <string>

#include "phonenumbers/regexpsandmappings.h"

namespace i18n {
namespace phonenumbers {

// Util class to normalize phone numbers.
class PhoneNumberNormalizer {
  friend class AsYouTypeFormatter;
  friend class PhoneContextParser;
  friend class PhoneNumberMatcher;
  friend class PhoneNumberUtil;
  friend class XCharValidator;
  friend class PhoneContextParserTest;
  friend class PhoneNumberNormalizerTest;

 private:
  std::shared_ptr<PhoneNumberRegExpsAndMappings> reg_exps_;

  explicit PhoneNumberNormalizer(
      std::shared_ptr<PhoneNumberRegExpsAndMappings> reg_exps);

  // Normalizes a string of characters representing a phone number. This
  // converts wide-ascii and arabic-indic numerals to European numerals, and
  // strips punctuation and alpha characters.
  void NormalizeDigitsOnly(std::string* number) const;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_PHONENUMBERNORMALIZER_H_