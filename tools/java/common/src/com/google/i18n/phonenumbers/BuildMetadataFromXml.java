/*
 * Copyright (C) 2009 The Libphonenumber Authors
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

import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Library to build phone number metadata from the XML format.
 *
 * @author Shaopeng Jia
 */
public class BuildMetadataFromXml {
  private static final Logger LOGGER = Logger.getLogger(BuildMetadataFromXml.class.getName());

  // String constants used to fetch the XML nodes and attributes.
  private static final String CARRIER_CODE_FORMATTING_RULE = "carrierCodeFormattingRule";
  private static final String COUNTRY_CODE = "countryCode";
  private static final String EMERGENCY = "emergency";
  private static final String EXAMPLE_NUMBER = "exampleNumber";
  private static final String FIXED_LINE = "fixedLine";
  private static final String FORMAT = "format";
  private static final String GENERAL_DESC = "generalDesc";
  private static final String INTERNATIONAL_PREFIX = "internationalPrefix";
  private static final String INTL_FORMAT = "intlFormat";
  private static final String LEADING_DIGITS = "leadingDigits";
  private static final String LEADING_ZERO_POSSIBLE = "leadingZeroPossible";
  private static final String MAIN_COUNTRY_FOR_CODE = "mainCountryForCode";
  private static final String MOBILE = "mobile";
  private static final String NATIONAL_NUMBER_PATTERN = "nationalNumberPattern";
  private static final String NATIONAL_PREFIX = "nationalPrefix";
  private static final String NATIONAL_PREFIX_FORMATTING_RULE = "nationalPrefixFormattingRule";
  private static final String NATIONAL_PREFIX_OPTIONAL_WHEN_FORMATTING =
      "nationalPrefixOptionalWhenFormatting";
  private static final String NATIONAL_PREFIX_FOR_PARSING = "nationalPrefixForParsing";
  private static final String NATIONAL_PREFIX_TRANSFORM_RULE = "nationalPrefixTransformRule";
  private static final String NO_INTERNATIONAL_DIALLING = "noInternationalDialling";
  private static final String NUMBER_FORMAT = "numberFormat";
  private static final String PAGER = "pager";
  private static final String PATTERN = "pattern";
  private static final String PERSONAL_NUMBER = "personalNumber";
  private static final String POSSIBLE_NUMBER_PATTERN = "possibleNumberPattern";
  private static final String PREFERRED_EXTN_PREFIX = "preferredExtnPrefix";
  private static final String PREFERRED_INTERNATIONAL_PREFIX = "preferredInternationalPrefix";
  private static final String PREMIUM_RATE = "premiumRate";
  private static final String SHARED_COST = "sharedCost";
  private static final String TOLL_FREE = "tollFree";
  private static final String UAN = "uan";
  private static final String VOICEMAIL = "voicemail";
  private static final String VOIP = "voip";

  // Build the PhoneMetadataCollection from the input XML file.
  public static PhoneMetadataCollection buildPhoneMetadataCollection(String inputXmlFile,
      boolean liteBuild) throws Exception {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    File xmlFile = new File(inputXmlFile);
    Document document = builder.parse(xmlFile);
    document.getDocumentElement().normalize();
    Element rootElement = document.getDocumentElement();
    NodeList territory = rootElement.getElementsByTagName("territory");
    PhoneMetadataCollection.Builder metadataCollection = PhoneMetadataCollection.newBuilder();
    int numOfTerritories = territory.getLength();
    for (int i = 0; i < numOfTerritories; i++) {
      Element territoryElement = (Element) territory.item(i);
      String regionCode = "";
      // For the main metadata file this should always be set, but for other supplementary data
      // files the country calling code may be all that is needed.
      if (territoryElement.hasAttribute("id")) {
        regionCode = territoryElement.getAttribute("id");
      }
      PhoneMetadata metadata = loadCountryMetadata(regionCode, territoryElement, liteBuild);
      metadataCollection.addMetadata(metadata);
    }
    return metadataCollection.build();
  }

