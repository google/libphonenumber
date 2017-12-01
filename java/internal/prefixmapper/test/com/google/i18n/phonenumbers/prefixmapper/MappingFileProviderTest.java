/*
 * Copyright (C) 2011 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.prefixmapper;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unittests for MappingFileProvider.java
 *
 * @author Shaopeng Jia
 */
public class MappingFileProviderTest extends TestCase {
  private final MappingFileProvider mappingProvider = new MappingFileProvider();
  private static final Logger logger = Logger.getLogger(MappingFileProviderTest.class.getName());

  public MappingFileProviderTest() {
    SortedMap<Integer, Set<String>> mapping = new TreeMap<Integer, Set<String>>();
    mapping.put(1, newHashSet("en"));
    mapping.put(86, newHashSet("zh", "en", "zh_Hant"));
    mapping.put(41, newHashSet("de", "fr", "it", "rm"));
    mapping.put(65, newHashSet("en", "zh_Hans", "ms", "ta"));

    mappingProvider.readFileConfigs(mapping);
  }

  private static HashSet<String> newHashSet(String... strings) {
    HashSet<String> set = new HashSet<String>();
    set.addAll(Arrays.asList(strings));
    return set;
  }

  public void testReadWriteExternal() {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      mappingProvider.writeExternal(objectOutputStream);
      objectOutputStream.flush();

      MappingFileProvider newMappingProvider = new MappingFileProvider();
      newMappingProvider.readExternal(
          new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));
      assertEquals(mappingProvider.toString(), newMappingProvider.toString());
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
      fail();
    }
  }

  public void testGetFileName() {
    assertEquals("1_en", mappingProvider.getFileName(1, "en", "", ""));
    assertEquals("1_en", mappingProvider.getFileName(1, "en", "", "US"));
    assertEquals("1_en", mappingProvider.getFileName(1, "en", "", "GB"));
    assertEquals("41_de", mappingProvider.getFileName(41, "de", "", "CH"));
    assertEquals("", mappingProvider.getFileName(44, "en", "", "GB"));
    assertEquals("86_zh", mappingProvider.getFileName(86, "zh", "", ""));
    assertEquals("86_zh", mappingProvider.getFileName(86, "zh", "Hans", ""));
    assertEquals("86_zh", mappingProvider.getFileName(86, "zh", "", "CN"));
    assertEquals("", mappingProvider.getFileName(86, "", "", "CN"));
    assertEquals("86_zh", mappingProvider.getFileName(86, "zh", "Hans", "CN"));
    assertEquals("86_zh", mappingProvider.getFileName(86, "zh", "Hans", "SG"));
    assertEquals("86_zh", mappingProvider.getFileName(86, "zh", "", "SG"));
    assertEquals("86_zh_Hant", mappingProvider.getFileName(86, "zh", "", "TW"));
    assertEquals("86_zh_Hant", mappingProvider.getFileName(86, "zh", "", "HK"));
    assertEquals("86_zh_Hant", mappingProvider.getFileName(86, "zh", "Hant", "TW"));
  }
}
