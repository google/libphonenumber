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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;
import static java.lang.Integer.numberOfTrailingZeros;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.RangeSet;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Minimal decision tree for matching digit sequences. A range tree represents an arbitrary set of
 * digit sequences typically grouped as a set of disjoint ranges. A range tree can be thought of
 * as equivalent to either {@code RangeSet<DigitSequence>} or a canonical
 * {@code List<RangeSpecification>}. Range trees can have set-like operations performed on them,
 * such as union and intersection.
 */
public final class RangeTree {
  /**
   * Simple API for representing nodes in the DFA during visitation. See also {@link DfaVisitor}
   * and {@link #accept(DfaVisitor)}.
   */
  public interface DfaNode {
    /** Returns whether this node can terminate. */
    boolean canTerminate();

    /** Accepts the given visitor on this node, visiting its immediate outgoing (child) edges. */
    void accept(DfaVisitor visitor);

    /** Finds the outgoing edge from this node which contains the given digit. */
    @Nullable
    DfaEdge find(int digit);

    /** Returns the list of edges leading out from this node.*/
    List<DfaEdge> getEdges();

    /**
     * Returns a bit-mask of possible lengths from this node. Bit-N is set of the sub-tree rooted
     * at this node can terminate (in any branch) at depth N. A corollary to this is that bit-0 is
     * set if this node can terminate.
     */
    int getLengthMask();
  }

  /**
   * Simple API for representing edges in the DFA during visitation. See also {@link DfaVisitor}
   * and {@link #accept(DfaVisitor)}.
   */
  public interface DfaEdge {
    /** Returns a bit-mask of the digits accepted by this edge. */
    int getDigitMask();

    /** Returns the target node of this edge. */
    DfaNode getTarget();
  }

  /**
   * Visitor API for traversing edges in {@code RangeTrees}. When a node accepts a visitor, it
   * visits only the immediate outgoing edges of that node. If recursive visitation is required, it
   * is up to the visitor to call {@link DfaNode#accept(DfaVisitor)} during visitation.
   *
   * <p>Graph nodes and edges obey {@code Object} equality and this can be used by visitor
   * implementations to track which nodes have been reached if they only wish to visit each edge
   * once (e.g. storing visited nodes in a set or map).
   */
  public interface DfaVisitor {
    /**
     * Visits an edge in the DFA graph of a {@code RangeTree} as the result of a call to
     * {@link DfaNode#accept(DfaVisitor)} on the source node.
     */
    void visit(DfaNode source, DfaEdge edge, DfaNode target);
  }

  /**
   * A single node within a range tree. Nodes are really just a specialized implementation of a
   * node in a deterministic finite state automaton (DFA), and {@link RangeTree} instances are just
   * wrappers around a single "root" node.
   * <p>
   * Nodes have outgoing {@link Edge}s but may optionally allow termination of matching operations
   * when they are reached (this is the same as in DFAs). Unlike DFAs however, the out-edges of a
   * node are grouped according to a mask of all digits which can reach the same target node. For
   * node {@code A}, which can reach a target node {@code B} via digits {@code {1, 2, 3, 7, 9 }},
   * there is a single edge labeled with the bitmask {@code 0x287} (binary {@code 1010001110}) as
   * opposed to 5 separate edges, each marked with a single digit.
   * <p>
   * This approach more closely matches the data representations in classes like {@link
   * RangeSpecification} and affords additional efficiencies compared to the {@code JAutomata}
   * library, which has performance issues when processing large trees.
   */
  private static final class Node implements DfaNode {
    /** The unique "terminal" node which must be the final node in all paths in all RangeTrees. */
    private static final Node TERMINAL = new Node();

    /**
     * The list of edges, ordered by the lowest bit in each mask. The masks for each edge must be
     * mutually disjoint. Only the terminal node is permitted to have zero outbound edges.
     */
    private final ImmutableList<Edge> edges;

    /**
     * Derived bit-packed table of digit-number to edge-index. The edge index for digit {@code 'd'}
     * is stored in 4-bits, starting at bit {@code 4 * d}. This is useful as it makes finding an
     * outbound edge a constant time operation (instead of having to search through the list of
     * edges).
     */
    private final long jumpTable;

    /** A cached value of the total number of unique digits sequences matched by this DFA. */
    private final long matchCount;

    /**
     * Mask of all possible lengths of digit sequences this node can match. If bit-N is set then
     * this node can match at least one input sequence of length N. Note that this includes bit-0,
     * which is matched if the node itself can terminate. It is even possible to have a range tree
     * containing only the terminal node which matches only the empty digit sequence (this is
     * distinct from the "empty" tree which matches no sequences at all).
     */
    private final int lengthMask;

    /** Nodes are used a keys during graph interning so we cache the hashcode. */
    private final int hashcode;

    // Only for the terminal node.
    private Node() {
      this.edges = ImmutableList.of();
      this.jumpTable = -1L;
      // Unlike the empty tree, the terminal node matches one input sequence, the empty sequence.
      // The empty tree on the other hand doesn't even reference a terminal node, so there is no
      // possible sequence it can match.
      this.matchCount = 1L;
      this.lengthMask = 1;
      this.hashcode = -1;
    }

