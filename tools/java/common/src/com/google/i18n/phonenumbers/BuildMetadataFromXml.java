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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Library to build phone number metadata from the XML format.
 *
 * @author Shaopeng Jia
 */
public class BuildMetadataFromXml {
  private static final Logger logger = Logger.getLogger(BuildMetadataFromXml.class.getName());

  // String constants used to fetch the XML nodes and attributes.
  private static final String CARRIER_CODE_FORMATTING_RULE = "carrierCodeFormattingRule";
  private static final String CARRIER_SPECIFIC = "carrierSpecific";
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
  private static final String MOBILE_NUMBER_PORTABLE_REGION = "mobileNumberPortableRegion";
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
  private static final String POSSIBLE_LENGTHS = "possibleLengths";
  private static final String NATIONAL = "national";
  private static final String LOCAL_ONLY = "localOnly";
  private static final String PREFERRED_EXTN_PREFIX = "preferredExtnPrefix";
  private static final String PREFERRED_INTERNATIONAL_PREFIX = "preferredInternationalPrefix";
  private static final String PREMIUM_RATE = "premiumRate";
  private static final String SHARED_COST = "sharedCost";
  private static final String SHORT_CODE = "shortCode";
  private static final String STANDARD_RATE = "standardRate";
  private static final String TOLL_FREE = "tollFree";
  private static final String UAN = "uan";
  private static final String VOICEMAIL = "voicemail";
  private static final String VOIP = "voip";

  private static final Set<String> PHONE_NUMBER_DESCS_WITHOUT_MATCHING_TYPES =
      new HashSet<String>(Arrays.asList(new String[]{NO_INTERNATIONAL_DIALLING}));

  // Build the PhoneMetadataCollection from the input XML file.
  public static PhoneMetadataCollection buildPhoneMetadataCollection(String inputXmlFile,
      boolean liteBuild, boolean specialBuild) throws Exception {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    File xmlFile = new File(inputXmlFile);
    Document document = builder.parse(xmlFile);
    // TODO: Look for other uses of these constants and possibly pull them out into a separate
    // constants file.
    boolean isShortNumberMetadata = inputXmlFile.contains("ShortNumberMetadata");
    boolean isAlternateFormatsMetadata = inputXmlFile.contains("PhoneNumberAlternateFormats");
    return buildPhoneMetadataCollection(document, liteBuild, specialBuild,
        isShortNumberMetadata, isAlternateFormatsMetadata);
  }

