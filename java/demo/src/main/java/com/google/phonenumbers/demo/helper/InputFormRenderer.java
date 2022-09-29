package com.google.phonenumbers.demo.helper;

import com.google.phonenumbers.demo.template.InputFormTemplates;
import com.google.phonenumbers.demo.template.InputFormTemplates.InputForm;

public class InputFormRenderer extends LibPhoneNumberRenderer<InputForm> {

  public InputFormRenderer() {
    super("input_form.soy", "com.google.phonenumbers.demo.inputForm");
  }

  @Override
  public String genHtml() {

    return super.render(
        InputFormTemplates.InputForm.builder()
            .setWelcomeTitle("Phone Number Parser Demo for LibPhoneNumber")
            .build());
  }
}
