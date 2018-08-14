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

#ifndef I18N_PHONENUMBERS_STL_UTIL_H_
#define I18N_PHONENUMBERS_STL_UTIL_H_

namespace i18n {
namespace phonenumbers {

namespace gtl {
// Compares the first attribute of two pairs.
struct OrderByFirst {
  template <typename T>
  bool operator()(const T& p1, const T& p2) const {
    return p1.first < p2.first;
  }
};

// Deletes the second attribute (pointer type expected) of the pairs contained
// in the provided range.
template <typename ForwardIterator>
void STLDeleteContainerPairSecondPointers(const ForwardIterator& begin,
                                          const ForwardIterator& end) {
  for (ForwardIterator it = begin; it != end; ++it) {
    delete it->second;
  }
}

// Deletes the pointers contained in the provided container.
template <typename T>
void STLDeleteElements(T* container) {
  for (typename T::iterator it = container->begin(); it != container->end();
       ++it) {
    delete *it;
  }
}
}  // namespace gtl
}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_STL_UTIL_H_
