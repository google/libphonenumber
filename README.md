# What is it?

Google's common Java, C++ and JavaScript library for parsing, formatting, and validating international phone numbers. The Java version is optimized for running on smartphones, and is used by the Android framework since 4.0 (Ice Cream Sandwich).

# Want to report an issue?
If you want to report an issue, or to contribute to the project, please read the guidelines [here] (https://github.com/googlei18n/libphonenumber/blob/master/CONTRIBUTING.md) first.

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

# Demo (v7.2.7)
[Java](http://libphonenumber.appspot.com/)

[JavaScript](https://rawgit.com/googlei18n/libphonenumber/master/javascript/i18n/phonenumbers/demo-compiled.html)

# Code
To include the code in your application, either integrate with Maven or download the latest Jars from the Maven repository:

http://repo1.maven.org/maven2/com/googlecode/libphonenumber/libphonenumber/

# Quick Examples
Let's say you have a string representing a phone number from Switzerland. This is how you parse/normalize it into a ` PhoneNumber ` object:

```java
String swissNumberStr = "044 668 18 00"
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

` PhoneNumber ` is a class that is auto-generated from the phonenumber.proto with necessary modifications for efficiency. For details on the meaning of each field, refer to https://github.com/googlei18n/test/blob/master/resources/phonenumber.proto

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


More examples on how to use the library can be found in the unittests at https://github.com/googlei18n/test/tree/master/java/libphonenumber/test/com/google/i18n/phonenumbers

# Known Ports
Several people are porting the phone number library to other languages. Here are some we know about. Note that they are done on voluntary basis by developers outside our project, so we cannot guarantee their quality.
  * C#: https://github.com/erezak/libphonenumber-csharp
  * objective-c: https://github.com/iziz/libPhoneNumber-iOS
  * Python: https://github.com/daviddrysdale/python-phonenumbers
  * Ruby: https://github.com/sstephenson/global_phone
  * PHP: https://github.com/giggsey/libphonenumber-for-php
