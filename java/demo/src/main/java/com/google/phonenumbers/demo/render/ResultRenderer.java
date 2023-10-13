/*
 * Copyright (C) 2022 The Libphonenumber Authors
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
 *
 * @author Tobias Rogg
 */

package com.google.phonenumbers.demo.render;

import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.AsYouTypeFormatter;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.ShortNumberInfo;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.google.phonenumbers.demo.helper.WebHelper;
import com.google.phonenumbers.demo.template.ResultTemplates.SingleNumber;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResultRenderer extends LibPhoneNumberRenderer<SingleNumber> {
  private final String phoneNumber;
  private final String defaultCountry;
  private final Locale geocodingLocale;
  private final PhoneNumber number;
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
  private final ShortNumberInfo shortInfo = ShortNumberInfo.getInstance();

  public ResultRenderer(
      String phoneNumber, String defaultCountry, Locale geocodingLocale, PhoneNumber number) {
    this.phoneNumber = phoneNumber;
    this.defaultCountry = defaultCountry;
    this.geocodingLocale = geocodingLocale;
    this.number = number;
  }

  @Override
  public String genHtml() {
    // Header info at Start of Page
    SingleNumber.Builder soyTemplate =
        SingleNumber.builder()
            .setPhoneNumber(phoneNumber)
            .setDefaultCountry(defaultCountry)
            .setGeocodingLocale(geocodingLocale.toLanguageTag());

    soyTemplate
        .setCountryCode(number.getCountryCode())
        .setNationalNumber(number.getNationalNumber())
        .setExtension(number.getExtension())
        .setCountryCodeSource(number.getCountryCodeSource().toString())
        .setItalianLeadingZero(number.isItalianLeadingZero())
        .setNumberOfLeadingZeros(number.getNumberOfLeadingZeros())
        .setRawInput(number.getRawInput())
        .setPreferredDomesticCarrierCode(number.getPreferredDomesticCarrierCode());

    boolean isNumberValid = phoneUtil.isValidNumber(number);
    boolean hasDefaultCountry = !defaultCountry.isEmpty() && !defaultCountry.equals("ZZ");

    // Validation Results Table
    soyTemplate
        .setIsPossibleNumber(phoneUtil.isPossibleNumber(number))
        .setIsValidNumber(isNumberValid)
        .setIsValidNumberForRegion(
            isNumberValid && hasDefaultCountry
                ? phoneUtil.isValidNumberForRegion(number, defaultCountry)
                : null)
        .setPhoneNumberRegion(phoneUtil.getRegionCodeForNumber(number))
        .setNumberType(phoneUtil.getNumberType(number).toString())
        .setValidationResult(phoneUtil.isPossibleNumberWithReason(number).toString());

    // Short Number Results Table
    soyTemplate
        .setIsPossibleShortNumber(shortInfo.isPossibleShortNumber(number))
        .setIsValidShortNumber(shortInfo.isValidShortNumber(number))
        .setIsPossibleShortNumberForRegion(
            hasDefaultCountry
                ? shortInfo.isPossibleShortNumberForRegion(number, defaultCountry)
                : null)
        .setIsValidShortNumberForRegion(
            hasDefaultCountry
                ? shortInfo.isValidShortNumberForRegion(number, defaultCountry)
                : null);

    // Formatting Results Table
    soyTemplate
        .setE164Format(isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.E164) : "invalid")
        .setOriginalFormat(phoneUtil.formatInOriginalFormat(number, defaultCountry))
        .setNationalFormat(phoneUtil.format(number, PhoneNumberFormat.NATIONAL))
        .setInternationalFormat(
            isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.INTERNATIONAL) : "invalid")
        .setOutOfCountryFormatFromUs(
            isNumberValid ? phoneUtil.formatOutOfCountryCallingNumber(number, "US") : "invalid")
        .setOutOfCountryFormatFromCh(
            isNumberValid ? phoneUtil.formatOutOfCountryCallingNumber(number, "CH") : "invalid")
        .setMobileDiallingFormatFromUs(
            isNumberValid ? phoneUtil.formatNumberForMobileDialing(number, "US", true) : "invalid")
        .setNationalDiallingFormatWithPreferredCarrierCode(
            isNumberValid ? phoneUtil.formatNationalNumberWithCarrierCode(number, "") : "invalid");

    // Get As You Type Formatter Table
    List<List<String>> rows = new ArrayList<>();
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter(defaultCountry);
    int rawNumberLength = phoneNumber.length();
    for (int i = 0; i < rawNumberLength; i++) {
      // Note this doesn't handle supplementary characters, but it shouldn't be a big deal as
      // there are no dial-pad characters in the supplementary range.
      char inputChar = phoneNumber.charAt(i);
      rows.add(ImmutableList.of(String.valueOf(inputChar), formatter.inputDigit(inputChar)));
    }
    soyTemplate.setRows(rows);

    // Geo Info Tables
    String guidelinesLink = "https://github.com/google/libphonenumber/blob/master/CONTRIBUTING.md";
    soyTemplate
        .setDescriptionForNumber(
            PhoneNumberOfflineGeocoder.getInstance()
                .getDescriptionForNumber(number, geocodingLocale))
        .setTimeZonesForNumber(
            PhoneNumberToTimeZonesMapper.getInstance().getTimeZonesForNumber(number).toString())
        .setNameForNumber(
            PhoneNumberToCarrierMapper.getInstance().getNameForNumber(number, geocodingLocale))
        .setNewIssueLink(WebHelper.getNewIssueLink(phoneNumber, defaultCountry, geocodingLocale))
        .setGuidelinesLink(guidelinesLink);

    return super.render(soyTemplate.build());
  }
}
