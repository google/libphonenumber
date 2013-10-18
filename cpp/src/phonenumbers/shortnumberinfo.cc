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

// Author: David Yonge-Mallo

#include "phonenumbers/shortnumberinfo.h"

#include <string.h>
#include <iterator>
#include <map>

#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/default_logger.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/regexp_adapter.h"
#include "phonenumbers/regexp_factory.h"
#include "phonenumbers/short_metadata.h"

namespace i18n {
namespace phonenumbers {

using std::make_pair;
using std::map;
using std::string;

bool LoadCompiledInMetadata(PhoneMetadataCollection* metadata) {
  if (!metadata->ParseFromArray(short_metadata_get(), short_metadata_size())) {
    LOG(ERROR) << "Could not parse binary data.";
    return false;
  }
  return true;
}


ShortNumberInfo::ShortNumberInfo()
    : phone_util_(*PhoneNumberUtil::GetInstance()),
      region_to_short_metadata_map_(new map<string, PhoneMetadata>()) {
  PhoneMetadataCollection metadata_collection;
  if (!LoadCompiledInMetadata(&metadata_collection)) {
    LOG(DFATAL) << "Could not parse compiled-in metadata.";
    return;
  }
  for (RepeatedPtrField<PhoneMetadata>::const_iterator it =
           metadata_collection.metadata().begin();
       it != metadata_collection.metadata().end();
       ++it) {
    const string& region_code = it->id();
    region_to_short_metadata_map_->insert(make_pair(region_code, *it));
  }
}

// Returns a pointer to the phone metadata for the appropriate region or NULL
// if the region code is invalid or unknown.
const PhoneMetadata* ShortNumberInfo::GetMetadataForRegion(
    const string& region_code) const {
  map<string, PhoneMetadata>::const_iterator it =
      region_to_short_metadata_map_->find(region_code);
  if (it != region_to_short_metadata_map_->end()) {
    return &it->second;
  }
  return NULL;
}

bool ShortNumberInfo::ConnectsToEmergencyNumber(const string& number,
    const string& region_code) const {
  return MatchesEmergencyNumberHelper(number, region_code,
      true /* allows prefix match */);
}

bool ShortNumberInfo::IsEmergencyNumber(const string& number,
    const string& region_code) const {
  return MatchesEmergencyNumberHelper(number, region_code,
      false /* doesn't allow prefix match */);
}

bool ShortNumberInfo::MatchesEmergencyNumberHelper(const string& number,
    const string& region_code, bool allow_prefix_match) const {
  string extracted_number;
  phone_util_.ExtractPossibleNumber(number, &extracted_number);
  if (phone_util_.StartsWithPlusCharsPattern(extracted_number)) {
    // Returns false if the number starts with a plus sign. We don't believe
    // dialing the country code before emergency numbers (e.g. +1911) works,
    // but later, if that proves to work, we can add additional logic here to
    // handle it.
    return false;
  }
  const PhoneMetadata* metadata = GetMetadataForRegion(region_code);
  if (!metadata || !metadata->has_emergency()) {
    return false;
  }
  const scoped_ptr<const AbstractRegExpFactory> regexp_factory(
      new RegExpFactory());
  const scoped_ptr<const RegExp> emergency_number_pattern(
      regexp_factory->CreateRegExp(
          metadata->emergency().national_number_pattern()));
  phone_util_.NormalizeDigitsOnly(&extracted_number);
  const scoped_ptr<RegExpInput> normalized_number_input(
      regexp_factory->CreateInput(extracted_number));

  // In Brazil and Chile, emergency numbers don't work when additional digits
  // are appended.
  return (!allow_prefix_match ||
      region_code == "BR" || region_code == "CL")
      ? emergency_number_pattern->FullMatch(extracted_number)
      : emergency_number_pattern->Consume(normalized_number_input.get());
}

}  // namespace phonenumbers
}  // namespace i18n
