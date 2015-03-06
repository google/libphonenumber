// Copyright (C) 2009 The Libphonenumber Authors
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

// Author: David Yonge-Mallo

// Note that these tests use the test metadata, not the normal metadata file, so
// should not be used for regression test purposes - these tests are
// illustrative only and test functionality.

#include "phonenumbers/shortnumberinfo.h"

#include <gtest/gtest.h>

#include "phonenumbers/base/logging.h"
#include "phonenumbers/default_logger.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/stringutil.h"
#include "phonenumbers/test_util.h"

namespace i18n {
namespace phonenumbers {

class ShortNumberInfoTest : public testing::Test {
 protected:
  PhoneNumber ParseNumberForTesting(const string& number,
                                    const string& region_code) {
    PhoneNumber phone_number;
    CHECK_EQ(phone_util_.Parse(number, region_code, &phone_number),
             PhoneNumberUtil::NO_PARSING_ERROR);
    return phone_number;
  }

  ShortNumberInfoTest() : short_info_() {
    PhoneNumberUtil::GetInstance()->SetLogger(new StdoutLogger());
  }

  const PhoneNumberUtil phone_util_;
  const ShortNumberInfo short_info_;

 private:
  DISALLOW_COPY_AND_ASSIGN(ShortNumberInfoTest);
};

TEST_F(ShortNumberInfoTest, IsPossibleShortNumber) {
  PhoneNumber possible_number;
  possible_number.set_country_code(33);
  possible_number.set_national_number(123456ULL);
  EXPECT_TRUE(short_info_.IsPossibleShortNumber(possible_number));
  EXPECT_TRUE(short_info_.IsPossibleShortNumberForRegion(
      ParseNumberForTesting("123456", TestRegionCode::FR()),
      TestRegionCode::FR()));

  PhoneNumber impossible_number;
  impossible_number.set_country_code(33);
  impossible_number.set_national_number(9ULL);
  EXPECT_FALSE(short_info_.IsPossibleShortNumber(impossible_number));

  // Note that GB and GG share the country calling code 44, and that this
  // number is possible but not valid.
  PhoneNumber shared_number;
  shared_number.set_country_code(44);
  shared_number.set_national_number(11001ULL);
  EXPECT_TRUE(short_info_.IsPossibleShortNumber(shared_number));
}

TEST_F(ShortNumberInfoTest, IsValidShortNumber) {
  PhoneNumber valid_number;
  valid_number.set_country_code(33);
  valid_number.set_national_number(1010ULL);
  EXPECT_TRUE(short_info_.IsValidShortNumber(valid_number));
  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting("1010", TestRegionCode::FR()),
      TestRegionCode::FR()));

  PhoneNumber invalid_number;
  invalid_number.set_country_code(33);
  invalid_number.set_national_number(123456ULL);
  EXPECT_FALSE(short_info_.IsValidShortNumber(invalid_number));
  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting("123456", TestRegionCode::FR()),
      TestRegionCode::FR()));

  // Note that GB and GG share the country calling code 44.
  PhoneNumber shared_number;
  shared_number.set_country_code(44);
  shared_number.set_national_number(18001ULL);
  EXPECT_TRUE(short_info_.IsValidShortNumber(shared_number));
}

