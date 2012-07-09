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
//
// Author: Patrick Mezard

#ifndef I18N_PHONENUMBERS_GENERATE_GEOCODING_DATA_H
#define I18N_PHONENUMBERS_GENERATE_GEOCODING_DATA_H

#include <string>

namespace i18n {
namespace phonenumbers {

using std::string;

string MakeStringLiteral(const string& s);

string ReplaceAll(const string& input, const string& pattern,
                  const string& value);

int Main(int argc, const char* argv[]);

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_GENERATE_GEOCODING_DATA_H
