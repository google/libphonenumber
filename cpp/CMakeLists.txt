# Copyright (C) 2011 The Libphonenumber Authors
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

cmake_minimum_required (VERSION 3.11)

project (libphonenumber VERSION 8.13.0)

if (32BIT)
  set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -m32")
  set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -m32")
  set (CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -m32")
  set (CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -m32")
endif ()

# Set out-of-tree tools directory
set (TOOLS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../tools/cpp")
set (TOOLS_BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/tools")

# Helper functions dealing with finding libraries and programs this library
# depends on.

include (FetchContent)
include (GNUInstallDirs)

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
function (check_library_version VARNAME LIBRARY_WITH_VERSION)
  if (PKG_CONFIG_FOUND)
    pkg_check_modules (${VARNAME} ${LIBRARY_WITH_VERSION})
  endif ()
endfunction ()

# Find a program. If it has not been found, stop CMake with a fatal error
# message.
function (find_required_program NAME FILENAME DESCRIPTION)
  find_program (${NAME}_BIN NAMES ${FILENAME})

  if (${NAME}_BIN STREQUAL "${NAME}_BIN-NOTFOUND")
    print_error (${DESCRIPTION} ${FILENAME})
  endif ()
endfunction (find_required_program)

# Options that can be passed to CMake using 'cmake -DKEY=VALUE'.
option (BUILD_GEOCODER "Build the offline phone number geocoder" ON)
option (REGENERATE_METADATA "Regenerate metadata instead of using it from the source tree" ON)
option (USE_ALTERNATE_FORMATS "Use alternate formats" ON)
option (USE_PROTOBUF_LITE "Link to protobuf-lite" OFF)
option (USE_BOOST "Use Boost" ON)
option (USE_ICU_REGEXP "Use ICU regexp engine" ON)
option (USE_LITE_METADATA "Use lite metadata" OFF)
option (USE_RE2 "Use RE2" OFF)
option (USE_STD_MAP "Force the use of std::map" OFF)
option (BUILD_STATIC_LIB "Build static libraries" ON)
option (BUILD_SHARED_LIBS "Build shared libraries" ON)
option (BUILD_TESTING "Build testing" ON)
option (BUILD_TOOLS_ONLY "Limit build to targets in ../tools/cpp" OFF)
option (USE_STDMUTEX "Use C++ 2011 std::mutex for multi-threading" OFF)
option (USE_POSIX_THREAD "Use Posix api for multi-threading" OFF)

if (USE_ALTERNATE_FORMATS)
  add_definitions ("-DI18N_PHONENUMBERS_USE_ALTERNATE_FORMATS")
endif ()

# Find all the required libraries and programs.
find_package(absl)

if(NOT absl_FOUND)
  # Overide abseil install rules for subprojects
  set(ABSL_ENABLE_INSTALL ON)
  
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

if (BUILD_TESTING)
  include (../tools/cpp/gtest.cmake)
  find_or_build_gtest ()
endif()

if (BUILD_TOOLS_ONLY)
  add_subdirectory("${TOOLS_DIR}" "${TOOLS_BINARY_DIR}")
  return()
endif()

if (USE_BOOST)
  add_definitions ("-DI18N_PHONENUMBERS_USE_BOOST")
  if (WIN32)
    set (Boost_USE_STATIC_LIBS ON)
  endif ()
  find_package (Boost 1.40.0 COMPONENTS date_time system thread)
  if (NOT Boost_FOUND)
    print_error ("Boost Date_Time/System/Thread" "Boost")
  endif ()
  include_directories (${Boost_INCLUDE_DIRS})
endif ()

if (USE_STDMUTEX)
  add_definitions ("-DI18N_PHONENUMBERS_USE_STDMUTEX")
endif ()

if (USE_POSIX_THREAD)
  add_definitions ("-DI18N_PHONENUMBERS_HAVE_POSIX_THREAD")
  find_package (Threads REQUIRED)
endif ()

if ((NOT USE_BOOST) AND (NOT USE_STDMUTEX))
  find_package (Threads)
endif()

if (USE_RE2)
  find_required_library (RE2 re2/re2.h re2 "Google RE2")
endif ()

if (USE_PROTOBUF_LITE)
  find_required_library (PROTOBUF google/protobuf/message_lite.h protobuf-lite
                         "Google Protocol Buffers")
  check_library_version (PC_PROTOBUF protobuf-lite>=2.4)
else ()
  find_required_library (PROTOBUF google/protobuf/message_lite.h protobuf
                         "Google Protocol Buffers")
  check_library_version (PC_PROTOBUF protobuf>=2.4)
endif ()

find_required_library (ICU_UC unicode/uchar.h icuuc "ICU")
check_library_version (PC_ICU_UC icu-uc>=4.4)

set (ICU_INCLUDE_DIR ${ICU_UC_INCLUDE_DIR})
set (ICU_LIB ${ICU_UC_LIB})
# If ICU regexp engine is used or if the geocoder is built, use icui18n as well.
if (USE_ICU_REGEXP OR BUILD_GEOCODER)
  find_required_library (ICU_I18N unicode/regex.h icui18n "ICU")
  check_library_version (PC_ICU_I18N icu-i18n>=4.4)
  list (APPEND ICU_INCLUDE_DIR ${ICU_I18N_INCLUDE_DIR})
  list (APPEND ICU_LIB ${ICU_I18N_LIB})
endif ()

find_required_program (PROTOC protoc
                       "Google Protocol Buffers compiler (protoc)")

if (REGENERATE_METADATA)
  find_required_program (JAVA java
                         "Java Runtime Environment")
endif ()

if (APPLE)
  FIND_LIBRARY (COREFOUNDATION_LIB CoreFoundation)
  FIND_LIBRARY (FOUNDATION_LIB Foundation)
  set (CMAKE_MACOSX_RPATH OFF)
  set (CMAKE_SHARED_LIBRARY_CREATE_CXX_FLAGS
    "${CMAKE_SHARED_LIBRARY_CREATE_CXX_FLAGS} -undefined dynamic_lookup")
endif ()

if (NOT USE_STD_MAP)
  INCLUDE (CheckIncludeFileCXX)
  CHECK_INCLUDE_FILE_CXX ("tr1/unordered_map" HAVE_CXX_TR1_UNORDERED_MAP)
  if (HAVE_CXX_TR1_UNORDERED_MAP)
    add_definitions ("-DI18N_PHONENUMBERS_USE_TR1_UNORDERED_MAP")
  endif ()
endif ()

# Add protoc (Protocol Buffers compiler) target.
set (RESOURCES_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../resources")

set (
  PROTOBUF_SOURCES "${RESOURCES_DIR}/phonemetadata.proto"
                   "${RESOURCES_DIR}/phonenumber.proto"
)

set (
  PROTOBUF_OUTPUT "${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers/phonemetadata.pb.cc"
                  "${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers/phonemetadata.pb.h"
                  "${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers/phonenumber.pb.cc"
                  "${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers/phonenumber.pb.h"
)

add_custom_command (
  COMMAND ${PROTOC_BIN} --cpp_out=${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers/
    --proto_path=${RESOURCES_DIR} ${PROTOBUF_SOURCES}

  OUTPUT ${PROTOBUF_OUTPUT}
  DEPENDS ${PROTOBUF_SOURCES}
)

if (BUILD_GEOCODER)
  # Geocoding data cpp file generation
  add_subdirectory("${TOOLS_DIR}" "${TOOLS_BINARY_DIR}")

  set (GEOCODING_DIR "${RESOURCES_DIR}/geocoding")
  file (GLOB_RECURSE GEOCODING_SOURCES "${GEOCODING_DIR}/*.txt")

  set (GEOCODING_DATA_OUTPUT
    "${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers/geocoding/geocoding_data.cc"
  )

  add_custom_command (
    COMMAND generate_geocoding_data "${GEOCODING_DIR}"
      "${GEOCODING_DATA_OUTPUT}"

    OUTPUT ${GEOCODING_DATA_OUTPUT}
    DEPENDS ${GEOCODING_SOURCES}
            generate_geocoding_data
    COMMENT "Generating geocoding data code"
  )
endif ()

set (
  SOURCES
  "src/phonenumbers/asyoutypeformatter.cc"
  "src/phonenumbers/base/strings/string_piece.cc"
  "src/phonenumbers/default_logger.cc"
  "src/phonenumbers/logger.cc"
  "src/phonenumbers/phonemetadata.pb.cc" # Generated by Protocol Buffers.
  "src/phonenumbers/phonenumber.cc"
  "src/phonenumbers/phonenumber.pb.cc"   # Generated by Protocol Buffers.
  "src/phonenumbers/phonenumberutil.cc"
  "src/phonenumbers/regex_based_matcher.cc"
  "src/phonenumbers/regexp_cache.cc"
  "src/phonenumbers/shortnumberinfo.cc"
  "src/phonenumbers/string_byte_sink.cc"
  "src/phonenumbers/stringutil.cc"
  "src/phonenumbers/unicodestring.cc"
  "src/phonenumbers/utf/rune.c"
  "src/phonenumbers/utf/unicodetext.cc"
  "src/phonenumbers/utf/unilib.cc"
)

if (BUILD_GEOCODER)
  set (
    GEOCODING_SOURCES
    "src/phonenumbers/geocoding/area_code_map.cc"
    "src/phonenumbers/geocoding/default_map_storage.cc"
    "src/phonenumbers/geocoding/geocoding_data.cc"
    "src/phonenumbers/geocoding/mapping_file_provider.cc"
    "src/phonenumbers/geocoding/phonenumber_offline_geocoder.cc"
    "src/phonenumbers/phonenumber.pb.h"  # Forces proto buffer generation.
  )
endif ()

# Add regexp engine-dependent sources. ICU is used by default.
if (USE_RE2)
  # Add a flag to select the right regexp factory implementation used by
  # regexp_factory.h and regexp_adapter_test.cc.
  # When both ICU regexp and RE2 are defined, the regexp engine adapter defaults
  # to RE2 unless the ICU implementation is instantiated explictly obviously.
  add_definitions ("-DI18N_PHONENUMBERS_USE_RE2")
  list (APPEND SOURCES "src/phonenumbers/regexp_adapter_re2.cc")
endif ()

if (USE_ICU_REGEXP)
  add_definitions ("-DI18N_PHONENUMBERS_USE_ICU_REGEXP")
  list (APPEND SOURCES "src/phonenumbers/regexp_adapter_icu.cc")
  # The phone number matcher needs ICU.
  list (APPEND SOURCES "src/phonenumbers/phonenumbermatch.cc")
  list (APPEND SOURCES "src/phonenumbers/phonenumbermatcher.cc")
  if (USE_ALTERNATE_FORMATS)
    list (APPEND SOURCES "src/phonenumbers/alternate_format.cc")
  endif ()
endif ()

# Library sources excluding the metadata files, since special metadata is used
# for unit-testing. Note that a single testing library is built for both
# libphonenumber and geocoding.
set (TESTING_LIBRARY_SOURCES ${SOURCES})
if (BUILD_GEOCODER)
  list (APPEND TESTING_LIBRARY_SOURCES ${GEOCODING_SOURCES})
endif ()

# Add metadata code generation targets.

# This function is invoked to create metadata, test metadata and lite metadata
# code generation targets.
function (add_metadata_gen_target TARGET_NAME XML_FILE METADATA_TYPE METADATA_HEADER)
  set (METADATA_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers")
  set (GEN_OUTPUT "${METADATA_SOURCE_DIR}/${METADATA_TYPE}.cc" "${METADATA_SOURCE_DIR}/${METADATA_HEADER}.h")
  set (JAR_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../tools/java/cpp-build/target")
  set (JAR_PATH "${JAR_PATH}/cpp-build-1.0-SNAPSHOT-jar-with-dependencies.jar")

  if (REGENERATE_METADATA)
    add_custom_command (
      COMMAND ${JAVA_BIN} -jar
        ${JAR_PATH} BuildMetadataCppFromXml ${XML_FILE}
        ${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers ${METADATA_TYPE}

      OUTPUT ${GEN_OUTPUT}
      DEPENDS ${XML_FILE}
    )
  else ()
    add_custom_command (
      COMMAND echo "skip metadata generation from"
        ${XML_FILE} "to"
        ${CMAKE_CURRENT_SOURCE_DIR}/src/phonenumbers ${METADATA_TYPE}

      OUTPUT ${GEN_OUTPUT}
      DEPENDS ${XML_FILE}
    )
  endif ()

  add_custom_target (
    ${TARGET_NAME}
    DEPENDS ${GEN_OUTPUT}
    COMMENT "Generating Metadata code"
  )
endfunction (add_metadata_gen_target)

if (USE_LITE_METADATA)
  # Add the lite metadata generation target.
  set (METADATA_TARGET "generate-lite-metadata")
  add_metadata_gen_target (
    ${METADATA_TARGET}
    "${RESOURCES_DIR}/PhoneNumberMetadata.xml"
    "lite_metadata"
    "metadata"
  )
  list (APPEND SOURCES "src/phonenumbers/lite_metadata.cc")
else ()
  # Add the metadata generation target.
  set (METADATA_TARGET "generate-metadata")
  add_metadata_gen_target (
    ${METADATA_TARGET}
    "${RESOURCES_DIR}/PhoneNumberMetadata.xml"
    "metadata"
    "metadata"
  )
  list (APPEND SOURCES "src/phonenumbers/metadata.cc")
endif ()

# Add the test metadata generation target.
set (TEST_METADATA_TARGET "generate-test-metadata")
add_metadata_gen_target (
  ${TEST_METADATA_TARGET}
  "${RESOURCES_DIR}/PhoneNumberMetadataForTesting.xml"
  "test_metadata"
  "test_metadata"
)
list (APPEND TESTING_LIBRARY_SOURCES "src/phonenumbers/test_metadata.cc")

# Add the short metadata generation target.
set (SHORT_METADATA_TARGET "generate-short-number-metadata")
add_metadata_gen_target (
  ${SHORT_METADATA_TARGET}
  "${RESOURCES_DIR}/ShortNumberMetadata.xml"
  "short_metadata"
  "short_metadata"
)
# This is used both for the real library and for testing.
list (APPEND SOURCES "src/phonenumbers/short_metadata.cc")
list (APPEND TESTING_LIBRARY_SOURCES "src/phonenumbers/short_metadata.cc")

if (USE_ICU_REGEXP)
  if (USE_ALTERNATE_FORMATS)
    # Add alternate format metadata generation for the phone number matcher.
    set (ALT_FORMAT_METADATA_TARGET "generate-alt-format-metadata")
    add_metadata_gen_target (
      ${ALT_FORMAT_METADATA_TARGET}
      "${RESOURCES_DIR}/PhoneNumberAlternateFormats.xml"
      "alternate_format"
      "alternate_format"
    )
  endif ()
endif ()

if (NOT WIN32)
  add_definitions ("-Wall -Werror")
endif ()

include_directories ("src")

#----------------------------------------------------------------
# Collate dependencies
#----------------------------------------------------------------

set (LIBRARY_DEPS ${ICU_LIB} ${PROTOBUF_LIB} absl::node_hash_set absl::strings absl::synchronization)

if (USE_BOOST)
  list (APPEND LIBRARY_DEPS ${Boost_LIBRARIES})
endif ()

if (USE_RE2)
  list (APPEND LIBRARY_DEPS ${RE2_LIB})
endif ()

if (USE_POSIX_THREAD OR ((APPLE OR UNIX) AND (NOT (USE_BOOST OR USE_STDMUTEX))))
  if(CMAKE_USE_PTHREADS_INIT)
    set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -pthread")
  endif()
  if(CMAKE_THREAD_LIBS_INIT)
    list (APPEND LIBRARY_DEPS ${CMAKE_THREAD_LIBS_INIT})
  endif()
endif ()

if (APPLE)
  list (APPEND LIBRARY_DEPS ${COREFOUNDATION_LIB} ${FOUNDATION_LIB})
endif ()

#----------------------------------------------------------------
# Build libraries
#----------------------------------------------------------------

if (BUILD_STATIC_LIB)
  # Build a static library (without -fPIC).
  add_library (phonenumber STATIC ${SOURCES})
  target_link_libraries (phonenumber ${LIBRARY_DEPS})
  target_include_directories(phonenumber PUBLIC $<INSTALL_INTERFACE:include>)

  if (BUILD_GEOCODER)
    add_library (geocoding STATIC ${GEOCODING_SOURCES})
    target_link_libraries (geocoding ${LIBRARY_DEPS})
    target_include_directories(geocoding PUBLIC $<INSTALL_INTERFACE:include>)
    add_dependencies (geocoding generate_geocoding_data)
    add_dependencies (phonenumber generate_geocoding_data)
  endif ()

  if (USE_ICU_REGEXP AND USE_ALTERNATE_FORMATS)
    add_dependencies (phonenumber ${ALT_FORMAT_METADATA_TARGET})
  endif ()
endif ()


if (BUILD_SHARED_LIBS)
  # Build a shared library (with -fPIC).
  add_library (phonenumber-shared SHARED ${SOURCES})
  target_link_libraries (phonenumber-shared ${LIBRARY_DEPS})
  target_include_directories(phonenumber-shared PUBLIC $<INSTALL_INTERFACE:include>)

  set_target_properties (phonenumber-shared
    PROPERTIES
      OUTPUT_NAME "phonenumber"
      PREFIX "lib"
      SOVERSION ${libphonenumber_VERSION_MAJOR}
      VERSION ${libphonenumber_VERSION_MAJOR}.${libphonenumber_VERSION_MINOR})

  if (USE_ICU_REGEXP AND USE_ALTERNATE_FORMATS)
   add_dependencies (phonenumber-shared ${ALT_FORMAT_METADATA_TARGET})
  endif ()

  if (BUILD_GEOCODER)
    add_library (geocoding-shared SHARED ${GEOCODING_SOURCES})
    target_link_libraries (geocoding-shared ${LIBRARY_DEPS})
    target_include_directories(geocoding-shared PUBLIC $<INSTALL_INTERFACE:include>)
    add_dependencies (geocoding-shared generate_geocoding_data)
    add_dependencies (phonenumber-shared generate_geocoding_data)

    set_target_properties (geocoding-shared
      PROPERTIES
        OUTPUT_NAME "geocoding"
        PREFIX "lib"
        SOVERSION ${libphonenumber_VERSION_MAJOR}
        VERSION ${libphonenumber_VERSION_MAJOR}.${libphonenumber_VERSION_MINOR})
  endif ()
endif ()

#----------------------------------------------------------------
# Build testing library
#----------------------------------------------------------------

if(BUILD_TESTING)
  add_library (phonenumber_testing STATIC ${TESTING_LIBRARY_SOURCES})
  if (BUILD_GEOCODER)
    add_dependencies (phonenumber_testing generate_geocoding_data)
  endif ()
  target_link_libraries (phonenumber_testing ${LIBRARY_DEPS})

  if (BUILD_GEOCODER)
    # Test geocoding data cpp files generation.
    set (GEOCODING_TEST_DIR "${RESOURCES_DIR}/test/geocoding")
    file (GLOB_RECURSE GEOCODING_TEST_SOURCES "${GEOCODING_TEST_DIR}/*.txt")

    set (GEOCODING_TEST_DATA_OUTPUT "${CMAKE_CURRENT_SOURCE_DIR}/test/phonenumbers/geocoding/geocoding_test_data.cc")

    add_custom_command (
      COMMAND generate_geocoding_data "${GEOCODING_TEST_DIR}" "${GEOCODING_TEST_DATA_OUTPUT}" "_test"
      OUTPUT ${GEOCODING_TEST_DATA_OUTPUT}
      DEPENDS ${GEOCODING_TEST_SOURCES} generate_geocoding_data
      COMMENT "Generating geocoding test data code"
    )
  endif ()

  set (TEST_SOURCES
      "test/phonenumbers/asyoutypeformatter_test.cc"
      "test/phonenumbers/logger_test.cc"
      "test/phonenumbers/matcher_test.cc"
      "test/phonenumbers/phonenumberutil_test.cc"
      "test/phonenumbers/regexp_adapter_test.cc"
      "test/phonenumbers/regexp_cache_test.cc"
      "test/phonenumbers/run_tests.cc"
      "test/phonenumbers/shortnumberinfo_test.cc"
      "test/phonenumbers/stringutil_test.cc"
      "test/phonenumbers/test_util.cc"
      "test/phonenumbers/unicodestring_test.cc"
      "test/phonenumbers/utf/unicodetext_test.cc")

  if (BUILD_GEOCODER)
    set (GEOCODING_TEST_SOURCES
        "test/phonenumbers/geocoding/area_code_map_test.cc"
        "test/phonenumbers/geocoding/geocoding_data_test.cc"
        "test/phonenumbers/geocoding/geocoding_test_data.cc"
        "test/phonenumbers/geocoding/mapping_file_provider_test.cc"
        "test/phonenumbers/geocoding/phonenumber_offline_geocoder_test.cc")
    list (APPEND TEST_SOURCES ${GEOCODING_TEST_SOURCES})
  endif ()

  if (USE_ICU_REGEXP)
    # Add the phone number matcher tests.
    list (APPEND TEST_SOURCES "test/phonenumbers/phonenumbermatch_test.cc")
    list (APPEND TEST_SOURCES "test/phonenumbers/phonenumbermatcher_test.cc")
  endif ()

  # Build the testing binary.
  include_directories ("test")
  add_executable (libphonenumber_test ${TEST_SOURCES})
  set (TEST_LIBS phonenumber_testing ${GTEST_LIB})

  if (NOT WIN32)
    list (APPEND TEST_LIBS pthread)
  endif ()

  target_link_libraries (libphonenumber_test ${TEST_LIBS})

  # Unfortunately add_custom_target() can't accept a single command provided as a
  # list of commands.
  if (BUILD_GEOCODER)
    add_custom_target (tests
      COMMAND generate_geocoding_data_test
      COMMAND libphonenumber_test
      DEPENDS generate_geocoding_data_test libphonenumber_test
    )
  else ()
    add_custom_target (tests
      COMMAND libphonenumber_test
      DEPENDS libphonenumber_test
    )
  endif ()

  # Build an example program using geocoding, mainly to make sure that both
  # libraries are built properly.
  if (BUILD_GEOCODER)
    add_executable (geocoding_test_program "test/phonenumbers/geocoding/geocoding_test_program.cc")
    target_link_libraries (geocoding_test_program geocoding phonenumber)
  endif ()
endif()

#----------------------------------------------------------------
# Install built libraries
#----------------------------------------------------------------

set (BUILT_LIBS)
set(targets_export_name "${PROJECT_NAME}-targets")

if (BUILD_STATIC_LIB)
  list (APPEND BUILT_LIBS phonenumber)
  if (BUILD_GEOCODER)
    list (APPEND BUILT_LIBS geocoding)
  endif()
endif()

if (BUILD_SHARED_LIBS)
  list (APPEND BUILT_LIBS phonenumber-shared)
  if (BUILD_GEOCODER)
    list (APPEND BUILT_LIBS geocoding-shared)
  endif()
endif()

install (
  TARGETS ${BUILT_LIBS}
  EXPORT "${targets_export_name}"
  LIBRARY DESTINATION "${CMAKE_INSTALL_LIBDIR}"
  ARCHIVE DESTINATION "${CMAKE_INSTALL_LIBDIR}")

#----------------------------------------------------------------
# Install headers
#----------------------------------------------------------------

install (FILES
  "src/phonenumbers/asyoutypeformatter.h"
  "src/phonenumbers/callback.h"
  "src/phonenumbers/logger.h"
  "src/phonenumbers/matcher_api.h"
  "src/phonenumbers/phonenumber.pb.h"
  "src/phonenumbers/phonemetadata.pb.h"
  "src/phonenumbers/phonenumberutil.h"
  "src/phonenumbers/regexp_adapter.h"
  "src/phonenumbers/regexp_cache.h"
  "src/phonenumbers/region_code.h"
  "src/phonenumbers/shortnumberinfo.h"
  "src/phonenumbers/unicodestring.h"
  DESTINATION include/phonenumbers/
)

install (FILES "src/phonenumbers/utf/unicodetext.h"
         DESTINATION include/phonenumbers/utf/)

if (USE_ICU_REGEXP)
  # Install the phone number matcher headers.
  install (FILES
    "src/phonenumbers/phonenumbermatch.h"
    "src/phonenumbers/phonenumbermatcher.h"
    "src/phonenumbers/regexp_adapter.h"
    DESTINATION include/phonenumbers/
  )
endif ()

if (BUILD_GEOCODER)
  install (FILES
    "src/phonenumbers/geocoding/phonenumber_offline_geocoder.h"
    DESTINATION include/phonenumbers/geocoding
  )
endif ()

install (
  FILES
  "src/phonenumbers/base/basictypes.h"
  "src/phonenumbers/base/template_util.h"
  "src/phonenumbers/base/logging.h"
  "src/phonenumbers/base/thread_checker.h"
  DESTINATION include/phonenumbers/base/
)

install (FILES
  "src/phonenumbers/base/memory/scoped_ptr.h"
  "src/phonenumbers/base/memory/singleton.h"
  "src/phonenumbers/base/memory/singleton_boost.h"
  "src/phonenumbers/base/memory/singleton_posix.h"
  "src/phonenumbers/base/memory/singleton_stdmutex.h"
  "src/phonenumbers/base/memory/singleton_unsafe.h"
  "src/phonenumbers/base/memory/singleton_win32.h"
  DESTINATION include/phonenumbers/base/memory/
)

install (FILES
  "src/phonenumbers/base/synchronization/lock.h"
  "src/phonenumbers/base/synchronization/lock_boost.h"
  "src/phonenumbers/base/synchronization/lock_posix.h"
  "src/phonenumbers/base/synchronization/lock_stdmutex.h"
  "src/phonenumbers/base/synchronization/lock_unsafe.h"
  "src/phonenumbers/base/synchronization/lock_win32.h"
  DESTINATION include/phonenumbers/base/synchronization/)

#----------------------------------------------------------------
# Build and install cmake config files
#----------------------------------------------------------------

include(CMakePackageConfigHelpers)

set(config_install_dir "${CMAKE_INSTALL_LIBDIR}/cmake/${PROJECT_NAME}")
set(version_config "${CMAKE_CURRENT_BINARY_DIR}/${PROJECT_NAME}-config-version.cmake")
set(project_config "${CMAKE_CURRENT_BINARY_DIR}/${PROJECT_NAME}-config.cmake")

# libphonenumber-config-version.cmake
write_basic_package_version_file("${version_config}" COMPATIBILITY SameMajorVersion)

# libphonenumber-config.cmake
configure_package_config_file("cmake/config.cmake.in" "${project_config}" INSTALL_DESTINATION "${config_install_dir}")

install(FILES "${project_config}" "${version_config}" DESTINATION "${config_install_dir}")
install(EXPORT "${targets_export_name}" NAMESPACE ${PROJECT_NAME}:: DESTINATION "${config_install_dir}")

#----------------------------------------------------------------
# Build an RPM
#----------------------------------------------------------------

set (CPACK_PACKAGE_VERSION ${libphonenumber_VERSION_MAJOR}.${libphonenumber_VERSION_MINOR}.${libphonenumber_VERSION_PATCH})
set (CPACK_GENERATOR "RPM")
set (CPACK_PACKAGE_NAME "libphonenumber")
set (CPACK_RPM_PACKAGE_RELEASE 1)
set (CPACK_PACKAGE_DESCRIPTION_SUMMARY "Google's phone number handling library")
if (32BIT)
  set (CPACK_RPM_PACKAGE_ARCHITECTURE i686)
else ()
  set (CPACK_RPM_PACKAGE_ARCHITECTURE x86_64)
endif ()
set (CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}-${CPACK_RPM_PACKAGE_RELEASE}.${CPACK_RPM_PACKAGE_ARCHITECTURE}")
include (CPack)
