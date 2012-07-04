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

#include <gtest/gtest.h>

#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/shortnumberutil.h"
#include "phonenumbers/test_util.h"

namespace i18n {
namespace phonenumbers {

class ShortNumberUtilTest : public testing::Test {
 protected:
  ShortNumberUtilTest() : short_util_() {
  }

  const ShortNumberUtil short_util_;

 private:
  DISALLOW_COPY_AND_ASSIGN(ShortNumberUtilTest);
};

TEST_F(ShortNumberUtilTest, ConnectsToEmergencyNumber_US) {
  EXPECT_TRUE(short_util_.ConnectsToEmergencyNumber("911", RegionCode::US()));
  EXPECT_TRUE(short_util_.ConnectsToEmergencyNumber("119", RegionCode::US()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("999", RegionCode::US()));
}

TEST_F(ShortNumberUtilTest, ConnectsToEmergencyNumberLongNumber_US) {
  EXPECT_TRUE(short_util_.ConnectsToEmergencyNumber("9116666666",
      RegionCode::US()));
  EXPECT_TRUE(short_util_.ConnectsToEmergencyNumber("1196666666",
      RegionCode::US()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("9996666666",
      RegionCode::US()));
}

TEST_F(ShortNumberUtilTest, ConnectsToEmergencyNumberWithFormatting_US) {
  EXPECT_TRUE(short_util_.ConnectsToEmergencyNumber("9-1-1", RegionCode::US()));
  EXPECT_TRUE(short_util_.ConnectsToEmergencyNumber("1-1-9", RegionCode::US()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("9-9-9",
      RegionCode::US()));
}

TEST_F(ShortNumberUtilTest, ConnectsToEmergencyNumberWithPlusSign_US) {
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("+911", RegionCode::US()));
  // This hex sequence is the full-width plus sign U+FF0B.
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("\xEF\xBC\x8B" "911",
      RegionCode::US()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber(" +911",
      RegionCode::US()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("+119", RegionCode::US()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("+999", RegionCode::US()));
}

TEST_F(ShortNumberUtilTest, ConnectsToEmergencyNumber_BR) {
  EXPECT_TRUE(short_util_.ConnectsToEmergencyNumber("911", RegionCode::BR()));
  EXPECT_TRUE(short_util_.ConnectsToEmergencyNumber("190", RegionCode::BR()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("999", RegionCode::BR()));
}

TEST_F(ShortNumberUtilTest, ConnectsToEmergencyNumberLongNumber_BR) {
  // Brazilian emergency numbers don't work when additional digits are appended.
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("9111", RegionCode::BR()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("1900", RegionCode::BR()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("9996", RegionCode::BR()));
}

TEST_F(ShortNumberUtilTest, ConnectsToEmergencyNumber_AO) {
  // Angola doesn't have any metadata for emergency numbers in the test
  // metadata.
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("911", RegionCode::AO()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("222123456",
      RegionCode::AO()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("923123456",
      RegionCode::AO()));
}

TEST_F(ShortNumberUtilTest, ConnectsToEmergencyNumber_ZW) {
  // Zimbabwe doesn't have any metadata in the test metadata.
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("911", RegionCode::ZW()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("01312345",
      RegionCode::ZW()));
  EXPECT_FALSE(short_util_.ConnectsToEmergencyNumber("0711234567",
      RegionCode::ZW()));
}

TEST_F(ShortNumberUtilTest, IsEmergencyNumber_US) {
  EXPECT_TRUE(short_util_.IsEmergencyNumber("911", RegionCode::US()));
  EXPECT_TRUE(short_util_.IsEmergencyNumber("119", RegionCode::US()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("999", RegionCode::US()));
}

TEST_F(ShortNumberUtilTest, IsEmergencyNumberLongNumber_US) {
  EXPECT_FALSE(short_util_.IsEmergencyNumber("9116666666", RegionCode::US()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("1196666666", RegionCode::US()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("9996666666", RegionCode::US()));
}

TEST_F(ShortNumberUtilTest, IsEmergencyNumberWithFormatting_US) {
  EXPECT_TRUE(short_util_.IsEmergencyNumber("9-1-1", RegionCode::US()));
  EXPECT_TRUE(short_util_.IsEmergencyNumber("*911", RegionCode::US()));
  EXPECT_TRUE(short_util_.IsEmergencyNumber("1-1-9", RegionCode::US()));
  EXPECT_TRUE(short_util_.IsEmergencyNumber("*119", RegionCode::US()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("9-9-9", RegionCode::US()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("*999", RegionCode::US()));
}

TEST_F(ShortNumberUtilTest, IsEmergencyNumberWithPlusSign_US) {
  EXPECT_FALSE(short_util_.IsEmergencyNumber("+911", RegionCode::US()));
  // This hex sequence is the full-width plus sign U+FF0B.
  EXPECT_FALSE(short_util_.IsEmergencyNumber("\xEF\xBC\x8B" "911",
      RegionCode::US()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber(" +911", RegionCode::US()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("+119", RegionCode::US()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("+999", RegionCode::US()));
}

TEST_F(ShortNumberUtilTest, IsEmergencyNumber_BR) {
  EXPECT_TRUE(short_util_.IsEmergencyNumber("911", RegionCode::BR()));
  EXPECT_TRUE(short_util_.IsEmergencyNumber("190", RegionCode::BR()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("999", RegionCode::BR()));
}

TEST_F(ShortNumberUtilTest, EmergencyNumberLongNumber_BR) {
  EXPECT_FALSE(short_util_.IsEmergencyNumber("9111", RegionCode::BR()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("1900", RegionCode::BR()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("9996", RegionCode::BR()));
}

TEST_F(ShortNumberUtilTest, IsEmergencyNumber_AO) {
  // Angola doesn't have any metadata for emergency numbers in the test
  // metadata.
  EXPECT_FALSE(short_util_.IsEmergencyNumber("911", RegionCode::AO()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("222123456", RegionCode::AO()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("923123456", RegionCode::AO()));
}

TEST_F(ShortNumberUtilTest, IsEmergencyNumber_ZW) {
  // Zimbabwe doesn't have any metadata in the test metadata.
  EXPECT_FALSE(short_util_.IsEmergencyNumber("911", RegionCode::ZW()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("01312345", RegionCode::ZW()));
  EXPECT_FALSE(short_util_.IsEmergencyNumber("0711234567", RegionCode::ZW()));
}

}  // namespace phonenumbers
}  // namespace i18n
