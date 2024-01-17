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
//
// Note that these tests use the metadata contained in the test metadata file,
// not the normal metadata file, so should not be used for regression test
// purposes - these tests are illustrative only and test functionality.

#include "phonenumbers/phonenumberutil.h"

#include <algorithm>
#include <iostream>
#include <list>
#include <set>
#include <string>

#include <gtest/gtest.h>
#include <unicode/uchar.h>

#include "phonenumbers/default_logger.h"
#include "phonenumbers/normalize_utf8.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/test_util.h"

namespace i18n {
namespace phonenumbers {

using std::find;
using std::ostream;

using google::protobuf::RepeatedPtrField;

static const int kInvalidCountryCode = 2;

class PhoneNumberUtilTest : public testing::Test {
 public:
  // This type is neither copyable nor movable.
  PhoneNumberUtilTest(const PhoneNumberUtilTest&) = delete;
  PhoneNumberUtilTest& operator=(const PhoneNumberUtilTest&) = delete;

 protected:
  PhoneNumberUtilTest() : phone_util_(*PhoneNumberUtil::GetInstance()) {
    PhoneNumberUtil::GetInstance()->SetLogger(new StdoutLogger());
  }

  // Wrapper functions for private functions that we want to test.
  const PhoneMetadata* GetPhoneMetadata(const string& region_code) const {
    return phone_util_.GetMetadataForRegion(region_code);
  }

  const PhoneMetadata* GetMetadataForNonGeographicalRegion(
      int country_code) const {
    return phone_util_.GetMetadataForNonGeographicalRegion(country_code);
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

  bool ContainsOnlyValidDigits(const string& s) const {
    return phone_util_.ContainsOnlyValidDigits(s);
  }

  void AssertThrowsForInvalidPhoneContext(const string number_to_parse) {
    PhoneNumber actual_number;
    EXPECT_EQ(
        PhoneNumberUtil::NOT_A_NUMBER,
        phone_util_.Parse(number_to_parse, RegionCode::ZZ(), &actual_number));
  }

  const PhoneNumberUtil& phone_util_;

};

TEST_F(PhoneNumberUtilTest, ContainsOnlyValidDigits) {
  EXPECT_TRUE(ContainsOnlyValidDigits(""));
  EXPECT_TRUE(ContainsOnlyValidDigits("2"));
  EXPECT_TRUE(ContainsOnlyValidDigits("25"));
  EXPECT_TRUE(ContainsOnlyValidDigits("\xEF\xBC\x96" /* "６" */));
  EXPECT_FALSE(ContainsOnlyValidDigits("a"));
  EXPECT_FALSE(ContainsOnlyValidDigits("2a"));
}

TEST_F(PhoneNumberUtilTest, InterchangeInvalidCodepoints) {
  PhoneNumber phone_number;

  std::vector<string> valid_inputs = {
    "+44" "\xE2\x80\x93" "2087654321", // U+2013, EN DASH
  };
  for (auto input : valid_inputs) {
    EXPECT_EQ(input, NormalizeUTF8::NormalizeDecimalDigits(input));
    EXPECT_TRUE(IsViablePhoneNumber(input));
    EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
              phone_util_.Parse(input, RegionCode::GB(), &phone_number));
  }

  std::vector<string> invalid_inputs = {
    "+44" "\x96"         "2087654321", // Invalid sequence
    "+44" "\xC2\x96"     "2087654321", // U+0096
    "+44" "\xEF\xBF\xBE" "2087654321", // U+FFFE
  };
  for (auto input : invalid_inputs) {
    EXPECT_TRUE(NormalizeUTF8::NormalizeDecimalDigits(input).empty());
    EXPECT_FALSE(IsViablePhoneNumber(input));
    EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
              phone_util_.Parse(input, RegionCode::GB(), &phone_number));
  }
}

TEST_F(PhoneNumberUtilTest, GetSupportedRegions) {
  std::set<string> regions;

  phone_util_.GetSupportedRegions(&regions);
  EXPECT_GT(regions.size(), 0U);
}

TEST_F(PhoneNumberUtilTest, GetSupportedGlobalNetworkCallingCodes) {
  std::set<int> calling_codes;

  phone_util_.GetSupportedGlobalNetworkCallingCodes(&calling_codes);
  EXPECT_GT(calling_codes.size(), 0U);
  for (std::set<int>::const_iterator it = calling_codes.begin();
       it != calling_codes.end(); ++it) {
    EXPECT_GT(*it, 0);
    string region_code;
    phone_util_.GetRegionCodeForCountryCode(*it, &region_code);
    EXPECT_EQ(RegionCode::UN001(), region_code);
  }
}

TEST_F(PhoneNumberUtilTest, GetSupportedCallingCodes) {
  std::set<int> calling_codes;

  phone_util_.GetSupportedCallingCodes(&calling_codes);
  EXPECT_GT(calling_codes.size(), 0U);
  for (std::set<int>::const_iterator it = calling_codes.begin();
       it != calling_codes.end(); ++it) {
    EXPECT_GT(*it, 0);
    string region_code;
    phone_util_.GetRegionCodeForCountryCode(*it, &region_code);
    EXPECT_NE(RegionCode::ZZ(), region_code);
  }
  std::set<int> supported_global_network_calling_codes;
  phone_util_.GetSupportedGlobalNetworkCallingCodes(
      &supported_global_network_calling_codes);
  // There should be more than just the global network calling codes in this
  // set.
  EXPECT_GT(calling_codes.size(),
            supported_global_network_calling_codes.size());
  // But they should be included. Testing one of them.
  EXPECT_NE(calling_codes.find(979), calling_codes.end());
}

TEST_F(PhoneNumberUtilTest, GetSupportedTypesForRegion) {
  std::set<PhoneNumberUtil::PhoneNumberType> types;
  phone_util_.GetSupportedTypesForRegion(RegionCode::BR(), &types);
  EXPECT_NE(types.find(PhoneNumberUtil::FIXED_LINE), types.end());
  // Our test data has no mobile numbers for Brazil.
  EXPECT_EQ(types.find(PhoneNumberUtil::MOBILE), types.end());
  // UNKNOWN should never be returned.
  EXPECT_EQ(types.find(PhoneNumberUtil::UNKNOWN), types.end());

  types.clear();
  // In the US, many numbers are classified as FIXED_LINE_OR_MOBILE; but we
  // don't want to expose this as a supported type, instead we say FIXED_LINE
  // and MOBILE are both present.
  phone_util_.GetSupportedTypesForRegion(RegionCode::US(), &types);
  EXPECT_NE(types.find(PhoneNumberUtil::FIXED_LINE), types.end());
  EXPECT_NE(types.find(PhoneNumberUtil::MOBILE), types.end());
  EXPECT_EQ(types.find(PhoneNumberUtil::FIXED_LINE_OR_MOBILE), types.end());
  types.clear();
  phone_util_.GetSupportedTypesForRegion(RegionCode::ZZ(), &types);
  // Test the invalid region code.
  EXPECT_EQ(0u, types.size());
}

TEST_F(PhoneNumberUtilTest, GetSupportedTypesForNonGeoEntity) {
  std::set<PhoneNumberUtil::PhoneNumberType> types;
  // No data exists for 999 at all, no types should be returned.
  phone_util_.GetSupportedTypesForNonGeoEntity(999, &types);
  EXPECT_EQ(0u, types.size());

  types.clear();
  phone_util_.GetSupportedTypesForNonGeoEntity(979, &types);
  EXPECT_NE(types.find(PhoneNumberUtil::PREMIUM_RATE), types.end());
  EXPECT_EQ(types.find(PhoneNumberUtil::MOBILE), types.end());
  EXPECT_EQ(types.find(PhoneNumberUtil::UNKNOWN), types.end());
}

TEST_F(PhoneNumberUtilTest, GetRegionCodesForCountryCallingCode) {
  std::list<string> regions;

  phone_util_.GetRegionCodesForCountryCallingCode(1, &regions);
  EXPECT_TRUE(find(regions.begin(), regions.end(), RegionCode::US())
              != regions.end());
  EXPECT_TRUE(find(regions.begin(), regions.end(), RegionCode::BS())
              != regions.end());

  regions.clear();
  phone_util_.GetRegionCodesForCountryCallingCode(44, &regions);
  EXPECT_TRUE(find(regions.begin(), regions.end(), RegionCode::GB())
              != regions.end());

  regions.clear();
  phone_util_.GetRegionCodesForCountryCallingCode(49, &regions);
  EXPECT_TRUE(find(regions.begin(), regions.end(), RegionCode::DE())
              != regions.end());

  regions.clear();
  phone_util_.GetRegionCodesForCountryCallingCode(800, &regions);
  EXPECT_TRUE(find(regions.begin(), regions.end(), RegionCode::UN001())
              != regions.end());

  regions.clear();
  phone_util_.GetRegionCodesForCountryCallingCode(
      kInvalidCountryCode, &regions);
  EXPECT_TRUE(regions.empty());
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
  EXPECT_EQ("[13-689]\\d{9}|2[0-35-9]\\d{8}",
            metadata->fixed_line().national_number_pattern());
  EXPECT_EQ(1, metadata->general_desc().possible_length_size());
  EXPECT_EQ(10, metadata->general_desc().possible_length(0));
  // Possible lengths are the same as the general description, so aren't stored
  // separately in the toll free element as well.
  EXPECT_EQ(0, metadata->toll_free().possible_length_size());
  EXPECT_EQ("900\\d{7}", metadata->premium_rate().national_number_pattern());
  // No shared-cost data is available, so its national number data should not be
  // set.
  EXPECT_FALSE(metadata->shared_cost().has_national_number_pattern());
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
  EXPECT_EQ(2, metadata->general_desc().possible_length_local_only_size());
  EXPECT_EQ(8, metadata->general_desc().possible_length_size());
  // Nothing is present for fixed-line, since it is the same as the general
  // desc, so for efficiency reasons we don't store an extra value.
  EXPECT_EQ(0, metadata->fixed_line().possible_length_size());
  EXPECT_EQ(2, metadata->mobile().possible_length_size());
  EXPECT_EQ("$1 $2 $3", metadata->number_format(5).format());
  EXPECT_EQ("(?:[24-6]\\d{2}|3[03-9]\\d|[789](?:0[2-9]|[1-9]\\d))\\d{1,8}",
            metadata->fixed_line().national_number_pattern());
  EXPECT_EQ("30123456", metadata->fixed_line().example_number());
  EXPECT_EQ(10, metadata->toll_free().possible_length(0));
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
  EXPECT_EQ("(\\d)(\\d{4})(\\d{2})(\\d{4})",
            metadata->number_format(3).pattern());
  EXPECT_EQ("(\\d)(\\d{4})(\\d{2})(\\d{4})",
            metadata->intl_number_format(3).pattern());
  EXPECT_EQ("$1 $2 $3 $4", metadata->intl_number_format(3).format());
}

TEST_F(PhoneNumberUtilTest, GetInstanceLoadInternationalTollFreeMetadata) {
  const PhoneMetadata* metadata = GetMetadataForNonGeographicalRegion(800);
  EXPECT_FALSE(metadata == NULL);
  EXPECT_EQ("001", metadata->id());
  EXPECT_EQ(800, metadata->country_code());
  EXPECT_EQ("$1 $2", metadata->number_format(0).format());
  EXPECT_EQ("(\\d{4})(\\d{4})", metadata->number_format(0).pattern());
  EXPECT_EQ(0, metadata->general_desc().possible_length_local_only_size());
  EXPECT_EQ(1, metadata->general_desc().possible_length_size());
  EXPECT_EQ("12345678", metadata->toll_free().example_number());
}

TEST_F(PhoneNumberUtilTest, GetNationalSignificantNumber) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{6502530000});
  string national_significant_number;
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("6502530000", national_significant_number);

  // An Italian mobile number.
  national_significant_number.clear();
  number.set_country_code(39);
  number.set_national_number(uint64{312345678});
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("312345678", national_significant_number);

  // An Italian fixed line number.
  national_significant_number.clear();
  number.set_country_code(39);
  number.set_national_number(uint64{236618300});
  number.set_italian_leading_zero(true);
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("0236618300", national_significant_number);

  national_significant_number.clear();
  number.Clear();
  number.set_country_code(800);
  number.set_national_number(uint64{12345678});
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("12345678", national_significant_number);
}

TEST_F(PhoneNumberUtilTest, GetNationalSignificantNumber_ManyLeadingZeros) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{650});
  number.set_italian_leading_zero(true);
  number.set_number_of_leading_zeros(2);
  string national_significant_number;
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("00650", national_significant_number);

  // Set a bad value; we shouldn't crash, we shouldn't output any leading zeros
  // at all.
  number.set_number_of_leading_zeros(-3);
  national_significant_number.clear();
  phone_util_.GetNationalSignificantNumber(number,
                                           &national_significant_number);
  EXPECT_EQ("650", national_significant_number);
}