    // A node is defined entirely from its set of edges and whether it can terminate.
    private Node(ImmutableList<Edge> edges, boolean canTerminate) {
      checkArgument(!edges.isEmpty());
      this.edges = edges;
      // Everything below here is derived information from the edges and termination flag.
      int lengthMask = 0;
      // Set all bits in the jump table (so each 4-bit entry is '-1' unless otherwise overwritten).
      long jumpTable = -1L;
      int outMask = 0;
      int lastLowBit = -1;
      // If we can terminate we get an additional match count for the sequence that reaches us, but
      // we may get more for longer sequences matched by nodes we link to.
      long matchCount = canTerminate ? 1 : 0;
      for (int n = 0; n < edges.size(); n++) {
        Edge e = edges.get(n);
        // Make sure edges are disjoint (edges masks are already known to be valid individually).
        checkArgument((outMask & e.mask) == 0, "edge masks not disjoint: %s", e);
        outMask |= e.mask;
        // Make sure edges are ordered as expected.
        int lowBit = numberOfTrailingZeros(e.mask);
        checkArgument(lowBit > lastLowBit, "edge masks not ordered: %s", e);
        lastLowBit = lowBit;
        // Work out what the match count is based on the counts of everything we link to (the sum
        // of all the counts of our target nodes weighted by how many times we link to them).
        matchCount += Integer.bitCount(e.mask) * e.target.matchCount;
        // Build up a mask of all the lengths that our target node can contain.
        lengthMask |= e.target.lengthMask;
        // For each bit in the edge mask, set the 4-bit nibble in the jump table to the edge index.
        for (int d = 0; d <= 9; d++) {
          if ((e.mask & (1 << d)) != 0) {
            // We rely on the jump table entry having all its bits set here (true from above).
            // n = 1010 (9).
            // (n ^ 1111) << (4d) == 0000.0101.0000...
            // xxxx.1111.yyyy... ^ 0000.0101.0000... == xxxx.1010.yyyy
            jumpTable ^= (n ^ 0xFL) << (4 * d);
          }
        }
      }
      this.jumpTable = jumpTable;
      this.matchCount = matchCount;
      // Our length set is one more than all our targets (including bit zero if we can terminate).
      this.lengthMask = (lengthMask << 1) | (canTerminate ? 1 : 0);
      // Caching the hashcode makes interning faster (note that this is not recursive because the
      // hashcode of an Edge relies on the identity hashcode of the target nodes).
      this.hashcode = edges.hashCode() ^ Boolean.hashCode(canTerminate);
    }

    /**
     * Returns the target node for the given input digit, or {@code null} if there is no out-edge
     * for that digit.
     */
    Node findTarget(int digit) {
      checkArgument(0 <= digit && digit <= 9);
      return targetFromJumpTableIndex((int) (jumpTable >>> (4 * digit)) & 0xF);
    }

    /** Helper to get the target node from an edge index (rather than a digit value). */
    private Node targetFromJumpTableIndex(int n) {
      return (n != 0xF) ? edges.get(n).target : null;
    }

    @Nullable
    @Override
    public DfaEdge find(int digit) {
      checkArgument(0 <= digit && digit <= 9);
      int jumpTableIndex = (int) (jumpTable >>> (4 * digit)) & 0xF;
      return (jumpTableIndex != 0xF) ? edges.get(jumpTableIndex) : null;
    }

    @Override
    public boolean canTerminate() {
      return (lengthMask & 1) != 0;
    }

    @Override
    public void accept(DfaVisitor visitor) {
      for (Edge e : edges) {
        visitor.visit(this, e, e.target);
      }
    }

    @Override
    public List<DfaEdge> getEdges() {
      // NOTE: This DOES NOT make a copy (or any allocations), since ImmutableList is clever
      // enough to know that a list of <Bar extends Foo> is also a List<Foo> if they are
      // unmodifiable. It's a clever cast essentially!
      return ImmutableList.copyOf(edges);
    }

    @Override
    public int getLengthMask() {
      return lengthMask;
    }

    /**
     * Returns whether this node is interchangeable with the given instance. Equality of
     * {@code Node} instances is not "deep" equality and is carefully designed to make constructing
     * minimal range trees easier. Two nodes are equal if they have the same set of edges and
     * termination flag; however edges are equal only if they point to exactly the same target
     * instances. This is carefully designed to make "interning" work efficiently and avoid
     * unwanted recursion during various operations.
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Node)) {
        return false;
      }
      Node tree = (Node) o;
      return edges.equals(tree.edges) && canTerminate() == tree.canTerminate();
    }

    @Override
    public int hashCode() {
      return hashcode;
    }
  }

  /**
   * A directed edge to a target {@link Node}. Note that edge equality is based on instance identity
   * as part of the interning semantics of range trees.
   */
  private static final class Edge implements DfaEdge {
    /** Bit mask of digit values this edge accepts (bit-N set implies this edge accepts digit N). */
    private final int mask;
    /** Target node (or node function) this edge points at. */
    private final Node target;

    private Edge(int mask, Node target) {
      checkArgument(mask > 0 && mask <= RangeSpecification.ALL_DIGITS_MASK);
      this.mask = mask;
      this.target = checkNotNull(target);
    }

    /** Returns a new edge with the same target whose mask is OR'ed with the given value. */
    Edge merge(int m) {
      return new Edge(mask | m, target);
    }

    @Override
    public int getDigitMask() {
      return mask;
    }

    @Override
    public DfaNode getTarget() {
      return target;
    }

    /**
     * Edges are equal only if the point to exactly the same targets. This is important to avoid
     * expensive recursive equality checking during common operations.
     */
    @Override
    @SuppressWarnings("ReferenceEquality")
    public boolean equals(Object o) {
      if (!(o instanceof Edge)) {
        return false;
      }
      Edge other = (Edge) o;
      return mask == other.mask && target == other.target;
    }

    @Override
    public int hashCode() {
      return mask ^ System.identityHashCode(target);
    }

    /** The natural string representation of an edge is the set of digits it accepts. */
    @Override
    public String toString() {
      return RangeSpecification.toString(mask);
    }
  }

  /**
   * Implementation of set-like operations for range trees. As well as having well defined set-like
   * operations (union, intersection etc...), range trees are also minimal DFAs, ensuring that no
   * duplication of sub-trees occurs. This class implements set-like operations efficiently using
   * recursive interning of nodes and always produces minimal results by construction. This
   * approach is similar to (but not the same as) dynamic programming.
   * <p>
   * Note that the terms "interning" and "minimizing" of range trees are related but not the same.
   * Interning only makes sense in relation to some sequence of operations (and is a pure
   * implementation detail for efficiency reasons). A minimal sub-tree exists outside of logical
   * operations and may be the result of a logical operation, or something else. Minimization of
   * the DFA represented by a range tree is a user concept.
   * <p>
   * Currently all logical operations produce minimal DFAs, but a minimal DFA can come from other
   * sources (the DFA generated for a single range specification is minimal by construction).
   */
  public static final class SetOperations {
    // A weak reference wrapper around a Node allowing it to be garbage collected when not needed.
    private static final class WeakNodeRef extends WeakReference<Node> {
      // This MUST be cached since it cannot be calculated once the Node is garbage collected, and
      // it's required that keys in maps do not change their hashcode.
      private final int hashCode;

      public WeakNodeRef(Node referent, ReferenceQueue<? super Node> q) {
        super(checkNotNull(referent), q);
        this.hashCode = referent.hashCode();
      }

