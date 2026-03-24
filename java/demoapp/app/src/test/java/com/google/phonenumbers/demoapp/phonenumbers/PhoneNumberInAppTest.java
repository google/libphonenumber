package com.google.phonenumbers.demoapp.phonenumbers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.phonenumbers.demoapp.phonenumbers.PhoneNumberInApp.FormattingState;
import org.junit.Test;

/** JUnit Tests for class {@link PhoneNumberInApp}. */
public class PhoneNumberInAppTest {

  @Test
  public void constructor() {
    String id = "45";
    String contactName = "Alena Potts";
    String originalPhoneNumber = "0446681800";

    PhoneNumberInApp phoneNumberInApp = new PhoneNumberInApp(id, contactName, originalPhoneNumber);

    assertEquals(id, phoneNumberInApp.getId());
    assertEquals(contactName, phoneNumberInApp.getContactName());
    assertEquals(originalPhoneNumber, phoneNumberInApp.getOriginalPhoneNumber());
    assertNull(phoneNumberInApp.getFormattedPhoneNumber());
    assertEquals(PhoneNumberInApp.FormattingState.PENDING, phoneNumberInApp.getFormattingState());
    assertFalse(phoneNumberInApp.shouldContactBeUpdated());
  }

  @Test
  public void setFormattedPhoneNumber() {
    PhoneNumberInApp phoneNumberInApp = new PhoneNumberInApp("2", "Beatrice Bradley", "0446681801");
    String formattedPhoneNumber = "+41446681801";

    phoneNumberInApp.setFormattedPhoneNumber(formattedPhoneNumber);

    assertEquals(formattedPhoneNumber, phoneNumberInApp.getFormattedPhoneNumber());
  }

  @Test
  public void setFormattingState() {
    PhoneNumberInApp phoneNumberInApp = new PhoneNumberInApp("1283", "Donte Salinas", "0446681802");
    FormattingState formattingState = FormattingState.NUMBER_IS_ALREADY_IN_E164;

    phoneNumberInApp.setFormattingState(formattingState);

    assertEquals(formattingState, phoneNumberInApp.getFormattingState());
  }

  @Test
  public void setShouldContactBeUpdated() {
    PhoneNumberInApp phoneNumberInApp =
        new PhoneNumberInApp("19735", "Izabelle Goodwin", "0446681803");

    phoneNumberInApp.setShouldContactBeUpdated(true);

    assertTrue(phoneNumberInApp.shouldContactBeUpdated());
  }

  @Test
  public void compareTo() {
    PhoneNumberInApp phoneNumberInApp1 =
        new PhoneNumberInApp("345", "Kassandra Coffey", "0446681804");
    PhoneNumberInApp phoneNumberInApp2 =
        new PhoneNumberInApp("22", "Mariyah Johnston", "0446681805");

    assertTrue(phoneNumberInApp1.compareTo(phoneNumberInApp2) < 0);
    assertTrue(phoneNumberInApp2.compareTo(phoneNumberInApp1) > 0);
  }
}
