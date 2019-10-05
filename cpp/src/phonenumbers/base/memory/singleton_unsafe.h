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

#ifndef I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_UNSAFE_H_
#define I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_UNSAFE_H_

#include "phonenumbers/base/logging.h"
#include "phonenumbers/base/thread_checker.h"

namespace i18n {
namespace phonenumbers {

// Note that this implementation is not thread-safe. For a thread-safe
// implementation on non-POSIX platforms, please compile with
// -DI18N_PHONENUMBERS_USE_BOOST.
template <class T>
class Singleton {
 public:
  Singleton() : thread_checker_() {}
  virtual ~Singleton() {}

  static T* GetInstance() {
    static T* instance = NULL;
    if (!instance) {
      instance = new T();
    }
    DCHECK(instance->thread_checker_.CalledOnValidThread());
    return instance;
  }

 private:
  const ThreadChecker thread_checker_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_UNSAFE_H_
