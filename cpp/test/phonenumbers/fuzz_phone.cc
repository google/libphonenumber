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

#include <unicode/unistr.h>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/base/memory/singleton.h"
#include "phonenumbers/default_logger.h"
#include "phonenumbers/phonenumber.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/phonenumbermatch.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/stringutil.h"

#include <fuzzer/FuzzedDataProvider.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)
{
    FuzzedDataProvider fuzzed_data(data, size);

    std::string input = fuzzed_data.ConsumeRandomLengthString();
    std::string input2 = fuzzed_data.ConsumeRandomLengthString();

    i18n::phonenumbers::PhoneNumberUtil *phone_util = i18n::phonenumbers::PhoneNumberUtil::GetInstance();
    i18n::phonenumbers::PhoneNumber parsed;

    phone_util->Parse(input, input2, &parsed);
    phone_util->IsValidNumber(parsed);
    phone_util->GetCountryCodeForRegion(input);

    return 0;
}
