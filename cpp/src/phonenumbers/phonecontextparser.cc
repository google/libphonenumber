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

#include "phonenumbers/phonecontextparser.h"

#include <string>

#include "phonenumbers/constants.h"

namespace i18n {
namespace phonenumbers {

PhoneContextParser::PhoneContextParser(
    std::unique_ptr<std::vector<int>> country_calling_codes,
    std::shared_ptr<PhoneNumberRegExpsAndMappings> reg_exps,
    std::shared_ptr<PhoneNumberNormalizer> normalizer)
    : country_calling_codes_(std::move(country_calling_codes)),
      reg_exps_(reg_exps),
      normalizer_(normalizer) {}

std::optional<absl::string_view> PhoneContextParser::ExtractPhoneContext(
    const absl::string_view phone_number) {
  size_t index_of_phone_context =
      phone_number.find(Constants::kRfc3966PhoneContext);

  if (index_of_phone_context == std::string::npos) {
    return std::nullopt;
  }

  size_t phone_context_start =
      index_of_phone_context + strlen(Constants::kRfc3966PhoneContext);

  // If phone-context parameter is empty
  if (phone_context_start >= phone_number.length()) {
    return "";
  }

  size_t phone_context_end = phone_number.find(';', phone_context_start);
  // If phone-context is the last parameter
  if (phone_context_end == std::string::npos) {
    return phone_number.substr(phone_context_start);
  }

  return phone_number.substr(phone_context_start,
                             phone_context_end - phone_context_start);
}

bool PhoneContextParser::isValid(absl::string_view phone_context) {
  if (phone_context.empty()) {
    return false;
  }

  // Does phone-context value match the global number digits pattern or the
  // domain name pattern?
  return reg_exps_->rfc3966_global_number_digits_pattern_->FullMatch(
             std::string{phone_context}) ||
         reg_exps_->rfc3966_domainname_pattern_->FullMatch(
             std::string{phone_context});
}

bool PhoneContextParser::isValidCountryCode(int country_code) {
  return std::find(country_calling_codes_->begin(),
                   country_calling_codes_->end(),
                   country_code) != country_calling_codes_->end();
}

PhoneContextParser::PhoneContext PhoneContextParser::ParsePhoneContext(
    absl::string_view phone_context) {
  PhoneContextParser::PhoneContext phone_context_object;
  phone_context_object.raw_context = phone_context;
  phone_context_object.country_code = std::nullopt;

  // Ignore phone-context values that do not start with a plus sign. Could be a
  // domain name.
  if (!phone_context.empty() &&
      phone_context.at(0) == Constants::kPlusSign[0]) {
    return phone_context_object;
  }

  // Remove the plus sign from the phone context and normalize the digits.
  std::string normalized_phone_context = std::string(phone_context.substr(1));
  normalizer_->NormalizeDigitsOnly(&normalized_phone_context);

  if (normalized_phone_context.empty() ||
      normalized_phone_context.length() > Constants::kMaxLengthCountryCode) {
    return phone_context_object;
  }

  int potential_country_code = std::stoi(normalized_phone_context, nullptr, 10);
  if (!isValidCountryCode(potential_country_code)) {
    return phone_context_object;
  }

  phone_context_object.country_code = potential_country_code;
  return phone_context_object;
}

absl::StatusOr<std::optional<PhoneContextParser::PhoneContext>>
PhoneContextParser::Parse(absl::string_view phone_number) {
  std::optional<absl::string_view> phone_context =
      ExtractPhoneContext(phone_number);
  if (!phone_context.has_value()) {
    return std::nullopt;
  }

  if (!isValid(phone_context.value())) {
    return absl::InvalidArgumentError("Phone context is invalid.");
  }

  return ParsePhoneContext(phone_context.value());
}

}  // namespace phonenumbers
}  // namespace i18n