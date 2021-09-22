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
package com.google.i18n.phonenumbers.metadata.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A specifier for the three types of format available in a formatting rule, "national",
 * "international" and "carrier specific". Each format is represented by a single string which acts
 * as a format template, and from which the necessary XML regular expressions can be recovered.
 *
 * <p>The basic syntax of a specifier is something like {@code "XX XXX-XXXX"}, where '{@code X}'
 * represents a digit from the phone number being formatted. When converted into the legacy XML
 * syntax, a national specifier with this format would represent the "pattern" attribute
 * {@code "(\d{2})(\d{3})(\d{4})"} and the "format" element {@code "$1 $2-$3"}.
 *
 * <p>By adding the '{@code *}' character, one group of variable length may be defined. Thus
 * {@code "XX XXX-XX**"} represents the pattern {@code "(\d{2})(\d{3})(\d{2,4})"}.
 *
 * <p>If the national prefix should be present, for either national or carrier specific formatting,
 * it is represented by the '{@code #}' symbol. Similarly, for carrier specific formatting, the
 * '{@code @}' symbol represents the carrier code placeholder (and must be present exactly once in
 * any carrier specific format specifier).
 *
 * <p>By analyzing the unique prefixes of both national and carrier specific specifiers, the XML
 * syntax can be derived. In a fairly simple example, the format specifiers:
 * <ul>
 *   <li>national: {@code "(#XX) XXX-XXXX"}
 *   <li>carrier: {@code "#@ XX XXX-XXXX"}
 *   <li>international: {@code "XX XXX XXXX"}
 * </ul>
 * would result in:
 * <ul>
 *   <li>pattern: {@code "(\d{2})(\d{3})(\d{4})"}
 *   <li>national_prefix_formatting_rule: {@code "($NP$FG)"}
 *   <li>carrier_specific_formatting_rule: {@code "$NP$CC $FG"}
 *   <li>format: {@code "$1 $2-$3"}
 *   <li>international_format: {@code "$1 $2 $3"}
 * </ul>
 * The derived "pattern" groups must be the same between all specifiers, while the "national" and
 * "carrier" specifiers must share a common suffix after the "first group". This is a limitation of
 * the XML representation which must be preserved here.
 *
 * <p>If no carrier specific format specifier is present, the extraction of a format rule will
 * still occur (since the formatting rule also affects "as you type" formatting"). Thus:
 * <ul>
 *   <li>national: {@code "(XX) XXX"}
 * </ul>
 * will result in:
 * <ul>
 *   <li>format: {@code "$1 $2"}
 *   <li>national_prefix_formatting_rule: {@code "($FG)"}
 * </ul>
 * and not:
 * <ul>
 *   <li>format: {@code "($1) $2"}
 * </ul>
 *
 * <p>An international format specifier must exist if international formatting is possible (even if
 * it is identical to the national format specifier). If no international specifier exists, then
 * the range of phone numbers associated with this format must be a subset of the "no international
 * dialling" range, and the derived XML element "intlFormat" will contain the value "NA".
 *
 * <p>If literal characters such as "*" are required to be present in the format string, they can
 * be escaped via a '{@code \}' (backslash) character. The set of characters that might need
 * escaping is '{@code X}', '{@code *}', '{@code #}' and '{@code @}'. Note that the dollar symbol
 * '{@code $}' is special, and is prohibited from ever appearing in a format specifier (even though
 * it's not strictly part of the syntax).
 *
 * <p>A {@code FormatSpec} also defines the ranges of numbers for which this format applies. This
 * is a {@link RangeTree}, rather than a {@code PrefixTree}, since length matters (different
 * formats are sometimes distinguished purely on the basis of number length). The possible lengths
 * of the range tree must match the possible lengths of all defined specifier strings.
 */
@AutoValue
public abstract class FormatSpec {
  /**
   * Returns a format specifier from the serialized fields. Note that the given non-local
   * specifiers must share certain properties (e.g. same number of format groups, same min/max
   * length, same trailing group format). Some of this is necessary due to limitations in how
   * formats are represented in the legacy XML schema (e.g. between national and carrier specific
   * formats). Exceptions are raised when any of these properties are violated.
   *
   * @param nationalSpec the national format specifier string (can contain \-escaped characters).
   * @param carrierSpec the optional carrier format specifier string.
   * @param intlSpec the optional international format specifier string.
   * @param localSpec additional local format specifier string.
   * @param nationalPrefixOptional allows the national prefix omitted during parsing even if
   *     present in the format, or given during parsing when not present in the format.
   * @param comment a free-from comment for this specifier.
   */
  public static FormatSpec of(
      String nationalSpec,
      Optional<String> carrierSpec,
      Optional<String> intlSpec,
      Optional<String> localSpec,
      boolean nationalPrefixOptional,
      Optional<Comment> comment) {
    FormatTemplate national = FormatTemplate.parse(nationalSpec);
    checkArgument(!national.hasCarrierCode(),
        "national format specifier must not contain carrier code: %s", nationalSpec);
    Optional<FormatTemplate> carrier = carrierSpec.map(s -> parseCarrierSpec(s, national));
    Optional<FormatTemplate> intl = intlSpec.map(s -> parseIntlSpec(s, national));
    Optional<FormatTemplate> local = localSpec.map(s -> parseLocalSpec(s, national));
    int minLength = national.minLength();
    int maxLength = national.maxLength();
    return new AutoValue_FormatSpec(
        national, carrier, intl, local, minLength, maxLength, nationalPrefixOptional, comment);
  }

  /**
   * Returns a local format specifier for the given template. Local specifiers only have a national
   * template and national prefix prohibited.
   */
  public static FormatSpec localFormat(FormatTemplate local) {
    checkArgument(!local.hasNationalPrefix(),
        "a local template must not have national prefix: %s", local);
    return new AutoValue_FormatSpec(
        local,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        local.minLength(),
        local.maxLength(),
        false,
        Optional.empty());
  }

  /** Returns the national format template (e.g. "#XX XXX XXXX"). */
  public abstract FormatTemplate national();

  /** Returns the carrier specific format template (e.g. "(@ #XX) XXX XXXX"). */
  public abstract Optional<FormatTemplate> carrier();

  /** Returns the international format template (e.g. "XX-XXX-XXXX"). */
  public abstract Optional<FormatTemplate> international();

  /**
   * Returns the local format template (e.g. "XXX-XXXX"). Local formats must correspond to the
   * "Area Code Length" values in at least some of the ranges to which they are assigned.
   */
  public abstract Optional<FormatTemplate> local();

  /** Returns the minimum number of digits which this format matches. */
  public abstract int minLength();

  /** Returns the maximum number of digits which this format matches. */
  public abstract int maxLength();

  /**
   * Returns whether, for formats without a national prefix specified, it is still possible to
   * trigger this format by adding a national prefix (even though its is not shown). Formats for
   * which this method returns {@code true} are grouped alongside formats with an explicit national
   * prefix (since they must be ordered carefully with respect to each other to account for their
   * "leading digits").
   */
  public abstract boolean nationalPrefixOptional();

  /** Returns the free-form comment associated with this format specifier. */
  public abstract Optional<Comment> comment();

  /**
   * Returns the length based bounds for this format (e.g. all digit sequences between the minimum
   * and maximum lengths).
   */
  public RangeTree getLengthBasedBounds() {
    return RangeTree.from(IntStream.rangeClosed(minLength(), maxLength())
        .mapToObj(RangeSpecification::any));
  }

  @Override
  public final String toString() {
    StringBuilder out = new StringBuilder("FormatSpec{national=").append(national());
    carrier().ifPresent(t -> out.append(", carrier=").append(t));
    local().ifPresent(t -> out.append(", local=").append(t));
    international().ifPresent(t -> out.append(", international=").append(t));
    out.append(", minLength=").append(minLength());
    out.append(", maxLength=").append(maxLength());
    comment().ifPresent(c -> out.append(", comment='").append(c).append('\''));
    return out.append('}').toString();
  }

  // ---- RULE PARSING AND CONVERSION METHODS ----

  private static FormatTemplate parseCarrierSpec(String spec, FormatTemplate national) {
    FormatTemplate carrier = FormatTemplate.parse(spec);
    checkArgument(carrier.hasCarrierCode(),
        "carrier format specifier must contain carrier code: %s", spec);
    // This verifies the groups have the same lengths, but does not check for same formatting.
    checkArgument(carrier.isCompatibleWith(national),
        "carrier format specifier must have compatible groups: %s - %s",
        national.getSpecifier(), spec);
    // This is really ugly, since carrier formats must share the same format in the legacy XML, but
    // can have different formatting rules for the first group. The best way to test this is just
    // compare the XML output directly instead of trying to reason about groups, since group replace
    // also needs to be taken into account.
    checkArgument(carrier.getXmlFormat().equals(national.getXmlFormat()),
        "carrier format specifier must have equal trailing groups: %s - %s",
        national.getSpecifier(), spec);
    // Artificial check (currently true everywhere and likely to never be broken). If this is ever
    // relaxed, the nationalPrefixForParsing regeneration code will need changing to take account
    // of ordering (e.g. generate "(<CC>)?<NP>" instead of "<NP>(<CC>)?").
    checkArgument(!carrier.hasNationalPrefix() || spec.indexOf('#') < spec.indexOf('@'),
        "national prefix must precede carrier code in carrier format: %s", spec);
    return carrier;
  }

  private static FormatTemplate parseIntlSpec(String spec, FormatTemplate national) {
    FormatTemplate intl = FormatTemplate.parse(spec);
    // In theory this could be relaxed, but then when the spec is written it cannot just call
    // getFormat(). For now, it's always true the international formats don't have "fancy"
    // formatting around the first group (i.e. never "(XXX) XXX XXX") which makes sense since
    // international formats cannot be assumed to be read by people with local knowledge.

    checkArgument(
        !intl.hasNationalPrefix(),
        "international format specifier must not contain national prefix: %s",
        spec);
    checkArgument(!intl.hasCarrierCode(),
        "international format specifier must not contain carrier code: %s", spec);
    checkArgument(intl.isCompatibleWith(national),
        "international format specifier must have compatible groups: %s - %s",
        national.getSpecifier(), spec);
    return intl;
  }

  private static FormatTemplate parseLocalSpec(String spec, FormatTemplate national) {
    FormatTemplate local = FormatTemplate.parse(spec);
    checkArgument(!local.getXmlPrefix().isPresent(),
        "local format specifier must not have separate prefix: %s", spec);
    checkArgument(!local.hasNationalPrefix(),
        "local format specifier must not contain national prefix: %s", spec);
    checkArgument(!local.hasCarrierCode(),
        "local format specifier must not contain carrier code: %s", spec);
    checkArgument(local.minLength() < national.minLength(),
        "local format specifier must be shorter than the national format: %s - %s",
        national.getSpecifier(), spec);
    return local;
  }

  // ---- TEMPLATE CLASSES ----

  /**
   * A single template corresponding to a format specifier such as {@code "(# XXX) XXX-XXXX"}.
   * A template represents one of the types of format (national, international, carrier specific)
   * and enforces as much structural correctness as possible.
   *
   * <p>Templates bridge between the specifier syntax and the XML syntax, with its split prefixes
   * and confusing semantics. As such, there's a lot of slightly subtle business logic in the
   * parsing of templates that, over time, might need to adapt to real world changes (e.g. suffix
   * separators and precise expectations of format structure).
   */
  @AutoValue
  public abstract static class FormatTemplate {
    // This could be extended, but must never overlap with the escape characters used in the
    // "skeleton" string. It must also always be limited to the Basic Multilingual Plane (BMP).
    // It's really important that '$' is never a meta-character in this syntax, since we escape
    // strings like "$FG" which would otherwise be broken.
    private static final CharMatcher VALID_TEMPLATE_CHARS =
        CharMatcher.ascii().and(CharMatcher.javaIsoControl().negate()).and(CharMatcher.isNot('$'));

    private static final CharMatcher VALID_METACHARS = CharMatcher.anyOf("#@X*{>}\\");
    // Need to include '$' as a separator, since groups can abut.
    private static final CharMatcher SUFFIX_SEPARATOR = CharMatcher.anyOf(". /-$");

    private static final char NATIONAL_PREFIX = '#';
    private static final char CARRIER_CODE = '@';
    private static final char REQUIRED_DIGIT = 'X';
    private static final char OPTIONAL_DIGIT = '*';
    private static final char SUBSTITUTION_START = '{';
    private static final char SUBSTITUTION_MAP = '>';
    private static final char SUBSTITUTION_END = '}';

    private static final String ESCAPED_NATIONAL_PREFIX = "$NP";
    private static final String ESCAPED_CARRIER_CODE = "$CC";

    static FormatTemplate parse(String spec) {
      checkArgument(VALID_TEMPLATE_CHARS.matchesAllOf(spec),
          "illegal characters in template: %s", spec);
      List<FormatGroup> groups = new ArrayList<>();
      StringBuilder skeleton = new StringBuilder();
      boolean hasNationalPrefix = false;
      boolean hasCarrierCode = false;
      boolean hasVariableLengthGroup = false;
      // Used to avoid abutting groups (i.e. "XXX**XX").
      boolean canStartGroup = true;
      for (int n = 0; n < spec.length(); n++) {
        char c = spec.charAt(n);

        if (c == REQUIRED_DIGIT) {
          checkArgument(canStartGroup, "illegal group start: %s", spec);
          FormatGroup group = extractGroup(spec, n);
          checkArgument(!(hasVariableLengthGroup && group.isVariableLength()),
              "multiple variable length groups not allowed: %s", spec);
          hasVariableLengthGroup = group.isVariableLength();

          groups.add(group);
          skeleton.append(escapeGroupNumber(groups.size()));

          // Move to the last character of the group (since we increment again as we loop).
          n += group.maxLength() - 1;
          canStartGroup = false;
          continue;
        }

        if (c == SUBSTITUTION_START) {
          // Expect {GROUP>REPLACEMENT} where group can have optional digits (but normally won't).
          checkArgument(canStartGroup, "illegal group start: %s", spec);
          checkArgument(spec.charAt(n + 1) == REQUIRED_DIGIT,
              "illegal group replacement start: %s", spec);
          FormatGroup group = extractGroup(spec, n + 1);
          checkArgument(!(hasVariableLengthGroup && group.isVariableLength()),
              "multiple variable length groups not allowed: %s", spec);
          hasVariableLengthGroup = group.isVariableLength();

          // Now expect mapping character and substitution string.
          n += group.maxLength() + 1;
          checkArgument(spec.charAt(n) == SUBSTITUTION_MAP,
              "illegal group replacement (expected %s): '%s'", SUBSTITUTION_MAP, spec);
          int end = spec.indexOf(SUBSTITUTION_END, n + 1);
          checkArgument(end != -1, "missing group replacement end: %s", spec);

          groups.add(group.withReplacement(spec.substring(n + 1, end)));
          skeleton.append(escapeGroupNumber(groups.size()));
          // Unlike the "normal" case above, you can start another group immediately after this
          // (since the {,} make it unambiguous).
          n = end;
          continue;
        }

        canStartGroup = true;

        if (c == NATIONAL_PREFIX) {
          checkArgument(!hasNationalPrefix, "multiple national prefixes not allowed: %s", spec);
          hasNationalPrefix = true;
          skeleton.append(ESCAPED_NATIONAL_PREFIX);
          continue;
        }

        if (c == CARRIER_CODE) {
          checkArgument(!hasCarrierCode, "multiple carrier codes not allowed: %s", spec);
          hasCarrierCode = true;
          skeleton.append(ESCAPED_CARRIER_CODE);
          continue;
        }

        if (c == '\\') {
          // Blows up if trailing '\', but that's fine.
          c = spec.charAt(++n);
          checkArgument(VALID_METACHARS.matches(c), "invalid escaped character '%s': %s", c, spec);
        } else {
          checkArgument(c != OPTIONAL_DIGIT, "unexpected optional marker: %s", spec);
        }
        skeleton.append(c);
      }
      checkArgument(!groups.isEmpty(), "format specifiers must have at least one group: %s", spec);
      // Find the first group which has a replacement (one must exist). This is important for
      // determining where the prefix and suffix should be split when considering hoisting the
      // prefix into a format rule (see getSuffixStart() / getXmlPrefix() / getXmlFormat()).
      int fgIndex = 0;
      while (fgIndex < groups.size() && groups.get(fgIndex).replacement().isPresent()) {
        fgIndex++;
      }
      checkArgument(fgIndex < groups.size(), "cannot replace all groups in a template: %s", spec);
      return new AutoValue_FormatSpec_FormatTemplate(
          spec,
          hasNationalPrefix,
          hasCarrierCode,
          ImmutableList.copyOf(groups),
          fgIndex,
          skeleton.toString());
    }

    /**
     * Returns the specifier string (e.g. "# XXX-XXXX") which is the serialized form of the
     * template.
     */
    public abstract String getSpecifier();

    /** Whether this template formats a national prefix. */
    public abstract boolean hasNationalPrefix();

    /** Whether this template formats a carrier selection code prefix. */
    public abstract boolean hasCarrierCode();

    /** Returns the information about the groups in this template. */
    public abstract ImmutableList<FormatGroup> getGroups();

    /**
     * Returns the index of the first group which does not have a replacement (at least one must).
     */
    public abstract int getFirstAvailableGroupIndex();

    // This is an internal representation of the format string used by the XML. It differs in that
    // it isn't split into prefix and suffix (as required in some situations for the XML). As such
    // it only contains "$NP", "$CC", "$<N>", but never "$FG". All valid specifier skeletons must
    // contain "$1"..."$<N>" rather than any replacement strings.
    abstract String skeleton();

    /** Returns the minumin number of digits which can be matched by this template. */
    public int minLength() {
      return getLength(this, FormatGroup::minLength);
    }

    /** Returns the maximum number of digits which can be matched by this template. */
    public int maxLength() {
      return getLength(this, FormatGroup::maxLength);
    }

    /**
     * Returns the maximum number of digits which can be formatted as a single block by this
     * template. If no more than this number of digits are entered, they will be formatted as a
     * single block by this template.
     *
     * <p>This is useful when calculating the leading digits of a format since it might be
     * acceptable to match shortcodes to some formats if they would still format the shortcode
     * within the first block. This simplifies the leading digits in some cases.
     */
    public int getBlockFormatLength() {
      // If only one group everything is a block, otherwise take the minimum length of the first
      // group.
      return (getGroups().size() == 1) ? maxLength() : getGroups().get(0).minLength();
    }

    /** Returns a regex to capture the groups for this template (e.g. "(\d{3})(\d{4,5})") */
    public String getXmlCapturingPattern() {
      return getGroups().stream()
          .map(FormatGroup::toRegex)
          .collect(Collectors.joining(")(", "(", ")"));
    }

    /**
     * Returns the format string for use in the XML (e.g. "$1 $2-$3").
     *
     * <p>For example given the following templates:
     * <ul>
     *   <li>{@code "XXX XXX-XXX"} ⟹ {@code "$1 $2-$3"}
     *   <li>{@code "(#XXX) XXX-XXX"} ⟹ {@code "$1 $2-$3"} (the prefix is hoisted)
     *   <li>{@code "#{XXX>123} XXX-XXX"} ⟹ {@code "$2-$3"} ($1 was replaced and hoisted)
     *   <li>{@code "{X>}XXX-XXX"} ⟹ {@code "$2-$3"} ($1 was removed)
     * </ul>
     */
    public String getXmlFormat() {
      int fgIndex = getFirstAvailableGroupIndex();
      // Always replace the prefix with $N (which is what $FG maps to). This might be a no-op.
      String format = "$" + (fgIndex + 1) + skeleton().substring(getSuffixStart());
      // Finally do any group replacement from the skeleton after the "first available group".
      //
      // Note that this code isn't exercised in data at the moment (2018) but is here to avoid
      // needing to place artificial limitations on where group replacement can occur.
      for (int n = fgIndex + 1; n < getGroups().size(); n++) {
        Optional<String> replacement = getGroups().get(n).replacement();
        if (replacement.isPresent()) {
          format = format.replace("$" + (n + 1), replacement.get());
        }
      }
      return format;
    }

    /**
     * Returns the format prefix for use in the XML formatting rules (e.g. "($NP $FG)"). If the
     * calculated prefix is just "$FG" then nothing is returned (since that's a no-op value).
     *
     * <p>For example given the following templates:
     * <ul>
     *   <li>{@code "XXX XXX-XXX"} ⟹ XML prefix is empty
     *   <li>{@code "(#XXX) XXX-XXX"} ⟹ {@code "($NP$FG)"}
     *   <li>{@code "#{XXX>123} XXX-XXX"} ⟹ {@code "$NP123 $FG"}
     *   <li>{@code "{X>}XXX-XXX"} ⟹ XML prefix is empty (but the format will not contain $1)
     * </ul>
     */
    public Optional<String> getXmlPrefix() {
      String prefix = skeleton().substring(0, getSuffixStart());
      // We know that "$<fgIndex + 1>" (substitutions are 1-indexed) is in the prefix and
      // should be replaced with "$FG", and everything before that has a replacement.
      int fgIndex = getFirstAvailableGroupIndex();
      for (int n = 0; n < fgIndex; n++) {
        // Everything before the "first available group" must have a replacement (by definition).
        prefix = prefix.replace("$" + (n + 1), getGroups().get(n).replacement().get());
      }
      prefix = prefix.replace("$" + (fgIndex + 1), "$FG");
      checkState(prefix.contains("$FG"),
          "XML prefix must always contain '$FG' (this must be a code error): %s", prefix);
      // After all this work we could still end up with a no-op substitution!
      return prefix.equals("$FG") ? Optional.empty() : Optional.of(prefix);
    }

    /**
     * Returns whether all groups have the same "structure" (i.e. min/max length). They can
     * differ in terms of having replacements however.
     */
    boolean isCompatibleWith(FormatTemplate other) {
      if (getGroups().size() != other.getGroups().size()) {
        return false;
      }
      for (int n = 0; n < getGroups().size(); n++) {
        if (!getGroups().get(n).isCompatibleWith(other.getGroups().get(n))) {
          return false;
        }
      }
      return true;
    }

    private int getSuffixStart() {
      // This is only safe because "\$1" cannot be present ('$' cannot be escaped).
      int suffixStart = SUFFIX_SEPARATOR.indexIn(skeleton(), skeleton().indexOf("$1") + 1);
      // If no suffix start found, the entire skeleton is the prefix.
      if (suffixStart == -1) {
        suffixStart = skeleton().length();
      }
      // Now account for the fact that the first group (and others) could have replacements, which
      // pushes the suffix start to just after the "first available group" (which is what becomes
      // $FG). If the first available group is "$1" then we just get suffixStart.
      int fgNumber = getFirstAvailableGroupIndex() + 1;
      checkState(fgNumber < 10, "invalid first group number: %s", fgNumber);
      return Math.max(suffixStart, skeleton().indexOf("$" + fgNumber) + 2);
    }

    @Override
    public final String toString() {
      return getSpecifier();
    }

    private static int getLength(FormatTemplate template, ToIntFunction<FormatGroup> lengthFn) {
      return template.getGroups().stream().mapToInt(lengthFn).sum();
    }

    private static FormatGroup extractGroup(String template, int start) {
      // We know that 'start' references a group start (i.e. 'X') so length must be at least 1.
      int endRequired = findEndOf(REQUIRED_DIGIT, template, start);
      int endGroup = findEndOf(OPTIONAL_DIGIT, template, endRequired);
      return FormatGroup.of(endRequired - start, endGroup - start);
    }

    private static int findEndOf(char c, String template, int start) {
      int endRequired = CharMatcher.isNot(c).indexIn(template, start);
      return endRequired != -1 ? endRequired : template.length();
    }

    private static String escapeGroupNumber(int n) {
      checkArgument(n >= 1 && n <= 9, "bad group number: %s", n);
      return "$" + n;
    }
  }

  /** Represents contiguous digit groups in a format (e.g. "XXX" or "XXX***"). */
  @AutoValue
  public abstract static class FormatGroup {
    private static FormatGroup of(int min, int max) {
      checkArgument(max >= min, "bad group lengths: %s, %s", min, max);
      return new AutoValue_FormatSpec_FormatGroup(min, max, Optional.empty());
    }

    private FormatGroup withReplacement(String s) {
      return new AutoValue_FormatSpec_FormatGroup(minLength(), maxLength(), Optional.of(s));
    }

    /** Returns the minimum number of digits in this group. */
    public abstract int minLength();

    /** Returns the maximum number of digits in this group. */
    public abstract int maxLength();

    /** Returns the optional, arbitrary (possibly empty) replacement string for this group. */
    abstract Optional<String> replacement();

    /**
     * Returns if this group can match a variable number of digits. Only one group in any format
     * specifier can have variable length.
     */
    private boolean isVariableLength() {
      return maxLength() > minLength();
    }

    /**
     * Returns whether two groups have the same "structure" (i.e. min/max lengths), but does not
     * compare replacement values. Used only for internal checks.
     */
    private boolean isCompatibleWith(FormatGroup other) {
      return minLength() == other.minLength() && maxLength() == other.maxLength();
    }

    private String toRegex() {
      if (maxLength() > minLength()) {
        return String.format("\\d{%d,%d}", minLength(), maxLength());
      } else if (minLength() > 1) {
        return String.format("\\d{%d}", minLength());
      } else {
        return "\\d";
      }
    }

    @Override
    public final String toString() {
      String group =
          "X".repeat(minLength()) + "*".repeat(maxLength() - minLength());
      return replacement().map(r -> String.format("{%s>%s}", group, r)).orElse(group);
    }
  }
}
