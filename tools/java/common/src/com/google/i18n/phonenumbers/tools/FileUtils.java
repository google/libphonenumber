/*
 *  Copyright (C) 2011 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package com.google.i18n.phonenumbers.tools;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class containing methods designed to ease file manipulation and generation.
 *
 * @author Philippe Liard
 */
public class FileUtils {
  /**
   * Silently closes a resource (i.e: don't throw any exception).
   */
  private static void close(Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Silently closes multiple resources. This method doesn't throw any exception when an error
   * occurs when a resource is being closed.
   */
  public static void closeFiles(Closeable ... closeables) {
    for (Closeable closeable : closeables) {
      close(closeable);
    }
  }

  /**
   * Returns true if the provided output file/directory is older than the provided input
   * file/directory. The last modification time of a directory is the maximum last
   * modification time of its children. It assumes the provided output directory only contains
   * generated files.
   */
  public static boolean isGenerationRequired(File inputFile, File outputDir) {
    if (!outputDir.exists()) {
      return true;
    }
    return getLastModificationTime(inputFile) > getLastModificationTime(outputDir);
  }

  /**
   * Gets the modification time of the most recently modified file contained in a directory.
   */
  private static long getLastModificationTime(File file) {
    if (!file.isDirectory()) {
      return file.lastModified();
    }
    long maxModificationTime = 0;

    for (File child : file.listFiles()) {
      long modificationTime = getLastModificationTime(child);

      if (modificationTime > maxModificationTime) {
        maxModificationTime = modificationTime;
      }
    }
    return maxModificationTime;
  }
}
