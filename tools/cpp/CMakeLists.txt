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

# Pick the C++ standard to compile with.
# Abseil currently supports C++11, C++14, and C++17.
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

project (generate_geocoding_data)

# Helper functions dealing with finding libraries and programs this library
# depends on.
include (gtest.cmake)
include (FetchContent)

if(NOT absl_FOUND)
  # Downloading the abseil sources at particular version to not catch up
  # with its new build requirements like min C++14 is mandated in that lib.
  FetchContent_Declare(
      abseil-cpp
      GIT_REPOSITORY  https://github.com/abseil/abseil-cpp.git
      GIT_TAG         273292d
  )

  # Building the abseil binaries
  FetchContent_GetProperties(abseil-cpp)
  if (NOT abseil-cpp_POPULATED)
      FetchContent_Populate(abseil-cpp)
  endif ()

  if (NOT abseil-cpp_POPULATED)
     message (FATAL_ERROR "Could not build abseil-cpp binaries.")
  endif ()

  # Safeguarding against any potential link errors as mentioned in
  # https://github.com/abseil/abseil-cpp/issues/225
  set(CMAKE_POSITION_INDEPENDENT_CODE TRUE)
  add_subdirectory(${abseil-cpp_SOURCE_DIR} ${abseil-cpp_BINARY_DIR})
endif()

find_or_build_gtest ()
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
