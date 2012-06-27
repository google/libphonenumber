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

cmake_minimum_required (VERSION 2.8)

project (generate_geocoding_data)

# Helper functions dealing with finding libraries and programs this library
# depends on.

function (print_error DESCRIPTION FILE)
  message (FATAL_ERROR
    "Can't find ${DESCRIPTION}: can't locate ${FILE}. Please read the README.")
endfunction ()

# Find a library. If it has not been found, stop CMake with a fatal error
# message.
function (find_required_library NAME HEADER LIBRARY DESCRIPTION)
  # Check the header.
  find_path (${NAME}_INCLUDE_DIR ${HEADER})
  set (INCLUDE_DIR ${${NAME}_INCLUDE_DIR})

  if (${INCLUDE_DIR} STREQUAL "${INCLUDE_DIR}-NOTFOUND")
    print_error (${DESCRIPTION} ${HEADER})
  endif ()
  include_directories (${INCLUDE_DIR})
  # Check the binary.
  find_library (${NAME}_LIB ${LIBRARY})
  set (LIB ${NAME}_LIB)

  if (${LIB} STREQUAL "${LIB}-NOTFOUND")
    print_error (${DESCRIPTION} ${LIBRARY})
  endif ()
endfunction (find_required_library)

find_required_library (GTEST gtest/gtest.h gtest "Google Test framework")

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

set (TEST_SOURCES
  "src/cpp-build/generate_geocoding_data.cc"
  "test/cpp-build/generate_geocoding_data_test.cc"
  "test/cpp-build/run_tests.cc"
)

set (TEST_LIBS ${GTEST_LIB})

# Build the testing binary.
include_directories ("test")
add_executable (generate_geocoding_data_test ${TEST_SOURCES})
target_link_libraries (generate_geocoding_data_test ${TEST_LIBS})
