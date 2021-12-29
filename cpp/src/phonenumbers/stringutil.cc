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

#include <stdint.h>
#include <algorithm>
#include <cassert>
#include <cstring>
#include <sstream>
#include <array>

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

/* begin code from 
https://github.com/jeaiii/itoa/blob/main/itoa/itoa_jeaiii.cpp
*/
/*
MIT License

Copyright (c) 2017 James Edward Anhalt III - https://github.com/jeaiii/itoa

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

#include <stdint.h>

// form a 4.32 fixed point number: t = u * 2^32 / 10^log10(u)
// use as much precision as possible when needed (log10(u) >= 5) 
// so shift up then down afterwards by log10(u) * log2(10) ~= 53/16
// need to round up before and or after in some cases
// once we have the fixed point number we can read off the digit in the upper 32 bits
// and multiply the lower 32 bits by 10 to get the next digit and so on
// we can do 2 digits at a time by multiplying by 100 each time

// TODO:
// x64 optimized verison (no need to line up on 32bit boundary, so can multiply by 5 instead of 10 using lea instruction)
// full 64 bit LG()
// try splitting the number into chucks that can be processed independently
// try odd digit first
// try writing 4 chars at a time

#if 0
// 1 char at a time

#define W(N, I) b[N] = char(I) + '0'
#define A(N) t = (uint64_t(1) << (32 + N / 5 * N * 53 / 16)) / uint32_t(1e##N) + 1 - N / 9, t *= u, t >>= N / 5 * N * 53 / 16, t += N / 5 * 4, W(0, t >> 32)
#define D(N) t = uint64_t(10) * uint32_t(t), W(N, t >> 32)

#define L0 W(0, u)
#define L1 A(1), D(1)
#define L2 A(2), D(1), D(2)
#define L3 A(3), D(1), D(2), D(3)
#define L4 A(4), D(1), D(2), D(3), D(4)
#define L5 A(5), D(1), D(2), D(3), D(4), D(5)
#define L6 A(6), D(1), D(2), D(3), D(4), D(5), D(6)
#define L7 A(7), D(1), D(2), D(3), D(4), D(5), D(6), D(7)
#define L8 A(8), D(1), D(2), D(3), D(4), D(5), D(6), D(7), D(8)
#define L9 A(9), D(1), D(2), D(3), D(4), D(5), D(6), D(7), D(8), D(9)

#else
// 2 chars at a time

struct pair { char t, o; };
#define P(T) T, '0',  T, '1', T, '2', T, '3', T, '4', T, '5', T, '6', T, '7', T, '8', T, '9'
static const pair s_pairs[] = { P('0'), P('1'), P('2'), P('3'), P('4'), P('5'), P('6'), P('7'), P('8'), P('9') };

#define W(N, I) *(pair*)&b[N] = s_pairs[I]
#define A(N) t = (uint64_t(1) << (32 + N / 5 * N * 53 / 16)) / uint32_t(1e##N) + 1 + N/6 - N/8, t *= u, t >>= N / 5 * N * 53 / 16, t += N / 6 * 4, W(0, t >> 32)
#define S(N) b[N] = char(uint64_t(10) * uint32_t(t) >> 32) + '0'
#define D(N) t = uint64_t(100) * uint32_t(t), W(N, t >> 32)

#define L0 b[0] = char(u) + '0'
#define L1 W(0, u)
#define L2 A(1), S(2)
#define L3 A(2), D(2)
#define L4 A(3), D(2), S(4)
#define L5 A(4), D(2), D(4)
#define L6 A(5), D(2), D(4), S(6)
#define L7 A(6), D(2), D(4), D(6)
#define L8 A(7), D(2), D(4), D(6), S(8)
#define L9 A(8), D(2), D(4), D(6), D(8)

#endif

#define LN(N) (L##N, b += N + 1)
#define LZ LN
// if you want to '\0' terminate
//#define LZ(N) &(L##N, b[N + 1] = '\0')

#define LG(F) (u<100 ? u<10 ? F(0) : F(1) : u<1000000 ? u<10000 ? u<1000 ? F(2) : F(3) : u<100000 ? F(4) : F(5) : u<100000000 ? u<10000000 ? F(6) : F(7) : u<1000000000 ? F(8) : F(9))

inline char* u32toa_jeaiii(uint32_t u, char* b)
{
    uint64_t t;
    return LG(LZ);
}

inline char* i32toa_jeaiii(int32_t i, char* b)
{
    uint32_t u = i < 0 ? *b++ = '-', 0 - uint32_t(i) : i;
    uint64_t t;
    return LG(LZ);
}

inline char* u64toa_jeaiii(uint64_t n, char* b)
{
    uint32_t u;
    uint64_t t;

    if (uint32_t(n >> 32) == 0)
        return u = uint32_t(n), LG(LZ);

    uint64_t a = n / 100000000;

    if (uint32_t(a >> 32) == 0)
    {
        u = uint32_t(a);
        LG(LN);
    }
    else
    {
        u = uint32_t(a / 100000000);
        LG(LN);
        u = a % 100000000;
        LN(7);
    }

    u = n % 100000000;
    return LZ(7);
}

inline char* i64toa_jeaiii(int64_t i, char* b)
{
    uint64_t n = i < 0 ? *b++ = '-', 0 - uint64_t(i) : i;
    return u64toa_jeaiii(n, b);
}

/* end code from 
https://github.com/jeaiii/itoa/blob/main/itoa/itoa_jeaiii.cpp
*/

string SimpleItoa(int n) {
  std::array<char, 0x18> str;
  char* end_number = i64toa_jeaiii(n,str.data());
  return string(str.data(), end_number - str.data());
}

string SimpleItoa(uint64 n) {
  std::array<char, 0x18> str;
  char* end_number = u64toa_jeaiii(n,str.data());
  return string(str.data(), end_number - str.data());
}

string SimpleItoa(int64 n) {
  std::array<char, 0x18> str;
  char* end_number = i64toa_jeaiii(n,str.data());
  return string(str.data(), end_number - str.data());
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
       pos = static_cast<int>(match_pos + substring.length()),
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
