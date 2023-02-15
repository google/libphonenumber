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

import static com.google.common.primitives.Bytes.asList;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OperationTest {

  @Test public void testWriteJumpTableNoExtraBranches() {
    ByteArrayDataOutput outBytes = ByteStreams.newDataOutput();
    Operation.writeJumpTable(outBytes, ImmutableList.of(0x10, 0x80, 0xFC), Statistics.NO_OP);
    // The jump table size is added to the offsets.
    Assert.assertEquals(
        asList(new byte[] {(byte) 0x13, (byte) 0x83, (byte) 0xFF}),
        asList(outBytes.toByteArray()));
  }

  // An easy way to reason about what the offsets for the branches should be is to consider
  // that the last branch must always have the original offset (it jumps from the very end of
  // the jump table, which is exactly what the original offset specified. The branch before it
  // is the same except that it must jump over the final branch (ie, +2 bytes) and so on.
  // Direct offsets are relative to the start of the jump table however and must be adjusted.
  @Test public void testWriteJumpTableExtraBranches() {
    ByteArrayDataOutput outBytes = ByteStreams.newDataOutput();
    // Two extra branches needed (0x200 and 0xF7). Worst case adjustment is 9 bytes.
    // Total adjustment is 7 bytes (jump table size + 2 * branch)
    Operation.writeJumpTable(outBytes, ImmutableList.of(0xF7, 0xF6, 0x200), Statistics.NO_OP);
    Assert.assertEquals(asList(new byte[] {
        // Jump table: (offset-to-branch, direct-adjusted-offset, offset-to-branch)
        (byte) 0x03, (byte) 0xFD, (byte) 0x05,
        // Extra branch: offset = 0xF7 + 2 (jumps over last branch)
        (byte) 0x10, (byte) 0xF9,
        // Extra branch: offset = 0x200 (last branch always has original offset)
        (byte) 0x12, (byte) 0x00}),
        asList(outBytes.toByteArray()));
  }
}
