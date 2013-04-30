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

#ifndef I18N_PHONENUMBERS_ENCODING_UTILS_H_
#define I18N_PHONENUMBERS_ENCODING_UTILS_H_

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/utf/unilib.h"
#include "phonenumbers/utf/utf.h"

namespace i18n {
namespace phonenumbers {

class EncodingUtils {
 public:
  // Decodes one Unicode code-point value from a UTF-8 array. Returns the number
  // of bytes read from the array. If the array does not contain valid UTF-8,
  // the function stores 0xFFFD in the output variable and returns 1.
  static inline int DecodeUTF8Char(const char* in, char32* out) {
    Rune r;
    int len = chartorune(&r, in);
    *out = r;
    return len;
  }

  static const char* AdvanceOneUTF8Character(const char* buf_utf8) {
      return buf_utf8 + UniLib::OneCharLen(buf_utf8);
  }

  static const char* BackUpOneUTF8Character(const char* start,
                                            const char* end) {
    while (start < end && UniLib::IsTrailByte(*--end)) {}
    return end;
  }
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_ENCODING_UTILS_H_
