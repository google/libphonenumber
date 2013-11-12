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

#include "phonenumbers/default_logger.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/test_util.h"

namespace i18n {
namespace phonenumbers {

class ShortNumberInfoTest : public testing::Test {
 protected:
  ShortNumberInfoTest() : short_info_() {
    PhoneNumberUtil::GetInstance()->SetLogger(new StdoutLogger());
  }

  const ShortNumberInfo short_info_;

 private:
  DISALLOW_COPY_AND_ASSIGN(ShortNumberInfoTest);
};

TEST_F(ShortNumberInfoTest, IsPossibleShortNumber) {
  PhoneNumber possible_number;
  possible_number.set_country_code(33);
  possible_number.set_national_number(123456ULL);
  EXPECT_TRUE(short_info_.IsPossibleShortNumber(possible_number));
  EXPECT_TRUE(short_info_.IsPossibleShortNumberForRegion("123456",
      RegionCode::FR()));

  PhoneNumber impossible_number;
  impossible_number.set_country_code(33);
  impossible_number.set_national_number(9ULL);
  EXPECT_FALSE(short_info_.IsPossibleShortNumber(impossible_number));
  EXPECT_FALSE(short_info_.IsPossibleShortNumberForRegion("9",
      RegionCode::FR()));

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
  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion("1010",
      RegionCode::FR()));

