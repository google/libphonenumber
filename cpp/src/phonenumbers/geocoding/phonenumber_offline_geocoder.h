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

#ifndef I18N_PHONENUMBERS_GEOCODING_PHONENUMBER_OFFLINE_GEOCODER_H_
#define I18N_PHONENUMBERS_GEOCODING_PHONENUMBER_OFFLINE_GEOCODER_H_

#include <map>
#include <string>

#include <unicode/locid.h>  // NOLINT(build/include_order)

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"

namespace i18n {
namespace phonenumbers {

using std::map;
using std::string;

class AreaCodeMap;
class MappingFileProvider;
class PhoneNumber;
class PhoneNumberUtil;
struct CountryLanguages;
struct PrefixDescriptions;
typedef icu::Locale Locale;

// An offline geocoder which provides geographical information related to a
// phone number.
class PhoneNumberOfflineGeocoder {
 private:
  typedef map<string, const AreaCodeMap*> AreaCodeMaps;

 public:
  typedef const CountryLanguages* (*country_languages_getter)(int index);
  typedef const PrefixDescriptions* (*prefix_descriptions_getter)(int index);

  PhoneNumberOfflineGeocoder();

  // For tests
  PhoneNumberOfflineGeocoder(
      const int* country_calling_codes,
      int country_calling_codes_size,
      country_languages_getter get_country_languages,
      const char** prefix_language_code_pairs,
      int prefix_language_code_pairs_size,
      prefix_descriptions_getter get_prefix_descriptions);

  virtual ~PhoneNumberOfflineGeocoder();

  // Returns a text description for the given phone number, in the language
  // provided. The description might consist of the name of the country where
  // the phone number is from, or the name of the geographical area the phone
  // number is from if more detailed information is available.
  //
  // This method assumes the validity of the number passed in has already been
  // checked.
  string GetDescriptionForValidNumber(const PhoneNumber& number,
                                      const Locale& language) const;

  // As per GetDescriptionForValidNumber(PhoneNumber, Locale) but also considers
  // the region of the user. If the phone number is from the same region as the
  // user, only a lower-level description will be returned, if one exists.
  // Otherwise, the phone number's region will be returned, with optionally some
  // more detailed information.
  //
  // For example, for a user from the region "US" (United States), we would show
  // "Mountain View, CA" for a particular number, omitting the United States
  // from the description. For a user from the United Kingdom (region "GB"), for
  // the same number we may show "Mountain View, CA, United States" or even just
  // "United States".
  //
  // This method assumes the validity of the number passed in has already been
  // checked.
  //
  // user_region is the region code for a given user. This region will be
  // omitted from the description if the phone number comes from this region. It
  // is a two-letter uppercase ISO country code as defined by ISO 3166-1.
  string GetDescriptionForValidNumber(const PhoneNumber& number,
      const Locale& language, const string& user_region) const;

  // As per GetDescriptionForValidNumber(PhoneNumber, Locale) but explicitly
  // checks the validity of the number passed in.
  string GetDescriptionForNumber(const PhoneNumber& number,
                                 const Locale& locale) const;

  // As per GetDescriptionForValidNumber(PhoneNumber, Locale, String) but
  // explicitly checks the validity of the number passed in.
  string GetDescriptionForNumber(const PhoneNumber& number,
      const Locale& language, const string& user_region) const;

 private:
  void Init(const int* country_calling_codes,
            int country_calling_codes_size,
            country_languages_getter get_country_languages,
            const char** prefix_language_code_pairs,
            int prefix_language_code_pairs_size,
            prefix_descriptions_getter get_prefix_descriptions);

  const AreaCodeMap* GetPhonePrefixDescriptions(int prefix,
      const string& language, const string& script, const string& region) const;

  AreaCodeMaps::const_iterator LoadAreaCodeMapFromFile(
      const string& filename) const;

  // Returns the customary display name in the given language for the given
  // region.
  string GetRegionDisplayName(const string* region_code,
                              const Locale& language) const;

  // Returns the customary display name in the given language for the given
  // territory the phone number is from.
  string GetCountryNameForNumber(const PhoneNumber& number,
                                 const Locale& language) const;

  // Returns an area-level text description in the given language for the given
  // phone number, or an empty string.
  // lang is a two-letter lowercase ISO language codes as defined by ISO 639-1.
  // script is a four-letter titlecase (the first letter is uppercase and the
  // rest of the letters are lowercase) ISO script codes as defined in ISO
  // 15924.
  // region is a two-letter uppercase ISO country codes as defined by ISO
  // 3166-1.
  const char* GetAreaDescription(const PhoneNumber& number, const string& lang,
                                 const string& script,
                                 const string& region) const;

  bool MayFallBackToEnglish(const string& lang) const;

 private:
  const PhoneNumberUtil* phone_util_;
  // The MappingFileProvider knows for which combination of country calling code
  // and language a phone prefix mapping file is available in the file system,
  // so that a file can be loaded when needed.
  scoped_ptr<const MappingFileProvider> provider_;

  const char** prefix_language_code_pairs_;
  int prefix_language_code_pairs_size_;
  prefix_descriptions_getter get_prefix_descriptions_;

  // A mapping from country calling codes languages pairs to the corresponding
  // phone prefix map that has been loaded.
  mutable AreaCodeMaps available_maps_;

  DISALLOW_COPY_AND_ASSIGN(PhoneNumberOfflineGeocoder);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif /* I18N_PHONENUMBERS_GEOCODING_PHONENUMBER_OFFLINE_GEOCODER_H_ */