      @Override
      public int hashCode() {
        return hashCode;
      }

      /*
       * This is very subtle. To avoid multiple "cleared" references becoming equal to each other
       * (which violates the expectations of a map) we consider other nodes equal only if either:
       * 1) they are the same instance
       * 2) they have a non-null referent that's equal() to our referent
       *
       * Two distinct cleared references with the same hashcode must not compare as equal.
       */
      @Override
      public boolean equals(Object obj) {
        if (obj == this) {
          return true;
        }
        // Don't worry about checking "instanceof" since this is a private type.
        Node referent = get();
        return referent != null && referent.equals(((WeakNodeRef) obj).get());
      }
    }

    /** Minimal API for any logical operation which can be applied to two range trees. */
    interface LogicalOperation extends BiFunction<Node, Node, Node> { }

    /**
     * Implementation of the "union" operation for two sub-trees. The union of two sub-trees is a
     * sub-tree which matches digit sequences if-and-only-if they are matched by either of the
     * original sub-trees.
     */
    private final class Union implements LogicalOperation {
      @Override
      @SuppressWarnings("ReferenceEquality")
      public Node apply(Node lhs, Node rhs) {
        // Assert that inputs are always interned.
        // NOTE: It might be worth doing checks for "TERMINAL" here as well.
        if (lhs == rhs || rhs == null) {
          // (A ∪ A) = A and (A ∪ ∅) = A
          return lhs;
        } else if (lhs == null) {
          // (∅ ∪ B) = B
          return rhs;
        }
        return recurse(this, lhs, rhs, lhs.canTerminate() || rhs.canTerminate());
      }
    }

    /**
     * Implementation of the "intersection" operation for two sub-trees. The intersection of two
     * sub-trees is a sub-tree which matches digit sequences if-and-only-if they are matched by
     * both the original sub-trees.
     */
    private final class Intersection implements LogicalOperation {
      @Override
      @SuppressWarnings("ReferenceEquality")
      public Node apply(Node lhs, Node rhs) {
        // Assert that inputs are always interned.
        // NOTE: It might be worth doing checks for "TERMINAL" here as well.
        if (lhs == rhs) {
          // (A ∩ A) = A
          return lhs;
        } else if (lhs == null || rhs == null) {
          // (∅ ∩ X) = ∅ for any X
          return null;
        }
        return recurse(this, lhs, rhs, lhs.canTerminate() && rhs.canTerminate());
      }
    }

    /**
     * Implementation of the "subtraction" operation for two sub-trees. The subtraction of two
     * sub-trees {@code A} and {@code B}, is a sub-tree which matches digit sequences if-and-only-if
     * they are matched by {@code A} but not {@code B}. This is not a symmetrical operation.
     */
    private final class Subtraction implements LogicalOperation {
      @Override
      @SuppressWarnings("ReferenceEquality")
      public Node apply(Node lhs, Node rhs) {
        // Assert that inputs are always interned.
        // NOTE: It might be worth doing checks for "TERMINAL" here as well.
        if (lhs == rhs || lhs == null) {
          // (A ∖ A) = ∅ and (∅ ∖ B) = ∅
          return null;
        } else if (rhs == null) {
          // (A ∖ ∅) = A
          return lhs;
        }
        return recurse(this, lhs, rhs, lhs.canTerminate() && !rhs.canTerminate());
      }
    }

    private final class Filter implements LogicalOperation {
      // IMPORTANT: The prefix is neither returned, nor tested directly against instances in the
      // range tree being filtered (other than the singleton TERMINAL node) which means it need
      // not be interned before calling this function. If this method were ever changed to return
      // nodes from the prefix tree or test instance equality with (interned) nodes in the range
      // tree, then the prefix tree must also be interned before this method is called.
      @Override
      @SuppressWarnings("ReferenceEquality")
      public Node apply(Node prefix, Node range) {
        // Assert that ranges are always interned (prefixes don't need to be since we never return
        // nodes in the prefix tree to form part of the filtered range).
        if (prefix == null || range == null) {
          return null;
        }
        // If we get to the end of the prefix, just return whatever's left in the range.
        if (prefix == Node.TERMINAL) {
          return range;
        }
        // Still "in" the prefix but we hit the end of the range (e.g. "123".filter("12") == ∅
        if (range == Node.TERMINAL) {
          return null;
        }
        // Since we only recurse while still "in" the prefix we are never terminating (e.g.
        // "123".filter({"12", "1234"}) == {"1234"} and does not contain "12").
        return recurse(this, prefix, range, false);
      }
    }

    // Singleton set operations instance to handle interning of nodes.
    static final SetOperations INSTANCE = new SetOperations();

    /**
     * Weak-referenced interning map. This cannot be a standard Guava {@code Interner} because it
     * must recursively intern the targets of any nodes to ensure that once a Node is interned, all
     * the nodes reachable from it are also interned.
     */
    private final Map<WeakNodeRef, WeakNodeRef> interningMap = new ConcurrentHashMap<>();

    /**
     * Referent queue onto which node references clear by GC will be put. The elements in this
     * queue should correspond to unused entries in the map which need to be tidied up.
     */
    private final ReferenceQueue<Node> tidyUpQueue = new ReferenceQueue<>();

    private final LogicalOperation unionFn = new Union();
    private final LogicalOperation intersectionFn = new Intersection();
    private final LogicalOperation subtractionFn = new Subtraction();
    private final LogicalOperation retainFromFn = new Filter();

    private SetOperations() {
      intern(Node.TERMINAL);
    }

    /**
     * Interns the target of an edge (this does not make the edge itself interned, but it does
     * allow edges to be efficiently compared via their targets). If the target of the given edge
     * was already interned then it is just returned.
     */
    @SuppressWarnings("ReferenceEquality")
    private Edge internTarget(Edge edge) {
      Node target = intern(edge.target);
      return (target == edge.target) ? edge : new Edge(edge.mask, target);
    }

