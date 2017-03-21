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

#ifndef I18N_PHONENUMBERS_STRINGUTIL_H_
#define I18N_PHONENUMBERS_STRINGUTIL_H_

#include <cstddef>
#include <string>
#include <vector>

#include "phonenumbers/base/basictypes.h"

namespace i18n {
namespace phonenumbers {

using std::string;
using std::vector;

// Supports string("hello") + 10.
string operator+(const string& s, int n);  // NOLINT(runtime/string)

// Converts integer to string.
string SimpleItoa(uint64 n);
string SimpleItoa(int64 n);
string SimpleItoa(int n);

// Returns whether the provided string starts with the supplied prefix.
bool HasPrefixString(const string& s, const string& prefix);

// Returns the index of the nth occurence of c in s or string::npos if less than
// n occurrences are present.
size_t FindNth(const string& s, char c, int n);

// Splits a string using a character delimiter. Appends the components to the
// provided vector. Note that empty tokens are ignored.
void SplitStringUsing(const string& s, const string& delimiter,
                      vector<string>* result);

// Replaces any occurrence of the character 'remove' (or the characters
// in 'remove') with the character 'replacewith'.
void StripString(string* s, const char* remove, char replacewith);

// Returns true if 'in' starts with 'prefix' and writes 'in' minus 'prefix' into
// 'out'.
bool TryStripPrefixString(const string& in, const string& prefix, string* out);

// Returns true if 's' ends with 'suffix'.
bool HasSuffixString(const string& s, const string& suffix);

// Converts string to int32.
void safe_strto32(const string& s, int32 *n);

// Converts string to uint64.
void safe_strtou64(const string& s, uint64 *n);

// Converts string to int64.
void safe_strto64(const string& s, int64* n);

// Remove all occurrences of a given set of characters from a string.
void strrmm(string* s, const string& chars);

// Replaces all instances of 'substring' in 's' with 'replacement'. Returns the
// number of instances replaced. Replacements are not subject to re-matching.
int GlobalReplaceSubstring(const string& substring, const string& replacement,
                           string* s);

// Holds a reference to a std::string or C string. It can also be constructed
// from an integer which is converted to a string.
class StringHolder {
 public:
  // Don't make the constructors explicit to make the StrCat usage convenient.
  StringHolder(const string& s);  // NOLINT(runtime/explicit)
  StringHolder(const char* s);    // NOLINT(runtime/explicit)
  StringHolder(uint64 n);         // NOLINT(runtime/explicit)
  ~StringHolder();

  const string* GetString() const {
    return string_;
  }

  const char* GetCString() const {
    return cstring_;
  }

  size_t Length() const {
    return len_;
  }

 private:
  const string converted_string_;  // Stores the string converted from integer.
  const string* const string_;
  const char* const cstring_;
  const size_t len_;
};

string& operator+=(string& lhs, const StringHolder& rhs);

// Efficient string concatenation.

string StrCat(const StringHolder& s1, const StringHolder& s2);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14,
              const StringHolder& s15);

string StrCat(const StringHolder& s1, const StringHolder& s2,
              const StringHolder& s3, const StringHolder& s4,
              const StringHolder& s5, const StringHolder& s6,
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11, const StringHolder& s12,
              const StringHolder& s13, const StringHolder& s14,
              const StringHolder& s15, const StringHolder& s16);

void StrAppend(string* dest, const StringHolder& s1);

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2);

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3);

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3, const StringHolder& s4);

void StrAppend(string* dest, const StringHolder& s1, const StringHolder& s2,
               const StringHolder& s3, const StringHolder& s4,
               const StringHolder& s5);

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_STRINGUTIL_H_
