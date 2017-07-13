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
//
// Basic test cases for MappingFileProvider.

#include "phonenumbers/geocoding/area_code_map.h"

#include <cstddef>
#include <vector>

#include <gtest/gtest.h>  // NOLINT(build/include_order)

#include "phonenumbers/geocoding/geocoding_data.h"
#include "phonenumbers/phonenumber.pb.h"

namespace i18n {
namespace phonenumbers {

namespace {

void MakeCodeMap(const PrefixDescriptions* descriptions,
                 scoped_ptr<AreaCodeMap>* code_map) {
  scoped_ptr<AreaCodeMap> cm(new AreaCodeMap());
  cm->ReadAreaCodeMap(descriptions);
  code_map->swap(cm);
}

const int32 prefix_1_us_prefixes[] = {
  1212,
  1480,
  1650,
  1907,
  1201664,
  1480893,
  1501372,
  1626308,
  1650345,
  1867993,
  1972480,
};

const char* prefix_1_us_descriptions[] = {
  "New York",
  "Arizona",
  "California",
  "Alaska",
  "Westwood, NJ",
  "Phoenix, AZ",
  "Little Rock, AR",
  "Alhambra, CA",
  "San Mateo, CA",
  "Dawson, YT",
  "Richardson, TX",
};

const int32 prefix_1_us_lengths[] = {
  4, 7,
};

const PrefixDescriptions prefix_1_us = {
  prefix_1_us_prefixes,
  sizeof(prefix_1_us_prefixes) / sizeof(*prefix_1_us_prefixes),
  prefix_1_us_descriptions,
  prefix_1_us_lengths,
  sizeof(prefix_1_us_lengths) / sizeof(*prefix_1_us_lengths),
};

const int32 prefix_39_it_prefixes[] = {
  3902,
  3906,
  39010,
  390131,
  390321,
  390975,
};

const char* prefix_39_it_descriptions[] = {
  "Milan",
  "Rome",
  "Genoa",
  "Alessandria",
  "Novara",
  "Potenza",
};

const int32 prefix_39_it_lengths[] = {
  4, 5, 6,
};

const PrefixDescriptions prefix_39_it = {
  prefix_39_it_prefixes,
  sizeof(prefix_39_it_prefixes) / sizeof(*prefix_39_it_prefixes),
  prefix_39_it_descriptions,
  prefix_39_it_lengths,
  sizeof(prefix_39_it_lengths) / sizeof(*prefix_1_us_lengths),
};

void MakeCodeMapUS(scoped_ptr<AreaCodeMap>* code_map) {
  MakeCodeMap(&prefix_1_us, code_map);
}

void MakeCodeMapIT(scoped_ptr<AreaCodeMap>* code_map) {
  MakeCodeMap(&prefix_39_it, code_map);
}

PhoneNumber MakePhoneNumber(int32 country_code, uint64 national_number) {
  PhoneNumber number;
  number.set_country_code(country_code);
  number.set_national_number(national_number);
  return number;
}

}  // namespace

class AreaCodeMapTest : public testing::Test {
 protected:
  virtual void SetUp() {
    MakeCodeMapUS(&map_US_);
    MakeCodeMapIT(&map_IT_);
  }

  scoped_ptr<AreaCodeMap> map_US_;
  scoped_ptr<AreaCodeMap> map_IT_;
};

TEST_F(AreaCodeMapTest, TestLookupInvalidNumberUS) {
  EXPECT_STREQ("New York", map_US_->Lookup(MakePhoneNumber(1, 2121234567L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberNJ) {
  EXPECT_STREQ("Westwood, NJ",
               map_US_->Lookup(MakePhoneNumber(1, 2016641234L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberNY) {
  EXPECT_STREQ("New York", map_US_->Lookup(MakePhoneNumber(1, 2126641234L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberCA1) {
  EXPECT_STREQ("San Mateo, CA",
               map_US_->Lookup(MakePhoneNumber(1, 6503451234LL)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberCA2) {
  EXPECT_STREQ("California", map_US_->Lookup(MakePhoneNumber(1, 6502531234LL)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberTX) {
  EXPECT_STREQ("Richardson, TX",
            map_US_->Lookup(MakePhoneNumber(1, 9724801234LL)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberNotFoundTX) {
  EXPECT_STREQ(NULL, map_US_->Lookup(MakePhoneNumber(1, 9724811234LL)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberCH) {
  EXPECT_STREQ(NULL, map_US_->Lookup(MakePhoneNumber(41, 446681300L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberIT) {
  PhoneNumber number = MakePhoneNumber(39, 212345678L);
  number.set_italian_leading_zero(true);
  EXPECT_STREQ("Milan", map_IT_->Lookup(number));

  number.set_national_number(612345678L);
  EXPECT_STREQ("Rome", map_IT_->Lookup(number));

  number.set_national_number(3211234L);
  EXPECT_STREQ("Novara", map_IT_->Lookup(number));

  // A mobile number
  number.set_national_number(321123456L);
  number.set_italian_leading_zero(false);
  EXPECT_STREQ(NULL, map_IT_->Lookup(number));

  // An invalid number (too short)
  number.set_national_number(321123L);
  number.set_italian_leading_zero(true);
  EXPECT_STREQ("Novara", map_IT_->Lookup(number));
}

}  // namespace phonenumbers
}  // namespace i18n
