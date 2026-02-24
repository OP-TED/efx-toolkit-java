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
package eu.europa.ted.efx.sdk2;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.ParseInfo;
import org.antlr.v4.runtime.atn.ProfilingATNSimulator;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.InvalidIndentationException;
import eu.europa.ted.efx.interfaces.Argument;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.interfaces.IncludedFileResolver;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.TemplateSection;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.util.EfxProfilerReportGenerator;
import eu.europa.ted.efx.util.TranslatorTimings;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.templates.Conditional;
import eu.europa.ted.efx.model.templates.Conditionals;
import eu.europa.ted.efx.model.templates.ContentBlock;
import eu.europa.ted.efx.model.templates.ContentBlockStack;
import eu.europa.ted.efx.model.templates.TemplateDefinition;
import eu.europa.ted.efx.model.templates.TemplateInvocation;
import eu.europa.ted.efx.model.templates.Markup;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.model.variables.Dictionary;
import eu.europa.ted.efx.model.variables.Function;
import eu.europa.ted.efx.model.variables.StrictArguments;
import eu.europa.ted.efx.model.variables.Identifier;
import eu.europa.ted.efx.model.variables.ParsedParameter;
import eu.europa.ted.efx.model.variables.ParsedParameters;
import eu.europa.ted.efx.model.variables.Template;
import eu.europa.ted.efx.model.variables.Variable;
import eu.europa.ted.efx.model.variables.Variables;
import eu.europa.ted.efx.model.expressions.scalar.MultilingualStringPath;
import eu.europa.ted.efx.sdk2.EfxParser.*;

