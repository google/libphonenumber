/*
 * Copyright (C) 2010 Google Inc.
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

/**
 * Definition of the class representing metadata for international telephone numbers. This class is
 * hand created based on the class file compiled from phonemetadata.proto. Please refer to that file
 * for detailed descriptions of the meaning of each field.
 */

package com.google.i18n.phonenumbers;

import java.io.Serializable;

public final class Phonemetadata {
  private Phonemetadata() {}
  public static final class NumberFormat implements Serializable {
    private static final long serialVersionUID = 1;
    public NumberFormat() {}

    // required string pattern = 1;
    private boolean hasPattern;
    private String pattern_ = "";
    public boolean hasPattern() { return hasPattern; }
    public String getPattern() { return pattern_; }
    public NumberFormat setPattern(String value) {
      hasPattern = true;
      pattern_ = value;
      return this;
    }
    public NumberFormat clearPattern() {
      hasPattern = false;
      pattern_ = "";
      return this;
    }

    // required string format = 2;
    private boolean hasFormat;
    private String format_ = "";
    public boolean hasFormat() { return hasFormat; }
    public String getFormat() { return format_; }
    public NumberFormat setFormat(String value) {
      hasFormat = true;
      format_ = value;
      return this;
    }
    public NumberFormat clearFormat() {
      hasFormat = false;
      format_ = "";
      return this;
    }

    // optional string leading_digits = 3;
    private boolean hasLeadingDigits;
    private String leadingDigits_ = "";
    public boolean hasLeadingDigits() { return hasLeadingDigits; }
    public String getLeadingDigits() { return leadingDigits_; }
    public NumberFormat setLeadingDigits(String value) {
      hasLeadingDigits = true;
      leadingDigits_ = value;
      return this;
    }
    public NumberFormat clearLeadingDigits() {
      hasLeadingDigits = false;
      leadingDigits_ = "";
      return this;
    }

    // optional string national_prefix_formatting_rule = 4;
    private boolean hasNationalPrefixFormattingRule;
    private String nationalPrefixFormattingRule_ = "";
    public boolean hasNationalPrefixFormattingRule() { return hasNationalPrefixFormattingRule; }
    public String getNationalPrefixFormattingRule() { return nationalPrefixFormattingRule_; }
    public NumberFormat setNationalPrefixFormattingRule(String value) {
      hasNationalPrefixFormattingRule = true;
      nationalPrefixFormattingRule_ = value;
      return this;
    }
    public NumberFormat clearNationalPrefixFormattingRule() {
      hasNationalPrefixFormattingRule = false;
      nationalPrefixFormattingRule_ = "";
      return this;
    }

    public final NumberFormat clear() {
      clearPattern();
      clearFormat();
      clearLeadingDigits();
      clearNationalPrefixFormattingRule();
      return this;
    }

    public NumberFormat mergeFrom(NumberFormat other) {
      if (other.hasPattern()) {
        setPattern(other.getPattern());
      }
      if (other.hasFormat()) {
        setFormat(other.getFormat());
      }
      if (other.hasLeadingDigits()) {
        setLeadingDigits(other.getLeadingDigits());
      }
      if (other.hasNationalPrefixFormattingRule()) {
        setNationalPrefixFormattingRule(other.getNationalPrefixFormattingRule());
      }
      return this;
    }

    public final boolean isInitialized() {
      if (!hasPattern) return false;
      if (!hasFormat) return false;
      return true;
    }
  }

  public static final class PhoneNumberDesc implements Serializable {
    private static final long serialVersionUID = 1;
    public PhoneNumberDesc() {}

    // optional string national_number_pattern = 2;
    private boolean hasNationalNumberPattern;
    private String nationalNumberPattern_ = "";
    public boolean hasNationalNumberPattern() { return hasNationalNumberPattern; }
    public String getNationalNumberPattern() { return nationalNumberPattern_; }
    public PhoneNumberDesc setNationalNumberPattern(String value) {
      hasNationalNumberPattern = true;
      nationalNumberPattern_ = value;
      return this;
    }
    public PhoneNumberDesc clearNationalNumberPattern() {
      hasNationalNumberPattern = false;
      nationalNumberPattern_ = "";
      return this;
    }

