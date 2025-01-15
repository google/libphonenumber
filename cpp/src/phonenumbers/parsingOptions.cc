#include "i18n/phonenumbers/parsingoptions.h"

#include "i18n/identifiers/regioncode.h"

namespace i18n {
namespace phonenumbers {

ParsingOptions& ParsingOptions::SetDefaultRegion(
    i18n_identifiers::RegionCode default_region) {
  default_region_ = default_region;
  return *this;
}

ParsingOptions& ParsingOptions::SetKeepRawInput(bool keep_raw_input) {
  keep_raw_input_ = keep_raw_input;
  return *this;
}

}  // namespace phonenumbers
}  // namespace i18n