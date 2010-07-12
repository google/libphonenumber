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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public final class Phonemetadata {
  private Phonemetadata() {}
  public static final class NumberFormat implements Externalizable {
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

    // optional string domestic_carrier_code_formatting_rule = 5;
    private boolean hasDomesticCarrierCodeFormattingRule;
    private String domesticCarrierCodeFormattingRule_ = "";
    public boolean hasDomesticCarrierCodeFormattingRule() {
      return hasDomesticCarrierCodeFormattingRule; }
    public String getDomesticCarrierCodeFormattingRule() {
      return domesticCarrierCodeFormattingRule_; }
    public NumberFormat setDomesticCarrierCodeFormattingRule(String value) {
      hasDomesticCarrierCodeFormattingRule = true;
      domesticCarrierCodeFormattingRule_ = value;
      return this;
    }

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeUTF(pattern_);
      objectOutput.writeUTF(format_);
      objectOutput.writeBoolean(hasLeadingDigits);
      if (hasLeadingDigits) {
        objectOutput.writeUTF(leadingDigits_);
      }
      objectOutput.writeBoolean(hasNationalPrefixFormattingRule);
      if (hasNationalPrefixFormattingRule) {
        objectOutput.writeUTF(nationalPrefixFormattingRule_);
      }
      objectOutput.writeBoolean(hasDomesticCarrierCodeFormattingRule);
      if (hasDomesticCarrierCodeFormattingRule) {
        objectOutput.writeUTF(domesticCarrierCodeFormattingRule_);
      }
    }

    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      setPattern(objectInput.readUTF());
      setFormat(objectInput.readUTF());
      if (objectInput.readBoolean()) {
        setLeadingDigits(objectInput.readUTF());
      }
      if (objectInput.readBoolean()) {
        setNationalPrefixFormattingRule(objectInput.readUTF());
      }
      if (objectInput.readBoolean()) {
        setDomesticCarrierCodeFormattingRule(objectInput.readUTF());
      }
    }
  }

  public static final class PhoneNumberDesc implements Externalizable {
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

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeBoolean(hasNationalNumberPattern);
      if (hasNationalNumberPattern) {
        objectOutput.writeUTF(nationalNumberPattern_);
      }

      objectOutput.writeBoolean(hasPossibleNumberPattern);
      if (hasPossibleNumberPattern) {
        objectOutput.writeUTF(possibleNumberPattern_);
      }

      objectOutput.writeBoolean(hasExampleNumber);
      if (hasExampleNumber) {
        objectOutput.writeUTF(exampleNumber_);
      }
    }

    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      if (objectInput.readBoolean()) {
        setNationalNumberPattern(objectInput.readUTF());
      }

      if (objectInput.readBoolean()) {
        setPossibleNumberPattern(objectInput.readUTF());
      }

      if (objectInput.readBoolean()) {
        setExampleNumber(objectInput.readUTF());
      }
    }
  }

  public static final class PhoneMetadata implements Externalizable {
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

    // repeated NumberFormat number_format = 19;
    private java.util.List<NumberFormat> numberFormat_ = new java.util.ArrayList<NumberFormat>();
    public java.util.List<NumberFormat> getNumberFormatList() {
      return numberFormat_;
    }
    public int getNumberFormatCount() { return numberFormat_.size(); }
    public NumberFormat getNumberFormat(int index) {
      return numberFormat_.get(index);
    }
    public PhoneMetadata addNumberFormat(NumberFormat value) {
      if (value == null) {
        throw new NullPointerException();
      }
      numberFormat_.add(value);
      return this;
    }

    // repeated NumberFormat intl_number_format = 20;
    private java.util.List<NumberFormat> intlNumberFormat_ =
        new java.util.ArrayList<NumberFormat>();
    public java.util.List<NumberFormat> getIntlNumberFormatList() {
      return intlNumberFormat_;
    }
    public int getIntlNumberFormatCount() { return intlNumberFormat_.size(); }
    public NumberFormat getIntlNumberFormat(int index) {
      return intlNumberFormat_.get(index);
    }

    public PhoneMetadata addIntlNumberFormat(NumberFormat value) {
      if (value == null) {
        throw new NullPointerException();
      }
      intlNumberFormat_.add(value);
      return this;
    }

    // optional bool main_country_for_code = 22 [default = false];
    private boolean hasMainCountryForCode;
    private boolean mainCountryForCode_ = false;
    public boolean hasMainCountryForCode() { return hasMainCountryForCode; }
    public boolean getMainCountryForCode() { return mainCountryForCode_; }
    public PhoneMetadata setMainCountryForCode(boolean value) {
      hasMainCountryForCode = true;
      mainCountryForCode_ = value;
      return this;
    }

    // optional string leading_digits = 23;
    private boolean hasLeadingDigits;
    private String leadingDigits_ = "";
    public boolean hasLeadingDigits() { return hasLeadingDigits; }
    public String getLeadingDigits() { return leadingDigits_; }
    public PhoneMetadata setLeadingDigits(String value) {
      hasLeadingDigits = true;
      leadingDigits_ = value;
      return this;
    }    

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeBoolean(hasGeneralDesc);
      if (hasGeneralDesc) {
        generalDesc_.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(hasFixedLine);
      if (hasFixedLine) {
        fixedLine_.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(hasMobile);
      if (hasMobile) {
        mobile_.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(hasTollFree);
      if (hasTollFree) {
        tollFree_.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(hasPremiumRate);
      if (hasPremiumRate) {
        premiumRate_.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(hasSharedCost);
      if (hasSharedCost) {
        sharedCost_.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(hasPersonalNumber);
      if (hasPersonalNumber) {
        personalNumber_.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(hasVoip);
      if (hasVoip) {
        voip_.writeExternal(objectOutput);
      }

      objectOutput.writeUTF(id_);
      objectOutput.writeInt(countryCode_);
      objectOutput.writeUTF(internationalPrefix_);

      objectOutput.writeBoolean(hasPreferredInternationalPrefix);
      if (hasPreferredInternationalPrefix) {
        objectOutput.writeUTF(preferredInternationalPrefix_);
      }

      objectOutput.writeBoolean(hasNationalPrefix);
      if (hasNationalPrefix) {
        objectOutput.writeUTF(nationalPrefix_);
      }

      objectOutput.writeBoolean(hasPreferredExtnPrefix);
      if (hasPreferredExtnPrefix) {
        objectOutput.writeUTF(preferredExtnPrefix_);
      }

      objectOutput.writeBoolean(hasNationalPrefixForParsing);
      if (hasNationalPrefixForParsing) {
        objectOutput.writeUTF(nationalPrefixForParsing_);
      }

      objectOutput.writeBoolean(hasNationalPrefixTransformRule);
      if (hasNationalPrefixTransformRule) {
        objectOutput.writeUTF(nationalPrefixTransformRule_);
      }

      objectOutput.writeBoolean(sameMobileAndFixedLinePattern_);

      int numberFormatSize = getNumberFormatCount();
      objectOutput.writeInt(numberFormatSize);
      for (int i = 0; i < numberFormatSize; i++) {
        numberFormat_.get(i).writeExternal(objectOutput);
      }

      int intlNumberFormatSize = getIntlNumberFormatCount();
      objectOutput.writeInt(intlNumberFormatSize);
      for (int i = 0; i < intlNumberFormatSize; i++) {
        intlNumberFormat_.get(i).writeExternal(objectOutput);
      }

      objectOutput.writeBoolean(mainCountryForCode_);

      objectOutput.writeBoolean(hasLeadingDigits);
      if (hasLeadingDigits) {
        objectOutput.writeUTF(leadingDigits_);
      }
    }

    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      boolean hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        setGeneralDesc(desc);
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        setFixedLine(desc);
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        setMobile(desc);
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        setTollFree(desc);
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        setPremiumRate(desc);
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        setSharedCost(desc);
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        setPersonalNumber(desc);
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        setVoip(desc);
      }

      setId(objectInput.readUTF());
      setCountryCode(objectInput.readInt());
      setInternationalPrefix(objectInput.readUTF());

      boolean hasString = objectInput.readBoolean();
      if (hasString) {
        setPreferredInternationalPrefix(objectInput.readUTF());
      }

      hasString = objectInput.readBoolean();
      if (hasString) {
        setNationalPrefix(objectInput.readUTF());
      }

      hasString = objectInput.readBoolean();
      if (hasString) {
        setPreferredExtnPrefix(objectInput.readUTF());
      }

      hasString = objectInput.readBoolean();
      if (hasString) {
        setNationalPrefixForParsing(objectInput.readUTF());
      }

      hasString = objectInput.readBoolean();
      if (hasString) {
        setNationalPrefixTransformRule(objectInput.readUTF());
      }

      setSameMobileAndFixedLinePattern(objectInput.readBoolean());

      int nationalFormatSize = objectInput.readInt();
      for (int i = 0; i < nationalFormatSize; i++) {
        NumberFormat numFormat = new NumberFormat();
        numFormat.readExternal(objectInput);
        numberFormat_.add(numFormat);
      }

      int intlNumberFormatSize = objectInput.readInt();
      for (int i = 0; i < intlNumberFormatSize; i++) {
        NumberFormat numFormat = new NumberFormat();
        numFormat.readExternal(objectInput);
        intlNumberFormat_.add(numFormat);
      }

      setMainCountryForCode(objectInput.readBoolean());

      hasString = objectInput.readBoolean();
      if (hasString) {
        setLeadingDigits(objectInput.readUTF());
      }
    }
  }

  public static final class PhoneMetadataCollection implements Externalizable {
    private static final long serialVersionUID = 1;
    public PhoneMetadataCollection() {}

    // repeated PhoneMetadata metadata = 1;
    private java.util.List<PhoneMetadata> metadata_ = new java.util.ArrayList<PhoneMetadata>();

    public java.util.List<PhoneMetadata> getMetadataList() {
      return metadata_;
    }
    public int getMetadataCount() { return metadata_.size(); }

    public PhoneMetadataCollection addMetadata(PhoneMetadata value) {
      if (value == null) {
        throw new NullPointerException();
      }
      metadata_.add(value);
      return this;
    }

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
      int size = getMetadataCount();
      objectOutput.writeInt(size);
      for (int i = 0; i < size; i++) {
        metadata_.get(i).writeExternal(objectOutput);
      }
    }

    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
      int size = objectInput.readInt();
      for (int i = 0; i < size; i++) {
        PhoneMetadata metadata = new PhoneMetadata();
        metadata.readExternal(objectInput);
        metadata_.add(metadata);
      }
    }
  }
}
