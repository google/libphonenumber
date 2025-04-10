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

// returns a short number cost based on the data we got from libfuzzer
i18n::phonenumbers::ShortNumberInfo::ShortNumberCost ConsumeShortNumberCost(
    FuzzedDataProvider& fuzzed_data) {
  switch (fuzzed_data.ConsumeIntegralInRange(0, 4)) {
    case 0: return i18n::phonenumbers::ShortNumberInfo::TOLL_FREE;
    case 1: return i18n::phonenumbers::ShortNumberInfo::STANDARD_RATE;
    case 2: return i18n::phonenumbers::ShortNumberInfo::PREMIUM_RATE;
    default: return i18n::phonenumbers::ShortNumberInfo::UNKNOWN_COST;
  }
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  // setup the data provider and util
  FuzzedDataProvider fuzzed_data(data, size);
  i18n::phonenumbers::PhoneNumberUtil* phone_util = 
      i18n::phonenumbers::PhoneNumberUtil::GetInstance();

  // setup all the data we need to pass to the target methods
  i18n::phonenumbers::PhoneNumber phone_number;
  std::string number = fuzzed_data.ConsumeRandomLengthString(32);
  bool region_is_2_bytes = fuzzed_data.ConsumeBool();
  std::string region = fuzzed_data.ConsumeBytesAsString(region_is_2_bytes ? 2 : 3);
  if (fuzzed_data.ConsumeBool()) {
    phone_util->ParseAndKeepRawInput(number, region, &phone_number);
  } else {
    phone_util->Parse(number, region, &phone_number);
  }

  // fuzz the public methods
  i18n::phonenumbers::ShortNumberInfo short_info;
  short_info.IsPossibleShortNumberForRegion(phone_number, region);
  short_info.IsPossibleShortNumber(phone_number);
  short_info.IsValidShortNumber(phone_number);
  short_info.GetExpectedCostForRegion(phone_number, region);
  short_info.GetExpectedCost(phone_number);
  short_info.GetExampleShortNumber(region);
  i18n::phonenumbers::ShortNumberInfo::ShortNumberCost cost = 
      ConsumeShortNumberCost(fuzzed_data);
  short_info.GetExampleShortNumberForCost(region, cost);
  short_info.ConnectsToEmergencyNumber(number, region);
  short_info.IsEmergencyNumber(number, region);
  short_info.IsCarrierSpecific(phone_number);
  short_info.IsCarrierSpecificForRegion(phone_number, region);
  short_info.IsSmsServiceForRegion(phone_number, region);

  return 0;
}