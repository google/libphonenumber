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

#include "phonenumbers/unicodestring.h"

#include <algorithm>
#include <cassert>
#include <iterator>

using std::advance;
using std::equal;

namespace i18n {
namespace phonenumbers {

UnicodeString& UnicodeString::operator=(const UnicodeString& src) {
  if (&src != this) {
    invalidateCachedIndex();
    text_ = src.text_;
  }
  return *this;
}

bool UnicodeString::operator==(const UnicodeString& rhs) const {
  return equal(text_.begin(), text_.end(), rhs.text_.begin());
}

void UnicodeString::append(const UnicodeString& unicode_string) {
  invalidateCachedIndex();
  for (UnicodeString::const_iterator it = unicode_string.begin();
       it != unicode_string.end(); ++it) {
    append(*it);
  }
}

int UnicodeString::indexOf(char32 codepoint) const {
  int pos = 0;
  for (UnicodeText::const_iterator it = text_.begin(); it != text_.end();
       ++it, ++pos) {
    if (*it == codepoint) {
      return pos;
    }
  }
  return -1;
}

void UnicodeString::replace(int start, int length, const UnicodeString& src) {
  assert(length >= 0 && length <= this->length());
  invalidateCachedIndex();
  UnicodeText::const_iterator start_it = text_.begin();
  advance(start_it, start);
  UnicodeText unicode_text;
  unicode_text.append(text_.begin(), start_it);
  unicode_text.append(src.text_);
  advance(start_it, length);
  unicode_text.append(start_it, text_.end());
  text_ = unicode_text;
}

void UnicodeString::setCharAt(int pos, char32 c) {
  assert(pos < length());
  invalidateCachedIndex();
  UnicodeText::const_iterator pos_it = text_.begin();
  advance(pos_it, pos);
  UnicodeText unicode_text;
  unicode_text.append(text_.begin(), pos_it);
  unicode_text.push_back(c);
  ++pos_it;
  unicode_text.append(pos_it, text_.end());
  text_ = unicode_text;
}

UnicodeString UnicodeString::tempSubString(int start, int length) const {
  const int unicodestring_length = this->length();
  if (length == std::numeric_limits<int>::max()) {
    length = unicodestring_length - start;
  }
  if (start > unicodestring_length || length > unicodestring_length) {
    return UnicodeString("");
  }
  UnicodeText::const_iterator start_it = text_.begin();
  advance(start_it, start);
  UnicodeText::const_iterator end_it = start_it;
  advance(end_it, length);
  UnicodeString substring;
  substring.text_.PointTo(start_it, end_it);
  return substring;
}

char32 UnicodeString::operator[](int index) const {
  assert(index < length());
  if (cached_index_ == -1 || cached_index_ > index) {
    cached_it_ = text_.begin();
    cached_index_ = 0;
  }
  for (; cached_index_ < index; ++cached_index_, ++cached_it_) {}
  return *cached_it_;
}

}  // namespace phonenumbers
}  // namespace i18n
