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

#ifndef I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_WINDOWS_H_
#define I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_WINDOWS_H_

#include <windows.h>
#include <synchapi.h>

#include "phonenumbers/base/basictypes.h"

namespace i18n {
namespace phonenumbers {

class Lock {
 public:
  Lock() {
    InitializeCriticalSection(&cs_);
  }

  ~Lock() {
    DeleteCriticalSection(&cs_);
  }

  void Acquire() {
    EnterCriticalSection(&cs_);
  }

  void Release() {
    LeaveCriticalSection(&cs_);
  }

 private:
  DISALLOW_COPY_AND_ASSIGN(Lock);
  CRITICAL_SECTION cs_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_POSIX_H_
