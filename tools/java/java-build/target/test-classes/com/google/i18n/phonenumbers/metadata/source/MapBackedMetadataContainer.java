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

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link MetadataContainer} implementation backed by a {@link ConcurrentHashMap} with generic
 * keys.
 */
final class MapBackedMetadataContainer<T> implements MetadataContainer {

  static MapBackedMetadataContainer<String> byRegionCode() {
    return new MapBackedMetadataContainer<>(
        new KeyProvider<String>() {
          @Override
          public String getKeyOf(PhoneMetadata phoneMetadata) {
            return phoneMetadata.getId();
          }
        });
  }

  static MapBackedMetadataContainer<Integer> byCountryCallingCode() {
    return new MapBackedMetadataContainer<>(
        new KeyProvider<Integer>() {
          @Override
          public Integer getKeyOf(PhoneMetadata phoneMetadata) {
            return phoneMetadata.getCountryCode();
          }
        });
  }

  private final ConcurrentMap<T, PhoneMetadata> metadataMap;

  private final KeyProvider<T> keyProvider;

  private MapBackedMetadataContainer(KeyProvider<T> keyProvider) {
    this.metadataMap = new ConcurrentHashMap<>();
    this.keyProvider = keyProvider;
  }

  PhoneMetadata getMetadataBy(T key) {
    return key != null ? metadataMap.get(key) : null;
  }

  KeyProvider<T> getKeyProvider() {
    return keyProvider;
  }

  @Override
  public void accept(PhoneMetadata phoneMetadata) {
    metadataMap.put(keyProvider.getKeyOf(phoneMetadata), phoneMetadata);
  }

  interface KeyProvider<T> {
    T getKeyOf(PhoneMetadata phoneMetadata);
  }
}
