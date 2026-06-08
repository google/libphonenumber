#include <fstream>
#include <string>
#include <iostream>
#include "absl/flags/flag.h"
#include "absl/flags/parse.h"
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/metadataconvertor.h"
#include "phonenumbers/short_metadata.h"

ABSL_FLAG(std::string, output_file, "metadata.dat", "output file name");

bool LoadCompiledInMetadataShort(i18n::phonenumbers::PhoneMetadataCollection* metadata) {
  if (!metadata->ParseFromArray(i18n::phonenumbers::short_metadata_get(), i18n::phonenumbers::short_metadata_size())) {
    std::cerr << "Could not parse binary data.";
    return false;
  }

  return true;
}

std::string get_short_file_name(const std::string& filename) {
  std::string::size_type idx = filename.rfind('.');
  if(idx != std::string::npos){
    std::string ext = filename.substr(idx+1);
    std::string name = filename.substr(0, idx);

    std::string short_name = name + "_short." + ext;
    return short_name;
  }
  return filename + "_short";
}

int main(int argc, char *argv[]) {

  absl::ParseCommandLine(argc, argv);

  std::string output_file = absl::GetFlag(FLAGS_output_file);

  i18n::phonenumbers::PhoneMetadataCollection metadata_collection;

  MetadataConvertor convertor;
  if (!convertor.LoadCompiledInMetadata(&metadata_collection)) {
    std::cerr << "Could not parse compiled-in metadata." << std::endl;
    return -1;
  }

  std::fstream output(output_file, std::ios::out | std::ios::trunc | std::ios::binary);
  if (!metadata_collection.SerializeToOstream(&output)) {
    std::cerr << "Failed to write metadata output file." << std::endl;
    return -1;
  }

  i18n::phonenumbers::PhoneMetadataCollection short_metadata_collection;
  std::string short_output_file = get_short_file_name(output_file);
  if (!LoadCompiledInMetadataShort(&short_metadata_collection)) {
    std::cerr << "Could not parse compiled-in metadata." << std::endl;
    return -1;
  }

  std::fstream output_short(short_output_file, std::ios::out | std::ios::trunc | std::ios::binary);
  if (!short_metadata_collection.SerializeToOstream(&output_short)) {
    std::cerr << "Failed to write metadata output file." << std::endl;
    return -1;
  }

  return 0;
}
