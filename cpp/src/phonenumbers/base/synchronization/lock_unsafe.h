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

#ifndef I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_UNSAFE_H_
#define I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_UNSAFE_H_

#include "phonenumbers/base/logging.h"
#include "phonenumbers/base/thread_checker.h"

// Dummy lock implementation on non-POSIX platforms. If you are running on a
// different platform and care about thread-safety, please compile with
// -DI18N_PHONENUMBERS_USE_BOOST.
namespace i18n {
namespace phonenumbers {

class Lock {
 public:
  Lock() {}

  void Acquire() const {
    DCHECK(thread_checker_.CalledOnValidThread());
    IGNORE_UNUSED(thread_checker_);
  }

  void Release() const {
    DCHECK(thread_checker_.CalledOnValidThread());
    IGNORE_UNUSED(thread_checker_);
  }

 private:
  DISALLOW_COPY_AND_ASSIGN(Lock);
  const ThreadChecker thread_checker_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_UNSAFE_H_
