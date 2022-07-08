package eu.europa.ted.efx.translator;

import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;
import eu.europa.ted.eforms.sdk.factory.AbstractSdkObjectFactory;
import eu.europa.ted.efx.interfaces.EfxExpressionTranslator;
import eu.europa.ted.efx.interfaces.EfxTemplateProcessor;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;

public class EfxTranslatorFactory extends AbstractSdkObjectFactory {
  public static final EfxTranslatorFactory INSTANCE = new EfxTranslatorFactory();

  private EfxTranslatorFactory() {
    super();
  }

  public static EfxExpressionTranslator getEfxExpressionTranslator(final String sdkVersion,
      final TranslatorDependencyFactory factory) throws InstantiationException {
    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentTypeEnum.EFX_EXPRESSION_TRANSLATOR, EfxExpressionTranslator.class)
        .init(factory.createSymbolResolver(sdkVersion), factory.createScriptGenerator(),
            factory.createErrorListener());
  }

  public static EfxTemplateProcessor getEfxTemplateTranslator(final String sdkVersion,
      final TranslatorDependencyFactory factory) throws InstantiationException {
    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentTypeEnum.EFX_TEMPLATE_TRANSLATOR, EfxTemplateProcessor.class)
        .init(factory.createMarkupGenerator(), factory.createSymbolResolver(sdkVersion),
            factory.createScriptGenerator(), factory.createErrorListener());
  }
}
