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

// The RE2Cache provides an interface to store RE2 objects in some kind of
// cache. Currently, it doesn't do any caching at all but just provides the
// interface. TODO: Implement caching. ;-)

#ifndef I18N_PHONENUMBERS_RE2_CACHE_H_
#define I18N_PHONENUMBERS_RE2_CACHE_H_

#include <cstddef>
#include <string>

#include "base/scoped_ptr.h"

namespace re2 {
class RE2;
}  // namespace re2

namespace i18n {
namespace phonenumbers {

using re2::RE2;
using std::string;

class RE2Cache {
 public:
  explicit RE2Cache(size_t max_items);
  ~RE2Cache();

  class ScopedAccess {
   public:
    ScopedAccess(RE2Cache* cache, const string& pattern);
    ~ScopedAccess();
    operator const RE2&() const { return *regexp_; }

   private:
    const string pattern_;
    scoped_ptr<const RE2> regexp_;
    friend class RE2CacheTest_AccessConstructor_Test;
  };
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_RE2_CACHE_H_
