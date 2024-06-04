// Copyright (C) 2009 The Libphonenumber Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Utility for international phone numbers.

#ifndef I18N_PHONENUMBERS_PHONENUMBERUTIL_H_
#define I18N_PHONENUMBERS_PHONENUMBERUTIL_H_

#include <stddef.h>
#include <list>
#include <map>
#include <set>
#include <string>
#include <utility>
#include <vector>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/base/memory/singleton.h"
#include "phonenumbers/phonenumber.pb.h"

#include "absl/container/node_hash_set.h"
#include "absl/container/node_hash_map.h"

class TelephoneNumber;

namespace i18n {
namespace phonenumbers {

using google::protobuf::RepeatedPtrField;
using std::string;

class AsYouTypeFormatter;
class Logger;
class MatcherApi;
class NumberFormat;
class PhoneMetadata;
class PhoneNumberDesc;
class PhoneNumberRegExpsAndMappings;
class RegExp;

// NOTE: A lot of methods in this class require Region Code strings. These must
// be provided using CLDR two-letter region-code format. These should be in
// upper-case. The list of the codes can be found here:
// http://www.unicode.org/cldr/charts/30/supplemental/territory_information.html

class PhoneNumberUtil : public Singleton<PhoneNumberUtil> {
 private:
  friend class AsYouTypeFormatter;
  friend class PhoneNumberMatcher;
  friend class PhoneNumberMatcherRegExps;
  friend class PhoneNumberMatcherTest;
  friend class PhoneNumberRegExpsAndMappings;
  friend class PhoneNumberUtilTest;
  friend class ShortNumberInfo;
  friend class ShortNumberInfoTest;
  friend class Singleton<PhoneNumberUtil>;

 public:
  // This type is neither copyable nor movable.
  PhoneNumberUtil(const PhoneNumberUtil&) = delete;
  PhoneNumberUtil& operator=(const PhoneNumberUtil&) = delete;

  ~PhoneNumberUtil();
  static const char kRegionCodeForNonGeoEntity[];

  // INTERNATIONAL and NATIONAL formats are consistent with the definition
  // in ITU-T Recommendation E.123. However we follow local conventions such as
  // using '-' instead of whitespace as separators. For example, the number of
  // the Google Switzerland office will be written as "+41 44 668 1800" in
  // INTERNATIONAL format, and as "044 668 1800" in NATIONAL format. E164
  // format is as per INTERNATIONAL format but with no formatting applied e.g.
  // "+41446681800". RFC3966 is as per INTERNATIONAL format, but with all spaces
  // and other separating symbols replaced with a hyphen, and with any phone
  // number extension appended with ";ext=". It also will have a prefix of
  // "tel:" added, e.g. "tel:+41-44-668-1800".
  enum PhoneNumberFormat {
    E164,
    INTERNATIONAL,
    NATIONAL,
    RFC3966
  };

  static const PhoneNumberFormat kMaxNumberFormat = RFC3966;

  // Type of phone numbers.
  enum PhoneNumberType {
    FIXED_LINE,
    MOBILE,
    // In some regions (e.g. the USA), it is impossible to distinguish between
    // fixed-line and mobile numbers by looking at the phone number itself.
    FIXED_LINE_OR_MOBILE,
    // Freephone lines
    TOLL_FREE,
    PREMIUM_RATE,
    // The cost of this call is shared between the caller and the recipient, and
    // is hence typically less than PREMIUM_RATE calls. See
    // http://en.wikipedia.org/wiki/Shared_Cost_Service for more information.
    SHARED_COST,
    // Voice over IP numbers. This includes TSoIP (Telephony Service over IP).
    VOIP,
    // A personal number is associated with a particular person, and may be
    // routed to either a MOBILE or FIXED_LINE number. Some more information can
    // be found here: http://en.wikipedia.org/wiki/Personal_Numbers
    PERSONAL_NUMBER,
    PAGER,
    // Used for "Universal Access Numbers" or "Company Numbers". They may be
    // further routed to specific offices, but allow one number to be used for a
    // company.
    UAN,
    // Used for "Voice Mail Access Numbers".
    VOICEMAIL,
    // A phone number is of type UNKNOWN when it does not fit any of the known
    // patterns for a specific region.
    UNKNOWN
  };

  static const PhoneNumberType kMaxNumberType = UNKNOWN;

  // Types of phone number matches. See detailed description beside the
  // IsNumberMatch() method.
  enum MatchType {
    INVALID_NUMBER,  // NOT_A_NUMBER in the java version.
    NO_MATCH,
    SHORT_NSN_MATCH,
    NSN_MATCH,
    EXACT_MATCH,
  };

  static const MatchType kMaxMatchType = EXACT_MATCH;

  enum ErrorType {
    NO_PARSING_ERROR,
    INVALID_COUNTRY_CODE_ERROR,  // INVALID_COUNTRY_CODE in the java version.
    NOT_A_NUMBER,
    TOO_SHORT_AFTER_IDD,
    TOO_SHORT_NSN,
    TOO_LONG_NSN,  // TOO_LONG in the java version.
  };

  static const ErrorType kMaxErrorType = TOO_LONG_NSN;

