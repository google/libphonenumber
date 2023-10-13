# Demo App: E.164 Formatter

## What is this?

The E.164 Formatter is an Android App that reads all the phone numbers stored in
the device's contacts and processes them using the
[LibPhoneNumber](https://github.com/google/libphonenumber) Library.

The purpose of this App is to show an example of how LPN can be used in a
real-life situation, in this case specifically in an Android App using Java.

## How can I install the app?

You can use the source code to build the app yourself.

## Where is the LPN code located?

The code using LPN is located in
[`PhoneNumberFormatting#formatPhoneNumberInApp(PhoneNumberInApp, String,
boolean)`](app/src/main/java/com/google/phonenumbers/demoapp/phonenumbers/PhoneNumberFormatting.java#L31)
.

## How does the app work?

On the start screen, the app asks the user for a country to later use when
trying to convert the phone numbers to E.164. After the user starts the process
and grants permission to read and write contacts, the app shows the user two
lists in the UI.

**List 1: Formattable**

Contains all the phone number that are parsable by LPN, are not short numbers,
and are valid numbers and can be reformatted to E.164 using the country selected
on the start screen. In other words, valid locally formatted phone numbers of
the selected country (e.g. `044 668 18 00` if the selected country is
Switzerland).

Each list item (= one phone number in the device's contacts) has a checkbox.
With the click of the button "Update selected" under the list, the app replaces
the phone numbers of the checked list elements in the contacts with the
suggested E.164 replacements.

**List 2: Not formattable**

Shows all the phone number that do not fit the criteria of List 1, each tagged
with one of the following errors:

*   Parsing error
*   Short number (e.g. `112`)
*   Invalid number (e.g. `+41446681800123`)
*   Already E.164 (e.g. `+41446681800`)
