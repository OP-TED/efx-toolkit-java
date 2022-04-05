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
import org.antlr.v4.runtime.tree.TerminalNode;
import eu.europa.ted.efx.EfxParser.AssetIdContext;
import eu.europa.ted.efx.EfxParser.AssetTypeContext;
import eu.europa.ted.efx.EfxParser.ExpressionBlockContext;
import eu.europa.ted.efx.EfxParser.LabelTemplateContext;
import eu.europa.ted.efx.EfxParser.LabelTypeContext;
import eu.europa.ted.efx.EfxParser.ShorthandBtLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandBtLabelTypeReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandFieldLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandFieldLabelTypeReferenceContext;
import eu.europa.ted.efx.EfxParser.ShorthandFieldValueLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.StandardLabelReferenceContext;
import eu.europa.ted.efx.EfxParser.TemplateFileContext;
import eu.europa.ted.efx.EfxParser.TemplateLineContext;
import eu.europa.ted.efx.EfxParser.TextTemplateContext;
import eu.europa.ted.efx.EfxParser.ValueTemplateContext;
import eu.europa.ted.efx.interfaces.Renderer;
import eu.europa.ted.efx.interfaces.SymbolMap;
import eu.europa.ted.efx.interfaces.SyntaxMap;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;

public class EfxTemplateTranslator extends EfxExpressionTranslator {

  private static final String INCONSISTENT_INDENTATION_SPACES =
      "Inconsistent indentation. Expected a multiple of %d spaces.";
  private static final String INDENTATION_LEVEL_SKIPPED = "Indentation level skipped.";
  private static final String START_INTENDAT_AT_ZERO =
      "Incorrect intendation. Please do not indent the first level in your template.";
  private static final String MIXED_INDENTATION =
      "Don't mix indentation methods. Stick with either tabs or spaces.";
  private static final String UNEXPECTED_INDENTATION = "Unexpected indentation tracker state.";

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

  public EfxTemplateTranslator(final SymbolMap symbols, final SyntaxMap syntax, final Renderer renderer) {
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
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }

    final ParseTree tree = parser.templateFile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxTemplateTranslator translator = new EfxTemplateTranslator(factory, sdkVersion);

    walker.walk(translator, tree);