  // Build a mapping from a country calling code to the region codes which denote the country/region
  // represented by that country code. In the case of multiple countries sharing a calling code,
  // such as the NANPA countries, the one indicated with "isMainCountryForCode" in the metadata
  // should be first.
  public static Map<Integer, List<String>> buildCountryCodeToRegionCodeMap(
      PhoneMetadataCollection metadataCollection) {
    Map<Integer, List<String>> countryCodeToRegionCodeMap =
        new TreeMap<Integer, List<String>>();
    for (PhoneMetadata metadata : metadataCollection.getMetadataList()) {
      String regionCode = metadata.getId();
      int countryCode = metadata.getCountryCode();
      if (countryCodeToRegionCodeMap.containsKey(countryCode)) {
        if (metadata.getMainCountryForCode()) {
          countryCodeToRegionCodeMap.get(countryCode).add(0, regionCode);
        } else {
          countryCodeToRegionCodeMap.get(countryCode).add(regionCode);
        }
      } else {
        // For most countries, there will be only one region code for the country calling code.
        List<String> listWithRegionCode = new ArrayList<String>(1);
        if (!regionCode.isEmpty()) {  // For alternate formats, there are no region codes at all.
          listWithRegionCode.add(regionCode);
        }
        countryCodeToRegionCodeMap.put(countryCode, listWithRegionCode);
      }
    }
    return countryCodeToRegionCodeMap;
  }

  private static String validateRE(String regex) {
    return validateRE(regex, false);
  }

  // @VisibleForTesting
  static String validateRE(String regex, boolean removeWhitespace) {
    // Removes all the whitespace and newline from the regexp. Not using pattern compile options to
    // make it work across programming languages.
    if (removeWhitespace) {
      regex = regex.replaceAll("\\s", "");
    }
    Pattern.compile(regex);
    // return regex itself if it is of correct regex syntax
    // i.e. compile did not fail with a PatternSyntaxException.
    return regex;
  }

  /**
   * Returns the national prefix of the provided country element.
   */
  // @VisibleForTesting
  static String getNationalPrefix(Element element) {
    return element.hasAttribute(NATIONAL_PREFIX) ? element.getAttribute(NATIONAL_PREFIX) : "";
  }

  // @VisibleForTesting
  static PhoneMetadata.Builder loadTerritoryTagMetadata(String regionCode, Element element,
                                                        String nationalPrefix) {
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    metadata.setId(regionCode);
    metadata.setCountryCode(Integer.parseInt(element.getAttribute(COUNTRY_CODE)));
    if (element.hasAttribute(LEADING_DIGITS)) {
      metadata.setLeadingDigits(validateRE(element.getAttribute(LEADING_DIGITS)));
    }
    metadata.setInternationalPrefix(validateRE(element.getAttribute(INTERNATIONAL_PREFIX)));
    if (element.hasAttribute(PREFERRED_INTERNATIONAL_PREFIX)) {
      String preferredInternationalPrefix = element.getAttribute(PREFERRED_INTERNATIONAL_PREFIX);
      metadata.setPreferredInternationalPrefix(preferredInternationalPrefix);
    }
    if (element.hasAttribute(NATIONAL_PREFIX_FOR_PARSING)) {
      metadata.setNationalPrefixForParsing(
          validateRE(element.getAttribute(NATIONAL_PREFIX_FOR_PARSING), true));
      if (element.hasAttribute(NATIONAL_PREFIX_TRANSFORM_RULE)) {
        metadata.setNationalPrefixTransformRule(
            validateRE(element.getAttribute(NATIONAL_PREFIX_TRANSFORM_RULE)));
      }
    }
    if (!nationalPrefix.isEmpty()) {
      metadata.setNationalPrefix(nationalPrefix);
      if (!metadata.hasNationalPrefixForParsing()) {
        metadata.setNationalPrefixForParsing(nationalPrefix);
      }
    }
    if (element.hasAttribute(PREFERRED_EXTN_PREFIX)) {
      metadata.setPreferredExtnPrefix(element.getAttribute(PREFERRED_EXTN_PREFIX));
    }
    if (element.hasAttribute(MAIN_COUNTRY_FOR_CODE)) {
      metadata.setMainCountryForCode(true);
    }
    if (element.hasAttribute(LEADING_ZERO_POSSIBLE)) {
      metadata.setLeadingZeroPossible(true);
    }
    return metadata;
  }

