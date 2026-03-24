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

package com.google.i18n.phonenumbers.metadata.regex;

import com.google.auto.value.AutoValue;

/**
 * Value type for nodes in NFA graphs of phone number regular expressions. This is basically a
 * trivial wrapper for an {@code int}, but it makes a lot of other pieces of code type safe.
 * Outside this package, this type is mainly used for examining NFA graphs which represent a
 * regular expression, generated via {@link RangeTreeConverter#toNfaGraph}.
 */
@AutoValue
public abstract class Node implements Comparable<Node> {
  /** The unique initial node in an NFA graph with in-order zero. */
  public static final Node INITIAL = new AutoValue_Node(0);
  /** The unique terminal node in an NFA graph with out-order zero. */
  public static final Node TERMINAL = new AutoValue_Node(1);

  /** Returns a new node whose ID is one greater than this node. */
  public Node createNext() {
    return (id() == 0) ? TERMINAL : new AutoValue_Node(id() + 1);
  }

  /** Returns the numeric ID of this node, which must be unique within an NFA graph. */
  abstract int id();

  @Override
  public int compareTo(Node o) {
    return Integer.compare(id(), o.id());
  }

  @Override
  public final String toString() {
    return Integer.toString(id());
  }
}
