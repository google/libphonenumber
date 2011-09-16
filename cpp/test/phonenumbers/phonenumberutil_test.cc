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

// Author: Shaopeng Jia
// Author: Lara Rennie
// Open-sourced by: Philippe Liard

#include <iostream>
#include <set>
#include <string>

#include <gtest/gtest.h>

#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/phonenumberutil.h"

namespace i18n {
namespace phonenumbers {

using std::endl;
using std::make_pair;
using std::ostream;

using google::protobuf::RepeatedPtrField;

namespace {

// Class containing string constants of region codes for easier testing. This is
// intended to replace region_code.h for testing in this file, with more
// constants defined.
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
};

}  // namespace

class PhoneNumberUtilTest : public testing::Test {
 protected:
  PhoneNumberUtilTest() : phone_util_(*PhoneNumberUtil::GetInstance()) {
  }

  // Wrapper functions for private functions that we want to test.
  const PhoneMetadata* GetPhoneMetadata(const string& region_code) const {
    return phone_util_.GetMetadataForRegion(region_code);
  }

  void GetSupportedRegions(set<string>* regions) {
    phone_util_.GetSupportedRegions(regions);
  }

  void ExtractPossibleNumber(const string& number,
                             string* extracted_number) const {
    phone_util_.ExtractPossibleNumber(number, extracted_number);
  }

  bool IsViablePhoneNumber(const string& number) const {
    return phone_util_.IsViablePhoneNumber(number);
  }

  void Normalize(string* number) const {
    phone_util_.Normalize(number);
  }

  bool IsLeadingZeroPossible(int country_calling_code) const {
    return phone_util_.IsLeadingZeroPossible(country_calling_code);
  }

  PhoneNumber::CountryCodeSource MaybeStripInternationalPrefixAndNormalize(
      const string& possible_idd_prefix,
      string* number) const {
    return phone_util_.MaybeStripInternationalPrefixAndNormalize(
        possible_idd_prefix,
        number);
  }

  void MaybeStripNationalPrefixAndCarrierCode(const PhoneMetadata& metadata,
                                              string* number,
                                              string* carrier_code) const {
    phone_util_.MaybeStripNationalPrefixAndCarrierCode(metadata, number,
                                                            carrier_code);
  }

  bool MaybeStripExtension(string* number, string* extension) const {
    return phone_util_.MaybeStripExtension(number, extension);
  }

  PhoneNumberUtil::ErrorType MaybeExtractCountryCode(
      const PhoneMetadata* default_region_metadata,
      bool keep_raw_input,
      string* national_number,
      PhoneNumber* phone_number) const {
    return phone_util_.MaybeExtractCountryCode(default_region_metadata,
                                               keep_raw_input,
                                               national_number,
                                               phone_number);
  }

  static bool Equals(const PhoneNumberDesc& expected_number,
                     const PhoneNumberDesc& actual_number) {
    return ExactlySameAs(expected_number, actual_number);
  }

  void GetNddPrefixForRegion(const string& region,
                             bool strip_non_digits,
                             string* ndd_prefix) const {
    // For testing purposes, we check this is empty first.
    ndd_prefix->clear();
    phone_util_.GetNddPrefixForRegion(region, strip_non_digits, ndd_prefix);
  }

  const PhoneNumberUtil& phone_util_;
};

// Provides PhoneNumber comparison operators to support the use of EXPECT_EQ and
// EXPECT_NE in the unittests.
bool operator==(const PhoneNumber& number1, const PhoneNumber& number2) {
  return ExactlySameAs(number1, number2);
}

bool operator!=(const PhoneNumber& number1, const PhoneNumber& number2) {
  return !(number1 == number2);
}

// Needed by Google Test to display errors.
ostream& operator<<(ostream& os, const PhoneNumber& number) {
  os << endl
     << "country_code: " << number.country_code() << endl
     << "national_number: " << number.national_number() << endl;
  if (number.has_extension()) {
     os << "extension: " << number.extension() << endl;
  }
  if (number.has_italian_leading_zero()) {
     os << "italian_leading_zero: " << number.italian_leading_zero() << endl;
  }
  if (number.has_raw_input()) {
     os << "raw_input: " << number.raw_input() << endl;
  }
  if (number.has_country_code_source()) {
     os << "country_code_source: " << number.country_code_source() << endl;
  }
  if (number.has_preferred_domestic_carrier_code()) {
     os << "preferred_domestic_carrier_code: "
        << number.preferred_domestic_carrier_code() << endl;
  }
  return os;
}

TEST_F(PhoneNumberUtilTest, GetSupportedRegions) {
  set<string> regions;

  GetSupportedRegions(&regions);
  EXPECT_GT(regions.size(), 0U);
}

TEST_F(PhoneNumberUtilTest, GetInstanceLoadUSMetadata) {
  const PhoneMetadata* metadata = GetPhoneMetadata(RegionCode::US());
  EXPECT_EQ("US", metadata->id());
  EXPECT_EQ(1, metadata->country_code());
  EXPECT_EQ("011", metadata->international_prefix());
  EXPECT_TRUE(metadata->has_national_prefix());
  ASSERT_EQ(2, metadata->number_format_size());
  EXPECT_EQ("(\\d{3})(\\d{3})(\\d{4})",
            metadata->number_format(1).pattern());
  EXPECT_EQ("$1 $2 $3", metadata->number_format(1).format());
  EXPECT_EQ("[13-689]\\d{9}|2[0-35-9]\\d{8}",
            metadata->general_desc().national_number_pattern());
  EXPECT_EQ("\\d{7}(?:\\d{3})?",
            metadata->general_desc().possible_number_pattern());
  EXPECT_TRUE(Equals(metadata->general_desc(), metadata->fixed_line()));
  EXPECT_EQ("\\d{10}", metadata->toll_free().possible_number_pattern());
  EXPECT_EQ("900\\d{7}", metadata->premium_rate().national_number_pattern());
  // No shared-cost data is available, so it should be initialised to "NA".
  EXPECT_EQ("NA", metadata->shared_cost().national_number_pattern());
  EXPECT_EQ("NA", metadata->shared_cost().possible_number_pattern());
}

TEST_F(PhoneNumberUtilTest, GetInstanceLoadDEMetadata) {
  const PhoneMetadata* metadata = GetPhoneMetadata(RegionCode::DE());
  EXPECT_EQ("DE", metadata->id());
  EXPECT_EQ(49, metadata->country_code());
  EXPECT_EQ("00", metadata->international_prefix());
  EXPECT_EQ("0", metadata->national_prefix());
  ASSERT_EQ(6, metadata->number_format_size());
  EXPECT_EQ(1, metadata->number_format(5).leading_digits_pattern_size());
  EXPECT_EQ("900", metadata->number_format(5).leading_digits_pattern(0));
  EXPECT_EQ("(\\d{3})(\\d{3,4})(\\d{4})",
            metadata->number_format(5).pattern());
  EXPECT_EQ("$1 $2 $3", metadata->number_format(5).format());
  EXPECT_EQ("(?:[24-6]\\d{2}|3[03-9]\\d|[789](?:[1-9]\\d|0[2-9]))\\d{1,8}",
            metadata->fixed_line().national_number_pattern());
  EXPECT_EQ("\\d{2,14}", metadata->fixed_line().possible_number_pattern());
  EXPECT_EQ("30123456", metadata->fixed_line().example_number());
  EXPECT_EQ("\\d{10}", metadata->toll_free().possible_number_pattern());
  EXPECT_EQ("900([135]\\d{6}|9\\d{7})",
            metadata->premium_rate().national_number_pattern());
}

TEST_F(PhoneNumberUtilTest, GetInstanceLoadARMetadata) {
  const PhoneMetadata* metadata = GetPhoneMetadata(RegionCode::AR());
  EXPECT_EQ("AR", metadata->id());
  EXPECT_EQ(54, metadata->country_code());
  EXPECT_EQ("00", metadata->international_prefix());
  EXPECT_EQ("0", metadata->national_prefix());
  EXPECT_EQ("0(?:(11|343|3715)15)?", metadata->national_prefix_for_parsing());
  EXPECT_EQ("9$1", metadata->national_prefix_transform_rule());
  ASSERT_EQ(5, metadata->number_format_size());
  EXPECT_EQ("$2 15 $3-$4", metadata->number_format(2).format());
  EXPECT_EQ("(9)(\\d{4})(\\d{2})(\\d{4})",
            metadata->number_format(3).pattern());
  EXPECT_EQ("(9)(\\d{4})(\\d{2})(\\d{4})",
            metadata->intl_number_format(3).pattern());
  EXPECT_EQ("$1 $2 $3 $4", metadata->intl_number_format(3).format());
}

TEST_F(PhoneNumberUtilTest, GetNationalSignificantNumber) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(6502530000ULL);
  string national_significant_number;
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("6502530000", national_significant_number);

  // An Italian mobile number.
  national_significant_number.clear();
  number.set_country_code(39);
  number.set_national_number(312345678ULL);
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("312345678", national_significant_number);

  // An Italian fixed line number.
  national_significant_number.clear();
  number.set_country_code(39);
  number.set_national_number(236618300ULL);
  number.set_italian_leading_zero(true);
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("0236618300", national_significant_number);
}

TEST_F(PhoneNumberUtilTest, GetExampleNumber) {
  PhoneNumber de_number;
  de_number.set_country_code(49);
  de_number.set_national_number(30123456ULL);
  PhoneNumber test_number;
  bool success = phone_util_.GetExampleNumber(RegionCode::DE(), &test_number);
  EXPECT_TRUE(success);
  EXPECT_EQ(de_number, test_number);
  success = phone_util_.GetExampleNumberForType(RegionCode::DE(),
                                                PhoneNumberUtil::FIXED_LINE,
                                                &test_number);
  EXPECT_TRUE(success);
  EXPECT_EQ(de_number, test_number);
  test_number.Clear();
  success = phone_util_.GetExampleNumberForType(RegionCode::DE(),
                                                PhoneNumberUtil::MOBILE,
                                                &test_number);
  // Here we test that an example number was not returned, and that the number
  // passed in was not modified.
  EXPECT_FALSE(success);
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);
  // For the US, the example number is placed under general description, and
  // hence should be used for both fixed line and mobile, so neither of these
  // should return null.
  test_number.Clear();
  success = phone_util_.GetExampleNumberForType(RegionCode::US(),
                                                PhoneNumberUtil::FIXED_LINE,
                                                &test_number);
  // Here we test that the call to get an example number succeeded, and that the
  // number passed in was modified.
  EXPECT_TRUE(success);
  EXPECT_NE(PhoneNumber::default_instance(), test_number);
  test_number.Clear();
  success = phone_util_.GetExampleNumberForType(RegionCode::US(),
                                                PhoneNumberUtil::MOBILE,
                                                &test_number);
  EXPECT_TRUE(success);
  EXPECT_NE(PhoneNumber::default_instance(), test_number);

  test_number.Clear();
  // CS is an invalid region, so we have no data for it. We should return false.
  EXPECT_FALSE(phone_util_.GetExampleNumberForType(RegionCode::CS(),
                                                   PhoneNumberUtil::MOBILE,
                                                   &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);
}

