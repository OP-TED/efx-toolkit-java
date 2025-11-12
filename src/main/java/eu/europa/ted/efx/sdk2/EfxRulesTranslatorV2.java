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
package eu.europa.ted.efx.sdk2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.EfxRulesTranslator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.schematron.SchematronSchema;
import eu.europa.ted.efx.schematron.SchematronMarkupGenerator;
import freemarker.template.TemplateException;

/**
 * EFX Rules translator for SDK version 2.
 *
 * This translator parses EFX Rules files and generates Schematron validation files.
 * It extends EfxExpressionTranslatorV2 to reuse XPath expression generation and
 * implements EfxRulesTranslator to provide the rules translation API.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_RULES_TRANSLATOR)
public class EfxRulesTranslatorV2 extends EfxExpressionTranslatorV2
    implements EfxRulesTranslator {

  private static final Logger logger = LoggerFactory.getLogger(EfxRulesTranslatorV2.class);

  /**
   * The SchematronMarkupGenerator is used to generate Schematron XML from our data model.
   */
  private final SchematronMarkupGenerator markupGenerator;

  /**
   * The Schematron schema being built during parse tree walking.
   */
  private SchematronSchema schema;

  @SuppressWarnings("unused")
  private EfxRulesTranslatorV2() {
    super();
    this.markupGenerator = null;
  }

  /**
   * Constructor for EfxRulesTranslatorV2.
   *
   * @param markupGenerator The generator for creating Schematron XML markup.
   * @param symbolResolver The symbol resolver for looking up fields, nodes, and codelists.
   * @param scriptGenerator The script generator for creating XPath expressions.
   * @param errorListener The error listener for capturing parse errors.
   */
  public EfxRulesTranslatorV2(final SchematronMarkupGenerator markupGenerator,
      final SymbolResolver symbolResolver, final ScriptGenerator scriptGenerator,
      final BaseErrorListener errorListener) {
    super(symbolResolver, scriptGenerator, errorListener);
    this.markupGenerator = markupGenerator;
  }

  @Override
  public Map<String, String> translateRules(Path pathname, TranslatorOptions options)
      throws IOException {
    logger.debug("Translating EFX rules from file: {}", pathname);
    CharStream input = CharStreams.fromPath(pathname);
    return translateRulesFromCharStream(input, options);
  }

  @Override
  public Map<String, String> translateRules(String rules, TranslatorOptions options) {
    logger.debug("Translating EFX rules from string");
    CharStream input = CharStreams.fromString(rules);
    try {
      return translateRulesFromCharStream(input, options);
    } catch (IOException e) {
      // This should never happen when reading from a string
      throw new RuntimeException("Unexpected IOException while translating rules from string", e);
    }
  }

  @Override
  public Map<String, String> translateRules(InputStream stream, TranslatorOptions options)
      throws IOException {
    logger.debug("Translating EFX rules from input stream");
    CharStream input = CharStreams.fromStream(stream);
    return translateRulesFromCharStream(input, options);
  }

  /**
   * Internal method to translate EFX rules from a CharStream.
   *
   * @param input The CharStream containing the EFX rules.
   * @param options The translator options.
   * @return A map of output file paths to generated content.
   * @throws IOException If an I/O error occurs during translation.
   */
  private Map<String, String> translateRulesFromCharStream(CharStream input,
      TranslatorOptions options) throws IOException {
    logger.debug("Parsing EFX rules");

    // Create lexer and parser
    EfxLexer lexer = new EfxLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(this.errorListener);

    CommonTokenStream tokens = new CommonTokenStream(lexer);

    EfxParser parser = new EfxParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(this.errorListener);

    // Parse the rules file
    ParseTree tree = parser.rulesFile();

    logger.debug("Walking parse tree to generate Schematron");

    // Initialize the schema for this translation
    this.schema = new SchematronSchema("eForms schematron rules");

    // Walk the parse tree - this translator IS the listener
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);

    // Generate Schematron XML files
    Map<String, String> outputFiles = new HashMap<>();

    try {
      // Generate complete-validation.sch
      String completeValidation = markupGenerator.generateCompleteValidation(this.schema);
      outputFiles.put("complete-validation.sch", completeValidation);

      // TODO: Generate individual pattern files
      // TODO: Generate schematrons.json

      logger.debug("Generated {} Schematron files", outputFiles.size());

      return outputFiles;

    } catch (TemplateException e) {
      throw new IOException("Failed to generate Schematron XML", e);
    }
  }
}
