# Timezone Mapper

The timezone mapper allows one to get a likely timezone for a particular phone
number. For mobile phones which are associated with particular area codes, it
returns the timezone of the area code; it does not track the user's current
location in any way. This could be used to work out whether it is likely to be a
good time to ring a user based on their provided number.

Code Location:
[java/geocoder/src/com/google/i18n/phonenumbers/PhoneNumberToTimeZonesMapper.java](https://github.com/googlei18n/libphonenumber/blob/master/java/geocoder/src/com/google/i18n/phonenumbers/PhoneNumberToTimeZonesMapper.java)

Example usage:

```
PhoneNumberToTimeZonesMapper timeZonesMapper = PhoneNumberToTimeZonesMapper.getInstance();

List<String> timezones = timeZonesMapper.getTimeZonesForNumber(phoneNumber);
```

## Contributing to the timezone metadata

The timezone metadata is auto-generated with few exceptions, so we cannot except
pull requests. If we have an error please file an issue and we can see if we
can make a generic fix.

If making fixes in your own fork while you wait for this fix, build the metadata
by running this command from the root of the repository:

```
ant -f java/build.xml build-timezones-data
```
