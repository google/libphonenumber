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

#include <cstdint>
#include "phonenumbers/base/basictypes.h"

namespace i18n {
namespace phonenumbers {

struct PrefixDescriptions;

// Default area code map storage strategy that is used for data not
// containing description duplications. It is mainly intended to avoid
// the overhead of the string table management when it is actually
// unnecessary (i.e no string duplication).
class DefaultMapStorage {
 public:
  DefaultMapStorage();

   // This type is neither copyable nor movable.
  DefaultMapStorage(const DefaultMapStorage&) = delete;
  DefaultMapStorage& operator=(const DefaultMapStorage&) = delete;

  virtual ~DefaultMapStorage();

  // Returns the phone number prefix located at the provided index.
  int32_t GetPrefix(int index) const;

  // Gets the description corresponding to the phone number prefix located
  // at the provided index. If the description is not available in the current
  // language an empty string is returned.
  const char* GetDescription(int index) const;

  // Sets the internal state of the underlying storage implementation from the
  // provided area_codes that maps phone number prefixes to description strings.
  void ReadFromMap(const PrefixDescriptions* descriptions);

  // Returns the number of entries contained in the area code map.
  int GetNumOfEntries() const;

  // Returns an array containing the possible lengths of prefixes sorted in
  // ascending order.
  const int* GetPossibleLengths() const;

  // Returns the number of elements in GetPossibleLengths() array.
  int GetPossibleLengthsSize() const;

 private:
  // Sorted sequence of phone number prefixes.
  const int32_t* prefixes_;
  int prefixes_size_;
  // Sequence of prefix descriptions, in the same order than prefixes_.
  const char** descriptions_;
  // Sequence of unique possible lengths in ascending order.
  const int32_t* possible_lengths_;
  int possible_lengths_size_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif /* I18N_PHONENUMBERS_DEFAULT_MAP_STORAGE_H_ */
