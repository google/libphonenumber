/**
 * @license
 * Protocol Buffer 2 Copyright 2008 Google Inc.
 * All other code copyright its respective owners.
 * Copyright (C) 2010 The Libphonenumber Authors
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

/**
 * @fileoverview Generated Protocol Buffer code for file
 * phonenumber.proto.
 */

goog.provide('i18n.phonenumbers.PhoneNumber');
goog.provide('i18n.phonenumbers.PhoneNumber.CountryCodeSource');

goog.require('goog.proto2.Message');
goog.require('goog.proto2.Descriptor');



/**
 * Message PhoneNumber.
 * @constructor
 * @extends {goog.proto2.Message}
 * @final
 */
i18n.phonenumbers.PhoneNumber = function() {
  goog.proto2.Message.call(this);
};
goog.inherits(i18n.phonenumbers.PhoneNumber, goog.proto2.Message);


/**
 * Descriptor for this message, deserialized lazily in getDescriptor().
 * @private {?goog.proto2.Descriptor}
 */
i18n.phonenumbers.PhoneNumber.descriptor_ = null;


/**
 * Overrides {@link goog.proto2.Message#clone} to specify its exact return type.
 * @return {!i18n.phonenumbers.PhoneNumber} The cloned message.
 * @override
 */
i18n.phonenumbers.PhoneNumber.prototype.clone;


/**
 * Gets the value of the country_code field.
 * @return {?number} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getCountryCode = function() {
  return /** @type {?number} */ (this.get$Value(1));
};


/**
 * Gets the value of the country_code field or the default value if not set.
 * @return {number} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getCountryCodeOrDefault = function() {
  return /** @type {number} */ (this.get$ValueOrDefault(1));
};


/**
 * Sets the value of the country_code field.
 * @param {number} value The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.setCountryCode = function(value) {
  this.set$Value(1, value);
};


/**
 * @return {boolean} Whether the country_code field has a value.
 */
i18n.phonenumbers.PhoneNumber.prototype.hasCountryCode = function() {
  return this.has$Value(1);
};


/**
 * @return {number} The number of values in the country_code field.
 */
i18n.phonenumbers.PhoneNumber.prototype.countryCodeCount = function() {
  return this.count$Values(1);
};


/**
 * Clears the values in the country_code field.
 */
i18n.phonenumbers.PhoneNumber.prototype.clearCountryCode = function() {
  this.clear$Field(1);
};


/**
 * Gets the value of the national_number field.
 * @return {?number} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getNationalNumber = function() {
  return /** @type {?number} */ (this.get$Value(2));
};


/**
 * Gets the value of the national_number field or the default value if not set.
 * @return {number} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getNationalNumberOrDefault = function() {
  return /** @type {number} */ (this.get$ValueOrDefault(2));
};


/**
 * Sets the value of the national_number field.
 * @param {number} value The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.setNationalNumber = function(value) {
  this.set$Value(2, value);
};


/**
 * @return {boolean} Whether the national_number field has a value.
 */
i18n.phonenumbers.PhoneNumber.prototype.hasNationalNumber = function() {
  return this.has$Value(2);
};


/**
 * @return {number} The number of values in the national_number field.
 */
i18n.phonenumbers.PhoneNumber.prototype.nationalNumberCount = function() {
  return this.count$Values(2);
};


/**
 * Clears the values in the national_number field.
 */
i18n.phonenumbers.PhoneNumber.prototype.clearNationalNumber = function() {
  this.clear$Field(2);
};


/**
 * Gets the value of the extension field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getExtension = function() {
  return /** @type {?string} */ (this.get$Value(3));
};


/**
 * Gets the value of the extension field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getExtensionOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(3));
};


/**
 * Sets the value of the extension field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.setExtension = function(value) {
  this.set$Value(3, value);
};


/**
 * @return {boolean} Whether the extension field has a value.
 */
i18n.phonenumbers.PhoneNumber.prototype.hasExtension = function() {
  return this.has$Value(3);
};


/**
 * @return {number} The number of values in the extension field.
 */
i18n.phonenumbers.PhoneNumber.prototype.extensionCount = function() {
  return this.count$Values(3);
};


/**
 * Clears the values in the extension field.
 */
