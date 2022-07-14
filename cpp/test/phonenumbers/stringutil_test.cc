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

// Author: Philippe Liard

#include "phonenumbers/stringutil.h"

#include <string>
#include <vector>

#include <gtest/gtest.h>

using std::string;
using std::vector;

namespace i18n {
namespace phonenumbers {

// Test operator+(const string&, int).
TEST(StringUtilTest, OperatorPlus) {
  EXPECT_EQ("hello10", string("hello") + 10);
}

// Test SimpleItoa implementation.
TEST(StringUtilTest, SimpleItoa) {
  EXPECT_EQ("10", SimpleItoa(10));
}

TEST(StringUtilTest, HasPrefixString) {
  EXPECT_TRUE(HasPrefixString("hello world", "hello"));
  EXPECT_FALSE(HasPrefixString("hello world", "hellO"));
}

TEST(StringUtilTest, FindNthWithEmptyString) {
  EXPECT_EQ(string::npos, FindNth("", 'a', 1));
}

TEST(StringUtilTest, FindNthWithNNegative) {
  EXPECT_EQ(string::npos, FindNth("hello world", 'o', -1));
}

TEST(StringUtilTest, FindNthWithNTooHigh) {
  EXPECT_EQ(string::npos, FindNth("hello world", 'o', 3));
}

TEST(StringUtilTest, FindNth) {
  EXPECT_EQ(7U, FindNth("hello world", 'o', 2));
}

TEST(StringUtilTest, SplitStringUsingWithEmptyString) {
  vector<string> result;
  SplitStringUsing("", ':', &result);
  EXPECT_EQ(0U, result.size());
}

TEST(StringUtilTest, SplitStringUsing) {
  vector<string> result;
  SplitStringUsing(":hello:world:", ':', &result);
  EXPECT_EQ(2U, result.size());
  EXPECT_EQ("hello", result[0]);
  EXPECT_EQ("world", result[1]);
}

TEST(StringUtilTest, SplitStringUsingIgnoresEmptyToken) {
  vector<string> result;
  SplitStringUsing("hello::world", ':', &result);
  EXPECT_EQ(2U, result.size());
  EXPECT_EQ("hello", result[0]);
  EXPECT_EQ("world", result[1]);
}

// Test TryStripPrefixString.
TEST(StringUtilTest, TryStripPrefixString) {
  string s;

  EXPECT_TRUE(TryStripPrefixString("hello world", "hello", &s));
  EXPECT_EQ(" world", s);
  s.clear();

  EXPECT_FALSE(TryStripPrefixString("hello world", "helloa", &s));
  s.clear();

  EXPECT_TRUE(TryStripPrefixString("hello world", "", &s));
  EXPECT_EQ("hello world", s);
  s.clear();

  EXPECT_FALSE(TryStripPrefixString("", "hello", &s));
  s.clear();
}

// Test HasSuffixString.
TEST(StringUtilTest, HasSuffixString) {
  EXPECT_TRUE(HasSuffixString("hello world", "hello world"));
  EXPECT_TRUE(HasSuffixString("hello world", "world"));
  EXPECT_FALSE(HasSuffixString("hello world", "world!"));
  EXPECT_TRUE(HasSuffixString("hello world", ""));
  EXPECT_FALSE(HasSuffixString("", "hello"));
}

// Test safe_strto32.
TEST(StringUtilTest, safe_strto32) {
  int32 n;

  safe_strto32("0", &n);
  EXPECT_EQ(0, n);

  safe_strto32("16", &n);
  EXPECT_EQ(16, n);

  safe_strto32("2147483647", &n);
  EXPECT_EQ(2147483647, n);

  safe_strto32("-2147483648", &n);
  EXPECT_EQ(-2147483648LL, n);
}

// Test safe_strtou64.
TEST(StringUtilTest, safe_strtou64) {
  uint64 n;

  safe_strtou64("0", &n);
  EXPECT_EQ(0U, n);

  safe_strtou64("16", &n);
  EXPECT_EQ(16U, n);

  safe_strtou64("18446744073709551615", &n);
  EXPECT_EQ(18446744073709551615ULL, n);
}

// Test strrmm.
TEST(StringUtilTest, strrmm) {
  string input("hello");

  strrmm(&input, "");
  EXPECT_EQ(input, input);

  string empty;
  strrmm(&empty, "");
  EXPECT_EQ("", empty);

  strrmm(&empty, "aa");
  EXPECT_EQ("", empty);

  strrmm(&input, "h");
  EXPECT_EQ("ello", input);

  strrmm(&input, "el");
  EXPECT_EQ("o", input);
}

// Test GlobalReplaceSubstring.
TEST(StringUtilTest, GlobalReplaceSubstring) {
  string input("hello");

  EXPECT_EQ(0, GlobalReplaceSubstring("aaa", "", &input));
  EXPECT_EQ("hello", input);

  EXPECT_EQ(0, GlobalReplaceSubstring("", "aaa", &input));
  EXPECT_EQ("hello", input);

  EXPECT_EQ(0, GlobalReplaceSubstring("", "", &input));
  EXPECT_EQ("hello", input);

  EXPECT_EQ(0, GlobalReplaceSubstring("aaa", "bbb", &input));
  EXPECT_EQ("hello", input);

  EXPECT_EQ(1, GlobalReplaceSubstring("o", "o world", &input));
  ASSERT_EQ("hello world", input);

  EXPECT_EQ(2, GlobalReplaceSubstring("o", "O", &input));
  EXPECT_EQ("hellO wOrld", input);
}

// Test the StringHolder class.
TEST(StringUtilTest, StringHolder) {
  // Test with C string.
  static const char cstring[] = "aaa";
  StringHolder sh1(cstring);
  EXPECT_EQ(cstring, sh1.GetCString());

  // Test with absl::string_view.
  string s = "aaa";
  StringHolder sh2(s);
  EXPECT_EQ(cstring, sh2.GetString());

  // Test GetLength().
  string s2 = "hello";
  StringHolder sh3(s2);
  EXPECT_EQ(5U, sh3.Length());

  // Test with uint64.
  StringHolder sh4(42);
  static const char cstring2[] = "42";;
  EXPECT_EQ(cstring2, sh4.GetString());
}

// Test the operator+=(string& lhs, const StringHolder& rhs) implementation.
TEST(StringUtilTest, OperatorPlusEquals) {
  // Test with a const char* string to append.
  string s = "h";
  static const char append1[] = "ello";
  s += StringHolder(append1);   // force StringHolder usage

  EXPECT_EQ("hello", s);

  // Test with a std::string to append.
  s = "h";
  string append2 = "ello";
  s += StringHolder(append2);   // force StringHolder usage

  EXPECT_EQ("hello", s);
}

// Test the StrCat implementations
TEST(StringUtilTest, StrCat) {
  string s;

  // Test with 2 arguments.
  s = StrCat("a", "b");
  EXPECT_EQ("ab", s);

  // Test with 3 arguments.
  s = StrCat("a", "b", "c");
  EXPECT_EQ("abc", s);

  // Test with 4 arguments.
  s = StrCat("a", "b", "c", "d");
  EXPECT_EQ("abcd", s);

  // Test with 5 arguments.
  s = StrCat("a", "b", "c", "d", "e");
  EXPECT_EQ("abcde", s);

  // Test with 6 arguments.
  s = StrCat("a", "b", "c", "d", "e", "f");
  EXPECT_EQ("abcdef", s);

  // Test with 7 arguments.
  s = StrCat("a", "b", "c", "d", "e", "f", "g");
  EXPECT_EQ("abcdefg", s);

  // Test with 8 arguments.
  s = StrCat("a", "b", "c", "d", "e", "f", "g", "h");
  EXPECT_EQ("abcdefgh", s);

  // Test with 9 arguments.
  s = StrCat("a", "b", "c", "d", "e", "f", "g", "h", "i");
  EXPECT_EQ("abcdefghi", s);

  // Test with 11 arguments.
  s = StrCat("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
  EXPECT_EQ("abcdefghijk", s);
}

// Test the StrAppend implementations.
TEST(StringUtilTest, StrAppend) {
  string s;

  // Test with 1 argument.
  StrAppend(&s, "a");
  ASSERT_EQ("a", s);

  // Test with 2 arguments.
  StrAppend(&s, "b", "c");
  ASSERT_EQ("abc", s);

  // Test with 3 arguments.
  StrAppend(&s, "d", "e", "f");
  ASSERT_EQ("abcdef", s);

  // Test with 4 arguments.
  StrAppend(&s, "g", "h", "i", "j");
  ASSERT_EQ("abcdefghij", s);

  // Test with 5 arguments.
  StrAppend(&s, "k", "l", "m", "n", "o");
  ASSERT_EQ("abcdefghijklmno", s);

  // Test with int argument.
  StrAppend(&s, 42);
  ASSERT_EQ("abcdefghijklmno42", s);
}

}  // namespace phonenumbers
}  // namespace i18n
