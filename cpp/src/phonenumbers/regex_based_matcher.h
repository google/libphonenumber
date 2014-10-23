#ifndef I18N_PHONENUMBERS_REGEX_BASED_MATCHER_H_
#define I18N_PHONENUMBERS_REGEX_BASED_MATCHER_H_

#include <memory>
#include <string>

#include "phonenumbers/base/basictypes.h"
#include "phonenumbers/base/memory/scoped_ptr.h"
#include "phonenumbers/matcher_api.h"

namespace i18n {
namespace phonenumbers {

class AbstractRegExpFactory;
class PhoneNumberDesc;
class RegExpCache;

// Implementation of the matcher API using the regular expressions in the
// PhoneNumberDesc proto message to match numbers.
class RegexBasedMatcher : public MatcherApi {
 public:
  RegexBasedMatcher();
  ~RegexBasedMatcher();

  bool MatchesNationalNumber(const string& national_number,
                             const PhoneNumberDesc& number_desc,
                             bool allow_prefix_match) const;

  bool MatchesPossibleNumber(const string& national_number,
                             const PhoneNumberDesc& number_desc) const;

 private:
  bool Match(const string& national_number, const string& number_pattern,
             bool allow_prefix_match) const;

  const scoped_ptr<const AbstractRegExpFactory> regexp_factory_;
  const scoped_ptr<RegExpCache> regexp_cache_;

  DISALLOW_COPY_AND_ASSIGN(RegexBasedMatcher);
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_REGEX_BASED_MATCHER_H_
