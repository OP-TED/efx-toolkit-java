/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Lic
 */

package eu.europa.ted.efx.interfaces;

/**
 * Defines the API of an EFX expression translator.
 * 
 * Note that the behaviour of the translator is defined once during its instantiation using a
 * TranslatorDependencyFactory.
 */
public interface EfxExpressionTranslator {

  /**
   * Translate the given EFX expression,
   * 
   * @param expression A string containing the EFX expression to be translated.
   * @param expressionParameters The values of any parameters that the given expression expects.
   * @return
   */
  String translateExpression(final String expression, final String... expressionParameters);
}
