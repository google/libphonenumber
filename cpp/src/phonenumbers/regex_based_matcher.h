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

#ifndef I18N_PHONENUMBERS_REGEX_BASED_MATCHER_H_
#define I18N_PHONENUMBERS_REGEX_BASED_MATCHER_H_

#include <memory>
#include <string>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/matcher_api.h"

namespace i18n {
namespace phonenumbers {

class AbstractRegExpFactory;
class PhoneNumberDesc;
class RegExpCache;

// Implementation of the matcher API using the regular expressions in the
// PhoneNumberDesc proto message to match numbers.
class RegexBasedMatcher : public MatcherApi {
 public:
  RegexBasedMatcher();
  ~RegexBasedMatcher();

  bool MatchesNationalNumber(const string& national_number,
                             const PhoneNumberDesc& number_desc,
                             bool allow_prefix_match) const;

 private:
  bool Match(const string& national_number, const string& number_pattern,
             bool allow_prefix_match) const;

  const scoped_ptr<const AbstractRegExpFactory> regexp_factory_;
  const scoped_ptr<RegExpCache> regexp_cache_;

  DISALLOW_COPY_AND_ASSIGN(RegexBasedMatcher);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_REGEX_BASED_MATCHER_H_
