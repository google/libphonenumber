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

#include "phonenumbers/phonenumbermatcher.h"

#include <string>
#include <vector>

#include <gtest/gtest.h>
#include <unicode/unistr.h>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/base/memory/singleton.h"
#include "phonenumbers/default_logger.h"
#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/phonenumbermatch.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/stringutil.h"
#include "phonenumbers/test_util.h"

namespace i18n {
namespace phonenumbers {

using std::string;
using icu::UnicodeString;

namespace {
// Small class that holds the context of the number we are testing against. The
// test will insert the phone number to be found between leading_text and
// trailing_text.
struct NumberContext {
  string leading_text_;
  string trailing_text_;
  NumberContext(const string& leading_text, const string& trailing_text)
    : leading_text_(leading_text),
      trailing_text_(trailing_text) {
  }
};

// Small class that holds the number we want to test and the region for which it
// should be valid.
struct NumberTest {
  string raw_string_;
  string region_;

  string ToString() const {
    return StrCat(raw_string_, " (", region_, ")");
  }

  NumberTest(const string& raw_string, const string& region)
      : raw_string_(raw_string),
        region_(region) {
  }
};
}  // namespace

class PhoneNumberMatcherTest : public testing::Test {
 protected:
  PhoneNumberMatcherTest()
      : phone_util_(*PhoneNumberUtil::GetInstance()),
        matcher_(phone_util_, "",
                 RegionCode::US(),
                 PhoneNumberMatcher::VALID, 5),
        offset_(0) {
    PhoneNumberUtil::GetInstance()->SetLogger(new StdoutLogger());
  }

  bool IsLatinLetter(char32 letter) {
    return PhoneNumberMatcher::IsLatinLetter(letter);
  }

  bool ContainsMoreThanOneSlashInNationalNumber(
      const PhoneNumber& phone_number, const string& candidate) {
    return PhoneNumberMatcher::ContainsMoreThanOneSlashInNationalNumber(
        phone_number, candidate, phone_util_);
  }

  bool ExtractMatch(const string& text, PhoneNumberMatch* match) {
    return matcher_.ExtractMatch(text, offset_, match);
  }

  PhoneNumberMatcher* GetMatcherWithLeniency(
      const string& text, const string& region,
      PhoneNumberMatcher::Leniency leniency) const {
    return new PhoneNumberMatcher(phone_util_, text, region, leniency,
                                  100 /* max_tries */);
  }

  // Tests each number in the test cases provided is found in its entirety for
  // the specified leniency level.
  void DoTestNumberMatchesForLeniency(
      const std::vector<NumberTest>& test_cases,
      PhoneNumberMatcher::Leniency leniency) const {
    scoped_ptr<PhoneNumberMatcher> matcher;
    for (std::vector<NumberTest>::const_iterator test = test_cases.begin();
         test != test_cases.end(); ++test) {
      matcher.reset(GetMatcherWithLeniency(
          test->raw_string_, test->region_, leniency));
      EXPECT_TRUE(matcher->HasNext())
          << "No match found in " << test->ToString()
          << " for leniency: " << leniency;
      if (matcher->HasNext()) {
        PhoneNumberMatch match;
        matcher->Next(&match);
        EXPECT_EQ(test->raw_string_, match.raw_string())
            << "Found wrong match in test " << test->ToString()
            << ". Found " << match.raw_string();
      }
    }
  }

  // Tests no number in the test cases provided is found for the specified
  // leniency level.
  void DoTestNumberNonMatchesForLeniency(
      const std::vector<NumberTest>& test_cases,
      PhoneNumberMatcher::Leniency leniency) const {
    scoped_ptr<PhoneNumberMatcher> matcher;
    for (std::vector<NumberTest>::const_iterator test = test_cases.begin();
         test != test_cases.end(); ++test) {
      matcher.reset(GetMatcherWithLeniency(
          test->raw_string_, test->region_, leniency));
      EXPECT_FALSE(matcher->HasNext()) << "Match found in " << test->ToString()
                                       << " for leniency: " << leniency;
    }
  }

  // Asserts that the raw string and expected proto buffer for a match are set
  // appropriately.
  void AssertMatchProperties(const PhoneNumberMatch& match, const string& text,
                             const string& number, const string& region_code) {
    PhoneNumber expected_result;
    phone_util_.Parse(number, region_code, &expected_result);

    EXPECT_EQ(expected_result, match.number());
    EXPECT_EQ(number, match.raw_string()) << " Wrong number found in " << text;
  }

  // Asserts that another number can be found in "text" starting at "index", and
  // that its corresponding range is [start, end).
  void AssertEqualRange(const string& text, int index, int start, int end) {
    string sub = text.substr(index);
    PhoneNumberMatcher matcher(phone_util_, sub, RegionCode::NZ(),
                               PhoneNumberMatcher::POSSIBLE,
                               1000000 /* max_tries */);
    PhoneNumberMatch match;
    ASSERT_TRUE(matcher.HasNext());
    matcher.Next(&match);
    EXPECT_EQ(start - index, match.start());
    EXPECT_EQ(end - index, match.end());
    EXPECT_EQ(sub.substr(match.start(), match.length()), match.raw_string());
  }

  // Tests numbers found by the PhoneNumberMatcher in various textual contexts.
  void DoTestFindInContext(const string& number,
                           const string& default_country) {
    FindPossibleInContext(number, default_country);

    PhoneNumber parsed;
    phone_util_.Parse(number, default_country, &parsed);
    if (phone_util_.IsValidNumber(parsed)) {
      FindValidInContext(number, default_country);
    }
  }

  // Helper method which tests the contexts provided and ensures that:
  // -- if is_valid is true, they all find a test number inserted in the middle
  //   when leniency of matching is set to VALID; else no test number should be
  //   extracted at that leniency level
  // -- if is_possible is true, they all find a test number inserted in the
  //   middle when leniency of matching is set to POSSIBLE; else no test number
  //   should be extracted at that leniency level
  void FindMatchesInContexts(const std::vector<NumberContext>& contexts,
                             bool is_valid, bool is_possible,
                             const string& region, const string& number) {
    if (is_valid) {
      DoTestInContext(number, region, contexts, PhoneNumberMatcher::VALID);
    } else {
      for (std::vector<NumberContext>::const_iterator it = contexts.begin();
           it != contexts.end(); ++it) {
        string text = StrCat(it->leading_text_, number, it->trailing_text_);
        PhoneNumberMatcher matcher(text, region);
        EXPECT_FALSE(matcher.HasNext());
      }
    }
    if (is_possible) {
      DoTestInContext(number, region, contexts, PhoneNumberMatcher::POSSIBLE);
    } else {
      for (std::vector<NumberContext>::const_iterator it = contexts.begin();
           it != contexts.end(); ++it) {
        string text = StrCat(it->leading_text_, number, it->trailing_text_);
        PhoneNumberMatcher matcher(phone_util_, text, region,
                                   PhoneNumberMatcher::POSSIBLE,
                                   10000);  // Number of matches.
        EXPECT_FALSE(matcher.HasNext());
      }
    }
  }

  // Variant of FindMatchesInContexts that uses a default number and region.
  void FindMatchesInContexts(const std::vector<NumberContext>& contexts,
                             bool is_valid, bool is_possible) {
    const string& region = RegionCode::US();
    const string number("415-666-7777");

    FindMatchesInContexts(contexts, is_valid, is_possible, region, number);
  }

