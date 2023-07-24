package eu.europa.ted.efx.sdk2;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
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
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.ContextStack;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
import eu.europa.ted.efx.model.expressions.path.MultilingualStringPathExpression;
import eu.europa.ted.efx.model.expressions.path.NodePathExpression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.expressions.path.StringPathExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.model.variables.Parameter;
import eu.europa.ted.efx.model.variables.Variable;
import eu.europa.ted.efx.sdk2.EfxParser.*;

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
 * {@link EfxExpressionTranslatorV2} only focuses on EFX expressions. To translate EFX templates you
 * need to use the {@link EfxTemplateTranslatorV2} which derives from this class.
 */
@SdkComponent(versions = {"2"}, componentType = SdkComponentType.EFX_EXPRESSION_TRANSLATOR)
public class EfxExpressionTranslatorV2 extends EfxBaseListener
    implements EfxExpressionTranslator {

  private static final String NOT_MODIFIER =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.Not).replaceAll("^'|'$", "");

  private static final String VARIABLE_PREFIX =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.VariablePrefix).replaceAll("^'|'$", "");
  private static final String ATTRIBUTE_PREFIX =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.AttributePrefix).replaceAll("^'|'$", "");
  private static final String CODELIST_PREFIX =
      EfxLexer.VOCABULARY.getLiteralName(EfxLexer.CodelistPrefix).replaceAll("^'|'$", "");

  private static final String BEGIN_EXPRESSION_BLOCK = "{";
  private static final String END_EXPRESSION_BLOCK = "}";

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

    // New in EFX-2: expression preprocessing
    final ExpressionPreprocessor preprocessor = this.new ExpressionPreprocessor(expression);
    final String preprocessedExpression = preprocessor.processExpression();

    // Now parse the preprocessed expression
    final EfxLexer lexer =
        new EfxLexer(CharStreams.fromString(preprocessedExpression));
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

  private <T extends Expression> T translateParameter(final String parameterValue,
      final Class<T> parameterType) {
    final EfxExpressionTranslatorV2 translator =
        new EfxExpressionTranslatorV2(this.symbols, this.script,
            this.errorListener);

    final EfxLexer lexer =
        new EfxLexer(
            CharStreams.fromString(BEGIN_EXPRESSION_BLOCK + parameterValue + END_EXPRESSION_BLOCK));
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
      sb.insert(0, '\n').insert(0, this.stack.pop(Expression.class).getScript());
    }
    return sb.toString().trim();
  }

  /**
   * Helper method that starts from a given {@link ParserRuleContext} and recursively searches for a
   * {@link SimpleFieldReferenceContext} to locate a field identifier.
   * 
   * @param ctx The context to start from.
   * @return The field identifier, or null if none was found.
   */
  protected static String getFieldIdFromChildSimpleFieldReferenceContext(ParserRuleContext ctx) {

    if (ctx instanceof SimpleFieldReferenceContext) {
      return ((SimpleFieldReferenceContext) ctx).FieldId().getText();
    }

    if (ctx instanceof AbsoluteFieldReferenceContext) {
      return ((AbsoluteFieldReferenceContext) ctx).reference.reference.simpleFieldReference()
          .FieldId()
          .getText();
    }

    if (ctx instanceof FieldReferenceWithFieldContextOverrideContext) {
      return ((FieldReferenceWithFieldContextOverrideContext) ctx).reference.reference
          .simpleFieldReference()
          .FieldId().getText();
    }

    if (ctx instanceof FieldReferenceWithNodeContextOverrideContext) {
      return ((FieldReferenceWithNodeContextOverrideContext) ctx).reference.reference.reference
          .simpleFieldReference().FieldId().getText();
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
   * 
   * @param ctx The context to start from.
   * @return The node identifier, or null if none was found.
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

  // #region Boolean expressions ----------------------------------------------

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

  // #region Boolean expressions - Comparisons --------------------------------

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

  // #endregion Boolean expressions - Comparisons -----------------------------

  // #region Boolean expressions - Conditions --------------------------------

  @Override
  public void exitEmptinessCondition(EfxParser.EmptinessConditionContext ctx) {
    StringExpression expression = this.stack.pop(StringExpression.class);
    String operator = ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER) ? "!=" : "==";
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
    PathExpression needle = this.stack.pop(haystack.getClass());

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
    BooleanExpression condition = this.script.composePatternMatchCondition(expression, ctx.pattern.getText());
    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  // #endregion Boolean expressions - Conditions ------------------------------

  // #region Boolean expressions - List membership conditions -----------------

  @Override
  public void exitStringInListCondition(EfxParser.StringInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanInListCondition(BooleanInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumberInListCondition(NumberInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitDateInListCondition(DateInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeInListCondition(TimeInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationInListCondition(DurationInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, DurationExpression.class, DurationSequenceExpression.class);
  }

  private void exitInListCondition(
      Token modifier, Class<? extends ScalarExpression> expressionType, Class<? extends SequenceExpression> listType) {
    SequenceExpression list = this.stack.pop(listType);
    ScalarExpression expression = this.stack.pop(expressionType);
    BooleanExpression condition = this.script.composeContainsCondition(expression, list);
    if (modifier != null && modifier.getText().equals(NOT_MODIFIER)) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  // #endregion Boolean expressions - List membership conditions -----------------

  // #endregion Boolean expressions -------------------------------------------

  // #region Quantified expressions -------------------------------------------

  @Override
  public void enterQuantifiedExpression(QuantifiedExpressionContext ctx) {
    this.stack.pushStackFrame(); // Quantified expressions need their own scope because they
                                 // introduce new variables.
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
    this.stack.popStackFrame(); // Variables declared in the quantified expression go out of scope
                                // here.
  }

  // #endregion Quantified expressions ----------------------------------------

  // #region Numeric expressions ----------------------------------------------

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

  // #endregion Numeric expressions -------------------------------------------

  // #region Duration Expressions ---------------------------------------------

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
      this.stack.push(this.script.composeList(Collections.emptyList(), StringSequenceExpression.class));
    }
  }

  @Override
  public void exitStringList(StringListContext ctx) {
    this.exitList(ctx.stringExpression().size(), StringExpression.class,
        StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanList(BooleanListContext ctx) {
    this.exitList(ctx.booleanExpression().size(), BooleanExpression.class,
        BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericList(NumericListContext ctx) {
    this.exitList(ctx.numericExpression().size(), NumericExpression.class,
        NumericSequenceExpression.class);
  }

  @Override
  public void exitDateList(DateListContext ctx) {
    this.exitList(ctx.dateExpression().size(), DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeList(TimeListContext ctx) {
    this.exitList(ctx.timeExpression().size(), TimeExpression.class, TimeSequenceExpression.class);
  }


  @Override
  public void exitDurationList(DurationListContext ctx) {
    this.exitList(ctx.durationExpression().size(), DurationExpression.class,
        DurationSequenceExpression.class);
  }

  private <T extends ScalarExpression> void exitList(int listSize,
      Class<T> expressionType, Class<? extends SequenceExpression> listType) {
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

  // #endregion Duration Expressions ------------------------------------------

  // #region Conditional Expressions ------------------------------------------

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

  // #endregion Conditional Expressions ---------------------------------------

  // #region Iterators --------------------------------------------------------

  @Override
  public void exitStringIteratorExpression(StringIteratorExpressionContext ctx) {
    this.exitIteratorExpression(getVariableName(ctx.stringVariableDeclaration()), StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanIteratorExpression(BooleanIteratorExpressionContext ctx) {
    this.exitIteratorExpression(getVariableName(ctx.booleanVariableDeclaration()), BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericIteratorExpression(NumericIteratorExpressionContext ctx) {
    this.exitIteratorExpression(getVariableName(ctx.numericVariableDeclaration()),  NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitDateIteratorExpression(DateIteratorExpressionContext ctx) {
    this.exitIteratorExpression(getVariableName(ctx.dateVariableDeclaration()), DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeIteratorExpression(TimeIteratorExpressionContext ctx) {
    this.exitIteratorExpression(getVariableName(ctx.timeVariableDeclaration()), TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationIteratorExpression(DurationIteratorExpressionContext ctx) {
    this.exitIteratorExpression(getVariableName(ctx.durationVariableDeclaration()), DurationExpression.class, DurationSequenceExpression.class);
  }

  @Override
  public void exitContextIteratorExpression(ContextIteratorExpressionContext ctx) {
    PathExpression path = this.stack.pop(PathExpression.class);

    var variableType = path.getClass();
    var variableName = getVariableName(ctx.contextVariableDeclaration());
    Variable variable = new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, variableType),
        Expression.empty(variableType),
        this.script.composeVariableReference(variableName, variableType));
    this.stack.declareIdentifier(variable);

    this.stack.push(this.script.composeIteratorExpression(variable.declarationExpression, path));
    if (ctx.fieldContext() != null) {
      final String contextFieldId =
          getFieldIdFromChildSimpleFieldReferenceContext(ctx.fieldContext());
      this.efxContext.declareContextVariable(variable.name,
          new FieldContext(contextFieldId, this.symbols.getAbsolutePathOfField(contextFieldId),
              this.symbols.getRelativePathOfField(contextFieldId, this.efxContext.absolutePath())));
    } else if (ctx.nodeContext() != null) {
      final String contextNodeId =
          getNodeIdFromChildSimpleNodeReferenceContext(ctx.nodeContext());
      this.efxContext.declareContextVariable(variable.name,
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
        this.stack.pop(StringSequenceExpression.class), StringSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedNumbersFromIteration(ParenthesizedNumbersFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(NumericSequenceExpression.class), NumericSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedBooleansFromIteration(
      ParenthesizedBooleansFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(BooleanSequenceExpression.class), BooleanSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedDatesFromIteration(ParenthesizedDatesFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(DateSequenceExpression.class), DateSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedTimesFromIteration(ParenthesizedTimesFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(TimeSequenceExpression.class), TimeSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedDurationsFromIteration(
      ParenthesizedDurationsFromIterationContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(DurationSequenceExpression.class), DurationSequenceExpression.class));
  }

  @Override
  public void enterStringSequenceFromIteration(StringSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitStringSequenceFromIteration(StringSequenceFromIterationContext ctx) {
    this.exitIterationExpression(StringExpression.class, StringSequenceExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterNumericSequenceFromIteration(NumericSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitNumericSequenceFromIteration(NumericSequenceFromIterationContext ctx) {
    this.exitIterationExpression(NumericExpression.class, NumericSequenceExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterBooleanSequenceFromIteration(BooleanSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitBooleanSequenceFromIteration(BooleanSequenceFromIterationContext ctx) {
    this.exitIterationExpression(BooleanExpression.class, BooleanSequenceExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterDateSequenceFromIteration(DateSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitDateSequenceFromIteration(DateSequenceFromIterationContext ctx) {
    this.exitIterationExpression(DateExpression.class, DateSequenceExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterTimeSequenceFromIteration(TimeSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitTimeSequenceFromIteration(TimeSequenceFromIterationContext ctx) {
    this.exitIterationExpression(TimeExpression.class, TimeSequenceExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterDurationSequenceFromIteration(DurationSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitDurationSequenceFromIteration(DurationSequenceFromIterationContext ctx) {
    this.exitIterationExpression(DurationExpression.class, DurationSequenceExpression.class);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  public <T1 extends ScalarExpression, T2 extends SequenceExpression> void exitIteratorExpression(String variableName,
      Class<T1> variableType, Class<T2> listType) {
    Expression declarationExpression = this.script.composeVariableDeclaration(variableName, variableType);
    SequenceExpression initialisationExpression = this.stack.pop(listType);
    ScalarExpression referenceExpression = this.script.composeVariableReference(variableName, variableType);
    Variable variable = new Variable(variableName, declarationExpression, initialisationExpression,
        referenceExpression);
    this.stack.declareIdentifier(variable);
    this.stack.push(this.script.composeIteratorExpression(variable.declarationExpression, initialisationExpression));
  }

  public <T extends ScalarExpression> void exitIterationExpression(
      Class<T> expressionType,
      Class<? extends SequenceExpression> targetListType) {
    T expression = this.stack.pop(expressionType);
    IteratorListExpression iterators = this.stack.pop(IteratorListExpression.class);
    this.stack
        .push(this.script.composeForExpression(iterators, expression, targetListType));
  }

  // #endregion Iterators -----------------------------------------------------

  // #region Literals ---------------------------------------------------------

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

  // #endregion Literals ------------------------------------------------------

  // #region References -------------------------------------------------------

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


  // #region References with Predicates ---------------------------------------

  @Override
  public void exitNodeReferenceWithPredicate(NodeReferenceWithPredicateContext ctx) {
    if (ctx.predicate() != null) {
      BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
      PathExpression nodeReference = this.stack.pop(NodePathExpression.class);
      this.stack.push(this.script.composeNodeReferenceWithPredicate(nodeReference, predicate));
    }
  }

  @Override
  public void exitFieldReferenceWithPredicate(EfxParser.FieldReferenceWithPredicateContext ctx) {
    if (ctx.predicate() != null) {
      BooleanExpression predicate = this.stack.pop(BooleanExpression.class);
      PathExpression fieldReference = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.composeFieldReferenceWithPredicate(fieldReference, predicate));
    }
  }

  /**
   * Any field references in the predicate must be resolved relative to the node or field on which
   * the predicate is applied. Therefore we need to switch to that context while the predicate is
   * being parsed.
   * 
   * @param ctx The predicate context
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
          this.stack.pop(PathExpression.class), ctx.axis().Axis().getText()));
    }
  }

  // #endregion References with Predicates ------------------------------------

  // #region External References ----------------------------------------------

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

  // #endregion External References -------------------------------------------

  // #region Value References -------------------------------------------------

  @Override
  public void exitScalarFromFieldReference(ScalarFromFieldReferenceContext ctx) {

    PathExpression path = this.stack.pop(PathExpression.class);
    String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
    if (this.symbols.isAttributeField(fieldId)) {
      this.stack.push(this.script.composeFieldAttributeReference(
          this.symbols.getRelativePath(
              this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId), this.efxContext.peek().absolutePath()),
          this.symbols.getAttributeNameFromAttributeField(fieldId),
          PathExpression.fromFieldType.get(FieldTypes.fromString(this.symbols.getTypeOfField(fieldId)))));
    } else {
    this.stack.push(this.script.composeFieldValueReference(path));
    }
  }

  @Override
  public void exitSequenceFromFieldReference(SequenceFromFieldReferenceContext ctx) {
    PathExpression path = this.stack.pop(PathExpression.class);
    String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
    if (this.symbols.isAttributeField(fieldId)) {
      this.stack.push(this.script.composeFieldAttributeReference(
          this.symbols.getRelativePath(
              this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId), this.efxContext.peek().absolutePath()),
          this.symbols.getAttributeNameFromAttributeField(fieldId),
          PathExpression.fromFieldType.get(FieldTypes.fromString(this.symbols.getTypeOfField(fieldId)))));
    } else {
      this.stack.push(this.script.composeFieldValueReference(path));
    }
  }

  @Override
  public void exitScalarFromAttributeReference(ScalarFromAttributeReferenceContext ctx) {
    this.stack.push(this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
        this.getAttributeName(ctx), StringPathExpression.class));
  }

  @Override
  public void exitSequenceFromAttributeReference(SequenceFromAttributeReferenceContext ctx) {
    this.stack.push(this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
        this.getAttributeName(ctx), StringPathExpression.class));
  }

  // #endregion Value References ----------------------------------------------

  // #region References with context override ---------------------------------

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
    Context variableContext = this.efxContext.getContextFromVariable(getVariableName(ctx.variableReference()));
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
  }

  @Override
  public void exitFieldReferenceWithVariableContextOverride(
      FieldReferenceWithVariableContextOverrideContext ctx) {
    if (ctx.contextVariableSpecifier() != null) {
      final PathExpression field = this.stack.pop(PathExpression.class);
      final PathExpression contextVariable = this.stack.pop(PathExpression.class);
      this.stack.push(this.script.joinPaths(contextVariable, field));
      this.efxContext.pop(); // Restores the previous context
    }
  }

  // #endregion References with context override ------------------------------

  // #region Other References -------------------------------------------------

  @Override
  public void exitCodelistReference(CodelistReferenceContext ctx) {
    this.stack.push(this.script.composeList(this.symbols.expandCodelist(this.getCodelistName(ctx))
        .stream().map(s -> this.script.getStringLiteralFromUnquotedString(s))
        .collect(Collectors.toList()), StringSequenceExpression.class));
  }

  @Override
  public void exitVariableReference(VariableReferenceContext ctx) {
    String variableName = getVariableName(ctx);
    this.stack.pushIdentifierReference(variableName);
  }

  // #endregion Other References ----------------------------------------------

  // #endregion References ----------------------------------------------------

  // #region New in EFX-2: Indexers ------------------------------------------

  @Override
  public void exitStringAtSequenceIndex(StringAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitNumericAtSequenceIndex(NumericAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitBooleanAtSequenceIndex(BooleanAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitDateAtSequenceIndex(DateAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeAtSequenceIndex(TimeAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationAtSequenceIndex(DurationAtSequenceIndexContext ctx) {
    this.exitSequenceAtIndex(DurationExpression.class, DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitSequenceAtIndex(
      Class<? extends ScalarExpression> itemType, Class<T> listType) {
    NumericExpression index = this.stack.pop(NumericExpression.class);
    T list = this.stack.pop(listType);
    this.stack.push(this.script.composeIndexer(list, index, itemType));
  }

  // #endregion New in EFX-2: Indexers ---------------------------------------

  // #region Parameter Declarations -------------------------------------------


  @Override
  public void exitStringParameterDeclaration(StringParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(getVariableName(ctx), StringExpression.class);
  }

  @Override
  public void exitNumericParameterDeclaration(NumericParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(getVariableName(ctx), NumericExpression.class);
  }

  @Override
  public void exitBooleanParameterDeclaration(BooleanParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(getVariableName(ctx), BooleanExpression.class);
  }

  @Override
  public void exitDateParameterDeclaration(DateParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(getVariableName(ctx), DateExpression.class);
  }

  @Override
  public void exitTimeParameterDeclaration(TimeParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(getVariableName(ctx), TimeExpression.class);
  }

  @Override
  public void exitDurationParameterDeclaration(DurationParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(getVariableName(ctx), DurationExpression.class);
  }

  private void exitParameterDeclaration(String parameterName, Class<? extends TypedExpression> parameterType) {
    if (this.expressionParameters.isEmpty()) {
      throw new ParseCancellationException("No parameter passed for " + parameterName);
    }

    Parameter parameter = new Parameter(parameterName,
        this.script.composeParameterDeclaration(parameterName, parameterType),
        this.script.composeVariableReference(parameterName, parameterType),
        this.translateParameter(this.expressionParameters.pop(), parameterType));
    this.stack.declareIdentifier(parameter);
  }

  // #endregion Parameter Declarations ----------------------------------------

  // #region Boolean functions ------------------------------------------------

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
    final SequenceExpression two = this.stack.pop(SequenceExpression.class);
    final SequenceExpression one = this.stack.pop(SequenceExpression.class);
    this.stack.push(this.script.composeSequenceEqualFunction(one, two));
  }

  // #endregion Boolean functions ---------------------------------------------

  // #region Numeric functions ------------------------------------------------

  @Override
  public void exitCountFunction(CountFunctionContext ctx) {
    final SequenceExpression expression = this.stack.pop(SequenceExpression.class);
    this.stack.push(this.script.composeCountOperation(expression));
  }

  @Override
  public void exitNumberFunction(NumberFunctionContext ctx) {
    this.stack.push(this.script.composeToNumberConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitSumFunction(SumFunctionContext ctx) {
    this.stack.push(this.script.composeSumOperation(this.stack.pop(NumericSequenceExpression.class)));
  }

  @Override
  public void exitStringLengthFunction(StringLengthFunctionContext ctx) {
    this.stack
        .push(this.script.composeStringLengthCalculation(this.stack.pop(StringExpression.class)));
  }

  // #endregion Numeric functions ---------------------------------------------

  // #region String functions -------------------------------------------------

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

  // #region New in EFX-2 -----------------------------------------------------

  @Override
  public void exitUpperCaseFunction(UpperCaseFunctionContext ctx) {
    this.stack.push(this.script.composeToUpperCaseConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitLowerCaseFunction(LowerCaseFunctionContext ctx) {
    this.stack.push(this.script.composeToLowerCaseConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitStringJoinFunction(StringJoinFunctionContext ctx) {
    final StringExpression separator = this.stack.pop(StringExpression.class);
    final StringSequenceExpression list = this.stack.pop(StringSequenceExpression.class);
    this.stack.push(this.script.composeStringJoin(list, separator));
  }

  @Override
  public void exitPreferredLanguageFunction(PreferredLanguageFunctionContext ctx) {
    this.stack.push(this.script.getPreferredLanguage(this.stack.pop(MultilingualStringPathExpression.class)));
  }

  @Override
  public void exitPreferredLanguageTextFunction(PreferredLanguageTextFunctionContext ctx) {
    this.stack.push(this.script.getTextInPreferredLanguage(this.stack.pop(MultilingualStringPathExpression.class)));
  }

  // #endregion New in EFX-2 --------------------------------------------------

  // #endregion String functions ----------------------------------------------

  // #region Date functions ---------------------------------------------------

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

  // #endregion Date functions ------------------------------------------------

  // #region Time functions ---------------------------------------------------

  @Override
  public void exitTimeFromStringFunction(TimeFromStringFunctionContext ctx) {
    this.stack.push(this.script.composeToTimeConversion(this.stack.pop(StringExpression.class)));
  }

  // #endregion Time functions ----------------------------------------------

  // #region Duration Functions -----------------------------------------------

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

  // #endregion Duration Functions -------------------------------------------- 

  // #region Sequence Functions -----------------------------------------------

  @Override
  public void exitDistinctValuesFunction(DistinctValuesFunctionContext ctx) {
    var sequence = this.stack.peek();
    assert sequence instanceof TypedExpression : "Expected a TypedExpression at the top of the stack.";
    final Class<?> sequenceType = ((TypedExpression)sequence).getDataType();
    if (EfxDataType.String.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(StringSequenceExpression.class);
    } else if (EfxDataType.Number.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(NumericSequenceExpression.class);
    } else if (EfxDataType.Boolean.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(BooleanSequenceExpression.class);
    } else if (EfxDataType.Date.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(DateSequenceExpression.class);
    } else if (EfxDataType.Time.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(TimeSequenceExpression.class);
    } else if (EfxDataType.Duration.class.isAssignableFrom(sequenceType)) {
      this.exitDistinctValuesFunction(DurationSequenceExpression.class);
    } else {
      throw new IllegalArgumentException(
          "Unsupported sequence type: " + sequenceType.getSimpleName());
    }
  }

  private <T extends SequenceExpression> void exitDistinctValuesFunction(
      Class<T> listType) {
    final T list = this.stack.pop(listType);
    this.stack.push(this.script.composeDistinctValuesFunction(list, listType));
  }

  @Override
  public void exitUnionFunction(UnionFunctionContext ctx) {
    var sequence = this.stack.peek();
    assert sequence instanceof TypedExpression : "Expected a TypedExpression at the top of the stack.";
    final Class<?> sequenceType = ((TypedExpression)sequence).getDataType();
    if (EfxDataType.String.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(StringSequenceExpression.class);
    } else if (EfxDataType.Number.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(NumericSequenceExpression.class);
    } else if (EfxDataType.Boolean.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(BooleanSequenceExpression.class);
    } else if (EfxDataType.Date.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(DateSequenceExpression.class);
    } else if (EfxDataType.Time.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(TimeSequenceExpression.class);
    } else if (EfxDataType.Duration.class.isAssignableFrom(sequenceType)) {
      this.exitUnionFunction(DurationSequenceExpression.class);
    } else {
      throw new IllegalArgumentException(
          "Unsupported sequence type: " + sequenceType.getSimpleName());
    }
  }

  private <T extends SequenceExpression> void exitUnionFunction(
      Class<T> listType) {
    final T two = this.stack.pop(listType);
    final T one = this.stack.pop(listType);
    this.stack.push(this.script.composeUnionFunction(one, two, listType));
  }

  @Override
  public void exitIntersectFunction(IntersectFunctionContext ctx) {
    var sequence = this.stack.peek();
    assert sequence instanceof TypedExpression : "Expected a TypedExpression at the top of the stack.";
    final Class<?> sequenceType = ((TypedExpression)sequence).getDataType();
    if (EfxDataType.String.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(StringSequenceExpression.class);
    } else if (EfxDataType.Number.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(NumericSequenceExpression.class);
    } else if (EfxDataType.Boolean.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(BooleanSequenceExpression.class);
    } else if (EfxDataType.Date.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(DateSequenceExpression.class);
    } else if (EfxDataType.Time.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(TimeSequenceExpression.class);
    } else if (EfxDataType.Duration.class.isAssignableFrom(sequenceType)) {
      this.exitIntersectFunction(DurationSequenceExpression.class);
    } else {
      throw new IllegalArgumentException(
          "Unsupported sequence type: " + sequenceType.getSimpleName());
    }
  }

  private <T extends SequenceExpression> void exitIntersectFunction(
      Class<T> listType) {
    final T two = this.stack.pop(listType);
    final T one = this.stack.pop(listType);
    this.stack.push(this.script.composeIntersectFunction(one, two, listType));
  }

  @Override
  public void exitExceptFunction(ExceptFunctionContext ctx) {
    var sequence = this.stack.peek();
    assert sequence instanceof TypedExpression : "Expected a TypedExpression at the top of the stack.";
    final Class<?> sequenceType = ((TypedExpression)sequence).getDataType();
    if (EfxDataType.String.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(StringSequenceExpression.class);
    } else if (EfxDataType.Number.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(NumericSequenceExpression.class);
    } else if (EfxDataType.Boolean.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(BooleanSequenceExpression.class);
    } else if (EfxDataType.Date.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(DateSequenceExpression.class);
    } else if (EfxDataType.Time.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(TimeSequenceExpression.class);
    } else if (EfxDataType.Duration.class.isAssignableFrom(sequenceType)) {
      this.exitExceptFunction(DurationSequenceExpression.class);
    } else {
      throw new IllegalArgumentException(
          "Unsupported sequence type: " + sequenceType.getSimpleName());
    }
  }

  private <T extends SequenceExpression> void exitExceptFunction(
      Class<T> listType) {
    final T two = this.stack.pop(listType);
    final T one = this.stack.pop(listType);
    this.stack.push(this.script.composeExceptFunction(one, two, listType));
  }

  // #endregion Sequence Functions --------------------------------------------

  // #region Helpers ----------------------------------------------------------
      
  protected static String getLexerSymbol(int tokenType) {
    return EfxLexer.VOCABULARY.getLiteralName(tokenType).replaceAll("^'|'$", "");
  }

  // #region Codelist Names ---------------------------------------------------    

  protected String getCodelistName(String efxCodelistIdentifier) {
    return StringUtils.substringAfter(efxCodelistIdentifier, CODELIST_PREFIX);
  }

  private String getCodelistName(CodelistReferenceContext ctx) {
    return this.getCodelistName(ctx.CodelistId().getText());
  }

  // #endregion Codelist Names ------------------------------------------------

  // #region Attribute Names --------------------------------------------------

  protected String getAttributeName(String efxAttributeIdentifier) {
    return StringUtils.substringAfter(efxAttributeIdentifier, ATTRIBUTE_PREFIX);
  }

  private String getAttributeName(SequenceFromAttributeReferenceContext ctx) {
    return this.getAttributeName(ctx.attributeReference().Attribute().getText());
  }

  private String getAttributeName(ScalarFromAttributeReferenceContext ctx) {
    return this.getAttributeName(ctx.attributeReference().Attribute().getText());
  }

  // #endregion Attribute Names -----------------------------------------------

  // #region Variable Names ---------------------------------------------------

  static protected String getVariableName(String efxVariableIdentifier) {
    return StringUtils.substringAfter(efxVariableIdentifier, VARIABLE_PREFIX);
  }

  static private String getVariableName(VariableReferenceContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static protected String getVariableName(ContextVariableDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(StringVariableDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(NumericVariableDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(BooleanVariableDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(DateVariableDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(TimeVariableDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(DurationVariableDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(StringParameterDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(NumericParameterDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(BooleanParameterDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(DateParameterDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(TimeParameterDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  static private String getVariableName(DurationParameterDeclarationContext ctx) {
    return getVariableName(ctx.Variable().getText());
  }

  // #endregion Variable Names ---------------------------------------------------

  // #endregion Helpers -------------------------------------------------------

  // #region Pre-processing ---------------------------------------------------

  @Override
  public void exitLateBoundSequence(LateBoundSequenceContext ctx) {
    assert false: "This should have been handled by the preprocessor: " + ctx.getText() +". Check any changes that you might have made in the EFX grammar that may have broken this assumption.";
  }

  @Override
  public void exitLateBoundScalar(LateBoundScalarContext ctx) {
    assert false: "This should have been handled by the preprocessor: " + ctx.getText() +". Check any changes that you might have made in the EFX grammar that may have broken this assumption.";
  }

  private static String textTypeName = getLexerSymbol(EfxLexer.Text);
  private static String booleanTypeName = getLexerSymbol(EfxLexer.Indicator);
  private static String numericTypeName = getLexerSymbol(EfxLexer.Number);
  private static String dateTypeName = getLexerSymbol(EfxLexer.Date);
  private static String timeTypeName = getLexerSymbol(EfxLexer.Time);
  private static String durationTypeName = getLexerSymbol(EfxLexer.Measure);

  private static final Map<String, String> eFormsToEfxTypeMap = Map.ofEntries( //
      entry(FieldTypes.ID.getName(), textTypeName), //
      entry(FieldTypes.ID_REF.getName(), textTypeName), //
      entry(FieldTypes.TEXT.getName(), textTypeName), //
      entry(FieldTypes.TEXT_MULTILINGUAL.getName(), textTypeName), //
      entry(FieldTypes.INDICATOR.getName(), booleanTypeName), //
      entry(FieldTypes.AMOUNT.getName(), numericTypeName), //
      entry(FieldTypes.NUMBER.getName(), numericTypeName), //
      entry(FieldTypes.MEASURE.getName(), durationTypeName), //
      entry(FieldTypes.CODE.getName(), textTypeName), //
      entry(FieldTypes.INTERNAL_CODE.getName(), textTypeName), //
      entry(FieldTypes.INTEGER.getName(), numericTypeName), //
      entry(FieldTypes.DATE.getName(), dateTypeName), //
      entry(FieldTypes.ZONED_DATE.getName(), dateTypeName), //
      entry(FieldTypes.TIME.getName(), timeTypeName), //
      entry(FieldTypes.ZONED_TIME.getName(), timeTypeName), //
      entry(FieldTypes.URL.getName(), textTypeName), //
      entry(FieldTypes.PHONE.getName(), textTypeName), //
      entry(FieldTypes.EMAIL.getName(), textTypeName));

  private static final Map<Class<? extends EfxDataType>, String> javaToEfxTypeMap = Map.ofEntries(
      entry(EfxDataType.String.class, textTypeName), //
      entry(EfxDataType.Boolean.class, booleanTypeName), //
      entry(EfxDataType.Number.class, numericTypeName), //
      entry(EfxDataType.Duration.class, durationTypeName), //
      entry(EfxDataType.Date.class, dateTypeName), //
      entry(EfxDataType.Time.class, timeTypeName));

  /**
   * The EFX expression pre-processor is used to remove expression ambiguities
   * that cannot be addressed by the EFX grammar itself. The EFX grammar tries to
   * enforce type checking to the extent possible, however, the types of fields,
   * variables and expression parameters (as well as the type of some expressions
   * that reference them) cannot be determined until the EFX expression is being
   * parsed. For example, adding a duration to a date has different semantics than
   * adding two numbers together.
   * 
   * Expressions referencing fields, variables and parameters are called
   * late-bound expressions in EFX because their type cannot be inferred by the
   * syntax but instead needs to be determined by the parser. Since the types of
   * late-bound expressions are critical in determining the correct parse tree, a
   * one-pass parser would require the use of type casting in the EFX expression
   * itself to resolve any ambiguities. The role of the expression preprocessor is
   * therefore to do a first pass on the EFX expression to insert type casts where
   * necessary.
   */
  class ExpressionPreprocessor extends EfxBaseListener {
    final SymbolResolver symbols;
    final BaseErrorListener errorListener;
    final EfxLexer lexer;
    final CommonTokenStream tokens;
    final EfxParser parser;
    final TokenStreamRewriter rewriter;
    final CallStack stack = new CallStack();
    
    ExpressionPreprocessor(String expression) {
      this(CharStreams.fromString(expression));
    }

    ExpressionPreprocessor(final CharStream charStream) {
      super();

      this.symbols = EfxExpressionTranslatorV2.this.symbols;
      this.errorListener = EfxExpressionTranslatorV2.this.errorListener;

      this.lexer = new EfxLexer(charStream);
      this.tokens = new CommonTokenStream(lexer);
      this.parser = new EfxParser(tokens);
      this.rewriter = new TokenStreamRewriter(tokens);

      if (this.errorListener != null) {
        lexer.removeErrorListeners();
        lexer.addErrorListener(this.errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(this.errorListener);
      }
    }

    String processExpression() {
      final ParseTree tree = parser.singleExpression();
      final ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(this, tree);
      return this.rewriter.getText();
    }

    @Override
    public void exitScalarFromFieldReference(ScalarFromFieldReferenceContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundScalarContext.class)) {
        return;
      }

      String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
      String fieldType = eFormsToEfxTypeMap.get(this.symbols.getTypeOfField(fieldId));

      // Insert the type cast
      this.rewriter.insertBefore(ctx.getStart(), "(" + fieldType + ")");
    }

    @Override
    public void exitScalarFromAttributeReference(ScalarFromAttributeReferenceContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundScalarContext.class)) {
        return;
      }

      // Insert the type cast. For attributes, the type is always text.
      this.rewriter.insertBefore(ctx.getStart(), "(" + textTypeName + ")");
    }

    @Override
    public void exitSequenceFromFieldReference(SequenceFromFieldReferenceContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundSequenceContext.class)) {
        return;
      }

      // Find the referenced field and get its type
      String fieldId = getFieldIdFromChildSimpleFieldReferenceContext(ctx);
      String fieldType = eFormsToEfxTypeMap.get(this.symbols.getTypeOfField(fieldId));

      if (fieldType != null) {
        // Insert the type cast
        this.rewriter.insertBefore(ctx.getStart(), "(" + fieldType + ")");
      }
    }

    @Override
    public void exitSequenceFromAttributeReference(SequenceFromAttributeReferenceContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundSequenceContext.class)) {
        return;
      }

      // Insert the type cast
      this.rewriter.insertBefore(ctx.getStart(), "(" + textTypeName + ")");
    }

    @Override
    public void exitVariableReference(VariableReferenceContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundScalarContext.class)) {
        return;
      }

      String variableName = getVariableName(ctx);
      String variableType = javaToEfxTypeMap.get(this.stack.getTypeOfIdentifier(variableName));

      if (variableType != null) {
        // Insert the type cast
        this.rewriter.insertBefore(ctx.Variable().getSymbol(), "(" + variableType + ")");
      }
    }

    boolean hasParentContextOfType(ParserRuleContext ctx, Class<? extends ParserRuleContext> parentClass) {
      ParserRuleContext parent = ctx.getParent();
      while (parent != null) {
        if (parentClass.isInstance(parent)) {
          return true;
        }
        parent = parent.getParent();
      }
      return false;
    }

    // #region Variable declarations ------------------------------------------

    @Override
    public void exitStringVariableDeclaration(StringVariableDeclarationContext ctx) {
      String variableName = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(variableName, StringExpression.empty(), StringExpression.empty(), StringExpression.empty()));
    }
  
    @Override
    public void exitBooleanVariableDeclaration(BooleanVariableDeclarationContext ctx) {
      String variableName = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(variableName, BooleanExpression.empty(), BooleanExpression.empty(), BooleanExpression.empty()));
    }
  
    @Override
    public void exitNumericVariableDeclaration(NumericVariableDeclarationContext ctx) {
      String variableName = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(variableName, NumericExpression.empty(), NumericExpression.empty(), NumericExpression.empty()));
    }
  
    @Override
    public void exitDateVariableDeclaration(DateVariableDeclarationContext ctx) {
      String variableName = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(variableName, DateExpression.empty(), DateExpression.empty(), DateExpression.empty()));
    }
  
    @Override
    public void exitTimeVariableDeclaration(TimeVariableDeclarationContext ctx) {
      String variableName = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(variableName, TimeExpression.empty(), TimeExpression.empty(), TimeExpression.empty()));
    }
  
    @Override
    public void exitDurationVariableDeclaration(DurationVariableDeclarationContext ctx) {
      String variableName = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(variableName, DurationExpression.empty(), DurationExpression.empty(), DurationExpression.empty()));
    }

    @Override
    public void exitContextIteratorExpression(ContextIteratorExpressionContext ctx) {
      var variableName = getVariableName(ctx.contextVariableDeclaration()); 
      this.stack.declareIdentifier(new Variable(variableName, DurationExpression.empty(), DurationExpression.empty(), DurationExpression.empty()));
    }

    // #endregion Variable declarations ---------------------------------------

    // #region Parameter declarations -----------------------------------------

    @Override
    public void exitStringParameterDeclaration(StringParameterDeclarationContext ctx) {
      String identifier = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(identifier, StringExpression.empty(), StringExpression.empty(), StringExpression.empty()));
    }

    @Override
    public void exitNumericParameterDeclaration(NumericParameterDeclarationContext ctx) {
      String identifier = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(identifier, NumericExpression.empty(), NumericExpression.empty(), NumericExpression.empty()));
    }

    @Override
    public void exitBooleanParameterDeclaration(BooleanParameterDeclarationContext ctx) {
      String identifier = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(identifier, BooleanExpression.empty(), BooleanExpression.empty(), BooleanExpression.empty()));
    }

    @Override
    public void exitDateParameterDeclaration(DateParameterDeclarationContext ctx) {
      String identifier = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(identifier, DateExpression.empty(), DateExpression.empty(), DateExpression.empty()));
    }

    @Override
    public void exitTimeParameterDeclaration(TimeParameterDeclarationContext ctx) {
      String identifier = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(identifier, TimeExpression.empty(), TimeExpression.empty(), TimeExpression.empty()));
    }

    @Override
    public void exitDurationParameterDeclaration(DurationParameterDeclarationContext ctx) {
      String identifier = getVariableName(ctx);
      this.stack.declareIdentifier(new Variable(identifier, DurationExpression.empty(), DurationExpression.empty(), DurationExpression.empty()));

    }

    // #endregion Parameter declarations --------------------------------------

    // #region Scope management -----------------------------------------------

    @Override
    public void enterQuantifiedExpression(QuantifiedExpressionContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitQuantifiedExpression(QuantifiedExpressionContext ctx) {
      this.stack.popStackFrame();
    }

    @Override
    public void enterStringSequenceFromIteration(StringSequenceFromIterationContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitStringSequenceFromIteration(StringSequenceFromIterationContext ctx) {
      this.stack.popStackFrame();
    }

    @Override
    public void enterNumericSequenceFromIteration(NumericSequenceFromIterationContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitNumericSequenceFromIteration(NumericSequenceFromIterationContext ctx) {
      this.stack.popStackFrame();
    }

    @Override
    public void enterBooleanSequenceFromIteration(BooleanSequenceFromIterationContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitBooleanSequenceFromIteration(BooleanSequenceFromIterationContext ctx) {
      this.stack.popStackFrame();
    }

    @Override
    public void enterDateSequenceFromIteration(DateSequenceFromIterationContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitDateSequenceFromIteration(DateSequenceFromIterationContext ctx) {
      this.stack.popStackFrame();
    }

    @Override
    public void enterTimeSequenceFromIteration(TimeSequenceFromIterationContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitTimeSequenceFromIteration(TimeSequenceFromIterationContext ctx) {
      this.stack.popStackFrame();
    }

    @Override
    public void enterDurationSequenceFromIteration(DurationSequenceFromIterationContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitDurationSequenceFromIteration(DurationSequenceFromIterationContext ctx) {
      this.stack.popStackFrame();
    }

    @Override
    public void enterLateBoundSequenceFromIteration(LateBoundSequenceFromIterationContext ctx) {
      this.stack.pushStackFrame();
    }

    @Override
    public void exitLateBoundSequenceFromIteration(LateBoundSequenceFromIterationContext ctx) {
      this.stack.popStackFrame();
    }

    // #endregion Scope management --------------------------------------------

  }

  // #endregion Pre-processing ------------------------------------------------

}
