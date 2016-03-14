# Falsehoods Programmers Believe About Phone Numbers

... and tips on how to use libphonenumber.

Given how ubiquitous phone numbers are and how long they've been around, it's
surprising how many false assumptions programmers continue to make about them.

1.  **Phone numbers that are valid today will always be valid. Phone numbers of
    a certain type today (e.g., mobile) will never be reassigned to another
    type.**

    A phone number which connects today may be disconnected tomorrow. A number
    which is free to call today may cost money to call tomorrow. The phone
    company may decide to expand the range of available phone numbers by
    inserting a digit into an existing number.

    **Tip:** Don’t store properties for a phone number such as validity or
    type. Check this information again from the library when you need it.

1.  **A phone number uniquely identifies an individual**

    It wasn't even that long ago that mobile phones didn't exist, and it was
    common for an entire household to share one fixed-line telephone number. In
    some parts of the world, this is still true, and relatives (or even friends)
    share a single phone number.

1.  **An individual has only one phone number**

    Obviously, this isn't necessarily true.

1.  **Phone numbers cannot be re-used**

    Old phone numbers are recycled and get reassigned to other people.

1.  **Each country calling code corresponds to exactly one country**

    The USA, Canada, and several Caribbean islands share the country calling
    code +1. Russia and Kazakhstan share +7. These are not the only examples!

1.  **Each country has only one country calling code**

    As of this present moment (in Mar. 2016), phones in the disputed territory
    and partially recognised state of Kosovo may be reached by dialing the
    country calling code for Serbia (+381), Slovenia (+386), or Monaco (+377),
    depending on where and when one obtained the number.

    **Tip:** Use the phone widget to encourage users to enter their phone number
    in an international format such that we can understand it.

1.  **A phone number is dialable from anywhere**

    Some numbers can only be dialed within the country. Some can only be dialled
    from within a subset of countries, such as the international 00800 numbers.
    Some may be dialable only if the caller is a subscriber to a particular
    telecom company.

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

    ITU says things like "national numbers can not be longer than sixteen
    digits" but valid numbers in Germany have been assigned that are longer than
    this.

1.  **All valid phone numbers belong to a country**

    There are many "country calling codes" issued to non-geographical entities,
    such as "800" or satellite services.

1.  **Phone numbers contain only digits**

    In Israel, certain advertising numbers start with a `*`.

1.  **Phone numbers are always written in ASCII**

    In Egypt, it is common for phone numbers to be written in native digits.

