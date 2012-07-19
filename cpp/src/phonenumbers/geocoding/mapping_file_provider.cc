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

#include <algorithm>
#include <cstddef>
#include <cstring>
#include <sstream>
#include <string>

#include "phonenumbers/geocoding/geocoding_data.h"

namespace i18n {
namespace phonenumbers {

using std::string;

namespace {

struct NormalizedLocale {
  const char* locale;
  const char* normalized_locale;
};

const NormalizedLocale kNormalizedLocales[] = {
  {"zh_TW", "zh_Hant"},
  {"zh_HK", "zh_Hant"},
  {"zh_MO", "zh_Hant"},
};

const char* GetNormalizedLocale(const string& full_locale) {
  const int size = sizeof(kNormalizedLocales) / sizeof(*kNormalizedLocales);
  for (int i = 0; i != size; ++i) {
    if (full_locale.compare(kNormalizedLocales[i].locale) == 0) {
      return kNormalizedLocales[i].normalized_locale;
    }
  }
  return NULL;
}

void AppendLocalePart(const string& part, string* full_locale) {
  if (!part.empty()) {
    full_locale->append("_");
    full_locale->append(part);
  }
}

void ConstructFullLocale(const string& language, const string& script, const
                         string& region, string* full_locale) {
  full_locale->assign(language);
  AppendLocalePart(script, full_locale);
  AppendLocalePart(region, full_locale);
}

// Returns true if s1 comes strictly before s2 in lexicographic order.
bool IsLowerThan(const char* s1, const char* s2) {
  return strcmp(s1, s2) < 0;
}

// Returns true if languages contains language.
bool HasLanguage(const CountryLanguages* languages, const string& language) {
  const char** const start = languages->available_languages;
  const char** const end = start + languages->available_languages_size;
  const char** const it =
      std::lower_bound(start, end, language.c_str(), IsLowerThan);
  return it != end && strcmp(language.c_str(), *it) == 0;
}

}  // namespace

MappingFileProvider::MappingFileProvider(
    const int* country_calling_codes, int country_calling_codes_size,
    country_languages_getter get_country_languages)
  : country_calling_codes_(country_calling_codes),
    country_calling_codes_size_(country_calling_codes_size),
    get_country_languages_(get_country_languages) {
}

const string& MappingFileProvider::GetFileName(int country_calling_code,
                                               const string& language,
                                               const string& script,
                                               const string& region,
                                               string* filename) const {
  filename->clear();
  if (language.empty()) {
    return *filename;
  }
  const int* const country_calling_codes_end = country_calling_codes_ +
      country_calling_codes_size_;
  const int* const it =
      std::lower_bound(country_calling_codes_,
                       country_calling_codes_end,
                       country_calling_code);
  if (it == country_calling_codes_end || *it != country_calling_code) {
    return *filename;
  }
  const CountryLanguages* const langs =
      get_country_languages_(it - country_calling_codes_);
  if (langs->available_languages_size > 0) {
    string language_code;
    FindBestMatchingLanguageCode(langs, language, script, region,
                                 &language_code);
  if (!language_code.empty()) {
    std::stringstream filename_buf;
    filename_buf << country_calling_code << "_" << language_code;
    *filename = filename_buf.str();
    }
  }
  return *filename;
}

void MappingFileProvider::FindBestMatchingLanguageCode(
  const CountryLanguages* languages, const string& language,
  const string& script, const string& region, string* best_match) const {
  string full_locale;
  ConstructFullLocale(language, script, region, &full_locale);
  const char* const normalized_locale = GetNormalizedLocale(full_locale);
  if (normalized_locale != NULL) {
    string normalized_locale_str(normalized_locale);
    if (HasLanguage(languages, normalized_locale_str)) {
      best_match->swap(normalized_locale_str);
      return;
    }
  }

  if (HasLanguage(languages, full_locale)) {
    best_match->swap(full_locale);
    return;
  }

  if (script.empty() != region.empty()) {
    if (HasLanguage(languages, language)) {
      *best_match = language;
      return;
    }
  } else if (!script.empty() && !region.empty()) {
    string lang_with_script(language);
    lang_with_script.append("_");
    lang_with_script.append(script);
    if (HasLanguage(languages, lang_with_script)) {
      best_match->swap(lang_with_script);
      return;
    }
  }

  string lang_with_region(language);
  lang_with_region.append("_");
  lang_with_region.append(region);
  if (HasLanguage(languages, lang_with_region)) {
    best_match->swap(lang_with_region);
    return;
  }
  if (HasLanguage(languages, language)) {
    *best_match = language;
    return;
  }
  best_match->clear();
}

}  // namespace phonenumbers
}  // namespace i18n
