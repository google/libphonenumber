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
 * phonemetadata.proto.
 */

goog.provide('i18n.phonenumbers.NumberFormat');
goog.provide('i18n.phonenumbers.PhoneMetadata');
goog.provide('i18n.phonenumbers.PhoneMetadataCollection');
goog.provide('i18n.phonenumbers.PhoneNumberDesc');

goog.require('goog.proto2.Message');



/**
 * Message NumberFormat.
 * @constructor
 * @extends {goog.proto2.Message}
 * @final
 */
i18n.phonenumbers.NumberFormat = function() {
  goog.proto2.Message.call(this);
};
goog.inherits(i18n.phonenumbers.NumberFormat, goog.proto2.Message);


/**
 * Descriptor for this message, deserialized lazily in getDescriptor().
 * @private {?goog.proto2.Descriptor}
 */
i18n.phonenumbers.NumberFormat.descriptor_ = null;


/**
 * Overrides {@link goog.proto2.Message#clone} to specify its exact return type.
 * @return {!i18n.phonenumbers.NumberFormat} The cloned message.
 * @override
 */
i18n.phonenumbers.NumberFormat.prototype.clone;


/**
 * Gets the value of the pattern field.
 * @return {?string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getPattern = function() {
  return /** @type {?string} */ (this.get$Value(1));
};


/**
 * Gets the value of the pattern field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getPatternOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(1));
};


/**
 * Sets the value of the pattern field.
 * @param {string} value The value.
 */
i18n.phonenumbers.NumberFormat.prototype.setPattern = function(value) {
  this.set$Value(1, value);
};


/**
 * @return {boolean} Whether the pattern field has a value.
 */
i18n.phonenumbers.NumberFormat.prototype.hasPattern = function() {
  return this.has$Value(1);
};


/**
 * @return {number} The number of values in the pattern field.
 */
i18n.phonenumbers.NumberFormat.prototype.patternCount = function() {
  return this.count$Values(1);
};


/**
 * Clears the values in the pattern field.
 */
i18n.phonenumbers.NumberFormat.prototype.clearPattern = function() {
  this.clear$Field(1);
};


/**
 * Gets the value of the format field.
 * @return {?string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getFormat = function() {
  return /** @type {?string} */ (this.get$Value(2));
};


/**
 * Gets the value of the format field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getFormatOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(2));
};


/**
 * Sets the value of the format field.
 * @param {string} value The value.
 */
i18n.phonenumbers.NumberFormat.prototype.setFormat = function(value) {
  this.set$Value(2, value);
};


/**
 * @return {boolean} Whether the format field has a value.
 */
i18n.phonenumbers.NumberFormat.prototype.hasFormat = function() {
  return this.has$Value(2);
};


/**
 * @return {number} The number of values in the format field.
 */
i18n.phonenumbers.NumberFormat.prototype.formatCount = function() {
  return this.count$Values(2);
};


/**
 * Clears the values in the format field.
 */
i18n.phonenumbers.NumberFormat.prototype.clearFormat = function() {
  this.clear$Field(2);
};


/**
 * Gets the value of the leading_digits_pattern field at the index given.
 * @param {number} index The index to lookup.
 * @return {?string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getLeadingDigitsPattern = function(index) {
  return /** @type {?string} */ (this.get$Value(3, index));
};


/**
 * Gets the value of the leading_digits_pattern field at the index given or the default value if not set.
 * @param {number} index The index to lookup.
 * @return {string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getLeadingDigitsPatternOrDefault = function(index) {
  return /** @type {string} */ (this.get$ValueOrDefault(3, index));
};


/**
 * Adds a value to the leading_digits_pattern field.
 * @param {string} value The value to add.
 */
i18n.phonenumbers.NumberFormat.prototype.addLeadingDigitsPattern = function(value) {
  this.add$Value(3, value);
};


/**
 * Returns the array of values in the leading_digits_pattern field.
 * @return {!Array<string>} The values in the field.
 */
i18n.phonenumbers.NumberFormat.prototype.leadingDigitsPatternArray = function() {
  return /** @type {!Array<string>} */ (this.array$Values(3));
};


/**
 * @return {boolean} Whether the leading_digits_pattern field has a value.
 */
i18n.phonenumbers.NumberFormat.prototype.hasLeadingDigitsPattern = function() {
  return this.has$Value(3);
};


/**
 * @return {number} The number of values in the leading_digits_pattern field.
 */
i18n.phonenumbers.NumberFormat.prototype.leadingDigitsPatternCount = function() {
  return this.count$Values(3);
};


/**
 * Clears the values in the leading_digits_pattern field.
 */
i18n.phonenumbers.NumberFormat.prototype.clearLeadingDigitsPattern = function() {
  this.clear$Field(3);
};


/**
 * Gets the value of the national_prefix_formatting_rule field.
 * @return {?string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getNationalPrefixFormattingRule = function() {
  return /** @type {?string} */ (this.get$Value(4));
};


/**
 * Gets the value of the national_prefix_formatting_rule field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getNationalPrefixFormattingRuleOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(4));
};


/**
 * Sets the value of the national_prefix_formatting_rule field.
 * @param {string} value The value.
 */
i18n.phonenumbers.NumberFormat.prototype.setNationalPrefixFormattingRule = function(value) {
  this.set$Value(4, value);
};


/**
 * @return {boolean} Whether the national_prefix_formatting_rule field has a value.
 */
i18n.phonenumbers.NumberFormat.prototype.hasNationalPrefixFormattingRule = function() {
  return this.has$Value(4);
};


/**
 * @return {number} The number of values in the national_prefix_formatting_rule field.
 */
i18n.phonenumbers.NumberFormat.prototype.nationalPrefixFormattingRuleCount = function() {
  return this.count$Values(4);
};


/**
 * Clears the values in the national_prefix_formatting_rule field.
 */
i18n.phonenumbers.NumberFormat.prototype.clearNationalPrefixFormattingRule = function() {
  this.clear$Field(4);
};


