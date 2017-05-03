# Timezone Mapper

The timezone mapper allows one to get a likely timezone for a particular phone
number. The timezone returned is the canonical ID from [CLDR](
http://www.unicode.org/cldr/charts/latest/supplemental/zone_tzid.html), not a
localised name (or any other identifier). For mobile phones which are associated
with particular area codes, it returns the timezone of the area code; it does
not track the user's current location in any way. This could be used to work out
whether it is likely to be a good time to ring a user based on their provided
number.

Code Location:
[java/geocoder/src/com/google/i18n/phonenumbers/PhoneNumberToTimeZonesMapper.java](https://github.com/googlei18n/libphonenumber/blob/master/java/geocoder/src/com/google/i18n/phonenumbers/PhoneNumberToTimeZonesMapper.java)

Example usage:

```
PhoneNumberToTimeZonesMapper timeZonesMapper = PhoneNumberToTimeZonesMapper.getInstance();

List<String> timezones = timeZonesMapper.getTimeZonesForNumber(phoneNumber);
```

## Contributing to the timezone metadata

The timezone metadata is auto-generated with few exceptions, so we cannot accept
pull requests. If we have an error please file an issue and we'll see if we can
make a generic fix.

If making fixes in your own fork while you wait for this, build the metadata by
running this command from the root of the repository (assuming you have `ant`
installed):

```
ant -f java/build.xml build-timezones-data
```

Note that, due to our using CLDR timezone IDs which are stable, we do not change
the ID for an existing timezone when the name of a region or subdivision
changes. The library returns the *ID*, which you may use to get the localised
name from CLDR.