  /**
   * Extracts the pattern for international format. If there is no intlFormat, default to using the
   * national format. If the intlFormat is set to "NA" the intlFormat should be ignored.
   *
   * @throws  RuntimeException if multiple intlFormats have been encountered.
   * @return  whether an international number format is defined.
   */
  // @VisibleForTesting
  static boolean loadInternationalFormat(PhoneMetadata.Builder metadata,
                                         Element numberFormatElement,
                                         String nationalFormat) {
    NumberFormat.Builder intlFormat = NumberFormat.newBuilder();
    setLeadingDigitsPatterns(numberFormatElement, intlFormat);
    intlFormat.setPattern(numberFormatElement.getAttribute(PATTERN));
    NodeList intlFormatPattern = numberFormatElement.getElementsByTagName(INTL_FORMAT);
    boolean hasExplicitIntlFormatDefined = false;

    if (intlFormatPattern.getLength() > 1) {
      LOGGER.log(Level.SEVERE,
                 "A maximum of one intlFormat pattern for a numberFormat element should be " +
                 "defined.");
      throw new RuntimeException("Invalid number of intlFormat patterns for country: " +
                                 metadata.getId());
    } else if (intlFormatPattern.getLength() == 0) {
      // Default to use the same as the national pattern if none is defined.
      intlFormat.setFormat(nationalFormat);
    } else {
      String intlFormatPatternValue = intlFormatPattern.item(0).getFirstChild().getNodeValue();
      if (!intlFormatPatternValue.equals("NA")) {
        intlFormat.setFormat(intlFormatPatternValue);
      }
      hasExplicitIntlFormatDefined = true;
    }

    if (intlFormat.hasFormat()) {
      metadata.addIntlNumberFormat(intlFormat);
    }
    return hasExplicitIntlFormatDefined;
  }

  /**
   * Extracts the pattern for the national format.
   *
   * @throws  RuntimeException if multiple or no formats have been encountered.
   * @return  the national format string.
   */
  // @VisibleForTesting
  static String loadNationalFormat(PhoneMetadata.Builder metadata, Element numberFormatElement,
                                   NumberFormat.Builder format) {
    setLeadingDigitsPatterns(numberFormatElement, format);
    format.setPattern(validateRE(numberFormatElement.getAttribute(PATTERN)));

    NodeList formatPattern = numberFormatElement.getElementsByTagName(FORMAT);
    if (formatPattern.getLength() != 1) {
      LOGGER.log(Level.SEVERE,
                 "Only one format pattern for a numberFormat element should be defined.");
      throw new RuntimeException("Invalid number of format patterns for country: " +
                                   metadata.getId());
    }
    String nationalFormat = formatPattern.item(0).getFirstChild().getNodeValue();
    format.setFormat(nationalFormat);
    return nationalFormat;
  }

  /**
   *  Extracts the available formats from the provided DOM element. If it does not contain any
   *  nationalPrefixFormattingRule, the one passed-in is retained. The nationalPrefix,
   *  nationalPrefixFormattingRule and nationalPrefixOptionalWhenFormatting values are provided from
   *  the parent (territory) element.
   */
  // @VisibleForTesting
  static void loadAvailableFormats(PhoneMetadata.Builder metadata,
                                   Element element, String nationalPrefix,
                                   String nationalPrefixFormattingRule,
                                   boolean nationalPrefixOptionalWhenFormatting) {
    String carrierCodeFormattingRule = "";
    if (element.hasAttribute(CARRIER_CODE_FORMATTING_RULE)) {
      carrierCodeFormattingRule = validateRE(
          getDomesticCarrierCodeFormattingRuleFromElement(element, nationalPrefix));
    }
    NodeList numberFormatElements = element.getElementsByTagName(NUMBER_FORMAT);
    boolean hasExplicitIntlFormatDefined = false;

    int numOfFormatElements = numberFormatElements.getLength();
    if (numOfFormatElements > 0) {
      for (int i = 0; i < numOfFormatElements; i++) {
        Element numberFormatElement = (Element) numberFormatElements.item(i);
        NumberFormat.Builder format = NumberFormat.newBuilder();

        if (numberFormatElement.hasAttribute(NATIONAL_PREFIX_FORMATTING_RULE)) {
          format.setNationalPrefixFormattingRule(
              getNationalPrefixFormattingRuleFromElement(numberFormatElement, nationalPrefix));
          format.setNationalPrefixOptionalWhenFormatting(
              numberFormatElement.hasAttribute(NATIONAL_PREFIX_OPTIONAL_WHEN_FORMATTING));
        } else {
          format.setNationalPrefixFormattingRule(nationalPrefixFormattingRule);
          format.setNationalPrefixOptionalWhenFormatting(nationalPrefixOptionalWhenFormatting);
        }
        if (numberFormatElement.hasAttribute(CARRIER_CODE_FORMATTING_RULE)) {
          format.setDomesticCarrierCodeFormattingRule(validateRE(
              getDomesticCarrierCodeFormattingRuleFromElement(numberFormatElement,
                                                              nationalPrefix)));
        } else {
          format.setDomesticCarrierCodeFormattingRule(carrierCodeFormattingRule);
        }
        String nationalFormat =
            loadNationalFormat(metadata, numberFormatElement, format);
        metadata.addNumberFormat(format);

        if (loadInternationalFormat(metadata, numberFormatElement, nationalFormat)) {
          hasExplicitIntlFormatDefined = true;
        }
      }
      // Only a small number of regions need to specify the intlFormats in the xml. For the majority
      // of countries the intlNumberFormat metadata is an exact copy of the national NumberFormat
      // metadata. To minimize the size of the metadata file, we only keep intlNumberFormats that
      // actually differ in some way to the national formats.
      if (!hasExplicitIntlFormatDefined) {
        metadata.clearIntlNumberFormat();
      }
    }
  }

