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

# Author: Philippe Liard

cmake_minimum_required (VERSION 2.8)

project (libphonenumber)

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

# Check the library version (if pkg-config available).
find_package (PkgConfig)
function (check_library_version NAME LIBRARY VERSION)
  if (PKG_CONFIG_EXECUTABLE)
    pkg_check_modules (NAME REQUIRED ${LIBRARY}>=${VERSION})
  endif (PKG_CONFIG_EXECUTABLE)
endfunction ()

# Find a program. If it has not been found, stop CMake with a fatal error
# message.
function (find_required_program NAME FILENAME DESCRIPTION)
  find_program (${NAME}_BIN NAMES ${FILENAME})

  if (${NAME}_BIN STREQUAL "${${NAME}_BIN}-NOTFOUND")
    print_error (${DESCRIPTION} ${FILENAME})
  endif ()
endfunction (find_required_program)

# Find all the required libraries and programs.
find_required_library (GTEST gtest/gtest.h gtest "Google Test framework")

find_required_library (RE2 re2/re2.h re2 "Google RE2")

find_required_library (PROTOBUF google/protobuf/message_lite.h protobuf
                       "Google Protocol Buffers")
check_library_version (PC_PROTOBUF protobuf 2.4)

find_required_library (ICU unicode/unistr.h icui18n "ICU")
check_library_version (PC_ICU icui18n 4.4)

find_required_program (PROTOC protoc
                       "Google Protocol Buffers compiler (protoc)")

find_required_program (JAVA java
                       "Java Runtime Environment")

# Add protoc (Protocol Buffers compiler) target.
set (RESOURCES_DIR "${CMAKE_SOURCE_DIR}/../resources")

set (
  PROTOBUF_SOURCES "${RESOURCES_DIR}/phonemetadata.proto"
                   "${RESOURCES_DIR}/phonenumber.proto"
)

set (
  PROTOBUF_OUTPUT "${CMAKE_SOURCE_DIR}/src/phonemetadata.pb.cc"
                  "${CMAKE_SOURCE_DIR}/src/phonemetadata.pb.h"
                  "${CMAKE_SOURCE_DIR}/src/phonenumber.pb.cc"
                  "${CMAKE_SOURCE_DIR}/src/phonenumber.pb.h"
)

add_custom_command (
  COMMAND ${PROTOC_BIN} --cpp_out="${CMAKE_SOURCE_DIR}/src"
    --proto_path=${RESOURCES_DIR} ${PROTOBUF_SOURCES}

  OUTPUT ${PROTOBUF_OUTPUT}
  DEPENDS ${PROTOBUF_SOURCES}
)

add_custom_target (
  generate-sources

  DEPENDS ${PROTOBUF_OUTPUT}
  COMMENT "Generating Protocol Buffers code"
)

# Add metadata code generation targets.

# This function is invoked twice to create metadata & test metadata code
# generation targets.
function (add_metadata_gen_target TARGET_NAME
                                  XML_FILE
                                  FOR_TESTING)
  if (${FOR_TESTING} STREQUAL "true")
    set (GEN_OUTPUT_PREFIX "${CMAKE_SOURCE_DIR}/src/metadata")
  elseif (${FOR_TESTING} STREQUAL "false")
    set (GEN_OUTPUT_PREFIX "${CMAKE_SOURCE_DIR}/src/test_metadata")
  else ()
    message (FATAL_ERROR
      "Unexpected value '${FOR_TESTING}' for testing parameter")
  endif ()

  set (GEN_OUTPUT "${GEN_OUTPUT_PREFIX}.cc ${GEN_OUTPUT_PREFIX}.h")
  set (JAR_PATH "${CMAKE_SOURCE_DIR}/../tools/java/cpp-build/target")
  set (JAR_PATH "${JAR_PATH}/cpp-build-1.0-SNAPSHOT-jar-with-dependencies.jar")

  add_custom_command (
    COMMAND ${JAVA_BIN} -jar
      ${JAR_PATH} BuildMetadataCppFromXml ${XML_FILE} ${CMAKE_SOURCE_DIR}/src
      ${FOR_TESTING}

    OUTPUT ${GEN_OUTPUT}
    DEPENDS ${XML_FILE}
  )
  add_custom_target (
    ${TARGET_NAME}
    DEPENDS ${GEN_OUTPUT}
    COMMENT "Generating Metadata code"
  )
