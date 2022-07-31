package eu.europa.ted.efx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.selector.component.EfxTranslatorFactory;

public class EfxTranslator {
  enum KnownSdkVersions {
    SDK_0_6("0.6"), SDK_0_7("0.7"), SDK_1_0("1.0"), UNSUPPORTED("N/A");

    private String name;

    private KnownSdkVersions(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  public static String translateExpression(final String context, final String expression,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion)
      throws InstantiationException {
    return EfxTranslatorFactory
        .getEfxExpressionTranslator(normalizeVersion(sdkVersion).getName(), dependencyFactory)
        .translateExpression(context, expression);
  }

  public static String translateTemplate(final Path pathname,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(normalizeVersion(sdkVersion).getName(), dependencyFactory)
        .renderTemplate(pathname);
  }

  public static String translateTemplate(final String template,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion)
      throws InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(normalizeVersion(sdkVersion).getName(), dependencyFactory)
        .renderTemplate(template);
  }

  public static String translateTemplate(final InputStream stream,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(normalizeVersion(sdkVersion).getName(), dependencyFactory)
        .renderTemplate(stream);
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
