/*
 * Copyright (C) 2016 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class to encapsulate the metadata filtering logic and restrict visibility into raw data
 * structures.
 *
 * <p>
 * WARNING: This is an internal API which is under development and subject to backwards-incompatible
 * changes without notice. Any changes are not guaranteed to be reflected in the versioning scheme
 * of the public API, nor in release notes.
 */
final class MetadataFilter {
  // The following 3 sets comprise all the PhoneMetadata fields as defined at phonemetadata.proto
  // which may be excluded from customized serializations of the binary metadata. Fields that are
  // core to the library functionality may not be listed here.
  // excludableParentFields are PhoneMetadata fields of type PhoneNumberDesc.
  // excludableChildFields are PhoneNumberDesc fields of primitive type.
  // excludableChildlessFields are PhoneMetadata fields of primitive type.
  // Currently we support only one non-primitive type and the depth of the "family tree" is 2,
  // meaning a field may have only direct descendants, who may not have descendants of their own. If
  // this changes, the blacklist handling in this class should also change.
  // @VisibleForTesting
  static final TreeSet<String> excludableParentFields = new TreeSet<String>(Arrays.asList(
      "fixedLine",
      "mobile",
      "tollFree",
      "premiumRate",
      "sharedCost",
      "personalNumber",
      "voip",
      "pager",
      "uan",
      "emergency",
      "voicemail",
      "shortCode",
      "standardRate",
      "carrierSpecific",
      "smsServices",
      "noInternationalDialling"));

  // Note: If this set changes, the descHasData implementation must change in PhoneNumberUtil.
  // The current implementation assumes that all PhoneNumberDesc fields are present here, since it
  // "clears" a PhoneNumberDesc field by simply clearing all of the fields under it. See the comment
  // above, about all 3 sets, for more about these fields.
  // @VisibleForTesting
  static final TreeSet<String> excludableChildFields = new TreeSet<String>(Arrays.asList(
      "nationalNumberPattern",
      "possibleLength",
      "possibleLengthLocalOnly",
      "exampleNumber"));

  // @VisibleForTesting
  static final TreeSet<String> excludableChildlessFields = new TreeSet<String>(Arrays.asList(
      "preferredInternationalPrefix",
      "nationalPrefix",
      "preferredExtnPrefix",
      "nationalPrefixTransformRule",
      "sameMobileAndFixedLinePattern",
      "mainCountryForCode",
      "mobileNumberPortableRegion"));

  private final TreeMap<String, TreeSet<String>> blacklist;

  // Note: If changing the blacklist here or the name of the method, update documentation about
  // affected methods at the same time:
  // https://github.com/google/libphonenumber/blob/master/FAQ.md#what-is-the-metadatalitejsmetadata_lite-option
  static MetadataFilter forLiteBuild() {
    // "exampleNumber" is a blacklist.
    return new MetadataFilter(parseFieldMapFromString("exampleNumber"));
  }

  static MetadataFilter forSpecialBuild() {
    // "mobile" is a whitelist.
    return new MetadataFilter(computeComplement(parseFieldMapFromString("mobile")));
  }

  static MetadataFilter emptyFilter() {
    // Empty blacklist, meaning we filter nothing.
    return new MetadataFilter(new TreeMap<String, TreeSet<String>>());
  }

  // @VisibleForTesting
  MetadataFilter(TreeMap<String, TreeSet<String>> blacklist) {
    this.blacklist = blacklist;
  }

  @Override
  public boolean equals(Object obj) {
    return blacklist.equals(((MetadataFilter) obj).blacklist);
  }

  @Override
  public int hashCode() {
    return blacklist.hashCode();
  }

