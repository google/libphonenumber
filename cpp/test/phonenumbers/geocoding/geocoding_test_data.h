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

#ifndef I18N_PHONENUMBERS_GEOCODING_TEST_DATA
#define I18N_PHONENUMBERS_GEOCODING_TEST_DATA

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/geocoding/geocoding_data.h"

namespace i18n {
namespace phonenumbers {

// Returns a sorted array of country calling codes.
const int* get_test_country_calling_codes();

// Returns the number of country calling codes in
// get_test_country_calling_codes() array.
int get_test_country_calling_codes_size();

// Returns the CountryLanguages record for country at index, index
// being in [0, get_test_country_calling_codes_size()).
const CountryLanguages* get_test_country_languages(int index);

// Returns a sorted array of prefix language code pairs like
// "1_de" or "82_ko".
const char** get_test_prefix_language_code_pairs();

// Returns the number of elements in
// get_prefix_language_code_pairs()
int get_test_prefix_language_code_pairs_size();

// Returns the PrefixDescriptions for language/code pair at index,
// index being in [0, get_prefix_language_code_pairs_size()).
const PrefixDescriptions* get_test_prefix_descriptions(int index);

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_GEOCODING_TEST_DATA