    /**
     * Recursively interns a node and all nodes reachable from it. Note that if the given nodes do
     * not represent a minimal DFA, then the interning process itself won't necessarily produce a
     * minimal result. The minimal DFA property of range trees exists by induction and assumes that
     * all trees are constructed minimally and that logical operations produce minimal trees. Note
     * that if necessary, the interning operation could ensure minimization but at the cost of some
     * efficiency (you would use the EDGE_COLLECTOR to squash duplicate edges).
     */
    private Node intern(Node node) {
      WeakNodeRef ref = new WeakNodeRef(node, tidyUpQueue);
      WeakNodeRef existingRef = interningMap.get(ref);
      if (existingRef != null) {
        // Claim strong reference once into a local variable.
        Node interned = existingRef.get();
        if (interned != null) {
          // Clear "ref" to prevent it going in the tidy-up queue (it wasn't added to the map).
          ref.clear();
          return interned;
        }
      }
      // In the vast majority of cases, the edges of the node being interned already reference
      // interned targets. The returned list contains edges to (recursively) interned nodes.
      // If the edges we get back are the edges of our node, we just need to add ourselves to the
      // intern map. If our edges were not interned (and we aren't in the map yet) then we must
      // make a duplicate node that has only the interned edges before adding it to the map. This
      // preserves the property that interned nodes only ever connect to other interned nodes.
      ImmutableList<Edge> edges =
          node.edges.stream().map(this::internTarget).collect(toImmutableList());
      if (!node.edges.equals(edges)) {
        // Clear the original reference before overwriting the node to avoid it being put on the
        // tidy-up queue (otherwise as soon as "node" is overwritten, GC could enqueue "ref").
        ref.clear();
        // Create a new node with interned edges and a corresponding weak reference.
        node = new Node(edges, node.canTerminate());
        ref = new WeakNodeRef(node, tidyUpQueue);
      }
      // Consider the race condition where another thread added this node in the meantime. We
      // cannot obtain a strong reference until after the WeakNode is returned from the map, which
      // means there's always a race condition under which the referenced node will be collected.
      while (true) {
        existingRef = interningMap.putIfAbsent(ref, ref);
        if (existingRef == null) {
          // Easy case: We succeeded in putting our new reference into the map, so return our node.
          return node;
        }
        // There's still a risk that the reference became null after being found in the map.
        Node interned = existingRef.get();
        if (interned != null) {
          // Clear "ref" to prevent it going in the tidy-up queue (it wasn't added to the map).
          ref.clear();
          return interned;
        }
        // The reference must have been garbage collected after the weak node was found. This is
        // very rare but possible, and the only real strategy is to try again. We can't really end
        // up in a loop here unless we're continuously garbage collecting (i.e. bigger problems).
        // We can't find the same reference again (it was cleared and is no longer equal-to "ref").
      }
    }

    // Remove all WeakNodeRefs that have been cleared by the garbage collector. This should
    // precisely account for all weak nodes in the interning map which have been cleared by the
    // garbage collector (weak nodes that were never added to the map are cleared manually and
    // should not appear in the tidy-up queue).
    private void tidyUpInterningMap() {
      Reference<?> ref;
      while ((ref = tidyUpQueue.poll()) != null) {
        interningMap.remove(ref);
      }
    }

    /**
     * Applies the given operation recursively to a pair of interned nodes. The resulting node is
     * interned and (if the input nodes were both minimal) minimal.
     */
    @SuppressWarnings("ReferenceEquality")
    Node recurse(LogicalOperation logicalOp, Node lhs, Node rhs, boolean canTerminate) {
      // Stage 1: Use the jump tables of target nodes to make a lookup of input edge index and mask.
      //
      // Each entry in the 'inputMap' array is a coded integer containing:
      // [ lhs edge index | rhs edge index | bitmask of edges ]
      // [  bits 20-24    |  bits 16-20    |  bits 0-10       ]
      //
      // Basically the top 16 bits are the indices for the inputs to the logical operation and the
      // lower 16 bits are the mask of edge indices to which that result will apply. Because the
      // map is constructed to avoid any duplication of the indices (the 'inputKey') we ensure that
      // the logical operation is applied to the minimal number of unique inputs (no duplication).
      long lhsJumpTable = lhs.jumpTable;
      long rhsJumpTable = rhs.jumpTable;
      // Note: Could reuse from a  field (no longer thread safe, but that might be fine)
      int[] inputMap = new int[10];
      int mapSize = 0;
      // The digit mask runs from bit-0 (1) to bit-9.
      for (int digitMask = 1; digitMask <= (1 << 9); digitMask <<= 1) {
        int inputKey = (int) (((lhsJumpTable & 0xF) << 4) | (rhsJumpTable & 0xF));
        int n;
        for (n = 0; n < mapSize; n++) {
          if ((inputMap[n] >> 16) == inputKey) {
            // Add this digit to an existing entry in the input map (the inputs are the same).
            inputMap[n] |= digitMask;
            break;
          }
        }
        if (n == mapSize) {
          // Add this digit to a new entry in the input map (and increase the map size).
          inputMap[n] = (inputKey << 16) | digitMask;
          mapSize++;
        }
        lhsJumpTable >>= 4;
        rhsJumpTable >>= 4;
      }
      // Stage 2: Given a minimal set in inputs, perform the minimal number of logical operations.
      // Note however that two operations can often return the same value (especially null or the
      // TERMINAL node) so we have to minimize the results again and merge identical targets and
      // masks.
      List<Edge> out = new ArrayList<>();
      for (int n = 0; n < mapSize; n++) {
        int mask = inputMap[n];
        // If lhs and rhs nodes are interned, then every target they reference is interned.
        // We also assert that any nodes returned by logical operations are also interned.
        Node node = logicalOp.apply(
            lhs.targetFromJumpTableIndex((mask >> 20) & 0xF),
            rhs.targetFromJumpTableIndex((mask >> 16) & 0xF));
        if (node == null) {
          continue;
        }
        // Mask out the upper bits that are no longer needed.
        mask &= RangeSpecification.ALL_DIGITS_MASK;
        // Find if the result of the logical operation matches an existing result in the edge list.
        int idx;
        for (idx = 0; idx < out.size(); idx++) {
          Edge e = out.get(idx);
          if (e.target == node) {
            // We matched an existing result, so replace the existing entry with a new edge which
            // points to the same target but includes the new digits that also share this result.
            out.set(idx, e.merge(mask));
            break;
          }
        }
        if (idx == out.size()) {
          // This is the first time this result was seen, so add it in a new entry.
          out.add(new Edge(mask, node));
        }
      }
      // Stage 3: Given a minimal list of final edges (and after checking for degenerate cases
      // (empty or terminating nodes) create and intern a new minimal node for the edges.
      if (out.isEmpty()) {
        return canTerminate ? Node.TERMINAL : null;
      } else {
        return intern(new Node(ImmutableList.copyOf(out), canTerminate));
      }
    }