  /**
   * Clears certain fields in {@code metadata} as defined by the {@code MetadataFilter} instance.
   * Note that this changes the mutable {@code metadata} object, and is not thread-safe. If this
   * method does not return successfully, do not assume {@code metadata} has not changed.
   *
   * @param metadata  The {@code PhoneMetadata} object to be filtered
   */
  void filterMetadata(PhoneMetadata.Builder metadata) {
    // TODO: Consider clearing if the filtered PhoneNumberDesc is empty.
    if (metadata.hasFixedLine()) {
      metadata.setFixedLine(getFiltered("fixedLine", metadata.getFixedLine()));
    }
    if (metadata.hasMobile()) {
      metadata.setMobile(getFiltered("mobile", metadata.getMobile()));
    }
    if (metadata.hasTollFree()) {
      metadata.setTollFree(getFiltered("tollFree", metadata.getTollFree()));
    }
    if (metadata.hasPremiumRate()) {
      metadata.setPremiumRate(getFiltered("premiumRate", metadata.getPremiumRate()));
    }
    if (metadata.hasSharedCost()) {
      metadata.setSharedCost(getFiltered("sharedCost", metadata.getSharedCost()));
    }
    if (metadata.hasPersonalNumber()) {
      metadata.setPersonalNumber(getFiltered("personalNumber", metadata.getPersonalNumber()));
    }
    if (metadata.hasVoip()) {
      metadata.setVoip(getFiltered("voip", metadata.getVoip()));
    }
    if (metadata.hasPager()) {
      metadata.setPager(getFiltered("pager", metadata.getPager()));
    }
    if (metadata.hasUan()) {
      metadata.setUan(getFiltered("uan", metadata.getUan()));
    }
    if (metadata.hasEmergency()) {
      metadata.setEmergency(getFiltered("emergency", metadata.getEmergency()));
    }
    if (metadata.hasVoicemail()) {
      metadata.setVoicemail(getFiltered("voicemail", metadata.getVoicemail()));
    }
    if (metadata.hasShortCode()) {
      metadata.setShortCode(getFiltered("shortCode", metadata.getShortCode()));
    }
    if (metadata.hasStandardRate()) {
      metadata.setStandardRate(getFiltered("standardRate", metadata.getStandardRate()));
    }
    if (metadata.hasCarrierSpecific()) {
      metadata.setCarrierSpecific(getFiltered("carrierSpecific", metadata.getCarrierSpecific()));
    }
    if (metadata.hasSmsServices()) {
      metadata.setSmsServices(getFiltered("smsServices", metadata.getSmsServices()));
    }
    if (metadata.hasNoInternationalDialling()) {
      metadata.setNoInternationalDialling(getFiltered("noInternationalDialling",
              metadata.getNoInternationalDialling()));
    }

    if (shouldDrop("preferredInternationalPrefix")) {
      metadata.clearPreferredInternationalPrefix();
    }
    if (shouldDrop("nationalPrefix")) {
      metadata.clearNationalPrefix();
    }
    if (shouldDrop("preferredExtnPrefix")) {
      metadata.clearPreferredExtnPrefix();
    }
    if (shouldDrop("nationalPrefixTransformRule")) {
      metadata.clearNationalPrefixTransformRule();
    }
    if (shouldDrop("sameMobileAndFixedLinePattern")) {
      metadata.clearSameMobileAndFixedLinePattern();
    }
    if (shouldDrop("mainCountryForCode")) {
      metadata.clearMainCountryForCode();
    }
    if (shouldDrop("mobileNumberPortableRegion")) {
      metadata.clearMobileNumberPortableRegion();
    }
  }

  /**
   * The input blacklist or whitelist string is expected to be of the form "a(b,c):d(e):f", where
   * b and c are children of a, e is a child of d, and f is either a parent field, a child field, or
   * a childless field. Order and whitespace don't matter. We throw RuntimeException for any
   * duplicates, malformed strings, or strings where field tokens do not correspond to strings in
   * the sets of excludable fields. We also throw RuntimeException for empty strings since such
   * strings should be treated as a special case by the flag checking code and not passed here.
   */
  // @VisibleForTesting
  static TreeMap<String, TreeSet<String>> parseFieldMapFromString(String string) {
    if (string == null) {
      throw new RuntimeException("Null string should not be passed to parseFieldMapFromString");
    }
    // Remove whitespace.
    string = string.replaceAll("\\s", "");
    if (string.isEmpty()) {
      throw new RuntimeException("Empty string should not be passed to parseFieldMapFromString");
    }

    TreeMap<String, TreeSet<String>> fieldMap = new TreeMap<String, TreeSet<String>>();
    TreeSet<String> wildcardChildren = new TreeSet<String>();
    for (String group : string.split(":", -1)) {
      int leftParenIndex = group.indexOf('(');
      int rightParenIndex = group.indexOf(')');
      if (leftParenIndex < 0 && rightParenIndex < 0) {
        if (excludableParentFields.contains(group)) {
          if (fieldMap.containsKey(group)) {
            throw new RuntimeException(group + " given more than once in " + string);
          }
          fieldMap.put(group, new TreeSet<String>(excludableChildFields));
        } else if (excludableChildlessFields.contains(group)) {
          if (fieldMap.containsKey(group)) {
            throw new RuntimeException(group + " given more than once in " + string);
          }
          fieldMap.put(group, new TreeSet<String>());
        } else if (excludableChildFields.contains(group)) {
          if (wildcardChildren.contains(group)) {
            throw new RuntimeException(group + " given more than once in " + string);
          }
          wildcardChildren.add(group);
        } else {
          throw new RuntimeException(group + " is not a valid token");
        }
      } else if (leftParenIndex > 0 && rightParenIndex == group.length() - 1) {
        // We don't check for duplicate parentheses or illegal characters since these will be caught
        // as not being part of valid field tokens.
        String parent = group.substring(0, leftParenIndex);
        if (!excludableParentFields.contains(parent)) {
          throw new RuntimeException(parent + " is not a valid parent token");
        }
        if (fieldMap.containsKey(parent)) {
          throw new RuntimeException(parent + " given more than once in " + string);
        }
        TreeSet<String> children = new TreeSet<String>();
        for (String child : group.substring(leftParenIndex + 1, rightParenIndex).split(",", -1)) {
          if (!excludableChildFields.contains(child)) {
            throw new RuntimeException(child + " is not a valid child token");
          }
          if (!children.add(child)) {
            throw new RuntimeException(child + " given more than once in " + group);
          }
        }
        fieldMap.put(parent, children);
      } else {
        throw new RuntimeException("Incorrect location of parantheses in " + group);
      }
    }
    for (String wildcardChild : wildcardChildren) {
      for (String parent : excludableParentFields) {
        TreeSet<String> children = fieldMap.get(parent);
        if (children == null) {
          children = new TreeSet<String>();
          fieldMap.put(parent, children);
        }
        if (!children.add(wildcardChild)
            && fieldMap.get(parent).size() != excludableChildFields.size()) {
          // The map already contains parent -> wildcardChild but not all possible children.
          // So wildcardChild was given explicitly as a child of parent, which is a duplication
          // since it's also given as a wildcard child.
          throw new RuntimeException(
              wildcardChild + " is present by itself so remove it from " + parent + "'s group");
        }
      }
    }
    return fieldMap;
  }

