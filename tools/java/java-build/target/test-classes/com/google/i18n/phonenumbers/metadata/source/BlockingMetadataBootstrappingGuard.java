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

package com.google.i18n.phonenumbers.metadata.source;

import com.google.i18n.phonenumbers.MetadataLoader;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.metadata.init.MetadataParser;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A blocking implementation of {@link MetadataBootstrappingGuard}. Can be used for both single-file
 * (bulk) and multi-file metadata
 *
 * @param <T> needs to extend {@link MetadataContainer}
 */
final class BlockingMetadataBootstrappingGuard<T extends MetadataContainer>
    implements MetadataBootstrappingGuard<T> {

  private final MetadataLoader metadataLoader;
  private final MetadataParser metadataParser;
  private final T metadataContainer;
  private final Map<String, String> loadedFiles; // identity map

  BlockingMetadataBootstrappingGuard(
      MetadataLoader metadataLoader, MetadataParser metadataParser, T metadataContainer) {
    this.metadataLoader = metadataLoader;
    this.metadataParser = metadataParser;
    this.metadataContainer = metadataContainer;
    this.loadedFiles = new ConcurrentHashMap<>();
  }

  @Override
  public T getOrBootstrap(String phoneMetadataFile) {
    if (!loadedFiles.containsKey(phoneMetadataFile)) {
      bootstrapMetadata(phoneMetadataFile);
    }
    return metadataContainer;
  }

  private synchronized void bootstrapMetadata(String phoneMetadataFile) {
    // Additional check is needed because multiple threads could pass the first check when calling
    // getOrBootstrap() at the same time for unloaded metadata file
    if (loadedFiles.containsKey(phoneMetadataFile)) {
      return;
    }
    Collection<PhoneMetadata> phoneMetadata = read(phoneMetadataFile);
    for (PhoneMetadata metadata : phoneMetadata) {
      metadataContainer.accept(metadata);
    }
    loadedFiles.put(phoneMetadataFile, phoneMetadataFile);
  }

  private Collection<PhoneMetadata> read(String phoneMetadataFile) {
    try {
      InputStream metadataStream = metadataLoader.loadMetadata(phoneMetadataFile);
      return metadataParser.parse(metadataStream);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new IllegalStateException("Failed to read file " + phoneMetadataFile, e);
    }
  }
}
