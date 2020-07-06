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
package com.google.i18n.phonenumbers.metadata.testing;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.PrefixTree;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import javax.annotation.Nullable;

/** A Truth subject for asserting on {@link RangeTree} instances. */
public class RangeTreeSubject extends Subject {

  public static RangeTreeSubject assertThat(@Nullable RangeTree tree) {
    return assertAbout(RangeTreeSubject.SUBJECT_FACTORY).that(tree);
  }

  public static RangeTreeSubject assertThat(@Nullable PrefixTree tree) {
    return assertAbout(RangeTreeSubject.SUBJECT_FACTORY).that(tree.asRangeTree());
  }

  public static RangeTreeSubject assertWithMessageThat(
      @Nullable RangeTree tree, String message, Object... args) {
    return assertWithMessage(message, args).about(
        RangeTreeSubject.SUBJECT_FACTORY).that(tree);
  }

  private static final Factory<RangeTreeSubject, RangeTree> SUBJECT_FACTORY =
      RangeTreeSubject::new;

  private final RangeTree actual;

  private RangeTreeSubject(FailureMetadata failureMetadata, @Nullable RangeTree subject) {
    super(failureMetadata, subject);
    this.actual = subject;
  }

  // Add more methods below as needed.

  public void isEmpty() {
    if (!actual.isEmpty()) {
      failWithActual(simpleFact("expected to be empty"));
    }
  }

  public void isNotEmpty() {
    if (actual.isEmpty()) {
      failWithActual(simpleFact("expected not to be empty"));
    }
  }

  public void hasSize(long size) {
    check("size()").withMessage("size").that(actual.size()).isEqualTo(size);
  }

  public void contains(String digits) {
    DigitSequence seq = digits.isEmpty() ? DigitSequence.empty() : DigitSequence.of(digits);
    if (!actual.contains(seq)) {
      failWithActual("expected to contain ", digits);
    }
  }

  public void doesNotContain(String digits) {
    DigitSequence seq = digits.isEmpty() ? DigitSequence.empty() : DigitSequence.of(digits);
    if (actual.contains(seq)) {
      failWithActual("expected not to contain", digits);
    }
  }

  public void containsExactly(RangeSpecification spec) {
    RangeTree tree = RangeTree.from(spec);
    if (!actual.equals(tree)) {
      failWithActual("expected to be equal to", spec);
    }
  }

  public void containsExactly(Iterable<RangeSpecification> specs) {
    RangeTree tree = RangeTree.from(specs);
    if (!actual.equals(tree)) {
      failWithActual("expected to be equal to", specs);
    }
  }

  public void containsExactly(String spec) {
    containsExactly(RangeSpecification.parse(spec));
  }

  public void containsExactly(String... specs) {
    containsExactly(FluentIterable.from(specs).transform(RangeSpecification::parse));
  }

  public void hasLengths(Integer... lengths) {
    check("getLengths()")
        .that(actual.getLengths())
        .containsExactlyElementsIn(ImmutableSet.copyOf(lengths));
  }
}
