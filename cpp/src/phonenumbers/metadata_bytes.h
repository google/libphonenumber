/*
 * Copyright (C) 2026 The Libphonenumber Authors
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

#ifndef I18N_PHONENUMBERS_METADATA_BYTES_H_
#define I18N_PHONENUMBERS_METADATA_BYTES_H_

#include <cstdint>
#include <memory>

namespace i18n {
namespace phonenumbers {

// Owning-or-borrowed view over a serialized PhoneMetadataCollection byte
// buffer returned by the GetMetadata(), GetShortMetadata() and
// GetAlternateFormat() accessors.
//
// The default implementations shipped with libphonenumber return a
// non-owning instance that points at a `static const unsigned char data[]`
// array compiled into the binary, so behaviour and storage are unchanged
// for existing callers.
//
// Embedders that produce the metadata at runtime (for example by
// decompressing it on first use) can return an owning instance constructed
// from a `std::unique_ptr<uint8_t[]>`; the buffer is released when the
// MetadataBytes goes out of scope. The typical call shape
//
//     MetadataBytes bytes = GetMetadata();
//     collection->ParseFromArray(bytes.data(), bytes.size());
//
// keeps the buffer alive for exactly as long as ParseFromArray() needs it
// and then frees it.
class MetadataBytes {
 public:
  // Non-owning wrapper around `data` of `size` bytes. `data` must outlive
  // this object; pass a pointer to a static array.
  MetadataBytes(const void* data, int size) : data_(data), size_(size) {}

  // Owning wrapper. The buffer is deleted when this object is destroyed.
  MetadataBytes(std::unique_ptr<uint8_t[]> data, int size)
      : data_(data.get()), size_(size), owned_(std::move(data)) {}

  MetadataBytes(MetadataBytes&&) noexcept = default;
  MetadataBytes& operator=(MetadataBytes&&) noexcept = default;

  MetadataBytes(const MetadataBytes&) = delete;
  MetadataBytes& operator=(const MetadataBytes&) = delete;

  ~MetadataBytes() = default;

  const void* data() const { return data_; }
  int size() const { return size_; }

 private:
  const void* data_;
  int size_;
  std::unique_ptr<uint8_t[]> owned_;
};

}  // namespace phonenumbers
}  // namespace i18n

#endif  // I18N_PHONENUMBERS_METADATA_BYTES_H_
