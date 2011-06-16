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

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

/**
 * Tool to convert phone number metadata from the XML format to protocol buffer format.
 *
 * @author Shaopeng Jia
 */
public class BuildMetadataProtoFromXml extends Command {
  private static final String PACKAGE_NAME = "com/google/i18n/phonenumbers";
  private static final String META_DATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProto";
  private static final String TEST_META_DATA_FILE_PREFIX =
  "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting";
  private static final String TEST_COUNTRY_CODE_TO_REGION_CODE_MAP_CLASS_NAME =
      "CountryCodeToRegionCodeMapForTesting";
  private static final String COUNTRY_CODE_TO_REGION_CODE_MAP_CLASS_NAME =
      "CountryCodeToRegionCodeMap";

  private static final String HELP_MESSAGE =
      "Usage:\n" +
      "BuildMetadataProtoFromXml <inputFile> <outputDir> <forTesting> [<liteBuild>]\n" +
      "\n" +
      "where:\n" +
      "  inputFile    The input file containing phone number metadata in XML format.\n" +
      "  outputDir    The output directory to store phone number metadata in proto\n" +
      "               format (one file per region) and the country code to region code\n" +
      "               mapping file.\n" +
      "  forTesting   Flag whether to generate metadata for testing purposes or not.\n" +
      "  liteBuild    Whether to generate the lite-version of the metadata (default:\n" +
      "               false). When set to true certain metadata will be omitted.\n" +
      "               At this moment, example numbers information is omitted.\n" +
      "\n" +
      "Metadata will be stored in:\n" +
      "  <outputDir>" + META_DATA_FILE_PREFIX + "_*\n" +
      "Mapping file will be stored in:\n" +
      "  <outputDir>/" + PACKAGE_NAME + "/" +
          COUNTRY_CODE_TO_REGION_CODE_MAP_CLASS_NAME + ".java\n" +
      "\n" +
      "Example command line invocation:\n" +
      "BuildMetadataProtoFromXml PhoneNumberMetadata.xml src false false\n";

  @Override
  public String getCommandName() {
    return "BuildMetadataProtoFromXml";
  }

  @Override
  public boolean start() {
    String[] args = getArgs();
    if (args.length != 4 && args.length != 5) {
      System.err.println(HELP_MESSAGE);
      return false;
    }
    String inputFile = args[1];
    String outputDir = args[2];
    boolean forTesting = args[3].equals("true");
    boolean liteBuild = args.length > 4 && args[4].equals("true");

    String filePrefix;
    if (forTesting) {
      filePrefix = outputDir + TEST_META_DATA_FILE_PREFIX;
    } else {
      filePrefix = outputDir + META_DATA_FILE_PREFIX;
    }

    try {
      PhoneMetadataCollection metadataCollection =
          BuildMetadataFromXml.buildPhoneMetadataCollection(inputFile, liteBuild);

      for (PhoneMetadata metadata : metadataCollection.getMetadataList()) {
        String regionCode = metadata.getId();
        PhoneMetadataCollection outMetadataCollection = new PhoneMetadataCollection();
        outMetadataCollection.addMetadata(metadata);
        FileOutputStream outputForRegion = new FileOutputStream(filePrefix + "_" + regionCode);
        ObjectOutputStream out = new ObjectOutputStream(outputForRegion);
        outMetadataCollection.writeExternal(out);
        out.close();
      }

      Map<Integer, List<String>> countryCodeToRegionCodeMap =
          BuildMetadataFromXml.buildCountryCodeToRegionCodeMap(metadataCollection);

      writeCountryCallingCodeMappingToJavaFile(countryCodeToRegionCodeMap, outputDir, forTesting);
    } catch (Exception e) {
      System.err.println(HELP_MESSAGE);
      return false;
    }
    return true;
  }

  private static final String MAPPING_IMPORTS =
      "import java.util.ArrayList;\n" +
      "import java.util.HashMap;\n" +
      "import java.util.List;\n" +
      "import java.util.Map;\n";
  private static final String MAPPING_COMMENT =
      "  // A mapping from a country code to the region codes which denote the\n" +
      "  // country/region represented by that country code. In the case of multiple\n" +
      "  // countries sharing a calling code, such as the NANPA countries, the one\n" +
      "  // indicated with \"isMainCountryForCode\" in the metadata should be first.\n";
  private static final double MAPPING_LOAD_FACTOR = 0.75;
  private static final String MAPPING_COMMENT_2 =
      "    // The capacity is set to %d as there are %d different country codes,\n" +
      "    // and this offers a load factor of roughly " + MAPPING_LOAD_FACTOR + ".\n";

  private static void writeCountryCallingCodeMappingToJavaFile(
      Map<Integer, List<String>> countryCodeToRegionCodeMap,
      String outputDir, boolean forTesting) throws IOException {
    String mappingClassName;
    if (forTesting) {
      mappingClassName = TEST_COUNTRY_CODE_TO_REGION_CODE_MAP_CLASS_NAME;
    } else {
      mappingClassName = COUNTRY_CODE_TO_REGION_CODE_MAP_CLASS_NAME;
    }
    String mappingFile =
        outputDir + "/" + PACKAGE_NAME.replaceAll("\\.", "/") + "/" + mappingClassName + ".java";
    int capacity = (int) (countryCodeToRegionCodeMap.size() / MAPPING_LOAD_FACTOR);

    BufferedWriter writer = new BufferedWriter(new FileWriter(mappingFile));

    writer.write(CopyrightNotice.TEXT);
    if (PACKAGE_NAME.length() > 0) {
      writer.write("package " + PACKAGE_NAME + ";\n\n");
    }
    writer.write(MAPPING_IMPORTS);
    writer.write("\n");
    writer.write("public class " + mappingClassName + " {\n");
    writer.write(MAPPING_COMMENT);
    writer.write("  static Map<Integer, List<String>> getCountryCodeToRegionCodeMap() {\n");
    Formatter formatter = new Formatter(writer);
    formatter.format(MAPPING_COMMENT_2, capacity, countryCodeToRegionCodeMap.size());
    writer.write("    Map<Integer, List<String>> countryCodeToRegionCodeMap =\n");
    writer.write("        new HashMap<Integer, List<String>>(" + capacity + ");\n");
    writer.write("\n");
    writer.write("    ArrayList<String> listWithRegionCode;\n");
    writer.write("\n");

    for (Map.Entry<Integer, List<String>> entry : countryCodeToRegionCodeMap.entrySet()) {
      int countryCallingCode = entry.getKey();
      List<String> regionCodes = entry.getValue();
      writer.write("    listWithRegionCode = new ArrayList<String>(" + regionCodes.size() + ");\n");
      for (String regionCode : regionCodes) {
        writer.write("    listWithRegionCode.add(\"" + regionCode + "\");\n");
      }
      writer.write("    countryCodeToRegionCodeMap.put(" + countryCallingCode +
                   ", listWithRegionCode);\n");
      writer.write("\n");
    }

    writer.write("    return countryCodeToRegionCodeMap;\n");
    writer.write("  }\n");
    writer.write("}\n");

    writer.flush();
    writer.close();
  }
}
