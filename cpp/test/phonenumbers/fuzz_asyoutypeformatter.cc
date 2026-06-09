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

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  // initial setup of all the structs we need
  FuzzedDataProvider fuzzed_data(data, size);
  i18n::phonenumbers::PhoneNumberUtil* phone_util = 
      i18n::phonenumbers::PhoneNumberUtil::GetInstance();
  bool region_is_2_bytes = fuzzed_data.ConsumeBool();
  std::string region = fuzzed_data.ConsumeBytesAsString(region_is_2_bytes ? 2 : 3);
  std::unique_ptr<i18n::phonenumbers::AsYouTypeFormatter> formatter(
      phone_util->GetAsYouTypeFormatter(region));

  // setup the data passed to the target methods
  const int iterations = fuzzed_data.ConsumeIntegralInRange(0, 32);
  std::string result;

  // Random amount of iterations 
  for (int i = 0; i < iterations; ++i) {
    const char32_t next_char = fuzzed_data.ConsumeIntegral<char32_t>();
    const bool remember = fuzzed_data.ConsumeBool();
    
    // Randomly trigger the remember method
    if (remember) {
      formatter->InputDigitAndRememberPosition(next_char, &result);
    } else {
      formatter->InputDigit(next_char, &result);
    }

    // get the remembered position whether we remembered it or not
    formatter->GetRememberedPosition();
  }

  return 0;
}