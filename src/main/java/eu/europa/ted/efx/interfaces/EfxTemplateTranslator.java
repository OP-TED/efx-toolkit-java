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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Defines the API of an EFX template translator.
 * 
 * Note that the behaviour of the translator is defined once during its instantiation using a
 * TranslatorDependencyFactory.
 */
public interface EfxTemplateTranslator extends EfxExpressionTranslator {

  /**
   * Translate the EFX template stored in a file, given the pathname of the file.
   * 
   * @param pathname The path and filename of the EFX template file to translate.
   * @return A string containing the translated template.
   * @throws IOException If the file cannot be read.
   */
  String renderTemplate(Path pathname) throws IOException;

  /**
   * Translate the EFX template stored in the given string.
   * 
   * @param template A string containing an EFX template to be translated.
   * @return A string containing the translated template.
   */
  String renderTemplate(String template);

  /**
   * Translate the EFX template given as an InputStream.
   * 
   * @param stream An InputStream with the EFX template to be translated.
   * @return A string containing the translated template.
   * @throws IOException If the InputStream cannot be read.
   */
  String renderTemplate(InputStream stream) throws IOException;
}