  // Possible outcomes when testing if a PhoneNumber is possible.
  enum ValidationResult {
    // The number length matches that of valid numbers for this region.
    IS_POSSIBLE,
    // The number length matches that of local numbers for this region only
    // (i.e. numbers that may be able to be dialled within an area, but do not
    // have all the information to be dialled from anywhere inside or outside
    // the country).
    IS_POSSIBLE_LOCAL_ONLY,
    // The number has an invalid country calling code.
    INVALID_COUNTRY_CODE,
    // The number is shorter than all valid numbers for this region.
    TOO_SHORT,
    // The number is longer than the shortest valid numbers for this region,
    // shorter than the longest valid numbers for this region, and does not
    // itself have a number length that matches valid numbers for this region.
    // This can also be returned in the case where
    // IsPossibleNumberForTypeWithReason was called, and there are no numbers of
    // this type at all for this region.
    INVALID_LENGTH,
    // The number is longer than all valid numbers for this region.
    TOO_LONG,
  };

  static const ValidationResult kMaxValidationResult = TOO_LONG;

  // Returns all regions the library has metadata for.
  // @returns an unordered set of the two-letter region codes for every
  // geographical region the library supports
  void GetSupportedRegions(
      std::set<string>* regions) const;

  // Returns all global network calling codes the library has metadata for.
  // @returns an unordered set of the country calling codes for every
  // non-geographical entity the library supports
  void GetSupportedGlobalNetworkCallingCodes(
      std::set<int>* calling_codes) const;

  // Returns all country calling codes the library has metadata for, covering
  // both non-geographical entities (global network calling codes) and those
  // used for geographical entities. This could be used to populate a drop-down
  // box of country calling codes for a phone-number widget, for instance.
  void GetSupportedCallingCodes(std::set<int>* calling_codes) const;

  // Returns the types for a given region which the library has metadata for.
  // Will not include FIXED_LINE_OR_MOBILE (if numbers for this non-geographical
  // entity could be classified as FIXED_LINE_OR_MOBILE, both FIXED_LINE and
  // MOBILE would be present) and UNKNOWN.
  //
  // No types will be returned for invalid or unknown region codes.
  void GetSupportedTypesForRegion(
      const string& region_code,
      std::set<PhoneNumberType>* types) const;

  // Returns the types for a country-code belonging to a non-geographical entity
  // which the library has metadata for. Will not include FIXED_LINE_OR_MOBILE
  // (instead both FIXED_LINE and FIXED_LINE_OR_MOBILE (if numbers for this
  // non-geographical entity could be classified as FIXED_LINE_OR_MOBILE, both
  // FIXED_LINE and MOBILE would be present) and UNKNOWN.
  //
  // No types will be returned for country calling codes that do not map to a
  // known non-geographical entity.
  void GetSupportedTypesForNonGeoEntity(
      int country_calling_code,
      std::set<PhoneNumberType>* types) const;

  // Gets a PhoneNumberUtil instance to carry out international phone number
  // formatting, parsing, or validation. The instance is loaded with phone
  // number metadata for a number of most commonly used regions, as specified by
  // DEFAULT_REGIONS_.
  //
  // The PhoneNumberUtil is implemented as a singleton. Therefore, calling
  // GetInstance multiple times will only result in one instance being created.
  static PhoneNumberUtil* GetInstance();

  // Returns true if the number is a valid vanity (alpha) number such as 800
  // MICROSOFT. A valid vanity number will start with at least 3 digits and will
  // have three or more alpha characters. This does not do region-specific
  // checks - to work out if this number is actually valid for a region, it
  // should be parsed and methods such as IsPossibleNumberWithReason or
  // IsValidNumber should be used.
  bool IsAlphaNumber(const string& number) const;

  // Converts all alpha characters in a number to their respective digits on
  // a keypad, but retains existing formatting.
  void ConvertAlphaCharactersInNumber(string* number) const;

  // Normalizes a string of characters representing a phone number. This
  // converts wide-ascii and arabic-indic numerals to European numerals, and
  // strips punctuation and alpha characters.
  void NormalizeDigitsOnly(string* number) const;

  // Normalizes a string of characters representing a phone number. This strips
  // all characters which are not diallable on a mobile phone keypad (including
  // all non-ASCII digits).
  void NormalizeDiallableCharsOnly(string* number) const;

  // Gets the national significant number of a phone number. Note a national
  // significant number doesn't contain a national prefix or any formatting.
  void GetNationalSignificantNumber(const PhoneNumber& number,
                                    string* national_significant_num) const;

