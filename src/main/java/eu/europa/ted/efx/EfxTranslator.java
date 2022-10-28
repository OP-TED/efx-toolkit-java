package eu.europa.ted.efx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import eu.europa.ted.efx.component.EfxTranslatorFactory;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;

public class EfxTranslator {
 
  public static String translateExpression(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String expression, final String... expressionParameters) throws InstantiationException {
    return EfxTranslatorFactory.getEfxExpressionTranslator(sdkVersion, dependencyFactory)
        .translateExpression(expression, expressionParameters);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion, 
      final Path pathname)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, dependencyFactory)
        .renderTemplate(pathname);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String template)
      throws InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, dependencyFactory)
        .renderTemplate(template);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion, 
      final InputStream stream)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, dependencyFactory)
        .renderTemplate(stream);
  }
}