    /**
     * Returns the (minimal) logical union {@code '∪'} of two (minimal) sub-trees rooted at the
     * given nodes. The sub-trees are interned before recursively applying the "union" function.
     */
    private Node unionImpl(Node lhs, Node rhs) {
      if (lhs == null) {
        return rhs;
      } else if (rhs == null) {
        return lhs;
      } else {
        return unionFn.apply(intern(lhs), intern(rhs));
      }
    }

    /**
     * Returns the (minimal) logical intersection {@code '∩'} of two (minimal) sub-trees rooted at
     * the given nodes. The sub-trees are interned before recursively applying the "union" function.
     */
    private Node intersectImpl(Node lhs, Node rhs) {
      if (lhs == null || rhs == null) {
        return null;
      } else {
        return intersectionFn.apply(intern(lhs), intern(rhs));
      }
    }

    /**
     * Returns the (minimal) logical subtraction {@code '∖'} of two (minimal) sub-trees rooted at
     * the given nodes. The sub-trees are interned before recursively applying the "union" function.
     */
    private Node subtractImpl(Node lhs, Node rhs) {
      if (lhs == null) {
        return null;
      } else if (rhs == null) {
        return lhs;
      } else {
        return subtractionFn.apply(intern(lhs), intern(rhs));
      }
    }

    private Node retainFromImpl(Node prefix, Node range) {
      if (prefix == null || range == null) {
        return null;
      }
      // As this operation never returns nodes that were in the prefix tree, or tests if prefix
      // nodes are the same as instances in the range tree, there's no need to intern it.
      return retainFromFn.apply(prefix, intern(range));
    }

    /** Returns the union of one or more range trees. */
    private RangeTree union(RangeTree first, RangeTree... rest) {
      Node node = first.root;
      for (RangeTree t : rest) {
        node = unionImpl(node, t.root);
      }
      tidyUpInterningMap();
      return newOrEmptyTree(node);
    }

    /**
     * Returns the union of two prefix trees. For prefix trees {@code p1} and {@code p2}, the union
     * {@code P = p1.union(p2)} is defined such that:
     * <pre>{@code
     *   P.filter(R) = p1.filter(R).union(p2.filter(R))
     * }</pre>
     * If prefixes are the same length this is equivalent to {@link RangeTree#union(RangeTree)},
     * but when prefixes overlap, only the more general (shorter) prefix is retained.
     */
    PrefixTree union(PrefixTree lhs, PrefixTree rhs) {
      // Using one prefix tree (A) to filter another (B), gives you the set of ranges in B which
      // are, at least, also contained in A. The union of two prefix trees need only contain the
      // more general (shorter) prefix and the more specific (longer) prefixes must be removed
      // since they overlap with the more general ones.
      //
      // For example "12".retainFrom("1234") == "1234", but we don't want to retain "1234" in the
      // final range tree (since it will already contain "12" anyway).
      //
      // If the same prefix exists in both inputs however, just doing this subtraction would remove
      // it from the result (which is not what we want), so we also include the intersection of the
      // prefix ranges.
      RangeTree ltree = lhs.asRangeTree();
      RangeTree rtree = rhs.asRangeTree();
      return PrefixTree.from(union(
          // Prefixes in both inputs (which would otherwise be removed by the subtractions below).
          intersect(ltree, rtree),
          // Prefixes in "lhs" which are strictly more general than any prefix in "rhs"
          subtract(ltree, retainFrom(rhs, ltree)),
          // Prefixes in "rhs" which are strictly more general than any prefix in "lhs"
          subtract(rtree, retainFrom(lhs, rtree))));
    }

    /** Returns the intersection of one or more range trees. */
    private RangeTree intersect(RangeTree first, RangeTree... rest) {
      Node node = first.root;
      for (RangeTree t : rest) {
        node = intersectImpl(node, t.root);
      }
      tidyUpInterningMap();
      return newOrEmptyTree(node);
    }

    /**
     * Returns the intersection of two prefix trees. For prefix trees {@code p1} and {@code p2},
     * the intersection {@code P = p1.intersect(p2)} is defined such that:
     * <pre>{@code
     *   P.filter(R) = p1.filter(R).intersect(p2.filter(R))
     * }</pre>
     * If prefixes are the same length this is equivalent to {@link RangeTree#intersect(RangeTree)},
     * but when prefixes overlap, only the more specific (longer) prefix is retained.
     */
    PrefixTree intersect(PrefixTree lhs, PrefixTree rhs) {
      return PrefixTree.from(union(
          // Prefixes in "lhs" which are the same or more specific as any prefix in "rhs"
          retainFrom(rhs, lhs.asRangeTree()),
          // Prefixes in "rhs" which are the same or more specific as any prefix in "lhs"
          retainFrom(lhs, rhs.asRangeTree())));
    }

    /** Returns the difference of two range trees, {@code lhs - rhs}. */
    private RangeTree subtract(RangeTree lhs, RangeTree rhs) {
      Node node = subtractImpl(lhs.root, rhs.root);
      tidyUpInterningMap();
      return newOrEmptyTree(node);
    }

    /**
     * Returns a subset of the given ranges, containing only ranges which are prefixed by an
     * element in the given prefix tree. For example:
     * <pre> {@code
     *   RangeTree r = { "12xx", "1234x" }
     *   PrefixTree p = { "12[0-5]" }
     *   retainFrom(p, r) = { "12[0-5]x", "1234x"}
     * }</pre>
     * Note that if the prefix tree is empty, this method returns the empty range tree.
     */
    RangeTree retainFrom(PrefixTree prefixes, RangeTree ranges) {
      Node node = retainFromImpl(prefixes.asRangeTree().root, ranges.root);
      tidyUpInterningMap();
      return newOrEmptyTree(node);
    }
  }

  /**
   * Returns a minimal range tree for the given specification. The tree has only one path and only
   * matches digit sequences of the same length as the given specification.
   */
  public static RangeTree from(RangeSpecification s) {
    Node node = Node.TERMINAL;
    for (int n = s.length() - 1; n >= 0; n--) {
      node = new Node(ImmutableList.of(new Edge(s.getBitmask(n), node)), false);
    }
    return newOrEmptyTree(node);
  }

