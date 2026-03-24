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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.i18n.phonenumbers.metadata.model.MetadataException.checkMetadata;
import static com.google.i18n.phonenumbers.metadata.model.XmlRangesSchema.AREA_CODE_LENGTH;
import static com.google.i18n.phonenumbers.metadata.model.XmlRangesSchema.FORMAT;
import static com.google.i18n.phonenumbers.metadata.model.XmlRangesSchema.NATIONAL_ONLY;
import static com.google.i18n.phonenumbers.metadata.model.XmlRangesSchema.PER_REGION_COLUMNS;
import static com.google.i18n.phonenumbers.metadata.model.XmlRangesSchema.REGIONS;
import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.i18n.phonenumbers.metadata.DigitSequence;
import com.google.i18n.phonenumbers.metadata.PrefixTree;
import com.google.i18n.phonenumbers.metadata.RangeSpecification;
import com.google.i18n.phonenumbers.metadata.RangeTree;
import com.google.i18n.phonenumbers.metadata.i18n.PhoneRegion;
import com.google.i18n.phonenumbers.metadata.model.FormatSpec.FormatTemplate;
import com.google.i18n.phonenumbers.metadata.model.NumberingScheme.Comment.Anchor;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType;
import com.google.i18n.phonenumbers.metadata.proto.Types.XmlShortcodeType;
import com.google.i18n.phonenumbers.metadata.table.RangeTable;
import com.google.i18n.phonenumbers.metadata.table.RangeTable.OverwriteMode;
import com.google.i18n.phonenumbers.metadata.table.Schema;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * An abstraction of all the phone number metadata known about for a single calling code.
 * <p>
 * Note that there is no builder for NumberingScheme. The expectation is that CSV tables and other
 * primary sources will be used to build numbering schemes at a single point in the business logic.
 * Handling incremental modification of a builder, or partially built schemes just isn't something
 * that's expected to be needed (though there is {@code TestNumberingScheme} for use in unit tests.
 */
@AutoValue
public abstract class NumberingScheme {
  // Bitmask for [1-9] (bits 1..9 set, bit 0 clear).
  private static final int NOT_ZERO_MASK = 0x3FE;
  private static final String JAPAN_COUNTRY_CODE = "81";

  /** Top level information about a numbering scheme. */
  @AutoValue
  public abstract static class Attributes {
    /** Returns a new attributes instance for the given data. */
    public static Attributes create(
        DigitSequence cc,
        PhoneRegion mainRegion,
        Set<PhoneRegion> extraRegions,
        ImmutableSet<DigitSequence> nationalPrefix,
        RangeTree carrierPrefixes,
        String defaultIddPrefix,
        RangeTree allIddRanges,
        String extensionPrefix,
        Set<PhoneRegion> mobilePortableRegions) {
      // In theory there could be IDD prefix for a non-geographic region (and this check could be
      // removed) but it's not something we've ever seen and don't have any expectation of.
      checkMetadata(!mainRegion.equals(PhoneRegion.getWorld()) || allIddRanges.isEmpty(),
          "[%s] IDD prefixes must not be present for non-geographic regions", cc);
      checkMetadata(mainRegion.equals(PhoneRegion.getWorld()) || !allIddRanges.isEmpty(),
          "[%s] IDD prefixes must be present for all geographic regions", cc);
      checkMetadata(nationalPrefix.stream().noneMatch(allIddRanges::contains),
          "[%s] National prefix %s and IDD prefixes (%s) must be disjoint",
          cc, nationalPrefix, allIddRanges);
      checkMetadata(nationalPrefix.stream().noneMatch(carrierPrefixes::contains),
          "[%s] National prefix %s and carrier prefixes (%s) must be disjoint",
          cc, nationalPrefix, carrierPrefixes);
      // Allow exactly one '~' to separate the prefix digits to indicate a pause during dialling
      // (this check could be relaxed in future, but it's currently true for all data).
      checkMetadata(defaultIddPrefix.isEmpty() || defaultIddPrefix.matches("[0-9]+(?:~[0-9]+)?"),
          "[%s] Invalid IDD prefix: %s", cc, defaultIddPrefix);
      DigitSequence iddPrefix = DigitSequence.of(defaultIddPrefix.replace("~", ""));
      checkMetadata(iddPrefix.isEmpty() || allIddRanges.contains(iddPrefix),
          "[%s] IDD ranges must contain the default prefix: %s", cc, iddPrefix);
      checkMetadata(!extraRegions.contains(mainRegion),
          "[%s] duplicated main region '%s' in extra regions: %s",
          cc, mainRegion, extraRegions);
      // Main region comes first in iteration order, remaining regions are ordered naturally.
      ImmutableSet.Builder<PhoneRegion> set = ImmutableSet.builder();
      set.add(mainRegion);
      extraRegions.stream().sorted().forEach(set::add);
      ImmutableSet<PhoneRegion> allRegions = set.build();
      checkMetadata(allRegions.containsAll(mobilePortableRegions),
          "invalid mobile portable regions: %s", mobilePortableRegions);
      return new AutoValue_NumberingScheme_Attributes(
          cc,
          allRegions,
          nationalPrefix,
          carrierPrefixes,
          defaultIddPrefix,
          allIddRanges,
          !extensionPrefix.isEmpty() ? Optional.of(extensionPrefix) : Optional.empty(),
          ImmutableSortedSet.copyOf(Ordering.natural(), mobilePortableRegions));
    }

