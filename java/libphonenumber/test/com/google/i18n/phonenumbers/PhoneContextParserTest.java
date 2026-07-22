/*
 * Copyright (C) 2025 The Libphonenumber Authors
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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.function.ThrowingRunnable;
import com.google.i18n.phonenumbers.PhoneContextParser.PhoneContext;
import junit.framework.TestCase;

/**
 * Unit tests for PhoneContextParser.java
 */
public class PhoneContextParserTest extends TestCase {

  private static final Set<Integer> countryCallingCodeSet;
  static {
    Set<Integer> tempSet = new HashSet<>();
    tempSet.add(64);
    countryCallingCodeSet = Collections.unmodifiableSet(tempSet);
  }

  /**
   * An instance of PhoneContextParser.
   */
  protected final PhoneContextParser phoneContextParser =
      new PhoneContextParser(countryCallingCodeSet);

  public void testParseShouldWorkAsExpected() throws NumberParseException {
    PhoneContext actual;

    actual = phoneContextParser.parse("tel:03-331-6005;phone-context=+64");
    assertEquals("+64", actual.getRawContext());
    assertEquals(new Integer(64), actual.getCountryCode());
    
    actual = phoneContextParser.parse("tel:03-331-6005;phone-context=example.com");
    assertEquals("example.com", actual.getRawContext());
    assertNull(actual.getCountryCode());
    
    actual = phoneContextParser.parse("03-331-6005;phone-context=+64;");
    assertEquals("+64", actual.getRawContext());
    assertEquals(new Integer(64), actual.getCountryCode());
    
    actual = phoneContextParser.parse("+64-3-331-6005;phone-context=+64;");
    assertEquals("+64", actual.getRawContext());
    assertEquals(new Integer(64), actual.getCountryCode());
    
    actual = phoneContextParser.parse("tel:03-331-6005;foo=bar;phone-context=+64;baz=qux");
    assertEquals("+64", actual.getRawContext());
    assertEquals(new Integer(64), actual.getCountryCode());
    
    actual = phoneContextParser.parse("tel:03-331-6005");
    assertNull(actual);
    
    actual = phoneContextParser.parse("tel:03-331-6005;phone-context=+0");
    assertEquals("+0", actual.getRawContext());
    assertNull(actual.getCountryCode());
    
    actual = phoneContextParser.parse("tel:03-331-6005;phone-context=+1234");
    assertEquals("+1234", actual.getRawContext());
    assertNull(actual.getCountryCode());
  }

  public void testParseShouldFailForInvalidPhoneContext() throws NumberParseException {
    assertThrows(
        NumberParseException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws NumberParseException {
            phoneContextParser.parse("tel:03-331-6005;phone-context=");
          }
        });
    assertThrows(
        NumberParseException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws NumberParseException {
            phoneContextParser.parse("tel:03-331-6005;phone-context=;");
          }
        });
    assertThrows(
        NumberParseException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws NumberParseException {
            phoneContextParser.parse("tel:03-331-6005;phone-context=0");
          }
        });
  }

}
