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
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;
import static java.lang.Integer.bitCount;
import static java.lang.Integer.lowestOneBit;
import static java.lang.Integer.numberOfTrailingZeros;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.i18n.phonenumbers.internal.finitestatematcher.compiler.RegressionTestProto;
import com.google.i18n.phonenumbers.internal.finitestatematcher.compiler.RegressionTestProto.TestCase;
import com.google.i18n.phonenumbers.internal.finitestatematcher.compiler.RegressionTestProto.Tests;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaEdge;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaVisitor;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher;
import com.google.i18n.phonenumbers.metadata.finitestatematcher.DigitSequenceMatcher.Result;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CompilerRegressionTest {
  // Tests that the compiler produces the expected output, byte-for-byte.
  @Test
  public void testCompiledBytesEqualExpectedMatcherBytes() throws IOException {
    StringWriter buffer = new StringWriter();
    PrintWriter errors = new PrintWriter(buffer);
    try (InputStream data =
        CompilerRegressionTest.class.getResourceAsStream("regression_test_data.textpb")) {
      Tests.Builder tests = RegressionTestProto.Tests.newBuilder();
      TextFormat.merge(new InputStreamReader(data, StandardCharsets.UTF_8), tests);
      for (TestCase tc : tests.getTestCaseList()) {
        byte[] actual = MatcherCompiler.compile(ranges(tc.getRangeList()));
        byte[] expected = combine(tc.getExpectedList());
        int diffIndex = indexOfDiff(actual, expected);
        if (!tc.getShouldFail()) {
          if (diffIndex != -1) {
            errors.format("FAILED [%s]: First difference at index %d\n", tc.getName(), diffIndex);
            errors.format("Actual  : %s\n", formatPbSnippet(actual, diffIndex, 20));
            errors.format("Expected: %s\n", formatPbSnippet(expected, diffIndex, 20));
            writeGoldenPbOutput(actual, errors);
          }
        } else {
          if (diffIndex == -1) {
            errors.format("FAILED [%s]: Expected difference, but got none\n", tc.getName());
          }
        }
      }
    }
    String errorMessage = buffer.toString();
    if (!errorMessage.isEmpty()) {
      assertWithMessage(errorMessage).fail();
    }
  }

  // Test that the matcher behaves correctly with respect to the input ranges using the expected
  // byte sequences. If this test fails, then the matcher implementation is doing something wrong,
  // or the expected bytes were generated incorrectly (either by hand or from the compiler).
  //
  // IMPORTANT: This test tests that the expected bytes (rather than the compiled bytes) match the
  // numbers in the ranges. This avoids the risk of any bugs in both the matcher and compiler
  // somehow cancelling each other out. However this also means that this test depends on the
  // equality test above for validity (i.e. this test can pass even if the matcher compiler is
  // broken, so it should not be run in isolation when debugging).
  @Test
  public void testExpectedMatcherBytesMatchRanges() throws IOException {
    try (InputStream data =
        CompilerRegressionTest.class.getResourceAsStream("regression_test_data.textpb")) {
      RegressionTestProto.Tests.Builder tests = RegressionTestProto.Tests.newBuilder();
      TextFormat.merge(new InputStreamReader(data, StandardCharsets.UTF_8), tests);
      for (TestCase tc : tests.getTestCaseList()) {
        RangeTree ranges = ranges(tc.getRangeList());
        // If we compiled the ranges here, we could risk a situation where the compiled bytes were
        // broken but the compiler had a corresponding bug that cancelled it out. This test only
        // tests the matcher behaviour, whereas the test above only tests the compiler behaviour.
        DigitSequenceMatcher matcher = DigitSequenceMatcher.create(combine(tc.getExpectedList()));
        Multimap<Result, DigitSequence> numbers = buildTestNumbers(ranges);
        if (!tc.getShouldFail()) {
          testExpectedMatch(tc.getName(), matcher, numbers);
        } else {
          testExpectedFailure(tc.getName(), matcher, numbers);
        }
      }
    }
  }

  private static void testExpectedMatch(String testName, DigitSequenceMatcher matcher,
      Multimap<Result, DigitSequence> numbers) {
    for (Result expectedResult : Result.values()) {
      for (DigitSequence s : numbers.get(expectedResult)) {
        Result result = matcher.match(new Sequence(s));
        assertWithMessage("FAILED [%s]: Sequence %s", testName, s)
            .that(result).isEqualTo(expectedResult);
      }
    }
  }

  private static void testExpectedFailure(String testName, DigitSequenceMatcher matcher,
      Multimap<Result, DigitSequence> numbers) {
    for (Result expectedResult : Result.values()) {
      for (DigitSequence s : numbers.get(expectedResult)) {
        Result result = matcher.match(new Sequence(s));
        if (result != expectedResult) {
          return;
        }
      }
    }
    assertWithMessage("FAILED [%s]: Expected at least one failure", testName).fail();
  }

  // Magic number: DigitSequences cannot be longer than 18 digits at the moment, so a check is
  // needed to prevent us trying to make a longer-than-allowed sequences in tests. This only
  // happens in the case of a terminal node, since non-terminal paths must be < 17 digits long.
  // If the allowed digits increases, this value can be modified or left as-is.
  private static final int MAX_SEQUENCE_LENGTH = 18;

  // Trivial adapter from the metadata DigitSequence to the matcher's lightweight sequence.
  private static final class Sequence implements DigitSequenceMatcher.DigitSequence {
    private final DigitSequence seq;
    private int index = 0;

    Sequence(DigitSequence seq) {
      this.seq = seq;
    }

    @Override
    public boolean hasNext() {
      return index < seq.length();
    }

    @Override
    public int next() {
      return seq.getDigit(index++);
    }
  }

  // Returns a RangeTree for the list of RangeSpecification strings.
  RangeTree ranges(List<String> specs) {
    return RangeTree.from(specs.stream().map(RangeSpecification::parse).collect(toImmutableList()));
  }

  // Builds a map of numbers for the given RangeTree to test every branching point in the DFA.
  // All paths combinations are generated exactly once to give coverage. This does use pseudo
  // random numbers to pick random digits from masks, but it should not be flaky. If it _ever_
  // fails then it implies a serious problem with the matcher compiler or matcher implementation.
  private static Multimap<Result, DigitSequence> buildTestNumbers(RangeTree ranges) {
    SetMultimap<Result, DigitSequence> numbers =
        MultimapBuilder.enumKeys(Result.class).treeSetValues().build();
    Set<DfaNode> visited = new HashSet<>();
    ranges.accept(new Visitor(RangeSpecification.empty(), numbers, visited));
    return numbers;
  }

  /**
   * Visitor to generate a targeted set of test numbers from a range tree DFA, which should
   * exercise every instruction in the corresponding matcher data. These numbers should ensure
   * that every "branch" (including early terminations) is taken at least once. Where digits
   * should be equivalent (i.e. both x & y have the same effect) they are chosen randomly, since
   * otherwise you would need to generate billions of numbers to cover every possible combination.
   */
  private static final class Visitor implements DfaVisitor {
    private final RangeSpecification sourcePath;
    private final SetMultimap<Result, DigitSequence> numbers;
    private final Set<DfaNode> visited;
    private int outEdgesMask = 0;

    Visitor(RangeSpecification sourcePath,
        SetMultimap<Result, DigitSequence> numbers,
        Set<DfaNode> visited) {
      this.sourcePath = sourcePath;
      this.numbers = numbers;
      this.visited = visited;
    }

    @Override
    public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
      // Record the current outgoing edge mask.
      int mask = edge.getDigitMask();
      outEdgesMask |= mask;
      // Get the current path and add a test number for it.
      RangeSpecification path = sourcePath.extendByMask(mask);
      numbers.put(target.canTerminate() ? Result.MATCHED : Result.TOO_SHORT, sequenceIn(path));
      // Avoid recursing into nodes we've already visited. This avoids generating many (hundreds)
      // of test numbers for nodes which are reachable in many ways (via many path prefixes). This
      // is an optional check and could be removed, but for testing larger ranges it seems to make
      // a difference in test time. DFA node/instruction coverage should be unaffected by this.
      if (visited.contains(target)) {
        return;
      }
      visited.add(target);
      // Recurse into the next level with a new visitor starting from our path (it's okay to visit
      // the terminal node here since it does nothing and leaves the out edges mask zero).
      Visitor childVisitor = new Visitor(path, numbers, visited);
      target.accept(childVisitor);
      // After recursion, find out which of our target's out-edges cannot be reached.
      int unreachableMask = ~childVisitor.outEdgesMask & ALL_DIGITS_MASK;
      if (unreachableMask != 0 && path.length() < MAX_SEQUENCE_LENGTH) {
        // Create a path which cannot be reached directly from our target node. If this is the
        // terminal node then we create a path that's too long, otherwise it's just invalid.
        Result expected = target.equals(RangeTree.getTerminal()) ? Result.TOO_LONG : Result.INVALID;
        numbers.put(expected, sequenceIn(path.extendByMask(unreachableMask)));
      }
    }
  }

  // Returns a pseudo randomly chosen sequence from the given path.
  private static final DigitSequence sequenceIn(RangeSpecification path) {
    DigitSequence seq = DigitSequence.empty();
    for (int n = 0; n < path.length(); n++) {
      int mask = path.getBitmask(n);
      // A random number M in [0..BitCount), not the bit itself.
      // E.g. mask = 0011010011 ==> (0 <= maskBit < 5) (allowed digits are {0,1,4,6,7})
      int maskBit = (int) (bitCount(mask) * Math.random());
      // Mask out the M lower bits which come before the randomly selected one.
      // E.g. maskBit = 3 ==> mask = 0011000000 (3 lower bits cleared)
      while (maskBit > 0) {
        mask &= ~lowestOneBit(mask);
        maskBit--;
      }
      // Extend the sequence by the digit value of the randomly selected bit.
      // E.g. mask = 0011000000 ==> digit = 6 (randomly chosen from the allowed digits).
      seq = seq.extendBy(numberOfTrailingZeros(mask));
    }
    return seq;
  }

  // Combines multiple ByteStrings into a single byte[] (we allow splitting in the regression test
  // file for readability.
  private static byte[] combine(List<ByteString> bytes) {
    int size = bytes.stream().mapToInt(ByteString::size).sum();
    byte[] out = new byte[size];
    int offset = 0;
    for (ByteString b : bytes) {
      b.copyTo(out, offset);
      offset += b.size();
    }
    return out;
  }

  // Return the index of the first difference, or -1 is the byte arrays are the same.
  private static int indexOfDiff(byte[] a, byte[] b) {
    int length = Math.min(a.length, b.length);
    for (int n = 0; n < length; n++) {
      if (a[n] != b[n]) {
        return n;
      }
    }
    return (a.length == length && b.length == length) ? -1 : length;
  }

  // Formats a subset of the bytes as a human readable snippet using C-style hex escaping (which
  // is compatible with the regression test data).
  private static String formatPbSnippet(byte[] bytes, int start, int length) {
    StringBuilder out = new StringBuilder();
    if (start > 0) {
      out.append("...");
    }
    appendBytes(out, bytes, start, length);
    if (start + length < bytes.length) {
      out.append("...");
    }
    return out.toString();
  }

  // Writes bytes such that they can be cut & pasted into a regression test file as new golden data.
  private static void writeGoldenPbOutput(byte[] bytes, PrintWriter errors) {
    errors.println("Golden Data:");
    StringBuilder out = new StringBuilder();
    for (int start = 0; start < bytes.length; start += 20) {
      errors.format("  expected: \"%s\"\n", appendBytes(out, bytes, start, 20));
      out.setLength(0);
    }
  }

  // Appends a set of bytes in C-style hex format (e.g. \xHH).
  private static StringBuilder appendBytes(StringBuilder out, byte[] bytes, int start, int length) {
    int end = Math.min(start + length, bytes.length);
    for (int n = start; n < end; n++) {
      out.append(String.format("\\x%02x", bytes[n] & 0xFF));
    }
    return out;
  }
}
