/* Copyright 2020 Google Inc.

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

using google::protobuf::RepeatedPtrField;

// consume PhoneNumberUtil::PhoneNumberType from libfuzzer data
i18n::phonenumbers::PhoneNumberUtil::PhoneNumberType ConsumePhoneNumberType(
    FuzzedDataProvider& fuzzed_data) {
  switch (fuzzed_data.ConsumeIntegralInRange(0, 11)) {
    case 0:
      return i18n::phonenumbers::PhoneNumberUtil::FIXED_LINE;
    case 1:
      return i18n::phonenumbers::PhoneNumberUtil::MOBILE;
    case 2:
      return i18n::phonenumbers::PhoneNumberUtil::FIXED_LINE_OR_MOBILE;
    case 3:
      return i18n::phonenumbers::PhoneNumberUtil::TOLL_FREE;
    case 4:
      return i18n::phonenumbers::PhoneNumberUtil::PREMIUM_RATE;
    case 5:
      return i18n::phonenumbers::PhoneNumberUtil::SHARED_COST;
    case 6:
      return i18n::phonenumbers::PhoneNumberUtil::VOIP;
    case 7:
      return i18n::phonenumbers::PhoneNumberUtil::PERSONAL_NUMBER;
    case 8:
      return i18n::phonenumbers::PhoneNumberUtil::PAGER;
    case 9:
      return i18n::phonenumbers::PhoneNumberUtil::UAN;
    case 10:
      return i18n::phonenumbers::PhoneNumberUtil::VOICEMAIL;
    default:
      return i18n::phonenumbers::PhoneNumberUtil::UNKNOWN;
  }
}

// consume PhoneNumberUtil::PhoneNumberFormat from libfuzzer data
i18n::phonenumbers::PhoneNumberUtil::PhoneNumberFormat ConsumePhoneNumberFormat(
    FuzzedDataProvider& fuzzed_data) {
  switch (fuzzed_data.ConsumeIntegralInRange(0, 3)) {
    case 0:
      return i18n::phonenumbers::PhoneNumberUtil::E164;
    case 1:
      return i18n::phonenumbers::PhoneNumberUtil::INTERNATIONAL;
    case 2:
      return i18n::phonenumbers::PhoneNumberUtil::NATIONAL;
    default:
      return i18n::phonenumbers::PhoneNumberUtil::RFC3966;
  }
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  // initialize the phone util
  i18n::phonenumbers::PhoneNumberUtil* phone_util = 
      i18n::phonenumbers::PhoneNumberUtil::GetInstance();
  FuzzedDataProvider fuzzed_data(data, size);

  // initialize the first phone number, region and country calling code
  i18n::phonenumbers::PhoneNumber phone_number;
  bool region_is_2_bytes = fuzzed_data.ConsumeBool();
  std::string region = fuzzed_data.ConsumeBytesAsString(region_is_2_bytes ? 2 : 3);
  std::string number = fuzzed_data.ConsumeRandomLengthString(32);
  int country_calling_code = fuzzed_data.ConsumeIntegral<int>();

  // trigger either one of the public parse methods
  if (fuzzed_data.ConsumeBool()) {
    phone_util->ParseAndKeepRawInput(number, region, &phone_number);
  } else {
    phone_util->Parse(number, region, &phone_number);
  }

  // initialize the second phone number, this is used only for the 
  // isNumberMatch* methods
  i18n::phonenumbers::PhoneNumber phone_number2;
  std::string number2 = fuzzed_data.ConsumeRandomLengthString(32);
  if (fuzzed_data.ConsumeBool()) {
    phone_util->ParseAndKeepRawInput(number2, region, &phone_number2);
  } else {
    phone_util->Parse(number2, region, &phone_number2);
  }

  // randomly trigger the truncate method, this may affect state of the input
  // for the method calls that follow it
  if (fuzzed_data.ConsumeIntegralInRange(0, 10) == 5) {
    phone_util->TruncateTooLongNumber(&phone_number);
  }

  // fuzz public methods
  phone_util->IsAlphaNumber(number);
  phone_util->IsPossibleNumber(phone_number);
  phone_util->IsNumberMatch(phone_number, phone_number2);
  phone_util->IsNumberMatchWithOneString(phone_number, number2);
  phone_util->IsNumberMatchWithTwoStrings(number, number2);
  phone_util->CanBeInternationallyDialled(phone_number);
  phone_util->GetNumberType(phone_number);
  phone_util->GetLengthOfGeographicalAreaCode(phone_number);
  phone_util->GetLengthOfNationalDestinationCode(phone_number);
  phone_util->IsNANPACountry(region);
  phone_util->GetCountryCodeForRegion(region);
  phone_util->IsPossibleNumberForString(number, region);
  phone_util->IsNumberGeographical(phone_number);
  i18n::phonenumbers::PhoneNumberUtil::PhoneNumberType number_type = 
      ConsumePhoneNumberType(fuzzed_data);
  phone_util->IsNumberGeographical(number_type, country_calling_code);
  phone_util->IsPossibleNumberForType(phone_number, number_type);

  i18n::phonenumbers::PhoneNumber example_number;
  phone_util->GetExampleNumberForType(region, number_type, &example_number);

  i18n::phonenumbers::PhoneNumber example_number_2;
  phone_util->GetExampleNumberForType(number_type, &example_number_2);

  i18n::phonenumbers::PhoneNumber invalid_number;
  phone_util->GetInvalidExampleNumber(region, &invalid_number);

  i18n::phonenumbers::PhoneNumber non_geo_number;
  phone_util->GetExampleNumberForNonGeoEntity(country_calling_code, &non_geo_number);
  
  std::string output;
  phone_util->GetCountryMobileToken(country_calling_code, &output);
  output.clear();

  phone_util->GetRegionCodeForNumber(phone_number, &output);
  output.clear();

  phone_util->GetNddPrefixForRegion(region, fuzzed_data.ConsumeBool(), &output);
  output.clear();

  // Fuzz the methods which affect the input string, but not the PhoneNumber object
  std::string input = fuzzed_data.ConsumeRandomLengthString(32);
  phone_util->ConvertAlphaCharactersInNumber(&input);
  input.clear();

  input = fuzzed_data.ConsumeRandomLengthString(32);
  phone_util->NormalizeDigitsOnly(&input);
  input.clear();

  input = fuzzed_data.ConsumeRandomLengthString(32);
  phone_util->NormalizeDiallableCharsOnly(&input);
  input.clear();

  // Fuzz the formatting methods
  i18n::phonenumbers::PhoneNumberUtil::PhoneNumberFormat format = ConsumePhoneNumberFormat(fuzzed_data);

  std::string formatted;
  phone_util->Format(phone_number, format, &formatted);
  formatted.clear();

  phone_util->FormatInOriginalFormat(phone_number, region, &formatted);
  formatted.clear();

  phone_util->FormatNumberForMobileDialing(phone_number, region, 
      fuzzed_data.ConsumeBool(), &formatted);
  formatted.clear();

  phone_util->FormatNationalNumberWithPreferredCarrierCode(phone_number, region, &formatted);
  formatted.clear();

  phone_util->FormatOutOfCountryKeepingAlphaChars(phone_number, region, &formatted);
  formatted.clear();

  std::string carrier = fuzzed_data.ConsumeRandomLengthString(8);
  phone_util->FormatNationalNumberWithCarrierCode(phone_number, carrier, &formatted);
  formatted.clear();

  // setup the parameters for FormatByPattern
  i18n::phonenumbers::PhoneNumberUtil::PhoneNumberFormat number_format = ConsumePhoneNumberFormat(fuzzed_data);
  RepeatedPtrField<i18n::phonenumbers::NumberFormat> number_formats;
  i18n::phonenumbers::NumberFormat* temp_number_format = number_formats.Add();  
  std::string pattern = fuzzed_data.ConsumeRandomLengthString(16);
  std::string format_string = fuzzed_data.ConsumeRandomLengthString(16);
  temp_number_format->set_pattern(pattern);
  temp_number_format->set_format(format_string);

  // fuzz FormatByPattern
  phone_util->FormatByPattern(phone_number, number_format, number_formats, &formatted);
  formatted.clear();

  return 0;
}
