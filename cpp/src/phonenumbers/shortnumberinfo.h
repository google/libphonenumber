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

// Utility for international short phone numbers, such as short codes and
// emergency numbers. Note most commercial short numbers are not handled here,
// but by the phonenumberutil.

#ifndef I18N_PHONENUMBERS_SHORTNUMBERINFO_H_
#define I18N_PHONENUMBERS_SHORTNUMBERINFO_H_

#include <map>
#include <string>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/phonemetadata.pb.h"

namespace i18n {
namespace phonenumbers {

using std::map;
using std::string;

class PhoneNumberUtil;

class ShortNumberInfo {
 public:
  ShortNumberInfo();

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

 private:
  const PhoneNumberUtil& phone_util_;

  // A mapping from a RegionCode to the PhoneMetadata for that region.
  scoped_ptr<map<string, PhoneMetadata> >
      region_to_short_metadata_map_;

  const i18n::phonenumbers::PhoneMetadata* GetMetadataForRegion(
      const string& region_code) const;

  bool MatchesEmergencyNumberHelper(const string& number,
                                    const string& region_code,
                                    bool allow_prefix_match) const;

  DISALLOW_COPY_AND_ASSIGN(ShortNumberInfo);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_SHORTNUMBERINFO_H_
