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

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exposes single method for parsing {@link InputStream} content into {@link Collection} of {@link
 * PhoneMetadata}
 */
public final class MetadataParser {

  private static final Logger logger = Logger.getLogger(MetadataParser.class.getName());

  /**
   * Creates new instance in lenient mode, see {@link MetadataParser#parse(InputStream)} for more
   * info.
   */
  public static MetadataParser newLenientParser() {
    return new MetadataParser(false);
  }

  /**
   * Creates new instance in strict mode, see {@link MetadataParser#parse(InputStream)} for more
   * info
   */
  public static MetadataParser newStrictParser() {
    return new MetadataParser(true);
  }

  private final boolean strictMode;

  private MetadataParser(boolean strictMode) {
    this.strictMode = strictMode;
  }

  /**
   * Parses given {@link InputStream} into a {@link Collection} of {@link PhoneMetadata}.
   *
   * @throws IllegalArgumentException if {@code source} is {@code null} and strict mode is on
   * @return parsed {@link PhoneMetadata}, or empty {@link Collection} if {@code source} is {@code
   *     null} and lenient mode is on
   */
  public Collection<PhoneMetadata> parse(InputStream source) {
    if (source == null) {
      return handleNullSource();
    }
    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(source);
      PhoneMetadataCollection phoneMetadataCollection = new PhoneMetadataCollection();
      phoneMetadataCollection.readExternal(ois);
      List<PhoneMetadata> phoneMetadata = phoneMetadataCollection.getMetadataList();
      // Sanity check; this should not happen if provided InputStream is valid
      if (phoneMetadata.isEmpty()) {
        throw new IllegalStateException("Empty metadata");
      }
      return phoneMetadataCollection.getMetadataList();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to parse metadata file", e);
    } finally {
      if (ois != null) {
        // This will close all underlying streams as well, including source.
        close(ois);
      } else {
        close(source);
      }
    }
  }

  private List<PhoneMetadata> handleNullSource() {
    if (strictMode) {
      throw new IllegalArgumentException("Source cannot be null");
    }
    return Collections.emptyList();
  }

  private void close(InputStream inputStream) {
    try {
      inputStream.close();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error closing input stream (ignored)", e);
    }
  }
}