  // @VisibleForTesting
  static void setLeadingDigitsPatterns(Element numberFormatElement, NumberFormat.Builder format) {
    NodeList leadingDigitsPatternNodes = numberFormatElement.getElementsByTagName(LEADING_DIGITS);
    int numOfLeadingDigitsPatterns = leadingDigitsPatternNodes.getLength();
    if (numOfLeadingDigitsPatterns > 0) {
      for (int i = 0; i < numOfLeadingDigitsPatterns; i++) {
        format.addLeadingDigitsPattern(
            validateRE((leadingDigitsPatternNodes.item(i)).getFirstChild().getNodeValue(), true));
      }
    }
  }

  // @VisibleForTesting
  static String getNationalPrefixFormattingRuleFromElement(Element element,
                                                           String nationalPrefix) {
    String nationalPrefixFormattingRule = element.getAttribute(NATIONAL_PREFIX_FORMATTING_RULE);
    // Replace $NP with national prefix and $FG with the first group ($1).
    nationalPrefixFormattingRule =
        nationalPrefixFormattingRule.replaceFirst("\\$NP", nationalPrefix)
            .replaceFirst("\\$FG", "\\$1");
    return nationalPrefixFormattingRule;
  }

  // @VisibleForTesting
  static String getDomesticCarrierCodeFormattingRuleFromElement(Element element,
                                                                String nationalPrefix) {
    String carrierCodeFormattingRule = element.getAttribute(CARRIER_CODE_FORMATTING_RULE);
    // Replace $FG with the first group ($1) and $NP with the national prefix.
    carrierCodeFormattingRule = carrierCodeFormattingRule.replaceFirst("\\$FG", "\\$1")
        .replaceFirst("\\$NP", nationalPrefix);
    return carrierCodeFormattingRule;
  }

  // @VisibleForTesting
  static boolean isValidNumberType(String numberType) {
    return numberType.equals(FIXED_LINE) || numberType.equals(MOBILE) ||
         numberType.equals(GENERAL_DESC);
  }

