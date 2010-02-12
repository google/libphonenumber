/*
 * Copyright (C) 2009 Google Inc.
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

import junit.framework.TestCase;

import java.io.InputStream;

/**
 * Unit tests for PhoneNumberUtil.java
 *
 * Note that these tests use the metadata contained in the file specified by TEST_META_DATA_FILE,
 * not the normal metadata file, so should not be used for regression test purposes - these tests
 * are illustrative only and test functionality.
 *
 * @author Shaopeng Jia
 */
public class AsYouTypeFormatterTest extends TestCase {
  private PhoneNumberUtil phoneUtil;
  private static final String TEST_META_DATA_FILE =
      "/com/google/i18n/phonenumbers/test/generated_files/PhoneNumberMetadataProtoForTesting";

  public AsYouTypeFormatterTest() {
    PhoneNumberUtil.resetInstance();
    InputStream in = PhoneNumberUtilTest.class.getResourceAsStream(TEST_META_DATA_FILE);
    phoneUtil = PhoneNumberUtil.getInstance(in);
  }

  public void testAsYouTypeFormatterUS() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("US");
    assertEquals("6", formatter.inputDigit('6'));
    assertEquals("65", formatter.inputDigit('5'));
    assertEquals("650", formatter.inputDigit('0'));
    assertEquals("6502", formatter.inputDigit('2'));
    assertEquals("65025", formatter.inputDigit('5'));
    assertEquals("650 253 \u2008\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("650 253 2\u2008\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 22\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 222\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 2222", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("6", formatter.inputDigit('6'));
    assertEquals("65", formatter.inputDigit('5'));
    assertEquals("650", formatter.inputDigit('0'));
    assertEquals("6502", formatter.inputDigit('2'));
    assertEquals("65025", formatter.inputDigit('5'));
    assertEquals("650 253 \u2008\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("650 253 2\u2008\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 22\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 222\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 2222", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("6", formatter.inputDigit('6'));
    assertEquals("65", formatter.inputDigit('5'));
    assertEquals("650", formatter.inputDigit('0'));
    assertEquals("650-", formatter.inputDigit('-'));
    assertEquals("650-2", formatter.inputDigit('2'));
    assertEquals("650-25", formatter.inputDigit('5'));
    assertEquals("650 253 \u2008\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("650 253 \u2008\u2008\u2008\u2008", formatter.inputDigit('-'));
    assertEquals("650 253 2\u2008\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 22\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 222\u2008", formatter.inputDigit('2'));
    assertEquals("650 253 2222", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011", formatter.inputDigit('1'));
    assertEquals("0114", formatter.inputDigit('4'));
    assertEquals("01148", formatter.inputDigit('8'));
    assertEquals("011 48 8", formatter.inputDigit('8'));
    assertEquals("011 48 88", formatter.inputDigit('8'));
    assertEquals("011 48 881", formatter.inputDigit('1'));
    assertEquals("011 48 88 12\u2008 \u2008\u2008 \u2008\u2008", formatter.inputDigit('2'));
    assertEquals("011 48 88 123 \u2008\u2008 \u2008\u2008", formatter.inputDigit('3'));
    assertEquals("011 48 88 123 1\u2008 \u2008\u2008", formatter.inputDigit('1'));
    assertEquals("011 48 88 123 12 \u2008\u2008", formatter.inputDigit('2'));
    assertEquals("011 48 88 123 12 1\u2008", formatter.inputDigit('1'));
    assertEquals("011 48 88 123 12 12", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("01", formatter.inputDigit('1'));
    assertEquals("011", formatter.inputDigit('1'));
    assertEquals("0114", formatter.inputDigit('4'));
    assertEquals("01144", formatter.inputDigit('4'));
    assertEquals("011 44 6", formatter.inputDigit('6'));
    assertEquals("011 44 61", formatter.inputDigit('1'));
    assertEquals("011 44 612", formatter.inputDigit('2'));
    assertEquals("011 44 6 123 \u2008\u2008\u2008 \u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("011 44 6 123 1\u2008\u2008 \u2008\u2008\u2008", formatter.inputDigit('1'));
    assertEquals("011 44 6 123 12\u2008 \u2008\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("011 44 6 123 123 \u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("011 44 6 123 123 1\u2008\u2008", formatter.inputDigit('1'));
    assertEquals("011 44 6 123 123 12\u2008", formatter.inputDigit('2'));
    assertEquals("011 44 6 123 123 123", formatter.inputDigit('3'));

    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+1", formatter.inputDigit('1'));
    assertEquals("+16", formatter.inputDigit('6'));
    assertEquals("+165", formatter.inputDigit('5'));
    assertEquals("+1650", formatter.inputDigit('0'));
    assertEquals("+1 650 2\u2008\u2008 \u2008\u2008\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("+1 650 25\u2008 \u2008\u2008\u2008\u2008", formatter.inputDigit('5'));
    assertEquals("+1 650 253 \u2008\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("+1 650 253 2\u2008\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("+1 650 253 22\u2008\u2008", formatter.inputDigit('2'));
    assertEquals("+1 650 253 222\u2008", formatter.inputDigit('2'));

    formatter.clear();
    assertEquals("+", formatter.inputDigit('+'));
    assertEquals("+4", formatter.inputDigit('4'));
    assertEquals("+48", formatter.inputDigit('8'));
    assertEquals("+488", formatter.inputDigit('8'));
    assertEquals("+4888", formatter.inputDigit('8'));
    assertEquals("+48 881", formatter.inputDigit('1'));
    assertEquals("+48 88 12\u2008 \u2008\u2008 \u2008\u2008", formatter.inputDigit('2'));
    assertEquals("+48 88 123 \u2008\u2008 \u2008\u2008", formatter.inputDigit('3'));
    assertEquals("+48 88 123 1\u2008 \u2008\u2008", formatter.inputDigit('1'));
    assertEquals("+48 88 123 12 \u2008\u2008", formatter.inputDigit('2'));
    assertEquals("+48 88 123 12 1\u2008", formatter.inputDigit('1'));
    assertEquals("+48 88 123 12 12", formatter.inputDigit('2'));

    // Test US number with full-width characters.
    formatter.clear();
    assertEquals("\uFF16", formatter.inputDigit('\uFF16'));
    assertEquals("\uFF16\uFF15", formatter.inputDigit('\uFF15'));
    assertEquals("\uFF16\uFF15\uFF10", formatter.inputDigit('\uFF10'));
    assertEquals("\uFF16\uFF15\uFF10\uFF12", formatter.inputDigit('\uFF12'));
    assertEquals("\uFF16\uFF15\uFF10\uFF12\uFF15", formatter.inputDigit('\uFF15'));
    assertEquals("650 253 \u2008\u2008\u2008\u2008", formatter.inputDigit('\uFF13'));
    assertEquals("650 253 2\u2008\u2008\u2008", formatter.inputDigit('\uFF12'));
    assertEquals("650 253 22\u2008\u2008", formatter.inputDigit('\uFF12'));
    assertEquals("650 253 222\u2008", formatter.inputDigit('\uFF12'));
    assertEquals("650 253 2222", formatter.inputDigit('\uFF12'));

    // Mobile short code.
    formatter.clear();
    assertEquals("*", formatter.inputDigit('*'));
    assertEquals("*1", formatter.inputDigit('1'));
    assertEquals("*12", formatter.inputDigit('2'));
    assertEquals("*121", formatter.inputDigit('1'));
    assertEquals("*121#", formatter.inputDigit('#'));
  }

  public void testAsYouTypeFormatterGBFixedLine() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("GB");
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("02", formatter.inputDigit('2'));
    assertEquals("020", formatter.inputDigit('0'));
    assertEquals("0207", formatter.inputDigit('7'));
    assertEquals("02070", formatter.inputDigit('0'));
    assertEquals("020 703\u2008 \u2008\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("020 7031 \u2008\u2008\u2008\u2008", formatter.inputDigit('1'));
    assertEquals("020 7031 3\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("020 7031 30\u2008\u2008", formatter.inputDigit('0'));
    assertEquals("020 7031 300\u2008", formatter.inputDigit('0'));
    assertEquals("020 7031 3000", formatter.inputDigit('0'));
  }

  public void testAsYouTypeFormatterGBTollFree() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("GB");
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("08", formatter.inputDigit('8'));
    assertEquals("080", formatter.inputDigit('0'));
    assertEquals("0807", formatter.inputDigit('7'));
    assertEquals("08070", formatter.inputDigit('0'));
    assertEquals("080 703\u2008 \u2008\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("080 7031 \u2008\u2008\u2008\u2008", formatter.inputDigit('1'));
    assertEquals("080 7031 3\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("080 7031 30\u2008\u2008", formatter.inputDigit('0'));
    assertEquals("080 7031 300\u2008", formatter.inputDigit('0'));
    assertEquals("080 7031 3000", formatter.inputDigit('0'));
  }

  public void testAsYouTypeFormatterGBPremiumRate() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("GB");
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("09", formatter.inputDigit('9'));
    assertEquals("090", formatter.inputDigit('0'));
    assertEquals("0907", formatter.inputDigit('7'));
    assertEquals("09070", formatter.inputDigit('0'));
    assertEquals("090 703\u2008 \u2008\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("090 7031 \u2008\u2008\u2008\u2008", formatter.inputDigit('1'));
    assertEquals("090 7031 3\u2008\u2008\u2008", formatter.inputDigit('3'));
    assertEquals("090 7031 30\u2008\u2008", formatter.inputDigit('0'));
    assertEquals("090 7031 300\u2008", formatter.inputDigit('0'));
    assertEquals("090 7031 3000", formatter.inputDigit('0'));
  }

  public void testAsYouTypeFormatterNZMobile() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("NZ");
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("02", formatter.inputDigit('2'));
    assertEquals("021", formatter.inputDigit('1'));
    assertEquals("0211", formatter.inputDigit('1'));
    assertEquals("02112", formatter.inputDigit('2'));
    assertEquals("021123", formatter.inputDigit('3'));
    assertEquals("0211234", formatter.inputDigit('4'));
    assertEquals("02112345", formatter.inputDigit('5'));
    assertEquals("021123456", formatter.inputDigit('6'));
  }

  public void testAsYouTypeFormatterDE() {
    AsYouTypeFormatter formatter = phoneUtil.getAsYouTypeFormatter("DE");
    assertEquals("0", formatter.inputDigit('0'));
    assertEquals("03", formatter.inputDigit('3'));
    assertEquals("030", formatter.inputDigit('0'));
    assertEquals("0301", formatter.inputDigit('1'));
    assertEquals("03012", formatter.inputDigit('2'));
    assertEquals("030123", formatter.inputDigit('3'));
    assertEquals("0301234", formatter.inputDigit('4'));
  }
}
