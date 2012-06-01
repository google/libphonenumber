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

#include "phonenumbers/geocoding/default_map_storage.h"

#include <math.h>
#include <utility>

#include "base/logging.h"

namespace i18n {
namespace phonenumbers {

using std::map;
using std::set;
using std::string;

DefaultMapStorage::DefaultMapStorage() {
}

DefaultMapStorage::~DefaultMapStorage() {
}

int DefaultMapStorage::GetPrefix(int index) const {
  DCHECK_GE(index, 0);
  DCHECK_LT(index, static_cast<int>(prefixes_.size()));
  return prefixes_[index];
}

const string& DefaultMapStorage::GetDescription(int index) const {
  DCHECK_GE(index, 0);
  DCHECK_LT(index, static_cast<int>(descriptions_.size()));
  return descriptions_[index];
}

void DefaultMapStorage::ReadFromMap(const map<int, string>& area_codes) {
  prefixes_.resize(area_codes.size());
  descriptions_.resize(area_codes.size());
  possible_lengths_.clear();
  int index = 0;
  for (map<int, string>::const_iterator it = area_codes.begin();
       it != area_codes.end(); ++it, ++index) {
    prefixes_[index] = it->first;
    descriptions_[index] = it->second;
    possible_lengths_.insert(static_cast<int>(log10(it->first)) + 1);
  }
}

int DefaultMapStorage::GetNumOfEntries() const {
  return prefixes_.size();
}

const set<int>& DefaultMapStorage::GetPossibleLengths() const {
  return possible_lengths_;
}

}  // namespace phonenumbers
}  // namespace i18n