TEST_F(PhoneNumberUtilTest, FormatUSNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(1);
  test_number.set_national_number(6502530000ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("650 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 650 253 0000", formatted_number);

  test_number.set_national_number(8002530000ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("800 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 800 253 0000", formatted_number);

  test_number.set_national_number(9002530000ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("900 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 900 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::RFC3966, &formatted_number);
  EXPECT_EQ("+1-900-253-0000", formatted_number);
  test_number.set_national_number(0ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("0", formatted_number);
  // Numbers with all zeros in the national number part will be formatted by
  // using the raw_input if that is available no matter which format is
  // specified.
  test_number.set_raw_input("000-000-0000");
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("000-000-0000", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatBSNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(1);
  test_number.set_national_number(2421234567ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("242 123 4567", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 242 123 4567", formatted_number);

  test_number.set_national_number(8002530000ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("800 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 800 253 0000", formatted_number);

  test_number.set_national_number(9002530000ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("900 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 900 253 0000", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatGBNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(44);
  test_number.set_national_number(2087389353ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("(020) 8738 9353", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+44 20 8738 9353", formatted_number);

  test_number.set_national_number(7912345678ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("(07912) 345 678", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+44 7912 345 678", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatDENumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(49);
  test_number.set_national_number(301234ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("030/1234", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 30/1234", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::RFC3966, &formatted_number);
  EXPECT_EQ("+49-30-1234", formatted_number);

  test_number.set_national_number(291123ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("0291 123", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 291 123", formatted_number);

  test_number.set_national_number(29112345678ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("0291 12345678", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 291 12345678", formatted_number);

  test_number.set_national_number(9123123ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("09123 123", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 9123 123", formatted_number);

  test_number.set_national_number(80212345ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("08021 2345", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 8021 2345", formatted_number);

  test_number.set_national_number(1234ULL);
  // Note this number is correctly formatted without national prefix. Most of
  // the numbers that are treated as invalid numbers by the library are short
  // numbers, and they are usually not dialed with national prefix.
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("1234", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 1234", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatITNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(39);
  test_number.set_national_number(236618300ULL);
  test_number.set_italian_leading_zero(true);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("02 3661 8300", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+39 02 3661 8300", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+390236618300", formatted_number);

  test_number.set_national_number(345678901ULL);
  test_number.set_italian_leading_zero(false);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("345 678 901", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+39 345 678 901", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+39345678901", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatAUNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(61);
  test_number.set_national_number(236618300ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("02 3661 8300", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+61 2 3661 8300", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+61236618300", formatted_number);

  test_number.set_national_number(1800123456ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("1800 123 456", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+61 1800 123 456", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+611800123456", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatARNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(54);
  test_number.set_national_number(1187654321ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("011 8765-4321", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+54 11 8765-4321", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+541187654321", formatted_number);

  test_number.set_national_number(91187654321ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("011 15 8765-4321", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+54 9 11 8765 4321", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+5491187654321", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatMXNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(52);
  test_number.set_national_number(12345678900ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("045 234 567 8900", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+52 1 234 567 8900", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+5212345678900", formatted_number);

  test_number.set_national_number(15512345678ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("045 55 1234 5678", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+52 1 55 1234 5678", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+5215512345678", formatted_number);

  test_number.set_national_number(3312345678LL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("01 33 1234 5678", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+52 33 1234 5678", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+523312345678", formatted_number);

  test_number.set_national_number(8211234567LL);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("01 821 123 4567", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+52 821 123 4567", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+528211234567", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatOutOfCountryCallingNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(1);
  test_number.set_national_number(9002530000ULL);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::DE(),
                                              &formatted_number);
  EXPECT_EQ("00 1 900 253 0000", formatted_number);

  test_number.set_national_number(6502530000ULL);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::BS(),
                                              &formatted_number);
  EXPECT_EQ("1 650 253 0000", formatted_number);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::PL(),
                                              &formatted_number);
  EXPECT_EQ("0~0 1 650 253 0000", formatted_number);

  test_number.set_country_code(44);
  test_number.set_national_number(7912345678ULL);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::US(),
                                              &formatted_number);
  EXPECT_EQ("011 44 7912 345 678", formatted_number);

  test_number.set_country_code(49);
  test_number.set_national_number(1234ULL);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::GB(),
                                              &formatted_number);
  EXPECT_EQ("00 49 1234", formatted_number);
  // Note this number is correctly formatted without national prefix. Most of
  // the numbers that are treated as invalid numbers by the library are short
  // numbers, and they are usually not dialed with national prefix.
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::DE(),
                                              &formatted_number);
  EXPECT_EQ("1234", formatted_number);

  test_number.set_country_code(39);
  test_number.set_national_number(236618300ULL);
  test_number.set_italian_leading_zero(true);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::US(),
                                              &formatted_number);
  EXPECT_EQ("011 39 02 3661 8300", formatted_number);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::IT(),
                                              &formatted_number);
  EXPECT_EQ("02 3661 8300", formatted_number);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::SG(),
                                              &formatted_number);
  EXPECT_EQ("+39 02 3661 8300", formatted_number);

  test_number.set_country_code(65);
  test_number.set_national_number(94777892ULL);
  test_number.set_italian_leading_zero(false);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::SG(),
                                              &formatted_number);
  EXPECT_EQ("9477 7892", formatted_number);

  test_number.set_country_code(54);
  test_number.set_national_number(91187654321ULL);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::US(),
                                              &formatted_number);
  EXPECT_EQ("011 54 9 11 8765 4321", formatted_number);

  test_number.set_extension("1234");
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::US(),
                                              &formatted_number);
  EXPECT_EQ("011 54 9 11 8765 4321 ext. 1234", formatted_number);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::AU(),
                                              &formatted_number);
  EXPECT_EQ("0011 54 9 11 8765 4321 ext. 1234", formatted_number);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::AR(),
                                              &formatted_number);
  EXPECT_EQ("011 15 8765-4321 ext. 1234", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatOutOfCountryWithPreferredIntlPrefix) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(39);
  test_number.set_national_number(236618300ULL);
  test_number.set_italian_leading_zero(true);
  // This should use 0011, since that is the preferred international prefix
  // (both 0011 and 0012 are accepted as possible international prefixes in our
  // test metadta.)
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::AU(),
                                              &formatted_number);
  EXPECT_EQ("0011 39 02 3661 8300", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatOutOfCountryKeepingAlphaChars) {
  PhoneNumber alpha_numeric_number;
  string formatted_number;
  alpha_numeric_number.set_country_code(1);
  alpha_numeric_number.set_national_number(8007493524ULL);
  alpha_numeric_number.set_raw_input("1800 six-flag");
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AU(),
                                                  &formatted_number);
  EXPECT_EQ("0011 1 800 SIX-FLAG", formatted_number);

  formatted_number.clear();
  alpha_numeric_number.set_raw_input("1-800-SIX-flag");
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AU(),
                                                  &formatted_number);
  EXPECT_EQ("0011 1 800-SIX-FLAG", formatted_number);

  formatted_number.clear();
  alpha_numeric_number.set_raw_input("Call us from UK: 00 1 800 SIX-flag");
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AU(),
                                                  &formatted_number);
  EXPECT_EQ("0011 1 800 SIX-FLAG", formatted_number);

  formatted_number.clear();
  alpha_numeric_number.set_raw_input("800 SIX-flag");
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AU(),
                                                  &formatted_number);
  EXPECT_EQ("0011 1 800 SIX-FLAG", formatted_number);

  // Formatting from within the NANPA region.
  formatted_number.clear();
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::US(),
                                                  &formatted_number);
  EXPECT_EQ("1 800 SIX-FLAG", formatted_number);
  formatted_number.clear();
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::BS(),
                                                  &formatted_number);
  EXPECT_EQ("1 800 SIX-FLAG", formatted_number);

  // Testing that if the raw input doesn't exist, it is formatted using
  // FormatOutOfCountryCallingNumber.
  alpha_numeric_number.clear_raw_input();
  formatted_number.clear();
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::DE(),
                                                  &formatted_number);
  EXPECT_EQ("00 1 800 749 3524", formatted_number);

  // Testing AU alpha number formatted from Australia.
  alpha_numeric_number.set_country_code(61);
  alpha_numeric_number.set_national_number(827493524ULL);
  alpha_numeric_number.set_raw_input("+61 82749-FLAG");
  formatted_number.clear();
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AU(),
                                                  &formatted_number);
  // This number should have the national prefix prefixed.
  EXPECT_EQ("082749-FLAG", formatted_number);

  alpha_numeric_number.set_raw_input("082749-FLAG");
  formatted_number.clear();
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AU(),
                                                  &formatted_number);
  EXPECT_EQ("082749-FLAG", formatted_number);

  alpha_numeric_number.set_national_number(18007493524ULL);
  alpha_numeric_number.set_raw_input("1-800-SIX-flag");
  formatted_number.clear();
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AU(),
                                                  &formatted_number);
  // This number should not have the national prefix prefixed, in accordance
  // with the override for this specific formatting rule.
  EXPECT_EQ("1-800-SIX-FLAG", formatted_number);
  // The metadata should not be permanently changed, since we copied it before
  // modifying patterns. Here we check this.
  formatted_number.clear();
  alpha_numeric_number.set_national_number(1800749352ULL);
  phone_util_.FormatOutOfCountryCallingNumber(alpha_numeric_number,
                                              RegionCode::AU(),
                                              &formatted_number);
  EXPECT_EQ("1800 749 352", formatted_number);

  // Testing a country with multiple international prefixes.
  formatted_number.clear();
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::SG(),
                                                  &formatted_number);
  EXPECT_EQ("+61 1-800-SIX-FLAG", formatted_number);

  // Testing the case with an invalid country code.
  formatted_number.clear();
  alpha_numeric_number.set_country_code(0);
  alpha_numeric_number.set_national_number(18007493524ULL);
  alpha_numeric_number.set_raw_input("1-800-SIX-flag");
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::DE(),
                                                  &formatted_number);
  // Uses the raw input only.
  EXPECT_EQ("1-800-SIX-flag", formatted_number);

  // Testing the case of an invalid alpha number.
  formatted_number.clear();
  alpha_numeric_number.set_country_code(1);
  alpha_numeric_number.set_national_number(80749ULL);
  alpha_numeric_number.set_raw_input("180-SIX");
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::DE(),
                                                  &formatted_number);
  // No country-code stripping can be done.
  EXPECT_EQ("00 1 180-SIX", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatWithCarrierCode) {
  // We only support this for AR in our test metadata.
  PhoneNumber ar_number;
  string formatted_number;
  ar_number.set_country_code(54);
  ar_number.set_national_number(91234125678ULL);
  phone_util_.Format(ar_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("01234 12-5678", formatted_number);
  // Test formatting with a carrier code.
  phone_util_.FormatNationalNumberWithCarrierCode(ar_number, "15",
                                                  &formatted_number);
  EXPECT_EQ("01234 15 12-5678", formatted_number);
  phone_util_.FormatNationalNumberWithCarrierCode(ar_number, "",
                                                  &formatted_number);
  EXPECT_EQ("01234 12-5678", formatted_number);
  // Here the international rule is used, so no carrier code should be present.
  phone_util_.Format(ar_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+5491234125678", formatted_number);
  // We don't support this for the US so there should be no change.
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(4241231234ULL);
  phone_util_.Format(us_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("424 123 1234", formatted_number);
  phone_util_.FormatNationalNumberWithCarrierCode(us_number, "15",
                                                  &formatted_number);
  EXPECT_EQ("424 123 1234", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatWithPreferredCarrierCode) {
  // We only support this for AR in our test metadata.
  PhoneNumber ar_number;
  string formatted_number;
  ar_number.set_country_code(54);
  ar_number.set_national_number(91234125678ULL);
  // Test formatting with no preferred carrier code stored in the number itself.
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(ar_number, "15",
                                                           &formatted_number);
  EXPECT_EQ("01234 15 12-5678", formatted_number);
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(ar_number, "",
                                                           &formatted_number);
  EXPECT_EQ("01234 12-5678", formatted_number);
  // Test formatting with preferred carrier code present.
  ar_number.set_preferred_domestic_carrier_code("19");
  phone_util_.Format(ar_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("01234 12-5678", formatted_number);
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(ar_number, "15",
                                                           &formatted_number);
  EXPECT_EQ("01234 19 12-5678", formatted_number);
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(ar_number, "",
                                                           &formatted_number);
  EXPECT_EQ("01234 19 12-5678", formatted_number);
  // When the preferred_domestic_carrier_code is present (even when it contains
  // an empty string), use it instead of the default carrier code passed in.
  ar_number.set_preferred_domestic_carrier_code("");
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(ar_number, "15",
                                                           &formatted_number);
  EXPECT_EQ("01234 12-5678", formatted_number);
  // We don't support this for the US so there should be no change.
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(4241231234ULL);
  us_number.set_preferred_domestic_carrier_code("99");
  phone_util_.Format(us_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("424 123 1234", formatted_number);
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(us_number, "15",
                                                           &formatted_number);
  EXPECT_EQ("424 123 1234", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatByPattern) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(1);
  test_number.set_national_number(6502530000ULL);

  RepeatedPtrField<NumberFormat> number_formats;
  NumberFormat* number_format = number_formats.Add();
  number_format->set_pattern("(\\d{3})(\\d{3})(\\d{4})");
  number_format->set_format("($1) $2-$3");
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::NATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("(650) 253-0000", formatted_number);
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::INTERNATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("+1 (650) 253-0000", formatted_number);

  // $NP is set to '1' for the US. Here we check that for other NANPA countries
  // the US rules are followed.
  number_format->set_national_prefix_formatting_rule("$NP ($FG)");
  number_format->set_format("$1 $2-$3");
  test_number.set_country_code(1);
  test_number.set_national_number(4168819999ULL);
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::NATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("1 (416) 881-9999", formatted_number);
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::INTERNATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("+1 416 881-9999", formatted_number);

  test_number.set_country_code(39);
  test_number.set_national_number(236618300ULL);
  test_number.set_italian_leading_zero(true);
  number_format->set_pattern("(\\d{2})(\\d{5})(\\d{3})");
  number_format->set_format("$1-$2 $3");
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::NATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("02-36618 300", formatted_number);
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::INTERNATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("+39 02-36618 300", formatted_number);

  test_number.set_country_code(44);
  test_number.set_national_number(2012345678ULL);
  test_number.set_italian_leading_zero(false);
  number_format->set_national_prefix_formatting_rule("$NP$FG");
  number_format->set_pattern("(\\d{2})(\\d{4})(\\d{4})");
  number_format->set_format("$1 $2 $3");
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::NATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("020 1234 5678", formatted_number);

  number_format->set_national_prefix_formatting_rule("($NP$FG)");
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::NATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("(020) 1234 5678", formatted_number);
  number_format->set_national_prefix_formatting_rule("");
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::NATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("20 1234 5678", formatted_number);
  number_format->set_national_prefix_formatting_rule("");
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::INTERNATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("+44 20 1234 5678", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatE164Number) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(1);
  test_number.set_national_number(6502530000ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+16502530000", formatted_number);

  test_number.set_country_code(49);
  test_number.set_national_number(301234ULL);
  phone_util_.Format(test_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+49301234", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatNumberWithExtension) {
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(33316005ULL);
  nz_number.set_extension("1234");
  string formatted_number;
  // Uses default extension prefix:
  phone_util_.Format(nz_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("03-331 6005 ext. 1234", formatted_number);
  // Uses RFC 3966 syntax.
  phone_util_.Format(nz_number, PhoneNumberUtil::RFC3966, &formatted_number);
  EXPECT_EQ("+64-3-331-6005;ext=1234", formatted_number);
  // Extension prefix overridden in the territory information for the US:
  PhoneNumber us_number_with_extension;
  us_number_with_extension.set_country_code(1);
  us_number_with_extension.set_national_number(6502530000ULL);
  us_number_with_extension.set_extension("4567");
  phone_util_.Format(us_number_with_extension,
                     PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("650 253 0000 extn. 4567", formatted_number);
}

TEST_F(PhoneNumberUtilTest, GetLengthOfGeographicalAreaCode) {
  PhoneNumber number;
  // Google MTV, which has area code "650".
  number.set_country_code(1);
  number.set_national_number(6502530000ULL);
  EXPECT_EQ(3, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // A North America toll-free number, which has no area code.
  number.set_country_code(1);
  number.set_national_number(8002530000ULL);
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // An invalid US number (1 digit shorter), which has no area code.
  number.set_country_code(1);
  number.set_national_number(650253000ULL);
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Google London, which has area code "20".
  number.set_country_code(44);
  number.set_national_number(2070313000ULL);
  EXPECT_EQ(2, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // A UK mobile phone, which has no area code.
  number.set_country_code(44);
  number.set_national_number(7123456789ULL);
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Google Buenos Aires, which has area code "11".
  number.set_country_code(54);
  number.set_national_number(1155303000ULL);
  EXPECT_EQ(2, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Google Sydney, which has area code "2".
  number.set_country_code(61);
  number.set_national_number(293744000ULL);
  EXPECT_EQ(1, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Google Singapore. Singapore has no area code and no national prefix.
  number.set_country_code(65);
  number.set_national_number(65218000ULL);
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));
}

TEST_F(PhoneNumberUtilTest, GetLengthOfNationalDestinationCode) {
  PhoneNumber number;
  // Google MTV, which has national destination code (NDC) "650".
  number.set_country_code(1);
  number.set_national_number(6502530000ULL);
  EXPECT_EQ(3, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A North America toll-free number, which has NDC "800".
  number.set_country_code(1);
  number.set_national_number(8002530000ULL);
  EXPECT_EQ(3, phone_util_.GetLengthOfNationalDestinationCode(number));

  // Google London, which has NDC "20".
  number.set_country_code(44);
  number.set_national_number(2070313000ULL);
  EXPECT_EQ(2, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A UK mobile phone, which has NDC "7123"
  number.set_country_code(44);
  number.set_national_number(7123456789ULL);
  EXPECT_EQ(4, phone_util_.GetLengthOfNationalDestinationCode(number));

  // Google Buenos Aires, which has NDC "11".
  number.set_country_code(54);
  number.set_national_number(1155303000ULL);
  EXPECT_EQ(2, phone_util_.GetLengthOfNationalDestinationCode(number));

  // Google Sydney, which has NDC "2".
  number.set_country_code(61);
  number.set_national_number(293744000ULL);
  EXPECT_EQ(1, phone_util_.GetLengthOfNationalDestinationCode(number));

  // Google Singapore. Singapore has NDC "6521".
  number.set_country_code(65);
  number.set_national_number(65218000ULL);
  EXPECT_EQ(4, phone_util_.GetLengthOfNationalDestinationCode(number));

  // An invalid US number (1 digit shorter), which has no NDC.
  number.set_country_code(1);
  number.set_national_number(650253000ULL);
  EXPECT_EQ(0, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A number containing an invalid country code, which shouldn't have any NDC.
  number.set_country_code(123);
  number.set_national_number(650253000ULL);
  EXPECT_EQ(0, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A number that has only one group of digits after country code when
  // formatted in the international format.
  number.set_country_code(376);
  number.set_national_number(12345ULL);
  EXPECT_EQ(0, phone_util_.GetLengthOfNationalDestinationCode(number));

  // The same number above, but with an extension.
  number.set_country_code(376);
  number.set_national_number(12345ULL);
  number.set_extension("321");
  EXPECT_EQ(0, phone_util_.GetLengthOfNationalDestinationCode(number));
}

TEST_F(PhoneNumberUtilTest, ExtractPossibleNumber) {
  // Removes preceding funky punctuation and letters but leaves the rest
  // untouched.
  string extracted_number;
  ExtractPossibleNumber("Tel:0800-345-600", &extracted_number);
  EXPECT_EQ("0800-345-600", extracted_number);
  ExtractPossibleNumber("Tel:0800 FOR PIZZA", &extracted_number);
  EXPECT_EQ("0800 FOR PIZZA", extracted_number);

  // Should not remove plus sign.
  ExtractPossibleNumber("Tel:+800-345-600", &extracted_number);
  EXPECT_EQ("+800-345-600", extracted_number);
  // Should recognise wide digits as possible start values.
  ExtractPossibleNumber("\xEF\xBC\x90\xEF\xBC\x92\xEF\xBC\x93" /* "０２３" */,
                        &extracted_number);
  EXPECT_EQ("\xEF\xBC\x90\xEF\xBC\x92\xEF\xBC\x93" /* "０２３" */,
            extracted_number);
  // Dashes are not possible start values and should be removed.
  ExtractPossibleNumber("Num-\xEF\xBC\x91\xEF\xBC\x92\xEF\xBC\x93"
                        /* "Num-１２３" */, &extracted_number);
  EXPECT_EQ("\xEF\xBC\x91\xEF\xBC\x92\xEF\xBC\x93" /* "１２３" */,
            extracted_number);
  // If not possible number present, return empty string.
  ExtractPossibleNumber("Num-....", &extracted_number);
  EXPECT_EQ("", extracted_number);
  // Leading brackets are stripped - these are not used when parsing.
  ExtractPossibleNumber("(650) 253-0000", &extracted_number);
  EXPECT_EQ("650) 253-0000", extracted_number);

  // Trailing non-alpha-numeric characters should be removed.
  ExtractPossibleNumber("(650) 253-0000..- ..", &extracted_number);
  EXPECT_EQ("650) 253-0000", extracted_number);
  ExtractPossibleNumber("(650) 253-0000.", &extracted_number);
  EXPECT_EQ("650) 253-0000", extracted_number);
  // This case has a trailing RTL char.
  ExtractPossibleNumber("(650) 253-0000\xE2\x80\x8F"
                        /* "(650) 253-0000‏" */, &extracted_number);
  EXPECT_EQ("650) 253-0000", extracted_number);
}

TEST_F(PhoneNumberUtilTest, IsNANPACountry) {
  EXPECT_TRUE(phone_util_.IsNANPACountry(RegionCode::US()));
  EXPECT_TRUE(phone_util_.IsNANPACountry(RegionCode::BS()));
}

TEST_F(PhoneNumberUtilTest, IsValidNumber) {
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(6502530000ULL);
  EXPECT_TRUE(phone_util_.IsValidNumber(us_number));

  PhoneNumber it_number;
  it_number.set_country_code(39);
  it_number.set_national_number(236618300ULL);
  it_number.set_italian_leading_zero(true);
  EXPECT_TRUE(phone_util_.IsValidNumber(it_number));

  PhoneNumber gb_number;
  gb_number.set_country_code(44);
  gb_number.set_national_number(7912345678ULL);
  EXPECT_TRUE(phone_util_.IsValidNumber(gb_number));

  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(21387835ULL);
  EXPECT_TRUE(phone_util_.IsValidNumber(nz_number));
}

TEST_F(PhoneNumberUtilTest, IsValidForRegion) {
  // This number is valid for the Bahamas, but is not a valid US number.
  PhoneNumber bs_number;
  bs_number.set_country_code(1);
  bs_number.set_national_number(2423232345ULL);
  EXPECT_TRUE(phone_util_.IsValidNumber(bs_number));
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(bs_number, RegionCode::BS()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(bs_number, RegionCode::US()));
  bs_number.set_national_number(2421232345ULL);
  // This number is no longer valid.
  EXPECT_FALSE(phone_util_.IsValidNumber(bs_number));

  // La Mayotte and Réunion use 'leadingDigits' to differentiate them.
  PhoneNumber re_number;
  re_number.set_country_code(262);
  re_number.set_national_number(262123456ULL);
  EXPECT_TRUE(phone_util_.IsValidNumber(re_number));
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::RE()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::YT()));
  // Now change the number to be a number for La Mayotte.
  re_number.set_national_number(269601234ULL);
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::YT()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::RE()));
  // This number is no longer valid.
  re_number.set_national_number(269123456ULL);
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::YT()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::RE()));
  EXPECT_FALSE(phone_util_.IsValidNumber(re_number));
  // However, it should be recognised as from La Mayotte.
  string region_code;
  phone_util_.GetRegionCodeForNumber(re_number, &region_code);
  EXPECT_EQ(RegionCode::YT(), region_code);
  // This number is valid in both places.
  re_number.set_national_number(800123456ULL);
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::YT()));
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::RE()));
}

TEST_F(PhoneNumberUtilTest, IsNotValidNumber) {
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(2530000ULL);
  EXPECT_FALSE(phone_util_.IsValidNumber(us_number));

  PhoneNumber it_number;
  it_number.set_country_code(39);
  it_number.set_national_number(23661830000ULL);
  it_number.set_italian_leading_zero(true);
  EXPECT_FALSE(phone_util_.IsValidNumber(it_number));

  PhoneNumber gb_number;
  gb_number.set_country_code(44);
  gb_number.set_national_number(791234567ULL);
  EXPECT_FALSE(phone_util_.IsValidNumber(gb_number));

  PhoneNumber de_number;
  de_number.set_country_code(49);
  de_number.set_national_number(1234ULL);
  EXPECT_FALSE(phone_util_.IsValidNumber(de_number));

  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(3316005ULL);
  EXPECT_FALSE(phone_util_.IsValidNumber(nz_number));
}

TEST_F(PhoneNumberUtilTest, IsPossibleNumber) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(6502530000ULL);
  EXPECT_TRUE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(1);
  number.set_national_number(2530000ULL);
  EXPECT_TRUE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(44);
  number.set_national_number(2070313000ULL);
  EXPECT_TRUE(phone_util_.IsPossibleNumber(number));

  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+1 650 253 0000",
                                                    RegionCode::US()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+1 650 GOO OGLE",
                                                    RegionCode::US()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("(650) 253-0000",
                                                    RegionCode::US()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("253-0000",
                                                    RegionCode::US()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+1 650 253 0000",
                                                    RegionCode::GB()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+44 20 7031 3000",
                                                    RegionCode::GB()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("(020) 7031 3000",
                                                    RegionCode::GB()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("7031 3000",
                                                    RegionCode::GB()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("3331 6005",
                                                    RegionCode::NZ()));
}

TEST_F(PhoneNumberUtilTest, IsPossibleNumberWithReason) {
  // FYI, national numbers for country code +1 that are within 7 to 10 digits
  // are possible.
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(6502530000ULL);
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(1);
  number.set_national_number(2530000ULL);
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(0);
  number.set_national_number(2530000ULL);
  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(1);
  number.set_national_number(253000ULL);
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(1);
  number.set_national_number(65025300000ULL);
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(44);
  number.set_national_number(2070310000ULL);
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(49);
  number.set_national_number(30123456ULL);
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(65);
  number.set_national_number(1234567890ULL);
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  // Try with number that we don't have metadata for.
  PhoneNumber ad_number;
  ad_number.set_country_code(376);
  ad_number.set_national_number(12345ULL);
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(ad_number));
  ad_number.set_country_code(376);
  ad_number.set_national_number(13ULL);
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberWithReason(ad_number));
  ad_number.set_country_code(376);
  ad_number.set_national_number(1234567890123456ULL);
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberWithReason(ad_number));
}

TEST_F(PhoneNumberUtilTest, IsNotPossibleNumber) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(65025300000ULL);
  EXPECT_FALSE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(1);
  number.set_national_number(253000ULL);
  EXPECT_FALSE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(44);
  number.set_national_number(300ULL);
  EXPECT_FALSE(phone_util_.IsPossibleNumber(number));

  EXPECT_FALSE(phone_util_.IsPossibleNumberForString("+1 650 253 00000",
                                                     RegionCode::US()));
  EXPECT_FALSE(phone_util_.IsPossibleNumberForString("(650) 253-00000",
                                                     RegionCode::US()));
  EXPECT_FALSE(phone_util_.IsPossibleNumberForString("I want a Pizza",
                                                     RegionCode::US()));
  EXPECT_FALSE(phone_util_.IsPossibleNumberForString("253-000",
                                                     RegionCode::US()));
  EXPECT_FALSE(phone_util_.IsPossibleNumberForString("1 3000",
                                                     RegionCode::GB()));
  EXPECT_FALSE(phone_util_.IsPossibleNumberForString("+44 300",
                                                     RegionCode::GB()));
}

TEST_F(PhoneNumberUtilTest, TruncateTooLongNumber) {
  // US number 650-253-0000, but entered with one additional digit at the end.
  PhoneNumber too_long_number;
  too_long_number.set_country_code(1);
  too_long_number.set_national_number(65025300001ULL);
  PhoneNumber valid_number;
  valid_number.set_country_code(1);
  valid_number.set_national_number(6502530000ULL);
  EXPECT_TRUE(phone_util_.TruncateTooLongNumber(&too_long_number));
  EXPECT_EQ(valid_number, too_long_number);

  // GB number 080 1234 5678, but entered with 4 extra digits at the end.
  too_long_number.set_country_code(44);
  too_long_number.set_national_number(80123456780123ULL);
  valid_number.set_country_code(44);
  valid_number.set_national_number(8012345678ULL);
  EXPECT_TRUE(phone_util_.TruncateTooLongNumber(&too_long_number));
  EXPECT_EQ(valid_number, too_long_number);

  // IT number 022 3456 7890, but entered with 3 extra digits at the end.
  too_long_number.set_country_code(39);
  too_long_number.set_national_number(2234567890123ULL);
  too_long_number.set_italian_leading_zero(true);
  valid_number.set_country_code(39);
  valid_number.set_national_number(2234567890ULL);
  valid_number.set_italian_leading_zero(true);
  EXPECT_TRUE(phone_util_.TruncateTooLongNumber(&too_long_number));
  EXPECT_EQ(valid_number, too_long_number);

  // Tests what happens when a valid number is passed in.
  PhoneNumber valid_number_copy(valid_number);
  EXPECT_TRUE(phone_util_.TruncateTooLongNumber(&valid_number));
  // Tests the number is not modified.
  EXPECT_EQ(valid_number_copy, valid_number);

  // Tests what happens when a number with invalid prefix is passed in.
  PhoneNumber number_with_invalid_prefix;
  number_with_invalid_prefix.set_country_code(1);
  // The test metadata says US numbers cannot have prefix 240.
  number_with_invalid_prefix.set_national_number(2401234567ULL);
  PhoneNumber invalid_number_copy(number_with_invalid_prefix);
  EXPECT_FALSE(phone_util_.TruncateTooLongNumber(&number_with_invalid_prefix));
  // Tests the number is not modified.
  EXPECT_EQ(invalid_number_copy, number_with_invalid_prefix);

  // Tests what happens when a too short number is passed in.
  PhoneNumber too_short_number;
  too_short_number.set_country_code(1);
  too_short_number.set_national_number(1234ULL);
  PhoneNumber too_short_number_copy(too_short_number);
  EXPECT_FALSE(phone_util_.TruncateTooLongNumber(&too_short_number));
  // Tests the number is not modified.
  EXPECT_EQ(too_short_number_copy, too_short_number);
}

TEST_F(PhoneNumberUtilTest, IsLeadingZeroPossible) {
  EXPECT_TRUE(IsLeadingZeroPossible(39));  // Italy
  EXPECT_FALSE(IsLeadingZeroPossible(1));  // USA
  EXPECT_FALSE(IsLeadingZeroPossible(800));  // Not in metadata file, should
                                             // return default value of false.
}

TEST_F(PhoneNumberUtilTest, FormatUsingOriginalNumberFormat) {
  PhoneNumber phone_number;
  string formatted_number;

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("+442087654321", RegionCode::GB(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::GB(),
                                     &formatted_number);
  EXPECT_EQ("+44 20 8765 4321", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("02087654321", RegionCode::GB(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::GB(),
                                     &formatted_number);
  EXPECT_EQ("(020) 8765 4321", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("011442087654321",
                                             RegionCode::US(), &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::US(),
                                     &formatted_number);
  EXPECT_EQ("011 44 20 8765 4321", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("442087654321", RegionCode::GB(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::GB(),
                                     &formatted_number);
  EXPECT_EQ("44 20 8765 4321", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+442087654321", RegionCode::GB(),
                              &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::GB(),
                                     &formatted_number);
  EXPECT_EQ("(020) 8765 4321", formatted_number);
}

TEST_F(PhoneNumberUtilTest, IsPremiumRate) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(9004433030ULL);
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(39);
  number.set_national_number(892123ULL);
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(44);
  number.set_national_number(9187654321ULL);
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(9001654321ULL);
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(90091234567ULL);
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsTollFree) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(8881234567ULL);
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));

  number.set_country_code(39);
  number.set_national_number(803123ULL);
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));

  number.set_country_code(44);
  number.set_national_number(8012345678ULL);
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(8001234567ULL);
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsMobile) {
  PhoneNumber number;
  // A Bahama mobile number
  number.set_country_code(1);
  number.set_national_number(2423570000ULL);
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));

  number.set_country_code(39);
  number.set_national_number(312345678ULL);
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));

  number.set_country_code(44);
  number.set_national_number(7912345678ULL);
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(15123456789ULL);
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));

  number.set_country_code(54);
  number.set_national_number(91187654321ULL);
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsFixedLine) {
  PhoneNumber number;
  // A Bahama fixed-line number
  number.set_country_code(1);
  number.set_national_number(2423651234ULL);
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE, phone_util_.GetNumberType(number));

  // An Italian fixed-line number
  number.Clear();
  number.set_country_code(39);
  number.set_national_number(236618300ULL);
  number.set_italian_leading_zero(true);
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE, phone_util_.GetNumberType(number));

  number.Clear();
  number.set_country_code(44);
  number.set_national_number(2012345678ULL);
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(301234ULL);
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsFixedLineAndMobile) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(6502531111ULL);
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE_OR_MOBILE,
            phone_util_.GetNumberType(number));

  number.set_country_code(54);
  number.set_national_number(1987654321ULL);
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE_OR_MOBILE,
            phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsSharedCost) {
  PhoneNumber number;
  number.set_country_code(44);
  number.set_national_number(8431231234ULL);
  EXPECT_EQ(PhoneNumberUtil::SHARED_COST, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsVoip) {
  PhoneNumber number;
  number.set_country_code(44);
  number.set_national_number(5631231234ULL);
  EXPECT_EQ(PhoneNumberUtil::VOIP, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsPersonalNumber) {
  PhoneNumber number;
  number.set_country_code(44);
  number.set_national_number(7031231234ULL);
  EXPECT_EQ(PhoneNumberUtil::PERSONAL_NUMBER,
            phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsUnknown) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(65025311111ULL);
  EXPECT_EQ(PhoneNumberUtil::UNKNOWN, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, GetCountryCodeForRegion) {
  EXPECT_EQ(1, phone_util_.GetCountryCodeForRegion(RegionCode::US()));
  EXPECT_EQ(64, phone_util_.GetCountryCodeForRegion(RegionCode::NZ()));
  EXPECT_EQ(0, phone_util_.GetCountryCodeForRegion(RegionCode::GetUnknown()));
  // CS is already deprecated so the library doesn't support it.
  EXPECT_EQ(0, phone_util_.GetCountryCodeForRegion(RegionCode::CS()));
}

TEST_F(PhoneNumberUtilTest, GetNationalDiallingPrefixForRegion) {
  string ndd_prefix;
  GetNddPrefixForRegion(RegionCode::US(), false, &ndd_prefix);
  EXPECT_EQ("1", ndd_prefix);

  // Test non-main country to see it gets the national dialling prefix for the
  // main country with that country calling code.
  GetNddPrefixForRegion(RegionCode::BS(), false, &ndd_prefix);
  EXPECT_EQ("1", ndd_prefix);

  GetNddPrefixForRegion(RegionCode::NZ(), false, &ndd_prefix);
  EXPECT_EQ("0", ndd_prefix);

  // Test case with non digit in the national prefix.
  GetNddPrefixForRegion(RegionCode::AO(), false, &ndd_prefix);
  EXPECT_EQ("0~0", ndd_prefix);

  GetNddPrefixForRegion(RegionCode::AO(), true, &ndd_prefix);
  EXPECT_EQ("00", ndd_prefix);

  // Test cases with invalid regions.
  GetNddPrefixForRegion(RegionCode::GetUnknown(), false, &ndd_prefix);
  EXPECT_EQ("", ndd_prefix);

  // CS is already deprecated so the library doesn't support it.
  GetNddPrefixForRegion(RegionCode::CS(), false, &ndd_prefix);
  EXPECT_EQ("", ndd_prefix);
}

TEST_F(PhoneNumberUtilTest, IsViablePhoneNumber) {
  // Only one or two digits before strange non-possible punctuation.
  EXPECT_FALSE(IsViablePhoneNumber("12. March"));
  EXPECT_FALSE(IsViablePhoneNumber("1+1+1"));
  EXPECT_FALSE(IsViablePhoneNumber("80+0"));
  EXPECT_FALSE(IsViablePhoneNumber("00"));
  // Three digits is viable.
  EXPECT_TRUE(IsViablePhoneNumber("111"));
  // Alpha numbers.
  EXPECT_TRUE(IsViablePhoneNumber("0800-4-pizza"));
  EXPECT_TRUE(IsViablePhoneNumber("0800-4-PIZZA"));
  // Only one or two digits before possible punctuation followed by more digits.
  // The punctuation used here is the unicode character u+3000.
  EXPECT_TRUE(IsViablePhoneNumber("1\xE3\x80\x80" "34" /* "1　34" */));
  EXPECT_FALSE(IsViablePhoneNumber("1\xE3\x80\x80" "3+4" /* "1　3+4" */));
  // Unicode variants of possible starting character and other allowed
  // punctuation/digits.
  EXPECT_TRUE(IsViablePhoneNumber("\xEF\xBC\x88" "1\xEF\xBC\x89\xE3\x80\x80"
                                  "3456789" /* "（1）　3456789" */));
  // Testing a leading + is okay.
  EXPECT_TRUE(IsViablePhoneNumber("+1\xEF\xBC\x89\xE3\x80\x80"
                                  "3456789" /* "+1）　3456789" */));
}

TEST_F(PhoneNumberUtilTest, ConvertAlphaCharactersInNumber) {
  string input("1800-ABC-DEF");
  phone_util_.ConvertAlphaCharactersInNumber(&input);
  // Alpha chars are converted to digits; everything else is left untouched.
  static const string kExpectedOutput = "1800-222-333";
  EXPECT_EQ(kExpectedOutput, input);

  // Try with some non-ASCII characters.
  input.assign("1\xE3\x80\x80\xEF\xBC\x88" "800) ABC-DEF"
               /* "1　（800) ABCD-DEF" */);
  static const string kExpectedFullwidthOutput =
      "1\xE3\x80\x80\xEF\xBC\x88" "800) 222-333" /* "1　（800) 222-333" */;
  phone_util_.ConvertAlphaCharactersInNumber(&input);
  EXPECT_EQ(kExpectedFullwidthOutput, input);
}

TEST_F(PhoneNumberUtilTest, NormaliseRemovePunctuation) {
  string input_number("034-56&+#234");
  Normalize(&input_number);
  static const string kExpectedOutput("03456234");
  EXPECT_EQ(kExpectedOutput, input_number)
      << "Conversion did not correctly remove punctuation";
}

TEST_F(PhoneNumberUtilTest, NormaliseReplaceAlphaCharacters) {
  string input_number("034-I-am-HUNGRY");
  Normalize(&input_number);
  static const string kExpectedOutput("034426486479");
  EXPECT_EQ(kExpectedOutput, input_number)
      << "Conversion did not correctly replace alpha characters";
}

TEST_F(PhoneNumberUtilTest, NormaliseOtherDigits) {
  // The first digit is a full-width 2, the last digit is an Arabic-indic digit
  // 5.
  string input_number("\xEF\xBC\x92" "5\xD9\xA5" /* "２5٥" */);
  Normalize(&input_number);
  static const string kExpectedOutput("255");
  EXPECT_EQ(kExpectedOutput, input_number)
      << "Conversion did not correctly replace non-latin digits";
  // The first digit is an Eastern-Arabic 5, the latter an Eastern-Arabic 0.
  string eastern_arabic_input_number("\xDB\xB5" "2\xDB\xB0" /* "۵2۰" */);
  Normalize(&eastern_arabic_input_number);
  static const string kExpectedOutput2("520");
  EXPECT_EQ(kExpectedOutput2, eastern_arabic_input_number)
      << "Conversion did not correctly replace non-latin digits";
}

TEST_F(PhoneNumberUtilTest, NormaliseStripAlphaCharacters) {
  string input_number("034-56&+a#234");
  phone_util_.NormalizeDigitsOnly(&input_number);
  static const string kExpectedOutput("03456234");
  EXPECT_EQ(kExpectedOutput, input_number)
      << "Conversion did not correctly remove alpha characters";
}

TEST_F(PhoneNumberUtilTest, MaybeStripInternationalPrefix) {
  string international_prefix("00[39]");
  string number_to_strip("0034567700-3898003");
  // Note the dash is removed as part of the normalization.
  string stripped_number("45677003898003");
  EXPECT_EQ(PhoneNumber::FROM_NUMBER_WITH_IDD,
      MaybeStripInternationalPrefixAndNormalize(international_prefix,
                                                &number_to_strip));
  EXPECT_EQ(stripped_number, number_to_strip)
      << "The number was not stripped of its international prefix.";

  // Now the number no longer starts with an IDD prefix, so it should now report
  // FROM_DEFAULT_COUNTRY.
  EXPECT_EQ(PhoneNumber::FROM_DEFAULT_COUNTRY,
      MaybeStripInternationalPrefixAndNormalize(international_prefix,
                                                &number_to_strip));

  number_to_strip.assign("00945677003898003");
  EXPECT_EQ(PhoneNumber::FROM_NUMBER_WITH_IDD,
      MaybeStripInternationalPrefixAndNormalize(international_prefix,
                                                &number_to_strip));
  EXPECT_EQ(stripped_number, number_to_strip)
      << "The number was not stripped of its international prefix.";

  // Test it works when the international prefix is broken up by spaces.
  number_to_strip.assign("00 9 45677003898003");
  EXPECT_EQ(PhoneNumber::FROM_NUMBER_WITH_IDD,
      MaybeStripInternationalPrefixAndNormalize(international_prefix,
                                                &number_to_strip));
  EXPECT_EQ(stripped_number, number_to_strip)
      << "The number was not stripped of its international prefix.";
  // Now the number no longer starts with an IDD prefix, so it should now report
  // FROM_DEFAULT_COUNTRY.
  EXPECT_EQ(PhoneNumber::FROM_DEFAULT_COUNTRY,
      MaybeStripInternationalPrefixAndNormalize(international_prefix,
                                                &number_to_strip));

  // Test the + symbol is also recognised and stripped.
  number_to_strip.assign("+45677003898003");
  stripped_number.assign("45677003898003");
  EXPECT_EQ(PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN,
      MaybeStripInternationalPrefixAndNormalize(international_prefix,
                                                &number_to_strip));
  EXPECT_EQ(stripped_number, number_to_strip)
      << "The number supplied was not stripped of the plus symbol.";

  // If the number afterwards is a zero, we should not strip this - no country
  // code begins with 0.
  number_to_strip.assign("0090112-3123");
  stripped_number.assign("00901123123");
  EXPECT_EQ(PhoneNumber::FROM_DEFAULT_COUNTRY,
      MaybeStripInternationalPrefixAndNormalize(international_prefix,
                                                &number_to_strip));
  EXPECT_EQ(stripped_number, number_to_strip)
      << "The number had a 0 after the match so shouldn't be stripped.";
  // Here the 0 is separated by a space from the IDD.
  number_to_strip.assign("009 0-112-3123");
  EXPECT_EQ(PhoneNumber::FROM_DEFAULT_COUNTRY,
      MaybeStripInternationalPrefixAndNormalize(international_prefix,
                                                &number_to_strip));
}

TEST_F(PhoneNumberUtilTest, MaybeStripNationalPrefixAndCarrierCode) {
  PhoneMetadata metadata;
  metadata.set_national_prefix_for_parsing("34");
  metadata.mutable_general_desc()->set_national_number_pattern("\\d{4,8}");
  string number_to_strip("34356778");
  string stripped_number("356778");
  string carrier_code;
  MaybeStripNationalPrefixAndCarrierCode(metadata, &number_to_strip,
                                         &carrier_code);
  EXPECT_EQ(stripped_number, number_to_strip)
      << "Should have had national prefix stripped.";
  EXPECT_EQ("", carrier_code) << "Should have had no carrier code stripped.";
  // Retry stripping - now the number should not start with the national prefix,
  // so no more stripping should occur.
  MaybeStripNationalPrefixAndCarrierCode(metadata, &number_to_strip,
                                         &carrier_code);
  EXPECT_EQ(stripped_number, number_to_strip)
      << "Should have had no change - no national prefix present.";
  // Some countries have no national prefix. Repeat test with none specified.
  metadata.clear_national_prefix_for_parsing();
  MaybeStripNationalPrefixAndCarrierCode(metadata, &number_to_strip,
                                         &carrier_code);
  EXPECT_EQ(stripped_number, number_to_strip)
      << "Should have had no change - empty national prefix.";
  // If the resultant number doesn't match the national rule, it shouldn't be
  // stripped.
  metadata.set_national_prefix_for_parsing("3");
  number_to_strip.assign("3123");
  stripped_number.assign("3123");
  MaybeStripNationalPrefixAndCarrierCode(metadata, &number_to_strip,
                                         &carrier_code);
  EXPECT_EQ(stripped_number, number_to_strip)
      << "Should have had no change - after stripping, it wouldn't have "
      << "matched the national rule.";
  // Test extracting carrier selection code.
  metadata.set_national_prefix_for_parsing("0(81)?");
  number_to_strip.assign("08122123456");
  stripped_number.assign("22123456");
  MaybeStripNationalPrefixAndCarrierCode(metadata, &number_to_strip,
                                         &carrier_code);
  EXPECT_EQ("81", carrier_code) << "Should have had carrier code stripped.";
  EXPECT_EQ(stripped_number, number_to_strip)
      << "Should have had national prefix and carrier code stripped.";
  // If there was a transform rule, check it was applied.
  metadata.set_national_prefix_transform_rule("5$15");
  // Note that a capturing group is present here.
  metadata.set_national_prefix_for_parsing("0(\\d{2})");
  number_to_strip.assign("031123");
  string transformed_number("5315123");
  MaybeStripNationalPrefixAndCarrierCode(metadata, &number_to_strip,
                                         &carrier_code);
  EXPECT_EQ(transformed_number, number_to_strip)
      << "Was not successfully transformed.";
}

TEST_F(PhoneNumberUtilTest, MaybeStripExtension) {
  // One with extension.
  string number("1234576 ext. 1234");
  string extension;
  string expected_extension("1234");
  string stripped_number("1234576");
  EXPECT_TRUE(MaybeStripExtension(&number, &extension));
  EXPECT_EQ(stripped_number, number);
  EXPECT_EQ(expected_extension, extension);

  // One without extension.
  number.assign("1234-576");
  extension.clear();
  stripped_number.assign("1234-576");
  EXPECT_FALSE(MaybeStripExtension(&number, &extension));
  EXPECT_EQ(stripped_number, number);
  EXPECT_TRUE(extension.empty());

  // One with an extension caught by the second capturing group in
  // kKnownExtnPatterns.
  number.assign("1234576-123#");
  extension.clear();
  expected_extension.assign("123");
  stripped_number.assign("1234576");
  EXPECT_TRUE(MaybeStripExtension(&number, &extension));
  EXPECT_EQ(stripped_number, number);
  EXPECT_EQ(expected_extension, extension);

  number.assign("1234576 ext.123#");
  extension.clear();
  EXPECT_TRUE(MaybeStripExtension(&number, &extension));
  EXPECT_EQ(stripped_number, number);
  EXPECT_EQ(expected_extension, extension);
}

TEST_F(PhoneNumberUtilTest, MaybeExtractCountryCode) {
  PhoneNumber number;
  const PhoneMetadata* metadata = GetPhoneMetadata(RegionCode::US());
  // Note that for the US, the IDD is 011.
  string phone_number("011112-3456789");
  string stripped_number("123456789");
  int expected_country_code = 1;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            MaybeExtractCountryCode(metadata, true, &phone_number, &number));
  EXPECT_EQ(expected_country_code, number.country_code());
  EXPECT_EQ(PhoneNumber::FROM_NUMBER_WITH_IDD, number.country_code_source());
  EXPECT_EQ(stripped_number, phone_number);

  number.Clear();
  phone_number.assign("+6423456789");
  stripped_number.assign("23456789");
  expected_country_code = 64;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            MaybeExtractCountryCode(metadata, true, &phone_number, &number));
  EXPECT_EQ(expected_country_code, number.country_code());
  EXPECT_EQ(PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN,
            number.country_code_source());
  EXPECT_EQ(stripped_number, phone_number);

  // Should not have extracted a country code - no international prefix present.
  number.Clear();
  expected_country_code = 0;
  phone_number.assign("2345-6789");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            MaybeExtractCountryCode(metadata, true, &phone_number, &number));
  EXPECT_EQ(expected_country_code, number.country_code());
  EXPECT_EQ(PhoneNumber::FROM_DEFAULT_COUNTRY, number.country_code_source());
  EXPECT_EQ(stripped_number, phone_number);

  expected_country_code = 0;
  phone_number.assign("0119991123456789");
  stripped_number.assign(phone_number);
  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE_ERROR,
            MaybeExtractCountryCode(metadata, true, &phone_number, &number));

  number.Clear();
  phone_number.assign("(1 610) 619 4466");
  stripped_number.assign("6106194466");
  expected_country_code = 1;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            MaybeExtractCountryCode(metadata, true, &phone_number, &number));
  EXPECT_EQ(expected_country_code, number.country_code());
  EXPECT_EQ(PhoneNumber::FROM_NUMBER_WITHOUT_PLUS_SIGN,
            number.country_code_source());
  EXPECT_EQ(stripped_number, phone_number);

  number.Clear();
  phone_number.assign("(1 610) 619 4466");
  stripped_number.assign("6106194466");
  expected_country_code = 1;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            MaybeExtractCountryCode(metadata, false, &phone_number, &number));
  EXPECT_EQ(expected_country_code, number.country_code());
  EXPECT_FALSE(number.has_country_code_source());
  EXPECT_EQ(stripped_number, phone_number);

  // Should not have extracted a country code - invalid number after extraction
  // of uncertain country code.
  number.Clear();
  phone_number.assign("(1 610) 619 446");
  stripped_number.assign("1610619446");
  expected_country_code = 0;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            MaybeExtractCountryCode(metadata, false, &phone_number, &number));
  EXPECT_EQ(expected_country_code, number.country_code());
  EXPECT_FALSE(number.has_country_code_source());
  EXPECT_EQ(stripped_number, phone_number);

  number.Clear();
  phone_number.assign("(1 610) 619");
  stripped_number.assign("1610619");
  expected_country_code = 0;
  // Should not have extracted a country code - invalid number both before and
  // after extraction of uncertain country code.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            MaybeExtractCountryCode(metadata, true, &phone_number, &number));
  EXPECT_EQ(expected_country_code, number.country_code());
  EXPECT_EQ(PhoneNumber::FROM_DEFAULT_COUNTRY, number.country_code_source());
  EXPECT_EQ(stripped_number, phone_number);
}

TEST_F(PhoneNumberUtilTest, CountryWithNoNumberDesc) {
  string formatted_number;
  // Andorra is a country where we don't have PhoneNumberDesc info in the
  // metadata.
  PhoneNumber ad_number;
  ad_number.set_country_code(376);
  ad_number.set_national_number(12345ULL);
  phone_util_.Format(ad_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+376 12345", formatted_number);
  phone_util_.Format(ad_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+37612345", formatted_number);
  phone_util_.Format(ad_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("12345", formatted_number);
  EXPECT_EQ(PhoneNumberUtil::UNKNOWN, phone_util_.GetNumberType(ad_number));
  EXPECT_TRUE(phone_util_.IsValidNumber(ad_number));

  // Test dialing a US number from within Andorra.
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(6502530000ULL);
  phone_util_.FormatOutOfCountryCallingNumber(us_number, RegionCode::AD(),
                                              &formatted_number);
  EXPECT_EQ("00 1 650 253 0000", formatted_number);
}

TEST_F(PhoneNumberUtilTest, UnknownCountryCallingCodeForValidation) {
  PhoneNumber invalid_number;
  invalid_number.set_country_code(0);
  invalid_number.set_national_number(1234ULL);
  EXPECT_FALSE(phone_util_.IsValidNumber(invalid_number));
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchMatches) {
  // Test simple matches where formatting is different, or leading zeroes, or
  // country code has been specified.
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331 6005",
                                                    "+64 03 331 6005"));
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 03 331-6005",
                                                    "+64 03331 6005"));
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+643 331-6005",
                                                    "+64033316005"));
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+643 331-6005",
                                                    "+6433316005"));
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005",
                                                    "+6433316005"));
  // Test alpha numbers.
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+1800 siX-Flags",
                                                    "+1 800 7493 5247"));
  // Test numbers with extensions.
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005 extn 1234",
                                                    "+6433316005#1234"));
  // Test proto buffers.
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(33316005ULL);
  nz_number.set_extension("3456");
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithOneString(nz_number,
                                                   "+643 331 6005 ext 3456"));
  nz_number.clear_extension();
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithOneString(nz_number,
                                                   "+643 331 6005"));
  // Check empty extensions are ignored.
  nz_number.set_extension("");
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithOneString(nz_number,
                                                   "+643 331 6005"));
  // Check variant with two proto buffers.
  PhoneNumber nz_number_2;
  nz_number_2.set_country_code(64);
  nz_number_2.set_national_number(33316005ULL);
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatch(nz_number, nz_number_2));

  // Check raw_input, country_code_source and preferred_domestic_carrier_code
  // are ignored.
  PhoneNumber br_number_1;
  PhoneNumber br_number_2;
  br_number_1.set_country_code(55);
  br_number_1.set_national_number(3121286979ULL);
  br_number_1.set_country_code_source(PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN);
  br_number_1.set_preferred_domestic_carrier_code("12");
  br_number_1.set_raw_input("012 3121286979");
  br_number_2.set_country_code(55);
  br_number_2.set_national_number(3121286979ULL);
  br_number_2.set_country_code_source(PhoneNumber::FROM_DEFAULT_COUNTRY);
  br_number_2.set_preferred_domestic_carrier_code("14");
  br_number_2.set_raw_input("143121286979");
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatch(br_number_1, br_number_2));
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchNonMetches) {
  // NSN matches.
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("03 331 6005",
                                                    "03 331 6006"));
  // Different country code, partial number match.
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005",
                                                    "+16433316005"));
  // Different country code, same number.
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005",
                                                    "+6133316005"));
  // Extension different, all else the same.
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005 extn 1234",
                                                    "+0116433316005#1235"));
  // NSN matches, but extension is different - not the same number.
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005 ext.1235",
                                                    "3 331 6005#1234"));
  // Invalid numbers that can't be parsed.
  EXPECT_EQ(PhoneNumberUtil::INVALID_NUMBER,
            phone_util_.IsNumberMatchWithTwoStrings("43", "3 331 6043"));
  // Invalid numbers that can't be parsed.
  EXPECT_EQ(PhoneNumberUtil::INVALID_NUMBER,
            phone_util_.IsNumberMatchWithTwoStrings("+43", "+64 3 331 6005"));
  EXPECT_EQ(PhoneNumberUtil::INVALID_NUMBER,
            phone_util_.IsNumberMatchWithTwoStrings("+43", "64 3 331 6005"));
  EXPECT_EQ(PhoneNumberUtil::INVALID_NUMBER,
            phone_util_.IsNumberMatchWithTwoStrings("Dog", "64 3 331 6005"));
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchNsnMatches) {
  // NSN matches.
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005",
                                                    "03 331 6005"));

  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(33316005ULL);
  nz_number.set_extension("");
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithOneString(nz_number, "03 331 6005"));
  // Here the second number possibly starts with the country code for New
  // Zealand, although we are unsure.
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithOneString(nz_number,
                                                   "(64-3) 331 6005"));

  // Here, the 1 might be a national prefix, if we compare it to the US number,
  // so the resultant match is an NSN match.
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(2345678901ULL);
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithOneString(us_number,
                                                   "1-234-567-8901"));
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithOneString(us_number, "2345678901"));
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+1 234-567 8901",
                                                    "1 234 567 8901"));
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("1 234-567 8901",
                                                    "1 234 567 8901"));
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("1 234-567 8901",
                                                    "+1 234 567 8901"));
  // For this case, the match will be a short NSN match, because we cannot
  // assume that the 1 might be a national prefix, so don't remove it when
  // parsing.
  PhoneNumber random_number;
  random_number.set_country_code(41);
  random_number.set_national_number(2345678901ULL);
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithOneString(random_number,
                                                   "1-234-567-8901"));
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchShortNsnMatches) {
  // Short NSN matches with the country not specified for either one or both
  // numbers.
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005",
                                                    "331 6005"));

  // We did not know that the "0" was a national prefix since neither number has
  // a country code, so this is considered a SHORT_NSN_MATCH.
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("3 331-6005",
                                                    "03 331 6005"));

  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
              phone_util_.IsNumberMatchWithTwoStrings("3 331-6005",
                                                      "331 6005"));

  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("3 331-6005",
                                                    "+64 331 6005"));

  // Short NSN match with the country specified.
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("03 331-6005",
                                                    "331 6005"));

  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
              phone_util_.IsNumberMatchWithTwoStrings("1 234 345 6789",
                                                      "345 6789"));

  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+1 (234) 345 6789",
                                                    "345 6789"));

  // NSN matches, country code omitted for one number, extension missing for
  // one.
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005",
                                                    "3 331 6005#1234"));

  // One has Italian leading zero, one does not.
  PhoneNumber it_number_1, it_number_2;
  it_number_1.set_country_code(39);
  it_number_1.set_national_number(1234ULL);
  it_number_1.set_italian_leading_zero(true);
  it_number_2.set_country_code(39);
  it_number_2.set_national_number(1234ULL);
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatch(it_number_1, it_number_2));

  // One has an extension, the other has an extension of "".
  it_number_1.set_extension("1234");
  it_number_1.clear_italian_leading_zero();
  it_number_2.set_extension("");
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatch(it_number_1, it_number_2));
}

