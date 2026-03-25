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
package eu.europa.ted.efx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import eu.europa.ted.efx.component.EfxTranslatorFactory;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.dependencies.DependencyGraph;

/**
 * Provided for convenience, this class exposes static methods that allow you to quickly instantiate
 * an EFX translator to translate EFX expressions and templates.
 */
public class EfxTranslator {

  private EfxTranslator() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
 
  private static TranslatorOptions defaultOptions = EfxTranslatorOptions.DEFAULT;

  //#region Translate EFX expressions -----------------------------------------

  /**
   * Instantiates an EFX expression translator and translates a given expression.
   * 
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the EFX expression translator.
   * @param sdkVersion The version of the eForms SDK that defines the EFX grammar used by the
   *        expression to be translated.
   * @param expression The EFX expression to translate.
   * @param expressionParameters The values of any parameters that the EFX expression requires.
   * @param options The options to be used by the EFX expression translator.
   * @return The translated expression in the target script language supported by the given
   *         {@link TranslatorDependencyFactory}.
   * @throws InstantiationException If the EFX expression translator cannot be instantiated.
   */
  public static String translateExpression(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String expression, TranslatorOptions options, final String... expressionParameters)
      throws InstantiationException {
    return EfxTranslatorFactory.getEfxExpressionTranslator(sdkVersion, dependencyFactory, options)
        .translateExpression(expression, expressionParameters);
  }

  public static String translateExpression(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String expression, final String... expressionParameters) throws InstantiationException {
    return translateExpression(dependencyFactory, sdkVersion, expression, defaultOptions, expressionParameters);
  }

  //#endregion Translate EFX expressions --------------------------------------

  //#region Translate EFX templates -------------------------------------------

