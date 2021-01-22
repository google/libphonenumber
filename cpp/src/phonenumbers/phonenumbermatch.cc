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
//
// Author: Tao Huang
//
// Implementation of a mutable match of a phone number within a piece of
// text. Matches may be found using PhoneNumberUtil::FindNumbers.

#include "phonenumbers/phonenumbermatch.h"

#include <string>

#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/stringutil.h"

namespace i18n {
namespace phonenumbers {

PhoneNumberMatch::PhoneNumberMatch(int start,
                                   const string& raw_string,
                                   const PhoneNumber& number)
    : start_(start), raw_string_(raw_string), number_(number) {
}

PhoneNumberMatch::PhoneNumberMatch()
    : start_(-1), raw_string_(""), number_(PhoneNumber::default_instance()) {
}

const PhoneNumber& PhoneNumberMatch::number() const {
  return number_;
}

int PhoneNumberMatch::start() const {
  return start_;
}

int PhoneNumberMatch::end() const {
  return static_cast<int>(start_ + raw_string_.length());
}

int PhoneNumberMatch::length() const {
  return static_cast<int>(raw_string_.length());
}

const string& PhoneNumberMatch::raw_string() const {
  return raw_string_;
}

void PhoneNumberMatch::set_start(int start) {
  start_ = start;
}

void PhoneNumberMatch::set_raw_string(const string& raw_string) {
  raw_string_ = raw_string;
}

void PhoneNumberMatch::set_number(const PhoneNumber& number) {
  number_.CopyFrom(number);
}

string PhoneNumberMatch::ToString() const {
  return StrCat("PhoneNumberMatch [", start(), ",", end(), ") ",
                raw_string_.c_str());
}

bool PhoneNumberMatch::Equals(const PhoneNumberMatch& match) const {
  return ExactlySameAs(match.number_, number_) &&
      match.raw_string_.compare(raw_string_) == 0 &&
      match.start_ == start_;
}

void PhoneNumberMatch::CopyFrom(const PhoneNumberMatch& match) {
  raw_string_ = match.raw_string();
  start_ = match.start();
  number_ = match.number();
}

}  // namespace phonenumbers
}  // namespace i18n
