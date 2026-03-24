// Copyright (C) 2012 The Libphonenumber Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Author: Patrick Mezard

#include "cpp-build/generate_geocoding_data.h"

#include <dirent.h>
#include <errno.h>
#include <locale>
#include <sys/stat.h>
#include <algorithm>
#include <cctype>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <iomanip>
#include <iterator>
#include <map>
#include <set>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

#include "base/basictypes.h"

#include "absl/container/btree_map.h"
#include "absl/container/btree_set.h"

namespace i18n {
namespace phonenumbers {

using std::map;
using std::string;
using std::vector;
using std::set;
using std::pair;

template <typename ResourceType> class AutoCloser {
 public:
  typedef int (*ReleaseFunction) (ResourceType* resource);

  AutoCloser(ResourceType** resource, ReleaseFunction release_function)
      : resource_(resource),
        release_function_(release_function)
  {}

  ~AutoCloser() {
    Close();
  }

  ResourceType* get_resource() const {
    return *resource_;
  }

  void Close() {
    if (*resource_) {
      release_function_(*resource_);
      *resource_ = NULL;
    }
  }

 private:
  ResourceType** resource_;
  ReleaseFunction release_function_;
};

enum DirEntryKinds {
  kFile = 0,
  kDirectory = 1,
};

class DirEntry {
 public:
  DirEntry(const char* n, DirEntryKinds k)
      : name_(n),
        kind_(k)
  {}

  const std::string& name() const { return name_; }
  DirEntryKinds kind() const { return kind_; }

