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

// C++11 Lock implementation based on std::mutex.
#if  __cplusplus>=201103L
#include <mutex>

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
  mutable std::mutex mutex_;
};

}  // namespace phonenumbers
}  // namespace i18n

// Dummy lock implementation on non-POSIX platforms. If you are running on a
// different platform and care about thread-safety, please compile with
// -DI18N_PHONENUMBERS_USE_BOOST.
#elif !defined(__linux__) && !defined(__APPLE__)

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
  const ThreadChecker thread_checker_;
};

}  // namespace phonenumbers
}  // namespace i18n

#else
#include "phonenumbers/base/synchronization/lock_posix.h"
#endif

namespace i18n {
namespace phonenumbers {

class AutoLock {
 public:
  AutoLock(Lock& lock) : lock_(lock) {
    lock_.Acquire();
  }

  ~AutoLock() {
    lock_.Release();
  }

 private:
  Lock& lock_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_USE_BOOST
#endif  // I18N_PHONENUMBERS_BASE_SYNCHRONIZATION_LOCK_H_
