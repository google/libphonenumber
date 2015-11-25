/*
 * Copyright (C) 2011 The Libphonenumber Authors
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

import java.util.Set;

/*
 * Utility for international short phone numbers, such as short codes and emergency numbers. Note
 * most commercial short numbers are not handled here, but by the PhoneNumberUtil.
 *
 * @deprecated("As of release 5.8, replaced by ShortNumberInfo.")
 *
 * @author Shaopeng Jia
 * @author David Yonge-Mallo
 */
@Deprecated public class ShortNumberUtil {

  /**
   * Cost categories of short numbers.
   */
  public enum ShortNumberCost {
    TOLL_FREE,
    STANDARD_RATE,
    PREMIUM_RATE,
    UNKNOWN_COST
  }

  public ShortNumberUtil() {
  }

  /**
   * Convenience method to get a list of what regions the library has metadata for.
   */
  public Set<String> getSupportedRegions() {
    return ShortNumberInfo.getInstance().getSupportedRegions();
  }

  /**
   * Returns true if the number might be used to connect to an emergency service in the given
   * region.
   *
   * This method takes into account cases where the number might contain formatting, or might have
   * additional digits appended (when it is okay to do that in the region specified).
   *
   * @param number  the phone number to test
   * @param regionCode  the region where the phone number is being dialed
   * @return  if the number might be used to connect to an emergency service in the given region.
   */
  public boolean connectsToEmergencyNumber(String number, String regionCode) {
    return ShortNumberInfo.getInstance().connectsToEmergencyNumber(number, regionCode);
  }

  /**
   * Returns true if the number exactly matches an emergency service number in the given region.
   *
   * This method takes into account cases where the number might contain formatting, but doesn't
   * allow additional digits to be appended.
   *
   * @param number  the phone number to test
   * @param regionCode  the region where the phone number is being dialed
   * @return  if the number exactly matches an emergency services number in the given region.
   */
  public boolean isEmergencyNumber(String number, String regionCode) {
    return ShortNumberInfo.getInstance().isEmergencyNumber(number, regionCode);
  }
}
