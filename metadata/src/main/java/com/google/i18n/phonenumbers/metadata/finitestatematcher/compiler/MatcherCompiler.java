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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.Integer.numberOfTrailingZeros;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaEdge;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaVisitor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Compiles non-capturing phone number regular expressions into sequences of bytes suitable for
 * creating {@link com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher
 * DigitSequenceMatcher} instances.
 */
public final class MatcherCompiler {
  /**
   * Compiles the given {@code RangeTree} into a sequence of bytes suitable for creating a
   * {@link com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher
   * DigitSequenceMatcher}.
   */
  public static byte[] compile(RangeTree dfa) {
    return compile(dfa, Statistics.NO_OP);
  }

  /**
   * As {@link #compile(RangeTree)} but additionally accepts a {@link Statistics} instance
   * to record metrics about the compilation.
   */
  public static byte[] compile(RangeTree dfa, Statistics stats) {
    return new MatcherCompiler(dfa).compile(stats);
  }

  /** The DFA from which the matcher data is to be compiled. */
  private final ValueGraph<DfaNode, DfaEdge> dfa;
  /** The unique initial node of the DFA. */
  private final DfaNode init;
  /**
   * A map from nodes which are at the beginning of a sequence to that sequence. Not all nodes
   * will be present in the key set of this map.
   */
  private final ImmutableMap<DfaNode, Sequence> seqStart;

