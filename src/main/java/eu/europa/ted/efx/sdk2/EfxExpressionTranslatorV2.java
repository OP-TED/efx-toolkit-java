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
import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.exceptions.InvalidArgumentException;
import eu.europa.ted.efx.exceptions.InvalidIdentifierException;
import eu.europa.ted.efx.exceptions.InvalidUsageException;
import eu.europa.ted.efx.exceptions.SdkInconsistencyException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.exceptions.TypeMismatchException;
import eu.europa.ted.efx.exceptions.ConsistencyCheckException;
import eu.europa.ted.efx.interfaces.EfxExpressionTranslator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.CallStack;
import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.Context.FieldContext;
import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.ContextStack;
import eu.europa.ted.efx.model.PrivacySetting;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorExpression;
import eu.europa.ted.efx.model.expressions.iteration.IteratorListExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.DateExpression;
import eu.europa.ted.efx.model.expressions.scalar.DurationExpression;
import eu.europa.ted.efx.model.expressions.scalar.MultilingualStringPath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.scalar.TimeExpression;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DateSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.DurationSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.NumericSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.StringSequenceExpression;
import eu.europa.ted.efx.model.expressions.sequence.TimeSequenceExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.model.types.EfxTypeLattice;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.model.variables.ParsedArguments;
import eu.europa.ted.efx.model.variables.Function;
import eu.europa.ted.efx.model.variables.StrictArguments;
import eu.europa.ted.efx.model.variables.ParsedParameter;
import eu.europa.ted.efx.model.variables.ParsedParameters;
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

  private LinkedList<String> expressionArguments = new LinkedList<>();

  protected EfxExpressionTranslatorV2() {}

  public EfxExpressionTranslatorV2(final SymbolResolver symbolResolver,
      final ScriptGenerator scriptGenerator, final BaseErrorListener errorListener) {
    this.symbols = symbolResolver;
    this.script = scriptGenerator;
    this.errorListener = errorListener;

    this.efxContext = new ContextStack(symbols);
  }

  @Override
  public String translateExpression(final String expression, final String... arguments) {
    this.expressionArguments.addAll(Arrays.asList(arguments));

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

  private <T extends Expression> T translateArgument(final String parameterValue,
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

  private String getLinkedFieldId(String baseFieldId, LinkedFieldPropertyContext ctx) {
    if (ctx.PublicationDate() != null)
      return this.symbols.getPrivacySettingOfField(baseFieldId, PrivacySetting.PUBLICATION_DATE_FIELD);
    else if (ctx.JustificationCode() != null)
      return this.symbols.getPrivacySettingOfField(baseFieldId, PrivacySetting.JUSTIFICATION_CODE_FIELD);
    else if (ctx.JustificationDescription() != null)
      return this.symbols.getPrivacySettingOfField(baseFieldId, PrivacySetting.JUSTIFICATION_DESCRIPTION_FIELD);
    else
      throw ConsistencyCheckException.unhandledLinkedFieldProperty(ctx.getText());
  }

  protected String getFieldId(LinkedFieldReferenceContext ctx) {
    if (ctx == null) {
      return null;
    }
    String baseFieldId = ctx.simpleFieldReference().fieldId.getText();
    if (ctx.linkedFieldProperty() == null) {
      return baseFieldId;
    }
    return this.getLinkedFieldId(baseFieldId, ctx.linkedFieldProperty());
  }

  protected String getFieldId(EfxParser.FieldMentionContext ctx) {
    if (ctx == null) {
      return null;
    }
    String baseFieldId = ctx.fieldId.getText();
    if (ctx.linkedFieldProperty() == null) {
      return baseFieldId;
    }
    return this.getLinkedFieldId(baseFieldId, ctx.linkedFieldProperty());
  }

  protected String getFieldId(FieldReferenceContext ctx) {
    if (ctx == null) {
      return null;
    }

    if (ctx.absoluteFieldReference() != null) {
      return this.getFieldId(ctx.absoluteFieldReference());
    }

    if (ctx.fieldReferenceInOtherNotice() != null) {
      return this.getFieldId(ctx.fieldReferenceInOtherNotice());
    }
    assert false : "Unexpected context type for field reference: " + ctx.getClass().getSimpleName();
    return null;
  }

  protected String getFieldId(AbsoluteFieldReferenceContext ctx) {
    if (ctx == null) {
      return null;
    }
    return this.getFieldId(ctx.reference.reference.linkedFieldReference());
  }

  protected String getFieldId(FieldReferenceInOtherNoticeContext ctx) {
    if (ctx == null) {
      return null;
    }
    return this.getFieldId(ctx.reference.reference.reference.reference.reference.linkedFieldReference());
  }

  protected String getFieldId(FieldContextContext ctx) {
    if (ctx == null) {
      return null;
    }

    if (ctx.absoluteFieldReference() != null) {
      return this.getFieldId(ctx.absoluteFieldReference());
    }

    if (ctx.fieldReferenceWithPredicate() != null) {
      return this.getFieldId(ctx.fieldReferenceWithPredicate());
    }

    assert false : "Unexpected context type for field reference: " + ctx.getClass().getSimpleName();
    return null;
  }

  protected String getFieldId(FieldReferenceWithPredicateContext ctx) {
    if (ctx == null) {
      return null;
    }
    return this.getFieldId(ctx.fieldReferenceWithAxis().linkedFieldReference());
  }

  protected static String getNodeId(NodeReferenceContext ctx) {
    if (ctx == null) {
      return null;
    }

    if (ctx.absoluteNodeReference() != null) {
      return getNodeId(ctx.absoluteNodeReference().nodeReferenceWithPredicate());
    }

    if (ctx.nodeReferenceInOtherNotice() != null) {
      return getNodeId(ctx.nodeReferenceInOtherNotice().nodeReferenceWithPredicate());
    }

    assert false : "Unexpected context type for node reference: " + ctx.getClass().getSimpleName();
    return null;
  }

  protected static String getNodeId(NodeContextContext ctx) {
    if (ctx == null) {
      return null;
    }

    if (ctx.absoluteNodeReference() != null) {
      return getNodeId(ctx.absoluteNodeReference().nodeReferenceWithPredicate());
    }

    if (ctx.nodeReferenceWithPredicate() != null) {
      return getNodeId(ctx.nodeReferenceWithPredicate());
    }

    assert false : "Unexpected context type for node reference: " + ctx.getClass().getSimpleName();
    return null;
  }

  protected static String getNodeId(NodeReferenceWithPredicateContext ctx) {
    if (ctx == null) {
      return null;
    }
    return ctx.simpleNodeReference().NodeId().getText();
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
      } else {
        final TerminalNode alias = ctx.Identifier();
        if (alias != null) {
          String fieldId = this.symbols.getFieldIdFromAlias(alias.getText());
          if (fieldId != null) {
            this.efxContext.pushFieldContext(fieldId);
          } else {
            String nodeId = this.symbols.getNodeIdFromAlias(alias.getText());
            if (nodeId != null) {
              this.efxContext.pushNodeContext(nodeId);
            } else {
              throw SymbolResolutionException.unknownSymbol(alias.getText());
            }
          }
        }
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
  public void exitStringUniqueValueCondition(EfxParser.StringUniqueValueConditionContext ctx) {
    StringSequenceExpression haystack = this.stack.pop(StringSequenceExpression.class);
    StringExpression needle = this.stack.pop(StringExpression.class);

    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      this.stack.push(
          this.script.composeLogicalNot(this.script.composeUniqueValueCondition(needle, haystack)));
    } else {
      this.stack.push(this.script.composeUniqueValueCondition(needle, haystack));
    }
  }

  @Override
  public void exitNumericUniqueValueCondition(EfxParser.NumericUniqueValueConditionContext ctx) {
    NumericSequenceExpression haystack = this.stack.pop(NumericSequenceExpression.class);
    NumericExpression needle = this.stack.pop(NumericExpression.class);

    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      this.stack.push(
          this.script.composeLogicalNot(this.script.composeUniqueValueCondition(needle, haystack)));
    } else {
      this.stack.push(this.script.composeUniqueValueCondition(needle, haystack));
    }
  }

  @Override
  public void exitBooleanUniqueValueCondition(EfxParser.BooleanUniqueValueConditionContext ctx) {
    BooleanSequenceExpression haystack = this.stack.pop(BooleanSequenceExpression.class);
    BooleanExpression needle = this.stack.pop(BooleanExpression.class);

    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      this.stack.push(
          this.script.composeLogicalNot(this.script.composeUniqueValueCondition(needle, haystack)));
    } else {
      this.stack.push(this.script.composeUniqueValueCondition(needle, haystack));
    }
  }

  @Override
  public void exitDateUniqueValueCondition(EfxParser.DateUniqueValueConditionContext ctx) {
    DateSequenceExpression haystack = this.stack.pop(DateSequenceExpression.class);
    DateExpression needle = this.stack.pop(DateExpression.class);

    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      this.stack.push(
          this.script.composeLogicalNot(this.script.composeUniqueValueCondition(needle, haystack)));
    } else {
      this.stack.push(this.script.composeUniqueValueCondition(needle, haystack));
    }
  }

  @Override
  public void exitTimeUniqueValueCondition(EfxParser.TimeUniqueValueConditionContext ctx) {
    TimeSequenceExpression haystack = this.stack.pop(TimeSequenceExpression.class);
    TimeExpression needle = this.stack.pop(TimeExpression.class);

    if (ctx.modifier != null && ctx.modifier.getText().equals(NOT_MODIFIER)) {
      this.stack.push(
          this.script.composeLogicalNot(this.script.composeUniqueValueCondition(needle, haystack)));
    } else {
      this.stack.push(this.script.composeUniqueValueCondition(needle, haystack));
    }
  }

  @Override
  public void exitDurationUniqueValueCondition(EfxParser.DurationUniqueValueConditionContext ctx) {
    DurationSequenceExpression haystack = this.stack.pop(DurationSequenceExpression.class);
    DurationExpression needle = this.stack.pop(DurationExpression.class);

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
    this.exitIteratorExpression(ctx.stringIteratorVariableDeclaration().variableName.getText(), StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanIteratorExpression(BooleanIteratorExpressionContext ctx) {
    this.exitIteratorExpression(ctx.booleanIteratorVariableDeclaration().variableName.getText(), BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericIteratorExpression(NumericIteratorExpressionContext ctx) {
    this.exitIteratorExpression(ctx.numericIteratorVariableDeclaration().variableName.getText(),  NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitDateIteratorExpression(DateIteratorExpressionContext ctx) {
    this.exitIteratorExpression(ctx.dateIteratorVariableDeclaration().variableName.getText(), DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeIteratorExpression(TimeIteratorExpressionContext ctx) {
    this.exitIteratorExpression(ctx.timeIteratorVariableDeclaration().variableName.getText(), TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationIteratorExpression(DurationIteratorExpressionContext ctx) {
    this.exitIteratorExpression(ctx.durationIteratorVariableDeclaration().variableName.getText(), DurationExpression.class, DurationSequenceExpression.class);
  }

  @Override
  public void exitContextIteratorExpression(ContextIteratorExpressionContext ctx) {
    PathExpression path = this.stack.pop(PathExpression.class);

    var variableType = path.asScalar().getClass();
    var variableName = ctx.contextIteratorVariableDeclaration().variableName.getText();
    Variable variable = new Variable(variableName,
        this.script.composeVariableDeclaration(variableName, variableType),
        Expression.empty(variableType),
        this.script.composeVariableReference(variableName, variableType));
    this.stack.declareIdentifier(variable);

    this.stack.push(this.script.composeIteratorExpression(variable.declarationExpression, path.asSequence()));
    if (ctx.fieldContext() != null) {
      final String contextFieldId = getFieldId(ctx.fieldContext());
      this.efxContext.declareContextVariable(variable.name,
          new FieldContext(contextFieldId, this.symbols.getAbsolutePathOfField(contextFieldId),
              this.symbols.getRelativePathOfField(contextFieldId, this.efxContext.symbol())));
    } else if (ctx.nodeContext() != null) {
      final String contextNodeId =
          getNodeId(ctx.nodeContext());
      this.efxContext.declareContextVariable(variable.name,
          new NodeContext(contextNodeId, this.symbols.getAbsolutePathOfNode(contextNodeId),
              this.symbols.getRelativePathOfNode(contextNodeId, this.efxContext.symbol())));
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
  public void exitParenthesizedStrings(ParenthesizedStringsContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(StringSequenceExpression.class), StringSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedNumbers(ParenthesizedNumbersContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(NumericSequenceExpression.class), NumericSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedBooleans(ParenthesizedBooleansContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(BooleanSequenceExpression.class), BooleanSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedDates(ParenthesizedDatesContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(DateSequenceExpression.class), DateSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedTimes(ParenthesizedTimesContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(TimeSequenceExpression.class), TimeSequenceExpression.class));
  }

  @Override
  public void exitParenthesizedDurations(ParenthesizedDurationsContext ctx) {
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
    this.stack.push(this.script.getDateLiteralEquivalent(ctx.DateLiteral().getText()));
  }

  @Override
  public void exitTimeLiteral(TimeLiteralContext ctx) {
    this.stack.push(this.script.getTimeLiteralEquivalent(ctx.TimeLiteral().getText()));
  }

  @Override
  public void exitDurationLiteral(DurationLiteralContext ctx) {
    this.stack.push(this.script.getDurationLiteralEquivalent(ctx.getText()));
  }

  // Sequence literals for parameter values
  @Override
  public void exitStringSequenceLiteral(StringSequenceLiteralContext ctx) {
    int count = ctx.stringLiteral().size();
    List<StringExpression> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(0, this.stack.pop(StringExpression.class));
    }
    this.stack.push(this.script.composeList(items, StringSequenceExpression.class));
  }

  @Override
  public void exitNumericSequenceLiteral(NumericSequenceLiteralContext ctx) {
    int count = ctx.numericLiteral().size();
    List<NumericExpression> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(0, this.stack.pop(NumericExpression.class));
    }
    this.stack.push(this.script.composeList(items, NumericSequenceExpression.class));
  }

  @Override
  public void exitBooleanSequenceLiteral(BooleanSequenceLiteralContext ctx) {
    int count = ctx.booleanLiteral().size();
    List<BooleanExpression> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(0, this.stack.pop(BooleanExpression.class));
    }
    this.stack.push(this.script.composeList(items, BooleanSequenceExpression.class));
  }

  @Override
  public void exitDateSequenceLiteral(DateSequenceLiteralContext ctx) {
    int count = ctx.dateLiteral().size();
    List<DateExpression> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(0, this.stack.pop(DateExpression.class));
    }
    this.stack.push(this.script.composeList(items, DateSequenceExpression.class));
  }

  @Override
  public void exitTimeSequenceLiteral(TimeSequenceLiteralContext ctx) {
    int count = ctx.timeLiteral().size();
    List<TimeExpression> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(0, this.stack.pop(TimeExpression.class));
    }
    this.stack.push(this.script.composeList(items, TimeSequenceExpression.class));
  }

  @Override
  public void exitDurationSequenceLiteral(DurationSequenceLiteralContext ctx) {
    int count = ctx.durationLiteral().size();
    List<DurationExpression> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(0, this.stack.pop(DurationExpression.class));
    }
    this.stack.push(this.script.composeList(items, DurationSequenceExpression.class));
  }

  // #endregion Literals ------------------------------------------------------

  // #region References -------------------------------------------------------

  @Override
  public void exitSimpleNodeReference(SimpleNodeReferenceContext ctx) {
    this.stack.push(
        this.symbols.getRelativePathOfNode(ctx.NodeId().getText(), this.efxContext.symbol()));
  }

  @Override
  public void exitSimpleFieldReference(EfxParser.SimpleFieldReferenceContext ctx) {
    this.stack.push(
        symbols.getRelativePathOfField(ctx.fieldId.getText(), this.efxContext.symbol()));
  }

  @Override
  public void exitLinkedFieldReference(LinkedFieldReferenceContext ctx) {
    if (ctx.linkedFieldProperty() != null) {
      this.stack.pop(PathExpression.class); // discard base field path
      String companionFieldId = getFieldId(ctx);
      this.stack.push(
          symbols.getRelativePathOfField(companionFieldId, this.efxContext.symbol()));
    }
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
      PathExpression nodeReference = this.stack.pop(PathExpression.class);
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
    var parent = ctx.getParent();
    if (parent instanceof NodeReferenceWithPredicateContext) {
      final String nodeId = getNodeId((NodeReferenceWithPredicateContext) parent);
      this.efxContext.pushNodeContext(nodeId);
    } else if (parent instanceof FieldReferenceWithPredicateContext) {
      final String fieldId = getFieldId((FieldReferenceWithPredicateContext) parent);
      this.efxContext.pushFieldContext(fieldId);
    } else {
      throw new ParseCancellationException("Unexpected parent context for predicate: " + parent.getClass().getSimpleName());
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
    String fieldId = getFieldId(ctx.fieldReference());
    if (this.symbols.isAttributeField(fieldId)) {
      this.stack.push(this.script.composeFieldAttributeReference(
          this.script.contextualizePath(
              this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId), this.efxContext.peek().absolutePath()),
          this.symbols.getAttributeNameFromAttributeField(fieldId),
          ScalarPath.fromFieldType.get(FieldTypes.fromString(this.symbols.getTypeOfField(fieldId)))));
    } else {
    this.stack.push(this.script.composeFieldValueReference(path));
    }
  }

  @Override
  public void exitSequenceFromFieldReference(SequenceFromFieldReferenceContext ctx) {
    PathExpression path = this.stack.pop(PathExpression.class);
    String fieldId = getFieldId(ctx.fieldReference());
    if (this.symbols.isAttributeField(fieldId)) {
      this.stack.push(this.script.composeFieldAttributeReference(
          this.script.contextualizePath(
              this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId), this.efxContext.peek().absolutePath()),
          this.symbols.getAttributeNameFromAttributeField(fieldId),
          ScalarPath.fromFieldType.get(FieldTypes.fromString(this.symbols.getTypeOfField(fieldId)))));
    } else {
      this.stack.push(this.script.composeFieldValueReference(path));
    }
  }

  @Override
  public void exitScalarFromAttributeReference(ScalarFromAttributeReferenceContext ctx) {
    this.stack.push(this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
        ctx.attributeReference().attributeName.getText(), StringPath.class));
  }

  @Override
  public void exitSequenceFromAttributeReference(SequenceFromAttributeReferenceContext ctx) {
    this.stack.push(this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
        ctx.attributeReference().attributeName.getText(), StringPath.class));
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
    final String contextFieldId = getFieldId(ctx.fieldContext());
    this.efxContext
        .push(new FieldContext(contextFieldId, this.symbols.getAbsolutePathOfField(contextFieldId),
            this.symbols.getRelativePathOfField(contextFieldId, this.efxContext.symbol())));
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
    final String contextNodeId = getNodeId(ctx.node);
    this.efxContext
        .push(new NodeContext(contextNodeId, this.symbols.getAbsolutePathOfNode(contextNodeId),
            this.symbols.getRelativePathOfNode(contextNodeId, this.efxContext.symbol())));
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
    String variableName = ctx.variableReference().variableName.getText();
    Context variableContext = this.efxContext.getContextFromVariable(variableName);
    if (variableContext == null) {
      throw InvalidIdentifierException.notAContextVariable(variableName);
    }
    if (variableContext.isFieldContext()) {
      this.efxContext.push(new FieldContext(variableContext.symbol(),
          this.symbols.getAbsolutePathOfField(variableContext.symbol()), this.symbols
              .getRelativePathOfField(variableContext.symbol(), this.efxContext.symbol())));
    } else if (variableContext.isNodeContext()) {
      this.efxContext.push(new NodeContext(variableContext.symbol(),
          this.symbols.getAbsolutePathOfNode(variableContext.symbol()), this.symbols
              .getRelativePathOfNode(variableContext.symbol(), this.efxContext.symbol())));
    } else {
      assert false : "Context variable must be either a field or node context: " + variableName;
    }
    // Push the context variable's path onto the stack for use by exitFieldReferenceWithVariableContextOverride
    this.stack.pushIdentifierReference(variableName);
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
    this.stack.push(this.script.composeList(this.symbols.expandCodelist(ctx.codelistName.getText())
        .stream().map(s -> this.script.getStringLiteralFromUnquotedString(s))
        .collect(Collectors.toList()), StringSequenceExpression.class));
  }

  @Override
  public void exitScalarFromVariableReference(ScalarFromVariableReferenceContext ctx) {
    String variableName = ctx.variableReference().variableName.getText();
    this.stack.pushIdentifierReference(variableName);
  }

  @Override
  public void exitSequenceFromVariableReference(SequenceFromVariableReferenceContext ctx) {
    String variableName = ctx.variableReference().variableName.getText();
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

    var addParenthesis = this.stack.peek() instanceof PathExpression;

    T list = this.stack.pop(listType);

    if (addParenthesis) {
      list = this.script.composeParenthesizedExpression(list, listType);
    }
    this.stack.push(this.script.composeIndexer(list, index, itemType));
  }

  // #endregion New in EFX-2: Indexers ---------------------------------------

  // #region New in EFX-2: Function invocation ------------------------------


  @Override
  public void enterFunctionInvocation(FunctionInvocationContext ctx) {
    final Function function = this.stack.getFunction(ctx.functionName.getText());
    this.stack.push(new StrictArguments(function));
  }

  @Override
  public void exitArgument(ArgumentContext ctx) {
    var argument = this.stack.pop(TypedExpression.class);        // pop the argument
    var functionArguments = this.stack.pop(StrictArguments.class);  // pop the function arguments
    functionArguments.addArgument(argument);                                  // add the argument to the list of arguments
    this.stack.push(functionArguments);                                       // push the updated list of arguments
  }

  @Override
  public void exitArgumentList(ArgumentListContext ctx) {
    var arguments = this.stack.pop(StrictArguments.class); // pop the function arguments
    var countExpectedArguments = arguments.identifier.parameters.size();
    var countPassedArguments = arguments.size();
    if (countExpectedArguments != countPassedArguments) { // check if the number of passed arguments is correct
      throw InvalidArgumentException.argumentNumberMismatch(arguments.identifier, countExpectedArguments, countPassedArguments);
    }
    this.stack.push(arguments); // push the list of arguments back to the stack
  }

  @Override
  public void exitStringFunctionInvocation(StringFunctionInvocationContext ctx) {
    var parameters = this.stack.pop(ParsedArguments.class).getArgumentValues();
    this.stack.push(this.script.composeFunctionInvocation(ctx.functionInvocation().functionName.getText(), parameters, StringExpression.class));
  }

  @Override
  public void exitNumericFunctionInvocation(NumericFunctionInvocationContext ctx) {
    var parameters = this.stack.pop(ParsedArguments.class).getArgumentValues();
    this.stack.push(this.script.composeFunctionInvocation(ctx.functionInvocation().functionName.getText(), parameters, NumericExpression.class));
  }

  @Override
  public void exitBooleanFunctionInvocation(BooleanFunctionInvocationContext ctx) {
    var parameters = this.stack.pop(ParsedArguments.class).getArgumentValues();
    this.stack.push(this.script.composeFunctionInvocation(ctx.functionInvocation().functionName.getText(), parameters, BooleanExpression.class));
  }

  @Override
  public void exitDateFunctionInvocation(DateFunctionInvocationContext ctx) {
    var parameters = this.stack.pop(ParsedArguments.class).getArgumentValues();
    this.stack.push(this.script.composeFunctionInvocation(ctx.functionInvocation().functionName.getText(), parameters, DateExpression.class));
  }

  @Override
  public void exitTimeFunctionInvocation(TimeFunctionInvocationContext ctx) {
    var parameters = this.stack.pop(ParsedArguments.class).getArgumentValues();
    this.stack.push(this.script.composeFunctionInvocation(ctx.functionInvocation().functionName.getText(), parameters, TimeExpression.class));
  }

  @Override
  public void exitDurationFunctionInvocation(DurationFunctionInvocationContext ctx) {
    var parameters = this.stack.pop(ParsedArguments.class).getArgumentValues();
    this.stack.push(this.script.composeFunctionInvocation(ctx.functionInvocation().functionName.getText(), parameters, DurationExpression.class));
  }

  // Map from EfxDataType to sequence expression types for function invocations
  private static final Map<Class<? extends EfxDataType>, Class<? extends SequenceExpression>> efxDataTypeToSequenceExpressionMap = Map.ofEntries(
      entry(EfxDataType.String.class, StringSequenceExpression.class),
      entry(EfxDataType.Boolean.class, BooleanSequenceExpression.class),
      entry(EfxDataType.Number.class, NumericSequenceExpression.class),
      entry(EfxDataType.Date.class, DateSequenceExpression.class),
      entry(EfxDataType.Time.class, TimeSequenceExpression.class),
      entry(EfxDataType.Duration.class, DurationSequenceExpression.class));

  @Override
  public void exitSequenceFromFunctionInvocation(SequenceFromFunctionInvocationContext ctx) {
    var arguments = this.stack.pop(StrictArguments.class);
    var function = (Function) arguments.identifier;
    var baseType = EfxTypeLattice.toPrimitive(function.dataType);
    var sequenceExpressionType = efxDataTypeToSequenceExpressionMap.get(baseType);
    if (sequenceExpressionType == null) {
      throw ConsistencyCheckException.missingTypeMapping(function.dataType, "efxDataTypeToSequenceExpressionMap");
    }
    this.stack.push(this.script.composeFunctionInvocation(
        ctx.functionInvocation().functionName.getText(),
        arguments.getArgumentValues(),
        sequenceExpressionType));
  }

  // #endregion New in EFX-2: Function invocation -----------------------------

  // #region Parameter Declarations -------------------------------------------


  @Override
  public void exitStringParameterDeclaration(StringParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), StringExpression.class);
  }

  @Override
  public void exitNumericParameterDeclaration(NumericParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), NumericExpression.class);
  }

  @Override
  public void exitBooleanParameterDeclaration(BooleanParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), BooleanExpression.class);
  }

  @Override
  public void exitDateParameterDeclaration(DateParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), DateExpression.class);
  }

  @Override
  public void exitTimeParameterDeclaration(TimeParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), TimeExpression.class);
  }

  @Override
  public void exitDurationParameterDeclaration(DurationParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), DurationExpression.class);
  }

  // Sequence parameter declarations
  @Override
  public void exitStringSequenceParameterDeclaration(StringSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), StringSequenceExpression.class);
  }

  @Override
  public void exitNumericSequenceParameterDeclaration(NumericSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), NumericSequenceExpression.class);
  }

  @Override
  public void exitBooleanSequenceParameterDeclaration(BooleanSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), BooleanSequenceExpression.class);
  }

  @Override
  public void exitDateSequenceParameterDeclaration(DateSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSequenceParameterDeclaration(TimeSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSequenceParameterDeclaration(DurationSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx.parameterName.getText(), DurationSequenceExpression.class);
  }

  private void exitParameterDeclaration(String parameterName, Class<? extends TypedExpression> parameterType) {
    if (this.expressionArguments.isEmpty()) {
      throw InvalidArgumentException.missingArgument(parameterName);
    }

    ParsedParameter parameter = new ParsedParameter(parameterName,
        this.translateArgument(this.expressionArguments.pop(), parameterType));
    this.stack.declareIdentifier(parameter);
  }

  // #endregion Parameter Declarations ----------------------------------------

  // #region Boolean functions ------------------------------------------------

  @Override
  public void exitNotFunction(NotFunctionContext ctx) {
    this.stack.push(this.script.composeLogicalNot(this.stack.pop(BooleanExpression.class)));
  }

  @Override
  public void exitBooleanFromNumberFunction(BooleanFromNumberFunctionContext ctx) {
    this.stack.push(this.script.composeToBooleanConversion(this.stack.pop(NumericExpression.class)));
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

  // #region Privacy settings ------------------------------------------------

  @Override
  public void exitFieldWasWithheldProperty(FieldWasWithheldPropertyContext ctx) {
    final String fieldId = getFieldId(ctx.fieldMention());
    if (this.isFieldRepeatableFromContext(fieldId, this.efxContext.peek())) {
      throw TypeMismatchException.fieldMayRepeat(fieldId, this.efxContext.symbol());
    }

    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(fieldId);
    }

    this.stack.push(this.composeWasWithheldCondition(fieldId, privacyCode));
  }

  @Override
  public void exitFieldIsWithheldProperty(FieldIsWithheldPropertyContext ctx) {
    final String fieldId = getFieldId(ctx.fieldMention());
    if (this.isFieldRepeatableFromContext(fieldId, this.efxContext.peek())) {
      throw TypeMismatchException.fieldMayRepeat(fieldId, this.efxContext.symbol());
    }

    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(fieldId);
    }

    this.stack.push(this.script.composeLogicalAnd(
        this.composeWasWithheldCondition(fieldId, privacyCode),
        this.composeStillWithheldCondition(fieldId)));
  }

  @Override
  public void exitFieldIsWithholdableProperty(FieldIsWithholdablePropertyContext ctx) {
    final String fieldId = getFieldId(ctx.fieldMention());
    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    final boolean isWithholdable = privacyCode != null && !privacyCode.isEmpty();
    this.stack.push(this.script.getBooleanEquivalent(isWithholdable));
  }

  @Override
  public void exitFieldIsDisclosedProperty(FieldIsDisclosedPropertyContext ctx) {
    final String fieldId = getFieldId(ctx.fieldMention());
    if (this.isFieldRepeatableFromContext(fieldId, this.efxContext.peek())) {
      throw TypeMismatchException.fieldMayRepeat(fieldId, this.efxContext.symbol());
    }

    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(fieldId);
    }

    // "isDisclosed" = "was withheld" AND NOT "still withheld" AND NOT "masked"
    this.stack.push(this.script.composeLogicalAnd(
        this.script.composeLogicalAnd(
            this.composeWasWithheldCondition(fieldId, privacyCode),
            this.script.composeLogicalNot(this.composeStillWithheldCondition(fieldId))),
        this.script.composeLogicalNot(this.composeIsMaskedCondition(fieldId))));
  }

  @Override
  public void exitFieldIsMaskedProperty(FieldIsMaskedPropertyContext ctx) {
    final String fieldId = getFieldId(ctx.fieldMention());
    if (this.isFieldRepeatableFromContext(fieldId, this.efxContext.peek())) {
      throw TypeMismatchException.fieldMayRepeat(fieldId, this.efxContext.symbol());
    }

    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(fieldId);
    }

    // "isMasked" = was withheld AND field value equals the privacy mask
    this.stack.push(this.script.composeLogicalAnd(
        this.composeWasWithheldCondition(fieldId, privacyCode),
        this.composeIsMaskedCondition(fieldId)));
  }

  @Override
  public void exitFieldPrivacyCodeProperty(FieldPrivacyCodePropertyContext ctx) {
    final String fieldId = getFieldId(ctx.fieldMention());
    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(fieldId);
    }
    this.stack.push(this.script.getStringLiteralFromUnquotedString(privacyCode));
  }

  private boolean isFieldRepeatableFromContext(String fieldId, Context context) {
    String contextNodeId = context.isFieldContext()
        ? this.symbols.getParentNodeOfField(context.symbol())
        : context.symbol();
    return this.symbols.isFieldRepeatableFromContext(fieldId, contextNodeId);
  }

  private BooleanExpression composeWasWithheldCondition(String fieldId, String privacyCode) {
    final String privacyCodeFieldId = this.symbols.getPrivacySettingOfField(fieldId, PrivacySetting.PRIVACY_CODE_FIELD);
    if (privacyCodeFieldId == null) {
      throw SdkInconsistencyException.missingPrivacyCodeField(fieldId);
    }

    return this.script.composeComparisonOperation(
        new StringExpression(this.script.composeFieldValueReference(
            this.symbols.getRelativePathOfField(privacyCodeFieldId, this.efxContext.symbol())).getScript()),
        "==",
        this.script.getStringLiteralFromUnquotedString(privacyCode));
  }

  private BooleanExpression composeStillWithheldCondition(String fieldId) {
    final String publicationDateFieldId = this.symbols.getPrivacySettingOfField(fieldId, PrivacySetting.PUBLICATION_DATE_FIELD);
    if (publicationDateFieldId == null) {
      throw SdkInconsistencyException.missingPublicationDateField(fieldId);
    }

    final PathExpression pubDateFieldPath = this.symbols.getRelativePathOfField(publicationDateFieldId,
        this.efxContext.symbol());

    return this.script.composeParenthesizedExpression(
        this.script.composeLogicalOr(
            this.script.composeLogicalNot(this.script.composeExistsCondition(pubDateFieldPath)),
            this.script.composeComparisonOperation(
                new DateExpression(this.script.composeFieldValueReference(pubDateFieldPath).getScript()), ">",
                this.script.getCurrentDate())),
        BooleanExpression.class);
  }

  private BooleanExpression composeIsMaskedCondition(String fieldId) {
    final String maskingValue = this.symbols.getPrivacyMask(fieldId);
    final PathExpression fieldValue = this.script.composeFieldValueReference(this.symbols.getRelativePathOfField(fieldId, this.efxContext.symbol()));

    if (!(fieldValue instanceof ScalarExpression)) {
      throw TypeMismatchException.fieldMayRepeat(fieldId, this.efxContext.symbol());
    }

    return this.script.composeComparisonOperation(
        TypedExpression.from(fieldValue, ScalarExpression.class),
        "==",
        this.getTypedLiteralFromUnquotedString(maskingValue, fieldValue.getDataType()));
  }

  private ScalarExpression getTypedLiteralFromUnquotedString(String value, Class<? extends EfxDataType> type) {
    if (EfxDataType.Number.class.isAssignableFrom(type)) {
      return this.script.getNumericLiteralEquivalent(value);
    }
    if (EfxDataType.Date.class.isAssignableFrom(type)) {
      return this.script.getDateLiteralEquivalent(value);
    }
    if (EfxDataType.Time.class.isAssignableFrom(type)) {
      return this.script.getTimeLiteralEquivalent(value);
    }
    return this.script.getStringLiteralFromUnquotedString(value);
  }

  // #endregion Privacy settings ---------------------------------------------

  // #region Sequence-equal ----------------------------------------------------

  @Override
  public void exitStringSequenceEqualFunction(StringSequenceEqualFunctionContext ctx) {
    exitSequenceEqualFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanSequenceEqualFunction(BooleanSequenceEqualFunctionContext ctx) {
    exitSequenceEqualFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericSequenceEqualFunction(NumericSequenceEqualFunctionContext ctx) {
    exitSequenceEqualFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateSequenceEqualFunction(DateSequenceEqualFunctionContext ctx) {
    exitSequenceEqualFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSequenceEqualFunction(TimeSequenceEqualFunctionContext ctx) {
    exitSequenceEqualFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSequenceEqualFunction(DurationSequenceEqualFunctionContext ctx) {
    exitSequenceEqualFunction(DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitSequenceEqualFunction(Class<T> sequenceType) {
    final T two = this.stack.pop(sequenceType);
    final T one = this.stack.pop(sequenceType);
    this.stack.push(this.script.composeSequenceEqualFunction(one, two));
  }

  // #endregion Sequence-equal -------------------------------------------------

  // #endregion Boolean functions ---------------------------------------------

  // #region Numeric functions ------------------------------------------------

  @Override
  public void exitCountStringsFunction(CountStringsFunctionContext ctx) {
    this.stack.push(this.script.composeCountOperation(this.stack.pop(StringSequenceExpression.class)));
  }

  @Override
  public void exitCountBooleansFunction(CountBooleansFunctionContext ctx) {
    this.stack.push(this.script.composeCountOperation(this.stack.pop(BooleanSequenceExpression.class)));
  }

  @Override
  public void exitCountNumbersFunction(CountNumbersFunctionContext ctx) {
    this.stack.push(this.script.composeCountOperation(this.stack.pop(NumericSequenceExpression.class)));
  }

  @Override
  public void exitCountDatesFunction(CountDatesFunctionContext ctx) {
    this.stack.push(this.script.composeCountOperation(this.stack.pop(DateSequenceExpression.class)));
  }

  @Override
  public void exitCountTimesFunction(CountTimesFunctionContext ctx) {
    this.stack.push(this.script.composeCountOperation(this.stack.pop(TimeSequenceExpression.class)));
  }

  @Override
  public void exitCountDurationsFunction(CountDurationsFunctionContext ctx) {
    this.stack.push(this.script.composeCountOperation(this.stack.pop(DurationSequenceExpression.class)));
  }

  @Override
  public void exitNumberFromStringFunction(NumberFromStringFunctionContext ctx) {
    this.stack.push(this.script.composeToNumberConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitNumberFromBooleanFunction(NumberFromBooleanFunctionContext ctx) {
    this.stack.push(this.script.composeToNumberConversion(this.stack.pop(BooleanExpression.class)));
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
  public void exitNumberToStringFunction(NumberToStringFunctionContext ctx) {
    this.stack.push(this.script.composeToStringConversion(this.stack.pop(NumericExpression.class)));
  }

  @Override
  public void exitBooleanToStringFunction(BooleanToStringFunctionContext ctx) {
    this.stack.push(this.script.composeToStringConversion(this.stack.pop(BooleanExpression.class)));
  }

  @Override
  public void exitDateToStringFunction(DateToStringFunctionContext ctx) {
    this.stack.push(this.script.composeToStringConversion(this.stack.pop(DateExpression.class)));
  }

  @Override
  public void exitTimeToStringFunction(TimeToStringFunctionContext ctx) {
    this.stack.push(this.script.composeToStringConversion(this.stack.pop(TimeExpression.class)));
  }

  @Override
  public void exitDurationToStringFunction(DurationToStringFunctionContext ctx) {
    this.stack.push(this.script.composeToStringConversion(this.stack.pop(DurationExpression.class)));
  }

  @Override
  public void exitConcatFunction(ConcatFunctionContext ctx) {
    if (this.stack.empty() || ctx.stringExpression().isEmpty()) {
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
    this.stack.push(this.script.getPreferredLanguage(this.stack.pop(MultilingualStringPath.class)));
  }

  @Override
  public void exitPreferredLanguageTextFunction(PreferredLanguageTextFunctionContext ctx) {
    this.stack.push(this.script.getTextInPreferredLanguage(this.stack.pop(MultilingualStringPath.class)));
  }

  @Override
  public void exitDictionaryLookup(DictionaryLookupContext ctx) {

    var dictionary = this.stack.getDictionary(ctx.dictionaryName.getText());
    this.stack.push(this.script.composeDictionaryLookup(
        dictionary.name, this.stack.pop(StringExpression.class),
        dictionary.type));

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

  // #region Distinct-values ---------------------------------------------------

  @Override
  public void exitStringDistinctValuesFunction(StringDistinctValuesFunctionContext ctx) {
    exitDistinctValuesFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanDistinctValuesFunction(BooleanDistinctValuesFunctionContext ctx) {
    exitDistinctValuesFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericDistinctValuesFunction(NumericDistinctValuesFunctionContext ctx) {
    exitDistinctValuesFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateDistinctValuesFunction(DateDistinctValuesFunctionContext ctx) {
    exitDistinctValuesFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeDistinctValuesFunction(TimeDistinctValuesFunctionContext ctx) {
    exitDistinctValuesFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationDistinctValuesFunction(DurationDistinctValuesFunctionContext ctx) {
    exitDistinctValuesFunction(DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitDistinctValuesFunction(Class<T> listType) {
    final T list = this.stack.pop(listType);
    this.stack.push(this.script.composeDistinctValuesFunction(list, listType));
  }

  // #endregion Distinct-values ------------------------------------------------

  // #region Union ------------------------------------------------------------

  @Override
  public void exitStringUnionFunction(StringUnionFunctionContext ctx) {
    exitUnionFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanUnionFunction(BooleanUnionFunctionContext ctx) {
    exitUnionFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericUnionFunction(NumericUnionFunctionContext ctx) {
    exitUnionFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateUnionFunction(DateUnionFunctionContext ctx) {
    exitUnionFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeUnionFunction(TimeUnionFunctionContext ctx) {
    exitUnionFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationUnionFunction(DurationUnionFunctionContext ctx) {
    exitUnionFunction(DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitUnionFunction(Class<T> listType) {
    final T two = this.stack.pop(listType);
    final T one = this.stack.pop(listType);
    this.stack.push(this.script.composeUnionFunction(one, two, listType));
  }

  // #endregion Union ---------------------------------------------------------

  // #region Intersect -------------------------------------------------------

  @Override
  public void exitStringIntersectFunction(StringIntersectFunctionContext ctx) {
    exitIntersectFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanIntersectFunction(BooleanIntersectFunctionContext ctx) {
    exitIntersectFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericIntersectFunction(NumericIntersectFunctionContext ctx) {
    exitIntersectFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateIntersectFunction(DateIntersectFunctionContext ctx) {
    exitIntersectFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeIntersectFunction(TimeIntersectFunctionContext ctx) {
    exitIntersectFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationIntersectFunction(DurationIntersectFunctionContext ctx) {
    exitIntersectFunction(DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitIntersectFunction(Class<T> listType) {
    final T two = this.stack.pop(listType);
    final T one = this.stack.pop(listType);
    this.stack.push(this.script.composeIntersectFunction(one, two, listType));
  }

  // #endregion Intersect ------------------------------------------------------

  // #region Except ----------------------------------------------------------

  @Override
  public void exitStringExceptFunction(StringExceptFunctionContext ctx) {
    exitExceptFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanExceptFunction(BooleanExceptFunctionContext ctx) {
    exitExceptFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericExceptFunction(NumericExceptFunctionContext ctx) {
    exitExceptFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateExceptFunction(DateExceptFunctionContext ctx) {
    exitExceptFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeExceptFunction(TimeExceptFunctionContext ctx) {
    exitExceptFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationExceptFunction(DurationExceptFunctionContext ctx) {
    exitExceptFunction(DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitExceptFunction(Class<T> listType) {
    final T two = this.stack.pop(listType);
    final T one = this.stack.pop(listType);
    this.stack.push(this.script.composeExceptFunction(one, two, listType));
  }

  // #endregion Except ---------------------------------------------------------

  // #endregion Sequence Functions --------------------------------------------

  // #region Helpers ----------------------------------------------------------
      
  protected static String getLexerSymbol(int tokenType) {
    return EfxLexer.VOCABULARY.getLiteralName(tokenType).replaceAll("^'|'$", "");
  }

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

  // Type name constants - made protected for reuse in subclasses
  protected static String textTypeName = getLexerSymbol(EfxLexer.Text);
  protected static String booleanTypeName = getLexerSymbol(EfxLexer.Indicator);
  protected static String numericTypeName = getLexerSymbol(EfxLexer.Number);
  protected static String dateTypeName = getLexerSymbol(EfxLexer.Date);
  protected static String timeTypeName = getLexerSymbol(EfxLexer.Time);
  protected static String durationTypeName = getLexerSymbol(EfxLexer.Measure);

  // Map from eForms field types to EFX type names - made protected for reuse in subclasses
  protected static final Map<String, String> eFormsToEfxTypeMap = Map.ofEntries( //
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

  // Map from Java EfxDataType classes to EFX type names - made protected for reuse in subclasses
  protected static final Map<Class<? extends EfxDataType>, String> javaToEfxTypeMap = Map.ofEntries(
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
    final ContextStack efxContext;
    
    ExpressionPreprocessor(String expression) {
      this(CharStreams.fromString(expression));
    }

    ExpressionPreprocessor(final CharStream charStream) {
      super();

      this.symbols = EfxExpressionTranslatorV2.this.symbols;
      this.errorListener = EfxExpressionTranslatorV2.this.errorListener;
      this.efxContext = new ContextStack(this.symbols);

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

    // #region Context tracking -----------------------------------------------

    @Override
    public void enterSingleExpression(SingleExpressionContext ctx) {
      final TerminalNode fieldContext = ctx.FieldId();
      if (fieldContext != null) {
        this.efxContext.pushFieldContext(fieldContext.getText());
      } else {
        final TerminalNode nodeContext = ctx.NodeId();
        if (nodeContext != null) {
          this.efxContext.pushNodeContext(nodeContext.getText());
        } else {
          final TerminalNode alias = ctx.Identifier();
          if (alias != null) {
            String fieldId = this.symbols.getFieldIdFromAlias(alias.getText());
            if (fieldId != null) {
              this.efxContext.pushFieldContext(fieldId);
            } else {
              String nodeId = this.symbols.getNodeIdFromAlias(alias.getText());
              if (nodeId != null) {
                this.efxContext.pushNodeContext(nodeId);
              } else {
                throw SymbolResolutionException.unknownSymbol(alias.getText());
              }
            }
          }
        }
      }
    }

    @Override
    public void exitSingleExpression(SingleExpressionContext ctx) {
      this.efxContext.pop();
    }

    @Override
    public void enterPredicate(EfxParser.PredicateContext ctx) {
      var parent = ctx.getParent();
      if (parent instanceof NodeReferenceWithPredicateContext) {
        final String nodeId = getNodeId((NodeReferenceWithPredicateContext) parent);
        this.efxContext.pushNodeContext(nodeId);
      } else if (parent instanceof FieldReferenceWithPredicateContext) {
        final String fieldId = getFieldId((FieldReferenceWithPredicateContext) parent);
        this.efxContext.pushFieldContext(fieldId);
      } else {
        throw new ParseCancellationException("Unexpected parent context for predicate: " + parent.getClass().getSimpleName());
      }
    }

    @Override
    public void exitPredicate(EfxParser.PredicateContext ctx) {
      this.efxContext.pop();
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

    @Override
    public void enterFieldReferenceInOtherNotice(FieldReferenceInOtherNoticeContext ctx) {
      if (ctx.noticeReference() != null) {
        this.efxContext.push(null);
      }
    }

    @Override
    public void exitFieldReferenceInOtherNotice(EfxParser.FieldReferenceInOtherNoticeContext ctx) {
      if (ctx.noticeReference() != null) {
        this.efxContext.pop();
      }
    }

    @Override
    public void exitContextFieldSpecifier(ContextFieldSpecifierContext ctx) {
      final String contextFieldId = getFieldId(ctx.fieldContext());
      this.efxContext
          .push(new FieldContext(contextFieldId, this.symbols.getAbsolutePathOfField(contextFieldId),
              this.symbols.getRelativePathOfField(contextFieldId, this.efxContext.symbol())));
    }

    @Override
    public void exitFieldReferenceWithFieldContextOverride(
        FieldReferenceWithFieldContextOverrideContext ctx) {
      if (ctx.contextFieldSpecifier() != null) {
        this.efxContext.pop();
      }
    }

    @Override
    public void exitContextNodeSpecifier(ContextNodeSpecifierContext ctx) {
      final String contextNodeId = getNodeId(ctx.node);
      this.efxContext
          .push(new NodeContext(contextNodeId, this.symbols.getAbsolutePathOfNode(contextNodeId),
              this.symbols.getRelativePathOfNode(contextNodeId, this.efxContext.symbol())));
    }

    @Override
    public void exitFieldReferenceWithNodeContextOverride(
        FieldReferenceWithNodeContextOverrideContext ctx) {
      if (ctx.contextNodeSpecifier() != null) {
        this.efxContext.pop();
      }
    }

    @Override
    public void exitContextVariableSpecifier(ContextVariableSpecifierContext ctx) {
      String variableName = ctx.variableReference().variableName.getText();
      Context variableContext = this.efxContext.getContextFromVariable(variableName);
      if (variableContext == null) {
        throw InvalidIdentifierException.notAContextVariable(variableName);
      }
      if (variableContext.isFieldContext()) {
        this.efxContext.push(new FieldContext(variableContext.symbol(),
            this.symbols.getAbsolutePathOfField(variableContext.symbol()), this.symbols
                .getRelativePathOfField(variableContext.symbol(), this.efxContext.symbol())));
      } else if (variableContext.isNodeContext()) {
        this.efxContext.push(new NodeContext(variableContext.symbol(),
            this.symbols.getAbsolutePathOfNode(variableContext.symbol()), this.symbols
                .getRelativePathOfNode(variableContext.symbol(), this.efxContext.symbol())));
      } else {
        assert false : "Context variable must be either a field or node context: " + variableName;
      }
    }

    @Override
    public void exitFieldReferenceWithVariableContextOverride(
        FieldReferenceWithVariableContextOverrideContext ctx) {
      if (ctx.contextVariableSpecifier() != null) {
        this.efxContext.pop();
      }
    }

    // #endregion Context tracking --------------------------------------------

    @Override
    public void exitScalarFromFieldReference(ScalarFromFieldReferenceContext ctx) {
      String fieldId = getFieldId(ctx.fieldReference());
      String fieldType = eFormsToEfxTypeMap.get(this.symbols.getTypeOfField(fieldId));

      // Skip repeatability check if context IS this field (e.g., inside a predicate on this field).
      // In that case, we're referencing the current element being iterated, not the whole sequence.
      if (this.efxContext.isEmpty() || !fieldId.equals(this.efxContext.symbol())) {
        String contextNodeId = getContextNodeId();
        if (this.symbols.isFieldRepeatableFromContext(fieldId, contextNodeId)) {
          // B2 Solution 3: If we're in a top-level expression context where sequences
          // are valid (lateBoundScalar → lateBoundExpression → expression), auto-insert
          // sequence cast instead of throwing.
          if (isTopLevelLateBoundExpression(ctx)) {
            this.rewriter.insertBefore(ctx.getStart(), "(" + fieldType + "*)");
            return;
          }
          throw TypeMismatchException.fieldMayRepeat(fieldId, this.efxContext.symbol());
        }
      }

      if (!hasParentContextOfType(ctx, LateBoundScalarContext.class)) {
        return;
      }

      // Insert the type cast
      this.rewriter.insertBefore(ctx.getStart(), "(" + fieldType + ")");
    }

    /**
     * Gets the node ID of the current context for use with isFieldRepeatableFromContext.
     * If the context is a NodeContext, returns the node ID directly.
     * If the context is a FieldContext, returns the parent node of that field.
     * If the context is empty/null, returns null (root context).
     */
    private String getContextNodeId() {
      if (this.efxContext.isEmpty() || this.efxContext.peek() == null) {
        return null;
      }
      Context context = this.efxContext.peek();
      if (context.isNodeContext()) {
        return context.symbol();
      } else {
        // FieldContext - get the parent node of the field
        return this.symbols.getParentNodeOfField(context.symbol());
      }
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
    public void exitScalarFromFunctionInvocation(ScalarFromFunctionInvocationContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundScalarContext.class)) {
        return;
      }

      String functionName = ctx.functionInvocation().functionName.getText();
      String functionType = javaToEfxTypeMap.get(EfxTypeLattice.toPrimitive(this.stack.getTypeOfIdentifier(functionName)));

      if (functionType != null) {
        // Insert the type cast
        this.rewriter.insertBefore(ctx.functionInvocation().FunctionPrefix().getSymbol(), "(" + functionType + ")");
      }
    }

    @Override
    public void exitSequenceFromFieldReference(SequenceFromFieldReferenceContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundSequenceContext.class)) {
        return;
      }

      // Find the referenced field and get its type
      String fieldId = getFieldId(ctx.fieldReference());
      String fieldType = eFormsToEfxTypeMap.get(this.symbols.getTypeOfField(fieldId));

      if (fieldType != null) {
        // Insert the sequence type cast (type*)
        this.rewriter.insertBefore(ctx.getStart(), "(" + fieldType + "*)");
      }
    }

    @Override
    public void exitSequenceFromAttributeReference(SequenceFromAttributeReferenceContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundSequenceContext.class)) {
        return;
      }

      // Insert the sequence type cast (text*)
      this.rewriter.insertBefore(ctx.getStart(), "(" + textTypeName + "*)");
    }

    @Override
    public void exitSequenceFromFunctionInvocation(SequenceFromFunctionInvocationContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundSequenceContext.class)) {
        return;
      }

      String functionName = ctx.functionInvocation().functionName.getText();
      String functionType = javaToEfxTypeMap.get(EfxTypeLattice.toPrimitive(this.stack.getTypeOfIdentifier(functionName)));

      if (functionType != null) {
        // Insert the sequence type cast (type*)
        this.rewriter.insertBefore(ctx.functionInvocation().FunctionPrefix().getSymbol(), "(" + functionType + "*)");
      }
    }

    @Override
    public void exitScalarFromVariableReference(ScalarFromVariableReferenceContext ctx) {
      String variableName = ctx.variableReference().variableName.getText();
      Context variableContext = this.efxContext.getContextFromVariable(variableName);

      // Guard: Node context variables cannot be used as values
      if (variableContext != null && variableContext.isNodeContext()) {
        throw TypeMismatchException.nodesHaveNoValue(variableName, variableContext.symbol());
      }

      // Guard: Field context variables must not be repeatable in scalar context
      if (variableContext != null && variableContext.isFieldContext()) {
        String fieldId = variableContext.symbol();
        if (this.symbols.isFieldRepeatableFromContext(fieldId, getContextNodeId())) {
          throw TypeMismatchException.fieldMayRepeat(fieldId, this.efxContext.symbol());
        }
      }

      // Guard: Skip type cast insertion if not in late-bound context (explicit cast already present)
      if (!hasParentContextOfType(ctx, LateBoundScalarContext.class)) {
        return;
      }

      // Determine type for cast: field type for context variables, variable type for regular variables
      String typeCast = (variableContext != null)
          ? eFormsToEfxTypeMap.get(this.symbols.getTypeOfField(variableContext.symbol()))
          : javaToEfxTypeMap.get(EfxTypeLattice.toPrimitive(this.stack.getTypeOfIdentifier(variableName)));

      if (typeCast != null) {
        this.rewriter.insertBefore(ctx.variableReference().VariablePrefix().getSymbol(), "(" + typeCast + ")");
      }
    }

    @Override
    public void exitSequenceFromVariableReference(SequenceFromVariableReferenceContext ctx) {
      String variableName = ctx.variableReference().variableName.getText();
      Context variableContext = this.efxContext.getContextFromVariable(variableName);

      // Guard: Node context variables cannot be used as values
      if (variableContext != null && variableContext.isNodeContext()) {
        throw TypeMismatchException.nodesHaveNoValue(variableName, variableContext.symbol());
      }

      // No repeatability check needed - sequences can have multiple values

      // Guard: Skip type cast insertion if not in late-bound context (explicit cast already present)
      if (!hasParentContextOfType(ctx, LateBoundSequenceContext.class)) {
        return;
      }

      // Determine type for cast: field type for context variables, variable type for regular variables
      String typeCast = (variableContext != null)
          ? eFormsToEfxTypeMap.get(this.symbols.getTypeOfField(variableContext.symbol()))
          : javaToEfxTypeMap.get(EfxTypeLattice.toPrimitive(this.stack.getTypeOfIdentifier(variableName)));

      if (typeCast != null) {
        this.rewriter.insertBefore(ctx.variableReference().VariablePrefix().getSymbol(), "(" + typeCast + "*)");
      }
    }

    @Override
    public void exitDictionaryLookup(DictionaryLookupContext ctx) {
      if (!hasParentContextOfType(ctx, LateBoundScalarContext.class)) {
        return;
      }

      String dictionaryName = ctx.dictionaryName.getText();
      String dictionaryType = javaToEfxTypeMap.get(EfxTypeLattice.toPrimitive(this.stack.getTypeOfIdentifier(dictionaryName)));

      if (dictionaryType != null) {
        // Insert the type cast
        this.rewriter.insertBefore(ctx.VariablePrefix().getSymbol(), "(" + dictionaryType + ")");
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

    /**
     * Checks if we're in a top-level late-bound expression context where sequences are valid.
     * This is the path: lateBoundScalar → lateBoundExpression → expression
     * In this path, the grammar allows either scalars or sequences, so a repeatable field
     * can be auto-cast to sequence instead of throwing an error.
     */
    private boolean isTopLevelLateBoundExpression(ParserRuleContext ctx) {
      // Walk up to find LateBoundScalarContext
      ParserRuleContext current = ctx;
      while (current != null && !(current instanceof LateBoundScalarContext)) {
        current = current.getParent();
      }
      if (current == null) {
        return false;
      }
      // Check if LateBoundScalarContext's parent is LateBoundExpressionContext
      ParserRuleContext parent = current.getParent();
      if (!(parent instanceof LateBoundExpressionContext)) {
        return false;
      }
      // Check if LateBoundExpressionContext's parent is ExpressionContext
      ParserRuleContext grandparent = parent.getParent();
      return grandparent instanceof ExpressionContext;
    }

    // #region Variable declarations ------------------------------------------

    @Override
    public void exitStringIteratorVariableDeclaration(StringIteratorVariableDeclarationContext ctx) {
      String variableName = ctx.variableName.getText();
      this.stack.declareIdentifier(new Variable(variableName,  StringExpression.empty(), StringExpression.empty()));
    }
  
    @Override
    public void exitBooleanIteratorVariableDeclaration(BooleanIteratorVariableDeclarationContext ctx) {
      String variableName = ctx.variableName.getText();
      this.stack.declareIdentifier(new Variable(variableName, BooleanExpression.empty(), BooleanExpression.empty()));
    }
  
    @Override
    public void exitNumericIteratorVariableDeclaration(NumericIteratorVariableDeclarationContext ctx) {
      String variableName = ctx.variableName.getText();
      this.stack.declareIdentifier(new Variable(variableName, NumericExpression.empty(), NumericExpression.empty()));
    }
  
    @Override
    public void exitDateIteratorVariableDeclaration(DateIteratorVariableDeclarationContext ctx) {
      String variableName = ctx.variableName.getText();
      this.stack.declareIdentifier(new Variable(variableName, DateExpression.empty(), DateExpression.empty()));
    }
  
    @Override
    public void exitTimeIteratorVariableDeclaration(TimeIteratorVariableDeclarationContext ctx) {
      String variableName = ctx.variableName.getText();
      this.stack.declareIdentifier(new Variable(variableName, TimeExpression.empty(), TimeExpression.empty()));
    }
  
    @Override
    public void exitDurationIteratorVariableDeclaration(DurationIteratorVariableDeclarationContext ctx) {
      String variableName = ctx.variableName.getText();
      this.stack.declareIdentifier(new Variable(variableName, DurationExpression.empty(), DurationExpression.empty()));
    }

    @Override
    public void exitContextIteratorExpression(ContextIteratorExpressionContext ctx) {
      var variableName = ctx.contextIteratorVariableDeclaration().variableName.getText();
      this.stack.declareIdentifier(new Variable(variableName, DurationExpression.empty(), DurationExpression.empty()));

      // Declare context variable for context tracking
      if (ctx.fieldContext() != null) {
        final String contextFieldId = getFieldId(ctx.fieldContext());
        this.efxContext.declareContextVariable(variableName,
            new FieldContext(contextFieldId, this.symbols.getAbsolutePathOfField(contextFieldId),
                this.symbols.getRelativePathOfField(contextFieldId, this.efxContext.symbol())));
      } else if (ctx.nodeContext() != null) {
        final String contextNodeId = getNodeId(ctx.nodeContext());
        this.efxContext.declareContextVariable(variableName,
            new NodeContext(contextNodeId, this.symbols.getAbsolutePathOfNode(contextNodeId),
                this.symbols.getRelativePathOfNode(contextNodeId, this.efxContext.symbol())));
      }
    }

    // #endregion Variable declarations ---------------------------------------

    // #region Parameter declarations -----------------------------------------

    @Override
    public void exitStringParameterDeclaration(StringParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, StringExpression.empty()));
    }

    @Override
    public void exitNumericParameterDeclaration(NumericParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, NumericExpression.empty()));
    }

    @Override
    public void exitBooleanParameterDeclaration(BooleanParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, BooleanExpression.empty()));
    }

    @Override
    public void exitDateParameterDeclaration(DateParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, DateExpression.empty()));
    }

    @Override
    public void exitTimeParameterDeclaration(TimeParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, TimeExpression.empty()));
    }

    @Override
    public void exitDurationParameterDeclaration(DurationParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, DurationExpression.empty()));
    }

    // Sequence parameter declarations
    @Override
    public void exitStringSequenceParameterDeclaration(StringSequenceParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, new StringSequenceExpression("")));
    }

    @Override
    public void exitNumericSequenceParameterDeclaration(NumericSequenceParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, new NumericSequenceExpression("")));
    }

    @Override
    public void exitBooleanSequenceParameterDeclaration(BooleanSequenceParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, new BooleanSequenceExpression("")));
    }

    @Override
    public void exitDateSequenceParameterDeclaration(DateSequenceParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, new DateSequenceExpression("")));
    }

    @Override
    public void exitTimeSequenceParameterDeclaration(TimeSequenceParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, new TimeSequenceExpression("")));
    }

    @Override
    public void exitDurationSequenceParameterDeclaration(DurationSequenceParameterDeclarationContext ctx) {
      String identifier = ctx.parameterName.getText();
      this.stack.declareIdentifier(new ParsedParameter(identifier, new DurationSequenceExpression("")));
    }

    // #endregion Parameter declarations --------------------------------------

    // #region Function declarations -----------------------------------------

    @Override
    public void exitStringFunctionDeclaration(StringFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), StringExpression.empty()));
    }

    @Override
    public void exitNumericFunctionDeclaration(NumericFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), NumericExpression.empty()));
    }

    @Override
    public void exitBooleanFunctionDeclaration(BooleanFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), BooleanExpression.empty()));
    }


    @Override
    public void exitDateFunctionDeclaration(DateFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), DateExpression.empty()));
    }


    @Override
    public void exitTimeFunctionDeclaration(TimeFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), TimeExpression.empty()));
    }

    @Override
    public void exitDurationFunctionDeclaration(DurationFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), DurationExpression.empty()));
    }

    // Sequence function declarations

    @Override
    public void exitStringSequenceFunctionDeclaration(StringSequenceFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), new StringSequenceExpression("")));
    }

    @Override
    public void exitNumericSequenceFunctionDeclaration(NumericSequenceFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), new NumericSequenceExpression("")));
    }

    @Override
    public void exitBooleanSequenceFunctionDeclaration(BooleanSequenceFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), new BooleanSequenceExpression("")));
    }

    @Override
    public void exitDateSequenceFunctionDeclaration(DateSequenceFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), new DateSequenceExpression("")));
    }

    @Override
    public void exitTimeSequenceFunctionDeclaration(TimeSequenceFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), new TimeSequenceExpression("")));
    }

    @Override
    public void exitDurationSequenceFunctionDeclaration(DurationSequenceFunctionDeclarationContext ctx) {
      String functionName = ctx.functionName.getText();
      this.stack.declareFunction(new Function(functionName, new ParsedParameters(), new DurationSequenceExpression("")));
    }

    // #endregion Function declarations ---------------------------------------

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
