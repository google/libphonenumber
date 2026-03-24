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

/**
 * Implementation of the AbstractPhonePrefixDataIOHandler required by the GeneratePhonePrefixData
 * class used here to create the output files.
 */
class PhonePrefixDataIOHandler extends AbstractPhonePrefixDataIOHandler {

  // The path to the output directory.
  private final File outputPath;

  public PhonePrefixDataIOHandler(File outputPath) throws IOException {
    if (outputPath.exists()) {
      if (!outputPath.isDirectory()) {
        throw new IOException("Expected directory: " + outputPath.getAbsolutePath());
      }
    } else {
      if (!outputPath.mkdirs()) {
        throw new IOException("Could not create directory " + outputPath.getAbsolutePath());
      }
    }
    this.outputPath = outputPath;
  }

  /**
   * This is a <b>no-op</b>.
   *
   * <p>This would be the place dealing with the addition of the provided file to the resulting JAR
   * if the global output was a JAR instead of a directory containing the binary files.
   */
  @Override
  public void addFileToOutput(File file) {
  }

  @Override
  public File createFile(String path) {
    return new File(outputPath, path);
  }

  /**
   * This is a <b>no-op</b>, as no resource needs to be released.
   */
  @Override
  public void close() {
  }
}