  // Gets the length of the geographical area code from the PhoneNumber object
  // passed in, so that clients could use it to split a national significant
  // number into geographical area code and subscriber number. It works in such
  // a way that the resultant subscriber number should be diallable, at least on
  // some devices. An example of how this could be used:
  //
  // const PhoneNumberUtil& phone_util(*PhoneNumberUtil::GetInstance());
  // PhoneNumber number;
  // phone_util.Parse("16502530000", "US", &number);
  // string national_significant_number;
  // phone_util.GetNationalSignificantNumber(number,
  //                                         &national_significant_number);
  // string area_code;
  // string subscriber_number;
  //
  // int area_code_length = phone_util.GetLengthOfGeographicalAreaCode(number);
  // if (area_code_length > 0) {
  //   area_code = national_significant_number.substr(0, area_code_length);
  //   subscriber_number = national_significant_number.substr(
  //       area_code_length, string::npos);
  // } else {
  //   area_code = "";
  //   subscriber_number = national_significant_number;
  // }
  //
  // N.B.: area code is a very ambiguous concept, so the authors generally
  // recommend against using it for most purposes, but recommend using the
  // more general national_number instead. Read the following carefully before
  // deciding to use this method:
  //
  //  - geographical area codes change over time, and this method honors those
  //    changes; therefore, it doesn't guarantee the stability of the result it
  //    produces.
  //  - subscriber numbers may not be diallable from all devices (notably mobile
  //    devices, which typically requires the full national_number to be dialled
  //    in most regions).
  //  - most non-geographical numbers have no area codes, including numbers
  //    from non-geographical entities.
  //  - some geographical numbers have no area codes.
  int GetLengthOfGeographicalAreaCode(const PhoneNumber& number) const;

  // Gets the length of the national destination code (NDC) from the PhoneNumber
  // object passed in, so that clients could use it to split a national
  // significant number into NDC and subscriber number. The NDC of a phone
  // number is normally the first group of digit(s) right after the country
  // calling code when the number is formatted in the international format, if
  // there is a subscriber number part that follows.
  //
  // N.B.: similar to an area code, not all numbers have an NDC!
  //
  // An example of how this could be used:
  //
  // const PhoneNumberUtil& phone_util(*PhoneNumberUtil::GetInstance());
  // PhoneNumber number;
  // phone_util.Parse("16502530000", "US", &number);
  // string national_significant_number;
  // phone_util.GetNationalSignificantNumber(number,
  //                                         &national_significant_number);
  // string national_destination_code;
  // string subscriber_number;
  //
  // int national_destination_code_length =
  //     phone_util.GetLengthOfNationalDestinationCode(number);
  // if (national_destination_code_length > 0) {
  //   national_destination_code = national_significant_number.substr(
  //       0, national_destination_code_length);
  //   subscriber_number = national_significant_number.substr(
  //       national_destination_code_length, string::npos);
  // } else {
  //   national_destination_code = "";
  //   subscriber_number = national_significant_number;
  // }
  //
  // Refer to the unittests to see the difference between this function and
  // GetLengthOfGeographicalAreaCode().
  int GetLengthOfNationalDestinationCode(const PhoneNumber& number) const;

  // Returns the mobile token for the provided country calling code if it has
  // one, otherwise returns an empty string. A mobile token is a number inserted
  // before the area code when dialing a mobile number from that country from
  // abroad.
  void GetCountryMobileToken(int country_calling_code,
                             string* mobile_token) const;

  // Formats a phone number in the specified format using default rules. Note
  // that this does not promise to produce a phone number that the user can
  // dial from where they are - although we do format in either NATIONAL or
  // INTERNATIONAL format depending on what the client asks for, we do not
  // currently support a more abbreviated format, such as for users in the
  // same area who could potentially dial the number without area code.
  void Format(const PhoneNumber& number,
              PhoneNumberFormat number_format,
              string* formatted_number) const;

  // Formats a phone number in the specified format using client-defined
  // formatting rules.
  void FormatByPattern(
      const PhoneNumber& number,
      PhoneNumberFormat number_format,
      const RepeatedPtrField<NumberFormat>& user_defined_formats,
      string* formatted_number) const;

  // Formats a phone number in national format for dialing using the carrier as
  // specified in the carrier_code. The carrier_code will always be used
  // regardless of whether the phone number already has a preferred domestic
  // carrier code stored. If carrier_code contains an empty string, return the
  // number in national format without any carrier code.
  void FormatNationalNumberWithCarrierCode(const PhoneNumber& number,
                                           const string& carrier_code,
                                           string* formatted_number) const;

  // Formats a phone number in national format for dialing using the carrier as
  // specified in the preferred_domestic_carrier_code field of the PhoneNumber
  // object passed in. If that is missing, use the fallback_carrier_code passed
  // in instead. If there is no preferred_domestic_carrier_code, and the
  // fallback_carrier_code contains an empty string, return the number in
  // national format without any carrier code.
  //
  // Use FormatNationalNumberWithCarrierCode instead if the carrier code passed
  // in should take precedence over the number's preferred_domestic_carrier_code
  // when formatting.
  void FormatNationalNumberWithPreferredCarrierCode(
      const PhoneNumber& number,
      const string& fallback_carrier_code,
      string* formatted_number) const;

  // Returns a number formatted in such a way that it can be dialed from a
  // mobile phone in a specific region. If the number cannot be reached from
  // the region (e.g. some countries block toll-free numbers from being called
  // outside of the country), the method returns an empty string.
  void FormatNumberForMobileDialing(
      const PhoneNumber& number,
      const string& region_calling_from,
      bool with_formatting,
      string* formatted_number) const;

