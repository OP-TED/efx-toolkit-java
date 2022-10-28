package eu.europa.ted.efx.component;

import eu.europa.ted.eforms.sdk.component.SdkComponentFactory;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.EfxExpressionTranslator;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;

public class EfxTranslatorFactory extends SdkComponentFactory {
  public static final EfxTranslatorFactory INSTANCE = new EfxTranslatorFactory();

  private EfxTranslatorFactory() {
    super();
  }

  public static EfxExpressionTranslator getEfxExpressionTranslator(final String sdkVersion,
      final TranslatorDependencyFactory factory) throws InstantiationException {
    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.EFX_EXPRESSION_TRANSLATOR, EfxExpressionTranslator.class,
        factory.createSymbolResolver(sdkVersion), factory.createScriptGenerator(sdkVersion),
        factory.createErrorListener());
  }

  public static EfxTemplateTranslator getEfxTemplateTranslator(final String sdkVersion,
      final TranslatorDependencyFactory factory) throws InstantiationException {
    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.EFX_TEMPLATE_TRANSLATOR, EfxTemplateTranslator.class,
        factory.createMarkupGenerator(sdkVersion), factory.createSymbolResolver(sdkVersion),
        factory.createScriptGenerator(sdkVersion), factory.createErrorListener());
  }
}