/**
 * Gets the value of the national_prefix_optional_when_formatting field.
 * @return {?boolean} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getNationalPrefixOptionalWhenFormatting = function() {
  return /** @type {?boolean} */ (this.get$Value(6));
};


/**
 * Gets the value of the national_prefix_optional_when_formatting field or the default value if not set.
 * @return {boolean} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getNationalPrefixOptionalWhenFormattingOrDefault = function() {
  return /** @type {boolean} */ (this.get$ValueOrDefault(6));
};


/**
 * Sets the value of the national_prefix_optional_when_formatting field.
 * @param {boolean} value The value.
 */
i18n.phonenumbers.NumberFormat.prototype.setNationalPrefixOptionalWhenFormatting = function(value) {
  this.set$Value(6, value);
};


/**
 * @return {boolean} Whether the national_prefix_optional_when_formatting field has a value.
 */
i18n.phonenumbers.NumberFormat.prototype.hasNationalPrefixOptionalWhenFormatting = function() {
  return this.has$Value(6);
};


/**
 * @return {number} The number of values in the national_prefix_optional_when_formatting field.
 */
i18n.phonenumbers.NumberFormat.prototype.nationalPrefixOptionalWhenFormattingCount = function() {
  return this.count$Values(6);
};


/**
 * Clears the values in the national_prefix_optional_when_formatting field.
 */
i18n.phonenumbers.NumberFormat.prototype.clearNationalPrefixOptionalWhenFormatting = function() {
  this.clear$Field(6);
};


/**
 * Gets the value of the domestic_carrier_code_formatting_rule field.
 * @return {?string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getDomesticCarrierCodeFormattingRule = function() {
  return /** @type {?string} */ (this.get$Value(5));
};


/**
 * Gets the value of the domestic_carrier_code_formatting_rule field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.NumberFormat.prototype.getDomesticCarrierCodeFormattingRuleOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(5));
};


/**
 * Sets the value of the domestic_carrier_code_formatting_rule field.
 * @param {string} value The value.
 */
i18n.phonenumbers.NumberFormat.prototype.setDomesticCarrierCodeFormattingRule = function(value) {
  this.set$Value(5, value);
};


/**
 * @return {boolean} Whether the domestic_carrier_code_formatting_rule field has a value.
 */
i18n.phonenumbers.NumberFormat.prototype.hasDomesticCarrierCodeFormattingRule = function() {
  return this.has$Value(5);
};


/**
 * @return {number} The number of values in the domestic_carrier_code_formatting_rule field.
 */
i18n.phonenumbers.NumberFormat.prototype.domesticCarrierCodeFormattingRuleCount = function() {
  return this.count$Values(5);
};


/**
 * Clears the values in the domestic_carrier_code_formatting_rule field.
 */
i18n.phonenumbers.NumberFormat.prototype.clearDomesticCarrierCodeFormattingRule = function() {
  this.clear$Field(5);
};



/**
 * Message PhoneNumberDesc.
 * @constructor
 * @extends {goog.proto2.Message}
 * @final
 */
i18n.phonenumbers.PhoneNumberDesc = function() {
  goog.proto2.Message.call(this);
};
goog.inherits(i18n.phonenumbers.PhoneNumberDesc, goog.proto2.Message);


/**
 * Descriptor for this message, deserialized lazily in getDescriptor().
 * @private {?goog.proto2.Descriptor}
 */
i18n.phonenumbers.PhoneNumberDesc.descriptor_ = null;


/**
 * Overrides {@link goog.proto2.Message#clone} to specify its exact return type.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The cloned message.
 * @override
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.clone;


/**
 * Gets the value of the national_number_pattern field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getNationalNumberPattern = function() {
  return /** @type {?string} */ (this.get$Value(2));
};


/**
 * Gets the value of the national_number_pattern field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getNationalNumberPatternOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(2));
};


/**
 * Sets the value of the national_number_pattern field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.setNationalNumberPattern = function(value) {
  this.set$Value(2, value);
};


/**
 * @return {boolean} Whether the national_number_pattern field has a value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.hasNationalNumberPattern = function() {
  return this.has$Value(2);
};


/**
 * @return {number} The number of values in the national_number_pattern field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.nationalNumberPatternCount = function() {
  return this.count$Values(2);
};


/**
 * Clears the values in the national_number_pattern field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.clearNationalNumberPattern = function() {
  this.clear$Field(2);
};


/**
 * Gets the value of the possible_number_pattern field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getPossibleNumberPattern = function() {
  return /** @type {?string} */ (this.get$Value(3));
};


/**
 * Gets the value of the possible_number_pattern field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getPossibleNumberPatternOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(3));
};


/**
 * Sets the value of the possible_number_pattern field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.setPossibleNumberPattern = function(value) {
  this.set$Value(3, value);
};


/**
 * @return {boolean} Whether the possible_number_pattern field has a value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.hasPossibleNumberPattern = function() {
  return this.has$Value(3);
};


/**
 * @return {number} The number of values in the possible_number_pattern field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.possibleNumberPatternCount = function() {
  return this.count$Values(3);
};


/**
 * Clears the values in the possible_number_pattern field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.clearPossibleNumberPattern = function() {
  this.clear$Field(3);
};


/**
 * Gets the value of the possible_length field at the index given.
 * @param {number} index The index to lookup.
 * @return {?number} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getPossibleLength = function(index) {
  return /** @type {?number} */ (this.get$Value(9, index));
};


/**
 * Gets the value of the possible_length field at the index given or the default value if not set.
 * @param {number} index The index to lookup.
 * @return {number} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getPossibleLengthOrDefault = function(index) {
  return /** @type {number} */ (this.get$ValueOrDefault(9, index));
};


/**
 * Adds a value to the possible_length field.
 * @param {number} value The value to add.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.addPossibleLength = function(value) {
  this.add$Value(9, value);
};


/**
 * Returns the array of values in the possible_length field.
 * @return {!Array<number>} The values in the field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.possibleLengthArray = function() {
  return /** @type {!Array<number>} */ (this.array$Values(9));
};


