/*
 * Copyright (C) 2017 The Libphonenumber Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.i18n.phonenumbers.metadata.finitestatematcher.compiler;


/**
 * A simple class for capturing statistics produced during regular expression compilation. This can
 * be used to quantify how proposed changes to the byte-code definition will affect the size of any
 * compiled matcher bytes.
 */
public interface Statistics {

  public static final Statistics NO_OP = new Statistics() {
    @Override public void record(Type type) { }
  };

  /** The type of things we are counting. */
  public enum Type {
    SHORT_BRANCH,
    MEDIUM_BRANCH,
    LONG_BRANCH,
    DOUBLE_JUMP,
    CONTINUATION,
    TERMINATING,
    FINAL;
  }

  /** Records an operation of the specified type during bytecode compilation. */
  void record(Type type);
}
