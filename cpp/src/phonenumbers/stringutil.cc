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

#include <algorithm>
#include <cassert>
#include <sstream>

#include "absl/strings/match.h"
#include "absl/strings/str_replace.h"
#include "absl/strings/substitute.h"

namespace i18n {
namespace phonenumbers {

using std::equal;
using std::stringstream;

string operator+(const string &s, int n) {  // NOLINT(runtime/string)
  string result;
  absl::StrAppend(&result, s, n);
  return result;
}

string SimpleItoa(int n) { return absl::StrCat(n); }

string SimpleItoa(uint64 n) { return absl::StrCat(n); }

string SimpleItoa(int64 n) { return absl::StrCat(n); }

bool HasPrefixString(const string &s, const string &prefix) {
  return absl::StartsWith(s, prefix);
}

size_t FindNth(const string &s, char c, int n) {
  auto it = std::find_if(s.begin(), s.end(),
                         [&](char val) { return val == c && --n == 0; });
  if (it != s.end()) {
    return std::distance(s.begin(), it);
  }
  return std::string::npos;
}

void SplitStringUsing(const string &s, char delimiter, vector<string> *result) {
  assert(result);
  for (absl::string_view split_piece :
       absl::StrSplit(s, absl::ByChar(delimiter), absl::SkipEmpty())) {
    result->emplace_back(split_piece);
  }
}

bool TryStripPrefixString(const string &in, const string &prefix, string *out) {
  assert(out);
  const bool has_prefix = in.compare(0, prefix.length(), prefix) == 0;
  out->assign(has_prefix ? in.substr(prefix.length()) : in);

  return has_prefix;
}

bool HasSuffixString(const string &s, const string &suffix) {
  return absl::EndsWith(s, suffix);
}

template <typename T>
void GenericAtoi(const string &s, T *out) {
  if (!absl::SimpleAtoi(s, out)) *out = 0;
}

void safe_strto32(const string &s, int32 *n) { GenericAtoi(s, n); }

void safe_strtou64(const string &s, uint64 *n) { GenericAtoi(s, n); }

void safe_strto64(const string &s, int64 *n) { GenericAtoi(s, n); }

void strrmm(string *s, const string &chars) {
  s->erase(std::remove_if(s->begin(), s->end(),
                          [&chars](char current_char) {
                            return chars.find(current_char) !=
                                   std::string::npos;
                          }),
           s->end());
}

int GlobalReplaceSubstring(const string &substring, const string &replacement,
                           string *s) {
  return absl::StrReplaceAll({{substring, replacement}}, s);
  ;
}

// StringHolder class
StringHolder::StringHolder(const string &s) : absl::AlphaNum(s) {}

StringHolder::StringHolder(const char *cp) : absl::AlphaNum(cp) {}

StringHolder::StringHolder(uint64 n) : absl::AlphaNum(n) {}

// StrCat

// Implements s += sh; (s: string, sh: StringHolder)
string &operator+=(string &lhs, const StringHolder &rhs) {
  absl::string_view s = rhs.GetString();
  ;
  if (!s.empty()) {
    lhs += s.data();
  } else {
    const char *const cs = rhs.GetCString();
    if (cs) lhs.append(cs, rhs.Length());
  }
  return lhs;
}

}  // namespace phonenumbers
}  // namespace i18n