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

// Author: Philippe Liard

// This file does not come from Chromium.
// It provides a minimalist implementation of common macros.

#ifndef BASE_LOGGING_H_
# define BASE_LOGGING_H_

# include <cassert>
# include <iostream>

# define CHECK_EQ(X, Y) assert((X) == (Y))

# define DCHECK(X) assert(X)
# define DCHECK_EQ(X, Y) CHECK_EQ((X), (Y))

# define NOTREACHED() std::cerr
# define LOG(Level) std::cerr

# define FATAL 1

#endif
