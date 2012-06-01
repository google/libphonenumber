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
// Default class for storing area codes.

#ifndef I18N_PHONENUMBERS_DEFAULT_MAP_STORAGE_H_
#define I18N_PHONENUMBERS_DEFAULT_MAP_STORAGE_H_

#include <map>
#include <set>
#include <string>
#include <vector>

#include "base/basictypes.h"
#include "phonenumbers/geocoding/area_code_map_storage_strategy.h"

namespace i18n {
namespace phonenumbers {

using std::map;
using std::set;
using std::string;
using std::vector;

// Default area code map storage strategy that is used for data not
// containing description duplications. It is mainly intended to avoid
// the overhead of the string table management when it is actually
// unnecessary (i.e no string duplication).
class DefaultMapStorage : public AreaCodeMapStorageStrategy {
 public:
  DefaultMapStorage();
  virtual ~DefaultMapStorage();

  virtual int GetPrefix(int index) const;
  virtual const string& GetDescription(int index) const;
  virtual void ReadFromMap(const map<int, string>& area_codes);
  virtual int GetNumOfEntries() const;
  virtual const set<int>& GetPossibleLengths() const;

 private:
  // Sorted sequence of phone number prefixes.
  vector<int> prefixes_;
  // Sequence of prefix descriptions, in the same order than prefixes_.
  vector<string> descriptions_;
  // Sequence of unique possible lengths in ascending order.
  set<int> possible_lengths_;

  DISALLOW_COPY_AND_ASSIGN(DefaultMapStorage);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif /* I18N_PHONENUMBERS_DEFAULT_MAP_STORAGE_H_ */
