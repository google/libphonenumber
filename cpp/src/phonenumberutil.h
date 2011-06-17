// Copyright (C) 2009 Google Inc.
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
//
// Author: Shaopeng Jia
// Open-sourced by: Philippe Liard

#ifndef I18N_PHONENUMBERS_PHONENUMBERUTIL_H_
#define I18N_PHONENUMBERS_PHONENUMBERUTIL_H_

#include <list>
#include <map>
#include <set>
#include <string>
#include <utility>
#include <vector>

#include "base/basictypes.h"
#include "base/scoped_ptr.h"
#include "base/singleton.h"
#include "phonenumber.pb.h"

class TelephoneNumber;

namespace i18n {
namespace phonenumbers {

using std::list;
using std::map;
using std::pair;
using std::set;
using std::string;
using std::vector;

using google::protobuf::RepeatedPtrField;

class LoggerAdapter;
class NumberFormat;
class PhoneMetadata;
class PhoneMetadataCollection;
class PhoneNumber;

// NOTE: A lot of methods in this class require Region Code strings. These must
// be provided using ISO 3166-1 two-letter country-code format. The list of the
// codes can be found here:
// http://www.iso.org/iso/english_country_names_and_code_elements

#ifdef USE_GOOGLE_BASE
class PhoneNumberUtil {
  friend struct DefaultSingletonTraits<PhoneNumberUtil>;
#else
class PhoneNumberUtil : public Singleton<PhoneNumberUtil> {
  friend class Singleton<PhoneNumberUtil>;
#endif
  friend class PhoneNumberUtilTest;
 public:
  ~PhoneNumberUtil();

  // INTERNATIONAL and NATIONAL formats are consistent with the definition
  // in ITU-T Recommendation E. 123. For example, the number of the Google
  // Zürich office will be written as "+41 44 668 1800" in INTERNATIONAL
  // format, and as "044 668 1800" in NATIONAL format. E164 format is as per
  // INTERNATIONAL format but with no formatting applied e.g. +41446681800.
  // RFC3966 is as per INTERNATIONAL format, but with all spaces and other
  // separating symbols replaced with a hyphen, and with any phone number
  // extension appended with ";ext=".
  enum PhoneNumberFormat {
    E164,
    INTERNATIONAL,
    NATIONAL,
    RFC3966
  };

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
    // A phone number is of type UNKNOWN when it does not fit any of the known
    // patterns for a specific region.
    UNKNOWN
  };

  // Types of phone number matches. See detailed description beside the
  // IsNumberMatch() method.
  enum MatchType {
    INVALID_NUMBER,  // NOT_A_NUMBER in the java version.
    NO_MATCH,
    SHORT_NSN_MATCH,
    NSN_MATCH,
    EXACT_MATCH,
  };

  enum ErrorType {
    NO_PARSING_ERROR,
    INVALID_COUNTRY_CODE_ERROR,  // INVALID_COUNTRY_CODE in the java version.
    NOT_A_NUMBER,
    TOO_SHORT_AFTER_IDD,
    TOO_SHORT_NSN,
    TOO_LONG_NSN,  // TOO_LONG in the java version.
  };

  // Possible outcomes when testing if a PhoneNumber is possible.
  enum ValidationResult {
    IS_POSSIBLE,
    INVALID_COUNTRY_CODE,
    TOO_SHORT,
    TOO_LONG,
  };

  // Gets a PhoneNumberUtil instance to carry out international phone number
  // formatting, parsing, or validation. The instance is loaded with phone
  // number metadata for a number of most commonly used regions, as specified by
  // DEFAULT_REGIONS_.
  //
  // The PhoneNumberUtil is implemented as a singleton. Therefore, calling
  // getInstance multiple times will only result in one instance being created.
#ifdef USE_GOOGLE_BASE
  static PhoneNumberUtil* GetInstance();
#endif

  // Initialisation helper function used to populate the regular expressions in
  // a defined order.
  void CreateRegularExpressions() const;

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
  static void NormalizeDigitsOnly(string* number);

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
  // const PhoneNumberUtil& phone_util(PhoneNumberUtil::GetInstance());
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
  //   area_code = national_significant_number.substring(0, area_code_length);
  //   subscriber_number = national_significant_number.substring(
  //       area_code_length, string::npos);
  // else {
  //   area_code = "";
  //   subscriber_number = national_significant_number;
  // }
  //
  // N.B.: area code is a very ambiguous concept, so the I18N team generally
  // recommends against using it for most purposes, but recommends using the
  // more general national_number instead. Read the following carefully before
  // deciding to use this method:
  //
  //  - geographical area codes change over time, and this method honors those
  //    changes; therefore, it doesn't guarantee the stability of the result it
  //    produces.
  //  - subscriber numbers may not be diallable from all devices (notably mobile
  //    devices, which typically requires the full national_number to be dialled
  //    in most regions).
  //  - most non-geographical numbers have no area codes.
  //  - some geographical numbers have no area codes.
  int GetLengthOfGeographicalAreaCode(const PhoneNumber& number) const;

