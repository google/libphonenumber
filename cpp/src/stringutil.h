// Copyright (C) 2011 Google Inc.
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

namespace i18n {
namespace phonenumbers {

using std::string;

// Support string("hello") + 10
string operator+(const string& s, int n);

// Convert integer into string
string SimpleItoa(int n);

// Return true if 'in' starts with 'prefix' and write into 'out'
// 'in' minus 'prefix'
bool TryStripPrefixString(const string& in, const string& prefix, string* out);

// Return true if 's' ends with 'suffix'
bool HasSuffixString(const string& s, const string& suffix);


// Hold a reference to a std::string or C string.
class StringHolder {
public:
  // Don't make the constructors explicit to make the StrCat usage convenient.
  StringHolder(const string& s);
  StringHolder(const char* s);
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
  const string* const string_;
  const char* const cstring_;
  const size_t len_;
};


string& operator+=(string& lhs, const StringHolder& rhs);


// Efficient string concatenation

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
              const StringHolder& s7, const StringHolder& s8,
              const StringHolder& s9, const StringHolder& s10,
              const StringHolder& s11);

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_STRINGUTIL_H_
