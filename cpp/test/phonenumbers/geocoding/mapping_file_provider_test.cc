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
// Author: Patrick Mezard

#include "phonenumbers/geocoding/mapping_file_provider.h"

#include <gtest/gtest.h>  // NOLINT(build/include_order)

#include "phonenumbers/geocoding/geocoding_data.h"

namespace i18n {
namespace phonenumbers {

using std::string;

namespace {

#define COUNTRY_LANGUAGES(code, languagelist)                             \
  const char* country_languages_##code[] = languagelist;                  \
  const CountryLanguages country_##code = {                               \
    country_languages_##code,                                             \
    sizeof(country_languages_##code) / sizeof(*country_languages_##code), \
  };

// Array literals cannot be passed as regular macro arguments, the separating
// commas are interpreted as macro arguments separators. The following dummy
// variadic macro wraps the array commas, and appears as a single argument to an
// outer macro call.
#define ARRAY_WRAPPER(...) __VA_ARGS__

const int country_calling_codes[] = {1, 41, 65, 86};

const int country_calling_codes_size =
  sizeof(country_calling_codes) / sizeof(*country_calling_codes);

COUNTRY_LANGUAGES(1,  ARRAY_WRAPPER({"en"}));
COUNTRY_LANGUAGES(41, ARRAY_WRAPPER({"de", "fr", "it", "rm"}));
COUNTRY_LANGUAGES(65, ARRAY_WRAPPER({"en", "ms", "ta", "zh_Hans"}));
COUNTRY_LANGUAGES(86, ARRAY_WRAPPER({"en", "zh", "zh_Hant"}));

const CountryLanguages* country_languages[] = {
  &country_1,
  &country_41,
  &country_65,
  &country_86,
};

const CountryLanguages* test_get_country_languages(int index) {
  return country_languages[index];
}

}  // namespace

TEST(MappingFileProviderTest, TestGetFileName) {
  MappingFileProvider provider(country_calling_codes,
                               country_calling_codes_size,
                               test_get_country_languages);

  string filename;
  EXPECT_EQ("1_en", provider.GetFileName(1, "en", "", "", &filename));
  EXPECT_EQ("1_en", provider.GetFileName(1, "en", "", "US", &filename));
  EXPECT_EQ("1_en", provider.GetFileName(1, "en", "", "GB", &filename));
  EXPECT_EQ("41_de", provider.GetFileName(41, "de", "", "CH", &filename));
  EXPECT_EQ("", provider.GetFileName(44, "en", "", "GB", &filename));
  EXPECT_EQ("86_zh", provider.GetFileName(86, "zh", "", "", &filename));
  EXPECT_EQ("86_zh", provider.GetFileName(86, "zh", "Hans", "", &filename));
  EXPECT_EQ("86_zh", provider.GetFileName(86, "zh", "", "CN", &filename));
  EXPECT_EQ("", provider.GetFileName(86, "", "", "CN", &filename));
  EXPECT_EQ("86_zh", provider.GetFileName(86, "zh", "Hans", "CN", &filename));
  EXPECT_EQ("86_zh", provider.GetFileName(86, "zh", "Hans", "SG", &filename));
  EXPECT_EQ("86_zh", provider.GetFileName(86, "zh", "", "SG", &filename));
  EXPECT_EQ("86_zh_Hant", provider.GetFileName(86, "zh", "", "TW", &filename));
  EXPECT_EQ("86_zh_Hant", provider.GetFileName(86, "zh", "", "HK", &filename));
  EXPECT_EQ("86_zh_Hant", provider.GetFileName(86, "zh", "Hant", "TW",
                                               &filename));
}

}  // namespace phonenumbers
}  // namespace i18n
