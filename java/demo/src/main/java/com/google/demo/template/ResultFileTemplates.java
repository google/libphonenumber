package com.google.demo.template;

/**
 * Wrapper class containing {@link com.google.template.soy.data.SoyTemplate} builders for each
 * template in: result_file.soy.
 */
@javax.annotation.Generated("com.google.template.soy.SoyParseInfoGenerator")
public final class ResultFileTemplates {

  private static final com.google.common.collect.ImmutableMap<java.lang.String, java.lang.String>
      __PROVIDED_CSS_MAP__ =
          com.google.common.collect.ImmutableMap.<java.lang.String, java.lang.String>of();

  private static final com.google.common.collect.ImmutableList<java.lang.String> __PROVIDED_CSS__ =
      com.google.common.collect.ImmutableList.<java.lang.String>of();

  public static final class File extends com.google.template.soy.data.BaseSoyTemplateImpl {

    public static final com.google.template.soy.data.SoyTemplateParam<java.lang.Object>
        CSP_STYLE_NONCE =
            com.google.template.soy.data.SoyTemplateParam.injected(
                "csp_style_nonce",
                /* required= */ false,
                com.google.common.reflect.TypeToken.of(java.lang.Object.class));
    private static final java.lang.String __NAME__ = "demo.output.file";
    private static final com.google.template.soy.data.SoyTemplateParam<?> ROWS =
        com.google.template.soy.data.SoyTemplateParam.standard(
            "rows",
            /* required= */ true,
            com.google.common.reflect.TypeToken.of(java.lang.Object.class));
    private static final com.google.common.collect.ImmutableSet<
            com.google.template.soy.data.SoyTemplateParam<?>>
        __PARAMS__ = com.google.common.collect.ImmutableSet.of(ROWS);

    private File(
        com.google.common.collect.ImmutableMap<
                java.lang.String, com.google.template.soy.data.SoyValueProvider>
            data) {
      super(data);
    }

    public static com.google.template.soy.data.SoyTemplate.AsyncWrapper<File> wrapFuture(
        com.google.common.util.concurrent.ListenableFuture<File> paramsFuture) {
      return new com.google.template.soy.data.SoyTemplate.AsyncWrapper<>(__NAME__, paramsFuture);
    }

    public static Builder builder() {
      return new Builder();
    }

    @java.lang.Override
    public final java.lang.String getTemplateName() {
      return __NAME__;
    }

    public static final class Builder
        extends com.google.template.soy.data.BaseSoyTemplateImpl
                .AbstractBuilderWithAccumulatorParameters<
            Builder, File> {

      private Builder() {
        super(1);
        initListParam(ROWS);
      }

      @java.lang.Override
      protected com.google.common.collect.ImmutableSet<
              com.google.template.soy.data.SoyTemplateParam<?>>
          allParams() {
        return __PARAMS__;
      }

      @java.lang.Override
      protected File buildInternal(
          com.google.common.collect.ImmutableMap<
                  java.lang.String, com.google.template.soy.data.SoyValueProvider>
              data) {
        return new File(data);
      }

      @com.google.errorprone.annotations.CanIgnoreReturnValue
      public Builder addRows(
          long id,
          java.lang.String numberStr,
          @javax.annotation.Nullable java.lang.String prettyFormat,
          @javax.annotation.Nullable java.lang.String internationalFormat,
          @javax.annotation.Nullable java.lang.String error) {
        return addToListParam(
            ROWS,
            asRecord(
                "id",
                asInt(id),
                "numberStr",
                asString(numberStr),
                "prettyFormat",
                asNullableString(prettyFormat),
                "internationalFormat",
                asNullableString(internationalFormat),
                "error",
                asNullableString(error)));
      }
    }
  }
}
