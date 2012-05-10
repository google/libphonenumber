// Copyright (C) 2012 The Libphonenumber Authors
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

// We need this because when ICU is built without std::string support,
// UnicodeString::toUTF8String() is not available. The alternative,
// UnicodeString::toUTF8(), requires an implementation of a string byte sink.
// See unicode/unistr.h and unicode/bytestream.h in ICU for more details.

#include <string>

#include <unicode/unistr.h>

namespace i18n {
namespace phonenumbers {

class StringByteSink : public icu::ByteSink {
 public:
  // Constructs a ByteSink that will append bytes to the dest string.
  explicit StringByteSink(std::string* dest);
  virtual ~StringByteSink();

  virtual void Append(const char* data, int32_t n);

 private:
  std::string* const dest_;
};

}  // namespace phonenumbers
}  // namespace i18n
