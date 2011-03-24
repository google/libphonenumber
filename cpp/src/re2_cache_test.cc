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

#include <cstddef>
#include <string>

#include <gtest/gtest.h>
#include <re2/re2.h>

#include "re2_cache.h"

namespace i18n {
namespace phonenumbers {

using std::string;

class RE2CacheTest : public testing::Test {
 protected:
  static const size_t min_items_ = 2;

  RE2CacheTest() : cache_(min_items_) {}
  virtual ~RE2CacheTest() {}

  RE2Cache cache_;
};

TEST_F(RE2CacheTest, CacheConstructor) {
  ASSERT_TRUE(cache_.cache_impl_ != NULL);
  EXPECT_TRUE(cache_.cache_impl_->empty());
}

TEST_F(RE2CacheTest, AccessConstructor) {
  static const string foo("foo");
  RE2Cache::ScopedAccess access(&cache_, foo);

  EXPECT_TRUE(access.regexp_ != NULL);
  ASSERT_TRUE(cache_.cache_impl_ != NULL);
  EXPECT_EQ(1, cache_.cache_impl_->size());
}

TEST_F(RE2CacheTest, OperatorRE2) {
  static const string foo("foo");
  RE2Cache::ScopedAccess access(&cache_, foo);

  const RE2& regexp = access;
  EXPECT_EQ(foo, regexp.pattern());
}

}  // namespace phonenumbers
}  // namespace i18n
