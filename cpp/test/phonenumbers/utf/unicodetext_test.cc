// Copyright (C) 2011 The Libphonenumber Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License. You may obtain
// a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
//
// Author: Ben Gertzfield

#include <gtest/gtest.h>

#include "phonenumbers/utf/unicodetext.h"

namespace i18n {
namespace phonenumbers {

TEST(UnicodeTextTest, Iterator) {
  struct value {
    const char* utf8;
    char32 code_point;
  } values[] = {
    { "\x31", 0x31 }, // U+0031 DIGIT ONE
    { "\xC2\xBD", 0x00BD }, // U+00BD VULGAR FRACTION ONE HALF
    { "\xEF\xBC\x91", 0xFF11 }, // U+FF11 FULLWIDTH DIGIT ONE
    { "\xF0\x9F\x80\x80", 0x1F000 }, // U+1F000 MAHJONG TILE EAST WIND
  };

  for (size_t i = 0; i < sizeof values / sizeof values[0]; i++) {
    string number(values[i].utf8);
    UnicodeText number_as_unicode;
    number_as_unicode.PointToUTF8(number.data(), number.size());
    EXPECT_TRUE(number_as_unicode.UTF8WasValid());
    UnicodeText::const_iterator it = number_as_unicode.begin();
    EXPECT_EQ(values[i].code_point, *it);
  }
}

} // namespace phonenumbers
} // namespace i18n
