package com.google.phonenumbers.migrator;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetadataReaderTest {

  @Test
  public void testInvalidFileLocation() {
    String fileLocation = "invalid-zipfile-location";
    Assertions.assertThrows(IOException.class, () -> new MetadataReader(fileLocation));
  }
}