/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.i18n.phonenumbers.geocoding.AreaCodeMap;
import com.google.i18n.phonenumbers.geocoding.MappingFileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility that generates the binary serialization of the area code/location mappings from
 * human-readable text files. It also generates a configuration file which contains information on
 * data files available for use.
 *
 * <p> The text files must be located in sub-directories of the provided input path. For each input
 * file inputPath/lang/countryCallingCode.txt the corresponding binary file is generated as
 * outputPath/countryCallingCode_lang.
 *
 * @author Philippe Liard
 */
public class GenerateAreaCodeData extends Command {
  // The path to the input directory containing the languages directories.
  private final File inputPath;
  // The path to the output directory.
  private final File outputPath;
  private static final int NANPA_COUNTRY_CODE = 1;

  private static final Logger LOGGER = Logger.getLogger(GenerateAreaCodeData.class.getName());

  /**
   * Empty constructor used by the EntryPoint class.
   */
  public GenerateAreaCodeData() {
    inputPath = null;
    outputPath = null;
  }

  public GenerateAreaCodeData(File inputPath, File outputPath) throws IOException {
    if (!inputPath.isDirectory()) {
      throw new IOException("The provided input path does not exist: " +
                             inputPath.getAbsolutePath());
    }
    if (outputPath.exists()) {
      if (!outputPath.isDirectory()) {
        throw new IOException("Expected directory: " + outputPath.getAbsolutePath());
      }
    } else {
      if (!outputPath.mkdirs()) {
        throw new IOException("Could not create directory " + outputPath.getAbsolutePath());
      }
    }
    this.inputPath = inputPath;
    this.outputPath = outputPath;
  }