  // Formats a phone number for out-of-country dialing purposes.
  //
  // Note this function takes care of the case for calling inside of NANPA
  // and between Russia and Kazakhstan (who share the same country calling
  // code). In those cases, no international prefix is used. For regions which
  // have multiple international prefixes, the number in its INTERNATIONAL
  // format will be returned instead.
  void FormatOutOfCountryCallingNumber(
      const PhoneNumber& number,
      const string& calling_from,
      string* formatted_number) const;

  // Formats a phone number using the original phone number format (e.g.
  // INTERNATIONAL or NATIONAL) that the number is parsed from, provided that
  // the number has been parsed with ParseAndKeepRawInput. Otherwise the number
  // will be formatted in NATIONAL format. The original format is embedded in
  // the country_code_source field of the PhoneNumber object passed in, which is
  // only set when parsing keeps the raw input. When we don't have a formatting
  // pattern for the number, the method falls back to returning the raw input.
  // When the number is an invalid number, the method returns the raw input when
  // it is available.
  void FormatInOriginalFormat(const PhoneNumber& number,
                              const string& region_calling_from,
                              string* formatted_number) const;

  // Formats a phone number for out-of-country dialing purposes.
  //
  // Note that in this version, if the number was entered originally using alpha
  // characters and this version of the number is stored in raw_input, this
  // representation of the number will be used rather than the digit
  // representation. Grouping information, as specified by characters such as
  // "-" and " ", will be retained.
  //
  // Caveats:
  // 1) This will not produce good results if the country calling code is both
  // present in the raw input _and_ is the start of the national number. This
  // is not a problem in the regions which typically use alpha numbers.
  // 2) This will also not produce good results if the raw input has any
  // grouping information within the first three digits of the national number,
  // and if the function needs to strip preceding digits/words in the raw input
  // before these digits. Normally people group the first three digits together
  // so this is not a huge problem - and will be fixed if it proves to be so.
  void FormatOutOfCountryKeepingAlphaChars(
      const PhoneNumber& number,
      const string& calling_from,
      string* formatted_number) const;

  // Attempts to extract a valid number from a phone number that is too long to
  // be valid, and resets the PhoneNumber object passed in to that valid
  // version. If no valid number could be extracted, the PhoneNumber object
  // passed in will not be modified. It returns true if a valid phone number can
  // be successfully extracted.
  bool TruncateTooLongNumber(PhoneNumber* number) const;

  // Gets the type of a valid phone number, or UNKNOWN if it is invalid.
  PhoneNumberType GetNumberType(const PhoneNumber& number) const;

  // Tests whether a phone number matches a valid pattern. Note this doesn't
  // verify the number is actually in use, which is impossible to tell by just
  // looking at a number itself.
  // It only verifies whether the parsed, canonicalised number is valid: not
  // whether a particular series of digits entered by the user is diallable from
  // the region provided when parsing. For example, the number +41 (0) 78 927
  // 2696 can be parsed into a number with country code "41" and national
  // significant number "789272696". This is valid, while the original string
  // is not diallable.
  bool IsValidNumber(const PhoneNumber& number) const;

  // Tests whether a phone number is valid for a certain region. Note this
  // doesn't verify the number is actually in use, which is impossible to tell
  // by just looking at a number itself. If the country calling code is not the
  // same as the country calling code for the region, this immediately exits
  // with false. After this, the specific number pattern rules for the region
  // are examined.
  // This is useful for determining for example whether a particular number is
  // valid for Canada, rather than just a valid NANPA number.
  // Warning: In most cases, you want to use IsValidNumber instead. For
  // example, this method will mark numbers from British Crown dependencies
  // such as the Isle of Man as invalid for the region "GB" (United Kingdom),
  // since it has its own region code, "IM", which may be undesirable.
  bool IsValidNumberForRegion(
      const PhoneNumber& number,
      const string& region_code) const;

  // Returns the region where a phone number is from. This could be used for
  // geocoding at the region level. Only guarantees correct results for valid,
  // full numbers (not short-codes, or invalid numbers).
  void GetRegionCodeForNumber(const PhoneNumber& number,
                              string* region_code) const;

  // Returns the country calling code for a specific region. For example,
  // this would be 1 for the United States, and 64 for New Zealand.
  int GetCountryCodeForRegion(const string& region_code) const;

  // Returns the region code that matches the specific country code. Note that
  // it is possible that several regions share the same country calling code
  // (e.g. US and Canada), and in that case, only one of the regions (normally
  // the one with the largest population) is returned. If the
  // countryCallingCode entered is valid but doesn't match a specific region
  // (such as in the case of non-geographical calling codes like 800) the
  // RegionCode 001 will be returned (corresponding to the value for World in
  // the UN M.49 schema).
  void GetRegionCodeForCountryCode(int country_code, string* region_code) const;

  // Populates a list with the region codes that match the specific country
  // calling code. For non-geographical country calling codes, the region code
  // 001 is returned. Also, in the case of no region code being found, the list
  // is left unchanged.
  void GetRegionCodesForCountryCallingCode(
      int country_calling_code,
      std::list<string>* region_codes) const;

  // Checks if this is a region under the North American Numbering Plan
  // Administration (NANPA).
  bool IsNANPACountry(const string& region_code) const;

