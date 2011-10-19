/*
 *  Copyright (C) 2011 The Libphonenumber Authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.i18n.phonenumbers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Tests the BuildMetadataCppFromXml implementation to make sure it emits the expected code.
 */
public class BuildMetadataCppFromXmlTest {

  @Test
  public void emitStaticArrayCode() {
    final int streamSize = 4;

    try {
      ByteArrayOutputStream stream = new ByteArrayOutputStream(streamSize);

      stream.write(0xca);
      stream.write(0xfe);
      stream.write(0xba);
      stream.write(0xbe);

      ByteArrayOutputStream result = new ByteArrayOutputStream(streamSize);
      PrintWriter printWriter = new PrintWriter(result);

      BuildMetadataCppFromXml buildMetadataCppFromXml = new BuildMetadataCppFromXml();
      buildMetadataCppFromXml.setBinaryStream(stream);
      buildMetadataCppFromXml.emitStaticArrayCode(printWriter);

      assertEquals("\n  0xCA, 0xFE, 0xBA, 0xBE\n", result.toString());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }
}
