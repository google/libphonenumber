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

package com.google.i18n.phonenumbers.metadata;

import com.google.i18n.phonenumbers.MetadataLoader;
import com.google.i18n.phonenumbers.metadata.init.ClassPathResourceMetadataLoader;
import com.google.i18n.phonenumbers.metadata.init.MetadataParser;
import com.google.i18n.phonenumbers.metadata.source.FormattingMetadataSource;
import com.google.i18n.phonenumbers.metadata.source.FormattingMetadataSourceImpl;
import com.google.i18n.phonenumbers.metadata.source.MetadataSource;
import com.google.i18n.phonenumbers.metadata.source.MetadataSourceImpl;
import com.google.i18n.phonenumbers.metadata.source.RegionMetadataSource;
import com.google.i18n.phonenumbers.metadata.source.RegionMetadataSourceImpl;
import com.google.i18n.phonenumbers.metadata.source.SingleFileModeFileNameProvider;

/**
 * Provides metadata init & source dependencies when metadata is stored in multi-file mode and
 * loaded as a classpath resource.
 */
public final class DefaultMetadataDependenciesProvider {

  private static final DefaultMetadataDependenciesProvider INSTANCE = new DefaultMetadataDependenciesProvider();

  public static DefaultMetadataDependenciesProvider getInstance() {
    return INSTANCE;
  }

  private DefaultMetadataDependenciesProvider() {}

  private final MetadataParser metadataParser = MetadataParser.newStrictParser();
  private final MetadataLoader metadataLoader = new ClassPathResourceMetadataLoader();
  private final MetadataSource phoneNumberMetadataSource =
      new MetadataSourceImpl(
          new SingleFileModeFileNameProvider(
              "/com/google/i18n/phonenumbers/buildtools/PhoneNumberMetadataProto"),
          metadataLoader,
          metadataParser);
  private final RegionMetadataSource shortNumberMetadataSource =
      new RegionMetadataSourceImpl(
          new SingleFileModeFileNameProvider(
              "/com/google/i18n/phonenumbers/buildtools/ShortNumberMetadataProto"),
          metadataLoader,
          metadataParser);
  private final FormattingMetadataSource alternateFormatsMetadataSource =
      new FormattingMetadataSourceImpl(
          new SingleFileModeFileNameProvider(
              "/com/google/i18n/phonenumbers/buildtools/PhoneNumberAlternateFormatsProto"),
          metadataLoader,
          metadataParser);

  public MetadataParser getMetadataParser() {
    return metadataParser;
  }

  public MetadataLoader getMetadataLoader() {
    return metadataLoader;
  }

  public MetadataSource getPhoneNumberMetadataSource() {
    return phoneNumberMetadataSource;
  }

  public RegionMetadataSource getShortNumberMetadataSource() {
    return shortNumberMetadataSource;
  }

  public FormattingMetadataSource getAlternateFormatsMetadataSource() {
    return alternateFormatsMetadataSource;
  }

  public String getCarrierDataDirectory() {
    return "/com/google/i18n/phonenumbers/buildtools/carrier_data/";
  }

  public String getGeocodingDataDirectory() {
    return "/com/google/i18n/phonenumbers/buildtools/geocoding_data/";
  }
}
