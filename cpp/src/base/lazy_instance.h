// Copyright (c) 2008 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// The LazyInstance<Type, Traits> class manages a single instance of Type,
// which will be lazily created on the first time it's accessed.  This class is
// useful for places you would normally use a function-level static, but you
// need to have guaranteed thread-safety.  The Type constructor will only ever
// be called once, even if two threads are racing to create the object.  Get()
// and Pointer() will always return the same, completely initialized instance.
// When the instance is constructed it is registered with AtExitManager.  The
// destructor will be called on program exit.
//
// LazyInstance is completely thread safe, assuming that you create it safely.
// The class was designed to be POD initialized, so it shouldn't require a
// static constructor.  It really only makes sense to declare a LazyInstance as
// a global variable using the base::LinkerInitialized constructor.
//
// LazyInstance is similar to Singleton, except it does not have the singleton
// property.  You can have multiple LazyInstance's of the same type, and each
// will manage a unique instance.  It also preallocates the space for Type, as
// to avoid allocating the Type instance on the heap.  This may help with the
// performance of creating the instance, and reducing heap fragmentation.  This
// requires that Type be a complete type so we can determine the size.
//
// Example usage:
//   static LazyInstance<MyClass> my_instance(base::LINKER_INITIALIZED);
//   void SomeMethod() {
//     my_instance.Get().SomeMethod();  // MyClass::SomeMethod()
//
//     MyClass* ptr = my_instance.Pointer();
//     ptr->DoDoDo();  // MyClass::DoDoDo
//   }

#ifndef BASE_LAZY_INSTANCE_H_
#define BASE_LAZY_INSTANCE_H_
#pragma once

#include <new>  // For placement new.

#include "base/atomicops.h"
#include "base/basictypes.h"
#include "base/third_party/dynamic_annotations/dynamic_annotations.h"
#include "base/threading/thread_restrictions.h"

namespace base {

template <typename Type>
struct DefaultLazyInstanceTraits {
  static const bool kAllowedToAccessOnNonjoinableThread = false;

  static Type* New(void* instance) {
    // Use placement new to initialize our instance in our preallocated space.
    // The parenthesis is very important here to force POD type initialization.
    return new (instance) Type();
  }
  static void Delete(void* instance) {
    // Explicitly call the destructor.
    reinterpret_cast<Type*>(instance)->~Type();
  }
};

template <typename Type>
struct LeakyLazyInstanceTraits {
  static const bool kAllowedToAccessOnNonjoinableThread = true;

  static Type* New(void* instance) {
    return DefaultLazyInstanceTraits<Type>::New(instance);
  }
  // Rather than define an empty Delete function, we make Delete itself
  // a null pointer.  This allows us to completely sidestep registering
  // this object with an AtExitManager, which allows you to use
  // LeakyLazyInstanceTraits in contexts where you don't have an
  // AtExitManager.
  static void (*Delete)(void* instance);
};

template <typename Type>
void (*LeakyLazyInstanceTraits<Type>::Delete)(void* instance) = NULL;

// We pull out some of the functionality into a non-templated base, so that we
// can implement the more complicated pieces out of line in the .cc file.
class LazyInstanceHelper {
 protected:
  enum {
    STATE_EMPTY    = 0,
    STATE_CREATING = 1,
    STATE_CREATED  = 2
  };

  explicit LazyInstanceHelper(LinkerInitialized /*unused*/) {/* state_ is 0 */}
  // Declaring a destructor (even if it's empty) will cause MSVC to register a
  // static initializer to register the empty destructor with atexit().

  // Check if instance needs to be created. If so return true otherwise
  // if another thread has beat us, wait for instance to be created and
  // return false.
  bool NeedsInstance();

  // After creating an instance, call this to register the dtor to be called
  // at program exit and to update the state to STATE_CREATED.
  void CompleteInstance(void* instance, void (*dtor)(void*));

  base::subtle::Atomic32 state_;

 private:
  DISALLOW_COPY_AND_ASSIGN(LazyInstanceHelper);
};

template <typename Type, typename Traits = DefaultLazyInstanceTraits<Type> >
class LazyInstance : public LazyInstanceHelper {
 public:
  explicit LazyInstance(LinkerInitialized x) : LazyInstanceHelper(x) { }
  // Declaring a destructor (even if it's empty) will cause MSVC to register a
  // static initializer to register the empty destructor with atexit().

  Type& Get() {
    return *Pointer();
  }

  Type* Pointer() {
    if (!Traits::kAllowedToAccessOnNonjoinableThread)
      base::ThreadRestrictions::AssertSingletonAllowed();

    // We will hopefully have fast access when the instance is already created.
    if ((base::subtle::NoBarrier_Load(&state_) != STATE_CREATED) &&
        NeedsInstance()) {
      // Create the instance in the space provided by |buf_|.
      instance_ = Traits::New(buf_);
      // Traits::Delete will be null for LeakyLazyInstanceTraits
      void (*dtor)(void*) = Traits::Delete;
      CompleteInstance(this, (dtor == NULL) ? NULL : OnExit);
    }

    // This annotation helps race detectors recognize correct lock-less
    // synchronization between different threads calling Pointer().
    // We suggest dynamic race detection tool that "Traits::New" above
    // and CompleteInstance(...) happens before "return instance_" below.
    // See the corresponding HAPPENS_BEFORE in CompleteInstance(...).
    ANNOTATE_HAPPENS_AFTER(&state_);
    return instance_;
  }

  bool operator==(Type* p) {
    switch (base::subtle::NoBarrier_Load(&state_)) {
      case STATE_EMPTY:
        return p == NULL;
      case STATE_CREATING:
        return static_cast<int8*>(static_cast<void*>(p)) == buf_;
      case STATE_CREATED:
        return p == instance_;
      default:
        return false;
    }
  }

 private:
  // Adapter function for use with AtExit.  This should be called single
  // threaded, so don't use atomic operations.
  // Calling OnExit while the instance is in use by other threads is a mistake.
  static void OnExit(void* lazy_instance) {
    LazyInstance<Type, Traits>* me =
        reinterpret_cast<LazyInstance<Type, Traits>*>(lazy_instance);
    Traits::Delete(me->instance_);
    me->instance_ = NULL;
    base::subtle::Release_Store(&me->state_, STATE_EMPTY);
  }

  int8 buf_[sizeof(Type)];  // Preallocate the space for the Type instance.
  Type *instance_;

  DISALLOW_COPY_AND_ASSIGN(LazyInstance);
};

}  // namespace base

#endif  // BASE_LAZY_INSTANCE_H_
