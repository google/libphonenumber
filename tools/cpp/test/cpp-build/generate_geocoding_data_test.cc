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

#include "cpp-build/generate_geocoding_data.h"

#include <gtest/gtest.h>

namespace i18n {
namespace phonenumbers {

TEST(GenerateGeocodingDataTest, TestMakeStringLiteral) {
  EXPECT_EQ("\"\"", MakeStringLiteral(""));
  EXPECT_EQ("\"Op\"\"\\xc3\"\"\\xa9\"\"ra\"",
            MakeStringLiteral("Op\xc3\xa9ra"));
}

TEST(GenerateGeocodingDataTest, TestReplaceAll) {
  EXPECT_EQ("", ReplaceAll("", "$input$", "cc"));
  EXPECT_EQ("accb", ReplaceAll("a$input$b", "$input$", "cc"));
  EXPECT_EQ("ab", ReplaceAll("a$input$b", "$input$", ""));
  EXPECT_EQ("ab", ReplaceAll("ab", "", "cc"));
  EXPECT_EQ("acdc", ReplaceAll("a$input$d$input$", "$input$", "c"));
}

}  // namespace phonenumbers
}  // namespace i18n
