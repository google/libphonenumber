// Copyright (C) 2011 Google Inc.
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

#include "regexp_adapter.h"

#include <cstddef>
#include <string>

#include <re2/re2.h>
#include <re2/stringpiece.h>

#include "base/basictypes.h"
#include "base/logging.h"
#include "stringutil.h"

namespace i18n {
namespace phonenumbers {

using re2::StringPiece;

// Implementation of RegExpInput abstract class.
class RE2RegExpInput : public RegExpInput {
 public:
  explicit RE2RegExpInput(const string& utf8_input)
      : string_(utf8_input),
        utf8_input_(string_) {}

  virtual string ToString() const {
    return utf8_input_.ToString();
  }

  StringPiece* Data() {
    return &utf8_input_;
  }

 private:
  // string_ holds the string referenced by utf8_input_ as StringPiece doesn't
  // copy the string passed in.
  const string string_;
  StringPiece utf8_input_;
};

namespace {

template <typename Function, typename Input>
bool DispatchRE2Call(Function regex_function,
                     Input input,
                     const RE2& regexp,
                     string* out1,
                     string* out2,
                     string* out3) {
  if (out3) {
    return regex_function(input, regexp, out1, out2, out3);
  }
  if (out2) {
    return regex_function(input, regexp, out1, out2);
  }
  if (out1) {
    return regex_function(input, regexp, out1);
  }
  return regex_function(input, regexp);
}

// Replaces unescaped dollar-signs with backslashes. Backslashes are deleted
// when they escape dollar-signs.
string TransformRegularExpressionToRE2Syntax(const string& regex) {
  string re2_regex(regex);
  if (GlobalReplaceSubstring("$", "\\", &re2_regex) == 0) {
    return regex;
  }
  // If we replaced a dollar sign with a backslash and there are now two
  // backslashes in the string, we assume that the dollar-sign was previously
  // escaped and that we need to retain it. To do this, we replace pairs of
  // backslashes with a dollar sign.
  GlobalReplaceSubstring("\\\\", "$", &re2_regex);
  return re2_regex;
}

}  // namespace

// Implementation of RegExp abstract class.
class RE2RegExp : public RegExp {
 public:
  explicit RE2RegExp(const string& utf8_regexp)
      : utf8_regexp_(utf8_regexp) {}

  virtual bool Consume(RegExpInput* input_string,
                       bool anchor_at_start,
                       string* matched_string1,
                       string* matched_string2,
                       string* matched_string3) const {
    DCHECK(input_string);
    StringPiece* utf8_input =
        static_cast<RE2RegExpInput*>(input_string)->Data();

    if (anchor_at_start) {
      return DispatchRE2Call(RE2::Consume, utf8_input, utf8_regexp_,
                             matched_string1, matched_string2,
                             matched_string3);
    } else {
      return DispatchRE2Call(RE2::FindAndConsume, utf8_input, utf8_regexp_,
                             matched_string1, matched_string2,
                             matched_string3);
    }
  }

  virtual bool Match(const string& input_string,
                     bool full_match,
                     string* matched_string) const {
    if (full_match) {
      return DispatchRE2Call(RE2::FullMatch, input_string, utf8_regexp_,
                             matched_string, NULL, NULL);
    } else {
      return DispatchRE2Call(RE2::PartialMatch, input_string, utf8_regexp_,
                             matched_string, NULL, NULL);
    }
  }

  virtual bool Replace(string* string_to_process,
                       bool global,
                       const string& replacement_string) const {
    DCHECK(string_to_process);
    const string re2_replacement_string =
        TransformRegularExpressionToRE2Syntax(replacement_string);
    if (global) {
      return RE2::GlobalReplace(string_to_process, utf8_regexp_,
                                re2_replacement_string);
    } else {
      return RE2::Replace(string_to_process, utf8_regexp_,
                          re2_replacement_string);
    }
  }

 private:
  RE2 utf8_regexp_;
};

// Implementation of the adapter static factory methods.
// RE2 RegExp engine is the default implementation.
RegExpInput* RegExpInput::Create(const string& utf8_input) {
  return new RE2RegExpInput(utf8_input);
}

RegExp* RegExp::Create(const string& utf8_regexp) {
  return new RE2RegExp(utf8_regexp);
}

}  // namespace phonenumbers
}  // namespace i18n