  // Tests valid numbers in contexts that should pass for
  // PhoneNumberMatcher::POSSIBLE.
  void FindPossibleInContext(const string& number,
                             const string& default_country) {
    std::vector<NumberContext> context_pairs;
    context_pairs.push_back(NumberContext("", ""));  // no context
    context_pairs.push_back(NumberContext("   ", "\t"));  // whitespace only
    context_pairs.push_back(NumberContext("Hello ", ""));  // no context at end
    // No context at start.
    context_pairs.push_back(NumberContext("", " to call me!"));
    context_pairs.push_back(NumberContext("Hi there, call ", " to reach me!"));
    // With commas.
    context_pairs.push_back(NumberContext("Hi there, call ", ", or don't"));
    // Three examples without whitespace around the number.
    context_pairs.push_back(NumberContext("Hi call", ""));
    context_pairs.push_back(NumberContext("", "forme"));
    context_pairs.push_back(NumberContext("Hi call", "forme"));
    // With other small numbers.
    context_pairs.push_back(NumberContext("It's cheap! Call ", " before 6:30"));
    // With a second number later.
    context_pairs.push_back(NumberContext("Call ", " or +1800-123-4567!"));
    // With a Month-Day date.
    context_pairs.push_back(NumberContext("Call me on June 2 at", ""));
    // With publication pages.
    context_pairs.push_back(NumberContext(
        "As quoted by Alfonso 12-15 (2009), you may call me at ", ""));
    context_pairs.push_back(NumberContext(
        "As quoted by Alfonso et al. 12-15 (2009), you may call me at ", ""));
    // With dates, written in the American style.
    context_pairs.push_back(NumberContext(
        "As I said on 03/10/2011, you may call me at ", ""));
    // With trailing numbers after a comma. The 45 should not be considered an
    // extension.
    context_pairs.push_back(NumberContext("", ", 45 days a year"));
    // When matching we don't consider semicolon along with legitimate extension
    // symbol to indicate an extension. The 7246433 should not be considered an
    // extension.
    context_pairs.push_back(NumberContext("", ";x 7246433"));
    // With a postfix stripped off as it looks like the start of another number.
    context_pairs.push_back(NumberContext("Call ", "/x12 more"));

    DoTestInContext(number, default_country, context_pairs,
                    PhoneNumberMatcher::POSSIBLE);
  }

  // Tests valid numbers in contexts that fail for PhoneNumberMatcher::POSSIBLE
  // but are valid for PhoneNumberMatcher::VALID.
  void FindValidInContext(const string& number, const string& default_country) {
    std::vector<NumberContext> context_pairs;
    // With other small numbers.
    context_pairs.push_back(NumberContext("It's only 9.99! Call ", " to buy"));
    // With a number Day.Month.Year date.
    context_pairs.push_back(NumberContext("Call me on 21.6.1984 at ", ""));
    // With a number Month/Day date.
    context_pairs.push_back(NumberContext("Call me on 06/21 at ", ""));
    // With a number Day.Month date.
    context_pairs.push_back(NumberContext("Call me on 21.6. at ", ""));
    // With a number Month/Day/Year date.
    context_pairs.push_back(NumberContext("Call me on 06/21/84 at ", ""));

    DoTestInContext(number, default_country, context_pairs,
                    PhoneNumberMatcher::VALID);
  }

  void DoTestInContext(const string& number, const string& default_country,
                       const std::vector<NumberContext>& context_pairs,
                       PhoneNumberMatcher::Leniency leniency) {
    for (std::vector<NumberContext>::const_iterator it = context_pairs.begin();
         it != context_pairs.end(); ++it) {
      string prefix = it->leading_text_;
      string text = StrCat(prefix, number, it->trailing_text_);

      int start = prefix.length();
      int end = start + number.length();
      PhoneNumberMatcher matcher(phone_util_, text, default_country, leniency,
                                 1000000 /* max_tries */);
      PhoneNumberMatch match;
      ASSERT_TRUE(matcher.HasNext())
          << "Did not find a number in '" << text << "'; expected '"
          << number << "'";
      matcher.Next(&match);

      string extracted = text.substr(match.start(), match.length());
      EXPECT_EQ(start, match.start());
      EXPECT_EQ(end, match.end());
      EXPECT_EQ(number, extracted);
      EXPECT_EQ(extracted, match.raw_string())
          << "Unexpected phone region in '" << text << "'; extracted '"
          << extracted << "'";
      EnsureTermination(text, default_country, leniency);
    }
  }

  // Exhaustively searches for phone numbers from each index within "text" to
  // test that finding matches always terminates.
  void EnsureTermination(const string& text, const string& default_country,
                         PhoneNumberMatcher::Leniency leniency) {
    for (size_t index = 0; index <= text.length(); ++index) {
      string sub = text.substr(index);
      // Iterates over all matches.
      PhoneNumberMatcher matcher(phone_util_, text, default_country, leniency,
                                 1000000 /* max_tries */);
      string matches;
      PhoneNumberMatch match;
      int match_count = 0;
      while (matcher.HasNext()) {
        matcher.Next(&match);
        StrAppend(&matches, ",", match.ToString());
        ++match_count;
      }
      // We should not ever find more than 10 matches in a single candidate text
      // in these test cases, so we check here that the matcher was limited by
      // the number of matches, rather than by max_tries.
      ASSERT_LT(match_count, 10);
    }
  }

  const PhoneNumberUtil& phone_util_;