/**
 * @return {boolean} Whether the possible_length field has a value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.hasPossibleLength = function() {
  return this.has$Value(9);
};


/**
 * @return {number} The number of values in the possible_length field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.possibleLengthCount = function() {
  return this.count$Values(9);
};


/**
 * Clears the values in the possible_length field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.clearPossibleLength = function() {
  this.clear$Field(9);
};


/**
 * Gets the value of the possible_length_local_only field at the index given.
 * @param {number} index The index to lookup.
 * @return {?number} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getPossibleLengthLocalOnly = function(index) {
  return /** @type {?number} */ (this.get$Value(10, index));
};


/**
 * Gets the value of the possible_length_local_only field at the index given or the default value if not set.
 * @param {number} index The index to lookup.
 * @return {number} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getPossibleLengthLocalOnlyOrDefault = function(index) {
  return /** @type {number} */ (this.get$ValueOrDefault(10, index));
};


/**
 * Adds a value to the possible_length_local_only field.
 * @param {number} value The value to add.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.addPossibleLengthLocalOnly = function(value) {
  this.add$Value(10, value);
};


/**
 * Returns the array of values in the possible_length_local_only field.
 * @return {!Array<number>} The values in the field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.possibleLengthLocalOnlyArray = function() {
  return /** @type {!Array<number>} */ (this.array$Values(10));
};


/**
 * @return {boolean} Whether the possible_length_local_only field has a value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.hasPossibleLengthLocalOnly = function() {
  return this.has$Value(10);
};


/**
 * @return {number} The number of values in the possible_length_local_only field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.possibleLengthLocalOnlyCount = function() {
  return this.count$Values(10);
};


/**
 * Clears the values in the possible_length_local_only field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.clearPossibleLengthLocalOnly = function() {
  this.clear$Field(10);
};


/**
 * Gets the value of the example_number field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getExampleNumber = function() {
  return /** @type {?string} */ (this.get$Value(6));
};


/**
 * Gets the value of the example_number field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getExampleNumberOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(6));
};


/**
 * Sets the value of the example_number field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.setExampleNumber = function(value) {
  this.set$Value(6, value);
};


/**
 * @return {boolean} Whether the example_number field has a value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.hasExampleNumber = function() {
  return this.has$Value(6);
};


/**
 * @return {number} The number of values in the example_number field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.exampleNumberCount = function() {
  return this.count$Values(6);
};


/**
 * Clears the values in the example_number field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.clearExampleNumber = function() {
  this.clear$Field(6);
};


/**
 * Gets the value of the national_number_matcher_data field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getNationalNumberMatcherData = function() {
  return /** @type {?string} */ (this.get$Value(7));
};


/**
 * Gets the value of the national_number_matcher_data field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.getNationalNumberMatcherDataOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(7));
};


/**
 * Sets the value of the national_number_matcher_data field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.setNationalNumberMatcherData = function(value) {
  this.set$Value(7, value);
};


/**
 * @return {boolean} Whether the national_number_matcher_data field has a value.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.hasNationalNumberMatcherData = function() {
  return this.has$Value(7);
};


/**
 * @return {number} The number of values in the national_number_matcher_data field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.nationalNumberMatcherDataCount = function() {
  return this.count$Values(7);
};


/**
 * Clears the values in the national_number_matcher_data field.
 */
i18n.phonenumbers.PhoneNumberDesc.prototype.clearNationalNumberMatcherData = function() {
  this.clear$Field(7);
};



/**
 * Message PhoneMetadata.
 * @constructor
 * @extends {goog.proto2.Message}
 * @final
 */
i18n.phonenumbers.PhoneMetadata = function() {
  goog.proto2.Message.call(this);
};
goog.inherits(i18n.phonenumbers.PhoneMetadata, goog.proto2.Message);


/**
 * Descriptor for this message, deserialized lazily in getDescriptor().
 * @private {?goog.proto2.Descriptor}
 */
i18n.phonenumbers.PhoneMetadata.descriptor_ = null;


/**
 * Overrides {@link goog.proto2.Message#clone} to specify its exact return type.
 * @return {!i18n.phonenumbers.PhoneMetadata} The cloned message.
 * @override
 */
i18n.phonenumbers.PhoneMetadata.prototype.clone;


