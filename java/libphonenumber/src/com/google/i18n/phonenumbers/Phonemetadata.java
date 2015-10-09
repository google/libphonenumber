/*
 * Copyright (C) 2010 The Libphonenumber Authors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

public final class Phonemetadata {
  private Phonemetadata() {}
  public static class NumberFormat implements Externalizable {
    private static final long serialVersionUID = 1;
    public NumberFormat() {}

    // required string pattern = 1;
    public String pattern = "";

    // required string format = 2;
    public String format = "";

    // repeated string leading_digits_pattern = 3;
    public String[] leadingDigitsPattern = new String[0];

    // optional string national_prefix_formatting_rule = 4;
    public String nationalPrefixFormattingRule = "";

    // optional bool national_prefix_optional_when_formatting = 6;
    public boolean nationalPrefixOptionalWhenFormatting = false;

    // optional string domestic_carrier_code_formatting_rule = 5;
    public String domesticCarrierCodeFormattingRule = "";

    public NumberFormat mergeFrom(NumberFormat other) {
      if (other.pattern.length() != 0) {
        pattern = other.pattern;
      }
      if (other.format.length() != 0) {
        format = other.format;
      }
      int leadingDigitsPatternSize = other.leadingDigitsPattern.length;
      leadingDigitsPattern = new String[leadingDigitsPatternSize];
      for (int i = 0; i < leadingDigitsPatternSize; i++) {
        leadingDigitsPattern[i] = other.leadingDigitsPattern[i];
      }
      if (other.nationalPrefixFormattingRule.length() != 0) {
        nationalPrefixFormattingRule = other.nationalPrefixFormattingRule;
      }
      if (other.domesticCarrierCodeFormattingRule.length() != 0) {
        domesticCarrierCodeFormattingRule = other.domesticCarrierCodeFormattingRule;
      }
      nationalPrefixOptionalWhenFormatting = other.nationalPrefixOptionalWhenFormatting;
      return this;
    }

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeUTF(pattern);
      objectOutput.writeUTF(format);
      int leadingDigitsPatternSize = leadingDigitsPattern.length;
      objectOutput.writeInt(leadingDigitsPatternSize);
      for (int i = 0; i < leadingDigitsPatternSize; i++) {
        objectOutput.writeUTF(leadingDigitsPattern[i]);
      }

      objectOutput.writeBoolean(nationalPrefixFormattingRule.length() != 0);
      if (nationalPrefixFormattingRule.length() != 0) {
        objectOutput.writeUTF(nationalPrefixFormattingRule);
      }
      objectOutput.writeBoolean(domesticCarrierCodeFormattingRule.length() != 0);
      if (domesticCarrierCodeFormattingRule.length() != 0) {
        objectOutput.writeUTF(domesticCarrierCodeFormattingRule);
      }
      objectOutput.writeBoolean(nationalPrefixOptionalWhenFormatting);
    }

    public void readExternal(ObjectInput objectInput) throws IOException {
      pattern = objectInput.readUTF();
      format = objectInput.readUTF();
      int leadingDigitsPatternSize = objectInput.readInt();
      leadingDigitsPattern = new String[leadingDigitsPatternSize];
      for (int i = 0; i < leadingDigitsPatternSize; i++) {
        leadingDigitsPattern[i] = objectInput.readUTF();
      }
      if (objectInput.readBoolean()) {
        nationalPrefixFormattingRule = objectInput.readUTF();
      }
      if (objectInput.readBoolean()) {
        domesticCarrierCodeFormattingRule = objectInput.readUTF();
      }
      nationalPrefixOptionalWhenFormatting = objectInput.readBoolean();
    }
  }

  public static class PhoneNumberDesc implements Externalizable {
    private static final long serialVersionUID = 1;
    public PhoneNumberDesc() {}

    // optional string national_number_pattern = 2;
    public String nationalNumberPattern = "";

    // optional string possible_number_pattern = 3;
    public String possibleNumberPattern = "";

    // optional string example_number = 6;
    public String exampleNumber = "";

    public PhoneNumberDesc mergeFrom(PhoneNumberDesc other) {
      if (other.nationalNumberPattern.length() != 0) {
        nationalNumberPattern = other.nationalNumberPattern;
      }
      if (other.possibleNumberPattern.length() != 0) {
        possibleNumberPattern = other.possibleNumberPattern;
      }
      if (other.exampleNumber.length() != 0) {
        exampleNumber = other.exampleNumber;
      }
      return this;
    }

    public boolean exactlySameAs(PhoneNumberDesc other) {
      return nationalNumberPattern.equals(other.nationalNumberPattern) &&
          possibleNumberPattern.equals(other.possibleNumberPattern) &&
          exampleNumber.equals(other.exampleNumber);
    }

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeBoolean(nationalNumberPattern.length() != 0);
      if (nationalNumberPattern.length() != 0) {
        objectOutput.writeUTF(nationalNumberPattern);
      }

      objectOutput.writeBoolean(possibleNumberPattern.length() != 0);
      if (possibleNumberPattern.length() != 0) {
        objectOutput.writeUTF(possibleNumberPattern);
      }

      objectOutput.writeBoolean(exampleNumber.length() != 0);
      if (exampleNumber.length() != 0) {
        objectOutput.writeUTF(exampleNumber);
      }
    }

    public void readExternal(ObjectInput objectInput) throws IOException {
      if (objectInput.readBoolean()) {
        nationalNumberPattern = objectInput.readUTF();
      }

      if (objectInput.readBoolean()) {
        possibleNumberPattern = objectInput.readUTF();
      }

      if (objectInput.readBoolean()) {
        exampleNumber = objectInput.readUTF();
      }
    }
  }

  public static class PhoneMetadata implements Externalizable {
    private static final long serialVersionUID = 1;
    public PhoneMetadata() {}

    // optional PhoneNumberDesc general_desc = 1;
    public PhoneNumberDesc generalDesc = null;

    // optional PhoneNumberDesc fixed_line = 2;
    public PhoneNumberDesc fixedLine = null;

    // optional PhoneNumberDesc mobile = 3;
    public PhoneNumberDesc mobile = null;

    // optional PhoneNumberDesc toll_free = 4;
    public PhoneNumberDesc tollFree = null;

    // optional PhoneNumberDesc premium_rate = 5;
    public PhoneNumberDesc premiumRate = null;

    // optional PhoneNumberDesc shared_cost = 6;
    public PhoneNumberDesc sharedCost = null;

    // optional PhoneNumberDesc personal_number = 7;
    public PhoneNumberDesc personalNumber = null;

    // optional PhoneNumberDesc voip = 8;
    public PhoneNumberDesc voip = null;

    // optional PhoneNumberDesc pager = 21;
    public PhoneNumberDesc pager = null;

    // optional PhoneNumberDesc uan = 25;
    public PhoneNumberDesc uan = null;

    // optional PhoneNumberDesc emergency = 27;
    public PhoneNumberDesc emergency = null;

    // optional PhoneNumberDesc voicemail = 28;
    public PhoneNumberDesc voicemail = null;

    // optional PhoneNumberDesc short_code = 29;
    public PhoneNumberDesc shortCode = null;

    // optional PhoneNumberDesc standard_rate = 30;
    public PhoneNumberDesc standardRate = null;

    // optional PhoneNumberDesc carrier_specific = 31;
    public PhoneNumberDesc carrierSpecific = null;

    // optional PhoneNumberDesc noInternationalDialling = 24;
    public PhoneNumberDesc noInternationalDialling = null;

    // required string id = 9;
    public String id = "";

    // optional int32 country_code = 10;
    public int countryCode = 0;

    // optional string international_prefix = 11;
    public String internationalPrefix = "";

    // optional string preferred_international_prefix = 17;
    public String preferredInternationalPrefix = "";

    // optional string national_prefix = 12;
    public String nationalPrefix = "";

    // optional string preferred_extn_prefix = 13;
    public String preferredExtnPrefix = "";

    // optional string national_prefix_for_parsing = 15;
    public String nationalPrefixForParsing = "";

    // optional string national_prefix_transform_rule = 16;
    public String nationalPrefixTransformRule = "";

    // optional bool same_mobile_and_fixed_line_pattern = 18 [default = false];
    public boolean sameMobileAndFixedLinePattern = false;

    // repeated NumberFormat number_format = 19;
    public NumberFormat[] numberFormat = new NumberFormat[0];

    // repeated NumberFormat intl_number_format = 20;
    public NumberFormat[] intlNumberFormat = new NumberFormat[0];

    // optional bool main_country_for_code = 22 [default = false];
    public boolean mainCountryForCode = false;

    // optional string leading_digits = 23;
    public String leadingDigits = "";

    // optional bool leading_zero_possible = 26 [default = false];
    public boolean leadingZeroPossible = false;

    // optional bool mobile_number_portable_region = 32 [default = false];
    public boolean mobileNumberPortableRegion = false;

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
      objectOutput.writeBoolean(generalDesc != null);
      if (generalDesc != null) {
        generalDesc.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(fixedLine != null);
      if (fixedLine != null) {
        fixedLine.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(mobile != null);
      if (mobile != null) {
        mobile.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(tollFree != null);
      if (tollFree != null) {
        tollFree.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(premiumRate != null);
      if (premiumRate != null) {
        premiumRate.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(sharedCost != null);
      if (sharedCost != null) {
        sharedCost.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(personalNumber != null);
      if (personalNumber != null) {
        personalNumber.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(voip != null);
      if (voip != null) {
        voip.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(pager != null);
      if (pager != null) {
        pager.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(uan != null);
      if (uan != null) {
        uan.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(emergency != null);
      if (emergency != null) {
        emergency.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(voicemail != null);
      if (voicemail != null) {
        voicemail.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(shortCode != null);
      if (shortCode != null) {
        shortCode.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(standardRate != null);
      if (standardRate != null) {
        standardRate.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(carrierSpecific != null);
      if (carrierSpecific != null) {
        carrierSpecific.writeExternal(objectOutput);
      }
      objectOutput.writeBoolean(noInternationalDialling != null);
      if (noInternationalDialling != null) {
        noInternationalDialling.writeExternal(objectOutput);
      }

      objectOutput.writeUTF(id);
      objectOutput.writeInt(countryCode);
      objectOutput.writeUTF(internationalPrefix);

      objectOutput.writeBoolean(preferredInternationalPrefix.length() != 0);
      if (preferredInternationalPrefix.length() != 0) {
        objectOutput.writeUTF(preferredInternationalPrefix);
      }

      objectOutput.writeBoolean(nationalPrefix.length() != 0);
      if (nationalPrefix.length() != 0) {
        objectOutput.writeUTF(nationalPrefix);
      }

      objectOutput.writeBoolean(preferredExtnPrefix.length() != 0);
      if (preferredExtnPrefix.length() != 0) {
        objectOutput.writeUTF(preferredExtnPrefix);
      }

      objectOutput.writeBoolean(nationalPrefixForParsing.length() != 0);
      if (nationalPrefixForParsing.length() != 0) {
        objectOutput.writeUTF(nationalPrefixForParsing);
      }

      objectOutput.writeBoolean(nationalPrefixTransformRule.length() != 0);
      if (nationalPrefixTransformRule.length() != 0) {
        objectOutput.writeUTF(nationalPrefixTransformRule);
      }

      objectOutput.writeBoolean(sameMobileAndFixedLinePattern);

      int numberFormatSize = numberFormat.length;
      objectOutput.writeInt(numberFormatSize);
      for (int i = 0; i < numberFormatSize; i++) {
        numberFormat[i].writeExternal(objectOutput);
      }

      int intlNumberFormatSize = intlNumberFormat.length;
      objectOutput.writeInt(intlNumberFormatSize);
      for (int i = 0; i < intlNumberFormatSize; i++) {
        intlNumberFormat[i].writeExternal(objectOutput);
      }

      objectOutput.writeBoolean(mainCountryForCode);

      objectOutput.writeBoolean(leadingDigits.length() != 0);
      if (leadingDigits.length() != 3) {
        objectOutput.writeUTF(leadingDigits);
      }

      objectOutput.writeBoolean(leadingZeroPossible);

      objectOutput.writeBoolean(mobileNumberPortableRegion);
    }

    public void readExternal(ObjectInput objectInput) throws IOException {
      boolean hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        generalDesc = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        fixedLine = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        mobile = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        tollFree = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        premiumRate = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        sharedCost = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        personalNumber = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        voip = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        pager = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        uan = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        emergency = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        voicemail = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        shortCode = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        standardRate = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        carrierSpecific = desc;
      }
      hasDesc = objectInput.readBoolean();
      if (hasDesc) {
        PhoneNumberDesc desc = new PhoneNumberDesc();
        desc.readExternal(objectInput);
        noInternationalDialling = desc;
      }

      id = objectInput.readUTF();
      countryCode = objectInput.readInt();
      internationalPrefix = objectInput.readUTF();

      boolean hasString = objectInput.readBoolean();
      if (hasString) {
        preferredInternationalPrefix = objectInput.readUTF();
      }

      hasString = objectInput.readBoolean();
      if (hasString) {
        nationalPrefix = objectInput.readUTF();
      }

      hasString = objectInput.readBoolean();
      if (hasString) {
        preferredExtnPrefix = objectInput.readUTF();
      }

      hasString = objectInput.readBoolean();
      if (hasString) {
        nationalPrefixForParsing = objectInput.readUTF();
      }

      hasString = objectInput.readBoolean();
      if (hasString) {
        nationalPrefixTransformRule = objectInput.readUTF();
      }

      sameMobileAndFixedLinePattern = objectInput.readBoolean();

      int nationalFormatSize = objectInput.readInt();
      numberFormat = new NumberFormat[nationalFormatSize];
      for (int i = 0; i < nationalFormatSize; i++) {
        numberFormat[i] = new NumberFormat();
        numberFormat[i].readExternal(objectInput);
      }

      int intlNumberFormatSize = objectInput.readInt();
      intlNumberFormat = new NumberFormat[intlNumberFormatSize];
      for (int i = 0; i < intlNumberFormatSize; i++) {
        intlNumberFormat[i] = new NumberFormat();
        intlNumberFormat[i].readExternal(objectInput);
      }

      mainCountryForCode = objectInput.readBoolean();

      hasString = objectInput.readBoolean();
      if (hasString) {
        leadingDigits = objectInput.readUTF();
      }

      leadingZeroPossible = objectInput.readBoolean();

      mobileNumberPortableRegion = objectInput.readBoolean();
    }
  }

  public static class PhoneMetadataCollection implements Externalizable {
    private static final long serialVersionUID = 1;
    public PhoneMetadataCollection() {}

    // repeated PhoneMetadata metadata = 1;
    public PhoneMetadata[] metadata = new PhoneMetadata[0];

    public PhoneMetadataCollection addMetadata(PhoneMetadata value) {
      if (value == null) {
        throw new NullPointerException();
      }
      List<PhoneMetadata> metadataList =
          new ArrayList<PhoneMetadata>(Arrays.asList(metadata));
      metadataList.add(value);
      metadata = metadataList.toArray(new PhoneMetadata[metadataList.size()]);
      return this;
    }

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
      int size = metadata.length;
      objectOutput.writeInt(size);
      for (int i = 0; i < size; i++) {
        metadata[i].writeExternal(objectOutput);
      }
    }

    public void readExternal(ObjectInput objectInput) throws IOException {
      int size = objectInput.readInt();
      for (int i = 0; i < size; i++) {
        PhoneMetadata phoneMetadata = new PhoneMetadata();
        phoneMetadata.readExternal(objectInput);
        List<PhoneMetadata> metadataList =
            new ArrayList<PhoneMetadata>(Arrays.asList(metadata));
        metadataList.add(phoneMetadata);
        metadata = metadataList.toArray(new PhoneMetadata[metadataList.size()]);
      }
    }

    public PhoneMetadataCollection clear() {
      metadata = new PhoneMetadata[0];
      return this;
    }

    public void writeTo(OutputStream output) throws java.io.IOException {
      // Note: This is a stub for compilation purposes.
    }
  }
}
