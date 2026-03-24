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
//
// This file is generated automatically, do not edit it manually.

#ifndef I18N_PHONENUMBERS_GEOCODING_DATA
#define I18N_PHONENUMBERS_GEOCODING_DATA

#include <cstdint>

namespace i18n {
namespace phonenumbers {

struct CountryLanguages {
  // Sorted array of language codes.
  const char** available_languages;

  // Number of elements in available_languages.
  const int available_languages_size;
};

struct PrefixDescriptions {
  // Sorted array of phone number prefixes.
  const int32_t* prefixes;

  // Number of elements in prefixes.
  const int prefixes_size;

  // Array of phone number prefix descriptions, mapped one to one
  // to prefixes.
  const char** descriptions;

  // Sorted array of unique prefix lengths in base 10.
  const int32_t* possible_lengths;

  // Number of elements in possible_lengths.
  const int possible_lengths_size;
};

// Returns a sorted array of country calling codes.
const int* get_country_calling_codes();

// Returns the number of country calling codes in
// get_country_calling_codes() array.
int get_country_calling_codes_size();

// Returns the CountryLanguages record for country at index, index
// being in [0, get_country_calling_codes_size()).
const CountryLanguages* get_country_languages(int index);

// Returns a sorted array of prefix language code pairs like
// "1_de" or "82_ko".
const char** get_prefix_language_code_pairs();

// Returns the number of elements in
// get_prefix_language_code_pairs()
int get_prefix_language_code_pairs_size();

// Returns the PrefixDescriptions for language/code pair at index,
// index being in [0, get_prefix_language_code_pairs_size()).
const PrefixDescriptions* get_prefix_descriptions(int index);

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_GEOCODING_DATA
