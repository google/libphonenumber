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

#include <algorithm>
#include <cassert>
#include <cstring>
#include <sstream>

#include "phonenumbers/stringutil.h"

namespace i18n {
namespace phonenumbers {

using std::equal;
using std::stringstream;

string operator+(const string& s, int n) {  // NOLINT(runtime/string)
  stringstream stream;

  stream << s << n;
  string result;
  stream >> result;

  return result;
}

template <typename T>
string GenericSimpleItoa(const T& n) {
  stringstream stream;

  stream << n;
  string result;
  stream >> result;

  return result;
}

string SimpleItoa(int n) {
  return GenericSimpleItoa(n);
}

string SimpleItoa(uint64 n) {
  return GenericSimpleItoa(n);
}

string SimpleItoa(int64 n) {
  return GenericSimpleItoa(n);
}

bool HasPrefixString(const string& s, const string& prefix) {
  return s.size() >= prefix.size() &&
      equal(s.begin(), s.begin() + prefix.size(), prefix.begin());
}

size_t FindNth(const string& s, char c, int n) {
  size_t pos = string::npos;

  for (int i = 0; i < n; ++i) {
    pos = s.find_first_of(c, pos + 1);
    if (pos == string::npos) {
      break;
    }
  }
  return pos;
}

void SplitStringUsing(const string& s, const string& delimiter,
                      vector<string>* result) {
  assert(result);
  size_t start_pos = 0;
  size_t find_pos = string::npos;
  if (delimiter.empty()) {
    return;
  }
  while ((find_pos = s.find(delimiter, start_pos)) != string::npos) {
    const string substring = s.substr(start_pos, find_pos - start_pos);
    if (!substring.empty()) {
      result->push_back(substring);
    }
    start_pos = find_pos + delimiter.length();
  }
  if (start_pos != s.length()) {
    result->push_back(s.substr(start_pos));
  }
}

void StripString(string* s, const char* remove, char replacewith) {
  const char* str_start = s->c_str();
  const char* str = str_start;
  for (str = strpbrk(str, remove);
       str != NULL;
       str = strpbrk(str + 1, remove)) {
    (*s)[str - str_start] = replacewith;
  }
}

bool TryStripPrefixString(const string& in, const string& prefix, string* out) {
  assert(out);
  const bool has_prefix = in.compare(0, prefix.length(), prefix) == 0;
  out->assign(has_prefix ? in.substr(prefix.length()) : in);

  return has_prefix;
}

bool HasSuffixString(const string& s, const string& suffix) {
  if (s.length() < suffix.length()) {
    return false;
  }
  return s.compare(s.length() - suffix.length(), suffix.length(), suffix) == 0;
}

template <typename T>
void GenericAtoi(const string& s, T* out) {
  stringstream stream;
  stream << s;
  stream >> *out;
}

void safe_strto32(const string& s, int32 *n) {
  GenericAtoi(s, n);
}

void safe_strtou64(const string& s, uint64 *n) {
  GenericAtoi(s, n);
}

void safe_strto64(const string& s, int64* n) {
  GenericAtoi(s, n);
}

void strrmm(string* s, const string& chars) {
  for (string::iterator it = s->begin(); it != s->end(); ) {
    const char current_char = *it;
    if (chars.find(current_char) != string::npos) {
      it = s->erase(it);
    } else {
      ++it;
    }
  }
}

int GlobalReplaceSubstring(const string& substring,
                           const string& replacement,
                           string* s) {
  assert(s != NULL);
  if (s->empty() || substring.empty())
    return 0;
  string tmp;
  int num_replacements = 0;
  int pos = 0;
  for (size_t match_pos = s->find(substring.data(), pos, substring.length());
       match_pos != string::npos;
       pos = match_pos + substring.length(),
          match_pos = s->find(substring.data(), pos, substring.length())) {
    ++num_replacements;
    // Append the original content before the match.
    tmp.append(*s, pos, match_pos - pos);
    // Append the replacement for the match.
    tmp.append(replacement.begin(), replacement.end());
  }
  // Append the content after the last match.
  tmp.append(*s, pos, s->length() - pos);
  s->swap(tmp);
  return num_replacements;
}

// StringHolder class

StringHolder::StringHolder(const string& s)
  : string_(&s),
    cstring_(NULL),
    len_(s.size())
{}

StringHolder::StringHolder(const char* s)
  : string_(NULL),
    cstring_(s),
    len_(std::strlen(s))
{}

StringHolder::StringHolder(uint64 n)
  : converted_string_(SimpleItoa(n)),
    string_(&converted_string_),
    cstring_(NULL),
    len_(converted_string_.length())
{}

StringHolder::~StringHolder() {}

// StrCat

// Implements s += sh; (s: string, sh: StringHolder)
string& operator+=(string& lhs, const StringHolder& rhs) {
  const string* const s = rhs.GetString();
  if (s) {
    lhs += *s;
  } else {
    const char* const cs = rhs.GetCString();
    if (cs)
      lhs.append(cs, rhs.Length());
  }
  return lhs;
}

string StrCat(const StringHolder& s1, const StringHolder& s2) {
  string result;
  result.reserve(s1.Length() + s2.Length() + 1);

  result += s1;
  result += s2;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3) {
  string result;
  result.reserve(s1.Length() + s2.Length() + s3.Length() + 1);

  result += s1;
  result += s2;
  result += s3;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4) {
  string result;
  result.reserve(s1.Length() + s2.Length() + s3.Length() + s4.Length() + 1);

  result += s1;
  result += s2;
  result += s3;
  result += s4;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5) {
  string result;
  result.reserve(s1.Length() + s2.Length() + s3.Length() + s4.Length() +
                 s5.Length() + 1);
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6) {
  string result;
  result.reserve(s1.Length() + s2.Length() + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length() + 1);
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7) {
  string result;
  result.reserve(s1.Length() + s2.Length() + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length() + s7.Length() + 1);
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8) {
  string result;
  result.reserve(s1.Length() + s2.Length() + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length() + s7.Length() + s8.Length() + 1);
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;
  result += s8;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9) {
  string result;
  result.reserve(s1.Length() + s2.Length() + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length() + s7.Length() + s8.Length() +
                 s9.Length() + 1);
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;
  result += s8;
  result += s9;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11) {
  string result;
  result.reserve(s1.Length() + s2.Length()  + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length()  + s7.Length() + s8.Length() +
                 s9.Length() + s10.Length() + s11.Length());
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;
  result += s8;
  result += s9;
  result += s10;
  result += s11;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12) {
  string result;
  result.reserve(s1.Length() + s2.Length()  + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length()  + s7.Length() + s8.Length() +
                 s9.Length() + s10.Length() + s11.Length() + s12.Length());
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;
  result += s8;
  result += s9;
  result += s10;
  result += s11;
  result += s12;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13) {
  string result;
  result.reserve(s1.Length() + s2.Length()  + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length()  + s7.Length() + s8.Length() +
                 s9.Length() + s10.Length() + s11.Length() + s12.Length() +
                 s13.Length());
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;
  result += s8;
  result += s9;
  result += s10;
  result += s11;
  result += s12;
  result += s13;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14) {
  string result;
  result.reserve(s1.Length() + s2.Length()  + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length()  + s7.Length() + s8.Length() +
                 s9.Length() + s10.Length() + s11.Length() + s12.Length() +
                 s13.Length() + s14.Length());
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;
  result += s8;
  result += s9;
  result += s10;
  result += s11;
  result += s12;
  result += s13;
  result += s14;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14,
              const StringHolder& s15) {
  string result;
  result.reserve(s1.Length() + s2.Length()  + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length()  + s7.Length() + s8.Length() +
                 s9.Length() + s10.Length() + s11.Length() + s12.Length() +
                 s13.Length() + s14.Length() + s15.Length());
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;
  result += s8;
  result += s9;
  result += s10;
  result += s11;
  result += s12;
  result += s13;
  result += s14;
  result += s15;

  return result;
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14,
              const StringHolder& s15, const StringHolder& s16) {
  string result;
  result.reserve(s1.Length() + s2.Length()  + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length()  + s7.Length() + s8.Length() +
                 s9.Length() + s10.Length() + s11.Length() + s12.Length() +
                 s13.Length() + s14.Length() + s15.Length() + s16.Length());
  result += s1;
  result += s2;
  result += s3;
  result += s4;
  result += s5;
  result += s6;
  result += s7;
  result += s8;
  result += s9;
  result += s10;
  result += s11;
  result += s12;
  result += s13;
  result += s14;
  result += s15;
  result += s16;

  return result;
}

// StrAppend

void StrAppend(string* dest, const StringHolder& s1) {
  assert(dest);

  dest->reserve(dest->length() + s1.Length() + 1);
  *dest += s1;
}

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2) {
  assert(dest);

  dest->reserve(dest->length() + s1.Length() + s2.Length() + 1);
  *dest += s1;
  *dest += s2;
}

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3) {
  assert(dest);

  dest->reserve(dest->length() + s1.Length() + s2.Length() + s3.Length() + 1);
  *dest += s1;
  *dest += s2;
  *dest += s3;
}

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3, const StringHolder& s4) {
  assert(dest);

  dest->reserve(dest->length() + s1.Length() + s2.Length() + s3.Length() +
      s4.Length() + 1);
  *dest += s1;
  *dest += s2;
  *dest += s3;
  *dest += s4;
}

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3, const StringHolder& s4,
               const StringHolder& s5) {
  assert(dest);

  dest->reserve(dest->length() + s1.Length() + s2.Length() + s3.Length() +
      s4.Length() + s5.Length() + 1);
  *dest += s1;
  *dest += s2;
  *dest += s3;
  *dest += s4;
  *dest += s5;
}

}  // namespace phonenumbers
}  // namespace i18n
