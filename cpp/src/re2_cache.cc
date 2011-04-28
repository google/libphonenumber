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

#include "re2_cache.h"

#include <cstddef>
#include <string>
#include <utility>

#include <re2/re2.h>

#include "base/logging.h"
#include "base/synchronization/lock.h"

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

RE2Cache::RE2Cache(size_t min_items) : cache_impl_(new CacheImpl(min_items)) {}
RE2Cache::~RE2Cache() {
  base::AutoLock l(lock_);
  LOG(2) << "Cache entries upon destruction: " << cache_impl_->size();
  for (CacheImpl::const_iterator
       it = cache_impl_->begin(); it != cache_impl_->end(); ++it) {
    delete it->second;
  }
}

RE2Cache::ScopedAccess::ScopedAccess(RE2Cache* cache, const string& pattern) {
  DCHECK(cache);
  base::AutoLock l(cache->lock_);
  CacheImpl* const cache_impl = cache->cache_impl_.get();
  CacheImpl::const_iterator it = cache_impl->find(pattern);
  if (it != cache_impl->end()) {
    regexp_ = it->second;
  } else {
    regexp_ = new RE2(pattern);
    cache_impl->insert(make_pair(pattern, regexp_));
  }
}

}  // namespace phonenumbers
}  // namespace i18n
