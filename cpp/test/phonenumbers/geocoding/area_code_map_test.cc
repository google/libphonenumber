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

#include "phonenumbers/phonenumber.pb.h"

namespace i18n {
namespace phonenumbers {

using std::map;
using std::string;
using std::vector;

namespace {

void MakeCodeMap(const map<int, string>& m, scoped_ptr<AreaCodeMap>* code_map) {
  scoped_ptr<AreaCodeMap> cm(new AreaCodeMap());
  cm->ReadAreaCodeMap(m);
  code_map->swap(cm);
}

void MakeCodeMapUS(scoped_ptr<AreaCodeMap>* code_map) {
  map<int, string> m;
  m[1212] = "New York";
  m[1480] = "Arizona";
  m[1650] = "California";
  m[1907] = "Alaska";
  m[1201664] = "Westwood, NJ";
  m[1480893] = "Phoenix, AZ";
  m[1501372] = "Little Rock, AR";
  m[1626308] = "Alhambra, CA";
  m[1650345] = "San Mateo, CA";
  m[1867993] = "Dawson, YT";
  m[1972480] = "Richardson, TX";
  MakeCodeMap(m, code_map);
}

void MakeCodeMapIT(scoped_ptr<AreaCodeMap>* code_map) {
  map<int, string> m;
  m[3902] = "Milan";
  m[3906] = "Rome";
  m[39010] = "Genoa";
  m[390131] = "Alessandria";
  m[390321] = "Novara";
  m[390975] = "Potenza";
  MakeCodeMap(m, code_map);
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
  EXPECT_EQ("New York", *map_US_->Lookup(MakePhoneNumber(1, 2121234567L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberNJ) {
  EXPECT_EQ("Westwood, NJ", *map_US_->Lookup(MakePhoneNumber(1, 2016641234L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberNY) {
  EXPECT_EQ("New York", *map_US_->Lookup(MakePhoneNumber(1, 2126641234L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberCA1) {
  EXPECT_EQ("San Mateo, CA", *map_US_->Lookup(MakePhoneNumber(1, 6503451234L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberCA2) {
  EXPECT_EQ("California", *map_US_->Lookup(MakePhoneNumber(1, 6502531234L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberTX) {
  EXPECT_EQ("Richardson, TX",
            *map_US_->Lookup(MakePhoneNumber(1, 9724801234L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberNotFoundTX) {
  EXPECT_EQ(NULL, map_US_->Lookup(MakePhoneNumber(1, 9724811234L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberCH) {
  EXPECT_EQ(NULL, map_US_->Lookup(MakePhoneNumber(41, 446681300L)));
}

TEST_F(AreaCodeMapTest, TestLookupNumberIT) {
  PhoneNumber number = MakePhoneNumber(39, 212345678L);
  number.set_italian_leading_zero(true);
  EXPECT_EQ("Milan", *map_IT_->Lookup(number));

  number.set_national_number(612345678L);
  EXPECT_EQ("Rome", *map_IT_->Lookup(number));

  number.set_national_number(3211234L);
  EXPECT_EQ("Novara", *map_IT_->Lookup(number));

  // A mobile number
  number.set_national_number(321123456L);
  number.set_italian_leading_zero(false);
  EXPECT_EQ(NULL, map_IT_->Lookup(number));

  // An invalid number (too short)
  number.set_national_number(321123L);
  number.set_italian_leading_zero(true);
  EXPECT_EQ("Novara", *map_IT_->Lookup(number));
}

}  // namespace phonenumbers
}  // namespace i18n