TEST_F(ShortNumberInfoTest, GetExpectedCost) {
  uint64 national_number;
  const string& premium_rate_example =
      short_info_.GetExampleShortNumberForCost(
          TestRegionCode::FR(), ShortNumberInfo::PREMIUM_RATE);
  EXPECT_EQ(ShortNumberInfo::PREMIUM_RATE,
            short_info_.GetExpectedCostForRegion(
                ParseNumberForTesting(premium_rate_example,
                                      TestRegionCode::FR()),
                TestRegionCode::FR()));
  PhoneNumber premium_rate_number;
  premium_rate_number.set_country_code(33);
  safe_strtou64(premium_rate_example, &national_number);
  premium_rate_number.set_national_number(national_number);
  EXPECT_EQ(ShortNumberInfo::PREMIUM_RATE,
     short_info_.GetExpectedCost(premium_rate_number));

  const string& standard_rate_example =
      short_info_.GetExampleShortNumberForCost(
          TestRegionCode::FR(), ShortNumberInfo::STANDARD_RATE);
  EXPECT_EQ(ShortNumberInfo::STANDARD_RATE,
            short_info_.GetExpectedCostForRegion(
                ParseNumberForTesting(standard_rate_example,
                                      TestRegionCode::FR()),
                TestRegionCode::FR()));
  PhoneNumber standard_rate_number;
  standard_rate_number.set_country_code(33);
  safe_strtou64(standard_rate_example, &national_number);
  standard_rate_number.set_national_number(national_number);
  EXPECT_EQ(ShortNumberInfo::STANDARD_RATE,
     short_info_.GetExpectedCost(standard_rate_number));

  const string& toll_free_example =
      short_info_.GetExampleShortNumberForCost(
          TestRegionCode::FR(), ShortNumberInfo::TOLL_FREE);
  EXPECT_EQ(ShortNumberInfo::TOLL_FREE,
            short_info_.GetExpectedCostForRegion(
                ParseNumberForTesting(toll_free_example, TestRegionCode::FR()),
                TestRegionCode::FR()));
  PhoneNumber toll_free_number;
  toll_free_number.set_country_code(33);
  safe_strtou64(toll_free_example, &national_number);
  toll_free_number.set_national_number(national_number);
  EXPECT_EQ(ShortNumberInfo::TOLL_FREE,
     short_info_.GetExpectedCost(toll_free_number));

  EXPECT_EQ(
      ShortNumberInfo::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting("12345", TestRegionCode::FR()),
          TestRegionCode::FR()));
  PhoneNumber unknown_cost_number;
  unknown_cost_number.set_country_code(33);
  unknown_cost_number.set_national_number(12345ULL);
  EXPECT_EQ(ShortNumberInfo::UNKNOWN_COST,
     short_info_.GetExpectedCost(unknown_cost_number));

  // Test that an invalid number may nevertheless have a cost other than
  // UNKNOWN_COST.
  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting("116123", TestRegionCode::FR()),
      TestRegionCode::FR()));
  EXPECT_EQ(
      ShortNumberInfo::TOLL_FREE,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting("116123", TestRegionCode::FR()),
          TestRegionCode::FR()));
  PhoneNumber invalid_number;
  invalid_number.set_country_code(33);
  invalid_number.set_national_number(116123ULL);
  EXPECT_FALSE(short_info_.IsValidShortNumber(invalid_number));
  EXPECT_EQ(ShortNumberInfo::TOLL_FREE,
      short_info_.GetExpectedCost(invalid_number));

  // Test a nonexistent country code.
  EXPECT_EQ(
      ShortNumberInfo::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting("911", TestRegionCode::US()),
          TestRegionCode::ZZ()));
  unknown_cost_number.Clear();
  unknown_cost_number.set_country_code(123);
  unknown_cost_number.set_national_number(911ULL);
  EXPECT_EQ(ShortNumberInfo::UNKNOWN_COST,
      short_info_.GetExpectedCost(unknown_cost_number));
}

TEST_F(ShortNumberInfoTest, GetExpectedCostForSharedCountryCallingCode) {
  // Test some numbers which have different costs in countries sharing the same
  // country calling code. In Australia, 1234 is premium-rate, 1194 is
  // standard-rate, and 733 is toll-free. These are not known to be valid
  // numbers in the Christmas Islands.
  string ambiguous_premium_rate_string("1234");
  PhoneNumber ambiguous_premium_rate_number;
  ambiguous_premium_rate_number.set_country_code(61);
  ambiguous_premium_rate_number.set_national_number(1234ULL);
  string ambiguous_standard_rate_string("1194");
  PhoneNumber ambiguous_standard_rate_number;
  ambiguous_standard_rate_number.set_country_code(61);
  ambiguous_standard_rate_number.set_national_number(1194ULL);
  string ambiguous_toll_free_string("733");
  PhoneNumber ambiguous_toll_free_number;
  ambiguous_toll_free_number.set_country_code(61);
  ambiguous_toll_free_number.set_national_number(733ULL);

  EXPECT_TRUE(short_info_.IsValidShortNumber(ambiguous_premium_rate_number));
  EXPECT_TRUE(short_info_.IsValidShortNumber(ambiguous_standard_rate_number));
  EXPECT_TRUE(short_info_.IsValidShortNumber(ambiguous_toll_free_number));

  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting(ambiguous_premium_rate_string,
                            TestRegionCode::AU()),
      TestRegionCode::AU()));
  EXPECT_EQ(ShortNumberInfo::PREMIUM_RATE,
            short_info_.GetExpectedCostForRegion(
                ParseNumberForTesting(ambiguous_premium_rate_string,
                                      TestRegionCode::AU()),
                TestRegionCode::AU()));

  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting(ambiguous_premium_rate_string,
                            TestRegionCode::CX()),
      TestRegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::UNKNOWN_COST,
            short_info_.GetExpectedCostForRegion(
                ParseNumberForTesting(ambiguous_premium_rate_string,
                                      TestRegionCode::CX()),
                TestRegionCode::CX()));
  // PREMIUM_RATE takes precedence over UNKNOWN_COST.
  EXPECT_EQ(ShortNumberInfo::PREMIUM_RATE,
      short_info_.GetExpectedCost(ambiguous_premium_rate_number));

  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting(ambiguous_standard_rate_string,
                            TestRegionCode::AU()),
      TestRegionCode::AU()));
  EXPECT_EQ(ShortNumberInfo::STANDARD_RATE,
            short_info_.GetExpectedCostForRegion(
                ParseNumberForTesting(ambiguous_standard_rate_string,
                                      TestRegionCode::AU()),
                TestRegionCode::AU()));

  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting(ambiguous_standard_rate_string,
                            TestRegionCode::CX()),
      TestRegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::UNKNOWN_COST,
            short_info_.GetExpectedCostForRegion(
                ParseNumberForTesting(ambiguous_standard_rate_string,
                                      TestRegionCode::CX()),
                TestRegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::UNKNOWN_COST,
      short_info_.GetExpectedCost(ambiguous_standard_rate_number));

  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting(ambiguous_toll_free_string, TestRegionCode::AU()),
      TestRegionCode::AU()));
  EXPECT_EQ(
      ShortNumberInfo::TOLL_FREE,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting(ambiguous_toll_free_string,
                                TestRegionCode::AU()),
          TestRegionCode::AU()));

  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting(ambiguous_toll_free_string, TestRegionCode::CX()),
      TestRegionCode::CX()));
  EXPECT_EQ(
      ShortNumberInfo::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting(ambiguous_toll_free_string,
                                TestRegionCode::CX()),
          TestRegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::UNKNOWN_COST,
      short_info_.GetExpectedCost(ambiguous_toll_free_number));
}