    /** Returns the unique calling code of this numbering scheme. */
    public abstract DigitSequence getCallingCode();

    /**
     * Returns the regions represented by this numbering scheme. The main region is always present
     * and listed first, and remaining regions are listed in "natural" order.
     */
    public abstract ImmutableSet<PhoneRegion> getRegions();

    /**
     * Returns the "main" region for this numbering scheme. The notion of a main region for a
     * country calling code is slightly archaic and mostly comes from the way in which the legacy
     * XML data is structured. However there are a few places in the public API where the "main"
     * region is returned in situations of ambiguity, so it can be useful to know it.
     */
    public final PhoneRegion getMainRegion() {
      return getRegions().asList().get(0);
    }

    /**
     * Returns all possible national prefixes which can be used when dialling national numbers. In
     * most cases this set just contains the preferred prefix, but alternate values may be present
     * when a region switches between prefixes or for other reasons. Any "non preferred" prefixes
     * are recognized only during parsing, and otherwise ignored.
     *
     * <p>If there is a preferred prefix, it is listed first, otherwise the set is empty.
     */
    public abstract ImmutableSet<DigitSequence> getNationalPrefixes();

    /**
     * Returns the (possibly empty) prefix used when dialling national numbers (e.g. "0" for "US").
     * Not all regions require a prefix for national dialling.
     */
    public DigitSequence getPreferredNationalPrefix() {
      ImmutableSet<DigitSequence> prefixes = getNationalPrefixes();
      return prefixes.isEmpty() ? DigitSequence.empty() : prefixes.iterator().next();
    }

    /**
     * Returns all carrier prefixes for national dialling. This range must not contain the national
     * prefix.
     */
    public abstract RangeTree getCarrierPrefixes();

    /**
     * Returns the (possible empty) default international dialling (IDD) prefix, possibly
     * containing a '~' to indicate a pause during dialling (e.g. "8~10" for Russia).
     */
    public abstract String getDefaultIddPrefix();

    /**
     * Returns all IDD prefixes which may be used for international dialling. If the default prefix
     * is not empty it must be contained in this range.
     */
    public abstract RangeTree getIddPrefixes();

    /** Returns the preferred label to use for indicating extensions for numbers. */
    public abstract Optional<String> getExtensionPrefix();

    /** Returns the regions in which mobile numbers are portable between carriers. */
    public abstract ImmutableSet<PhoneRegion> getMobilePortableRegions();
  }

  /**
   * Creates a numbering scheme from a range table and example numbers. No rules are applied to the
   * data in the tables, and they are assumed to be complete.
   */
  public static NumberingScheme from(
      Attributes attributes,
      RangeTable xmlTable,
      Map<PhoneRegion, RangeTable> shortcodeMap,
      Map<String, FormatSpec> formats,
      ImmutableList<AltFormatSpec> altFormats,
      Table<PhoneRegion, ValidNumberType, DigitSequence> exampleNumbers,
      List<Comment> comments) {
    checkPossibleRegions(attributes.getRegions(), xmlTable);
    checkNationalOnly(attributes, xmlTable);
    checkUnambiguousIdd(attributes, xmlTable, formats);
    ImmutableSortedMap<PhoneRegion, RangeTable> shortcodes =
        checkShortCodeConsistency(shortcodeMap, xmlTable);
    return new AutoValue_NumberingScheme(
        attributes,
        xmlTable,
        shortcodes,
        checkFormatConsistency(attributes, formats, xmlTable, shortcodes),
        checkAltFormatConsistency(altFormats, formats, xmlTable),
        checkExampleNumbers(attributes.getRegions(), xmlTable, exampleNumbers),
        addSyntheticComments(comments, attributes));
  }

