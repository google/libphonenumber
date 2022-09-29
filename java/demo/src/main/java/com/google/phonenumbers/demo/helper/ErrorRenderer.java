package com.google.phonenumbers.demo.helper;

import com.google.phonenumbers.demo.template.ResultErrorTemplates;
import com.google.phonenumbers.demo.template.ResultErrorTemplates.Error;
import java.util.Locale;

public class ErrorRenderer extends LibPhoneNumberRenderer<Error> {
  private final String phoneNumber;
  private final String defaultCountry;
  private final Locale geocodingLocale;
  private final String error;

  public ErrorRenderer(
      String phoneNumber, String defaultCountry, Locale geocodingLocale, String error) {
    super("result_error.soy", "com.google.phonenumbers.demo.error");
    this.phoneNumber = phoneNumber;
    this.defaultCountry = defaultCountry;
    this.geocodingLocale = geocodingLocale;
    this.error = error;
  }

  @Override
  public String genHtml() {
    return super.render(
        ResultErrorTemplates.Error.builder()
            .setPhoneNumber(phoneNumber)
            .setDefaultCountry(defaultCountry)
            .setGeocodingLocale(geocodingLocale.toLanguageTag())
            .setError(error)
            .build());
  }
}
