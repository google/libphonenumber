# Falsehoods Programmers Believe About Phone Numbers

... and tips on how to use libphonenumber.

Given how ubiquitous phone numbers are and how long they've been around, it's
surprising how many false assumptions programmers continue to make about them.

1.  **An individual has a phone number**

    Some people do not own phones, or do not wish to provide you with their
    telephone number when asked. Do not require a user to provide a phone number
    unless it is essential, and whenever possible try to provide a fallback
    to accommodate these users.

1. **You can make a call to any phone number**
  
    Some devices such as EFTPOS terminals, fax machines and mobile internet
    dongles may not support receiving calls. In addition, some people can not
    use their phones for phone calls. This may be permanent (such as a hearing
    disability), temporary (temporary hearing loss) or situational (when the
    user is in a noisy environment).
  
    **Tip:** Make how you use phone numbers apparent to the user and give them
    the option to set their preference. Ensure that you look up this preference
    before attempting to communicate with the user.

1.  **An individual has only one phone number**

    Obviously, this isn't necessarily true.

1.  **A phone number uniquely identifies an individual**

    It wasn't even that long ago that mobile phones didn't exist, and it was
    common for an entire household to share one fixed-line telephone number. In
    some parts of the world, this is still true, and relatives (or even friends)
    share a single phone number. Many phone services (especially for businesses)
    allow multiple inbound calls to or outbound calls from the same phone
    number.

1.  **Phone numbers cannot be re-used**

    Old phone numbers are recycled and get reassigned to other people.

1.  **Phone numbers that are valid today will always be valid. Phone numbers of
    a certain type today (e.g., mobile) will never be reassigned to another
    type.**

    A phone number which connects today may be disconnected tomorrow. A number
    which is free to call today may cost money to call tomorrow. The phone
    company may decide to expand the range of available phone numbers by
    inserting a digit into an existing number.

    **Tip:** Don’t store properties for a phone number such as validity or
    type. Check this information again from the library when you need it.

1.  **Each country calling code corresponds to exactly one country**

    The USA, Canada, and several Caribbean islands share the country calling
    code +1. Russia and Kazakhstan share +7. These are not the only examples!

1.  **Each country has only one country calling code**

    As of this present moment (in Mar. 2016), phones in the disputed territory
    and partially recognised state of Kosovo may be reached by dialing the
    country calling code for Serbia (+381), Slovenia (+386), or Monaco (+377),
    depending on where and when one obtained the number.

    **Tip:** Use a phone widget to encourage users to enter their phone number
    in an international format such that it can be unambiguously understood.