TEST_F(PhoneNumberUtilTest, GetExampleNumber) {
  PhoneNumber de_number;
  de_number.set_country_code(49);
  de_number.set_national_number(uint64{30123456});
  PhoneNumber test_number;
  bool success = phone_util_.GetExampleNumber(RegionCode::DE(), &test_number);
  EXPECT_TRUE(success);
  EXPECT_EQ(de_number, test_number);

  success = phone_util_.GetExampleNumberForType(
      RegionCode::DE(), PhoneNumberUtil::FIXED_LINE, &test_number);
  EXPECT_TRUE(success);
  EXPECT_EQ(de_number, test_number);

  // Should return the same response if asked for FIXED_LINE_OR_MOBILE too.
  success = phone_util_.GetExampleNumberForType(
      RegionCode::DE(), PhoneNumberUtil::FIXED_LINE_OR_MOBILE, &test_number);
  EXPECT_EQ(de_number, test_number);

  success = phone_util_.GetExampleNumberForType(
      RegionCode::DE(), PhoneNumberUtil::MOBILE, &test_number);
  // We have data for the US, but no data for VOICEMAIL, so the number passed in
  // should be left empty.
  success = phone_util_.GetExampleNumberForType(
      RegionCode::US(), PhoneNumberUtil::VOICEMAIL, &test_number);
  test_number.Clear();
  EXPECT_FALSE(success);
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  success = phone_util_.GetExampleNumberForType(
      RegionCode::US(), PhoneNumberUtil::FIXED_LINE, &test_number);
  // Here we test that the call to get an example number succeeded, and that the
  // number passed in was modified.
  EXPECT_TRUE(success);
  EXPECT_NE(PhoneNumber::default_instance(), test_number);
  success = phone_util_.GetExampleNumberForType(
      RegionCode::US(), PhoneNumberUtil::MOBILE, &test_number);
  EXPECT_TRUE(success);
  EXPECT_NE(PhoneNumber::default_instance(), test_number);

  // CS is an invalid region, so we have no data for it. We should return false.
  test_number.Clear();
  EXPECT_FALSE(phone_util_.GetExampleNumberForType(
      RegionCode::CS(), PhoneNumberUtil::MOBILE, &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  // RegionCode 001 is reserved for supporting non-geographical country calling
  // code. We don't support getting an example number for it with this method.
  EXPECT_FALSE(phone_util_.GetExampleNumber(RegionCode::UN001(), &test_number));
}

TEST_F(PhoneNumberUtilTest, GetInvalidExampleNumber) {
  // RegionCode 001 is reserved for supporting non-geographical country calling
  // codes. We don't support getting an invalid example number for it with
  // GetInvalidExampleNumber.
  PhoneNumber test_number;
  EXPECT_FALSE(phone_util_.GetInvalidExampleNumber(RegionCode::UN001(),
                                                   &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);
  EXPECT_FALSE(phone_util_.GetInvalidExampleNumber(RegionCode::CS(),
                                                   &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_TRUE(phone_util_.GetInvalidExampleNumber(RegionCode::US(),
                                                  &test_number));
  // At least the country calling code should be set correctly.
  EXPECT_EQ(1, test_number.country_code());
  EXPECT_NE(0u, test_number.national_number());
}

TEST_F(PhoneNumberUtilTest, GetExampleNumberForNonGeoEntity) {
  PhoneNumber toll_free_number;
  toll_free_number.set_country_code(800);
  toll_free_number.set_national_number(uint64{12345678});
  PhoneNumber test_number;
  bool success =
      phone_util_.GetExampleNumberForNonGeoEntity(800 , &test_number);
  EXPECT_TRUE(success);
  EXPECT_EQ(toll_free_number, test_number);

  PhoneNumber universal_premium_rate;
  universal_premium_rate.set_country_code(979);
  universal_premium_rate.set_national_number(uint64{123456789});
  success = phone_util_.GetExampleNumberForNonGeoEntity(979 , &test_number);
  EXPECT_TRUE(success);
  EXPECT_EQ(universal_premium_rate, test_number);
}

TEST_F(PhoneNumberUtilTest, GetExampleNumberWithoutRegion) {
  // In our test metadata we don't cover all types: in our real metadata, we do.
  PhoneNumber test_number;
  bool success = phone_util_.GetExampleNumberForType(
      PhoneNumberUtil::FIXED_LINE,
      &test_number);
  // We test that the call to get an example number succeeded, and that the
  // number passed in was modified.
  EXPECT_TRUE(success);
  EXPECT_NE(PhoneNumber::default_instance(), test_number);
  test_number.Clear();

  success = phone_util_.GetExampleNumberForType(PhoneNumberUtil::MOBILE,
                                                &test_number);
  EXPECT_TRUE(success);
  EXPECT_NE(PhoneNumber::default_instance(), test_number);
  test_number.Clear();

  success = phone_util_.GetExampleNumberForType(PhoneNumberUtil::PREMIUM_RATE,
                                                &test_number);
  EXPECT_TRUE(success);
  EXPECT_NE(PhoneNumber::default_instance(), test_number);
}

TEST_F(PhoneNumberUtilTest, FormatUSNumber) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(1);
  test_number.set_national_number(uint64{6502530000});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("650 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 650 253 0000", formatted_number);

  test_number.set_national_number(uint64{8002530000});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("800 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 800 253 0000", formatted_number);

  test_number.set_national_number(uint64{9002530000});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("900 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 900 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::RFC3966, &formatted_number);
  EXPECT_EQ("tel:+1-900-253-0000", formatted_number);
  test_number.set_national_number(uint64{0});
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
  test_number.set_national_number(uint64{2421234567});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("242 123 4567", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 242 123 4567", formatted_number);

  test_number.set_national_number(uint64{8002530000});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("800 253 0000", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+1 800 253 0000", formatted_number);

  test_number.set_national_number(uint64{9002530000});
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
  test_number.set_national_number(uint64{2087389353});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("(020) 8738 9353", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+44 20 8738 9353", formatted_number);

  test_number.set_national_number(uint64{7912345678});
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
  test_number.set_national_number(uint64{301234});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("030/1234", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 30/1234", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::RFC3966, &formatted_number);
  EXPECT_EQ("tel:+49-30-1234", formatted_number);

  test_number.set_national_number(uint64{291123});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("0291 123", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 291 123", formatted_number);

  test_number.set_national_number(uint64{29112345678});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("0291 12345678", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 291 12345678", formatted_number);

  test_number.set_national_number(uint64{9123123});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("09123 123", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 9123 123", formatted_number);

  test_number.set_national_number(uint64{80212345});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("08021 2345", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+49 8021 2345", formatted_number);

  test_number.set_national_number(uint64{1234});
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
  test_number.set_national_number(uint64{236618300});
  test_number.set_italian_leading_zero(true);
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("02 3661 8300", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+39 02 3661 8300", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+390236618300", formatted_number);

  test_number.set_national_number(uint64{345678901});
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
  test_number.set_national_number(uint64{236618300});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("02 3661 8300", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+61 2 3661 8300", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+61236618300", formatted_number);

  test_number.set_national_number(uint64{1800123456});
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
  test_number.set_national_number(uint64{1187654321});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("011 8765-4321", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+54 11 8765-4321", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+541187654321", formatted_number);

  test_number.set_national_number(uint64{91187654321});
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
  test_number.set_national_number(uint64{12345678900});
  phone_util_.Format(test_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("045 234 567 8900", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+52 1 234 567 8900", formatted_number);
  phone_util_.Format(test_number, PhoneNumberUtil::E164,
                     &formatted_number);
  EXPECT_EQ("+5212345678900", formatted_number);

  test_number.set_national_number(uint64{15512345678});
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
  test_number.set_national_number(uint64{9002530000});
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::DE(),
                                              &formatted_number);
  EXPECT_EQ("00 1 900 253 0000", formatted_number);

  test_number.set_national_number(uint64{6502530000});
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::BS(),
                                              &formatted_number);
  EXPECT_EQ("1 650 253 0000", formatted_number);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::PL(),
                                              &formatted_number);
  EXPECT_EQ("00 1 650 253 0000", formatted_number);

  test_number.set_country_code(44);
  test_number.set_national_number(uint64{7912345678});
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::US(),
                                              &formatted_number);
  EXPECT_EQ("011 44 7912 345 678", formatted_number);

  test_number.set_country_code(49);
  test_number.set_national_number(uint64{1234});
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
  test_number.set_national_number(uint64{236618300});
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
  test_number.set_national_number(uint64{94777892});
  test_number.set_italian_leading_zero(false);
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::SG(),
                                              &formatted_number);
  EXPECT_EQ("9477 7892", formatted_number);

  test_number.set_country_code(800);
  test_number.set_national_number(uint64{12345678});
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::US(),
                                              &formatted_number);
  EXPECT_EQ("011 800 1234 5678", formatted_number);

  test_number.set_country_code(54);
  test_number.set_national_number(uint64{91187654321});
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

TEST_F(PhoneNumberUtilTest, FormatOutOfCountryWithInvalidRegion) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(1);
  test_number.set_national_number(uint64{6502530000});
  // AQ/Antarctica isn't a valid region code for phone number formatting,
  // so this falls back to intl formatting.
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::AQ(),
                                              &formatted_number);
  EXPECT_EQ("+1 650 253 0000", formatted_number);
  // For region code 001, the out-of-country format always turns into the
  // international format.
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::UN001(),
                                              &formatted_number);
  EXPECT_EQ("+1 650 253 0000", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatOutOfCountryWithPreferredIntlPrefix) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(39);
  test_number.set_national_number(uint64{236618300});
  test_number.set_italian_leading_zero(true);
  // This should use 0011, since that is the preferred international prefix
  // (both 0011 and 0012 are accepted as possible international prefixes in our
  // test metadata.)
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::AU(),
                                              &formatted_number);
  EXPECT_EQ("0011 39 02 3661 8300", formatted_number);
  
  // Testing preferred international prefixes with ~ are supported (designates
  // waiting).
  phone_util_.FormatOutOfCountryCallingNumber(test_number, RegionCode::UZ(),
                                              &formatted_number);
  EXPECT_EQ("8~10 39 02 3661 8300", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatOutOfCountryKeepingAlphaChars) {
  PhoneNumber alpha_numeric_number;
  string formatted_number;
  alpha_numeric_number.set_country_code(1);
  alpha_numeric_number.set_national_number(uint64{8007493524});
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
  alpha_numeric_number.set_national_number(uint64{827493524});
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

  alpha_numeric_number.set_national_number(uint64{18007493524});
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
  alpha_numeric_number.set_national_number(uint64{1800749352});
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
  // Testing the case of calling from a non-supported region.
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AQ(),
                                                  &formatted_number);
  EXPECT_EQ("+61 1-800-SIX-FLAG", formatted_number);

  // Testing the case with an invalid country code.
  formatted_number.clear();
  alpha_numeric_number.set_country_code(0);
  alpha_numeric_number.set_national_number(uint64{18007493524});
  alpha_numeric_number.set_raw_input("1-800-SIX-flag");
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::DE(),
                                                  &formatted_number);
  // Uses the raw input only.
  EXPECT_EQ("1-800-SIX-flag", formatted_number);

  // Testing the case of an invalid alpha number.
  formatted_number.clear();
  alpha_numeric_number.set_country_code(1);
  alpha_numeric_number.set_national_number(uint64{80749});
  alpha_numeric_number.set_raw_input("180-SIX");
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::DE(),
                                                  &formatted_number);
  // No country-code stripping can be done.
  EXPECT_EQ("00 1 180-SIX", formatted_number);
  // Testing the case of calling from a non-supported region.
  phone_util_.FormatOutOfCountryKeepingAlphaChars(alpha_numeric_number,
                                                  RegionCode::AQ(),
                                                  &formatted_number);
  // No country-code stripping can be done since the number is invalid.
  EXPECT_EQ("+1 180-SIX", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatWithCarrierCode) {
  // We only support this for AR in our test metadata.
  PhoneNumber ar_number;
  string formatted_number;
  ar_number.set_country_code(54);
  ar_number.set_national_number(uint64{91234125678});
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
  us_number.set_national_number(uint64{4241231234});
  phone_util_.Format(us_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("424 123 1234", formatted_number);
  phone_util_.FormatNationalNumberWithCarrierCode(us_number, "15",
                                                  &formatted_number);
  EXPECT_EQ("424 123 1234", formatted_number);

  // Invalid country code should just get the NSN.
  PhoneNumber invalid_number;
  invalid_number.set_country_code(kInvalidCountryCode);
  invalid_number.set_national_number(uint64{12345});
  phone_util_.FormatNationalNumberWithCarrierCode(invalid_number, "89",
                                                  &formatted_number);
  EXPECT_EQ("12345", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatWithPreferredCarrierCode) {
  // We only support this for AR in our test metadata.
  PhoneNumber ar_number;
  string formatted_number;
  ar_number.set_country_code(54);
  ar_number.set_national_number(uint64{91234125678});
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
  // When the preferred_domestic_carrier_code is present (even when it is just a
  // space), use it instead of the default carrier code passed in.
  ar_number.set_preferred_domestic_carrier_code(" ");
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(ar_number, "15",
                                                           &formatted_number);
  EXPECT_EQ("01234   12-5678", formatted_number);
  // When the preferred_domestic_carrier_code is present but empty, treat it as
  // unset and use instead the default carrier code passed in.
  ar_number.set_preferred_domestic_carrier_code("");
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(ar_number, "15",
                                                           &formatted_number);
  EXPECT_EQ("01234 15 12-5678", formatted_number);
  // We don't support this for the US so there should be no change.
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(uint64{4241231234});
  us_number.set_preferred_domestic_carrier_code("99");
  phone_util_.Format(us_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("424 123 1234", formatted_number);
  phone_util_.FormatNationalNumberWithPreferredCarrierCode(us_number, "15",
                                                           &formatted_number);
  EXPECT_EQ("424 123 1234", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatNumberForMobileDialing) {
  PhoneNumber test_number;
  string formatted_number;

  // Numbers are normally dialed in national format in-country, and
  // international format from outside the country.
  test_number.set_country_code(57);
  test_number.set_national_number(uint64{6012345678});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::CO(), false, /* remove formatting */
      &formatted_number);
  EXPECT_EQ("6012345678", formatted_number);
  test_number.set_country_code(49);
  test_number.set_national_number(uint64{30123456});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::DE(), false, /* remove formatting */
      &formatted_number);
  EXPECT_EQ("030123456", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::CH(), false, /* remove formatting */
      &formatted_number);
  EXPECT_EQ("+4930123456", formatted_number);

  test_number.set_extension("1234");
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::DE(), false, /* remove formatting */
      &formatted_number);
  EXPECT_EQ("030123456", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::CH(), false, /* remove formatting */
      &formatted_number);
  EXPECT_EQ("+4930123456", formatted_number);

  test_number.set_country_code(1);
  test_number.clear_extension();
  // US toll free numbers are marked as noInternationalDialling in the test
  // metadata for testing purposes. For such numbers, we expect nothing to be
  // returned when the region code is not the same one.
  test_number.set_national_number(uint64{8002530000});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), true, /* keep formatting */
      &formatted_number);
  EXPECT_EQ("800 253 0000", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::CN(), true, &formatted_number);
  EXPECT_EQ("", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, /* remove formatting */
      &formatted_number);
  EXPECT_EQ("8002530000", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::CN(), false, &formatted_number);
  EXPECT_EQ("", formatted_number);

  test_number.set_national_number(uint64{6502530000});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), true, &formatted_number);
  EXPECT_EQ("+1 650 253 0000", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, &formatted_number);
  EXPECT_EQ("+16502530000", formatted_number);

  test_number.set_extension("1234");
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), true, &formatted_number);
  EXPECT_EQ("+1 650 253 0000", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, &formatted_number);
  EXPECT_EQ("+16502530000", formatted_number);

  // An invalid US number, which is one digit too long.
  test_number.set_national_number(uint64{65025300001});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), true, &formatted_number);
  EXPECT_EQ("+1 65025300001", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, &formatted_number);
  EXPECT_EQ("+165025300001", formatted_number);

  // Star numbers. In real life they appear in Israel, but we have them in JP
  // in our test metadata.
  test_number.set_country_code(81);
  test_number.set_national_number(uint64{2345});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::JP(), true, &formatted_number);
  EXPECT_EQ("*2345", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::JP(), false, &formatted_number);
  EXPECT_EQ("*2345", formatted_number);

  test_number.set_country_code(800);
  test_number.set_national_number(uint64{12345678});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::JP(), false, &formatted_number);
  EXPECT_EQ("+80012345678", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::JP(), true, &formatted_number);
  EXPECT_EQ("+800 1234 5678", formatted_number);

  // UAE numbers beginning with 600 (classified as UAN) need to be dialled
  // without +971 locally.
  test_number.set_country_code(971);
  test_number.set_national_number(uint64{600123456});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::JP(), false, &formatted_number);
  EXPECT_EQ("+971600123456", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::AE(), true, &formatted_number);
  EXPECT_EQ("600123456", formatted_number);

  test_number.set_country_code(52);
  test_number.set_national_number(uint64{3312345678});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::MX(), false, &formatted_number);
  EXPECT_EQ("+523312345678", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, &formatted_number);
  EXPECT_EQ("+523312345678", formatted_number);

  // Test whether Uzbek phone numbers are returned in international format even
  // when dialled from same region or other regions.
  // Fixed-line number
  test_number.set_country_code(998);
  test_number.set_national_number(uint64{612201234});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::UZ(), false, &formatted_number);
  EXPECT_EQ("+998612201234", formatted_number);
  // Mobile number
  test_number.set_country_code(998);
  test_number.set_national_number(uint64{950123456});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::UZ(), false, &formatted_number);
  EXPECT_EQ("+998950123456", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, &formatted_number);
  EXPECT_EQ("+998950123456", formatted_number);

  // Non-geographical numbers should always be dialed in international format.
  test_number.set_country_code(800);
  test_number.set_national_number(uint64{12345678});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, &formatted_number);
  EXPECT_EQ("+80012345678", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::UN001(), false, &formatted_number);
  EXPECT_EQ("+80012345678", formatted_number);

  // Test that a short number is formatted correctly for mobile dialing within
  // the region, and is not diallable from outside the region.
  test_number.set_country_code(49);
  test_number.set_national_number(123L);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::DE(), false, &formatted_number);
  EXPECT_EQ("123", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::IT(), false, &formatted_number);
  EXPECT_EQ("", formatted_number);

  // Test the special logic for NANPA countries, for which regular length phone
  // numbers are always output in international format, but short numbers are
  // in national format.
  test_number.set_country_code(1);
  test_number.set_national_number(uint64{6502530000});
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, &formatted_number);
  EXPECT_EQ("+16502530000", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::CA(), false, &formatted_number);
  EXPECT_EQ("+16502530000", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::BR(), false, &formatted_number);
  EXPECT_EQ("+16502530000", formatted_number);
  test_number.set_national_number(911L);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::US(), false, &formatted_number);
  EXPECT_EQ("911", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::CA(), false, &formatted_number);
  EXPECT_EQ("", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::BR(), false, &formatted_number);
  EXPECT_EQ("", formatted_number);
  // Test that the Australian emergency number 000 is formatted correctly.
  test_number.set_country_code(61);
  test_number.set_national_number(0L);
  test_number.set_italian_leading_zero(true);
  test_number.set_number_of_leading_zeros(2);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::AU(), false, &formatted_number);
  EXPECT_EQ("000", formatted_number);
  phone_util_.FormatNumberForMobileDialing(
      test_number, RegionCode::NZ(), false, &formatted_number);
  EXPECT_EQ("", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatByPattern) {
  PhoneNumber test_number;
  string formatted_number;
  test_number.set_country_code(1);
  test_number.set_national_number(uint64{6502530000});

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
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::RFC3966,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("tel:+1-650-253-0000", formatted_number);

  // $NP is set to '1' for the US. Here we check that for other NANPA countries
  // the US rules are followed.
  number_format->set_national_prefix_formatting_rule("$NP ($FG)");
  number_format->set_format("$1 $2-$3");
  test_number.set_country_code(1);
  test_number.set_national_number(uint64{4168819999});
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::NATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("1 (416) 881-9999", formatted_number);
  phone_util_.FormatByPattern(test_number, PhoneNumberUtil::INTERNATIONAL,
                              number_formats,
                              &formatted_number);
  EXPECT_EQ("+1 416 881-9999", formatted_number);

  test_number.set_country_code(39);
  test_number.set_national_number(uint64{236618300});
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
  test_number.set_national_number(uint64{2012345678});
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
  test_number.set_national_number(uint64{6502530000});
  phone_util_.Format(test_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+16502530000", formatted_number);

  test_number.set_country_code(49);
  test_number.set_national_number(uint64{301234});
  phone_util_.Format(test_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+49301234", formatted_number);

  test_number.set_country_code(800);
  test_number.set_national_number(uint64{12345678});
  phone_util_.Format(test_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+80012345678", formatted_number);
}

TEST_F(PhoneNumberUtilTest, FormatNumberWithExtension) {
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(uint64{33316005});
  nz_number.set_extension("1234");
  string formatted_number;
  // Uses default extension prefix:
  phone_util_.Format(nz_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("03-331 6005 ext. 1234", formatted_number);
  // Uses RFC 3966 syntax.
  phone_util_.Format(nz_number, PhoneNumberUtil::RFC3966, &formatted_number);
  EXPECT_EQ("tel:+64-3-331-6005;ext=1234", formatted_number);
  // Extension prefix overridden in the territory information for the US:
  PhoneNumber us_number_with_extension;
  us_number_with_extension.set_country_code(1);
  us_number_with_extension.set_national_number(uint64{6502530000});
  us_number_with_extension.set_extension("4567");
  phone_util_.Format(us_number_with_extension,
                     PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("650 253 0000 extn. 4567", formatted_number);
}

TEST_F(PhoneNumberUtilTest, GetLengthOfGeographicalAreaCode) {
  PhoneNumber number;
  // Google MTV, which has area code "650".
  number.set_country_code(1);
  number.set_national_number(uint64{6502530000});
  EXPECT_EQ(3, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // A North America toll-free number, which has no area code.
  number.set_country_code(1);
  number.set_national_number(uint64{8002530000});
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // An invalid US number (1 digit shorter), which has no area code.
  number.set_country_code(1);
  number.set_national_number(uint64{650253000});
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Google London, which has area code "20".
  number.set_country_code(44);
  number.set_national_number(uint64{2070313000});
  EXPECT_EQ(2, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // A mobile number in the UK does not have an area code (by default, mobile
  // numbers do not, unless they have been added to our list of exceptions).
  number.set_country_code(44);
  number.set_national_number(uint64{7912345678});
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Google Buenos Aires, which has area code "11".
  number.set_country_code(54);
  number.set_national_number(uint64{1155303000});
  EXPECT_EQ(2, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // A mobile number in Argentina also has an area code.
  number.set_country_code(54);
  number.set_national_number(uint64{91187654321});
  EXPECT_EQ(3, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Google Sydney, which has area code "2".
  number.set_country_code(61);
  number.set_national_number(uint64{293744000});
  EXPECT_EQ(1, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Italian numbers - there is no national prefix, but it still has an area
  // code.
  number.set_country_code(39);
  number.set_national_number(uint64{236618300});
  number.set_italian_leading_zero(true);
  EXPECT_EQ(2, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // Google Singapore. Singapore has no area code and no national prefix.
  number.set_country_code(65);
  number.set_national_number(uint64{65218000});
  number.set_italian_leading_zero(false);
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // An international toll free number, which has no area code.
  number.set_country_code(800);
  number.set_national_number(uint64{12345678});
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(number));

  // A mobile number from China is geographical, but does not have an area code.
  PhoneNumber cn_mobile;
  cn_mobile.set_country_code(86);
  cn_mobile.set_national_number(uint64{18912341234});
  EXPECT_EQ(0, phone_util_.GetLengthOfGeographicalAreaCode(cn_mobile));
}

TEST_F(PhoneNumberUtilTest, GetLengthOfNationalDestinationCode) {
  PhoneNumber number;
  // Google MTV, which has national destination code (NDC) "650".
  number.set_country_code(1);
  number.set_national_number(uint64{6502530000});
  EXPECT_EQ(3, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A North America toll-free number, which has NDC "800".
  number.set_country_code(1);
  number.set_national_number(uint64{8002530000});
  EXPECT_EQ(3, phone_util_.GetLengthOfNationalDestinationCode(number));

  // Google London, which has NDC "20".
  number.set_country_code(44);
  number.set_national_number(uint64{2070313000});
  EXPECT_EQ(2, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A UK mobile phone, which has NDC "7912"
  number.set_country_code(44);
  number.set_national_number(uint64{7912345678});
  EXPECT_EQ(4, phone_util_.GetLengthOfNationalDestinationCode(number));

  // Google Buenos Aires, which has NDC "11".
  number.set_country_code(54);
  number.set_national_number(uint64{1155303000});
  EXPECT_EQ(2, phone_util_.GetLengthOfNationalDestinationCode(number));

  // An Argentinian mobile which has NDC "911".
  number.set_country_code(54);
  number.set_national_number(uint64{91187654321});
  EXPECT_EQ(3, phone_util_.GetLengthOfNationalDestinationCode(number));

  // Google Sydney, which has NDC "2".
  number.set_country_code(61);
  number.set_national_number(uint64{293744000});
  EXPECT_EQ(1, phone_util_.GetLengthOfNationalDestinationCode(number));

  // Google Singapore. Singapore has NDC "6521".
  number.set_country_code(65);
  number.set_national_number(uint64{65218000});
  EXPECT_EQ(4, phone_util_.GetLengthOfNationalDestinationCode(number));

  // An invalid US number (1 digit shorter), which has no NDC.
  number.set_country_code(1);
  number.set_national_number(uint64{650253000});
  EXPECT_EQ(0, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A number containing an invalid country code, which shouldn't have any NDC.
  number.set_country_code(123);
  number.set_national_number(uint64{650253000});
  EXPECT_EQ(0, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A number that has only one group of digits after country code when
  // formatted in the international format.
  number.set_country_code(376);
  number.set_national_number(uint64{12345});
  EXPECT_EQ(0, phone_util_.GetLengthOfNationalDestinationCode(number));

  // The same number above, but with an extension.
  number.set_country_code(376);
  number.set_national_number(uint64{12345});
  number.set_extension("321");
  EXPECT_EQ(0, phone_util_.GetLengthOfNationalDestinationCode(number));

  // An international toll free number, which has NDC "1234".
  number.Clear();
  number.set_country_code(800);
  number.set_national_number(uint64{12345678});
  EXPECT_EQ(4, phone_util_.GetLengthOfNationalDestinationCode(number));

  // A mobile number from China is geographical, but does not have an area code:
  // however it still can be considered to have a national destination code.
  PhoneNumber cn_mobile;
  cn_mobile.set_country_code(86);
  cn_mobile.set_national_number(uint64{18912341234});
  EXPECT_EQ(3, phone_util_.GetLengthOfNationalDestinationCode(cn_mobile));
}

TEST_F(PhoneNumberUtilTest, GetCountryMobileToken) {
  int country_calling_code;
  string mobile_token;

  country_calling_code = phone_util_.GetCountryCodeForRegion(RegionCode::AR());
  phone_util_.GetCountryMobileToken(country_calling_code, &mobile_token);
  EXPECT_EQ("9", mobile_token);

  // Country calling code for Sweden, which has no mobile token.
  country_calling_code = phone_util_.GetCountryCodeForRegion(RegionCode::SE());
  phone_util_.GetCountryMobileToken(country_calling_code, &mobile_token);
  EXPECT_EQ("", mobile_token);
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
  EXPECT_FALSE(phone_util_.IsNANPACountry(RegionCode::DE()));
  EXPECT_FALSE(phone_util_.IsNANPACountry(RegionCode::GetUnknown()));
  EXPECT_FALSE(phone_util_.IsNANPACountry(RegionCode::UN001()));
}

TEST_F(PhoneNumberUtilTest, IsValidNumber) {
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(uint64{6502530000});
  EXPECT_TRUE(phone_util_.IsValidNumber(us_number));

  PhoneNumber it_number;
  it_number.set_country_code(39);
  it_number.set_national_number(uint64{236618300});
  it_number.set_italian_leading_zero(true);
  EXPECT_TRUE(phone_util_.IsValidNumber(it_number));

  PhoneNumber gb_number;
  gb_number.set_country_code(44);
  gb_number.set_national_number(uint64{7912345678});
  EXPECT_TRUE(phone_util_.IsValidNumber(gb_number));

  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(uint64{21387835});
  EXPECT_TRUE(phone_util_.IsValidNumber(nz_number));

  PhoneNumber intl_toll_free_number;
  intl_toll_free_number.set_country_code(800);
  intl_toll_free_number.set_national_number(uint64{12345678});
  EXPECT_TRUE(phone_util_.IsValidNumber(intl_toll_free_number));

  PhoneNumber universal_premium_rate;
  universal_premium_rate.set_country_code(979);
  universal_premium_rate.set_national_number(uint64{123456789});
  EXPECT_TRUE(phone_util_.IsValidNumber(universal_premium_rate));
}

TEST_F(PhoneNumberUtilTest, IsValidForRegion) {
  // This number is valid for the Bahamas, but is not a valid US number.
  PhoneNumber bs_number;
  bs_number.set_country_code(1);
  bs_number.set_national_number(uint64{2423232345});
  EXPECT_TRUE(phone_util_.IsValidNumber(bs_number));
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(bs_number, RegionCode::BS()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(bs_number, RegionCode::US()));
  bs_number.set_national_number(uint64{2421232345});
  // This number is no longer valid.
  EXPECT_FALSE(phone_util_.IsValidNumber(bs_number));

  // La Mayotte and Réunion use 'leadingDigits' to differentiate them.
  PhoneNumber re_number;
  re_number.set_country_code(262);
  re_number.set_national_number(uint64{262123456});
  EXPECT_TRUE(phone_util_.IsValidNumber(re_number));
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::RE()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::YT()));
  // Now change the number to be a number for La Mayotte.
  re_number.set_national_number(uint64{269601234});
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::YT()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::RE()));
  // This number is no longer valid.
  re_number.set_national_number(uint64{269123456});
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::YT()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::RE()));
  EXPECT_FALSE(phone_util_.IsValidNumber(re_number));
  // However, it should be recognised as from La Mayotte.
  string region_code;
  phone_util_.GetRegionCodeForNumber(re_number, &region_code);
  EXPECT_EQ(RegionCode::YT(), region_code);
  // This number is valid in both places.
  re_number.set_national_number(uint64{800123456});
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::YT()));
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(re_number, RegionCode::RE()));

  PhoneNumber intl_toll_free_number;
  intl_toll_free_number.set_country_code(800);
  intl_toll_free_number.set_national_number(uint64{12345678});
  EXPECT_TRUE(phone_util_.IsValidNumberForRegion(intl_toll_free_number,
                                                 RegionCode::UN001()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(intl_toll_free_number,
                                                  RegionCode::US()));
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(intl_toll_free_number,
                                                  RegionCode::ZZ()));

  PhoneNumber invalid_number;
  // Invalid country calling codes.
  invalid_number.set_country_code(3923);
  invalid_number.set_national_number(uint64{2366});
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(invalid_number,
                                                  RegionCode::ZZ()));
  invalid_number.set_country_code(3923);
  invalid_number.set_national_number(uint64{2366});
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(invalid_number,
                                                  RegionCode::UN001()));
  invalid_number.set_country_code(0);
  invalid_number.set_national_number(uint64{2366});
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(invalid_number,
                                                  RegionCode::UN001()));
  invalid_number.set_country_code(0);
  EXPECT_FALSE(phone_util_.IsValidNumberForRegion(invalid_number,
                                                  RegionCode::ZZ()));
}

TEST_F(PhoneNumberUtilTest, IsNotValidNumber) {
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(uint64{2530000});
  EXPECT_FALSE(phone_util_.IsValidNumber(us_number));

  PhoneNumber it_number;
  it_number.set_country_code(39);
  it_number.set_national_number(uint64{23661830000});
  it_number.set_italian_leading_zero(true);
  EXPECT_FALSE(phone_util_.IsValidNumber(it_number));

  PhoneNumber gb_number;
  gb_number.set_country_code(44);
  gb_number.set_national_number(uint64{791234567});
  EXPECT_FALSE(phone_util_.IsValidNumber(gb_number));

  PhoneNumber de_number;
  de_number.set_country_code(49);
  de_number.set_national_number(uint64{1234});
  EXPECT_FALSE(phone_util_.IsValidNumber(de_number));

  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(uint64{3316005});
  EXPECT_FALSE(phone_util_.IsValidNumber(nz_number));

  PhoneNumber invalid_number;
  // Invalid country calling codes.
  invalid_number.set_country_code(3923);
  invalid_number.set_national_number(uint64{2366});
  EXPECT_FALSE(phone_util_.IsValidNumber(invalid_number));
  invalid_number.set_country_code(0);
  EXPECT_FALSE(phone_util_.IsValidNumber(invalid_number));

  PhoneNumber intl_toll_free_number_too_long;
  intl_toll_free_number_too_long.set_country_code(800);
  intl_toll_free_number_too_long.set_national_number(uint64{123456789});
  EXPECT_FALSE(phone_util_.IsValidNumber(intl_toll_free_number_too_long));
}

TEST_F(PhoneNumberUtilTest, GetRegionCodeForCountryCode) {
  string region_code;
  phone_util_.GetRegionCodeForCountryCode(1, &region_code);
  EXPECT_EQ(RegionCode::US(), region_code);
  phone_util_.GetRegionCodeForCountryCode(44, &region_code);
  EXPECT_EQ(RegionCode::GB(), region_code);
  phone_util_.GetRegionCodeForCountryCode(49, &region_code);
  EXPECT_EQ(RegionCode::DE(), region_code);
  phone_util_.GetRegionCodeForCountryCode(800, &region_code);
  EXPECT_EQ(RegionCode::UN001(), region_code);
  phone_util_.GetRegionCodeForCountryCode(979, &region_code);
  EXPECT_EQ(RegionCode::UN001(), region_code);
}

TEST_F(PhoneNumberUtilTest, GetRegionCodeForNumber) {
  string region_code;
  PhoneNumber bs_number;
  bs_number.set_country_code(1);
  bs_number.set_national_number(uint64{2423232345});
  phone_util_.GetRegionCodeForNumber(bs_number, &region_code);
  EXPECT_EQ(RegionCode::BS(), region_code);

  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(uint64{4241231234});
  phone_util_.GetRegionCodeForNumber(us_number, &region_code);
  EXPECT_EQ(RegionCode::US(), region_code);

  PhoneNumber gb_mobile;
  gb_mobile.set_country_code(44);
  gb_mobile.set_national_number(uint64{7912345678});
  phone_util_.GetRegionCodeForNumber(gb_mobile, &region_code);
  EXPECT_EQ(RegionCode::GB(), region_code);

  PhoneNumber intl_toll_free_number;
  intl_toll_free_number.set_country_code(800);
  intl_toll_free_number.set_national_number(uint64{12345678});
  phone_util_.GetRegionCodeForNumber(intl_toll_free_number, &region_code);
  EXPECT_EQ(RegionCode::UN001(), region_code);

  PhoneNumber universal_premium_rate;
  universal_premium_rate.set_country_code(979);
  universal_premium_rate.set_national_number(uint64{123456789});
  phone_util_.GetRegionCodeForNumber(universal_premium_rate, &region_code);
  EXPECT_EQ(RegionCode::UN001(), region_code);
}

TEST_F(PhoneNumberUtilTest, IsPossibleNumber) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{6502530000});
  EXPECT_TRUE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(1);
  number.set_national_number(uint64{2530000});
  EXPECT_TRUE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(44);
  number.set_national_number(uint64{2070313000});
  EXPECT_TRUE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(800);
  number.set_national_number(uint64{12345678});
  EXPECT_TRUE(phone_util_.IsPossibleNumber(number));

  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+1 650 253 0000",
                                                    RegionCode::US()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+1 650 GOO OGLE",
                                                    RegionCode::US()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("(650) 253-0000",
                                                    RegionCode::US()));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForString("253-0000", RegionCode::US()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+1 650 253 0000",
                                                    RegionCode::GB()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+44 20 7031 3000",
                                                    RegionCode::GB()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("(020) 7031 300",
                                                    RegionCode::GB()));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForString("7031 3000", RegionCode::GB()));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForString("3331 6005", RegionCode::NZ()));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForString("+800 1234 5678",
                                                    RegionCode::UN001()));
}

