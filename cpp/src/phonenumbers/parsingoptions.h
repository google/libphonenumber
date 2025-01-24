#ifndef I18N_PHONENUMBERS_PARSINGOPTIONS_H_
#define I18N_PHONENUMBERS_PARSINGOPTIONS_H_

#include <string>

namespace i18n {
namespace phonenumbers {

// Options for parsing a phone number. To be used with the ParseWithOptions
// method.
// Example:
//   ParsingOptions().SetDefaultRegion(RegionCode::US()).SetKeepRawInput(true);
class ParsingOptions {
 public:
  ParsingOptions() : default_region_("ZZ"), keep_raw_input_(false) {};

  // Set the value for default_region_.
  ParsingOptions& SetDefaultRegion(
      const std::string& default_region);

  // Set the value for keep_raw_input_.
  ParsingOptions& SetKeepRawInput(bool keep_raw_input);

 private:
  friend class PhoneNumberUtil;

  // The region we are expecting the number to be from. This is ignored if the
  // number being parsed is written in international format. In case of national
  // format, the country_code will be set to the one of this default region. If
  // the number is guaranteed to start with a '+' followed by the country
  // calling code, then RegionCode.ZZ or null can be supplied.
  std::string default_region_;

  // Whether the raw input should be kept in the PhoneNumber object. If true,
  // the raw_input field and country_code_source fields will be populated.
  bool keep_raw_input_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I1