i18n.phonenumbers.PhoneNumber.prototype.clearExtension = function() {
  this.clear$Field(3);
};


/**
 * Gets the value of the italian_leading_zero field.
 * @return {?boolean} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getItalianLeadingZero = function() {
  return /** @type {?boolean} */ (this.get$Value(4));
};


/**
 * Gets the value of the italian_leading_zero field or the default value if not set.
 * @return {boolean} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getItalianLeadingZeroOrDefault = function() {
  return /** @type {boolean} */ (this.get$ValueOrDefault(4));
};


/**
 * Sets the value of the italian_leading_zero field.
 * @param {boolean} value The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.setItalianLeadingZero = function(value) {
  this.set$Value(4, value);
};


/**
 * @return {boolean} Whether the italian_leading_zero field has a value.
 */
i18n.phonenumbers.PhoneNumber.prototype.hasItalianLeadingZero = function() {
  return this.has$Value(4);
};


/**
 * @return {number} The number of values in the italian_leading_zero field.
 */
i18n.phonenumbers.PhoneNumber.prototype.italianLeadingZeroCount = function() {
  return this.count$Values(4);
};


/**
 * Clears the values in the italian_leading_zero field.
 */
i18n.phonenumbers.PhoneNumber.prototype.clearItalianLeadingZero = function() {
  this.clear$Field(4);
};


/**
 * Gets the value of the number_of_leading_zeros field.
 * @return {?number} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getNumberOfLeadingZeros = function() {
  return /** @type {?number} */ (this.get$Value(8));
};


/**
 * Gets the value of the number_of_leading_zeros field or the default value if not set.
 * @return {number} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getNumberOfLeadingZerosOrDefault = function() {
  return /** @type {number} */ (this.get$ValueOrDefault(8));
};


/**
 * Sets the value of the number_of_leading_zeros field.
 * @param {number} value The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.setNumberOfLeadingZeros = function(value) {
  this.set$Value(8, value);
};


/**
 * @return {boolean} Whether the number_of_leading_zeros field has a value.
 */
i18n.phonenumbers.PhoneNumber.prototype.hasNumberOfLeadingZeros = function() {
  return this.has$Value(8);
};


/**
 * @return {number} The number of values in the number_of_leading_zeros field.
 */
i18n.phonenumbers.PhoneNumber.prototype.numberOfLeadingZerosCount = function() {
  return this.count$Values(8);
};


/**
 * Clears the values in the number_of_leading_zeros field.
 */
i18n.phonenumbers.PhoneNumber.prototype.clearNumberOfLeadingZeros = function() {
  this.clear$Field(8);
};


/**
 * Gets the value of the raw_input field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getRawInput = function() {
  return /** @type {?string} */ (this.get$Value(5));
};


/**
 * Gets the value of the raw_input field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getRawInputOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(5));
};


/**
 * Sets the value of the raw_input field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.setRawInput = function(value) {
  this.set$Value(5, value);
};


/**
 * @return {boolean} Whether the raw_input field has a value.
 */
i18n.phonenumbers.PhoneNumber.prototype.hasRawInput = function() {
  return this.has$Value(5);
};


/**
 * @return {number} The number of values in the raw_input field.
 */
i18n.phonenumbers.PhoneNumber.prototype.rawInputCount = function() {
  return this.count$Values(5);
};


/**
 * Clears the values in the raw_input field.
 */
i18n.phonenumbers.PhoneNumber.prototype.clearRawInput = function() {
  this.clear$Field(5);
};


/**
 * Gets the value of the country_code_source field.
 * @return {?i18n.phonenumbers.PhoneNumber.CountryCodeSource} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getCountryCodeSource = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumber.CountryCodeSource} */ (this.get$Value(6));
};


/**
 * Gets the value of the country_code_source field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumber.CountryCodeSource} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getCountryCodeSourceOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumber.CountryCodeSource} */ (this.get$ValueOrDefault(6));
};


/**
 * Sets the value of the country_code_source field.
 * @param {!i18n.phonenumbers.PhoneNumber.CountryCodeSource} value The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.setCountryCodeSource = function(value) {
  this.set$Value(6, value);
};


/**
 * @return {boolean} Whether the country_code_source field has a value.
 */
