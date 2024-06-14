#ifndef I18N_PHONENUMBERS_METADATACONVERTOR_H_
#define I18N_PHONENUMBERS_METADATACONVERTOR_H_

#ifdef USE_LITE_METADATA
#include "phonenumbers/metadata_lite.h"
#else
#include "phonenumbers/metadata.h"
#endif

class MetadataConvertor {
  public:
    bool LoadCompiledInMetadata(i18n::phonenumbers::PhoneMetadataCollection* metadata)const {
      if (!metadata->ParseFromArray(i18n::phonenumbers::metadata_get(), i18n::phonenumbers::metadata_size())) {
        std::cerr << "ShortNumberConvertor : Could not parse binary data.";
        return false;
      }

    return true;
  }

 public:
  MetadataConvertor(){}
  ~MetadataConvertor(){}
};
#endif  // I18N_PHONENUMBERS_METADATACONVERTOR_H_
