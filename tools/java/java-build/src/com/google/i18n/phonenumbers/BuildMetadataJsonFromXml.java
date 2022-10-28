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

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata.NumberFormat;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

/**
 * Tool to convert phone number metadata from the XML format to JSON format.
 *
 * @author Nikolaos Trogkanis
 */
public class BuildMetadataJsonFromXml extends Command {
  private static final String NAMESPACE = "i18n.phonenumbers.metadata";

  private static final String HELP_MESSAGE =
      "Usage:\n" +
      "BuildMetadataJsonFromXml <inputFile> <outputFile> [<liteBuild>] [<namespace>]\n" +
      "\n" +
      "where:\n" +
      "  inputFile    The input file containing phone number metadata in XML format.\n" +
      "  outputFile   The output file to contain phone number metadata in JSON format.\n" +
      "  liteBuild    Whether to generate the lite-version of the metadata (default:\n" +
      "               false). When set to true certain metadata will be omitted.\n" +
      "               At this moment, example numbers information is omitted.\n" +
      "  namespace    If present, the namespace to provide the metadata with (default:\n" +
      "               " + NAMESPACE + ").\n" +
      "\n" +
      "Example command line invocation:\n" +
      "BuildMetadataJsonFromXml PhoneNumberMetadata.xml metadatalite.js true i18n.phonenumbers.testmetadata\n";

  private static final String FILE_OVERVIEW =
      "/**\n"
      + " * @fileoverview Generated metadata for file\n"
      + " * %s\n"
      + " * @author Nikolaos Trogkanis\n"
      + " */\n\n";

  private static final String COUNTRY_CODE_TO_REGION_CODE_MAP_COMMENT =
      "/**\n"
      + " * A mapping from a country calling code to the region codes which denote the\n"
      + " * region represented by that country calling code. In the case of multiple\n"
      + " * countries sharing a calling code, such as the NANPA regions, the one\n"
      + " * indicated with \"isMainCountryForCode\" in the metadata should be first.\n"
      + " * @type {!Object.<number, Array.<string>>}\n"
      + " */\n";

  private static final String COUNTRY_TO_METADATA_COMMENT =
      "/**\n"
      + " * A mapping from a region code to the PhoneMetadata for that region.\n"
      + " * @type {!Object.<string, Array>}\n"
      + " */\n";

  private static final int COPYRIGHT_YEAR = 2010;

  @Override
  public String getCommandName() {
    return "BuildMetadataJsonFromXml";
  }

  @Override
  public boolean start() {
    String[] args = getArgs();

    if (args.length != 3 && args.length != 4 && args.length != 5) {
      System.err.println(HELP_MESSAGE);
      return false;
    }
    String inputFile = args[1];
    String outputFile = args[2];
    boolean liteBuild = args.length > 3 && args[3].equals("true");
    String namespace = args.length > 4 ? args[4] : NAMESPACE;
    return start(inputFile, outputFile, liteBuild, namespace);
  }

  static boolean start(String inputFile, String outputFile, boolean liteBuild) {
    return start(inputFile, outputFile, liteBuild, NAMESPACE);
  }

