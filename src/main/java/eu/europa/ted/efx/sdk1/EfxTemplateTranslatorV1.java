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
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;
import eu.europa.ted.efx.interfaces.EfxTemplateTranslator;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.ContentBlock;
import eu.europa.ted.efx.model.ContentBlockStack;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Markup;
import eu.europa.ted.efx.sdk1.EfxParser.*;
import eu.europa.ted.efx.xpath.XPathAttributeLocator;

/**
 * The EfxTemplateTranslator extends the {@link EfxExpressionTranslatorV1} to provide additional
 * translation capabilities for EFX templates. If has been implemented as an extension to the
 * EfxExpressionTranslator in order to keep things simpler when one only needs to translate EFX
 * expressions (like the condition associated with a business rule).
 */
@VersionDependentComponent(versions = {"1"}, componentType = VersionDependentComponentType.EFX_TEMPLATE_TRANSLATOR)
public class EfxTemplateTranslatorV1 extends EfxExpressionTranslatorV1
    implements EfxTemplateTranslator {

  private static final Logger logger = LoggerFactory.getLogger(EfxTemplateTranslatorV1.class);

  private static final String INCONSISTENT_INDENTATION_SPACES =
      "Inconsistent indentation. Expected a multiple of %d spaces.";
  private static final String INDENTATION_LEVEL_SKIPPED = "Indentation level skipped.";
  private static final String START_INDENT_AT_ZERO =
      "Incorrect indentation. Please do not indent the first level in your template.";
  private static final String MIXED_INDENTATION =
      "Do not mix indentation methods. Stick with either tabs or spaces.";
  private static final String UNEXPECTED_INDENTATION = "Unexpected indentation tracker state.";

  private static final String LABEL_TYPE_NAME =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.LABEL_TYPE_NAME).replaceAll("^'|'$", "");
  private static final String LABEL_TYPE_WHEN = EfxLexer.VOCABULARY
      .getLiteralName(EfxLexer.LABEL_TYPE_WHEN_TRUE).replaceAll("^'|'$", "").replace("-true", "");
  private static final String SHORTHAND_CONTEXT_FIELD_LABEL_REFERENCE =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ValueKeyword).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_INDICATOR =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_INDICATOR).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_BT =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_BT).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_FIELD =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_FIELD).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_NODE =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_NODE).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_CODE =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_CODE).replaceAll("^'|'$", "");


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

  /*** Template File ***/

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

  /*** Source template blocks ***/

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


  /*** Label Blocks #{...} ***/

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
    final PathExpression path =
        symbols.getRelativePathOfField(fieldId, currentContext.absolutePath());
    final XPathAttributeLocator parsedPath = XPathAttributeLocator.findAttribute(path);
    final StringExpression valueReference = parsedPath.hasAttribute()
        ? this.script.composeFieldAttributeReference(parsedPath.getPath(),
            parsedPath.getAttribute(), StringExpression.class)
        : this.script.composeFieldValueReference(path, StringExpression.class);

    switch (fieldType) {
      case "indicator":
        this.stack
            .push(this.markup.renderLabelFromExpression(this.script.composeStringConcatenation(
                List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_INDICATOR),
                    this.script.getStringLiteralFromUnquotedString("|"),
                    this.script.getStringLiteralFromUnquotedString(LABEL_TYPE_WHEN),
                    this.script.getStringLiteralFromUnquotedString("-"), valueReference,
                    this.script.getStringLiteralFromUnquotedString("|"),
                    this.script.getStringLiteralFromUnquotedString(fieldId)))));
        break;
      case "code":
      case "internal-code":
        this.stack
            .push(this.markup.renderLabelFromExpression(this.script.composeStringConcatenation(
                List.of(this.script.getStringLiteralFromUnquotedString(ASSET_TYPE_CODE),
                    this.script.getStringLiteralFromUnquotedString("|"),
                    this.script.getStringLiteralFromUnquotedString(LABEL_TYPE_NAME),
                    this.script.getStringLiteralFromUnquotedString("|"),
                    this.script.getStringLiteralFromUnquotedString(
                        this.symbols.getRootCodelistOfField(fieldId)),
                    this.script.getStringLiteralFromUnquotedString("."), valueReference))));
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
  public void exitShorthandLabelReferenceFromContext(ShorthandLabelReferenceFromContextContext ctx) {
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

  /*** Expression Blocks ${...} ***/

  /**
   * Handles a standard expression block in a template line. Most of the work is done by the base
   * class EfxExpressionTranslator. After the expression is translated, the result is passed
   * through the renderer.
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

  /*** Context Declaration Blocks {...} ***/

  /**
   * This method changes the current EFX context.
   * 
   * The EFX context is always assumed to be either a Field or a Node. Any predicate included in the
   * EFX context declaration is not relevant and is ignored.
   */
  @Override
  public void exitContextDeclarationBlock(ContextDeclarationBlockContext ctx) {

    final String filedId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
    if (filedId != null) {
      this.efxContext.push(new FieldContext(filedId, this.stack.pop(PathExpression.class)));
    } else {
      final String nodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx);
      assert nodeId != null : "We should have been able to locate the FieldId or NodeId declared as context.";
      this.efxContext.push(new NodeContext(nodeId, this.stack.pop(PathExpression.class)));
    }

    // final PathExpression absolutePath = this.stack.pop(PathExpression.class);
    // final String filedId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
    // if (filedId != null) {
    //   this.efxContext.push(new FieldContext(filedId, absolutePath, this.symbols.getRelativePath(absolutePath, this.blockStack.absolutePath())));
    // } else {
    //   final String nodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx);
    //   assert nodeId != null : "We should have been able to locate the FieldId or NodeId declared as context.";
    //   this.efxContext.push(new NodeContext(nodeId, absolutePath, this.symbols.getRelativePath(absolutePath, this.blockStack.absolutePath())));
    // }
  }


  /*** Template lines ***/

  @Override
  public void exitTemplateLine(TemplateLineContext ctx) {
    final Context lineContext = this.efxContext.pop();
    final int indentLevel = this.getIndentLevel(ctx);
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    final Markup content = ctx.template() != null ? this.stack.pop(Markup.class) : new Markup("");
    final Integer outlineNumber =
        ctx.OutlineNumber() != null ? Integer.parseInt(ctx.OutlineNumber().getText().trim()) : -1;
    assert this.stack.isEmpty() : "Stack should be empty at this point.";

    if (indentChange > 1) {
      throw new ParseCancellationException(INDENTATION_LEVEL_SKIPPED);
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
        throw new ParseCancellationException(START_INDENT_AT_ZERO);
      }
      this.blockStack.pushChild(outlineNumber, content, this.relativizeContext(lineContext, this.blockStack.currentContext()));
    } else if (indentChange < 0) {
      // lower indent level
      for (int i = indentChange; i < 0; i++) {
        assert !this.blockStack.isEmpty() : UNEXPECTED_INDENTATION;
        assert this.blockStack.currentIndentationLevel() > indentLevel : UNEXPECTED_INDENTATION;
        this.blockStack.pop();
      }
      assert this.blockStack.currentIndentationLevel() == indentLevel : UNEXPECTED_INDENTATION;
      this.blockStack.pushSibling(outlineNumber, content, this.relativizeContext(lineContext, this.blockStack.parentContext()));
    } else if (indentChange == 0) {

      if (blockStack.isEmpty()) {
        assert indentLevel == 0 : UNEXPECTED_INDENTATION;
        this.blockStack.push(this.rootBlock.addChild(outlineNumber, content, this.relativizeContext(lineContext, this.rootBlock.getContext())));
      } else {
        this.blockStack.pushSibling(outlineNumber, content, this.relativizeContext(lineContext, this.blockStack.parentContext()));
      }
    }
  }

  private Context relativizeContext(Context childContext, Context parentContext) {
    if (parentContext == null) {
      return childContext;
    }

    if (FieldContext.class.isAssignableFrom(childContext.getClass())) {
      return new FieldContext(childContext.symbol(), childContext.absolutePath(),
          this.symbols.getRelativePath(childContext.absolutePath(), parentContext.absolutePath()));
    }

    assert NodeContext.class.isAssignableFrom(
        childContext.getClass()) : "Child context should be either a FieldContext NodeContext.";

    return new NodeContext(childContext.symbol(), childContext.absolutePath(),
        this.symbols.getRelativePath(childContext.absolutePath(), parentContext.absolutePath()));
  }

  /*** Helpers ***/

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
}
