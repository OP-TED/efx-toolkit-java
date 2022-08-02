package eu.europa.ted.efx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.selector.component.EfxTranslatorFactory;

public class EfxTranslator {
  public static String translateExpression(final String context, final String expression,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion)
      throws InstantiationException {
    return EfxTranslatorFactory
        .getEfxExpressionTranslator(sdkVersion, dependencyFactory)
        .translateExpression(context, expression);
  }

  public static String translateTemplate(final Path pathname,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, dependencyFactory)
        .renderTemplate(pathname);
  }

  public static String translateTemplate(final String template,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion)
      throws InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, dependencyFactory)
        .renderTemplate(template);
  }

  public static String translateTemplate(final InputStream stream,
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, dependencyFactory)
        .renderTemplate(stream);
  }
}
