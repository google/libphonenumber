<p align="right">
<img src="https://travis-ci.org/google/libphonenumber.svg?branch=master">
</p>

# What is it?

Google's common Java, C++ and JavaScript library for parsing, formatting, and
validating international phone numbers. The Java version is optimized for
running on smartphones, and is used by the Android framework since 4.0 (Ice
Cream Sandwich).

# Quick links

*   **Reporting an issue?** Want to send a pull request? See the [contribution
    guidelines](CONTRIBUTING.md)
*   Check the [frequently asked questions](FAQ.md)
*   Fun! [Falsehoods Programmers Believe About Phone Numbers](FALSEHOODS.md)
*   Look for
    [`README`s](https://github.com/google/libphonenumber/find/master) in
    directories relevant to the code you're interested in.
*   For contributors and porters: [How to run the Java demo](run-java-demo.md)
*   For porters: [How to make metadata changes](making-metadata-changes.md)

# Highlights of functionality

*   Parsing, formatting, and validating phone numbers for all countries/regions
    of the world.
*   `getNumberType` - gets the type of the number based on the number itself;
    able to distinguish Fixed-line, Mobile, Toll-free, Premium Rate, Shared
    Cost, VoIP, Personal Numbers, UAN, Pager, and Voicemail (whenever feasible).
*   `isNumberMatch` - gets a confidence level on whether two numbers could be
    the same.
*   `getExampleNumber` and `getExampleNumberForType` - provide valid example
    numbers for all countries/regions, with the option of specifying which type
    of example phone number is needed.
*   `isPossibleNumber` - quickly guesses whether a number is a possible
    phone number by using only the length information, much faster than a full
    validation.
*   `isValidNumber` - full validation of a phone number for a region using
    length and prefix information.
*   `AsYouTypeFormatter` - formats phone numbers on-the-fly when users enter
    each digit.
*   `findNumbers` - finds numbers in text.
*   `PhoneNumberOfflineGeocoder` - provides geographical information related to
    a phone number.
*   `PhoneNumberToCarrierMapper` - provides carrier information related to a
    phone number.
*   `PhoneNumberToTimeZonesMapper` - provides timezone information related to a
    phone number.

# Demo

## Java

The [Java demo](https://libphonenumber.appspot.com/) is updated with a slight
delay after the GitHub release.

Last demo update: v8.13.30.

Note: Even though the library (main branch/[maven release](https://repo1.maven.org/maven2/com/googlecode/libphonenumber/libphonenumber/8.12.56/))
is at v8.12.57, because of some deployment issues, we were unable to update the
Java demo with the new binary version. We will soon fix this. Meantime, please
use JS demo.

If this number is lower than the [latest release's version
number](https://github.com/google/libphonenumber/releases), we are between
releases and the demo may be at either version.

### Demo App

There is a demo Android App called [E.164 Formatter](java/demoapp) in this
repository. The purpose of this App is to show an example of how the library can
be used in a real-life situation, in this case specifically in an Android App
using Java.

## JavaScript

The [JavaScript
demo](https://htmlpreview.github.io/?https://github.com/google/libphonenumber/blob/master/javascript/i18n/phonenumbers/demo-compiled.html)
may be run at various tags; this link will take you to `master`.

# Java code

To include the Java code in your application, either integrate with Maven (see
[wiki](https://github.com/google/libphonenumber/wiki)) or download the latest
jars from the [Maven
repository](https://repo1.maven.org/maven2/com/googlecode/libphonenumber/libphonenumber/).

# Javadoc

Javadoc is automatically updated to reflect the latest release at
https://javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/.

# Versioning and Announcements

We generally choose the release number following these guidelines.

If any of the changes pushed to master since the last release are incompatible
with the intent / specification of an existing libphonenumber API or may cause
libphonenumber (Java, C++, or JS) clients to have to change their code to keep
building, we publish a major release. For example, if the last release were
7.7.3, the new one would be 8.0.0.

If any of those changes *enable* clients to update their code to take advantage
of new functionality, and if clients would have to roll-back these changes in
the event that the release was marked as "bad", we publish a minor release. For
example, we'd go from 7.7.3 to 7.8.0.

Otherwise, including when a release contains only
[metadata](FAQ.md#metadata_definition) changes, we publish a sub-minor release,
e.g. 7.7.3 to 7.7.4.

Sometimes we make internal changes to the code or metadata that, while not
affecting compatibility for clients, could affect compatibility for **porters**
of the library. For such changes we make announcements to
[libphonenumber-discuss](
https://groups.google.com/forum/#!forum/libphonenumber-discuss). Such changes
are not reflected in the version number, and we would publish a sub-minor
release if there were no other changes.

Want to get notified of new releases? During most of the year, excepting
holidays and extenuating circumstances, we release fortnightly. We update
[release tags](https://github.com/google/libphonenumber/releases) and
document detailed [release notes](
https://github.com/google/libphonenumber/blob/master/release_notes.txt).
We also send an announcement to [libphonenumber-discuss](
https://groups.google.com/forum/#!forum/libphonenumber-discuss) for every
release.

# Quick Examples

Let's say you have a string representing a phone number from Switzerland. This
is how you parse/normalize it into a `PhoneNumber` object:

```java
String swissNumberStr = "044 668 18 00";
PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
try {
  PhoneNumber swissNumberProto = phoneUtil.parse(swissNumberStr, "CH");
} catch (NumberParseException e) {
  System.err.println("NumberParseException was thrown: " + e.toString());
}
```

At this point, `swissNumberProto` contains:

```json
{
  "country_code": 41,
  "national_number": 446681800
}
```

`PhoneNumber` is a class that was originally auto-generated from
`phonenumber.proto` with necessary modifications for efficiency. For details on
the meaning of each field, refer to `resources/phonenumber.proto`.

Now let us validate whether the number is valid:

```java
boolean isValid = phoneUtil.isValidNumber(swissNumberProto); // returns true
```

There are a few formats supported by the formatting method, as illustrated
below:

```java
// Produces "+41 44 668 18 00"
System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.INTERNATIONAL));
// Produces "044 668 18 00"
System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.NATIONAL));
// Produces "+41446681800"
System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.E164));
```

You could also choose to format the number in the way it is dialed from another
country:

```java
// Produces "011 41 44 668 1800", the number when it is dialed in the United States.
System.out.println(phoneUtil.formatOutOfCountryCallingNumber(swissNumberProto, "US"));
```

## Formatting Phone Numbers 'as you type'

```java
PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("US");
System.out.println(formatter.inputDigit('6'));  // Outputs "6"
...  // Input more digits
System.out.println(formatter.inputDigit('3'));  // Now outputs "650 253"
```

## Geocoding Phone Numbers

```java
PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
// Outputs "Zurich"
System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.ENGLISH));
// Outputs "Zürich"
System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.GERMAN));
// Outputs "Zurigo"
System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.ITALIAN));
```

## Mapping Phone Numbers to original carriers

Caveat: We do not provide data about the current carrier of a phone number, only
the original carrier who is assigned the corresponding range. Read about [number
portability](FAQ.md#what-is-mobile-number-portability).

```java
PhoneNumber swissMobileNumber =
    new PhoneNumber().setCountryCode(41).setNationalNumber(798765432L);
PhoneNumberToCarrierMapper carrierMapper = PhoneNumberToCarrierMapper.getInstance();
// Outputs "Swisscom"
System.out.println(carrierMapper.getNameForNumber(swissMobileNumber, Locale.ENGLISH));
```

More examples on how to use the library can be found in the [unit
tests](https://github.com/google/libphonenumber/tree/master/java/libphonenumber/test/com/google/i18n/phonenumbers).

# Third-party Ports

Several third-party ports of the phone number library are known to us. We share
them here in case they're useful for developers.

However, we emphasize that these ports are by developers outside the
libphonenumber project. We do not evaluate their quality or influence their
maintenance processes.

*   C#: https://github.com/twcclegg/libphonenumber-csharp
*   Gleam: https://github.com/massivefermion/phony
*   Go: https://github.com/nyaruka/phonenumbers
*   Objective-c: https://github.com/iziz/libPhoneNumber-iOS
*   Swift: https://github.com/marmelroy/PhoneNumberKit
*   PHP: https://github.com/giggsey/libphonenumber-for-php
*   PostgreSQL in-database types: https://github.com/blm768/pg-libphonenumber
*   Python: https://github.com/daviddrysdale/python-phonenumbers
*   Ruby: https://github.com/ianks/mini_phone
*   Ruby: https://github.com/daddyz/phonelib
*   Ruby: https://github.com/mobi/telephone_number
*   Rust: https://github.com/1aim/rust-phonenumber
*   Erlang: https://github.com/marinakr/libphonenumber_erlang
*   Clojure: https://github.com/randomseed-io/phone-number
*   R: https://github.com/socialresearchcentre/dialr/
*   Elixir: https://github.com/socialpaymentsbv/ex_phone_number
*   Salesforce: https://appexchange.salesforce.com/appxListingDetail?listingId=a0N3A00000G12oJUAR

Alternatives to our own versions:

*   Android-optimized: Our Java version loads the metadata from
    `Class#getResourcesAsStream` and asks that Android apps follow the Android
    loading best practices of repackaging the metadata and loading from
    `AssetManager#open()` themselves
    ([FAQ](https://github.com/google/libphonenumber/blob/master/FAQ.md#optimize-loads)).
    If you don't want to do this, check out the port at
    https://github.com/MichaelRocks/libphonenumber-android, which does repackage
    the metadata and use `AssetManager#open()`, and may be depended on without
    needing those specific loading optimizations from clients. You should also check
    out the port at https://github.com/lionscribe/libphonenumber-android which also
    supports geocoding, and only requires a one line code change.
*   Javascript: If you don't want to use our version, which depends on Closure,
    there are several other options, including
    https://github.com/catamphetamine/libphonenumber-js - a stripped-down
    rewrite, about 110 KB in size - and
    https://github.com/seegno/google-libphonenumber - a browserify-compatible
    wrapper around the original unmodified library installable via npm, which
    packs the Google Closure library, about 420 KB in size.

Tools based on libphonenumber metadata:

*   Scala: https://github.com/mr-tolmach/raf - library for generating valid phone numbers in the E.164 format
