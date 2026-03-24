package com.google.phonenumbers.demoapp.phonenumbers;

import java.io.Serializable;

/**
 * Represents a phone number and the conversion of it in the app (between reading from and writing
 * to contacts).
 */
public class PhoneNumberInApp implements Serializable, Comparable<PhoneNumberInApp> {

  /** ID to identify the phone number in the device's contacts. */
  private final String id;
  /** Display name of the contact the phone number belongs to. */
  private final String contactName;

  /** Phone number as originally in contacts. */
  private final String originalPhoneNumber;
  /**
   * The in E.164 formatted {@link PhoneNumberInApp#originalPhoneNumber} (e.g. {@code +41446681800})
   * if formattable, else {@code null}.
   */
  private String formattedPhoneNumber = null;

  private FormattingState formattingState = FormattingState.PENDING;

  /**
   * Equal to the value of the checkbox in the UI. Only if {@code true} the phone number should be
   * updated in the contacts.
   */
  private boolean shouldContactBeUpdated = false;

  public PhoneNumberInApp(String id, String contactName, String originalPhoneNumber) {
    this.id = id;
    this.contactName = contactName;
    this.originalPhoneNumber = originalPhoneNumber;
  }

  public String getId() {
    return id;
  }

  public String getContactName() {
    return contactName;
  }

  public String getOriginalPhoneNumber() {
    return originalPhoneNumber;
  }

  public String getFormattedPhoneNumber() {
    return formattedPhoneNumber;
  }

  public void setFormattedPhoneNumber(String formattedPhoneNumber) {
    this.formattedPhoneNumber = formattedPhoneNumber;
  }

  public FormattingState getFormattingState() {
    return formattingState;
  }

  public void setFormattingState(FormattingState formattingState) {
    this.formattingState = formattingState;
  }

  public boolean shouldContactBeUpdated() {
    return shouldContactBeUpdated;
  }

  public void setShouldContactBeUpdated(boolean shouldContactBeUpdated) {
    this.shouldContactBeUpdated = shouldContactBeUpdated;
  }

  @Override
  public int compareTo(PhoneNumberInApp o) {
    return getContactName().compareTo(o.getContactName());
  }

  /**
   * Represents the state the formatting of {@link PhoneNumberInApp#originalPhoneNumber} can be at.
   */
  public enum FormattingState {
    /** Used before the formatting is tried/done. */
    PENDING,
    /** Formatting completed to {@link PhoneNumberInApp#formattedPhoneNumber} without errors. */
    COMPLETED,
    /** Error while parsing the {@link PhoneNumberInApp#originalPhoneNumber}. */
    PARSING_ERROR,
    /** {@link PhoneNumberInApp#originalPhoneNumber} is a short number. */
    NUMBER_IS_SHORT_NUMBER,
    /** {@link PhoneNumberInApp#originalPhoneNumber} is not a valid number. */
    NUMBER_IS_NOT_VALID,
    /** {@link PhoneNumberInApp#originalPhoneNumber} is already in E.164 format. */
    NUMBER_IS_ALREADY_IN_E164
  }
}
