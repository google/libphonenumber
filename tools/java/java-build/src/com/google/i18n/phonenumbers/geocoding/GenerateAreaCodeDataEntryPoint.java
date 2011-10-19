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

package com.google.i18n.phonenumbers.geocoding;

import com.google.i18n.phonenumbers.Command;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point class used to invoke the generation of the binary geocoding data files.
 *
 * @author Philippe Liard
 */
public class GenerateAreaCodeDataEntryPoint extends Command {
  private static final Logger LOGGER = Logger.getLogger(GenerateAreaCodeData.class.getName());

  /**
   * Implementation of the IOHandler required by the GenerateAreaCodeData class used here to create
   * the output files.
   */
  private static class IOHandler extends GenerateAreaCodeData.IOHandler {
    // The path to the output directory.
    private final File outputPath;

    public IOHandler(File outputPath) throws IOException {
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

    @Override
    public void addFileToOutput(File file) throws IOException {
      // Do nothing. This would be the place dealing with the addition of the provided file to the
      // resulting JAR if the global output was a JAR instead of a directory containing the binary
      // files.
    }

    @Override
    public File createFile(String path) {
      return new File(outputPath, path);
    }

    @Override
    public void close() {
      // Do nothing as no resource needs to be released.
    }
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
          new GenerateAreaCodeData(new File(args[1]), new IOHandler(new File(args[2])));
      generateAreaCodeData.run();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
      return false;
    }
    return true;
  }
}
