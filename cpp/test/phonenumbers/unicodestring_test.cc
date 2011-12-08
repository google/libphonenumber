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

#include <iostream>

#include <gtest/gtest.h>

#include "phonenumbers/unicodestring.h"

using std::ostream;

namespace i18n {
namespace phonenumbers {

// Used by GTest to print the expected and actual results in case of failure.
ostream& operator<<(ostream& out, const UnicodeString& s) {
  string utf8;
  s.toUTF8String(utf8);
  out << utf8;
  return out;
}

TEST(UnicodeString, ToUTF8StringWithEmptyString) {
  UnicodeString s;
  string utf8;
  s.toUTF8String(utf8);
  EXPECT_EQ("", utf8);
}

TEST(UnicodeString, ToUTF8String) {
  UnicodeString s("hello");
  string utf8;
  s.toUTF8String(utf8);
  EXPECT_EQ("hello", utf8);
}

TEST(UnicodeString, ToUTF8StringWithNonAscii) {
  UnicodeString s("\xEF\xBC\x95\xEF\xBC\x93" /* "５３" */);
  string utf8;
  s.toUTF8String(utf8);
  EXPECT_EQ("\xEF\xBC\x95\xEF\xBC\x93", utf8);
}

TEST(UnicodeString, AppendCodepoint) {
  UnicodeString s;
  s.append('h');
  ASSERT_EQ(UnicodeString("h"), s);
  s.append('e');
  EXPECT_EQ(UnicodeString("he"), s);
}

TEST(UnicodeString, AppendCodepointWithNonAscii) {
  UnicodeString s;
  s.append(0xFF15 /* ５ */);
  ASSERT_EQ(UnicodeString("\xEF\xBC\x95" /* ５ */), s);
  s.append(0xFF13 /* ３ */);
  EXPECT_EQ(UnicodeString("\xEF\xBC\x95\xEF\xBC\x93" /* ５３ */), s);
}

TEST(UnicodeString, AppendUnicodeString) {
  UnicodeString s;
  s.append(UnicodeString("he"));
  ASSERT_EQ(UnicodeString("he"), s);
  s.append(UnicodeString("llo"));
  EXPECT_EQ(UnicodeString("hello"), s);
}

TEST(UnicodeString, AppendUnicodeStringWithNonAscii) {
  UnicodeString s;
  s.append(UnicodeString("\xEF\xBC\x95" /* ５ */));
  ASSERT_EQ(UnicodeString("\xEF\xBC\x95"), s);
  s.append(UnicodeString("\xEF\xBC\x93" /* ３ */));
  EXPECT_EQ(UnicodeString("\xEF\xBC\x95\xEF\xBC\x93" /* ５３ */), s);
}

TEST(UnicodeString, IndexOf) {
  UnicodeString s("hello");
  EXPECT_EQ(0, s.indexOf('h'));
  EXPECT_EQ(2, s.indexOf('l'));
  EXPECT_EQ(4, s.indexOf('o'));
}

TEST(UnicodeString, IndexOfWithNonAscii) {
  UnicodeString s("\xEF\xBC\x95\xEF\xBC\x93" /* ５３ */);
  EXPECT_EQ(1, s.indexOf(0xFF13 /* ３ */));
}

TEST(UnicodeString, ReplaceWithEmptyInputs) {
  UnicodeString s;
  s.replace(0, 0, UnicodeString(""));
  EXPECT_EQ(UnicodeString(""), s);
}

TEST(UnicodeString, ReplaceWithEmptyReplacement) {
  UnicodeString s("hello");
  s.replace(0, 5, UnicodeString(""));
  EXPECT_EQ(UnicodeString(""), s);
}

TEST(UnicodeString, ReplaceBegining) {
  UnicodeString s("hello world");
  s.replace(0, 5, UnicodeString("HELLO"));
  EXPECT_EQ(UnicodeString("HELLO world"), s);
}

TEST(UnicodeString, ReplaceMiddle) {
  UnicodeString s("hello world");
  s.replace(5, 1, UnicodeString("AB"));
  EXPECT_EQ(UnicodeString("helloABworld"), s);
}

TEST(UnicodeString, ReplaceEnd) {
  UnicodeString s("hello world");
  s.replace(10, 1, UnicodeString("AB"));
  EXPECT_EQ(UnicodeString("hello worlAB"), s);
}

TEST(UnicodeString, ReplaceWithNonAscii) {
  UnicodeString s("hello world");
  s.replace(3, 2, UnicodeString("\xEF\xBC\x91\xEF\xBC\x90" /* １０ */));
  EXPECT_EQ(UnicodeString("hel\xEF\xBC\x91\xEF\xBC\x90 world"), s);
}

TEST(UnicodeString, SetCharBegining) {
  UnicodeString s("hello");
  s.setCharAt(0, 'H');
  EXPECT_EQ(UnicodeString("Hello"), s);
}

TEST(UnicodeString, SetCharMiddle) {
  UnicodeString s("hello");
  s.setCharAt(2, 'L');
  EXPECT_EQ(UnicodeString("heLlo"), s);
}

TEST(UnicodeString, SetCharEnd) {
  UnicodeString s("hello");
  s.setCharAt(4, 'O');
  EXPECT_EQ(UnicodeString("hellO"), s);
}

TEST(UnicodeString, SetCharWithNonAscii) {
  UnicodeString s("hello");
  s.setCharAt(4, 0xFF10 /* ０ */);
  EXPECT_EQ(UnicodeString("hell\xEF\xBC\x90" /* ０ */), s);
}

TEST(UnicodeString, TempSubStringWithEmptyString) {
  EXPECT_EQ(UnicodeString(""), UnicodeString().tempSubString(0, 0));
}

TEST(UnicodeString, TempSubStringWithInvalidInputs) {
  UnicodeString s("hello");
  // tempSubString() returns an empty unicode string if one of the provided
  // paramaters is out of range.
  EXPECT_EQ(UnicodeString(""), s.tempSubString(6));
  EXPECT_EQ(UnicodeString(""), s.tempSubString(2, 6));
}

TEST(UnicodeString, TempSubString) {
  UnicodeString s("hello");
  EXPECT_EQ(UnicodeString(""), s.tempSubString(0, 0));
  EXPECT_EQ(UnicodeString("h"), s.tempSubString(0, 1));
  EXPECT_EQ(UnicodeString("hello"), s.tempSubString(0, 5));
  EXPECT_EQ(UnicodeString("llo"), s.tempSubString(2, 3));
}

TEST(UnicodeString, TempSubStringWithNoLength) {
  UnicodeString s("hello");
  EXPECT_EQ(UnicodeString("hello"), s.tempSubString(0));
  EXPECT_EQ(UnicodeString("llo"), s.tempSubString(2));
}

TEST(UnicodeString, TempSubStringWithNonAscii) {
  UnicodeString s("hel\xEF\xBC\x91\xEF\xBC\x90" /* １０ */);
  EXPECT_EQ(UnicodeString("\xEF\xBC\x91" /* １ */), s.tempSubString(3, 1));
}

TEST(UnicodeString, OperatorEqual) {
  UnicodeString s("hello");
  s = UnicodeString("Hello");
  EXPECT_EQ(UnicodeString("Hello"), s);
}

TEST(UnicodeString, OperatorEqualWithNonAscii) {
  UnicodeString s("hello");
  s = UnicodeString("hel\xEF\xBC\x91\xEF\xBC\x90" /* １０ */);
  EXPECT_EQ(UnicodeString("hel\xEF\xBC\x91\xEF\xBC\x90"), s);
}

TEST(UnicodeString, OperatorBracket) {
  UnicodeString s("hello");
  EXPECT_EQ('h', s[0]);
  EXPECT_EQ('e', s[1]);
  EXPECT_EQ('l', s[2]);
  EXPECT_EQ('l', s[3]);
  EXPECT_EQ('o', s[4]);
}

TEST(UnicodeString, OperatorBracketWithNonAscii) {
  UnicodeString s("hel\xEF\xBC\x91\xEF\xBC\x90" /* １０ */);
  EXPECT_EQ('h', s[0]);
  EXPECT_EQ('e', s[1]);
  EXPECT_EQ('l', s[2]);
  EXPECT_EQ(0xFF11 /* １ */, s[3]);
  EXPECT_EQ(0xFF10 /* ０ */, s[4]);
}

TEST(UnicodeString, OperatorBracketWithIteratorCacheInvalidation) {
  UnicodeString s("hello");
  EXPECT_EQ('h', s[0]);
  EXPECT_EQ('e', s[1]);
  // Modify the string which should invalidate the iterator cache.
  s.setCharAt(1, 'E');
  EXPECT_EQ(UnicodeString("hEllo"), s);
  EXPECT_EQ('E', s[1]);
  // Get the previous character which should invalidate the iterator cache.
  EXPECT_EQ('h', s[0]);
  EXPECT_EQ('o', s[4]);
}

}  // namespace phonenumbers
}  // namespace i18n
