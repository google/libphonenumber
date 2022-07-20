package com.google.i18n.phonenumbers.metadata;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

public class PhoneMetadataCollectionUtil {

  public static InputStream toInputStream(PhoneMetadataCollection metadata) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    metadata.writeExternal(objectOutputStream);
    objectOutputStream.flush();
    InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    objectOutputStream.close();
    return inputStream;
  }
}
