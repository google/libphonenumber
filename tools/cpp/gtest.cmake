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

# Author: Fredrik Roubert

# It used to be common to provide pre-compiled Google Test libraries, but this
# practice is not recommended and is increasingly rare today:
#
# http://code.google.com/p/googletest/wiki/FAQ#Why_is_it_not_recommended_to_install_a_pre-compiled_copy_of_Goog
#
# This helper function will either find a pre-compiled library or else find the
# source code and add a library target to build the library from source.

function (find_or_build_gtest)
  # Find header files.
  find_path (GTEST_INCLUDE_DIR gtest/gtest.h)
  if (${GTEST_INCLUDE_DIR} STREQUAL "GTEST_INCLUDE_DIR-NOTFOUND")
    message (FATAL_ERROR
      "Can't find Google C++ Testing Framework: can't locate gtest/gtest.h. "
      "Please read the README and also take a look at tools/cpp/gtest.cmake.")
  endif ()
  include_directories (${GTEST_INCLUDE_DIR})

  # Check for a pre-compiled library.
  find_library (GTEST_LIB gtest)
  if (${GTEST_LIB} STREQUAL "GTEST_LIB-NOTFOUND")
    # No pre-compiled library found, attempt building it from source.
    find_path (GTEST_SOURCE_DIR src/gtest-all.cc
               HINTS /usr/src/gtest /usr/local/src/gtest)
    add_library (gtest STATIC ${GTEST_SOURCE_DIR}/src/gtest-all.cc)
    include_directories (${GTEST_SOURCE_DIR})

    set (GTEST_LIB gtest PARENT_SCOPE)
  endif ()

endfunction ()
