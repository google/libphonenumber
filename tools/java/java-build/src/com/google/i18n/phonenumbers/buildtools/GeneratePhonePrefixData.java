/*
 * Copyright (C) 2011 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.buildtools;

import com.google.i18n.phonenumbers.prefixmapper.MappingFileProvider;
import com.google.i18n.phonenumbers.prefixmapper.PhonePrefixMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A utility that generates the binary serialization of the phone prefix mappings from
 * human-readable text files. It also generates a configuration file which contains information on
 * data files available for use.
 *
 * <p> The text files must be located in sub-directories of the provided input path. For each input
 * file inputPath/lang/countryCallingCode.txt the corresponding binary file is generated as
 * outputPath/countryCallingCode_lang.
 */
public class GeneratePhonePrefixData {
  // The path to the input directory containing the languages directories.
  private final File inputPath;
  private static final int NANPA_COUNTRY_CODE = 1;
  // Pattern used to match the language code contained in the input text file path. This may be a
  // two-letter code like fr, or a three-letter code like ban, or a code containing script
  // information like zh_Hans (simplified Chinese).
  private static final Pattern LANGUAGE_IN_FILE_PATH_PATTERN =
      Pattern.compile("(.*/)(?:[a-zA-Z_]+)(/\\d+\\.txt)");
  // Map used to store the English mappings to avoid reading the English text files multiple times.
  private final Map<Integer /* country code */, SortedMap<Integer, String>> englishMaps =
      new HashMap<Integer, SortedMap<Integer, String>>();
  // The IO Handler used to output the generated binary files.
  private final AbstractPhonePrefixDataIOHandler ioHandler;

  private static final Logger logger = Logger.getLogger(GeneratePhonePrefixData.class.getName());

  public GeneratePhonePrefixData(File inputPath, AbstractPhonePrefixDataIOHandler ioHandler)
      throws IOException {
    if (!inputPath.isDirectory()) {
      throw new IOException("The provided input path does not exist: "
          + inputPath.getAbsolutePath());
    }
    this.inputPath = inputPath;
    this.ioHandler = ioHandler;
  }

  /**
   * Implement this interface to provide a callback to the parseTextFile() method.
   */
  static interface PhonePrefixMappingHandler {
    /**
     * Method called every time the parser matches a mapping. Note that 'prefix' is the prefix as
     * it is written in the text file (i.e phone number prefix appended to country code).
     */
    void process(int prefix, String location);
  }

  /**
   * Reads phone prefix data from the provided input stream and invokes the given handler for each
   * mapping read.
   */
  // @VisibleForTesting
  static void parseTextFile(InputStream input,
                            PhonePrefixMappingHandler handler) throws IOException {
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(
            new BufferedInputStream(input), Charset.forName("UTF-8")));
    int lineNumber = 1;

