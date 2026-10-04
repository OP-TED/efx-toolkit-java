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
package eu.europa.ted.efx.sdk1;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.InvalidIndentationException;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TemplateSection;
import eu.europa.ted.efx.interfaces.TranslatorContext;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.templates.ContentBlockStack;
import eu.europa.ted.efx.model.templates.Markup;
import eu.europa.ted.efx.model.templates.ContentTemplate;
import eu.europa.ted.efx.model.templates.ContentTemplateFragment;
import eu.europa.ted.efx.model.templates.DisplayContentTemplate;
import eu.europa.ted.efx.model.templates.ExpressionContentTemplateFragment;
import eu.europa.ted.efx.model.templates.LabelFromExpressionContentTemplateFragment;
import eu.europa.ted.efx.model.templates.LabelFromKeyContentTemplateFragment;
import eu.europa.ted.efx.model.templates.TextContentTemplateFragment;
import eu.europa.ted.efx.model.templates.ViewTemplateSection;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.variables.Variable;
import eu.europa.ted.efx.model.variables.Variables;
import eu.europa.ted.efx.sdk1.EfxParser.*;

/**
 * The EfxTemplateTranslator extends the {@link EfxExpressionTranslatorV1} to provide additional
 * translation capabilities for EFX templates. If has been implemented as an extension to the
 * EfxExpressionTranslator in order to keep things simpler when one only needs to translate EFX
 * expressions (like the condition associated with a business rule).
 */
