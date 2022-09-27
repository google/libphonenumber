package com.google.phonenumbers.demo.helper;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyTemplate;
import com.google.template.soy.tofu.SoyTofu;
import java.net.URL;

public class LibPhoneNumberTemplate {
  private final String template;
  private final String namespace;
  private final SoyTemplate soyTemplate;

  public LibPhoneNumberTemplate(String template, String namespace, SoyTemplate soyTemplate) {
    this.template = template;
    this.namespace = namespace;
    this.soyTemplate = soyTemplate;
  }

  private static URL getResource(String template) {
    return LibPhoneNumberTemplate.class.getResource("../" + template);
  }

  public String render() {
    SoyFileSet sfs = SoyFileSet.builder().add(getResource(template)).build();
    SoyTofu tofu = sfs.compileToTofu();
    // For convenience, create another SoyTofu object that has a
    // namespace specified, so you can pass partial template names to
    // the newRenderer() method.
    SoyTofu simpleTofu = tofu.forNamespace(namespace);
    return simpleTofu.newRenderer(soyTemplate).render();
  }
}
