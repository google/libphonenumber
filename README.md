<p align="right">
<img src="https://travis-ci.org/googlei18n/libphonenumber.svg?branch=master">
</p>

# What is it?

Google's common Java, C++ and JavaScript library for parsing, formatting, and
validating international phone numbers. The Java version is optimized for
running on smartphones, and is used by the Android framework since 4.0 (Ice
Cream Sandwich).

# Want to report an issue?
If you want to report an issue, or to contribute to the project, please read
the guidelines [here](CONTRIBUTING.md) first.

# Highlights of functionality
  * Parsing/formatting/validating phone numbers for all countries/regions of the world.
  * ` getNumberType ` - gets the type of the number based on the number itself; able to distinguish Fixed-line, Mobile, Toll-free, Premium Rate, Shared Cost, VoIP and Personal Numbers  (whenever feasible).
  * ` isNumberMatch ` - gets a confidence level on whether two numbers could be the same.
  * ` getExampleNumber `/` getExampleNumberByType ` - provides valid example numbers for all countries/regions, with the option of specifying which type of example phone number is needed.
  * ` isPossibleNumber ` - quickly guessing whether a number is a possible phonenumber by using only the length information, much faster than a full validation.
  * ` isValidNumber ` - full validation of a phone number for a region using length and prefix information.
  * ` AsYouTypeFormatter ` - formats phone numbers on-the-fly when users enter each digit.
  * ` findNumbers ` - finds numbers in text input.
  * ` PhoneNumberOfflineGeocoder ` - provides geographical information related to a phone number.
  * ` PhoneNumberToCarrierMapper ` - provides carrier information related to a phone number.
  * ` PhoneNumberToTimeZonesMapper ` - provides timezone information related to a phone number.

# Demo (v8.4.3)
[Java](http://libphonenumber.appspot.com/)

[JavaScript](https://rawgit.com/googlei18n/libphonenumber/master/javascript/i18n/phonenumbers/demo-compiled.html)

# Code
To include the code in your application, either integrate with Maven or
download the latest Jars from the Maven repository:

http://repo1.maven.org/maven2/com/googlecode/libphonenumber/libphonenumber/

# Javadoc

[Latest release](https://javadoc.io/doc/com.googlecode.libphonenumber/libphonenumber/)

# FAQ

See the [FAQ](FAQ.md) for common questions and tips.

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
http://groups.google.com/forum/#!forum/libphonenumber-discuss). Such changes
are not reflected in the version number, and we would publish a sub-minor
release if there were no other changes.

Want to get notified of new releases? During most of the year, excepting
holidays and extenuating circumstances, we release fortnightly. We update
[release tags](http://github.com/googlei18n/libphonenumber/releases) and
document detailed [release notes](
http://github.com/googlei18n/libphonenumber/blob/master/release_notes.txt).
We also send an announcement to [libphonenumber-discuss](
http://groups.google.com/forum/#!forum/libphonenumber-discuss) for every
release.

# Quick Examples
Let's say you have a string representing a phone number from Switzerland. This is how you parse/normalize it into a ` PhoneNumber ` object:

```java
String swissNumberStr = "044 668 18 00";
PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
try {
  PhoneNumber swissNumberProto = phoneUtil.parse(swissNumberStr, "CH");
} catch (NumberParseException e) {
  System.err.println("NumberParseException was thrown: " + e.toString());
}
```

At this point, swissNumberProto contains:
```json
{
  "country_code": 41,
  "national_number": 446681800
}
```

` PhoneNumber ` is a class that is auto-generated from the phonenumber.proto with necessary modifications for efficiency. For details on the meaning of each field, refer to https://github.com/googlei18n/libphonenumber/blob/master/resources/phonenumber.proto

Now let us validate whether the number is valid:
```java
boolean isValid = phoneUtil.isValidNumber(swissNumberProto); // returns true
```

There are a few formats supported by the formatting method, as illustrated below:
```java
// Produces "+41 44 668 18 00"
System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.INTERNATIONAL));
// Produces "044 668 18 00"
System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.NATIONAL));
// Produces "+41446681800"
System.out.println(phoneUtil.format(swissNumberProto, PhoneNumberFormat.E164));
```

You could also choose to format the number in the way it is dialed from another country:

```java
// Produces "011 41 44 668 1800", the number when it is dialed in the United States.
System.out.println(phoneUtil.formatOutOfCountryCallingNumber(swissNumberProto, "US"));
```

### Formatting Phone Numbers 'as you type'
```java
PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("US");
System.out.println(formatter.inputDigit('6'));  // Outputs "6"
...  // Input more digits
System.out.println(formatter.inputDigit('3'));  // Now outputs "650 253"
```

### Geocoding Phone Numbers offline
```java
PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
// Outputs "Zurich"
System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.ENGLISH));
// Outputs "Zürich"
System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.GERMAN));
// Outputs "Zurigo"
System.out.println(geocoder.getDescriptionForNumber(swissNumberProto, Locale.ITALIAN));
```

### Mapping Phone Numbers to carrier
```java
PhoneNumber swissMobileNumber =
    new PhoneNumber().setCountryCode(41).setNationalNumber(798765432L);
PhoneNumberToCarrierMapper carrierMapper = PhoneNumberToCarrierMapper.getInstance();
// Outputs "Swisscom"
System.out.println(carrierMapper.getNameForNumber(swissMobileNumber, Locale.ENGLISH));
```


---


More examples on how to use the library can be found in the unittests at https://github.com/googlei18n/libphonenumber/tree/master/java/libphonenumber/test/com/google/i18n/phonenumbers

# Third-party Ports

Several third-party ports of the phone number library are known to us. We share
them here in case they're useful for developers.

However, we emphasize that these ports are by developers outside the
libphonenumber project. We do not evaluate their quality or influence their
maintenance processes.

*   C#: https://github.com/aidanbebbington/libphonenumber-csharp
*   Javascript: If you don't want to use our version, which depends on Closure,
    there are several other options, including
    https://github.com/halt-hammerzeit/libphonenumber-js (a stripped-down
    version) and https://github.com/seegno/google-libphonenumber (installable
    via npm, a browserify-compatible wrapper)
*   Objective-c: https://github.com/iziz/libPhoneNumber-iOS
*   PHP: https://github.com/giggsey/libphonenumber-for-php
*   PostgreSQL in-database types: https://github.com/blm768/pg-libphonenumber
*   Python: https://github.com/daviddrysdale/python-phonenumbers
*   Ruby: https://github.com/mobi/telephone_number
