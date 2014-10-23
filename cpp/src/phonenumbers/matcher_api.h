#ifndef I18N_PHONENUMBERS_MATCHER_API_H_
#define I18N_PHONENUMBERS_MATCHER_API_H_

#include <string>

namespace i18n {
namespace phonenumbers {

using std::string;

class PhoneNumberDesc;

// Internal phonenumber matching API used to isolate the underlying
// implementation of the matcher and allow different implementations to be
// swapped in easily.
class MatcherApi {
 public:
  virtual ~MatcherApi() {}

  // Returns whether the given national number (a string containing only decimal
  // digits) matches the national number pattern defined in the given
  // PhoneNumberDesc message.
  virtual bool MatchesNationalNumber(const string& national_number,
                                     const PhoneNumberDesc& number_desc,
                                     bool allow_prefix_match) const = 0;

  // Returns whether the given national number (a string containing only decimal
  // digits) matches the possible number pattern defined in the given
  // PhoneNumberDesc message.
  virtual bool MatchesPossibleNumber(
      const string& national_number,
      const PhoneNumberDesc& number_desc) const = 0;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_MATCHER_API_H_
