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
import static com.google.common.base.Preconditions.checkState;
import static com.google.i18n.phonenumbers.metadata.RangeSpecification.ALL_DIGITS_MASK;

import com.google.common.collect.Iterables;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.regex.Edge.SimpleEdge;
import com.google.i18n.phonenumbers.metadata.regex.Edge.Visitor;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/** Writes an NFA graph edge instance as a regular expression. */
final class EdgeWriter implements Visitor {
  // Regex constant strings pulled out for some degree of readability.
  private static final String DOT_MATCH = ".";
  private static final String DIGIT_MATCH = "\\d";
  private static final String OPTIONAL_MARKER = "?";
  private static final String GROUP_START = "(?:";
  private static final String GROUP_DISJUNCTION = "|";
  private static final String GROUP_END = ")";
  private static final String OPTIONAL_GROUP_END = GROUP_END + OPTIONAL_MARKER;

  /**
   * Returns a regular expression corresponding to the structure of the given edge. This method
   * does not apply any specific optimizations to the edge it is given and any optimizations which
   * affect the output must have already been applied to the graph from which the input edge was
   * derived.
   *
   * @param edge A collapsed edge typically derived from serializing an NFA graph.
   * @param useDotMatch true if {@code '.'} should be used to "match any digit" (instead of
   *     {@code '\\d'}) which results in shorter output.
   */
  public static String toRegex(Edge edge, boolean useDotMatch) {
    EdgeWriter writer = new EdgeWriter(useDotMatch);
    edge.accept(writer);
    return writer.out.toString();
  }

  // The token to match any input digit (e.g. "\\d" or ".").
  private final String anyToken;
  // Accumulated regular expression appended to during visitation.
  private final StringBuilder out = new StringBuilder();
  // Flag to determine when the top-level edge visited is a group, because if it is we can often
  // omit the explicit grouping tokens and save some space.
  private boolean isTopLevelGroup = true;

  private EdgeWriter(boolean useDotMatch) {
    this.anyToken = useDotMatch ? DOT_MATCH : DIGIT_MATCH;
  }

  @Override
  public void visit(SimpleEdge e) {
    checkArgument(!e.equals(Edge.epsilon()), "unexpected bare epsilon");
    isTopLevelGroup = false;
    // It's easier to just attempt to extract an "any" edge as that code already has to work for
    // simple edges when they are inside other composite edges. Optionality is encoded into the
    // resulting AnyPath and handled by appendRegex(), so we don't need to handle it again here.
    Optional<AnyPath> any = AnyPathVisitor.extractAnyPath(e);
    if (any.isPresent()) {
      appendRegex(out, any.get().mask());
    } else {
      // Not an "any" edge so append the usual range representation (e.g. "6" or "[014-9]").
      out.append(RangeSpecification.toString(e.getDigitMask()));
      if (e.isOptional()) {
        out.append(OPTIONAL_MARKER);
      }
    }
  }

  @Override
  public void visitSequence(List<Edge> edges) {
    checkArgument(!edges.isEmpty(), "sequences must have at least one edge");
    isTopLevelGroup = false;
    // At this level a sequence might be a mix of normal and "any" edges (e.g. "123xxxx"). To
    // cope with this, track and accumulate the un-written "any" edge, and emit it just before
    // any other output (or at the end).
    AnyPath any = AnyPath.EMPTY;
    for (Edge e : edges) {
      Optional<AnyPath> next = AnyPathVisitor.extractAnyPath(e);
      if (next.isPresent()) {
        any = any.join(next.get());
        continue;
      }
      // Here we have a "normal" edge, but we still might need to emit a collected "any" edge.
      if (!any.isEmpty()) {
        appendRegex(out, any.mask());
        any = AnyPath.EMPTY;
      }
      // This recursion only happens when this was not an "any" edge (though it may still be a
      // composite that contains other "any" edges).
      e.accept(this);
    }
    // If the last thing we saw in this sequence was an "any" edge, don't forget to emit it.
    if (!any.isEmpty()) {
      appendRegex(out, any.mask());
    }
  }

