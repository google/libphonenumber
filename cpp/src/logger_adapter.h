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

// Author: Philippe Liard

#ifndef I18N_PHONENUMBERS_LOGGER_ADAPTER_H_
#define I18N_PHONENUMBERS_LOGGER_ADAPTER_H_

#include <string>

using std::string;

namespace i18n {
namespace phonenumbers {

// Implement this 'interface' to override the way logging is handled
// in the library.
class LoggerAdapter {
 public:
  virtual ~LoggerAdapter();

  // Logging methods
  virtual void Fatal(const string& msg) const = 0;

  virtual void Error(const string& msg) const = 0;

  virtual void Warning(const string& msg) const = 0;

  virtual void Info(const string& msg) const = 0;

  virtual void Debug(const string& msg) const = 0;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_LOGGER_ADAPTER_H_
