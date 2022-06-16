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

#ifndef I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_WIN32_H_
#define I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_WIN32_H_

#include <windows.h>
#include <synchapi.h>

#include "phonenumbers/base/basictypes.h"

namespace i18n {
namespace phonenumbers {

template <class T>
class Singleton {
 public:
  Singleton() {}
  virtual ~Singleton() {}

  static T* GetInstance() {
    if (once_init_) {
      EnterCriticalSection(&critical_section_);
      if (once_init_) {
        Init();
        once_init_ = false;
      }
      LeaveCriticalSection(&critical_section_);
    }
    return instance_;
  }

 private:
  DISALLOW_COPY_AND_ASSIGN(Singleton);

  static void Init() {
    instance_ = new T();
  }

  static T* instance_;  // Leaky singleton.
  static CRITICAL_SECTION critical_section_;
  static bool once_init_;
};

static bool perform_init_crit(CRITICAL_SECTION& cs)
{
  InitializeCriticalSection(&cs);
  return true;
}

template <class T> T* Singleton<T>::instance_;
template <class T> CRITICAL_SECTION Singleton<T>::critical_section_;
template <class T> bool Singleton<T>::once_init_=perform_init_crit(Singleton<T>::critical_section_);

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_WIN32_H_
