// Copyright (C) 2012 The Libphonenumber Authors
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
// Author: Philippe Liard.
//
// Light implementation emulating base/callback.h. This is an internal header,
// users should not depend on it.
// Note that this implementation is very limited for now and
// libphonenumber-specific.

#ifndef I18N_PHONENUMBERS_CALLBACK_H_
#define I18N_PHONENUMBERS_CALLBACK_H_

namespace i18n {
namespace phonenumbers {

template <typename R, typename A1, typename A2, typename A3, typename A4>
class ResultCallback4 {
 public:
  virtual ~ResultCallback4() {}
  virtual R Run(A1 a1, A2 a2, A3 a3, A4 a4) = 0;
};

template <typename R, typename A1, typename A2, typename A3, typename A4>
class FunctionCallback4 : public ResultCallback4<R, A1, A2, A3, A4> {
 public:
  typedef R (FunctionType)(A1, A2, A3, A4);

  explicit FunctionCallback4(FunctionType* function) : function_(function) {}
  virtual ~FunctionCallback4() {}

  virtual R Run(A1 a1, A2 a2, A3 a3, A4 a4) {
    return function_(a1, a2, a3, a4);
  }

 private:
  FunctionType* const function_;
};

template <typename T, typename R, typename A1, typename A2, typename A3,
          typename A4>
class ConstMethodCallback4 : public ResultCallback4<R, A1, A2, A3, A4> {
 public:
  typedef R (T::*MethodType)(A1, A2, A3, A4) const;

  ConstMethodCallback4(const T* instance, MethodType method)
    : instance_(instance),
      method_(method) {}
  virtual ~ConstMethodCallback4() {}

  virtual R Run(A1 a1, A2 a2, A3 a3, A4 a4) {
    return (instance_->*method_)(a1, a2, a3, a4);
  }

 private:
  const T* const instance_;
  MethodType const method_;
};

template <typename R, typename A1, typename A2, typename A3, typename A4>
ResultCallback4<R, A1, A2, A3, A4>* NewPermanentCallback(
    R (*function)(A1, A2, A3, A4)) {
  return new FunctionCallback4<R, A1, A2, A3, A4>(function);
}

template <typename T, typename R, typename A1, typename A2, typename A3,
          typename A4>
ResultCallback4<R, A1, A2, A3, A4>* NewPermanentCallback(
    const T* instance,
    R (T::*method)(A1, A2, A3, A4) const) {
  return new ConstMethodCallback4<T, R, A1, A2, A3, A4>(instance, method);
}

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_CALLBACK_H_
