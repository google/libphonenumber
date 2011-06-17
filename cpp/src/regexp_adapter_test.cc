// Copyright (C) 2011 Google Inc.
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

// Author: George Yakovlev
//         Philippe Liard

#include "regexp_adapter.h"

#include <string>

#include <gtest/gtest.h>

#include "base/scoped_ptr.h"

namespace i18n {
namespace phonenumbers {

using std::string;

class RegExpAdapterTest : public testing::Test {
 protected:
  RegExpAdapterTest()
      : digits_(RegExp::Create("\\d+")),
        parentheses_digits_(RegExp::Create("\\((\\d+)\\)")),
        single_digit_(RegExp::Create("\\d")),
        two_digit_groups_(RegExp::Create("(\\d+)-(\\d+)")) {}

  const scoped_ptr<const RegExp> digits_;
  const scoped_ptr<const RegExp> parentheses_digits_;
  const scoped_ptr<const RegExp> single_digit_;
  const scoped_ptr<const RegExp> two_digit_groups_;
};

TEST_F(RegExpAdapterTest, TestConsumeNoMatch) {
  const scoped_ptr<RegExpInput> input(RegExpInput::Create("+1-123-456-789"));

  // When 'true' is passed to Consume(), the match occurs from the beginning of
  // the input.
  ASSERT_FALSE(digits_->Consume(input.get(), true, NULL, NULL, NULL));
  ASSERT_EQ("+1-123-456-789", input->ToString());

  string res1;
  ASSERT_FALSE(parentheses_digits_->Consume(
      input.get(), true, &res1, NULL, NULL));
  ASSERT_EQ("+1-123-456-789", input->ToString());
  ASSERT_EQ("", res1);
}

TEST_F(RegExpAdapterTest, TestConsumeWithNull) {
  const scoped_ptr<RegExpInput> input(RegExpInput::Create("+123"));
  const scoped_ptr<const RegExp> plus_sign(RegExp::Create("(\\+)"));

  ASSERT_TRUE(plus_sign->Consume(input.get(), true, NULL, NULL, NULL));
  ASSERT_EQ("123", input->ToString());
}

TEST_F(RegExpAdapterTest, TestConsumeRetainsMatches) {
  const scoped_ptr<RegExpInput> input(RegExpInput::Create("1-123-456-789"));

  string res1, res2;
  ASSERT_TRUE(two_digit_groups_->Consume(
      input.get(), true, &res1, &res2, NULL));
  ASSERT_EQ("-456-789", input->ToString());
  ASSERT_EQ("1", res1);
  ASSERT_EQ("123", res2);
}

TEST_F(RegExpAdapterTest, TestFindAndConsume) {
  const scoped_ptr<RegExpInput> input(RegExpInput::Create("+1-123-456-789"));

  // When 'false' is passed to Consume(), the match can occur from any place in
  // the input.
  ASSERT_TRUE(digits_->Consume(input.get(), false, NULL, NULL, NULL));
  ASSERT_EQ("-123-456-789", input->ToString());

  ASSERT_TRUE(digits_->Consume(input.get(), false, NULL, NULL, NULL));
  ASSERT_EQ("-456-789", input->ToString());

  ASSERT_FALSE(parentheses_digits_->Consume(
      input.get(), false, NULL, NULL, NULL));
  ASSERT_EQ("-456-789", input->ToString());

  string res1, res2;
  ASSERT_TRUE(two_digit_groups_->Consume(
      input.get(), false, &res1, &res2, NULL));
  ASSERT_EQ("", input->ToString());
  ASSERT_EQ("456", res1);
  ASSERT_EQ("789", res2);
}

TEST(RegExpAdapter, TestPartialMatch) {
  const scoped_ptr<const RegExp> reg_exp(RegExp::Create("([\\da-z]+)"));
  string matched;

  EXPECT_TRUE(reg_exp->PartialMatch("12345af", &matched));
  EXPECT_EQ("12345af", matched);

  EXPECT_TRUE(reg_exp->PartialMatch("12345af", NULL));

  EXPECT_TRUE(reg_exp->PartialMatch("[12]", &matched));
  EXPECT_EQ("12", matched);

  matched.clear();
  EXPECT_FALSE(reg_exp->PartialMatch("[]", &matched));
  EXPECT_EQ("", matched);
}

TEST(RegExpAdapter, TestFullMatch) {
  const scoped_ptr<const RegExp> reg_exp(RegExp::Create("([\\da-z]+)"));
  string matched;

  EXPECT_TRUE(reg_exp->FullMatch("12345af", &matched));
  EXPECT_EQ("12345af", matched);

  EXPECT_TRUE(reg_exp->FullMatch("12345af", NULL));

  matched.clear();
  EXPECT_FALSE(reg_exp->FullMatch("[12]", &matched));
  EXPECT_EQ("", matched);

  matched.clear();
  EXPECT_FALSE(reg_exp->FullMatch("[]", &matched));
  EXPECT_EQ("", matched);
}

TEST_F(RegExpAdapterTest, TestReplace) {
  string input("123-4567 ");

  ASSERT_TRUE(single_digit_->Replace(&input, "+"));
  ASSERT_EQ("+23-4567 ", input);

  ASSERT_TRUE(single_digit_->Replace(&input, "+"));
  ASSERT_EQ("++3-4567 ", input);

  const scoped_ptr<const RegExp> single_letter(RegExp::Create("[a-z]"));
  ASSERT_FALSE(single_letter->Replace(&input, "+"));
  ASSERT_EQ("++3-4567 ", input);
}

TEST_F(RegExpAdapterTest, TestReplaceWithGroup) {
  // Make sure referencing groups in the regexp in the replacement string works.
  // $[0-9] notation is used.
  string input = "123-4567 abc";
  ASSERT_TRUE(two_digit_groups_->Replace(&input, "$2"));
  ASSERT_EQ("4567 abc", input);

  input = "123-4567";
  ASSERT_TRUE(two_digit_groups_->Replace(&input, "$1"));
  ASSERT_EQ("123", input);

  input = "123-4567";
  ASSERT_TRUE(two_digit_groups_->Replace(&input, "$2"));
  ASSERT_EQ("4567", input);

  input = "123-4567";
  ASSERT_TRUE(two_digit_groups_->Replace(&input, "$1 $2"));
  ASSERT_EQ("123 4567", input);
}

TEST_F(RegExpAdapterTest, TestReplaceWithDollarSign) {
  // Make sure '$' can be used in the replacement string when escaped.
  string input = "123-4567";
  ASSERT_TRUE(two_digit_groups_->Replace(&input, "\\$1 \\$2"));
  ASSERT_EQ("$1 $2", input);
}

TEST_F(RegExpAdapterTest, TestGlobalReplace) {
  string input("123-4567 ");

  ASSERT_TRUE(single_digit_->GlobalReplace(&input, "*"));
  ASSERT_EQ("***-**** ", input);

  ASSERT_FALSE(single_digit_->GlobalReplace(&input, "*"));
  ASSERT_EQ("***-**** ", input);
}

TEST(RegExpAdapter, TestUtf8) {
  const scoped_ptr<const RegExp> reg_exp(RegExp::Create("℡⊏([α-ω]*)⊐"));
  string matched;

  EXPECT_FALSE(reg_exp->Match("℡⊏123⊐", true, &matched));
  EXPECT_TRUE(reg_exp->Match("℡⊏αβ⊐", true, &matched));
  EXPECT_EQ("αβ", matched);
}

}  // namespace phonenumbers
}  // namespace i18n