/**
 * Gets the value of the general_desc field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getGeneralDesc = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(1));
};


/**
 * Gets the value of the general_desc field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getGeneralDescOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(1));
};


/**
 * Sets the value of the general_desc field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setGeneralDesc = function(value) {
  this.set$Value(1, value);
};


/**
 * @return {boolean} Whether the general_desc field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasGeneralDesc = function() {
  return this.has$Value(1);
};


/**
 * @return {number} The number of values in the general_desc field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.generalDescCount = function() {
  return this.count$Values(1);
};


/**
 * Clears the values in the general_desc field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearGeneralDesc = function() {
  this.clear$Field(1);
};


/**
 * Gets the value of the fixed_line field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getFixedLine = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(2));
};


/**
 * Gets the value of the fixed_line field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getFixedLineOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(2));
};


/**
 * Sets the value of the fixed_line field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setFixedLine = function(value) {
  this.set$Value(2, value);
};


/**
 * @return {boolean} Whether the fixed_line field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasFixedLine = function() {
  return this.has$Value(2);
};


/**
 * @return {number} The number of values in the fixed_line field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.fixedLineCount = function() {
  return this.count$Values(2);
};


/**
 * Clears the values in the fixed_line field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearFixedLine = function() {
  this.clear$Field(2);
};


/**
 * Gets the value of the mobile field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getMobile = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(3));
};


/**
 * Gets the value of the mobile field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getMobileOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(3));
};


/**
 * Sets the value of the mobile field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setMobile = function(value) {
  this.set$Value(3, value);
};


/**
 * @return {boolean} Whether the mobile field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasMobile = function() {
  return this.has$Value(3);
};


/**
 * @return {number} The number of values in the mobile field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.mobileCount = function() {
  return this.count$Values(3);
};


/**
 * Clears the values in the mobile field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearMobile = function() {
  this.clear$Field(3);
};


/**
 * Gets the value of the toll_free field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getTollFree = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(4));
};


/**
 * Gets the value of the toll_free field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getTollFreeOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(4));
};


/**
 * Sets the value of the toll_free field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setTollFree = function(value) {
  this.set$Value(4, value);
};


/**
 * @return {boolean} Whether the toll_free field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasTollFree = function() {
  return this.has$Value(4);
};


/**
 * @return {number} The number of values in the toll_free field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.tollFreeCount = function() {
  return this.count$Values(4);
};


/**
 * Clears the values in the toll_free field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearTollFree = function() {
  this.clear$Field(4);
};


/**
 * Gets the value of the premium_rate field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPremiumRate = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(5));
};


/**
 * Gets the value of the premium_rate field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPremiumRateOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(5));
};


/**
 * Sets the value of the premium_rate field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setPremiumRate = function(value) {
  this.set$Value(5, value);
};


/**
 * @return {boolean} Whether the premium_rate field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasPremiumRate = function() {
  return this.has$Value(5);
};


/**
 * @return {number} The number of values in the premium_rate field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.premiumRateCount = function() {
  return this.count$Values(5);
};


/**
 * Clears the values in the premium_rate field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearPremiumRate = function() {
  this.clear$Field(5);
};


/**
 * Gets the value of the shared_cost field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getSharedCost = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(6));
};


/**
 * Gets the value of the shared_cost field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getSharedCostOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(6));
};


/**
 * Sets the value of the shared_cost field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setSharedCost = function(value) {
  this.set$Value(6, value);
};


/**
 * @return {boolean} Whether the shared_cost field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasSharedCost = function() {
  return this.has$Value(6);
};


/**
 * @return {number} The number of values in the shared_cost field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.sharedCostCount = function() {
  return this.count$Values(6);
};


/**
 * Clears the values in the shared_cost field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearSharedCost = function() {
  this.clear$Field(6);
};


/**
 * Gets the value of the personal_number field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPersonalNumber = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(7));
};


/**
 * Gets the value of the personal_number field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPersonalNumberOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(7));
};


/**
 * Sets the value of the personal_number field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setPersonalNumber = function(value) {
  this.set$Value(7, value);
};


/**
 * @return {boolean} Whether the personal_number field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasPersonalNumber = function() {
  return this.has$Value(7);
};


/**
 * @return {number} The number of values in the personal_number field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.personalNumberCount = function() {
  return this.count$Values(7);
};


/**
 * Clears the values in the personal_number field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearPersonalNumber = function() {
  this.clear$Field(7);
};


/**
 * Gets the value of the voip field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getVoip = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(8));
};


/**
 * Gets the value of the voip field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getVoipOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(8));
};


/**
 * Sets the value of the voip field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setVoip = function(value) {
  this.set$Value(8, value);
};


/**
 * @return {boolean} Whether the voip field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasVoip = function() {
  return this.has$Value(8);
};


/**
 * @return {number} The number of values in the voip field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.voipCount = function() {
  return this.count$Values(8);
};


/**
 * Clears the values in the voip field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearVoip = function() {
  this.clear$Field(8);
};


/**
 * Gets the value of the pager field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPager = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(21));
};


/**
 * Gets the value of the pager field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPagerOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(21));
};


/**
 * Sets the value of the pager field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setPager = function(value) {
  this.set$Value(21, value);
};


/**
 * @return {boolean} Whether the pager field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasPager = function() {
  return this.has$Value(21);
};


/**
 * @return {number} The number of values in the pager field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.pagerCount = function() {
  return this.count$Values(21);
};


/**
 * Clears the values in the pager field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearPager = function() {
  this.clear$Field(21);
};


/**
 * Gets the value of the uan field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getUan = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(25));
};


/**
 * Gets the value of the uan field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getUanOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(25));
};


/**
 * Sets the value of the uan field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setUan = function(value) {
  this.set$Value(25, value);
};


/**
 * @return {boolean} Whether the uan field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasUan = function() {
  return this.has$Value(25);
};


/**
 * @return {number} The number of values in the uan field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.uanCount = function() {
  return this.count$Values(25);
};


/**
 * Clears the values in the uan field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearUan = function() {
  this.clear$Field(25);
};


/**
 * Gets the value of the emergency field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getEmergency = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(27));
};


/**
 * Gets the value of the emergency field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getEmergencyOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(27));
};


/**
 * Sets the value of the emergency field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setEmergency = function(value) {
  this.set$Value(27, value);
};


/**
 * @return {boolean} Whether the emergency field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasEmergency = function() {
  return this.has$Value(27);
};


/**
 * @return {number} The number of values in the emergency field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.emergencyCount = function() {
  return this.count$Values(27);
};


/**
 * Clears the values in the emergency field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearEmergency = function() {
  this.clear$Field(27);
};


/**
 * Gets the value of the voicemail field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getVoicemail = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(28));
};


/**
 * Gets the value of the voicemail field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getVoicemailOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(28));
};


/**
 * Sets the value of the voicemail field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setVoicemail = function(value) {
  this.set$Value(28, value);
};


/**
 * @return {boolean} Whether the voicemail field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasVoicemail = function() {
  return this.has$Value(28);
};


/**
 * @return {number} The number of values in the voicemail field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.voicemailCount = function() {
  return this.count$Values(28);
};


/**
 * Clears the values in the voicemail field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearVoicemail = function() {
  this.clear$Field(28);
};


/**
 * Gets the value of the no_international_dialling field.
 * @return {?i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNoInternationalDialling = function() {
  return /** @type {?i18n.phonenumbers.PhoneNumberDesc} */ (this.get$Value(24));
};


/**
 * Gets the value of the no_international_dialling field or the default value if not set.
 * @return {!i18n.phonenumbers.PhoneNumberDesc} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNoInternationalDiallingOrDefault = function() {
  return /** @type {!i18n.phonenumbers.PhoneNumberDesc} */ (this.get$ValueOrDefault(24));
};