1.  **A phone number is dialable from anywhere**

    Some numbers can only be dialed within the country. Some can only be dialled
    from within a subset of countries, such as the +800 [Universal
    International Freephone Numbers](https://en.wikipedia.org/wiki/Toll-free_telephone_number#Universal_International_Freephone_Service).
    Some may be dialable only if the caller is a subscriber to a particular
    telecom company.

1.  **You can send a text message to any phone number**

    A lot of people still only have a fixed-line telephone, which typically cannot
    send or receive text messages.

    **Tip:** Whenever possible, try to accommodate these users by calling them
    with a voice message or voice prompts. Alternatively, if your service
    requires sending text messages, verify that the number can receive text
    messages after the user provides it.

1.  **Only mobile phones can receive text messages**

    Some service providers support sending and receiving text messages to fixed-line
    numbers. There are also online services like Skype that can send and receive
    text messages.

1.  **There are only two ways to dial a phone number: domestically and from
    overseas**

    Some numbers may need different prefixes depending on: the carrier you are
    using; what device you are dialling from/to; whether you are inside or
    outside a particular geographical region.

    Examples:
    *   In Brazil, to dial numbers internally but across a certain geographical
        boundary, a carrier code must be explicitly dialed to say which carrier
        you will use to pay for the call.
    *   In Nepal, the leading zero in national format is omitted depending on
        whether the originating phone is mobile or fixed-line.
    *   In New Zealand, you need to dial the area-code (e.g. 03) even if the
        number is within the same area-code region as you are, unless it is
        "close" (something approximating city/district boundaries), in which
        case it shouldn’t be dialled.

    **Tip:** Use formatForMobileDialling to get the number a user should
    actually dial on their mobile phone.

1.  **To make a number dialable, you only need to change the prefix**

    In Argentina, to dial a mobile number domestically, the digits "15" need to
    be inserted *after* the area code but *before* the local number, and the "9"
    after the country code (54) needs to be removed. This transforms +54 9 2982
    123456 into 02982 15 123456.

1.  **No prefix of a valid phone number can be a valid phone number**

    In some countries, it's possible to connect to a different endpoint by
    dialing more digits after a number. So "12345678" may not reach the same
    person as dialing "123456".

1.  **An invalid number will not reach an endpoint**

    In some countries, or on some phones, extra digits are thrown away. Hence,
    1-800-MICROSOFT is an invalid number - but it still connects to Microsoft,
    since any later digits are ignored. Numbers such as "911" can be reached by
    dialling "911 123" in some countries: but not in others.

    In other countries, invalid numbers may be "fixed" by a carrier, e.g.,
    adding a mobile token if they know it is a mobile number, such that it
    connects.

1.  **All valid phone numbers follow the ITU specifications**

    ITU-T specifies that a phone number cannot be longer than fifteen digits,
    with one to three digits reserved for the country calling code, but valid
    numbers in Germany have been assigned that are longer than this.

1.  **All valid phone numbers belong to a country**

    There are many "country calling codes" issued to non-geographical entities,
    such as satellite services, and the "800" code for [Universal International
    Freephone Numbers](https://en.wikipedia.org/wiki/Toll-free_telephone_number#Universal_International_Freephone_Service).

1.  **Phone numbers contain only digits**

    In Israel, certain advertising numbers start with a `*`. In New Zealand,
    non-urgent traffic incidents can be reported by calling `*555` from a mobile
    phone. Alpha characters may also be used in phone numbers, such as in
    `1-800-Flowers`.

1.  **Phone numbers are always written in ASCII**

    In Egypt, it is common for phone numbers to be written in native digits.

1.  **Phone numbers have only one prefix (area code or national destination
    code) at a given time**

    In the mid-90s in Iceland, phone numbers changed from 5 and 6 digits to 7
    digits. The old system had regional prefixes, but the new one doesn't.
    During the transition period, phone numbers could be reached by the old area
    code or the new 7 digit number (a different prefix).  For example, a
    Reykjavik phone number could be dialed with `nnnnn` and `55nnnnn` inside the
    region, and `91-nnnnn` and `55nnnnn` from outside.

    **Tip:** During transition periods, make sure that both forms of a number
    are supported for look-up by phone number. If sending messages/dialling
    affected numbers, both the new and old prefix may need to be tried.
    Depending on the type of transition, you may need to contact affected
    customers and ask them to update their number.

1.  **A leading zero in numbers formatted for domestic usage can always be
    discarded when dialing from abroad**

    In some countries, people write the national prefix in brackets (typically
    `(0)`) after the country calling code to indicate that it should be
    discarded when dialing internationally. In Italy, since 1998, the prefix was
    "fixed" to the phone numbers, so that `(01) 2345` became `012345` and should
    be dialed (internationally) as `+39012345` (including the leading zero).

    **Tip:** Use the library to parse and format numbers to handle this
    correctly for every country.

1.  **The country or area code of a phone number indicates the user's location,
    place of residence, time-zone, or preferred language**

    There are many reasons for someone to have a phone number issued in a state
    or region other than where they reside or hold citizenship.

    These include, but are not limited to:
    *   *Moving within a country*: In countries with phone number portability,
        you may retain your number when moving, even in some cases if it is a
        fixed-line number and even if it has an area code (see
        *[xkcd](https://xkcd.com/1129/)* for a US example.)
    *   *Moving to another country*: Some people keep their mobile phones when
        they move to another country.
    *   *Geopolitical turmoil*: Sometimes countries change their borders, cease
        to exist, or come into existence.
    *   *Business, family, and friends*: A business may have many customers in a
        neighbouring country, or a person may have many family and friends
        there.
    *   *Wanting cheaper rates*: VoIP is often cheaper than regular calls.
        People traveling around Europe may get a SIM card from one country and
        have a roaming plan.

    Note that geographical area codes are assigned in some countries to mobile phones.

    **Tip:** Programmers should not disallow users from signing up with a phone
    number merely because it belongs to another country. Care should be taken
    when calling methods such as `isValidNumberForRegion` that this is what's
    actually intended. If you have a phone input widget, make sure you allow
    numbers to be entered in international format (i.e., allow the `+` sign) so
    people can sign up with an international contact number.

1.  **The plus sign in front of phone numbers in international format is
    optional, or can always be replaced by `00`**

    The plus sign is part of the [E.164
    format](https://en.wikipedia.org/wiki/E.164) for international telephone
    numbers. It can be replaced with the [international call
    prefix](https://en.wikipedia.org/wiki/List_of_international_call_prefixes)
    when dialing internationally. Note that while `00` is a common international
    call prefix, this actually varies by country.

    In North America, which has a country calling code of `+1`, it is a common
    error to drop the `+` in front of the number and write it like
    `1-555-123-4567`. This is technically incorrect. To call this number from
    Japan, where the international call prefix is `010`, one may dial either `+1
    555 123 4567` or `010 1 555 123 4567`.

1.  **Users will only store phone numbers in your product's phone number fields**

    Some users use their contact lists to store things like birthdays or other
    information. Unless a piece of user-supplied data has actually been verified
    to be a phone number, it should be stored as-is as entered by the user.

1.  **Phone numbers are numbers**

    Never try to store phone numbers as an int or any other kind of numeric
    data type. You can't do arithmetic on them, and while 007, 07 and 7 are the
    same number they are not necessarily the same phone number - in some
    countries a leading 0 is significant and forms part of the number itself
    (see *A leading zero in numbers formatted for domestic usage can always be
    discarded when dialing from abroad*). Moreover, a phone number may contain
    other diallable characters (see *Phone numbers contain only digits*) or an
    extension portion, dialled after waiting for a tone.

    **Tip:** Use the library to parse and format numbers to handle this
    correctly for every country.

1.  **Phone numbering plans published by governments or telecoms represent
    reality**

    National numbering plans, such as those administered by the
    [ITU](http://www.itu.int/oth/T0202.aspx?parent=T0202), represent the
    intentions of the government or telecom. These may be published before,
    during, or after the actual implementation of numbering plan changes in the
    real world. The actual date on which a phone number range becomes active may
    not always adhere to official announcements.

1. **Nobody voice calls M2M sim numbers**

    M2M sim cards are for machine-to-machine communication and are up to 
    15 characters long. Typically they are used for data and SMS messages, 
    however there are cases in which someone may voice-call an M2M sim. 
    One example is for in-home & roaming telecare/medical alarms,
    in the event of an alarm activation then automated voice messages can be
    played on the device via telephony APIs, or a human may call the device
    directly (potentially also via a telephony API).
