# Frequently Asked Questions (FAQ)

## Validation and types of numbers

### What is the difference between isPossibleNumber and isValidNumber?

To understand the behavior of functions, please refer to the documentation in
the Javadoc/C++ header files. For example, see `isPossibleNumberWithReason` in
[`PhoneNumberUtil`]
(https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java).

### Why does PhoneNumberUtil return false for valid short numbers?

Short numbers are out of scope of
[`PhoneNumberUtil`]
(https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java).
For short numbers, use
[`ShortNumberInfo`]
(https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/ShortNumberInfo.java).

### What does it mean for a phone number to be valid?

Our phone number library can tell that a number range is valid when there is
sufficient official documentation, with some latency after this fact is brought
to our attention via issue reports or notifications (see below for more
information on where our metadata comes from). A valid number range is one from
which numbers can be freely assigned by carriers to users.

Do not rely on libphonenumber to determine whether numbers are currently
assigned to a specific user and reachable. Some products (e.g.
[Google 2-step verification] (https://www.google.com/landing/2step/)) do
this with a verification step e.g. by sending an SMS or placing an automated
phone call with a verification code). This is not technically feasible without
such a verification step given the complicated international world we live in,
with varying standardization practices in different regions.

### When should I use isValidNumberForRegion?

Rarely! Many people have phone numbers that do not belong to the country they
live in. This applies particularly to mobile numbers, but may also be true
for VoIP numbers etc. Note also that the regions your application supports may
not be the same as the regions we support. For example, the Channel Islands such
as "Jersey" have their own region code - JE. If you allow these users to sign up
as a British user ("GB"), their phone numbers will not be considered valid for
the region "JE".

One use-case where this method may be useful is if you want to see if a
`FIXED_LINE` number for a business matches the country it is in, to try and spot
data errors.

### What types of phone numbers can SMSs be sent to?

SMSs can be sent to `TYPE_MOBILE` or `TYPE_FIXED_LINE_OR_MOBILE` numbers. However,
in some countries it is possible to configure other types, such as normal
land-lines, to receive SMSs.

### Why did I get `TYPE_FIXED_LINE_OR_MOBILE` as the type of my phone number?

Some number ranges are explicitly defined as being for fixed-line or mobile
phones. We even represent ranges defined as being "Mostly land-line" in this
way.

### What about M2M (machine to machine) numbers?

libphonenumber does not support M2M numbers at the moment, but might in the
future.

One of the reasons libphonenumber doesn't support M2M so far is because no one
could explain their use to us sufficiently.

We don't require that a number to be supported by the library has a human at the
other end since we already accept premium rate services and they might go to an
automated system instead. But to date we only accept ranges that a human might
call or send an SMS to.

M2M numbers would violate this assumption and we'd have to evaluate the
consequences for existing APIs and clients if M2M numbers would be considered
valid by the library.

Many people use this library for formatting the numbers of their contacts, for
allowing people to sign up for services, for working out how to dial someone in
a different country, for working out what kind of cost might be associated with
a number in an advert, etc. We don't think the lack of M2M support hinders any
of those use-case, but we might be wrong.

If you would like libphonenumber to support M2M numbers, please engage with the
developer community at [Support M2M numbers #680]
(https://github.com/googlei18n/libphonenumber/issues/680) with further
information to address our questions and concerns and please describe what kinds
of use-cases fail because M2M numbers are not supported by the library.

More information on this issue would be very welcomed!

Related issues: [Support M2M numbers #680]
(https://github.com/googlei18n/libphonenumber/issues/680),
[#930: JTGlobal - an MNO based in the UK]
(https://github.com/googlei18n/libphonenumber/issues/930), [#976: Norway]
(https://github.com/googlei18n/libphonenumber/issues/976), [#985: South Africa,
Vodacom](https://github.com/googlei18n/libphonenumber/issues/985), [#910:
Sweden](https://github.com/googlei18n/libphonenumber/issues/910), [#657:
Canada](https://github.com/googlei18n/libphonenumber/issues/657), [#550:
Belgium](https://github.com/googlei18n/libphonenumber/issues/550), [#351:
Norway](https://github.com/googlei18n/libphonenumber/issues/351), [#332:
Netherlands](https://github.com/googlei18n/libphonenumber/issues/332)

## Representation

### What is the maximum and minimum length of a phone number?

We support parsing and storing numbers from a minimum length of two digits to a
maximum length of 17 digits currently (excluding country calling code). The ITU
standard says the national significant number should not be longer than
fifteen digits, but empirically this has been proven not to be followed by all
countries.

## Formatting

### Can / should we format phone numbers in a language-specific way?

No, phone number formatting is country-specific and language-independent. E.g.
formatting a US number in a French way (e.g. the way a France number is
formatted) for a French user is undefined and wrong.

It is true that in some countries phone numbers are typically written using
native, not ASCII, digits; our phone number library supports parsing these but
doesn't support it at formatting time at the moment.

### Why does formatNumberForMobileDialing return an empty string for my number?

If we don't think we can guarantee that the number is diallable from the user's
mobile phone, we won't return anything. This means that for numbers that we
don't think are internationally diallable, if the user is outside the country
we will return an empty string. Similarly, in Brazil a carrier code is essential
for dialling long-distance domestically. If none has been provided at parsing
time then we will return an empty string. If you get an empty string and are
okay providing a number that may not be diallable, you can call another of our
formatting numbers instead.

## Metadata

### Where do we get information from to determine if a number range is valid?

In theory, phone numbering plans are all supposed to be administered through the
ITU. Many countries' phone numbering plans may be found on the [ITU website](
http://www.itu.int/oth/T0202.aspx?parent=T0202).

We receive automatic notifications when a new ITU plan has been filed, which may
or may not be before it comes into effect.

Not every country files their numbering plans with the ITU nor are the plans
filed with ITU always up to date. In some countries, the numbering plans are
directly handled by a government authority, while in others, most of the work
is done by telecom companies (the government's role being only to distribute
ranges at the prefix level, with the actual partitioning within the prefix done
by the telecom).

A large part of the data in `PhoneNumberMetadata.xml` comes from the ITU
documents, but because they're sometimes insufficient, we also include data from
other sources, including user bug reports, telecom company home pages and
government telecommunication authorities.

There is no RFC indicating where the data comes from, or what format they're in.

We'd love to consume machine-readable numbering plan data (assigned ranges,
carrier & geo mappings). If you can connect us with partners in the industry
to achieve this, please do so. Thanks!
