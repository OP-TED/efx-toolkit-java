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
package eu.europa.ted.efx.mock;

import org.antlr.v4.runtime.BaseErrorListener;

import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;

/**
 * Gives the translator the markup generator of a test, such as one that records what it receives,
 * and the mocks of {@link DependencyFactoryMock} for the rest.
 */
public class DependencyFactoryWithMarkupGenerator implements TranslatorDependencyFactory {

  private final MarkupGenerator markupGenerator;

  public DependencyFactoryWithMarkupGenerator(final MarkupGenerator markupGenerator) {
    this.markupGenerator = markupGenerator;
  }

  @Override
  public SymbolResolver createSymbolResolver(String sdkVersion, String qualifier) {
    return DependencyFactoryMock.INSTANCE.createSymbolResolver(sdkVersion, qualifier);
  }

  @Override
  public ScriptGenerator createScriptGenerator(String sdkVersion, String qualifier,
      TranslatorOptions options) {
    return DependencyFactoryMock.INSTANCE.createScriptGenerator(sdkVersion, qualifier, options);
  }

  @Override
  public MarkupGenerator createMarkupGenerator(String sdkVersion, String qualifier,
      TranslatorOptions options) {
    return this.markupGenerator;
  }

  @Override
  public ValidatorGenerator createValidatorGenerator(String sdkVersion, String qualifier,
      TranslatorOptions options) {
    return null;
  }

  @Override
  public BaseErrorListener createErrorListener() {
    return DependencyFactoryMock.INSTANCE.createErrorListener();
  }
}
