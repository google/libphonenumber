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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSetMultimap.flatteningToImmutableSetMultimap;
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;
import static java.lang.Integer.numberOfTrailingZeros;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.OpCode;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.compiler.Statistics.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A specific instance of a number matching operation derived from a DFA. Operations are created by
 * analyzing a sequence in a DFA and knowing how to write the corresponding instruction(s) as bytes
 * (to be processed by DigitSequenceMatcher or similar).
 */
abstract class Operation {
  /** Represents the digits which can be accepted during matching operations. */
  private enum Digit {
    // Order of enums must match the digit value itself (this is checked for in the constructor).
    ZERO(0), ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9);

    private static final Digit[] VALUES = values();

    // Iteration order is order of enum declaration (and thus also the value order).
    public static final ImmutableSet<Digit> ALL = ImmutableSet.copyOf(VALUES);

    Digit(int value) {
      // No need to store the digit value if we know it matches our ordinal value.
      Preconditions.checkArgument(value == ordinal());
    }

    /** Returns the digit corresponding to the integral value in the range {@code 0...9}. */
    public static Digit of(int n) {
      return VALUES[n];
    }

    /**
     * Returns the set of digits corresponding to a bit-mask in which bits 0 to 9 represent the
     * corresponding digits.
     */
    public static ImmutableSet<Digit> fromMask(int mask) {
      Preconditions.checkArgument(mask >= 1 && mask <= ALL_DIGITS_MASK);
      if (mask == ALL_DIGITS_MASK) {
        return ALL;
      }
      ImmutableSet.Builder<Digit> digits = ImmutableSet.builder();
      for (int n = 0; n <= 9; n++) {
        if ((mask & (1 << n)) != 0) {
          digits.add(VALUES[n]);
        }
      }
      return digits.build();
    }