TEST_F(ShortNumberInfoTest, GetExampleShortNumber) {
  EXPECT_EQ("8711", short_info_.GetExampleShortNumber(TestRegionCode::AM()));
  EXPECT_EQ("1010", short_info_.GetExampleShortNumber(TestRegionCode::FR()));
  EXPECT_EQ("", short_info_.GetExampleShortNumber(TestRegionCode::UN001()));
  EXPECT_EQ("", short_info_.GetExampleShortNumber(
      TestRegionCode::GetUnknown()));
}

TEST_F(ShortNumberInfoTest, GetExampleShortNumberForCost) {
  EXPECT_EQ("3010",
      short_info_.GetExampleShortNumberForCost(TestRegionCode::FR(),
      ShortNumberInfo::TOLL_FREE));
  EXPECT_EQ("1023",
      short_info_.GetExampleShortNumberForCost(TestRegionCode::FR(),
      ShortNumberInfo::STANDARD_RATE));
  EXPECT_EQ("42000",
      short_info_.GetExampleShortNumberForCost(TestRegionCode::FR(),
      ShortNumberInfo::PREMIUM_RATE));
  EXPECT_EQ("", short_info_.GetExampleShortNumberForCost(TestRegionCode::FR(),
      ShortNumberInfo::UNKNOWN_COST));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_US) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("911",
      TestRegionCode::US()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("112",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("999",
      TestRegionCode::US()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberLongNumber_US) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("9116666666",
      TestRegionCode::US()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("1126666666",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("9996666666",
      TestRegionCode::US()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberWithFormatting_US) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("9-1-1",
      TestRegionCode::US()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("1-1-2",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("9-9-9",
      TestRegionCode::US()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberWithPlusSign_US) {
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("+911",
      TestRegionCode::US()));
  // This hex sequence is the full-width plus sign U+FF0B.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("\xEF\xBC\x8B" "911",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber(" +911",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("+112",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("+999",
      TestRegionCode::US()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_BR) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("911",
      TestRegionCode::BR()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("190",
      TestRegionCode::BR()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("999",
      TestRegionCode::BR()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberLongNumber_BR) {
  // Brazilian emergency numbers don't work when additional digits are appended.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("9111",
      TestRegionCode::BR()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("1900",
      TestRegionCode::BR()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("9996",
      TestRegionCode::BR()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_CL) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("131",
      TestRegionCode::CL()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("133",
      TestRegionCode::CL()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberLongNumber_CL) {
  // Chilean emergency numbers don't work when additional digits are appended.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("1313",
      TestRegionCode::CL()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("1330",
      TestRegionCode::CL()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_AO) {
  // Angola doesn't have any metadata for emergency numbers in the test
  // metadata.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("911",
      TestRegionCode::AO()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("222123456",
      TestRegionCode::AO()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("923123456",
      TestRegionCode::AO()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_ZW) {
  // Zimbabwe doesn't have any metadata in the test metadata.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("911",
      TestRegionCode::ZW()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("01312345",
      TestRegionCode::ZW()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("0711234567",
      TestRegionCode::ZW()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumber_US) {
  EXPECT_TRUE(short_info_.IsEmergencyNumber("911", TestRegionCode::US()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("112", TestRegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("999", TestRegionCode::US()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumberLongNumber_US) {
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9116666666",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("1126666666",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9996666666",
      TestRegionCode::US()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumberWithFormatting_US) {
  EXPECT_TRUE(short_info_.IsEmergencyNumber("9-1-1", TestRegionCode::US()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("*911", TestRegionCode::US()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("1-1-2", TestRegionCode::US()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("*112", TestRegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9-9-9", TestRegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("*999", TestRegionCode::US()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumberWithPlusSign_US) {
  EXPECT_FALSE(short_info_.IsEmergencyNumber("+911", TestRegionCode::US()));
  // This hex sequence is the full-width plus sign U+FF0B.
  EXPECT_FALSE(short_info_.IsEmergencyNumber("\xEF\xBC\x8B" "911",
      TestRegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber(" +911", TestRegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("+112", TestRegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("+999", TestRegionCode::US()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumber_BR) {
  EXPECT_TRUE(short_info_.IsEmergencyNumber("911", TestRegionCode::BR()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("190", TestRegionCode::BR()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("999", TestRegionCode::BR()));
}

TEST_F(ShortNumberInfoTest, EmergencyNumberLongNumber_BR) {
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9111", TestRegionCode::BR()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("1900", TestRegionCode::BR()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9996", TestRegionCode::BR()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumber_AO) {
  // Angola doesn't have any metadata for emergency numbers in the test
  // metadata.
  EXPECT_FALSE(short_info_.IsEmergencyNumber("911", TestRegionCode::AO()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("222123456",
      TestRegionCode::AO()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("923123456",
      TestRegionCode::AO()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumber_ZW) {
  // Zimbabwe doesn't have any metadata in the test metadata.
  EXPECT_FALSE(short_info_.IsEmergencyNumber("911", TestRegionCode::ZW()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("01312345", TestRegionCode::ZW()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("0711234567",
      TestRegionCode::ZW()));
}

TEST_F(ShortNumberInfoTest, EmergencyNumberForSharedCountryCallingCode) {
  // Test the emergency number 112, which is valid in both Australia and the
  // Christmas Islands.
  EXPECT_TRUE(short_info_.IsEmergencyNumber("112", TestRegionCode::AU()));
  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting("112", TestRegionCode::AU()),
      TestRegionCode::AU()));
  EXPECT_EQ(
      ShortNumberInfo::TOLL_FREE,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting("112", TestRegionCode::AU()),
          TestRegionCode::AU()));

  EXPECT_TRUE(short_info_.IsEmergencyNumber("112", TestRegionCode::CX()));
  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion(
      ParseNumberForTesting("112", TestRegionCode::CX()),
      TestRegionCode::CX()));
  EXPECT_EQ(
      ShortNumberInfo::TOLL_FREE,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting("112", TestRegionCode::CX()),
          TestRegionCode::CX()));

  PhoneNumber shared_emergency_number;
  shared_emergency_number.set_country_code(61);
  shared_emergency_number.set_national_number(112ULL);
  EXPECT_TRUE(short_info_.IsValidShortNumber(shared_emergency_number));
  EXPECT_EQ(ShortNumberInfo::TOLL_FREE,
      short_info_.GetExpectedCost(shared_emergency_number));
}

TEST_F(ShortNumberInfoTest, OverlappingNANPANumber) {
  // 211 is an emergency number in Barbados, while it is a toll-free
  // information line in Canada and the USA.
  EXPECT_TRUE(short_info_.IsEmergencyNumber("211", TestRegionCode::BB()));
  EXPECT_EQ(
      ShortNumberInfo::TOLL_FREE,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting("211", TestRegionCode::BB()),
          TestRegionCode::BB()));

  EXPECT_FALSE(short_info_.IsEmergencyNumber("211", TestRegionCode::US()));
  EXPECT_EQ(
      ShortNumberInfo::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting("211", TestRegionCode::US()),
          TestRegionCode::US()));

  EXPECT_FALSE(short_info_.IsEmergencyNumber("211", TestRegionCode::CA()));
  EXPECT_EQ(
      ShortNumberInfo::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion(
          ParseNumberForTesting("211", TestRegionCode::CA()),
          TestRegionCode::CA()));
}

}  // namespace phonenumbers
}  // namespace i18n
