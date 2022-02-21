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

/** A source of formatting phone metadata. */
public interface FormattingMetadataSource {

  /**
   * Returns formatting phone metadata for provided country calling code.
   *
   * <p>This method is similar to the one in {@link
   * NonGeographicalEntityMetadataSource#getMetadataForNonGeographicalRegion(int)}, except that it
   * will not fail for geographical regions, it can be used for both geo- and non-geo entities.
   *
   * <p>In case the provided {@code countryCallingCode} maps to several different regions, only one
   * would contain formatting metadata.
   *
   * @return the phone metadata for provided {@code countryCallingCode}, or null if there is none.
   */
  PhoneMetadata getFormattingMetadataForCountryCallingCode(int countryCallingCode);
}
