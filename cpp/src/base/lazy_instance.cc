// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "base/lazy_instance.h"

#include "base/at_exit.h"
#include "base/atomicops.h"
#include "base/basictypes.h"
#include "base/threading/platform_thread.h"
#include "base/third_party/dynamic_annotations/dynamic_annotations.h"

namespace base {

bool LazyInstanceHelper::NeedsInstance() {
  // Try to create the instance, if we're the first, will go from EMPTY
  // to CREATING, otherwise we've already been beaten here.
  if (base::subtle::Acquire_CompareAndSwap(
          &state_, STATE_EMPTY, STATE_CREATING) == STATE_EMPTY) {
    // Caller must create instance
    return true;
  } else {
    // It's either in the process of being created, or already created.  Spin.
    while (base::subtle::NoBarrier_Load(&state_) != STATE_CREATED)
      PlatformThread::YieldCurrentThread();
  }

  // Someone else created the instance.
  return false;
}

void LazyInstanceHelper::CompleteInstance(void* instance, void (*dtor)(void*)) {
  // See the comment to the corresponding HAPPENS_AFTER in Pointer().
  ANNOTATE_HAPPENS_BEFORE(&state_);

  // Instance is created, go from CREATING to CREATED.
  base::subtle::Release_Store(&state_, STATE_CREATED);

  // Make sure that the lazily instantiated object will get destroyed at exit.
  if (dtor)
    base::AtExitManager::RegisterCallback(dtor, instance);
}

}  // namespace base
