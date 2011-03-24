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

// Author: Fredrik Roubert <roubert@google.com>

// RE2Cache is a simple wrapper around hash_map<> to store RE2 objects.
//
// To get a cached RE2 object for a regexp pattern string, create a ScopedAccess
// object with a pointer to the cache object and the pattern string itself as
// constructor parameters. If an RE2 object corresponding to the pattern string
// doesn't already exist, it will be created by the access object constructor.
// The access object implements operator const RE& and can therefore be passed
// as an argument to any function that expects an RE2 object.
//
// RE2Cache cache;
// RE2Cache::ScopedAccess foo(&cache, "foo");
// bool match = RE2::FullMatch("foobar", foo);

#ifndef I18N_PHONENUMBERS_RE2_CACHE_H_
#define I18N_PHONENUMBERS_RE2_CACHE_H_

#ifdef __DEPRECATED
#undef __DEPRECATED  // Don't warn for using <hash_map>.
#endif

#include <cstddef>
#include <hash_map>
#include <string>

#include "base/scoped_ptr.h"
#include "base/synchronization/lock.h"

namespace re2 {
class RE2;
}  // namespace re2

namespace i18n {
namespace phonenumbers {

using re2::RE2;
using std::string;
using __gnu_cxx::hash_map;

class RE2Cache {
 private:
  typedef hash_map<string, const RE2*> CacheImpl;

 public:
  explicit RE2Cache(size_t min_items);
  ~RE2Cache();

  class ScopedAccess {
   public:
    ScopedAccess(RE2Cache* cache, const string& pattern);
    operator const RE2&() const { return *regexp_; }

   private:
    const RE2* regexp_;
    friend class RE2CacheTest_AccessConstructor_Test;
  };

 private:
  base::Lock lock_;  // protects cache_impl_
  scoped_ptr<CacheImpl> cache_impl_;  // protected by lock_
  friend class RE2CacheTest_CacheConstructor_Test;
  friend class RE2CacheTest_AccessConstructor_Test;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_RE2_CACHE_H_
