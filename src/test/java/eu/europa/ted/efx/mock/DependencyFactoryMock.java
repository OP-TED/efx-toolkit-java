package eu.europa.ted.efx.mock;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.BaseErrorListener;

import eu.europa.ted.eforms.sdk.ComponentFactory;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;

/**
 * Provides EfxTranslator dependencies used for unit testing.
 */
public class DependencyFactoryMock implements TranslatorDependencyFactory {

  private DependencyFactoryMock() {}

  final public static DependencyFactoryMock INSTANCE = new DependencyFactoryMock();

  Map<String, ScriptGenerator> scriptGenerators = new HashMap<>();
  Map<String, MarkupGenerator> markupGenerators = new HashMap<>();
  
  @Override
  public SymbolResolver createSymbolResolver(String sdkVersion) {
    return SymbolResolverMockFactory.getInstance(sdkVersion);
  }

  @Override
  public ScriptGenerator createScriptGenerator(String sdkVersion, TranslatorOptions options) {
    if (!scriptGenerators.containsKey(sdkVersion)) {
      try {
        this.scriptGenerators.put(sdkVersion, ComponentFactory.getScriptGenerator(sdkVersion, options));
      } catch (InstantiationException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return this.scriptGenerators.get(sdkVersion);
  }

  @Override
  public MarkupGenerator createMarkupGenerator(String sdkVersion, TranslatorOptions options) {
    if (!this.markupGenerators.containsKey(sdkVersion)) {
      this.markupGenerators.put(sdkVersion, new MarkupGeneratorMock());
    }
    return this.markupGenerators.get(sdkVersion);
  }

  @Override
  public BaseErrorListener createErrorListener() {
    return ThrowingErrorListener.INSTANCE;
  }
}
