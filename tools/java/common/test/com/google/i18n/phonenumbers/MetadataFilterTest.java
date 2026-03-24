/*
 *  Copyright (C) 2016 The Libphonenumber Authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;
import com.google.i18n.phonenumbers.Phonemetadata.PhoneNumberDesc;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import junit.framework.TestCase;

/**
 * Unit tests for {@link MetadataFilter}.
 */
public class MetadataFilterTest extends TestCase {
  private static final String ID = "AM";
  private static final int COUNTRY_CODE = 374;
  private static final String INTERNATIONAL_PREFIX = "0[01]";
  private static final String PREFERRED_INTERNATIONAL_PREFIX = "00";
  private static final String NATIONAL_NUMBER_PATTERN = "\\d{8}";
  private static final int[] possibleLengths = {8};
  private static final int[] possibleLengthsLocalOnly = {5, 6};
  private static final String EXAMPLE_NUMBER = "10123456";

  // If this behavior changes then consider whether the change in the blacklist is intended, or you
  // should change the special build configuration. Also look into any change in the size of the
  // build.
  public void testForLiteBuild() {
    TreeMap<String, TreeSet<String>> blacklist = new TreeMap<String, TreeSet<String>>();
    blacklist.put("fixedLine", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("mobile", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("tollFree", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("premiumRate", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("sharedCost", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("personalNumber", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("voip", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("pager", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("uan", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("emergency", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("voicemail", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("shortCode", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("standardRate", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("carrierSpecific", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("smsServices", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("noInternationalDialling",
        new TreeSet<String>(Arrays.asList("exampleNumber")));

    assertEquals(MetadataFilter.forLiteBuild(), new MetadataFilter(blacklist));
  }

  // If this behavior changes then consider whether the change in the blacklist is intended, or you
  // should change the special build configuration. Also look into any change in the size of the
  // build.
  public void testForSpecialBuild() {
    TreeMap<String, TreeSet<String>> blacklist = new TreeMap<String, TreeSet<String>>();
    blacklist.put("fixedLine", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("tollFree", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("premiumRate", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("sharedCost", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("personalNumber", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("voip", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("pager", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("uan", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("emergency", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("voicemail", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("shortCode", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("standardRate", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("carrierSpecific", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("smsServices", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("noInternationalDialling",
        new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("preferredInternationalPrefix", new TreeSet<String>());
    blacklist.put("nationalPrefix", new TreeSet<String>());
    blacklist.put("preferredExtnPrefix", new TreeSet<String>());
    blacklist.put("nationalPrefixTransformRule", new TreeSet<String>());
    blacklist.put("sameMobileAndFixedLinePattern", new TreeSet<String>());
    blacklist.put("mainCountryForCode", new TreeSet<String>());
    blacklist.put("mobileNumberPortableRegion", new TreeSet<String>());

    assertEquals(MetadataFilter.forSpecialBuild(), new MetadataFilter(blacklist));
  }

  public void testEmptyFilter() {
    assertEquals(MetadataFilter.emptyFilter(),
        new MetadataFilter(new TreeMap<String, TreeSet<String>>()));
  }

  public void testParseFieldMapFromString_parentAsGroup() {
    TreeMap<String, TreeSet<String>> fieldMap = new TreeMap<String, TreeSet<String>>();
    fieldMap.put("fixedLine", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleLength", "possibleLengthLocalOnly", "exampleNumber")));

    assertEquals(MetadataFilter.parseFieldMapFromString("fixedLine"), fieldMap);
  }

  public void testParseFieldMapFromString_childAsGroup() {
    TreeMap<String, TreeSet<String>> fieldMap = new TreeMap<String, TreeSet<String>>();
    fieldMap.put("fixedLine", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("mobile", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("tollFree", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("premiumRate", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("sharedCost", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("personalNumber", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("voip", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("pager", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("uan", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("emergency", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("voicemail", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("shortCode", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("standardRate", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("carrierSpecific", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("smsServices", new TreeSet<String>(Arrays.asList("exampleNumber")));
    fieldMap.put("noInternationalDialling", new TreeSet<String>(Arrays.asList("exampleNumber")));

    assertEquals(MetadataFilter.parseFieldMapFromString("exampleNumber"), fieldMap);
  }

  public void testParseFieldMapFromString_childlessFieldAsGroup() {
    TreeMap<String, TreeSet<String>> fieldMap = new TreeMap<String, TreeSet<String>>();
    fieldMap.put("nationalPrefix", new TreeSet<String>());

    assertEquals(MetadataFilter.parseFieldMapFromString("nationalPrefix"), fieldMap);
  }

  public void testParseFieldMapFromString_parentWithOneChildAsGroup() {
    TreeMap<String, TreeSet<String>> fieldMap = new TreeMap<String, TreeSet<String>>();
    fieldMap.put("fixedLine", new TreeSet<String>(Arrays.asList("exampleNumber")));

    assertEquals(MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber)"), fieldMap);
  }

  public void testParseFieldMapFromString_parentWithTwoChildrenAsGroup() {
    TreeMap<String, TreeSet<String>> fieldMap = new TreeMap<String, TreeSet<String>>();
    fieldMap.put("fixedLine", new TreeSet<String>(Arrays.asList(
        "exampleNumber", "possibleLength")));

    assertEquals(
        MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber,possibleLength)"),
        fieldMap);
  }

  public void testParseFieldMapFromString_mixOfGroups() {
    TreeMap<String, TreeSet<String>> fieldMap = new TreeMap<String, TreeSet<String>>();
    fieldMap.put("uan", new TreeSet<String>(Arrays.asList(
        "possibleLength", "exampleNumber", "possibleLengthLocalOnly", "nationalNumberPattern")));
    fieldMap.put("pager", new TreeSet<String>(Arrays.asList(
        "exampleNumber", "nationalNumberPattern")));
    fieldMap.put("fixedLine", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleLength", "possibleLengthLocalOnly", "exampleNumber")));
    fieldMap.put("nationalPrefix", new TreeSet<String>());
    fieldMap.put("mobile", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("tollFree", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("premiumRate", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("sharedCost", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("personalNumber", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("voip", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("emergency", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("voicemail", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("shortCode", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("standardRate", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("carrierSpecific", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("smsServices", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    fieldMap.put("noInternationalDialling", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern")));

    assertEquals(MetadataFilter.parseFieldMapFromString(
        "uan(possibleLength,exampleNumber,possibleLengthLocalOnly)"
        + ":pager(exampleNumber)"
        + ":fixedLine"
        + ":nationalPrefix"
        + ":nationalNumberPattern"),
        fieldMap);
  }

  // Many of the strings in this test may be possible to express in shorter ways with the current
  // sets of excludable fields, but their shortest representation changes as those sets change, as
  // do their semantics; therefore we allow currently longer expressions, and we allow explicit
  // listing of children, even if these are currently all the children.
  public void testParseFieldMapFromString_equivalentExpressions() {
    // Listing all excludable parent fields is equivalent to listing all excludable child fields.
    assertEquals(
        MetadataFilter.parseFieldMapFromString(
            "fixedLine"
            + ":mobile"
            + ":tollFree"
            + ":premiumRate"
            + ":sharedCost"
            + ":personalNumber"
            + ":voip"
            + ":pager"
            + ":uan"
            + ":emergency"
            + ":voicemail"
            + ":shortCode"
            + ":standardRate"
            + ":carrierSpecific"
            + ":smsServices"
            + ":noInternationalDialling"),
        MetadataFilter.parseFieldMapFromString(
            "nationalNumberPattern"
            + ":possibleLength"
            + ":possibleLengthLocalOnly"
            + ":exampleNumber"));

    // Order and whitespace don't matter.
    assertEquals(
        MetadataFilter.parseFieldMapFromString(
            " nationalNumberPattern "
            + ": uan ( exampleNumber , possibleLengthLocalOnly,     possibleLength ) "
            + ": nationalPrefix "
            + ": fixedLine "
            + ": pager ( exampleNumber ) "),
        MetadataFilter.parseFieldMapFromString(
            "uan(possibleLength,exampleNumber,possibleLengthLocalOnly)"
            + ":pager(exampleNumber)"
            + ":fixedLine"
            + ":nationalPrefix"
            + ":nationalNumberPattern"));

    // Parent explicitly listing all possible children.
    assertEquals(
        MetadataFilter.parseFieldMapFromString(
            "uan(nationalNumberPattern,possibleLength,exampleNumber,possibleLengthLocalOnly)"),
        MetadataFilter.parseFieldMapFromString("uan"));

    // All parent's children covered, some implicitly and some explicitly.
    assertEquals(
        MetadataFilter.parseFieldMapFromString(
            "uan(nationalNumberPattern,possibleLength,exampleNumber):possibleLengthLocalOnly"),
        MetadataFilter.parseFieldMapFromString("uan:possibleLengthLocalOnly"));

    // Child field covered by all parents explicitly.
    // It seems this will always be better expressed as a wildcard child, but the check is complex
    // and may not be worth it.
    assertEquals(
        MetadataFilter.parseFieldMapFromString(
            "fixedLine(exampleNumber)"
            + ":mobile(exampleNumber)"
            + ":tollFree(exampleNumber)"
            + ":premiumRate(exampleNumber)"
            + ":sharedCost(exampleNumber)"
            + ":personalNumber(exampleNumber)"
            + ":voip(exampleNumber)"
            + ":pager(exampleNumber)"
            + ":uan(exampleNumber)"
            + ":emergency(exampleNumber)"
            + ":voicemail(exampleNumber)"
            + ":shortCode(exampleNumber)"
            + ":standardRate(exampleNumber)"
            + ":carrierSpecific(exampleNumber)"
            + ":smsServices(exampleNumber)"
            + ":noInternationalDialling(exampleNumber)"),
        MetadataFilter.parseFieldMapFromString("exampleNumber"));

    // Child field given as a group by itself while it's covered by all parents implicitly.
    // It seems this will always be better expressed without the wildcard child, but the check is
    // complex and may not be worth it.
    assertEquals(
        MetadataFilter.parseFieldMapFromString(
            "fixedLine"
            + ":mobile"
            + ":tollFree"
            + ":premiumRate"
            + ":sharedCost"
            + ":personalNumber"
            + ":voip"
            + ":pager"
            + ":uan"
            + ":emergency"
            + ":voicemail"
            + ":shortCode"
            + ":standardRate"
            + ":carrierSpecific"
            + ":smsServices"
            + ":noInternationalDialling"
            + ":exampleNumber"),
        MetadataFilter.parseFieldMapFromString(
            "fixedLine"
            + ":mobile"
            + ":tollFree"
            + ":premiumRate"
            + ":sharedCost"
            + ":personalNumber"
            + ":voip"
            + ":pager"
            + ":uan"
            + ":emergency"
            + ":voicemail"
            + ":shortCode"
            + ":standardRate"
            + ":carrierSpecific"
            + ":smsServices"
            + ":noInternationalDialling"));
  }

  public void testParseFieldMapFromString_RuntimeExceptionCases() {
    // Null input.
    try {
      MetadataFilter.parseFieldMapFromString(null);
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Empty input.
    try {
      MetadataFilter.parseFieldMapFromString("");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Whitespace input.
    try {
      MetadataFilter.parseFieldMapFromString(" ");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Bad token given as only group.
    try {
      MetadataFilter.parseFieldMapFromString("something_else");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Bad token given as last group.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine:something_else");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Bad token given as middle group.
    try {
      MetadataFilter.parseFieldMapFromString(
          "pager:nationalPrefix:something_else:nationalNumberPattern");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Childless field given as parent.
    try {
      MetadataFilter.parseFieldMapFromString("nationalPrefix(exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Child field given as parent.
    try {
      MetadataFilter.parseFieldMapFromString("possibleLength(exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Bad token given as parent.
    try {
      MetadataFilter.parseFieldMapFromString("something_else(exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Parent field given as only child.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(uan)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Parent field given as first child.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(uan,possibleLength)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Parent field given as last child.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(possibleLength,uan)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Parent field given as middle child.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(possibleLength,uan,exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Childless field given as only child.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(nationalPrefix)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Bad token given as only child.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(something_else)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Bad token given as last child.
    try {
      MetadataFilter.parseFieldMapFromString("uan(possibleLength,something_else)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Empty parent.
    try {
      MetadataFilter.parseFieldMapFromString("(exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Whitespace parent.
    try {
      MetadataFilter.parseFieldMapFromString(" (exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Empty child.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine()");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Whitespace child.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine( )");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Empty parent and child.
    try {
      MetadataFilter.parseFieldMapFromString("()");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Whitespace parent and empty child.
    try {
      MetadataFilter.parseFieldMapFromString(" ()");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Parent field given as a group twice.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine:uan:fixedLine");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Parent field given as the parent of a group and as a group by itself.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber):fixedLine");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Parent field given as the parent of one group and then as the parent of another group.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber):fixedLine(possibleLength)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Childless field given twice as a group.
    try {
      MetadataFilter.parseFieldMapFromString("nationalPrefix:uan:nationalPrefix");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Child field given twice as a group.
    try {
      MetadataFilter.parseFieldMapFromString("exampleNumber:uan:exampleNumber");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Child field given first as the only child in a group and then as a group by itself.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber):exampleNumber");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Child field given first as a child in a group and then as a group by itself.
    try {
      MetadataFilter.parseFieldMapFromString(
          "uan(nationalNumberPattern,possibleLength,exampleNumber)"
          + ":possibleLengthLocalOnly"
          + ":exampleNumber");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Child field given twice as children of the same parent.
    try {
      MetadataFilter.parseFieldMapFromString(
          "fixedLine(possibleLength,exampleNumber,possibleLength)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Child field given as a group by itself while it's covered by all parents explicitly.
    try {
      MetadataFilter.parseFieldMapFromString(
          "fixedLine(exampleNumber)"
          + ":mobile(exampleNumber)"
          + ":tollFree(exampleNumber)"
          + ":premiumRate(exampleNumber)"
          + ":sharedCost(exampleNumber)"
          + ":personalNumber(exampleNumber)"
          + ":voip(exampleNumber)"
          + ":pager(exampleNumber)"
          + ":uan(exampleNumber)"
          + ":emergency(exampleNumber)"
          + ":voicemail(exampleNumber)"
          + ":shortCode(exampleNumber)"
          + ":standardRate(exampleNumber)"
          + ":carrierSpecific(exampleNumber)"
          + ":noInternationalDialling(exampleNumber)"
          + ":exampleNumber");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Child field given as a group by itself while it's covered by all parents, some implicitly and
    // some explicitly.
    try {
      MetadataFilter.parseFieldMapFromString(
          "fixedLine"
          + ":mobile"
          + ":tollFree"
          + ":premiumRate"
          + ":sharedCost"
          + ":personalNumber"
          + ":voip"
          + ":pager(exampleNumber)"
          + ":uan(exampleNumber)"
          + ":emergency(exampleNumber)"
          + ":voicemail(exampleNumber)"
          + ":shortCode(exampleNumber)"
          + ":standardRate(exampleNumber)"
          + ":carrierSpecific(exampleNumber)"
          + ":smsServices"
          + ":noInternationalDialling(exampleNumber)"
          + ":exampleNumber");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Missing right parenthesis in only group.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Missing right parenthesis in first group.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber:pager");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Missing left parenthesis in only group.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLineexampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Early right parenthesis in only group.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(example_numb)er");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Extra right parenthesis at end of only group.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber))");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Extra right parenthesis between proper parentheses.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(example_numb)er)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Extra left parenthesis in only group.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine((exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Extra level of children.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber(possibleLength))");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Trailing comma in children.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(exampleNumber,)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Leading comma in children.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(,exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Empty token between commas.
    try {
      MetadataFilter.parseFieldMapFromString("fixedLine(possibleLength,,exampleNumber)");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Trailing colon.
    try {
      MetadataFilter.parseFieldMapFromString("uan:");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Leading colon.
    try {
      MetadataFilter.parseFieldMapFromString(":uan");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Empty token between colons.
    try {
      MetadataFilter.parseFieldMapFromString("uan::fixedLine");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }

    // Missing colon between groups.
    try {
      MetadataFilter.parseFieldMapFromString("uan(possibleLength)pager");
      fail();
    } catch (RuntimeException e) {
      // Test passed.
    }
  }

  public void testComputeComplement_allAndNothing() {
    TreeMap<String, TreeSet<String>> map1 = new TreeMap<String, TreeSet<String>>();
    map1.put("fixedLine", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("mobile", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("tollFree", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("premiumRate", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("sharedCost", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("personalNumber", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("voip", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("pager", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("uan", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("emergency", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("voicemail", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("shortCode", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("standardRate", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("carrierSpecific", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("smsServices", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("noInternationalDialling",
        new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("preferredInternationalPrefix", new TreeSet<String>());
    map1.put("nationalPrefix", new TreeSet<String>());
    map1.put("preferredExtnPrefix", new TreeSet<String>());
    map1.put("nationalPrefixTransformRule", new TreeSet<String>());
    map1.put("sameMobileAndFixedLinePattern", new TreeSet<String>());
    map1.put("mainCountryForCode", new TreeSet<String>());
    map1.put("mobileNumberPortableRegion", new TreeSet<String>());

    TreeMap<String, TreeSet<String>> map2 = new TreeMap<String, TreeSet<String>>();

    assertEquals(MetadataFilter.computeComplement(map1), map2);
    assertEquals(MetadataFilter.computeComplement(map2), map1);
  }

  public void testComputeComplement_inBetween() {
    TreeMap<String, TreeSet<String>> map1 = new TreeMap<String, TreeSet<String>>();
    map1.put("fixedLine", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("mobile", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("tollFree", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("premiumRate", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("emergency", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    map1.put("voicemail", new TreeSet<String>(Arrays.asList("possibleLength", "exampleNumber")));
    map1.put("shortCode", new TreeSet<String>(Arrays.asList("exampleNumber")));
    map1.put("standardRate", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("carrierSpecific", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("smsServices", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    map1.put("noInternationalDialling",
        new TreeSet<String>(MetadataFilter.excludableChildFields));
    map1.put("nationalPrefixTransformRule", new TreeSet<String>());
    map1.put("sameMobileAndFixedLinePattern", new TreeSet<String>());
    map1.put("mainCountryForCode", new TreeSet<String>());
    map1.put("mobileNumberPortableRegion", new TreeSet<String>());

    TreeMap<String, TreeSet<String>> map2 = new TreeMap<String, TreeSet<String>>();
    map2.put("sharedCost", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map2.put("personalNumber", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map2.put("voip", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map2.put("pager", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map2.put("uan", new TreeSet<String>(MetadataFilter.excludableChildFields));
    map2.put("emergency", new TreeSet<String>(Arrays.asList(
        "possibleLength", "possibleLengthLocalOnly", "exampleNumber")));
    map2.put("smsServices", new TreeSet<String>(Arrays.asList(
        "possibleLength", "possibleLengthLocalOnly", "exampleNumber")));
    map2.put("voicemail", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleLengthLocalOnly")));
    map2.put("shortCode", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleLength", "possibleLengthLocalOnly")));
    map2.put("preferredInternationalPrefix", new TreeSet<String>());
    map2.put("nationalPrefix", new TreeSet<String>());
    map2.put("preferredExtnPrefix", new TreeSet<String>());

    assertEquals(MetadataFilter.computeComplement(map1), map2);
    assertEquals(MetadataFilter.computeComplement(map2), map1);
  }

  public void testShouldDrop() {
    TreeMap<String, TreeSet<String>> blacklist = new TreeMap<String, TreeSet<String>>();
    blacklist.put("fixedLine", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("mobile", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("tollFree", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("premiumRate", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("emergency", new TreeSet<String>(Arrays.asList("nationalNumberPattern")));
    blacklist.put("voicemail", new TreeSet<String>(Arrays.asList(
        "possibleLength", "exampleNumber")));
    blacklist.put("shortCode", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("standardRate", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("carrierSpecific", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("smsServices", new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("noInternationalDialling",
        new TreeSet<String>(MetadataFilter.excludableChildFields));
    blacklist.put("nationalPrefixTransformRule", new TreeSet<String>());
    blacklist.put("sameMobileAndFixedLinePattern", new TreeSet<String>());
    blacklist.put("mainCountryForCode", new TreeSet<String>());
    blacklist.put("mobileNumberPortableRegion", new TreeSet<String>());

    MetadataFilter filter = new MetadataFilter(blacklist);
    assertTrue(filter.shouldDrop("fixedLine", "exampleNumber"));
    assertFalse(filter.shouldDrop("sharedCost", "exampleNumber"));
    assertFalse(filter.shouldDrop("emergency", "exampleNumber"));
    assertTrue(filter.shouldDrop("emergency", "nationalNumberPattern"));
    assertFalse(filter.shouldDrop("preferredInternationalPrefix"));
    assertTrue(filter.shouldDrop("mobileNumberPortableRegion"));
    assertTrue(filter.shouldDrop("smsServices", "nationalNumberPattern"));

    // Integration tests starting with flag values.
    assertTrue(BuildMetadataFromXml.getMetadataFilter(true, false)
        .shouldDrop("fixedLine", "exampleNumber"));

    // Integration tests starting with blacklist strings.
    assertTrue(new MetadataFilter(MetadataFilter.parseFieldMapFromString("fixedLine"))
        .shouldDrop("fixedLine", "exampleNumber"));
    assertFalse(new MetadataFilter(MetadataFilter.parseFieldMapFromString("uan"))
        .shouldDrop("fixedLine", "exampleNumber"));

    // Integration tests starting with whitelist strings.
    assertFalse(new MetadataFilter(MetadataFilter.computeComplement(
        MetadataFilter.parseFieldMapFromString("exampleNumber")))
        .shouldDrop("fixedLine", "exampleNumber"));
    assertTrue(new MetadataFilter(MetadataFilter.computeComplement(
        MetadataFilter.parseFieldMapFromString("uan"))).shouldDrop("fixedLine", "exampleNumber"));

    // Integration tests with an empty blacklist.
    assertFalse(new MetadataFilter(new TreeMap<String, TreeSet<String>>())
        .shouldDrop("fixedLine", "exampleNumber"));
  }

  // Test that a fake PhoneMetadata filtered for liteBuild ends up clearing exactly the expected
  // fields. The lite build is used to clear example_number fields from all PhoneNumberDescs.
  public void testFilterMetadata_liteBuild() {
    PhoneMetadata.Builder metadata = getFakeArmeniaPhoneMetadata();

    MetadataFilter.forLiteBuild().filterMetadata(metadata);

    // id, country_code, and international_prefix should never be cleared.
    assertEquals(metadata.getId(), ID);
    assertEquals(metadata.getCountryCode(), COUNTRY_CODE);
    assertEquals(metadata.getInternationalPrefix(), INTERNATIONAL_PREFIX);

    // preferred_international_prefix should not be cleared in liteBuild.
    assertEquals(metadata.getPreferredInternationalPrefix(), PREFERRED_INTERNATIONAL_PREFIX);

    // All PhoneNumberDescs must have only example_number cleared.
    for (PhoneNumberDesc desc : Arrays.asList(
        metadata.getGeneralDesc(),
        metadata.getFixedLine(),
        metadata.getMobile(),
        metadata.getTollFree())) {
      assertEquals(desc.getNationalNumberPattern(), NATIONAL_NUMBER_PATTERN);
      assertContentsEqual(desc.getPossibleLengthList(), possibleLengths);
      assertContentsEqual(desc.getPossibleLengthLocalOnlyList(), possibleLengthsLocalOnly);
      assertFalse(desc.hasExampleNumber());
    }
  }

  // Test that a fake PhoneMetadata filtered for specialBuild ends up clearing exactly the expected
  // fields. The special build is used to clear PhoneNumberDescs other than general_desc and mobile,
  // and non-PhoneNumberDesc PhoneMetadata fields that aren't needed for parsing.
  public void testFilterMetadata_specialBuild() {
    PhoneMetadata.Builder metadata = getFakeArmeniaPhoneMetadata();

    MetadataFilter.forSpecialBuild().filterMetadata(metadata);

    // id, country_code, and international_prefix should never be cleared.
    assertEquals(metadata.getId(), ID);
    assertEquals(metadata.getCountryCode(), COUNTRY_CODE);
    assertEquals(metadata.getInternationalPrefix(), INTERNATIONAL_PREFIX);

    // preferred_international_prefix should be cleared in specialBuild.
    assertFalse(metadata.hasPreferredInternationalPrefix());

    // general_desc should have all fields but example_number; mobile should have all fields.
    for (PhoneNumberDesc desc : Arrays.asList(
        metadata.getGeneralDesc(),
        metadata.getMobile())) {
      assertEquals(desc.getNationalNumberPattern(), NATIONAL_NUMBER_PATTERN);
      assertContentsEqual(desc.getPossibleLengthList(), possibleLengths);
      assertContentsEqual(desc.getPossibleLengthLocalOnlyList(), possibleLengthsLocalOnly);
    }
    assertFalse(metadata.getGeneralDesc().hasExampleNumber());
    assertEquals(metadata.getMobile().getExampleNumber(), EXAMPLE_NUMBER);

    // All other PhoneNumberDescs must have all fields cleared.
    for (PhoneNumberDesc desc : Arrays.asList(
        metadata.getFixedLine(),
        metadata.getTollFree())) {
      assertFalse(desc.hasNationalNumberPattern());
      assertEquals(desc.getPossibleLengthList().size(), 0);
      assertEquals(desc.getPossibleLengthLocalOnlyList().size(), 0);
      assertFalse(desc.hasExampleNumber());
    }
  }

  // Test that filtering a fake PhoneMetadata with the empty MetadataFilter results in no change.
  public void testFilterMetadata_emptyFilter() {
    PhoneMetadata.Builder metadata = getFakeArmeniaPhoneMetadata();

    MetadataFilter.emptyFilter().filterMetadata(metadata);

    // None of the fields should be cleared.
    assertEquals(metadata.getId(), ID);
    assertEquals(metadata.getCountryCode(), COUNTRY_CODE);
    assertEquals(metadata.getInternationalPrefix(), INTERNATIONAL_PREFIX);
    assertEquals(metadata.getPreferredInternationalPrefix(), PREFERRED_INTERNATIONAL_PREFIX);
    for (PhoneNumberDesc desc : Arrays.asList(
        metadata.getGeneralDesc(),
        metadata.getFixedLine(),
        metadata.getMobile(),
        metadata.getTollFree())) {
      assertEquals(desc.getNationalNumberPattern(), NATIONAL_NUMBER_PATTERN);
      assertContentsEqual(desc.getPossibleLengthList(), possibleLengths);
      assertContentsEqual(desc.getPossibleLengthLocalOnlyList(), possibleLengthsLocalOnly);
    }
    assertFalse(metadata.getGeneralDesc().hasExampleNumber());
    assertEquals(metadata.getFixedLine().getExampleNumber(), EXAMPLE_NUMBER);
    assertEquals(metadata.getMobile().getExampleNumber(), EXAMPLE_NUMBER);
    assertEquals(metadata.getTollFree().getExampleNumber(), EXAMPLE_NUMBER);
  }

  public void testIntegrityOfFieldSets() {
    TreeSet<String> union = new TreeSet<String>();
    union.addAll(MetadataFilter.excludableParentFields);
    union.addAll(MetadataFilter.excludableChildFields);
    union.addAll(MetadataFilter.excludableChildlessFields);

    // Mutually exclusive sets.
    assertTrue(union.size() == MetadataFilter.excludableParentFields.size()
        + MetadataFilter.excludableChildFields.size()
        + MetadataFilter.excludableChildlessFields.size());

    // Nonempty sets.
    assertTrue(MetadataFilter.excludableParentFields.size() > 0
        && MetadataFilter.excludableChildFields.size() > 0
        && MetadataFilter.excludableChildlessFields.size() > 0);

    // Nonempty and canonical field names.
    for (String field : union) {
      assertTrue(field.length() > 0 && field.trim().equals(field));
    }
  }

  private static PhoneMetadata.Builder getFakeArmeniaPhoneMetadata() {
    PhoneMetadata.Builder metadata = PhoneMetadata.newBuilder();
    metadata.setId(ID);
    metadata.setCountryCode(COUNTRY_CODE);
    metadata.setInternationalPrefix(INTERNATIONAL_PREFIX);
    metadata.setPreferredInternationalPrefix(PREFERRED_INTERNATIONAL_PREFIX);
    metadata.setGeneralDesc(getFakeArmeniaPhoneNumberDesc(true));
    metadata.setFixedLine(getFakeArmeniaPhoneNumberDesc(false));
    metadata.setMobile(getFakeArmeniaPhoneNumberDesc(false));
    metadata.setTollFree(getFakeArmeniaPhoneNumberDesc(false));
    return metadata;
  }

  private static PhoneNumberDesc getFakeArmeniaPhoneNumberDesc(boolean generalDesc) {
    PhoneNumberDesc desc = new PhoneNumberDesc().setNationalNumberPattern(NATIONAL_NUMBER_PATTERN);
    if (!generalDesc) {
      desc.setExampleNumber(EXAMPLE_NUMBER);
    }
    for (int i : possibleLengths) {
      desc.addPossibleLength(i);
    }
    for (int i : possibleLengthsLocalOnly) {
      desc.addPossibleLengthLocalOnly(i);
    }
    return desc;
  }

  private static void assertContentsEqual(List<Integer> list, int[] array) {
    assertEquals(list.size(), array.length);
    for (int i = 0; i < list.size(); i++) {
      assertEquals((int) list.get(i), array[i]);
    }
  }
}