/**
 * Sets the value of the no_international_dialling field.
 * @param {!i18n.phonenumbers.PhoneNumberDesc} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setNoInternationalDialling = function(value) {
  this.set$Value(24, value);
};


/**
 * @return {boolean} Whether the no_international_dialling field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasNoInternationalDialling = function() {
  return this.has$Value(24);
};


/**
 * @return {number} The number of values in the no_international_dialling field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.noInternationalDiallingCount = function() {
  return this.count$Values(24);
};


/**
 * Clears the values in the no_international_dialling field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearNoInternationalDialling = function() {
  this.clear$Field(24);
};


/**
 * Gets the value of the id field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getId = function() {
  return /** @type {?string} */ (this.get$Value(9));
};


/**
 * Gets the value of the id field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getIdOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(9));
};


/**
 * Sets the value of the id field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setId = function(value) {
  this.set$Value(9, value);
};


/**
 * @return {boolean} Whether the id field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasId = function() {
  return this.has$Value(9);
};


/**
 * @return {number} The number of values in the id field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.idCount = function() {
  return this.count$Values(9);
};


/**
 * Clears the values in the id field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearId = function() {
  this.clear$Field(9);
};


/**
 * Gets the value of the country_code field.
 * @return {?number} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getCountryCode = function() {
  return /** @type {?number} */ (this.get$Value(10));
};


/**
 * Gets the value of the country_code field or the default value if not set.
 * @return {number} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getCountryCodeOrDefault = function() {
  return /** @type {number} */ (this.get$ValueOrDefault(10));
};


/**
 * Sets the value of the country_code field.
 * @param {number} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setCountryCode = function(value) {
  this.set$Value(10, value);
};


/**
 * @return {boolean} Whether the country_code field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasCountryCode = function() {
  return this.has$Value(10);
};


/**
 * @return {number} The number of values in the country_code field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.countryCodeCount = function() {
  return this.count$Values(10);
};


/**
 * Clears the values in the country_code field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearCountryCode = function() {
  this.clear$Field(10);
};


/**
 * Gets the value of the international_prefix field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getInternationalPrefix = function() {
  return /** @type {?string} */ (this.get$Value(11));
};


/**
 * Gets the value of the international_prefix field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getInternationalPrefixOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(11));
};


/**
 * Sets the value of the international_prefix field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setInternationalPrefix = function(value) {
  this.set$Value(11, value);
};


/**
 * @return {boolean} Whether the international_prefix field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasInternationalPrefix = function() {
  return this.has$Value(11);
};


/**
 * @return {number} The number of values in the international_prefix field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.internationalPrefixCount = function() {
  return this.count$Values(11);
};


/**
 * Clears the values in the international_prefix field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearInternationalPrefix = function() {
  this.clear$Field(11);
};


/**
 * Gets the value of the preferred_international_prefix field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPreferredInternationalPrefix = function() {
  return /** @type {?string} */ (this.get$Value(17));
};


/**
 * Gets the value of the preferred_international_prefix field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPreferredInternationalPrefixOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(17));
};


/**
 * Sets the value of the preferred_international_prefix field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setPreferredInternationalPrefix = function(value) {
  this.set$Value(17, value);
};


/**
 * @return {boolean} Whether the preferred_international_prefix field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasPreferredInternationalPrefix = function() {
  return this.has$Value(17);
};


/**
 * @return {number} The number of values in the preferred_international_prefix field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.preferredInternationalPrefixCount = function() {
  return this.count$Values(17);
};


/**
 * Clears the values in the preferred_international_prefix field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearPreferredInternationalPrefix = function() {
  this.clear$Field(17);
};


/**
 * Gets the value of the national_prefix field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNationalPrefix = function() {
  return /** @type {?string} */ (this.get$Value(12));
};


/**
 * Gets the value of the national_prefix field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNationalPrefixOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(12));
};


/**
 * Sets the value of the national_prefix field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setNationalPrefix = function(value) {
  this.set$Value(12, value);
};


/**
 * @return {boolean} Whether the national_prefix field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasNationalPrefix = function() {
  return this.has$Value(12);
};


/**
 * @return {number} The number of values in the national_prefix field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.nationalPrefixCount = function() {
  return this.count$Values(12);
};


/**
 * Clears the values in the national_prefix field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearNationalPrefix = function() {
  this.clear$Field(12);
};


/**
 * Gets the value of the preferred_extn_prefix field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPreferredExtnPrefix = function() {
  return /** @type {?string} */ (this.get$Value(13));
};


/**
 * Gets the value of the preferred_extn_prefix field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getPreferredExtnPrefixOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(13));
};


/**
 * Sets the value of the preferred_extn_prefix field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setPreferredExtnPrefix = function(value) {
  this.set$Value(13, value);
};


/**
 * @return {boolean} Whether the preferred_extn_prefix field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasPreferredExtnPrefix = function() {
  return this.has$Value(13);
};


/**
 * @return {number} The number of values in the preferred_extn_prefix field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.preferredExtnPrefixCount = function() {
  return this.count$Values(13);
};


/**
 * Clears the values in the preferred_extn_prefix field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearPreferredExtnPrefix = function() {
  this.clear$Field(13);
};


/**
 * Gets the value of the national_prefix_for_parsing field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNationalPrefixForParsing = function() {
  return /** @type {?string} */ (this.get$Value(15));
};


/**
 * Gets the value of the national_prefix_for_parsing field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNationalPrefixForParsingOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(15));
};


/**
 * Sets the value of the national_prefix_for_parsing field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setNationalPrefixForParsing = function(value) {
  this.set$Value(15, value);
};


/**
 * @return {boolean} Whether the national_prefix_for_parsing field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasNationalPrefixForParsing = function() {
  return this.has$Value(15);
};


/**
 * @return {number} The number of values in the national_prefix_for_parsing field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.nationalPrefixForParsingCount = function() {
  return this.count$Values(15);
};


/**
 * Clears the values in the national_prefix_for_parsing field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearNationalPrefixForParsing = function() {
  this.clear$Field(15);
};


/**
 * Gets the value of the national_prefix_transform_rule field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNationalPrefixTransformRule = function() {
  return /** @type {?string} */ (this.get$Value(16));
};


