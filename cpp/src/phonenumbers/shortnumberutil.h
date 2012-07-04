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
//
// Author: David Yonge-Mallo
//
// This is a direct port from ShortNumberUtil.java.
// Changes to this class should also happen to the Java version, whenever it
// makes sense.

#ifndef I18N_PHONENUMBERS_SHORTNUMBERUTIL_H_
#define I18N_PHONENUMBERS_SHORTNUMBERUTIL_H_

#include <string>

#include "base/basictypes.h"

namespace i18n {
namespace phonenumbers {

using std::string;

class PhoneNumberUtil;

class ShortNumberUtil {
 public:
  ShortNumberUtil();

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

  bool MatchesEmergencyNumberHelper(const string& number,
                                    const string& region_code,
                                    bool allow_prefix_match) const;

  DISALLOW_COPY_AND_ASSIGN(ShortNumberUtil);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_SHORTNUMBERUTIL_H_
