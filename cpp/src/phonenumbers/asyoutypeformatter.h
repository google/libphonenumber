// Copyright (C) 2011 The Libphonenumber Authors
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

// A formatter which formats phone numbers as they are entered.
//
// An AsYouTypeFormatter can be created by invoking the GetAsYouTypeFormatter
// method of the PhoneNumberUtil. After that digits can be added by invoking the
// InputDigit method on the formatter instance, and the partially formatted
// phone number will be returned each time a digit is added. The Clear method
// can be invoked before a new number needs to be formatted.
//
// See AYTF_US, AYTF_GBFixedLine and AYTF_DE test functions in
// asyoutypeformatter_test.cc for more details on how the formatter is to be
// used.
//
// This is a direct port from AsYouTypeFormatter.java.
// Changes to this class should also happen to the Java version, whenever it
// makes sense.
//
// This class is NOT THREAD SAFE.

#ifndef I18N_PHONENUMBERS_ASYOUTYPEFORMATTER_H_
#define I18N_PHONENUMBERS_ASYOUTYPEFORMATTER_H_

#include <list>
#include <string>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/regexp_adapter.h"
#include "phonenumbers/regexp_cache.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/unicodestring.h"

namespace i18n {
namespace phonenumbers {

using std::list;

class PhoneNumberUtil;

class AsYouTypeFormatter {
 public:

 // This type is neither copyable nor movable.
  AsYouTypeFormatter(const AsYouTypeFormatter&) = delete;
  AsYouTypeFormatter& operator=(const AsYouTypeFormatter&) = delete;

  ~AsYouTypeFormatter() {}

  // Formats a phone number on-the-fly as each digit is entered.
  // next_char is the most recently entered digit of a phone number. Formatting
  // characters are allowed, but as soon as they are encountered this method
  // formats the number as entered and not "as you type" anymore. Full width
  // digits and Arabic-indic digits are allowed, and will be shown as they are.
  // Returns the partially formatted phone number (which is a reference to the
  // given string parameter for convenience).
  const string& InputDigit(char32 next_char, string* result);

  // Same as InputDigit, but remembers the position where next_char is inserted,
  // so that it could be retrieved later by using GetRememberedPosition(). The
  // remembered position will be automatically adjusted if additional formatting
  // characters are later inserted/removed in front of next_char.
  // Returns the partially formatted phone number (which is a reference to the
  // given string parameter for convenience).
  const string& InputDigitAndRememberPosition(char32 next_char, string* result);

  // Returns the current position in the partially formatted phone number of the
  // character which was previously passed in as the parameter of
  // InputDigitAndRememberPosition().
  int GetRememberedPosition() const;

  // Clears the internal state of the formatter, so it could be reused.
  void Clear();

 private:
  // Constructs an as-you-type formatter. Should be obtained from
  // PhoneNumberUtil::GetAsYouTypeFormatter().
  explicit AsYouTypeFormatter(const string& region_code);

  // Returns the metadata corresponding to the given region code or empty
  // metadata if it is unsupported.
  const PhoneMetadata* GetMetadataForRegion(const string& region_code) const;

  // Returns true if a new template is created as opposed to reusing the
  // existing template.
  bool MaybeCreateNewTemplate();

  void GetAvailableFormats(const string& leading_digits);

  void NarrowDownPossibleFormats(const string& leading_digits);

  // Calculates whether we should be adding a space after the national prefix
  // for this formatting rule or not.
  void SetShouldAddSpaceAfterNationalPrefix(const NumberFormat& format);

  bool CreateFormattingTemplate(const NumberFormat& format);

  // Gets a formatting template which could be used to efficiently format a
  // partial number where digits are added one by one.
  void GetFormattingTemplate(const string& number_pattern,
                             const string& number_format,
                             UnicodeString* formatting_template);

  void InputDigitWithOptionToRememberPosition(char32 next_char,
                                              bool remember_position,
                                              string* phone_number);

  void AttemptToChoosePatternWithPrefixExtracted(string* formatted_number);

  const string& GetExtractedNationalPrefix() const;

  // Some national prefixes are a substring of others. If extracting the
  // shorter NDD doesn't result in a number we can format, we try to see if we
  // can extract a longer version here.
  bool AbleToExtractLongerNdd();

