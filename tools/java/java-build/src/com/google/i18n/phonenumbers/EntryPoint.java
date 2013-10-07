/*
 *  Copyright (C) 2011 The Libphonenumber Authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.buildtools.GeneratePhonePrefixDataEntryPoint;
import com.google.i18n.phonenumbers.buildtools.GenerateTimeZonesMapDataEntryPoint;

/**
 * Entry point class for Java and JavaScript build tools.
 *
 * @author Philippe Liard
 */
public class EntryPoint {

  public static void main(String[] args) {
    boolean status = new CommandDispatcher(args, new Command[] {
      new BuildMetadataJsonFromXml(),
      new BuildMetadataProtoFromXml(),
      new GeneratePhonePrefixDataEntryPoint(),
      new GenerateTimeZonesMapDataEntryPoint(),
    }).start();

    System.exit(status ? 0 : 1);
  }
}
