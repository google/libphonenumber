/**
 * @license
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
 * @fileoverview Generated metadata for file
 * googledata/third_party/i18n/phonenumbers/PhoneNumberMetadata.xml
 * @author Nikolaos Trogkanis
 */

goog.provide('i18n.phonenumbers.metadata');

/**
 * A mapping from a country calling code to the region codes which denote the
 * region represented by that country calling code. In the case of multiple
 * countries sharing a calling code, such as the NANPA regions, the one
 * indicated with "isMainCountryForCode" in the metadata should be first.
 * @type {!Object.<number, Array.<string>>}
 */
i18n.phonenumbers.metadata.countryCodeToRegionCodeMap = {
1:["GU"]
,54:["AR"]
,247:["AC"]
,979:["001"]
};

/**
 * A mapping from a region code to the PhoneMetadata for that region.
 * @type {!Object.<string, Array>}
 */
i18n.phonenumbers.metadata.countryToMetadata = {
"AC":[,[,,"[46]\\d{4}|[01589]\\d{5}",,,,,,,[5,6]
]
,[,,"6[2-467]\\d{3}",,,,"62889",,,[5]
]
,[,,"4\\d{4}",,,,"40123",,,[5]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,"AC",247,"00",,,,,,,,,,[,,"NA",,,,,,,[-1]
]
,,,[,,"NA",,,,,,,[-1]
]
,[,,"[01589]\\d{5}",,,,"542011",,,[6]
]
,,,[,,"NA",,,,,,,[-1]
]
]
,"AR":[,[,,"11\\d{8}|[2368]\\d{9}|9\\d{10}",,,,,,,[10]
,[6,7,8]
]
,[,,"11\\d{8}",,,,"1123456789",,,,[6,7,8]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,"AR",54,"00","0",,,"0?(?:(11)?15)?","9$1",,,[[,"([68]\\d{2})(\\d{3})(\\d{4})","$1-$2-$3",["[68]"]
,"0$1"]
,[,"(\\d{2})(\\d{4})","$1-$2",["[2-9]"]
,"$1"]
,[,"(9)(11)(\\d{4})(\\d{4})","$2 15-$3-$4",["911"]
,"0$1"]
,[,"(11)(\\d{4})(\\d{4})","$1 $2-$3",["1"]
,"0$1",,1]
]
,[[,"([68]\\d{2})(\\d{3})(\\d{4})","$1-$2-$3",["[68]"]
,"0$1"]
,[,"(9)(11)(\\d{4})(\\d{4})","$1 $2 $3-$4",["911"]
]
,[,"(11)(\\d{4})(\\d{4})","$1 $2-$3",["1"]
,"0$1",,1]
]
,[,,"NA",,,,,,,[-1]
]
,,,[,,"810\\d{7}",,,,"8101234567"]
,[,,"810\\d{7}",,,,"8101234567"]
,,,[,,"NA",,,,,,,[-1]
]
]
,"GU":[,[,,"[5689]\\d{9}",,,,,,,[10]
,[7]
]
,[,,"671(?:3(?:00|3[39]|4[349]|55|6[26])|4(?:56|7[1-9]|8[236-9]))\\d{4}",,,,"6713001234",,,,[7]
]
,[,,"671(?:3(?:00|3[39]|4[349]|55|6[26])|4(?:56|7[1-9]|8[236-9]))\\d{4}",,,,"6713001234",,,,[7]
]
,[,,"8(?:00|33|44|55|66|77|88)[2-9]\\d{6}",,,,"8002123456"]
,[,,"900[2-9]\\d{6}",,,,"9002123456"]
,[,,"NA",,,,,,,[-1]
]
,[,,"5(?:00|22|33|44|66|77|88)[2-9]\\d{6}",,,,"5002345678"]
,[,,"NA",,,,,,,[-1]
]
,"GU",1,"011","1",,,"1",,,1,,,[,,"NA",,,,,,,[-1]
]
,,"671",[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,,,[,,"NA",,,,,,,[-1]
]
]
,"979":[,[,,"\\d{9}",,,,,,,[9]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"\\d{9}",,,,"123456789"]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,"001",979,,,,,,,,1,[[,"(\\d)(\\d{4})(\\d{4})","$1 $2 $3"]
]
,,[,,"NA",,,,,,,[-1]
]
,,,[,,"NA",,,,,,,[-1]
]
,[,,"NA",,,,,,,[-1]
]
,1,,[,,"NA",,,,,,,[-1]
]
]
};
