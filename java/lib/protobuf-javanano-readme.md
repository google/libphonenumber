Steps to update the protobuf library version:

- Start by finding a protobuf library version released to Maven Central that has functionality you
  need, e.g. nano. See http://mvnrepository.com/artifact/com.google.protobuf.
- Update the protobuf library dependency's `groupId`, `artifactId`, and `version` to match this
  artifact in all the `pom.xml` files. You can find the files by searching for
  `<groupId>com.google.protobuf` in the GitHub repository.
- Download the `.jar` from the Maven Repository,
  e.g. from http://repo1.maven.org/maven2/com/google/protobuf/nano/protobuf-javanano/3.0.0-alpha-7/,
  and copy to `java/lib/`, replacing the previous version's `.jar`.
- See https://github.com/google/protobuf/releases for the release containing the chosen version, and
  download the `protoc` binary for your machine's architecture from the same release and use this to
  re-generate `java/libphonenumber/src/com/google/i18n/phonenumbers/nano/Phonemetadata.java` and
  check in any changes to this file. The command from the root directory is:

  ```
  protoc --javanano_out=java/libphonenumber/src \
         resources/phonemetadata.proto \
         --proto_path=resources
  ```