    /** Returns the integer value of this digit instance. */
    public int value() {
      return ordinal();
    }
  }

  /**
   * An invalid jump offset indicating that instead of jumping to a new instruction, the state
   * machine can just terminate (used to avoid jumping directly to the termination instruction).
   */
  static final int TERMINATION_OFFSET = -1;

  /** The number of bytes required by a "long" branch instruction. */
  private static final int LONG_BRANCH_SIZE = 2;

  private final boolean isTerminating;
  private final boolean isBranching;

  private Operation(boolean isTerminating, boolean isBranching) {
    this.isTerminating = isTerminating;
    this.isBranching = isBranching;
  }

  /** Returns whether this operation can terminate the state machine when it has been reached. */
  boolean isTerminating() {
    return isTerminating;
  }

  /**
   * Returns whether this operation is branching. A branching operation has more than one output
   * node it can reach.
   */
  boolean isBranching() {
    return isBranching;
  }

  /**
   * Returns the output nodes of this operation. For branching operations the order of multiple
   * output nodes is defined by the operation itself (most operations are not branching and have
   * only one output state anyway).
   */
  abstract ImmutableList<DfaNode> getOuts();

  /** Returns the op-code for this operation, used when writing out instruction bytes. */
  abstract OpCode getOpCode();

  /** Writes this operation out as a series of instruction bytes. */
  abstract void writeImpl(
      ByteArrayDataOutput out, Map<DfaNode, Integer> offsetMap, Statistics stats);

  void writeTo(ByteArrayDataOutput out, Map<DfaNode, Integer> offsetMap, Statistics stats) {
    if (isTerminating()) {
      stats.record(Type.TERMINATING);
    }
    writeImpl(out, offsetMap, stats);
  }

  /**
   * Merges two adjacent operations (a poor man's compiler optimization). Useful for collapsing
   * sequences of "ANY" operations. If this instruction cannot be merged with the given "next"
   * instruction then it should return {@code null}, which is the default behavior.
   *
   * @param next the operation following this operation which we will try and merge with.
   */
  Operation mergeWith(Operation next) {
    return null;
  }

  /** Writes a branch instructions into the output byte sequence. */
  static void writeBranch(ByteArrayDataOutput out, int jump, Statistics stats) {
    Preconditions.checkArgument(jump >= 0 && jump < 0x1000, "invalid jump: " + jump);
    if (jump == 0) {
      stats.record(Type.CONTINUATION);
    } else if (jump < 16) {
      stats.record(Type.SHORT_BRANCH);
      out.writeByte((OpCode.BRANCH.ordinal() << 5) | jump);
    } else {
      stats.record(jump < 0x100 ? Type.MEDIUM_BRANCH : Type.LONG_BRANCH);
      out.writeShort((OpCode.BRANCH.ordinal() << 13) | (1 << 12) | jump);
    }
  }

  /** Writes a termination byte into the output byte sequence. */
  static void writeTerminator(ByteArrayDataOutput out, Statistics stats) {
    stats.record(Type.FINAL);
    out.writeByte(0);
  }

  /**
   * Creates a new operation to represent the output state transition given by {@code outMasks}.
   * Note that where multiple nodes exist in {@code outMasks}, their ordering must be consistent
   * with the {@code Mapping} operation (whereby nodes are ordered by the lowest bit set in the
   * corresponding mask.
   */
  static Operation from(boolean isTerminating, ImmutableMap<DfaNode, Integer> outMasks) {
    if (outMasks.isEmpty()) {
      // No out nodes; then it's a "Terminal" operation.
      Preconditions.checkState(isTerminating);
      return new Operation.Terminal();
    }
    ImmutableList<DfaNode> outStates = outMasks.keySet().asList();
    if (outStates.size() == 1) {
      DfaNode outState = Iterables.getOnlyElement(outStates);
      int digitMask = outMasks.get(outState);
      if (Integer.bitCount(digitMask) == 1) {
        // One output state reached by a single input; then it's a "Single" operation.
        return new Operation.Single(isTerminating, numberOfTrailingZeros(digitMask), outStates);
      }
      if (digitMask == ALL_DIGITS_MASK) {
        // One output state reached by any input; then it's an "Any" operation.
        return new Operation.Any(isTerminating, 1, outStates);
      }
      // One output state reached other general input; then it's a "Range" operation.
      return new Operation.Range(isTerminating, digitMask, outStates);
    }
    if (outStates.size() == 2) {
      // Test if the 2 disjoint masks cover all inputs. If so, use a shorter branch operation.
      List<Integer> masks = outMasks.values().asList();
      if ((masks.get(0) | masks.get(1)) == ALL_DIGITS_MASK) {
        // One of two output nodes reached by any input; then it's a branching "Range" operation.
        return new Operation.Range(isTerminating, masks.get(0), outStates);
      }
    }
    // Any other combination of nodes or inputs; then it's a "Mapping" operation. This code relies
    // on the ordering of entries in the output map to correspond to edge order.
    return new Operation.Mapping(isTerminating, outMasks);
  }

  /** Respresents a state with no legal outputs, which must be a terminal state in the matcher. */
  private static final class Terminal extends Operation {
    Terminal() {
      super(true, true);
    }

    @Override
    OpCode getOpCode() {
      return OpCode.BRANCH;
    }

    @Override
    ImmutableList<DfaNode> getOuts() {
      return ImmutableList.of();
    }

    @Override
    void writeImpl(ByteArrayDataOutput out, Map<DfaNode, Integer> unused, Statistics stats) {
      writeTerminator(out, stats);
    }

    @Override
    public String toString() {
      return "TERMINAL";
    }
  }

  /**
   * Respresents a state which can be transitioned from to a single output state via a single input
   * (eg, "0" or "9").
   */
  private static final class Single extends Operation {
    private final Digit digit;
    private final ImmutableList<DfaNode> outs;

    Single(boolean isTerminating, int digit, ImmutableList<DfaNode> outs) {
      super(isTerminating, false);
      Preconditions.checkArgument(outs.size() == 1);
      this.digit = Digit.of(digit);
      this.outs = outs;
    }

    @Override
    OpCode getOpCode() {
      return OpCode.SINGLE;
    }

    @Override ImmutableList<DfaNode> getOuts() {
      return outs;
    }

    @Override
    void writeImpl(ByteArrayDataOutput out, Map<DfaNode, Integer> unused, Statistics stats) {
      //  <--------- 1 byte --------->
      // [ OPCODE | TRM |    VALUE    ]
      out.writeByte((getOpCode().ordinal() << 5)
          | (isTerminating() ? (1 << 4) : 0)
          | digit.value());
    }

    @Override
    public String toString() {
      return format(digit.value());
    }
  }

  /**
   * Respresents a state which can be transitioned from to a single output state via any input
   * (ie, "\d"). Successive "Any" oeprations can be merged to represent a repeated sequence
   * (eg, "\d{5}").
   */
  private static final class Any extends Operation {
    private final int count;
    private final ImmutableList<DfaNode> outs;

    Any(boolean isTerminating, int count, ImmutableList<DfaNode> outs) {
      super(isTerminating, false);
      Preconditions.checkArgument(outs.size() == 1);
      Preconditions.checkArgument(count > 0);
      this.count = count;
      this.outs = outs;
    }

    @Override
    OpCode getOpCode() {
      return OpCode.ANY;
    }

    @Override ImmutableList<DfaNode> getOuts() {
      return outs;
    }

    @Override
    void writeImpl(ByteArrayDataOutput out, Map<DfaNode, Integer> unused, Statistics stats) {
      int remainingCount = count;
      //  <--------- 1 byte --------->
      // [ OPCODE | TRM |   COUNT-1   ]
      int anyN = (getOpCode().ordinal() << 5) | (isTerminating() ? (1 << 4) : 0);
      while (remainingCount > 16) {
        out.writeByte(anyN | 15);
        remainingCount -= 16;
      }
      out.writeByte(anyN | remainingCount - 1);
    }

    @Override
    public Operation mergeWith(Operation next) {
      if (next.getOpCode() == OpCode.ANY && isTerminating() == next.isTerminating()) {
        return new Any(isTerminating(), this.count + ((Any) next).count, ((Any) next).outs);
      }
      return null;
    }

    @Override
    public String toString() {
      return format(count);
    }
  }

  /**
   * Represents a state which can be transitioned from via an arbitrary set of inputs to either
   * one or two output nodes (eg, "[23-69]" or "[0-4]X|[5-9]Y"). In the case where there are two
   * output nodes, any input must reach one of the two possible nodes (ie, there is no invalid
   * input).
   */
  private static final class Range extends Operation {
    private final ImmutableSet<Digit> digits;
    private final ImmutableList<DfaNode> outs;

    Range(boolean isTerminating, int digitMask, ImmutableList<DfaNode> outs) {
      super(isTerminating, outs.size() == 2);
      Preconditions.checkArgument(outs.size() <= 2);
      this.digits = Digit.fromMask(digitMask);
      this.outs = outs;
    }

    @Override
    OpCode getOpCode() {
      return OpCode.RANGE;
    }

    /**
     * For branching Range operations (with 2 output nodes), the order is that the state matched
     * by {@code digits} is the first state and the state reached by any other input is second.
     */
    @Override ImmutableList<DfaNode> getOuts() {
      return outs;
    }

    @Override
    void writeImpl(ByteArrayDataOutput out, Map<DfaNode, Integer> offsetMap, Statistics stats) {
      //  <-------------- 2 bytes --------------> <-------- 2 bytes --------->
      // [ OPCODE | TRM |  0  |     BIT SET      ]
      // [ OPCODE | TRM |  1  |     BIT SET      |   JUMP_IN   |   JUMP_OUT   ]
      out.writeShort((getOpCode().ordinal() << 13)
          | (isTerminating() ? (1 << 12) : 0)
          | (isBranching() ? (1 << 11) : 0)
          | asBitMask(digits));
      if (isBranching()) {
        writeJumpTable(out, ImmutableList.of(
            offsetMap.get(outs.get(0)), offsetMap.get(outs.get(1))), stats);
      }
    }

    @Override
    public String toString() {
      return format(asRangeString(digits));
    }
  }

  /**
   * Represents a state in the matcher which can be transitioned from via an arbitrary set of
   * inputs, to an arbitrary set of nodes. This is the most general form of operation and (apart
   * from branches) provides the only truly necessary instruction in the matcher; everything else
   * is just some specialization of this operation.
   */
  private static final class Mapping extends Operation {
    private final ImmutableSetMultimap<DfaNode, Digit> nodeMap;

    Mapping(boolean isTerminating, ImmutableMap<DfaNode, Integer> outMasks) {
      super(isTerminating, true);
      this.nodeMap = outMasks.entrySet().stream()
          .collect(flatteningToImmutableSetMultimap(
              Entry::getKey, e -> Digit.fromMask(e.getValue()).stream()));
    }

    @Override
    OpCode getOpCode() {
      return isTerminating() ? OpCode.TMAP : OpCode.MAP;
    }

    /**
     * For Mapping operations, output node order is defined by the lowest digit by which that
     * node can be reached. For example, if a map operation can reach three nodes {@code A},
     * {@code B} and {@code C} via inputs in the ranges {@code [1-38]}, {@code [4-6]} and
     * {@code [09]} respectively, then they will be ordered {@code (C, A, B)}.
     */
    @Override ImmutableList<DfaNode> getOuts() {
      return nodeMap.keySet().asList();
    }

    @Override
    void writeImpl(ByteArrayDataOutput out, Map<DfaNode, Integer> offsetMap, Statistics stats) {
      //  <------------ 4 bytes ------------> <-- 1 byte per offset --->
      // [ OPCODE |        CODED MAP         |  JUMP_1  | ... | JUMP_N  ]
      out.writeInt((getOpCode().ordinal() << 29) | asCodedMap(nodeMap));
      ImmutableList<Integer> offsets =
          getOuts().stream().map(offsetMap::get).collect(toImmutableList());
      writeJumpTable(out, offsets, stats);
    }

    @Override
    public String toString() {
      return format(nodeMap.asMap().values().stream()
          .map(Operation::asRangeString).collect(joining(", ")));
    }
  }

  String format(Object extra) {
    return String.format("%s%s : %s", getOpCode(), isTerminating() ? "*" : "", extra);
  }

  /**
   * Returns an integer with the lowest 10 bits set in accordance with the digits in the given set.
   */
  private static int asBitMask(ImmutableSet<Digit> digits) {
    int bitMask = 0;
    for (Digit digit : digits) {
      bitMask |= (1 << digit.value());
    }
    return bitMask;
  }

  /**
   * Returns a integer with the lowest 29 bits set to encode an arbitrary mapping from input digit
   * to an output index. The 29 bits are partitioned such that lower inputs require fewer bits to
   * encode (output indices are assigned as they are encountered, starting at the first input).
   * Each digit can then be quickly mapped to either its 1-indexed output node, or 0 if the input
   * was invalid.
   */
  private static int asCodedMap(ImmutableSetMultimap<DfaNode, Digit> nodeMap) {
    int codedMap = 0;
    List<DfaNode> outs = nodeMap.keySet().asList();
    for (int n = 0; n < outs.size(); n++) {
      for (Digit digit : nodeMap.get(outs.get(n))) {
        // Coded indices are 1-to-10 (0 is the "invalid" node).
        codedMap |= ((n + 1) << OpCode.getMapShift(digit.value()));
      }
    }
    return codedMap;
  }

  /**
   * Writes a sequence of offsets representing a unsigned byte-based jump table after either a
   * Mapping or Range instruction. This accounts correctly for the need to introduce a new
   * "trampoline" branch instruction after the jump table (when the desired offset is too large
   * to fit in a single unsigned byte).
   * <p>
   * Offsets are either:
   * <ul>
   * <li>The number of bytes to jump from the end of the current {@code Sequence} bytes to the
   *     start of the destination {@code Sequence} bytes.
   * <li>{@code -1} to indicate that a terminal node has been reached.
   * </ul>
   * <p>
   * Note that the offset written into the jump table itself must be relative to the beginning of
   * the jump table and so must be adjusted by the number of bytes in the jump table and any other
   * branch instructions that follow it. This it probably the most awkward logic in the entire
   * compiler.
   */
  static void writeJumpTable(ByteArrayDataOutput out, List<Integer> offsets,
      Statistics stats) {
    int jumpTableSize = offsets.size();
    boolean needsExtraBranches = false;
    for (int n = 0; n < jumpTableSize && !needsExtraBranches; n++) {
      // Check whether the adjusted offset (ie, the one we would write) will fit in a byte.
      // It's no issue to have offsets of -1 as it can never trigger "needsExtraBranches".
      needsExtraBranches = (offsets.get(n) + jumpTableSize >= 0x100);
    }
    if (needsExtraBranches) {
      // We only get here if at least one offset (after adjustment by the original jump table size)
      // would not fit into a byte. Now we must calculate exactly how many extra branches we are
      // going to need. For this we must assume the worst case adjustment of "3 x jumpTableSize"
      // which is 1 byte for the jump table offset and 2 bytes for the extra branch for every entry.
      // This is pessimistic because there will now be cases where we write a trampoline jump for
      // an offset that could have fitted had we not assumed that we might need the extra space for
      // the branch. However these cases are rare enough that we choose to ignore them.
      int maxOffsetAdjust = ((1 + LONG_BRANCH_SIZE) * jumpTableSize);
      int extraBranchCount = 0;
      for (int n = 0; n < jumpTableSize; n++) {
        if (offsets.get(n) + maxOffsetAdjust >= 0x100) {
          extraBranchCount += 1;
        }
      }
      // Now we know a reasonable upper bound for how many extra branches are needed, use this to
      // adjust the actual offsets and write them. When a "trampoline" branch instruction is needed
      // we split the offset so the jump table jumps to the branch instruction and that jumps the
      // rest. Branch instructions are positioned, in order, immediately after the jump table.
      List<Integer> extraBranchOffsets = new ArrayList<>();
      int totalOffsetAdjust = jumpTableSize + (LONG_BRANCH_SIZE * extraBranchCount);
      for (int n = 0; n < jumpTableSize; n++) {
        int offset = offsets.get(n);
        if (offset >= 0) {
          int worstCaseOffset = offset + maxOffsetAdjust;
          // Get the actual total offset we want to jump by.
          offset += totalOffsetAdjust;
          // Use the worst case offset here so we repeat exactly the same decision as the loop
          // above (otherwise we might add fewer branches which would screw up our offsets).
          if (worstCaseOffset >= 0x100) {
            // Split the original offset, recording the jump to the trampoline branch as well as
            // the branch offset itself. Note that the offset adjustment changes as more trampoline
            // branches are encountered (but the overall offset jumped remains the same).
            int extraBranchIndex = extraBranchOffsets.size();
            // This offset will always be small (max jump table is 10 entries, so offset to the
            // last possible branch will be at most 28 bytes).
            int branchInstructionOffset = jumpTableSize + (LONG_BRANCH_SIZE * extraBranchIndex);
            // Subtract one additional branch instruction here because when we trampoline jump, we
            // jump to the start of the branch instruction, but jump away from the end of it.
            extraBranchOffsets.add((offset - branchInstructionOffset) - LONG_BRANCH_SIZE);
            offset = branchInstructionOffset;
          }
          // Write the total offset (offset must be < 0x100 here as worstCaseOffset was < 0x100).
          Preconditions.checkState(offset < 0x100, "jump too long: %s", offset);
          out.writeByte(offset);
        } else {
          // If the destination of this jump would just be a termination instruction, just write
          // the termination byte here directly (no point jumping to the termination byte).
          Preconditions.checkArgument(offset == TERMINATION_OFFSET, "bad offset: %s", offset);
          writeTerminator(out, stats);
        }
      }
      // Write out the trampoline jumps in the order they were found.
      for (int offset : extraBranchOffsets) {
        stats.record(Type.DOUBLE_JUMP);
        Operation.writeBranch(out, offset, stats);
      }
    } else {
      // In the simple case, there are no extra branches, so we just write the offsets we have.
      // This has the same effect as running the code above with (extraBranchCount == 0) but can be
      // reached more optimistically because we don't need to account for the worst case offset
      // adjustment when deciding if it's safe to just use the offsets we were given. It's a form
      // of hysteresis between the no-branch and extra-branch cases.
      for (int n = 0; n < jumpTableSize; n++) {
        int offset = offsets.get(n);
        if (offset >= 0) {
          offset += jumpTableSize;
          Preconditions.checkState(offset < 0x100, "jump too long: " + offset);
          out.writeByte(offset);
        } else {
          writeTerminator(out, stats);
        }
      }
    }
  }

  // Helper function for asRanges() to print a single range (eg, "[014-7]").
  private static String asRangeString(Collection<Digit> digits) {
    StringBuilder out = new StringBuilder();
    out.append("[");
    Digit lhs = null;
    Digit rhs = null;
    for (Digit digit : digits) {
      if (lhs != null) {
        if (digit.value() == rhs.value() + 1) {
          rhs = digit;
          continue;
        }
        if (rhs != lhs) {
          if (rhs.value() > lhs.value() + 1) {
            out.append("-");
          }
          out.append(rhs.value());
        }
      }
      lhs = digit;
      rhs = digit;
      out.append(lhs.value());
    }
    if (rhs != lhs) {
      if (rhs.value() > lhs.value() + 1) {
        out.append("-");
      }
      out.append(rhs.value());
    }
    out.append("]");
    return out.toString();
  }
}
