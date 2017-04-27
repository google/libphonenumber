/*
 * Copyright (C) 2010 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.internal;

import junit.framework.TestCase;

/**
 * Unittests for LRU Cache for compiled regular expressions used by the libphonenumbers libary.
 *
 * @author Shaopeng Jia
 */

public class RegexCacheTest extends TestCase {
  private RegexCache regexCache;

  public RegexCacheTest() {
    regexCache = new RegexCache(2);
  }

  public void testRegexInsertion() {
    final String regex1 = "[1-5]";
    final String regex2 = "(?:12|34)";
    final String regex3 = "[1-3][58]";

    regexCache.getPatternForRegex(regex1);
    assertTrue(regexCache.containsRegex(regex1));

    regexCache.getPatternForRegex(regex2);
    assertTrue(regexCache.containsRegex(regex2));
    assertTrue(regexCache.containsRegex(regex1));

    regexCache.getPatternForRegex(regex1);
    assertTrue(regexCache.containsRegex(regex1));

    regexCache.getPatternForRegex(regex3);
    assertTrue(regexCache.containsRegex(regex3));

    assertFalse(regexCache.containsRegex(regex2));
    assertTrue(regexCache.containsRegex(regex1));
  }
}
