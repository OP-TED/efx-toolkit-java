/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.sdk2;

import org.antlr.v4.runtime.BaseErrorListener;

import eu.europa.ted.eforms.sdk.ComponentFactory;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import eu.europa.ted.efx.mock.MarkupGeneratorMock;

/**
 * Provides the EfxTranslator dependencies used for translating the SDK corpus: the symbols of the
 * real SDK metadata, the XPath script generator and the Schematron validator generator of the
 * toolkit, and the markup generator mock of the unit tests.
 */
class SdkCorpusDependencyFactory implements TranslatorDependencyFactory {

  private final SymbolResolver symbolResolver;

  /**
   * @param symbolResolver The symbol resolver loaded from the SDK whose corpus is translated.
   */
  SdkCorpusDependencyFactory(final SymbolResolver symbolResolver) {
    this.symbolResolver = symbolResolver;
  }

  @Override
  public SymbolResolver createSymbolResolver(String sdkVersion, String qualifier) {
    return this.symbolResolver;
  }

  @Override
  public ScriptGenerator createScriptGenerator(String sdkVersion, String qualifier,
      TranslatorOptions options) {
    try {
      return ComponentFactory.getScriptGenerator(sdkVersion, qualifier, options);
    } catch (InstantiationException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public MarkupGenerator createMarkupGenerator(String sdkVersion, String qualifier,
      TranslatorOptions options) {
    return new MarkupGeneratorMock();
  }

  @Override
  public ValidatorGenerator createValidatorGenerator(String sdkVersion, String qualifier,
      TranslatorOptions options) {
    try {
      return ComponentFactory.getValidatorGenerator(sdkVersion, qualifier, options);
    } catch (InstantiationException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public BaseErrorListener createErrorListener() {
    return ThrowingErrorListener.INSTANCE;
  }
}