  // Adds the first comments for main and auxiliary regions, giving the English name and detailing
  // auxiliary region information if necessary.
  private static ImmutableList<Comment> addSyntheticComments(
      List<Comment> comments, Attributes attributes) {
    PhoneRegion mainRegion = attributes.getMainRegion();
    if (!mainRegion.equals(PhoneRegion.getWorld())) {
      List<Comment> modified = new ArrayList<>(getRegionNameComments(mainRegion));
      List<PhoneRegion> auxRegions =
          attributes.getRegions().asList().subList(1, attributes.getRegions().size());
      if (!auxRegions.isEmpty()) {
        String comment = String.format("Main region for '%s'", Joiner.on(',').join(auxRegions));
        modified.add(Comment.create(Comment.anchor(mainRegion), ImmutableList.of(comment)));
        for (PhoneRegion r : auxRegions) {
          modified.addAll(getRegionNameComments(r));
          String auxComment =
              String.format("Calling code and formatting shared with '%s'", mainRegion);
          modified.add(Comment.create(Comment.anchor(r), ImmutableList.of(auxComment)));
        }
      }
      // Do this last, since order matters (because anchors are not unique) and we want the
      // synthetic comments to come first.
      modified.addAll(comments);
      comments = modified;
    }
    return ImmutableList.copyOf(comments);
  }

  private static List<Comment> getRegionNameComments(PhoneRegion region) {
    ImmutableList<String> enName = ImmutableList.of(region.getEnglishNameForXmlComments());
    return ImmutableList.of(
        Comment.create(Comment.anchor(region), enName),
        Comment.create(Comment.shortcodeAnchor(region), enName));
  }

  private static void checkPossibleRegions(Set<PhoneRegion> regions, RangeTable xmlTable) {
    ImmutableSet<PhoneRegion> actual = REGIONS.extractGroupColumns(xmlTable.getColumns()).keySet();
    // Allow no region column in the table if there's only one region (since it's implicit).
    checkState((actual.isEmpty() && regions.size() == 1) || actual.equals(regions),
        "regions added to range table do not match the expected numbering scheme regions\n"
            + "expected: %s\n"
            + "actual: %s\n",
        regions, actual);
  }

  // An assumption has generally been that if a range is "national only" then it either:
  // a) belongs to only one region (the one it's national only for)
  // b) belongs to at least the main region (since in some schemes ranges mostly just overlap all
  //    possible regions).
  // Thus we preclude the possibility of having a "national only" number that appears in multiple
  // regions, but not the main region.
  //
  // If this check is ever removed (because there is real data where this is not the case), then
  // the code which generates the "<noInternationalDialling>" patterns will have to be revisited.
  private static void checkNationalOnly(Attributes attributes, RangeTable xmlTable) {
    RangeTree allNationalOnly = xmlTable.getRanges(NATIONAL_ONLY, true);
    if (allNationalOnly.isEmpty()) {
      return;
    }
    ImmutableList<PhoneRegion> regions = attributes.getRegions().asList();
    PhoneRegion main = regions.get(0);
    // Anything assigned to the main region can be ignored as we allow it to have multiple regions.
    // Now we have to ensure that these ranges are assigned to exactly one auxiliary region.
    RangeTree remaining =
        allNationalOnly.subtract(xmlTable.getRanges(REGIONS.getColumn(main), true));
    if (remaining.isEmpty()) {
      return;
    }
    DigitSequence cc = attributes.getCallingCode();
    for (PhoneRegion r : regions.subList(1, regions.size())) {
      RangeTree auxNationalOnly =
          xmlTable.getRanges(REGIONS.getColumn(r), true).intersect(allNationalOnly);
      // Anything already removed from "remaining" was already accounted for by another region.
      checkMetadata(remaining.containsAll(auxNationalOnly),
          "[%s] %s has national-only ranges which overlap other regions: %s",
          cc, r, auxNationalOnly.subtract(remaining));
      remaining = remaining.subtract(auxNationalOnly);
    }
    // This is not data issue since it should have been checked already, this is bug.
    checkState(remaining.isEmpty(), "[%s] ranges not assigned to any region: %s", cc, remaining);
  }

