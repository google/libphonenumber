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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedSet;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Tool to convert phone number metadata from the XML format to protocol buffer format.
 *
 * <p>
 * Based on the name of the {@code inputFile}, some optimization and removal of unnecessary metadata
 * is carried out to reduce the size of the output file.
 *
 * @author Shaopeng Jia
 */
public class BuildMetadataProtoFromXml extends Command {
  private static final String CLASS_NAME = BuildMetadataProtoFromXml.class.getSimpleName();
  private static final String PACKAGE_NAME = BuildMetadataProtoFromXml.class.getPackage().getName();

  // Command line parameter names.
  private static final String INPUT_FILE = "input-file";
  private static final String OUTPUT_DIR = "output-dir";
  private static final String DATA_PREFIX = "data-prefix";
  private static final String MAPPING_CLASS = "mapping-class";
  private static final String COPYRIGHT = "copyright";
  private static final String SINGLE_FILE = "single-file";
  private static final String LITE_BUILD = "lite-build";
  private static final String BUILD_REGIONCODE = "build-regioncode";
  // Only supported for clients who have consulted with the libphonenumber team, and the behavior is
  // subject to change without notice.
  private static final String SPECIAL_BUILD = "special-build";

  private static final String HELP_MESSAGE =
      "Usage: " + CLASS_NAME + " [OPTION]...\n" +
      "\n" +
      "  --" + INPUT_FILE + "=PATH     Read phone number metadata in XML format from PATH.\n" +
      "  --" + OUTPUT_DIR + "=PATH     Use PATH as the root directory for output files.\n" +
      "  --" + DATA_PREFIX +
          "=PATH    Use PATH (relative to " + OUTPUT_DIR + ") as the basename when\n" +
      "                        writing phone number metadata in proto format.\n" +
      "                        One file per region will be written unless " + SINGLE_FILE + "\n" +
      "                        is set, in which case a single file will be written with\n" +
      "                        metadata for all regions.\n" +
      "  --" + MAPPING_CLASS + "=NAME  Store country code mappings in the class NAME, which\n" +
      "                        will be written to a file in " + OUTPUT_DIR + ".\n" +
      "  --" + COPYRIGHT + "=YEAR      Use YEAR in generated copyright headers.\n" +
      "\n" +
      "  [--" + SINGLE_FILE + "=<true|false>] Optional (default: false). Whether to write\n" +
      "                               metadata to a single file, instead of one file\n" +
      "                               per region.\n" +
      "  [--" + LITE_BUILD + "=<true|false>]  Optional (default: false). In a lite build,\n" +
      "                               certain metadata will be omitted. At this\n" +
      "                               moment, example numbers information is omitted.\n" +
      "  [--" + BUILD_REGIONCODE + "=<true|false>]  Optional (default: false). Generate\n" +
      "                               RegionCode class with constants for all region codes.\n" +
      "\n" +
      "Example command line invocation:\n" +
      CLASS_NAME + " \\\n" +
      "  --" + INPUT_FILE + "=resources/PhoneNumberMetadata.xml \\\n" +
      "  --" + OUTPUT_DIR + "=java/libphonenumber/src/com/google/i18n/phonenumbers \\\n" +
      "  --" + DATA_PREFIX + "=data/PhoneNumberMetadataProto \\\n" +
      "  --" + MAPPING_CLASS + "=CountryCodeToRegionCodeMap \\\n" +
      "  --" + COPYRIGHT + "=2010 \\\n" +
      "  --" + SINGLE_FILE + "=false \\\n" +
      "  --" + LITE_BUILD + "=false\n" +
      "  --" + BUILD_REGIONCODE + "=true\n";

  private static final String GENERATION_COMMENT =
      "/* This file is automatically generated by {@link " + CLASS_NAME + "}.\n" +
      " * Please don't modify it directly.\n" +
      " */\n\n";
  
  private static final String MAP_COMMENT =
      "  // A mapping from a country code to the region codes which denote the\n" +
      "  // country/region represented by that country code. In the case of multiple\n" +
      "  // countries sharing a calling code, such as the NANPA countries, the one\n" +
      "  // indicated with \"isMainCountryForCode\" in the metadata should be first.\n";
  private static final String COUNTRY_CODE_SET_COMMENT =
      "  // A set of all country codes for which data is available.\n";
  private static final String REGION_CODE_SET_COMMENT =
      "  // A set of all region codes for which data is available.\n";
  private static final double CAPACITY_FACTOR = 0.75;
  private static final String CAPACITY_COMMENT =
      "    // The capacity is set to %d as there are %d different entries,\n" +
      "    // and this offers a load factor of roughly " + CAPACITY_FACTOR + ".\n";

