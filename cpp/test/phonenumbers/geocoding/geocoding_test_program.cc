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

// Sample program using the geocoding functionality. This is used to test that
// the geocoding library is compiled correctly.

#include <iostream>
#include <string>

#include "phonenumbers/base/logging.h"
#include "phonenumbers/geocoding/phonenumber_offline_geocoder.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/phonenumberutil.h"

using i18n::phonenumbers::PhoneNumber;
using i18n::phonenumbers::PhoneNumberOfflineGeocoder;
using i18n::phonenumbers::PhoneNumberUtil;

int main() {
  PhoneNumber number;
  const PhoneNumberUtil& phone_util = *PhoneNumberUtil::GetInstance();
  const PhoneNumberUtil::ErrorType status = phone_util.Parse(
      "16502530000", "US", &number);
  CHECK_EQ(status, PhoneNumberUtil::NO_PARSING_ERROR);
  IGNORE_UNUSED(status);

  const std::string description =
      PhoneNumberOfflineGeocoder().GetDescriptionForNumber(
          number, icu::Locale("en", "GB"));
  std::cout << description << std::endl;
  CHECK_EQ(description, "Mountain View, CA");
  return 0;
}
