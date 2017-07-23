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

import com.google.i18n.phonenumbers.CppMetadataGenerator.Type;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class generates the C++ code representation of the provided XML metadata file. It lets us
 * embed metadata directly in a native binary. We link the object resulting from the compilation of
 * the code emitted by this class with the C++ phonenumber library.
 *
 * @author Philippe Liard
 * @author David Beaumont
 */
public class BuildMetadataCppFromXml extends Command {

  /** An enum encapsulating the variations of metadata that we can produce. */
  public enum Variant {
    /** The default 'full' variant which contains all the metadata. */
    FULL("%s"),
    /** The test variant which contains fake data for tests. */
    TEST("test_%s"),
    /**
     * The lite variant contains the same metadata as the full version but excludes any example
     * data. This is typically used for clients with space restrictions.
     */
    LITE("lite_%s");

    private final String template;

    private Variant(String template) {
      this.template = template;
    }

    /**
     * Returns the basename of the type by adding the name of the current variant. The basename of
     * a Type is used to determine the name of the source file in which the metadata is defined.
     *
     * <p>Note that when the variant is {@link Variant#FULL} this method just returns the type name.
     */
    public String getBasename(Type type) {
      return String.format(template, type);
    }

    /**
     * Parses metadata variant name. By default (for a name of {@code ""} or {@code null}) we return
     * {@link Variant#FULL}, otherwise we match against the variant name (either "test" or "lite").
     */
    public static Variant parse(String variantName) {
      if ("test".equalsIgnoreCase(variantName)) {
        return Variant.TEST;
      } else if ("lite".equalsIgnoreCase(variantName)) {
        return Variant.LITE;
      } else if (variantName == null || variantName.length() == 0) {
        return Variant.FULL;
      } else {
        return null;
      }
    }
  }

  /**
   * An immutable options class for parsing and representing the command line options for this
   * command.
   */
  // @VisibleForTesting
  static final class Options {
    private static final Pattern BASENAME_PATTERN =
        Pattern.compile("(?:(test|lite)_)?([a-z_]+)");

    public static Options parse(String commandName, String[] args) {
      if (args.length == 4) {
        String inputXmlFilePath = args[1];
        String outputDirPath = args[2];
        Matcher basenameMatcher = BASENAME_PATTERN.matcher(args[3]);
        if (basenameMatcher.matches()) {
          Variant variant = Variant.parse(basenameMatcher.group(1));
          Type type = Type.parse(basenameMatcher.group(2));
          if (type != null && variant != null) {
            return new Options(inputXmlFilePath, outputDirPath, type, variant);
          }
        }
      }
      throw new IllegalArgumentException(String.format(
          "Usage: %s <inputXmlFile> <outputDir> ( <type> | test_<type> | lite_<type> )\n" +
          "       where <type> is one of: %s",
          commandName, Arrays.asList(Type.values())));
    }

    // File path where the XML input can be found.
    private final String inputXmlFilePath;
    // Output directory where the generated files will be saved.
    private final String outputDirPath;
    private final Type type;
    private final Variant variant;

    private Options(String inputXmlFilePath, String outputDirPath, Type type, Variant variant) {
      this.inputXmlFilePath = inputXmlFilePath;
      this.outputDirPath = outputDirPath;
      this.type = type;
      this.variant = variant;
    }

    public String getInputFilePath() {
      return inputXmlFilePath;
    }

    public String getOutputDir() {
      return outputDirPath;
    }

    public Type getType() {
      return type;
    }

    public Variant getVariant() {
      return variant;
    }
  }

  @Override
  public String getCommandName() {
    return "BuildMetadataCppFromXml";
  }

  /**
   * Generates C++ header and source files to represent the metadata specified by this command's
   * arguments. The metadata XML file is read and converted to a byte array before being written
   * into a C++ source file as a static data array.
   *
   * @return  true if the generation succeeded.
   */
  @Override
  public boolean start() {
    try {
      Options opt = Options.parse(getCommandName(), getArgs());
      byte[] data = loadMetadataBytes(opt.getInputFilePath(), opt.getVariant() == Variant.LITE);
      CppMetadataGenerator metadata = CppMetadataGenerator.create(opt.getType(), data);

      // TODO: Consider adding checking for correctness of file paths and access.
      OutputStream headerStream = null;
      OutputStream sourceStream = null;
      try {
        File dir = new File(opt.getOutputDir());
        headerStream = openHeaderStream(dir, opt.getType());
        sourceStream = openSourceStream(dir, opt.getType(), opt.getVariant());
        metadata.outputHeaderFile(new OutputStreamWriter(headerStream, UTF_8));
        metadata.outputSourceFile(new OutputStreamWriter(sourceStream, UTF_8));
      } finally {
        FileUtils.closeFiles(headerStream, sourceStream);
      }
      return true;
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (RuntimeException e) {
      System.err.println(e.getMessage());
    }
    return false;
  }

  /** Loads the metadata XML file and converts its contents to a byte array. */
  private byte[] loadMetadataBytes(String inputFilePath, boolean liteMetadata) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      writePhoneMetadataCollection(inputFilePath, liteMetadata, out);
    } catch (Exception e) {
      // We cannot recover from any exceptions thrown here, so promote them to runtime exceptions.
      throw new RuntimeException(e);
    } finally {
      FileUtils.closeFiles(out);
    }
    return out.toByteArray();
  }

  // @VisibleForTesting
  void writePhoneMetadataCollection(
      String inputFilePath, boolean liteMetadata, OutputStream out) throws IOException, Exception {
    BuildMetadataFromXml.buildPhoneMetadataCollection(inputFilePath, liteMetadata, false)
        .writeTo(out);
  }

  // @VisibleForTesting
  OutputStream openHeaderStream(File dir, Type type) throws FileNotFoundException {
    return new FileOutputStream(new File(dir, type + ".h"));
  }

  // @VisibleForTesting
  OutputStream openSourceStream(File dir, Type type, Variant variant) throws FileNotFoundException {
    return new FileOutputStream(new File(dir, variant.getBasename(type) + ".cc"));
  }

  /** The charset in which our source and header files will be written. */
  private static final Charset UTF_8 = Charset.forName("UTF-8");
}