    for (String line; (line = bufferedReader.readLine()) != null; lineNumber++) {
      line = line.trim();
      if (line.length() == 0 || line.startsWith("#")) {
        continue;
      }
      int indexOfPipe = line.indexOf('|');
      if (indexOfPipe == -1) {
        throw new RuntimeException(String.format("line %d: malformatted data, expected '|'",
                                                 lineNumber));
      }
      String prefix = line.substring(0, indexOfPipe);
      String location = line.substring(indexOfPipe + 1);
      handler.process(Integer.parseInt(prefix), location);
    }
  }

  /**
   * Writes the provided phone prefix map to the provided output stream.
   *
   * @throws IOException
   */
  // @VisibleForTesting
  static void writeToBinaryFile(SortedMap<Integer, String> sortedMap, OutputStream output)
      throws IOException {
    // Build the corresponding phone prefix map and serialize it to the binary format.
    PhonePrefixMap phonePrefixMap = new PhonePrefixMap();
    phonePrefixMap.readPhonePrefixMap(sortedMap);
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(output);
    phonePrefixMap.writeExternal(objectOutputStream);
    objectOutputStream.flush();
  }

  /**
   * Reads the mappings contained in the provided input stream pointing to a text file.
   *
   * @return  a map containing the mappings that were read
   */
  // @VisibleForTesting
  static SortedMap<Integer, String> readMappingsFromTextFile(InputStream input)
      throws IOException {
    final SortedMap<Integer, String> phonePrefixMap = new TreeMap<Integer, String>();
    parseTextFile(input, new PhonePrefixMappingHandler() {
      @Override
      public void process(int prefix, String location) {
        if (phonePrefixMap.put(prefix, location) != null) {
          throw new RuntimeException(String.format("duplicated prefix %d", prefix));
        }
      }
    });
    return phonePrefixMap;
  }

  private static class PhonePrefixLanguagePair {
    public final String prefix;
    public final String language;

    public PhonePrefixLanguagePair(String prefix, String language) {
      this.prefix = prefix;
      this.language = language;
    }
  }

  private static String generateBinaryFilename(int prefix, String lang) {
    return String.format("%d_%s", prefix, lang);
  }

  /**
   * Extracts the phone prefix and the language code contained in the provided file name.
   */
  private static PhonePrefixLanguagePair getPhonePrefixLanguagePairFromFilename(String filename) {
    int indexOfUnderscore = filename.indexOf('_');
    String prefix = filename.substring(0, indexOfUnderscore);
    String language = filename.substring(indexOfUnderscore + 1);
    return new PhonePrefixLanguagePair(prefix, language);
  }

  /**
   * Method used by {@code #createInputOutputMappings()} to generate the list of output binary files
   * from the provided input text file. For the data files expected to be large (currently only
   * NANPA is supported), this method generates a list containing one output file for each area
   * code. Otherwise, a single file is added to the list.
   */
  private List<File> createOutputFiles(File countryCodeFile, int countryCode, String language)
      throws IOException {
    List<File> outputFiles = new ArrayList<File>();
    // For NANPA, split the data into multiple binary files.
    if (countryCode == NANPA_COUNTRY_CODE) {
      // Fetch the 4-digit prefixes stored in the file.
      final Set<Integer> phonePrefixes = new HashSet<Integer>();
      FileInputStream inputStream = new FileInputStream(countryCodeFile);
      parseTextFile(inputStream, new PhonePrefixMappingHandler() {
        @Override
        public void process(int prefix, String location) {
          phonePrefixes.add(Integer.parseInt(String.valueOf(prefix).substring(0, 4)));
        }
      });
      for (int prefix : phonePrefixes) {
        outputFiles.add(ioHandler.createFile(generateBinaryFilename(prefix, language)));
      }
    } else {
      outputFiles.add(ioHandler.createFile(generateBinaryFilename(countryCode, language)));
    }
    return outputFiles;
  }

  /**
   * Returns the country code extracted from the provided text file name expected as
   * [1-9][0-9]*.txt.
   *
   * @throws RuntimeException if the file path is not formatted as expected
   */
  private static int getCountryCodeFromTextFileName(String filename) {
    int indexOfDot = filename.indexOf('.');
    if (indexOfDot < 1) {
      throw new RuntimeException(
          String.format("unexpected file name %s, expected pattern [1-9][0-9]*.txt", filename));
    }
    String countryCode = filename.substring(0, indexOfDot);
    return Integer.parseInt(countryCode);
  }

  /**
   * Generates the mappings between the input text files and the output binary files.
   *
   * @throws IOException
   */
  private Map<File, List<File>> createInputOutputMappings() throws IOException {
    Map<File, List<File>> mappings = new LinkedHashMap<File, List<File>>();
    File[] languageDirectories = inputPath.listFiles();
    // Make sure that filenames are processed in the same order build-to-build.
    Arrays.sort(languageDirectories);

    for (File languageDirectory : languageDirectories) {
      if (!languageDirectory.isDirectory() || languageDirectory.isHidden()) {
        continue;
      }
      File[] countryCodeFiles = languageDirectory.listFiles();
      Arrays.sort(countryCodeFiles);

      for (File countryCodeFile : countryCodeFiles) {
        if (countryCodeFile.isHidden()) {
          continue;
        }
        String countryCodeFileName = countryCodeFile.getName();
        List<File> outputFiles = createOutputFiles(
            countryCodeFile, getCountryCodeFromTextFileName(countryCodeFileName),
            languageDirectory.getName());
        mappings.put(countryCodeFile, outputFiles);
      }
    }
    return mappings;
  }

  /**
   * Adds a phone number prefix/language mapping to the provided map. The prefix and language are
   * generated from the provided file name previously used to output the phone prefix mappings for
   * the given country.
   */
  // @VisibleForTesting
  static void addConfigurationMapping(SortedMap<Integer, Set<String>> availableDataFiles,
                                      File outputPhonePrefixMappingsFile) {
    String outputPhonePrefixMappingsFileName = outputPhonePrefixMappingsFile.getName();
    PhonePrefixLanguagePair phonePrefixLanguagePair =
        getPhonePrefixLanguagePairFromFilename(outputPhonePrefixMappingsFileName);
    int prefix = Integer.parseInt(phonePrefixLanguagePair.prefix);
    String language = phonePrefixLanguagePair.language;
    Set<String> languageSet = availableDataFiles.get(prefix);
    if (languageSet == null) {
      languageSet = new HashSet<String>();
      availableDataFiles.put(prefix, languageSet);
    }
    languageSet.add(language);
  }

  /**
   * Outputs the binary configuration file mapping country codes to language strings.
   */
  // @VisibleForTesting
  static void outputBinaryConfiguration(SortedMap<Integer, Set<String>> availableDataFiles,
                                        OutputStream outputStream) throws IOException {
    MappingFileProvider mappingFileProvider = new MappingFileProvider();
    mappingFileProvider.readFileConfigs(availableDataFiles);
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    mappingFileProvider.writeExternal(objectOutputStream);
    objectOutputStream.flush();
  }

  /**
   * Splits the provided phone prefix map into multiple maps according to the provided list of
   * output binary files. A map associating output binary files to phone prefix maps is returned as
   * a result.
   * <pre>
   * Example:
   *   input map: { 12011: Description1, 12021: Description2 }
   *   outputBinaryFiles: { 1201_en, 1202_en }
   *   output map: { 1201_en: { 12011: Description1 }, 1202_en: { 12021: Description2 } }
   * </pre>
   */
  // @VisibleForTesting
  static Map<File, SortedMap<Integer, String>> splitMap(
      SortedMap<Integer, String> mappings, List<File> outputBinaryFiles) {
    Map<File, SortedMap<Integer, String>> mappingsForFiles =
        new LinkedHashMap<File, SortedMap<Integer, String>>();
    for (Map.Entry<Integer, String> mapping : mappings.entrySet()) {
      String prefix = String.valueOf(mapping.getKey());
      File targetFile = null;

      for (File outputBinaryFile : outputBinaryFiles) {
        String outputBinaryFilePrefix =
            getPhonePrefixLanguagePairFromFilename(outputBinaryFile.getName()).prefix;
        if (prefix.startsWith(outputBinaryFilePrefix)) {
          targetFile = outputBinaryFile;
          break;
        }
      }
      SortedMap<Integer, String> mappingsForPhonePrefixLangPair = mappingsForFiles.get(targetFile);
      if (mappingsForPhonePrefixLangPair == null) {
        mappingsForPhonePrefixLangPair = new TreeMap<Integer, String>();
        mappingsForFiles.put(targetFile, mappingsForPhonePrefixLangPair);
      }
      mappingsForPhonePrefixLangPair.put(mapping.getKey(), mapping.getValue());
    }
    return mappingsForFiles;
  }

  /**
   * Gets the English data text file path corresponding to the provided one.
   */
  // @VisibleForTesting
  static String getEnglishDataPath(String inputTextFileName) {
    return LANGUAGE_IN_FILE_PATH_PATTERN.matcher(inputTextFileName).replaceFirst("$1en$2");
  }

  /**
   * Tests whether any prefix of the given number overlaps with any phone number prefix contained in
   * the provided map.
   */
  // @VisibleForTesting
  static boolean hasOverlappingPrefix(int number, SortedMap<Integer, String> mappings) {
    while (number > 0) {
      number = number / 10;
      if (mappings.get(number) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Compresses the provided non-English map according to the English map provided. For each mapping
   * which is contained in both maps with a same description this method either:
   * <ul>
   *  <li> Removes from the non-English map the mapping whose prefix does not overlap with an
   *       existing prefix in the map, or;
   *  <li> Keeps this mapping in both maps but makes the description an empty string in the
   *       non-English map.
   * </ul>
   */
  // @VisibleForTesting
  static void compressAccordingToEnglishData(
      SortedMap<Integer, String> englishMap, SortedMap<Integer, String> nonEnglishMap) {
    Iterator<Map.Entry<Integer, String>> it = nonEnglishMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, String> entry = it.next();
      int prefix = entry.getKey();
      String englishDescription = englishMap.get(prefix);
      if (englishDescription != null && englishDescription.equals(entry.getValue())) {
        if (!hasOverlappingPrefix(prefix, nonEnglishMap)) {
          it.remove();
        } else {
          nonEnglishMap.put(prefix, "");
        }
      }
    }
  }

  /**
   * Compresses the provided mappings according to the English data file if any.
   *
   * @throws IOException
   */
  private void makeDataFallbackToEnglish(File inputTextFile, SortedMap<Integer, String> mappings)
      throws IOException {
    File englishTextFile = new File(getEnglishDataPath(inputTextFile.getAbsolutePath()));
    if (inputTextFile.getAbsolutePath().equals(englishTextFile.getAbsolutePath())
        || !englishTextFile.exists()) {
      return;
    }
    int countryCode = getCountryCodeFromTextFileName(inputTextFile.getName());
    SortedMap<Integer, String> englishMap = englishMaps.get(countryCode);
    if (englishMap == null) {
      FileInputStream englishFileInputStream = null;
      try {
        englishFileInputStream = new FileInputStream(englishTextFile);
        englishMap = readMappingsFromTextFile(englishFileInputStream);
        englishMaps.put(countryCode, englishMap);
      } finally {
        ioHandler.closeFile(englishFileInputStream);
      }
    }
    compressAccordingToEnglishData(englishMap, mappings);
  }

  /**
   * Removes the empty-description mappings in the provided map if the language passed-in is "en".
   */
  // @VisibleForTesting
  static void removeEmptyEnglishMappings(SortedMap<Integer, String> map, String lang) {
    if (!lang.equals("en")) {
      return;
    }
    Iterator<Map.Entry<Integer, String>> it = map.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, String> mapping = it.next();
      if (mapping.getValue().isEmpty()) {
        it.remove();
      }
    }
  }

  /**
   * Runs the phone prefix data generator.
   *
   * @throws IOException
   */
  public void run() throws IOException {
    Map<File, List<File>> inputOutputMappings = createInputOutputMappings();
    SortedMap<Integer, Set<String>> availableDataFiles = new TreeMap<Integer, Set<String>>();

    for (Map.Entry<File, List<File>> inputOutputMapping : inputOutputMappings.entrySet()) {
      FileInputStream fileInputStream = null;
      FileOutputStream fileOutputStream = null;

      try {
        File textFile = inputOutputMapping.getKey();
        List<File> outputBinaryFiles = inputOutputMapping.getValue();
        fileInputStream = new FileInputStream(textFile);
        SortedMap<Integer, String> mappings = readMappingsFromTextFile(fileInputStream);
        removeEmptyEnglishMappings(mappings, textFile.getParentFile().getName());
        makeDataFallbackToEnglish(textFile, mappings);
        Map<File, SortedMap<Integer, String>> mappingsForFiles =
            splitMap(mappings, outputBinaryFiles);

        for (Map.Entry<File, SortedMap<Integer, String>> mappingsForFile :
             mappingsForFiles.entrySet()) {
          File outputBinaryFile = mappingsForFile.getKey();
          fileOutputStream = null;
          try {
            fileOutputStream = new FileOutputStream(outputBinaryFile);
            writeToBinaryFile(mappingsForFile.getValue(), fileOutputStream);
            addConfigurationMapping(availableDataFiles, outputBinaryFile);
            ioHandler.addFileToOutput(outputBinaryFile);
          } finally {
            ioHandler.closeFile(fileOutputStream);
          }
        }
      } catch (RuntimeException e) {
        logger.log(Level.SEVERE,
                   "Error processing file " + inputOutputMapping.getKey().getAbsolutePath());
        throw e;
      } catch (IOException e) {
        logger.log(Level.SEVERE, e.getMessage());
      } finally {
        ioHandler.closeFile(fileInputStream);
        ioHandler.closeFile(fileOutputStream);
      }
    }
    // Output the binary configuration file mapping country codes to languages.
    FileOutputStream fileOutputStream = null;
    try {
      File configFile = ioHandler.createFile("config");
      fileOutputStream = new FileOutputStream(configFile);
      outputBinaryConfiguration(availableDataFiles, fileOutputStream);
      ioHandler.addFileToOutput(configFile);
    } finally {
      ioHandler.closeFile(fileOutputStream);
      ioHandler.close();
    }
    logger.log(Level.INFO, "Phone prefix data successfully generated.");
  }
}
