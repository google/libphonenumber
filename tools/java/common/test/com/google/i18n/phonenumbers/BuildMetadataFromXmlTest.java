/*
 *  Copyright (C) 2011 The Libphonenumber Authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.regex.PatternSyntaxException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Unit tests for BuildMetadataFromXml.java
 *
 * @author Philippe Liard
 */
public class BuildMetadataFromXmlTest extends TestCase {

  // Helper method that outputs a DOM element from a XML string.
  private static Element parseXmlString(String xmlString)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    InputSource inputSource = new InputSource();
    inputSource.setCharacterStream(new StringReader(xmlString));
    return documentBuilder.parse(inputSource).getDocumentElement();
  }

  // Tests validateRE().
  public void testValidateRERemovesWhiteSpaces() {
    String input = " hello world ";
    // Should remove all the white spaces contained in the provided string.
    assertEquals("helloworld", BuildMetadataFromXml.validateRE(input, true));
    // Make sure it only happens when the last parameter is set to true.
    assertEquals(" hello world ", BuildMetadataFromXml.validateRE(input, false));
  }

  public void testValidateREThrowsException() {
    String invalidPattern = "[";
    // Should throw an exception when an invalid pattern is provided independently of the last
    // parameter (remove white spaces).
    try {
      BuildMetadataFromXml.validateRE(invalidPattern, false);
      fail();
    } catch (PatternSyntaxException e) {
      // Test passed.
    }
    try {
      BuildMetadataFromXml.validateRE(invalidPattern, true);
      fail();
    } catch (PatternSyntaxException e) {
      // Test passed.
    }
    // We don't allow | to be followed by ) because it introduces bugs, since we typically use it at
    // the end of each line and when a line is deleted, if the pipe from the previous line is not
    // removed, we end up erroneously accepting an empty group as well.
    String patternWithPipeFollowedByClosingParentheses = "|)";
    try {
      BuildMetadataFromXml.validateRE(patternWithPipeFollowedByClosingParentheses, true);
      fail();
    } catch (PatternSyntaxException e) {
      // Test passed.
    }
    String patternWithPipeFollowedByNewLineAndClosingParentheses = "|\n)";
    try {
      BuildMetadataFromXml.validateRE(patternWithPipeFollowedByNewLineAndClosingParentheses, true);
      fail();
    } catch (PatternSyntaxException e) {
      // Test passed.
    }
  }

  public void testValidateRE() {
    String validPattern = "[a-zA-Z]d{1,9}";
    // The provided pattern should be left unchanged.
    assertEquals(validPattern, BuildMetadataFromXml.validateRE(validPattern, false));
  }

  // Tests getNationalPrefix().
  public void testGetNationalPrefix()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory nationalPrefix='00'/>";
    Element territoryElement = parseXmlString(xmlInput);
    assertEquals("00", BuildMetadataFromXml.getNationalPrefix(territoryElement));
  }

  // Tests loadTerritoryTagMetadata().
  public void testLoadTerritoryTagMetadata()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory"
        + "  countryCode='33' leadingDigits='2' internationalPrefix='00'"
        + "  preferredInternationalPrefix='00~11' nationalPrefixForParsing='0'"
        + "  nationalPrefixTransformRule='9$1'"  // nationalPrefix manually injected.
        + "  preferredExtnPrefix=' x' mainCountryForCode='true'"
        + "  mobileNumberPortableRegion='true'>"
        + "</territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder phoneMetadata =
        BuildMetadataFromXml.loadTerritoryTagMetadata("33", territoryElement, "0");
    assertEquals(33, phoneMetadata.getCountryCode());
    assertEquals("2", phoneMetadata.getLeadingDigits());
    assertEquals("00", phoneMetadata.getInternationalPrefix());
    assertEquals("00~11", phoneMetadata.getPreferredInternationalPrefix());
    assertEquals("0", phoneMetadata.getNationalPrefixForParsing());
    assertEquals("9$1", phoneMetadata.getNationalPrefixTransformRule());
    assertEquals("0", phoneMetadata.getNationalPrefix());
    assertEquals(" x", phoneMetadata.getPreferredExtnPrefix());
    assertTrue(phoneMetadata.getMainCountryForCode());
    assertTrue(phoneMetadata.isMobileNumberPortableRegion());
  }

  public void testLoadTerritoryTagMetadataSetsBooleanFieldsToFalseByDefault()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory countryCode='33'/>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder phoneMetadata =
        BuildMetadataFromXml.loadTerritoryTagMetadata("33", territoryElement, "");
    assertFalse(phoneMetadata.getMainCountryForCode());
    assertFalse(phoneMetadata.isMobileNumberPortableRegion());
  }

  public void testLoadTerritoryTagMetadataSetsNationalPrefixForParsingByDefault()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory countryCode='33'/>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder phoneMetadata =
        BuildMetadataFromXml.loadTerritoryTagMetadata("33", territoryElement, "00");
    // When unspecified, nationalPrefixForParsing defaults to nationalPrefix.
    assertEquals("00", phoneMetadata.getNationalPrefix());
    assertEquals(phoneMetadata.getNationalPrefix(), phoneMetadata.getNationalPrefixForParsing());
  }

  public void testLoadTerritoryTagMetadataWithRequiredAttributesOnly()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory countryCode='33' internationalPrefix='00'/>";
    Element territoryElement = parseXmlString(xmlInput);
    // Should not throw any exception.
    BuildMetadataFromXml.loadTerritoryTagMetadata("33", territoryElement, "");
  }

  // Tests loadInternationalFormat().
  public void testLoadInternationalFormat()
      throws ParserConfigurationException, SAXException, IOException {
    String intlFormat = "$1 $2";
    String xmlInput = "<numberFormat><intlFormat>" + intlFormat + "</intlFormat></numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    NumberFormat nationalFormat = NumberFormat.newBuilder().build();

    assertTrue(BuildMetadataFromXml.loadInternationalFormat(metadata, numberFormatElement,
                                                            nationalFormat));
    assertEquals(intlFormat, metadata.getIntlNumberFormat(0).getFormat());
  }

  public void testLoadInternationalFormatWithBothNationalAndIntlFormatsDefined()
      throws ParserConfigurationException, SAXException, IOException {
    String intlFormat = "$1 $2";
    String xmlInput = "<numberFormat><intlFormat>" + intlFormat + "</intlFormat></numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    NumberFormat.Builder nationalFormat = NumberFormat.newBuilder();
    nationalFormat.setFormat("$1");

    assertTrue(BuildMetadataFromXml.loadInternationalFormat(metadata, numberFormatElement,
                                                            nationalFormat.build()));
    assertEquals(intlFormat, metadata.getIntlNumberFormat(0).getFormat());
  }

  public void testLoadInternationalFormatExpectsOnlyOnePattern()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<numberFormat><intlFormat/><intlFormat/></numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();

    // Should throw an exception as multiple intlFormats are provided.
    try {
      BuildMetadataFromXml.loadInternationalFormat(metadata, numberFormatElement,
                                                   NumberFormat.newBuilder().build());
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }
  }

  public void testLoadInternationalFormatUsesNationalFormatByDefault()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<numberFormat></numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    NumberFormat.Builder nationalFormat = NumberFormat.newBuilder();
    String nationalPattern = "$1 $2 $3";
    nationalFormat.setFormat(nationalPattern);

    assertFalse(BuildMetadataFromXml.loadInternationalFormat(metadata, numberFormatElement,
                                                             nationalFormat.build()));
    assertEquals(nationalPattern, metadata.getIntlNumberFormat(0).getFormat());
  }

  public void testLoadInternationalFormatCopiesNationalFormatData()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<numberFormat></numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    NumberFormat.Builder nationalFormat = NumberFormat.newBuilder();
    nationalFormat.setFormat("$1-$2");
    nationalFormat.setNationalPrefixOptionalWhenFormatting(true);

    assertFalse(BuildMetadataFromXml.loadInternationalFormat(metadata, numberFormatElement,
                                                             nationalFormat.build()));
    assertTrue(metadata.getIntlNumberFormat(0).getNationalPrefixOptionalWhenFormatting());
  }

  public void testLoadNationalFormat()
      throws ParserConfigurationException, SAXException, IOException {
    String nationalFormat = "$1 $2";
    String xmlInput = String.format("<numberFormat><format>%s</format></numberFormat>",
                                    nationalFormat);
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    NumberFormat.Builder numberFormat = NumberFormat.newBuilder();
    BuildMetadataFromXml.loadNationalFormat(metadata, numberFormatElement, numberFormat);
    assertEquals(nationalFormat, numberFormat.getFormat());
  }

  public void testLoadNationalFormatRequiresFormat()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<numberFormat></numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    NumberFormat.Builder numberFormat = NumberFormat.newBuilder();

    try {
      BuildMetadataFromXml.loadNationalFormat(metadata, numberFormatElement, numberFormat);
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }
  }

  public void testLoadNationalFormatExpectsExactlyOneFormat()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<numberFormat><format/><format/></numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    NumberFormat.Builder numberFormat = NumberFormat.newBuilder();

    try {
      BuildMetadataFromXml.loadNationalFormat(metadata, numberFormatElement, numberFormat);
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }
  }

  // Tests loadAvailableFormats().
  public void testLoadAvailableFormats()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory>"
        + "  <availableFormats>"
        + "    <numberFormat nationalPrefixFormattingRule='($FG)'"
        + "                  carrierCodeFormattingRule='$NP $CC ($FG)'>"
        + "      <format>$1 $2 $3</format>"
        + "    </numberFormat>"
        + "  </availableFormats>"
        + "</territory>";
    Element element = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    BuildMetadataFromXml.loadAvailableFormats(
        metadata, element, "0", "", false /* NP not optional */);
    assertEquals("($1)", metadata.getNumberFormat(0).getNationalPrefixFormattingRule());
    assertEquals("0 $CC ($1)", metadata.getNumberFormat(0).getDomesticCarrierCodeFormattingRule());
    assertEquals("$1 $2 $3", metadata.getNumberFormat(0).getFormat());
  }

  public void testLoadAvailableFormatsPropagatesCarrierCodeFormattingRule()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput =
        "<territory carrierCodeFormattingRule='$NP $CC ($FG)'>"
        + "  <availableFormats>"
        + "    <numberFormat nationalPrefixFormattingRule='($FG)'>"
        + "      <format>$1 $2 $3</format>"
        + "    </numberFormat>"
        + "  </availableFormats>"
        + "</territory>";
    Element element = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    BuildMetadataFromXml.loadAvailableFormats(
        metadata, element, "0", "", false /* NP not optional */);
    assertEquals("($1)", metadata.getNumberFormat(0).getNationalPrefixFormattingRule());
    assertEquals("0 $CC ($1)", metadata.getNumberFormat(0).getDomesticCarrierCodeFormattingRule());
    assertEquals("$1 $2 $3", metadata.getNumberFormat(0).getFormat());
  }

  public void testLoadAvailableFormatsSetsProvidedNationalPrefixFormattingRule()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory>"
        + "  <availableFormats>"
        + "    <numberFormat><format>$1 $2 $3</format></numberFormat>"
        + "  </availableFormats>"
        + "</territory>";
    Element element = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    BuildMetadataFromXml.loadAvailableFormats(
        metadata, element, "", "($1)", false /* NP not optional */);
    assertEquals("($1)", metadata.getNumberFormat(0).getNationalPrefixFormattingRule());
  }

  public void testLoadAvailableFormatsClearsIntlFormat()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory>"
        + "  <availableFormats>"
        + "    <numberFormat><format>$1 $2 $3</format></numberFormat>"
        + "  </availableFormats>"
        + "</territory>";
    Element element = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    BuildMetadataFromXml.loadAvailableFormats(
        metadata, element, "0", "($1)", false /* NP not optional */);
    assertEquals(0, metadata.getIntlNumberFormatCount());
  }

  public void testLoadAvailableFormatsHandlesMultipleNumberFormats()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory>"
        + "  <availableFormats>"
        + "    <numberFormat><format>$1 $2 $3</format></numberFormat>"
        + "    <numberFormat><format>$1-$2</format></numberFormat>"
        + "  </availableFormats>"
        + "</territory>";
    Element element = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    BuildMetadataFromXml.loadAvailableFormats(
        metadata, element, "0", "($1)", false /* NP not optional */);
    assertEquals("$1 $2 $3", metadata.getNumberFormat(0).getFormat());
    assertEquals("$1-$2", metadata.getNumberFormat(1).getFormat());
  }

  public void testLoadInternationalFormatDoesNotSetIntlFormatWhenNA()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<numberFormat><intlFormat>NA</intlFormat></numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    NumberFormat.Builder nationalFormat = NumberFormat.newBuilder();
    nationalFormat.setFormat("$1 $2");

    BuildMetadataFromXml.loadInternationalFormat(metadata, numberFormatElement,
                                                 nationalFormat.build());
    assertEquals(0, metadata.getIntlNumberFormatCount());
  }

  // Tests setLeadingDigitsPatterns().
  public void testSetLeadingDigitsPatterns()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<numberFormat>"
        + "<leadingDigits>1</leadingDigits><leadingDigits>2</leadingDigits>"
        + "</numberFormat>";
    Element numberFormatElement = parseXmlString(xmlInput);
    NumberFormat.Builder numberFormat = NumberFormat.newBuilder();
    BuildMetadataFromXml.setLeadingDigitsPatterns(numberFormatElement, numberFormat);

    assertEquals("1", numberFormat.getLeadingDigitsPattern(0));
    assertEquals("2", numberFormat.getLeadingDigitsPattern(1));
  }

  // Tests setLeadingDigitsPatterns() in the case of international and national formatting rules
  // being present but not both defined for this numberFormat - we don't want to add them twice.
  public void testSetLeadingDigitsPatternsNotAddedTwiceWhenInternationalFormatsPresent()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<availableFormats>"
        + "  <numberFormat pattern=\"(1)(\\d{3})\">"
        + "    <leadingDigits>1</leadingDigits>"
        + "    <format>$1</format>"
        + "  </numberFormat>"
        + "  <numberFormat pattern=\"(2)(\\d{3})\">"
        + "    <leadingDigits>2</leadingDigits>"
        + "    <format>$1</format>"
        + "    <intlFormat>9-$1</intlFormat>"
        + "  </numberFormat>"
        + "</availableFormats>";
    Element element = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    BuildMetadataFromXml.loadAvailableFormats(
        metadata, element, "0", "", false /* NP not optional */);
    assertEquals(1, metadata.getNumberFormat(0).leadingDigitsPatternSize());
    assertEquals(1, metadata.getNumberFormat(1).leadingDigitsPatternSize());
    // When we merge the national format rules into the international format rules, we shouldn't add
    // the leading digit patterns multiple times.
    assertEquals(1, metadata.getIntlNumberFormat(0).leadingDigitsPatternSize());
    assertEquals(1, metadata.getIntlNumberFormat(1).leadingDigitsPatternSize());
  }

  // Tests getNationalPrefixFormattingRuleFromElement().
  public void testGetNationalPrefixFormattingRuleFromElement()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory nationalPrefixFormattingRule='$NP$FG'/>";
    Element element = parseXmlString(xmlInput);
    assertEquals("0$1",
                 BuildMetadataFromXml.getNationalPrefixFormattingRuleFromElement(element, "0"));
  }

  // Tests getDomesticCarrierCodeFormattingRuleFromElement().
  public void testGetDomesticCarrierCodeFormattingRuleFromElement()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory carrierCodeFormattingRule='$NP$CC $FG'/>";
    Element element = parseXmlString(xmlInput);
    assertEquals("0$CC $1",
                 BuildMetadataFromXml.getDomesticCarrierCodeFormattingRuleFromElement(element,
                                                                                      "0"));
  }

  // Tests processPhoneNumberDescElement().
  public void testProcessPhoneNumberDescElementWithInvalidInput()
      throws ParserConfigurationException, SAXException, IOException {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    Element territoryElement = parseXmlString("<territory/>");
    PhoneNumberDesc.Builder phoneNumberDesc;

    phoneNumberDesc = BuildMetadataFromXml.processPhoneNumberDescElement(
        generalDesc, territoryElement, "invalidType");
    assertFalse(phoneNumberDesc.hasNationalNumberPattern());
  }

  public void testProcessPhoneNumberDescElementOverridesGeneralDesc()
      throws ParserConfigurationException, SAXException, IOException {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.setNationalNumberPattern("\\d{8}");
    String xmlInput = "<territory><fixedLine>"
        + "  <nationalNumberPattern>\\d{6}</nationalNumberPattern>"
        + "</fixedLine></territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneNumberDesc.Builder phoneNumberDesc;

    phoneNumberDesc = BuildMetadataFromXml.processPhoneNumberDescElement(
        generalDesc, territoryElement, "fixedLine");
    assertEquals("\\d{6}", phoneNumberDesc.getNationalNumberPattern());
  }

  public void testBuildPhoneMetadataCollection_liteBuild() throws Exception {
    String xmlInput =
        "<phoneNumberMetadata>"
        + "  <territories>"
        + "    <territory id=\"AM\" countryCode=\"374\" internationalPrefix=\"00\">"
        + "      <generalDesc>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "      </generalDesc>"
        + "      <fixedLine>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "        <possibleLengths national=\"8\" localOnly=\"5,6\"/>"
        + "        <exampleNumber>10123456</exampleNumber>"
        + "      </fixedLine>"
        + "      <mobile>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "        <possibleLengths national=\"8\" localOnly=\"5,6\"/>"
        + "        <exampleNumber>10123456</exampleNumber>"
        + "      </mobile>"
        + "    </territory>"
        + "  </territories>"
        + "</phoneNumberMetadata>";
    Document document = parseXmlString(xmlInput).getOwnerDocument();

    PhoneMetadataCollection metadataCollection = BuildMetadataFromXml.buildPhoneMetadataCollection(
        document,
        true,  // liteBuild
        false,  // specialBuild
        false,  // isShortNumberMetadata
        false);  // isAlternateFormatsMetadata

    assertTrue(metadataCollection.getMetadataCount() == 1);
    PhoneMetadata metadata = metadataCollection.getMetadataList().get(0);
    assertTrue(metadata.hasGeneralDesc());
    assertFalse(metadata.getGeneralDesc().hasExampleNumber());
    // Some Phonemetadata.java implementations may have custom logic, so we ensure this
    // implementation is doing the right thing by checking the value of the example number even when
    // hasExampleNumber is false.
    assertEquals("", metadata.getGeneralDesc().getExampleNumber());
    assertTrue(metadata.hasFixedLine());
    assertFalse(metadata.getFixedLine().hasExampleNumber());
    assertEquals("", metadata.getFixedLine().getExampleNumber());
    assertTrue(metadata.hasMobile());
    assertFalse(metadata.getMobile().hasExampleNumber());
    assertEquals("", metadata.getMobile().getExampleNumber());
  }

  public void testBuildPhoneMetadataCollection_specialBuild() throws Exception {
    String xmlInput =
        "<phoneNumberMetadata>"
        + "  <territories>"
        + "    <territory id=\"AM\" countryCode=\"374\" internationalPrefix=\"00\">"
        + "      <generalDesc>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "      </generalDesc>"
        + "      <fixedLine>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "        <possibleLengths national=\"8\" localOnly=\"5,6\"/>"
        + "        <exampleNumber>10123456</exampleNumber>"
        + "      </fixedLine>"
        + "      <mobile>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "        <possibleLengths national=\"8\" localOnly=\"5,6\"/>"
        + "        <exampleNumber>10123456</exampleNumber>"
        + "      </mobile>"
        + "    </territory>"
        + "  </territories>"
        + "</phoneNumberMetadata>";
    Document document = parseXmlString(xmlInput).getOwnerDocument();

    PhoneMetadataCollection metadataCollection = BuildMetadataFromXml.buildPhoneMetadataCollection(
        document,
        false,  // liteBuild
        true,  // specialBuild
        false,  // isShortNumberMetadata
        false);  // isAlternateFormatsMetadata

    assertTrue(metadataCollection.getMetadataCount() == 1);
    PhoneMetadata metadata = metadataCollection.getMetadataList().get(0);
    assertTrue(metadata.hasGeneralDesc());
    assertFalse(metadata.getGeneralDesc().hasExampleNumber());
    // Some Phonemetadata.java implementations may have custom logic, so we ensure this
    // implementation is doing the right thing by checking the value of the example number even when
    // hasExampleNumber is false.
    assertEquals("", metadata.getGeneralDesc().getExampleNumber());
    // TODO: Consider clearing fixed-line if empty after being filtered.
    assertTrue(metadata.hasFixedLine());
    assertFalse(metadata.getFixedLine().hasExampleNumber());
    assertEquals("", metadata.getFixedLine().getExampleNumber());
    assertTrue(metadata.hasMobile());
    assertTrue(metadata.getMobile().hasExampleNumber());
    assertEquals("10123456", metadata.getMobile().getExampleNumber());
  }

  public void testBuildPhoneMetadataCollection_fullBuild() throws Exception {
    String xmlInput =
        "<phoneNumberMetadata>"
        + "  <territories>"
        + "    <territory id=\"AM\" countryCode=\"374\" internationalPrefix=\"00\">"
        + "      <generalDesc>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "      </generalDesc>"
        + "      <fixedLine>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "        <possibleLengths national=\"8\" localOnly=\"5,6\"/>"
        + "        <exampleNumber>10123456</exampleNumber>"
        + "      </fixedLine>"
        + "      <mobile>"
        + "        <nationalNumberPattern>[1-9]\\d{7}</nationalNumberPattern>"
        + "        <possibleLengths national=\"8\" localOnly=\"5,6\"/>"
        + "        <exampleNumber>10123456</exampleNumber>"
        + "      </mobile>"
        + "    </territory>"
        + "  </territories>"
        + "</phoneNumberMetadata>";
    Document document = parseXmlString(xmlInput).getOwnerDocument();

    PhoneMetadataCollection metadataCollection = BuildMetadataFromXml.buildPhoneMetadataCollection(
        document,
        false,  // liteBuild
        false,  // specialBuild
        false,  // isShortNumberMetadata
        false);  // isAlternateFormatsMetadata

    assertTrue(metadataCollection.getMetadataCount() == 1);
    PhoneMetadata metadata = metadataCollection.getMetadataList().get(0);
    assertTrue(metadata.hasGeneralDesc());
    assertFalse(metadata.getGeneralDesc().hasExampleNumber());
    // Some Phonemetadata.java implementations may have custom logic, so we ensure this
    // implementation is doing the right thing by checking the value of the example number even when
    // hasExampleNumber is false.
    assertEquals("", metadata.getGeneralDesc().getExampleNumber());
    assertTrue(metadata.hasFixedLine());
    assertTrue(metadata.getFixedLine().hasExampleNumber());
    assertEquals("10123456", metadata.getFixedLine().getExampleNumber());
    assertTrue(metadata.hasMobile());
    assertTrue(metadata.getMobile().hasExampleNumber());
    assertEquals("10123456", metadata.getMobile().getExampleNumber());
  }

  public void testProcessPhoneNumberDescOutputsExampleNumberByDefault()
      throws ParserConfigurationException, SAXException, IOException {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    String xmlInput = "<territory><fixedLine>"
        + "  <exampleNumber>01 01 01 01</exampleNumber>"
        + "</fixedLine></territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneNumberDesc.Builder phoneNumberDesc;

    phoneNumberDesc = BuildMetadataFromXml.processPhoneNumberDescElement(
        generalDesc, territoryElement, "fixedLine");
    assertEquals("01 01 01 01", phoneNumberDesc.getExampleNumber());
  }

  public void testProcessPhoneNumberDescRemovesWhiteSpacesInPatterns()
      throws ParserConfigurationException, SAXException, IOException {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    String xmlInput = "<territory><fixedLine>"
        + "  <nationalNumberPattern>\t \\d { 6 } </nationalNumberPattern>"
        + "</fixedLine></territory>";
    Element countryElement = parseXmlString(xmlInput);
    PhoneNumberDesc.Builder phoneNumberDesc = BuildMetadataFromXml.processPhoneNumberDescElement(
        generalDesc, countryElement, "fixedLine");
    assertEquals("\\d{6}", phoneNumberDesc.getNationalNumberPattern());
  }

  // Tests setRelevantDescPatterns().
  public void testSetRelevantDescPatternsSetsSameMobileAndFixedLinePattern()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory countryCode=\"33\">"
        + "  <fixedLine><nationalNumberPattern>\\d{6}</nationalNumberPattern></fixedLine>"
        + "  <mobile><nationalNumberPattern>\\d{6}</nationalNumberPattern></mobile>"
        + "</territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    // Should set sameMobileAndFixedPattern to true.
    BuildMetadataFromXml.setRelevantDescPatterns(metadata, territoryElement,
        false /* isShortNumberMetadata */);
    assertTrue(metadata.getSameMobileAndFixedLinePattern());
  }

  public void testSetRelevantDescPatternsSetsAllDescriptionsForRegularLengthNumbers()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory countryCode=\"33\">"
        + "  <fixedLine><nationalNumberPattern>\\d{1}</nationalNumberPattern></fixedLine>"
        + "  <mobile><nationalNumberPattern>\\d{2}</nationalNumberPattern></mobile>"
        + "  <pager><nationalNumberPattern>\\d{3}</nationalNumberPattern></pager>"
        + "  <tollFree><nationalNumberPattern>\\d{4}</nationalNumberPattern></tollFree>"
        + "  <premiumRate><nationalNumberPattern>\\d{5}</nationalNumberPattern></premiumRate>"
        + "  <sharedCost><nationalNumberPattern>\\d{6}</nationalNumberPattern></sharedCost>"
        + "  <personalNumber><nationalNumberPattern>\\d{7}</nationalNumberPattern></personalNumber>"
        + "  <voip><nationalNumberPattern>\\d{8}</nationalNumberPattern></voip>"
        + "  <uan><nationalNumberPattern>\\d{9}</nationalNumberPattern></uan>"
        + "</territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    BuildMetadataFromXml.setRelevantDescPatterns(metadata, territoryElement,
        false /* isShortNumberMetadata */);
    assertEquals("\\d{1}", metadata.getFixedLine().getNationalNumberPattern());
    assertEquals("\\d{2}", metadata.getMobile().getNationalNumberPattern());
    assertEquals("\\d{3}", metadata.getPager().getNationalNumberPattern());
    assertEquals("\\d{4}", metadata.getTollFree().getNationalNumberPattern());
    assertEquals("\\d{5}", metadata.getPremiumRate().getNationalNumberPattern());
    assertEquals("\\d{6}", metadata.getSharedCost().getNationalNumberPattern());
    assertEquals("\\d{7}", metadata.getPersonalNumber().getNationalNumberPattern());
    assertEquals("\\d{8}", metadata.getVoip().getNationalNumberPattern());
    assertEquals("\\d{9}", metadata.getUan().getNationalNumberPattern());
  }

  public void testSetRelevantDescPatternsSetsAllDescriptionsForShortNumbers()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory ID=\"FR\">"
        + "  <tollFree><nationalNumberPattern>\\d{1}</nationalNumberPattern></tollFree>"
        + "  <standardRate><nationalNumberPattern>\\d{2}</nationalNumberPattern></standardRate>"
        + "  <premiumRate><nationalNumberPattern>\\d{3}</nationalNumberPattern></premiumRate>"
        + "  <shortCode><nationalNumberPattern>\\d{4}</nationalNumberPattern></shortCode>"
        + "  <carrierSpecific>"
        + "    <nationalNumberPattern>\\d{5}</nationalNumberPattern>"
        + "  </carrierSpecific>"
        + "  <smsServices>"
        + "    <nationalNumberPattern>\\d{6}</nationalNumberPattern>"
        + "  </smsServices>"
        + "</territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    BuildMetadataFromXml.setRelevantDescPatterns(metadata, territoryElement,
        true /* isShortNumberMetadata */);
    assertEquals("\\d{1}", metadata.getTollFree().getNationalNumberPattern());
    assertEquals("\\d{2}", metadata.getStandardRate().getNationalNumberPattern());
    assertEquals("\\d{3}", metadata.getPremiumRate().getNationalNumberPattern());
    assertEquals("\\d{4}", metadata.getShortCode().getNationalNumberPattern());
    assertEquals("\\d{5}", metadata.getCarrierSpecific().getNationalNumberPattern());
    assertEquals("\\d{6}", metadata.getSmsServices().getNationalNumberPattern());
  }

  public void testSetRelevantDescPatternsThrowsErrorIfTypePresentMultipleTimes()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory countryCode=\"33\">"
        + "  <fixedLine><nationalNumberPattern>\\d{6}</nationalNumberPattern></fixedLine>"
        + "  <fixedLine><nationalNumberPattern>\\d{6}</nationalNumberPattern></fixedLine>"
        + "</territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    try {
      BuildMetadataFromXml.setRelevantDescPatterns(metadata, territoryElement,
          false /* isShortNumberMetadata */);
      fail("Fixed-line info present twice for France: we should fail.");
    } catch (RuntimeException expected) {
      assertEquals("Multiple elements with type fixedLine found.", expected.getMessage());
    }
  }

  public void testAlternateFormatsOmitsDescPatterns()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory countryCode=\"33\">"
        + "  <availableFormats>"
        + "    <numberFormat pattern=\"(1)(\\d{3})\">"
        + "      <leadingDigits>1</leadingDigits>"
        + "      <format>$1</format>"
        + "    </numberFormat>"
        + "  </availableFormats>"
        + "  <fixedLine><nationalNumberPattern>\\d{1}</nationalNumberPattern></fixedLine>"
        + "  <shortCode><nationalNumberPattern>\\d{2}</nationalNumberPattern></shortCode>"
        + "</territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata metadata = BuildMetadataFromXml.loadCountryMetadata("FR", territoryElement,
        false /* isShortNumberMetadata */, true /* isAlternateFormatsMetadata */).build();
    assertEquals("(1)(\\d{3})", metadata.getNumberFormat(0).getPattern());
    assertEquals("1", metadata.getNumberFormat(0).getLeadingDigitsPattern(0));
    assertEquals("$1", metadata.getNumberFormat(0).getFormat());
    assertFalse(metadata.hasFixedLine());
    assertNull(metadata.getFixedLine());
    assertFalse(metadata.hasShortCode());
    assertNull(metadata.getShortCode());
  }

  public void testNationalPrefixRulesSetCorrectly()
      throws ParserConfigurationException, SAXException, IOException {
    String xmlInput = "<territory countryCode=\"33\" nationalPrefix=\"0\""
        + " nationalPrefixFormattingRule=\"$NP$FG\">"
        + "  <availableFormats>"
        + "    <numberFormat pattern=\"(1)(\\d{3})\" nationalPrefixOptionalWhenFormatting=\"true\">"
        + "      <leadingDigits>1</leadingDigits>"
        + "      <format>$1</format>"
        + "    </numberFormat>"
        + "    <numberFormat pattern=\"(\\d{3})\" nationalPrefixOptionalWhenFormatting=\"false\">"
        + "      <leadingDigits>2</leadingDigits>"
        + "      <format>$1</format>"
        + "    </numberFormat>"
        + "  </availableFormats>"
        + "  <fixedLine><nationalNumberPattern>\\d{1}</nationalNumberPattern></fixedLine>"
        + "</territory>";
    Element territoryElement = parseXmlString(xmlInput);
    PhoneMetadata metadata = BuildMetadataFromXml.loadCountryMetadata("FR", territoryElement,
        false /* isShortNumberMetadata */, true /* isAlternateFormatsMetadata */).build();
    assertTrue(metadata.getNumberFormat(0).getNationalPrefixOptionalWhenFormatting());
    // This is inherited from the territory, with $NP replaced by the actual national prefix, and
    // $FG replaced with $1.
    assertEquals("0$1", metadata.getNumberFormat(0).getNationalPrefixFormattingRule());
    // Here it is explicitly set to false.
    assertFalse(metadata.getNumberFormat(1).getNationalPrefixOptionalWhenFormatting());
  }

  public void testProcessPhoneNumberDescElement_PossibleLengthsSetCorrectly() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    // The number lengths set for the general description must be a super-set of those in the
    // element being parsed.
    generalDesc.addPossibleLength(4);
    generalDesc.addPossibleLength(6);
    generalDesc.addPossibleLength(7);
    generalDesc.addPossibleLength(13);
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        // Sorting will be done when parsing.
        + "  <possibleLengths national=\"13,4\" localOnly=\"6\"/>"
        + "</fixedLine>"
        + "</territory>");

    PhoneNumberDesc.Builder fixedLine;
    PhoneNumberDesc.Builder mobile;

    fixedLine = BuildMetadataFromXml.processPhoneNumberDescElement(
        generalDesc, territoryElement, "fixedLine");
    mobile = BuildMetadataFromXml.processPhoneNumberDescElement(
        generalDesc, territoryElement, "mobile");

    assertEquals(2, fixedLine.getPossibleLengthCount());
    assertEquals(4, fixedLine.getPossibleLength(0));
    assertEquals(13, fixedLine.getPossibleLength(1));
    assertEquals(1, fixedLine.getPossibleLengthLocalOnlyCount());

    // We use [-1] to denote that there are no possible lengths; we don't leave it empty, since for
    // compression reasons, we use the empty list to mean that the generalDesc possible lengths
    // apply.
    assertEquals(1, mobile.getPossibleLengthCount());
    assertEquals(-1, mobile.getPossibleLength(0));
    assertEquals(0, mobile.getPossibleLengthLocalOnlyCount());
  }

  public void testSetPossibleLengthsGeneralDesc_BuiltFromChildElements() throws Exception {
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"13\" localOnly=\"6\"/>"
        + "</fixedLine>"
        + "<mobile>"
        + "  <possibleLengths national=\"15\" localOnly=\"7,13\"/>"
        + "</mobile>"
        + "<tollFree>"
        + "  <possibleLengths national=\"15\"/>"
        + "</tollFree>"
        + "</territory>");
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    BuildMetadataFromXml.setPossibleLengthsGeneralDesc(
        generalDesc, "someId", territoryElement, false /* not short-number metadata */);

    assertEquals(2, generalDesc.getPossibleLengthCount());
    assertEquals(13, generalDesc.getPossibleLength(0));
    // 15 is present twice in the input in different sections, but only once in the output.
    assertEquals(15, generalDesc.getPossibleLength(1));
    assertEquals(2, generalDesc.getPossibleLengthLocalOnlyCount());
    assertEquals(6, generalDesc.getPossibleLengthLocalOnly(0));
    assertEquals(7, generalDesc.getPossibleLengthLocalOnly(1));
    // 13 is skipped as a "local only" length, since it is also present as a normal length.
  }

  public void testSetPossibleLengthsGeneralDesc_IgnoresNoIntlDialling() throws Exception {
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"13\"/>"
        + "</fixedLine>"
        + "<noInternationalDialling>"
        + "  <possibleLengths national=\"15\"/>"
        + "</noInternationalDialling>"
        + "</territory>");
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    BuildMetadataFromXml.setPossibleLengthsGeneralDesc(
        generalDesc, "someId", territoryElement, false /* not short-number metadata */);

    assertEquals(1, generalDesc.getPossibleLengthCount());
    assertEquals(13, generalDesc.getPossibleLength(0));
    // 15 is skipped because noInternationalDialling should not contribute to the general lengths;
    // it isn't a particular "type" of number per se, it is a property that different types may
    // have.
  }

  public void testSetPossibleLengthsGeneralDesc_ShortNumberMetadata() throws Exception {
    Element territoryElement = parseXmlString("<territory>"
        + "<shortCode>"
        + "  <possibleLengths national=\"6,13\"/>"
        + "</shortCode>"
        + "<carrierSpecific>"
        + "  <possibleLengths national=\"7,13,15\"/>"
        + "</carrierSpecific>"
        + "<tollFree>"
        + "  <possibleLengths national=\"15\"/>"
        + "</tollFree>"
        + "<smsServices>"
        + "  <possibleLengths national=\"5\"/>"
        + "</smsServices>"
        + "</territory>");
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    BuildMetadataFromXml.setPossibleLengthsGeneralDesc(
        generalDesc, "someId", territoryElement, true /* short-number metadata */);

    // All elements other than shortCode are ignored when creating the general desc.
    assertEquals(2, generalDesc.getPossibleLengthCount());
    assertEquals(6, generalDesc.getPossibleLength(0));
    assertEquals(13, generalDesc.getPossibleLength(1));
  }

  public void testSetPossibleLengthsGeneralDesc_ShortNumberMetadataErrorsOnLocalLengths()
      throws Exception {
    Element territoryElement = parseXmlString("<territory>"
        + "<shortCode>"
        + "  <possibleLengths national=\"13\" localOnly=\"6\"/>"
        + "</shortCode>"
        + "</territory>");

    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    try {
      BuildMetadataFromXml.setPossibleLengthsGeneralDesc(
          generalDesc, "someId", territoryElement, true /* short-number metadata */);
      fail();
    } catch (RuntimeException expected) {
      // This should be an error, localOnly is not permitted in short-code metadata.
      assertEquals("Found local-only lengths in short-number metadata", expected.getMessage());
    }
  }

  public void testProcessPhoneNumberDescElement_ErrorDuplicates() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(6);

    Element territoryElement = parseXmlString("<territory>"
        + "<mobile>"
        + "  <possibleLengths national=\"6,6\"/>"
        + "</mobile>"
        + "</territory>");

    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(generalDesc, territoryElement, "mobile");
      fail("Invalid data seen: expected failure.");
    } catch (RuntimeException expected) {
      // This should be an error, 6 is seen twice.
      assertEquals("Duplicate length element found (6) in possibleLength string 6,6",
          expected.getMessage());
    }
  }

  public void testProcessPhoneNumberDescElement_ErrorDuplicatesOneLocal() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(6);

    Element territoryElement = parseXmlString("<territory>"
        + "<mobile>"
        + "  <possibleLengths national=\"6\" localOnly=\"6\"/>"
        + "</mobile>"
        + "</territory>");

    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(generalDesc, territoryElement, "mobile");
      fail("Invalid data seen: expected failure.");
    } catch (RuntimeException expected) {
      // This should be an error, 6 is seen twice.
      assertEquals("Possible length(s) found specified as a normal and local-only length: [6]",
          expected.getMessage());
    }
  }

  public void testProcessPhoneNumberDescElement_ErrorUncoveredLengths() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(4);
    Element territoryElement = parseXmlString("<territory>"
        + "<noInternationalDialling>"
        // Sorting will be done when parsing.
        + "  <possibleLengths national=\"6,7,4\"/>"
        + "</noInternationalDialling>"
        + "</territory>");
    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(
          generalDesc, territoryElement, "noInternationalDialling");
      fail("Lengths present not covered by the general desc: should fail.");
    } catch (RuntimeException expected) {
      // Lengths were present that the general description didn't know about.
      assertTrue(expected.getMessage().contains("Out-of-range possible length"));
    }
  }

  public void testProcessPhoneNumberDescElement_SameAsParent() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    // The number lengths set for the general description must be a super-set of those in the
    // element being parsed.
    generalDesc.addPossibleLength(4);
    generalDesc.addPossibleLength(6);
    generalDesc.addPossibleLength(7);
    generalDesc.addPossibleLengthLocalOnly(2);
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        // Sorting will be done when parsing.
        + "  <possibleLengths national=\"6,7,4\" localOnly=\"2\"/>"
        + "</fixedLine>"
        + "</territory>");

    PhoneNumberDesc.Builder phoneNumberDesc = BuildMetadataFromXml.processPhoneNumberDescElement(
        generalDesc, territoryElement, "fixedLine");
    // No possible lengths should be present, because they match the general description.
    assertEquals(0, phoneNumberDesc.getPossibleLengthCount());
    // Local-only lengths should be present for child elements such as fixed-line.
    assertEquals(1, phoneNumberDesc.getPossibleLengthLocalOnlyCount());
  }

  public void testProcessPhoneNumberDescElement_InvalidNumber() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(4);
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"4d\"/>"
        + "</fixedLine>"
        + "</territory>");

    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(
          generalDesc, territoryElement, "fixedLine");
      fail("4d is not a number.");
    } catch (NumberFormatException expected) {
      assertEquals("For input string: \"4d\"", expected.getMessage());
    }
  }

  public void testLoadCountryMetadata_GeneralDescHasNumberLengthsSet() throws Exception {
    Element territoryElement = parseXmlString("<territory>"
        + "<generalDesc>"
        // This shouldn't be set, the possible lengths should be derived for generalDesc.
        + "  <possibleLengths national=\"4\"/>"
        + "</generalDesc>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"4\"/>"
        + "</fixedLine>"
        + "</territory>");

    try {
      BuildMetadataFromXml.loadCountryMetadata("FR", territoryElement,
          false /* isShortNumberMetadata */, false /* isAlternateFormatsMetadata */);
      fail("Possible lengths explicitly set for generalDesc and should not be: we should fail.");
    } catch (RuntimeException expected) {
      assertEquals("Found possible lengths specified at general desc: this should be derived"
          + " from child elements. Affected country: FR", expected.getMessage());
    }
  }

  public void testProcessPhoneNumberDescElement_ErrorEmptyPossibleLengthStringAttribute()
      throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(4);
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"\"/>"
        + "</fixedLine>"
        + "</territory>");
    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(
          generalDesc, territoryElement, "fixedLine");
      fail("Empty possible length string.");
    } catch (RuntimeException expected) {
      assertEquals("Empty possibleLength string found.", expected.getMessage());
    }
  }

  public void testProcessPhoneNumberDescElement_ErrorRangeSpecifiedWithComma()
      throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(4);
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"[4,7]\"/>"
        + "</fixedLine>"
        + "</territory>");
    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(
          generalDesc, territoryElement, "fixedLine");
      fail("Ranges shouldn't use a comma.");
    } catch (RuntimeException expected) {
      assertEquals("Missing end of range character in possible length string [4,7].",
          expected.getMessage());
    }
  }

  public void testProcessPhoneNumberDescElement_ErrorIncompleteRange() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(4);
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"[4-\"/>"
        + "</fixedLine>"
        + "</territory>");

    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(
          generalDesc, territoryElement, "fixedLine");
      fail("Should fail: range incomplete.");
    } catch (RuntimeException expected) {
      assertEquals("Missing end of range character in possible length string [4-.",
          expected.getMessage());
    }
  }

  public void testProcessPhoneNumberDescElement_ErrorNoDashInRange() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(4);
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"[4:10]\"/>"
        + "</fixedLine>"
        + "</territory>");

    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(
          generalDesc, territoryElement, "fixedLine");
      fail("Should fail: range incomplete.");
    } catch (RuntimeException expected) {
      assertEquals("Ranges must have exactly one - character: missing for [4:10].",
          expected.getMessage());
    }
  }

  public void testProcessPhoneNumberDescElement_ErrorRangeIsNotFromMinToMax() throws Exception {
    PhoneNumberDesc.Builder generalDesc = PhoneNumberDesc.newBuilder();
    generalDesc.addPossibleLength(4);
    Element territoryElement = parseXmlString("<territory>"
        + "<fixedLine>"
        + "  <possibleLengths national=\"[10-10]\"/>"
        + "</fixedLine>"
        + "</territory>");

    try {
      BuildMetadataFromXml.processPhoneNumberDescElement(
          generalDesc, territoryElement, "fixedLine");
      fail("Should fail: range even.");
    } catch (RuntimeException expected) {
      assertEquals("The first number in a range should be two or more digits lower than the second."
          + " Culprit possibleLength string: [10-10]", expected.getMessage());
    }
  }

  public void testGetMetadataFilter() {
    assertEquals(BuildMetadataFromXml.getMetadataFilter(false, false),
        MetadataFilter.emptyFilter());
    assertEquals(BuildMetadataFromXml.getMetadataFilter(true, false),
        MetadataFilter.forLiteBuild());
    assertEquals(BuildMetadataFromXml.getMetadataFilter(false, true),
        MetadataFilter.forSpecialBuild());
    try {
      BuildMetadataFromXml.getMetadataFilter(true, true);
      fail("getMetadataFilter should fail when liteBuild and specialBuild are both set");
    } catch (RuntimeException e) {
      // Test passed.
    }
  }
}
