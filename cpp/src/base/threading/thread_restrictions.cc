// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "base/threading/thread_restrictions.h"

// This entire file is compiled out in Release mode.
#ifndef NDEBUG

#include "base/lazy_instance.h"
#include "base/logging.h"
#include "base/threading/thread_local.h"

namespace base {

namespace {

LazyInstance<ThreadLocalBoolean, LeakyLazyInstanceTraits<ThreadLocalBoolean> >
    g_io_disallowed(LINKER_INITIALIZED);

LazyInstance<ThreadLocalBoolean, LeakyLazyInstanceTraits<ThreadLocalBoolean> >
    g_singleton_disallowed(LINKER_INITIALIZED);

}  // anonymous namespace

// static
bool ThreadRestrictions::SetIOAllowed(bool allowed) {
  bool previous_disallowed = g_io_disallowed.Get().Get();
  g_io_disallowed.Get().Set(!allowed);
  return !previous_disallowed;
}

// static
void ThreadRestrictions::AssertIOAllowed() {
  if (g_io_disallowed.Get().Get()) {
    LOG(FATAL) <<
        "Function marked as IO-only was called from a thread that "
        "disallows IO!  If this thread really should be allowed to "
        "make IO calls, adjust the call to "
        "base::ThreadRestrictions::SetIOAllowed() in this thread's "
        "startup.";
  }
}

bool ThreadRestrictions::SetSingletonAllowed(bool allowed) {
  bool previous_disallowed = g_singleton_disallowed.Get().Get();
  g_singleton_disallowed.Get().Set(!allowed);
  return !previous_disallowed;
}

// static
void ThreadRestrictions::AssertSingletonAllowed() {
  if (g_singleton_disallowed.Get().Get()) {
    LOG(FATAL) << "LazyInstance/Singleton is not allowed to be used on this "
               << "thread.  Most likely it's because this thread is not "
               << "joinable, so AtExitManager may have deleted the object "
               << "on shutdown, leading to a potential shutdown crash.";
  }
}

}  // namespace base

#endif  // NDEBUG
