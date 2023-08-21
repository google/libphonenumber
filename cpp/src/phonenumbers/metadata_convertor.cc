#include <fstream>
#include <string>
#include <iostream>
#include "phonenumbers/phonemetadata.pb.h"
#include "phonenumbers/phonenumber.pb.h"
#include "phonenumbers/metadata.h"

void usage(std::string prog_name) {
  std::cout << prog_name << "[options]" << std::endl <<
      "Options:" << std::endl <<
      "-h | --help        Print this help" << std::endl <<
      "-o | --output_file Output file name" << std::endl;
}

bool LoadCompiledInMetadata(i18n::phonenumbers::PhoneMetadataCollection* metadata) {
  if (!metadata->ParseFromArray(i18n::phonenumbers::metadata_get(), i18n::phonenumbers::metadata_size())) {
    std::cerr << "Could not parse binary data.";
    return false;
  }
  
  return true;
}

int main(int argc, char *argv[]) {

  if(argc < 3) {
    std::cerr << "please provide output file name argument" <<  std::endl;
    usage(argv[0]);
    return -1;
  }

  if(strncmp(argv[1], "-o", 2) != 0 && strncmp(argv[1], "--output", 8) != 0) {
    std::cerr << "please provide output file name argument" <<  std::endl;
    usage(argv[0]);
    return -1;
  }

  i18n::phonenumbers::PhoneMetadataCollection metadata_collection;
  if (!LoadCompiledInMetadata(&metadata_collection)) {
    std::cerr << "Could not parse compiled-in metadata." << std::endl;
    return -1;
  }

  std::fstream output(argv[2], std::ios::out | std::ios::trunc | std::ios::binary);
  if (!metadata_collection.SerializeToOstream(&output)) {
     std::cerr << "Failed to write metadata output file." << std::endl;
     return -1;
  }

  return 0;
}
