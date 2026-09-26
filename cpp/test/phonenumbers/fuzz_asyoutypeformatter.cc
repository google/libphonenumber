/* Copyright 2025 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
#include "phonenumbers/phonenumbermatcher.h"
#include <string>
#include <vector>
#include <limits>
#include <unicode/unistr.h>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/base/memory/singleton.h"
#include "phonenumbers/default_logger.h"
#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumbermatch.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/stringutil.h"
#include "phonenumbers/asyoutypeformatter.h"
#include "phonenumbers/shortnumberinfo.h"
#include <fuzzer/FuzzedDataProvider.h>

namespace {

// A fixed set of valid ISO-3166-1 alpha-2 region codes spanning different
// numbering plans (NANP, variable-length, leading-zero, etc.). Selecting from
// this set guarantees that AsYouTypeFormatter is constructed with real
// metadata instead of the empty metadata instance, so the formatting state
// machine is actually exercised.
const char* const kRegions[] = {
    "US", "CA", "GB", "DE", "FR", "IN", "BR", "RU", "JP", "CN",
    "AU", "MX", "IT", "ES", "NL", "KR",
};

// A phone-relevant alphabet. Feeding characters from this set (rather than
// arbitrary char32_t code points) keeps InputDigit on the formatting path
// instead of immediately setting able_to_format_ = false on the first
// non-digit, while still letting the fuzzer control the exact sequence,
// length, and position of '+' / '*' / '#' / '(' / ')' / '-' / ' '.
const char kPhoneChars[] = "0123456789+*#() -";

}  // namespace

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  FuzzedDataProvider fuzzed_data(data, size);
  i18n::phonenumbers::PhoneNumberUtil* phone_util =
      i18n::phonenumbers::PhoneNumberUtil::GetInstance();

  // Pick a valid region from the fixed set.
  const char* region = kRegions[fuzzed_data.ConsumeIntegralInRange<uint8_t>(
      0, (sizeof(kRegions) / sizeof(kRegions[0])) - 1)];
  std::unique_ptr<i18n::phonenumbers::AsYouTypeFormatter> formatter(
      phone_util->GetAsYouTypeFormatter(region));

  const int iterations = fuzzed_data.ConsumeIntegralInRange(0, 64);
  std::string result;

  for (int i = 0; i < iterations; ++i) {
    // Feed a character from the phone alphabet so the digit state machine is
    // actually exercised.
    const char32_t next_char = kPhoneChars[fuzzed_data.ConsumeIntegralInRange<uint8_t>(
        0, sizeof(kPhoneChars) - 2)];
    const bool remember = fuzzed_data.ConsumeBool();

    if (remember) {
      formatter->InputDigitAndRememberPosition(next_char, &result);
    } else {
      formatter->InputDigit(next_char, &result);
    }

    formatter->GetRememberedPosition();
  }

  return 0;
}
