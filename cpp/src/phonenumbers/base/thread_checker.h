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

#ifndef I18N_PHONENUMBERS_BASE_THREAD_CHECKER_H_
#define I18N_PHONENUMBERS_BASE_THREAD_CHECKER_H_

#if !defined(I18N_PHONENUMBERS_USE_BOOST)

// Note that I18N_PHONENUMBERS_NO_THREAD_SAFETY must be defined only to let the
// user of the library know that it can't be used in a thread-safe manner when
// it is not depending on Boost.
#if !defined(__linux__) && !defined(__APPLE__) && !defined(I18N_PHONENUMBERS_HAVE_POSIX_THREAD) && \
    !defined(I18N_PHONENUMBERS_NO_THREAD_SAFETY) && \
	!((__cplusplus >= 201103L) && defined(I18N_PHONENUMBERS_USE_STDMUTEX)) && \
	!defined(WIN32)
#error Building without Boost, please provide \
       -DI18N_PHONENUMBERS_NO_THREAD_SAFETY
#endif

#endif

#if !defined(NDEBUG) && !defined(I18N_PHONENUMBERS_USE_BOOST) && \
    (defined(__linux__) || defined(__APPLE__) || defined(I18N_PHONENUMBERS_HAVE_POSIX_THREAD))

#include <pthread.h>

namespace i18n {
namespace phonenumbers {

class ThreadChecker {
 public:
  ThreadChecker() : thread_id_(pthread_self()) {}

  bool CalledOnValidThread() const {
    return thread_id_ == pthread_self();
  }

 private:
  const pthread_t thread_id_;
};

#else

namespace i18n {
namespace phonenumbers {

class ThreadChecker {
 public:
  bool CalledOnValidThread() const {
    return true;
  }
};

#endif

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_THREAD_CHECKER_H_
