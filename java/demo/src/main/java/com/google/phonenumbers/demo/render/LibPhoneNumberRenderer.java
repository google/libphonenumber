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
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletContext;

public abstract class LibPhoneNumberRenderer<T extends SoyTemplate> {
  private final String template;
  private final String namespace;
  private final ServletContext servletContext;

  public LibPhoneNumberRenderer(String template, String namespace, ServletContext servletContext) {
    this.template = template;
    this.namespace = namespace;
    this.servletContext = servletContext;
  }

  private URL getResource() throws MalformedURLException {
    return servletContext.getResource(
        "src/main/resources/com/google/phonenumbers/demo/" + template);
  }

  String render(T soyTemplate) throws MalformedURLException {
    SoyFileSet sfs = SoyFileSet.builder().add(getResource()).build();
    SoyTofu tofu = sfs.compileToTofu();
    // For convenience, create another SoyTofu object that has a
    // namespace specified, so you can pass partial template names to
    // the newRenderer() method.
    SoyTofu simpleTofu = tofu.forNamespace(namespace);
    return simpleTofu.newRenderer(soyTemplate).render();
  }

  public abstract String genHtml() throws MalformedURLException;
}
