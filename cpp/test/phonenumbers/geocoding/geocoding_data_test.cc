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

#include <cmath>
#include <set>
#include <string>

#include <gtest/gtest.h>  // NOLINT(build/include_order)

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/geocoding/geocoding_data.h"
#include "phonenumbers/geocoding/geocoding_test_data.h"

#include "absl/container/btree_set.h"

namespace i18n {
namespace phonenumbers {

using std::set;
using std::string;

namespace {

typedef const CountryLanguages* (*country_languages_getter)(int index);
typedef const PrefixDescriptions* (*prefix_descriptions_getter)(int index);

void TestCountryLanguages(const CountryLanguages* languages) {
  EXPECT_GT(languages->available_languages_size, 0);
  for (int i = 0; i < languages->available_languages_size; ++i) {
    string language(languages->available_languages[i]);
    EXPECT_GT(language.size(), 0);
    if (i > 0) {
      EXPECT_LT(string(languages->available_languages[i - 1]),
                language);
    }
  }
}

void TestCountryCallingCodeLanguages(
    const int* country_calling_codes, int country_calling_codes_size,
    country_languages_getter get_country_languages) {
  EXPECT_GT(country_calling_codes_size, 0);
  for (int i = 0; i < country_calling_codes_size; ++i) {
    int code = country_calling_codes[i];
    EXPECT_GT(code, 0);
    if (i > 0) {
      EXPECT_LT(country_calling_codes[i-1], code);
    }
    TestCountryLanguages(get_country_languages(i));
  }
}

void TestPrefixDescriptions(const PrefixDescriptions* descriptions) {
  EXPECT_GT(descriptions->prefixes_size, 0);
  absl::btree_set<int> possible_lengths;
  for (int i = 0; i < descriptions->prefixes_size; ++i) {
    int prefix = descriptions->prefixes[i];
    EXPECT_GT(prefix, 0);
    if (i > 0) {
      EXPECT_LT(descriptions->prefixes[i - 1], prefix);
    }
    possible_lengths.insert(log10(prefix) + 1);
  }

  EXPECT_GT(descriptions->possible_lengths_size, 0);
  for (int i = 0; i < descriptions->possible_lengths_size; ++i) {
    int possible_length = descriptions->possible_lengths[i];
    EXPECT_GT(possible_length, 0);
    if (i > 0) {
      EXPECT_LT(descriptions->possible_lengths[i - 1], possible_length);
    }
    EXPECT_TRUE(
        possible_lengths.find(possible_length) != possible_lengths.end());
  }
}

void TestAllPrefixDescriptions(
    const char** prefix_language_code_pairs,
    int prefix_language_code_pairs_size,
    prefix_descriptions_getter get_prefix_descriptions) {
  EXPECT_GT(prefix_language_code_pairs_size, 0);
  for (int i = 0; i < prefix_language_code_pairs_size; ++i) {
    string language_code_pair(prefix_language_code_pairs[i]);
    EXPECT_GT(language_code_pair.size(), 0);
    if (i > 0) {
      EXPECT_LT(string(prefix_language_code_pairs[i - 1]),
                language_code_pair);
    }
    TestPrefixDescriptions(get_prefix_descriptions(i));
  }
}

}  // namespace

TEST(GeocodingDataTest, TestCountryCallingCodeLanguages) {
  TestCountryCallingCodeLanguages(get_country_calling_codes(),
                                  get_country_calling_codes_size(),
                                  get_country_languages);
}

TEST(GeocodingDataTest, TestTestCountryCallingCodeLanguages) {
  TestCountryCallingCodeLanguages(get_test_country_calling_codes(),
                                  get_test_country_calling_codes_size(),
                                  get_test_country_languages);
}

TEST(GeocodingDataTest, TestPrefixDescriptions) {
  TestAllPrefixDescriptions(get_prefix_language_code_pairs(),
                            get_prefix_language_code_pairs_size(),
                            get_prefix_descriptions);
}


TEST(GeocodingDataTest, TestTestPrefixDescriptions) {
  TestAllPrefixDescriptions(get_test_prefix_language_code_pairs(),
                            get_test_prefix_language_code_pairs_size(),
                            get_test_prefix_descriptions);
}

TEST(GeocodingDataTest, TestTestGeocodingData) {
  ASSERT_EQ(3, get_test_country_calling_codes_size());
  const int* country_calling_codes = get_test_country_calling_codes();
  const int expected_calling_codes[] = {1, 54, 82};
  for (int i = 0; i < get_test_country_calling_codes_size(); ++i) {
    EXPECT_EQ(expected_calling_codes[i], country_calling_codes[i]);
  }

  const CountryLanguages* langs_1 = get_test_country_languages(0);
  ASSERT_EQ(2, langs_1->available_languages_size);
  const char* expected_languages[] = {"de", "en"};
  for (int i = 0; i < langs_1->available_languages_size; ++i) {
    EXPECT_STREQ(expected_languages[i], langs_1->available_languages[i]);
  }

  ASSERT_EQ(5, get_test_prefix_language_code_pairs_size());
  const char** language_code_pairs = get_test_prefix_language_code_pairs();
  const char* expected_language_code_pairs[] = {
    "1_de", "1_en", "54_en", "82_en", "82_ko",
  };
  for (int i = 0; i < get_test_prefix_language_code_pairs_size(); ++i) {
    EXPECT_STREQ(expected_language_code_pairs[i], language_code_pairs[i]);
  }

  const PrefixDescriptions* desc_1_de = get_test_prefix_descriptions(0);
  ASSERT_EQ(2, desc_1_de->prefixes_size);
  const int32 expected_prefixes[] = {1201, 1650};
  const char* expected_descriptions[] = {
    "New Jersey",
    "Kalifornien",
  };
  for (int i = 0; i < desc_1_de->prefixes_size; ++i) {
    EXPECT_EQ(expected_prefixes[i], desc_1_de->prefixes[i]);
    EXPECT_STREQ(expected_descriptions[i], desc_1_de->descriptions[i]);
  }

  ASSERT_EQ(1, desc_1_de->possible_lengths_size);
  const int expected_lengths[] = {4};
  for (int i = 0; i < desc_1_de->possible_lengths_size; ++i) {
    EXPECT_EQ(expected_lengths[i], desc_1_de->possible_lengths[i]);
  }
}

}  // namespace phonenumbers
}  // namespace i18n