  /**
   * Ensures no national range can start with an IDD (international dialling code of any kind).
   * This is slightly more complex than just looking for any IDD prefix at the start of a range
   * because of cases like India, where "00800..." is a valid range and does start with IDD.
   *
   * <p>We allow this because:
   * <ol>
   * <li>The number is required to have the national prefix in front, so must be dialled as
   * {@code 000800...} (according to the Indian numbering plan)
   * <li>and {@code 000...} is not a valid sequence that would lead to dialing into another region,
   * because all calling codes start with {@code [1-9]}.
   * </ol>
   */
  private static void checkUnambiguousIdd(
      Attributes attributes, RangeTable xmlTable, Map<String, FormatSpec> formats) {
    // It can be empty for non-geographic (world) numbering schemes.
    if (attributes.getIddPrefixes().isEmpty()) {
      return;
    }

    // All IDDs extended by one non-zero digit. These are the prefixes which if dialled may end
    // up in another region, so they cannot be allowed at the start of any national number.
    RangeTree iddPlusOneDigit = attributes.getIddPrefixes().map(r -> r.extendByMask(NOT_ZERO_MASK));
    // We only care about ranges up to this length, which can speed things up.
    int maxPrefixLength = iddPlusOneDigit.getLengths().last();

    // Now prefix any ranges which could be dialled with a national prefix with all possible
    // national prefixes, based on how they are formatted (and assume that no format means no
    // national prefix).
    RangeTree withNationalPrefix = RangeTree.empty();
    RangeTree withoutNationalPrefix = xmlTable.getRanges(FORMAT, FORMAT.defaultValue());
    for (String fid : formats.keySet()) {
      FormatSpec spec = formats.get(fid);
      // Only bother with ranges up to the maximum prefix length we care about.
      RangeTree r = xmlTable.getRanges(FORMAT, fid).slice(0, maxPrefixLength);
      if (spec.nationalPrefixOptional()) {
        withNationalPrefix = withNationalPrefix.union(r);
        withoutNationalPrefix = withoutNationalPrefix.union(r);
      } else if (spec.national().hasNationalPrefix()) {
        withNationalPrefix = withNationalPrefix.union(r);
      } else {
        withoutNationalPrefix = withoutNationalPrefix.union(r);
      }
    }
    // Only here due to lambdas requiring an effectively final field (this makes me sad).
    RangeTree withNationalPrefixCopy = withNationalPrefix;
    RangeTree allDiallablePrefixes =
        withoutNationalPrefix
            .union(attributes.getNationalPrefixes().stream()
                .map(RangeSpecification::from)
                .map(p -> withNationalPrefixCopy.prefixWith(p))
                .reduce(RangeTree.empty(), RangeTree::union));
    // These are prefixes which are claimed to be nationally diallable but overlap with the IDD.
    RangeTree iddOverlap = PrefixTree.from(iddPlusOneDigit).retainFrom(allDiallablePrefixes);
    checkMetadata(iddOverlap.isEmpty(),
        "[%s] ranges cannot start with IDD: %s", attributes.getCallingCode(), iddOverlap);
  }

  /**
   * Ensures the shortcodes are disjoint from main ranges and consistent with each other by format
   * (since format information isn't held separately for each shortcode table).
   */
  private static ImmutableSortedMap<PhoneRegion, RangeTable> checkShortCodeConsistency(
      Map<PhoneRegion, RangeTable> shortcodeMap, RangeTable table) {
    ImmutableSortedMap<PhoneRegion, RangeTable> shortcodes =
        ImmutableSortedMap.copyOf(shortcodeMap);
    shortcodes.forEach((region, shortcodeTable) -> {
      RangeTree overlap = table.getAllRanges().intersect(shortcodeTable.getAllRanges());
      checkMetadata(overlap.isEmpty(),
          "Shortcode and national numbers overlap for %s: %s", region, overlap);
    });
    return shortcodes;
  }

  private static final Schema FORMAT_SCHEMA =
      Schema.builder().add(AREA_CODE_LENGTH).add(FORMAT).build();