endfunction (add_metadata_gen_target)

# Add metadata generation target.
add_metadata_gen_target (
  "generate-metadata"
  "${RESOURCES_DIR}/PhoneNumberMetaData.xml"
  "false"
)

# Add test metadata generation target.
add_metadata_gen_target (
  "generate-test-metadata"
  "${RESOURCES_DIR}/PhoneNumberMetaDataForTesting.xml"
  "true"
)

# Platform independent sources.
set (
  SOURCES
  "src/base/at_exit.cc"
  "src/base/lazy_instance.cc"
  "src/base/string_piece.cc"
  "src/base/synchronization/lock.cc"
  "src/base/threading/thread_restrictions.cc"
  "src/default_logger.cc"
  "src/logger_adapter.cc"
  "src/metadata.cc"         # Generated by build tools.
  "src/phonemetadata.pb.cc" # Generated by Protocol Buffers.
  "src/phonenumber.cc"
  "src/phonenumber.pb.cc"   # Generated by Protocol Buffers.
  "src/phonenumberutil.cc"
  "src/re2_cache.cc"
  "src/stringutil.cc"
  "src/utf/rune.c"
  "src/utf/unicodetext.cc"
  "src/utf/unilib.cc"
)

if (UNIX)
  if (CMAKE_COMPILER_IS_GNUCXX)
    add_definitions ("-Wall -Wextra -Werror")

    # The next flags are needed by base/ source files to compile low level code
    # needed by Singleton.
    add_definitions ("-DCOMPILER_GCC -DOS_POSIX -DOS_LINUX")

    if (${CMAKE_SYSTEM_PROCESSOR} MATCHES "x86.*")
      add_definitions ("-DARCH_CPU_X86_FAMILY")
      # Add GCC specific sources.
      list (APPEND SOURCES "src/base/atomicops_internals_x86_gcc.cc")

    elseif (${CMAKE_SYSTEM_PROCESSOR} MATCHES ".*arm.*")
      add_definitions ("-DARCH_CPU_ARM_FAMILY")
    endif ()
  endif ()

  # Add POSIX specific sources.
  list (
    APPEND SOURCES
    "src/base/synchronization/lock_impl_posix.cc"
    "src/base/threading/platform_thread_posix.cc"
    "src/base/threading/thread_local_posix.cc"
  )
else (WIN32)
  # TODO: add Windows support (define COMPILER_MSVC, OS_WIN).
  list (
    APPEND SOURCES
    "src/base/synchronization/lock_impl_win.cc"
    "src/base/threading/platform_thread_win.cc"
    "src/base/threading/thread_local_win.cc"
  )
  # TODO: Windows specific flags.
endif ()

include_directories ("src")
include_directories (".")

add_library (phonenumber STATIC ${SOURCES})
add_dependencies (phonenumber generate-sources)

target_link_libraries (phonenumber ${RE2_LIB} ${PROTOBUF_LIB} ${ICU_LIB})

# Tests.
set (TEST_SOURCES
  "src/phonenumberutil_test.cc"
  "src/re2_cache_test.cc"
  "src/run_tests.cc"
  "src/stringutil_test.cc"
  "src/test_metadata.cc"  # Generated by build tools.
  "src/utf/unicodetext_test.cc"
)

add_executable (libphonenumber_test ${TEST_SOURCES})
target_link_libraries (libphonenumber_test phonenumber ${GTEST_LIB} pthread)
