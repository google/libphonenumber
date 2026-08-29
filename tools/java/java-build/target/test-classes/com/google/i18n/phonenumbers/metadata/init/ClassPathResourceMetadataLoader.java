/*
 * Copyright (C) 2022 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.metadata.init;

import com.google.i18n.phonenumbers.MetadataLoader;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link MetadataLoader} implementation that reads phone number metadata files as classpath
 * resources.
 */
public final class ClassPathResourceMetadataLoader implements MetadataLoader {

  private static final Logger logger =
      Logger.getLogger(ClassPathResourceMetadataLoader.class.getName());

  @Override
  public InputStream loadMetadata(String metadataFileName) {
    InputStream inputStream =
        ClassPathResourceMetadataLoader.class.getResourceAsStream(metadataFileName);
    if (inputStream == null) {
      logger.log(Level.WARNING, String.format("File %s not found", metadataFileName));
    }
    return inputStream;
  }
}