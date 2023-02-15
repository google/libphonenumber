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
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Value type for edges in NFA graphs of phone number regular expressions. Outside this package,
 * this type is mainly used for examining NFA graphs which represent a regular expression,
 * generated via {@link RangeTreeConverter#toNfaGraph}..
 *
 * <p>Note that the ordering of edges is carefully designed to attempt to replicate as much of the
 * existing intuition about ordering in regular expressions as possible. This should result in any
 * generated expressions being as close to existing hand edited expressions as possible.
 */
public abstract class Edge implements Comparable<Edge> {
  /** API for visiting composite edges; see also {@link #accept(Visitor)}. */
  public interface Visitor {
    /** Visits a leaf node simple edge. */
    void visit(SimpleEdge edge);
    /**
     * Visits a composited sequence of edges. Note that sequences only ever contain disjunctions or
     * simple edges, but never other sequences. For edges "a", "b", "c", this represents the
     * concatenated edge "abc".
     */
    void visitSequence(List<Edge> edges);
    /**
     * Visits a disjunction of parallel edges. Note that disjunctions only ever contain sequences
     * or simple edges, but never other disjunctions. For edges "a", "b", "c", this represents the
     * disjunctive group "(a|b|c)".
     */
    void visitGroup(Set<Edge> edges, boolean isOptional);
  }

  // The singleton epsilon edge.
  private static final SimpleEdge EPSILON = new SimpleEdge();
  // The singleton edge matching any digit (i.e. 'x' or '\d').
  private static final SimpleEdge ANY = new SimpleEdge(ALL_DIGITS_MASK, false);
  // The singleton edge optionally matching any digit (i.e. 'x?' or '\d?').
  private static final SimpleEdge OPTIONAL_ANY = ANY.optional();

  /** Returns an edge which accepts digits 0 to 9 according tothe bits set in the given mask. */
  public static SimpleEdge fromMask(int digitMask) {
    return digitMask == ALL_DIGITS_MASK ? ANY : new SimpleEdge(digitMask, false);
  }

  /**
   * Returns the epsilon edge which accepts zero length input and transitions immediately. This
   * edge should only ever appear parallel to other edges, and not as the only transition between
   * two nodes.
   */
  public static SimpleEdge epsilon() {
    return EPSILON;
  }

  /** Returns the edge which accepts any digit {@code [0-9]}. */
  public static SimpleEdge any() {
    return ANY;
  }

  /** Returns the edge which optionally accepts any digit {@code [0-9]}. */
  public static SimpleEdge optionalAny() {
    return OPTIONAL_ANY;
  }

  /**
   * Returns the ordered concatenation of the given edges. If either edge is a concatenation, it
   * is first expanded, so that the resulting edge contains only simple edges or disjunctions.
   */
  public static Edge concatenation(Edge lhs, Edge rhs) {
    checkArgument(!lhs.equals(EPSILON) && !rhs.equals(EPSILON), "cannot concatenate epsilon edges");
    // Don't make concatenations of concatenations; flatten them out so you only have singletons
    // or disjunctions. This is equivalent to writing "xyz" instead of "x(yz)".
    List<Edge> edges = Stream.of(lhs, rhs)
        .flatMap(
            e -> (e instanceof Concatenation) ? ((Concatenation) e).edges.stream() : Stream.of(e))
        .collect(Collectors.toList());
    return new Concatenation(edges);
  }

  /**
   * Returns the disjunction of the given edges. If either edge is already a concatenation, it
   * is first expanded, so that the resulting edge contains only simple edges or disjunctions.
   */
  public static Edge disjunction(Collection<Edge> edges) {
    // Don't make disjunctions of disjunctions; flatten them out so you only have singletons,
    // concatenations or epsilon. This is equivalent to writing "(x|y|z)" instead of "(x|(y|z))".
    List<Edge> allEdges = edges.stream()
        .flatMap(
            e -> (e instanceof Disjunction) ? ((Disjunction) e).edges.stream() : Stream.of(e))
        .sorted()
        .distinct()
        .collect(Collectors.toList());
    // There should only ever be one epsilon when we make a disjunction (disjunctions are made when
    // subgraphs collapse and each subgraph should only have one epsilon to make it optional).
    // Epsilons sort to-the-left of everything, so if there is an epsilon it must be the first edge.
    boolean isOptional = allEdges.get(0) == EPSILON;
    if (isOptional) {
      allEdges = allEdges.subList(1, allEdges.size());
    }
    Preconditions.checkState(!allEdges.contains(EPSILON));
    return new Disjunction(allEdges, isOptional);
  }

  /** An edge optionally matching a single input token, or the epsilon transition. */
  public static final class SimpleEdge extends Edge {
    private final int digitMask;
    private final boolean isOptional;

    // Constructor for singleton epsilon edge.
    private SimpleEdge() {
      this.digitMask = 0;
      // An optional epsilon makes no real sense.
      this.isOptional = false;
    }

    private SimpleEdge(int digitMask, boolean isOptional) {
      checkArgument(digitMask > 0 && digitMask < (1 << 10), "invalid bit mask %s", digitMask);
      this.digitMask = digitMask;
      this.isOptional = isOptional;
    }

    /** Returns the mask of digits accepted by this edge. */
    public int getDigitMask() {
      return digitMask;
    }

    /** Returns whether this edge is optional. */
    public boolean isOptional() {
      return isOptional;
    }

    /** Returns an optional version of this, non-optional edge. */
    public SimpleEdge optional() {
      Preconditions.checkState(digitMask != 0, "cannot make epsilon optional");
      Preconditions.checkState(!isOptional, "edge already optional");
      return new SimpleEdge(digitMask, true);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof SimpleEdge) && digitMask == ((SimpleEdge) obj).digitMask;
    }

    @Override
    public int hashCode() {
      return digitMask;
    }

    @Override
    public int compareTo(Edge rhs) {
      if (rhs instanceof SimpleEdge) {
        return compare((SimpleEdge) rhs);
      } else {
        // Composite types know how to compare themselves to SimpleEdges, so delegate to them but
        // remember to invert the result since we are reversing the comparison order.
        return -rhs.compareTo(this);
      }
    }

    private int compare(SimpleEdge rhs) {
      if (isOptional != rhs.isOptional) {
        // Optional edges sort to-the-right of non-optional things.
        return isOptional ? 1 : -1;
      }
      if (digitMask == rhs.digitMask) {
        return 0;
      }
      if (digitMask == 0 || rhs.digitMask == 0) {
        // Epsilon sorts to-the-left of everything.
        return digitMask == 0 ? -1 : 1;
      }
      // Unlike many other places where range specifications are used, we cannot guarantee the
      // ranges are disjoint here, so we sort on the reversed bitmask to favour the lowest set bit.
      // This sorts 'x' ([0-9]) to the left of everything, and epsilon to the right of everything.
      // I.e. "x" < "0", "0" < "1", "[0-3]" < "[0-2]", "9" < epsilon.
      //
      // Remember to logical-shift back down to avoid negative values.
      int reverseLhsMask = (Integer.reverse(digitMask) >>> 22);
      int reverseRhsMask = (Integer.reverse(rhs.digitMask) >>> 22);
      // Compare in the opposite order, so the largest reversed value is ordered "to the left".
      return Integer.compare(reverseRhsMask, reverseLhsMask);
    }
  }

  // A sequence of edges (disjunctions or simple edges).
  private static final class Concatenation extends Edge {
    private final ImmutableList<Edge> edges;

    private Concatenation(Collection<Edge> edges) {
      this.edges = ImmutableList.copyOf(edges);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitSequence(edges);
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof Concatenation) && edges.equals(((Concatenation) obj).edges);
    }

    @Override
    public int hashCode() {
      return edges.hashCode();
    }

    @Override
    public int compareTo(Edge rhs) {
      if (rhs instanceof Concatenation) {
        return compareEdges(edges, ((Concatenation) rhs).edges);
      } else {
        // Compare our first edge to the non-concatenation. If this compares as equal, order the
        // concatenation between simple edges and disjunctions to break the tie and avoid implying
        // that a concatenation and a non-concatenation are equal.
        int comparison = -rhs.compareTo(edges.get(0));
        return comparison != 0 ? comparison : (rhs instanceof SimpleEdge ? 1 : -1);
      }
    }
  }

  // A disjunctive group of edges (sequences or simple edges).
  private static final class Disjunction extends Edge {
    private final ImmutableSortedSet<Edge> edges;
    private final boolean isOptional;

    private Disjunction(Collection<Edge> edges, boolean isOptional) {
      checkArgument(!edges.isEmpty());
      this.edges = ImmutableSortedSet.copyOf(edges);
      this.isOptional = isOptional;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitGroup(edges, isOptional);
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof Disjunction) && edges.equals(((Disjunction) obj).edges);
    }

    @Override
    public int hashCode() {
      // Negate bits here to be different from Concatenation.
      return ~edges.hashCode();
    }

    @Override
    public int compareTo(Edge rhs) {
      if (rhs instanceof Disjunction) {
        return compareEdges(edges.asList(), ((Disjunction) rhs).edges.asList());
      } else {
        // Compare our first edge to the non-disjunction. If this compares as equal, order the
        // disjunction to the right of the other edge to break the tie and avoid implying that
        // a disjunction and a non-disjunction are equal.
        int comparison = -rhs.compareTo(edges.asList().get(0));
        return comparison == 0 ? 1 : comparison;
      }
    }
  }

  /**
   * Accepts a visitor on this edge, visiting any sub-edges from which it is composed. This is a
   * double-dispatch visitor to avoid anyone processing edges needing to know about specific types.
   * Only the immediate edge is visited and the visitor is then responsible for visiting child
   * edges.
   */
  public abstract void accept(Visitor visitor);

  // Compare lists according to elements, and tie break on length if different. This is effectively
  // a lexicographical ordering.
  private static int compareEdges(ImmutableList<Edge> lhs, ImmutableList<Edge> rhs) {
    int minSize = Math.min(lhs.size(), rhs.size());
    for (int n = 0; n < minSize; n++) {
      int compared = lhs.get(n).compareTo(rhs.get(n));
      if (compared != 0) {
        return compared;
      }
    }
    return Integer.compare(lhs.size(), rhs.size());
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    accept(new Visitor() {
      @Override
      public void visit(SimpleEdge e) {
        if (e.equals(Edge.epsilon())) {
          // Epsilon cannot be optional.
          out.append("e");
        } else {
          int m = e.getDigitMask();
          out.append(m == ALL_DIGITS_MASK ? "x" : RangeSpecification.toString(m));
          if (e.isOptional()) {
            out.append('?');
          }
        }
      }

      @Override
      public void visitSequence(List<Edge> edges) {
        edges.forEach(e -> e.accept(this));
      }

      @Override
      public void visitGroup(Set<Edge> edges, boolean isOptional) {
        out.append("(");
        edges.forEach(e -> {
          e.accept(this);
          out.append("|");
        });
        out.setLength(out.length() - 1);
        out.append(isOptional ? ")?" : ")");
      }
    });
    return out.toString();
  }
}
