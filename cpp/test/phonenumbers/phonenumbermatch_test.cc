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
//
// Author: Tao Huang
//
// Basic test cases for PhoneNumberMatch.

#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumbermatch.h"

#include <gtest/gtest.h>

#include "phonenumbers/phonenumber.pb.h"

namespace i18n {
namespace phonenumbers {

TEST(PhoneNumberMatch, TestGetterMethods) {
  PhoneNumber number;
  const int start_index = 10;
  const string raw_phone_number("1 800 234 45 67");
  PhoneNumberMatch match1(start_index, raw_phone_number, number);

  EXPECT_EQ(start_index, match1.start());
  EXPECT_EQ(start_index + static_cast<int>(raw_phone_number.length()),
            match1.end());
  EXPECT_EQ(static_cast<int>(raw_phone_number.length()), match1.length());
  EXPECT_EQ(raw_phone_number, match1.raw_string());

  EXPECT_EQ("PhoneNumberMatch [10,25) 1 800 234 45 67", match1.ToString());
}

TEST(PhoneNumberMatch, TestEquals) {
  PhoneNumber number;
  PhoneNumberMatch match1(10, "1 800 234 45 67", number);
  PhoneNumberMatch match2(10, "1 800 234 45 67", number);

  match2.set_start(11);
  ASSERT_FALSE(match1.Equals(match2));
  match2.set_start(match1.start());
  EXPECT_TRUE(match1.Equals(match2));

  PhoneNumber number2;
  number2.set_raw_input("123");
  match2.set_number(number2);
  ASSERT_FALSE(match1.Equals(match2));
  match2.set_number(match1.number());
  EXPECT_TRUE(ExactlySameAs(match1.number(), match2.number()));
  EXPECT_TRUE(match1.Equals(match2));

  match2.set_raw_string("123");
  ASSERT_FALSE(match1.Equals(match2));
}

TEST(PhoneNumberMatch, TestAssignmentOverload) {
  PhoneNumber number;
  PhoneNumberMatch match1(10, "1 800 234 45 67", number);
  PhoneNumberMatch match2;
  ASSERT_FALSE(match1.Equals(match2));

  match2.CopyFrom(match1);
  ASSERT_TRUE(match1.Equals(match2));

  PhoneNumberMatch match3;
  PhoneNumberMatch match4;
  match4.CopyFrom(match2);
  match3.CopyFrom(match2);
  ASSERT_TRUE(match3.Equals(match4));
  ASSERT_TRUE(match4.Equals(match2));
}

TEST(PhoneNumberMatch, TestCopyConstructor) {
  PhoneNumber number;
  PhoneNumberMatch match1(10, "1 800 234 45 67", number);
  PhoneNumberMatch match2;
  match2.CopyFrom(match1);
  ASSERT_TRUE(match1.Equals(match2));
}

}  // namespace phonenumbers
}  // namespace i18n
