package com.google.phonenumbers.demoapp.phonenumbers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp.FormattingState;
import org.junit.Test;

/** JUnit Tests for class {@link PhoneNumberFormatting}. */
public class PhoneNumberFormattingTest {

  @Test
  public void formatPhoneNumberInApp_parsingError() {
    PhoneNumberInApp phoneNumberInApp = new PhoneNumberInApp("19735", "Izabelle Goodwin", "#");

    PhoneNumberFormatting.formatPhoneNumberInApp(phoneNumberInApp, "CH", false);

    assertNull(phoneNumberInApp.getFormattedPhoneNumber());
    assertEquals(FormattingState.PARSING_ERROR, phoneNumberInApp.getFormattingState());
    assertFalse(phoneNumberInApp.shouldContactBeUpdated());
  }

  @Test
  public void formatPhoneNumberInApp_numberIsShortNumber() {
    PhoneNumberInApp phoneNumberInApp = new PhoneNumberInApp("2", "Beatrice Bradley", "144");

    PhoneNumberFormatting.formatPhoneNumberInApp(phoneNumberInApp, "CH", false);

    assertNull(phoneNumberInApp.getFormattedPhoneNumber());
    assertEquals(FormattingState.NUMBER_IS_SHORT_NUMBER, phoneNumberInApp.getFormattingState());
    assertFalse(phoneNumberInApp.shouldContactBeUpdated());
  }

  @Test
  public void formatPhoneNumberInApp_invalidNumber() {
    PhoneNumberInApp phoneNumberInApp =
        new PhoneNumberInApp("1283", "Donte Salinas", "04466818029999");

    PhoneNumberFormatting.formatPhoneNumberInApp(phoneNumberInApp, "CH", false);

    assertNull(phoneNumberInApp.getFormattedPhoneNumber());
    assertEquals(FormattingState.NUMBER_IS_NOT_VALID, phoneNumberInApp.getFormattingState());
    assertFalse(phoneNumberInApp.shouldContactBeUpdated());
  }

  @Test
  public void formatPhoneNumberInApp_numberIsAlreadyInE164() {
    PhoneNumberInApp phoneNumberInApp =
        new PhoneNumberInApp("345", "Kassandra Coffey", "+41446681804");

    PhoneNumberFormatting.formatPhoneNumberInApp(phoneNumberInApp, "CH", false);

    assertNull(phoneNumberInApp.getFormattedPhoneNumber());
    assertEquals(FormattingState.NUMBER_IS_ALREADY_IN_E164, phoneNumberInApp.getFormattingState());
    assertFalse(phoneNumberInApp.shouldContactBeUpdated());
  }

  @Test
  public void
      formatPhoneNumberInApp_originalWithWhitespace_ignoreWhitespaceTrue_numberIsAlreadyInE164() {
    PhoneNumberInApp phoneNumberInApp =

        new PhoneNumberInApp("443221", "Nayeli Martinez", "+41 446 68 18 07");
    PhoneNumberFormatting.formatPhoneNumberInApp(phoneNumberInApp, "CH", true);

    assertNull(phoneNumberInApp.getFormattedPhoneNumber());
    assertEquals(FormattingState.NUMBER_IS_ALREADY_IN_E164, phoneNumberInApp.getFormattingState());
    assertFalse(phoneNumberInApp.shouldContactBeUpdated());
  }

  @Test
  public void formatPhoneNumberInApp_originalWithWhitespace_ignoreWhitespaceFalse_completed() {
    PhoneNumberInApp phoneNumberInApp =
        new PhoneNumberInApp("22", "Mariyah Johnston", "+41 446 68 18 05");

    PhoneNumberFormatting.formatPhoneNumberInApp(phoneNumberInApp, "CH", false);

    assertEquals("+41446681805", phoneNumberInApp.getFormattedPhoneNumber());
    assertEquals(FormattingState.COMPLETED, phoneNumberInApp.getFormattingState());
    assertTrue(phoneNumberInApp.shouldContactBeUpdated());
  }

  @Test
  public void formatPhoneNumberInApp_completed() {
    PhoneNumberInApp phoneNumberInAppCh = new PhoneNumberInApp("45", "Alena Potts", "0446681800");
    PhoneNumberInApp phoneNumberInAppUs =
        new PhoneNumberInApp("3829", "Rebecca Haimo", "9495550102");

    PhoneNumberFormatting.formatPhoneNumberInApp(phoneNumberInAppCh, "CH", false);
    PhoneNumberFormatting.formatPhoneNumberInApp(phoneNumberInAppUs, "US", false);

    String expectedFormattedPhoneNumberCh = "+41446681800";
    assertEquals(expectedFormattedPhoneNumberCh, phoneNumberInAppCh.getFormattedPhoneNumber());
    assertEquals(FormattingState.COMPLETED, phoneNumberInAppCh.getFormattingState());
    assertTrue(phoneNumberInAppCh.shouldContactBeUpdated());
    String expectedFormattedPhoneNumberUs = "+19495550102";
    assertEquals(expectedFormattedPhoneNumberUs, phoneNumberInAppUs.getFormattedPhoneNumber());
    assertEquals(FormattingState.COMPLETED, phoneNumberInAppUs.getFormattingState());
    assertTrue(phoneNumberInAppUs.shouldContactBeUpdated());
  }
}
