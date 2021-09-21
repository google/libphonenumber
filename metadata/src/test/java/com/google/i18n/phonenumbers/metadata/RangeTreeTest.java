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
package com.google.i18n.phonenumbers.metadata;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.i18n.phonenumbers.metadata.DigitSequence.domain;
import static com.google.i18n.phonenumbers.metadata.testing.AssertUtil.assertThrows;
import static com.google.i18n.phonenumbers.metadata.testing.RangeTreeSubject.assertThat;
import static java.util.Arrays.asList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaEdge;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaNode;
import com.google.i18n.phonenumbers.metadata.RangeTree.DfaVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RangeTreeTest {
  @Test
  public void testEmptyTree() {
    assertThat(RangeTree.empty()).containsExactly();
    assertThat(RangeTree.empty()).hasSize(0);
  }

  @Test
  public void testEmptySequenceTree() {
    // The tree that matches a zero length input is a perfectly valid range tree (zero length input
    // is perfectly valid input). This is very distinct from the empty tree, which cannot match any
    // input. It's not used very often, but it is well defined.
    RangeTree r = RangeTree.from(RangeSpecification.empty());
    assertThat(r).containsExactly(RangeSpecification.empty());
    assertThat(r).hasSize(1);
  }

  @Test
  public void testFromRangeSetSimple() {
    // Single ranges produce minimal/canoncial range specifications.
    RangeTree r = RangeTree.from(rangeSetOf(range("1000", "4999")));
    assertThat(r).containsExactly("[1-4]xxx");
    assertThat(r).hasSize(4000);
  }

  @Test
  public void testFromRangeSetMinMax() {
    RangeTree r = RangeTree.from(rangeSetOf(range("0000", "9999")));
    assertThat(r).containsExactly("xxxx");
    assertThat(r).hasSize(10000);
  }

  @Test
  public void testFromRangeSetAllValues() {
    // Just checking for any out-of-bounds issues at the end of the domain.
    RangeTree r = RangeTree.from(rangeSetOf(range("0", domain().maxValue().toString())));
    assertThat(r).containsExactly(
        "x",
        "xx",
        "xxx",
        "xxxx",
        "xxxxx",
        "xxxxxx",
        "xxxxxxx",
        "xxxxxxxx",
        "xxxxxxxxx",
        "xxxxxxxxxx",
        "xxxxxxxxxxx",
        "xxxxxxxxxxxx",
        "xxxxxxxxxxxxx",
        "xxxxxxxxxxxxxx",
        "xxxxxxxxxxxxxxx",
        "xxxxxxxxxxxxxxxx",
        "xxxxxxxxxxxxxxxxx",
        "xxxxxxxxxxxxxxxxxx");
  }

  @Test
  public void testContains() {
    // The tree generated from the empty range specification actually contains one digit sequence
    // (the empty one). This is not the same as RangeTree.empty() which really contains nothing.
    assertThat(RangeTree.empty()).doesNotContain("");
    assertThat(RangeTree.from(RangeSpecification.empty())).contains("");
    assertThat(RangeTree.from(spec("x"))).contains("7");
    assertThat(RangeTree.from(spec("1"))).contains("1");
    assertThat(RangeTree.from(spec("1"))).doesNotContain("5");
    assertThat(RangeTree.from(spec("xx"))).contains("99");
    assertThat(RangeTree.from(spec("xx"))).doesNotContain("100");
    assertThat(RangeTree.from(spec("0[123]x[456]x[789]"))).contains("027617");
  }

  @Test
  public void testMatchCount() {
    assertThat(RangeTree.empty()).hasSize(0);
    assertThat(RangeTree.from(RangeSpecification.empty())).hasSize(1);
    assertThat(RangeTree.from(spec("x"))).hasSize(10);
    assertThat(RangeTree.from(spec("1"))).hasSize(1);
    assertThat(RangeTree.from(spec("[123]"))).hasSize(3);
    assertThat(RangeTree.from(spec("xx"))).hasSize(100);
    assertThat(RangeTree.from(spec("[234]xx"))).hasSize(300);
    assertThat(RangeTree.from(spec("1[234]xx"))).hasSize(300);
    assertThat(RangeTree.from(spec("1[234][567]xx"))).hasSize(900);
    assertThat(RangeTree.from(spec("0[123]x[456]x[789]"))).hasSize(2700);
  }

  @Test
  public void testUnion() {
    RangeTree a = ranges("12xx", "456xx");
    assertThat(a.union(a)).isEqualTo(a);
    assertThat(a.union(RangeTree.empty())).isEqualTo(a);
    assertThat(RangeTree.empty().union(a)).isEqualTo(a);

    RangeTree b = ranges("1234", "4xxxx", "999");
    assertThat(a.union(b)).containsExactly("999", "12xx", "4xxxx");
    assertThat(b.union(a)).containsExactly("999", "12xx", "4xxxx");
  }

  @Test
  public void testIntersection() {
    RangeTree a = ranges("12xx", "456xx");
    assertThat(a.intersect(a)).isEqualTo(a);
    assertThat(a.intersect(RangeTree.empty())).isSameInstanceAs(RangeTree.empty());
    assertThat(RangeTree.empty().intersect(a)).isSameInstanceAs(RangeTree.empty());

    RangeTree b = ranges("1234", "4xxxx", "999");
    assertThat(a.intersect(b)).containsExactly("1234", "456xx");
    assertThat(b.intersect(a)).containsExactly("1234", "456xx");
  }

  @Test
  public void testSubtraction() {
    RangeTree a = ranges("12xx", "456xx");
    assertThat(a.subtract(a)).isSameInstanceAs(RangeTree.empty());
    assertThat(a.subtract(RangeTree.empty())).isEqualTo(a);
    assertThat(RangeTree.empty().subtract(a)).isSameInstanceAs(RangeTree.empty());

    RangeTree b = ranges("1234", "4xxxx", "999");
    assertThat(a.subtract(b)).containsExactly("12[0-24-9]x", "123[0-35-9]");
    assertThat(b.subtract(a)).containsExactly("999", "4[0-46-9]xxx", "45[0-57-9]xx");
  }

  @Test
  public void testContainsAll() {
    RangeTree a = ranges("12[3-6]xx", "13[5-8]xx", "456xxxx");
    assertThat(a.containsAll(a)).isTrue();
    assertThat(a.containsAll(RangeTree.empty())).isTrue();
    assertThat(RangeTree.empty().containsAll(a)).isFalse();
    // Test branching, since 12.. and 13... are distinct branches but both contain ..[56][78]x
    assertThat(a.containsAll(ranges("1[23][56][78]x", "4567890"))).isTrue();

    // Path 127.. is not contained.
    assertThat(a.containsAll(ranges("12[357]xx"))).isFalse();
    // Hard to test for, but this should fail immediately (due to length mismatch).
    assertThat(a.containsAll(ranges("123456"))).isFalse();

    // Check edge case for zero-length paths.
    assertThat(ranges("", "1").containsAll(ranges(""))).isTrue();
    assertThat(RangeTree.empty().containsAll(ranges(""))).isFalse();
  }

  @Test
  public void testVennDiagram() {
    // Test basic set-theoretic assumptions about the logical operations.
    // In theory we could run this test with any non-disjoint pair of trees.
    RangeTree a = ranges("12xx", "456xx");
    RangeTree b = ranges("1234", "4xxxx", "999");

    RangeTree intAB = a.intersect(b);
    RangeTree subAB = a.subtract(b);
    RangeTree subBA = b.subtract(a);

    // (A\B) and (B\A) are disjoint with (A^B) and each other.
    assertThat(subAB.intersect(intAB)).isSameInstanceAs(RangeTree.empty());
    assertThat(subBA.intersect(intAB)).isSameInstanceAs(RangeTree.empty());
    assertThat(subAB.intersect(subBA)).isSameInstanceAs(RangeTree.empty());

    // Even the union of (A\B) and (B\A) is disjoint to the intersection.
    assertThat(subAB.union(subBA).intersect(intAB)).isSameInstanceAs(RangeTree.empty());

    // (A\B) + (A^B) = A, (B\A) + (A^B) = B, (A\B) + (B\A) + (A^B) == (A+B)
    assertThat(subAB.union(intAB)).isEqualTo(a);
    assertThat(subBA.union(intAB)).isEqualTo(b);
    assertThat(subAB.union(subBA).union(intAB)).isEqualTo(a.union(b));
  }

  @Test
  public void testFromRaggedRange() {
    RangeTree r = RangeTree.from(rangeSetOf(range("123980", "161097")));
    // Very 'ragged' ranges produde a lot of range specifications.
    assertThat(r).containsExactly(
        "1239[8-9]x",
        "12[4-9]xxx",
        "1[3-5]xxxx",
        "160xxx",
        "1610[0-8]x",
        "16109[0-7]");
  }

  @Test
  public void testComplexSpecsToSimpleRange() {
    List<RangeSpecification> specs = specs(
        "12[3-9]",
        "1[3-9]x",
        "[2-9]xx",
        "xxxx",
        "[0-3]xxxx",
        "4[0-4]xxx",
        "45[0-5]xx",
        "456[0-6]x",
        "4567[0-8]");
    RangeTree r = RangeTree.from(specs);
    assertThat(r).containsExactly(specs);
    assertThat(r.asRangeSet()).isEqualTo(rangeSetOf(range("123", "45678")));
  }

  @Test
  public void testAsRangeSetMultipleGroups() {
    // The range specification has 4 ranges, one each for the four 123x prefixes.
    RangeTree r = ranges("012[3-58][2-7]x");
    assertThat(r.asRangeSet()).isEqualTo(rangeSetOf(
        range("012320", "012379"),
        range("012420", "012479"),
        range("012520", "012579"),
        range("012820", "012879")));
  }

  @Test
  public void testAsRangeSetMerging() {
    // In isolation, the first specification represents two range, and the second represents one.
    RangeTree r = ranges("12[3-4][7-9]x", "125[0-5]x");
    // The range ending 12499 merges with the range starting 12500, giving 2 rather than 3 ranges.
    assertThat(r.asRangeSet()).isEqualTo(rangeSetOf(
        range("12370", "12399"),
        range("12470", "12559")));
  }

  @Test
  public void testVisitor() {
    // Carefully construct DFA so depth first visitation order is just incrementing from 0.
    RangeTree r = ranges("012", "345", "367", "3689");
    TestVisitor v = new TestVisitor();
    r.accept(v);

    DfaNode initial = r.getInitial();
    DfaNode terminal = RangeTree.getTerminal();

    assertThat(v.visited).hasSize(10);
    // Edges 0 & 3 leave the initial state, edges 2,5,7,9 reach the terminal.
    assertThat(v.visited.stream().map(Edge::source).filter(initial::equals).count()).isEqualTo(2);
    assertThat(v.visited.stream().map(Edge::target).filter(terminal::equals).count()).isEqualTo(4);
    // Check expected edge value masks.
    for (int n = 0; n < 10; n++) {
      assertThat(v.visited.get(n).digitMask()).isEqualTo(1 << n);
    }
  }

  @Test
  public void testMin() {
    assertThrows(IllegalStateException.class, () -> RangeTree.empty().first());
    assertThat(RangeTree.from(RangeSpecification.empty()).first()).isEqualTo(DigitSequence.empty());
    RangeTree tree = ranges("[1-6]xxxx", "[6-9]xx", "[89]xxx");
    assertThat(tree.first()).isEqualTo(DigitSequence.of("600"));
    assertThat(tree.subtract(ranges("[6-8]xx")).first()).isEqualTo(DigitSequence.of("900"));
    assertThat(tree.subtract(ranges("xxx")).first()).isEqualTo(DigitSequence.of("8000"));
    assertThat(tree.subtract(ranges("xxx", "8[0-6]xx")).first())
        .isEqualTo(DigitSequence.of("8700"));
    assertThat(tree.subtract(ranges("xxx", "xxxx")).first()).isEqualTo(DigitSequence.of("10000"));
  }

  @Test
  public void testSample() {
    assertThrows(IndexOutOfBoundsException.class, () -> RangeTree.empty().sample(0));
    assertThat(RangeTree.from(RangeSpecification.empty()).sample(0))
        .isEqualTo(DigitSequence.empty());
    RangeTree tree = ranges("[1-6]xxxx", "[6-9]xx", "[89]xxx");
    // sometimes iteration looks ordered ...
    assertThat(tree.sample(0)).isEqualTo(DigitSequence.of("10000"));
    assertThat(tree.sample(1)).isEqualTo(DigitSequence.of("10001"));
    assertThat(tree.sample(10)).isEqualTo(DigitSequence.of("10010"));
    // but in general sample(n).next() != sample(n+1)
    assertThat(tree.sample(49999)).isEqualTo(DigitSequence.of("59999"));
    assertThat(tree.sample(50000)).isEqualTo(DigitSequence.of("600"));
    assertThat(tree.sample(50001)).isEqualTo(DigitSequence.of("60000"));
    assertThat(tree.sample(tree.size() - 1)).isEqualTo(DigitSequence.of("9999"));
    assertThrows(IndexOutOfBoundsException.class, () -> RangeTree.empty().sample(tree.size()));
  }

  @Test
  public void testSignificantDigits() {
    RangeTree ranges = ranges("123xx", "14567", "789");
    assertThat(ranges.significantDigits(3)).containsExactly("123xx", "145xx", "789");
    assertThat(ranges.significantDigits(2)).containsExactly("12xxx", "14xxx", "78x");
    assertThat(ranges.significantDigits(1)).containsExactly("1xxxx", "7xx");
    assertThat(ranges.significantDigits(0)).containsExactly("xxxxx", "xxx");
  }

  @Test
  public void testPrefixWith() {
    RangeTree ranges = ranges("123xx", "456x");
    assertThat(ranges.prefixWith(spec("00"))).isEqualTo(ranges("00123xx", "00456x"));
    assertThat(ranges.prefixWith(RangeSpecification.empty())).isSameInstanceAs(ranges);
    // The prefixing of an empty tree is empty (all paths that exist been prefixed correctly).
    assertThat(RangeTree.empty().prefixWith(spec("00"))).isEqualTo(RangeTree.empty());
  }

  @Test
  public void testSlicing() {
    RangeTree ranges = ranges("", "1", "123", "125xx", "456x");
    assertThat(ranges.slice(1)).isEqualTo(ranges("[14]"));
    assertThat(ranges.slice(2)).isEqualTo(ranges("12", "45"));
    assertThat(ranges.slice(3)).isEqualTo(ranges("12[35]", "456"));
    assertThat(ranges.slice(4)).isEqualTo(ranges("125x", "456x"));
    assertThat(ranges.slice(2, 4)).isEqualTo(ranges("123", "125x", "456x"));
    assertThat(ranges.slice(0, 5)).isEqualTo(ranges);
  }

  @Test
  public void testSerializingRealWorldExample() {
    List<RangeSpecification> expected = specs(
        "11[2-7]xxxxxxx",
        "12[0-249][2-7]xxxxxx",
        "12[35-8]x[2-7]xxxxx",
        "13[0-25][2-7]xxxxxx",
        "13[346-9]x[2-7]xxxxx",
        "14[145][2-7]xxxxxx",
        "14[236-9]x[2-7]xxxxx",
        "1[59][0235-9]x[2-7]xxxxx",
        "1[59][14][2-7]xxxxxx",
        "16[014][2-7]xxxxxx",
        "16[235-9]x[2-7]xxxxx",
        "17[1257][2-7]xxxxxx",
        "17[34689]x[2-7]xxxxx",
        "18[01346][2-7]xxxxxx",
        "18[257-9]x[2-7]xxxxx",
        "2[02][2-7]xxxxxxx",
        "21[134689]x[2-7]xxxxx",
        "21[257][2-7]xxxxxx",
        "23[013][2-7]xxxxxx",
        "23[24-8]x[2-7]xxxxx",
        "24[01][2-7]xxxxxx",
        "24[2-8]x[2-7]xxxxx",
        "25[0137][2-7]xxxxxx",
        "25[25689]x[2-7]xxxxx",
        "26[0158][2-7]xxxxxx",
        "26[2-4679]x[2-7]xxxxx",
        "27[13-79]x[2-7]xxxxx",
        "278[2-7]xxxxxx",
        "28[1568][2-7]xxxxxx",
        "28[2-479]x[2-7]xxxxx",
        "29[14][2-7]xxxxxx",
        "29[235-9]x[2-7]xxxxx",
        "301x[2-7]xxxxx",
        "31[79]x[2-7]xxxxx",
        "32[1-5]x[2-7]xxxxx",
        "326[2-7]xxxxxx",
        "33[2-7]xxxxxxx",
        "34[13][2-7]xxxxxx",
        "342[0189][2-7]xxxxx",
        "342[2-7]xxxxxx",
        "34[5-8]x[2-7]xxxxx",
        "35[125689]x[2-7]xxxxx",
        "35[34][2-7]xxxxxx",
        "36[01489][2-7]xxxxxx",
        "36[235-7]x[2-7]xxxxx",
        "37[02-46][2-7]xxxxxx",
        "37[157-9]x[2-7]xxxxx",
        "38[159][2-7]xxxxxx",
        "38[2-467]x[2-7]xxxxx",
        "4[04][2-7]xxxxxxx",
        "41[14578]x[2-7]xxxxx",
        "41[36][2-7]xxxxxx",
        "42[1-47][2-7]xxxxxx",
        "42[5689]x[2-7]xxxxx",
        "43[15][2-7]xxxxxx",
        "43[2-467]x[2-7]xxxxx",
        "45[12][2-7]xxxxxx",
        "45[4-7]x[2-7]xxxxx",
        "46[0-26-9][2-7]xxxxxx",
        "46[35]x[2-7]xxxxx",
        "47[0-24-9][2-7]xxxxxx",
        "473x[2-7]xxxxx",
        "48[013-57][2-7]xxxxxx",
        "48[2689]x[2-7]xxxxx",
        "49[014-7][2-7]xxxxxx",
        "49[2389]x[2-7]xxxxx",
        "51[025][2-7]xxxxxx",
        "51[146-9]x[2-7]xxxxx",
        "52[14-8]x[2-7]xxxxx",
        "522[2-7]xxxxxx",
        "53[1346]x[2-7]xxxxx",
        "53[25][2-7]xxxxxx",
        "54[14-69]x[2-7]xxxxx",
        "54[28][2-7]xxxxxx",
        "55[12][2-7]xxxxxx",
        "55[46]x[2-7]xxxxx",
        "56[146-9]x[2-7]xxxxx",
        "56[25][2-7]xxxxxx",
        "571[2-7]xxxxxx",
        "57[2-4]x[2-7]xxxxx",
        "581[2-7]xxxxxx",
        "58[2-8]x[2-7]xxxxx",
        "59[15][2-7]xxxxxx",
        "59[246]x[2-7]xxxxx",
        "61[1358]x[2-7]xxxxx",
        "612[2-7]xxxxxx",
        "621[2-7]xxxxxx",
        "62[2457]x[2-7]xxxxx",
        "631[2-7]xxxxxx",
        "63[2-4]x[2-7]xxxxx",
        "641[2-7]xxxxxx",
        "64[235-7]x[2-7]xxxxx",
        "65[17][2-7]xxxxxx",
        "65[2-689]x[2-7]xxxxx",
        "66[13][2-7]xxxxxx",
        "66[24578]x[2-7]xxxxx",
        "671[2-7]xxxxxx",
        "67[235689]x[2-7]xxxxx",
        "674[0189][2-7]xxxxx",
        "674[2-7]xxxxxx",
        "680[2-7]xxxxxx",
        "68[1-6]x[2-7]xxxxx",
        "71[013-9]x[2-7]xxxxx",
        "712[2-7]xxxxxx",
        "72[0235-9]x[2-7]xxxxx",
        "72[14][2-7]xxxxxx",
        "73[134][2-7]xxxxxx",
        "73[2679]x[2-7]xxxxx",
        "74[1-35689]x[2-7]xxxxx",
        "74[47][2-7]xxxxxx",
        "75[15][2-7]xxxxxx",
        "75[2-46-9]x[2-7]xxxxx",
        "7[67][02-9]x[2-7]xxxxx",
        "7[67]1[2-7]xxxxxx",
        "78[013-7]x[2-7]xxxxx",
        "782[0-6][2-7]xxxxx",
        "788[0189][2-7]xxxxx",
        "788[2-7]xxxxxx",
        "79[0189]x[2-7]xxxxx",
        "79[2-7]xxxxxxx",
        "80[2-467]xxxxxxx",
        "81[1357-9]x[2-7]xxxxx",
        "816[2-7]xxxxxx",
        "82[014][2-7]xxxxxx",
        "82[235-8]x[2-7]xxxxx",
        "83[03-57-9]x[2-7]xxxxx",
        "83[126][2-7]xxxxxx",
        "84[0-24-9]x[2-7]xxxxx",
        "85xx[2-7]xxxxx",
        "86[136][2-7]xxxxxx",
        "86[2457-9]x[2-7]xxxxx",
        "87[078][2-7]xxxxxx",
        "87[1-6]x[2-7]xxxxx",
        "88[1256]x[2-7]xxxxx",
        "88[34][2-7]xxxxxx",
        "891[2-7]xxxxxx",
        "89[2-4]x[2-7]xxxxx");

    RangeTree t1 = RangeTree.from(expected);
    assertThat(t1).containsExactly(expected);
    assertThat(RangeTree.from(t1.asRangeSet())).containsExactly(expected);
  }

  @Test
  public void testThreadSafety() throws ExecutionException, InterruptedException {
    // For 10^5 this takes ~500ms. For 10^6 it starts to take non-trivial time (~10 seconds).
    int numDigits = 5;
    // At 1000 threads this starts to take non-trivial time.
    int numThreads = 100;

    // Collect 10^N ranges from "00..." to "99...", all distinct.
    List<RangeTree> ranges = Stream
        .iterate(DigitSequence.zeros(numDigits), DigitSequence::next)
        .limit((int) Math.pow(10, numDigits))
        .map(RangeTreeTest::singletonRange)
        .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(ranges, new Random(1234L));

    // Recombining all 10^N ranges should give a single combined block (i.e. "xx..."). Doing it
    // with high parallelism should test the thread safety of the concurrent interning map.
    RangeTree combined = new ForkJoinPool(numThreads)
        .submit(() -> ranges.parallelStream().reduce(RangeTree.empty(), RangeTree::union))
        .get();
    assertThat(combined).isEqualTo(ranges("x".repeat(numDigits)));
  }

  @AutoValue
  abstract static class Edge {
    static Edge of(DfaNode source, DfaNode target, DfaEdge edge) {
      return new AutoValue_RangeTreeTest_Edge(source, target, edge.getDigitMask());
    }
    abstract DfaNode source();
    abstract DfaNode target();
    abstract int digitMask();
  }

  // Range tree visitor that captures edges visited (in depth first order)
  private static final class TestVisitor implements DfaVisitor {
    List<Edge> visited = new ArrayList<>();

    @Override
    public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
      visited.add(Edge.of(source, target, edge));
      target.accept(this);
    }
  }

  RangeTree ranges(String... s) {
    return RangeTree.from(specs(s));
  }

  private static RangeSpecification spec(String s) {
    return RangeSpecification.parse(s);
  }

  private static List<RangeSpecification> specs(String... s) {
    return Stream.of(s).map(RangeSpecification::parse).collect(toImmutableList());
  }

  private static Range<DigitSequence> range(String lo, String hi) {
    return Range.closed(DigitSequence.of(lo), DigitSequence.of(hi)).canonical(domain());
  }

  private static RangeSet<DigitSequence> rangeSetOf(Range<DigitSequence>... r) {
    return ImmutableRangeSet.copyOf(asList(r));
  }

  private static RangeTree singletonRange(DigitSequence s) {
    return RangeTree.from(spec(s.toString()));
  }
}
