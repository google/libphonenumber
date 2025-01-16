#include "i18n/phonenumbers/parsingoptions.h"

#include "phonenumbers/region_code.h"

namespace i18n {
namespace phonenumbers {

ParsingOptions& ParsingOptions::SetDefaultRegion(
    const string& default_region) {
  default_region_ = default_region;
  return *this;
}

ParsingOptions& ParsingOptions::SetKeepRawInput(bool keep_raw_input) {
  keep_raw_input_ = keep_raw_input;
  return *this;
}

}  // namespace phonenumbers
}  // namespace i18n