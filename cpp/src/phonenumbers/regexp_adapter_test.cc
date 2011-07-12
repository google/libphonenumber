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

#include "phonenumbers/regexp_adapter.h"

#include <string>
#include <vector>

#include <gtest/gtest.h>

#include "base/basictypes.h"
#include "base/scoped_ptr.h"
#include "stl_util.h"
#include "stringutil.h"

#ifdef USE_RE2
#include "phonenumbers/regexp_adapter_re2.h"
#else
#include "phonenumbers/regexp_adapter_icu.h"
#endif  // USE_RE2

namespace i18n {
namespace phonenumbers {

using std::string;
using std::vector;

// Structure that contains the attributes used to test an implementation of the
// regexp adapter.
struct RegExpTestContext {
  explicit RegExpTestContext(const string& name,
                             const AbstractRegExpFactory* factory)
      : name(name),
        factory(factory),
        digits(factory->CreateRegExp("\\d+")),
        parentheses_digits(factory->CreateRegExp("\\((\\d+)\\)")),
        single_digit(factory->CreateRegExp("\\d")),
        two_digit_groups(factory->CreateRegExp("(\\d+)-(\\d+)")) {}

  const string name;
  const scoped_ptr<const AbstractRegExpFactory> factory;
  const scoped_ptr<const RegExp> digits;
  const scoped_ptr<const RegExp> parentheses_digits;
  const scoped_ptr<const RegExp> single_digit;
  const scoped_ptr<const RegExp> two_digit_groups;
};

class RegExpAdapterTest : public testing::Test {
 protected:
  RegExpAdapterTest() {
#ifdef USE_RE2
    contexts_.push_back(
        new RegExpTestContext("RE2", new RE2RegExpFactory()));
#else
    contexts_.push_back(
        new RegExpTestContext("ICU Regex", new ICURegExpFactory()));
#endif  // USE_RE2
  }

  ~RegExpAdapterTest() {
    STLDeleteElements(&contexts_);
  }

  static string ErrorMessage(const RegExpTestContext& context) {
    return StrCat("Test failed with ", context.name, " implementation.");
  }

  typedef vector<const RegExpTestContext*>::const_iterator TestContextIterator;
  vector<const RegExpTestContext*> contexts_;
};

TEST_F(RegExpAdapterTest, TestConsumeNoMatch) {
  for (vector<const RegExpTestContext*>::const_iterator it = contexts_.begin();
       it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;
    const scoped_ptr<RegExpInput> input(
        context.factory->CreateInput("+1-123-456-789"));

    // When 'true' is passed to Consume(), the match occurs from the beginning
    // of the input.
    ASSERT_FALSE(context.digits->Consume(input.get(), true, NULL, NULL, NULL))
        << ErrorMessage(context);
    ASSERT_EQ("+1-123-456-789", input->ToString()) << ErrorMessage(context);

    string res1;
    ASSERT_FALSE(context.parentheses_digits->Consume(
        input.get(), true, &res1, NULL, NULL)) << ErrorMessage(context);
    ASSERT_EQ("+1-123-456-789", input->ToString()) << ErrorMessage(context);
    ASSERT_EQ("", res1) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestConsumeWithNull) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;
    const AbstractRegExpFactory& factory = *context.factory;
    const scoped_ptr<RegExpInput> input(factory.CreateInput("+123"));
    const scoped_ptr<const RegExp> plus_sign(factory.CreateRegExp("(\\+)"));

    ASSERT_TRUE(plus_sign->Consume(input.get(), true, NULL, NULL, NULL))
        << ErrorMessage(context);
    ASSERT_EQ("123", input->ToString()) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestConsumeRetainsMatches) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;
    const scoped_ptr<RegExpInput> input(
        context.factory->CreateInput("1-123-456-789"));

    string res1, res2;
    ASSERT_TRUE(context.two_digit_groups->Consume(
        input.get(), true, &res1, &res2, NULL)) << ErrorMessage(context);
    ASSERT_EQ("-456-789", input->ToString()) << ErrorMessage(context);
    ASSERT_EQ("1", res1) << ErrorMessage(context);
    ASSERT_EQ("123", res2) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestFindAndConsume) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;
    const scoped_ptr<RegExpInput> input(
        context.factory->CreateInput("+1-123-456-789"));

    // When 'false' is passed to Consume(), the match can occur from any place
    // in the input.
    ASSERT_TRUE(context.digits->Consume(input.get(), false, NULL, NULL, NULL))
        << ErrorMessage(context);
    ASSERT_EQ("-123-456-789", input->ToString()) << ErrorMessage(context);

    ASSERT_TRUE(context.digits->Consume(input.get(), false, NULL, NULL, NULL))
        << ErrorMessage(context);
    ASSERT_EQ("-456-789", input->ToString()) << ErrorMessage(context);

    ASSERT_FALSE(context.parentheses_digits->Consume(
        input.get(), false, NULL, NULL, NULL)) << ErrorMessage(context);
    ASSERT_EQ("-456-789", input->ToString()) << ErrorMessage(context);

