package eu.europa.ted.efx;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.europa.ted.efx.EfxParser.AssetIdContext;
import eu.europa.ted.efx.EfxParser.AssetTypeContext;
import eu.europa.ted.efx.EfxParser.ContextDeclarationBlockContext;
import eu.europa.ted.efx.EfxParser.LabelTemplateContext;
import eu.europa.ted.efx.EfxParser.LabelTypeContext;
import eu.europa.ted.efx.EfxParser.ShorthandBtLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandContextFieldLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandContextFieldValueReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandContextLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandFieldLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandFieldValueLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.StandardExpressionBlockContext;
import eu.europa.ted.efx.EfxParser.StandardLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.TemplateFileContext;
import eu.europa.ted.efx.EfxParser.TemplateLineContext;
import eu.europa.ted.efx.EfxParser.TextTemplateContext;
import eu.europa.ted.efx.EfxParser.ValueTemplateContext;
import eu.europa.ted.efx.interfaces.Renderer;
import eu.europa.ted.efx.interfaces.SymbolMap;
import eu.europa.ted.efx.interfaces.SyntaxMap;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.model.ContentBlock;
import eu.europa.ted.efx.model.ContentBlockStack;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;

public class EfxTemplateTranslator extends EfxExpressionTranslator {

  private static final String INCONSISTENT_INDENTATION_SPACES =
      "Inconsistent indentation. Expected a multiple of %d spaces.";
  private static final String INDENTATION_LEVEL_SKIPPED = "Indentation level skipped.";
  private static final String START_INTENDAT_AT_ZERO =
      "Incorrect intendation. Please do not indent the first level in your template.";
  private static final String MIXED_INDENTATION =
      "Don't mix indentation methods. Stick with either tabs or spaces.";
  private static final String UNEXPECTED_INDENTATION = "Unexpected indentation tracker state.";

