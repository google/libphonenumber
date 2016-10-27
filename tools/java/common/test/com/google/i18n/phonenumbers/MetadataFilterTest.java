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

import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;
import junit.framework.TestCase;

public final class MetadataFilterTest extends TestCase {
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
    blacklist.put("noInternationalDialling",
        new TreeSet<String>(Arrays.asList("exampleNumber")));

    assertEquals(MetadataFilter.forLiteBuild(), new MetadataFilter(blacklist));
  }

  // If this behavior changes then consider whether the change in the blacklist is intended, or you
  // should change the special build configuration. Also look into any change in the size of the
  // build.
  public void testForSpecialBuild() {
    TreeMap<String, TreeSet<String>> blacklist = new TreeMap<String, TreeSet<String>>();
    blacklist.put("fixedLine", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("tollFree", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("premiumRate", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("sharedCost", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("personalNumber", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("voip", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("pager", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("uan", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("emergency", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("voicemail", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("shortCode", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("standardRate", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("carrierSpecific", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("noInternationalDialling",
        new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("preferredInternationalPrefix", new TreeSet<String>());
    blacklist.put("nationalPrefix", new TreeSet<String>());
    blacklist.put("preferredExtnPrefix", new TreeSet<String>());
    blacklist.put("nationalPrefixTransformRule", new TreeSet<String>());
    blacklist.put("sameMobileAndFixedLinePattern", new TreeSet<String>());
    blacklist.put("mainCountryForCode", new TreeSet<String>());
    blacklist.put("leadingZeroPossible", new TreeSet<String>());
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
        "nationalNumberPattern", "possibleNumberPattern", "possibleLength",
                "possibleLengthLocalOnly", "exampleNumber")));

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
        "possibleLength", "exampleNumber", "possibleLengthLocalOnly",
        "nationalNumberPattern")));
    fieldMap.put("pager", new TreeSet<String>(Arrays.asList(
        "exampleNumber", "nationalNumberPattern")));
    fieldMap.put("fixedLine", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleNumberPattern", "possibleLength",
        "possibleLengthLocalOnly", "exampleNumber")));
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
            + ":noInternationalDialling"),
        MetadataFilter.parseFieldMapFromString(
            "nationalNumberPattern"
            + ":possibleNumberPattern"
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
            "uan(nationalNumberPattern,possibleNumberPattern,possibleLength,exampleNumber,"
            + "possibleLengthLocalOnly)"),
        MetadataFilter.parseFieldMapFromString("uan"));

    // All parent's children covered, some implicitly and some explicitly.
    assertEquals(
        MetadataFilter.parseFieldMapFromString(
            "uan(nationalNumberPattern,possibleNumberPattern,possibleLength,exampleNumber)"
            + ":possibleLengthLocalOnly"),
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
          "uan(nationalNumberPattern,possibleNumberPattern,possibleLength,exampleNumber)"
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
    map1.put("fixedLine", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("mobile", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("tollFree", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("premiumRate", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("sharedCost", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("personalNumber", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("voip", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("pager", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("uan", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("emergency", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("voicemail", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("shortCode", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("standardRate", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("carrierSpecific", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("noInternationalDialling",
        new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("preferredInternationalPrefix", new TreeSet<String>());
    map1.put("nationalPrefix", new TreeSet<String>());
    map1.put("preferredExtnPrefix", new TreeSet<String>());
    map1.put("nationalPrefixTransformRule", new TreeSet<String>());
    map1.put("sameMobileAndFixedLinePattern", new TreeSet<String>());
    map1.put("mainCountryForCode", new TreeSet<String>());
    map1.put("leadingZeroPossible", new TreeSet<String>());
    map1.put("mobileNumberPortableRegion", new TreeSet<String>());

    TreeMap<String, TreeSet<String>> map2 = new TreeMap<String, TreeSet<String>>();

    assertEquals(MetadataFilter.computeComplement(map1), map2);
    assertEquals(MetadataFilter.computeComplement(map2), map1);
  }

  public void testComputeComplement_inBetween() {
    TreeMap<String, TreeSet<String>> map1 = new TreeMap<String, TreeSet<String>>();
    map1.put("fixedLine", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("mobile", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("tollFree", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("premiumRate", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("emergency", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleNumberPattern")));
    map1.put("voicemail", new TreeSet<String>(Arrays.asList("possibleLength", "exampleNumber")));
    map1.put("shortCode", new TreeSet<String>(Arrays.asList("exampleNumber")));
    map1.put("standardRate", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("carrierSpecific", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("noInternationalDialling",
        new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map1.put("nationalPrefixTransformRule", new TreeSet<String>());
    map1.put("sameMobileAndFixedLinePattern", new TreeSet<String>());
    map1.put("mainCountryForCode", new TreeSet<String>());
    map1.put("leadingZeroPossible", new TreeSet<String>());
    map1.put("mobileNumberPortableRegion", new TreeSet<String>());

    TreeMap<String, TreeSet<String>> map2 = new TreeMap<String, TreeSet<String>>();
    map2.put("sharedCost", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map2.put("personalNumber", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map2.put("voip", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map2.put("pager", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map2.put("uan", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    map2.put("emergency", new TreeSet<String>(Arrays.asList(
        "possibleLength", "possibleLengthLocalOnly", "exampleNumber")));
    map2.put("voicemail", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleNumberPattern", "possibleLengthLocalOnly")));
    map2.put("shortCode", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleNumberPattern", "possibleLength",
        "possibleLengthLocalOnly")));
    map2.put("preferredInternationalPrefix", new TreeSet<String>());
    map2.put("nationalPrefix", new TreeSet<String>());
    map2.put("preferredExtnPrefix", new TreeSet<String>());

    assertEquals(MetadataFilter.computeComplement(map1), map2);
    assertEquals(MetadataFilter.computeComplement(map2), map1);
  }

  public void testDrop() {
    TreeMap<String, TreeSet<String>> blacklist = new TreeMap<String, TreeSet<String>>();
    blacklist.put("fixedLine", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("mobile", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("tollFree", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("premiumRate", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("emergency", new TreeSet<String>(Arrays.asList(
        "nationalNumberPattern", "possibleNumberPattern")));
    blacklist.put("voicemail", new TreeSet<String>(Arrays.asList(
        "possibleLength", "exampleNumber")));
    blacklist.put("shortCode", new TreeSet<String>(Arrays.asList("exampleNumber")));
    blacklist.put("standardRate", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("carrierSpecific", new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("noInternationalDialling",
        new TreeSet<String>(MetadataFilter.EXCLUDABLE_CHILD_FIELDS));
    blacklist.put("nationalPrefixTransformRule", new TreeSet<String>());
    blacklist.put("sameMobileAndFixedLinePattern", new TreeSet<String>());
    blacklist.put("mainCountryForCode", new TreeSet<String>());
    blacklist.put("leadingZeroPossible", new TreeSet<String>());
    blacklist.put("mobileNumberPortableRegion", new TreeSet<String>());

    MetadataFilter filter = new MetadataFilter(blacklist);
    assertTrue(filter.drop("fixedLine", "exampleNumber"));
    assertFalse(filter.drop("sharedCost", "exampleNumber"));
    assertFalse(filter.drop("emergency", "exampleNumber"));
    assertTrue(filter.drop("emergency", "nationalNumberPattern"));
    assertFalse(filter.drop("preferredInternationalPrefix"));
    assertTrue(filter.drop("mobileNumberPortableRegion"));

    // Integration tests starting with flag values.
    assertTrue(BuildMetadataFromXml.getMetadataFilter(true, false)
        .drop("fixedLine", "exampleNumber"));

    // Integration tests starting with blacklist strings.
    assertTrue(new MetadataFilter(MetadataFilter.parseFieldMapFromString("fixedLine"))
        .drop("fixedLine", "exampleNumber"));
    assertFalse(new MetadataFilter(MetadataFilter.parseFieldMapFromString("uan"))
        .drop("fixedLine", "exampleNumber"));

    // Integration tests starting with whitelist strings.
    assertFalse(new MetadataFilter(MetadataFilter.computeComplement(
        MetadataFilter.parseFieldMapFromString("exampleNumber")))
        .drop("fixedLine", "exampleNumber"));
    assertTrue(new MetadataFilter(MetadataFilter.computeComplement(
        MetadataFilter.parseFieldMapFromString("uan"))).drop("fixedLine", "exampleNumber"));

    // Integration tests with an empty blacklist.
    assertFalse(new MetadataFilter(new TreeMap<String, TreeSet<String>>())
        .drop("fixedLine", "exampleNumber"));
  }

  public void testIntegrityOfFieldSets() {
    TreeSet<String> union = new TreeSet<String>();
    union.addAll(MetadataFilter.EXCLUDABLE_PARENT_FIELDS);
    union.addAll(MetadataFilter.EXCLUDABLE_CHILD_FIELDS);
    union.addAll(MetadataFilter.EXCLUDABLE_CHILDLESS_FIELDS);

    // Mutually exclusive sets.
    assertTrue(union.size() == MetadataFilter.EXCLUDABLE_PARENT_FIELDS.size()
        + MetadataFilter.EXCLUDABLE_CHILD_FIELDS.size()
        + MetadataFilter.EXCLUDABLE_CHILDLESS_FIELDS.size());

    // Nonempty sets.
    assertTrue(MetadataFilter.EXCLUDABLE_PARENT_FIELDS.size() > 0
        && MetadataFilter.EXCLUDABLE_CHILD_FIELDS.size() > 0
        && MetadataFilter.EXCLUDABLE_CHILDLESS_FIELDS.size() > 0);

    // Nonempty and canonical field names.
    for (String field : union) {
      assertTrue(field.length() > 0 && field.trim().equals(field));
    }
  }
}
