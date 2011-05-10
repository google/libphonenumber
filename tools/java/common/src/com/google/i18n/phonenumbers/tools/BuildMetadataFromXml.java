/*
 * Copyright (C) 2009 Google Inc.
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

package com.google.i18n.phonenumbers.tools;

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
  private static Boolean liteBuild;

  // Build the PhoneMetadataCollection from the input XML file.
  public static PhoneMetadataCollection buildPhoneMetadataCollection(String inputXmlFile,
      boolean liteBuild) throws Exception {
    BuildMetadataFromXml.liteBuild = liteBuild;
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
      String regionCode = territoryElement.getAttribute("id");
      PhoneMetadata metadata = loadCountryMetadata(regionCode, territoryElement);
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
        listWithRegionCode.add(regionCode);
        countryCodeToRegionCodeMap.put(countryCode, listWithRegionCode);
      }
    }
    return countryCodeToRegionCodeMap;
  }

  private static String validateRE(String regex) {
    return validateRE(regex, false);
  }

  private static String validateRE(String regex, boolean removeWhitespace) {
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

  private static PhoneMetadata loadCountryMetadata(String regionCode, Element element) {
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    metadata.setId(regionCode);
    metadata.setCountryCode(Integer.parseInt(element.getAttribute("countryCode")));
    if (element.hasAttribute("leadingDigits")) {
      metadata.setLeadingDigits(validateRE(element.getAttribute("leadingDigits")));
    }
    metadata.setInternationalPrefix(validateRE(element.getAttribute("internationalPrefix")));
    if (element.hasAttribute("preferredInternationalPrefix")) {
      String preferredInternationalPrefix = element.getAttribute("preferredInternationalPrefix");
      metadata.setPreferredInternationalPrefix(preferredInternationalPrefix);
    }
    if (element.hasAttribute("nationalPrefixForParsing")) {
      metadata.setNationalPrefixForParsing(
          validateRE(element.getAttribute("nationalPrefixForParsing")));
      if (element.hasAttribute("nationalPrefixTransformRule")) {
        metadata.setNationalPrefixTransformRule(
            validateRE(element.getAttribute("nationalPrefixTransformRule")));
      }
    }
    String nationalPrefix = "";
    String nationalPrefixFormattingRule = "";
    if (element.hasAttribute("nationalPrefix")) {
      nationalPrefix = element.getAttribute("nationalPrefix");
      metadata.setNationalPrefix(nationalPrefix);
      nationalPrefixFormattingRule =
          getNationalPrefixFormattingRuleFromElement(element, nationalPrefix);

      if (!metadata.hasNationalPrefixForParsing()) {
        metadata.setNationalPrefixForParsing(nationalPrefix);
      }
    }
    String carrierCodeFormattingRule = "";
    if (element.hasAttribute("carrierCodeFormattingRule")) {
      carrierCodeFormattingRule = validateRE(
          getDomesticCarrierCodeFormattingRuleFromElement(element, nationalPrefix));
    }
    if (element.hasAttribute("preferredExtnPrefix")) {
      metadata.setPreferredExtnPrefix(element.getAttribute("preferredExtnPrefix"));
    }
    if (element.hasAttribute("mainCountryForCode")) {
      metadata.setMainCountryForCode(true);
    }
    if (element.hasAttribute("leadingZeroPossible")) {
      metadata.setLeadingZeroPossible(true);
    }

    // Extract availableFormats
    NodeList numberFormatElements = element.getElementsByTagName("numberFormat");
    boolean hasExplicitIntlFormatDefined = false;

    int numOfFormatElements = numberFormatElements.getLength();
    if (numOfFormatElements > 0) {
      for (int i = 0; i < numOfFormatElements; i++) {
        Element numberFormatElement = (Element) numberFormatElements.item(i);
        NumberFormat.Builder format = NumberFormat.newBuilder();

        if (numberFormatElement.hasAttribute("nationalPrefixFormattingRule")) {
          format.setNationalPrefixFormattingRule(
              getNationalPrefixFormattingRuleFromElement(numberFormatElement, nationalPrefix));
        } else {
          format.setNationalPrefixFormattingRule(nationalPrefixFormattingRule);
        }
        if (numberFormatElement.hasAttribute("carrierCodeFormattingRule")) {
          format.setDomesticCarrierCodeFormattingRule(validateRE(
              getDomesticCarrierCodeFormattingRuleFromElement(numberFormatElement,
                                                              nationalPrefix)));
        } else {
          format.setDomesticCarrierCodeFormattingRule(carrierCodeFormattingRule);
        }

        // Extract the pattern for the national format.
        setLeadingDigitsPatterns(numberFormatElement, format);
        format.setPattern(validateRE(numberFormatElement.getAttribute("pattern")));

        NodeList formatPattern = numberFormatElement.getElementsByTagName("format");
        if (formatPattern.getLength() != 1) {
          LOGGER.log(Level.SEVERE,
                     "Only one format pattern for a numberFormat element should be defined.");
          throw new RuntimeException("Invalid number of format patterns for country: " +
                                     regionCode);
        }
        String nationalFormat = formatPattern.item(0).getFirstChild().getNodeValue();
        format.setFormat(nationalFormat);
        metadata.addNumberFormat(format);

        // Extract the pattern for international format. If there is no intlFormat, default to
        // using the national format. If the intlFormat is set to "NA" the intlFormat should be
        // ignored.
        NumberFormat.Builder intlFormat = NumberFormat.newBuilder();
        setLeadingDigitsPatterns(numberFormatElement, intlFormat);
        intlFormat.setPattern(numberFormatElement.getAttribute("pattern"));
        NodeList intlFormatPattern = numberFormatElement.getElementsByTagName("intlFormat");

        if (intlFormatPattern.getLength() > 1) {
          LOGGER.log(Level.SEVERE,
                     "A maximum of one intlFormat pattern for a numberFormat element should be " +
                     "defined.");
          throw new RuntimeException("Invalid number of intlFormat patterns for country: " +
                                     regionCode);
        } else if (intlFormatPattern.getLength() == 0) {
          // Default to use the same as the national pattern if none is defined.
          intlFormat.setFormat(nationalFormat);
        } else {
           String intlFormatPatternValue =
               intlFormatPattern.item(0).getFirstChild().getNodeValue();
           if (!intlFormatPatternValue.equals("NA")) {
             intlFormat.setFormat(intlFormatPatternValue);
           }
           hasExplicitIntlFormatDefined = true;
        }

        if (intlFormat.hasFormat()) {
          metadata.addIntlNumberFormat(intlFormat);
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

    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc = processPhoneNumberDescElement(generalDesc, element, "generalDesc");
    metadata.setGeneralDesc(generalDesc);
    metadata.setFixedLine(processPhoneNumberDescElement(generalDesc, element, "fixedLine"));
    metadata.setMobile(processPhoneNumberDescElement(generalDesc, element, "mobile"));
    metadata.setTollFree(processPhoneNumberDescElement(generalDesc, element, "tollFree"));
    metadata.setPremiumRate(processPhoneNumberDescElement(generalDesc, element, "premiumRate"));
    metadata.setSharedCost(processPhoneNumberDescElement(generalDesc, element, "sharedCost"));
    metadata.setVoip(processPhoneNumberDescElement(generalDesc, element, "voip"));
    metadata.setPersonalNumber(processPhoneNumberDescElement(generalDesc, element,
                                                             "personalNumber"));
    metadata.setPager(processPhoneNumberDescElement(generalDesc, element, "pager"));
    metadata.setUan(processPhoneNumberDescElement(generalDesc, element, "uan"));
    metadata.setNoInternationalDialling(processPhoneNumberDescElement(generalDesc, element,
                                                                      "noInternationalDialling"));

    if (metadata.getMobile().getNationalNumberPattern().equals(
        metadata.getFixedLine().getNationalNumberPattern())) {
      metadata.setSameMobileAndFixedLinePattern(true);
    }
    return metadata.build();
  }

  private static void setLeadingDigitsPatterns(Element numberFormatElement,
                                               NumberFormat.Builder format) {
    NodeList leadingDigitsPatternNodes = numberFormatElement.getElementsByTagName("leadingDigits");
    int numOfLeadingDigitsPatterns = leadingDigitsPatternNodes.getLength();
    if (numOfLeadingDigitsPatterns > 0) {
      for (int i = 0; i < numOfLeadingDigitsPatterns; i++) {
        format.addLeadingDigitsPattern(
            validateRE((leadingDigitsPatternNodes.item(i)).getFirstChild().getNodeValue(), true));
      }
    }
  }

  private static String getNationalPrefixFormattingRuleFromElement(Element element,
                                                                   String nationalPrefix) {
    String nationalPrefixFormattingRule = element.getAttribute("nationalPrefixFormattingRule");
    // Replace $NP with national prefix and $FG with the first group ($1).
    nationalPrefixFormattingRule =
        nationalPrefixFormattingRule.replaceFirst("\\$NP", nationalPrefix)
            .replaceFirst("\\$FG", "\\$1");
    return nationalPrefixFormattingRule;
  }

  private static String getDomesticCarrierCodeFormattingRuleFromElement(Element element,
                                                                        String nationalPrefix) {
    String carrierCodeFormattingRule = element.getAttribute("carrierCodeFormattingRule");
    // Replace $FG with the first group ($1) and $NP with the national prefix.
    carrierCodeFormattingRule = carrierCodeFormattingRule.replaceFirst("\\$FG", "\\$1")
        .replaceFirst("\\$NP", nationalPrefix);
    return carrierCodeFormattingRule;
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
  private static PhoneNumberDesc.Builder processPhoneNumberDescElement(
          PhoneNumberDesc.Builder generalDesc,
          Element countryElement,
          String numberType) {
    NodeList phoneNumberDescList = countryElement.getElementsByTagName(numberType);
    PhoneNumberDesc.Builder numberDesc = PhoneNumberDesc.newBuilder();
    if (phoneNumberDescList.getLength() == 0 &&
        (!numberType.equals("fixedLine") && !numberType.equals("mobile") &&
         !numberType.equals("generalDesc"))) {
      numberDesc.setNationalNumberPattern("NA");
      numberDesc.setPossibleNumberPattern("NA");
      return numberDesc;
    }
    numberDesc.mergeFrom(generalDesc.build());
    if (phoneNumberDescList.getLength() > 0) {
      Element element = (Element) phoneNumberDescList.item(0);
      NodeList possiblePattern = element.getElementsByTagName("possibleNumberPattern");
      if (possiblePattern.getLength() > 0) {
        numberDesc.setPossibleNumberPattern(
            validateRE(possiblePattern.item(0).getFirstChild().getNodeValue(), true));
      }

      NodeList validPattern = element.getElementsByTagName("nationalNumberPattern");
      if (validPattern.getLength() > 0) {
        numberDesc.setNationalNumberPattern(
            validateRE(validPattern.item(0).getFirstChild().getNodeValue(), true));
      }

      if (!liteBuild) {
        NodeList exampleNumber = element.getElementsByTagName("exampleNumber");
        if (exampleNumber.getLength() > 0) {
          numberDesc.setExampleNumber(exampleNumber.item(0).getFirstChild().getNodeValue());
        }
      }
    }
    return numberDesc;
  }
}
