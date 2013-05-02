// Copyright (C) 2011 The Libphonenumber Authors
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

#ifndef I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_H_
#define I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_H_

#if defined(I18N_PHONENUMBERS_USE_BOOST)
#include <boost/thread/mutex.hpp>

namespace i18n {
namespace phonenumbers {

typedef boost::mutex Lock;
typedef boost::mutex::scoped_lock AutoLock;

}  // namespace phonenumbers
}  // namespace i18n

#else  // I18N_PHONENUMBERS_USE_BOOST

#include "phonenumbers/base/logging.h"
#include "phonenumbers/base/thread_checker.h"

namespace i18n {
namespace phonenumbers {

// Dummy lock implementation. If you care about thread-safety, please compile
// with -DI18N_PHONENUMBERS_USE_BOOST.
class Lock {
 public:
  Lock() : thread_checker_() {}

  void Acquire() const {
    DCHECK(thread_checker_.CalledOnValidThread());
  }

  // No need for Release() since Acquire() is a no-op and Release() is not used
  // in the codebase.

 private:
  const ThreadChecker thread_checker_;
};

class AutoLock {
 public:
  AutoLock(Lock& lock) {
    lock.Acquire();
  }
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_USE_BOOST
#endif  // I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_H_