TEST_F(PhoneNumberUtilTest, IsPossibleNumberForType_DifferentTypeLengths) {
  // We use Argentinian numbers since they have different possible lengths for
  // different types.
  PhoneNumber number;
  number.set_country_code(54);
  number.set_national_number(uint64{12345});
  // Too short for any Argentinian number, including fixed-line.
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::UNKNOWN));

  // 6-digit numbers are okay for fixed-line.
  number.set_national_number(uint64{123456});
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::UNKNOWN));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  // But too short for mobile.
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::MOBILE));
  // And too short for toll-free.
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::TOLL_FREE));

  // The same applies to 9-digit numbers.
  number.set_national_number(uint64{123456789});
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::UNKNOWN));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::MOBILE));
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::TOLL_FREE));

  // 10-digit numbers are universally possible.
  number.set_national_number(uint64{1234567890});
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::UNKNOWN));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::MOBILE));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::TOLL_FREE));

  // 11-digit numbers are only possible for mobile numbers. Note we don't
  // require the leading 9, which all mobile numbers start with, and would be
  // required for a valid mobile number.
  number.set_national_number(uint64{12345678901});
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::UNKNOWN));
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::MOBILE));
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::TOLL_FREE));
}

TEST_F(PhoneNumberUtilTest, IsPossibleNumberForType_LocalOnly) {
  PhoneNumber number;
  // Here we test a number length which matches a local-only length.
  number.set_country_code(49);
  number.set_national_number(uint64{12});
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::UNKNOWN));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  // Mobile numbers must be 10 or 11 digits, and there are no local-only
  // lengths.
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::MOBILE));
}

