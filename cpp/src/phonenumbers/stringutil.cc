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

#include "absl/strings/str_replace.h"
#include "absl/strings/substitute.h"
#include "absl/strings/match.h"
#include "absl/strings/str_cat.h"

namespace i18n {
namespace phonenumbers {

using std::equal;
using std::stringstream;

string operator+(const string& s, int n) {  // NOLINT(runtime/string)
  string result;
  absl::StrAppend(&result,s,n);
  return result;
}

string SimpleItoa(int n) {
  return absl::StrCat(n);
}

string SimpleItoa(uint64 n) {
  return absl::StrCat(n);
}

string SimpleItoa(int64 n) {
  return absl::StrCat(n);
}

bool HasPrefixString(const string& s, const string& prefix) {
  return absl::StartsWith(s, prefix);
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
  return absl::EndsWith(s, suffix);
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
  return absl::StrReplaceAll({{substring, replacement}}, s);;
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

string StrCat(absl::string_view s1, absl::string_view s2) {
  return absl::StrCat(s1, s2);
}

string StrCat(absl::string_view s1, absl::string_view s2,
              absl::string_view s3) {
  return absl::StrCat(s1, s2, s3);
}

string StrCat(absl::string_view s1, absl::string_view s2,
              absl::string_view s3, absl::string_view s4) {
  return absl::StrCat(s1, s2, s3, s4);
}

string StrCat(absl::string_view s1, absl::string_view s2,
              absl::string_view s3, absl::string_view s4,
              absl::string_view s5) {
  return absl::StrCat(s1, s2, s3, s4, s5);
}

template<typename... args>
string StrCat(absl::string_view s1, absl::string_view s2,
              absl::string_view s3, absl::string_view s4,
              absl::string_view s5, args... s6) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6...);
}

// StrAppend

void StrAppend(string* dest, absl::string_view s1) {
  absl::StrAppend(dest, s1);
}

void StrAppend(string* dest, absl::string_view s1, absl::string_view s2) {
  absl::StrAppend(dest, s1, s2);
}

void StrAppend(string* dest, absl::string_view s1, absl::string_view s2,
               absl::string_view s3) {
  absl::StrAppend(dest, s1, s2, s3);
}

void StrAppend(string* dest, absl::string_view s1, absl::string_view s2,
               absl::string_view s3, absl::string_view s4) {
  absl::StrAppend(dest, s1, s2, s3, s4);
}

void StrAppend(string* dest, absl::string_view s1, absl::string_view s2,
               absl::string_view s3, absl::string_view s4,
               absl::string_view s5) {
  absl::StrAppend(dest, s1, s2, s3, s4, s5);
}

}  // namespace phonenumbers
}  // namespace i18n