  /**
   * Instantiates an EFX template translator and translates the EFX template contained in the given
   * file.
   * 
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the EFX template translator.
   * @param sdkVersion The version of the eForms SDK that defines the EFX grammar used by the EFX
   *        template to be translated.
   * @param pathname The path to the file containing the EFX template to translate.
   * @return The translated template in the target markup language supported by the given
   *         {@link TranslatorDependencyFactory}.
   * @param options The options to be used by the EFX template translator. 
   * @throws IOException If the file cannot be read.
   * @throws InstantiationException If the EFX template translator cannot be instantiated.
   */
  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final Path pathname, TranslatorOptions options)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, dependencyFactory, options)
        .renderTemplate(pathname, options);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final Path pathname)
      throws IOException, InstantiationException {
    return translateTemplate(dependencyFactory, sdkVersion, pathname, defaultOptions);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String qualifier,
      final Path pathname, TranslatorOptions options)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, qualifier, dependencyFactory, options)
        .renderTemplate(pathname, options);
  }

  /**
   * Instantiates an EFX template translator and translates the given EFX template.
   * 
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the EFX template translator.
   * @param sdkVersion The version of the eForms SDK that defines the EFX grammar used by the EFX
   *        template to be translated.
   * @param template A string containing the EFX template to translate.
   * @return The translated template in the target markup language supported by the given
   *         {@link TranslatorDependencyFactory}.
   * @param options The options to be used by the EFX template translator.
   * @throws InstantiationException If the EFX template translator cannot be instantiated.
   */
  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String template, TranslatorOptions options)
      throws InstantiationException {
    return translateTemplate(dependencyFactory, sdkVersion, "", template, options);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String template)
      throws InstantiationException {
    return translateTemplate(dependencyFactory, sdkVersion, template, defaultOptions);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String qualifier, final String template, TranslatorOptions options)
      throws InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, qualifier, dependencyFactory, options)
        .renderTemplate(template, options);
  }

  /**
   * Instantiates an EFX template translator and translates the EFX template contained in the given
   * InputStream.
   * 
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the EFX template translator.
   * @param sdkVersion The version of the eForms SDK that defines the EFX grammar used by the EFX
   *        template to be translated.
   * @param stream An InputStream containing the EFX template to be translated.
   * @return The translated template in the target markup language supported by the given
   *         {@link TranslatorDependencyFactory}.
   * @param options The options to be used by the EFX template translator.
   * @throws IOException If the InputStream cannot be read.
   * @throws InstantiationException If the EFX template translator cannot be instantiated.
   */
  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final InputStream stream, TranslatorOptions options)
      throws IOException, InstantiationException {
      return translateTemplate(dependencyFactory, sdkVersion, "", stream, options);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final InputStream stream)
      throws IOException, InstantiationException {
    return translateTemplate(dependencyFactory, sdkVersion, stream, defaultOptions);
  }

  public static String translateTemplate(final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String qualifier, final InputStream stream, TranslatorOptions options)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxTemplateTranslator(sdkVersion, qualifier, dependencyFactory, options)
        .renderTemplate(stream, options);
  }

  //#endregion Translate EFX templates ----------------------------------------

  //#region Translate EFX rules -----------------------------------------------

  /**
   * Instantiates an EFX rules translator and translates the EFX rules contained in the given file.
   *
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the EFX rules translator.
   * @param sdkVersion The version of the eForms SDK that defines the EFX grammar used by the EFX
   *        rules to be translated.
   * @param pathname The path to the file containing the EFX rules to translate.
   * @param options The options to be used by the EFX rules translator.
   * @return A map where keys are output file paths (relative) and values are the generated Schematron content.
   * @throws IOException If the file cannot be read.
   * @throws InstantiationException If the EFX rules translator cannot be instantiated.
   */
  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final Path pathname, TranslatorOptions options)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxRulesTranslator(sdkVersion, dependencyFactory, options)
        .translateRules(pathname, options);
  }

  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final Path pathname)
      throws IOException, InstantiationException {
    return translateRules(dependencyFactory, sdkVersion, pathname, defaultOptions);
  }

  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final String qualifier, final Path pathname, TranslatorOptions options)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxRulesTranslator(sdkVersion, qualifier, dependencyFactory, options)
        .translateRules(pathname, options);
  }

  /**
   * Instantiates an EFX rules translator and translates the given EFX rules.
   *
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the EFX rules translator.
   * @param sdkVersion The version of the eForms SDK that defines the EFX grammar used by the EFX
   *        rules to be translated.
   * @param rules A string containing the EFX rules to translate.
   * @param options The options to be used by the EFX rules translator.
   * @return A map where keys are output file paths (relative) and values are the generated Schematron content.
   * @throws InstantiationException If the EFX rules translator cannot be instantiated.
   */
  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final String rules, TranslatorOptions options)
      throws InstantiationException {
    return translateRules(dependencyFactory, sdkVersion, "", rules, options);
  }

  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final String rules)
      throws InstantiationException {
    return translateRules(dependencyFactory, sdkVersion, rules, defaultOptions);
  }

  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final String qualifier, final String rules, TranslatorOptions options)
      throws InstantiationException {
    return EfxTranslatorFactory.getEfxRulesTranslator(sdkVersion, qualifier, dependencyFactory, options)
        .translateRules(rules, options);
  }

  /**
   * Instantiates an EFX rules translator and translates the EFX rules contained in the given
   * InputStream.
   *
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the EFX rules translator.
   * @param sdkVersion The version of the eForms SDK that defines the EFX grammar used by the EFX
   *        rules to be translated.
   * @param stream An InputStream containing the EFX rules to be translated.
   * @param options The options to be used by the EFX rules translator.
   * @return A map where keys are output file paths (relative) and values are the generated Schematron content.
   * @throws IOException If the InputStream cannot be read.
   * @throws InstantiationException If the EFX rules translator cannot be instantiated.
   */
  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final InputStream stream, TranslatorOptions options)
      throws IOException, InstantiationException {
    return translateRules(dependencyFactory, sdkVersion, "", stream, options);
  }

  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final InputStream stream)
      throws IOException, InstantiationException {
    return translateRules(dependencyFactory, sdkVersion, stream, defaultOptions);
  }

  public static Map<String, String> translateRules(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final String qualifier, final InputStream stream, TranslatorOptions options)
      throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxRulesTranslator(sdkVersion, qualifier, dependencyFactory, options)
        .translateRules(stream, options);
  }

  //#endregion Translate EFX rules --------------------------------------------

  //#region Extract EFX dependencies -------------------------------------------

  /**
   * Instantiates an EFX compute dependency extractor and extracts all field and node identifiers
   * referenced in the given expression.
   *
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the extractor.
   * @param sdkVersion The version of the eForms SDK that defines the EFX grammar used by the
   *        expression to be analysed.
   * @param expression The EFX expression to analyse.
   * @return An unmodifiable set of field and node identifiers referenced in the expression.
   * @throws InstantiationException If the dependency extractor cannot be instantiated.
   */
  public static Set<String> extractComputeDependencies(final TranslatorDependencyFactory dependencyFactory,
      final String sdkVersion, final String expression) throws InstantiationException {
    return EfxTranslatorFactory.getEfxComputeDependencyExtractor(sdkVersion, dependencyFactory)
        .extractDependencies(expression);
  }

  /**
   * Instantiates an EFX validation dependency extractor and extracts the dependency graph
   * from the given EFX rules string.
   *
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the extractor.
   * @param sdkVersion The version of the eForms SDK.
   * @param rules The EFX rules to analyse.
   * @return A {@link DependencyGraph} with all dependencies and reverse dependencies.
   * @throws InstantiationException If the dependency extractor cannot be instantiated.
   */
  public static DependencyGraph extractValidationDependencies(
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final String rules) throws InstantiationException {
    return EfxTranslatorFactory.getEfxValidationDependencyExtractor(sdkVersion, dependencyFactory)
        .extractDependencyGraph(rules);
  }

  /**
   * Instantiates an EFX validation dependency extractor and extracts the dependency graph
   * from the given EFX rules file.
   *
   * @param dependencyFactory A {@link TranslatorDependencyFactory} to be used for instantiating the
   *        dependencies of the extractor.
   * @param sdkVersion The version of the eForms SDK.
   * @param pathname The path to the EFX rules file.
   * @return A {@link DependencyGraph} with all dependencies and reverse dependencies.
   * @throws IOException If the file cannot be read.
   * @throws InstantiationException If the dependency extractor cannot be instantiated.
   */
  public static DependencyGraph extractValidationDependencies(
      final TranslatorDependencyFactory dependencyFactory, final String sdkVersion,
      final Path pathname) throws IOException, InstantiationException {
    return EfxTranslatorFactory.getEfxValidationDependencyExtractor(sdkVersion, dependencyFactory)
        .extractDependencyGraph(pathname);
  }

  //#endregion Extract EFX dependencies ----------------------------------------
}
