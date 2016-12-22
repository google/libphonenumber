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

#ifndef I18N_PHONENUMBERS_DEFAULT_LOGGER_H_
#define I18N_PHONENUMBERS_DEFAULT_LOGGER_H_

#include "phonenumbers/logger.h"

#include <sstream>
#include <string>

namespace i18n {
namespace phonenumbers {

using i18n::phonenumbers::Logger;
using std::stringstream;

// Class template used to inline the right implementation for the T -> string
// conversion.
template <typename T>
struct ConvertToString;

template <typename T>
struct ConvertToString {
  static inline string DoWork(const T& s) {
    return string(s);
  }
};

template <>
struct ConvertToString<int> {
  static inline string DoWork(int n) {
    stringstream stream;
    stream << n;
    string result;
    stream >> result;
    return result;
  }
};

class LoggerHandler {
 public:
  LoggerHandler(Logger* impl) : impl_(impl) {}

  ~LoggerHandler() {
    if (impl_) {
      impl_->WriteMessage("\n");
    }
  }

  template <typename T>
  LoggerHandler& operator<<(const T& value) {
    if (impl_) {
      impl_->WriteMessage(ConvertToString<T>::DoWork(value));
    }
    return *this;
  }

 private:
  Logger* const impl_;
};

inline LoggerHandler LOG(int n) {
  Logger* const logger_impl = Logger::mutable_logger_impl();
  if (logger_impl->level() < n) {
    return LoggerHandler(NULL);
  }
  logger_impl->WriteLevel();
  return LoggerHandler(logger_impl);
}

inline LoggerHandler VLOG(int n) {
  // VLOG(1) is the next logging level after LOG(DEBUG).
  n += LOG_DEBUG;
  return LOG(n);
}

// Default logger implementation used by PhoneNumberUtil class. It outputs the
// messages to the standard output.
class StdoutLogger : public Logger {
 public:
  virtual ~StdoutLogger() {}

  virtual void WriteLevel();
  virtual void WriteMessage(const string& msg);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_DEFAULT_LOGGER_H_