TEST_F(PhoneNumberUtilTest, ParseNationalNumber) {
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(33316005ULL);
  PhoneNumber test_number;
  // National prefix attached.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("033316005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // National prefix missing.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("33316005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // National prefix attached and some formatting present.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03-331 6005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 331 6005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  // Testing international prefixes.
  // Should strip country code.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0064 3 331 6005",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // Try again, but this time we have an international number with Region Code
  // US. It should recognise the country code and parse accordingly.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("01164 3 331 6005",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+64 3 331 6005",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // We should ignore the leading plus here, since it is not followed by a valid
  // country code but instead is followed by the IDD for the US.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+01164 3 331 6005",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+0064 3 331 6005",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+ 00 64 3 331 6005",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  // Test for http://b/issue?id=2247493
  nz_number.Clear();
  nz_number.set_country_code(64);
  nz_number.set_national_number(64123456ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+64(0)64123456",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  // Check that using a "/" is fine in a phone number.
  PhoneNumber de_number;
  de_number.set_country_code(49);
  de_number.set_national_number(12345678ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("123/45678", RegionCode::DE(), &test_number));
  EXPECT_EQ(de_number, test_number);

  PhoneNumber us_number;
  us_number.set_country_code(1);
  // Check it doesn't use the '1' as a country code when parsing if the phone
  // number was already possible.
  us_number.set_national_number(1234567890ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("123-456-7890", RegionCode::US(), &test_number));
  EXPECT_EQ(us_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseNumberWithAlphaCharacters) {
  // Test case with alpha characters.
  PhoneNumber test_number;
  PhoneNumber tollfree_number;
  tollfree_number.set_country_code(64);
  tollfree_number.set_national_number(800332005ULL);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0800 DDA 005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(tollfree_number, test_number);

  test_number.Clear();
  PhoneNumber premium_number;
  premium_number.set_country_code(64);
  premium_number.set_national_number(9003326005ULL);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 DDA 6005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);

  // Not enough alpha characters for them to be considered intentional, so they
  // are stripped.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 332 6005a",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 332 600a5",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);

  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 332 600A5",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);

  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 a332 600A5",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseWithInternationalPrefixes) {
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(6503336000ULL);
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+1 (650) 333-6000",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(us_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+1-650-333-6000",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(us_number, test_number);

  // Calling the US number from Singapore by using different service providers
  // 1st test: calling using SingTel IDD service (IDD is 001)
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0011-650-333-6000",
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // 2nd test: calling using StarHub IDD service (IDD is 008)
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0081-650-333-6000",
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // 3rd test: calling using SingTel V019 service (IDD is 019)
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0191-650-333-6000",
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // Calling the US number from Poland
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0~01-650-333-6000",
                              RegionCode::PL(), &test_number));
  EXPECT_EQ(us_number, test_number);

  // Using "++" at the start.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("++1 (650) 333-6000",
                              RegionCode::PL(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // Using a full-width plus sign.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("\xEF\xBC\x8B" "1 (650) 333-6000",
                              /* "＋1 (650) 333-6000" */
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // The whole number, including punctuation, is here represented in full-width
  // form.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("\xEF\xBC\x8B\xEF\xBC\x91\xE3\x80\x80\xEF\xBC\x88"
                              "\xEF\xBC\x96\xEF\xBC\x95\xEF\xBC\x90\xEF\xBC\x89"
                              "\xE3\x80\x80\xEF\xBC\x93\xEF\xBC\x93\xEF\xBC\x93"
                              "\xEF\xBC\x8D\xEF\xBC\x96\xEF\xBC\x90\xEF\xBC\x90"
                              "\xEF\xBC\x90",
                              /* "＋１　（６５０）　３３３－６０００" */
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);

  // Using the U+30FC dash.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("\xEF\xBC\x8B\xEF\xBC\x91\xE3\x80\x80\xEF\xBC\x88"
                              "\xEF\xBC\x96\xEF\xBC\x95\xEF\xBC\x90\xEF\xBC\x89"
                              "\xE3\x80\x80\xEF\xBC\x93\xEF\xBC\x93\xEF\xBC\x93"
                              "\xE3\x83\xBC\xEF\xBC\x96\xEF\xBC\x90\xEF\xBC\x90"
                              "\xEF\xBC\x90",
                              /* "＋１　（６５０）　３３３ー６０００" */
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseWithLeadingZero) {
  PhoneNumber it_number;
  it_number.set_country_code(39);
  it_number.set_national_number(236618300ULL);
  it_number.set_italian_leading_zero(true);
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+39 02-36618 300",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(it_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("02-36618 300", RegionCode::IT(), &test_number));
  EXPECT_EQ(it_number, test_number);

  it_number.Clear();
  it_number.set_country_code(39);
  it_number.set_national_number(312345678ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("312 345 678", RegionCode::IT(), &test_number));
  EXPECT_EQ(it_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseNationalNumberArgentina) {
  // Test parsing mobile numbers of Argentina.
  PhoneNumber ar_number;
  ar_number.set_country_code(54);
  ar_number.set_national_number(93435551212ULL);
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 9 343 555 1212", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0343 15 555 1212", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);

  ar_number.set_national_number(93715654320ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 9 3715 65 4320", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03715 15 65 4320", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);

  // Test parsing fixed-line numbers of Argentina.
  ar_number.set_national_number(1137970000ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 11 3797 0000", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("011 3797 0000", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);

  ar_number.set_national_number(3715654321ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 3715 65 4321", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03715 65 4321", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);

  ar_number.set_national_number(2312340000ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 23 1234 0000", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("023 1234 0000", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseWithXInNumber) {
  // Test that having an 'x' in the phone number at the start is ok and that it
  // just gets removed.
  PhoneNumber ar_number;
  ar_number.set_country_code(54);
  ar_number.set_national_number(123456789ULL);
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0123456789", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(0) 123456789", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0 123456789", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(0xx) 123456789", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);

  PhoneNumber ar_from_us;
  ar_from_us.set_country_code(54);
  ar_from_us.set_national_number(81429712ULL);
  // This test is intentionally constructed such that the number of digit after
  // xx is larger than 7, so that the number won't be mistakenly treated as an
  // extension, as we allow extensions up to 7 digits. This assumption is okay
  // for now as all the countries where a carrier selection code is written in
  // the form of xx have a national significant number of length larger than 7.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("011xx5481429712", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(ar_from_us, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseNumbersMexico) {
  // Test parsing fixed-line numbers of Mexico.
  PhoneNumber mx_number;

  mx_number.set_country_code(52);
  mx_number.set_national_number(4499780001ULL);
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+52 (449)978-0001", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("01 (449)978-0001", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(449)978-0001", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);

  // Test parsing mobile numbers of Mexico.
  mx_number.Clear();
  mx_number.set_country_code(52);
  mx_number.set_national_number(13312345678ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+52 1 33 1234-5678", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("044 (33) 1234-5678", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("045 33 1234-5678", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
}

TEST_F(PhoneNumberUtilTest, FailedParseOnInvalidNumbers) {
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("This is not a phone number", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::TOO_LONG_NSN,
            phone_util_.Parse("01495 72553301873 810104", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT_NSN,
            phone_util_.Parse("+49 0", RegionCode::DE(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE_ERROR,
            phone_util_.Parse("+210 3456 56789", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  // 00 is a correct IDD, but 210 is not a valid country code.
  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE_ERROR,
            phone_util_.Parse("+ 00 210 3 331 6005", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE_ERROR,
            phone_util_.Parse("123 456 7890", RegionCode::GetUnknown(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE_ERROR,
            phone_util_.Parse("123 456 7890", RegionCode::CS(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT_AFTER_IDD,
            phone_util_.Parse("0044-----", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT_AFTER_IDD,
            phone_util_.Parse("0044", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT_AFTER_IDD,
            phone_util_.Parse("011", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT_AFTER_IDD,
            phone_util_.Parse("0119", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);
}

TEST_F(PhoneNumberUtilTest, ParseNumbersWithPlusWithNoRegion) {
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(33316005ULL);
  // RegionCode::GetUnknown() is allowed only if the number starts with a '+' -
  // then the country code can be calculated.
  PhoneNumber result_proto;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+64 3 331 6005", RegionCode::GetUnknown(),
                              &result_proto));
  EXPECT_EQ(nz_number, result_proto);

  // Test with full-width plus.
  result_proto.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("\xEF\xBC\x8B" "64 3 331 6005",
                              /* "＋64 3 331 6005" */
                              RegionCode::GetUnknown(), &result_proto));
  EXPECT_EQ(nz_number, result_proto);
  // Test with normal plus but leading characters that need to be stripped.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("  +64 3 331 6005", RegionCode::GetUnknown(),
                              &result_proto));
  EXPECT_EQ(nz_number, result_proto);

  nz_number.set_raw_input("+64 3 331 6005");
  nz_number.set_country_code_source(PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN);
  // It is important that we set this to an empty string, since we used
  // ParseAndKeepRawInput and no carrrier code was found.
  nz_number.set_preferred_domestic_carrier_code("");
  result_proto.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("+64 3 331 6005",
                                             RegionCode::GetUnknown(),
                                             &result_proto));
  EXPECT_EQ(nz_number, result_proto);
}

TEST_F(PhoneNumberUtilTest, ParseExtensions) {
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(33316005ULL);
  nz_number.set_extension("3456");
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 331 6005 ext 3456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 331 6005x3456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03-331 6005 int.3456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 331 6005 #3456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);

  // Test the following do not extract extensions:
  PhoneNumber non_extn_number;
  non_extn_number.set_country_code(1);
  non_extn_number.set_national_number(80074935247ULL);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("1800 six-flags", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(non_extn_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("1800 SIX-FLAGS", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(non_extn_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0~0 1800 7493 5247", RegionCode::PL(),
                              &test_number));
  EXPECT_EQ(non_extn_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(1800) 7493.5247", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(non_extn_number, test_number);

  // Check that the last instance of an extension token is matched.
  PhoneNumber extn_number;
  extn_number.set_country_code(1);
  extn_number.set_national_number(80074935247ULL);
  extn_number.set_extension("1234");
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0~0 1800 7493 5247 ~1234", RegionCode::PL(),
                              &test_number));
  EXPECT_EQ(extn_number, test_number);

  // Verifying bug-fix where the last digit of a number was previously omitted
  // if it was a 0 when extracting the extension. Also verifying a few different
  // cases of extensions.
  PhoneNumber uk_number;
  uk_number.set_country_code(44);
  uk_number.set_national_number(2034567890ULL);
  uk_number.set_extension("456");
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890x456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890x456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 x456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 X456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 X 456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 X   456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 x 456  ", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890  X 456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44-2034567890;ext=456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);

  PhoneNumber us_with_extension;
  us_with_extension.set_country_code(1);
  us_with_extension.set_national_number(8009013355ULL);
  us_with_extension.set_extension("7246433");
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 x 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 , ext 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ,extension 7246433",
                              RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ,extensi\xC3\xB3n 7246433",
                              /* "(800) 901-3355 ,extensión 7246433" */
                              RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  test_number.Clear();
  // Repeat with the small letter o with acute accent created by combining
  // characters.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ,extensio\xCC\x81n 7246433",
                              /* "(800) 901-3355 ,extensión 7246433" */
                              RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 , 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ext: 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);

  // Test that if a number has two extensions specified, we ignore the second.
  PhoneNumber us_with_two_extensions_number;
  us_with_two_extensions_number.set_country_code(1);
  us_with_two_extensions_number.set_national_number(2121231234ULL);
  us_with_two_extensions_number.set_extension("508");
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(212)123-1234 x508/x1234", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_two_extensions_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(212)123-1234 x508/ x1234", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_two_extensions_number, test_number);
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(212)123-1234 x508\\x1234", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_two_extensions_number, test_number);

  // Test parsing numbers in the form (645) 123-1234-910# works, where the last
  // 3 digits before the # are an extension.
  us_with_extension.Clear();
  us_with_extension.set_country_code(1);
  us_with_extension.set_national_number(6451231234ULL);
  us_with_extension.set_extension("910");
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+1 (645) 123 1234-910#", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseAndKeepRaw) {
  PhoneNumber alpha_numeric_number;
  alpha_numeric_number.set_country_code(1);
  alpha_numeric_number.set_national_number(80074935247ULL);
  alpha_numeric_number.set_raw_input("800 six-flags");
  alpha_numeric_number.set_country_code_source(
      PhoneNumber::FROM_DEFAULT_COUNTRY);
  alpha_numeric_number.set_preferred_domestic_carrier_code("");

  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("800 six-flags", RegionCode::US(),
                                             &test_number));
  EXPECT_EQ(alpha_numeric_number, test_number);

  alpha_numeric_number.set_national_number(8007493524ULL);
  alpha_numeric_number.set_raw_input("1800 six-flag");
  alpha_numeric_number.set_country_code_source(
      PhoneNumber::FROM_NUMBER_WITHOUT_PLUS_SIGN);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("1800 six-flag", RegionCode::US(),
                                             &test_number));
  EXPECT_EQ(alpha_numeric_number, test_number);

  alpha_numeric_number.set_raw_input("+1800 six-flag");
  alpha_numeric_number.set_country_code_source(
      PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("+1800 six-flag", RegionCode::CN(),
                                             &test_number));
  EXPECT_EQ(alpha_numeric_number, test_number);

  alpha_numeric_number.set_raw_input("001800 six-flag");
  alpha_numeric_number.set_country_code_source(
      PhoneNumber::FROM_NUMBER_WITH_IDD);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("001800 six-flag",
                                             RegionCode::NZ(),
                                             &test_number));
  EXPECT_EQ(alpha_numeric_number, test_number);

  // Try with invalid region - expect failure.
  test_number.Clear();
  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE_ERROR,
            phone_util_.Parse("123 456 7890", RegionCode::CS(), &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  PhoneNumber korean_number;
  korean_number.set_country_code(82);
  korean_number.set_national_number(22123456);
  korean_number.set_raw_input("08122123456");
  korean_number.set_country_code_source(PhoneNumber::FROM_DEFAULT_COUNTRY);
  korean_number.set_preferred_domestic_carrier_code("81");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("08122123456",
                                             RegionCode::KR(),
                                             &test_number));
  EXPECT_EQ(korean_number, test_number);
}

TEST_F(PhoneNumberUtilTest, IsAlphaNumber) {
  static const string kAlphaNumber("1800 six-flags");
  EXPECT_TRUE(phone_util_.IsAlphaNumber(kAlphaNumber));
  static const string kAlphaNumberWithExtension = "1800 six-flags ext. 1234";
  EXPECT_TRUE(phone_util_.IsAlphaNumber(kAlphaNumberWithExtension));
  static const string kNonAlphaNumber("1800 123-1234");
  EXPECT_FALSE(phone_util_.IsAlphaNumber(kNonAlphaNumber));
  static const string kNonAlphaNumberWithExtension(
      "1800 123-1234 extension: 1234");
  EXPECT_FALSE(phone_util_.IsAlphaNumber(kNonAlphaNumberWithExtension));
}

}  // namespace phonenumbers
}  // namespace i18n
