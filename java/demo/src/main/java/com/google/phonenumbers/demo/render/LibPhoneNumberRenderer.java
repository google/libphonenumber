/*
 * Copyright (C) 2022 The Libphonenumber Authors
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
 *
 * @author Tobias Rogg
 */

package com.google.phonenumbers.demo.render;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyTemplate;
import com.google.template.soy.tofu.SoyTofu;
import java.net.URL;

public abstract class LibPhoneNumberRenderer<T extends SoyTemplate> {
  private final String template;
  private final String namespace;

  public LibPhoneNumberRenderer(String template, String namespace) {
    this.template = template;
    this.namespace = namespace;
  }

  private URL getResource() {
    return LibPhoneNumberRenderer.class.getResource("../" + template);
  }

  String render(T soyTemplate) {
    SoyFileSet sfs = SoyFileSet.builder().add(getResource()).build();
    SoyTofu tofu = sfs.compileToTofu();
    // For convenience, create another SoyTofu object that has a
    // namespace specified, so you can pass partial template names to
    // the newRenderer() method.
    SoyTofu simpleTofu = tofu.forNamespace(namespace);
    return simpleTofu.newRenderer(soyTemplate).render();
  }

  public abstract String genHtml();
}
