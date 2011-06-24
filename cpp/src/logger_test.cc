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

#include <string>

#include <gtest/gtest.h>

#include "default_logger.h"

namespace i18n {
namespace phonenumbers {

using std::string;

// String logger implementation used for testing. Messages are output to a
// string for convenience.
class StringLogger : public Logger {
 public:
  virtual ~StringLogger() {}

  const string& message() const {
    return msg_;
  }

  virtual void WriteMessage(const string& msg) {
    msg_ += msg;
  }

 private:
  string msg_;
};

class LoggerTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    test_logger_.reset(new StringLogger());
    test_logger_->set_level(LOG_INFO);
    // Save the current logger implementation and restore it when the test is
    // done to avoid side-effects in other tests (including phonenumberutil
    // tests) as the logger implementation is global.
    old_logger_ = Logger::mutable_logger_impl();
    Logger::set_logger_impl(test_logger_.get());
  }

  virtual void TearDown() {
    // Restore the previous logger implementation to avoid side-effects in other
    // tests as mentioned above.
    Logger::set_logger_impl(old_logger_);
  }

  scoped_ptr<StringLogger> test_logger_;
  Logger* old_logger_;
};

TEST_F(LoggerTest, LoggerIgnoresHigherVerbosity) {
  // The logger verbosity is set to LOG_INFO, therefore LOG_DEBUG messages
  // should be ignored.
  VLOG(LOG_DEBUG) << "Hello";
  EXPECT_EQ("", test_logger_->message());
}

TEST_F(LoggerTest, LoggerOutputsNewline) {
  VLOG(LOG_INFO) << "Hello";
  EXPECT_EQ("Hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerLogsEqualVerbosity) {
  VLOG(LOG_INFO) << "Hello";
  EXPECT_EQ("Hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerLogsLowerVerbosity) {
  VLOG(LOG_WARNING) << "Hello";
  EXPECT_EQ("Hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerConcatenatesMessages) {
  VLOG(LOG_INFO) << "Hello";
  ASSERT_EQ("Hello\n", test_logger_->message());

  VLOG(LOG_INFO) << " World";
  EXPECT_EQ("Hello\n World\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerHandlesDifferentTypes) {
  VLOG(LOG_INFO) << "Hello " << 42;
  EXPECT_EQ("Hello 42\n", test_logger_->message());
}

}  // namespace phonenumbers
}  // namespace i18n
