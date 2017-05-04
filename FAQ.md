# Frequently Asked Questions (FAQ)

## Parsing

### Why wasn't the country code removed when parsing?

In some cases, the library cannot tell if the leading digits of a phone number
are intended to be the country calling code, or the start of the national
significant number.

This affects primarily German phone numbers, where 49 is both a country calling
code and an area code, and numbers of variable lengths are valid. The leading
digits will only be interpreted as a country calling code if the number is not
already considered a possible number for the region provided when parsing.

If you know that your numbers are always in the form &lt;country calling
code&gt;&lt;national significant number&gt;, it is safe to put a "+" in front to
indicate this to the library.

## Validation and types of numbers

### What is the difference between isPossibleNumber and isValidNumber?

To understand the behavior of functions, please refer to the documentation in
the Javadoc/C++ header files. For example, see `isPossibleNumberWithReason` in
[`PhoneNumberUtil`](https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java).

### Why does PhoneNumberUtil return false for valid short numbers?

Short numbers are out of scope of
[`PhoneNumberUtil`](https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java).
For short numbers, use
[`ShortNumberInfo`](https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/ShortNumberInfo.java).

### What does it mean for a phone number to be valid?

Our phone number library can tell that a number range is valid when there is
sufficient official documentation, with some latency after this fact is brought
to our attention via issue reports or notifications (see below for more
information on where our metadata comes from). A valid number range is one from
which numbers can be freely assigned by carriers to users.