    // optional string possible_number_pattern = 3;
    private boolean hasPossibleNumberPattern;
    private String possibleNumberPattern_ = "";
    public boolean hasPossibleNumberPattern() { return hasPossibleNumberPattern; }
    public String getPossibleNumberPattern() { return possibleNumberPattern_; }
    public PhoneNumberDesc setPossibleNumberPattern(String value) {
      hasPossibleNumberPattern = true;
      possibleNumberPattern_ = value;
      return this;
    }
    public PhoneNumberDesc clearPossibleNumberPattern() {
      hasPossibleNumberPattern = false;
      possibleNumberPattern_ = "";
      return this;
    }

    // optional string example_number = 6;
    private boolean hasExampleNumber;
    private String exampleNumber_ = "";
    public boolean hasExampleNumber() { return hasExampleNumber; }
    public String getExampleNumber() { return exampleNumber_; }
    public PhoneNumberDesc setExampleNumber(String value) {
      hasExampleNumber = true;
      exampleNumber_ = value;
      return this;
    }
    public PhoneNumberDesc clearExampleNumber() {
      hasExampleNumber = false;
      exampleNumber_ = "";
      return this;
    }

    public final PhoneNumberDesc clear() {
      clearNationalNumberPattern();
      clearPossibleNumberPattern();
      clearExampleNumber();
      return this;
    }

    public PhoneNumberDesc mergeFrom(PhoneNumberDesc other) {
      if (other.hasNationalNumberPattern()) {
        setNationalNumberPattern(other.getNationalNumberPattern());
      }
      if (other.hasPossibleNumberPattern()) {
        setPossibleNumberPattern(other.getPossibleNumberPattern());
      }
      if (other.hasExampleNumber()) {
        setExampleNumber(other.getExampleNumber());
      }
      return this;
    }

    public boolean exactlySameAs(PhoneNumberDesc other) {
      return nationalNumberPattern_.equals(other.nationalNumberPattern_) &&
          possibleNumberPattern_.equals(other.possibleNumberPattern_) &&
          exampleNumber_.equals(other.exampleNumber_);
    }
  }

  public static final class PhoneMetadata implements Serializable {
    private static final long serialVersionUID = 1;
    public PhoneMetadata() {}

    // required PhoneNumberDesc general_desc = 1;
    private boolean hasGeneralDesc;
    private PhoneNumberDesc generalDesc_ = null;
    public boolean hasGeneralDesc() { return hasGeneralDesc; }
    public PhoneNumberDesc getGeneralDesc() { return generalDesc_; }
    public PhoneMetadata setGeneralDesc(PhoneNumberDesc value) {
      if (value == null) {
        throw new NullPointerException();
      }
      hasGeneralDesc = true;
      generalDesc_ = value;
      return this;
    }
    public PhoneMetadata clearGeneralDesc() {
      hasGeneralDesc = false;
      generalDesc_ = null;
      return this;
    }

    // required PhoneNumberDesc fixed_line = 2;
    private boolean hasFixedLine;
    private PhoneNumberDesc fixedLine_ = null;
    public boolean hasFixedLine() { return hasFixedLine; }
    public PhoneNumberDesc getFixedLine() { return fixedLine_; }
    public PhoneMetadata setFixedLine(PhoneNumberDesc value) {
      if (value == null) {
        throw new NullPointerException();
      }
      hasFixedLine = true;
      fixedLine_ = value;
      return this;
    }
    public PhoneMetadata clearFixedLine() {
      hasFixedLine = false;
      fixedLine_ = null;
      return this;
    }

    // required PhoneNumberDesc mobile = 3;
    private boolean hasMobile;
    private PhoneNumberDesc mobile_ = null;
    public boolean hasMobile() { return hasMobile; }
    public PhoneNumberDesc getMobile() { return mobile_; }
    public PhoneMetadata setMobile(PhoneNumberDesc value) {
      if (value == null) {
        throw new NullPointerException();
      }
      hasMobile = true;
      mobile_ = value;
      return this;
    }
    public PhoneMetadata clearMobile() {
      hasMobile = false;
      mobile_ = null;
      return this;
    }

    // required PhoneNumberDesc toll_free = 4;
    private boolean hasTollFree;
    private PhoneNumberDesc tollFree_ = null;
    public boolean hasTollFree() { return hasTollFree; }
    public PhoneNumberDesc getTollFree() { return tollFree_; }
    public PhoneMetadata setTollFree(PhoneNumberDesc value) {
      if (value == null) {
        throw new NullPointerException();
      }
      hasTollFree = true;
      tollFree_ = value;
      return this;
    }
    public PhoneMetadata clearTollFree() {
      hasTollFree = false;
      tollFree_ = null;
      return this;
    }

