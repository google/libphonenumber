// Copyright (C) 2011 The Libphonenumber Authors
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

// Author: Philippe Liard

#ifndef I18N_PHONENUMBERS_REGEXP_ADAPTER_RE2_H_
#define I18N_PHONENUMBERS_REGEXP_ADAPTER_RE2_H_

#include <string>

#include "phonenumbers/regexp_adapter.h"

namespace i18n {
namespace phonenumbers {

// RE2 regexp factory that lets the user instantiate the underlying
// implementation of RegExp and RegExpInput classes based on RE2.
class RE2RegExpFactory : public AbstractRegExpFactory {
 public:
  virtual ~RE2RegExpFactory() {}

  virtual RegExpInput* CreateInput(const string& utf8_input) const;
  virtual RegExp* CreateRegExp(const string& utf8_regexp) const;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_REGEXP_ADAPTER_RE2_H_
