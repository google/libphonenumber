#! /bin/sh

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

# Compiles the Javascript implementation of libphonenumber, using the Google
# Closure Compiler.
#
# The environment variable CLASSPATH is used to provide the location of the
# Closure Compiler JAR (compiler.jar), and the environment variable CLOSUREPATH
# is used to provide the root directory of the Closure library source files. If
# the defaults don't work for your system, override them on the command line,
# for example like this:
#
# CLASSPATH=/opt/closure-compiler/compiler.jar ./compile.sh

: ${CLASSPATH:='/usr/local/share/closure-compiler/compiler.jar'}
: ${CLOSUREPATH:='../../closure-library/closure/goog'}

# If CLASSPATH was set by this script, it needs to be exported to make it
# visible to the JVM.
export CLASSPATH

SRCPATH='i18n/phonenumbers'

function jscomp {
  java com.google.javascript.jscomp.CommandLineRunner \
    --jscomp_warning=deprecated \
    --jscomp_warning=missingProperties \
    --js $CLOSUREPATH/base.js \
    --js $CLOSUREPATH/array/array.js \
    --js $CLOSUREPATH/asserts/asserts.js \
    --js $CLOSUREPATH/debug/error.js \
    --js $CLOSUREPATH/object/object.js \
    --js $CLOSUREPATH/proto2/descriptor.js \
    --js $CLOSUREPATH/proto2/fielddescriptor.js \
    --js $CLOSUREPATH/proto2/lazydeserializer.js \
    --js $CLOSUREPATH/proto2/message.js \
    --js $CLOSUREPATH/proto2/pbliteserializer.js \
    --js $CLOSUREPATH/proto2/serializer.js \
    --js $CLOSUREPATH/proto2/util.js \
    --js $CLOSUREPATH/string/string.js \
    --js $CLOSUREPATH/string/stringbuffer.js \
    "$@"
}

echo 'Compiling with regular metadata...'

jscomp \
  --js $SRCPATH/asyoutypeformatter.js \
  --js $SRCPATH/metadata.js \
  --js $SRCPATH/phonemetadata.pb.js \
  --js $SRCPATH/phonenumber.pb.js \
  --js $SRCPATH/phonenumberutil.js \
  --js_output_file=libphonenumber-compiled.js \
  --output_manifest=libphonenumber-compiled.MF

echo 'Compiling with lite metadata...'

jscomp \
  --js $SRCPATH/asyoutypeformatter.js \
  --js $SRCPATH/metadatalite.js \
  --js $SRCPATH/phonemetadata.pb.js \
  --js $SRCPATH/phonenumber.pb.js \
  --js $SRCPATH/phonenumberutil.js \
  --js_output_file=libphonenumber-lite-compiled.js \
  --output_manifest=libphonenumber-lite-compiled.MF

echo 'Compiling with metadata for testing...'

jscomp \
  --js $SRCPATH/asyoutypeformatter.js \
  --js $SRCPATH/metadatafortesting.js \
  --js $SRCPATH/phonemetadata.pb.js \
  --js $SRCPATH/phonenumber.pb.js \
  --js $SRCPATH/phonenumberutil.js \
  --js_output_file=libphonenumber-testing-compiled.js \
  --output_manifest=libphonenumber-testing-compiled.MF

echo 'Done.'
