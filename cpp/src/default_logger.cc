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

#include <iostream>

#include "default_logger.h"

using std::cerr;
using std::cout;
using std::endl;

namespace i18n {
namespace phonenumbers {

DefaultLogger::DefaultLogger(LogLevel level) : level_(level) {}

DefaultLogger::~DefaultLogger() {}

void DefaultLogger::Fatal(const string& msg) const {
  if (level_ >= FATAL) {
    cerr << "FATAL libphonenumber " << msg << endl;
  }
}

void DefaultLogger::Error(const string& msg) const {
  if (level_ >= ERROR) {
    cerr << "ERROR libphonenumber " << msg << endl;
  }
}

void DefaultLogger::Warning(const string& msg) const {
  if (level_ >= WARNING) {
    cerr << "WARNING libphonenumber " << msg << endl;
  }
}

void DefaultLogger::Info(const string& msg) const {
  if (level_ >= INFO) {
    cout << "INFO libphonenumber " << msg << endl;
  }
}

void DefaultLogger::Debug(const string& msg) const {
  if (level_ >= DEBUG) {
    cout << "DEBUG libphonenumber " << msg << endl;
  }
}

}  // namespace phonenumbers
}  // namespace i18n
