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

import java.io.IOException;
import java.io.Writer;
import java.util.Formatter;

/**
 * Class containing the Apache copyright notice used by code generators.
 *
 * @author Philippe Liard
 */
public class CopyrightNotice {

  private static final String TEXT_OPENING =
    "/*\n";

  private static final String TEXT_OPENING_FOR_JAVASCRIPT =
    "/**\n" +
    " * @license\n";

  private static final String TEXT =
    " * Copyright (C) %d The Libphonenumber Authors\n" +
    " *\n" +
    " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
    " * you may not use this file except in compliance with the License.\n" +
    " * You may obtain a copy of the License at\n" +
    " *\n" +
    " * http://www.apache.org/licenses/LICENSE-2.0\n" +
    " *\n" +
    " * Unless required by applicable law or agreed to in writing, software\n" +
    " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
    " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
    " * See the License for the specific language governing permissions and\n" +
    " * limitations under the License.\n" +
    " */\n\n";

  static final void writeTo(Writer writer, int year) throws IOException {
    writeTo(writer, year, false);
  }

  static final void writeTo(Writer writer, int year, boolean isJavascript) throws IOException {
    if (isJavascript) {
      writer.write(TEXT_OPENING_FOR_JAVASCRIPT);
    } else {
      writer.write(TEXT_OPENING);
    }
    Formatter formatter = new Formatter(writer);
    formatter.format(TEXT, year);
  }
}
