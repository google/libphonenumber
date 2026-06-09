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

// Library for obtaining information about international short phone numbers,
// such as short codes and emergency numbers. Note most commercial short
// numbers are not handled here, but by the phonenumberutil.

#ifndef I18N_PHONENUMBERS_SHORTNUMBERINFO_H_
#define I18N_PHONENUMBERS_SHORTNUMBERINFO_H_

#include <list>
#include <map>
#include <set>
#include <string>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "absl/container/flat_hash_set.h"
#include "absl/container/flat_hash_map.h"

namespace i18n {
namespace phonenumbers {

using std::list;
using std::map;
using std::set;
using std::string;

class MatcherApi;
class PhoneMetadata;
class PhoneNumber;
class PhoneNumberUtil;

class ShortNumberInfo {
 public:
  ShortNumberInfo();

  // This type is neither copyable nor movable.
  ShortNumberInfo(const ShortNumberInfo&) = delete;
  ShortNumberInfo& operator=(const ShortNumberInfo&) = delete;

  ~ShortNumberInfo();

  // Cost categories of short numbers.
  enum ShortNumberCost {
    TOLL_FREE,
    STANDARD_RATE,
    PREMIUM_RATE,
    UNKNOWN_COST
  };

  // Check whether a short number is a possible number when dialled from a
  // region, given the number in the form of a string, and the region where the
  // number is dialed from.  This provides a more lenient check than
  // IsValidShortNumberForRegion.
  bool IsPossibleShortNumberForRegion(
      const PhoneNumber& short_number,
      const string& region_dialing_from) const;

  // Check whether a short number is a possible number. If a country calling
  // code is shared by multiple regions, this returns true if it's possible in
  // any of them. This provides a more lenient check than IsValidShortNumber.
  // See IsPossibleShortNumberForRegion for details.
  bool IsPossibleShortNumber(const PhoneNumber& number) const;

  // Tests whether a short number matches a valid pattern in a region. Note
  // that this doesn't verify the number is actually in use, which is
  // impossible to tell by just looking at the number itself.
  bool IsValidShortNumberForRegion(
      const PhoneNumber& short_number,
      const string& region_dialing_from) const;

  // Tests whether a short number matches a valid pattern. If a country calling
  // code is shared by multiple regions, this returns true if it's valid in any
  // of them. Note that this doesn't verify the number is actually in use,
  // which is impossible to tell by just looking at the number itself. See
  // IsValidShortNumberForRegion for details.
  bool IsValidShortNumber(const PhoneNumber& number) const;

  // Gets the expected cost category of a short number when dialled from a
  // region (however, nothing is implied about its validity). If it is
  // important that the number is valid, then its validity must first be
  // checked using IsValidShortNumberForRegion. Note that emergency numbers are
  // always considered toll-free. Example usage:
  //
  // PhoneNumber number;
  // phone_util.Parse("110", "US", &number);
  // ...
  // string region_code("CA");
  // ShortNumberInfo short_info;
  // if (short_info.IsValidShortNumberForRegion(number, region_code)) {
  //   ShortNumberInfo::ShortNumberCost cost =
  //       short_info.GetExpectedCostForRegion(number, region_code);
  //   // Do something with the cost information here.
  // }
  ShortNumberCost GetExpectedCostForRegion(
      const PhoneNumber& short_number,
      const string& region_dialing_from) const;

  // Gets the expected cost category of a short number (however, nothing is
  // implied about its validity). If the country calling code is unique to a
  // region, this method behaves exactly the same as GetExpectedCostForRegion.
  // However, if the country calling code is shared by multiple regions, then
  // it returns the highest cost in the sequence PREMIUM_RATE, UNKNOWN_COST,
  // STANDARD_RATE, TOLL_FREE. The reason for the position of UNKNOWN_COST in
  // this order is that if a number is UNKNOWN_COST in one region but
  // STANDARD_RATE or TOLL_FREE in another, its expected cost cannot be
  // estimated as one of the latter since it might be a PREMIUM_RATE number.
  //
  // For example, if a number is STANDARD_RATE in the US, but TOLL_FREE in
  // Canada, the expected cost returned by this method will be STANDARD_RATE,
  // since the NANPA countries share the same country calling code.
  //
  // Note: If the region from which the number is dialed is known, it is highly
  // preferable to call GetExpectedCostForRegion instead.
  ShortNumberCost GetExpectedCost(const PhoneNumber& number) const;