    return translator.getTranspiledXPath();
  }

  public static String renderTemplate(final String template, final SymbolMap symbols, final SyntaxMap syntax,
      final Renderer renderer, final BaseErrorListener errorListener) {

    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(template));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);

    if (errorListener != null) {
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
    this.efxContext.push(null);
    assert blockStack.isEmpty() : UNEXPECTED_INDENTATION;
  }

  @Override
  public void exitTemplateFile(TemplateFileContext ctx) {
    this.efxContext.pop();
    this.blockStack.pop();

    List<String> templateCalls = new ArrayList<>();
    List<String> templates = new ArrayList<>();
    for (ContentBlock rootBlock : this.rootBlock.children) {
      templateCalls.add(rootBlock.renderCallTemplate(renderer));
      rootBlock.renderTemplate(renderer, templates);
    }
    String file =
        this.renderer.renderFile(String.join("\n", templateCalls), String.join("\n", templates));
    this.stack.push(file);
  }

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    String template = ctx.template() != null ? this.stack.pop() : "";
    String text = ctx.text() != null ? ctx.text().getText() : "";
    this.stack.push(String.format("%s%s", this.renderer.renderFreeText(text), template));
  }

  @Override
  public void exitLabelTemplate(LabelTemplateContext ctx) {
    String template = ctx.template() != null ? this.stack.pop() : "";
    String text = ctx.labelBlock() != null ? this.stack.pop() : "";
    this.stack.push(String.format("%s %s", text, template).trim());
  }

  @Override
  public void exitValueTemplate(ValueTemplateContext ctx) {
    String template = ctx.template() != null ? this.stack.pop() : "";
    String expression = ctx.expressionBlock() != null ? this.stack.pop() : "";
    this.stack.push(String.format("%s %s", expression, template).trim());
  }

  @Override
  public void exitStandardLabelReference(StandardLabelReferenceContext ctx) {
    String assetId = ctx.assetId() != null ? this.stack.pop() : "";
    String labelType = ctx.labelType() != null ? this.stack.pop() : "";
    String assetType = ctx.assetType() != null ? this.stack.pop() : "";
    this.stack.push(this.renderer
        .renderLabelFromKey(String.format("%s|%s|%s", assetType, labelType, assetId)));
  }

  @Override
  public void exitShorthandBtLabelTypeReference(ShorthandBtLabelTypeReferenceContext ctx) {
    String assetId = ctx.BtAssetId().getText();
    String labelType = ctx.labelType() != null ? this.stack.pop() : "";
    this.stack.push(this.renderer
        .renderLabelFromKey(String.format("%s|%s|%s", "business_term", labelType, assetId)));
  }

  @Override
  public void exitShorthandBtLabelReference(ShorthandBtLabelReferenceContext ctx) {
    this.stack.push(this.renderer.renderLabelFromKey(
        String.format("%s|%s|%s", "business_term", "name", ctx.BtAssetId().getText())));
  }


  @Override
  public void exitShorthandFieldLabelTypeReference(ShorthandFieldLabelTypeReferenceContext ctx) {
    String assetId = ctx.FieldAssetId().getText();
    String labelType = ctx.labelType() != null ? this.stack.pop() : "";
    this.stack.push(
        this.renderer.renderLabelFromKey(String.format("%s|%s|%s", "field", labelType, assetId)));
  }

  @Override
  public void exitShorthandFieldLabelReference(ShorthandFieldLabelReferenceContext ctx) {
    this.stack.push(this.renderer.renderLabelFromKey(
        String.format("%s|%s|%s", "field", "name", ctx.FieldAssetId().getText())));
  }

  @Override
  public void exitShorthandFieldValueLabelReference(ShorthandFieldValueLabelReferenceContext ctx) {
    final String fieldId = ctx.FieldAssetId().getText();
    final Context contextPath = this.efxContext.peek();
    final String valuePath = symbols.relativeXpathOfField(fieldId, contextPath.absolutePath());
    final String fieldType = this.symbols.typeOfField(fieldId);
    switch (fieldType) {
      case "indicator":
        this.stack.push(this.renderer.renderLabelFromExpression(String.format("concat('code|value-', %s, '|%s')", valuePath, fieldId)));
        break;
      case "code":
      case "internal-code":
        this.stack.push(this.renderer.renderLabelFromExpression(String.format("concat('code|value|%s.', %s)", this.symbols.rootCodelistOfField(fieldId), valuePath)));
        break;
      default:
        throw new InputMismatchException(String.format(
            "Unexpected field type '%s'. Expected a field of either type 'code' or 'indicator'.",
            fieldType));
    }
  }

  @Override
  public void exitAssetType(AssetTypeContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(ctx.getText());
    }
  }

  @Override
  public void exitLabelType(LabelTypeContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(ctx.getText());
    }
  }

  @Override
  public void exitAssetId(AssetIdContext ctx) {
    if (ctx.expressionBlock() == null) {
      this.stack.push(ctx.getText());
    }
  }

  @Override
  public void exitExpressionBlock(ExpressionBlockContext ctx) {
    final String expression = this.stack.pop();
    this.stack.push(this.renderer.renderValueReference(expression));
  }

  @Override
  public void enterTemplateLine(TemplateLineContext ctx) {
    final Context context = this.getTemplateLineContext(ctx);
    this.efxContext.push(context);
  }

  @Override
  public void exitTemplateLine(TemplateLineContext ctx) {
    this.efxContext.pop();
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
      this.blockStack.pushChild(content, this.getTemplateLineContext(ctx));
    } else if (indentChange < 0) {
      // lower indent level
      for (int i = indentChange; i < 0; i++) {
        assert !this.blockStack.isEmpty() : UNEXPECTED_INDENTATION;
        assert this.blockStack.currentIndentationLevel() > indentLevel : UNEXPECTED_INDENTATION;
        this.blockStack.pop();
      }
      assert this.blockStack.currentIndentationLevel() == indentLevel : UNEXPECTED_INDENTATION;
      this.blockStack.pushSibling(content, this.getTemplateLineContext(ctx));
    } else if (indentChange == 0) {

      if (blockStack.isEmpty()) {
        assert indentLevel == 0 : UNEXPECTED_INDENTATION;
        this.blockStack.push(this.rootBlock.addChild(content, this.getTemplateLineContext(ctx)));
      } else {
        this.blockStack.pushSibling(content, this.getTemplateLineContext(ctx));
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


  private Context getTemplateLineContext(TemplateLineContext ctx) {
    final int indent = this.getIndentLevel(ctx);
    final ContentBlock contextBlock = this.blockStack.blockAtLevel(indent - 1);
    final Context currentContext = contextBlock == null ? null : contextBlock.context;

    final TerminalNode fieldContext = ctx.FieldContext();
    if (fieldContext != null) {
      return Context.fromFieldId(fieldContext.getText(), currentContext, this.symbols);
    }
    
    final TerminalNode nodeContext = ctx.NodeContext();
    if (nodeContext != null) {
      return Context.fromNodeId(nodeContext.getText(), currentContext, this.symbols);
    }

    throw new RuntimeException("Unexpected context tracking state.");
  }
}

