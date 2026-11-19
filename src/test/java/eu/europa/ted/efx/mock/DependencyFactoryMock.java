package eu.europa.ted.efx.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.antlr.v4.runtime.BaseErrorListener;

import eu.europa.ted.eforms.sdk.ComponentFactory;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator; // Import

/**
 * Provides EfxTranslator dependencies used for unit testing.
 */
public class DependencyFactoryMock implements TranslatorDependencyFactory {

  private DependencyFactoryMock() {
  }

  final public static DependencyFactoryMock INSTANCE = new DependencyFactoryMock();

  // Concurrent, as tests that run in parallel ask for the same instance at once
  Map<String, ScriptGenerator> scriptGenerators = new ConcurrentHashMap<>();
  Map<String, MarkupGenerator> markupGenerators = new ConcurrentHashMap<>();

  @Override
  public SymbolResolver createSymbolResolver(String sdkVersion, String qualifier) {
    // Ignore the qualifier for unit tests
    return SymbolResolverMockFactory.getInstance(sdkVersion);
  }

  @Override
  public ScriptGenerator createScriptGenerator(String sdkVersion, String qualifier, TranslatorOptions options) {
    // Default hashCode() implementation is OK here
    // we just need to distinguish TranslatorOptions instances
    String key = sdkVersion + qualifier + options.hashCode();
    return this.scriptGenerators.computeIfAbsent(key, k -> {
      try {
        return ComponentFactory.getScriptGenerator(sdkVersion, qualifier, options);
      } catch (InstantiationException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    });
  }

  @Override
  public MarkupGenerator createMarkupGenerator(String sdkVersion, String qualifier, TranslatorOptions options) {
    String key = sdkVersion + qualifier;
    return this.markupGenerators.computeIfAbsent(key, k -> new MarkupGeneratorMock());
  }

  @Override
  public ValidatorGenerator createValidatorGenerator(String sdkVersion, String qualifier,
      TranslatorOptions options) {
    // Return null or a mock implementation as needed for tests
    return null;
  }

  @Override
  public BaseErrorListener createErrorListener() {
    return ThrowingErrorListener.INSTANCE;
  }
}
