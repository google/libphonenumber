/*
 * Copyright (C) 2012 The Libphonenumber Authors
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

package com.google.i18n.phonenumbers.buildtools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Implementation of the AbstractPhonePrefixDataIOHandler required by the GeneratePhonePrefixData
 * class used here to create the output files and add them to the resulting JAR.
 */
public class JarPhonePrefixDataIOHandler extends AbstractPhonePrefixDataIOHandler {

  // Base name of the output JAR files. It also forms part of the name of the package
  // containing the generated binary data.
  private final String jarBase;
  // The path to the output directory.
  private final File outputPath;
  // The JAR output stream used by the JarPhonePrefixDataIOHandler.
  private final JarOutputStream jarOutputStream;
  // The package that will be used to create the JAR entry file.
  private final Package outputPackage;

  public JarPhonePrefixDataIOHandler(File outputPath, String outputName, Package outputPackage)
      throws IOException {
    if (outputPath.exists()) {
      if (!outputPath.isDirectory()) {
        throw new IOException("Expected directory: " + outputPath.getAbsolutePath());
      }
    } else {
      if (!outputPath.mkdirs()) {
        throw new IOException("Could not create directory " + outputPath.getAbsolutePath());
      }
    }
    this.outputPath = outputPath;
    this.jarBase = outputName;
    this.outputPackage = outputPackage;
    jarOutputStream = createJar();
  }

  private JarOutputStream createJar() throws IOException {
    Manifest manifest = new java.util.jar.Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    return new JarOutputStream(new FileOutputStream(new File(outputPath, jarBase + ".jar")));
  }

  /**
   * Adds the provided file to the created JAR.
   */
  @Override
  public void addFileToOutput(File file) throws IOException {
    JarEntry entry =
        new JarEntry(
            outputPackage.getName().replace('.', '/')
                + String.format("/%s/", jarBase)
                + file.getPath());
    entry.setTime(file.lastModified());
    jarOutputStream.putNextEntry(entry);
    BufferedInputStream bufferedInputStream = null;

    try {
      bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
      byte[] buffer = new byte[4096];

      for (int read; (read = bufferedInputStream.read(buffer)) > 0; ) {
        jarOutputStream.write(buffer, 0, read);
      }
      if (!file.delete()) {
        throw new IOException("Could not delete: " + file.getAbsolutePath());
      }
    } finally {
      jarOutputStream.closeEntry();
      closeFile(bufferedInputStream);
    }
  }

  @Override
  public File createFile(String path) {
    return new File(path);
  }

  @Override
  public void close() {
    closeFile(jarOutputStream);
  }
}