    string res1, res2;
    ASSERT_TRUE(context.two_digit_groups->Consume(
        input.get(), false, &res1, &res2, NULL)) << ErrorMessage(context);
    ASSERT_EQ("", input->ToString()) << ErrorMessage(context);
    ASSERT_EQ("456", res1) << ErrorMessage(context);
    ASSERT_EQ("789", res2) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestPartialMatch) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;
    const AbstractRegExpFactory& factory = *context.factory;
    const scoped_ptr<const RegExp> reg_exp(factory.CreateRegExp("([\\da-z]+)"));
    string matched;

    EXPECT_TRUE(reg_exp->PartialMatch("12345af", &matched))
        << ErrorMessage(context);
    EXPECT_EQ("12345af", matched) << ErrorMessage(context);

    EXPECT_TRUE(reg_exp->PartialMatch("12345af", NULL))
        << ErrorMessage(context);

    EXPECT_TRUE(reg_exp->PartialMatch("[12]", &matched))
        << ErrorMessage(context);
    EXPECT_EQ("12", matched) << ErrorMessage(context);

    matched.clear();
    EXPECT_FALSE(reg_exp->PartialMatch("[]", &matched))
        << ErrorMessage(context);
    EXPECT_EQ("", matched) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestFullMatch) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;
    const AbstractRegExpFactory& factory = *context.factory;
    const scoped_ptr<const RegExp> reg_exp(factory.CreateRegExp("([\\da-z]+)"));
    string matched;

    EXPECT_TRUE(reg_exp->FullMatch("12345af", &matched))
        << ErrorMessage(context);
    EXPECT_EQ("12345af", matched) << ErrorMessage(context);

    EXPECT_TRUE(reg_exp->FullMatch("12345af", NULL)) << ErrorMessage(context);

    matched.clear();
    EXPECT_FALSE(reg_exp->FullMatch("[12]", &matched)) << ErrorMessage(context);
    EXPECT_EQ("", matched) << ErrorMessage(context);

    matched.clear();
    EXPECT_FALSE(reg_exp->FullMatch("[]", &matched)) << ErrorMessage(context);
    EXPECT_EQ("", matched) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestReplace) {
  for (vector<const RegExpTestContext*>::const_iterator it = contexts_.begin();
       it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;
    string input("123-4567 ");

    ASSERT_TRUE(context.single_digit->Replace(&input, "+"))
        << ErrorMessage(context);
    ASSERT_EQ("+23-4567 ", input) << ErrorMessage(context);

    ASSERT_TRUE(context.single_digit->Replace(&input, "+"))
        << ErrorMessage(context);
    ASSERT_EQ("++3-4567 ", input) << ErrorMessage(context);

    const scoped_ptr<const RegExp> single_letter(
        context.factory->CreateRegExp("[a-z]"));
    ASSERT_FALSE(single_letter->Replace(&input, "+")) << ErrorMessage(context);
    ASSERT_EQ("++3-4567 ", input) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestReplaceWithGroup) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;

    // Make sure referencing groups in the regexp in the replacement string
    // works. $[0-9] notation is used.
    string input = "123-4567 abc";
    ASSERT_TRUE(context.two_digit_groups->Replace(&input, "$2"))
        << ErrorMessage(context);
    ASSERT_EQ("4567 abc", input) << ErrorMessage(context);

    input = "123-4567";
    ASSERT_TRUE(context.two_digit_groups->Replace(&input, "$1"))
        << ErrorMessage(context);
    ASSERT_EQ("123", input) << ErrorMessage(context);

    input = "123-4567";
    ASSERT_TRUE(context.two_digit_groups->Replace(&input, "$2"))
        << ErrorMessage(context);
    ASSERT_EQ("4567", input) << ErrorMessage(context);

    input = "123-4567";
    ASSERT_TRUE(context.two_digit_groups->Replace(&input, "$1 $2"))
        << ErrorMessage(context);
    ASSERT_EQ("123 4567", input) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestReplaceWithDollarSign) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;

    // Make sure '$' can be used in the replacement string when escaped.
    string input = "123-4567";
    ASSERT_TRUE(context.two_digit_groups->Replace(&input, "\\$1 \\$2"))
        << ErrorMessage(context);

    ASSERT_EQ("$1 $2", input) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestGlobalReplace) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;

    string input("123-4567 ");

    ASSERT_TRUE(context.single_digit->GlobalReplace(&input, "*"))
        << ErrorMessage(context);
    ASSERT_EQ("***-**** ", input) << ErrorMessage(context);

    ASSERT_FALSE(context.single_digit->GlobalReplace(&input, "*"))
        << ErrorMessage(context);
    ASSERT_EQ("***-**** ", input) << ErrorMessage(context);
  }
}

TEST_F(RegExpAdapterTest, TestUtf8) {
  for (TestContextIterator it = contexts_.begin(); it != contexts_.end();
       ++it) {
    const RegExpTestContext& context = **it;
    const AbstractRegExpFactory& factory = *context.factory;

    const scoped_ptr<const RegExp> reg_exp(factory.CreateRegExp(
        "\xE2\x84\xA1\xE2\x8A\x8F([\xCE\xB1-\xCF\x89]*)\xE2\x8A\x90"
        /* "℡⊏([α-ω]*)⊐" */));
    string matched;

    EXPECT_FALSE(reg_exp->Match(
        "\xE2\x84\xA1\xE2\x8A\x8F" "123\xE2\x8A\x90" /* "℡⊏123⊐" */, true,
        &matched)) << ErrorMessage(context);
    EXPECT_TRUE(reg_exp->Match(
        "\xE2\x84\xA1\xE2\x8A\x8F\xCE\xB1\xCE\xB2\xE2\x8A\x90"
        /* "℡⊏αβ⊐" */, true, &matched)) << ErrorMessage(context);

    EXPECT_EQ("\xCE\xB1\xCE\xB2" /* "αβ" */, matched) << ErrorMessage(context);
  }
}

}  // namespace phonenumbers
}  // namespace i18n
