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

// Unit tests for asyoutypeformatter.cc, ported from AsYouTypeFormatterTest.java

#include "phonenumbers/asyoutypeformatter.h"

#include <gtest/gtest.h>

#include "base/logging.h"
#include "base/memory/scoped_ptr.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/test_util.h"

namespace i18n {
namespace phonenumbers {

class PhoneMetadata;

class AsYouTypeFormatterTest : public testing::Test {
 protected:
  AsYouTypeFormatterTest() : phone_util_(*PhoneNumberUtil::GetInstance()) {
  }

  const PhoneMetadata* GetCurrentMetadata() const {
    return formatter_->current_metadata_;
  }

  int ConvertUnicodeStringPosition(const UnicodeString& s, int pos) const {
    return AsYouTypeFormatter::ConvertUnicodeStringPosition(s, pos);
  }

  const PhoneNumberUtil& phone_util_;
  scoped_ptr<AsYouTypeFormatter> formatter_;
  string result_;

 private:
  DISALLOW_COPY_AND_ASSIGN(AsYouTypeFormatterTest);
};

TEST_F(AsYouTypeFormatterTest, ConvertUnicodeStringPosition) {
  EXPECT_EQ(-1, ConvertUnicodeStringPosition(UnicodeString("12345"), 10));
  EXPECT_EQ(3, ConvertUnicodeStringPosition(UnicodeString("12345"), 3));
  EXPECT_EQ(0, ConvertUnicodeStringPosition(
      UnicodeString("\xEF\xBC\x95" /* "５" */), 0));
  EXPECT_EQ(4, ConvertUnicodeStringPosition(
      UnicodeString("0\xEF\xBC\x95""3" /* "0５3" */), 2));
  EXPECT_EQ(5, ConvertUnicodeStringPosition(
      UnicodeString("0\xEF\xBC\x95""3" /* "0５3" */), 3));
}

TEST_F(AsYouTypeFormatterTest, Constructor) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::US()));

  EXPECT_TRUE(GetCurrentMetadata() != NULL);
}

TEST_F(AsYouTypeFormatterTest, InvalidPlusSign) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::GetUnknown()));
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+4", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+48 ", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+48 8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+48 88", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+48 88 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+48 88 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+48 88 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+48 88 123 1", formatter_->InputDigit('1', &result_));
  // A plus sign can only appear at the beginning of the number; otherwise, no
  // formatting is applied.
  EXPECT_EQ("+48881231+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+48881231+2", formatter_->InputDigit('2', &result_));
}

