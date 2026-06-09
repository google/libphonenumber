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

// Author: Patrick Mezard

#ifndef I18N_PHONENUMBERS_GEOCODING_MAPPING_FILE_PROVIDER_H_
#define I18N_PHONENUMBERS_GEOCODING_MAPPING_FILE_PROVIDER_H_

#include <string>

#include "phonenumbers/base/basictypes.h"

namespace i18n {
namespace phonenumbers {

using std::string;

struct CountryLanguages;

// A utility which knows the data files that are available for the geocoder to
// use. The data files contain mappings from phone number prefixes to text
// descriptions, and are organized by country calling code and language that the
// text descriptions are in.
class MappingFileProvider {
 public:
  typedef const CountryLanguages* (*country_languages_getter)(int index);

  // Initializes a MappingFileProvider with country_calling_codes, a sorted
  // list of country_calling_code_size calling codes, and a function
  // get_country_languages(int index) returning the CountryLanguage information
  // related to the country code at index in country_calling_codes.
  MappingFileProvider(const int* country_calling_codes,
                      int country_calling_code_size,
                      country_languages_getter get_country_languages);

  // This type is neither copyable nor movable.
  MappingFileProvider(const MappingFileProvider&) = delete;
  MappingFileProvider& operator=(const MappingFileProvider&) = delete;

  // Returns the name of the file that contains the mapping data for the
  // country_calling_code in the language specified, or an empty string if no
  // such file can be found.
  // language is a two or three-letter lowercase language code as defined by ISO
  // 639. Note that where two different language codes exist (e.g. 'he' and 'iw'
  // for Hebrew) we use the one that Java/Android canonicalized on ('iw' in this
  // case).
  // script is a four-letter titlecase (the first letter is uppercase and the
  // rest of the letters are lowercase) ISO script code as defined in ISO 15924.
  // region is a two-letter uppercase ISO country code as defined by ISO 3166-1.
  const string& GetFileName(int country_calling_code, const string& language,
                            const string& script, const string& region, string*
                            filename) const;

 private:
  void FindBestMatchingLanguageCode(const CountryLanguages* languages,
                                    const string& language,
                                    const string& script,
                                    const string& region,
                                    string* best_match) const;

  const int* const country_calling_codes_;
  const int country_calling_codes_size_;
  const country_languages_getter get_country_languages_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_GEOCODING_MAPPING_FILE_PROVIDER_H_
