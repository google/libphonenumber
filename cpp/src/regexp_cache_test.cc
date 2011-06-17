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

#include <cstddef>
#include <string>

#include <gtest/gtest.h>

#include "regexp_adapter.h"
#include "regexp_cache.h"

namespace i18n {
namespace phonenumbers {

using std::string;

class RegExpCacheTest : public testing::Test {
 protected:
  static const size_t min_items_ = 2;

  RegExpCacheTest() : cache_(min_items_) {}
  virtual ~RegExpCacheTest() {}

  RegExpCache cache_;
};

TEST_F(RegExpCacheTest, CacheConstructor) {
  ASSERT_TRUE(cache_.cache_impl_ != NULL);
  EXPECT_TRUE(cache_.cache_impl_->empty());
}

TEST_F(RegExpCacheTest, GetRegExp) {
  static const string pattern1("foo");
  static const string pattern2("foo");

  const RegExp& regexp1 = cache_.GetRegExp(pattern1);
  // "foo" has been cached therefore we must get the same object.
  const RegExp& regexp2 = cache_.GetRegExp(pattern2);

  EXPECT_TRUE(&regexp1 == &regexp2);
}

}  // namespace phonenumbers
}  // namespace i18n