/**
 * The EfxTemplateTranslator extends the {@link EfxExpressionTranslatorV2} to provide additional
 * translation capabilities for EFX templates. If has been implemented as an extension to the
 * EfxExpressionTranslator in order to keep things simpler when one only needs to translate EFX
 * expressions (like the condition associated with a business rule).
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_TEMPLATE_TRANSLATOR)
public class EfxTemplateTranslatorV2 extends EfxExpressionTranslatorV2
    implements EfxTemplateTranslator {

  private static final Logger logger = LoggerFactory.getLogger(EfxTemplateTranslatorV2.class);

  private static final String UNEXPECTED_INDENTATION = "Unexpected indentation tracker state.";

  private static final String LABEL_TYPE_NAME = getLexerSymbol(EfxLexer.LABEL_TYPE_NAME);
  private static final String LABEL_TYPE_WHEN = getLexerSymbol(EfxLexer.LABEL_TYPE_WHEN_TRUE).replace("-true", "");
  private static final String SHORTHAND_CONTEXT_FIELD_LABEL_REFERENCE = getLexerSymbol(EfxLexer.ValueKeyword);
  private static final String ASSET_TYPE_INDICATOR = getLexerSymbol(EfxLexer.Indicator);
  private static final String ASSET_TYPE_BT = getLexerSymbol(EfxLexer.ASSET_TYPE_BT);
  private static final String ASSET_TYPE_FIELD = getLexerSymbol(EfxLexer.ASSET_TYPE_FIELD);
  private static final String ASSET_TYPE_NODE = getLexerSymbol(EfxLexer.ASSET_TYPE_NODE);
  private static final String ASSET_TYPE_CODE = getLexerSymbol(EfxLexer.Code);

  /**
   * Used to control the indentation style used in a template
   */
  private enum Indent {
    TABS, SPACES, UNDETERMINED
  }

  private Indent indentWith = Indent.UNDETERMINED;
  private int indentSpaces = -1;

  /**
   * The MarkupGenerator is called to retrieve markup in the target template language when needed.
   */
  MarkupGenerator markup;

  TranslatorContext translatorContext = TranslatorContext.DEFAULT;

  final ContentBlock bodySectionRoot = ContentBlock.newRootBlock("body");
  final ContentBlock summarySectionRoot = ContentBlock.newRootBlock("summary");
  final ContentBlock navigationSectionRoot = ContentBlock.newRootBlock("nav");
  ContentBlock rootBlock = bodySectionRoot;

  /**
   * The block stack is used to keep track of the indentation of template lines and adjust the EFX
   * context accordingly. A block is a template line together with the template lines nested
   * (through indentation) under it. At the top of the blockStack is the template block that is
   * currently being processed. The next block in the stack is its parent block, and so on.
   */
  ContentBlockStack blockStack = new ContentBlockStack();

  @SuppressWarnings("unused")
  private EfxTemplateTranslatorV2() {
    super();
  }

  public EfxTemplateTranslatorV2(final MarkupGenerator markupGenerator,
      final SymbolResolver symbolResolver, final ScriptGenerator scriptGenerator,
      final BaseErrorListener errorListener) {
    super(symbolResolver, scriptGenerator, errorListener);

    this.markup = markupGenerator;
  }

  /**
   * Opens the indicated EFX file and translates the EFX template it contains.
   */
  @Override
  public String renderTemplate(final Path pathname, TranslatorOptions options) throws IOException {
    // Default to filesystem-based include resolution relative to the input file
    if (options.getIncludedFileResolver() == null) {
      Path baseDir = pathname.toAbsolutePath().getParent();
      options = TranslatorOptions.withResolver(options, new FileSystemIncludedFileResolver(baseDir));
    }

    return renderTemplate(CharStreams.fromPath(pathname), options);
  }

  /**
   * Translates the template contained in the string passed as a parameter.
   */
  @Override
  public String renderTemplate(final String template, TranslatorOptions options) {
    try {
      return renderTemplate(CharStreams.fromString(template), options);
    } catch (IOException e) {
      throw new UncheckedIOException("Include resolution failed during template rendering", e);
    }
  }

  @Override
  public String renderTemplate(final InputStream stream, TranslatorOptions options) throws IOException {
    return renderTemplate(CharStreams.fromStream(stream), options);
  }

  private String renderTemplate(final CharStream charStream, TranslatorOptions options)
      throws IOException {
    logger.debug("Rendering template");
    final long startTime = System.currentTimeMillis();

    // New in EFX-2: template preprocessing
    final long preprocessingStartTime = System.currentTimeMillis();
    final TemplatePreprocessor preprocessor = this.new TemplatePreprocessor(charStream, options.getIncludedFileResolver());
    final String preprocessedTemplate = preprocessor.processTemplate();
    final long preprocessingEndTime = System.currentTimeMillis();
    final long preprocessingDuration = preprocessingEndTime - preprocessingStartTime;

    // Now parse the preprocessed template
    final long translationStartTime = System.currentTimeMillis();
    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(preprocessedTemplate));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);
    parser.setErrorHandler(new EfxErrorStrategy());

    // Enable profiling if requested
    if (options != null && options.isProfilerEnabled()) {
      parser.setInterpreter(new ProfilingATNSimulator(parser));
    }

    if (errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }

    final ParseTree tree = parser.templateFile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);
    
    final long translationEndTime = System.currentTimeMillis();
    final long translationDuration = translationEndTime - translationStartTime;
    
    final long endTime = System.currentTimeMillis();
    final long totalDuration = endTime - startTime;
    
    // Log profiling information if enabled
    if (options != null && options.isProfilerEnabled()) {
      TranslatorTimings timingData = new TranslatorTimings(preprocessingDuration, translationDuration, totalDuration);
      this.generateAndSaveProfilerReport(parser, options.getProfilerOutputPath(), timingData);
    }

    logger.debug("Finished rendering template");

    return getTranslatedMarkup();
  }

  /**
   * Gets the translated code after the walker finished its walk. Every line in the template has now
   * been translated and a {@link Markup} object is in the stack for each block in the template. The
   * output template is built from the end towards the top, because the items are removed from the
   * stack in reverse order.
   * 
   * @return The translated code, trimmed
   */
  private String getTranslatedMarkup() {
    logger.debug("Getting translated markup.");

    final StringBuilder sb = new StringBuilder(64);
    while (!this.stack.empty()) {
      sb.insert(0, '\n').insert(0, this.stack.pop(Markup.class).script);
    }

    logger.debug("Finished getting translated markup.");

    return sb.toString().trim();
  }

  /**
   * Logs ANTLR4 profiling results showing which grammar rules took the most time.
   * Only active when profiling is enabled via system property.
   * 
   * @param parser The EfxParser instance to extract profiling data from
   * @param profilingOutputPath Path where the HTML report should be saved
   * @param timingData Timing measurements for different processing phases
   */
  private void generateAndSaveProfilerReport(final EfxParser parser, final Path profilingOutputPath, final TranslatorTimings timingData) {
    final ParseInfo parseInfo = parser.getParseInfo();
    if (parseInfo == null) {
      logger.warn("ParseInfo not available - profiling may not be enabled in parser");
      return;
    }

    final DecisionInfo[] decisions = parseInfo.getDecisionInfo();
    
    // Sort decisions by time descending
    java.util.Arrays.sort(decisions, (a, b) -> Long.compare(b.timeInPrediction, a.timeInPrediction));
    
    long totalTime = java.util.Arrays.stream(decisions)
        .mapToLong(d -> d.timeInPrediction)
        .sum();

    // Write HTML report to file if path is provided
    EfxProfilerReportGenerator.generateAndSaveProfilerReport(parser, decisions, totalTime, timingData, profilingOutputPath);
  }


  // #region Global declarationExpressions ---------------------------------------

  @Override
  public void exitVariableDeclaration(VariableDeclarationContext ctx) {
    var variable = this.stack.pop(Variable.class);
    this.stack.declareGlobalIdentifier(variable);
  }

  @Override
  public void enterStringFunctionDeclaration(StringFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitStringFunctionDeclaration(StringFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterBooleanFunctionDeclaration(BooleanFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitBooleanFunctionDeclaration(BooleanFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterNumericFunctionDeclaration(NumericFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitNumericFunctionDeclaration(NumericFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterDateFunctionDeclaration(DateFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitDateFunctionDeclaration(DateFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterTimeFunctionDeclaration(TimeFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitTimeFunctionDeclaration(TimeFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterDurationFunctionDeclaration(DurationFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitDurationFunctionDeclaration(DurationFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  // Sequence function declarations

  @Override
  public void enterStringSequenceFunctionDeclaration(StringSequenceFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitStringSequenceFunctionDeclaration(StringSequenceFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterNumericSequenceFunctionDeclaration(NumericSequenceFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitNumericSequenceFunctionDeclaration(NumericSequenceFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterBooleanSequenceFunctionDeclaration(BooleanSequenceFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitBooleanSequenceFunctionDeclaration(BooleanSequenceFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterDateSequenceFunctionDeclaration(DateSequenceFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitDateSequenceFunctionDeclaration(DateSequenceFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterTimeSequenceFunctionDeclaration(TimeSequenceFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitTimeSequenceFunctionDeclaration(TimeSequenceFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  @Override
  public void enterDurationSequenceFunctionDeclaration(DurationSequenceFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitDurationSequenceFunctionDeclaration(DurationSequenceFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText());
  }

  private void exitFunctionDeclaration(String functionName) {
    var expression = this.stack.pop(TypedExpression.class);
    var parameters = this.stack.pop(ParsedParameters.class);

    this.stack.declareFunction(new Function(functionName, parameters, expression));
  }

  @Override
  public void exitTemplateDefinition(TemplateDefinitionContext ctx) {
    var parameters = this.stack.pop(ParsedParameters.class);
    var template = new Template(ctx.templateName.getText(), parameters);
    this.stack.declareTemplate(template);
    this.stack.push(template);
  }

  @Override
  public void enterTemplateDefinition(TemplateDefinitionContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  // #endregion Global declarationExpressions ------------------------------------

  // #region Template File ----------------------------------------------------

  @Override
  public void enterTemplateFile(TemplateFileContext ctx) {
    assert blockStack.isEmpty() : UNEXPECTED_INDENTATION;
    this.translatorContext.setCurrentSection(TemplateSection.DEFAULT);
  }

  @Override
  public void exitTemplateFile(TemplateFileContext ctx) {
    // if there are no template lines the blockStack will be empty, 
    // otherwise there will be one block left: the one created when exiting the previous line; so we just remove it here.
    if (!this.blockStack.isEmpty()) {
      this.blockStack.pop();
    }

    List<Markup> globals = new ArrayList<>();
    for (Identifier identifier : this.stack.getGlobals()) {
      if (identifier instanceof Variable) {
        Variable variable = (Variable) identifier;
        globals.add(this.markup.renderVariableDeclaration(variable.name, variable.initializationExpression));
      } else if (identifier instanceof Function) {
        Function function = (Function) identifier;
        globals.add(this.markup.renderFunctionDeclaration(function.name, function.parameters.toMap(), function.expression));
      } else if (identifier instanceof Dictionary) {
        Dictionary dictionary = (Dictionary) identifier;
        globals.add(this.markup.renderDictionaryDeclaration(dictionary.name, dictionary.pathExpression, dictionary.keyExpression));
      }
    }

    List<Markup> fragments = new ArrayList<>();
    List<Markup> mainSection = renderSection(TemplateSection.DEFAULT, bodySectionRoot, fragments);
    List<Markup> summarySection = renderSection(TemplateSection.SUMMARY, summarySectionRoot, fragments);
    List<Markup> navigationSection = renderSection(TemplateSection.NAVIGATION, navigationSectionRoot, fragments);
    Markup file = this.markup.composeOutputFile(globals, mainSection, summarySection, navigationSection, fragments);
    this.stack.push(file);
  }

  private List<Markup> renderSection(TemplateSection section, ContentBlock sectionRoot, List<Markup> templates) {
    List<Markup> markupList = new ArrayList<>();
    this.translatorContext.setCurrentSection(section);
    for (ContentBlock block : sectionRoot.getChildren()) {
      if (!(block instanceof TemplateDefinition)) {
        markupList.add(block.renderInvocation(markup, this.translatorContext));
      }
      templates.addAll(block.renderDefinition(markup, this.translatorContext));
    }
    return markupList;
  }

  // #endregion Template File -------------------------------------------------
  
  // #region Source template blocks -------------------------------------------

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    Markup template = ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    String text = ctx.textBlock() != null ? ctx.textBlock().getText() : "";
    this.stack.push(this.markup.renderFreeText(this.markup.escapeSpecialCharacters(text), this.translatorContext).join(template));
  }

  @Override
  public void exitLinkedTextTemplate(LinkedTextTemplateContext ctx) {
    Markup template = ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    Markup text = this.stack.pop(Markup.class);
    this.stack.push(text.join(template));
  }

  @Override
  public void exitLabelTemplate(LabelTemplateContext ctx) {
    Markup template = ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    Markup label = ctx.labelBlock() != null ? this.stack.pop(Markup.class) : Markup.empty();
    this.stack.push(label.join(template));
  }

  @Override
  public void exitLinkedLabelTemplate(LinkedLabelTemplateContext ctx) {
    Markup template = ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    Markup label = this.stack.pop(Markup.class);
    this.stack.push(label.join(template));
  }

  @Override
  public void exitExpressionTemplate(ExpressionTemplateContext ctx) {
    Markup template = ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    Expression expression = this.stack.pop(Expression.class);
    this.stack.push(this.markup.renderVariableExpression(expression, this.translatorContext).join(template));
  }

  @Override
  public void exitLinkedExpressionTemplate(LinkedExpressionTemplateContext ctx) {
    Markup template = ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    Markup link = this.stack.pop(Markup.class);
    this.stack.push(link.join(template));
  }

  // #region New in EFX-2: Secondary templates --------------------------------

  @Override
  public void exitSecondaryTemplate(SecondaryTemplateContext ctx) {
    Markup template = ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    this.stack.push(this.markup.renderLineBreak(this.translatorContext).join(template));
  }

  // #endregion New in EFX-2: Secondary templates -----------------------------

  // #endregion Source template blocks ----------------------------------------
  
  // #region Label Blocks #{...} ----------------------------------------------

  @Override
  public void exitStandardLabelReference(StandardLabelReferenceContext ctx) {
    if (!this.stack.empty() && StringSequenceExpression.class.isAssignableFrom(this.stack.peek().getClass()) && ctx.assetId() != null) {

      // TODO: Review this in EFX-2

      // This is a workaround that allows EFX 1 to render a sequence of labels without a special
      // syntax. When a standard label reference is processed, the template translator checks if
      // the assetId is provided with a SequenceExpression. If this is the case, then the
      // translator generates the appropriate code to render a sequence of labels.
      //
      // For example, this will render a sequence of labels for a label reference of the form
      // #{assetType|labelType|${for text:$t in ('assetId1','assetId2') return $t}}:} 
      // The only restriction is that the assetType and labelType must be the same for all labels in the sequence.

      StringSequenceExpression assetIdSequence = this.stack.pop(StringSequenceExpression.class);
      this.exitStandardLabelReference(ctx, assetIdSequence);  // TODO: pluralisation is not addressed here yet
    } else {

      // Standard implementation as originally intended by EFX 1

      // New in EFX-2: Pluralisation of labels based on a supplied quantity
      NumericExpression quantity = ctx.pluraliser() != null ? this.stack.pop(NumericExpression.class)
          : NumericExpression.empty();

      StringExpression assetId = ctx.assetId() != null ? this.stack.pop(StringExpression.class)
          : this.script.getStringLiteralFromUnquotedString("");
      this.exitStandardLabelReference(ctx, assetId, quantity);
    }
  }
  
  /**
   * Renders a single label from a standard label reference.
   * 
   * @param ctx     The ParserRuleContext of the standard label reference.
   * @param assetId The assetId of the label to render.
   */
  private void exitStandardLabelReference(StandardLabelReferenceContext ctx, StringExpression assetId, NumericExpression quantity) {
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    StringExpression assetType = ctx.assetType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");

    this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
        List.of(assetType, this.script.getStringLiteralFromUnquotedString("|"), labelType,
            this.script.getStringLiteralFromUnquotedString("|"), assetId)), quantity, this.translatorContext));
  }

  /**
   * Renders a sequence of labels from a standard label reference.
   * 
   * @param ctx             The ParserRuleContext of the standard label reference.
   * @param assetIdSequence The sequence of assetIds for the labels to render.
   */
  private void exitStandardLabelReference(StandardLabelReferenceContext ctx, StringSequenceExpression assetIdSequence) {
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    StringExpression assetType = ctx.assetType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");

    Variable loopVariable = new Variable("item",
        this.script.composeVariableDeclaration("item", StringExpression.class), StringExpression.empty(),
        this.script.composeVariableReference("item", StringExpression.class));

    this.stack.push(this.markup.renderLabelFromExpression(
        this.script.composeDistinctValuesFunction(
            this.script.composeForExpression(
                this.script.composeIteratorList(
                    List.of(
                        this.script.composeIteratorExpression(loopVariable.declarationExpression, assetIdSequence))),
                this.script.composeStringConcatenation(List.of(
                    assetType,
                    this.script.getStringLiteralFromUnquotedString("|"),
                    labelType,
                    this.script.getStringLiteralFromUnquotedString("|"),
                    new StringExpression(loopVariable.referenceExpression.getScript()))),
                StringSequenceExpression.class),
            StringSequenceExpression.class), this.translatorContext));
  }


  @Override
  public void exitShorthandBtLabelReference(ShorthandBtLabelReferenceContext ctx) {
    NumericExpression quantity = ctx.pluraliser() != null ? this.stack.pop(NumericExpression.class) : NumericExpression.empty();
    StringExpression assetId = this.script.getStringLiteralFromUnquotedString(ctx.BtId().getText());
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
        List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_BT),
            this.script.getStringLiteralFromUnquotedString("|"), labelType,
            this.script.getStringLiteralFromUnquotedString("|"), assetId)), quantity, this.translatorContext));
  }

  @Override
  public void exitShorthandFieldLabelReference(ShorthandFieldLabelReferenceContext ctx) {
    final String fieldId = ctx.FieldId().getText();
    NumericExpression quantity = ctx.pluraliser() != null ? this.stack.pop(NumericExpression.class) : NumericExpression.empty();
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");

    if (labelType.getScript().equals("value")) {
      this.shorthandIndirectLabelReference(ctx, fieldId, quantity);
    } else {
      this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
          List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
              this.script.getStringLiteralFromUnquotedString("|"), labelType,
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(fieldId))), quantity, this.translatorContext));
    }
  }

  @Override
  public void exitShorthandIndirectLabelReference(ShorthandIndirectLabelReferenceContext ctx) {
    // New in EFX-2: Pluralisation of labels based on a supplied quantity
    NumericExpression quantity = ctx.pluraliser() != null ? this.stack.pop(NumericExpression.class) : NumericExpression.empty();
    this.shorthandIndirectLabelReference(ctx, ctx.FieldId().getText(), quantity);
  }

  private void shorthandIndirectLabelReference(ParserRuleContext ctx, final String fieldId, final NumericExpression quantity) {
    final Context currentContext = this.efxContext.peek();
    final String fieldType = this.symbols.getTypeOfField(fieldId);
    final PathExpression valueReference = this.symbols.isAttributeField(fieldId)
        ? this.script.composeFieldAttributeReference(
            this.script.contextualizePath(this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId),
                currentContext.absolutePath()),
            this.symbols.getAttributeNameFromAttributeField(fieldId), StringPath.class)
        : this.script.composeFieldValueReference(
        this.symbols.getRelativePathOfField(fieldId, currentContext.symbol()));
    Variable loopVariable = new Variable("item",
        this.script.composeVariableDeclaration("item", StringExpression.class), StringExpression.empty(),
        this.script.composeVariableReference("item", StringExpression.class));
    switch (fieldType) {
      case "indicator":
        this.stack.push(this.markup.renderLabelFromExpression(
            this.script.composeDistinctValuesFunction(
                this.script.composeForExpression(
                    this.script.composeIteratorList(
                        List.of(
                            this.script.composeIteratorExpression(loopVariable.declarationExpression, valueReference.asSequence()))),
                    this.script.composeStringConcatenation(
                        List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_INDICATOR),
                            this.script.getStringLiteralFromUnquotedString("|"),
                            this.script.getStringLiteralFromUnquotedString(LABEL_TYPE_WHEN),
                            this.script.getStringLiteralFromUnquotedString("-"),
                            new StringExpression(loopVariable.referenceExpression.getScript()),
                            this.script.getStringLiteralFromUnquotedString("|"),
                            this.script.getStringLiteralFromUnquotedString(fieldId))),
                    StringSequenceExpression.class),
                StringSequenceExpression.class), quantity, this.translatorContext));
        break;
      case "code":
      case "internal-code":
        this.stack.push(this.markup.renderLabelFromExpression(
            this.script.composeDistinctValuesFunction(
                this.script.composeForExpression(
                    this.script.composeIteratorList(
                        List.of(
                            this.script.composeIteratorExpression(loopVariable.declarationExpression, valueReference.asSequence()))),
                    this.script.composeStringConcatenation(List.of(
                        this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_CODE),
                        this.script.getStringLiteralFromUnquotedString("|"),
                        this.script.getStringLiteralFromUnquotedString(LABEL_TYPE_NAME),
                        this.script.getStringLiteralFromUnquotedString("|"),
                        this.script.getStringLiteralFromUnquotedString(
                            this.symbols.getRootCodelistOfField(fieldId)),
                        this.script.getStringLiteralFromUnquotedString("."),
                        new StringExpression(loopVariable.referenceExpression.getScript()))),
                    StringSequenceExpression.class),
                StringSequenceExpression.class),
            quantity, this.translatorContext));
        break;
      default:
        throw InvalidUsageException.shorthandRequiresCodeOrIndicator(ctx, fieldId, fieldType);
    }
  }

  /**
   * Handles the #{labelType} shorthand syntax which renders the label of the Field or Node declared
   * as the context.
   * 
   * If the labelType is 'value', then the label depends on the field's value and is rendered
   * according to the field's type. The assetType is inferred from the Field or Node declared as
   * context.
   */
  @Override
  public void exitShorthandLabelReferenceFromContext(ShorthandLabelReferenceFromContextContext ctx) {

    // New in EFX-2: Pluralisation of labels based on a supplied quantity
    NumericExpression quantity = ctx.pluraliser() != null ? this.stack.pop(NumericExpression.class) : NumericExpression.empty();

    final String labelType = ctx.LabelType().getText();
    if (this.efxContext.isFieldContext()) {
      if (labelType.equals(SHORTHAND_CONTEXT_FIELD_LABEL_REFERENCE)) {
        this.shorthandIndirectLabelReference(ctx, this.efxContext.symbol(), quantity);
      } else {
        this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
            List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(labelType),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(this.efxContext.symbol()))), quantity, this.translatorContext));
      }
    } else if (this.efxContext.isNodeContext()) {
      this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
          List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_NODE),
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(labelType),
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(this.efxContext.symbol()))), quantity, this.translatorContext));
    }
  }

  /**
   * Handles the #value shorthand syntax which renders the label corresponding to the value of the
   * the field declared as the context of the current line of the template. This shorthand syntax is
   * only supported for fields of type 'code' or 'indicator'.
   */
  @Override
  public void exitShorthandIndirectLabelReferenceFromContextField(
      ShorthandIndirectLabelReferenceFromContextFieldContext ctx) {
    if (!this.efxContext.isFieldContext()) {
      throw InvalidUsageException.shorthandRequiresFieldContext(ctx, "#value");
    }
    this.shorthandIndirectLabelReference(ctx, this.efxContext.symbol(), NumericExpression.empty());
  }

  @Override
  public void exitAssetType(AssetTypeContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(this.script.getStringLiteralFromUnquotedString(ctx.getText()));
    }
  }

  @Override
  public void exitLabelType(LabelTypeContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(this.script.getStringLiteralFromUnquotedString(ctx.getText()));
    }
  }

  @Override
  public void exitAssetId(AssetIdContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(this.script.getStringLiteralFromUnquotedString(ctx.getText()));
    }
  }
 
  // #region New in EFX-2 -----------------------------------------------------

  @Override
  public void exitComputedLabelReference(ComputedLabelReferenceContext ctx) {
    NumericExpression quantity = ctx.pluraliser() != null ? this.stack.pop(NumericExpression.class) : NumericExpression.empty();
    StringExpression expression = this.stack.pop(StringExpression.class);
    this.stack.push(this.markup.renderLabelFromExpression(expression, quantity, this.translatorContext));
  }


  @Override
  public void exitDictionaryDeclaration(DictionaryDeclarationContext ctx) {
    String name = ctx.dictionaryName.getText();
    StringExpression key = this.stack.pop(StringExpression.class);
    var match = this.stack.pop(PathExpression.class);

    // Declare the dictionary in the script
    this.stack.declareGlobalIdentifier(new Dictionary(name, match, key));
  }

  @Override
  public void exitDictionaryIndexClause(DictionaryIndexClauseContext ctx) {
    this.efxContext.pushFieldContext(getFieldId(ctx.fieldContext()));
  }

  @Override
  public void exitDictionaryKeyClause(DictionaryKeyClauseContext ctx) {
    this.efxContext.pop();
  }

  // #endregion New in EFX-2 --------------------------------------------------

  // #endregion Label Blocks #{...} -------------------------------------------
  
  // #region Expression Blocks ${...} -----------------------------------------

  /**
   * Handles a standard expression block in a template line. Most of the work is done by the base
   * class EfxExpressionTranslator. After the expression is translated, the result is passed through
   * the renderer.
   */
  @Override
  public void exitStandardExpressionBlock(StandardExpressionBlockContext ctx) {
    var expression = this.stack.pop(Expression.class);

    // Implicit formatting: date and time expressions in template blocks are automatically
    // formatted using format-short for display.
    // Users can override by using explicit format-short/format-medium/format-long in the expression.
    if (TypedExpression.class.isAssignableFrom(expression.getClass())) {
      if (EfxDataType.Date.class.isAssignableFrom(((TypedExpression) expression).getDataType())) {

        var loopVariable = new Variable("item",
            this.script.composeVariableDeclaration("item", DateExpression.class), DateExpression.empty(),
            this.script.composeVariableReference("item", DateExpression.class));

        expression = this.script.composeForExpression(
            this.script.composeIteratorList(
                List.of(this.script.composeIteratorExpression(loopVariable.declarationExpression,
                    new DateSequenceExpression(expression.getScript())))),
            this.script.composeFormatDateShort(new DateExpression(loopVariable.referenceExpression.getScript())),
            StringSequenceExpression.class);
      } else if (EfxDataType.Time.class.isAssignableFrom(((TypedExpression) expression).getDataType())) {

        var loopVariable = new Variable("item",
            this.script.composeVariableDeclaration("item", TimeExpression.class), TimeExpression.empty(),
            this.script.composeVariableReference("item", TimeExpression.class));

        expression = this.script.composeForExpression(
            this.script.composeIteratorList(
                List.of(this.script.composeIteratorExpression(loopVariable.declarationExpression,
                    new TimeSequenceExpression(expression.getScript())))),
            this.script.composeFormatTimeShort(new TimeExpression(loopVariable.referenceExpression.getScript())),
            StringSequenceExpression.class);
      }
    }

    this.stack.push(expression);
  }

  // #region Formatting functions (template-only) --------------------------------

  @Override
  public void exitFormatShortDateFunction(FormatShortDateFunctionContext ctx) {
    this.stack.push(this.script.composeFormatDateShort(this.stack.pop(DateExpression.class)));
  }

  @Override
  public void exitFormatShortTimeFunction(FormatShortTimeFunctionContext ctx) {
    this.stack.push(this.script.composeFormatTimeShort(this.stack.pop(TimeExpression.class)));
  }

  @Override
  public void exitFormatShortDateTimeFunction(FormatShortDateTimeFunctionContext ctx) {
    TimeExpression time = this.stack.pop(TimeExpression.class);
    DateExpression date = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeStringConcatenation(List.of(
        this.script.composeFormatDateShort(date),
        this.script.getStringLiteralFromUnquotedString(" "),
        this.script.composeFormatTimeShort(time))));
  }

  @Override
  public void exitFormatMediumDateFunction(FormatMediumDateFunctionContext ctx) {
    this.stack.push(this.script.composeFormatDateMedium(this.stack.pop(DateExpression.class)));
  }

  @Override
  public void exitFormatMediumTimeFunction(FormatMediumTimeFunctionContext ctx) {
    this.stack.push(this.script.composeFormatTimeMedium(this.stack.pop(TimeExpression.class)));
  }

  @Override
  public void exitFormatMediumDateTimeFunction(FormatMediumDateTimeFunctionContext ctx) {
    TimeExpression time = this.stack.pop(TimeExpression.class);
    DateExpression date = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeStringConcatenation(List.of(
        this.script.composeFormatDateMedium(date),
        this.script.getStringLiteralFromUnquotedString(" "),
        this.script.composeFormatTimeMedium(time))));
  }

  @Override
  public void exitFormatLongDateFunction(FormatLongDateFunctionContext ctx) {
    this.stack.push(this.script.composeFormatDateLong(this.stack.pop(DateExpression.class)));
  }

  @Override
  public void exitFormatLongTimeFunction(FormatLongTimeFunctionContext ctx) {
    this.stack.push(this.script.composeFormatTimeLong(this.stack.pop(TimeExpression.class)));
  }

  @Override
  public void exitFormatLongDateTimeFunction(FormatLongDateTimeFunctionContext ctx) {
    TimeExpression time = this.stack.pop(TimeExpression.class);
    DateExpression date = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeStringConcatenation(List.of(
        this.script.composeFormatDateLong(date),
        this.script.getStringLiteralFromUnquotedString(" "),
        this.script.composeFormatTimeLong(time))));
  }

  // #endregion Formatting functions ---------------------------------------------

  // #region Preferred language functions ----------------------------------------

  @Override
  public void exitPreferredLanguageFunction(PreferredLanguageFunctionContext ctx) {
    this.stack.push(this.script.getPreferredLanguage(this.stack.pop(MultilingualStringPath.class)));
  }

  @Override
  public void exitPreferredLanguageTextFunction(PreferredLanguageTextFunctionContext ctx) {
    this.stack.push(this.script.getTextInPreferredLanguage(this.stack.pop(MultilingualStringPath.class)));
  }

  // #endregion Preferred language functions -------------------------------------

  /***
   * Handles the $value shorthand syntax which renders the value of the field declared as context in
   * the current line of the template.
   */
  @Override
  public void exitShorthandFieldValueReferenceFromContextField(
      ShorthandFieldValueReferenceFromContextFieldContext ctx) {
    if (!this.efxContext.isFieldContext()) {
      throw InvalidUsageException.shorthandRequiresFieldContext(ctx, "$value");
    }
    this.stack.push(this.script.composeFieldValueReference(
        this.symbols.getRelativePathOfField(this.efxContext.symbol(), this.efxContext.symbol())));
  }

  // #endregion Expression Blocks ${...} --------------------------------------
  
  // #region Context Declaration Blocks {...} ---------------------------------

  // #region New in EFX-2 -----------------------------------------------------

  @Override
  public void exitContextDeclaration(ContextDeclarationContext ctx) {
    String shortcut = ctx.shortcut != null ? ctx.shortcut.getText() : "none";
    switch (shortcut) {
      case ".":
        this.exitSameContextDeclaration();
        break;
      case "..":
        this.exitParentContextDeclaration();
        break;
      case "/":
        this.exitRootContextDeclaration();
        break;
      default:
        PathExpression contextPath = this.stack.pop(PathExpression.class);
        if (ctx.fieldContext() != null) {
          String fieldId = getFieldId(ctx.fieldContext());
          assert fieldId != null : "We should have been able to locate the FieldId declared as context.";
          this.exitFieldContextDeclaration(fieldId, contextPath, null);
        } else if (ctx.contextVariableInitializer() != null) {
          Variable contextVariable = this.getContextVariable(ctx.contextVariableInitializer(), contextPath);
          assert contextVariable != null : "We should have been able to locate the ContextVariable declared as context.";
          this.stack.peek(Variables.class).add(contextVariable);
          if (ctx.contextVariableInitializer().fieldContext() != null) {
            String fieldId = getFieldId(ctx.contextVariableInitializer().fieldContext());
            this.exitFieldContextDeclaration(fieldId, contextPath, contextVariable);
          } else if (ctx.contextVariableInitializer().nodeContext() != null) {
            String nodeId = getNodeId(ctx.contextVariableInitializer().nodeContext());
            assert nodeId != null : "We should have been able to locate the NodeId declared as context.";
            this.exitNodeContextDeclaration(nodeId, contextPath, contextVariable);
          }
        } else if (ctx.nodeContext() != null) {
          String nodeId = getNodeId(ctx.nodeContext());
          assert nodeId != null : "We should have been able to locate the NodeId declared as context.";
          this.exitNodeContextDeclaration(nodeId, contextPath, null);
        }
        break;
    }
  }

  private void exitSameContextDeclaration() {
    Context currentContext = this.blockStack.currentContext();

    // Verify that efxContext.peek() gives the same result as blockStack.currentContext()
    assert this.efxContext.isEmpty() || currentContext.symbol().equals(this.efxContext.peek().symbol())
        : "Context mismatch: blockStack.currentContext()=" + currentContext.symbol()
            + " but efxContext.peek()=" + this.efxContext.peek().symbol();

    PathExpression contextPath = currentContext.absolutePath();
    String symbol = currentContext.symbol();
    if (currentContext.isFieldContext()) {
      this.exitFieldContextDeclaration(symbol, contextPath, null);
    } else if (currentContext.isNodeContext()) {
      this.exitNodeContextDeclaration(symbol, contextPath, currentContext.variable());
    }
  }

  private void exitParentContextDeclaration() {
    Context parentContext = this.blockStack.parentContext();

    // Verify that efxContext.peekParent() gives the same result as blockStack.parentContext()
    Context efxParentContext = this.efxContext.peekParentContext();
    assert efxParentContext == null || parentContext.symbol().equals(efxParentContext.symbol())
        : "Context mismatch: blockStack.parentContext()=" + parentContext.symbol()
            + " but efxContext.peekParent()=" + efxParentContext.symbol();

    PathExpression contextPath = parentContext.absolutePath();
    String symbol = parentContext.symbol();
    if (parentContext.isFieldContext()) {
      this.exitFieldContextDeclaration(symbol, contextPath, null);
    } else if (parentContext.isNodeContext()) {
      this.exitNodeContextDeclaration(symbol, contextPath, parentContext.variable());
    }
  }

  private void exitRootContextDeclaration() {
    this.exitNodeContextDeclaration(this.symbols.getRootNodeId(), this.symbols.getRootPath(), null);
  }

  private void exitFieldContextDeclaration(String fieldId, PathExpression contextPath, Variable contextVariable) {
    var context = new FieldContext(fieldId, contextPath, contextVariable);
    this.efxContext.push(context);
    if (contextVariable != null) {
      this.stack.declareIdentifier(contextVariable);
      this.efxContext.declareContextVariable(contextVariable.name, context);
    }
  }

  private void exitNodeContextDeclaration(String nodeId, PathExpression contextPath, Variable contextVariable) {
    var context = new NodeContext(nodeId, contextPath, contextVariable);
    this.efxContext.push(context);
    if (contextVariable != null) {
      this.stack.declareIdentifier(contextVariable);
      this.efxContext.declareContextVariable(contextVariable.name, context);
    }
  }

  private Variable getContextVariable(ContextVariableInitializerContext ctx,
      PathExpression contextPath) {
    if (ctx == null) {
      return null;
    }

    final String variableName = ctx.variableName.getText();
    final Class<? extends TypedExpression> variableType = contextPath.asScalar().getClass();

    return new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, variableType),
        this.script.contextualizePath(contextPath, contextPath),
        this.script.composeVariableReference(variableName, variableType));

  }

  // #region Conditional template blocks --------------------------------------


  @Override
  public void enterChooseTemplate(ChooseTemplateContext ctx) {
      this.stack.push(new Conditionals());
  }

  @Override
  public void exitWhenDisplayTemplate(WhenDisplayTemplateContext ctx) {
    var template = this.stack.pop(Markup.class);
    var condition = this.stack.pop(BooleanExpression.class);
    this.stack.peek(Conditionals.class).add(new Conditional(condition, template));
  }

  @Override
  public void exitWhenInvokeTemplate(WhenInvokeTemplateContext ctx) {
    var templateMarkup = this.stack.pop(Markup.class);
    var condition = this.stack.pop(BooleanExpression.class);
    this.stack.peek(Conditionals.class).add(new Conditional(condition, templateMarkup));
  }

  @Override
  public void enterInvokeTemplate(InvokeTemplateContext ctx) {
    final Template template = this.stack.getTemplate(ctx.templateName.getText());
    this.stack.push(new StrictArguments(template));
  }

  @Override
  public void exitInvokeTemplate(InvokeTemplateContext ctx) {
    final String templateName = ctx.templateName.getText();
    final StrictArguments arguments = this.stack.pop(StrictArguments.class);
    final Set<Argument> args = arguments.stream()
        .map(a -> new Argument.Impl(a.name, this.markup.getEfxDataTypeEquivalent(a.dataType), a.value))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    this.stack.push(this.markup.renderFragmentInvocation(templateName, args, this.translatorContext));
  }
  
  // #endregion Conditional template blocks ---------------------------------

  // #region Variable Initializers --------------------------------------------

  @Override
  public void exitStringVariableInitializer(StringVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), StringExpression.class);
  }

  @Override
  public void exitBooleanVariableInitializer(BooleanVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), BooleanExpression.class);
  }

  @Override
  public void exitNumericVariableInitializer(NumericVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), NumericExpression.class);
  }

  @Override
  public void exitDateVariableInitializer(DateVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), DateExpression.class);
  }

  @Override
  public void exitTimeVariableInitializer(TimeVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), TimeExpression.class);
  }

  @Override
  public void exitDurationVariableInitializer(DurationVariableInitializerContext ctx) {
    this.exitVariableInitializer(ctx.variableName.getText(), DurationExpression.class);
  }

  private void exitVariableInitializer(
      String variableName, Class<? extends ScalarExpression> expressionType) {
    var expression = this.stack.pop(expressionType);
    var variable = new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, expression.getClass()),
        expression,
        this.script.composeVariableReference(variableName, expression.getClass()));
    this.stack.push(variable);
  }

  @Override
  public void exitVariableInitializer(VariableInitializerContext arg0) {
    var variable = this.stack.pop(Variable.class);
    this.stack.declareIdentifier(variable);
    this.stack.peek(Variables.class).add(variable);
  }

  // Sequence variable initializers

  @Override
  public void exitStringSequenceVariableInitializer(StringSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), StringSequenceExpression.class);
  }

  @Override
  public void exitNumericSequenceVariableInitializer(NumericSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), NumericSequenceExpression.class);
  }

  @Override
  public void exitBooleanSequenceVariableInitializer(BooleanSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), BooleanSequenceExpression.class);
  }

  @Override
  public void exitDateSequenceVariableInitializer(DateSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSequenceVariableInitializer(TimeSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSequenceVariableInitializer(DurationSequenceVariableInitializerContext ctx) {
    this.exitSequenceVariableInitializer(ctx.variableName.getText(), DurationSequenceExpression.class);
  }

  private void exitSequenceVariableInitializer(
      String variableName, Class<? extends SequenceExpression> expressionType) {
    var expression = this.stack.pop(expressionType);
    var variable = new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, expression.getClass()),
        expression,
        this.script.composeVariableReference(variableName, expression.getClass()));
    this.stack.push(variable);
  }

  // #endregion Variable Initializers -----------------------------------------

  // #region Hyperlinks -------------------------------------------------------

  @Override
  public void exitLinkedTextBlock(LinkedTextBlockContext ctx) {
    var url = this.stack.pop(StringExpression.class);
    var text = this.markup.renderFreeText(ctx.textBlock().getText(), this.translatorContext);
    this.stack.push(this.markup.renderHyperlink(text, url, this.translatorContext));
  }

  @Override
  public void exitLinkedLabelBlock(LinkedLabelBlockContext ctx) {
    var url = this.stack.pop(StringExpression.class);
    var text = this.stack.pop(Markup.class);
    this.stack.push(this.markup.renderHyperlink(text, url, this.translatorContext));
  }

  @Override
  public void exitLinkedExpressionBlock(LinkedExpressionBlockContext ctx) {
    var url = this.stack.pop(StringExpression.class);
    var text = this.markup.renderVariableExpression(this.stack.pop(Expression.class), this.translatorContext);
    this.stack.push(this.markup.renderHyperlink(text, url, this.translatorContext));
  }

  // #endregion Hyperlinks ----------------------------------------------------

  // #endregion New in EFX-2 --------------------------------------------------

  @Override
  public void enterContextDeclarationBlock(ContextDeclarationBlockContext arg0) {
    this.stack.push(new Variables());
  }




  // #endregion Context Declaration Blocks {...} ------------------------------
  
  // #region Parameter Declarations -------------------------------------------

  @Override
  public void exitStringParameterDeclaration(StringParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), StringExpression.class);
  }

  @Override
  public void exitNumericParameterDeclaration(NumericParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), NumericExpression.class);
  }

  @Override
  public void exitBooleanParameterDeclaration(BooleanParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), BooleanExpression.class);
  }

  @Override
  public void exitDateParameterDeclaration(DateParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), DateExpression.class);
  }

  @Override
  public void exitTimeParameterDeclaration(TimeParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), TimeExpression.class);
  }

  @Override
  public void exitDurationParameterDeclaration(DurationParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), DurationExpression.class);
  }

  // Sequence parameter declarations

  @Override
  public void exitStringSequenceParameterDeclaration(StringSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), StringSequenceExpression.class);
  }

  @Override
  public void exitNumericSequenceParameterDeclaration(NumericSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), NumericSequenceExpression.class);
  }

  @Override
  public void exitBooleanSequenceParameterDeclaration(BooleanSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), BooleanSequenceExpression.class);
  }

  @Override
  public void exitDateSequenceParameterDeclaration(DateSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSequenceParameterDeclaration(TimeSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSequenceParameterDeclaration(DurationSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), DurationSequenceExpression.class);
  }

  private void exitParameterDeclaration(String parameterName, Class<? extends TypedExpression> parameterType) {
    ParsedParameter parameter = new ParsedParameter(parameterName,
        this.script.composeParameterReference(parameterName, parameterType));
    this.stack.declareIdentifier(parameter);
    this.stack.peek(ParsedParameters.class).add(parameter);
  }

  // #endregion Parameter Declarations ----------------------------------------

  @Override
  public void enterSummarySection(SummarySectionContext ctx) {
    this.rootBlock = this.summarySectionRoot;
    while (!this.blockStack.isEmpty()) {
      this.blockStack.pop();
    }
    this.translatorContext.setCurrentSection(TemplateSection.SUMMARY);
  }

  @Override
    public void exitSummarySection(SummarySectionContext ctx) {

      this.translatorContext.setCurrentSection(TemplateSection.DEFAULT);
    }

  @Override
  public void enterNavigationSection(NavigationSectionContext ctx) {
    this.rootBlock = this.navigationSectionRoot;
    while (!this.blockStack.isEmpty()) {
      this.blockStack.pop();
    }
    this.translatorContext.setCurrentSection(TemplateSection.NAVIGATION);
  }

  @Override
  public void exitNavigationSection(NavigationSectionContext ctx) {
      this.translatorContext.setCurrentSection(TemplateSection.DEFAULT);
  }

  // #region Template lines  --------------------------------------------------

  @Override
  public void enterTemplateLine(TemplateLineContext ctx) {
    this.enterTemplateLine(ctx, this.getIndentLevel(ctx.indentation()));
    if (ctx.contextDeclarationBlock() == null) {
      this.exitRootContextDeclaration();
      this.stack.push(new Variables());
    }
  }

  @Override
  public void enterTemplateDeclaration(TemplateDeclarationContext ctx) {
    this.enterTemplateLine(ctx, 0);
  }

  private void enterTemplateLine(ParserRuleContext ctx, final int indentLevel) {
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    if (indentChange > 1) {
      throw InvalidIndentationException.indentationLevelSkipped(ctx);
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero(ctx);
      }
      this.stack.pushStackFrame(); // Create a stack frame for the new template line.
    } else if (indentChange < 0) {
      for (int i = indentChange; i < 0; i++) {
        assert !this.blockStack.isEmpty() : UNEXPECTED_INDENTATION;
        assert this.blockStack.currentIndentationLevel() > indentLevel : UNEXPECTED_INDENTATION;
        this.blockStack.pop();
        this.stack.popStackFrame(); // Each skipped indentation level must go out of scope.
      }
      this.stack.popStackFrame(); // The previous sibling goes out of scope (same indentation
                                  // level).
      this.stack.pushStackFrame(); // Create a stack frame for the new template line.
      assert this.blockStack.currentIndentationLevel() == indentLevel : UNEXPECTED_INDENTATION;
    } else if (indentChange == 0) {
      this.stack.popStackFrame(); // The previous sibling goes out of scope (same indentation
                                  // level).
      this.stack.pushStackFrame(); // Create a stack frame for the new template line.
    }
  }

  @Override
  public void exitTemplateLine(TemplateLineContext ctx) {
    final Context lineContext = this.efxContext.pop();
    final int indentLevel = this.getIndentLevel(ctx.indentation());
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    final Markup defaultContent = this.stack.peek() instanceof Markup ? this.stack.pop(Markup.class) : Markup.empty();
    final Conditionals conditionals = this.stack.peek() instanceof Conditionals ? this.stack.pop(Conditionals.class) : new Conditionals();
    final Variables variables = this.stack.pop(Variables.class);
    final Integer outlineNumber =
        ctx.OutlineNumber() != null ? Integer.parseInt(ctx.OutlineNumber().getText().trim()) : -1;
    assert this.stack.empty() : "Stack should be empty at this point.";

    if (indentChange > 1) {
      throw InvalidIndentationException.indentationLevelSkipped(ctx);
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero(ctx);
      }
      if (this.blockStack.peek() instanceof TemplateInvocation) {
        throw InvalidIndentationException.noNestingOnInvocations(ctx);
      }
        this.blockStack.pushChild(outlineNumber, this.relativizeContext(lineContext, this.blockStack.currentContext()), variables,
            conditionals, defaultContent);
    } else if (indentChange < 0) {
        this.blockStack.pushSibling(outlineNumber, this.relativizeContext(lineContext, this.blockStack.parentContext()), variables,
            conditionals, defaultContent);
    } else if (indentChange == 0) {
      if (blockStack.isEmpty()) {
        assert indentLevel == 0 : UNEXPECTED_INDENTATION;
        this.blockStack.push(this.rootBlock.addChild(outlineNumber, this.relativizeContext(lineContext, this.rootBlock.getContext()), variables,
            conditionals, defaultContent));
    } else {
          this.blockStack.pushSibling(outlineNumber, this.relativizeContext(lineContext, this.blockStack.parentContext()), variables,
              conditionals, defaultContent);
      }
    }
  }

  @Override
  public void exitTemplateDeclaration(TemplateDeclarationContext ctx) {
    final Markup defaultContent = this.stack.pop(Markup.class);
    final Conditionals conditionals = this.stack.peek() instanceof Conditionals ? this.stack.pop(Conditionals.class) : new Conditionals();
    final Template template = this.stack.pop(Template.class);
    assert this.stack.empty() : "Stack should be empty at this point.";

    if (this.getIndentLevel(ctx.indentation()) != 0) {
      throw InvalidIndentationException.noIndentOnTemplateDeclarations(ctx);
    }

    this.blockStack
        .push(this.rootBlock.addChild(template.name, conditionals, defaultContent, template.parameters));
  }

  private Context relativizeContext(Context childContext, Context parentContext) {
    if (parentContext == null) {
      return childContext;
    }

    PathExpression parentContextAbsolutePath = parentContext.isFieldContext()
        ? this.symbols.getAbsolutePathOfField(parentContext.symbol())
        : this.symbols.getAbsolutePathOfNode(parentContext.symbol());

    if (childContext.isFieldContext()) {
      return new FieldContext(childContext.symbol(), childContext.absolutePath(),
          this.script.contextualizePath(childContext.absolutePath(), parentContextAbsolutePath), childContext.variable());
    }

    assert childContext.isNodeContext() : "Child context should be either a FieldContext or a NodeContext.";

    return new NodeContext(childContext.symbol(), childContext.absolutePath(),
        this.script.contextualizePath(childContext.absolutePath(), parentContextAbsolutePath));
  }

  // #endregion Template lines  -----------------------------------------------
  
  // #region Helpers ----------------------------------------------------------

    private int getIndentLevel(IndentationContext ctx) {

      if (ctx == null) {
        return 0; // No indentation, default to 0.
      }

      if (ctx.MixedIndent() != null) {
          throw InvalidIndentationException.mixedIndentation(ctx);
      }

      if (ctx.Spaces() != null) {
        if (this.indentWith == Indent.UNDETERMINED) {
          this.indentWith = Indent.SPACES;
          this.indentSpaces = ctx.Spaces().getText().length();
        } else if (this.indentWith == Indent.TABS) {
          throw InvalidIndentationException.mixedIndentation(ctx);
        }

        if (ctx.Spaces().getText().length() % this.indentSpaces != 0) {
          throw InvalidIndentationException.inconsistentSpaces(ctx, this.indentSpaces);
        }
        return ctx.Spaces().getText().length() / this.indentSpaces;
      } else if (ctx.Tabs() != null) {
        if (this.indentWith == Indent.UNDETERMINED) {
          this.indentWith = Indent.TABS;
        } else if (this.indentWith == Indent.SPACES) {
          throw InvalidIndentationException.mixedIndentation(ctx);
        }

        return ctx.Tabs().getText().length();
      }
      return 0;
    }

  // #endregion Helpers -------------------------------------------------------

  // #region Pre-processing -------------------------------------------------

  /**
   * This class is used to pre-process the template before it is actually translated. 
   * For details, see the comments in the base class {@link ExpressionPreprocessor}.
   */
  class TemplatePreprocessor extends ExpressionPreprocessor {

    TemplatePreprocessor(CharStream template, IncludedFileResolver resolver) throws IOException {
      super(new IncludeProcessor(resolver).resolve(template));
    }

    String processTemplate() {
      final ParseTree tree = parser.templateFile();
      final ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(this, tree);
      return this.rewriter.getText();
    }

    // #region Template Variables ---------------------------------------------
  
    @Override
    public void exitVariableDeclaration(VariableDeclarationContext arg0) {
      var variable = this.stack.pop(Variable.class);
      this.stack.declareGlobalIdentifier(variable);
    }

    @Override
    public void exitVariableInitializer(VariableInitializerContext arg0) {
      var variable = this.stack.pop(Variable.class);
      this.stack.declareIdentifier(variable);
    }

    @Override
    public void exitContextDeclaration(ContextDeclarationContext ctx) {
      // Handle context shortcuts (., .., /)
      String shortcut = ctx.shortcut != null ? ctx.shortcut.getText() : "none";
      switch (shortcut) {
        case ".":
          this.exitSameContextDeclaration();
          return;
        case "..":
          this.exitParentContextDeclaration();
          return;
        case "/":
          this.exitRootContextDeclaration();
          return;
        default:
          break;
      }

      // Handle field or node context without variable
      if (ctx.fieldContext() != null) {
        final String fieldId = getFieldId(ctx.fieldContext());
        this.efxContext.push(new FieldContext(fieldId, this.symbols.getAbsolutePathOfField(fieldId),
            this.symbols.getRelativePathOfField(fieldId, this.efxContext.symbol())));
        return;
      }
      if (ctx.nodeContext() != null) {
        final String nodeId = getNodeId(ctx.nodeContext());
        this.efxContext.push(new NodeContext(nodeId, this.symbols.getAbsolutePathOfNode(nodeId),
            this.symbols.getRelativePathOfNode(nodeId, this.efxContext.symbol())));
        return;
      }

      // Handle context variable initializer
      final var initializer = ctx.contextVariableInitializer();
      if (initializer == null) {
        return;
      }
      final String variableName = initializer.variableName.getText();
      if (initializer.fieldContext() != null) {
        final String fieldId = getFieldId(initializer.fieldContext());
        var fieldType = FieldTypes.fromString(this.symbols.getTypeOfField(fieldId));
        this.stack.declareIdentifier(
            new Variable(variableName, ScalarPath.empty(fieldType),
                ScalarPath.empty(fieldType), ScalarPath.empty(fieldType)));
        var context = new FieldContext(fieldId, this.symbols.getAbsolutePathOfField(fieldId),
            this.symbols.getRelativePathOfField(fieldId, this.efxContext.symbol()));
        this.efxContext.push(context);
        this.efxContext.declareContextVariable(variableName, context);
      }
      if (initializer.nodeContext() != null) {
        final String nodeId = getNodeId(initializer.nodeContext());
        this.stack.declareIdentifier(new Variable(variableName, NodePath.empty(),
            NodePath.empty(), NodePath.empty()));
        var context = new NodeContext(nodeId, this.symbols.getAbsolutePathOfNode(nodeId),
            this.symbols.getRelativePathOfNode(nodeId, this.efxContext.symbol()));
        this.efxContext.push(context);
        this.efxContext.declareContextVariable(variableName, context);
      }
    }

    private void exitSameContextDeclaration() {
      // "." means same context as parent - reuse the current top of efxContext
      // This mirrors the main translator's blockStack.currentContext() behavior
      if (this.efxContext.isEmpty()) {
        this.exitRootContextDeclaration();
        return;
      }
      Context currentContext = this.efxContext.peek();
      PathExpression contextPath = currentContext.absolutePath();
      String symbol = currentContext.symbol();
      if (currentContext.isFieldContext()) {
        this.efxContext.push(new FieldContext(symbol, contextPath));
      } else {
        Variable variable = currentContext.variable();
        if (variable != null) {
          this.efxContext.push(new NodeContext(symbol, contextPath, variable));
        } else {
          this.efxContext.push(new NodeContext(symbol, contextPath));
        }
      }
    }

    private void exitParentContextDeclaration() {
      // ".." means parent context - go one level up from current context
      // This mirrors the main translator's blockStack.parentContext() behavior
      Context parentContext = this.efxContext.peekParentContext();
      if (parentContext == null) {
        this.exitRootContextDeclaration();
        return;
      }
      PathExpression contextPath = parentContext.absolutePath();
      String symbol = parentContext.symbol();
      if (parentContext.isFieldContext()) {
        this.efxContext.push(new FieldContext(symbol, contextPath));
      } else {
        Variable variable = parentContext.variable();
        if (variable != null) {
          this.efxContext.push(new NodeContext(symbol, contextPath, variable));
        } else {
          this.efxContext.push(new NodeContext(symbol, contextPath));
        }
      }
    }

    private void exitRootContextDeclaration() {
      this.efxContext.push(new NodeContext(this.symbols.getRootNodeId(), this.symbols.getRootPath()));
    }

    @Override
    public void exitStringVariableInitializer(StringVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), StringExpression.empty(), StringExpression.empty()));
    }
  
    @Override
    public void exitBooleanVariableInitializer(BooleanVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), BooleanExpression.empty(), BooleanExpression.empty()));
    }
  
    @Override
    public void exitNumericVariableInitializer(NumericVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), NumericExpression.empty(), NumericExpression.empty()));
    }
  
    @Override
    public void exitDateVariableInitializer(DateVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), DateExpression.empty(), DateExpression.empty()));
    }
  
    @Override
    public void exitTimeVariableInitializer(TimeVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), TimeExpression.empty(), TimeExpression.empty()));
    }
  
    @Override
    public void exitDurationVariableInitializer(DurationVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), DurationExpression.empty(), DurationExpression.empty()));
    }

    // Sequence variable initializers

    @Override
    public void exitStringSequenceVariableInitializer(StringSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), new StringSequenceExpression(""), new StringSequenceExpression("")));
    }

    @Override
    public void exitNumericSequenceVariableInitializer(NumericSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), new NumericSequenceExpression(""), new NumericSequenceExpression("")));
    }

    @Override
    public void exitBooleanSequenceVariableInitializer(BooleanSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), new BooleanSequenceExpression(""), new BooleanSequenceExpression("")));
    }

    @Override
    public void exitDateSequenceVariableInitializer(DateSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), new DateSequenceExpression(""), new DateSequenceExpression("")));
    }

    @Override
    public void exitTimeSequenceVariableInitializer(TimeSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), new TimeSequenceExpression(""), new TimeSequenceExpression("")));
    }

    @Override
    public void exitDurationSequenceVariableInitializer(DurationSequenceVariableInitializerContext ctx) {
      this.stack.push(new Variable(ctx.variableName.getText(), new DurationSequenceExpression(""), new DurationSequenceExpression("")));
    }

    // #endregion Template Variables ------------------------------------------
    
    // #region Scope management --------------------------------------------

    Stack<Integer> levels = new Stack<>();

    @Override
    public void enterTemplateLine(TemplateLineContext ctx) {
      final int indentLevel = EfxTemplateTranslatorV2.this.getIndentLevel(ctx.indentation());
      this.enterTemplateLine(ctx, indentLevel);
      // Push root context if no context declaration block (same as main translator)
      if (ctx.contextDeclarationBlock() == null) {
        this.exitRootContextDeclaration();
      }
    }

    @Override
    public void enterTemplateDeclaration(TemplateDeclarationContext ctx) {
      this.enterTemplateLine(ctx, 0);
    }

    private void enterTemplateLine(ParserRuleContext ctx, final int indentLevel) {
      final int indentChange = indentLevel - (this.levels.isEmpty() ? 0 : this.levels.peek());
      if (indentChange > 1) {
        throw InvalidIndentationException.indentationLevelSkipped(ctx);
      } else if (indentChange == 1) {
        if (this.levels.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero(ctx);
        }
        this.stack.pushStackFrame(); // Create a stack frame for the new template line.
      } else if (indentChange < 0) {
        for (int i = indentChange; i < 0; i++) {
          assert !this.levels.isEmpty() : UNEXPECTED_INDENTATION;
          assert this.levels.peek() > indentLevel : UNEXPECTED_INDENTATION;
          this.levels.pop();
          this.stack.popStackFrame(); // Each skipped indentation level must go out of scope.
        }
        this.stack.popStackFrame();
        this.stack.pushStackFrame();
        assert this.levels.peek() == indentLevel : UNEXPECTED_INDENTATION;
      } else if (indentChange == 0) {
        this.stack.popStackFrame();
        this.stack.pushStackFrame();
      }
    }

    @Override
    public void exitTemplateLine(TemplateLineContext ctx) {
      // Pop context for this line (same as main translator)
      if (!this.efxContext.isEmpty()) {
        this.efxContext.pop();
      }
      final int indentLevel = EfxTemplateTranslatorV2.this.getIndentLevel(ctx.indentation());
      this.exitTemplateLine(ctx, indentLevel);
    }

    @Override
    public void exitTemplateDeclaration(TemplateDeclarationContext ctx) {
      // Note: Template declarations don't push context (they have parameters instead of context block)
      // So we don't pop context here, unlike regular template lines
      this.exitTemplateLine(ctx, 0);
    }

    private void exitTemplateLine(ParserRuleContext ctx, final int indentLevel) {
      final int indentChange = indentLevel - (this.levels.isEmpty() ? 0 : this.levels.peek());
      assert this.stack.empty() : "Stack should be empty at this point.";

      if (indentChange > 1) {
        throw InvalidIndentationException.indentationLevelSkipped(ctx);
      } else if (indentChange == 1) {
        if (this.levels.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero(ctx);
        }
        this.levels.push(this.levels.peek() + 1);
      } else if (indentChange == 0 && this.levels.isEmpty()) {
          assert indentLevel == 0 : UNEXPECTED_INDENTATION;
          this.levels.push(0);
        }
    }

    // #endregion Scope management --------------------------------------------

    // #region Dictionary context handling ------------------------------------

    @Override
    public void exitDictionaryIndexClause(DictionaryIndexClauseContext ctx) {
      // Push field context for dictionary index - same as main translator
      this.efxContext.pushFieldContext(getFieldId(ctx.fieldContext()));
    }

    @Override
    public void exitDictionaryKeyClause(DictionaryKeyClauseContext ctx) {
      // Pop the dictionary index context - same as main translator
      this.efxContext.pop();
    }

    // #endregion Dictionary context handling ---------------------------------

    @Override
    public void exitDictionaryDeclaration(DictionaryDeclarationContext ctx) {
      var dictionaryName = ctx.dictionaryName.getText();
      var fieldId = getFieldId(ctx.index.fieldContext());
      var field = this.symbols.getAbsolutePathOfField(fieldId);
      this.stack.declareGlobalIdentifier(new Dictionary(dictionaryName, field, StringExpression.empty()));
    }
  }

  // #endregion Pre-processing ------------------------------------------------
}
