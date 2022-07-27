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

#include "phonenumbers/regexp_adapter_re2.h"

#include <cstddef>
#include <string>

#include <re2/re2.h>
#include <re2/stringpiece.h>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/logging.h"
#include "phonenumbers/stringutil.h"

#include "absl/strings/string_view.h"
namespace i18n {
namespace phonenumbers {

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
                     string* out3,
                     string* out4,
                     string* out5,
                     string* out6) {
  const RE2::Arg outs[] = { out1, out2, out3, out4, out5, out6};
  const RE2::Arg* const args[] = {&outs[0], &outs[1], &outs[2], 
                                  &outs[3], &outs[4], &outs[5]};
  const int argc =
      out6 ? 6 : out5 ? 5 : out4 ? 4 : out3 ? 3 : out2 ? 2 : out1 ? 1 : 0;
  return regex_function(input, regexp, args, argc);
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
                       string* matched_string3,
                       string* matched_string4,
                       string* matched_string5,
                       string* matched_string6) const {
    DCHECK(input_string);
    StringPiece* utf8_input =
        static_cast<RE2RegExpInput*>(input_string)->Data();

    if (anchor_at_start) {
      return DispatchRE2Call(RE2::ConsumeN, utf8_input, utf8_regexp_,
                             matched_string1, matched_string2,
                             matched_string3, matched_string4,
                             matched_string5, matched_string6);
    } else {
      return DispatchRE2Call(RE2::FindAndConsumeN, utf8_input, utf8_regexp_,
                             matched_string1, matched_string2,
                             matched_string3, matched_string4,
                             matched_string5, matched_string6);
    }
  }

  virtual bool Match(const string& input_string,
                     bool full_match,
                     string* matched_string) const {
    if (full_match) {
      return DispatchRE2Call(RE2::FullMatchN, input_string, utf8_regexp_,
                             matched_string, NULL, NULL, NULL, NULL, NULL);
    } else {
      return DispatchRE2Call(RE2::PartialMatchN, input_string, utf8_regexp_,
                             matched_string, NULL, NULL, NULL, NULL, NULL);
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

RegExpInput* RE2RegExpFactory::CreateInput(const string& utf8_input) const {
  return new RE2RegExpInput(utf8_input);
}

RegExp* RE2RegExpFactory::CreateRegExp(const string& utf8_regexp) const {
  return new RE2RegExp(utf8_regexp);
}

}  // namespace phonenumbers
}  // namespace i18n