    // required PhoneNumberDesc premium_rate = 5;
    private boolean hasPremiumRate;
    private PhoneNumberDesc premiumRate_ = null;
    public boolean hasPremiumRate() { return hasPremiumRate; }
    public PhoneNumberDesc getPremiumRate() { return premiumRate_; }
    public PhoneMetadata setPremiumRate(PhoneNumberDesc value) {
      if (value == null) {
        throw new NullPointerException();
      }
      hasPremiumRate = true;
      premiumRate_ = value;
      return this;
    }
    public PhoneMetadata clearPremiumRate() {
      hasPremiumRate = false;
      premiumRate_ = null;
      return this;
    }

    // required PhoneNumberDesc shared_cost = 6;
    private boolean hasSharedCost;
    private PhoneNumberDesc sharedCost_ = null;
    public boolean hasSharedCost() { return hasSharedCost; }
    public PhoneNumberDesc getSharedCost() { return sharedCost_; }
    public PhoneMetadata setSharedCost(PhoneNumberDesc value) {
      if (value == null) {
        throw new NullPointerException();
      }
      hasSharedCost = true;
      sharedCost_ = value;
      return this;
    }
    public PhoneMetadata clearSharedCost() {
      hasSharedCost = false;
      sharedCost_ = null;
      return this;
    }

    // required PhoneNumberDesc personal_number = 7;
    private boolean hasPersonalNumber;
    private PhoneNumberDesc personalNumber_ = null;
    public boolean hasPersonalNumber() { return hasPersonalNumber; }
    public PhoneNumberDesc getPersonalNumber() { return personalNumber_; }
    public PhoneMetadata setPersonalNumber(PhoneNumberDesc value) {
      if (value == null) {
        throw new NullPointerException();
      }
      hasPersonalNumber = true;
      personalNumber_ = value;
      return this;
    }
    public PhoneMetadata clearPersonalNumber() {
      hasPersonalNumber = false;
      personalNumber_ = null;
      return this;
    }

    // required PhoneNumberDesc voip = 8;
    private boolean hasVoip;
    private PhoneNumberDesc voip_ = null;
    public boolean hasVoip() { return hasVoip; }
    public PhoneNumberDesc getVoip() { return voip_; }
    public PhoneMetadata setVoip(PhoneNumberDesc value) {
      if (value == null) {
        throw new NullPointerException();
      }
      hasVoip = true;
      voip_ = value;
      return this;
    }
    public PhoneMetadata clearVoip() {
      hasVoip = false;
      voip_ = null;
      return this;
    }

    // required string id = 9;
    private boolean hasId;
    private String id_ = "";
    public boolean hasId() { return hasId; }
    public String getId() { return id_; }
    public PhoneMetadata setId(String value) {
      hasId = true;
      id_ = value;
      return this;
    }
    public PhoneMetadata clearId() {
      hasId = false;
      id_ = "";
      return this;
    }

    // required int32 country_code = 10;
    private boolean hasCountryCode;
    private int countryCode_ = 0;
    public boolean hasCountryCode() { return hasCountryCode; }
    public int getCountryCode() { return countryCode_; }
    public PhoneMetadata setCountryCode(int value) {
      hasCountryCode = true;
      countryCode_ = value;
      return this;
    }
    public PhoneMetadata clearCountryCode() {
      hasCountryCode = false;
      countryCode_ = 0;
      return this;
    }

    // required string international_prefix = 11;
    private boolean hasInternationalPrefix;
    private String internationalPrefix_ = "";
    public boolean hasInternationalPrefix() { return hasInternationalPrefix; }
    public String getInternationalPrefix() { return internationalPrefix_; }
    public PhoneMetadata setInternationalPrefix(String value) {
      hasInternationalPrefix = true;
      internationalPrefix_ = value;
      return this;
    }
    public PhoneMetadata clearInternationalPrefix() {
      hasInternationalPrefix = false;
      internationalPrefix_ = "";
      return this;
    }

