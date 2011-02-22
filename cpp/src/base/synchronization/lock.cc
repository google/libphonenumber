// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This file is used for debugging assertion support.  The Lock class
// is functionally a wrapper around the LockImpl class, so the only
// real intelligence in the class is in the debugging logic.

#if !defined(NDEBUG)

#include "base/synchronization/lock.h"
#include "base/logging.h"

namespace base {

Lock::Lock() : lock_() {
  owned_by_thread_ = false;
  owning_thread_id_ = static_cast<PlatformThreadId>(0);
}

void Lock::AssertAcquired() const {
  DCHECK(owned_by_thread_);
  DCHECK_EQ(owning_thread_id_, PlatformThread::CurrentId());
}

void Lock::CheckHeldAndUnmark() {
  DCHECK(owned_by_thread_);
  DCHECK_EQ(owning_thread_id_, PlatformThread::CurrentId());
  owned_by_thread_ = false;
  owning_thread_id_ = static_cast<PlatformThreadId>(0);
}

void Lock::CheckUnheldAndMark() {
  DCHECK(!owned_by_thread_);
  owned_by_thread_ = true;
  owning_thread_id_ = PlatformThread::CurrentId();
}

}  // namespace base

#endif  // NDEBUG
