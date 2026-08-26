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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
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
import eu.europa.ted.efx.exceptions.TranslatorConfigurationException;
import eu.europa.ted.efx.util.EfxRegexValidator;
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
import eu.europa.ted.efx.model.variables.Variable;
import eu.europa.ted.efx.sdk2.EfxParser.*;

/**
 * The goal of the EfxExpressionTranslator is to take an EFX expression and translate it to a
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

  /**
   * Tracks the expected cardinality (scalar vs. sequence) as the tree walker enters late-bound
   * expression nodes. Methods that resolve field, variable, function, or dictionary references
   * consult the top of this stack to determine whether to push a scalar or sequence expression.
   *
   * @see #currentCardinalityResolutionContext()
   */
  private final Deque<CardinalityResolutionContext> cardinalityResolutionStack = new ArrayDeque<>();

  protected EfxExpressionTranslatorV2() {}

  public EfxExpressionTranslatorV2(final SymbolResolver symbolResolver,
      final ScriptGenerator scriptGenerator, final BaseErrorListener errorListener) {
    this.symbols = symbolResolver;
    this.script = scriptGenerator;
    this.errorListener = errorListener;

    this.efxContext = new ContextStack(symbols);
  }

  @Override
  public void enterEveryRule(final ParserRuleContext ctx) {
    this.stack.pushContext(ctx);
  }

  @Override
  public void exitEveryRule(final ParserRuleContext ctx) {
    this.stack.popContext();
  }

  @Override
  public String translateExpression(final String expression, final String... arguments) {
    this.expressionArguments.addAll(Arrays.asList(arguments));

    final EfxLexer lexer = new EfxLexer(CharStreams.fromString(expression));
    final CommonTokenStream tokens = new CommonTokenStream(lexer);
    final EfxParser parser = new EfxParser(tokens);
    parser.setErrorHandler(new EfxErrorStrategy());

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
    parser.setErrorHandler(new EfxErrorStrategy());

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
      throw TranslatorConfigurationException.unhandledLinkedFieldProperty(ctx.getText());
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

  protected String getFieldId(FieldMentionContext ctx) {
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
      return getFieldId(ctx.absoluteFieldReference());
    }

    if (ctx.fieldReferenceWithVariableContextOverride() != null) {
      return getFieldId(ctx.fieldReferenceWithVariableContextOverride());
    }
    assert false : "Unexpected context type for field reference: " + ctx.getClass().getSimpleName();
    return null;
  }

  protected String getFieldId(AbsoluteFieldReferenceContext ctx) {
    if (ctx == null) {
      return null;
    }
    return getFieldId(ctx.reference.reference);
  }

    protected String getFieldId(FieldReferenceWithVariableContextOverrideContext ctx) {
    if (ctx == null) {
      return null;
    }
    return getFieldId(ctx.reference.reference.reference.reference);
  }

  protected String getFieldId(FieldContextContext ctx) {
    if (ctx == null) {
      return null;
    }

    if (ctx.absoluteFieldReference() != null) {
      return getFieldId(ctx.absoluteFieldReference());
    }

    if (ctx.fieldReferenceWithPredicate() != null) {
      return getFieldId(ctx.fieldReferenceWithPredicate());
    }

    assert false : "Unexpected context type for field reference: " + ctx.getClass().getSimpleName();
    return null;
  }

  protected String getFieldId(FieldReferenceWithPredicateContext ctx) {
    if (ctx == null) {
      return null;
    }
    return getFieldId(ctx.linkedFieldReference());
  }

  protected static String getNodeId(NodeReferenceContext ctx) {
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
      ParenthesizedBooleanExpressionContext ctx) {
    this.stack.push(this.script.composeParenthesizedExpression(
        this.stack.pop(BooleanExpression.class), BooleanExpression.class));
  }

  @Override
  public void exitLogicalAndCondition(LogicalAndConditionContext ctx) {
    this.exitLogicalAndCondition();
  }

  @Override
  public void exitLateBoundLogicalAndConditionRight(LateBoundLogicalAndConditionRightContext ctx) {
    this.exitLogicalAndCondition();
  }

  @Override
  public void exitLateBoundLogicalAndConditionLeft(LateBoundLogicalAndConditionLeftContext ctx) {
    this.exitLogicalAndCondition();
  }

  @Override
  public void exitLateBoundLogicalAndCondition(LateBoundLogicalAndConditionContext ctx) {
    this.exitLogicalAndCondition();
  }

  private void exitLogicalAndCondition() {
    BooleanExpression right = this.stack.pop(BooleanExpression.class);
    BooleanExpression left = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeLogicalAnd(left, right));
  }

  @Override
  public void exitLogicalOrCondition(LogicalOrConditionContext ctx) {
    this.exitLogicalOrCondition();
  }

  @Override
  public void exitLateBoundLogicalOrConditionRight(LateBoundLogicalOrConditionRightContext ctx) {
    this.exitLogicalOrCondition();
  }

  @Override
  public void exitLateBoundLogicalOrConditionLeft(LateBoundLogicalOrConditionLeftContext ctx) {
    this.exitLogicalOrCondition();
  }

  @Override
  public void exitLateBoundLogicalOrCondition(LateBoundLogicalOrConditionContext ctx) {
    this.exitLogicalOrCondition();
  }

  private void exitLogicalOrCondition() {
    BooleanExpression right = this.stack.pop(BooleanExpression.class);
    BooleanExpression left = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeLogicalOr(left, right));
  }

  // #region Boolean expressions - Comparisons --------------------------------

  @Override
  public void exitStringComparison(StringComparisonContext ctx) {
    this.exitComparison(ctx.operator.getText(), StringExpression.class);
  }

  @Override
  public void exitLateBoundStringComparisonLeft(LateBoundStringComparisonLeftContext ctx) {
    this.exitComparison(ctx.operator.getText(), StringExpression.class);
  }

  @Override
  public void exitLateBoundStringComparisonRight(LateBoundStringComparisonRightContext ctx) {
    this.exitComparison(ctx.operator.getText(), StringExpression.class);
  }

  @Override
  public void exitNumericComparison(NumericComparisonContext ctx) {
    this.exitComparison(ctx.operator.getText(), NumericExpression.class);
  }

  @Override
  public void exitLateBoundNumericComparisonLeft(LateBoundNumericComparisonLeftContext ctx) {
    this.exitComparison(ctx.operator.getText(), NumericExpression.class);
  }

  @Override
  public void exitLateBoundNumericComparisonRight(LateBoundNumericComparisonRightContext ctx) {
    this.exitComparison(ctx.operator.getText(), NumericExpression.class);
  }

  @Override
  public void exitBooleanComparison(BooleanComparisonContext ctx) {
    this.exitComparison(ctx.operator.getText(), BooleanExpression.class);
  }

  @Override
  public void exitLateBoundBooleanComparisonLeft(LateBoundBooleanComparisonLeftContext ctx) {
    this.exitComparison(ctx.operator.getText(), BooleanExpression.class);
  }

  @Override
  public void exitLateBoundBooleanComparisonRight(LateBoundBooleanComparisonRightContext ctx) {
    this.exitComparison(ctx.operator.getText(), BooleanExpression.class);
  }

  @Override
  public void exitDateComparison(DateComparisonContext ctx) {
    this.exitComparison(ctx.operator.getText(), DateExpression.class);
  }

  @Override
  public void exitLateBoundDateComparisonLeft(LateBoundDateComparisonLeftContext ctx) {
    this.exitComparison(ctx.operator.getText(), DateExpression.class);
  }

  @Override
  public void exitLateBoundDateComparisonRight(LateBoundDateComparisonRightContext ctx) {
    this.exitComparison(ctx.operator.getText(), DateExpression.class);
  }

  @Override
  public void exitTimeComparison(TimeComparisonContext ctx) {
    this.exitComparison(ctx.operator.getText(), TimeExpression.class);
  }

  @Override
  public void exitLateBoundTimeComparisonLeft(LateBoundTimeComparisonLeftContext ctx) {
    this.exitComparison(ctx.operator.getText(), TimeExpression.class);
  }

  @Override
  public void exitLateBoundTimeComparisonRight(LateBoundTimeComparisonRightContext ctx) {
    this.exitComparison(ctx.operator.getText(), TimeExpression.class);
  }

  @Override
  public void exitDurationComparison(DurationComparisonContext ctx) {
    this.exitComparison(ctx.operator.getText(), DurationExpression.class);
  }

  @Override
  public void exitLateBoundDurationComparisonLeft(LateBoundDurationComparisonLeftContext ctx) {
    this.exitComparison(ctx.operator.getText(), DurationExpression.class);
  }

  @Override
  public void exitLateBoundDurationComparisonRight(LateBoundDurationComparisonRightContext ctx) {
    this.exitComparison(ctx.operator.getText(), DurationExpression.class);
  }

  @Override
  public void exitLateBoundComparison(LateBoundComparisonContext ctx) {
    this.exitComparison(ctx.operator.getText(), this.resolveScalarType(this.stack.peekType(-1)));
  }

  private <T extends ScalarExpression> void exitComparison(String operator, Class<T> type) {
    T right = this.stack.pop(type);
    T left = this.stack.pop(type);
    this.stack.push(this.script.composeComparisonOperation(left, operator, right));
  }

  // #endregion Boolean expressions - Comparisons -----------------------------

  // #region Boolean expressions - Conditions --------------------------------

  @Override
  public void exitStringEmptyFunction(StringEmptyFunctionContext ctx) {
    StringExpression expression = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeComparisonOperation(expression, "==",
        this.script.getStringLiteralFromUnquotedString("")));
  }

  @Override
  public void exitPresenceCondition(PresenceConditionContext ctx) {
    PathExpression reference = this.stack.pop(PathExpression.class);
    if (ctx.modifier != null && ctx.modifier.getType() == EfxLexer.Not) {
      this.stack.push(this.script.composeLogicalNot(this.script.composeExistsCondition(reference)));
    } else {
      this.stack.push(this.script.composeExistsCondition(reference));
    }
  }

  @Override
  public void exitStringUniqueValueCondition(StringUniqueValueConditionContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringUniqueValueConditionRight(LateBoundStringUniqueValueConditionRightContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringUniqueValueConditionLeft(LateBoundStringUniqueValueConditionLeftContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitNumericUniqueValueCondition(NumericUniqueValueConditionContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericUniqueValueConditionRight(LateBoundNumericUniqueValueConditionRightContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericUniqueValueConditionLeft(LateBoundNumericUniqueValueConditionLeftContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitBooleanUniqueValueCondition(BooleanUniqueValueConditionContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanUniqueValueConditionRight(LateBoundBooleanUniqueValueConditionRightContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanUniqueValueConditionLeft(LateBoundBooleanUniqueValueConditionLeftContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitDateUniqueValueCondition(DateUniqueValueConditionContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateUniqueValueConditionRight(LateBoundDateUniqueValueConditionRightContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateUniqueValueConditionLeft(LateBoundDateUniqueValueConditionLeftContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeUniqueValueCondition(TimeUniqueValueConditionContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeUniqueValueConditionRight(LateBoundTimeUniqueValueConditionRightContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeUniqueValueConditionLeft(LateBoundTimeUniqueValueConditionLeftContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationUniqueValueCondition(DurationUniqueValueConditionContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, DurationExpression.class, DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationUniqueValueConditionRight(LateBoundDurationUniqueValueConditionRightContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, DurationExpression.class, DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationUniqueValueConditionLeft(LateBoundDurationUniqueValueConditionLeftContext ctx) {
    this.exitUniqueValueCondition(ctx.modifier, DurationExpression.class, DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundUniqueValueCondition(LateBoundUniqueValueConditionContext ctx) {
    TypedExpression needle = this.stack.peekType(-1);
    this.exitUniqueValueCondition(ctx.modifier, this.resolveScalarType(needle), this.resolveSequenceType(needle));
  }

  private void exitUniqueValueCondition(Token modifier,
      Class<? extends ScalarExpression> scalarType, Class<? extends SequenceExpression> sequenceType) {
    SequenceExpression haystack = this.stack.pop(sequenceType);
    ScalarExpression needle = this.stack.pop(scalarType);
    BooleanExpression condition = this.dispatchComposeUniqueValueCondition(needle, haystack);
    if (modifier != null && modifier.getType() == EfxLexer.Not) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  private BooleanExpression dispatchComposeUniqueValueCondition(ScalarExpression needle,
      SequenceExpression haystack) {
    if (needle instanceof NumericExpression) {
      return this.script.composeUniqueValueCondition(
          (NumericExpression) needle, (NumericSequenceExpression) haystack);
    } else if (needle instanceof BooleanExpression) {
      return this.script.composeUniqueValueCondition(
          (BooleanExpression) needle, (BooleanSequenceExpression) haystack);
    } else if (needle instanceof DateExpression) {
      return this.script.composeUniqueValueCondition(
          (DateExpression) needle, (DateSequenceExpression) haystack);
    } else if (needle instanceof TimeExpression) {
      return this.script.composeUniqueValueCondition(
          (TimeExpression) needle, (TimeSequenceExpression) haystack);
    } else if (needle instanceof DurationExpression) {
      return this.script.composeUniqueValueCondition(
          (DurationExpression) needle, (DurationSequenceExpression) haystack);
    } else if (needle instanceof StringExpression) {
      return this.script.composeUniqueValueCondition(
          (StringExpression) needle, (StringSequenceExpression) haystack);
    } else {
      throw TranslatorConfigurationException.missingTypeMapping(
          needle.getDataType(), "dispatchComposeUniqueValueCondition");
    }
  }

  @Override
  public void exitStringSequenceEmptinessCondition(
      StringSequenceEmptinessConditionContext ctx) {
    this.exitSequenceEmptinessCondition(StringSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitBooleanSequenceEmptinessCondition(
      BooleanSequenceEmptinessConditionContext ctx) {
    this.exitSequenceEmptinessCondition(BooleanSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitNumericSequenceEmptinessCondition(
      NumericSequenceEmptinessConditionContext ctx) {
    this.exitSequenceEmptinessCondition(NumericSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitDateSequenceEmptinessCondition(
      DateSequenceEmptinessConditionContext ctx) {
    this.exitSequenceEmptinessCondition(DateSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitTimeSequenceEmptinessCondition(
      TimeSequenceEmptinessConditionContext ctx) {
    this.exitSequenceEmptinessCondition(TimeSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitDurationSequenceEmptinessCondition(
      DurationSequenceEmptinessConditionContext ctx) {
    this.exitSequenceEmptinessCondition(DurationSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitLateBoundEmptinessCondition(LateBoundEmptinessConditionContext ctx) {
    this.exitSequenceEmptinessCondition(
        this.resolveSequenceType(this.stack.peekType()), ctx.modifier);
  }

  private <T extends SequenceExpression> void exitSequenceEmptinessCondition(
      Class<T> sequenceType, Token modifier) {
    final T sequence = this.stack.pop(sequenceType);
    BooleanExpression condition = this.script.composeEmptySequenceCondition(sequence);
    if (modifier != null && modifier.getType() == EfxLexer.Not) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  @Override
  public void exitStringSequenceDistinctCondition(
      StringSequenceDistinctConditionContext ctx) {
    this.exitSequenceDistinctCondition(StringSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitBooleanSequenceDistinctCondition(
      BooleanSequenceDistinctConditionContext ctx) {
    this.exitSequenceDistinctCondition(BooleanSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitNumericSequenceDistinctCondition(
      NumericSequenceDistinctConditionContext ctx) {
    this.exitSequenceDistinctCondition(NumericSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitDateSequenceDistinctCondition(
      DateSequenceDistinctConditionContext ctx) {
    this.exitSequenceDistinctCondition(DateSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitTimeSequenceDistinctCondition(
      TimeSequenceDistinctConditionContext ctx) {
    this.exitSequenceDistinctCondition(TimeSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitDurationSequenceDistinctCondition(
      DurationSequenceDistinctConditionContext ctx) {
    this.exitSequenceDistinctCondition(DurationSequenceExpression.class, ctx.modifier);
  }

  @Override
  public void exitLateBoundDistinctCondition(LateBoundDistinctConditionContext ctx) {
    this.exitSequenceDistinctCondition(
        this.resolveSequenceType(this.stack.peekType()), ctx.modifier);
  }

  private <T extends SequenceExpression> void exitSequenceDistinctCondition(
      Class<T> sequenceType, Token modifier) {
    final T sequence = this.stack.pop(sequenceType);
    BooleanExpression condition = this.script.composeIsDistinctCondition(sequence);
    if (modifier == null || modifier.getType() != EfxLexer.No) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  @Override
  public void exitLikePatternCondition(LikePatternConditionContext ctx) {
    this.exitLikePatternCondition(ctx.modifier, ctx.pattern.getText());
  }

  @Override
  public void exitLateBoundLikePatternCondition(LateBoundLikePatternConditionContext ctx) {
    this.exitLikePatternCondition(ctx.modifier, ctx.pattern.getText());
  }

  private void exitLikePatternCondition(Token modifier, String pattern) {
    EfxRegexValidator.validate(pattern);
    StringExpression expression = this.stack.pop(StringExpression.class);
    BooleanExpression condition = this.script.composePatternMatchCondition(expression, pattern);
    if (modifier != null && modifier.getType() == EfxLexer.Not) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  // #endregion Boolean expressions - Conditions ------------------------------

  // #region Boolean expressions - List membership conditions -----------------

  @Override
  public void exitStringInListCondition(StringInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringInListConditionRight(LateBoundStringInListConditionRightContext ctx) {
    this.exitInListCondition(ctx.modifier, StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringInListConditionLeft(LateBoundStringInListConditionLeftContext ctx) {
    this.exitInListCondition(ctx.modifier, StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanInListCondition(BooleanInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanInListConditionRight(LateBoundBooleanInListConditionRightContext ctx) {
    this.exitInListCondition(ctx.modifier, BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanInListConditionLeft(LateBoundBooleanInListConditionLeftContext ctx) {
    this.exitInListCondition(ctx.modifier, BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumberInListCondition(NumberInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumberInListConditionRight(LateBoundNumberInListConditionRightContext ctx) {
    this.exitInListCondition(ctx.modifier, NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumberInListConditionLeft(LateBoundNumberInListConditionLeftContext ctx) {
    this.exitInListCondition(ctx.modifier, NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitDateInListCondition(DateInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateInListConditionRight(LateBoundDateInListConditionRightContext ctx) {
    this.exitInListCondition(ctx.modifier, DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateInListConditionLeft(LateBoundDateInListConditionLeftContext ctx) {
    this.exitInListCondition(ctx.modifier, DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeInListCondition(TimeInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeInListConditionRight(LateBoundTimeInListConditionRightContext ctx) {
    this.exitInListCondition(ctx.modifier, TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeInListConditionLeft(LateBoundTimeInListConditionLeftContext ctx) {
    this.exitInListCondition(ctx.modifier, TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationInListCondition(DurationInListConditionContext ctx) {
    this.exitInListCondition(ctx.modifier, DurationExpression.class, DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationInListConditionRight(LateBoundDurationInListConditionRightContext ctx) {
    this.exitInListCondition(ctx.modifier, DurationExpression.class, DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationInListConditionLeft(LateBoundDurationInListConditionLeftContext ctx) {
    this.exitInListCondition(ctx.modifier, DurationExpression.class, DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundInListCondition(LateBoundInListConditionContext ctx) {
    TypedExpression expression = this.stack.peekType(-1);
    this.exitInListCondition(ctx.modifier, this.resolveScalarType(expression), this.resolveSequenceType(expression));
  }

  private void exitInListCondition(
      Token modifier, Class<? extends ScalarExpression> expressionType, Class<? extends SequenceExpression> listType) {
    SequenceExpression list = this.stack.pop(listType);
    ScalarExpression expression = this.stack.pop(expressionType);
    BooleanExpression condition = this.script.composeContainsCondition(expression, list);
    if (modifier != null && modifier.getType() == EfxLexer.Not) {
      condition = this.script.composeLogicalNot(condition);
    }
    this.stack.push(condition);
  }

  // #endregion Boolean expressions - List membership conditions -----------------

  // #endregion Boolean expressions -------------------------------------------

  // #region Quantified expressions -------------------------------------------

  @Override
  public void enterQuantifiedExpression(QuantifiedExpressionContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void enterLateBoundQuantifiedExpression(LateBoundQuantifiedExpressionContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void exitQuantifiedExpression(QuantifiedExpressionContext ctx) {
    this.exitQuantifiedExpression(ctx.Every() != null);
  }

  @Override
  public void exitLateBoundQuantifiedExpression(LateBoundQuantifiedExpressionContext ctx) {
    this.exitQuantifiedExpression(ctx.Every() != null);
  }

  private void exitQuantifiedExpression(boolean isEvery) {
    BooleanExpression booleanExpression = this.stack.pop(BooleanExpression.class);
    if (isEvery) {
      this.stack.push(this.script.composeAllSatisfy(this.stack.pop(IteratorListExpression.class),
          booleanExpression));
    } else {
      this.stack.push(this.script.composeAnySatisfies(this.stack.pop(IteratorListExpression.class),
          booleanExpression));
    }
    this.stack.popStackFrame();
  }

  // #endregion Quantified expressions ----------------------------------------

  // #region Numeric expressions ----------------------------------------------

  @Override
  public void exitAdditiveExpression(AdditiveExpressionContext ctx) {
    this.exitNumericOperation(ctx.operator.getText());
  }

  @Override
  public void exitLateBoundAdditiveExpressionLeft(LateBoundAdditiveExpressionLeftContext ctx) {
    this.exitNumericOperation(ctx.operator.getText());
  }

  @Override
  public void exitLateBoundAdditiveExpressionRight(LateBoundAdditiveExpressionRightContext ctx) {
    this.exitNumericOperation(ctx.operator.getText());
  }

  @Override
  public void exitLateBoundAdditiveExpression(LateBoundAdditiveExpressionContext ctx) {
    this.dispatchComposeLateBoundAdditiveExpression(ctx);
  }

  @Override
  public void exitMultiplicativeExpression(MultiplicativeExpressionContext ctx) {
    this.exitNumericOperation(ctx.operator.getText());
  }

  @Override
  public void exitLateBoundMultiplicativeExpressionLeft(LateBoundMultiplicativeExpressionLeftContext ctx) {
    this.exitNumericOperation(ctx.operator.getText());
  }

  @Override
  public void exitLateBoundMultiplicativeExpressionRight(LateBoundMultiplicativeExpressionRightContext ctx) {
    this.exitNumericOperation(ctx.operator.getText());
  }

  @Override
  public void exitLateBoundMultiplicativeExpression(LateBoundMultiplicativeExpressionContext ctx) {
    this.dispatchComposeLateBoundMultiplication(ctx);
  }

  private void exitNumericOperation(String operator) {
    NumericExpression right = this.stack.pop(NumericExpression.class);
    NumericExpression left = this.stack.pop(NumericExpression.class);
    this.stack.push(this.script.composeNumericOperation(left, operator, right));
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
    this.exitDurationAddition();
  }

  @Override
  public void exitLateBoundDurationAdditionExpressionLeft(LateBoundDurationAdditionExpressionLeftContext ctx) {
    this.exitDurationAddition();
  }

  @Override
  public void exitLateBoundDurationAdditionExpressionRight(LateBoundDurationAdditionExpressionRightContext ctx) {
    this.exitDurationAddition();
  }

  private void exitDurationAddition() {
    DurationExpression right = this.stack.pop(DurationExpression.class);
    DurationExpression left = this.stack.pop(DurationExpression.class);
    this.stack.push(this.script.composeAddition(left, right));
  }

  @Override
  public void exitDurationSubtractionExpression(DurationSubtractionExpressionContext ctx) {
    this.exitDurationSubtraction();
  }

  @Override
  public void exitLateBoundDurationSubtractionExpressionLeft(LateBoundDurationSubtractionExpressionLeftContext ctx) {
    this.exitDurationSubtraction();
  }

  @Override
  public void exitLateBoundDurationSubtractionExpressionRight(LateBoundDurationSubtractionExpressionRightContext ctx) {
    this.exitDurationSubtraction();
  }

  private void exitDurationSubtraction() {
    DurationExpression right = this.stack.pop(DurationExpression.class);
    DurationExpression left = this.stack.pop(DurationExpression.class);
    this.stack.push(this.script.composeSubtraction(left, right));
  }

  @Override
  public void exitDurationMultiplicationExpressionLeft(DurationMultiplicationExpressionLeftContext ctx) {
    this.exitDurationMultiplication();
  }

  @Override
  public void exitLateBoundDurationMultiplicationExpressionLeft(LateBoundDurationMultiplicationExpressionLeftContext ctx) {
    this.exitDurationMultiplication();
  }

  @Override
  public void exitDurationMultiplicationExpressionRight(DurationMultiplicationExpressionRightContext ctx) {
    this.exitReversedDurationMultiplication();
  }

  @Override
  public void exitLateBoundDurationMultiplicationExpressionRight(LateBoundDurationMultiplicationExpressionRightContext ctx) {
    this.exitDurationMultiplication();
  }

  @Override
  public void exitLateBoundReversedDurationMultiplicationExpression(LateBoundReversedDurationMultiplicationExpressionContext ctx) {
    this.exitReversedDurationMultiplication();
  }

  private void exitDurationMultiplication() {
    DurationExpression duration = this.stack.pop(DurationExpression.class);
    NumericExpression number = this.stack.pop(NumericExpression.class);
    this.stack.push(this.script.composeMultiplication(number, duration));
  }

  private void exitReversedDurationMultiplication() {
    NumericExpression number = this.stack.pop(NumericExpression.class);
    DurationExpression duration = this.stack.pop(DurationExpression.class);
    this.stack.push(this.script.composeMultiplication(number, duration));
  }

  @Override
  public void exitDateSubtractionExpression(DateSubtractionExpressionContext ctx) {
    this.exitDateSubtraction();
  }

  @Override
  public void exitLateBoundDateSubtractionExpressionLeft(LateBoundDateSubtractionExpressionLeftContext ctx) {
    this.exitDateSubtraction();
  }

  @Override
  public void exitLateBoundDateSubtractionExpressionRight(LateBoundDateSubtractionExpressionRightContext ctx) {
    this.exitDateSubtraction();
  }

  private void exitDateSubtraction() {
    DateExpression startDate = this.stack.pop(DateExpression.class);
    DateExpression endDate = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeSubtraction(startDate, endDate));
  }

  private void exitDateDurationAddition() {
    DurationExpression duration = this.stack.pop(DurationExpression.class);
    DateExpression date = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeAddition(date, duration));
  }

  private void exitDateDurationSubtraction() {
    DurationExpression duration = this.stack.pop(DurationExpression.class);
    DateExpression date = this.stack.pop(DateExpression.class);
    this.stack.push(this.script.composeSubtraction(date, duration));
  }

  // #endregion Duration Expressions ------------------------------------------

  // #region List Expressions -------------------------------------------------

  @Override
  public void exitCodeList(CodeListContext ctx) {
    if (this.stack.empty()) {
      this.stack.push(this.script.composeList(Collections.emptyList(), StringSequenceExpression.class));
    }
  }

  @Override
  public void exitStringList(StringListContext ctx) {
    this.exitList(ctx.stringExpression().size() + ctx.lateBoundScalar().size(),
        StringExpression.class, StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanList(BooleanListContext ctx) {
    this.exitList(ctx.booleanExpression().size() + ctx.lateBoundScalar().size(),
        BooleanExpression.class, BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericList(NumericListContext ctx) {
    this.exitList(ctx.numericExpression().size() + ctx.lateBoundScalar().size(),
        NumericExpression.class, NumericSequenceExpression.class);
  }

  @Override
  public void exitDateList(DateListContext ctx) {
    this.exitList(ctx.dateExpression().size() + ctx.lateBoundScalar().size(),
        DateExpression.class, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeList(TimeListContext ctx) {
    this.exitList(ctx.timeExpression().size() + ctx.lateBoundScalar().size(),
        TimeExpression.class, TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationList(DurationListContext ctx) {
    this.exitList(ctx.durationExpression().size() + ctx.lateBoundScalar().size(),
        DurationExpression.class, DurationSequenceExpression.class);
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

  @Override
  public void exitLateBoundList(LateBoundListContext ctx) {
    int listSize = ctx.lateBoundScalar().size();
    Class<? extends ScalarExpression> scalarType = this.resolveScalarType(this.stack.peekType());
    Class<? extends SequenceExpression> sequenceType = this.resolveSequenceType(this.stack.peekType());
    this.exitList(listSize, scalarType, sequenceType);
  }

  // #endregion List Expressions ----------------------------------------------

  // #region Parenthesized Sequences ------------------------------------------

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
  public void exitLateBoundParenthesizedSequence(LateBoundParenthesizedSequenceContext ctx) {
    this.exitLateBoundParenthesizedExpression();
  }

  @Override
  public void exitLateBoundParenthesizedScalar(LateBoundParenthesizedScalarContext ctx) {
    this.exitLateBoundParenthesizedExpression();
  }

  private void exitLateBoundParenthesizedExpression() {
    TypedExpression inner = this.stack.peekType();
    if (inner instanceof SequenceExpression) {
      Class<? extends SequenceExpression> type = this.resolveSequenceType(inner);
      this.stack.push(this.composeParenthesizedExpression(this.stack.pop(type), type));
    } else {
      Class<? extends ScalarExpression> type = this.resolveScalarType(inner);
      this.stack.push(this.composeParenthesizedExpression(this.stack.pop(type), type));
    }
  }

  private <T extends ScalarExpression> T composeParenthesizedExpression(ScalarExpression inner, Class<T> type) {
    return this.script.composeParenthesizedExpression(Expression.from(inner, type), type);
  }

  private <T extends SequenceExpression> T composeParenthesizedExpression(SequenceExpression sequence, Class<T> sequenceType) {
    return this.script.composeParenthesizedExpression(Expression.from(sequence, sequenceType), sequenceType);
  }

  // #endregion Parenthesized Sequences ---------------------------------------

  // #region Conditional Expressions ------------------------------------------

  @Override
  public void exitConditionalBooleanExpression(ConditionalBooleanExpressionContext ctx) {
    this.exitConditionalExpression(BooleanExpression.class);
  }

  @Override
  public void exitLateBoundConditionalBooleanExpressionLeft(LateBoundConditionalBooleanExpressionLeftContext ctx) {
    this.exitConditionalExpression(BooleanExpression.class);
  }

  @Override
  public void exitLateBoundConditionalBooleanExpressionRight(LateBoundConditionalBooleanExpressionRightContext ctx) {
    this.exitConditionalExpression(BooleanExpression.class);
  }

  @Override
  public void exitConditionalNumericExpression(ConditionalNumericExpressionContext ctx) {
    this.exitConditionalExpression(NumericExpression.class);
  }

  @Override
  public void exitLateBoundConditionalNumericExpressionLeft(LateBoundConditionalNumericExpressionLeftContext ctx) {
    this.exitConditionalExpression(NumericExpression.class);
  }

  @Override
  public void exitLateBoundConditionalNumericExpressionRight(LateBoundConditionalNumericExpressionRightContext ctx) {
    this.exitConditionalExpression(NumericExpression.class);
  }

  @Override
  public void exitConditionalStringExpression(ConditionalStringExpressionContext ctx) {
    this.exitConditionalExpression(StringExpression.class);
  }

  @Override
  public void exitLateBoundConditionalStringExpressionLeft(LateBoundConditionalStringExpressionLeftContext ctx) {
    this.exitConditionalExpression(StringExpression.class);
  }

  @Override
  public void exitLateBoundConditionalStringExpressionRight(LateBoundConditionalStringExpressionRightContext ctx) {
    this.exitConditionalExpression(StringExpression.class);
  }

  @Override
  public void exitConditionalDateExpression(ConditionalDateExpressionContext ctx) {
    this.exitConditionalExpression(DateExpression.class);
  }

  @Override
  public void exitLateBoundConditionalDateExpressionLeft(LateBoundConditionalDateExpressionLeftContext ctx) {
    this.exitConditionalExpression(DateExpression.class);
  }

  @Override
  public void exitLateBoundConditionalDateExpressionRight(LateBoundConditionalDateExpressionRightContext ctx) {
    this.exitConditionalExpression(DateExpression.class);
  }

  @Override
  public void exitConditionalTimeExpression(ConditionalTimeExpressionContext ctx) {
    this.exitConditionalExpression(TimeExpression.class);
  }

  @Override
  public void exitLateBoundConditionalTimeExpressionLeft(LateBoundConditionalTimeExpressionLeftContext ctx) {
    this.exitConditionalExpression(TimeExpression.class);
  }

  @Override
  public void exitLateBoundConditionalTimeExpressionRight(LateBoundConditionalTimeExpressionRightContext ctx) {
    this.exitConditionalExpression(TimeExpression.class);
  }

  @Override
  public void exitConditionalDurationExpression(ConditionalDurationExpressionContext ctx) {
    this.exitConditionalExpression(DurationExpression.class);
  }

  @Override
  public void exitLateBoundConditionalDurationExpressionLeft(LateBoundConditionalDurationExpressionLeftContext ctx) {
    this.exitConditionalExpression(DurationExpression.class);
  }

  @Override
  public void exitLateBoundConditionalDurationExpressionRight(LateBoundConditionalDurationExpressionRightContext ctx) {
    this.exitConditionalExpression(DurationExpression.class);
  }

  @Override
  public void exitLateBoundConditionalExpression(LateBoundConditionalExpressionContext ctx) {
    this.exitConditionalExpression(this.resolveScalarType(this.stack.peekType(-1)));
  }

  private <T extends ScalarExpression> void exitConditionalExpression(Class<T> type) {
    T whenFalse = this.stack.pop(type);
    T whenTrue = this.stack.pop(type);
    BooleanExpression condition = this.stack.pop(BooleanExpression.class);
    this.stack.push(this.script.composeConditionalExpression(condition, whenTrue, whenFalse, type));
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
              path));
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

  private <T1 extends ScalarExpression, T2 extends SequenceExpression> void exitIteratorExpression(String variableName,
      Class<T1> variableType, Class<T2> listType) {
    Expression declarationExpression = this.script.composeVariableDeclaration(variableName, variableType);
    SequenceExpression initialisationExpression = this.stack.pop(listType);
    ScalarExpression referenceExpression = this.script.composeVariableReference(variableName, variableType);
    Variable variable = new Variable(variableName, declarationExpression, initialisationExpression,
        referenceExpression);
    this.stack.declareIdentifier(variable);
    this.stack.push(this.script.composeIteratorExpression(variable.declarationExpression, initialisationExpression));
  }

  // #endregion Iterators -----------------------------------------------------

  // #region Iteration Expressions ---------------------------------------------

  // for ... return <scalar> (map iterations)

  @Override
  public void enterStringSequenceFromIteration(StringSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitStringSequenceFromIteration(StringSequenceFromIterationContext ctx) {
    this.exitSequenceFromIteration(StringExpression.class, StringSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterNumericSequenceFromIteration(NumericSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitNumericSequenceFromIteration(NumericSequenceFromIterationContext ctx) {
    this.exitSequenceFromIteration(NumericExpression.class, NumericSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterBooleanSequenceFromIteration(BooleanSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitBooleanSequenceFromIteration(BooleanSequenceFromIterationContext ctx) {
    this.exitSequenceFromIteration(BooleanExpression.class, BooleanSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterDateSequenceFromIteration(DateSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitDateSequenceFromIteration(DateSequenceFromIterationContext ctx) {
    this.exitSequenceFromIteration(DateExpression.class, DateSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterTimeSequenceFromIteration(TimeSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitTimeSequenceFromIteration(TimeSequenceFromIterationContext ctx) {
    this.exitSequenceFromIteration(TimeExpression.class, TimeSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void enterDurationSequenceFromIteration(DurationSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame(); // Iteration variables are local to the iteration
  }

  @Override
  public void exitDurationSequenceFromIteration(DurationSequenceFromIterationContext ctx) {
    this.exitSequenceFromIteration(DurationExpression.class, DurationSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame(); // Iteration variables are local to the iteration
  }

  private <T extends ScalarExpression, L extends SequenceExpression> void exitSequenceFromIteration(
      Class<T> expressionType, Class<L> targetListType, boolean distinct) {
    T expression = this.stack.pop(expressionType);
    IteratorListExpression iterators = this.stack.pop(IteratorListExpression.class);
    L result = this.script.composeForExpression(iterators, expression, targetListType);
    if (distinct) {
      result = this.script.composeDistinctValuesFunction(result, targetListType);
    }
    this.stack.push(result);
  }

  // for ... return <sequence> (concatenated iterations, i.e. flatMap)

  @Override
  public void enterStringSequenceFromConcatenatedIterations(StringSequenceFromConcatenatedIterationsContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void exitStringSequenceFromConcatenatedIterations(StringSequenceFromConcatenatedIterationsContext ctx) {
    this.exitSequenceFromConcatenatedIterations(StringSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame();
  }

  @Override
  public void enterBooleanSequenceFromConcatenatedIterations(BooleanSequenceFromConcatenatedIterationsContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void exitBooleanSequenceFromConcatenatedIterations(BooleanSequenceFromConcatenatedIterationsContext ctx) {
    this.exitSequenceFromConcatenatedIterations(BooleanSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame();
  }

  @Override
  public void enterNumericSequenceFromConcatenatedIterations(NumericSequenceFromConcatenatedIterationsContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void exitNumericSequenceFromConcatenatedIterations(NumericSequenceFromConcatenatedIterationsContext ctx) {
    this.exitSequenceFromConcatenatedIterations(NumericSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame();
  }

  @Override
  public void enterDateSequenceFromConcatenatedIterations(DateSequenceFromConcatenatedIterationsContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void exitDateSequenceFromConcatenatedIterations(DateSequenceFromConcatenatedIterationsContext ctx) {
    this.exitSequenceFromConcatenatedIterations(DateSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame();
  }

  @Override
  public void enterTimeSequenceFromConcatenatedIterations(TimeSequenceFromConcatenatedIterationsContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void exitTimeSequenceFromConcatenatedIterations(TimeSequenceFromConcatenatedIterationsContext ctx) {
    this.exitSequenceFromConcatenatedIterations(TimeSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame();
  }

  @Override
  public void enterDurationSequenceFromConcatenatedIterations(DurationSequenceFromConcatenatedIterationsContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void exitDurationSequenceFromConcatenatedIterations(DurationSequenceFromConcatenatedIterationsContext ctx) {
    this.exitSequenceFromConcatenatedIterations(DurationSequenceExpression.class, ctx.Distinct() != null);
    this.stack.popStackFrame();
  }

  private <T extends SequenceExpression> void exitSequenceFromConcatenatedIterations(Class<T> sequenceType, boolean distinct) {
    T sequenceExpression = this.stack.pop(sequenceType);
    IteratorListExpression iterators = this.stack.pop(IteratorListExpression.class);
    T result = this.script.composeForExpression(iterators, sequenceExpression, sequenceType);
    if (distinct) {
      result = this.script.composeDistinctValuesFunction(result, sequenceType);
    }
    this.stack.push(result);
  }

  // for ... return <late-bound> (late-bound iteration)

  @Override
  public void enterLateBoundSequenceFromIteration(LateBoundSequenceFromIterationContext ctx) {
    this.stack.pushStackFrame();
  }

  @Override
  public void exitLateBoundSequenceFromIteration(LateBoundSequenceFromIterationContext ctx) {
    TypedExpression body = this.stack.pop(TypedExpression.class);
    IteratorListExpression iterators = this.stack.pop(IteratorListExpression.class);

    this.composeForExpression(iterators, body, ctx.Distinct() != null, this.resolveSequenceType(body));
    this.stack.popStackFrame();
  }

  private <T extends SequenceExpression> void composeForExpression(
      IteratorListExpression iterators, TypedExpression body, boolean distinct, Class<T> sequenceType) {
    T result = (body instanceof ScalarExpression)
        ? this.script.composeForExpression(iterators, (ScalarExpression) body, sequenceType)
        : this.script.composeForExpression(iterators, (SequenceExpression) body, sequenceType);
    if (distinct) {
      result = this.script.composeDistinctValuesFunction(result, sequenceType);
    }
    this.stack.push(result);
  }

  // #endregion Iteration Expressions ------------------------------------------

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
  public void exitSimpleFieldReference(SimpleFieldReferenceContext ctx) {
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
  public void exitAbsoluteFieldReference(AbsoluteFieldReferenceContext ctx) {
    if (ctx.Slash() != null) {
      this.efxContext.pop();
    }
  }

  @Override
  public void exitFieldContext(FieldContextContext ctx) {
    if (ctx.indexer() != null) {
      NumericExpression index = this.stack.pop(NumericExpression.class);
      final TypedExpression top = this.stack.peekType();
      if (!(top instanceof PathExpression)) {
        throw TypeMismatchException.cannotConvert(ctx, PathExpression.class, top.getClass());
      }
      PathExpression fieldPath = (PathExpression) this.stack.pop(top.getClass());
      this.stack.push(this.script.composeIndexer(fieldPath.asSequence(), index,
          ScalarPath.fromEfxDataType.get(EfxTypeLattice.toPrimitive(fieldPath.getDataType()))));
    }
  }

  @Override
  public void enterAbsoluteNodeReference(AbsoluteNodeReferenceContext ctx) {
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
  public void exitFieldReferenceWithPredicate(FieldReferenceWithPredicateContext ctx) {
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
  public void enterPredicate(PredicateContext ctx) {
    var parent = ctx.getParent();
    if (parent instanceof NodeReferenceWithPredicateContext) {
      final String nodeId = getNodeId((NodeReferenceWithPredicateContext) parent);
      this.efxContext.pushNodeContext(nodeId);
    } else if (parent instanceof FieldReferenceWithPredicateContext) {
      final String fieldId = getFieldId((FieldReferenceWithPredicateContext) parent);
      this.efxContext.pushFieldContext(fieldId);
    } else {
      throw TranslatorConfigurationException.unhandledPredicateContext(parent.getClass().getSimpleName());
    }
  }

  /**
   * After the predicate is parsed we need to switch back to the previous context.
   */
  @Override
  public void exitPredicate(PredicateContext ctx) {
    this.efxContext.pop();
  }

  // #endregion References with Predicates ------------------------------------

  // #region Value References -------------------------------------------------

  @Override
  public void exitScalarFromFieldReference(ScalarFromFieldReferenceContext ctx) {
    PathExpression path = this.stack.pop(PathExpression.class);
    String fieldId = getFieldId(ctx.fieldReference());
    PathExpression result;
    if (this.symbols.isAttributeField(fieldId)) {
      result = this.script.composeFieldAttributeReference(
          this.script.contextualizePath(
              this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId), this.efxContext.peek().absolutePath()),
          this.symbols.getAttributeNameFromAttributeField(fieldId),
          ScalarPath.fromFieldType.get(FieldTypes.fromString(this.symbols.getTypeOfField(fieldId))));
    } else {
      result = this.script.composeFieldValueReference(path);
    }
    this.resolveAndPushFieldReference(ctx, result, fieldId);
  }

  @Override
  public void exitSequenceFromFieldReference(SequenceFromFieldReferenceContext ctx) {
    PathExpression path = this.stack.pop(PathExpression.class);
    String fieldId = getFieldId(ctx.fieldReference());
    PathExpression result;
    if (this.symbols.isAttributeField(fieldId)) {
      result = this.script.composeFieldAttributeReference(
          this.script.contextualizePath(
              this.symbols.getAbsolutePathOfFieldWithoutTheAttribute(fieldId), this.efxContext.peek().absolutePath()),
          this.symbols.getAttributeNameFromAttributeField(fieldId),
          ScalarPath.fromFieldType.get(FieldTypes.fromString(this.symbols.getTypeOfField(fieldId))));
    } else {
      result = this.script.composeFieldValueReference(path);
    }
    this.resolveAndPushFieldReference(ctx, result, fieldId);
  }

  /**
   * A selector yields the reference itself rather than its value: the value step that every other
   * reference position applies is deliberately not composed here. The path is otherwise resolved
   * exactly as it would be in an expression, relative to the declared context unless the author
   * wrote it as an absolute reference.
   *
   * <p>The reference tier has already left the path on the stack. Only an attribute reference
   * needs work, because {@code attributeReference} has no exit handler of its own; composing the
   * attribute step globally would double-compose it for the scalar and sequence positions.
   */
  @Override
  public void exitSelection(final SelectionContext ctx) {
    if (ctx.attributeReference() != null) {
      this.stack.push(this.script.composeFieldAttributeReference(
          this.stack.pop(PathExpression.class),
          ctx.attributeReference().attributeName.getText(), StringPath.class));
    }
  }

  @Override
  public void exitScalarFromAttributeReference(ScalarFromAttributeReferenceContext ctx) {
    PathExpression result = this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
        ctx.attributeReference().attributeName.getText(), StringPath.class);
    String fieldId = getFieldId(ctx.attributeReference().fieldReference());
    this.resolveAndPushFieldReference(ctx, result, fieldId);
  }

  @Override
  public void exitSequenceFromAttributeReference(SequenceFromAttributeReferenceContext ctx) {
    PathExpression result = this.script.composeFieldAttributeReference(this.stack.pop(PathExpression.class),
        ctx.attributeReference().attributeName.getText(), StringPath.class);
    String fieldId = getFieldId(ctx.attributeReference().fieldReference());
    this.resolveAndPushFieldReference(ctx, result, fieldId);
  }

  /**
   * Pushes a field or attribute reference onto the stack, applying cardinality resolution
   * when inside a late-bound expression. In a typed context ({@code RESOLVED}), the result
   * is pushed as-is. In a late-bound context, the field's repeatability determines whether
   * a scalar or sequence path is pushed, and errors are raised for type mismatches.
   *
   * @param ctx the parse tree context (for error reporting)
   * @param result the composed field/attribute path expression
   * @param fieldId the field identifier (used to check repeatability)
   */
  private void resolveAndPushFieldReference(ParserRuleContext ctx, PathExpression result, String fieldId) {
    switch (this.currentCardinalityResolutionContext()) {
      case RESOLVED:
        this.stack.push(result);
        break;
      case RESOLVE_SCALAR:
        if (this.fieldMayReturnMultipleValues(fieldId)) {
          throw this.fieldMayReturnMultipleValuesException(ctx, fieldId);
        }
        this.stack.push(result);
        break;
      case RESOLVE_SEQUENCE:
        this.stack.push(result.asSequence());
        break;
      case RESOLVE_EITHER:
        if (this.fieldMayReturnMultipleValues(fieldId)) {
          this.stack.push(result.asSequence());
        } else {
          this.stack.push(result);
        }
        break;
    }
  }

  /**
   * Returns true if a field may return multiple values from the current EFX context.
   * This includes fields that are repeatable (from SDK metadata or node hierarchy)
   * and multilingual fields (which have multiple XML elements, one per language).
   * A field is not considered repeatable if it IS the current context (e.g., inside a WITH block
   * on this field, we're referencing the current element being iterated, not the whole sequence).
   */
  private boolean fieldMayReturnMultipleValues(String fieldId) {
    if (!this.efxContext.isEmpty()
        && this.efxContext.peek() != null
        && this.efxContext.peek().isFieldContext()
        && fieldId.equals(this.efxContext.symbol())) {
      return false;
    }
    return this.symbols.isFieldRepeatableFromContext(fieldId, this.getContextNodeId());
  }

  /**
   * Creates the appropriate exception for a field that may return multiple values but is used
   * as a scalar. Returns {@link TypeMismatchException#fieldIsMultilingual} for multilingual fields,
   * or {@link TypeMismatchException#fieldMayRepeat} for structurally repeatable fields.
   */
  private TypeMismatchException fieldMayReturnMultipleValuesException(ParserRuleContext ctx, String fieldId) {
    if (FieldTypes.TEXT_MULTILINGUAL.getName().equals(this.symbols.getTypeOfField(fieldId))) {
      return TypeMismatchException.fieldIsMultilingual(ctx, fieldId);
    }
    return TypeMismatchException.fieldMayRepeat(ctx, fieldId, this.efxContext.symbol());
  }

  /**
   * Gets the node ID of the current context for use with
   * {@link SymbolResolver#isFieldRepeatableFromContext}.
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
      return this.symbols.getParentNodeOfField(context.symbol());
    }
  }

  // #endregion Value References ----------------------------------------------

  // #region References with context override ---------------------------------

  /**
   * Handles expressions of the form ContextField::ReferencedField. Changes the context before the
   * reference is resolved.
   */
  @Override
  public void exitContextFieldSpecifier(ContextFieldSpecifierContext ctx) {
    final PathExpression contextFieldPath = this.stack.pop(PathExpression.class);
    final String contextFieldId = getFieldId(ctx.fieldContext());
    this.efxContext
        .push(new FieldContext(contextFieldId, this.symbols.getAbsolutePathOfField(contextFieldId),
            contextFieldPath));
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
      throw InvalidIdentifierException.notAContextVariable(ctx, variableName);
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

  // #region Codelist and Variable References ---------------------------------

  @Override
  public void exitCodelistReference(CodelistReferenceContext ctx) {
    this.stack.push(this.script.composeList(this.symbols.expandCodelist(ctx.codelistName.getText())
        .stream().map(s -> this.script.getStringLiteralFromUnquotedString(s))
        .collect(Collectors.toList()), StringSequenceExpression.class));
  }

  @Override
  public void exitScalarFromVariableReference(ScalarFromVariableReferenceContext ctx) {
    this.resolveAndPushVariableReference(ctx.variableReference());
  }

  @Override
  public void exitSequenceFromVariableReference(SequenceFromVariableReferenceContext ctx) {
    this.resolveAndPushVariableReference(ctx.variableReference());
  }

  /**
   * Resolves and pushes a variable reference, applying cardinality checks when inside a
   * late-bound expression. Context variables (bound by {@code for context:}) are always scalar;
   * node context variables cannot be used as values. Regular variables have explicit cardinality
   * from their declaration.
   *
   * @param ctx the variable reference context
   */
  protected void resolveAndPushVariableReference(VariableReferenceContext ctx) {
    String variableName = ctx.variableName.getText();
    Context variableContext = this.efxContext.getContextFromVariable(variableName);

    if (variableContext != null) {
      if (variableContext.isNodeContext()) {
        throw TypeMismatchException.nodeContextUsedAsValue(ctx, variableName, variableContext.symbol());
      }
      if (this.currentCardinalityResolutionContext() == CardinalityResolutionContext.RESOLVE_SEQUENCE) {
        throw TypeMismatchException.identifierIsScalar(ctx, variableName);
      }
    } else {
      Optional<TypedExpression> refExpr = this.stack.getParameter(variableName)
          .or(() -> this.stack.getVariable(variableName).map(v -> v.referenceExpression));
      boolean isSequence = EfxTypeLattice.isSequence(
          refExpr.orElseThrow(() -> InvalidIdentifierException.undeclaredIdentifier(ctx, variableName)).getDataType());
      switch (this.currentCardinalityResolutionContext()) {
        case RESOLVE_SEQUENCE:
          if (!isSequence) {
            throw TypeMismatchException.identifierIsScalar(ctx, variableName);
          }
          break;
        case RESOLVE_SCALAR:
          if (isSequence) {
            throw TypeMismatchException.identifierIsSequence(ctx, variableName);
          }
          break;
        default:
          break;
      }
    }

    this.stack.pushIdentifierReference(variableName);
  }

  // #endregion Codelist and Variable References ------------------------------

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

  @Override
  public void exitLateBoundScalarAtSequenceIndex(LateBoundScalarAtSequenceIndexContext ctx) {
    TypedExpression sequence = this.stack.peekType(-1);
    this.exitSequenceAtIndex(this.resolveScalarType(sequence), this.resolveSequenceType(sequence));
  }




  // #endregion New in EFX-2: Indexers ---------------------------------------

  // #region Type resolution -------------------------------------------------

  private static final Map<Class<? extends EfxDataType>, Class<? extends TypedExpression>> efxDataTypeToScalarExpressionMap = Map.ofEntries(
      entry(EfxDataType.String.class, StringExpression.class),
      entry(EfxDataType.MultilingualString.class, StringExpression.class),
      entry(EfxDataType.Boolean.class, BooleanExpression.class),
      entry(EfxDataType.Number.class, NumericExpression.class),
      entry(EfxDataType.Date.class, DateExpression.class),
      entry(EfxDataType.Time.class, TimeExpression.class),
      entry(EfxDataType.Duration.class, DurationExpression.class));

  private static final Map<Class<? extends EfxDataType>, Class<? extends TypedExpression>> efxDataTypeToSequenceExpressionMap = Map.ofEntries(
      entry(EfxDataType.String.class, StringSequenceExpression.class),
      entry(EfxDataType.MultilingualString.class, StringSequenceExpression.class),
      entry(EfxDataType.Boolean.class, BooleanSequenceExpression.class),
      entry(EfxDataType.Number.class, NumericSequenceExpression.class),
      entry(EfxDataType.Date.class, DateSequenceExpression.class),
      entry(EfxDataType.Time.class, TimeSequenceExpression.class),
      entry(EfxDataType.Duration.class, DurationSequenceExpression.class));

  private Class<? extends ScalarExpression> resolveScalarType(TypedExpression expression) {
    Class<? extends EfxDataType> primitive = EfxTypeLattice.toPrimitive(expression.getDataType());
    return efxDataTypeToScalarExpressionMap.get(primitive).asSubclass(ScalarExpression.class);
  }

  protected Class<? extends ScalarExpression> resolveScalarType(Class<? extends EfxDataType> primitiveType) {
    Class<? extends TypedExpression> expressionType = efxDataTypeToScalarExpressionMap.get(primitiveType);
    if (expressionType == null || !ScalarExpression.class.isAssignableFrom(expressionType)) {
      throw TranslatorConfigurationException.missingTypeMapping(primitiveType, "resolveScalarType");
    }
    return expressionType.asSubclass(ScalarExpression.class);
  }

  private Class<? extends SequenceExpression> resolveSequenceType(TypedExpression expression) {
    Class<? extends EfxDataType> primitive = EfxTypeLattice.toPrimitive(expression.getDataType());
    return efxDataTypeToSequenceExpressionMap.get(primitive).asSubclass(SequenceExpression.class);
  }

  private Class<? extends SequenceExpression> resolveSequenceType(Class<? extends EfxDataType> primitiveType) {
    Class<? extends TypedExpression> expressionType = efxDataTypeToSequenceExpressionMap.get(primitiveType);
    if (expressionType == null || !SequenceExpression.class.isAssignableFrom(expressionType)) {
      throw TranslatorConfigurationException.missingTypeMapping(primitiveType, "resolveSequenceType");
    }
    return expressionType.asSubclass(SequenceExpression.class);
  }

  private boolean isNumeric(TypedExpression expression) {
    return EfxDataType.Number.class.isAssignableFrom(
        EfxTypeLattice.toPrimitive(expression.getDataType()));
  }

  private boolean isDuration(TypedExpression expression) {
    return EfxDataType.Duration.class.isAssignableFrom(
        EfxTypeLattice.toPrimitive(expression.getDataType()));
  }

  private boolean isDate(TypedExpression expression) {
    return EfxDataType.Date.class.isAssignableFrom(
        EfxTypeLattice.toPrimitive(expression.getDataType()));
  }

  // #endregion Type resolution ------------------------------------------------

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

  @Override
  public void exitScalarFromFunctionInvocation(ScalarFromFunctionInvocationContext ctx) {
    this.resolveAndPushFunctionInvocation(ctx.functionInvocation());
  }

  @Override
  public void exitSequenceFromFunctionInvocation(SequenceFromFunctionInvocationContext ctx) {
    this.resolveAndPushFunctionInvocation(ctx.functionInvocation());
  }

  /**
   * Resolves and pushes a function invocation, applying cardinality checks when inside a
   * late-bound expression. Scalar functions in sequence context (or vice versa) raise errors.
   * The appropriate expression type map (scalar or sequence) is selected automatically based
   * on the function's declared return cardinality.
   *
   * @param ctx the function invocation context
   */
  private void resolveAndPushFunctionInvocation(FunctionInvocationContext ctx) {
    var arguments = this.stack.pop(StrictArguments.class);
    var function = (Function) arguments.identifier;
    String functionName = ctx.functionName.getText();
    boolean isSequence = EfxTypeLattice.isSequence(function.dataType);

    switch (this.currentCardinalityResolutionContext()) {
      case RESOLVE_SEQUENCE:
        if (!isSequence) {
          throw TypeMismatchException.identifierIsScalar(ctx, functionName);
        }
        break;
      case RESOLVE_SCALAR:
        if (isSequence) {
          throw TypeMismatchException.identifierIsSequence(ctx, functionName);
        }
        break;
      default:
        break;
    }

    var baseType = EfxTypeLattice.toPrimitive(function.dataType);
    var expressionType = isSequence
        ? this.resolveSequenceType(baseType) : this.resolveScalarType(baseType);
    this.stack.push(this.script.composeFunctionInvocation(functionName,
        arguments.getArgumentValues(), expressionType));
  }

  // #endregion New in EFX-2: Function invocation -----------------------------

  // #region Parameter Declarations -------------------------------------------


  @Override
  public void exitStringParameterDeclaration(StringParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), StringExpression.class);
  }

  @Override
  public void exitNumericParameterDeclaration(NumericParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), NumericExpression.class);
  }

  @Override
  public void exitBooleanParameterDeclaration(BooleanParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), BooleanExpression.class);
  }

  @Override
  public void exitDateParameterDeclaration(DateParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), DateExpression.class);
  }

  @Override
  public void exitTimeParameterDeclaration(TimeParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), TimeExpression.class);
  }

  @Override
  public void exitDurationParameterDeclaration(DurationParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), DurationExpression.class);
  }

  // Sequence parameter declarations
  @Override
  public void exitStringSequenceParameterDeclaration(StringSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), StringSequenceExpression.class);
  }

  @Override
  public void exitNumericSequenceParameterDeclaration(NumericSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), NumericSequenceExpression.class);
  }

  @Override
  public void exitBooleanSequenceParameterDeclaration(BooleanSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), BooleanSequenceExpression.class);
  }

  @Override
  public void exitDateSequenceParameterDeclaration(DateSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSequenceParameterDeclaration(TimeSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSequenceParameterDeclaration(DurationSequenceParameterDeclarationContext ctx) {
    this.exitParameterDeclaration(ctx, ctx.parameterName.getText(), DurationSequenceExpression.class);
  }

  protected void exitParameterDeclaration(ParserRuleContext ctx, String parameterName, Class<? extends TypedExpression> parameterType) {
    if (this.expressionArguments.isEmpty()) {
      throw InvalidArgumentException.missingArgument(ctx, parameterName);
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

  // #endregion Boolean functions ---------------------------------------------

  // #region Privacy settings ------------------------------------------------

  @Override
  public void exitFieldWasWithheldProperty(FieldWasWithheldPropertyContext ctx) {
    final String fieldId = getFieldId(ctx.fieldMention());
    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(ctx, fieldId);
    }

    this.stack.push(this.composeWasWithheldCondition(fieldId, privacyCode));
  }

  @Override
  public void exitFieldIsWithheldProperty(FieldIsWithheldPropertyContext ctx) {
    final String fieldId = getFieldId(ctx.fieldMention());
    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(ctx, fieldId);
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
    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(ctx, fieldId);
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
    final String privacyCode = this.symbols.getPrivacyCodeOfField(fieldId);
    if (privacyCode == null || privacyCode.isEmpty()) {
      throw InvalidUsageException.fieldNotWithholdable(ctx, fieldId);
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
      throw InvalidUsageException.fieldNotWithholdable(ctx, fieldId);
    }
    this.stack.push(this.script.getStringLiteralFromUnquotedString(privacyCode));
  }

  @Override
  public void exitRawValueReference(RawValueReferenceContext ctx) {
    final TypedExpression top = this.stack.peekType();
    if (!(top instanceof PathExpression)) {
      throw TypeMismatchException.cannotConvert(ctx, PathExpression.class, top.getClass());
    }
    if (top instanceof SequenceExpression
        && this.currentCardinalityResolutionContext() == CardinalityResolutionContext.RESOLVE_SCALAR) {
      final String fieldId = getFieldId(ctx.fieldContext());
      throw this.fieldMayReturnMultipleValuesException(ctx, fieldId);
    }
    final PathExpression fieldPath = (PathExpression) this.stack.pop(top.getClass());
    this.stack.push(this.script.composeFieldRawValueReference(fieldPath));
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
    if (EfxDataType.Duration.class.isAssignableFrom(type)) {
      return this.script.getDurationLiteralEquivalent(value);
    }
    if (EfxDataType.Boolean.class.isAssignableFrom(type)) {
      return this.script.getBooleanEquivalent(Boolean.parseBoolean(value));
    }
    return this.script.getStringLiteralFromUnquotedString(value);
  }

  // #endregion Privacy settings ---------------------------------------------

  // #region Sequence-equal ----------------------------------------------------

  @Override
  public void exitLateBoundSequenceEqualFunction(LateBoundSequenceEqualFunctionContext ctx) {
    this.exitSequenceEqualFunction(this.resolveSequenceType(this.stack.peekType(-1)));
  }

  @Override
  public void exitStringSequenceEqualFunction(StringSequenceEqualFunctionContext ctx) {
    this.exitSequenceEqualFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringSequenceEqualLeft(LateBoundStringSequenceEqualLeftContext ctx) {
    this.exitSequenceEqualFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringSequenceEqualRight(LateBoundStringSequenceEqualRightContext ctx) {
    this.exitSequenceEqualFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanSequenceEqualFunction(BooleanSequenceEqualFunctionContext ctx) {
    this.exitSequenceEqualFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanSequenceEqualLeft(LateBoundBooleanSequenceEqualLeftContext ctx) {
    this.exitSequenceEqualFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanSequenceEqualRight(LateBoundBooleanSequenceEqualRightContext ctx) {
    this.exitSequenceEqualFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericSequenceEqualFunction(NumericSequenceEqualFunctionContext ctx) {
    this.exitSequenceEqualFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericSequenceEqualLeft(LateBoundNumericSequenceEqualLeftContext ctx) {
    this.exitSequenceEqualFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericSequenceEqualRight(LateBoundNumericSequenceEqualRightContext ctx) {
    this.exitSequenceEqualFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateSequenceEqualFunction(DateSequenceEqualFunctionContext ctx) {
    this.exitSequenceEqualFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateSequenceEqualLeft(LateBoundDateSequenceEqualLeftContext ctx) {
    this.exitSequenceEqualFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateSequenceEqualRight(LateBoundDateSequenceEqualRightContext ctx) {
    this.exitSequenceEqualFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSequenceEqualFunction(TimeSequenceEqualFunctionContext ctx) {
    this.exitSequenceEqualFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeSequenceEqualLeft(LateBoundTimeSequenceEqualLeftContext ctx) {
    this.exitSequenceEqualFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeSequenceEqualRight(LateBoundTimeSequenceEqualRightContext ctx) {
    this.exitSequenceEqualFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSequenceEqualFunction(DurationSequenceEqualFunctionContext ctx) {
    this.exitSequenceEqualFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationSequenceEqualLeft(LateBoundDurationSequenceEqualLeftContext ctx) {
    this.exitSequenceEqualFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationSequenceEqualRight(LateBoundDurationSequenceEqualRightContext ctx) {
    this.exitSequenceEqualFunction(DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitSequenceEqualFunction(Class<T> sequenceType) {
    final T two = this.stack.pop(sequenceType);
    final T one = this.stack.pop(sequenceType);
    this.stack.push(this.script.composeSequenceEqualFunction(one, two));
  }

  // #endregion Sequence-equal -------------------------------------------------

  // #region Numeric functions ------------------------------------------------

  @Override
  public void exitLateBoundCountFunction(LateBoundCountFunctionContext ctx) {
    this.stack.push(this.script.composeCountOperation(
        this.stack.pop(this.resolveSequenceType(this.stack.peekType()))));
  }

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
  public void exitLateBoundCountDuplicatesFunction(LateBoundCountDuplicatesFunctionContext ctx) {
    this.stack.push(this.script.composeCountDuplicatesFunction(
        this.stack.pop(this.resolveSequenceType(this.stack.peekType()))));
  }

  @Override
  public void exitCountDuplicatesStringsFunction(CountDuplicatesStringsFunctionContext ctx) {
    this.stack.push(this.script.composeCountDuplicatesFunction(this.stack.pop(StringSequenceExpression.class)));
  }

  @Override
  public void exitCountDuplicatesBooleansFunction(CountDuplicatesBooleansFunctionContext ctx) {
    this.stack.push(this.script.composeCountDuplicatesFunction(this.stack.pop(BooleanSequenceExpression.class)));
  }

  @Override
  public void exitCountDuplicatesNumbersFunction(CountDuplicatesNumbersFunctionContext ctx) {
    this.stack.push(this.script.composeCountDuplicatesFunction(this.stack.pop(NumericSequenceExpression.class)));
  }

  @Override
  public void exitCountDuplicatesDatesFunction(CountDuplicatesDatesFunctionContext ctx) {
    this.stack.push(this.script.composeCountDuplicatesFunction(this.stack.pop(DateSequenceExpression.class)));
  }

  @Override
  public void exitCountDuplicatesTimesFunction(CountDuplicatesTimesFunctionContext ctx) {
    this.stack.push(this.script.composeCountDuplicatesFunction(this.stack.pop(TimeSequenceExpression.class)));
  }

  @Override
  public void exitCountDuplicatesDurationsFunction(CountDuplicatesDurationsFunctionContext ctx) {
    this.stack.push(this.script.composeCountDuplicatesFunction(this.stack.pop(DurationSequenceExpression.class)));
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
  public void exitLateBoundToNumberFunction(LateBoundToNumberFunctionContext ctx) {
    Class<? extends ScalarExpression> type = this.resolveScalarType(this.stack.peekType());
    if (StringExpression.class.isAssignableFrom(type)) {
      this.stack.push(this.script.composeToNumberConversion(this.stack.pop(StringExpression.class)));
    } else if (BooleanExpression.class.isAssignableFrom(type)) {
      this.stack.push(this.script.composeToNumberConversion(this.stack.pop(BooleanExpression.class)));
    } else {
      throw TypeMismatchException.cannotConvert(ctx, StringExpression.class, type);
    }
  }

  @Override
  public void exitSumFunction(SumFunctionContext ctx) {
    this.stack.push(this.script.composeSumOperation(this.stack.pop(NumericSequenceExpression.class)));
  }

  @Override
  public void exitMinFunction(MinFunctionContext ctx) {
    this.stack.push(this.script.composeMinFunction(this.stack.pop(NumericSequenceExpression.class)));
  }

  @Override
  public void exitMaxFunction(MaxFunctionContext ctx) {
    this.stack.push(this.script.composeMaxFunction(this.stack.pop(NumericSequenceExpression.class)));
  }

  @Override
  public void exitAverageFunction(AverageFunctionContext ctx) {
    this.stack.push(this.script.composeAvgFunction(this.stack.pop(NumericSequenceExpression.class)));
  }

  @Override
  public void exitStringLengthFunction(StringLengthFunctionContext ctx) {
    this.stack
        .push(this.script.composeStringLengthCalculation(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitAbsoluteFunction(AbsoluteFunctionContext ctx) {
    this.stack.push(this.script.composeAbsFunction(this.stack.pop(NumericExpression.class)));
  }

  @Override
  public void exitRoundFunction(RoundFunctionContext ctx) {
    this.stack.push(this.script.composeRoundFunction(this.stack.pop(NumericExpression.class)));
  }

  @Override
  public void exitRoundDownFunction(RoundDownFunctionContext ctx) {
    this.stack.push(this.script.composeFloorFunction(this.stack.pop(NumericExpression.class)));
  }

  @Override
  public void exitRoundUpFunction(RoundUpFunctionContext ctx) {
    this.stack.push(this.script.composeCeilingFunction(this.stack.pop(NumericExpression.class)));
  }

  // #endregion Numeric functions ---------------------------------------------

  // #region String functions -------------------------------------------------

  @Override
  public void exitSubstringFunction(SubstringFunctionContext ctx) {
    // Use Comma count instead of ctx.length label, because the label only matches
    // numericExpression and is null when the length argument is a lateBoundScalar.
    final NumericExpression length =
        ctx.Comma().size() > 1 ? this.stack.pop(NumericExpression.class) : null;
    final NumericExpression start = this.stack.pop(NumericExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    if (length != null) {
      this.stack.push(this.script.composeSubstringExtraction(text, start, length));
    } else {
      this.stack.push(this.script.composeSubstringExtraction(text, start));
    }
  }

  @Override
  public void exitSubstringBeforeFunction(SubstringBeforeFunctionContext ctx) {
    final StringExpression delimiter = this.stack.pop(StringExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeSubstringBeforeFunction(text, delimiter));
  }

  @Override
  public void exitSubstringAfterFunction(SubstringAfterFunctionContext ctx) {
    final StringExpression delimiter = this.stack.pop(StringExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeSubstringAfterFunction(text, delimiter));
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
  public void exitLateBoundToStringFunction(LateBoundToStringFunctionContext ctx) {
    Class<? extends ScalarExpression> type = this.resolveScalarType(this.stack.peekType());
    if (NumericExpression.class.isAssignableFrom(type)) {
      this.stack.push(this.script.composeToStringConversion(this.stack.pop(NumericExpression.class)));
    } else if (BooleanExpression.class.isAssignableFrom(type)) {
      this.stack.push(this.script.composeToStringConversion(this.stack.pop(BooleanExpression.class)));
    } else if (DateExpression.class.isAssignableFrom(type)) {
      this.stack.push(this.script.composeToStringConversion(this.stack.pop(DateExpression.class)));
    } else if (TimeExpression.class.isAssignableFrom(type)) {
      this.stack.push(this.script.composeToStringConversion(this.stack.pop(TimeExpression.class)));
    } else if (DurationExpression.class.isAssignableFrom(type)) {
      this.stack.push(this.script.composeToStringConversion(this.stack.pop(DurationExpression.class)));
    } else {
      throw TypeMismatchException.cannotConvert(ctx, NumericExpression.class, type);
    }
  }

  @Override
  public void exitConcatFunction(ConcatFunctionContext ctx) {
    // Count both stringExpression and lateBoundScalar children, because each
    // pushes a StringExpression to the stack.
    final int childCount = ctx.stringExpression().size() + ctx.lateBoundScalar().size();
    if (this.stack.empty() || childCount == 0) {
      this.stack.push(this.script.composeStringConcatenation(Collections.emptyList()));
      return;
    }

    List<StringExpression> list = new ArrayList<>();
    for (int i = 0; i < childCount; i++) {
      list.add(0, this.stack.pop(StringExpression.class));
    }
    this.stack.push(this.script.composeStringConcatenation(list));
  }

  @Override
  public void exitFormatNumberFunction(FormatNumberFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-number");
  }

  @Override
  public void exitLateBoundFormatShortFunction(LateBoundFormatShortFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-short");
  }

  @Override
  public void exitFormatShortDateFunction(FormatShortDateFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-short");
  }

  @Override
  public void exitFormatShortTimeFunction(FormatShortTimeFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-short");
  }

  @Override
  public void exitLateBoundFormatMediumFunction(LateBoundFormatMediumFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-medium");
  }

  @Override
  public void exitFormatMediumDateFunction(FormatMediumDateFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-medium");
  }

  @Override
  public void exitFormatMediumTimeFunction(FormatMediumTimeFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-medium");
  }

  @Override
  public void exitLateBoundFormatLongFunction(LateBoundFormatLongFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-long");
  }

  @Override
  public void exitFormatLongDateFunction(FormatLongDateFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-long");
  }

  @Override
  public void exitFormatLongTimeFunction(FormatLongTimeFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-long");
  }

  @Override
  public void exitFormatShortDateTimeFunction(FormatShortDateTimeFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-short");
  }

  @Override
  public void exitFormatMediumDateTimeFunction(FormatMediumDateTimeFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-medium");
  }

  @Override
  public void exitFormatLongDateTimeFunction(FormatLongDateTimeFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "format-long");
  }

  @Override
  public void exitUpperCaseFunction(UpperCaseFunctionContext ctx) {
    this.stack.push(this.script.composeToUpperCaseConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitLowerCaseFunction(LowerCaseFunctionContext ctx) {
    this.stack.push(this.script.composeToLowerCaseConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitNormalizeSpaceFunction(NormalizeSpaceFunctionContext ctx) {
    this.stack.push(this.script.composeNormalizeSpaceFunction(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitTrimFunction(TrimFunctionContext ctx) {
    this.stack.push(this.script.composeTrimFunction(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitTrimLeftFunction(TrimLeftFunctionContext ctx) {
    this.stack.push(this.script.composeTrimLeftFunction(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitTrimRightFunction(TrimRightFunctionContext ctx) {
    this.stack.push(this.script.composeTrimRightFunction(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitPadLeftFunction(PadLeftFunctionContext ctx) {
    final StringExpression padChar = this.stack.pop(StringExpression.class);
    final NumericExpression length = this.stack.pop(NumericExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composePadLeftFunction(text, length, padChar));
  }

  @Override
  public void exitPadRightFunction(PadRightFunctionContext ctx) {
    final StringExpression padChar = this.stack.pop(StringExpression.class);
    final NumericExpression length = this.stack.pop(NumericExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composePadRightFunction(text, length, padChar));
  }

  @Override
  public void exitRepeatFunction(RepeatFunctionContext ctx) {
    final NumericExpression count = this.stack.pop(NumericExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeRepeatFunction(text, count));
  }

  @Override
  public void exitReplaceFunction(ReplaceFunctionContext ctx) {
    final StringExpression replacement = this.stack.pop(StringExpression.class);
    final StringExpression search = this.stack.pop(StringExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeReplaceFunction(text, search, replacement));
  }

  @Override
  public void exitReplaceRegexFunction(ReplaceRegexFunctionContext ctx) {
    if (ctx.pattern instanceof StringLiteralExpressionContext) {
      EfxRegexValidator.validate(ctx.pattern.getText());
    }
    final StringExpression replacement = this.stack.pop(StringExpression.class);
    final StringExpression pattern = this.stack.pop(StringExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeReplaceRegexFunction(text, pattern, replacement));
  }

  @Override
  public void exitUrlEncodeFunction(UrlEncodeFunctionContext ctx) {
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeUrlEncodeFunction(text));
  }

  @Override
  public void exitCapitalizeFirstFunction(CapitalizeFirstFunctionContext ctx) {
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeCapitalizeFirstFunction(text));
  }

  @Override
  public void exitStringJoinFunction(StringJoinFunctionContext ctx) {
    final StringExpression separator = this.stack.pop(StringExpression.class);
    final StringSequenceExpression list = this.stack.pop(StringSequenceExpression.class);
    this.stack.push(this.script.composeStringJoin(list, separator));
  }

  @Override
  public void exitPreferredLanguageFunction(PreferredLanguageFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "preferred-language");
  }

  @Override
  public void exitPreferredLanguageTextFunction(PreferredLanguageTextFunctionContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, "preferred-language-text");
  }

  @Override
  public void exitFieldPreferredLanguageProperty(FieldPreferredLanguagePropertyContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, ":preferredLanguage");
  }

  @Override
  public void exitFieldPreferredLanguageTextProperty(FieldPreferredLanguageTextPropertyContext ctx) {
    throw InvalidUsageException.templateOnlyFunction(ctx, ":preferredLanguageText");
  }

  @Override
  public void exitDictionaryLookup(DictionaryLookupContext ctx) {
    if (this.currentCardinalityResolutionContext() == CardinalityResolutionContext.RESOLVE_SCALAR) {
      throw TypeMismatchException.dictionaryIsSequence(ctx, ctx.dictionaryName.getText());
    }
    var dictionary = this.stack.getDictionary(ctx.dictionaryName.getText());
    this.stack.push(this.script.composeDictionaryLookup(
        dictionary.name, this.stack.pop(StringExpression.class),
        dictionary.type));
  }

  @Override
  public void exitSplitFunction(SplitFunctionContext ctx) {
    final StringExpression delimiter = this.stack.pop(StringExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeSplitFunction(text, delimiter));
  }

  @Override
  public void exitIndexOfSubstringFunction(IndexOfSubstringFunctionContext ctx) {
    final StringExpression substring = this.stack.pop(StringExpression.class);
    final StringExpression text = this.stack.pop(StringExpression.class);
    this.stack.push(this.script.composeIndexOfSubstringFunction(text, substring));
  }

  // #endregion String functions ----------------------------------------------

  // #region Date functions ---------------------------------------------------

  @Override
  public void exitDateFromStringFunction(DateFromStringFunctionContext ctx) {
    this.stack.push(this.script.composeToDateConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitDatePlusDurationFunction(DatePlusDurationFunctionContext ctx) {
    this.exitDateDurationAddition();
  }

  @Override
  public void exitDateMinusDurationFunction(DateMinusDurationFunctionContext ctx) {
    this.exitDateDurationSubtraction();
  }

  @Override
  public void exitCurrentDateFunction(CurrentDateFunctionContext ctx) {
    this.stack.push(this.script.getCurrentDate());
  }

  @Override
  public void exitYearFromDateFunction(YearFromDateFunctionContext ctx) {
    this.stack.push(this.script.composeYearFunction(this.stack.pop(DateExpression.class)));
  }

  @Override
  public void exitMonthFromDateFunction(MonthFromDateFunctionContext ctx) {
    this.stack.push(this.script.composeMonthFunction(this.stack.pop(DateExpression.class)));
  }

  @Override
  public void exitDayFromDateFunction(DayFromDateFunctionContext ctx) {
    this.stack.push(this.script.composeDayFunction(this.stack.pop(DateExpression.class)));
  }

  // #endregion Date functions ------------------------------------------------

  // #region Time functions ---------------------------------------------------

  @Override
  public void exitTimeFromStringFunction(TimeFromStringFunctionContext ctx) {
    this.stack.push(this.script.composeToTimeConversion(this.stack.pop(StringExpression.class)));
  }

  @Override
  public void exitCurrentTimeFunction(CurrentTimeFunctionContext ctx) {
    this.stack.push(this.script.getCurrentTime());
  }

  @Override
  public void exitHoursFromTimeFunction(HoursFromTimeFunctionContext ctx) {
    this.stack.push(this.script.composeHoursFunction(this.stack.pop(TimeExpression.class)));
  }

  @Override
  public void exitMinutesFromTimeFunction(MinutesFromTimeFunctionContext ctx) {
    this.stack.push(this.script.composeMinutesFunction(this.stack.pop(TimeExpression.class)));
  }

  @Override
  public void exitSecondsFromTimeFunction(SecondsFromTimeFunctionContext ctx) {
    this.stack.push(this.script.composeSecondsFunction(this.stack.pop(TimeExpression.class)));
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

  @Override
  public void exitYearsFromDurationFunction(YearsFromDurationFunctionContext ctx) {
    this.stack.push(this.script.composeYearsFromDurationFunction(this.stack.pop(DurationExpression.class)));
  }

  @Override
  public void exitMonthsFromDurationFunction(MonthsFromDurationFunctionContext ctx) {
    this.stack.push(this.script.composeMonthsFromDurationFunction(this.stack.pop(DurationExpression.class)));
  }

  @Override
  public void exitDaysFromDurationFunction(DaysFromDurationFunctionContext ctx) {
    this.stack.push(this.script.composeDaysFromDurationFunction(this.stack.pop(DurationExpression.class)));
  }

  // #endregion Duration Functions --------------------------------------------

  // #region Sequence Functions -----------------------------------------------

  // #region Distinct-values ---------------------------------------------------

  @Override
  public void exitStringDistinctValuesFunction(StringDistinctValuesFunctionContext ctx) {
    this.exitDistinctValuesFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanDistinctValuesFunction(BooleanDistinctValuesFunctionContext ctx) {
    this.exitDistinctValuesFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericDistinctValuesFunction(NumericDistinctValuesFunctionContext ctx) {
    this.exitDistinctValuesFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateDistinctValuesFunction(DateDistinctValuesFunctionContext ctx) {
    this.exitDistinctValuesFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeDistinctValuesFunction(TimeDistinctValuesFunctionContext ctx) {
    this.exitDistinctValuesFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationDistinctValuesFunction(DurationDistinctValuesFunctionContext ctx) {
    this.exitDistinctValuesFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDistinctValuesFunction(LateBoundDistinctValuesFunctionContext ctx) {
    this.exitDistinctValuesFunction(this.resolveSequenceType(this.stack.peekType()));
  }

  private <T extends SequenceExpression> void exitDistinctValuesFunction(Class<T> listType) {
    final T list = this.stack.pop(listType);
    this.stack.push(this.script.composeDistinctValuesFunction(list, listType));
  }

  // #endregion Distinct-values ------------------------------------------------

  // #region Get-duplicates ---------------------------------------------------

  @Override
  public void exitStringGetDuplicatesFunction(StringGetDuplicatesFunctionContext ctx) {
    this.exitGetDuplicatesFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanGetDuplicatesFunction(BooleanGetDuplicatesFunctionContext ctx) {
    this.exitGetDuplicatesFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericGetDuplicatesFunction(NumericGetDuplicatesFunctionContext ctx) {
    this.exitGetDuplicatesFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateGetDuplicatesFunction(DateGetDuplicatesFunctionContext ctx) {
    this.exitGetDuplicatesFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeGetDuplicatesFunction(TimeGetDuplicatesFunctionContext ctx) {
    this.exitGetDuplicatesFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationGetDuplicatesFunction(DurationGetDuplicatesFunctionContext ctx) {
    this.exitGetDuplicatesFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundGetDuplicatesFunction(LateBoundGetDuplicatesFunctionContext ctx) {
    this.exitGetDuplicatesFunction(this.resolveSequenceType(this.stack.peekType()));
  }

  private <T extends SequenceExpression> void exitGetDuplicatesFunction(final Class<T> listType) {
    final T list = this.stack.pop(listType);
    this.stack.push(this.script.composeGetDuplicatesFunction(list, listType));
  }

  // #endregion Get-duplicates ------------------------------------------------

  // #region Union ------------------------------------------------------------

  @Override
  public void exitStringUnionFunction(StringUnionFunctionContext ctx) {
    this.exitUnionFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringUnionLeft(LateBoundStringUnionLeftContext ctx) {
    this.exitUnionFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringUnionRight(LateBoundStringUnionRightContext ctx) {
    this.exitUnionFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanUnionFunction(BooleanUnionFunctionContext ctx) {
    this.exitUnionFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanUnionLeft(LateBoundBooleanUnionLeftContext ctx) {
    this.exitUnionFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanUnionRight(LateBoundBooleanUnionRightContext ctx) {
    this.exitUnionFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericUnionFunction(NumericUnionFunctionContext ctx) {
    this.exitUnionFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericUnionLeft(LateBoundNumericUnionLeftContext ctx) {
    this.exitUnionFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericUnionRight(LateBoundNumericUnionRightContext ctx) {
    this.exitUnionFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateUnionFunction(DateUnionFunctionContext ctx) {
    this.exitUnionFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateUnionLeft(LateBoundDateUnionLeftContext ctx) {
    this.exitUnionFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateUnionRight(LateBoundDateUnionRightContext ctx) {
    this.exitUnionFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeUnionFunction(TimeUnionFunctionContext ctx) {
    this.exitUnionFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeUnionLeft(LateBoundTimeUnionLeftContext ctx) {
    this.exitUnionFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeUnionRight(LateBoundTimeUnionRightContext ctx) {
    this.exitUnionFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationUnionFunction(DurationUnionFunctionContext ctx) {
    this.exitUnionFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationUnionLeft(LateBoundDurationUnionLeftContext ctx) {
    this.exitUnionFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationUnionRight(LateBoundDurationUnionRightContext ctx) {
    this.exitUnionFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundUnionFunction(LateBoundUnionFunctionContext ctx) {
    this.exitUnionFunction(this.resolveSequenceType(this.stack.peekType(-1)));
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
    this.exitIntersectFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringIntersectLeft(LateBoundStringIntersectLeftContext ctx) {
    this.exitIntersectFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringIntersectRight(LateBoundStringIntersectRightContext ctx) {
    this.exitIntersectFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanIntersectFunction(BooleanIntersectFunctionContext ctx) {
    this.exitIntersectFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanIntersectLeft(LateBoundBooleanIntersectLeftContext ctx) {
    this.exitIntersectFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanIntersectRight(LateBoundBooleanIntersectRightContext ctx) {
    this.exitIntersectFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericIntersectFunction(NumericIntersectFunctionContext ctx) {
    this.exitIntersectFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericIntersectLeft(LateBoundNumericIntersectLeftContext ctx) {
    this.exitIntersectFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericIntersectRight(LateBoundNumericIntersectRightContext ctx) {
    this.exitIntersectFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateIntersectFunction(DateIntersectFunctionContext ctx) {
    this.exitIntersectFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateIntersectLeft(LateBoundDateIntersectLeftContext ctx) {
    this.exitIntersectFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateIntersectRight(LateBoundDateIntersectRightContext ctx) {
    this.exitIntersectFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeIntersectFunction(TimeIntersectFunctionContext ctx) {
    this.exitIntersectFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeIntersectLeft(LateBoundTimeIntersectLeftContext ctx) {
    this.exitIntersectFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeIntersectRight(LateBoundTimeIntersectRightContext ctx) {
    this.exitIntersectFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationIntersectFunction(DurationIntersectFunctionContext ctx) {
    this.exitIntersectFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationIntersectLeft(LateBoundDurationIntersectLeftContext ctx) {
    this.exitIntersectFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationIntersectRight(LateBoundDurationIntersectRightContext ctx) {
    this.exitIntersectFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundIntersectFunction(LateBoundIntersectFunctionContext ctx) {
    this.exitIntersectFunction(this.resolveSequenceType(this.stack.peekType(-1)));
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
    this.exitExceptFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringExceptLeft(LateBoundStringExceptLeftContext ctx) {
    this.exitExceptFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitLateBoundStringExceptRight(LateBoundStringExceptRightContext ctx) {
    this.exitExceptFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanExceptFunction(BooleanExceptFunctionContext ctx) {
    this.exitExceptFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanExceptLeft(LateBoundBooleanExceptLeftContext ctx) {
    this.exitExceptFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitLateBoundBooleanExceptRight(LateBoundBooleanExceptRightContext ctx) {
    this.exitExceptFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericExceptFunction(NumericExceptFunctionContext ctx) {
    this.exitExceptFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericExceptLeft(LateBoundNumericExceptLeftContext ctx) {
    this.exitExceptFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitLateBoundNumericExceptRight(LateBoundNumericExceptRightContext ctx) {
    this.exitExceptFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateExceptFunction(DateExceptFunctionContext ctx) {
    this.exitExceptFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateExceptLeft(LateBoundDateExceptLeftContext ctx) {
    this.exitExceptFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDateExceptRight(LateBoundDateExceptRightContext ctx) {
    this.exitExceptFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeExceptFunction(TimeExceptFunctionContext ctx) {
    this.exitExceptFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeExceptLeft(LateBoundTimeExceptLeftContext ctx) {
    this.exitExceptFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitLateBoundTimeExceptRight(LateBoundTimeExceptRightContext ctx) {
    this.exitExceptFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationExceptFunction(DurationExceptFunctionContext ctx) {
    this.exitExceptFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationExceptLeft(LateBoundDurationExceptLeftContext ctx) {
    this.exitExceptFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundDurationExceptRight(LateBoundDurationExceptRightContext ctx) {
    this.exitExceptFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundExceptFunction(LateBoundExceptFunctionContext ctx) {
    this.exitExceptFunction(this.resolveSequenceType(this.stack.peekType(-1)));
  }

  private <T extends SequenceExpression> void exitExceptFunction(Class<T> type) {
    T two = this.stack.pop(type);
    T one = this.stack.pop(type);
    this.stack.push(this.script.composeExceptFunction(one, two, type));
  }

  // #endregion Except ---------------------------------------------------------

  // #region Sort --------------------------------------------------------------

  @Override
  public void exitStringSortFunction(StringSortFunctionContext ctx) {
    this.exitSortFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanSortFunction(BooleanSortFunctionContext ctx) {
    this.exitSortFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericSortFunction(NumericSortFunctionContext ctx) {
    this.exitSortFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateSortFunction(DateSortFunctionContext ctx) {
    this.exitSortFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSortFunction(TimeSortFunctionContext ctx) {
    this.exitSortFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSortFunction(DurationSortFunctionContext ctx) {
    this.exitSortFunction(DurationSequenceExpression.class);
  }

  @Override
  public void exitLateBoundSortFunction(LateBoundSortFunctionContext ctx) {
    this.exitSortFunction(this.resolveSequenceType(this.stack.peekType()));
  }

  private <T extends SequenceExpression> void exitSortFunction(Class<T> listType) {
    final T list = this.stack.pop(listType);
    this.stack.push(this.script.composeSortFunction(list, listType));
  }

  // #endregion Sort -----------------------------------------------------------

  // #region Reverse ----------------------------------------------------------

  @Override
  public void exitLateBoundReverseFunction(LateBoundReverseFunctionContext ctx) {
    this.exitReverseFunction(this.resolveSequenceType(this.stack.peekType()));
  }

  @Override
  public void exitStringReverseFunction(StringReverseFunctionContext ctx) {
    this.exitReverseFunction(StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanReverseFunction(BooleanReverseFunctionContext ctx) {
    this.exitReverseFunction(BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericReverseFunction(NumericReverseFunctionContext ctx) {
    this.exitReverseFunction(NumericSequenceExpression.class);
  }

  @Override
  public void exitDateReverseFunction(DateReverseFunctionContext ctx) {
    this.exitReverseFunction(DateSequenceExpression.class);
  }

  @Override
  public void exitTimeReverseFunction(TimeReverseFunctionContext ctx) {
    this.exitReverseFunction(TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationReverseFunction(DurationReverseFunctionContext ctx) {
    this.exitReverseFunction(DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitReverseFunction(Class<T> listType) {
    final T list = this.stack.pop(listType);
    this.stack.push(this.script.composeReverseFunction(list, listType));
  }

  // #endregion Reverse --------------------------------------------------------

  // #region Subsequence ------------------------------------------------------

  @Override
  public void exitLateBoundSubsequenceFunction(LateBoundSubsequenceFunctionContext ctx) {
    boolean hasLength = ctx.Comma().size() > 1;
    int sequenceOffset = hasLength ? -2 : -1;
    this.exitSubsequenceFunction(hasLength,
        this.resolveSequenceType(this.stack.peekType(sequenceOffset)));
  }

  @Override
  public void exitStringSubsequenceFunction(StringSubsequenceFunctionContext ctx) {
    this.exitSubsequenceFunction(ctx.Comma().size() > 1, StringSequenceExpression.class);
  }

  @Override
  public void exitBooleanSubsequenceFunction(BooleanSubsequenceFunctionContext ctx) {
    this.exitSubsequenceFunction(ctx.Comma().size() > 1, BooleanSequenceExpression.class);
  }

  @Override
  public void exitNumericSubsequenceFunction(NumericSubsequenceFunctionContext ctx) {
    this.exitSubsequenceFunction(ctx.Comma().size() > 1, NumericSequenceExpression.class);
  }

  @Override
  public void exitDateSubsequenceFunction(DateSubsequenceFunctionContext ctx) {
    this.exitSubsequenceFunction(ctx.Comma().size() > 1, DateSequenceExpression.class);
  }

  @Override
  public void exitTimeSubsequenceFunction(TimeSubsequenceFunctionContext ctx) {
    this.exitSubsequenceFunction(ctx.Comma().size() > 1, TimeSequenceExpression.class);
  }

  @Override
  public void exitDurationSubsequenceFunction(DurationSubsequenceFunctionContext ctx) {
    this.exitSubsequenceFunction(ctx.Comma().size() > 1, DurationSequenceExpression.class);
  }

  private <T extends SequenceExpression> void exitSubsequenceFunction(boolean hasLength,
      Class<T> listType) {
    final NumericExpression length =
        hasLength ? this.stack.pop(NumericExpression.class) : null;
    final NumericExpression start = this.stack.pop(NumericExpression.class);
    final T list = this.stack.pop(listType);
    if (length != null) {
      this.stack.push(this.script.composeSubsequenceFunction(list, start, length, listType));
    } else {
      this.stack.push(this.script.composeSubsequenceFunction(list, start, listType));
    }
  }

  // #endregion Subsequence ----------------------------------------------------

  // #region Index-of ----------------------------------------------------------

  @Override
  public void exitLateBoundIndexOfFunction(LateBoundIndexOfFunctionContext ctx) {
    TypedExpression sequence = this.stack.peekType(-1);
    this.exitIndexOfFunction(this.resolveSequenceType(sequence), this.resolveScalarType(sequence));
  }

  @Override
  public void exitIndexOfStringFunction(IndexOfStringFunctionContext ctx) {
    this.exitIndexOfFunction(StringSequenceExpression.class, StringExpression.class);
  }

  @Override
  public void exitLateBoundStringIndexOfLeft(LateBoundStringIndexOfLeftContext ctx) {
    this.exitIndexOfFunction(StringSequenceExpression.class, StringExpression.class);
  }

  @Override
  public void exitLateBoundStringIndexOfRight(LateBoundStringIndexOfRightContext ctx) {
    this.exitIndexOfFunction(StringSequenceExpression.class, StringExpression.class);
  }

  @Override
  public void exitIndexOfBooleanFunction(IndexOfBooleanFunctionContext ctx) {
    this.exitIndexOfFunction(BooleanSequenceExpression.class, BooleanExpression.class);
  }

  @Override
  public void exitLateBoundBooleanIndexOfLeft(LateBoundBooleanIndexOfLeftContext ctx) {
    this.exitIndexOfFunction(BooleanSequenceExpression.class, BooleanExpression.class);
  }

  @Override
  public void exitLateBoundBooleanIndexOfRight(LateBoundBooleanIndexOfRightContext ctx) {
    this.exitIndexOfFunction(BooleanSequenceExpression.class, BooleanExpression.class);
  }

  @Override
  public void exitIndexOfNumericFunction(IndexOfNumericFunctionContext ctx) {
    this.exitIndexOfFunction(NumericSequenceExpression.class, NumericExpression.class);
  }

  @Override
  public void exitLateBoundNumericIndexOfLeft(LateBoundNumericIndexOfLeftContext ctx) {
    this.exitIndexOfFunction(NumericSequenceExpression.class, NumericExpression.class);
  }

  @Override
  public void exitLateBoundNumericIndexOfRight(LateBoundNumericIndexOfRightContext ctx) {
    this.exitIndexOfFunction(NumericSequenceExpression.class, NumericExpression.class);
  }

  @Override
  public void exitIndexOfDateFunction(IndexOfDateFunctionContext ctx) {
    this.exitIndexOfFunction(DateSequenceExpression.class, DateExpression.class);
  }

  @Override
  public void exitLateBoundDateIndexOfLeft(LateBoundDateIndexOfLeftContext ctx) {
    this.exitIndexOfFunction(DateSequenceExpression.class, DateExpression.class);
  }

  @Override
  public void exitLateBoundDateIndexOfRight(LateBoundDateIndexOfRightContext ctx) {
    this.exitIndexOfFunction(DateSequenceExpression.class, DateExpression.class);
  }

  @Override
  public void exitIndexOfTimeFunction(IndexOfTimeFunctionContext ctx) {
    this.exitIndexOfFunction(TimeSequenceExpression.class, TimeExpression.class);
  }

  @Override
  public void exitLateBoundTimeIndexOfLeft(LateBoundTimeIndexOfLeftContext ctx) {
    this.exitIndexOfFunction(TimeSequenceExpression.class, TimeExpression.class);
  }

  @Override
  public void exitLateBoundTimeIndexOfRight(LateBoundTimeIndexOfRightContext ctx) {
    this.exitIndexOfFunction(TimeSequenceExpression.class, TimeExpression.class);
  }

  @Override
  public void exitIndexOfDurationFunction(IndexOfDurationFunctionContext ctx) {
    this.exitIndexOfFunction(DurationSequenceExpression.class, DurationExpression.class);
  }

  @Override
  public void exitLateBoundDurationIndexOfLeft(LateBoundDurationIndexOfLeftContext ctx) {
    this.exitIndexOfFunction(DurationSequenceExpression.class, DurationExpression.class);
  }

  @Override
  public void exitLateBoundDurationIndexOfRight(LateBoundDurationIndexOfRightContext ctx) {
    this.exitIndexOfFunction(DurationSequenceExpression.class, DurationExpression.class);
  }

  private <T extends SequenceExpression, S extends ScalarExpression> void exitIndexOfFunction(
      Class<T> listType, Class<S> valueType) {
    final S value = this.stack.pop(valueType);
    final T list = this.stack.pop(listType);
    this.stack.push(this.script.composeIndexOfFunction(list, value));
  }

  // #endregion Index-of -------------------------------------------------------

  // #endregion Sequence Functions --------------------------------------------

  protected static String getLexerSymbol(int tokenType) {
    return EfxLexer.VOCABULARY.getLiteralName(tokenType).replaceAll("^'|'$", "");
  }

  /**
   * Guard: throws if an {@code #include} directive survived into the parse tree,
   * meaning the {@link IncludeProcessor} did not resolve it.
   */
  @Override
  public void exitIncludeDirective(IncludeDirectiveContext ctx) {
    String path = ctx.IncludePath() != null ? ctx.IncludePath().getText().trim() : "<unknown>";
    throw TranslatorConfigurationException.unresolvedIncludeDirective(path);
  }

  // #region Late-bound expression handlers -----------------------------------
  //
  // Handlers for late-bound grammar rules that mix late-bound expressions
  // with typed expressions. Each handler mirrors its typed counterpart, relying on
  // the CallStack's auto-conversion to coerce late-bound values to the expected type.
  //
  // Most late-bound handlers are co-located with their typed counterparts in their
  // respective regions (Boolean, Numeric, Duration, Conditional, Quantified).
  // This region contains only the type conversion handlers which don't have a natural
  // typed counterpart region.

  // --- Late-bound arithmetic dispatchers ---

  private void dispatchComposeLateBoundAdditiveExpression(LateBoundAdditiveExpressionContext ctx) {
    TypedExpression right = this.stack.peekType();
    TypedExpression left = this.stack.peekType(-1);

    String operator = ctx.operator.getText();
    if (ctx.operator.getType() == EfxLexer.Plus) {
      if (this.isDuration(right) && this.isDuration(left)) {
        this.exitDurationAddition();
      } else if (this.isDuration(right) && this.isDate(left)) {
        this.exitDateDurationAddition();
      } else if (this.isNumeric(right) && this.isNumeric(left)) {
        this.exitNumericOperation(operator);
      } else {
        throw TypeMismatchException.incompatibleOperands(ctx, operator, (Expression) left, (Expression) right);
      }
    } else if (ctx.operator.getType() == EfxLexer.Minus) {
      if (this.isDuration(right) && this.isDuration(left)) {
        this.exitDurationSubtraction();
      } else if (this.isDuration(right) && this.isDate(left)) {
        this.exitDateDurationSubtraction();
      } else if (this.isDate(right) && this.isDate(left)) {
        this.exitDateSubtraction();
      } else if (this.isNumeric(right) && this.isNumeric(left)) {
        this.exitNumericOperation(operator);
      } else {
        throw TypeMismatchException.incompatibleOperands(ctx, operator, (Expression) left, (Expression) right);
      }
    } else {
      throw TranslatorConfigurationException.unhandledOperator(operator,
          "dispatchComposeLateBoundAdditiveExpression");
    }
  }

  private void dispatchComposeLateBoundMultiplication(LateBoundMultiplicativeExpressionContext ctx) {
    TypedExpression right = this.stack.peekType();
    TypedExpression left = this.stack.peekType(-1);

    String operator = ctx.operator.getText();
    if (ctx.operator.getType() == EfxLexer.Star && this.isDuration(right) && this.isNumeric(left)) {
      this.exitDurationMultiplication();
    } else if (ctx.operator.getType() == EfxLexer.Star && this.isNumeric(right) && this.isDuration(left)) {
      this.exitReversedDurationMultiplication();
    } else if (this.isNumeric(right) && this.isNumeric(left)) {
      this.exitNumericOperation(operator);
    } else {
      throw TypeMismatchException.incompatibleOperands(ctx, operator, (Expression) left, (Expression) right);
    }
  }

  // #endregion Late-bound expression handlers --------------------------------

  // #region Late-bound cardinality resolution --------------------------------

  @Override
  public void enterLateBoundScalarReference(LateBoundScalarReferenceContext ctx) {
    this.cardinalityResolutionStack.push(
        this.adjustForGrammarAmbiguity(ctx, CardinalityResolutionContext.RESOLVE_SCALAR));
  }

  @Override
  public void exitLateBoundScalarReference(LateBoundScalarReferenceContext ctx) {
    this.cardinalityResolutionStack.pop();
  }

  @Override
  public void enterLateBoundSequenceReference(LateBoundSequenceReferenceContext ctx) {
    this.cardinalityResolutionStack.push(
        this.adjustForGrammarAmbiguity(ctx, CardinalityResolutionContext.RESOLVE_SEQUENCE));
  }

  @Override
  public void exitLateBoundSequenceReference(LateBoundSequenceReferenceContext ctx) {
    this.cardinalityResolutionStack.pop();
  }

  /**
   * Returns the current cardinality resolution context, or {@code RESOLVED} if the type
   * resolution stack is empty (i.e. we are outside any late-bound expression).
   */
  private CardinalityResolutionContext currentCardinalityResolutionContext() {
    return this.cardinalityResolutionStack.isEmpty()
        ? CardinalityResolutionContext.RESOLVED
        : this.cardinalityResolutionStack.peek();
  }

  /**
   * Adjusts the cardinality resolution context for grammar ambiguity. When a late-bound
   * reference node appears as a direct child of {@code lateBoundExpression}, it is ambiguous
   * whether the expression should resolve to a scalar or a sequence. In that case, this method
   * returns {@code RESOLVE_EITHER} instead of the default context.
   *
   * Because ANTLR labeled alternatives replace the parent rule's context, a
   * {@code lateBoundScalarReference} node IS the {@code lateBoundScalar} node in the parse
   * tree. So when the grammar path is
   * {@code lateBoundExpression -> lateBoundScalar -> #lateBoundScalarReference}, the parent of
   * the reference context is directly {@code LateBoundExpressionContext}.
   *
   * @param ctx the late-bound reference parse tree node (scalar or sequence reference)
   * @param defaultContext the context to use when no ambiguity is detected
   * @return {@code RESOLVE_EITHER} if the parent is {@code LateBoundExpressionContext},
   *         otherwise {@code defaultContext}
   */
  private CardinalityResolutionContext adjustForGrammarAmbiguity(ParserRuleContext ctx,
      CardinalityResolutionContext defaultContext) {
    return (ctx.getParent() instanceof LateBoundExpressionContext)
        ? CardinalityResolutionContext.RESOLVE_EITHER
        : defaultContext;
  }

  private enum CardinalityResolutionContext {
    RESOLVED,
    RESOLVE_SCALAR,
    RESOLVE_SEQUENCE,
    RESOLVE_EITHER
  }

  // #endregion Late-bound cardinality resolution --------------------------------

}
