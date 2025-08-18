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

goog.provide('i18n.phonenumbers.PhoneNumberMatcher');

goog.require('goog.string.StringBuffer');
goog.require('i18n.phonenumbers.PhoneNumberUtil');
goog.require('i18n.phonenumbers.PhoneNumberMatch');

/** The potential states of a PhoneNumberMatcher. */
var State = {
    NOT_READY: -1,
    READY: 0,
    DONE: 1
};

/**
 * Matches strings that look like publication pages. Example:
 * <pre>Computing Complete Answers to Queries in the Presence of Limited
 * Access Patterns. Chen Li. VLDB J. 12(3): 211-227 (2003).</pre>
 *
 * The string "211-227 (2003)" is not a telephone number.
 */
var PUB_PAGES = /\d{1,5}-+\d{1,5}\s{0,4}\(\d{1,4}/;

/**
 * Matches strings that look like dates using "/" as a separator. Examples: 3/10/2011, 31/10/96 or
 * 08/31/95.
 */
var SLASH_SEPARATED_DATES = /(?:(?:[0-3]?\d\/[01]?\d)|(?:[01]?\d\/[0-3]?\d))\/(?:[12]\d)?\d{2}/;

/**
 * Matches timestamps. Examples: "2012-01-02 08:00". Note that the reg-ex does not include the
 * trailing ":\d\d" -- that is covered by TIME_STAMPS_SUFFIX.
 */
var TIME_STAMPS = /[12]\d{3}[-/]?[01]\d[-/]?[0-3]\d +[0-2]\d$/;
var TIME_STAMPS_SUFFIX = /:[0-5]\d/;

/**
 * Non-Spaceing Mark (Mn Unicode Category generated via https://apps.timwhitlock.info/js/regex#)
 */
var NON_SPACING_MARK = /[\u0300-\u036f\u0483-\u0487\u0591-\u05bd\u05bf\u05c1-\u05c2\u05c4-\u05c5\u05c7\u0610-\u061a\u064b-\u065e\u0670\u06d6-\u06dc\u06df-\u06e4\u06e7-\u06e8\u06ea-\u06ed\u0711\u0730-\u074a\u07a6-\u07b0\u07eb-\u07f3\u0901-\u0902\u093c\u0941-\u0948\u094d\u0951-\u0954\u0962-\u0963\u0981\u09bc\u09c1-\u09c4\u09cd\u09e2-\u09e3\u0a01-\u0a02\u0a3c\u0a41-\u0a42\u0a47-\u0a48\u0a4b-\u0a4d\u0a51\u0a70-\u0a71\u0a75\u0a81-\u0a82\u0abc\u0ac1-\u0ac5\u0ac7-\u0ac8\u0acd\u0ae2-\u0ae3\u0b01\u0b3c\u0b3f\u0b41-\u0b44\u0b4d\u0b56\u0b62-\u0b63\u0b82\u0bc0\u0bcd\u0c3e-\u0c40\u0c46-\u0c48\u0c4a-\u0c4d\u0c55-\u0c56\u0c62-\u0c63\u0cbc\u0cbf\u0cc6\u0ccc-\u0ccd\u0ce2-\u0ce3\u0d41-\u0d44\u0d4d\u0d62-\u0d63\u0dca\u0dd2-\u0dd4\u0dd6\u0e31\u0e34-\u0e3a\u0e47-\u0e4e\u0eb1\u0eb4-\u0eb9\u0ebb-\u0ebc\u0ec8-\u0ecd\u0f18-\u0f19\u0f35\u0f37\u0f39\u0f71-\u0f7e\u0f80-\u0f84\u0f86-\u0f87\u0f90-\u0f97\u0f99-\u0fbc\u0fc6\u102d-\u1030\u1032-\u1037\u1039-\u103a\u103d-\u103e\u1058-\u1059\u105e-\u1060\u1071-\u1074\u1082\u1085-\u1086\u108d\u135f\u1712-\u1714\u1732-\u1734\u1752-\u1753\u1772-\u1773\u17b7-\u17bd\u17c6\u17c9-\u17d3\u17dd\u180b-\u180d\u18a9\u1920-\u1922\u1927-\u1928\u1932\u1939-\u193b\u1a17-\u1a18\u1b00-\u1b03\u1b34\u1b36-\u1b3a\u1b3c\u1b42\u1b6b-\u1b73\u1b80-\u1b81\u1ba2-\u1ba5\u1ba8-\u1ba9\u1c2c-\u1c33\u1c36-\u1c37\u1dc0-\u1de6\u1dfe-\u1dff\u20d0-\u20dc\u20e1\u20e5-\u20f0\u2de0-\u2dff\u302a-\u302f\u3099-\u309a\ua66f\ua67c-\ua67d\ua802\ua806\ua80b\ua825-\ua826\ua8c4\ua926-\ua92d\ua947-\ua951\uaa29-\uaa2e\uaa31-\uaa32\uaa35-\uaa36\uaa43\uaa4c\ufb1e\ufe00-\ufe0f\ufe20-\ufe26]|\ud800\uddfd|\ud802[\ude01-\ude03\ude05-\ude06\ude0c-\ude0f\ude38-\ude3a\ude3f]|\ud834[\udd67-\udd69\udd7b-\udd82\udd85-\udd8b\uddaa-\uddad\ude42-\ude44]|\udb40[\udd00-\uddef]/;

/**
 * Currency Symbol (Sc Unicode Category generated via https://mothereff.in/regexpu with `/\p{Sc}/u`)
 */
var CURRENCY_SYMBOL = /[$\xA2-\xA5\u058F\u060B\u09F2\u09F3\u09FB\u0AF1\u0BF9\u0E3F\u17DB\u20A0-\u20BF\uA838\uFDFC\uFE69\uFF04\uFFE0\uFFE1\uFFE5\uFFE6]/;

/**
 * Is Letter - https://docs.oracle.com/javase/7/docs/api/java/lang/Character.html#isLetter(char)
 * 
 *   UPPERCASE_LETTER (Lu)
 *   LOWERCASE_LETTER (Ll)
 *   TITLECASE_LETTER (Lt)
 *   MODIFIER_LETTER  (Lm)
 *   OTHER_LETTER     (Lo)
 * 
 * Regex generated via https://mothereff.in/regexpu with `/\p{L}/u`
 */
var IS_LETTER = /(?:[A-Za-z\xAA\xB5\xBA\xC0-\xD6\xD8-\xF6\xF8-\u02C1\u02C6-\u02D1\u02E0-\u02E4\u02EC\u02EE\u0370-\u0374\u0376\u0377\u037A-\u037D\u037F\u0386\u0388-\u038A\u038C\u038E-\u03A1\u03A3-\u03F5\u03F7-\u0481\u048A-\u052F\u0531-\u0556\u0559\u0561-\u0587\u05D0-\u05EA\u05F0-\u05F2\u0620-\u064A\u066E\u066F\u0671-\u06D3\u06D5\u06E5\u06E6\u06EE\u06EF\u06FA-\u06FC\u06FF\u0710\u0712-\u072F\u074D-\u07A5\u07B1\u07CA-\u07EA\u07F4\u07F5\u07FA\u0800-\u0815\u081A\u0824\u0828\u0840-\u0858\u0860-\u086A\u08A0-\u08B4\u08B6-\u08BD\u0904-\u0939\u093D\u0950\u0958-\u0961\u0971-\u0980\u0985-\u098C\u098F\u0990\u0993-\u09A8\u09AA-\u09B0\u09B2\u09B6-\u09B9\u09BD\u09CE\u09DC\u09DD\u09DF-\u09E1\u09F0\u09F1\u09FC\u0A05-\u0A0A\u0A0F\u0A10\u0A13-\u0A28\u0A2A-\u0A30\u0A32\u0A33\u0A35\u0A36\u0A38\u0A39\u0A59-\u0A5C\u0A5E\u0A72-\u0A74\u0A85-\u0A8D\u0A8F-\u0A91\u0A93-\u0AA8\u0AAA-\u0AB0\u0AB2\u0AB3\u0AB5-\u0AB9\u0ABD\u0AD0\u0AE0\u0AE1\u0AF9\u0B05-\u0B0C\u0B0F\u0B10\u0B13-\u0B28\u0B2A-\u0B30\u0B32\u0B33\u0B35-\u0B39\u0B3D\u0B5C\u0B5D\u0B5F-\u0B61\u0B71\u0B83\u0B85-\u0B8A\u0B8E-\u0B90\u0B92-\u0B95\u0B99\u0B9A\u0B9C\u0B9E\u0B9F\u0BA3\u0BA4\u0BA8-\u0BAA\u0BAE-\u0BB9\u0BD0\u0C05-\u0C0C\u0C0E-\u0C10\u0C12-\u0C28\u0C2A-\u0C39\u0C3D\u0C58-\u0C5A\u0C60\u0C61\u0C80\u0C85-\u0C8C\u0C8E-\u0C90\u0C92-\u0CA8\u0CAA-\u0CB3\u0CB5-\u0CB9\u0CBD\u0CDE\u0CE0\u0CE1\u0CF1\u0CF2\u0D05-\u0D0C\u0D0E-\u0D10\u0D12-\u0D3A\u0D3D\u0D4E\u0D54-\u0D56\u0D5F-\u0D61\u0D7A-\u0D7F\u0D85-\u0D96\u0D9A-\u0DB1\u0DB3-\u0DBB\u0DBD\u0DC0-\u0DC6\u0E01-\u0E30\u0E32\u0E33\u0E40-\u0E46\u0E81\u0E82\u0E84\u0E87\u0E88\u0E8A\u0E8D\u0E94-\u0E97\u0E99-\u0E9F\u0EA1-\u0EA3\u0EA5\u0EA7\u0EAA\u0EAB\u0EAD-\u0EB0\u0EB2\u0EB3\u0EBD\u0EC0-\u0EC4\u0EC6\u0EDC-\u0EDF\u0F00\u0F40-\u0F47\u0F49-\u0F6C\u0F88-\u0F8C\u1000-\u102A\u103F\u1050-\u1055\u105A-\u105D\u1061\u1065\u1066\u106E-\u1070\u1075-\u1081\u108E\u10A0-\u10C5\u10C7\u10CD\u10D0-\u10FA\u10FC-\u1248\u124A-\u124D\u1250-\u1256\u1258\u125A-\u125D\u1260-\u1288\u128A-\u128D\u1290-\u12B0\u12B2-\u12B5\u12B8-\u12BE\u12C0\u12C2-\u12C5\u12C8-\u12D6\u12D8-\u1310\u1312-\u1315\u1318-\u135A\u1380-\u138F\u13A0-\u13F5\u13F8-\u13FD\u1401-\u166C\u166F-\u167F\u1681-\u169A\u16A0-\u16EA\u16F1-\u16F8\u1700-\u170C\u170E-\u1711\u1720-\u1731\u1740-\u1751\u1760-\u176C\u176E-\u1770\u1780-\u17B3\u17D7\u17DC\u1820-\u1877\u1880-\u1884\u1887-\u18A8\u18AA\u18B0-\u18F5\u1900-\u191E\u1950-\u196D\u1970-\u1974\u1980-\u19AB\u19B0-\u19C9\u1A00-\u1A16\u1A20-\u1A54\u1AA7\u1B05-\u1B33\u1B45-\u1B4B\u1B83-\u1BA0\u1BAE\u1BAF\u1BBA-\u1BE5\u1C00-\u1C23\u1C4D-\u1C4F\u1C5A-\u1C7D\u1C80-\u1C88\u1CE9-\u1CEC\u1CEE-\u1CF1\u1CF5\u1CF6\u1D00-\u1DBF\u1E00-\u1F15\u1F18-\u1F1D\u1F20-\u1F45\u1F48-\u1F4D\u1F50-\u1F57\u1F59\u1F5B\u1F5D\u1F5F-\u1F7D\u1F80-\u1FB4\u1FB6-\u1FBC\u1FBE\u1FC2-\u1FC4\u1FC6-\u1FCC\u1FD0-\u1FD3\u1FD6-\u1FDB\u1FE0-\u1FEC\u1FF2-\u1FF4\u1FF6-\u1FFC\u2071\u207F\u2090-\u209C\u2102\u2107\u210A-\u2113\u2115\u2119-\u211D\u2124\u2126\u2128\u212A-\u212D\u212F-\u2139\u213C-\u213F\u2145-\u2149\u214E\u2183\u2184\u2C00-\u2C2E\u2C30-\u2C5E\u2C60-\u2CE4\u2CEB-\u2CEE\u2CF2\u2CF3\u2D00-\u2D25\u2D27\u2D2D\u2D30-\u2D67\u2D6F\u2D80-\u2D96\u2DA0-\u2DA6\u2DA8-\u2DAE\u2DB0-\u2DB6\u2DB8-\u2DBE\u2DC0-\u2DC6\u2DC8-\u2DCE\u2DD0-\u2DD6\u2DD8-\u2DDE\u2E2F\u3005\u3006\u3031-\u3035\u303B\u303C\u3041-\u3096\u309D-\u309F\u30A1-\u30FA\u30FC-\u30FF\u3105-\u312E\u3131-\u318E\u31A0-\u31BA\u31F0-\u31FF\u3400-\u4DB5\u4E00-\u9FEA\uA000-\uA48C\uA4D0-\uA4FD\uA500-\uA60C\uA610-\uA61F\uA62A\uA62B\uA640-\uA66E\uA67F-\uA69D\uA6A0-\uA6E5\uA717-\uA71F\uA722-\uA788\uA78B-\uA7AE\uA7B0-\uA7B7\uA7F7-\uA801\uA803-\uA805\uA807-\uA80A\uA80C-\uA822\uA840-\uA873\uA882-\uA8B3\uA8F2-\uA8F7\uA8FB\uA8FD\uA90A-\uA925\uA930-\uA946\uA960-\uA97C\uA984-\uA9B2\uA9CF\uA9E0-\uA9E4\uA9E6-\uA9EF\uA9FA-\uA9FE\uAA00-\uAA28\uAA40-\uAA42\uAA44-\uAA4B\uAA60-\uAA76\uAA7A\uAA7E-\uAAAF\uAAB1\uAAB5\uAAB6\uAAB9-\uAABD\uAAC0\uAAC2\uAADB-\uAADD\uAAE0-\uAAEA\uAAF2-\uAAF4\uAB01-\uAB06\uAB09-\uAB0E\uAB11-\uAB16\uAB20-\uAB26\uAB28-\uAB2E\uAB30-\uAB5A\uAB5C-\uAB65\uAB70-\uABE2\uAC00-\uD7A3\uD7B0-\uD7C6\uD7CB-\uD7FB\uF900-\uFA6D\uFA70-\uFAD9\uFB00-\uFB06\uFB13-\uFB17\uFB1D\uFB1F-\uFB28\uFB2A-\uFB36\uFB38-\uFB3C\uFB3E\uFB40\uFB41\uFB43\uFB44\uFB46-\uFBB1\uFBD3-\uFD3D\uFD50-\uFD8F\uFD92-\uFDC7\uFDF0-\uFDFB\uFE70-\uFE74\uFE76-\uFEFC\uFF21-\uFF3A\uFF41-\uFF5A\uFF66-\uFFBE\uFFC2-\uFFC7\uFFCA-\uFFCF\uFFD2-\uFFD7\uFFDA-\uFFDC]|\uD800[\uDC00-\uDC0B\uDC0D-\uDC26\uDC28-\uDC3A\uDC3C\uDC3D\uDC3F-\uDC4D\uDC50-\uDC5D\uDC80-\uDCFA\uDE80-\uDE9C\uDEA0-\uDED0\uDF00-\uDF1F\uDF2D-\uDF40\uDF42-\uDF49\uDF50-\uDF75\uDF80-\uDF9D\uDFA0-\uDFC3\uDFC8-\uDFCF]|\uD801[\uDC00-\uDC9D\uDCB0-\uDCD3\uDCD8-\uDCFB\uDD00-\uDD27\uDD30-\uDD63\uDE00-\uDF36\uDF40-\uDF55\uDF60-\uDF67]|\uD802[\uDC00-\uDC05\uDC08\uDC0A-\uDC35\uDC37\uDC38\uDC3C\uDC3F-\uDC55\uDC60-\uDC76\uDC80-\uDC9E\uDCE0-\uDCF2\uDCF4\uDCF5\uDD00-\uDD15\uDD20-\uDD39\uDD80-\uDDB7\uDDBE\uDDBF\uDE00\uDE10-\uDE13\uDE15-\uDE17\uDE19-\uDE33\uDE60-\uDE7C\uDE80-\uDE9C\uDEC0-\uDEC7\uDEC9-\uDEE4\uDF00-\uDF35\uDF40-\uDF55\uDF60-\uDF72\uDF80-\uDF91]|\uD803[\uDC00-\uDC48\uDC80-\uDCB2\uDCC0-\uDCF2]|\uD804[\uDC03-\uDC37\uDC83-\uDCAF\uDCD0-\uDCE8\uDD03-\uDD26\uDD50-\uDD72\uDD76\uDD83-\uDDB2\uDDC1-\uDDC4\uDDDA\uDDDC\uDE00-\uDE11\uDE13-\uDE2B\uDE80-\uDE86\uDE88\uDE8A-\uDE8D\uDE8F-\uDE9D\uDE9F-\uDEA8\uDEB0-\uDEDE\uDF05-\uDF0C\uDF0F\uDF10\uDF13-\uDF28\uDF2A-\uDF30\uDF32\uDF33\uDF35-\uDF39\uDF3D\uDF50\uDF5D-\uDF61]|\uD805[\uDC00-\uDC34\uDC47-\uDC4A\uDC80-\uDCAF\uDCC4\uDCC5\uDCC7\uDD80-\uDDAE\uDDD8-\uDDDB\uDE00-\uDE2F\uDE44\uDE80-\uDEAA\uDF00-\uDF19]|\uD806[\uDCA0-\uDCDF\uDCFF\uDE00\uDE0B-\uDE32\uDE3A\uDE50\uDE5C-\uDE83\uDE86-\uDE89\uDEC0-\uDEF8]|\uD807[\uDC00-\uDC08\uDC0A-\uDC2E\uDC40\uDC72-\uDC8F\uDD00-\uDD06\uDD08\uDD09\uDD0B-\uDD30\uDD46]|\uD808[\uDC00-\uDF99]|\uD809[\uDC80-\uDD43]|[\uD80C\uD81C-\uD820\uD840-\uD868\uD86A-\uD86C\uD86F-\uD872\uD874-\uD879][\uDC00-\uDFFF]|\uD80D[\uDC00-\uDC2E]|\uD811[\uDC00-\uDE46]|\uD81A[\uDC00-\uDE38\uDE40-\uDE5E\uDED0-\uDEED\uDF00-\uDF2F\uDF40-\uDF43\uDF63-\uDF77\uDF7D-\uDF8F]|\uD81B[\uDF00-\uDF44\uDF50\uDF93-\uDF9F\uDFE0\uDFE1]|\uD821[\uDC00-\uDFEC]|\uD822[\uDC00-\uDEF2]|\uD82C[\uDC00-\uDD1E\uDD70-\uDEFB]|\uD82F[\uDC00-\uDC6A\uDC70-\uDC7C\uDC80-\uDC88\uDC90-\uDC99]|\uD835[\uDC00-\uDC54\uDC56-\uDC9C\uDC9E\uDC9F\uDCA2\uDCA5\uDCA6\uDCA9-\uDCAC\uDCAE-\uDCB9\uDCBB\uDCBD-\uDCC3\uDCC5-\uDD05\uDD07-\uDD0A\uDD0D-\uDD14\uDD16-\uDD1C\uDD1E-\uDD39\uDD3B-\uDD3E\uDD40-\uDD44\uDD46\uDD4A-\uDD50\uDD52-\uDEA5\uDEA8-\uDEC0\uDEC2-\uDEDA\uDEDC-\uDEFA\uDEFC-\uDF14\uDF16-\uDF34\uDF36-\uDF4E\uDF50-\uDF6E\uDF70-\uDF88\uDF8A-\uDFA8\uDFAA-\uDFC2\uDFC4-\uDFCB]|\uD83A[\uDC00-\uDCC4\uDD00-\uDD43]|\uD83B[\uDE00-\uDE03\uDE05-\uDE1F\uDE21\uDE22\uDE24\uDE27\uDE29-\uDE32\uDE34-\uDE37\uDE39\uDE3B\uDE42\uDE47\uDE49\uDE4B\uDE4D-\uDE4F\uDE51\uDE52\uDE54\uDE57\uDE59\uDE5B\uDE5D\uDE5F\uDE61\uDE62\uDE64\uDE67-\uDE6A\uDE6C-\uDE72\uDE74-\uDE77\uDE79-\uDE7C\uDE7E\uDE80-\uDE89\uDE8B-\uDE9B\uDEA1-\uDEA3\uDEA5-\uDEA9\uDEAB-\uDEBB]|\uD869[\uDC00-\uDED6\uDF00-\uDFFF]|\uD86D[\uDC00-\uDF34\uDF40-\uDFFF]|\uD86E[\uDC00-\uDC1D\uDC20-\uDFFF]|\uD873[\uDC00-\uDEA1\uDEB0-\uDFFF]|\uD87A[\uDC00-\uDFE0]|\uD87E[\uDC00-\uDE1D])/;

/**
 * Is Latin:
 * 
 *      UnicodeBlock.BASIC_LATIN
 *      UnicodeBlock.LATIN_1_SUPPLEMENT
 *      UnicodeBlock.LATIN_EXTENDED_A
 *      UnicodeBlock.LATIN_EXTENDED_ADDITIONAL
 *      UnicodeBlock.LATIN_EXTENDED_B
 *      UnicodeBlock.COMBINING_DIACRITICAL_MARKS
 *
 * JS equiv of Unicode categories for the above via https://apps.timwhitlock.info/js/regex#
 */
var IS_LATIN = /[\u0000-~\u0080-þĀ-žƀ-Ɏ\u0300-\u036eḀ-Ỿ]/;

/**
 * Patterns used to extract phone numbers from a larger phone-number-like pattern. These are
 * ordered according to specificity. For example, white-space is last since that is frequently
 * used in numbers, not just to separate two numbers. We have separate patterns since we don't
 * want to break up the phone-number-like text on more than one different kind of symbol at one
 * time, although symbols of the same type (e.g. space) can be safely grouped together.
 *
 * Note that if there is a match, we will always check any text found up to the first match as
 * well.
 */
var INNER_MATCHES = [
    // Breaks on the slash - e.g. "651-234-2345/332-445-1234"
    '\\/+(.*)',
    // Note that the bracket here is inside the capturing group, since we consider it part of the
    // phone number. Will match a pattern like "(650) 223 3345 (754) 223 3321".
    '(\\([^(]*)',
    // Breaks on a hyphen - e.g. "12345 - 332-445-1234 is my number."
    // We require a space on either side of the hyphen for it to be considered a separator.
    // Java uses /(?:\p{Z}-|-\p{Z})\p{Z}*(.+)/, and this regex is es5 compatible
    '(?:[ \\xA0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]\\-|\\-[ \\xA0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000])[ \\xA0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]*((?:[\\0-\\t\\x0B\\f\\x0E-\\u2027\\u202A-\\uD7FF\\uE000-\\uFFFF]|[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]|[\\uD800-\\uDBFF](?![\\uDC00-\\uDFFF])|(?:[^\\uD800-\\uDBFF]|^)[\\uDC00-\\uDFFF])+)',
    // Various types of wide hyphens. Note we have decided not to enforce a space here, since it's
    // possible that it's supposed to be used to break two numbers without spaces, and we haven't
    // seen many instances of it used within a number.
    // Java uses /[\u2012-\u2015\uFF0D]\p{Z}*(.+)/, and this regex is es5 compatible
    '[\\u2012-\\u2015\\uFF0D][ \\xA0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]*((?:[\\0-\\t\\x0B\\f\\x0E-\\u2027\\u202A-\\uD7FF\\uE000-\\uFFFF]|[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]|[\\uD800-\\uDBFF](?![\\uDC00-\\uDFFF])|(?:[^\\uD800-\\uDBFF]|^)[\\uDC00-\\uDFFF])+)',
    // Breaks on a full stop - e.g. "12345. 332-445-1234 is my number."
    // Java uses /\.+\p{Z}*([^.]+)/, and this regex is es5 compatible
    '\\.+[ \\xA0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]*((?:[\\0-\\-\\/-\\uD7FF\\uE000-\\uFFFF]|[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]|[\\uD800-\\uDBFF](?![\\uDC00-\\uDFFF])|(?:[^\\uD800-\\uDBFF]|^)[\\uDC00-\\uDFFF])+)',
    // Breaks on space - e.g. "3324451234 8002341234"
    // Java uses /\p{Z}+(\P{Z}+)/ and this regex is es5 compatible
    '[ \\xA0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]+((?:[\\0-\\x1F!-\\x9F\\xA1-\\u167F\\u1681-\\u1FFF\\u200B-\\u2027\\u202A-\\u202E\\u2030-\\u205E\\u2060-\\u2FFF\\u3001-\\uD7FF\\uE000-\\uFFFF]|[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]|[\\uD800-\\uDBFF](?![\\uDC00-\\uDFFF])|(?:[^\\uD800-\\uDBFF]|^)[\\uDC00-\\uDFFF])+)'
];

/**
 * The phone number pattern used by {@link #find}, similar to
 * {@code PhoneNumberUtil.VALID_PHONE_NUMBER}, but with the following differences:
 * <ul>
 *   <li>All captures are limited in order to place an upper bound to the text matched by the
 *       pattern.
 * <ul>
 *   <li>Leading punctuation / plus signs are limited.
 *   <li>Consecutive occurrences of punctuation are limited.
 *   <li>Number of digits is limited.
 * </ul>
 *   <li>No whitespace is allowed at the start or end.
 *   <li>No alpha digits (vanity numbers such as 1-800-SIX-FLAGS) are currently supported.
 * </ul>
 */
var PATTERN; // built dynamically below

/**
 * Pattern to check that brackets match. Opening brackets should be closed within a phone number.
 * This also checks that there is something inside the brackets. Having no brackets at all is also
 * fine.
 */
var MATCHING_BRACKETS; // built dynamically below

/**
 * Punctuation that may be at the start of a phone number - brackets and plus signs.
 */
var LEAD_CLASS; // built dynamically below

(function () {

  /** Returns a regular expression quantifier with an upper and lower limit. */
  function limit(lower, upper) {
      if ((lower < 0) || (upper <= 0) || (upper < lower)) {
          throw new Error('invalid lower or upper limit');
      }
      return '{' + lower + ',' + upper + '}';
  }

  /* Builds the MATCHING_BRACKETS and PATTERN regular expressions. The building blocks below exist
   * to make the pattern more easily understood. */

  var openingParens = '(\\[\uFF08\uFF3B';
  var closingParens = ')\\]\uFF09\uFF3D';
  var nonParens = '[^' + openingParens + closingParens + ']';

  /* Limit on the number of pairs of brackets in a phone number. */
  var bracketPairLimit = limit(0, 3);
  /*
    * An opening bracket at the beginning may not be closed, but subsequent ones should be.  It's
    * also possible that the leading bracket was dropped, so we shouldn't be surprised if we see a
    * closing bracket first. We limit the sets of brackets in a phone number to four.
    */
  MATCHING_BRACKETS = new RegExp(
      '(?:[' + openingParens + '])?' + '(?:' + nonParens + '+' +
      '[' + closingParens + '])?' + nonParens + '+'
      + '(?:[' + openingParens + ']' + nonParens + '+[' +
      closingParens + '])' + bracketPairLimit + nonParens + '*');

  /* Limit on the number of leading (plus) characters. */
  var leadLimit = limit(0, 2);
  /* Limit on the number of consecutive punctuation characters. */
  var punctuationLimit = limit(0, 4);
  /* The maximum number of digits allowed in a digit-separated block. As we allow all digits in a
    * single block, set high enough to accommodate the entire national number and the international
    * country code. */
  var digitBlockLimit = i18n.phonenumbers.PhoneNumberUtil.MAX_LENGTH_FOR_NSN_ +
    i18n.phonenumbers.PhoneNumberUtil.MAX_LENGTH_COUNTRY_CODE_;
  /* Limit on the number of blocks separated by punctuation. Uses digitBlockLimit since some
    * formats use spaces to separate each digit. */
  var blockLimit = limit(0, digitBlockLimit);

  /* A punctuation sequence allowing white space. */
  var punctuation = '[' + i18n.phonenumbers.PhoneNumberUtil.VALID_PUNCTUATION +
    ']' + punctuationLimit;
  /* A digits block without punctuation. */
  // XXX: can't use \p{Nd} in es5, so here's a transpiled version via https://mothereff.in/regexpu
  var es5DigitSequence = '(?:[0-9\\u0660-\\u0669\\u06F0-\\u06F9\\u07C0-\\u07C9\\u0966-\\u096F\\u09E6-\\u09EF\\u0A66-\\u0A6F\\u0AE6-\\u0AEF\\u0B66-\\u0B6F\\u0BE6-\\u0BEF\\u0C66-\\u0C6F\\u0CE6-\\u0CEF\\u0D66-\\u0D6F\\u0DE6-\\u0DEF\\u0E50-\\u0E59\\u0ED0-\\u0ED9\\u0F20-\\u0F29\\u1040-\\u1049\\u1090-\\u1099\\u17E0-\\u17E9\\u1810-\\u1819\\u1946-\\u194F\\u19D0-\\u19D9\\u1A80-\\u1A89\\u1A90-\\u1A99\\u1B50-\\u1B59\\u1BB0-\\u1BB9\\u1C40-\\u1C49\\u1C50-\\u1C59\\uA620-\\uA629\\uA8D0-\\uA8D9\\uA900-\\uA909\\uA9D0-\\uA9D9\\uA9F0-\\uA9F9\\uAA50-\\uAA59\\uABF0-\\uABF9\\uFF10-\\uFF19]|\\uD801[\\uDCA0-\\uDCA9]|\\uD804[\\uDC66-\\uDC6F\\uDCF0-\\uDCF9\\uDD36-\\uDD3F\\uDDD0-\\uDDD9\\uDEF0-\\uDEF9]|[\\uD805\\uD807][\\uDC50-\\uDC59\\uDCD0-\\uDCD9\\uDE50-\\uDE59\\uDEC0-\\uDEC9\\uDF30-\\uDF39]|\\uD806[\\uDCE0-\\uDCE9]|\\uD81A[\\uDE60-\\uDE69\\uDF50-\\uDF59]|\\uD835[\\uDFCE-\\uDFFF]|\\uD83A[\\uDD50-\\uDD59])';
  var digitSequence = es5DigitSequence + limit(1, digitBlockLimit);

  var leadClassChars = openingParens +
    i18n.phonenumbers.PhoneNumberUtil.PLUS_CHARS_;

  LEAD_CLASS = '[' + leadClassChars + ']';

  /* Phone number pattern allowing optional punctuation. */
  PATTERN = '(?:' + LEAD_CLASS + punctuation + ')' + leadLimit
    + digitSequence + '(?:' + punctuation + digitSequence + ')' + blockLimit
    + '(?:' + i18n.phonenumbers.PhoneNumberUtil.EXTN_PATTERNS_FOR_MATCHING +
    ')?';

}());

/**
 * Trims away any characters after the first match of {@code pattern} in {@code candidate},
 * returning the trimmed version.
 */
function trimAfterFirstMatch(pattern, candidate) {
  var trailingCharsMatcher = pattern.exec(candidate);
  if (trailingCharsMatcher && trailingCharsMatcher.length) {
    candidate = candidate.substring(0, trailingCharsMatcher.index);
  }
  return candidate;
}

function isInvalidPunctuationSymbol(character) {
  return character == '%' || CURRENCY_SYMBOL.test(character);
}

// Character.isDigit() equiv from Java
function isDigit(character) {
  return (new RegExp(i18n.phonenumbers.PhoneNumberUtil.VALID_DIGITS_)).test(character);
}

/**
 * Creates a new instance. See the factory methods in {@link PhoneNumberUtil} on how to obtain a
 * new instance.
 *
 * @param util  the phone number util to use
 * @param text  the character sequence that we will search, null for no text
 * @param country  the country to assume for phone numbers not written in international format
 *     (with a leading plus, or with the international dialing prefix of the specified region).
 *     May be null or "ZZ" if only numbers with a leading plus should be
 *     considered.
 * @param leniency  the leniency to use when evaluating candidate phone numbers
 * @param maxTries  the maximum number of invalid numbers to try before giving up on the text.
 *     This is to cover degenerate cases where the text has a lot of false positives in it. Must
 *     be {@code >= 0}.
 */
i18n.phonenumbers.PhoneNumberMatcher = function(util, text, country, leniency, maxTries) {
  if (util == null) {
    throw new Error('util can not be null');
  }
  if (leniency == null) {
    throw new Error('leniency can not be null');
  }
  if (maxTries < 0) {
    throw new Error('maxTries must be greater than 0');
  }

  /** The phone number utility. */
  this.phoneUtil = util;
  /** The text searched for phone numbers. */
  this.text = text || '';
  /**
   * The region (country) to assume for phone numbers without an international prefix, possibly
   * null.
   */
  this.preferredRegion = country;
  /** The degree of validation requested. NOTE: Java `findNumbers` always uses VALID, so we hard code that here */
  this.leniency = leniency;

  /** The maximum number of retries after matching an invalid number. */
  this.maxTries = maxTries;

  /** The iteration tristate. */
  this.state = State.NOT_READY;
  /** The last successful match, null unless in {@link State#READY}. */
  this.lastMatch = null;
  /** The next index to start searching at. Undefined in {@link State#DONE}. */
  this.searchIndex = 0;
};

/**
 * Helper method to determine if a character is a Latin-script letter or not. For our purposes,
 * combining marks should also return true since we assume they have been added to a preceding
 * Latin character.
 */
i18n.phonenumbers.PhoneNumberMatcher.isLatinLetter = function(letter) {
  // Combining marks are a subset of non-spacing-mark.
  if (!IS_LETTER.test(letter) && !NON_SPACING_MARK.test(letter)) {
    return false;
  }

  return IS_LATIN.test(letter);
};

/**
 * Attempts to find the next subsequence in the searched sequence on or after {@code searchIndex}
 * that represents a phone number. Returns the next match, null if none was found.
 *
 * @param index  the search index to start searching at
 * @return  the phone number match found, null if none can be found
 */
i18n.phonenumbers.PhoneNumberMatcher.prototype.find = function(index) {
  var matches;
  var patternRegex = new RegExp(PATTERN, 'ig');
  patternRegex.lastIndex = index;

  while((this.maxTries > 0) && ((matches = patternRegex.exec(this.text)))) {
    var start = matches.index;
    var candidate = matches[0];
    
    // Check for extra numbers at the end.
    // TODO: This is the place to start when trying to support extraction of multiple phone number
    // from split notations (+41 79 123 45 67 / 68).
    candidate = trimAfterFirstMatch(
      i18n.phonenumbers.PhoneNumberUtil.SECOND_NUMBER_START_PATTERN_,
      candidate
    );

    var match = this.extractMatch(candidate, start);
    if (match != null) {
      return match;
    }

    this.maxTries--;
    patternRegex.lastIndex = start + candidate.length + 1;
  }

  return null;
};

// XXX: do I care about doing iterator() to wrap these?  And/or
// should this have some more JS-like interface?
i18n.phonenumbers.PhoneNumberMatcher.prototype.hasNext = function() {
  if (this.state == State.NOT_READY) {
    this.lastMatch = this.find(this.searchIndex);
    if (this.lastMatch == null) {
      this.state = State.DONE;
    } else {
      this.searchIndex = this.lastMatch.end;
      this.state = State.READY;
    }
  }
  return this.state == State.READY;
};

i18n.phonenumbers.PhoneNumberMatcher.prototype.next = function() {
  // Check the state and find the next match as a side-effect if necessary.
  if (!this.hasNext()) {
    throw new Error('no element');
  }

  // Don't retain that memory any longer than necessary.
  var result = this.lastMatch;
  this.lastMatch = null;
  this.state = State.NOT_READY;
  return result;
};

i18n.phonenumbers.PhoneNumberMatcher.containsMoreThanOneSlashInNationalNumber = function(number, candidate) {
  var firstSlashInBodyIndex = candidate.indexOf('/');
  if (firstSlashInBodyIndex < 0) {
    // No slashes, this is okay.
    return false;
  }
  // Now look for a second one.
  var secondSlashInBodyIndex = candidate.indexOf('/', firstSlashInBodyIndex + 1);
  if (secondSlashInBodyIndex < 0) {
    // Only one slash, this is okay.
    return false;
  }

  // If the first slash is after the country calling code, this is permitted.
  var candidateHasCountryCode =
      (number.getCountryCodeSource() == CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN ||
       number.getCountryCodeSource() == CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN);
  if (candidateHasCountryCode && i18n.phonenumbers.PhoneNumberUtil.normalizeDigitsOnly(
    candidate.substring(0, firstSlashInBodyIndex)) == number.getCountryCode())
  {
    // Any more slashes and this is illegal.
    return candidate.substring(secondSlashInBodyIndex + 1).indexOf('/') > -1;
  }
  return true;
};

i18n.phonenumbers.PhoneNumberMatcher.containsOnlyValidXChars = function(number, candidate, util) {
  // The characters 'x' and 'X' can be (1) a carrier code, in which case they always precede the
  // national significant number or (2) an extension sign, in which case they always precede the
  // extension number. We assume a carrier code is more than 1 digit, so the first case has to
  // have more than 1 consecutive 'x' or 'X', whereas the second case can only have exactly 1 'x'
  // or 'X'. We ignore the character if it appears as the last character of the string.
  for (var index = 0; index < candidate.length - 1; index++) {
    var charAtIndex = candidate.charAt(index);
    if (charAtIndex == 'x' || charAtIndex == 'X') {
      var charAtNextIndex = candidate.charAt(index + 1);
      if (charAtNextIndex == 'x' || charAtNextIndex == 'X') {
        // This is the carrier code case, in which the 'X's always precede the national
        // significant number.
        index++;
        if (util.isNumberMatch(number, candidate.substring(index)) !=
          i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH
        ) {
          return false;
        }
      // This is the extension sign case, in which the 'x' or 'X' should always precede the
      // extension number.
      } else if (!i18n.phonenumbers.PhoneNumberUtil.normalizeDigitsOnly(
        candidate.substring(index)) == number.getExtension()
      ) {
        return false;
      }
    }
  }
  return true;
};

/**
 * Attempts to extract a match from a {@code candidate} character sequence.
 *
 * @param candidate  the candidate text that might contain a phone number
 * @param offset  the offset of {@code candidate} within {@link #text}
 * @return  the match found, null if none can be found
 */
i18n.phonenumbers.PhoneNumberMatcher.prototype.extractMatch = function(candidate, offset) {
  // Skip a match that is more likely to be a date.
  if (SLASH_SEPARATED_DATES.test(candidate)) {
    return null;
  }

  // Skip potential time-stamps.
  if (TIME_STAMPS.test(candidate)) {
    var followingText = this.text.substring(offset + candidate.length);
    if (TIME_STAMPS_SUFFIX.test(followingText)) {
      return null;
    }
  }

  // Try to come up with a valid match given the entire candidate.
  var match = this.parseAndVerify(candidate, offset);
  if (match != null) {
    return match;
  }

  // If that failed, try to find an "inner match" - there might be a phone number within this
  // candidate.
  return this.extractInnerMatch(candidate, offset);
};

/**
 * Attempts to extract a match from {@code candidate} if the whole candidate does not qualify as a
 * match.
 *
 * @param candidate  the candidate text that might contain a phone number
 * @param offset  the current offset of {@code candidate} within {@link #text}
 * @return  the match found, null if none can be found
 */
i18n.phonenumbers.PhoneNumberMatcher.prototype.extractInnerMatch = function(candidate, offset) {
  var groupMatch;
  var innerMatchRegex;
  var group;
  var match;
  var i;

  for (i = 0; i < INNER_MATCHES.length; i++) {
    var isFirstMatch = true;
    innerMatchRegex = new RegExp(INNER_MATCHES[i], 'ig');
    while ((groupMatch = innerMatchRegex.exec(candidate)) && this.maxTries > 0) {
      if (isFirstMatch) {
        // We should handle any group before this one too.
        group = trimAfterFirstMatch(
          i18n.phonenumbers.PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN_,
          candidate.substring(0, groupMatch.index)
        );
        match = this.parseAndVerify(group, offset);
        if (match != null) {
          return match;
        }
        this.maxTries--;
        isFirstMatch = false;
      }
      group = trimAfterFirstMatch(
        i18n.phonenumbers.PhoneNumberUtil.UNWANTED_END_CHAR_PATTERN_,
        groupMatch[1]
      );
      match = this.parseAndVerify(group, offset + groupMatch.index);
      if (match != null) {
        return match;
      }
      this.maxTries--;
    }
  }
  return null;
};

/**
 * Parses a phone number from the {@code candidate} using {@link PhoneNumberUtil#parse} and
 * verifies it matches the requested {@link #leniency}. If parsing and verification succeed, a
 * corresponding {@link PhoneNumberMatch} is returned, otherwise this method returns null.
 *
 * @param candidate  the candidate match
 * @param offset  the offset of {@code candidate} within {@link #text}
 * @return  the parsed and validated phone number match, or null
 */
i18n.phonenumbers.PhoneNumberMatcher.prototype.parseAndVerify = function(candidate, offset) {
  try {
    // Check the candidate doesn't contain any formatting which would indicate that it really
    // isn't a phone number.
    if (!MATCHING_BRACKETS.test(candidate) || PUB_PAGES.test(candidate)) {
      return null;
    }

    // If leniency is set to VALID or stricter, we also want to skip numbers that are surrounded
    // by Latin alphabetic characters, to skip cases like abc8005001234 or 8005001234def.
    // If the candidate is not at the start of the text, and does not start with phone-number
    // punctuation, check the previous character.
    if(this.leniency.value >= i18n.phonenumbers.PhoneNumberUtil.Leniency.VALID.value) {
      if (offset > 0) {
        var leadClassRe = new RegExp('^' + LEAD_CLASS);
        var leadClassMatches = leadClassRe.exec(candidate);
        if(leadClassMatches && leadClassMatches.index !== 0) {
          var previousChar = this.text.charAt(offset - 1);
          // We return null if it is a latin letter or an invalid punctuation symbol.
          if (isInvalidPunctuationSymbol(previousChar) ||
            i18n.phonenumbers.PhoneNumberMatcher.isLatinLetter(previousChar))
          {
            return null;
          }
        }
      }
      var lastCharIndex = offset + candidate.length;
      if (lastCharIndex < this.text.length) {
        var nextChar = this.text.charAt(lastCharIndex);
        if (isInvalidPunctuationSymbol(nextChar) ||
          i18n.phonenumbers.PhoneNumberMatcher.isLatinLetter(nextChar))
        {
          return null;
        }
      }
    }

    var number = this.phoneUtil.parseAndKeepRawInput(candidate, this.preferredRegion);

    // Check Israel * numbers: these are a special case in that they are four-digit numbers that
    // our library supports, but they can only be dialled with a leading *. Since we don't
    // actually store or detect the * in our phone number library, this means in practice we
    // detect most four digit numbers as being valid for Israel. We are considering moving these
    // numbers to ShortNumberInfo instead, in which case this problem would go away, but in the
    // meantime we want to restrict the false matches so we only allow these numbers if they are
    // preceded by a star. We enforce this for all leniency levels even though these numbers are
    // technically accepted by isPossibleNumber and isValidNumber since we consider it to be a
    // deficiency in those methods that they accept these numbers without the *.
    // TODO: Remove this or make it significantly less hacky once we've decided how to
    // handle these short codes going forward in ShortNumberInfo. We could use the formatting
    // rules for instance, but that would be slower.
    if (this.phoneUtil.getRegionCodeForCountryCode(number.getCountryCode()) == 'IL'
        && this.phoneUtil.getNationalSignificantNumber(number).length == 4
        && (offset == 0 || (offset > 0 && this.text.charAt(offset - 1) != '*')))
    {
      // No match.
      return null;
    }

    if (this.leniency.verify(number, candidate, this.phoneUtil)) {
      // We used parseAndKeepRawInput to create this number, but for now we don't return the extra
      // values parsed. TODO: stop clearing all values here and switch all users over
      // to using rawInput() rather than the rawString() of PhoneNumberMatch.
      number.clearCountryCodeSource();
      number.clearRawInput();
      number.clearPreferredDomesticCarrierCode();
      return new i18n.phonenumbers.PhoneNumberMatch(offset, candidate, number);
    }
  } catch (e) {
    // XXX: remove this, just filtering errors for easier debugging of tests...
    if(e.message.indexOf("hone number") == -1) {
      console.log(e);
    }
    // ignore and continue
  }
  return null;
};

i18n.phonenumbers.PhoneNumberMatcher.isNationalPrefixPresentIfRequired = function(number, util) {
  // First, check how we deduced the country code. If it was written in international format, then
  // the national prefix is not required.
  if (number.getCountryCodeSource() != CountryCodeSource.FROM_DEFAULT_COUNTRY) {
    return true;
  }
  var phoneNumberRegion =
    util.getRegionCodeForCountryCode(number.getCountryCode());
  var metadata = util.getMetadataForRegion(phoneNumberRegion);
  if (metadata == null) {
    return true;
  }
  // Check if a national prefix should be present when formatting this number.
  var nationalNumber = util.getNationalSignificantNumber(number);
  var formatRule = util.chooseFormattingPatternForNumber_(
    metadata.numberFormatArray(),
    nationalNumber
  );
  // To do this, we check that a national prefix formatting rule was present and that it wasn't
  // just the first-group symbol ($1) with punctuation.
  var nationalPrefixFormattingRule = formatRule &&
                                     formatRule.getNationalPrefixFormattingRule(); 
  if (nationalPrefixFormattingRule && nationalPrefixFormattingRule.length > 0) {
    if (formatRule.getNationalPrefixOptionalWhenFormatting()) {
      // The national-prefix is optional in these cases, so we don't need to check if it was
      // present.
      return true;
    }
    if (util.formattingRuleHasFirstGroupOnly(nationalPrefixFormattingRule)) {
      // National Prefix not needed for this number.
      return true;
    }
    // Normalize the remainder.
    var rawInputCopy = i18n.phonenumbers.PhoneNumberUtil.normalizeDigitsOnly(number.getRawInput());
    var rawInput = new goog.string.StringBuffer(rawInputCopy);
    // Check if we found a national prefix and/or carrier code at the start of the raw input, and
    // return the result.
    return util.maybeStripNationalPrefixAndCarrierCode(rawInput, metadata, null);
  }
  return true;
};

i18n.phonenumbers.PhoneNumberMatcher.checkNumberGroupingIsValid = function(number, candidate, util, checker) {
  // TODO: Evaluate how this works for other locales (testing has been limited to NANPA regions)
  // and optimise if necessary.
  var normalizedCandidate =
    i18n.phonenumbers.PhoneNumberUtil.normalizeDigits(candidate, true /* keep non-digits */);
  var formattedNumberGroups = getNationalNumberGroups(util, number, null);
  if (checker.checkGroups(util, number, normalizedCandidate, formattedNumberGroups)) {
    return true;
  }

/**
  XXX: TODO - not sure what to do here for MetadataManager.getAlternateFormatsForCountry(number.getCountryCode());

  // If this didn't pass, see if there are any alternate formats, and try them instead.
  var alternateFormats =
    MetadataManager.getAlternateFormatsForCountry(number.getCountryCode());
  if (alternateFormats != null) {
    var formats = alternateFormats.numberFormats();
    var alternateFormat;
    for (var i = 0; i < formats.length; i++) {
      alternateFormat = formats[i];
      formattedNumberGroups = getNationalNumberGroups(util, number, alternateFormat);
      if (checker.checkGroups(util, number, normalizedCandidate, formattedNumberGroups)) {
        return true;
      }
    }
  }

*/

  return false;
}

i18n.phonenumbers.PhoneNumberMatcher.allNumberGroupsRemainGrouped = function(
  util,number, normalizedCandidate, formattedNumberGroups) {

  var fromIndex = 0;
  if (number.getCountryCodeSource() != CountryCodeSource.FROM_DEFAULT_COUNTRY) {
    // First skip the country code if the normalized candidate contained it.
    var countryCode = number.getCountryCode();
    fromIndex = normalizedCandidate.indexOf(countryCode) + countryCode.length;
  }
  // Check each group of consecutive digits are not broken into separate groupings in the
  // {@code normalizedCandidate} string.
  for (var i = 0; i < formattedNumberGroups.length; i++) {
    // Fails if the substring of {@code normalizedCandidate} starting from {@code fromIndex}
    // doesn't contain the consecutive digits in formattedNumberGroups[i].
    fromIndex = normalizedCandidate.indexOf(formattedNumberGroups[i], fromIndex);
    if (fromIndex < 0) {
      return false;
    }
    // Moves {@code fromIndex} forward.
    fromIndex += formattedNumberGroups[i].length;
    if (i == 0 && fromIndex < normalizedCandidate.length) {
      // We are at the position right after the NDC. We get the region used for formatting
      // information based on the country code in the phone number, rather than the number itself,
      // as we do not need to distinguish between different countries with the same country
      // calling code and this is faster.
      var region = util.getRegionCodeForCountryCode(number.getCountryCode());
      if (util.getNddPrefixForRegion(region, true) != null
          && isDigit(normalizedCandidate.charAt(fromIndex)))
      {
        // This means there is no formatting symbol after the NDC. In this case, we only
        // accept the number if there is no formatting symbol at all in the number, except
        // for extensions. This is only important for countries with national prefixes.
        var nationalSignificantNumber = util.getNationalSignificantNumber(number);
        return normalizedCandidate.substring(fromIndex - formattedNumberGroups[i].length)
          .startsWith(nationalSignificantNumber);
      }
    }
  }
  // The check here makes sure that we haven't mistakenly already used the extension to
  // match the last group of the subscriber number. Note the extension cannot have
  // formatting in-between digits.
  return normalizedCandidate.substring(fromIndex).indexOf(number.getExtension()) > -1;
}

i18n.phonenumbers.PhoneNumberMatcher.allNumberGroupsAreExactlyPresent = function(
  util, number, normalizedCandidate, formattedNumberGroups) {

  var candidateGroups = normalizedCandidate.split(
    i18n.phonenumbers.PhoneNumberUtil.NON_DIGITS_PATTERN_);
  // Set this to the last group, skipping it if the number has an extension.
  var candidateNumberGroupIndex =
    number.hasExtension() ? candidateGroups.length - 2 : candidateGroups.length - 1;
  // First we check if the national significant number is formatted as a block.
  // We use contains and not equals, since the national significant number may be present with
  // a prefix such as a national number prefix, or the country code itself.
  if (candidateGroups.length == 1 ||
      candidateGroups[candidateNumberGroupIndex].indexOf(
        util.getNationalSignificantNumber(number)) > -1)
  {
    return true;
  }
  // Starting from the end, go through in reverse, excluding the first group, and check the
  // candidate and number groups are the same.
  for (var formattedNumberGroupIndex = (formattedNumberGroups.length - 1);
       formattedNumberGroupIndex > 0 && candidateNumberGroupIndex >= 0;
       formattedNumberGroupIndex--, candidateNumberGroupIndex--)
  {
    if (!candidateGroups[candidateNumberGroupIndex] ==
      formattedNumberGroups[formattedNumberGroupIndex])
    {
      return false;
    }
  }
  // Now check the first group. There may be a national prefix at the start, so we only check
  // that the candidate group ends with the formatted number group.
  return (candidateNumberGroupIndex >= 0
    && candidateGroups[candidateNumberGroupIndex].endsWith(formattedNumberGroups[0]));
}

/**
 * Helper method to get the national-number part of a number, formatted without any national
 * prefix, and return it as a set of digit blocks that would be formatted together.
 */
function getNationalNumberGroups(util, number, formattingPattern) {
  if (formattingPattern == null) {
    // This will be in the format +CC-DG;ext=EXT where DG represents groups of digits.
    var rfc3966Format = util.format(number, i18n.phonenumbers.PhoneNumberFormat.RFC3966);
    // We remove the extension part from the formatted string before splitting it into different
    // groups.
    var endIndex = rfc3966Format.indexOf(';');
    if (endIndex < 0) {
      endIndex = rfc3966Format.length;
    }
    // The country-code will have a '-' following it.
    var startIndex = rfc3966Format.indexOf('-') + 1;
    return rfc3966Format.substring(startIndex, endIndex).split('-');
  } else {
    // We format the NSN only, and split that according to the separator.
    var nationalSignificantNumber = util.getNationalSignificantNumber(number);
    return util.formatNsnUsingPattern(
      nationalSignificantNumber,
      formattingPattern,
      i18n.phonenumbers.PhoneNumberFormat.RFC3966
    ).split('-');
  }
}
