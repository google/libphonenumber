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
//
// Author: Lara Rennie
// Author: Tao Huang
//
// This is a direct port from PhoneNumberMatcher.java.
// Changes to this class should also happen to the Java version, whenever it
// makes sense.

#ifndef I18N_PHONENUMBERS_PHONENUMBERMATCHER_H_
#define I18N_PHONENUMBERS_PHONENUMBERMATCHER_H_

#include <string>
#include <vector>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/callback.h"
#include "phonenumbers/regexp_adapter.h"

namespace i18n {
namespace phonenumbers {

template <class R, class A1, class A2, class A3, class A4>
    class ResultCallback4;

using std::string;
using std::vector;

class AlternateFormats;
class NumberFormat;
class PhoneNumber;
class PhoneNumberMatch;
class PhoneNumberMatcherRegExps;
class PhoneNumberUtil;

class PhoneNumberMatcher {
  friend class PhoneNumberMatcherTest;
 public:
  // Leniency when finding potential phone numbers in text segments. The levels
  // here are ordered in increasing strictness.
  enum Leniency {
    // Phone numbers accepted are possible, but not necessarily valid.
    POSSIBLE,
    // Phone numbers accepted are possible and valid.
    VALID,
    // Phone numbers accepted are valid and are grouped in a possible way for
    // this locale. For example, a US number written as "65 02 53 00 00" is not
    // accepted at this leniency level, whereas "650 253 0000" or "6502530000"
    // are. Numbers with more than one '/' symbol are also dropped at this
    // level.
    // Warning: The next two levels might result in lower coverage especially
    // for regions outside of country code "+1". If you are not sure about which
    // level to use, you can send an e-mail to the discussion group
    // http://groups.google.com/group/libphonenumber-discuss/
    STRICT_GROUPING,
    // Phone numbers accepted are valid and are grouped in the same way that we
    // would have formatted it, or as a single block. For example, a US number
    // written as "650 2530000" is not accepted at this leniency level, whereas
    // "650 253 0000" or "6502530000" are.
    EXACT_GROUPING,
  };

  // Constructs a phone number matcher.
  PhoneNumberMatcher(const PhoneNumberUtil& util,
                     const string& text,
                     const string& region_code,
                     Leniency leniency,
                     int max_tries);

  // Wrapper to construct a phone number matcher, with no limitation on the
  // number of retries and VALID Leniency.
  PhoneNumberMatcher(const string& text,
                     const string& region_code);

  ~PhoneNumberMatcher();

  // Returns true if the text sequence has another match. Return false if not.
  // Always returns false when input contains non UTF-8 characters.
  bool HasNext();

  // Gets next match from text sequence.
  bool Next(PhoneNumberMatch* match);

 private:
  // The potential states of a PhoneNumberMatcher.
  enum State {
    NOT_READY,
    READY,
    DONE,
  };

  // Checks if the to check if the provided text_ is in UTF-8 or not.
  bool IsInputUtf8();

  // Attempts to extract a match from a candidate string. Returns true if a
  // match is found, otherwise returns false. The value "offset" refers to the
  // start index of the candidate string within the overall text.
  bool Find(int index, PhoneNumberMatch* match);

  // Checks a number was formatted with a national prefix, if the number was
  // found in national format, and a national prefix is required for that
  // number. Returns false if the number needed to have a national prefix and
  // none was found.
  bool IsNationalPrefixPresentIfRequired(const PhoneNumber& number) const;

  // Attempts to extract a match from candidate. Returns true if the match was
  // found, otherwise returns false.
  bool ExtractMatch(const string& candidate, int offset,
                    PhoneNumberMatch* match);

  // Attempts to extract a match from a candidate string if the whole candidate
  // does not qualify as a match. Returns true if a match is found, otherwise
  // returns false.
  bool ExtractInnerMatch(const string& candidate, int offset,
                         PhoneNumberMatch* match);

  // Parses a phone number from the candidate using PhoneNumberUtil::Parse() and
  // verifies it matches the requested leniency. If parsing and verification
  // succeed, returns true, otherwise this method returns false;
  bool ParseAndVerify(const string& candidate, int offset,
                      PhoneNumberMatch* match);

  bool CheckNumberGroupingIsValid(
    const PhoneNumber& phone_number,
    const string& candidate,
    ResultCallback4<bool, const PhoneNumberUtil&, const PhoneNumber&,
                    const string&, const vector<string>&>* checker) const;

  // Helper method to get the national-number part of a number, formatted
  // without any national prefix, and return it as a set of digit blocks that
  // would be formatted together following standard formatting rules.
  void GetNationalNumberGroups(
      const PhoneNumber& number,
      vector<string>* digit_blocks) const;

  // Helper method to get the national-number part of a number, formatted
  // without any national prefix, and return it as a set of digit blocks that
  // should be formatted together according to the formatting pattern passed in.
  void GetNationalNumberGroupsForPattern(
      const PhoneNumber& number,
      const NumberFormat* formatting_pattern,
      vector<string>* digit_blocks) const;

  bool AllNumberGroupsAreExactlyPresent(
      const PhoneNumberUtil& util,
      const PhoneNumber& phone_number,
      const string& normalized_candidate,
      const vector<string>& formatted_number_groups) const;

  bool VerifyAccordingToLeniency(Leniency leniency, const PhoneNumber& number,
                                 const string& candidate) const;

  // In interface for testing purposes.
  static bool ContainsMoreThanOneSlashInNationalNumber(
      const PhoneNumber& number,
      const string& candidate,
      const PhoneNumberUtil& util);

  // Helper method to determine if a character is a Latin-script letter or not.
  // For our purposes, combining marks should also return true since we assume
  // they have been added to a preceding Latin character.
  static bool IsLatinLetter(char32 letter);

  // Helper class holding useful regular expressions.
  const PhoneNumberMatcherRegExps* reg_exps_;

  // Helper class holding loaded data containing alternate ways phone numbers
  // might be formatted for certain regions.
  const AlternateFormats* alternate_formats_;

  // The phone number utility;
  const PhoneNumberUtil& phone_util_;

  // The text searched for phone numbers;
  const string text_;

  // The region(country) to assume for phone numbers without an international
  // prefix.
  const string preferred_region_;

  // The degree of validation requested.
  Leniency leniency_;

  // The maximum number of retries after matching an invalid number.
  int max_tries_;

  // The iteration tristate.
  State state_;

  // The last successful match, NULL unless in State.READY.
  scoped_ptr<PhoneNumberMatch> last_match_;

  // The next index to start searching at. Undefined in State.DONE.
  int search_index_;

  // Flag to set or check if input text is in UTF-8 or not.
  bool is_input_valid_utf8_;

  DISALLOW_COPY_AND_ASSIGN(PhoneNumberMatcher);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_PHONENUMBERMATCHER_H_
