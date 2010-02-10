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

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Tool to convert phone number metadata from the XML format to protocol buffer format. It is
 * wrapped in the genrule of the BUILD file and run as a preprocessing step when building the
 * phone number library. Example command line invocation:
 *
 * ./BuildMetadataProtoFromXml PhoneNumberMetadata.xml PhoneNumberMetadataProto true
 *
 * When liteBuild flag is set to true, the outputFile generated omits certain metadata which is not
 * needed for clients using liteBuild. At this moment, example numbers information is omitted. 
 *
 * @author Shaopeng Jia
 */
public class BuildMetadataProtoFromXml {
  private BuildMetadataProtoFromXml() { 
  }
  private static final Logger LOGGER = Logger.getLogger(BuildMetadataProtoFromXml.class.getName());
  private static Boolean liteBuild;

  public static void main(String[] args) {
    String inputFile = args[0];
    String outputFile = args[1];
    liteBuild = args.length > 2 && Boolean.getBoolean(args[2]); 
    File xmlFile = new File(inputFile);
    try {
      FileOutputStream output = new FileOutputStream(outputFile);
      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
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
      metadataCollection.build().writeTo(output);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.toString());
    } catch (SAXException e) {
      LOGGER.log(Level.SEVERE, e.toString());
    } catch (ParserConfigurationException e) {
      LOGGER.log(Level.SEVERE, e.toString());
    }
  }

  private static PhoneMetadata loadCountryMetadata(String regionCode, Element element) {
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    metadata.setId(regionCode);
    metadata.setCountryCode(Integer.parseInt(element.getAttribute("countryCode")));
    metadata.setInternationalPrefix(element.getAttribute("internationalPrefix"));
    if (element.hasAttribute("preferredInternationalPrefix")) {
      String preferredInternationalPrefix = element.getAttribute("preferredInternationalPrefix");
      metadata.setPreferredInternationalPrefix(preferredInternationalPrefix);
    }
    String nationalPrefix = "";
    if (element.hasAttribute("nationalPrefix")) {
      nationalPrefix = element.getAttribute("nationalPrefix");
      metadata.setNationalPrefix(nationalPrefix);
      metadata.setNationalPrefixFormattingRule(
          getNationalPrefixFormattingRuleFromElement(element, nationalPrefix));

      if (element.hasAttribute("nationalPrefixForParsing")) {
        metadata.setNationalPrefixForParsing(element.getAttribute("nationalPrefixForParsing"));
        if (element.hasAttribute("nationalPrefixTransformRule")) {
          metadata.setNationalPrefixTransformRule(
              element.getAttribute("nationalPrefixTransformRule"));
        }
      } else {
        metadata.setNationalPrefixForParsing(nationalPrefix);
      }
    }
    if (element.hasAttribute("preferredExtnPrefix")) {
      metadata.setPreferredExtnPrefix(element.getAttribute("preferredExtnPrefix"));
    }

    // Extract availableFormats
    NodeList numberFormatElements = element.getElementsByTagName("numberFormat");
    int numOfFormatElements = numberFormatElements.getLength();
    if (numOfFormatElements > 0) {
      for (int i = 0; i < numOfFormatElements; i++) {
        Element numberFormatElement = (Element) numberFormatElements.item(i);
        NumberFormat.Builder format = NumberFormat.newBuilder();
        if (numberFormatElement.hasAttribute("nationalPrefixFormattingRule")) {
          format.setNationalPrefixFormattingRule(
              getNationalPrefixFormattingRuleFromElement(numberFormatElement, nationalPrefix));
        } else {
          format.setNationalPrefixFormattingRule(metadata.getNationalPrefixFormattingRule());
        }
        if (numberFormatElement.hasAttribute("leadingDigits")) {
          format.setLeadingDigits(numberFormatElement.getAttribute("leadingDigits"));
        }
        format.setPattern(numberFormatElement.getAttribute("pattern"));
        String formatValue = numberFormatElement.getFirstChild().getNodeValue();
        format.setFormat(formatValue);
        metadata.addNumberFormat(format.build());
      }
    }

    NodeList intlNumberFormatElements = element.getElementsByTagName("intlNumberFormat");
    int numOfIntlFormatElements = intlNumberFormatElements.getLength();
    if (numOfIntlFormatElements > 0) {
      for (int i = 0; i < numOfIntlFormatElements; i++) {
        Element numberFormatElement = (Element) intlNumberFormatElements.item(i);
        NumberFormat.Builder format = NumberFormat.newBuilder();
        if (numberFormatElement.hasAttribute("leadingDigits")) {
          format.setLeadingDigits(numberFormatElement.getAttribute("leadingDigits"));
        }
        format.setPattern(numberFormatElement.getAttribute("pattern"));
        format.setFormat(numberFormatElement.getFirstChild().getNodeValue());
        metadata.addIntlNumberFormat(format.build());
      }
    }

    PhoneNumberDesc generalDesc =
        processPhoneNumberDescElement(PhoneNumberDesc.newBuilder().build(),
                                      element, "generalDesc");
    metadata.setGeneralDesc(generalDesc);
    metadata.setFixedLine(processPhoneNumberDescElement(generalDesc, element, "fixedLine"));
    metadata.setMobile(processPhoneNumberDescElement(generalDesc, element, "mobile"));
    metadata.setTollFree(processPhoneNumberDescElement(generalDesc, element, "tollFree"));
    metadata.setPremiumRate(processPhoneNumberDescElement(generalDesc, element, "premiumRate"));
    metadata.setSharedCost(processPhoneNumberDescElement(generalDesc, element, "sharedCost"));
    metadata.setVoip(processPhoneNumberDescElement(generalDesc, element, "voip"));
    metadata.setPersonalNumber(processPhoneNumberDescElement(generalDesc, element,
                                                             "personalNumber"));

    if (metadata.getMobile().getNationalNumberPattern().equals(
        metadata.getFixedLine().getNationalNumberPattern())) {
      metadata.setSameMobileAndFixedLinePattern(true);
    }
    return metadata.build();
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
  private static PhoneNumberDesc processPhoneNumberDescElement(PhoneNumberDesc generalDesc,
                                                               Element countryElement,
                                                               String numberType) {
    NodeList phoneNumberDescList = countryElement.getElementsByTagName(numberType);
    PhoneNumberDesc.Builder numberDesc = PhoneNumberDesc.newBuilder();
    if (phoneNumberDescList.getLength() == 0 &&
        (!numberType.equals("fixedLine") && !numberType.equals("mobile") &&
         !numberType.equals("generalDesc"))) {
      numberDesc.setNationalNumberPattern("NA");
      numberDesc.setPossibleNumberPattern("NA");
      return numberDesc.build();
    }
    numberDesc.mergeFrom(generalDesc);
    if (phoneNumberDescList.getLength() > 0) {
      Element element = (Element) phoneNumberDescList.item(0);
      NodeList possiblePattern = element.getElementsByTagName("possibleNumberPattern");
      if (possiblePattern.getLength() > 0) {
        numberDesc.setPossibleNumberPattern(possiblePattern.
            item(0).getFirstChild().getNodeValue());
      }

      NodeList validPattern = element.getElementsByTagName("nationalNumberPattern");
      if (validPattern.getLength() > 0) {
        numberDesc.setNationalNumberPattern(validPattern.
            item(0).getFirstChild().getNodeValue());
      }

      if (!liteBuild) {
        NodeList exampleNumber = element.getElementsByTagName("exampleNumber");
        if (exampleNumber.getLength() > 0) {
          numberDesc.setExampleNumber(exampleNumber.item(0).getFirstChild().getNodeValue());
        }
      }
    }
    return numberDesc.build();
  }
}
