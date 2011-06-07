// Copyright (C) 2011 Google Inc.
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

// Author: Fredrik Roubert

// RegExpCache is a simple wrapper around hash_map<> to store RegExp objects.
//
// To get a cached RegExp object for a regexp pattern string, call the
// GetRegExp() method of the class RegExpCache providing the pattern string. If
// a RegExp object corresponding to the pattern string doesn't already exist, it
// will be created by the GetRegExp() method.
//
// RegExpCache cache;
// const RegExp& regexp = cache.GetRegExp("\d");

#ifndef I18N_PHONENUMBERS_REGEXP_CACHE_H_
#define I18N_PHONENUMBERS_REGEXP_CACHE_H_

#include <cstddef>
#include <string>

#include "base/basictypes.h"
#include "base/scoped_ptr.h"
#include "base/synchronization/lock.h"

#ifdef USE_TR1_UNORDERED_MAP
#  include <tr1/unordered_map>
#elif defined(USE_HASH_MAP)
#  ifndef __DEPRECATED
#    define __DEPRECATED
#  endif
#  include <hash_map>
#else
#  error STL map type unsupported on this platform!
#endif

namespace i18n {
namespace phonenumbers {

using std::string;

class RegExp;

class RegExpCache {
 private:
#ifdef USE_TR1_UNORDERED_MAP
  typedef std::tr1::unordered_map<string, const RegExp*> CacheImpl;
#elif defined(USE_HASH_MAP)
  typedef std::hash_map<string, const RegExp*> CacheImpl;
#endif

 public:
  explicit RegExpCache(size_t min_items);
  ~RegExpCache();

  const RegExp& GetRegExp(const string& pattern);

 private:
  base::Lock lock_;  // protects cache_impl_
  scoped_ptr<CacheImpl> cache_impl_;  // protected by lock_
  friend class RegExpCacheTest_CacheConstructor_Test;
  friend class RegExpCacheTest_AccessConstructor_Test;
  DISALLOW_COPY_AND_ASSIGN(RegExpCache);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_REGEXP_CACHE_H_
