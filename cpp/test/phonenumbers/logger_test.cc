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

#include <string>

#include <gtest/gtest.h>

#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/default_logger.h"
#include "phonenumbers/logger.h"

namespace i18n {
namespace phonenumbers {

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
  LOG(LOG_DEBUG) << "Hello";
  EXPECT_EQ("", test_logger_->message());
}

TEST_F(LoggerTest, LoggerOutputsNewline) {
  LOG(LOG_INFO) << "Hello";
  EXPECT_EQ("Hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerLogsEqualVerbosity) {
  LOG(LOG_INFO) << "Hello";
  EXPECT_EQ("Hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerLogsMoreSeriousMessages) {
  // The logger verbosity is set to LOG_INFO, therefore LOG_WARNING messages
  // should still be printed.
  LOG(LOG_WARNING) << "Hello";
  EXPECT_EQ("Hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerConcatenatesMessages) {
  LOG(LOG_INFO) << "Hello";
  ASSERT_EQ("Hello\n", test_logger_->message());

  LOG(LOG_INFO) << " World";
  EXPECT_EQ("Hello\n World\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerHandlesDifferentTypes) {
  LOG(LOG_INFO) << "Hello " << 42;
  EXPECT_EQ("Hello 42\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerIgnoresVerboseLogs) {
  // VLOG is always lower verbosity than LOG, so with LOG_INFO set as the
  // verbosity level, no VLOG call should result in anything.
  VLOG(1) << "Hello";
  EXPECT_EQ("", test_logger_->message());

  // VLOG(0) is the same as LOG_DEBUG.
  VLOG(0) << "Hello";
  EXPECT_EQ("", test_logger_->message());

  // With LOG_DEBUG as the current verbosity level, VLOG(1) should still not
  // result in anything.
  test_logger_->set_level(LOG_DEBUG);

  VLOG(1) << "Hello";
  EXPECT_EQ("", test_logger_->message());

  // However, VLOG(0) does.
  VLOG(0) << "Hello";
  EXPECT_EQ("Hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerShowsDebugLogsAtDebugLevel) {
  test_logger_->set_level(LOG_DEBUG);
  // Debug logs should still be seen.
  LOG(LOG_DEBUG) << "Debug hello";
  EXPECT_EQ("Debug hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerOutputsDebugLogsWhenVerbositySet) {
  // This should now output LOG_DEBUG.
  int verbose_log_level = 2;
  test_logger_->set_verbosity_level(verbose_log_level);

  LOG(LOG_DEBUG) << "Debug hello";
  EXPECT_EQ("Debug hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerOutputsErrorLogsWhenVerbositySet) {
  // This should now output LOG_ERROR.
  int verbose_log_level = 2;
  test_logger_->set_verbosity_level(verbose_log_level);

  LOG(ERROR) << "Error hello";
  EXPECT_EQ("Error hello\n", test_logger_->message());
}

TEST_F(LoggerTest, LoggerOutputsLogsAccordingToVerbosity) {
  int verbose_log_level = 2;
  test_logger_->set_verbosity_level(verbose_log_level);

  // More verbose than the current limit.
  VLOG(verbose_log_level + 1) << "Hello 3";
  EXPECT_EQ("", test_logger_->message());

  // Less verbose than the current limit.
  VLOG(verbose_log_level - 1) << "Hello";
  EXPECT_EQ("Hello\n", test_logger_->message());

  // At the current limit. This will be appended to the previous log output.
  VLOG(verbose_log_level) << "Hello 2";
  EXPECT_EQ("Hello\nHello 2\n", test_logger_->message());
}

}  // namespace phonenumbers
}  // namespace i18n