  // Check to see if there is an exact pattern match for these digits. If so, we
  // should use this instead of any other formatting template whose
  // leadingDigitsPattern also matches the input.
  void AttemptToFormatAccruedDigits(string* formatted_number);

  // Combines the national number with any prefix (IDD/+ and country code or
  // national prefix) that was collected. A space will be inserted between them
  // if the current formatting template indicates this to be suitable.
  // The result will be stored in phone_number.
  void AppendNationalNumber(const string& national_number,
                            string* phone_number) const;

  // Attempts to set the formatting template and assigns the passed-in string
  // parameter to the formatted version of the digits entered so far.
  void AttemptToChooseFormattingPattern(string* formatted_number);

  // Invokes InputDigitHelper on each digit of the national number accrued, and
  // assigns the passed-in string parameter to a formatted string in the end.
  void InputAccruedNationalNumber(string* number);

  // Returns true if the current country is a NANPA country and the national
  // number begins with the national prefix.
  bool IsNanpaNumberWithNationalPrefix() const;

  // Extracts the national prefix into national_prefix, or sets it to empty
  // string if a national prefix is not present.
  void RemoveNationalPrefixFromNationalNumber(string* national_prefix);

  // Extracts IDD and plus sign to prefix_before_national_number_ when they are
  // available, and places the remaining input into national_number_.
  bool AttemptToExtractIdd();

  // Extracts country code from the begining of national_number_ to
  // prefix_before_national_number_ when they are available, and places the
  // remaining input into national_number_.
  // Returns true when a valid country code can be found.
  bool AttemptToExtractCountryCode();

  // Accrues digits and the plus sign to accrued_input_without_formatting for
  // later use. If next_char contains a digit in non-ASCII format (e.g the
  // full-width version of digits), it is first normalized to the ASCII
  // version. The return value is next_char itself, or its normalized version,
  // if next_char is a digit in non-ASCII format.
  char NormalizeAndAccrueDigitsAndPlusSign(char32 next_char,
                                           bool remember_position);

  void InputDigitHelper(char next_char, string* number);

  // Converts UnicodeString position to std::string position.
  static int ConvertUnicodeStringPosition(const UnicodeString& s, int pos);

  // Class attributes.
  const scoped_ptr<const AbstractRegExpFactory> regexp_factory_;
  RegExpCache regexp_cache_;

  string current_output_;

  UnicodeString formatting_template_;
  string current_formatting_pattern_;

  UnicodeString accrued_input_;
  UnicodeString accrued_input_without_formatting_;

  // This indicates whether AsYouTypeFormatter is currently doing the
  // formatting.
  bool able_to_format_;
  // Set to true when users enter their own formatting. AsYouTypeFormatter will
  // do no formatting at all when this is set to true.
  bool input_has_formatting_;
  // This is set to true when we know the user is entering a full national
  // significant number, since we have either detected a national prefix or an
  // international dialing prefix. When this is true, we will no longer use
  // local number formatting patterns.
  bool is_complete_number_;
  bool is_expecting_country_code_;

  const PhoneNumberUtil& phone_util_;

  const string default_country_;

  const PhoneMetadata empty_metadata_;
  const PhoneMetadata* const default_metadata_;
  const PhoneMetadata* current_metadata_;

  int last_match_position_;

  // The position of a digit upon which InputDigitAndRememberPosition is most
  // recently invoked, as found in the original sequence of characters the user
  // entered.
  int original_position_;

  // The position of a digit upon which InputDigitAndRememberPosition is most
  // recently invoked, as found in AccruedInputWithoutFormatting.
  int position_to_remember_;

  // This contains anything that has been entered so far preceding the national
  // significant number, and it is formatted (e.g. with space inserted). For
  // example, this can contain IDD, country code, and/or NDD, etc.
  string prefix_before_national_number_;
  bool should_add_space_after_national_prefix_;
  // This contains the national prefix that has been extracted. It contains only
  // digits without formatting.
  string extracted_national_prefix_;
  string national_number_;

  list<const NumberFormat*> possible_formats_;

  friend class PhoneNumberUtil;
  friend class AsYouTypeFormatterTest;

  // Disallow copy and assign since this class uses RegExpCache which can't be
  // copied.
  
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_ASYOUTYPEFORMATTER_H_
