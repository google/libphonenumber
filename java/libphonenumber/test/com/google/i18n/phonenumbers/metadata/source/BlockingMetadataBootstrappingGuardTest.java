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

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.i18n.phonenumbers.MetadataLoader;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import com.google.i18n.phonenumbers.metadata.PhoneMetadataCollectionUtil;
import com.google.i18n.phonenumbers.metadata.init.MetadataParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mockito;

public class BlockingMetadataBootstrappingGuardTest extends TestCase {

  private static final String PHONE_METADATA_FILE = "some metadata file";
  private static final PhoneMetadataCollection PHONE_METADATA =
      PhoneMetadataCollection.newBuilder()
          .addMetadata(PhoneMetadata.newBuilder().setId("id").build());

  private final MetadataLoader metadataLoader = Mockito.mock(MetadataLoader.class);
  private final MetadataContainer metadataContainer = Mockito.mock(MetadataContainer.class);

  private BlockingMetadataBootstrappingGuard<MetadataContainer> bootstrappingGuard;

  @Override
  public void setUp() throws IOException {
    when(metadataLoader.loadMetadata(PHONE_METADATA_FILE))
        .thenReturn(PhoneMetadataCollectionUtil.toInputStream(PHONE_METADATA));
    bootstrappingGuard =
        new BlockingMetadataBootstrappingGuard<>(
            metadataLoader, MetadataParser.newStrictParser(), metadataContainer);
  }

  public void test_getOrBootstrap_shouldInvokeBootstrappingOnlyOnce() {
    bootstrappingGuard.getOrBootstrap(PHONE_METADATA_FILE);
    bootstrappingGuard.getOrBootstrap(PHONE_METADATA_FILE);

    verify(metadataLoader, times(1)).loadMetadata(PHONE_METADATA_FILE);
  }

  public void test_getOrBootstrap_shouldIncludeFileNameInExceptionOnFailure() {
    when(metadataLoader.loadMetadata(PHONE_METADATA_FILE)).thenReturn(null);

    ThrowingRunnable throwingRunnable =
        new ThrowingRunnable() {
          @Override
          public void run() {
            bootstrappingGuard.getOrBootstrap(PHONE_METADATA_FILE);
          }
        };

    IllegalStateException exception = assertThrows(IllegalStateException.class, throwingRunnable);
    Assert.assertTrue(exception.getMessage().contains(PHONE_METADATA_FILE));
  }

  public void test_getOrBootstrap_shouldInvokeBootstrappingOnlyOnceWhenThreadsCallItAtTheSameTime()
      throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    List<BootstrappingRunnable> runnables = new ArrayList<>();
    runnables.add(new BootstrappingRunnable());
    runnables.add(new BootstrappingRunnable());
    executorService.invokeAll(runnables);

    verify(metadataLoader, times(1)).loadMetadata(PHONE_METADATA_FILE);
  }

  private class BootstrappingRunnable implements Callable<MetadataContainer> {

    @Override
    public MetadataContainer call() {
      return bootstrappingGuard.getOrBootstrap(PHONE_METADATA_FILE);
    }
  }
}