  // @VisibleForTesting
  static PhoneMetadataCollection buildPhoneMetadataCollection(Document document,
      boolean liteBuild, boolean specialBuild, boolean isShortNumberMetadata,
      boolean isAlternateFormatsMetadata) throws Exception {
    document.getDocumentElement().normalize();
    Element rootElement = document.getDocumentElement();
    NodeList territory = rootElement.getElementsByTagName("territory");
    PhoneMetadataCollection.Builder metadataCollection = PhoneMetadataCollection.newBuilder();
    int numOfTerritories = territory.getLength();
    // TODO: Infer filter from a single flag.
    MetadataFilter metadataFilter = getMetadataFilter(liteBuild, specialBuild);
    for (int i = 0; i < numOfTerritories; i++) {
      Element territoryElement = (Element) territory.item(i);
      String regionCode = "";
      // For the main metadata file this should always be set, but for other supplementary data
      // files the country calling code may be all that is needed.
      if (territoryElement.hasAttribute("id")) {
        regionCode = territoryElement.getAttribute("id");
      }
      PhoneMetadata.Builder metadata = loadCountryMetadata(regionCode, territoryElement,
          isShortNumberMetadata, isAlternateFormatsMetadata);
      metadataFilter.filterMetadata(metadata);
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
    Map<Integer, List<String>> countryCodeToRegionCodeMap = new TreeMap<Integer, List<String>>();
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
        if (!regionCode.equals("")) {  // For alternate formats, there are no region codes at all.
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
    String compressedRegex = removeWhitespace ? regex.replaceAll("\\s", "") : regex;
    Pattern.compile(compressedRegex);
    // We don't ever expect to see | followed by a ) in our metadata - this would be an indication
    // of a bug. If one wants to make something optional, we prefer ? to using an empty group.
    int errorIndex = compressedRegex.indexOf("|)");
    if (errorIndex >= 0) {
      logger.log(Level.SEVERE, "Error with original regex: " + regex
          + "\n| should not be followed directly by ) in phone number regular expressions.");
      throw new PatternSyntaxException("| followed by )", compressedRegex, errorIndex);
    }
    // return the regex if it is of correct syntax, i.e. compile did not fail with a
    // PatternSyntaxException.
    return compressedRegex;
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
    if (element.hasAttribute(COUNTRY_CODE)) {
      metadata.setCountryCode(Integer.parseInt(element.getAttribute(COUNTRY_CODE)));
    }
    if (element.hasAttribute(LEADING_DIGITS)) {
      metadata.setLeadingDigits(validateRE(element.getAttribute(LEADING_DIGITS)));
    }
    if (element.hasAttribute(INTERNATIONAL_PREFIX)) {
      metadata.setInternationalPrefix(validateRE(element.getAttribute(INTERNATIONAL_PREFIX)));
    }
    if (element.hasAttribute(PREFERRED_INTERNATIONAL_PREFIX)) {
      metadata.setPreferredInternationalPrefix(
          element.getAttribute(PREFERRED_INTERNATIONAL_PREFIX));
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
    if (element.hasAttribute(MOBILE_NUMBER_PORTABLE_REGION)) {
      metadata.setMobileNumberPortableRegion(true);
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
                                         NumberFormat nationalFormat) {
    NumberFormat.Builder intlFormat = NumberFormat.newBuilder();
    NodeList intlFormatPattern = numberFormatElement.getElementsByTagName(INTL_FORMAT);
    boolean hasExplicitIntlFormatDefined = false;

    if (intlFormatPattern.getLength() > 1) {
      logger.log(Level.SEVERE,
          "A maximum of one intlFormat pattern for a numberFormat element should be defined.");
      String countryId = metadata.getId().length() > 0 ? metadata.getId()
          : Integer.toString(metadata.getCountryCode());
      throw new RuntimeException("Invalid number of intlFormat patterns for country: " + countryId);
    } else if (intlFormatPattern.getLength() == 0) {
      // Default to use the same as the national pattern if none is defined.
      intlFormat.mergeFrom(nationalFormat);
    } else {
      intlFormat.setPattern(numberFormatElement.getAttribute(PATTERN));
      setLeadingDigitsPatterns(numberFormatElement, intlFormat);
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
   */
  // @VisibleForTesting
  static void loadNationalFormat(PhoneMetadata.Builder metadata, Element numberFormatElement,
                                 NumberFormat.Builder format) {
    setLeadingDigitsPatterns(numberFormatElement, format);
    format.setPattern(validateRE(numberFormatElement.getAttribute(PATTERN)));

    NodeList formatPattern = numberFormatElement.getElementsByTagName(FORMAT);
    int numFormatPatterns = formatPattern.getLength();
    if (numFormatPatterns != 1) {
      logger.log(Level.SEVERE, "One format pattern for a numberFormat element should be defined.");
      String countryId = metadata.getId().length() > 0 ? metadata.getId()
          : Integer.toString(metadata.getCountryCode());
      throw new RuntimeException("Invalid number of format patterns (" + numFormatPatterns
          + ") for country: " + countryId);
    }
    format.setFormat(formatPattern.item(0).getFirstChild().getNodeValue());
  }

  /**
   * Extracts the available formats from the provided DOM element. If it does not contain any
   * nationalPrefixFormattingRule, the one passed-in is retained; similarly for
   * nationalPrefixOptionalWhenFormatting. The nationalPrefix, nationalPrefixFormattingRule and
   * nationalPrefixOptionalWhenFormatting values are provided from the parent (territory) element.
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
        } else if (!nationalPrefixFormattingRule.equals("")) {
          format.setNationalPrefixFormattingRule(nationalPrefixFormattingRule);
        }
        if (numberFormatElement.hasAttribute(NATIONAL_PREFIX_OPTIONAL_WHEN_FORMATTING)) {
          format.setNationalPrefixOptionalWhenFormatting(
              Boolean.valueOf(numberFormatElement.getAttribute(
                  NATIONAL_PREFIX_OPTIONAL_WHEN_FORMATTING)));
        } else if (format.getNationalPrefixOptionalWhenFormatting()
            != nationalPrefixOptionalWhenFormatting) {
          // Inherit from the parent field if it is not already the same as the default.
          format.setNationalPrefixOptionalWhenFormatting(nationalPrefixOptionalWhenFormatting);
        }
        if (numberFormatElement.hasAttribute(CARRIER_CODE_FORMATTING_RULE)) {
          format.setDomesticCarrierCodeFormattingRule(validateRE(
              getDomesticCarrierCodeFormattingRuleFromElement(numberFormatElement,
                                                              nationalPrefix)));
        } else if (!carrierCodeFormattingRule.equals("")) {
          format.setDomesticCarrierCodeFormattingRule(carrierCodeFormattingRule);
        }
        loadNationalFormat(metadata, numberFormatElement, format);
        metadata.addNumberFormat(format);

        if (loadInternationalFormat(metadata, numberFormatElement, format.build())) {
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

  /**
   * Checks if the possible lengths provided as a sorted set are equal to the possible lengths
   * stored already in the description pattern. Note that possibleLengths may be empty but must not
   * be null, and the PhoneNumberDesc passed in should also not be null.
   */
  private static boolean arePossibleLengthsEqual(TreeSet<Integer> possibleLengths,
      PhoneNumberDesc desc) {
    if (possibleLengths.size() != desc.getPossibleLengthCount()) {
      return false;
    }
    // Note that both should be sorted already, and we know they are the same length.
    int i = 0;
    for (Integer length : possibleLengths) {
      if (length != desc.getPossibleLength(i)) {
        return false;
      }
      i++;
    }
    return true;
  }

  /**
   * Processes a phone number description element from the XML file and returns it as a
   * PhoneNumberDesc. If the description element is a fixed line or mobile number, the parent
   * description will be used to fill in the whole element if necessary, or any components that are
   * missing. For all other types, the parent description will only be used to fill in missing
   * components if the type has a partial definition. For example, if no "tollFree" element exists,
   * we assume there are no toll free numbers for that locale, and return a phone number description
   * with "NA" for both the national and possible number patterns. Note that the parent description
   * must therefore already be processed before this method is called on any child elements.
   *
   * @param parentDesc  a generic phone number description that will be used to fill in missing
   *     parts of the description, or null if this is the root node. This must be processed before
   *     this is run on any child elements.
   * @param countryElement  the XML element representing all the country information
   * @param numberType  the name of the number type, corresponding to the appropriate tag in the XML
   *     file with information about that type
   * @return  complete description of that phone number type
   */
  // @VisibleForTesting
  static PhoneNumberDesc.Builder processPhoneNumberDescElement(PhoneNumberDesc parentDesc,
                                                               Element countryElement,
                                                               String numberType) {
    NodeList phoneNumberDescList = countryElement.getElementsByTagName(numberType);
    PhoneNumberDesc.Builder numberDesc = PhoneNumberDesc.newBuilder();
    if (phoneNumberDescList.getLength() == 0) {
      numberDesc.setNationalNumberPattern("NA");
      // -1 will never match a possible phone number length, so is safe to use to ensure this never
      // matches. We don't leave it empty, since for compression reasons, we use the empty list to
      // mean that the generalDesc possible lengths apply.
      numberDesc.addPossibleLength(-1);
      return numberDesc;
    }
    if (phoneNumberDescList.getLength() > 0) {
      if (phoneNumberDescList.getLength() > 1) {
        throw new RuntimeException(
            String.format("Multiple elements with type %s found.", numberType));
      }
      Element element = (Element) phoneNumberDescList.item(0);
      if (parentDesc != null) {
        // New way of handling possible number lengths. We don't do this for the general
        // description, since these tags won't be present; instead we will calculate its values
        // based on the values for all the other number type descriptions (see
        // setPossibleLengthsGeneralDesc).
        TreeSet<Integer> lengths = new TreeSet<Integer>();
        TreeSet<Integer> localOnlyLengths = new TreeSet<Integer>();
        populatePossibleLengthSets(element, lengths, localOnlyLengths);
        setPossibleLengths(lengths, localOnlyLengths, parentDesc, numberDesc);
      }

      NodeList validPattern = element.getElementsByTagName(NATIONAL_NUMBER_PATTERN);
      if (validPattern.getLength() > 0) {
        numberDesc.setNationalNumberPattern(
            validateRE(validPattern.item(0).getFirstChild().getNodeValue(), true));
      }

      NodeList exampleNumber = element.getElementsByTagName(EXAMPLE_NUMBER);
      if (exampleNumber.getLength() > 0) {
        numberDesc.setExampleNumber(exampleNumber.item(0).getFirstChild().getNodeValue());
      }
    }
    return numberDesc;
  }

  // @VisibleForTesting
  static void setRelevantDescPatterns(PhoneMetadata.Builder metadata, Element element,
      boolean isShortNumberMetadata) {
    PhoneNumberDesc.Builder generalDescBuilder = processPhoneNumberDescElement(null, element,
        GENERAL_DESC);
    // Calculate the possible lengths for the general description. This will be based on the
    // possible lengths of the child elements.
    setPossibleLengthsGeneralDesc(
        generalDescBuilder, metadata.getId(), element, isShortNumberMetadata);
    metadata.setGeneralDesc(generalDescBuilder);

    PhoneNumberDesc generalDesc = metadata.getGeneralDesc();

    if (!isShortNumberMetadata) {
      // Set fields used by regular length phone numbers.
      metadata.setFixedLine(processPhoneNumberDescElement(generalDesc, element, FIXED_LINE));
      metadata.setMobile(processPhoneNumberDescElement(generalDesc, element, MOBILE));
      metadata.setSharedCost(processPhoneNumberDescElement(generalDesc, element, SHARED_COST));
      metadata.setVoip(processPhoneNumberDescElement(generalDesc, element, VOIP));
      metadata.setPersonalNumber(processPhoneNumberDescElement(generalDesc, element,
          PERSONAL_NUMBER));
      metadata.setPager(processPhoneNumberDescElement(generalDesc, element, PAGER));
      metadata.setUan(processPhoneNumberDescElement(generalDesc, element, UAN));
      metadata.setVoicemail(processPhoneNumberDescElement(generalDesc, element, VOICEMAIL));
      metadata.setNoInternationalDialling(processPhoneNumberDescElement(generalDesc, element,
          NO_INTERNATIONAL_DIALLING));
      boolean mobileAndFixedAreSame = metadata.getMobile().getNationalNumberPattern()
          .equals(metadata.getFixedLine().getNationalNumberPattern());
      if (metadata.getSameMobileAndFixedLinePattern() != mobileAndFixedAreSame) {
        // Set this if it is not the same as the default.
        metadata.setSameMobileAndFixedLinePattern(mobileAndFixedAreSame);
      }
      metadata.setTollFree(processPhoneNumberDescElement(generalDesc, element, TOLL_FREE));
      metadata.setPremiumRate(processPhoneNumberDescElement(generalDesc, element, PREMIUM_RATE));
    } else {
      // Set fields used by short numbers.
      metadata.setStandardRate(processPhoneNumberDescElement(generalDesc, element, STANDARD_RATE));
      metadata.setShortCode(processPhoneNumberDescElement(generalDesc, element, SHORT_CODE));
      metadata.setCarrierSpecific(processPhoneNumberDescElement(generalDesc, element,
          CARRIER_SPECIFIC));
      metadata.setEmergency(processPhoneNumberDescElement(generalDesc, element, EMERGENCY));
      metadata.setTollFree(processPhoneNumberDescElement(generalDesc, element, TOLL_FREE));
      metadata.setPremiumRate(processPhoneNumberDescElement(generalDesc, element, PREMIUM_RATE));
    }
  }

  /**
   * Parses a possible length string into a set of the integers that are covered.
   *
   * @param possibleLengthString  a string specifying the possible lengths of phone numbers. Follows
   *     this syntax: ranges or elements are separated by commas, and ranges are specified in
   *     [min-max] notation, inclusive. For example, [3-5],7,9,[11-14] should be parsed to
   *     3,4,5,7,9,11,12,13,14.
   */
  private static Set<Integer> parsePossibleLengthStringToSet(String possibleLengthString) {
    if (possibleLengthString.length() == 0) {
      throw new RuntimeException("Empty possibleLength string found.");
    }
    String[] lengths = possibleLengthString.split(",");
    Set<Integer> lengthSet = new TreeSet<Integer>();
    for (int i = 0; i < lengths.length; i++) {
      String lengthSubstring = lengths[i];
      if (lengthSubstring.length() == 0) {
        throw new RuntimeException(String.format("Leading, trailing or adjacent commas in possible "
            + "length string %s, these should only separate numbers or ranges.",
            possibleLengthString));
      } else if (lengthSubstring.charAt(0) == '[') {
        if (lengthSubstring.charAt(lengthSubstring.length() - 1) != ']') {
          throw new RuntimeException(String.format("Missing end of range character in possible "
              + "length string %s.", possibleLengthString));
        }
        // Strip the leading and trailing [], and split on the -.
        String[] minMax = lengthSubstring.substring(1, lengthSubstring.length() - 1).split("-");
        if (minMax.length != 2) {
          throw new RuntimeException(String.format("Ranges must have exactly one - character: "
              + "missing for %s.", possibleLengthString));
        }
        int min = Integer.parseInt(minMax[0]);
        int max = Integer.parseInt(minMax[1]);
        // We don't even accept [6-7] since we prefer the shorter 6,7 variant; for a range to be in
        // use the hyphen needs to replace at least one digit.
        if (max - min < 2) {
          throw new RuntimeException(String.format("The first number in a range should be two or "
              + "more digits lower than the second. Culprit possibleLength string: %s",
              possibleLengthString));
        }
        for (int j = min; j <= max; j++) {
          if (!lengthSet.add(j)) {
            throw new RuntimeException(String.format("Duplicate length element found (%d) in "
                + "possibleLength string %s", j, possibleLengthString));
          }
        }
      } else {
        int length = Integer.parseInt(lengthSubstring);
        if (!lengthSet.add(length)) {
            throw new RuntimeException(String.format("Duplicate length element found (%d) in "
                + "possibleLength string %s", length, possibleLengthString));
          }
      }
    }
    return lengthSet;
  }

  /**
   * Reads the possible lengths present in the metadata and splits them into two sets: one for
   * full-length numbers, one for local numbers.
   *
   * @param data  one or more phone number descriptions, represented as XML nodes
   * @param lengths  a set to which to add possible lengths of full phone numbers
   * @param localOnlyLengths  a set to which to add possible lengths of phone numbers only diallable
   *     locally (e.g. within a province)
   */
  private static void populatePossibleLengthSets(Element data, TreeSet<Integer> lengths,
      TreeSet<Integer> localOnlyLengths) {
    NodeList possibleLengths = data.getElementsByTagName(POSSIBLE_LENGTHS);
    for (int i = 0; i < possibleLengths.getLength(); i++) {
      Element element = (Element) possibleLengths.item(i);
      String nationalLengths = element.getAttribute(NATIONAL);
      // We don't add to the phone metadata yet, since we want to sort length elements found under
      // different nodes first, make sure there are no duplicates between them and that the
      // localOnly lengths don't overlap with the others.
      Set<Integer> thisElementLengths = parsePossibleLengthStringToSet(nationalLengths);
      if (element.hasAttribute(LOCAL_ONLY)) {
        String localLengths = element.getAttribute(LOCAL_ONLY);
        Set<Integer> thisElementLocalOnlyLengths = parsePossibleLengthStringToSet(localLengths);
        Set<Integer> intersection = new HashSet<Integer>(thisElementLengths);
        intersection.retainAll(thisElementLocalOnlyLengths);
        if (!intersection.isEmpty()) {
          throw new RuntimeException(String.format(
              "Possible length(s) found specified as a normal and local-only length: %s",
              intersection));
        }
        // We check again when we set these lengths on the metadata itself in setPossibleLengths
        // that the elements in localOnly are not also in lengths. For e.g. the generalDesc, it
        // might have a local-only length for one type that is a normal length for another type. We
        // don't consider this an error, but we do want to remove the local-only lengths.
        localOnlyLengths.addAll(thisElementLocalOnlyLengths);
      }
      // It is okay if at this time we have duplicates, because the same length might be possible
      // for e.g. fixed-line and for mobile numbers, and this method operates potentially on
      // multiple phoneNumberDesc XML elements.
      lengths.addAll(thisElementLengths);
    }
  }

  /**
   * Sets possible lengths in the general description, derived from certain child elements.
   */
  // @VisibleForTesting
  static void setPossibleLengthsGeneralDesc(PhoneNumberDesc.Builder generalDesc, String metadataId,
      Element data, boolean isShortNumberMetadata) {
    TreeSet<Integer> lengths = new TreeSet<Integer>();
    TreeSet<Integer> localOnlyLengths = new TreeSet<Integer>();
    // The general description node should *always* be present if metadata for other types is
    // present, aside from in some unit tests.
    // (However, for e.g. formatting metadata in PhoneNumberAlternateFormats, no PhoneNumberDesc
    // elements are present).
    NodeList generalDescNodes = data.getElementsByTagName(GENERAL_DESC);
    if (generalDescNodes.getLength() > 0) {
      Element generalDescNode = (Element) generalDescNodes.item(0);
      populatePossibleLengthSets(generalDescNode, lengths, localOnlyLengths);
      if (!lengths.isEmpty() || !localOnlyLengths.isEmpty()) {
        // We shouldn't have anything specified at the "general desc" level: we are going to
        // calculate this ourselves from child elements.
        throw new RuntimeException(String.format("Found possible lengths specified at general "
            + "desc: this should be derived from child elements. Affected country: %s",
            metadataId));
      }
    }
    if (!isShortNumberMetadata) {
      // Make a copy here since we want to remove some nodes, but we don't want to do that on our
      // actual data.
      Element allDescData = (Element) data.cloneNode(true /* deep copy */);
      for (String tag : PHONE_NUMBER_DESCS_WITHOUT_MATCHING_TYPES) {
        NodeList nodesToRemove = allDescData.getElementsByTagName(tag);
        if (nodesToRemove.getLength() > 0) {
          // We check when we process phone number descriptions that there are only one of each
          // type, so this is safe to do.
          allDescData.removeChild(nodesToRemove.item(0));
        }
      }
      populatePossibleLengthSets(allDescData, lengths, localOnlyLengths);
    } else {
      // For short number metadata, we want to copy the lengths from the "short code" section only.
      // This is because it's the more detailed validation pattern, it's not a sub-type of short
      // codes. The other lengths will be checked later to see that they are a sub-set of these
      // possible lengths.
      NodeList shortCodeDescList = data.getElementsByTagName(SHORT_CODE);
      if (shortCodeDescList.getLength() > 0) {
        Element shortCodeDesc = (Element) shortCodeDescList.item(0);
        populatePossibleLengthSets(shortCodeDesc, lengths, localOnlyLengths);
      }
      if (localOnlyLengths.size() > 0) {
        throw new RuntimeException("Found local-only lengths in short-number metadata");
      }
    }
    setPossibleLengths(lengths, localOnlyLengths, null, generalDesc);
  }

  /**
   * Sets the possible length fields in the metadata from the sets of data passed in. Checks that
   * the length is covered by the "parent" phone number description element if one is present, and
   * if the lengths are exactly the same as this, they are not filled in for efficiency reasons.
   *
   * @param parentDesc  the "general description" element or null if desc is the generalDesc itself
   * @param desc  the PhoneNumberDesc object that we are going to set lengths for
   */
  private static void setPossibleLengths(TreeSet<Integer> lengths,
      TreeSet<Integer> localOnlyLengths, PhoneNumberDesc parentDesc, PhoneNumberDesc.Builder desc) {
    // We clear these fields since the metadata tends to inherit from the parent element for other
    // fields (via a mergeFrom).
    desc.clearPossibleLength();
    desc.clearPossibleLengthLocalOnly();
    // Only add the lengths to this sub-type if they aren't exactly the same as the possible
    // lengths in the general desc (for metadata size reasons).
    if (parentDesc == null || !arePossibleLengthsEqual(lengths, parentDesc)) {
      for (Integer length : lengths) {
        if (parentDesc == null || parentDesc.getPossibleLengthList().contains(length)) {
          desc.addPossibleLength(length);
        } else {
          // We shouldn't have possible lengths defined in a child element that are not covered by
          // the general description. We check this here even though the general description is
          // derived from child elements because it is only derived from a subset, and we need to
          // ensure *all* child elements have a valid possible length.
          throw new RuntimeException(String.format(
              "Out-of-range possible length found (%d), parent lengths %s.",
              length, parentDesc.getPossibleLengthList()));
        }
      }
    }
    // We check that the local-only length isn't also a normal possible length (only relevant for
    // the general-desc, since within elements such as fixed-line we would throw an exception if we
    // saw this) before adding it to the collection of possible local-only lengths.
    for (Integer length : localOnlyLengths) {
      if (!lengths.contains(length)) {
        // We check it is covered by either of the possible length sets of the parent
        // PhoneNumberDesc, because for example 7 might be a valid localOnly length for mobile, but
        // a valid national length for fixedLine, so the generalDesc would have the 7 removed from
        // localOnly.
        if (parentDesc == null || parentDesc.getPossibleLengthLocalOnlyList().contains(length)
          || parentDesc.getPossibleLengthList().contains(length)) {
          desc.addPossibleLengthLocalOnly(length);
        } else {
          throw new RuntimeException(String.format(
              "Out-of-range local-only possible length found (%d), parent length %s.",
              length, parentDesc.getPossibleLengthLocalOnlyList()));
        }
      }
    }
  }

  // @VisibleForTesting
  static PhoneMetadata.Builder loadCountryMetadata(String regionCode,
      Element element,
      boolean isShortNumberMetadata,
      boolean isAlternateFormatsMetadata) {
    String nationalPrefix = getNationalPrefix(element);
    PhoneMetadata.Builder metadata = loadTerritoryTagMetadata(regionCode, element, nationalPrefix);
    String nationalPrefixFormattingRule =
        getNationalPrefixFormattingRuleFromElement(element, nationalPrefix);
    loadAvailableFormats(metadata, element, nationalPrefix,
                         nationalPrefixFormattingRule,
                         element.hasAttribute(NATIONAL_PREFIX_OPTIONAL_WHEN_FORMATTING));
    if (!isAlternateFormatsMetadata) {
      // The alternate formats metadata does not need most of the patterns to be set.
      setRelevantDescPatterns(metadata, element, isShortNumberMetadata);
    }
    return metadata;
  }

  /**
   * Processes the custom build flags and gets a {@code MetadataFilter} which may be used to
   * filter {@code PhoneMetadata} objects. Incompatible flag combinations throw RuntimeException.
   *
   * @param liteBuild  The liteBuild flag value as given by the command-line
   * @param specialBuild  The specialBuild flag value as given by the command-line
   */
  // @VisibleForTesting
  static MetadataFilter getMetadataFilter(boolean liteBuild, boolean specialBuild) {
    if (specialBuild) {
      if (liteBuild) {
        throw new RuntimeException("liteBuild and specialBuild may not both be set");
      }
      return MetadataFilter.forSpecialBuild();
    }
    if (liteBuild) {
      return MetadataFilter.forLiteBuild();
    }
    return MetadataFilter.emptyFilter();
  }
}
