package eu.europa.ted.efx.component;

import eu.europa.ted.eforms.sdk.component.SdkComponentFactory;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.EfxExpressionTranslator;
import eu.europa.ted.efx.interfaces.EfxRulesTranslator;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.EfxComputeDependencyExtractor;
import eu.europa.ted.efx.interfaces.EfxValidationDependencyExtractor;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;

public class EfxTranslatorFactory extends SdkComponentFactory {
  public static final EfxTranslatorFactory INSTANCE = new EfxTranslatorFactory();

  private EfxTranslatorFactory() {
    super();
  }

  public static EfxExpressionTranslator getEfxExpressionTranslator(final String sdkVersion,
      final TranslatorDependencyFactory factory, TranslatorOptions options) throws InstantiationException {
    return getEfxExpressionTranslator(sdkVersion, "", factory, options);
  }

  public static EfxExpressionTranslator getEfxExpressionTranslator(final String sdkVersion,
      final String qualifier, final TranslatorDependencyFactory factory, TranslatorOptions options)
      throws InstantiationException {

    SymbolResolver symbolResolver = factory.createSymbolResolver(sdkVersion, qualifier);
    ScriptGenerator scriptGenerator = factory.createScriptGenerator(sdkVersion, qualifier, options);

    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.EFX_EXPRESSION_TRANSLATOR, qualifier, EfxExpressionTranslator.class,
        symbolResolver, scriptGenerator, factory.createErrorListener());
  }

  public static EfxTemplateTranslator getEfxTemplateTranslator(final String sdkVersion,
      final TranslatorDependencyFactory factory, TranslatorOptions options) throws InstantiationException {
    return getEfxTemplateTranslator(sdkVersion, "", factory, options);
  }

  public static EfxTemplateTranslator getEfxTemplateTranslator(final String sdkVersion,
      final String qualifier, final TranslatorDependencyFactory factory, TranslatorOptions options)
      throws InstantiationException {

    MarkupGenerator markupGenerator = factory.createMarkupGenerator(sdkVersion, qualifier, options);
    SymbolResolver symbolResolver = factory.createSymbolResolver(sdkVersion, qualifier);
    ScriptGenerator scriptGenerator = factory.createScriptGenerator(sdkVersion, qualifier, options);

    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.EFX_TEMPLATE_TRANSLATOR, qualifier, EfxTemplateTranslator.class,
        markupGenerator, symbolResolver, scriptGenerator, factory.createErrorListener());
  }

  public static EfxRulesTranslator getEfxRulesTranslator(final String sdkVersion,
      final TranslatorDependencyFactory factory, TranslatorOptions options) throws InstantiationException {
    return getEfxRulesTranslator(sdkVersion, "", factory, options);
  }

  public static EfxRulesTranslator getEfxRulesTranslator(final String sdkVersion,
      final String qualifier, final TranslatorDependencyFactory factory, TranslatorOptions options)
      throws InstantiationException {

    ValidatorGenerator validatorGenerator = factory.createValidatorGenerator(sdkVersion, qualifier, options);
    SymbolResolver symbolResolver = factory.createSymbolResolver(sdkVersion, qualifier);
    ScriptGenerator scriptGenerator = factory.createScriptGenerator(sdkVersion, qualifier, options);

    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.EFX_RULES_TRANSLATOR, qualifier, EfxRulesTranslator.class,
        validatorGenerator, symbolResolver, scriptGenerator, factory.createErrorListener());
  }

  public static EfxComputeDependencyExtractor getEfxComputeDependencyExtractor(final String sdkVersion,
      final TranslatorDependencyFactory factory) throws InstantiationException {
    return getEfxComputeDependencyExtractor(sdkVersion, "", factory);
  }

  public static EfxComputeDependencyExtractor getEfxComputeDependencyExtractor(final String sdkVersion,
      final String qualifier, final TranslatorDependencyFactory factory)
      throws InstantiationException {

    SymbolResolver symbolResolver = factory.createSymbolResolver(sdkVersion, qualifier);

    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.EFX_COMPUTE_DEPENDENCY_EXTRACTOR, qualifier, EfxComputeDependencyExtractor.class,
        symbolResolver, factory.createErrorListener());
  }

  public static EfxValidationDependencyExtractor getEfxValidationDependencyExtractor(final String sdkVersion,
      final TranslatorDependencyFactory factory) throws InstantiationException {
    return getEfxValidationDependencyExtractor(sdkVersion, "", factory);
  }

  public static EfxValidationDependencyExtractor getEfxValidationDependencyExtractor(final String sdkVersion,
      final String qualifier, final TranslatorDependencyFactory factory)
      throws InstantiationException {

    SymbolResolver symbolResolver = factory.createSymbolResolver(sdkVersion, qualifier);

    return EfxTranslatorFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.EFX_VALIDATION_DEPENDENCY_EXTRACTOR, qualifier, EfxValidationDependencyExtractor.class,
        symbolResolver, factory.createErrorListener());
  }
}