/**
 * Gets the value of the national_prefix_transform_rule field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNationalPrefixTransformRuleOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(16));
};


/**
 * Sets the value of the national_prefix_transform_rule field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setNationalPrefixTransformRule = function(value) {
  this.set$Value(16, value);
};


/**
 * @return {boolean} Whether the national_prefix_transform_rule field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasNationalPrefixTransformRule = function() {
  return this.has$Value(16);
};


/**
 * @return {number} The number of values in the national_prefix_transform_rule field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.nationalPrefixTransformRuleCount = function() {
  return this.count$Values(16);
};


/**
 * Clears the values in the national_prefix_transform_rule field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearNationalPrefixTransformRule = function() {
  this.clear$Field(16);
};


/**
 * Gets the value of the same_mobile_and_fixed_line_pattern field.
 * @return {?boolean} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getSameMobileAndFixedLinePattern = function() {
  return /** @type {?boolean} */ (this.get$Value(18));
};


/**
 * Gets the value of the same_mobile_and_fixed_line_pattern field or the default value if not set.
 * @return {boolean} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getSameMobileAndFixedLinePatternOrDefault = function() {
  return /** @type {boolean} */ (this.get$ValueOrDefault(18));
};


/**
 * Sets the value of the same_mobile_and_fixed_line_pattern field.
 * @param {boolean} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setSameMobileAndFixedLinePattern = function(value) {
  this.set$Value(18, value);
};


/**
 * @return {boolean} Whether the same_mobile_and_fixed_line_pattern field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasSameMobileAndFixedLinePattern = function() {
  return this.has$Value(18);
};


/**
 * @return {number} The number of values in the same_mobile_and_fixed_line_pattern field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.sameMobileAndFixedLinePatternCount = function() {
  return this.count$Values(18);
};


/**
 * Clears the values in the same_mobile_and_fixed_line_pattern field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearSameMobileAndFixedLinePattern = function() {
  this.clear$Field(18);
};


/**
 * Gets the value of the number_format field at the index given.
 * @param {number} index The index to lookup.
 * @return {?i18n.phonenumbers.NumberFormat} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNumberFormat = function(index) {
  return /** @type {?i18n.phonenumbers.NumberFormat} */ (this.get$Value(19, index));
};


/**
 * Gets the value of the number_format field at the index given or the default value if not set.
 * @param {number} index The index to lookup.
 * @return {!i18n.phonenumbers.NumberFormat} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getNumberFormatOrDefault = function(index) {
  return /** @type {!i18n.phonenumbers.NumberFormat} */ (this.get$ValueOrDefault(19, index));
};


/**
 * Adds a value to the number_format field.
 * @param {!i18n.phonenumbers.NumberFormat} value The value to add.
 */
i18n.phonenumbers.PhoneMetadata.prototype.addNumberFormat = function(value) {
  this.add$Value(19, value);
};


/**
 * Returns the array of values in the number_format field.
 * @return {!Array<!i18n.phonenumbers.NumberFormat>} The values in the field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.numberFormatArray = function() {
  return /** @type {!Array<!i18n.phonenumbers.NumberFormat>} */ (this.array$Values(19));
};


/**
 * @return {boolean} Whether the number_format field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasNumberFormat = function() {
  return this.has$Value(19);
};


/**
 * @return {number} The number of values in the number_format field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.numberFormatCount = function() {
  return this.count$Values(19);
};


/**
 * Clears the values in the number_format field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearNumberFormat = function() {
  this.clear$Field(19);
};


/**
 * Gets the value of the intl_number_format field at the index given.
 * @param {number} index The index to lookup.
 * @return {?i18n.phonenumbers.NumberFormat} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getIntlNumberFormat = function(index) {
  return /** @type {?i18n.phonenumbers.NumberFormat} */ (this.get$Value(20, index));
};


/**
 * Gets the value of the intl_number_format field at the index given or the default value if not set.
 * @param {number} index The index to lookup.
 * @return {!i18n.phonenumbers.NumberFormat} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getIntlNumberFormatOrDefault = function(index) {
  return /** @type {!i18n.phonenumbers.NumberFormat} */ (this.get$ValueOrDefault(20, index));
};


/**
 * Adds a value to the intl_number_format field.
 * @param {!i18n.phonenumbers.NumberFormat} value The value to add.
 */
i18n.phonenumbers.PhoneMetadata.prototype.addIntlNumberFormat = function(value) {
  this.add$Value(20, value);
};


/**
 * Returns the array of values in the intl_number_format field.
 * @return {!Array<!i18n.phonenumbers.NumberFormat>} The values in the field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.intlNumberFormatArray = function() {
  return /** @type {!Array<!i18n.phonenumbers.NumberFormat>} */ (this.array$Values(20));
};


/**
 * @return {boolean} Whether the intl_number_format field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasIntlNumberFormat = function() {
  return this.has$Value(20);
};


/**
 * @return {number} The number of values in the intl_number_format field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.intlNumberFormatCount = function() {
  return this.count$Values(20);
};


/**
 * Clears the values in the intl_number_format field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearIntlNumberFormat = function() {
  this.clear$Field(20);
};


/**
 * Gets the value of the main_country_for_code field.
 * @return {?boolean} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getMainCountryForCode = function() {
  return /** @type {?boolean} */ (this.get$Value(22));
};


/**
 * Gets the value of the main_country_for_code field or the default value if not set.
 * @return {boolean} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getMainCountryForCodeOrDefault = function() {
  return /** @type {boolean} */ (this.get$ValueOrDefault(22));
};