  // Returns the national dialling prefix for a specific region. For example,
  // this would be 1 for the United States, and 0 for New Zealand. Set
  // strip_non_digits to true to strip symbols like "~" (which indicates a wait
  // for a dialling tone) from the prefix returned. If no national prefix is
  // present, we return an empty string.
  void GetNddPrefixForRegion(const string& region_code,
                             bool strip_non_digits,
                             string* national_prefix) const;

  // Checks whether a phone number is a possible number. It provides a more
  // lenient check than IsValidNumber() in the following sense:
  //   1. It only checks the length of phone numbers. In particular, it doesn't
  //      check starting digits of the number.
  //   2. It doesn't attempt to figure out the type of the number, but uses
  //      general rules which applies to all types of phone numbers in a
  //      region. Therefore, it is much faster than IsValidNumber().
  //   3. For some numbers (particularly fixed-line), many regions have the
  //      concept of area code, which together with subscriber number constitute
  //      the national significant number. It is sometimes okay to dial only the
  //      subscriber number when dialing in the same area. This function will
  //      return IS_POSSIBLE_LOCAL_ONLY if the subscriber-number-only version is
  //      passed in. On the other hand, because IsValidNumber() validates using
  //      information on both starting digits (for fixed line numbers, that
  //      would most likely be area codes) and length (obviously includes the
  //      length of area codes for fixed line numbers), it will return false for
  //      the subscriber-number-only version.
  ValidationResult IsPossibleNumberWithReason(const PhoneNumber& number) const;

  // Convenience wrapper around IsPossibleNumberWithReason(). Instead of
  // returning the reason for failure, this method returns true if the number is
  // either a possible fully-qualified number (containing the area code and
  // country code), or if the number could be a possible local number (with a
  // country code, but missing an area code). Local numbers are considered
  // possible if they could be possibly dialled in this format: if the area code
  // is needed for a call to connect, the number is not considered possible
  // without it.
  bool IsPossibleNumber(const PhoneNumber& number) const;

  // Check whether a phone number is a possible number of a particular type. For
  // types that don't exist in a particular region, this will return a result
  // that isn't so useful; it is recommended that you use
  // GetSupportedTypesForRegion() or GetSupportedTypesForNonGeoEntity()
  // respectively before calling this method to determine whether you should
  // call it for this number at all.
  //
  // This provides a more lenient check than IsValidNumber() in the following
  // sense:
  //
  //   1. It only checks the length of phone numbers. In particular, it doesn't
  //      check starting digits of the number.
  //   2. For some numbers (particularly fixed-line), many regions have the
  //      concept of area code, which together with subscriber number constitute
  //      the national significant number. It is sometimes okay to dial only the
  //      subscriber number when dialing in the same area. This function will
  //      return IS_POSSIBLE_LOCAL_ONLY if the subscriber-number-only version is
  //      passed in. On the other hand, because IsValidNumber() validates using
  //      information on both starting digits (for fixed line numbers, that
  //      would most likely be area codes) and length (obviously includes the
  //      length of area codes for fixed line numbers), it will return false for
  //      the subscriber-number-only version.
  ValidationResult IsPossibleNumberForTypeWithReason(
      const PhoneNumber& number, PhoneNumberType type) const;

  // Convenience wrapper around IsPossibleNumberForTypeWithReason(). Instead of
  // returning the reason for failure, this method returns true if the number is
  // either a possible fully-qualified number (containing the area code and
  // country code), or if the number could be a possible local number (with a
  // country code, but missing an area code). Local numbers are considered
  // possible if they could be possibly dialled in this format: if the area code
  // is needed for a call to connect, the number is not considered possible
  // without it.
  bool IsPossibleNumberForType(const PhoneNumber& number,
                               PhoneNumberType type) const;

  // Checks whether a phone number is a possible number given a number in the
  // form of a string, and the country where the number could be dialed from.
  // It provides a more lenient check than IsValidNumber(). See
  // IsPossibleNumber(const PhoneNumber& number) for details.
  //
  // This method first parses the number, then invokes
  // IsPossibleNumber(const PhoneNumber& number) with the resultant PhoneNumber
  // object.
  //
  // region_dialing_from represents the region that we are expecting the number
  // to be dialed from. Note this is different from the region where the number
  // belongs. For example, the number +1 650 253 0000 is a number that belongs
  // to US. When written in this form, it could be dialed from any region. When
  // it is written as 00 1 650 253 0000, it could be dialed from any region
  // which uses an international dialling prefix of 00. When it is written as
  // 650 253 0000, it could only be dialed from within the US, and when written
  // as 253 0000, it could only be dialed from within a smaller area in the US
  // (Mountain View, CA, to be more specific).
  bool IsPossibleNumberForString(
      const string& number,
      const string& region_dialing_from) const;

  // Returns true if the number can be dialled from outside the region, or
  // unknown. If the number can only be dialled from within the region, returns
  // false. Does not check the number is a valid number. Note that, at the
  // moment, this method does not handle short numbers (which are currently all
  // presumed to not be diallable from outside their country).
  bool CanBeInternationallyDialled(const PhoneNumber& number) const;

