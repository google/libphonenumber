/*
 *  Copyright (C) 2011 Google Inc.
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

package com.google.i18n.phonenumbers.tools;

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadataCollection;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This class generates the C++ code representation of the provided XML metadata file. It lets us
 * embed metadata directly in a native binary. We link the object resulting from the compilation of
 * the code emitted by this class with the C++ phonenumber library.
 *
 * @author Philippe Liard
 */
public class BuildMetadataCppFromXml extends Command {
  // File path where the XML input can be found.
  private String inputFilePath;
  // Output directory where the generated files will be saved.
  private String outputDir;
  // Either test_metadata or metadata.{cc,hh} depending on the value of the 'forTesting' command
  // line parameter.
  private String baseFilename;

  // The binary translation of the XML file is directly written to a byte array output stream
  // instead of creating an unnecessary file on the filesystem.
  private ByteArrayOutputStream binaryStream = new ByteArrayOutputStream();

  // Header (.h) file and implementation (.cc) file output streams.
  private FileOutputStream headerFileOutputStream;
  private FileOutputStream implFileOutputStream;

  /**
   * Package private setter used to inject the binary stream for testing purpose.
   */
  void setBinaryStream(ByteArrayOutputStream stream) {
    this.binaryStream = stream;
  }

  @Override
  public String getCommandName() {
    return "BuildMetadataCppFromXml";
  }

  /**
   * Starts the generation of the code. First it checks parameters from command line. Then it opens
   * all the streams (input and output streams), emits the header and implementation code and
   * finally closes all the streams.
   *
   * @return  true if the generation succeeded.
   */
  @Override
  public boolean start() {
    if (!parseCommandLine()) {
      return false;
    }
    try {
      generateBinaryFromXml();
      openFiles();
      emitHeader();
      emitImplementation();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return false;
    } finally {
      FileUtils.closeFiles(headerFileOutputStream, implFileOutputStream);
    }
    return true;
  }

  private void generateBinaryFromXml() throws Exception {
    PhoneMetadataCollection collection =
        BuildMetadataFromXml.buildPhoneMetadataCollection(inputFilePath, false);
    collection.writeTo(binaryStream);
  }

  /**
   * Opens the binary file input stream and the two file output streams used to emit header and
   * implementation code.
   */
  private void openFiles() throws IOException {
    headerFileOutputStream = new FileOutputStream(
        String.format("%s/%s.h", outputDir, baseFilename));
    implFileOutputStream = new FileOutputStream(String.format("%s/%s.cc", outputDir, baseFilename));
  }

  /**
   * Generates the header file containing the two function prototypes:
   * <pre>
   *   int X_size();
   *   const void* X_get();
   * </pre>
   *
   * with X: 'metadata' or 'test_metadata'.
   */
  private void emitHeader() {
    final PrintWriter pw = new PrintWriter(headerFileOutputStream);
    pw.write(CopyrightNotice.TEXT);
    final String guardName = String.format("EMBEDDED_DATA_%s_H_", baseFilename.toUpperCase());
    pw.println("#ifndef " + guardName);
    pw.println("#define " + guardName);

    pw.println();
    pw.println(String.format("int %s_size();", baseFilename));
    pw.println(String.format("const void* %s_get();", baseFilename));
    pw.println();

    pw.println("#endif  // " + guardName);
    pw.close();
  }

  /**
   * The next two methods generate the implementation file (.cc) containing the file data and the
   * two function implementations:
   *
   * <pre>
   * #include "X.h"
   *
   * static const unsigned char[] X_data = { .... };
   *
   * const void* X_get() {
   *   return X_data;
   * }
   *
   * unsigned int X_size() {
   *   return sizeof(X_data) / sizeof(X_data[0]);
   * }
   * </pre>
   */

  /**
   * Emits the C++ code implementation (.cc file) corresponding to the provided XML input file.
   */
  private void emitImplementation() throws IOException {
    final PrintWriter pw = new PrintWriter(implFileOutputStream);
    pw.write(CopyrightNotice.TEXT);
    pw.println(String.format("#include \"%s.h\"", baseFilename));
    pw.println();
    pw.print(String.format("static const unsigned char %s_data[] = { ", baseFilename));
    emitStaticArrayCode(pw);
    pw.println(" };");

    pw.println();
    pw.println(String.format("int %s_size() {", baseFilename));
    pw.println(String.format("  return sizeof(%s_data) / sizeof(%s_data[0]);",
                             baseFilename, baseFilename));
    pw.println("}");

    pw.println();
    pw.println(String.format("const void* %s_get() {", baseFilename));
    pw.println(String.format("  return %s_data;", baseFilename));
    pw.println("}");
    pw.close();
  }

  /**
   * Emits the C++ code corresponding to the provided XML input file into a static byte array.
   */
  void emitStaticArrayCode(PrintWriter pw) throws IOException {
    byte[] buf = binaryStream.toByteArray();

    for (int i = 0; i < buf.length; i++) {
      pw.printf("0x%02X, ", buf[i]);
    }
    pw.flush();
    binaryStream.flush();
    binaryStream.close();
  }

  private boolean parseCommandLine() {
    final String[] args = getArgs();

    if (args.length != 4) {
      System.err.println(String.format("Usage: %s <inputXmlFile> <outputDir> <forTesting>",
                                       getCommandName()));
      return false;
    }
    // args[0] is the name of the command.
    inputFilePath = args[1];
    outputDir = args[2];
    baseFilename = Boolean.parseBoolean(args[3]) ? "test_metadata" : "metadata";

    return true;
  }
}
