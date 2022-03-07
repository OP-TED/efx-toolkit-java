package eu.europa.ted.efx.xpath;

import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.europa.ted.efx.EfxLexer;
import eu.europa.ted.efx.EfxParser;
import eu.europa.ted.efx.EfxtBaseListener;
import eu.europa.ted.efx.EfxtLexer;
import eu.europa.ted.efx.EfxtParser;
import eu.europa.ted.efx.EfxtParser.LabelReferenceContext;
import eu.europa.ted.efx.EfxtParser.LabelTemplateContext;
import eu.europa.ted.efx.EfxtParser.TemplateFileContext;
import eu.europa.ted.efx.EfxtParser.TextTemplateContext;
import eu.europa.ted.efx.EfxtParser.ValueReferenceContext;
import eu.europa.ted.efx.EfxtParser.ValueTemplateContext;

public class EfxtToXsltTranslator extends EfxtBaseListener {

  static final boolean debug = true;

  /**
   * The stack is used by the methods of this listener to pass data to each other as the parse tree
   * is being walked.
   */
  Stack<String> stack = new Stack<>();

  @Override
  public void exitValueReference(ValueReferenceContext ctx) {
    this.stack.push(this.stack.pop());
  }

  @Override
  public void exitLabelReference(LabelReferenceContext ctx) {
    this.stack.push("{field|name|" + ctx.fieldId.getText() + "}");
  }

  @Override
  public void exitLabelTemplate(LabelTemplateContext ctx) {
    final String templ = ctx.templ == null ? "" : this.stack.pop();
    final String lbl = this.stack.pop();
    // Use xpath to get label from SDK files
    this.stack.push(lbl + templ);
  }

  @Override
  public void exitValueTemplate(ValueTemplateContext ctx) {
    final String templ = ctx.templ == null ? "" : this.stack.pop();
    final String value = this.stack.pop();
    this.stack.push(String.format("<xsl:value-of select=\"%s\" />%s", value, templ));
  }

  @Override
  public void exitTextTemplate(TextTemplateContext ctx) {
    final String templ = ctx.templ == null ? "" : this.stack.pop();
    final String text = this.stack.pop();
    this.stack.push(text + templ);
  }

  /**
   * The context stack is used to keep track of xPath contexts for nested conditions.
   */
  Stack<String> efxContext = new Stack<>();

  /**
   * Symbols are field identifiers and node identifiers. The symbol table is used to resolve these
   * identifiers to their xPath.
   */
  final EfxToXpathSymbols symbols;

  /**
   * Maps efx operators to xPath operators.
   */
  static final HashMap<String, String> operators = new HashMap<>();
  static {
    operators.put("+", "+");
    operators.put("-", "-");
    operators.put("*", "*");
    operators.put("/", "div");
    operators.put("%", "mod");
    operators.put("and", "and");
    operators.put("or", "or");
    operators.put("==", "=");
    operators.put("!=", "!=");
    operators.put("<", "<");
    operators.put("<=", "<=");
    operators.put(">", ">");
    operators.put(">=", ">=");
  }

  public EfxtToXsltTranslator(final String sdkVersion) {
    this.symbols = EfxToXpathSymbols.getInstance(sdkVersion);
  }

  // Static methods

  public static String translateTestFile(final String fileName, final String sdkVersion)
      throws IOException {

    final EfxLexer lexer = new EfxLexer(CharStreams.fromFileName(fileName));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);
    final ParseTree tree = parser.testfile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxtToXsltTranslator translator = new EfxtToXsltTranslator(sdkVersion);
    walker.walk(translator, tree);