  private static final String REGION_CODE_CONSTS_JAVADOC =
      "/**\n" +
      " * Class containing string constants of region codes for easier testing.\n" +
      " */\n";

  @Override
  public String getCommandName() {
    return CLASS_NAME;
  }

  @Override
  public boolean start() {
    // The format of a well-formed command line parameter.
    Pattern pattern = Pattern.compile("--(.+?)=(.*)");

    String inputFile = null;
    String outputDir = null;
    String dataPrefix = null;
    String mappingClass = null;
    String copyright = null;
    boolean singleFile = false;
    boolean liteBuild = false;
    boolean specialBuild = false;
    boolean buildRegioncode = false;

    for (int i = 1; i < getArgs().length; i++) {
      String key = null;
      String value = null;
      Matcher matcher = pattern.matcher(getArgs()[i]);
      if (matcher.matches()) {
        key = matcher.group(1);
        value = matcher.group(2);
      }

      if (INPUT_FILE.equals(key)) {
        inputFile = value;
      } else if (OUTPUT_DIR.equals(key)) {
        outputDir = value;
      } else if (DATA_PREFIX.equals(key)) {
        dataPrefix = value;
      } else if (MAPPING_CLASS.equals(key)) {
        mappingClass = value;
      } else if (COPYRIGHT.equals(key)) {
        copyright = value;
      } else if (SINGLE_FILE.equals(key) &&
                 ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
        singleFile = "true".equalsIgnoreCase(value);
      } else if (LITE_BUILD.equals(key) &&
                 ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
        liteBuild = "true".equalsIgnoreCase(value);
      } else if (SPECIAL_BUILD.equals(key) &&
                 ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
        specialBuild = "true".equalsIgnoreCase(value);
      } else if (BUILD_REGIONCODE.equals(key) &&
                 ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
        buildRegioncode = "true".equalsIgnoreCase(value);
      } else {
        System.err.println(HELP_MESSAGE);
        System.err.println("Illegal command line parameter: " + getArgs()[i]);
        return false;
      }
    }

    if (inputFile == null ||
        outputDir == null ||
        dataPrefix == null ||
        mappingClass == null ||
        copyright == null) {
      System.err.println(HELP_MESSAGE);
      return false;
    }

    String filePrefix = new File(outputDir, dataPrefix).getPath();

    try {
      PhoneMetadataCollection metadataCollection =
          BuildMetadataFromXml.buildPhoneMetadataCollection(inputFile, liteBuild, specialBuild);

      if (singleFile) {
        FileOutputStream output = new FileOutputStream(filePrefix);
        ObjectOutputStream out = new ObjectOutputStream(output);
        metadataCollection.writeExternal(out);
        out.close();
      } else {
        deleteAllFilesForPrefix(filePrefix);
        for (PhoneMetadata metadata : metadataCollection.getMetadataList()) {
          String regionCode = metadata.getId();
          // For non-geographical country calling codes (e.g. +800), or for alternate formats, use the
          // country calling codes instead of the region code to form the file name.
          if (regionCode.equals("001") || regionCode.isEmpty()) {
            regionCode = Integer.toString(metadata.getCountryCode());
          }
          PhoneMetadataCollection outMetadataCollection = new PhoneMetadataCollection();
          outMetadataCollection.addMetadata(metadata);
          FileOutputStream outputForRegion = new FileOutputStream(filePrefix + "_" + regionCode);
          ObjectOutputStream out = new ObjectOutputStream(outputForRegion);
          outMetadataCollection.writeExternal(out);
          out.close();
        }
        System.out.println("Generated " + metadataCollection.getMetadataCount() + " new files");
      }

      Map<Integer, List<String>> countryCodeToRegionCodeMap =
          BuildMetadataFromXml.buildCountryCodeToRegionCodeMap(metadataCollection);

      writeCountryCallingCodeMappingToJavaFile(
          countryCodeToRegionCodeMap, outputDir, mappingClass, copyright);
      
      if (buildRegioncode) {
        SortedSet<String> regionCodeSet = new TreeSet<String>();
        // Official code for the unknown region.
        regionCodeSet.add("ZZ");
        regionCodeSet.addAll(BuildMetadataFromXml.buildRegionCodeList(metadataCollection));
        System.out.println("Found " + regionCodeSet.size() + " region codes");
        
        writeRegionCodeConstantsToJavaFile(regionCodeSet, outputDir, copyright);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    System.out.println("Metadata code successfully created.");
    return true;
  }

  private void deleteAllFilesForPrefix(String filePrefix) {
    File[] allFiles = new File(filePrefix).getParentFile().listFiles();
    if (allFiles == null) {
      allFiles = new File[0];
    }
    int counter = 0;
    for (File file: allFiles) {
      if (file.getAbsolutePath().contains(filePrefix)) {
        if (file.delete()) {
          counter++;
        }
      }
    }
    System.out.println("Deleted " + counter + " old files");
  }

  private static void writeCountryCallingCodeMappingToJavaFile(
      Map<Integer, List<String>> countryCodeToRegionCodeMap,
      String outputDir, String mappingClass, String copyright) throws IOException {
    // Find out whether the countryCodeToRegionCodeMap has any region codes or country
    // calling codes listed in it.
    boolean hasRegionCodes = false;
    for (List<String> listWithRegionCode : countryCodeToRegionCodeMap.values()) {
      if (!listWithRegionCode.isEmpty()) {
        hasRegionCodes = true;
        break;
      }
    }
    boolean hasCountryCodes = countryCodeToRegionCodeMap.size() > 1;

    ClassWriter.Builder writer = ClassWriter.builder()
        .setOutputDir(outputDir)
        .setCopyright(Integer.parseInt(copyright))
        .setModifiers("public")
        .setName(mappingClass);

    int capacity = (int) (countryCodeToRegionCodeMap.size() / CAPACITY_FACTOR);
    if (hasRegionCodes && hasCountryCodes) {
      writeMap(writer, capacity, countryCodeToRegionCodeMap);
    } else if (hasCountryCodes) {
      writeCountryCodeSet(writer, capacity, countryCodeToRegionCodeMap.keySet());
    } else {
      List<String> regionCodeList = countryCodeToRegionCodeMap.get(0);
      capacity = (int) (regionCodeList.size() / CAPACITY_FACTOR);
      writeRegionCodeSet(writer, capacity, regionCodeList);
    }

    writer.build().writeToFile();
  }

  private static void writeMap(ClassWriter.Builder writer, int capacity,
                               Map<Integer, List<String>> countryCodeToRegionCodeMap) {
    writer.addToBody(MAP_COMMENT);

    writer.addToImports("java.util.ArrayList");
    writer.addToImports("java.util.HashMap");
    writer.addToImports("java.util.List");
    writer.addToImports("java.util.Map");

    writer.addToBody("  public static Map<Integer, List<String>> getCountryCodeToRegionCodeMap() {\n");
    writer.formatToBody(CAPACITY_COMMENT, capacity, countryCodeToRegionCodeMap.size());
    writer.addToBody("    Map<Integer, List<String>> countryCodeToRegionCodeMap =\n");
    writer.addToBody("        new HashMap<Integer, List<String>>(" + capacity + ");\n");
    writer.addToBody("\n");
    writer.addToBody("    ArrayList<String> listWithRegionCode;\n");
    writer.addToBody("\n");

    for (Map.Entry<Integer, List<String>> entry : countryCodeToRegionCodeMap.entrySet()) {
      int countryCallingCode = entry.getKey();
      List<String> regionCodes = entry.getValue();
      writer.addToBody("    listWithRegionCode = new ArrayList<String>(" +
                       regionCodes.size() + ");\n");
      for (String regionCode : regionCodes) {
        writer.addToBody("    listWithRegionCode.add(\"" + regionCode + "\");\n");
      }
      writer.addToBody("    countryCodeToRegionCodeMap.put(" + countryCallingCode +
                       ", listWithRegionCode);\n");
      writer.addToBody("\n");
    }

    writer.addToBody("    return countryCodeToRegionCodeMap;\n");
    writer.addToBody("  }\n");
  }

  private static void writeRegionCodeSet(ClassWriter.Builder writer, int capacity,
                                         List<String> regionCodeList) {
    writer.addToBody(REGION_CODE_SET_COMMENT);

    writer.addToImports("java.util.HashSet");
    writer.addToImports("java.util.Set");

    writer.addToBody("  public static Set<String> getRegionCodeSet() {\n");
    writer.formatToBody(CAPACITY_COMMENT, capacity, regionCodeList.size());
    writer.addToBody("    Set<String> regionCodeSet = new HashSet<String>(" + capacity + ");\n");
    writer.addToBody("\n");

    for (String regionCode : regionCodeList) {
      writer.addToBody("    regionCodeSet.add(\"" + regionCode + "\");\n");
    }

    writer.addToBody("\n");
    writer.addToBody("    return regionCodeSet;\n");
    writer.addToBody("  }\n");
  }

  private static void writeCountryCodeSet(ClassWriter.Builder writer, int capacity,
                                          Set<Integer> countryCodeSet) {
    writer.addToBody(COUNTRY_CODE_SET_COMMENT);

    writer.addToImports("java.util.HashSet");
    writer.addToImports("java.util.Set");

    writer.addToBody("  public static Set<Integer> getCountryCodeSet() {\n");
    writer.formatToBody(CAPACITY_COMMENT, capacity, countryCodeSet.size());
    writer.addToBody("    Set<Integer> countryCodeSet = new HashSet<Integer>(" + capacity + ");\n");
    writer.addToBody("\n");

    for (int countryCallingCode : countryCodeSet) {
      writer.addToBody("    countryCodeSet.add(" + countryCallingCode + ");\n");
    }

    writer.addToBody("\n");
    writer.addToBody("    return countryCodeSet;\n");
    writer.addToBody("  }\n");
  }

  private static void writeRegionCodeConstantsToJavaFile(Collection<String> regionCodeList,
                                                         String outputDir, String copyright) throws IOException {
    ClassWriter.Builder writer = ClassWriter.builder()
        .setOutputDir(outputDir)
        .setName("RegionCode")
        .setModifiers("final")
        .setCopyright(Integer.parseInt(copyright));

    writer.setJavadoc(REGION_CODE_CONSTS_JAVADOC);
    
    for (String regionCode : regionCodeList) {
      String variableName = regionCode.toUpperCase();
      if (variableName.equals("001")) {
        writer.addToBody("  // Region code for global networks (e.g. +800 numbers).\n");
        variableName = "UN001";
      } else if (variableName.equals("ZZ")) {
        writer.addToBody("  // Official code for the unknown region.\n");
      }
      writer.addToBody("  static final String " + variableName + " = \"" + regionCode + "\";\n");
    }
    
    writer.build().writeToFile();
  }


  @AutoValue
  abstract static class ClassWriter {
    abstract String outputDir();
    
    abstract Integer copyright();
    abstract ImmutableSortedSet<String> imports();
    abstract String javadoc();
    abstract String modifiers();
    abstract String name();
    abstract String body();

    static Builder builder() {
      return new AutoValue_BuildMetadataProtoFromXml_ClassWriter.Builder()
          .setJavadoc("").setModifiers("");
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setOutputDir(String outputDir);
      
      abstract Builder setCopyright(Integer copyright);
      
      abstract ImmutableSortedSet.Builder<String> importsBuilder();
      final Builder addToImports(String name) {
        importsBuilder().add(name);
        return this;
      }

      abstract Builder setJavadoc(String javadoc);      
      abstract Builder setName(String name);
      abstract Builder setModifiers(String modifiers);
      
      abstract Builder setBody(String body);
      private final StringBuilder bodyBuilder = new StringBuilder();
      final Builder addToBody(String text) {
        bodyBuilder.append(text);
        return this;
      }
      final Builder formatToBody(String format, Object... args) {
        Formatter formatter = new Formatter(bodyBuilder);
        formatter.format(format, args);
        return this;
      }

      abstract ClassWriter autoBuild();
      
      final ClassWriter build() {
        setBody(bodyBuilder.toString());
        return autoBuild();
      }
    }

    void writeToFile() throws IOException {
      Writer writer = new BufferedWriter(new FileWriter(new File(outputDir(), name() + ".java")));

      CopyrightNotice.writeTo(writer, copyright());
      writer.write(GENERATION_COMMENT);
      writer.write("package " + PACKAGE_NAME + ";\n\n");

      if (!imports().isEmpty()) {
        for (String item : imports()) {
          writer.write("import " + item + ";\n");
        }
        writer.write("\n");
      }

      writer.write(javadoc());
      if (!modifiers().isEmpty()) {
        writer.write(modifiers() + " ");
      }
      writer.write("class " + name() + " {\n");
      writer.write(body());
      writer.write("}\n");

      writer.flush();
      writer.close();
    }
  }
}