  /**
   * Builds a graph directly from the DFA in a RangeTree.
   *
   * <p>Rather than deal with the DFA tree directly (which is deliberately opaque as a data
   * structure) we serialize it into a more maleable ValueGraph. This allows simpler graph
   * traversal while maintaining a simple-as-possible node/edge structure. It's okay to reuse the
   * RangeTree types {@code DfaNode} and {@code DfaEdge} here because they have the expected
   * semantics (e.g. conforming to equals/hashcode etc...) but care must be taken not to keep the
   * instances around for a long time, since this will keep larger parts of the original DFA alive
   * in the garbage collector (but this is fine since only bytes are returned from this class).
   */
  private static ValueGraph<DfaNode, DfaEdge> buildGraph(RangeTree dfa) {
    Preconditions.checkArgument(!dfa.isEmpty());
    MutableValueGraph<DfaNode, DfaEdge> graph =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    graph.addNode(dfa.getInitial());
    DfaVisitor visitor = new DfaVisitor() {
      @Override
      public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
        boolean isFirstVisit = graph.addNode(target);
        graph.putEdgeValue(source, target, edge);
        if (isFirstVisit) {
          target.accept(this);
        }
      }
    };
    dfa.accept(visitor);
    return graph;
  }

  /**
   * Creates a {@code MatcherCompiler} from the given automaton by generating all the
   * {@code Sequence}'s of operations necessary to represent it.
   */
  MatcherCompiler(RangeTree ranges) {
    this.dfa = buildGraph(ranges);
    this.init = ranges.getInitial();
    LinkedHashMap<DfaNode, Sequence> start = new LinkedHashMap<>();
    buildSequencesFrom(init, start);
    this.seqStart = ImmutableMap.copyOf(start);
  }

  /**
   * Returns the output targets of the given node sorted according to the lowest "accepting" digit
   * on the corresponding edge. This ordering is necessary for stability, but also correctness when
   * building mapping operations. Apart from special cases (e.g. only one output) this is the only
   * method which should be used to obtain output nodes.
   */
  private ImmutableSet<DfaNode> sortedOutputs(DfaNode source) {
    Comparator<DfaNode> ordering = Comparator.comparing(
        target -> numberOfTrailingZeros(dfa.edgeValue(source, target).get().getDigitMask()));
    return dfa.successors(source).stream().sorted(ordering).collect(toImmutableSet());
  }

  /** Returns the single output target of the given node (or throws an exception). */
  private DfaNode singleOutput(DfaNode source) {
    return Iterables.getOnlyElement(dfa.successors(source));
  }

  /**
   * Builds the output map from a given node in the DFA in the correct order. Note that because
   * ImmutableSetMultimap.Builder orders keys based on the first time they are added, and we add
   * keys (nodes) in the order of the input by which they can be reached, the keys of the returned
   * map are ordered by the lowest digit in their set of values (inputs). This is necessary for
   * correct behaviour in the "Mapping" operation.
   */
  private ImmutableMap<DfaNode, Integer> getOutMap(DfaNode source) {
    Function<DfaNode, Integer> getMask =
        target -> dfa.edgeValue(source, target).get().getDigitMask();
    return sortedOutputs(source).stream().collect(toImmutableMap(Function.identity(), getMask));
  }

  /**
   * Recursively builds sequences by traversing the DFA and grouping successive sub-sequences of
   * nodes which neither branch, nor are branched to. Each such sub-sequence is represented by a
   * {@code Sequence} instance (a list of non-branching operations, optionally terminated with a
   * branching operation).
   */
  private void buildSequencesFrom(DfaNode start, LinkedHashMap<DfaNode, Sequence> map) {
    if (map.containsKey(start)) {
      return;
    }
    DfaNode current = start;
    ImmutableList.Builder<DfaNode> nodes = ImmutableList.builder();
    while (true) {
      nodes.add(current);
      if (dfa.outDegree(current) != 1) {
        break;
      }
      DfaNode next = singleOutput(current);
      if (dfa.inDegree(next) > 1) {
        break;
      }
      current = next;
    }
    Sequence seq = new Sequence(nodes.build());
    map.put(start, seq);
    // Recurse from the outputs at the end of the sequence according to their edge values.
    // IMPORTANT: We must not use "current.successors()" here since we need the order of insertion
    // to be well defined and ValueGraph does not make good enough promises about node ordering.
    for (DfaNode out : sortedOutputs(current)) {
      buildSequencesFrom(out, map);
    }
  }

  /** Creates and compiles a {@code MatcherBytes} instance to render the output bytes. */
  byte[] compile(Statistics stats) {
    return createMatcherBytes(stats).compile();
  }

  /** Creates a mutable {@code MatcherBytes} instance which will render the output bytes. */
  MatcherBytes createMatcherBytes(Statistics stats) {
    return new MatcherBytes(seqStart.values(), stats);
  }

  /**
   * A contiguous sub-sequence of nodes in the DFA which neither branch, nor are branched to.
   * <p>
   * The important property of a {@code Sequence} is that branching may only occur at the end of a
   * {@code Sequence} and branches may only jump to the start of another {@code Sequence}. This
   * makes it easier to separate the compilation of operations (inside sequences) from the
   * management of branches and offsets (between sequences).
   */
  class Sequence {
    private final ImmutableList<DfaNode> nodes;

    Sequence(ImmutableList<DfaNode> nodes) {
      checkArgument(!nodes.isEmpty());
      this.nodes = nodes;
    }

    private Operation getOp(DfaNode node) {
      return Operation.from(node.canTerminate(), getOutMap(node));
    }

    /**
     * Returns the operations representing this sequence, merging successive operations where
     * possible. The final list of operations is guaranteed to have at most one branching operation
     * which (if present) will always be the last element in the list.
     */
    List<Operation> createOps() {
      List<Operation> ops = new ArrayList<>();
      Operation current = getOp(nodes.get(0));
      for (int n = 1; n < nodes.size(); n++) {
        Operation next = getOp(nodes.get(n));
        Operation merged = current.mergeWith(next);
        if (merged != null) {
          current = merged;
        } else {
          ops.add(current);
          current = next;
        }
      }
      ops.add(current);
      return ops;
    }

    DfaNode getInitialState() {
      return Iterables.get(nodes, 0);
    }

    DfaNode getFinalState() {
      return Iterables.getLast(nodes);
    }

    Set<DfaNode> getOutStates() {
      return sortedOutputs(getFinalState());
    }

    /**
     * Not the same as "terminating" for an operation. A sequence is "final" if no other sequences
     * follow it. Normally there is only one final sequence in a normalized DFA, even if that
     * sequence contains only a single terminating node. However not all terminating nodes are
     * in final sequences.
     */
    boolean isFinal() {
      return getOutStates().isEmpty();
    }

    /** Returns the number of nodes that this sequence represents. */
    int size() {
      return nodes.size();
    }

    ImmutableSet<Sequence> unorderedOutSequences() {
      return getOutStates().stream().map(seqStart::get).collect(toImmutableSet());
    }

    @Override
    public String toString() {
      return toString(new StringBuilder(), 0).toString();
    }

    private StringBuilder toString(StringBuilder buf, int indent) {
      List<Operation> ops = createOps();
      appendIndent(buf, indent).append(
          String.format("{%s} %s", nodes.get(0), Joiner.on(" >> ").join(ops)));
      ImmutableList<DfaNode> outs = Iterables.getLast(ops).getOuts();
      if (!outs.isEmpty()) {
        buf.append(" {\n");
        for (DfaNode out : outs) {
          seqStart.get(out).toString(buf, indent + 1);
        }
        appendIndent(buf, indent).append("}\n");
      } else {
        buf.append('\n');
      }
      return buf;
    }
  }

  @Override
  public String toString() {
    return seqStart.get(init).toString();
  }

  private static StringBuilder appendIndent(StringBuilder out, int indent) {
    for (int n = 0; n < indent; n++) {
      out.append("  ");
    }
    return out;
  }
}
