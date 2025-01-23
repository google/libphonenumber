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

#ifndef I18N_PHONENUMBERS_PHONECONTEXTPARSER_H_
#define I18N_PHONENUMBERS_PHONECONTEXTPARSER_H_

#include <memory>
#include <optional>
#include <vector>
#include <string>

#include "absl/status/statusor.h"
#include "phonenumbers/phonenumbernormalizer.h"
#include "phonenumbers/regexpsandmappings.h"

namespace i18n {
namespace phonenumbers {

// Parses the phone-context parameter of a phone number in RFC3966 format.
class PhoneContextParser {
  friend class PhoneNumberUtil;
  friend class PhoneContextParserTest;

 private:
  struct PhoneContext {
    // The raw value of the phone-context parameter.
    std::string raw_context;

    // The country code of the phone-context parameter if the phone-context is
    // exactly and only a + followed by a valid country code.
    std::optional<int> country_code;
  };

  PhoneContextParser(std::unique_ptr<std::vector<int>> country_calling_codes,
                     std::shared_ptr<PhoneNumberRegExpsAndMappings> reg_exps,
                     std::shared_ptr<PhoneNumberNormalizer> normalizer);

  // Parses the phone-context parameter of a phone number in RFC3966 format.
  // If the phone-context parameter is not present, returns std::nullopt. If it
  // is present but invalid, returns an error status. If it is present and
  // valid, returns a PhoneContext object. This object contains the raw value of
  // the phone-context parameter. Additionally, if the phone-context is exactly
  // and only a + followed by a valid country code, it also contains the country
  // code.
  absl::StatusOr<std::optional<PhoneContextParser::PhoneContext>> Parse(
      absl::string_view phone_number);

  std::unique_ptr<std::vector<int>> country_calling_codes_;
  std::shared_ptr<PhoneNumberRegExpsAndMappings> reg_exps_;
  std::shared_ptr<PhoneNumberNormalizer> normalizer_;

  // Extracts the value of the phone-context parameter, following the
  // specification of RFC3966.
  static std::optional<absl::string_view> ExtractPhoneContext(
      absl::string_view phone_number);

  // Checks whether the phone context value follows the specification of
  // RFC3966.
  bool isValid(absl::string_view phone_context);

  bool isValidCountryCode(int country_code);

  // Parses the phone context value into a PhoneContext object.
  PhoneContext ParsePhoneContext(absl::string_view phone_context);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_PHONECONTEXTPARSER_H_