TEST_F(PhoneNumberUtilTest, IsPossibleNumberForType_DataMissingForSizeReasons) {
  PhoneNumber number;
  // Here we test something where the possible lengths match the possible
  // lengths of the country as a whole, and hence aren't present in the binary
  // for size reasons - this should still work.
  // Local-only number.
  number.set_country_code(55);
  number.set_national_number(uint64{12345678});
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::UNKNOWN));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  number.set_national_number(uint64{1234567890});
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::UNKNOWN));
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
}

TEST_F(PhoneNumberUtilTest,
       IsPossibleNumberForType_NumberTypeNotSupportedForRegion) {
  PhoneNumber number;
  // There are *no* mobile numbers for this region at all, so we return false.
  number.set_country_code(55);
  number.set_national_number(12345678L);
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::MOBILE));
  // This matches a fixed-line length though.
  EXPECT_TRUE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForType(
      number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));

  // There are *no* fixed-line OR mobile numbers for this country calling code
  // at all, so we return false for these.
  number.set_country_code(979);
  number.set_national_number(123456789L);
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::MOBILE));
  EXPECT_FALSE(
      phone_util_.IsPossibleNumberForType(number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_FALSE(phone_util_.IsPossibleNumberForType(
      number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));
  EXPECT_TRUE(phone_util_.IsPossibleNumberForType(
      number, PhoneNumberUtil::PREMIUM_RATE));
}

TEST_F(PhoneNumberUtilTest, IsPossibleNumberWithReason) {
  // FYI, national numbers for country code +1 that are within 7 to 10 digits
  // are possible.
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{6502530000});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(1);
  number.set_national_number(uint64{2530000});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE_LOCAL_ONLY,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(0);
  number.set_national_number(uint64{2530000});
  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(1);
  number.set_national_number(uint64{253000});
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(1);
  number.set_national_number(uint64{65025300000});
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(44);
  number.set_national_number(uint64{2070310000});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(49);
  number.set_national_number(uint64{30123456});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(65);
  number.set_national_number(uint64{1234567890});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberWithReason(number));

  number.set_country_code(800);
  number.set_national_number(uint64{123456789});
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberWithReason(number));
}

TEST_F(PhoneNumberUtilTest,
       IsPossibleNumberForTypeWithReason_DifferentTypeLengths) {
  // We use Argentinian numbers since they have different possible lengths for
  // different types.
  PhoneNumber number;
  number.set_country_code(54);
  number.set_national_number(uint64{12345});
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::UNKNOWN));
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));

  // 6-digit numbers are okay for fixed-line.
  number.set_national_number(uint64{123456});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::UNKNOWN));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  // But too short for mobile.
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  // And too short for toll-free.
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::TOLL_FREE));
  // The same applies to 9-digit numbers.
  number.set_national_number(uint64{123456789});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::UNKNOWN));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::TOLL_FREE));
  // 10-digit numbers are universally possible.
  number.set_national_number(uint64{1234567890});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::UNKNOWN));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::TOLL_FREE));
  // 11-digit numbers are possible for mobile numbers. Note we don't require the
  // leading 9, which all mobile numbers start with, and would be required for a
  // valid mobile number.
  number.set_national_number(uint64{12345678901});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::UNKNOWN));
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::TOLL_FREE));
}

TEST_F(PhoneNumberUtilTest, IsPossibleNumberForTypeWithReason_LocalOnly) {
  PhoneNumber number;
  // Here we test a number length which matches a local-only length.
  number.set_country_code(49);
  number.set_national_number(uint64{12});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE_LOCAL_ONLY,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::UNKNOWN));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE_LOCAL_ONLY,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  // Mobile numbers must be 10 or 11 digits, and there are no local-only
  // lengths.
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
}

TEST_F(PhoneNumberUtilTest,
       IsPossibleNumberForTypeWithReason_DataMissingForSizeReasons) {
  PhoneNumber number;
  // Here we test something where the possible lengths match the possible
  // lengths of the country as a whole, and hence aren't present in the binary
  // for size reasons - this should still work.
  // Local-only number.
  number.set_country_code(55);
  number.set_national_number(uint64{12345678});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE_LOCAL_ONLY,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::UNKNOWN));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE_LOCAL_ONLY,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  // Normal-length number.
  number.set_national_number(uint64{1234567890});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::UNKNOWN));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
}

