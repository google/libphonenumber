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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * Unittests for JarPhonePrefixDataIOHandler.java
 */
public class JarPhonePrefixDataIOHandlerTest extends TestCase {

  private static final String TESTING_JAR_BASE = "testing_data";
  private static final Logger logger =
      Logger.getLogger(JarPhonePrefixDataIOHandlerTest.class.getName());

  public void testAddFileToOutput() {
    File outputFile = null;

    try {
      // Create the output jar.
      File outputPath = new File("/tmp/build");
      Package outputPackage = JarPhonePrefixDataIOHandlerTest.class.getPackage();

      JarPhonePrefixDataIOHandler ioHandler =
          new JarPhonePrefixDataIOHandler(outputPath, TESTING_JAR_BASE, outputPackage);
      outputFile = File.createTempFile("outputTestFile", "txt");
      ioHandler.addFileToOutput(outputFile);
      ioHandler.close();

      JarFile outputJar = new JarFile(new File(outputPath, TESTING_JAR_BASE + ".jar"));
      // Test if there is exactly one entry in the jar.
      Enumeration<JarEntry> entries = outputJar.entries();
      int entriesCount = 0;
      while (entries.hasMoreElements()) {
        entriesCount++;
        entries.nextElement();
      }
      assertEquals(1, entriesCount);

      // Test if the entry file in the jar has the expected path.
      String jarEntryPath =
          "com/google/i18n/phonenumbers/buildtools/"
              + TESTING_JAR_BASE
              + "/"
              + outputFile.getPath();
      JarEntry jarEntry = outputJar.getJarEntry(jarEntryPath);
      assertNotNull("Output file not found inside the jar.", jarEntry);
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
      fail();
    } finally {
      if (outputFile != null && outputFile.exists()) {
        outputFile.delete();
      }
    }
  }
}