/**
 * Sets the value of the main_country_for_code field.
 * @param {boolean} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setMainCountryForCode = function(value) {
  this.set$Value(22, value);
};


/**
 * @return {boolean} Whether the main_country_for_code field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasMainCountryForCode = function() {
  return this.has$Value(22);
};


/**
 * @return {number} The number of values in the main_country_for_code field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.mainCountryForCodeCount = function() {
  return this.count$Values(22);
};


/**
 * Clears the values in the main_country_for_code field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearMainCountryForCode = function() {
  this.clear$Field(22);
};


/**
 * Gets the value of the leading_digits field.
 * @return {?string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getLeadingDigits = function() {
  return /** @type {?string} */ (this.get$Value(23));
};


/**
 * Gets the value of the leading_digits field or the default value if not set.
 * @return {string} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getLeadingDigitsOrDefault = function() {
  return /** @type {string} */ (this.get$ValueOrDefault(23));
};


/**
 * Sets the value of the leading_digits field.
 * @param {string} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setLeadingDigits = function(value) {
  this.set$Value(23, value);
};


/**
 * @return {boolean} Whether the leading_digits field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasLeadingDigits = function() {
  return this.has$Value(23);
};


/**
 * @return {number} The number of values in the leading_digits field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.leadingDigitsCount = function() {
  return this.count$Values(23);
};


/**
 * Clears the values in the leading_digits field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearLeadingDigits = function() {
  this.clear$Field(23);
};


/**
 * Gets the value of the leading_zero_possible field.
 * @return {?boolean} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getLeadingZeroPossible = function() {
  return /** @type {?boolean} */ (this.get$Value(26));
};


/**
 * Gets the value of the leading_zero_possible field or the default value if not set.
 * @return {boolean} The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.getLeadingZeroPossibleOrDefault = function() {
  return /** @type {boolean} */ (this.get$ValueOrDefault(26));
};


/**
 * Sets the value of the leading_zero_possible field.
 * @param {boolean} value The value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.setLeadingZeroPossible = function(value) {
  this.set$Value(26, value);
};


/**
 * @return {boolean} Whether the leading_zero_possible field has a value.
 */
i18n.phonenumbers.PhoneMetadata.prototype.hasLeadingZeroPossible = function() {
  return this.has$Value(26);
};


/**
 * @return {number} The number of values in the leading_zero_possible field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.leadingZeroPossibleCount = function() {
  return this.count$Values(26);
};


/**
 * Clears the values in the leading_zero_possible field.
 */
i18n.phonenumbers.PhoneMetadata.prototype.clearLeadingZeroPossible = function() {
  this.clear$Field(26);
};


/**
 * Message PhoneMetadataCollection.
 * @constructor
 * @extends {goog.proto2.Message}
 * @final
 */
i18n.phonenumbers.PhoneMetadataCollection = function() {
  goog.proto2.Message.call(this);
};
goog.inherits(i18n.phonenumbers.PhoneMetadataCollection, goog.proto2.Message);


/**
 * Descriptor for this message, deserialized lazily in getDescriptor().
 * @private {?goog.proto2.Descriptor}
 */
i18n.phonenumbers.PhoneMetadataCollection.descriptor_ = null;


/**
 * Overrides {@link goog.proto2.Message#clone} to specify its exact return type.
 * @return {!i18n.phonenumbers.PhoneMetadataCollection} The cloned message.
 * @override
 */
i18n.phonenumbers.PhoneMetadataCollection.prototype.clone;


/**
 * Gets the value of the metadata field at the index given.
 * @param {number} index The index to lookup.
 * @return {?i18n.phonenumbers.PhoneMetadata} The value.
 */
i18n.phonenumbers.PhoneMetadataCollection.prototype.getMetadata = function(index) {
  return /** @type {?i18n.phonenumbers.PhoneMetadata} */ (this.get$Value(1, index));
};


/**
 * Gets the value of the metadata field at the index given or the default value if not set.
 * @param {number} index The index to lookup.
 * @return {!i18n.phonenumbers.PhoneMetadata} The value.
 */
i18n.phonenumbers.PhoneMetadataCollection.prototype.getMetadataOrDefault = function(index) {
  return /** @type {!i18n.phonenumbers.PhoneMetadata} */ (this.get$ValueOrDefault(1, index));
};


/**
 * Adds a value to the metadata field.
 * @param {!i18n.phonenumbers.PhoneMetadata} value The value to add.
 */
i18n.phonenumbers.PhoneMetadataCollection.prototype.addMetadata = function(value) {
  this.add$Value(1, value);
};


/**
 * Returns the array of values in the metadata field.
 * @return {!Array<!i18n.phonenumbers.PhoneMetadata>} The values in the field.
 */
i18n.phonenumbers.PhoneMetadataCollection.prototype.metadataArray = function() {
  return /** @type {!Array<!i18n.phonenumbers.PhoneMetadata>} */ (this.array$Values(1));
};


/**
 * @return {boolean} Whether the metadata field has a value.
 */
i18n.phonenumbers.PhoneMetadataCollection.prototype.hasMetadata = function() {
  return this.has$Value(1);
};


/**
 * @return {number} The number of values in the metadata field.
 */
i18n.phonenumbers.PhoneMetadataCollection.prototype.metadataCount = function() {
  return this.count$Values(1);
};


/**
 * Clears the values in the metadata field.
 */
i18n.phonenumbers.PhoneMetadataCollection.prototype.clearMetadata = function() {
  this.clear$Field(1);
};


