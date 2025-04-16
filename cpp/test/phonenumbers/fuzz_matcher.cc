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
#include <memory>
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
#include "phonenumbers/regexp_adapter_icu.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/stringutil.h"
#include "phonenumbers/asyoutypeformatter.h"
#include "phonenumbers/shortnumberinfo.h"
#include <fuzzer/FuzzedDataProvider.h>

// returns a leniency level based on the data we got from libfuzzer
i18n::phonenumbers::PhoneNumberMatcher::Leniency ConsumeLeniency(
    FuzzedDataProvider& fuzzed_data) {
  switch (fuzzed_data.ConsumeIntegralInRange(0, 3)) {
    case 0:
      return i18n::phonenumbers::PhoneNumberMatcher::Leniency::POSSIBLE;
    case 1:
      return i18n::phonenumbers::PhoneNumberMatcher::Leniency::VALID;
    case 2:
      return i18n::phonenumbers::PhoneNumberMatcher::Leniency::STRICT_GROUPING;
    default:
      return i18n::phonenumbers::PhoneNumberMatcher::Leniency::EXACT_GROUPING;
  }
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  // Setup the data provider and util
  FuzzedDataProvider fuzzed_data(data, size);
  i18n::phonenumbers::PhoneNumberUtil* phone_util = 
      i18n::phonenumbers::PhoneNumberUtil::GetInstance();

  // this should be enought to get at least 2 matches
  std::string text = fuzzed_data.ConsumeBytesAsString(128);

  // the region is either 2 or 3 characters long
  bool region_is_2_bytes = fuzzed_data.ConsumeBool();
  std::string region = fuzzed_data.ConsumeBytesAsString(region_is_2_bytes ? 2 : 3);

  // setup fuzzed data for matchers
  i18n::phonenumbers::PhoneNumberMatcher::Leniency leniency = 
    ConsumeLeniency(fuzzed_data);
  int max_tries = fuzzed_data.ConsumeIntegralInRange(0, 500);
  bool full_match = fuzzed_data.ConsumeBool();
  std::string regexp_string = fuzzed_data.ConsumeRandomLengthString(32);


  // initialize and fuzz the built-in matcher
  i18n::phonenumbers::PhoneNumberMatcher matcher(*phone_util, text, region, 
      leniency, max_tries);
  while (matcher.HasNext()) {
    i18n::phonenumbers::PhoneNumberMatch match;
    matcher.Next(&match);
  }

  // fuzz the matching with the icu adapter
  std::string matched_string;
  i18n::phonenumbers::ICURegExpFactory factory;
  std::unique_ptr<i18n::phonenumbers::RegExp> regexp(
    factory.CreateRegExp(regexp_string));
  regexp->Match(text, full_match, &matched_string);

  return 0;
}
