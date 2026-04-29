/* Copyright 2026 Google Inc.

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

#include <string>
#include <vector>

#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/shortnumberinfo.h"
#include "phonenumbers/geocoding/phonenumber_offline_geocoder.h"
#include <fuzzer/FuzzedDataProvider.h>
#include <unicode/locid.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  FuzzedDataProvider fuzzed_data(data, size);

  i18n::phonenumbers::PhoneNumber phone_number;
  
  // Split data: some for proto parsing, some for other parameters
  size_t proto_size = fuzzed_data.ConsumeIntegralInRange<size_t>(0, size);
  std::vector<uint8_t> proto_data = fuzzed_data.ConsumeBytes<uint8_t>(proto_size);
  
  if (!phone_number.ParsePartialFromArray(proto_data.data(), proto_data.size())) {
    return 0;
  }

  // Avoid OOM from huge number_of_leading_zeros or long strings
  if (phone_number.number_of_leading_zeros() > 1024) {
    return 0;
  }
  if (phone_number.raw_input().size() > 1024 ||
      phone_number.extension().size() > 1024 ||
      phone_number.preferred_domestic_carrier_code().size() > 1024) {
    return 0;
  }

  i18n::phonenumbers::PhoneNumberUtil* phone_util = 
      i18n::phonenumbers::PhoneNumberUtil::GetInstance();

  // Fuzz with the parsed phone_number
  phone_util->IsPossibleNumber(phone_number);
  phone_util->IsValidNumber(phone_number);
  
  std::string region = fuzzed_data.ConsumeRandomLengthString(3);
  phone_util->IsValidNumberForRegion(phone_number, region);
  
  std::string formatted;
  phone_util->Format(phone_number, i18n::phonenumbers::PhoneNumberUtil::E164, &formatted);
  phone_util->Format(phone_number, i18n::phonenumbers::PhoneNumberUtil::INTERNATIONAL, &formatted);
  phone_util->Format(phone_number, i18n::phonenumbers::PhoneNumberUtil::NATIONAL, &formatted);
  phone_util->Format(phone_number, i18n::phonenumbers::PhoneNumberUtil::RFC3966, &formatted);

  phone_util->GetLengthOfGeographicalAreaCode(phone_number);
  phone_util->GetLengthOfNationalDestinationCode(phone_number);
  
  phone_util->GetNumberType(phone_number);
  
  std::string region_code;
  phone_util->GetRegionCodeForNumber(phone_number, &region_code);

  phone_util->IsNumberGeographical(phone_number);
  phone_util->IsNumberGeographical(phone_util->GetNumberType(phone_number), phone_number.country_code());
  
  phone_util->IsAlphaNumber(phone_number.raw_input());
  
  i18n::phonenumbers::PhoneNumber phone_number_copy(phone_number);
  phone_util->TruncateTooLongNumber(&phone_number_copy);

  // Fuzz with ShortNumberInfo
  i18n::phonenumbers::ShortNumberInfo short_info;
  short_info.IsPossibleShortNumber(phone_number);
  short_info.IsValidShortNumber(phone_number);
  short_info.IsPossibleShortNumberForRegion(phone_number, region);
  short_info.IsValidShortNumberForRegion(phone_number, region);
  short_info.GetExpectedCost(phone_number);

  // Fuzz with OfflineGeocoder
  i18n::phonenumbers::PhoneNumberOfflineGeocoder geocoder;
  std::string language = fuzzed_data.ConsumeRandomLengthString(3);
  std::string country = fuzzed_data.ConsumeRandomLengthString(3);
  icu::Locale locale(language.c_str(), country.c_str());

  geocoder.GetDescriptionForNumber(phone_number, locale);
  geocoder.GetDescriptionForValidNumber(phone_number, locale);
  geocoder.GetDescriptionForValidNumber(phone_number, locale, country);

  return 0;
}