  /**
   * Processes a phone number description element from the XML file and returns it as a
   * PhoneNumberDesc. If the description element is a fixed line or mobile number, the general
   * description will be used to fill in the whole element if necessary, or any components that are
   * missing. For all other types, the general description will only be used to fill in missing
   * components if the type has a partial definition. For example, if no "tollFree" element exists,
   * we assume there are no toll free numbers for that locale, and return a phone number description
   * with "NA" for both the national and possible number patterns.
   *
   * @param generalDesc  a generic phone number description that will be used to fill in missing
   *                     parts of the description
   * @param countryElement  the XML element representing all the country information
   * @param numberType  the name of the number type, corresponding to the appropriate tag in the XML
   *                    file with information about that type
   * @return  complete description of that phone number type
   */
  // @VisibleForTesting
  static PhoneNumberDesc.Builder processPhoneNumberDescElement(PhoneNumberDesc.Builder generalDesc,
                                                               Element countryElement,
                                                               String numberType,
                                                               boolean liteBuild) {
    NodeList phoneNumberDescList = countryElement.getElementsByTagName(numberType);
    PhoneNumberDesc.Builder numberDesc = PhoneNumberDesc.newBuilder();
    if (phoneNumberDescList.getLength() == 0 && !isValidNumberType(numberType)) {
      numberDesc.setNationalNumberPattern("NA");
      numberDesc.setPossibleNumberPattern("NA");
      return numberDesc;
    }
    numberDesc.mergeFrom(generalDesc.build());
    if (phoneNumberDescList.getLength() > 0) {
      Element element = (Element) phoneNumberDescList.item(0);
      NodeList possiblePattern = element.getElementsByTagName(POSSIBLE_NUMBER_PATTERN);
      if (possiblePattern.getLength() > 0) {
        numberDesc.setPossibleNumberPattern(
            validateRE(possiblePattern.item(0).getFirstChild().getNodeValue(), true));
      }

      NodeList validPattern = element.getElementsByTagName(NATIONAL_NUMBER_PATTERN);
      if (validPattern.getLength() > 0) {
        numberDesc.setNationalNumberPattern(
            validateRE(validPattern.item(0).getFirstChild().getNodeValue(), true));
      }

      if (!liteBuild) {
        NodeList exampleNumber = element.getElementsByTagName(EXAMPLE_NUMBER);
        if (exampleNumber.getLength() > 0) {
          numberDesc.setExampleNumber(exampleNumber.item(0).getFirstChild().getNodeValue());
        }
      }
    }
    return numberDesc;
  }

  // @VisibleForTesting
  static void loadGeneralDesc(PhoneMetadata.Builder metadata, Element element, boolean liteBuild) {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc = processPhoneNumberDescElement(generalDesc, element, GENERAL_DESC, liteBuild);
    metadata.setGeneralDesc(generalDesc);

    metadata.setFixedLine(
        processPhoneNumberDescElement(generalDesc, element, FIXED_LINE, liteBuild));
    metadata.setMobile(
        processPhoneNumberDescElement(generalDesc, element, MOBILE, liteBuild));
    metadata.setTollFree(
        processPhoneNumberDescElement(generalDesc, element, TOLL_FREE, liteBuild));
    metadata.setPremiumRate(
        processPhoneNumberDescElement(generalDesc, element, PREMIUM_RATE, liteBuild));
    metadata.setSharedCost(
        processPhoneNumberDescElement(generalDesc, element, SHARED_COST, liteBuild));
    metadata.setVoip(
        processPhoneNumberDescElement(generalDesc, element, VOIP, liteBuild));
    metadata.setPersonalNumber(
        processPhoneNumberDescElement(generalDesc, element, PERSONAL_NUMBER, liteBuild));
    metadata.setPager(
        processPhoneNumberDescElement(generalDesc, element, PAGER, liteBuild));
    metadata.setUan(
        processPhoneNumberDescElement(generalDesc, element, UAN, liteBuild));
    metadata.setVoicemail(
        processPhoneNumberDescElement(generalDesc, element, VOICEMAIL, liteBuild));
    metadata.setEmergency(
        processPhoneNumberDescElement(generalDesc, element, EMERGENCY, liteBuild));
    metadata.setNoInternationalDialling(
        processPhoneNumberDescElement(generalDesc, element, NO_INTERNATIONAL_DIALLING, liteBuild));
    metadata.setSameMobileAndFixedLinePattern(
        metadata.getMobile().getNationalNumberPattern().equals(
        metadata.getFixedLine().getNationalNumberPattern()));
  }

  // @VisibleForTesting
  static PhoneMetadata loadCountryMetadata(String regionCode, Element element, boolean liteBuild) {
    String nationalPrefix = getNationalPrefix(element);
    PhoneMetadata.Builder metadata =
        loadTerritoryTagMetadata(regionCode, element, nationalPrefix);
    String nationalPrefixFormattingRule =
        getNationalPrefixFormattingRuleFromElement(element, nationalPrefix);
    loadAvailableFormats(metadata, element, nationalPrefix.toString(),
                         nationalPrefixFormattingRule.toString(),
                         element.hasAttribute(NATIONAL_PREFIX_OPTIONAL_WHEN_FORMATTING));
    loadGeneralDesc(metadata, element, liteBuild);
    return metadata.build();
  }
}
