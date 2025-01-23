// Copyright (C) 2025 The Libphonenumber Authors
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

#include "phonenumbers/phonenumbernormalizer.h"

#include <gtest/gtest.h>

#include "phonenumbers/regexpsandmappings.h"

namespace i18n {
namespace phonenumbers {
using testing::Eq;

class PhoneNumberNormalizerTest : public testing::Test {
 public:
  // This type is neither copyable nor movable.
  PhoneNumberNormalizerTest(const PhoneNumberNormalizerTest&) = delete;
  PhoneNumberNormalizerTest& operator=(const PhoneNumberNormalizerTest&) =
      delete;

 protected:
  PhoneNumberNormalizerTest()
      : reg_exps_(new PhoneNumberRegExpsAndMappings()),
        normalizer_(new PhoneNumberNormalizer(reg_exps_)) {}

  std::shared_ptr<PhoneNumberRegExpsAndMappings> reg_exps_;
  std::shared_ptr<PhoneNumberNormalizer> normalizer_;

  void NormalizeDigitsOnly(std::string* number) {
    normalizer_->NormalizeDigitsOnly(number);
  }
};

TEST_F(PhoneNumberNormalizerTest, NormaliseStripAlphaCharacters) {
  string input_number("034-56&+a#234");
  NormalizeDigitsOnly(&input_number);
  static const string kExpectedOutput("03456234");
  EXPECT_EQ(kExpectedOutput, input_number)
      << "Conversion did not correctly remove alpha characters";
}

}  // namespace phonenumbers
}  // namespace i18n
