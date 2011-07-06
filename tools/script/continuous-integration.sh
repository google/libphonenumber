#! /bin/sh

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

# Countinuous integration script that tests the different versions and
# configurations of libphonenumber.

# Test the C++ version with the provided CMake parameter.
test_cpp_version() {
  CMAKE_FLAGS="$1"
  (
    rm -rf cpp/build && mkdir cpp/build && cd cpp/build && \
        cmake "${CMAKE_FLAGS}" .. && make && ./libphonenumber_test
  ) || exit $?
}
test_cpp_version ''
test_cpp_version '-DUSE_RE2=ON'
test_cpp_version '-DUSE_LITE_METADATA=ON'
test_cpp_version '-DUSE_STD_MAP=ON'

# Test Java version using Ant.
(cd java && ant clean jar && ant junit) || exit $?

# Test Java version using Maven.
(cd java && mvn clean package) || exit $?

# Test build tools.
(cd tools/java && mvn clean package) || exit $?