TEST_F(PhoneNumberUtilTest,
       IsPossibleNumberForTypeWithReason_NumberTypeNotSupportedForRegion) {
  PhoneNumber number;
  // There are *no* mobile numbers for this region at all, so we return
  // INVALID_LENGTH.
  number.set_country_code(55);
  number.set_national_number(uint64{12345678});
  EXPECT_EQ(PhoneNumberUtil::INVALID_LENGTH,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  // This matches a fixed-line length though.
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE_LOCAL_ONLY,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));
  // This is too short for fixed-line, and no mobile numbers exist.
  number.set_national_number(uint64{1234567});
  EXPECT_EQ(PhoneNumberUtil::INVALID_LENGTH,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  // This is too short for mobile, and no fixed-line number exist.
  number.set_country_code(882);
  number.set_national_number(uint64{1234567});
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));
  EXPECT_EQ(PhoneNumberUtil::INVALID_LENGTH,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));

  // There are *no* fixed-line OR mobile numbers for this country calling code
  // at all, so we return INVALID_LENGTH.
  number.set_country_code(979);
  number.set_national_number(uint64{123456789});
  EXPECT_EQ(PhoneNumberUtil::INVALID_LENGTH,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::INVALID_LENGTH,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_EQ(PhoneNumberUtil::INVALID_LENGTH,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::PREMIUM_RATE));
}

TEST_F(PhoneNumberUtilTest,
       IsPossibleNumberForTypeWithReason_FixedLineOrMobile) {
  PhoneNumber number;
  // For FIXED_LINE_OR_MOBILE, a number should be considered valid if it matches
  // the possible lengths for mobile *or* fixed-line numbers.
  number.set_country_code(290);
  number.set_national_number(uint64{1234});
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));

  number.set_national_number(uint64{12345});
  EXPECT_EQ(PhoneNumberUtil::TOO_SHORT,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::INVALID_LENGTH,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));

  number.set_national_number(uint64{123456});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));

  number.set_national_number(uint64{1234567});
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE));
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::MOBILE));
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));

  number.set_national_number(uint64{12345678});
  EXPECT_EQ(PhoneNumberUtil::IS_POSSIBLE,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::TOLL_FREE));
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG,
            phone_util_.IsPossibleNumberForTypeWithReason(
                number, PhoneNumberUtil::FIXED_LINE_OR_MOBILE));
}

TEST_F(PhoneNumberUtilTest, IsNotPossibleNumber) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{65025300000});
  EXPECT_FALSE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(800);
  number.set_national_number(uint64{123456789});
  EXPECT_FALSE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(1);
  number.set_national_number(uint64{253000});
  EXPECT_FALSE(phone_util_.IsPossibleNumber(number));

  number.set_country_code(44);
  number.set_national_number(uint64{300});
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
  EXPECT_FALSE(phone_util_.IsPossibleNumberForString("+800 1234 5678 9",
                                                     RegionCode::UN001()));
}

TEST_F(PhoneNumberUtilTest, TruncateTooLongNumber) {
  // US number 650-253-0000, but entered with one additional digit at the end.
  PhoneNumber too_long_number;
  too_long_number.set_country_code(1);
  too_long_number.set_national_number(uint64{65025300001});
  PhoneNumber valid_number;
  valid_number.set_country_code(1);
  valid_number.set_national_number(uint64{6502530000});
  EXPECT_TRUE(phone_util_.TruncateTooLongNumber(&too_long_number));
  EXPECT_EQ(valid_number, too_long_number);

  too_long_number.set_country_code(800);
  too_long_number.set_national_number(uint64{123456789});
  valid_number.set_country_code(800);
  valid_number.set_national_number(uint64{12345678});
  EXPECT_TRUE(phone_util_.TruncateTooLongNumber(&too_long_number));
  EXPECT_EQ(valid_number, too_long_number);

  // GB number 080 1234 5678, but entered with 4 extra digits at the end.
  too_long_number.set_country_code(44);
  too_long_number.set_national_number(uint64{80123456780123});
  valid_number.set_country_code(44);
  valid_number.set_national_number(uint64{8012345678});
  EXPECT_TRUE(phone_util_.TruncateTooLongNumber(&too_long_number));
  EXPECT_EQ(valid_number, too_long_number);

  // IT number 022 3456 7890, but entered with 3 extra digits at the end.
  too_long_number.set_country_code(39);
  too_long_number.set_national_number(uint64{2234567890123});
  too_long_number.set_italian_leading_zero(true);
  valid_number.set_country_code(39);
  valid_number.set_national_number(uint64{2234567890});
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
  number_with_invalid_prefix.set_national_number(uint64{2401234567});
  PhoneNumber invalid_number_copy(number_with_invalid_prefix);
  EXPECT_FALSE(phone_util_.TruncateTooLongNumber(&number_with_invalid_prefix));
  // Tests the number is not modified.
  EXPECT_EQ(invalid_number_copy, number_with_invalid_prefix);

  // Tests what happens when a too short number is passed in.
  PhoneNumber too_short_number;
  too_short_number.set_country_code(1);
  too_short_number.set_national_number(uint64{1234});
  PhoneNumber too_short_number_copy(too_short_number);
  EXPECT_FALSE(phone_util_.TruncateTooLongNumber(&too_short_number));
  // Tests the number is not modified.
  EXPECT_EQ(too_short_number_copy, too_short_number);
}

TEST_F(PhoneNumberUtilTest, IsNumberGeographical) {
  PhoneNumber number;

  // Bahamas, mobile phone number.
  number.set_country_code(1);
  number.set_national_number(uint64{2423570000});
  EXPECT_FALSE(phone_util_.IsNumberGeographical(number));

  // Australian fixed line number.
  number.set_country_code(61);
  number.set_national_number(uint64{236618300});
  EXPECT_TRUE(phone_util_.IsNumberGeographical(number));

  // International toll free number.
  number.set_country_code(800);
  number.set_national_number(uint64{12345678});
  EXPECT_FALSE(phone_util_.IsNumberGeographical(number));

  // We test that mobile phone numbers in relevant regions are indeed considered
  // geographical.

  // Argentina, mobile phone number.
  number.set_country_code(54);
  number.set_national_number(uint64{91187654321});
  EXPECT_TRUE(phone_util_.IsNumberGeographical(number));

  // Mexico, mobile phone number.
  number.set_country_code(52);
  number.set_national_number(uint64{12345678900});
  EXPECT_TRUE(phone_util_.IsNumberGeographical(number));

  // Mexico, another mobile phone number.
  number.set_country_code(52);
  number.set_national_number(uint64{15512345678});
  EXPECT_TRUE(phone_util_.IsNumberGeographical(number));
}

