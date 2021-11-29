Info:
=====
Google's JavaScript library for parsing, formatting, and validating
international phone numbers.


How to setup:
=============
1.  Checkout closure-library, closure-compiler, closure-linter and python-gflags next to libphonenumber:

* `git clone https://github.com/google/libphonenumber/`

* `git clone https://github.com/google/closure-library/`

* `git clone https://github.com/google/closure-compiler.git`

* `git clone https://github.com/google/closure-linter.git`

* `git clone https://github.com/google/python-gflags.git`

1.  We officially support only these versions of these dependencies:

* Closure library: v20201006

* Closure compiler: v20210302

* Closure linter: v2.3.19

* Python gflags: 3.1.2

Note: We were to build at latest versions of these dependencies, however, we cannot promise that
we continue to support newer version of these dependencies. We learned that newer Closure binaries
deprecate older apis earlier, leading to build breakages.

If you don't checkout the dependencies next to libphonenumber:

1. Change the path of the `<script src="">` in the html pages to point to wherever base.js is located
2. Update `javascript/build.xml` with the correct paths

3. Run the unit tests to make sure everything is working. Open the following pages with your web browser:
  `javascript/i18n/phonenumbers/phonenumberutil_test.html`
  `javascript/i18n/phonenumbers/asyoutypeformatter_test.html`

4. Run the demo: `javascript/i18n/phonenumbers/demo.html`


How to compile:
===============
1. Build the Closure Compiler JAR file by following the directions on the
   [Closure Compiler README](https://github.com/google/closure-compiler/tree/master/README.md).
   If this step doesn't work, try updating your local copy of each of the
   repositories listed above before filing an issue.

2. Compile the demo.js and all its dependencies to one file: `demo-compiled.js`:
  `ant -f javascript/build.xml compile-demo`

3. Run the compiled demo: `javascript/i18n/phonenumbers/demo-compiled.html`


How to use:
===========
To use and compile the library in your own project, use the `javascript/i18n/phonenumbers/demo.js` as an example. You will need to goog.exportSymbol all the methods you use in your html so that the compiler won't rename them. You can then invoke the compiler similarly to how the compile-demo ant target in `javascript/build.xml` invokes it.


How to update:
==============
The JavaScript library is ported from the Java implementation.
When the Java project gets updated follow these steps to update the JavaScript
project:

1. If the protocol buffers (phonemetadata.proto and phonenumber.proto) have changed:
  * Manually update the .pb.js files with the changes of the .proto files.
  * Manually update the toJsArray() Java methods in tools/java/java-build/src/com/google/i18n/phonenumbers/BuildMetadataJsonFromXml.java
  * Build `tools/java/java-build/target/java-build-1.0-SNAPSHOT-jar-with-dependencies.jar` by running: `mvn -f tools/java/java-build/pom.xml package`

2. If the phone number metadata in the XML format has changed `resources/PhoneNumberMetadata.xml` run the following commands to regenerate `metadata.js` and `metadatafortesting.js`:

  `ant -f java/build.xml build-js-metadata`

3. Manually port any changes of the Java code to the JavaScript code:
  * PhoneNumberUtil.java => phonenumberutil.js
  * AsYouTypeFormatter.java => asyoutypeformatter.js
  * PhoneNumberUtilTest.java => phonenumberutil_test.js
  * AsYouTypeFormatterTest.java => asyoutypeformatter_test.js

4. Run the Closure Compiler to get your changes syntax and type checked. This will also generate `demo-compiled.js` used by `demo-compiler.html`

  `ant -f javascript/build.xml compile`

5. Run the Closure Linter to lint the JavaScript files:

  `ant -f javascript/build.xml lint`


Missing functionality:
=====
1. JS port does not support extracting phone-numbers from text (findNumbers).
2. JS port does not have an offline phone number geocoder.
3. JS port of PhoneNumberUtil does not handle all digits, only a subset (JavaScript has no equivalent to the Java Character.digit).