  /**
   * Closes the provided file and log any potential IOException.
   */
  private static void closeFile(Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
    }
  }

  /**
   * Implement this interface to provide a callback to the parseTextFile() method.
   */
  static interface AreaCodeMappingHandler {
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
  static void parseTextFile(InputStream input, AreaCodeMappingHandler handler) throws IOException {
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
      if (indexOfPipe == line.length() - 1) {
        throw new RuntimeException(String.format("line %d: missing location", lineNumber));
      }
      String location = line.substring(indexOfPipe + 1);
      handler.process(Integer.parseInt(prefix), location);
    }
  }

  /**
   * Writes the provided area code map to the provided output stream.
   *
   * @throws IOException
   */
  // @VisibleForTesting
  static void writeToBinaryFile(SortedMap<Integer, String> sortedMap, OutputStream output)
      throws IOException {
    // Build the corresponding area code map and serialize it to the binary format.
    AreaCodeMap areaCodeMap = new AreaCodeMap();
    areaCodeMap.readAreaCodeMap(sortedMap);
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(output);
    areaCodeMap.writeExternal(objectOutputStream);
    objectOutputStream.flush();
  }

  /**
   * Reads the mappings contained in the provided input stream pointing to a text file.
   *
   * @return  a map containing the mappings that were read.
   */
  // @VisibleForTesting
  static SortedMap<Integer, String> readMappingsFromTextFile(InputStream input)
      throws IOException {
    final SortedMap<Integer, String> areaCodeMap = new TreeMap<Integer, String>();
    parseTextFile(input, new AreaCodeMappingHandler() {
      @Override
      public void process(int prefix, String location) {
        if (areaCodeMap.put(prefix, location) != null) {
          throw new RuntimeException(String.format("duplicated prefix %d", prefix));
        }
      }
    });
    return areaCodeMap;
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
      parseTextFile(inputStream, new AreaCodeMappingHandler() {
        @Override
        public void process(int prefix, String location) {
          phonePrefixes.add(Integer.parseInt(String.valueOf(prefix).substring(0, 4)));
        }
      });
      for (int prefix : phonePrefixes) {
        outputFiles.add(
            new File(outputPath, generateBinaryFilename(prefix, language)));
      }
    } else {
      outputFiles.add(
          new File(outputPath, generateBinaryFilename(countryCode, language)));
    }
    return outputFiles;
  }

  /**
   * Generates the mappings between the input text files and the output binary files.
   *
   * @throws IOException
   */
  private Map<File, List<File>> createInputOutputMappings() throws IOException {
    Map<File, List<File>> mappings = new HashMap<File, List<File>>();
    File[] languageDirectories = inputPath.listFiles();

    for (File languageDirectory : languageDirectories) {
      if (!languageDirectory.isDirectory() || languageDirectory.isHidden()) {
        continue;
      }
      File[] countryCodeFiles = languageDirectory.listFiles();

      for (File countryCodeFile : countryCodeFiles) {
        if (countryCodeFile.isHidden()) {
          continue;
        }
        String countryCodeFileName = countryCodeFile.getName();
        int indexOfDot = countryCodeFileName.indexOf('.');
        if (indexOfDot == -1) {
          throw new RuntimeException(
              String.format("unexpected file name %s, expected pattern .*\\.txt",
                            countryCodeFileName));
        }
        String countryCode = countryCodeFileName.substring(0, indexOfDot);
        if (!countryCode.matches("\\d+")) {
          throw new RuntimeException("unexpected file " + countryCodeFileName);
        }
        List<File> outputFiles = createOutputFiles(
            countryCodeFile, Integer.parseInt(countryCode), languageDirectory.getName());
        mappings.put(countryCodeFile, outputFiles);
      }
    }
    return mappings;
  }

  /**
   * Adds a phone number prefix/language mapping to the provided map. The prefix and language are
   * generated from the provided file name previously used to output the area code/location mappings
   * for the given country.
   */
   // @VisibleForTesting
  static void addConfigurationMapping(SortedMap<Integer, Set<String>> availableDataFiles,
                                      File outputAreaCodeMappingsFile) {
    String outputAreaCodeMappingsFileName = outputAreaCodeMappingsFile.getName();
    PhonePrefixLanguagePair areaCodeLanguagePair =
        getPhonePrefixLanguagePairFromFilename(outputAreaCodeMappingsFileName);
    int prefix = Integer.parseInt(areaCodeLanguagePair.prefix);
    String language = areaCodeLanguagePair.language;
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
   * Splits the provided area code map into multiple maps according to the provided list of output
   * binary files. A map associating output binary files to area code maps is returned as a result.
   * <pre>
   * Example:
   *   input map: { 12011: Location1, 12021: Location2 }
   *   outputBinaryFiles: { 1201_en, 1202_en }
   *   output map: { 1201_en: { 12011: Location1 }, 1202_en: { 12021: Location2 } }
   * </pre>
   */
  // @VisibleForTesting
  static Map<File, SortedMap<Integer, String>> splitMap(
      SortedMap<Integer, String> mappings, List<File> outputBinaryFiles) {
    Map<File, SortedMap<Integer, String>> mappingsForFiles =
        new HashMap<File, SortedMap<Integer, String>>();
    for (Map.Entry<Integer, String> mapping : mappings.entrySet()) {
      String prefix = String.valueOf(mapping.getKey());
      File targetFile = null;
      int correspondingAreaCode = -1;

      for (File outputBinaryFile : outputBinaryFiles) {
        String outputBinaryFilePrefix =
            getPhonePrefixLanguagePairFromFilename(outputBinaryFile.getName()).prefix;
        if (prefix.startsWith(outputBinaryFilePrefix)) {
          targetFile = outputBinaryFile;
          correspondingAreaCode = Integer.parseInt(outputBinaryFilePrefix);
          break;
        }
      }
      SortedMap<Integer, String> mappingsForAreaCodeLangPair = mappingsForFiles.get(targetFile);
      if (mappingsForAreaCodeLangPair == null) {
        mappingsForAreaCodeLangPair = new TreeMap<Integer, String>();
        mappingsForFiles.put(targetFile, mappingsForAreaCodeLangPair);
      }
      mappingsForAreaCodeLangPair.put(mapping.getKey(), mapping.getValue());
    }
    return mappingsForFiles;
  }

  /**
   * Runs the area code data generator.
   *
   * @throws IOException
   * @throws FileNotFoundException
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
          } finally {
            closeFile(fileOutputStream);
          }
        }
      } catch (RuntimeException e) {
        LOGGER.log(Level.SEVERE,
                   "Error processing file " + inputOutputMapping.getKey().getAbsolutePath());
        throw e;
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, e.getMessage());
      } finally {
        closeFile(fileInputStream);
        closeFile(fileOutputStream);
      }
    }
    // Output the binary configuration file mapping country codes to languages.
    FileOutputStream fileOutputStream = null;
    try {
      File configFile = new File(outputPath, "config");
      fileOutputStream = new FileOutputStream(configFile);
      outputBinaryConfiguration(availableDataFiles, fileOutputStream);
    } finally {
      closeFile(fileOutputStream);
    }
    LOGGER.log(Level.INFO, "Geocoding data successfully generated.");
  }

  @Override
  public String getCommandName() {
    return "GenerateAreaCodeData";
  }

  @Override
  public boolean start() {
    String[] args = getArgs();

    if (args.length != 3) {
      LOGGER.log(Level.SEVERE,
                 "usage: GenerateAreaCodeData /path/to/input/directory /path/to/output/directory");
      return false;
    }
    try {
      GenerateAreaCodeData generateAreaCodeData =
          new GenerateAreaCodeData(new File(args[1]), new File(args[2]));
      generateAreaCodeData.run();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
      return false;
    }
    return true;
  }
}