    // optional string preferred_international_prefix = 17;
    private boolean hasPreferredInternationalPrefix;
    private String preferredInternationalPrefix_ = "";
    public boolean hasPreferredInternationalPrefix() { return hasPreferredInternationalPrefix; }
    public String getPreferredInternationalPrefix() { return preferredInternationalPrefix_; }
    public PhoneMetadata setPreferredInternationalPrefix(String value) {
      hasPreferredInternationalPrefix = true;
      preferredInternationalPrefix_ = value;
      return this;
    }
    public PhoneMetadata clearPreferredInternationalPrefix() {
      hasPreferredInternationalPrefix = false;
      preferredInternationalPrefix_ = "";
      return this;
    }

    // optional string national_prefix = 12;
    private boolean hasNationalPrefix;
    private String nationalPrefix_ = "";
    public boolean hasNationalPrefix() { return hasNationalPrefix; }
    public String getNationalPrefix() { return nationalPrefix_; }
    public PhoneMetadata setNationalPrefix(String value) {
      hasNationalPrefix = true;
      nationalPrefix_ = value;
      return this;
    }
    public PhoneMetadata clearNationalPrefix() {
      hasNationalPrefix = false;
      nationalPrefix_ = "";
      return this;
    }

    // optional string preferred_extn_prefix = 13;
    private boolean hasPreferredExtnPrefix;
    private String preferredExtnPrefix_ = "";
    public boolean hasPreferredExtnPrefix() { return hasPreferredExtnPrefix; }
    public String getPreferredExtnPrefix() { return preferredExtnPrefix_; }
    public PhoneMetadata setPreferredExtnPrefix(String value) {
      hasPreferredExtnPrefix = true;
      preferredExtnPrefix_ = value;
      return this;
    }
    public PhoneMetadata clearPreferredExtnPrefix() {
      hasPreferredExtnPrefix = false;
      preferredExtnPrefix_ = "";
      return this;
    }

    // optional string national_prefix_for_parsing = 15;
    private boolean hasNationalPrefixForParsing;
    private String nationalPrefixForParsing_ = "";
    public boolean hasNationalPrefixForParsing() { return hasNationalPrefixForParsing; }
    public String getNationalPrefixForParsing() { return nationalPrefixForParsing_; }
    public PhoneMetadata setNationalPrefixForParsing(String value) {
      hasNationalPrefixForParsing = true;
      nationalPrefixForParsing_ = value;
      return this;
    }
    public PhoneMetadata clearNationalPrefixForParsing() {
      hasNationalPrefixForParsing = false;
      nationalPrefixForParsing_ = "";
      return this;
    }

    // optional string national_prefix_transform_rule = 16;
    private boolean hasNationalPrefixTransformRule;
    private String nationalPrefixTransformRule_ = "";
    public boolean hasNationalPrefixTransformRule() { return hasNationalPrefixTransformRule; }
    public String getNationalPrefixTransformRule() { return nationalPrefixTransformRule_; }
    public PhoneMetadata setNationalPrefixTransformRule(String value) {
      hasNationalPrefixTransformRule = true;
      nationalPrefixTransformRule_ = value;
      return this;
    }
    public PhoneMetadata clearNationalPrefixTransformRule() {
      hasNationalPrefixTransformRule = false;
      nationalPrefixTransformRule_ = "";
      return this;
    }

    // optional bool same_mobile_and_fixed_line_pattern = 18 [default = false];
    private boolean hasSameMobileAndFixedLinePattern;
    private boolean sameMobileAndFixedLinePattern_ = false;
    public boolean hasSameMobileAndFixedLinePattern() { return hasSameMobileAndFixedLinePattern; }
    public boolean getSameMobileAndFixedLinePattern() { return sameMobileAndFixedLinePattern_; }
    public PhoneMetadata setSameMobileAndFixedLinePattern(boolean value) {
      hasSameMobileAndFixedLinePattern = true;
      sameMobileAndFixedLinePattern_ = value;
      return this;
    }
    public PhoneMetadata clearSameMobileAndFixedLinePattern() {
      hasSameMobileAndFixedLinePattern = false;
      sameMobileAndFixedLinePattern_ = false;
      return this;
    }

    // repeated NumberFormat number_format = 19;
    private java.util.List<NumberFormat> numberFormat_ =
      java.util.Collections.emptyList();
    public java.util.List<NumberFormat> getNumberFormatList() {
      return numberFormat_;
    }
    public int getNumberFormatCount() { return numberFormat_.size(); }
    public NumberFormat getNumberFormat(int index) {
      return numberFormat_.get(index);
    }
    public PhoneMetadata setNumberFormat(int index, NumberFormat value) {
      if (value == null) {
        throw new NullPointerException();
      }
      numberFormat_.set(index, value);
      return this;
    }
    public PhoneMetadata addNumberFormat(NumberFormat value) {
      if (value == null) {
        throw new NullPointerException();
      }
      if (numberFormat_.isEmpty()) {
        numberFormat_ = new java.util.ArrayList<NumberFormat>();
      }
      numberFormat_.add(value);
      return this;
    }
    public PhoneMetadata clearNumberFormat() {
      numberFormat_ = java.util.Collections.emptyList();
      return this;
    }