TEST_F(PhoneNumberUtilTest, FormatInOriginalFormat) {
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

  // Invalid numbers that we have a formatting pattern for should be formatted
  // properly.  Note area codes starting with 7 are intentionally excluded in
  // the test metadata for testing purposes.
  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("7345678901", RegionCode::US(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::US(),
                                     &formatted_number);
  EXPECT_EQ("734 567 8901", formatted_number);

  // US is not a leading zero country, and the presence of the leading zero
  // leads us to format the number using raw_input.
  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("0734567 8901", RegionCode::US(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::US(),
                                     &formatted_number);
  EXPECT_EQ("0734567 8901", formatted_number);

  // This number is valid, but we don't have a formatting pattern for it. Fall
  // back to the raw input.
  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("02-4567-8900", RegionCode::KR(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::KR(),
                                     &formatted_number);
  EXPECT_EQ("02-4567-8900", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("01180012345678",
                                             RegionCode::US(), &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::US(),
                                     &formatted_number);
  EXPECT_EQ("011 800 1234 5678", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("+80012345678", RegionCode::KR(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::KR(),
                                     &formatted_number);
  EXPECT_EQ("+800 1234 5678", formatted_number);

  // US local numbers are formatted correctly, as we have formatting patterns
  // for them.
  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("2530000", RegionCode::US(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::US(),
                                     &formatted_number);
  EXPECT_EQ("253 0000", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // Number with national prefix in the US.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("18003456789", RegionCode::US(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::US(),
                                     &formatted_number);
  EXPECT_EQ("1 800 345 6789", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // Number without national prefix in the UK.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("2087654321", RegionCode::GB(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::GB(),
                                     &formatted_number);
  EXPECT_EQ("20 8765 4321", formatted_number);
  // Make sure no metadata is modified as a result of the previous function
  // call.
  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+442087654321", RegionCode::GB(),
                              &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::GB(),
                                     &formatted_number);
  EXPECT_EQ("(020) 8765 4321", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // Number with national prefix in Mexico.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("013312345678", RegionCode::MX(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::MX(),
                                     &formatted_number);
  EXPECT_EQ("01 33 1234 5678", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // Number without national prefix in Mexico.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("3312345678", RegionCode::MX(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::MX(),
                                     &formatted_number);
  EXPECT_EQ("33 1234 5678", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // Italian fixed-line number.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("0212345678", RegionCode::IT(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::IT(),
                                     &formatted_number);
  EXPECT_EQ("02 1234 5678", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // Number with national prefix in Japan.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("00777012", RegionCode::JP(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::JP(),
                                     &formatted_number);
  EXPECT_EQ("0077-7012", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // Number without national prefix in Japan.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("0777012", RegionCode::JP(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::JP(),
                                     &formatted_number);
  EXPECT_EQ("0777012", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // Number with carrier code in Brazil.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("012 3121286979", RegionCode::BR(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::BR(),
                                     &formatted_number);
  EXPECT_EQ("012 3121286979", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  // The default national prefix used in this case is 045. When a number with
  // national prefix 044 is entered, we return the raw input as we don't want to
  // change the number entered.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("044(33)1234-5678",
                                             RegionCode::MX(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::MX(),
                                     &formatted_number);
  EXPECT_EQ("044(33)1234-5678", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("045(33)1234-5678",
                                             RegionCode::MX(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::MX(),
                                     &formatted_number);
  EXPECT_EQ("045 33 1234 5678", formatted_number);

  // The default international prefix used in this case is 0011. When a number
  // with international prefix 0012 is entered, we return the raw input as we
  // don't want to change the number entered.
  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("0012 16502530000",
                                             RegionCode::AU(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::AU(),
                                     &formatted_number);
  EXPECT_EQ("0012 16502530000", formatted_number);

  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("0011 16502530000",
                                             RegionCode::AU(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::AU(),
                                     &formatted_number);
  EXPECT_EQ("0011 1 650 253 0000", formatted_number);

  // Test the star sign is not removed from or added to the original input by
  // this method.
  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("*1234",
                                             RegionCode::JP(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::JP(),
                                     &formatted_number);
  EXPECT_EQ("*1234", formatted_number);
  phone_number.Clear();
  formatted_number.clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("1234",
                                             RegionCode::JP(),
                                             &phone_number));
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::JP(),
                                     &formatted_number);
  EXPECT_EQ("1234", formatted_number);

  // Test that an invalid national number without raw input is just formatted
  // as the national number.
  phone_number.Clear();
  formatted_number.clear();
  phone_number.set_country_code_source(PhoneNumber::FROM_DEFAULT_COUNTRY);
  phone_number.set_country_code(1);
  phone_number.set_national_number(uint64{650253000});
  phone_util_.FormatInOriginalFormat(phone_number, RegionCode::US(),
                                     &formatted_number);
  EXPECT_EQ("650253000", formatted_number);
}

TEST_F(PhoneNumberUtilTest, IsPremiumRate) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{9004433030});
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(39);
  number.set_national_number(uint64{892123});
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(44);
  number.set_national_number(uint64{9187654321});
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(uint64{9001654321});
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(uint64{90091234567});
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));

  number.set_country_code(979);
  number.set_national_number(uint64{123456789});
  EXPECT_EQ(PhoneNumberUtil::PREMIUM_RATE, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsTollFree) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{8881234567});
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));

  number.set_country_code(39);
  number.set_national_number(uint64{803123});
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));

  number.set_country_code(44);
  number.set_national_number(uint64{8012345678});
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(uint64{8001234567});
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));

  number.set_country_code(800);
  number.set_national_number(uint64{12345678});
  EXPECT_EQ(PhoneNumberUtil::TOLL_FREE, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsMobile) {
  PhoneNumber number;
  // A Bahama mobile number
  number.set_country_code(1);
  number.set_national_number(uint64{2423570000});
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));

  number.set_country_code(39);
  number.set_national_number(uint64{312345678});
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));

  number.set_country_code(44);
  number.set_national_number(uint64{7912345678});
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(uint64{15123456789});
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));

  number.set_country_code(54);
  number.set_national_number(uint64{91187654321});
  EXPECT_EQ(PhoneNumberUtil::MOBILE, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsFixedLine) {
  PhoneNumber number;
  // A Bahama fixed-line number
  number.set_country_code(1);
  number.set_national_number(uint64{2423651234});
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE, phone_util_.GetNumberType(number));

  // An Italian fixed-line number
  number.Clear();
  number.set_country_code(39);
  number.set_national_number(uint64{236618300});
  number.set_italian_leading_zero(true);
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE, phone_util_.GetNumberType(number));

  number.Clear();
  number.set_country_code(44);
  number.set_national_number(uint64{2012345678});
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE, phone_util_.GetNumberType(number));

  number.set_country_code(49);
  number.set_national_number(uint64{301234});
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsFixedLineAndMobile) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{6502531111});
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE_OR_MOBILE,
            phone_util_.GetNumberType(number));

  number.set_country_code(54);
  number.set_national_number(uint64{1987654321});
  EXPECT_EQ(PhoneNumberUtil::FIXED_LINE_OR_MOBILE,
            phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsSharedCost) {
  PhoneNumber number;
  number.set_country_code(44);
  number.set_national_number(uint64{8431231234});
  EXPECT_EQ(PhoneNumberUtil::SHARED_COST, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsVoip) {
  PhoneNumber number;
  number.set_country_code(44);
  number.set_national_number(uint64{5631231234});
  EXPECT_EQ(PhoneNumberUtil::VOIP, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsPersonalNumber) {
  PhoneNumber number;
  number.set_country_code(44);
  number.set_national_number(uint64{7031231234});
  EXPECT_EQ(PhoneNumberUtil::PERSONAL_NUMBER,
            phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, IsUnknown) {
  PhoneNumber number;
  number.set_country_code(1);
  number.set_national_number(uint64{65025311111});
  EXPECT_EQ(PhoneNumberUtil::UNKNOWN, phone_util_.GetNumberType(number));
}

TEST_F(PhoneNumberUtilTest, GetCountryCodeForRegion) {
  EXPECT_EQ(1, phone_util_.GetCountryCodeForRegion(RegionCode::US()));
  EXPECT_EQ(64, phone_util_.GetCountryCodeForRegion(RegionCode::NZ()));
  EXPECT_EQ(0, phone_util_.GetCountryCodeForRegion(RegionCode::GetUnknown()));
  EXPECT_EQ(0, phone_util_.GetCountryCodeForRegion(RegionCode::UN001()));
  // CS is already deprecated so the library doesn't support it.
  EXPECT_EQ(0, phone_util_.GetCountryCodeForRegion(RegionCode::CS()));
}

TEST_F(PhoneNumberUtilTest, GetNationalDiallingPrefixForRegion) {
  string ndd_prefix;
  phone_util_.GetNddPrefixForRegion(RegionCode::US(), false, &ndd_prefix);
  EXPECT_EQ("1", ndd_prefix);

  // Test non-main country to see it gets the national dialling prefix for the
  // main country with that country calling code.
  ndd_prefix.clear();
  phone_util_.GetNddPrefixForRegion(RegionCode::BS(), false, &ndd_prefix);
  EXPECT_EQ("1", ndd_prefix);

  ndd_prefix.clear();
  phone_util_.GetNddPrefixForRegion(RegionCode::NZ(), false, &ndd_prefix);
  EXPECT_EQ("0", ndd_prefix);

  ndd_prefix.clear();
  // Test case with non digit in the national prefix.
  phone_util_.GetNddPrefixForRegion(RegionCode::AO(), false, &ndd_prefix);
  EXPECT_EQ("0~0", ndd_prefix);

  ndd_prefix.clear();
  phone_util_.GetNddPrefixForRegion(RegionCode::AO(), true, &ndd_prefix);
  EXPECT_EQ("00", ndd_prefix);

  // Test cases with invalid regions.
  ndd_prefix.clear();
  phone_util_.GetNddPrefixForRegion(RegionCode::GetUnknown(), false,
                                    &ndd_prefix);
  EXPECT_EQ("", ndd_prefix);

  ndd_prefix.clear();
  phone_util_.GetNddPrefixForRegion(RegionCode::UN001(), false, &ndd_prefix);
  EXPECT_EQ("", ndd_prefix);

  // CS is already deprecated so the library doesn't support it.
  ndd_prefix.clear();
  phone_util_.GetNddPrefixForRegion(RegionCode::CS(), false, &ndd_prefix);
  EXPECT_EQ("", ndd_prefix);
}

TEST_F(PhoneNumberUtilTest, IsViablePhoneNumber) {
  EXPECT_FALSE(IsViablePhoneNumber("1"));
  // Only one or two digits before strange non-possible punctuation.
  EXPECT_FALSE(IsViablePhoneNumber("1+1+1"));
  EXPECT_FALSE(IsViablePhoneNumber("80+0"));
  // Two digits is viable.
  EXPECT_TRUE(IsViablePhoneNumber("00"));
  EXPECT_TRUE(IsViablePhoneNumber("111"));
  // Alpha numbers.
  EXPECT_TRUE(IsViablePhoneNumber("0800-4-pizza"));
  EXPECT_TRUE(IsViablePhoneNumber("0800-4-PIZZA"));
  // We need at least three digits before any alpha characters.
  EXPECT_FALSE(IsViablePhoneNumber("08-PIZZA"));
  EXPECT_FALSE(IsViablePhoneNumber("8-PIZZA"));
  EXPECT_FALSE(IsViablePhoneNumber("12. March"));
}

TEST_F(PhoneNumberUtilTest, IsViablePhoneNumberNonAscii) {
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
               /* "1　（800) ABC-DEF" */);
  static const string kExpectedFullwidthOutput =
      "1\xE3\x80\x80\xEF\xBC\x88" "800) 222-333" /* "1　（800) 222-333" */;
  phone_util_.ConvertAlphaCharactersInNumber(&input);
  EXPECT_EQ(kExpectedFullwidthOutput, input);
}

TEST_F(PhoneNumberUtilTest, NormaliseRemovePunctuation) {
  string input_number("034-56&+#2" "\xC2\xAD" "34");
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

TEST_F(PhoneNumberUtilTest, NormaliseStripNonDiallableCharacters) {
  string input_number("03*4-56&+1a#234");
  phone_util_.NormalizeDiallableCharsOnly(&input_number);
  static const string kExpectedOutput("03*456+1#234");
  EXPECT_EQ(kExpectedOutput, input_number)
      << "Conversion did not correctly remove non-diallable characters";
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
  phone_number.assign("+80012345678");
  stripped_number.assign("12345678");
  expected_country_code = 800;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            MaybeExtractCountryCode(metadata, true, &phone_number, &number));
  EXPECT_EQ(expected_country_code, number.country_code());
  EXPECT_EQ(PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN,
            number.country_code_source());
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
  ad_number.set_national_number(uint64{12345});
  phone_util_.Format(ad_number, PhoneNumberUtil::INTERNATIONAL,
                     &formatted_number);
  EXPECT_EQ("+376 12345", formatted_number);
  phone_util_.Format(ad_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+37612345", formatted_number);
  phone_util_.Format(ad_number, PhoneNumberUtil::NATIONAL, &formatted_number);
  EXPECT_EQ("12345", formatted_number);
  EXPECT_EQ(PhoneNumberUtil::UNKNOWN, phone_util_.GetNumberType(ad_number));
  EXPECT_FALSE(phone_util_.IsValidNumber(ad_number));

  // Test dialing a US number from within Andorra.
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(uint64{6502530000});
  phone_util_.FormatOutOfCountryCallingNumber(us_number, RegionCode::AD(),
                                              &formatted_number);
  EXPECT_EQ("00 1 650 253 0000", formatted_number);
}

TEST_F(PhoneNumberUtilTest, UnknownCountryCallingCode) {
  PhoneNumber invalid_number;
  invalid_number.set_country_code(kInvalidCountryCode);
  invalid_number.set_national_number(uint64{12345});

  EXPECT_FALSE(phone_util_.IsValidNumber(invalid_number));

  // It's not very well defined as to what the E164 representation for a number
  // with an invalid country calling code is, but just prefixing the country
  // code and national number is about the best we can do.
  string formatted_number;
  phone_util_.Format(invalid_number, PhoneNumberUtil::E164, &formatted_number);
  EXPECT_EQ("+212345", formatted_number);
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchMatches) {
  // Test simple matches where formatting is different, or leading zeros, or
  // country code has been specified.
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331 6005",
                                                    "+64 03 331 6005"));
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+800 1234 5678",
                                                    "+80012345678"));
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
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings(
                "+64 3 331-6005", "tel:+64-3-331-6005;isub=123"));
  // Test alpha numbers.
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+1800 siX-Flags",
                                                    "+1 800 7493 5247"));
  // Test numbers with extensions.
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005 extn 1234",
                                                    "+6433316005#1234"));
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005 extn 1234",
                                                    "+6433316005;1234"));
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings(
"+7 423 202-25-11 ext 100",
"+7 4232022511 \xd0\xb4\xd0\xbe\xd0\xb1. 100"));

  // Test proto buffers.
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(uint64{33316005});
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
  nz_number_2.set_national_number(uint64{33316005});
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatch(nz_number, nz_number_2));
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchShortMatchIfDiffNumLeadingZeros) {
  PhoneNumber nz_number_one;
  nz_number_one.set_country_code(64);
  nz_number_one.set_national_number(uint64{33316005});
  nz_number_one.set_italian_leading_zero(true);

  PhoneNumber nz_number_two;
  nz_number_two.set_country_code(64);
  nz_number_two.set_national_number(uint64{33316005});
  nz_number_two.set_italian_leading_zero(true);
  nz_number_two.set_number_of_leading_zeros(2);

  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatch(nz_number_one, nz_number_two));

  nz_number_one.set_italian_leading_zero(false);
  nz_number_one.set_number_of_leading_zeros(1);
  nz_number_two.set_italian_leading_zero(true);
  nz_number_two.set_number_of_leading_zeros(1);
  // Since one doesn't have the "italian_leading_zero" set to true, we ignore
  // the number of leading zeros present (1 is in any case the default value).
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatch(nz_number_one, nz_number_two));
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchAcceptsProtoDefaultsAsMatch) {
  PhoneNumber nz_number_one;
  nz_number_one.set_country_code(64);
  nz_number_one.set_national_number(uint64{33316005});
  nz_number_one.set_italian_leading_zero(true);

  PhoneNumber nz_number_two;
  nz_number_two.set_country_code(64);
  nz_number_two.set_national_number(uint64{33316005});
  nz_number_two.set_italian_leading_zero(true);
  // The default for number_of_leading_zeros is 1, so it shouldn't normally be
  // set, however if it is it should be considered equivalent.
  nz_number_two.set_number_of_leading_zeros(1);
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatch(nz_number_one, nz_number_two));
}

TEST_F(PhoneNumberUtilTest,
       IsNumberMatchMatchesDiffLeadingZerosIfItalianLeadingZeroFalse) {
  PhoneNumber nz_number_one;
  nz_number_one.set_country_code(64);
  nz_number_one.set_national_number(uint64{33316005});

  PhoneNumber nz_number_two;
  nz_number_two.set_country_code(64);
  nz_number_two.set_national_number(uint64{33316005});
  // The default for number_of_leading_zeros is 1, so it shouldn't normally be
  // set, however if it is it should be considered equivalent.
  nz_number_two.set_number_of_leading_zeros(1);
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatch(nz_number_one, nz_number_two));
  // Even if it is set to ten, it is still equivalent because in both cases
  // italian_leading_zero is not true.
  nz_number_two.set_number_of_leading_zeros(10);
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatch(nz_number_one, nz_number_two));
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchIgnoresSomeFields) {
  // Check raw_input, country_code_source and preferred_domestic_carrier_code
  // are ignored.
  PhoneNumber br_number_1;
  PhoneNumber br_number_2;
  br_number_1.set_country_code(55);
  br_number_1.set_national_number(uint64{3121286979});
  br_number_1.set_country_code_source(PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN);
  br_number_1.set_preferred_domestic_carrier_code("12");
  br_number_1.set_raw_input("012 3121286979");
  br_number_2.set_country_code(55);
  br_number_2.set_national_number(uint64{3121286979});
  br_number_2.set_country_code_source(PhoneNumber::FROM_DEFAULT_COUNTRY);
  br_number_2.set_preferred_domestic_carrier_code("14");
  br_number_2.set_raw_input("143121286979");
  EXPECT_EQ(PhoneNumberUtil::EXACT_MATCH,
            phone_util_.IsNumberMatch(br_number_1, br_number_2));
}

