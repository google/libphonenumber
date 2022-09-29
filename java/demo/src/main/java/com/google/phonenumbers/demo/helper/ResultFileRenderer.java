package com.google.phonenumbers.demo.helper;

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
    super("result_file.soy", "com.google.phonenumbers.demo.file");
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
