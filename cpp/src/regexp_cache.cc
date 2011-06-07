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

#include "regexp_cache.h"

#include <cstddef>
#include <string>
#include <utility>

#include "base/logging.h"
#include "base/synchronization/lock.h"
#include "regexp_adapter.h"

using std::string;

#ifdef USE_HASH_MAP

// A basic text book string hash function implementation, this one taken from
// The Practice of Programming (Kernighan and Pike 1999). It could be a good
// idea in the future to evaluate how well it actually performs and possibly
// switch to another hash function better suited to this particular use case.
namespace __gnu_cxx {
template<> struct hash<string> {
  enum { MULTIPLIER = 31 };
  size_t operator()(const string& key) const {
    size_t h = 0;
    for (const char* p = key.c_str(); *p != '\0'; ++p) {
      h *= MULTIPLIER;
      h += *p;
    }
    return h;
  }
};
}  // namespace __gnu_cxx

#endif

namespace i18n {
namespace phonenumbers {

using base::AutoLock;

RegExpCache::RegExpCache(size_t min_items)
    : cache_impl_(new CacheImpl(min_items)) {}

RegExpCache::~RegExpCache() {
  AutoLock l(lock_);
  for (CacheImpl::const_iterator
       it = cache_impl_->begin(); it != cache_impl_->end(); ++it) {
    delete it->second;
  }
}

const RegExp& RegExpCache::GetRegExp(const string& pattern) {
  AutoLock l(lock_);
  CacheImpl::const_iterator it = cache_impl_->find(pattern);
  if (it != cache_impl_->end()) return *it->second;

  const RegExp* regexp = RegExp::Create(pattern);
  cache_impl_->insert(make_pair(pattern, regexp));
  return *regexp;
}

}  // namespace phonenumbers
}  // namespace i18n