Do not rely on libphonenumber to determine whether numbers are currently
assigned to a specific user and reachable. Some products (e.g.
[Google 2-step verification](https://www.google.com/landing/2step/)) do
this with a verification step e.g. by sending an SMS or placing an automated
phone call with a verification code). This is not technically feasible without
such a verification step given the complicated international world we live in,
with varying standardization practices in different regions.

#### But my dialled number connected, so isn't it valid?

Not necessarily. In some countries extra digits at the end are ignored. For
example, dialling `1800 MICROSOFT` in the US connects to `+1 (800) MIC-ROSO`.
Moreover, during renumbering transitions, e.g. when all numbers are getting an
extra `9` added to the front, some carriers will "fix" old numbers long after
they're no longer working for the majority.

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

SMSs can be sent to `MOBILE` or `FIXED_LINE_OR_MOBILE` numbers. However,
in some countries it is possible to configure other types, such as normal
land-lines, to receive SMSs.

### <a name="fixed_line_or_mobile"></a>Why did I get `FIXED_LINE_OR_MOBILE` as the type of my phone number?

Some number ranges are explicitly defined as being for fixed-line or mobile
phones. We even represent ranges defined as being "Mostly land-line" in this
way.

### What is mobile number portability?

The ability to keep your mobile phone number when changing carriers. To see whether a region supports mobile number portability use [isMobileNumberPortableRegion](https://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java#L3275).

### Since it's possible to change the carrier for a phone number, how is the data kept up-to-date?

Not all regions support mobile number portability. For those that don't, we return the carrier when available. For those that do, we return the original carrier for the supplied number.

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
valid by the library. Clients of libphonenumber expect `mobile` and `fixed-line`
numbers to have certain affordances, such as: Reachable for voice calls 
(and for mobile also SMS) as well as assuming standard cost. This expectation 
is broken by the lack of M2M standardization today.

Many people use this library for formatting the numbers of their contacts, for
allowing people to sign up for services, for working out how to dial someone in
a different country, for working out what kind of cost might be associated with
a number in an advert, etc. We don't think the lack of M2M support hinders any
of those use-case, but we might be wrong.

If you would like libphonenumber to support M2M numbers, please engage with the
developer community at [Support M2M numbers #680](
https://github.com/googlei18n/libphonenumber/issues/680) with further
information to address our questions and concerns such as:

*   **How to implement support?** e.g. new category, new library or method
    to call - along with pros and cons, and impact on existing APIs
*   **Authoritative and specific documentation** such as government sources since
    we currently have less than a dozen sources, which have varied definitions

More information and collabortation on this issue would be very welcomed!

Related issues: [Support M2M numbers #680](https://github.com/googlei18n/libphonenumber/issues/680),
[#930: JTGlobal - an MNO based in the UK](https://github.com/googlei18n/libphonenumber/issues/930),
[#976: Norway](https://github.com/googlei18n/libphonenumber/issues/976),
[#985: South Africa, Vodacom](https://github.com/googlei18n/libphonenumber/issues/985),
[#910: Sweden](https://github.com/googlei18n/libphonenumber/issues/910),
[#657: Canada](https://github.com/googlei18n/libphonenumber/issues/657),
[#550: Belgium](https://github.com/googlei18n/libphonenumber/issues/550),
[#351: Norway](https://github.com/googlei18n/libphonenumber/issues/351),
[#332: Netherlands](https://github.com/googlei18n/libphonenumber/issues/332)

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

### <a name="metadata_definition"></a>What do we mean by "metadata"?

We use the word "metadata" to refer to all information about phone numbering in
a particular country - what the country code, international and national
dialling prefixes are, what carrier codes are operational, which phone numbers
are possible or valid for a particular country, how to optimally format them,
which prefixes represent a particular geographical area, etc.

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

### Why is this number from Argentina (AR) or Mexico (MX) not identified as the right number type?

Certain countries' mobile and/or fixed line ranges may overlap, which may make
accurate identification impossible without additional and explicit context
such as a mobile prefix. We rely on this prefix being present to correctly identify
the phone number type (rather than returning ['FIXED_LINE_OR_MOBILE'](#fixed_line_or_mobile)
in ambiguous cases) until our metadata can be fine-grained enough to detect when a user has omitted it.

For example, when calling a mobile line from a fixed line in Argentina,
you need to dial 15 before the subscriber number, or 9 if you're calling
from another country. Without these additional digits, your call may not
connect at all!

Similarly, Mexico has different mobile prefixes needed when calling from a fixed
line such as 044 when calling locally, 045 when calling from another state, and
1 when dialing from another country.

Moreover, these countries have different possible lengths for area codes and subscriber
numbers depending on the city, which further complicate matters (e.g. Buenos Aires is 11
followed by eight digits, but Río Gallegos is 2966 followed by six digits).

Despite all the aforementioned complexity, users may not provide their phone number
with all the additional context unless explicitly asked. For instance,
since SMS messages can be sent in Argentina from a mobile phone without a prefix,
the user may not supply the mobile prefix.

### Why are Bouvet Island (BV), Pitcairn Island (PN), Antarctica (AQ) etc. not supported?

We only support a country if:

*   **The country has a single country calling code.** For instance, Kosovo (XK)
    has been using three different country codes until 2017 - those of Serbia,
    Monaco and Slovenia. The relevant numbers will be marked as valid, but as
    belonging to Serbia, Monaco or Slovenia respectively. When Kosovo starts
    using its own country calling code of 383 it will be added to the metadata
    by itself. Similarly, Antarctica doesn't use its assigned country calling
    code of 672 - instead the bases belonging to different countries have
    different solutions. For example, Scott Base, belonging to New Zealand, has
    an area code that is part of the New Zealand phone number plan, and we
    support those numbers as valid numbers for NZ.
*   **The country still exists.** For example, Yugoslavia (YU), Serbia and
    Montenegro (CS) and Netherlands Antilles (AN) have been dissolved and no
    longer exist as political entities so we do not support them.
*   **The country has some phone numbers in use that can be ascribed to it.**
    For instance, Pitcairn Island has only around thirty inhabitants and they
    use satellite phones, so there is no numbering plan for Pitcairn Island.
    Similarly, Bouvet Island is an uninhabited Antarctic volcanic island with no
    telephone country code and no telephone connection, so we will not support
    it.
*   **It has an assigned region code.** For instance, previously Kosovo did not
    have a region code assigned to it, so we could not support it until it was
    assigned XK by [CLDR](http://cldr.unicode.org/).

We support non-geographical entities that have been assigned country calling
codes by the ITU where a numbering plan is available, e.g. "800" (International
Freephone Service) and 870 (Inmarsat SNAC). However we do not support country
calling codes that are only "reserved", or that no data is available for (namely
388 - listed as "Group of countries, shared code" and 991 - listed as "Trial of
a proposed new international telecommunication public correspondence service,
shared code".)

### Why are Indonesian toll-free numbers beginning with "00x 803" not supported?

Although some numbers beginning with "001 803" or "007 803" do work in Indonesia
to reach toll-free endpoints, these numbers are hard to support because they
overlap with the international dialling prefix for Indonesia (IDD). It seems
that since 803 is unassigned and not a valid country code, some local
tel-companies in Indonesia hijack 803 and redirect it to their own services.

We have also found evidence that reaching some "00x 803" numbers cost local or
national tariff, rather than the call being toll-free.

These numbers are not diallable from any other country using their IDD,
and it's unclear whether all carriers in Indonesia support them. If we ever
supported them, they would have to be added to the `noInternationalDialling`
section, and it is likely some changes in the parsing code would have to be
made to interpret the "00x" as something other than an IDD: this could have
undesirable side-effects when parsing other numbers.

## Misc

### <a name="reduced_metadata"></a>What is the metadatalite.js/METADATA_LITE option?

For JavaScript, Java and C++ there is the option to use a stripped-down version
of the metadata. Currently this only removes the example number metadata, so the
savings are not a lot, but we may revisit this.

*Impact:*

-   `getExampleNumber`, `getInvalidExampleNumber`, `getExampleNumberForType`,
     `getExampleNumberForNonGeoEntity` will return `null`
-   Binary size (or download size for JS) will be slightly smaller

*JS:*
Simply include metadatalite.js instead of metadata.js in your project.

*C++:*
Set the compiler flag `USE_METADATA_LITE` to `ON` using ccmake or similar.

*Java:*
The metadata binary files can be generated using the ant build rules
`build-phone-metadata` and `build-short-metadata` with `lite-build` set to
`true`. This can be set in the [build
file](https://github.com/googlei18n/libphonenumber/blob/master/java/build.xml)
itself.

### Which versions of the Maven jars should I use?

When possible, use the [latest
version](https://github.com/googlei18n/libphonenumber/releases) of
libphonenumber.

For the other Maven artifacts, to find the version corresponding to a given
version of libphonenumber, follow these steps:

*   Go to the versioned GitHub tag, e.g.
    https://github.com/googlei18n/libphonenumber/find/v8.3.3
*   Type `pom.xml`. This will surface all the `pom.xml` files as they were
    released at the chosen tag.
*   Find the version you care about in the corresponding `pom.xml` file. Look
    for `<version>` in the top-level `project` element. For example, to find the
    version of the carrier jar corresponding to libphonenumber 8.3.3, open
    `java/carrier/pom.xml` at v8.3.3's search results. This is `1.56`.
*   If you depend on the carrier or geocoder jar, you also need to depend on
        the prefixmapper jar.

### How do I load libphonenumber resources in my Android app?

#### System considerations

tl;dr: Do not call `PhoneNumberUtil` API on the main thread.

If you get surprising exceptions involving metadata loading, e.g. "missing
metadata" exceptions when the metadata exists, then it's probably because you're
loading resources on the main thread.

Please ensure that you don't call `PhoneNumberUtil` API on the main thread. Not
loading resources in the main thread is the suggested best practice at the
[Android developer
guide](http://developer.android.com/guide/components/processes-and-threads.html),
and will prevent the issue reported in
[#265](https://github.com/googlei18n/libphonenumber/issues/265),
[#528](https://github.com/googlei18n/libphonenumber/issues/528), and
[#819](https://github.com/googlei18n/libphonenumber/issues/819).

#### Optimize loads

You can manage your own resources by supplying your own
[`MetadataLoader`](http://github.com/googlei18n/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/MetadataLoader.java)
implementation to the `PhoneNumberUtil` instance. It is thus possible for your
app to load the resources as Android assets, while libphonenumber loads Java
resources by default. The result is that the files are read as native Android assets
and so optimized for speed.

Here's the sample code for how to do it:

```
PhoneNumberUtil util = PhoneNumberUtil.createInstance(new MetadataLoader() {
  @Override
  public InputStream loadMetadata(String metadataFileName) {
    return Application.getContext().getAssets().open("some/asset/path" + metadataFileName);
  }
});
```

You also need to copy the binary metadata files into your app's asset directory, and
automate updating them from upstream. To avoid net increase of app size, remove them
from libphonenumber.

### What about Windows?

The libphonenumber team's support of the C++ library on Windows is primarily to
support Chromium's build environment, and we depend on the community to support
other Windows build environments / build chains. We list here some known issues
that would benefit from open-source collaboration. If you can contribute a PR
or review and test out someone else's PR, please chime in on these links, or
email the [discussion
group](https://groups.google.com/group/libphonenumber-discuss):

*   [#1000](https://github.com/googlei18n/libphonenumber/issues/1000) to provide
    a Windows DLL.
*   [#1010](https://github.com/googlei18n/libphonenumber/issues/1010) to require
    Visual Studio 2015 update 2 or later on Windows
*   PR [#1090](https://github.com/googlei18n/libphonenumber/pull/1090) /
    [#824](https://github.com/googlei18n/libphonenumber/issues/824) to "Replace
    POSIX directory operations by Boost Filesystem"
*   [#1307](https://github.com/googlei18n/libphonenumber/issues/1307) to use
    readdir instead of readdir_r
*   [#1555](https://github.com/googlei18n/libphonenumber/issues/1555) to allow
    Windows to build cpp library with pthreads for multi-threading

### How to remove a specific example number?

We supply example numbers as part of the library API. While we aim to have numbers
that are either explicitly allocated by the country as a test number, or look
fictitious (e.g. 1234567) we also need these numbers to validate correctly.
This means we sometimes have numbers that do connect to a real person.

If we by chance have actually listed your real number and would like it removed,
please report this through Google's new [Issue Tracker](http://issuetracker.google.com/issues/new?component=192347).
Only our internal team will have access to your identity (whereas GitHub usernames are public).
