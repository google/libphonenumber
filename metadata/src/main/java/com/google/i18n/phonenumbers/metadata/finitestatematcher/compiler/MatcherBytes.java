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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.compiler.MatcherCompiler.Sequence;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders the final bytecode representation for the matcher by connecting sequences of operations
 * together and fixing-up offsets and branch instructions. This is essentially the higher-level
 * aspect of matcher bytecode compilation.
 * <p>
 * Unlike {@link MatcherCompiler} in which a lot of the data is immutable (because sequences can
 * be defined in isolation), there's a lot of mutable state in this class due to the need to build
 * and manage offsets between the sequences, which relies on the order in which other sequences
 * have been rendered.
 */
class MatcherBytes {
  /**
   * A partial order on byte sequences based on their size. This is not "equivalent to equals" and
   * must not be used to construct an ordered set.
   */
  private static final Comparator<SequenceBytes> DECREASING_BY_SIZE =
      new Comparator<SequenceBytes>() {
        @Override public int compare(SequenceBytes lhs, SequenceBytes rhs) {
          return Integer.compare(rhs.size(), lhs.size());
        }
      };

  /**
   * Sequences we have not considered for rendering yet.
   */
  private final List<Sequence> remainingSequences;
  /**
   * Candidate sequences whose dependent sequences have all been rendered, and which may themselves
   * now be rendered.
   */
  private final Set<Sequence> canditiateSequences = new LinkedHashSet<>();
  /**
   * Sequences which have been rendered (used to determine when other sequences become renderable).
   */
  private final Set<Sequence> compiledSequences = new HashSet<>();
  /**
   * A map from which are final nodes of a sequence to the sequence they belong to. The key set of
   * this map is a subset of all nodes.
   */
  private final Map<DfaNode, SequenceBytes> sequenceMap = new HashMap<>();
  /**
   * A list of compiled byte sequences in reverse order (ie, the sequence with the terminal node
   * in it is first in this list and the sequence with the initial node is last). Compilation
   * occurs in reverse order to allow offsets between sequences to be calculated as we go.
   */
  private final List<SequenceBytes> reverseOrder = new ArrayList<>();
  /** Statistics instance for collecting inforation about the compilation. */
  private final Statistics stats;

  MatcherBytes(Iterable<Sequence> allSequences, Statistics stats) {
    // Our set of remaining sequences just starts out as all the sequences.
    // Sequences are processed in reverse order, so reverse the sorted sequences before beginning.
    remainingSequences = Lists.reverse(Lists.newArrayList(allSequences));
    this.stats = Preconditions.checkNotNull(stats);
  }

  /**
   * Compiles all sequences into a single byte buffer suitable for use by a
   * {@code DigitSequenceMatcher}.
   */
  byte[] compile() {
    int totalSequenceCount = remainingSequences.size();
    // Sequences with not dependent sequences are compiled first.
    compileFinalSequences();
    // Determine new candidate sequences.
    while (compiledSequences.size() < totalSequenceCount) {
      // We won't always add a new candidate sequence each time around the loop, but the set
      // should never be emptied until the final sequence is processed.
      for (Iterator<Sequence> it = remainingSequences.iterator(); it.hasNext();) {
        Sequence s = it.next();
        if (compiledSequences.containsAll(s.unorderedOutSequences())) {
          canditiateSequences.add(s);
          it.remove();
        }
      }
      // Compile the next candidate sequence.
      Sequence toCompile = Iterables.get(canditiateSequences, 0);
      reverseOrder.add(compile(toCompile));
      compiledSequences.add(toCompile);
      canditiateSequences.remove(toCompile);
    }
    // We should have always exhausted the candidate sequences when we've finished rendering.
    Preconditions.checkState(remainingSequences.isEmpty());
    Preconditions.checkState(canditiateSequences.isEmpty());
    return concatSequenceBytesInForwardOrder();
  }

  /**
   * Compiles any sequences which have no dependencies and orders them by size to heuristically
   * reduce the size of branch offsets needed to reach them.
   */
  private void compileFinalSequences() {
    for (Iterator<Sequence> it = remainingSequences.iterator(); it.hasNext();) {
      Sequence s = it.next();
      if (s.isFinal()) {
        reverseOrder.add(compile(s));
        compiledSequences.add(s);
        it.remove();
      }
    }
    // They are ordered by size (shortest first) because this will tend to reduce the number of
    // 2-byte branch instructions needed to jump to them.
    Collections.sort(reverseOrder, DECREASING_BY_SIZE);
  }

