/*
 *  Copyright (C) 2011 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers;

import java.io.Closeable;
import java.io.IOException;

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
}