  // We actually explicitly permit duplicate formats (for now) since the XML has them. Later, once
  // everything is settled, it might be possible to add a check here.
  private static ImmutableMap<String, FormatSpec> checkFormatConsistency(
      Attributes attributes,
      Map<String, FormatSpec> formatMap,
      RangeTable table,
      Map<PhoneRegion, RangeTable> shortcodes) {
    DigitSequence cc = attributes.getCallingCode();
    RangeTable.Builder allFormats = RangeTable.builder(FORMAT_SCHEMA);
    allFormats.copyNonDefaultValues(AREA_CODE_LENGTH, table, OverwriteMode.ALWAYS);
    allFormats.copyNonDefaultValues(FORMAT, table, OverwriteMode.ALWAYS);
    // Throws a RangeException (IllegalArgumentException) if inconsistent write occurs.
    shortcodes.values()
        .forEach(t -> allFormats.copyNonDefaultValues(FORMAT, t, OverwriteMode.SAME));
    RangeTable formatTable = allFormats.build();
    ImmutableMap<String, FormatSpec> formats = ImmutableMap.copyOf(formatMap);
    // TODO: Make this "equals" eventually (since it currently sees "synthetic" IDs).
    checkMetadata(
        formats.keySet().containsAll(formatTable.getAssignedValues(FORMAT)),
        "[%s] mismatched format IDs: %s",
        cc, Sets.symmetricDifference(formatTable.getAssignedValues(FORMAT), formats.keySet()));

    // If any of the checks relating to carrier formats are relaxed here, it might be necessary to
    // re-evaluate the logic around regeneration of nationalPrefixForParsing (so be careful!).
    boolean carrierTemplatesExist = false;
    boolean nationalPrefixExistsForFormatting = false;
    boolean nationalPrefixSometimesOptional = false;
    for (String id : formats.keySet()) {
      FormatSpec spec = formats.get(id);
      RangeTree assigned = allFormats.getRanges(FORMAT, id);
      checkMetadata(!assigned.isEmpty(),
          "[%s] format specifier '%s' not assigned to any range: %s", cc, id, spec);
      checkFormatLengths(cc, spec, assigned);
      checkLocalFormatLengths(cc, formatTable, spec, assigned);
      carrierTemplatesExist |= spec.carrier().isPresent();
      nationalPrefixExistsForFormatting |=
          spec.national().hasNationalPrefix()
              || spec.carrier().map(FormatTemplate::hasNationalPrefix).orElse(false);
      nationalPrefixSometimesOptional |= spec.nationalPrefixOptional();
    }
     // Only if the present region is not JP do this check as in Japan we are not capturing domestic
    // carrier codes.
    if (!cc.toString().equals(JAPAN_COUNTRY_CODE)) {
      checkMetadata(
          attributes.getCarrierPrefixes().isEmpty() || carrierTemplatesExist,
          "[%s] carrier prefixes exist but no formats have carrier templates: %s",
          cc,
          formats.values());
    }
    checkMetadata(!attributes.getNationalPrefixes().isEmpty() || !nationalPrefixExistsForFormatting,
        "[%s] if no national prefix exists, it cannot be specified in any format template: %s",
        cc, formats.values());
    checkMetadata(!attributes.getNationalPrefixes().isEmpty() || !nationalPrefixSometimesOptional,
        "[%s] if no national prefix exists, it cannot be optional for formatting: %s",
        cc, formats.values());
    return formats;
  }

  // Checks that the ranges to which formats are assigned don't have lengths outside the possible
  // lengths of that format (e.g. we don't have "12xx" assigned to the format "XXX-XXX").
  private static void checkFormatLengths(DigitSequence cc, FormatSpec spec, RangeTree assigned) {
    TreeSet<Integer> unexpected = new TreeSet<>(assigned.getLengths());
    unexpected.removeAll(ContiguousSet.closed(spec.minLength(), spec.maxLength()));
    if (!unexpected.isEmpty()) {
      RangeTree bad = RangeTree.empty();
      for (int n : unexpected) {
        bad = bad.union(assigned.intersect(RangeTree.from(RangeSpecification.any(n))));
      }
      throw new IllegalArgumentException(String.format(
          "[%s] format %s assigned to ranges of invalid length: %s", cc, spec, bad));
    }
  }

