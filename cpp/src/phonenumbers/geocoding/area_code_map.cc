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

#include "phonenumbers/geocoding/area_code_map.h"

#include <cstddef>
#include <iterator>
#include <set>

#include "phonenumbers/geocoding/area_code_map_storage_strategy.h"
#include "phonenumbers/geocoding/default_map_storage.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/phonenumberutil.h"
#include "phonenumbers/stringutil.h"

namespace i18n {
namespace phonenumbers {

AreaCodeMap::AreaCodeMap()
  : phone_util_(*PhoneNumberUtil::GetInstance()) {
}

AreaCodeMap::~AreaCodeMap() {
}

AreaCodeMapStorageStrategy* AreaCodeMap::CreateDefaultMapStorage() const {
  return new DefaultMapStorage();
}

void AreaCodeMap::ReadAreaCodeMap(const map<int, string>& area_codes) {
  AreaCodeMapStorageStrategy* storage = CreateDefaultMapStorage();
  storage->ReadFromMap(area_codes);
  storage_.reset(storage);
}

const string* AreaCodeMap::Lookup(const PhoneNumber& number) const {
  const int entries = storage_->GetNumOfEntries();
  if (!entries) {
    return NULL;
  }

  string national_number;
  phone_util_.GetNationalSignificantNumber(number, &national_number);
  int64 phone_prefix;
  safe_strto64(SimpleItoa(number.country_code()) + national_number,
               &phone_prefix);

  const set<int>& lengths = storage_->GetPossibleLengths();
  int current_index = entries - 1;
  for (set<int>::const_reverse_iterator lengths_it = lengths.rbegin();
       lengths_it != lengths.rend(); ++lengths_it) {
    const int possible_length = *lengths_it;
    string phone_prefix_str = SimpleItoa(phone_prefix);
    if (static_cast<int>(phone_prefix_str.length()) > possible_length) {
      safe_strto64(phone_prefix_str.substr(0, possible_length), &phone_prefix);
    }
    current_index = BinarySearch(0, current_index, phone_prefix);
    if (current_index < 0) {
      return NULL;
    }
    const int current_prefix = storage_->GetPrefix(current_index);
    if (phone_prefix == current_prefix) {
      return &storage_->GetDescription(current_index);
    }
  }
  return NULL;
}

int AreaCodeMap::BinarySearch(int start, int end, int64 value) const {
  int current = 0;
  while (start <= end) {
    current = (start + end) / 2;
    int current_value = storage_->GetPrefix(current);
    if (current_value == value) {
      return current;
    } else if (current_value > value) {
      --current;
      end = current;
    } else {
      start = current + 1;
    }
  }
  return current;
}

}  // namespace phonenumbers
}  // namespace i18n
