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

#include "base/basictypes.h"
#include "base/memory/scoped_ptr.h"
#include "phonenumbers/regexp_adapter.h"

namespace i18n {
namespace phonenumbers {

using std::string;

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
    // Warning: This and the next level might result in lower coverage
    // especially for regions outside of country code "+1".
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

  // Returns true if the text sequence has another match.
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

  // Attempts to extract a match from a candidate string. Returns true if a
  // match is found, otherwise returns false. The value "offset" refers to the
  // start index of the candidate string within the overall text.
  bool Find(int index, PhoneNumberMatch* match);

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

  bool VerifyAccordingToLeniency(Leniency leniency, const PhoneNumber& number,
                                 const string& candidate) const;

  // Helper method to determine if a character is a Latin-script letter or not.
  // For our purposes, combining marks should also return true since we assume
  // they have been added to a preceding Latin character.
  static bool IsLatinLetter(char32 letter);

  // Helper class holding useful regular expressions.
  const PhoneNumberMatcherRegExps* reg_exps_;

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

  DISALLOW_COPY_AND_ASSIGN(PhoneNumberMatcher);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_PHONENUMBERMATCHER_H_
