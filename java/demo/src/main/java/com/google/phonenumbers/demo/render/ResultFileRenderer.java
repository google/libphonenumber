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
 *
 * @author Tobias Rogg
 */

package com.google.phonenumbers.demo.render;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.phonenumbers.demo.template.ResultFileTemplates.File;
import java.util.StringTokenizer;

public class ResultFileRenderer extends LibPhoneNumberRenderer<File> {
  private final String defaultCountry;
  private final String fileContents;
  private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

  public ResultFileRenderer(String defaultCountry, String fileContents) {
    this.fileContents = fileContents;
    this.defaultCountry = defaultCountry;
  }

  @Override
  public String genHtml() {
    File.Builder soyTemplate = File.builder();
    int phoneNumberId = 0;
    StringTokenizer tokenizer = new StringTokenizer(fileContents, ",");
    while (tokenizer.hasMoreTokens()) {
      String numberStr = tokenizer.nextToken();
      phoneNumberId++;
      try {
        PhoneNumber number = phoneUtil.parseAndKeepRawInput(numberStr, defaultCountry);
        boolean isNumberValid = phoneUtil.isValidNumber(number);
        soyTemplate.addRows(
            phoneNumberId,
            numberStr,
            isNumberValid ? phoneUtil.formatInOriginalFormat(number, defaultCountry) : "invalid",
            isNumberValid ? phoneUtil.format(number, PhoneNumberFormat.INTERNATIONAL) : "invalid",
            null);
      } catch (NumberParseException e) {
        soyTemplate.addRows(phoneNumberId, numberStr, null, null, e.toString());
      }
    }
    return super.render(soyTemplate.build());
  }
}