  @Override
  public void visitGroup(Set<Edge> edges, boolean isOptional) {
    checkArgument(!edges.isEmpty(), "groups must have at least one edge");
    // The very top-level group is almost always non-optional and can be omitted for length
    // (ie. "(?:a|b|c)" can just be "a|b|c").
    boolean canSkipParens = isTopLevelGroup && !isOptional;
    // Unset this before recursing.
    isTopLevelGroup = false;

    // We have exactly one case where an "any" edge needs to be handled for groups, and that's
    // when there's an optional any group that's not part of an enclosing sequence (e.g. "(xx)?").
    if (edges.size() == 1 && isOptional) {
      Optional<AnyPath> any = AnyPathVisitor.extractAnyPath(Iterables.getOnlyElement(edges));
      if (any.isPresent()) {
        // Remember to account for the optionality of the outer group.
        appendRegex(out, any.get().makeOptional().mask());
        return;
      }
    }

    if (!canSkipParens) {
      out.append(GROUP_START);
    }
    for (Edge e : edges) {
      e.accept(this);
      out.append(GROUP_DISJUNCTION);
    }
    // Easier to just remove the disjunction we know was added last than track state in the loop.
    out.setLength(out.length() - GROUP_DISJUNCTION.length());
    if (!canSkipParens) {
      out.append(isOptional ? OPTIONAL_GROUP_END : GROUP_END);
    }
  }

  /**
   * Recursive visitor to extract "any" sequences from edges (simple or composite). A sequence of
   * edges is an "any path" if all edges accept any digit. Composite edges already enforce the
   * requirement that epsilon edges don't exist directly (they are represented via optionality).
   */
  private static final class AnyPathVisitor implements Visitor {
    /**
     * Returns the longest "any" sequence represented by the given edge (if the edge represents an
     * any sequence). If present, the result is non-empty.
     */
    @Nullable
    public static Optional<AnyPath> extractAnyPath(Edge e) {
      AnyPathVisitor visitor = new AnyPathVisitor();
      e.accept(visitor);
      return Optional.ofNullable(visitor.path);
    }

    // Accumulate value during visitation and set to null to abort.
    @Nullable
    private AnyPath path = AnyPath.EMPTY;

    @Override
    public void visit(SimpleEdge edge) {
      checkState(path != null, "path should never be null at start of recursion");
      if (edge.getDigitMask() == ALL_DIGITS_MASK) {
        path = path.join(edge.isOptional() ? AnyPath.OPTIONAL : AnyPath.SINGLE);
      } else {
        path = null;
      }
    }

    @Override
    public void visitSequence(List<Edge> edges) {
      checkState(path != null, "path should never be null at start of recursion");
      // Looking for a complete sequence of "any edges" (partial sequences in a concatenation are
      // taken care of by the caller).
      for (Edge e : edges) {
        Optional<AnyPath> next = AnyPathVisitor.extractAnyPath(e);
        if (next.isPresent()) {
          path = path.join(next.get());
        } else {
          path = null;
          break;
        }
      }
    }

    @Override
    public void visitGroup(Set<Edge> edges, boolean isOptional) {
      checkState(path != null, "path should never be null at start of recursion");
      // Looking for a group like (xxx(xx)?)? which contains one edge only. We just recurse into
      // that edge and then make the result optional (a disjuction with only one edge must be
      // optional or else it should have been a concatenation).
      if (edges.size() > 1) {
        path = null;
        return;
      }
      checkState(isOptional, "single edge disjunctions should be optional");
      Edge e = Iterables.getOnlyElement(edges);
      e.accept(this);
      if (path != null) {
        path = path.makeOptional();
      }
    }
  }

  // The code below here is really a bit squiffy and relies on a whole bunch of bit fiddling to
  // do what it does. The good news is that it's easy to unit-test the heck out of, so that's
  // what I've done. Don't look too hard at what's going on unless you're a bit of a masochist.

