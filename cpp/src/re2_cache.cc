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

#include <re2/re2.h>

namespace i18n {
namespace phonenumbers {

using std::string;

RE2Cache::RE2Cache(size_t /*max_items*/) {}
RE2Cache::~RE2Cache() {}

RE2Cache::ScopedAccess::ScopedAccess(RE2Cache* /*cache*/, const string& pattern)
    : pattern_(pattern), regexp_(new RE2(pattern_)) {}

RE2Cache::ScopedAccess::~ScopedAccess() {}

}  // namespace phonenumbers
}  // namespace i18n
