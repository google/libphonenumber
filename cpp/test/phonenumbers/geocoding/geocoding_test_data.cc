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

#include "phonenumbers/geocoding/geocoding_test_data.h"

#include <cstdint>

namespace i18n {
namespace phonenumbers {
namespace {

const int32_t prefix_1_de_prefixes[] = {
  1201,
  1650,
};

const char* prefix_1_de_descriptions[] = {
  "New Jersey",
  "Kalifornien",
};

const int32_t prefix_1_de_possible_lengths[] = {
  4,
};

const PrefixDescriptions prefix_1_de = {
  prefix_1_de_prefixes,
  sizeof(prefix_1_de_prefixes)/sizeof(*prefix_1_de_prefixes),
  prefix_1_de_descriptions,
  prefix_1_de_possible_lengths,
  sizeof(prefix_1_de_possible_lengths)/sizeof(*prefix_1_de_possible_lengths),
};

const int32_t prefix_1_en_prefixes[] = {
  1201,
  1212,
  1650,
  1989,
  1212812,
  1617423,
  1650960,
};

const char* prefix_1_en_descriptions[] = {
  "NJ",
  "NY",
  "CA",
  "MA",
  "New York, NY",
  "Boston, MA",
  "Mountain View, CA",
};

const int32_t prefix_1_en_possible_lengths[] = {
  4, 7,
};

const PrefixDescriptions prefix_1_en = {
  prefix_1_en_prefixes,
  sizeof(prefix_1_en_prefixes)/sizeof(*prefix_1_en_prefixes),
  prefix_1_en_descriptions,
  prefix_1_en_possible_lengths,
  sizeof(prefix_1_en_possible_lengths)/sizeof(*prefix_1_en_possible_lengths),
};

const int32_t prefix_54_en_prefixes[] = {
  542214,
};

const char* prefix_54_en_descriptions[] = {
  "La Plata",
};

const int32_t prefix_54_en_possible_lengths[] = {
  6,
};

const PrefixDescriptions prefix_54_en = {
  prefix_54_en_prefixes,
  sizeof(prefix_54_en_prefixes)/sizeof(*prefix_54_en_prefixes),
  prefix_54_en_descriptions,
  prefix_54_en_possible_lengths,
  sizeof(prefix_54_en_possible_lengths)/sizeof(*prefix_54_en_possible_lengths),
};

const int32_t prefix_82_en_prefixes[] = {
  822,
  8210,
  8231,
  8232,
  8233,
  8241,
  8242,
  8243,
  8251,
  8252,
  8253,
  8254,
  8255,
  8261,
  8262,
  8263,
  8264,
};

const char* prefix_82_en_descriptions[] = {
  "Seoul",
  "Mobile prefix, should not be geocoded.",
  "Gyeonggi",
  "Incheon",
  "Gangwon",
  "Chungnam",
  "Daejeon",
  "Chungbuk",
  "Busan",
  "Ulsan",
  "Daegu",
  "Gyeongbuk",
  "Gyeongnam",
  "Jeonnam",
  "Gwangju",
  "Jeonbuk",
  "Jeju",
};

const int32_t prefix_82_en_possible_lengths[] = {
  3, 4,
};

const PrefixDescriptions prefix_82_en = {
  prefix_82_en_prefixes,
  sizeof(prefix_82_en_prefixes)/sizeof(*prefix_82_en_prefixes),
  prefix_82_en_descriptions,
  prefix_82_en_possible_lengths,
  sizeof(prefix_82_en_possible_lengths)/sizeof(*prefix_82_en_possible_lengths),
};

const int32_t prefix_82_ko_prefixes[] = {
  822,
  8231,
  8232,
  8233,
  8241,
  8242,
  8243,
  8251,
  8252,
  8253,
  8254,
  8255,
  8261,
  8262,
  8263,
};

const char* prefix_82_ko_descriptions[] = {
  "\xec""\x84""\x9c""\xec""\x9a""\xb8",
  "\xea""\xb2""\xbd""\xea""\xb8""\xb0",
  "\xec""\x9d""\xb8""\xec""\xb2""\x9c",
  "\xea""\xb0""\x95""\xec""\x9b""\x90",
  "\xec""\xb6""\xa9""\xeb""\x82""\xa8",
  "\xeb""\x8c""\x80""\xec""\xa0""\x84",
  "\xec""\xb6""\xa9""\xeb""\xb6""\x81",
  "\xeb""\xb6""\x80""\xec""\x82""\xb0",
  "\xec""\x9a""\xb8""\xec""\x82""\xb0",
  "\xeb""\x8c""\x80""\xea""\xb5""\xac",
  "\xea""\xb2""\xbd""\xeb""\xb6""\x81",
  "\xea""\xb2""\xbd""\xeb""\x82""\xa8",
  "\xec""\xa0""\x84""\xeb""\x82""\xa8",
  "\xea""\xb4""\x91""\xec""\xa3""\xbc",
  "\xec""\xa0""\x84""\xeb""\xb6""\x81",
};

const int32_t prefix_82_ko_possible_lengths[] = {
  3, 4,
};

const PrefixDescriptions prefix_82_ko = {
  prefix_82_ko_prefixes,
  sizeof(prefix_82_ko_prefixes)/sizeof(*prefix_82_ko_prefixes),
  prefix_82_ko_descriptions,
  prefix_82_ko_possible_lengths,
  sizeof(prefix_82_ko_possible_lengths)/sizeof(*prefix_82_ko_possible_lengths),
};

const char* prefix_language_code_pairs[] = {
  "1_de",
  "1_en",
  "54_en",
  "82_en",
  "82_ko",
};

const PrefixDescriptions* prefixes_descriptions[] = {
  &prefix_1_de,
  &prefix_1_en,
  &prefix_54_en,
  &prefix_82_en,
  &prefix_82_ko,
};

const char* country_1[] = {
  "de",
  "en",
};

const CountryLanguages country_1_languages = {
  country_1,
  sizeof(country_1)/sizeof(*country_1),
};

const char* country_54[] = {
  "en",
};

const CountryLanguages country_54_languages = {
  country_54,
  sizeof(country_54)/sizeof(*country_54),
};

const char* country_82[] = {
  "en",
  "ko",
};

const CountryLanguages country_82_languages = {
  country_82,
  sizeof(country_82)/sizeof(*country_82),
};


const CountryLanguages* countries_languages[] = {
  &country_1_languages,
  &country_54_languages,
  &country_82_languages,
};

const int country_calling_codes[] = {
  1,
  54,
  82,
};

}  // namespace

const int* get_test_country_calling_codes() {
  return country_calling_codes;
}

int get_test_country_calling_codes_size() {
  return sizeof(country_calling_codes)
      /sizeof(*country_calling_codes);
}

const CountryLanguages* get_test_country_languages(int index) {
  return countries_languages[index];
}

const char** get_test_prefix_language_code_pairs() {
  return prefix_language_code_pairs;
}

int get_test_prefix_language_code_pairs_size() {
  return sizeof(prefix_language_code_pairs)
      /sizeof(*prefix_language_code_pairs);
}

const PrefixDescriptions* get_test_prefix_descriptions(int index) {
  return prefixes_descriptions[index];
}
}  // namespace phonenumbers
}  // namespace i18n
