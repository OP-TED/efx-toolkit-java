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

import static eu.europa.ted.efx.model.constructs.Parameter.Impl.parameter;
import static eu.europa.ted.efx.model.constructs.ParameterType.INDICATOR;
import static eu.europa.ted.efx.model.constructs.ParameterType.NUMBER;
import static eu.europa.ted.efx.model.constructs.ParameterType.TEXT;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.ParseInfo;
import org.antlr.v4.runtime.atn.ProfilingATNSimulator;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.InvalidIndentationException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.TemplateSection;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.constructs.Arguments;
import eu.europa.ted.efx.model.constructs.ParameterType;
import eu.europa.ted.efx.model.constructs.Spelling;
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
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericPath;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.templates.ContentTemplate;
import eu.europa.ted.efx.model.templates.ContentTemplateFragment;
import eu.europa.ted.efx.model.templates.DisplayContentTemplate;
import eu.europa.ted.efx.model.templates.FormatContentTemplateFragment;
import eu.europa.ted.efx.model.templates.FormatOptions;
import eu.europa.ted.efx.model.templates.FormatOptions.DateTimeFormatOptions;
import eu.europa.ted.efx.model.templates.FormatOptions.DefaultFormatOptions;
import eu.europa.ted.efx.model.templates.FormatOptions.FormatStyle;
import eu.europa.ted.efx.model.templates.FormatOptions.NoFormattingOptions;
import eu.europa.ted.efx.model.templates.FormatOptions.NumberFormatOptions;
import eu.europa.ted.efx.model.templates.HyperlinkContentTemplateFragment;
import eu.europa.ted.efx.model.templates.InvokeContentTemplate;
import eu.europa.ted.efx.model.templates.LabelContentTemplateFragment;
import eu.europa.ted.efx.model.templates.LabelFromExpressionContentTemplateFragment;
import eu.europa.ted.efx.model.templates.LabelFromKeyContentTemplateFragment;
import eu.europa.ted.efx.model.templates.LineBreakContentTemplateFragment;
import eu.europa.ted.efx.model.templates.TextContentTemplateFragment;
import eu.europa.ted.efx.model.templates.ContentBlock;
import eu.europa.ted.efx.model.templates.ContentBlockStack;
import eu.europa.ted.efx.model.templates.ViewTemplate;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.model.identifiers.Dictionary;
import eu.europa.ted.efx.model.identifiers.Function;
import eu.europa.ted.efx.model.identifiers.ParsedParameters;
import eu.europa.ted.efx.model.identifiers.Template;
import eu.europa.ted.efx.model.identifiers.Variable;
import eu.europa.ted.efx.model.identifiers.Variables;
import eu.europa.ted.efx.sdk2.EfxParser.*;