 private:
  std::string name_;
  DirEntryKinds kind_;
};

// Lists directory entries in path. "." and ".." are excluded. Returns true on
// success.
bool ListDirectory(const string& path, vector<DirEntry>* entries) {
  entries->clear();
  DIR* dir = opendir(path.c_str());
  if (!dir) {
    return false;
  }
  AutoCloser<DIR> dir_closer(&dir, closedir);
  struct dirent *entry;
  struct stat entry_stat;
  while (true) {
    // Set errno to 0 to be able to check if an error occurs during the
    // readdir() call. NULL is the return value when the end of the directory
    // stream is reached or when an error occurs, and the errno check is the
    // only thing that helps us distinguish between the two cases. See
    // documentation at
    // http://pubs.opengroup.org/onlinepubs/9699919799/functions/readdir.html
    errno = 0;
    entry = readdir(dir);
    if (entry == NULL) {
      return errno == 0;
    }
    if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
       continue;
    }
    const string entry_path = path + "/" + entry->d_name;
    if (stat(entry_path.c_str(), &entry_stat)) {
      return false;
    }
    DirEntryKinds kind = kFile;
    if (S_ISDIR(entry_stat.st_mode)) {
      kind = kDirectory;
    } else if (!S_ISREG(entry_stat.st_mode)) {
      continue;
    }
    entries->push_back(DirEntry(entry->d_name, kind));
  }
}

// Returns true if s ends with suffix.
bool EndsWith(const string& s, const string& suffix) {
  if (suffix.length() > s.length()) {
    return false;
  }
  return std::equal(suffix.rbegin(), suffix.rend(), s.rbegin());
}

// Converts string to integer, returns true on success.
bool StrToInt(const string& s, int32* n) {
  std::stringstream stream;
  stream << s;
  stream >> *n;
  return !stream.fail();
}

// Converts integer to string, returns true on success.
bool IntToStr(int32 n, string* s) {
  std::stringstream stream;
  stream << n;
  stream >> *s;
  return !stream.fail();
}

// Parses the prefix descriptions file at path, clears and fills the output
// prefixes phone number prefix to description mapping.
// Returns true on success.
bool ParsePrefixes(const string& path,
                   absl::btree_map<int32, string>* prefixes) {
  prefixes->clear();
  FILE* input = fopen(path.c_str(), "r");
  if (!input) {
    return false;
  }
  AutoCloser<FILE> input_closer(&input, fclose);
  const int kMaxLineLength = 2*1024;
  vector<char> buffer(kMaxLineLength);
  vector<char>::iterator begin, end, sep;
  string prefix, description;
  int32 prefix_code;
  while (fgets(&buffer[0], buffer.size(), input)) {
    begin = buffer.begin();
    end = std::find(begin, buffer.end(), '\0');
    if (end == begin) {
      continue;
    }
    --end;
    if (*end != '\n' && !feof(input)) {
      // A line without LF can only happen at the end of file.
      return false;
    }

    // Trim and check for comments.
    for (; begin != end && std::isspace(*begin); ++begin) {}
    for (; end != begin && std::isspace(*(end - 1)); --end) {}
    if (begin == end || *begin == '#') {
      continue;
    }

    sep = std::find(begin, end, '|');
    if (sep == end) {
      continue;
    }
    prefix = string(begin, sep);
    if (!StrToInt(prefix, &prefix_code)) {
      return false;
    }
    (*prefixes)[prefix_code] = string(sep + 1, end);
  }
  return ferror(input) == 0;
}

// Builds a C string literal from s. The output is enclosed in double-quotes and
// care is taken to escape input quotes and non-ASCII or control characters.
//
// An input string:
//   Op\xc3\xa9ra
// becomes:
//   "Op""\xc3""\xa9""ra"
string MakeStringLiteral(const string& s) {
  std::stringstream buffer;
  int prev_is_hex = 0;
  buffer << std::hex << std::setfill('0');
  buffer << "\"";
  for (string::const_iterator it = s.begin(); it != s.end(); ++it) {
    const char c = *it;
    if (c >= 32 && c < 127) {
      if (prev_is_hex == 2) {
        buffer << "\"\"";
      }
      if (c == '\'') {
        buffer << "\\";
      }
      buffer << c;
      prev_is_hex = 1;
    } else {
      if (prev_is_hex != 0) {
        buffer << "\"\"";
      }
      buffer << "\\x" << std::setw(2) << (c < 0 ? c + 256 : c);
      prev_is_hex = 2;
    }
  }
  buffer << "\"";
  return buffer.str();
}

void WriteStringLiteral(const string& s, FILE* output) {
  string literal = MakeStringLiteral(s);
  fprintf(output, "%s", literal.c_str());
}

const char kLicense[] =
  "// Copyright (C) 2012 The Libphonenumber Authors\n"
  "//\n"
  "// Licensed under the Apache License, Version 2.0 (the \"License\");\n"
  "// you may not use this file except in compliance with the License.\n"
  "// You may obtain a copy of the License at\n"
  "//\n"
  "// http://www.apache.org/licenses/LICENSE-2.0\n"
  "//\n"
  "// Unless required by applicable law or agreed to in writing, software\n"
  "// distributed under the License is distributed on an \"AS IS\" BASIS,\n"
  "// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or "
  "implied.\n"
  "// See the License for the specific language governing permissions and\n"
  "// limitations under the License.\n"
  "//\n"
  "// This file is generated automatically, do not edit it manually.\n"
  "\n";

void WriteLicense(FILE* output) {
  fprintf(output, "%s", kLicense);
}

const char kI18NNS[] = "i18n";
const char kPhoneNumbersNS[] = "phonenumbers";

void WriteNSHeader(FILE* output) {
  fprintf(output, "namespace %s {\n", kI18NNS);
  fprintf(output, "namespace %s {\n", kPhoneNumbersNS);
}

void WriteNSFooter(FILE* output) {
  fprintf(output, "}  // namespace %s\n", kPhoneNumbersNS);
  fprintf(output, "}  // namespace %s\n", kI18NNS);
}

void WriteCppHeader(const string& base_name, FILE* output) {
  fprintf(output, "#include \"phonenumbers/geocoding/%s.h\"\n",
          base_name.c_str());
  fprintf(output, "\n");
  fprintf(output, "#include <cstdint>\n");
  fprintf(output, "\n");
}

void WriteArrayAndSize(const string& name, FILE* output) {
  fprintf(output, "  %s,\n", name.c_str());
  fprintf(output, "  sizeof(%s)/sizeof(*%s),\n", name.c_str(), name.c_str());
}

// Writes a PrefixDescriptions variable named "name", with its prefixes field
// set to "prefixes_name" variable, its descriptions to "desc_name" and its
// possible_lengths to "possible_lengths_name":
//
// const PrefixDescriptions ${name} = {
//   ${prefix_name},
//   sizeof(${prefix_name})/sizeof(*${prefix_name}),
//   ${desc_name},
//   ${possible_lengths_name},
//   sizeof(${possible_lengths_name})/sizeof(*${possible_lengths_name}),
// };
//
void WritePrefixDescriptionsDefinition(
    const string& name, const string& prefixes_name, const string& desc_name,
    const string& possible_lengths_name, FILE* output) {
  fprintf(output, "const PrefixDescriptions %s = {\n", name.c_str());
  WriteArrayAndSize(prefixes_name, output);
  fprintf(output, "  %s,\n", desc_name.c_str());
  WriteArrayAndSize(possible_lengths_name, output);
  fprintf(output, "};\n");
}

// Writes prefixes, descriptions and possible_lengths arrays built from the
// phone number prefix to description mapping "prefixes". Binds these arrays
// in a single PrefixDescriptions variable named "var_name".
//
// const int32_t ${var_name}_prefixes[] = {
//   1201,
//   1650,
// };
//
// const char* ${var_name}_descriptions[] = {
//   "New Jerse",
//   "Kalifornie",
// };
//
// const int32_t ${var_name}_possible_lengths[] = {
//   4,
// };
//
// const PrefixDescriptions ${var_name} = {
//   ...
// };
//
void WritePrefixDescriptions(const string& var_name,
                             const absl::btree_map<int, string>& prefixes,
                             FILE* output) {
  absl::btree_set<int> possible_lengths;
  const string prefixes_name = var_name + "_prefixes";
  fprintf(output, "const int32_t %s[] = {\n", prefixes_name.c_str());
  for (absl::btree_map<int, string>::const_iterator it = prefixes.begin();
       it != prefixes.end(); ++it) {
    fprintf(output, "  %d,\n", it->first);
    possible_lengths.insert(static_cast<int>(log10(it->first) + 1));
  }
  fprintf(output,
          "};\n"
          "\n");

  const string desc_name = var_name + "_descriptions";
  fprintf(output, "const char* %s[] = {\n", desc_name.c_str());
  for (absl::btree_map<int, string>::const_iterator it = prefixes.begin();
       it != prefixes.end(); ++it) {
    fprintf(output, "  ");
    WriteStringLiteral(it->second, output);
    fprintf(output, ",\n");
  }
  fprintf(output,
          "};\n"
          "\n");

  const string possible_lengths_name = var_name + "_possible_lengths";
  fprintf(output, "const int32_t %s[] = {\n ", possible_lengths_name.c_str());
  for (absl::btree_set<int>::const_iterator it = possible_lengths.begin();
       it != possible_lengths.end(); ++it) {
    fprintf(output, " %d,", *it);
  }
  fprintf(output,
          "\n"
          "};\n"
          "\n");

  WritePrefixDescriptionsDefinition(var_name, prefixes_name, desc_name,
                                    possible_lengths_name, output);
  fprintf(output, "\n");
}

// Writes a pair of arrays mapping prefix language code pairs to
// PrefixDescriptions instances. "prefix_var_names" maps language code pairs
// to prefix variable names.
//
// const char* prefix_language_code_pairs[] = {
//   "1_de",
//   "1_en",
// };
//
// const PrefixDescriptions* prefix_descriptions[] = {
//   &prefix_1_de,
//   &prefix_1_en,
// };
//
void WritePrefixesDescriptions(
    const absl::btree_map<string, string>& prefix_var_names, FILE* output) {
  fprintf(output, "const char* prefix_language_code_pairs[] = {\n");
  for (absl::btree_map<string, string>::const_iterator it = prefix_var_names.begin();
       it != prefix_var_names.end(); ++it) {
    fprintf(output, "  \"%s\",\n", it->first.c_str());
  }
  fprintf(output,
          "};\n"
          "\n"
          "const PrefixDescriptions* prefixes_descriptions[] = {\n");
  for (absl::btree_map<string, string>::const_iterator it = prefix_var_names.begin();
       it != prefix_var_names.end(); ++it) {
    fprintf(output, "  &%s,\n", it->second.c_str());
  }
  fprintf(output,
          "};\n"
          "\n");
}

// For each entry in "languages" mapping a country calling code to a set
// of available languages, writes a sorted array of languages, then wraps it
// into a CountryLanguages instance. Finally, writes a pair of arrays mapping
// country calling codes to CountryLanguages instances.
//
// const char* country_1[] = {
//   "de",
//   "en",
// };
//
// const CountryLanguages country_1_languages = {
//   country_1,
//   sizeof(country_1)/sizeof(*country_1),
// };
//
// [...]
//
// const CountryLanguages* country_languages[] = {
//   &country_1_languages,
//   [...]
// }
//
// const int country_calling_codes[] = {
//   1,
//   [...]
// };
//
bool WriteCountryLanguages(const map<int32, set<string> >& languages,
                           FILE* output) {
  vector<string> country_languages_vars;
  vector<string> countries;
  for (map<int32, set<string> >::const_iterator it = languages.begin();
       it != languages.end(); ++it) {
    string country_code;
    if (!IntToStr(it->first, &country_code)) {
      return false;
    }
    const string country_var = "country_" + country_code;
    fprintf(output, "const char* %s[] = {\n", country_var.c_str());
    for (set<string>::const_iterator it_lang = it->second.begin();
         it_lang != it->second.end(); ++it_lang) {
      fprintf(output, "  \"%s\",\n", it_lang->c_str());
    }
    fprintf(output,
            "};\n"
            "\n");

    const string country_languages_var = country_var + "_languages";
    fprintf(output, "const CountryLanguages %s = {\n",
            country_languages_var.c_str());
    WriteArrayAndSize(country_var, output);
    fprintf(output,
            "};\n"
            "\n");
    country_languages_vars.push_back(country_languages_var);
    countries.push_back(country_code);
  }

  fprintf(output,
          "\n"
          "const CountryLanguages* countries_languages[] = {\n");
  for (vector<string>::const_iterator
       it_languages_var = country_languages_vars.begin();
       it_languages_var != country_languages_vars.end(); ++it_languages_var) {
    fprintf(output, "  &%s,\n", it_languages_var->c_str());
  }
  fprintf(output,
          "};\n"
          "\n"
          "const int country_calling_codes[] = {\n");
  for (vector<string>::const_iterator it_country = countries.begin();
       it_country != countries.end(); ++it_country) {
    fprintf(output, "  %s,\n", it_country->c_str());
  }
  fprintf(output,
          "};\n"
          "\n");
  return true;
}

// Returns a copy of input where all occurences of pattern are replaced with
// value. If pattern is empty, input is returned unchanged.
string ReplaceAll(const string& input, const string& pattern,
                  const string& value) {
  if (pattern.size() == 0) {
    return input;
  }
  string replaced;
  std::back_insert_iterator<string> output = std::back_inserter(replaced);
  string::const_iterator begin = input.begin(), end = begin;
  while (true) {
    const size_t pos = input.find(pattern, begin - input.begin());
    if (pos == string::npos) {
      std::copy(begin, input.end(), output);
      break;
    }
    end = input.begin() + pos;
    std::copy(begin, end, output);
    std::copy(value.begin(), value.end(), output);
    begin = end + pattern.length();
  }
  return replaced;
}

// Writes data accessor definitions, prefixed with "accessor_prefix".
void WriteAccessorsDefinitions(const string& accessor_prefix, FILE* output) {
  string templ =
      "const int* get$prefix$_country_calling_codes() {\n"
      "  return country_calling_codes;\n"
      "}\n"
      "\n"
      "int get$prefix$_country_calling_codes_size() {\n"
      "  return sizeof(country_calling_codes)\n"
      "      /sizeof(*country_calling_codes);\n"
      "}\n"
      "\n"
      "const CountryLanguages* get$prefix$_country_languages(int index) {\n"
      "  return countries_languages[index];\n"
      "}\n"
      "\n"
      "const char** get$prefix$_prefix_language_code_pairs() {\n"
      "  return prefix_language_code_pairs;\n"
      "}\n"
      "\n"
      "int get$prefix$_prefix_language_code_pairs_size() {\n"
      "  return sizeof(prefix_language_code_pairs)\n"
      "      /sizeof(*prefix_language_code_pairs);\n"
      "}\n"
      "\n"
      "const PrefixDescriptions* get$prefix$_prefix_descriptions(int index) {\n"
      "  return prefixes_descriptions[index];\n"
      "}\n";
  string defs = ReplaceAll(templ, "$prefix$", accessor_prefix);
  fprintf(output, "%s", defs.c_str());
}

// Writes geocoding data .cc file. "data_path" is the path of geocoding textual
// data directory. "base_name" is the base name of the .h/.cc pair, like
// "geocoding_data".
bool WriteSource(const string& data_path, const string& base_name,
                 const string& accessor_prefix, FILE* output) {
  WriteLicense(output);
  WriteCppHeader(base_name, output);
  WriteNSHeader(output);
  fprintf(output,
          "namespace {\n"
          "\n");

  // Enumerate language/script directories.
  absl::btree_map<string, string> prefix_vars;
  map<int32, set<string> > country_languages;
  vector<DirEntry> entries;
  if (!ListDirectory(data_path, &entries)) {
    fprintf(stderr, "failed to read directory entries");
    return false;
  }
  for (vector<DirEntry>::const_iterator it = entries.begin();
       it != entries.end(); ++it) {
    if (it->kind() != kDirectory) {
      continue;
    }
    // Enumerate country calling code files.
    const string dir_path = data_path + "/" + it->name();
    vector<DirEntry> files;
    if (!ListDirectory(dir_path, &files)) {
      fprintf(stderr, "failed to read file entries\n");
      return false;
    }
    for (vector<DirEntry>::const_iterator it_files = files.begin();
         it_files != files.end(); ++it_files) {
      const string fname = it_files->name();
      if (!EndsWith(fname, ".txt")) {
       continue;
      }
      int32 country_code;
      const string country_code_str = fname.substr(0, fname.length() - 4);
      if (!StrToInt(country_code_str, &country_code)) {
        return false;
      }
      const string path = dir_path + "/" + fname;

      absl::btree_map<int32, string> prefixes;
      if (!ParsePrefixes(path, &prefixes)) {
        return false;
      }

      const string prefix_var = "prefix_" + country_code_str + "_" + it->name();
      WritePrefixDescriptions(prefix_var, prefixes, output);
      prefix_vars[country_code_str + "_" + it->name()] = prefix_var;
      country_languages[country_code].insert(it->name());
    }
  }
  WritePrefixesDescriptions(prefix_vars, output);
  if (!WriteCountryLanguages(country_languages, output)) {
    return false;
  }
  fprintf(output, "}  // namespace\n");
  fprintf(output, "\n");
  WriteAccessorsDefinitions(accessor_prefix, output);
  WriteNSFooter(output);
  return ferror(output) == 0;
}

int PrintHelp(const string& message) {
  fprintf(stderr, "error: %s\n", message.c_str());
  fprintf(stderr, "generate_geocoding_data DATADIR CCPATH");
  return 1;
}

int Main(int argc, const char* argv[]) {
  if (argc < 2) {
    return PrintHelp("geocoding data root directory expected");
  }
  if (argc < 3) {
    return PrintHelp("output source path expected");
  }
  string accessor_prefix = "";
  if (argc > 3) {
    accessor_prefix = argv[3];
  }
  const string root_path(argv[1]);
  string source_path(argv[2]);
  std::replace(source_path.begin(), source_path.end(), '\\', '/');
  string base_name = source_path;
  if (base_name.rfind('/') != string::npos) {
    base_name = base_name.substr(base_name.rfind('/') + 1);
  }
  base_name = base_name.substr(0, base_name.rfind('.'));

  FILE* source_fp = fopen(source_path.c_str(), "w");
  if (!source_fp) {
    fprintf(stderr, "failed to open %s\n", source_path.c_str());
    return 1;
  }
  AutoCloser<FILE> source_closer(&source_fp, fclose);
  if (!WriteSource(root_path, base_name, accessor_prefix,
                   source_fp)) {
    return 1;
  }
  return 0;
}

}  // namespace phonenumbers
}  // namespace i18n
