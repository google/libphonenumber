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

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/base/synchronization/lock.h"

#ifdef I18N_PHONENUMBERS_USE_TR1_UNORDERED_MAP
#  include <tr1/unordered_map>
#else
#  include <map>
#endif

namespace i18n {
namespace phonenumbers {

using std::string;

class AbstractRegExpFactory;
class RegExp;

class RegExpCache {
 private:
#ifdef I18N_PHONENUMBERS_USE_TR1_UNORDERED_MAP
  typedef std::tr1::unordered_map<string, const RegExp*> CacheImpl;
#else
  typedef std::map<string, const RegExp*> CacheImpl;
#endif

 public:
  explicit RegExpCache(const AbstractRegExpFactory& regexp_factory,
                       size_t min_items);
  // This type is neither copyable nor movable.
  RegExpCache(const RegExpCache&) = delete;
  RegExpCache& operator=(const RegExpCache&) = delete;

  ~RegExpCache();

  const RegExp& GetRegExp(const string& pattern);

 private:
  const AbstractRegExpFactory& regexp_factory_;
  Lock lock_;  // protects cache_impl_
  scoped_ptr<CacheImpl> cache_impl_;  // protected by lock_
  friend class RegExpCacheTest_CacheConstructor_Test;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_REGEXP_CACHE_H_
