/*
 * Copyright (C) 2012 The Libphonenumber Authors
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

import com.google.i18n.phonenumbers.prefixmapper.PrefixTimeZonesMap;

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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility that generates the binary serialization of the prefix/time zones mappings from a
 * human-readable text file.
 */
public class GenerateTimeZonesMapData {
  private final File inputTextFile;
  private static final String MAPPING_DATA_FILE_NAME = "map_data";
  // The IO Handler used to output the generated binary file.
  private final AbstractPhonePrefixDataIOHandler ioHandler;

  private static final Logger logger = Logger.getLogger(GenerateTimeZonesMapData.class.getName());

  public GenerateTimeZonesMapData(File inputTextFile, AbstractPhonePrefixDataIOHandler ioHandler)
      throws IOException {
    this.inputTextFile = inputTextFile;
    if (!inputTextFile.isFile()) {
      throw new IOException("The provided input text file does not exist.");
    }
    this.ioHandler = ioHandler;
  }

  /**
   * Reads phone prefix data from the provided input stream and returns a SortedMap with the
   * prefix to time zones mappings.
   */
  // @VisibleForTesting
  static SortedMap<Integer, String> parseTextFile(InputStream input)
      throws IOException, RuntimeException {
    final SortedMap<Integer, String> timeZoneMap = new TreeMap<Integer, String>();
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
      Integer prefix = Integer.parseInt(line.substring(0, indexOfPipe));
      String timezones = line.substring(indexOfPipe + 1);
      if (timezones.isEmpty()) {
        throw new RuntimeException(String.format("line %d: missing time zones", lineNumber));
      }
      if (timeZoneMap.put(prefix, timezones) != null) {
         throw new RuntimeException(String.format("duplicated prefix %d", prefix));
      }
    }
    return timeZoneMap;
  }

  /**
   * Writes the provided phone prefix/time zones map to the provided output stream.
   *
   * @throws IOException
   */
  // @VisibleForTesting
  static void writeToBinaryFile(SortedMap<Integer, String> sortedMap, OutputStream output)
      throws IOException {
    // Build the corresponding PrefixTimeZonesMap and serialize it to the binary format.
    PrefixTimeZonesMap prefixTimeZonesMap = new PrefixTimeZonesMap();
    prefixTimeZonesMap.readPrefixTimeZonesMap(sortedMap);
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(output);
    prefixTimeZonesMap.writeExternal(objectOutputStream);
    objectOutputStream.flush();
  }

  /**
   * Runs the prefix to time zones map data generator.
   *
   * @throws IOException
   */
  public void run() throws IOException {
    FileInputStream fileInputStream = null;
    FileOutputStream fileOutputStream = null;
    try {
      fileInputStream = new FileInputStream(inputTextFile);
      SortedMap<Integer, String> mappings = parseTextFile(fileInputStream);
      File outputBinaryFile = ioHandler.createFile(MAPPING_DATA_FILE_NAME);
      try {
        fileOutputStream = new FileOutputStream(outputBinaryFile);
        writeToBinaryFile(mappings, fileOutputStream);
        ioHandler.addFileToOutput(outputBinaryFile);
      } finally {
        ioHandler.closeFile(fileOutputStream);
      }
    } catch (RuntimeException e) {
      logger.log(Level.SEVERE,
                 "Error processing file " + inputTextFile.getAbsolutePath());
      throw e;
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      ioHandler.closeFile(fileInputStream);
      ioHandler.closeFile(fileOutputStream);
      ioHandler.close();
    }
    logger.log(Level.INFO, "Time zone data successfully generated.");
  }
}
