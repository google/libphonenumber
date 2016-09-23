/*
 * Copyright (C) 2015 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;

/**
 * A source for phone metadata for all regions.
 */
interface MetadataSource {

  /**
   * Gets phone metadata for a region.
   * @param regionCode the region code.
   * @return the phone metadata for that region, or null if there is none.
   */
  PhoneMetadata getMetadataForRegion(String regionCode);

  /**
   * Gets phone metadata for a non-geographical region.
   * @param countryCallingCode the country calling code.
   * @return the phone metadata for that region, or null if there is none.
   */
  PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode);
}