  // Does not check that legal tokens are used, assuming that fieldMap is constructed using
  // parseFieldMapFromString(String) which does check. If fieldMap contains illegal tokens or parent
  // fields with no children or other unexpected state, the behavior of this function is undefined.
  // @VisibleForTesting
  static TreeMap<String, TreeSet<String>> computeComplement(
      TreeMap<String, TreeSet<String>> fieldMap) {
    TreeMap<String, TreeSet<String>> complement = new TreeMap<String, TreeSet<String>>();
    for (String parent : excludableParentFields) {
      if (!fieldMap.containsKey(parent)) {
        complement.put(parent, new TreeSet<String>(excludableChildFields));
      } else {
        TreeSet<String> otherChildren = fieldMap.get(parent);
        // If the other map has all the children for this parent then we don't want to include the
        // parent as a key.
        if (otherChildren.size() != excludableChildFields.size()) {
          TreeSet<String> children = new TreeSet<String>();
          for (String child : excludableChildFields) {
            if (!otherChildren.contains(child)) {
              children.add(child);
            }
          }
          complement.put(parent, children);
        }
      }
    }
    for (String childlessField : excludableChildlessFields) {
      if (!fieldMap.containsKey(childlessField)) {
        complement.put(childlessField, new TreeSet<String>());
      }
    }
    return complement;
  }

  // @VisibleForTesting
  boolean shouldDrop(String parent, String child) {
    if (!excludableParentFields.contains(parent)) {
      throw new RuntimeException(parent + " is not an excludable parent field");
    }
    if (!excludableChildFields.contains(child)) {
      throw new RuntimeException(child + " is not an excludable child field");
    }
    return blacklist.containsKey(parent) && blacklist.get(parent).contains(child);
  }

  // @VisibleForTesting
  boolean shouldDrop(String childlessField) {
    if (!excludableChildlessFields.contains(childlessField)) {
      throw new RuntimeException(childlessField + " is not an excludable childless field");
    }
    return blacklist.containsKey(childlessField);
  }

  private PhoneNumberDesc getFiltered(String type, PhoneNumberDesc desc) {
    PhoneNumberDesc.Builder builder = PhoneNumberDesc.newBuilder().mergeFrom(desc);
    if (shouldDrop(type, "nationalNumberPattern")) {
      builder.clearNationalNumberPattern();
    }
    if (shouldDrop(type, "possibleLength")) {
      builder.clearPossibleLength();
    }
    if (shouldDrop(type, "possibleLengthLocalOnly")) {
      builder.clearPossibleLengthLocalOnly();
    }
    if (shouldDrop(type, "exampleNumber")) {
      builder.clearExampleNumber();
    }
    return builder.build();
  }
}
