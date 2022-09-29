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
