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

// Author: Philippe Liard

#include <iostream>
#include <vector>

#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/test_util.h"

namespace i18n {
namespace phonenumbers {

ostream& operator<<(ostream& os, const PhoneNumber& number) {
  os << std::endl
     << "country_code: " << number.country_code() << std::endl
     << "national_number: " << number.national_number() << std::endl;
  if (number.has_extension()) {
     os << "extension: " << number.extension() << std::endl;
  }
  if (number.has_italian_leading_zero()) {
     os << "italian_leading_zero: " << number.italian_leading_zero() << std::endl;
  }
  if (number.has_raw_input()) {
     os << "raw_input: " << number.raw_input() << std::endl;
  }
  if (number.has_country_code_source()) {
     os << "country_code_source: " << number.country_code_source() << std::endl;
  }
  if (number.has_preferred_domestic_carrier_code()) {
     os << "preferred_domestic_carrier_code: "
        << number.preferred_domestic_carrier_code() << std::endl;
  }
  return os;
}

ostream& operator<<(ostream& os, const vector<PhoneNumber>& numbers) {
  os << "[" << std::endl;
  for (vector<PhoneNumber>::const_iterator it = numbers.begin();
       it != numbers.end(); ++it) {
    os << *it;
  }
  os << std::endl << "]" << std::endl;
  return os;
}

}  // namespace phonenumbers
}  // namespace i18n
