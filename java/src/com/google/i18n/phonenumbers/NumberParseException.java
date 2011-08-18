/*
 * Copyright (C) 2009 The Libphonenumber Authors
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

/**
 * Generic exception class for errors encountered when parsing phone numbers.
 * @author Lara Rennie
 */
@SuppressWarnings("serial")
public class NumberParseException extends Exception {

  public enum ErrorType {
    INVALID_COUNTRY_CODE,
    // This generally indicates the string passed in had less than 3 digits in it. More
    // specifically, the number failed to match the regular expression VALID_PHONE_NUMBER in
    // PhoneNumberUtil.java.
    NOT_A_NUMBER,
    // This indicates the string started with an international dialing prefix, but after this was
    // stripped from the number, had less digits than any valid phone number (including country
    // code) could have.
    TOO_SHORT_AFTER_IDD,
    // This indicates the string, after any country code has been stripped, had less digits than any
    // valid phone number could have.
    TOO_SHORT_NSN,
    // This indicates the string had more digits than any valid phone number could have.
    TOO_LONG,
  }

  private ErrorType errorType;
  private String message;

  public NumberParseException(ErrorType errorType, String message) {
    super(message);
    this.message = message;
    this.errorType = errorType;
  }

  /**
   * Returns the error type of the exception that has been thrown.
   */
  public ErrorType getErrorType() {
    return errorType;
  }

  @Override
  public String toString() {
    return "Error type: " + errorType + ". " + message;
  }
}