  /**
   * Returns a minimal range tree for the given specifications. This tree is formed as the logical
   * union of the trees for all given specifications.
   */
  public static RangeTree from(Iterable<RangeSpecification> specs) {
    SetOperations setOps = SetOperations.INSTANCE;
    Node node = null;
    for (RangeSpecification s : specs) {
      node = setOps.unionImpl(node, from(s).root);
    }
    setOps.tidyUpInterningMap();
    return newOrEmptyTree(node);
  }

  /**
   * Returns a minimal range tree for the given specifications. This tree is formed as the logical
   * union of the trees for all given specifications.
   */
  public static RangeTree from(Stream<RangeSpecification> specs) {
    return from(specs.collect(toImmutableList()));
  }

  /**
   * Returns a minimal range tree for the given digit sequence ranges. This tree is formed as the
   * logical union of all the range specifications derived from the given ranges.
   */
  public static RangeTree from(RangeSet<DigitSequence> ranges) {
    // Currently we don't accept an empty range set in RangeSpecification.from().
    return !ranges.isEmpty() ? from(RangeSpecification.from(ranges)) : RangeTree.empty();
  }

  /**
   * Returns a range tree whose root is the given DfaNode. The given node must have been found by
   * visiting an existing range tree. This method is useful for efficiently implementing "sub tree"
   * logic in some cases.
   *
   * @throws IllegalArgumentException if the given node did not come from a valid range tree.
   */
  @SuppressWarnings("ReferenceEquality")
  public static RangeTree from(DfaNode root) {
    checkNotNull(root, "root node cannot be null");
    checkArgument(root instanceof Node,
        "invalid root node (wrong type='%s'): %s", root.getClass(), root);
    Node node = (Node) root;
    // Reference equality is correct since this is testing for interning.
    checkArgument(node == SetOperations.INSTANCE.intern(node),
        "invalid root node (not from valid RangeTree): %s", node);
    return new RangeTree(node);
  }

  private static final RangeTree EMPTY = new RangeTree();

  /** Returns the enpty range tree, which matches only the empty digit sequence. */
  public static RangeTree empty() {
    return EMPTY;
  }

  private static RangeTree newOrEmptyTree(Node node) {
    return node != null ? new RangeTree(node) : EMPTY;
  }

  /**
   * The root node, possibly null to signify the "empty" tree which matches no possible digit
   * sequences (this is distinct from a tree that matches only the empty digit sequence).
   */
  private final Node root;
  private final long matchCount;
  private final Supplier<ImmutableSortedSet<Integer>> lengths;
  // Cached on demand.
  private Integer hashCode = null;

  /** Constructor for the singleton empty tree. */
  private RangeTree() {
    this.root = null;
    // Unlike the terminal node (which matches the empty sequence), the empty tree matches nothing.
    this.matchCount = 0L;
    this.lengths = Suppliers.ofInstance(ImmutableSortedSet.of());
  }

  /** Constructor for a non-empty tree. */
  private RangeTree(Node root) {
    this.root = Preconditions.checkNotNull(root);
    this.matchCount = root.matchCount;
    this.lengths = Suppliers.memoize(() -> calculateLengths(root));
  }

  /**
   * Returns whether this range tree accepts any input sequences. Note that in theory a range tree
   * could accept the empty digit sequence (but in that case it would not be empty). An empty range
   * tree cannot match any possible sequence.
   */
  public boolean isEmpty() {
    return root == null;
  }

  private static ImmutableSortedSet<Integer> calculateLengths(Node root) {
    // Length mask cannot be 0 as it must match (at least) sequences of length 0.
    int lengthMask = root.lengthMask;
    ImmutableSortedSet.Builder<Integer> lengths = ImmutableSortedSet.naturalOrder();
    do {
      int length = numberOfTrailingZeros(lengthMask);
      lengths.add(length);
      // Clear each bit as we go.
      lengthMask &= ~(1 << length);
    } while (lengthMask != 0);
    return lengths.build();
  }

  /** Returns the set of digit sequence lengths which could be matched by this range tree. */
  public ImmutableSortedSet<Integer> getLengths() {
    return lengths.get();
  }

  /**
   * Returns the smallest digit sequence which will be accepted by this range tree, in
   * {@link DigitSequence} order. Note that this is not the same as calling {@code sample(0)},
   * since {@link #sample(long)} does not use {@code DigitSequence} order.
   *
   * @return the smallest digit sequence accepted by this tree.
   * @throws IllegalStateException if the tree is empty.
   */
  public DigitSequence first() {
    checkState(!isEmpty(), "cannot get minimum sequence for an empty range tree");
    DigitSequence first = DigitSequence.empty();
    Node node = root;
    int minLength = Integer.numberOfTrailingZeros(root.lengthMask);
    if (minLength > 0) {
      // Length mask is the mask for checking against the target node's length(s), so we pre-shift
      // it by one (i.e. not "1 << minLength"). This is also why there needs to be a zero check
      // around this loop since otherwise we would not correctly detect when the empty sequence was
      // in the tree (we don't check the root node in this loop).
      for (int lengthMask = 1 << (minLength - 1); lengthMask > 0; lengthMask >>>= 1) {
        for (Edge e : node.edges) {
          // Exit when we find the first edge for which the minimum length path can be reached.
          // This is only possible for first() because edges are ordered by their minimum digit
          // (you could not use a similar trick to implement a last() method). This break must be
          // reached since at least once edge _must_ have the expected length bit set.
          if ((e.target.lengthMask & lengthMask) != 0) {
            first = first.extendBy(DigitSequence.singleton(Integer.numberOfTrailingZeros(e.mask)));
            node = e.target;
            break;
          }
        }
      }
    }
    return first;
  }