  // Checks that the local lengths for ranges (as determined by area code length) is compatible
  // with the assigned local format specifier. Note that it is allowed to have an area code length
  // of zero and still be assigned a format with a local specifier (the specifier may be shared
  // with other ranges which do have an area code length).
  private static void checkLocalFormatLengths(
      DigitSequence cc, RangeTable formatTable, FormatSpec spec, RangeTree assigned) {
    if (!spec.local().isPresent()) {
      return;
    }
    ImmutableSet<Integer> lengths =
        formatTable.subTable(assigned, AREA_CODE_LENGTH).getAssignedValues(AREA_CODE_LENGTH);
    FormatTemplate local = spec.local().get();
    // Format specifiers either vary length in the area code or the local number, but not both.
    int localLength = local.minLength();
    int localVariance = local.maxLength() - local.minLength();
    if (localVariance == 0) {
      // If there's no length variation in the "local" part, it means the area code length can
      // be variable.
      ContiguousSet<Integer> acls =
          ContiguousSet.closed(spec.minLength() - localLength, spec.maxLength() - localLength);
      checkMetadata(acls.containsAll(lengths),
          "[%s] area code lengths '%s' not supported by format: %s", cc, acls, spec);
    } else {
      // If the length variation of the format is in the trailing "local" part, we expect the a
      // unique area code length (only one "group" in the format can be variable).
      checkMetadata((spec.maxLength() - spec.minLength()) == localVariance,
          "[%s] invalid local format (bad length) in format specifier %s", cc, spec);
      int acl = spec.minLength() - localLength;
      checkMetadata(lengths.size() == 1 && lengths.contains(acl),
          "[%s] implied area code length(s) %s does not match expected length (%s) of format: %s",
          cc, lengths, acl, spec);
    }
  }

  private static ImmutableList<AltFormatSpec> checkAltFormatConsistency(
      ImmutableList<AltFormatSpec> altFormats,
      Map<String, FormatSpec> formats,
      RangeTable xmlTable) {
    for (AltFormatSpec altFormat : altFormats) {
      String parentId = altFormat.parentFormatId();
      FormatSpec parent = formats.get(parentId);
      checkMetadata(parent != null, "unknown parent format ID in alternate format: %s", altFormat);
      Set<Integer> altLengths = getLengths(altFormat.template());
      checkMetadata(getLengths(parent.national()).containsAll(altLengths),
          "alternate format lengths must be bounded by parent format lengths: %s", altFormat);

      // Only care about the parent ranges which have the same length(s) as the alt format.
      RangeTree lengthMask = RangeTree.from(altLengths.stream().map(RangeSpecification::any));
      RangeTree ranges = xmlTable.getRanges(FORMAT, parentId).intersect(lengthMask);
      RangeTree captured = PrefixTree.from(altFormat.prefix()).retainFrom(ranges);
      checkMetadata(!captured.isEmpty(),
          "alternate format must capture some of the parent format ranges: %s", altFormat);
      int prefixLength = altFormat.prefix().length();
      if (prefixLength > 0) {
        // A really ugly, but useful check to find if there's a better prefix. Specifically, it
        // determines if the given prefix is "over-capturing" ranges (e.g. prefix is "1[2-8]" but
        // only "1[3-6]" exists in the parent format's assigned ranges). Since this is an odd, non
        // set-like operation, it's just done "manually" using bit masks. It's not a union of the
        // paths, it's a "squashing" (since it results in the smallest single range specification).
        //
        // Start with all the paths trimmed to the prefix length (e.g. "123", "145", "247"). All
        // range specifications in the slice are the same length as the prefix we started with.
        RangeTree slice = captured.slice(prefixLength);
        // Now union the digit masks at each depth for all paths in the slice (in theory there
        // could be a "squash" operation on RangeSpecification to do all this).
        int[] masks = new int[prefixLength];
        slice.asRangeSpecifications().forEach(s -> {
          for (int n = 0; n < prefixLength; n++) {
            masks[n] |= s.getBitmask(n);
          }
        });
        // Now reconstruct the single "squashed" range specification (e.g. "[12][24][357]").
        RangeSpecification minSpec = RangeSpecification.empty();
        for (int n = 0; n < prefixLength; n++) {
          minSpec = minSpec.extendByMask(masks[n]);
        }
        checkMetadata(minSpec.equals(altFormat.prefix()),
            "alternate format prefix '%s' is too broad, it should be '%s' for: %s",
            altFormat.prefix(), minSpec, altFormat);
      }
    }
    return altFormats;
  }

