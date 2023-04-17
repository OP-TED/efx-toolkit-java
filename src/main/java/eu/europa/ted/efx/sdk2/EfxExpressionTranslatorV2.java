package eu.europa.ted.efx.sdk2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.EfxExpressionTranslator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.CallStack;
import eu.europa.ted.efx.model.CallStackObject;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.ContextStack;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.BooleanExpression;
import eu.europa.ted.efx.model.Expression.BooleanListExpression;
import eu.europa.ted.efx.model.Expression.ContextExpression;
import eu.europa.ted.efx.model.Expression.DateExpression;
import eu.europa.ted.efx.model.Expression.DateListExpression;
import eu.europa.ted.efx.model.Expression.DurationExpression;
import eu.europa.ted.efx.model.Expression.DurationListExpression;
import eu.europa.ted.efx.model.Expression.IteratorExpression;
import eu.europa.ted.efx.model.Expression.IteratorListExpression;
import eu.europa.ted.efx.model.Expression.ListExpression;
import eu.europa.ted.efx.model.Expression.NumericExpression;
import eu.europa.ted.efx.model.Expression.NumericListExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.model.Expression.StringExpression;
import eu.europa.ted.efx.model.Expression.StringListExpression;
import eu.europa.ted.efx.model.Expression.TimeExpression;
import eu.europa.ted.efx.model.Expression.TimeListExpression;
import eu.europa.ted.efx.sdk2.EfxParser.*;
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
 * {@link EfxExpressionTranslatorV1} only focuses on EFX expressions. To translate EFX templates
 * you need to use the {@link EfxTemplateTranslatorV1} which derives from this class.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_EXPRESSION_TRANSLATOR)