  /**
   * Returns a digit sequence in the range tree for a given sampling index (in the range
   * {@code 0 <= index < size()}). Note that this method makes no promises about the specific
   * ordering used since it is dependant on the internal tree structure.
   *
   * <p>However the mapping from index to sequence is guaranteed to be a bijection, so while it is
   * not true that {@code sample(n).next().equals(sample(n+1))}, it is true that
   * {@code sample(n).equals(sample(m))} if-and-only-if {@code n == m}. Thus a pseudo random sample
   * of N distinct indices will result in N distinct sequences.
   *
   * <p>This method is not recommended for general iteration over a tree, since there can be
   * trillions of digit sequences.
   *
   * @throws ArrayIndexOutOfBoundsException if the index is invalid.
   */
  public DigitSequence sample(long index) {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException(
          String.format("index (%d) out of bounds [0...%d]", index, size()));
    }
    return recursiveGet(root, index);
  }

  @SuppressWarnings("ReferenceEquality")  // Nodes are interned.
  private static DigitSequence recursiveGet(Node node, long index) {
    // We can assert that 0 <= index < node.matchCount by inspection (checked initially and true
    // by code inspection below).
    if (node.canTerminate()) {
      // Every recursion should end here (since at some point we traverse the final edge where the
      // index has been reduced to zero). However we also get here while still in the tree and must
      // decide whether to terminate if we see a terminating node.
      if (index == 0) {
        return DigitSequence.empty();
      }
      // Subtract 1 to account for this early terminating digit sequence (it is reflected in the
      // match count so we must adjust our index before moving on).
      index -= 1;
    }
    // Should always have at least one out edge here so the mask isn't empty.
    checkState(node != Node.TERMINAL, "!!! Bad RangeTree !!!");
    for (Edge e : node.edges) {
      long weightedCount = e.target.matchCount * Integer.bitCount(e.mask);
      if (index >= weightedCount) {
        // We are not following this edge, so adjust index and continue.
        index -= weightedCount;
        continue;
      }
      // Find which digit of the edge we are traversing. If we are in the Nth copy of the match
      // count we want the digit corresponding to the Nth bit in the edge mask. Achieve this by
      // repeatedly removing the lowest set bit each time around the loop (getting the lowest set
      // bit as a mask is way faster than getting it's bit position).
      int mask = e.mask;
      while (index >= e.target.matchCount) {
        index -= e.target.matchCount;
        mask &= ~Integer.lowestOneBit(mask);
      }
      return DigitSequence.singleton(Integer.numberOfTrailingZeros(mask))
          .extendBy(recursiveGet(e.target, index));
    }
    // Should be impossible since we should always find an edge for the current index. If we get
    // here something is very messed up with either this code or the internal data structure.
    throw new IllegalStateException("!!! Bad RangeTree !!!");
  }

  /** Returns the number of unique digit sequences contained in this range tree. */
  public long size() {
    return matchCount;
  }

  // -------- Set-like operations --------

  /** Returns the minimal logical union of this instance and the given tree. */
  public RangeTree union(RangeTree tree) {
    return SetOperations.INSTANCE.union(this, tree);
  }

  /** Returns the minimal logical intersection of this instance and the given tree. */
  public RangeTree intersect(RangeTree tree) {
    return SetOperations.INSTANCE.intersect(this, tree);
  }

  /** Returns the minimal logical subtraction of the given tree from this instance. */
  public RangeTree subtract(RangeTree tree) {
    return SetOperations.INSTANCE.subtract(this, tree);
  }

  /** Returns whether a given digit sequence is in the set of sequences defined by this tree. */
  public boolean contains(DigitSequence digits) {
    Node node = root;
    if (node == null) {
      return false;
    }
    for (int n = 0; n < digits.length(); n++) {
      node = node.findTarget(digits.getDigit(n));
      if (node == null) {
        return false;
      }
    }
    return node.canTerminate();
  }

  /**
   * Returns true if the given tree is a subset of this instance. This is functionally equivalent
   * to {@code tree.subtract(this).isEmpty()}, but much more efficient in cases where it returns
   * false.
   */
  public boolean containsAll(RangeTree tree) {
    if (tree.isEmpty()) {
      // Everything contains all the contents of the empty set (even the empty set).
      return true;
    }
    if (isEmpty()) {
      // Nothing is contained by the empty set.
      return false;
    }
    ContainsAllVisitor v = new ContainsAllVisitor(getInitial());
    tree.getInitial().accept(v);
    return v.containsAll;
  }

  // A very efficient test of tree containment (faster than doing "b.subtract(a).isEmpty()").
  private static final class ContainsAllVisitor implements DfaVisitor {
    private boolean containsAll = true;
    private DfaNode current;
    private ContainsAllVisitor(DfaNode node) {
      current = node;
    }

    @SuppressWarnings("ReferenceEquality")
    @Override
    public void visit(DfaNode source, DfaEdge edge, DfaNode target) {
      // Since nodes are interned once in a tree, '==' is sufficient and not potentially slow.
      if (current == source || !containsAll) {
        // An identical subtree means we can shortcut everything (also if we know we've failed).
        return;
      }
      // No containment if the "subset" tree has lengths not in the current tree.
      // This also effectively checks for termination at this node (via bit-0).
      if ((~current.getLengthMask() & source.getLengthMask()) != 0) {
        containsAll = false;
        return;
      }
      // Recursively check that the sub-tree of the target node is contained within one or more
      // edges of the current tree.
      int subMask = edge.getDigitMask();
      for (DfaEdge e : current.getEdges()) {
        // Look at paths which are in both trees.
        int m = (e.getDigitMask() & subMask);
        if (m != 0) {
          DfaNode oldCurrent = current;
          current = e.getTarget();
          edge.getTarget().accept(this);
          current = oldCurrent;
          if (!containsAll) {
            // Containment failure in some sub-tree.
            return;
          }
          // Clear bits we're accounted for.
          subMask &= ~m;
        }
      }
      // If not all edges were accounted for, this was not a valid sub-tree.
      if (subMask != 0) {
        containsAll = false;
      }
    }
  }

  // -------- Non set-like operations (transforming a RangeTree in non set-like ways) --------

  /** A general mapping function for transforming a range tree via its specifications. */
  public RangeTree map(Function<RangeSpecification, RangeSpecification> fn) {
    return from(asRangeSpecifications().stream().map(fn));
  }

  /**
   * Returns a range tree which matches the same digit sequences as this instance down to the first
   * {@code n} digits. The returned tree is a super-set of this instance.
   */
  public RangeTree significantDigits(int n) {
    checkArgument(n >= 0, "invalid significant digits");
    return map(s -> s.first(n).extendByLength(Math.max(s.length() - n, 0)));
  }