  private static Set<Integer> getLengths(FormatTemplate t) {
    return ContiguousSet.closed(t.minLength(), t.maxLength());
  }

  // Checks that example numbers are valid numbers in the ranges for their type.
  private static ImmutableTable<PhoneRegion, ValidNumberType, DigitSequence> checkExampleNumbers(
      Set<PhoneRegion> regions,
      RangeTable table,
      Table<PhoneRegion, ValidNumberType, DigitSequence> exampleNumbers) {
    for (PhoneRegion r : regions) {
      RangeTable regionTable =
          table.subTable(table.getRanges(REGIONS.getColumn(r), TRUE), XmlRangesSchema.TYPE);
      Map<ValidNumberType, DigitSequence> regionExamples = exampleNumbers.row(r);
      ImmutableSet<ValidNumberType> types = regionTable.getAssignedValues(XmlRangesSchema.TYPE);
      checkMetadata(types.equals(regionExamples.keySet()),
          "mismatched types for example numbers in region %s\nExpected: %s\nActual: %s",
          r, types, regionExamples);
      for (ValidNumberType t : types) {
        DigitSequence exampleNumber = regionExamples.get(t);
        RangeTree ranges = regionTable.getRanges(XmlRangesSchema.TYPE, t);
        // Special case, since we permit example numbers for fixed line/mobile to be valid for the
        // combined range as well.
        //
        // This logic smells, since it reveals information about the XML structure (in which fixed
        // line and mobile ranges can overlap). However if we insist that a fixed line examples are
        // in the "fixed line only" range, we end up with problems if (mobile == fixed line), since
        // there is no "fixed line only" range (but there is an example number in the XML).
        if (t == ValidNumberType.MOBILE || t == ValidNumberType.FIXED_LINE) {
          ranges = ranges.union(
              regionTable.getRanges(XmlRangesSchema.TYPE, ValidNumberType.FIXED_LINE_OR_MOBILE));
        }
        checkMetadata(ranges.contains(exampleNumber),
            "invalid example number '%s' of type %s in region %s", exampleNumber, t, r);
      }
    }
    return ImmutableTable.copyOf(exampleNumbers);
  }

  public abstract Attributes getAttributes();

  // TODO: Inline the wrapper methods below.

  /** Returns the unique calling code of this numbering scheme. */
  public DigitSequence getCallingCode() {
    return getAttributes().getCallingCode();
  }

  /**
   * Returns the regions represented by this numbering scheme. The main region is always present
   * and listed first, and remaining regions are listed in "natural" order.
   */
  public ImmutableSet<PhoneRegion> getRegions() {
    return getAttributes().getRegions();
  }

  /**
   * Returns a range table containing per-range attributes according to
   * {@link XmlRangesSchema#COLUMNS}.
   */
  public abstract RangeTable getTable();

  /**
   * Returns a RangeTable restricted to the given region, which conforms to the
   * {@link XmlRangesSchema} schema, with the exception that no region columns exist.
   */
  public final RangeTable getTableFor(PhoneRegion region) {
    checkArgument(getRegions().contains(region),
        "invalid region '%s' for calling code '%s'", region, getCallingCode());
    return getTable()
        .subTable(getTable().getRanges(REGIONS.getColumn(region), TRUE), PER_REGION_COLUMNS);
  }

  public abstract ImmutableSortedMap<PhoneRegion, RangeTable> getShortcodes();

  /** Returns the RangeTable for the shortcodes of the given region. */
  public final Optional<RangeTable> getShortcodesFor(PhoneRegion region) {
    checkArgument(getRegions().contains(region),
        "invalid region '%s' for calling code '%s'", region, getCallingCode());
    return Optional.ofNullable(getShortcodes().get(region));
  }

  /** Returns the map of format ID to format specifier. */
  public abstract ImmutableMap<String, FormatSpec> getFormats();

  /** Returns a list of alternate formats which are also expected for this numbering scheme. */
  public abstract ImmutableList<AltFormatSpec> getAlternateFormats();

  /** Returns a table of example numbers for each region code and number type. */
  public abstract ImmutableTable<PhoneRegion, ValidNumberType, DigitSequence> getExampleNumbers();

  /**
   * Returns all comments known about by this numbering scheme. Internal method, callers should
   * always use {@link #getComments(Anchor)} instead.
   */
  abstract ImmutableList<Comment> getAllComments();

