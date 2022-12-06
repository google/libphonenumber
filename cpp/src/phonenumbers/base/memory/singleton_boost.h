// Copyright (C) 2020 The Libphonenumber Authors
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

#ifndef I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_BOOST_H_
#define I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_BOOST_H_

#include <boost/scoped_ptr.hpp>
#include <boost/thread/once.hpp>
#include <boost/utility.hpp>

namespace i18n {
namespace phonenumbers {

template <class T>
class Singleton : private boost::noncopyable {
 public:
  Singleton() {}
  virtual ~Singleton() {}

  static T* GetInstance() {
    boost::call_once(Init, flag_);
    return instance_.get();
  }

 private:
  static void Init() {
    instance_.reset(new T());
  }

  static boost::scoped_ptr<T> instance_;
  static boost::once_flag flag_;
};

template <class T> boost::scoped_ptr<T> Singleton<T>::instance_;
template <class T> boost::once_flag Singleton<T>::flag_ = BOOST_ONCE_INIT;

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_BASE_MEMORY_SINGLETON_BOOST_H_