  /**
   * Appends the regular expression corresponding to the given AnyPath mask value. This is a
   * bit-mask where the Nth bit corresponds to accepting an any digit sequence of length N.
   *
   * <p>For example:
   * <ul>
   *   <li> {@code 00000010} accepts only length 1 (e.g. "\d")
   *   <li> {@code 00000011} accepts lengths 0 or 1 (e.g. "\d?")
   *   <li> {@code 00001000} accepts only length 3 (e.g. "\d{3}")
   *   <li> {@code 00011100} accepts lengths 2-4 (e.g. "\d{2,4}")
   *   <li> {@code 11101100} accepts lengths 0,2,3,5,6,7 (e.g. "(?:\d\d(?:\d(?:\d{2,4})?)?)?")
   * </ul>
   */
  private void appendRegex(StringBuilder out, int mask) {
    checkArgument(mask > 1, "unexpected mask value %s", mask);
    // Deal with optionality separately.
    boolean allOptional = (mask & 0x1) != 0;
    mask &= ~0x1;
    // We are looking for bit patterns like '1111000' for contiguous ranges (e.g. {3,7}).
    // Find the lo/hi size of the next contiguous range (inclusive).
    int lo = Integer.numberOfTrailingZeros(mask);
    int hi = Integer.numberOfTrailingZeros(~(mask >>> lo)) + (lo - 1);

    // If all the bits are accounted for (nothing above the "hi" bit) then this was the last
    // contiguous range and we don't need to recurse (so no more groups need to be opened).
    if (mask < (1 << (hi + 1))) {
      // Writes a contiguous range as a single token with optionality (e.g. "\d", "(?:\d{2,4})?").
      appendAnyRange(out, lo, hi, allOptional);
      return;
    }
    // This is about the entire group, not the subgroup we are about to recurse into.
    if (allOptional) {
      out.append(GROUP_START);
    }
    // IMPORTANT: If we are recursing, we must not attempt to emit the entire group here, only the
    // shortest matching length.
    //
    // Mask "11101100" does NOT represent "\d{2,3}(?:\d{2,4})?" as that can match 4-digits too.
    // Instead it should generate "\d\d(?:\d(?:\d{2,4})?)?", where the 3 digit match is part of an
    // optional group.
    appendRequiredAnyRange(out, lo);
    // Recurse using the mask that's had the match we just emitted "factored out". This is always
    // optional because bit-0 is what was the lowest set bit in our mask.
    appendRegex(out, mask >>> lo);
    if (allOptional) {
      out.append(OPTIONAL_GROUP_END);
    }
  }

  /**
   * Appends regular expression tokens that accept any digits for a single length.
   *
   * <p>For example:
   * <ol>
   *   <li>{@code n=1}: {@code "\d"}
   *   <li>{@code n=2}: {@code "\d\d"} (this could be extended if using '.')
   *   <li>{@code otherwise}: {@code "\d{n}"}
   * </ol>
   */
  private void appendRequiredAnyRange(StringBuilder out, int n) {
    checkArgument(n >= 1, "bad any length %s", n);
    out.append(anyToken);
    if (n == 2) {
      // Only safe to do this if the group is not optional ("\d\d?" != "(?:\d{2})?").
      out.append(anyToken);
    } else if (n > 2) {
      out.append('{').append(n).append('}');
    }
  }

  /**
   * Appends regular expression tokens that accept any digits in a contiguous range of lengths.
   *
   * <p>For example:
   * <ol>
   *   <li>{@code lo=1, hi=1, optional=false}: {@code "\d"}
   *   <li>{@code lo=1, hi=1, optional=true}: {@code "\d?"}
   *   <li>{@code lo=2, hi=2, optional=true}: {@code "(?:\d{2})?"}
   *   <li>{@code lo=3, hi=6, optional=false}: {@code "\d{3,6}"}
   *   <li>{@code lo=3, hi=6, optional=true}: {@code "(?:\d{3,6})?"}
   *   <li>{@code lo=1, hi=4, optional=true}: {@code "\d{0,4}"} (not {@code (?:\d{1,4})?})
   *   <li>{@code lo=2, hi=2, optional=false}: {@code "\d\d"} (special case for size)
   *   <li>{@code lo=1, hi=2, optional=false}: {@code "\d\d?"} (special case for size)
   * </ol>
   */
  private void appendAnyRange(StringBuilder out, int lo, int hi, boolean optional) {
    checkArgument(lo >= 1 && hi >= lo, "bad range arguments %s, %s", lo, hi);
    if (lo == hi) {
      if (!optional) {
        // Required single length.
        appendRequiredAnyRange(out, lo);
      } else {
        // Optional single length.
        if (lo > 1) {
          out.append(GROUP_START).append(anyToken);
          out.append('{').append(lo).append('}');
          out.append(OPTIONAL_GROUP_END);
        } else {
          out.append(anyToken).append(OPTIONAL_MARKER);
        }
      }
    } else if (lo == 1 && hi == 2 && !optional) {
      // Special case for "\d\d?" as it's shorter than "\d{1,2}" (and even shorter with '.').
      // Even though we append the "optional marker" (i.e. '?') here it's got nothing to do
      // with the entire group being optional. That would be "(?:\d{1,2})?" which is "\d{0,2}".
      out.append(anyToken).append(anyToken).append(OPTIONAL_MARKER);
    } else if (lo == 1 && optional) {
      // Special case to write "\d{0,N}" instead of "(?:\d{1,N})?"
      out.append(anyToken).append("{0,").append(hi).append('}');
    } else {
      if (optional) {
        out.append(GROUP_START);
      }
      // General case.
      out.append(anyToken).append('{').append(lo).append(',').append(hi).append('}');
      if (optional) {
        out.append(OPTIONAL_GROUP_END);
      }
    }
  }
}
