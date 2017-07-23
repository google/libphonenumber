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

#include <iostream>

#include "phonenumbers/default_logger.h"

namespace i18n {
namespace phonenumbers {

void StdoutLogger::WriteMessage(const string& msg) {
  std::cout << " " << msg;
}

void StdoutLogger::WriteLevel() {
  int verbosity_level = level();
  if (verbosity_level <= 0) {
    verbosity_level = LOG_FATAL;
  }

  std::cout << "[";

  // Handle verbose logs first.
  if (verbosity_level > LOG_DEBUG) {
    std::cout << "VLOG" << (verbosity_level - LOG_DEBUG);
  } else {
    switch (verbosity_level) {
      case LOG_FATAL:   std::cout << "FATAL"; break;
#ifdef ERROR  // In case ERROR is defined by MSVC (i.e not set to LOG_ERROR).
      case ERROR:
#endif
      case LOG_ERROR:   std::cout << "ERROR"; break;
      case LOG_WARNING: std::cout << "WARNING"; break;
      case LOG_INFO:    std::cout << "INFO"; break;
      case LOG_DEBUG:   std::cout << "DEBUG"; break;
    }
  }
  std::cout << "]";
}

}  // namespace phonenumbers
}  // namespace i18n