/** @override */
i18n.phonenumbers.NumberFormat.prototype.getDescriptor = function() {
  var descriptor = i18n.phonenumbers.NumberFormat.descriptor_;
  if (!descriptor) {
    // The descriptor is created lazily when we instantiate a new instance.
    var descriptorObj = {
      0: {
        name: 'NumberFormat',
        fullName: 'i18n.phonenumbers.NumberFormat'
      },
      1: {
        name: 'pattern',
        required: true,
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      2: {
        name: 'format',
        required: true,
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      3: {
        name: 'leading_digits_pattern',
        repeated: true,
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      4: {
        name: 'national_prefix_formatting_rule',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      6: {
        name: 'national_prefix_optional_when_formatting',
        fieldType: goog.proto2.Message.FieldType.BOOL,
        defaultValue: false,
        type: Boolean
      },
      5: {
        name: 'domestic_carrier_code_formatting_rule',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      }
    };
    i18n.phonenumbers.NumberFormat.descriptor_ = descriptor =
        goog.proto2.Message.createDescriptor(
             i18n.phonenumbers.NumberFormat, descriptorObj);
  }
  return descriptor;
};


/** @nocollapse */
i18n.phonenumbers.NumberFormat.getDescriptor =
    i18n.phonenumbers.NumberFormat.prototype.getDescriptor;


/** @override */
i18n.phonenumbers.PhoneNumberDesc.prototype.getDescriptor = function() {
  var descriptor = i18n.phonenumbers.PhoneNumberDesc.descriptor_;
  if (!descriptor) {
    // The descriptor is created lazily when we instantiate a new instance.
    var descriptorObj = {
      0: {
        name: 'PhoneNumberDesc',
        fullName: 'i18n.phonenumbers.PhoneNumberDesc'
      },
      2: {
        name: 'national_number_pattern',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      3: {
        name: 'possible_number_pattern',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      9: {
        name: 'possible_length',
        repeated: true,
        fieldType: goog.proto2.Message.FieldType.INT32,
        type: Number
      },
      10: {
        name: 'possible_length_local_only',
        repeated: true,
        fieldType: goog.proto2.Message.FieldType.INT32,
        type: Number
      },
      6: {
        name: 'example_number',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      7: {
        name: 'national_number_matcher_data',
        fieldType: goog.proto2.Message.FieldType.BYTES,
        type: String
      }
    };
    i18n.phonenumbers.PhoneNumberDesc.descriptor_ = descriptor =
        goog.proto2.Message.createDescriptor(
             i18n.phonenumbers.PhoneNumberDesc, descriptorObj);
  }
  return descriptor;
};


/** @nocollapse */
i18n.phonenumbers.PhoneNumberDesc.getDescriptor =
    i18n.phonenumbers.PhoneNumberDesc.prototype.getDescriptor;


/** @override */
i18n.phonenumbers.PhoneMetadata.prototype.getDescriptor = function() {
  var descriptor = i18n.phonenumbers.PhoneMetadata.descriptor_;
  if (!descriptor) {
    // The descriptor is created lazily when we instantiate a new instance.
    var descriptorObj = {
      0: {
        name: 'PhoneMetadata',
        fullName: 'i18n.phonenumbers.PhoneMetadata'
      },
      1: {
        name: 'general_desc',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      2: {
        name: 'fixed_line',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      3: {
        name: 'mobile',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      4: {
        name: 'toll_free',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      5: {
        name: 'premium_rate',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      6: {
        name: 'shared_cost',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      7: {
        name: 'personal_number',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      8: {
        name: 'voip',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      21: {
        name: 'pager',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      25: {
        name: 'uan',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      27: {
        name: 'emergency',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      28: {
        name: 'voicemail',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      24: {
        name: 'no_international_dialling',
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneNumberDesc
      },
      9: {
        name: 'id',
        required: true,
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      10: {
        name: 'country_code',
        fieldType: goog.proto2.Message.FieldType.INT32,
        type: Number
      },
      11: {
        name: 'international_prefix',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      17: {
        name: 'preferred_international_prefix',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      12: {
        name: 'national_prefix',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      13: {
        name: 'preferred_extn_prefix',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      15: {
        name: 'national_prefix_for_parsing',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      16: {
        name: 'national_prefix_transform_rule',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      18: {
        name: 'same_mobile_and_fixed_line_pattern',
        fieldType: goog.proto2.Message.FieldType.BOOL,
        defaultValue: false,
        type: Boolean
      },
      19: {
        name: 'number_format',
        repeated: true,
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.NumberFormat
      },
      20: {
        name: 'intl_number_format',
        repeated: true,
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.NumberFormat
      },
      22: {
        name: 'main_country_for_code',
        fieldType: goog.proto2.Message.FieldType.BOOL,
        defaultValue: false,
        type: Boolean
      },
      23: {
        name: 'leading_digits',
        fieldType: goog.proto2.Message.FieldType.STRING,
        type: String
      },
      26: {
        name: 'leading_zero_possible',
        fieldType: goog.proto2.Message.FieldType.BOOL,
        defaultValue: false,
        type: Boolean
      }
    };
    i18n.phonenumbers.PhoneMetadata.descriptor_ = descriptor =
        goog.proto2.Message.createDescriptor(
             i18n.phonenumbers.PhoneMetadata, descriptorObj);
  }
  return descriptor;
};


/** @nocollapse */
i18n.phonenumbers.PhoneMetadata.getDescriptor =
    i18n.phonenumbers.PhoneMetadata.prototype.getDescriptor;


/** @override */
i18n.phonenumbers.PhoneMetadataCollection.prototype.getDescriptor = function() {
  var descriptor = i18n.phonenumbers.PhoneMetadataCollection.descriptor_;
  if (!descriptor) {
    // The descriptor is created lazily when we instantiate a new instance.
    var descriptorObj = {
      0: {
        name: 'PhoneMetadataCollection',
        fullName: 'i18n.phonenumbers.PhoneMetadataCollection'
      },
      1: {
        name: 'metadata',
        repeated: true,
        fieldType: goog.proto2.Message.FieldType.MESSAGE,
        type: i18n.phonenumbers.PhoneMetadata
      }
    };
    i18n.phonenumbers.PhoneMetadataCollection.descriptor_ = descriptor =
        goog.proto2.Message.createDescriptor(
             i18n.phonenumbers.PhoneMetadataCollection, descriptorObj);
  }
  return descriptor;
};


/** @nocollapse */
i18n.phonenumbers.PhoneMetadataCollection.getDescriptor =
    i18n.phonenumbers.PhoneMetadataCollection.prototype.getDescriptor;
