// Copyright (C) 2012 The Libphonenumber Authors
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
// Author: Patrick Mezard

#include "phonenumbers/geocoding/phonenumber_offline_geocoder.h"

#include <gtest/gtest.h>
#include <unicode/locid.h>

#include "phonenumbers/geocoding/geocoding_test_data.h"
#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumber.pb.h"

namespace i18n {
namespace phonenumbers {

using icu::Locale;

namespace {

PhoneNumber MakeNumber(int32 country_code, uint64 national_number) {
  PhoneNumber n;
  n.set_country_code(country_code);
  n.set_national_number(national_number);
  return n;
}

const Locale kEnglishLocale = Locale("en", "GB");
const Locale kFrenchLocale = Locale("fr", "FR");
const Locale kGermanLocale = Locale("de", "DE");
const Locale kItalianLocale = Locale("it", "IT");
const Locale kKoreanLocale = Locale("ko", "KR");
const Locale kSimplifiedChineseLocale = Locale("zh", "CN");

}  // namespace

class PhoneNumberOfflineGeocoderTest : public testing::Test {
 protected:
  PhoneNumberOfflineGeocoderTest() :
    KO_NUMBER1(MakeNumber(82, 22123456UL)),
    KO_NUMBER2(MakeNumber(82, 322123456UL)),
    KO_NUMBER3(MakeNumber(82, uint64{6421234567})),
    KO_INVALID_NUMBER(MakeNumber(82, 1234UL)),
    KO_MOBILE(MakeNumber(82, uint64{101234567})),
    US_NUMBER1(MakeNumber(1, uint64{6502530000})),
    US_NUMBER2(MakeNumber(1, uint64{6509600000})),
    US_NUMBER3(MakeNumber(1, 2128120000UL)),
    US_NUMBER4(MakeNumber(1, uint64{6174240000})),
    US_INVALID_NUMBER(MakeNumber(1, 123456789UL)),
    BS_NUMBER1(MakeNumber(1, 2423651234UL)),
    AU_NUMBER(MakeNumber(61, 236618300UL)),
    NUMBER_WITH_INVALID_COUNTRY_CODE(MakeNumber(999, 2423651234UL)),
    INTERNATIONAL_TOLL_FREE(MakeNumber(800, 12345678UL)) {
  }

  virtual void SetUp() {
    geocoder_.reset(
        new PhoneNumberOfflineGeocoder(
            get_test_country_calling_codes(),
            get_test_country_calling_codes_size(),
            get_test_country_languages,
            get_test_prefix_language_code_pairs(),
            get_test_prefix_language_code_pairs_size(),
            get_test_prefix_descriptions));
  }

 protected:
  scoped_ptr<PhoneNumberOfflineGeocoder> geocoder_;

  const PhoneNumber KO_NUMBER1;
  const PhoneNumber KO_NUMBER2;
  const PhoneNumber KO_NUMBER3;
  const PhoneNumber KO_INVALID_NUMBER;
  const PhoneNumber KO_MOBILE;

  const PhoneNumber US_NUMBER1;
  const PhoneNumber US_NUMBER2;
  const PhoneNumber US_NUMBER3;
  const PhoneNumber US_NUMBER4;
  const PhoneNumber US_INVALID_NUMBER;

