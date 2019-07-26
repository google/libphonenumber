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

#ifndef I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_POSIX_H_
#define I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_POSIX_H_

#include <pthread.h>

#include "phonenumbers/base/logging.h"

namespace i18n {
namespace phonenumbers {

template <class T>
class Singleton {
 public:
  virtual ~Singleton() {}

  static T* GetInstance() {
    const int ret = pthread_once(&once_control_, &Init);
    (void) ret;
    DCHECK_EQ(0, ret);
    return instance_;
  }

 private:
  static void Init() {
    instance_ = new T();
  }

  static T* instance_;  // Leaky singleton.
  static pthread_once_t once_control_;
};

template <class T> T* Singleton<T>::instance_;
template <class T> pthread_once_t Singleton<T>::once_control_ =
    PTHREAD_ONCE_INIT;

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_POSIX_H_
