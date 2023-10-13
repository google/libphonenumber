# Copyright (C) 2012 The Libphonenumber Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Author: Patrick Mezard

cmake_minimum_required (VERSION 3.11)

project (generate_geocoding_data)

set (
  SOURCES
  "src/cpp-build/generate_geocoding_data.cc"
  "src/cpp-build/generate_geocoding_data_main.cc"
)

if (NOT WIN32)
  add_definitions ("-Wall -Werror")
endif ()

include_directories ("src")

add_executable (generate_geocoding_data ${SOURCES})
target_link_libraries (generate_geocoding_data absl::strings absl::btree absl::node_hash_set)

if (BUILD_TESTING)
  set (TEST_SOURCES
      "src/cpp-build/generate_geocoding_data.cc"
      "test/cpp-build/generate_geocoding_data_test.cc"
      "test/cpp-build/run_tests.cc"
  )

  set (TEST_LIBS ${GTEST_LIB})

  if (NOT WIN32)
    list (APPEND TEST_LIBS pthread)
  endif ()

  # Build the testing binary.
  include_directories ("test")
  add_executable (generate_geocoding_data_test ${TEST_SOURCES})
  target_link_libraries (generate_geocoding_data_test absl::btree ${TEST_LIBS} absl::node_hash_set)
endif()

