/*
 * Copyright (C) 2018 The Libphonenumber Authors
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import junit.framework.TestCase;

/**
 * Tests for the output of the JSON metadata producer.
 */
public final class BuildShortNumberMetadataJsonFromXmlGoldenTest extends TestCase {

  private static final String INPUT_FILE_NAME = "ShortNumberMetadataForGoldenTests.xml";
  private static final String GOLDEN_FILE_NAME = "expected_shortnumbermetadata.js";

  public void testBuildMetadataJsonFromXmlGolden() throws Exception {
    File srcDir = new File("target/test-classes/com/google/i18n/phonenumbers/buildtools/testdata");
    File inputXml = new File(srcDir, INPUT_FILE_NAME);
    File outputFile = File.createTempFile("testOutput", "");
    outputFile.deleteOnExit();
    File golden = new File(srcDir, GOLDEN_FILE_NAME);

    BuildMetadataJsonFromXml.start(
        inputXml.getAbsolutePath(), outputFile.getAbsolutePath(), false /* not liteBuild */,
        "i18n.phonenumbers.shortnumbergoldenmetadata" /* namespace */);
    BufferedReader outputReader =
        new BufferedReader(new InputStreamReader(new FileInputStream(outputFile), "UTF-8"));
    BufferedReader goldenReader =
        new BufferedReader(new InputStreamReader(new FileInputStream(golden), "UTF-8"));
    while (outputReader.ready() && goldenReader.ready()) {
      String goldenLine = goldenReader.readLine();
      if (goldenLine.contains("ShortNumberMetadata.xml")) {
        // The full path of the input file is contained in the output and these lines will be
        // different, so we just check the output file name is present and continue.
        assertTrue(outputReader.readLine().contains(INPUT_FILE_NAME));
        continue;
      }
      assertEquals(outputReader.readLine(), goldenLine);
    }
    // Check the files are the same size.
    assertEquals(outputReader.ready(), goldenReader.ready());
  }
}
