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

#ifndef I18N_PHONENUMBERS_LOGGER_H_
#define I18N_PHONENUMBERS_LOGGER_H_

#include <cstdio>
#include <string>

namespace i18n {
namespace phonenumbers {

using std::string;

enum {
  LOG_FATAL = 1,
  LOG_ERROR,
  LOG_WARNING,
  LOG_INFO,
  LOG_DEBUG,
};

enum {
  DFATAL = LOG_FATAL,
// ERROR seems to be defined on MSVC, therefore don't overwrite it.
#ifndef ERROR
  ERROR = LOG_ERROR,
#endif
  WARNING = LOG_WARNING,
};

// Subclass this abstract class to override the way logging is handled in the
// library. You can then call the PhoneNumberUtil::SetLogger() method.
class Logger {
 public:
  Logger() : level_(LOG_ERROR) {}
  virtual ~Logger() {}

  // Writes the message level to the underlying output stream.
  virtual void WriteLevel() {}
  // Writes the provided message to the underlying output stream.
  virtual void WriteMessage(const string& msg) = 0;

  // Note that if set_verbosity_level has been used to set the level to a value
  // that is not represented by an enum, the result here will be a log
  // level that is higher than LOG_DEBUG.
  inline int level() const {
    return level_;
  }

  inline void set_level(int level) {
    level_ = level;
  }

  // If you want to see verbose logs in addition to other logs, use this method.
  // This will result in all log messages at the levels above being shown, along
  // with calls to VLOG with the verbosity level set to this level or lower.
  // For example, set_verbosity_level(2) will show calls of VLOG(1) and VLOG(2)
  // but not VLOG(3), along with all calls to LOG().
  inline void set_verbosity_level(int verbose_logs_level) {
    set_level(LOG_DEBUG + verbose_logs_level);
  }

  static inline Logger* set_logger_impl(Logger* logger) {
    impl_ = logger;
    return logger;
  }

  static inline Logger* mutable_logger_impl() {
    return impl_;
  }

 private:
  static Logger* impl_;
  int level_;
};

// Logger that does not log anything. It could be useful to "mute" the
// phonenumber library.
class NullLogger : public Logger {
 public:
  virtual ~NullLogger() {}

  virtual void WriteMessage(const string& /* msg */) {}
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_LOGGER_ADAPTER_H_