TEST_F(PhoneNumberUtilTest, IsNumberMatchNonMatches) {
  // NSN matches.
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("03 331 6005",
                                                    "03 331 6006"));
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+800 1234 5678",
                                                    "+1 800 1234 5678"));
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
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings(
                "+64 3 331-6005 extn 1234", "tel:+64-3-331-6005;ext=1235"));
  // NSN matches, but extension is different - not the same number.
  EXPECT_EQ(PhoneNumberUtil::NO_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("+64 3 331-6005 ext.1235",
                                                    "3 331 6005#1234"));
  // Invalid numbers that can't be parsed.
  EXPECT_EQ(PhoneNumberUtil::INVALID_NUMBER,
            phone_util_.IsNumberMatchWithTwoStrings("4", "3 331 6043"));
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
  EXPECT_EQ(PhoneNumberUtil::NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings(
                "+64 3 331-6005",
                "tel:03-331-6005;isub=1234;phone-context=abc.nz"));

  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(uint64{33316005});
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
  us_number.set_national_number(uint64{2345678901});
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
  random_number.set_national_number(uint64{2345678901});
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
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings(
                "+64 3 331-6005", "tel:331-6005;phone-context=abc.nz"));
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
              phone_util_.IsNumberMatchWithTwoStrings(
                  "+64 3 331-6005",
                  "tel:331-6005;isub=1234;phone-context=abc.nz"));
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
              phone_util_.IsNumberMatchWithTwoStrings(
                  "+64 3 331-6005",
                  "tel:331-6005;isub=1234;phone-context=abc.nz;a=%A1"));

  // We did not know that the "0" was a national prefix since neither number has
  // a country code, so this is considered a SHORT_NSN_MATCH.
  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
            phone_util_.IsNumberMatchWithTwoStrings("3 331-6005",
                                                    "03 331 6005"));

  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
              phone_util_.IsNumberMatchWithTwoStrings("3 331-6005",
                                                      "331 6005"));

  EXPECT_EQ(PhoneNumberUtil::SHORT_NSN_MATCH,
              phone_util_.IsNumberMatchWithTwoStrings(
                  "3 331-6005", "tel:331-6005;phone-context=abc.nz"));
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
  it_number_1.set_national_number(uint64{1234});
  it_number_1.set_italian_leading_zero(true);
  it_number_2.set_country_code(39);
  it_number_2.set_national_number(uint64{1234});
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
  nz_number.set_national_number(uint64{33316005});
  PhoneNumber test_number;
  // National prefix attached.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("033316005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // Some fields are not filled in by Parse, but only by ParseAndKeepRawInput.
  EXPECT_FALSE(nz_number.has_country_code_source());
  EXPECT_EQ(PhoneNumber::UNSPECIFIED, nz_number.country_code_source());

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
  // Test parsing RFC3966 format with a phone context.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:03-331-6005;phone-context=+64",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:331-6005;phone-context=+64-3",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:331-6005;phone-context=+64-3",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("My number is tel:03-331-6005;phone-context=+64",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // Test parsing RFC3966 format with optional user-defined parameters. The
  // parameters will appear after the context if present.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:03-331-6005;phone-context=+64;a=%A1",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // Test parsing RFC3966 with an ISDN subaddress.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:03-331-6005;isub=12345;phone-context=+64",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:+64-3-331-6005;isub=12345",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03-331-6005;phone-context=+64",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // Testing international prefixes.
  // Should strip country code.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0064 3 331 6005",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // Try again, but this time we have an international number with Region Code
  // US. It should recognise the country code and parse accordingly.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("01164 3 331 6005",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+64 3 331 6005",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  // We should ignore the leading plus here, since it is not followed by a valid
  // country code but instead is followed by the IDD for the US.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+01164 3 331 6005",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+0064 3 331 6005",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+ 00 64 3 331 6005",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  PhoneNumber us_local_number;
  us_local_number.set_country_code(1);
  us_local_number.set_national_number(uint64{2530000});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:253-0000;phone-context=www.google.com",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(us_local_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "tel:253-0000;isub=12345;phone-context=www.google.com",
                RegionCode::US(), &test_number));
  EXPECT_EQ(us_local_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:2530000;isub=12345;phone-context=1234.com",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(us_local_number, test_number);

  // Test for http://b/issue?id=2247493
  nz_number.Clear();
  nz_number.set_country_code(64);
  nz_number.set_national_number(uint64{64123456});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+64(0)64123456",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  // Check that using a "/" is fine in a phone number.
  PhoneNumber de_number;
  de_number.set_country_code(49);
  de_number.set_national_number(uint64{12345678});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("123/45678", RegionCode::DE(), &test_number));
  EXPECT_EQ(de_number, test_number);

  PhoneNumber us_number;
  us_number.set_country_code(1);
  // Check it doesn't use the '1' as a country code when parsing if the phone
  // number was already possible.
  us_number.set_national_number(uint64{1234567890});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("123-456-7890", RegionCode::US(), &test_number));
  EXPECT_EQ(us_number, test_number);

  // Test star numbers. Although this is not strictly valid, we would like to
  // make sure we can parse the output we produce when formatting the number.
  PhoneNumber star_number;
  star_number.set_country_code(81);
  star_number.set_national_number(uint64{2345});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+81 *2345", RegionCode::JP(), &test_number));
  EXPECT_EQ(star_number, test_number);

  PhoneNumber short_number;
  short_number.set_country_code(64);
  short_number.set_national_number(uint64{12});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("12", RegionCode::NZ(), &test_number));
  EXPECT_EQ(short_number, test_number);

  // Test for short-code with leading zero for a country which has 0 as
  // national prefix. Ensure it's not interpreted as national prefix if the
  // remaining number length is local-only in terms of length. Example: In GB,
  // length 6-7 are only possible local-only.
  short_number.set_country_code(44);
  short_number.set_national_number(123456);
  short_number.set_italian_leading_zero(true);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0123456", RegionCode::GB(), &test_number));
  EXPECT_EQ(short_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseNumberWithAlphaCharacters) {
  // Test case with alpha characters.
  PhoneNumber test_number;
  PhoneNumber tollfree_number;
  tollfree_number.set_country_code(64);
  tollfree_number.set_national_number(uint64{800332005});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0800 DDA 005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(tollfree_number, test_number);

  PhoneNumber premium_number;
  premium_number.set_country_code(64);
  premium_number.set_national_number(uint64{9003326005});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 DDA 6005", RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);

  // Not enough alpha characters for them to be considered intentional, so they
  // are stripped.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 332 6005a",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 332 600a5",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 332 600A5",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0900 a332 600A5",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(premium_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseWithInternationalPrefixes) {
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(uint64{6503336000});
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+1 (650) 333-6000",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(us_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+1-650-333-6000",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(us_number, test_number);

  // Calling the US number from Singapore by using different service providers
  // 1st test: calling using SingTel IDD service (IDD is 001)
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0011-650-333-6000",
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // 2nd test: calling using StarHub IDD service (IDD is 008)
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0081-650-333-6000",
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // 3rd test: calling using SingTel V019 service (IDD is 019)
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0191-650-333-6000",
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // Calling the US number from Poland
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0~01-650-333-6000",
                              RegionCode::PL(), &test_number));
  EXPECT_EQ(us_number, test_number);

  // Using "++" at the start.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("++1 (650) 333-6000",
                              RegionCode::PL(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // Using a full-width plus sign.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("\xEF\xBC\x8B" "1 (650) 333-6000",
                              /* "＋1 (650) 333-6000" */
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // Using a soft hyphen U+00AD.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("1 (650) 333" "\xC2\xAD" "-6000",
                              /* "1 (650) 333­-6000­" */
                              RegionCode::US(), &test_number));
  EXPECT_EQ(us_number, test_number);
  // The whole number, including punctuation, is here represented in full-width
  // form.
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
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("\xEF\xBC\x8B\xEF\xBC\x91\xE3\x80\x80\xEF\xBC\x88"
                              "\xEF\xBC\x96\xEF\xBC\x95\xEF\xBC\x90\xEF\xBC\x89"
                              "\xE3\x80\x80\xEF\xBC\x93\xEF\xBC\x93\xEF\xBC\x93"
                              "\xE3\x83\xBC\xEF\xBC\x96\xEF\xBC\x90\xEF\xBC\x90"
                              "\xEF\xBC\x90",
                              /* "＋１　（６５０）　３３３ー６０００" */
                              RegionCode::SG(), &test_number));
  EXPECT_EQ(us_number, test_number);

  PhoneNumber toll_free_number;
  toll_free_number.set_country_code(800);
  toll_free_number.set_national_number(uint64{12345678});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("011 800 1234 5678",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(toll_free_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseWithLeadingZero) {
  PhoneNumber it_number;
  it_number.set_country_code(39);
  it_number.set_national_number(uint64{236618300});
  it_number.set_italian_leading_zero(true);
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+39 02-36618 300",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(it_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("02-36618 300", RegionCode::IT(), &test_number));
  EXPECT_EQ(it_number, test_number);

  it_number.Clear();
  it_number.set_country_code(39);
  it_number.set_national_number(uint64{312345678});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("312 345 678", RegionCode::IT(), &test_number));
  EXPECT_EQ(it_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseNationalNumberArgentina) {
  // Test parsing mobile numbers of Argentina.
  PhoneNumber ar_number;
  ar_number.set_country_code(54);
  ar_number.set_national_number(uint64{93435551212});
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 9 343 555 1212", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0343 15 555 1212", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);

  ar_number.set_national_number(uint64{93715654320});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 9 3715 65 4320", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03715 15 65 4320", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);

  // Test parsing fixed-line numbers of Argentina.
  ar_number.set_national_number(uint64{1137970000});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 11 3797 0000", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("011 3797 0000", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);

  ar_number.set_national_number(uint64{3715654321});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 3715 65 4321", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03715 65 4321", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);

  ar_number.set_national_number(uint64{2312340000});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+54 23 1234 0000", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("023 1234 0000", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseWithXInNumber) {
  // Test that having an 'x' in the phone number at the start is ok and that it
  // just gets removed.
  PhoneNumber ar_number;
  ar_number.set_country_code(54);
  ar_number.set_national_number(uint64{123456789});
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0123456789", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(0) 123456789", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0 123456789", RegionCode::AR(), &test_number));
  EXPECT_EQ(ar_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(0xx) 123456789", RegionCode::AR(),
                              &test_number));
  EXPECT_EQ(ar_number, test_number);

  PhoneNumber ar_from_us;
  ar_from_us.set_country_code(54);
  ar_from_us.set_national_number(uint64{81429712});
  // This test is intentionally constructed such that the number of digit after
  // xx is larger than 7, so that the number won't be mistakenly treated as an
  // extension, as we allow extensions up to 7 digits. This assumption is okay
  // for now as all the countries where a carrier selection code is written in
  // the form of xx have a national significant number of length larger than 7.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("011xx5481429712", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(ar_from_us, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseNumbersMexico) {
  // Test parsing fixed-line numbers of Mexico.
  PhoneNumber mx_number;

  mx_number.set_country_code(52);
  mx_number.set_national_number(uint64{4499780001});
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+52 (449)978-0001", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("01 (449)978-0001", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(449)978-0001", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);

  // Test parsing mobile numbers of Mexico.
  mx_number.Clear();
  mx_number.set_country_code(52);
  mx_number.set_national_number(uint64{13312345678});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+52 1 33 1234-5678", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("044 (33) 1234-5678", RegionCode::MX(),
                              &test_number));
  EXPECT_EQ(mx_number, test_number);
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

  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("1 Still not a number", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("1 MICROSOFT", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("12 MICROSOFT", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::TOO_LONG_NSN,
            phone_util_.Parse("01495 72553301873 810104", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("+---", RegionCode::DE(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("+***", RegionCode::DE(),
                              &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("+*******91", RegionCode::DE(),
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

  // RFC3966 phone-context is a website.
  EXPECT_EQ(PhoneNumberUtil::INVALID_COUNTRY_CODE_ERROR,
            phone_util_.Parse("tel:555-1234;phone-context=www.google.com",
                              RegionCode::ZZ(), &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);
  // This is invalid because no "+" sign is present as part of phone-context.
  // This should not succeed in being parsed.
  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("tel:555-1234;phone-context=1-331",
                              RegionCode::ZZ(), &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);

  // Only the phone-context symbol is present, but no data.
  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse(";phone-context=",
                              RegionCode::ZZ(), &test_number));
  EXPECT_EQ(PhoneNumber::default_instance(), test_number);
}

TEST_F(PhoneNumberUtilTest, ParseNumbersWithPlusWithNoRegion) {
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(uint64{33316005});
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

  PhoneNumber toll_free_number;
  toll_free_number.set_country_code(800);
  toll_free_number.set_national_number(uint64{12345678});
  result_proto.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+800 1234 5678",
                              RegionCode::GetUnknown(), &result_proto));
  EXPECT_EQ(toll_free_number, result_proto);

  PhoneNumber universal_premium_rate;
  universal_premium_rate.set_country_code(979);
  universal_premium_rate.set_national_number(uint64{123456789});
  result_proto.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+979 123 456 789",
                              RegionCode::GetUnknown(), &result_proto));
  EXPECT_EQ(universal_premium_rate, result_proto);

  result_proto.Clear();
  // Test parsing RFC3966 format with a phone context.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:03-331-6005;phone-context=+64",
                              RegionCode::GetUnknown(), &result_proto));
  EXPECT_EQ(nz_number, result_proto);
  result_proto.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("  tel:03-331-6005;phone-context=+64",
                              RegionCode::GetUnknown(), &result_proto));
  EXPECT_EQ(nz_number, result_proto);
  result_proto.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:03-331-6005;isub=12345;phone-context=+64",
                              RegionCode::GetUnknown(), &result_proto));
  EXPECT_EQ(nz_number, result_proto);

  nz_number.set_raw_input("+64 3 331 6005");
  nz_number.set_country_code_source(PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN);
  result_proto.Clear();
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("+64 3 331 6005",
                                             RegionCode::GetUnknown(),
                                             &result_proto));
  EXPECT_EQ(nz_number, result_proto);
}

