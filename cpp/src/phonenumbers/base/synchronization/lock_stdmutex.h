// Copyright (C) 2020 The Libphonenumber Authors
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

// Author: Philippe Liard

#ifndef I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_STDMUTEX_H_
#define I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_STDMUTEX_H_

#include <mutex>

#include "phonenumbers/base/basictypes.h"

namespace i18n {
namespace phonenumbers {

class Lock {
 public:
  Lock() = default;

  void Acquire() const {
    mutex_.lock();
  }

  void Release() const {
    mutex_.unlock();
  }

 private:
  DISALLOW_COPY_AND_ASSIGN(Lock);
  mutable std::mutex mutex_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_STDMUTEX_H_
