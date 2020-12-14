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

// Author: George Yakovlev
//         Philippe Liard
//
// RegExp adapter to allow a pluggable regexp engine. It has been introduced
// during the integration of the open-source version of this library into
// Chromium to be able to use the ICU Regex engine instead of RE2, which is not
// officially supported on Windows.
// Since RE2 was initially used in this library, the interface of this adapter
// is very close to the subset of the RE2 API used in phonenumberutil.cc.

#ifndef I18N_PHONENUMBERS_REGEXP_ADAPTER_H_
#define I18N_PHONENUMBERS_REGEXP_ADAPTER_H_

#include <cstddef>
#include <string>

namespace i18n {
namespace phonenumbers {

using std::string;

// RegExpInput is the interface that abstracts the input that feeds the
// Consume() method of RegExp which may differ depending on its various
// implementations (StringPiece for RE2, UnicodeString for ICU Regex).
class RegExpInput {
 public:
  virtual ~RegExpInput() {}

  // Converts to a C++ string.
  virtual string ToString() const = 0;
};

// The regular expression abstract class. It supports only functions used in
// phonenumberutil.cc. Consume(), Match() and Replace() methods must be
// implemented.
class RegExp {
 public:
  virtual ~RegExp() {}

  // Matches string to regular expression, returns true if expression was
  // matched, false otherwise, advances position in the match.
  // input_string - string to be searched.
  // anchor_at_start - if true, match would be successful only if it appears at
  // the beginning of the tested region of the string.
  // matched_string1..6 - string extracted from the match in sequential order.
  // Can be NULL.
  virtual bool Consume(RegExpInput* input_string,
                       bool anchor_at_start,
                       string* matched_string1,
                       string* matched_string2,
                       string* matched_string3,
                       string* matched_string4,
                       string* matched_string5,
                       string* matched_string6) const = 0;
                       
  // Helper methods calling the Consume method that assume the match must start
  // at the beginning.
  inline bool Consume(RegExpInput* input_string, string* matched_string1,
                      string* matched_string2,
                      string* matched_string3,
                      string* matched_string4,
                      string* matched_string5,
                      string* matched_string6) const {
    return Consume(input_string, true, matched_string1, matched_string2,
                   matched_string3, matched_string4, matched_string5,
                   matched_string6);
  }

  inline bool Consume(RegExpInput* input_string, string* matched_string1,
                      string* matched_string2,
                      string* matched_string3,
                      string* matched_string4,
                      string* matched_string5) const {
    return Consume(input_string, true, matched_string1, matched_string2,
                   matched_string3, matched_string4, matched_string5, NULL);
  }

  inline bool Consume(RegExpInput* input_string, string* matched_string1,
                      string* matched_string2,
                      string* matched_string3,
                      string* matched_string4) const {
    return Consume(input_string, true, matched_string1, matched_string2,
                   matched_string3, matched_string4, NULL, NULL);
  }


  // Helper methods calling the Consume method that assume the match must start
  // at the beginning.
  inline bool Consume(RegExpInput* input_string,
                      string* matched_string1,
                      string* matched_string2,
                      string* matched_string3) const {
    return Consume(input_string, true, matched_string1, matched_string2,
                   matched_string3, NULL, NULL, NULL);
  }

  inline bool Consume(RegExpInput* input_string,
                      string* matched_string1,
                      string* matched_string2) const {
    return Consume(input_string, true, matched_string1, matched_string2, NULL,
    		   NULL, NULL, NULL);
  }

  inline bool Consume(RegExpInput* input_string, string* matched_string) const {
    return Consume(input_string, true, matched_string, NULL, NULL, NULL, NULL,
    	   	   NULL);
  }

  inline bool Consume(RegExpInput* input_string) const {
    return Consume(input_string, true, NULL, NULL, NULL, NULL, NULL, NULL);
  }

  // Helper method calling the Consume method that assumes the match can start
  // at any place in the string.
  inline bool FindAndConsume(RegExpInput* input_string,
                             string* matched_string) const {
    return Consume(input_string, false, matched_string, NULL, NULL, NULL, NULL,
    	           NULL);
  }

  // Matches string to regular expression, returns true if the expression was
  // matched, false otherwise.
  // input_string - string to be searched.
  // full_match - if true, match would be successful only if it matches the
  // complete string.
  // matched_string - the string extracted from the match. Can be NULL.
  virtual bool Match(const string& input_string,
                     bool full_match,
                     string* matched_string) const = 0;

  // Helper methods calling the Match method with the right arguments.
  inline bool PartialMatch(const string& input_string,
                           string* matched_string) const {
    return Match(input_string, false, matched_string);
  }

  inline bool PartialMatch(const string& input_string) const {
    return Match(input_string, false, NULL);
  }

  inline bool FullMatch(const string& input_string,
                        string* matched_string) const {
    return Match(input_string, true, matched_string);
  }

  inline bool FullMatch(const string& input_string) const {
    return Match(input_string, true, NULL);
  }

  // Replaces match(es) in 'string_to_process'. If 'global' is true,
  // replaces all the matches, otherwise only the first match.
  // replacement_string - text the matches are replaced with. The groups in the
  // replacement string are referenced with the $[0-9] notation.
  // Returns true if the pattern matches and a replacement occurs, false
  // otherwise.
  virtual bool Replace(string* string_to_process,
                       bool global,
                       const string& replacement_string) const = 0;

  // Helper methods calling the Replace method with the right arguments.
  inline bool Replace(string* string_to_process,
                      const string& replacement_string) const {
    return Replace(string_to_process, false, replacement_string);
  }

  inline bool GlobalReplace(string* string_to_process,
                            const string& replacement_string) const {
    return Replace(string_to_process, true, replacement_string);
  }
};

// Abstract factory class that lets its subclasses instantiate the classes
// implementing RegExp and RegExpInput.
class AbstractRegExpFactory {
 public:
  virtual ~AbstractRegExpFactory() {}

  // Creates a new instance of RegExpInput. The deletion of the returned
  // instance is under the responsibility of the caller.
  virtual RegExpInput* CreateInput(const string& utf8_input) const = 0;

  // Creates a new instance of RegExp. The deletion of the returned instance is
  // under the responsibility of the caller.
  virtual RegExp* CreateRegExp(const string& utf8_regexp) const = 0;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_REGEXP_ADAPTER_H_