TEST_F(PhoneNumberUtilTest, ParseNumberTooShortIfNationalPrefixStripped) {
  PhoneNumber test_number;

  // Test that a number whose first digits happen to coincide with the national
  // prefix does not get them stripped if doing so would result in a number too
  // short to be a possible (regular length) phone number for that region.
  PhoneNumber by_number;
  by_number.set_country_code(375);
  by_number.set_national_number(8123L);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("8123", RegionCode::BY(),
                              &test_number));
  EXPECT_EQ(by_number, test_number);
  by_number.set_national_number(81234L);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("81234", RegionCode::BY(),
                              &test_number));
  EXPECT_EQ(by_number, test_number);

  // The prefix doesn't get stripped, since the input is a viable 6-digit
  // number, whereas the result of stripping is only 5 digits.
  by_number.set_national_number(812345L);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("812345", RegionCode::BY(),
                              &test_number));
  EXPECT_EQ(by_number, test_number);

  // The prefix gets stripped, since only 6-digit numbers are possible.
  by_number.set_national_number(123456L);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("8123456", RegionCode::BY(),
                              &test_number));
  EXPECT_EQ(by_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseExtensions) {
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(uint64{33316005});
  nz_number.set_extension("3456");
  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 331 6005 ext 3456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 331 6005x3456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03-331 6005 int.3456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 331 6005 #3456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);

  // Test the following do not extract extensions:
  PhoneNumber non_extn_number;
  non_extn_number.set_country_code(1);
  non_extn_number.set_national_number(uint64{80074935247});
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("1800 six-flags", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(non_extn_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("1800 SIX-FLAGS", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(non_extn_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0~0 1800 7493 5247", RegionCode::PL(),
                              &test_number));
  EXPECT_EQ(non_extn_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(1800) 7493.5247", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(non_extn_number, test_number);

  // Check that the last instance of an extension token is matched.
  PhoneNumber extn_number;
  extn_number.set_country_code(1);
  extn_number.set_national_number(uint64{80074935247});
  extn_number.set_extension("1234");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0~0 1800 7493 5247 ~1234", RegionCode::PL(),
                              &test_number));
  EXPECT_EQ(extn_number, test_number);

  // Verifying bug-fix where the last digit of a number was previously omitted
  // if it was a 0 when extracting the extension. Also verifying a few different
  // cases of extensions.
  PhoneNumber uk_number;
  uk_number.set_country_code(44);
  uk_number.set_national_number(uint64{2034567890});
  uk_number.set_extension("456");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890x456", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890x456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 x456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 X456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 X 456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 X   456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890 x 456  ", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44 2034567890  X 456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44-2034567890;ext=456", RegionCode::GB(),
                              &test_number));
  EXPECT_EQ(uk_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:2034567890;ext=456;phone-context=+44",
                              RegionCode::ZZ(), &test_number));
  EXPECT_EQ(uk_number, test_number);

  // Full-width extension, "extn" only.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "+442034567890\xEF\xBD\x85\xEF\xBD\x98\xEF\xBD\x94\xEF\xBD\x8E"
                "456", RegionCode::GB(), &test_number));
  EXPECT_EQ(uk_number, test_number);
  // "xtn" only.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "+44-2034567890\xEF\xBD\x98\xEF\xBD\x94\xEF\xBD\x8E""456",
                RegionCode::GB(), &test_number));
  EXPECT_EQ(uk_number, test_number);
  // "xt" only.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+44-2034567890\xEF\xBD\x98\xEF\xBD\x94""456",
                              RegionCode::GB(), &test_number));
  EXPECT_EQ(uk_number, test_number);

  PhoneNumber us_with_extension;
  us_with_extension.set_country_code(1);
  us_with_extension.set_national_number(uint64{8009013355});
  us_with_extension.set_extension("7246433");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 x 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 , ext 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ; 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  // To test an extension character without surrounding spaces.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355;7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ,extension 7246433",
                              RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ,extensi\xC3\xB3n 7246433",
                              /* "(800) 901-3355 ,extensión 7246433" */
                              RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  // Repeat with the small letter o with acute accent created by combining
  // characters.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ,extensio\xCC\x81n 7246433",
                              /* "(800) 901-3355 ,extensión 7246433" */
                              RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 , 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(800) 901-3355 ext: 7246433", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
  // Testing Russian extension "доб" with variants found onli
  PhoneNumber ru_with_extension;
  ru_with_extension.set_country_code(7);
  ru_with_extension.set_national_number(4232022511L);
  ru_with_extension.set_extension("100");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "8 (423) 202-25-11, \xd0\xb4\xd0\xbe\xd0\xb1. 100",
                RegionCode::RU(), &test_number));
  EXPECT_EQ(ru_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "8 (423) 202-25-11 \xd0\xb4\xd0\xbe\xd0\xb1. 100",
                RegionCode::RU(), &test_number));
  EXPECT_EQ(ru_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "8 (423) 202-25-11, \xd0\xb4\xd0\xbe\xd0\xb1 100",
                RegionCode::RU(), &test_number));
  EXPECT_EQ(ru_with_extension, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "8 (423) 202-25-11 \xd0\xb4\xd0\xbe\xd0\xb1 100",
                RegionCode::RU(), &test_number));
  EXPECT_EQ(ru_with_extension, test_number);
  // We are suppose to test input without spaces before and after this extension
  // character. As hex escape sequence becomes out of range, postfixed a space
  // to extension character here.
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "8 (423) 202-25-11\xd0\xb4\xd0\xbe\xd0\xb1 100",
                RegionCode::RU(), &test_number));
  // In upper case
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse(
                "8 (423) 202-25-11 \xd0\x94\xd0\x9e\xd0\x91 100",
                RegionCode::RU(), &test_number));
  EXPECT_EQ(ru_with_extension, test_number);

  // Test that if a number has two extensions specified, we ignore the second.
  PhoneNumber us_with_two_extensions_number;
  us_with_two_extensions_number.set_country_code(1);
  us_with_two_extensions_number.set_national_number(uint64{2121231234});
  us_with_two_extensions_number.set_extension("508");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(212)123-1234 x508/x1234", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_two_extensions_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(212)123-1234 x508/ x1234", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_two_extensions_number, test_number);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("(212)123-1234 x508\\x1234", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_two_extensions_number, test_number);

  // Test parsing numbers in the form (645) 123-1234-910# works, where the last
  // 3 digits before the # are an extension.
  us_with_extension.Clear();
  us_with_extension.set_country_code(1);
  us_with_extension.set_national_number(uint64{6451231234});
  us_with_extension.set_extension("910");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+1 (645) 123 1234-910#", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_with_extension, test_number);
}

TEST_F(PhoneNumberUtilTest, TestParseHandlesLongExtensionsWithExplicitLabels) {
  // Test lower and upper limits of extension lengths for each type of label.
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(33316005ULL);
  PhoneNumber test_number;

  // Firstly, when in RFC format: ext_limit_after_explicit_label
  nz_number.set_extension("0");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:+6433316005;ext=0", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);

  nz_number.set_extension("01234567890123456789");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:+6433316005;ext=01234567890123456789",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  // Extension too long.
  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("tel:+6433316005;ext=012345678901234567890",
                              RegionCode::NZ(), &test_number));

  // Explicit extension label: ext_limit_after_explicit_label
  nz_number.set_extension("1");
  EXPECT_EQ(
      PhoneNumberUtil::NO_PARSING_ERROR,
      phone_util_.Parse("03 3316005ext:1", RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  nz_number.set_extension("12345678901234567890");
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 xtn:12345678901234567890",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 extension\t12345678901234567890",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 xtensio:12345678901234567890",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 xtensión, 12345678901234567890#",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005extension.12345678901234567890",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 доб:12345678901234567890",
                              RegionCode::NZ(), &test_number));
  EXPECT_EQ(nz_number, test_number);

  // Extension too long.
  EXPECT_EQ(PhoneNumberUtil::TOO_LONG_NSN,
            phone_util_.Parse("03 3316005 extension 123456789012345678901",
                              RegionCode::NZ(), &test_number));
}

TEST_F(PhoneNumberUtilTest,
       TestParseHandlesLongExtensionsWithAutoDiallingLabels) {
  // Secondly, cases of auto-dialling and other standard extension labels:
  // ext_limit_after_likely_label
  PhoneNumber us_number_user_input;
  us_number_user_input.set_country_code(1);
  us_number_user_input.set_national_number(2679000000ULL);
  PhoneNumber test_number;
  us_number_user_input.set_extension("123456789012345");

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+12679000000,,123456789012345#",
                              RegionCode::US(), &test_number));
  EXPECT_EQ(us_number_user_input, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+12679000000;123456789012345#", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_number_user_input, test_number);

  PhoneNumber uk_number_user_input;
  uk_number_user_input.set_country_code(44);
  uk_number_user_input.set_national_number(2034000000ULL);
  uk_number_user_input.set_extension("123456789");

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+442034000000,,123456789#", RegionCode::GB(),
                              &test_number));

  // Extension too long.
  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("+12679000000,,1234567890123456#",
                              RegionCode::US(), &test_number));
}

TEST_F(PhoneNumberUtilTest, TestParseHandlesShortExtensionsWithAmbiguousChar) {
  // Thirdly, for single and non-standard cases: ext_limit_after_ambiguous_char
  PhoneNumber nz_number;
  nz_number.set_country_code(64);
  nz_number.set_national_number(33316005ULL);
  PhoneNumber test_number;
  nz_number.set_extension("123456789");// 

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 x 123456789", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 x. 123456789", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 #123456789#", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("03 3316005 ~ 123456789", RegionCode::NZ(),
                              &test_number));
  EXPECT_EQ(nz_number, test_number);

  EXPECT_EQ(PhoneNumberUtil::TOO_LONG_NSN,
            phone_util_.Parse("03 3316005 ~ 1234567890", RegionCode::NZ(),
                              &test_number));
}

TEST_F(PhoneNumberUtilTest, TestParseHandlesShortExtensionsWhenNotSureOfLabel) {
  // Thirdly, when no explicit extension label present, but denoted by
  // tailing #: ext_limit_when_not_sure
  PhoneNumber us_number;
  us_number.set_country_code(1);
  us_number.set_national_number(1234567890ULL);
  PhoneNumber test_number;
  us_number.set_extension("666666");

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("+1123-456-7890 666666#", RegionCode::US(),
                              &test_number));
  EXPECT_EQ(us_number, test_number);

  us_number.set_extension("6");
  EXPECT_EQ(
      PhoneNumberUtil::NO_PARSING_ERROR,
      phone_util_.Parse("+11234567890-6#", RegionCode::US(), &test_number));
  EXPECT_EQ(us_number, test_number);

  // Extension too long.
  EXPECT_EQ(PhoneNumberUtil::NOT_A_NUMBER,
            phone_util_.Parse("+1123-456-7890 7777777#", RegionCode::US(),
                              &test_number));
}

TEST_F(PhoneNumberUtilTest, ParseAndKeepRaw) {
  PhoneNumber alpha_numeric_number;
  alpha_numeric_number.set_country_code(1);
  alpha_numeric_number.set_national_number(uint64{80074935247});
  alpha_numeric_number.set_raw_input("800 six-flags");
  alpha_numeric_number.set_country_code_source(
      PhoneNumber::FROM_DEFAULT_COUNTRY);

  PhoneNumber test_number;
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.ParseAndKeepRawInput("800 six-flags", RegionCode::US(),
                                             &test_number));
  EXPECT_EQ(alpha_numeric_number, test_number);

  alpha_numeric_number.set_national_number(uint64{8007493524});
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

  // Try with invalid region - expect failure. We clear the test number first
  // because if parsing isn't successful, the number parsed in won't be changed.
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

TEST_F(PhoneNumberUtilTest, ParseItalianLeadingZeros) {
  PhoneNumber zeros_number;
  zeros_number.set_country_code(61);
  PhoneNumber test_number;

  // Test the number "011".
  zeros_number.set_national_number(11L);
  zeros_number.set_italian_leading_zero(true);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("011", RegionCode::AU(),
                              &test_number));
  EXPECT_EQ(zeros_number, test_number);

  // Test the number "001".
  zeros_number.set_national_number(1L);
  zeros_number.set_italian_leading_zero(true);
  zeros_number.set_number_of_leading_zeros(2);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("001", RegionCode::AU(),
                              &test_number));
  EXPECT_EQ(zeros_number, test_number);

  // Test the number "000". This number has 2 leading zeros.
  zeros_number.set_national_number(0L);
  zeros_number.set_italian_leading_zero(true);
  zeros_number.set_number_of_leading_zeros(2);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("000", RegionCode::AU(),
                              &test_number));
  EXPECT_EQ(zeros_number, test_number);

  // Test the number "0000". This number has 3 leading zeros.
  zeros_number.set_national_number(0L);
  zeros_number.set_italian_leading_zero(true);
  zeros_number.set_number_of_leading_zeros(3);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("0000", RegionCode::AU(),
                              &test_number));
  EXPECT_EQ(zeros_number, test_number);
}

TEST_F(PhoneNumberUtilTest, ParseWithPhoneContext) {
  PhoneNumber expected_number;
  expected_number.set_country_code(64);
  expected_number.set_national_number(33316005L);
  PhoneNumber actual_number;

  // context    = ";phone-context=" descriptor
  // descriptor = domainname / global-number-digits

  // Valid global-phone-digits
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=+64",
                              RegionCode::ZZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=+64;{this isn't "
                              "part of phone-context anymore!}",
                              RegionCode::ZZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  expected_number.set_national_number(3033316005L);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=+64-3",
                              RegionCode::ZZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  expected_number.set_country_code(55);
  expected_number.set_national_number(5033316005L);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=+(555)",
                              RegionCode::ZZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  expected_number.set_country_code(1);
  expected_number.set_national_number(23033316005L);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=+-1-2.3()",
                              RegionCode::ZZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  // Valid domainname
  expected_number.set_country_code(64);
  expected_number.set_national_number(33316005L);
  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=abc.nz",
                              RegionCode::NZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  EXPECT_EQ(
      PhoneNumberUtil::NO_PARSING_ERROR,
      phone_util_.Parse("tel:033316005;phone-context=www.PHONE-numb3r.com",
                        RegionCode::NZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=a", RegionCode::NZ(),
                              &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=3phone.J.",
                              RegionCode::NZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);
  actual_number.Clear();

  EXPECT_EQ(PhoneNumberUtil::NO_PARSING_ERROR,
            phone_util_.Parse("tel:033316005;phone-context=a--z",
                              RegionCode::NZ(), &actual_number));
  EXPECT_EQ(expected_number, actual_number);

  // Invalid descriptor
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=");
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=+");
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=64");
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=++64");
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=+abc");
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=.");
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=3phone");
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=a-.nz");
  AssertThrowsForInvalidPhoneContext("tel:033316005;phone-context=a{b}c");
}

TEST_F(PhoneNumberUtilTest, CanBeInternationallyDialled) {
  PhoneNumber test_number;
  test_number.set_country_code(1);

  // We have no-international-dialling rules for the US in our test metadata
  // that say that toll-free numbers cannot be dialled internationally.
  test_number.set_national_number(uint64{8002530000});
  EXPECT_FALSE(phone_util_.CanBeInternationallyDialled(test_number));

  // Normal US numbers can be internationally dialled.
  test_number.set_national_number(uint64{6502530000});
  EXPECT_TRUE(phone_util_.CanBeInternationallyDialled(test_number));

  // Invalid number.
  test_number.set_national_number(uint64{2530000});
  EXPECT_TRUE(phone_util_.CanBeInternationallyDialled(test_number));

  // We have no data for NZ - should return true.
  test_number.set_country_code(64);
  test_number.set_national_number(uint64{33316005});
  EXPECT_TRUE(phone_util_.CanBeInternationallyDialled(test_number));

  test_number.set_country_code(800);
  test_number.set_national_number(uint64{12345678});
  EXPECT_TRUE(phone_util_.CanBeInternationallyDialled(test_number));
}

TEST_F(PhoneNumberUtilTest, IsAlphaNumber) {
  EXPECT_TRUE(phone_util_.IsAlphaNumber("1800 six-flags"));
  EXPECT_TRUE(phone_util_.IsAlphaNumber("1800 six-flags ext. 1234"));
  EXPECT_TRUE(phone_util_.IsAlphaNumber("+800 six-flags"));
  EXPECT_TRUE(phone_util_.IsAlphaNumber("180 six-flags"));
  EXPECT_FALSE(phone_util_.IsAlphaNumber("1800 123-1234"));
  EXPECT_FALSE(phone_util_.IsAlphaNumber("1 six-flags"));
  EXPECT_FALSE(phone_util_.IsAlphaNumber("18 six-flags"));
  EXPECT_FALSE(phone_util_.IsAlphaNumber("1800 123-1234 extension: 1234"));
  EXPECT_FALSE(phone_util_.IsAlphaNumber("+800 1234-1234"));
}

}  // namespace phonenumbers
}  // namespace i18n
