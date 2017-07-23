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

#include <algorithm>
#include <string.h>
#include <iterator>
#include <map>

#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/default_logger.h"
#include "phonenumbers/matcher_api.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/regex_based_matcher.h"
#include "phonenumbers/region_code.h"
#include "phonenumbers/short_metadata.h"

namespace i18n {
namespace phonenumbers {

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
      matcher_api_(new RegexBasedMatcher()),
      region_to_short_metadata_map_(new map<string, PhoneMetadata>()),
      regions_where_emergency_numbers_must_be_exact_(new set<string>()) {
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
    region_to_short_metadata_map_->insert(std::make_pair(region_code, *it));
  }
  regions_where_emergency_numbers_must_be_exact_->insert("BR");
  regions_where_emergency_numbers_must_be_exact_->insert("CL");
  regions_where_emergency_numbers_must_be_exact_->insert("NI");
}

ShortNumberInfo::~ShortNumberInfo() {}

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

namespace {
// Same as the matchesPossibleNumberAndNationalNumber method in
// java/libphonenumber/src/com/google/i18n/phonenumbers/ShortNumberInfo.java
// TODO: Once we have benchmarked ShortNumberInfo, consider if it is
// worth keeping this performance optimization, and if so move this into the
// matcher implementation.
bool MatchesPossibleNumberAndNationalNumber(
    const MatcherApi& matcher_api, const string& number,
    const PhoneNumberDesc& number_desc) {
  return matcher_api.MatchesPossibleNumber(number, number_desc) &&
         matcher_api.MatchesNationalNumber(number, number_desc, false);
}
}  // namespace

// Helper method to check that the country calling code of the number matches
// the region it's being dialed from.
bool ShortNumberInfo::RegionDialingFromMatchesNumber(const PhoneNumber& number,
    const string& region_dialing_from) const {
  list<string> region_codes;
  phone_util_.GetRegionCodesForCountryCallingCode(number.country_code(),
                                                  &region_codes);
  return std::find(region_codes.begin(),
                   region_codes.end(),
                   region_dialing_from) != region_codes.end();
}

bool ShortNumberInfo::IsPossibleShortNumberForRegion(
    const string& short_number, const string& region_dialing_from) const {
  const PhoneMetadata* phone_metadata =
      GetMetadataForRegion(region_dialing_from);
  if (!phone_metadata) {
    return false;
  }
  const PhoneNumberDesc& general_desc = phone_metadata->general_desc();
  return matcher_api_->MatchesPossibleNumber(short_number, general_desc);
}

bool ShortNumberInfo::IsPossibleShortNumberForRegion(const PhoneNumber& number,
    const string& region_dialing_from) const {
  if (!RegionDialingFromMatchesNumber(number, region_dialing_from)) {
    return false;
  }
  const PhoneMetadata* phone_metadata =
      GetMetadataForRegion(region_dialing_from);
  if (!phone_metadata) {
    return false;
  }
  string short_number;
  phone_util_.GetNationalSignificantNumber(number, &short_number);
  const PhoneNumberDesc& general_desc = phone_metadata->general_desc();
  return matcher_api_->MatchesPossibleNumber(short_number, general_desc);
}

bool ShortNumberInfo::IsPossibleShortNumber(const PhoneNumber& number) const {
  list<string> region_codes;
  phone_util_.GetRegionCodesForCountryCallingCode(number.country_code(),
                                                  &region_codes);
  string short_number;
  phone_util_.GetNationalSignificantNumber(number, &short_number);
  for (list<string>::const_iterator it = region_codes.begin();
       it != region_codes.end(); ++it) {
    const PhoneMetadata* phone_metadata = GetMetadataForRegion(*it);
    if (!phone_metadata) {
      continue;
    }
    if (matcher_api_->MatchesPossibleNumber(short_number,
                                            phone_metadata->general_desc())) {
      return true;
    }
  }
  return false;
}

bool ShortNumberInfo::IsValidShortNumberForRegion(
    const string& short_number, const string& region_dialing_from) const {
  const PhoneMetadata* phone_metadata =
      GetMetadataForRegion(region_dialing_from);
  if (!phone_metadata) {
    return false;
  }
  const PhoneNumberDesc& general_desc = phone_metadata->general_desc();
  if (!MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                              general_desc)) {
    return false;
  }
  const PhoneNumberDesc& short_number_desc = phone_metadata->short_code();
  return MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                                short_number_desc);
}

