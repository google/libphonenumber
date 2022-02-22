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

import com.google.i18n.phonenumbers.Command;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point class used to invoke the generation of the binary phone prefix data files.
 */
public class GeneratePhonePrefixDataEntryPoint extends Command {

  private static final Logger logger = Logger.getLogger(GeneratePhonePrefixData.class.getName());
  private static final String USAGE_DESCRIPTION =
      "usage: GeneratePhonePrefixData /path/to/input/directory /path/to/output/directory"
          + " [outputJarName]";

  @Override
  public String getCommandName() {
    return "GeneratePhonePrefixData";
  }

  @Override
  public boolean start() {
    String[] args = getArgs();

    if (args.length < 3 || args.length > 4) {
      logger.log(Level.SEVERE, USAGE_DESCRIPTION);
      return false;
    }
    try {
      File inputPath = new File(args[1]);
      File outputPath = new File(args[2]);
      AbstractPhonePrefixDataIOHandler ioHandler =
          args.length == 3
              ? new PhonePrefixDataIOHandler(outputPath)
              : new JarPhonePrefixDataIOHandler(
                  outputPath, args[3], GeneratePhonePrefixData.class.getPackage());
      GeneratePhonePrefixData dataGenerator = new GeneratePhonePrefixData(inputPath, ioHandler);
      dataGenerator.run();
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
      return false;
    }
    return true;
  }
}