TEST_F(AsYouTypeFormatterTest, TooLongNumberMatchingMultipleLeadingDigits) {
  // See http://code.google.com/p/libphonenumber/issues/detail?id=36
  // The bug occurred last time for countries which have two formatting rules
  // with exactly the same leading digits pattern but differ in length.
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::GetUnknown()));

  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+81 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+81 9", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("+81 90", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("+81 90 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+81 90 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+81 90 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+81 90 1234", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+81 90 1234 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+81 90 1234 56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+81 90 1234 567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+81 90 1234 5678", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+81 90 12 345 6789", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("+81901234567890", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("+819012345678901", formatter_->InputDigit('1', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_US) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::US()));

  EXPECT_EQ("6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("650 253", formatter_->InputDigit('3', &result_));

  // Note this is how a US local number (without area code) should be formatted.
  EXPECT_EQ("650 2532", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("650 253 222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("650 253 2222", formatter_->InputDigit('2', &result_));

  formatter_->Clear();
  EXPECT_EQ("1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("16", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("1 65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("1 650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("1 650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("1 650 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("1 650 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 253 222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 253 2222", formatter_->InputDigit('2', &result_));

  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("01", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 4", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("011 44 ", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("011 44 6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("011 44 61", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 44 6 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 44 6 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("011 44 6 123 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 44 6 123 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 44 6 123 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("011 44 6 123 123 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 44 6 123 123 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 44 6 123 123 123", formatter_->InputDigit('3', &result_));

  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("01", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("011 54 ", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("011 54 9", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("011 54 91", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 54 9 11", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 54 9 11 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 54 9 11 23", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("011 54 9 11 231", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 54 9 11 2312", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 54 9 11 2312 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 54 9 11 2312 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 54 9 11 2312 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("011 54 9 11 2312 1234", formatter_->InputDigit('4', &result_));

  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("01", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 24", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("011 244 ", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("011 244 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 244 28", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("011 244 280", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 244 280 0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 244 280 00", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 244 280 000", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 244 280 000 0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 244 280 000 00", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 244 280 000 000", formatter_->InputDigit('0', &result_));

  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+4", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+48 ", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+48 8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+48 88", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+48 88 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+48 88 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+48 88 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+48 88 123 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+48 88 123 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+48 88 123 12 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+48 88 123 12 12", formatter_->InputDigit('2', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_USFullWidthCharacters) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::US()));

  EXPECT_EQ("\xEF\xBC\x96" /* "６" */,
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x96" /* "６" */)[0],
                                   &result_));
  EXPECT_EQ("\xEF\xBC\x96\xEF\xBC\x95" /* "６５" */,
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x95" /* "５" */)[0],
                                   &result_));
  EXPECT_EQ("650",
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x90" /* "０" */)[0],
                                   &result_));
  EXPECT_EQ("650 2",
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x92" /* "２" */)[0],
                                   &result_));
  EXPECT_EQ("650 25",
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x95" /* "５" */)[0],
                                   &result_));
  EXPECT_EQ("650 253",
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x93" /* "３" */)[0],
                                   &result_));
  EXPECT_EQ("650 2532",
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x92" /* "２" */)[0],
                                   &result_));
  EXPECT_EQ("650 253 22",
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x92" /* "２" */)[0],
                                   &result_));
  EXPECT_EQ("650 253 222",
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x92" /* "２" */)[0],
                                   &result_));
  EXPECT_EQ("650 253 2222",
            formatter_->InputDigit(UnicodeString("\xEF\xBC\x92" /* "２" */)[0],
                                   &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_USMobileShortCode) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::US()));

  EXPECT_EQ("*", formatter_->InputDigit('*', &result_));
  EXPECT_EQ("*1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("*12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("*121", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("*121#", formatter_->InputDigit('#', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_USVanityNumber) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::US()));

  EXPECT_EQ("8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("80", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("800", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("800 ", formatter_->InputDigit(' ', &result_));
  EXPECT_EQ("800 M", formatter_->InputDigit('M', &result_));
  EXPECT_EQ("800 MY", formatter_->InputDigit('Y', &result_));
  EXPECT_EQ("800 MY ", formatter_->InputDigit(' ', &result_));
  EXPECT_EQ("800 MY A", formatter_->InputDigit('A', &result_));
  EXPECT_EQ("800 MY AP", formatter_->InputDigit('P', &result_));
  EXPECT_EQ("800 MY APP", formatter_->InputDigit('P', &result_));
  EXPECT_EQ("800 MY APPL", formatter_->InputDigit('L', &result_));
  EXPECT_EQ("800 MY APPLE", formatter_->InputDigit('E', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTFAndRememberPositionUS) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::US()));

  EXPECT_EQ("1", formatter_->InputDigitAndRememberPosition('1', &result_));
  EXPECT_EQ(1, formatter_->GetRememberedPosition());

  EXPECT_EQ("16", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("1 65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ(1, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 650", formatter_->InputDigitAndRememberPosition('0', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 25", formatter_->InputDigit('5', &result_));

  // Note the remembered position for digit "0" changes from 4 to 5, because a
  // space is now inserted in the front.
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 650 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("1 650 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 650 253 222", formatter_->InputDigitAndRememberPosition('2',
        &result_));
  EXPECT_EQ(13, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 650 253 2222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(13, formatter_->GetRememberedPosition());
  EXPECT_EQ("165025322222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(10, formatter_->GetRememberedPosition());
  EXPECT_EQ("1650253222222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(10, formatter_->GetRememberedPosition());

  formatter_->Clear();
  EXPECT_EQ("1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("16", formatter_->InputDigitAndRememberPosition('6', &result_));
  EXPECT_EQ(2, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("1 650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ(3, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ(3, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 650 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("1 650 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(3, formatter_->GetRememberedPosition());
  EXPECT_EQ("1 650 253 222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("1 650 253 2222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("165025322222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(2, formatter_->GetRememberedPosition());
  EXPECT_EQ("1650253222222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(2, formatter_->GetRememberedPosition());

  formatter_->Clear();
  EXPECT_EQ("6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("650 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("650 2532",
            formatter_->InputDigitAndRememberPosition('2', &result_));
  EXPECT_EQ(8, formatter_->GetRememberedPosition());
  EXPECT_EQ("650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(9, formatter_->GetRememberedPosition());
  EXPECT_EQ("650 253 222", formatter_->InputDigit('2', &result_));
  // No more formatting when semicolon is entered.
  EXPECT_EQ("650253222;", formatter_->InputDigit(';', &result_));
  EXPECT_EQ(7, formatter_->GetRememberedPosition());
  EXPECT_EQ("650253222;2", formatter_->InputDigit('2', &result_));

  formatter_->Clear();
  EXPECT_EQ("6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("650", formatter_->InputDigit('0', &result_));
  // No more formatting when users choose to do their own formatting.
  EXPECT_EQ("650-", formatter_->InputDigit('-', &result_));
  EXPECT_EQ("650-2", formatter_->InputDigitAndRememberPosition('2', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("650-25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("650-253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("650-253-", formatter_->InputDigit('-', &result_));
  EXPECT_EQ("650-253-2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("650-253-22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("650-253-222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("650-253-2222", formatter_->InputDigit('2', &result_));

  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("01", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 4", formatter_->InputDigitAndRememberPosition('4', &result_));
  EXPECT_EQ("011 48 ", formatter_->InputDigit('8', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("011 48 8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("011 48 88", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("011 48 88 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 48 88 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("011 48 88 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("011 48 88 123 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 48 88 123 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("011 48 88 123 12 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 48 88 123 12 12", formatter_->InputDigit('2', &result_));

  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+1 6", formatter_->InputDigitAndRememberPosition('6', &result_));
  EXPECT_EQ("+1 65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+1 650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ(4, formatter_->GetRememberedPosition());
  EXPECT_EQ("+1 650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(4, formatter_->GetRememberedPosition());
  EXPECT_EQ("+1 650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+1 650 253",
            formatter_->InputDigitAndRememberPosition('3', &result_));
  EXPECT_EQ("+1 650 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+1 650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+1 650 253 222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(10, formatter_->GetRememberedPosition());

  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+1 6", formatter_->InputDigitAndRememberPosition('6', &result_));
  EXPECT_EQ("+1 65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+1 650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ(4, formatter_->GetRememberedPosition());
  EXPECT_EQ("+1 650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ(4, formatter_->GetRememberedPosition());
  EXPECT_EQ("+1 650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+1 650 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+1 650 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+1 650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+1 650 253 222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+1650253222;", formatter_->InputDigit(';', &result_));
  EXPECT_EQ(3, formatter_->GetRememberedPosition());
}

TEST_F(AsYouTypeFormatterTest, AYTF_GBFixedLine) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::GB()));

  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("02", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("020", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("020 7", formatter_->InputDigitAndRememberPosition('7', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("020 70", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("020 703", formatter_->InputDigit('3', &result_));
  EXPECT_EQ(5, formatter_->GetRememberedPosition());
  EXPECT_EQ("020 7031", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("020 7031 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("020 7031 30", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("020 7031 300", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("020 7031 3000", formatter_->InputDigit('0', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_GBTollFree) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::GB()));

  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("08", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("080", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("080 7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("080 70", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("080 703", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("080 7031", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("080 7031 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("080 7031 30", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("080 7031 300", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("080 7031 3000", formatter_->InputDigit('0', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_GBPremiumRate) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::GB()));

  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("09", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("090", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("090 7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("090 70", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("090 703", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("090 7031", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("090 7031 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("090 7031 30", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("090 7031 300", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("090 7031 3000", formatter_->InputDigit('0', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_NZMobile) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::NZ()));

  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("02", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("021", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("02-11", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("02-112", formatter_->InputDigit('2', &result_));
  // Note the unittest is using fake metadata which might produce non-ideal
  // results.
  EXPECT_EQ("02-112 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("02-112 34", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("02-112 345", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("02-112 3456", formatter_->InputDigit('6', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_DE) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::DE()));

  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("03", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("030", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("030/1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("030/12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("030/123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("030/1234", formatter_->InputDigit('4', &result_));

  // 08021 2345
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("08", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("080", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("080 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("080 21", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("08021 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("08021 23", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("08021 234", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("08021 2345", formatter_->InputDigit('5', &result_));

  // 00 1 650 253 2250
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00 1 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("00 1 6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("00 1 65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("00 1 650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00 1 650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("00 1 650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("00 1 650 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("00 1 650 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("00 1 650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("00 1 650 253 222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("00 1 650 253 2222", formatter_->InputDigit('2', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_AR) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::AR()));

  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("01", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("011 70", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 703", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("011 7031", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 7031-3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("011 7031-30", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 7031-300", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("011 7031-3000", formatter_->InputDigit('0', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_ARMobile) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::AR()));

  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+54 ", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+54 9", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("+54 91", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+54 9 11", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+54 9 11 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+54 9 11 23", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+54 9 11 231", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+54 9 11 2312", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+54 9 11 2312 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+54 9 11 2312 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+54 9 11 2312 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+54 9 11 2312 1234", formatter_->InputDigit('4', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_KR) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::KR()));

  // +82 51 234 5678
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+82 ", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+82 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+82 51", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+82 51-2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+82 51-23", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+82 51-234", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+82 51-234-5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+82 51-234-56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+82 51-234-567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+82 51-234-5678", formatter_->InputDigit('8', &result_));

  // +82 2 531 5678
  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+82 ", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+82 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+82 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+82 2-53", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+82 2-531", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+82 2-531-5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+82 2-531-56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+82 2-531-567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+82 2-531-5678", formatter_->InputDigit('8', &result_));

  // +82 2 3665 5678
  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+82 ", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+82 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+82 23", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+82 2-36", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+82 2-366", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+82 2-3665", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+82 2-3665-5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+82 2-3665-56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+82 2-3665-567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+82 2-3665-5678", formatter_->InputDigit('8', &result_));

  // 02-114
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("02", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("021", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("02-11", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("02-114", formatter_->InputDigit('4', &result_));

  // 02-1300
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("02", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("021", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("02-13", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("02-130", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("02-1300", formatter_->InputDigit('0', &result_));

  // 011-456-7890
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("01", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011-4", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("011-45", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("011-456", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("011-456-7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("011-456-78", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("011-456-789", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("011-456-7890", formatter_->InputDigit('0', &result_));

  // 011-9876-7890
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("01", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011-9", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("011-98", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("011-987", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("011-9876", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("011-9876-7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("011-9876-78", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("011-9876-789", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("011-9876-7890", formatter_->InputDigit('0', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_MX) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::MX()));

  // +52 800 123 4567
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 ", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+52 80", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("+52 800", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("+52 800 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+52 800 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 800 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+52 800 123 4", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+52 800 123 45", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 800 123 456", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+52 800 123 4567", formatter_->InputDigit('7', &result_));

  // +52 55 1234 5678
  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 ", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 55", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 55 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+52 55 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 55 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+52 55 1234", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+52 55 1234 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 55 1234 56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+52 55 1234 567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+52 55 1234 5678", formatter_->InputDigit('8', &result_));

  // +52 212 345 6789
  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 ", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 21", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+52 212", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 212 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+52 212 34", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+52 212 345", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 212 345 6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+52 212 345 67", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+52 212 345 678", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+52 212 345 6789", formatter_->InputDigit('9', &result_));

  // +52 1 55 1234 5678
  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 ", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+52 15", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 1 55", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 1 55 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+52 1 55 12", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 1 55 123", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+52 1 55 1234", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+52 1 55 1234 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 1 55 1234 56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+52 1 55 1234 567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+52 1 55 1234 5678", formatter_->InputDigit('8', &result_));

  // +52 1 541 234 5678
  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 ", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+52 15", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 1 54", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+52 1 541", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+52 1 541 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+52 1 541 23", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+52 1 541 234", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+52 1 541 234 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+52 1 541 234 56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+52 1 541 234 567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+52 1 541 234 5678", formatter_->InputDigit('8', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_MultipleLeadingDigitPatterns) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::JP()));

  // +81 50 2345 6789
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+81 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+81 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+81 50", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("+81 50 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+81 50 23", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+81 50 234", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("+81 50 2345", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+81 50 2345 6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+81 50 2345 67", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+81 50 2345 678", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+81 50 2345 6789", formatter_->InputDigit('9', &result_));

  // +81 222 12 5678
  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+81 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+81 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+81 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+81 22 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+81 22 21", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+81 2221 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+81 222 12 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+81 222 12 56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+81 222 12 567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+81 222 12 5678", formatter_->InputDigit('8', &result_));

  // 011113
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("01", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011 11", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("011113", formatter_->InputDigit('3', &result_));

  // +81 3332 2 5678
  formatter_->Clear();
  EXPECT_EQ("+", formatter_->InputDigit('+', &result_));
  EXPECT_EQ("+8", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("+81 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("+81 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+81 33", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+81 33 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("+81 3332", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+81 3332 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("+81 3332 2 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("+81 3332 2 56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("+81 3332 2 567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("+81 3332 2 5678", formatter_->InputDigit('8', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_LongIDD_AU) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::AU()));
  // 0011 1 650 253 2250
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("001", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("0011", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("0011 1 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("0011 1 6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("0011 1 65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("0011 1 650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("0011 1 650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 1 650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("0011 1 650 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("0011 1 650 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 1 650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 1 650 253 222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 1 650 253 2222", formatter_->InputDigit('2', &result_));

  // 0011 81 3332 2 5678
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("001", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("0011", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("00118", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("0011 81 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("0011 81 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("0011 81 33", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("0011 81 33 3", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("0011 81 3332", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 81 3332 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 81 3332 2 5", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("0011 81 3332 2 56", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("0011 81 3332 2 567", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("0011 81 3332 2 5678", formatter_->InputDigit('8', &result_));

  // 0011 244 250 253 222
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("001", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("0011", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("00112", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("001124", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("0011 244 ", formatter_->InputDigit('4', &result_));
  EXPECT_EQ("0011 244 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 244 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("0011 244 250", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("0011 244 250 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 244 250 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("0011 244 250 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("0011 244 250 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 244 250 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("0011 244 250 253 222", formatter_->InputDigit('2', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_LongIDD_KR) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::KR()));
  // 00300 1 650 253 2250
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("003", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("0030", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00300", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00300 1 ", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("00300 1 6", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("00300 1 65", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("00300 1 650", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("00300 1 650 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("00300 1 650 25", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("00300 1 650 253", formatter_->InputDigit('3', &result_));
  EXPECT_EQ("00300 1 650 253 2", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("00300 1 650 253 22", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("00300 1 650 253 222", formatter_->InputDigit('2', &result_));
  EXPECT_EQ("00300 1 650 253 2222", formatter_->InputDigit('2', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_LongNDD_KR) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::KR()));
  // 08811-9876-7890
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("08", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("088", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("0881", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("08811", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("08811-9", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("08811-98", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("08811-987", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("08811-9876", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("08811-9876-7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("08811-9876-78", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("08811-9876-789", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("08811-9876-7890", formatter_->InputDigit('0', &result_));

  // 08500 11-9876-7890
  formatter_->Clear();
  EXPECT_EQ("0", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("08", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("085", formatter_->InputDigit('5', &result_));
  EXPECT_EQ("0850", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("08500 ", formatter_->InputDigit('0', &result_));
  EXPECT_EQ("08500 1", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("08500 11", formatter_->InputDigit('1', &result_));
  EXPECT_EQ("08500 11-9", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("08500 11-98", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("08500 11-987", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("08500 11-9876", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("08500 11-9876-7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("08500 11-9876-78", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("08500 11-9876-789", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("08500 11-9876-7890", formatter_->InputDigit('0', &result_));
}

TEST_F(AsYouTypeFormatterTest, AYTF_LongNDD_SG) {
  formatter_.reset(phone_util_.GetAsYouTypeFormatter(RegionCode::SG()));
  // 777777 9876 7890
  EXPECT_EQ("7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("77", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("777", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("7777", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("77777", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("777777 ", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("777777 9", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("777777 98", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("777777 987", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("777777 9876", formatter_->InputDigit('6', &result_));
  EXPECT_EQ("777777 9876 7", formatter_->InputDigit('7', &result_));
  EXPECT_EQ("777777 9876 78", formatter_->InputDigit('8', &result_));
  EXPECT_EQ("777777 9876 789", formatter_->InputDigit('9', &result_));
  EXPECT_EQ("777777 9876 7890", formatter_->InputDigit('0', &result_));
}

}  // namespace phonenumbers
}  // namespace i18n
