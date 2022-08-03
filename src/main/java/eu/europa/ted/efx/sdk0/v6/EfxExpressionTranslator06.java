package eu.europa.ted.efx.sdk0.v6;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponent;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;
import eu.europa.ted.efx.interfaces.EfxExpressionTranslator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.CallStack;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.ContextStack;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;
import eu.europa.ted.efx.sdk0.v6.EfxParser.AbsoluteFieldReferenceContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.AbsoluteNodeReferenceContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.BooleanComparisonContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.CodeListContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.CodelistReferenceContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.ConcatFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.ContainsFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.CountFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DateComparisonContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DateFromStringFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DateLiteralContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DateMinusMeasureFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DatePlusMeasureFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DateSubtractionExpressionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DurationAdditionExpressionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DurationComparisonContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DurationLeftMultiplicationExpressionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DurationLiteralContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DurationRightMultiplicationExpressionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.DurationSubtractionExpressionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.EndsWithFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.ExplicitListContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.FalseBooleanLiteralContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.FieldContextContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.FieldReferenceWithFieldContextOverrideContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.FieldReferenceWithNodeContextOverrideContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.FieldValueComparisonContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.FormatNumberFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.NodeContextContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.NodeReferenceWithPredicateContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.NotFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.NumberFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.NumericComparisonContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.NumericLiteralContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.ParenthesizedNumericExpressionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.SimpleFieldReferenceContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.SimpleNodeReferenceContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.SingleExpressionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.StartsWithFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.StringComparisonContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.StringLengthFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.StringLiteralContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.SubstringFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.SumFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.TimeComparisonContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.TimeFromStringFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.TimeLiteralContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.ToStringFunctionContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.TrueBooleanLiteralContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.UntypedAttributeValueReferenceContext;
import eu.europa.ted.efx.sdk0.v6.EfxParser.UntypedFieldValueReferenceContext;
import eu.europa.ted.efx.sdk0.v7.EfxLexer;
import eu.europa.ted.efx.xpath.XPathAttributeLocator;

/**
 * The the goal of the EfxExpressionTranslator is to take an EFX expression and translate it to a
 * target scripting language.
 * 
 * The target language syntax is not hardcoded into the translator so that this class can be reused
 * to translate to several different languages. Instead a {@link ScriptGenerator} interface is used
 * to provide specifics on the syntax of the target scripting language.
 * 
 * Apart from writing expressions that can be translated and evaluated in a target scripting
 * language (e.g. XPath/XQuery, JavaScript etc.), EFX also allows the definition of templates that
 * can be translated to a target template markup language (e.g. XSLT, Thymeleaf etc.). The
 * {@link EfxExpressionTranslator06} only focuses on EFX expressions. To translate EFX templates
 * you need to use the {@link EfxTemplateTranslator06} which derives from this class.
 */
