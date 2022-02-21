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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import com.google.i18n.phonenumbers.metadata.PhoneMetadataCollectionUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class MetadataParserTest {

  private static final MetadataParser metadataParser = MetadataParser.newStrictParser();

  @Test
  public void parse_shouldThrowExceptionForNullInput() {
    assertThrows(
        IllegalArgumentException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            metadataParser.parse(null);
          }
        });
  }

  @Test
  public void parse_shouldThrowExceptionForEmptyInput() {
    final InputStream emptyInput = new ByteArrayInputStream(new byte[0]);

    assertThrows(
        IllegalStateException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            metadataParser.parse(emptyInput);
          }
        });
  }

  @Test
  public void parse_shouldThrowExceptionForInvalidInput() {
    final InputStream invalidInput = new ByteArrayInputStream("Some random input".getBytes(UTF_8));

    assertThrows(
        IllegalStateException.class,
        new ThrowingRunnable() {
          @Override
          public void run() {
            metadataParser.parse(invalidInput);
          }
        });
  }

  @Test
  public void parse_shouldParseValidInput() throws IOException {
    InputStream input = PhoneMetadataCollectionUtil.toInputStream(
        PhoneMetadataCollection.newBuilder()
            .addMetadata(PhoneMetadata.newBuilder().setId("id").build()));

    Collection<PhoneMetadata> actual = metadataParser.parse(input);

    assertEquals(1, actual.size());
  }

  @Test
  public void parse_shouldReturnEmptyCollectionForNullInput() {
    Collection<PhoneMetadata> actual = MetadataParser.newLenientParser().parse(null);

    assertTrue(actual.isEmpty());
  }
}