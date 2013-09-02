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

#include "phonenumbers/default_logger.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/shortnumberinfo.h"
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

}  // namespace phonenumbers
}  // namespace i18n