@VersionDependentComponent(versions = {"0.6"}, componentType = VersionDependentComponentType.EFX_EXPRESSION_TRANSLATOR)
public class EfxExpressionTranslator06 extends EfxBaseListener
    implements EfxExpressionTranslator {

  private static final String NOT_MODIFIER =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.Not).replaceAll("^'|'$", "");

  /**
   *
   */
  private static final String TYPE_MISMATCH_CANNOT_COMPARE_VALUES_OF_DIFFERENT_TYPES =
      "Type mismatch. Cannot compare values of different types: ";

  /**
   * The stack is used by the methods of this listener to pass data to each other as the parse tree
   * is being walked.
   */
  protected CallStack stack = new CallStack();

  /**
   * The context stack is used to keep track of context switching in nested expressions.
   */
  protected ContextStack efxContext;

  /**
   * Symbols are the field identifiers and node identifiers. The symbols map is used to resolve them
   * to their location in the data source (typically their XPath).
   */
  protected SymbolResolver symbols;

  /**
   * The ScriptGenerator is called to determine the target language syntax whenever needed.
   */
  protected ScriptGenerator script;

  protected BaseErrorListener errorListener;

  protected EfxExpressionTranslator06() {
    throw new UnsupportedOperationException();
  }

  public EfxExpressionTranslator06(final SymbolResolver symbolResolver,
      final ScriptGenerator scriptGenerator, final BaseErrorListener errorListener) {
    this.symbols = symbolResolver;
    this.script = scriptGenerator;
    this.errorListener = errorListener;

    this.efxContext = new ContextStack(symbols);
  }

  @Override
  public String translateExpression(final String expression) {
    final EfxLexer lexer =
        new EfxLexer(CharStreams.fromString(expression));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);

    if (errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }

    final ParseTree tree = parser.singleExpression();
    final ParseTreeWalker walker = new ParseTreeWalker();

    walker.walk(this, tree);

    return getTranslatedScript();
  }

  /**
   * Used to get the translated target language script, after the walker finished its walk.
   *
   * @return The translated code, trimmed
   */
  private String getTranslatedScript() {
    final StringBuilder sb = new StringBuilder(this.stack.size() * 100);
    while (!this.stack.empty()) {
      sb.insert(0, '\n').insert(0, this.stack.pop(Expression.class).script);
    }
    return sb.toString().trim();
  }

  /**
   * Helper method that starts from a given {@link ParserRuleContext} and recursively searches for a
   * {@link SimpleFieldReferenceContext} to locate a field identifier.
   */
  protected static String getFieldIdFromChildSimpleFieldReferenceContext(ParserRuleContext ctx) {

    if (ctx instanceof SimpleFieldReferenceContext) {
      return ((SimpleFieldReferenceContext) ctx).FieldId().getText();
    }

    if (ctx instanceof AbsoluteFieldReferenceContext) {
      return ((AbsoluteFieldReferenceContext) ctx).reference.simpleFieldReference().FieldId()
          .getText();
    }

    if (ctx instanceof FieldReferenceWithFieldContextOverrideContext) {
      return ((FieldReferenceWithFieldContextOverrideContext) ctx).reference.simpleFieldReference()
          .FieldId().getText();
    }

    if (ctx instanceof FieldReferenceWithNodeContextOverrideContext) {
      return ((FieldReferenceWithNodeContextOverrideContext) ctx).reference
          .fieldReferenceWithPredicate().simpleFieldReference().FieldId().getText();
    }

    SimpleFieldReferenceContext fieldReferenceContext =
        ctx.getChild(SimpleFieldReferenceContext.class, 0);
    if (fieldReferenceContext != null) {
      return fieldReferenceContext.FieldId().getText();
    }

    for (ParseTree child : ctx.children) {
      if (child instanceof ParserRuleContext) {
        String fieldId = getFieldIdFromChildSimpleFieldReferenceContext((ParserRuleContext) child);
        if (fieldId != null) {
          return fieldId;
        }
      }
    }

    return null;
  }

  /**
   * Helper method that starts from a given {@link ParserRuleContext} and recursively searches for a
   * {@link SimpleNodeReferenceContext} to locate a node identifier.
   */
  protected static String getNodeIdFromChildSimpleNodeReferenceContext(ParserRuleContext ctx) {

    if (ctx instanceof SimpleNodeReferenceContext) {
      return ((SimpleNodeReferenceContext) ctx).NodeId().getText();
    }

    for (ParseTree child : ctx.children) {
      if (child instanceof ParserRuleContext) {
        String nodeId = getNodeIdFromChildSimpleNodeReferenceContext((ParserRuleContext) child);
        if (nodeId != null) {
          return nodeId;
        }
      }
    }

    return null;
  }

  @Override
  public void enterSingleExpression(SingleExpressionContext ctx) {
    final TerminalNode fieldContext = ctx.FieldId();
    if (fieldContext != null) {
      this.efxContext.pushFieldContext(fieldContext.getText());
    } else {
      final TerminalNode nodeContext = ctx.NodeId();
      if (nodeContext != null) {
        this.efxContext.pushNodeContext(nodeContext.getText());
      }
    }
  }

  @Override
  public void exitSingleExpression(SingleExpressionContext ctx) {
    this.efxContext.pop();
  }

  /*** Boolean expressions ***/

  @Override
  public void exitParenthesizedBooleanExpression(
      EfxParser.ParenthesizedBooleanExpressionContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(BooleanExpression.class), BooleanExpression.class));
  }

  @Override
  public void exitLogicalAndCondition(EfxParser.LogicalAndConditionContext ctx) {
    BooleanExpression right = this.stack.pop(BooleanExpression.class);
    BooleanExpression left = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeLogicalAnd(left, right));
  }

  @Override
  public void exitLogicalOrCondition(EfxParser.LogicalOrConditionContext ctx) {
    BooleanExpression right = this.stack.pop(BooleanExpression.class);
    BooleanExpression left = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeLogicalOr(left, right));
  }

  /*** Boolean expressions - Comparisons ***/

  @Override
  public void exitFieldValueComparison(FieldValueComparisonContext ctx) {
    Expression right = this.stack.pop(Expression.class);
    Expression left = this.stack.pop(Expression.class);
    if (!left.getClass().equals(right.getClass())) {
      throw new ParseCancellationException(TYPE_MISMATCH_CANNOT_COMPARE_VALUES_OF_DIFFERENT_TYPES
          + left.getClass() + " and " + right.getClass());
    }
    this.stack.push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
  }

  @Override
  public void exitStringComparison(StringComparisonContext ctx) {
    StringExpression right = this.stack.pop(StringExpression.class);
    StringExpression left = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
  }

  @Override
  public void exitNumericComparison(NumericComparisonContext ctx) {
    NumericExpression right = this.stack.pop(NumericExpression.class);
    NumericExpression left = this.stack.pop(NumericExpression.class);
    this.stack.push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
  }

  @Override
  public void exitBooleanComparison(BooleanComparisonContext ctx) {
    BooleanExpression right = this.stack.pop(BooleanExpression.class);
    BooleanExpression left = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
  }

  @Override
  public void exitDateComparison(DateComparisonContext ctx) {
    DateExpression right = this.stack.pop(DateExpression.class);
    DateExpression left = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
  }

  @Override
  public void exitTimeComparison(TimeComparisonContext ctx) {
    TimeExpression right = this.stack.pop(TimeExpression.class);
    TimeExpression left = this.stack.pop(TimeExpression.class);
    this.stack.push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
  }

  @Override
  public void exitDurationComparison(DurationComparisonContext ctx) {
    DurationExpression right = this.stack.pop(DurationExpression.class);
    DurationExpression left = this.stack.pop(DurationExpression.class);
    this.stack.push(this.script.composeComparisonOperation(left, ctx.operator.getText(), right));
  }

  /*** Boolean expressions - Conditions ***/

  @Override
  public void exitEmptinessCondition(EfxParser.EmptinessConditionContext ctx) {
    StringExpression expression = this.stack.pop(StringExpression.class);
    String operator =
        ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER) ? "!=" : "==";
    this.stack.push(this.script.composeComparisonOperation(expression, operator,
        this.script.getStringLiteralFromUnquotedString("")));
  }

  @Override
  public void exitPresenceCondition(EfxParser.PresenceConditionContext ctx) {
    PathExpression reference = this.stack.pop(PathExpression.class);
    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      this.stack.push(this.script.composeLogicalNot(this.script.composeExistsCondition(reference)));
    } else {
      this.stack.push(this.script.composeExistsCondition(reference));
    }
  }

  @Override
  public void exitLikePatternCondition(EfxParser.LikePatternConditionContext ctx) {
    StringExpression expression = this.stack.pop(StringExpression.class);

    BooleanExpression condition =
        this.script.composePatternMatchCondition(expression, ctx.pattern.getText());
    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  @Override
  public void exitInListCondition(EfxParser.InListConditionContext ctx) {
    StringListExpression list = this.stack.pop(StringListExpression.class);
    StringExpression expression = this.stack.pop(StringExpression.class);
    BooleanExpression condition = this.script.composeContainsCondition(expression, list);
    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  /*** Numeric expressions ***/

  @Override
  public void exitAdditionExpression(EfxParser.AdditionExpressionContext ctx) {
    NumericExpression right = this.stack.pop(NumericExpression.class);
    NumericExpression left = this.stack.pop(NumericExpression.class);
    this.stack.push(this.script.composeNumericOperation(left, ctx.operator.getText(), right));
  }

  @Override
  public void exitMultiplicationExpression(EfxParser.MultiplicationExpressionContext ctx) {
    NumericExpression right = this.stack.pop(NumericExpression.class);
    NumericExpression left = this.stack.pop(NumericExpression.class);
    this.stack.push(this.script.composeNumericOperation(left, ctx.operator.getText(), right));
  }

  @Override
  public void exitParenthesizedNumericExpression(ParenthesizedNumericExpressionContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(NumericExpression.class), NumericExpression.class));
  }

  /*** Duration Expressions ***/

  @Override
  public void exitDurationAdditionExpression(DurationAdditionExpressionContext ctx) {
    DurationExpression right = this.stack.pop(DurationExpression.class);
    DurationExpression left = this.stack.pop(DurationExpression.class);
    this.stack.push(this.script.composeAddition(left, right));
  }

  @Override
  public void exitDurationSubtractionExpression(DurationSubtractionExpressionContext ctx) {
    DurationExpression right = this.stack.pop(DurationExpression.class);
    DurationExpression left = this.stack.pop(DurationExpression.class);
    this.stack.push(this.script.composeSubtraction(left, right));
  }

  @Override
  public void exitDurationLeftMultiplicationExpression(
      DurationLeftMultiplicationExpressionContext ctx) {
    DurationExpression duration = this.stack.pop(DurationExpression.class);
    NumericExpression number = this.stack.pop(NumericExpression.class);
    this.stack.push(this.script.composeMultiplication(number, duration));
  }

  @Override
  public void exitDurationRightMultiplicationExpression(
      DurationRightMultiplicationExpressionContext ctx) {
    NumericExpression number = this.stack.pop(NumericExpression.class);
    DurationExpression duration = this.stack.pop(DurationExpression.class);
    this.stack.push(this.script.composeMultiplication(number, duration));
  }

  @Override
  public void exitDateSubtractionExpression(DateSubtractionExpressionContext ctx) {
    final DateExpression startDate = this.stack.pop(DateExpression.class);
    final DateExpression endDate = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeSubtraction(startDate, endDate));
  }

  @Override
  public void exitCodeList(CodeListContext ctx) {
    if (this.stack.empty()) {
      this.stack.push(this.script.composeList(Collections.emptyList(), StringListExpression.class));
    }
  }

  @Override
  public void exitExplicitList(ExplicitListContext ctx) {
    if (this.stack.empty() || ctx.expression().size() == 0) {
      this.stack.push(this.script.composeList(Collections.emptyList(), StringListExpression.class));
      return;
    }

    List<StringExpression> list = new ArrayList<>();
    for (int i = 0; i < ctx.expression().size(); i++) {
      list.add(0, this.stack.pop(StringExpression.class));
    }
    this.stack.push(this.script.composeList(list, StringListExpression.class));
  }

  /*** Literals ***/

  @Override
  public void exitNumericLiteral(NumericLiteralContext ctx) {
    this.stack.push(this.script.getNumericLiteralEquivalent(ctx.getText()));
  }

  @Override
  public void exitStringLiteral(StringLiteralContext ctx) {
    this.stack.push(this.script.getStringLiteralEquivalent(ctx.getText()));
  }

  @Override
  public void exitTrueBooleanLiteral(TrueBooleanLiteralContext ctx) {
    this.stack.push(this.script.getBooleanEquivalent(true));
  }

  @Override
  public void exitFalseBooleanLiteral(FalseBooleanLiteralContext ctx) {
    this.stack.push(this.script.getBooleanEquivalent(false));
  }

  @Override
  public void exitDateLiteral(DateLiteralContext ctx) {
    this.stack.push(this.script.getDateLiteralEquivalent(ctx.DATE().getText()));
  }

  @Override
  public void exitTimeLiteral(TimeLiteralContext ctx) {
    this.stack.push(this.script.getTimeLiteralEquivalent(ctx.TIME().getText()));
  }

  @Override
  public void exitDurationLiteral(DurationLiteralContext ctx) {
    this.stack.push(this.script.getDurationLiteralEquivalent(ctx.getText()));
  }

  /*** References ***/

  @Override
  public void exitSimpleNodeReference(SimpleNodeReferenceContext ctx) {
    this.stack.push(
        this.symbols.getRelativePathOfNode(ctx.NodeId().getText(), this.efxContext.absolutePath()));
  }

  @Override
  public void exitSimpleFieldReference(EfxParser.SimpleFieldReferenceContext ctx) {
    this.stack.push(
        symbols.getRelativePathOfField(ctx.FieldId().getText(), this.efxContext.absolutePath()));
  }

  @Override
  public void enterAbsoluteFieldReference(AbsoluteFieldReferenceContext ctx) {
    if (ctx.Slash() != null) {
      this.efxContext.push(null);
    }
  }

  @Override
  public void exitAbsoluteFieldReference(EfxParser.AbsoluteFieldReferenceContext ctx) {
    if (ctx.Slash() != null) {
      this.efxContext.pop();
    }
  }

  @Override
  public void enterAbsoluteNodeReference(EfxParser.AbsoluteNodeReferenceContext ctx) {
    if (ctx.Slash() != null) {
      this.efxContext.push(null);
    }
  }

  @Override
  public void exitAbsoluteNodeReference(AbsoluteNodeReferenceContext ctx) {
    if (ctx.Slash() != null) {
      this.efxContext.pop();
    }
  }


  /*** References with Predicates ***/

  @Override
  public void exitNodeReferenceWithPredicate(NodeReferenceWithPredicateContext ctx) {
    if (ctx.predicate() != null) {
      BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
      PathExpression nodeReference = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.composeNodeReferenceWithPredicate(nodeReference, predicate,
          PathExpression.class));
    }
  }

  @Override
  public void exitFieldReferenceWithPredicate(EfxParser.FieldReferenceWithPredicateContext ctx) {
    if (ctx.predicate() != null) {
      BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
      PathExpression fieldReference = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.composeFieldReferenceWithPredicate(fieldReference, predicate,
          PathExpression.class));
    }
  }

  /**
   * Any field references in the predicate must be resolved relative to the field on which the
   * predicate is applied. Therefore we need to switch to the field's context while the predicate is
   * being parsed.
   */
  @Override
  public void enterPredicate(EfxParser.PredicateContext ctx) {
    final String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx.getParent());
    this.efxContext.pushFieldContext(fieldId);
  }

  /**
   * After the predicate is parsed we need to switch back to the previous context.
   */
  @Override
  public void exitPredicate(EfxParser.PredicateContext ctx) {
    this.efxContext.pop();
  }

  /*** External References ***/

  @Override
  public void exitNoticeReference(EfxParser.NoticeReferenceContext ctx) {
    this.stack.push(this.script.composeExternalReference(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitFieldReferenceInOtherNotice(EfxParser.FieldReferenceInOtherNoticeContext ctx) {
    if (ctx.noticeReference() != null) {
      PathExpression field = this.stack.pop(PathExpression.class);
      PathExpression notice = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.composeFieldInExternalReference(notice, field));
    }
  }

  /*** Value References ***/

  @Override
  public void exitUntypedFieldValueReference(UntypedFieldValueReferenceContext ctx) {

    PathExpression path = this.stack.pop(PathExpression.class);
    String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
    XPathAttributeLocator parsedPath = XPathAttributeLocator.findAttribute(path);

    if (parsedPath.hasAttribute()) {
      this.stack.push(this.script.composeFieldAttributeReference(parsedPath.getPath(),
          parsedPath.getAttribute(), StringExpression.class));
    } else if (fieldId != null) {
      this.stack.push(this.script.composeFieldValueReference(path,
          Expression.types.get(this.symbols.getTypeOfField(fieldId))));
    } else {
      this.stack.push(this.script.composeFieldValueReference(path, PathExpression.class));
    }
  }

  @Override
  public void exitUntypedAttributeValueReference(UntypedAttributeValueReferenceContext ctx) {
    this.stack.push(this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
        ctx.Identifier().getText(), StringExpression.class));
  }

  /*** References with context override ***/

  /**
   * Handles expressions of the form ContextField::ReferencedField. Changes the context before the
   * reference is resolved.
   */
  @Override
  public void exitFieldContext(FieldContextContext ctx) {
    this.stack.pop(PathExpression.class); // Discard the PathExpression placed in the stack for
                                          // the context field.
    final String contextFieldId = ctx.context.reference.simpleFieldReference().FieldId().getText();
    this.efxContext
        .push(new FieldContext(contextFieldId, this.symbols.getAbsolutePathOfField(contextFieldId),
            this.symbols.getRelativePathOfField(contextFieldId, this.efxContext.absolutePath())));
  }


  /**
   * Handles expressions of the form ContextField::ReferencedField. Changes the context before the
   * reference is resolved.
   */
  @Override
  public void exitFieldReferenceWithFieldContextOverride(
      FieldReferenceWithFieldContextOverrideContext ctx) {
    if (ctx.context != null) {
      final PathExpression field = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.joinPaths(this.efxContext.relativePath(), field));
      this.efxContext.pop(); // Restores the previous context
    }
  }

  /**
   * Handles expressions of the form ContextNode::ReferencedField. Changes the context before the
   * reference is resolved.
   */
  @Override
  public void exitNodeContext(NodeContextContext ctx) {
    this.stack.pop(PathExpression.class); // Discard the PathExpression placed in the stack for
                                          // the context node.
    final String contextNodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx.context);
    this.efxContext
        .push(new NodeContext(contextNodeId, this.symbols.getAbsolutePathOfNode(contextNodeId),
            this.symbols.getRelativePathOfNode(contextNodeId, this.efxContext.absolutePath())));
  }

  /**
   * Handles expressions of the form ContextNode::ReferencedField. Restores the context after the
   * reference is resolved.
   */
  @Override
  public void exitFieldReferenceWithNodeContextOverride(
      FieldReferenceWithNodeContextOverrideContext ctx) {
    if (ctx.context != null) {
      final PathExpression field = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.joinPaths(this.efxContext.relativePath(), field));
      this.efxContext.pop(); // Restores the previous context
    }
  }

  /*** Other References ***/

  @Override
  public void exitCodelistReference(CodelistReferenceContext ctx) {
    this.stack.push(this.script.composeList(this.symbols.expandCodelist(ctx.codeListId.getText())
        .stream().map(s -> this.script.getStringLiteralFromUnquotedString(s))
        .collect(Collectors.toList()), StringListExpression.class));
  }

  /*** Boolean functions ***/

  @Override
  public void exitNotFunction(NotFunctionContext ctx) {
    this.stack.push(this.script.composeLogicalNot(this.stack.pop(BooleanExpression.class)));
  }

  @Override
  public void exitContainsFunction(ContainsFunctionContext ctx) {
    final StringExpression needle = this.stack.pop(StringExpression.class);
    final StringExpression haystack = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeContainsCondition(haystack, needle));
  }

  @Override
  public void exitStartsWithFunction(StartsWithFunctionContext ctx) {
    final StringExpression startsWith = this.stack.pop(StringExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeStartsWithCondition(text, startsWith));
  }

  @Override
  public void exitEndsWithFunction(EndsWithFunctionContext ctx) {
    final StringExpression endsWith = this.stack.pop(StringExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeEndsWithCondition(text, endsWith));
  }

  /*** Numeric functions ***/

  @Override
  public void exitCountFunction(CountFunctionContext ctx) {
    this.stack.push(this.script.composeCountOperation(this.stack.pop(PathExpression.class)));
  }

  @Override
  public void exitNumberFunction(NumberFunctionContext ctx) {
    this.stack.push(this.script.composeToNumberConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitSumFunction(SumFunctionContext ctx) {
    this.stack.push(this.script.composeSumOperation(this.stack.pop(PathExpression.class)));
  }

  @Override
  public void exitStringLengthFunction(StringLengthFunctionContext ctx) {
    this.stack
        .push(this.script.composeStringLengthCalculation(this.stack.pop(StringExpression.class)));
  }

  /*** String functions ***/

  @Override
  public void exitSubstringFunction(SubstringFunctionContext ctx) {
    final NumericExpression length =
        ctx.length != null ? this.stack.pop(NumericExpression.class) : null;
    final NumericExpression start = this.stack.pop(NumericExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    if (length != null) {
      this.stack.push(this.script.composeSubstringExtraction(text, start, length));
    } else {
      this.stack.push(this.script.composeSubstringExtraction(text, start));
    }
  }

  @Override
  public void exitToStringFunction(ToStringFunctionContext ctx) {
    this.stack.push(this.script.composeToStringConversion(this.stack.pop(NumericExpression.class)));
  }

  @Override
  public void exitConcatFunction(ConcatFunctionContext ctx) {
    if (this.stack.empty() || ctx.stringExpression().size() == 0) {
      this.stack.push(this.script.composeStringConcatenation(Collections.emptyList()));
      return;
    }

    List<StringExpression> list = new ArrayList<>();
    for (int i = 0; i < ctx.stringExpression().size(); i++) {
      list.add(0, this.stack.pop(StringExpression.class));
    }
    this.stack.push(this.script.composeStringConcatenation(list));
  }

  @Override
  public void exitFormatNumberFunction(FormatNumberFunctionContext ctx) {
    final StringExpression format = this.stack.pop(StringExpression.class);
    final NumericExpression number = this.stack.pop(NumericExpression.class);
    this.stack.push(this.script.composeNumberFormatting(number, format));
  }

  /*** Date functions ***/

  @Override
  public void exitDateFromStringFunction(DateFromStringFunctionContext ctx) {
    this.stack.push(this.script.composeToDateConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitDatePlusMeasureFunction(DatePlusMeasureFunctionContext ctx) {
    DurationExpression right = this.stack.pop(DurationExpression.class);
    DateExpression left = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeAddition(left, right));
  }

  @Override
  public void exitDateMinusMeasureFunction(DateMinusMeasureFunctionContext ctx) {
    DurationExpression right = this.stack.pop(DurationExpression.class);
    DateExpression left = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeSubtraction(left, right));
  }

  /*** Time functions ***/

  @Override
  public void exitTimeFromStringFunction(TimeFromStringFunctionContext ctx) {
    this.stack.push(this.script.composeToTimeConversion(this.stack.pop(StringExpression.class)));
  }
}
