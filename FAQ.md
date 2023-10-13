# Frequently Asked Questions (FAQ)

## <a href="#parsing">Parsing</a>

### <a href="#country_code_not_removed">Why wasn't the country code removed when parsing?</a>

In some cases, the library cannot tell if the leading digits of a phone number
are intended to be the country calling code, or the start of the national
significant number.

This affects primarily German and Indonesian phone numbers, where country
calling code (49 and 62 respectively) is also a valid area code, and numbers of
variable lengths are valid. The leading digits will only be interpreted as a
country calling code if the number is not already considered a possible number
for the region provided when parsing.

If you know that your numbers are always in the form &lt;country calling
code&gt;&lt;national significant number&gt;, it is safe to put a "+" in front to
indicate this to the library.

### <a href="#non_digits">Why does the library treat some non-digit characters as digits?</a>

When parsing, the library does its best to extract a phone number out of the
given input string. It looks for the phone number in the provided text; it
doesn't aim to verify whether the string is *only* a phone number.

If the input looks like a vanity number to the library, `parse()` assumes this
is intentional and converts alpha characters to digits. Please read the
documentation for `PhoneNumber parse(String, String)` in
[PhoneNumberUtil](http://github.com/google/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java)
for details. Also see `Iterable<PhoneNumberMatch> findNumbers(CharSequence,
String)`.

Some examples:

*   `+1 412 535 abcd` is parsed the same as `+1 412 535 2223`.

*   If someone mistypes and adds an extra alpha character in the *middle*,
    then the library assumes this was a mistake and fixes it. E.g. the extra `c`
    in `+1 412 535 c0000` is ignored, and this is parsed the same as `+1 412 535
    0000`.

*   If someone mistypes and *replaces* a digit in the middle with an alpha
    character, and the remaining characters do not make up a valid number, this
    alpha character is not converted and the resulting number is invalid, e.g.
    with `+1 412 535 c000`.


### <a href="#prefix_not_removed">Why wasn't the national prefix removed when parsing?</a>

Usually, when parsing, we remove a country's national or trunk prefix, so we can
store a normalized form of the number. This is usually, but not always, a
leading zero. In some situations, we don't remove this, but instead keep it as
part of the national number:

1.  If a country does not use a national prefix, or does not use one anymore, we
    don't remove a leading zero, since then if the user wanted to format the
    number we would never know to prefix it with this leading zero.
1.  If the leading zero is not a national prefix but is needed for dialling from
    abroad (e.g. in the case of Italy) it is stored in the proto, not removed as
    a national prefix.
1.  If the number is too short to be a valid phone number in this country, we do
    not remove the national prefix. For instance, although `0` is a national
    prefix in Australia, we do not remove it from the number `000` which is the
    emergency number; if we did we would not know that it needs a `0` re-added
    when formatting since other short-codes do not, and we would be irreparably
    changing the phone number.

### <a href="#parsing_idds">Why wasn't `00` parsed the same as `+`?</a>

`00` is not an international prefix (IDD) in every region, so it's not
equivalent to `+`.

For example, if [parsing `0015417540000` as if dialled from the
US](http://libphonenumber.appspot.com/phonenumberparser?number=0015417540000&country=US),
the national number is `15417540000` because `00` is not an international prefix
in the US.

Ascension Island, however, does have `00` as an international prefix, so `00`
there works the same as `+` and [`0015417540000` as if dialled from
`AC`](http://libphonenumber.appspot.com/phonenumberparser?number=0015417540000&country=AC)
gives `5417540000` for the national number.

You can try [the demo](http://libphonenumber.appspot.com/) for more regions.
Also see `internationalPrefix` in
[`resources/PhoneNumberMetadata.xml`](http://github.com/google/libphonenumber/blob/master/resources/PhoneNumberMetadata.xml).

## <a href="#validation">Validation and types of numbers</a>

### <a href="#possible_vs_valid">What is the difference between isPossibleNumber and isValidNumber?</a>

To understand the behavior of functions, please refer to the documentation in
the Javadoc/C++ header files. For example, see `isPossibleNumberWithReason` in
[`PhoneNumberUtil`](https://github.com/google/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java).

### <a href="#short_numbers">Why does PhoneNumberUtil return false for valid short numbers?</a>

Short numbers are out of scope of
[`PhoneNumberUtil`](https://github.com/google/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java).
For short numbers, use
[`ShortNumberInfo`](https://github.com/google/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/ShortNumberInfo.java).

### <a href="#what_is_valid">What does it mean for a phone number to be valid?</a>

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

#### <a href="#why_not_valid">But my dialled number connected, so isn't it valid?</a>

Not necessarily.

*   In some countries extra digits at the end are ignored. For example, dialling
    `1800 MICROSOFT` in the US connects to `+1 (800) MIC-ROSO`.
*   During renumbering transitions, e.g. when all numbers are getting an extra
    `9` added to the front, some operators will "fix" old numbers long after
    they're no longer working for the majority.
*   Numbers that are only locally-diallable e.g. a 7-digit number dialled in the
    US are not valid, because without the rest of the number it is impossible
    for the library to canonicalize this.

### <a href="#isvalidnumberforregion">When should I use isValidNumberForRegion?</a>

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

### <a href="#incorrect_formatting">Some "valid" numbers are not formatting correctly.</a>

This could be due to number simplification. In order to keep the size of the XML
files to a reasonable level, it's necessary in some regions (e.g. "DE" or "AT")
to simplify number ranges. This results in a relatively small amount of false
positive numbers (i.e. numbers that should be reported as invalid, but which are
now shown as valid).

This issue here is that (for simplicity) only the number validity information is
simplified; the formatting information in the XML (leading digits) retains its
full accuracy and so doesn't cover these numbers.

Note that while it is probably possible to address this and expand the format
information to cover these numbers as well, it's a rather non-trivial task
(since the leading digits must not be over simplified so as to capture other
valid ranges with different formats). Note that this also applies to attributes
like "national only" or geocoding information.

### <a href="#sms_sending">What types of phone numbers can SMSs be sent to?</a>

SMSs can be sent to `MOBILE` or `FIXED_LINE_OR_MOBILE` numbers. However,
in some countries it is possible to configure other types, such as normal
land-lines, to receive SMSs.

### <a name="#fixed_line_or_mobile">Why did I get `FIXED_LINE_OR_MOBILE` as the type of my phone number?</a>

Some number ranges are explicitly defined as being for fixed-line or mobile
phones. We even represent ranges defined as being "Mostly land-line" in this
way.

### <a name="#portability">What is mobile number portability?</a>

The ability to keep your mobile phone number when changing carriers. To see whether a region supports mobile number portability use [isMobileNumberPortableRegion](https://github.com/google/libphonenumber/blob/58247207903f917839001bc62525a5b48a475b7e/java/libphonenumber/src/com/google/i18n/phonenumbers/PhoneNumberUtil.java#L3524).

### <a name="#carrier_changes">Since it's possible to change the carrier for a phone number, how is the data kept up-to-date?</a>

Not all regions support mobile number portability. For those that don't, we return the carrier when available. For those that do, we return the original carrier for the supplied number.

### <a name="#m2m">What about M2M (machine to machine) numbers?</a>

libphonenumber does not support M2M numbers in general, but only in one exception case where in Netherlands 097X M2M numbers are used as regular mobile phone numbers.

The reason for Libphonenumber to not provide M2M support is related to the lack of standardization and the need for a new Util API (not in radar for the time being).

We don't require that a number to be supported by the library has a human at the other end since we already accept premium rate services and they might go to an automated system instead. But to date we only accept ranges that a human might call or send an SMS to.

M2M numbers would violate this assumption and we'd have to evaluate the consequences for existing APIs and clients if M2M numbers would be considered  valid by the library. Clients of libphonenumber expect mobile and fixed-line numbers to have certain affordances, such as: Reachable for voice calls (and for mobile also SMS) as well as assuming standard cost. This expectation is broken by the lack of M2M standardization today.

Many people use this library for formatting the numbers of their contacts, for allowing people to sign up for services, for working out how to dial someone in a different country, for working out what kind of cost might be associated with a number in an advert, etc. We don't think the lack of M2M support hinders any of those use-case, but we might be wrong.

At the moment Libphonenumber can only support phone numbers that can be dialled by us, not by machines. If you found any humanly diallable M2M numbers that library is not supporting, please raise an [issue here](http://issuetracker.google.com/issues/new?component=192347) with all authoritative and specific documentation such as government sources, which have varied definitions.

### <a href="#why_only_nl_m2m">Why supporting only NL M2M numbers?; Are we sure all 097X numbers are MOBILE?</a>

Official authority has [explicitly stated](https://www.acm.nl/en/publications/information-about-dutch-097-numbers-non-dutch-providers) that this range “should be made accessible, just like other, regular number series from the Netherlands” and that “you can set up a voice and SMS connection towards prefix +31-97 in the same way as you have done already with the +31-6 series.[...] you should enable your systems for voice telephony for the numbers
in the +31-97 series”. This means, however, that there might be cases where the library would categorise a number as a valid mobile number, but in reality, the particular number is used as pure M2M, is not SMS or voice-enabled. There is not much we can do from our side about this, since we always follow official guidelines.

Therefore, clients should be aware that there is possibility of false positives in NL MOBILE category. The library will continue to not support M2M numbers in general.

### <a href="#operator_specific_numbers">What about numbers that are only valid for a set of subscribers?</a>

There are some numbers that only work for the subscribers of certain operators
for special operator-specific services. These differ from carrierSpecific since
they're not shortcodes. We don't support these numbers due to their limited use
scope, few examples (only the [area code 700](https://en.wikipedia.org/wiki/Area_code_700)
in the US), and lack of authoritative evidence.

Until there are more examples with authoritative evidence and a proposal on how
the library should handle these numbers, we won't be able to support these
similar to our prerequisites for supporting M2M.

Please see [this issue](https://issuetracker.google.com/issues/65238929) for more
context, and file a new issue if you're able to provide more information than this.

## <a name="#representation">Representation</a>

### <a name="#max_length">What is the maximum and minimum length of a phone number?</a>

We support parsing and storing numbers from a minimum length of two digits to a
maximum length of 17 digits currently (excluding country calling code). The ITU
standard says the national significant number should not be longer than
fifteen digits, but empirically this has been proven not to be followed by all
countries.

## <a name="#formatting">Formatting</a>

### <a name="#language_specific_format">Can / should we format phone numbers in a language-specific way?</a>

No, phone number formatting is country-specific and language-independent. E.g.
formatting a US number in a French way (e.g. the way a France number is
formatted) for a French user is undefined and wrong.

It is true that in some countries phone numbers are typically written using
native, not ASCII, digits; our phone number library supports parsing these but
doesn't support it at formatting time at the moment.

### <a href="#changes_in_formatting">When does formatting in a country change?</a>

The formatting within a country changes sparingly, but may be announced explicitly
or noted implicitly in a national numbering plan update that introduces a new number
length, number type, or other significant change. This may include the grouping and
punctuation used (e.g. parentheses versus hyphens).

In the event of lack of evidence and/or enforcement by a central government
regulatory or telecommunication authority, we'll stick with the status quo
since the community prefers to bias towards stability and avoid flip-flopping
between formats over time. If the silent majority becomes vocal to support
new formatting with authoritative evidence, then we'll collaborate with
community stakeholders on a transition.

An example of this is the shift from using parentheses to hyphens to separate
area codes within North America. Hyphens may indicate that the area code is
optional in local circumstances, but this is shifting to become mandatory
in areas that have had more area code splits. However, the usage of
parentheses persists and both methods are acceptable.

See [issue #1996](https://github.com/google/libphonenumber/issues/1996)
for some additional discussion.

### <a name="#empty_format_result">Why does formatNumberForMobileDialing return an empty string for my number?</a>

If we don't think we can guarantee that the number is diallable from the user's
mobile phone, we won't return anything. This means that for numbers that we
don't think are internationally diallable, if the user is outside the country
we will return an empty string. Similarly, in Brazil a carrier code is essential
for dialling long-distance domestically. If none has been provided at parsing
time then we will return an empty string. If you get an empty string and are
okay providing a number that may not be diallable, you can call another of our
formatting numbers instead.

## <a name="#metadata">Metadata</a>

### <a name="#metadata_definition">What do we mean by "metadata"?</a>

We use the word "metadata" to refer to all information about phone numbering in
a particular country - what the country code, international and national
dialling prefixes are, what carrier codes are operational, which phone numbers
are possible or valid for a particular country, how to optimally format them,
which prefixes represent a particular geographical area, etc.

### <a name="#metadata_sources">Where do we get information from to determine if a number range is valid?</a>

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

### <a name="#mx_and_ar">Why is this number from Argentina (AR) or Mexico (MX) not identified as the right number type?</a>

Certain countries' mobile and/or fixed line ranges may overlap or too granular,
which may make accurate identification impossible. Eg: Argentina and Mexico.
We tried incorporating such granular data (prefix of 7 digit for a 10 digit
number). However, due to very high maintainance to keep the data fresh and
also leading to significant increase in metadata size, we are supporting
ranges/prefixes only at higher level. More details in [Argentina](https://issuetracker.google.com/issues/78443410) and
[Mexico](https://issuetracker.google.com/issues/74517266) triaged issues.

@Argentina,
When calling a mobile line from a fixed line in Argentina, you need
to dial 15 before the subscriber number, or 9 if you're calling from another
country. Without these additional digits, your call may not connect at all!

We rely on these additional and explicit context such as a mobile prefix to
correctly identify the phone number type (rather than returning [`FIXED_LINE_OR_MOBILE`](#fixed_line_or_mobile)
in ambiguous cases).

Moreover, Argentina has different possible lengths for area codes and
subscriber numbers depending on the city, which further complicate matters (e.g.
Buenos Aires is 11 followed by eight digits, but Río Gallegos is 2966 followed
by six digits).

Despite all the aforementioned complexity, users may not provide their phone
number with all the additional context unless explicitly asked. For instance,
since SMS messages can be sent in Argentina from a mobile phone without a
prefix, the user may not supply the mobile prefix.

We are aware of these issues but fixing them is not trivial. In the meantime, we
recommend the following workarounds to support affected users.

*   If you know an Argentina number is mobile (e.g. if you're doing signups with
    device numbers or will send them an SMS verification code), follow these steps:
    *   For raw input strings:
        *   Parse a raw input string into a `PhoneNumber` and follow the next
            instructions for `PhoneNumber` objects.
    *   For `PhoneNumber` objects:
        *   Check that the library validates a `PhoneNumber` as mobile, by
            calling `getNumberType`;
        *   If not, format it in national format and prepend a `9`
        *   Parse the modified string and if the library validates it as mobile,
            accept the resulting `PhoneNumber` as canonical.
*   Consider prompting for type (mobile or not) in the phone number input UI.

IMPORTANT: Do not add a leading 9 for displaying or formatting the numbers.
Depending on the use case, other tokens may be needed. The library will do the
right thing if the phone number object is as intended.

@Mexico,
Mexico used to have such additional prefixes (1, 02, 045, ...) for dialling
mobile numbers internationally, fixed-line to mobile nationally.. As these
dialling patterns were deprecated, we have continued to maintain mobile and
fixed-line ranges at higher level, returning type as [`FIXED_LINE_OR_MOBILE`](#fixed_line_or_mobile)

### <a href="#mx_legacy_formats">Why Mexico (MX) numbers in older dialling formats are accepted as valid ones?</a>
Though library has stopped supporting below older dialling codes in the canonical
form and formatting results, we are lenient in parsing the number, i.e removing
all older codes.
- 1 -> in E.164 international diallings
- 01, 02, 044 and 045 -> for local/national diallings

This is because we found the older dialling codes supported even after deprecation
period, so we decided to support them for longer time. However, we will stop this as
part of [this issue](https://issuetracker.google.com/issues/205606725). More details there.

### <a name="#unsupported_regions">Why are Bouvet Island (BV), Pitcairn Island (PN), Antarctica (AQ) etc. not supported?</a>

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

### <a name="#indonesia_tollfree">Why are Indonesian toll-free numbers beginning with "00x 803" not supported?</a>

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

## <a name="#misc">Misc</a>

### <a name="#reduced_metadata">What is the metadatalite.js/METADATA_LITE option?</a>

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
file](https://github.com/google/libphonenumber/blob/master/java/build.xml)
itself.

### <a name="#maven">Which versions of the Maven jars should I use?</a>

When possible, use the [latest
version](https://github.com/google/libphonenumber/releases) of
libphonenumber.

For the other Maven artifacts, to find the version corresponding to a given
version of libphonenumber, follow these steps:

*   Go to the versioned GitHub tag, e.g.
    https://github.com/google/libphonenumber/find/v8.3.3
*   Type `pom.xml`. This will surface all the `pom.xml` files as they were
    released at the chosen tag.
*   Find the version you care about in the corresponding `pom.xml` file. Look
    for `<version>` in the top-level `project` element. For example, to find the
    version of the carrier jar corresponding to libphonenumber 8.3.3, open
    `java/carrier/pom.xml` at v8.3.3's search results. This is `1.56`.
*   If you depend on the carrier or geocoder jar, you also need to depend on
        the prefixmapper jar.

### <a name="#android">How do I load libphonenumber resources in my Android app?</a>

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
[#265](https://github.com/google/libphonenumber/issues/265),
[#528](https://github.com/google/libphonenumber/issues/528), and
[#819](https://github.com/google/libphonenumber/issues/819).

#### Optimize loads

You can manage your own resources by supplying your own
[`MetadataLoader`](http://github.com/google/libphonenumber/blob/master/java/libphonenumber/src/com/google/i18n/phonenumbers/MetadataLoader.java)
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

### <a name="#windows">What about Windows?</a>

The libphonenumber team's support of the C++ library on Windows is primarily to
support Chromium's build environment, and we depend on the community to support
other Windows build environments / build chains. We list here some known issues
that would benefit from open-source collaboration. If you can contribute a PR
or review and test out someone else's PR, please chime in on these links, or
email the [discussion
group](https://groups.google.com/group/libphonenumber-discuss):

*   [#1000](https://github.com/google/libphonenumber/issues/1000) to provide
    a Windows DLL.
*   [#1010](https://github.com/google/libphonenumber/issues/1010) to require
    Visual Studio 2015 update 2 or later on Windows
*   PR [#1090](https://github.com/google/libphonenumber/pull/1090) /
    [#824](https://github.com/google/libphonenumber/issues/824) to "Replace
    POSIX directory operations by Boost Filesystem"
*   [#1555](https://github.com/google/libphonenumber/issues/1555) to allow
    Windows to build cpp library with pthreads for multi-threading

### <a name="#remove_example_number">How to remove a specific example number</a>

We supply example numbers as part of the library API. While we aim to have numbers
that are either explicitly allocated by the country as a test number, or look
fictitious (e.g. 1234567) we also need these numbers to validate correctly.
This means we sometimes have numbers that do connect to a real person.

If we by chance have actually listed your real number and would like it removed,
please report this through Google's new [Issue Tracker](http://issuetracker.google.com/issues/new?component=192347).
Only our internal team will have access to your identity (whereas GitHub usernames are public).

### <a name="#long_numbers_in_js">Why can the smallest digits of parsed numbers that are very long be incorrect when parsing in Javascript?</a>

Eg: National number of 900184080594493**87**, ```region: JP``` is parsed as
900184080594493**90**. Reason: When the provided number is more than the max
limit of JavaScript ```Number``` type - 2^53, JS starts rounding the value.
libphonenumber cannot do anything better here. More details mentioned [in this issue](https://issuetracker.google.com/issues/198423548).