    return translator.getTranslatedString();
  }

  public static String translateTemplateFile(final String fileName, final String sdkVersion)
      throws IOException {

    final EfxtLexer lexer = new EfxtLexer(CharStreams.fromFileName(fileName));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxtParser parser = new EfxtParser(tokens);
    final ParseTree tree = parser.templateFile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxtToXsltTranslator translator = new EfxtToXsltTranslator(sdkVersion);
    walker.walk(translator, tree);

    return translator.getTranslatedString();
  }

  public static String translateCondition(final String condition, final String sdkVersion) {

    final EfxtLexer lexer = new EfxtLexer(CharStreams.fromString(condition));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxtParser parser = new EfxtParser(tokens);
    final ParseTree tree = parser.condition();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxtToXsltTranslator translator = new EfxtToXsltTranslator(sdkVersion);
    walker.walk(translator, tree);

    return translator.getTranslatedString();
  }

  /**
   * Call this method to get the translated code after the walker finished its walk.
   *
   * @return The translated code, trimmed
   */
  public String getTranslatedString() {
    final StringBuilder sb = new StringBuilder(64);
    while (!this.stack.empty()) {
      sb.insert(0, this.stack.pop() + '\n'); //
    }
    return sb.toString().trim();
  }

  @Override
  public void enterTemplateFile(TemplateFileContext ctx) {
    this.efxContext.push(null);
    this.stack.push("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    this.stack.push(
        "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">");
    this.stack.push("<xsl:output method=\"xml\" indent=\"yes\"/>");
  }

  @Override
  public void exitTemplateFile(TemplateFileContext ctx) {
    this.efxContext.pop();
    this.stack.push("</xsl:stylesheet>");
  }

  @Override
  public void enterStatement(EfxtParser.StatementContext ctx) {
    // Set the contextPath for the statement
    // TODO: also take axis into account here
    this.efxContext.push(symbols.getContextPathOfField(ctx.context().field.getText()));
  }

  @Override
  public void exitStatement(EfxtParser.StatementContext ctx) {
    this.efxContext.pop();
    if (debug) {
      System.out.println("[In]:  " + ctx.getText());
      System.out.println("[Out]: " + this.stack.peek());
      System.out.println();
    }
  }

  @Override
  public void exitLineComment(EfxtParser.LineCommentContext ctx) {
    if (debug) {
      System.out.println(ctx.getText());
    }
  }

  @Override
  public void exitLogicalAndCondition(EfxtParser.LogicalAndConditionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + " " + operators.get("and") + " " + right);
  }

  @Override
  public void exitLogicalOrCondition(EfxtParser.LogicalOrConditionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + " " + operators.get("or") + " " + right);
  }

  @Override
  public void exitLogicalNotCondition(EfxtParser.LogicalNotConditionContext ctx) {
    String condition = this.stack.pop();
    this.stack.push(operators.get("not") + " " + condition);
  }

  @Override
  public void exitAlwaysCondition(EfxtParser.AlwaysConditionContext ctx) {
    this.stack.push("true");
  }

  @Override
  public void exitNeverCondition(EfxtParser.NeverConditionContext ctx) {
    this.stack.push("false");
  }

  @Override
  public void exitComparisonCondition(EfxtParser.ComparisonConditionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + operators.get(ctx.operator.getText()) + right);
  }

  @Override
  public void exitEmptynessCondition(EfxtParser.EmptynessConditionContext ctx) {
    String expression = this.stack.pop();
    String operator = ctx.modifier != null && ctx.modifier.getText() == "not" ? "!=" : "==";
    this.stack.push(expression + " " + operators.get(operator) + " ''");
  }

  @Override
  public void exitPresenceCondition(EfxtParser.PresenceConditionContext ctx) {
    String reference = this.stack.pop();
    String operator = ctx.modifier != null && ctx.modifier.getText() == "not" ? "==" : "!=";
    this.stack.push(reference + " " + operators.get(operator) + " ''");
  }

  @Override
  public void exitParenthesizedCondition(EfxtParser.ParenthesizedConditionContext ctx) {
    String condition = this.stack.pop();
    this.stack.push('(' + condition + ')');
  }

  @Override
  public void exitExpressionCondition(EfxtParser.ExpressionConditionContext ctx) {
    String expression = this.stack.pop();
    this.stack.push(expression);
  }

  @Override
  public void exitLikePatternCondition(EfxtParser.LikePatternConditionContext ctx) {
    String expression = this.stack.pop();
    String modifier = ctx.modifier != null && ctx.modifier.getText() == "not" ? "!" : "";
    this.stack.push(modifier + "fn:matches(" + expression + ", " + ctx.pattern.getText() + ")");
  }

  @Override
  public void exitInListCondition(EfxtParser.InListConditionContext ctx) {
    String list = this.stack.pop();
    String expression = this.stack.pop();
    String not = ctx.modifier != null && ctx.modifier.getText() == "not" ? "!" : "";
    this.stack.push(not + list + ".contains(" + expression + ")");
  }

  @Override
  public void exitAdditionExpression(EfxtParser.AdditionExpressionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + operators.get(ctx.operator.getText()) + right);
  }

  @Override
  public void exitMultiplicationExpression(EfxtParser.MultiplicationExpressionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + operators.get(ctx.operator.getText()) + right);
  }

  @Override
  public void exitList(EfxtParser.ListContext ctx) {
    if (this.stack.empty() || ctx.value().size() == 0) {
      this.stack.push("{}");
      return;
    }

    String list = this.stack.pop() + " }";
    for (int i = 1; i < ctx.value().size(); i++) {
      list = this.stack.pop() + ", " + list;
    }
    list = "{ " + list;
    this.stack.push(list);
  }

  @Override
  public void exitLiteral(EfxtParser.LiteralContext ctx) {
    this.stack.push(ctx.getText());
  }

  @Override
  public void exitFunctionCall(EfxtParser.FunctionCallContext ctx) {
    this.stack.push(ctx.getText());
  }

  @Override
  public void exitNoticeReference(EfxtParser.NoticeReferenceContext ctx) {
    this.stack.push("fn:doc('http://notice.service/" + this.stack.pop() + "')");
  }

  @Override
  public void exitNodeReference(EfxtParser.NodeReferenceContext ctx) {
    this.stack
        .push(symbols.getRelativeXpathOfFieldOrNode(ctx.node.getText(), this.efxContext.peek()));
  }

  @Override
  public void exitFieldInNoticeReference(EfxtParser.FieldInNoticeReferenceContext ctx) {
    String field = this.stack.pop();
    String notice = this.stack.pop();
    this.stack.push(notice + "/" + field);
  }

  @Override
  public void exitSimpleFieldReference(EfxtParser.SimpleFieldReferenceContext ctx) {
    this.stack
        .push(symbols.getRelativeXpathOfFieldOrNode(ctx.field.getText(), this.efxContext.peek()));
  }

  @Override
  public void exitFieldReferenceWithPredicate(EfxtParser.FieldReferenceWithPredicateContext ctx) {
    String condition = this.stack.pop();
    String fieldRef = this.stack.pop();
    this.stack.push(fieldRef + '[' + condition + ']');
  }

  /**
   * Any field references in the predicate must be resolved relative to the field on which the
   * predicate is applied. Therefore we need to switch to the field's context while the predicate is
   * being parsed.
   */
  @Override
  public void enterPredicate(EfxtParser.PredicateContext ctx) {
    EfxtParser.SimpleFieldReferenceContext refCtx =
        ctx.getParent().getChild(EfxtParser.SimpleFieldReferenceContext.class, 0);
    this.efxContext
        .push(symbols.getNarrowerContextPathOf(refCtx.field.getText(), this.efxContext.peek()));
  }

  /**
   * After the predicate is parsed we need to switch back to the previous context.
   */
  @Override
  public void exitPredicate(EfxtParser.PredicateContext ctx) {
    this.efxContext.pop();
  }

  @Override
  public void enterReferenceWithContextOverride(
      EfxtParser.ReferenceWithContextOverrideContext ctx) {
    this.efxContext
        .push(symbols.getNarrowerContextPathOf(ctx.ctx.getText(), this.efxContext.peek()));
  }

  @Override
  public void exitReferenceWithContextOverride(EfxtParser.ReferenceWithContextOverrideContext ctx) {
    this.efxContext.pop();
  }

  @Override
  public void exitSimpleReference(EfxtParser.SimpleReferenceContext ctx) {
    String field = this.stack.pop();
    String attribute = ctx.attribute != null ? "/@" + ctx.attribute.getText() : "/text()";
    this.stack.push(field + attribute);
  }
}

