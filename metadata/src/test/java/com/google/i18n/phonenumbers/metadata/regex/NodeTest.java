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

import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.regex.Node.INITIAL;
import static com.google.i18n.phonenumbers.metadata.regex.Node.TERMINAL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NodeTest {
  @Test
  public void testConstants() {
    assertThat(INITIAL.id()).isEqualTo(0);
    assertThat(TERMINAL.id()).isEqualTo(1);
    assertThat(TERMINAL).isNotEqualTo(INITIAL);
  }

  @Test
  public void testNext() {
    assertThat(INITIAL.createNext()).isSameInstanceAs(TERMINAL);
    assertThat(TERMINAL.createNext()).isNotEqualTo(TERMINAL);
    assertThat(TERMINAL.createNext().id()).isEqualTo(2);
    Node node = INITIAL;
    for (int id = 0; id < 10; id++) {
      assertThat(node.id()).isEqualTo(id);
      node = node.createNext();
    }
  }

  @Test
  public void testToString() {
    Node node = INITIAL;
    for (int id = 0; id < 10; id++) {
      assertThat(node.toString()).isEqualTo(Integer.toString(id));
      node = node.createNext();
    }
  }

  // Consistent ordering helps ensure regular expressions derived from graphs are deterministic.
  @Test
  public void testOrdering() {
    assertThat(TERMINAL).isGreaterThan(INITIAL);
    Node node = INITIAL;
    for (int id = 0; id < 10; id++) {
      Node next = node.createNext();
      assertThat(next).isGreaterThan(node);
      node = next;
    }
  }
}
