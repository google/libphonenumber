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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
  // 'metadata', 'test_metadata' or 'lite_metadata' depending on the value of the last command line
  // parameter.
  private String baseFilename;
  // Whether to generate "lite" metadata or not.
  private boolean liteMetadata;
  // The binary translation of the XML file is directly written to a byte array output stream
  // instead of creating an unnecessary file on the filesystem.
  private ByteArrayOutputStream binaryStream = new ByteArrayOutputStream();

  // Header (.h) file and implementation (.cc) file output streams.
  private FileOutputStream headerFileOutputStream;
  private FileOutputStream implFileOutputStream;

  private static final Set<String> METADATA_TYPES =
      new HashSet<String>(Arrays.asList("metadata", "test_metadata", "lite_metadata"));

  private static final int COPYRIGHT_YEAR = 2011;

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
        BuildMetadataFromXml.buildPhoneMetadataCollection(inputFilePath, liteMetadata);
    collection.writeTo(binaryStream);
  }

  /**
   * Opens the binary file input stream and the two file output streams used to emit header and
   * implementation code.
   */
  private void openFiles() throws IOException {
    headerFileOutputStream = new FileOutputStream(String.format("%s/metadata.h", outputDir));
    implFileOutputStream = new FileOutputStream(String.format("%s/%s.cc", outputDir, baseFilename));
  }

  private void emitNamespacesBeginning(PrintWriter pw) {
    pw.println("namespace i18n {");
    pw.println("namespace phonenumbers {");
  }

  private void emitNamespacesEnd(PrintWriter pw) {
    pw.println("}  // namespace phonenumbers");
    pw.println("}  // namespace i18n");
  }

  /**
   * Generates the header file containing the two function prototypes in namespace
   * i18n::phonenumbers.
   * <pre>
   *   int metadata_size();
   *   const void* metadata_get();
   * </pre>
   */
  private void emitHeader() throws IOException {
    final PrintWriter pw = new PrintWriter(headerFileOutputStream);
    CopyrightNotice.writeTo(pw, COPYRIGHT_YEAR);
    final String guardName = "I18N_PHONENUMBERS_METADATA_H_";
    pw.println("#ifndef " + guardName);
    pw.println("#define " + guardName);

    pw.println();
    emitNamespacesBeginning(pw);
    pw.println();

    pw.println("int metadata_size();");
    pw.println("const void* metadata_get();");
    pw.println();

    emitNamespacesEnd(pw);
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
   * namespace i18n {
   * namespace phonenumbers {
   *
   * namespace {
   *   const unsigned char[] data = { .... };
   * }  // namespace
   *
   * const void* metadata_get() {
   *   return data;
   * }
   *
   * int metadata_size() {
   *   return sizeof(data) / sizeof(data[0]);
   * }
   *
   * }  // namespace phonenumbers
   * }  // namespace i18n
   *
   * </pre>
   */

  /**
   * Emits the C++ code implementation (.cc file) corresponding to the provided XML input file.
   */
  private void emitImplementation() throws IOException {
    final PrintWriter pw = new PrintWriter(implFileOutputStream);
    CopyrightNotice.writeTo(pw, COPYRIGHT_YEAR);
    pw.println("#include \"phonenumbers/metadata.h\"");
    pw.println();

    emitNamespacesBeginning(pw);
    pw.println();

    pw.println("namespace {");
    pw.print("static const unsigned char data[] = {");
    emitStaticArrayCode(pw);
    pw.println("};");
    pw.println("}  // namespace");

    pw.println();
    pw.println("int metadata_size() {");
    pw.println("  return sizeof(data) / sizeof(data[0]);");
    pw.println("}");

    pw.println();
    pw.println("const void* metadata_get() {");
    pw.println("  return data;");
    pw.println("}");

    pw.println();
    emitNamespacesEnd(pw);

    pw.close();
  }

  /**
   * Emits the C++ code corresponding to the provided XML input file into a static byte array.
   */
  void emitStaticArrayCode(PrintWriter pw) throws IOException {
    byte[] buf = binaryStream.toByteArray();
    pw.print("\n  ");

    for (int i = 0; i < buf.length; i++) {
      String format = "0x%02X";

      if (i == buf.length - 1) {
        format += "\n";
      } else if ((i + 1) % 13 == 0) {  // 13 bytes per line to have lines of 79 characters.
        format += ",\n  ";
      } else {
        format += ", ";
      }
      pw.printf(format, buf[i]);
    }
    pw.flush();
    binaryStream.flush();
    binaryStream.close();
  }

  private boolean parseCommandLine() {
    final String[] args = getArgs();

    if (args.length != 4 || !METADATA_TYPES.contains(args[3])) {
      System.err.println(String.format(
          "Usage: %s <inputXmlFile> <outputDir> ( metadata | test_metadata | lite_metadata )",
          getCommandName()));
      return false;
    }
    // args[0] is the name of the command.
    inputFilePath = args[1];
    outputDir = args[2];
    baseFilename = args[3];
    liteMetadata = baseFilename.equals("lite_metadata");

    return true;
  }
}
