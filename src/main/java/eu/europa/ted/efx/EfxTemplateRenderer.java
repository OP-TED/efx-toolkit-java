package eu.europa.ted.efx;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Stack;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
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
import eu.europa.ted.efx.EfxParser.TextContext;
import eu.europa.ted.efx.EfxParser.TextTemplateContext;
import eu.europa.ted.efx.EfxParser.ValueTemplateContext;

public class EfxTemplateRenderer extends EfxToXPathTranspiler {

  static final boolean debug = true;

  private enum Indent {
    TABS, SPACES, UNSET
  };

  private Indent indentWith = Indent.UNSET;
  private int indentSpaces = -1;

  private Stack<Integer> indentStack = new Stack<>();

  NoticeRenderer renderer;

  LabelMap labels;

  NoticeReader xml;

  String language;

  public EfxTemplateRenderer(final SymbolMap symbols, final LabelMap labels,
      final NoticeReader xml, final NoticeRenderer renderer, final String language) {
    super(symbols);
    this.xml = xml;
    this.labels = labels;
    this.renderer = renderer;
    this.language = language;
  }

  // Static methods

  public static String renderTemplate(final String template, final SymbolMap symbols,
      final LabelMap labels, final NoticeReader xml, final NoticeRenderer renderer, final String language,
      final BaseErrorListener errorListener) {

    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(template));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);

    if (errorListener != null) {
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }

    final ParseTree tree = parser.templateFile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxTemplateRenderer translator =
        new EfxTemplateRenderer(symbols, labels, xml, renderer, language);
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
    this.renderer.beginFile();
  }

  @Override
  public void exitTemplateFile(TemplateFileContext ctx) {
    this.efxContext.pop();
    while (!this.indentStack.isEmpty()) {
      this.indentStack.pop();
      this.renderer.endBlock();
    }
    this.renderer.endFile();
  }

  @Override
  public void exitText(TextContext ctx) {
    this.stack.push(ctx.getText());
  }

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    String template = ctx.template() != null ? this.stack.pop() : "";
    String text = ctx.text() != null ? this.stack.pop() : "";
    this.stack.push(String.format("%s %s", text, template).trim());
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
    try {
      this.stack.push(this.labels.mapLabel(assetType, labelType, assetId, this.language));
    } catch (IOException e) {
      throw new ParseCancellationException("Unable to load labels.", e);
    }
  }

  @Override
  public void exitShorthandBtLabelTypeReference(ShorthandBtLabelTypeReferenceContext ctx) {
    String assetId = ctx.BtAssetId().getText();
    String labelType = ctx.labelType() != null ? this.stack.pop() : "";
    try {
      this.stack.push(this.labels.mapLabel("business_term", labelType, assetId, this.language));
    } catch (IOException e) {
      throw new ParseCancellationException("Unable to load labels.", e);
    }
  }

  @Override
  public void exitShorthandBtLabelReference(ShorthandBtLabelReferenceContext ctx) {
    try {
      this.stack.push(
          this.labels.mapLabel("business_term", "name", ctx.BtAssetId().getText(), this.language));
    } catch (IOException e) {
      throw new ParseCancellationException("Unable to load labels.", e);
    }
  }


  @Override
  public void exitShorthandFieldLabelTypeReference(ShorthandFieldLabelTypeReferenceContext ctx) {
    String assetId = ctx.FieldAssetId().getText();
    String labelType = ctx.labelType() != null ? this.stack.pop() : "";
    try {
      this.stack.push(this.labels.mapLabel("field", labelType, assetId, this.language));
    } catch (IOException e) {
      throw new ParseCancellationException("Unable to load labels.", e);
    }
  }

  @Override
  public void exitShorthandFieldLabelReference(ShorthandFieldLabelReferenceContext ctx) {
    try {
      this.stack
          .push(this.labels.mapLabel("field", "name", ctx.FieldAssetId().getText(), this.language));
    } catch (IOException e) {
      throw new ParseCancellationException("Unable to load labels.", e);
    }
  }

  @Override
  public void exitShorthandFieldValueLabelReference(ShorthandFieldValueLabelReferenceContext ctx) {
    final String fieldId = ctx.FieldAssetId().getText();
    final String contextPath = this.efxContext.peek();
    final String valuePath = symbols.relativeXpathOfField(fieldId, contextPath);
    final String value = this.xml.valueOf(valuePath, contextPath);
    final String fieldType = this.symbols.typeOfField(fieldId);
    switch (fieldType) {
      case "indicator":
        try {
          this.stack.push(this.labels.mapLabel("code", String.format("value-%s", value), fieldId,
              this.language));
        } catch (IOException e) {
          throw new ParseCancellationException("Unable to load labels.", e);
        }
        break;
      case "code":
        try {
          this.stack.push(this.labels.mapLabel("code", "value",
              String.format("%s.%s", this.symbols.rootCodelistOfField(fieldId), value),
              this.language));
        } catch (IOException e) {
          throw new ParseCancellationException("Unable to load labels.", e);
        }
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
    System.out.print("Value='" + ctx.getText() + "'");
  }

  @Override
  public void enterTemplateLine(TemplateLineContext ctx) {
    if (this.indentStack.isEmpty()) {
      this.indentStack.push(0);
    }
    this.efxContext.push(this.symbols.contextPathOfField(ctx.Context().getText()));

    System.out.println();
  }

  @Override
  public void exitTemplateLine(TemplateLineContext ctx) {
    int indentLevel = 0;

    if (ctx.Spaces() != null) {
      if (this.indentWith == Indent.UNSET) {
        this.indentWith = Indent.SPACES;
        this.indentSpaces = ctx.Spaces().getText().length();
      } else if (this.indentWith == Indent.TABS) {
        throw new InputMismatchException(
            "Don't mix indentation methods. Stick with either tabs or spaces.");
      }

      if (ctx.Spaces().getText().length() % this.indentSpaces != 0) {
        throw new InputMismatchException(String.format(
            "Inconsistent indentation. Expected a multiple of %s spaces.", this.indentSpaces));
      }
      indentLevel = ctx.Spaces().getText().length() / this.indentSpaces;
    } else if (ctx.Tabs() != null) {
      if (this.indentWith == Indent.UNSET) {
        this.indentWith = Indent.TABS;
      } else if (this.indentWith == Indent.SPACES) {
        throw new InputMismatchException(
            "Don't mix indentation methods. Stick with either tabs or spaces.");
      }

      indentLevel = ctx.Tabs().getText().length();
    }

    int indentChange = indentLevel - this.indentStack.peek();

    if (indentChange > 1) {
      throw new InputMismatchException("Indentation level skipped.");
    } else if (indentChange == 1) {
      this.indentStack.push(indentLevel);
      System.out.println(String.format("Indent %s", indentLevel));
      this.renderer.beginBlock(indentLevel, ctx.template().getText());
    } else if (indentChange < 0) {
      // lower indent level
      for (int i = indentChange; i < 0; i++) {
        if (this.indentStack.isEmpty()) {
          throw new RuntimeException("Unexpected indentation tracker state.");
        }
        if (this.indentStack.peek() > indentLevel) {
          this.indentStack.pop();
          this.renderer.endBlock();
        }
      }
      if (this.indentStack.peek() != indentLevel) {
        throw new RuntimeException("Unexpected indentation tracker state.");
      }
    } else if (indentChange == 0) {
      // same indent level
      this.renderer.beginBlock(indentLevel, ctx.template().getText());
    }
  }
}