  /** Returns a range tree with the given "path" prefixed to the front. */
  public RangeTree prefixWith(RangeSpecification prefix) {
    checkArgument(isEmpty() || getLengths().last() + prefix.length() <= DigitSequence.MAX_DIGITS,
        "cannot extend range tree (prefix '%s' too long): %s", prefix, this);
    return (prefix.length() > 0) ? map(prefix::extendBy) : this;
  }

  /**
   * Slices a range tree at a single length. This is equivalent to {@code slice(length, length)}.
   */
  public RangeTree slice(int length) {
    return slice(length, length);
  }

  /**
   * Slices a range tree within the specified length bounds. A path exists in the returned tree if
   * it's length is in the (inclusive) range {@code [minLength, maxLength]} or it was longer than
   * {@code maxLength} but has been truncated. Importantly the returned range tree is not a subset
   * of the original tree.
   *
   * <p>This method can be thought of as returning the "complete or partially complete digit
   * sequences up to the specified maximum length". It is useful for calculating prefixes which
   * match partial digit sequences (i.e. "as you type" formatting).
   *
   * <p>For example:
   * <pre> {@code
   * slice({ 12345, 67xxx, 89 }, 0, 3) == { 123, 67x, 89 }
   * slice({ 12345, 67xxx, 89 }, 3, 3) == { 123, 67x }
   * slice({ 12, 34, 5 }, 2, 3) == { 12, 34 }
   * slice({ 12, 34 }, 3, 3) == { }
   * }</pre>
   */
  public RangeTree slice(int minLength, int maxLength) {
    return from(
        asRangeSpecifications().stream()
            .filter(s -> s.length() >= minLength)
            .map(s -> s.first(maxLength)));
  }

  // -------- Transformation APIs (converting a RangeTree to another representation) --------

  /** Returns the minimal, ordered list of range specifications represented by this tree. */
  public ImmutableList<RangeSpecification> asRangeSpecifications() {
    if (root == null) {
      return ImmutableList.of();
    }
    List<RangeSpecification> out = new ArrayList<>();
    int lenMask = root.lengthMask;

    if ((lenMask & (lenMask - 1)) == 0) {
      // If this tree only matches one length of sequences, we can just serialize it directly.
      addSpecs(this.root, RangeSpecification.empty(), out);
    } else {
      // When a tree matches more than one length, we cannot just serialize it in one go, because
      // the tree for ["123", "####"] would serialize as:
      //   ["123", "[02-9]###", "1[013-9]##", "12[0-24-9]#", "123#"]
      // and while the union of those 4-digit sequences is the same as "####", it's hardly minimal
      // or user friendly. In order to get a minimal serialization for a given length N, it is
      // sufficient to intersect the tree with the "allMatch" tree of length N (e.g. "####...")
      // and then serialize the result.
      SetOperations setOps = SetOperations.INSTANCE;
      for (Integer length : getLengths()) {
        // This can't be empty because we know there's at least one branch that matches digits
        // of the current length (or we would not have returned the length from getLengths()).
        addSpecs(
            setOps.intersectImpl(this.root, allMatch(length)), RangeSpecification.empty(), out);
      }
      setOps.tidyUpInterningMap();
    }
    return ImmutableList.sortedCopyOf(Comparator.comparing(RangeSpecification::min), out);
  }

  /**
   * Recursively adds the range specifications generated from the given sub-tree to the output
   * list.
   */
  private static void addSpecs(Node node, RangeSpecification spec, List<RangeSpecification> out) {
    if (node.canTerminate()) {
      out.add(spec);
    }
    for (Edge e : node.edges) {
      addSpecs(e.target, spec.extendByMask(e.mask), out);
    }
  }

  /** Returns a node which accepts any digit sequences of the given length. */
  private static Node allMatch(int length) {
    Node node = Node.TERMINAL;
    for (int n = 0; n < length; n++) {
      node = new Node(ImmutableList.of(new Edge(ALL_DIGITS_MASK, node)), false);
    }
    return node;
  }

  /** Returns the minimal covering of ranges specified by this tree. */
  public ImmutableRangeSet<DigitSequence> asRangeSet() {
    ImmutableRangeSet.Builder<DigitSequence> out = ImmutableRangeSet.builder();
    // Not all ranges create different range specifications are disjoint and this will merge then
    // into then minimal set.
    for (RangeSpecification s : asRangeSpecifications()) {
      out.addAll(s.asRanges());
    }
    return out.build();
  }

  // -------- DFA visitor API --------

  /**
   * Accepts the given visitor on the root of this non-empty tree.
   *
   * @throws IllegalStateException if the tree is empty.
   */
  public void accept(DfaVisitor visitor) {
    checkState(root != null, "cannot accept a visitor on an empty range tree");
    root.accept(visitor);
  }

  /**
   * Returns the initial node of this non-empty tree.
   *
   * @throws IllegalStateException if the tree is empty.
   */
  public DfaNode getInitial() {
    checkState(root != null, "cannot get the initial node from an empty range tree");
    return root;
  }

  /** Returns the singleton terminal node of any range tree. */
  public static DfaNode getTerminal() {
    return Node.TERMINAL;
  }

  // -------- Miscellaneous Object APIs --------

  @SuppressWarnings("ReferenceEquality")
  @Override
  public boolean equals(Object o) {
    // This could also just convert both to range specifications and use that, but that's likely
    // a lot more work (not that this is trivial). If you really want fast equality of trees then
    // the interning map needs to be global (but also switched to a weak map).
    if (this == o) {
      return true;
    }
    if (!(o instanceof RangeTree)) {
      return false;
    }
    RangeTree other = (RangeTree) o;
    if (root == null && other.root == null) {
      // Empty trees are equal.
      return true;
    }
    if (root == null || other.root == null) {
      // An empty tree is never equal to a non-empty tree.
      return false;
    }
    // Intern both trees and see if their roots are now identical.
    SetOperations setOps = SetOperations.INSTANCE;
    return setOps.intern(root) == setOps.intern(((RangeTree) o).root);
  }

  @Override
  public int hashCode() {
    if (hashCode == null) {
      hashCode = asRangeSpecifications().hashCode();
    }
    return hashCode;
  }

  /** Debugging only. */
  @Override
  public String toString() {
    return asRangeSpecifications().toString();
  }
}