bool ShortNumberInfo::IsValidShortNumberForRegion(
    const PhoneNumber& number, const string& region_dialing_from) const {
  if (!RegionDialingFromMatchesNumber(number, region_dialing_from)) {
    return false;
  }
  const PhoneMetadata* phone_metadata =
      GetMetadataForRegion(region_dialing_from);
  if (!phone_metadata) {
    return false;
  }
  string short_number;
  phone_util_.GetNationalSignificantNumber(number, &short_number);
  const PhoneNumberDesc& general_desc = phone_metadata->general_desc();
  if (!MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                              general_desc)) {
    return false;
  }
  const PhoneNumberDesc& short_number_desc = phone_metadata->short_code();
  return MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                                short_number_desc);
}

bool ShortNumberInfo::IsValidShortNumber(const PhoneNumber& number) const {
  list<string> region_codes;
  phone_util_.GetRegionCodesForCountryCallingCode(number.country_code(),
                                                  &region_codes);
  string region_code;
  GetRegionCodeForShortNumberFromRegionList(number, region_codes, &region_code);
  if (region_codes.size() > 1 && region_code != RegionCode::GetUnknown()) {
    return true;
  }
  return IsValidShortNumberForRegion(number, region_code);
}

ShortNumberInfo::ShortNumberCost ShortNumberInfo::GetExpectedCostForRegion(
    const string& short_number, const string& region_dialing_from) const {
  const PhoneMetadata* phone_metadata =
      GetMetadataForRegion(region_dialing_from);
  if (!phone_metadata) {
    return ShortNumberInfo::UNKNOWN_COST;
  }

  // The cost categories are tested in order of decreasing expense, since if
  // for some reason the patterns overlap the most expensive matching cost
  // category should be returned.
  if (MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                             phone_metadata->premium_rate())) {
    return ShortNumberInfo::PREMIUM_RATE;
  }
  if (MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                             phone_metadata->standard_rate())) {
    return ShortNumberInfo::STANDARD_RATE;
  }
  if (MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                             phone_metadata->toll_free())) {
    return ShortNumberInfo::TOLL_FREE;
  }
  if (IsEmergencyNumber(short_number, region_dialing_from)) {
    // Emergency numbers are implicitly toll-free.
    return ShortNumberInfo::TOLL_FREE;
  }
  return ShortNumberInfo::UNKNOWN_COST;
}

ShortNumberInfo::ShortNumberCost ShortNumberInfo::GetExpectedCostForRegion(
    const PhoneNumber& number, const string& region_dialing_from) const {
  if (!RegionDialingFromMatchesNumber(number, region_dialing_from)) {
    return ShortNumberInfo::UNKNOWN_COST;
  }
  const PhoneMetadata* phone_metadata =
      GetMetadataForRegion(region_dialing_from);
  if (!phone_metadata) {
    return ShortNumberInfo::UNKNOWN_COST;
  }
  string short_number;
  phone_util_.GetNationalSignificantNumber(number, &short_number);

  // The cost categories are tested in order of decreasing expense, since if
  // for some reason the patterns overlap the most expensive matching cost
  // category should be returned.
  if (MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                             phone_metadata->premium_rate())) {
    return ShortNumberInfo::PREMIUM_RATE;
  }
  if (MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                             phone_metadata->standard_rate())) {
    return ShortNumberInfo::STANDARD_RATE;
  }
  if (MatchesPossibleNumberAndNationalNumber(*matcher_api_, short_number,
                                             phone_metadata->toll_free())) {
    return ShortNumberInfo::TOLL_FREE;
  }
  if (IsEmergencyNumber(short_number, region_dialing_from)) {
    // Emergency numbers are implicitly toll-free.
    return ShortNumberInfo::TOLL_FREE;
  }
  return ShortNumberInfo::UNKNOWN_COST;
}

