// Copyright (C) 2009 The Libphonenumber Authors
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

#ifndef I18N_PHONENUMBERS_PHONENUMBER_H_
#define I18N_PHONENUMBERS_PHONENUMBER_H_

// Helper functions dealing with PhoneNumber and PhoneNumberDesc comparisons.

namespace i18n {
namespace phonenumbers {

class PhoneNumber;
class PhoneNumberDesc;

// Compares two phone numbers.
bool ExactlySameAs(const PhoneNumber& first_number,
                   const PhoneNumber& second_number);


// Compares two phone number descriptions.
bool ExactlySameAs(const PhoneNumberDesc& first_number_desc,
                   const PhoneNumberDesc& second_number_desc);

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_PHONENUMBER_H_