  // Tests whether a phone number has a geographical association. It checks if
  // the number is associated with a certain region in the country to which it
  // belongs. Note that this doesn't verify if the number is actually in use.
  bool IsNumberGeographical(const PhoneNumber& phone_number) const;

  // Overload of IsNumberGeographical(PhoneNumber), since calculating the phone
  // number type is expensive; if we have already done this, we don't want to do
  // it again.
  bool IsNumberGeographical(PhoneNumberType phone_number_type,
                            int country_calling_code) const;

  // Gets a valid fixed-line number for the specified region. Returns false if
  // the region was unknown, or the region 001 is passed in. For 001
  // (representing non-geographical numbers), call
  // GetExampleNumberForNonGeoEntity instead.
  bool GetExampleNumber(const string& region_code,
                        PhoneNumber* number) const;

  // Gets an invalid number for the specified region. This is useful for
  // unit-testing purposes, where you want to test that will happen with an
  // invalid number. Note that the number that is returned will always be able
  // to be parsed and will have the correct country code. It may also be a valid
  // *short* number/code for this region. Validity checking such
  // numbers is handled with ShortNumberInfo.
  //
  // Returns false when an unsupported region or the region 001 (Earth) is
  // passed in.
  bool GetInvalidExampleNumber(const string& region_code,
                               PhoneNumber* number) const;

  // Gets a valid number of the specified type for the specified region.
  // Returns false if the region was unknown or 001, or if no example number of
  // that type could be found. For 001 (representing non-geographical numbers),
  // call GetExampleNumberForNonGeoEntity instead.
  bool GetExampleNumberForType(const string& region_code,
                               PhoneNumberType type,
                               PhoneNumber* number) const;

  // Gets a valid number for the specified type (it may belong to any country).
  // Returns false when the metadata does not contain such information.  This
  // should only happen when no numbers of this type are allocated anywhere in
  // the world anymore.
  bool GetExampleNumberForType(PhoneNumberType type,
                               PhoneNumber* number) const;

  // Gets a valid number for the specified country calling code for a
  // non-geographical entity. Returns false if the metadata does not contain
  // such information, or the country calling code passed in does not belong to
  // a non-geographical entity.
  bool GetExampleNumberForNonGeoEntity(
      int country_calling_code, PhoneNumber* number) const;

  // Parses a string and returns it as a phone number in proto buffer format.
  // The method is quite lenient and looks for a number in the input text
  // (raw input) and does not check whether the string is definitely only a
  // phone number. To do this, it ignores punctuation and white-space, as well
  // as any text before the number (e.g. a leading “Tel: ”) and trims the
  // non-number bits. It will accept a number in any format (E164, national,
  // international etc), assuming it can be interpreted with the defaultRegion
  // supplied. It also attempts to convert any alpha characters into digits
  // if it thinks this is a vanity number of the type "1800 MICROSOFT".
  //
  // This method will return an error if the number is not considered to be a
  // possible number, and NO_PARSING_ERROR if it is parsed correctly.
  // Note that validation of whether the number is actually a valid number for
  // a particular region is not performed. This can be done separately with
  // IsValidNumber().
  //
  // Note this method canonicalizes the phone number such that different
  // representations can be easily compared, no matter what form it was
  // originally entered in (e.g. national, international). If you want to record
  // context about the number being parsed, such as the raw input that was
  // entered, how the country code was derived etc. then call
  // ParseAndKeepRawInput() instead.
  //
  // number_to_parse can contain formatting such as +, ( and -, as well as a
  // phone number extension. It can also be provided in RFC3966 format.
  //
  // default_region represents the country that we are expecting the number to
  // be from. This is only used if the number being parsed is not written in
  // international format. The country_code for the number in this case would be
  // stored as that of the default country supplied. If the number is guaranteed
  // to start with a '+' followed by the country calling code, then
  // "ZZ" can be supplied.
  //
  // Returns an error if the string is not considered to be a viable phone
  // number (e.g.too few or too many digits) or if no default region was
  // supplied and the number is not in international format (does not start with
  // +).
  ErrorType Parse(const string& number_to_parse,
                  const string& default_region,
                  PhoneNumber* number) const;
  // Parses a string and returns it in proto buffer format. This method differs
  // from Parse() in that it always populates the raw_input field of the
  // protocol buffer with number_to_parse as well as the country_code_source
  // field.
  ErrorType ParseAndKeepRawInput(const string& number_to_parse,
                                 const string& default_region,
                                 PhoneNumber* number) const;

  // Takes two phone numbers and compares them for equality.
  //
  // Returns EXACT_MATCH if the country calling code, NSN, presence of a leading
  // zero for Italian numbers and any extension present are the same.
  // Returns NSN_MATCH if either or both has no country calling code specified,
  // and the NSNs and extensions are the same.
  // Returns SHORT_NSN_MATCH if either or both has no country calling code
  // specified, or the country calling code specified is the same, and one NSN
  // could be a shorter version of the other number. This includes the case
  // where one has an extension specified, and the other does not.
  // Returns NO_MATCH otherwise.
  // For example, the numbers +1 345 657 1234 and 657 1234 are a
  // SHORT_NSN_MATCH. The numbers +1 345 657 1234 and 345 657 are a NO_MATCH.
  MatchType IsNumberMatch(const PhoneNumber& first_number,
                          const PhoneNumber& second_number) const;

