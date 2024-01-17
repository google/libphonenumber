// Copyright (C) 2013 The Libphonenumber Authors
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

#ifndef I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_POSIX_H_
#define I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_POSIX_H_

#include <pthread.h>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/logging.h"

namespace i18n {
namespace phonenumbers {

class Lock {
 public:
  Lock() {
    const int ret = pthread_mutex_init(&mutex_, NULL);
    (void) ret;
    DCHECK_EQ(0, ret);
  }

  // This type is neither copyable nor movable.
  Lock(const Lock&) = delete;
  Lock& operator=(const Lock&) = delete;

  ~Lock() {
    const int ret = pthread_mutex_destroy(&mutex_);
    (void) ret;
    DCHECK_EQ(0, ret);
  }

  void Acquire() const {
    int ret = pthread_mutex_lock(&mutex_);
    (void) ret;
    DCHECK_EQ(0, ret);
  }

  void Release() const {
    int ret = pthread_mutex_unlock(&mutex_);
    (void) ret;
    DCHECK_EQ(0, ret);
  }

 private:
  mutable pthread_mutex_t mutex_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_POSIX_H_