  private static final String LABEL_TYPE_VALUE =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.LABEL_TYPE_VALUE).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_INDICATOR =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_INDICATOR).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_BT =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_BT).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_FIELD =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_FIELD).replaceAll("^'|'$", "");
  private static final String ASSET_TYPE_CODE =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.ASSET_TYPE_CODE).replaceAll("^'|'$", "");

  static final boolean debug = true;

  private enum Indent {
    TABS, SPACES, UNSET
  };

  private Indent indentWith = Indent.UNSET;
  private int indentSpaces = -1;

  Renderer renderer;


  final ContentBlock rootBlock = ContentBlock.newRootBlock();

  ContentBlockStack blockStack = new ContentBlockStack();


  public EfxTemplateTranslator(TranslatorDependencyFactory factory, final String sdkVersion) {
    super(factory.createSymbolMap(sdkVersion), factory.createSyntaxMap());
    this.renderer = factory.createRenderer();
  }

  public EfxTemplateTranslator(final SymbolMap symbols, final SyntaxMap syntax,
      final Renderer renderer) {
    super(symbols, syntax);
    this.renderer = renderer;
  }

  // Static methods

  public static String renderTemplateFile(final Path pathname, final String sdkVersion,
      final TranslatorDependencyFactory factory) throws IOException {
    final EfxLexer lexer = new EfxLexer(CharStreams.fromPath(pathname));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);

    final BaseErrorListener errorListener = factory.createErrorListener();
    if (errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }

    final ParseTree tree = parser.templateFile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxTemplateTranslator translator = new EfxTemplateTranslator(factory, sdkVersion);

    walker.walk(translator, tree);

    return translator.getTranspiledXPath();
  }

  public static String renderTemplate(final String template, final String sdkVersion,
      final TranslatorDependencyFactory factory) {

    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(template));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);

    final BaseErrorListener errorListener = factory.createErrorListener();
    if (errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }

    final ParseTree tree = parser.templateFile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxTemplateTranslator translator = new EfxTemplateTranslator(factory, sdkVersion);

    walker.walk(translator, tree);

    return translator.getTranspiledXPath();
  }

  public static String renderTemplate(final String template, final SymbolMap symbols,
      final SyntaxMap syntax, final Renderer renderer, final BaseErrorListener errorListener) {

    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(template));
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
    final EfxTemplateTranslator translator = new EfxTemplateTranslator(symbols, syntax, renderer);
    walker.walk(translator, tree);

    return translator.getTranspiledXPath();
  }

  /**
   * Call this method to get the translated code after the walker finished its walk.
   *
   * @return The translated code, trimmed
   */
  private String getTranspiledXPath() {
    final StringBuilder sb = new StringBuilder(64);
    while (!this.stack.empty()) {
      sb.insert(0, this.stack.pop() + '\n'); //
    }
    return sb.toString().trim();
  }

  @Override
  public void enterTemplateFile(TemplateFileContext ctx) {
    assert blockStack.isEmpty() : UNEXPECTED_INDENTATION;
  }

  @Override
  public void exitTemplateFile(TemplateFileContext ctx) {
    this.blockStack.pop();

    List<String> templateCalls = new ArrayList<>();
    List<String> templates = new ArrayList<>();
    for (ContentBlock rootBlock : this.rootBlock.getChildren()) {
      templateCalls.add(rootBlock.renderCallTemplate(renderer));
      rootBlock.renderTemplate(renderer, templates);
    }
    String file = this.renderer.renderFile(templateCalls, templates);
    this.stack.push(file);
  }

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    String template = ctx.templateFragment() != null ? this.stack.pop() : "";
    String text = ctx.text() != null ? ctx.text().getText() : "";
    this.stack.push(String.format("%s%s", this.renderer.renderFreeText(text), template));
  }

  @Override
  public void exitLabelTemplate(LabelTemplateContext ctx) {
    String template = ctx.templateFragment() != null ? this.stack.pop() : "";
    String text = ctx.labelBlock() != null ? this.stack.pop() : "";
    this.stack.push(String.format("%s %s", text, template).trim());
  }

  @Override
  public void exitValueTemplate(ValueTemplateContext ctx) {
    String template = ctx.templateFragment() != null ? this.stack.pop() : "";
    String expression = ctx.expressionBlock() != null ? this.stack.pop() : "";
    this.stack.push(String.format("%s %s", expression, template).trim());
  }

  @Override
  public void exitStandardLabelReference(StandardLabelReferenceContext ctx) {
    String assetId = ctx.assetId() != null ? this.stack.pop() : "";
    String labelType = ctx.labelType() != null ? this.stack.pop() : "";
    String assetType = ctx.assetType() != null ? this.stack.pop() : "";
    this.stack.push(this.renderer.renderLabelFromKey(this.syntax.mapStringConcatenationFunction(
        List.of(assetType, quoted("|"), labelType, quoted("|"), assetId))));
  }

  @Override
  public void exitShorthandBtLabelReference(ShorthandBtLabelReferenceContext ctx) {
    String assetId = ctx.BtAssetId().getText();
    String labelType = ctx.labelType() != null ? this.stack.pop() : quoted("");
    this.stack.push(this.renderer.renderLabelFromKey(this.syntax.mapStringConcatenationFunction(
        List.of(quoted(ASSET_TYPE_BT), quoted("|"), labelType, quoted("|"), quoted(assetId)))));
  }

  @Override
  public void exitShorthandFieldLabelReference(ShorthandFieldLabelReferenceContext ctx) {
    final String fieldId = ctx.FieldAssetId().getText();
    String labelType = ctx.labelType() != null ? this.stack.pop() : quoted("");

    if (labelType.equals("value")) {
      this.shorthandFieldValueLabelReference(fieldId);
    } else {
      this.stack.push(this.renderer.renderLabelFromKey(
          this.syntax.mapStringConcatenationFunction(List.of(quoted(ASSET_TYPE_FIELD), quoted("|"),
              labelType, quoted("|"), quoted(fieldId)))));
    }
  }

  @Override
  public void exitShorthandFieldValueLabelReference(ShorthandFieldValueLabelReferenceContext ctx) {
    this.shorthandFieldValueLabelReference(ctx.FieldAssetId().getText());
  }


  private void shorthandFieldValueLabelReference(final String fieldId) {
    final Context currentContext = this.efxContext.peek();
    final String valueReference = this.syntax.mapFieldValueReference(
        symbols.relativeXpathOfField(fieldId, currentContext.absolutePath()));
    final String fieldType = this.symbols.typeOfField(fieldId);
    switch (fieldType) {
      case "indicator":
        this.stack.push(
            this.renderer.renderLabelFromExpression(this.syntax.mapStringConcatenationFunction(
                List.of(quoted(ASSET_TYPE_INDICATOR), quoted("|"), quoted(LABEL_TYPE_VALUE),
                    quoted("-"), valueReference, quoted("|"), quoted(fieldId)))));
        break;
      case "code":
      case "internal-code":
        this.stack.push(
            this.renderer.renderLabelFromExpression(this.syntax.mapStringConcatenationFunction(
                List.of(quoted(ASSET_TYPE_CODE), quoted("|"), quoted(LABEL_TYPE_VALUE), quoted("|"),
                    quoted(this.symbols.rootCodelistOfField(fieldId)), valueReference))));
        break;
      default:
        throw new InputMismatchException(String.format(
            "Unexpected field type '%s'. Expected a field of either type 'code' or 'indicator'.",
            fieldType));
    }
  }

  /**
   * Handles the #{labelType} shorthand syntax which renders the label of the Field or Node declared
   * as the context.
   * 
   * If the labelType is 'value', then the label depends on the field's value and is rendered
   * according to the field's type. The assetType is infered from the Field or Node declared as
   * context.
   */
  @Override
  public void exitShorthandContextLabelReference(ShorthandContextLabelReferenceContext ctx) {
    final String labelType = ctx.LabelType().getText();
    if (this.efxContext.isFieldContext()) {
      if (labelType.equals(LABEL_TYPE_VALUE)) {
        this.shorthandFieldValueLabelReference(this.efxContext.symbol());
      } else {
        this.stack.push(this.renderer.renderLabelFromKey(
            this.syntax.mapStringConcatenationFunction(List.of(quoted(ASSET_TYPE_FIELD),
                quoted("|"), quoted(labelType), quoted("|"), quoted(this.efxContext.symbol())))));
      }
    } else if (this.efxContext.isNodeContext()) {
      this.stack.push(this.renderer.renderLabelFromKey(
          this.syntax.mapStringConcatenationFunction(List.of(quoted(ASSET_TYPE_BT), quoted("|"),
              quoted(labelType), quoted("|"), quoted(this.efxContext.symbol())))));
    }
  }

  /**
   * Handles the #value shorthand syntax which renders the label coresponding to the value of the
   * the field declared as the context of the current line of the template.
   * This shorthand syntax is only supported for fields of type 'code' or 'indicator'.
   */
  @Override
  public void exitShorthandContextFieldLabelReference(
      ShorthandContextFieldLabelReferenceContext ctx) {
    if (!this.efxContext.isFieldContext()) {
      throw new InputMismatchException(
          "The #value shorthand syntax can only be used in a field is declared as context.");
    }
    this.shorthandFieldValueLabelReference(this.efxContext.symbol());
  }

  @Override
  public void exitAssetType(AssetTypeContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(quoted(ctx.getText()));
    }
  }

  @Override
  public void exitLabelType(LabelTypeContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(quoted(ctx.getText()));
    }
  }

  @Override
  public void exitAssetId(AssetIdContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(quoted(ctx.getText()));
    }
  }

  /**
   * Handles a standard expression block in a template line. Most of the work is done by the base
   * class {@link EfxExpressionTranslator}. After the expression is translated, the result is passed
   * through the renderer.
   */
  @Override
  public void exitStandardExpressionBlock(StandardExpressionBlockContext ctx) {
    this.stack.push(this.renderer.renderValueReference(this.stack.pop()));
  }

  /***
   * Handles the $value shorthand syntax which renders the value of the field declared as context in
   * the current line of the template.
   */
  @Override
  public void exitShorthandContextFieldValueReference(
      ShorthandContextFieldValueReferenceContext ctx) {
    if (!this.efxContext.isFieldContext()) {
      throw new InputMismatchException(
          "The $value shorthand syntax can only be used when a field is declated as the context.");
    }
    this.stack.push(this.renderer.renderValueReference(this.syntax.mapFieldValueReference(
        symbols.relativeXpathOfField(this.efxContext.symbol(), this.efxContext.absolutePath()))));
  }

  /**
   * This method changes the current EFX context.
   * 
   * The EFX context is always assummed to be either a Field or a Node. Any predicate included in
   * the EFX context declaration is not relevant and is ignored.
   */
  @Override
  public void exitContextDeclarationBlock(ContextDeclarationBlockContext ctx) {

    final String filedId = getContextFieldId(ctx);
    if (filedId != null) {
      this.efxContext.push(new FieldContext(filedId, this.stack.pop()));
    } else {
      final String nodeId = getContextNodeId(ctx);
      assert nodeId != null : "We should have been able to locate the FieldId or NodeId declared as context.";
      this.efxContext.push(new NodeContext(nodeId, this.stack.pop()));
    }
  }

  @Override
  public void exitTemplateLine(TemplateLineContext ctx) {
    final Context lineContext = this.efxContext.pop();
    final int indentLevel = this.getIndentLevel(ctx);
    final int indentChange = indentLevel - this.blockStack.currentIndentationLevel();
    final String content = ctx.template() != null ? this.stack.pop() : "";
    assert this.stack.isEmpty() : "Stack should be empty at this point.";

    if (indentChange > 1) {
      throw new InputMismatchException(INDENTATION_LEVEL_SKIPPED);
    } else if (indentChange == 1) {
      if (this.blockStack.isEmpty()) {
        throw new InputMismatchException(START_INTENDAT_AT_ZERO);
      }
      this.blockStack.pushChild(content, lineContext);
    } else if (indentChange < 0) {
      // lower indent level
      for (int i = indentChange; i < 0; i++) {
        assert !this.blockStack.isEmpty() : UNEXPECTED_INDENTATION;
        assert this.blockStack.currentIndentationLevel() > indentLevel : UNEXPECTED_INDENTATION;
        this.blockStack.pop();
      }
      assert this.blockStack.currentIndentationLevel() == indentLevel : UNEXPECTED_INDENTATION;
      this.blockStack.pushSibling(content, lineContext);
    } else if (indentChange == 0) {

      if (blockStack.isEmpty()) {
        assert indentLevel == 0 : UNEXPECTED_INDENTATION;
        this.blockStack.push(this.rootBlock.addChild(content, lineContext));
      } else {
        this.blockStack.pushSibling(content, lineContext);
      }
    }
  }

  private int getIndentLevel(TemplateLineContext ctx) {
    if (ctx.Spaces() != null) {
      if (this.indentWith == Indent.UNSET) {
        this.indentWith = Indent.SPACES;
        this.indentSpaces = ctx.Spaces().getText().length();
      } else if (this.indentWith == Indent.TABS) {
        throw new InputMismatchException(MIXED_INDENTATION);
      }

      if (ctx.Spaces().getText().length() % this.indentSpaces != 0) {
        throw new InputMismatchException(
            String.format(INCONSISTENT_INDENTATION_SPACES, this.indentSpaces));
      }
      return ctx.Spaces().getText().length() / this.indentSpaces;
    } else if (ctx.Tabs() != null) {
      if (this.indentWith == Indent.UNSET) {
        this.indentWith = Indent.TABS;
      } else if (this.indentWith == Indent.SPACES) {
        throw new InputMismatchException(MIXED_INDENTATION);
      }

      return ctx.Tabs().getText().length();
    }
    return 0;
  }
}

