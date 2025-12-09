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
 * the Licence.
 */
package eu.europa.ted.efx.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import eu.europa.ted.efx.EfxTranslatorOptions;

/**
 * Defines the API of an EFX rules translator.
 *
 * An EFX rules translator converts EFX rules files into Schematron validation files.
 * It generates multiple .sch pattern files, a complete-validation.sch master file,
 * and a schematrons.json metadata file.
 *
 * Note that the behaviour of the translator is defined once during its instantiation using a
 * TranslatorDependencyFactory.
 */
public interface EfxRulesTranslator extends EfxExpressionTranslator {

  /**
   * Translate the EFX rules stored in a file, given the pathname of the file.
   *
   * @param pathname The path and filename of the EFX rules file to translate.
   * @param options The options to be used by the EFX rules translator.
   * @return A map where keys are output file paths (relative) and values are the generated content.
   *         For example: {"validation-stage-1a.sch" -> "<?xml...", "complete-validation.sch" -> "<?xml...", ...}
   * @throws IOException If the file cannot be read.
   */
  Map<String, String> translateRules(Path pathname, TranslatorOptions options) throws IOException;

  /**
   * Translate the EFX rules stored in a file, given the pathname of the file.
   *
   * @param pathname The path and filename of the EFX rules file to translate.
   * @return A map where keys are output file paths (relative) and values are the generated content.
   * @throws IOException If the file cannot be read.
   */
  default Map<String, String> translateRules(Path pathname) throws IOException {
    return translateRules(pathname, EfxTranslatorOptions.DEFAULT);
  }

  /**
   * Translate the EFX rules stored in the given string.
   *
   * @param rules A string containing EFX rules to be translated.
   * @param options The options to be used by the EFX rules translator.
   * @return A map where keys are output file paths (relative) and values are the generated content.
   */
  Map<String, String> translateRules(String rules, TranslatorOptions options);

  /**
   * Translate the EFX rules stored in the given string.
   *
   * @param rules A string containing EFX rules to be translated.
   * @return A map where keys are output file paths (relative) and values are the generated content.
   */
  default Map<String, String> translateRules(String rules) {
    return translateRules(rules, EfxTranslatorOptions.DEFAULT);
  }

  /**
   * Translate the EFX rules given as an InputStream.
   *
   * @param stream An InputStream with the EFX rules to be translated.
   * @param options The options to be used by the EFX rules translator.
   * @return A map where keys are output file paths (relative) and values are the generated content.
   * @throws IOException If the InputStream cannot be read.
   */
  Map<String, String> translateRules(InputStream stream, TranslatorOptions options) throws IOException;

  /**
   * Translate the EFX rules given as an InputStream.
   *
   * @param stream An InputStream with the EFX rules to be translated.
   * @return A map where keys are output file paths (relative) and values are the generated content.
   * @throws IOException If the InputStream cannot be read.
   */
  default Map<String, String> translateRules(InputStream stream) throws IOException {
    return translateRules(stream, EfxTranslatorOptions.DEFAULT);
  }
}