public class EfxExpressionTranslatorV2 extends EfxBaseListener
    implements EfxExpressionTranslator {

  private static final String NOT_MODIFIER = EfxLexer.VOCABULARY.getLiteralName(EfxLexer.Not).replaceAll("^'|'$", "");

  private static final String VARIABLE_PREFIX = EfxLexer.VOCABULARY.getLiteralName(EfxLexer.VariablePrefix).replaceAll("^'|'$", "");
  private static final String ATTRIBUTE_PREFIX = EfxLexer.VOCABULARY.getLiteralName(EfxLexer.AttributePrefix).replaceAll("^'|'$", "");
  private static final String CODELIST_PREFIX = EfxLexer.VOCABULARY.getLiteralName(EfxLexer.CodelistPrefix).replaceAll("^'|'$", "");

  private static final String BEGIN_EXPRESSION_BLOCK = "{";
  private static final String END_EXPRESSION_BLOCK = "}";

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

  protected BaseErrorListener errorListener;

  /**
   * The ScriptGenerator is called to determine the target language syntax whenever needed.
   */
  protected ScriptGenerator script;

  private LinkedList<String> expressionParameters = new LinkedList<>();
  
  protected EfxExpressionTranslatorV2() {}

  public EfxExpressionTranslatorV2(final SymbolResolver symbolResolver,
      final ScriptGenerator scriptGenerator, final BaseErrorListener errorListener) {
    this.symbols = symbolResolver;
    this.script = scriptGenerator;
    this.errorListener = errorListener;

    this.efxContext = new ContextStack(symbols);
  }

  @Override
  public String translateExpression(final String expression, final String... parameters) {
    this.expressionParameters.addAll(Arrays.asList(parameters));

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

  private <T extends Expression> T translateParameter(final String parameterValue, final Class<T> parameterType) {
    final EfxExpressionTranslatorV2 translator = new EfxExpressionTranslatorV2(this.symbols, this.script,
        this.errorListener);

    final EfxLexer lexer =
        new EfxLexer(CharStreams.fromString(BEGIN_EXPRESSION_BLOCK + parameterValue + END_EXPRESSION_BLOCK));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);

    if (errorListener != null) {
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }

      final ParseTree tree = parser.parameterValue();
      final ParseTreeWalker walker = new ParseTreeWalker();

      walker.walk(translator, tree);

      return Expression.instantiate(translator.getTranslatedScript(), parameterType);
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
      return ((AbsoluteFieldReferenceContext) ctx).reference.reference.simpleFieldReference().FieldId()
          .getText();
    }

    if (ctx instanceof FieldReferenceWithFieldContextOverrideContext) {
      return ((FieldReferenceWithFieldContextOverrideContext) ctx).reference.reference.simpleFieldReference()
          .FieldId().getText();
    }

    if (ctx instanceof FieldReferenceWithNodeContextOverrideContext) {
      return ((FieldReferenceWithNodeContextOverrideContext) ctx).reference.reference
          .reference.simpleFieldReference().FieldId().getText();
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
  public void exitUniqueValueCondition(EfxParser.UniqueValueConditionContext ctx) {
    PathExpression haystack = this.stack.pop(PathExpression.class);
    PathExpression needle = this.stack.pop(PathExpression.class);

    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      this.stack.push(
          this.script.composeLogicalNot(this.script.composeUniqueValueCondition(needle, haystack)));
    } else {
      this.stack.push(this.script.composeUniqueValueCondition(needle, haystack));
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

  /*** Boolean expressions - List membership conditions ***/

  @Override
  public void exitStringInListCondition(EfxParser.StringInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, StringExpression.class, StringListExpression.class);
  }

  @Override
  public void exitBooleanInListCondition(BooleanInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, BooleanExpression.class, BooleanListExpression.class);
  }

  @Override
  public void exitNumberInListCondition(NumberInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, NumericExpression.class, NumericListExpression.class);
  }

  @Override
  public void exitDateInListCondition(DateInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, DateExpression.class, DateListExpression.class);
  }

  @Override
  public void exitTimeInListCondition(TimeInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, TimeExpression.class, TimeListExpression.class);
  }

  @Override
  public void exitDurationInListCondition(DurationInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, DurationExpression.class, DurationListExpression.class);
  }

  private <T extends Expression, L extends ListExpression<T>> void exitInListCondition(
      Token modifier, Class<T> expressionType, Class<L> listType) {
    ListExpression<T> list = this.stack.pop(listType);
    T expression = this.stack.pop(expressionType);
    BooleanExpression condition = this.script.composeContainsCondition(expression, list);
    if (modifier != null && modifier.getText().equals(NOT_MODIFIER)) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  /*** Quantified expressions ***/

  @Override
  public void enterQuantifiedExpression(QuantifiedExpressionContext ctx) {
    this.stack.pushStackFrame();  // Quantified expressions need their own scope because they introduce new variables.
  }

  @Override
  public void exitQuantifiedExpression(QuantifiedExpressionContext ctx) {
    BooleanExpression booleanExpression = this.stack.pop(BooleanExpression.class);
    if (ctx.Every() != null) {
      this.stack.push(this.script.composeAllSatisfy(this.stack.pop(IteratorListExpression.class),
          booleanExpression));
    } else {
      this.stack.push(this.script.composeAnySatisfies(this.stack.pop(IteratorListExpression.class),
          booleanExpression));
    }
    this.stack.popStackFrame(); // Variables declared in the quantified expression go out of scope here.
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
  public void exitStringList(StringListContext ctx) {
    this.exitList(ctx.stringExpression().size(), StringExpression.class,
        StringListExpression.class);
  }

  @Override
  public void exitBooleanList(BooleanListContext ctx) {
    this.exitList(ctx.booleanExpression().size(), BooleanExpression.class,
        BooleanListExpression.class);
  }

  @Override
  public void exitNumericList(NumericListContext ctx) {
    this.exitList(ctx.numericExpression().size(), NumericExpression.class,
        NumericListExpression.class);
  }

  @Override
  public void exitDateList(DateListContext ctx) {
    this.exitList(ctx.dateExpression().size(), DateExpression.class, DateListExpression.class);
  }

  @Override
  public void exitTimeList(TimeListContext ctx) {
    this.exitList(ctx.timeExpression().size(), TimeExpression.class, TimeListExpression.class);
  }


  @Override
  public void exitDurationList(DurationListContext ctx) {
    this.exitList(ctx.durationExpression().size(), DurationExpression.class,
        DurationListExpression.class);
  }

  private <T extends Expression, L extends ListExpression<T>> void exitList(int listSize,
      Class<T> expressionType, Class<L> listType) {
    if (this.stack.empty() || listSize == 0) {
      this.stack.push(this.script.composeList(Collections.emptyList(), listType));
      return;
    }

    List<T> list = new ArrayList<>();
    for (int i = 0; i < listSize; i++) {
      list.add(0, this.stack.pop(expressionType));
    }
    this.stack.push(this.script.composeList(list, listType));
  }

  /*** Conditional Expressions ***/

  @Override
  public void exitUntypedConditionalExpression(UntypedConditionalExpressionContext ctx) {
    Class<? extends CallStackObject> typeWhenFalse = this.stack.peek().getClass();
    if (typeWhenFalse == BooleanExpression.class) {
      this.exitConditionalBooleanExpression();
    } else if (typeWhenFalse == NumericExpression.class) {
      this.exitConditionalNumericExpression();
    } else if (typeWhenFalse == StringExpression.class) {
      this.exitConditionalStringExpression();
    } else if (typeWhenFalse == DateExpression.class) {
      this.exitConditionalDateExpression();
    } else if (typeWhenFalse == TimeExpression.class) {
      this.exitConditionalTimeExpression();
    } else if (typeWhenFalse == DurationExpression.class) {
      this.exitConditionalDurationExpression();
    } else {
      throw new IllegalStateException("Unknown type " + typeWhenFalse);
    }
  }

  @Override
  public void exitConditionalBooleanExpression(ConditionalBooleanExpressionContext ctx) {
    this.exitConditionalBooleanExpression();
  }

  @Override
  public void exitConditionalNumericExpression(ConditionalNumericExpressionContext ctx) {
    this.exitConditionalNumericExpression();
  }

  @Override
  public void exitConditionalStringExpression(ConditionalStringExpressionContext ctx) {
    this.exitConditionalStringExpression();
  }

  @Override
  public void exitConditionalDateExpression(ConditionalDateExpressionContext ctx) {
    this.exitConditionalDateExpression();
  }

  @Override
  public void exitConditionalTimeExpression(ConditionalTimeExpressionContext ctx) {
    this.exitConditionalTimeExpression();
  }

  @Override
  public void exitConditionalDurationExpression(ConditionalDurationExpressionContext ctx) {
    this.exitConditionalDurationExpression();
  }

  private void exitConditionalBooleanExpression() {
    BooleanExpression whenFalse = this.stack.pop(BooleanExpression.class);
    BooleanExpression whenTrue = this.stack.pop(BooleanExpression.class);
    BooleanExpression condition = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse,
        BooleanExpression.class));
  }

  private void exitConditionalNumericExpression() {
    NumericExpression whenFalse = this.stack.pop(NumericExpression.class);
    NumericExpression whenTrue = this.stack.pop(NumericExpression.class);
    BooleanExpression condition = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse,
        NumericExpression.class));
  }

  private void exitConditionalStringExpression() {
    StringExpression whenFalse = this.stack.pop(StringExpression.class);
    StringExpression whenTrue = this.stack.pop(StringExpression.class);
    BooleanExpression condition = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse,
        StringExpression.class));
  }

  private void exitConditionalDateExpression() {
    DateExpression whenFalse = this.stack.pop(DateExpression.class);
    DateExpression whenTrue = this.stack.pop(DateExpression.class);
    BooleanExpression condition = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse,
        DateExpression.class));
  }

  private void exitConditionalTimeExpression() {
    TimeExpression whenFalse = this.stack.pop(TimeExpression.class);
    TimeExpression whenTrue = this.stack.pop(TimeExpression.class);
    BooleanExpression condition = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse,
        TimeExpression.class));
  }

  private void exitConditionalDurationExpression() {
    DurationExpression whenFalse = this.stack.pop(DurationExpression.class);
    DurationExpression whenTrue = this.stack.pop(DurationExpression.class);
    BooleanExpression condition = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse,
        DurationExpression.class));
  }

  /*** Iterators ***/

  @Override
  public void exitStringIteratorExpression(StringIteratorExpressionContext ctx) {
    this.exitIteratorExpression(StringExpression.class, StringListExpression.class);
  }

  @Override
  public void exitBooleanIteratorExpression(BooleanIteratorExpressionContext ctx) {
    this.exitIteratorExpression(BooleanExpression.class, BooleanListExpression.class);
  }

  @Override
  public void exitNumericIteratorExpression(NumericIteratorExpressionContext ctx) {
    this.exitIteratorExpression(NumericExpression.class, NumericListExpression.class);
  }

  @Override
  public void exitDateIteratorExpression(DateIteratorExpressionContext ctx) {
    this.exitIteratorExpression(DateExpression.class, DateListExpression.class);
  }

  @Override
  public void exitTimeIteratorExpression(TimeIteratorExpressionContext ctx) {
    this.exitIteratorExpression(TimeExpression.class, TimeListExpression.class);
  }
  
  @Override
  public void exitDurationIteratorExpression(DurationIteratorExpressionContext ctx) {
    this.exitIteratorExpression(DurationExpression.class, DurationListExpression.class);
  }

  @Override
  public void exitContextIteratorExpression(ContextIteratorExpressionContext ctx) {
    PathExpression path = this.stack.pop(PathExpression.class);
    ContextExpression variable = this.stack.pop(ContextExpression.class);
    this.stack.push(this.script.composeIteratorExpression(variable.script, path));
    if (ctx.fieldContext() != null) {
      final String contextFieldId =
          getFieldIdFromChildSimpleFieldReferenceContext(ctx.fieldContext());
      this.efxContext.declareContextVariable(variable.script,
          new FieldContext(contextFieldId, this.symbols.getAbsolutePathOfField(contextFieldId),
              this.symbols.getRelativePathOfField(contextFieldId, this.efxContext.absolutePath())));
    } else if (ctx.nodeContext() != null) {
      final String contextNodeId =
          getNodeIdFromChildSimpleNodeReferenceContext(ctx.nodeContext());
      this.efxContext.declareContextVariable(variable.script,
          new NodeContext(contextNodeId, this.symbols.getAbsolutePathOfNode(contextNodeId),
              this.symbols.getRelativePathOfNode(contextNodeId, this.efxContext.absolutePath())));
    }
  }

  @Override
  public void exitIteratorList(IteratorListContext ctx) {
    List<IteratorExpression> iterators = new ArrayList<>();
    for (int i = 0; i < ctx.iteratorExpression().size(); i++) {
      iterators.add(0, this.stack.pop(IteratorExpression.class));
    }
    this.stack.push(this.script.composeIteratorList(iterators));
 }

  @Override
  public void exitParenthesizedStringsFromIteration(ParenthesizedStringsFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(StringListExpression.class), StringListExpression.class));
  }

  @Override
  public void exitParenthesizedNumbersFromIteration(ParenthesizedNumbersFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(NumericListExpression.class), NumericListExpression.class));
  }

  @Override
  public void exitParenthesizedBooleansFromIteration(
      ParenthesizedBooleansFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(BooleanListExpression.class), BooleanListExpression.class));
  }

  @Override
  public void exitParenthesizedDatesFromIteration(ParenthesizedDatesFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(DateListExpression.class), DateListExpression.class));
  }

  @Override
  public void exitParenthesizedTimesFromIteration(ParenthesizedTimesFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(TimeListExpression.class), TimeListExpression.class));
  }

  @Override
  public void exitParenthesizedDurationsFromIteration(
      ParenthesizedDurationsFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(DurationListExpression.class), DurationListExpression.class));
  }

  @Override
  public void enterStringSequenceFromIteration(StringSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitStringSequenceFromIteration(StringSequenceFromIterationContext ctx) {
    this.exitIterationExpression(StringExpression.class, StringListExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterNumericSequenceFromIteration(NumericSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitNumericSequenceFromIteration(NumericSequenceFromIterationContext ctx) {
    this.exitIterationExpression(NumericExpression.class, NumericListExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterBooleanSequenceFromIteration(BooleanSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitBooleanSequenceFromIteration(BooleanSequenceFromIterationContext ctx) {
    this.exitIterationExpression(BooleanExpression.class, BooleanListExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterDateSequenceFromIteration(DateSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitDateSequenceFromIteration(DateSequenceFromIterationContext ctx) {
    this.exitIterationExpression(DateExpression.class, DateListExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterTimeSequenceFromIteration(TimeSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitTimeSequenceFromIteration(TimeSequenceFromIterationContext ctx) {
    this.exitIterationExpression(TimeExpression.class, TimeListExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterDurationSequenceFromIteration(DurationSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitDurationSequenceFromIteration(DurationSequenceFromIterationContext ctx) {
    this.exitIterationExpression(DurationExpression.class, DurationListExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  public <T extends Expression, L extends ListExpression<T>> void exitIteratorExpression(
      Class<T> variableType, Class<L> listType) {
    L list = this.stack.pop(listType);
    T variable = this.stack.pop(variableType);
    this.stack.push(this.script.composeIteratorExpression(variable.script, list));
  }

  public <T extends Expression, L extends ListExpression<T>> void exitIterationExpression(Class<T> expressionType,
      Class<L> targetListType) {
    T expression = this.stack.pop(expressionType);
    IteratorListExpression iterators = this.stack.pop(IteratorListExpression.class);
    this.stack
        .push(this.script.composeForExpression(iterators, expression, targetListType));
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
   * Any field references in the predicate must be resolved relative to the node or field on which
   * the predicate is applied. Therefore we need to switch to that context while the predicate is
   * being parsed.
   */
  @Override
  public void enterPredicate(EfxParser.PredicateContext ctx) {
    final String nodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx.getParent());
    if (nodeId != null) {
      this.efxContext.pushNodeContext(nodeId);
    } else {
      final String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx.getParent());
      this.efxContext.pushFieldContext(fieldId);
    }
  }

  /**
   * After the predicate is parsed we need to switch back to the previous context.
   */
  @Override
  public void exitPredicate(EfxParser.PredicateContext ctx) {
    this.efxContext.pop();
  }

  @Override
  public void exitFieldReferenceWithAxis(FieldReferenceWithAxisContext ctx) {
    if (ctx.axis() != null) {
      this.stack.push(this.script.composeFieldReferenceWithAxis(
          this.stack.pop(PathExpression.class), ctx.axis().Axis().getText(), PathExpression.class));
    }
  }

  /*** External References ***/

  @Override
  public void exitNoticeReference(EfxParser.NoticeReferenceContext ctx) {
    this.stack.push(this.script.composeExternalReference(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void enterFieldReferenceInOtherNotice(FieldReferenceInOtherNoticeContext ctx) {
    if (ctx.noticeReference() != null) {
      // We push a null context as we switch to an external notice and we need XPaths to be absolute
      this.efxContext.push(null);
    }
  }

  @Override
  public void exitFieldReferenceInOtherNotice(EfxParser.FieldReferenceInOtherNoticeContext ctx) {
    if (ctx.noticeReference() != null) {
      PathExpression field = this.stack.pop(PathExpression.class);
      PathExpression notice = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.composeFieldInExternalReference(notice, field));
      
      // Finally, pop the null context we pushed during enterFieldReferenceInOtherNotice 
      this.efxContext.pop();  
    }
  }

  /*** Value References ***/

  @Override
  public void exitScalarFromFieldReference(ScalarFromFieldReferenceContext ctx) {

    PathExpression path = this.stack.pop(PathExpression.class);
    String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
    // TODO: Use an interface for locating attributes. A PathExpression is not necessarily an
    // XPath in every implementation.
    XPathAttributeLocator parsedPath = XPathAttributeLocator.findAttribute(path);

    if (parsedPath.hasAttribute()) {
      this.stack.push(this.script.composeFieldAttributeReference(parsedPath.getPath(),
          parsedPath.getAttribute(), Expression.types.get(this.symbols.getTypeOfField(fieldId))));
    } else if (fieldId != null) {
      this.stack.push(this.script.composeFieldValueReference(path,
          Expression.types.get(this.symbols.getTypeOfField(fieldId))));
    } else {
      this.stack.push(this.script.composeFieldValueReference(path, PathExpression.class));
    }
  }

  @Override
  public void exitSequenceFromFieldReference(SequenceFromFieldReferenceContext ctx) {
    PathExpression path = this.stack.pop(PathExpression.class);
    String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
    // TODO: Use an interface for locating attributes. A PathExpression is not necessarily an
    // XPath in every implementation.
    XPathAttributeLocator parsedPath = XPathAttributeLocator.findAttribute(path);

    if (parsedPath.hasAttribute()) {
      this.stack.push(this.script.composeFieldAttributeReference(parsedPath.getPath(),
          parsedPath.getAttribute(),
          Expression.listTypes.get(this.symbols.getTypeOfField(fieldId))));
    } else if (fieldId != null) {
      this.stack.push(this.script.composeFieldValueReference(path,
          Expression.listTypes.get(this.symbols.getTypeOfField(fieldId))));
    } else {
      this.stack.push(this.script.composeFieldValueReference(path, PathExpression.class));
    }
  }

  @Override
  public void exitScalarFromAttributeReference(ScalarFromAttributeReferenceContext ctx) {
    this.stack.push(this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
        this.getAttributeName(ctx), StringExpression.class));
  }

  /*** References with context override ***/

  /**
   * Handles expressions of the form ContextField::ReferencedField. Changes the context before the
   * reference is resolved.
   */
  @Override
  public void exitContextFieldSpecifier(ContextFieldSpecifierContext ctx) {
    this.stack.pop(PathExpression.class); // Discard the PathExpression placed in the stack for
                                          // the context field.
    final String contextFieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx.field);
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
    if (ctx.contextFieldSpecifier() != null) {
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
  public void exitContextNodeSpecifier(ContextNodeSpecifierContext ctx) {
    this.stack.pop(PathExpression.class); // Discard the PathExpression placed in the stack for
                                          // the context node.
    final String contextNodeId = getNodeIdFromChildSimpleNodeReferenceContext(ctx.node);
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
    if (ctx.contextNodeSpecifier() != null) {
      final PathExpression field = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.joinPaths(this.efxContext.relativePath(), field));
      this.efxContext.pop(); // Restores the previous context
    }
  }

  @Override
  public void exitContextVariableSpecifier(ContextVariableSpecifierContext ctx) {
    ContextExpression variableName = this.stack.pop(ContextExpression.class);
    Context variableContext = this.efxContext.getContextFromVariable(variableName.script);
    if (variableContext.isFieldContext()) {
      this.efxContext.push(new FieldContext(variableContext.symbol(),
          this.symbols.getAbsolutePathOfField(variableContext.symbol()), this.symbols
              .getRelativePathOfField(variableContext.symbol(), this.efxContext.absolutePath())));
    } else if (variableContext.isNodeContext()) {
      this.efxContext.push(new NodeContext(variableContext.symbol(),
          this.symbols.getAbsolutePathOfNode(variableContext.symbol()), this.symbols
              .getRelativePathOfNode(variableContext.symbol(), this.efxContext.absolutePath())));
    } else {
      throw new IllegalStateException("Variable context is neither a field nor a node context.");
    }
    this.stack.push(variableName);
  }

  @Override
  public void exitFieldReferenceWithVariableContextOverride(
      FieldReferenceWithVariableContextOverrideContext ctx) {
    if (ctx.contextVariableSpecifier() != null) {
      final PathExpression field = this.stack.pop(PathExpression.class);
      final ContextExpression variableName = this.stack.pop(ContextExpression.class);
      this.stack.push(this.script.joinPaths(new PathExpression(variableName.script), field));
      this.efxContext.pop(); // Restores the previous context
    }
  }

  /*** Other References ***/

  @Override
  public void exitCodelistReference(CodelistReferenceContext ctx) {
    this.stack.push(this.script.composeList(this.symbols.expandCodelist(this.getCodelistName(ctx))
        .stream().map(s -> this.script.getStringLiteralFromUnquotedString(s))
        .collect(Collectors.toList()), StringListExpression.class));
  }

  @Override
  public void exitVariableReference(VariableReferenceContext ctx) {
    String variableName = this.getVariableName(ctx);
    this.stack.pushVariableReference(variableName,
        this.script.composeVariableReference(variableName, Expression.class));
  }

  /*** Indexers ***/

  @Override
  public void exitStringAtSequenceIndex(StringAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(StringExpression.class, StringListExpression.class);
  }

  @Override
  public void exitNumericAtSequenceIndex(NumericAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(NumericExpression.class, NumericListExpression.class);
  }

  @Override
  public void exitBooleanAtSequenceIndex(BooleanAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(BooleanExpression.class, BooleanListExpression.class);
  }

  @Override
  public void exitDateAtSequenceIndex(DateAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(DateExpression.class, DateListExpression.class);
  }

  @Override
  public void exitTimeAtSequenceIndex(TimeAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(TimeExpression.class, TimeListExpression.class);
  }

  @Override
  public void exitDurationAtSequenceIndex(DurationAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(DurationExpression.class, DurationListExpression.class);
  }

  private <T extends Expression, L extends ListExpression<T>> void exitSequenceAtIndex(Class<T> itemType, Class<L> listType) {
    NumericExpression index = this.stack.pop(NumericExpression.class);
    L list = this.stack.pop(listType);
    this.stack.push(this.script.composeIndexer(list, index, itemType));
  }

  /*** Parameter Declarations ***/


  @Override
  public void exitStringParameterDeclaration(StringParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(this.getVariableName(ctx), StringExpression.class);
  }

  @Override
  public void exitNumericParameterDeclaration(NumericParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(this.getVariableName(ctx), NumericExpression.class);
  }

  @Override
  public void exitBooleanParameterDeclaration(BooleanParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(this.getVariableName(ctx), BooleanExpression.class);
  }

  @Override
  public void exitDateParameterDeclaration(DateParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(this.getVariableName(ctx), DateExpression.class);
  }

  @Override
  public void exitTimeParameterDeclaration(TimeParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(this.getVariableName(ctx), TimeExpression.class);
  }

  @Override
  public void exitDurationParameterDeclaration(DurationParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(this.getVariableName(ctx), DurationExpression.class);
  }

  private <T extends Expression> void exitParameterDeclaration(String parameterName, Class<T> parameterType) {
    if (this.expressionParameters.isEmpty()) {
      throw new ParseCancellationException("No parameter passed for " + parameterName);
    }

    this.stack.pushParameterDeclaration(parameterName,
        this.script.composeParameterDeclaration(parameterName, parameterType),
        this.translateParameter(this.expressionParameters.pop(), parameterType));
  }

  /*** Variable Declarations ***/

  @Override
  public void exitStringVariableDeclaration(StringVariableDeclarationContext ctx) {
    String variableName = this.getVariableName(ctx);
      this.stack.pushVariableDeclaration(variableName,
          this.script.composeVariableDeclaration(variableName, StringExpression.class));
  }

  @Override
  public void exitBooleanVariableDeclaration(BooleanVariableDeclarationContext ctx) {
    String variableName = this.getVariableName(ctx);
    this.stack.pushVariableDeclaration(variableName,
        this.script.composeVariableDeclaration(variableName, BooleanExpression.class));
  }

  @Override
  public void exitNumericVariableDeclaration(NumericVariableDeclarationContext ctx) {
    String variableName = this.getVariableName(ctx);
    this.stack.pushVariableDeclaration(variableName,
        this.script.composeVariableDeclaration(variableName, NumericExpression.class));
  }

  @Override
  public void exitDateVariableDeclaration(DateVariableDeclarationContext ctx) {
    String variableName = this.getVariableName(ctx);
    this.stack.pushVariableDeclaration(variableName,
        this.script.composeVariableDeclaration(variableName, DateExpression.class));
  }

  @Override
  public void exitTimeVariableDeclaration(TimeVariableDeclarationContext ctx) {
    String variableName = this.getVariableName(ctx);
    this.stack.pushVariableDeclaration(variableName,
        this.script.composeVariableDeclaration(variableName, TimeExpression.class));
  }

  @Override
  public void exitDurationVariableDeclaration(DurationVariableDeclarationContext ctx) {
    String variableName = this.getVariableName(ctx);
    this.stack.pushVariableDeclaration(variableName,
        this.script.composeVariableDeclaration(variableName, DurationExpression.class));
  }

  @Override
  public void exitContextVariableDeclaration(ContextVariableDeclarationContext ctx) {
    String variableName = this.getVariableName(ctx);
    this.stack.pushVariableDeclaration(variableName,
        this.script.composeVariableDeclaration(variableName, ContextExpression.class));
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

  @Override
  public void exitSequenceEqualFunction(SequenceEqualFunctionContext ctx) {
    final ListExpression<?> two = this.stack.pop(ListExpression.class);
    final ListExpression<?> one = this.stack.pop(ListExpression.class);
    this.stack.push(this.script.composeSequenceEqualFunction(one, two));
  }

  /*** Numeric functions ***/

  @Override
  public void exitCountFunction(CountFunctionContext ctx) {
    ListExpression<?> expression = this.stack.pop(ListExpression.class);
    this.stack.push(this.script.composeCountOperation(expression));
  }

  @Override
  public void exitNumberFunction(NumberFunctionContext ctx) {
    this.stack.push(this.script.composeToNumberConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitSumFunction(SumFunctionContext ctx) {
    this.stack.push(this.script.composeSumOperation(this.stack.pop(NumericListExpression.class)));
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
  public void exitStringJoinFunction(StringJoinFunctionContext ctx) {
    final StringExpression separator = this.stack.pop(StringExpression.class);
    final StringListExpression list = this.stack.pop(StringListExpression.class);
    this.stack.push(this.script.composeStringJoin(list, separator));
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

  /*** Duration Functions ***/

  @Override
  public void exitDayTimeDurationFromStringFunction(DayTimeDurationFromStringFunctionContext ctx) {
    this.stack.push(
        this.script.composeToDayTimeDurationConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitYearMonthDurationFromStringFunction(
      YearMonthDurationFromStringFunctionContext ctx) {
    this.stack.push(
        this.script.composeToYearMonthDurationConversion(this.stack.pop(StringExpression.class)));
  }

  /*** Sequence Functions ***/

  @Override
  public void exitDistinctValuesFunction(DistinctValuesFunctionContext ctx) {
    final Class<?> sequenceType = this.stack.peek().getClass();
    if (StringListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(StringListExpression.class);
    } else if (NumericListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(NumericListExpression.class);
    } else if (BooleanListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(BooleanListExpression.class);
    } else if (DateListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(DateListExpression.class);
    } else if (TimeListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(TimeListExpression.class);
    } else if (DurationListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(DurationListExpression.class);
    } else {
      throw new IllegalArgumentException(
          "Unsupported sequence type: " + sequenceType.getSimpleName());
    }
  }

  private <T extends Expression, L extends ListExpression<T>> void exitDistinctValuesFunction(
      Class<L> listType) {
    final L list = this.stack.pop(listType);
    this.stack.push(this.script.composeDistinctValuesFunction(list, listType));
  }

  @Override
  public void exitUnionFunction(UnionFunctionContext ctx) {
    final Class<?> sequenceType = this.stack.peek().getClass();
    if (StringListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(StringListExpression.class);
    } else if (NumericListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(NumericListExpression.class);
    } else if (BooleanListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(BooleanListExpression.class);
    } else if (DateListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(DateListExpression.class);
    } else if (TimeListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(TimeListExpression.class);
    } else if (DurationListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(DurationListExpression.class);
    } else {
      throw new IllegalArgumentException(
          "Unsupported sequence type: " + sequenceType.getSimpleName());
    }
  }

  private <T extends Expression, L extends ListExpression<T>> void exitUnionFunction(
      Class<L> listType) {
    final L two = this.stack.pop(listType);
    final L one = this.stack.pop(listType);
    this.stack.push(this.script.composeUnionFunction(one, two, listType));
  }

  @Override
  public void exitIntersectFunction(IntersectFunctionContext ctx) {
    final Class<?> sequenceType = this.stack.peek().getClass();
    if (StringListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(StringListExpression.class);
    } else if (NumericListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(NumericListExpression.class);
    } else if (BooleanListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(BooleanListExpression.class);
    } else if (DateListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(DateListExpression.class);
    } else if (TimeListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(TimeListExpression.class);
    } else if (DurationListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(DurationListExpression.class);
    } else {
      throw new IllegalArgumentException(
          "Unsupported sequence type: " + sequenceType.getSimpleName());
    }
  }

  private <T extends Expression, L extends ListExpression<T>> void exitIntersectFunction(
      Class<L> listType) {
    final L two = this.stack.pop(listType);
    final L one = this.stack.pop(listType);
    this.stack.push(this.script.composeIntersectFunction(one, two, listType));
  }

  @Override
  public void exitExceptFunction(ExceptFunctionContext ctx) {
    final Class<?> sequenceType = this.stack.peek().getClass();
    if (StringListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(StringListExpression.class);
    } else if (NumericListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(NumericListExpression.class);
    } else if (BooleanListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(BooleanListExpression.class);
    } else if (DateListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(DateListExpression.class);
    } else if (TimeListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(TimeListExpression.class);
    } else if (DurationListExpression.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(DurationListExpression.class);
    } else {
      throw new IllegalArgumentException(
          "Unsupported sequence type: " + sequenceType.getSimpleName());
    }
  }

  private <T extends Expression, L extends ListExpression<T>> void exitExceptFunction(
      Class<L> listType) {
    final L two = this.stack.pop(listType);
    final L one = this.stack.pop(listType);
    this.stack.push(this.script.composeExceptFunction(one, two, listType));
  }

  protected String getCodelistName(String efxCodelistIdentifier) {
    return StringUtils.substringAfter(efxCodelistIdentifier, CODELIST_PREFIX);
  }

  private String getCodelistName(CodelistReferenceContext ctx) {
    return this.getCodelistName(ctx.CodelistId().getText());
  }

  protected String getAttributeName(String efxAttributeIdentifier) {
    return StringUtils.substringAfter(efxAttributeIdentifier, ATTRIBUTE_PREFIX);
  }

  private String getAttributeName(ScalarFromAttributeReferenceContext ctx) {
    return this.getAttributeName(ctx.attributeReference().Attribute().getText());
  }

  protected String getVariableName(String efxVariableIdentifier) {
    return StringUtils.substringAfter(efxVariableIdentifier, VARIABLE_PREFIX);
  }

  private String getVariableName(VariableReferenceContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  protected String getVariableName(ContextVariableDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

    private String getVariableName(StringVariableDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(NumericVariableDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(BooleanVariableDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(DateVariableDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(TimeVariableDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(DurationVariableDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(StringParameterDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(NumericParameterDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(BooleanParameterDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(DateParameterDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(TimeParameterDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }

  private String getVariableName(DurationParameterDeclarationContext ctx) {
    return this.getVariableName(ctx.Variable().getText());
  }
}
