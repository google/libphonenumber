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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.FIXED_LINE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.MOBILE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.PAGER;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.PERSONAL_NUMBER;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.PREMIUM_RATE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.SHARED_COST;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.TOLL_FREE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.UAN;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.VOICEMAIL;
import static com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType.VOIP;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_FIXED_LINE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_MOBILE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_PAGER;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_PERSONAL_NUMBER;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_PREMIUM_RATE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_SHARED_COST;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_TOLL_FREE;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_UAN;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_UNKNOWN;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_VOICEMAIL;
import static com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType.XML_VOIP;
import static java.util.function.Function.identity;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.i18n.phonenumbers.metadata.proto.Types.ValidNumberType;
import com.google.i18n.phonenumbers.metadata.proto.Types.XmlNumberType;
import com.google.i18n.phonenumbers.metadata.proto.Types.XmlShortcodeType;
import java.util.Optional;
import java.util.stream.Stream;

/** Static utility for conversion of number types. */
public final class Types {
  private static final ImmutableBiMap<String, XmlNumberType> XML_TYPE_MAP =
      Stream.of(XmlNumberType.values())
          .filter(t -> t != XML_UNKNOWN && t != XmlNumberType.UNRECOGNIZED)
          .collect(toImmutableBiMap(Types::toXmlName, identity()));

  // Map the subset of XmlNumberType values which correspond to valid number types. Note that while
  // FIXED_LINE and MOBILE exist in both types, and can be converted, their semantics change.
  private static final ImmutableBiMap<XmlNumberType, ValidNumberType> XML_TO_SCHEMA_TYPE_MAP =
      ImmutableBiMap.<XmlNumberType, ValidNumberType>builder()
          .put(XML_FIXED_LINE, FIXED_LINE)
          .put(XML_MOBILE, MOBILE)
          .put(XML_PAGER, PAGER)
          .put(XML_TOLL_FREE, TOLL_FREE)
          .put(XML_PREMIUM_RATE, PREMIUM_RATE)
          .put(XML_SHARED_COST, SHARED_COST)
          .put(XML_PERSONAL_NUMBER, PERSONAL_NUMBER)
          .put(XML_VOIP, VOIP)
          .put(XML_UAN, UAN)
          .put(XML_VOICEMAIL, VOICEMAIL)
          .buildOrThrow();

  /** Returns the set of valid XML type names. */
  public static ImmutableSet<String> getXmlNames() {
    return XML_TYPE_MAP.keySet();
  }

  /** Returns the XML element name based on the given XML range type. */
  public static String toXmlName(XmlNumberType type) {
    checkState(type.name().startsWith("XML_"), "Bad type: %s", type);
    return UPPER_UNDERSCORE.to(LOWER_CAMEL, type.name().substring(4));
  }

  /** Returns the XML element name based on the given XML shortcode type. */
  public static String toXmlName(XmlShortcodeType type) {
    checkState(type.name().startsWith("SC_"), "Bad type: %s", type);
    return UPPER_UNDERSCORE.to(LOWER_CAMEL, type.name().substring(3));
  }

  /**
   * Returns the XML range type based on the given case-sensitive XML element name (e.g.
   * "fixedLine").
   */
  public static Optional<XmlNumberType> forXmlName(String xmlName) {
    return Optional.ofNullable(XML_TYPE_MAP.get(xmlName));
  }

  /** Returns the {@code ValidNumberType} equivalent of the given XML range type (if it exists). */
  public static Optional<ValidNumberType> toSchemaType(XmlNumberType rangeType) {
    return Optional.ofNullable(XML_TO_SCHEMA_TYPE_MAP.get(rangeType));
  }

  /** Returns the {@code XmlNumberType} equivalent of the given schema range type (if it exists). */
  public static Optional<XmlNumberType> toXmlType(ValidNumberType schemaType) {
    return Optional.ofNullable(XML_TO_SCHEMA_TYPE_MAP.inverse().get(schemaType));
  }

  private Types() {}
}
