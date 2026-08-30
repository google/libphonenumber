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

#include <string>

namespace i18n {
namespace phonenumbers {

using std::string;

// Log levels used to control the verbosity of logging output.
enum LogLevel {
  LOG_FATAL = 1,  // Critical errors that terminate the program
  LOG_ERROR,      // Non-critical errors
  LOG_WARNING,    // Potential issues or warnings
  LOG_INFO,       // Informational messages
  LOG_DEBUG,      // Debugging messages
};

// Aliases for log levels to improve readability and compatibility.
enum {
  DFATAL = LOG_FATAL,
#ifndef ERROR  // Avoid redefinition if ERROR is defined (e.g., in MSVC)
  ERROR = LOG_ERROR,
#endif
  WARNING = LOG_WARNING,
};

// Abstract base class for custom logging implementations.
// Subclass this to define how logs are handled and use PhoneNumberUtil::SetLogger() to apply it.
class Logger {
 public:
  Logger() : level_(LOG_WARNING) {}  // Default level set to LOG_WARNING for broader visibility
  virtual ~Logger() {}

  // Writes the log level prefix to the output stream (optional override).
  virtual void WriteLevel() {}

  // Writes the provided message to the output stream (must be implemented by subclasses).
  virtual void WriteMessage(const string& msg) = 0;

  // Returns the current log level.
  inline int level() const {
    return level_;
  }

  // Sets the log level to control which messages are displayed.
  inline void set_level(int level) {
    level_ = level;
  }

  // Sets verbosity level for detailed logging. Shows all messages up to LOG_DEBUG plus
  // verbose logs up to the specified level (e.g., set_verbosity_level(2) shows VLOG(1) and VLOG(2)).
  inline void set_verbosity_level(int verbose_logs_level) {
    set_level(LOG_DEBUG + verbose_logs_level);
  }

  // Sets the global logger instance and returns it.
  static inline Logger* set_logger_impl(Logger* logger) {
    impl_ = logger;
    return logger;
  }

  // Retrieves the current global logger instance.
  static inline Logger* mutable_logger_impl() {
    return impl_;
  }

 private:
  static Logger* impl_;  // Global logger instance
  int level_;            // Current log level
};

// A logger that discards all messages, useful for muting logs in the phonenumber library.
class NullLogger : public Logger {
 public:
  virtual ~NullLogger() {}

  virtual void WriteMessage(const string& /* msg */) {}  // Does nothing
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_LOGGER_H_