  // Takes two phone numbers as strings and compares them for equality. This
  // is a convenience wrapper for IsNumberMatch(PhoneNumber firstNumber,
  // PhoneNumber secondNumber). No default region is known.
  // Returns INVALID_NUMBER if either number cannot be parsed into a phone
  // number.
  MatchType IsNumberMatchWithTwoStrings(const string& first_number,
                                        const string& second_number) const;

  // Takes two phone numbers and compares them for equality. This is a
  // convenience wrapper for IsNumberMatch(PhoneNumber firstNumber,
  // PhoneNumber secondNumber). No default region is known.
  // Returns INVALID_NUMBER if second_number cannot be parsed into a phone
  // number.
  MatchType IsNumberMatchWithOneString(const PhoneNumber& first_number,
                                       const string& second_number) const;

  // Overrides the default logging system. This takes ownership of the provided
  // logger.
  void SetLogger(Logger* logger);

  // Gets an AsYouTypeFormatter for the specific region.
  // Returns an AsYouTypeFormatter object, which could be used to format phone
  // numbers in the specific region "as you type".
  // The deletion of the returned instance is under the responsibility of the
  // caller.
  AsYouTypeFormatter* GetAsYouTypeFormatter(const string& region_code) const;

  friend bool ConvertFromTelephoneNumberProto(
      const TelephoneNumber& proto_to_convert,
      PhoneNumber* new_proto);
  friend bool ConvertToTelephoneNumberProto(const PhoneNumber& proto_to_convert,
                                            TelephoneNumber* resulting_proto);

 protected:
  bool IsNumberMatchingDesc(const string& national_number,
                            const PhoneNumberDesc& number_desc) const;

  PhoneNumberUtil::PhoneNumberType GetNumberTypeHelper(
      const string& national_number, const PhoneMetadata& metadata) const;

 private:
  scoped_ptr<Logger> logger_;

  typedef std::pair<int, std::list<string>*> IntRegionsPair;

  // The minimum and maximum length of the national significant number.
  static const size_t kMinLengthForNsn = 2;
  // The ITU says the maximum length should be 15, but we have found longer
  // numbers in Germany.
  static const size_t kMaxLengthForNsn = 17;
  // The maximum length of the country calling code.
  static const size_t kMaxLengthCountryCode = 3;

  static const char kPlusChars[];
  // Regular expression of acceptable punctuation found in phone numbers. This
  // excludes punctuation found as a leading character only. This consists of
  // dash characters, white space characters, full stops, slashes, square
  // brackets, parentheses and tildes. It also includes the letter 'x' as that
  // is found as a placeholder for carrier information in some phone numbers.
  // Full-width variants are also present.
  static const char kValidPunctuation[];

  // Regular expression of characters typically used to start a second phone
  // number for the purposes of parsing. This allows us to strip off parts of
  // the number that are actually the start of another number, such as for:
  // (530) 583-6985 x302/x2303 -> the second extension here makes this actually
  // two phone numbers, (530) 583-6985 x302 and (530) 583-6985 x2303. We remove
  // the second extension so that the first number is parsed correctly. The
  // string preceding this is captured.
  // This corresponds to SECOND_NUMBER_START in the java version.
  static const char kCaptureUpToSecondNumberStart[];

  // An API for validation checking.
  scoped_ptr<MatcherApi> matcher_api_;

  // Helper class holding useful regular expressions and character mappings.
  scoped_ptr<PhoneNumberRegExpsAndMappings> reg_exps_;

  // A mapping from a country calling code to a RegionCode object which denotes
  // the region represented by that country calling code. Note regions under
  // NANPA share the country calling code 1 and Russia and Kazakhstan share the
  // country calling code 7. Under this map, 1 is mapped to region code "US" and
  // 7 is mapped to region code "RU". This is implemented as a sorted vector to
  // achieve better performance.
  scoped_ptr<std::vector<IntRegionsPair> >
      country_calling_code_to_region_code_map_;

  // The set of regions that share country calling code 1.
  scoped_ptr<absl::node_hash_set<string> > nanpa_regions_;
  static const int kNanpaCountryCode = 1;

  // A mapping from a region code to a PhoneMetadata for that region.
  scoped_ptr<absl::node_hash_map<string, PhoneMetadata> > region_to_metadata_map_;

  // A mapping from a country calling code for a non-geographical entity to the
  // PhoneMetadata for that country calling code. Examples of the country
  // calling codes include 800 (International Toll Free Service) and 808
  // (International Shared Cost Service).
  scoped_ptr<absl::node_hash_map<int, PhoneMetadata> >
      country_code_to_non_geographical_metadata_map_;

  PhoneNumberUtil();

  // Returns a regular expression for the possible extensions that may be found
  // in a number, for use when matching.
  const string& GetExtnPatternsForMatching() const;

  // Checks if a number matches the plus chars pattern.
  bool StartsWithPlusCharsPattern(const string& number) const;