  PhoneNumber invalid_number;
  invalid_number.set_country_code(33);
  invalid_number.set_national_number(123456ULL);
  EXPECT_FALSE(short_info_.IsValidShortNumber(invalid_number));
  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion("123456",
      RegionCode::FR()));

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
          RegionCode::FR(), ShortNumberInfo::ShortNumberCost::PREMIUM_RATE);
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::PREMIUM_RATE,
      short_info_.GetExpectedCostForRegion(premium_rate_example,
          RegionCode::FR()));
  PhoneNumber premium_rate_number;
  premium_rate_number.set_country_code(33);
  safe_strtou64(premium_rate_example, &national_number);
  premium_rate_number.set_national_number(national_number);
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::PREMIUM_RATE,
     short_info_.GetExpectedCost(premium_rate_number));

  const string& standard_rate_example =
      short_info_.GetExampleShortNumberForCost(
          RegionCode::FR(), ShortNumberInfo::ShortNumberCost::STANDARD_RATE);
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::STANDARD_RATE,
      short_info_.GetExpectedCostForRegion(standard_rate_example,
          RegionCode::FR()));
  PhoneNumber standard_rate_number;
  standard_rate_number.set_country_code(33);
  safe_strtou64(standard_rate_example, &national_number);
  standard_rate_number.set_national_number(national_number);
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::STANDARD_RATE,
     short_info_.GetExpectedCost(standard_rate_number));

  const string& toll_free_example =
      short_info_.GetExampleShortNumberForCost(
          RegionCode::FR(), ShortNumberInfo::ShortNumberCost::TOLL_FREE);
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
      short_info_.GetExpectedCostForRegion(toll_free_example,
          RegionCode::FR()));
  PhoneNumber toll_free_number;
  toll_free_number.set_country_code(33);
  safe_strtou64(toll_free_example, &national_number);
  toll_free_number.set_national_number(national_number);
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
     short_info_.GetExpectedCost(toll_free_number));

  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion("12345", RegionCode::FR()));
  PhoneNumber unknown_cost_number;
  unknown_cost_number.set_country_code(33);
  unknown_cost_number.set_national_number(12345ULL);
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
     short_info_.GetExpectedCost(unknown_cost_number));

  // Test that an invalid number may nevertheless have a cost other than
  // UNKNOWN_COST.
  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion("116123",
      RegionCode::FR()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
      short_info_.GetExpectedCostForRegion("116123", RegionCode::FR()));
  PhoneNumber invalid_number;
  invalid_number.set_country_code(33);
  invalid_number.set_national_number(116123ULL);
  EXPECT_FALSE(short_info_.IsValidShortNumber(invalid_number));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
      short_info_.GetExpectedCost(invalid_number));

  // Test a nonexistent country code.
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion("911", RegionCode::ZZ()));
  unknown_cost_number.clear();
  unknown_cost_number.set_country_code(123);
  unknown_cost_number.set_national_number(911ULL);
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
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
      ambiguous_premium_rate_string, RegionCode::AU()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::PREMIUM_RATE,
      short_info_.GetExpectedCostForRegion(ambiguous_premium_rate_string,
          RegionCode::AU()));
  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion(
      ambiguous_premium_rate_string, RegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion(ambiguous_premium_rate_string,
          RegionCode::CX()));
  // PREMIUM_RATE takes precedence over UNKNOWN_COST.
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::PREMIUM_RATE,
      short_info_.GetExpectedCost(ambiguous_premium_rate_number));

  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion(
      ambiguous_standard_rate_string, RegionCode::AU()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::STANDARD_RATE,
      short_info_.GetExpectedCostForRegion(ambiguous_standard_rate_string,
          RegionCode::AU()));
  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion(
      ambiguous_standard_rate_string, RegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion(ambiguous_standard_rate_string,
          RegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCost(ambiguous_standard_rate_number));

  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion(
      ambiguous_toll_free_string, RegionCode::AU()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
      short_info_.GetExpectedCostForRegion(ambiguous_toll_free_string,
          RegionCode::AU()));
  EXPECT_FALSE(short_info_.IsValidShortNumberForRegion(
      ambiguous_toll_free_string, RegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion(ambiguous_toll_free_string,
          RegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCost(ambiguous_toll_free_number));
}

TEST_F(ShortNumberInfoTest, GetExampleShortNumber) {
  EXPECT_EQ("8711", short_info_.GetExampleShortNumber(RegionCode::AM()));
  EXPECT_EQ("1010", short_info_.GetExampleShortNumber(RegionCode::FR()));
  EXPECT_EQ("", short_info_.GetExampleShortNumber(RegionCode::UN001()));
  EXPECT_EQ("", short_info_.GetExampleShortNumber(RegionCode::GetUnknown()));
}

TEST_F(ShortNumberInfoTest, GetExampleShortNumberForCost) {
  EXPECT_EQ("3010",
      short_info_.GetExampleShortNumberForCost(RegionCode::FR(),
      ShortNumberInfo::ShortNumberCost::TOLL_FREE));
  EXPECT_EQ("1023",
      short_info_.GetExampleShortNumberForCost(RegionCode::FR(),
      ShortNumberInfo::ShortNumberCost::STANDARD_RATE));
  EXPECT_EQ("42000",
      short_info_.GetExampleShortNumberForCost(RegionCode::FR(),
      ShortNumberInfo::ShortNumberCost::PREMIUM_RATE));
  EXPECT_EQ("", short_info_.GetExampleShortNumberForCost(RegionCode::FR(),
      ShortNumberInfo::ShortNumberCost::UNKNOWN_COST));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_US) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("911", RegionCode::US()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("112", RegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("999", RegionCode::US()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberLongNumber_US) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("9116666666",
      RegionCode::US()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("1126666666",
      RegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("9996666666",
      RegionCode::US()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberWithFormatting_US) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("9-1-1", RegionCode::US()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("1-1-2", RegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("9-9-9",
      RegionCode::US()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberWithPlusSign_US) {
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("+911", RegionCode::US()));
  // This hex sequence is the full-width plus sign U+FF0B.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("\xEF\xBC\x8B" "911",
      RegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber(" +911",
      RegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("+112", RegionCode::US()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("+999", RegionCode::US()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_BR) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("911", RegionCode::BR()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("190", RegionCode::BR()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("999", RegionCode::BR()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberLongNumber_BR) {
  // Brazilian emergency numbers don't work when additional digits are appended.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("9111", RegionCode::BR()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("1900", RegionCode::BR()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("9996", RegionCode::BR()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_CL) {
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("131", RegionCode::CL()));
  EXPECT_TRUE(short_info_.ConnectsToEmergencyNumber("133", RegionCode::CL()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumberLongNumber_CL) {
  // Chilean emergency numbers don't work when additional digits are appended.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("1313", RegionCode::CL()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("1330", RegionCode::CL()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_AO) {
  // Angola doesn't have any metadata for emergency numbers in the test
  // metadata.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("911", RegionCode::AO()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("222123456",
      RegionCode::AO()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("923123456",
      RegionCode::AO()));
}

TEST_F(ShortNumberInfoTest, ConnectsToEmergencyNumber_ZW) {
  // Zimbabwe doesn't have any metadata in the test metadata.
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("911", RegionCode::ZW()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("01312345",
      RegionCode::ZW()));
  EXPECT_FALSE(short_info_.ConnectsToEmergencyNumber("0711234567",
      RegionCode::ZW()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumber_US) {
  EXPECT_TRUE(short_info_.IsEmergencyNumber("911", RegionCode::US()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("112", RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("999", RegionCode::US()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumberLongNumber_US) {
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9116666666", RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("1126666666", RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9996666666", RegionCode::US()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumberWithFormatting_US) {
  EXPECT_TRUE(short_info_.IsEmergencyNumber("9-1-1", RegionCode::US()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("*911", RegionCode::US()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("1-1-2", RegionCode::US()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("*112", RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9-9-9", RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("*999", RegionCode::US()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumberWithPlusSign_US) {
  EXPECT_FALSE(short_info_.IsEmergencyNumber("+911", RegionCode::US()));
  // This hex sequence is the full-width plus sign U+FF0B.
  EXPECT_FALSE(short_info_.IsEmergencyNumber("\xEF\xBC\x8B" "911",
      RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber(" +911", RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("+112", RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("+999", RegionCode::US()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumber_BR) {
  EXPECT_TRUE(short_info_.IsEmergencyNumber("911", RegionCode::BR()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("190", RegionCode::BR()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("999", RegionCode::BR()));
}

TEST_F(ShortNumberInfoTest, EmergencyNumberLongNumber_BR) {
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9111", RegionCode::BR()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("1900", RegionCode::BR()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("9996", RegionCode::BR()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumber_AO) {
  // Angola doesn't have any metadata for emergency numbers in the test
  // metadata.
  EXPECT_FALSE(short_info_.IsEmergencyNumber("911", RegionCode::AO()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("222123456", RegionCode::AO()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("923123456", RegionCode::AO()));
}

TEST_F(ShortNumberInfoTest, IsEmergencyNumber_ZW) {
  // Zimbabwe doesn't have any metadata in the test metadata.
  EXPECT_FALSE(short_info_.IsEmergencyNumber("911", RegionCode::ZW()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("01312345", RegionCode::ZW()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("0711234567", RegionCode::ZW()));
}

TEST_F(ShortNumberInfoTest, EmergencyNumberForSharedCountryCallingCode) {
  // Test the emergency number 112, which is valid in both Australia and the
  // Christmas Islands.
  EXPECT_TRUE(short_info_.IsEmergencyNumber("112", RegionCode::AU()));
  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion("112", RegionCode::AU()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
      short_info_.GetExpectedCostForRegion("112", RegionCode::AU()));
  EXPECT_TRUE(short_info_.IsEmergencyNumber("112", RegionCode::CX()));
  EXPECT_TRUE(short_info_.IsValidShortNumberForRegion("112", RegionCode::CX()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
      short_info_.GetExpectedCostForRegion("112", RegionCode::CX()));
  PhoneNumber shared_emergency_number;
  shared_emergency_number.set_country_code(61);
  shared_emergency_number.set_national_number(112ULL);
  EXPECT_TRUE(short_info_.IsValidShortNumber(shared_emergency_number));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
      short_info_.GetExpectedCost(shared_emergency_number));
}

TEST_F(ShortNumberInfoTest, OverlappingNANPANumber) {
  // 211 is an emergency number in Barbados, while it is a toll-free
  // information line in Canada and the USA.
  EXPECT_TRUE(short_info_.IsEmergencyNumber("211", RegionCode::BB()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::TOLL_FREE,
      short_info_.GetExpectedCostForRegion("211", RegionCode::BB()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("211", RegionCode::US()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion("211", RegionCode::US()));
  EXPECT_FALSE(short_info_.IsEmergencyNumber("211", RegionCode::CA()));
  EXPECT_EQ(ShortNumberInfo::ShortNumberCost::UNKNOWN_COST,
      short_info_.GetExpectedCostForRegion("211", RegionCode::CA()));
}

}  // namespace phonenumbers
}  // namespace i18n
