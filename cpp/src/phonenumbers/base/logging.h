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

// This file provides a minimalist implementation of common macros.

#ifndef I18N_PHONENUMBERS_BASE_LOGGING_H_
#define I18N_PHONENUMBERS_BASE_LOGGING_H_

#include <cassert>

#if !defined(CHECK_EQ)
#define CHECK_EQ(X, Y) assert((X) == (Y))
#endif

#if !defined(DCHECK)
#define DCHECK(X) assert(X)
#define DCHECK_EQ(X, Y) CHECK_EQ((X), (Y))
#define DCHECK_GE(X, Y) assert((X) >= (Y))
#define DCHECK_GT(X, Y) assert((X) > (Y))
#define DCHECK_LT(X, Y) assert((X) < (Y))
#endif

template <typename T> T* CHECK_NOTNULL(T* ptr) {
  assert(ptr);
  return ptr;
}

#if !defined(IGNORE_UNUSED)
#define IGNORE_UNUSED(X) (void)(X)
#endif 

#endif  // I18N_PHONENUMBERS_BASE_LOGGING_H_