  void SetItalianLeadingZerosForPhoneNumber(
      const string& national_number, PhoneNumber* phone_number) const;

  // Checks whether a string contains only valid digits.
  bool ContainsOnlyValidDigits(const string& s) const;

  // Checks if a format is eligible to be used by the AsYouTypeFormatter. This
  // method is here rather than in asyoutypeformatter.h since it depends on the
  // valid punctuation declared by the phone number util.
  bool IsFormatEligibleForAsYouTypeFormatter(const string& format) const;

  // Helper function to check if the national prefix formatting rule has the
  // first group only, i.e., does not start with the national prefix.
  bool FormattingRuleHasFirstGroupOnly(
      const string& national_prefix_formatting_rule) const;

  // Trims unwanted end characters from a phone number string.
  void TrimUnwantedEndChars(string* number) const;

  // Helper function to check region code is not unknown or null.
  bool IsValidRegionCode(const string& region_code) const;

  // Helper function to check the country calling code is valid.
  bool HasValidCountryCallingCode(int country_calling_code) const;

  const i18n::phonenumbers::PhoneMetadata* GetMetadataForRegion(
      const string& region_code) const;

  const i18n::phonenumbers::PhoneMetadata* GetMetadataForNonGeographicalRegion(
      int country_calling_code) const;

  const i18n::phonenumbers::PhoneMetadata* GetMetadataForRegionOrCallingCode(
      int country_calling_code,
      const string& region_code) const;

  // As per GetCountryCodeForRegion, but assumes the validity of the region_code
  // has already been checked.
  int GetCountryCodeForValidRegion(const string& region_code) const;

  const NumberFormat* ChooseFormattingPatternForNumber(
      const RepeatedPtrField<NumberFormat>& available_formats,
      const string& national_number) const;

  void FormatNsnUsingPatternWithCarrier(
      const string& national_number,
      const NumberFormat& formatting_pattern,
      PhoneNumberUtil::PhoneNumberFormat number_format,
      const string& carrier_code,
      string* formatted_number) const;

  void FormatNsnUsingPattern(
      const string& national_number,
      const NumberFormat& formatting_pattern,
      PhoneNumberUtil::PhoneNumberFormat number_format,
      string* formatted_number) const;

  // Check if raw_input, which is assumed to be in the national format, has a
  // national prefix. The national prefix is assumed to be in digits-only form.
  bool RawInputContainsNationalPrefix(
      const string& raw_input,
      const string& national_prefix,
      const string& region_code) const;

  bool HasFormattingPatternForNumber(const PhoneNumber& number) const;

  // Simple wrapper of FormatNsnWithCarrier for the common case of
  // no carrier code.
  void FormatNsn(const string& number,
                 const PhoneMetadata& metadata,
                 PhoneNumberFormat number_format,
                 string* formatted_number) const;

  void FormatNsnWithCarrier(const string& number,
                            const PhoneMetadata& metadata,
                            PhoneNumberFormat number_format,
                            const string& carrier_code,
                            string* formatted_number) const;

  void MaybeAppendFormattedExtension(
      const PhoneNumber& number,
      const PhoneMetadata& metadata,
      PhoneNumberFormat number_format,
      string* extension) const;

  void GetRegionCodeForNumberFromRegionList(
      const PhoneNumber& number,
      const std::list<string>& region_codes,
      string* region_code) const;

  // Strips the IDD from the start of the number if present. Helper function
  // used by MaybeStripInternationalPrefixAndNormalize.
  bool ParsePrefixAsIdd(const RegExp& idd_pattern, string* number) const;

  void Normalize(string* number) const;

  PhoneNumber::CountryCodeSource MaybeStripInternationalPrefixAndNormalize(
      const string& possible_idd_prefix,
      string* number) const;

  bool MaybeStripNationalPrefixAndCarrierCode(
      const PhoneMetadata& metadata,
      string* number,
      string* carrier_code) const;

  void ExtractPossibleNumber(const string& number,
                             string* extracted_number) const;

  bool IsViablePhoneNumber(const string& number) const;

  bool MaybeStripExtension(string* number, string* extension) const;

  int ExtractCountryCode(string* national_number) const;
  ErrorType MaybeExtractCountryCode(
      const PhoneMetadata* default_region_metadata,
      bool keepRawInput,
      string* national_number,
      PhoneNumber* phone_number) const;

  bool CheckRegionForParsing(
      const string& number_to_parse,
      const string& default_region) const;

  ErrorType ParseHelper(const string& number_to_parse,
                        const string& default_region,
                        bool keep_raw_input,
                        bool check_region,
                        PhoneNumber* phone_number) const;

  absl::optional<string> ExtractPhoneContext(
      const string& number_to_extract_from,
      size_t index_of_phone_context) const;

  bool IsPhoneContextValid(absl::optional<string> phone_context) const;

  ErrorType BuildNationalNumberForParsing(const string& number_to_parse,
                                          string* national_number) const;

  bool IsShorterThanPossibleNormalNumber(const PhoneMetadata* country_metadata,
                                         const string& number) const;

};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_PHONENUMBERUTIL_H_