  // Gets the length of the national destination code (NDC) from the PhoneNumber
  // object passed in, so that clients could use it to split a national
  // significant number into NDC and subscriber number. The NDC of a phone
  // number is normally the first group of digit(s) right after the country
  // calling code when the number is formatted in the international format, if
  // there is a subscriber number part that follows. An example of how this
  // could be used:
  //
  // const PhoneNumberUtil& phone_util(PhoneNumberUtil::GetInstance());
  // PhoneNumber number;
  // phone_util.Parse("16502530000", "US", &number);
  // string national_significant_number;
  // phone_util.GetNationalSignificantNumber(number,
  //                                         &national_significant_number);
  // string national_destination_code;
  // string subscriber_number;
  //
  // int national_destination_code_length =
  //     phone_util.GetLengthOfGeographicalAreaCode(number);
  // if (national_destination_code_length > 0) {
  //   national_destination_code = national_significant_number.substring(
  //       0, national_destination_code_length);
  //   subscriber_number = national_significant_number.substring(
  //       national_destination_code_length, string::npos);
  // else {
  //   national_destination_code = "";
  //   subscriber_number = national_significant_number;
  // }
  //
  // Refer to the unittests to see the difference between this function and
  // GetLengthOfGeographicalAreaCode().
  int GetLengthOfNationalDestinationCode(const PhoneNumber& number) const;

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

  // Formats a phone number using the original phone number format that the
  // number is parsed from. The original format is embedded in the
  // country_code_source field of the PhoneNumber object passed in. If such
  // information is missing, the number will be formatted into the NATIONAL
  // format by default.
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

  // Gets the type of a phone number.
  PhoneNumberType GetNumberType(const PhoneNumber& number) const;

  // Tests whether a phone number matches a valid pattern. Note this doesn't
  // verify the number is actually in use, which is impossible to tell by just
  // looking at a number itself.
  bool IsValidNumber(const PhoneNumber& number) const;

  // Tests whether a phone number is valid for a certain region. Note this
  // doesn't verify the number is actually in use, which is impossible to tell
  // by just looking at a number itself. If the country calling code is not the
  // same as the country calling code for the region, this immediately exits
  // with false.  After this, the specific number pattern rules for the region
  // are examined.
  // This is useful for determining for example whether a particular number is
  // valid for Canada, rather than just a valid NANPA number.
  bool IsValidNumberForRegion(
      const PhoneNumber& number,
      const string& region_code) const;

  // Returns the region where a phone number is from. This could be used for
  // geo-coding at the region level.
  void GetRegionCodeForNumber(const PhoneNumber& number,
                              string* region_code) const;

  // Returns the country calling code for a specific region. For example,
  // this would be 1 for the United States, and 64 for New Zealand.
  int GetCountryCodeForRegion(const string& region_code) const;

  // Returns the region code that matches the specific country code. Note that
  // it is possible that several regions share the same country code (e.g. US
  // and Canada), and in that case, only one of the regions (normally the one
  // with the largest population) is returned.
  void GetRegionCodeForCountryCode(int country_code, string* region_code) const;

  // Checks if this is a region under the North American Numbering Plan
  // Administration (NANPA).
  bool IsNANPACountry(const string& region_code) const;

  // Checks whether a phone number is a possible number. It provides a more
  // lenient check than IsValidNumber() in the following sense:
  //   1. It only checks the length of phone numbers. In particular, it doesn't
  //      check starting digits of the number.
  //   2. It doesn't attempt to figure out the type of the number, but uses
  //      general rules which applies to all types of phone numbers in a
  //      region. Therefore, it is much faster than IsValidNumber().
  //   3. For fixed line numbers, many regions have the concept of area code,
  //      which together with subscriber number constitute the national
  //      significant number. It is sometimes okay to dial the subscriber
  //      number only when dialing in the same area. This function will return
  //      true if the subscriber-number-only version is passed in. On the other
  //      hand, because IsValidNumber() validates using information on both
  //      starting digits (for fixed line numbers, that would most likely be
  //      area codes) and length (obviously includes the length of area codes
  //      for fixed line numbers), it will return false for the
  //      subscriber-number-only version.
  ValidationResult IsPossibleNumberWithReason(const PhoneNumber& number) const;

  // Convenience wrapper around IsPossibleNumberWithReason. Instead of returning
  // the reason for failure, this method returns a boolean value.
  bool IsPossibleNumber(const PhoneNumber& number) const;

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

  // Gets a valid fixed-line number for the specified region. Returns false if
  // the region was unknown.
  bool GetExampleNumber(const string& region_code,
                        PhoneNumber* number) const;

  // Gets a valid number of the specified type for the specified region.
  // Returns false if the region was unknown or if no example number of that
  // type could be found.
  bool GetExampleNumberForType(const string& region_code,
                               PhoneNumberType type,
                               PhoneNumber* number) const;

