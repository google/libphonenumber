/*
 * Copyright (C) 2014 The Libphonenumber Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef I18N_PHONENUMBERS_MATCHER_API_H_
#define I18N_PHONENUMBERS_MATCHER_API_H_

#include <string>

namespace i18n {
namespace phonenumbers {

using std::string;

class PhoneNumberDesc;

// Internal phonenumber matching API used to isolate the underlying
// implementation of the matcher and allow different implementations to be
// swapped in easily.
class MatcherApi {
 public:
  virtual ~MatcherApi() {}

  // Returns whether the given national number (a string containing only decimal
  // digits) matches the national number pattern defined in the given
  // PhoneNumberDesc message.
  virtual bool MatchNationalNumber(const string& number,
                                   const PhoneNumberDesc& number_desc,
                                   bool allow_prefix_match) const = 0;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_MATCHER_API_H_
