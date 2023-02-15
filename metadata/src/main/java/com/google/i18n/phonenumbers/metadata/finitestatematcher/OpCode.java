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

package com.google.i18n.phonenumbers.metadata.finitestatematcher;

import com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.DataView;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.DigitSequence;

/**
 * Implementation of instructions for the phone number matcher state machine.
 * <p>
 * <h3>Jump Tables</h3>
 *
 * Several instructions use a "jump table" concept which is simply a contiguous region of bytes
 * containing offsets from which a new position is calculated. The new position is the current
 * position (at the start of the jump table) plus the value of the chosen jump offset.
 *
 * <pre>{@code
 * [    ...    | JUMP_0 | JUMP_1 | ... | JUMP_N |    ...    |  DEST  |  ...
 *  position --^            ^                               ^
 *             `---index ---'                               |
 *  offset     `----------------  [ position + index ] -----'
 *
 *  position = position + unsignedByteValueAt(position + index)
 * }</pre>
 *
 * A jump offset of zero signifies that the state jumped to is terminal (this avoids having to jump
 * to a termination byte). A jump table will always occur immediately after an associated
 * instruction and the instruction's stated size includes the number of bytes in the jump table.
 */
public enum OpCode {
  /**
   * Jumps ahead by between 1 and 4095 bytes from the end of this opcode. This opcode does not
   * consume any input.
   * <p>
   * This is a variable length instruction, taking one byte for offsets up to 15 and (if EXT is set)
   * two bytes for larger offsets up to 4095. The jump offset signifies how many bytes to skip after
   * this instruction.
   * <p>
   * As a special case, a single byte branch with a jump offset of zero (represented by a single
   * zero byte) can be used to signify that the current state is terminal and the state machine
   * should exit (a zero jump offset never makes sense in any instruction).
   *
   * <pre>{@code
   * [ 0 | 0 |  JUMP   ]
   * [ 0 | 1 |  JUMP   |  EXT_JUMP   ]
   *  <3>.<1>.<-- 4 -->.<---- 8 ---->
   * }</pre>
   */
  BRANCH(0) {
    @Override
    State execute(DataView data, DigitSequence ignored) {
      int op = data.readByte();
      int offset = op & 0xF;
      if ((op & (1 << 4)) != 0) {
        offset = (offset << 8) + data.readByte();
      }
      return data.branch(offset);
    }
  },
  /**
   * Accepts a single input (and transition to a single state). Inputs not matching "VAL" are
   * invalid from the current state. If "TRM" is set then the state being transitioned from may
   * terminate.
   *
   * <pre>{@code
   * [ 1 |TRM|  VAL  ]
   *  <3>.<1>.<- 4 ->
   * }</pre>
   */
  SINGLE(1) {
    @Override
    State execute(DataView data, DigitSequence in) {
      int op = data.readByte();
      if (!in.hasNext()) {
        return ((op & (1 << 4)) != 0) ? State.TERMINAL : State.TRUNCATED;
      }
      int n = in.next();
      return ((op & 0xF) == n) ? State.CONTINUE : State.INVALID;
    }
  },
  /**
   * Accept any input to transition to a single state one or more times.
   * <p>
   * If "TRM" is set then every state that is transitioned from may terminate.
   *
   * <pre>{@code
   * [ 2 |TRM| NUM-1 ]
   *  <3>.<1>.<- 4 ->
   * }</pre>
   */
  ANY(2) {
    @Override
    State execute(DataView data, DigitSequence in) {
      int op = data.readByte();
      int num = (op & 0xF) + 1;
      boolean isTerminating = (op & (1 << 4)) != 0;
      while (num-- > 0) {
        if (!in.hasNext()) {
          return isTerminating ? State.TERMINAL : State.TRUNCATED;
        }
        in.next();
      }
      return State.CONTINUE;
    }
  },
  /**
   * Accepts multiple inputs to transition to one or two states. The bit-set has the Nth bit set if
   * we should accept digit N (bit-0 is the lowest bit of the 2 byte form of the instruction).
   * <p>
   * This is a variable length instruction which either treats non-matched inputs as invalid
   * (2 byte form) or branches to one of two states via a 2-entry jump table (4 byte form).
   * <p>
   * If "TRM" is set then the state being transitioned from may terminate.
   *
   * <pre>{@code
   * [ 3 |TRM| 0 |---|   BIT SET  ]
   * [ 3 |TRM| 1 |---|   BIT SET  |  JUMP_IN  | JUMP_OUT  ]
   *  <3>.<1>.<1>.<1>.<--- 10 --->.<--- 8 --->.<--- 8 --->
   * }</pre>
   */
  RANGE(3) {
    @Override
    State execute(DataView data, DigitSequence in) {
      int op = data.readShort();
      if (!in.hasNext()) {
        return ((op & (1 << 12)) != 0) ? State.TERMINAL : State.TRUNCATED;
      }
      int n = in.next();
      if ((op & (1 << 11)) == 0) {
        // 2 byte form, non-matched input is invalid.
        return ((op & (1 << n)) != 0) ? State.CONTINUE : State.INVALID;
      }
      // 4 byte form uses jump table (use bitwise negation so a set bit becomes a 0 index).
      return data.jumpTable((~op >>> n) & 1);
    }
  },
  /**
   * Accept multiple inputs to transition to between one and ten states via jump offsets. Inputs
   * not encoded in "CODED MAP" are invalid from the current state.
   * <p>
   * Because there is no room for a termination bit in this instruction, there is an alternate
   * version, {@code TMAP}, which should be used when transitioning from a terminating state.
   * <p>
   * TODO: Figure out if we can save one bit here and merge MAP and TMAP.
   *
   * <pre>{@code
   * [ 4 |      CODED MAP       |  JUMP_1   |  ... |  JUMP_N   ]
   *  <3>.<-------- 29 -------->.<--- 8 --->.  ... .<--- 8 --->
   * }</pre>
   */
  MAP(4) {
    @Override
    State execute(DataView data, DigitSequence in) {
      return map(data, in, State.TRUNCATED);
    }
  },
  /**
   * Like {@code MAP} but transitions from a terminating state.
   */
  TMAP(5) {
    @Override
    State execute(DataView data, DigitSequence in) {
      return map(data, in, State.TERMINAL);
    }
  };

