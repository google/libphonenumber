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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstracts the way GeneratePhonePrefixDataEntryPoint creates files and writes
 * the phone prefix data to them.
 */
public abstract class AbstractPhonePrefixDataIOHandler {
  private static final Logger logger = Logger.getLogger(
      AbstractPhonePrefixDataIOHandler.class.getName());
  /**
   * Adds the provided file to a global output that can be for example a JAR.
   *
   * @throws IOException
   */
  abstract void addFileToOutput(File file) throws IOException;

  /**
   * Creates a new file from the provided path.
   */
  abstract File createFile(String path);

  /**
   * Releases the resources used by the underlying implementation if any.
   */
  abstract void close();

  /**
   * Closes the provided file and logs any potential IOException.
   */
  void closeFile(Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage());
    }
  }
}
