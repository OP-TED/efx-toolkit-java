package eu.europa.ted.efx.sdk2;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.BooleanVariable;
import eu.europa.ted.efx.model.ContentBlock;
import eu.europa.ted.efx.model.ContentBlockStack;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.DateVariable;
import eu.europa.ted.efx.model.DurationVariable;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;
import eu.europa.ted.efx.model.Markup;
import eu.europa.ted.efx.model.NumericVariable;
import eu.europa.ted.efx.model.StringVariable;
import eu.europa.ted.efx.model.TimeVariable;
import eu.europa.ted.efx.model.Variable;
import eu.europa.ted.efx.model.VariableList;
import eu.europa.ted.efx.sdk2.EfxParser.*;
import eu.europa.ted.efx.xpath.XPathAttributeLocator;
import eu.europa.ted.efx.xpath.XPathContextualizer;

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

  private static final String INCONSISTENT_INDENTATION_SPACES =
      "Inconsistent indentation. Expected a multiple of %d spaces.";
  private static final String INDENTATION_LEVEL_SKIPPED = "Indentation level skipped.";
  private static final String START_INDENT_AT_ZERO =
      "Incorrect indentation. Please do not indent the first level in your template.";
  private static final String MIXED_INDENTATION =
      "Do not mix indentation methods. Stick with either tabs or spaces.";
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

    final TemplatePreprocessor preprocessor = this.new TemplatePreprocessor(charStream);
    final String preprocessedTemplate = preprocessor.processTemplate();

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

  // #region Template File ----------------------------------------------------

  @Override
  public void enterTemplateFile(TemplateFileContext ctx) {
    assert blockStack.isEmpty() : UNEXPECTED_INDENTATION;
  }

  @Override
  public void exitTemplateFile(TemplateFileContext ctx) {
    this.blockStack.pop();

    List<Markup> templateCalls = new ArrayList<>();
    List<Markup> templates = new ArrayList<>();
    for (ContentBlock rootBlock : this.rootBlock.getChildren()) {
      templateCalls.add(rootBlock.renderCallTemplate(markup));
      rootBlock.renderTemplate(markup, templates);
    }
    Markup file = this.markup.composeOutputFile(templateCalls, templates);
    this.stack.push(file);
  }

  // #endregion Template File -------------------------------------------------
  
  // #region Source template blocks -------------------------------------------

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    Markup template =
        ctx.templateFragment() != null ? this.stack.pop(Markup.class) : Markup.empty();
    String text = ctx.textBlock() != null ? ctx.textBlock().getText() : "";
    this.stack.push(this.markup.renderFreeText(text).join(template));
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


  // #endregion Source template blocks ----------------------------------------
  
  // #region Label Blocks #{...} ----------------------------------------------

  @Override
  public void exitStandardLabelReference(StandardLabelReferenceContext ctx) {
    StringExpression assetId = ctx.assetId() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    StringExpression assetType = ctx.assetType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
        List.of(assetType, this.script.getStringLiteralFromUnquotedString("|"), labelType,
            this.script.getStringLiteralFromUnquotedString("|"), assetId))));
  }

  @Override
  public void exitShorthandBtLabelReference(ShorthandBtLabelReferenceContext ctx) {
    StringExpression assetId = this.script.getStringLiteralFromUnquotedString(ctx.BtId().getText());
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
        List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_BT),
            this.script.getStringLiteralFromUnquotedString("|"), labelType,
            this.script.getStringLiteralFromUnquotedString("|"), assetId))));
  }

  @Override
  public void exitShorthandFieldLabelReference(ShorthandFieldLabelReferenceContext ctx) {
    final String fieldId = ctx.FieldId().getText();
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");

    if (labelType.script.equals("value")) {
      this.shorthandIndirectLabelReference(fieldId);
    } else {
      this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
          List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
              this.script.getStringLiteralFromUnquotedString("|"), labelType,
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(fieldId)))));
    }
  }

  @Override
  public void exitShorthandIndirectLabelReference(ShorthandIndirectLabelReferenceContext ctx) {
    this.shorthandIndirectLabelReference(ctx.FieldId().getText());
  }

  private void shorthandIndirectLabelReference(final String fieldId) {
    final Context currentContext = this.efxContext.peek();
    final String fieldType = this.symbols.getTypeOfField(fieldId);
    final XPathAttributeLocator parsedPath =
        XPathAttributeLocator.findAttribute(symbols.getAbsolutePathOfField(fieldId));
    final PathExpression valueReference = parsedPath.hasAttribute()
        ? this.script.composeFieldAttributeReference(
            symbols.getRelativePath(parsedPath.getPath(), currentContext.absolutePath()),
            parsedPath.getAttribute(), PathExpression.class)
        : this.script.composeFieldValueReference(
            symbols.getRelativePathOfField(fieldId, currentContext.absolutePath()),
            PathExpression.class);
    final StringExpression loopVariable =
        this.script.composeVariableReference("item", StringExpression.class);
    switch (fieldType) {
      case "indicator":
        this.stack.push(this.markup.renderLabelFromExpression(this.script.composeForExpression(
            this.script.composeIteratorList(
                List.of(
                    this.script.composeIteratorExpression(loopVariable.script, valueReference))),
            this.script.composeStringConcatenation(
                List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_INDICATOR),
                    this.script.getStringLiteralFromUnquotedString("|"),
                    this.script.getStringLiteralFromUnquotedString(LABEL_TYPE_WHEN),
                    this.script.getStringLiteralFromUnquotedString("-"),
                    loopVariable,
                    this.script.getStringLiteralFromUnquotedString("|"),
                    this.script.getStringLiteralFromUnquotedString(fieldId))),
            StringListExpression.class)));
        break;
      case "code":
      case "internal-code":
        this.stack.push(this.markup.renderLabelFromExpression(this.script.composeForExpression(
            this.script.composeIteratorList(
                List.of(
                    this.script.composeIteratorExpression(loopVariable.script, valueReference))),
            this.script.composeStringConcatenation(List.of(
                this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_CODE),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(LABEL_TYPE_NAME),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(
                    this.symbols.getRootCodelistOfField(fieldId)),
                this.script.getStringLiteralFromUnquotedString("."),
                loopVariable)),
            StringListExpression.class)));
        break;
      default:
        throw new ParseCancellationException(String.format(
            "Unexpected field type '%s'. Expected a field of either type 'code' or 'indicator'.",
            fieldType));
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
  public void exitShorthandLabelReferenceFromContext(
      ShorthandLabelReferenceFromContextContext ctx) {
    final String labelType = ctx.LabelType().getText();
    if (this.efxContext.isFieldContext()) {
      if (labelType.equals(SHORTHAND_CONTEXT_FIELD_LABEL_REFERENCE)) {
        this.shorthandIndirectLabelReference(this.efxContext.symbol());
      } else {
        this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
            List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(labelType),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(this.efxContext.symbol())))));
      }
    } else if (this.efxContext.isNodeContext()) {
      this.stack.push(this.markup.renderLabelFromKey(this.script.composeStringConcatenation(
          List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_NODE),
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(labelType),
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(this.efxContext.symbol())))));
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
      throw new ParseCancellationException(
          "The #value shorthand syntax can only be used in a field is declared as context.");
    }
    this.shorthandIndirectLabelReference(this.efxContext.symbol());
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

  // #endregion Label Blocks #{...} -------------------------------------------
  
  // #region Expression Blocks ${...} -----------------------------------------

  /**
   * Handles a standard expression block in a template line. Most of the work is done by the base
   * class EfxExpressionTranslator. After the expression is translated, the result is passed through
   * the renderer.
   */
  @Override
  public void exitStandardExpressionBlock(StandardExpressionBlockContext ctx) {
    this.stack.push(this.stack.pop(Expression.class));
  }

  /***
   * Handles the $value shorthand syntax which renders the value of the field declared as context in
   * the current line of the template.
   */
  @Override
  public void exitShorthandFieldValueReferenceFromContextField(
      ShorthandFieldValueReferenceFromContextFieldContext ctx) {
    if (!this.efxContext.isFieldContext()) {
      throw new ParseCancellationException(
          "The $value shorthand syntax can only be used when a field is declared as the context.");
    }
    this.stack.push(this.script.composeFieldValueReference(
        symbols.getRelativePathOfField(this.efxContext.symbol(), this.efxContext.absolutePath()),
        Expression.class));
  }

  // #endregion Expression Blocks ${...} --------------------------------------
  
  // #region Context Declaration Blocks {...} ---------------------------------

  /**
   * This method changes the current EFX context.
   * 
   * The EFX context is always assumed to be either a Field or a Node. Any predicate included in the
   * EFX context declaration is not relevant and is ignored.
   */
  @Override
  public void exitContextDeclarationBlock(ContextDeclarationBlockContext ctx) {
    if (ctx.templateVariableList() == null) {
      this.stack.push(new VariableList());
    }
  }

  @Override
  public void exitContextDeclaration(ContextDeclarationContext ctx) {
    final PathExpression contextPath = this.stack.pop(PathExpression.class);
    Variable<PathExpression> contextVariable = this.getContextVariable(ctx, contextPath);
    final String filedId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
    if (filedId != null) {
      this.efxContext.push(new FieldContext(filedId, contextPath, contextVariable));
      if (contextVariable != null) {
        this.stack.declareTemplateVariable(contextVariable.name,
            Expression.types.get(this.symbols.getTypeOfField(filedId)));
      }
    } else {
      final String nodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx);
      assert nodeId != null : "We should have been able to locate the FieldId or NodeId declared as context.";
      this.efxContext.push(new NodeContext(nodeId, contextPath));
    }
  }

  private Variable<PathExpression> getContextVariable(ContextDeclarationContext ctx,
      PathExpression contextPath) {
    if (ctx.contextVariableInitializer() == null) {
      return null;
    }
    final String variableName = getVariableName(ctx.contextVariableInitializer());
    return new Variable<>(variableName, XPathContextualizer.contextualize(contextPath, contextPath),
        this.script.composeVariableReference(variableName, PathExpression.class));
  }

  @Override
  public void enterTemplateVariableList(TemplateVariableListContext ctx) {
    this.stack.push(new VariableList());
  }

  @Override
  public void exitStringVariableInitializer(StringVariableInitializerContext ctx) {
    this.exitVariableInitializer(getVariableName(ctx), StringVariable.class,
        StringExpression.class);
  }

  @Override
  public void exitBooleanVariableInitializer(BooleanVariableInitializerContext ctx) {
    this.exitVariableInitializer(getVariableName(ctx), BooleanVariable.class,
        BooleanExpression.class);
  }

  @Override
  public void exitNumericVariableInitializer(NumericVariableInitializerContext ctx) {
    this.exitVariableInitializer(getVariableName(ctx), NumericVariable.class,
        NumericExpression.class);
  }

  @Override
  public void exitDateVariableInitializer(DateVariableInitializerContext ctx) {
    this.exitVariableInitializer(getVariableName(ctx), DateVariable.class,
        DateExpression.class);
  }

  @Override
  public void exitTimeVariableInitializer(TimeVariableInitializerContext ctx) {
    this.exitVariableInitializer(getVariableName(ctx), TimeVariable.class,
        TimeExpression.class);
  }

  @Override
  public void exitDurationVariableInitializer(DurationVariableInitializerContext ctx) {
    this.exitVariableInitializer(getVariableName(ctx), DurationVariable.class,
        DurationExpression.class);
  }

  private <T extends Expression, V extends Variable<T>> void exitVariableInitializer(
      String variableName, Class<V> variableType, Class<T> expressionType) {
    T expression = this.stack.pop(expressionType);
    VariableList variables = this.stack.pop(VariableList.class);
    try {
      Constructor<V> constructor =
          variableType.getConstructor(String.class, expressionType, expressionType);
      variables.push(constructor.newInstance(variableName, expression,
          this.script.composeVariableReference(variableName, expressionType)));
      this.stack.push(variables);
      this.stack.declareTemplateVariable(variableName, expressionType);
    } catch (Exception e) {
      throw new ParseCancellationException(e);
    }
  }

  // #endregion Context Declaration Blocks {...} ------------------------------
  
  // #region Template lines  --------------------------------------------------

  @Override
  public void enterTemplateLine(TemplateLineContext ctx) {
    final int indentLevel = this.getIndentLevel(ctx);
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    if (indentChange > 1) {
      throw new ParseCancellationException(INDENTATION_LEVEL_SKIPPED);
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
        throw new ParseCancellationException(START_INDENT_AT_ZERO);
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
    final int indentLevel = this.getIndentLevel(ctx);
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    final Markup content = ctx.template() != null ? this.stack.pop(Markup.class) : new Markup("");
    final VariableList templateVariables = this.stack.pop(VariableList.class);
    final Integer outlineNumber =
        ctx.OutlineNumber() != null ? Integer.parseInt(ctx.OutlineNumber().getText().trim()) : -1;
    assert this.stack.empty() : "Stack should be empty at this point.";

    if (indentChange > 1) {
      throw new ParseCancellationException(INDENTATION_LEVEL_SKIPPED);
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
        throw new ParseCancellationException(START_INDENT_AT_ZERO);
      }
      this.blockStack.pushChild(outlineNumber, content,
          this.relativizeContext(lineContext, this.blockStack.currentContext()), templateVariables);
    } else if (indentChange < 0) {
      this.blockStack.pushSibling(outlineNumber, content,
          this.relativizeContext(lineContext, this.blockStack.parentContext()), templateVariables);
    } else if (indentChange == 0) {

      if (blockStack.isEmpty()) {
        assert indentLevel == 0 : UNEXPECTED_INDENTATION;
        this.blockStack.push(this.rootBlock.addChild(outlineNumber, content,
            this.relativizeContext(lineContext, this.rootBlock.getContext()), templateVariables));
      } else {
        this.blockStack.pushSibling(outlineNumber, content,
            this.relativizeContext(lineContext, this.blockStack.parentContext()),
            templateVariables);
      }
    }
  }

  private Context relativizeContext(Context childContext, Context parentContext) {
    if (parentContext == null) {
      return childContext;
    }

    if (FieldContext.class.isAssignableFrom(childContext.getClass())) {
      return new FieldContext(childContext.symbol(), childContext.absolutePath(),
          this.symbols.getRelativePath(childContext.absolutePath(), parentContext.absolutePath()),
          childContext.variable());
    }

    assert NodeContext.class.isAssignableFrom(
        childContext.getClass()) : "Child context should be either a FieldContext NodeContext.";

    return new NodeContext(childContext.symbol(), childContext.absolutePath(),
        this.symbols.getRelativePath(childContext.absolutePath(), parentContext.absolutePath()));
  }

  // #endregion Template lines  -----------------------------------------------
  
  // #region Helpers ----------------------------------------------------------

  private int getIndentLevel(TemplateLineContext ctx) {
    if (ctx.MixedIndent() != null) {
      throw new ParseCancellationException(MIXED_INDENTATION);
    }

    if (ctx.Spaces() != null) {
      if (this.indentWith == Indent.UNDETERMINED) {
        this.indentWith = Indent.SPACES;
        this.indentSpaces = ctx.Spaces().getText().length();
      } else if (this.indentWith == Indent.TABS) {
        throw new ParseCancellationException(MIXED_INDENTATION);
      }

      if (ctx.Spaces().getText().length() % this.indentSpaces != 0) {
        throw new ParseCancellationException(
            String.format(INCONSISTENT_INDENTATION_SPACES, this.indentSpaces));
      }
      return ctx.Spaces().getText().length() / this.indentSpaces;
    } else if (ctx.Tabs() != null) {
      if (this.indentWith == Indent.UNDETERMINED) {
        this.indentWith = Indent.TABS;
      } else if (this.indentWith == Indent.SPACES) {
        throw new ParseCancellationException(MIXED_INDENTATION);
      }

      return ctx.Tabs().getText().length();
    }
    return 0;
  }

  static private String getVariableName(StringVariableInitializerContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(NumericVariableInitializerContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(BooleanVariableInitializerContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(DateVariableInitializerContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(TimeVariableInitializerContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(DurationVariableInitializerContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(ContextVariableInitializerContext ctx) {
    return getVariableName(ctx.Variable().getText());
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
    public void exitContextDeclaration(ContextDeclarationContext ctx) {
      final String filedId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
      if (filedId != null) {
        final ContextVariableInitializerContext initializer = ctx.contextVariableInitializer();
        if (initializer != null) {
          this.stack.declareTemplateVariable(getVariableName(initializer),
              Expression.types.get(this.symbols.getTypeOfField(filedId)));
        }
      }
    }

    @Override
    public void exitStringVariableInitializer(StringVariableInitializerContext ctx) {
      this.stack.declareTemplateVariable(getVariableName(ctx), StringExpression.class);
    }
  
    @Override
    public void exitBooleanVariableInitializer(BooleanVariableInitializerContext ctx) {
      this.stack.declareTemplateVariable(getVariableName(ctx), BooleanExpression.class);
    }
  
    @Override
    public void exitNumericVariableInitializer(NumericVariableInitializerContext ctx) {
      this.stack.declareTemplateVariable(getVariableName(ctx), NumericExpression.class);
    }
  
    @Override
    public void exitDateVariableInitializer(DateVariableInitializerContext ctx) {
      this.stack.declareTemplateVariable(getVariableName(ctx), DateExpression.class);
    }
  
    @Override
    public void exitTimeVariableInitializer(TimeVariableInitializerContext ctx) {
      this.stack.declareTemplateVariable(getVariableName(ctx), TimeExpression.class);
    }
  
    @Override
    public void exitDurationVariableInitializer(DurationVariableInitializerContext ctx) {
      this.stack.declareTemplateVariable(getVariableName(ctx), DurationExpression.class);
    }
  
    // #endregion Template Variables ------------------------------------------
    
    // #region Scope management --------------------------------------------

    Stack<Integer> levels = new Stack<Integer>();

    @Override
    public void enterTemplateLine(TemplateLineContext ctx) {
      final int indentLevel = EfxTemplateTranslatorV2.this.getIndentLevel(ctx);
      final int indentChange = indentLevel - (this.levels.isEmpty() ? 0 : this.levels.peek());
      if (indentChange > 1) {
        throw new ParseCancellationException(INDENTATION_LEVEL_SKIPPED);
      } else if (indentChange == 1) {
        if (this.levels.isEmpty()) {
          throw new ParseCancellationException(START_INDENT_AT_ZERO);
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
      final int indentLevel = EfxTemplateTranslatorV2.this.getIndentLevel(ctx);
      final int indentChange = indentLevel - (this.levels.isEmpty() ? 0 : this.levels.peek());
      assert this.stack.empty() : "Stack should be empty at this point.";

      if (indentChange > 1) {
        throw new ParseCancellationException(INDENTATION_LEVEL_SKIPPED);
      } else if (indentChange == 1) {
        if (this.levels.isEmpty()) {
          throw new ParseCancellationException(START_INDENT_AT_ZERO);
        }
        this.levels.push(this.levels.peek() + 1);
      } else if (indentChange == 0) {

        if (this.levels.isEmpty()) {
          assert indentLevel == 0 : UNEXPECTED_INDENTATION;
          this.levels.push(0);
        }
      }
    }

    // #endregion Scope management --------------------------------------------

  }

  // #endregion Pre-processing ------------------------------------------------
}
