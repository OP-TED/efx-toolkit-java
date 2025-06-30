package eu.europa.ted.efx.sdk2;

import java.io.IOException;
import java.io.InputStream;
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
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.path.NodePathExpression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.expressions.path.StringPathExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
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
import eu.europa.ted.efx.sdk2.EfxParser.AssetIdContext;
import eu.europa.ted.efx.sdk2.EfxParser.AssetTypeContext;
import eu.europa.ted.efx.sdk2.EfxParser.BooleanFunctionDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.BooleanParameterDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.BooleanVariableInitializerContext;
import eu.europa.ted.efx.sdk2.EfxParser.ChooseTemplateContext;
import eu.europa.ted.efx.sdk2.EfxParser.ComputedLabelReferenceContext;
import eu.europa.ted.efx.sdk2.EfxParser.ContextDeclarationBlockContext;
import eu.europa.ted.efx.sdk2.EfxParser.ContextDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.ContextVariableInitializerContext;
import eu.europa.ted.efx.sdk2.EfxParser.DateFunctionDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.DateParameterDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.DateVariableInitializerContext;
import eu.europa.ted.efx.sdk2.EfxParser.DictionaryDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.DurationFunctionDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.DurationParameterDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.DurationVariableInitializerContext;
import eu.europa.ted.efx.sdk2.EfxParser.ExpressionTemplateContext;
import eu.europa.ted.efx.sdk2.EfxParser.GlobalVariableDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.IndentationContext;
import eu.europa.ted.efx.sdk2.EfxParser.InvokeTemplateContext;
import eu.europa.ted.efx.sdk2.EfxParser.LabelTemplateContext;
import eu.europa.ted.efx.sdk2.EfxParser.LabelTypeContext;
import eu.europa.ted.efx.sdk2.EfxParser.NumericFunctionDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.NumericParameterDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.NumericVariableInitializerContext;
import eu.europa.ted.efx.sdk2.EfxParser.SecondaryTemplateContext;
import eu.europa.ted.efx.sdk2.EfxParser.ShorthandBtLabelReferenceContext;
import eu.europa.ted.efx.sdk2.EfxParser.ShorthandFieldLabelReferenceContext;
import eu.europa.ted.efx.sdk2.EfxParser.ShorthandFieldValueReferenceFromContextFieldContext;
import eu.europa.ted.efx.sdk2.EfxParser.ShorthandIndirectLabelReferenceContext;
import eu.europa.ted.efx.sdk2.EfxParser.ShorthandIndirectLabelReferenceFromContextFieldContext;
import eu.europa.ted.efx.sdk2.EfxParser.ShorthandLabelReferenceFromContextContext;
import eu.europa.ted.efx.sdk2.EfxParser.StandardExpressionBlockContext;
import eu.europa.ted.efx.sdk2.EfxParser.StandardLabelReferenceContext;
import eu.europa.ted.efx.sdk2.EfxParser.StringFunctionDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.StringParameterDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.StringVariableInitializerContext;
import eu.europa.ted.efx.sdk2.EfxParser.TemplateDefinitionContext;
import eu.europa.ted.efx.sdk2.EfxParser.TemplateDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.TemplateFileContext;
import eu.europa.ted.efx.sdk2.EfxParser.TemplateLineContext;
import eu.europa.ted.efx.sdk2.EfxParser.TemplateVariableDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.TextTemplateContext;
import eu.europa.ted.efx.sdk2.EfxParser.TimeFunctionDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.TimeParameterDeclarationContext;
import eu.europa.ted.efx.sdk2.EfxParser.TimeVariableInitializerContext;
import eu.europa.ted.efx.sdk2.EfxParser.WhenDisplayTemplateContext;
import eu.europa.ted.efx.sdk2.EfxParser.WhenInvokeTemplateContext;

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

  final ContentBlock rootBlock = ContentBlock.newRootBlock();

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
  public String renderTemplate(final Path pathname) throws IOException {

    return renderTemplate(CharStreams.fromPath(pathname));
  }

  /**
   * Translates the template contained in the string passed as a parameter.
   */
  @Override
  public String renderTemplate(final String template) {
    return renderTemplate(CharStreams.fromString(template));
  }

  @Override
  public String renderTemplate(final InputStream stream) throws IOException {
    return renderTemplate(CharStreams.fromStream(stream));
  }

  private String renderTemplate(final CharStream charStream) {
    logger.debug("Rendering template");

    // New in EFX-2: template preprocessing
    final TemplatePreprocessor preprocessor = this.new TemplatePreprocessor(charStream);
    final String preprocessedTemplate = preprocessor.processTemplate();

    // Now parse the preprocessed template
    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(preprocessedTemplate));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);

    if (errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }

    final ParseTree tree = parser.templateFile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(this, tree);

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

  // #region Global declarationExpressions ---------------------------------------

  @Override
  public void exitGlobalVariableDeclaration(GlobalVariableDeclarationContext ctx) {
    var variable = this.stack.pop(Variable.class);
    this.stack.declareGlobalIdentifier(variable);
  }

  @Override
  public void enterStringFunctionDeclaration(StringFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitStringFunctionDeclaration(StringFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText(), EfxDataType.String.class);
  }

  @Override
  public void enterBooleanFunctionDeclaration(BooleanFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitBooleanFunctionDeclaration(BooleanFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText(), EfxDataType.Boolean.class);
  }

  @Override
  public void enterNumericFunctionDeclaration(NumericFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitNumericFunctionDeclaration(NumericFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText(), EfxDataType.Number.class);
  }

  @Override
  public void enterDateFunctionDeclaration(DateFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitDateFunctionDeclaration(DateFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText(), EfxDataType.Date.class);
  }

  @Override
  public void enterTimeFunctionDeclaration(TimeFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitTimeFunctionDeclaration(TimeFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText(), EfxDataType.Time.class);
  }

  @Override
  public void enterDurationFunctionDeclaration(DurationFunctionDeclarationContext ctx) {
    this.stack.push(new ParsedParameters());
  }

  @Override
  public void exitDurationFunctionDeclaration(DurationFunctionDeclarationContext ctx) {
      this.exitFunctionDeclaration(ctx.functionName.getText(), EfxDataType.Duration.class);
  }

  private void exitFunctionDeclaration(String functionName, Class<? extends EfxDataType> returnType) {
    var expression = this.stack.pop(TypedExpression.class);
    var parameters = this.stack.pop(ParsedParameters.class);

    this.stack.declareFunction(new Function(functionName, returnType, parameters, expression));
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
        globals.add(this.markup.renderVariableDeclaration(variable.dataType, variable.name, variable.initializationExpression));
      } else if (identifier instanceof Function) {
        Function function = (Function) identifier;
        globals.add(this.markup.renderFunctionDeclaration(function.dataType, function.name, function.parameters.toMap(), function.expression));
      } else if (identifier instanceof Dictionary) {
        Dictionary dictionary = (Dictionary) identifier;
        globals.add(this.markup.renderDictionaryDeclaration(dictionary.name, dictionary.pathExpression, dictionary.keyExpression));
      }
    }

    List<Markup> templateCalls = new ArrayList<>();
    List<Markup> templates = new ArrayList<>();
    for (ContentBlock block : this.rootBlock.getChildren()) {
      if (!(block instanceof TemplateDefinition)) {
        templateCalls.add(block.renderInvocation(markup));
      }
      templates.addAll(block.renderDefinition(markup));
    }
    Markup file = this.markup.composeOutputFile(globals, templateCalls, templates);
    this.stack.push(file);
  }

  // #endregion Template File -------------------------------------------------
  
  // #region Source template blocks -------------------------------------------

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    Markup template =
        ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    String text = ctx.textBlock() != null ? ctx.textBlock().getText() : "";
    this.stack.push(this.markup.renderFreeText(this.markup.escapeSpecialCharacters(text)).join(template));
  }

  @Override
  public void exitLabelTemplate(LabelTemplateContext ctx) {
    Markup template =
        ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    Markup label = ctx.labelBlock() != null ? this.stack.pop(Markup.class) : Markup.empty();
    this.stack.push(label.join(template));
  }

  @Override
  public void exitExpressionTemplate(ExpressionTemplateContext ctx) {
    Markup template =
        ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    Expression expression = this.stack.pop(Expression.class);
    this.stack.push(this.markup.renderVariableExpression(expression).join(template));
  }

  // #region New in EFX-2: Secondary templates --------------------------------

  @Override
  public void exitSecondaryTemplate(SecondaryTemplateContext ctx) {
    Markup template = ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    this.stack.push(this.markup.renderLineBreak().join(template));
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
            this.script.getStringLiteralFromUnquotedString("|"), assetId)), quantity));
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
            StringSequenceExpression.class)));
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
            this.script.getStringLiteralFromUnquotedString("|"), assetId)), quantity));
  }

  @Override
  public void exitShorthandFieldLabelReference(ShorthandFieldLabelReferenceContext ctx) {
    final String fieldId = ctx.FieldId().getText();
    NumericExpression quantity = ctx.pluraliser() != null ? this.stack.pop(NumericExpression.class) : NumericExpression.empty();
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");

    if (labelType.getScript().equals("value")) {
      this.shorthandIndirectLabelReference(fieldId, quantity);
    } else {
      this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
          List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
              this.script.getStringLiteralFromUnquotedString("|"), labelType,
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(fieldId))), quantity));
    }
  }

  @Override
  public void exitShorthandIndirectLabelReference(ShorthandIndirectLabelReferenceContext ctx) {
    // New in EFX-2: Pluralisation of labels based on a supplied quantity
    NumericExpression quantity = ctx.pluraliser() != null ? this.stack.pop(NumericExpression.class) : NumericExpression.empty();
    this.shorthandIndirectLabelReference(ctx.FieldId().getText(), quantity);
  }

  private void shorthandIndirectLabelReference(final String fieldId, final NumericExpression quantity) {
    final Context currentContext = this.efxContext.peek();
    final String fieldType = this.symbols.getTypeOfField(fieldId);
    final PathExpression valueReference = this.symbols.isAttributeField(fieldId)
        ? this.script.composeFieldAttributeReference(
            this.symbols.getRelativePath(this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId),
                currentContext.absolutePath()),
            this.symbols.getAttributeNameFromAttributeField(fieldId), StringPathExpression.class)
        : this.script.composeFieldValueReference(
        this.symbols.getRelativePathOfField(fieldId, currentContext.absolutePath()));
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
                            this.script.composeIteratorExpression(loopVariable.declarationExpression, valueReference))),
                    this.script.composeStringConcatenation(
                        List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_INDICATOR),
                            this.script.getStringLiteralFromUnquotedString("|"),
                            this.script.getStringLiteralFromUnquotedString(LABEL_TYPE_WHEN),
                            this.script.getStringLiteralFromUnquotedString("-"),
                            new StringExpression(loopVariable.referenceExpression.getScript()),
                            this.script.getStringLiteralFromUnquotedString("|"),
                            this.script.getStringLiteralFromUnquotedString(fieldId))),
                    StringSequenceExpression.class),
                StringSequenceExpression.class), quantity));
        break;
      case "code":
      case "internal-code":
        this.stack.push(this.markup.renderLabelFromExpression(
            this.script.composeDistinctValuesFunction(
                this.script.composeForExpression(
                    this.script.composeIteratorList(
                        List.of(
                            this.script.composeIteratorExpression(loopVariable.declarationExpression, valueReference))),
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
            quantity));
        break;
      default:
        throw InvalidUsageException.shorthandRequiresCodeOrIndicator(fieldId, fieldType);
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
        this.shorthandIndirectLabelReference(this.efxContext.symbol(), quantity);
      } else {
        this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
            List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(labelType),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(this.efxContext.symbol()))), quantity));
      }
    } else if (this.efxContext.isNodeContext()) {
      this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
          List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_NODE),
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(labelType),
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(this.efxContext.symbol()))), quantity));
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
      throw InvalidUsageException.shorthandRequiresFieldContext("#value");
    }
    this.shorthandIndirectLabelReference(this.efxContext.symbol(), NumericExpression.empty());
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
    this.stack.push(this.markup.renderLabelFromExpression(expression, quantity));
  }

  @Override
  public void exitDictionaryDeclaration(DictionaryDeclarationContext ctx) {
    String name = ctx.dictionaryName.getText();
    StringExpression key = this.stack.pop(StringExpression.class);
    var match = this.stack.pop(PathExpression.class);

    // Declare the dictionary in the script
    this.stack.declareGlobalIdentifier(new Dictionary(name, match, key));
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

    // TODO: Review this in EFX-2

    // This is a hack to make sure that the date and time expressions are rendered in the correct
    // format. We had to do this because EFX 1 does not support the format-date() and format-time()
    // functions.
    if (TypedExpression.class.isAssignableFrom(expression.getClass())) {
      if (EfxDataType.Date.class.isAssignableFrom(((TypedExpression) expression).getDataType())) {

        var loopVariable = new Variable("item",
            this.script.composeVariableDeclaration("item", DateExpression.class), DateExpression.empty(),
            this.script.composeVariableReference("item", DateExpression.class));

        expression = this.script.composeForExpression(
            this.script.composeIteratorList(
                List.of(this.script.composeIteratorExpression(loopVariable.declarationExpression,
                    new DateSequenceExpression(expression.getScript())))),
            new StringExpression("format-date($item, '[D01]/[M01]/[Y0001]')"),
            StringSequenceExpression.class);
      } else if (EfxDataType.Time.class.isAssignableFrom(((TypedExpression) expression).getDataType())) {

        var loopVariable = new Variable("item",
            this.script.composeVariableDeclaration("item", DateExpression.class), DateExpression.empty(),
            this.script.composeVariableReference("item", DateExpression.class));

        expression = this.script.composeForExpression(
            this.script.composeIteratorList(
                List.of(this.script.composeIteratorExpression(loopVariable.declarationExpression,
                    new TimeSequenceExpression(expression.getScript())))),
            new StringExpression("format-time($item, '[H01]:[m01] [Z]')"),
            StringSequenceExpression.class);
      }
    }

    this.stack.push(expression);
  }

  /***
   * Handles the $value shorthand syntax which renders the value of the field declared as context in
   * the current line of the template.
   */
  @Override
  public void exitShorthandFieldValueReferenceFromContextField(
      ShorthandFieldValueReferenceFromContextFieldContext ctx) {
    if (!this.efxContext.isFieldContext()) {
      throw InvalidUsageException.shorthandRequiresFieldContext("$value");
    }
    this.stack.push(this.script.composeFieldValueReference(
        this.symbols.getRelativePathOfField(this.efxContext.symbol(), this.efxContext.absolutePath())));
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
        String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
        if (fieldId != null) {
          Variable contextVariable = this.getContextVariable(ctx, contextPath);
          if (contextVariable != null) {
            this.stack.peek(Variables.class).add(contextVariable);
          }
          this.exitFieldContextDeclaration(fieldId, contextPath, contextVariable);
        } else {
          String nodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx);
          assert nodeId != null : "We should have been able to locate the FieldId or NodeId declared as context.";
          this.exitNodeContextDeclaration(nodeId, contextPath);
        }
        break;
    }
  }

  private void exitSameContextDeclaration() {
    Context currentContext = this.blockStack.currentContext();
    PathExpression contextPath = currentContext.absolutePath();
    String symbol = currentContext.symbol();
    if (currentContext.isFieldContext()) {
      this.exitFieldContextDeclaration(symbol, contextPath, null);
    } else if (currentContext.isNodeContext()) {
      this.exitNodeContextDeclaration(symbol, contextPath);
    }
  }

  private void exitParentContextDeclaration() {
    Context parentContext = this.blockStack.parentContext();
    PathExpression contextPath = parentContext.absolutePath();
    String symbol = parentContext.symbol();
    if (parentContext.isFieldContext()) {
      this.exitFieldContextDeclaration(symbol, contextPath, null);
    } else if (parentContext.isNodeContext()) {
      this.exitNodeContextDeclaration(symbol, contextPath);
    }
  }

  private void exitRootContextDeclaration() {
    PathExpression contextPath = new NodePathExpression("/*");
    String symbol = "ND-Root";
    this.exitNodeContextDeclaration(symbol, contextPath);
  }

  private void exitFieldContextDeclaration(String fieldId, PathExpression contextPath, Variable contextVariable) {
    this.efxContext.push(new FieldContext(fieldId, contextPath, contextVariable));
    if (contextVariable != null) {
      this.stack.declareIdentifier(contextVariable);
    }
  }

  private void exitNodeContextDeclaration(String nodeId, PathExpression contextPath) {
    this.efxContext.push(new NodeContext(nodeId, contextPath));
  }

  private Variable getContextVariable(ContextDeclarationContext ctx,
      PathExpression contextPath) {
    if (ctx.contextVariableInitializer() == null) {
      return null;
    }

    final String variableName = ctx.contextVariableInitializer().variableName.getText();
    final Class<? extends TypedExpression> variableType = contextPath.getClass();

    return new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, variableType),
        this.symbols.getRelativePath(contextPath, contextPath),
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
    this.stack.push(this.markup.renderFragmentInvocation(templateName, args));
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
  public void exitTemplateVariableDeclaration(TemplateVariableDeclarationContext arg0) {
    var variable = this.stack.pop(Variable.class);
    this.stack.declareIdentifier(variable);
    this.stack.peek(Variables.class).add(variable);
  }

  // #endregion Variable Initializers -----------------------------------------

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

  private void exitParameterDeclaration(String parameterName, Class<? extends TypedExpression> parameterType) {
    ParsedParameter parameter = new ParsedParameter(parameterName,
        this.script.composeParameterReference(parameterName, parameterType));
    this.stack.declareIdentifier(parameter);
    this.stack.peek(ParsedParameters.class).add(parameter);
  }

  // #endregion Parameter Declarations ----------------------------------------
  // #region Template lines  --------------------------------------------------

  @Override
  public void enterTemplateLine(TemplateLineContext ctx) {
    this.enterTemplateLine(this.getIndentLevel(ctx.indentation()));
    if (ctx.contextDeclarationBlock() == null) {
      this.exitRootContextDeclaration();
      this.stack.push(new Variables());
    }
  }

  @Override
  public void enterTemplateDeclaration(TemplateDeclarationContext ctx) {
    this.enterTemplateLine(0);
  }

  private void enterTemplateLine(final int indentLevel) {
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    if (indentChange > 1) {
      throw InvalidIndentationException.indentationLevelSkipped();
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero();
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
      throw InvalidIndentationException.indentationLevelSkipped();
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero();
      }
      if (this.blockStack.peek() instanceof TemplateInvocation) {
        throw InvalidIndentationException.noNestingOnInvocations();
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
      throw InvalidIndentationException.noIndentOnTemplateDeclarations();
    }

    this.blockStack
        .push(this.rootBlock.addChild(template.name, conditionals, defaultContent, template.parameters));
  }

  private Context relativizeContext(Context childContext, Context parentContext) {
    if (parentContext == null) {
      return childContext;
    }

    if (childContext.isFieldContext()) {
      return new FieldContext(childContext.symbol(), childContext.absolutePath(),
          this.symbols.getRelativePath(childContext.absolutePath(), parentContext.absolutePath()), childContext.variable());
    }

    assert childContext.isNodeContext() : "Child context should be either a FieldContext NodeContext.";

    return new NodeContext(childContext.symbol(), childContext.absolutePath(),
        this.symbols.getRelativePath(childContext.absolutePath(), parentContext.absolutePath()));
  }

  // #endregion Template lines  -----------------------------------------------
  
  // #region Helpers ----------------------------------------------------------

    private int getIndentLevel(IndentationContext ctx) {

      if (ctx == null) {
        return 0; // No indentation, default to 0.
      }

      if (ctx.MixedIndent() != null) {
          throw InvalidIndentationException.mixedIndentation();
      }

      if (ctx.Spaces() != null) {
        if (this.indentWith == Indent.UNDETERMINED) {
          this.indentWith = Indent.SPACES;
          this.indentSpaces = ctx.Spaces().getText().length();
        } else if (this.indentWith == Indent.TABS) {
          throw InvalidIndentationException.mixedIndentation();
        }

        if (ctx.Spaces().getText().length() % this.indentSpaces != 0) {
          throw InvalidIndentationException.inconsistentSpaces(this.indentSpaces);
        }
        return ctx.Spaces().getText().length() / this.indentSpaces;
      } else if (ctx.Tabs() != null) {
        if (this.indentWith == Indent.UNDETERMINED) {
          this.indentWith = Indent.TABS;
        } else if (this.indentWith == Indent.SPACES) {
          throw InvalidIndentationException.mixedIndentation();
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

    TemplatePreprocessor(CharStream template) {
      super(template);
    }

    String processTemplate() {
      final ParseTree tree = parser.templateFile();
      final ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(this, tree);
      return this.rewriter.getText();
    }

    // #region Template Variables ---------------------------------------------
  
    @Override
    public void exitGlobalVariableDeclaration(GlobalVariableDeclarationContext arg0) {
      var variable = this.stack.pop(Variable.class);
      this.stack.declareGlobalIdentifier(variable);
    }

    @Override
    public void exitTemplateVariableDeclaration(TemplateVariableDeclarationContext arg0) {
      var variable = this.stack.pop(Variable.class);
      this.stack.declareIdentifier(variable);
    }

    @Override
    public void exitContextDeclaration(ContextDeclarationContext ctx) {
      final String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
      if (fieldId != null) {
        final ContextVariableInitializerContext initializer = ctx.contextVariableInitializer();
        if (initializer != null) {
          var t = FieldTypes.fromString(this.symbols.getTypeOfField(fieldId));
          this.stack.declareIdentifier(new Variable(initializer.variableName.getText(), PathExpression.instantiate("", t),
              PathExpression.instantiate("", t)));
        }
      }
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
  
    // #endregion Template Variables ------------------------------------------
    
    // #region Scope management --------------------------------------------

    Stack<Integer> levels = new Stack<>();

    @Override
    public void enterTemplateLine(TemplateLineContext ctx) {
      final int indentLevel = EfxTemplateTranslatorV2.this.getIndentLevel(ctx.indentation());
      this.enterTemplateLine(indentLevel);
    }

    @Override
    public void enterTemplateDeclaration(TemplateDeclarationContext ctx) {
      this.enterTemplateLine(0);
    }

    private void enterTemplateLine(final int indentLevel) {
      final int indentChange = indentLevel - (this.levels.isEmpty() ? 0 : this.levels.peek());
      if (indentChange > 1) {
        throw InvalidIndentationException.indentationLevelSkipped();
      } else if (indentChange == 1) {
        if (this.levels.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero();
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
    public void exitTemplateDeclaration(TemplateDeclarationContext ctx) {
      this.exitTemplateLine(0);
    }

    @Override
    public void exitTemplateLine(TemplateLineContext ctx) {
      final int indentLevel = EfxTemplateTranslatorV2.this.getIndentLevel(ctx.indentation());
      this.exitTemplateLine(indentLevel);
    }

    private void exitTemplateLine(final int indentLevel) {
      final int indentChange = indentLevel - (this.levels.isEmpty() ? 0 : this.levels.peek());
      assert this.stack.empty() : "Stack should be empty at this point.";

      if (indentChange > 1) {
        throw InvalidIndentationException.indentationLevelSkipped();
      } else if (indentChange == 1) {
        if (this.levels.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero();
        }
        this.levels.push(this.levels.peek() + 1);
      } else if (indentChange == 0 && this.levels.isEmpty()) {
          assert indentLevel == 0 : UNEXPECTED_INDENTATION;
          this.levels.push(0);
        }
    }

    // #endregion Scope management --------------------------------------------

    @Override
    public void exitDictionaryDeclaration(DictionaryDeclarationContext ctx) {
      var dictionaryName = ctx.dictionaryName.getText();
      var fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx.fieldContext());
      var field = this.symbols.getAbsolutePathOfField(fieldId);
      this.stack.declareGlobalIdentifier(new Dictionary(dictionaryName, field, StringExpression.empty()));
    }
  }

  // #endregion Pre-processing ------------------------------------------------
}
