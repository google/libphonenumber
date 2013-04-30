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

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/logging.h"
#include "phonenumbers/geocoding/geocoding_data.h"

namespace i18n {
namespace phonenumbers {

DefaultMapStorage::DefaultMapStorage() {
}

DefaultMapStorage::~DefaultMapStorage() {
}

int32 DefaultMapStorage::GetPrefix(int index) const {
  DCHECK_GE(index, 0);
  DCHECK_LT(index, prefixes_size_);
  return prefixes_[index];
}

const char* DefaultMapStorage::GetDescription(int index) const {
  DCHECK_GE(index, 0);
  DCHECK_LT(index, prefixes_size_);
  return descriptions_[index];
}

void DefaultMapStorage::ReadFromMap(const PrefixDescriptions* descriptions) {
  prefixes_ = descriptions->prefixes;
  prefixes_size_ = descriptions->prefixes_size;
  descriptions_ = descriptions->descriptions;
  possible_lengths_ = descriptions->possible_lengths;
  possible_lengths_size_ = descriptions->possible_lengths_size;
}

int DefaultMapStorage::GetNumOfEntries() const {
  return prefixes_size_;
}

const int* DefaultMapStorage::GetPossibleLengths() const {
  return possible_lengths_;
}

int DefaultMapStorage::GetPossibleLengthsSize() const {
  return possible_lengths_size_;
}

}  // namespace phonenumbers
}  // namespace i18n
