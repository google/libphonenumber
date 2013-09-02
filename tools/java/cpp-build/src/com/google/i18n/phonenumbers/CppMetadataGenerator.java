/*
 *  Copyright (C) 2012 The Libphonenumber Authors
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;

/**
 * Encapsulation of binary metadata created from XML to be included as static data in C++ source
 * files.
 *
 * @author David Beaumont
 * @author Philippe Liard
 */
public final class CppMetadataGenerator {

  /**
   * The metadata type represents the known types of metadata and includes additional information
   * such as the copyright year. It is expected that the generated files will be named after the
   * {@link #toString} of their type.
   */
  public enum Type {
    /** The basic phone number metadata (expected to be written to metadata.[h/cc]). */
    METADATA("metadata", 2011),
    /** The alternate format metadata (expected to be written to alternate_format.[h/cc]). */
    ALTERNATE_FORMAT("alternate_format", 2012),
    /** Metadata for short numbers (expected to be written to short_metadata.[h/cc]). */
    SHORT_NUMBERS("short_metadata", 2013);

    private final String typeName;
    private final int copyrightYear;

    private Type(String typeName, int copyrightYear) {
      this.typeName = typeName;
      this.copyrightYear = copyrightYear;
    }

    /** Returns the year in which this metadata type was first introduced. */
    public int getCopyrightYear() {
      return copyrightYear;
    }

    /**
     * Returns the name of this type for use in C++ source/header files. Use this in preference to
     * using {@link #name}.
     */
    @Override public String toString() {
      return typeName;
    }

    /**
     * Parses the type from a string case-insensitively.
     *
     * @return the matching Type instance or null if not matched.
     */
    public static Type parse(String typeName) {
      if (Type.METADATA.toString().equalsIgnoreCase(typeName)) {
        return Type.METADATA;
      } else if (Type.ALTERNATE_FORMAT.toString().equalsIgnoreCase(typeName)) {
        return Type.ALTERNATE_FORMAT;
      } else if (Type.SHORT_NUMBERS.toString().equalsIgnoreCase(typeName)) {
        return Type.SHORT_NUMBERS;
      } else {
        return null;
      }
    }
  }

  /**
   * Creates a metadata instance that can write C++ source and header files to represent this given
   * byte array as a static unsigned char array. Note that a direct reference to the byte[] is
   * retained by the newly created CppXmlMetadata instance, so the caller should treat the array as
   * immutable after making this call.
   */
  public static CppMetadataGenerator create(Type type, byte[] data) {
    return new CppMetadataGenerator(type, data);
  }

  private final Type type;
  private final byte[] data;
  private final String guardName;      // e.g. "I18N_PHONENUMBERS_<TYPE>_H_"
  private final String headerInclude;  // e.g. "phonenumbers/<type>.h"

  private CppMetadataGenerator(Type type, byte[] data) {
    this.type = type;
    this.data = data;
    this.guardName = createGuardName(type);
    this.headerInclude = createHeaderInclude(type);
  }

  /**
   * Writes the header file for the C++ representation of the metadata to the given writer. Note
   * that this method does not close the given writer.
   */
  public void outputHeaderFile(Writer out) throws IOException {
    PrintWriter pw = new PrintWriter(out);
    CopyrightNotice.writeTo(pw, type.getCopyrightYear());
    pw.println("#ifndef " + guardName);
    pw.println("#define " + guardName);
    pw.println();
    emitNamespaceStart(pw);
    pw.println();
    pw.println("int " + type + "_size();");
    pw.println("const void* " + type + "_get();");
    pw.println();
    emitNamespaceEnd(pw);
    pw.println();
    pw.println("#endif  // " + guardName);
    pw.flush();
  }

  /**
   * Writes the source file for the C++ representation of the metadata, including a static array
   * containing the data itself, to the given writer. Note that this method does not close the given
   * writer.
   */
  public void outputSourceFile(Writer out) throws IOException {
    // TODO: Consider outputting a load method to return the parsed proto directly.
    PrintWriter pw = new PrintWriter(out);
    CopyrightNotice.writeTo(pw, type.getCopyrightYear());
    pw.println("#include \"" + headerInclude + "\"");
    pw.println();
    emitNamespaceStart(pw);
    pw.println();
    pw.println("namespace {");
    pw.println("static const unsigned char data[] = {");
    emitStaticArrayData(pw, data);
    pw.println("};");
    pw.println("}  // namespace");
    pw.println();
    pw.println("int " + type + "_size() {");
    pw.println("  return sizeof(data) / sizeof(data[0]);");
    pw.println("}");
    pw.println();
    pw.println("const void* " + type + "_get() {");
    pw.println("  return data;");
    pw.println("}");
    pw.println();
    emitNamespaceEnd(pw);
    pw.flush();
  }

  private static String createGuardName(Type type) {
    return String.format("I18N_PHONENUMBERS_%s_H_", type.toString().toUpperCase(Locale.ENGLISH));
  }

  private static String createHeaderInclude(Type type) {
    return String.format("phonenumbers/%s.h", type);
  }

  private static void emitNamespaceStart(PrintWriter pw) {
    pw.println("namespace i18n {");
    pw.println("namespace phonenumbers {");
  }

  private static void emitNamespaceEnd(PrintWriter pw) {
    pw.println("}  // namespace phonenumbers");
    pw.println("}  // namespace i18n");
  }

  /** Emits the C++ code corresponding to the binary metadata as a static byte array. */
  // @VisibleForTesting
  static void emitStaticArrayData(PrintWriter pw, byte[] data) {
    String separator = "  ";
    for (int i = 0; i < data.length; i++) {
      pw.print(separator);
      emitHexByte(pw, data[i]);
      separator = ((i + 1) % 13 == 0) ? ",\n  " : ", ";
    }
    pw.println();
  }

  /** Emits a single byte in the form 0xHH, where H is an upper case hex digit in [0-9A-F]. */
  private static void emitHexByte(PrintWriter pw, byte v) {
    pw.print("0x");
    pw.print(UPPER_HEX[(v & 0xF0) >>> 4]);
    pw.print(UPPER_HEX[v & 0xF]);
  }

  private static final char[] UPPER_HEX = "0123456789ABCDEF".toCharArray();
}
