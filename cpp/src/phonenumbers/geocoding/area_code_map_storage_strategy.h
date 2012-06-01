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
//
// Interface for phone numbers area prefixes storage classes.

#ifndef I18N_PHONENUMBERS_AREA_CODE_MAP_STRATEGY_H_
#define I18N_PHONENUMBERS_AREA_CODE_MAP_STRATEGY_H_

#include <map>
#include <set>
#include <string>

namespace i18n {
namespace phonenumbers {

using std::map;
using std::set;
using std::string;

// Abstracts the way area code data is stored into memory. It is used by
// AreaCodeMap to support the most space-efficient storage strategy according
// to the provided data.
class AreaCodeMapStorageStrategy {
 public:
  virtual ~AreaCodeMapStorageStrategy() {}

  // Returns the phone number prefix located at the provided index.
  virtual int GetPrefix(int index) const = 0;

  // Gets the description corresponding to the phone number prefix located
  // at the provided index. If the description is not available in the current
  // language an empty string is returned.
  virtual const string& GetDescription(int index) const = 0;

  // Sets the internal state of the underlying storage implementation from the
  // provided area_codes that maps phone number prefixes to description strings.
  virtual void ReadFromMap(const map<int, string>& area_codes) = 0;

  // Returns the number of entries contained in the area code map.
  virtual int GetNumOfEntries() const = 0;

  // Returns the set containing the possible lengths of prefixes.
  virtual const set<int>& GetPossibleLengths() const = 0;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_AREA_CODE_MAP_STRATEGY_H_
