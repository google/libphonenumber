/*
 * Copyright (C) 2024 The Libphonenumber Authors
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

/** Options for the phone number parser. */
public class ParsingOptions {
  
  private boolean hasDefaultRegion;
  /**
   * The region we are expecting the number to be from. This is ignored if the number being
   * parsed is written in international format. In case of national format, the country_code will be
   * set to the one of this default region. If the number is guaranteed to start with a '+' followed
   * by the country calling code, then RegionCode.ZZ or null can be supplied.
   */
  private String defaultRegion_ = null;
  public boolean hasDefaultRegion() { return hasDefaultRegion; }
  /**
   * Returns the value of {@link #defaultRegion_}.
   */
  public String getDefaultRegion() { return defaultRegion_; }
  /**
   * Sets the {@link #defaultRegion_} to the given value.
   */
  public ParsingOptions setDefaultRegion(String value) {
    hasDefaultRegion = (value != null);
    defaultRegion_ = value;
    return this;
  }

  /**
   * Whether the raw input should be kept in the PhoneNumber object. If true, the raw_input
   * field and country_code_source fields will be populated.
   */
  private boolean hasKeepRawInput;
  private boolean keepRawInput_ = false;
  /**
   * Returns the value of {@link #hasKeepRawInput}.
   */
  public boolean hasKeepRawInput() { return hasKeepRawInput; }
  /**
   * Returns the value of {@link #keepRawInput_}.
   */
  public boolean isKeepRawInput() { return keepRawInput_; }
  /**
   * Decides with the given value if it should keep the raw input.
   */
  public ParsingOptions setKeepRawInput(boolean value) {
    hasKeepRawInput = true;
    keepRawInput_ = value;
    return this;
  }
}