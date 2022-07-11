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

void SplitStringUsing(const string& s, char delimiter,
                      vector<string>* result) {
  assert(result);
  for (absl::string_view split_piece : absl::StrSplit(
           s, absl::ByChar(delimiter), absl::SkipEmpty())) {
    result->push_back(std::string(split_piece));
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
  if (!absl::SimpleAtoi(s, out))
    *out = 0;
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
  : absl::AlphaNum(s)
{}

StringHolder::StringHolder(const char* cp)
  : absl::AlphaNum(cp)
{}

StringHolder::StringHolder(uint64 n)
  : absl::AlphaNum(n)
{}

StringHolder::~StringHolder() {}

// StrCat

// Implements s += sh; (s: string, sh: StringHolder)
string& operator+=(string& lhs, const StringHolder& rhs) {
  absl::string_view s = rhs.GetString();;
  if (s.size() != 0) {
    lhs += s.data();
  } else {
    const char* const cs = rhs.GetCString();
    if (cs)
      lhs.append(cs, rhs.Length());
  }
  return lhs;
}

string StrCat(const StringHolder& s1, const StringHolder& s2) {
  return absl::StrCat(s1, s2);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3) {
  return absl::StrCat(s1, s2, s3);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4) {
  return absl::StrCat(s1, s2, s3, s4);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5) {
  return absl::StrCat(s1, s2, s3, s4, s5);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8) {
  string result;
  result.reserve(s1.Length() + s2.Length() + s3.Length() + s4.Length() +
                 s5.Length() + s6.Length() + s7.Length() + s8.Length() + 1);
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7, s8);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7, s8, s9);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12,
                      s13);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12,
                      s13, s14);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14,
              const StringHolder& s15) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12,
                      s13, s14, s15);
}

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14,
              const StringHolder& s15, const StringHolder& s16) {
  return absl::StrCat(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12,
                      s13, s14, s15, s16);
}

// StrAppend

void StrAppend(string* dest, const StringHolder& s1) {
  absl::StrAppend(dest, s1);
}

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2) {
  absl::StrAppend(dest, s1, s2);
}

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3) {
  absl::StrAppend(dest, s1, s2, s3);
}

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3, const StringHolder& s4) {
  absl::StrAppend(dest, s1, s2, s3, s4);
}

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3, const StringHolder& s4,
               const StringHolder& s5) {
  absl::StrAppend(dest, s1, s2, s3, s4, s5);
}

}  // namespace phonenumbers
}  // namespace i18n