    // repeated NumberFormat intl_number_format = 20;
    private java.util.List<NumberFormat> intlNumberFormat_ =
      java.util.Collections.emptyList();
    public java.util.List<NumberFormat> getIntlNumberFormatList() {
      return intlNumberFormat_;
    }
    public int getIntlNumberFormatCount() { return intlNumberFormat_.size(); }
    public NumberFormat getIntlNumberFormat(int index) {
      return intlNumberFormat_.get(index);
    }
    public PhoneMetadata setIntlNumberFormat(int index, NumberFormat value) {
      if (value == null) {
        throw new NullPointerException();
      }
      intlNumberFormat_.set(index, value);
      return this;
    }
    public PhoneMetadata addIntlNumberFormat(NumberFormat value) {
      if (value == null) {
        throw new NullPointerException();
      }
      if (intlNumberFormat_.isEmpty()) {
        intlNumberFormat_ = new java.util.ArrayList<NumberFormat>();
      }
      intlNumberFormat_.add(value);
      return this;
    }
    public PhoneMetadata clearIntlNumberFormat() {
      intlNumberFormat_ = java.util.Collections.emptyList();
      return this;
    }

    // optional string national_prefix_formatting_rule = 21;
    private boolean hasNationalPrefixFormattingRule;
    private String nationalPrefixFormattingRule_ = "";
    public boolean hasNationalPrefixFormattingRule() { return hasNationalPrefixFormattingRule; }
    public String getNationalPrefixFormattingRule() { return nationalPrefixFormattingRule_; }
    public PhoneMetadata setNationalPrefixFormattingRule(String value) {
      hasNationalPrefixFormattingRule = true;
      nationalPrefixFormattingRule_ = value;
      return this;
    }
    public PhoneMetadata clearNationalPrefixFormattingRule() {
      hasNationalPrefixFormattingRule = false;
      nationalPrefixFormattingRule_ = "";
      return this;
    }

    public final PhoneMetadata clear() {
      clearGeneralDesc();
      clearFixedLine();
      clearMobile();
      clearTollFree();
      clearPremiumRate();
      clearSharedCost();
      clearPersonalNumber();
      clearVoip();
      clearId();
      clearCountryCode();
      clearInternationalPrefix();
      clearPreferredInternationalPrefix();
      clearNationalPrefix();
      clearPreferredExtnPrefix();
      clearNationalPrefixForParsing();
      clearNationalPrefixTransformRule();
      clearSameMobileAndFixedLinePattern();
      clearNumberFormat();
      clearIntlNumberFormat();
      clearNationalPrefixFormattingRule();
      return this;
    }
  }

  public static final class PhoneMetadataCollection implements Serializable {
    private static final long serialVersionUID = 1;
    public PhoneMetadataCollection() {}
    
    // repeated PhoneMetadata metadata = 1;
    private java.util.List<PhoneMetadata> metadata_ =
      java.util.Collections.emptyList();
    public java.util.List<PhoneMetadata> getMetadataList() {
      return metadata_;
    }
    public int getMetadataCount() { return metadata_.size(); }
    public PhoneMetadata getMetadata(int index) {
      return metadata_.get(index);
    }
    public PhoneMetadataCollection setMetadata(int index, PhoneMetadata value) {
      if (value == null) {
        throw new NullPointerException();
      }
      metadata_.set(index, value);
      return this;
    }
    public PhoneMetadataCollection addMetadata(PhoneMetadata value) {
      if (value == null) {
        throw new NullPointerException();
      }
      if (metadata_.isEmpty()) {
        metadata_ = new java.util.ArrayList<PhoneMetadata>();
      }
      metadata_.add(value);
      return this;
    }
    public PhoneMetadataCollection clearMetadata() {
      metadata_ = java.util.Collections.emptyList();
      return this;
    }
    
    public final PhoneMetadataCollection clear() {
      clearMetadata();
      return this;
    }
  }
}
