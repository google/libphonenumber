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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer.MergeStrategy.ALLOW_EDGE_SPLITTING;
import static com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer.MergeStrategy.REQUIRE_EQUAL_EDGES;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Preconditions;
import com.google.common.graph.ValueGraph;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer;
import com.google.i18n.phonenumbers.metadata.RangeTreeFactorizer.MergeStrategy;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import java.util.Optional;

/** Produces partially optimized regular expressions from {@code RangeTree}s. */
public final class RegexGenerator {
  private static final RegexGenerator BASIC = new RegexGenerator(false, false, false, false);

  // NOTE: Tail optimization should remain disabled since it seems to undo some of the benefits of
  // subgroup optimization. At some point the code can probably just be removed.
  private static final RegexGenerator DEFAULT_XML =
      BASIC.withDfaFactorization().withSubgroupOptimization();

  /**
   * Returns a basic regular expression generator with no optional optimizations enabled. This will
   * produce regular expressions with a simpler structure than other generators but output will
   * almost always be longer.
   */
  public static RegexGenerator basic() {
    return BASIC;
  }

  /**
   * Returns the default regex generator for XML data. This should be used by any tool wishing to
   * obtain the same regular expressions as the legacy XML data. It is deliberately not specified
   * as to which optimizations are enabled for this regular expression generator.
   */
  public static RegexGenerator defaultXmlGenerator() {
    return DEFAULT_XML;
  }

  /**
   * Returns a new regular expression generator which uses the {@code '.'} token for matching any
   * digit (rather than {@code '\d'}). This results in shorter output, but possibly at the cost of
   * performance on certain platforms (and a degree of readability).
   */
  public RegexGenerator withDotMatch() {
    Preconditions.checkState(!this.useDotMatch, "Dot-matching already enabled");
    return new RegexGenerator(true, this.factorizeDfa, this.optimizeSubgroups, this.optimizeTail);
  }

  /**
   * Returns a new regular expression generator which applies a length-based factorization of the
   * DFA graph in an attempt to reduce the number of problematic terminating states. This results
   * in regular expressions with additional non-determinism, but which can greatly reduce size.
   */
  public RegexGenerator withDfaFactorization() {
    Preconditions.checkState(!this.factorizeDfa, "Length based factorizing already enabled");
    return new RegexGenerator(this.useDotMatch, true, this.optimizeSubgroups, this.optimizeTail);
  }

  /**
   * Returns a new regular expression generator which applies experimental factorization of the
   * DFA graph in an attempt to identify and handle subgroups which would cause repetition. This
   * results in regular expressions with additional non-determinism, but which can greatly reduce
   * size.
   */
  public RegexGenerator withSubgroupOptimization() {
    Preconditions.checkState(!this.optimizeSubgroups, "Subgroup optimization already enabled");
    return new RegexGenerator(this.useDotMatch, this.factorizeDfa, true, this.optimizeTail);
  }

  /**
   * Returns a new regular expression generator which applies tail optimization to the intermediate
   * NFA graph to factor out common trailing paths. This results in a small size improvement to
   * many cases and does not adversely affect readability.
   */
  public RegexGenerator withTailOptimization() {
    Preconditions.checkState(!this.optimizeTail, "Tail optimization already enabled");
    return new RegexGenerator(this.useDotMatch, this.factorizeDfa, this.optimizeSubgroups, true);
  }

  private final boolean useDotMatch;
  private final boolean factorizeDfa;
  private final boolean optimizeSubgroups;
  private final boolean optimizeTail;

  private RegexGenerator(
      boolean useDotMatch, boolean factorizeDfa, boolean optimizeSubgroups, boolean optimizeTail) {
    this.useDotMatch = useDotMatch;
    this.factorizeDfa = factorizeDfa;
    this.optimizeSubgroups = optimizeSubgroups;
    this.optimizeTail = optimizeTail;
  }

  /**
   * Generates a regular expression from a range tree, applying the configured options for this
   * generator.
   */
  public String toRegex(RangeTree ranges) {
    // The regex of the empty range is "a regex that matches nothing". This is meaningless.
    checkArgument(!ranges.isEmpty(),
        "cannot generate regular expression from empty ranges");
    // We cannot generate any regular expressions if there are no explicit state transitions in the
    // graph (i.e. we can generate "(?:<re>)?" but only if "<re>" is non-empty). We just get
    // "the regex that always immediately terminates after no input". This is also meaningless.
    checkArgument(!ranges.getInitial().equals(RangeTree.getTerminal()),
        "range tree must not contain only the empty digit sequence: %s", ranges);

    String regex = generateFactorizedRegex(ranges);
    if (optimizeSubgroups) {
      regex = recursivelyOptimizeSubgroups(ranges, regex);
    }
    return regex;
  }

  private String recursivelyOptimizeSubgroups(RangeTree ranges, String regex) {
    Optional<RangeTree> subgraphRanges = SubgroupOptimizer.extractRepeatingSubgraph(ranges);
    if (subgraphRanges.isPresent()) {
      RangeTree leftoverRanges = ranges.subtract(subgraphRanges.get());
      String leftoverRegex = generateFactorizedRegex(leftoverRanges);
      leftoverRegex = recursivelyOptimizeSubgroups(leftoverRanges, leftoverRegex);
      String optimizedRegex = leftoverRegex + "|" + generateFactorizedRegex(subgraphRanges.get());
      if (optimizedRegex.length() < regex.length()) {
        regex = optimizedRegex;
      }
    }
    return regex;
  }

  private String generateFactorizedRegex(RangeTree ranges) {
    String regex = regexOf(ranges);
    if (factorizeDfa) {
      regex = generateFactorizedRegex(ranges, regex, REQUIRE_EQUAL_EDGES);
      regex = generateFactorizedRegex(ranges, regex, ALLOW_EDGE_SPLITTING);
    }
    return regex;
  }

  private String generateFactorizedRegex(RangeTree dfa, String bestRegex, MergeStrategy strategy) {
    String factoredRegex = RangeTreeFactorizer.factor(dfa, strategy).stream()
        .map(this::regexOf)
        .collect(joining("|"));
    return factoredRegex.length() < bestRegex.length() ? factoredRegex : bestRegex;
  }

  private String regexOf(RangeTree ranges) {
    ValueGraph<Node, SimpleEdge> nfa = RangeTreeConverter.toNfaGraph(ranges);
    if (optimizeTail) {
      nfa = TrailingPathOptimizer.optimize(nfa);
    }
    return EdgeWriter.toRegex(NfaFlattener.flatten(nfa), useDotMatch);
  }
}
