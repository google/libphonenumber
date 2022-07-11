# How to make metadata changes

## Introduction

These steps outline how to edit the metadata for the phone number library to fix
problems with validation or formatting.

Note that we cannot accept pull requests to modify metadata directly, but you may
use these guidelines to create changes in your own fork until we fix issues
upstream. Please [file a corresponding issue](CONTRIBUTING.md#checklist-before-filing-an-issue)
with us.

You can read more about the pull request [contribution guidelines](CONTRIBUTING.md#pull-requests).

## Details

### Edit the data

Edit the appropriate files:

*   `resources/PhoneNumberMetadata.xml` for normal validation / formatting
    issues
*   `resources/ShortNumberMetadata.xml` for short-code data
*   `resources/PhoneNumberAlternateFormats.xml` for alternate patterns for phone
    number matching
*   `resources/geocoding/xx` for geocoding data (where `xx` is the language code
    you wish to edit)
*   `resources/carrier/xx` for carrier mapping data

Note:

*   To track provenance for your own maintenance needs, consider including
    sources for data where appropriate
*   If multiple countries share a country calling code, check all of them are
    updated. Formatting rules will only be listed by the country with
    `mainCountryForCode` set to `true`.
*   Details on each field in the xml file can be found at the top of the file
    and in `resources/phonemetadata.proto`.

### Generate data files

#### Java

```
ant -f java/build.xml junit
```

This will generate new binary files under
`java/libphonenumber/src/com/google/i18n/phonenumbers/data`,
`geocoder/src/com/google/i18n/phonenumbers/carrier/data`, and
`geocoder/src/com/google/i18n/phonenumbers/geocoding/data`.

#### Javascript

```
ant -f java/build.xml build-js-metadata
```

This will generate new js metadata files; you should also now compile the
changes, as per the instructions in `javascript/README`.

#### C++

See the [C++
README](http://github.com/google/libphonenumber/blob/master/cpp/README)
instructions for how to build and run C++. You should build it with
`USE_LITE_METADATA` set to `ON` as well as `OFF`, which will generate both a
`metadata.cc` and a `metadata_lite.cc` file.

### Test changes

#### Java

Build a jar:

```
ant -f java/build.xml jar
```

Then [run your own demo](run-java-demo.md) and test your changes are as
expected.

#### Javascript

See `javascript/README` for how to run the demo page in your browser.