  /** The types of states that the state-machine can be in. */
  public enum State {
    CONTINUE, TERMINAL, INVALID, TRUNCATED;
  }

  private static final OpCode[] VALUES = values();

  /**
   * Encode maps as 29 bits where each digit takes a different number of bits to encode its offset.
   * Specifically:
   * <ul>
   * <li>The first entry (matching 0) has only two possible values (it is either not present or maps
   * to the first entry in the jump table), so takes only 1 bit.
   * <li>The second entry (matching 1) has three possible values (not present or maps to either the
   * first or second entry in the jump table), so it takes 2 bits.
   * <li>In general the entry matching digit N has (N+1) possible states and takes log2(N+1) bits.
   * </ul>
   */
  private static final long MAP_SHIFT_BITS = 0L << 0 | // 1 bit  (1x, mask=1)
      1L << 5 | 3L << 10 |                             // 2 bits (2x, mask=3)
      5L << 15 | 8L << 20 | 11L << 25 | 14L << 30 |    // 3 bits (4x, mask=7)
      17L << 35 | 21L << 40 | 25L << 45;               // 4 bits (3x, mask=F)

  /**
   * A table of values with which to mask the coded jump table map, after shifting it. Each nibble
   * is a mask of up to 4 bits to extract the encoded index from a map instruction after it has
   * been shifted.
   */
  private static final long MAP_MASK_BITS = 0xFFF7777331L;

  /**
   * Returns the number of bits we must shift the coded jump table map for a digit with value
   * {@code n} such that the jump index is in the lowest bits.
   */
  public static int getMapShift(int n) {
    return (int) (MAP_SHIFT_BITS >>> (5 * n)) & 0x1F;
  }

  /**
   * Returns a mask we must apply to the shifted jump table map to extract only the jump index from
   * the lowest bits.
   */
  public static int getMapMask(int n) {
    return (int) (MAP_MASK_BITS >>> (4 * n)) & 0xF;
  }

  /**
   * Executes a map instruction by decoding the map data and selecting a jump offset to apply.
   */
  private static State map(DataView data, DigitSequence in, State noInputState) {
    int op = data.readInt();
    if (!in.hasNext()) {
      return noInputState;
    }
    int n = in.next();
    // Coded indices are 1-to-10 (0 is the "invalid" state).
    int index = ((op >>> getMapShift(n)) & getMapMask(n));
    if (index == 0) {
      return State.INVALID;
    }
    // Jump offsets are zero based.
    return data.jumpTable(index - 1);
  }

  /**
   * Returns the opcode associated with the given unsigned byte value (the first byte of any
   * instruction).
   */
  static OpCode decode(int unsignedByte) {
    return VALUES[unsignedByte >>> 5];
  }

  private OpCode(int code) {
    // Assertion checks during enum creation. Opcodes must be 3 bits and match the ordinal of the
    // enum (this prevents issues if reordering enums occurs).
    if ((code & ~0x7) != 0 || code != ordinal()) {
      throw new AssertionError("bad opcode value: " + code);
    }
  }

  abstract State execute(DataView data, DigitSequence in);
}
