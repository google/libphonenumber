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

// Class containing string constants of region codes for easier testing.
class RegionCode {
 public:
  static const string& AD() {
    static const string s = "AD";
    return s;
  }

  static const string& AO() {
    static const string s = "AO";
    return s;
  }

  static const string& AQ() {
    static const string s = "AQ";
    return s;
  }

  static const string& AR() {
    static const string s = "AR";
    return s;
  }

  static const string& AU() {
    static const string s = "AU";
    return s;
  }

  static const string& BS() {
    static const string s = "BS";
    return s;
  }

  static const string& CA() {
    static const string s = "CA";
    return s;
  }

  static const string& CN() {
    static const string s = "CN";
    return s;
  }

  static const string& CS() {
    static const string s = "CS";
    return s;
  }

  static const string& DE() {
    static const string s = "DE";
    return s;
  }

  static const string& GB() {
    static const string s = "GB";
    return s;
  }

  static const string& IT() {
    static const string s = "IT";
    return s;
  }

  static const string& JP() {
    static const string s = "JP";
    return s;
  }

  static const string& KR() {
    static const string s = "KR";
    return s;
  }

  static const string& MX() {
    static const string s = "MX";
    return s;
  }

  static const string& NZ() {
    static const string s = "NZ";
    return s;
  }

  static const string& PL() {
    static const string s = "PL";
    return s;
  }

  static const string& RE() {
    static const string s = "RE";
    return s;
  }

  static const string& SG() {
    static const string s = "SG";
    return s;
  }

  static const string& US() {
    static const string s = "US";
    return s;
  }

  static const string& YT() {
    static const string s = "YT";
    return s;
  }

  // Returns a region code string representing the "unknown" region.
  static const string& GetUnknown() {
    static const string s = "ZZ";
    return s;
  }

  static const string& ZZ() {
    return GetUnknown();
  }
};

}  // namespace phonenumbers
}  // namespace i18n
