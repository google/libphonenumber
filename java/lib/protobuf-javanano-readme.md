# How to regenerate `Phonemetadata.java`

You need to re-generate
`java/libphonenumber/src/com/google/i18n/phonenumbers/nano/Phonemetadata.java` if you update
`resources/phonemetadata.proto` or the protobuf version.

Here are convenience instructions to update `Phonemetadata.java`.

Checklist:

- Are we still depending on Maven's 3.0.0-alpha-7 version of com.google.protobuf.nano
  protobuf-javanano?
- Are you on a Linux x86_64 architecture? Check via the `arch` command in your terminal.

If you answered no to any question, refer instead to the full instructions, [How to update the
protobuf version](#how-to-update-the-protobuf-version).

```
mkdir /tmp/custom_protoc/ && \
cd /tmp/custom_protoc/ && \
wget https://github.com/google/protobuf/releases/download/v3.0.0-beta-4/protoc-3.0.0-beta-4-linux-x86_64.zip && \
unzip protoc-3.0.0-beta-4-linux-x86_64.zip
```

Then go to the root of your libphonenumber git repository and run:

```
/tmp/custom_protoc/bin/protoc --javanano_out=java/libphonenumber/src \
  resources/phonemetadata.proto \
  --proto_path=resources
```

# How to update the protobuf version

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
- Update the convenience instructions, [How to regenerate `Phonemetadata.java`]
  (#how-to-regenerate-phonemetadatajava).