ShortNumberInfo::ShortNumberCost ShortNumberInfo::GetExpectedCost(
    const PhoneNumber& number) const {
  list<string> region_codes;
  phone_util_.GetRegionCodesForCountryCallingCode(number.country_code(),
                                                  &region_codes);
  if (region_codes.size() == 0) {
    return ShortNumberInfo::UNKNOWN_COST;
  }
  if (region_codes.size() == 1) {
    return GetExpectedCostForRegion(number, region_codes.front());
  }
  ShortNumberInfo::ShortNumberCost cost = ShortNumberInfo::TOLL_FREE;
  for (list<string>::const_iterator it = region_codes.begin();
       it != region_codes.end(); ++it) {
    ShortNumberInfo::ShortNumberCost cost_for_region =
        GetExpectedCostForRegion(number, *it);
    switch (cost_for_region) {
     case ShortNumberInfo::PREMIUM_RATE:
       return ShortNumberInfo::PREMIUM_RATE;
     case ShortNumberInfo::UNKNOWN_COST:
       return ShortNumberInfo::UNKNOWN_COST;
     case ShortNumberInfo::STANDARD_RATE:
       if (cost != ShortNumberInfo::UNKNOWN_COST) {
         cost = ShortNumberInfo::STANDARD_RATE;
       }
       break;
     case ShortNumberInfo::TOLL_FREE:
       // Do nothing.
       break;
     default:
       LOG(ERROR) << "Unrecognised cost for region: "
                  << static_cast<int>(cost_for_region);
       break;
    }
  }
  return cost;
}

void ShortNumberInfo::GetRegionCodeForShortNumberFromRegionList(
    const PhoneNumber& number, const list<string>& region_codes,
    string* region_code) const {
  if (region_codes.size() == 0) {
    region_code->assign(RegionCode::GetUnknown());
    return;
  } else if (region_codes.size() == 1) {
    region_code->assign(region_codes.front());
    return;
  }
  string national_number;
  phone_util_.GetNationalSignificantNumber(number, &national_number);
  for (list<string>::const_iterator it = region_codes.begin();
       it != region_codes.end(); ++it) {
    const PhoneMetadata* phone_metadata = GetMetadataForRegion(*it);
    if (phone_metadata != NULL &&
        MatchesPossibleNumberAndNationalNumber(*matcher_api_, national_number,
                                               phone_metadata->short_code())) {
      // The number is valid for this region.
      region_code->assign(*it);
      return;
    }
  }
  region_code->assign(RegionCode::GetUnknown());
}

string ShortNumberInfo::GetExampleShortNumber(const string& region_code) const {
  const PhoneMetadata* phone_metadata = GetMetadataForRegion(region_code);
  if (!phone_metadata) {
    return "";
  }
  const PhoneNumberDesc& desc = phone_metadata->short_code();
  if (desc.has_example_number()) {
    return desc.example_number();
  }
  return "";
}

string ShortNumberInfo::GetExampleShortNumberForCost(const string& region_code,
    ShortNumberInfo::ShortNumberCost cost) const {
  const PhoneMetadata* phone_metadata = GetMetadataForRegion(region_code);
  if (!phone_metadata) {
    return "";
  }
  const PhoneNumberDesc* desc = NULL;
  switch (cost) {
    case TOLL_FREE:
      desc = &(phone_metadata->toll_free());
      break;
    case STANDARD_RATE:
      desc = &(phone_metadata->standard_rate());
      break;
    case PREMIUM_RATE:
      desc = &(phone_metadata->premium_rate());
      break;
    default:
      // UNKNOWN_COST numbers are computed by the process of elimination from
      // the other cost categories.
      break;
  }
  if (desc != NULL && desc->has_example_number()) {
    return desc->example_number();
  }
  return "";
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
  phone_util_.NormalizeDigitsOnly(&extracted_number);
  bool allow_prefix_match_for_region =
      allow_prefix_match &&
      regions_where_emergency_numbers_must_be_exact_->find(region_code) ==
          regions_where_emergency_numbers_must_be_exact_->end();
  return matcher_api_->MatchesNationalNumber(
      extracted_number, metadata->emergency(), allow_prefix_match_for_region);
}

bool ShortNumberInfo::IsCarrierSpecific(const PhoneNumber& number) const {
  list<string> region_codes;
  phone_util_.GetRegionCodesForCountryCallingCode(number.country_code(),
                                                  &region_codes);
  string region_code;
  GetRegionCodeForShortNumberFromRegionList(number, region_codes, &region_code);
  string national_number;
  phone_util_.GetNationalSignificantNumber(number, &national_number);
  const PhoneMetadata* phone_metadata = GetMetadataForRegion(region_code);
  return phone_metadata &&
         MatchesPossibleNumberAndNationalNumber(*matcher_api_, national_number,
             phone_metadata->carrier_specific());
}

}  // namespace phonenumbers
}  // namespace i18n