  // Parses a string and returns it in proto buffer format. This method will
  // return an error like INVALID_COUNTRY_CODE if the number is not considered
  // to be a possible number, and NO_PARSING_ERROR if it parsed correctly. Note
  // that validation of whether the number is actually a valid number for a
  // particular region is not performed. This can be done separately with
  // IsValidNumber().
  //
  // default_region represents the country that we are expecting the number to
  // be from. This is only used if the number being parsed is not written in
  // international format. The country_code for the number in this case would be
  // stored as that of the default country supplied. If the number is guaranteed
  // to start with a '+' followed by the country calling code, then
  // "ZZ" can be supplied.
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

  // Override the default logging system. The provided adapter destruction is
  // handled by this class (don't delete it).
  static void SetLoggerAdapter(LoggerAdapter* logger_adapter);

  friend bool ConvertFromTelephoneNumberProto(
      const TelephoneNumber& proto_to_convert,
      PhoneNumber* new_proto);
  friend bool ConvertToTelephoneNumberProto(const PhoneNumber& proto_to_convert,
                                            TelephoneNumber* resulting_proto);

 protected:
  // Check whether the country_calling_code is from a country whose national
  // significant number could contain a leading zero. An example of such a
  // country is Italy.
  bool IsLeadingZeroPossible(int country_calling_code) const;

 private:
  typedef pair<int, list<string>*> IntRegionsPair;

  // The minimum and maximum length of the national significant number.
  static const size_t kMinLengthForNsn = 3;
  static const size_t kMaxLengthForNsn = 15;
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

  // A mapping from a country calling code to a region code which denotes the
  // region represented by that country calling code. Note countries under
  // NANPA share the country calling code 1 and Russia and Kazakhstan share the
  // country calling code 7. Under this map, 1 is mapped to region code "US" and
  // 7 is mapped to region code "RU". This is implemented as a sorted vector to
  // achieve better performance.
  scoped_ptr<vector<IntRegionsPair> > country_calling_code_to_region_code_map_;

  // The set of regions that share country calling code 1.
  scoped_ptr<set<string> > nanpa_regions_;
  static const int kNanpaCountryCode = 1;

  // A mapping from a region code to a PhoneMetadata for that region.
  scoped_ptr<map<string, PhoneMetadata> > region_to_metadata_map_;

  PhoneNumberUtil();

  // Returns a regular expression for the possible extensions that may be found
  // in a number.
  const string& GetExtnPatterns() const;

  // Trims unwanted end characters from a phone number string.
  void TrimUnwantedEndChars(string* number) const;

  // Gets all the supported regions.
  void GetSupportedRegions(set<string>* regions) const;

  // Returns the national dialling prefix for a specific region. For example,
  // this would be 1 for the United States, and 0 for New Zealand. Set
  // stripNonDigits to true to strip symbols like "~" (which indicates a wait
  // for a dialling tone) from the prefix returned. If no national prefix is
  // present, we return an empty string.
  //
  // Set strip_non_digits to true to strip non-digits from the national
  // dialling prefix.
  void GetNddPrefixForRegion(const string& region_code,
                             bool strip_non_digits,
                             string* national_prefix) const;

  // Helper function to check region code is not unknown or null.
  bool IsValidRegionCode(const string& region_code) const;

  // Helper function to check region code is not unknown. The
  // country_calling_code and number supplied is used only for the resultant log
  // message.
  bool HasValidRegionCode(const string& region_code,
                          int country_code,
                          const string& number) const;

  const i18n::phonenumbers::PhoneMetadata* GetMetadataForRegion(
      const string& region_code) const;

  void GetRegionCodesForCountryCallingCode(
      int country_calling_code,
      list<string>* region_codes) const;

  // Simple wrapper of FormatNationalNumberWithCarrier for the common case of
  // no carrier code.
  void FormatNationalNumber(const string& number,
                            const string& region_code,
                            PhoneNumberFormat number_format,
                            string* formatted_number) const;

  void FormatNationalNumberWithCarrier(const string& number,
                                       const string& region_code,
                                       PhoneNumberFormat number_format,
                                       const string& carrier_code,
                                       string* formatted_number) const;
  void MaybeGetFormattedExtension(
      const PhoneNumber& number,
      const string& region_code,
      PhoneNumberFormat number_format,
      string* extension) const;

  void FormatExtension(const string& extension_digits,
                       const string& region_code,
                       string* extension) const;

  void GetRegionCodeForNumberFromRegionList(
      const PhoneNumber& number,
      const list<string>& region_codes,
      string* region_code) const;

  void Normalize(string* number) const;
  PhoneNumber::CountryCodeSource MaybeStripInternationalPrefixAndNormalize(
      const string& possible_idd_prefix,
      string* number) const;

  void MaybeStripNationalPrefixAndCarrierCode(
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

  DISALLOW_COPY_AND_ASSIGN(PhoneNumberUtil);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_PHONENUMBERUTIL_H_
