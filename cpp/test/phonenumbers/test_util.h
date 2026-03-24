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

#include <string>
#include <ostream>
#include <vector>

#include "phonenumbers/phonenumber.h"

namespace i18n {
namespace phonenumbers {

using std::string;
using std::ostream;
using std::vector;

class PhoneNumber;

// Provides PhoneNumber comparison operators to support the use of EXPECT_EQ and
// EXPECT_NE in the unittests.
inline bool operator==(const PhoneNumber& number1, const PhoneNumber& number2) {
  return ExactlySameAs(number1, number2);
}

inline bool operator!=(const PhoneNumber& number1, const PhoneNumber& number2) {
  return !(number1 == number2);
}

// Needed by Google Test to display errors.
ostream& operator<<(ostream& os, const PhoneNumber& number);

ostream& operator<<(ostream& os, const vector<PhoneNumber>& numbers);

// Class containing string constants of region codes for easier testing. Note
// that another private RegionCode class is defined in
// cpp/src/phonenumbers/region_code.h. This one contains more constants.
class RegionCode {
 public:
  static const char* AD() {
    return "AD";
  }

  static const char* AE() {
    return "AE";
  }

  static const char* AM() {
    return "AM";
  }

  static const char* AO() {
    return "AO";
  }

  static const char* AQ() {
    return "AQ";
  }

  static const char* AR() {
    return "AR";
  }

  static const char* AU() {
    return "AU";
  }

  static const char* BB() {
    return "BB";
  }

  static const char* BR() {
    return "BR";
  }

  static const char* BS() {
    return "BS";
  }

  static const char* BY() {
    return "BY";
  }

  static const char* CA() {
    return "CA";
  }

  static const char* CH() {
    return "CH";
  }

  static const char* CL() {
    return "CL";
  }

  static const char* CN() {
    return "CN";
  }

  static const char* CO() {
    return "CO";
  }

  static const char* CS() {
    return "CS";
  }

  static const char* CX() {
    return "CX";
  }

  static const char* DE() {
    return "DE";
  }

  static const char* FR() {
    return "FR";
  }

  static const char* GB() {
    return "GB";
  }

  static const char* HU() {
    return "HU";
  }

  static const char* IT() {
    return "IT";
  }

  static const char* JP() {
    return "JP";
  }

  static const char* KR() {
    return "KR";
  }

  static const char* MX() {
    return "MX";
  }

  static const char* NZ() {
    return "NZ";
  }

  static const char* PL() {
    return "PL";
  }

  static const char* RE() {
    return "RE";
  }

  static const char* RU() {
    return "RU";
  }

  static const char* SE() {
    return "SE";
  }

  static const char* SG() {
    return "SG";
  }

  static const char* UN001() {
    return "001";
  }

  static const char* US() {
    return "US";
  }

  static const char* UZ() {
    return "UZ";
  }

  static const char* YT() {
    return "YT";
  }

  static const char* ZW() {
    return "ZW";
  }

  // Returns a region code string representing the "unknown" region.
  static const char* GetUnknown() {
    return "ZZ";
  }

  static const char* ZZ() {
    return GetUnknown();
  }
};

}  // namespace phonenumbers
}  // namespace i18n
