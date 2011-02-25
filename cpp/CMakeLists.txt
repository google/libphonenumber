# Copyright (C) 2011 Google Inc.
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

cmake_minimum_required (VERSION 2.8)

project (libphonenumber)

# Find Google Test library
find_path (GTEST_INCLUDE_DIR gtest/gtest.h)

if (GTEST_INCLUDE_DIR STREQUAL "GTEST_INCLUDE_DIR-NOTFOUND")
  message (FATAL_ERROR
    "Can't find Google Test framework headers. Please read README.")
endif ()

include_directories (${GTEST_INCLUDE_DIR})

find_library (GTEST_LIB gtest)

if (GTEST_LIB STREQUAL "GTEST_LIB-NOTFOUND")
  message (FATAL_ERROR
    "Can't find Google Test framework library. Please read README.")
endif ()


# Find Google RE2 library
find_path (RE2_INCLUDE_DIR re2/re2.h)

if (RE2_INCLUDE_DIR STREQUAL "RE2_INCLUDE_DIR-NOTFOUND")
  message (FATAL_ERROR
    "Can't find Google RE2 headers. Please read README.")
endif ()

include_directories (${RE2_INCLUDE_DIR})

find_library (RE2_LIB re2)

if (RE2_LIB STREQUAL "RE2_LIB-NOTFOUND")
  message (FATAL_ERROR
    "Can't find Google RE2 library. Please read README.")
endif ()


# Find Protocol Buffers compiler
find_program (PROTOC NAMES protoc)

if (PROTOC STREQUAL "PROTOC-NOTFOUND")
  message (FATAL_ERROR
      "Can't find Google Protocol Buffers compiler (protoc). "
      "Please read README.")
endif ()

# Add protoc (Protocol Buffers compiler) target
set (
  PROTOBUF_SOURCES "${CMAKE_SOURCE_DIR}/src/phonemetadata.proto"
                   "${CMAKE_SOURCE_DIR}/src/phonenumber.proto"

)

set (
  PROTOBUF_OUTPUT "${CMAKE_SOURCE_DIR}/src/phonemetadata.pb.cc"
                  "${CMAKE_SOURCE_DIR}/src/phonemetadata.pb.h"
                  "${CMAKE_SOURCE_DIR}/src/phonenumber.pb.cc"
                  "${CMAKE_SOURCE_DIR}/src/phonenumber.pb.h"
)

add_custom_command (
  COMMAND ${PROTOC} --cpp_out=${CMAKE_SOURCE_DIR}
    --proto_path=${CMAKE_SOURCE_DIR} ${PROTOBUF_SOURCES}

  OUTPUT ${PROTOBUF_OUTPUT}
  DEPENDS ${PROTOBUF_SOURCES}
)

add_custom_target (
  generate-sources

  DEPENDS ${PROTOBUF_OUTPUT}
  COMMENT "Generating Protocol Buffers code"
)

# Platform independent sources
set (
  SOURCES
  "src/base/at_exit.cc"
  "src/base/lazy_instance.cc"
  "src/base/synchronization/lock.cc"
  "src/base/threading/thread_restrictions.cc"
  "src/phonenumberutil.cc"
  "src/re2_cache.cc"
  "src/stringutil.cc"
  "src/utf/rune.c"
)

if (UNIX)
  if (CMAKE_COMPILER_IS_GNUCXX)
    add_definitions ("-Wall -Wextra -Werror")

    # The next flags are needed by base/ source files to compile
    # low level code needed by Singleton
    add_definitions ("-DCOMPILER_GCC -DOS_POSIX -DOS_LINUX")

    if (${CMAKE_SYSTEM_PROCESSOR} MATCHES "x86.*")
      add_definitions ("-DARCH_CPU_X86_FAMILY")

      # Add GCC specific sources
      list (APPEND SOURCES "src/base/atomicops_internals_x86_gcc.cc")
    endif ()

    # TODO: add support for ARCM arch
  endif ()

  # Add POSIX specific sources
  list (
    APPEND SOURCES
    "src/base/synchronization/lock_impl_posix.cc"
    "src/base/threading/platform_thread_posix.cc"
    "src/base/threading/thread_local_posix.cc"
  )
else (WIN32)
  # TODO: add Windows support (define COMPILER_MSVC, OS_WIN)
  list (
    APPEND SOURCES
    "src/base/synchronization/lock_impl_win.cc"
    "src/base/threading/platform_thread_win.cc"
    "src/base/threading/thread_local_win.cc"
  )
  # TODO: Windows specific flags
endif ()


include_directories ("src")
                          
add_library (phonenumber STATIC ${SOURCES})
add_dependencies (phonenumber generate-sources)

target_link_libraries (phonenumber re2)

# Tests

set (TEST_SOURCES
  "src/phonenumberutil_test.cc"
  "src/re2_cache_test.cc"
  "src/run_tests.cc"
  "src/stringutil_test.cc"
)

add_executable (libphonenumber_test ${TEST_SOURCES})

target_link_libraries (libphonenumber_test phonenumber gtest pthread)
