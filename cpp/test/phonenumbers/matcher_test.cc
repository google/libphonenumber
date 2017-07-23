// Copyright (C) 2017 The Libphonenumber Authors
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

// Tests that all implementations of MatcherApi are consistent.

#include "phonenumbers/matcher_api.h"

#include <string>
#include <vector>

#include <gtest/gtest.h>

#include "phonenumbers/regex_based_matcher.h"
#include "phonenumbers/phonemetadata.pb.h"

namespace i18n {
namespace phonenumbers {

namespace {

string ToString(const PhoneNumberDesc& desc) {
  string str = "pattern: ";
  if (desc.has_national_number_pattern()) {
    str += desc.national_number_pattern();
  } else {
    str += "none";
  }
  return str;
}

void ExpectMatched(
    const MatcherApi& matcher,
    const string& number,
    const PhoneNumberDesc& desc) {
  EXPECT_TRUE(matcher.MatchNationalNumber(number, desc, false))
      << number << " should have matched " << ToString(desc);
  EXPECT_TRUE(matcher.MatchNationalNumber(number, desc, true))
      << number << " should have matched " << ToString(desc);
}

void ExpectInvalid(
    const MatcherApi& matcher,
    const string& number,
    const PhoneNumberDesc& desc) {
  EXPECT_FALSE(matcher.MatchNationalNumber(number, desc, false))
      << number << " should not have matched " << ToString(desc);
  EXPECT_FALSE(matcher.MatchNationalNumber(number, desc, true))
      << number << " should not have matched " << ToString(desc);
}

void ExpectTooLong(
    const MatcherApi& matcher,
    const string& number,
    const PhoneNumberDesc& desc) {
  EXPECT_FALSE(matcher.MatchNationalNumber(number, desc, false))
      << number << " should have been too long for " << ToString(desc);
  EXPECT_TRUE(matcher.MatchNationalNumber(number, desc, true))
      << number << " should have been too long for " << ToString(desc);
}

}  // namespace

class MatcherTest : public testing::Test {
 protected:
  void CheckMatcherBehavesAsExpected(const MatcherApi& matcher) const {
    PhoneNumberDesc desc;

    desc = CreateDesc("");
    // Test if there is no matcher data.
    ExpectInvalid(matcher, "1", desc);

    desc = CreateDesc("9\\d{2}");
    ExpectInvalid(matcher, "91", desc);
    ExpectInvalid(matcher, "81", desc);
    ExpectMatched(matcher, "911", desc);
    ExpectInvalid(matcher, "811", desc);
    ExpectTooLong(matcher, "9111", desc);
    ExpectInvalid(matcher, "8111", desc);

    desc = CreateDesc("\\d{1,2}");
    ExpectMatched(matcher, "2", desc);
    ExpectMatched(matcher, "20", desc);

    desc = CreateDesc("20?");
    ExpectMatched(matcher, "2", desc);
    ExpectMatched(matcher, "20", desc);

    desc = CreateDesc("2|20");
    ExpectMatched(matcher, "2", desc);
    // Subtle case where lookingAt() and matches() result in different end()s.
    ExpectMatched(matcher, "20", desc);
  }

 private:
  // Helper method to set national number fields in the PhoneNumberDesc proto.
  // Empty fields won't be set.
  PhoneNumberDesc CreateDesc(
      const string& national_number_pattern) const {
    PhoneNumberDesc desc;
    if (!national_number_pattern.empty()) {
      desc.set_national_number_pattern(national_number_pattern);
    }
    return desc;
  }
};

TEST_F(MatcherTest, RegexBasedMatcher) {
  RegexBasedMatcher matcher;
  CheckMatcherBehavesAsExpected(matcher);
}

}  // namespace phonenumbers
}  // namespace i18n