  // Gets a valid short number for the specified region.
  string GetExampleShortNumber(const string& region_code) const;

  // Gets a valid short number for the specified cost category.
  string GetExampleShortNumberForCost(const string& region_code,
                                      ShortNumberCost cost) const;

  // Returns true if the number might be used to connect to an emergency service
  // in the given region.
  //
  // This method takes into account cases where the number might contain
  // formatting, or might have additional digits appended (when it is okay to do
  // that in the region specified).
  bool ConnectsToEmergencyNumber(const string& number,
                                 const string& region_code) const;

  // Returns true if the number exactly matches an emergency service number in
  // the given region.
  //
  // This method takes into account cases where the number might contain
  // formatting, but doesn't allow additional digits to be appended.
  bool IsEmergencyNumber(const string& number,
                         const string& region_code) const;

  // Given a valid short number, determines whether it is carrier-specific
  // (however, nothing is implied about its validity). Carrier-specific numbers
  // may connect to a different end-point, or not connect at all, depending on
  // the user's carrier. If it is important that the number is valid, then its
  // validity must first be checked using IsValidShortNumber or
  // IsValidShortNumberForRegion.
  bool IsCarrierSpecific(const PhoneNumber& number) const;

  // Given a valid short number, determines whether it is carrier-specific when
  // dialed from the given region (however, nothing is implied about its
  // validity). Carrier-specific numbers may connect to a different end-point,
  // or not connect at all, depending on the user's carrier. If it is important
  // that the number is valid, then its validity must first be checked using
  // IsValidShortNumber or IsValidShortNumberForRegion. Returns false if the
  // number doesn't match the region provided.
  bool IsCarrierSpecificForRegion(
      const PhoneNumber& number,
      const string& region_dialing_from) const;

  // Given a valid short number, determines whether it is an SMS service
  // (however, nothing is implied about its validity). An SMS service is where
  // the primary or only intended usage is to receive and/or send text messages
  // (SMSs). This includes MMS as MMS numbers downgrade to SMS if the other
  // party isn't MMS-capable. If it is important that the number is valid, then
  // its validity must first be checked using IsValidShortNumber or
  // IsValidShortNumberForRegion. Returns false if the number doesn't match the
  // region provided.
  bool IsSmsServiceForRegion(
      const PhoneNumber& number,
      const string& region_dialing_from) const;

 private:
  const PhoneNumberUtil& phone_util_;
  const scoped_ptr<const MatcherApi> matcher_api_;

  // A mapping from a RegionCode to the PhoneMetadata for that region.
  scoped_ptr<absl::flat_hash_map<string, PhoneMetadata> >
      region_to_short_metadata_map_;

  // In these countries, if extra digits are added to an emergency number, it no
  // longer connects to the emergency service.
  scoped_ptr<absl::flat_hash_set<string> >
      regions_where_emergency_numbers_must_be_exact_;

  const i18n::phonenumbers::PhoneMetadata* GetMetadataForRegion(
      const string& region_code) const;

  bool RegionDialingFromMatchesNumber(const PhoneNumber& number,
      const string& region_dialing_from) const;

  // Helper method to get the region code for a given phone number, from a list
  // of possible region codes. If the list contains more than one region, the
  // first region for which the number is valid is returned.
  void GetRegionCodeForShortNumberFromRegionList(
      const PhoneNumber& number,
      const list<string>& region_codes,
      string* region_code) const;

  bool MatchesEmergencyNumberHelper(const string& number,
                                    const string& region_code,
                                    bool allow_prefix_match) const;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_SHORTNUMBERINFO_H_