  const PhoneNumber BS_NUMBER1;
  const PhoneNumber AU_NUMBER;
  const PhoneNumber NUMBER_WITH_INVALID_COUNTRY_CODE;
  const PhoneNumber INTERNATIONAL_TOLL_FREE;
};

TEST_F(PhoneNumberOfflineGeocoderTest,
       TestGetDescriptionForNumberWithNoDataFile) {
  // No data file containing mappings for US numbers is available in Chinese for
  // the unittests. As a result, the country name of United States in simplified
  // Chinese is returned.

  // "\u7F8E\u56FD" (unicode escape sequences are not always supported)
  EXPECT_EQ("\xe7""\xbe""\x8e""\xe5""\x9b""\xbd",
            geocoder_->GetDescriptionForNumber(US_NUMBER1,
                                               kSimplifiedChineseLocale));
  EXPECT_EQ("Bahamas",
            geocoder_->GetDescriptionForNumber(BS_NUMBER1, Locale("en", "US")));
  EXPECT_EQ("Australia",
            geocoder_->GetDescriptionForNumber(AU_NUMBER, Locale("en", "US")));
  EXPECT_EQ("",
            geocoder_->GetDescriptionForNumber(NUMBER_WITH_INVALID_COUNTRY_CODE,
                                               Locale("en", "US")));
  EXPECT_EQ("",
            geocoder_->GetDescriptionForNumber(INTERNATIONAL_TOLL_FREE,
                                               Locale("en", "US")));
}

TEST_F(PhoneNumberOfflineGeocoderTest,
       TestGetDescriptionForNumberWithMissingPrefix) {
  // Test that the name of the country is returned when the number passed in is
  // valid but not covered by the geocoding data file.
  EXPECT_EQ("United States",
            geocoder_->GetDescriptionForNumber(US_NUMBER4, Locale("en", "US")));
}

TEST_F(PhoneNumberOfflineGeocoderTest, TestGetDescriptionForNumber_en_US) {
  EXPECT_EQ("CA",
            geocoder_->GetDescriptionForNumber(US_NUMBER1, Locale("en", "US")));
  EXPECT_EQ("Mountain View, CA",
            geocoder_->GetDescriptionForNumber(US_NUMBER2, Locale("en", "US")));
  EXPECT_EQ("New York, NY",
            geocoder_->GetDescriptionForNumber(US_NUMBER3, Locale("en", "US")));
}

TEST_F(PhoneNumberOfflineGeocoderTest, TestGetDescriptionForKoreanNumber) {
  EXPECT_EQ("Seoul",
            geocoder_->GetDescriptionForNumber(KO_NUMBER1, kEnglishLocale));
  EXPECT_EQ("Incheon",
            geocoder_->GetDescriptionForNumber(KO_NUMBER2, kEnglishLocale));
  EXPECT_EQ("Jeju",
            geocoder_->GetDescriptionForNumber(KO_NUMBER3, kEnglishLocale));
  // "\uC11C\uC6B8"
  EXPECT_EQ("\xec""\x84""\x9c""\xec""\x9a""\xb8",
            geocoder_->GetDescriptionForNumber(KO_NUMBER1, kKoreanLocale));
  // "\uC778\uCC9C"
  EXPECT_EQ("\xec""\x9d""\xb8""\xec""\xb2""\x9c",
            geocoder_->GetDescriptionForNumber(KO_NUMBER2, kKoreanLocale));
}

TEST_F(PhoneNumberOfflineGeocoderTest, TestGetDescriptionForFallBack) {
  // No fallback, as the location name for the given phone number is available
  // in the requested language.
  EXPECT_EQ("Kalifornien",
            geocoder_->GetDescriptionForNumber(US_NUMBER1, kGermanLocale));
  // German falls back to English.
  EXPECT_EQ("New York, NY",
            geocoder_->GetDescriptionForNumber(US_NUMBER3, kGermanLocale));
  // Italian falls back to English.
  EXPECT_EQ("CA",
            geocoder_->GetDescriptionForNumber(US_NUMBER1, kItalianLocale));
  // Korean doesn't fall back to English.
  // "\uB300\uD55C\uBBFC\uAD6D"
  EXPECT_EQ("\xeb""\x8c""\x80""\xed""\x95""\x9c""\xeb""\xaf""\xbc""\xea""\xb5"
            "\xad",
            geocoder_->GetDescriptionForNumber(KO_NUMBER3, kKoreanLocale));
}

TEST_F(PhoneNumberOfflineGeocoderTest,
       TestGetDescriptionForNumberWithUserRegion) {
  // User in Italy, American number. We should just show United States, in
  // Spanish, and not more detailed information.
  EXPECT_EQ("Estados Unidos",
            geocoder_->GetDescriptionForNumber(US_NUMBER1, Locale("es", "ES"),
                                               "IT"));
  // Unknown region - should just show country name.
  EXPECT_EQ("Estados Unidos",
            geocoder_->GetDescriptionForNumber(US_NUMBER1, Locale("es", "ES"),
                                               "ZZ"));
  // User in the States, language German, should show detailed data.
  EXPECT_EQ("Kalifornien",
            geocoder_->GetDescriptionForNumber(US_NUMBER1, kGermanLocale,
                                               "US"));
  // User in the States, language French, no data for French, so we fallback to
  // English detailed data.
  EXPECT_EQ("CA",
            geocoder_->GetDescriptionForNumber(US_NUMBER1, kFrenchLocale,
                                               "US"));
  // Invalid number - return an empty string.
  EXPECT_EQ("",
            geocoder_->GetDescriptionForNumber(US_INVALID_NUMBER,
                                               kEnglishLocale,
                                               "US"));
}

TEST_F(PhoneNumberOfflineGeocoderTest, TestGetDescriptionForInvalidNumber) {
  EXPECT_EQ("", geocoder_->GetDescriptionForNumber(KO_INVALID_NUMBER,
                                                   kEnglishLocale));
  EXPECT_EQ("", geocoder_->GetDescriptionForNumber(US_INVALID_NUMBER,
                                                   kEnglishLocale));
}

TEST_F(PhoneNumberOfflineGeocoderTest,
       TestGetDescriptionForNonGeographicalNumberWithGeocodingPrefix) {
  // We have a geocoding prefix, but we shouldn't use it since this is not
  // geographical.
  EXPECT_EQ("South Korea",
            geocoder_->GetDescriptionForNumber(KO_MOBILE, kEnglishLocale));
}
}  // namespace phonenumbers
}  // namespace i18n
