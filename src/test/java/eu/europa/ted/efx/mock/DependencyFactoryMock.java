package eu.europa.ted.efx.mock;

import org.antlr.v4.runtime.BaseErrorListener;

import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

/**
 * Provides EfxTranslator dependencies used for unit testing.
 */
public class DependencyFactoryMock implements TranslatorDependencyFactory {

  private DependencyFactoryMock() {}

  final public static DependencyFactoryMock INSTANCE = new DependencyFactoryMock();

  ScriptGenerator scriptGenerator;
  MarkupGenerator markupGenerator;

  @Override
  public SymbolResolver createSymbolResolver(String sdkVersion) {
    return SymbolResolverMock.getInstance(sdkVersion);
  }

  @Override
  public ScriptGenerator createScriptGenerator(String sdkVersion, TranslatorOptions options) {
    if (scriptGenerator == null) {
      this.scriptGenerator = new XPathScriptGenerator(options);
    }
    return this.scriptGenerator;
  }

  @Override
  public MarkupGenerator createMarkupGenerator(String sdkVersion, TranslatorOptions options) {
    if (this.markupGenerator == null) {
      this.markupGenerator = new MarkupGeneratorMock();
    }
    return this.markupGenerator;
  }

  @Override
  public BaseErrorListener createErrorListener() {
    return ThrowingErrorListener.INSTANCE;
  }
}