/**
 * The EfxTemplateTranslator translates EFX templates. It listens to the rules of a template file
 * that are not expressions, and asks an {@link EfxExpressionVisitor} for each expression, field
 * context and node context they hold. Its visitor may call the built-ins that only templates may
 * call, such as the formatting functions.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_TEMPLATE_TRANSLATOR)
public class EfxTemplateTranslatorV2 extends TranslatorListener
    implements EfxTemplateTranslator {

  private static final Logger logger = LoggerFactory.getLogger(EfxTemplateTranslatorV2.class);

  private static final String UNEXPECTED_INDENTATION = "Unexpected indentation tracker state.";

  /**
   * The variable that the translator's own loops bind each item to: the numbers, durations,
   * dates and times it formats and the asset ids of a label. It starts with the prefix that no
   * declaration may give, so it cannot hide a variable of the template that a label's asset type
   * or label type reads inside the loop.
   */
  private static final String ITEM_VARIABLE = Translation.RESERVED_PREFIX + "item";

  /**
   * The variable that the translator's own loops bind each unit code to, for the space before a
   * unit and the label of the unit. It starts with the prefix that no declaration may give, as every
   * variable that the translator generates does.
   */
  private static final String CODE_VARIABLE = Translation.RESERVED_PREFIX + "code";

  /** The condition of a conditional template block, as a type error names it. */
  private static final Clause WHEN = Clause.of(Clause.describe("when"),
      parameter("condition", INDICATOR));

  /** The key of a dictionary declaration, as a type error names it. */
  private static final Clause BY = Clause.of(Clause.describe("by"), parameter("key", TEXT));

  /** The URL of a hyperlink, as a type error names it. */
  private static final Clause HYPERLINK = Clause.of("Hyperlink", parameter("URL", TEXT));

  /** A label block, as a type error names it with the one of its parts that is at fault. */
  private static final String LABEL_BLOCK = "Label block";

  /** The parts of a label block, each as a type error names it. */
  private static final Clause ASSET_TYPE = Clause.of(LABEL_BLOCK, parameter("asset type", TEXT));
  private static final Clause LABEL_TYPE = Clause.of(LABEL_BLOCK, parameter("label type", TEXT));
  private static final Clause ASSET_ID = Clause.of(LABEL_BLOCK, parameter("asset id", TEXT));
  private static final Clause PLURALISER = Clause.of(LABEL_BLOCK, parameter("pluraliser", NUMBER));
  private static final Clause LABEL_KEY = Clause.of(LABEL_BLOCK, parameter("key", TEXT));

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

  // Set by the first indented line of each template.
  private Indent indentWith;
  private int indentSpaces;

  /**
   * The MarkupGenerator is called to retrieve markup in the target template language when needed.
   */
  MarkupGenerator markup;

  TranslatorContext translatorContext = new TranslatorContext();

  /**
   * The view template built while the template file is parsed.
   */
  ViewTemplate viewTemplate;

  /**
   * The block stack is used to keep track of the indentation of template lines and adjust the EFX
   * context accordingly. A block is a template line together with the template lines nested
   * (through indentation) under it. At the top of the blockStack is the template block that is
   * currently being processed. The next block in the stack is its parent block, and so on.
   */
  ContentBlockStack blockStack = new ContentBlockStack();

  /**
   * The block of the line above the template line being translated, which the line is nested in:
   * the root of its section or of the declared templates for a line at the top, or none for the
   * first line.
   */
  private ContentBlock blockAbove;

  @SuppressWarnings("unused")
  private EfxTemplateTranslatorV2() {
    this(null, null, null, null);
  }

  public EfxTemplateTranslatorV2(final MarkupGenerator markupGenerator,
      final SymbolResolver symbolResolver, final ScriptGenerator scriptGenerator,
      final BaseErrorListener errorListener) {
    super(symbolResolver, scriptGenerator, errorListener);
    this.markup = markupGenerator;
  }

  @Override
  EfxExpressionVisitor visitorOf(final Translation translation) {
    return new TemplateExpressionVisitor(translation);
  }

  /**
   * Translates a single expression, in which the built-ins that only templates may call may be
   * called.
   */
  @Override
  public String translateExpression(final String expression, final String... arguments) {
    return new EfxExpressionTranslatorV2(this.translation.symbols(), this.translation.script(),
        this.errorListener, TemplateExpressionVisitor::new)
        .translateExpression(expression, arguments);
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

    // Resolve #include directives before parsing
    final CharStream resolvedInput =
        new IncludeProcessor(options.getIncludedFileResolver()).resolve(charStream);

    final EfxParser parser = this.parserOf(resolvedInput);

    // Enable profiling if requested
    if (options != null && options.isProfilerEnabled()) {
      parser.setInterpreter(new ProfilingATNSimulator(parser));
    }

    this.translate(parser.templateFile());
    assert this.translation.stack().empty() : "Stack should be empty at this point.";

    final String output = this.viewTemplate.render(this.markup, this.translatorContext).script.trim();

    final long endTime = System.currentTimeMillis();
    final long totalDuration = endTime - startTime;

    // Log profiling information if enabled
    if (options != null && options.isProfilerEnabled()) {
      TranslatorTimings timingData = new TranslatorTimings(0, totalDuration, totalDuration);
      this.generateAndSaveProfilerReport(parser, options.getProfilerOutputPath(), timingData);
    }

    logger.debug("Finished rendering template");

    return output;
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
    var variable = this.translation.stack().pop(Variable.class);
    this.translation.stack().declareGlobalIdentifier(variable);
    this.viewTemplate.addVariable(variable);
  }

  /**
   * Opens the scope of a function: its parameters are declared in a stack frame of its own.
   */
  @Override
  public void enterFunctionDeclaration(FunctionDeclarationContext ctx) {
    this.translation.stack().pushStackFrame();
    this.translation.stack().push(new ParsedParameters());
  }

  /**
   * Declares a function. Its body must give a value of the declared type, which is the type of a
   * call of it. The function is declared before its scope closes, so it cannot take the name of
   * one of its parameters.
   */
  @Override
  public void exitFunctionDeclaration(FunctionDeclarationContext ctx) {
    var type = EfxExpressionVisitor.typeOf(ctx.typeDeclaration());
    var expression = this.expressions.valueOf(ctx.expression(),
        Clause.of(Spelling.function(ctx.typeDeclaration().getText() + ":?"
            + ctx.functionName.getText()).describe(), parameter("value", ParameterType.of(type))))
        .at(0, TypedExpression.class);
    var parameters = this.translation.stack().pop(ParsedParameters.class);
    var function = new Function(ctx.functionName.getText(), parameters, type, expression);
    this.translation.stack().declareFunction(function);
    this.translation.stack().popStackFrame();
    this.viewTemplate.addFunction(function);
  }

  @Override
  public void exitTemplateDefinition(TemplateDefinitionContext ctx) {
    var parameters = this.translation.stack().pop(ParsedParameters.class);
    var template = new Template(ctx.templateName.getText(), parameters);
    this.translation.stack().declareTemplate(template);
    this.translation.stack().push(template);
  }

  @Override
  public void enterTemplateDefinition(TemplateDefinitionContext ctx) {
    this.translation.stack().push(new ParsedParameters());
  }

  // #endregion Global declarationExpressions ------------------------------------

  // #region Template File ----------------------------------------------------

  @Override
  public void enterTemplateFile(TemplateFileContext ctx) {
    assert this.blockStack.isEmpty() : UNEXPECTED_INDENTATION;
    this.viewTemplate = new ViewTemplate();
    this.translatorContext = new TranslatorContext();
    this.indentWith = Indent.UNDETERMINED;
    this.indentSpaces = -1;
  }

  @Override
  public void exitTemplateFile(TemplateFileContext ctx) {
    // The end of the file closes the levels still open, as a line back at level 0 would: the block of
    // each nested level and its stack frame.
    while (this.blockStack.currentIndentationLevel() > 0) {
      this.blockStack.pop();
      this.translation.stack().popStackFrame();
    }
    // If there are template lines, the top-level block of the last one is left; so we remove it here.
    if (!this.blockStack.isEmpty()) {
      this.blockStack.pop();
    }
  }

  // #endregion Template File -------------------------------------------------
  
  // #region Source template blocks -------------------------------------------

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    DisplayContentTemplate template = ctx.templateFragment() != null ? this.translation.stack().pop(DisplayContentTemplate.class) : new DisplayContentTemplate();
    TextContentTemplateFragment text = new TextContentTemplateFragment(ctx.textBlock() != null ? ctx.textBlock().getText() : "");
    if (ctx.linkBlock() == null) {
      template.prependText(text);
    } else {
      template.prepend(this.linked(text, ctx.linkBlock()));
    }
    this.translation.stack().push(template);
  }

  @Override
  public void exitLabelTemplate(LabelTemplateContext ctx) {
    DisplayContentTemplate template = ctx.templateFragment() != null ? this.translation.stack().pop(DisplayContentTemplate.class) : new DisplayContentTemplate();
    if (ctx.labelBlock() != null) {
      template.prepend(this.linked(this.translation.stack().pop(ContentTemplateFragment.class), ctx.linkBlock()));
    }
    this.translation.stack().push(template);
  }

  // #region New in EFX-2: Formatted expression blocks -------------------------

  @Override
  public void exitExpressionTemplate(ExpressionTemplateContext ctx) {
    DisplayContentTemplate template = ctx.templateFragment() != null ? this.translation.stack().pop(DisplayContentTemplate.class) : new DisplayContentTemplate();
    template.prepend(this.linked(this.translation.stack().pop(FormatContentTemplateFragment.class), ctx.linkBlock()));
    this.translation.stack().push(template);
  }

  /**
   * Formats the value of an expression: ${BT-00-Number * 2}, or with options, ${BT-00-Number * 2|2}. A
   * calculated duration is the only value of an expression that has a unit, and it is formatted as a
   * number.
   */
  @Override
  public void exitComputedExpressionBlock(ComputedExpressionBlockContext ctx) {
    FormatOptions options = ctx.formatOptions() != null ? this.translation.stack().pop(FormatOptions.class) : new DefaultFormatOptions(false);
    TypedExpression value =
        this.expressions.valueOf(ctx.expression(), EXPRESSION_BLOCK).at(0, TypedExpression.class);
    Optional<NumberFormatOptions> numberOptions = options.forNumbers();
    if (value.is(EfxDataType.DurationScalar.class) && numberOptions.isPresent()) {
      this.translation.stack().push(this.createFormattedExpressionBlockFragment(TypedExpression.from(value, DurationExpression.class),
          numberOptions.get()));
    } else if (value.is(EfxDataType.DurationSequence.class) && numberOptions.isPresent()) {
      this.translation.stack().push(this.createFormattedExpressionBlockFragment(TypedExpression.from(value, DurationSequenceExpression.class),
          numberOptions.get()));
    } else {
      this.translation.stack().push(this.createFormattedExpressionBlockFragment(value, options));
    }
  }

  /**
   * Formats the value of a field: ${BT-00-Number}, or with options, ${BT-00-Number|2}.
   */
  @Override
  public void exitFieldExpressionBlock(FieldExpressionBlockContext ctx) {
    FormatOptions options = ctx.formatOptions() != null ? this.translation.stack().pop(FormatOptions.class) : new DefaultFormatOptions(false);
    this.lookingUpField(ctx.FieldId(), () -> this.translation.stack().push(
        this.createFormattedExpressionBlockFragment(ctx.FieldId().getText(), options)));
  }

  /**
   * Formats the value of the context field: $value. It has no options.
   */
  @Override
  public void exitContextFieldExpressionBlock(ContextFieldExpressionBlockContext ctx) {
    if (!this.translation.efxContext().isFieldContext()) {
      throw InvalidUsageException.shorthandRequiresFieldContext(ctx, "$value");
    }
    this.translation.stack().push(this.createFormattedExpressionBlockFragment(this.translation.efxContext().symbol(), new DefaultFormatOptions(false)));
  }

  /**
   * Reads the format options once, for the expression block that uses them.
   */
  @Override
  public void exitFormatOptions(FormatOptionsContext ctx) {
    boolean noUnit = ctx.FormatNoUnit() != null;
    FormatStyleContext style = ctx.formatStyle();
    if (ctx.FormatNoFormatting() != null) {
      this.translation.stack().push(new NoFormattingOptions());
    } else if (ctx.FormatDecimals() != null) {
      this.translation.stack().push(new NumberFormatOptions(Integer.parseInt(ctx.FormatDecimals().getText()), noUnit));
    } else if (style == null) {
      this.translation.stack().push(new DefaultFormatOptions(noUnit));
    } else if (style.FormatShort() != null) {
      this.translation.stack().push(new DateTimeFormatOptions(FormatStyle.SHORT));
    } else if (style.FormatMedium() != null) {
      this.translation.stack().push(new DateTimeFormatOptions(FormatStyle.MEDIUM));
    } else if (style.FormatLong() != null) {
      this.translation.stack().push(new DateTimeFormatOptions(FormatStyle.LONG));
    }
  }

  /**
   * Creates the fragment of an expression block that displays the value of a field. Only the type of the field
   * tells whether its value has a unit: amounts, measures and durations do, and they are formatted as a number.
   * The value of any other field, and a field with a unit given a style, is formatted like the value of an
   * expression. No-formatting displays the value of the field as entered: the text of a number, a date, a time
   * or a duration, and the value of a text, which is its text.
   */
  private FormatContentTemplateFragment createFormattedExpressionBlockFragment(String fieldId, FormatOptions options) {
    PathExpression field = this.translation.symbols().getRelativePathOfField(fieldId,
        this.translation.efxContext().symbol());
    if (!options.formatsValue()) {
      return new FormatContentTemplateFragment(field.is(EfxDataType.String.class)
          ? this.expressions.fieldValueOf(fieldId, field)
          : this.translation.script().composeFieldRawValueReference(field));
    }
    String fieldType = this.translation.symbols().getTypeOfField(fieldId);
    Optional<NumberFormatOptions> numberOptions = options.forNumbers();
    switch (FieldTypes.fromString(fieldType)) {
      case DURATION:
      case MEASURE:
      case AMOUNT:
        if (numberOptions.isPresent()) {
          return this.createFormattedExpressionBlockFragment(field, fieldId, numberOptions.get());
        }
        break;
      default:
        break;
    }
    return this.createFormattedExpressionBlockFragment(this.expressions.fieldValueOf(fieldId, field), options);
  }

  /**
   * Creates the fragment of an expression block for a field with a unit: the number entered, followed by its
   * unit unless no-unit is given. The number is also the quantity that selects the plural form of the unit.
   * A unit can only be displayed with a single value, so the numbers of a repeatable field are displayed
   * without it, and only if no-unit is given. The attribute field that holds the unit, and its code list, are
   * only looked up when the unit is displayed.
   *
   * @param field The path of the field.
   * @param fieldId The field.
   */
  private FormatContentTemplateFragment createFormattedExpressionBlockFragment(PathExpression field, String fieldId,
      NumberFormatOptions options) {
    if (field instanceof SequenceExpression) {
      if (!options.hideUnit()) {
        throw InvalidUsageException.unitForList(this.translation.stack().peekParserContext());
      }
      PathExpression numbers = this.translation.script().composeFieldValueReference(new NumericSequencePath(field.getScript()));
      return new FormatContentTemplateFragment(this.composeFormattedExpression(numbers, options));
    }
    NumericExpression number = TypedExpression.from(
        this.translation.script().composeFieldValueReference(new NumericPath(field.getScript())), NumericExpression.class);
    if (options.hideUnit()) {
      return new FormatContentTemplateFragment(this.composeFormattedExpression(number, options));
    }
    String unitFieldId = this.translation.symbols().getAttributeFieldIdOfField(fieldId);
    SequenceExpression unitCode =
        this.composeGetUnitCode(field, this.translation.symbols().getAttributeNameFromAttributeField(unitFieldId));
    return new FormatContentTemplateFragment(this.composeFormattedExpression(number, options),
        this.composeUnitSeparator(unitCode),
        this.composeCodeLabelKey(this.translation.symbols().getRootCodelistOfField(unitFieldId),
            unitCode),
        number);
  }

  /**
   * Creates the fragment of an expression block for a calculated duration. It has no unit entered with it, so
   * it is displayed as a number of months or days, in the unit it is calculated in. That number also
   * selects the plural form of the unit. It has no field either, so the code list of its unit is the one that
   * the duration data type defines for its supplementary component, looked up only when the unit is displayed.
   */
  private FormatContentTemplateFragment createFormattedExpressionBlockFragment(DurationExpression duration, NumberFormatOptions options) {
    NumericExpression number = this.composeGetNumberFromDuration(duration);
    if (options.hideUnit()) {
      return new FormatContentTemplateFragment(this.composeFormattedScalar(number, options));
    }
    String unitCodelistId = this.translation.symbols().getAttributeFieldCodelistIdFromDataType(FieldTypes.DURATION.getName());
    SequenceExpression unitCode = this.composeGetUnitCode(duration);
    return new FormatContentTemplateFragment(this.composeFormattedScalar(number, options),
        this.composeUnitSeparator(unitCode), this.composeCodeLabelKey(unitCodelistId, unitCode),
        number);
  }

  /**
   * Creates the fragment of an expression block for a sequence of calculated durations. A unit can only be
   * displayed with a single value, so each duration is displayed as its number of months or days, and
   * only if no-unit is given.
   */
  private FormatContentTemplateFragment createFormattedExpressionBlockFragment(DurationSequenceExpression durations,
      NumberFormatOptions options) {
    if (!options.hideUnit()) {
      throw InvalidUsageException.unitForList(this.translation.stack().peekParserContext());
    }
    return new FormatContentTemplateFragment(this.composeFormattedSequence(durations, options));
  }

  /**
   * Creates the fragment of an expression block for a value that has no unit, so no-unit is not applicable.
   * No-formatting displays the value as it is.
   */
  private FormatContentTemplateFragment createFormattedExpressionBlockFragment(TypedExpression value, FormatOptions options) {
    if (!options.formatsValue()) {
      return new FormatContentTemplateFragment(value);
    }
    if (options.hideUnit()) {
      throw InvalidUsageException.noUnitNotApplicable(this.translation.stack().peekParserContext(),
          value.getDataType());
    }
    return new FormatContentTemplateFragment(this.composeFormattedExpression(value, options));
  }

  /**
   * Formats a value, or each value of a sequence, according to its type. Format options are a shorthand
   * for the formatting functions: ${BT-00-Number|2} displays the same as
   * ${format-number(BT-00-Number, '#,##0.00')}, and ${BT-00-Date|medium} the same as
   * ${format-medium(BT-00-Date)}. A number is formatted with options for numbers, and a date or a time
   * with options for dates and times. Any other value is displayed as it is, unless the options give a
   * number of decimals or a style, which it cannot be formatted with: the error names the option and the
   * type of the value. Options that give a number of decimals are for numbers only, and options that give
   * a style for dates and times only.
   */
  private Expression composeFormattedExpression(TypedExpression value, FormatOptions options) {
    Optional<NumberFormatOptions> numberOptions = options.forNumbers();
    Optional<DateTimeFormatOptions> dateTimeOptions = options.forDatesAndTimes();
    if (value.is(EfxDataType.NumberScalar.class) && numberOptions.isPresent()) {
      return this.composeFormattedScalar(TypedExpression.from(value, NumericExpression.class), numberOptions.get());
    } else if (value.is(EfxDataType.NumberSequence.class) && numberOptions.isPresent()) {
      return this.composeFormattedSequence(TypedExpression.from(value, NumericSequenceExpression.class),
          numberOptions.get());
    } else if (value.is(EfxDataType.DateScalar.class) && dateTimeOptions.isPresent()) {
      return this.composeFormattedScalar(TypedExpression.from(value, DateExpression.class), dateTimeOptions.get());
    } else if (value.is(EfxDataType.DateSequence.class) && dateTimeOptions.isPresent()) {
      return this.composeFormattedSequence(TypedExpression.from(value, DateSequenceExpression.class),
          dateTimeOptions.get());
    } else if (value.is(EfxDataType.TimeScalar.class) && dateTimeOptions.isPresent()) {
      return this.composeFormattedScalar(TypedExpression.from(value, TimeExpression.class), dateTimeOptions.get());
    } else if (value.is(EfxDataType.TimeSequence.class) && dateTimeOptions.isPresent()) {
      return this.composeFormattedSequence(TypedExpression.from(value, TimeSequenceExpression.class),
          dateTimeOptions.get());
    }
    if (!options.hasSpecifier()) {
      return value;
    }
    if (numberOptions.isPresent()) {
      throw InvalidUsageException.decimalsNotApplicable(this.translation.stack().peekParserContext(),
          numberOptions.get().getDecimals().getAsInt(), value.getDataType());
    }
    throw InvalidUsageException.styleNotApplicable(this.translation.stack().peekParserContext(),
        dateTimeOptions.get().getStyle(), value.getDataType());
  }

  /**
   * Formats a number like a sequence of at most one number: format-number would display an absent number as
   * NaN, whereas an absent number is not displayed at all.
   */
  private StringSequenceExpression composeFormattedScalar(NumericExpression number, NumberFormatOptions options) {
    return this.composeFormattedSequence(new NumericSequenceExpression(number.getScript()), options);
  }

  private StringSequenceExpression composeFormattedSequence(NumericSequenceExpression numbers, NumberFormatOptions options) {
    IteratorListExpression iterators = this.translation.script().composeIteratorList(List.of(this.translation.script().composeIteratorExpression(
        this.translation.script().composeVariableDeclaration(ITEM_VARIABLE,
            NumericExpression.class),
        numbers)));
    return this.translation.script().composeForExpression(iterators,
        this.composeFormattedNumber(
            this.translation.script().composeVariableReference(ITEM_VARIABLE,
                NumericExpression.class),
            options),
        StringSequenceExpression.class);
  }

  private StringSequenceExpression composeFormattedSequence(DurationSequenceExpression durations,
      NumberFormatOptions options) {
    IteratorListExpression iterators = this.translation.script().composeIteratorList(List.of(this.translation.script().composeIteratorExpression(
        this.translation.script().composeVariableDeclaration(ITEM_VARIABLE,
            DurationExpression.class),
        durations)));
    return this.translation.script().composeForExpression(iterators,
        this.composeFormattedNumber(
            this.composeGetNumberFromDuration(
                this.translation.script().composeVariableReference(ITEM_VARIABLE,
                    DurationExpression.class)),
            options),
        StringSequenceExpression.class);
  }

  /**
   * Formats a number that exists.
   */
  private StringExpression composeFormattedNumber(NumericExpression number, NumberFormatOptions options) {
    return this.translation.script().composeNumberFormatting(number,
        this.translation.script().getStringLiteralFromUnquotedString(options.getNumberPattern()));
  }

  private StringExpression composeFormattedScalar(DateExpression date, DateTimeFormatOptions options) {
    switch (options.getStyle()) {
      case MEDIUM:
        return this.translation.script().composeFormatDateMedium(date);
      case LONG:
        return this.translation.script().composeFormatDateLong(date);
      default:
        return this.translation.script().composeFormatDateShort(date);
    }
  }

  private StringSequenceExpression composeFormattedSequence(DateSequenceExpression dates, DateTimeFormatOptions options) {
    IteratorListExpression iterators = this.translation.script().composeIteratorList(List.of(this.translation.script().composeIteratorExpression(
        this.translation.script().composeVariableDeclaration(ITEM_VARIABLE, DateExpression.class),
        dates)));
    return this.translation.script().composeForExpression(iterators,
        this.composeFormattedScalar(
            this.translation.script().composeVariableReference(ITEM_VARIABLE, DateExpression.class),
            options),
        StringSequenceExpression.class);
  }

  private StringExpression composeFormattedScalar(TimeExpression time, DateTimeFormatOptions options) {
    switch (options.getStyle()) {
      case MEDIUM:
        return this.translation.script().composeFormatTimeMedium(time);
      case LONG:
        return this.translation.script().composeFormatTimeLong(time);
      default:
        return this.translation.script().composeFormatTimeShort(time);
    }
  }

  private StringSequenceExpression composeFormattedSequence(TimeSequenceExpression times, DateTimeFormatOptions options) {
    IteratorListExpression iterators = this.translation.script().composeIteratorList(List.of(this.translation.script().composeIteratorExpression(
        this.translation.script().composeVariableDeclaration(ITEM_VARIABLE, TimeExpression.class),
        times)));
    return this.translation.script().composeForExpression(iterators,
        this.composeFormattedScalar(
            this.translation.script().composeVariableReference(ITEM_VARIABLE, TimeExpression.class),
            options),
        StringSequenceExpression.class);
  }

  /**
   * Returns the number of a calculated duration: its months if it is calculated in years and months,
   * otherwise its whole days, as days() does.
   */
  private NumericExpression composeGetNumberFromDuration(DurationExpression duration) {
    return this.translation.script().composeConditionalExpression(
        this.composeIsYearMonthDuration(duration),
        this.translation.script().composeMonthsFromDurationFunction(duration),
        this.translation.script().composeDaysFromDurationFunction(duration),
        NumericExpression.class);
  }

  /**
   * Tests whether a duration is a year-month duration. EFX calculates a duration either in years and
   * months or in days, never in both, so a year-month duration is one that has months. The script
   * generator has no test for the kind of a duration, so a duration of zero counts as a day duration.
   */
  private BooleanExpression composeIsYearMonthDuration(DurationExpression duration) {
    return this.translation.script().composeComparisonOperation(this.translation.script().composeMonthsFromDurationFunction(duration), "!=",
        this.translation.script().getNumericLiteralEquivalent("0"));
  }

  /**
   * Returns the unit code entered with the value of a field, from the given attribute. An absent value has
   * none.
   */
  private SequenceExpression composeGetUnitCode(PathExpression field, String attribute) {
    return this.translation.script().composeFieldAttributeReference(field, attribute,
        StringSequencePath.class);
  }

  /**
   * Returns the unit code of a calculated duration, in the duration-unit code list: MONTH or DAY. An absent
   * duration has none.
   */
  private StringSequenceExpression composeGetUnitCode(DurationExpression duration) {
    IteratorListExpression iterators = this.translation.script().composeIteratorList(List.of(this.translation.script().composeIteratorExpression(
        this.translation.script().composeVariableDeclaration(ITEM_VARIABLE,
            DurationExpression.class),
        new DurationSequenceExpression(duration.getScript()))));
    return this.translation.script().composeForExpression(iterators,
        this.translation.script().composeConditionalExpression(
            this.composeIsYearMonthDuration(
                this.translation.script().composeVariableReference(ITEM_VARIABLE,
                    DurationExpression.class)),
            this.translation.script().getStringLiteralFromUnquotedString("MONTH"),
            this.translation.script().getStringLiteralFromUnquotedString("DAY"),
            StringExpression.class),
        StringSequenceExpression.class);
  }

  /**
   * Returns the space that separates a unit from its value, for each unit code: none when there is no code.
   */
  private StringSequenceExpression composeUnitSeparator(SequenceExpression code) {
    IteratorListExpression iterators = this.translation.script().composeIteratorList(List.of(this.translation.script().composeIteratorExpression(
        this.translation.script().composeVariableDeclaration(CODE_VARIABLE, StringExpression.class),
        code)));
    return this.translation.script().composeForExpression(iterators, this.translation.script().getStringLiteralFromUnquotedString(" "),
        StringSequenceExpression.class);
  }

  /**
   * Returns the label key of each code: code|name|codelist.code. There is none when there is no code.
   */
  private StringSequenceExpression composeCodeLabelKey(String codelist, SequenceExpression code) {
    IteratorListExpression iterators = this.translation.script().composeIteratorList(List.of(this.translation.script().composeIteratorExpression(
        this.translation.script().composeVariableDeclaration(CODE_VARIABLE, StringExpression.class),
        code)));
    return this.translation.script().composeForExpression(iterators, this.translation.script().composeStringConcatenation(List.of(
        this.translation.script().getStringLiteralFromUnquotedString(ASSET_TYPE_CODE),
        this.translation.script().getStringLiteralFromUnquotedString("|"),
        this.translation.script().getStringLiteralFromUnquotedString(LABEL_TYPE_NAME),
        this.translation.script().getStringLiteralFromUnquotedString("|"),
        this.translation.script().getStringLiteralFromUnquotedString(codelist + "."),
        this.translation.script().composeVariableReference(CODE_VARIABLE, StringExpression.class))),
        StringSequenceExpression.class);
  }

  // #endregion New in EFX-2: Formatted expression blocks ----------------------

  // #region New in EFX-2: Secondary templates --------------------------------

  @Override
  public void exitSecondaryTemplate(SecondaryTemplateContext ctx) {
    DisplayContentTemplate template = ctx.templateFragment() != null ? this.translation.stack().pop(DisplayContentTemplate.class) : new DisplayContentTemplate();
    template.prepend(new LineBreakContentTemplateFragment());
    this.translation.stack().push(template);
  }

  // #endregion New in EFX-2: Secondary templates -----------------------------

  // #endregion Source template blocks ----------------------------------------
  
  // #region Label Blocks #{...} ----------------------------------------------

  /**
   * Pluralises the label that the label id gave, when the label block has a pluraliser. The
   * pluraliser follows the label id, so it is translated after the label is.
   */
  @Override
  public void exitLabelReference(LabelReferenceContext ctx) {
    if (ctx.pluraliser() != null) {
      final NumericExpression quantity = this.labelPart(ctx, PLURALISER).numericAt(0);
      final LabelContentTemplateFragment label =
          this.translation.stack().pop(LabelContentTemplateFragment.class);
      this.translation.stack().push(label.pluralisedBy(quantity));
    }
  }

  /**
   * Leaves the asset type of #{field|name|BT-00-Text} on the stack before the parts that follow it,
   * as an expression block leaves a computed asset type, so that both are taken off the stack in the
   * same way.
   */
  @Override
  public void enterStandardLabelId(StandardLabelIdContext ctx) {
    this.translation.stack().push(this.translation.script().getStringLiteralFromUnquotedString(
        ctx.AssetType().getText()));
  }

  @Override
  public void exitStandardLabelId(StandardLabelIdContext ctx) {
    this.standardLabelReference(ctx.getParent());
  }

  /**
   * Leaves the label type of #{name|BT-00} and #{name|BT-00-Text} on the stack, as an expression
   * block leaves a computed label type.
   */
  @Override
  public void enterShorthandLabelId(ShorthandLabelIdContext ctx) {
    this.translation.stack().push(this.translation.script().getStringLiteralFromUnquotedString(
        ctx.LabelType().getText()));
  }

  @Override
  public void exitShorthandLabelId(ShorthandLabelIdContext ctx) {
    if (ctx.BtId() != null) {
      this.shorthandBtLabelReference(ctx.getParent(), ctx.BtId());
    } else {
      this.shorthandFieldLabelReference(ctx.getParent(), ctx.FieldId());
    }
  }

  @Override
  public void exitShorthandIndirectLabelId(ShorthandIndirectLabelIdContext ctx) {
    this.lookingUpField(ctx.FieldId(),
        () -> this.shorthandIndirectLabelReference(ctx.getParent(), ctx.FieldId().getText()));
  }

  /**
   * Translates a label id that starts with an expression block. Depending on what follows it, the
   * expression block is the asset type, as in #{${$assetType}|name|BT-00-Text}, the label type, as
   * in #{${$labelType}|BT-00} and #{${$labelType}|BT-00-Text}, or the whole label id, as in
   * #{${concat('code|name|currency.', BT-00-Code)}}.
   */
  @Override
  public void exitLabelIdStartingWithExpressionBlock(LabelIdStartingWithExpressionBlockContext ctx) {
    final ParserRuleContext labelBlock = ctx.getParent();
    if (ctx.assetId() != null) {
      this.standardLabelReference(labelBlock);
    } else if (ctx.BtId() != null) {
      this.shorthandBtLabelReference(labelBlock, ctx.BtId());
    } else if (ctx.FieldId() != null) {
      this.shorthandFieldLabelReference(labelBlock, ctx.FieldId());
    } else {
      this.computedLabelReference(labelBlock);
    }
  }

  /**
   * Renders the label, or labels, of #{assetType|labelType|assetId}, whose three parts are on the
   * stack, the asset id on top.
   *
   * @param labelBlock the label block, where an error is reported
   */
  private void standardLabelReference(final ParserRuleContext labelBlock) {
    if (StringSequenceExpression.class.isAssignableFrom(this.translation.stack().peek().getClass())) {

      // TODO: Review this in EFX-2

      // This is a workaround that allows EFX 1 to render a sequence of labels without a special
      // syntax. When a standard label reference is processed, the template translator checks if
      // the assetId is provided with a SequenceExpression. If this is the case, then the
      // translator generates the appropriate code to render a sequence of labels.
      //
      // For example, this will render a sequence of labels for a label reference of the form
      // #{assetType|labelType|${for text:$t in ('assetId1','assetId2') return $t}}:} 
      // The only restriction is that the assetType and labelType must be the same for all labels in the sequence.

      StringSequenceExpression assetIdSequence = this.translation.stack().pop(
          StringSequenceExpression.class);
      this.standardLabelReference(labelBlock, assetIdSequence);
    } else {

      // Standard implementation as originally intended by EFX 1

      StringExpression assetId = this.labelPart(labelBlock, ASSET_ID).stringAt(0);
      this.standardLabelReference(labelBlock, assetId);
    }
  }

  /**
   * Renders a single label from a standard label reference.
   *
   * @param labelBlock the label block, where an error is reported
   * @param assetId    the assetId of the label to render
   */
  private void standardLabelReference(final ParserRuleContext labelBlock, StringExpression assetId) {
    StringExpression labelType = this.labelPart(labelBlock, LABEL_TYPE).stringAt(0);
    StringExpression assetType = this.labelPart(labelBlock, ASSET_TYPE).stringAt(0);

    StringExpression key = this.translation.script().composeStringConcatenation(
        List.of(assetType, this.translation.script().getStringLiteralFromUnquotedString("|"),
            labelType, this.translation.script().getStringLiteralFromUnquotedString("|"),
            assetId));
    this.translation.stack().push(new LabelFromKeyContentTemplateFragment(key,
        NumericExpression.empty()));
  }

  /**
   * Returns the variable that a label's loop binds each of its asset ids to.
   */
  private Variable composeAssetIdVariable() {
    return new Variable(ITEM_VARIABLE,
        this.translation.script().composeVariableDeclaration(ITEM_VARIABLE, StringExpression.class),
        StringExpression.empty(),
        this.translation.script().composeVariableReference(ITEM_VARIABLE, StringExpression.class));
  }

  /**
   * Renders a sequence of labels from a standard label reference.
   *
   * @param labelBlock      the label block, where an error is reported
   * @param assetIdSequence the sequence of assetIds for the labels to render
   */
  private void standardLabelReference(final ParserRuleContext labelBlock,
      StringSequenceExpression assetIdSequence) {
    StringExpression labelType = this.labelPart(labelBlock, LABEL_TYPE).stringAt(0);
    StringExpression assetType = this.labelPart(labelBlock, ASSET_TYPE).stringAt(0);

    Variable loopVariable = this.composeAssetIdVariable();

    StringSequenceExpression keys = this.translation.script().composeDistinctValuesFunction(
        this.translation.script().composeForExpression(
            this.translation.script().composeIteratorList(
                List.of(
                    this.translation.script().composeIteratorExpression(loopVariable.declarationExpression, assetIdSequence))),
            this.translation.script().composeStringConcatenation(List.of(
                assetType,
                this.translation.script().getStringLiteralFromUnquotedString("|"),
                labelType,
                this.translation.script().getStringLiteralFromUnquotedString("|"),
                new StringExpression(loopVariable.referenceExpression.getScript()))),
            StringSequenceExpression.class),
        StringSequenceExpression.class);
    this.translation.stack().push(new LabelFromExpressionContentTemplateFragment(keys,
        NumericExpression.empty()));
  }

  /**
   * Renders the label of #{labelType|BT-00}, whose label type is on the stack.
   *
   * @param labelBlock the label block, where an error is reported
   * @param btId       the business term that the label is of
   */
  private void shorthandBtLabelReference(final ParserRuleContext labelBlock, final TerminalNode btId) {
    StringExpression assetId = this.translation.script().getStringLiteralFromUnquotedString(
        btId.getText());
    StringExpression labelType = this.labelPart(labelBlock, LABEL_TYPE).stringAt(0);
    StringExpression key = this.translation.script().composeStringConcatenation(
        List.of(this.translation.script().getStringLiteralFromUnquotedString(ASSET_TYPE_BT),
            this.translation.script().getStringLiteralFromUnquotedString("|"), labelType,
            this.translation.script().getStringLiteralFromUnquotedString("|"), assetId));
    this.translation.stack().push(new LabelFromKeyContentTemplateFragment(key,
        NumericExpression.empty()));
  }

  /**
   * Renders the label of #{labelType|BT-00-Text}, whose label type is on the stack.
   *
   * @param labelBlock the label block, where an error is reported
   * @param field      the field that the label is of
   */
  private void shorthandFieldLabelReference(final ParserRuleContext labelBlock,
      final TerminalNode field) {
    final String fieldId = field.getText();
    StringExpression labelType = this.labelPart(labelBlock, LABEL_TYPE).stringAt(0);

    if (labelType.getScript().equals("value")) {
      this.shorthandIndirectLabelReference(labelBlock, fieldId);
    } else {
      StringExpression key = this.translation.script().composeStringConcatenation(
          List.of(this.translation.script().getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
              this.translation.script().getStringLiteralFromUnquotedString("|"), labelType,
              this.translation.script().getStringLiteralFromUnquotedString("|"),
              this.translation.script().getStringLiteralFromUnquotedString(fieldId)));
      this.translation.stack().push(new LabelFromKeyContentTemplateFragment(key,
          NumericExpression.empty()));
    }
  }

  /**
   * Renders the label whose whole key is computed, as in
   * #{${concat('code|name|currency.', BT-00-Code)}}, and on the stack.
   *
   * @param labelBlock the label block, where an error is reported
   */
  private void computedLabelReference(final ParserRuleContext labelBlock) {
    StringExpression expression = this.labelPart(labelBlock, LABEL_KEY).stringAt(0);
    this.translation.stack().push(new LabelFromExpressionContentTemplateFragment(expression,
        NumericExpression.empty()));
  }

  /**
   * Runs a translation that looks up a field written as a single token, as BT-00-Number is in
   * ${BT-00-Number} and BT-00-Code in #{BT-00-Code}, and places an error in that lookup at the
   * token. The grammar reads such a field as a token, not as a rule, so the error would otherwise
   * be placed at the start of the block.
   */
  private void lookingUpField(final TerminalNode fieldId, final Runnable translation) {
    try {
      translation.run();
    } catch (final SymbolResolutionException e) {
      throw e.at(fieldId.getSymbol());
    }
  }

  /**
   * Renders the label of the value of a code or indicator field. The label of the unit of an amount,
   * a measure or a duration is pluralised by the number that it is the unit of, unless the label
   * block has a pluraliser (see {@link #exitLabelReference}).
   *
   * @param labelBlock the label block, where an error is reported
   * @param fieldId    the field whose value the label is of
   */
  private void shorthandIndirectLabelReference(final ParserRuleContext labelBlock, final String fieldId) {
    final Context currentContext = this.translation.efxContext().peek();
    final NumericExpression quantity = this.composeQuantityOfUnit(fieldId);
    final String fieldType = this.translation.symbols().getTypeOfField(fieldId);
    final PathExpression valueReference = this.expressions.fieldValueOf(fieldId,
        this.translation.symbols().getRelativePathOfField(fieldId, currentContext.symbol()));
    Variable loopVariable = this.composeAssetIdVariable();
    switch (fieldType) {
      case "indicator": {
        StringSequenceExpression keys = this.translation.script().composeDistinctValuesFunction(
            this.translation.script().composeForExpression(
                this.translation.script().composeIteratorList(
                    List.of(
                        this.translation.script().composeIteratorExpression(loopVariable.declarationExpression, valueReference.asSequence()))),
                this.translation.script().composeStringConcatenation(
                    List.of(
                        this.translation.script().getStringLiteralFromUnquotedString(
                            ASSET_TYPE_INDICATOR),
                        this.translation.script().getStringLiteralFromUnquotedString("|"),
                        this.translation.script().getStringLiteralFromUnquotedString(
                            LABEL_TYPE_WHEN),
                        this.translation.script().getStringLiteralFromUnquotedString("-"),
                        new StringExpression(loopVariable.referenceExpression.getScript()),
                        this.translation.script().getStringLiteralFromUnquotedString("|"),
                        this.translation.script().getStringLiteralFromUnquotedString(fieldId))),
                StringSequenceExpression.class),
            StringSequenceExpression.class);
        this.translation.stack().push(new LabelFromExpressionContentTemplateFragment(keys,
            quantity));
        break;
      }
      case "code":
      case "internal-code": {
        StringSequenceExpression keys = this.translation.script().composeDistinctValuesFunction(
            this.translation.script().composeForExpression(
                this.translation.script().composeIteratorList(
                    List.of(
                        this.translation.script().composeIteratorExpression(loopVariable.declarationExpression, valueReference.asSequence()))),
                this.translation.script().composeStringConcatenation(List.of(
                    this.translation.script().getStringLiteralFromUnquotedString(ASSET_TYPE_CODE),
                    this.translation.script().getStringLiteralFromUnquotedString("|"),
                    this.translation.script().getStringLiteralFromUnquotedString(LABEL_TYPE_NAME),
                    this.translation.script().getStringLiteralFromUnquotedString("|"),
                    this.translation.script().getStringLiteralFromUnquotedString(
                        this.translation.symbols().getRootCodelistOfField(fieldId)),
                    this.translation.script().getStringLiteralFromUnquotedString("."),
                    new StringExpression(loopVariable.referenceExpression.getScript()))),
                StringSequenceExpression.class),
            StringSequenceExpression.class);
        this.translation.stack().push(new LabelFromExpressionContentTemplateFragment(keys,
            quantity));
        break;
      }
      default:
        throw InvalidUsageException.shorthandRequiresCodeOrIndicator(labelBlock, fieldId, fieldType);
    }
  }

  /**
   * Returns the number that the given field is the unit of: the value of the amount, measure or
   * duration that holds it as its attribute, on the same element. It is empty if the given field is not
   * such a unit, or if that number repeats, because a label is pluralised by a single quantity.
   */
  private NumericExpression composeQuantityOfUnit(final String unitFieldId) {
    final String fieldId = this.translation.symbols().getFieldIdOfAttributeField(unitFieldId);
    if (fieldId == null) {
      return NumericExpression.empty();
    }
    switch (FieldTypes.fromString(this.translation.symbols().getTypeOfField(fieldId))) {
      case DURATION:
      case MEASURE:
      case AMOUNT:
        break;
      default:
        return NumericExpression.empty();
    }
    final PathExpression field = this.translation.symbols().getRelativePathOfField(fieldId, this.translation.efxContext().symbol());
    if (field instanceof SequenceExpression) {
      return NumericExpression.empty();
    }
    return TypedExpression.from(
        this.translation.script().composeFieldValueReference(new NumericPath(field.getScript())), NumericExpression.class);
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
  public void exitShorthandLabelIdFromContext(ShorthandLabelIdFromContextContext ctx) {
    final String labelType = ctx.LabelType().getText();
    if (this.translation.efxContext().isFieldContext()) {
      if (labelType.equals(SHORTHAND_CONTEXT_FIELD_LABEL_REFERENCE)) {
        this.shorthandIndirectLabelReference(ctx.getParent(), this.translation.efxContext().symbol());
      } else {
        StringExpression key = this.translation.script().composeStringConcatenation(
            List.of(this.translation.script().getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
                this.translation.script().getStringLiteralFromUnquotedString("|"),
                this.translation.script().getStringLiteralFromUnquotedString(labelType),
                this.translation.script().getStringLiteralFromUnquotedString("|"),
                this.translation.script().getStringLiteralFromUnquotedString(
                    this.translation.efxContext().symbol())));
        this.translation.stack().push(new LabelFromKeyContentTemplateFragment(key,
            NumericExpression.empty()));
      }
    } else if (this.translation.efxContext().isNodeContext()) {
      StringExpression key = this.translation.script().composeStringConcatenation(
          List.of(this.translation.script().getStringLiteralFromUnquotedString(ASSET_TYPE_NODE),
              this.translation.script().getStringLiteralFromUnquotedString("|"),
              this.translation.script().getStringLiteralFromUnquotedString(labelType),
              this.translation.script().getStringLiteralFromUnquotedString("|"),
              this.translation.script().getStringLiteralFromUnquotedString(
                  this.translation.efxContext().symbol())));
      this.translation.stack().push(new LabelFromKeyContentTemplateFragment(key,
          NumericExpression.empty()));
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
    if (!this.translation.efxContext().isFieldContext()) {
      throw InvalidUsageException.shorthandRequiresFieldContext(ctx, "#value");
    }
    this.shorthandIndirectLabelReference(ctx, this.translation.efxContext().symbol());
  }

  /**
   * Takes a part of a label block off the stack, where the part, or its expression block, left
   * it, and converts it to the class the part requires. The error of a part that does not convert
   * names that part alone, since each part is taken, and checked, on its own.
   *
   * @param labelBlock the label block, where an error is reported
   * @param part the part, such as {@link #ASSET_ID}
   * @return the part as the construct's one argument, converted to the class it requires
   * @throws eu.europa.ted.efx.exceptions.TypeMismatchException if the part does not convert to
   *         that class
   */
  private Arguments<?> labelPart(final ParserRuleContext labelBlock, final Clause part) {
    return this.expressions.valueOf(this.translation.stack().pop(TypedExpression.class), part,
        labelBlock);
  }

  @Override
  public void exitLabelType(LabelTypeContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.translation.stack().push(this.translation.script().getStringLiteralFromUnquotedString(
          ctx.getText()));
    }
  }

  @Override
  public void exitAssetId(AssetIdContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.translation.stack().push(this.translation.script().getStringLiteralFromUnquotedString(
          ctx.getText()));
    }
  }
 
  // #region New in EFX-2 -----------------------------------------------------

  @Override
  public void exitDictionaryDeclaration(DictionaryDeclarationContext ctx) {
    String name = ctx.dictionaryName.getText();
    StringExpression key = this.translation.stack().pop(StringExpression.class);
    var match = this.translation.stack().pop(PathExpression.class);

    // Declare the dictionary in the script
    var dictionary = new Dictionary(name, match, key);
    this.translation.stack().declareGlobalIdentifier(dictionary);
    this.viewTemplate.addDictionary(dictionary);
  }

  /**
   * Leaves the path of the indexed field for the declaration, and switches to the context of the
   * field, which the key is read in.
   */
  @Override
  public void exitDictionaryIndexClause(DictionaryIndexClauseContext ctx) {
    final Context index = this.expressions.contextOf(ctx.fieldContext());
    this.translation.stack().push(index.relativePath());
    this.translation.efxContext().pushFieldContext(index.symbol());
  }

  /**
   * Takes the value of the key as the text it is, and then switches back from the context of the
   * indexed field. The value is taken first, so that a field of the key that may repeat is
   * reported with the context the key reads it from.
   */
  @Override
  public void exitDictionaryKeyClause(DictionaryKeyClauseContext ctx) {
    this.translation.stack().push(this.expressions.valueOf(ctx.expression(), BY).stringAt(0));
    this.translation.efxContext().pop();
  }

  // #endregion New in EFX-2 --------------------------------------------------

  // #endregion Label Blocks #{...} -------------------------------------------
  
  // #region Expression Blocks ${...} -----------------------------------------

  /***
   * Handles the $value shorthand syntax which renders the value of the field declared as context in
   * the current line of the template.
   */
  @Override
  public void exitShorthandFieldValueReferenceFromContextField(
      ShorthandFieldValueReferenceFromContextFieldContext ctx) {
    if (!this.translation.efxContext().isFieldContext()) {
      throw InvalidUsageException.shorthandRequiresFieldContext(ctx, "$value");
    }
    this.translation.stack().push(this.translation.script().composeFieldValueReference(
        this.translation.symbols().getRelativePathOfField(this.translation.efxContext().symbol(),
            this.translation.efxContext().symbol())));
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
        this.declareRootLineContext();
        break;
      default:
        final Context declared = this.declaredContextOf(ctx);
        Variable contextVariable = null;
        if (ctx.contextVariableInitializer() != null) {
          contextVariable =
              this.getContextVariable(ctx.contextVariableInitializer(), declared.relativePath());
          this.translation.stack().peek(Variables.class).add(contextVariable);
        }
        this.declareContext(declared, contextVariable);
        break;
    }
  }

  private void exitSameContextDeclaration() {
    final Context currentContext = this.blockStack.currentContext();
    // A line nested in the line before it stands in that line's element. A line after it, at the
    // same level, stands in the same line above, so it takes that line's context as it is.
    if (this.blockAbove != null && this.blockAbove == this.blockStack.peek()) {
      this.declareLineContext(currentContext);
    } else {
      this.declareContext(currentContext, null);
    }
  }

  private void exitParentContextDeclaration() {
    this.declareLineContext(this.blockStack.parentContext());
  }

  /**
   * Declares the root as the context of a template line that writes it, {@code /}, or that
   * declares no context.
   */
  private void declareRootLineContext() {
    this.declareLineContext(new NodeContext(this.translation.symbols().getRootNodeId(),
        this.translation.symbols().getRootPath()));
  }

  /**
   * Declares the context of a template line that names a context by its absolute path: the root,
   * or the context of another line. Its path from the context above is the path between the
   * two.
   *
   * @param named the context the line names
   */
  private void declareLineContext(final Context named) {
    this.declareContext(this.relativizeContext(named, this.contextAbove()), null);
  }

  // #region Conditional template blocks --------------------------------------


  @Override
  public void exitWhenDisplayTemplate(WhenDisplayTemplateContext ctx) {
    var template = this.translation.stack().pop(DisplayContentTemplate.class);
    template.setCondition(
        this.expressions.valueOf(ctx.expression(), WHEN).booleanAt(0));
    this.translation.stack().push(template);
  }

  @Override
  public void exitWhenInvokeTemplate(WhenInvokeTemplateContext ctx) {
    var template = this.translation.stack().pop(InvokeContentTemplate.class);
    template.setCondition(
        this.expressions.valueOf(ctx.expression(), WHEN).booleanAt(0));
    this.translation.stack().push(template);
  }

  /**
   * Invokes a declared template, with its arguments matched against the parameters it declares.
   */
  @Override
  public void exitInvokeTemplate(InvokeTemplateContext ctx) {
    final Template template = this.translation.stack().getTemplate(ctx.templateName.getText());
    this.translation.stack().push(new InvokeContentTemplate(ctx.templateName.getText(),
        this.expressions.argumentsOf(ctx, template, ctx.argumentList())));
  }
  
  // #endregion Conditional template blocks ---------------------------------

  // #region Variable Initializers --------------------------------------------

  /**
   * A variable is initialized with a value of its declared type.
   */
  @Override
  public void exitTypedVariableInitializer(TypedVariableInitializerContext ctx) {
    var variableName = ctx.variableName.getText();
    var expression = this.expressions.initialValueOf(ctx);
    var variable = new Variable(variableName,
        this.translation.script().composeVariableDeclaration(variableName, expression.getClass()),
        expression,
        this.translation.script().composeVariableReference(variableName, expression.getClass()));
    this.exitVariableInitializer(ctx, variable);
  }

  /**
   * A variable of a context declaration block is declared at once, so that the initializers after
   * it can read it. The variable of a global declaration is declared when the declaration exits.
   */
  private void exitVariableInitializer(VariableInitializerContext initializer, Variable variable) {
    if (initializer.getParent() instanceof VariableListContext) {
      this.translation.stack().declareIdentifier(variable);
      this.translation.stack().peek(Variables.class).add(variable);
    } else {
      this.translation.stack().push(variable);
    }
  }

  // #endregion Variable Initializers -----------------------------------------

  // #region Hyperlinks -------------------------------------------------------

  /**
   * Displays a fragment as a hyperlink when a link block follows it, as in
   * {@code ${'a link'}@{'https://example.com'}}.
   *
   * @param fragment the text, the label or the value displayed
   * @param link the link block that follows the fragment, or null if there is none
   * @return the fragment, as a hyperlink to the URL of the link block if there is one
   */
  private ContentTemplateFragment linked(final ContentTemplateFragment fragment,
      final LinkBlockContext link) {
    if (link == null) {
      return fragment;
    }
    final StringExpression url = this.expressions.valueOf(link.expression(), HYPERLINK).stringAt(0);
    return new HyperlinkContentTemplateFragment(fragment, url);
  }

  // #endregion Hyperlinks ----------------------------------------------------

  // #endregion New in EFX-2 --------------------------------------------------

  @Override
  public void enterContextDeclarationBlock(ContextDeclarationBlockContext arg0) {
    this.translation.stack().push(new Variables());
  }




  // #endregion Context Declaration Blocks {...} ------------------------------
  
  @Override
  public void enterSummarySection(SummarySectionContext ctx) {
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
    final int indentLevel = this.getIndentLevel(ctx.indentation());
    this.enterTemplateLine(ctx, indentLevel);
    this.blockAbove = this.blockStack.blockAtLevel(indentLevel - 1);
    if (ctx.contextDeclarationBlock() == null) {
      this.declareRootLineContext();
      this.translation.stack().push(new Variables());
    }
  }

  @Override
  public void enterTemplateDeclaration(TemplateDeclarationContext ctx) {
    this.enterTemplateLine(ctx, 0);
  }

  private void enterTemplateLine(ParserRuleContext ctx, final int indentLevel) {
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    if (indentChange > 0 && this.blockStack.isEmpty()) {
      throw InvalidIndentationException.startIndentAtZero(ctx);
    } else if (indentChange > 1) {
      throw InvalidIndentationException.indentationLevelSkipped(ctx);
    } else if (indentChange == 1) {
      this.translation.stack().pushStackFrame(); // Create a stack frame for the new template line.
    } else if (indentChange < 0) {
      for (int i = indentChange; i < 0; i++) {
        assert !this.blockStack.isEmpty() : UNEXPECTED_INDENTATION;
        assert this.blockStack.currentIndentationLevel() > indentLevel : UNEXPECTED_INDENTATION;
        this.blockStack.pop();
        // Each skipped indentation level must go out of scope.
        this.translation.stack().popStackFrame();
      }
      // The previous sibling goes out of scope (same indentation level).
      this.translation.stack().popStackFrame();
      this.translation.stack().pushStackFrame(); // Create a stack frame for the new template line.
      assert this.blockStack.currentIndentationLevel() == indentLevel : UNEXPECTED_INDENTATION;
    } else if (indentChange == 0) {
      // The previous sibling goes out of scope (same indentation level).
      this.translation.stack().popStackFrame();
      this.translation.stack().pushStackFrame(); // Create a stack frame for the new template line.
    }
  }

  /**
   * @return the context of the line above the template line being translated, which the line is
   *         nested in, or {@code null} when there is none, as for a line at the top
   */
  private Context contextAbove() {
    return this.blockAbove != null ? this.blockAbove.getContext() : null;
  }

  @Override
  public void exitTemplateLine(TemplateLineContext ctx) {
    final Context lineContext = this.translation.efxContext().pop();
    final int indentLevel = this.getIndentLevel(ctx.indentation());
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    // What the line displays is on the stack in order: a content template for each WHEN alternative,
    // then the one displayed otherwise.
    final LinkedList<ContentTemplate> contentTemplates = new LinkedList<>();
    while (this.translation.stack().peek() instanceof ContentTemplate) {
      contentTemplates.addFirst(this.translation.stack().pop(ContentTemplate.class));
    }
    final Variables variables = this.translation.stack().pop(Variables.class);
    final int outlineNumber = this.outlineNumberOf(ctx);
    assert this.translation.stack().empty() : "Stack should be empty at this point.";

    if (indentChange > 0 && this.blockStack.isEmpty()) {
      throw InvalidIndentationException.startIndentAtZero(ctx);
    } else if (indentChange > 1) {
      throw InvalidIndentationException.indentationLevelSkipped(ctx);
    } else if (indentChange == 1) {
      this.blockStack.pushChild(outlineNumber, lineContext, variables, contentTemplates);
    } else if (indentLevel == 0) {
      // A top-level line is a new block of the current section, also when it follows a declared
      // template and its lines.
      this.blockStack.clear();
      final ContentBlock sectionRoot = this.viewTemplate.getSection(this.translatorContext.getCurrentSection()).getRoot();
      this.blockStack.push(sectionRoot.addChild(outlineNumber, lineContext, variables,
          contentTemplates));
    } else {
      this.blockStack.pushSibling(outlineNumber, lineContext, variables, contentTemplates);
    }
  }

  @Override
  public void exitTemplateDeclaration(TemplateDeclarationContext ctx) {
    // What the line displays is on the stack in order: a content template for each WHEN alternative,
    // then the one displayed otherwise.
    final LinkedList<ContentTemplate> contentTemplates = new LinkedList<>();
    while (this.translation.stack().peek() instanceof ContentTemplate) {
      contentTemplates.addFirst(this.translation.stack().pop(ContentTemplate.class));
    }
    final Template template = this.translation.stack().pop(Template.class);
    assert this.translation.stack().empty() : "Stack should be empty at this point.";

    if (this.getIndentLevel(ctx.indentation()) != 0) {
      throw InvalidIndentationException.noIndentOnTemplateDeclarations(ctx);
    }

    this.blockStack.clear();
    this.blockStack.push(this.viewTemplate.getTemplateDeclarations().addChild(template.name, contentTemplates, template.parameters));
  }

  /**
   * Translates the context a template line declares by a field or a node, or by a context
   * variable assigned one, where the line stands: in the context of the line above, so that an
   * index after the reference, and every position, condition and reference in its brackets, is
   * read within the element of that line, as in an expression.
   *
   * @param declaration the context declaration of the line
   * @return the context it declares: the absolute path of its field or node, and its path from
   *         the context above
   */
  private Context declaredContextOf(final ContextDeclarationContext declaration) {
    final Context contextAbove = this.contextAbove();
    if (contextAbove == null) {
      return this.translatedContextOf(declaration);
    }
    this.translation.efxContext().push(contextAbove);
    final Context declared = this.translatedContextOf(declaration);
    this.translation.efxContext().pop();
    return declared;
  }

  /**
   * @param declaration the context declaration of a template line, by a field or a node, or by a
   *        context variable assigned one
   * @return the context it declares, translated in the current context
   */
  private Context translatedContextOf(final ContextDeclarationContext declaration) {
    return declaration.contextVariableInitializer() != null
        ? this.expressions.contextAssignedBy(declaration.contextVariableInitializer())
        : this.expressions.contextOf(declaration.getRuleContext(ParserRuleContext.class, 0));
  }

  private Context relativizeContext(Context childContext, Context parentContext) {
    if (parentContext == null) {
      return childContext;
    }

    PathExpression parentContextAbsolutePath = parentContext.isFieldContext()
        ? this.translation.symbols().getAbsolutePathOfField(parentContext.symbol())
        : this.translation.symbols().getAbsolutePathOfNode(parentContext.symbol());

    if (childContext.isFieldContext()) {
      return new FieldContext(childContext.symbol(), childContext.absolutePath(),
          this.translation.script().contextualizePath(childContext.absolutePath(), parentContextAbsolutePath), childContext.variable());
    }

    assert childContext.isNodeContext() : "Child context should be either a FieldContext or a NodeContext.";

    return new NodeContext(childContext.symbol(), childContext.absolutePath(),
        this.translation.script().contextualizePath(childContext.absolutePath(),
            parentContextAbsolutePath));
  }

  // #endregion Template lines  -----------------------------------------------
  
  // #region Helpers ----------------------------------------------------------

  private static String getLexerSymbol(int tokenType) {
    return EfxLexer.VOCABULARY.getLiteralName(tokenType).replaceAll("^'|'$", "");
  }

  /**
   * @return the outline number written at the start of the line, or -1 when there is none
   * @throws InvalidUsageException if the number is larger than the largest {@code int}
   */
  private int outlineNumberOf(TemplateLineContext ctx) {
    if (ctx.OutlineNumber() == null) {
      return -1;
    }
    final String number = ctx.OutlineNumber().getText().trim();
    try {
      return Integer.parseInt(number);
    } catch (NumberFormatException e) {
      throw InvalidUsageException.outlineNumberTooLarge(ctx.OutlineNumber().getSymbol(), number);
    }
  }

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
          throw InvalidIndentationException.inconsistentSpaces(ctx, ctx.Spaces().getText().length(),
              this.indentSpaces);
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


  /**
   * Translates the expressions of a template, which may call the built-ins that only templates may
   * call, such as the formatting functions.
   */
  private static final class TemplateExpressionVisitor extends EfxExpressionVisitor {

    TemplateExpressionVisitor(final Translation translation) {
      super(translation);
    }

    @Override
    protected boolean translatesTemplateOnlyBuiltIns() {
      return true;
    }
  }
}