  /** Returns comments with a specified anchor for this numbering scheme. */
  public ImmutableList<Comment> getComments(Anchor anchor) {
    checkArgument(getAttributes().getRegions().contains(anchor.region()),
        "invalid region: %s", anchor.region());
    return getAllComments().stream()
        .filter(c -> c.getAnchor().equals(anchor))
        .collect(toImmutableList());
  }

  /**
   * An encapsulation of a comment to be associated with an element in the XML. Rather than have
   * many APIs for setting/getting comments on a {@link NumberingScheme}, the approach taken here
   * is to let comments describe for themselves where they go but keep them in one big bucket.
   * <p>
   * This simplifies a lot of the intermediate APIs in the builders, but is less efficient (since
   * finding comments is now a linear search). If this is ever an issue, they should be mapped by
   * key, using a {@code ListMultimap<String, Comment>} (since comments are also ordered by their
   * number).
   */
  @AutoValue
  public abstract static class Comment {
    private static final Joiner JOIN_LINES = Joiner.on('\n');
    private static final Splitter SPLIT_LINES = Splitter.on('\n');

    /** An anchor defining which element, in which territory, a comment should be attached to. */
    @AutoValue
    public abstract static class Anchor implements Comparable<Anchor> {
      // Special anchor for comments that are not stored in the comment table, but are attached to
      // data directly (e.g. formats).
      private static final Anchor ANONYMOUS = of(PhoneRegion.getUnknown(), "");

      private static final Comparator<Anchor> ORDERING =
          comparing(Anchor::region).thenComparing(Anchor::label);

      /** Creates a comment anchor from a region and xml type. */
      static Anchor of(PhoneRegion region, String label) {
        // TODO: Add check for valid label.
        return anchor(region, label);
      }

      /** The region of the territory this comment should be attached to. */
      public abstract PhoneRegion region();

      /**
       * The type in the territory this comment should be attached to. If missing, attach this
       * comment to the main comment block for the territory.
       */
      public abstract String label();

      @Override
      public int compareTo(Anchor that) {
        return ORDERING.compare(this, that);
      }
    }

    // Private since we want to funnel people through type safe factory methods.
    private static Anchor anchor(PhoneRegion region, String label) {
      return new AutoValue_NumberingScheme_Comment_Anchor(region, label);
    }

    /** Returns a key identifying a comment for a region. */
    public static Anchor anchor(PhoneRegion region) {
      return anchor(region, "XML");
    }

    /** Returns a key identifying a comment for the validation range of a given type in a region. */
    public static Anchor anchor(PhoneRegion region, XmlNumberType xmlType) {
      return anchor(region, xmlType.toString());
    }

    /**
     * Returns a key identifying a comment for the validation range of a given shortcode type in
     * a region.
     */
    public static Anchor shortcodeAnchor(PhoneRegion region) {
      return anchor(region, "SC");
    }

    /**
     * Returns a key identifying a comment for the validation range of a given shortcode type in
     * a region.
     */
    public static Anchor shortcodeAnchor(PhoneRegion region, XmlShortcodeType xmlType) {
      return anchor(region, xmlType.toString());
    }

    /** Creates a comment the applies to data identified by the specified key. */
    public static Comment create(Anchor anchor, List<String> lines) {
      return new AutoValue_NumberingScheme_Comment(anchor, ImmutableList.copyOf(lines));
    }

    /** Creates a comment the applies to data identified by the specified key. */
    public static Comment createAnonymous(List<String> lines) {
      return new AutoValue_NumberingScheme_Comment(Anchor.ANONYMOUS, ImmutableList.copyOf(lines));
    }

    public static Comment fromText(Anchor anchor, String text) {
      return create(anchor, SPLIT_LINES.splitToList(text));
    }

    public static Comment fromText(String text) {
      return createAnonymous(SPLIT_LINES.splitToList(text));
    }

    /**
     * Returns the key which defines what this comment relates to (and thus where it should appear
     * in the XML file).
     */
    public abstract Anchor getAnchor();

    /** The lines of a single mulit-line comment. */
    // TODO: Switch to a single string (with newlines) which is what's done elsewhere.
    public abstract ImmutableList<String> getLines();

    public String toText() {
      return JOIN_LINES.join(getLines());
    }

    // Visible for AutoValue.
    Comment() {}
  }

  // Visible for AutoValue.
  NumberingScheme() {}
}
