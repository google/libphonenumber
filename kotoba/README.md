# Kotoba v1 for libphonenumber

First-class sibling tree to `cpp/`, `java/`, and `javascript/`. This directory
is a Kotoba check surface for the fork, not a language binding over the Java,
C++, or JavaScript libraries.

Public operator: awai.network. Sales: Ryo Awai.

License: Apache-2.0 (same as this repository).

## Honest scope

v1 checks whether a short hardcoded fixture is a **well-formed E.164-looking**
string: a plus prefix plus a digit length.

- First byte must be `+` (ASCII 43).
- Every following byte must be an ASCII digit `0`–`9`.
- Digit count must be in `2..15`.
  - `15` is the ITU E.164 maximum for the complete international number.
  - `2` is this repository's `PhoneNumberUtil.MIN_LENGTH_FOR_NSN`, applied
    here to the complete digit string, not to a region national number.

Two accept fixtures and two reject fixtures are taken from this repository's
own testdata (`US_NUMBER` / `GB_NUMBER` in
`java/libphonenumber/test/com/google/i18n/phonenumbers/PhoneNumberUtilTest.java`):

| fixture | bytes | expected |
|---|---|---|
| `fixtures/us-e164.txt` | `+16502530000` | accept (E.164 of US_NUMBER) |
| `fixtures/gb-e164.txt` | `+442070313000` | accept (E.164 of GB_NUMBER) |
| `fixtures/us-international.txt` | `+1 650 253 0000` | reject (INTERNATIONAL spaces) |
| `fixtures/us-nsn.txt` | `6502530000` | reject (national number, no plus) |

This is **not**:

- a drop-in replacement for the Java, C++, or JavaScript libraries
- full region metadata / carrier / geocoding / timezone lookup
- `isValidNumber`, `isPossibleNumber` against country ranges
- parse, format, AsYouType, matcher, or short-number support
- a copy of `resources/phonenumber.proto` or the metadata proto set
- an FFI or JNI/C++ driver

A full libphonenumber port will not fit on wasm32 i64-v1. That dialect has no
IEEE floats, no admitted byte-builder, and no host imports in the
host-independent profile. Shrinking to a plus-prefix digit-length check of a
few project fixtures is the smallest codec/header/fixture that still proves a
real accept and a real reject. The shrink is documented here, not stubbed.

## Module

`phonenumber.kotoba` is one compilation unit (`phonenumber.v1`).

| export | meaning |
|---|---|
| `e164-looking` | `1` if fixture `id` is E.164-looking, else `0` |
| `accept-us` / `accept-gb` | `1` if the matching accept fixture is admitted |
| `reject-spaces` / `reject-nsn` | `1` if the matching reject fixture is refused |
| `main` | `1` if both accepts and both rejects hold |

## Build (kotoba CLI v0.7.2)

Language authority: [kotoba-lang/kotoba-lang](https://github.com/kotoba-lang/kotoba-lang).
CLI: [kotoba-lang/kotoba](https://github.com/kotoba-lang/kotoba) tag **v0.7.2**.

```sh
kotoba compile kotoba/phonenumber.kotoba --target wasm --output phonenumber.wasm --json
```

Accept `kotoba.cli/ok?` true, `kotoba.cli/code` `emitted`,
`value-profile` `i64-v1`. The artifact is host-independent wasm32
(`wasm32-kotoba-v1`, `value-abi` `direct-v1`): no `kotoba:typed` imports, no
libphonenumber native code.

`kotoba compile --target wasm --run` is not used. The i64-v1 guest does not
match the kototama/chicory runner on v0.7.2. Fixture execution is
`--target web --run` (`js-kotoba-v1`), which still reports `value-profile`
`i64-v1`.

```sh
kotoba/checks.sh
```

Install the CLI from the v0.7.2 release tarball or
`brew tap kotoba-lang/kotoba && brew trust kotoba-lang/kotoba && brew install kotoba`.
