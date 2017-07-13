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
 *
 * @author Philippe Liard
 */
public class GeneratePhonePrefixDataEntryPoint extends Command {
  private static final Logger logger = Logger.getLogger(GeneratePhonePrefixData.class.getName());

  @Override
  public String getCommandName() {
    return "GeneratePhonePrefixData";
  }

  @Override
  public boolean start() {
    String[] args = getArgs();

    if (args.length != 3) {
      logger.log(Level.SEVERE,
                 "usage: GeneratePhonePrefixData /path/to/input/directory "
                 + "/path/to/output/directory");
      return false;
    }
    try {
      GeneratePhonePrefixData generatePhonePrefixData =
          new GeneratePhonePrefixData(new File(args[1]), new PhonePrefixDataIOHandler(new File(args[2])));
      generatePhonePrefixData.run();
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
      return false;
    }
    return true;
  }
}
