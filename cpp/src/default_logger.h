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

#ifndef I18N_PHONENUMBERS_DEFAULT_LOGGER_H_
#define I18N_PHONENUMBERS_DEFAULT_LOGGER_H_

#include "logger_adapter.h"

namespace i18n {
namespace phonenumbers {

enum LogLevel {
  FATAL,
  ERROR,
  WARNING,
  INFO,
  DEBUG,
};

class DefaultLogger : public LoggerAdapter {
 public:
  virtual ~DefaultLogger();

  DefaultLogger(LogLevel level = WARNING);

  virtual void Fatal(const string& msg) const;

  virtual void Error(const string& msg) const;

  virtual void Warning(const string& msg) const;

  virtual void Info(const string& msg) const;

  virtual void Debug(const string& msg) const;

 private:
  LogLevel level_;
};

}  // namespace phonenumbers
}  // namespace i18n

# endif   // I18N_PHONENUMBERS_DEFAULT_LOGGER_H_