  /** Compiles a sequence for which all dependent sequences have already been compiled. */
  private SequenceBytes compile(Sequence sequence) {
    // Note: Even non branching sequences will have an out node here.
    Map<DfaNode, Integer> offsetMap = new HashMap<>();
    for (DfaNode out : sequence.getOutStates()) {
      SequenceBytes targetSequence = sequenceMap.get(out);
      int offsetToStartOfSequence = 0;
      for (int n = reverseOrder.size() - 1; n >= 0 && reverseOrder.get(n) != targetSequence; n--) {
        offsetToStartOfSequence += reverseOrder.get(n).size();
      }
      if (offsetToStartOfSequence > 0 && targetSequence.isTerminator()) {
        // If we would explicitly jump to a terminator sequence, we can just exit
        // unconditionally at this point.
        offsetToStartOfSequence = Operation.TERMINATION_OFFSET;
      }
      offsetMap.put(out, offsetToStartOfSequence);
    }
    SequenceBytes compiled = new SequenceBytes(sequence, offsetMap, stats);
    sequenceMap.put(sequence.getInitialState(), compiled);
    return compiled;
  }

  /** Creates the final, single buffer of bytecode instructions for the matcher. */
  private byte[] concatSequenceBytesInForwardOrder() {
    try {
      ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
      for (int n = reverseOrder.size() - 1; n >= 0; n--) {
        outBuffer.write(reverseOrder.get(n).getBytes());
      }
      return outBuffer.toByteArray();
    } catch (IOException e) {
      throw new AssertionError("ByteArrayOutputStream cannot throw IOException");
    }
  }

  /** Renders a sequence (along with a map of branch offsets) to its bytecode form. */
  private static byte[] renderSequence(
      Sequence sequence, Map<DfaNode, Integer> offsetMap, Statistics stats) {
    // Because our operations come from a sequence, we can assert that only the last operation
    // could possibly be branching.
    List<Operation> ops = sequence.createOps();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ByteArrayDataOutput outBytes = ByteStreams.newDataOutput(baos);
    // Write all but the last operation (there are no branches to worry about).
    for (int n = 0; n < ops.size() - 1; n++) {
      ops.get(n).writeTo(outBytes, null, stats);
    }
    Operation lastOp = Iterables.getLast(ops);
    if (lastOp.isTerminating()) {
      stats.record(Statistics.Type.TERMINATING);
    }
    if (lastOp.isBranching()) {
      // A branching operation uses the offset map directly to fill in its jump table information.
      lastOp.writeTo(outBytes, offsetMap, stats);
    } else {
      // A non-branching operation does not use offsets, but we may need to add an explicit branch
      // instruction after it.
      lastOp.writeTo(outBytes, null, stats);
      if (!offsetMap.isEmpty()) {
        // When adding a branch instruction, there should only be a single offset to use.
        int offset = Iterables.getOnlyElement(offsetMap.values());
        if (offset >= 0) {
          // The offset could still be zero, but this is handled correctly by writeBranch().
          Operation.writeBranch(outBytes, offset, stats);
        } else {
          // This is a terminal instruction and the matcher should exit.
          Preconditions.checkArgument(offset == Operation.TERMINATION_OFFSET);
          Operation.writeTerminator(outBytes, stats);
        }
      }
    }
    return baos.toByteArray();
  }

  /**
   * A single compiled sequence of operations. This is just a holder for a {@link Sequence} and the
   * compiled bytes it produces.
   */
  static class SequenceBytes {
    private final Sequence sequence;
    private final byte[] bytes;

    SequenceBytes(Sequence sequence, Map<DfaNode, Integer> offsetMap, Statistics stats) {
      this.sequence = sequence;
      this.bytes = renderSequence(sequence, offsetMap, stats);
    }

    Sequence getSequence() {
      return sequence;
    }

    boolean isTerminator() {
      return sequence.isFinal() && sequence.size() == 1;
    }

    int size() {
      return bytes.length;
    }

    byte[] getBytes() {
      return bytes;
    }
  }
}