@SdkComponent(versions = {"1"}, componentType = SdkComponentType.EFX_TEMPLATE_TRANSLATOR)
public class EfxTemplateTranslatorV1 extends EfxExpressionTranslatorV1
    implements EfxTemplateTranslator {

  private static final Logger logger = LoggerFactory.getLogger(EfxTemplateTranslatorV1.class);

  private static final String UNEXPECTED_INDENTATION = "Unexpected indentation tracker state.";

  private static final String LABEL_TYPE_NAME = getLexerSymbol(EfxLexer.LABEL_TYPE_NAME);
  private static final String LABEL_TYPE_WHEN = getLexerSymbol(EfxLexer.LABEL_TYPE_WHEN_TRUE).replace("-true", "");
  private static final String SHORTHAND_CONTEXT_FIELD_LABEL_REFERENCE = getLexerSymbol(EfxLexer.ValueKeyword);
  private static final String ASSET_TYPE_INDICATOR = getLexerSymbol(EfxLexer.ASSET_TYPE_INDICATOR);
  private static final String ASSET_TYPE_BT = getLexerSymbol(EfxLexer.ASSET_TYPE_BT);
  private static final String ASSET_TYPE_FIELD = getLexerSymbol(EfxLexer.ASSET_TYPE_FIELD);
  private static final String ASSET_TYPE_NODE = getLexerSymbol(EfxLexer.ASSET_TYPE_NODE);
  private static final String ASSET_TYPE_CODE = getLexerSymbol(EfxLexer.ASSET_TYPE_CODE);

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

  // EFX 1 templates have a single section. Its top-level template lines are added to its root block.
  ViewTemplateSection section;

  /**
   * The block stack is used to keep track of the indentation of template lines and adjust the EFX
   * context accordingly. A block is a template line together with the template lines nested
   * (through indentation) under it. At the top of the blockStack is the template block that is
   * currently being processed. The next block in the stack is its parent block, and so on.
   */
  ContentBlockStack blockStack = new ContentBlockStack();

  @SuppressWarnings("unused")
  private EfxTemplateTranslatorV1() {
    super();
  }

  public EfxTemplateTranslatorV1(final MarkupGenerator markupGenerator,
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
    return renderTemplate(CharStreams.fromPath(pathname), options);
  }

  /**
   * Translates the template contained in the string passed as a parameter.
   */
  @Override
  public String renderTemplate(final String template, TranslatorOptions options) {
    return renderTemplate(CharStreams.fromString(template), options);
  }

  @Override
  public String renderTemplate(final InputStream stream, TranslatorOptions options) throws IOException {
    return renderTemplate(CharStreams.fromStream(stream), options);
  }

  private String renderTemplate(final CharStream charStream, TranslatorOptions options) {
    logger.debug("Rendering template");

    if (options != null && options.isProfilerEnabled()) {
      logger.warn("EFX profiling is not available for EFX-1 templates. No profiler output will be generated.");
    }

    final EfxLexer lexer = new EfxLexer(charStream);
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
    this.section = new ViewTemplateSection(TemplateSection.DEFAULT, "block");
    this.indentWith = Indent.UNDETERMINED;
    this.indentSpaces = -1;
  }

  @Override
  public void exitTemplateFile(TemplateFileContext ctx) {
    // The end of the file closes the levels still open, as a line back at level 0 would: the block of
    // each nested level and its stack frame.
    while (this.blockStack.currentIndentationLevel() > 0) {
      this.blockStack.pop();
      this.stack.popStackFrame();
    }
    this.blockStack.pop();

    List<Markup> templates = new ArrayList<>();
    List<Markup> templateCalls = this.section.render(this.markup, new TranslatorContext(), templates);
    Markup file = this.markup.composeOutputFile(templateCalls, templates);
    this.stack.push(file);
  }

  // #endregion Template File -------------------------------------------------
  
  // #region Source template blocks -------------------------------------------

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    DisplayContentTemplate template =
        ctx.templateFragment() != null ? this.stack.pop(DisplayContentTemplate.class) : new DisplayContentTemplate();
    String text = ctx.textBlock() != null ? ctx.textBlock().getText() : "";
    template.prepend(new TextContentTemplateFragment(text));
    this.stack.push(template);
  }

  @Override
  public void exitLabelTemplate(LabelTemplateContext ctx) {
    DisplayContentTemplate template =
        ctx.templateFragment() != null ? this.stack.pop(DisplayContentTemplate.class) : new DisplayContentTemplate();
    if (ctx.labelBlock() != null) {
      template.prepend(this.stack.pop(ContentTemplateFragment.class));
    }
    this.stack.push(template);
  }

  @Override
  public void exitExpressionTemplate(ExpressionTemplateContext ctx) {
    DisplayContentTemplate template =
        ctx.templateFragment() != null ? this.stack.pop(DisplayContentTemplate.class) : new DisplayContentTemplate();
    Expression expression = this.stack.pop(Expression.class);
    template.prepend(new ExpressionContentTemplateFragment(expression));
    this.stack.push(template);
  }


  // #endregion Source template blocks ----------------------------------------
  
  // #region Label Blocks #{...} ----------------------------------------------

  @Override
  public void exitStandardLabelReference(StandardLabelReferenceContext ctx) {
    if (!this.stack.empty() && StringSequenceExpression.class.isAssignableFrom(this.stack.peek().getClass()) && ctx.assetId() != null) {

      // This is a workaround that allows EFX 1 to render a sequence of labels without a special
      // syntax. When a standard label reference is processed, the template translator checks if
      // the assetId is provided with a SequenceExpression. If this is the case, then the
      // translator generates the appropriate code to render a sequence of labels.
      //
      // For example, this will render a sequence of labels for a label reference of the form
      // #{assetType|labelType|${for text:$t in ('assetId1','assetId2') return $t}}:} 
      // The only restriction is that the assetType and labelType must be the same for all labels in the sequence.

      StringSequenceExpression assetIdSequence = this.stack.pop(StringSequenceExpression.class);
      this.exitStandardLabelReference(ctx, assetIdSequence);
    } else {

      // Standard implementation as originally intended by EFX 1

      StringExpression assetId = ctx.assetId() != null ? this.stack.pop(StringExpression.class)
          : this.script.getStringLiteralFromUnquotedString("");
      this.exitStandardLabelReference(ctx, assetId);
    }
  }

  /**
   * Renders a single label from a standard label reference.
   * 
   * @param ctx     The ParserRuleContext of the standard label reference.
   * @param assetId The assetId of the label to render.
   */
  private void exitStandardLabelReference(StandardLabelReferenceContext ctx, StringExpression assetId) {
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    StringExpression assetType = ctx.assetType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");

    StringExpression key = this.script.composeStringConcatenation(
        List.of(assetType, this.script.getStringLiteralFromUnquotedString("|"), labelType,
            this.script.getStringLiteralFromUnquotedString("|"), assetId));
    this.stack.push(new LabelFromKeyContentTemplateFragment(key, NumericExpression.empty()));
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

    StringSequenceExpression keys = this.script.composeDistinctValuesFunction(
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
        StringSequenceExpression.class);
    this.stack.push(new LabelFromExpressionContentTemplateFragment(keys, NumericExpression.empty()));
  }


  @Override
  public void exitShorthandBtLabelReference(ShorthandBtLabelReferenceContext ctx) {
    StringExpression assetId = this.script.getStringLiteralFromUnquotedString(ctx.BtId().getText());
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");
    StringExpression key = this.script.composeStringConcatenation(
        List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_BT),
            this.script.getStringLiteralFromUnquotedString("|"), labelType,
            this.script.getStringLiteralFromUnquotedString("|"), assetId));
    this.stack.push(new LabelFromKeyContentTemplateFragment(key, NumericExpression.empty()));
  }

  @Override
  public void exitShorthandFieldLabelReference(ShorthandFieldLabelReferenceContext ctx) {
    final String fieldId = ctx.FieldId().getText();
    StringExpression labelType = ctx.labelType() != null ? this.stack.pop(StringExpression.class)
        : this.script.getStringLiteralFromUnquotedString("");

    if (labelType.getScript().equals("value")) {
      this.shorthandIndirectLabelReference(fieldId);
    } else {
      StringExpression key = this.script.composeStringConcatenation(
          List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
              this.script.getStringLiteralFromUnquotedString("|"), labelType,
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(fieldId)));
      this.stack.push(new LabelFromKeyContentTemplateFragment(key, NumericExpression.empty()));
    }
  }

  @Override
  public void exitShorthandIndirectLabelReference(ShorthandIndirectLabelReferenceContext ctx) {
    this.shorthandIndirectLabelReference(ctx.FieldId().getText());
  }

  private void shorthandIndirectLabelReference(final String fieldId) {
    final Context currentContext = this.efxContext.peek();
    final String fieldType = this.symbols.getTypeOfField(fieldId);
    final PathExpression valueReference = this.symbols.isAttributeField(fieldId)
        ? this.script.composeFieldAttributeReference(
            this.script.contextualizePath(this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId), currentContext.absolutePath()),
            this.symbols.getAttributeNameFromAttributeField(fieldId), StringPath.class)
        : this.composeFieldValueReference(
        this.symbols.getRelativePathOfField(fieldId, currentContext.symbol()));
    Variable loopVariable = new Variable("item",
        this.script.composeVariableDeclaration("item", StringExpression.class), StringExpression.empty(),
        this.script.composeVariableReference("item", StringExpression.class));
    switch (fieldType) {
      case "indicator": {
        StringSequenceExpression keys = this.script.composeDistinctValuesFunction(
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
            StringSequenceExpression.class);
        this.stack.push(new LabelFromExpressionContentTemplateFragment(keys, NumericExpression.empty()));
        break;
      }
      case "code":
      case "internal-code": {
        StringSequenceExpression keys = this.script.composeDistinctValuesFunction(
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
            StringSequenceExpression.class);
        this.stack.push(new LabelFromExpressionContentTemplateFragment(keys, NumericExpression.empty()));
        break;
      }
      default:
        throw InvalidUsageException.shorthandRequiresCodeOrIndicator(this.stack.peekParserContext(), fieldId, fieldType);
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
    final String labelType = ctx.LabelType().getText();
    if (this.efxContext.isFieldContext()) {
      if (labelType.equals(SHORTHAND_CONTEXT_FIELD_LABEL_REFERENCE)) {
        this.shorthandIndirectLabelReference(this.efxContext.symbol());
      } else {
        StringExpression key = this.script.composeStringConcatenation(
            List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_FIELD),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(labelType),
                this.script.getStringLiteralFromUnquotedString("|"),
                this.script.getStringLiteralFromUnquotedString(this.efxContext.symbol())));
        this.stack.push(new LabelFromKeyContentTemplateFragment(key, NumericExpression.empty()));
      }
    } else if (this.efxContext.isNodeContext()) {
      StringExpression key = this.script.composeStringConcatenation(
          List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_NODE),
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(labelType),
              this.script.getStringLiteralFromUnquotedString("|"),
              this.script.getStringLiteralFromUnquotedString(this.efxContext.symbol())));
      this.stack.push(new LabelFromKeyContentTemplateFragment(key, NumericExpression.empty()));
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
    var expression = this.stack.pop(Expression.class);

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
      throw InvalidUsageException.shorthandRequiresFieldContext(ctx, "$value");
    }
    this.stack.push(this.composeFieldValueReference(
        this.symbols.getRelativePathOfField(this.efxContext.symbol(), this.efxContext.symbol())));
  }

  // #endregion Expression Blocks ${...} --------------------------------------

  // #region Value References -------------------------------------------------

  /***
   * Multilingual fields are handled by this class, so the value reference is composed here instead
   * of directly by the script generator. Anything else is left to the inherited behaviour.
   *
   * @see #composeFieldValueReference(PathExpression)
   */
  @Override
  public void exitScalarFromFieldReference(final ScalarFromFieldReferenceContext ctx) {
    if (!this.stack.peekType().is(EfxDataType.MultilingualString.class)) {
      super.exitScalarFromFieldReference(ctx);
      return;
    }
    this.stack.push(this.composeFieldValueReference(this.stack.pop(PathExpression.class)));
  }

  /***
   * @see #exitScalarFromFieldReference(ScalarFromFieldReferenceContext)
   */
  @Override
  public void exitSequenceFromFieldReference(final SequenceFromFieldReferenceContext ctx) {
    if (!this.stack.peekType().is(EfxDataType.MultilingualString.class)) {
      super.exitSequenceFromFieldReference(ctx);
      return;
    }
    this.stack.push(this.composeFieldValueReference(this.stack.pop(PathExpression.class)));
  }

  /***
   * In a view template the value of a multilingual field must be rendered in the language preferred
   * by the reader, which EFX-1 gives the template author no syntax to ask for. Template translation
   * therefore selects the preferred language implicitly, for every multilingual field it renders.
   *
   * Outside of view templates no such selection is possible: the function that performs it is
   * provided by the XSL of the notice viewer and exists nowhere else. There the value of a
   * multilingual field is retrieved like that of any other text field.
   */
  private PathExpression composeFieldValueReference(final PathExpression fieldReference) {
    if (fieldReference.is(EfxDataType.MultilingualString.class)) {
      return Expression.from(this.script.getTextInPreferredLanguage(fieldReference),
          fieldReference.getClass());
    }
    return this.script.composeFieldValueReference(fieldReference);
  }

  // #endregion Value References ----------------------------------------------

  // #region Context Declaration Blocks {...} ---------------------------------

  /**
   * This method changes the current EFX context.
   * 
   * The EFX context is always assumed to be either a Field or a Node. Any predicate included in the
   * EFX context declaration is not relevant and is ignored.
   */
  @Override
  public void exitContextDeclarationBlock(ContextDeclarationBlockContext ctx) {
    PathExpression contextPath = this.stack.pop(PathExpression.class);
    if (ctx.fieldContext() != null) {
      String fieldId = getFieldId(ctx.fieldContext());
      assert fieldId != null : "We should have been able to locate the FieldId declared as context.";
      this.efxContext.push(new FieldContext(fieldId, contextPath));
    } else if (ctx.nodeContext() != null) {
      String nodeId = getNodeId(ctx.nodeContext());
      assert nodeId != null : "We should have been able to locate the NodeId declared as context.";
      this.efxContext.push(new NodeContext(nodeId, contextPath));
    }
  }


  // #endregion Context Declaration Blocks {...} ------------------------------
  
  // #region Template lines  --------------------------------------------------

  @Override
  public void enterTemplateLine(TemplateLineContext ctx) {
    final int indentLevel = this.getIndentLevel(ctx);
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
    final int indentLevel = this.getIndentLevel(ctx);
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    final List<ContentTemplate> contentTemplates =
        ctx.template() != null ? List.of(this.stack.pop(ContentTemplate.class)) : List.of();
    final Variables variables = new Variables(); // template variables not supported in EFX-1
    final Integer outlineNumber =
        ctx.OutlineNumber() != null ? Integer.parseInt(ctx.OutlineNumber().getText().trim()) : -1;
    assert this.stack.empty() : "Stack should be empty at this point.";
    this.stack.clear(); // Variable scope boundary. Clear declared variables

    if (indentChange > 1) {
      throw InvalidIndentationException.indentationLevelSkipped(ctx);
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
          throw InvalidIndentationException.startIndentAtZero(ctx);
      }
      this.blockStack.pushChild(outlineNumber, this.relativizeContext(lineContext, this.blockStack.currentContext()), variables,
          contentTemplates);
    } else if (indentChange < 0) {
      this.blockStack.pushSibling(outlineNumber, this.relativizeContext(lineContext, this.blockStack.parentContext()), variables,
          contentTemplates);
    } else if (indentChange == 0) {

      if (blockStack.isEmpty()) {
        assert indentLevel == 0 : UNEXPECTED_INDENTATION;
        this.blockStack.push(this.section.getRoot().addChild(outlineNumber, this.relativizeContext(lineContext, this.section.getRoot().getContext()), variables,
            contentTemplates));
      } else {
        this.blockStack.pushSibling(outlineNumber, this.relativizeContext(lineContext, this.blockStack.parentContext()), variables,
            contentTemplates);
      }
    }
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

    assert childContext.isNodeContext() : "Child context should be either a FieldContext NodeContext.";

    return new NodeContext(childContext.symbol(), childContext.absolutePath(),
        this.script.contextualizePath(childContext.absolutePath(), parentContextAbsolutePath));
  }

  // #endregion Template lines  -----------------------------------------------
  
  // #region Helpers ----------------------------------------------------------

  private int getIndentLevel(TemplateLineContext ctx) {
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

}