i18n.phonenumbers.PhoneNumber.prototype.hasCountryCodeSource = function() {
  return this.has$Value(6);
};


/**
 * @return {number} The number of values in the country_code_source field.
 */
i18n.phonenumbers.PhoneNumber.prototype.countryCodeSourceCount = function() {
  return this.count$Values(6);
};


/**
 * Clears the values in the country_code_source field.
 */
i18n.phonenumbers.PhoneNumber.prototype.clearCountryCodeSource = function() {
  this.clear$Field(6);
};


/**
 * Gets the value of the preferred_domestic_carrier_code field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getPreferredDomesticCarrierCode = function() {
  return /** @type {?string} */ (this.get$Value(7));
};


/**
 * Gets the value of the preferred_domestic_carrier_code field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.getPreferredDomesticCarrierCodeOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(7));
};


/**
 * Sets the value of the preferred_domestic_carrier_code field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneNumber.prototype.setPreferredDomesticCarrierCode = function(value) {
  this.set$Value(7, value);
};


/**
 * @return {boolean} Whether the preferred_domestic_carrier_code field has a value.
 */
i18n.phonenumbers.PhoneNumber.prototype.hasPreferredDomesticCarrierCode = function() {
  return this.has$Value(7);
};


/**
 * @return {number} The number of values in the preferred_domestic_carrier_code field.
 */
i18n.phonenumbers.PhoneNumber.prototype.preferredDomesticCarrierCodeCount = function() {
  return this.count$Values(7);
};


/**
 * Clears the values in the preferred_domestic_carrier_code field.
 */
i18n.phonenumbers.PhoneNumber.prototype.clearPreferredDomesticCarrierCode = function() {
  this.clear$Field(7);
};


/**
 * Enumeration CountryCodeSource.
 * @enum {number}
 */
i18n.phonenumbers.PhoneNumber.CountryCodeSource = {
  UNSPECIFIED: 0,
  FROM_NUMBER_WITH_PLUS_SIGN: 1,
  FROM_NUMBER_WITH_IDD: 5,
  FROM_NUMBER_WITHOUT_PLUS_SIGN: 10,
  FROM_DEFAULT_COUNTRY: 20
};


/** @override */
i18n.phonenumbers.PhoneNumber.prototype.getDescriptor = function() {
  var descriptor = i18n.phonenumbers.PhoneNumber.descriptor_;
  if (!descriptor) {
    // The descriptor is created lazily when we instantiate a new instance.
    var descriptorObj = {
      0: {
        name: 'PhoneNumber',
        fullName: 'i18n.phonenumbers.PhoneNumber'
      },
      1: {
        name: 'country_code',
        required: true,
        fieldType: goog.proto2.Message.FieldType.INT32,
        type: Number
      },
      2: {
        name: 'national_number',
        required: true,
        fieldType: goog.proto2.Message.FieldType.UINT64,
        type: Number
      },
      3: {
        name: 'extension',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      4: {
        name: 'italian_leading_zero',
        fieldType: goog.proto2.Message.FieldType.BOOL,
        type: Boolean
      },
      8: {
        name: 'number_of_leading_zeros',
        fieldType: goog.proto2.Message.FieldType.INT32,
        defaultValue: 1,
        type: Number
      },
      5: {
        name: 'raw_input',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      6: {
        name: 'country_code_source',
        fieldType: goog.proto2.Message.FieldType.ENUM,
        defaultValue: i18n.phonenumbers.PhoneNumber.CountryCodeSource.UNSPECIFIED,
        type: i18n.phonenumbers.PhoneNumber.CountryCodeSource
      },
      7: {
        name: 'preferred_domestic_carrier_code',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      }
    };
    i18n.phonenumbers.PhoneNumber.descriptor_ = descriptor =
        goog.proto2.Message.createDescriptor(
             i18n.phonenumbers.PhoneNumber, descriptorObj);
  }
  return descriptor;
};


// Export getDescriptor static function robust to minification.
i18n.phonenumbers.PhoneNumber['ctor'] = i18n.phonenumbers.PhoneNumber;
i18n.phonenumbers.PhoneNumber['ctor'].getDescriptor =
    i18n.phonenumbers.PhoneNumber.prototype.getDescriptor;
