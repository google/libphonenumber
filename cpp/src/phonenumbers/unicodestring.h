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

#ifndef I18N_PHONENUMBERS_UNICODESTRING_H_
#define I18N_PHONENUMBERS_UNICODESTRING_H_

#include "phonenumbers/utf/unicodetext.h"

#include <cstring>
#include <limits>

namespace i18n {
namespace phonenumbers {

// This class supports the minimal subset of icu::UnicodeString needed by
// AsYouTypeFormatter in order to let the libphonenumber not depend on ICU
// which is not available by default on some systems, such as iOS.
class UnicodeString {
 public:
  UnicodeString() : cached_index_(-1) {}

  // Constructs a new unicode string copying the provided C string.
  explicit UnicodeString(const char* utf8)
      : text_(UTF8ToUnicodeText(utf8, static_cast<int>(std::strlen(utf8)))),
        cached_index_(-1) {}

  // Constructs a new unicode string containing the provided codepoint.
  explicit UnicodeString(char32 codepoint) : cached_index_(-1) {
    append(codepoint);
  }

  UnicodeString(const UnicodeString& src)
      : text_(src.text_), cached_index_(-1) {}

  UnicodeString& operator=(const UnicodeString& src);

  bool operator==(const UnicodeString& rhs) const;

  void append(const UnicodeString& unicode_string);

  inline void append(char32 codepoint) {
    invalidateCachedIndex();
    text_.push_back(codepoint);
  }

  typedef UnicodeText::const_iterator const_iterator;

  inline const_iterator begin() const {
    return text_.begin();
  }

  inline const_iterator end() const {
    return text_.end();
  }

  // Returns the index of the provided codepoint or -1 if not found.
  int indexOf(char32 codepoint) const;

  // Returns the number of codepoints contained in the unicode string.
  inline int length() const {
    return text_.size();
  }

  // Clears the unicode string.
  inline void remove() {
    invalidateCachedIndex();
    text_.clear();
  }

  // Replaces the substring located at [ start, start + length - 1 ] with the
  // provided unicode string.
  void replace(int start, int length, const UnicodeString& src);

  void setCharAt(int pos, char32 c);

  // Copies the provided C string.
  inline void setTo(const char* s, size_t len) {
    invalidateCachedIndex();
    text_.CopyUTF8(s, static_cast<int>(len));
  }

  // Was this UnicodeString created from valid UTF-8?
  bool UTF8WasValid() const { return text_.UTF8WasValid(); }

  // Returns the substring located at [ start, start + length - 1 ] without
  // copying the underlying C string. If one of the provided parameters is out
  // of range, the function returns an empty unicode string.
  UnicodeString tempSubString(
      int start,
      int length = std::numeric_limits<int>::max()) const;

  inline void toUTF8String(string& out) const {
    out = UnicodeTextToUTF8(text_);
  }

  char32 operator[](int index) const;

 private:
  UnicodeText text_;

  // As UnicodeText doesn't provide random access, an operator[] implementation
  // would naively iterate from the beginning of the string to the supplied
  // index which would be inefficient.
  // As operator[] is very likely to be called in a loop with consecutive
  // indexes, we save the corresponding iterator so we can reuse it the next
  // time it is called.

  // The following function which invalidates the cached index corresponding to
  // the iterator position must be called every time the unicode string is
  // modified (i.e. in all the non-const methods).
  inline void invalidateCachedIndex() {
    cached_index_ = -1;
  }

  // Iterator corresponding to the cached index below, used by operator[].
  mutable UnicodeText::const_iterator cached_it_;
  mutable int cached_index_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_UNICODESTRING_H_