 private:
  PhoneNumberMatcher matcher_;
  int offset_;
};

TEST_F(PhoneNumberMatcherTest, ContainsMoreThanOneSlashInNationalNumber) {
  // A date should return true.
  PhoneNumber number;
  number.set_country_code(1);
  number.set_country_code_source(PhoneNumber::FROM_DEFAULT_COUNTRY);
  string candidate = "1/05/2013";
  EXPECT_TRUE(ContainsMoreThanOneSlashInNationalNumber(number, candidate));

  // Here, the country code source thinks it started with a country calling
  // code, but this is not the same as the part before the slash, so it's still
  // true.
  number.Clear();
  number.set_country_code(274);
  number.set_country_code_source(PhoneNumber::FROM_NUMBER_WITHOUT_PLUS_SIGN);
  candidate = "27/4/2013";
  EXPECT_TRUE(ContainsMoreThanOneSlashInNationalNumber(number, candidate));

  // Now it should be false, because the first slash is after the country
  // calling code.
  number.Clear();
  number.set_country_code(49);
  number.set_country_code_source(PhoneNumber::FROM_NUMBER_WITH_PLUS_SIGN);
  candidate = "49/69/2013";
  EXPECT_FALSE(ContainsMoreThanOneSlashInNationalNumber(number, candidate));

  number.Clear();
  number.set_country_code(49);
  number.set_country_code_source(PhoneNumber::FROM_NUMBER_WITHOUT_PLUS_SIGN);
  candidate = "+49/69/2013";
  EXPECT_FALSE(ContainsMoreThanOneSlashInNationalNumber(number, candidate));

  candidate = "+ 49/69/2013";
  EXPECT_FALSE(ContainsMoreThanOneSlashInNationalNumber(number, candidate));

  candidate = "+ 49/69/20/13";
  EXPECT_TRUE(ContainsMoreThanOneSlashInNationalNumber(number, candidate));

  // Here, the first group is not assumed to be the country calling code, even
  // though it is the same as it, so this should return true.
  number.Clear();
  number.set_country_code(49);
  number.set_country_code_source(PhoneNumber::FROM_DEFAULT_COUNTRY);
  candidate = "49/69/2013";
  EXPECT_TRUE(ContainsMoreThanOneSlashInNationalNumber(number, candidate));
}

// See PhoneNumberUtilTest::ParseNationalNumber.
TEST_F(PhoneNumberMatcherTest, FindNationalNumber) {
  // Same cases as in ParseNationalNumber.
  DoTestFindInContext("033316005", RegionCode::NZ());
  // "33316005", RegionCode::NZ() is omitted since the national-prefix is
  // obligatory for these types of numbers in New Zealand.
  // National prefix attached and some formatting present.
  DoTestFindInContext("03-331 6005", RegionCode::NZ());
  DoTestFindInContext("03 331 6005", RegionCode::NZ());
  // Testing international prefixes.
  // Should strip country code.
  DoTestFindInContext("0064 3 331 6005", RegionCode::NZ());
  // Try again, but this time we have an international number with Region Code
  // US. It should recognize the country code and parse accordingly.
  DoTestFindInContext("01164 3 331 6005", RegionCode::US());
  DoTestFindInContext("+64 3 331 6005", RegionCode::US());

  DoTestFindInContext("64(0)64123456", RegionCode::NZ());
  // Check that using a "/" is fine in a phone number.
  // Note that real Polish numbers do *not* start with a 0.
  DoTestFindInContext("0123/456789", RegionCode::PL());
  DoTestFindInContext("123-456-7890", RegionCode::US());
}

// See PhoneNumberUtilTest::ParseWithInternationalPrefixes.
TEST_F(PhoneNumberMatcherTest, FindWithInternationalPrefixes) {
  DoTestFindInContext("+1 (650) 333-6000", RegionCode::NZ());
  DoTestFindInContext("1-650-333-6000", RegionCode::US());
  // Calling the US number from Singapore by using different service providers
  // 1st test: calling using SingTel IDD service (IDD is 001)
  DoTestFindInContext("0011-650-333-6000", RegionCode::SG());
  // 2nd test: calling using StarHub IDD service (IDD is 008)
  DoTestFindInContext("0081-650-333-6000", RegionCode::SG());
  // 3rd test: calling using SingTel V019 service (IDD is 019)
  DoTestFindInContext("0191-650-333-6000", RegionCode::SG());
  // Calling the US number from Poland
  DoTestFindInContext("0~01-650-333-6000", RegionCode::PL());
  // Using "++" at the start.
  DoTestFindInContext("++1 (650) 333-6000", RegionCode::PL());
  // Using a full-width plus sign.
  DoTestFindInContext(
      "\xEF\xBC\x8B""1 (650) 333-6000" /* "＋1 (650) 333-6000" */,
      RegionCode::SG());
  // The whole number, including punctuation, is here represented in full-width
  // form.
  DoTestFindInContext(
      /* "＋１　（６５０）　３３３－６０００" */
      "\xEF\xBC\x8B\xEF\xBC\x91\xE3\x80\x80\xEF\xBC\x88\xEF\xBC\x96\xEF\xBC\x95"
      "\xEF\xBC\x90\xEF\xBC\x89\xE3\x80\x80\xEF\xBC\x93\xEF\xBC\x93\xEF\xBC\x93"
      "\xEF\xBC\x8D\xEF\xBC\x96\xEF\xBC\x90\xEF\xBC\x90\xEF\xBC\x90",
      RegionCode::SG());
}

// See PhoneNumberUtilTest::ParseWithLeadingZero.
TEST_F(PhoneNumberMatcherTest, FindWithLeadingZero) {
  DoTestFindInContext("+39 02-36618 300", RegionCode::NZ());
  DoTestFindInContext("02-36618 300", RegionCode::IT());
  DoTestFindInContext("312 345 678", RegionCode::IT());
}

// See PhoneNumberUtilTest::ParseNationalNumberArgentina.
TEST_F(PhoneNumberMatcherTest, FindNationalNumberArgentina) {
  // Test parsing mobile numbers of Argentina.
  DoTestFindInContext("+54 9 343 555 1212", RegionCode::AR());
  DoTestFindInContext("0343 15 555 1212", RegionCode::AR());

  DoTestFindInContext("+54 9 3715 65 4320", RegionCode::AR());
  DoTestFindInContext("03715 15 65 4320", RegionCode::AR());

  // Test parsing fixed-line numbers of Argentina.
  DoTestFindInContext("+54 11 3797 0000", RegionCode::AR());
  DoTestFindInContext("011 3797 0000", RegionCode::AR());

  DoTestFindInContext("+54 3715 65 4321", RegionCode::AR());
  DoTestFindInContext("03715 65 4321", RegionCode::AR());

  DoTestFindInContext("+54 23 1234 0000", RegionCode::AR());
  DoTestFindInContext("023 1234 0000", RegionCode::AR());
}

// See PhoneNumberMatcherTest::ParseWithXInNumber.
TEST_F(PhoneNumberMatcherTest, FindWithXInNumber) {
  DoTestFindInContext("(0xx) 123456789", RegionCode::AR());
  // A case where x denotes both carrier codes and extension symbol.
  DoTestFindInContext("(0xx) 123456789 x 1234", RegionCode::AR());

  // This test is intentionally constructed such that the number of digit after
  // xx is larger than 7, so that the number won't be mistakenly treated as an
  // extension, as we allow extensions up to 7 digits. This assumption is okay
  // for now as all the countries where a carrier selection code is written in
  // the form of xx have a national significant number of length larger than 7.
  DoTestFindInContext("011xx5481429712", RegionCode::US());
}

// See PhoneNumberUtilTest::ParseNumbersMexico.
TEST_F(PhoneNumberMatcherTest, FindNumbersMexico) {
  // Test parsing fixed-line numbers of Mexico.
  DoTestFindInContext("+52 (449)978-0001", RegionCode::MX());
  DoTestFindInContext("01 (449)978-0001", RegionCode::MX());
  DoTestFindInContext("(449)978-0001", RegionCode::MX());

  // Test parsing mobile numbers of Mexico.
  DoTestFindInContext("+52 1 33 1234-5678", RegionCode::MX());
  DoTestFindInContext("044 (33) 1234-5678", RegionCode::MX());
  DoTestFindInContext("045 33 1234-5678", RegionCode::MX());
}

// See PhoneNumberUtilTest::ParseNumbersWithPlusWithNoRegion.
TEST_F(PhoneNumberMatcherTest, FindNumbersWithPlusWithNoRegion) {
  // RegionCode::ZZ() is allowed only if the number starts with a '+' - then the
  // country code can be calculated.
  DoTestFindInContext("+64 3 331 6005", RegionCode::ZZ());
}

// See PhoneNumberUtilTest::ParseExtensions.
TEST_F(PhoneNumberMatcherTest, FindExtensions) {
  DoTestFindInContext("03 331 6005 ext 3456", RegionCode::NZ());
  DoTestFindInContext("03-3316005x3456", RegionCode::NZ());
  DoTestFindInContext("03-3316005 int.3456", RegionCode::NZ());
  DoTestFindInContext("03 3316005 #3456", RegionCode::NZ());
  DoTestFindInContext("0~0 1800 7493 524", RegionCode::PL());
  DoTestFindInContext("(1800) 7493.524", RegionCode::US());
  // Check that the last instance of an extension token is matched.
  DoTestFindInContext("0~0 1800 7493 524 ~1234", RegionCode::PL());
  // Verifying bug-fix where the last digit of a number was previously omitted
  // if it was a 0 when extracting the extension. Also verifying a few different
  // cases of extensions.
  DoTestFindInContext("+44 2034567890x456", RegionCode::NZ());
  DoTestFindInContext("+44 2034567890x456", RegionCode::GB());
  DoTestFindInContext("+44 2034567890 x456", RegionCode::GB());
  DoTestFindInContext("+44 2034567890 X456", RegionCode::GB());
  DoTestFindInContext("+44 2034567890 X 456", RegionCode::GB());
  DoTestFindInContext("+44 2034567890 X  456", RegionCode::GB());
  DoTestFindInContext("+44 2034567890  X 456", RegionCode::GB());

  DoTestFindInContext("(800) 901-3355 x 7246433", RegionCode::US());
  DoTestFindInContext("(800) 901-3355 , ext 7246433", RegionCode::US());
  DoTestFindInContext("(800) 901-3355 ,extension 7246433", RegionCode::US());
  // The next test differs from PhoneNumberUtil -> when matching we don't
  // consider a lone comma to indicate an extension, although we accept it when
  // parsing.
  DoTestFindInContext("(800) 901-3355 ,x 7246433", RegionCode::US());
  DoTestFindInContext("(800) 901-3355 ext: 7246433", RegionCode::US());
}

TEST_F(PhoneNumberMatcherTest, FindInterspersedWithSpace) {
  DoTestFindInContext("0 3   3 3 1   6 0 0 5", RegionCode::NZ());
}

// Test matching behavior when starting in the middle of a phone number.
TEST_F(PhoneNumberMatcherTest, IntermediateParsePositions) {
  string text = "Call 033316005  or 032316005!";
  //             |    |    |    |    |    |
  //             0    5   10   15   20   25

  // Iterate over all possible indices.
  for (int i = 0; i <= 5; ++i) {
    AssertEqualRange(text, i, 5, 14);
  }
  // 7 and 8 digits in a row are still parsed as number.
  AssertEqualRange(text, 6, 6, 14);
  AssertEqualRange(text, 7, 7, 14);
  // Anything smaller is skipped to the second instance.
  for (int i = 8; i <= 19; ++i) {
    AssertEqualRange(text, i, 19, 28);
  }
}

TEST_F(PhoneNumberMatcherTest, FourMatchesInARow) {
  string number1 = "415-666-7777";
  string number2 = "800-443-1223";
  string number3 = "212-443-1223";
  string number4 = "650-443-1223";
  string text = StrCat(number1, " - ", number2, " - ", number3, " - ", number4);

  PhoneNumberMatcher matcher(text, RegionCode::US());
  PhoneNumberMatch match;

  EXPECT_TRUE(matcher.HasNext());
  EXPECT_TRUE(matcher.Next(&match));
  AssertMatchProperties(match, text, number1, RegionCode::US());

  EXPECT_TRUE(matcher.HasNext());
  EXPECT_TRUE(matcher.Next(&match));
  AssertMatchProperties(match, text, number2, RegionCode::US());

  EXPECT_TRUE(matcher.HasNext());
  EXPECT_TRUE(matcher.Next(&match));
  AssertMatchProperties(match, text, number3, RegionCode::US());

  EXPECT_TRUE(matcher.HasNext());
  EXPECT_TRUE(matcher.Next(&match));
  AssertMatchProperties(match, text, number4, RegionCode::US());
}

TEST_F(PhoneNumberMatcherTest, MatchesFoundWithMultipleSpaces) {
  string number1 = "415-666-7777";
  string number2 = "800-443-1223";
  string text = StrCat(number1, " ", number2);

  PhoneNumberMatcher matcher(text, RegionCode::US());
  PhoneNumberMatch match;

  EXPECT_TRUE(matcher.HasNext());
  EXPECT_TRUE(matcher.Next(&match));
  AssertMatchProperties(match, text, number1, RegionCode::US());

  EXPECT_TRUE(matcher.HasNext());
  EXPECT_TRUE(matcher.Next(&match));
  AssertMatchProperties(match, text, number2, RegionCode::US());
}

TEST_F(PhoneNumberMatcherTest, MatchWithSurroundingZipcodes) {
  string number = "415-666-7777";
  string zip_preceding =
      StrCat("My address is CA 34215 - ", number, " is my number.");
  PhoneNumber expected_result;
  phone_util_.Parse(number, RegionCode::US(), &expected_result);

  scoped_ptr<PhoneNumberMatcher> matcher(
      GetMatcherWithLeniency(zip_preceding, RegionCode::US(),
                             PhoneNumberMatcher::VALID));

  PhoneNumberMatch match;
  EXPECT_TRUE(matcher->HasNext());
  EXPECT_TRUE(matcher->Next(&match));
  AssertMatchProperties(match, zip_preceding, number, RegionCode::US());

  // Now repeat, but this time the phone number has spaces in it. It should
  // still be found.
  number = "(415) 666 7777";

  string zip_following =
      StrCat("My number is ", number, ". 34215 is my zip-code.");
  matcher.reset(
      GetMatcherWithLeniency(zip_following, RegionCode::US(),
                             PhoneNumberMatcher::VALID));

  PhoneNumberMatch match_with_spaces;
  EXPECT_TRUE(matcher->HasNext());
  EXPECT_TRUE(matcher->Next(&match_with_spaces));
  AssertMatchProperties(
      match_with_spaces, zip_following, number, RegionCode::US());
}

TEST_F(PhoneNumberMatcherTest, IsLatinLetter) {
  EXPECT_TRUE(IsLatinLetter('c'));
  EXPECT_TRUE(IsLatinLetter('C'));
  EXPECT_TRUE(IsLatinLetter(UnicodeString::fromUTF8("\xC3\x89" /* "É" */)[0]));
  // Combining acute accent.
  EXPECT_TRUE(IsLatinLetter(UnicodeString::fromUTF8("\xCC\x81")[0]));
  EXPECT_FALSE(IsLatinLetter(':'));
  EXPECT_FALSE(IsLatinLetter('5'));
  EXPECT_FALSE(IsLatinLetter('-'));
  EXPECT_FALSE(IsLatinLetter('.'));
  EXPECT_FALSE(IsLatinLetter(' '));
  EXPECT_FALSE(
      IsLatinLetter(UnicodeString::fromUTF8("\xE6\x88\x91" /* "我" */)[0]));
  /* Hiragana letter no (の) - this should neither seem to start or end with a
     Latin letter. */
  EXPECT_FALSE(IsLatinLetter(UnicodeString::fromUTF8("\xE3\x81\xAE")[0]));
  EXPECT_FALSE(IsLatinLetter(UnicodeString::fromUTF8("\xE3\x81\xAE")[2]));
}

TEST_F(PhoneNumberMatcherTest, MatchesWithSurroundingLatinChars) {
  std::vector<NumberContext> possible_only_contexts;
  possible_only_contexts.push_back(NumberContext("abc", "def"));
  possible_only_contexts.push_back(NumberContext("abc", ""));
  possible_only_contexts.push_back(NumberContext("", "def"));
  possible_only_contexts.push_back(NumberContext("\xC3\x89" /* "É" */, ""));
  // e with an acute accent decomposed (with combining mark).
  possible_only_contexts.push_back(
      NumberContext("\x20\x22\xCC\x81""e\xCC\x81" /* "́e\xCC\x81" */, ""));

  // Numbers should not be considered valid, if they are surrounded by Latin
  // characters, but should be considered possible.
  FindMatchesInContexts(possible_only_contexts, false, true);
}

TEST_F(PhoneNumberMatcherTest, MoneyNotSeenAsPhoneNumber) {
  std::vector<NumberContext> possible_only_contexts;
  possible_only_contexts.push_back(NumberContext("$", ""));
  possible_only_contexts.push_back(NumberContext("", "$"));
  possible_only_contexts.push_back(NumberContext("\xC2\xA3" /* "£" */, ""));
  possible_only_contexts.push_back(NumberContext("\xC2\xA5" /* "¥" */, ""));
  FindMatchesInContexts(possible_only_contexts, false, true);
}

TEST_F(PhoneNumberMatcherTest, PercentageNotSeenAsPhoneNumber) {
  std::vector<NumberContext> possible_only_contexts;
  possible_only_contexts.push_back(NumberContext("", "%"));
  // Numbers followed by % should be dropped.
  FindMatchesInContexts(possible_only_contexts, false, true);
}

TEST_F(PhoneNumberMatcherTest, PhoneNumberWithLeadingOrTrailingMoneyMatches) {
  std::vector<NumberContext> contexts;
  contexts.push_back(NumberContext("$20 ", ""));
  contexts.push_back(NumberContext("", " 100$"));
  // Because of the space after the 20 (or before the 100) these dollar amounts
  // should not stop the actual number from being found.
  FindMatchesInContexts(contexts, true, true);
}

TEST_F(PhoneNumberMatcherTest,
       MatchesWithSurroundingLatinCharsAndLeadingPunctuation) {
  std::vector<NumberContext> possible_only_contexts;
  // Contexts with trailing characters. Leading characters are okay here since
  // the numbers we will insert start with punctuation, but trailing characters
  // are still not allowed.
  possible_only_contexts.push_back(NumberContext("abc", "def"));
  possible_only_contexts.push_back(NumberContext("", "def"));
  possible_only_contexts.push_back(NumberContext("", "\xC3\x89" /* "É" */));

  // Numbers should not be considered valid, if they have trailing Latin
  // characters, but should be considered possible.
  string number_with_plus = "+14156667777";
  string number_with_brackets = "(415)6667777";
  FindMatchesInContexts(possible_only_contexts, false, true, RegionCode::US(),
                        number_with_plus);
  FindMatchesInContexts(possible_only_contexts, false, true, RegionCode::US(),
                        number_with_brackets);

  std::vector<NumberContext> valid_contexts;
  valid_contexts.push_back(NumberContext("abc", ""));
  valid_contexts.push_back(NumberContext("\xC3\x89" /* "É" */, ""));
  valid_contexts.push_back(
      NumberContext("\xC3\x89" /* "É" */, "."));  // Trailing punctuation.
  // Trailing white-space.
  valid_contexts.push_back(NumberContext("\xC3\x89" /* "É" */, " def"));

  // Numbers should be considered valid, since they start with punctuation.
  FindMatchesInContexts(valid_contexts, true, true, RegionCode::US(),
                        number_with_plus);
  FindMatchesInContexts(valid_contexts, true, true, RegionCode::US(),
                        number_with_brackets);
}

TEST_F(PhoneNumberMatcherTest, MatchesWithSurroundingChineseChars) {
  std::vector<NumberContext> valid_contexts;
  valid_contexts.push_back(NumberContext(
      /* "我的电话号码是" */
      "\xE6\x88\x91\xE7\x9A\x84\xE7\x94\xB5\xE8\xAF\x9D\xE5\x8F\xB7\xE7\xA0\x81"
      "\xE6\x98\xAF", ""));
  valid_contexts.push_back(NumberContext(
      "",
      /* "是我的电话号码" */
      "\xE6\x98\xAF\xE6\x88\x91\xE7\x9A\x84\xE7\x94\xB5\xE8\xAF\x9D\xE5\x8F\xB7"
      "\xE7\xA0\x81"));
  valid_contexts.push_back(NumberContext(
      "\xE8\xAF\xB7\xE6\x8B\xA8\xE6\x89\x93" /* "请拨打" */,
      "\xE6\x88\x91\xE5\x9C\xA8\xE6\x98\x8E\xE5\xA4\xA9" /* "我在明天" */));

  // Numbers should be considered valid, since they are surrounded by Chinese.
  FindMatchesInContexts(valid_contexts, true, true);
}

TEST_F(PhoneNumberMatcherTest, MatchesWithSurroundingPunctuation) {
  std::vector<NumberContext> valid_contexts;
  // At end of text.
  valid_contexts.push_back(NumberContext("My number-", ""));
  // At start of text.
  valid_contexts.push_back(NumberContext("", ".Nice day."));
  // Punctuation surround number.
  valid_contexts.push_back(NumberContext("Tel:", "."));
  // White-space is also fine.
  valid_contexts.push_back(NumberContext("Tel: ", " on Saturdays."));

  // Numbers should be considered valid, since they are surrounded by
  // punctuation.
  FindMatchesInContexts(valid_contexts, true, true);
}

TEST_F(PhoneNumberMatcherTest,
       MatchesMultiplePhoneNumbersSeparatedByPhoneNumberPunctuation) {
  const string text = "Call 650-253-4561 -- 455-234-3451";
  const string& region = RegionCode::US();
  PhoneNumber number1;
  number1.set_country_code(phone_util_.GetCountryCodeForRegion(region));
  number1.set_national_number(6502534561ULL);
  PhoneNumberMatch match1(5, "650-253-4561", number1);

  PhoneNumber number2;
  number2.set_country_code(phone_util_.GetCountryCodeForRegion(region));
  number2.set_national_number(4552343451ULL);
  PhoneNumberMatch match2(21, "455-234-3451", number2);

  PhoneNumberMatcher matcher(
      phone_util_, text, region, PhoneNumberMatcher::VALID, 100);

  PhoneNumberMatch actual_match1;
  PhoneNumberMatch actual_match2;
  matcher.Next(&actual_match1);
  matcher.Next(&actual_match2);
  EXPECT_TRUE(match1.Equals(actual_match1))
      << "Got: " << actual_match1.ToString();
  EXPECT_TRUE(match2.Equals(actual_match2))
      << "Got: " << actual_match2.ToString();
}

TEST_F(PhoneNumberMatcherTest,
       DoesNotMatchMultiplePhoneNumbersSeparatedWithNoWhiteSpace) {
  const string text = "Call 650-253-4561--455-234-3451";
  const string& region = RegionCode::US();
  PhoneNumberMatcher matcher(
      phone_util_, text, region, PhoneNumberMatcher::VALID, 100);
  EXPECT_FALSE(matcher.HasNext());
}

// Strings with number-like things that shouldn't be found under any level.
static const NumberTest kImpossibleCases[] = {
  NumberTest("12345", RegionCode::US()),
  NumberTest("23456789", RegionCode::US()),
  NumberTest("234567890112", RegionCode::US()),
  NumberTest("650+253+1234", RegionCode::US()),
  NumberTest("3/10/1984", RegionCode::CA()),
  NumberTest("03/27/2011", RegionCode::US()),
  NumberTest("31/8/2011", RegionCode::US()),
  NumberTest("1/12/2011", RegionCode::US()),
  NumberTest("10/12/82", RegionCode::DE()),
  NumberTest("650x2531234", RegionCode::US()),
  NumberTest("2012-01-02 08:00", RegionCode::US()),
  NumberTest("2012/01/02 08:00", RegionCode::US()),
  NumberTest("20120102 08:00", RegionCode::US()),
  NumberTest("2014-04-12 04:04 PM", RegionCode::US()),
  NumberTest("2014-04-12 &nbsp;04:04 PM", RegionCode::US()),
  NumberTest("2014-04-12 &nbsp;04:04 PM", RegionCode::US()),
  NumberTest("2014-04-12  04:04 PM", RegionCode::US()),
};

// Strings with number-like things that should only be found under "possible".
static const NumberTest kPossibleOnlyCases[] = {
  // US numbers cannot start with 7 in the test metadata to be valid.
  NumberTest("7121115678", RegionCode::US()),
  // 'X' should not be found in numbers at leniencies stricter than POSSIBLE,
  // unless it represents a carrier code or extension.
  NumberTest("1650 x 253 - 1234", RegionCode::US()),
  NumberTest("650 x 253 - 1234", RegionCode::US()),
  NumberTest("6502531x234", RegionCode::US()),
  NumberTest("(20) 3346 1234", RegionCode::GB()),  // Non-optional NP omitted
};

// Strings with number-like things that should only be found up to and including
// the "valid" leniency level.
static const NumberTest kValidCases[] = {
  NumberTest("65 02 53 00 00", RegionCode::US()),
  NumberTest("6502 538365", RegionCode::US()),
  // 2 slashes are illegal at higher levels.
  NumberTest("650//253-1234", RegionCode::US()),
  NumberTest("650/253/1234", RegionCode::US()),
  NumberTest("9002309. 158", RegionCode::US()),
  NumberTest("12 7/8 - 14 12/34 - 5", RegionCode::US()),
  NumberTest("12.1 - 23.71 - 23.45", RegionCode::US()),
  NumberTest("800 234 1 111x1111", RegionCode::US()),
  NumberTest("1979-2011 100", RegionCode::US()),
  // National number in wrong format.
  NumberTest("+494949-4-94", RegionCode::DE()),
  NumberTest(
      /* "４１５６６６-７７７７" */
      "\xEF\xBC\x94\xEF\xBC\x91\xEF\xBC\x95\xEF\xBC\x96\xEF\xBC\x96\xEF\xBC\x96"
      "\x2D\xEF\xBC\x97\xEF\xBC\x97\xEF\xBC\x97\xEF\xBC\x97", RegionCode::US()),
  NumberTest("2012-0102 08", RegionCode::US()),  // Very strange formatting.
  NumberTest("2012-01-02 08", RegionCode::US()),
  // Breakdown assistance number with unexpected formatting.
  NumberTest("1800-1-0-10 22", RegionCode::AU()),
  NumberTest("030-3-2 23 12 34", RegionCode::DE()),
  NumberTest("03 0 -3 2 23 12 34", RegionCode::DE()),
  NumberTest("(0)3 0 -3 2 23 12 34", RegionCode::DE()),
  NumberTest("0 3 0 -3 2 23 12 34", RegionCode::DE()),
#ifdef I18N_PHONENUMBERS_USE_ALTERNATE_FORMATS
  // Fits an alternate pattern, but the leading digits don't match.
  NumberTest("+52 332 123 23 23", RegionCode::MX()),
#endif  // I18N_PHONENUMBERS_USE_ALTERNATE_FORMATS
};

// Strings with number-like things that should only be found up to and including
// the "strict_grouping" leniency level.
static const NumberTest kStrictGroupingCases[] = {
  NumberTest("(415) 6667777", RegionCode::US()),
  NumberTest("415-6667777", RegionCode::US()),
  // Should be found by strict grouping but not exact grouping, as the last two
  // groups are formatted together as a block.
  NumberTest("0800-2491234", RegionCode::DE()),
  // If the user is using alternate formats, test that numbers formatted in
  // that way are found.
#ifdef I18N_PHONENUMBERS_USE_ALTERNATE_FORMATS
  // Doesn't match any formatting in the test file, but almost matches an
  // alternate format (the last two groups have been squashed together here).
  NumberTest("0900-1 123123", RegionCode::DE()),
  NumberTest("(0)900-1 123123", RegionCode::DE()),
  NumberTest("0 900-1 123123", RegionCode::DE()),
#endif  // I18N_PHONENUMBERS_USE_ALTERNATE_FORMATS
  // NDC also found as part of the country calling code; this shouldn't ruin the
  // grouping expectations.
  NumberTest("+33 3 34 2312", RegionCode::FR()),
};

// Strings with number-like things that should be found at all levels.
static const NumberTest kExactGroupingCases[] = {
  NumberTest(
      /* "４１５６６６７７７７" */
      "\xEF\xBC\x94\xEF\xBC\x91\xEF\xBC\x95\xEF\xBC\x96\xEF\xBC\x96\xEF\xBC\x96"
      "\xEF\xBC\x97\xEF\xBC\x97\xEF\xBC\x97\xEF\xBC\x97", RegionCode::US()),
  NumberTest(
      /* "４１５－６６６－７７７７" */
      "\xEF\xBC\x94\xEF\xBC\x91\xEF\xBC\x95\xEF\xBC\x8D\xEF\xBC\x96\xEF\xBC\x96"
      "\xEF\xBC\x96\xEF\xBC\x8D\xEF\xBC\x97\xEF\xBC\x97\xEF\xBC\x97"
      "\xEF\xBC\x97", RegionCode::US()),
  NumberTest("4156667777", RegionCode::US()),
  NumberTest("4156667777 x 123", RegionCode::US()),
  NumberTest("415-666-7777", RegionCode::US()),
  NumberTest("415/666-7777", RegionCode::US()),
  NumberTest("415-666-7777 ext. 503", RegionCode::US()),
  NumberTest("1 415 666 7777 x 123", RegionCode::US()),
  NumberTest("+1 415-666-7777", RegionCode::US()),
  NumberTest("+494949 49", RegionCode::DE()),
  NumberTest("+49-49-34", RegionCode::DE()),
  NumberTest("+49-4931-49", RegionCode::DE()),
  NumberTest("04931-49", RegionCode::DE()),  // With National Prefix
  NumberTest("+49-494949", RegionCode::DE()),  // One group with country code
  NumberTest("+49-494949 ext. 49", RegionCode::DE()),
  NumberTest("+49494949 ext. 49", RegionCode::DE()),
  NumberTest("0494949", RegionCode::DE()),
  NumberTest("0494949 ext. 49", RegionCode::DE()),
  NumberTest("01 (33) 3461 2234", RegionCode::MX()),  // Optional NP present
  NumberTest("(33) 3461 2234", RegionCode::MX()),  // Optional NP omitted
  // If the user is using alternate formats, test that numbers formatted in
  // that way are found.
#ifdef I18N_PHONENUMBERS_USE_ALTERNATE_FORMATS
  // Breakdown assistance number using alternate formatting pattern.
  NumberTest("1800-10-10 22", RegionCode::AU()),
  // Doesn't match any formatting in the test file, but matches an alternate
  // format exactly.
  NumberTest("0900-1 123 123", RegionCode::DE()),
  NumberTest("(0)900-1 123 123", RegionCode::DE()),
  NumberTest("0 900-1 123 123", RegionCode::DE()),
#endif  // I18N_PHONENUMBERS_USE_ALTERNATE_FORMATS
  NumberTest("+33 3 34 23 12", RegionCode::FR()),
};

TEST_F(PhoneNumberMatcherTest, MatchesWithPossibleLeniency) {
  std::vector<NumberTest> test_cases;
  test_cases.insert(test_cases.begin(), kPossibleOnlyCases,
                    kPossibleOnlyCases + arraysize(kPossibleOnlyCases));
  test_cases.insert(test_cases.begin(), kValidCases,
                    kValidCases + arraysize(kValidCases));
  test_cases.insert(
      test_cases.begin(), kStrictGroupingCases,
      kStrictGroupingCases + arraysize(kStrictGroupingCases));
  test_cases.insert(test_cases.begin(), kExactGroupingCases,
                    kExactGroupingCases + arraysize(kExactGroupingCases));
  DoTestNumberMatchesForLeniency(test_cases, PhoneNumberMatcher::POSSIBLE);
}

TEST_F(PhoneNumberMatcherTest, NonMatchesWithPossibleLeniency) {
  std::vector<NumberTest> test_cases;
  test_cases.insert(test_cases.begin(), kImpossibleCases,
                    kImpossibleCases + arraysize(kImpossibleCases));
  DoTestNumberNonMatchesForLeniency(test_cases, PhoneNumberMatcher::POSSIBLE);
}

TEST_F(PhoneNumberMatcherTest, MatchesWithValidLeniency) {
  std::vector<NumberTest> test_cases;
  test_cases.insert(test_cases.begin(), kValidCases,
                    kValidCases + arraysize(kValidCases));
  test_cases.insert(
      test_cases.begin(), kStrictGroupingCases,
      kStrictGroupingCases + arraysize(kStrictGroupingCases));
  test_cases.insert(test_cases.begin(), kExactGroupingCases,
                    kExactGroupingCases + arraysize(kExactGroupingCases));
  DoTestNumberMatchesForLeniency(test_cases, PhoneNumberMatcher::VALID);
}

TEST_F(PhoneNumberMatcherTest, NonMatchesWithValidLeniency) {
  std::vector<NumberTest> test_cases;
  test_cases.insert(test_cases.begin(), kImpossibleCases,
                    kImpossibleCases + arraysize(kImpossibleCases));
  test_cases.insert(test_cases.begin(), kPossibleOnlyCases,
                    kPossibleOnlyCases + arraysize(kPossibleOnlyCases));
  DoTestNumberNonMatchesForLeniency(test_cases, PhoneNumberMatcher::VALID);
}

TEST_F(PhoneNumberMatcherTest, MatchesWithStrictGroupingLeniency) {
  std::vector<NumberTest> test_cases;
  test_cases.insert(
      test_cases.begin(), kStrictGroupingCases,
      kStrictGroupingCases + arraysize(kStrictGroupingCases));
  test_cases.insert(test_cases.begin(), kExactGroupingCases,
                    kExactGroupingCases + arraysize(kExactGroupingCases));
  DoTestNumberMatchesForLeniency(test_cases,
                                 PhoneNumberMatcher::STRICT_GROUPING);
}

TEST_F(PhoneNumberMatcherTest, NonMatchesWithStrictGroupingLeniency) {
  std::vector<NumberTest> test_cases;
  test_cases.insert(test_cases.begin(), kImpossibleCases,
                    kImpossibleCases + arraysize(kImpossibleCases));
  test_cases.insert(test_cases.begin(), kPossibleOnlyCases,
                    kPossibleOnlyCases + arraysize(kPossibleOnlyCases));
  test_cases.insert(test_cases.begin(), kValidCases,
                    kValidCases + arraysize(kValidCases));
  DoTestNumberNonMatchesForLeniency(test_cases,
                                    PhoneNumberMatcher::STRICT_GROUPING);
}

TEST_F(PhoneNumberMatcherTest, MatchesWithExactGroupingLeniency) {
  std::vector<NumberTest> test_cases;
  test_cases.insert(test_cases.begin(), kExactGroupingCases,
                    kExactGroupingCases + arraysize(kExactGroupingCases));
  DoTestNumberMatchesForLeniency(test_cases,
                                 PhoneNumberMatcher::EXACT_GROUPING);
}

TEST_F(PhoneNumberMatcherTest, NonMatchesWithExactGroupingLeniency) {
  std::vector<NumberTest> test_cases;
  test_cases.insert(test_cases.begin(), kImpossibleCases,
                    kImpossibleCases + arraysize(kImpossibleCases));
  test_cases.insert(test_cases.begin(), kPossibleOnlyCases,
                    kPossibleOnlyCases + arraysize(kPossibleOnlyCases));
  test_cases.insert(test_cases.begin(), kValidCases,
                    kValidCases + arraysize(kValidCases));
  test_cases.insert(
      test_cases.begin(), kStrictGroupingCases,
      kStrictGroupingCases + arraysize(kStrictGroupingCases));
  DoTestNumberNonMatchesForLeniency(test_cases,
                                    PhoneNumberMatcher::EXACT_GROUPING);
}

TEST_F(PhoneNumberMatcherTest, ExtractMatchIgnoresAmericanDates) {
  PhoneNumberMatch match;
  string text = "As I said on 03/10/2011, you may call me at ";
  EXPECT_FALSE(ExtractMatch(text, &match));
  text = "As I said on 03/27/2011, you may call me at ";
  EXPECT_FALSE(ExtractMatch(text, &match));
  text = "As I said on 31/8/2011, you may call me at ";
  EXPECT_FALSE(ExtractMatch(text, &match));
  text = "As I said on 1/12/2011, you may call me at ";
  EXPECT_FALSE(ExtractMatch(text, &match));
  text = "I was born on 10/12/82. Please call me at ";
  EXPECT_FALSE(ExtractMatch(text, &match));
}

TEST_F(PhoneNumberMatcherTest, NonMatchingBracketsAreInvalid) {
  // The digits up to the ", " form a valid US number, but it shouldn't be
  // matched as one since there was a non-matching bracket present.
  scoped_ptr<PhoneNumberMatcher> matcher(GetMatcherWithLeniency(
      "80.585 [79.964, 81.191]", RegionCode::US(),
      PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());

  // The trailing "]" is thrown away before parsing, so the resultant number,
  // while a valid US number, does not have matching brackets.
  matcher.reset(GetMatcherWithLeniency(
      "80.585 [79.964]", RegionCode::US(), PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());

  matcher.reset(GetMatcherWithLeniency(
      "80.585 ((79.964)", RegionCode::US(), PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());

  // This case has too many sets of brackets to be valid.
  matcher.reset(GetMatcherWithLeniency(
      "(80).(585) (79).(9)64", RegionCode::US(), PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());
}

TEST_F(PhoneNumberMatcherTest, NoMatchIfRegionIsUnknown) {
  // Fail on non-international prefix if region code is ZZ.
  scoped_ptr<PhoneNumberMatcher> matcher(GetMatcherWithLeniency(
      "Random text body - number is 0331 6005, see you there",
      RegionCode::ZZ(), PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());
}

TEST_F(PhoneNumberMatcherTest, NoMatchInEmptyString) {
  scoped_ptr<PhoneNumberMatcher> matcher(GetMatcherWithLeniency(
      "", RegionCode::US(), PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());
  matcher.reset(GetMatcherWithLeniency("  ", RegionCode::US(),
                                       PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());
}

TEST_F(PhoneNumberMatcherTest, NoMatchIfNoNumber) {
  scoped_ptr<PhoneNumberMatcher> matcher(GetMatcherWithLeniency(
      "Random text body - number is foobar, see you there", RegionCode::US(),
      PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());
}

TEST_F(PhoneNumberMatcherTest, NoErrorWithSpecialCharacters) {
  string stringWithSpecialCharacters =
      "Myfuzzvar1152: \"My info:%415-666-7777 123 fake street\"\nfuzzvar1155: "
      "47\nfuzzvar1158: %415-666-1234 "
      "i18n_phonenumbers_Pho\356eNumberMatcher_Leniency_VALID_1"
      "\nfuzzvar1159: 20316 info:%415-666-7777 123 fake str79ee\nt";
  string Numbers;
  for (int i = 0; i < 100; ++i)
    Numbers.append(stringWithSpecialCharacters);
  scoped_ptr<PhoneNumberMatcher> matcher(
      GetMatcherWithLeniency(Numbers, RegionCode::US(),
                             PhoneNumberMatcher::POSSIBLE));
  // Since the input text contains invalid UTF-8, we do not return
  // any matches.
  EXPECT_FALSE(matcher->HasNext());
}

TEST_F(PhoneNumberMatcherTest, Sequences) {
  // Test multiple occurrences.
  const string text = "Call 033316005  or 032316005!";
  const string& region = RegionCode::NZ();

  PhoneNumber number1;
  number1.set_country_code(phone_util_.GetCountryCodeForRegion(region));
  number1.set_national_number(33316005ULL);
  PhoneNumberMatch match1(5, "033316005", number1);

  PhoneNumber number2;
  number2.set_country_code(phone_util_.GetCountryCodeForRegion(region));
  number2.set_national_number(32316005ULL);
  PhoneNumberMatch match2(19, "032316005", number2);

  PhoneNumberMatcher matcher(
      phone_util_, text, region, PhoneNumberMatcher::POSSIBLE, 100);

  PhoneNumberMatch actual_match1;
  PhoneNumberMatch actual_match2;
  matcher.Next(&actual_match1);
  matcher.Next(&actual_match2);
  EXPECT_TRUE(match1.Equals(actual_match1));
  EXPECT_TRUE(match2.Equals(actual_match2));
}

TEST_F(PhoneNumberMatcherTest, MaxMatches) {
  // Set up text with 100 valid phone numbers.
  string numbers;
  for (int i = 0; i < 100; ++i) {
    numbers.append("My info: 415-666-7777,");
  }

  // Matches all 100. Max only applies to failed cases.
  PhoneNumber number;
  phone_util_.Parse("+14156667777", RegionCode::US(), &number);
  std::vector<PhoneNumber> expected(100, number);

  PhoneNumberMatcher matcher(
      phone_util_, numbers, RegionCode::US(), PhoneNumberMatcher::VALID, 10);
  std::vector<PhoneNumber> actual;
  PhoneNumberMatch match;
  while (matcher.HasNext()) {
    matcher.Next(&match);
    actual.push_back(match.number());
  }
  EXPECT_EQ(expected, actual);
}

TEST_F(PhoneNumberMatcherTest, MaxMatchesInvalid) {
  // Set up text with 10 invalid phone numbers followed by 100 valid.
  string numbers;
  for (int i = 0; i < 10; ++i) {
    numbers.append("My address 949-8945-0");
  }
  for (int i = 0; i < 100; ++i) {
    numbers.append("My info: 415-666-7777,");
  }

  PhoneNumberMatcher matcher(
      phone_util_, numbers, RegionCode::US(), PhoneNumberMatcher::VALID, 10);
  EXPECT_FALSE(matcher.HasNext());
}

TEST_F(PhoneNumberMatcherTest, MaxMatchesMixed) {
  // Set up text with 100 valid numbers inside an invalid number.
  string numbers;
  for (int i = 0; i < 100; ++i) {
    numbers.append("My info: 415-666-7777 123 fake street");
  }

  PhoneNumber number;
  phone_util_.Parse("+14156667777", RegionCode::ZZ(), &number);
  std::vector<PhoneNumber> expected(10, number);

  PhoneNumberMatcher matcher(
      phone_util_, numbers, RegionCode::US(), PhoneNumberMatcher::VALID, 10);
  std::vector<PhoneNumber> actual;
  PhoneNumberMatch match;
  while (matcher.HasNext()) {
    matcher.Next(&match);
    actual.push_back(match.number());
  }
  EXPECT_EQ(expected, actual);
}

TEST_F(PhoneNumberMatcherTest, NonPlusPrefixedNumbersNotFoundForInvalidRegion) {
  PhoneNumberMatch match;
  scoped_ptr<PhoneNumberMatcher> matcher(
      GetMatcherWithLeniency("1 456 764 156", RegionCode::GetUnknown(),
                             PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());
  EXPECT_FALSE(matcher->Next(&match));
  EXPECT_FALSE(matcher->HasNext());
}

TEST_F(PhoneNumberMatcherTest, EmptyIteration) {
  PhoneNumberMatch match;
  scoped_ptr<PhoneNumberMatcher> matcher(
      GetMatcherWithLeniency("", RegionCode::GetUnknown(),
                             PhoneNumberMatcher::VALID));
  EXPECT_FALSE(matcher->HasNext());
  EXPECT_FALSE(matcher->HasNext());
  EXPECT_FALSE(matcher->Next(&match));
  EXPECT_FALSE(matcher->HasNext());
}

TEST_F(PhoneNumberMatcherTest, SingleIteration) {
  PhoneNumberMatch match;
  scoped_ptr<PhoneNumberMatcher> matcher(
      GetMatcherWithLeniency("+14156667777", RegionCode::GetUnknown(),
                             PhoneNumberMatcher::VALID));

  // Try HasNext() twice to ensure it does not advance.
  EXPECT_TRUE(matcher->HasNext());
  EXPECT_TRUE(matcher->HasNext());
  EXPECT_TRUE(matcher->Next(&match));

  EXPECT_FALSE(matcher->HasNext());
  EXPECT_FALSE(matcher->Next(&match));
}

TEST_F(PhoneNumberMatcherTest, SingleIteration_WithNextOnly) {
  PhoneNumberMatch match;
  scoped_ptr<PhoneNumberMatcher> matcher(
      GetMatcherWithLeniency("+14156667777", RegionCode::GetUnknown(),
                             PhoneNumberMatcher::VALID));
  EXPECT_TRUE(matcher->Next(&match));
  EXPECT_FALSE(matcher->Next(&match));
}

TEST_F(PhoneNumberMatcherTest, DoubleIteration) {
  PhoneNumberMatch match;
  scoped_ptr<PhoneNumberMatcher> matcher(
      GetMatcherWithLeniency("+14156667777 foobar +14156667777 ",
                             RegionCode::GetUnknown(),
                             PhoneNumberMatcher::VALID));

  // Double HasNext() to ensure it does not advance.
  EXPECT_TRUE(matcher->HasNext());
  EXPECT_TRUE(matcher->HasNext());
  EXPECT_TRUE(matcher->Next(&match));
  EXPECT_TRUE(matcher->HasNext());
  EXPECT_TRUE(matcher->HasNext());
  EXPECT_TRUE(matcher->Next(&match));

  EXPECT_FALSE(matcher->HasNext());
  EXPECT_FALSE(matcher->Next(&match));
  EXPECT_FALSE(matcher->HasNext());
}

TEST_F(PhoneNumberMatcherTest, DoubleIteration_WithNextOnly) {
  PhoneNumberMatch match;
  scoped_ptr<PhoneNumberMatcher> matcher(
      GetMatcherWithLeniency("+14156667777 foobar +14156667777 ",
                             RegionCode::GetUnknown(),
                             PhoneNumberMatcher::VALID));

  EXPECT_TRUE(matcher->Next(&match));
  EXPECT_TRUE(matcher->Next(&match));
  EXPECT_FALSE(matcher->Next(&match));
}

}  // namespace phonenumbers
}  // namespace i18n
