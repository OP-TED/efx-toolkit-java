package eu.europa.ted.efx.xpath;

import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import eu.europa.ted.efx.EfxBaseListener;
import eu.europa.ted.efx.EfxLexer;
import eu.europa.ted.efx.EfxParser;
import eu.europa.ted.efx.EfxParser.CodeListContext;
import eu.europa.ted.efx.EfxParser.CodelistReferenceContext;
import eu.europa.ted.efx.EfxParser.ExplicitListContext;

public class EfxToXpathTranslator extends EfxBaseListener {

  static final boolean debug = false;

  /**
   * The stack is used by the methods of this listener to pass data to each other as the parse tree
   * is being walked.
   */
  Stack<String> stack = new Stack<>();

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
    operators.put("not", "not");
    operators.put("==", "=");
    operators.put("!=", "!=");
    operators.put("<", "<");
    operators.put("<=", "<=");
    operators.put(">", ">");
    operators.put(">=", ">=");
  }

  public EfxToXpathTranslator(final String sdkVersion, final boolean useNewContextualizer) {
    this.symbols = EfxToXpathSymbols.getInstance(sdkVersion);
    this.symbols.useNewContextualizer(useNewContextualizer);
  }

  // Static methods

  public static String translateTestFile(final String fileName, final String sdkVersion,
      final boolean useNewContextualizer) throws IOException {

    final EfxLexer lexer = new EfxLexer(CharStreams.fromFileName(fileName));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);
    final ParseTree tree = parser.testfile();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxToXpathTranslator translator =
        new EfxToXpathTranslator(sdkVersion, useNewContextualizer);
    walker.walk(translator, tree);

    return translator.getTranslatedString();
  }

  public static String translateCondition(final String condition, final String sdkVersion,
      final boolean useNewContextualizer) {

    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(condition));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);
    final ParseTree tree = parser.statement();

    final ParseTreeWalker walker = new ParseTreeWalker();
    final EfxToXpathTranslator translator =
        new EfxToXpathTranslator(sdkVersion, useNewContextualizer);
    walker.walk(translator, tree);

    return translator.getTranslatedString();
  }

  /**
   * Call this method to get the translated code after the walker finished its walk.
   *
   * @return The translated code, trimmed
   */
  public String getTranslatedString() {
    final StringBuilder sb = new StringBuilder(this.stack.size() * 10);
    while (!this.stack.empty()) {
      sb.insert(0, '\n').insert(0, this.stack.pop());
    }
    return sb.toString().trim();
  }

  @Override
  public void enterStatement(EfxParser.StatementContext ctx) {
    // Set the contextPath for the statement
    // TODO: also take axis into account here
    this.efxContext.push(symbols.getContextPathOfField(ctx.context().field.getText()));
  }

  @Override
  public void exitStatement(EfxParser.StatementContext ctx) {
    this.efxContext.pop();
    if (debug) {
      System.out.println("[In]:  " + ctx.getText());
      System.out.println("[Out]: " + this.stack.peek());
      System.out.println();
    }
  }

  @Override
  public void exitLineComment(EfxParser.LineCommentContext ctx) {
    if (debug) {
      System.out.println(ctx.getText());
    }
  }

  @Override
  public void exitLogicalAndCondition(EfxParser.LogicalAndConditionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + " " + operators.get("and") + " " + right);
  }

  @Override
  public void exitLogicalOrCondition(EfxParser.LogicalOrConditionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + " " + operators.get("or") + " " + right);
  }

  @Override
  public void exitLogicalNotCondition(EfxParser.LogicalNotConditionContext ctx) {
    String condition = this.stack.pop();
    this.stack.push(operators.get("not") + " " + condition);
  }

  @Override
  public void exitAlwaysCondition(EfxParser.AlwaysConditionContext ctx) {
    this.stack.push("true");
  }

  @Override
  public void exitNeverCondition(EfxParser.NeverConditionContext ctx) {
    this.stack.push("false");
  }

  @Override
  public void exitComparisonCondition(EfxParser.ComparisonConditionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + operators.get(ctx.operator.getText()) + right);
  }

  @Override
  public void exitEmptinessCondition(EfxParser.EmptinessConditionContext ctx) {
    String expression = this.stack.pop();
    String operator = ctx.modifier != null && ctx.modifier.getText() == "not" ? "!=" : "==";
    this.stack.push(expression + " " + operators.get(operator) + " ''");
  }

  @Override
  public void exitPresenceCondition(EfxParser.PresenceConditionContext ctx) {
    String reference = this.stack.pop();
    String operator = ctx.modifier != null && ctx.modifier.getText() == "not" ? "==" : "!=";
    this.stack.push(reference + " " + operators.get(operator) + " ''");
  }

  @Override
  public void exitParenthesizedCondition(EfxParser.ParenthesizedConditionContext ctx) {
    String condition = this.stack.pop();
    this.stack.push('(' + condition + ')');
  }

  @Override
  public void exitExpressionCondition(EfxParser.ExpressionConditionContext ctx) {
    String expression = this.stack.pop();
    this.stack.push(expression);
  }

  @Override
  public void exitLikePatternCondition(EfxParser.LikePatternConditionContext ctx) {
    String expression = this.stack.pop();
    boolean hasNot = ctx.modifier != null && ctx.modifier.getText().equals("not");
    String not = hasNot ? "not(" : "";
    String endNot = hasNot ? ")" : "";
    this.stack.push(not + "fn:matches(" + expression + ", " + ctx.pattern.getText() + ")" + endNot);
  }

  @Override
  public void exitInListCondition(EfxParser.InListConditionContext ctx) {
    String list = this.stack.pop();
    String expression = this.stack.pop();
    boolean hasNot = ctx.modifier != null && ctx.modifier.getText().equals("not");
    String not = hasNot ? "not(" : "";
    String endNot = hasNot ? ")" : "";
    this.stack.push(not + expression + "=" + list + endNot);
  }

  @Override
  public void exitAdditionExpression(EfxParser.AdditionExpressionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + operators.get(ctx.operator.getText()) + right);
  }

  @Override
  public void exitMultiplicationExpression(EfxParser.MultiplicationExpressionContext ctx) {
    String right = this.stack.pop();
    String left = this.stack.pop();
    this.stack.push(left + operators.get(ctx.operator.getText()) + right);
  }

  @Override
  public void exitCodeList(CodeListContext ctx) {
    if (this.stack.empty()) {
      this.stack.push("()");
      return;
    }
  }

  @Override
  public void exitExplicitList(ExplicitListContext ctx) {
    if (this.stack.empty() || ctx.value().size() == 0) {
      this.stack.push("()");
      return;
    }

    String list = this.stack.pop() + ")";
    for (int i = 1; i < ctx.value().size(); i++) {
      list = this.stack.pop() + ", " + list;
    }
    list = "(" + list;
    this.stack.push(list);
  }

  @Override
  public void exitLiteral(EfxParser.LiteralContext ctx) {
    this.stack.push(ctx.getText());
  }

  @Override
  public void exitFunctionCall(EfxParser.FunctionCallContext ctx) {
    this.stack.push(ctx.getText());
  }

  @Override
  public void exitNoticeReference(EfxParser.NoticeReferenceContext ctx) {
    this.stack.push("fn:doc('http://notice.service/" + this.stack.pop() + "')");
  }

  @Override
  public void exitNodeReference(EfxParser.NodeReferenceContext ctx) {
    this.stack
        .push(symbols.getRelativeXpathOfFieldOrNode(ctx.node.getText(), this.efxContext.peek()));
  }

  @Override
  public void exitFieldInNoticeReference(EfxParser.FieldInNoticeReferenceContext ctx) {
    String field = this.stack.pop();
    String notice = this.stack.pop();
    this.stack.push(notice + "/" + field);
  }

  @Override
  public void exitSimpleFieldReference(EfxParser.SimpleFieldReferenceContext ctx) {
    this.stack
        .push(symbols.getRelativeXpathOfFieldOrNode(ctx.field.getText(), this.efxContext.peek()));
  }

  @Override
  public void exitFieldReferenceWithPredicate(EfxParser.FieldReferenceWithPredicateContext ctx) {
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
  public void enterPredicate(EfxParser.PredicateContext ctx) {
    EfxParser.SimpleFieldReferenceContext refCtx =
        ctx.getParent().getChild(EfxParser.SimpleFieldReferenceContext.class, 0);
    this.efxContext
        .push(symbols.getNarrowerContextPathOf(refCtx.field.getText(), this.efxContext.peek()));
  }

  /**
   * After the predicate is parsed we need to switch back to the previous context.
   */
  @Override
  public void exitPredicate(EfxParser.PredicateContext ctx) {
    this.efxContext.pop();
  }

  @Override
  public void enterReferenceWithContextOverride(EfxParser.ReferenceWithContextOverrideContext ctx) {
    this.efxContext
        .push(symbols.getNarrowerContextPathOf(ctx.ctx.getText(), this.efxContext.peek()));
  }

  @Override
  public void exitReferenceWithContextOverride(EfxParser.ReferenceWithContextOverrideContext ctx) {
    this.efxContext.pop();
  }

  @Override
  public void exitSimpleReference(EfxParser.SimpleReferenceContext ctx) {
    String field = this.stack.pop();
    String attribute = ctx.attribute != null ? "/@" + ctx.attribute.getText() : "/text()";
    this.stack.push(field + attribute);
  }

  @Override
  public void exitCodelistReference(CodelistReferenceContext ctx) {
    this.stack.push(this.symbols.getCodelistCodesAsEfxList(ctx.CodelistId().getText()));
  }
}

