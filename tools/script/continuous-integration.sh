#! /bin/sh

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

# Countinuous integration script that tests the different versions and
# configurations of libphonenumber.

# Check geocoding resource files encoding.
(find resources/geocoding -type f | xargs file | egrep -v 'UTF-8|ASCII') && exit 1

# Test the C++ version with the provided CMake parameter.
CXX=g++
INSTALL_PREFIX=/tmp/libphonenumber

test_cpp_version() {
  CC_TEST_FILE=`mktemp`.cc
  CC_TEST_BINARY=`mktemp`
  CMAKE_FLAGS="$1"
  LD_FLAGS="-L${INSTALL_PREFIX}/lib -lphonenumber -lboost_thread $2"

  # Write the program that tests the installation of the library to a temporary
  # source file.
  > $CC_TEST_FILE echo '
    #include <cassert>

    #include <base/memory/scoped_ptr.h>

    // Include all the public headers.
    #include <phonenumbers/asyoutypeformatter.h>
    #include <phonenumbers/phonenumber.pb.h>
    #include <phonenumbers/phonenumbermatch.h>
    #include <phonenumbers/phonenumbermatcher.h>
    #include <phonenumbers/phonenumberutil.h>

    using i18n::phonenumbers::AsYouTypeFormatter;
    using i18n::phonenumbers::PhoneNumberUtil;

    int main() {
      PhoneNumberUtil* const phone_util = PhoneNumberUtil::GetInstance();
      const scoped_ptr<AsYouTypeFormatter> asytf(
          phone_util->GetAsYouTypeFormatter("US"));

      assert(phone_util != NULL);
      assert(asytf != NULL);
    }'

  # Run the build and tests.
  (
    set +e
    rm -rf cpp/build /tmp/libphonenumber
    mkdir cpp/build /tmp/libphonenumber
    cd cpp/build
    cmake "${CMAKE_FLAGS}" -DCMAKE_INSTALL_PREFIX=$INSTALL_PREFIX ..
    make test
    make install
    $CXX -o $CC_TEST_BINARY $CC_TEST_FILE -I${INSTALL_PREFIX}/include $LD_FLAGS
    LD_LIBRARY_PATH="${INSTALL_PREFIX}/lib" $CC_TEST_BINARY
  )
  STATUS=$?
  # Remove the temporary files.
  rm -f $CC_TEST_FILE
  rm -f $CC_TEST_BINARY

  [ $STATUS -ne 0 ] && exit $STATUS
}

BASE_LIBS='-licuuc -lprotobuf'

test_cpp_version '' "$BASE_LIBS"
test_cpp_version '-DUSE_ICU_REGEXP=ON' "$BASE_LIBS"
test_cpp_version '-DUSE_LITE_METADATA=ON' '-licuuc -lprotobuf-lite'
test_cpp_version '-DUSE_RE2=ON' "$BASE_LIBS -lre2"
test_cpp_version '-DUSE_STD_MAP=ON' "$BASE_LIBS"

# Test Java version using Ant.
(cd java && ant clean jar && ant junit) || exit $?

# Test Java version using Maven.
(cd java && mvn clean package) || exit $?

# Test build tools.
(cd tools/java && mvn clean package) || exit $?
