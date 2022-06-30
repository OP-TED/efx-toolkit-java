package eu.europa.ted.efx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;

public class EfxTranslator {

  private static final String SDK_VERSION_V_NOT_SUPPORTED = "SDK version '%s' not supported";

  enum KnownSdkVersions {
    SDK_0_6, SDK_0_7, UNSUPPORTED
  };

  public static String translateExpression(final String context, final String expression,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion) {
    switch (normalizeVersion(sdkVersion)) {
      case SDK_0_6:
        return eu.europa.ted.efx.sdk0.v6.EfxExpressionTranslator.translateExpression(context,
            expression, dependencyFactory.createSymbolResolver(sdkVersion),
            dependencyFactory.createScriptGenerator(), dependencyFactory.createErrorListener());
      case SDK_0_7:
        return eu.europa.ted.efx.sdk0.v7.EfxExpressionTranslator.translateExpression(context,
            expression, dependencyFactory.createSymbolResolver(sdkVersion),
            dependencyFactory.createScriptGenerator(), dependencyFactory.createErrorListener());
      default:
        throw new RuntimeException(String.format(SDK_VERSION_V_NOT_SUPPORTED, sdkVersion));
    }
  }

  public static String translateTemplate(final Path pathname,
      final TranslatorDependencyFactory factory, final String sdkVersion) throws IOException {

    switch (normalizeVersion(sdkVersion)) {
      case SDK_0_6:
        return eu.europa.ted.efx.sdk0.v6.EfxTemplateTranslator.renderTemplate(pathname, factory,
            sdkVersion);
      case SDK_0_7:
        return eu.europa.ted.efx.sdk0.v7.EfxTemplateTranslator.renderTemplate(pathname, factory,
            sdkVersion);
      default:
        throw new RuntimeException(String.format(SDK_VERSION_V_NOT_SUPPORTED, sdkVersion));
    }
  }

  public static String translateTemplate(final String template,
      final TranslatorDependencyFactory factory, final String sdkVersion) {

    switch (normalizeVersion(sdkVersion)) {
      case SDK_0_6:
        return eu.europa.ted.efx.sdk0.v6.EfxTemplateTranslator.renderTemplate(template, factory,
            sdkVersion);
      case SDK_0_7:
            return eu.europa.ted.efx.sdk0.v7.EfxTemplateTranslator.renderTemplate(template, factory,
                sdkVersion);
          default:
        throw new RuntimeException(String.format(SDK_VERSION_V_NOT_SUPPORTED, sdkVersion));
    }
  }

  public static String translateTemplate(final InputStream stream,
      final TranslatorDependencyFactory factory, final String sdkVersion) throws IOException {

    switch (normalizeVersion(sdkVersion)) {
      case SDK_0_6:
        return eu.europa.ted.efx.sdk0.v6.EfxTemplateTranslator.renderTemplate(stream, factory,
            sdkVersion);
      case SDK_0_7:
            return eu.europa.ted.efx.sdk0.v7.EfxTemplateTranslator.renderTemplate(stream, factory,
                sdkVersion);
          default:
        throw new RuntimeException(String.format(SDK_VERSION_V_NOT_SUPPORTED, sdkVersion));
    }
  }

  private static KnownSdkVersions normalizeVersion(final String sdkVersion) {
    String normalizedVersion = sdkVersion;

    if (normalizedVersion.startsWith("eforms-sdk-")) {
      normalizedVersion = normalizedVersion.substring(11);
    }
    String[] numbers = normalizedVersion.split("\\.", -2);
    normalizedVersion = "SDK_" + (numbers.length > 0 ? numbers[0] : "") + "_"
        + (numbers.length > 1 ? numbers[1] : "");

    try {
      return KnownSdkVersions.valueOf(normalizedVersion);
    } catch (IllegalArgumentException e) {
      return KnownSdkVersions.UNSUPPORTED;
    }
  }
}