  static boolean start(String inputFile, String outputFile, boolean liteBuild, String namespace) {
    try {
      PhoneMetadataCollection metadataCollection =
          BuildMetadataFromXml.buildPhoneMetadataCollection(inputFile, liteBuild, false);
      Map<Integer, List<String>> countryCodeToRegionCodeMap =
          BuildMetadataFromXml.buildCountryCodeToRegionCodeMap(metadataCollection);

      BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

      CopyrightNotice.writeTo(writer, COPYRIGHT_YEAR, true);
      Formatter formatter = new Formatter(writer);
      formatter.format(FILE_OVERVIEW, inputFile);

      writer.write("goog.provide('" + namespace + "');\n\n");

      writer.write(COUNTRY_CODE_TO_REGION_CODE_MAP_COMMENT);
      writer.write(namespace + ".countryCodeToRegionCodeMap = ");
      writeCountryCodeToRegionCodeMap(countryCodeToRegionCodeMap, writer);
      writer.write(";\n\n");

      writer.write(COUNTRY_TO_METADATA_COMMENT);
      writer.write(namespace + ".countryToMetadata = ");
      writeCountryToMetadataMap(metadataCollection, writer);
      writer.write(";\n");

      writer.flush();
      writer.close();
      formatter.close();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  // Writes a PhoneMetadataCollection in JSON format.
  private static void writeCountryToMetadataMap(PhoneMetadataCollection metadataCollection,
                                                BufferedWriter writer) throws IOException {
    writer.write("{\n");
    boolean isFirstTimeInLoop = true;
    for (PhoneMetadata metadata : metadataCollection.getMetadataList()) {
      if (isFirstTimeInLoop) {
        isFirstTimeInLoop = false;
      } else {
        writer.write(",");
      }
      String key = metadata.getId();
      // For non-geographical country calling codes (e.g. +800), use the country calling codes
      // instead of the region code as key in the map.
      if (key.equals("001")) {
        key = Integer.toString(metadata.getCountryCode());
      }
      JSArrayBuilder jsArrayBuilder = new JSArrayBuilder();
      toJsArray(metadata, jsArrayBuilder);
      writer.write("\"");
      writer.write(key);
      writer.write("\":");
      writer.write(jsArrayBuilder.toString());
    }
    writer.write("}");
  }

  // Writes a Map<Integer, List<String>> in JSON format.
  private static void writeCountryCodeToRegionCodeMap(
      Map<Integer, List<String>> countryCodeToRegionCodeMap,
      BufferedWriter writer) throws IOException {
    writer.write("{\n");
    boolean isFirstTimeInLoop = true;
    for (Map.Entry<Integer, List<String>> entry : countryCodeToRegionCodeMap.entrySet()) {
      if (isFirstTimeInLoop) {
        isFirstTimeInLoop = false;
      } else {
        writer.write(",");
      }
      writer.write(Integer.toString(entry.getKey()));
      writer.write(":");
      JSArrayBuilder jsArrayBuilder = new JSArrayBuilder();
      jsArrayBuilder.beginArray();
      jsArrayBuilder.appendIterator(entry.getValue().iterator());
      jsArrayBuilder.endArray();
      writer.write(jsArrayBuilder.toString());
    }
    writer.write("}");
  }

  // Converts NumberFormat to JSArray.
  private static void toJsArray(NumberFormat format, JSArrayBuilder jsArrayBuilder) {
    jsArrayBuilder.beginArray();

    // missing 0
    jsArrayBuilder.append(null);
    // required string pattern = 1;
    jsArrayBuilder.append(format.getPattern());
    // required string format = 2;
    jsArrayBuilder.append(format.getFormat());
    // repeated string leading_digits_pattern = 3;
    int leadingDigitsPatternSize = format.leadingDigitsPatternSize();
    if (leadingDigitsPatternSize > 0) {
      jsArrayBuilder.beginArray();
      for (int i = 0; i < leadingDigitsPatternSize; i++) {
        jsArrayBuilder.append(format.getLeadingDigitsPattern(i));
      }
      jsArrayBuilder.endArray();
    } else {
      jsArrayBuilder.append(null);
    }
    // optional string national_prefix_formatting_rule = 4;
    if (format.hasNationalPrefixFormattingRule()) {
      jsArrayBuilder.append(format.getNationalPrefixFormattingRule());
    } else {
      jsArrayBuilder.append(null);
    }
    // optional string domestic_carrier_code_formatting_rule = 5;
    if (format.hasDomesticCarrierCodeFormattingRule()) {
      jsArrayBuilder.append(format.getDomesticCarrierCodeFormattingRule());
    } else {
      jsArrayBuilder.append(null);
    }
    // optional bool national_prefix_optional_when_formatting = 6 [default = false];
    if (format.hasNationalPrefixOptionalWhenFormatting()) {
      jsArrayBuilder.append(format.getNationalPrefixOptionalWhenFormatting());
    } else {
      jsArrayBuilder.append(null);
    }

    jsArrayBuilder.endArray();
  }

  // Converts PhoneNumberDesc to JSArray.
  private static void toJsArray(PhoneNumberDesc desc, JSArrayBuilder jsArrayBuilder) {
    if (desc == null) {
      // Some descriptions are optional; in these cases we just append null and return if they are
      // absent.
      jsArrayBuilder.append(null);
      return;
    }
    jsArrayBuilder.beginArray();

    // missing 0
    jsArrayBuilder.append(null);
    // missing 1
    jsArrayBuilder.append(null);
    // optional string national_number_pattern = 2;
    if (desc.hasNationalNumberPattern()) {
      jsArrayBuilder.append(desc.getNationalNumberPattern());
    } else {
      jsArrayBuilder.append(null);
    }
    // missing 3
    jsArrayBuilder.append(null);
    // missing 4
    jsArrayBuilder.append(null);
    // missing 5
    jsArrayBuilder.append(null);
    // optional string example_number = 6;
    if (desc.hasExampleNumber()) {
      jsArrayBuilder.append(desc.getExampleNumber());
    } else {
      jsArrayBuilder.append(null);
    }
    // missing 7
    jsArrayBuilder.append(null);
    // missing 8
    jsArrayBuilder.append(null);
    // repeated int32 possible_length = 9;
    int possibleLengthSize = desc.getPossibleLengthCount();
    if (possibleLengthSize > 0) {
      jsArrayBuilder.beginArray();
      for (int i = 0; i < possibleLengthSize; i++) {
        jsArrayBuilder.append(desc.getPossibleLength(i));
      }
      jsArrayBuilder.endArray();
    } else {
      jsArrayBuilder.append(null);
    }
    // repeated int32 possible_length = 10;
    int possibleLengthLocalOnlySize = desc.getPossibleLengthLocalOnlyCount();
    if (possibleLengthLocalOnlySize > 0) {
      jsArrayBuilder.beginArray();
      for (int i = 0; i < possibleLengthLocalOnlySize; i++) {
        jsArrayBuilder.append(desc.getPossibleLengthLocalOnly(i));
      }
      jsArrayBuilder.endArray();
    } else {
      jsArrayBuilder.append(null);
    }

    jsArrayBuilder.endArray();
  }

  // Converts PhoneMetadata to JSArray.
  private static void toJsArray(PhoneMetadata metadata, JSArrayBuilder jsArrayBuilder) {
    jsArrayBuilder.beginArray();

    // missing 0
    jsArrayBuilder.append(null);
    // optional PhoneNumberDesc general_desc = 1;
    toJsArray(metadata.getGeneralDesc(), jsArrayBuilder);
    // optional PhoneNumberDesc fixed_line = 2;
    toJsArray(metadata.getFixedLine(), jsArrayBuilder);
    // optional PhoneNumberDesc mobile = 3;
    toJsArray(metadata.getMobile(), jsArrayBuilder);
    // optional PhoneNumberDesc toll_free = 4;
    toJsArray(metadata.getTollFree(), jsArrayBuilder);
    // optional PhoneNumberDesc premium_rate = 5;
    toJsArray(metadata.getPremiumRate(), jsArrayBuilder);
    // optional PhoneNumberDesc shared_cost = 6;
    toJsArray(metadata.getSharedCost(), jsArrayBuilder);
    // optional PhoneNumberDesc personal_number = 7;
    toJsArray(metadata.getPersonalNumber(), jsArrayBuilder);
    // optional PhoneNumberDesc voip = 8;
    toJsArray(metadata.getVoip(), jsArrayBuilder);
    // required string id = 9;
    jsArrayBuilder.append(metadata.getId());
    // optional int32 country_code = 10;
    if (metadata.hasCountryCode()) {
      jsArrayBuilder.append(metadata.getCountryCode());
    } else {
      jsArrayBuilder.append(null);
    }
    // optional string international_prefix = 11;
    if (metadata.hasInternationalPrefix()) {
      jsArrayBuilder.append(metadata.getInternationalPrefix());
    } else {
      jsArrayBuilder.append(null);
    }

    // optional string national_prefix = 12;
    if (metadata.hasNationalPrefix()) {
      jsArrayBuilder.append(metadata.getNationalPrefix());
    } else {
      jsArrayBuilder.append(null);
    }
    // optional string preferred_extn_prefix = 13;
    if (metadata.hasPreferredExtnPrefix()) {
      jsArrayBuilder.append(metadata.getPreferredExtnPrefix());
    } else {
      jsArrayBuilder.append(null);
    }
    // missing 14
    jsArrayBuilder.append(null);
    // optional string national_prefix_for_parsing = 15;
    if (metadata.hasNationalPrefixForParsing()) {
      jsArrayBuilder.append(metadata.getNationalPrefixForParsing());
    } else {
      jsArrayBuilder.append(null);
    }
    // optional string national_prefix_transform_rule = 16;
    if (metadata.hasNationalPrefixTransformRule()) {
      jsArrayBuilder.append(metadata.getNationalPrefixTransformRule());
    } else {
      jsArrayBuilder.append(null);
    }
    // optional string preferred_international_prefix = 17;
    if (metadata.hasPreferredInternationalPrefix()) {
      jsArrayBuilder.append(metadata.getPreferredInternationalPrefix());
    } else {
      jsArrayBuilder.append(null);
    }
    // optional bool same_mobile_and_fixed_line_pattern = 18 [default=false];
    if (metadata.hasSameMobileAndFixedLinePattern()) {
      jsArrayBuilder.append(metadata.getSameMobileAndFixedLinePattern());
    } else {
      jsArrayBuilder.append(null);
    }
    // repeated NumberFormat number_format = 19;
    int numberFormatSize = metadata.numberFormatSize();
    if (numberFormatSize > 0) {
      jsArrayBuilder.beginArray();
      for (int i = 0; i < numberFormatSize; i++) {
        toJsArray(metadata.getNumberFormat(i), jsArrayBuilder);
      }
      jsArrayBuilder.endArray();
    } else {
      jsArrayBuilder.append(null);
    }
    // repeated NumberFormat intl_number_format = 20;
    int intlNumberFormatSize = metadata.intlNumberFormatSize();
    if (intlNumberFormatSize > 0) {
      jsArrayBuilder.beginArray();
      for (int i = 0; i < intlNumberFormatSize; i++) {
        toJsArray(metadata.getIntlNumberFormat(i), jsArrayBuilder);
      }
      jsArrayBuilder.endArray();
    } else {
      jsArrayBuilder.append(null);
    }
    // optional PhoneNumberDesc pager = 21;
    toJsArray(metadata.getPager(), jsArrayBuilder);
    // optional bool main_country_for_code = 22 [default=false];
    if (metadata.isMainCountryForCode()) {
      jsArrayBuilder.append(1);
    } else {
      jsArrayBuilder.append(null);
    }
    // optional string leading_digits = 23;
    if (metadata.hasLeadingDigits()) {
      jsArrayBuilder.append(metadata.getLeadingDigits());
    } else {
      jsArrayBuilder.append(null);
    }
    // optional PhoneNumberDesc no_international_dialling = 24;
    toJsArray(metadata.getNoInternationalDialling(), jsArrayBuilder);
    // optional PhoneNumberDesc uan = 25;
    toJsArray(metadata.getUan(), jsArrayBuilder);
    // missing 26
    jsArrayBuilder.append(null);
    // optional PhoneNumberDesc emergency = 27;
    toJsArray(metadata.getEmergency(), jsArrayBuilder);
    // optional PhoneNumberDesc voicemail = 28;
    toJsArray(metadata.getVoicemail(), jsArrayBuilder);
    // optional PhoneNumberDesc short_code = 29;
    toJsArray(metadata.getShortCode(), jsArrayBuilder);
    // optional PhoneNumberDesc standard_rate = 30;
    toJsArray(metadata.getStandardRate(), jsArrayBuilder);
    // optional PhoneNumberDesc carrier_specific = 31;
    toJsArray(metadata.getCarrierSpecific(), jsArrayBuilder);
    // optional bool mobile_number_portable_region = 32 [default=false];
    // left as null because this data is not used in the current JS API's.
    jsArrayBuilder.append(null);
    // optional PhoneNumberDesc sms_services = 33;
    toJsArray(metadata.getSmsServices(), jsArrayBuilder);

    jsArrayBuilder.endArray();
  }
}
