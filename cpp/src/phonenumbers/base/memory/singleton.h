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

#ifndef I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_H_
#define I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_H_

#if defined(I18N_PHONENUMBERS_USE_BOOST)
#include "phonenumbers/base/memory/singleton_boost.h"
#elif (__cplusplus >= 201103L) && defined(I18N_PHONENUMBERS_USE_STDMUTEX)
// C++11 Lock implementation based on std::mutex.
#include "phonenumbers/base/memory/singleton_stdmutex.h"
#elif defined(__linux__) || defined(__APPLE__) || defined(I18N_PHONENUMBERS_HAVE_POSIX_THREAD)
#include "phonenumbers/base/memory/singleton_posix.h"
#elif defined(WIN32)
#include "phonenumbers/base/memory/singleton_win32.h"
#else
#include "phonenumbers/base/memory/singleton_unsafe.h"
#endif  // !I18N_PHONENUMBERS_USE_BOOST

#endif  // I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_H_
