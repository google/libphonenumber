// Copyright (C) 2025 The Libphonenumber Authors
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

#include "phonenumbers/phonecontextparser.h"

#include <gtest/gtest.h>

#include "phonenumbers/phonenumbernormalizer.h"
#include "phonenumbers/regexpsandmappings.h"

namespace i18n {
namespace phonenumbers {
using testing::Eq;

class PhoneContextParserTest : public testing::Test {
 public:
  // This type is neither copyable nor movable.
  PhoneContextParserTest(const PhoneContextParserTest&) = delete;
  PhoneContextParserTest& operator=(const PhoneContextParserTest&) = delete;

 protected:
  PhoneContextParserTest()
      : country_calling_codes_(
            std::make_unique<std::vector<int>>(std::vector<int>{64})),
        reg_exps_(new PhoneNumberRegExpsAndMappings()),
        normalizer_(new PhoneNumberNormalizer(reg_exps_)),
        context_parser_(new PhoneContextParser(
            std::move(country_calling_codes_), reg_exps_, normalizer_)) {}

  std::unique_ptr<std::vector<int>> country_calling_codes_;
  std::shared_ptr<PhoneNumberRegExpsAndMappings> reg_exps_;
  std::shared_ptr<PhoneNumberNormalizer> normalizer_;
  std::unique_ptr<PhoneContextParser> context_parser_;

  absl::StatusOr<std::optional<PhoneContextParser::PhoneContext>> Parse(
      absl::string_view phone_number) {
    return context_parser_->Parse(phone_number);
  }
};

TEST_F(PhoneContextParserTest, ParsePhoneContext) {
  auto parse_result = Parse("tel:03-331-6005;phone-context=+64");
  ASSERT_TRUE(parse_result.ok());
  ASSERT_TRUE(parse_result->has_value());
  EXPECT_EQ("+64", parse_result.value()->raw_context);
  EXPECT_EQ(64, parse_result.value()->country_code);

  auto parse_result = Parse("tel:03-331-6005;phone-context=example.com");
  ASSERT_TRUE(parse_result.ok());
  ASSERT_TRUE(parse_result->has_value());
  EXPECT_EQ("example.com", parse_result.value()->raw_context);
  EXPECT_EQ(std::nullopt, parse_result.value()->country_code);

  auto parse_result = Parse("03-331-6005;phone-context=+64;");
  ASSERT_TRUE(parse_result.ok());
  ASSERT_TRUE(parse_result->has_value());
  EXPECT_EQ("+64", parse_result.value()->raw_context);
  EXPECT_EQ(64, parse_result.value()->country_code);

  auto parse_result = Parse("+64-3-331-6005;phone-context=+64;");
  ASSERT_TRUE(parse_result.ok());
  ASSERT_TRUE(parse_result->has_value());
  EXPECT_EQ("+64", parse_result.value()->raw_context);
  EXPECT_EQ(64, parse_result.value()->country_code);

  auto parse_result =
      Parse("tel:03-331-6005;foo=bar;phone-context=+64;baz=qux");
  ASSERT_TRUE(parse_result.ok());
  ASSERT_TRUE(parse_result->has_value());
  EXPECT_EQ("+64", parse_result.value()->raw_context);
  EXPECT_EQ(64, parse_result.value()->country_code);

  auto parse_result = Parse("tel:03-331-6005");
  ASSERT_TRUE(parse_result.ok());
  ASSERT_EQ(std::nullopt, parse_result);

  auto parse_result = Parse("tel:03-331-6005;phone-context=+0");
  ASSERT_TRUE(parse_result.ok());
  ASSERT_TRUE(parse_result->has_value());
  EXPECT_EQ("+0", parse_result.value()->raw_context);
  EXPECT_EQ(std::nullopt, parse_result.value()->country_code);

  auto parse_result = Parse("tel:03-331-6005;phone-context=+1234");
  ASSERT_TRUE(parse_result.ok());
  ASSERT_TRUE(parse_result->has_value());
  EXPECT_EQ("+1234", parse_result.value()->raw_context);
  EXPECT_EQ(std::nullopt, parse_result.value()->country_code);
}

TEST_F(PhoneContextParserTest, ParsePhoneContextInvalid) {
  auto parse_result = Parse("tel:03-331-6005;phone-context=");
  EXPECT_EQ(absl::StatusCode::kInvalidArgument, parse_result.status().code());

  auto parse_result = Parse("tel:03-331-6005;phone-context=;");
  EXPECT_EQ(absl::StatusCode::kInvalidArgument, parse_result.status().code());

  auto parse_result = Parse("tel:03-331-6005;phone-context=0");
  EXPECT_EQ(absl::StatusCode::kInvalidArgument, parse_result.status().code());
}

}  // namespace phonenumbers
}  // namespace i18n
