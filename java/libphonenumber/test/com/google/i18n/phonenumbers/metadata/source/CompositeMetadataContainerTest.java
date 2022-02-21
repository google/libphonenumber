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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.internal.GeoEntityUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CompositeMetadataContainerTest {

  private static final String REGION_CODE = "US";
  private static final Integer COUNTRY_CODE = 1;
  private static final PhoneMetadata PHONE_METADATA_WITH_REGION_CODE =
      PhoneMetadata.newBuilder().setId(REGION_CODE).setCountryCode(COUNTRY_CODE);
  private static final PhoneMetadata PHONE_METADATA_WITH_COUNTRY_CODE =
      PhoneMetadata.newBuilder()
          .setId(GeoEntityUtility.REGION_CODE_FOR_NON_GEO_ENTITIES)
          .setCountryCode(COUNTRY_CODE);

  private CompositeMetadataContainer metadataContainer;

  @Before
  public void setUp() {
    metadataContainer = new CompositeMetadataContainer();
  }

  @Test
  public void getMetadataBy_shouldReturnNullForNonExistingRegionCode() {
    assertNull(metadataContainer.getMetadataBy(REGION_CODE));
  }

  @Test
  public void getMetadataBy_shouldReturnMetadataForExistingRegionCode() {
    metadataContainer.accept(PHONE_METADATA_WITH_REGION_CODE);

    assertSame(PHONE_METADATA_WITH_REGION_CODE, metadataContainer.getMetadataBy(REGION_CODE));
  }

  @Test
  public void getMetadataBy_shouldReturnNullForNonExistingCountryCode() {
    assertNull(metadataContainer.getMetadataBy(COUNTRY_CODE));
  }

  @Test
  public void getMetadataBy_shouldReturnMetadataForExistingCountryCode() {
    metadataContainer.accept(PHONE_METADATA_WITH_COUNTRY_CODE);

    assertSame(PHONE_METADATA_WITH_COUNTRY_CODE, metadataContainer.getMetadataBy(COUNTRY_CODE));
  }

  @Test
  public void getMetadataBy_shouldReturnNullForExistingCountryCodeOfGeoRegion() {
    metadataContainer.accept(PHONE_METADATA_WITH_REGION_CODE);

    assertNull(metadataContainer.getMetadataBy(COUNTRY_CODE));
  }
}
