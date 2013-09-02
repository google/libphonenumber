// Copyright (C) 2012 The Libphonenumber Authors
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

// Author: David Yonge-Mallo

#include "phonenumbers/shortnumberutil.h"

#include "phonenumbers/shortnumberinfo.h"

namespace i18n {
namespace phonenumbers {

using std::string;

ShortNumberUtil::ShortNumberUtil() {
}

bool ShortNumberUtil::ConnectsToEmergencyNumber(const string& number,
    const string& region_code) const {
  ShortNumberInfo short_info;
  return short_info.ConnectsToEmergencyNumber(number, region_code);
}

bool ShortNumberUtil::IsEmergencyNumber(const string& number,
    const string& region_code) const {
  ShortNumberInfo short_info;
  return short_info.IsEmergencyNumber(number, region_code);
}

}  // namespace phonenumbers
}  // namespace i18n
