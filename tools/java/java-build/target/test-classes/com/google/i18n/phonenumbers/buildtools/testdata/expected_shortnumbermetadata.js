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
 * googledata/third_party/i18n/phonenumbers/ShortNumberMetadata.xml
 * @author Nikolaos Trogkanis
 */

goog.provide('i18n.phonenumbers.shortnumbergoldenmetadata');

/**
 * A mapping from a country calling code to the region codes which denote the
 * region represented by that country calling code. In the case of multiple
 * countries sharing a calling code, such as the NANPA regions, the one
 * indicated with "isMainCountryForCode" in the metadata should be first.
 * @type {!Object.<number, Array.<string>>}
 */
i18n.phonenumbers.shortnumbergoldenmetadata.countryCodeToRegionCodeMap = {
0:["AC","AR","GU"]
};

/**
 * A mapping from a region code to the PhoneMetadata for that region.
 * @type {!Object.<string, Array>}
 */
i18n.phonenumbers.shortnumbergoldenmetadata.countryToMetadata = {
"AC":[,[,,"9\\d{2}",,,,,,,[3]
]
,,,[,,,,,,,,,[-1]
]
,[,,,,,,,,,[-1]
]
,,,,"AC",,,,,,,,,,,,,,,,,,[,,"911",,,,"911"]
,,[,,"911",,,,"911"]
,[,,,,,,,,,[-1]
]
,[,,,,,,,,,[-1]
]
,,[,,,,,,,,,[-1]
]
]
,"AR":[,[,,"[01389]\\d{1,4}",,,,,,,[2,3,4,5]
]
,,,[,,"[09]\\d{2}|1(?:[02-9]\\d?|1[0-24-9]?)",,,,"111",,,[2,3]
]
,[,,,,,,,,,[-1]
]
,,,,"AR",,,,,,,,,,,,,,,,,,[,,"10[017]|911",,,,"101",,,[3]
]
,,[,,"000|1(?:0[0-35-7]|1[02-5]|2[15]|9)|3372|89338|911",,,,"121"]
,[,,,,,,,,,[-1]
]
,[,,"89338|911",,,,"89338",,,[3,5]
]
,,[,,"3372|89338",,,,"3372",,,[4,5]
]
]
,"GU":[,[,,"9\\d{2}",,,,,,,[3]
]
,,,[,,,,,,,,,[-1]
]
,[,,,,,,,,,[-1]
]
,,,,"GU",,,,,,,,,,,,,,,,,,[,,"911",,,,"911"]
,,[,,"911",,,,"911"]
,[,,,,,,,,,[-1]
]
,[,,,,,,,,,[-1]
]
,,[,,,,,,,,,[-1]
]
]
};
